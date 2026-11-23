# PHÂN QUYỀN NHÂN VIÊN TRÊN TRANG WEB ADMIN

**Dự án:** Hệ sinh thái Thương mại điện tử Nhà sách Tri Thức Việt  
**Phạm vi:** Trang quản trị Web Admin (`frontend/admin-web`) & bảng tài khoản `tk_nguoi_dung`

---

## 1. Ai được phép đăng nhập vào Trang Web Admin?

Trong bảng `tk_nguoi_dung`, cột `vai_tro` gồm 4 giá trị:

| Vai trò (`vai_tro`) | Tên gọi | Được vào Web Admin? | Ghi chú |
| :--- | :--- | :---: | :--- |
| `ADMIN` | Quản trị viên (Chủ cửa hàng) | **Có** | Toàn quyền trên tất cả trang và chức năng |
| `WAREHOUSE_MANAGER` | Quản lý Kho | **Có** | Chỉ dùng các chức năng liên quan đến Sách, Danh mục, Nhập kho & Đóng gói đơn |
| `CSKH` | Chăm sóc Khách hàng | **Có** | Chỉ dùng các chức năng xử lý Đơn hàng, Đổi trả, Đánh giá, Thông báo & Khách hàng |
| `CUSTOMER` | Khách hàng | **Không** | Bị chặn đăng nhập Web Admin; chỉ sử dụng Mobile App |

---

## 2. Danh sách Tài khoản Nhân viên mẫu trong `tk_nguoi_dung`

| ID | Họ và tên | Email đăng nhập | Vai trò |
| :--- | :--- | :--- | :--- |
| `nd_admin` | Phạm Minh Quân | `admin@trithucviet.vn` | `ADMIN` |
| `nd_admin2` | Nguyễn Hải Đăng | `dang.admin@trithucviet.vn` | `ADMIN` |
| `nd_admin3` | Vũ Thị Minh Thư | `thu.admin@trithucviet.vn` | `ADMIN` |
| `nd_kho` | Trần Quốc Đạt | `kho@trithucviet.vn` | `WAREHOUSE_MANAGER` |
| `nd_kho2` | Lê Văn Hùng | `hung.kho@trithucviet.vn` | `WAREHOUSE_MANAGER` |
| `nd_kho3` | Đoàn Thị Thanh Tâm | `tam.kho@trithucviet.vn` | `WAREHOUSE_MANAGER` |
| `nd_cskh` | Lê Ngọc Hân | `cskh@trithucviet.vn` | `CSKH` |
| `nd_cskh2` | Nguyễn Phương Nhi | `nhi.cskh@trithucviet.vn` | `CSKH` |
| `nd_cskh3` | Trương Hoàng Long | `long.cskh@trithucviet.vn` | `CSKH` |

---

## 3. Ma trận Phân quyền Chi tiết trên Web Admin

### 3.1. Bảng tổng hợp theo từng trang (`frontend/admin-web/src/pages/`)

| Trang / Chức năng | `ADMIN` | `WAREHOUSE_MANAGER` (Quản lý Kho) | `CSKH` (Chăm sóc Khách hàng) |
| :--- | :---: | :---: | :---: |
| **Dashboard** (`Dashboard.tsx` - Doanh thu, Lợi nhuận, Top sách) | Xem toàn bộ (kể cả doanh thu & tài chính) | Chỉ xem chỉ số tồn kho, cảnh báo hết hàng (**không** xem doanh thu/lợi nhuận) | Chỉ xem thống kê đơn mới, yêu cầu đổi trả (**không** xem doanh thu/lợi nhuận) |
| **Sách** (`Books.tsx` - Thêm/Sửa/Ẩn sách, chỉnh giá) | Toàn quyền (Thêm, Sửa, Xóa/Ẩn, chỉnh giá bán & Flash Sale) | Thêm/Sửa thông tin sách, cập nhật tồn kho (**không** được tự ý chỉnh giá bán/Flash Sale hoặc xóa sách) | Chỉ xem để tra cứu tư vấn (**không** được Thêm/Sửa/Xóa) |
| **Danh mục, Tác giả, NXB** (`Categories.tsx`) | Toàn quyền | Thêm/Sửa danh mục, tác giả, NXB khi nhập sách mới | Chỉ xem |
| **Nhập kho & Tồn kho** (`Inventory.tsx` - Phiếu nhập `phieu_nhap_kho`) | Toàn quyền | **Toàn quyền** (Tạo phiếu nhập kho, nhập số lượng, giá nhập, kiểm kê tồn) | **Không được dùng** |
| **Đơn hàng & Đổi trả** (`Orders.tsx` - `don_hang`, `yeu_cau_doi_tra`) | Toàn quyền mọi chuyển trạng thái 1 chiều, hủy đơn ở `CHO_XAC_NHAN` / `DANG_DONG_GOI` & duyệt đổi trả | Chỉ chuyển trạng thái đóng gói & xuất kho: `CHO_XAC_NHAN` → `DANG_DONG_GOI` → `DANG_GIAO` (**không** xác nhận `DA_GIAO`, **không** hủy đơn, **không** duyệt đổi trả) | Xác nhận đơn (`CHO_XAC_NHAN` → `DANG_DONG_GOI`), hủy đơn ở `CHO_XAC_NHAN` / `DANG_DONG_GOI`, xác nhận `DA_GIAO` (`DANG_GIAO` → `DA_GIAO`) & duyệt/từ chối `yeu_cau_doi_tra` (**không** tự xuất kho `DANG_DONG_GOI` → `DANG_GIAO`) |
| **Mã giảm giá** (`Vouchers.tsx` - `ma_giam_gia`) | Toàn quyền (Tạo, Sửa, Bật/Tắt mã) | **Không được dùng** | Chỉ xem mã đang chạy để gửi cho khách (**không** được Tạo/Sửa/Tắt mã) |
| **Khách hàng & Nhân sự** (`CustomersStaff.tsx` - `tk_nguoi_dung`) | Toàn quyền (Quản lý khách + Tạo/Khóa/Phân quyền nhân viên) | **Không được dùng** | Xem thông tin khách hàng để hỗ trợ đơn (**không** được khóa tài khoản hay xem/sửa nhân sự) |
| **Báo cáo, Đánh giá & Cài đặt** (`ReportsSettings.tsx`) | Toàn quyền (Xem báo cáo tài chính, ẩn/hiện đánh giá, gửi thông báo, cấu hình hệ thống) | **Không được dùng** | Ẩn/Hiện bình luận vi phạm (`danh_gia`), gửi thông báo hỗ trợ (`thong_bao`); **không** xem báo cáo doanh thu & cài đặt hệ thống |

---

### 3.2. Tóm tắt nhanh: Mỗi vai trò **ĐƯỢC DÙNG** và **KHÔNG ĐƯỢC DÙNG** gì?

#### 1) Quản trị viên (`ADMIN`)
- **Được dùng:** Tất cả mọi trang và mọi thao tác trên Web Admin (doanh thu, giá bán, Flash Sale, kho, đơn hàng, voucher, duyệt đổi trả, khóa/mở tài khoản, tạo tài khoản nhân viên, cấu hình hệ thống).
- **Không được dùng:** Không bị giới hạn chức năng nào trên Web Admin.

#### 2) Quản lý Kho (`WAREHOUSE_MANAGER`)
- **Được dùng:**
  - Tạo phiếu nhập kho (`phieu_nhap_kho`, `chi_tiet_phieu_nhap`) để cộng `ton_kho`.
  - Theo dõi sách sắp hết hàng (`ton_kho <= 5`).
  - Thêm/Sửa thông tin đầu sách, danh mục, tác giả, nhà xuất bản (`sach`, `danh_muc`, `tac_gia`, `nha_xuat_ban`).
  - Cập nhật trạng thái đóng gói/xuất kho cho đơn hàng: `CHO_XAC_NHAN` (`PENDING`) → `DANG_DONG_GOI` (`PREPARING`) → `DANG_GIAO` (`SHIPPING`).
- **Không được dùng:**
  - Không xem báo cáo doanh thu, lợi nhuận.
  - Không tự ý chỉnh giá bán, Flash Sale, hay tạo/sửa `ma_giam_gia`.
  - Không xác nhận giao thành công (`DA_GIAO`), không hủy đơn (`DA_HUY`) và không duyệt yêu cầu đổi trả/hoàn tiền (`yeu_cau_doi_tra`).
  - Không quản lý tài khoản khách hàng hay nhân sự (`tk_nguoi_dung`).

#### 3) Chăm sóc Khách hàng (`CSKH`)
- **Được dùng:**
  - Tra cứu thông tin sách và mã giảm giá đang hoạt động để tư vấn khách.
  - Xem danh sách khách hàng (`CUSTOMER`) và lịch sử đơn hàng để hỗ trợ.
  - Xác nhận đơn (`CHO_XAC_NHAN` → `DANG_DONG_GOI`), hủy đơn ở `CHO_XAC_NHAN` / `DANG_DONG_GOI` theo yêu cầu khách và xác nhận hoàn tất giao hàng (`DANG_GIAO` → `DA_GIAO`).
  - Duyệt hoặc từ chối yêu cầu đổi trả (`yeu_cau_doi_tra`).
  - Kiểm duyệt/ẩn đánh giá vi phạm (`danh_gia`) và gửi thông báo cho khách (`thong_bao`).
- **Không được dùng:**
  - Không tạo phiếu nhập kho hoặc sửa số lượng tồn kho (`phieu_nhap_kho`).
  - Không tự chuyển đơn ở khâu xuất kho (`DANG_DONG_GOI` → `DANG_GIAO`).
  - Không thêm/sửa/xóa sách, danh mục, giá bán, hay tạo/sửa mã giảm giá.
  - Không xem báo cáo doanh thu/tài chính tổng hợp.
  - Không tạo/xóa/khóa tài khoản nhân viên hay thay đổi cấu hình hệ thống.
