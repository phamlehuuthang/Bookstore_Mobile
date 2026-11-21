package com.trithucviet.bookstore;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trithucviet.bookstore.config.JwtTokenProvider;
import com.trithucviet.bookstore.entity.enums.VaiTro;
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
 * Kiểm thử tích hợp toàn bộ API Quản lý Voucher phía Web Admin (Bước 4 - phần 3) trên bookstore_db.
 * - @Transactional tự rollback mọi thao tác tạo/sửa/toggle nên không làm thay đổi dữ liệu mẫu.
 * - Kiểm tra đủ 4 vai trò: ADMIN (toàn quyền), CSKH (chỉ xem), WAREHOUSE_MANAGER (chặn 403), CUSTOMER (chặn 403).
 */
@SpringBootTest
@Transactional
class AdminVoucherApiTests {

    private static final String BASE = "/api/admin/vouchers";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;
    private String adminToken;
    private String cskhToken;
    private String warehouseToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        adminToken = bearer("nd_admin", "admin@trithucviet.vn", VaiTro.ADMIN);
        cskhToken = bearer("nd_cskh", "cskh@trithucviet.vn", VaiTro.CSKH);
        warehouseToken = bearer("nd_kho", "kho@trithucviet.vn", VaiTro.WAREHOUSE_MANAGER);
        customerToken = bearer("nd_kh01", "thang.nguyen@gmail.com", VaiTro.CUSTOMER);
    }

    private String bearer(String userId, String email, VaiTro vaiTro) {
        return "Bearer " + jwtTokenProvider.generateToken(userId, email, vaiTro);
    }

    // --- Nhóm 1: Xem & lọc danh sách, xem chi tiết (ADMIN và CSKH đều xem được) ---

    @Test
    void adminVaCskh_xemDanhSachVaLocVoucher_thanhCong() throws Exception {
        // DB mẫu có 6 voucher (5 đang mở + 1 đã tắt HETHAN10)
        mockMvc.perform(get(BASE).header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[*].maCode", hasItem("HETHAN10")));

        // CSKH xem được danh sách & lọc theo trạng thái tắt + từ khóa
        mockMvc.perform(get(BASE)
                        .param("dangHoatDong", "false")
                        .param("keyword", "hethan")
                        .header("Authorization", cskhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].maCode").value("HETHAN10"));

        // Xem chi tiết mgg_04 (CONGNGHE25)
        mockMvc.perform(get(BASE + "/mgg_04").header("Authorization", cskhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maCode").value("CONGNGHE25"))
                .andExpect(jsonPath("$.data.danhMucId").value("dm_cong_nghe"));

        mockMvc.perform(get(BASE + "/mgg_999").header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    // --- Nhóm 2: Tạo mới, Sửa, Bật/Tắt kích hoạt (chỉ ADMIN) ---

    @Test
    void admin_taoSuaVaToggleVoucher_thanhCong() throws Exception {
        String createJson = """
                {
                  "maCode": "trithuc40k",
                  "tieuDe": "Giảm 40.000đ cho sách Công nghệ",
                  "moTa": "Áp dụng đơn từ 250.000đ",
                  "loaiGiam": "CO_DINH",
                  "giaTriGiam": 40000,
                  "donToiThieu": 250000,
                  "danhMucId": "dm_cong_nghe",
                  "gioiHanLuot": 200,
                  "ngayKetThuc": "2026-12-31"
                }
                """;

        mockMvc.perform(post(BASE)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maCode").value("TRITHUC40K"))
                .andExpect(jsonPath("$.data.daSuDung").value(0))
                .andExpect(jsonPath("$.data.dangHoatDong").value(true))
                .andExpect(jsonPath("$.data.danhMucId").value("dm_cong_nghe"));

        // Gửi trùng mã -> báo 400
        mockMvc.perform(post(BASE)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mã giảm giá TRITHUC40K đã tồn tại trong hệ thống."));

        // Sửa voucher mẫu mgg_01 (giữ nguyên maCode CHAOBAN20 vì đã có 120 lượt dùng)
        String updateJson = """
                {
                  "maCode": "CHAOBAN20",
                  "tieuDe": "Giảm 25.000đ cho bạn mới (đã cập nhật)",
                  "moTa": "Áp dụng đơn từ 150.000đ",
                  "loaiGiam": "CO_DINH",
                  "giaTriGiam": 25000,
                  "donToiThieu": 150000,
                  "gioiHanLuot": 600,
                  "ngayBatDau": "2026-01-01",
                  "ngayKetThuc": "2026-12-31",
                  "dangHoatDong": true
                }
                """;

        mockMvc.perform(put(BASE + "/mgg_01")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tieuDe").value("Giảm 25.000đ cho bạn mới (đã cập nhật)"))
                .andExpect(jsonPath("$.data.giaTriGiam").value(25000))
                .andExpect(jsonPath("$.data.gioiHanLuot").value(600));

        // Bật / tạm ngưng nhanh mgg_01
        mockMvc.perform(patch(BASE + "/mgg_01/toggle").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã tạm ngưng mã giảm giá"))
                .andExpect(jsonPath("$.data.dangHoatDong").value(false));

        mockMvc.perform(patch(BASE + "/mgg_01/toggle").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã kích hoạt mã giảm giá"))
                .andExpect(jsonPath("$.data.dangHoatDong").value(true));
    }

    // --- Nhóm 3: Kiểm tra ràng buộc nghiệp vụ 400 / 404 ---

    @Test
    void taoVaSuaViPhamRangBuoc_tra400Hoac404() throws Exception {
        // Phần trăm > 100 -> 400
        String over100Json = """
                {
                  "maCode": "LOIPHANTRAM",
                  "tieuDe": "Giảm 150%",
                  "loaiGiam": "PHAN_TRAM",
                  "giaTriGiam": 150,
                  "gioiHanLuot": 50,
                  "ngayKetThuc": "2026-12-31"
                }
                """;
        mockMvc.perform(post(BASE)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(over100Json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Với loại giảm theo phần trăm, giá trị giảm phải nằm trong khoảng từ 1 đến 100."));

        // Ngày kết thúc trước ngày bắt đầu -> 400
        String badDateJson = """
                {
                  "maCode": "LOINGAY",
                  "tieuDe": "Ngày sai",
                  "loaiGiam": "CO_DINH",
                  "giaTriGiam": 20000,
                  "gioiHanLuot": 50,
                  "ngayBatDau": "2026-12-31",
                  "ngayKetThuc": "2026-12-01"
                }
                """;
        mockMvc.perform(post(BASE)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badDateJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ngày kết thúc không được trước ngày bắt đầu."));

        // danhMucId không tồn tại -> 404
        String badCategoryJson = """
                {
                  "maCode": "LOIDANHMUC",
                  "tieuDe": "Danh mục sai",
                  "loaiGiam": "CO_DINH",
                  "giaTriGiam": 20000,
                  "danhMucId": "dm_khong_ton_tai",
                  "gioiHanLuot": 50,
                  "ngayKetThuc": "2026-12-31"
                }
                """;
        mockMvc.perform(post(BASE)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badCategoryJson))
                .andExpect(status().isNotFound());

        // Chặn đổi maCode của voucher đã có lượt dùng (mgg_01 daSuDung=120) -> 400
        String renameUsedJson = """
                {
                  "maCode": "TENMOI20",
                  "tieuDe": "Thử đổi tên mã đã dùng",
                  "loaiGiam": "CO_DINH",
                  "giaTriGiam": 20000,
                  "gioiHanLuot": 500,
                  "ngayBatDau": "2026-01-01",
                  "ngayKetThuc": "2026-12-31"
                }
                """;
        mockMvc.perform(put(BASE + "/mgg_01")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(renameUsedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không thể đổi mã code của voucher đã có đơn hàng sử dụng. Hãy tạo mã mới hoặc tạm ngưng mã này."));

        // Chặn giảm gioiHanLuot thấp hơn daSuDung (mgg_01 daSuDung=120) -> 400
        String lowerLimitJson = """
                {
                  "maCode": "CHAOBAN20",
                  "tieuDe": "Giảm giới hạn lượt thấp hơn đã dùng",
                  "loaiGiam": "CO_DINH",
                  "giaTriGiam": 20000,
                  "gioiHanLuot": 50,
                  "ngayBatDau": "2026-01-01",
                  "ngayKetThuc": "2026-12-31"
                }
                """;
        mockMvc.perform(put(BASE + "/mgg_01")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lowerLimitJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Giới hạn lượt dùng (50) không được nhỏ hơn số lượt đã sử dụng (120)."));
    }

    // --- Nhóm 4: Bảo mật & Phân quyền theo PHAN_QUYEN_WEB_ADMIN.md (401 / 403) ---

    @Test
    void phanQuyenAdminVoucher_chanDungVaiTro() throws Exception {
        String sampleJson = """
                {
                  "maCode": "TESTROLE",
                  "tieuDe": "Test quyền",
                  "loaiGiam": "CO_DINH",
                  "giaTriGiam": 10000,
                  "gioiHanLuot": 10,
                  "ngayKetThuc": "2026-12-31"
                }
                """;

        // Không có token -> 401
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        // CUSTOMER bị chặn toàn bộ cổng /api/admin/** -> 403
        mockMvc.perform(get(BASE).header("Authorization", customerToken)).andExpect(status().isForbidden());

        // WAREHOUSE_MANAGER bị chặn toàn bộ phần Voucher -> 403
        mockMvc.perform(get(BASE).header("Authorization", warehouseToken)).andExpect(status().isForbidden());
        mockMvc.perform(post(BASE)
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleJson))
                .andExpect(status().isForbidden());

        // CSKH chỉ được GET, bị chặn 403 khi Tạo / Sửa / Bật-Tắt
        mockMvc.perform(post(BASE)
                        .header("Authorization", cskhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleJson))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/mgg_01")
                        .header("Authorization", cskhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleJson))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(BASE + "/mgg_01/toggle").header("Authorization", cskhToken))
                .andExpect(status().isForbidden());
    }
}
