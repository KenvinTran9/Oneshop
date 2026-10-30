# Bảo mật backend OneShop

Rà soát/cập nhật ngày 30/10/2026. Cấu hình ở `SecurityConfig` có hai filter chain:

## Website dùng session và CSRF

- Trang bán hàng công khai; checkout, lịch sử và chi tiết đơn cần đăng nhập. Khách chỉ đọc/hủy đơn của mình. Giỏ hàng lấy danh tính từ principal, không nhận email chủ giỏ từ trình duyệt.
- `/admin/**` chỉ dành cho ADMIN, kiểm tra thêm ở service quản trị sản phẩm và các thao tác quản trị đơn. Đăng ký công khai luôn cấp CUSTOMER, bỏ qua trường role/active do client gửi thêm.
- Form thay đổi dữ liệu yêu cầu CSRF, gồm đăng nhập, đăng ký, giỏ, đơn, upload và đăng xuất. GET `/logout` chỉ hiện trang xác nhận tiếng Việt; POST `/logout` có token mới hủy session và cookie. Session ID đổi sau đăng nhập để chống session fixation.
- UI đọc trạng thái phiên tại GET `/account/status`. Endpoint này trả 401 nếu chưa đăng nhập và không nhận Bearer token để tạo phiên.
- Cookie session HttpOnly, SameSite=Lax, chỉ truyền qua cookie (không URL rewriting), hết hạn sau 30 phút không hoạt động. Profile prod đặt Secure=true nên phải truy cập qua HTTPS.

## API JWT không dùng session

- POST `/api/auth/login` nhận JSON email/password, trả access/refresh token. Email được trim/chuyển chữ thường; DTO giới hạn độ dài và che password/token trong toString.
- POST `/api/auth/refresh` chỉ nhận refresh token hợp lệ. GET `/api/auth/me` yêu cầu access token ở header `Authorization: Bearer ...`; cookie web không đăng nhập được API.
- API không tạo/lưu session và không tự nhập giỏ khách từ cookie khi gọi login. Việc nhập giỏ web vẫn nằm trong luồng form đăng nhập.
- Chỉ chain API tắt CSRF, vì xác thực bằng token gửi rõ ràng trong header/body, không dựa trên cookie hoặc HTTP Basic. Website vẫn bật CSRF đầy đủ. Không bật CORS wildcard.
- Token kiểm tra chữ ký, issuer, hạn dùng, đúng loại access/refresh và tài khoản còn hoạt động; lấy quyền hiện tại từ database. Refresh token không thể dùng thay access token.
- Sai thông tin xác thực trả JSON 401 chung; JSON/validation sai trả 400, không trả stacktrace hay thông tin chi tiết phân biệt tài khoản tồn tại. Lỗi phân quyền API trả JSON 403.

## Giới hạn yêu cầu và header

- Mỗi địa chỉ kết nối trực tiếp: tổng 15 POST login/phút (web + API dùng chung), 5 POST đăng ký/phút, 30 POST refresh/phút. Vượt ngưỡng trả 429 và Retry-After. Cache tối đa 10.000 bucket, tự loại bucket hết hạn. Không đọc X-Forwarded-For do người gọi tự gửi để vượt giới hạn.
- CSP chặn inline script, object/embed, base và gửi form khác origin; chỉ cho script Bootstrap CDN hiện có. Style inline vẫn cho phép vì Bootstrap/template đang dùng. Cho ảnh HTTPS, Google Fonts và font Bootstrap. X-Frame-Options DENY, frame-ancestors none, nosniff, referrer policy và permissions policy được đặt trên cả web/API. Không đưa secret vào template hoặc JS.
- Mật khẩu BCrypt; email normalized có unique constraint trong MySQL và bắt cả lỗi đăng ký đồng thời. Kiểm tra xác nhận mật khẩu, tối thiểu 8 ký tự, tối đa 72 byte BCrypt. Không yêu cầu mật khẩu phải duy nhất giữa các tài khoản.
- Upload chỉ admin, kiểm tra chữ ký file ảnh và kích thước tối đa 5 MB trước khi gửi Cloudinary. Giá/tổng đơn và tồn kho do backend tính/kiểm tra, không tin giá client gửi.

## Khi deploy

Thiết lập profile `prod`, HTTPS, JWT secret/issuer/expiry, database, Cloudinary, mail và APP_BASE_URL qua biến môi trường. Không bật dev cùng prod; không commit `.env`. `server.port` nhận PORT của hosting. Database prod dùng validate, cần chuẩn bị schema trước.

Giới hạn hiện tại cần tính khi đưa lên nhiều instance:

- Throttle là bộ nhớ từng instance; cần giới hạn tập trung ở WAF/API gateway hoặc kho dùng chung cho nhiều máy. Sau reverse proxy, phải cấu hình proxy tin cậy đúng theo hosting trước khi dựa vào IP khách; không bật tin mọi forwarding header.
- Profile prod lưu session web/giỏ khách và CSRF trong MySQL qua Spring Session JDBC, cookie Secure/HttpOnly/SameSite=Lax. Local dev giữ servlet session trong bộ nhớ. Giỏ tài khoản và đơn lưu MySQL.
- JWT có phiên bản thông tin đăng nhập để thu hồi access/refresh token sau khi đặt lại mật khẩu; chưa có refresh-token rotation. Đăng xuất web chỉ hủy session web, không thu hồi token API. Xem [quên mật khẩu](password-reset.md).
- Đây là tăng cường các luồng hiện có, chưa phải chứng nhận pentest hay cam kết an toàn tuyệt đối.

## Tài liệu đối chiếu

- https://docs.spring.io/spring-security/reference/servlet/authentication/logout.html
- https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html
- https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html

Kiểm thử chạy trên schema MySQL UUID riêng do MySqlIntegrationTestSupport tạo/xóa. Email tắt hoặc mock; không tạo đơn/gửi email thật trong database đang dùng.

Kết quả ngày 30/10/2026: bộ đầy đủ 90 kiểm tra đạt, gồm 11 kiểm tra mới về cô lập session/JWT, loại token, tài khoản bị khóa, lỗi JSON, logout/CSRF, session fixation, header và throttle. Sau đó bổ sung một kiểm tra HTTP thật và chạy lại AccountPageHttpTest: cả hai kiểm tra HTTP đạt (tổng cộng 91 trường hợp khác nhau đã xác minh trên cùng mã backend). Các kiểm tra hiện có về email trùng, mass assignment quyền, giỏ hàng, đơn của người khác và CRUD admin cũng đạt.
