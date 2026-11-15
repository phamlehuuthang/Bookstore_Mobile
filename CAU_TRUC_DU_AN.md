# 📁 TÀI LIỆU CẤU TRÚC DỰ ÁN & NHIỆM VỤ TỪNG FILE
**Dự án:** Hệ sinh thái Thương mại Điện tử Nhà sách Tri Thức Việt (`BookStore_Antigravity`)  
**Công nghệ:** Expo React Native (Mobile) • React + Vite (Web Admin) • Java 21 + Spring Boot + Gradle (Backend) • PostgreSQL 16 + `pgvector` (`bookstore_db` trên Docker)

> **Mục đích tài liệu:** Giúp người đọc, thành viên nhóm hoặc giảng viên chấm bài nắm bắt nhanh vị trí từng thư mục, từng file mã nguồn và nhiệm vụ cụ thể của chúng trong dự án.

---

## 📌 MỤC LỤC
1. [Sơ đồ Cây thư mục Tổng thể (Monorepo)](#1-sơ-đồ-cây-thư-mục-tổng-thể-monorepo)
2. [Các file cấu hình & tài liệu ở Thư mục Gốc](#2-các-file-cấu-hình--tài-liệu-ở-thư-mục-gốc)
3. [Phân hệ 1: Cơ sở dữ liệu PostgreSQL 16 + `pgvector` (`database/`)](#3-phân-hệ-1-cơ-sở-dữ-liệu-postgresql-16--pgvector-database)
4. [Phân hệ 2: Backend Spring Boot + Gradle (`backend/`)](#4-phân-hệ-2-backend-spring-boot--gradle-backend)
5. [Phân hệ 3: Thư mục Dùng chung Frontend (`shared/`)](#5-phân-hệ-3-thư-mục-dùng-chung-frontend-shared)
6. [Phân hệ 4: Mobile App Khách hàng (`frontend/client-mobile/`)](#6-phân-hệ-4-mobile-app-khách-hàng-frontendclient-mobile)
7. [Phân hệ 5: Web Quản trị Admin (`frontend/admin-web/`)](#7-phân-hệ-5-web-quản-trị-admin-frontendadmin-web)
8. [Luồng chạy dữ liệu (Data Flow) khi thuyết trình](#8-luồng-chạy-dữ-liệu-data-flow-khi-thuyết-trình)

---

## 1. Sơ đồ Cây thư mục Tổng thể (Monorepo)

Dự án được chia thành 5 phân hệ rõ ràng: **Cơ sở dữ liệu PostgreSQL + `pgvector`**, **Backend Spring Boot + Gradle (Java 21)**, **Thư mục dùng chung `shared/`**, **Mobile App cho Khách hàng (`CUSTOMER`)**, và **Web Quản trị cho Nhân sự (`ADMIN`, `WAREHOUSE_MANAGER`, `CSKH`)**:

```text
BookStore_Antigravity/
├── package.json                        # Quản lý workspace cho Frontend (shared, client-mobile, admin-web)
├── tsconfig.base.json                  # Cấu hình TypeScript chuẩn dùng chung
├── README.md                           # Hướng dẫn cài đặt, chạy dự án và xuất file APK
├── PHAN_TICH_NGHIEP_VU_VA_CSDL.md      # Tài liệu phân tích nghiệp vụ & thiết kế CSDL
├── PHAN_QUYEN_WEB_ADMIN.md             # Ma trận phân quyền 3 vai trò nhân viên (ADMIN, KHO, CSKH) trên Web Admin
├── cau_truc_backend_gradle.md          # Sơ đồ cây kiến trúc chi tiết của Backend Spring Boot + Gradle
├── giai_thich_cau_truc_backend.md      # Giải thích chi tiết nhiệm vụ từng file/package trong Backend
├── CAU_TRUC_DU_AN.md                   # Tài liệu tổng hợp cấu trúc toàn dự án (File này)
│
├── database/                           # [PHÂN HỆ 1] CƠ SỞ DỮ LIỆU POSTGRESQL 16 + PGVECTOR (bookstore_db)
│   ├── 01_schema.sql                   # Tạo extension vector & 17 bảng tiếng Việt không dấu + 6 index
│   └── 02_seed_data.sql                # Nạp 186 cuốn sách (12 danh mục, ảnh bìa khớp tên sách), 148 tác giả, 19 tài khoản...
│
├── backend/                            # [PHÂN HỆ 2] MÁY CHỦ SPRING BOOT + GRADLE (JAVA 21)
│   ├── build.gradle                    # Khai báo Java 21, Spring Boot, Web, Data JPA, Security, Validation, PostgreSQL...
│   ├── settings.gradle                 # Tên project: bookstore-backend
│   ├── gradlew / gradlew.bat           # Gradle Wrapper chạy trực tiếp không cần cài Gradle rời
│   ├── gradle/wrapper/                 # Cấu hình phiên bản Gradle Wrapper
│   └── src/
│       ├── main/
│       │   ├── resources/
│       │   │   ├── application.properties  # Cấu hình kết nối bookstore_db, port, JWT, AI API Key
│       │   │   ├── static/                 # Tài nguyên tĩnh
│       │   │   └── templates/              # Template giao diện (nếu dùng)
│       │   └── java/com/trithucviet/bookstore/
│       │       ├── BookstoreApplication.java   # Điểm khởi chạy chính của Spring Boot
│       │       ├── config/                     # Cấu hình CORS, Security, JWT Filter, AI ChatClient & Embedding
│       │       ├── entity/                     # Ánh xạ 17 bảng tiếng Việt không dấu trong bookstore_db
│       │       │   └── enums/                  # VaiTro, TrangThaiDon, LoaiGiam, PhuongThucThanhToan...
│       │       ├── repository/                 # Giao tiếp PostgreSQL + truy vấn tương đồng vector (<=>)
│       │       ├── dto/                        # Dữ liệu truyền nhận với Mobile & Web Admin
│       │       │   ├── request/                # Dữ liệu đầu vào (Login, Đặt hàng, Thêm giỏ, Chat...)
│       │       │   └── response/               # Dữ liệu trả về (ApiResponse, BookDto, OrderDto, ChatResponse...)
│       │       ├── service/                    # Xử lý nghiệp vụ & @Transactional (Đặt hàng, Nhập kho, Đổi trả...)
│       │       ├── ai/                         # Tư vấn viên AI đọc DB, đồng bộ vector(768) & tự đặt sách qua Tool
│       │       ├── controller/                 # Các cổng REST API
│       │       │   ├── client/                 # API cho Mobile App (/api/v1/**)
│       │       │   └── admin/                  # API cho Web Admin (/api/admin/** - phân quyền ADMIN/KHO/CSKH)
│       │       └── exception/                  # Bắt lỗi tập trung & trả thông báo tiếng Việt
│       └── test/
│           └── java/com/trithucviet/bookstore/
│               └── BookstoreApplicationTests.java
│
├── shared/                             # [PHÂN HỆ 3] DỮ LIỆU & KIỂU DỮ LIỆU DÙNG CHUNG FRONTEND
│   ├── index.ts                        # Điểm xuất (export) tất cả kiểu dữ liệu & API
│   ├── types/
│   │   └── index.ts                    # Khai báo cấu trúc Sách, Đơn hàng, User, Voucher...
│   └── api-client/
│       ├── mockData.ts                 # Dữ liệu mẫu giả lập cho Frontend
│       └── index.ts                    # Bộ xử lý API cho Mobile App và Web Admin
│
└── frontend/
    ├── client-mobile/                  # [PHÂN HỆ 4] MOBILE APP DÀNH CHO KHÁCH HÀNG (Expo)
    │   ├── app.json                    # Cấu hình tên App, icon, package Android xuất APK
    │   ├── eas.json                    # Cấu hình đóng gói file cài đặt .apk Android
    │   ├── metro.config.js             # Cấu hình kết nối thư mục shared/ cho React Native
    │   ├── app/                        # Chứa các màn hình điều hướng (Expo Router)
    │   │   ├── _layout.tsx             # Khung gốc bọc toàn App + gắn nút "Tư vấn" nổi
    │   │   ├── cart.tsx                # Màn hình Giỏ hàng & chọn mã giảm giá
    │   │   ├── checkout.tsx            # Màn hình Thanh toán & Đặt hàng thành công
    │   │   ├── book/
    │   │   │   └── [id].tsx            # Màn hình Chi tiết 1 cuốn sách (theo ID)
    │   │   └── (tabs)/                 # 4 màn hình chính trên thanh Tab dưới đáy
    │   │       ├── _layout.tsx         # Cấu hình thanh TabBar (Trang chủ, Danh mục, Đơn, Tôi)
    │   │       ├── index.tsx           # Tab 1: Trang chủ, Tìm kiếm trực tiếp, Flash Sale
    │   │       ├── catalog.tsx         # Tab 2: Danh mục & Bộ lọc sách đa tiêu chí
    │   │       ├── orders.tsx          # Tab 3: Theo dõi đơn, Hủy đơn, Đánh giá & Đổi trả
    │   │       └── profile.tsx         # Tab 4: Đăng nhập/Đăng ký, Sổ địa chỉ, Voucher, FAQ
    │   └── src/                        # Các thành phần xử lý bên dưới giao diện Mobile
    │       ├── theme/
    │       │   └── colors.ts           # Bảng màu thương hiệu và màu nhãn trạng thái đơn
    │       ├── components/common/
    │       │   ├── BannerCarousel.tsx  # Thanh trượt Banner khuyến mãi ở Trang chủ
    │       │   ├── BookCard.tsx        # Thẻ hiển thị 1 cuốn sách (Ảnh, Giá, Sao, Nút thêm giỏ)
    │       │   └── StateViews.tsx      # Giao diện lúc Đang tải (Skeleton), Rỗng, Lỗi mạng
    │       ├── features/ai-chatbot/
    │       │   └── FloatingAiChatbot.tsx # Nút tròn "Tư vấn" nổi & Cửa sổ chat hỗ trợ
    │       ├── hooks/
    │       │   ├── useAuth.tsx         # Quản lý trạng thái Đăng nhập, Sổ địa chỉ, Yêu thích
    │       │   ├── useCart.tsx         # Quản lý Giỏ hàng, tính phí ship và giảm giá Voucher
    │       │   └── useAiChat.tsx       # Quản lý lịch sử tin nhắn trong hộp thoại Tư vấn
    │       └── services/
    │           ├── authService.ts      # Gọi API Đăng nhập (chặn Admin), Đăng ký, Địa chỉ
    │           ├── bookService.ts      # Gọi API lấy danh sách sách, chi tiết, gửi đánh giá
    │           ├── orderService.ts     # Gọi API tạo đơn hàng, hủy đơn, gửi yêu cầu đổi trả
    │           └── aiService.ts        # Xử lý câu hỏi gợi ý nhanh trong hộp thoại Tư vấn
    │
    └── admin-web/                      # [PHÂN HỆ 5] WEB QUẢN TRỊ DÀNH CHO ADMIN & NHÂN VIÊN (React + Vite)
        ├── index.html                  # Trang HTML gốc của Web Admin
        ├── vite.config.ts              # Cấu hình chạy máy chủ Web ở cổng 5173
        ├── tailwind.config.js          # Cấu hình màu sắc giao diện TailwindCSS
        └── src/
            ├── main.tsx                # Điểm khởi chạy đầu tiên gắn React vào index.html
            ├── App.tsx                 # Bọc bộ định tuyến trình duyệt (BrowserRouter)
            ├── routes/
            │   └── AppRoutes.tsx       # Định nghĩa đường dẫn URL cho 8 trang quản trị
            ├── layouts/
            │   └── AdminLayout.tsx     # Khung giao diện Desktop (Menu trái + Header trên)
            ├── components/
            │   ├── StatCard.tsx        # Thẻ hiển thị chỉ số KPI (Doanh thu, Đơn hàng, % tăng)
            │   ├── RevenueChart.tsx    # Biểu đồ miền Doanh thu & Lợi nhuận (Ngày/Tháng/Năm)
            │   ├── DataTable.tsx       # Bảng dữ liệu chuẩn có phân trang, cuộn ngang
            │   ├── FilterBar.tsx       # Thanh công cụ Tìm kiếm và Lọc đầu mỗi trang
            │   └── ImageUploader.tsx   # Khung nhập link ảnh bìa sách và xem trước ảnh
            ├── pages/
            │   ├── Dashboard.tsx       # Trang 1: Tổng quan KPI, Biểu đồ, Top sách, Cảnh báo kho
            │   ├── Books.tsx           # Trang 2: Quản lý Sách (Thêm/Sửa/Xóa, Ẩn/Hiện, Giá bán)
            │   ├── Categories.tsx      # Trang 3: Quản lý Danh mục Cha/Con, Tác giả & NXB
            │   ├── Orders.tsx          # Trang 4: Duyệt đơn hàng & Duyệt khiếu nại Đổi trả
            │   ├── Inventory.tsx       # Trang 5: Quản lý Tồn kho (< 5 cuốn) & Lập phiếu nhập kho
            │   ├── CustomersStaff.tsx  # Trang 6: Quản lý Khách hàng (Khóa TK) & Phân quyền Nhân sự
            │   ├── Vouchers.tsx        # Trang 7: Tạo mã Voucher, Ẩn bình luận xấu, Phát thông báo
            │   └── ReportsSettings.tsx # Trang 8: Xuất báo cáo Excel CSV & Cấu hình cửa hàng
            └── services/
                ├── adminBookService.ts   # Nghiệp vụ thêm/sửa/xóa Sách, Danh mục, Tác giả, NXB
                ├── adminOrderService.ts  # Nghiệp vụ chuyển trạng thái đơn và xử lý đổi trả
                ├── adminStatsService.ts  # Nghiệp vụ thống kê biểu đồ và xuất file CSV/Excel
                └── adminSystemService.ts # Nghiệp vụ Nhập kho, Phân quyền, Voucher, Cấu hình
```

---

## 2. Các file cấu hình & tài liệu ở Thư mục Gốc

| Tên file | Nhiệm vụ cụ thể |
| :--- | :--- |
| `package.json` | Khai báo **NPM Workspaces** liên kết `shared`, `frontend/client-mobile` và `frontend/admin-web`. |
| `tsconfig.base.json` | Cấu hình chuẩn kiểm tra kiểu dữ liệu TypeScript và định nghĩa đường dẫn tắt `@shared/*` trỏ vào `shared/`. |
| `README.md` | Hướng dẫn cài đặt, khởi chạy Mobile App, Web Admin và đóng gói file `.apk` cho Android. |
| `PHAN_TICH_NGHIEP_VU_VA_CSDL.md` | Phân tích chi tiết 6 phân hệ nghiệp vụ, công thức tính toán và thiết kế CSDL. |
| `PHAN_QUYEN_WEB_ADMIN.md` | Quy định rõ 3 vai trò nhân sự (`ADMIN`, `WAREHOUSE_MANAGER`, `CSKH`) được phép đăng nhập Web Admin (chặn `CUSTOMER`), kèm ma trận quyền được dùng / không được dùng trên từng trang. |
| `cau_truc_backend_gradle.md` | Sơ đồ cây thư mục chuẩn của Backend Spring Boot + Gradle. |
| `giai_thich_cau_truc_backend.md` | Giải thích chi tiết vai trò của từng file và từng tầng trong Backend Spring Boot + Gradle. |

---

## 3. Phân hệ 1: Cơ sở dữ liệu PostgreSQL 16 + `pgvector` (`database/`)

Chạy trên Docker container `bookstore-postgres` (`pgvector/pgvector:pg16`, cổng `5432`, database `bookstore_db`). Toàn bộ tên bảng và cột đều đặt bằng **tiếng Việt không dấu** (`snake_case`) để dễ đọc, dễ hiểu:

| Đường dẫn file | Nhiệm vụ & Nội dung bên trong |
| :--- | :--- |
| `database/01_schema.sql` | **Định nghĩa cấu trúc (DDL)**:<br>• Kích hoạt `CREATE EXTENSION IF NOT EXISTS vector;` (`pgvector 0.8.7`).<br>• Khởi tạo **17 bảng đang hoạt động**: `tk_nguoi_dung`, `so_dia_chi`, `danh_muc`, `tac_gia`, `nha_xuat_ban`, `sach` (có cột `vector_ngu_nghia vector(768)`), `sach_yeu_thich`, `phieu_nhap_kho`, `chi_tiet_phieu_nhap`, `gio_hang`, `ma_giam_gia`, `don_hang`, `chi_tiet_don_hang`, `lich_su_don_hang`, `danh_gia`, `yeu_cau_doi_tra`, `thong_bao` (bảng `cau_hinh_cua_hang` đang được tạm comment theo yêu cầu).<br>• Tạo 6 Index tối ưu tìm kiếm theo danh mục, giá bán, tồn kho, đơn hàng và đánh giá. |
| `database/02_seed_data.sql` | **Nạp dữ liệu mẫu phong phú (DML)**:<br>• **186 cuốn sách** thuộc **12 danh mục** (mỗi danh mục từ 13–18 cuốn), ảnh bìa (`anh_bia_url`) lấy từ Open Library khớp đúng theo từng tên sách.<br>• **148 tác giả**, **8 nhà xuất bản**.<br>• **19 tài khoản** trong `tk_nguoi_dung`: 3 `ADMIN`, 3 `WAREHOUSE_MANAGER`, 3 `CSKH`, 10 `CUSTOMER` (tên người thật).<br>• Đầy đủ dữ liệu mẫu cho sổ địa chỉ (10), mã giảm giá (6), phiếu nhập kho (2) + chi tiết (5), đơn hàng (8) + chi tiết (8) + lịch sử đơn (8), yêu thích (10), giỏ hàng (4), đánh giá (5), đổi trả (1), thông báo (3). |

---

## 4. Phân hệ 2: Backend Spring Boot + Gradle (`backend/`)

Dự án chuẩn **Spring Boot + Gradle (Java 21)** khởi tạo theo cấu trúc IntelliJ IDEA / Spring Initializr, đặt tại `backend/src/main/java/com/trithucviet/bookstore/`:

| Package / Thư mục | Nhiệm vụ cụ thể |
| :--- | :--- |
| `config/` | Cấu hình CORS (cho phép cổng `8081` và `5173`), `SecurityConfig` (`@EnableMethodSecurity`), bộ lọc JWT (`JwtAuthenticationFilter`, `JwtTokenProvider`) và cấu hình `ChatClient` + `EmbeddingModel` cho tư vấn viên. |
| `entity/` & `entity/enums/` | Chứa các Enum (`VaiTro`, `TrangThaiDon`, `LoaiGiam`...) và 17 class `@Entity` ánh xạ trực tiếp vào 17 bảng tiếng Việt không dấu trong `bookstore_db`. |
| `repository/` | Các interface kế thừa `JpaRepository` để truy vấn PostgreSQL và thực hiện tìm kiếm tương đồng ngữ nghĩa bằng toán tử cosine `<=>` trên cột `sach.vector_ngu_nghia`. |
| `dto/request/` & `dto/response/` | Tách biệt dữ liệu nhận vào và trả ra khỏi Entity (ẩn `mat_khau_ma_hoa`, ẩn mảng `vector(768)` và tránh vòng lặp JSON), đồng bộ cấu trúc với `shared/types` của Frontend. |
| `service/` | Xử lý toàn bộ quy tắc nghiệp vụ và `@Transactional`: đăng nhập (chặn `CUSTOMER` vào Web Admin), đặt hàng (trừ `ton_kho`, tăng lượt dùng voucher), hủy đơn (hoàn kho), nhập kho (cộng dồn `ton_kho`), đánh giá, đổi trả và thống kê báo cáo. |
| `ai/` | • `BookVectorService`: Ghép `Tên sách + Tác giả + Danh mục + NXB + Tóm tắt + Từ khóa` thành vector 768 chiều lưu vào `sach.vector_ngu_nghia`.<br>• `BookstoreAiTools`: Cung cấp các hàm (Function Calling) để trợ lý tư vấn tìm sách, kiểm tra mã giảm giá, lấy địa chỉ mặc định, thêm vào giỏ (`gio_hang`) và tạo đơn hàng (`don_hang`) thông qua tầng `service/`.<br>• `AdvisorChatService`: Điều phối hội thoại và trả câu trả lời kèm danh sách sách gợi ý. |
| `controller/client/` | Các cổng REST API `/api/v1/**` phục vụ riêng cho Mobile App khách hàng (Auth, Sách, Giỏ hàng, Yêu thích, Địa chỉ, Voucher, Đơn hàng, Đánh giá/Đổi trả, Thông báo, Chat tư vấn). |
| `controller/admin/` | Các cổng REST API `/api/admin/**` phục vụ Web Admin, gắn `@PreAuthorize` phân quyền chặt chẽ cho 3 vai trò `ADMIN`, `WAREHOUSE_MANAGER`, `CSKH` theo đúng `PHAN_QUYEN_WEB_ADMIN.md`. |
| `exception/` | `BusinessException` và `GlobalExceptionHandler` (`@RestControllerAdvice`) bắt lỗi tập trung và trả về thông báo tiếng Việt thống nhất. |

---

## 5. Phân hệ 3: Thư mục Dùng chung Frontend (`shared/`)

Giúp Mobile App và Web Admin dùng chung định nghĩa kiểu dữ liệu TypeScript:

| Đường dẫn file | Nhiệm vụ & Chức năng bên trong |
| :--- | :--- |
| `shared/types/index.ts` | Định nghĩa toàn bộ cấu trúc dữ liệu (Interfaces) cho Sách, Danh mục, Tác giả, Nhà xuất bản, Đơn hàng, Voucher, Phiếu nhập kho, Yêu cầu đổi trả... |
| `shared/api-client/mockData.ts` | Dữ liệu mẫu tiếng Việt dùng khi chạy thử nghiệm độc lập trên Frontend. |
| `shared/api-client/index.ts` | Bộ xử lý gọi API dùng chung cho Mobile App và Web Admin. |
| `shared/index.ts` | File cửa ngõ gom toàn bộ `types` và `api-client` để bên Mobile và Web import qua `@shared`. |

---

## 6. Phân hệ 4: Mobile App Khách hàng (`frontend/client-mobile/`)

> **Quy tắc cứng:** Chỉ dành riêng cho Khách hàng (`CUSTOMER`) mua sách. Mọi chức năng quản trị đều không xuất hiện ở đây.

### 6.1. Các màn hình giao diện (`app/`)

| Đường dẫn file | Màn hình | Chức năng chi tiết trên màn hình |
| :--- | :--- | :--- |
| `app/_layout.tsx` | **Khung gốc (Root)** | Bọc các Provider (`AuthProvider`, `CartProvider`, `AiChatProvider`) quanh toàn bộ ứng dụng và gắn nút tròn **"Tư vấn"** nổi ở góc dưới bên phải mọi màn hình. |
| `app/(tabs)/_layout.tsx` | **Thanh Tab đáy** | Tạo thanh điều hướng 4 Tab dưới đáy: **Trang chủ**, **Danh mục**, **Đơn hàng**, **Tài khoản**. Hiển thị chấm đỏ số lượng sách đang có trong giỏ. |
| `app/(tabs)/index.tsx` | **Tab 1: Trang chủ** | • **Thanh tìm kiếm trực tiếp (`TextInput`)**: Gõ tới đâu lọc sách ngay tại Trang chủ tới đó.<br>• **Banner Carousel**: Trượt ngang các chương trình khuyến mãi.<br>• **Lối tắt danh mục**, **Khung giờ Flash Sale** đếm ngược, kệ **Sách Bán Chạy** & **Sách Mới Tuyển Chọn**. |
| `app/(tabs)/catalog.tsx` | **Tab 2: Danh mục** | • Tìm kiếm theo tên sách, tác giả, mã ISBN.<br>• Lọc kết hợp theo **Thể loại**, **Khoảng giá**, **Đánh giá từ 4 sao**.<br>• Sắp xếp theo: Phổ biến nhất, Mới nhất, Giá tăng/giảm dần. |
| `app/(tabs)/orders.tsx` | **Tab 3: Đơn hàng** | • Lọc đơn theo từng tab trạng thái (*Chờ xác nhận, Đang đóng gói, Đang giao, Đã giao, Đã hủy*).<br>• Xem **Timeline hành trình đơn hàng**.<br>• Nút **Hủy đơn hàng** (chỉ mở khi đơn còn ở trạng thái `PENDING`).<br>• Nút **Đánh giá 5 sao** và **Yêu cầu Đổi/Trả hàng** (khi đơn `DELIVERED`). |
| `app/(tabs)/profile.tsx` | **Tab 4: Tài khoản** | • **Khi chưa đăng nhập**: Hiển thị form Đăng nhập, Đăng ký tài khoản mới, Quên mật khẩu.<br>• **Khi đã đăng nhập**: Quản lý Sổ địa chỉ nhận hàng, Tủ sách yêu thích, Kho Voucher, Thông báo, Câu hỏi thường gặp (FAQ) và Đăng xuất. |
| `app/book/[id].tsx` | **Chi tiết Sách** | • Xem ảnh bìa lớn, giá bán, phần trăm giảm, tình trạng tồn kho.<br>• Bảng thông số xuất bản (Mã ISBN, NXB, Năm XB, Số trang).<br>• Xem tóm tắt nội dung, danh sách bình luận, kệ Sách cùng thể loại, thanh đáy **Thêm vào giỏ / Mua ngay**. |
| `app/cart.tsx` | **Giỏ hàng** | • Tăng/giảm số lượng từng cuốn (chặn không cho vượt quá tồn kho).<br>• Chọn hoặc nhập mã giảm giá Voucher.<br>• Tự động tính Tạm tính, Phí vận chuyển, Giảm giá và Tổng thanh toán. |
| `app/checkout.tsx` | **Thanh toán** | • Chọn địa chỉ giao hàng từ Sổ địa chỉ.<br>• Chọn phương thức thanh toán (*COD*, *BANK_QR*, *EWALLET*).<br>• Xác nhận đặt hàng và hiển thị màn hình Đặt hàng thành công kèm mã đơn. |

### 6.2. Các thành phần xử lý bên dưới (`src/`)

| Đường dẫn file | Nhiệm vụ cụ thể |
| :--- | :--- |
| `src/theme/colors.ts` | Quy định bảng màu thương hiệu và nhãn màu tiếng Việt cho từng trạng thái đơn hàng. |
| `src/components/common/BookCard.tsx` | Thẻ hiển thị 1 cuốn sách dùng chung ở mọi danh sách. |
| `src/components/common/BannerCarousel.tsx` | Thanh trượt ngang các Banner chiến dịch khuyến mãi trên Trang chủ. |
| `src/components/common/StateViews.tsx` | Chứa 3 giao diện trạng thái: Đang tải (`SkeletonCard`), Trống (`EmptyState`) và Lỗi mạng (`ErrorState`). |
| `src/features/ai-chatbot/FloatingAiChatbot.tsx` | Nút tròn **"Tư vấn"** nổi ở góc màn hình mở hộp thoại chat tư vấn sách, tra cứu mã giảm giá và hỗ trợ đặt sách. |
| `src/hooks/` (`useAuth`, `useCart`, `useAiChat`) | Quản lý trạng thái đăng nhập, giỏ hàng và lịch sử trò chuyện tư vấn. |
| `src/services/` (`authService`, `bookService`, `orderService`, `aiService`) | Kết nối API tương ứng với các cổng `/api/v1/**` của Backend. |

---

## 7. Phân hệ 5: Web Quản trị Admin (`frontend/admin-web/`)

> **Quy tắc cứng:** Dành cho 3 nhóm nhân sự (`ADMIN`, `WAREHOUSE_MANAGER`, `CSKH`) theo ma trận tại `PHAN_QUYEN_WEB_ADMIN.md`. Tài khoản `CUSTOMER` bị chặn hoàn toàn.

| Đường dẫn file | Tên trang / Thành phần | Chức năng quản trị chi tiết |
| :--- | :--- | :--- |
| `src/layouts/AdminLayout.tsx` & `src/routes/AppRoutes.tsx` | **Khung bố cục & Định tuyến** | Menu dọc bên trái (Sidebar), Header phía trên và định tuyến cho 8 trang quản trị. |
| `src/pages/Dashboard.tsx` | **1. Tổng quan (Dashboard)** | 4 thẻ KPI, Biểu đồ Doanh thu & Lợi nhuận, Top sách bán chạy và Cảnh báo sách sắp hết hàng (`ton_kho <= 5`). |
| `src/pages/Books.tsx` | **2. Quản lý Sách** | Tìm kiếm, lọc, Thêm/Sửa thông tin sách, bật/tắt hiển thị, chỉnh giá & Flash Sale (chặn xóa sách đã có trong đơn hàng). |
| `src/pages/Categories.tsx` | **3. Danh mục, Tác giả & NXB** | Quản lý cây danh mục Cha–Con (`danh_muc`), Tác giả (`tac_gia`) và Nhà xuất bản (`nha_xuat_ban`). |
| `src/pages/Orders.tsx` | **4. Đơn hàng & Đổi trả** | Xem/lọc đơn, chuyển trạng thái một chiều (`PENDING` → `PREPARING` → `SHIPPING` → `DELIVERED`) và duyệt/từ chối yêu cầu đổi trả (`yeu_cau_doi_tra`). |
| `src/pages/Inventory.tsx` | **5. Quản lý Kho hàng** | Lọc sách sắp hết hàng và lập Phiếu nhập kho mới (`phieu_nhap_kho`, `chi_tiet_phieu_nhap`) tự động cộng dồn `ton_kho`. |
| `src/pages/CustomersStaff.tsx` | **6. Khách hàng & Nhân sự** | Xem thông tin khách hàng, khóa/mở tài khoản vi phạm; tạo tài khoản nhân sự và phân quyền (`ADMIN`, `WAREHOUSE_MANAGER`, `CSKH`). |
| `src/pages/Vouchers.tsx` | **7. Khuyến mãi & Đánh giá** | Quản lý mã giảm giá (`ma_giam_gia`), ẩn/hiện bình luận đánh giá (`danh_gia`) và phát thông báo (`thong_bao`). |
| `src/pages/ReportsSettings.tsx` | **8. Báo cáo & Cấu hình** | Xuất báo cáo CSV (có UTF-8 BOM mở bằng Excel) và quản lý cấu hình hệ thống. |

---

## 8. Luồng chạy dữ liệu (Data Flow) khi thuyết trình

Khi giảng viên hỏi **1 thao tác bấm nút trên màn hình (hoặc khách nhắn tư vấn đặt sách) chạy qua những tầng nào**, bạn trình bày theo sơ đồ 5 bước sau:

```text
[Bước 1: Giao diện Người dùng (Frontend)]
  • Mobile App (Khách hàng): frontend/client-mobile/app/...
  • Web Admin (Nhân sự)    : frontend/admin-web/src/pages/...
        │
        ▼ (Gọi HTTP Request kèm JWT Token)
[Bước 2: Bảo mật & Cổng API (Backend Controller)]
  • config/JwtAuthenticationFilter + SecurityConfig (Kiểm tra Token & Vai trò)
  • controller/client/* (/api/v1/**) hoặc controller/admin/* (/api/admin/**)
        │
        ▼ (Truyền Request DTO đã kiểm tra hợp lệ)
[Bước 3: Xử lý Nghiệp vụ & Tư vấn AI (Service / AI Module)]
  • service/* (OrderService, InventoryService, BookService... có @Transactional)
  • ai/* (AdvisorChatService + BookstoreAiTools + BookVectorService)
        │
        ▼ (Gọi hàm truy vấn qua Spring Data JPA / Native Vector Query)
[Bước 4: Giao tiếp Dữ liệu (Repository & Entity)]
  • repository/* (SachRepository, DonHangRepository...)
  • entity/* (Ánh xạ 17 bảng tiếng Việt không dấu)
        │
        ▼ (Thực thi SQL & Toán tử tương đồng Cosine <=>)
[Bước 5: Cơ sở dữ liệu PostgreSQL 16 + pgvector (Docker)]
  • Container: bookstore-postgres -> Database: bookstore_db
  • Khởi tạo từ: database/01_schema.sql & database/02_seed_data.sql (186 cuốn sách)
```
