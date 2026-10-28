# Giỏ hàng theo màu và quy cách

Mỗi dòng được nhận diện bằng `productId + size + shade`. Khách vãng lai lưu `Map<CartSelection, Integer>` trong session; tài khoản lưu các dòng nhúng trong một MongoDB document `shopping_carts`, có unique index theo tài khoản. Các route cập nhật/xóa vẫn dùng product ID nhưng phải gửi cả `size` và `shade` của đúng dòng. Mọi mutation giữ CSRF và giới hạn theo tài khoản/session hiện tại. Document có version; khi xung đột cập nhật, máy chủ tải lại rồi thử lại có giới hạn.

Máy chủ kiểm tra lựa chọn theo danh mục hiện tại, không nhận giá hoặc URL ảnh từ trình duyệt. Một lựa chọn duy nhất có thể được điền tự động; nhiều lựa chọn yêu cầu khách chọn rõ. Ảnh riêng của màu được lấy từ `ProductShade` có `imageType=photo`; bảng màu chung hoặc thiếu ảnh dùng ảnh sản phẩm. Giá/tổng tiền được tính lại khi mở giỏ.

Tồn kho hiện ở cấp sản phẩm. Cả thêm và cập nhật đều kiểm tra tổng số lượng của tất cả màu/quy cách trong giỏ với tồn chung; đây chưa phải hệ thống giữ hàng hoặc tồn kho riêng từng biến thể. Khi đăng nhập, gộp đúng lựa chọn (tối đa 99 mỗi dòng); hàng hết tồn hoặc lựa chọn đã ngừng được giữ với cảnh báo, không âm thầm thay màu. Session cũ dùng product ID được đọc lại; dòng cũ không có màu được yêu cầu chọn lại nếu có nhiều màu. Giỏ localStorage của phiên bản UI trước không được nhập tự động.

## Database MongoDB

App dùng MongoDB cho mọi profile. Giỏ tài khoản cùng các lựa chọn được giữ sau khi khởi động lại Java; giỏ khách vẫn theo vòng đời HTTP session. Không có collection dòng giỏ riêng và không cần migration SQL của phiên bản trước. Xem [mongodb.md](mongodb.md) cho cấu trúc collection, seed local và phạm vi giao dịch. Bộ nạp không tự chuyển dữ liệu từ SQL hoặc ghi đè database khác.

## Kiểm tra

`CartControllerTest` kiểm tra tách/gộp lựa chọn, gộp khi đăng nhập, CSRF, quyền sở hữu, lựa chọn không hợp lệ, tổng tồn chung, giá/ảnh do máy chủ quyết định, session cũ và màu đã ngừng. Bài kiểm tra render tạo `target/cart-preview/fragment.html` với dữ liệu thử để kiểm tra bố cục; không thêm tồn kho giả vào danh mục dev.

`cart-page.js` chỉ tăng/giảm số lượng trong ô nhập và nhắc bấm Cập nhật; việc lưu và tính tiền vẫn qua form POST. Giỏ dùng được khi không có JavaScript. Thanh toán và quản lý tồn riêng từng biến thể nằm ngoài bước này.

18 kiểm thử giỏ hàng được giữ và chuyển sang MongoDB thật. Kiểm tra trình duyệt ở desktop, 390 và 320 px: hai dòng có đúng ảnh màu, nút tăng/giảm giữ giới hạn 1–99 và nhắc cập nhật, bố cục không tràn ngang. Kiểm tra lưu trữ bổ sung nằm trong `MongoPersistenceTests`.
