# OneShop Cosmetic Chain

Khung suon website ban my pham OneShop theo mo hinh chuoi cua hang.

## Cong nghe

- Spring Boot 3.5.x, Java 21
- Thymeleaf + Bootstrap 5
- Spring Data JPA
- Spring Security + JWT
- SiteMesh decorator
- Cloudinary upload anh
- Profile database: H2 dev, MySQL, PostgreSQL, SQL Server

## Chay nhanh

Yeu cau JDK 21 tro len. Khuyen nghi dung JDK 21 cho dung cau hinh project.

```bash
mvn spring-boot:run
```

Mac dinh project dung profile `dev` voi H2 de chay thu khong can cai database ngoai.

Chay voi database that:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
mvn spring-boot:run -Dspring-boot.run.profiles=sqlserver
```

## Cau truc chinh

```text
src/main/java/com/oneshop
  config        Cau hinh Security, Cloudinary
  entity        Entity JPA cho san pham, cua hang, ton kho, don hang, user
  repository    Repository JPA
  security      JWT filter, JWT service, user details
  service       Lop xu ly nghiep vu dang khung
  web           Controller Thymeleaf va API auth

src/main/resources
  templates/decorators  Layout SiteMesh
  templates/pages       Trang nguoi dung
  templates/admin       Trang quan tri
  static/css, static/js File giao dien ngoai
```

## Cac man hinh da tao san

- Trang chu khach hang: `/`
- Danh sach san pham: `/products`
- Chi tiet san pham: `/products/{slug}`
- Danh sach cua hang: `/stores`
- Dang nhap: `/login`
- Dang ky: `/register`
- Dashboard admin: `/admin`
- Quan ly san pham admin: `/admin/products`
- API login JWT: `POST /api/auth/login`

Tai khoan mau khi chay profile `dev`:

- Admin: `admin@oneshop.vn` / `123456`
- Khach hang: `khachhang@oneshop.vn` / `123456`

## Ghi chu phat trien

- Sua thong tin Cloudinary trong bien moi truong hoac `.env`.
- Doi profile database trong `src/main/resources/application.yml` hoac luc chay app.
- Cac service hien dang o muc khung, co the them logic CRUD, gio hang, thanh toan, phan quyen chi tiet sau.
