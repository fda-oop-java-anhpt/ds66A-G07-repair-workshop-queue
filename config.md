# Cấu hình PostgreSQL và chạy ứng dụng

Hướng dẫn này dành cho Windows PowerShell. Ứng dụng biên dịch trực tiếp bằng
`javac`, không cần Maven.

## 1. Cài PostgreSQL

Tải PostgreSQL cho Windows từ [trang tải chính thức](https://www.postgresql.org/download/windows/)
và cài đặt. Ghi nhớ username và password của PostgreSQL (thường username là
`postgres` nếu không đổi trong quá trình cài đặt).

## 2. Tải PostgreSQL JDBC driver

Tải phiên bản **42.7.14** của PostgreSQL JDBC driver:

[postgresql-42.7.14.jar (Maven Central)](https://repo.maven.apache.org/maven2/org/postgresql/postgresql/42.7.14/postgresql-42.7.14.jar)

Chép file `postgresql-42.7.14.jar` vào thư mục `lib` ở thư mục gốc project. Sau
khi chép, đường dẫn cần có là:

```text
lib\postgresql-42.7.14.jar
```

## 3. Tạo database và nạp schema/dữ liệu mẫu

Tạo database PostgreSQL tên `repair-workshop-queue` bằng pgAdmin (Create >
Database), hoặc mở PowerShell và chạy:

```powershell
createdb -U postgres repair-workshop-queue
```

Thay `postgres` bằng username PostgreSQL của bạn nếu cần. Nếu lệnh
`createdb` không được nhận diện, dùng pgAdmin để tạo database hoặc thêm thư mục
`bin` của PostgreSQL vào `PATH`.

Chạy hai file SQL từ thư mục gốc project, đúng thứ tự:

```powershell
psql -U postgres -d repair-workshop-queue -f .\database\schema.sql
psql -U postgres -d repair-workshop-queue -f .\database\seed_data.sql
```

Nhập password PostgreSQL nếu được hỏi. Script SQL chỉ nằm trong thư mục
`database`. Không chạy lại `schema.sql` hoặc `seed_data.sql` trên database có dữ liệu cần
giữ: `schema.sql` xóa và tạo lại các bảng, còn `seed_data.sql` xóa dữ liệu rồi
nạp dữ liệu mẫu.

Kiểm tra nhanh sau khi nạp (kỳ vọng 7 repair: `RECEIVED` 1, `DIAGNOSING` 1,
`WAITING_FOR_APPROVAL` 2, `REPAIRING` 1, `READY` 1, `CLOSED` 1):

```powershell
psql -U postgres -d repair-workshop-queue -c "SELECT status, COUNT(*) FROM repair_jobs GROUP BY status;"
```

## 4. Cấu hình kết nối

Tạo hoặc cập nhật file `.env` tại thư mục gốc project:

```text
DB_URL=jdbc:postgresql://localhost:5432/repair-workshop-queue
DB_USERNAME=postgres
DB_PASSWORD=your_postgresql_password
```

Thay username/password mẫu bằng thông tin của PostgreSQL trên máy bạn. Không
đưa mật khẩu thật lên Git. Có thể đặt `DB_URL`, `DB_USERNAME` và `DB_PASSWORD`
thành environment variables thay cho `.env`; giá trị environment được ưu tiên
nếu đồng thời có trong `.env`.

## 5. Biên dịch và chạy giao diện Home

Mở PowerShell tại thư mục gốc project rồi chạy:

```powershell
$files = Get-ChildItem .\src -Recurse -Filter *.java |
    ForEach-Object { $_.FullName }

javac -encoding UTF-8 -cp ".\lib\*" -d ".\out" $files
java -cp ".\out;.\lib\*" com.oop.project.ui.Home
```

Lệnh đầu tiên biên dịch toàn bộ file Java trong `src` vào thư mục `out`. Lệnh
thứ hai mở giao diện chính của ứng dụng.

## 6. Lưu ý khi nhập liệu

- Các trường bắt buộc được đánh dấu `*`; các trường khác có thể để trống nếu
  không có thông tin.
- Điện thoại khách hàng chỉ chấp nhận chữ số và phải có đúng 10 chữ số;
  biểu mẫu báo lỗi ngay khi giá trị chưa hợp lệ.
- Email khách hàng có thể để trống; nếu nhập, biểu mẫu kiểm tra định dạng và
  hiển thị cảnh báo ngay dưới trường nhập.
- **Device type** được chọn từ danh sách có sẵn thay vì nhập tên tùy ý.
- Các số trong bảng và tổng báo giá được hiển thị với dấu phân tách hàng nghìn,
  ví dụ `2,000.00`. Đây là định dạng hiển thị; giá trị lưu trong database không
  bị thay đổi.

## 7. Thử import bảo hành legacy với dữ liệu mẫu

File legacy dùng UTF-8, mỗi dòng có dạng `serialNumber|STATUS|description`, với
`STATUS` là `COVERED`, `EXPIRED` hoặc `NOT_COVERED`. Serial phải trùng thiết bị
trong database. File mẫu `src\com\oop\project\adapter\warranty.txt` dùng các
serial `IP13000`, `SN123456`, `IP13003` không có trong dữ liệu mẫu, nên các
dòng đó sẽ không được lưu.

Dữ liệu mẫu có các serial `DK-0001`, `PN-7788`, `LG-2031`, `DL-5521`. Hai thiết
bị đầu đã có bảo hành legacy mẫu, nên để thử nhập mới hãy tạo một file UTF-8,
ví dụ `legacy-test.txt`:

```text
LG-2031|COVERED|Thay quạt dàn lạnh
DL-5521|EXPIRED|Thay pin laptop
```

Trong tab Warranty chọn **Import legacy file**, chọn file này, nhập ngày bắt
đầu/kết thúc ở bảng review rồi xác nhận.

## 8. Lỗi thường gặp

| Triệu chứng | Nguyên nhân và cách xử lý |
|---|---|
| `Missing DB_USERNAME or DB_PASSWORD` | Thiếu thông tin trong `.env` hoặc biến môi trường. Kiểm tra bước 4 |
| `Cannot read the project-root .env file` | Chạy ứng dụng không phải từ thư mục gốc project. `.env` được đọc theo đường dẫn tương đối, nên chạy lệnh `java` tại thư mục gốc |
| `ClassNotFoundException: org.postgresql.Driver` hoặc lỗi không tìm thấy driver | Chưa có `lib\postgresql-42.7.14.jar`, hoặc thiếu `-cp ".\out;.\lib\*"` khi chạy |
| `Connection ... refused` | Dịch vụ PostgreSQL chưa chạy, sai cổng hoặc sai `DB_URL` |
| `relation ... does not exist` | Chưa chạy `schema.sql` trên database đang dùng, hoặc `DB_URL` trỏ sai database |
| Tiếng Việt hiển thị sai khi biên dịch | Biên dịch thiếu `-encoding UTF-8` |
