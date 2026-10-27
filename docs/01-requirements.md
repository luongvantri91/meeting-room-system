# 01 – Phân tích yêu cầu

## 1. Bối cảnh
Công ty ~10 phòng họp, ~200 nhân viên, hiện đặt phòng qua nhắn tin nên hay trùng lịch.
Mục tiêu: backend REST API loại bỏ trùng lịch, có người dùng thường và quản trị viên.

## 2. Phạm vi

| Nhóm | Chức năng | Nguồn |
|---|---|---|
| Auth | Đăng ký, đăng nhập (JWT), vai trò `USER` / `ADMIN` | Đề: "người dùng thường và quản trị" |
| Room | Tạo / xem / sửa / vô hiệu hóa phòng (chỉ ADMIN ghi) | Đề |
| Booking | Tạo, xem, hủy lịch đặt | Đề |
| Calendar | Xem lịch theo phòng / khoảng thời gian | Đề |
| Availability | Tìm phòng trống theo khoảng thời gian (+ sức chứa) | Đề |
| Deployment | `docker compose up` chạy toàn bộ | Đề |

Ngoài phạm vi (ghi rõ trong README): frontend, gửi email/thông báo, lịch lặp (recurring), SSO/LDAP, thiết bị phòng họp.

## 3. Quy tắc nghiệp vụ

| # | Quy tắc | Loại |
|---|---|---|
| BR1 | `endTime > startTime` | Đề xuất |
| BR2 | Không đặt phòng đã vô hiệu hóa | Đề xuất |
| BR3 | Số người tham dự ≤ sức chứa phòng | Đề xuất |
| BR4 | Hai booking `CONFIRMED` cùng phòng không được giao nhau; kết thúc đúng lúc bắt đầu booking khác là hợp lệ (khoảng nửa mở `[start, end)`) | Từ đề (không trùng lịch) |
| BR5 | Booking `CANCELLED` không giữ chỗ | Đề xuất |
| BR6 | Không đặt trong quá khứ | Giả định |
| BR7 | Thời lượng tối thiểu 15 phút, tối đa 8 giờ; một booking không vắt qua nhiều ngày không bắt buộc | Giả định (cấu hình được) |
| BR8 | Thời gian lưu UTC, API dùng ISO-8601 có offset | Giả định |
| BR9 | USER chỉ hủy booking của chính mình; ADMIN hủy được tất cả | Giả định |
| BR10 | Người đặt lấy từ JWT, không tin dữ liệu client gửi | Bảo mật |
| BR11 | Vô hiệu hóa phòng không xóa booking tương lai có sẵn; trả cảnh báo/ từ chối nếu còn booking tương lai (chọn: **từ chối 409**) | Giả định |
| BR12 | Hủy booking đã hủy: idempotent, trả 200 với trạng thái hiện tại; hủy booking đã bắt đầu/đã qua: 409 | Giả định |

## 4. Tiêu chí nghiệm thu (rút gọn)

- Đặt hợp lệ → 201.
- Giao nhau một phần / nằm trong / bao trùm / trùng hệt → 409.
- Nối tiếp (end == start) → 201.
- Phòng không tồn tại → 404; phòng inactive → 422; vượt sức chứa → 422.
- Thời gian sai → 400; thiếu/sai token → 401; sai quyền → 403.
- Hai request đồng thời cùng khung giờ → đúng 1 thành công, 1 conflict.
- Booking đã hủy → đặt lại được cùng khung giờ.
- Tìm phòng trống không trả phòng đang có booking giao nhau hoặc phòng inactive.
