-- ============================================================================
-- HỆ SINH THÁI THƯƠNG MẠI ĐIỆN TỬ NHÀ SÁCH TRI THỨC VIỆT
-- File 1: Khởi tạo Extension pgvector & Toàn bộ 18 Bảng CSDL (Tiếng Việt không dấu)
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS vector;

-- Dọn dẹp các bảng cũ (cả tên tiếng Anh trước đó và tên tiếng Việt mới) nếu chạy lại
DROP TABLE IF EXISTS return_requests, reviews, order_timelines, order_items, orders, cart_items, wishlists, inventory_receipt_items, inventory_receipts, books, categories, authors, publishers, vouchers, addresses, notifications, users, store_configs CASCADE;

DROP TABLE IF EXISTS hanh_vi_nguoi_dung, yeu_cau_doi_tra, danh_gia, lich_su_don_hang, chi_tiet_don_hang, don_hang, gio_hang, sach_yeu_thich, chi_tiet_phieu_nhap, phieu_nhap_kho, sach, danh_muc, tac_gia, nha_xuat_ban, ma_giam_gia, so_dia_chi, thong_bao, nguoi_dung, tk_nguoi_dung, cau_hinh_cua_hang CASCADE;

-- ============================================================================
-- PHÂN HỆ 1: TÀI KHOẢN, PHÂN QUYỀN & SỔ ĐỊA CHỈ
-- ============================================================================

CREATE TABLE tk_nguoi_dung (
    id VARCHAR(50) PRIMARY KEY,
    ho_ten VARCHAR(120) NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    mat_khau_ma_hoa VARCHAR(255) NOT NULL,
    so_dien_thoai VARCHAR(20) NOT NULL,
    vai_tro VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER'
        CHECK (vai_tro IN ('CUSTOMER', 'ADMIN', 'WAREHOUSE_MANAGER', 'CSKH')),
    bi_khoa BOOLEAN NOT NULL DEFAULT FALSE,
    tong_chi_tieu DECIMAL(15, 0) NOT NULL DEFAULT 0,
    so_don_hang INT NOT NULL DEFAULT 0,
    ngay_tao TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE so_dia_chi (
    id VARCHAR(50) PRIMARY KEY,
    nguoi_dung_id VARCHAR(50) NOT NULL REFERENCES tk_nguoi_dung(id) ON DELETE CASCADE,
    ho_ten_nguoi_nhan VARCHAR(120) NOT NULL,
    so_dien_thoai VARCHAR(20) NOT NULL,
    tinh_thanh VARCHAR(100) NOT NULL,
    quan_huyen VARCHAR(100) NOT NULL,
    phuong_xa VARCHAR(100) NOT NULL,
    dia_chi_chi_tiet VARCHAR(255) NOT NULL,
    mac_dinh BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================================
-- PHÂN HỆ 2: DANH MỤC (CHA-CON), TÁC GIẢ, NHÀ XUẤT BẢN & KHO SÁCH (CÓ PGVECTOR)
-- ============================================================================

CREATE TABLE danh_muc (
    id VARCHAR(50) PRIMARY KEY,
    ten_danh_muc VARCHAR(120) NOT NULL,
    duong_dan VARCHAR(150) UNIQUE NOT NULL,
    danh_muc_cha_id VARCHAR(50) NULL REFERENCES danh_muc(id) ON DELETE SET NULL,
    mo_ta VARCHAR(500),
    ten_bieu_tuong VARCHAR(50),
    thu_tu_hien_thi INT NOT NULL DEFAULT 1,
    dang_hoat_dong BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE tac_gia (
    id VARCHAR(50) PRIMARY KEY,
    ten_tac_gia VARCHAR(150) NOT NULL,
    tieu_su VARCHAR(500)
);

CREATE TABLE nha_xuat_ban (
    id VARCHAR(50) PRIMARY KEY,
    ten_nxb VARCHAR(150) NOT NULL,
    dia_chi VARCHAR(255)
);

CREATE TABLE sach (
    id VARCHAR(50) PRIMARY KEY,
    ma_isbn VARCHAR(30) UNIQUE NOT NULL,
    tieu_de VARCHAR(255) NOT NULL,
    duong_dan VARCHAR(255) UNIQUE NOT NULL,
    danh_muc_id VARCHAR(50) NOT NULL REFERENCES danh_muc(id),
    ten_danh_muc_con VARCHAR(120),
    tac_gia_id VARCHAR(50) NOT NULL REFERENCES tac_gia(id),
    nha_xuat_ban_id VARCHAR(50) NOT NULL REFERENCES nha_xuat_ban(id),
    gia_ban DECIMAL(12, 0) NOT NULL CHECK (gia_ban >= 0),
    gia_goc DECIMAL(12, 0) NOT NULL CHECK (gia_goc >= 0),
    dang_flash_sale BOOLEAN NOT NULL DEFAULT FALSE,
    gia_flash_sale DECIMAL(12, 0),
    ton_kho INT NOT NULL DEFAULT 0 CHECK (ton_kho >= 0),
    da_ban INT NOT NULL DEFAULT 0,
    diem_danh_gia_tb DECIMAL(3, 2) NOT NULL DEFAULT 5.00,
    so_luot_danh_gia INT NOT NULL DEFAULT 0,
    so_trang INT,
    nam_xuat_ban INT,
    loai_bia VARCHAR(30) NOT NULL DEFAULT 'Bìa mềm',
    anh_bia_url VARCHAR(500) NOT NULL,
    anh_chi_tiet_urls TEXT,
    tom_tat_ngan VARCHAR(500),
    mo_ta_chi_tiet TEXT,
    tu_khoa VARCHAR(500),
    vector_ngu_nghia vector(768),
    ban_chay BOOLEAN NOT NULL DEFAULT FALSE,
    hien_thi BOOLEAN NOT NULL DEFAULT TRUE,
    ngay_tao TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE sach_yeu_thich (
    nguoi_dung_id VARCHAR(50) NOT NULL REFERENCES tk_nguoi_dung(id) ON DELETE CASCADE,
    sach_id VARCHAR(50) NOT NULL REFERENCES sach(id) ON DELETE CASCADE,
    ngay_tao TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (nguoi_dung_id, sach_id)
);

CREATE TABLE hanh_vi_nguoi_dung (
    id VARCHAR(50) PRIMARY KEY,
    nguoi_dung_id VARCHAR(50) NOT NULL REFERENCES tk_nguoi_dung(id) ON DELETE CASCADE,
    sach_id VARCHAR(50) NULL REFERENCES sach(id) ON DELETE CASCADE,
    danh_muc_id VARCHAR(50) NOT NULL REFERENCES danh_muc(id) ON DELETE CASCADE,
    loai_hanh_vi VARCHAR(30) NOT NULL
        CHECK (loai_hanh_vi IN ('XEM_CHI_TIET', 'TIM_KIEM', 'THEM_YEU_THICH', 'THEM_GIO_HANG', 'MUA_HANG', 'BO_QUA_DE_XUAT')),
    thoi_gian_xem_giay INT NOT NULL DEFAULT 0 CHECK (thoi_gian_xem_giay >= 0),
    trong_so_diem INT NOT NULL DEFAULT 1,
    ngay_tao TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- PHÂN HỆ 3: QUẢN LÝ NHẬP KHO & TỒN KHO
-- ============================================================================

CREATE TABLE phieu_nhap_kho (
    id VARCHAR(50) PRIMARY KEY,
    ma_phieu VARCHAR(30) UNIQUE NOT NULL,
    ten_nha_cung_cap VARCHAR(200) NOT NULL,
    nguoi_tao VARCHAR(100) NOT NULL,
    ghi_chu VARCHAR(500),
    tong_so_luong INT NOT NULL DEFAULT 0,
    tong_tien_nhap DECIMAL(15, 0) NOT NULL DEFAULT 0,
    ngay_tao TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE chi_tiet_phieu_nhap (
    id VARCHAR(50) PRIMARY KEY,
    phieu_nhap_id VARCHAR(50) NOT NULL REFERENCES phieu_nhap_kho(id) ON DELETE CASCADE,
    sach_id VARCHAR(50) NOT NULL REFERENCES sach(id),
    so_luong INT NOT NULL CHECK (so_luong > 0),
    gia_nhap DECIMAL(12, 0) NOT NULL CHECK (gia_nhap >= 0)
);

-- ============================================================================
-- PHÂN HỆ 4: GIỎ HÀNG, MÃ GIẢM GIÁ (VOUCHER) & ĐẶT HÀNG
-- ============================================================================

CREATE TABLE gio_hang (
    nguoi_dung_id VARCHAR(50) NOT NULL REFERENCES tk_nguoi_dung(id) ON DELETE CASCADE,
    sach_id VARCHAR(50) NOT NULL REFERENCES sach(id) ON DELETE CASCADE,
    so_luong INT NOT NULL CHECK (so_luong > 0),
    da_chon BOOLEAN NOT NULL DEFAULT TRUE,
    ngay_cap_nhat TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (nguoi_dung_id, sach_id)
);

CREATE TABLE ma_giam_gia (
    id VARCHAR(50) PRIMARY KEY,
    ma_code VARCHAR(30) UNIQUE NOT NULL,
    tieu_de VARCHAR(150) NOT NULL,
    mo_ta VARCHAR(500),
    loai_giam VARCHAR(20) NOT NULL CHECK (loai_giam IN ('CO_DINH', 'PHAN_TRAM')),
    gia_tri_giam DECIMAL(12, 0) NOT NULL,
    giam_toi_da DECIMAL(12, 0),
    don_toi_thieu DECIMAL(12, 0) NOT NULL DEFAULT 0,
    gioi_han_luot INT NOT NULL,
    da_su_dung INT NOT NULL DEFAULT 0,
    ngay_bat_dau DATE NOT NULL,
    ngay_ket_thuc DATE NOT NULL,
    dang_hoat_dong BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE don_hang (
    id VARCHAR(50) PRIMARY KEY,
    ma_don_hang VARCHAR(30) UNIQUE NOT NULL,
    nguoi_dung_id VARCHAR(50) NOT NULL REFERENCES tk_nguoi_dung(id),
    ten_khach_hang VARCHAR(120) NOT NULL,
    sdt_khach_hang VARCHAR(20) NOT NULL,
    dia_chi_giao_hang VARCHAR(500) NOT NULL,
    tam_tinh DECIMAL(15, 0) NOT NULL,
    phi_van_chuyen DECIMAL(12, 0) NOT NULL,
    tien_giam_gia DECIMAL(12, 0) NOT NULL DEFAULT 0,
    ma_giam_gia_code VARCHAR(30) REFERENCES ma_giam_gia(ma_code),
    tong_thanh_toan DECIMAL(15, 0) NOT NULL,
    phuong_thuc_thanh_toan VARCHAR(20) NOT NULL CHECK (phuong_thuc_thanh_toan IN ('TIEN_MAT_COD', 'CHUYEN_KHOAN_QR', 'VI_DIEN_TU')),
    trang_thai_thanh_toan VARCHAR(20) NOT NULL DEFAULT 'CHUA_THANH_TOAN' CHECK (trang_thai_thanh_toan IN ('CHUA_THANH_TOAN', 'DA_THANH_TOAN', 'DA_HOAN_TIEN')),
    trang_thai_don VARCHAR(20) NOT NULL DEFAULT 'CHO_XAC_NHAN'
        CHECK (trang_thai_don IN ('CHO_XAC_NHAN', 'DANG_DONG_GOI', 'DANG_GIAO', 'DA_GIAO', 'DA_HUY')),
    ghi_chu VARCHAR(500),
    da_danh_gia BOOLEAN NOT NULL DEFAULT FALSE,
    ngay_tao TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE chi_tiet_don_hang (
    id VARCHAR(50) PRIMARY KEY,
    don_hang_id VARCHAR(50) NOT NULL REFERENCES don_hang(id) ON DELETE CASCADE,
    sach_id VARCHAR(50) NOT NULL REFERENCES sach(id),
    tieu_de_sach VARCHAR(255) NOT NULL,
    anh_bia_url VARCHAR(500) NOT NULL,
    ten_tac_gia VARCHAR(150) NOT NULL,
    don_gia DECIMAL(12, 0) NOT NULL,
    so_luong INT NOT NULL CHECK (so_luong > 0)
);

CREATE TABLE lich_su_don_hang (
    id VARCHAR(50) PRIMARY KEY,
    don_hang_id VARCHAR(50) NOT NULL REFERENCES don_hang(id) ON DELETE CASCADE,
    trang_thai VARCHAR(20) NOT NULL,
    tieu_de VARCHAR(150) NOT NULL,
    mo_ta VARCHAR(500),
    thoi_gian VARCHAR(50) NOT NULL
);

-- ============================================================================
-- PHÂN HỆ 5: HẬU MÃI - ĐÁNH GIÁ SÁCH & ĐỔI TRẢ HÀNG
-- ============================================================================

CREATE TABLE danh_gia (
    id VARCHAR(50) PRIMARY KEY,
    sach_id VARCHAR(50) NOT NULL REFERENCES sach(id) ON DELETE CASCADE,
    nguoi_dung_id VARCHAR(50) NOT NULL REFERENCES tk_nguoi_dung(id) ON DELETE CASCADE,
    don_hang_id VARCHAR(50) REFERENCES don_hang(id) ON DELETE SET NULL,
    so_sao INT NOT NULL CHECK (so_sao BETWEEN 1 AND 5),
    binh_luan VARCHAR(1000) NOT NULL,
    anh_danh_gia_urls TEXT,
    bi_an BOOLEAN NOT NULL DEFAULT FALSE,
    ngay_tao TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE yeu_cau_doi_tra (
    id VARCHAR(50) PRIMARY KEY,
    don_hang_id VARCHAR(50) UNIQUE NOT NULL REFERENCES don_hang(id) ON DELETE CASCADE,
    ly_do VARCHAR(255) NOT NULL,
    mo_ta VARCHAR(1000) NOT NULL,
    anh_minh_chung_urls TEXT NOT NULL,
    trang_thai VARCHAR(20) NOT NULL DEFAULT 'CHO_DUYET' CHECK (trang_thai IN ('CHO_DUYET', 'DA_CHAP_THUAN', 'TU_CHOI')),
    ghi_chu_admin VARCHAR(500),
    ngay_tao TIMESTAMP NOT NULL DEFAULT NOW(),
    ngay_xu_ly TIMESTAMP NULL
);

-- ============================================================================
-- PHÂN HỆ 6: THÔNG BÁO & CẤU HÌNH CỬA HÀNG
-- ============================================================================

CREATE TABLE thong_bao (
    id VARCHAR(50) PRIMARY KEY,
    nguoi_dung_id VARCHAR(50) NULL REFERENCES tk_nguoi_dung(id) ON DELETE CASCADE,
    tieu_de VARCHAR(200) NOT NULL,
    noi_dung VARCHAR(1000) NOT NULL,
    loai_thong_bao VARCHAR(20) NOT NULL CHECK (loai_thong_bao IN ('DON_HANG', 'KHUYEN_MAI', 'HE_THONG')),
    da_doc BOOLEAN NOT NULL DEFAULT FALSE,
    ngay_tao TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Tạm ẩn bảng cau_hinh_cua_hang theo yêu cầu để chỉnh sửa thông tin sau
-- CREATE TABLE cau_hinh_cua_hang (
--     id VARCHAR(50) PRIMARY KEY,
--     ten_cua_hang VARCHAR(150) NOT NULL,
--     khau_hieu VARCHAR(255),
--     logo_url VARCHAR(500),
--     hotline VARCHAR(30) NOT NULL,
--     email_ho_tro VARCHAR(120) NOT NULL,
--     dia_chi_tru_so VARCHAR(255) NOT NULL,
--     phi_ship_mac_dinh DECIMAL(12, 0) NOT NULL DEFAULT 25000,
--     mien_phi_ship_tu DECIMAL(12, 0) NOT NULL DEFAULT 300000,
--     nguong_canh_bao_ton_kho INT NOT NULL DEFAULT 5,
--     ngan_hang_ten VARCHAR(100) NOT NULL,
--     ngan_hang_so_tk VARCHAR(50) NOT NULL,
--     ngan_hang_chu_tk VARCHAR(150) NOT NULL
-- );

-- Chỉ mục tối ưu tìm kiếm
CREATE INDEX idx_sach_danh_muc ON sach(danh_muc_id);
CREATE INDEX idx_sach_gia_ban ON sach(gia_ban);
CREATE INDEX idx_sach_ton_kho ON sach(ton_kho);
CREATE INDEX idx_don_hang_nguoi_dung ON don_hang(nguoi_dung_id);
CREATE INDEX idx_don_hang_trang_thai ON don_hang(trang_thai_don);
CREATE INDEX idx_danh_gia_sach ON danh_gia(sach_id);
CREATE INDEX idx_hanh_vi_nguoi_dung ON hanh_vi_nguoi_dung(nguoi_dung_id, ngay_tao DESC);
CREATE INDEX idx_hanh_vi_danh_muc ON hanh_vi_nguoi_dung(nguoi_dung_id, danh_muc_id);
