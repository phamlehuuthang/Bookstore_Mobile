package com.trithucviet.bookstore.entity;

import com.trithucviet.bookstore.entity.enums.VaiTro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tk_nguoi_dung")
public class TkNguoiDung {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @Column(name = "ho_ten", length = 120, nullable = false)
    private String hoTen;

    @Column(name = "email", length = 120, nullable = false, unique = true)
    private String email;

    @Column(name = "mat_khau_ma_hoa", length = 255, nullable = false)
    private String matKhauMaHoa;

    @Column(name = "so_dien_thoai", length = 20, nullable = false)
    private String soDienThoai;

    @Enumerated(EnumType.STRING)
    @Column(name = "vai_tro", length = 30, nullable = false)
    private VaiTro vaiTro = VaiTro.CUSTOMER;

    @Column(name = "bi_khoa", nullable = false)
    private Boolean biKhoa = false;

    @Column(name = "tong_chi_tieu", precision = 15, scale = 0, nullable = false)
    private BigDecimal tongChiTieu = BigDecimal.ZERO;

    @Column(name = "so_don_hang", nullable = false)
    private Integer soDonHang = 0;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    public TkNguoiDung() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMatKhauMaHoa() {
        return matKhauMaHoa;
    }

    public void setMatKhauMaHoa(String matKhauMaHoa) {
        this.matKhauMaHoa = matKhauMaHoa;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public VaiTro getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(VaiTro vaiTro) {
        this.vaiTro = vaiTro;
    }

    public Boolean getBiKhoa() {
        return biKhoa;
    }

    public void setBiKhoa(Boolean biKhoa) {
        this.biKhoa = biKhoa;
    }

    public BigDecimal getTongChiTieu() {
        return tongChiTieu;
    }

    public void setTongChiTieu(BigDecimal tongChiTieu) {
        this.tongChiTieu = tongChiTieu;
    }

    public Integer getSoDonHang() {
        return soDonHang;
    }

    public void setSoDonHang(Integer soDonHang) {
        this.soDonHang = soDonHang;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }
}
