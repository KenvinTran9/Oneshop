# Giỏ hàng theo màu và quy cách

Mỗi dòng được nhận diện bằng `productId + size + shade`. Khách vãng lai lưu `Map<CartSelection, Integer>` trong session; tài khoản lưu vào `shopping_cart_items` với unique constraint trên cart, product và hai lựa chọn. Các route cập nhật/xóa vẫn dùng product ID nhưng phải gửi cả `size` và `shade` của đúng dòng. Mọi mutation giữ CSRF và giới hạn theo tài khoản/session hiện tại.

Máy chủ kiểm tra lựa chọn theo danh mục hiện tại, không nhận giá hoặc URL ảnh từ trình duyệt. Một lựa chọn duy nhất có thể được điền tự động; nhiều lựa chọn yêu cầu khách chọn rõ. Ảnh riêng của màu được lấy từ `ProductShade` có `imageType=photo`; bảng màu chung hoặc thiếu ảnh dùng ảnh sản phẩm. Giá/tổng tiền được tính lại khi mở giỏ.

Tồn kho hiện ở cấp sản phẩm. Cả thêm và cập nhật đều kiểm tra tổng số lượng của tất cả màu/quy cách trong giỏ với tồn chung; đây chưa phải hệ thống giữ hàng hoặc tồn kho riêng từng biến thể. Khi đăng nhập, gộp đúng lựa chọn (tối đa 99 mỗi dòng); hàng hết tồn hoặc lựa chọn đã ngừng được giữ với cảnh báo, không âm thầm thay màu. Session cũ dùng product ID được đọc lại; dòng cũ không có màu được yêu cầu chọn lại nếu có nhiều màu. Giỏ localStorage của phiên bản UI trước không được nhập tự động.

## Database đang tồn tại

Dev H2 in-memory tạo schema mới khi khởi động. Database đã có dữ liệu cần áp dụng đúng script trong `docs/migrations/cart-selections/` **trước khi chạy bản mới**; Hibernate `update` không tự xóa unique constraint cũ. Các script thêm cột mặc định rỗng để giữ dòng cũ, rồi thay constraint chỉ theo product bằng constraint theo lựa chọn. Không đoán màu cho dòng cũ. Sao lưu, ngừng ghi trong lúc chuyển đổi và kiểm tra tên constraint/schema thực tế trước khi áp dụng. Script không được chạy tự động; chưa xác minh trên các máy chủ MySQL/PostgreSQL/SQL Server thực tế. Production vẫn dùng `ddl-auto=validate`.

## Kiểm tra

`CartControllerTest` kiểm tra tách/gộp lựa chọn, gộp khi đăng nhập, CSRF, quyền sở hữu, lựa chọn không hợp lệ, tổng tồn chung, giá/ảnh do máy chủ quyết định, session cũ và màu đã ngừng. Bài kiểm tra render tạo `target/cart-preview/fragment.html` với dữ liệu thử để kiểm tra bố cục; không thêm tồn kho giả vào danh mục dev.

`cart-page.js` chỉ tăng/giảm số lượng trong ô nhập và nhắc bấm Cập nhật; việc lưu và tính tiền vẫn qua form POST. Giỏ dùng được khi không có JavaScript. Thanh toán và quản lý tồn riêng từng biến thể nằm ngoài bước này.

Kết quả: 52 kiểm thử Java đạt (18 kiểm thử giỏ hàng); đóng gói `mvn package -DskipTests` thành công. Kiểm tra trình duyệt với dữ liệu render từ test ở desktop, 390 và 320 px: hai dòng có đúng ảnh màu, nút tăng/giảm giữ giới hạn 1–99 và nhắc cập nhật, bố cục không tràn ngang. Ảnh kiểm tra nằm ở `target/ui-cart-variants-desktop.png` và `target/ui-cart-variants-mobile.png`; đây là dữ liệu thử, không phải tồn kho của shop.
