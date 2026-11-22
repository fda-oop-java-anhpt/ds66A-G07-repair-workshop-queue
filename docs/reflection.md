# Reflection – Repair Workshop Queue

> Phần của từng thành viên do chính thành viên đó xác nhận. Các ô còn để trống
> (`[]`, `[Nhóm tự bổ sung ...]`) là phần chưa được điền.

## Contribution

| Thành viên | Phần việc đã thực hiện | Bằng chứng / ghi chú |
|---|---|---|
| Dương Minh Anh | Thiết kế logic và Adapter | [] |
| Hà Thu Hà | Thiết kế UI | [] |
| Đinh Vân Anh | Thiết kế Database và tầng Repository (JDBC) | `database/schema.sql`, `database/seed_data.sql`, `repository/`, `config/`; chi tiết bên dưới |

### Đinh Vân Anh – Database và Repository

- **Thiết kế cơ sở dữ liệu:** `schema.sql` với 9 bảng (`customers`, `devices`,
  `repair_jobs`, `estimates`, `parts`, `labor_services`, `status_history`,
  `warranties`, `payments`), khóa ngoại `ON DELETE RESTRICT`, ràng buộc `CHECK`
  và `UNIQUE`, 15 index, trigger `updated_at`, 3 view; `seed_data.sql` theo kịch
  bản demo.
- **Tầng Repository:** interface và hiện thực JDBC cho khách hàng, thiết bị,
  job, estimate, lịch sử trạng thái và bảo hành; `AbstractJdbcRepository` dùng
  chung truy vấn, dịch lỗi và transaction; các `RowMapper`.
- **Transaction:** năm thao tác nguyên tử (tạo job kèm lịch sử, đổi trạng thái
  kèm lịch sử, lưu estimate cùng dòng chi tiết, duyệt/từ chối estimate kèm cập
  nhật job và lịch sử, import warranty legacy).
- **Kết nối và cấu hình:** `DatabaseConnection` (cấu hình bằng `.env` hoặc biến
  môi trường), `AppContext` để giao diện lấy repository và service từ một chỗ.
- **Tài liệu và phối hợp:** hướng dẫn cài đặt, cấu hình và nạp dữ liệu; gộp các
  nhánh phát triển song song và xử lý xung đột.

## Design Decisions

- **Vì sao dùng kế thừa?** Trong mã hiện tại, các repository JDBC dùng
  `AbstractJdbcRepository` để chia sẻ hạ tầng truy vấn và transaction. Các
  hành vi nghiệp vụ chính được tách qua interface như `JobService` và các
  repository, thay vì dùng kế thừa để biểu diễn từng loại job.
- **Điều gì khó nhất trong quá trình phát triển?** [Nhóm tự bổ sung trải
  nghiệm thực tế.] Phần Database/Repository xem bên dưới.
- **Điều nhóm sẽ cải thiện nếu tiếp tục phát triển?** [Nhóm tự bổ sung.] Đề xuất
  cho Database/Repository xem bên dưới.

### Quyết định thiết kế phần Database và Repository (Đinh Vân Anh)

- **`NUMERIC(12,2)` / `BigDecimal` cho tiền, không dùng `FLOAT`/`DOUBLE`.** Kiểu
  dấu phẩy động chỉ lưu xấp xỉ nên cộng nhiều dòng báo giá có thể lệch vài
  đồng. Đánh đổi: code phải dùng `add`, `multiply` của `BigDecimal`.
- **`ON DELETE RESTRICT`, không dùng `CASCADE`.** Lịch sử đơn và báo giá cần
  giữ cho bảo hành và đối soát; xóa nhầm một khách hàng không được làm mất toàn
  bộ dữ liệu liên quan. Đánh đổi: muốn xóa thật phải xóa dữ liệu con trước.
- **Trạng thái lưu bằng `VARCHAR` + `CHECK`, không dùng kiểu `ENUM` của
  PostgreSQL.** Giá trị trùng tên hằng Java Enum (`valueOf`), và thêm trạng thái
  chỉ cần sửa ràng buộc. Đánh đổi: danh sách trạng thái lặp ở vài ràng buộc.
- **Estimate là quan hệ 1-N theo phiên bản** (`UNIQUE (job_id, version)`), và
  mỗi job tối đa một estimate `APPROVED` bằng index duy nhất có điều kiện. Báo
  giá được điều chỉnh nhiều lần nên cần giữ bản cũ; luật "một bản được duyệt"
  được database bảo đảm, không chỉ dựa vào code.
- **Không lưu giá trị suy ra được** (tổng tiền báo giá, hạn chót job). Lưu sẵn sẽ
  có nguy cơ lệch khi dữ liệu gốc đổi.
- **Ràng buộc đặt ở cả model/service và database** (điện thoại 10 chữ số,
  `closed_at` chỉ có khi `CLOSED`, nguồn gốc bảo hành). Database là lớp phòng
  thủ cuối khi có thao tác đi vòng qua service. Đánh đổi: lỗi từ database khó
  đọc, nên `AbstractJdbcRepository` dịch mã lỗi sang thông báo dễ hiểu.
- **Transaction đặt ở tầng repository** (`inTransaction`) thay vì để service tự
  mở kết nối: service không cần biết `Connection`, và tính nguyên tử được bảo
  đảm ở một chỗ. Lịch sử trạng thái ghi bằng code (cần kèm ghi chú, cùng
  transaction với việc đổi trạng thái), còn `updated_at` dùng trigger vì chỉ là
  việc cơ học.
- **`PreparedStatement` cho mọi truy vấn** để chống SQL Injection.
- **`PGSimpleDataSource` + `.env`, không dùng connection pool.** Đủ cho ứng dụng
  desktop một người dùng và biên dịch được chỉ bằng `javac`; đánh đổi là mỗi lần
  lấy kết nối đều mở kết nối mới. Mật khẩu nằm trong `.env`, được `.gitignore`
  chặn.
- **Import bảo hành legacy idempotent:** `legacy_ref` duy nhất và
  `ON CONFLICT DO NOTHING` trong một transaction, nên nhập lại cùng file không
  tạo bản ghi trùng.
- **Schema có view nhưng Java truy vấn trực tiếp bảng.** Khi yêu cầu bảo hành
  chuyển sang khớp theo serial number, truy vấn trong Java dễ chỉnh hơn việc sửa
  view; view còn lại để truy vấn thủ công.

### Phần khó nhất (Database và Repository)

- **Giữ dữ liệu nhất quán khi nhiều bảng đổi cùng lúc.** Đổi trạng thái job phải
  ghi lịch sử, duyệt estimate phải đổi cả estimate lẫn job. Khi tạo phiên bản estimate mới còn phải khóa dòng của job (`SELECT ... FOR UPDATE`) để hai thao tác đồng thời không gán trùng số phiên bản. Cần
  rollback toàn bộ khi một bước lỗi và luôn trả kết nối.
- **Yêu cầu thay đổi muộn.** Bảo hành, thanh toán và việc khớp thiết bị theo
  serial number được thêm sau, kéo theo sửa schema, view, repository và dữ liệu
  mẫu cùng lúc; trong lúc đó có hai bản SQL trôi lệch nhau.
- **Ánh xạ đối tượng lồng nhau** (job chứa thiết bị, thiết bị chứa khách hàng)
  bằng một câu `JOIN` để tránh truy vấn N+1, giữ tên cột trong SQL khớp với
  mapper.
- **Phối hợp nhóm.** Các nhánh phát triển song song gây xung đột khi gộp; bài học
  là chốt hợp đồng dữ liệu sớm và gộp nhánh thường xuyên.

### Đề xuất cải thiện (Database và Repository)

- Bổ sung bộ test tự động cho repository trên database riêng.
- Hoàn thiện luồng thanh toán (model, repository, màn hình) và ràng buộc đóng
  job theo thanh toán, dùng bảng `payments` đã có.
- Dùng connection pool nếu ứng dụng cần nhiều người dùng đồng thời.
- Thống nhất ngôn ngữ thông báo lỗi giữa service và repository.
