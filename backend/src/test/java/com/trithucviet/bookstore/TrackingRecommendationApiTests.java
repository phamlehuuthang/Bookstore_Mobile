package com.trithucviet.bookstore;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.trithucviet.bookstore.config.JwtTokenProvider;
import com.trithucviet.bookstore.entity.HanhViNguoiDung;
import com.trithucviet.bookstore.entity.enums.LoaiHanhVi;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import com.trithucviet.bookstore.repository.HanhViNguoiDungRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Kiểm thử tích hợp toàn bộ Bước 8 bên Người A:
 * 1. Ghi nhận Hành vi Người dùng (POST /api/v1/tracking/event) theo Quy tắc 7.1 (Dwell Time & Trọng số điểm)
 * 2. Tự động ghi nhận hành vi theo Cách B khi Thả tim (+4), Thêm giỏ (+6) và Đặt hàng (+10)
 * 3. Đề xuất Sách Cá nhân hóa (GET /api/v1/books/recommendations) theo Quy tắc 7.2 (3:2:1),
 *    Quy tắc 7.3 (Time Decay 48h/14d/30d) và Quy tắc 7.4 (Candidate Exclusion + Impression Fatigue)
 */
@SpringBootTest
@Transactional
class TrackingRecommendationApiTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private HanhViNguoiDungRepository hanhViNguoiDungRepository;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private String kh01Token;
    private String kh05Token;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        kh01Token = bearer("nd_kh01", "thang.nguyen@gmail.com", VaiTro.CUSTOMER);
        kh05Token = bearer("nd_kh05", "bao.vo@gmail.com", VaiTro.CUSTOMER);
        adminToken = bearer("nd_admin", "admin@trithucviet.vn", VaiTro.ADMIN);
    }

    private String bearer(String userId, String email, VaiTro vaiTro) {
        return "Bearer " + jwtTokenProvider.generateToken(userId, email, vaiTro);
    }

    // =========================================================================
    // TEST 1: QUY TẮC 7.1 — DWELL TIME (<3s BỎ QUA, 3-4s +1, 5-14s +3, >=15s +5)
    // =========================================================================

    @Test
    void ghiNhanHanhVi_dwellTimeVaTrongSoDiem_chanChuaDangNhapVaAdmin() throws Exception {
        // Khách vãng lai gọi POST /api/v1/tracking/event -> 401
        mockMvc.perform(post("/api/v1/tracking/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loaiHanhVi\":\"XEM_CHI_TIET\",\"sachId\":\"sach_001\",\"thoiGianXemGiay\":10}"))
                .andExpect(status().isUnauthorized());

        // ADMIN gọi POST /api/v1/tracking/event -> 403
        mockMvc.perform(post("/api/v1/tracking/event")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loaiHanhVi\":\"XEM_CHI_TIET\",\"sachId\":\"sach_001\",\"thoiGianXemGiay\":10}"))
                .andExpect(status().isForbidden());

        long countBefore = hanhViNguoiDungRepository.findByNguoiDungIdWithDetailsOrdered("nd_kh05").size();

        // 1. XEM_CHI_TIET dưới 3 giây (2 giây) -> ignored = true, KHÔNG lưu vào DB
        mockMvc.perform(post("/api/v1/tracking/event")
                        .header("Authorization", kh05Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loaiHanhVi": "XEM_CHI_TIET",
                                  "sachId": "sach_001",
                                  "thoiGianXemGiay": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ignored").value(true))
                .andExpect(jsonPath("$.data.trongSoDiem").value(0));

        assertEquals(countBefore, hanhViNguoiDungRepository.findByNguoiDungIdWithDetailsOrdered("nd_kh05").size());

        // 2. XEM_CHI_TIET 4 giây -> +1 điểm
        mockMvc.perform(post("/api/v1/tracking/event")
                        .header("Authorization", kh05Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loaiHanhVi": "XEM_CHI_TIET",
                                  "sachId": "sach_001",
                                  "thoiGianXemGiay": 4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ignored").value(false))
                .andExpect(jsonPath("$.data.trongSoDiem").value(1));

        // 3. XEM_CHI_TIET 10 giây -> +3 điểm
        mockMvc.perform(post("/api/v1/tracking/event")
                        .header("Authorization", kh05Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loaiHanhVi": "XEM_CHI_TIET",
                                  "sachId": "sach_002",
                                  "thoiGianXemGiay": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trongSoDiem").value(3));

        // 4. XEM_CHI_TIET 25 giây -> +5 điểm
        mockMvc.perform(post("/api/v1/tracking/event")
                        .header("Authorization", kh05Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loaiHanhVi": "XEM_CHI_TIET",
                                  "sachId": "sach_019",
                                  "thoiGianXemGiay": 25
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trongSoDiem").value(5));

        assertEquals(countBefore + 3, hanhViNguoiDungRepository.findByNguoiDungIdWithDetailsOrdered("nd_kh05").size());
    }

    // =========================================================================
    // TEST 2: CÁCH B — TỰ ĐỘNG GHI NHẬN HÀNH VI KHI THẢ TIM, THÊM GIỎ & ĐẶT HÀNG
    // =========================================================================

    @Test
    void cachB_tuDongGhiNhanHanhViKhiThaTimThemGioVaDatHang() throws Exception {
        // Dọn sạch lịch sử hành vi, giỏ hàng và yêu thích của nd_kh05 để kiểm tra chính xác
        entityManager.createNativeQuery("DELETE FROM hanh_vi_nguoi_dung WHERE nguoi_dung_id = 'nd_kh05'").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM sach_yeu_thich WHERE nguoi_dung_id = 'nd_kh05'").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM gio_hang WHERE nguoi_dung_id = 'nd_kh05'").executeUpdate();
        entityManager.flush();
        entityManager.clear();

        // 1. Thả tim sach_005 -> tự sinh THEM_YEU_THICH (+4 điểm)
        mockMvc.perform(post("/api/v1/wishlists/sach_005/toggle").header("Authorization", kh05Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.daYeuThich").value(true));

        // 2. Thêm sach_019 vào giỏ -> tự sinh THEM_GIO_HANG (+6 điểm)
        mockMvc.perform(post("/api/v1/cart")
                        .header("Authorization", kh05Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sachId\":\"sach_019\",\"soLuong\":1}"))
                .andExpect(status().isOk());

        // 3. Đặt hàng COD cho giỏ hàng vừa thêm -> tự sinh MUA_HANG (+10 điểm)
        // Đảm bảo nd_kh05 có địa chỉ nhận hàng
        entityManager.createNativeQuery("""
                INSERT INTO so_dia_chi (id, nguoi_dung_id, ho_ten_nguoi_nhan, so_dien_thoai, tinh_thanh, quan_huyen, phuong_xa, dia_chi_chi_tiet, mac_dinh)
                VALUES ('dc_test_kh05', 'nd_kh05', 'Võ Quốc Bảo', '0912345605', 'Cần Thơ', 'Ninh Kiều', 'Tân An', '19 Hòa Bình', TRUE)
                """).executeUpdate();
        entityManager.flush();

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", kh05Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"soDiaChiId\":\"dc_test_kh05\",\"phuongThucThanhToan\":\"TIEN_MAT_COD\"}"))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        List<HanhViNguoiDung> logs = hanhViNguoiDungRepository.findByNguoiDungIdWithDetailsOrdered("nd_kh05");
        assertEquals(3, logs.size());
        assertTrue(logs.stream().anyMatch(h -> h.getLoaiHanhVi() == LoaiHanhVi.THEM_YEU_THICH && h.getTrongSoDiem() == 4));
        assertTrue(logs.stream().anyMatch(h -> h.getLoaiHanhVi() == LoaiHanhVi.THEM_GIO_HANG && h.getTrongSoDiem() == 6));
        assertTrue(logs.stream().anyMatch(h -> h.getLoaiHanhVi() == LoaiHanhVi.MUA_HANG && h.getTrongSoDiem() == 10));
    }

    // =========================================================================
    // TEST 3: QUY TẮC 7.2, 7.3, 7.4 — PHA TRỘN 3:2:1, TIME DECAY, LOẠI TRỪ & FATIGUE
    // =========================================================================

    @Test
    void deXuatSachCaNhanHoa_phaTron321_loaiTruSachDaCo_vaXuLyImpressionFatigue() throws Exception {
        // 1. Khách vãng lai (không có token) vẫn gọi được GET /api/v1/books/recommendations -> trả về 6 cuốn xu hướng
        mockMvc.perform(get("/api/v1/books/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.books.length()").value(6))
                .andExpect(jsonPath("$.data.reasonLabel", containsString("xu hướng")));

        // 2. Cô lập dữ liệu cho nd_kh01:
        // - Đã mua sach_001 (trong dh_01), đang thích sach_016, đang bỏ giỏ sach_011 -> cả 3 cuốn này PHẢI bị loại trừ!
        // - Tạo lịch sử hành vi trên 3 chủ đề: dm_van_hoc (cao nhất), dm_trinh_tham (thứ 2), dm_vien_tuong (thứ 3)
        entityManager.createNativeQuery("DELETE FROM hanh_vi_nguoi_dung WHERE nguoi_dung_id = 'nd_kh01'").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM sach_yeu_thich WHERE nguoi_dung_id = 'nd_kh01'").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM gio_hang WHERE nguoi_dung_id = 'nd_kh01'").executeUpdate();

        entityManager.createNativeQuery("INSERT INTO sach_yeu_thich (nguoi_dung_id, sach_id) VALUES ('nd_kh01', 'sach_016')").executeUpdate();
        entityManager.createNativeQuery("INSERT INTO gio_hang (nguoi_dung_id, sach_id, so_luong, da_chon) VALUES ('nd_kh01', 'sach_011', 1, TRUE)").executeUpdate();

        // Thêm 1 hành vi XEM_CHI_TIET đã quá hạn 10 ngày trên dm_thieu_nhi -> Time Decay = 0 (không được lọt vào Top 3)
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        entityManager.createNativeQuery("""
                INSERT INTO hanh_vi_nguoi_dung (id, nguoi_dung_id, sach_id, danh_muc_id, loai_hanh_vi, thoi_gian_xem_giay, trong_so_diem, ngay_tao)
                VALUES
                  ('hv_t1', 'nd_kh01', 'sach_001', 'dm_van_hoc', 'MUA_HANG', 0, 10, :t1),
                  ('hv_t2', 'nd_kh01', 'sach_019', 'dm_trinh_tham', 'THEM_GIO_HANG', 0, 6, :t2),
                  ('hv_t3', 'nd_kh01', 'sach_034', 'dm_vien_tuong', 'THEM_YEU_THICH', 0, 4, :t3),
                  ('hv_expired', 'nd_kh01', 'sach_159', 'dm_thieu_nhi', 'XEM_CHI_TIET', 20, 5, :tExp)
                """)
                .setParameter("t1", now.minusHours(1))
                .setParameter("t2", now.minusHours(2))
                .setParameter("t3", now.minusHours(3))
                .setParameter("tExp", now.minusDays(10))
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        MvcResult recResult = mockMvc.perform(get("/api/v1/books/recommendations")
                        .header("Authorization", kh01Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.books.length()").value(6))
                .andExpect(jsonPath("$.data.topCategories.length()").value(3))
                // Loại trừ sách đã mua (sach_001), đã thích (sach_016), đã bỏ giỏ (sach_011)
                .andExpect(jsonPath("$.data.books[*].id", not(hasItem("sach_001"))))
                .andExpect(jsonPath("$.data.books[*].id", not(hasItem("sach_016"))))
                .andExpect(jsonPath("$.data.books[*].id", not(hasItem("sach_011"))))
                .andReturn();

        List<String> categoryIds = JsonPath.read(recResult.getResponse().getContentAsString(), "$.data.books[*].danhMucId");
        long vanHocCount = categoryIds.stream().filter("dm_van_hoc"::equals).count();
        long trinhThamCount = categoryIds.stream().filter("dm_trinh_tham"::equals).count();
        long vienTuongCount = categoryIds.stream().filter("dm_vien_tuong"::equals).count();

        // Đúng tỷ lệ 3:2:1
        assertEquals(3L, vanHocCount);
        assertEquals(2L, trinhThamCount);
        assertEquals(1L, vienTuongCount);

        // 3. Kiểm tra Impression Fatigue: Gửi 10 lần BO_QUA_DE_XUAT liên tiếp cho dm_van_hoc -> điểm dm_van_hoc bị phạt giảm xuống dưới dm_trinh_tham
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/tracking/event")
                            .header("Authorization", kh01Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"loaiHanhVi\":\"BO_QUA_DE_XUAT\",\"danhMucId\":\"dm_van_hoc\"}"))
                    .andExpect(status().isOk());
        }

        // Sau 10 lần ngó lơ dm_van_hoc, chủ đề #1 chuyển sang Trinh thám & Bí ẩn
        mockMvc.perform(get("/api/v1/books/recommendations").header("Authorization", kh01Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topCategories[0]", containsString("Trinh thám")));
    }
}
