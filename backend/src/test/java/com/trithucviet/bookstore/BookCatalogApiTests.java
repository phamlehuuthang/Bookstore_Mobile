package com.trithucviet.bookstore;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.repository.SachRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Kiểm thử tích hợp toàn bộ API Danh mục, Trang chủ, Tìm kiếm/Lọc/Sắp xếp & Chi tiết Sách (Bước 3 - phần 1 & 2)
 * trực tiếp trên 186 sách và 12 danh mục thật của bookstore_db.
 * - Tất cả các API đều công khai (không cần gửi Authorization header).
 * - @Transactional tự rollback mọi thao tác ẩn tạm sách thử nghiệm.
 */
@SpringBootTest
@Transactional
class BookCatalogApiTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SachRepository sachRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // --- Phần 1: Cây danh mục & Trang chủ ---

    @Test
    void layCayDanhMuc_traVe7DanhMucChaGopSoSachCuaCon() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(7)))
                .andExpect(jsonPath("$.data[0].id").value("dm_van_hoc"))
                .andExpect(jsonPath("$.data[0].danhMucCon", hasSize(2)))
                .andExpect(jsonPath("$.data[0].danhMucCon[*].id", hasItem("dm_trinh_tham")))
                .andExpect(jsonPath("$.data[0].danhMucCon[*].id", hasItem("dm_vien_tuong")))
                .andReturn();

        List<Number> rootCounts = JsonPath.read(result.getResponse().getContentAsString(), "$.data[*].soLuongSach");
        long total = rootCounts.stream().mapToLong(Number::longValue).sum();
        assertEquals(186L, total, "Tổng số sách cộng gộp qua 7 danh mục cha phải bằng đúng 186 cuốn");
    }

    @Test
    void laySachTrangChu_gioiHan12CuonMacDinhVaHoTroTuyChinhLimit() throws Exception {
        mockMvc.perform(get("/api/v1/books/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flashSaleBooks", hasSize(12)))
                .andExpect(jsonPath("$.data.bestSellerBooks", hasSize(12)))
                .andExpect(jsonPath("$.data.tongSoSachFlashSale").value(26))
                .andExpect(jsonPath("$.data.tongSoSachBanChay").value(69))
                .andExpect(jsonPath("$.data.tongSoSachHienThi").value(186));

        mockMvc.perform(get("/api/v1/books/home").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flashSaleBooks", hasSize(5)))
                .andExpect(jsonPath("$.data.bestSellerBooks", hasSize(5)));
    }

    // --- Phần 2: Phân trang, 8 kiểu sắp xếp chuẩn TMĐT & Bộ lọc đa tiêu chí ---

    @Test
    void phanTrangMacDinh_traVe20CuonMoiTrangTrongTong186Cuon() throws Exception {
        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(20)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(186))
                .andExpect(jsonPath("$.data.totalPages").value(10))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.hasPrevious").value(false))
                .andExpect(jsonPath("$.data.appliedSort").value("popular"));

        // Trang cuối (page=9) phải còn đúng 6 cuốn (186 - 9*20)
        mockMvc.perform(get("/api/v1/books").param("page", "9").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(6)))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.hasPrevious").value(true));
    }

    @Test
    void sapXepGiaTangGiam_giamGiaNhieuNhat_saoCaoNhat_moiNhatVaTenAZ() throws Exception {
        // 1. Giá thấp -> cao (price_asc) theo donGiaApDung
        MvcResult ascRes = mockMvc.perform(get("/api/v1/books")
                        .param("sortBy", "price_asc")
                        .param("size", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appliedSort").value("price_asc"))
                .andReturn();
        List<Number> ascPrices = JsonPath.read(ascRes.getResponse().getContentAsString(), "$.data.items[*].donGiaApDung");
        for (int i = 1; i < ascPrices.size(); i++) {
            assertTrue(ascPrices.get(i - 1).doubleValue() <= ascPrices.get(i).doubleValue(),
                    "price_asc phải tăng dần theo donGiaApDung");
        }

        // 2. Giá cao -> thấp (price_desc) theo donGiaApDung
        MvcResult descRes = mockMvc.perform(get("/api/v1/books")
                        .param("sortBy", "price_desc")
                        .param("size", "30"))
                .andExpect(status().isOk())
                .andReturn();
        List<Number> descPrices = JsonPath.read(descRes.getResponse().getContentAsString(), "$.data.items[*].donGiaApDung");
        for (int i = 1; i < descPrices.size(); i++) {
            assertTrue(descPrices.get(i - 1).doubleValue() >= descPrices.get(i).doubleValue(),
                    "price_desc phải giảm dần theo donGiaApDung");
        }

        // 3. Giảm giá nhiều nhất (discount_desc)
        MvcResult discountRes = mockMvc.perform(get("/api/v1/books")
                        .param("sortBy", "discount_desc")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andReturn();
        List<Number> discounts = JsonPath.read(discountRes.getResponse().getContentAsString(), "$.data.items[*].phanTramGiam");
        for (int i = 1; i < discounts.size(); i++) {
            assertTrue(discounts.get(i - 1).intValue() >= discounts.get(i).intValue(),
                    "discount_desc phải giảm dần theo phanTramGiam");
        }

        // 4. Đánh giá cao nhất (rating)
        MvcResult ratingRes = mockMvc.perform(get("/api/v1/books")
                        .param("sortBy", "rating")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andReturn();
        List<Number> ratings = JsonPath.read(ratingRes.getResponse().getContentAsString(), "$.data.items[*].diemDanhGiaTb");
        for (int i = 1; i < ratings.size(); i++) {
            assertTrue(ratings.get(i - 1).doubleValue() >= ratings.get(i).doubleValue(),
                    "rating phải giảm dần theo diemDanhGiaTb");
        }

        // 5. Mới nhất (newest), Tên A->Z (title_asc), Tên Z->A (title_desc) đều trả 200 hợp lệ
        mockMvc.perform(get("/api/v1/books").param("sortBy", "newest")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/books").param("sortBy", "title_asc")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/books").param("sortBy", "title_desc")).andExpect(status().isOk());

        // 6. Kiểu sắp xếp sai -> 400 Bad Request
        mockMvc.perform(get("/api/v1/books").param("sortBy", "khong_ton_tai"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void locDaTieuChi_danhMucChaGomCon_khoangGia_tuKhoa_flashSale_vaChongGiaKhongHopLe() throws Exception {
        // Lọc theo danh mục cha dm_van_hoc -> phải bao gồm cả sách của danh mục con dm_trinh_tham / dm_vien_tuong
        MvcResult parentRes = mockMvc.perform(get("/api/v1/books")
                        .param("categoryId", "dm_van_hoc")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].danhMucId", hasItem("dm_trinh_tham")))
                .andExpect(jsonPath("$.data.items[*].danhMucId", hasItem("dm_vien_tuong")))
                .andReturn();
        int vanHocTotal = JsonPath.read(parentRes.getResponse().getContentAsString(), "$.data.totalElements");
        assertTrue(vanHocTotal > 20, "dm_van_hoc gộp cả 2 danh mục con phải có hơn 20 cuốn");

        // Kết hợp danh mục + khoảng giá + sắp xếp giá thấp -> cao
        MvcResult comboRes = mockMvc.perform(get("/api/v1/books")
                        .param("categoryId", "dm_cong_nghe")
                        .param("minPrice", "100000")
                        .param("maxPrice", "300000")
                        .param("inStockOnly", "true")
                        .param("sortBy", "price_asc")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", greaterThan(0)))
                .andReturn();
        List<Number> comboPrices = JsonPath.read(comboRes.getResponse().getContentAsString(), "$.data.items[*].donGiaApDung");
        for (Number p : comboPrices) {
            assertTrue(p.doubleValue() >= 100000 && p.doubleValue() <= 300000,
                    "Mọi sách sau khi lọc phải có donGiaApDung trong khoảng [100000, 300000]");
        }

        // Lọc chỉ Flash Sale -> đúng 26 cuốn; chỉ Bán chạy -> đúng 69 cuốn
        mockMvc.perform(get("/api/v1/books").param("flashSaleOnly", "true").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(26));

        mockMvc.perform(get("/api/v1/books").param("bestSellerOnly", "true").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(69));

        // Tìm theo tên tác giả
        mockMvc.perform(get("/api/v1/books").param("search", "Lewis Carroll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem("sach_001")));

        // minPrice > maxPrice -> 400 Bad Request
        mockMvc.perform(get("/api/v1/books").param("minPrice", "500000").param("maxPrice", "100000"))
                .andExpect(status().isBadRequest());
    }

    // --- Chi tiết sách, Sách ẩn & Đánh giá công khai ---

    @Test
    void chiTietSach_traVeThongSoVaSachCungChuDe_anSachBiChan404() throws Exception {
        mockMvc.perform(get("/api/v1/books/sach_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("sach_001"))
                .andExpect(jsonPath("$.data.tenTacGia").isNotEmpty())
                .andExpect(jsonPath("$.data.tenNhaXuatBan").isNotEmpty())
                .andExpect(jsonPath("$.data.anhChiTietUrls", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.tuKhoa", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.sachCungChuDe", hasSize(6)))
                .andExpect(jsonPath("$.data.sachCungChuDe[*].id", not(hasItem("sach_001"))));

        // Sách không tồn tại -> 404
        mockMvc.perform(get("/api/v1/books/sach_khong_ton_tai"))
                .andExpect(status().isNotFound());

        // Thử ẩn tạm sach_001 (tự rollback sau test) -> chi tiết trả 404 và tổng sách giảm xuống 185
        Sach s1 = sachRepository.findById("sach_001").orElseThrow();
        s1.setHienThi(false);
        sachRepository.saveAndFlush(s1);

        mockMvc.perform(get("/api/v1/books/sach_001")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/books")).andExpect(jsonPath("$.data.totalElements").value(185));
    }

    @Test
    void layDanhGiaCongKhaiCuaSach_thanhCongVaChanSachKhongTonTai() throws Exception {
        mockMvc.perform(get("/api/v1/books/sach_001/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/books/sach_khong_ton_tai/reviews"))
                .andExpect(status().isNotFound());
    }
}
