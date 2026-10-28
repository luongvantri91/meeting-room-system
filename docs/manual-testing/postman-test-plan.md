# Kịch bản test thủ công bằng Postman

File Postman: [meeting-room.postman_collection.json](meeting-room.postman_collection.json)

## 1. Chuẩn bị
1. Postman → **Import** → chọn file collection ở trên.
2. Tab **Variables** của collection: sửa `baseUrl` thành URL cổng 8080 (Codespaces: cổng phải ở chế độ **Public**; chạy local: `http://localhost:8080`). Nếu đã đổi mật khẩu admin thì sửa `adminPassword`.
3. Mỗi request đã có script kiểm tra kết quả (tab **Tests**), kết quả hiện ở tab **Test Results** (xanh = đạt, đỏ = lỗi).

## 2. Cách chạy
- **Chạy từng request** (để chụp ảnh): chạy lần lượt từ trên xuống. Chụp 3 thứ: request (method, URL, body), response (status, body) và tab Test Results.
- **Chạy cả bộ**: chuột phải collection → **Run collection** → giữ nguyên thứ tự → **Run**. Chụp màn hình tổng kết (số test pass/fail).
- Chạy lại được nhiều lần: request đầu tiên tự sinh `runId` (email, tên phòng không trùng) và `day` (một ngày ngẫu nhiên trong tương lai, tránh trùng lịch với lần chạy trước).
- Các request phụ thuộc nhau qua biến (token, id phòng, id booking). **Không bỏ qua hoặc đảo thứ tự**; nếu một request tạo dữ liệu bị lỗi, các request sau sẽ lỗi theo.

## 3. Danh sách test case

### 1. Auth - đăng ký / đăng nhập

| ID | Mô tả | Request | Kỳ vọng | Quy tắc |
|---|---|---|---|---|
| AUTH-01 | Admin mặc định đăng nhập thành công | `POST /api/v1/auth/login` | 200 | BR-Auth |
| AUTH-02 | Đăng ký user A hợp lệ -> role USER | `POST /api/v1/auth/register` | 201 |  |
| AUTH-03 | Đăng ký user B hợp lệ | `POST /api/v1/auth/register` | 201 |  |
| AUTH-04 | Đăng ký trùng email (khác hoa thường) -> 409 | `POST /api/v1/auth/register` | 409 `EMAIL_ALREADY_EXISTS` | BR15 |
| AUTH-05 | Đăng ký email sai định dạng -> 400 | `POST /api/v1/auth/register` | 400 `VALIDATION_ERROR` |  |
| AUTH-06 | Đăng ký mật khẩu 7 ký tự -> 400 | `POST /api/v1/auth/register` | 400 `VALIDATION_ERROR` | BR15 |
| AUTH-07 | Đăng ký thiếu fullName -> 400 | `POST /api/v1/auth/register` | 400 `VALIDATION_ERROR` |  |
| AUTH-08 | Client gửi role=ADMIN khi đăng ký -> vẫn là USER | `POST /api/v1/auth/register` | 201 | Bảo mật |
| AUTH-09 | User A đăng nhập đúng -> 200 + token | `POST /api/v1/auth/login` | 200 |  |
| AUTH-10 | User B đăng nhập đúng -> 200 + token | `POST /api/v1/auth/login` | 200 |  |
| AUTH-11 | Sai mật khẩu -> 401 | `POST /api/v1/auth/login` | 401 `INVALID_CREDENTIALS` |  |
| AUTH-12 | Email không tồn tại -> 401 (cùng lỗi với sai mật khẩu) | `POST /api/v1/auth/login` | 401 `INVALID_CREDENTIALS` |  |
| AUTH-13 | Gọi API cần đăng nhập khi không có token -> 401 | `GET /api/v1/rooms` | 401 `UNAUTHORIZED` |  |
| AUTH-14 | Token sai -> 401 | `GET /api/v1/rooms` | 401 `UNAUTHORIZED` |  |
| AUTH-15 | Health check không cần token | `GET /actuator/health` | 200 |  |

### 2. Rooms - quản lý phòng

| ID | Mô tả | Request | Kỳ vọng | Quy tắc |
|---|---|---|---|---|
| ROOM-01 | ADMIN tạo phòng A (sức chứa 10) | `POST /api/v1/rooms` | 201 |  |
| ROOM-02 | ADMIN tạo phòng B (sức chứa 5) | `POST /api/v1/rooms` | 201 |  |
| ROOM-03 | ADMIN tạo phòng C (sức chứa 20) | `POST /api/v1/rooms` | 201 |  |
| ROOM-04 | ADMIN tạo phòng D (sẽ bị vô hiệu hóa) | `POST /api/v1/rooms` | 201 |  |
| ROOM-05 | ADMIN tạo phòng F (dùng cho test sửa booking) | `POST /api/v1/rooms` | 201 |  |
| ROOM-06 | ADMIN tạo phòng G (dùng cho test lịch / phòng trống) | `POST /api/v1/rooms` | 201 |  |
| ROOM-07 | ADMIN tạo phòng H (dùng cho test vô hiệu hóa) | `POST /api/v1/rooms` | 201 |  |
| ROOM-08 | Tạo phòng trùng tên -> 409 | `POST /api/v1/rooms` | 409 `ROOM_NAME_ALREADY_EXISTS` | BR13 |
| ROOM-09 | Tạo phòng trùng tên khác hoa thường -> 409 | `POST /api/v1/rooms` | 409 `ROOM_NAME_ALREADY_EXISTS` | BR13 |
| ROOM-10 | USER tạo phòng -> 403 | `POST /api/v1/rooms` | 403 `FORBIDDEN` |  |
| ROOM-11 | Tạo phòng sức chứa 0 -> 400 | `POST /api/v1/rooms` | 400 `VALIDATION_ERROR` |  |
| ROOM-12 | Tạo phòng sức chứa âm -> 400 | `POST /api/v1/rooms` | 400 `VALIDATION_ERROR` |  |
| ROOM-13 | Tạo phòng tên rỗng -> 400 | `POST /api/v1/rooms` | 400 `VALIDATION_ERROR` |  |
| ROOM-14 | Tạo phòng không có token -> 401 | `POST /api/v1/rooms` | 401 `UNAUTHORIZED` |  |
| ROOM-15 | USER xem danh sách phòng (có phân trang) | `GET /api/v1/rooms` | 200 |  |
| ROOM-16 | Lọc active=true | `GET /api/v1/rooms` | 200 |  |
| ROOM-17 | Lọc minCapacity=20 | `GET /api/v1/rooms` | 200 |  |
| ROOM-18 | Phân trang size=101 -> 400 | `GET /api/v1/rooms` | 400 `VALIDATION_ERROR` |  |
| ROOM-19 | Xem chi tiết phòng A | `GET /api/v1/rooms/{{roomA}}` | 200 |  |
| ROOM-20 | Xem phòng không tồn tại -> 404 | `GET /api/v1/rooms/999999999` | 404 `ROOM_NOT_FOUND` |  |
| ROOM-21 | Id không phải số -> 400 | `GET /api/v1/rooms/abc` | 400 `MALFORMED_REQUEST` |  |
| ROOM-22 | USER sửa phòng -> 403 | `PATCH /api/v1/rooms/{{roomA}}` | 403 `FORBIDDEN` |  |
| ROOM-23 | ADMIN sửa location (các trường khác giữ nguyên) | `PATCH /api/v1/rooms/{{roomA}}` | 200 |  |
| ROOM-24 | ADMIN đổi tên phòng B | `PATCH /api/v1/rooms/{{roomB}}` | 200 |  |
| ROOM-25 | Đổi tên thành tên phòng khác đã có -> 409 | `PATCH /api/v1/rooms/{{roomB}}` | 409 `ROOM_NAME_ALREADY_EXISTS` |  |
| ROOM-26 | Đổi tên toàn khoảng trắng -> 400 | `PATCH /api/v1/rooms/{{roomB}}` | 400 `VALIDATION_ERROR` |  |
| ROOM-27 | Đổi sức chứa = 0 -> 400 | `PATCH /api/v1/rooms/{{roomB}}` | 400 `VALIDATION_ERROR` |  |
| ROOM-28 | Sửa phòng không tồn tại -> 404 | `PATCH /api/v1/rooms/999999999` | 404 `ROOM_NOT_FOUND` |  |
| ROOM-29 | ADMIN vô hiệu hóa phòng D (chưa có booking) -> 200 | `PATCH /api/v1/rooms/{{roomD}}` | 200 | BR11 |
| ROOM-30 | Danh sách active=false chỉ gồm phòng vô hiệu hóa | `GET /api/v1/rooms` | 200 |  |

### 3. Bookings - đặt phòng

| ID | Mô tả | Request | Kỳ vọng | Quy tắc |
|---|---|---|---|---|
| BOOK-01 | A đặt phòng A 09:00-10:00 (booking gốc) | `POST /api/v1/bookings` | 201 | BR10 |
| BOOK-02 | Trùng: 09:30-10:30 (chồng cuối) -> 409 | `POST /api/v1/bookings` | 409 `BOOKING_CONFLICT` | BR4 |
| BOOK-03 | Trùng: 08:30-09:30 (chồng đầu) -> 409 | `POST /api/v1/bookings` | 409 `BOOKING_CONFLICT` | BR4 |
| BOOK-04 | Trùng: 09:15-09:45 (nằm trong) -> 409 | `POST /api/v1/bookings` | 409 `BOOKING_CONFLICT` | BR4 |
| BOOK-05 | Trùng: 08:00-11:00 (bao trùm) -> 409 | `POST /api/v1/bookings` | 409 `BOOKING_CONFLICT` | BR4 |
| BOOK-06 | Trùng hệt: 09:00-10:00 -> 409 | `POST /api/v1/bookings` | 409 `BOOKING_CONFLICT` | BR4 |
| BOOK-07 | Nối tiếp sau: 10:00-11:00 -> 201 (nửa mở) | `POST /api/v1/bookings` | 201 | BR4 |
| BOOK-08 | Nối tiếp trước: 08:00-09:00 -> 201 (nửa mở) | `POST /api/v1/bookings` | 201 | BR4 |
| BOOK-09 | Khác phòng, cùng giờ 09:00-10:00 (phòng B) -> 201 | `POST /api/v1/bookings` | 201 | BR4 |
| BOOK-10 | Đúng sức chứa (10 người) 12:00-13:00 -> 201 | `POST /api/v1/bookings` | 201 | BR3 |
| BOOK-11 | Vượt sức chứa (11 người) -> 422 | `POST /api/v1/bookings` | 422 `CAPACITY_EXCEEDED` | BR3 |
| BOOK-12 | Phòng không tồn tại -> 404 | `POST /api/v1/bookings` | 404 `ROOM_NOT_FOUND` |  |
| BOOK-13 | Phòng đã vô hiệu hóa -> 422 | `POST /api/v1/bookings` | 422 `ROOM_INACTIVE` | BR2 |
| BOOK-14 | Kết thúc trước bắt đầu -> 400 | `POST /api/v1/bookings` | 400 `INVALID_TIME_RANGE` | BR1 |
| BOOK-15 | Kết thúc = bắt đầu -> 400 | `POST /api/v1/bookings` | 400 `INVALID_TIME_RANGE` | BR1 |
| BOOK-16 | Thiếu title -> 400 | `POST /api/v1/bookings` | 400 `VALIDATION_ERROR` |  |
| BOOK-17 | Thiếu roomId -> 400 | `POST /api/v1/bookings` | 400 `VALIDATION_ERROR` |  |
| BOOK-18 | attendees = 0 -> 400 | `POST /api/v1/bookings` | 400 `VALIDATION_ERROR` |  |
| BOOK-19 | title chỉ có khoảng trắng -> 400 | `POST /api/v1/bookings` | 400 `VALIDATION_ERROR` |  |
| BOOK-20 | Giờ thiếu múi giờ (2030-01-10T09:00:00) -> 400 | `POST /api/v1/bookings` | 400 `MALFORMED_REQUEST` | BR8 |
| BOOK-21 | Bắt đầu trong quá khứ -> 422 | `POST /api/v1/bookings` | 422 `BOOKING_IN_PAST` | BR6 |
| BOOK-22 | Thời lượng 14 phút -> 422 | `POST /api/v1/bookings` | 422 `INVALID_DURATION` | BR7 |
| BOOK-23 | Thời lượng đúng 15 phút -> 201 | `POST /api/v1/bookings` | 201 | BR7 |
| BOOK-24 | Thời lượng đúng 8 giờ -> 201 | `POST /api/v1/bookings` | 201 | BR7 |
| BOOK-25 | Thời lượng 8 giờ 1 phút -> 422 | `POST /api/v1/bookings` | 422 `INVALID_DURATION` | BR7 |
| BOOK-26 | Không có token -> 401 | `POST /api/v1/bookings` | 401 `UNAUTHORIZED` |  |
| BOOK-27 | Client giả userId/bookedBy = B -> bị bỏ qua, vẫn là A | `POST /api/v1/bookings` | 201 | BR10 |
| BOOK-28 | B đặt phòng A 18:00-19:00 (dùng cho test quyền) | `POST /api/v1/bookings` | 201 |  |
| BOOK-29 | A đặt phòng A 20:00-21:00 (sẽ hủy) | `POST /api/v1/bookings` | 201 | BR5 |
| BOOK-30 | A hủy booking 20:00-21:00 | `POST /api/v1/bookings/{{bkRC}}/cancel` | 200 |  |
| BOOK-31 | Đặt lại đúng khung giờ vừa hủy -> 201 | `POST /api/v1/bookings` | 201 | BR5 |

### 4. Bookings - xem / hủy

| ID | Mô tả | Request | Kỳ vọng | Quy tắc |
|---|---|---|---|---|
| VIEW-01 | B xem booking của A -> 200 (ai cũng xem được) | `GET /api/v1/bookings/{{bkA1}}` | 200 | BR16 |
| VIEW-02 | Xem booking không tồn tại -> 404 | `GET /api/v1/bookings/999999999` | 404 `BOOKING_NOT_FOUND` |  |
| VIEW-03 | Xem booking không có token -> 401 | `GET /api/v1/bookings/{{bkA1}}` | 401 `UNAUTHORIZED` |  |
| CANCEL-01 | B hủy booking của A -> 403 | `POST /api/v1/bookings/{{bkA1}}/cancel` | 403 `FORBIDDEN` | BR9 |
| CANCEL-02 | Hủy không có token -> 401 | `POST /api/v1/bookings/{{bkA1}}/cancel` | 401 `UNAUTHORIZED` |  |
| CANCEL-03 | B hủy booking 10:00-11:00 của chính mình -> 200 | `POST /api/v1/bookings/{{bkA2}}/cancel` | 200 |  |
| CANCEL-04 | Hủy lần hai (đã hủy) -> 200, vẫn CANCELLED | `POST /api/v1/bookings/{{bkA2}}/cancel` | 200 | BR12 |
| CANCEL-05 | ADMIN hủy booking của B -> 200 | `POST /api/v1/bookings/{{bkUserB}}/cancel` | 200 | BR9 |
| CANCEL-06 | Hủy booking không tồn tại -> 404 | `POST /api/v1/bookings/999999999/cancel` | 404 `BOOKING_NOT_FOUND` |  |

### 5. Bookings - sửa

| ID | Mô tả | Request | Kỳ vọng | Quy tắc |
|---|---|---|---|---|
| UPD-00a | Chuẩn bị: A đặt phòng F 09:00-10:00 | `POST /api/v1/bookings` | 201 |  |
| UPD-00b | Chuẩn bị: B đặt phòng F 10:30-11:30 | `POST /api/v1/bookings` | 201 |  |
| UPD-01 | Body rỗng {} -> 400 | `PATCH /api/v1/bookings/{{bkF1}}` | 400 `VALIDATION_ERROR` |  |
| UPD-02 | title rỗng -> 400 | `PATCH /api/v1/bookings/{{bkF1}}` | 400 `VALIDATION_ERROR` |  |
| UPD-03 | Chỉ đổi title -> 200, trường khác giữ nguyên | `PATCH /api/v1/bookings/{{bkF1}}` | 200 |  |
| UPD-04 | Kéo dài đến 10:30 (nối tiếp booking B, không tự trùng chính nó) -> 200 | `PATCH /api/v1/bookings/{{bkF1}}` | 200 | BR18 |
| UPD-05 | Kéo dài đến 11:00 (trùng booking của B) -> 409 | `PATCH /api/v1/bookings/{{bkF1}}` | 409 `BOOKING_CONFLICT` | BR18 |
| UPD-06 | Đổi attendees = 11 (> 10) -> 422 | `PATCH /api/v1/bookings/{{bkF1}}` | 422 `CAPACITY_EXCEEDED` | BR17 |
| UPD-07 | Rút còn 09:00-09:10 (dưới 15 phút) -> 422 | `PATCH /api/v1/bookings/{{bkF1}}` | 422 `INVALID_DURATION` | BR17 |
| UPD-08 | Giờ kết thúc 08:00 sớm hơn giờ bắt đầu 09:00 -> 400 | `PATCH /api/v1/bookings/{{bkF1}}` | 400 `INVALID_TIME_RANGE` |  |
| UPD-09 | Đổi sang giờ quá khứ -> 422 | `PATCH /api/v1/bookings/{{bkF1}}` | 422 `BOOKING_IN_PAST` | BR17 |
| UPD-10 | Đổi sang phòng đã vô hiệu hóa (D) -> 422 | `PATCH /api/v1/bookings/{{bkF1}}` | 422 `ROOM_INACTIVE` | BR17 |
| UPD-11 | Đổi sang phòng không tồn tại -> 404 | `PATCH /api/v1/bookings/{{bkF1}}` | 404 `ROOM_NOT_FOUND` |  |
| UPD-12 | B sửa booking của A -> 403 | `PATCH /api/v1/bookings/{{bkF1}}` | 403 `FORBIDDEN` | BR9 |
| UPD-13 | Sửa không có token -> 401 | `PATCH /api/v1/bookings/{{bkF1}}` | 401 `UNAUTHORIZED` |  |
| UPD-14 | Sửa booking không tồn tại -> 404 | `PATCH /api/v1/bookings/999999999` | 404 `BOOKING_NOT_FOUND` |  |
| UPD-15 | ADMIN sửa title booking của A -> 200 | `PATCH /api/v1/bookings/{{bkF1}}` | 200 | BR9 |
| UPD-16 | Dời sang 14:00-15:00 -> 200 | `PATCH /api/v1/bookings/{{bkF1}}` | 200 | BR19 |
| UPD-17 | Đổi sang phòng B (cùng giờ 14:00-15:00) -> 200 | `PATCH /api/v1/bookings/{{bkF1}}` | 200 | BR19 |
| UPD-18 | Phòng F đã trống lại: B đặt 14:00-15:00 -> 201 | `POST /api/v1/bookings` | 201 |  |
| UPD-19 | B sửa attendees booking của mình = 8 -> 200 | `PATCH /api/v1/bookings/{{bkF2}}` | 200 |  |
| UPD-20 | attendees = 0 -> 400 | `PATCH /api/v1/bookings/{{bkF2}}` | 400 `VALIDATION_ERROR` |  |
| UPD-21 | Sửa booking đã hủy -> 409 | `PATCH /api/v1/bookings/{{bkA2}}` | 409 `BOOKING_CANCELLED` | BR21 |

### 6. Calendar - xem lịch

| ID | Mô tả | Request | Kỳ vọng | Quy tắc |
|---|---|---|---|---|
| CAL-00a | Chuẩn bị: A đặt phòng G 09:00-10:00 | `POST /api/v1/bookings` | 201 |  |
| CAL-00b | Chuẩn bị: B đặt phòng G 11:00-12:00 | `POST /api/v1/bookings` | 201 |  |
| CAL-00c | Chuẩn bị: A đặt phòng G 13:00-14:00 | `POST /api/v1/bookings` | 201 |  |
| CAL-00d | Chuẩn bị: A hủy booking 13:00-14:00 | `POST /api/v1/bookings/{{bkG3}}/cancel` | 200 |  |
| CAL-01 | Lịch phòng G trong ngày: chỉ booking CONFIRMED (2) | `GET /api/v1/bookings` | 200 |  |
| CAL-02 | includeCancelled=true -> thêm booking đã hủy (3) | `GET /api/v1/bookings` | 200 |  |
| CAL-03 | mine=true (A) -> chỉ booking của A | `GET /api/v1/bookings` | 200 |  |
| CAL-04 | mine=true (B) -> chỉ booking của B | `GET /api/v1/bookings` | 200 |  |
| CAL-05 | Cửa sổ 10:00-11:00 (chỉ chạm biên) -> rỗng | `GET /api/v1/bookings` | 200 | BR4 |
| CAL-06 | Cửa sổ 09:30-11:30 -> 2 booking | `GET /api/v1/bookings` | 200 |  |
| CAL-07 | Phân trang size=1 -> 1 phần tử, tổng 2 | `GET /api/v1/bookings` | 200 |  |
| CAL-08 | from = to -> 400 | `GET /api/v1/bookings` | 400 `INVALID_TIME_RANGE` |  |
| CAL-09 | to trước from -> 400 | `GET /api/v1/bookings` | 400 `INVALID_TIME_RANGE` |  |
| CAL-10 | size=101 -> 400 | `GET /api/v1/bookings` | 400 `VALIDATION_ERROR` |  |
| CAL-11 | page=-1 -> 400 | `GET /api/v1/bookings` | 400 `VALIDATION_ERROR` |  |
| CAL-12 | Ngày sai định dạng -> 400 | `GET /api/v1/bookings` | 400 `MALFORMED_REQUEST` |  |
| CAL-13 | Xem lịch không có token -> 401 | `GET /api/v1/bookings` | 401 `UNAUTHORIZED` |  |

### 7. Availability - tìm phòng trống

| ID | Mô tả | Request | Kỳ vọng | Quy tắc |
|---|---|---|---|---|
| AVAIL-01 | Tìm 09:30-10:30: phòng G (có booking 09:00-10:00) KHÔNG có trong kết quả | `GET /api/v1/rooms/available` | 200 |  |
| AVAIL-02 | Tìm 10:00-11:00 (nối tiếp): phòng G CÓ trong kết quả | `GET /api/v1/rooms/available` | 200 | BR4 |
| AVAIL-03 | Phòng đã hủy booking (13:00-14:00) vẫn trống | `GET /api/v1/rooms/available` | 200 | BR5 |
| AVAIL-04 | minCapacity=1000 -> mảng rỗng | `GET /api/v1/rooms/available` | 200 |  |
| AVAIL-05 | minCapacity=20: có phòng C (20), không có phòng G (10) | `GET /api/v1/rooms/available` | 200 |  |
| AVAIL-06 | Phòng vô hiệu hóa (D) không bao giờ xuất hiện | `GET /api/v1/rooms/available` | 200 |  |
| AVAIL-07 | to = from -> 400 | `GET /api/v1/rooms/available` | 400 `INVALID_TIME_RANGE` |  |
| AVAIL-08 | Thiếu tham số from -> 400 | `GET /api/v1/rooms/available` | 400 `MALFORMED_REQUEST` |  |
| AVAIL-09 | Tìm phòng trống không có token -> 401 | `GET /api/v1/rooms/available` | 401 `UNAUTHORIZED` |  |

### 8. Rooms - vô hiệu hóa khi còn booking

| ID | Mô tả | Request | Kỳ vọng | Quy tắc |
|---|---|---|---|---|
| DEACT-01 | Chuẩn bị: A đặt phòng H 09:00-10:00 | `POST /api/v1/bookings` | 201 |  |
| DEACT-02 | ADMIN vô hiệu hóa phòng H khi còn booking sắp tới -> 409 | `PATCH /api/v1/rooms/{{roomH}}` | 409 `ROOM_HAS_UPCOMING_BOOKINGS` | BR11 |
| DEACT-03 | A hủy booking | `POST /api/v1/bookings/{{bkH}}/cancel` | 200 |  |
| DEACT-04 | Vô hiệu hóa phòng H sau khi hủy -> 200 | `PATCH /api/v1/rooms/{{roomH}}` | 200 | BR11 |
| DEACT-05 | Đặt phòng H đã vô hiệu hóa -> 422 | `POST /api/v1/bookings` | 422 `ROOM_INACTIVE` | BR2 |
| DEACT-06 | Kích hoạt lại phòng H -> 200 | `PATCH /api/v1/rooms/{{roomH}}` | 200 |  |
| DEACT-07 | Đặt phòng H lại -> 201 | `POST /api/v1/bookings` | 201 |  |

Tổng cộng **141 request**.

## 4. Không test được bằng Postman (đã có test tự động trong `mvn verify`)
| Tình huống | Lý do |
|---|---|
| Hủy / sửa booking **đã bắt đầu** hoặc **đã kết thúc** (BR12, BR20, BR21) | API không cho đặt booking trong quá khứ, nên cần giả lập đồng hồ. |
| 12 request đồng thời cùng phòng, cùng giờ -> 1×201 + 11×409 (BR14) | Postman Runner gửi tuần tự, không song song. |
| Token hết hạn | TTL mặc định 8 giờ. |

## 5. Đối chiếu trong database (làm bằng chứng)
Kết nối `localhost:5432`, DB `meetingroom`, user `meetingroom`.

```sql
-- Booking vừa tạo: đúng người đặt, trạng thái
SELECT b.id, r.name AS room, u.email, b.start_time, b.end_time, b.status, b.cancelled_at
FROM bookings b JOIN rooms r ON r.id = b.room_id JOIN users u ON u.id = b.user_id
ORDER BY b.id DESC LIMIT 30;

-- Ràng buộc chống trùng lịch tồn tại trong DB
SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'ex_bookings_no_overlap';

-- Mật khẩu được băm, không lưu dạng thô
SELECT email, role, left(password_hash, 7) AS hash_prefix FROM users ORDER BY id DESC LIMIT 5;

-- Phòng đã bị vô hiệu hóa
SELECT id, name, capacity, active FROM rooms ORDER BY id DESC;
```
