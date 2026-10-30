# Ôn vấn đáp OneShop: logic code và luồng chạy

Tài liệu đối chiếu với mã nguồn ngày 30/10/2026. Mục tiêu là giải thích được request đi đâu, dữ liệu thay đổi thế nào và vì sao có các bước kiểm tra. Các đường dẫn dưới đây tính từ thư mục gốc repository.

## 1. Giới thiệu trong khoảng một phút

> OneShop là website bán mỹ phẩm theo mô hình chuỗi cửa hàng. Em dùng Spring Boot để xử lý request và nghiệp vụ, Thymeleaf để render HTML, Bootstrap cho giao diện, SiteMesh để dùng chung layout. Dữ liệu lưu MySQL thông qua JPA/Hibernate. Web dùng Spring Security với session, còn API xác thực dùng JWT. Admin được quản lý sản phẩm và xử lý đơn. Khách có thể chọn tùy chọn sản phẩm, dùng giỏ hàng, đặt COD, theo dõi đơn và khôi phục mật khẩu qua email. Backend kiểm tra giá, tồn kho và quyền truy cập; bản triển khai dùng Render, Aiven MySQL và Cloudinary cho ảnh upload.

Không nói mọi chức năng đã hoàn chỉnh như hệ thống thương mại thật. Hiện chưa có cổng thanh toán, tích hợp hãng vận chuyển hoặc tồn riêng từng màu.

## 2. Request chạy qua đâu?

```text
Trình duyệt gửi HTTP request
    → filter: security / bố cục / thành phần liên quan
    → DispatcherServlet của Spring MVC
    → Controller khớp URL và HTTP method
    → Service xử lý nghiệp vụ
    → Repository truy vấn thông qua JPA/Hibernate
    → MySQL
    → Service chuyển dữ liệu sang dạng dùng cho màn hình
    → Controller đưa dữ liệu vào Model và trả tên view
    → Thymeleaf render HTML; SiteMesh ghép layout
    → trả HTML về trình duyệt
```

Đây là sơ đồ khái quát; filter bao quanh request/response, không phải một controller gọi lần lượt tất cả filter. API `@RestController` trả JSON thay vì HTML.

| Lớp | Trách nhiệm | Ví dụ |
| --- | --- | --- |
| Controller | Nhận HTTP, bind/validate form, chọn view hoặc redirect | `OrderController` |
| Service | Quy tắc nghiệp vụ, transaction, kiểm tra điều kiện | `OrderService` |
| Repository | Đọc/ghi database và khai báo truy vấn/khóa | `OrderRepository`, `StoreStockRepository` |
| Entity | Ánh xạ đối tượng Java tới bảng và quan hệ | `Order`, `OrderItem`, `StoreStock` |
| DTO/Form | Giới hạn dữ liệu nhận/trả cho từng thao tác | `CheckoutForm`, `OrderView` |
| Template | Hiển thị dữ liệu và form | `templates/pages/checkout.html` |

**Vì sao chia lớp?** Để phần HTTP, nghiệp vụ và dữ liệu có trách nhiệm rõ ràng, dễ kiểm thử và bảo trì. Không nói Controller tuyệt đối không được gọi repository: code hiện có một số truy vấn đơn giản ngay trong controller; nghiệp vụ đặt hàng vẫn ở service.

## 3. Khi khởi động ứng dụng

1. Java chạy `OneShopApplication.main()` và `SpringApplication.run(...)`.
2. Spring Boot đọc cấu hình, profile và biến môi trường; tạo các bean và inject dependency qua constructor.
3. Kết nối MySQL, khởi tạo JPA, Spring Security, MVC và các thành phần tích hợp.
4. Với `dev`, Hibernate cập nhật schema; các runner bổ sung dữ liệu demo nếu được bật.
5. Với `prod`, Hibernate validate schema, JDBC session được bật; không chạy seed sản phẩm/admin demo.
6. Tomcat phục vụ request trên cổng cấu hình, mặc định 8080.

Đọc các file: `OneShopApplication.java`, `application.yml`, `application-dev.yml`, `application-prod.yml`, `config/DataInitializer.java`, `config/CatalogDataInitializer.java`, `config/DevStockInitializer.java`.

Các runner dev có thứ tự: tài khoản/cửa hàng → catalog → tồn mẫu, giúp tồn kho chỉ được tạo sau khi có sản phẩm và cửa hàng.

## 4. Luồng xem sản phẩm

**Ví dụ:** khách mở `/products?q=serum&sort=price-asc`.

1. `ProductController.products(...)` nhận `page`, `category`, `q`, `sort`.
2. Chỉ nhận các kiểu sort được hỗ trợ; giá trị khác quay về `newest`.
3. Tạo `PageRequest`, mỗi trang 12 sản phẩm; sắp xếp thêm ID để thứ tự ổn định khi giá/tên bằng nhau.
4. Gọi `ProductService.searchCatalog(...)` để lấy dữ liệu.
5. Đặt `productPage` và thông tin lọc vào `Model`.
6. Trả `pages/products`; Thymeleaf render và SiteMesh thêm header/footer.

Chi tiết dùng `/products/{slug}` → `findActiveProductBySlug(...)`. Slug phục vụ URL dễ đọc; ID là định danh quan hệ trong database, SKU phục vụ mã sản phẩm.

**Câu hỏi dễ gặp:** Tại sao phân trang? Vì không nên tải toàn bộ catalog trong mỗi request. Tại sao không tin sort từ client? Vì backend cần giới hạn trường/thứ tự hợp lệ.

## 5. Đăng ký tài khoản

```text
POST /register
  → kiểm tra CSRF
  → AuthPageController.register(...)
  → @Valid + BindingResult
  → CustomerRegistrationService.register(...)
  → UserRepository.saveAndFlush(...)
  → redirect /login
```

Các điểm phải giải thích:

- DTO `RegisterRequest` kiểm tra dữ liệu và xác nhận mật khẩu. Dữ liệu lỗi được trả lại form.
- Service chuẩn hóa email: trim và chuyển chữ thường.
- Kiểm tra email tồn tại để báo lỗi thân thiện; database có unique constraint để ngăn hai request đồng thời tạo cùng email.
- Mật khẩu được `PasswordEncoder.encode(...)` bằng BCrypt; không lưu mật khẩu gốc.
- Backend tự gán role `CUSTOMER`; không nhận role admin từ form đăng ký.
- `saveAndFlush` đẩy thay đổi xuống database ngay trong luồng này để phát hiện ràng buộc.
- Thành công dùng redirect để trình duyệt tải trang login bằng GET, giảm gửi lại POST khi refresh.

**Không kiểm tra “mật khẩu có trùng người khác không”.** Người dùng khác nhau có thể chọn cùng mật khẩu. Ràng buộc duy nhất là email; BCrypt có salt nên cùng mật khẩu không nhất thiết cho cùng chuỗi hash.

## 6. Đăng nhập web: session, không phải JWT

1. GET `/login` được `AuthPageController` trả form.
2. POST `/login` được filter form login của Spring Security xử lý, không phải một hàm POST login trong controller giao diện.
3. Authentication provider dùng `CustomUserDetailsService.loadUserByUsername(email)` để đọc tài khoản, hash, trạng thái active và role.
4. Password encoder kiểm tra mật khẩu nhập với hash BCrypt bằng phép đối chiếu, không giải mã hash.
5. Thành công: Spring Security thiết lập authentication và đổi session ID để chống session fixation.
6. `CartLoginSuccessHandler` gộp giỏ khách vào giỏ tài khoản, xóa dữ liệu giỏ khách sau khi gộp.
7. Chuyển về trang hợp lệ đã yêu cầu như checkout/đơn hàng; nếu không có, admin về `/admin`, khách về `/`.

**Session là gì?** Trạng thái phía server; cookie `JSESSIONID` giúp server nhận ra phiên. Cookie không chứa mật khẩu.

Ở `dev`, session nằm trong bộ nhớ. Ở `prod`, Spring Session JDBC lưu session trong MySQL. Tài khoản và giỏ tài khoản vẫn là dữ liệu riêng trong các bảng nghiệp vụ.

## 7. API JWT khác chỗ nào?

```text
POST /api/auth/login
  → AuthApiController
  → AuthenticationService.login(...)
  → AuthenticationManager kiểm tra thông tin
  → JwtService tạo access token và refresh token
  → trả JSON TokenResponse
```

Request API tiếp theo gửi `Authorization: Bearer <accessToken>`. Bộ lọc JWT kiểm tra token và xây authentication cho request. Không lấy cookie web làm thông tin đăng nhập API.

- Access token dùng gọi API; refresh token dùng `/api/auth/refresh` để xin token.
- Kiểm tra chữ ký, issuer, hạn dùng, loại token và phiên bản thông tin xác thực theo logic hiện tại.
- JWT ký số không có nghĩa nội dung được mã hóa; không đưa mật khẩu vào payload.
- API stateless về session HTTP không có nghĩa hoàn toàn không truy vấn database: dự án vẫn kiểm tra tài khoản/trạng thái liên quan.
- Logout web kết thúc session web; không tự thu hồi mọi JWT. Khôi phục mật khẩu tăng credential version để từ chối token cũ.

## 8. Giỏ hàng và chọn biến thể

Đọc `controller/CartController.java`, `service/CartService.java` và `dto/CartSelection.java`.

1. Khách POST ID sản phẩm, số lượng, màu và quy cách; form có CSRF token.
2. Service lấy sản phẩm thật, kiểm tra đang bán, tùy chọn hợp lệ, số lượng và tồn khả dụng.
3. Nếu chưa đăng nhập: lưu các lựa chọn trong session.
4. Nếu đăng nhập: thay đổi `shopping_carts` và `shopping_cart_items` trong transaction.
5. Các lựa chọn khác màu/quy cách tạo dòng giỏ riêng; khi kiểm tra tồn phải cộng chung lượng theo sản phẩm.
6. Giá, ảnh và thông tin hiển thị được dựng lại phía server, không tin đơn giá do client gửi.

**Thêm vào giỏ chưa trừ tồn.** Kiểm tra ở giỏ giúp người dùng biết khả năng mua; checkout phải kiểm tra và khóa lại vì người khác có thể mua trước.

Ví dụ: SKU A có 5 sản phẩm, khách chọn màu đỏ 3 và hồng 3. Vì tồn hiện dùng chung SKU, tổng 6 vượt 5; không thể coi hai màu là hai kho độc lập.

## 9. Đặt hàng: luồng quan trọng nhất

Mở trực tiếp `OrderController.place(...)` và `OrderService.place(...)` khi thuyết trình.

### Hiển thị checkout

GET `/checkout` yêu cầu đăng nhập, kiểm tra giỏ không rỗng, điền sẵn tên/điện thoại và tạo `checkoutToken` UUID. Khách nhập địa chỉ và thông tin nhận hàng; không chọn cửa hàng xử lý.

### Xác nhận đặt hàng

1. POST `/checkout` qua security và validation `CheckoutForm`.
2. Service mở transaction và khóa bản ghi tài khoản để tuần tự hóa các thay đổi giỏ/đặt hàng của cùng khách.
3. Tìm đơn có `checkoutToken` này. Nếu đã có và thuộc đúng khách, trả mã đơn cũ thay vì trừ hàng lần nữa.
4. Đọc lại giỏ server, từ chối giỏ rỗng hoặc dòng có cảnh báo.
5. Gom số lượng theo product ID bằng `TreeMap` để có thứ tự ổn định.
6. `reserveAvailableStore(...)` duyệt cửa hàng hoạt động theo ID, khóa bản ghi tồn theo product ID bằng `PESSIMISTIC_WRITE`.
7. Chọn một cửa hàng đủ **toàn bộ giỏ** và trừ tồn tại đó; thiếu thì báo lỗi nghiệp vụ.
8. Tạo mã đơn `OS-...`, thông tin nhận hàng, phí vận chuyển và tổng tiền từ giỏ backend.
9. Tạo `OrderItem` chứa bản chụp tên, giá, ảnh, màu/quy cách; thêm `OrderEvent` đầu tiên.
10. Lưu đơn, xóa giỏ tài khoản và phát sự kiện `OrderService.Placed` trong luồng transaction.
11. Khi transaction commit thành công, listener gửi email được kích hoạt.
12. Controller redirect sang `/orders/{code}`.

### Vì sao dùng transaction và khóa?

Ví dụ kho còn 1 sản phẩm, hai khách đặt cùng lúc. Nếu cả hai chỉ đọc tồn bằng 1 rồi cùng ghi 0 thì có thể bán 2 sản phẩm dù chỉ có 1. Khóa ghi khiến request còn lại phải đợi; sau đó nó kiểm tra tồn đã cập nhật và không đặt vượt kho.

`@Transactional` gom thay đổi kho, đơn và giỏ vào cùng giao dịch. Với exception runtime phù hợp, giao dịch rollback: không để trường hợp đã trừ kho nhưng lưu đơn thất bại. `saveAndFlush` chưa đồng nghĩa transaction đã commit.

Khóa theo cùng thứ tự cửa hàng/sản phẩm giúp giảm nguy cơ deadlock. Không khẳng định hệ thống không thể deadlock trong mọi tình huống.

### Tại sao giữ snapshot trong OrderItem?

Khách mua giá 200.000 đồng hôm nay; mai admin sửa sản phẩm thành 250.000 đồng thì hóa đơn cũ vẫn phải là 200.000 đồng. Vì vậy chi tiết đơn giữ bản chụp thông tin lúc mua, không chỉ đọc tên/giá hiện tại từ `products`.

## 10. Email và theo dõi đơn

`OrderMailListener` dùng `@TransactionalEventListener(AFTER_COMMIT)`. Điều này đảm bảo không gửi xác nhận thành công cho một đơn bị rollback. `OrderMailService.sendConfirmation(...)` dùng giao dịch mới để lưu kết quả gửi.

- Local có thể dùng Gmail SMTP; bản deploy hỗ trợ Resend HTTPS.
- Kết quả có `PENDING`, `NOT_CONFIGURED`, `FAILED`, `SENT`.
- Gửi lỗi không rollback đơn đã commit. Admin có thể gửi lại theo điều kiện nghiệp vụ.
- `SENT` nghĩa dịch vụ tiếp nhận yêu cầu, chưa bảo đảm thư nằm trong Inbox.
- **AFTER_COMMIT không tự có nghĩa chạy bất đồng bộ.** Không nói mọi email đều được đẩy vào queue bền vững: email đơn hiện không có outbox/queue tự retry bền vững.

Theo dõi: `/orders/{code}` truy vấn bằng cả **code và email từ Principal**. Biết mã đơn không đủ để xem đơn người khác. Timeline là các bản ghi `order_events` do hệ thống/admin tạo, chưa phải tracking trực tiếp từ hãng vận chuyển.

## 11. Admin đổi trạng thái, hủy và chuyển cửa hàng

```text
NEW → CONFIRMED → PACKING → SHIPPING → COMPLETED
NEW / CONFIRMED / PACKING → CANCELLED
```

- `OrderStatus.getNextStatuses()` xác định các bước hợp lệ.
- `OrderService.updateStatus(...)` khóa đơn, kiểm tra trạng thái và quyền admin trước khi chuyển.
- Khách chỉ tự hủy đơn của mình ở `NEW`; admin được hủy ở ba trạng thái trước giao.
- Khi hủy, `transition(...)` hoàn tồn về cửa hàng đang giữ hàng và ghi lịch sử. Trạng thái cùng khóa đơn ngăn hủy lặp để hoàn tồn nhiều lần.
- `assignStore(...)` chỉ cho đơn `NEW`: khóa tồn hai cửa hàng theo thứ tự, kiểm tra kho mới, hoàn kho cũ, trừ kho mới và ghi sự kiện trong cùng transaction.

**Vì sao phải kiểm tra server dù UI đã ẩn nút?** Người dùng vẫn có thể tự gửi HTTP request. UI chỉ hướng dẫn thao tác, backend mới thực thi quyền và quy tắc.

## 12. Chỉ admin được đăng sản phẩm

Đọc `config/SecurityConfig.java`, `controller/AdminController.java`, `service/AdminService.java` và `service/CloudinaryService.java`.

1. Security yêu cầu role `ADMIN` cho `/admin/**`.
2. `AdminService` còn có `@PreAuthorize` để bảo vệ tầng service.
3. Form được validate, kiểm tra SKU/slug, giá và dữ liệu danh mục/thương hiệu.
4. Nếu upload, kiểm tra nội dung file/giới hạn dung lượng rồi gọi Cloudinary.
5. Lưu thông tin và URL ảnh trong MySQL; ảnh upload nằm ở Cloudinary.

Thao tác ngừng kinh doanh là đổi trạng thái thay vì xóa sạch lịch sử liên quan. Không trình bày upload cloud và ghi MySQL là một transaction phân tán nguyên tử; chúng là hai hệ thống khác nhau.

## 13. Quên mật khẩu qua email đã đăng ký

Đọc `PasswordResetController`, `PasswordResetService`, `PasswordResetMailListener` và `CredentialVersionFilter`.

1. Khách gửi email ở `/forgot-password`; dữ liệu được validate và rate limit.
2. Service tìm tài khoản hoạt động; phản hồi công khai không tiết lộ rõ email có tồn tại không.
3. Sinh 32 byte ngẫu nhiên, tạo token URL-safe. Chỉ SHA-256 digest được lưu trong `password_reset_accounts`.
4. Token có hạn 15 phút, có thời gian chờ giữa các yêu cầu; sự kiện gửi mail chỉ xử lý sau commit.
5. Khách bấm link, server kiểm tra token và hạn dùng.
6. POST mật khẩu mới: khóa dữ liệu, kiểm tra lại token, băm BCrypt rồi cập nhật user.
7. Xóa hash token/hạn dùng và tăng `credential_version` trong cùng transaction.
8. Link cũ không dùng lại được; session/token đăng nhập cũ bị từ chối khi kiểm tra phiên bản xác thực. Không tự đăng nhập khách sau reset.

Không gửi lại mật khẩu cũ qua email, vì hệ thống không lưu mật khẩu gốc. Email chứa quyền thực hiện reset nên không được chia sẻ hoặc ghi log token.

## 14. Các câu hỏi nền tảng và câu trả lời ngắn

| Câu hỏi | Cách trả lời |
| --- | --- |
| JPA và Hibernate khác gì? | JPA là đặc tả ánh xạ/làm việc với persistence; Hibernate là implementation dự án sử dụng. |
| Spring Data JPA làm gì? | Tạo repository implementation và hỗ trợ CRUD/query dựa trên JPA. |
| `@Entity`/`@Table`? | Khai báo entity và bảng ánh xạ; quan hệ có FK thể hiện bằng mapping. |
| `@ManyToOne`? | Nhiều bản ghi cùng tham chiếu một bản ghi khác, ví dụ nhiều sản phẩm thuộc một thương hiệu. |
| Vì sao `BigDecimal` cho giá? | Giữ phép tính tiền thập phân chính xác hơn số dấu phẩy động như `double`. |
| DTO thay Entity để nhận form? | Chỉ cho client gửi các trường được phép, tách validation và tránh tự gán role/trạng thái ngoài ý muốn. |
| `@Valid` và `BindingResult`? | Chạy validation và nhận lỗi để trả lại form thay vì tiếp tục nghiệp vụ. |
| CSRF token khác JWT? | CSRF token bảo vệ request thay đổi dữ liệu trong ngữ cảnh cookie; JWT là thông tin xác thực API ở đây. |
| Authentication và authorization? | Xác định ai đang gọi, rồi xác định người đó được phép làm gì. |
| `@Version` khác khóa ghi? | Version phát hiện cập nhật xung đột theo optimistic locking; pessimistic lock giữ khóa DB trong giao dịch để tuần tự hóa thao tác. |
| `readOnly=true` có phải bảo mật chống ghi? | Không. Đây là khai báo tối ưu/ngữ nghĩa giao dịch, không thay phân quyền và DB permission. |
| Vì sao `open-in-view=false`? | Service chuẩn bị dữ liệu trước render, tránh template tự phát sinh truy vấn lazy không kiểm soát. |
| Session local có mất khi restart? | Session dev trong RAM có thể mất; dữ liệu nghiệp vụ MySQL vẫn còn. Prod lưu session JDBC và vẫn có hạn dùng. |
| Có tự đồng bộ local với Aiven không? | Không. `MYSQL_URL` quyết định server/database nhận dữ liệu; migrate là bước chủ động riêng. |
| Thymeleaf khác SiteMesh? | Thymeleaf render nội dung có dữ liệu; SiteMesh dùng decorator để ghép bố cục chung. |
| Có thể dùng JWT làm toàn bộ web không? | Có thể thiết kế như vậy, nhưng code hiện tại web dùng session, API dùng JWT. |
| Tại sao code frontend validate mà backend vẫn validate? | Client có thể bị bỏ qua hoặc sửa request, nên server phải tự đảm bảo tính hợp lệ. |
| 112 test đạt có nghĩa không còn bug? | Không. Nó cho biết các kịch bản đã viết đạt ở môi trường test; tích hợp cloud và hành vi chưa có test vẫn cần xác minh. |

## 15. Kịch bản demo khoảng 8–10 phút

1. Giới thiệu kiến trúc và các bảng chính.
2. Tìm/lọc một sản phẩm; mở chi tiết và chọn màu/quy cách.
3. Thêm giỏ trước đăng nhập; đăng nhập tài khoản demo để cho thấy gộp giỏ.
4. Đặt COD, giải thích giá lấy backend, cửa hàng tự phân bổ và transaction.
5. Mở lịch sử/timeline của khách.
6. Đăng nhập admin ở trình duyệt/phiên riêng, xử lý trạng thái; cho thấy khách cập nhật được tiến trình.
7. Nếu demo hủy/hoàn tồn, tạo đơn riêng còn NEW và đối chiếu tồn trước/sau.
8. Giải thích reset password; chỉ thử gửi đến email do mình kiểm soát và được provider cho phép.
9. Nếu hỏi JWT, gọi ba API auth bằng công cụ HTTP; nói rõ đây là luồng khác form login.
10. Nêu giới hạn và hướng phát triển, kết thúc bằng kết quả test đã chạy.

Không thay đổi mật khẩu/tồn hoặc tạo đơn trên dữ liệu người khác để demo. Nếu dùng live, chuẩn bị tài khoản và sản phẩm demo của mình; nếu dùng local, kiểm tra MySQL và dịch vụ trước buổi vấn đáp.

## 16. Thứ tự mở code để ôn tối nay

Ưu tiên theo thứ tự, không cần học thuộc toàn bộ dự án:

1. `config/SecurityConfig.java`: ba filter chain; public assets, API JWT, web session.
2. `controller/AuthPageController.java` → `service/CustomerRegistrationService.java`: đăng ký.
3. `security/CustomUserDetailsService.java` → `security/CartLoginSuccessHandler.java`: đăng nhập và gộp giỏ.
4. `controller/ProductController.java` → `service/ProductService.java`: đọc và phân trang dữ liệu.
5. `controller/CartController.java` → `service/CartService.java`: kiểm tra lựa chọn, giỏ session/database.
6. `controller/OrderController.java` → `service/OrderService.java` → `repository/StoreStockRepository.java`: đọc kỹ nhất.
7. `entity/Order.java`, `OrderItem.java`, `OrderEvent.java`, `StoreStock.java`, `OrderStatus.java`: quan hệ và trạng thái.
8. `service/OrderMailListener.java` → `service/OrderMailService.java`: email sau commit.
9. `service/PasswordResetService.java`: token, khóa, một lần sử dụng và credential version.
10. `api/controller/AuthApiController.java` → `service/AuthenticationService.java` → `security/JwtService.java`: JWT.

Khi được hỏi một chức năng, trả lời theo khung: **URL → controller/filter → service → bảng dữ liệu → điều kiện lỗi → kết quả**. Sau đó chỉ đúng hàm trong code và giải thích một ví dụ cụ thể.
