package com.trithucviet.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "chi_tiet_don_hang")
public class ChiTietDonHang {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_id", nullable = false)
    private DonHang donHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sach_id", nullable = false)
    private Sach sach;

    @Column(name = "tieu_de_sach", length = 255, nullable = false)
    private String tieuDeSach;

    @Column(name = "anh_bia_url", length = 500, nullable = false)
    private String anhBiaUrl;

    @Column(name = "ten_tac_gia", length = 150, nullable = false)
    private String tenTacGia;

    @Column(name = "don_gia", precision = 12, scale = 0, nullable = false)
    private BigDecimal donGia;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    public ChiTietDonHang() {
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

    public Sach getSach() {
        return sach;
    }

    public void setSach(Sach sach) {
        this.sach = sach;
    }

    public String getTieuDeSach() {
        return tieuDeSach;
    }

    public void setTieuDeSach(String tieuDeSach) {
        this.tieuDeSach = tieuDeSach;
    }

    public String getAnhBiaUrl() {
        return anhBiaUrl;
    }

    public void setAnhBiaUrl(String anhBiaUrl) {
        this.anhBiaUrl = anhBiaUrl;
    }

    public String getTenTacGia() {
        return tenTacGia;
    }

    public void setTenTacGia(String tenTacGia) {
        this.tenTacGia = tenTacGia;
    }

    public BigDecimal getDonGia() {
        return donGia;
    }

    public void setDonGia(BigDecimal donGia) {
        this.donGia = donGia;
    }

    public Integer getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(Integer soLuong) {
        this.soLuong = soLuong;
    }
}
