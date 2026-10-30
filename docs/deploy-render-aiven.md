# OneShop: Render + Aiven MySQL

Spring Boot phục vụ cả Thymeleaf và backend trong **một Docker Web Service**.
Aiven lưu tài khoản, sản phẩm, giỏ, đơn và session; Cloudinary lưu ảnh upload.

## 1. Aiven

Tạo dịch vụ MySQL với **plan Free**, không nhầm Developer dùng trial credit.
Banner trial của tài khoản không xác định plan của dịch vụ; kiểm tra plan trong Service settings.
Đợi service Running. Trong Overview lấy Host, Port, Database name, User, Password và tải CA certificate.

Copy `deploy/aiven.env.example` thành `.env.aiven`, điền giá trị trực tiếp trên máy.
Lưu CA vào `backups/aiven-ca.pem`; `AIVEN_CA_CERT` chứa **đường dẫn**, không chứa nội dung chứng chỉ.
Các file này bị loại khỏi Git. Không gửi mật khẩu trong chat.

## 2. Chuyển dữ liệu local

Cần Python và MySQL 8.4 client (`mysql`, `mysqldump`). Chạy từ thư mục repo:

```powershell
python tools/migrate_aiven.py
python tools/migrate_aiven.py --migrate
python tools/prepare_render.py
```

Lệnh đầu chỉ đọc và xác minh TLS/hostname. Lệnh thứ hai chỉ import khi Aiven **chưa có bảng**.
Nó xuất snapshot 14 bảng ứng dụng, lưu backup riêng trong `backups/aiven-migration/`, đối chiếu số bản ghi và giữ nguyên local.
Không migrate session cũ, không chạy DROP, không tự xóa khi có lỗi import một phần.
Nếu import thất bại, kiểm tra database trước khi thử lại; không xóa database đang có dữ liệu.
Muốn bản chụp chính xác, tránh thay đổi sản phẩm/đặt hàng local trong lúc xuất dữ liệu.

`prepare_render.py` tạo `.env.render` riêng, gồm cấu hình Aiven, khóa JWT ngẫu nhiên và Cloudinary từ `.env` local.
Chạy lại sẽ giữ khóa JWT và cấu hình email đã điền. Không commit file này.
Không dùng `.env` local để import vào Render vì nó trỏ MySQL trên máy bạn.

## 3. Push mã và tạo Render Web Service

Đẩy commit triển khai lên GitHub trước. Trong Render:

1. **New → Web Service → Git Provider**, chọn repo OneShop và branch chứa commit vừa đẩy.
2. **Language: Docker**, Root Directory để trống, Dockerfile Path `./Dockerfile`.
3. Chọn **Free**, region gần Aiven nhất có thể. Không cần Build Command/Start Command riêng.
4. Import biến từ **`.env.render`** vào Environment Variables (Add from .env).
5. File `.env.render` đã có `MYSQL_CA_CERT_BASE64` do công cụ tạo từ CA công khai, nên không cần thêm file riêng.
   Nếu nhập biến thủ công, có thể thay biến này bằng **Environment → Secret Files**, Filename **`aiven-ca.pem`**, nội dung gồm toàn bộ BEGIN/END CERTIFICATE. Render mount file thành `/etc/secrets/aiven-ca.pem`.
6. Trong Advanced/Settings, đặt **Health Check Path `/healthz`**. Chọn Deploy Web Service.

Render tự cấp `RENDER_EXTERNAL_URL`; email dùng URL này nếu không đặt `APP_BASE_URL`.
Khi dùng tên miền riêng, đặt `APP_BASE_URL=https://ten-mien-cua-ban`.
`render.yaml` là cấu hình Blueprint thay thế cho tạo thủ công; cần nhập biến và CA base64, nó không tự đọc file trên máy.

Docker không copy `.env`, Git, backup hay test vào image. Chạy user không có quyền root.
`start.sh` tạo truststore PKCS12 từ CA; Connector/J dùng `sslMode=VERIFY_IDENTITY`.
Truststore này chỉ dành cho MySQL, không thay JVM truststore mặc định của HTTPS/Cloudinary/Resend.
Prod dùng Hibernate `validate`, không seed sản phẩm/admin và không tự ghi đè schema ứng dụng.
Hai bảng `SPRING_SESSION*` được tạo bổ sung nếu thiếu, session hết hạn sau 30 phút không hoạt động.

## 4. Email trên Render Free

Render Free chặn cổng SMTP 25/465/587 nên Gmail SMTP local không dùng cho bản deploy này.
Ban đầu `.env.render` đặt `MAIL_ENABLED=false`; đơn hàng vẫn được lưu.

Đăng ký Resend, tạo API key rồi nhập trực tiếp vào Render `RESEND_API_KEY`.
Đặt `MAIL_PROVIDER=resend`, `MAIL_FROM=onboarding@resend.dev`, `MAIL_ENABLED=true`.
Chưa xác minh tên miền thì **chỉ gửi thử tới email chủ tài khoản Resend**.
Đặt đơn thử bằng tài khoản có email đó; email khác có thể bị nhà cung cấp từ chối.
Ứng dụng đánh dấu FAILED khi gửi lỗi, không báo rằng khách đã nhận email.
Muốn gửi cho mọi khách, xác minh domain trong Resend và đổi MAIL_FROM sang địa chỉ thuộc domain đó.
Không tự gửi lại đơn cũ chỉ vì bật email; cần thao tác gửi lại riêng.

## 5. Kiểm tra sau deploy

- Logs có `Started OneShopApplication`; `/healthz` trả `{"status":"UP"}`.
- Trang chủ, sản phẩm, ảnh, đăng ký/đăng nhập, giỏ và admin hoạt động qua HTTPS.
- Dùng tài khoản admin đã migrate, không tạo tài khoản admin mặc định mới.
- Đơn mới xuất hiện trong Aiven; người khác không xem/sửa được đơn đó.
- Reload/redeploy vẫn đọc được dữ liệu và session chưa hết hạn từ Aiven.
- Khi đã cấu hình Resend, kiểm tra email bằng đúng email chủ tài khoản Resend.

Free service ngủ sau 15 phút không có request, lần truy cập sau có thể chờ khoảng một phút.
Không lưu dữ liệu kinh doanh vào ổ đĩa container vì Render xóa file local khi restart/redeploy.
Ảnh đã có trong `src/main/resources/static` theo image; ảnh admin upload dùng Cloudinary.

## Tài liệu chính thức

- [Render Docker](https://render.com/docs/docker)
- [Environment variables và Secret Files](https://render.com/docs/configure-environment-variables)
- [Giới hạn Render Free](https://render.com/docs/free)
- [Aiven MySQL Free](https://aiven.io/docs/products/mysql/concepts/mysql-free-tier)
- [Resend: giới hạn địa chỉ gửi thử](https://resend.com/docs/knowledge-base/403-error-resend-dev-domain)
