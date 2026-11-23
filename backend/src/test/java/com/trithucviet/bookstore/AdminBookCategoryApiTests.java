package com.trithucviet.bookstore;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.trithucviet.bookstore.config.JwtTokenProvider;
import com.trithucviet.bookstore.entity.enums.VaiTro;
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
 * Kiểm thử tích hợp API Quản trị Sách, Danh mục, Tác giả & NXB phía Web Admin (Bước 3 - Phần 3 & 4).
 * - @Transactional tự rollback mọi thao tác thêm/sửa/xóa trên bookstore_db.
 * - Kiểm tra đủ 4 vai trò: ADMIN (toàn quyền), WAREHOUSE_MANAGER (thêm/sửa thông tin nhưng chặn sửa giá/Flash Sale/xóa),
 *   CSKH (chỉ xem), CUSTOMER (chặn 403).
 */
@SpringBootTest
@Transactional
class AdminBookCategoryApiTests {

    private static final String BASE = "/api/admin";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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

    @Test
    void nhanSuAdminKhoCskh_xemDanhSachSachDanhMucTacGiaNxb_conCustomerBiChan() throws Exception {
        mockMvc.perform(get(BASE + "/books").header("Authorization", cskhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(186));

        mockMvc.perform(get(BASE + "/categories").header("Authorization", warehouseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(12)));

        mockMvc.perform(get(BASE + "/authors").header("Authorization", cskhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(10)));

        mockMvc.perform(get(BASE + "/publishers").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(3)));

        mockMvc.perform(get(BASE + "/books").header("Authorization", customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_taoSuaToggleHienThiVaXoaSachChuaCoDonHang_vaChanXoaSachDaCoDon() throws Exception {
        String createJson = """
                {
                  "maIsbn": "978-604-999-001-1",
                  "tieuDe": "Kiến trúc Microservices Thực chiến",
                  "danhMucId": "dm_cong_nghe",
                  "tacGiaId": "tg_001",
                  "nhaXuatBanId": "nxb_tre",
                  "giaBan": 220000,
                  "giaGoc": 260000,
                  "dangFlashSale": true,
                  "giaFlashSale": 185000,
                  "tonKho": 35,
                  "loaiBia": "Bìa mềm",
                  "hienThi": true
                }
                """;

        MvcResult created = mockMvc.perform(post(BASE + "/books")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tieuDe").value("Kiến trúc Microservices Thực chiến"))
                .andExpect(jsonPath("$.data.dangFlashSale").value(true))
                .andExpect(jsonPath("$.data.donGiaApDung").value(185000))
                .andExpect(jsonPath("$.data.hienThi").value(true))
                .andReturn();

        String newBookId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        // Toggle ẩn / hiện
        mockMvc.perform(patch(BASE + "/books/" + newBookId + "/visibility")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hienThi").value(false));

        // Sách mới chưa phát sinh đơn -> xóa thành công (200)
        mockMvc.perform(delete(BASE + "/books/" + newBookId).header("Authorization", adminToken))
                .andExpect(status().isOk());

        // Sách sach_001 đã có trong chi_tiet_don_hang -> chặn xóa cứng (400)
        mockMvc.perform(delete(BASE + "/books/sach_001").header("Authorization", adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void warehouseManager_suaThongTinVaTonKhoDuoc_nhungBiChanKhiSuaGiaFlashSaleVaXoa() throws Exception {
        // Sửa tồn kho & tiêu đề (giữ nguyên giá bán sach_001) -> 200 OK
        MvcResult detail = mockMvc.perform(get(BASE + "/books/sach_001").header("Authorization", warehouseToken))
                .andExpect(status().isOk())
                .andReturn();

        Number giaBan = JsonPath.read(detail.getResponse().getContentAsString(), "$.data.giaBan");
        Number giaGoc = JsonPath.read(detail.getResponse().getContentAsString(), "$.data.giaGoc");

        String allowedUpdate = """
                {
                  "tieuDe": "Alice's Adventures in Wonderland (Kho kiểm kê)",
                  "danhMucId": "dm_van_hoc",
                  "tacGiaId": "tg_001",
                  "nhaXuatBanId": "nxb_tre",
                  "giaBan": %s,
                  "giaGoc": %s,
                  "tonKho": 42
                }
                """.formatted(giaBan, giaGoc);

        mockMvc.perform(put(BASE + "/books/sach_001")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(allowedUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tonKho").value(42));

        // Cố tình sửa giá bán -> bị chặn 403 Forbidden
        String forbiddenPriceUpdate = """
                {
                  "tieuDe": "Alice's Adventures in Wonderland",
                  "danhMucId": "dm_van_hoc",
                  "tacGiaId": "tg_001",
                  "nhaXuatBanId": "nxb_tre",
                  "giaBan": 15000,
                  "giaGoc": %s,
                  "tonKho": 42
                }
                """.formatted(giaGoc);

        mockMvc.perform(put(BASE + "/books/sach_001")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forbiddenPriceUpdate))
                .andExpect(status().isForbidden());

        // Quản lý Kho & CSKH bị chặn toggle hiển thị và xóa sách
        mockMvc.perform(patch(BASE + "/books/sach_001/visibility").header("Authorization", warehouseToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "/books/sach_001").header("Authorization", warehouseToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/books")
                        .header("Authorization", cskhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tieuDe\":\"Test\",\"danhMucId\":\"dm_van_hoc\",\"giaBan\":100000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crudDanhMucTacGiaNxb_vaChanXoaKhiDangCoSachLienKet() throws Exception {
        // 1. Danh mục: Quản lý Kho tạo & sửa được; Admin xóa danh mục rỗng được; xóa danh mục có sách/con bị chặn 400
        MvcResult createdCat = mockMvc.perform(post(BASE + "/categories")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenDanhMuc\":\"Kỹ năng AI ứng dụng\",\"danhMucChaId\":\"dm_cong_nghe\",\"moTa\":\"Chuyên mục mới\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.danhMucChaId").value("dm_cong_nghe"))
                .andReturn();
        String newCatId = JsonPath.read(createdCat.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(put(BASE + "/categories/" + newCatId)
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenDanhMuc\":\"Kỹ năng AI thực hành\",\"danhMucChaId\":\"dm_cong_nghe\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenDanhMuc").value("Kỹ năng AI thực hành"));

        // Không được chọn chính nó làm danh mục cha
        mockMvc.perform(put(BASE + "/categories/" + newCatId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenDanhMuc\":\"Lỗi\",\"danhMucChaId\":\"" + newCatId + "\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete(BASE + "/categories/" + newCatId).header("Authorization", adminToken))
                .andExpect(status().isOk());

        // dm_van_hoc đang có danh mục con & sách -> chặn 400
        mockMvc.perform(delete(BASE + "/categories/dm_van_hoc").header("Authorization", adminToken))
                .andExpect(status().isBadRequest());

        // 2. Tác giả & NXB CRUD + chặn xóa khi đang có sách liên kết
        MvcResult createdAuthor = mockMvc.perform(post(BASE + "/authors")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenTacGia\":\"Tác giả Thử Nghiệm\",\"tieuSu\":\"Chuyên gia Java\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String newAuthorId = JsonPath.read(createdAuthor.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(delete(BASE + "/authors/" + newAuthorId).header("Authorization", adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(delete(BASE + "/authors/tg_001").header("Authorization", adminToken))
                .andExpect(status().isBadRequest());

        MvcResult createdPub = mockMvc.perform(post(BASE + "/publishers")
                        .header("Authorization", warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenNxb\":\"NXB Thử Nghiệm\",\"diaChi\":\"Quận 1, TP.HCM\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String newPubId = JsonPath.read(createdPub.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(delete(BASE + "/publishers/" + newPubId).header("Authorization", adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(delete(BASE + "/publishers/nxb_tre").header("Authorization", adminToken))
                .andExpect(status().isBadRequest());
    }
}
