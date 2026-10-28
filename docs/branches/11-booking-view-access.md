# Branch 11 – `fix/booking-view-access`

**Loại:** sửa theo phản hồi review · **Tổng số test:** 51 (một test được đổi nội dung)

## Mục tiêu

Sửa quy tắc quyền: **mọi người đã đăng nhập được xem mọi booking**, nhưng chỉ chủ booking hoặc ADMIN được hủy.

## Vấn đề được phát hiện khi review

Danh sách lịch (`GET /bookings`) trả booking của mọi người, nhưng xem chi tiết (`GET /bookings/{id}`) của người khác lại bị `403`. Hai API không nhất quán, và nhu cầu thực tế là phải thấy phòng nào đang bận để chọn khung giờ khác.

## Công việc đã làm

- `BookingService.get` không còn kiểm tra chủ sở hữu; `BookingController.get` bỏ tham số người dùng không còn dùng.
- Tách kiểm tra quyền thành `assertCanCancel` (chủ hoặc ADMIN), chỉ áp dụng cho hủy.
- Đổi test `otherUserCannotCancelButAdminCan` thành "người khác xem được (`200`) nhưng hủy bị `403`, ADMIN hủy được".
- Cập nhật quy tắc BR9 và BR16 trong tài liệu, README, bảng API.

## Các quyết định đáng nhớ

| Quyết định | Lý do |
|---|---|
| Xem: mọi người; hủy/sửa: chủ hoặc ADMIN | Cần minh bạch lịch để đặt đúng, nhưng thao tác ghi phải được bảo vệ |
| Sửa trước tài liệu, rồi mới sửa code | Requirement là nguồn sự thật; code theo sau |
| Thêm test cho hành vi mới thay vì chỉ sửa code | Hành vi quyền là điều dễ hồi quy |

## Câu hỏi NTD có thể hỏi

**Bạn xử lý phản hồi review thế nào?**
Xác nhận vấn đề, sửa tài liệu yêu cầu trước, sửa code, cập nhật test, chạy lại toàn bộ test, rồi commit riêng với mô tả rõ ràng.

**Việc cho xem mọi booking có vấn đề riêng tư không?**
Có thể, nếu có cuộc họp cần giữ kín nội dung. Đã ghi trong phần giả định; cách xử lý là tính năng "booking riêng tư" (ẩn tiêu đề với người ngoài), nằm ngoài phạm vi hiện tại.

**Vì sao không dùng `403` cho xem booking của người khác?**
`403` chỉ đúng khi người dùng không nên thấy dữ liệu đó. Ở đây lịch là thông tin chung của công ty.
