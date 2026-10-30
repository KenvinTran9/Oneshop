# Tài khoản và quyền đăng sản phẩm

## Sử dụng

- `/register`: tạo tài khoản khách hàng; họ tên, email, điện thoại và mật khẩu được kiểm tra ở backend.
- `/login`: đăng nhập bằng email và mật khẩu. Khách hàng về trang chủ; admin về `/admin`.
- Tài khoản nằm trong bảng `oneshop_mysql.users`. Mật khẩu được băm bằng BCrypt, không lưu nguyên văn. Không có mật khẩu mặc định được ghi trong mã nguồn.
- Email được chuẩn hóa và có chỉ mục duy nhất. Đăng ký trùng email hiển thị lỗi ngay trong form.
- Form có CSRF, nút hiện/ẩn mật khẩu và kiểm tra xác nhận mật khẩu. Backend vẫn kiểm tra khi JavaScript tắt hoặc yêu cầu được gửi trực tiếp.

## Phân quyền

Chi tiết cấu hình JWT/session, CSRF, cookie và giới hạn đăng nhập: [backend-security.md](backend-security.md). Website đọc phiên qua `/account/status`; API `/api/auth/me` chỉ nhận access token Bearer.

Đăng ký công khai luôn cấp vai trò `CUSTOMER`; các trường gửi thêm như `role`, `role.name`, `active` không cấp được quyền quản trị.

Chỉ `ADMIN` được truy cập `/admin`, mở form thêm sản phẩm và gửi yêu cầu tạo, sửa, ngừng kinh doanh sản phẩm. Quyền được kiểm tra cả ở Spring Security và `AdminService`; ẩn menu chỉ là phần giao diện.

Tài khoản admin được cấp riêng. Khi phát triển local, cơ chế có sẵn tạo `admin@oneshop.vn` nếu đặt biến môi trường `DEMO_ADMIN_PASSWORD` trước khi chạy profile `dev`. Chọn mật khẩu riêng; không đưa vào Git. Cơ chế này chỉ tạo tài khoản chưa tồn tại, không đổi mật khẩu hay quyền của tài khoản đã có, và không chạy trong profile `prod`.

## Kiểm thử

Chạy Maven với MySQL local đang hoạt động:

```powershell
mvn verify '-Doneshop.build.directory=C:\OneShop\target\mysql-build' '-DargLine=-Djdk.net.unixdomain.tmpdir=C:\OneShop\target'
```

Các bài kiểm thử dùng schema ngẫu nhiên riêng, không sửa dữ liệu `oneshop_mysql`.

- Đăng ký → đọc tài khoản từ MySQL → đăng nhập bằng mật khẩu thực → kiểm tra quyền khách hàng.
- Admin đăng nhập qua cùng form và truy cập trang đăng sản phẩm.
- Từ chối tài khoản bị khóa, sai mật khẩu và thiếu CSRF; đăng xuất hủy phiên.
- Khách hàng không thể thêm, sửa, đổi trạng thái hoặc ngừng kinh doanh sản phẩm ngay cả khi gửi POST với CSRF hợp lệ.
- Kiểm thử HTTP với Tomcat thật kiểm tra lỗi đăng ký vẫn hiển thị trong giao diện hoàn chỉnh. `DecoratorController` phục vụ đúng hai decorator cho GET/HEAD/POST vì SiteMesh giữ phương thức gốc khi include; static resource mặc định chỉ nhận GET/HEAD và gây lỗi 405 khi form không hợp lệ.

Tham khảo: [Spring Security: request authorization](https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/authorize-http-requests.html), [method security](https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/method-security.html).
