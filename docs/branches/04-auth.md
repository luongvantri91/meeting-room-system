# Branch 4 – `feature/auth`

**Giai đoạn:** 4 – Triển khai (lát cắt 1) · **Test:** `AuthApiTest` (6 test)

## Mục tiêu

Xác thực và phân quyền: đăng ký, đăng nhập bằng JWT, hai vai trò `USER` / `ADMIN`, và định dạng lỗi thống nhất cho toàn hệ thống.

## Công việc đã làm

- `POST /auth/register` (tạo `USER`), `POST /auth/login` (trả JWT).
- **JWT stateless** (jjwt) với BCrypt cho mật khẩu; token hết hạn sau 8 giờ (`JWT_TTL`).
- `SecurityConfig`: public chỉ gồm register, login, `/actuator/health`; mọi API khác cần token; `401` và `403` trả JSON thống nhất.
- `JwtAuthenticationFilter`: đọc token, tạo `AuthUser(id, email, role)`; token sai coi như chưa đăng nhập.
- `AdminBootstrap`: tạo ADMIN đầu tiên từ `ADMIN_EMAIL` / `ADMIN_PASSWORD` nếu chưa có.
- `ApiException` + `GlobalExceptionHandler`: lỗi RFC 7807 kèm `code`; lỗi validation có thêm `errors`; lỗi lạ trả `500` không lộ chi tiết.
- Sửa `docker-compose.yml` để có `JWT_SECRET` và admin mặc định cho môi trường dev.

## Các quyết định đáng nhớ

| Quyết định | Lý do |
|---|---|
| `JWT_SECRET` bắt buộc, không có mặc định, ≥ 32 byte | Fail-fast, không bao giờ chạy với secret yếu hoặc secret nằm trong code |
| Đăng nhập sai trả cùng một thông báo cho sai email và sai mật khẩu | Không cho kẻ tấn công dò xem email nào đã đăng ký |
| Đăng ký luôn là `USER`; ADMIN chỉ tạo qua biến môi trường | Không ai tự nâng quyền được |
| Mật khẩu 8–72 ký tự | BCrypt chỉ dùng 72 byte đầu |
| Stateless, không session | 200 người dùng, chạy nhiều instance dễ |
| Lỗi có `code` ổn định | Client xử lý theo `code`, không phụ thuộc câu chữ |

## Cách kiểm chứng

Test: đăng ký thành công, trùng email → `409`, validation → `400`, đăng nhập đúng/sai, gọi API thiếu token → `401`.

## Câu hỏi NTD có thể hỏi

**JWT có nhược điểm gì?**
Không thu hồi được token trước khi hết hạn. Giảm thiểu bằng TTL ngắn (8 giờ); nếu cần thu hồi, thêm refresh token hoặc danh sách thu hồi (đã ghi trong hướng mở rộng).

**Vì sao không lưu mật khẩu dạng thường hoặc SHA?**
BCrypt có salt riêng và cố tình chậm, chống dò mật khẩu bằng brute-force.

**Vì sao `401` và `403` khác nhau?**
`401`: chưa xác thực (không có hoặc sai token). `403`: đã xác thực nhưng không đủ quyền.

**Làm sao ngăn người dùng tự đăng ký làm admin?**
Endpoint đăng ký luôn gán `USER` và không nhận trường role. ADMIN được seed từ biến môi trường.

**Secret lưu ở đâu?**
Biến môi trường, không commit. Compose chỉ có giá trị dev và `.env.example` hướng dẫn đổi khi triển khai thật.

**Token chứa gì và có tin được không?**
Chứa id, email, role, được ký bằng secret. Server kiểm tra chữ ký và hạn dùng trước khi tin.
