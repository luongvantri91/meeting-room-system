# 04 – Tài liệu API

Base path: `/api/v1`. Thiết kế chi tiết hợp đồng API: [03-system-design.md](03-system-design.md).

## Quy ước chung

- Xác thực: header `Authorization: Bearer <token>` (token lấy từ `POST /auth/login`).
- Thời gian: ISO-8601, UTC (ví dụ `2030-01-10T02:00:00Z`); khoảng thời gian là nửa mở `[start, end)`.
- Danh sách có phân trang: `page`, `size`; kết quả gồm `content`, `page`, `size`, `totalElements`, `totalPages`.
- Lỗi theo RFC 7807 (`application/problem+json`) kèm trường `code` ổn định.

## Endpoint

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
| GET | `/bookings/{id}` | đăng nhập | chi tiết (xem được mọi booking) |
| POST | `/bookings/{id}/cancel` | chủ booking / ADMIN | hủy (idempotent) |
| PATCH | `/bookings/{id}` | chủ booking / ADMIN | sửa phòng, giờ, tiêu đề, số người |

## Ví dụ

```bash
# Đăng nhập admin
TOKEN=$(curl -s localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@company.local","password":"ChangeMe123!"}' | jq -r .accessToken)

# Tạo phòng
curl -X POST localhost:8080/api/v1/rooms -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Phòng A","capacity":10,"location":"Tầng 2"}'

# Đặt phòng
curl -X POST localhost:8080/api/v1/bookings -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"roomId":1,"title":"Sprint planning","attendees":6,"startTime":"2030-01-10T02:00:00Z","endTime":"2030-01-10T03:00:00Z"}'

# Tìm phòng trống
curl "localhost:8080/api/v1/rooms/available?from=2030-01-10T02:00:00Z&to=2030-01-10T03:00:00Z&minCapacity=5" \
  -H "Authorization: Bearer $TOKEN"
```

Bộ 141 request Postman tương ứng: [manual-testing/](manual-testing/).

## Mã lỗi

| HTTP | Ý nghĩa | `code` |
|------|---------|--------|
| 400 | dữ liệu / khoảng thời gian sai | `VALIDATION_ERROR`, `MALFORMED_REQUEST`, `INVALID_TIME_RANGE` |
| 401 | chưa đăng nhập / sai thông tin | `UNAUTHORIZED`, `INVALID_CREDENTIALS` |
| 403 | không đủ quyền | `FORBIDDEN` |
| 404 | không tìm thấy | `ROOM_NOT_FOUND`, `BOOKING_NOT_FOUND` |
| 409 | xung đột | `BOOKING_CONFLICT`, `EMAIL_ALREADY_EXISTS`, `ROOM_NAME_ALREADY_EXISTS`, `ROOM_HAS_UPCOMING_BOOKINGS`, `BOOKING_ALREADY_STARTED`, `BOOKING_CANCELLED`, `BOOKING_ALREADY_ENDED`, `BOOKING_IN_PROGRESS` |
| 422 | vi phạm nghiệp vụ | `ROOM_INACTIVE`, `CAPACITY_EXCEEDED`, `BOOKING_IN_PAST`, `INVALID_DURATION` (thời lượng ngoài 15 phút – 8 giờ) |
