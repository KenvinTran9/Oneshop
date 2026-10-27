# OneShop - Nền tảng Mỹ phẩm & Chuỗi Cửa hàng 🌸

> **OneShop** là khung sườn website thương mại điện tử chuyên ngành mỹ phẩm và làm đẹp theo mô hình chuỗi cửa hàng (Multi-store Inventory). Giao diện người dùng được thiết kế hiện đại, tinh tế theo phong cách **Glossier Skincare Collection** (Minimalist, Clean, Soft Pastel & Serif Typography).

---

## 🚀 Công nghệ sử dụng

- **Backend:** Spring Boot 3.5.x, Java 21+
- **Security:** Spring Security 6 (Form Login + JWT Authentication Filter)
- **Database & ORM:** Spring Data JPA, Hibernate ORM
- **Supported Databases:**
  - H2 Database (In-Memory cho môi trường Dev)
  - MySQL 8.x
  - PostgreSQL
  - Microsoft SQL Server
- **Frontend & UI:**
  - Thymeleaf Template Engine
  - Bootstrap 5.3 & Bootstrap Icons
  - SiteMesh 3 Decorator (Quản lý layout nhất quán)
  - Google Fonts: *Playfair Display* (Serif Heading) & *Inter* (Body)
- **Cloud Storage:** Cloudinary (Quản lý và lưu trữ hình ảnh sản phẩm)

---

## 📦 Hướng dẫn cài đặt & Chạy dự án

### Yêu cầu hệ thống
- **JDK:** Java Development Kit 21 trở lên
- **Maven:** 3.8+ (hoặc dùng Maven Wrapper)

### 1. Khởi chạy nhanh với H2 In-Memory (profile `dev`)

Profile không còn được chọn mặc định để tránh vô tình chạy cấu hình phát triển ở production. Chạy profile `dev` để hệ thống tự động khởi tạo H2 và nạp dữ liệu demo:

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Ứng dụng sẽ chạy tại: **`http://localhost:8080`**  
H2 Console: **`http://localhost:8080/h2-console`** *(JDBC URL: `jdbc:h2:mem:oneshop`, User: `sa`, Password: để trống)*

---

### 2. Chạy với Cơ sở dữ liệu khác

Tùy chỉnh thông tin kết nối trong file `.env` hoặc file cấu hình tương ứng trong `src/main/resources/`:

- **MySQL:**
  ```bash
  mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
  ```
- **PostgreSQL:**
  ```bash
  mvn spring-boot:run "-Dspring-boot.run.profiles=postgres"
  ```
- **SQL Server:**
  ```bash
  mvn spring-boot:run "-Dspring-boot.run.profiles=sqlserver"
  ```

---

## 🔑 Tài khoản mẫu (Demo Accounts)

Khi chạy với profile `dev`, hệ thống tự động khởi tạo 2 tài khoản mẫu:

| Loại tài khoản | Email | Mật khẩu | Quyền hạn |
| :--- | :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin@oneshop.vn` | `123456` | Toàn quyền quản trị (`/admin`) |
| **Khách hàng (Customer)** | `khachhang@oneshop.vn` | `123456` | Mua sắm, xem thông tin cá nhân |

---

## 🔐 Cấu hình JWT

Production bắt buộc cung cấp đủ bốn biến môi trường sau; ứng dụng sẽ không khởi động nếu thiếu hoặc không hợp lệ:

| Biến | Ý nghĩa | Giá trị gợi ý |
| :--- | :--- | :--- |
| `JWT_SECRET` | Khóa ngẫu nhiên mã hóa Base64, tối thiểu 32 byte sau khi giải mã | Không có mặc định production |
| `JWT_ISSUER` | Định danh hệ thống phát token | `oneshop` |
| `JWT_ACCESS_TOKEN_EXPIRATION_MILLIS` | Thời hạn access token | `900000` (15 phút) |
| `JWT_REFRESH_TOKEN_EXPIRATION_MILLIS` | Thời hạn refresh token | `604800000` (7 ngày) |

Tạo secret an toàn bằng OpenSSL:

```bash
openssl rand -base64 32
```

Hoặc PowerShell:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Không commit secret thật. File [.env.example](.env.example) chỉ liệt kê tên biến và giá trị mẫu; Spring Boot không tự động nạp `.env`, vì vậy hãy cấu hình biến trong shell, IDE, container hoặc secret manager của môi trường triển khai.

Profile `dev` có một khóa công khai chỉ dành cho local để khởi chạy nhanh. Không sử dụng profile hoặc khóa này trong môi trường dùng chung/production.

---

## 🎨 Giao diện & Các màn hình chính

### 1. Phân hệ Khách hàng (Storefront) - Glossier Style
- **Trang chủ (`/`):** Hero banner lớn, danh mục nổi bật, sản phẩm mới với rating & giá ưu đãi, thông tin hệ thống cửa hàng.
- **Danh mục Sản phẩm (`/products`):**
  - Hero banner với tone màu gradient pastel đặc trưng của Glossier.
  - Thanh bộ lọc nhanh danh mục (Tất cả, Skincare, Makeup, Serum, Kem dưỡng, Sữa rửa mặt).
  - Tùy chọn sắp xếp (Mới nhất, Phổ biến, Giá tăng/giảm).
  - Lưới sản phẩm 4 cột thanh lịch, hỗ trợ phân trang chuẩn SEO.
- **Chi tiết Sản phẩm (`/products/{slug}`):** Điều hướng Breadcrumb, hình ảnh độ phân giải cao, đánh giá sao, nút thêm vào giỏ hàng và kiểm tra tồn kho tại chi nhánh.
- **Hệ thống Cửa hàng (`/stores`):** Danh sách các chi nhánh OneShop kèm địa chỉ, khu vực và hotline liên hệ.
- **Đăng nhập / Đăng ký (`/login`, `/register`):** Khung xác thực tối giản, giao diện bo góc mềm mại, hiệu ứng focus rose glow.

### 2. Phân hệ Quản trị (Admin Dashboard)
- **Tổng quan Quản trị (`/admin`):** Thống kê số liệu sản phẩm, đơn hàng, khách hàng.
- **Quản lý Sản phẩm (`/admin/products`):** Xem danh sách, thêm/sửa/xóa sản phẩm và tích hợp tải ảnh lên Cloudinary.

### 3. REST API
- **Đăng nhập cấp access và refresh token:** `POST /api/auth/login` (Body: `{ "email": "...", "password": "..." }`)
- **Đổi refresh token:** `POST /api/auth/refresh` (Body: `{ "refreshToken": "..." }`)

Access token được gửi bằng header `Authorization: Bearer <access-token>`. Refresh token chỉ được chấp nhận tại endpoint refresh và không thể dùng để truy cập tài nguyên được bảo vệ.

---

## 📂 Cấu trúc thư mục

```text
OneShop/
├── src/main/java/com/oneshop/
│   ├── config/          # Cấu hình Security, Cloudinary, DataInitializer
│   ├── entity/          # JPA Entities (Product, Store, StoreStock, User, Order...)
│   ├── repository/      # Spring Data JPA Repositories (@EntityGraph tối ưu lazy load)
│   ├── security/        # JWT Service, JwtAuthenticationFilter, CustomUserDetailsService
│   ├── service/         # Business Logic Services
│   └── web/             # Controllers (Web MVC Controllers & REST API)
│       └── dto/         # Request / Response DTOs
├── src/main/resources/
│   ├── application.yml              # Cấu hình chung của ứng dụng
│   ├── application-dev.yml          # Cấu hình H2 Database
│   ├── application-mysql.yml        # Cấu hình MySQL
│   ├── application-postgres.yml     # Cấu hình PostgreSQL
│   ├── application-sqlserver.yml    # Cấu hình SQL Server
│   ├── static/                      # CSS, JS, Images, SiteMesh Decorators
│   │   ├── css/app.css              # Design System phong cách Glossier
│   │   └── decorators/main.html     # Layout chính của Storefront
│   └── templates/                   # Giao diện Thymeleaf
│       ├── pages/                   # Giao diện người dùng (home, products, login...)
│       └── admin/                   # Giao diện trang quản trị
├── .env.example                     # Tên biến môi trường và giá trị mẫu giả
└── pom.xml                          # Quản lý thư viện Maven
```

---

## 💡 Ghi chú mở rộng phát triển

### Danh mục 100 sản phẩm và giỏ hàng

Profile `dev` nạp 100 sản phẩm từ `src/main/resources/catalog/products.json`: 5 danh mục, 20 sản phẩm mỗi danh mục và 59 thương hiệu. Dữ liệu được trích từ workbook `Tong_hop_100_san_pham_my_pham.xlsx`, sheet `Danh sách 100`, dòng 5–104; các sheet danh mục được đối chiếu để không nhập lặp.

Mỗi sản phẩm có ảnh cục bộ, một giá bán cố định bằng VNĐ, quy cách, thành phần, công dụng, loại da phù hợp và cách dùng. 19 sản phẩm trang điểm có mã màu để chọn; màu trên màn hình chỉ dùng minh họa. Quốc gia thương hiệu không được diễn giải thành nơi sản xuất. Không tạo đánh giá hoặc tồn kho giả.

Trang bán hàng không hiển thị thông tin đối chiếu bên ngoài. Các trường `priceSourceUrl`, `imageSourceUrl` và ngày kiểm tra chỉ được giữ nội bộ trong dữ liệu. Các giá ngoại tệ đã được khởi tạo thành một mức VNĐ, làm tròn 1.000 đồng, theo tỷ giá bán Vietcombank được đối chiếu ngày 26/10/2026 (USD 26.020; GBP 35.132,35); các trường gốc được giữ để kiểm tra nội bộ. Có thể chỉnh giá bán trong JSON trước khi nạp hoặc qua dữ liệu quản trị. Quy cách được chuẩn hóa theo phiên bản niêm yết, gồm COSRX Vitamin C 23 (20 g), ILLIYOON (230 ml) và Eucerin UreaRepair 5% (kem dưỡng tay 75 ml).

Giao diện có tìm sản phẩm/thương hiệu, lọc danh mục, sắp xếp tên/giá, chọn màu và số lượng. `/cart` cho phép thêm hàng, gộp đúng SKU/quy cách/màu, đổi số lượng 1–99, xóa/khôi phục và tính tổng. Giỏ lưu trong `localStorage` trên trình duyệt hiện tại, không đồng bộ tài khoản. Giá và lựa chọn trong giỏ là dữ liệu giao diện, cần được máy chủ kiểm tra lại khi triển khai đặt hàng thật. Hộp thông tin giao hàng chỉ là UI; nút xác nhận bị vô hiệu hóa, không gọi API đặt hàng, không lưu thông tin nhận hàng và không thanh toán.

Ảnh sản phẩm nằm trong `src/main/resources/static/images/catalog/`. CSS/JS mới tại `static/css/shop.css` và `static/js/shop.js`. Kiểm thử giỏ độc lập: `node tools/test_shop_cart.cjs`; kiểm thử danh mục và giao diện: `mvn test`.

Hiệu ứng dùng chung nằm ở `static/css/motion.css` và `static/js/motion.js`: thẻ xuất hiện một lần khi vào vùng nhìn, phản hồi nút/giỏ hàng và mở/đóng hộp thoại. Hiệu ứng chuyển động dùng transform/opacity, không thêm thư viện. Nội dung luôn hiện được khi JavaScript hoặc IntersectionObserver không có; hiệu ứng dừng khi tab ở nền hoặc người dùng bật giảm chuyển động. Giỏ cập nhật từng dòng theo mã sản phẩm/quy cách/màu để giữ vị trí nhập và focus. Kiểm tra các tình huống ngắt hiệu ứng, thao tác nhanh và giảm chuyển động: `node tools/test_shop_motion.cjs`.

### Nâng cấp UI theo từng bước

Bước 1: header dùng chung có tìm kiếm toàn danh mục bằng `GET /products?q=...`, giỏ hàng và tài khoản nằm ngoài menu thu gọn. Menu dẫn tới 5 danh mục và cửa hàng, đánh dấu trang hiện tại và hỗ trợ đóng bằng Esc. Trang danh mục dùng ô tìm kiếm trên header để tránh lặp. CSS riêng ở `static/css/header.css`; chiều cao header được theo dõi để nội dung cố định bên dưới không bị che. Đã kiểm tra luồng tìm kiếm từ trang cửa hàng, giữ từ khóa trên kết quả, menu/Esc, mở giỏ trực tiếp và bố cục ở 320, 390, 992 px cùng desktop.

Các bước tiếp theo dự kiến: trang chủ và gợi ý sản phẩm đa danh mục → bộ lọc danh mục → trang chi tiết → thông tin hỗ trợ và luồng checkout UI. Mỗi bước được hoàn tất và commit riêng để kiểm tra trước khi tiếp tục.

Khởi động lại profile `dev` sẽ nạp lại dữ liệu khi dùng H2 in-memory. Bộ nạp nhận diện sản phẩm bằng SKU nội bộ `CATALOG-001` đến `CATALOG-100`, không nhập trùng và không ghi đè các sản phẩm đã có hoặc các chỉnh sửa quản trị. Các profile cơ sở dữ liệu khác không tự nhập danh mục này.

Để cập nhật nội dung từ một workbook cùng cấu trúc (Python cần `openpyxl`):

```powershell
python tools/import_product_catalog.py "duong-dan-file.xlsx"
```

Script giữ lại giá và ảnh đã nghiên cứu khi SKU và slug còn trùng khớp. Sau khi sửa dữ liệu JSON, khởi động lại bản dev mới để xem thay đổi. Trên máy Windows này, nếu Java báo lỗi socket nội bộ, có thể dùng:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
mvn spring-boot:run "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=C:\OneShop\target"
```

1. **Cloudinary:** Cập nhật thông tin `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` trong `.env` để kích hoạt tính năng upload ảnh thực tế.
2. **Logic Giỏ hàng & Đơn hàng:** Các Entity `Order`, `OrderItem`, `StoreStock` đã được chuẩn bị sẵn để mở rộng quy trình đặt hàng chọn chi nhánh nhận hàng / giao hàng.

