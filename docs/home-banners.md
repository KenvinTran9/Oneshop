# Banner trang chủ OneShop

> Đây là bố cục banner phiên bản trước. Bố cục hiện tại và bộ ảnh mới được mô tả tại [editorial-storefront.md](editorial-storefront.md); một số ảnh cũ vẫn dùng cho danh mục.

Tạo ngày 30/10/2026 bằng công cụ image_gen tích hợp (built-in), ba ảnh ngang 1536 × 1024. Đây là ảnh trang trí không thương hiệu, không đại diện một sản phẩm cụ thể trong danh mục. Bản WebP chất lượng 87 giữ nguyên kích thước, không crop hay sửa bố cục ảnh gốc. Ảnh cũ beauty-editorial-v1 vẫn được giữ cho trang danh mục/đăng nhập.

File trong dự án:
- `src/main/resources/static/images/home/banner-skincare-v1.webp`
- `src/main/resources/static/images/home/banner-makeup-v1.webp`
- `src/main/resources/static/images/home/banner-serum-v1.webp`

Banner tự đổi sau 5000 ms. Nút trước/sau và chấm chọn ảnh hỗ trợ bàn phím. Nút tạm dừng đã được bỏ theo yêu cầu giao diện. Tự chuyển tạm dừng khi trỏ chuột vào, focus trong banner, cuộn khỏi banner hoặc tab nền; tắt theo prefers-reduced-motion. Khi không có JavaScript, banner đầu và các liên kết vẫn dùng được. Chữ/CTA là HTML, không nằm trong ảnh; liên kết mở đúng danh mục. Không thêm khuyến mãi hay đánh giá chưa có dữ liệu. Bố cục desktop là một ô lớn và hai ô nhỏ bên phải, mobile xếp các ô phụ bên dưới. Khối tổng số sản phẩm/thương hiệu đã được bỏ khỏi hero.

## Bộ prompt cuối cùng

### skincare

Use case: ads-marketing. Asset type: wide photographic hero banner for OneShop cosmetics website. Create a premium photorealistic skincare still life, landscape composition 1536x1024. Warm luminous ivory background, pale cream stone plinths and gentle window sunlight, soft blush reflections. On the RIGHT HALF arrange a large elegant unbranded frosted glass serum bottle with charcoal dropper, an open ivory moisturizer jar with tactile cream, and a delicate cream smear. Keep LEFT HALF mostly clean pale ivory negative space for website text that will be added separately. Sophisticated feminine editorial beauty photography, natural glass reflections, realistic materials, airy and refined, pale rose, cream and charcoal. Objects contained within central vertical 65% for wide responsive crops. No text, no logos, no brand labels, no people, no watermarks, no UI, no discount badges. Decorative campaign artwork, not a named product photograph.

### makeup

Use case: ads-marketing. Asset type: wide photographic makeup banner for OneShop cosmetics website. Create a premium photorealistic beauty editorial still life, landscape 1536x1024. Soft muted dusty rose background, matte blush stone platform and a small drape of pale pink satin. On the RIGHT HALF arrange two unbranded lipsticks in charcoal cases, one rose nude and one muted berry, a fine makeup brush and sculptural rose pigment swatches. Keep LEFT HALF clean pale blush negative space for live HTML typography. Soft side lighting, subtle beautiful shadows, tactile velvet pigment, elegant feminine modern cosmetics campaign. Composition protects central vertical 65% so a wide crop keeps all main products. Refined light palette, never bright orange/red sale graphics. No text, logos, labels, people, watermarks or UI. Decorative campaign artwork, not a named product photograph.

### serum

Use case: ads-marketing. Asset type: wide serum and skincare photographic campaign banner for OneShop beauty website. Premium photorealistic cosmetics still life, landscape 1536x1024. Pale sage ivory background with gentle morning light, transparent amber glass serum bottle with unbranded black dropper, clear water droplets, one translucent glass dish, a delicate botanical green leaf. Arrange hero objects on the RIGHT HALF on light limestone; LEFT HALF clean pale sage ivory negative space for website copy. Airy, calm, luxurious but approachable beauty editorial, soft natural textures, luminous glass, delicate shadows. Keep hero objects within the central vertical 65% for wide responsive cropping. No text, logos, labels, people, watermarks, graphics or UI. Decorative artwork, no claims about ingredients of real catalog products.

