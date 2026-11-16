package com.trithucviet.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "phieu_nhap_kho")
public class PhieuNhapKho {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @Column(name = "ma_phieu", length = 30, nullable = false, unique = true)
    private String maPhieu;

    @Column(name = "ten_nha_cung_cap", length = 200, nullable = false)
    private String tenNhaCungCap;

    @Column(name = "nguoi_tao", length = 100, nullable = false)
    private String nguoiTao;

    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;

    @Column(name = "tong_so_luong", nullable = false)
    private Integer tongSoLuong = 0;

    @Column(name = "tong_tien_nhap", precision = 15, scale = 0, nullable = false)
    private BigDecimal tongTienNhap = BigDecimal.ZERO;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    public PhieuNhapKho() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMaPhieu() {
        return maPhieu;
    }

    public void setMaPhieu(String maPhieu) {
        this.maPhieu = maPhieu;
    }

    public String getTenNhaCungCap() {
        return tenNhaCungCap;
    }

    public void setTenNhaCungCap(String tenNhaCungCap) {
        this.tenNhaCungCap = tenNhaCungCap;
    }

    public String getNguoiTao() {
        return nguoiTao;
    }

    public void setNguoiTao(String nguoiTao) {
        this.nguoiTao = nguoiTao;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public Integer getTongSoLuong() {
        return tongSoLuong;
    }

    public void setTongSoLuong(Integer tongSoLuong) {
        this.tongSoLuong = tongSoLuong;
    }

    public BigDecimal getTongTienNhap() {
        return tongTienNhap;
    }

    public void setTongTienNhap(BigDecimal tongTienNhap) {
        this.tongTienNhap = tongTienNhap;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }
}
