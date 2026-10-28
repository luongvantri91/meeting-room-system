# Ghi chú bàn giao: trạng thái dự án và việc tiếp theo

File này giúp quay lại làm việc ở một máy khác mà không mất ngữ cảnh. Không chứa mật khẩu hay secret.

## 1. Dự án

Backend REST API đặt phòng họp (Java 17, Spring Boot 3.3.5, PostgreSQL 16, Flyway, JWT, Docker).
Repo: `luongvantri91/meeting-room-system`. Đọc theo thứ tự:

1. [README.md](../../README.md): cách chạy, API, quyết định kỹ thuật.
2. [docs/01-requirements.md](../01-requirements.md): quy tắc nghiệp vụ BR1–BR21 và tiêu chí nghiệm thu.
3. [docs/02-system-design.md](../02-system-design.md): kiến trúc, DB, API, mã lỗi, bảng truy vết.
4. [docs/branches/README.md](README.md): nhiệm vụ từng branch và câu hỏi phỏng vấn.

## 2. Trạng thái hiện tại

- 12 branch đã merge vào `main` trên GitHub bằng Pull Request (#1 → #11 và các merge nội bộ).
- `main` có 63 test, tất cả pass (`mvn verify`).
- Đã smoke test file jar với PostgreSQL thật: đăng nhập, quản lý phòng, đặt/sửa/hủy booking, trùng lịch, phân quyền đều đúng.
- **Chưa kiểm chứng:** `docker compose up` (máy phát triển không có Docker). Nên chạy một lần trên máy sạch trước khi nộp.

## 3. Quy trình làm việc của dự án

Mỗi giai đoạn là một branch: `docs/*`, `chore/*`, `feature/*`, `test/*`, `fix/*`.
Quy trình mỗi branch: checkout branch → merge branch trước để đồng bộ → sửa → push → Pull Request → merge vào `main`.
`main` local chỉ để đồng bộ từ GitHub (`git fetch` rồi `git reset --hard origin/main` nếu lệch).
Commit có dòng cuối `Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>`.

## 4. Chạy lại ở máy mới

Cần: Git, **JDK 17**, **Maven 3.9+** (hoặc Docker Desktop).

```bash
git clone https://github.com/luongvantri91/meeting-room-system.git
cd meeting-room-system
mvn verify                  # không cần Docker, dùng PostgreSQL nhúng
docker compose up --build   # chạy cả hệ thống (cần Docker)
```

Tài khoản admin mặc định khi dev: `admin@company.local` / `ChangeMe123!` (đổi khi triển khai thật).
Máy cũ đã dùng JDK/Maven tải tạm vào thư mục `%TEMP%\tools`, nên máy mới phải cài lại.

## 5. Việc còn lại (theo thứ tự ưu tiên)

1. Chạy `docker compose up --build` trên máy có Docker, thử các lệnh `curl` trong README.
2. **Sửa lỗ hổng nhỏ:** `PATCH /rooms/{id}` cho hạ `capacity` thấp hơn số người của booking sắp tới. Đề xuất: từ chối `409` nếu có booking `CONFIRMED` chưa kết thúc vượt sức chứa mới; thêm test; branch `fix/room-capacity-guard`.
3. Cân nhắc: test đồng thời cho `PATCH` booking, khóa lạc quan (`version`) khi hai người cùng sửa một booking, rate limiting đăng nhập, OpenAPI/Swagger.
4. Ôn tập bằng [docs/branches/](README.md) để trả lời nhà tuyển dụng.

## 6. Điểm cần nhớ khi trả lời nhà tuyển dụng

- Chống trùng lịch bằng **exclusion constraint** của PostgreSQL (`tstzrange '[)'`, chỉ tính `CONFIRMED`), service kiểm tra trước để trả lỗi thân thiện; có test 12 request đồng thời.
- Khoảng thời gian **nửa mở**: 9–10h và 10–11h không xung đột.
- `400` viết sai · `422` vi phạm nghiệp vụ · `409` xung đột trạng thái · `401` chưa đăng nhập · `403` thiếu quyền.
- Mọi người xem được mọi booking; chỉ chủ hoặc ADMIN được sửa và hủy.
- Các giới hạn đã biết được nêu thẳng trong README.

## 7. Khôi phục ngữ cảnh với trợ lý AI

Lịch sử trò chuyện lưu trên máy cũ và sẽ mất. Ở máy mới, mở repo rồi yêu cầu trợ lý đọc `docs/branches/00-handoff.md`
và `docs/branches/README.md` trước khi làm tiếp.
