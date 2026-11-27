package com.trithucviet.bookstore;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.trithucviet.bookstore.config.JwtTokenProvider;
import com.trithucviet.bookstore.entity.DonHang;
import com.trithucviet.bookstore.entity.enums.TrangThaiDon;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import com.trithucviet.bookstore.repository.DonHangRepository;
import com.trithucviet.bookstore.repository.GioHangRepository;
import com.trithucviet.bookstore.repository.SachRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Kiểm thử tích hợp toàn bộ Bước 10 nâng cấp (10 Agentic Tools: Tự đặt đơn 1 bước + Tự săn Voucher tốt nhất,
 * Thanh toán giỏ, Thao tác Giỏ/Yêu thích, Tra cứu & Hủy đơn, Gợi ý chủ đề/Flash Sale/Combo/Hợp gu & RBAC Vector).
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "app.ai.gemini.api-key=",
        "app.ai.gemini.auto-sync-on-startup=false"
})
class AiAdvisorApiTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SachRepository sachRepository;

    @Autowired
    private GioHangRepository gioHangRepository;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private String kh01Token;
    private String adminToken;
    private String warehouseToken;
    private String cskhToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        kh01Token = bearer("nd_kh01", "thang.nguyen@gmail.com", VaiTro.CUSTOMER);
        adminToken = bearer("nd_admin", "admin@trithucviet.vn", VaiTro.ADMIN);
        warehouseToken = bearer("nd_kho", "kho@trithucviet.vn", VaiTro.WAREHOUSE_MANAGER);
        cskhToken = bearer("nd_cskh", "cskh@trithucviet.vn", VaiTro.CSKH);
    }

    private String bearer(String userId, String email, VaiTro vaiTro) {
        return "Bearer " + jwtTokenProvider.generateToken(userId, email, vaiTro);
    }

    @Test
    void khachVangLai_tuVanChuDe_flashSale_combo_voucher_nhungDatHangNhacDangNhap() throws Exception {
        // 1. Tìm sách kinh tế dưới 150k
        MvcResult res = mockMvc.perform(post("/api/v1/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Tìm sách kinh tế dưới 150k\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.suggestedBooks.length()", greaterThan(0)))
                .andReturn();

        List<Number> prices = JsonPath.read(res.getResponse().getContentAsString(), "$.data.suggestedBooks[*].donGiaApDung");
        assertTrue(prices.stream().allMatch(p -> p.longValue() <= 150_000L));

        // 1b. Tìm sách giá rẻ dưới 100 ngàn (đơn vị "ngàn", không có chữ "k") -> mọi cuốn phải <= 100.000đ
        MvcResult cheapRes = mockMvc.perform(post("/api/v1/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"gợi ý tui vài cuốn sách giá rẻ dưới 100 ngàn đi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestedBooks.length()", greaterThan(0)))
                .andReturn();
        List<Number> cheapPrices = JsonPath.read(cheapRes.getResponse().getContentAsString(), "$.data.suggestedBooks[*].donGiaApDung");
        assertTrue(cheapPrices.stream().allMatch(p -> p.longValue() <= 100_000L));

        // 2. Gợi ý theo chủ đề tâm lý học -> đúng danh mục dm_ky_nang
        MvcResult topicRes = mockMvc.perform(post("/api/v1/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Gợi ý sách theo chủ đề tâm lý học\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestedBooks.length()", greaterThan(0)))
                .andReturn();
        List<String> catIds = JsonPath.read(topicRes.getResponse().getContentAsString(), "$.data.suggestedBooks[*].danhMucId");
        assertTrue(catIds.stream().allMatch("dm_ky_nang"::equals));

        // 3. Săn Flash Sale & ghép Combo theo ngân sách
        mockMvc.perform(post("/api/v1/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hôm nay có sách nào đang Flash Sale giảm sâu?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestedBooks.length()", greaterThan(0)));

        mockMvc.perform(post("/api/v1/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Ghép combo sách dưới 350k giúp mình\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestedBooks.length()", greaterThan(0)));

        // 4. Hỏi voucher đang mở
        mockMvc.perform(post("/api/v1/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Cho mình xem các mã giảm giá hiện có\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply", containsString("CHAOBAN20")));

        // 5. Khách vãng lai nhờ đặt sách -> nhắc đăng nhập, KHÔNG tạo đơn
        mockMvc.perform(post("/api/v1/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Đặt luôn 1 cuốn Hamlet giúp mình\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply", containsString("đăng nhập")))
                .andExpect(jsonPath("$.data.createdOrderCode", nullValue()));
    }

    @Test
    void khachDaDangNhap_tuDatSach1BuocKemSanVoucher_thaoTacGioYeuThich_vaTraCuuHuyDonQuaAi() throws Exception {
        entityManager.createNativeQuery("DELETE FROM gio_hang WHERE nguoi_dung_id = 'nd_kh01'").executeUpdate();
        entityManager.createNativeQuery(
                "INSERT INTO gio_hang (nguoi_dung_id, sach_id, so_luong, da_chon) VALUES ('nd_kh01', 'sach_011', 1, TRUE)"
        ).executeUpdate();
        entityManager.flush();
        entityManager.clear();

        // 1. Nhờ AI thêm cuốn Alice vào giỏ -> cartUpdated = true (2 món trong giỏ)
        mockMvc.perform(post("/api/v1/chat/ask")
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Thêm cuốn Alice's Adventures in Wonderland vào giỏ hàng giúp mình\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cartUpdated").value(true));
        assertEquals(2, gioHangRepository.findAllWithSachByNguoiDungId("nd_kh01").size());

        // 2. Nhờ AI thả tim cuốn Hamlet -> wishlistUpdated = true
        mockMvc.perform(post("/api/v1/chat/ask")
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Thả tim lưu cuốn Hamlet vào yêu thích giúp mình\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wishlistUpdated").value(true));

        // 3. Nhờ AI TỰ ĐẶT NGAY 1 cuốn Hamlet (1 bước, KHÔNG đụng giỏ cũ 2 món, tự áp mã giảm giá tốt nhất!)
        int stockBefore = sachRepository.findById("sach_004").orElseThrow().getTonKho();

        MvcResult orderRes = mockMvc.perform(post("/api/v1/chat/ask")
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Đặt ngay cho mình 1 cuốn Hamlet\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdOrderCode", containsString("DH-")))
                .andExpect(jsonPath("$.data.notificationsUpdated").value(true))
                .andExpect(jsonPath("$.data.dynamicSuggestions.length()", greaterThan(0)))
                .andReturn();

        String createdCode = JsonPath.read(orderRes.getResponse().getContentAsString(), "$.data.createdOrderCode");
        entityManager.flush();
        entityManager.clear();

        // Kiểm tra tồn kho Hamlet giảm đúng 1, giỏ hàng cũ 2 món giữ nguyên, và đơn đã được tự săn Voucher giảm tiền!
        assertEquals(stockBefore - 1, sachRepository.findById("sach_004").orElseThrow().getTonKho());
        assertEquals(2, gioHangRepository.findAllWithSachByNguoiDungId("nd_kh01").size());

        DonHang savedOrder = donHangRepository.findAllOrdered().stream()
                .filter(o -> createdCode.equals(o.getMaDonHang()))
                .findFirst()
                .orElseThrow();
        assertNotNull(savedOrder.getMaGiamGiaCode());
        assertTrue(savedOrder.getTienGiamGia().longValue() > 0L);

        // 4. Nhờ AI tra cứu mã đơn vừa đặt -> hiển thị đúng CHO_XAC_NHAN
        mockMvc.perform(post("/api/v1/chat/ask")
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Kiểm tra đơn " + createdCode + " giúp mình\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply", containsString(createdCode)))
                .andExpect(jsonPath("$.data.reply", containsString("CHO_XAC_NHAN")));

        // 5. Nhờ AI HỦY luôn đơn vừa đặt -> chuyển sang DA_HUY và hoàn lại tồn kho Hamlet về stockBefore!
        mockMvc.perform(post("/api/v1/chat/ask")
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hủy đơn " + createdCode + " giúp mình nhé\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply", containsString(createdCode)));

        entityManager.flush();
        entityManager.clear();

        DonHang afterCancel = donHangRepository.findById(savedOrder.getId()).orElseThrow();
        assertEquals(TrangThaiDon.DA_HUY, afterCancel.getTrangThaiDon());
        assertEquals(stockBefore, sachRepository.findById("sach_004").orElseThrow().getTonKho());

        // 6. Nhờ AI thanh toán toàn bộ giỏ hàng hiện tại -> tạo đơn mới & dọn sách đã mua khỏi giỏ
        mockMvc.perform(post("/api/v1/chat/ask")
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Thanh toán giỏ hàng giúp mình luôn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdOrderCode", notNullValue()))
                .andExpect(jsonPath("$.data.cartUpdated").value(true));

        entityManager.flush();
        entityManager.clear();
        assertEquals(0, gioHangRepository.findAllWithSachByNguoiDungId("nd_kh01").size());
    }

    @Test
    void dongBoAiVector_chiChoPhepAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/books/sync-vectors").header("Authorization", warehouseToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/books/sync-vectors").header("Authorization", cskhToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/books/sync-vectors").header("Authorization", kh01Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/books/sync-vectors").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBooks", greaterThanOrEqualTo(186)));
    }
}
