# Branch 5 – `feature/room-management`

**Giai đoạn:** 4 – Triển khai (lát cắt 2) · **Test:** `RoomApiTest` (7 test)

## Mục tiêu

Quản lý danh sách phòng họp: ADMIN tạo, sửa, vô hiệu hóa; mọi người đã đăng nhập xem được.

## Công việc đã làm

- `Room` (entity), `RoomRepository`, `RoomService`, `RoomController`, DTO.
- `POST /rooms` (ADMIN), `GET /rooms` (lọc `active`, `minCapacity`, phân trang), `GET /rooms/{id}`, `PATCH /rooms/{id}` (ADMIN, sửa một phần, gồm `active`).
- Quyền ghi bằng `@PreAuthorize("hasRole('ADMIN')")`.
- Tên phòng được chuẩn hóa (cắt khoảng trắng), **duy nhất không phân biệt hoa thường** (`409 ROOM_NAME_ALREADY_EXISTS`).
- Validation: tên không rỗng, sức chứa ≥ 1.
- `PageResponse` dùng chung cho các danh sách.

## Các quyết định đáng nhớ

| Quyết định | Lý do |
|---|---|
| `PATCH` với trường `null` = giữ nguyên | Client chỉ gửi cái muốn đổi; tránh vô tình ghi đè |
| Vô hiệu hóa (`active=false`) thay vì xóa | Giữ nguyên lịch sử booking, khóa ngoại không bị vỡ |
| Kiểm tra trùng tên ở service **và** unique index ở DB | Thông báo thân thiện + an toàn khi đồng thời |
| `size` tối đa 100 | Không cho client kéo cả bảng trong một request |
| DTO riêng, không trả Entity | Kiểm soát dữ liệu trả ra |

## Cách kiểm chứng

Test: ADMIN tạo và đọc phòng, USER không tạo được (`403`) nhưng đọc được, dữ liệu sai `400`, trùng tên khác hoa thường `409`, phòng không tồn tại `404`, `PATCH` chỉ đổi trường được gửi và vô hiệu hóa được, tham số phân trang sai `400`.

## Câu hỏi NTD có thể hỏi

**Vì sao `PATCH` chứ không `PUT`?**
`PUT` thay thế toàn bộ, buộc client gửi đủ trường. `PATCH` hợp với việc đổi một vài trường như bật/tắt `active`.

**Vì sao không xóa phòng?**
Booking tham chiếu phòng. Xóa làm mất lịch sử hoặc vi phạm khóa ngoại. Vô hiệu hóa giữ được dữ liệu.

**Quyền ADMIN được kiểm tra ở đâu?**
Ở tầng bảo mật (`@PreAuthorize`) trước khi vào nghiệp vụ, nên không thể quên kiểm tra trong từng hàm.

**Hai admin cùng tạo phòng trùng tên cùng lúc thì sao?**
Kiểm tra ở service có thể cùng lọt qua, nhưng unique index trên `lower(name)` từ chối request thứ hai.

**Phân trang để làm gì?**
Dữ liệu tăng lên không làm chậm hoặc làm tràn bộ nhớ; client luôn nhận kích thước có giới hạn.
