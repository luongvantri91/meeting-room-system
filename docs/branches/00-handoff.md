# Ghi chú bàn giao: trạng thái dự án và việc tiếp theo

File này giúp quay lại làm việc ở một máy khác mà không mất ngữ cảnh. Không chứa mật khẩu hay secret.

## 1. Dự án

Backend REST API đặt phòng họp (Java 17, Spring Boot 3.3.5, PostgreSQL 16, Flyway, JWT, Docker).
Repo: `luongvantri91/meeting-room-system`. Đọc theo thứ tự:

1. [README.md](../../README.md): giới thiệu, cách chạy, thiết kế, quyết định kỹ thuật (ngắn gọn).
2. [docs/01-requirements.md](../01-requirements.md): quy tắc nghiệp vụ BR1–BR21 và tiêu chí nghiệm thu.
3. [docs/02-high-level-architecture.md](../02-high-level-architecture.md): kiến trúc tổng quan và sơ đồ.
4. [docs/03-system-design.md](../03-system-design.md): DB, API, mã lỗi, bảng truy vết.
5. [docs/04-api-reference.md](../04-api-reference.md): endpoint, ví dụ `curl`, mã lỗi.
6. [docs/05-project-structure.md](../05-project-structure.md): cấu trúc thư mục, luồng một request, mục lục tài liệu.
7. [docs/manual-testing/](../manual-testing/postman-test-plan.md): Postman collection, kế hoạch test thủ công, bằng chứng.
8. [docs/branches/README.md](README.md): nhiệm vụ từng branch và câu hỏi phỏng vấn.

## 2. Tổng quan trước khi nộp

| Hạng mục | Nội dung | Trạng thái |
|----------|----------|------------|
| Mã nguồn | 4 module (`auth`, `room`, `booking`, `common`), 12 endpoint, 1 migration Flyway | hoàn thành |
| Chống trùng lịch | exclusion constraint PostgreSQL + kiểm tra ở service | hoàn thành, có test đồng thời |
| Test tự động | 63 test (unit, integration, ràng buộc DB, đồng thời), CI GitHub Actions | pass |
| Test thủ công | 141 request Postman, 284 assertion, kèm evidence | 0 lỗi |
| Tài liệu | README + `docs/01`–`05`, ghi chú branch, kế hoạch test thủ công | hoàn thiện, chờ merge |
| Đóng gói | `Dockerfile`, `docker-compose.yml`, `.env.example` | compose chưa kiểm chứng |
| Giới hạn đã biết | giảm `capacity` dưới booking sắp tới; không có khóa lạc quan, refresh token, rate limiting | nêu trong README |

## 3. Trạng thái hiện tại

- 12 branch phát triển đã merge vào `main` bằng Pull Request (#1 → #11 và các merge nội bộ).
- `main` có 63 test tự động, tất cả pass (`mvn verify`, lần chạy gần nhất khi hoàn tất branch 12; chưa chạy lại sau các thay đổi tài liệu, vốn không đụng mã nguồn).
- **Test thủ công đã hoàn thành** (branch `test/postman-manual-testing`): 141 request Postman, 8 nhóm, 284 assertion, 0 lỗi.
  Chạy trên GitHub Codespaces (Postgres bằng Docker, app bằng `mvn spring-boot:run`). Kết quả JSON, ảnh chụp và dữ liệu DB lưu ở [docs/manual-testing/evidence/](../manual-testing/evidence/).
- **Tài liệu đã cải thiện** (branch `docs/improve-document`, đang làm): đánh số lại `01`–`05`, thêm kiến trúc tổng quan (có sơ đồ Mermaid) và tài liệu API, viết lại README ngắn gọn, tách cấu trúc thư mục và API sang tài liệu riêng.
- **Chưa kiểm chứng:** `docker compose up`. Máy phát triển không có Docker; trên Codespaces, app trong compose không kết nối được tới `db` do mạng Docker-in-Docker, nên các lần chạy thủ công dùng cách 2 trong README. Nên chạy một lần trên máy có Docker thật.

### Các branch phát sinh sau 12 branch đầu

| Branch | Mục đích | Trạng thái |
|--------|----------|------------|
| `test/postman-manual-testing` | Postman collection, kế hoạch test và bằng chứng test thủ công ([13](13-manual-testing.md)) | đã đồng bộ vào `main` |
| `docs/improve-document` | đánh số lại tài liệu, thêm kiến trúc tổng quan và tài liệu API, viết lại README, cập nhật handoff ([14](14-improve-document.md)) | đã merge |
| `docs/branch-notes` | chứa các ghi chú nhiệm vụ từng branch ([README.md](README.md), `00`–`14`) | đã có trên GitHub |

## 4. Quy trình làm việc của dự án

Mỗi giai đoạn là một branch: `docs/*`, `chore/*`, `feature/*`, `test/*`, `fix/*`.
Quy trình mỗi branch: checkout branch → merge branch trước để đồng bộ → sửa → push → Pull Request → merge vào `main`.
`main` local chỉ để đồng bộ từ GitHub (`git fetch` rồi `git reset --hard origin/main` nếu lệch).
Commit có dòng cuối `Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>`.

## 5. Chạy lại ở máy mới

Cần: Git, **JDK 17**, **Maven 3.9+** (hoặc Docker Desktop).

```bash
git clone https://github.com/luongvantri91/meeting-room-system.git
cd meeting-room-system
mvn verify                  # không cần Docker, dùng PostgreSQL nhúng
docker compose up --build   # chạy cả hệ thống (cần Docker; chưa kiểm chứng, xem mục 3)
```

Nếu `docker compose` lỗi mạng, dùng cách 2 trong [README.md](../../README.md): Postgres bằng `docker run`, app bằng `mvn spring-boot:run`.
Tài khoản admin mặc định khi dev: `admin@company.local` / `ChangeMe123!` (đổi khi triển khai thật).
Máy cũ đã dùng JDK/Maven tải tạm vào thư mục `%TEMP%\tools`, nên máy mới phải cài lại.

## 6. Việc còn lại trước khi nộp (theo thứ tự ưu tiên)

1. Tạo Pull Request và merge `docs/improve-document` vào `main`; xem README render trên GitHub (sơ đồ Mermaid, bảng, badge CI).
2. Chạy `docker compose up --build` trên máy có Docker thật rồi cập nhật README và mục "Chưa kiểm chứng" ở trên. Nếu không làm được, giữ dòng giới hạn trong README.
3. **Sửa lỗ hổng nhỏ:** `PATCH /rooms/{id}` cho hạ `capacity` thấp hơn số người của booking sắp tới. Đề xuất: từ chối `409` nếu có booking `CONFIRMED` chưa kết thúc vượt sức chứa mới; thêm test; branch `fix/room-capacity-guard`. Nếu không sửa, README đã nêu đây là giới hạn đã biết.
4. Rà soát lần cuối: `mvn verify` xanh trên `main`, CI xanh, evidence không chứa token/mật khẩu, repo không có tệp tạm.
5. Xóa Codespace khi không còn dùng.
6. Tùy chọn: test đồng thời cho `PATCH` booking, khóa lạc quan (`version`) khi hai người cùng sửa một booking, rate limiting đăng nhập, OpenAPI/Swagger.
7. Ôn tập bằng [docs/branches/](README.md) để trả lời người đánh giá.

## 7. Điểm cần nhớ khi trả lời người đánh giá

- Chống trùng lịch bằng **exclusion constraint** của PostgreSQL (`tstzrange '[)'`, chỉ tính `CONFIRMED`), service kiểm tra trước để trả lỗi thân thiện; có test 12 request đồng thời.
- Khoảng thời gian **nửa mở**: 9–10h và 10–11h không xung đột.
- `400` viết sai · `422` vi phạm nghiệp vụ · `409` xung đột trạng thái · `401` chưa đăng nhập · `403` thiếu quyền.
- Mọi người xem được mọi booking; chỉ chủ hoặc ADMIN được sửa và hủy.
- Kiểm chứng hai lớp: 63 test tự động và 141 request Postman (284 assertion, 0 lỗi) kèm bằng chứng. Những ca Postman không làm được (booking đã bắt đầu/kết thúc, đồng thời, token hết hạn) do test tự động đảm nhiệm.
- Các giới hạn đã biết được nêu thẳng trong README.

## 8. Khôi phục ngữ cảnh với trợ lý AI

Lịch sử trò chuyện lưu trên máy cũ và sẽ mất. Ở máy mới, mở repo rồi yêu cầu trợ lý đọc `docs/branches/00-handoff.md`
và `docs/branches/README.md` trước khi làm tiếp.
