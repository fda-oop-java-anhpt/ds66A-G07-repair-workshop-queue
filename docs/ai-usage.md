## AI Usage

Nhóm có sử dụng AI coding tools trong quá trình làm project. Tài liệu này khai
báo công cụ đã dùng, mục đích, các câu hỏi mẫu và phần code hoặc tài liệu có AI
hỗ trợ. Phần chung của nhóm ở ngay dưới; riêng phần Database/Repository có khai
báo chi tiết hơn.

- AI tools used (if any): ChatGPT, GitHub Copilot, Claude (Anthropic)

---

## Khai báo chung của nhóm

- Prompt mẫu:
  - Merge các nhánh giúp tôi sao cho các nhánh vẫn giữ nguyên logic ban đầu.
  - Tôi đang có một dự án có cấu trúc như thế này, hãy chỉ ra những điều chưa
    hợp lí và sửa nếu cần.
- Code nào do AI gợi ý?
  - Code GUI (điểm nối giữa các adapter và các file trong `helpers/` là do AI
    code).
- Code nào tự chỉnh sửa?
  - Sau khi nhận gợi ý từ AI, các thành viên có tự đọc, sửa và ghép vào project
    nhưng không nhớ chi tiết từng chỗ đã sửa. Các thay đổi cụ thể có thể xem
    trong lịch sử commit của Git (`git log`, `git blame`).

---

## Dương Minh Anh – Logic và Adapter

**Công cụ:** Claude (Anthropic), dùng qua giao diện chat. Claude không truy cập repository của nhóm; code được dán vào chat và tự gõ lại vào project.

**Mục đích:** giải thích và hướng dẫn khái niệm, kiến thức liên quan đến phần nhiệm vụ của mình, đối chiếu code với `docs/contract.md` và rà soát lỗi cho phần Domain Model, Service và Adapter.

### Phần có AI hỗ trợ

| Phần                                                         | AI hỗ trợ gì                                                                                                                                                                                 |
| ------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Lộ trình và thứ tự làm                                       | Đề xuất và giải thích kiến thức liên quan, đối chiếu contract với các file cần tạo trong `model`, `exception`, `service`                                                                     |
| `JobStatus`, `EstimateStatus`                                | Giải thích enum và `switch` expression, chỉ ra lỗi tên hằng và điều kiện sai, rà soát `canTransitionTo`, gợi ý `TECHNICIAN_QUEUE`                                                            |
| `Customer`, `Device`, `Part`, `LaborService`, `EstimateItem` | Chỉ ra lỗi (thiếu `return`, validate chưa gọi, `double` đổi sang `BigDecimal`, kiểu `id`, field nullable), gợi ý `normalizeOptional`, đề xuất interface `EstimateItem`, đưa bản `Device` mẫu |
| `RepairJob`, `Estimate`, `StatusHistory`                     | Liệt kê field/hàm cần có, sửa lỗi compile, gợi ý `getDeadline()`, `getTotalCost()`, `normalizeList`                                                                                          |
| Exception, `JobService`                                      | Đưa mẫu `InvalidStatusTransitionException`, `EntityNotFoundException`; giải thích interface/implements, sửa kiểu trả về                                                                      |
| `JobServiceImpl`                                             | Hướng dẫn theo bước, đưa code mẫu cho `validateTransition` và các hàm chính, chỉ ra lỗi trong `rejectEstimate`                                                                               |
| Adapter Warranty                                             | Giải thích Adapter Pattern, gợi ý các bước, tạo `warranty.txt` giả để thử                                                                                                                    |

**Code do Claude đưa ra:** ví dụ ngắn trên chủ đề khác để minh họa cú pháp, vài dòng sửa lỗi, bản `Device` mẫu, mẫu hai exception, code mẫu cho `validateTransition` và các hàm của `JobServiceImpl`.

### Prompt mẫu đã dùng với Claude

- Code này đang báo đỏ ở đâu, sai ở đâu?
- Kiểm tra giúp tôi file này đúng chưa, có khớp `docs/contract.md` không?

### Cách kiểm tra

- Tự biên dịch trong VS Code và chạy thử các ca hợp lệ/không hợp lệ trong `Main.java`.
- Hỏi lại Thành viên 2 về kiểu `id` và quy ước với DAO trước khi chốt.

### Phần tự viết hoặc chỉnh sửa

- Các class model và `exception` do tôi tự gõ, sửa theo các lỗi Claude chỉ ra.
- `validateTransition` và `JobServiceImpl` nhận code mẫu từ Claude, sau đó tôi tự đọc hiểu, gõ lại, sửa message và chỉnh cho khớp repository của Thành viên 2.
- Không nhớ chi tiết từng chỗ đã sửa; xem `git log`.

---

## Hà Thu Hà – Giao diện (UI)

- Công cụ AI: theo khai báo chung của nhóm (ChatGPT, GitHub Copilot); chưa tách
  chi tiết theo từng cá nhân.
- Phần có AI hỗ trợ: code giao diện, đặc biệt điểm nối giữa các adapter và các
  file trong `helpers/`, theo khai báo chung ở trên.
- Phần tự chỉnh sửa: không nhớ chi tiết từng chỗ đã sửa; xem lịch sử commit.

---

## Đinh Vân Anh – Database và Repository

**Công cụ:** Claude (Anthropic), dùng qua giao diện chat.

**Mục đích:** hỗ trợ lập kế hoạch, viết bản đầu và rà soát phần Database,
Repository, tài liệu liên quan và việc gộp nhánh. Claude không truy cập vào
repository hay database của nhóm; mọi file được gửi qua chat (đoạn code, file
zip) và kết quả được chép lại vào project.

### Phần có AI hỗ trợ

| Phần                                                      | AI hỗ trợ gì                                                                                                                                                                                                                       |
| --------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Lộ trình công việc                                        | Chia phần Database/Repository thành các bước (hợp đồng dữ liệu, schema, kết nối, repository, transaction, kiểm thử) và nêu các câu hỏi giảng viên có thể hỏi                                                                       |
| `database/schema.sql`, `database/seed_data.sql` (bản đầu) | Đề xuất cấu trúc 7 bảng đầu tiên, khóa ngoại `ON DELETE RESTRICT`, ràng buộc `CHECK`/`UNIQUE`, index và dữ liệu mẫu theo kịch bản demo                                                                                             |
| Tầng `repository` (bản đầu)                               | Interface, `AbstractJdbcRepository`, `RowMapper` và các mapper, các lớp `...RepositoryImpl`, `JobWrites`, cơ chế `inTransaction`, các hàm `findQueue`, `saveWithInitialHistory`, `updateWithHistory`, `updateEstimateStatusAndJob` |
| Exception                                                 | Gợi ý tổ chức `DomainException`, `DataAccessException` và cách dịch mã lỗi PostgreSQL (SQLState) sang thông báo                                                                                                                    |
| Gộp nhánh Git                                             | Đọc hai bản sao của nhánh `feature/db-jdbc` và `feature/domain-adapter`, chỉ ra file trùng, xung đột và đề xuất file nào giữ, file nào bỏ                                                                                          |
| Tài liệu                                                  | Soạn và cập nhật `docs/contract.md`, `docs/design.md`, `README.md`, `config.md`, `WORKINGFLOW.md`, `docs/requirements.md`, `docs/project-info.md`, và phần Database/Repository trong `docs/reflection.md`                          |
| Làm sạch dữ liệu mẫu                                      | Phát hiện `seed_data.sql` có hai khối seed bị dán nối nhau và đưa ra bản chỉ giữ khối mới                                                                                                                                          |

Claude cũng dựng một trang HTML mô phỏng các exception của project để minh họa.
Trang này không thuộc sản phẩm nộp và không nằm trong repository.

**Những phần Claude không viết:** `DatabaseConnection`, `AppContext`, các bảng
`warranties` và `payments`, `WarrantyRepository`, các ràng buộc bổ sung về sau
(ví dụ số điện thoại 10 chữ số), mã giao diện và logic nghiệp vụ của
`JobServiceImpl`.

### Prompt mẫu đã dùng với Claude

- Thiết kế lại lộ trình từ đầu đến cuối cho phần Database/Repository dựa trên tài
  liệu hướng dẫn của học phần.
- Viết contract giữa ba thành viên cho dữ liệu, repository và service.
- Thêm các hàm `saveWithInitialHistory`, `updateWithHistory`,
  `updateEstimateStatusAndJob` vào các repository.
- Đọc hai file zip của hai nhánh `db-jdbc` và `domain-adapter`, cho biết file nào
  giữ, file nào bỏ, hay merge luôn.
- Dựa vào các thay đổi mới nhất, cập nhật các file `.md` có liên quan.
- Bổ sung các design decisions trong phần của tôi và lý do.

### Cách kiểm tra

- Claude biên dịch mã repository bằng `javac` và kiểm tra hành vi
  commit/rollback của các hàm transaction bằng một `DataSource` giả (trong môi
  trường của Claude). Claude **không** chạy được với PostgreSQL thật.
- Phía nhóm: mã repository được chạy trên PostgreSQL thật thông qua các bài test
  JUnit trong nhánh `feature/db-jdbc` (báo cáo `target/surefire-reports` của bản
  đó ghi 19 test, không có test lỗi). Bản final không còn thư mục `src/test`.

### Phần tự chỉnh sửa

- Sau khi nhận mã và tài liệu từ Claude, đã tự đọc, chỉnh sửa và ghép vào project
  nhưng không nhớ chi tiết từng chỗ đã sửa; xem lịch sử commit của Git để biết
  thay đổi cụ thể.

### Ghi chú

- Nội dung do AI sinh ra có thể sai hoặc không khớp với project, nên được đối
  chiếu với mã nguồn và `schema.sql` trước khi đưa vào repository.
