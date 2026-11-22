# Hợp đồng dữ liệu và giao diện lập trình (Contract)

Dự án: **Repair Workshop Queue** (package gốc `com.oop.project`).
Phạm vi: thỏa thuận giữa ba phần của hệ thống: **logic và Adapter** (`model`,
`service`, `adapter`), **giao diện** (`ui`) và **database và repository**
(`database/`, `repository`, `config`).

Tài liệu mô tả **hiện trạng của bản final**, được đối chiếu với mã nguồn và
`database/schema.sql`. Khi thay đổi một mục dưới đây, cần sửa đồng thời mã
nguồn, `schema.sql`, `seed_data.sql` (nếu liên quan), file này, và báo cho các
thành viên còn lại. Các luồng thao tác trên giao diện nằm trong
[WORKINGFLOW.md](../WORKINGFLOW.md); thiết kế tổng quan nằm trong
[design.md](./design.md).

## Mục lục

1. [Công nghệ và môi trường](#1-công-nghệ-và-môi-trường)
2. [Quy ước chung](#2-quy-ước-chung)
3. [Enum](#3-enum)
4. [Thực thể và bảng dữ liệu](#4-thực-thể-và-bảng-dữ-liệu)
5. [Ràng buộc, index, trigger và view trong database](#5-ràng-buộc-index-trigger-và-view-trong-database)
6. [Luật chuyển trạng thái](#6-luật-chuyển-trạng-thái)
7. [Hợp đồng Repository](#7-hợp-đồng-repository)
8. [Hợp đồng Service](#8-hợp-đồng-service)
9. [Transaction](#9-transaction)
10. [Exception](#10-exception)
11. [Bảo hành và import legacy](#11-bảo-hành-và-import-legacy)
12. [Cấu hình và AppContext](#12-cấu-hình-và-appcontext)
13. [Dữ liệu mẫu](#13-dữ-liệu-mẫu)
14. [Giới hạn đã biết](#14-giới-hạn-đã-biết)
15. [Phân công và quy trình thay đổi](#15-phân-công-và-quy-trình-thay-đổi)

---

## 1. Công nghệ và môi trường

| Hạng mục | Giá trị |
|---|---|
| Ngôn ngữ, giao diện | Java, Swing |
| Cơ sở dữ liệu | PostgreSQL, truy cập bằng JDBC thuần |
| Build | `javac` trực tiếp, **không dùng Maven** (xem [config.md](../config.md)) |
| JDBC driver | `lib/postgresql-42.7.14.jar` |
| Entry point | `com.oop.project.ui.Home` |
| Kết nối | `PGSimpleDataSource` (không dùng connection pool) |

**Cấu hình kết nối** đọc từ biến môi trường hoặc file `.env` ở thư mục làm việc
hiện tại (biến môi trường được ưu tiên). Vì `.env` được đọc theo đường dẫn tương
đối, phải chạy ứng dụng từ thư mục gốc project.

| Khóa | Bắt buộc | Mặc định |
|---|---|---|
| `DB_URL` | Không | `jdbc:postgresql://localhost:5432/repair-workshop-queue` |
| `DB_USERNAME` | Có | (không có) |
| `DB_PASSWORD` | Có | (không có) |

`.env` không được commit (đã nằm trong `.gitignore`).

**Dựng database:** chạy `database/schema.sql` rồi `database/seed_data.sql`. Cả
hai file xóa dữ liệu cũ nên chỉ chạy trên database dev/test.

## 2. Quy ước chung

| Chủ đề | Quy ước |
|---|---|
| Đặt tên | Java dùng `camelCase`; database dùng `snake_case`, tên bảng ở dạng số nhiều |
| Khóa chính | `SERIAL` ở database, `int` ở Java, tên cột `id` |
| Khóa ngoại | `<bảng_số_ít>_id`, ví dụ `customer_id` |
| Tiền, số giờ | `NUMERIC(12,2)` (số giờ `NUMERIC(6,2)`) ở database, `BigDecimal` ở Java; không dùng `double`/`float` |
| Thời gian | `TIMESTAMP` ↔ `LocalDateTime`; ngày bảo hành `DATE` ↔ `LocalDate` |
| Trạng thái | Lưu `VARCHAR` + `CHECK`; giá trị trùng tên hằng enum trong Java (`name()`) |
| Xóa dữ liệu | Mọi khóa ngoại dùng `ON DELETE RESTRICT`, không xóa lan truyền |
| Giá trị rỗng | Chuỗi rỗng ở field tùy chọn được chuẩn hóa thành `null` |
| Ngôn ngữ thông báo | **Chưa thống nhất:** model và service dùng tiếng Anh, repository dùng tiếng Việt (xem mục 14) |
| Trả về | `findById` trả `Optional`; các hàm `find...` trả danh sách rỗng (không bao giờ `null`) khi không có dữ liệu |

## 3. Enum

### 3.1. `JobStatus` (trạng thái repair job)

| Giá trị | Hiển thị |
|---|---|
| `RECEIVED` | Received |
| `DIAGNOSING` | Diagnosing |
| `WAITING_FOR_APPROVAL` | Waiting for Approval |
| `REPAIRING` | Repairing |
| `READY` | Ready |
| `CLOSED` | Closed |

- `canTransitionTo(next)` trả `true` cho các bước liền kề: `RECEIVED → DIAGNOSING
  → WAITING_FOR_APPROVAL → REPAIRING → READY → CLOSED`, cộng thêm
  `WAITING_FOR_APPROVAL → CLOSED`. `CLOSED` không chuyển tiếp được.
- `JobStatus.TECHNICIAN_QUEUE` = {`RECEIVED`, `DIAGNOSING`, `REPAIRING`}: các
  trạng thái "việc của thợ".

### 3.2. `EstimateStatus`

`DRAFT` (Draft), `APPROVED` (Approved), `REJECTED` (Rejected).

### 3.3. `WarrantyStatus` (dòng trong file legacy)

`COVERED`, `EXPIRED`, `NOT_COVERED`. Không có bảng tương ứng trong database.

## 4. Thực thể và bảng dữ liệu

Các model dùng constructor có kiểm tra dữ liệu (ném `DomainException`). Chỉ có
setter cho các field do hệ thống gán: `id`, `createdAt`, `updatedAt`, và (tùy
lớp) `status`, `version`, `estimatedReadyAt`, `closedAt`. Mỗi lớp có hai
constructor: một dạng "tạo mới" (không có id, thời gian) và một dạng đầy đủ dùng
khi đọc từ database.

### 4.1. `Customer` ↔ `customers`

| Field Java | Kiểu | Cột | Bắt buộc | Kiểm tra |
|---|---|---|---|---|
| `id` | `int` | `id` | tự sinh | |
| `fullName` | `String` | `full_name` `VARCHAR(100)` | có | không rỗng |
| `phone` | `String` | `phone` `VARCHAR(20)` | có | đúng 10 chữ số (model và `CHECK` của database) |
| `email` | `String` | `email` `VARCHAR(100)` | không | |
| `address` | `String` | `address` `VARCHAR(255)` | không | |
| `createdAt`, `updatedAt` | `LocalDateTime` | `created_at`, `updated_at` | tự sinh | |

### 4.2. `Device` ↔ `devices`

| Field Java | Kiểu | Cột | Bắt buộc | Kiểm tra |
|---|---|---|---|---|
| `id` | `int` | `id` | tự sinh | |
| `customer` | `Customer` | `customer_id` (FK → `customers`) | có | không `null` |
| `deviceType` | `String` | `device_type` `VARCHAR(50)` | có | không rỗng |
| `brand`, `model` | `String` | `brand`, `model` `VARCHAR(50)` | không | |
| `serialNumber` | `String` | `serial_number` `VARCHAR(50)` | không | dùng để khớp bảo hành legacy |
| `createdAt`, `updatedAt` | `LocalDateTime` | | tự sinh | |

### 4.3. `RepairJob` ↔ `repair_jobs`

| Field Java | Kiểu | Cột | Bắt buộc | Kiểm tra |
|---|---|---|---|---|
| `id` | `int` | `id` | tự sinh | |
| `device` | `Device` | `device_id` (FK → `devices`) | có | không `null` |
| `issueDescription` | `String` | `issue_description` `TEXT` | có | không rỗng |
| `status` | `JobStatus` | `status` `VARCHAR(30)` | có | mặc định `RECEIVED` |
| `desiredDays` | `int` | `desired_days` | có | `> 0` (model và `CHECK`) |
| `estimatedReadyAt` | `LocalDateTime` | `estimated_ready_at` | không | do nhân viên nhập, không ràng buộc với hạn chót |
| `closedAt` | `LocalDateTime` | `closed_at` | không | có giá trị khi và chỉ khi `status = CLOSED` (`CHECK`) |
| `createdAt`, `updatedAt` | `LocalDateTime` | | tự sinh | |

`getDeadline()` = `createdAt + desiredDays` ngày; **không lưu ở database**, và
ném `DomainException` nếu đơn chưa có `createdAt` (chưa được lưu).

### 4.4. `Estimate` ↔ `estimates`

| Field Java | Kiểu | Cột | Bắt buộc | Kiểm tra |
|---|---|---|---|---|
| `id` | `int` | `id` | tự sinh | |
| `jobId` | `int` | `job_id` (FK → `repair_jobs`) | có | `> 0` |
| `version` | `int` | `version` | có | `>= 1`; repository luôn gán lại = số lớn nhất của job + 1 |
| `status` | `EstimateStatus` | `status` `VARCHAR(20)` | có | mặc định `DRAFT` |
| `note` | `String` | `note` `TEXT` | không | |
| `parts` | `List<Part>` | bảng `parts` | | getter trả danh sách **chỉ đọc**; thêm bằng `addPart` |
| `laborServices` | `List<LaborService>` | bảng `labor_services` | | thêm bằng `addLaborService` |
| `createdAt`, `updatedAt` | `LocalDateTime` | | tự sinh | |

`getTotalCost()` = tổng `getCost()` của mọi part và labor service; **không lưu ở
database**. Các hàm `isDraft()`, `isApproved()`, `isRejected()` để kiểm tra
trạng thái.

### 4.5. `EstimateItem`, `Part`, `LaborService`

`EstimateItem` là interface chung gồm `getName(): String` và
`getCost(): BigDecimal`. Chỉ là hàm tính toán, không có cột tương ứng.

**`Part` ↔ `parts`** (cài đặt `EstimateItem`)

| Field | Kiểu | Cột | Kiểm tra |
|---|---|---|---|
| `id`, `estimateId` | `int` | `id`, `estimate_id` (FK → `estimates`) | |
| `name` | `String` | `name` `VARCHAR(100)` | không rỗng; `getName()` trả field này |
| `unitPrice` | `BigDecimal` | `unit_price` `NUMERIC(12,2)` | `>= 0` |
| `quantity` | `int` | `quantity` | `> 0` |

`getCost()` = `unitPrice × quantity`.

**`LaborService` ↔ `labor_services`** (cài đặt `EstimateItem`)

| Field | Kiểu | Cột | Kiểm tra |
|---|---|---|---|
| `id`, `estimateId` | `int` | `id`, `estimate_id` (FK → `estimates`) | |
| `description` | `String` | `description` `VARCHAR(200)` | không rỗng; `getName()` trả field này |
| `hours` | `BigDecimal` | `hours` `NUMERIC(6,2)` | `> 0` |
| `hourlyRate` | `BigDecimal` | `hourly_rate` `NUMERIC(12,2)` | `>= 0` |

`getCost()` = `hours × hourlyRate`.

### 4.6. `StatusHistory` ↔ `status_history`

| Field | Kiểu | Cột | Bắt buộc | Ghi chú |
|---|---|---|---|---|
| `id` | `int` | `id` | tự sinh | |
| `jobId` | `int` | `job_id` (FK → `repair_jobs`) | có | `> 0` |
| `oldStatus` | `JobStatus` | `old_status` | không | `null` ở dòng đầu tiên (lúc tạo đơn) |
| `newStatus` | `JobStatus` | `new_status` | có | |
| `note` | `String` | `note` `TEXT` | không | |
| `changedAt` | `LocalDateTime` | `changed_at` | tự sinh | |

### 4.7. Model chỉ để hiển thị (không có bảng riêng)

| Lớp | Nguồn | Field |
|---|---|---|
| `WarrantyRecord` | `warranties` ghép `devices`, `customers` | `warrantyId`, `repairId` (`Integer`, có thể `null`), `serialNumber`, `deviceName`, `customerName`, `startDate`, `endDate`, `source` (`NATIVE`/`LEGACY`), `status` (`VALID`/`EXPIRED`, tính theo `end_date >= hôm nay`) |
| `WarrantyCandidate` | `repair_jobs` đã `READY`/`CLOSED` và chưa có bảo hành | `repairId`, `serialNumber`, `deviceName`, `customerName`, `receivedAt`, `completedAt` (lần chuyển sang `READY` gần nhất, có thể `null`) |
| `LegacyWarrantyImport` | một dòng legacy đã sẵn sàng để lưu | `serialNumber`, `startDate`, `endDate`, `legacyRef`, `note` |

`deviceName` = `brand model` nếu có, ngược lại là `device_type`.

### 4.8. Bảng `payments` (chưa có model)

| Cột | Kiểu | Kiểm tra |
|---|---|---|
| `id` | `SERIAL` | |
| `estimate_id` | `INT` FK → `estimates` | |
| `amount` | `NUMERIC(12,2)` | `> 0` |
| `method` | `VARCHAR(20)`, mặc định `CASH` | `CASH`, `CARD`, `BANK_TRANSFER`, `OTHER` |
| `paid_at` | `TIMESTAMP` | mặc định hiện tại |
| `note` | `TEXT` | không bắt buộc |

Bảng có trong schema và dữ liệu mẫu nhưng **chưa có model, repository hay màn
hình** trong Java (xem mục 14).

### 4.9. Field cho phép `null`

| Lớp | Field |
|---|---|
| `Customer` | `email`, `address` |
| `Device` | `brand`, `model`, `serialNumber` |
| `RepairJob` | `estimatedReadyAt`, `closedAt` |
| `Estimate` | `note` |
| `StatusHistory` | `oldStatus`, `note` |
| `WarrantyRecord` | `repairId` |
| `WarrantyCandidate` | `completedAt` |

Mọi field còn lại là bắt buộc.

## 5. Ràng buộc, index, trigger và view trong database

**Ràng buộc đáng chú ý** (ngoài các `NOT NULL`, `CHECK` đã nêu ở mục 4):

| Bảng | Ràng buộc |
|---|---|
| `estimates` | `UNIQUE (job_id, version)`; `status` thuộc `DRAFT`/`APPROVED`/`REJECTED`; mỗi job tối đa **một** estimate `APPROVED` (index duy nhất `uq_estimates_one_approved_per_job` có điều kiện) |
| `repair_jobs` | `status` thuộc 6 trạng thái; `desired_days > 0`; `closed_at` khớp trạng thái `CLOSED` |
| `status_history` | `old_status` (nếu có) và `new_status` thuộc 6 trạng thái |
| `warranties` | `end_date >= start_date`; `source` thuộc `NATIVE`/`LEGACY`; bảo hành `NATIVE` bắt buộc có `repair_job_id`; `legacy_ref` chỉ dùng cho `LEGACY`; mỗi `repair_job_id` tối đa một bảo hành (`uq_warranties_repair_job`); `legacy_ref` duy nhất (`uq_warranties_legacy_ref`) |

**Quan hệ:**

```
customers 1 ─── N devices 1 ─── N repair_jobs 1 ─── N estimates 1 ─┬─ N parts
                    │                  │                           ├─ N labor_services
                    │                  ├─── N status_history       └─ N payments
                    └─── N warranties ─┘ (repair_job_id có thể NULL với LEGACY)
```

**Index:** `idx_devices_customer_id`, `idx_repair_jobs_status`,
`idx_repair_jobs_device_id`, `idx_repair_jobs_created_at`, `idx_parts_estimate_id`,
`idx_labor_services_estimate_id`, `idx_status_history_job_id`,
`idx_status_history_changed_at`, `idx_customers_phone`, `idx_payments_estimate_id`,
`idx_warranties_device_id`, `idx_warranties_end_date`, cùng ba index duy nhất ở
trên.

**Trigger:** hàm `set_updated_at()` gắn vào `customers`, `devices`,
`repair_jobs`, `estimates`, `warranties` (`BEFORE UPDATE`). Vì vậy code Java
không cần tự đặt `updated_at`.

**View:** `v_repair_queue`, `v_warranty_list`, `v_warranty_candidates` có trong
schema nhưng **mã Java hiện truy vấn trực tiếp các bảng**, không dùng view.

## 6. Luật chuyển trạng thái

**Chuyển trạng thái hợp lệ** (định nghĩa ở `JobStatus.canTransitionTo`):

| Từ | Sang | Điều kiện bổ sung (do `JobServiceImpl` kiểm tra) |
|---|---|---|
| `RECEIVED` | `DIAGNOSING` | không |
| `DIAGNOSING` | `WAITING_FOR_APPROVAL` | phải có estimate `DRAFT` mới nhất |
| `WAITING_FOR_APPROVAL` | `REPAIRING` | **chỉ** qua `approveEstimate` (không đi qua `updateJobStatusWithHistory`) |
| `WAITING_FOR_APPROVAL` | `CLOSED` | **chỉ** qua `rejectEstimate` |
| `REPAIRING` | `READY` | không |
| `READY` | `CLOSED` | không; `closedAt` được gán lúc đóng |

Các quy tắc khác:

- Job mới luôn bắt đầu ở `RECEIVED` và có một dòng lịch sử đầu tiên
  (`oldStatus = null`, `newStatus = RECEIVED`).
- Chỉ lập estimate khi job đang `DIAGNOSING` hoặc `WAITING_FOR_APPROVAL`.
- Chỉ estimate **mới nhất** của job và đang `DRAFT` mới được duyệt hoặc từ chối.
- Duyệt: estimate `APPROVED`, job sang `REPAIRING`. Từ chối: estimate
  `REJECTED`, job sang `CLOSED` (chỉ từ `WAITING_FOR_APPROVAL`).
- Mọi lần đổi trạng thái ghi một dòng `status_history`.
- Luật thanh toán (chặn đóng job khi chưa trả đủ) **chưa được triển khai**.

| Việc | Do ai |
|---|---|
| Kiểm tra luật, ném `InvalidStatusTransitionException` | `JobServiceImpl` (và `JobStatus.canTransitionTo`) |
| Lưu trạng thái mới và lịch sử trong một transaction | Repository |
| Hiển thị lỗi và các nút trạng thái | Giao diện |

## 7. Hợp đồng Repository

Package `com.oop.project.repository`. Cài đặt JDBC ở `repository.impl`, ánh xạ
`ResultSet` ở `repository.mapper`. Mọi truy vấn dùng `PreparedStatement`.

**Quy ước chung:**

- `save` trả về entity đã có `id` (và thời gian do database sinh).
- `update` và `delete` ném `EntityNotFoundException` nếu không có dòng nào bị
  tác động.
- Vi phạm ràng buộc database (khóa ngoại, trùng khóa, thiếu dữ liệu, `CHECK`)
  hoặc lỗi kết nối được đổi thành `DataAccessException` kèm thông báo.
- Đầu vào sai (ví dụ đơn chưa lưu) bị từ chối bằng `IllegalArgumentException`
  trước khi mở transaction.

### 7.1. `GenericRepository<T, ID>`

`Optional<T> findById(ID)`, `List<T> findAll()`, `T save(T)`, `void update(T)`,
`void delete(ID)`.

### 7.2. Từng repository

| Repository | Hàm | Ghi chú |
|---|---|---|
| `CustomerRepository` (kế thừa Generic, `Integer`) | `findByPhone(String)`, `searchByName(String)` | Tìm tên không phân biệt hoa thường; từ khóa trống trả toàn bộ danh sách |
| `DeviceRepository` (kế thừa Generic) | `findByCustomerId(int)` | `Device` luôn nạp kèm `Customer` |
| `RepairJobRepository` (kế thừa Generic) | `findByStatus`, `findByDeviceId`, `findByCustomerId` | Sắp xếp theo `id`; `RepairJob` luôn nạp kèm `Device` và `Customer` bằng một câu `JOIN` |
| | `findQueue()` | Mọi đơn chưa `CLOSED` |
| | `findQueue(Collection<JobStatus>)` | Chỉ các trạng thái được truyền (bỏ qua `CLOSED`); tập rỗng hoặc `null` trả danh sách rỗng |
| | `saveWithInitialHistory(job, note)` | Một transaction: ghi đơn và dòng lịch sử đầu tiên. Đơn mới phải có status `RECEIVED` (hoặc `null`) |
| | `updateWithHistory(job, history)` | Một transaction: chỉ ghi `status`, `closed_at` của đơn rồi ghi lịch sử. `history.newStatus` phải khớp `job.status`; đơn `CLOSED` phải có `closedAt`, đơn chưa `CLOSED` phải để `closedAt = null` |
| `EstimateRepository` | `save(Estimate)` | Một transaction: khóa dòng job (`FOR UPDATE`), gán `version` = lớn nhất + 1 (ghi đè version đang có), ghi header, `parts`, `labor_services`; trả về estimate có id |
| | `findById(int)`, `findByJobId(int)`, `findLatestByJobId(int)` | Luôn nạp kèm parts và labor; `findByJobId` sắp xếp version tăng dần |
| | `updateStatus(int, EstimateStatus)` | Đổi trạng thái đơn lẻ |
| | `updateEstimateStatusAndJob(id, status, job, history)` | Một transaction: đổi estimate, cập nhật `status`/`closed_at` của job, ghi lịch sử. Estimate phải thuộc đúng job được truyền vào, nếu không ném `EntityNotFoundException` |
| `StatusHistoryRepository` | `findAll()`, `findByJobId(int)` | Sắp xếp `changed_at`, rồi `id`, tăng dần |
| `WarrantyRepository` | `findAll()` | Sắp xếp `end_date` giảm dần |
| | `findCandidates()` | Repair `READY`/`CLOSED` chưa có bảo hành; mới hoàn tất trước |
| | `createForRepair(repairId, start, end)` | Tạo bảo hành `NATIVE`; kiểm tra `end >= start`; không đủ điều kiện hoặc đã có bảo hành thì ném `DataAccessException` |
| | `int importLegacy(List<LegacyWarrantyImport>)` | Một transaction; trả về số dòng đã lưu (xem mục 11) |

## 8. Hợp đồng Service

`JobService` (`JobServiceImpl`) là điểm vào cho mọi thao tác có luật nghiệp vụ
hoặc cần nhiều bước. `JobServiceImpl` chỉ cần ba repository: thiết bị, job và
estimate.

| Hàm | Hành vi | Lỗi có thể ném |
|---|---|---|
| `RepairJob createJob(deviceId, issueDescription, desiredDays)` | Tạo đơn `RECEIVED` kèm lịch sử đầu (`saveWithInitialHistory`) | `EntityNotFoundException` (thiết bị không tồn tại); `DomainException` (mô tả rỗng, `desiredDays <= 0`) |
| `void updateJobStatusWithHistory(jobId, newStatus, note)` | Kiểm tra luật mục 6, đổi trạng thái, gán `closedAt` nếu `CLOSED`, ghi lịch sử (`updateWithHistory`) | `EntityNotFoundException`; `InvalidStatusTransitionException` |
| `List<JobStatus> getAvailableNextStatuses(jobId)` | Các trạng thái giao diện được phép đề nghị (đã loại các bước phải đi qua duyệt/từ chối estimate, và `WAITING_FOR_APPROVAL` khi chưa có `DRAFT`) | `EntityNotFoundException` |
| `Estimate createEstimate(jobId, parts, laborServices, note)` | Tạo phiên bản `DRAFT` mới (`EstimateRepository.save`) | `EntityNotFoundException`; `InvalidStatusTransitionException` (job không ở `DIAGNOSING`/`WAITING_FOR_APPROVAL`); `DomainException` (dữ liệu dòng chi tiết sai) |
| `void approveEstimate(estimateId)` | Estimate `APPROVED`, job `REPAIRING`, lịch sử "Customer approves the estimate" (`updateEstimateStatusAndJob`) | `EntityNotFoundException`; `InvalidStatusTransitionException` (không phải bản mới nhất, không phải `DRAFT`, hoặc job không chuyển được sang `REPAIRING`) |
| `void rejectEstimate(estimateId, note)` | Estimate `REJECTED`, job `CLOSED` kèm `closedAt`; ghi chú mặc định "Customer rejects the estimate" nếu `note` trống | `EntityNotFoundException`; `InvalidStatusTransitionException` (không phải bản mới nhất, không phải `DRAFT`, hoặc job không ở `WAITING_FOR_APPROVAL`) |
| `void updateEstimatedReadyAt(jobId, time)` | Gán thời điểm ước tính nhận hàng (`RepairJobRepository.update`), không đổi trạng thái | `EntityNotFoundException` |

## 9. Transaction

Năm thao tác chạy trong `AbstractJdbcRepository.inTransaction` (tắt auto-commit,
commit khi thành công, rollback khi có lỗi, luôn trả kết nối):

1. Tạo đơn kèm lịch sử đầu tiên (`saveWithInitialHistory`).
2. Đổi trạng thái đơn kèm lịch sử (`updateWithHistory`).
3. Lưu estimate cùng các dòng chi tiết (`EstimateRepository.save`).
4. Duyệt hoặc từ chối estimate kèm cập nhật đơn và lịch sử
   (`updateEstimateStatusAndJob`).
5. Import bảo hành legacy (`importLegacy`).

Service và giao diện **không** tự quản lý `Connection` hay import `java.sql`.

## 10. Exception

Package `com.oop.project.exception`; tất cả là unchecked.

```
RuntimeException
├── DomainException                       lỗi nghiệp vụ, thông báo có thể hiển thị cho người dùng
│   ├── InvalidStatusTransitionException  chuyển trạng thái / thao tác estimate không hợp lệ
│   └── EntityNotFoundException           không tìm thấy dữ liệu cần thiết
└── DataAccessException                   lỗi kỹ thuật khi truy cập database
```

| Exception | Ném bởi | Giao diện nên |
|---|---|---|
| `DomainException` | Model (dữ liệu nhập sai), service | Hiện cảnh báo với đúng message để người dùng sửa |
| `InvalidStatusTransitionException` | `JobServiceImpl` | Như trên |
| `EntityNotFoundException` | Service, repository (`update`/`delete` không có dòng) | Như trên; thường là dữ liệu đã bị đổi, nên tải lại danh sách |
| `DataAccessException` | Repository, `DatabaseConnection` | Hiện thông báo lỗi chung; ghi log kèm nguyên nhân gốc (`getCause()`) |

`DataAccessException` không kế thừa `DomainException`, vì đây là lỗi kỹ thuật,
không phải thao tác sai của người dùng. Repository đổi mã lỗi PostgreSQL thành
thông báo: `23503` (khóa ngoại), `23505` (trùng khóa), `23502` (thiếu dữ liệu
bắt buộc), `23514` (vi phạm `CHECK`); các mã khác dùng thông báo gốc của driver.

## 11. Bảo hành và import legacy

**Hai nguồn bảo hành:**

| `source` | Cách tạo | Ràng buộc |
|---|---|---|
| `NATIVE` | `WarrantyRepository.createForRepair` từ một repair `READY`/`CLOSED` chưa có bảo hành | `repair_job_id` bắt buộc |
| `LEGACY` | Import từ file của hệ thống cũ | `repair_job_id` có thể `NULL`; `legacy_ref` duy nhất |

**Định dạng file legacy:** UTF-8, mỗi dòng không rỗng có ba trường phân cách bằng
`|`:

```text
serialNumber|STATUS|description
```

`STATUS` là `COVERED`, `EXPIRED` hoặc `NOT_COVERED`; dòng sai định dạng bị báo
lỗi kèm số dòng.

**Luồng:**

1. `WarrantyFileImporter.importFromFile(Path)` đọc file, trả `List<WarrantyResult>`.
2. `WarrantyAdapter` (cài đặt `WarrantyResult`) bọc một `LegacyWarrantyRecord`
   (serial, trạng thái, mô tả); `WarrantyResult` cung cấp `getSerialNumber()`,
   `getWarrantyStatus()`, `isUnderWarranty()`, `getDescription()`.
3. Người dùng nhập ngày bắt đầu và kết thúc ở bảng review. Giao diện kiểm tra:
   `COVERED` phải có ngày kết thúc từ hôm nay trở đi, `EXPIRED` phải trước hôm
   nay. Ngoài ra `LegacyWarrantyImport` và `WarrantyRepository` đều từ chối ngày
   thiếu hoặc `end < start`.
4. `LegacyWarrantyImportAdapter.adapt(result, start, end, legacyRef)` trả
   `Optional<LegacyWarrantyImport>`; dòng `NOT_COVERED` trả `Optional.empty()`
   (không được lưu).
5. `WarrantyRepository.importLegacy` tìm thiết bị theo `serial_number`, rồi
   `INSERT ... ON CONFLICT DO NOTHING` với `source = 'LEGACY'`. Dòng không có
   thiết bị khớp hoặc đã nhập trước đó bị bỏ qua; kết quả trả về là số dòng đã
   lưu. Nhập lại cùng một file là an toàn.

File mẫu `src/com/oop/project/adapter/warranty.txt` dùng các serial `IP13000`,
`SN123456`, `IP13003` không có trong dữ liệu mẫu, nên các dòng đó không được lưu
(xem [config.md](../config.md) mục 7).

## 12. Cấu hình và AppContext

`DatabaseConnection` (singleton đồng bộ):

| Hàm | Mô tả |
|---|---|
| `static DataSource getDataSource()` | Tạo `PGSimpleDataSource` từ cấu hình ở mục 1; thiếu `DB_USERNAME`/`DB_PASSWORD` thì ném `DataAccessException` |
| `static void close()` | Giải phóng datasource |

`AppContext` là nơi duy nhất khởi tạo repository và service; giao diện **không
tự `new` các lớp `...Impl`**.

| Hàm | Trả về |
|---|---|
| `static AppContext create()` | Context dùng database của ứng dụng (cấu hình `.env`/biến môi trường) |
| `new AppContext(DataSource)` | Context dùng một `DataSource` cho trước |
| `customerRepository()` | `CustomerRepository` |
| `deviceRepository()` | `DeviceRepository` |
| `repairJobRepository()` | `RepairJobRepository` |
| `estimateRepository()` | `EstimateRepository` |
| `statusHistoryRepository()` | `StatusHistoryRepository` |
| `warrantyRepository()` | `WarrantyRepository` |
| `jobService()` | `JobService` |

**Cách dùng trong giao diện:**

- Ghi dữ liệu có luật hoặc nhiều bước: gọi `jobService()`.
- Đọc dữ liệu và CRUD đơn giản (khách hàng, thiết bị, bảo hành, lịch sử): gọi
  repository.
- Hàng đợi: `repairJobRepository().findQueue()` (tổng quan) hoặc
  `findQueue(JobStatus.TECHNICIAN_QUEUE)` (việc của thợ). Không tự sắp xếp lại.

## 13. Dữ liệu mẫu

`database/seed_data.sql` xóa sạch dữ liệu rồi nạp bộ mẫu (giả định "hôm nay" là
2026-10-23; `created_at` được gán cố định để thứ tự hàng đợi không đổi giữa các
lần chạy). Bộ mẫu gồm 3 khách hàng, 6 thiết bị (serial `DK-0001`, `PN-7788`,
`LG-2031`, `DL-5521`; hai thiết bị không có serial), 7 repair, 5 estimate, 2
khoản thanh toán và 2 bảo hành legacy (`SEED-LEGACY-01`, `SEED-LEGACY-02`).

| Repair | Trạng thái | Hạn chót | Dùng cho |
|---|---|---|---|
| #1 | `REPAIRING` | 24/10 | Thử đóng đơn đang sửa (phải báo lỗi); báo giá đã duyệt, chưa có thanh toán |
| #2 | `WAITING_FOR_APPROVAL` | 23/10 | Nhánh duyệt estimate |
| #3 | `RECEIVED` | 26/10 | Hàng đợi |
| #4 | `READY` | 25/10 | Đóng đơn thành công; ứng viên "Create warranty"; đã thanh toán đủ 620.000 |
| #5 | `DIAGNOSING` | 26/10 | Hàng đợi (cùng hạn chót với #3, nhận trước) |
| #6 | `CLOSED` | 17/10 | Estimate bị từ chối |
| #7 | `WAITING_FOR_APPROVAL` | 28/10 | Nhánh từ chối estimate |

Thứ tự `findQueue()` trên dữ liệu mẫu: **#2, #1, #4, #5, #3, #7**; với
`findQueue(TECHNICIAN_QUEUE)`: **#1, #5, #3**.

## 14. Giới hạn đã biết

| Mục | Hiện trạng |
|---|---|
| Thanh toán | Bảng `payments` có trong schema và dữ liệu mẫu nhưng chưa có model, repository hay màn hình; chưa có luật chặn đóng job khi chưa thanh toán |
| Ngôn ngữ thông báo | Model và service dùng tiếng Anh; repository dùng tiếng Việt; một số thông báo pha cả hai |
| Kiểm thử tự động | Bản final không có thư mục `src/test` |
| Connection pool | Không dùng; mỗi lần lấy kết nối đều mở kết nối mới |
| View | Có trong schema nhưng mã Java không dùng |
| Sơ đồ lớp | Chưa có `docs/class-diagram.png` |
| File mẫu bảo hành | Serial trong `warranty.txt` không khớp dữ liệu mẫu |

## 15. Phân công và quy trình thay đổi

| Phần | Nội dung | Thành viên |
|---|---|---|
| Logic và Adapter | `model`, `service`, `adapter`, `JobStatus`, các exception nghiệp vụ | Dương Minh Anh |
| Giao diện | `ui` (các tab, helpers, theme) | Hà Thu Hà |
| Database và Repository | `database/`, `repository`, `config`, `DataAccessException` | Đinh Vân Anh |

**Khi cần thay đổi hợp đồng:**

1. Người đề xuất nêu thay đổi và lý do cho các thành viên bị ảnh hưởng.
2. Sửa đồng thời: mã nguồn, `database/schema.sql` và `seed_data.sql` (nếu liên
   quan đến dữ liệu), các mapper, và file này.
3. Chạy lại `schema.sql` và `seed_data.sql` trên database dev, biên dịch toàn bộ
   source và thử các kịch bản trong [requirements.md](./requirements.md) mục 6.
4. Cập nhật tài liệu liên quan ([README.md](../README.md),
   [WORKINGFLOW.md](../WORKINGFLOW.md), [design.md](./design.md)).
