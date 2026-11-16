package com.trithucviet.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "sach")
public class Sach {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @Column(name = "ma_isbn", length = 30, nullable = false, unique = true)
    private String maIsbn;

    @Column(name = "tieu_de", length = 255, nullable = false)
    private String tieuDe;

    @Column(name = "duong_dan", length = 255, nullable = false, unique = true)
    private String duongDan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "danh_muc_id", nullable = false)
    private DanhMuc danhMuc;

    @Column(name = "ten_danh_muc_con", length = 120)
    private String tenDanhMucCon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tac_gia_id", nullable = false)
    private TacGia tacGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nha_xuat_ban_id", nullable = false)
    private NhaXuatBan nhaXuatBan;

    @Column(name = "gia_ban", precision = 12, scale = 0, nullable = false)
    private BigDecimal giaBan;

    @Column(name = "gia_goc", precision = 12, scale = 0, nullable = false)
    private BigDecimal giaGoc;

    @Column(name = "dang_flash_sale", nullable = false)
    private Boolean dangFlashSale = false;

    @Column(name = "gia_flash_sale", precision = 12, scale = 0)
    private BigDecimal giaFlashSale;

    @Column(name = "ton_kho", nullable = false)
    private Integer tonKho = 0;

    @Column(name = "da_ban", nullable = false)
    private Integer daBan = 0;

    @Column(name = "diem_danh_gia_tb", precision = 3, scale = 2, nullable = false)
    private BigDecimal diemDanhGiaTb = new BigDecimal("5.00");

    @Column(name = "so_luot_danh_gia", nullable = false)
    private Integer soLuotDanhGia = 0;

    @Column(name = "so_trang")
    private Integer soTrang;

    @Column(name = "nam_xuat_ban")
    private Integer namXuatBan;

    @Column(name = "loai_bia", length = 30, nullable = false)
    private String loaiBia = "Bìa mềm";

    @Column(name = "anh_bia_url", length = 500, nullable = false)
    private String anhBiaUrl;

    @Column(name = "anh_chi_tiet_urls", columnDefinition = "TEXT")
    private String anhChiTietUrls;

    @Column(name = "tom_tat_ngan", length = 500)
    private String tomTatNgan;

    @Column(name = "mo_ta_chi_tiet", columnDefinition = "TEXT")
    private String moTaChiTiet;

    @Column(name = "tu_khoa", length = 500)
    private String tuKhoa;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    @Column(name = "vector_ngu_nghia")
    private float[] vectorNguNghia;

    @Column(name = "ban_chay", nullable = false)
    private Boolean banChay = false;

    @Column(name = "hien_thi", nullable = false)
    private Boolean hienThi = true;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    public Sach() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMaIsbn() {
        return maIsbn;
    }

    public void setMaIsbn(String maIsbn) {
        this.maIsbn = maIsbn;
    }

    public String getTieuDe() {
        return tieuDe;
    }

    public void setTieuDe(String tieuDe) {
        this.tieuDe = tieuDe;
    }

    public String getDuongDan() {
        return duongDan;
    }

    public void setDuongDan(String duongDan) {
        this.duongDan = duongDan;
    }

    public DanhMuc getDanhMuc() {
        return danhMuc;
    }

    public void setDanhMuc(DanhMuc danhMuc) {
        this.danhMuc = danhMuc;
    }

    public String getTenDanhMucCon() {
        return tenDanhMucCon;
    }

    public void setTenDanhMucCon(String tenDanhMucCon) {
        this.tenDanhMucCon = tenDanhMucCon;
    }

    public TacGia getTacGia() {
        return tacGia;
    }

    public void setTacGia(TacGia tacGia) {
        this.tacGia = tacGia;
    }

    public NhaXuatBan getNhaXuatBan() {
        return nhaXuatBan;
    }

    public void setNhaXuatBan(NhaXuatBan nhaXuatBan) {
        this.nhaXuatBan = nhaXuatBan;
    }

    public BigDecimal getGiaBan() {
        return giaBan;
    }

    public void setGiaBan(BigDecimal giaBan) {
        this.giaBan = giaBan;
    }

    public BigDecimal getGiaGoc() {
        return giaGoc;
    }

    public void setGiaGoc(BigDecimal giaGoc) {
        this.giaGoc = giaGoc;
    }

    public Boolean getDangFlashSale() {
        return dangFlashSale;
    }

    public void setDangFlashSale(Boolean dangFlashSale) {
        this.dangFlashSale = dangFlashSale;
    }

    public BigDecimal getGiaFlashSale() {
        return giaFlashSale;
    }

    public void setGiaFlashSale(BigDecimal giaFlashSale) {
        this.giaFlashSale = giaFlashSale;
    }

    public Integer getTonKho() {
        return tonKho;
    }

    public void setTonKho(Integer tonKho) {
        this.tonKho = tonKho;
    }

    public Integer getDaBan() {
        return daBan;
    }

    public void setDaBan(Integer daBan) {
        this.daBan = daBan;
    }

    public BigDecimal getDiemDanhGiaTb() {
        return diemDanhGiaTb;
    }

    public void setDiemDanhGiaTb(BigDecimal diemDanhGiaTb) {
        this.diemDanhGiaTb = diemDanhGiaTb;
    }

    public Integer getSoLuotDanhGia() {
        return soLuotDanhGia;
    }

    public void setSoLuotDanhGia(Integer soLuotDanhGia) {
        this.soLuotDanhGia = soLuotDanhGia;
    }

    public Integer getSoTrang() {
        return soTrang;
    }

    public void setSoTrang(Integer soTrang) {
        this.soTrang = soTrang;
    }

    public Integer getNamXuatBan() {
        return namXuatBan;
    }

    public void setNamXuatBan(Integer namXuatBan) {
        this.namXuatBan = namXuatBan;
    }

    public String getLoaiBia() {
        return loaiBia;
    }

    public void setLoaiBia(String loaiBia) {
        this.loaiBia = loaiBia;
    }

    public String getAnhBiaUrl() {
        return anhBiaUrl;
    }

    public void setAnhBiaUrl(String anhBiaUrl) {
        this.anhBiaUrl = anhBiaUrl;
    }

    public String getAnhChiTietUrls() {
        return anhChiTietUrls;
    }

    public void setAnhChiTietUrls(String anhChiTietUrls) {
        this.anhChiTietUrls = anhChiTietUrls;
    }

    public String getTomTatNgan() {
        return tomTatNgan;
    }

    public void setTomTatNgan(String tomTatNgan) {
        this.tomTatNgan = tomTatNgan;
    }

    public String getMoTaChiTiet() {
        return moTaChiTiet;
    }

    public void setMoTaChiTiet(String moTaChiTiet) {
        this.moTaChiTiet = moTaChiTiet;
    }

    public String getTuKhoa() {
        return tuKhoa;
    }

    public void setTuKhoa(String tuKhoa) {
        this.tuKhoa = tuKhoa;
    }

    public float[] getVectorNguNghia() {
        return vectorNguNghia;
    }

    public void setVectorNguNghia(float[] vectorNguNghia) {
        this.vectorNguNghia = vectorNguNghia;
    }

    public Boolean getBanChay() {
        return banChay;
    }

    public void setBanChay(Boolean banChay) {
        this.banChay = banChay;
    }

    public Boolean getHienThi() {
        return hienThi;
    }

    public void setHienThi(Boolean hienThi) {
        this.hienThi = hienThi;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }
}
