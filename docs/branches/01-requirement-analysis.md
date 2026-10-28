# Branch 1 – `docs/requirement-analysis`

**Giai đoạn:** 1 – Phân tích yêu cầu · **Sản phẩm:** [01-requirements.md](../01-requirements.md)

## Mục tiêu

Trước khi viết code, làm rõ **hệ thống phải làm gì** và **khi nào thì coi là xong**, để cả nhóm hiểu giống nhau. Đề bài chỉ nói ngắn gọn (10 phòng, 200 nhân viên, đặt phòng, USER/ADMIN), nên phải tự tìm ra những quy tắc đề chưa nói.

## Công việc đã làm

- Xác định phạm vi: Auth, Room, Booking, Calendar, Availability, Deployment; ghi rõ cái gì **ngoài phạm vi** (frontend, email, lịch lặp, SSO).
- Lập bảng vai trò và quyền (chưa đăng nhập / USER / ADMIN).
- Liệt kê quy tắc nghiệp vụ `BR1…BR21`, mỗi quy tắc có ví dụ số liệu và nguồn: *Từ đề*, *Đề xuất*, *Giả định*, *Bảo mật*.
- Viết tiêu chí nghiệm thu dạng bảng "Tình huống → Kết quả HTTP".
- Tổng hợp mã trạng thái HTTP và liệt kê các giả định cần xác nhận.

## Các quyết định đáng nhớ

| Quyết định | Lý do |
|---|---|
| Khoảng thời gian **nửa mở `[start, end)`** | Họp 9–10h và 10–11h liên tiếp là bình thường, không được coi là trùng |
| Phân biệt rõ quy tắc *Từ đề* và *Giả định* | Không tự ý coi giả định của mình là yêu cầu chính thức |
| Không cho đặt vào quá khứ; thời lượng 15 phút – 8 giờ | Tránh dữ liệu vô nghĩa; giá trị chỉnh được bằng cấu hình |
| Lưu giờ UTC, ISO-8601 có múi giờ | Tránh lỗi múi giờ, client tự đổi giờ hiển thị |
| Mọi người xem được mọi booking, chỉ chủ/ADMIN được sửa, hủy | Cần biết phòng nào bận để chọn giờ khác (đổi sau khi review) |
| `400` cho request viết sai, `422` cho vi phạm nghiệp vụ, `409` cho xung đột trạng thái | Client xử lý lỗi theo ý nghĩa, không phải đoán |

## Cách kiểm chứng

Mỗi dòng trong "Tiêu chí nghiệm thu" là một test ở các branch sau (API test, unit test, test ràng buộc DB).

## Câu hỏi NTD có thể hỏi

**Đề bài thiếu thông tin thì bạn làm thế nào?**
Tôi liệt kê các điểm chưa rõ, tự chọn hướng hợp lý, ghi rõ đó là *giả định* trong tài liệu (mục 8) để người đọc xác nhận hoặc đổi.

**Vì sao `201` chứ không phải `200` khi đặt phòng?**
`201 Created` là mã chuẩn khi request tạo ra tài nguyên mới. Hủy booking không tạo gì mới nên trả `200`.

**Vì sao đặt vào quá khứ là `422` mà giờ kết thúc trước giờ bắt đầu là `400`?**
`400`: yêu cầu viết sai (vô lý về mặt dữ liệu). `422`: viết đúng nhưng nghiệp vụ không cho phép.

**Khoảng nửa mở nghĩa là gì?**
Booking 9:00–10:00 chiếm phòng từ 9:00 đến 9:59, còn 10:00 là phòng đã trống. Nhờ vậy booking 10:00–11:00 hợp lệ.

**Vì sao viết ví dụ trong tài liệu requirement?**
Câu chữ chung chung dễ hiểu nhầm (ví dụ "end == start" không rõ của booking nào). Ví dụ số liệu thì không mơ hồ và dùng thẳng được làm test.
