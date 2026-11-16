# GitHub Copilot Instructions — Superpowers & BookStore Project

## 1. Superpowers Workflow Skills (`.github/skills/`)

Trước khi trả lời hoặc viết code cho bất kỳ tác vụ nào, hãy kiểm tra và tuân thủ các skill trong thư mục `.github/skills/` (bắt đầu từ `.github/skills/using-superpowers/SKILL.md`):

- **Brainstorming & Thiết kế** (`.github/skills/brainstorming/SKILL.md`): Dùng trước khi tạo tính năng mới, thêm API, hoặc thay đổi hành vi hệ thống.
- **Lập kế hoạch chi tiết** (`.github/skills/writing-plans/SKILL.md` & `.github/skills/executing-plans/SKILL.md`): Dùng cho tác vụ nhiều bước (ví dụ: dựng tầng Entity -> Repository -> Service -> Controller).
- **Test-Driven Development (TDD)** (`.github/skills/test-driven-development/SKILL.md`): Viết kiểm thử tự động trước/song song khi triển khai tính năng hoặc sửa lỗi.
- **Gỡ lỗi có hệ thống** (`.github/skills/systematic-debugging/SKILL.md`): Dùng khi gặp lỗi build, lỗi test, lỗi Hibernate/SQL — tìm nguyên nhân gốc rễ trước khi sửa.
- **Xác minh trước khi hoàn tất** (`.github/skills/verification-before-completion/SKILL.md`): Chạy lệnh kiểm tra thực tế (`./gradlew test`, truy vấn DB) trước khi kết luận hoàn thành.

## 2. Quy ước Dự án BookStore (`BookStore_Antigravity`)

- **Cơ sở dữ liệu (`database/`)**: PostgreSQL 16 + `pgvector` (`bookstore_db` trên cổng `5432`), gồm 17 bảng đặt tên bằng **tiếng Việt không dấu** (`tk_nguoi_dung`, `so_dia_chi`, `danh_muc`, `tac_gia`, `nha_xuat_ban`, `sach`, `sach_yeu_thich`, `phieu_nhap_kho`, `chi_tiet_phieu_nhap`, `gio_hang`, `ma_giam_gia`, `don_hang`, `chi_tiet_don_hang`, `lich_su_don_hang`, `danh_gia`, `yeu_cau_doi_tra`, `thong_bao`).
- **Backend (`backend/`)**: Java 21 + Spring Boot + Gradle (`com.trithucviet.bookstore`). Sử dụng `spring.jpa.hibernate.ddl-auto=validate` để giữ nguyên dữ liệu mẫu trong `bookstore_db`.
- **Phân quyền Web Admin**: Tuân thủ ma trận tại `PHAN_QUYEN_WEB_ADMIN.md` cho 3 vai trò `ADMIN`, `WAREHOUSE_MANAGER`, `CSKH` (chặn `CUSTOMER`).
