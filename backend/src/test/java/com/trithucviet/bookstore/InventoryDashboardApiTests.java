package com.trithucviet.bookstore;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trithucviet.bookstore.config.JwtTokenProvider;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import com.trithucviet.bookstore.repository.SachRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Kiểm thử tích hợp toàn bộ API Bước 8:
 * 1. Quản lý Nhập kho (GET/POST /api/admin/inventory/receipts) & Cảnh báo tồn kho <= 5 (/low-stock)
 * 2. Thống kê Dashboard theo phân quyền RBAC (GET /api/admin/dashboard)
 * 3. Xuất Báo cáo Doanh thu CSV (GET /api/admin/reports/revenue-export)
 */
@SpringBootTest
@Transactional
class InventoryDashboardApiTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SachRepository sachRepository;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private String adminToken;
    private String warehouseToken;
    private String cskhToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        adminToken = bearer("nd_admin", "admin@trithucviet.vn", VaiTro.ADMIN);
        warehouseToken = bearer("nd_kho", "kho@trithucviet.vn", VaiTro.WAREHOUSE_MANAGER);
        cskhToken = bearer("nd_cskh", "cskh@trithucviet.vn", VaiTro.CSKH);
        customerToken = bearer("nd_kh01", "thang.nguyen@gmail.com", VaiTro.CUSTOMER);
    }

    private String bearer(String userId, String email, VaiTro vaiTro) {
        return "Bearer " + jwtTokenProvider.generateToken(userId, email, vaiTro);
    }

    // =========================================================================
    // TEST 1: NHẬP KHO & TỰ ĐỘNG CỘNG TỒN KHO + PHÂN QUYỀN
    // =========================================================================

    @Test
    void quanLyKhoVaAdminLapPhieuNhap_congTonKhoTuDong_chanCskhVaCustomer() throws Exception {
        // CSKH & CUSTOMER bị chặn 403 ở toàn bộ API kho
        mockMvc.perform(get("/api/admin/inventory/receipts").header("Authorization", cskhToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/inventory/receipts").header("Authorization", customerToken))
                .andExpect(status().isForbidden());

        // WAREHOUSE_MANAGER xem danh sách phiếu nhập có sẵn trong seed (pnk_01, pnk_02)
        mockMvc.perform(get("/api/admin/inventory/receipts").header("Authorization", warehouseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem("pnk_01")))
                .andExpect(jsonPath("$.data[*].id", hasItem("pnk_02")));

        int s01Before = sachRepository.findById("sach_001").orElseThrow().getTonKho();
        int s02Before = sachRepository.findById("sach_002").orElseThrow().getTonKho();

        // WAREHOUSE_MANAGER (Trần Quốc Đạt) lập phiếu nhập 2 đầu sách
        String payload = """
                {
                  "tenNhaCungCap": "Công ty Phát hành Sách Fahasa",
                  "ghiChu": "Nhập bổ sung cuối tháng",
                  "items": [
                    { "sachId": "sach_001", "soLuong": 10, "giaNhap": 70000 },
                    { "sachId": "sach_002", "soLuong": 5, "giaNhap": 80000 }
                  ]
                }
                """;

        mockMvc.perform(post("/api/admin/inventory/receipts")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maPhieu", containsString("PNK-")))
                .andExpect(jsonPath("$.data.nguoiTao").value("Trần Quốc Đạt"))
                .andExpect(jsonPath("$.data.tongSoLuong").value(15))
                .andExpect(jsonPath("$.data.tongTienNhap").value(1100000))
                .andExpect(jsonPath("$.data.items.length()").value(2));

        entityManager.flush();
        entityManager.clear();

        // Xác nhận tồn kho tăng đúng 10 và 5 cuốn
        assertEquals(s01Before + 10, sachRepository.findById("sach_001").orElseThrow().getTonKho());
        assertEquals(s02Before + 5, sachRepository.findById("sach_002").orElseThrow().getTonKho());
    }

    @Test
    void lapPhieuNhapViPhamRangBuoc_tra400Hoac404() throws Exception {
        // Số lượng = 0 -> 400
        mockMvc.perform(post("/api/admin/inventory/receipts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenNhaCungCap": "NXB Trẻ",
                                  "items": [{ "sachId": "sach_001", "soLuong": 0, "giaNhap": 50000 }]
                                }
                                """))
                .andExpect(status().isBadRequest());

        // Mã sách không tồn tại -> 404
        mockMvc.perform(post("/api/admin/inventory/receipts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenNhaCungCap": "NXB Trẻ",
                                  "items": [{ "sachId": "sach_khong_ton_tai", "soLuong": 5, "giaNhap": 50000 }]
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // TEST 2: CẢNH BÁO TỒN KHO THỐNG NHẤT NGƯỠNG <= 5 CUỐN
    // =========================================================================

    @Test
    void canhBaoTonKho_locDungNguongNhoHonHoacBang5Cuon() throws Exception {
        Sach s01 = sachRepository.findById("sach_001").orElseThrow();
        Sach s02 = sachRepository.findById("sach_002").orElseThrow();
        s01.setTonKho(5);
        s02.setTonKho(6);
        sachRepository.saveAndFlush(s01);
        sachRepository.saveAndFlush(s02);

        mockMvc.perform(get("/api/admin/inventory/low-stock").header("Authorization", warehouseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem("sach_001")))
                .andExpect(jsonPath("$.data[*].id", not(hasItem("sach_002"))));
    }

    // =========================================================================
    // TEST 3: DASHBOARD RBAC & XUẤT BÁO CÁO DOANH THU CSV
    // =========================================================================

    @Test
    void dashboardPhanQuyenTheoVaiTro_anDoanhThuVoiKhoVaCskh() throws Exception {
        // CUSTOMER bị chặn 403
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", customerToken))
                .andExpect(status().isForbidden());

        // ADMIN nhận đầy đủ tài chính + biểu đồ 7 ngày / 6 tháng + top 5 sách
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue", notNullValue()))
                .andExpect(jsonPath("$.data.revenueByDay.length()").value(7))
                .andExpect(jsonPath("$.data.revenueByMonth.length()").value(6))
                .andExpect(jsonPath("$.data.topSellingBooks.length()").value(5))
                .andExpect(jsonPath("$.data.lowStockThreshold").value(5))
                .andExpect(jsonPath("$.data.totalOrders", greaterThanOrEqualTo(4)));

        // WAREHOUSE_MANAGER & CSKH bị ẩn hoàn toàn các trường tài chính (null)
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", warehouseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue", nullValue()))
                .andExpect(jsonPath("$.data.revenueByMonth", nullValue()))
                .andExpect(jsonPath("$.data.lowStockThreshold").value(5))
                .andExpect(jsonPath("$.data.topSellingBooks.length()").value(5));

        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", cskhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue", nullValue()))
                .andExpect(jsonPath("$.data.revenueByDay", nullValue()))
                .andExpect(jsonPath("$.data.pendingReturnsCount", greaterThanOrEqualTo(1)));
    }

    @Test
    void xuatBaoCaoCsv_chiChoPhepAdmin() throws Exception {
        // WAREHOUSE_MANAGER, CSKH, CUSTOMER bị chặn 403
        mockMvc.perform(get("/api/admin/reports/revenue-export").header("Authorization", warehouseToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/reports/revenue-export").header("Authorization", cskhToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/reports/revenue-export").header("Authorization", customerToken))
                .andExpect(status().isForbidden());

        // ADMIN tải CSV thành công
        mockMvc.perform(get("/api/admin/reports/revenue-export").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("Bao_Cao_Doanh_Thu_BookStore_")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("Ky bao cao,Doanh thu (VND)")))
                .andExpect(content().string(containsString("Top Sach Ban Chay,Ma ISBN")));
    }
}
