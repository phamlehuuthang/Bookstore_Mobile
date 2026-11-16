package com.trithucviet.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "gio_hang")
public class GioHang {

    @EmbeddedId
    private GioHangId id = new GioHangId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("nguoiDungId")
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private TkNguoiDung nguoiDung;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sachId")
    @JoinColumn(name = "sach_id", nullable = false)
    private Sach sach;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "da_chon", nullable = false)
    private Boolean daChon = true;

    @Column(name = "ngay_cap_nhat", nullable = false)
    private LocalDateTime ngayCapNhat = LocalDateTime.now();

    public GioHang() {
    }

    public GioHangId getId() {
        return id;
    }

    public void setId(GioHangId id) {
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

    public Integer getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(Integer soLuong) {
        this.soLuong = soLuong;
    }

    public Boolean getDaChon() {
        return daChon;
    }

    public void setDaChon(Boolean daChon) {
        this.daChon = daChon;
    }

    public LocalDateTime getNgayCapNhat() {
        return ngayCapNhat;
    }

    public void setNgayCapNhat(LocalDateTime ngayCapNhat) {
        this.ngayCapNhat = ngayCapNhat;
    }

    @Embeddable
    public static class GioHangId implements Serializable {

        @Column(name = "nguoi_dung_id", length = 50, nullable = false)
        private String nguoiDungId;

        @Column(name = "sach_id", length = 50, nullable = false)
        private String sachId;

        public GioHangId() {
        }

        public GioHangId(String nguoiDungId, String sachId) {
            this.nguoiDungId = nguoiDungId;
            this.sachId = sachId;
        }

        public String getNguoiDungId() {
            return nguoiDungId;
        }

        public void setNguoiDungId(String nguoiDungId) {
            this.nguoiDungId = nguoiDungId;
        }

        public String getSachId() {
            return sachId;
        }

        public void setSachId(String sachId) {
            this.sachId = sachId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GioHangId that)) return false;
            return Objects.equals(nguoiDungId, that.nguoiDungId)
                    && Objects.equals(sachId, that.sachId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nguoiDungId, sachId);
        }
    }
}
