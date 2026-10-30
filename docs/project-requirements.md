# Đối chiếu yêu cầu công nghệ OneShop

Đối chiếu theo danh sách yêu cầu người dùng gửi. Link tài liệu LMS cần truy cập được riêng; chưa khẳng định đã kiểm tra toàn bộ rubric trong tài liệu đó.

| Yêu cầu | Trạng thái trong dự án | Bằng chứng / giới hạn |
|---|---|---|
| Spring Boot | Có | `pom.xml`, application, MVC controller và service |
| Thymeleaf | Có | `templates/pages`, form binding và validation phía server |
| Bootstrap | Có | CSS/JS Bootstrap 5 được nạp ở decorator |
| JPA | Có sau khi chuyển đổi | JPA entities, repositories, transaction, auditing và Hibernate |
| SQL Server / MySQL / PostgreSQL | Chọn MySQL | Driver MySQL; bản local chạy MySQL 8.4.13. Hiểu danh sách này là các lựa chọn database, không phải bắt buộc cả ba; cần đối chiếu rubric nếu giảng viên yêu cầu khác |
| Decorator SiteMesh | Có | Cấu hình mapping storefront/admin, decorator và bộ điều khiển hỗ trợ render form POST |
| JWT | Có cho API | `/api/auth/login`, `/api/auth/refresh`, JWT service và Bearer filter. Form đăng nhập Thymeleaf hiện dùng session; không nên trình bày rằng mọi luồng web đều là JWT |
| Cloudinary | Có tích hợp upload ảnh | Admin gọi service upload ảnh; test dùng mock. Môi trường chạy cần cloud name/API key/API secret hợp lệ; cấu hình bí mật không được đưa vào tài liệu. Cần upload thật trên môi trường báo cáo để xác nhận dịch vụ hoạt động |
| Mô hình chuỗi cửa hàng | Có nền tảng dữ liệu | Store, StoreStock theo từng cửa hàng, danh sách cửa hàng và liên kết đơn hàng. Chưa hoàn chỉnh quản trị chi nhánh, điều chuyển kho và phân quyền nhân viên theo chi nhánh |

## Phần nghiệp vụ còn cần hoàn thiện

1. Đã có checkout COD giao hàng; backend tự chọn cửa hàng đủ toàn bộ giỏ, admin có thể phân công lại khi đơn còn NEW. Khách không chọn cửa hàng. Kiểm tra/trừ tồn trong transaction và hoàn tồn khi hủy. Chưa có thanh toán qua cổng, nhận tại cửa hàng hoặc phân bổ một đơn cho nhiều kho.
2. Đã có lịch sử đơn riêng từng khách, timeline, quản trị trạng thái và khôi phục mật khẩu qua email. Gmail SMTP dùng cho local; bản deploy hỗ trợ Resend HTTPS, cần cấu hình và đáp ứng giới hạn người nhận. Xem [orders.md](orders.md), [password-reset.md](password-reset.md) và [deploy-render-aiven.md](deploy-render-aiven.md).
3. Quản trị cửa hàng/tồn kho theo chi nhánh, bổ sung quyền nhân viên nếu rubric yêu cầu.
4. Kiểm tra upload Cloudinary thật trên môi trường báo cáo; ảnh catalog/banner đi kèm vẫn có thể dùng file static, ảnh upload mới dùng Cloudinary.
5. Chuẩn bị báo cáo ERD, sơ đồ luồng, tài khoản demo và ví dụ gọi API JWT phù hợp với chức năng đã triển khai.

Các chức năng đã có: danh mục 100 sản phẩm, lọc/tìm kiếm/sắp xếp, chi tiết và ảnh theo màu, giỏ session/tài khoản, đăng ký/đăng nhập và chỉ admin được tạo/sửa/ngừng kinh doanh sản phẩm. Những phần còn thiếu ở trên chưa được ngầm coi là đã hoàn tất trong bước chuyển database.
