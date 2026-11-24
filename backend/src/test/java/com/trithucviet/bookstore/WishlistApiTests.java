package com.trithucviet.bookstore;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trithucviet.bookstore.config.JwtTokenProvider;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Kiểm thử tích hợp toàn bộ API Tủ sách yêu thích trên dữ liệu thật của bookstore_db.
 * - @Transactional ở cấp class giúp Spring tự rollback mọi thay đổi sau từng test -> không làm bẩn dữ liệu mẫu.
 * - MockMvc được gắn .apply(springSecurity()) để chạy đầy đủ JwtAuthenticationFilter và @PreAuthorize như server thật.
 * - Dữ liệu mẫu tham chiếu: nd_kh01 (CUSTOMER hoạt động, đang thích sach_001 & sach_016), nd_kh09 (CUSTOMER bị khóa).
 */
@SpringBootTest
@Transactional
class WishlistApiTests {

    private static final String BASE = "/api/v1/wishlists";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private String customerToken;

    @BeforeEach
    void setUp() {
        // Kích hoạt chuỗi Filter của Spring Security cho MockMvc để test được cả 401/403
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        customerToken = bearer("nd_kh01", "thang.nguyen@gmail.com", VaiTro.CUSTOMER);

        // Cô lập dữ liệu tủ sách yêu thích của nd_kh01 trước mỗi test để không phụ thuộc thao tác demo trên giao diện
        entityManager.createNativeQuery("DELETE FROM sach_yeu_thich WHERE nguoi_dung_id = 'nd_kh01'").executeUpdate();
        entityManager.createNativeQuery(
                "INSERT INTO sach_yeu_thich (nguoi_dung_id, sach_id) VALUES ('nd_kh01', 'sach_001'), ('nd_kh01', 'sach_016')"
        ).executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    // Sinh chuỗi Authorization header hợp lệ bằng chính JwtTokenProvider của dự án (thay cho API Login khi Người A chưa xong Bước 2)
    private String bearer(String userId, String email, VaiTro vaiTro) {
        return "Bearer " + jwtTokenProvider.generateToken(userId, email, vaiTro);
    }

    // --- Nhóm 1: Các luồng nghiệp vụ thành công (200 OK) ---

    @Test
    void layTuSachYeuThich_traVeDungSachCuaKhach() throws Exception {
        mockMvc.perform(get(BASE).header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].sachId", hasItem("sach_001")))
                .andExpect(jsonPath("$.data[*].sachId", hasItem("sach_016")))
                .andExpect(jsonPath("$.data[0].tieuDe").isNotEmpty())
                .andExpect(jsonPath("$.data[0].tenTacGia").isNotEmpty())
                .andExpect(jsonPath("$.data[0].anhBiaUrl").isNotEmpty());
    }

    @Test
    void thaTimRoiBoTim_capNhatDungTrangThaiVaTongSo() throws Exception {
        mockMvc.perform(post(BASE + "/sach_002/toggle").header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã thêm vào Tủ sách yêu thích"))
                .andExpect(jsonPath("$.data.daYeuThich").value(true))
                .andExpect(jsonPath("$.data.tongSoSachYeuThich").value(3));

        mockMvc.perform(get(BASE + "/sach_002/status").header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.daYeuThich").value(true));

        mockMvc.perform(post(BASE + "/sach_002/toggle").header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã bỏ khỏi Tủ sách yêu thích"))
                .andExpect(jsonPath("$.data.daYeuThich").value(false))
                .andExpect(jsonPath("$.data.tongSoSachYeuThich").value(2));
    }

    @Test
    void xoaSachKhoiTuYeuThich_thanhCongVaKhongConTrongDanhSach() throws Exception {
        mockMvc.perform(delete(BASE + "/sach_001").header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.daYeuThich").value(false))
                .andExpect(jsonPath("$.data.tongSoSachYeuThich").value(1));

        mockMvc.perform(get(BASE).header("Authorization", customerToken))
                .andExpect(jsonPath("$.data[*].sachId", not(hasItem("sach_001"))));
    }

    // --- Nhóm 2: Các ca kiểm thử lỗi nghiệp vụ & bảo mật (404 / 401 / 403) ---

    @Test
    void xoaSachKhongCoTrongTuYeuThich_tra404() throws Exception {
        mockMvc.perform(delete(BASE + "/sach_100").header("Authorization", customerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void thaTimSachKhongTonTai_tra404() throws Exception {
        mockMvc.perform(post(BASE + "/sach_999/toggle").header("Authorization", customerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy cuốn sách có mã sach_999."));
    }

    @Test
    void khongCoToken_tra401() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void tokenSai_tra401() throws Exception {
        mockMvc.perform(get(BASE).header("Authorization", "Bearer token-gia-mao"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nhanSuAdmin_khongDuocDungTuSachYeuThich_tra403() throws Exception {
        mockMvc.perform(get(BASE).header("Authorization", bearer("nd_admin", "admin@trithucviet.vn", VaiTro.ADMIN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void khachBiKhoa_tra403() throws Exception {
        mockMvc.perform(get(BASE).header("Authorization", bearer("nd_kh09", "kiet.do@gmail.com", VaiTro.CUSTOMER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ CSKH để được hỗ trợ."));
    }
}
