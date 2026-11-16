package com.trithucviet.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "danh_muc")
public class DanhMuc {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @Column(name = "ten_danh_muc", length = 120, nullable = false)
    private String tenDanhMuc;

    @Column(name = "duong_dan", length = 150, nullable = false, unique = true)
    private String duongDan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "danh_muc_cha_id")
    private DanhMuc danhMucCha;

    @Column(name = "mo_ta", length = 500)
    private String moTa;

    @Column(name = "ten_bieu_tuong", length = 50)
    private String tenBieuTuong;

    @Column(name = "thu_tu_hien_thi", nullable = false)
    private Integer thuTuHienThi = 1;

    @Column(name = "dang_hoat_dong", nullable = false)
    private Boolean dangHoatDong = true;

    public DanhMuc() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenDanhMuc() {
        return tenDanhMuc;
    }

    public void setTenDanhMuc(String tenDanhMuc) {
        this.tenDanhMuc = tenDanhMuc;
    }

    public String getDuongDan() {
        return duongDan;
    }

    public void setDuongDan(String duongDan) {
        this.duongDan = duongDan;
    }

    public DanhMuc getDanhMucCha() {
        return danhMucCha;
    }

    public void setDanhMucCha(DanhMuc danhMucCha) {
        this.danhMucCha = danhMucCha;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public String getTenBieuTuong() {
        return tenBieuTuong;
    }

    public void setTenBieuTuong(String tenBieuTuong) {
        this.tenBieuTuong = tenBieuTuong;
    }

    public Integer getThuTuHienThi() {
        return thuTuHienThi;
    }

    public void setThuTuHienThi(Integer thuTuHienThi) {
        this.thuTuHienThi = thuTuHienThi;
    }

    public Boolean getDangHoatDong() {
        return dangHoatDong;
    }

    public void setDangHoatDong(Boolean dangHoatDong) {
        this.dangHoatDong = dangHoatDong;
    }
}
