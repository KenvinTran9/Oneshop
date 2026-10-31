# Sản phẩm hết hàng và theo dõi có hàng

## Quản trị

- Trang /admin/products có nút Hết hàng, Có hàng trở lại và Xóa khỏi shop.
- Hết hàng giữ sản phẩm trên trang chủ, danh mục và trang chi tiết; hiển thị nhãn Hết hàng và chặn thêm/đặt hàng. Tồn kho không bị xóa khi đánh dấu hết hàng.
- Xóa khỏi shop là ngừng kinh doanh và ẩn khỏi danh mục công khai; giữ bản ghi để bảo toàn đơn hàng, giỏ và lịch sử kho. Admin có thể mở bán lại.
- Mở bán nhưng tồn khả dụng bằng 0 vẫn hiển thị hết hàng. Thông báo chỉ tạo khi sản phẩm ACTIVE, active và có tồn dương tại cửa hàng đang hoạt động.
- Nhập kho tăng số lượng cho sản phẩm đang OUT_OF_STOCK sẽ mở lại ACTIVE. Có thể bấm Có hàng trở lại nếu tồn thực tế đã được cập nhật.

## Theo dõi và lựa chọn kênh

- Khách đăng nhập, mở sản phẩm hết hàng và bấm Theo dõi sản phẩm. Mặc định chỉ nhận thông báo trên web; checkbox email mặc định bỏ chọn.
- Chọn Nhận thêm thông báo qua email tài khoản mới bật mail; không nhập email tùy ý. Địa chỉ nhận lấy từ tài khoản sở hữu đăng ký.
- Trang /account/watches và menu tài khoản quản lý theo dõi, bật/tắt mail, đánh dấu thông báo đã đọc hoặc bỏ theo dõi.
- Chuông trên thanh điều hướng hiển thị số thông báo chưa đọc của tài khoản hiện tại. Việc mở danh sách không tự đánh dấu đã đọc.
- Mỗi lượt theo dõi nhận một thông báo có hàng. Khi sản phẩm lại hết hàng, khách có thể đăng ký lại. Giới hạn 50 sản phẩm theo dõi mỗi tài khoản.

## Xử lý nền và độ tin cậy

- Worker kiểm tra tối đa 10 đăng ký sẵn sàng mỗi lượt, cách nhau 60 giây khi máy chủ hoạt động. Thông báo web được lưu vào MySQL trước khi thử gửi email, nên lỗi mail không làm mất thông báo web.
- Mail chỉ gửi khi khách đã chọn, dùng cấu hình SMTP/Resend hiện có. Mỗi lượt theo dõi có khóa Resend riêng và khóa bản ghi MySQL khi xử lý để hạn chế gửi lặp giữa các instance.
- Mail lỗi hoặc chưa cấu hình vẫn chờ, thử lại sau ít nhất 5 phút. Chỉ đánh dấu đã gửi khi nhà cung cấp tiếp nhận. SMTP không có bảo đảm gửi đúng một lần nếu tiến trình dừng sau khi SMTP nhận thư nhưng trước khi commit; Resend có idempotency key cho retry.
- Chuông kiểm tra mỗi 60 giây khi tab đang hiển thị, lùi thời gian khi lỗi và ngừng khi hết phiên. Render Free ngủ thì tác vụ nền cũng dừng; tiếp tục kiểm tra khi ứng dụng thức dậy.
- Thông báo không giữ hàng hoặc bảo đảm sản phẩm còn tồn khi khách mở thư/web.

## Schema và kiểm thử

Production chạy migration lặp được tại src/main/resources/db/product-watches-mysql.sql trước Hibernate validate. Bảng product_watches liên kết users và products, lưu lựa chọn email, thời điểm có hàng/đọc/gửi thư và khóa lần gửi. Migration chỉ bổ sung, không xóa dữ liệu cũ. Profile dev dùng Hibernate update.

Kiểm thử dùng schema MySQL riêng: xem sản phẩm hết hàng, chặn giỏ hàng, gỡ khỏi shop, theo dõi lặp, email mặc định tắt, đổi lựa chọn, retry mail, thông báo chưa đọc, CSRF và quyền sở hữu. Công cụ chuyển Aiven đã bổ sung product_watches.
