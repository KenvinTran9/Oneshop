# Đặt hàng, theo dõi và email xác nhận

Luồng dành cho project: giỏ → đăng nhập → `/checkout` → đặt COD → `/orders/{code}`. Khách mở lịch sử tại `/orders` hoặc menu tài khoản “Đơn hàng của tôi”. Cần đăng nhập đúng tài khoản mua; biết mã đơn không đủ quyền xem thông tin cá nhân.

Admin vào `/admin/orders`, lọc trạng thái, xem đơn và cập nhật lần lượt: Chờ xác nhận → Đã xác nhận → Đang chuẩn bị → Đang giao hàng → Đã giao hàng. Mỗi thay đổi ghi vào `order_events` với thời gian thực; trang theo dõi phản ánh dữ liệu admin đã cập nhật, khách bấm “Cập nhật tiến trình” để tải lại. Đây là tiến trình nội bộ, chưa tích hợp vận đơn của hãng giao hàng.

Khách tự hủy được khi NEW. Admin hủy được khi NEW/CONFIRMED/PACKING. Đơn SHIPPING/COMPLETED/CANCELLED không được hủy hoặc quay lùi. Hủy hoàn tồn một lần trong transaction. Kho vẫn theo sản phẩm/cửa hàng, chưa tách màu; tổng số lượng các màu được kiểm tra chung. Mỗi đơn dùng một cửa hàng phải đủ toàn bộ giỏ.

## Bảo toàn dữ liệu

- Giá và tổng tiền tính từ giỏ phía server; phí giao hàng cố định cho project mặc định 30.000 ₫, cấu hình `ORDER_SHIPPING_FEE`.
- Lưu bản chụp tên, ảnh, màu, dung tích, đơn giá và phí giao hàng vào đơn; sửa sản phẩm sau đó không đổi lịch sử mua.
- Khóa tài khoản và tồn kho trong transaction, trừ kho khi tạo đơn thành công, xóa giỏ cùng transaction. Token checkout unique ngăn cùng một lần gửi form tạo/trừ tồn hai lần.
- Email chỉ gửi sau khi commit, qua SMTP hoặc Resend HTTPS tùy cấu hình. Lỗi gửi không xóa đơn hay trả lại giỏ. Trạng thái PENDING/NOT_CONFIGURED/FAILED/SENT lưu ở đơn. Admin có nút gửi lại khi chưa SENT, trừ đơn đã hủy. Nếu ứng dụng dừng giữa commit và gửi email, admin có thể gửi lại đơn PENDING; chưa có hàng đợi tự retry.
- SENT nghĩa là dịch vụ gửi đã tiếp nhận, không bảo đảm vào Inbox. Cần đối chiếu trạng thái ở nhà cung cấp/hộp thư khi retry lỗi không rõ kết quả.
- Các đơn cũ không có customer_email chưa được tự gán cho tài khoản; admin vẫn xem được, không mở tra cứu công khai.

## Gmail SMTP

1. Bật xác minh 2 bước cho Google Account gửi thư.
2. Tạo [App Password](https://myaccount.google.com/apppasswords) nếu tài khoản hỗ trợ. Không dùng mật khẩu đăng nhập Gmail, không gửi App Password trong chat và không commit `.env`.
3. Trong `.env`, điền (giá trị không cần dấu nháy):

```properties
MAIL_ENABLED=true
MAIL_PROVIDER=smtp
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-shop@gmail.com
MAIL_FROM=your-shop@gmail.com
MAIL_PASSWORD=YOUR_16_CHARACTER_APP_PASSWORD_WITHOUT_SPACES
APP_BASE_URL=http://localhost:8080
ORDER_SHIPPING_FEE=30000
```

4. Restart OneShop. Đặt đơn từ tài khoản khách có email bạn kiểm soát; kiểm tra Inbox/Spam và nút theo dõi. Link yêu cầu đăng nhập đúng tài khoản. `localhost` chỉ dùng được trên máy chạy ứng dụng; khi demo qua tên miền/địa chỉ khác, đổi `APP_BASE_URL` tương ứng.
5. Admin xem trạng thái email trong chi tiết đơn. NOT_CONFIGURED: kiểm tra biến môi trường; FAILED: kiểm tra App Password, xác minh 2 bước và kết nối SMTP; SENT: kiểm tra cả thư rác nếu chưa nhận được.

Mặc định `MAIL_ENABLED=false` để project chạy được khi chưa có Gmail. Không hiển thị “đã gửi email” cho khách nếu chưa gửi thành công. Không dùng địa chỉ/from do người dùng nhập hoặc Host header để xây URL email.

Tham khảo chính thức: [Spring Boot 3.5 mail](https://docs.spring.io/spring-boot/3.5/reference/io/email.html), [Google App Passwords](https://support.google.com/accounts/answer/185833), [Gmail SMTP và TLS](https://developers.google.com/workspace/gmail/imap/imap-smtp).

## Kiểm tra

`OrderFlowTest` dùng schema MySQL riêng và JavaMailSender giả lập: snapshot/giá server, gửi trùng form, lỗi email và retry, quyền sở hữu/CSRF/admin, chuyển trạng thái, hủy/hoàn tồn và hai khách mua cùng lúc. Test không gửi email thật. Profile test luôn tắt mail trừ lớp test có mock sender.

Local `dev` tự thêm bảng/cột qua Hibernate update. `docs/schema/mysql.sql` là snapshot DDL ở thời điểm xuất, cần đối chiếu entity và SQL bổ sung hiện tại. Production cần chuẩn bị schema/migration có sao lưu; không bật dev cùng prod. Cấu hình Resend cho bản deploy và chuyển dữ liệu xem [deploy-render-aiven.md](deploy-render-aiven.md); giới hạn địa chỉ gửi thử áp dụng cả email đơn hàng.
