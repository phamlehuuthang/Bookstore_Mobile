# HỆ SINH THÁI THƯƠNG MẠI ĐIỆN TỬ BOOKSTORE ENTERPRISE

Hệ sinh thái Thương mại điện tử **Tri Thức Việt BookStore** được thiết kế theo kiến trúc **Monorepo Doanh nghiệp**, phân tách độc lập 100% giữa **Client Mobile App (Khách hàng)**, **Admin Web Dashboard (Quản trị viên)**, **Shared Core** và **Backend API**.

---

## 1. Cấu trúc Thư mục Dự án (Monorepo Taxonomy)

```text
BookStore_Antigravity/
├── frontend/
│   ├── client-mobile/           # [1] Ứng dụng Di động Khách hàng (React Native + Expo Router)
│   └── admin-web/               # [2] Hệ thống Quản trị Web Desktop (React 18 + Vite + TailwindCSS + Recharts)
├── backend/                     # [3] Máy chủ REST API & Xử lý nghiệp vụ (Controllers, Services, Database)
└── shared/                      # [4] Data Models, DTOs, Enums & Stateful Mock API Client dùng chung
```

---

## 2. Hướng dẫn Khởi chạy Nhanh

### Khởi chạy Web Admin Dashboard (Desktop `http://localhost:5173`)
```bash
cd frontend/admin-web
npm run dev
```

### Khởi chạy Client Mobile App (Expo Go / Android Emulator / iOS Simulator)
```bash
cd frontend/client-mobile
npx expo start
```

### Kiểm tra Kiểu dữ liệu (Zero-Error Typecheck & Production Build)
```bash
# Kiểm tra TypeScript cho Mobile App
cd frontend/client-mobile && npx tsc --noEmit

# Kiểm tra TypeScript & Build Production cho Admin Web
cd frontend/admin-web && npm run build
```

---

## 3. Hướng dẫn Xuất file APK Android Độc lập (`client-mobile`)

Ứng dụng `frontend/client-mobile` đã được cấu hình sẵn `app.json` (Android package `vn.bookstore.trithucviet.client`) và `eas.json` (profile `preview-apk`).

### Cách 1: Xuất APK thông qua EAS Build Cloud (Khuyên dùng)
```bash
cd frontend/client-mobile
npm install -g eas-cli
eas login
eas build -p android --profile preview-apk
```
Sau khi hoàn tất, hệ thống trả về đường link tải trực tiếp file `.apk` để cài đặt lên mọi thiết bị Android.

### Cách 2: Xuất APK Local bằng Gradle (Android Studio)
```bash
cd frontend/client-mobile
npx expo prebuild --platform android
cd android
./gradlew assembleRelease
```
File APK xuất ra nằm tại: `frontend/client-mobile/android/app/build/outputs/apk/release/app-release.apk`.

