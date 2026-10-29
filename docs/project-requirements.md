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
| Cloudinary | Có tích hợp, chưa xác minh cloud thật | Admin gọi service upload ảnh; test dùng mock. Các biến cloud name/API key/API secret chưa được cấu hình trên máy khi kiểm tra, cần cấu hình và thử upload thật trước khi báo cáo hoàn tất |
| Mô hình chuỗi cửa hàng | Có nền tảng dữ liệu | Store, StoreStock theo từng cửa hàng, danh sách cửa hàng và liên kết đơn hàng. Chưa hoàn chỉnh quản trị chi nhánh, điều chuyển kho và phân quyền nhân viên theo chi nhánh |

## Phần nghiệp vụ còn cần hoàn thiện

1. Checkout tạo đơn thật, chọn cửa hàng xử lý/nhận hàng hoặc giao hàng, kiểm tra và trừ tồn trong transaction.
2. Quản trị đơn hàng và các trạng thái xử lý; giỏ hiện dẫn tới liên hệ cửa hàng.
3. Quản trị cửa hàng/tồn kho theo chi nhánh, bổ sung quyền nhân viên nếu rubric yêu cầu.
4. Cấu hình Cloudinary và kiểm tra upload thật; hiện ảnh danh mục vẫn dùng file local.
5. Chuẩn bị báo cáo ERD, sơ đồ luồng, tài khoản demo và ví dụ gọi API JWT phù hợp với chức năng đã triển khai.

Các chức năng đã có: danh mục 100 sản phẩm, lọc/tìm kiếm/sắp xếp, chi tiết và ảnh theo màu, giỏ session/tài khoản, đăng ký/đăng nhập và chỉ admin được tạo/sửa/ngừng kinh doanh sản phẩm. Những phần còn thiếu ở trên chưa được ngầm coi là đã hoàn tất trong bước chuyển database.
