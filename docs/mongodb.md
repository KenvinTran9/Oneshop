# MongoDB backend

MongoDB là database duy nhất của bản hiện tại. Không tự nhập dữ liệu từ server SQL khác. Khi chuyển đổi trên máy này, chưa có database OneShop trong MongoDB; bản dev tạo database `oneshop` và nạp danh mục cục bộ. Các database khác trên server không được đọc hoặc thay đổi bởi bộ nạp.

## Collections

`users`, `roles`, `products`, `categories`, `brands`, `stores`, `store_stocks`, `shopping_carts`, `orders`, `order_items`, `_sequences`.

- ID số được cấp bằng `findAndModify` tăng counter nguyên tử trong `_sequences`, giữ kiểu ID và route hiện có. Không đặt lại counter khi khởi động.
- Quan hệ giữa các collection dùng `@DocumentReference` lưu ID. Màu và quy cách nhúng trong sản phẩm; dòng giỏ nhúng trong `shopping_carts`. Không có collection `shopping_cart_items` riêng.
- Giá `BigDecimal` được lưu bằng BSON `Decimal128`, để sắp xếp theo số thay vì chuỗi. Các truy vấn danh mục, đếm thương hiệu và tồn kho hoạt động trực tiếp trên MongoDB.
- Email chuẩn hóa chữ thường. Unique index cho email/SKU/slug có collation không phân biệt hoa thường; role, mã cửa hàng, giỏ theo user và tồn theo cặp cửa hàng/sản phẩm cũng có index duy nhất.
- `@Version` ngăn ghi đè dữ liệu đã thay đổi. Mutation giỏ tải lại và thử lại có giới hạn khi xung đột phiên bản; gộp giỏ khách vào tài khoản được lưu một lần cho toàn bộ document.

Không bật `MongoTransactionManager` trên MongoDB standalone. Hiện tại không có giao dịch nhiều document cho thanh toán/giữ tồn kho; phần đặt hàng trực tuyến chưa triển khai. Một giỏ tài khoản là một aggregate được lưu nguyên tử, tồn kho được kiểm tra nhưng chưa được giữ chỗ. Khi triển khai checkout thực tế, cần thiết kế giữ/trừ tồn và cấu hình replica set/transaction phù hợp, không giả định annotation JPA cũ còn hiệu lực.

## Seed local

`RoleInitializer` tạo role nền khi thiếu. Trong profile `dev`, `DataInitializer` tạo cửa hàng và tài khoản mẫu chỉ khi có mật khẩu từ môi trường; `CatalogDataInitializer` bổ sung 100 SKU; `DevStockInitializer` chạy sau đó, tạo 20 đơn vị mỗi SKU ở `HCM-D1` khi chưa có cặp tồn kho. Không bổ sung lại lượng đã bán hoặc ghi đè quantity hiện tại, kể cả 0. `APP_DEV_SEED_STOCK=false` tắt bước tạo tồn mẫu.

Database `oneshop` giữ dữ liệu sau khi tắt Java. Thông tin quản trị và giỏ tài khoản vẫn còn sau restart. Giỏ khách vãng lai vẫn là HTTP session và có thể mất khi phiên hoặc tiến trình kết thúc; đăng nhập để lưu lâu dài.

## Test và cấu hình

Đặt `MONGODB_URI` cho app; dev mặc định `mongodb://127.0.0.1:27017/oneshop`, prod không có mặc định URI/secret. `.env.example` chỉ là tài liệu, Spring Boot không tự nạp file này.

`MongoIntegrationTestSupport` luôn thay database bằng tên ngẫu nhiên riêng, xác minh tên trước khi dọn fixture và xóa đúng database đó khi JVM test kết thúc. Các bài kiểm thử hiện có được giữ, chuyển từ rollback JPA sang fixture MongoDB độc lập; thêm kiểm tra native decimal, reference, unique index, optimistic locking, ID counter và seed tồn không ghi đè.

Tham khảo thiết kế: [Spring Data document references](https://docs.spring.io/spring-data/mongodb/reference/mongodb/mapping/document-references.html), [Spring Data type mapping](https://docs.spring.io/spring-data/mongodb/reference/mongodb/mapping/mapping.html), [MongoDB single-document atomicity](https://www.mongodb.com/docs/manual/core/write-operations-atomicity/).

## Xác minh ngày 29/10/2026

- `mvn verify` với thư mục build riêng đạt 59 kiểm thử; `mvn package -DskipTests` ở thư mục build mặc định cũng thành công.
- MongoDB `oneshop`: 100 sản phẩm, 59 thương hiệu, 5 danh mục, 100 bản ghi tồn mẫu quantity 20, 2 role. Không còn database thử sau khi JVM kiểm thử kết thúc.
- Khi đã tắt Java, các document vẫn tồn tại. Khởi động lại nhập 0 SKU và tạo 0 bản ghi tồn mới; ID sản phẩm mẫu vẫn là 16.
- Website thật trả HTTP 200. Đã thêm Bare Grape ×1 và Jujube ×2 vào giỏ, giữ đúng ảnh và quy cách, tổng 465.000 ₫; cập nhật Jujube lên 3 ra 620.000 ₫ rồi về 2 ra 465.000 ₫. Ảnh xác minh: `target/mongodb-cart-live.png`.
