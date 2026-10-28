# 01 – Phân tích yêu cầu

> Tài liệu này mô tả **hệ thống cần làm gì** (what), chưa nói **làm bằng cách nào** (how).
> Cách làm nằm ở [03-system-design.md](03-system-design.md).

## 1. Bối cảnh và mục tiêu

Công ty có khoảng **10 phòng họp** và **200 nhân viên**. Hiện nay nhân viên đặt phòng bằng cách nhắn tin cho nhau, nên thường xảy ra **hai nhóm cùng đặt một phòng vào cùng một giờ**.

**Mục tiêu:** xây dựng backend REST API để
- nhân viên tự đặt phòng và biết ngay phòng nào còn trống;
- hệ thống **không bao giờ** cho phép hai cuộc họp trùng giờ trong cùng một phòng;
- quản trị viên quản lý danh sách phòng.

## 2. Vai trò và quyền hạn

| Chức năng | Chưa đăng nhập | USER (nhân viên) | ADMIN (quản trị) |
|---|:-:|:-:|:-:|
| Đăng ký, đăng nhập | ✅ | ✅ | ✅ |
| Xem danh sách / chi tiết phòng | ❌ | ✅ | ✅ |
| Tạo, sửa, vô hiệu hóa phòng | ❌ | ❌ | ✅ |
| Tìm phòng trống | ❌ | ✅ | ✅ |
| Xem lịch đặt phòng (calendar) | ❌ | ✅ | ✅ |
| Đặt phòng | ❌ | ✅ | ✅ |
| Xem chi tiết một booking (ai đặt, giờ nào, nội dung) | ❌ | ✅ Tất cả | ✅ Tất cả |
| Sửa booking | ❌ | Chỉ booking **của mình** | Tất cả |
| Hủy booking | ❌ | Chỉ booking **của mình** | Tất cả |

- Ai tự đăng ký thì luôn là `USER`. Tài khoản `ADMIN` đầu tiên được tạo sẵn khi hệ thống khởi động (cấu hình bằng biến môi trường).
- Chưa đăng nhập hoặc token sai → `401`. Đăng nhập rồi nhưng không đủ quyền → `403`.

## 3. Phạm vi

| Nhóm | Chức năng | Nguồn |
|---|---|---|
| Auth | Đăng ký, đăng nhập (JWT), vai trò `USER` / `ADMIN` | Đề: "người dùng thường và quản trị" |
| Room | Tạo / xem / sửa / vô hiệu hóa phòng (chỉ ADMIN được ghi) | Đề |
| Booking | Tạo, xem, **sửa**, hủy lịch đặt | Đề (phần sửa: bổ sung theo yêu cầu review) |
| Calendar | Xem lịch theo phòng / khoảng thời gian | Đề |
| Availability | Tìm phòng trống theo khoảng thời gian (và sức chứa) | Đề |
| Deployment | `docker compose up` chạy toàn bộ hệ thống | Đề |

**Ngoài phạm vi** (sẽ ghi rõ trong README): giao diện frontend, gửi email/thông báo, lịch lặp lại (họp hàng tuần), SSO/LDAP, quản lý thiết bị trong phòng.

## 4. Thuật ngữ

| Thuật ngữ | Ý nghĩa |
|---|---|
| **Booking** | Một lượt đặt phòng: *ai* đặt *phòng nào* từ *giờ bắt đầu* đến *giờ kết thúc*. |
| **CONFIRMED** | Booking đang có hiệu lực, **đang giữ chỗ**. |
| **CANCELLED** | Booking đã hủy, **không còn giữ chỗ**; vẫn được lưu lại để xem lịch sử. |
| **Booking cũ / booking mới** | Trong các ví dụ: *booking cũ* là booking đã tồn tại trong hệ thống; *booking mới* là booking đang được gửi lên để đặt. |
| **Khoảng nửa mở `[start, end)`** | Booking chiếm phòng **từ lúc bắt đầu (tính cả) đến lúc kết thúc (không tính)**. Booking 09:00–10:00 chiếm phòng đến 09:59, và **10:00 là lúc phòng đã trống**. |
| **Đã bắt đầu** | Thời điểm hiện tại ≥ giờ bắt đầu của booking. |
| **Chưa kết thúc** | Giờ kết thúc của booking còn ở phía sau thời điểm hiện tại (gồm cả cuộc họp đang diễn ra). |

> **Quy ước giờ trong các ví dụ:** để dễ đọc, ví dụ chỉ ghi giờ (09:00, 10:00…) và hiểu là cùng một ngày, cùng múi giờ. Hệ thống thật dùng UTC, xem BR8.

## 5. Quy tắc nghiệp vụ

Cột **Loại** cho biết quy tắc đến từ đâu: *Từ đề* (đề bài nêu), *Đề xuất* (do người phân tích bổ sung vì cần thiết), *Giả định* (chọn một hướng khi đề chưa nói rõ), *Bảo mật*.

### 5.1. Thời gian và thời lượng

| # | Quy tắc | Ví dụ | Loại |
|---|---|---|---|
| BR1 | Giờ kết thúc phải **sau** giờ bắt đầu. | 10:00 → 09:00 ❌; 10:00 → 10:00 ❌; 09:00 → 10:00 ✅ | Đề xuất |
| BR6 | Không đặt phòng bắt đầu trong **quá khứ**. Bắt đầu đúng thời điểm hiện tại thì được. | Bây giờ là 09:00: đặt 08:30–09:30 ❌; đặt 09:00–10:00 ✅ | Giả định |
| BR7 | Thời lượng từ **15 phút đến 8 giờ** (gồm cả hai mốc), có thể chỉnh bằng cấu hình. Không giới hạn việc họp qua nửa đêm. | 14 phút ❌; 15 phút ✅; 8 giờ ✅; 8 giờ 1 phút ❌ | Giả định |
| BR8 | Hệ thống lưu và xử lý giờ theo **UTC**. API nhận/trả giờ theo ISO-8601 và phải có múi giờ. Việc đổi sang giờ địa phương do client làm. | `2030-01-10T02:00:00Z` ✅ (09:00 giờ Việt Nam); `2030-01-10 09:00` ❌ (thiếu múi giờ) | Giả định |

### 5.2. Phòng

| # | Quy tắc | Ví dụ | Loại |
|---|---|---|---|
| BR2 | Không đặt được phòng đã **vô hiệu hóa**. | Phòng B đã vô hiệu hóa → đặt phòng B bị từ chối. | Đề xuất |
| BR3 | Số người tham dự phải **≤ sức chứa** của phòng. | Phòng A chứa 10 người: đặt cho 10 người ✅; 11 người ❌ | Đề xuất |
| BR11 | Chỉ được vô hiệu hóa phòng khi **không còn booking CONFIRMED chưa kết thúc**. Nếu còn thì từ chối, ADMIN phải hủy các booking đó trước. | Phòng A còn booking ngày mai → vô hiệu hóa bị từ chối (`409`). Hủy booking đó rồi → vô hiệu hóa được. | Giả định |
| BR13 | Tên phòng là **duy nhất**, không phân biệt hoa thường. | Đã có "Phòng A" → tạo "phòng a" bị từ chối (`409`). | Đề xuất |

### 5.3. Chống trùng lịch

| # | Quy tắc | Loại |
|---|---|---|
| BR4 | Hai booking `CONFIRMED` trong **cùng một phòng** không được giao nhau về thời gian. Kết thúc đúng lúc bắt đầu của booking khác là **hợp lệ** (khoảng nửa mở). | Từ đề |
| BR5 | Booking `CANCELLED` không giữ chỗ, nên khung giờ đó đặt lại được. | Đề xuất |
| BR14 | Khi nhiều người **đặt cùng lúc** cùng phòng, cùng khung giờ thì **đúng một người thành công**, những người còn lại nhận lỗi trùng lịch. | Từ đề |

**Ví dụ minh họa BR4** (phòng A đã có booking cũ **09:00–10:00**):

| Booking mới | Quan hệ với booking cũ | Kết quả |
|---|---|---|
| 09:30 – 10:30 | Giao nhau một phần (chồng sang cuối) | ❌ `409` |
| 08:30 – 09:30 | Giao nhau một phần (chồng sang đầu) | ❌ `409` |
| 09:15 – 09:45 | Nằm hoàn toàn trong booking cũ | ❌ `409` |
| 08:00 – 11:00 | Bao trùm booking cũ | ❌ `409` |
| 09:00 – 10:00 | Trùng hệt booking cũ | ❌ `409` |
| 10:00 – 11:00 | **Nối tiếp**: booking mới bắt đầu đúng lúc booking cũ kết thúc | ✅ `201` |
| 08:00 – 09:00 | **Nối tiếp**: booking mới kết thúc đúng lúc booking cũ bắt đầu | ✅ `201` |
| 09:00 – 10:00 ở phòng B | Khác phòng | ✅ `201` |

**Ví dụ minh họa BR5:** Lan đặt phòng A 09:00–10:00, sau đó hủy. Minh đặt phòng A 09:00–10:00 → ✅ thành công vì chỗ của Lan không còn giữ.

**Ví dụ minh họa BR14:** lúc 08:00, An và Bình cùng bấm "đặt" phòng A 14:00–15:00 trong cùng một giây. Kết quả: một người nhận `201`, người kia nhận `409`. Không bao giờ có hai booking cùng tồn tại.

### 5.4. Quyền xem và hủy booking

| # | Quy tắc | Ví dụ | Loại |
|---|---|---|---|
| BR16 | Mọi người đã đăng nhập đều **xem được tất cả booking** (lịch và chi tiết), để biết phòng nào đang bận và chọn khung giờ khác còn trống. | An xem được booking 09:00–10:00 của Bình ở phòng A, rồi chọn đặt 10:00–11:00. | Giả định |
| BR9 | Chỉ **người đặt** mới hủy hoặc sửa được booking của mình. `ADMIN` làm được với tất cả. | An hủy hoặc sửa booking của Bình → `403`. Admin hủy booking của Bình → ✅ | Giả định |
| BR10 | Người đặt được lấy từ **tài khoản đang đăng nhập**, không tin dữ liệu client gửi lên. | An gửi request kèm `"bookedBy": "Bình"` → trường đó bị bỏ qua, booking vẫn ghi là của An. | Bảo mật |
| BR12 | **Hủy booking đã hủy:** không lỗi, trả `200` với trạng thái hiện tại (an toàn khi bấm hai lần).<br>**Hủy booking đã bắt đầu hoặc đã qua:** từ chối `409`. | Booking 09:00–10:00, bây giờ 08:00: hủy ✅. Bây giờ 09:30: hủy ❌ `409`. Hủy lần hai lúc 08:05: `200`. | Giả định |

### 5.5. Sửa booking

Người đặt (hoặc ADMIN) được sửa **phòng, giờ bắt đầu, giờ kết thúc, tiêu đề, số người tham dự**. Chỉ cần gửi những trường muốn đổi, trường không gửi giữ nguyên.

| # | Quy tắc | Ví dụ | Loại |
|---|---|---|---|
| BR17 | Booking sau khi sửa phải thỏa **tất cả quy tắc như khi đặt mới**: giờ hợp lệ (BR1, BR6, BR7), phòng đang hoạt động (BR2), đủ sức chứa (BR3), không trùng lịch (BR4). | Booking phòng A 09:00–10:00, sửa thành 08:00–08:10 → ❌ `422` (dưới 15 phút). Sửa số người thành 11 khi phòng chứa 10 → ❌ `422`. | Giả định |
| BR18 | Khi kiểm tra trùng lịch, **không tính chính booking đang sửa**. | Booking 09:00–10:00, kéo dài thành 09:00–10:30 → ✅ (không coi là trùng với chính nó). Sửa thành 10:00–11:00 khi phòng đã có booking khác 10:30–11:30 → ❌ `409`. | Giả định |
| BR19 | **Booking chưa bắt đầu:** sửa được mọi trường. | Bây giờ 08:00, booking phòng A 09:00–10:00 đổi sang phòng B 14:00–15:00 → ✅ | Giả định |
| BR20 | **Booking đang diễn ra:** chỉ sửa được **giờ kết thúc, tiêu đề, số người**. Không đổi phòng hoặc giờ bắt đầu (`409`). Giờ kết thúc mới vẫn phải ở tương lai. | Bây giờ 09:30, booking 09:00–10:00: kéo dài đến 10:30 ✅; đổi sang phòng B ❌ `409`; đổi giờ bắt đầu thành 09:15 ❌ `409`; rút ngắn đến 09:00 ❌ `422`. | Giả định |
| BR21 | **Booking đã hủy hoặc đã kết thúc:** không sửa được (`409`). Muốn đặt lại thì tạo booking mới. | Booking đã hủy → sửa ❌ `409`. Bây giờ 11:00, booking 09:00–10:00 → sửa ❌ `409`. | Giả định |

### 5.6. Tài khoản

| # | Quy tắc | Ví dụ | Loại |
|---|---|---|---|
| BR15 | Email là **duy nhất**; mật khẩu tối thiểu 8 ký tự. | Đăng ký lại email đã có → `409`. Mật khẩu "abc123" → `400`. | Đề xuất |

## 6. Tiêu chí nghiệm thu

Mỗi dòng là một tình huống kiểm tra được. Nếu tất cả đều đạt, yêu cầu được coi là hoàn thành.

### 6.1. Đăng ký / đăng nhập

| Tình huống | Kết quả |
|---|---|
| Đăng ký email mới, mật khẩu hợp lệ | `201`, vai trò `USER` |
| Đăng ký email đã tồn tại | `409` |
| Đăng ký email sai định dạng hoặc mật khẩu dưới 8 ký tự | `400` |
| Đăng nhập đúng | `200`, nhận token |
| Đăng nhập sai mật khẩu | `401` |
| Gọi API cần đăng nhập mà không có token, hoặc token sai/hết hạn | `401` |

### 6.2. Quản lý phòng

| Tình huống | Kết quả |
|---|---|
| ADMIN tạo phòng hợp lệ (tên, sức chứa ≥ 1) | `201` |
| USER tạo hoặc sửa phòng | `403` |
| Tạo phòng trùng tên (không phân biệt hoa thường) | `409` |
| Tạo phòng sức chứa 0 hoặc âm, hoặc tên rỗng | `400` |
| Xem phòng không tồn tại | `404` |
| ADMIN vô hiệu hóa phòng không còn booking chưa kết thúc | `200` |
| ADMIN vô hiệu hóa phòng còn booking `CONFIRMED` chưa kết thúc | `409` |

### 6.3. Đặt phòng

| Tình huống | Kết quả |
|---|---|
| Đặt hợp lệ: phòng tồn tại, đang hoạt động, khung giờ trống, đủ sức chứa | `201` |
| Khung giờ trùng với booking khác | `409` (xem bảng ở mục 5.3) |
| Nối tiếp ngay sau hoặc ngay trước một booking | `201` (xem bảng ở mục 5.3) |
| Phòng không tồn tại | `404` |
| Phòng đã vô hiệu hóa | `422` |
| Số người tham dự lớn hơn sức chứa | `422` |
| Giờ kết thúc không sau giờ bắt đầu, hoặc thiếu trường, hoặc sai định dạng giờ | `400` |
| Bắt đầu trong quá khứ | `422` |
| Thời lượng dưới 15 phút hoặc trên 8 giờ | `422` |
| Đặt lại đúng khung giờ của một booking đã hủy | `201` |
| 12 request cùng lúc đặt cùng phòng, cùng khung giờ | Đúng **1** `201`, **11** `409` |

### 6.4. Xem, hủy booking

| Tình huống | Kết quả |
|---|---|
| USER hủy booking của mình, trước giờ bắt đầu | `200` |
| USER xem chi tiết booking của người khác | `200` |
| USER hủy booking của người khác | `403` |
| ADMIN hủy booking của người khác | `200` |
| Xem hoặc hủy booking không tồn tại | `404` |
| Hủy booking đã hủy | `200`, trạng thái vẫn `CANCELLED` |
| Hủy booking đã bắt đầu | `409` |

### 6.5. Sửa booking

Giả sử phòng A (sức chứa 10) có booking 09:00–10:00 của An, và booking khác 10:30–11:30 của Bình. Bây giờ là 08:00 (trừ khi ghi khác).

| Tình huống | Kết quả |
|---|---|
| An đổi tiêu đề (chỉ gửi `title`) | `200`, các trường khác giữ nguyên |
| An đổi sang 14:00–15:00 | `200` |
| An đổi sang phòng B cùng giờ | `200`; phòng A trống lại khung 09:00–10:00 |
| An kéo dài sang 09:00–10:30 | `200` (nối tiếp booking của Bình) |
| An kéo dài sang 09:00–11:00 | `409` (trùng booking của Bình) |
| An đổi số người thành 11 | `422` |
| An đổi sang 09:00–09:10 | `422` (dưới 15 phút) |
| An đổi giờ kết thúc sớm hơn giờ bắt đầu | `400` |
| An đổi sang giờ trong quá khứ | `422` |
| An đổi sang phòng đã vô hiệu hóa | `422` |
| An đổi sang phòng không tồn tại | `404` |
| Không gửi trường nào, hoặc tiêu đề rỗng | `400` |
| Bình hoặc USER khác sửa booking của An | `403` |
| ADMIN sửa booking của An | `200` |
| Booking đã hủy | `409` |
| Booking đã kết thúc (bây giờ là 11:00) | `409` |
| Bây giờ 09:30, An kéo dài đến 10:15 | `200` |
| Bây giờ 09:30, An đổi phòng hoặc đổi giờ bắt đầu | `409` |
| Bây giờ 09:30, An rút ngắn giờ kết thúc về 09:00 (đã qua) | `422` |
| Hai người cùng lúc sửa hai booking vào cùng một khung giờ trống | Đúng một người `200`, người kia `409` |

### 6.6. Xem lịch và tìm phòng trống

| Tình huống | Kết quả |
|---|---|
| Xem lịch theo phòng và khoảng thời gian | Trả các booking `CONFIRMED` giao với khoảng đó |
| Xem lịch với tham số `mine=true` | Chỉ trả booking của người đang đăng nhập |
| Xem lịch mặc định | Không gồm booking đã hủy; chỉ gồm khi yêu cầu rõ (`includeCancelled=true`) |
| Khoảng thời gian có giờ kết thúc không sau giờ bắt đầu | `400` |
| Tìm phòng trống 09:00–10:00, phòng A có booking 09:30–10:30 | Phòng A **không** nằm trong kết quả |
| Tìm phòng trống 10:00–11:00, phòng A có booking 09:00–10:00 | Phòng A **có** trong kết quả (nối tiếp) |
| Tìm phòng trống có yêu cầu sức chứa ≥ 12, cả 10 phòng chứa tối đa 10 | Kết quả rỗng |
| Phòng đã vô hiệu hóa | Không bao giờ có trong kết quả tìm phòng trống |
| Danh sách dài | Có phân trang (`page`, `size`, tối đa 100 mục mỗi trang) |

## 7. Bảng tổng hợp mã trạng thái HTTP

| Mã | Ý nghĩa | Khi nào |
|---|---|---|
| `200` | Thành công | Đọc, sửa, hủy booking |
| `201` | Đã tạo mới | Đăng ký, tạo phòng, đặt phòng |
| `400` | Dữ liệu gửi lên sai | Thiếu trường, sai định dạng, giờ kết thúc không sau giờ bắt đầu |
| `401` | Chưa xác thực | Không có token, token sai hoặc hết hạn, sai mật khẩu |
| `403` | Không đủ quyền | USER gọi API của ADMIN, USER hủy booking của người khác |
| `404` | Không tìm thấy | Phòng hoặc booking không tồn tại |
| `409` | Xung đột với trạng thái hiện có | Trùng lịch, trùng tên phòng/email, hủy hoặc sửa booking không còn được phép (đã bắt đầu, đã hủy, đã kết thúc), vô hiệu hóa phòng còn booking |
| `422` | Dữ liệu đúng định dạng nhưng vi phạm nghiệp vụ | Phòng đã vô hiệu hóa, vượt sức chứa, đặt vào quá khứ, thời lượng không hợp lệ |

> Phân biệt `400` và `422`: `400` là yêu cầu **viết sai** (hệ thống không hiểu); `422` là yêu cầu **viết đúng** nhưng **không được phép** theo nghiệp vụ.

## 8. Giả định cần được xác nhận

Đề bài chưa nói rõ các điểm sau, nên tài liệu này tự chọn một hướng:

1. Mọi nhân viên đã đăng nhập đều **xem được mọi booking** (kể cả nội dung cuộc họp và người đặt), nhưng chỉ **hủy** được booking của mình. Nếu có cuộc họp cần giữ kín nội dung, cần bổ sung tính năng "booking riêng tư" (ngoài phạm vi hiện tại).
2. ADMIN cũng có thể đặt phòng như nhân viên thường.
3. Cho **sửa** booking (xem mục 5.5); booking đang diễn ra chỉ được gia hạn/rút ngắn giờ kết thúc, không được dời giờ bắt đầu hay đổi phòng.
4. Không có giới hạn số booking mỗi người, không có giờ làm việc cố định (đặt được cả ban đêm, cuối tuần).
5. Chỉ có một công ty và một tòa nhà; không phân chia chi nhánh.
