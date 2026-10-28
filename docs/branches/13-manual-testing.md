# Branch 13 – `test/postman-manual-testing`

**Giai đoạn:** 5 – Kiểm thử (thủ công) · **Test tự động không đổi:** 63

## Mục tiêu

Tự tay chạy API như một người dùng thật (qua HTTP, PostgreSQL thật, token thật) và lưu lại bằng chứng, để bổ sung cho bộ test tự động. Test tự động chứng minh logic; test thủ công chứng minh hệ thống chạy được từ đầu đến cuối.

## Công việc đã làm

- Dựng môi trường: máy phát triển không có Docker, nên dùng **GitHub Codespaces** – Postgres chạy bằng `docker run`, app chạy bằng `mvn spring-boot:run`, mở cổng 8080 (Public) để Postman gọi.
- Viết **Postman collection** [`meeting-room.postman_collection.json`](../manual-testing/meeting-room.postman_collection.json): 141 request chia 8 nhóm. Mỗi request có mã test case (ví dụ `[AUTH-01]`) và script kiểm tra `status`, mã lỗi `code`, dữ liệu trả về; token và id được lưu vào biến để các nhóm nối tiếp nhau.
- Viết [kế hoạch test](../manual-testing/postman-test-plan.md): cách cài đặt, thứ tự chạy, bảng toàn bộ test case, các ca không thể thử bằng Postman, câu SQL để đối chiếu dữ liệu.
- Chạy toàn bộ 8 nhóm và lưu **bằng chứng** vào [`evidence/`](../manual-testing/evidence/): kết quả chạy (JSON), ảnh tổng kết và ảnh dữ liệu DB cho từng nhóm.

## Kết quả

| Nhóm | Request | Assertion đạt |
|---|---:|---:|
| 1. Auth | 15 | 29/29 |
| 2. Rooms | 30 | 60/60 |
| 3. Bookings – tạo | 31 | 63/63 |
| 4. Bookings – xem / hủy | 9 | 18/18 |
| 5. Bookings – sửa | 23 | 48/48 |
| 6. Calendar | 17 | 34/34 |
| 7. Availability | 9 | 19/19 |
| 8. Vô hiệu hóa phòng | 7 | 13/13 |
| **Tổng** | **141** | **284/284** |

## Các quyết định đáng nhớ

| Quyết định | Lý do |
|---|---|
| Mỗi request tự kiểm tra bằng script | Chạy lại được và so kết quả được, không phụ thuộc mắt người |
| Dữ liệu tạo theo `runId` và ngày ngẫu nhiên trong tương lai | Chạy lại nhiều lần không đụng dữ liệu cũ, không báo trùng lịch giả |
| Chạy theo thứ tự nhóm 1 → 8, bật "Persist variables" | Các nhóm phụ thuộc nhau qua biến (token, id phòng, id booking) |
| Giữ cổng Postgres (5432) ở chế độ Private | Cổng database không nên công khai ra Internet |
| Lưu kết quả JSON + ảnh DB làm bằng chứng | Người đánh giá kiểm lại được mà không phải tự chạy |

## Giới hạn

Postman không thử được: booking đã bắt đầu/đã kết thúc (cần thời gian trôi), đồng thời (race condition) và token hết hạn. Các ca này do test tự động (`mvn verify`) đảm nhiệm.

## Câu hỏi NTD có thể hỏi

**Đã có 63 test tự động, vì sao còn test thủ công?**
Test tự động chạy trên PostgreSQL nhúng và MockMvc. Test thủ công đi qua mạng thật, bảo mật thật và dữ liệu thật trong DB, nên bắt được lỗi cấu hình, tích hợp mà test tự động có thể bỏ sót.

**Bằng chứng ở đâu?**
Thư mục `docs/manual-testing/evidence/`: kết quả chạy từng nhóm (JSON), ảnh tổng kết Postman và ảnh bảng DB tương ứng.

**Vì sao chạy trên Codespaces?**
Máy phát triển không cài được Docker. Codespaces cho Java, Maven và Docker sẵn, nên chạy được hệ thống thật mà không cần cài gì.

**Chạy lại bộ test có an toàn không?**
Có, nhờ dữ liệu theo `runId` và ngày ngẫu nhiên. Tuy vậy nên chạy cả bộ từ nhóm 1; chạy riêng nhóm giữa chừng sẽ thiếu biến.

**Test thủ công có thay thế được test tự động không?**
Không. Test thủ công chậm và cần người chạy; nó bổ sung, không thay thế, và không phủ được ca đồng thời hay thời gian.
