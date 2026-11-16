package com.trithucviet.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "nha_xuat_ban")
public class NhaXuatBan {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @Column(name = "ten_nxb", length = 150, nullable = false)
    private String tenNxb;

    @Column(name = "dia_chi", length = 255)
    private String diaChi;

    public NhaXuatBan() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenNxb() {
        return tenNxb;
    }

    public void setTenNxb(String tenNxb) {
        this.tenNxb = tenNxb;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }
}
