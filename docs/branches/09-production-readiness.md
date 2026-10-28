# Branch 9 – `chore/production-readiness`

**Giai đoạn:** 6 – Sẵn sàng production · **Test:** `OperationalTest` (4 test) · **Tổng số test:** 51

## Mục tiêu

Làm cho hệ thống dễ vận hành và khó bị lộ thông tin: truy vết từng request, log hợp lý, không lộ chi tiết lỗi, hướng dẫn cấu hình rõ ràng. Không thêm công nghệ thừa (Kubernetes, Redis, Kafka…) vì bài toán nhỏ.

## Công việc đã làm

- `RequestIdFilter`: mỗi request có `X-Request-Id` (nhận từ client nếu hợp lệ, nếu không thì tự sinh UUID), gắn vào log (MDC) và vào header response; ghi một dòng log truy cập gồm method, đường dẫn, mã trả về, thời gian xử lý (bỏ qua `/actuator`).
- Cấu hình log: mẫu log có `requestId`; tắt log SQL trùng lặp cho lỗi trùng lịch (đã được chuyển thành `409`).
- `.env.example`: mẫu biến môi trường (`DB_*`, `JWT_SECRET`, `ADMIN_*`) với hướng dẫn đổi khi triển khai thật.
- `OperationalTest`: có `X-Request-Id`, giá trị an toàn được trả lại và giá trị không an toàn bị thay, health báo `UP`, token sai không lộ chi tiết.

Các hạng mục production đã có ở các branch trước (nhắc lại khi trả lời): secret bắt buộc không mặc định (branch 4), không trả stack trace và chỉ mở `health` (branch 3), Hikari timeout (branch 3), chạy Docker bằng user không phải root (branch 3), lỗi `500` ẩn chi tiết (branch 4).

## Các quyết định đáng nhớ

| Quyết định | Lý do |
|---|---|
| Request ID + MDC | Từ một mã trong response, tìm lại được toàn bộ log của request đó |
| Chỉ nhận request ID khớp `[A-Za-z0-9._-]{1,64}` | Chống chèn ký tự lạ làm giả dòng log (log injection) |
| Không log `/actuator` | Healthcheck gọi liên tục, tránh làm nhiễu log |
| Chỉ mở endpoint `health` | Giảm bề mặt tấn công, không lộ thông tin nội bộ |
| Không thêm Redis/Kafka/tracing phân tán | Bài toán nhỏ; chỉ thêm công nghệ khi có nhu cầu thực tế |

## Cách kiểm chứng

```
mvn verify          # 51 test
curl -i localhost:8080/actuator/health    # thấy header X-Request-Id
```

## Câu hỏi NTD có thể hỏi

**Khi có lỗi trên production, bạn tìm nguyên nhân thế nào?**
Người dùng cung cấp `X-Request-Id` (có trong response), tôi lọc log theo mã đó để thấy toàn bộ luồng và thời gian xử lý của request.

**Vì sao phải kiểm tra định dạng request ID?**
Nếu nhận nguyên giá trị client gửi, kẻ xấu có thể nhét xuống dòng để giả mạo dòng log.

**Bạn đã bảo vệ hệ thống thế nào?**
Secret từ biến môi trường và bắt buộc có; BCrypt; JWT có hạn; phân quyền theo vai trò và theo chủ sở hữu; không trả stack trace; validation đầu vào; chỉ mở `health`; container chạy user thường.

**Hệ thống còn thiếu gì để lên production thật?**
Rate limiting, refresh token, metrics (Prometheus), tracing, quản lý secret (Vault), HTTPS qua reverse proxy, sao lưu DB.

**Vì sao không dùng ELK hay tracing phân tán?**
Một ứng dụng đơn, log có request ID là đủ. Thêm công cụ khi quy mô đòi hỏi.
