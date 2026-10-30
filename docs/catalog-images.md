# Rà soát ảnh sản phẩm — 30/10/2026

Đã xem trực quan toàn bộ 100 ảnh chính và 60 ảnh màu. Thay 18 ảnh chính có logo/watermark cửa hàng, khung gian hàng hoặc bố cục quảng cáo nhiều chữ, gồm CATALOG-003, 010, 013, 025, 027, 028, 029, 030, 036, 037, 040, 051, 056, 057, 080, 091, 092, 094. Giữ nhận diện thương hiệu thật trên bao bì và tên màu; không tạo ảnh giả cho sản phẩm.

Ưu tiên ảnh từ trang hãng, dùng ảnh packshot từ nhà bán lẻ khi trang hãng không cung cấp ảnh truy cập được. Ảnh Estée Lauder dùng lại ảnh sạch cùng dòng sản phẩm đã có trong thư viện màu. Mỗi ảnh được đối chiếu tên sản phẩm, dung tích và xem trước khi thay. Chỉ đổi định dạng/giảm kích thước tối đa 1200 px, không tẩy watermark, không crop ảnh để giấu logo.

Giữ nguyên đường dẫn `/images/catalog/...` để các sản phẩm đang lưu trong MySQL, giỏ hàng và ảnh chụp đơn hàng dùng ngay ảnh mới sau khi khởi động lại. Không chạy seed ghi đè hay thay giá/tồn kho/chỉnh sửa của admin. Metadata trong catalog JSON được cập nhật cho các lần nhập mới; metadata nguồn cũ trong bản ghi MySQL hiện có không bị sửa.

Chi tiết ảnh, trang đối chiếu, ngày kiểm tra và mã SHA-256 trước/sau ở [catalog-image-audit-2026-10-30.json](catalog-image-audit-2026-10-30.json). Thông tin đối chiếu chỉ phục vụ bảo trì dự án, không thêm nhãn nguồn hay liên kết shop khác trên giao diện bán hàng.
