# Chuyển khoản và vận chuyển

## Luồng xử lý

1. Khách chọn COD hoặc chuyển khoản ở checkout. Máy chủ tính tổng tiền và lưu phương thức thanh toán.
2. Với chuyển khoản, trang theo dõi hiển thị ngân hàng, số tài khoản, chủ tài khoản, QR, tổng tiền và nội dung bằng mã đơn bỏ dấu gạch ngang.
3. Khách bấm **Tôi đã chuyển khoản**. Thanh toán đổi từ `UNPAID` sang `REPORTED`, chưa được coi là đã nhận tiền.
4. Admin vào `/admin/orders/{code}`, kiểm tra tài khoản ngân hàng, nhập mã giao dịch và chọn **Đã nhận đủ tiền chuyển khoản**. Thanh toán đổi sang `PAID`.
5. Admin xác nhận đơn trước (không phụ thuộc đối soát). Sau khi đối soát tiền chuyển khoản, admin hoặc quản lý chi nhánh mới bắt đầu chuẩn bị hàng, lưu shipper và bàn giao. Khách xác nhận đã nhận hàng để hoàn thành đơn. Backend chặn bỏ bước và chặn admin hoàn thành thay khách.
6. Cửa hàng nhập đơn vị vận chuyển, mã vận đơn, tên/số điện thoại shipper, ngày giao dự kiến và ghi chú cho khách. Lưu thông tin shipper không tự chuyển trạng thái đơn.

COD vẫn được xử lý theo luồng cũ; khi xác nhận đã giao hàng, hệ thống ghi nhận đã thu tiền COD. Không gửi yêu cầu trừ tiền hoặc chuyển tiền qua ngân hàng từ ứng dụng.

## Quyền và dữ liệu

- Chỉ ADMIN được xác nhận nhận tiền, yêu cầu kiểm tra lại hoặc xác nhận hoàn tiền. Quản lý chi nhánh chỉ cập nhật vận chuyển/trạng thái của cửa hàng mình.
- Khách chỉ xem và báo chuyển khoản trên đơn của chính tài khoản đăng nhập; không nhận email, số tiền hay trạng thái thanh toán từ biểu mẫu khách làm dữ liệu có thẩm quyền.
- Gửi lại thông báo hoặc xác nhận nhận tiền không tạo thêm sự kiện hay trừ kho lần hai. Các thay đổi khóa đơn trong giao dịch.
- Thông tin tài khoản nhận tiền được lưu cùng đơn, để thay đổi cấu hình ngân hàng sau này không làm đổi hướng dẫn của đơn cũ.
- Biểu mẫu shipper chứa phiên bản đơn; từ chối cập nhật nếu đơn vừa có thay đổi, tránh ghi đè dữ liệu mới.
- Trang khách kiểm tra phiên bản đơn mỗi 15 giây khi tab đang hiển thị; tải lại khi có thay đổi. Tab ẩn không polling; lỗi kết nối tăng thời gian chờ tối đa 60 giây. Endpoint chỉ trả tiến trình của đúng chủ đơn và không cache.

## Hủy đơn và hoàn tiền

- Hủy đơn đã thanh toán: hoàn lại tồn kho, đánh dấu `REFUND_PENDING`; chưa coi là đã hoàn tiền.
- Admin chuyển tiền hoàn bên ngoài ứng dụng rồi nhập mã giao dịch để xác nhận `REFUNDED`.
- Nếu khách đã báo chuyển khoản nhưng đơn bị hủy trước đối soát, admin vẫn kiểm tra khoản nhận. Xác nhận đã nhận tiền trên đơn hủy sẽ chuyển sang chờ hoàn, không mở lại đơn.
- Báo cáo doanh thu vẫn chỉ tính đơn hoàn tất, không tính khoản chuyển cho đơn bị hủy hay đang chờ xử lý.

## Cấu hình

Điền các biến sau trong `.env` local hoặc Render Environment:

```properties
BANK_NAME=Ten ngan hang
BANK_ACCOUNT_NUMBER=So tai khoan
BANK_ACCOUNT_HOLDER=Ten chu tai khoan
BANK_QR_IMAGE=/images/payments/ten-anh-qr.png
```

Thiếu thông tin ngân hàng hợp lệ sẽ vô hiệu hóa lựa chọn chuyển khoản; COD vẫn dùng được. `tools/prepare_render.py` chuyển các biến ngân hàng sang `.env.render` cùng cấu hình deploy. Không commit `.env` hay `.env.render`.

Ảnh QR do chủ cửa hàng cung cấp được phục vụ như tài nguyên public theo yêu cầu nhận thanh toán. QR hiện tại chỉ chứa tài khoản nhận; người mua cần nhập đúng số tiền và mã đơn. Ứng dụng chưa nhận webhook ngân hàng hoặc tự xác minh giao dịch. Không dùng thao tác thử nghiệm để khẳng định tiền thật đã được chuyển.

## Database và triển khai

Chạy `src/main/resources/db/order-payment-delivery-mysql.sql` cho database hiện có trước khi chạy bản mới với Hibernate validate. Profile prod cũng chạy script này trước validate. Script chỉ bổ sung cột nếu chưa có, chạy lặp được; cần quyền ALTER TABLE. Đơn cũ không có thông tin thanh toán được đọc theo phương thức COD, đơn cũ đã giao được hiển thị đã thanh toán.

Profile dev có thể dùng Hibernate update. Sao lưu database trước khi cập nhật. Không nhập lại dump hoặc khởi tạo lại dữ liệu mẫu khi đang có dữ liệu thật.

`PaymentDeliveryFlowTest` kiểm tra chặn xử lý trước thanh toán, đối soát lặp, hoàn tiền, dữ liệu shipper, quyền chi nhánh, endpoint tiến trình, tương thích COD, nội dung email và migration trên database kiểm thử riêng.


## Email theo tiến trình đơn hàng

- Mỗi lần chuyển trạng thái thành công (xác nhận, chuẩn bị, đang giao, đã giao hoặc hủy) gửi một email cập nhật tới email tài khoản đã đặt hàng.
- Xác nhận nhận tiền, yêu cầu kiểm tra lại thanh toán, xác nhận hoàn tiền và thay đổi thông tin vận chuyển cũng gửi email riêng.
- Mail giao hàng gồm đơn vị vận chuyển, tên và số điện thoại shipper, mã vận đơn, ngày giao dự kiến và ghi chú nếu có. Nút theo dõi yêu cầu khách đăng nhập tài khoản sở hữu đơn.
- Chỉ gửi sau khi transaction commit. Cập nhật bị từ chối/rollback không gửi mail; lưu lại dữ liệu không thay đổi và xác nhận thanh toán lặp lại không gửi thêm mail.
- Nội dung lấy bản chụp dữ liệu ở thời điểm cập nhật. Mỗi sự kiện có khóa gửi riêng, không dùng lại khóa email đặt hàng thành công của Resend.
- Dùng cấu hình MAIL_ENABLED, MAIL_PROVIDER, MAIL_FROM, APP_BASE_URL và thông tin SMTP/Resend hiện có. Khi dùng Resend với địa chỉ onboarding@resend.dev chưa xác thực tên miền, chỉ thử gửi tới email đăng ký Resend; muốn gửi mọi khách cần xác thực tên miền gửi.
- Lỗi nhà cung cấp email không làm mất thay đổi đơn hàng. Kiểm tra log máy chủ khi không nhận được thư; hiện chưa có hàng đợi lưu bền hoặc tự gửi lại email cập nhật bị lỗi. Trạng thái email đặt hàng ban đầu vẫn được quản lý riêng.


## Khách xác nhận nhận hàng

Trang theo dõi có nút “Tôi đã nhận được hàng” khi đơn đang giao. POST /orders/{code}/received kiểm tra chủ đơn, CSRF và khóa bản ghi trước khi chuyển sang COMPLETED. Đơn hoàn thành có nhãn “Đã giao hàng · Đã hoàn thành”; COD được ghi nhận đã thu tiền, chuyển khoản phải được admin đối soát trước đó. Xác nhận lặp không tạo thêm sự kiện hoặc email, không tính trùng doanh thu. Doanh thu được tính theo thời điểm hoàn thành và không gồm phí giao hàng. Admin và quản lý chi nhánh không được tự chuyển sang COMPLETED; khách sở hữu đơn xác nhận nhận hàng mới hoàn tất đơn.

Email hoàn thành cảm ơn khách và mời góp ý qua SUPPORT_PHONE; nếu chưa cấu hình dùng số cửa hàng phụ trách. Nhập SUPPORT_PHONE vào environment của Render khi deploy để dùng cùng số liên hệ như local. Đã bỏ khung trạng thái kỹ thuật “Email xác nhận” khỏi trang quản trị đơn hàng.


## Thứ tự xử lý bắt buộc

1. Đơn mới: admin xác nhận đơn (NEW → CONFIRMED), kể cả khi chuyển khoản đang chờ đối soát. Việc xác nhận đơn không xác nhận đã nhận tiền.
2. Chuẩn bị: admin chuyển CONFIRMED → PACKING. Với chuyển khoản, phải xác nhận nhận tiền trước bước này; COD không có điều kiện thanh toán trước.
3. Phân công shipper: chỉ nhập/sửa khi PACKING hoặc SHIPPING. Khi PACKING phải lưu đủ tên và điện thoại shipper rồi mới bàn giao (SHIPPING). Lưu thông tin shipper không tự chuyển trạng thái.
4. Nhận hàng: chỉ khách sở hữu đơn đang SHIPPING được xác nhận để hoàn thành. Admin và chi nhánh không thể hoàn thành thay khách. Giai đoạn SHIPPING chưa ghi nhận doanh thu; COMPLETED mới ghi nhận một lần.

Thứ tự này được kiểm tra ở backend, không chỉ ẩn nút giao diện. Thông báo khách đã chuyển tiền được tiếp nhận cả khi NEW và CONFIRMED.
