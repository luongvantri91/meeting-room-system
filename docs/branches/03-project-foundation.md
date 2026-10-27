# Branch 3 – `chore/project-foundation`

**Giai đoạn:** 3 – Khởi tạo nền tảng · **Commit chính:** bootstrap Spring Boot, Flyway, Docker, test kết nối DB

## Mục tiêu

Dựng khung dự án **chạy được từ đầu** (build, DB, migration, Docker) trước khi viết nghiệp vụ, để lỗi môi trường lộ ra sớm thay vì sát deadline.

## Công việc đã làm

- **Khởi tạo Spring Boot** (`pom.xml`): Java 17, Spring Boot 3.3.5, Maven; Web, JPA, Validation, Actuator, Flyway, PostgreSQL driver; PostgreSQL nhúng cho test.
- **Cấu hình** (`application.yml`): thông tin DB lấy từ biến môi trường; Hikari timeout 5 giây; Hibernate chỉ `validate`; dùng UTC; tắt `open-in-view`; graceful shutdown; không trả stack trace; Actuator chỉ mở `health`.
- **Schema** (`V1__init_schema.sql`): bảng `users`, `rooms`, `bookings`; khóa ngoại, `CHECK`, unique trên `lower(...)`, **exclusion constraint** chống trùng lịch, hai index phụ.
- **Docker** (`Dockerfile`, `docker-compose.yml`): PostgreSQL 16 có healthcheck; app chờ DB sẵn sàng mới chạy; chạy bằng user không phải root; có healthcheck.
- **Test nền**: `AbstractIntegrationTest` khởi động PostgreSQL nhúng; `DatabaseConnectivityTest` xác nhận app kết nối được DB và migration chạy.
- `.gitignore` (loại `target/`, `.env`, file IDE).

## Các quyết định đáng nhớ

| Quyết định | Lý do |
|---|---|
| Flyway + `ddl-auto: validate` | Schema có phiên bản, Hibernate chỉ kiểm tra khớp chứ không tự sửa DB |
| Biến môi trường cho mật khẩu DB | Không commit secret lên Git |
| `open-in-view: false` | Tránh query DB lén lút trong lúc render response, giữ ranh giới transaction rõ |
| `depends_on` + `service_healthy` | App không khởi động khi DB chưa sẵn sàng |
| PostgreSQL nhúng cho test (zonky) | Test trên PG thật mà không cần Docker; chạy được ở máy cá nhân lẫn CI |
| Hikari `connection-timeout` | Lỗi DB báo nhanh thay vì treo request |

## Cách kiểm chứng

```
mvn verify                 # chạy DatabaseConnectivityTest trên PostgreSQL nhúng
docker compose up --build  # chạy toàn bộ hệ thống (chưa được chạy thử trên máy phát triển)
```

## Câu hỏi NTD có thể hỏi

**Vì sao dùng Flyway mà không để Hibernate tự tạo bảng?**
Tự tạo bảng không có lịch sử, không tạo được exclusion constraint, và không an toàn cho production. Flyway cho schema có phiên bản và có thể review như code.

**Vì sao test trên PostgreSQL thật mà không dùng H2?**
Ràng buộc quan trọng nhất (exclusion constraint với `tstzrange`) chỉ có ở PostgreSQL. H2 không chứng minh được nó hoạt động.

**Vì sao không dùng Testcontainers?**
Máy phát triển không có Docker. Dùng PostgreSQL nhúng cho kết quả tương đương, chạy được cả ở CI. Nếu có Docker, có thể chuyển sang Testcontainers mà không đổi test.

**Healthcheck dùng để làm gì?**
DB: app chỉ chạy khi DB sẵn sàng. App: Docker/orchestrator biết khi nào app thật sự phục vụ được.

**Điểm nào còn chưa chắc?**
`docker compose up` chưa được chạy thử trên máy phát triển (không có Docker). Dockerfile giả định image JRE có sẵn `curl` cho healthcheck; cần chạy thử một lần trước khi nộp.
