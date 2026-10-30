# OneShop — giao diện editorial beauty

Cập nhật 30/10/2026. Tham khảo nhịp bố cục ảnh lớn, header gọn, wordmark và các hàng sản phẩm tại https://www.glossier.com/. Nội dung tiếng Việt và ảnh chiến dịch được tạo riêng cho OneShop.

## Bố cục và hành vi

Hero tràn chiều ngang với ba ảnh tự đổi sau 5 giây; giữ điều hướng trước/sau, chấm chọn, hỗ trợ bàn phím và prefers-reduced-motion. Tiếp theo là hai hàng tổng cộng tám sản phẩm lấy từ database, cặp ảnh chăm da/trang điểm, khu vực skincare, năm danh mục bằng ảnh, câu chuyện thương hiệu, thương hiệu và cửa hàng. Header có tìm kiếm thu gọn; footer hồng với mặt cười. Trang danh mục dùng cùng màu sắc, ảnh chiến dịch và đường nét vuông.

Giữ nguyên giỏ hàng, đăng nhập, đăng ký, thanh toán và theo dõi đơn. Không thêm khuyến mãi hay đánh giá giả. Ảnh campaign là ảnh trang trí; ảnh sản phẩm trong danh mục vẫn gắn đúng sản phẩm.

## Kiểm tra

Build Maven thành công; 15 kiểm tra hiện có (CatalogIntegrationTests, AuthenticationFlowTest, AccountPageHttpTest) đạt. Kiểm tra trực tiếp viewport desktop 1280 px và mobile 390/320 px: không tràn ngang, menu mở đủ danh mục, tìm kiếm mở/đóng bằng Escape và trả kết quả đúng. Banner chuyển từ slide 1 sang slide 2 sau 5,2 giây; không có nút tạm dừng. Tám thẻ sản phẩm và các ảnh trang chủ tải được, console không có lỗi. Ảnh chụp kiểm tra nằm trong target/ (không đưa vào Git).

## Nguồn tài sản

Bộ ảnh tạo bằng công cụ image_gen tích hợp (built-in), nhân vật người lớn hư cấu, sản phẩm không thương hiệu. Không sử dụng ảnh hay logo của Glossier. File WebP chất lượng 87, giữ nguyên kích thước ảnh gốc, chỉ chuyển định dạng bằng Pillow; không crop hay sửa ảnh sau khi tạo.

- `src/main/resources/static/images/home/editorial-color-v1.webp`
- `src/main/resources/static/images/home/editorial-skin-v1.webp`
- `src/main/resources/static/images/home/editorial-lips-v1.webp`
- `src/main/resources/static/images/home/editorial-ritual-v1.webp`

## Bộ prompt cuối cùng

### editorial-color-v1

Use case: ads-marketing. Asset type: landscape 1536x1024 beauty cosmetics campaign for OneShop website. Photorealistic playful high fashion product still life on glossy pale pink surface and pink background: unbranded translucent rose lip gloss tube, a cherry-red lipstick in sleek chrome packaging, an open small blush compact with berry pink pigment, tiny pink ribbon, swirled cosmetic pigment. Objects arranged sculpturally toward RIGHT TWO THIRDS, interesting bold diagonal shadows and glossy reflections. Editorial direct flash with sophisticated light and shadow, vivid rose and cherry accents against pale pink, clean modern composition with ample left-third pale negative space for live web text. Tactile, realistic glass, metal and pigment. No logos, lettering, labels, sale badges, watermarks, UI or text. Not an image of any named catalog product. Avoid beige stone pedestals and heavy vintage aesthetics.

### editorial-skin-v1

Use case: photorealistic-natural. Asset type: portrait 1024x1536 skincare campaign editorial image for OneShop website. An adult East Asian woman around 30 with natural dewy warm skin, dark hair in a loose bun, gently applying a small dab of white moisturizer to her cheek with two fingers, looking toward camera with calm confident smile. Tight head and shoulders crop, realistic hand anatomy, subtle natural skin texture and freckles, soft pale pink towel around shoulders. Background a dreamy blush pink bathroom tile wall with beautiful morning sunlight and a little glass reflection. High-end beauty magazine photography, feminine luminous clean and candid, modern minimal styling. Face and fingers prominent and unobscured. No text, brands, labels, watermark, graphics or UI. Fictional adult model, not a celebrity, not an endorsement of a named product. No plastic skin or dramatic heavy makeup.

### editorial-lips-v1

Use case: ads-marketing. Asset type: portrait 1024x1536 makeup beauty editorial for OneShop website. Photorealistic close crop of an adult woman around 27 with warm medium brown skin, natural glossy rose-berry lips and part of smiling cheeks, nose and one eye visible, dark curls at edge. She holds a sleek unbranded rose lipstick with a silver case near her lower lip, a natural poised hand with short neutral manicure. Beautiful candid confident beauty campaign, direct soft studio flash, skin texture and tiny freckles visible, a pale pink/cream background, rose color accents, highly tactile lips and skin. Close intimate crop but clearly adult fashion beauty photography. No text, logos, brand labels, watermark, collage or UI. Fictional adult woman not a celebrity and not a named product endorsement.

### editorial-ritual-v1

Use case: ads-marketing. Asset type: landscape 1536x1024 skincare lifestyle still-life for OneShop beauty ecommerce. Photorealistic editorial photograph of a beautiful pale pink bathroom shelf and glossy white sink edge in warm morning light. An unbranded frosted pink serum bottle with chrome dropper, translucent cleanser pump, a small open white moisturizer jar, a folded white cotton towel, small glass dish with water droplets and one pink flower branch at the side. Product shapes varied and carefully art directed, gentle ripple reflections on pink tile wall, rich yet fresh natural light, clean modern magazine style, visually lush but uncluttered. Objects mostly centered/right, useful uncluttered upper-left area for live HTML text if required. Real glass and ceramic texture, no beige stone podiums. No text, logos, labels, watermarks, graphic design or UI. Decorative scene not named catalog product photos.

