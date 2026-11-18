# BẢNG ÁNH XẠ CHI TIẾT TỪNG CHỨC NĂNG GIAO DIỆN VÀO CÁC FILE BACKEND

**Dự án:** Hệ sinh thái Thương mại Điện tử Nhà sách Tri Thức Việt (`BookStore_Antigravity`)  
**Cấu trúc Backend tham chiếu:** [cau_truc_backend_gradle.md](cau_truc_backend_gradle.md) (`backend/src/main/java/com/trithucviet/bookstore/`)

---

## PHẦN I. CÁC CHỨC NĂNG TRÊN MOBILE APP KHÁCH HÀNG (`frontend/client-mobile/`)

### 1. Nhóm chức năng Tài khoản & Bảo mật (`app/(tabs)/profile.tsx`)

| STT | Tính năng trên Mobile App | Controller (`controller/client/`) | Service (`service/`) | Repository (`repository/`) | DTO (`dto/request/` & `dto/response/`) | Entity (`entity/`) & Config |
| :-: | :--- | :--- | :--- | :--- | :--- | :--- |
| 1.1 | **Đăng ký tài khoản khách hàng mới** | `ClientAuthController.java`<br>(`POST /api/v1/auth/register`) | `AuthService.java`<br>(Kiểm tra trùng email, mã hóa BCrypt, gán `VaiTro.CUSTOMER`) | `TkNguoiDungRepository.java`<br>(`existsByEmail`, `save`) | `RegisterRequest.java`<br>`AuthResponse.java` | `TkNguoiDung.java`<br>`SecurityConfig.java` |
| 1.2 | **Đăng nhập trên Mobile App** *(Chặn nhân sự/Admin đăng nhập ở nguồn Mobile nếu cần hoặc kiểm tra khóa TK)* | `ClientAuthController.java`<br>(`POST /api/v1/auth/login`) | `AuthService.java`<br>(Kiểm tra `biKhoa == true` → báo lỗi; kiểm tra mật khẩu; sinh JWT) | `TkNguoiDungRepository.java`<br>(`findByEmail`) | `LoginRequest.java`<br>`AuthResponse.java` | `TkNguoiDung.java`<br>`JwtTokenProvider.java` |
| 1.3 | **Xem & Cập nhật hồ sơ cá nhân** (Họ tên, SĐT, Đổi mật khẩu) | `ClientAuthController.java`<br>(`GET /api/v1/auth/me`, `PUT /api/v1/auth/profile`) | `AuthService.java` / `UserAddressService.java` | `TkNguoiDungRepository.java` | `UpdateProfileRequest.java`<br>`UserDto.java` | `TkNguoiDung.java`<br>`JwtAuthenticationFilter.java` |

---

### 2. Nhóm chức năng Sổ Địa Chỉ Nhận Hàng (`app/(tabs)/profile.tsx` & `app/checkout.tsx`)

| STT | Tính năng trên Mobile App | Controller (`controller/client/`) | Service (`service/`) | Repository (`repository/`) | DTO (`dto/request/` & `dto/response/`) | Entity (`entity/`) |
| :-: | :--- | :--- | :--- | :--- | :--- | :--- |
| 2.1 | **Xem danh sách địa chỉ nhận hàng của khách** | `ClientAddressController.java`<br>(`GET /api/v1/addresses`) | `UserAddressService.java` | `SoDiaChiRepository.java`<br>(`findByNguoiDungIdOrderByMacDinhDesc`) | `AddressDto.java` | `SoDiaChi.java` |
| 2.2 | **Thêm / Sửa / Xóa địa chỉ & Đặt làm mặc định** *(Khi đặt 1 địa chỉ `macDinh = true`, tự bỏ `macDinh` của các địa chỉ cũ)* | `ClientAddressController.java`<br>(`POST`, `PUT`, `DELETE /api/v1/addresses/{id}`, `PATCH /api/v1/addresses/{id}/default`) | `UserAddressService.java`<br>(`@Transactional` đảm bảo chỉ có 1 địa chỉ mặc định) | `SoDiaChiRepository.java` | `AddressUpsertRequest.java`<br>`AddressDto.java` | `SoDiaChi.java` |

---

### 3. Nhóm chức năng Trang chủ, Danh mục, Tìm kiếm & Chi tiết Sách (`index.tsx`, `catalog.tsx`, `book/[id].tsx`)

| STT | Tính năng trên Mobile App | Controller (`controller/client/`) | Service (`service/`) | Repository (`repository/`) | DTO (`dto/request/` & `dto/response/`) | Entity (`entity/`) |
| :-: | :--- | :--- | :--- | :--- | :--- | :--- |
| 3.1 | **Lấy cây Danh mục sách** (Danh mục cha + danh mục con đang hoạt động) | `ClientBookController.java`<br>(`GET /api/v1/categories`) | `BookService.java` | `DanhMucRepository.java`<br>(`findByDangHoatDongTrueOrderByThuTuHienThiAsc`) | `CategoryDto.java` | `DanhMuc.java` |
| 3.2 | **Kệ sách Flash Sale & Sách Bán Chạy trên Trang chủ** | `ClientBookController.java`<br>(`GET /api/v1/books/home`) | `BookService.java` | `SachRepository.java`<br>(`findByDangFlashSaleTrueAndHienThiTrue`, `findByBanChayTrueAndHienThiTrue`) | `HomeBooksResponse.java`<br>`BookDto.java` | `Sach.java`, `TacGia.java`, `DanhMuc.java` |
| 3.3 | **Tìm kiếm & Lọc sách đa tiêu chí** (Theo từ khóa Tên/Tác giả/ISBN, Danh mục, Khoảng giá `giaBan`, Đánh giá sao, Sắp xếp bán chạy/giá tăng/giảm/mới nhất) | `ClientBookController.java`<br>(`GET /api/v1/books`) | `BookService.java` | `SachRepository.java`<br>(Custom `@Query` / `Specification` lọc theo `danhMuc`, `giaBan`, `diemDanhGiaTb`, `hienThi = true`) | `BookFilterRequest.java`<br>`BookDto.java` | `Sach.java`, `TacGia.java`, `NhaXuatBan.java`, `DanhMuc.java` |
| 3.4 | **Xem Chi tiết 1 cuốn sách + Thông số xuất bản + Sách cùng chủ đề + Đánh giá của độc giả** | `ClientBookController.java`<br>(`GET /api/v1/books/{id}`, `GET /api/v1/books/{id}/reviews`) | `BookService.java`<br>`ReviewReturnService.java` | `SachRepository.java`<br>`DanhGiaRepository.java`<br>(`findBySachIdAndBiAnFalseOrderByNgayTaoDesc`) | `BookDetailDto.java`<br>`ReviewDto.java` | `Sach.java`, `DanhGia.java` |
| 3.5 | **Theo dõi thói quen người dùng (`User Tracking`) & Kệ "Đề xuất dành riêng cho bạn" trên Trang chủ**<br>• Đo thời gian dừng lại ở trang Chi tiết sách (`thoiGianXemGiay >= 3s`), ghi nhận `XEM_CHI_TIET`, `TIM_KIEM`, `THEM_YEU_THICH`, `THEM_GIO_HANG`, `MUA_HANG`, `BO_QUA_DE_XUAT`.<br>• **Đa chủ đề (Multi-Interest 3:2:1)**: Pha trộn Top 3 chủ đề cao điểm nhất.<br>• **Time Decay**: Suy giảm điểm theo thời gian (`XEM_CHI_TIET` bán rã 48h/hết hạn 7 ngày; `THEM_YEU_THICH`/`THEM_GIO_HANG` 14 ngày; `MUA_HANG` 30 ngày).<br>• **Candidate Exclusion & Impression Fatigue**: Loại trừ sách đã nằm trong Yêu thích/Giỏ hàng/Đơn hàng (chỉ gợi ý sách khác cùng chủ đề) và giảm 20% điểm nếu chủ đề hiện > 3 lần mà khách không bấm vào đọc. | `ClientTrackingController.java`<br>(`POST /api/v1/tracking/event`, `GET /api/v1/books/recommendations`) | `RecommendationTrackingService.java` | `HanhViNguoiDungRepository.java`<br>`SachRepository.java` | `TrackBehaviorRequest.java`<br>`RecommendationResponse.java` | `HanhViNguoiDung.java` (`LoaiHanhVi`), `Sach.java`, `DanhMuc.java` |

---

### 4. Nhóm chức năng Tủ Sách Yêu Thích (`book/[id].tsx` & `profile.tsx`)

| STT | Tính năng trên Mobile App | Controller (`controller/client/`) | Service (`service/`) | Repository (`repository/`) | DTO (`dto/request/` & `dto/response/`) | Entity (`entity/`) |
| :-: | :--- | :--- | :--- | :--- | :--- | :--- |
| 4.1 | **Xem danh sách sách yêu thích & Bấm thả tim / bỏ tim (`Toggle Wishlist`)** | `ClientWishlistController.java`<br>(`GET /api/v1/wishlists`, `POST /api/v1/wishlists/{bookId}/toggle`) | `CartWishlistService.java` | `SachYeuThichRepository.java`<br>(`findById_NguoiDungId`, `existsById`, `deleteById`) | `BookDto.java` | `SachYeuThich.java` (`SachYeuThichId`) |

---

### 5. Nhóm chức năng Giỏ Hàng & Mã Giảm Giá Voucher (`app/cart.tsx` & `app/checkout.tsx`)

| STT | Tính năng trên Mobile App | Controller (`controller/client/`) | Service (`service/`) | Repository (`repository/`) | DTO (`dto/request/` & `dto/response/`) | Entity (`entity/`) |
| :-: | :--- | :--- | :--- | :--- | :--- | :--- |
| 5.1 | **Xem giỏ hàng, Thêm sách vào giỏ, Tăng/Giảm số lượng, Tick chọn sản phẩm, Xóa khỏi giỏ** *(Chặn không cho `soLuong > sach.tonKho`)* | `ClientCartController.java`<br>(`GET /api/v1/cart`, `POST /api/v1/cart`, `PUT /api/v1/cart/{bookId}`, `DELETE /api/v1/cart/{bookId}`) | `CartWishlistService.java`<br>(Kiểm tra `sach.getTonKho()`, ném `BusinessException` nếu vượt tồn kho) | `GioHangRepository.java`<br>`SachRepository.java` | `CartItemRequest.java`<br>`CartResponseDto.java` | `GioHang.java` (`GioHangId`), `Sach.java` |
| 5.2 | **Xem danh sách Voucher đang mở & Áp mã giảm giá vào giỏ hàng** *(Kiểm tra ngày hiệu lực, `daSuDung < gioiHanLuot`, `tamTinh >= donToiThieu`, tính `CO_DINH` hoặc `PHAN_TRAM` có `giamToiDa`)* | `ClientVoucherController.java`<br>(`GET /api/v1/vouchers/active`, `POST /api/v1/vouchers/validate`) | `VoucherService.java` | `MaGiamGiaRepository.java`<br>(`findByMaCodeIgnoreCase`) | `ApplyVoucherRequest.java`<br>`VoucherValidationResponse.java` | `MaGiamGia.java` (`LoaiGiam.CO_DINH`, `LoaiGiam.PHAN_TRAM`) |

---

### 6. Nhóm chức năng Đặt Hàng, Theo Dõi Đơn, Hủy Đơn, Đánh Giá & Đổi Trả (`checkout.tsx` & `orders.tsx`)

| STT | Tính năng trên Mobile App | Controller (`controller/client/`) | Service (`service/`) | Repository (`repository/`) | DTO (`dto/request/` & `dto/response/`) | Entity (`entity/`) |
| :-: | :--- | :--- | :--- | :--- | :--- | :--- |
| 6.1 | **Đặt hàng (`Checkout`)** *(Chạy trong 1 `@Transactional`: kiểm tra tồn kho → trừ `sach.tonKho`, cộng `sach.daBan` → cộng `maGiamGia.daSuDung` → tạo `DonHang` + `ChiTietDonHang` + mốc đầu tiên `LichSuDonHang` → cộng `tongChiTieu`, `soDonHang` của khách → xóa món đã mua khỏi `GioHang` → tạo `ThongBao`)* | `ClientOrderController.java`<br>(`POST /api/v1/orders`) | `OrderService.java`<br>(`@Transactional createOrder`) | `DonHangRepository.java`<br>`ChiTietDonHangRepository.java`<br>`LichSuDonHangRepository.java`<br>`SachRepository.java`<br>`MaGiamGiaRepository.java`<br>`GioHangRepository.java` | `CreateOrderRequest.java`<br>`OrderDto.java` | `DonHang.java`, `ChiTietDonHang.java`, `LichSuDonHang.java`, `Sach.java`, `MaGiamGia.java`, `TkNguoiDung.java` |
| 6.2 | **Xem danh sách đơn hàng theo tab trạng thái (`CHO_XAC_NHAN`, `DANG_DONG_GOI`, `DANG_GIAO`, `DA_GIAO`, `DA_HUY`) & Xem Timeline hành trình đơn** | `ClientOrderController.java`<br>(`GET /api/v1/orders`, `GET /api/v1/orders/{id}`) | `OrderService.java` | `DonHangRepository.java`<br>`ChiTietDonHangRepository.java`<br>`LichSuDonHangRepository.java` | `OrderDto.java`<br>`OrderTimelineDto.java` | `DonHang.java`, `ChiTietDonHang.java`, `LichSuDonHang.java` |
| 6.3 | **Khách hủy đơn hàng** *(Chỉ cho phép khi `trangThaiDon == CHO_XAC_NHAN`; tự động hoàn lại `sach.tonKho` và ghi thêm mốc `DA_HUY` vào `LichSuDonHang`)* | `ClientOrderController.java`<br>(`POST /api/v1/orders/{id}/cancel`) | `OrderService.java`<br>(`@Transactional cancelOrder`) | `DonHangRepository.java`<br>`SachRepository.java`<br>`LichSuDonHangRepository.java` | `CancelOrderRequest.java`<br>`OrderDto.java` | `DonHang.java`, `Sach.java`, `LichSuDonHang.java` |
| 6.4 | **Gửi đánh giá 1–5 sao kèm nhận xét & ảnh** *(Chỉ mở khi đơn `DA_GIAO`; sau khi lưu `DanhGia`, tự động tính lại `sach.diemDanhGiaTb` và `sach.soLuotDanhGia`, đánh dấu `donHang.daDanhGia = true`)* | `ClientReviewReturnController.java`<br>(`POST /api/v1/reviews`) | `ReviewReturnService.java`<br>(`@Transactional submitReview`) | `DanhGiaRepository.java`<br>`SachRepository.java`<br>`DonHangRepository.java` | `CreateReviewRequest.java`<br>`ReviewDto.java` | `DanhGia.java`, `Sach.java`, `DonHang.java` |
| 6.5 | **Gửi yêu cầu Đổi/Trả hàng kèm lý do & ảnh minh chứng** *(1 đơn chỉ tạo tối đa 1 yêu cầu ở trạng thái `CHO_DUYET`)* | `ClientReviewReturnController.java`<br>(`POST /api/v1/returns`) | `ReviewReturnService.java`<br>(`@Transactional submitReturnRequest`) | `YeuCauDoiTraRepository.java`<br>`DonHangRepository.java` | `CreateReturnRequest.java`<br>`ReturnRequestDto.java` | `YeuCauDoiTra.java` (`TrangThaiDoiTra.CHO_DUYET`) |
| 6.6 | **Xem danh sách Thông báo & Đánh dấu đã đọc** | `ClientNotificationController.java`<br>(`GET /api/v1/notifications`, `PATCH /api/v1/notifications/{id}/read`) | `NotificationService.java` | `ThongBaoRepository.java`<br>(Lấy thông báo riêng của `nguoiDungId` + thông báo chung `nguoiDung IS NULL`) | `NotificationDto.java` | `ThongBao.java` |

---

### 7. Nhóm chức năng Hộp Thoại Tư Vấn Viên AI + Tìm Kiếm Ngữ Nghĩa Vector + Tự Đặt Sách (`FloatingAiChatbot.tsx`)

| STT | Tính năng Tư vấn & AI | Controller | AI & Service (`ai/` & `service/`) | Repository (`repository/`) | DTO (`dto/`) | Entity & Config |
| :-: | :--- | :--- | :--- | :--- | :--- | :--- |
| 7.1 | **Đồng bộ dữ liệu 186 cuốn sách sang Vector 768 chiều** *(Ghép `Tên sách + Tác giả + Danh mục + NXB + Tóm tắt + Từ khóa` → gọi `EmbeddingModel` → lưu vào `sach.vector_ngu_nghia`)* | `AdminBookCategoryController.java`<br>(`POST /api/admin/books/sync-vectors`) | `BookVectorService.java` | `SachRepository.java` | `ApiResponse<String>` | `Sach.java` (`vectorNguNghia`), `AiConfig.java` |
| 7.2 | **Khách hỏi tìm sách theo nội dung / tên tác giả / chủ đề / tóm tắt sách** | `ClientChatController.java`<br>(`POST /api/v1/chat/ask`) | `AdvisorChatService.java`<br>`BookstoreAiTools.java` (`searchBooksBySemanticOrAuthor`)<br>`BookVectorService.java` | `SachRepository.java`<br>(Truy vấn kết hợp `vector_ngu_nghia <=> :queryVector` và `ILIKE` tên tác giả/tên sách) | `ChatRequest.java`<br>`ChatResponse.java` (chứa `reply` + `suggestedBooks`) | `Sach.java`, `TacGia.java`, `AiConfig.java` |
| 7.3 | **Khách yêu cầu Tư vấn viên thêm sách vào giỏ hoặc tự đặt sách giúp** *(Model gọi Tool → Tool gọi `CartWishlistService` hoặc `OrderService` + lấy địa chỉ mặc định từ `SoDiaChiRepository`)* | `ClientChatController.java`<br>(`POST /api/v1/chat/ask`) | `AdvisorChatService.java`<br>`BookstoreAiTools.java` (`addBookToCustomerCart`, `placeOrderForCustomer`, `getActiveVouchers`)<br>`OrderService.java` | `GioHangRepository.java`<br>`SoDiaChiRepository.java`<br>`DonHangRepository.java` | `ChatRequest.java`<br>`ChatResponse.java` | `GioHang.java`, `DonHang.java`, `SoDiaChi.java` |

---

## PHẦN II. CÁC CHỨC NĂNG TRÊN TRANG WEB QUẢN TRỊ ADMIN (`frontend/admin-web/`)

> Toàn bộ cổng `/api/admin/**` được bảo vệ bởi `SecurityConfig.java` + `JwtAuthenticationFilter.java` và phân quyền chi tiết cho 3 vai trò **`ADMIN`**, **`WAREHOUSE_MANAGER`**, **`CSKH`** theo đúng [PHAN_QUYEN_WEB_ADMIN.md](PHAN_QUYEN_WEB_ADMIN.md).

| Trang Web Admin (`src/pages/`) | Tính năng cụ thể trên trang | Quyền được phép (`@PreAuthorize`) | Controller (`controller/admin/`) | Service (`service/`) | Repository (`repository/`) | Entity (`entity/`) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **1. Đăng nhập Web Admin** | Đăng nhập vào Dashboard (Chặn hoàn toàn `CUSTOMER`) | `ADMIN`, `WAREHOUSE_MANAGER`, `CSKH` | `ClientAuthController.java` (hoặc `/api/admin/auth/login`) | `AuthService.java` | `TkNguoiDungRepository.java` | `TkNguoiDung.java` |
| **2. Tổng quan** (`Dashboard.tsx`) | • 4 thẻ KPI (Doanh thu, Lợi nhuận, Tổng đơn, Khách mới)<br>• Biểu đồ Doanh thu & Lợi nhuận theo Ngày/Tháng/Năm<br>• Top sách bán chạy & Cảnh báo sách sắp hết (`tonKho <= 5`) | • `ADMIN`: Xem toàn bộ<br>• `WAREHOUSE_MANAGER`: Chỉ số kho<br>• `CSKH`: Chỉ số đơn & đổi trả | `AdminDashboardController.java` | `ReportService.java` | `DonHangRepository.java`<br>`SachRepository.java`<br>`TkNguoiDungRepository.java`<br>`PhieuNhapKhoRepository.java` | `DonHang.java`, `Sach.java`, `PhieuNhapKho.java` |
| **3. Quản lý Sách** (`Books.tsx`) | • Tìm kiếm, lọc sách<br>• Thêm sách mới / Sửa thông tin sách<br>• Bật/tắt hiển thị (`hienThi`), bật Flash Sale (`dangFlashSale`, `giaFlashSale`)<br>• Xóa sách (chặn xóa nếu sách đã từng nằm trong `ChiTietDonHang`) | • `ADMIN`: Toàn quyền<br>• `WAREHOUSE_MANAGER`: Thêm/Sửa thông tin sách (không sửa giá/xóa)<br>• `CSKH`: Chỉ xem | `AdminBookCategoryController.java` | `BookService.java`<br>`BookVectorService.java` (cập nhật lại vector khi sửa sách) | `SachRepository.java`<br>`ChiTietDonHangRepository.java` (`existsBySachId`) | `Sach.java`, `DanhMuc.java`, `TacGia.java`, `NhaXuatBan.java` |
| **4. Danh mục, Tác giả & NXB** (`Categories.tsx`) | • CRUD Danh mục Cha–Con (`DanhMuc`)<br>• CRUD Tác giả (`TacGia`)<br>• CRUD Nhà xuất bản (`NhaXuatBan`) | • `ADMIN`, `WAREHOUSE_MANAGER`: Thêm/Sửa<br>• `CSKH`: Chỉ xem | `AdminBookCategoryController.java` | `BookService.java` | `DanhMucRepository.java`<br>`TacGiaRepository.java`<br>`NhaXuatBanRepository.java` | `DanhMuc.java`, `TacGia.java`, `NhaXuatBan.java` |
| **5. Quản lý Kho hàng** (`Inventory.tsx`) | • Xem danh sách sách sắp hết hàng (`tonKho <= 5`)<br>• Xem lịch sử Phiếu nhập kho<br>• **Lập Phiếu nhập kho mới** (`@Transactional`: lưu `PhieuNhapKho` + `ChiTietPhieuNhap` + cộng dồn `sach.tonKho`) | `ADMIN`, `WAREHOUSE_MANAGER`<br>*(Chặn `CSKH`)* | `AdminInventoryController.java` | `InventoryService.java` | `PhieuNhapKhoRepository.java`<br>`ChiTietPhieuNhapRepository.java`<br>`SachRepository.java` | `PhieuNhapKho.java`, `ChiTietPhieuNhap.java`, `Sach.java` |
| **6. Đơn hàng & Đổi trả** (`Orders.tsx`) | • Lọc đơn theo trạng thái, xem chi tiết đơn<br>• Chuyển trạng thái đơn 1 chiều (`CHO_XAC_NHAN` → `DANG_DONG_GOI` → `DANG_GIAO` → `DA_GIAO`) & ghi `LichSuDonHang` + gửi `ThongBao`<br>• Duyệt (`DA_CHAP_THUAN`) hoặc Từ chối (`TU_CHOI`) yêu cầu đổi trả (`YeuCauDoiTra`) | • `ADMIN`: Toàn quyền<br>• `WAREHOUSE_MANAGER`: Chuyển trạng thái đóng gói/xuất kho<br>• `CSKH`: Xác nhận/hủy đơn & Duyệt đổi trả | `AdminOrderReturnController.java` | `OrderService.java`<br>`ReviewReturnService.java`<br>`NotificationService.java` | `DonHangRepository.java`<br>`LichSuDonHangRepository.java`<br>`YeuCauDoiTraRepository.java`<br>`ThongBaoRepository.java` | `DonHang.java`, `LichSuDonHang.java`, `YeuCauDoiTra.java`, `ThongBao.java` |
| **7. Khách hàng & Nhân sự** (`CustomersStaff.tsx`) | • Xem danh sách khách hàng (`CUSTOMER`), khóa/mở khóa tài khoản (`biKhoa`)<br>• Xem danh sách nhân viên, tạo nhân viên mới & gán vai trò (`ADMIN`, `WAREHOUSE_MANAGER`, `CSKH`) | • `ADMIN`: Toàn quyền<br>• `CSKH`: Chỉ xem danh sách khách hàng | `AdminUserController.java` | `UserAddressService.java` | `TkNguoiDungRepository.java` | `TkNguoiDung.java` (`VaiTro`) |
| **8. Khuyến mãi, Đánh giá & Thông báo** (`Vouchers.tsx`) | • Tạo/Sửa/Bật-Tắt mã giảm giá (`MaGiamGia`)<br>• Kiểm duyệt bình luận: Ẩn/Hiện đánh giá vi phạm (`danhGia.biAn`)<br>• Phát thông báo khuyến mãi/hệ thống (`ThongBao`) | • `ADMIN`: Toàn quyền<br>• `CSKH`: Xem voucher, Ẩn/Hiện đánh giá, Gửi thông báo | `AdminVoucherController.java`<br>`AdminReviewNotifController.java` | `VoucherService.java`<br>`ReviewReturnService.java`<br>`NotificationService.java` | `MaGiamGiaRepository.java`<br>`DanhGiaRepository.java`<br>`ThongBaoRepository.java` | `MaGiamGia.java`, `DanhGia.java`, `ThongBao.java` |
| **9. Báo cáo CSV** (`ReportsSettings.tsx`) | • Xuất 3 file báo cáo CSV có UTF-8 BOM (Doanh thu & Lợi nhuận, Tồn kho sách, Danh sách Đơn hàng) | `ADMIN` | `AdminDashboardController.java` | `ReportService.java` | `DonHangRepository.java`<br>`SachRepository.java`<br>`PhieuNhapKhoRepository.java` | `DonHang.java`, `Sach.java`, `PhieuNhapKho.java` |

---

## PHẦN III. TỔNG HỢP DANH SÁCH FILE CẦN CÓ TRONG TỪNG PACKAGE BACKEND

1. **`entity/` (Đã hoàn thành 100% — 8 Enum & 18 class `@Entity`)**:
   - `enums/` (8 file): `VaiTro`, `LoaiGiam`, `PhuongThucThanhToan`, `TrangThaiThanhToan`, `TrangThaiDon`, `TrangThaiDoiTra`, `LoaiThongBao`, `LoaiHanhVi`
   - 18 class Entity: `TkNguoiDung`, `SoDiaChi`, `DanhMuc`, `TacGia`, `NhaXuatBan`, `Sach`, `SachYeuThich`, `HanhViNguoiDung`, `GioHang`, `PhieuNhapKho`, `ChiTietPhieuNhap`, `MaGiamGia`, `DonHang`, `ChiTietDonHang`, `LichSuDonHang`, `DanhGia`, `YeuCauDoiTra`, `ThongBao`
2. **`repository/` (18 interface tương ứng 18 bảng)**:
   - `TkNguoiDungRepository`, `SoDiaChiRepository`, `DanhMucRepository`, `TacGiaRepository`, `NhaXuatBanRepository`, `SachRepository`, `SachYeuThichRepository`, `HanhViNguoiDungRepository`, `GioHangRepository`, `PhieuNhapKhoRepository`, `ChiTietPhieuNhapRepository`, `MaGiamGiaRepository`, `DonHangRepository`, `ChiTietDonHangRepository`, `LichSuDonHangRepository`, `DanhGiaRepository`, `YeuCauDoiTraRepository`, `ThongBaoRepository`
3. **`dto/request/` & `dto/response/`**:
   - Request: `LoginRequest`, `RegisterRequest`, `UpdateProfileRequest`, `AddressUpsertRequest`, `BookFilterRequest`, `BookUpsertRequest`, `CategoryUpsertRequest`, `CartItemRequest`, `ApplyVoucherRequest`, `VoucherUpsertRequest`, `CreateOrderRequest`, `UpdateOrderStatusRequest`, `CreateInventoryReceiptRequest`, `CreateReviewRequest`, `CreateReturnRequest`, `ProcessReturnRequest`, `CreateStaffRequest`, `CreateNotificationRequest`, `TrackBehaviorRequest`, `ChatRequest`
   - Response: `ApiResponse<T>`, `AuthResponse`, `UserDto`, `AddressDto`, `CategoryDto`, `BookDto`, `BookDetailDto`, `CartResponseDto`, `VoucherDto`, `OrderDto`, `InventoryReceiptDto`, `ReviewDto`, `ReturnRequestDto`, `NotificationDto`, `RecommendationResponse`, `DashboardStatsDto`, `ChatResponse`
4. **`service/` (11 file)**:
   - `AuthService`, `BookService`, `CartWishlistService`, `OrderService`, `InventoryService`, `ReviewReturnService`, `VoucherService`, `UserAddressService`, `NotificationService`, `RecommendationTrackingService`, `ReportService`
5. **`ai/` (3 file)**:
   - `BookVectorService`, `BookstoreAiTools`, `AdvisorChatService`
6. **`controller/client/` (11 file) & `controller/admin/` (7 file)**:
   - Client: `ClientAuthController`, `ClientBookController`, `ClientCartController`, `ClientWishlistController`, `ClientAddressController`, `ClientVoucherController`, `ClientOrderController`, `ClientReviewReturnController`, `ClientNotificationController`, `ClientTrackingController`, `ClientChatController`
   - Admin: `AdminDashboardController`, `AdminBookCategoryController`, `AdminInventoryController`, `AdminOrderReturnController`, `AdminVoucherController`, `AdminUserController`, `AdminReviewNotifController`
7. **`config/` (5 file) & `exception/` (2 file)**:
   - Config: `CorsConfig`, `SecurityConfig`, `JwtTokenProvider`, `JwtAuthenticationFilter`, `AiConfig`
   - Exception: `BusinessException`, `GlobalExceptionHandler`
