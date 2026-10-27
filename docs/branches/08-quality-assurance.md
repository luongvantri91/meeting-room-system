# Branch 8 – `test/quality-assurance`

**Giai đoạn:** 5 – Kiểm thử · **Tổng số test sau branch này:** 47

## Mục tiêu

Chứng minh các quy tắc quan trọng thật sự đúng, không chỉ happy path: thêm test ở tầng DB, test bảo mật token, các trường hợp biên của hủy booking, và chạy test tự động trên CI.

## Công việc đã làm

- `BookingConstraintTest` (5 test): ghi **trực tiếp bằng SQL** (bỏ qua ứng dụng) để chứng minh chính DB chặn được:
  - booking `CONFIRMED` giao nhau bị từ chối;
  - booking nối tiếp được chấp nhận;
  - booking đã hủy không giữ chỗ;
  - giờ kết thúc phải sau giờ bắt đầu;
  - sức chứa phòng phải dương.
- `JwtServiceTest` (3 test): token đúng giữ nguyên danh tính và vai trò; token hết hạn, bị sửa hoặc ký bằng khóa khác bị từ chối; secret quá ngắn bị từ chối.
- `BookingServiceTest` thêm 2 test: hủy booking đã bắt đầu (`BOOKING_ALREADY_STARTED`) và người khác hủy (`403`).
- `.github/workflows/ci.yml`: mỗi lần push chạy `mvn -B verify` với JDK 17.

## Các quyết định đáng nhớ

| Quyết định | Lý do |
|---|---|
| Test constraint bằng SQL trực tiếp | Nếu chỉ test qua API thì không biết lớp bảo vệ nào đã chặn; test này chứng minh DB tự chặn |
| Test trên PostgreSQL thật, không mock DB | Mock không chứng minh truy vấn và ràng buộc SQL đúng |
| Kim tự tháp test: nhiều unit test nhanh, ít integration test | Phản hồi nhanh, vẫn có bảo đảm ở tầng thật |
| CI chạy cùng lệnh `mvn verify` với máy dev | Kết quả giống nhau mọi nơi |

## Cách kiểm chứng

```
mvn verify
```

Kết quả: 47 test, không lỗi (Auth 6, Jwt 3, Constraint 5, Create 6, QueryCancel 10, Service 9, Room 7, DB 1).

## Câu hỏi NTD có thể hỏi

**Làm sao bạn biết quy tắc không trùng lịch thật sự đúng?**
Ba lớp bằng chứng: unit test cho logic service, API test cho hành vi từ ngoài, test SQL trực tiếp chứng minh DB tự chặn, cộng test 12 request đồng thời.

**Vì sao test trực tiếp bằng SQL?**
Để biết constraint hoạt động độc lập với code ứng dụng. Nếu có người ghi thẳng vào DB, dữ liệu vẫn không thể sai.

**Unit test và integration test khác nhau thế nào ở đây?**
Unit test (Mockito) kiểm tra logic nghiệp vụ nhanh, không cần DB. Integration test chạy toàn bộ luồng với PostgreSQL thật.

**Có đo độ phủ test không?**
Chưa cấu hình công cụ đo độ phủ. Ưu tiên là phủ các quy tắc nghiệp vụ và biên quan trọng; có thể thêm JaCoCo sau.

**CI để làm gì?**
Mỗi thay đổi tự động được build và test, lỗi bị phát hiện sớm, không phụ thuộc máy của một người.
