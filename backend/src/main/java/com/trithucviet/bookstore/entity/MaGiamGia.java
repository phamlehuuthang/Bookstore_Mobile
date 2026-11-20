package com.trithucviet.bookstore.entity;

import com.trithucviet.bookstore.entity.enums.LoaiGiam;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ma_giam_gia")
public class MaGiamGia {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @Column(name = "ma_code", length = 30, nullable = false, unique = true)
    private String maCode;

    @Column(name = "tieu_de", length = 150, nullable = false)
    private String tieuDe;

    @Column(name = "mo_ta", length = 500)
    private String moTa;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_giam", length = 20, nullable = false)
    private LoaiGiam loaiGiam;

    @Column(name = "gia_tri_giam", precision = 12, scale = 0, nullable = false)
    private BigDecimal giaTriGiam;

    @Column(name = "giam_toi_da", precision = 12, scale = 0)
    private BigDecimal giamToiDa;

    @Column(name = "don_toi_thieu", precision = 12, scale = 0, nullable = false)
    private BigDecimal donToiThieu = BigDecimal.ZERO;

    // NULL = áp dụng toàn cửa hàng; có giá trị = chỉ áp dụng cho sách thuộc danh mục này (hoặc danh mục con)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "danh_muc_id")
    private DanhMuc danhMuc;

    @Column(name = "gioi_han_luot", nullable = false)
    private Integer gioiHanLuot;

    @Column(name = "da_su_dung", nullable = false)
    private Integer daSuDung = 0;

    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDate ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDate ngayKetThuc;

    @Column(name = "dang_hoat_dong", nullable = false)
    private Boolean dangHoatDong = true;

    public MaGiamGia() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMaCode() {
        return maCode;
    }

    public void setMaCode(String maCode) {
        this.maCode = maCode;
    }

    public String getTieuDe() {
        return tieuDe;
    }

    public void setTieuDe(String tieuDe) {
        this.tieuDe = tieuDe;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public LoaiGiam getLoaiGiam() {
        return loaiGiam;
    }

    public void setLoaiGiam(LoaiGiam loaiGiam) {
        this.loaiGiam = loaiGiam;
    }

    public BigDecimal getGiaTriGiam() {
        return giaTriGiam;
    }

    public void setGiaTriGiam(BigDecimal giaTriGiam) {
        this.giaTriGiam = giaTriGiam;
    }

    public BigDecimal getGiamToiDa() {
        return giamToiDa;
    }

    public void setGiamToiDa(BigDecimal giamToiDa) {
        this.giamToiDa = giamToiDa;
    }

    public BigDecimal getDonToiThieu() {
        return donToiThieu;
    }

    public void setDonToiThieu(BigDecimal donToiThieu) {
        this.donToiThieu = donToiThieu;
    }

    public DanhMuc getDanhMuc() {
        return danhMuc;
    }

    public void setDanhMuc(DanhMuc danhMuc) {
        this.danhMuc = danhMuc;
    }

    public Integer getGioiHanLuot() {
        return gioiHanLuot;
    }

    public void setGioiHanLuot(Integer gioiHanLuot) {
        this.gioiHanLuot = gioiHanLuot;
    }

    public Integer getDaSuDung() {
        return daSuDung;
    }

    public void setDaSuDung(Integer daSuDung) {
        this.daSuDung = daSuDung;
    }

    public LocalDate getNgayBatDau() {
        return ngayBatDau;
    }

    public void setNgayBatDau(LocalDate ngayBatDau) {
        this.ngayBatDau = ngayBatDau;
    }

    public LocalDate getNgayKetThuc() {
        return ngayKetThuc;
    }

    public void setNgayKetThuc(LocalDate ngayKetThuc) {
        this.ngayKetThuc = ngayKetThuc;
    }

    public Boolean getDangHoatDong() {
        return dangHoatDong;
    }

    public void setDangHoatDong(Boolean dangHoatDong) {
        this.dangHoatDong = dangHoatDong;
    }
}
