# Branch 7 – `feature/booking-query-cancel`

**Giai đoạn:** 4 – Triển khai (lát cắt 5) · **Test:** `BookingQueryCancelApiTest` (10 test; một test được sửa ở branch 11)

## Mục tiêu

Xem lịch đặt phòng, xem chi tiết, hủy booking, tìm phòng trống, và chặn vô hiệu hóa phòng còn booking sắp tới.

## Công việc đã làm

- `GET /bookings/{id}`: chi tiết booking.
- `GET /bookings?roomId&from&to&mine&includeCancelled&page&size`: lịch đặt phòng, phân trang, sắp xếp theo `startTime, id`; mặc định ẩn booking đã hủy.
- `POST /bookings/{id}/cancel`: hủy mềm.
  - Hủy booking đã hủy → `200` nguyên trạng (idempotent).
  - Hủy booking đã bắt đầu → `409 BOOKING_ALREADY_STARTED`.
- `GET /rooms/available?from&to&minCapacity`: chỉ trả phòng đang hoạt động, đủ sức chứa, không có booking giao nhau trong khoảng đó.
- Quy tắc BR11: vô hiệu hóa phòng còn booking `CONFIRMED` chưa kết thúc → `409 ROOM_HAS_UPCOMING_BOOKINGS`.
- `BookingApiTestBase`: lớp dùng chung cho các test API đặt phòng.

## Các quyết định đáng nhớ

| Quyết định | Lý do |
|---|---|
| Truy vấn lịch bằng Specification | Ghép điều kiện lọc động; tránh lỗi tham số `null` không có kiểu của PostgreSQL |
| `@EntityGraph` nạp luôn `room` | Tránh N+1 query khi trả tên phòng cho mỗi booking |
| Hủy mềm (`CANCELLED`), không xóa | Giữ lịch sử; booking hủy không giữ chỗ nên khung giờ đặt lại được |
| Hủy là idempotent | Người dùng bấm hai lần hoặc client retry không gây lỗi |
| Không cho hủy booking đã bắt đầu | Cuộc họp đã diễn ra, hủy không còn ý nghĩa |
| Điều kiện giao nhau khi lọc: `end > from AND start < to` | Cùng quy ước nửa mở với chống trùng lịch |
| Giới hạn `size` ≤ 100 | Không cho kéo quá nhiều dữ liệu |

> Ở branch này, xem chi tiết và hủy booking của người khác đều bị `403`. Branch 11 đổi lại: ai cũng xem được, chỉ chủ/ADMIN hủy được.

## Cách kiểm chứng

Test: hủy xong thì đặt lại được cùng khung giờ; hủy hai lần vẫn `200`; lịch lọc đúng theo phòng/khoảng giờ và ẩn booking đã hủy; `mine=true` chỉ trả booking của mình; khoảng thời gian sai `400`; tìm phòng trống loại phòng bận, phòng ngừng hoạt động, phòng nhỏ; không vô hiệu hóa được phòng còn booking sắp tới.

## Câu hỏi NTD có thể hỏi

**Vì sao phải lo N+1?**
Mỗi booking có tham chiếu lazy đến phòng. Không nạp trước thì 20 booking sinh thêm 20 truy vấn. `@EntityGraph` gộp vào một truy vấn có join.

**Vì sao hủy booking là idempotent?**
Mạng chập chờn có thể làm client gửi lại. Nếu lần hai báo lỗi, client khó phân biệt "thất bại" và "đã xong".

**Tìm phòng trống hoạt động thế nào?**
Lấy phòng đang hoạt động, đủ sức chứa, loại phòng có booking `CONFIRMED` giao với `[from, to)`. Phòng có booking nối tiếp vẫn được coi là trống.

**Vì sao không cho vô hiệu hóa phòng còn booking sắp tới?**
Tránh bỏ rơi những người đã đặt. ADMIN phải hủy các booking đó trước, nên có quyết định rõ ràng.

**Phân trang với sắp xếp thế nào cho ổn định?**
Sắp xếp theo `startTime` rồi `id` để thứ tự không đổi giữa các trang khi có booking cùng giờ.
