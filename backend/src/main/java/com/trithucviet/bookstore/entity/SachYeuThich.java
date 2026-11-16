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
@Table(name = "sach_yeu_thich")
public class SachYeuThich {

    @EmbeddedId
    private SachYeuThichId id = new SachYeuThichId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("nguoiDungId")
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private TkNguoiDung nguoiDung;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sachId")
    @JoinColumn(name = "sach_id", nullable = false)
    private Sach sach;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    public SachYeuThich() {
    }

    public SachYeuThichId getId() {
        return id;
    }

    public void setId(SachYeuThichId id) {
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

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }

    @Embeddable
    public static class SachYeuThichId implements Serializable {

        @Column(name = "nguoi_dung_id", length = 50, nullable = false)
        private String nguoiDungId;

        @Column(name = "sach_id", length = 50, nullable = false)
        private String sachId;

        public SachYeuThichId() {
        }

        public SachYeuThichId(String nguoiDungId, String sachId) {
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
            if (!(o instanceof SachYeuThichId that)) return false;
            return Objects.equals(nguoiDungId, that.nguoiDungId)
                    && Objects.equals(sachId, that.sachId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nguoiDungId, sachId);
        }
    }
}
