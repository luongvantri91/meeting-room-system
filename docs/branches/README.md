# Nhiệm vụ của từng branch

Quy trình theo 7 giai đoạn. Mỗi branch là một PR riêng, merge vào `main` theo đúng thứ tự.
Mỗi file bên dưới mô tả: mục tiêu, việc đã làm, quyết định kỹ thuật kèm lý do, cách kiểm chứng và các câu hỏi phỏng vấn có thể gặp.

| # | Branch | Giai đoạn | Tài liệu |
|---|---|---|---|
| – | (ghi chú bàn giao) | Trạng thái dự án, cách chạy lại, việc còn lại | [00](00-handoff.md) |
| 1 | `docs/requirement-analysis` | 1. Phân tích yêu cầu | [01](01-requirement-analysis.md) |
| 2 | `docs/system-design` | 2. Thiết kế hệ thống | [02](02-system-design.md) |
| 3 | `chore/project-foundation` | 3. Khởi tạo nền tảng | [03](03-project-foundation.md) |
| 4 | `feature/auth` | 4. Triển khai | [04](04-auth.md) |
| 5 | `feature/room-management` | 4. Triển khai | [05](05-room-management.md) |
| 6 | `feature/booking-creation` | 4. Triển khai | [06](06-booking-creation.md) |
| 7 | `feature/booking-query-cancel` | 4. Triển khai | [07](07-booking-query-cancel.md) |
| 8 | `test/quality-assurance` | 5. Kiểm thử | [08](08-quality-assurance.md) |
| 9 | `chore/production-readiness` | 6. Sẵn sàng production | [09](09-production-readiness.md) |
| 10 | `docs/delivery-readme` | 7. Bàn giao | [10](10-delivery-readme.md) |
| 11 | `fix/booking-view-access` | Sửa theo review | [11](11-booking-view-access.md) |
| 12 | `feature/booking-update` | Bổ sung theo review | [12](12-booking-update.md) |

## Năm điều cần nhớ nhất khi được hỏi

1. **Chống đặt trùng ở tầng DB** bằng exclusion constraint (`tstzrange '[)'`, chỉ tính `CONFIRMED`); service kiểm tra trước chỉ để báo lỗi thân thiện. Có test 12 request đồng thời.
2. **Khoảng nửa mở `[start, end)`**: booking 9–10h và 10–11h không xung đột.
3. **Phân biệt mã lỗi**: `400` viết sai, `422` vi phạm nghiệp vụ, `409` xung đột trạng thái, `401` chưa đăng nhập, `403` thiếu quyền.
4. **Bảo mật**: JWT stateless + BCrypt, secret bắt buộc từ biến môi trường, người đặt lấy từ token, không lộ stack trace.
5. **Trung thực về giới hạn**: `docker compose up` chưa được chạy thử trên máy phát triển; chưa có khóa lạc quan khi hai người cùng sửa một booking; chưa có refresh token, rate limiting, booking riêng tư.
