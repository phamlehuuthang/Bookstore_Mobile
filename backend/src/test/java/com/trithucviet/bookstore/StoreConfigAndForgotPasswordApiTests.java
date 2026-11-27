package com.trithucviet.bookstore;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trithucviet.bookstore.config.JwtTokenProvider;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

@SpringBootTest
@Transactional
class StoreConfigAndForgotPasswordApiTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;
    private String adminToken;
    private String cskhToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        adminToken = "Bearer " + jwtTokenProvider.generateToken("nd_admin", "admin@trithucviet.vn", VaiTro.ADMIN);
        cskhToken = "Bearer " + jwtTokenProvider.generateToken("nd_cskh", "cskh@trithucviet.vn", VaiTro.CSKH);
    }

    @Test
    void storeConfig_publicAndAdminEndpoints_shouldWorkWithRbac() throws Exception {
        mockMvc.perform(get("/api/v1/store-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenCuaHang", notNullValue()))
                .andExpect(jsonPath("$.data.nganHangSoTk", notNullValue()));

        String updatePayload = """
                {
                  "tenCuaHang": "Nhà Sách Tri Thức Việt Test",
                  "khauHieu": "Đọc sách mỗi ngày",
                  "logoUrl": "https://example.com/logo.png",
                  "hotline": "1900 9999",
                  "emailHoTro": "support@trithucviet.vn",
                  "diaChiTruSo": "Hà Nội",
                  "phiShipMacDinh": 30000,
                  "mienPhiShipTu": 350000,
                  "nguongCanhBaoTonKho": 8,
                  "nganHangTen": "MBBank",
                  "nganHangSoTk": "090123456789",
                  "nganHangChuTk": "TRI THUC VIET"
                }
                """;

        mockMvc.perform(put("/api/admin/settings")
                        .header("Authorization", cskhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/settings")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenCuaHang").value("Nhà Sách Tri Thức Việt Test"))
                .andExpect(jsonPath("$.data.nganHangTen").value("MBBank"));
    }

    @Test
    void forgotAndResetPassword_shouldVerifyOtpAndUpdatePassword() throws Exception {
        MvcResult otpResult = mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"thang.nguyen@gmail.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.demoOtp", notNullValue()))
                .andReturn();

        String body = otpResult.getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("\"demoOtp\"\\s*:\\s*\"(\\d{6})\"").matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("Không tìm thấy demoOtp trong phản hồi: " + body);
        }
        String otp = matcher.group(1);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"thang.nguyen@gmail.com","otp":"000000","matKhauMoi":"newpass123"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"thang.nguyen@gmail.com","otp":"%s","matKhauMoi":"newpass123"}
                                """.formatted(otp)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"thang.nguyen@gmail.com\",\"matKhau\":\"newpass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));
    }

    @Test
    void exportOrdersAndInventoryCsv_shouldRequireAdminAndReturnCsv() throws Exception {
        mockMvc.perform(get("/api/admin/reports/orders-export").header("Authorization", cskhToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/reports/orders-export").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("Bao_Cao_Don_Hang_BookStore_")))
                .andExpect(content().string(containsString("Ma don hang,Ngay tao")));

        mockMvc.perform(get("/api/admin/reports/inventory-export").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("Bao_Cao_Ton_Kho_BookStore_")))
                .andExpect(content().string(containsString("Ma ISBN,Ten sach")));
    }
}
