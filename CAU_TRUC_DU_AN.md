# 📁 TÀI LIỆU CẤU TRÚC DỰ ÁN & NHIỆM VỤ TỪNG FILE
**Dự án:** Hệ sinh thái Thương mại Điện tử Nhà sách Tri Thức Việt (`BookStore_Antigravity`)

> **Mục đích tài liệu:** Giúp người đọc, thành viên nhóm hoặc giảng viên chấm bài nắm bắt nhanh vị trí từng thư mục, từng file mã nguồn và nhiệm vụ cụ thể của chúng trong dự án.

---

## 📌 MỤC LỤC
1. [Sơ đồ Cây thư mục Tổng thể (Monorepo)](#1-sơ-đồ-cây-thư-mục-tổng-thể-monorepo)
2. [Các file cấu hình ở Thư mục Gốc](#2-các-file-cấu-hình-ở-thư-mục-gốc)
3. [Phân hệ 1: Thư mục Dùng chung (`shared/`)](#3-phân-hệ-1-thư-mục-dùng-chung-shared)
4. [Phân hệ 2: Mobile App Khách hàng (`frontend/client-mobile/`)](#4-phân-hệ-2-mobile-app-khách-hàng-frontendclient-mobile)
5. [Phân hệ 3: Web Quản trị Admin (`frontend/admin-web/`)](#5-phân-hệ-3-web-quản-trị-admin-frontendadmin-web)
6. [Phân hệ 4: Khung Máy chủ (`backend/`)](#6-phân-hệ-4-khung-máy-chủ-backend)
7. [Luồng chạy dữ liệu (Data Flow) khi thuyết trình](#7-luồng-chạy-dữ-liệu-data-flow-khi-thuyết-trình)

---

## 1. Sơ đồ Cây thư mục Tổng thể (Monorepo)

Dự án được chia thành các phân hệ tách biệt rõ ràng giữa **App Điện thoại cho Khách hàng** và **Web Quản trị cho Admin**, cùng sử dụng chung một bộ kiểu dữ liệu tại `shared/`:

```text
BookStore_Antigravity/
├── package.json                        # Quản lý chung toàn bộ dự án (NPM Workspaces)
├── tsconfig.base.json                  # Cấu hình TypeScript chuẩn dùng chung
├── README.md                           # Hướng dẫn cài đặt, chạy dự án và xuất file APK
├── PHAN_TICH_NGHIEP_VU_VA_CSDL.md      # Tài liệu phân tích 18 bảng CSDL & quy tắc nghiệp vụ
├── CAU_TRUC_DU_AN.md                   # Tài liệu giải thích nhiệm vụ từng file (File này)
│
├── shared/                             # [PHÂN HỆ 1] DỮ LIỆU & KIỂU DỮ LIỆU DÙNG CHUNG
│   ├── index.ts                        # Điểm xuất (export) tất cả kiểu dữ liệu & API
│   ├── types/
│   │   └── index.ts                    # Khai báo cấu trúc Sách, Đơn hàng, User, Voucher...
│   └── api-client/
│       ├── mockData.ts                 # Kho dữ liệu mẫu tiếng Việt (12 cuốn sách, đơn hàng...)
│       └── index.ts                    # Bộ xử lý nghiệp vụ & API giả lập cho cả App và Web
│
├── frontend/
│   ├── client-mobile/                  # [PHÂN HỆ 2] MOBILE APP DÀNH CHO KHÁCH HÀNG (Expo)
│   │   ├── app.json                    # Cấu hình tên App, icon, package Android xuất APK
│   │   ├── eas.json                    # Cấu hình đóng gói file cài đặt .apk Android
│   │   ├── metro.config.js             # Cấu hình kết nối thư mục shared/ cho React Native
│   │   ├── app/                        # Chứa các màn hình điều hướng (Expo Router)
│   │   │   ├── _layout.tsx             # Khung gốc bọc toàn App + gắn nút "Tư vấn" nổi
│   │   │   ├── cart.tsx                # Màn hình Giỏ hàng & chọn mã giảm giá
│   │   │   ├── checkout.tsx            # Màn hình Thanh toán & Đặt hàng thành công
│   │   │   ├── book/
│   │   │   │   └── [id].tsx            # Màn hình Chi tiết 1 cuốn sách (theo ID)
│   │   │   └── (tabs)/                 # 4 màn hình chính trên thanh Tab dưới đáy
│   │   │       ├── _layout.tsx         # Cấu hình thanh TabBar (Trang chủ, Danh mục, Đơn, Tôi)
│   │   │       ├── index.tsx           # Tab 1: Trang chủ, Tìm kiếm trực tiếp, Flash Sale
│   │   │       ├── catalog.tsx         # Tab 2: Danh mục & Bộ lọc sách đa tiêu chí
│   │   │       ├── orders.tsx          # Tab 3: Theo dõi đơn, Hủy đơn, Đánh giá & Đổi trả
│   │   │       └── profile.tsx         # Tab 4: Đăng nhập/Đăng ký, Sổ địa chỉ, Voucher, FAQ
│   │   └── src/                        # Các thành phần xử lý bên dưới giao diện Mobile
│   │       ├── theme/
│   │       │   └── colors.ts           # Bảng màu thương hiệu và màu nhãn trạng thái đơn
│   │       ├── components/common/
│   │       │   ├── BannerCarousel.tsx  # Thanh trượt Banner khuyến mãi ở Trang chủ
│   │       │   ├── BookCard.tsx        # Thẻ hiển thị 1 cuốn sách (Ảnh, Giá, Sao, Nút thêm giỏ)
│   │       │   └── StateViews.tsx      # Giao diện lúc Đang tải (Skeleton), Rỗng, Lỗi mạng
│   │       ├── features/ai-chatbot/
│   │       │   └── FloatingAiChatbot.tsx # Nút tròn "Tư vấn" nổi & Cửa sổ chat hỗ trợ
│   │       ├── hooks/
│   │       │   ├── useAuth.tsx         # Quản lý trạng thái Đăng nhập, Sổ địa chỉ, Yêu thích
│   │       │   ├── useCart.tsx         # Quản lý Giỏ hàng, tính phí ship và giảm giá Voucher
│   │       │   └── useAiChat.tsx       # Quản lý lịch sử tin nhắn trong hộp thoại Tư vấn
│   │       └── services/
│   │           ├── authService.ts      # Gọi API Đăng nhập (chặn Admin), Đăng ký, Địa chỉ
│   │           ├── bookService.ts      # Gọi API lấy danh sách sách, chi tiết, gửi đánh giá
│   │           ├── orderService.ts     # Gọi API tạo đơn hàng, hủy đơn, gửi yêu cầu đổi trả
│   │           └── aiService.ts        # Xử lý câu hỏi gợi ý nhanh trong hộp thoại Tư vấn
│   │
│   └── admin-web/                      # [PHÂN HỆ 3] WEB QUẢN TRỊ DÀNH CHO ADMIN (React + Vite)
│       ├── index.html                  # Trang HTML gốc của Web Admin
│       ├── vite.config.ts              # Cấu hình chạy máy chủ Web ở cổng 5173
│       ├── tailwind.config.js          # Cấu hình màu sắc giao diện TailwindCSS
│       └── src/
│           ├── main.tsx                # Điểm khởi chạy đầu tiên gắn React vào index.html
│           ├── App.tsx                 # Bọc bộ định tuyến trình duyệt (BrowserRouter)
│           ├── routes/
│           │   └── AppRoutes.tsx       # Định nghĩa đường dẫn URL cho 8 trang quản trị
│           ├── layouts/
│           │   └── AdminLayout.tsx     # Khung giao diện Desktop (Menu trái + Header trên)
│           ├── components/
│           │   ├── StatCard.tsx        # Thẻ hiển thị chỉ số KPI (Doanh thu, Đơn hàng, % tăng)
│           │   ├── RevenueChart.tsx    # Biểu đồ miền Doanh thu & Lợi nhuận (Ngày/Tháng/Năm)
│           │   ├── DataTable.tsx       # Bảng dữ liệu chuẩn có phân trang, cuộn ngang
│           │   ├── FilterBar.tsx       # Thanh công cụ Tìm kiếm và Lọc đầu mỗi trang
│           │   └── ImageUploader.tsx   # Khung nhập link ảnh bìa sách và xem trước ảnh
│           ├── pages/
│           │   ├── Dashboard.tsx       # Trang 1: Tổng quan KPI, Biểu đồ, Top sách, Cảnh báo kho
│           │   ├── Books.tsx           # Trang 2: Quản lý Sách (Thêm/Sửa/Xóa, Ẩn/Hiện, Giá bán)
│           │   ├── Categories.tsx      # Trang 3: Quản lý Danh mục Cha/Con, Tác giả & NXB
│           │   ├── Orders.tsx          # Trang 4: Duyệt đơn hàng & Duyệt khiếu nại Đổi trả
│           │   ├── Inventory.tsx       # Trang 5: Quản lý Tồn kho (< 5 cuốn) & Lập phiếu nhập kho
│           │   ├── CustomersStaff.tsx  # Trang 6: Quản lý Khách hàng (Khóa TK) & Phân quyền Nhân sự
│           │   ├── Vouchers.tsx        # Trang 7: Tạo mã Voucher, Ẩn bình luận xấu, Phát thông báo
│           │   └── ReportsSettings.tsx # Trang 8: Xuất báo cáo Excel CSV & Cấu hình cửa hàng
│           └── services/
│               ├── adminBookService.ts   # Nghiệp vụ thêm/sửa/xóa Sách, Danh mục, Tác giả, NXB
│               ├── adminOrderService.ts  # Nghiệp vụ chuyển trạng thái đơn và xử lý đổi trả
│               ├── adminStatsService.ts  # Nghiệp vụ thống kê biểu đồ và xuất file CSV/Excel
│               └── adminSystemService.ts # Nghiệp vụ Nhập kho, Phân quyền, Voucher, Cấu hình
│
└── backend/                            # [PHÂN HỆ 4] KHUNG MÁY CHỦ DỰ PHÒNG
    └── src/
        └── server.ts                   # Khung API mẫu để sau này kết nối CSDL thật (SQL/MySQL)
```

---

## 2. Các file cấu hình ở Thư mục Gốc

| Tên file | Nhiệm vụ cụ thể |
| :--- | :--- |
| `package.json` | Khai báo **NPM Workspaces** liên kết `shared`, `frontend/client-mobile`, `frontend/admin-web` và `backend`. Chứa các lệnh chạy nhanh toàn dự án. |
| `tsconfig.base.json` | Cấu hình chuẩn kiểm tra lỗi kiểu dữ liệu TypeScript và định nghĩa đường dẫn tắt `@shared/*` trỏ thẳng vào thư mục `shared/`. |
| `README.md` | Hướng dẫn các câu lệnh cài đặt (`npm install`), khởi chạy Mobile App, khởi chạy Web Admin và đóng gói file `.apk` cho Android. |
| `PHAN_TICH_NGHIEP_VU_VA_CSDL.md` | Tài liệu phân tích chi tiết 6 phân hệ nghiệp vụ và thiết kế 18 bảng Cơ sở dữ liệu (Primary Key, Foreign Key, Ràng buộc). |

---

## 3. Phân hệ 1: Thư mục Dùng chung (`shared/`)

Đây là **"trái tim dữ liệu"** giúp Mobile App và Web Admin nói cùng một ngôn ngữ dữ liệu, không bị lệch tên trường:

| Đường dẫn file | Nhiệm vụ & Chức năng bên trong |
| :--- | :--- |
| `shared/types/index.ts` | Định nghĩa toàn bộ cấu trúc dữ liệu (Interfaces):<br>• **Phân quyền (`Role`)**: `CUSTOMER`, `ADMIN`, `STAFF_WAREHOUSE`, `STAFF_SALES`, `STAFF_SUPPORT`.<br>• **Trạng thái đơn (`OrderStatus`)**: `PENDING` (Chờ xác nhận), `PACKING` (Đang đóng gói), `SHIPPING` (Đang giao), `COMPLETED` (Đã giao), `CANCELLED` (Đã hủy), `RETURN_REQUESTED` (Yêu cầu đổi trả).<br>• **Đối tượng (`Entities`)**: `Book`, `Category`, `Author`, `Publisher`, `Order`, `Voucher`, `StockImportReceipt`, `ReturnRequest`, `StoreSettings`... |
| `shared/api-client/mockData.ts` | Kho dữ liệu mẫu tiếng Việt chuẩn thực tế:<br>• **12 đầu sách nổi tiếng** (*Nhà Giả Kim, Đắc Nhân Tâm, Clean Code, Mắt Biếc, Tư Duy Nhanh Và Chậm...*) kèm giá gốc, giá bìa, tồn kho, ISBN, số trang.<br>• **6 danh mục**, **6 tác giả**, **4 nhà xuất bản**, **5 đơn hàng mẫu** ở đủ các trạng thái, danh sách mã giảm giá (`TRITHUC30`, `FREESHIP`, `WELCOME50`), phiếu nhập kho, yêu cầu đổi trả. |
| `shared/api-client/index.ts` | Đóng vai trò **Bộ xử lý API trung tâm (`bookstoreApi`)** chứa toàn bộ quy tắc nghiệp vụ:<br>• Kiểm tra đăng nhập & **chặn tài khoản Admin đăng nhập trên Mobile App**.<br>• Tìm kiếm, lọc sách theo danh mục/giá/sao, sắp xếp sách.<br>• Tính toán mã giảm giá Voucher và tự động trừ kho khi đặt hàng, hoàn kho khi hủy đơn.<br>• Cung cấp các hàm thêm/sửa/xóa và xuất báo cáo CSV cho Web Admin. |
| `shared/index.ts` | File cửa ngõ gom toàn bộ `types` và `api-client` để bên Mobile và Web chỉ cần gọi `import { ... } from '@shared'`. |

---

## 4. Phân hệ 2: Mobile App Khách hàng (`frontend/client-mobile/`)

> **Quy tắc cứng:** Chỉ dành riêng cho Khách hàng (`CUSTOMER`) mua sách. Mọi chức năng quản trị đều không xuất hiện ở đây.

### 4.1. Các màn hình giao diện (`app/`)

| Đường dẫn file | Màn hình | Chức năng chi tiết trên màn hình |
| :--- | :--- | :--- |
| `app/_layout.tsx` | **Khung gốc (Root)** | Bọc các Provider (`AuthProvider`, `CartProvider`, `AiChatProvider`) quanh toàn bộ ứng dụng và gắn nút tròn **"Tư vấn"** nổi ở góc dưới bên phải mọi màn hình. |
| `app/(tabs)/_layout.tsx` | **Thanh Tab đáy** | Tạo thanh điều hướng 4 Tab dưới đáy: **Trang chủ**, **Danh mục**, **Đơn hàng**, **Tài khoản**. Hiển thị chấm đỏ số lượng sách đang có trong giỏ. |
| `app/(tabs)/index.tsx` | **Tab 1: Trang chủ** | • **Thanh tìm kiếm trực tiếp (`TextInput`)**: Gõ tới đâu lọc sách ngay tại Trang chủ tới đó.<br>• **Banner Carousel**: Trượt ngang các chương trình khuyến mãi.<br>• **6 lối tắt danh mục**, **Khung giờ Flash Sale** đếm ngược, kệ **Sách Bán Chạy** & **Sách Mới Tuyển Chọn**. |
| `app/(tabs)/catalog.tsx` | **Tab 2: Danh mục** | • Tìm kiếm theo tên sách, tác giả, mã ISBN.<br>• Lọc kết hợp theo **Thể loại**, **Khoảng giá** (*Dưới 100k, 100k–200k, Trên 200k*), **Đánh giá từ 4 sao**.<br>• Sắp xếp theo: Phổ biến nhất, Mới nhất, Giá tăng/giảm dần. |
| `app/(tabs)/orders.tsx` | **Tab 3: Đơn hàng** | • Lọc đơn theo từng tab trạng thái (*Chờ xác nhận, Đang đóng gói, Đang giao, Đã giao, Đã hủy*).<br>• Xem **Timeline hành trình đơn hàng**.<br>• Nút **Hủy đơn hàng** (chỉ mở khi đơn còn ở trạng thái `Chờ xác nhận`).<br>• Nút **Đánh giá 5 sao** và **Yêu cầu Đổi/Trả hàng** (khi đơn `Đã giao`). |
| `app/(tabs)/profile.tsx` | **Tab 4: Tài khoản** | • **Khi chưa đăng nhập**: Hiển thị form Đăng nhập, Đăng ký tài khoản mới, Quên mật khẩu.<br>• **Khi đã đăng nhập**: Quản lý Sổ địa chỉ nhận hàng, Tủ sách yêu thích, Kho Voucher, Thông báo, Câu hỏi thường gặp (FAQ) và Đăng xuất. |
| `app/book/[id].tsx` | **Chi tiết Sách** | • Xem ảnh bìa lớn, giá bán, phần trăm giảm, tình trạng tồn kho.<br>• Bảng thông số xuất bản (Mã ISBN, NXB, Năm XB, Số trang, Trọng lượng).<br>• Nút **"Xem tóm tắt nhanh nội dung cuốn sách này"**.<br>• Danh sách bình luận của người mua trước, kệ Sách cùng thể loại, thanh đáy **Thêm vào giỏ / Mua ngay**. |
| `app/cart.tsx` | **Giỏ hàng** | • Tăng/giảm số lượng từng cuốn (chặn không cho vượt quá tồn kho).<br>• Chọn hoặc nhập mã giảm giá Voucher (`TRITHUC30`, `FREESHIP`...).<br>• Tự động tính Tạm tính, Phí vận chuyển (Miễn phí cho đơn từ `250.000đ`), Giảm giá và Tổng thanh toán. |
| `app/checkout.tsx` | **Thanh toán** | • Chọn địa chỉ giao hàng từ Sổ địa chỉ.<br>• Chọn phương thức vận chuyển (*Tiêu chuẩn* hoặc *Hỏa tốc 2h*).<br>• Chọn phương thức thanh toán (*COD*, *VNPay*, *Ví MoMo*, *Chuyển khoản VietQR*).<br>• Xác nhận đặt hàng và hiển thị màn hình Đặt hàng thành công kèm mã vận đơn. |

### 4.2. Các thành phần xử lý bên dưới (`src/`)

| Đường dẫn file | Nhiệm vụ cụ thể |
| :--- | :--- |
| `src/theme/colors.ts` | Quy định bảng màu Xanh Cổ Vịt (`#0F766E`), Cam đất (`#EA580C`), nền kem sáng (`#F8FAFC`) và bảng nhãn màu tiếng Việt cho từng trạng thái đơn hàng. |
| `src/components/common/BookCard.tsx` | Thẻ hiển thị 1 cuốn sách dùng chung ở mọi danh sách (hiển thị ảnh bìa, nhãn `-28%`, tên sách, tác giả, số sao, giá tiền và nút cộng vào giỏ). |
| `src/components/common/BannerCarousel.tsx` | Thanh trượt ngang các Banner chiến dịch khuyến mãi trên Trang chủ. |
| `src/components/common/StateViews.tsx` | Chứa 3 giao diện trạng thái: Khung xương xám lúc đang tải (`SkeletonCard`), Màn hình báo trống (`EmptyState`) và Màn hình báo lỗi mạng (`ErrorState`). |
| `src/features/ai-chatbot/FloatingAiChatbot.tsx` | Nút tròn **"Tư vấn"** nổi ở góc màn hình. Khi bấm vào sẽ mở hộp thoại chat với **Tư vấn viên Tri Thức Việt**, có các nút gợi ý hỏi nhanh và thẻ sách bấm vào xem chi tiết được ngay. |
| `src/hooks/useAuth.tsx` | Lưu giữ thông tin người dùng đang đăng nhập, danh sách sách yêu thích và sổ địa chỉ xuyên suốt toàn App. |
| `src/hooks/useCart.tsx` | Lưu giữ danh sách sách trong giỏ hàng, tính tổng tiền, phí ship và số tiền được giảm từ mã Voucher theo thời gian thực. |
| `src/hooks/useAiChat.tsx` | Lưu lịch sử cuộc trò chuyện giữa khách hàng và Tư vấn viên hỗ trợ. |
| `src/services/authService.ts` | Kết nối API Đăng nhập, Đăng ký, Quên mật khẩu, Thêm/Xóa địa chỉ nhận hàng. |
| `src/services/bookService.ts` | Kết nối API lấy dữ liệu Trang chủ, tìm kiếm/lọc sách, xem chi tiết sách và gửi đánh giá sao. |
| `src/services/orderService.ts` | Kết nối API Đặt hàng, lấy lịch sử đơn hàng, Hủy đơn và gửi yêu cầu Đổi/Trả hàng. |
| `src/services/aiService.ts` | Cung cấp danh sách câu hỏi gợi ý thường gặp và xử lý trả lời tư vấn sách, chính sách vận chuyển, mã giảm giá. |

---

## 5. Phân hệ 3: Web Quản trị Admin (`frontend/admin-web/`)

> **Quy tắc cứng:** Thiết kế chuyên biệt cho màn hình máy tính (Desktop), có Sidebar bên trái và bảng dữ liệu lớn để Quản trị viên và Nhân viên vận hành toàn bộ nhà sách.

### 5.1. Khung bố cục & Thành phần dùng chung (`src/layouts/`, `src/components/`)

| Đường dẫn file | Nhiệm vụ cụ thể |
| :--- | :--- |
| `src/routes/AppRoutes.tsx` | Định nghĩa đường dẫn URL cho 8 trang chức năng quản trị (`/`, `/books`, `/categories`, `/orders`, `/inventory`, `/customers-staff`, `/vouchers`, `/reports-settings`). |
| `src/layouts/AdminLayout.tsx` | Khung giao diện chính gồm **Menu dọc bên trái (Sidebar)** để chuyển giữa 8 phân hệ và **Thanh Header phía trên** hiển thị thông tin Admin đang trực. |
| `src/components/StatCard.tsx` | Thẻ hiển thị con số thống kê KPI lớn (Tổng doanh thu, Lợi nhuận ròng, Tổng số đơn...) kèm mũi tên % tăng trưởng. |
| `src/components/RevenueChart.tsx` | Biểu đồ miền (AreaChart) vẽ trực quan đường Doanh thu và Lợi nhuận theo Ngày, Tháng hoặc Năm. |
| `src/components/DataTable.tsx` | Bảng hiển thị dữ liệu chuẩn có chia trang (Pagination), thanh cuộn ngang và thông báo khi không tìm thấy dữ liệu. |
| `src/components/FilterBar.tsx` | Thanh công cụ đầu mỗi trang gồm ô Tìm kiếm từ khóa, các bộ lọc Dropdown và nút hành động chính. |
| `src/components/ImageUploader.tsx` | Khung nhập đường dẫn ảnh bìa sách kèm khung xem trước (Preview) ảnh bìa trực tiếp trong hộp thoại Thêm/Sửa sách. |

### 5.2. 8 Trang Quản trị Nghiệp vụ (`src/pages/`)

| Đường dẫn file | Tên trang trên Menu | Chức năng quản trị chi tiết |
| :--- | :--- | :--- |
| `src/pages/Dashboard.tsx` | **1. Tổng quan (Dashboard)** | • Hiển thị **4 thẻ KPI**: Doanh thu, Lợi nhuận ròng, Tổng đơn hàng, Khách hàng mới.<br>• **Biểu đồ Doanh thu & Lợi nhuận** chuyển đổi theo Ngày / Tháng / Năm.<br>• Bảng **Top sách bán chạy nhất** và danh sách **Cảnh báo sách sắp hết hàng (`tồn kho < 5`)**. |
| `src/pages/Books.tsx` | **2. Quản lý Sách** | • Tìm kiếm sách theo Tên, Tác giả, ISBN; lọc theo Danh mục.<br>• Mở hộp thoại **Thêm sách mới** hoặc **Chỉnh sửa thông tin sách** (Giá nhập, Giá bìa, Giá bán, Số trang, Trọng lượng, Mô tả...).<br>• Bật/tắt nút **Đang bán / Tạm ẩn** và **Xóa sách** (tự động chặn xóa nếu sách đã từng phát sinh đơn hàng). |
| `src/pages/Categories.tsx` | **3. Danh mục & Tác giả** | Chia làm 3 tab quản lý:<br>• **Danh mục sách**: Tạo danh mục gốc và danh mục con phân cấp.<br>• **Tác giả**: Thêm/sửa tiểu sử tác giả và thống kê số đầu sách.<br>• **Nhà xuất bản**: Quản lý thông tin liên hệ các Nhà xuất bản & Công ty phát hành. |
| `src/pages/Orders.tsx` | **4. Đơn hàng & Đổi trả** | • Xem danh sách đơn hàng, lọc theo trạng thái và xem chi tiết từng món trong đơn.<br>• **Chuyển trạng thái đơn hàng một chiều**: `Chờ xác nhận` → `Đang đóng gói` → `Đang giao hàng` → `Đã giao thành công`.<br>• Tab **Yêu cầu Đổi/Trả**: Xem lý do và bằng chứng khách gửi để bấm **Chấp thuận** hoặc **Từ chối**. |
| `src/pages/Inventory.tsx` | **5. Quản lý Kho hàng** | • Theo dõi tồn kho thực tế và lọc riêng các cuốn **Sắp hết hàng (`< 5 cuốn`)**.<br>• **Lập Phiếu Nhập Kho mới**: Chọn Nhà cung cấp, chọn từng đầu sách, số lượng nhập và giá nhập gốc -> Hệ thống tự động **cộng dồn tồn kho** và lưu lịch sử phiếu nhập. |
| `src/pages/CustomersStaff.tsx` | **6. Khách hàng & Nhân sự** | • **Tab Khách hàng**: Xem tổng chi tiêu, số đơn đã mua và nút **Khóa / Mở khóa tài khoản** khách vi phạm.<br>• **Tab Nhân viên & Phân quyền**: Tạo tài khoản nhân viên mới và gán quyền (`ADMIN`, `Nhân viên Kho`, `Nhân viên Bán hàng`, `CSKH`), xem Ma trận phân quyền từng vai trò. |
| `src/pages/Vouchers.tsx` | **7. Khuyến mãi & Đánh giá** | • **Quản lý Voucher**: Tạo mã giảm giá theo `%` hoặc số tiền cố định, giới hạn đơn tối thiểu và số lượt dùng.<br>• **Kiểm duyệt Đánh giá**: Xem bình luận của khách và bấm **Ẩn đánh giá** nếu spam/vi phạm.<br>• **Gửi Thông báo**: Phát thông báo khuyến mãi mới tới toàn bộ Mobile App. |
| `src/pages/ReportsSettings.tsx` | **8. Báo cáo & Cấu hình** | • **Xuất báo cáo Excel (CSV)**: Tải về máy tính 3 file báo cáo (*Doanh thu & Lợi nhuận*, *Tồn kho*, *Danh sách Đơn hàng*) có hỗ trợ tiếng Việt có dấu.<br>• **Cấu hình Cửa hàng**: Chỉnh sửa phí vận chuyển mặc định, ngưỡng miễn phí vận chuyển, thông tin ngân hàng nhận tiền VietQR và Banner trang chủ. |

### 5.3. Tầng Dịch vụ của Web Admin (`src/services/`)

| Đường dẫn file | Nhiệm vụ cụ thể |
| :--- | :--- |
| `src/services/adminBookService.ts` | Gọi API lấy danh sách và thực hiện Thêm / Sửa / Xóa / Ẩn-Hiện cho Sách, Danh mục, Tác giả và Nhà xuất bản. |
| `src/services/adminOrderService.ts` | Gọi API lấy danh sách đơn hàng, cập nhật trạng thái đơn hàng và duyệt yêu cầu đổi trả. |
| `src/services/adminStatsService.ts` | Gọi API lấy số liệu thống kê Dashboard và tạo file `.csv` (kèm mã UTF-8 BOM để mở bằng Microsoft Excel không bị lỗi font tiếng Việt). |
| `src/services/adminSystemService.ts` | Gọi API tạo phiếu nhập kho, khóa/mở khóa người dùng, tạo nhân viên, tạo mã Voucher, ẩn bình luận và lưu cấu hình cửa hàng. |

---

## 6. Phân hệ 4: Khung Máy chủ (`backend/`)

| Đường dẫn file | Nhiệm vụ cụ thể |
| :--- | :--- |
| `backend/src/server.ts` | Đóng vai trò là **khung sườn (blueprint) máy chủ REST API** minh họa các đường dẫn (`/api/auth`, `/api/books`, `/api/orders`, `/api/admin/...`). Hiện tại Mobile App và Web Admin đang chạy trực tiếp với bộ xử lý tại `shared/api-client/` nên bạn có thể giữ nguyên để báo cáo kiến trúc hoặc phát triển tiếp kết nối SQL Server / MySQL tại đây. |

---

## 7. Luồng chạy dữ liệu (Data Flow) khi thuyết trình

Khi giảng viên hoặc người đọc muốn hiểu **1 thao tác bấm nút trên màn hình chạy qua những file nào**, bạn chỉ cần nhìn theo sơ đồ 4 bước sau:

```text
[Bước 1: Giao diện Màn hình]
  • Mobile: frontend/client-mobile/app/...
  • Admin : frontend/admin-web/src/pages/...
        │
        ▼ (Người dùng bấm nút: Tìm sách, Đặt hàng, Nhập kho, Duyệt đơn...)
[Bước 2: Hooks & State (Quản lý trạng thái)]
  • frontend/client-mobile/src/hooks/ (useAuth, useCart, useAiChat)
        │
        ▼ (Gọi hàm xử lý tương ứng)
[Bước 3: Tầng Dịch vụ (Services)]
  • Mobile: frontend/client-mobile/src/services/ (bookService, orderService...)
  • Admin : frontend/admin-web/src/services/ (adminBookService, adminOrderService...)
        │
        ▼ (Gửi yêu cầu xử lý dữ liệu)
[Bước 4: Lõi Nghiệp vụ & Dữ liệu Dùng chung (Shared Core)]
  • shared/api-client/index.ts  (Kiểm tra điều kiện, tính tiền, trừ kho, đổi trạng thái)
  • shared/api-client/mockData.ts & shared/types/index.ts
```
