# G07 – Repair Workshop Queue

## 1. Bối cảnh

Workshop cần theo dõi khách hàng, thiết bị nhận sửa, tình trạng chẩn đoán, báo
giá và lịch sử xử lý. Hệ thống cũng hỗ trợ quản lý bảo hành và chuyển đổi kết
quả bảo hành từ định dạng legacy.

## 2. Phạm vi sản phẩm

Ứng dụng desktop Java Swing gồm các màn hình:

- **Customers/Devices** – quản lý khách hàng, thiết bị và tạo repair job.
- **Repair Queue** – xem và xử lý các job.
- **Estimate** – lập báo giá, thêm linh kiện/công sửa chữa và xử lý duyệt.
- **Warranty** – xem, tạo và nhập dữ liệu bảo hành.
- **Job History** – xem lịch sử trạng thái.

Dữ liệu được lưu bằng PostgreSQL qua JDBC.

## 3. Yêu cầu chức năng và trạng thái triển khai

| Yêu cầu | Trạng thái theo mã hiện tại |
|---|---|
| Quản lý khách hàng và thiết bị | Có màn hình và repository tương ứng |
| Tạo repair job gắn với thiết bị, mô tả lỗi và số ngày mong muốn | Đã triển khai qua `JobService` |
| Theo dõi các trạng thái `RECEIVED`, `DIAGNOSING`, `WAITING_FOR_APPROVAL`, `REPAIRING`, `READY`, `CLOSED` | Đã khai báo; chuyển trạng thái được kiểm tra bởi service |
| Lưu lịch sử thay đổi trạng thái | Đã triển khai qua repository lịch sử |
| Tạo báo giá từ linh kiện và công sửa chữa; tính tổng chi phí | Đã triển khai trong model và màn hình Estimate |
| Duyệt/từ chối báo giá mới nhất | Đã triển khai; duyệt đưa job sang `REPAIRING`, từ chối đóng job |
| Tạo và hiển thị bảo hành | Có model, repository và màn hình Warranty |
| Đọc dữ liệu bảo hành legacy và chuyển đổi qua Adapter | Đã có importer và adapter |
| Lưu dữ liệu chính vào PostgreSQL qua JDBC | Đã triển khai qua các repository JDBC |
| Chặn đóng job khi chưa thanh toán đủ | Chưa được thực thi trong mã Java; schema có bảng `payments` nhưng chưa có luồng thanh toán trong ứng dụng |

## 4. Quy tắc nghiệp vụ

1. Job mới bắt đầu ở trạng thái `RECEIVED`.
2. Các chuyển trạng thái hợp lệ được định nghĩa trong `JobStatus`; job đã
   `CLOSED` không thể chuyển tiếp.
3. Chỉ có thể lập estimate khi job đang `DIAGNOSING` hoặc
   `WAITING_FOR_APPROVAL`.
4. Chỉ estimate mới nhất đang ở `DRAFT` mới được duyệt hoặc từ chối.
5. Estimate được duyệt đưa job sang `REPAIRING`; estimate bị từ chối đưa job
   từ `WAITING_FOR_APPROVAL` sang `CLOSED`.
6. Job ở `READY` có thể được đóng. Việc kiểm tra thanh toán trước khi đóng
   hiện chưa được triển khai.
7. Job bắt buộc gắn với thiết bị; thiết bị bắt buộc gắn với khách hàng.
8. File bảo hành legacy dùng UTF-8, mỗi dòng có ba trường phân cách bằng dấu
   `|`: `serialNumber|status|description`.

## 5. Yêu cầu dữ liệu

PostgreSQL schema định nghĩa các bảng `customers`, `devices`, `repair_jobs`,
`estimates`, `parts`, `labor_services`, `status_history`, `warranties` và
`payments`. Các bảng bảo đảm quan hệ bằng khóa ngoại và áp dụng các ràng buộc
như giá trị/số lượng hợp lệ, trạng thái được hỗ trợ và ngày bảo hành hợp lệ.

Ứng dụng cấu hình kết nối bằng `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` từ biến
môi trường hoặc file `.env`. Cách tạo schema, nạp dữ liệu mẫu, biên dịch và
khởi chạy được hướng dẫn trong [config.md](../config.md).

## 6. Kịch bản kiểm thử/chạy thử

1. Tạo khách hàng, thiết bị và repair job; kiểm tra job xuất hiện ở Repair
   Queue và có lịch sử ban đầu.
2. Chuyển job qua các trạng thái hợp lệ; thử một chuyển trạng thái không hợp
   lệ và xác nhận ứng dụng báo lỗi.
3. Tạo estimate gồm part và labor service; kiểm tra tổng chi phí, duyệt hoặc
   từ chối estimate.
4. Nhập file bảo hành đúng định dạng và kiểm tra dữ liệu được hiển thị; thử
   dòng sai định dạng để xác nhận lỗi được báo.
5. Xác nhận dữ liệu vẫn còn sau khi khởi động lại ứng dụng.
6. Kiểm tra thanh toán trước khi đóng job là yêu cầu chưa hoàn thành, không
   phải hành vi hiện có.

## 7. Ngoài phạm vi hoặc chưa hoàn tất

- Tích hợp SMS.
- Theo dõi tải công việc kỹ thuật viên.
- In hóa đơn sửa chữa.
- Hoàn thiện luồng thanh toán và ràng buộc đóng job theo thanh toán.
- Sơ đồ lớp được xuất thành `docs/class-diagram.png` nếu được yêu cầu trong
  bản bàn giao.
