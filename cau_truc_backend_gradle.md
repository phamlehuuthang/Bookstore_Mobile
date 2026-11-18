# CẤU TRÚC BACKEND SPRING BOOT + GRADLE

**Dự án:** Hệ sinh thái Thương mại điện tử Nhà sách Tri Thức Việt (BookStore)  
**Công nghệ:** Java 21, Spring Boot, Gradle, PostgreSQL 16 + pgvector (`bookstore_db`)  
**Giải thích chi tiết từng file:** xem [giai_thich_cau_truc_backend.md](giai_thich_cau_truc_backend.md)

---

```text
backend/
├── build.gradle                              # Khai báo Java 21, Spring Boot, Web, Data JPA, Security, JWT, PostgreSQL, pgvector, Spring AI
├── settings.gradle                           # Tên project: bookstore-backend
├── gradlew / gradlew.bat                     # Gradle Wrapper (chạy không cần cài Gradle rời)
├── gradle/wrapper/                           # File cấu hình Gradle Wrapper
│
└── src/
    ├── main/
    │   ├── resources/
    │   │   ├── application.yml               # Kết nối bookstore_db (5432), ddl-auto: validate, JWT Secret, Gemini/OpenAI Key
    │   │   └── application-dev.yml           # Cấu hình riêng cho môi trường dev (log SQL, CORS localhost)
    │   │
    │   └── java/com/trithucviet/bookstore/
    │       ├── BookstoreApplication.java     # Entry point Spring Boot
    │       │
    │       ├── config/                       # 1. CẤU HÌNH HỆ THỐNG & BẢO MẬT
    │       │   ├── CorsConfig.java           # Cho phép Mobile (8081) & Web Admin (5173) gọi API
    │       │   ├── SecurityConfig.java       # @EnableMethodSecurity, phân luồng /api/v1/** và /api/admin/**
    │       │   ├── JwtTokenProvider.java     # Sinh & giải mã JWT (chứa userId, vai_tro)
    │       │   ├── JwtAuthenticationFilter.java
    │       │   └── AiConfig.java             # Cấu hình ChatClient + EmbeddingModel + System Prompt Tư vấn viên
    │       │
    │       ├── entity/                       # 2. ÁNH XẠ ĐỦ 18 BẢNG TIẾNG VIỆT TRONG bookstore_db
    │       │   ├── enums/                    # VaiTro, TrangThaiDon, LoaiGiam, LoaiHanhVi...
    │       │   ├── TkNguoiDung.java          # @Table(name = "tk_nguoi_dung")
    │       │   ├── SoDiaChi.java             # @Table(name = "so_dia_chi")
    │       │   ├── DanhMuc.java              # @Table(name = "danh_muc")
    │       │   ├── TacGia.java               # @Table(name = "tac_gia")
    │       │   ├── NhaXuatBan.java           # @Table(name = "nha_xuat_ban")
    │       │   ├── Sach.java                 # @Table(name = "sach") - có vector_ngu_nghia vector(768)
    │       │   ├── SachYeuThich.java         # @Table(name = "sach_yeu_thich")
    │       │   ├── HanhViNguoiDung.java      # @Table(name = "hanh_vi_nguoi_dung") - User Tracking & Dwell time
    │       │   ├── GioHang.java              # @Table(name = "gio_hang")
    │       │   ├── PhieuNhapKho.java         # @Table(name = "phieu_nhap_kho")
    │       │   ├── ChiTietPhieuNhap.java     # @Table(name = "chi_tiet_phieu_nhap")
    │       │   ├── MaGiamGia.java            # @Table(name = "ma_giam_gia")
    │       │   ├── DonHang.java              # @Table(name = "don_hang")
    │       │   ├── ChiTietDonHang.java       # @Table(name = "chi_tiet_don_hang")
    │       │   ├── LichSuDonHang.java        # @Table(name = "lich_su_don_hang")
    │       │   ├── DanhGia.java              # @Table(name = "danh_gia")
    │       │   ├── YeuCauDoiTra.java         # @Table(name = "yeu_cau_doi_tra")
    │       │   └── ThongBao.java             # @Table(name = "thong_bao")
    │       │   # (Tạm chưa tạo CauHinhCuaHang.java vì bảng cau_hinh_cua_hang đang được ẩn trong DB)
    │       │
    │       ├── repository/                   # 3. TRUY VẤN POSTGRESQL + PGVECTOR
    │       │   ├── SachRepository.java       # Lọc giá, danh mục, tồn kho <= 5 & query cosine (<=>) trên vector_ngu_nghia
    │       │   ├── DonHangRepository.java    # Lọc đơn theo trạng thái, thống kê doanh thu
    │       │   ├── TkNguoiDungRepository.java
    │       │   ├── GioHangRepository.java
    │       │   ├── MaGiamGiaRepository.java
    │       │   └── ...                       # Các Repository còn lại tương ứng từng bảng
    │       │
    │       ├── dto/                          # 4. DỮ LIỆU VÀO/RA (KHỚP FRONTEND)
    │       │   ├── request/                  # LoginRequest, CartItemRequest, CreateOrderRequest, BookUpsertRequest, ChatRequest...
    │       │   └── response/                 # ApiResponse<T>, AuthResponse, BookDto, OrderDto, DashboardStatsDto, ChatResponse...
    │       │
    │       ├── service/                      # 5. LOGIC NGHIỆP VỤ & TRANSACTIONAL
    │       │   ├── AuthService.java          # Chặn CUSTOMER vào Web Admin; cho phép ADMIN, WAREHOUSE_MANAGER, CSKH
    │       │   ├── BookService.java          # CRUD sách, danh mục, tác giả, NXB
    │       │   ├── CartWishlistService.java  # Quản lý gio_hang & sach_yeu_thich
    │       │   ├── OrderService.java         # @Transactional đặt hàng (trừ ton_kho, tăng da_su_dung voucher) & hủy đơn (hoàn kho)
    │       │   ├── InventoryService.java     # @Transactional tạo phieu_nhap_kho & cộng dồn ton_kho
    │       │   ├── ReviewReturnService.java  # Đánh giá 1-5 sao, duyệt/từ chối yeu_cau_doi_tra
    │       │   ├── VoucherService.java       # Kiểm tra điều kiện & CRUD ma_giam_gia
    │       │   ├── UserAddressService.java   # Quản lý so_dia_chi, danh sách khách hàng & nhân sự
    │       │   ├── NotificationService.java  # Gửi & đánh dấu đã đọc thong_bao
    │       │   ├── RecommendationTrackingService.java # Ghi nhận hành vi (thời gian dừng xem sách) & gợi ý sách cùng chủ đề
    │       │   └── ReportService.java        # Thống kê Dashboard, xuất CSV
    │       │
    │       ├── ai/                           # 6. MODULE TƯ VẤN VIÊN & VECTOR
    │       │   ├── BookVectorService.java    # Ghép Tên sách + Tác giả + Danh mục + NXB + Tóm tắt + Từ khóa -> vector(768)
    │       │   ├── BookstoreAiTools.java     # Tool cho AI: tìm sách, xem voucher, lấy địa chỉ mặc định, thêm giỏ, tạo đơn
    │       │   └── AdvisorChatService.java   # Điều phối hội thoại & trả về câu trả lời + danh sách sách gợi ý
    │       │
    │       ├── controller/                   # 7. REST API CHIA RÕ MOBILE & ADMIN THEO PHÂN QUYỀN
    │       │   ├── client/                   # Mobile App (/api/v1/**)
    │       │   │   ├── ClientAuthController.java
    │       │   │   ├── ClientBookController.java
    │       │   │   ├── ClientCartController.java
    │       │   │   ├── ClientWishlistController.java
    │       │   │   ├── ClientAddressController.java
    │       │   │   ├── ClientVoucherController.java
    │       │   │   ├── ClientOrderController.java
    │       │   │   ├── ClientReviewReturnController.java
    │       │   │   ├── ClientNotificationController.java
    │       │   │   ├── ClientTrackingController.java      # Ghi nhận User Tracking & trả kệ "Đề xuất dành riêng cho bạn"
    │       │   │   └── ClientChatController.java
    │       │   │
    │       │   └── admin/                    # Web Admin (/api/admin/** - gắn @PreAuthorize theo PHAN_QUYEN_WEB_ADMIN.md)
    │       │       ├── AdminDashboardController.java      # ADMIN (full), KHO/CSKH (chỉ số theo vai trò)
    │       │       ├── AdminBookCategoryController.java   # ADMIN (full), WAREHOUSE_MANAGER (sách/danh mục, không sửa giá/xóa)
    │       │       ├── AdminInventoryController.java      # ADMIN, WAREHOUSE_MANAGER (CSKH bị chặn)
    │       │       ├── AdminOrderReturnController.java    # ADMIN (full), WAREHOUSE_MANAGER (đóng gói/giao), CSKH (xác nhận & đổi trả)
    │       │       ├── AdminVoucherController.java        # ADMIN (CRUD), CSKH (chỉ xem)
    │       │       ├── AdminUserController.java           # ADMIN (full khách + nhân sự), CSKH (chỉ xem khách)
    │       │       └── AdminReviewNotifController.java    # ADMIN, CSKH (ẩn/hiện đánh giá, gửi thông báo)
    │       │
    │       └── exception/                    # 8. XỬ LÝ LỖI TẬP TRUNG
    │           ├── BusinessException.java
    │           └── GlobalExceptionHandler.java
    │
    └── test/
        └── java/com/trithucviet/bookstore/
            └── BookstoreApplicationTests.java
```
