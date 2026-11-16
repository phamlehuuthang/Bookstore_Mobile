package com.trithucviet.bookstore.entity;

import com.trithucviet.bookstore.entity.enums.PhuongThucThanhToan;
import com.trithucviet.bookstore.entity.enums.TrangThaiDon;
import com.trithucviet.bookstore.entity.enums.TrangThaiThanhToan;
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
import java.time.LocalDateTime;

@Entity
@Table(name = "don_hang")
public class DonHang {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @Column(name = "ma_don_hang", length = 30, nullable = false, unique = true)
    private String maDonHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private TkNguoiDung nguoiDung;

    @Column(name = "ten_khach_hang", length = 120, nullable = false)
    private String tenKhachHang;

    @Column(name = "sdt_khach_hang", length = 20, nullable = false)
    private String sdtKhachHang;

    @Column(name = "dia_chi_giao_hang", length = 500, nullable = false)
    private String diaChiGiaoHang;

    @Column(name = "tam_tinh", precision = 15, scale = 0, nullable = false)
    private BigDecimal tamTinh;

    @Column(name = "phi_van_chuyen", precision = 12, scale = 0, nullable = false)
    private BigDecimal phiVanChuyen;

    @Column(name = "tien_giam_gia", precision = 12, scale = 0, nullable = false)
    private BigDecimal tienGiamGia = BigDecimal.ZERO;

    @Column(name = "ma_giam_gia_code", length = 30)
    private String maGiamGiaCode;

    @Column(name = "tong_thanh_toan", precision = 15, scale = 0, nullable = false)
    private BigDecimal tongThanhToan;

    @Enumerated(EnumType.STRING)
    @Column(name = "phuong_thuc_thanh_toan", length = 20, nullable = false)
    private PhuongThucThanhToan phuongThucThanhToan;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_thanh_toan", length = 20, nullable = false)
    private TrangThaiThanhToan trangThaiThanhToan = TrangThaiThanhToan.CHUA_THANH_TOAN;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_don", length = 20, nullable = false)
    private TrangThaiDon trangThaiDon = TrangThaiDon.CHO_XAC_NHAN;

    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;

    @Column(name = "da_danh_gia", nullable = false)
    private Boolean daDanhGia = false;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    public DonHang() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMaDonHang() {
        return maDonHang;
    }

    public void setMaDonHang(String maDonHang) {
        this.maDonHang = maDonHang;
    }

    public TkNguoiDung getNguoiDung() {
        return nguoiDung;
    }

    public void setNguoiDung(TkNguoiDung nguoiDung) {
        this.nguoiDung = nguoiDung;
    }

    public String getTenKhachHang() {
        return tenKhachHang;
    }

    public void setTenKhachHang(String tenKhachHang) {
        this.tenKhachHang = tenKhachHang;
    }

    public String getSdtKhachHang() {
        return sdtKhachHang;
    }

    public void setSdtKhachHang(String sdtKhachHang) {
        this.sdtKhachHang = sdtKhachHang;
    }

    public String getDiaChiGiaoHang() {
        return diaChiGiaoHang;
    }

    public void setDiaChiGiaoHang(String diaChiGiaoHang) {
        this.diaChiGiaoHang = diaChiGiaoHang;
    }

    public BigDecimal getTamTinh() {
        return tamTinh;
    }

    public void setTamTinh(BigDecimal tamTinh) {
        this.tamTinh = tamTinh;
    }

    public BigDecimal getPhiVanChuyen() {
        return phiVanChuyen;
    }

    public void setPhiVanChuyen(BigDecimal phiVanChuyen) {
        this.phiVanChuyen = phiVanChuyen;
    }

    public BigDecimal getTienGiamGia() {
        return tienGiamGia;
    }

    public void setTienGiamGia(BigDecimal tienGiamGia) {
        this.tienGiamGia = tienGiamGia;
    }

    public String getMaGiamGiaCode() {
        return maGiamGiaCode;
    }

    public void setMaGiamGiaCode(String maGiamGiaCode) {
        this.maGiamGiaCode = maGiamGiaCode;
    }

    public BigDecimal getTongThanhToan() {
        return tongThanhToan;
    }

    public void setTongThanhToan(BigDecimal tongThanhToan) {
        this.tongThanhToan = tongThanhToan;
    }

    public PhuongThucThanhToan getPhuongThucThanhToan() {
        return phuongThucThanhToan;
    }

    public void setPhuongThucThanhToan(PhuongThucThanhToan phuongThucThanhToan) {
        this.phuongThucThanhToan = phuongThucThanhToan;
    }

    public TrangThaiThanhToan getTrangThaiThanhToan() {
        return trangThaiThanhToan;
    }

    public void setTrangThaiThanhToan(TrangThaiThanhToan trangThaiThanhToan) {
        this.trangThaiThanhToan = trangThaiThanhToan;
    }

    public TrangThaiDon getTrangThaiDon() {
        return trangThaiDon;
    }

    public void setTrangThaiDon(TrangThaiDon trangThaiDon) {
        this.trangThaiDon = trangThaiDon;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public Boolean getDaDanhGia() {
        return daDanhGia;
    }

    public void setDaDanhGia(Boolean daDanhGia) {
        this.daDanhGia = daDanhGia;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }
}
