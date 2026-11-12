# Tài liệu thiết kế – Repair Workshop Queue

## 1. Kiến trúc tổng quan

Ứng dụng Java Swing dùng PostgreSQL để quản lý khách hàng, thiết bị, phiếu sửa
chữa, báo giá, lịch sử trạng thái và bảo hành. Mã nguồn được chia thành các
package chính:

| Package | Trách nhiệm |
|---|---|
| `ui` | Các màn hình Swing và thành phần bảng |
| `model` | Mô hình nghiệp vụ, trạng thái và quy tắc kiểm tra dữ liệu |
| `service` | Điều phối nghiệp vụ sửa chữa và báo giá |
| `repository` | Hợp đồng truy cập dữ liệu |
| `repository.impl` | Truy vấn PostgreSQL bằng JDBC |
| `repository.mapper` | Ánh xạ `ResultSet` thành mô hình |
| `adapter` | Đọc và chuyển đổi dữ liệu bảo hành legacy |
| `config` | Khởi tạo kết nối và ghép các thành phần ứng dụng |
| `exception` | Các lỗi nghiệp vụ, chuyển trạng thái và truy cập dữ liệu |

`Home` tạo các tab Customers/Devices, Repair Queue, Estimate, Warranty và Job
History. `AppContext` khởi tạo các repository và `JobService` từ cùng một
`DataSource`; UI gọi các interface này thay vì tự tạo kết nối JDBC.

## 2. Các lớp và vai trò

| Class / Interface | Package | Vai trò |
|---|---|---|
| `Customer` | `model` | Thông tin khách hàng và kiểm tra tên, số điện thoại |
| `Device` | `model` | Thiết bị thuộc một khách hàng |
| `RepairJob` | `model` | Phiếu sửa chữa, mô tả lỗi, trạng thái và mốc thời gian |
| `JobStatus` | `model` | Các trạng thái và quy tắc chuyển trạng thái |
| `StatusHistory` | `model` | Ghi lại trạng thái trước/sau, ghi chú và thời điểm |
| `Estimate` | `model` | Báo giá theo phiên bản, trạng thái và các hạng mục |
| `EstimateItem` | `model` | Hợp đồng chung cho tên và chi phí của một hạng mục |
| `Part` | `model` | Linh kiện, đơn giá và số lượng; cài đặt `EstimateItem` |
| `LaborService` | `model` | Công sửa chữa, số giờ và đơn giá; cài đặt `EstimateItem` |
| `WarrantyRecord` | `model` | Dữ liệu bảo hành để hiển thị |
| `WarrantyResult` | `adapter` | Giao diện kết quả bảo hành thống nhất |
| `WarrantyAdapter` | `adapter` | Bọc bản ghi legacy thành `WarrantyResult` |
| `WarrantyFileImporter` | `adapter` | Đọc file legacy UTF-8 theo từng dòng |
| `LegacyWarrantyImportAdapter` | `adapter` | Chuyển kết quả legacy đủ điều kiện thành dữ liệu import |
| `JobService` / `JobServiceImpl` | `service` | Hợp đồng và xử lý nghiệp vụ job, báo giá, trạng thái |
| `GenericRepository<T, ID>` | `repository` | Các thao tác CRUD dùng chung |
| `CustomerRepository`, `DeviceRepository`, `RepairJobRepository`, `EstimateRepository`, `StatusHistoryRepository`, `WarrantyRepository` | `repository` | Các thao tác dữ liệu theo từng loại nghiệp vụ |
| `AbstractJdbcRepository` | `repository.impl` | Dùng chung truy vấn, transaction và chuyển lỗi JDBC |
| `*RepositoryImpl` | `repository.impl` | Cài đặt repository bằng PostgreSQL/JDBC |
| `AppContext` | `config` | Khởi tạo và cung cấp các dependency cho UI |
| `Home` | `ui` | Cửa sổ chính và điều phối các tab |

## 3. Nguyên lý OOP

### 3.1. Encapsulation

Các trường dữ liệu trong các model như `Customer`, `Device`, `RepairJob` và
`Estimate` được khai báo `private`; getter/setter chỉ được cung cấp ở những
trường hợp cần thiết. Constructor và các hàm kiểm tra giữ các điều kiện dữ liệu
ở gần model (ví dụ tên và số điện thoại khách hàng, mô tả lỗi, số ngày mong
muốn). `Estimate` trả về danh sách chỉ đọc qua getter để bên ngoài không thể
thay đổi trực tiếp danh sách nội bộ.

### 3.2. Inheritance

`AbstractJdbcRepository` là lớp trừu tượng cơ sở; các repository JDBC cụ thể
như `CustomerRepositoryImpl`, `DeviceRepositoryImpl` và
`RepairJobRepositoryImpl` kế thừa để dùng chung thao tác truy vấn, transaction
và xử lý lỗi. Các cửa sổ Swing cũng kế thừa lớp giao diện Swing tương ứng.

Kế thừa được dùng cho hạ tầng dùng chung, không phải để mô hình hóa các loại
phiếu sửa chữa bằng các nhánh `if/else`.

### 3.3. Polymorphism và interface

- `Part` và `LaborService` cùng cài đặt `EstimateItem`, cung cấp tên và chi phí
  theo cách riêng.
- `WarrantyAdapter` cài đặt `WarrantyResult`; file importer trả về danh sách
  kết quả qua interface này thay vì buộc UI phụ thuộc trực tiếp vào cấu trúc
  bản ghi legacy.
- `JobServiceImpl` cài đặt `JobService`; repository JDBC cài đặt các interface
  repository tương ứng. `AppContext` cung cấp chúng cho các màn hình.

### 3.4. Abstraction

`JobService` mô tả các thao tác nghiệp vụ mà UI cần mà không phơi bày chi tiết
SQL. `GenericRepository` định nghĩa các thao tác CRUD phổ biến. Lớp trừu tượng
`AbstractJdbcRepository` gom chi tiết JDBC dùng chung cho các repository cụ
thể. Không có abstract model dùng để phân loại thiết bị hoặc loại job trong
domain.

## 4. Design pattern và cấu trúc thiết kế

| Pattern / cấu trúc | Nơi áp dụng | Mục đích |
|---|---|---|
| Adapter | `WarrantyAdapter`, `WarrantyFileImporter` | Chuyển bản ghi legacy thành hợp đồng `WarrantyResult` dùng chung |
| Adapter chuyển đổi nghiệp vụ | `LegacyWarrantyImportAdapter` | Chuyển kết quả bảo hành legacy sang `LegacyWarrantyImport`; kết quả `NOT_COVERED` không tạo dữ liệu import |
| Repository | Các interface repository và lớp `*RepositoryImpl` | Tách truy cập PostgreSQL khỏi UI và nghiệp vụ |
| Composition root | `AppContext` | Tạo và nối các repository, service từ một `DataSource` |

## 5. Các quan hệ chính

- Một `Customer` có thể gắn với nhiều `Device`; mỗi `Device` tham chiếu một
  khách hàng.
- `RepairJob` tham chiếu một `Device`. Job lưu thông tin lỗi, trạng thái, thời
  hạn mong muốn và các mốc thời gian.
- Một job có thể có nhiều `Estimate` theo phiên bản. Mỗi estimate chứa các
  `Part` và `LaborService`; tổng tiền được tính từ chi phí của các hạng mục.
- `StatusHistory` ghi các lần thay đổi trạng thái của job.
- `WarrantyRecord` liên kết dữ liệu bảo hành với thiết bị và, khi có, job sửa
  chữa.

## 6. Luồng hoạt động

### 6.1. Khởi động

1. `Home` gọi `AppContext.create()`.
2. `AppContext` lấy `DataSource` từ cấu hình kết nối và khởi tạo repository,
   service.
3. `Home` tạo năm tab và chia sẻ các thao tác làm mới sau khi dữ liệu thay đổi.

### 6.2. Tạo khách hàng, thiết bị và job

1. Nhân viên nhập khách hàng và thiết bị trong tab Customers/Devices.
2. Khi tạo job, UI gọi `JobService.createJob` với thiết bị, mô tả lỗi và số
   ngày mong muốn.
3. Service xác nhận thiết bị tồn tại, tạo job ở trạng thái `RECEIVED` và lưu
   lịch sử ban đầu.

### 6.3. Báo giá và xử lý job

1. Job chuyển theo các trạng thái được khai báo trong `JobStatus`: `RECEIVED`,
   `DIAGNOSING`, `WAITING_FOR_APPROVAL`, `REPAIRING`, `READY`, `CLOSED`.
2. Khi chẩn đoán hoặc chờ duyệt, nhân viên có thể lập estimate gồm linh kiện
   và công sửa chữa; chi phí được tính từ các hạng mục.
3. Duyệt estimate mới nhất sẽ cập nhật estimate thành `APPROVED` và job sang
   `REPAIRING`; từ chối estimate sẽ cập nhật thành `REJECTED` và đóng job.
4. Các thay đổi trạng thái được ghi vào `status_history`. Việc cập nhật job và
   lịch sử được thực hiện qua repository.
5. Job ở `READY` có thể chuyển sang `CLOSED` qua luồng cập nhật trạng thái.

### 6.4. Nhập và quản lý bảo hành

1. `WarrantyFileImporter` đọc file UTF-8, mỗi dòng có dạng
   `serialNumber|status|description`.
2. `WarrantyAdapter` biểu diễn mỗi bản ghi legacy theo `WarrantyResult`.
3. `LegacyWarrantyImportAdapter` chuyển kết quả được bảo hành thành dữ liệu
   import; kết quả `NOT_COVERED` được bỏ qua.
4. Tab Warranty dùng `WarrantyRepository` để hiển thị và quản lý dữ liệu bảo
   hành trong cơ sở dữ liệu.

## 7. Lưu trữ dữ liệu

Ứng dụng dùng PostgreSQL qua JDBC. Schema định nghĩa các bảng:

| Bảng | Dữ liệu |
|---|---|
| `customers` | Thông tin khách hàng |
| `devices` | Thiết bị và khóa ngoại tới khách hàng |
| `repair_jobs` | Job, trạng thái, mô tả lỗi và thời gian |
| `estimates` | Báo giá, phiên bản và trạng thái |
| `parts` | Linh kiện và số lượng theo estimate |
| `labor_services` | Công sửa chữa theo estimate |
| `status_history` | Lịch sử trạng thái của job |
| `warranties` | Bảo hành native hoặc import legacy |
| `payments` | Bảng thanh toán được khai báo trong schema |

Schema còn định nghĩa các view phục vụ Repair Queue và Warranty. Dù bảng
`payments` có trong schema, hiện chưa có model, repository hay màn hình thanh
toán tương ứng trong mã Java. Vì vậy không nên coi điều kiện thanh toán là đã
được kiểm tra khi đóng job.

## 8. Giới hạn và hướng hoàn thiện

- Chưa có xử lý thanh toán trong Java để ngăn đóng job khi chưa thanh toán.
- Chưa có file sơ đồ lớp `docs/class-diagram.png`; cần bổ sung nếu đây là yêu
  cầu bàn giao của nhóm.
- Thông tin đóng góp từng thành viên và phần tự đánh giá cần do nhóm xác nhận,
  không thể suy ra chỉ từ mã nguồn.
