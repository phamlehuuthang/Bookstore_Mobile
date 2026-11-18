package com.trithucviet.bookstore.entity;

import com.trithucviet.bookstore.entity.enums.LoaiHanhVi;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "hanh_vi_nguoi_dung")
public class HanhViNguoiDung {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private TkNguoiDung nguoiDung;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sach_id")
    private Sach sach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "danh_muc_id", nullable = false)
    private DanhMuc danhMuc;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_hanh_vi", length = 30, nullable = false)
    private LoaiHanhVi loaiHanhVi;

    @Column(name = "thoi_gian_xem_giay", nullable = false)
    private Integer thoiGianXemGiay = 0;

    @Column(name = "trong_so_diem", nullable = false)
    private Integer trongSoDiem = 1;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    public HanhViNguoiDung() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TkNguoiDung getNguoiDung() {
        return nguoiDung;
    }

    public void setNguoiDung(TkNguoiDung nguoiDung) {
        this.nguoiDung = nguoiDung;
    }

    public Sach getSach() {
        return sach;
    }

    public void setSach(Sach sach) {
        this.sach = sach;
    }

    public DanhMuc getDanhMuc() {
        return danhMuc;
    }

    public void setDanhMuc(DanhMuc danhMuc) {
        this.danhMuc = danhMuc;
    }

    public LoaiHanhVi getLoaiHanhVi() {
        return loaiHanhVi;
    }

    public void setLoaiHanhVi(LoaiHanhVi loaiHanhVi) {
        this.loaiHanhVi = loaiHanhVi;
    }

    public Integer getThoiGianXemGiay() {
        return thoiGianXemGiay;
    }

    public void setThoiGianXemGiay(Integer thoiGianXemGiay) {
        this.thoiGianXemGiay = thoiGianXemGiay;
    }

    public Integer getTrongSoDiem() {
        return trongSoDiem;
    }

    public void setTrongSoDiem(Integer trongSoDiem) {
        this.trongSoDiem = trongSoDiem;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }
}
