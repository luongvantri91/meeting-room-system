# Branch 10 – `docs/delivery-readme`

**Giai đoạn:** 7 – Bàn giao · **Sản phẩm:** [README.md](../../README.md)

## Mục tiêu

Người đánh giá có thể chạy hệ thống và hiểu các quyết định mà **không cần hỏi lại**.

## Công việc đã làm

README (tiếng Việt) gồm:

1. Cách chạy bằng `docker compose up --build`, địa chỉ API, health check.
2. Bảng biến môi trường và lưu ý đổi giá trị khi triển khai thật.
3. Cách chạy test (`mvn verify`, không cần Docker) và số lượng test.
4. Bảng toàn bộ API kèm quyền, ví dụ `curl`, bảng mã lỗi.
5. Sơ đồ kiến trúc, cấu trúc package, liên kết tới tài liệu yêu cầu và thiết kế.
6. Các quyết định kỹ thuật: exclusion constraint, khoảng nửa mở, UTC, JWT, Flyway, hủy mềm, vận hành.
7. Giả định, giới hạn và hướng mở rộng.
8. Ghi chú trung thực: `docker compose up` chưa được chạy thử trên máy phát triển.

## Các quyết định đáng nhớ

| Quyết định | Lý do |
|---|---|
| README có ví dụ `curl` chạy được ngay | Người chấm tái hiện luồng đầu-cuối không phải đoán |
| Nêu rõ giới hạn và hướng mở rộng | Cho thấy biết mình chưa làm gì và vì sao |
| Ghi thẳng phần chưa kiểm chứng | Trung thực, người đọc biết chỗ cần kiểm tra |
| Link tài liệu thay vì lặp nội dung | Một nguồn sự thật, không bị lệch |

## Cách kiểm chứng

Làm theo đúng README trong một thư mục sạch (clone mới) và chạy `docker compose up --build`. **Nên làm một lần trước khi nộp** vì đây là phần chưa được chạy thử.

## Câu hỏi NTD có thể hỏi

**Làm sao tôi chạy được hệ thống của bạn?**
`docker compose up --build`, rồi gọi API theo các ví dụ trong README; tài khoản admin mặc định ở phần biến môi trường.

**Vì sao README ghi cả những thứ chưa làm?**
Phạm vi rõ ràng và trung thực quan trọng hơn khoe tính năng. Phần chưa làm đi kèm lý do và hướng mở rộng.

**Nếu bạn có thêm thời gian thì làm gì?**
Refresh token, đặt phòng lặp lại, thông báo email, rate limiting, OpenAPI/Swagger, metrics, booking riêng tư.

**Điều gì bạn chưa kiểm chứng?**
Stack Docker chưa được chạy trên máy phát triển vì không có Docker. DB, migration và toàn bộ nghiệp vụ đã được test trên PostgreSQL thật (nhúng).
