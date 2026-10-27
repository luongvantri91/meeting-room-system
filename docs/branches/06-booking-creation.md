# Branch 6 – `feature/booking-creation`

**Giai đoạn:** 4 – Triển khai (lát cắt 3, 4) · **Test:** `BookingServiceTest` (7 test đơn vị), `BookingCreateApiTest` (6 test, gồm test đồng thời)

## Mục tiêu

Đặt phòng đúng nghiệp vụ và **không bao giờ cho hai booking trùng giờ** trong cùng một phòng, kể cả khi nhiều người đặt cùng lúc. Đây là phần quan trọng nhất của bài.

## Công việc đã làm

- `Booking` (entity), `BookingRepository`, `BookingService.create`, `BookingController`, DTO, `BookingStatus`.
- `POST /bookings` (`201`): người đặt lấy từ JWT, request không có trường người đặt.
- Các kiểm tra theo thứ tự: giờ kết thúc sau bắt đầu (`400`) → không ở quá khứ (`422`) → thời lượng 15 phút–8 giờ (`422`) → phòng tồn tại (`404`), đang hoạt động (`422`), đủ sức chứa (`422`) → không trùng lịch (`409`).
- Truy vấn trùng lịch theo khoảng nửa mở: `start < endMới AND end > startMới`, chỉ tính booking `CONFIRMED`.
- Ghi bằng `saveAndFlush` trong `try/catch`: nếu request khác thắng cuộc đua, exclusion constraint từ chối và service chuyển thành `409 BOOKING_CONFLICT`.
- `Clock` được inject (`AppConfig`) và `BookingProperties` (`app.booking.min-duration`, `max-duration`) để test và cấu hình được.

## Các quyết định đáng nhớ

| Quyết định | Lý do |
|---|---|
| Hai lớp bảo vệ: kiểm tra ở service + exclusion constraint | Service cho lỗi thân thiện; constraint đảm bảo đúng khi đồng thời |
| `saveAndFlush` rồi mới bắt lỗi | Nếu chỉ `save`, lỗi chỉ phát sinh lúc commit, ngoài `try/catch` nên trả `500` |
| Nhận diện lỗi bằng tên constraint `ex_bookings_no_overlap` trong chuỗi nguyên nhân | Chỉ chuyển đúng lỗi trùng lịch thành `409`, các lỗi khác vẫn là lỗi thật |
| Inject `Clock` | Test được "bây giờ" mà không phụ thuộc giờ máy |
| `422` cho vi phạm nghiệp vụ, `400` cho dữ liệu sai | Phân biệt "không hiểu" và "hiểu nhưng không được phép" |
| Người đặt từ JWT | Client không giả danh người khác được |

## Cách kiểm chứng

- Test đơn vị (Mockito, `Clock` cố định): từng quy tắc bị từ chối đúng mã lỗi; nhận diện lỗi constraint.
- Test API: đặt hợp lệ `201`; các kiểu giao nhau bị `409` nhưng booking nối tiếp được `201`; cùng giờ khác phòng được; lỗi validation và nghiệp vụ; phòng ngừng hoạt động.
- **Test đồng thời:** 12 request cùng lúc đặt một phòng một khung giờ → đúng **1** thành công, 11 `409`. Log của PostgreSQL nhúng sẽ in nhiều lỗi vi phạm constraint, đó là bình thường.

## Câu hỏi NTD có thể hỏi

**Hai người đặt cùng lúc thì chuyện gì xảy ra?**
Cả hai có thể cùng vượt qua bước kiểm tra ở service. Khi ghi xuống DB, exclusion constraint chỉ chấp nhận một, người còn lại bị từ chối và nhận `409`. Test 12 luồng chứng minh điều này.

**Vì sao `@Transactional` một mình không đủ?**
Hai transaction độc lập đều thấy "chưa có booking" ở mức cô lập mặc định rồi cùng `INSERT`. Cần một ràng buộc mà DB cưỡng chế.

**Vì sao không dùng isolation `SERIALIZABLE`?**
Cũng được nhưng phải tự xử lý retry khi lỗi serialization và có chi phí hiệu năng. Constraint đơn giản và rõ ràng hơn.

**Booking 9–10h và 10–11h có bị coi là trùng không?**
Không, vì khoảng nửa mở `[start, end)`. Có test riêng cho trường hợp nối tiếp.

**Làm sao test được những quy tắc phụ thuộc giờ hiện tại?**
Inject `Clock` và dùng `Clock.fixed(...)` trong test đơn vị.

**Nếu muốn đổi giới hạn thời lượng?**
Đổi `app.booking.min-duration` / `max-duration` trong cấu hình, không cần sửa code.
