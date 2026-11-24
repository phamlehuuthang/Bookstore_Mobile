package com.trithucviet.bookstore;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.trithucviet.bookstore.config.JwtTokenProvider;
import com.trithucviet.bookstore.entity.DonHang;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.entity.ThongBao;
import com.trithucviet.bookstore.entity.enums.TrangThaiThanhToan;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import com.trithucviet.bookstore.repository.DonHangRepository;
import com.trithucviet.bookstore.repository.SachRepository;
import com.trithucviet.bookstore.repository.ThongBaoRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
 * Kiểm thử tích hợp toàn bộ API Bước 7:
 * 1. Đánh giá sách (Mobile) & Kiểm duyệt đánh giá (Web Admin)
 * 2. Yêu cầu Đổi/Trả hàng (Mobile) & Duyệt Đổi/Trả (Web Admin) + trả kèm returnRequest trong OrderDto
 * 3. Thông báo đẩy (Mobile) & Phát sóng thông báo (Web Admin)
 *
 * - @Transactional tự rollback toàn bộ dữ liệu sau mỗi test case.
 * - Kiểm tra chặt phân quyền 4 vai trò: CUSTOMER, ADMIN, CSKH, WAREHOUSE_MANAGER.
 */
@SpringBootTest
@Transactional
class ReviewReturnNotifApiTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private SachRepository sachRepository;

    @Autowired
    private ThongBaoRepository thongBaoRepository;

    private MockMvc mockMvc;
    private String kh01Token;
    private String kh02Token;
    private String kh03Token;
    private String kh04Token;
    private String kh10Token;
    private String adminToken;
    private String cskhToken;
    private String warehouseToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        kh01Token = bearer("nd_kh01", "thang.nguyen@gmail.com", VaiTro.CUSTOMER);
        kh02Token = bearer("nd_kh02", "minhanh.tran@gmail.com", VaiTro.CUSTOMER);
        kh03Token = bearer("nd_kh03", "nam.le@gmail.com", VaiTro.CUSTOMER);
        kh04Token = bearer("nd_kh04", "ha.pham@gmail.com", VaiTro.CUSTOMER);
        kh10Token = bearer("nd_kh10", "thao.ngo@gmail.com", VaiTro.CUSTOMER);
        adminToken = bearer("nd_admin", "admin@trithucviet.vn", VaiTro.ADMIN);
        cskhToken = bearer("nd_cskh", "cskh@trithucviet.vn", VaiTro.CSKH);
        warehouseToken = bearer("nd_kho", "kho@trithucviet.vn", VaiTro.WAREHOUSE_MANAGER);
    }

    private String bearer(String userId, String email, VaiTro vaiTro) {
        return "Bearer " + jwtTokenProvider.generateToken(userId, email, vaiTro);
    }

    // =========================================================================
    // TEST 1: ĐÁNH GIÁ SÁCH TRÊN ĐƠN 1 CUỐN & ĐƠN NHIỀU CUỐN + CHẶN LỖI
    // =========================================================================

    @Test
    void khachDanhGiaDonDaGiao_tinhLaiSao_chongTrungLapVaChongDonChuaGiao() throws Exception {
        // 1. Đơn dh_08 (nd_kh10, DA_GIAO, da_danh_gia = false, gồm 1 cuốn sach_008) -> bỏ trống sachId tự chọn sach_008
        String reviewPayload = """
                {
                  "donHangId": "dh_08",
                  "soSao": 4,
                  "binhLuan": "Sách dịch mượt, giấy đẹp, giao nhanh!",
                  "anhDanhGiaUrls": ["https://covers.openlibrary.org/b/id/13301713-M.jpg"]
                }
                """;

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", kh10Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sachId").value("sach_008"))
                .andExpect(jsonPath("$.data.soSao").value(4))
                .andExpect(jsonPath("$.data.anhDanhGiaUrls.length()").value(1));

        entityManager.flush();
        entityManager.clear();

        // Xác nhận sach_008 được tính lại điểm trung bình (4.00, 1 lượt) và dh_08 bật daDanhGia = true
        Sach sach08 = sachRepository.findById("sach_008").orElseThrow();
        assertEquals(1, sach08.getSoLuotDanhGia());
        assertEquals(0, new BigDecimal("4.00").compareTo(sach08.getDiemDanhGiaTb()));

        DonHang dh08 = donHangRepository.findById("dh_08").orElseThrow();
        assertTrue(Boolean.TRUE.equals(dh08.getDaDanhGia()));

        // 2. Gửi đánh giá trùng (dh_08, sach_008) -> 400 Bad Request
        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", kh10Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("đã đánh giá")));

        // 3. Gửi đánh giá cho đơn chưa giao (dh_04 đang DANG_DONG_GOI của nd_kh04) -> 400 Bad Request
        String notDeliveredPayload = """
                {
                  "donHangId": "dh_04",
                  "soSao": 5,
                  "binhLuan": "Chưa nhận nhưng vẫn muốn đánh giá"
                }
                """;
        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", kh04Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notDeliveredPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("đã giao thành công")));

        // 4. Khách khác (nd_kh01) cố đánh giá hộ đơn dh_08 của nd_kh10 -> 404 Not Found (chống IDOR)
        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewPayload))
                .andExpect(status().isNotFound());
    }

    @Test
    void donNhieuCuon_chiBatDaDanhGiaKhiDanhGiaDuTatCaSachTrongDon() throws Exception {
        // Tạo 1 đơn DA_GIAO của nd_kh01 gồm 2 đầu sách khác nhau: sach_010 và sach_011
        entityManager.createNativeQuery("""
                INSERT INTO don_hang (
                    id, ma_don_hang, nguoi_dung_id, ten_khach_hang, sdt_khach_hang, dia_chi_giao_hang,
                    tam_tinh, phi_van_chuyen, tien_giam_gia, tong_thanh_toan,
                    phuong_thuc_thanh_toan, trang_thai_thanh_toan, trang_thai_don, da_danh_gia
                ) VALUES (
                    'dh_test_multi', 'DH-TEST-MULTI', 'nd_kh01', 'Nguyễn Hữu Thắng', '0912345601', '12 Lê Lợi, Q.1',
                    634000, 0, 0, 634000,
                    'CHUYEN_KHOAN_QR', 'DA_THANH_TOAN', 'DA_GIAO', FALSE
                )
                """).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO chi_tiet_don_hang (id, don_hang_id, sach_id, tieu_de_sach, anh_bia_url, ten_tac_gia, don_gia, so_luong)
                VALUES
                    ('ct_tm_1', 'dh_test_multi', 'sach_010', 'Treasure Island', 'https://example.com/10.jpg', 'Stevenson', 304000, 1),
                    ('ct_tm_2', 'dh_test_multi', 'sach_011', 'Macbeth', 'https://example.com/11.jpg', 'Shakespeare', 330000, 1)
                """).executeUpdate();
        entityManager.flush();
        entityManager.clear();

        // Truyền sachId không thuộc đơn -> 400
        String wrongBookPayload = """
                {
                  "donHangId": "dh_test_multi",
                  "sachId": "sach_001",
                  "soSao": 5,
                  "binhLuan": "Sách này không có trong đơn"
                }
                """;
        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongBookPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("không nằm trong đơn hàng")));

        // Đánh giá cuốn 1 (sach_010) -> đơn vẫn chưa daDanhGia = true
        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "donHangId": "dh_test_multi",
                                  "sachId": "sach_010",
                                  "soSao": 5,
                                  "binhLuan": "Cuốn Treasure Island cực kỳ hấp dẫn"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sachId").value("sach_010"));

        entityManager.flush();
        entityManager.clear();
        assertFalse(Boolean.TRUE.equals(donHangRepository.findById("dh_test_multi").orElseThrow().getDaDanhGia()));

        // Đánh giá cuốn 2 (sach_011) -> đủ 2/2 cuốn, bật daDanhGia = true
        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "donHangId": "dh_test_multi",
                                  "sachId": "sach_011",
                                  "soSao": 3,
                                  "binhLuan": "Macbeth bản in ổn"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sachId").value("sach_011"));

        entityManager.flush();
        entityManager.clear();
        assertTrue(Boolean.TRUE.equals(donHangRepository.findById("dh_test_multi").orElseThrow().getDaDanhGia()));
    }

    // =========================================================================
    // TEST 2: KIỂM DUYỆT ĐÁNH GIÁ (ADMIN & CSKH) + PHÂN QUYỀN RBAC
    // =========================================================================

    @Test
    void adminVaCskhKiemDuyetAnHienDanhGia_tinhLaiSao_chanKhoVaCustomer() throws Exception {
        // WAREHOUSE_MANAGER & CUSTOMER bị chặn 403
        mockMvc.perform(get("/api/admin/reviews").header("Authorization", warehouseToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/reviews").header("Authorization", kh01Token))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/reviews/dg_01/toggle-hidden").header("Authorization", warehouseToken))
                .andExpect(status().isForbidden());

        // CSKH xem danh sách đánh giá (chứa dg_01 của sach_001)
        mockMvc.perform(get("/api/admin/reviews")
                        .param("keyword", "Alice")
                        .header("Authorization", cskhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem("dg_01")));

        // CSKH ẩn đánh giá dg_01 (sach_001 chỉ có 1 review công khai trong seed)
        mockMvc.perform(patch("/api/admin/reviews/dg_01/toggle-hidden")
                        .header("Authorization", cskhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("dg_01"))
                .andExpect(jsonPath("$.data.biAn").value(true));

        entityManager.flush();
        entityManager.clear();

        // Mobile GET /api/v1/books/sach_001/reviews không còn thấy dg_01; sach_001 về 0 lượt đánh giá
        mockMvc.perform(get("/api/v1/books/sach_001/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", not(hasItem("dg_01"))));

        Sach sach01Hidden = sachRepository.findById("sach_001").orElseThrow();
        assertEquals(0, sach01Hidden.getSoLuotDanhGia());

        // ADMIN hiện lại đánh giá dg_01 -> sach_001 khôi phục 1 lượt, 5.00 sao
        mockMvc.perform(patch("/api/admin/reviews/dg_01/toggle-hidden")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.biAn").value(false));

        entityManager.flush();
        entityManager.clear();

        Sach sach01Restored = sachRepository.findById("sach_001").orElseThrow();
        assertEquals(1, sach01Restored.getSoLuotDanhGia());
        assertEquals(0, new BigDecimal("5.00").compareTo(sach01Restored.getDiemDanhGiaTb()));
    }

    // =========================================================================
    // TEST 3: YÊU CẦU ĐỔI TRẢ (MOBILE) + TRẢ KÈM TRONG ORDER DTO + CHẶN LỖI
    // =========================================================================

    @Test
    void khachGuiYeuCauDoiTra_traKemTrongChiTietDon_chanTrungVaDonChuaGiao() throws Exception {
        // 1. nd_kh01 gửi yêu cầu đổi trả cho đơn dh_01 (DA_GIAO, chưa có yêu cầu đổi trả)
        String payload = """
                {
                  "donHangId": "dh_01",
                  "lyDo": "Sách bị rách trang 15",
                  "moTa": "Khi mở sách ra thấy trang 15-16 bị rách góc do lỗi đóng gáy",
                  "anhMinhChungUrls": ["https://covers.openlibrary.org/b/id/10527843-M.jpg"]
                }
                """;

        mockMvc.perform(post("/api/v1/returns")
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.donHangId").value("dh_01"))
                .andExpect(jsonPath("$.data.trangThai").value("CHO_DUYET"))
                .andExpect(jsonPath("$.data.lyDo").value("Sách bị rách trang 15"));

        // 2. Gửi trùng cho dh_01 -> 400 Bad Request
        mockMvc.perform(post("/api/v1/returns")
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("đã có yêu cầu đổi trả")));

        // 3. Kiểm tra GET /api/v1/orders/dh_01 và GET /api/admin/orders/dh_01 đều trả kèm returnRequest
        mockMvc.perform(get("/api/v1/orders/dh_01").header("Authorization", kh01Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.returnRequest", notNullValue()))
                .andExpect(jsonPath("$.data.returnRequest.trangThai").value("CHO_DUYET"));

        mockMvc.perform(get("/api/admin/orders/dh_01").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.returnRequest.lyDo").value("Sách bị rách trang 15"));

        // 4. Đơn chưa giao (dh_04 của nd_kh04) -> 400
        mockMvc.perform(post("/api/v1/returns")
                        .header("Authorization", kh04Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "donHangId": "dh_04",
                                  "lyDo": "Đổi ý",
                                  "moTa": "Chưa giao nhưng muốn đổi trả"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("đã giao thành công")));

        // 5. Khách khác (nd_kh02) cố gửi đổi trả hộ đơn dh_01 -> 404
        mockMvc.perform(post("/api/v1/returns")
                        .header("Authorization", kh02Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // TEST 4: DUYỆT ĐỔI TRẢ (ADMIN & CSKH) — ĐỔI MỚI KHÔNG ĐỔI KHO/THANH TOÁN
    // =========================================================================

    @Test
    void adminVaCskhDuyetDoiTra_khongDoiKhoVaThanhToan_guiThongBao_chanKhoVaDuyetLai() throws Exception {
        // WAREHOUSE_MANAGER & CUSTOMER bị chặn 403
        mockMvc.perform(get("/api/admin/orders/returns").header("Authorization", warehouseToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/orders/dh_08/return")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "trangThai": "DA_CHAP_THUAN",
                                  "ghiChuAdmin": "Kho thử duyệt"
                                }
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/orders/returns").header("Authorization", kh01Token))
                .andExpect(status().isForbidden());

        // CSKH xem danh sách yêu cầu đổi trả CHO_DUYET (có dt_01 của dh_08)
        mockMvc.perform(get("/api/admin/orders/returns")
                        .param("trangThai", "CHO_DUYET")
                        .header("Authorization", cskhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem("dt_01")));

        // Lưu tồn kho sach_008 và trạng thái thanh toán dh_08 trước khi duyệt
        int stockBefore = sachRepository.findById("sach_008").orElseThrow().getTonKho();
        TrangThaiThanhToan paymentBefore = donHangRepository.findById("dh_08").orElseThrow().getTrangThaiThanhToan();

        // CSKH phê duyệt DA_CHAP_THUAN cho dh_08
        mockMvc.perform(patch("/api/admin/orders/dh_08/return")
                        .header("Authorization", cskhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "trangThai": "DA_CHAP_THUAN",
                                  "ghiChuAdmin": "Shop sẽ gửi sách mới thay thế trong 2 ngày tới"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trangThai").value("DA_CHAP_THUAN"))
                .andExpect(jsonPath("$.data.ghiChuAdmin").value("Shop sẽ gửi sách mới thay thế trong 2 ngày tới"))
                .andExpect(jsonPath("$.data.ngayXuLy", notNullValue()));

        entityManager.flush();
        entityManager.clear();

        // Đổi mới: KHÔNG làm thay đổi tồn kho sach_008 và KHÔNG đổi trạng thái thanh toán dh_08
        assertEquals(stockBefore, sachRepository.findById("sach_008").orElseThrow().getTonKho());
        assertEquals(paymentBefore, donHangRepository.findById("dh_08").orElseThrow().getTrangThaiThanhToan());

        // Đã gửi ThongBao tới nd_kh10
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", kh10Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].tieuDe", hasItem(containsString("đã được chấp thuận"))));

        // Cố duyệt lại lần 2 -> 400 Bad Request
        mockMvc.perform(patch("/api/admin/orders/dh_08/return")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "trangThai": "TU_CHOI",
                                  "ghiChuAdmin": "Đổi ý từ chối"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("đã được xử lý")));
    }

    // =========================================================================
    // TEST 5: THÔNG BÁO ĐẨY (MOBILE) & PHÁT SÓNG THÔNG BÁO (ADMIN & CSKH)
    // =========================================================================

    @Test
    void phatSongThongBaoRiengTungKhach_docDocLap_khongSuaThongBaoChungCu() throws Exception {
        // WAREHOUSE_MANAGER & CUSTOMER bị chặn 403 khi gọi broadcast
        String broadcastPayload = """
                {
                  "tieuDe": "Siêu Sale Giữa Tháng",
                  "noiDung": "Nhập mã SACHHAY15 để nhận ưu đãi ngay hôm nay!",
                  "loaiThongBao": "KHUYEN_MAI"
                }
                """;
        mockMvc.perform(post("/api/admin/notifications/broadcast")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(broadcastPayload))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/notifications/broadcast")
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(broadcastPayload))
                .andExpect(status().isForbidden());

        // ADMIN phát sóng -> tạo 1 dòng riêng cho mỗi tài khoản CUSTOMER (>= 9 khách trong seed)
        mockMvc.perform(post("/api/admin/notifications/broadcast")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(broadcastPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.soKhachHangNhan", greaterThanOrEqualTo(9)));

        // nd_kh01 xem danh sách thông báo: có cả thông báo broadcast mới + tb_01 + tb_02 (chung cũ)
        MvcResult kh01ListRes = mockMvc.perform(get("/api/v1/notifications").header("Authorization", kh01Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem("tb_01")))
                .andExpect(jsonPath("$.data[*].id", hasItem("tb_02")))
                .andExpect(jsonPath("$.data[*].tieuDe", hasItem("Siêu Sale Giữa Tháng")))
                .andReturn();

        // Lấy ID bản ghi broadcast riêng của nd_kh01
        String kh01BroadcastId = JsonPath.read(
                kh01ListRes.getResponse().getContentAsString(),
                "$.data[?(@.tieuDe == 'Siêu Sale Giữa Tháng')].id"
        ).toString().replaceAll("[\\[\\]\"]", "");
        assertNotNull(kh01BroadcastId);

        // nd_kh01 đánh dấu đã đọc thông báo broadcast của mình
        mockMvc.perform(patch("/api/v1/notifications/" + kh01BroadcastId + "/read")
                        .header("Authorization", kh01Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.daDoc").value(true));

        // nd_kh02 vẫn thấy bản ghi broadcast của RIÊNG nd_kh02 ở trạng thái chưa đọc (daDoc = false)
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", kh02Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.tieuDe == 'Siêu Sale Giữa Tháng')].daDoc", hasItem(false)));

        // nd_kh01 bấm đọc thông báo chung cũ tb_02 -> trả về daDoc=true nhưng DB vẫn giữ FALSE để không ảnh hưởng khách khác
        mockMvc.perform(patch("/api/v1/notifications/tb_02/read").header("Authorization", kh01Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.daDoc").value(true));

        entityManager.flush();
        entityManager.clear();
        ThongBao tb02 = thongBaoRepository.findById("tb_02").orElseThrow();
        assertFalse(Boolean.TRUE.equals(tb02.getDaDoc()));

        // nd_kh01 cố đọc thông báo riêng tb_03 của nd_kh03 -> 404 Not Found (chống IDOR)
        mockMvc.perform(patch("/api/v1/notifications/tb_03/read").header("Authorization", kh01Token))
                .andExpect(status().isNotFound());

        // nd_kh02 bấm read-all -> đánh dấu toàn bộ thông báo cá nhân chưa đọc của nd_kh02 (>= 1)
        mockMvc.perform(patch("/api/v1/notifications/read-all").header("Authorization", kh02Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.soThongBaoDaDoc", greaterThanOrEqualTo(1)));
    }
}
