package com.trithucviet.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "danh_gia")
public class DanhGia {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sach_id", nullable = false)
    private Sach sach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private TkNguoiDung nguoiDung;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_id")
    private DonHang donHang;

    @Column(name = "so_sao", nullable = false)
    private Integer soSao;

    @Column(name = "binh_luan", length = 1000, nullable = false)
    private String binhLuan;

    @Column(name = "anh_danh_gia_urls", columnDefinition = "TEXT")
    private String anhDanhGiaUrls;

    @Column(name = "bi_an", nullable = false)
    private Boolean biAn = false;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    public DanhGia() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Sach getSach() {
        return sach;
    }

    public void setSach(Sach sach) {
        this.sach = sach;
    }

    public TkNguoiDung getNguoiDung() {
        return nguoiDung;
    }

    public void setNguoiDung(TkNguoiDung nguoiDung) {
        this.nguoiDung = nguoiDung;
    }

    public DonHang getDonHang() {
        return donHang;
    }

    public void setDonHang(DonHang donHang) {
        this.donHang = donHang;
    }

    public Integer getSoSao() {
        return soSao;
    }

    public void setSoSao(Integer soSao) {
        this.soSao = soSao;
    }

    public String getBinhLuan() {
        return binhLuan;
    }

    public void setBinhLuan(String binhLuan) {
        this.binhLuan = binhLuan;
    }

    public String getAnhDanhGiaUrls() {
        return anhDanhGiaUrls;
    }

    public void setAnhDanhGiaUrls(String anhDanhGiaUrls) {
        this.anhDanhGiaUrls = anhDanhGiaUrls;
    }

    public Boolean getBiAn() {
        return biAn;
    }

    public void setBiAn(Boolean biAn) {
        this.biAn = biAn;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }
}
