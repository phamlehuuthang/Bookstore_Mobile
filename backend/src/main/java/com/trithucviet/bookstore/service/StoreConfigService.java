package com.trithucviet.bookstore.service;

import com.trithucviet.bookstore.dto.response.StoreConfigDto;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dịch vụ quản lý Cấu hình Cửa hàng lưu trực tiếp vào bảng PostgreSQL `cau_hinh_cua_hang` (Phần B1).
 * - Tự động đảm bảo bảng `cau_hinh_cua_hang` và dòng mặc định `cfg_01` tồn tại mà không làm ảnh hưởng `ddl-auto=validate`.
 */
@Service
public class StoreConfigService {

    private static final String DEFAULT_ID = "cfg_01";

    private final JdbcTemplate jdbcTemplate;
    private volatile boolean initialized = false;

    public StoreConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void ensureTableAndDefaultRow() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS cau_hinh_cua_hang (
                        id VARCHAR(50) PRIMARY KEY,
                        ten_cua_hang VARCHAR(150) NOT NULL,
                        khau_hieu VARCHAR(255),
                        logo_url VARCHAR(500),
                        hotline VARCHAR(30) NOT NULL,
                        email_ho_tro VARCHAR(120) NOT NULL,
                        dia_chi_tru_so VARCHAR(255) NOT NULL,
                        phi_ship_mac_dinh DECIMAL(12, 0) NOT NULL DEFAULT 25000,
                        mien_phi_ship_tu DECIMAL(12, 0) NOT NULL DEFAULT 300000,
                        nguong_canh_bao_ton_kho INT NOT NULL DEFAULT 5,
                        ngan_hang_ten VARCHAR(100) NOT NULL,
                        ngan_hang_so_tk VARCHAR(50) NOT NULL,
                        ngan_hang_chu_tk VARCHAR(150) NOT NULL
                    )
                    """);

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM cau_hinh_cua_hang WHERE id = ?",
                    Integer.class,
                    DEFAULT_ID
            );
            if (count == null || count == 0) {
                jdbcTemplate.update("""
                        INSERT INTO cau_hinh_cua_hang (
                            id, ten_cua_hang, khau_hieu, logo_url, hotline, email_ho_tro, dia_chi_tru_so,
                            phi_ship_mac_dinh, mien_phi_ship_tu, nguong_canh_bao_ton_kho,
                            ngan_hang_ten, ngan_hang_so_tk, ngan_hang_chu_tk
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        DEFAULT_ID,
                        "Nhà Sách Tri Thức Việt",
                        "Đọc để bứt phá - Khơi nguồn tri thức",
                        "https://covers.openlibrary.org/b/id/10527843-M.jpg",
                        "1900 6886",
                        "hotro@trithucviet.vn",
                        "Số 12 Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
                        BigDecimal.valueOf(25_000L),
                        BigDecimal.valueOf(300_000L),
                        5,
                        "VietinBank",
                        "109603022005",
                        "PHAM LE HUU THANG"
                );
            }
            initialized = true;
        }
    }

    @Transactional
    public StoreConfigDto getStoreConfig() {
        ensureTableAndDefaultRow();
        List<StoreConfigDto> rows = jdbcTemplate.query(
                "SELECT id, ten_cua_hang, khau_hieu, logo_url, hotline, email_ho_tro, dia_chi_tru_so, "
                        + "phi_ship_mac_dinh, mien_phi_ship_tu, nguong_canh_bao_ton_kho, "
                        + "ngan_hang_ten, ngan_hang_so_tk, ngan_hang_chu_tk FROM cau_hinh_cua_hang WHERE id = ?",
                (rs, rowNum) -> new StoreConfigDto(
                        rs.getString("id"),
                        rs.getString("ten_cua_hang"),
                        rs.getString("khau_hieu"),
                        rs.getString("logo_url"),
                        rs.getString("hotline"),
                        rs.getString("email_ho_tro"),
                        rs.getString("dia_chi_tru_so"),
                        rs.getBigDecimal("phi_ship_mac_dinh"),
                        rs.getBigDecimal("mien_phi_ship_tu"),
                        rs.getInt("nguong_canh_bao_ton_kho"),
                        rs.getString("ngan_hang_ten"),
                        rs.getString("ngan_hang_so_tk"),
                        rs.getString("ngan_hang_chu_tk")
                ),
                DEFAULT_ID
        );
        return rows.get(0);
    }

    @Transactional
    public StoreConfigDto updateStoreConfig(StoreConfigDto request) {
        ensureTableAndDefaultRow();
        jdbcTemplate.update("""
                UPDATE cau_hinh_cua_hang
                SET ten_cua_hang = ?,
                    khau_hieu = ?,
                    logo_url = ?,
                    hotline = ?,
                    email_ho_tro = ?,
                    dia_chi_tru_so = ?,
                    phi_ship_mac_dinh = ?,
                    mien_phi_ship_tu = ?,
                    nguong_canh_bao_ton_kho = ?,
                    ngan_hang_ten = ?,
                    ngan_hang_so_tk = ?,
                    ngan_hang_chu_tk = ?
                WHERE id = ?
                """,
                request.tenCuaHang().trim(),
                request.khauHieu() == null ? "" : request.khauHieu().trim(),
                request.logoUrl() == null ? "" : request.logoUrl().trim(),
                request.hotline().trim(),
                request.emailHoTro().trim(),
                request.diaChiTruSo().trim(),
                request.phiShipMacDinh(),
                request.mienPhiShipTu(),
                request.nguongCanhBaoTonKho(),
                request.nganHangTen().trim(),
                request.nganHangSoTk().trim(),
                request.nganHangChuTk().trim(),
                DEFAULT_ID
        );
        return getStoreConfig();
    }
}
