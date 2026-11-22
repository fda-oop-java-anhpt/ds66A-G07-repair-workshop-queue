# Luồng hoạt động của Repair Workshop Queue

Tài liệu này mô tả cách khởi động ứng dụng và luồng thao tác của các chức năng
trên giao diện Java Swing. Ứng dụng gồm năm tab: **Customers/Devices**,
**Repair Queue**, **Estimate**, **Warranty** và **Job History**.

## 1. Khởi động ứng dụng

1. Cài PostgreSQL, tạo database `repair-workshop-queue`, rồi chạy
   `database/schema.sql` và `database/seed_data.sql` theo thứ tự nếu cần tạo
   schema và dữ liệu mẫu.
2. Tải PostgreSQL JDBC driver vào `lib/postgresql-42.7.14.jar`.
3. Tạo `.env` ở thư mục gốc với thông tin kết nối:

   ```text
   DB_URL=jdbc:postgresql://localhost:5432/repair-workshop-queue
   DB_USERNAME=postgres
   DB_PASSWORD=<mật-khẩu-PostgreSQL>
   ```

   Có thể dùng environment variables thay cho `.env`. Environment variables
   được ưu tiên nếu cùng khóa đã có trong cả hai nơi.
4. Biên dịch và chạy từ PowerShell tại thư mục gốc (phải chạy tại thư mục gốc
   vì `.env` được đọc theo đường dẫn tương đối):

   ```powershell
   $files = Get-ChildItem .\src -Recurse -Filter *.java |
       ForEach-Object { $_.FullName }

   javac -encoding UTF-8 -cp ".\lib\*" -d ".\out" $files
   java -cp ".\out;.\lib\*" com.oop.project.ui.Home
   ```

5. `Home` tạo kết nối ứng dụng qua `AppContext`, khởi tạo các repository và
   service, tải dữ liệu ban đầu cho các tab rồi mở cửa sổ chính. Nếu thiếu cấu
   hình database hoặc không kết nối được PostgreSQL, ứng dụng báo lỗi khi khởi
   động; các màn hình chính dùng dữ liệu PostgreSQL, không phải chế độ offline.

Luồng xử lý dữ liệu tổng quát:

```text
Người dùng
    → Swing UI (Home và các tab)
    → JobService (quy tắc tạo/sửa trạng thái job và estimate)
      hoặc Repository (đọc/ghi customer, device, warranty, history)
    → PostgreSQL qua JDBC
    → UI tải lại dữ liệu và hiển thị kết quả
```

## 2. Luồng sửa chữa thông thường

```text
Tạo/chọn khách hàng
    → Thêm/chọn thiết bị
    → Tạo repair job (trạng thái ban đầu RECEIVED)
    → Kiểm tra và chẩn đoán trong Repair Queue
    → Tạo estimate dạng DRAFT trong tab Estimate
    → Đưa job sang WAITING_FOR_APPROVAL trong Repair Queue
    → Khách duyệt: chuyển sang REPAIRING
      hoặc từ chối: chuyển sang CLOSED
    → Hoàn tất sửa chữa: READY
    → Đóng đơn sau khi bàn giao: CLOSED
    → Tạo warranty cho repair READY/CLOSED (nếu đủ điều kiện)
```

Mỗi lần đổi trạng thái hợp lệ được lưu vào lịch sử. Sau khi tạo repair job,
cập nhật trạng thái hoặc xử lý estimate, các danh sách liên quan được làm mới
để phản ánh dữ liệu mới.

## 3. Customers/Devices

Tab này quản lý khách hàng, thiết bị của khách hàng và các repair job liên
quan.

1. **Tìm khách hàng:** nhập tên hoặc số điện thoại rồi chọn **Search** (hoặc
   nhấn Enter). Xóa nội dung tìm kiếm để tải lại danh sách đầy đủ.
2. **Thêm khách hàng:** chọn **Add customer**, điền tên và điện thoại (được
   đánh dấu `*` là bắt buộc). Điện thoại chỉ gồm chữ số và có đúng 10 chữ số;
   email và địa chỉ là tùy chọn, nhưng email phải đúng định dạng nếu được nhập.
   Cảnh báo hiện dưới trường tương ứng khi bấm **Add**; form giữ nguyên dữ liệu
   để sửa. Nút **Add** hiển thị nhạt khi dữ liệu chưa hợp lệ nhưng vẫn bấm được.
   Đóng cửa sổ bằng **X** nếu muốn bỏ thao tác. Khách hàng mới được chọn sau
   khi lưu.
3. **Sửa/xóa khách hàng:** chọn một khách hàng trước khi dùng **Edit customer**
   hoặc **Delete customer**. Xóa cần xác nhận; thao tác có thể bị từ chối nếu
   dữ liệu liên quan trong database không cho phép xóa.
4. **Quản lý thiết bị:** chọn khách hàng để xem thiết bị. Dùng **Add device**,
   **Edit device** hoặc **Delete device**; các thao tác này yêu cầu đã chọn
   khách hàng, và sửa/xóa yêu cầu chọn thêm thiết bị. Chọn **Device type** từ
   danh sách có sẵn (Điều hòa, Máy giặt, Tủ lạnh, Laptop, Điện thoại, Máy tính
   bàn, Tivi hoặc Khác); cần chọn loại thiết bị trước khi lưu, các trường khác
   là tùy chọn. Nếu chưa chọn loại, nút **Add** hiển thị nhạt nhưng vẫn bấm
   được; cảnh báo hiện dưới trường chọn và form giữ nguyên để sửa. Biểu mẫu
   thêm chỉ có nút **Add**; đóng cửa sổ bằng **X** để bỏ thao tác.
5. **Tạo repair job:** chọn thiết bị rồi chọn **Create repair job**. Nhập mô tả
   sự cố và số ngày dự kiến (được đánh dấu `*`; số ngày phải là số nguyên lớn
   hơn 0). Khi lưu thành công, job bắt đầu ở trạng thái `RECEIVED`.
6. **Xem repair history của khách hàng:** chọn tab con **Repair history** ở
   vùng hồ sơ khách hàng. Chọn một repair để xem các trạng thái kế tiếp có thể
   chọn và nhập ghi chú, rồi bấm **Update status**.
7. Thông tin liên hệ và số lượng thiết bị/repair job của khách hàng được hiển
   thị trong khung chi tiết bên cạnh danh sách.

## 4. Repair Queue và vòng đời trạng thái

Tab **Repair Queue** hiển thị các repair đang trong hàng chờ ở bên trái và
vùng tìm kiếm/cập nhật ở bên phải.

1. Dùng các trường lọc để tìm theo nội dung tổng quát, nhóm trạng thái, thiết
   bị, khách hàng, triệu chứng hoặc ghi nhận; chọn thứ tự ngày nhận nếu cần.
2. Chọn **Search** để áp dụng bộ lọc; **Reset filters** xóa các điều kiện lọc.
3. Chọn repair trong danh sách đã lọc, chọn trạng thái kế tiếp đang được phép
   (danh sách do `JobService.getAvailableNextStatuses` tính), nhập ghi chú nếu
   cần và bấm **Update selected repair**.
4. Ứng dụng kiểm tra chuyển trạng thái qua `JobService`, lưu thay đổi cùng một
   bản ghi history, rồi tải lại Queue, Job History và Warranty.
5. Khi job đang `DIAGNOSING`, cần lưu một estimate `DRAFT` trong tab Estimate
   trước khi chuyển job sang `WAITING_FOR_APPROVAL`. Khi đang chờ khách, xử lý
   **Approve latest** hoặc **Reject latest** trong tab Estimate; không cập nhật
   các trạng thái này trực tiếp từ Queue.

Các chuyển trạng thái hợp lệ:

| Trạng thái hiện tại | Trạng thái tiếp theo | Điều kiện |
|---|---|---|
| `RECEIVED` | `DIAGNOSING` | Bắt đầu chẩn đoán |
| `DIAGNOSING` | `WAITING_FOR_APPROVAL` | Gửi estimate để khách duyệt; cần có estimate `DRAFT` mới nhất |
| `WAITING_FOR_APPROVAL` | `REPAIRING` | Thực hiện bằng **Approve latest** ở tab Estimate; không chọn trực tiếp từ Queue |
| `WAITING_FOR_APPROVAL` | `CLOSED` | Thực hiện bằng **Reject latest** ở tab Estimate (khách từ chối estimate) |
| `REPAIRING` | `READY` | Hoàn tất sửa chữa |
| `READY` | `CLOSED` | Đóng đơn sau khi hoàn tất/bàn giao |

`CLOSED` là trạng thái cuối; các chuyển trạng thái khác với bảng trên sẽ bị
từ chối và ứng dụng hiển thị lỗi.

## 5. Estimate

Tab **Estimate** tạo và xử lý các phiên bản báo giá cho repair job.

1. Bấm **Find repair...**, tìm theo mã repair, thiết bị, khách hàng hoặc mô
   tả sự cố, chọn một dòng rồi xác nhận. Thông tin repair và estimate mới nhất
   (nếu đã có) được tải vào màn hình.
2. Nếu sửa estimate chưa lưu mà chuyển sang repair/phiên bản khác, ứng dụng
   hỏi có bỏ các thay đổi chưa lưu hay không.
3. Chọn tab con **Parts** hoặc **Labor**. Dùng **Add part**/**Add labor** để
   thêm dòng và các nút **Remove selected...** để xóa dòng đang chọn.
   - Parts cần tên, đơn giá không âm (tối đa 2 chữ số thập phân) và số lượng
     nguyên lớn hơn 0.
   - Labor cần mô tả, số giờ lớn hơn 0 và đơn giá theo giờ không âm; số giờ,
     đơn giá tối đa 2 chữ số thập phân.
   - Subtotal và tổng estimate được tính tự động; dòng sai dữ liệu sẽ hiển thị
     lỗi ngay trong tab tương ứng.
   - Các trường bắt buộc được đánh dấu `*`. Số lượng, đơn giá, số giờ, subtotal
     và tổng tiền hiển thị dấu phân tách hàng nghìn, ví dụ `2,000.00`.
4. Khi repair đang `DIAGNOSING` hoặc `WAITING_FOR_APPROVAL`, nhập ghi chú nếu cần
   rồi bấm **Save as new draft version**. Mỗi lần lưu tạo một phiên bản `DRAFT`
   mới, không ghi đè các phiên bản trước. Các nút sửa và lưu bị khóa ở trạng
   thái repair không cho phép lập estimate.
5. Chọn phiên bản trong **Saved versions** để xem lại. Chỉ phiên bản mới nhất
   ở trạng thái `DRAFT` mới có thể được duyệt hoặc từ chối.
6. Các nút duyệt/từ chối chỉ bật khi repair đang `WAITING_FOR_APPROVAL` và
   phiên bản đang xem là bản `DRAFT` mới nhất.
   **Approve latest:** xác nhận khách duyệt estimate; estimate thành
   `APPROVED` và job chuyển sang `REPAIRING`.
7. **Reject latest:** xác nhận khách từ chối; estimate thành `REJECTED` và job
   chuyển sang `CLOSED`. Ghi chú có thể được lưu cùng lịch sử trạng thái.

## 6. Warranty

Tab **Warranty** có danh sách warranty hiện có, số liệu tổng quan, danh sách
repair đủ điều kiện tạo warranty và chức năng nhập dữ liệu legacy.

### Tạo warranty cho repair trong ứng dụng

1. Tìm trong danh sách **Repairs eligible for warranty** theo repair/thiết bị/
   khách hàng.
2. Chọn một repair để xem thông tin và nhập ngày bắt đầu/kết thúc (các trường
   bắt buộc được đánh dấu `*`) theo định dạng `yyyy-MM-dd`.
3. Ngày kết thúc phải cùng ngày hoặc sau ngày bắt đầu. Bấm **Create warranty**
   để lưu.
4. Chỉ repair ở trạng thái `READY` hoặc `CLOSED`, chưa có warranty mới đủ điều
   kiện. Danh sách và số liệu được tải lại sau khi tạo.

### Import warranty legacy

1. Bấm **Import legacy file** và chọn file UTF-8. Mỗi dòng không rỗng có định
   dạng:

   ```text
   serialNumber|STATUS|description
   ```

   `STATUS` là `COVERED`, `EXPIRED` hoặc `NOT_COVERED`; ví dụ:
   `LG-2031|COVERED|Thay quạt dàn lạnh`. `serialNumber` phải trùng số serial của
   một thiết bị trong database (dữ liệu mẫu có `DK-0001`, `PN-7788`, `LG-2031`,
   `DL-5521`).
2. Ứng dụng đọc và kiểm tra file, sau đó mở bảng review. Nhập ngày bắt đầu và
   kết thúc cho mỗi dòng `COVERED`/`EXPIRED`. Ngày phải đúng định dạng và khớp
   với trạng thái legacy: `COVERED` có ngày kết thúc hôm nay hoặc trong tương
   lai, `EXPIRED` có ngày kết thúc trước hôm nay.
3. Dòng `NOT_COVERED` được bỏ qua, không tạo warranty.
4. Xác nhận import để lưu các dòng hợp lệ có serial number khớp thiết bị trong
   database. Dòng trùng được bỏ qua; ứng dụng báo số dòng đã nhập, bị trùng
   hoặc không được lưu. Sau đó danh sách warranty được làm mới.

## 7. Job History

Tab **Job History** là nhật ký các lần đổi trạng thái repair.

1. Nhập từ khóa để tìm trên các cột; chọn trạng thái mới để lọc theo trạng
   thái đích.
2. Chọn **From date** và/hoặc **To date** bằng ô ngày hoặc nút lịch.
3. Bấm **Search** để áp dụng. Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.
   **Reset** xóa từ khóa, trạng thái và cả hai mốc ngày.
4. Dòng trạng thái phía dưới cho biết số bản ghi đang hiển thị trên tổng số
   bản ghi. History được tự tải lại sau các thao tác cập nhật trạng thái.

## 8. Kiểm tra lỗi và lưu ý dữ liệu

- Lỗi nhập liệu được hiển thị ngay cạnh trường/dòng liên quan; lỗi nghiệp vụ
  hoặc database được hiển thị trong hộp thoại để người dùng xử lý.
- Nếu danh sách chưa phản ánh dữ liệu mới, thao tác tạo/cập nhật trong giao
  diện chính thường tự tải lại các tab liên quan. Có thể tìm kiếm lại hoặc
  chuyển tab để xem dữ liệu hiện tại.
- `database/seed_data.sql` xóa dữ liệu hiện có trước khi nạp dữ liệu mẫu.
  Không chạy lại trên database cần giữ dữ liệu thật.
- Hướng dẫn cài PostgreSQL, JDBC driver và lệnh chạy đầy đủ nằm trong
  [config.md](./config.md); mô tả chức năng và cấu trúc project nằm trong
  [README.md](./README.md).
