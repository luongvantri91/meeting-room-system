# 05 – Cấu trúc thư mục

```
.
├── src/main/java/com/company/meetingroom/
│   ├── auth/      đăng ký, đăng nhập, JWT, SecurityConfig, user
│   ├── room/      Controller · Service · Repository · DTO của phòng
│   ├── booking/   Controller · Service · Repository · DTO của booking
│   └── common/    xử lý lỗi (RFC 7807), RequestIdFilter, cấu hình chung
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/     Flyway (V1__init_schema.sql)
├── src/test/             63 test tự động (unit + integration trên PostgreSQL nhúng)
├── docs/                 tài liệu (xem bên dưới)
├── Dockerfile · docker-compose.yml · .env.example
└── .github/workflows/ci.yml
```

Mỗi module (`auth`, `room`, `booking`) giữ đủ các tầng `controller → service → repository` của riêng nó
(package-by-feature); `common` chứa phần dùng chung.

## Luồng một request

`POST /api/v1/bookings`: `RequestIdFilter` → `JwtAuthenticationFilter` → `BookingController` (validate DTO)
→ `BookingService` (luật nghiệp vụ) → `BookingRepository` → PostgreSQL (exclusion constraint).
Lỗi được `GlobalExceptionHandler` chuyển thành RFC 7807. Sơ đồ chi tiết: [02-high-level-architecture.md](02-high-level-architecture.md).

## Tài liệu

| Tệp | Nội dung |
|-----|----------|
| [01-requirements.md](01-requirements.md) | yêu cầu, luật nghiệp vụ, tiêu chí nghiệm thu |
| [02-high-level-architecture.md](02-high-level-architecture.md) | kiến trúc tổng quan, sơ đồ |
| [03-system-design.md](03-system-design.md) | thiết kế chi tiết: DB, API, lỗi |
| [04-api-reference.md](04-api-reference.md) | endpoint, ví dụ, mã lỗi |
| [manual-testing/](manual-testing/) | Postman collection, kế hoạch test, bằng chứng |
| [branches/](branches/) | ghi chú từng giai đoạn phát triển |
