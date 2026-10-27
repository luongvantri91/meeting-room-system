# Branch 12 – `feature/booking-update`

**Loại:** tính năng bổ sung theo phản hồi review · **Test mới:** `BookingUpdateApiTest` (9) + 3 test đơn vị · **Tổng số test:** 63

## Mục tiêu

Cho phép **sửa booking** đã đặt (đổi phòng, giờ, tiêu đề, số người) thay vì bắt người dùng hủy rồi đặt lại.

## Công việc đã làm

- `PATCH /bookings/{id}`: sửa một phần, trường vắng mặt giữ nguyên, phải có ít nhất một trường.
- Chủ booking hoặc ADMIN mới được sửa (`403` với người khác).
- Booking sau khi sửa phải thỏa **mọi quy tắc như đặt mới**: giờ hợp lệ, thời lượng, phòng hoạt động, sức chứa, không trùng lịch.
- Kiểm tra trùng lịch **loại trừ chính booking đang sửa** (`existsOverlapExcluding`), nên kéo dài booking của chính mình vẫn được.
- Booking chưa bắt đầu: sửa mọi trường. Booking đang diễn ra: chỉ đổi giờ kết thúc (phải ở tương lai), tiêu đề, số người. Đã hủy hoặc đã kết thúc: `409`.
- Ghi bằng `saveAndFlush` và bắt lỗi constraint để request đồng thời vẫn trả `409`.
- Refactor `validateTime` thành các hàm nhỏ (`validateRange`, `validateDuration`) để dùng chung cho đặt phòng và sửa.
- Cập nhật tài liệu thiết kế, README và requirement.

## Các quyết định đáng nhớ

| Quyết định | Lý do |
|---|---|
| Dùng lại đúng bộ kiểm tra của đặt phòng | Một nguồn quy tắc, không có "đường vòng" qua sửa để lách luật |
| Loại trừ chính nó khi kiểm tra trùng | Nếu không, mọi lần kéo dài booking đều tự va chạm với chính nó |
| Exclusion constraint áp dụng cho cả `UPDATE` | Sửa booking cũng an toàn khi đồng thời mà không cần migration mới |
| Booking đang diễn ra chỉ được gia hạn/rút ngắn | Không cho "dời" cuộc họp đã bắt đầu sang phòng/giờ khác |
| Không cho rút ngắn về quá khứ (`422`) | Tránh biến cuộc họp đang diễn ra thành đã kết thúc bằng cách sửa |
| Không có trường người đặt trong request | Không đổi chủ booking được |

## Cách kiểm chứng

- API: đổi giờ/tiêu đề/số người, `PATCH` một phần, đổi phòng (phòng cũ trống lại), va chạm khung giờ khác (`409`) nhưng nối tiếp và gia hạn được, các quy tắc validation, phòng ngừng hoạt động/không tồn tại, quyền chủ/ADMIN, booking đã hủy, chưa đăng nhập.
- Đơn vị (đồng hồ cố định): booking đang diễn ra gia hạn được nhưng không đổi phòng/giờ bắt đầu; không rút ngắn về quá khứ; booking đã kết thúc không sửa được.

## Câu hỏi NTD có thể hỏi

**Sửa booking có nguy cơ gì so với đặt mới?**
Nguy cơ tự va chạm với chính nó khi kiểm tra trùng, và lách luật qua đường sửa. Đã xử lý bằng loại trừ chính nó và dùng chung bộ kiểm tra.

**Hai người cùng sửa vào một khung giờ trống thì sao?**
Exclusion constraint cũng áp dụng cho `UPDATE`, nên đúng một người thành công, người kia nhận `409`. Điểm này dựa vào constraint và chưa có test đồng thời riêng cho việc sửa.

**Vì sao không cho dời booking đang diễn ra sang phòng khác?**
Cuộc họp đã bắt đầu ở phòng đó. Cho dời là sửa lại lịch sử, nên chỉ cho gia hạn hoặc rút ngắn.

**Hai người cùng sửa một booking thì sao?**
Hiện chưa có khóa lạc quan (version). Bản ghi sau thắng. Hướng mở rộng là thêm trường `version` và trả `409` khi sai phiên bản.

**Vì sao `PATCH` mà không phải `PUT`?**
Client thường chỉ đổi một vài trường (ví dụ gia hạn thêm 30 phút), không cần gửi lại toàn bộ.
