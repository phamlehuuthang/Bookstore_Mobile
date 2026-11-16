package com.trithucviet.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tac_gia")
public class TacGia {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @Column(name = "ten_tac_gia", length = 150, nullable = false)
    private String tenTacGia;

    @Column(name = "tieu_su", length = 500)
    private String tieuSu;

    public TacGia() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenTacGia() {
        return tenTacGia;
    }

    public void setTenTacGia(String tenTacGia) {
        this.tenTacGia = tenTacGia;
    }

    public String getTieuSu() {
        return tieuSu;
    }

    public void setTieuSu(String tieuSu) {
        this.tieuSu = tieuSu;
    }
}
