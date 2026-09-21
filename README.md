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

### 1. Khởi chạy nhanh với H2 In-Memory (Mặc định)

Chỉ cần chạy lệnh sau, hệ thống sẽ tự động khởi tạo cơ sở dữ liệu mẫu và nạp dữ liệu demo (Sản phẩm, Thương hiệu, Danh mục, Cửa hàng, Tài khoản):

```bash
mvn spring-boot:run
```
*(Hoặc chỉ định rõ profile)*:
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
- **Đăng nhập cấp JWT Token:** `POST /api/auth/login` (Body: `{ "email": "...", "password": "..." }`)

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
├── .env                             # Biến môi trường mẫu (JWT, Cloudinary, DB)
└── pom.xml                          # Quản lý thư viện Maven
```

---

## 💡 Ghi chú mở rộng phát triển

1. **Cloudinary:** Cập nhật thông tin `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` trong `.env` để kích hoạt tính năng upload ảnh thực tế.
2. **Logic Giỏ hàng & Đơn hàng:** Các Entity `Order`, `OrderItem`, `StoreStock` đã được chuẩn bị sẵn để mở rộng quy trình đặt hàng chọn chi nhánh nhận hàng / giao hàng.

