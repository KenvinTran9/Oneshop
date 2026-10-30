# Hiệu năng Render Free và dọn mã cũ

## Đo trước thay đổi (30/10/2026)

Các GET HTTPS từ máy phát triển, theo thứ tự: `/healthz` 13,97 giây, `/` 8,38 giây, `/products` 6,22 giây, `/healthz` lần sau 2,19 giây.
Đây là vài mẫu gồm cả mạng và server, không phải p95 hoặc cam kết hiệu năng. Không có số đo CPU/log cold start từ Render để kết luận toàn bộ thời gian này là do một nguyên nhân.

[Render Free](https://render.com/docs/free) ngủ sau 15 phút không có request và có thể mất khoảng một phút để thức dậy. Dọn file không loại bỏ giới hạn này.
Kết nối database ở region xa ứng dụng cũng làm mỗi lượt SQL tốn thêm thời gian; đối chiếu region của Render với Aiven khi chọn nơi chạy.

## Những gì đã giảm

- Trang chủ: 10 xuống 6 câu SQL trên cùng catalog 100 sản phẩm/5 danh mục. MySQL xếp hạng ứng viên theo danh mục trong một truy vấn; chỉ nạp tối đa 16 ứng viên và vẫn chọn đủ 8 gợi ý cân bằng.
- Bỏ truy vấn thống kê tổng sản phẩm/thương hiệu và dữ liệu banner cũ không còn hiển thị. Trang danh mục mặc định không đọc lại toàn bộ danh mục chỉ để đặt tiêu đề.
- Counter giỏ tài khoản dùng một SUM, không hydrate giỏ/sản phẩm/chọn màu. Header gộp hai fetch thành `/account/header`; response cá nhân vẫn no-store.
- File công khai có security chain riêng với NullSecurityContextRepository; không chạy đăng nhập/JWT/session management. Spring MVC không đọc flash message cho những file đó. Tắt ServletRequestHandledEvent không có listener sử dụng vì việc tạo event tự đọc session ID.
- Test HTTP với cookie JDBC session xác minh tải ảnh, CSS và JavaScript không gọi session repository; request header/đăng nhập vẫn dùng session như bình thường.
- Prod bật nén nội dung văn bản/SVG, cho phép browser lưu asset và revalidate bằng no-cache để cập nhật tài nguyên sau deploy. HTML/tài khoản/đơn vẫn giữ no-store.
- Smoke test profile prod với Aiven: HTML danh sách sản phẩm 29.165 byte còn 4.942 byte khi client chấp nhận gzip; kiểm tra asset revalidation thành công.
- Gỡ bộ giỏ localStorage, toast và hộp checkout giả; giữ số lượng, chọn màu, ảnh zoom, sắp xếp và POST giỏ backend. `shop.js` từ 16.247 byte xuống 2.779 byte.
- Xóa công cụ chuyển dữ liệu cũ, tài liệu database cũ và test chỉ dành cho giỏ UI trước đây. POM/JAR không có Mongo/BSON; các tài liệu đó vốn không chạy trong backend.
- Xóa hai ảnh chiến dịch không dùng, tổng 189.290 byte, sau khi kiểm tra cả source, catalog, sản phẩm, màu và ảnh đơn hàng ở local/Aiven.

## Xác minh

`CatalogQueryBudgetTest` giữ ngân sách SQL và xác nhận counter không load entity. `JdbcSessionHttpTest` kiểm tra file công khai không đọc JDBC session, session đăng nhập vẫn hoạt động và logout vẫn xóa session.
Các kiểm tra giỏ hàng, quyền admin, xử lý đơn, mật khẩu/JWT và ảnh màu tiếp tục chạy trong `mvn verify` trên schema test riêng.

Muốn so thời gian trên Render sau thay đổi, push commit rồi đợi deploy hoàn tất và đo lại cùng URL, cùng trạng thái đăng nhập, cùng mạng. Bản local nhanh hơn không chứng minh bản Free có cùng độ trễ.
