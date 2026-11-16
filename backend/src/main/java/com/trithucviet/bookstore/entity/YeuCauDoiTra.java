package com.trithucviet.bookstore.entity;

import com.trithucviet.bookstore.entity.enums.TrangThaiDoiTra;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "yeu_cau_doi_tra")
public class YeuCauDoiTra {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_id", nullable = false, unique = true)
    private DonHang donHang;

    @Column(name = "ly_do", length = 255, nullable = false)
    private String lyDo;

    @Column(name = "mo_ta", length = 1000, nullable = false)
    private String moTa;

    @Column(name = "anh_minh_chung_urls", columnDefinition = "TEXT", nullable = false)
    private String anhMinhChungUrls;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", length = 20, nullable = false)
    private TrangThaiDoiTra trangThai = TrangThaiDoiTra.CHO_DUYET;

    @Column(name = "ghi_chu_admin", length = 500)
    private String ghiChuAdmin;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_xu_ly")
    private LocalDateTime ngayXuLy;

    public YeuCauDoiTra() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DonHang getDonHang() {
        return donHang;
    }

    public void setDonHang(DonHang donHang) {
        this.donHang = donHang;
    }

    public String getLyDo() {
        return lyDo;
    }

    public void setLyDo(String lyDo) {
        this.lyDo = lyDo;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public String getAnhMinhChungUrls() {
        return anhMinhChungUrls;
    }

    public void setAnhMinhChungUrls(String anhMinhChungUrls) {
        this.anhMinhChungUrls = anhMinhChungUrls;
    }

    public TrangThaiDoiTra getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(TrangThaiDoiTra trangThai) {
        this.trangThai = trangThai;
    }

    public String getGhiChuAdmin() {
        return ghiChuAdmin;
    }

    public void setGhiChuAdmin(String ghiChuAdmin) {
        this.ghiChuAdmin = ghiChuAdmin;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }

    public LocalDateTime getNgayXuLy() {
        return ngayXuLy;
    }

    public void setNgayXuLy(LocalDateTime ngayXuLy) {
        this.ngayXuLy = ngayXuLy;
    }
}
