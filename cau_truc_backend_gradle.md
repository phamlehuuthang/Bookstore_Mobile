# CẤU TRÚC BACKEND SPRING BOOT + GRADLE

**Dự án:** Hệ sinh thái Thương mại điện tử Nhà sách Tri Thức Việt (BookStore)  
**Công nghệ:** Java 21, Spring Boot, Gradle, PostgreSQL 16 + pgvector (`bookstore_db`)  
**Giải thích chi tiết từng file:** xem [giai_thich_cau_truc_backend.md](giai_thich_cau_truc_backend.md)

---

```text
backend/
├── build.gradle                              # Khai báo Java 21, Spring Boot, Web, Data JPA, Security, JWT, PostgreSQL, hibernate-vector, Spring AI
├── settings.gradle                           # Tên project: bookstore-backend
├── gradlew / gradlew.bat                     # Gradle Wrapper (chạy không cần cài Gradle rời)
├── gradle/wrapper/                           # File cấu hình phiên bản Gradle Wrapper
│
└── src/
    ├── main/
    │   ├── resources/
    │   │   ├── application.properties        # Kết nối bookstore_db (5432), ddl-auto=validate, JWT Secret, Gemini/OpenAI Key
    │   │   ├── static/                       # Tài nguyên tĩnh
    │   │   └── templates/                    # Template giao diện (nếu dùng)
    │   │
    │   └── java/com/trithucviet/bookstore/
    │       ├── BookstoreApplication.java     # Entry point khởi chạy Spring Boot
    │       │
    │       ├── config/                       # 1. CẤU HÌNH HỆ THỐNG & BẢO MẬT
    │       │   ├── CorsConfig.java           # Cho phép Mobile (8081) & Web Admin (5173) gọi API không bị chặn CORS
    │       │   ├── SecurityConfig.java       # @EnableMethodSecurity, phân luồng bảo mật /api/v1/** và /api/admin/**
    │       │   ├── JwtTokenProvider.java     # Sinh & giải mã JWT Token (nhúng userId, vai_tro, thời hạn)
    │       │   ├── JwtAuthenticationFilter.java # Bộ lọc kiểm tra header Authorization: Bearer <token> trước mỗi request
    │       │   └── AiConfig.java             # Cấu hình ChatClient + EmbeddingModel + System Prompt cho Tư vấn viên
    │       │
    │       ├── entity/                       # 2. ÁNH XẠ ĐỦ 18 BẢNG TIẾNG VIỆT TRONG bookstore_db
    │       │   ├── enums/                    # 8 Enum: VaiTro, LoaiGiam, PhuongThucThanhToan, TrangThaiThanhToan, TrangThaiDon, TrangThaiDoiTra, LoaiThongBao, LoaiHanhVi
    │       │   ├── TkNguoiDung.java          # @Table(name = "tk_nguoi_dung") - Tài khoản khách hàng & 3 vai trò nhân sự
    │       │   ├── SoDiaChi.java             # @Table(name = "so_dia_chi") - Sổ địa chỉ nhận hàng của khách
    │       │   ├── DanhMuc.java              # @Table(name = "danh_muc") - Danh mục sách phân cấp Cha - Con
    │       │   ├── TacGia.java               # @Table(name = "tac_gia") - Thông tin tác giả
    │       │   ├── NhaXuatBan.java           # @Table(name = "nha_xuat_ban") - Thông tin nhà xuất bản
    │       │   ├── Sach.java                 # @Table(name = "sach") - Thông tin sách, tồn kho, giá bán & cột vector_ngu_nghia vector(768)
    │       │   ├── SachYeuThich.java         # @Table(name = "sach_yeu_thich") - Tủ sách yêu thích (khóa chính ghép)
    │       │   ├── HanhViNguoiDung.java      # @Table(name = "hanh_vi_nguoi_dung") - User Tracking, thời gian dừng xem sách (Dwell time) & điểm chủ đề
    │       │   ├── GioHang.java              # @Table(name = "gio_hang") - Giỏ hàng của khách (khóa chính ghép)
    │       │   ├── PhieuNhapKho.java         # @Table(name = "phieu_nhap_kho") - Đầu phiếu nhập kho hàng từ nhà cung cấp
    │       │   ├── ChiTietPhieuNhap.java     # @Table(name = "chi_tiet_phieu_nhap") - Từng dòng sách, số lượng và giá nhập trong phiếu nhập
    │       │   ├── MaGiamGia.java            # @Table(name = "ma_giam_gia") - Mã giảm giá (CO_DINH / PHAN_TRAM)
    │       │   ├── DonHang.java              # @Table(name = "don_hang") - Đơn đặt hàng của khách
    │       │   ├── ChiTietDonHang.java       # @Table(name = "chi_tiet_don_hang") - Từng cuốn sách và đơn giá chốt tại thời điểm mua
    │       │   ├── LichSuDonHang.java        # @Table(name = "lich_su_don_hang") - Timeline hành trình trạng thái đơn hàng
    │       │   ├── DanhGia.java              # @Table(name = "danh_gia") - Đánh giá 1-5 sao và bình luận của độc giả
    │       │   ├── YeuCauDoiTra.java         # @Table(name = "yeu_cau_doi_tra") - Yêu cầu đổi/trả hàng kèm ảnh minh chứng
    │       │   └── ThongBao.java             # @Table(name = "thong_bao") - Thông báo đơn hàng và khuyến mãi
    │       │   # (Tạm chưa tạo CauHinhCuaHang.java vì bảng cau_hinh_cua_hang đang được tạm ẩn trong DB)
    │       │
    │       ├── repository/                   # 3. GIAO TIẾP DATABASE POSTGRESQL + PGVECTOR (18 INTERFACE)
    │       │   ├── TkNguoiDungRepository.java        # Tìm tài khoản theo email, lọc khách hàng & nhân sự theo vai_tro
    │       │   ├── SoDiaChiRepository.java           # Lấy danh sách địa chỉ và địa chỉ mặc định của khách
    │       │   ├── DanhMucRepository.java            # Truy vấn cây danh mục cha - con đang hoạt động
    │       │   ├── TacGiaRepository.java             # Tìm kiếm và quản lý tác giả
    │       │   ├── NhaXuatBanRepository.java         # Tìm kiếm và quản lý nhà xuất bản
    │       │   ├── SachRepository.java               # Lọc giá, danh mục, tồn kho <= 5 & query tương đồng cosine (<=>) trên vector_ngu_nghia
    │       │   ├── SachYeuThichRepository.java       # Kiểm tra, thêm/xóa sách yêu thích của khách
    │       │   ├── HanhViNguoiDungRepository.java    # Truy vấn lịch sử hành vi (Dwell time, lượt bỏ qua) để tính điểm đề xuất đa chủ đề
    │       │   ├── GioHangRepository.java            # Lấy các sản phẩm trong giỏ, lọc món đã chọn để thanh toán
    │       │   ├── PhieuNhapKhoRepository.java       # Lịch sử phiếu nhập kho và tổng chi phí nhập hàng
    │       │   ├── ChiTietPhieuNhapRepository.java   # Chi tiết các dòng sách trong từng phiếu nhập kho
    │       │   ├── MaGiamGiaRepository.java          # Tìm mã giảm giá theo ma_code và lọc voucher còn hiệu lực
    │       │   ├── DonHangRepository.java            # Lọc đơn theo trạng thái, khách hàng và thống kê doanh thu
    │       │   ├── ChiTietDonHangRepository.java     # Lấy chi tiết đơn & kiểm tra sách đã từng phát sinh đơn hàng chưa
    │       │   ├── LichSuDonHangRepository.java      # Lấy các mốc thời gian (timeline) của đơn hàng
    │       │   ├── DanhGiaRepository.java            # Lấy đánh giá hiển thị của sách & lọc bình luận cho CSKH kiểm duyệt
    │       │   ├── YeuCauDoiTraRepository.java       # Truy vấn yêu cầu đổi trả theo đơn hàng và trạng thái duyệt
    │       │   └── ThongBaoRepository.java           # Lấy thông báo riêng của khách + thông báo khuyến mãi chung
    │       │
    │       ├── dto/                          # 4. DỮ LIỆU VÀO/RA (TÁCH BIỆT ENTITY & KHỚP FRONTEND)
    │       │   ├── request/                  # Dữ liệu Client gửi lên: LoginRequest, CartItemRequest, CreateOrderRequest, TrackBehaviorRequest, ChatRequest...
    │       │   └── response/                 # Dữ liệu Server trả về: ApiResponse<T>, AuthResponse, BookDto, OrderDto, RecommendationResponse, ChatResponse...
    │       │
    │       ├── service/                      # 5. LOGIC NGHIỆP VỤ & TRANSACTIONAL
    │       │   ├── AuthService.java                  # Đăng ký, đăng nhập; chặn CUSTOMER vào Web Admin (chỉ cho ADMIN, WAREHOUSE_MANAGER, CSKH)
    │       │   ├── BookService.java                  # Tìm kiếm/lọc sách, CRUD sách/danh mục/tác giả/NXB (chặn xóa sách đã có trong đơn)
    │       │   ├── CartWishlistService.java          # Quản lý gio_hang (chặn vượt tồn kho) & sach_yeu_thich
    │       │   ├── OrderService.java                 # @Transactional đặt hàng (trừ ton_kho, tăng da_su_dung voucher) & hủy đơn (hoàn kho)
    │       │   ├── InventoryService.java             # @Transactional tạo phieu_nhap_kho & tự động cộng dồn sach.ton_kho
    │       │   ├── ReviewReturnService.java          # Gửi đánh giá 1-5 sao (tính lại điểm TB sách) & duyệt/từ chối yeu_cau_doi_tra
    │       │   ├── VoucherService.java               # Kiểm tra điều kiện áp mã, tính tiền giảm & CRUD ma_giam_gia
    │       │   ├── UserAddressService.java           # Quản lý so_dia_chi (1 mặc định), khóa/mở khách hàng & tạo tài khoản nhân sự
    │       │   ├── NotificationService.java          # Tạo thông báo tự động khi đơn đổi trạng thái, phát thông báo & đánh dấu đã đọc
    │       │   ├── RecommendationTrackingService.java # Tính điểm hành vi (Dwell time + Time Decay + Impression Fatigue) & đề xuất đa chủ đề 3:2:1
    │       │   └── ReportService.java                # Thống kê KPI Dashboard, biểu đồ Doanh thu - Lợi nhuận & xuất file CSV
    │       │
    │       ├── ai/                           # 6. MODULE TƯ VẤN VIÊN & VECTOR
    │       │   ├── BookVectorService.java    # Ghép Tên sách + Tác giả + Danh mục + NXB + Tóm tắt + Từ khóa -> vector(768)
    │       │   ├── BookstoreAiTools.java     # Tool cho AI (Function Calling): tìm sách, xem voucher, lấy địa chỉ mặc định, thêm giỏ, tạo đơn
    │       │   └── AdvisorChatService.java   # Điều phối hội thoại & trả về câu trả lời tiếng Việt + danh sách sách gợi ý
    │       │
    │       ├── controller/                   # 7. REST API CHIA RÕ MOBILE & ADMIN THEO PHÂN QUYỀN
    │       │   ├── client/                   # Dành cho Mobile App Khách hàng (/api/v1/**)
    │       │   │   ├── ClientAuthController.java          # Đăng ký, đăng nhập, xem & cập nhật hồ sơ cá nhân (/api/v1/auth/**)
    │       │   │   ├── ClientBookController.java          # Trang chủ, cây danh mục, lọc/tìm kiếm sách & chi tiết sách (/api/v1/books/**)
    │       │   │   ├── ClientCartController.java          # Xem giỏ hàng, thêm sách, tăng/giảm số lượng, xóa món (/api/v1/cart/**)
    │       │   │   ├── ClientWishlistController.java      # Xem tủ sách yêu thích, bấm thả tim / bỏ tim (/api/v1/wishlists/**)
    │       │   │   ├── ClientAddressController.java       # Thêm/sửa/xóa địa chỉ nhận hàng & đặt địa chỉ mặc định (/api/v1/addresses/**)
    │       │   │   ├── ClientVoucherController.java       # Lấy danh sách voucher đang mở & kiểm tra mã giảm giá (/api/v1/vouchers/**)
    │       │   │   ├── ClientOrderController.java         # Đặt hàng, xem lịch sử & timeline đơn, hủy đơn CHO_XAC_NHAN (/api/v1/orders/**)
    │       │   │   ├── ClientReviewReturnController.java  # Gửi đánh giá 1-5 sao & gửi yêu cầu đổi/trả hàng (/api/v1/reviews, /api/v1/returns)
    │       │   │   ├── ClientNotificationController.java  # Xem danh sách thông báo & đánh dấu đã đọc (/api/v1/notifications/**)
    │       │   │   ├── ClientTrackingController.java      # Ghi nhận User Tracking (Dwell time) & trả kệ "Đề xuất dành riêng cho bạn"
    │       │   │   └── ClientChatController.java          # Hộp thoại Tư vấn viên hỗ trợ tìm sách & đặt sách (/api/v1/chat/ask)
    │       │   │
    │       │   └── admin/                    # Dành cho Web Admin (/api/admin/** - gắn @PreAuthorize theo PHAN_QUYEN_WEB_ADMIN.md)
    │       │       ├── AdminDashboardController.java      # ADMIN (full KPI, biểu đồ, xuất CSV), KHO (chỉ số kho), CSKH (chỉ số đơn/đổi trả)
    │       │       ├── AdminBookCategoryController.java   # ADMIN (full), WAREHOUSE_MANAGER (thêm/sửa sách & danh mục, không sửa giá/xóa)
    │       │       ├── AdminInventoryController.java      # ADMIN, WAREHOUSE_MANAGER (lập phiếu nhập kho & cảnh báo tồn kho; chặn CSKH)
    │       │       ├── AdminOrderReturnController.java    # ADMIN (full), WAREHOUSE_MANAGER (đóng gói/xuất kho), CSKH (xác nhận đơn & duyệt đổi trả)
    │       │       ├── AdminVoucherController.java        # ADMIN (tạo/sửa/tắt mã giảm giá), CSKH (chỉ xem mã để tư vấn khách)
    │       │       ├── AdminUserController.java           # ADMIN (quản lý khách + tạo/khóa/phân quyền nhân sự), CSKH (chỉ xem khách hàng)
    │       │       └── AdminReviewNotifController.java    # ADMIN, CSKH (ẩn/hiện bình luận đánh giá vi phạm & phát thông báo khuyến mãi)
    │       │
    │       └── exception/                    # 8. XỬ LÝ LỖI TẬP TRUNG
    │           ├── BusinessException.java           # Lỗi nghiệp vụ chủ động (VD: "Tồn kho không đủ", "Đơn chưa đủ điều kiện áp mã")
    │           └── GlobalExceptionHandler.java      # @RestControllerAdvice bắt mọi lỗi và trả JSON tiếng Việt thống nhất (400/401/403/404/500)
    │
    └── test/
        └── java/com/trithucviet/bookstore/
            └── BookstoreApplicationTests.java       # Kiểm thử khởi động Spring Boot & xác nhận 18 Entity khớp 100% với PostgreSQL
```
