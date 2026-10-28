# Branch 14 – `docs/improve-document`

**Giai đoạn:** 7 – Bàn giao (hoàn thiện) · **Mã nguồn:** không đổi

## Mục tiêu

Rà soát toàn bộ dự án trước khi nộp và làm cho người đánh giá đọc là hiểu: README ngắn gọn gây ấn tượng đầu tiên, tài liệu chia tầng từ tổng quan đến chi tiết, không có liên kết hỏng hay thông tin mâu thuẫn.

## Công việc đã làm

- **Đánh số lại tài liệu** theo thứ tự đọc: `01-requirements` → `02-high-level-architecture` → `03-system-design`, rồi `04-api-reference`, `05-project-structure`. Cập nhật mọi liên kết chéo.
- **Thêm kiến trúc tổng quan** ([02](../02-high-level-architecture.md)) với 6 sơ đồ Mermaid: bối cảnh, triển khai, thành phần, luồng đặt phòng, bảo vệ chống trùng lịch hai lớp, luồng xác thực.
- **Tách tài liệu API** ([04](../04-api-reference.md)): endpoint, ví dụ `curl`, mã lỗi chi tiết theo từng `code`.
- **Tách cấu trúc thư mục** ([05](../05-project-structure.md)): cây thư mục, luồng một request qua các tầng, mục lục tài liệu.
- **Viết lại README** theo bố cục: Tính năng chính → Công nghệ → Cài đặt & Chạy thử → Thiết kế (kèm bảng quyết định và lý do) → Giới hạn. Chỉ còn một ví dụ API, phần chi tiết chuyển sang tài liệu riêng.
- **Rà soát liên kết**: sửa liên kết hỏng tới tệp không tồn tại (`personal-idea.md`).
- **Đồng bộ trung thực**: README, handoff và ghi chú branch cùng nêu giới hạn đã biết (`PATCH /rooms/{id}` giảm `capacity`, `docker compose` chưa kiểm chứng trên máy sạch).
- **Cập nhật handoff** ([00](00-handoff.md)): bảng tổng quan, trạng thái, việc còn lại trước khi nộp.

## Các quyết định đáng nhớ

| Quyết định | Lý do |
|---|---|
| README ngắn, chi tiết đẩy sang `docs/` | Người đánh giá đọc README trước; 30 giây đầu phải thấy được giá trị dự án |
| Tài liệu chia tầng 01 → 05 | Đọc từ "cái gì" → "bức tranh chung" → "chi tiết" → "tra cứu" |
| Sơ đồ bằng Mermaid | Nằm ngay trong Markdown, GitHub tự vẽ, sửa bằng văn bản và theo dõi được diff |
| Nêu giới hạn thay vì giấu | Thể hiện hiểu biết về hệ thống; người đánh giá tự phát hiện sẽ mất lòng tin hơn |
| Không đụng vào mã nguồn | Giữ 63 test pass; mọi thay đổi chỉ ở tài liệu |

## Cách kiểm chứng

- Mở README trên GitHub: sơ đồ Mermaid, bảng, badge CI hiển thị đúng.
- Bấm thử mọi liên kết trong README và `docs/`.
- `mvn verify` vẫn pass (mã nguồn không đổi).

## Câu hỏi NTD có thể hỏi

**Vì sao tách kiến trúc tổng quan khỏi thiết kế hệ thống?**
Kiến trúc tổng quan trả lời "hệ thống gồm những gì, nói chuyện với nhau thế nào" cho mọi người đọc. Thiết kế hệ thống đi sâu vào DB, hợp đồng API, mã lỗi cho người phát triển.

**Tài liệu có luôn đúng với mã nguồn không?**
Mã lỗi, endpoint và quy tắc nghiệp vụ trong tài liệu đã được đối chiếu với mã nguồn và bộ Postman. Khi đổi API cần cập nhật cùng lúc, nên lý tưởng là có OpenAPI sinh tự động (đã ghi trong hướng mở rộng).

**Vì sao nêu giới hạn trong README?**
Chọn trung thực hơn là che giấu: người đọc thấy bạn biết hệ thống còn thiếu gì và đã cân nhắc thứ tự ưu tiên.

**Nếu có thêm thời gian, làm gì tiếp?**
Sửa lỗ hổng `capacity`, kiểm chứng `docker compose` trên máy sạch, khóa lạc quan cho cập nhật booking, rồi OpenAPI/Swagger.
