# Quên mật khẩu qua email

Người dùng chọn **Quên mật khẩu?** tại `/login`, nhập email đã đăng ký và mở liên kết trong email để đặt mật khẩu mới.
Không tự đăng nhập sau khi đổi; cần đăng nhập lại bằng mật khẩu mới.

## Xác thực và lưu trữ

- Token ngẫu nhiên 32 byte, mã hóa base64url, có hiệu lực 15 phút và chỉ dùng một lần. Mở trang liên kết không tiêu thụ token; POST xác nhận mới tiêu thụ.
- MySQL chỉ lưu SHA-256 của token, không lưu token thô hoặc mật khẩu rõ. Mật khẩu dùng cùng BCrypt/chính sách độ dài 8 ký tự, tối đa 72 byte như đăng ký.
- Mỗi tài khoản có một yêu cầu hiện hành; yêu cầu mới sau thời gian chờ thay thế liên kết cũ. Giới hạn gửi lại 60 giây/tài khoản, thêm giới hạn IP 5 yêu cầu gửi và 10 lần xác nhận/phút/instance.
- Email không tồn tại, tài khoản vô hiệu hóa và lỗi nhà cung cấp cùng nhận thông báo công khai chung. Gửi mail sau commit, qua hàng đợi giới hạn, không chờ SMTP/HTTPS trên request thread.
- Thao tác reset khóa user trước rồi trạng thái token. Hai lần xác nhận đồng thời chỉ một lần thành công.
- `credential_version` tăng sau khi đổi mật khẩu. Session web cũ mất hiệu lực ở request kế tiếp; access/refresh JWT cũ bị từ chối. Phiên và token của tài khoản khác không bị thu hồi.
- Phiên/token được tạo trước khi có tính năng này được coi là phiên bản 0. Xác thực mới chụp phiên bản cùng mật khẩu để không cấp token phiên bản mới cho một lần xác thực bằng mật khẩu cũ đang chạy đồng thời.
- Form dùng CSRF. Trang liên kết đặt lại đặt `Referrer-Policy: no-referrer` và không cache; token, mật khẩu và email tài khoản không đi vào log.

## Cấu hình email

Dùng chung cấu hình gửi email với xác nhận đơn hàng:

- Local: `MAIL_ENABLED=true`, `MAIL_PROVIDER=smtp`, Gmail SMTP và App Password trong `.env`.
- Render: `MAIL_ENABLED=true`, `MAIL_PROVIDER=resend`, `RESEND_API_KEY`, `MAIL_FROM` trong Environment.
- URL liên kết lấy từ `APP_BASE_URL`, hoặc `RENDER_EXTERNAL_URL` do Render cấp; không lấy Host header của request.
- Chưa xác minh domain Resend: chỉ gửi thử tới email chủ tài khoản Resend. Gmail của tài khoản OneShop dùng test phải trùng email đó. Muốn gửi cho khách khác phải xác minh domain và dùng địa chỉ gửi thuộc domain.
- Thay cấu hình mail cần khởi động/deploy lại. Yêu cầu cũ không tự gửi lại; người dùng yêu cầu một liên kết mới sau thời gian chờ.

## Database và deploy

Local dev bổ sung bảng `password_reset_accounts` qua Hibernate update. Prod chạy file `db/password-reset-mysql.sql` trước Hibernate validate.
Script chỉ `CREATE TABLE IF NOT EXISTS`, không đổi/xóa tài khoản, sản phẩm hay đơn. Có khóa ngoại tới `users`.
Bảng lưu cả phiên bản thông tin đăng nhập; không xóa bảng hoặc các dòng đã hết hạn vì sẽ bỏ thông tin thu hồi phiên cũ.
Công cụ migrate Aiven sao chép cả bảng này; backup phải được giữ riêng ngoài Git.

## Kiểm tra

```powershell
mvn test '-Dtest=PasswordResetFlowTest,PasswordResetMailServiceTest' '-Doneshop.build.directory=C:\OneShop\target\mysql-build' '-DargLine=-Djdk.net.unixdomain.tmpdir=C:\OneShop\target'
```

Test dùng MySQL schema riêng và mock mail, không gửi tới khách thật. Có kiểm tra HTTP thật, token hết hạn/thay thế/dùng lại, xác nhận không khớp, CSRF, tiêu thụ đồng thời, đổi mật khẩu và thu hồi web session/JWT.

[OWASP Forgot Password](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html)
