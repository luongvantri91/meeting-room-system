# Quy trình xây dựng hệ thống meeting room theo tư duy Senior Engineer

1. Requirement Analysis
Làm rõ nghiệp vụ và tiêu chí nghiệm thu

2. System Design
Kiến trúc, database, API contract

3. Project Foundation
Spring Boot, cấu hình, migration, Docker

4. Core Implementation
Phòng họp, đặt phòng, transaction

5. Quality Assurance
Unit test, integration test, edge cases

6. Production Readiness
Bảo mật, logging, cấu hình và vận hành

7. Delivery
EADME, hướng dẫn chạy, quyết định kỹ thuật

Giai đoạn 1 — Phân tích yêu cầu và nghiệp vụ
Trước khi code, cần xác định chính xác hệ thống phải làm gì và đâu là những tình huống có thể gây lỗi trong thực tế.
1.1. Xác định phạm vi chức năng
Với một hệ thống đặt phòng họp tối thiểu, tôi sẽ phân chia như sau:
Nhóm	Chức năng
Room Management	Tạo, xem danh sách, xem chi tiết, cập nhật, vô hiệu hóa phòng
Booking Management	Tạo, xem, hủy lịch đặt phòng
Availability	Kiểm tra phòng còn trống trong một khoảng thời gian
Validation	Kiểm tra thời gian, sức chứa, trạng thái phòng
Error Handling	Trả lỗi nhất quán, HTTP status đúng
Deployment	Khởi chạy ứng dụng và PostgreSQL bằng Docker

Nếu đề bài không yêu cầu đăng nhập, phân quyền hoặc giao diện frontend, tôi sẽ không tự động xây dựng các phần đó. Tuy nhiên, cần ghi rõ các giả định trong README.
1.2. Xác định quy tắc nghiệp vụ
Một Senior cần chủ động tìm ra các quy tắc chưa được nói rõ trong đề.

Ví dụ:
- Thời gian kết thúc phải sau thời gian bắt đầu.
- Không được đặt phòng đã bị vô hiệu hóa.
- Số người tham dự không được vượt quá sức chứa phòng.
- Không cho phép hai booking đang có hiệu lực trùng thời gian trong cùng một phòng.
- Booking đã hủy không còn giữ chỗ.
- Cần xác định có cho phép đặt phòng trong quá khứ hay không.
- Cần xác định múi giờ và cách biểu diễn thời gian.
- Cần xác định người dùng có được hủy booking của người khác hay không, nếu hệ thống có xác thực.
Một điểm quan trọng: không nên tự ý xem tất cả những giả định này là yêu cầu chính thức. Bạn nên ghi rõ quy tắc nào được đề cung cấp và quy tắc nào do mình đề xuất.
1.3. Xác định tiêu chí nghiệm thu
Ví dụ với chức năng đặt phòng:
- Đặt phòng hợp lệ → tạo booking thành công.
- Phòng đã được đặt trong khoảng thời gian giao nhau → từ chối.
- Phòng không tồn tại → trả 404 Not Found.
- Thời gian không hợp lệ → trả 400 Bad Request.
- Hai request đồng thời tranh cùng một khoảng thời gian → không được tạo ra hai booking xung đột.
Đầu ra của giai đoạn 1: một tài liệu ngắn gồm phạm vi, business rules, assumptions và acceptance criteria.

Đây là bước giúp bạn chứng minh năng lực phân tích nghiệp vụ, thay vì chỉ làm theo yêu cầu từng dòng.
Giai đoạn 2 — Thiết kế kiến trúc hệ thống
2.1. Chọn kiến trúc phù hợp
Tôi khuyên dùng Modular Monolith + Layered Architecture cho bài test này.

Lý do: nghiệp vụ nhỏ, chưa có yêu cầu triển khai độc lập nhiều service. Microservices sẽ làm tăng độ phức tạp của giao tiếp, triển khai và xử lý transaction mà chưa đem lại lợi ích rõ ràng.
REST API — Spring MVC

Request mapping · Validation · HTTP response

Application Layer

RoomService · BookingService · Use cases

Persistence Layer

Spring Data JPA · Hibernate · Repositories

PostgreSQL

Constraints · Indexes · Transactions


Luồng xử lý một request:
1. Controller nhận request và kiểm tra định dạng dữ liệu.
2. Service thực hiện nghiệp vụ.
3. Repository truy cập database.
4. PostgreSQL đảm bảo các ràng buộc dữ liệu.
5. Service trả kết quả cho Controller.
6. Controller chuyển kết quả thành HTTP response.
Tương tự hệ thống Golang mà bạn đã quen, Controller tương ứng với Handler, Service tương ứng với lớp nghiệp vụ, Repository tương ứng với lớp truy cập dữ liệu.
2.2. Phân chia trách nhiệm
Thành phần	Trách nhiệm
Controller	HTTP, request/response, status code
DTO	Định dạng dữ liệu đầu vào và đầu ra
Service	Quy tắc nghiệp vụ, transaction
Entity	Mô hình dữ liệu persistence
Repository	Truy vấn dữ liệu
Exception Handler	Chuyển lỗi thành response nhất quán
Migration	Quản lý phiên bản schema database

Không nên đặt toàn bộ nghiệp vụ trong Controller hoặc để Entity được trả trực tiếp ra API mà không cân nhắc cấu trúc response.
2.3. Thiết kế database
Tối thiểu cần hai bảng chính:
rooms
Thông tin phòng họp
Cột	Ý nghĩa
id	Primary key
name	Tên phòng
capacity	Sức chứa, phải lớn hơn 0
location	Vị trí
active	Phòng có thể được đặt hay không
created_at	Thời điểm tạo
updated_at	Thời điểm cập nhật



bookings
Thông tin lịch đặt phòng
Cột	Ý nghĩa
id	Primary key
room_id	Foreign key đến rooms
title	Nội dung cuộc họp
booked_by	Người đặt, nếu có
start_time	Thời gian bắt đầu
end_time	Thời gian kết thúc
status	CONFIRMED, CANCELLED
created_at	Thời điểm tạo



Các quyết định thiết kế quan trọng:
- Dùng khóa ngoại để đảm bảo booking tham chiếu đến phòng hợp lệ.
- Dùng CHECK (capacity > 0) và CHECK (end_time > start_time).
- Có index phục vụ truy vấn booking theo phòng và khoảng thời gian.
- Dùng migration như Flyway để quản lý schema thay vì phụ thuộc hoàn toàn vào Hibernate tự tạo bảng.
- Lưu thời điểm theo một quy ước thống nhất, thường là UTC; chuyển sang múi giờ người dùng ở tầng hiển thị.
Một Senior không chỉ thiết kế bảng để lưu dữ liệu, mà còn cân nhắc làm sao để database bảo vệ tính đúng đắn khi ứng dụng gặp lỗi hoặc nhận nhiều request đồng thời.
2.4. Thiết kế API contract
Tôi sẽ thiết kế API theo tài nguyên, sử dụng HTTP method và status code nhất quán.
Method	Endpoint	Mục đích
POST	/api/v1/rooms	Tạo phòng
GET	/api/v1/rooms	Danh sách phòng
GET	/api/v1/rooms/{id}	Chi tiết phòng
PATCH	/api/v1/rooms/{id}	Cập nhật một phần
POST	/api/v1/bookings	Tạo booking
GET	/api/v1/bookings/{id}	Chi tiết booking
GET	/api/v1/bookings?roomId=1&from=...&to=...	Tra cứu lịch đặt
POST	/api/v1/bookings/{id}/cancel	Hủy booking

Ví dụ request tạo booking:
{
  "roomId": 1,
  "title": "Sprint Planning",
  "bookedBy": "user-123",
  "startTime": "2026-10-28T02:00:00Z",
  "endTime": "2026-10-28T03:00:00Z"
}


Ví dụ response thành công:
{
  "id": 101,
  "roomId": 1,
  "title": "Sprint Planning",
  "status": "CONFIRMED",
  "startTime": "2026-10-28T02:00:00Z",
  "endTime": "2026-10-28T03:00:00Z"
}


Các API thực tế cần bổ sung phân trang cho danh sách lớn, quy định rõ định dạng lỗi, validation và cách xử lý dữ liệu không hợp lệ. bookedBy ở ví dụ trên chỉ là dữ liệu đầu vào minh họa; nếu có authentication thì danh tính người đặt nên được lấy từ principal đã xác thực, không tin giá trị do client tự gửi.

Đầu ra giai đoạn 2: sơ đồ kiến trúc, ERD, danh sách API và quy tắc dữ liệu.

Giai đoạn 3 — Khởi tạo nền tảng project
Chỉ sau khi có thiết kế sơ bộ, bạn mới bắt đầu code.
3.1. Chuẩn bị môi trường
- Java 17 trở lên.
- Maven hoặc Gradle; với bài test, Maven thường dễ bắt đầu.
- Spring Boot 3.
- PostgreSQL.
- Docker và Docker Compose.
- Git.
- Công cụ gọi API như Postman hoặc curl.
Các dependency chính có thể gồm:
- Spring Web
- Spring Data JPA
- Spring Validation
- PostgreSQL Driver
- Flyway
- Spring Boot Test
- Testcontainers, nếu thời gian và môi trường cho phép
3.2. Thiết lập cấu hình
Tạo cấu hình cho datasource, migration và logging; đưa thông tin kết nối nhạy cảm vào environment variables, không commit password thật lên Git.

Tạo Dockerfile và compose.yaml để khởi chạy ứng dụng cùng database. Nên dùng health check cho PostgreSQL và cơ chế retry hợp lý ở ứng dụng nếu cần, thay vì mặc định rằng database luôn sẵn sàng ngay khi container khởi động.
3.3. Thiết lập chất lượng code từ đầu
- Cấu hình formatter hoặc quy ước code rõ ràng.
- Bật compiler và test qua Maven.
- Thêm .gitignore.
- Tạo migration đầu tiên.
- Viết một integration test xác nhận ứng dụng kết nối database được.
Đừng chờ đến cuối mới kiểm tra Docker hoặc cấu hình database. Các lỗi môi trường thường gây mất nhiều thời gian khi gần deadline.

Giai đoạn 4 — Triển khai theo vertical slice
Một cách làm hiệu quả là hoàn thiện từng luồng nghiệp vụ từ API đến database, thay vì viết toàn bộ entity trước rồi mới viết toàn bộ controller.

Tôi đề xuất thứ tự sau:
1. Room Management
   
   Hoàn thành tạo, đọc, cập nhật, vô hiệu hóa phòng. Kiểm tra validation và lỗi phòng không tồn tại.
2. Booking Creation
   
   Hoàn thành tạo booking hợp lệ, kiểm tra phòng tồn tại, trạng thái phòng, thời gian và sức chứa.
3. Conflict Detection
   
   Phát hiện các booking có thời gian giao nhau, xử lý transaction và đảm bảo không có hai booking xung đột cùng được chấp nhận.
4. Booking Query and Cancellation
   
   Tra cứu lịch đặt, lọc theo phòng/thời gian, hủy booking và loại booking đã hủy khỏi kiểm tra xung đột.
5. Error Handling and Consistency
   
   Chuẩn hóa response lỗi, status code, logging và các trường hợp dữ liệu không hợp lệ.
Phần kỹ thuật cần đặc biệt chú ý: race condition
Giả sử hai request cùng lúc muốn đặt phòng A từ 09:00 đến 10:00. Cả hai cùng truy vấn và đều thấy chưa có booking. Nếu chỉ kiểm tra bằng câu SELECT rồi INSERT, cả hai có thể tạo booking thành công.

Đó là race condition.

Đối với bài test, bạn có thể cân nhắc hai phương án:
- Khóa bản ghi phòng trong transaction: sử dụng pessimistic write lock trên bản ghi rooms, sau đó kiểm tra trùng lịch và tạo booking. Những request cùng phòng sẽ được tuần tự hóa nếu tuân thủ cùng cơ chế khóa.
- Ràng buộc loại trừ ở PostgreSQL: sử dụng range và exclusion constraint để ngăn các khoảng thời gian giao nhau ở database. Đây là phương án mạnh, nhưng cấu hình và migration phức tạp hơn.
Dù chọn phương án nào, cần đảm bảo các đường ghi dữ liệu đều tuân thủ cơ chế bảo vệ đó. @Transactional đơn thuần không tự giải quyết được race condition.

Nếu chọn phương án khóa phòng, logic có thể hình dung như sau:
BEGIN TRANSACTION

1. Lock room row
2. Validate room and booking time
3. Query overlapping active bookings
4. If conflict: reject
5. Insert booking

COMMIT


Điểm cần nhớ là thời gian kết thúc của booking cũ bằng thời gian bắt đầu booking mới có thể được phép, nếu quy tắc nghiệp vụ cho phép hai cuộc họp nối tiếp nhau.
Giai đoạn 5 — Kiểm thử và đảm bảo chất lượng
Một hệ thống được đánh giá theo tiêu chuẩn Senior không chỉ cần chạy được happy path.

Bạn nên thiết kế test theo nhiều cấp độ.
5.1. Unit test
Kiểm tra logic nghiệp vụ của BookingService, chẳng hạn:
- Từ chối khi endTime <= startTime.
- Từ chối khi phòng không tồn tại hoặc không hoạt động.
- Từ chối khi lịch bị trùng.
- Cho phép đặt sau khi booking cũ đã hủy.
- Trả đúng lỗi khi hủy booking không tồn tại hoặc đã hủy, theo quy tắc đã chọn.
5.2. Integration test
Dùng PostgreSQL thực tế, chẳng hạn thông qua Testcontainers, để kiểm tra:
- Mapping entity và schema.
- Truy vấn tìm booking giao nhau.
- Transaction và rollback.
- Các constraint trong database.
- Cơ chế khóa hoặc exclusion constraint khi đặt phòng đồng thời.
Không nên chỉ dùng mock cho tất cả test database, vì mock không chứng minh được câu truy vấn và ràng buộc SQL hoạt động đúng.
5.3. API test
Tình huống	Kết quả mong đợi
Tạo phòng hợp lệ	201 Created
Lấy phòng không tồn tại	404 Not Found
Tạo booking hợp lệ	201 Created
Thời gian sai định dạng hoặc không hợp lệ	400 Bad Request
Trùng lịch	409 Conflict
Hủy booking hợp lệ	Thành công theo contract đã thiết kế
Gửi hai request đặt trùng đồng thời	Chỉ một request được chấp nhận

Ngoài ra, nên kiểm tra trường hợp lịch chồng lấn một phần, lịch nằm hoàn toàn bên trong lịch khác và lịch bắt đầu đúng lúc lịch trước kết thúc.
5.4. Kiểm thử thủ công
Dùng Postman hoặc curl để kiểm tra luồng từ đầu đến cuối. Lưu collection hoặc cung cấp các lệnh mẫu trong README để người chấm có thể tái hiện kết quả.

Tiêu chí hoàn thành: không chỉ có test pass, mà còn có bằng chứng rằng những quy tắc nghiệp vụ quan trọng đã được kiểm thử.

Giai đoạn 6 — Đánh giá mức độ sẵn sàng production
Không phải bài test nào cũng yêu cầu triển khai lên cloud. Tuy nhiên, bạn nên xem xét những yếu tố sau và ưu tiên theo phạm vi.
Bảo mật

Không commit secrets; validate input; không trả stack trace cho client; dùng authentication và authorization nếu nằm trong yêu cầu.


Observability

Log lỗi có ngữ cảnh, dùng correlation/request ID nếu phù hợp, cấu hình health endpoint và kiểm tra kết nối database.


Database và hiệu năng

Kiểm tra index bằng truy vấn thực tế, tránh N+1 queries, cấu hình connection pool hợp lý và có timeout phù hợp.


Container và vận hành

Dùng cấu hình theo môi trường, tránh chạy bằng root khi phù hợp, có health check và hướng dẫn khởi chạy, dừng, khôi phục môi trường.



Không cần triển khai tất cả các hệ thống phức tạp như Kubernetes, Redis, Kafka, distributed tracing hoặc microservices cho một bài toán nhỏ nếu không có nhu cầu thực tế. Senior thể hiện năng lực qua việc biết khi nào cần và khi nào không cần thêm công nghệ.
Giai đoạn 7 — Đóng gói và bàn giao
Một bài test tốt nên giúp người đánh giá chạy được hệ thống mà không phải đoán cách cấu hình.

README nên có:
1. Mô tả bài toán và phạm vi chức năng.
2. Sơ đồ kiến trúc và cấu trúc project.
3. Yêu cầu môi trường.
4. Hướng dẫn chạy bằng Docker Compose.
5. Cách cấu hình biến môi trường.
6. Các API và request mẫu.
7. Cách chạy test.
8. Business rules và các giả định.
9. Các quyết định kỹ thuật quan trọng, hạn chế hiện tại và hướng mở rộng.
Trước khi nộp, hãy clone project vào một thư mục sạch hoặc dùng môi trường Docker mới để chạy theo đúng README. Nếu có thể chạy thành công mà không dựa vào trạng thái máy phát triển của bạn, khả năng bàn giao sẽ đáng tin cậy hơn.

Kế hoạch thực hiện thực tế cho bạn
Vì thế mạnh của bạn là Golang chứ không phải Java, tôi khuyên bạn chia thời gian theo mức độ ưu tiên sau.
Ưu tiên	Công việc	Mục tiêu
P0	Phân tích nghiệp vụ, database, API	Không bỏ sót yêu cầu cốt lõi
P0	Spring Boot cơ bản, JPA, validation	Có thể triển khai backend
P0	Đặt phòng, kiểm tra trùng lịch, transaction	Đảm bảo tính đúng đắn
P0	Docker, PostgreSQL, README	Người chấm chạy được hệ thống
P1	Unit test và integration test	Chứng minh chất lượng
P1	Error handling, logging, index	Hoàn thiện tính chuyên nghiệp
P2	Authentication, phân quyền nâng cao	Làm khi đề yêu cầu hoặc còn thời gian
P2	Tối ưu hiệu năng, CI/CD	Mở rộng theo nhu cầu

Checklist trước khi nộp bài
Tiến độ kiểm tra
0/12



Phân tích và thiết kế
[ ] Đã xác định business rules và assumptions
[ ] Đã thiết kế database và API contract
[ ] Đã quyết định cách ngăn đặt phòng trùng lịch

Triển khai
[ ] Các API chính hoạt động đúng
[ ] Có validation và error response nhất quán
[ ] Có transaction và cơ chế chống race condition

Chất lượng
[ ] Có unit test và integration test phù hợp
[ ] Đã kiểm tra các trường hợp biên và booking đồng thời
[ ] Migration và cấu hình môi trường hoạt động

Bàn giao
[ ] Docker Compose chạy được từ môi trường sạch
[ ] README có hướng dẫn chạy và gọi API
[ ] Git không chứa secrets hoặc file build không cần thiết


Quan điểm cuối cùng
Tôi muốn bạn tiếp cận bài test với tư duy của một Senior Engineer đã có kinh nghiệm thực tế, nhưng đồng thời trung thực về công nghệ mình đang học.

Bạn không cần chứng minh mình là chuyên gia Spring Boot ngay lập tức. Bạn cần chứng minh rằng mình biết phân tích yêu cầu, đưa ra lựa chọn kiến trúc hợp lý, kiểm soát các rủi ro về dữ liệu, viết code có cấu trúc, kiểm thử và bàn giao sản phẩm có thể chạy được.

Bước tiếp theo hợp lý nhất: trước khi viết code, hãy tạo một tài liệu thiết kế ngắn cho bài toán này. Sau đó triển khai từng luồng nghiệp vụ theo thứ tự, và dùng AI để hỗ trợ cú pháp Java/Spring Boot trong khi bạn vẫn tự chịu trách nhiệm về các quyết định kỹ thuật.