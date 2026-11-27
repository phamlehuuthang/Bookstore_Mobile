package com.trithucviet.bookstore.service;

import com.trithucviet.bookstore.dto.request.ApplyVoucherRequest;
import com.trithucviet.bookstore.dto.request.VoucherUpsertRequest;
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
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ Mã giảm giá Voucher cho cả Mobile (Mục 5.2) và Web Admin (Bước 4 - phần 3).
 * - Mobile: lọc mã đang mở, kiểm tra điều kiện áp dụng, ràng buộc danh mục cha–con và tính tiền giảm.
 * - Web Admin: xem/lọc toàn bộ mã, tạo mới, cập nhật (khóa đổi mã đã có lượt dùng để bảo toàn lịch sử đơn), bật/tắt.
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
     * Tự động tìm mã giảm giá đang mở mang lại số tiền giảm cao nhất cho 1 đầu sách cụ thể (không ném Exception).
     * Dùng cho Tư vấn viên AI tự động áp mã hời nhất khi đặt đơn nhanh cho khách.
     */
    @Transactional(readOnly = true)
    public Optional<VoucherValidationResponse> findBestVoucherForSingleBook(BigDecimal tamTinh, DanhMuc bookCategory) {
        if (tamTinh == null || tamTinh.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        BigDecimal phiVanChuyen = tamTinh.compareTo(NGUONG_MIEN_PHI_SHIP) >= 0
                ? BigDecimal.ZERO
                : PHI_VAN_CHUYEN_MAC_DINH;
        BigDecimal maxPayable = tamTinh.add(phiVanChuyen);

        VoucherValidationResponse best = null;
        for (MaGiamGia v : maGiamGiaRepository.findActiveVouchers(LocalDate.now())) {
            if (v.getDanhMuc() != null && (bookCategory == null || !isCategoryInScope(bookCategory, v.getDanhMuc().getId()))) {
                continue;
            }
            BigDecimal donToiThieu = v.getDonToiThieu() == null ? BigDecimal.ZERO : v.getDonToiThieu();
            if (tamTinh.compareTo(donToiThieu) < 0) {
                continue;
            }
            BigDecimal discount = calculateDiscountAmount(v, tamTinh).min(maxPayable);
            if (discount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (best == null || discount.compareTo(best.tienGiamGia()) > 0) {
                best = new VoucherValidationResponse(
                        toVoucherDto(v),
                        tamTinh,
                        tamTinh,
                        phiVanChuyen,
                        discount,
                        maxPayable.subtract(discount)
                );
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Tự động tìm mã giảm giá đang mở tiết kiệm nhiều tiền nhất cho các sách đang tick chọn trong giỏ hàng.
     */
    @Transactional(readOnly = true)
    public Optional<VoucherValidationResponse> findBestVoucherForSelectedCart(String nguoiDungId) {
        List<GioHang> selectedRows = gioHangRepository.findAllWithSachByNguoiDungId(nguoiDungId)
                .stream()
                .filter(row -> Boolean.TRUE.equals(row.getDaChon()))
                .toList();
        if (selectedRows.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal tamTinhToanDon = BigDecimal.ZERO;
        for (GioHang row : selectedRows) {
            tamTinhToanDon = tamTinhToanDon.add(
                    resolveEffectivePrice(row.getSach()).multiply(BigDecimal.valueOf(row.getSoLuong()))
            );
        }
        if (tamTinhToanDon.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        BigDecimal phiVanChuyen = tamTinhToanDon.compareTo(NGUONG_MIEN_PHI_SHIP) >= 0
                ? BigDecimal.ZERO
                : PHI_VAN_CHUYEN_MAC_DINH;
        BigDecimal maxPayable = tamTinhToanDon.add(phiVanChuyen);

        VoucherValidationResponse best = null;
        for (MaGiamGia v : maGiamGiaRepository.findActiveVouchers(LocalDate.now())) {
            String targetCatId = v.getDanhMuc() != null ? v.getDanhMuc().getId() : null;
            BigDecimal matchingSubtotal = BigDecimal.ZERO;
            for (GioHang row : selectedRows) {
                Sach s = row.getSach();
                if (targetCatId == null || isCategoryInScope(s.getDanhMuc(), targetCatId)) {
                    matchingSubtotal = matchingSubtotal.add(
                            resolveEffectivePrice(s).multiply(BigDecimal.valueOf(row.getSoLuong()))
                    );
                }
            }
            if (matchingSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal donToiThieu = v.getDonToiThieu() == null ? BigDecimal.ZERO : v.getDonToiThieu();
            if (matchingSubtotal.compareTo(donToiThieu) < 0) {
                continue;
            }
            BigDecimal discount = calculateDiscountAmount(v, matchingSubtotal).min(maxPayable);
            if (discount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (best == null || discount.compareTo(best.tienGiamGia()) > 0) {
                best = new VoucherValidationResponse(
                        toVoucherDto(v),
                        tamTinhToanDon,
                        matchingSubtotal,
                        phiVanChuyen,
                        discount,
                        maxPayable.subtract(discount)
                );
            }
        }
        return Optional.ofNullable(best);
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

    // =========================================================================
    // PHẦN 3: QUẢN LÝ VOUCHER PHÍA WEB ADMIN (ADMIN & CSKH)
    // =========================================================================

    /**
     * Lấy danh sách toàn bộ voucher cho Web Admin (kể cả mã đang tạm tắt hoặc đã hết hạn).
     * Thực hiện lọc dangHoatDong/keyword trên Java sau khi tải danh sách (bảng ma_giam_gia ít dòng)
     * để tránh lỗi PostgreSQL không suy luận được kiểu tham số khi truyền null vào JPQL.
     */
    @Transactional(readOnly = true)
    public List<VoucherDto> getAdminVouchers(Boolean dangHoatDong, String keyword) {
        String normalizedKeyword = (keyword == null || keyword.isBlank())
                ? null
                : keyword.trim().toLowerCase(Locale.ROOT);

        return maGiamGiaRepository.findAllWithDanhMuc()
                .stream()
                .filter(v -> dangHoatDong == null || Boolean.valueOf(Boolean.TRUE.equals(v.getDangHoatDong())).equals(dangHoatDong))
                .filter(v -> normalizedKeyword == null
                        || v.getMaCode().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || v.getTieuDe().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .map(this::toVoucherDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public VoucherDto getVoucherById(String id) {
        MaGiamGia voucher = maGiamGiaRepository.findByIdWithDanhMuc(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy mã giảm giá có id " + id + "."));
        return toVoucherDto(voucher);
    }

    /**
     * Tạo voucher mới (chỉ ADMIN): chuẩn hóa mã code in hoa, kiểm tra trùng mã, phần trăm 1..100, ngày hợp lệ.
     */
    @Transactional
    public VoucherDto createVoucher(VoucherUpsertRequest request) {
        String maCode = request.maCode().trim().toUpperCase(Locale.ROOT);
        if (maGiamGiaRepository.existsByMaCodeIgnoreCase(maCode)) {
            throw new BusinessException("Mã giảm giá " + maCode + " đã tồn tại trong hệ thống.");
        }

        LocalDate ngayBatDau = request.ngayBatDau() != null ? request.ngayBatDau() : LocalDate.now();
        validateVoucherForm(request, ngayBatDau);
        DanhMuc danhMuc = resolveOptionalDanhMuc(request.danhMucId());

        MaGiamGia voucher = new MaGiamGia();
        voucher.setId("mgg_" + UUID.randomUUID().toString().substring(0, 8));
        voucher.setDaSuDung(0);
        boolean dangHoatDong = request.dangHoatDong() == null || Boolean.TRUE.equals(request.dangHoatDong());
        applyVoucherFields(voucher, maCode, request, ngayBatDau, danhMuc, dangHoatDong);

        maGiamGiaRepository.saveAndFlush(voucher);
        return toVoucherDto(voucher);
    }

    /**
     * Cập nhật voucher (chỉ ADMIN):
     * - Chặn đổi maCode nếu voucher đã phát sinh lượt sử dụng (daSuDung > 0) để bảo vệ khóa ngoại
     *   don_hang.ma_giam_gia_code đang tham chiếu sang ma_giam_gia(ma_code).
     * - Chặn hạ gioiHanLuot xuống thấp hơn số lượt đã dùng (daSuDung).
     */
    @Transactional
    public VoucherDto updateVoucher(String id, VoucherUpsertRequest request) {
        MaGiamGia voucher = maGiamGiaRepository.findByIdWithDanhMuc(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy mã giảm giá có id " + id + "."));

        String maCodeMoi = request.maCode().trim().toUpperCase(Locale.ROOT);
        if (!maCodeMoi.equalsIgnoreCase(voucher.getMaCode()) && voucher.getDaSuDung() > 0) {
            throw new BusinessException(
                    "Không thể đổi mã code của voucher đã có đơn hàng sử dụng. Hãy tạo mã mới hoặc tạm ngưng mã này."
            );
        }
        if (maGiamGiaRepository.existsByMaCodeIgnoreCaseAndIdNot(maCodeMoi, id)) {
            throw new BusinessException("Mã giảm giá " + maCodeMoi + " đã tồn tại trong hệ thống.");
        }
        if (request.gioiHanLuot() < voucher.getDaSuDung()) {
            throw new BusinessException(
                    "Giới hạn lượt dùng (" + request.gioiHanLuot()
                            + ") không được nhỏ hơn số lượt đã sử dụng (" + voucher.getDaSuDung() + ")."
            );
        }

        LocalDate ngayBatDau = request.ngayBatDau() != null ? request.ngayBatDau() : voucher.getNgayBatDau();
        validateVoucherForm(request, ngayBatDau);
        DanhMuc danhMuc = resolveOptionalDanhMuc(request.danhMucId());
        boolean dangHoatDong = request.dangHoatDong() != null
                ? Boolean.TRUE.equals(request.dangHoatDong())
                : Boolean.TRUE.equals(voucher.getDangHoatDong());

        applyVoucherFields(voucher, maCodeMoi, request, ngayBatDau, danhMuc, dangHoatDong);
        maGiamGiaRepository.saveAndFlush(voucher);
        return toVoucherDto(voucher);
    }

    /**
     * Bật/tắt nhanh trạng thái kích hoạt của voucher (thay cho xóa cứng để giữ lịch sử đơn hàng).
     */
    @Transactional
    public VoucherDto toggleVoucherActive(String id) {
        MaGiamGia voucher = maGiamGiaRepository.findByIdWithDanhMuc(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy mã giảm giá có id " + id + "."));
        voucher.setDangHoatDong(!Boolean.TRUE.equals(voucher.getDangHoatDong()));
        maGiamGiaRepository.saveAndFlush(voucher);
        return toVoucherDto(voucher);
    }

    private void validateVoucherForm(VoucherUpsertRequest request, LocalDate ngayBatDau) {
        if (request.loaiGiam() == LoaiGiam.PHAN_TRAM
                && (request.giaTriGiam().compareTo(BigDecimal.ONE) < 0
                || request.giaTriGiam().compareTo(ONE_HUNDRED) > 0)) {
            throw new BusinessException("Với loại giảm theo phần trăm, giá trị giảm phải nằm trong khoảng từ 1 đến 100.");
        }
        if (request.ngayKetThuc().isBefore(ngayBatDau)) {
            throw new BusinessException("Ngày kết thúc không được trước ngày bắt đầu.");
        }
    }

    private DanhMuc resolveOptionalDanhMuc(String danhMucId) {
        if (danhMucId == null || danhMucId.isBlank()) {
            return null;
        }
        String trimmedId = danhMucId.trim();
        return danhMucRepository.findById(trimmedId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy danh mục có mã " + trimmedId + "."
                ));
    }

    private void applyVoucherFields(
            MaGiamGia voucher,
            String maCode,
            VoucherUpsertRequest request,
            LocalDate ngayBatDau,
            DanhMuc danhMuc,
            boolean dangHoatDong
    ) {
        voucher.setMaCode(maCode);
        voucher.setTieuDe(request.tieuDe().trim());
        voucher.setMoTa(request.moTa() != null && !request.moTa().isBlank() ? request.moTa().trim() : null);
        voucher.setLoaiGiam(request.loaiGiam());
        voucher.setGiaTriGiam(request.giaTriGiam());
        // Chỉ lưu mức giảm tối đa cho loại PHAN_TRAM (loại CO_DINH luôn để null)
        BigDecimal giamToiDa = (request.loaiGiam() == LoaiGiam.PHAN_TRAM
                && request.giamToiDa() != null
                && request.giamToiDa().compareTo(BigDecimal.ZERO) > 0)
                ? request.giamToiDa()
                : null;
        voucher.setGiamToiDa(giamToiDa);
        voucher.setDonToiThieu(request.donToiThieu() != null ? request.donToiThieu() : BigDecimal.ZERO);
        voucher.setDanhMuc(danhMuc);
        voucher.setGioiHanLuot(request.gioiHanLuot());
        voucher.setNgayBatDau(ngayBatDau);
        voucher.setNgayKetThuc(request.ngayKetThuc());
        voucher.setDangHoatDong(dangHoatDong);
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
