package com.trithucviet.bookstore.service;

import com.trithucviet.bookstore.dto.request.ApplyVoucherRequest;
import com.trithucviet.bookstore.dto.response.VoucherDto;
import com.trithucviet.bookstore.dto.response.VoucherValidationResponse;
import com.trithucviet.bookstore.entity.DanhMuc;
import com.trithucviet.bookstore.entity.GioHang;
import com.trithucviet.bookstore.entity.MaGiamGia;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.entity.TkNguoiDung;
import com.trithucviet.bookstore.entity.enums.LoaiGiam;
import com.trithucviet.bookstore.exception.BusinessException;
import com.trithucviet.bookstore.repository.DanhMucRepository;
import com.trithucviet.bookstore.repository.GioHangRepository;
import com.trithucviet.bookstore.repository.MaGiamGiaRepository;
import com.trithucviet.bookstore.repository.TkNguoiDungRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ Mã giảm giá Voucher (Bước 4 - phần 2 / Mục 5.2 phía Mobile).
 * Kiểm tra đầy đủ trạng thái bật/tắt, ngày hiệu lực, lượt dùng, ràng buộc danh mục (cha–con) và đơn tối thiểu.
 */
@Service
public class VoucherService {

    private static final BigDecimal NGUONG_MIEN_PHI_SHIP = BigDecimal.valueOf(300_000L);
    private static final BigDecimal PHI_VAN_CHUYEN_MAC_DINH = BigDecimal.valueOf(25_000L);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100L);

    private final MaGiamGiaRepository maGiamGiaRepository;
    private final GioHangRepository gioHangRepository;
    private final TkNguoiDungRepository tkNguoiDungRepository;
    private final DanhMucRepository danhMucRepository;

    public VoucherService(
            MaGiamGiaRepository maGiamGiaRepository,
            GioHangRepository gioHangRepository,
            TkNguoiDungRepository tkNguoiDungRepository,
            DanhMucRepository danhMucRepository
    ) {
        this.maGiamGiaRepository = maGiamGiaRepository;
        this.gioHangRepository = gioHangRepository;
        this.tkNguoiDungRepository = tkNguoiDungRepository;
        this.danhMucRepository = danhMucRepository;
    }

    /**
     * Trả về các mã giảm giá đang mở (còn hạn, đang bật kích hoạt, còn lượt).
     * Endpoint công khai dùng cho trang Chi tiết sách, Giỏ hàng và Thanh toán.
     */
    @Transactional(readOnly = true)
    public List<VoucherDto> getActiveVouchers() {
        return maGiamGiaRepository.findActiveVouchers(LocalDate.now())
                .stream()
                .map(this::toVoucherDto)
                .toList();
    }

    /**
     * Kiểm tra điều kiện áp mã và tính thử số tiền được giảm:
     * 1. Nếu request.tamTinh() == null: đọc trực tiếp các món đang tick chọn trong gio_hang của khách,
     *    tính riêng phần tiền các sách khớp danh mục nếu voucher có gắn danh_muc_id.
     * 2. Nếu request.tamTinh() != null: dùng tạm tính gửi lên (phục vụ màn Mua ngay hoặc test nhanh Postman);
     *    nếu voucher yêu cầu danh mục thì bắt buộc truyền kèm danhMucId hợp lệ.
     */
    @Transactional(readOnly = true)
    public VoucherValidationResponse validateVoucher(String nguoiDungId, ApplyVoucherRequest request) {
        getActiveCustomer(nguoiDungId);

        String maCode = request.maCode().trim();
        MaGiamGia voucher = maGiamGiaRepository.findByMaCodeWithDanhMucIgnoreCase(maCode)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy mã giảm giá " + maCode.toUpperCase(Locale.ROOT) + "."
                ));

        ensureVoucherUsable(voucher, LocalDate.now());

        BigDecimal tamTinhToanDon;
        BigDecimal tamTinhApDungVoucher;

        if (request.tamTinh() == null) {
            List<GioHang> selectedRows = gioHangRepository.findAllWithSachByNguoiDungId(nguoiDungId)
                    .stream()
                    .filter(row -> Boolean.TRUE.equals(row.getDaChon()))
                    .toList();

            if (selectedRows.isEmpty()) {
                throw new BusinessException("Vui lòng chọn ít nhất 1 cuốn sách trong giỏ hàng trước khi áp mã giảm giá.");
            }

            tamTinhToanDon = BigDecimal.ZERO;
            BigDecimal matchingSubtotal = BigDecimal.ZERO;
            String targetCategoryId = voucher.getDanhMuc() != null ? voucher.getDanhMuc().getId() : null;

            for (GioHang row : selectedRows) {
                Sach sach = row.getSach();
                BigDecimal lineTotal = resolveEffectivePrice(sach).multiply(BigDecimal.valueOf(row.getSoLuong()));
                tamTinhToanDon = tamTinhToanDon.add(lineTotal);
                if (targetCategoryId == null || isCategoryInScope(sach.getDanhMuc(), targetCategoryId)) {
                    matchingSubtotal = matchingSubtotal.add(lineTotal);
                }
            }

            if (targetCategoryId != null && matchingSubtotal.compareTo(BigDecimal.ZERO) == 0) {
                throw new BusinessException(
                        "Mã " + voucher.getMaCode() + " chỉ áp dụng cho sách thuộc danh mục '"
                                + voucher.getDanhMuc().getTenDanhMuc() + "'."
                );
            }
            tamTinhApDungVoucher = matchingSubtotal;
        } else {
            tamTinhToanDon = request.tamTinh();
            if (voucher.getDanhMuc() == null) {
                tamTinhApDungVoucher = tamTinhToanDon;
            } else {
                // Nếu test/gọi bằng tamTinh thủ công đối với mã có ràng buộc danh mục, cần truyền kèm danhMucId để đối chiếu
                if (request.danhMucId() == null || request.danhMucId().isBlank()) {
                    throw new BusinessException(
                            "Mã " + voucher.getMaCode() + " chỉ áp dụng cho danh mục '"
                                    + voucher.getDanhMuc().getTenDanhMuc()
                                    + "'. Vui lòng gửi kèm danhMucId hoặc bỏ trống tamTinh để kiểm tra theo giỏ hàng."
                    );
                }
                String requestedCategoryId = request.danhMucId().trim();
                DanhMuc requestedCategory = danhMucRepository.findById(requestedCategoryId)
                        .orElseThrow(() -> new BusinessException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy danh mục có mã " + requestedCategoryId + "."
                        ));
                if (!isCategoryInScope(requestedCategory, voucher.getDanhMuc().getId())) {
                    throw new BusinessException(
                            "Mã " + voucher.getMaCode() + " chỉ áp dụng cho sách thuộc danh mục '"
                                    + voucher.getDanhMuc().getTenDanhMuc() + "'."
                    );
                }
                tamTinhApDungVoucher = tamTinhToanDon;
            }
        }

        // Kiểm tra giá trị đơn tối thiểu dựa trên phần tiền đủ điều kiện hưởng mã
        BigDecimal donToiThieu = voucher.getDonToiThieu() == null ? BigDecimal.ZERO : voucher.getDonToiThieu();
        if (tamTinhApDungVoucher.compareTo(donToiThieu) < 0) {
            BigDecimal conThieu = donToiThieu.subtract(tamTinhApDungVoucher);
            String prefix = voucher.getDanhMuc() == null
                    ? "Đơn hàng"
                    : "Tổng tiền sách thuộc danh mục '" + voucher.getDanhMuc().getTenDanhMuc() + "'";
            throw new BusinessException(
                    prefix + " cần tối thiểu " + formatVnd(donToiThieu)
                            + " để sử dụng mã " + voucher.getMaCode()
                            + " (còn thiếu " + formatVnd(conThieu) + ")."
            );
        }

        BigDecimal phiVanChuyen = (tamTinhToanDon.compareTo(BigDecimal.ZERO) <= 0
                || tamTinhToanDon.compareTo(NGUONG_MIEN_PHI_SHIP) >= 0)
                ? BigDecimal.ZERO
                : PHI_VAN_CHUYEN_MAC_DINH;

        BigDecimal rawDiscount = calculateDiscountAmount(voucher, tamTinhApDungVoucher);
        // Chặn không để tiền giảm vượt quá tổng tiền hàng + ship -> đảm bảo tổng thanh toán cuối >= 0
        BigDecimal maxPayable = tamTinhToanDon.add(phiVanChuyen);
        BigDecimal tienGiamGia = rawDiscount.min(maxPayable);
        BigDecimal tongThanhToanDuKien = maxPayable.subtract(tienGiamGia);

        return new VoucherValidationResponse(
                toVoucherDto(voucher),
                tamTinhToanDon,
                tamTinhApDungVoucher,
                phiVanChuyen,
                tienGiamGia,
                tongThanhToanDuKien
        );
    }

    private void ensureVoucherUsable(MaGiamGia voucher, LocalDate homNay) {
        if (!Boolean.TRUE.equals(voucher.getDangHoatDong())) {
            throw new BusinessException("Mã giảm giá " + voucher.getMaCode() + " hiện đang tạm ngưng.");
        }
        if (homNay.isBefore(voucher.getNgayBatDau())) {
            throw new BusinessException(
                    "Mã giảm giá " + voucher.getMaCode() + " chỉ có hiệu lực từ ngày " + voucher.getNgayBatDau() + "."
            );
        }
        if (homNay.isAfter(voucher.getNgayKetThuc())) {
            throw new BusinessException(
                    "Mã giảm giá " + voucher.getMaCode() + " đã hết hạn sử dụng vào ngày " + voucher.getNgayKetThuc() + "."
            );
        }
        if (voucher.getDaSuDung() >= voucher.getGioiHanLuot()) {
            throw new BusinessException("Mã giảm giá " + voucher.getMaCode() + " đã hết lượt sử dụng.");
        }
    }

    /**
     * Công thức tính giảm giá theo PHAN_TICH_NGHIEP_VU_VA_CSDL.md:
     * - CO_DINH: giảm đúng giaTriGiam.
     * - PHAN_TRAM: tamTinhApDungVoucher * giaTriGiam / 100 (làm tròn về đồng VND), không vượt giamToiDa nếu có.
     */
    private BigDecimal calculateDiscountAmount(MaGiamGia voucher, BigDecimal eligibleSubtotal) {
        if (voucher.getLoaiGiam() == LoaiGiam.CO_DINH) {
            return voucher.getGiaTriGiam();
        }
        BigDecimal percentDiscount = eligibleSubtotal
                .multiply(voucher.getGiaTriGiam())
                .divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP);
        BigDecimal maxDiscount = voucher.getGiamToiDa();
        if (maxDiscount != null && maxDiscount.compareTo(BigDecimal.ZERO) > 0 && percentDiscount.compareTo(maxDiscount) > 0) {
            return maxDiscount;
        }
        return percentDiscount;
    }

    /**
     * Kiểm tra một danh mục (của sách) có nằm trong phạm vi danh mục đích của Voucher hay không.
     * Hỗ trợ cả danh mục cha lẫn danh mục con (VD: voucher gắn dm_cong_nghe sẽ áp được cho cả sách
     * ở danh mục dm_cong_nghe lẫn danh mục con dm_kien_truc_pm).
     */
    private boolean isCategoryInScope(DanhMuc bookCategory, String targetCategoryId) {
        DanhMuc current = bookCategory;
        int depth = 0;
        while (current != null && depth < 5) {
            if (targetCategoryId.equals(current.getId())) {
                return true;
            }
            current = current.getDanhMucCha();
            depth++;
        }
        return false;
    }

    private BigDecimal resolveEffectivePrice(Sach sach) {
        if (Boolean.TRUE.equals(sach.getDangFlashSale())
                && sach.getGiaFlashSale() != null
                && sach.getGiaFlashSale().compareTo(BigDecimal.ZERO) > 0) {
            return sach.getGiaFlashSale();
        }
        return sach.getGiaBan();
    }

    private TkNguoiDung getActiveCustomer(String nguoiDungId) {
        TkNguoiDung nguoiDung = tkNguoiDungRepository.findById(nguoiDungId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại. Vui lòng đăng nhập lại."));
        if (Boolean.TRUE.equals(nguoiDung.getBiKhoa())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ CSKH để được hỗ trợ.");
        }
        return nguoiDung;
    }

    private VoucherDto toVoucherDto(MaGiamGia voucher) {
        int conLai = Math.max(0, voucher.getGioiHanLuot() - voucher.getDaSuDung());
        String danhMucId = voucher.getDanhMuc() != null ? voucher.getDanhMuc().getId() : null;
        String tenDanhMuc = voucher.getDanhMuc() != null ? voucher.getDanhMuc().getTenDanhMuc() : null;
        return new VoucherDto(
                voucher.getId(),
                voucher.getMaCode(),
                voucher.getTieuDe(),
                voucher.getMoTa(),
                voucher.getLoaiGiam(),
                voucher.getGiaTriGiam(),
                voucher.getGiamToiDa(),
                voucher.getDonToiThieu(),
                voucher.getGioiHanLuot(),
                voucher.getDaSuDung(),
                conLai,
                voucher.getNgayBatDau(),
                voucher.getNgayKetThuc(),
                voucher.getDangHoatDong(),
                danhMucId,
                tenDanhMuc
        );
    }

    private String formatVnd(BigDecimal amount) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(amount) + "đ";
    }
}
