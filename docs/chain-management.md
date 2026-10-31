# Quản trị chuỗi cửa hàng

| Đường dẫn | Quyền | Chức năng |
|---|---|---|
| `/admin/inventory` | ADMIN | Kho từng cửa hàng, nhập hàng, giảm tồn có lý do, lịch sử phiếu |
| `/admin/stores` | ADMIN | Tạo, sửa, ngừng hoạt động cửa hàng |
| `/admin/users` | ADMIN | Tìm tài khoản, khóa/mở, gán quản lý và cửa hàng |
| `/admin/reports` | ADMIN | Doanh thu theo ngày/cửa hàng, sản phẩm bán chạy, xuất XLSX |
| `/branch/orders` | BRANCH_MANAGER | Xem và chuyển trạng thái đơn tại chi nhánh được giao |
| `/branch/inventory` | BRANCH_MANAGER | Nhập/điều chỉnh kho tại chi nhánh được giao |
| `/branch/reports` | BRANCH_MANAGER | Báo cáo chỉ thuộc chi nhánh được giao |
| `/account/profile` | Người đã đăng nhập | Sửa hồ sơ, đổi mật khẩu, sổ địa chỉ |

Quản lý chi nhánh không được phân công lại cửa hàng xử lý đơn, sửa danh mục sản phẩm hay quản lý tài khoản. Backend đọc lại cửa hàng được gán từ database trước mỗi thao tác. Thay đổi quyền/trạng thái sẽ tăng phiên bản xác thực, làm hết hiệu lực session và JWT cũ.

## Cài đặt và dữ liệu

Local profile `dev` dùng Hibernate update, tạo thêm `stock_movements` và `customer_addresses`. Khởi động lại ứng dụng để cập nhật. Không có bước xóa hay đặt lại dữ liệu cũ.

Production profile `prod` chạy migration bổ sung `src/main/resources/db/chain-management-mysql.sql` trước Hibernate validate. Cần tài khoản MySQL có quyền CREATE TABLE và INSERT role. Có thể chạy script trực tiếp trong MySQL trước deploy; script chạy lại được, không sửa tồn kho hay đơn cũ. Sao lưu database trước khi cập nhật ứng dụng.

Với database mới, import `docs/schema/mysql.sql` rồi chạy `src/main/resources/db/password-reset-mysql.sql` và `src/main/resources/db/chain-management-mysql.sql`. Dữ liệu mẫu được khởi tạo khi chạy profile dev; prod không khởi tạo tài khoản demo.

## Luồng demo

1. Admin tạo cửa hàng tại `/admin/stores`.
2. Admin tạo sản phẩm, nhập quy cách mỗi dòng một lựa chọn. Màu có cú pháp `Đỏ gạch | #AA3344 | https://example.com/red.jpg`; URL ảnh là tùy chọn.
3. Vào Kho hàng, chọn cửa hàng và sản phẩm. Nhập số dương để nhập hàng; số âm để giảm tồn. Mọi thay đổi cần lý do. Gửi lại cùng một phiếu không thay đổi tồn lần hai.
4. Khách lưu địa chỉ tại Hồ sơ và địa chỉ, thêm hàng vào giỏ, chọn địa chỉ đã lưu khi checkout.
5. Admin gán tài khoản thử nghiệm thành BRANCH_MANAGER và chọn cửa hàng tại `/admin/users`. Người dùng đăng nhập lại; vào `/branch` để xử lý đơn của cửa hàng mình.
6. Chuyển đơn qua các bước tới COMPLETED. Báo cáo tính theo thời điểm sự kiện hoàn tất, không phải ngày tạo đơn hoặc ngày gửi lại email. Doanh thu là tiền sản phẩm, không gồm phí vận chuyển; không phải lợi nhuận. Đơn hủy được đếm theo ngày hủy.
7. Xuất Excel để đối chiếu doanh thu cửa hàng và sản phẩm.

Tồn hiện tại vẫn tính theo sản phẩm/cửa hàng, dùng chung giữa các màu và dung tích. Lô hàng, hạn sử dụng và tồn theo biến thể chưa thuộc bản cập nhật này. Lịch sử phiếu kho ghi nhập/điều chỉnh thủ công; biến động giữ/hoàn kho do checkout nằm trong lịch sử đơn hàng.

## Kiểm thử

`ChainManagementTest` kiểm tra phiếu kho trùng, số âm, nhập sản phẩm mới rồi mua, quyền chéo cửa hàng, số liệu doanh thu, XLSX, cập nhật quyền, địa chỉ và mật khẩu. Các bài kiểm thử dùng database `oneshop_test_<uuid>` riêng.

```powershell
mvn verify '-Doneshop.build.directory=C:\OneShop\target\chain-build' '-DargLine=-Djdk.net.unixdomain.tmpdir=C:\OneShop\target'
```
