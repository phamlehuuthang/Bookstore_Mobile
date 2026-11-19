package com.trithucviet.bookstore;

import com.trithucviet.bookstore.config.JwtTokenProvider;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import com.trithucviet.bookstore.repository.DanhMucRepository;
import com.trithucviet.bookstore.repository.DonHangRepository;
import com.trithucviet.bookstore.repository.HanhViNguoiDungRepository;
import com.trithucviet.bookstore.repository.SachRepository;
import com.trithucviet.bookstore.repository.TkNguoiDungRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BookstoreApplicationTests {

    @Autowired
    private SachRepository sachRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private TkNguoiDungRepository tkNguoiDungRepository;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private HanhViNguoiDungRepository hanhViNguoiDungRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test

    void contextLoadsAndDatabaseMatchesSeedData() {
        assertThat(sachRepository.count()).isEqualTo(186);
        assertThat(danhMucRepository.count()).isEqualTo(12);
        assertThat(tkNguoiDungRepository.count()).isEqualTo(19);
        assertThat(donHangRepository.count()).isEqualTo(8);
        assertThat(hanhViNguoiDungRepository.count()).isEqualTo(8);
    }

    @Test

    void jwtTokenProviderGeneratesAndParsesTokenCorrectly() {
        String token = jwtTokenProvider.generateToken("nd_admin", "admin@trithucviet.vn", VaiTro.ADMIN);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo("nd_admin");
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("admin@trithucviet.vn");
        assertThat(jwtTokenProvider.getVaiTroFromToken(token)).isEqualTo("ADMIN");
    }
}
