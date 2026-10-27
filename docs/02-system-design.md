# 02 – Thiết kế hệ thống

## 1. Kiến trúc
Modular monolith, layered: `controller → service → repository → PostgreSQL`.
Package theo module: `auth`, `room`, `booking`, `common` (error, config).

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

## 2. Database

```
users(id PK, email UNIQUE, password_hash, full_name, role, created_at)
rooms(id PK, name UNIQUE, capacity CHECK>0, location, active, created_at, updated_at)
bookings(id PK, room_id FK, user_id FK, title, attendees CHECK>0,
         start_time timestamptz, end_time timestamptz CHECK end>start,
         status CONFIRMED|CANCELLED, created_at, cancelled_at)
```

Chống trùng lịch ở tầng DB (nguồn sự thật):
```sql
EXCLUDE USING gist (room_id WITH =, tstzrange(start_time, end_time, '[)') WITH &&)
WHERE (status = 'CONFIRMED')
```
(cần extension `btree_gist`). Index phụ: `(room_id, start_time)`, `(user_id, start_time)`.

## 3. Quyết định chính

| Vấn đề | Chọn | Lý do |
|---|---|---|
| Race condition | Exclusion constraint + kiểm tra trước ở service để trả lỗi thân thiện | Đúng ngay cả khi có đường ghi khác; không phụ thuộc lock ở app |
| Auth | Spring Security + JWT (stateless), BCrypt | 200 user, không cần session; đơn giản chạy đa instance |
| Migration | Flyway | Schema có version, kiểm soát được |
| Thời gian | `timestamptz`, `Instant`, UTC | Tránh lỗi múi giờ |
| Khoảng thời gian | Nửa mở `[start, end)` | Cho phép họp nối tiếp |
| Lỗi | RFC 7807 `ProblemDetail` | Chuẩn, nhất quán |
| Test | JUnit5 + Mockito (unit), Testcontainers PostgreSQL (integration) | Constraint của PG phải test trên PG thật |
| Admin đầu tiên | Seed từ biến môi trường khi khởi động nếu chưa có | Không commit secret |

## 4. API contract (`/api/v1`)

| Method | Path | Quyền | Mô tả | Thành công |
|---|---|---|---|---|
| POST | /auth/register | public | Đăng ký (role USER) | 201 |
| POST | /auth/login | public | Nhận JWT | 200 |
| POST | /rooms | ADMIN | Tạo phòng | 201 |
| GET | /rooms | auth | Danh sách phòng (phân trang) | 200 |
| GET | /rooms/{id} | auth | Chi tiết | 200 |
| PATCH | /rooms/{id} | ADMIN | Cập nhật một phần (gồm `active`) | 200 |
| GET | /rooms/available?from&to&minCapacity | auth | Tìm phòng trống | 200 |
| POST | /bookings | auth | Đặt phòng | 201 |
| GET | /bookings/{id} | đăng nhập | Chi tiết (xem được mọi booking) | 200 |
| GET | /bookings?roomId&from&to&mine | auth | Lịch (phân trang) | 200 |
| POST | /bookings/{id}/cancel | chủ/ADMIN | Hủy | 200 |

Mã lỗi: 400 validation, 401, 403, 404, 409 trùng lịch / email trùng / vô hiệu hóa phòng còn booking, 422 phòng inactive / vượt sức chứa / thời gian quá khứ.

Body lỗi:
```json
{"type":"about:blank","title":"Booking conflict","status":409,"detail":"Room is already booked in the requested time range","code":"BOOKING_CONFLICT"}
```
