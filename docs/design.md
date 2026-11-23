Design Document
1. Danh sách các lớp và vai trò (Class List & Responsibilities)
Liệt kê các class chính trong hệ thống và mô tả ngắn gọn vai trò của từng class.
Class	Package	Vai trò
		
		
		
---
2. Áp dụng các nguyên lý OOP
Mô tả rõ từng nguyên lý OOP được áp dụng ở đâu trong hệ thống.
2.1. Encapsulation
Các thuộc tính nào được khai báo `private`?
Truy cập thông qua getter/setter nào?
Lý do áp dụng encapsulation?
Mô tả:
> …
2.2. Inheritance
Class cha là gì?
Các class con kế thừa từ đâu?
Lý do sử dụng kế thừa?
Mô tả:
> …
2.3. Polymorphism
Phương thức nào được override?
Được gọi thông qua reference kiểu cha ở đâu?
Mô tả:
> …
2.4. Interface
Interface nào được sử dụng?
Vai trò của interface trong thiết kế?
Mô tả:
> …
2.5. Abstraction
Abstract class / method nào được sử dụng?
Phần chi tiết nào được ẩn đi?
Mô tả:
> …
---
3. Design Patterns được sử dụng
Liệt kê các design pattern (nếu có) và giải thích ngắn gọn cách áp dụng.
Design Pattern	Áp dụng ở đâu	Mục đích
		
		
> Nếu không sử dụng design pattern nào, hãy giải thích lý do.
---
4. Luồng hoạt động chính (Main Application Flows)
Mô tả các luồng xử lý chính của hệ thống theo dạng từng bước.
4.1. Login
Người dùng nhập username và password.
`LoginView` gửi thông tin đăng nhập đến `AuthService`.
`AuthService` kiểm tra thông tin người dùng.
Nếu hợp lệ, hệ thống chuyển sang `MenuView`.
4.2. [Tên luồng chức năng khác]
…
…
…
---
5. Class Diagram
Vẽ class diagram cho hệ thống bằng draw.io. Sơ đồ phải thể hiện:
Quan hệ kế thừa
Quan hệ association / composition (nếu có)
Interface và class implement
📌 Yêu cầu: xuất sơ đồ thành file ảnh (PNG hoặc JPG), lưu tại `docs/class-diagram.png`.
---
6. Thiết kế lưu trữ dữ liệu (Database / File Design)
6.1. Hình thức lưu trữ
[ ] In-memory
[ ] File (txt / csv / json)
[x] Database (PostgreSQL)
Lý do lựa chọn:
> Dữ liệu có nhiều quan hệ (khách hàng, thiết bị, đơn sửa chữa, báo giá, lịch sử) và cần transaction: đổi trạng thái đơn phải đi cùng việc ghi lịch sử, duyệt/từ chối báo giá phải đi cùng việc đổi trạng thái đơn. PostgreSQL cho khóa ngoại, `CHECK` và transaction sẵn có. Truy cập bằng JDBC thuần + HikariCP. Mỗi máy dùng hai database: `repair\\\_shop` (demo, seed ổn định) và `repair\\\_shop\\\_test` (test, được phép xóa và tạo lại).
6.2. Cấu trúc dữ liệu lưu trữ
Tên bảng	Mô tả	Dữ liệu chính
`customers`	Khách hàng	`full\\\_name`, `phone`, `email` (null), `address` (null)
`devices`	Thiết bị của khách	`customer\\\_id`, `device\\\_type`, `brand`, `model`, `serial\\\_number`
`repair\\\_jobs`	Đơn sửa chữa	`device\\\_id`, `issue\\\_description`, `status`, `desired\\\_days`, `estimated\\\_ready\\\_at` (null), `closed\\\_at` (null)
`estimates`	Báo giá của một đơn (nhiều phiên bản)	`job\\\_id`, `version`, `status`, `note`
`parts`	Dòng linh kiện của một báo giá	`estimate\\\_id`, `name`, `unit\\\_price`, `quantity`
`labor\\\_services`	Dòng công thợ của một báo giá	`estimate\\\_id`, `description`, `hours`, `hourly\\\_rate`
`status\\\_history`	Lịch sử đổi trạng thái của đơn	`job\\\_id`, `old\\\_status` (null), `new\\\_status`, `note`, `changed\\\_at`
Mọi bảng (trừ `parts`, `labor\\\_services`, `status\\\_history`) có `created\\\_at`, `updated\\\_at`; `status\\\_history` có `changed\\\_at`. Khóa chính là `SERIAL id`. Định nghĩa đầy đủ ở `src/main/resources/db/schema.sql`, mô tả từng cột ở `docs/contract.md` mục 4.
6.3. Sơ đồ bảng
```
customers 1 ──── N devices 1 ──── N repair\\\_jobs 1 ──── N estimates 1 ──┬── N parts
                                        │                              └── N labor\\\_services
                                        └──── N status\\\_history
```
Khóa ngoại:
Khóa ngoại	Bảng cha
`devices.customer\\\_id`	`customers.id`
`repair\\\_jobs.device\\\_id`	`devices.id`
`estimates.job\\\_id`	`repair\\\_jobs.id`
`parts.estimate\\\_id`	`estimates.id`
`labor\\\_services.estimate\\\_id`	`estimates.id`
`status\\\_history.job\\\_id`	`repair\\\_jobs.id`
Ràng buộc đáng chú ý:
`status` của job, estimate và history có `CHECK` theo danh sách giá trị enum.
`desired\\\_days > 0`; `unit\\\_price >= 0`, `quantity > 0`, `hours > 0`, `hourly\\\_rate >= 0`.
`UNIQUE (job\\\_id, version)` trên `estimates`.
`chk\\\_repair\\\_jobs\\\_closed\\\_at` buộc `closed\\\_at` có giá trị khi và chỉ khi `status = 'CLOSED'`.
Index: `repair\\\_jobs(status)`, `repair\\\_jobs(device\\\_id)`, `status\\\_history(job\\\_id)`, `devices(customer\\\_id)`, `parts(estimate\\\_id)`, `labor\\\_services(estimate\\\_id)`. `estimates(job\\\_id)` đã được `UNIQUE (job\\\_id, version)` phủ.
6.4. Lý do thiết kế
Khóa ngoại `ON DELETE RESTRICT`, không dùng `CASCADE`
Đơn sửa chữa, báo giá và lịch sử là hồ sơ cần giữ. Với `CASCADE`, xóa nhầm một khách hàng sẽ kéo theo mất toàn bộ thiết bị, đơn, báo giá và lịch sử của khách đó mà không có cảnh báo. Với `RESTRICT`, DB từ chối xóa khi còn dữ liệu con, lỗi lộ ra ngay và được bọc thành `DataAccessException`.
> Từ khóa `CASCADE` trong `DROP TABLE ... CASCADE` của `schema.sql` chỉ để xóa đối tượng phụ thuộc khi dựng lại DB, không liên quan đến `ON DELETE`.
Báo giá có `version`, quan hệ Job ↔ Estimate là 1-N
Khi khách chưa đồng ý, tiệm có thể báo giá lại. Thay vì sửa đè báo giá cũ (mất lịch sử), mỗi lần báo giá là một dòng mới với `version` tăng dần; bản có hiệu lực là bản `version` lớn nhất (`findLatestByJobId`). `UNIQUE (job\\\_id, version)` đảm bảo không có hai bản cùng số.
`version` do Repository gán chứ không do Service/UI: trong một transaction, `EstimateRepository.save()` khóa dòng job (`FOR UPDATE`) rồi lấy `MAX(version) + 1`. Khóa dòng job làm hai lần lưu đồng thời của cùng một đơn phải xếp hàng nhau thay vì cùng nhận một số. Việc gán nằm cùng chỗ với câu `INSERT` nên không thể lệch với dữ liệu thực.
Hạn chót tính theo biểu thức, không lưu cột
Hạn chót = `created\\\_at + desired\\\_days` ngày. Nếu lưu thành cột `deadline`, cột này có thể lệch khi `created\\\_at` hoặc `desired\\\_days` đổi, và phải nhớ cập nhật ở mọi nơi ghi. Tính khi truy vấn thì luôn đúng:
```sql
ORDER BY (created\\\_at + desired\\\_days \\\* INTERVAL '1 day') ASC, created\\\_at ASC, id ASC
```
Hai tiêu chí sau để phá hòa và giữ thứ tự ổn định giữa các lần chạy. Với lượng đơn của một tiệm sửa chữa, `repair\\\_jobs(status)` là đủ nên không tạo index riêng cho biểu thức. Phía Java, `RepairJob.getDeadline()` trả `createdAt.plusDays(desiredDays)` để UI hiển thị.
Tổng tiền không lưu ở DB
`Estimate.getTotalCost()` cộng `getCost()` của mọi dòng (`unit\\\_price × quantity` và `hours × hourly\\\_rate`), nên tổng không bao giờ lệch với chi tiết.
Tiền dùng `NUMERIC`, không dùng số thực
`NUMERIC(12,2)` ở DB và `BigDecimal` ở Java để tránh sai số làm tròn; `hours` dùng `NUMERIC(6,2)`.
Trạng thái lưu `VARCHAR` + `CHECK`, không dùng kiểu enum của PostgreSQL
Giá trị trùng tên hằng trong Java (`JobStatus`, `EstimateStatus`). Thêm trạng thái chỉ cần sửa `CHECK` và enum, không phải `ALTER TYPE`.
Transaction ở tầng Repository
Các thao tác cần nhiều bước (`saveWithInitialHistory`, `updateWithHistory`, `updateEstimateStatusAndJob`, `EstimateRepository.save`) mỗi hàm tự chạy trong một transaction, nên tầng trên không quản lý `Connection` và không import `java.sql.\\\*`. Service kiểm tra luật nghiệp vụ trước rồi mới gọi.
Seed data cố định
`seed\\\_data.sql` gán `created\\\_at` cụ thể cho từng đơn để thứ tự hàng đợi không đổi giữa các lần demo, và nạp lại `setval` cho các bộ đếm `SERIAL` sau khi chèn id cố định.
---
7. Nhận xét về thiết kế (Optional)
Ưu điểm của thiết kế hiện tại: …
Hạn chế: …
Hướng cải tiến trong tương lai (nếu có): …
---
8. Kết luận
Tóm tắt ngắn gọn cách thiết kế hệ thống và cách áp dụng OOP trong project.
