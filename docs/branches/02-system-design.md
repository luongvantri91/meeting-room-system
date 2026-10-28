# Branch 2 – `docs/system-design`

**Giai đoạn:** 2 – Thiết kế hệ thống · **Sản phẩm:** [03-system-design.md](../03-system-design.md)

## Mục tiêu

Quyết định **làm bằng cách nào** trước khi code: kiến trúc, database, API, cách chống đặt trùng, cách trả lỗi. Thiết kế phải bám sát requirement.

## Công việc đã làm

- Chọn kiến trúc **modular monolith, phân tầng** `controller → service → repository → PostgreSQL`.
- Thiết kế 3 bảng `users`, `rooms`, `bookings` và các ràng buộc (`CHECK`, unique, exclusion).
- Chọn cơ chế chống trùng lịch: **exclusion constraint của PostgreSQL** + kiểm tra trước ở service.
- Viết API contract (đường dẫn, quyền, mã trả về), định dạng lỗi RFC 7807 với trường `code`.
- Lập **bảng truy vết** BR1–BR21 → nơi thực thi (service hay constraint DB).

## Các quyết định đáng nhớ

| Quyết định | Lý do | Phương án đã loại |
|---|---|---|
| Monolith phân tầng | 10 phòng, 200 người dùng, không cần tách service | Microservices: phức tạp, không có lợi ích rõ |
| Exclusion constraint (`gist`, `tstzrange '[)'`) | DB là chốt chặn cuối, đúng cả khi nhiều request/instance, mọi đường ghi đều được bảo vệ | `SELECT` rồi `INSERT` (race condition); khóa dòng phòng (chỉ đúng nếu mọi đường ghi nhớ lấy lock, và chặn cả các khung giờ khác nhau) |
| Vẫn kiểm tra trước ở service | Trả lỗi `409` thân thiện thay vì lỗi DB | Chỉ dựa vào constraint |
| JWT stateless + BCrypt | Không cần lưu session, chạy đa instance dễ | Session server-side |
| Flyway | Schema có version, kiểm soát được | `ddl-auto=update` |
| `timestamptz` + `Instant` + UTC | Tránh lỗi múi giờ | `LocalDateTime` |
| Unique trên `lower(email)`, `lower(name)` | Không phân biệt hoa thường | Unique thường |

## Cách kiểm chứng

Thiết kế được kiểm chứng bằng test ở các branch sau, đặc biệt test 12 request đồng thời ở branch 6.

## Câu hỏi NTD có thể hỏi

**Vì sao không dùng pessimistic lock?**
Lock hoạt động nhưng chỉ khi *mọi* đường ghi đều nhớ lấy lock, và nó tuần tự hóa mọi request cùng phòng kể cả khung giờ khác nhau. Constraint ở DB đúng bất kể code ứng dụng, và chỉ từ chối khi thực sự giao nhau.

**Nếu chạy nhiều instance của ứng dụng thì sao?**
Vẫn đúng, vì sự thật nằm ở DB chứ không nằm ở bộ nhớ hay lock của một instance.

**Vì sao vẫn cần kiểm tra ở service nếu đã có constraint?**
Để trả lỗi `409 BOOKING_CONFLICT` rõ ràng ngay ở tình huống thường gặp. Constraint chỉ xử lý trường hợp hiếm khi hai request đua nhau.

**Vì sao trả Entity ra API là không tốt?**
Lộ cấu trúc DB, dễ vô tình lộ trường nhạy cảm (ví dụ `password_hash`), và ràng buộc API với schema. Dùng DTO để kiểm soát.

**Vì sao trả lỗi theo RFC 7807?**
Là chuẩn, nhất quán, client xử lý theo `code` thay vì đọc chuỗi thông báo.

**Bảng truy vết để làm gì?**
Cho thấy mỗi quy tắc nghiệp vụ được đảm bảo ở đâu, người khác đọc nhanh biết thiếu hoặc thừa chỗ nào.
