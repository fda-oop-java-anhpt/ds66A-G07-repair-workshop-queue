# Reflection – Repair Workshop Queue

> Phần đóng góp và tự đánh giá dưới đây cần được từng thành viên/nhóm xác nhận.
> Không thể xác định chính xác từ mã nguồn nên chưa điền thay nhóm.

## Contribution

| Thành viên | Phần việc đã thực hiện | Bằng chứng / ghi chú |
|---|---|---|
| Dương Minh Anh | [Nhóm điền] | [Nhóm điền] |
| Hà Thu Hà | [Nhóm điền] | [Nhóm điền] |
| Đinh Vân Anh | [Nhóm điền] | [Nhóm điền] |

## Design Decisions

- **Vì sao dùng kế thừa?** Trong mã hiện tại, các repository JDBC dùng
  `AbstractJdbcRepository` để chia sẻ hạ tầng truy vấn và transaction. Các
  hành vi nghiệp vụ chính được tách qua interface như `JobService` và các
  repository, thay vì dùng kế thừa để biểu diễn từng loại job.
- **Điều gì khó nhất trong quá trình phát triển?** [Nhóm tự bổ sung trải
  nghiệm thực tế.]
- **Điều nhóm sẽ cải thiện nếu tiếp tục phát triển?** [Nhóm tự bổ sung.]
