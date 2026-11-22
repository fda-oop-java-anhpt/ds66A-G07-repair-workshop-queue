# Repair Workshop Queue

Ứng dụng Java Swing quản lý khách hàng, thiết bị, quy trình sửa chữa, báo giá,
bảo hành và lịch sử trạng thái. Project được biên dịch trực tiếp bằng `javac`,
không yêu cầu Maven.

Xem [WORKINGFLOW.md](./WORKINGFLOW.md) để theo dõi luồng khởi động và các thao
tác trên từng tab của ứng dụng.

## Cấu trúc project

```text
.
├── database/                         # SQL khởi tạo database local (nguồn duy nhất)
│   ├── schema.sql                    # 9 bảng, ràng buộc, index, trigger, view
│   └── seed_data.sql                 # Dữ liệu mẫu cho demo
├── docs/                             # Tài liệu yêu cầu, thiết kế và nhóm
├── lib/                              # PostgreSQL JDBC driver (cần tải riêng)
├── src/com/oop/project/              # Java source root (không dùng Maven)
│   ├── adapter/                      # Đọc và chuyển đổi dữ liệu bảo hành legacy
│   ├── config/                       # DatabaseConnection và AppContext
│   ├── exception/                    # Lỗi nghiệp vụ và lỗi truy cập dữ liệu
│   ├── model/                        # Entity, domain model và enum trạng thái
│   ├── repository/                   # Interface repository
│   │   ├── impl/                     # Cài đặt JDBC và transaction
│   │   └── mapper/                   # Ánh xạ ResultSet sang model
│   ├── sample_data/                  # Dữ liệu mẫu cho giao diện
│   ├── service/                      # JobService: nghiệp vụ job và estimate
│   ├── ui/                           # Màn hình Swing, helpers, theme
│   │   └── Home.java                 # Entry point, mở giao diện chính
│   └── util/                         # Tiện ích dùng chung
├── .env                              # Cấu hình database local (không commit)
├── README.md
├── WORKINGFLOW.md                    # Luồng khởi động và thao tác từng tab
└── config.md                         # Cài đặt, cấu hình và chạy ứng dụng
```

`src/com/oop/project` là source root Java hiện tại. Script SQL chỉ nằm trong
`database/`; không giữ bản sao ở nơi khác để tránh hai bản lệch nhau.

## Tra cứu file (file dictionary)

| File/thư mục | Chức năng |
|---|---|
| `src/com/oop/project/ui/Home.java` | Entry point giao diện; tạo các tab Customers/Devices, Repair Queue, Estimate, Warranty và Job History. Chạy file này để mở ứng dụng. |
| `src/com/oop/project/ui/CustomerDevicePanel.java` | Giao diện quản lý khách hàng, thiết bị và lịch sử repair của khách hàng. |
| `src/com/oop/project/ui/RepairQueue.java` | Danh sách repair queue và thao tác cập nhật trạng thái repair. |
| `src/com/oop/project/ui/Estimate.java` | Tìm repair, lập các phiên bản estimate, quản lý parts/labor và duyệt hoặc từ chối estimate. |
| `src/com/oop/project/ui/Warranty.java` | Tìm/xem warranty, chọn repair đủ điều kiện và import warranty legacy. |
| `src/com/oop/project/ui/JobHistory.java` | Tìm và xem lịch sử thay đổi trạng thái repair. |
| `src/com/oop/project/ui/MainFrame.java` | Frame giao diện Swing dùng chung. |
| `src/com/oop/project/ui/helpers/` | Renderer, editor cho nút trong bảng, table model có thể refresh và hiển thị nội dung nhiều dòng. |
| `src/com/oop/project/ui/theme/AppTheme.java` | Theme và style dùng chung cho giao diện Swing. |
| `src/com/oop/project/config/DatabaseConnection.java` | Đọc cấu hình PostgreSQL từ environment hoặc `.env` và tạo datasource. |
| `src/com/oop/project/config/AppContext.java` | Khởi tạo, kết nối các repository và service được dùng trong ứng dụng. |
| `src/com/oop/project/model/` | Các entity/domain model như Customer, Device, RepairJob, Estimate, Part, LaborService, Warranty và status. |
| `src/com/oop/project/repository/` | Hợp đồng truy cập dữ liệu, các JDBC implementation và row mapper cho PostgreSQL. |
| `src/com/oop/project/service/JobService.java` và `JobServiceImpl.java` | Quy tắc nghiệp vụ cho repair job, cập nhật trạng thái và estimate; `getAvailableNextStatuses` cho giao diện biết các trạng thái được phép chọn. |
| `src/com/oop/project/adapter/WarrantyFileImporter.java` | Đọc file warranty legacy theo định dạng `serialNumber|STATUS|description`, xác thực từng dòng và báo số dòng lỗi. |
| `src/com/oop/project/adapter/WarrantyAdapter.java` và `WarrantyResult.java` | Chuyển bản ghi legacy sang dạng kết quả thống nhất dùng trong màn review import. |
| `src/com/oop/project/adapter/LegacyWarrantyImportAdapter.java` | Chuyển warranty legacy đủ điều kiện thành domain import; bỏ qua trạng thái `NOT_COVERED`. |
| `src/com/oop/project/adapter/` | Adapter và model dữ liệu tương thích cho luồng warranty legacy. |
| `src/com/oop/project/exception/` | Các exception về nghiệp vụ, trạng thái và truy cập dữ liệu. |
| `database/schema.sql` | Tạo lại schema PostgreSQL (9 bảng, ràng buộc, index, trigger `updated_at`, view) cho môi trường local/dev/test. |
| `database/seed_data.sql` | Xóa dữ liệu hiện có và nạp bộ dữ liệu mẫu sau khi tạo schema. |
| `config.md` | Hướng dẫn cài PostgreSQL/JDBC driver, tạo database, cấu hình kết nối, biên dịch, chạy ứng dụng và xử lý lỗi thường gặp. |
| `WORKINGFLOW.md` | Luồng khởi động và thao tác trên từng tab của ứng dụng. |
| `docs/` | Yêu cầu (`requirements.md`), thiết kế (`design.md`), thông tin nhóm, khai báo AI và tự đánh giá. |

## Những chức năng đã triển khai

- **Customers/Devices**: tìm kiếm và quản lý thông tin khách hàng, thiết bị;
  xem lịch sử repair của khách hàng và tạo repair job cho thiết bị. Các trường
  bắt buộc được đánh dấu `*`; loại thiết bị được chọn từ danh sách có sẵn.
- **Repair status**: cập nhật trạng thái từ Repair Queue hoặc lịch sử repair
  trong màn Customer; kiểm tra transition qua service và lưu status history.
  Danh sách trạng thái kế tiếp do `JobService.getAvailableNextStatuses` tính.
  Chuyển sang `WAITING_FOR_APPROVAL` cần có estimate `DRAFT`; chuyển sang
  `REPAIRING` hoặc đóng job từ `WAITING_FOR_APPROVAL` chỉ thực hiện qua
  duyệt/từ chối estimate ở tab Estimate.
- **Repair Queue**: lọc danh sách và xem trạng thái/hạn dự kiến của các repair.
- **Estimate**: tìm repair job theo mã, thiết bị, khách hàng hoặc mô tả; khi
  chọn repair, tự tải estimate mới nhất và cho phép xem các phiên bản đã lưu.
  Nhập Parts/Labor theo tab riêng, theo dõi tổng tiền, rồi lưu thành phiên bản
  draft mới hoặc duyệt/từ chối draft mới nhất. Trường bắt buộc được đánh dấu
  `*`, số tiền và số liệu được hiển thị với dấu phân tách hàng nghìn (ví dụ
  `2,000.00`). Trường nhập kiểm tra định dạng và range trước khi lưu.
- **Quy tắc estimate**: chỉ lập estimate khi repair đang `DIAGNOSING` hoặc
  `WAITING_FOR_APPROVAL`; mỗi lần lưu tạo một phiên bản `DRAFT` mới; mỗi job chỉ
  có tối đa một estimate `APPROVED`.
- **Warranty**: xem/tìm kiếm warranties và repairs đủ điều kiện; tạo warranty
  từ repair được chọn; import dữ liệu legacy (khớp thiết bị theo serial number). Repair đã có warranty không còn là
  candidate để tạo thêm warranty.
- **Job History**: xem và lọc lịch sử chuyển trạng thái.
- **Input validation**: kiểm tra điện thoại khách hàng chỉ gồm chữ số và có
  đúng 10 chữ số (database cũng có ràng buộc `CHECK` tương ứng), kiểm tra định dạng email nếu được nhập; hiển thị lỗi ngay
  dưới trường nhập thay vì popup validation cho biểu mẫu khách hàng. Các lỗi
  nhập ngày Warranty, ngày import, số liệu Estimate và số ngày khi tạo repair
  job cũng hiển thị cạnh trường liên quan.
- **Required fields**: đánh dấu `*` trên các trường bắt buộc trong biểu mẫu
  khách hàng, thiết bị, repair job, estimate và tạo warranty.

## Cơ sở dữ liệu

- **PostgreSQL, 9 bảng:** `customers`, `devices`, `repair_jobs`, `estimates`,
  `parts`, `labor_services`, `status_history`, `warranties`, `payments`.
- **Toàn vẹn dữ liệu:** các khóa ngoại đều `ON DELETE RESTRICT` (không xóa lan
  truyền, giữ lịch sử sửa chữa); ràng buộc `CHECK` cho trạng thái, tiền và số
  lượng, ngày bảo hành, điện thoại 10 chữ số, và `closed_at` chỉ có khi job
  `CLOSED`; mỗi job chỉ có tối đa một estimate `APPROVED`, mỗi repair tối đa
  một bảo hành.
- **Trigger:** `set_updated_at` tự cập nhật `updated_at` cho `customers`,
  `devices`, `repair_jobs`, `estimates` và `warranties`.
- **Truy cập dữ liệu:** JDBC thuần với `PreparedStatement`; kết nối tạo từ
  `DatabaseConnection` (cấu hình bằng `.env` hoặc biến môi trường). Năm thao tác
  chạy trong một transaction (commit hoặc rollback toàn bộ): tạo job kèm lịch
  sử đầu tiên, đổi trạng thái kèm lịch sử, lưu estimate cùng các dòng chi tiết,
  duyệt/từ chối estimate kèm cập nhật job và lịch sử, và import warranty legacy.
- **View:** `v_repair_queue`, `v_warranty_list`, `v_warranty_candidates` có
  trong schema nhưng mã Java hiện truy vấn trực tiếp các bảng.
- **Bảng `payments`:** có trong schema và dữ liệu mẫu nhưng chưa có model,
  repository hay màn hình trong Java, nên chưa có ràng buộc "đóng job phải
  thanh toán đủ".

Chi tiết thiết kế xem [docs/design.md](./docs/design.md).

Tài liệu này hướng dẫn **các bước bắt buộc** để sinh viên tạo repository cho project từ **template chính thức của học phần**, cũng như các **quy định về đặt tên, phân quyền và làm việc nhóm**.

⚠️ Không tuân thủ các bước dưới đây sẽ bị trừ điểm project.

---

## 1. Tạo repository từ template (BẮT BUỘC)

### Bước 1: Sử dụng template
1. Truy cập [Template Project Repository](https://github.com/fda-oop-java-anhpt/template-project)
.
2. Nhấn nút **Use this template**.
3. Chọn:
   - **Owner**: `fda-oop-java-anhpt`.
   - **Repository visibility**: Private.

---

### Bước 2: Đặt tên repository (BẮT BUỘC)

Tên repository phải theo **đúng định dạng sau**:
`<class>-pXX-<project-keyword>`


Trong đó:

| Thành phần | Ý nghĩa | Ví dụ |
|----------|--------|-------|
| `<class>` | Mã lớp <ai66a, ai66b> | `ai66a, ai66b` |
| `pXX` | Mã project (01 → 10) | `p01` |
| `project-keyword` | Tên project viết thường, không dấu, dùng dấu `-` | `yummy-catering` |

### Ví dụ hợp lệ:
```
ai66a-p01-yummy-catering
ai66b-p04-library-system
```

---

## 2. Thêm giảng viên vào repository (BẮT BUỘC)

Sau khi tạo repository, nhóm phải **thêm giảng viên làm collaborator với quyền Admin**.

### Các bước:
1. Vào **Settings** → **Collaborators**.
2. Thêm GitHub username của giảng viên: `anhpt204`
3. Chọn quyền **Admin**.
4. Gửi lời mời và đảm bảo giảng viên đã accept.

⚠️ Không thêm giảng viên = coi như **chưa nộp bài**.

---

## 3. Thêm các thành viên trong nhóm

### Yêu cầu:
- Mỗi sinh viên phải có **tài khoản GitHub cá nhân**.
- Không dùng chung tài khoản GitHub.

### Thực hiện:
1. Vào **Settings** → **Collaborators**.
2. Thêm GitHub username của từng thành viên.
3. Cấp quyền **Write** cho các thành viên.

---

## 4. Khai báo thông tin project và nhóm (BẮT BUỘC)

Sau khi tạo repository, nhóm phải cập nhật đầy đủ các file sau:

### 4.1. `docs/project-info.md`
Cần điền đầy đủ:
- Project ID
- Tên project
- Lớp
- Nhóm
- Danh sách thành viên và vai trò

---

### 4.2. `docs/requirements.md`
- Copy **toàn bộ nội dung đề bài project** được giao.
- Không được để trống file này.

---

## 5. Quy định làm việc với Git (BẮT BUỘC)

### 5.1. Commit
- Mỗi thành viên: **tối thiểu 20 commit**.
- Commit message phải rõ ràng, có ý nghĩa.

Ví dụ commit hợp lệ:
```
Add abstract class User
Implement login validation
Refactor OrderService logic
```

Ví dụ commit KHÔNG chấp nhận:
```
update
fix
done
```


---

### 5.2. Thời gian commit
- Không dồn toàn bộ commit vào 1–2 ngày cuối.
- Mỗi tuần ít nhất 1 commit
- Lịch sử commit sẽ được sử dụng để đánh giá mức độ đóng góp cá nhân.

---

## 6. Sử dụng AI coding tools

Sinh viên **được phép** sử dụng các AI coding tools (ChatGPT, Copilot, v.v.).

### BẮT BUỘC:
- Phải khai báo trung thực trong file: `docs/ai-usage.md`

Nội dung cần ghi:
- AI tool đã sử dụng.
- Prompt mẫu.
- Phần code có AI hỗ trợ.
- Phần code do sinh viên tự viết hoặc chỉnh sửa.

⚠️ Không khai báo AI usage được xem là **vi phạm học thuật**.

---

## 7. Checklist trước khi nộp bài

Trước khi nộp, nhóm phải tự kiểm tra:

- [ ] Repository được tạo từ template chính thức.
- [ ] Tên repository đúng định dạng.
- [ ] Đã thêm giảng viên với quyền Admin.
- [ ] Đã thêm đầy đủ các thành viên trong nhóm.
- [ ] `docs/project-info.md` đã điền đầy đủ.
- [ ] `docs/requirements.md` không để trống.
- [ ] Có commit đều của các thành viên.

---

## 8. Lưu ý quan trọng

- Mỗi nhóm chỉ có **01 repository duy nhất**.
- Không đổi tên repository sau khi đã bắt đầu làm project.
- Mọi chỉnh sửa và trao đổi kỹ thuật phải thể hiện qua **commit trên GitHub**.

- Repository là sản phẩm học tập chính thức của nhóm.  
- Giảng viên sẽ đánh giá dựa trên code, lịch sử commit và khả năng giải thích khi bảo vệ project.  
- Các vấn đề phát sinh do không đọc kỹ README sẽ không được giải quyết ngoại lệ.

## Cài đặt, cấu hình và chạy

Xem [config.md](./config.md) để cài PostgreSQL JDBC driver, tạo database từ SQL,
cấu hình thông tin kết nối, biên dịch toàn bộ source bằng `javac` và chạy giao
diện `Home`.

File legacy bảo hành có một bản ghi mỗi dòng, theo định dạng
`serialNumber|COVERED|description`, `serialNumber|EXPIRED|description` hoặc
`serialNumber|NOT_COVERED|description`; `serialNumber` phải trùng số serial của
một thiết bị trong database. Khi nhập, điền ngày bắt đầu/kết thúc thực tế cho
các bản ghi `COVERED` và `EXPIRED`; `NOT_COVERED` không được lưu thành bảo
hành. Nhập lại cùng file sẽ nhận diện các dòng đã nhập trước đó và bỏ qua.

File mẫu `src/com/oop/project/adapter/warranty.txt` dùng các serial `IP13000`,
`SN123456`, `IP13003` không có trong dữ liệu mẫu, nên khi nhập các dòng này sẽ
không được lưu. Để thử với dữ liệu mẫu, dùng serial có sẵn (`LG-2031`,
`DL-5521`), xem [config.md](./config.md).
