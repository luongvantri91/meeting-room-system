# 03 – Thiết kế hệ thống

> Tài liệu này mô tả **làm bằng cách nào** để đáp ứng [01-requirements.md](01-requirements.md).
> Mã `BRxx` là mã quy tắc nghiệp vụ trong tài liệu requirement.
> Cái nhìn tổng thể (sơ đồ ngữ cảnh, triển khai, thành phần) nằm ở [02-high-level-architecture.md](02-high-level-architecture.md).

## 1. Kiến trúc

Modular monolith, phân tầng: `controller → service → repository → PostgreSQL`.
Package theo module: `auth`, `room`, `booking`, `common` (lỗi, cấu hình).

```
Client ──HTTP/JSON──► Controller (validation, status code)
                         │ DTO
                         ▼
                      Service (business rules, @Transactional)
                         │ Entity
                         ▼
                      Repository (Spring Data JPA)
                         ▼
                      PostgreSQL (FK, CHECK, EXCLUDE, index)
```

**Vì sao monolith:** ~10 phòng, ~200 người dùng, không có nhu cầu tách service; tránh độ phức tạp của microservices.

## 2. Database

```
users(id PK, email, password_hash, full_name, role USER|ADMIN, created_at)
rooms(id PK, name, capacity CHECK>0, location, active, created_at, updated_at)
bookings(id PK, room_id FK, user_id FK, title, attendees CHECK>0,
         start_time timestamptz, end_time timestamptz CHECK end>start,
         status CONFIRMED|CANCELLED, created_at, cancelled_at)
```

**Ràng buộc quan trọng**

| Ràng buộc | Mục đích | Quy tắc |
|---|---|---|
| `UNIQUE INDEX ON users (lower(email))` | Email duy nhất, không phân biệt hoa thường | BR15 |
| `UNIQUE INDEX ON rooms (lower(name))` | Tên phòng duy nhất, không phân biệt hoa thường | BR13 |
| `CHECK (capacity > 0)`, `CHECK (attendees > 0)` | Dữ liệu hợp lệ ngay cả khi ghi trực tiếp vào DB | BR3 |
| `CHECK (end_time > start_time)` | Giờ kết thúc sau giờ bắt đầu | BR1 |
| `EXCLUDE USING gist (...)` (bên dưới) | Chống trùng lịch, kể cả khi đồng thời | BR4, BR5, BR14 |

**Chống trùng lịch ở tầng DB (nguồn sự thật):**

```sql
EXCLUDE USING gist (room_id WITH =, tstzrange(start_time, end_time, '[)') WITH &&)
WHERE (status = 'CONFIRMED')
```

- Cần extension `btree_gist`.
- `'[)'` là khoảng nửa mở nên booking nối tiếp không bị coi là giao nhau (BR4).
- `WHERE status = 'CONFIRMED'` nên booking đã hủy không giữ chỗ (BR5).
- Ràng buộc áp dụng cho cả `INSERT` lẫn `UPDATE`, nên việc **sửa booking** cũng được bảo vệ (BR17, BR18).

Index phụ: `(room_id, start_time)` cho truy vấn lịch theo phòng; `(user_id, start_time)` cho `mine=true`.

## 3. Quyết định chính

| Vấn đề | Chọn | Lý do |
|---|---|---|
| Race condition | Exclusion constraint + kiểm tra trước ở service để trả lỗi thân thiện | Đúng ngay cả khi có đường ghi khác; không phụ thuộc lock ở app |
| Auth | Spring Security + JWT (stateless), BCrypt, token hết hạn sau 8 giờ (`JWT_TTL`) | 200 user, không cần session; chạy đa instance dễ |
| Phân quyền | `@PreAuthorize("hasRole('ADMIN')")` cho API ghi phòng; chủ/ADMIN kiểm tra trong service cho sửa/hủy booking | Quyền theo dữ liệu (chủ booking) không biểu diễn được chỉ bằng role |
| Danh tính người đặt | Lấy từ JWT, request không có trường người đặt | BR10 |
| Migration | Flyway | Schema có version, kiểm soát được |
| Thời gian | `timestamptz`, `Instant`, UTC; `Clock` được inject | Tránh lỗi múi giờ; test được các tình huống "bây giờ" |
| Khoảng thời gian | Nửa mở `[start, end)` | Cho phép họp nối tiếp (BR4) |
| Giới hạn thời lượng | 15 phút – 8 giờ, cấu hình `app.booking.min-duration` / `max-duration` | BR7 |
| Lỗi | RFC 7807 `ProblemDetail` kèm trường `code` | Chuẩn, client xử lý theo `code` |
| Truy vấn lịch | Spring Data Specification, phân trang, sắp xếp theo `startTime, id` | Lọc linh hoạt, tránh lỗi tham số null của PostgreSQL |
| Test | JUnit 5 + Mockito (unit); integration trên **PostgreSQL thật** (embedded, không cần Docker) | Constraint của PG phải test trên PG thật; chạy được ở CI |
| Admin đầu tiên | Seed từ biến môi trường khi khởi động nếu chưa có | Không commit secret |

## 4. API contract (`/api/v1`)

Mọi API trừ `register`, `login` và `/actuator/health` đều yêu cầu `Authorization: Bearer <token>`.
Thời gian dùng ISO-8601 có múi giờ, ví dụ `2030-01-10T02:00:00Z`.

### 4.1. Danh sách API

| Method | Path | Quyền | Mô tả | Thành công |
|---|---|---|---|---|
| POST | `/auth/register` | public | Đăng ký (role USER) | 201 |
| POST | `/auth/login` | public | Nhận JWT | 200 |
| POST | `/rooms` | ADMIN | Tạo phòng | 201 |
| GET | `/rooms?active&minCapacity&page&size` | đăng nhập | Danh sách phòng (phân trang) | 200 |
| GET | `/rooms/{id}` | đăng nhập | Chi tiết phòng | 200 |
| PATCH | `/rooms/{id}` | ADMIN | Cập nhật một phần (gồm `active`) | 200 |
| GET | `/rooms/available?from&to&minCapacity` | đăng nhập | Tìm phòng trống | 200 |
| POST | `/bookings` | đăng nhập | Đặt phòng | 201 |
| GET | `/bookings/{id}` | đăng nhập | Chi tiết booking (xem được mọi booking, BR16) | 200 |
| GET | `/bookings?roomId&from&to&mine&includeCancelled&page&size` | đăng nhập | Lịch đặt phòng (phân trang) | 200 |
| PATCH | `/bookings/{id}` | chủ / ADMIN | Sửa một phần: phòng, giờ, tiêu đề, số người (BR17–BR21) | 200 |
| POST | `/bookings/{id}/cancel` | chủ / ADMIN | Hủy (BR9, BR12) | 200 |

`size` tối đa 100 (mặc định 20). Lịch mặc định chỉ gồm booking `CONFIRMED`; thêm `includeCancelled=true` để xem cả booking đã hủy; `mine=true` chỉ lấy booking của người đang đăng nhập.

### 4.2. Ví dụ request

Đặt phòng (`POST /bookings`) – tất cả trường bắt buộc:

```json
{"roomId": 1, "title": "Sprint planning", "attendees": 6,
 "startTime": "2030-01-10T02:00:00Z", "endTime": "2030-01-10T03:00:00Z"}
```

Sửa booking (`PATCH /bookings/{id}`) – gửi ít nhất một trường, trường vắng mặt giữ nguyên:

```json
{"endTime": "2030-01-10T03:30:00Z"}
```

### 4.3. Luồng xử lý đặt phòng và sửa booking

**Đặt phòng** (thứ tự kiểm tra quyết định mã lỗi trả về):

1. Giờ kết thúc sau giờ bắt đầu (`400`).
2. Không bắt đầu trong quá khứ (`422`), thời lượng 15 phút – 8 giờ (`422`).
3. Phòng tồn tại (`404`), đang hoạt động (`422`), đủ sức chứa (`422`).
4. Kiểm tra trùng lịch ở service (`409`) rồi ghi xuống DB; nếu có request đồng thời thắng trước, exclusion constraint từ chối và service cũng trả `409`.

**Sửa booking** (`PATCH`):

1. Có ít nhất một trường và tiêu đề không rỗng (`400`).
2. Booking tồn tại (`404`), người sửa là chủ hoặc ADMIN (`403`).
3. Booking chưa hủy (`409`) và chưa kết thúc (`409`).
4. Booking đang diễn ra thì không được đổi phòng hoặc giờ bắt đầu (`409`) (BR20).
5. Áp lại các kiểm tra như khi đặt phòng cho kết quả sau khi sửa (BR17). Booking đang diễn ra thì giờ kết thúc mới phải ở tương lai (`422`).
6. Kiểm tra trùng lịch **loại trừ chính booking này** (BR18), rồi ghi và flush để bắt lỗi từ constraint.

**Hủy booking:** chủ hoặc ADMIN (`403`); đã hủy thì trả `200` nguyên trạng (BR12); đã bắt đầu thì `409`.

**Vô hiệu hóa phòng:** từ chối `409` nếu còn booking `CONFIRMED` chưa kết thúc (BR11).

## 5. Mã lỗi

Body lỗi (RFC 7807, thêm trường `code`):

```json
{"type":"about:blank","title":"Booking conflict","status":409,"detail":"Room is already booked in the requested time range","code":"BOOKING_CONFLICT"}
```

Lỗi validation có thêm `errors` (map trường → thông báo). Không bao giờ trả stack trace cho client.

| HTTP | `code` | Khi nào |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Thiếu/sai trường, tiêu đề rỗng, mật khẩu < 8 ký tự, sai tham số query, `PATCH` không có trường nào |
| 400 | `MALFORMED_REQUEST` | Body không phải JSON hợp lệ hoặc sai định dạng giờ |
| 400 | `INVALID_TIME_RANGE` | Giờ kết thúc không sau giờ bắt đầu (cả khi tìm lịch/phòng trống) |
| 401 | `UNAUTHORIZED` | Không có token, token sai hoặc hết hạn |
| 401 | `INVALID_CREDENTIALS` | Đăng nhập sai email/mật khẩu |
| 403 | `FORBIDDEN` | USER gọi API của ADMIN; sửa/hủy booking của người khác |
| 404 | `ROOM_NOT_FOUND`, `BOOKING_NOT_FOUND` | Phòng / booking không tồn tại |
| 409 | `BOOKING_CONFLICT` | Trùng lịch (BR4, BR14, BR18) |
| 409 | `EMAIL_ALREADY_EXISTS`, `ROOM_NAME_ALREADY_EXISTS` | Trùng email / tên phòng (BR13, BR15) |
| 409 | `ROOM_HAS_UPCOMING_BOOKINGS` | Vô hiệu hóa phòng còn booking sắp tới (BR11) |
| 409 | `BOOKING_ALREADY_STARTED` | Hủy booking đã bắt đầu (BR12) |
| 409 | `BOOKING_CANCELLED`, `BOOKING_ALREADY_ENDED`, `BOOKING_IN_PROGRESS` | Sửa booking đã hủy / đã kết thúc / đổi phòng hoặc giờ bắt đầu khi đang diễn ra (BR20, BR21) |
| 422 | `ROOM_INACTIVE` | Đặt hoặc chuyển booking sang phòng đã vô hiệu hóa (BR2) |
| 422 | `CAPACITY_EXCEEDED` | Số người vượt sức chứa (BR3) |
| 422 | `BOOKING_IN_PAST` | Bắt đầu trong quá khứ, hoặc giờ kết thúc mới đã qua khi sửa booking đang diễn ra (BR6) |
| 422 | `INVALID_DURATION` | Thời lượng ngoài 15 phút – 8 giờ (BR7) |
| 500 | `INTERNAL_ERROR` | Lỗi không lường trước; chi tiết chỉ nằm trong log |

## 6. Bảng truy vết: quy tắc nghiệp vụ → nơi thực thi

| Quy tắc | Nơi thực thi |
|---|---|
| BR1 `end > start` | Service (`400`) + `CHECK` ở DB |
| BR2 phòng inactive | Service (`422`) |
| BR3 sức chứa | Service (`422`) |
| BR4 không trùng lịch, nửa mở | Service (kiểm tra trước) + **exclusion constraint** (chốt chặn cuối) |
| BR5 booking đã hủy không giữ chỗ | Điều kiện `WHERE status='CONFIRMED'` ở constraint và ở truy vấn |
| BR6 không đặt quá khứ | Service (`422`) |
| BR7 thời lượng 15 phút – 8 giờ | Service, cấu hình bằng `app.booking.*` |
| BR8 UTC, ISO-8601 | `timestamptz` + `Instant` |
| BR9 chủ/ADMIN sửa, hủy | Service (`403`) |
| BR10 người đặt từ JWT | DTO không có trường người đặt; service lấy `AuthUser` |
| BR11 vô hiệu hóa phòng còn booking | `RoomService` (`409`) |
| BR12 hủy idempotent, không hủy booking đã bắt đầu | `BookingService.cancel` |
| BR13, BR15 duy nhất tên phòng / email | Unique index trên `lower(...)` + kiểm tra trước ở service |
| BR14 đặt đồng thời | Exclusion constraint + test 12 request đồng thời |
| BR16 xem mọi booking | `GET /bookings/{id}` và lịch không giới hạn theo chủ |
| BR17–BR21 sửa booking | `BookingService.update` (xem 4.3) |
