# Meeting Room Booking System

Backend REST API đặt phòng họp cho công ty (~10 phòng, ~200 nhân viên).
Java 17 · Spring Boot 3 · PostgreSQL 16 · Flyway · JWT · Docker.

## 1. Chạy hệ thống

```bash
docker compose up --build
```

API chạy tại `http://localhost:8080`, health check: `GET /actuator/health`.

Biến môi trường (xem [.env.example](.env.example); compose có giá trị mặc định cho dev):

| Biến | Ý nghĩa | Mặc định (dev) |
|------|---------|----------------|
| `DB_USER` / `DB_PASSWORD` | tài khoản PostgreSQL | `meetingroom` / `meetingroom` |
| `JWT_SECRET` | khóa ký JWT, **bắt buộc**, ≥ 32 ký tự | giá trị dev trong compose |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | admin được tạo lúc khởi động | `admin@company.local` / `ChangeMe123!` |

> Production: bắt buộc đổi `JWT_SECRET`, `ADMIN_PASSWORD`, `DB_PASSWORD`.

## 2. Chạy test

Không cần Docker – test dùng PostgreSQL nhúng (zonky embedded-postgres):

```bash
mvn verify
```

51 test: unit (service, JWT), integration API (MockMvc + PostgreSQL thật), test ràng buộc DB và test đồng thời
(12 request cùng lúc đặt 1 phòng/1 khung giờ → đúng 1 thành công). CI: `.github/workflows/ci.yml`.

## 3. API (`/api/v1`)

Xác thực: header `Authorization: Bearer <token>`. Lỗi theo RFC 7807 (`application/problem+json`) kèm trường `code`.

| Method | Path | Quyền | Mô tả |
|--------|------|-------|-------|
| POST | `/auth/register` | public | đăng ký (role USER) |
| POST | `/auth/login` | public | đăng nhập, nhận JWT |
| POST | `/rooms` | ADMIN | tạo phòng |
| GET | `/rooms?active=&minCapacity=&page=&size=` | đăng nhập | danh sách phòng |
| GET | `/rooms/{id}` | đăng nhập | chi tiết phòng |
| PATCH | `/rooms/{id}` | ADMIN | sửa / vô hiệu hóa phòng |
| GET | `/rooms/available?from=&to=&minCapacity=` | đăng nhập | tìm phòng trống |
| POST | `/bookings` | đăng nhập | đặt phòng |
| GET | `/bookings?roomId=&from=&to=&mine=&includeCancelled=` | đăng nhập | lịch đặt phòng (calendar) |
| GET | `/bookings/{id}` | chủ booking / ADMIN | chi tiết |
| POST | `/bookings/{id}/cancel` | chủ booking / ADMIN | hủy |

Ví dụ:

```bash
# Đăng nhập admin
TOKEN=$(curl -s localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@company.local","password":"ChangeMe123!"}' | jq -r .accessToken)

# Tạo phòng
curl -X POST localhost:8080/api/v1/rooms -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Phòng A","capacity":10,"location":"Tầng 2"}'

# Đặt phòng (thời gian ISO-8601, UTC)
curl -X POST localhost:8080/api/v1/bookings -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"roomId":1,"title":"Sprint planning","attendees":6,"startTime":"2030-01-10T02:00:00Z","endTime":"2030-01-10T03:00:00Z"}'

# Tìm phòng trống
curl "localhost:8080/api/v1/rooms/available?from=2030-01-10T02:00:00Z&to=2030-01-10T03:00:00Z&minCapacity=5" \
  -H "Authorization: Bearer $TOKEN"
```

Mã lỗi: `400` dữ liệu/khoảng thời gian sai · `401/403` chưa đăng nhập/không đủ quyền · `404` không tìm thấy ·
`409` xung đột (trùng lịch, trùng tên/email, booking đã bắt đầu, phòng còn booking sắp tới) ·
`422` vi phạm nghiệp vụ (phòng ngừng hoạt động, vượt sức chứa, đặt vào quá khứ, thời lượng ngoài 15 phút–8 giờ).

## 4. Thiết kế

```
Client → Controller → Service → Repository → PostgreSQL
          (validation, authz)   (business rules)   (constraints)
```

Package `com.company.meetingroom`: `auth`, `room`, `booking`, `common`. Tài liệu chi tiết:
[yêu cầu](docs/01-requirements.md) · [thiết kế hệ thống](docs/02-system-design.md) · [quy trình](docs/process-build.md).

### Quyết định kỹ thuật

- **Chống trùng lịch ở tầng DB**: constraint `EXCLUDE USING gist (room_id WITH =, tstzrange(start,end,'[)') WITH &&) WHERE status='CONFIRMED'`.
  Kiểm tra ở service chỉ để trả lỗi thân thiện; DB là chốt chặn cuối nên an toàn khi nhiều request đồng thời và nhiều instance.
- **Khoảng nửa mở `[start, end)`**: booking 9–10h và 10–11h không xung đột.
- **Thời gian**: lưu `timestamptz`, truyền ISO-8601 UTC; `Clock` được inject để test được.
- **JWT stateless + BCrypt**, phân quyền USER/ADMIN bằng `@PreAuthorize`; secret không có giá trị mặc định trong code.
- **Flyway** quản lý schema; Hibernate chỉ validate.
- **Hủy mềm** (status `CANCELLED`) để giữ lịch sử; hủy lặp lại là idempotent.
- **Vận hành**: `X-Request-Id` + MDC logging, graceful shutdown, không lộ stack trace, chỉ mở health của actuator.

### Giả định

- Một công ty / một múi giờ hiển thị phía client; API làm việc với UTC.
- Đăng ký tự do tạo USER; chỉ ADMIN (seed từ env) quản lý phòng.
- Không có booking định kỳ, không gửi thông báo.

### Giới hạn & hướng mở rộng

Refresh token, booking định kỳ, thông báo email, rate limiting, OpenAPI/Swagger, metrics (Prometheus).

> Lưu ý: `docker compose up` chưa được chạy thử trên máy phát triển (không có Docker); test chạy qua PostgreSQL nhúng.
