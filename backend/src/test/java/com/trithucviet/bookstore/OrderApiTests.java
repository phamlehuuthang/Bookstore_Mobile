package com.trithucviet.bookstore;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.trithucviet.bookstore.config.JwtTokenProvider;
import com.trithucviet.bookstore.entity.GioHang;
import com.trithucviet.bookstore.entity.MaGiamGia;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.entity.TkNguoiDung;
import com.trithucviet.bookstore.entity.enums.TrangThaiDon;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import com.trithucviet.bookstore.repository.DonHangRepository;
import com.trithucviet.bookstore.repository.GioHangRepository;
import com.trithucviet.bookstore.repository.MaGiamGiaRepository;
import com.trithucviet.bookstore.repository.SachRepository;
import com.trithucviet.bookstore.repository.TkNguoiDungRepository;
import com.trithucviet.bookstore.service.OrderService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
 * Kiểm thử tích hợp toàn bộ API Bước 6 (Đặt hàng, Theo dõi đơn + Timeline, Hủy đơn & Duyệt trạng thái Web Admin).
 * - @Transactional tự rollback toàn bộ đơn hàng, kho, voucher và giỏ hàng sau mỗi test case.
 * - Kiểm tra đầy đủ 4 vai trò: CUSTOMER, ADMIN, WAREHOUSE_MANAGER, CSKH.
 */
@SpringBootTest
@Transactional
class OrderApiTests {

    private static final String CLIENT_BASE = "/api/v1/orders";
    private static final String ADMIN_BASE = "/api/admin/orders";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SachRepository sachRepository;

    @Autowired
    private MaGiamGiaRepository maGiamGiaRepository;

    @Autowired
    private TkNguoiDungRepository tkNguoiDungRepository;

    @Autowired
    private GioHangRepository gioHangRepository;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private OrderService orderService;

    private MockMvc mockMvc;
    private String kh01Token;
    private String kh02Token;
    private String kh03Token;
    private String kh04Token;
    private String adminToken;
    private String warehouseToken;
    private String cskhToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        kh01Token = bearer("nd_kh01", "thang.nguyen@gmail.com", VaiTro.CUSTOMER);
        kh02Token = bearer("nd_kh02", "minhanh.tran@gmail.com", VaiTro.CUSTOMER);
        kh03Token = bearer("nd_kh03", "nam.le@gmail.com", VaiTro.CUSTOMER);
        kh04Token = bearer("nd_kh04", "ha.pham@gmail.com", VaiTro.CUSTOMER);
        adminToken = bearer("nd_admin", "admin@trithucviet.vn", VaiTro.ADMIN);
        warehouseToken = bearer("nd_kho", "kho@trithucviet.vn", VaiTro.WAREHOUSE_MANAGER);
        cskhToken = bearer("nd_cskh", "cskh@trithucviet.vn", VaiTro.CSKH);

        // Cô lập giỏ hàng nd_kh01: 2 cuốn được tick chọn (sach_011, sach_026) và 1 cuốn bỏ tick (sach_001)
        entityManager.createNativeQuery("DELETE FROM gio_hang WHERE nguoi_dung_id = 'nd_kh01'").executeUpdate();
        entityManager.createNativeQuery(
                "INSERT INTO gio_hang (nguoi_dung_id, sach_id, so_luong, da_chon) "
                        + "VALUES ('nd_kh01', 'sach_011', 1, TRUE), "
                        + "('nd_kh01', 'sach_026', 1, TRUE), "
                        + "('nd_kh01', 'sach_001', 1, FALSE)"
        ).executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private String bearer(String userId, String email, VaiTro vaiTro) {
        return "Bearer " + jwtTokenProvider.generateToken(userId, email, vaiTro);
    }

    @Test
    void khachDatHangThanhCong_coVoucher_truKho_xoaMonDaChon_taoTimelineVaThongBao() throws Exception {
        Sach sach11Before = sachRepository.findById("sach_011").orElseThrow();
        Sach sach26Before = sachRepository.findById("sach_026").orElseThrow();
        MaGiamGia voucherBefore = maGiamGiaRepository.findByMaCodeWithDanhMucIgnoreCase("SACHHAY15").orElseThrow();
        TkNguoiDung khBefore = tkNguoiDungRepository.findById("nd_kh01").orElseThrow();

        int s11StockBefore = sach11Before.getTonKho();
        int s11SoldBefore = sach11Before.getDaBan();
        int s26StockBefore = sach26Before.getTonKho();
        int s26SoldBefore = sach26Before.getDaBan();
        int voucherUsedBefore = voucherBefore.getDaSuDung();
        int khOrdersBefore = khBefore.getSoDonHang();
        BigDecimal khSpentBefore = khBefore.getTongChiTieu();

        // sach_011 (330k) + sach_026 (248k) = 578k (>= 300k freeship), SACHHAY15 giảm tối đa 50k -> 528k
        String payload = """
                {
                  "soDiaChiId": "dc_02",
                  "phuongThucThanhToan": "CHUYEN_KHOAN_QR",
                  "maGiamGiaCode": "sachhay15",
                  "ghiChu": "Gọi trước khi giao"
                }
                """;

        MvcResult result = mockMvc.perform(post(CLIENT_BASE)
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trangThaiDon").value("CHO_XAC_NHAN"))
                .andExpect(jsonPath("$.data.trangThaiThanhToan").value("DA_THANH_TOAN"))
                .andExpect(jsonPath("$.data.maGiamGiaCode").value("SACHHAY15"))
                .andExpect(jsonPath("$.data.diaChiGiaoHang", containsString("88 Pasteur")))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.timeline.length()").value(1))
                .andReturn();

        Number tongThanhToan = JsonPath.read(result.getResponse().getContentAsString(), "$.data.tongThanhToan");

        entityManager.flush();
        entityManager.clear();

        Sach sach11After = sachRepository.findById("sach_011").orElseThrow();
        Sach sach26After = sachRepository.findById("sach_026").orElseThrow();
        MaGiamGia voucherAfter = maGiamGiaRepository.findByMaCodeWithDanhMucIgnoreCase("SACHHAY15").orElseThrow();
        TkNguoiDung khAfter = tkNguoiDungRepository.findById("nd_kh01").orElseThrow();
        List<GioHang> remainingCart = gioHangRepository.findAllWithSachByNguoiDungId("nd_kh01");

        assertEquals(s11StockBefore - 1, sach11After.getTonKho());
        assertEquals(s11SoldBefore + 1, sach11After.getDaBan());
        assertEquals(s26StockBefore - 1, sach26After.getTonKho());
        assertEquals(s26SoldBefore + 1, sach26After.getDaBan());
        assertEquals(voucherUsedBefore + 1, voucherAfter.getDaSuDung());
        assertEquals(khOrdersBefore + 1, khAfter.getSoDonHang());
        assertEquals(0, khSpentBefore.add(new BigDecimal(tongThanhToan.toString())).compareTo(khAfter.getTongChiTieu()));

        // Chỉ 2 cuốn đã chọn bị xóa, cuốn sach_001 bỏ tick vẫn phải giữ nguyên trong giỏ
        assertEquals(1, remainingCart.size());
        assertEquals("sach_001", remainingCart.get(0).getSach().getId());
    }

    @Test
    void khachDatHang_cacTruongHopLoi_gioChonRong_diaChiNguoiKhac_vuotTonKho() throws Exception {
        // 1. Dùng soDiaChiId của nd_kh02 -> 404 chống IDOR
        mockMvc.perform(post(CLIENT_BASE)
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"soDiaChiId\":\"dc_03\",\"phuongThucThanhToan\":\"TIEN_MAT_COD\"}"))
                .andExpect(status().isNotFound());

        // 2. nd_kh03 trong seed chỉ có 1 món da_chon = FALSE -> 400 giỏ chọn rỗng
        mockMvc.perform(post(CLIENT_BASE)
                        .header("Authorization", kh03Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phuongThucThanhToan\":\"TIEN_MAT_COD\"}"))
                .andExpect(status().isBadRequest());

        // 3. Số lượng đặt vượt quá ton_kho -> 400
        Sach sach11 = sachRepository.findById("sach_011").orElseThrow();
        int overStock = sach11.getTonKho() + 50;
        entityManager.createNativeQuery(
                "UPDATE gio_hang SET so_luong = " + overStock + " WHERE nguoi_dung_id = 'nd_kh01' AND sach_id = 'sach_011'"
        ).executeUpdate();
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post(CLIENT_BASE)
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phuongThucThanhToan\":\"TIEN_MAT_COD\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void khachXemDonVaHuyDonChoXacNhan_hoanDuLieuDayDu_vaChanHuyKhiDangDongGoi() throws Exception {
        Sach sach11Before = sachRepository.findById("sach_011").orElseThrow();
        MaGiamGia voucherBefore = maGiamGiaRepository.findByMaCodeWithDanhMucIgnoreCase("SACHHAY15").orElseThrow();
        TkNguoiDung khBefore = tkNguoiDungRepository.findById("nd_kh01").orElseThrow();

        int stockBefore = sach11Before.getTonKho();
        int soldBefore = sach11Before.getDaBan();
        int voucherUsedBefore = voucherBefore.getDaSuDung();
        int ordersBefore = khBefore.getSoDonHang();
        BigDecimal spentBefore = khBefore.getTongChiTieu();

        MvcResult created = mockMvc.perform(post(CLIENT_BASE)
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phuongThucThanhToan\":\"VI_DIEN_TU\",\"maGiamGiaCode\":\"SACHHAY15\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String orderId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        // Xem danh sách & chi tiết đơn cá nhân
        mockMvc.perform(get(CLIENT_BASE)
                        .param("trangThai", "CHO_XAC_NHAN")
                        .header("Authorization", kh01Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(orderId)));

        // Khách khác xem chi tiết đơn này -> 404 chống IDOR
        mockMvc.perform(get(CLIENT_BASE + "/" + orderId).header("Authorization", kh02Token))
                .andExpect(status().isNotFound());

        // Khách hủy đơn khi đang CHO_XAC_NHAN -> hoàn kho, hoàn voucher, trừ lại chi tiêu/số đơn & DA_HOAN_TIEN
        mockMvc.perform(post(CLIENT_BASE + "/" + orderId + "/cancel")
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lyDo\":\"Tôi muốn đổi sang tựa sách khác\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trangThaiDon").value("DA_HUY"))
                .andExpect(jsonPath("$.data.trangThaiThanhToan").value("DA_HOAN_TIEN"))
                .andExpect(jsonPath("$.data.timeline.length()").value(2));

        entityManager.flush();
        entityManager.clear();

        Sach sach11After = sachRepository.findById("sach_011").orElseThrow();
        MaGiamGia voucherAfter = maGiamGiaRepository.findByMaCodeWithDanhMucIgnoreCase("SACHHAY15").orElseThrow();
        TkNguoiDung khAfter = tkNguoiDungRepository.findById("nd_kh01").orElseThrow();

        assertEquals(stockBefore, sach11After.getTonKho());
        assertEquals(soldBefore, sach11After.getDaBan());
        assertEquals(voucherUsedBefore, voucherAfter.getDaSuDung());
        assertEquals(ordersBefore, khAfter.getSoDonHang());
        assertEquals(0, spentBefore.compareTo(khAfter.getTongChiTieu()));

        // Đơn dh_04 của nd_kh04 đang DANG_DONG_GOI -> khách tự hủy bị chặn 400
        mockMvc.perform(post(CLIENT_BASE + "/dh_04/cancel")
                        .header("Authorization", kh04Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lyDo\":\"Không muốn nhận nữa\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminKhoCskh_chuyenTrangThai1Chieu_vaKiemTraRbacChinhXac() throws Exception {
        // Chỉ chọn sach_026 (248k < 300k -> ship 25k) + CHAOBAN20 (giảm 20k) = 253k, thanh toán COD (CHUA_THANH_TOAN)
        entityManager.createNativeQuery("UPDATE gio_hang SET da_chon = FALSE WHERE nguoi_dung_id = 'nd_kh01' AND sach_id = 'sach_011'")
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        MvcResult created = mockMvc.perform(post(CLIENT_BASE)
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phuongThucThanhToan\":\"TIEN_MAT_COD\",\"maGiamGiaCode\":\"CHAOBAN20\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phiVanChuyen").value(25000))
                .andExpect(jsonPath("$.data.tienGiamGia").value(20000))
                .andExpect(jsonPath("$.data.trangThaiThanhToan").value("CHUA_THANH_TOAN"))
                .andReturn();

        String orderId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        // CUSTOMER bị chặn gọi cổng Admin
        mockMvc.perform(get(ADMIN_BASE).header("Authorization", kh01Token))
                .andExpect(status().isForbidden());

        // Cố nhảy cóc CHO_XAC_NHAN -> DANG_GIAO -> 400
        mockMvc.perform(patch(ADMIN_BASE + "/" + orderId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThaiMoi\":\"DANG_GIAO\"}"))
                .andExpect(status().isBadRequest());

        // 1. CSKH xác nhận đơn: CHO_XAC_NHAN -> DANG_DONG_GOI (200)
        mockMvc.perform(patch(ADMIN_BASE + "/" + orderId + "/status")
                        .header("Authorization", cskhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThaiMoi\":\"DANG_DONG_GOI\",\"ghiChu\":\"CSKH đã xác nhận qua điện thoại\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trangThaiDon").value("DANG_DONG_GOI"));

        // 2. CSKH cố tự chuyển sang DANG_GIAO -> bị chặn 403
        mockMvc.perform(patch(ADMIN_BASE + "/" + orderId + "/status")
                        .header("Authorization", cskhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThaiMoi\":\"DANG_GIAO\"}"))
                .andExpect(status().isForbidden());

        // 3. WAREHOUSE_MANAGER xuất kho: DANG_DONG_GOI -> DANG_GIAO (200)
        mockMvc.perform(patch(ADMIN_BASE + "/" + orderId + "/status")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThaiMoi\":\"DANG_GIAO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trangThaiDon").value("DANG_GIAO"));

        // 4. WAREHOUSE_MANAGER cố xác nhận DA_GIAO -> bị chặn 403
        mockMvc.perform(patch(ADMIN_BASE + "/" + orderId + "/status")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThaiMoi\":\"DA_GIAO\"}"))
                .andExpect(status().isForbidden());

        // 5. CSKH (hoặc ADMIN) xác nhận đã giao: DANG_GIAO -> DA_GIAO & COD tự thành DA_THANH_TOAN
        mockMvc.perform(patch(ADMIN_BASE + "/" + orderId + "/status")
                        .header("Authorization", cskhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThaiMoi\":\"DA_GIAO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trangThaiDon").value("DA_GIAO"))
                .andExpect(jsonPath("$.data.trangThaiThanhToan").value("DA_THANH_TOAN"))
                .andExpect(jsonPath("$.data.timeline.length()").value(4));

        // 6. Đơn đã DA_GIAO thì không thể quay ngược hoặc hủy
        mockMvc.perform(patch(ADMIN_BASE + "/" + orderId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThaiMoi\":\"DA_HUY\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminVaCskh_huyDonKhiDangDongGoi_hoanDuLieu_vaChanKhoHuyDon() throws Exception {
        Sach sach11Before = sachRepository.findById("sach_011").orElseThrow();
        int stockBefore = sach11Before.getTonKho();

        MvcResult created = mockMvc.perform(post(CLIENT_BASE)
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phuongThucThanhToan\":\"CHUYEN_KHOAN_QR\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String orderId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        // Kho chuyển sang DANG_DONG_GOI
        mockMvc.perform(patch(ADMIN_BASE + "/" + orderId + "/status")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThaiMoi\":\"DANG_DONG_GOI\"}"))
                .andExpect(status().isOk());

        // Kho cố hủy đơn -> bị chặn 403
        mockMvc.perform(patch(ADMIN_BASE + "/" + orderId + "/status")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThaiMoi\":\"DA_HUY\"}"))
                .andExpect(status().isForbidden());

        // CSKH hủy đơn khi đang DANG_DONG_GOI -> 200 OK và hoàn kho + hoàn tiền
        mockMvc.perform(patch(ADMIN_BASE + "/" + orderId + "/status")
                        .header("Authorization", cskhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThaiMoi\":\"DA_HUY\",\"ghiChu\":\"Khách gọi tổng đài xin hủy đơn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trangThaiDon").value("DA_HUY"))
                .andExpect(jsonPath("$.data.trangThaiThanhToan").value("DA_HOAN_TIEN"));

        entityManager.flush();
        entityManager.clear();

        assertEquals(stockBefore, sachRepository.findById("sach_011").orElseThrow().getTonKho());

        // Đơn dh_03 trong seed đang DANG_GIAO -> kể cả ADMIN cũng bị chặn hủy (400)
        mockMvc.perform(patch(ADMIN_BASE + "/dh_03/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThaiMoi\":\"DA_HUY\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void khachXacNhanDaNhanDuocHang_chuyenDaGiaoVaThanhToanCod_vaChanSaiTrangThaiHoacIdor() throws Exception {
        // Tạo đơn COD cho nd_kh01 (ban đầu CHO_XAC_NHAN, CHUA_THANH_TOAN)
        MvcResult created = mockMvc.perform(post(CLIENT_BASE)
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phuongThucThanhToan\":\"TIEN_MAT_COD\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String orderId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        // Đơn đang CHO_XAC_NHAN -> chưa thể bấm "Đã nhận được hàng" (400)
        mockMvc.perform(post(CLIENT_BASE + "/" + orderId + "/confirm-received")
                        .header("Authorization", kh01Token))
                .andExpect(status().isBadRequest());

        // Kho đóng gói & xuất kho sang DANG_GIAO
        mockMvc.perform(patch(ADMIN_BASE + "/" + orderId + "/status")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThaiMoi\":\"DANG_DONG_GOI\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch(ADMIN_BASE + "/" + orderId + "/status")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThaiMoi\":\"DANG_GIAO\"}"))
                .andExpect(status().isOk());

        // Khách khác cố bấm xác nhận đơn này -> 404 chống IDOR
        mockMvc.perform(post(CLIENT_BASE + "/" + orderId + "/confirm-received")
                        .header("Authorization", kh02Token))
                .andExpect(status().isNotFound());

        // Chính chủ nd_kh01 bấm "Đã nhận được hàng" -> DA_GIAO + DA_THANH_TOAN, Admin không cần bấm tay
        mockMvc.perform(post(CLIENT_BASE + "/" + orderId + "/confirm-received")
                        .header("Authorization", kh01Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trangThaiDon").value("DA_GIAO"))
                .andExpect(jsonPath("$.data.trangThaiThanhToan").value("DA_THANH_TOAN"))
                .andExpect(jsonPath("$.data.timeline.length()").value(4));
    }

    @Test
    void tuDongChotDonDangGiaoQuaHan_khongAnhHuongDonMoiGiao() throws Exception {
        // Tạo 1 đơn mới và đưa sang DANG_GIAO tại thời điểm hiện tại (chưa quá hạn)
        MvcResult created = mockMvc.perform(post(CLIENT_BASE)
                        .header("Authorization", kh01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phuongThucThanhToan\":\"TIEN_MAT_COD\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String freshOrderId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(patch(ADMIN_BASE + "/" + freshOrderId + "/status")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThaiMoi\":\"DANG_DONG_GOI\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch(ADMIN_BASE + "/" + freshOrderId + "/status")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThaiMoi\":\"DANG_GIAO\"}"))
                .andExpect(status().isOk());

        // Lần quét 1 với ngưỡng 4320 phút (3 ngày): đơn mới giao chưa quá hạn phải giữ nguyên DANG_GIAO
        orderService.autoCompleteOverdueShippingOrders(4320);
        entityManager.flush();
        entityManager.clear();
        assertEquals(TrangThaiDon.DANG_GIAO, donHangRepository.findById(freshOrderId).orElseThrow().getTrangThaiDon());

        // Lùi mốc DANG_GIAO của freshOrderId về 5000 phút trước để giả lập đơn quá hạn 3 ngày
        String overdueTime = LocalDateTime.now().minusMinutes(5000)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        entityManager.createNativeQuery(
                "UPDATE lich_su_don_hang SET thoi_gian = :ts WHERE don_hang_id = :oid AND trang_thai = 'DANG_GIAO'"
        )
                .setParameter("ts", overdueTime)
                .setParameter("oid", freshOrderId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        // Lần quét 2: đơn quá hạn tự động chuyển sang DA_GIAO
        orderService.autoCompleteOverdueShippingOrders(4320);
        entityManager.flush();
        entityManager.clear();
        assertEquals(TrangThaiDon.DA_GIAO, donHangRepository.findById(freshOrderId).orElseThrow().getTrangThaiDon());
    }
}
