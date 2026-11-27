package com.trithucviet.bookstore.service;

import com.trithucviet.bookstore.dto.request.ApplyVoucherRequest;
import com.trithucviet.bookstore.dto.request.CancelOrderRequest;
import com.trithucviet.bookstore.dto.request.CreateOrderRequest;
import com.trithucviet.bookstore.dto.request.UpdateOrderStatusRequest;
import com.trithucviet.bookstore.dto.response.OrderDto;
import com.trithucviet.bookstore.dto.response.OrderItemDto;
import com.trithucviet.bookstore.dto.response.OrderTimelineDto;
import com.trithucviet.bookstore.dto.response.ReturnRequestDto;
import com.trithucviet.bookstore.dto.response.VoucherValidationResponse;
import com.trithucviet.bookstore.entity.ChiTietDonHang;
import com.trithucviet.bookstore.entity.DonHang;
import com.trithucviet.bookstore.entity.GioHang;
import com.trithucviet.bookstore.entity.LichSuDonHang;
import com.trithucviet.bookstore.entity.MaGiamGia;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.entity.SoDiaChi;
import com.trithucviet.bookstore.entity.ThongBao;
import com.trithucviet.bookstore.entity.TkNguoiDung;
import com.trithucviet.bookstore.entity.YeuCauDoiTra;
import com.trithucviet.bookstore.entity.enums.LoaiHanhVi;
import com.trithucviet.bookstore.entity.enums.LoaiThongBao;
import com.trithucviet.bookstore.entity.enums.PhuongThucThanhToan;
import com.trithucviet.bookstore.entity.enums.TrangThaiDon;
import com.trithucviet.bookstore.entity.enums.TrangThaiThanhToan;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import com.trithucviet.bookstore.exception.BusinessException;
import com.trithucviet.bookstore.repository.ChiTietDonHangRepository;
import com.trithucviet.bookstore.repository.DonHangRepository;
import com.trithucviet.bookstore.repository.GioHangRepository;
import com.trithucviet.bookstore.repository.LichSuDonHangRepository;
import com.trithucviet.bookstore.repository.MaGiamGiaRepository;
import com.trithucviet.bookstore.repository.SachRepository;
import com.trithucviet.bookstore.repository.SoDiaChiRepository;
import com.trithucviet.bookstore.repository.ThongBaoRepository;
import com.trithucviet.bookstore.repository.TkNguoiDungRepository;
import com.trithucviet.bookstore.repository.YeuCauDoiTraRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ Đặt hàng, Theo dõi hành trình, Hủy đơn & Duyệt trạng thái đơn hàng (Bước 6).
 *
 * Bao gồm 3 nhóm chính:
 * 1. Khách hàng đặt hàng (@Transactional createOrder): kiểm kho -> tính tiền + phí ship + voucher
 *    -> trừ sach.tonKho & tăng sach.daBan -> tăng maGiamGia.daSuDung -> lưu DonHang + ChiTietDonHang
 *    + mốc đầu LichSuDonHang -> cộng tongChiTieu/soDonHang -> xóa món đã mua khỏi GioHang -> tạo ThongBao.
 * 2. Khách hàng xem danh sách đơn theo trạng thái, xem Timeline và Hủy đơn (chỉ cho hủy khi CHO_XAC_NHAN).
 * 3. Nhân sự Web Admin chuyển trạng thái 1 chiều theo đúng RBAC và hoàn dữ liệu đầy đủ khi hủy đơn:
 *    cộng lại tonKho, giảm daBan, hoàn 1 lượt voucher, trừ lại tongChiTieu/soDonHang và chuyển DA_HOAN_TIEN nếu đã trả trước.
 */
@Service
public class OrderService {

    private static final BigDecimal NGUONG_MIEN_PHI_SHIP = BigDecimal.valueOf(300_000L);
    private static final BigDecimal PHI_VAN_CHUYEN_MAC_DINH = BigDecimal.valueOf(25_000L);
    private static final DateTimeFormatter TIMELINE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter LEGACY_TIMELINE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DonHangRepository donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final LichSuDonHangRepository lichSuDonHangRepository;
    private final GioHangRepository gioHangRepository;
    private final SachRepository sachRepository;
    private final SoDiaChiRepository soDiaChiRepository;
    private final TkNguoiDungRepository tkNguoiDungRepository;
    private final MaGiamGiaRepository maGiamGiaRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final YeuCauDoiTraRepository yeuCauDoiTraRepository;
    private final VoucherService voucherService;
    private final RecommendationTrackingService recommendationTrackingService;

    public OrderService(
            DonHangRepository donHangRepository,
            ChiTietDonHangRepository chiTietDonHangRepository,
            LichSuDonHangRepository lichSuDonHangRepository,
            GioHangRepository gioHangRepository,
            SachRepository sachRepository,
            SoDiaChiRepository soDiaChiRepository,
            TkNguoiDungRepository tkNguoiDungRepository,
            MaGiamGiaRepository maGiamGiaRepository,
            ThongBaoRepository thongBaoRepository,
            YeuCauDoiTraRepository yeuCauDoiTraRepository,
            VoucherService voucherService,
            RecommendationTrackingService recommendationTrackingService
    ) {
        this.donHangRepository = donHangRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.lichSuDonHangRepository = lichSuDonHangRepository;
        this.gioHangRepository = gioHangRepository;
        this.sachRepository = sachRepository;
        this.soDiaChiRepository = soDiaChiRepository;
        this.tkNguoiDungRepository = tkNguoiDungRepository;
        this.maGiamGiaRepository = maGiamGiaRepository;
        this.thongBaoRepository = thongBaoRepository;
        this.yeuCauDoiTraRepository = yeuCauDoiTraRepository;
        this.voucherService = voucherService;
        this.recommendationTrackingService = recommendationTrackingService;
    }

    // =========================================================================
    // PHẦN 1: KHÁCH HÀNG ĐẶT HÀNG (CHECKOUT)
    // =========================================================================

    /**
     * Tạo đơn hàng mới trong 1 Transaction duy nhất — nếu bất kỳ bước nào lỗi (hết hàng, voucher hết hạn...)
     * toàn bộ thay đổi kho/giỏ/voucher sẽ tự động rollback.
     */
    @Transactional
    public OrderDto createOrder(String nguoiDungId, CreateOrderRequest request) {
        TkNguoiDung customer = requireActiveCustomer(nguoiDungId);
        SoDiaChi shippingAddress = resolveCustomerAddress(nguoiDungId, request.soDiaChiId());

        List<GioHang> selectedRows = gioHangRepository.findAllWithSachByNguoiDungId(nguoiDungId)
                .stream()
                .filter(row -> Boolean.TRUE.equals(row.getDaChon()))
                .toList();

        if (selectedRows.isEmpty()) {
            throw new BusinessException("Giỏ hàng chưa có sách nào được chọn để đặt hàng.");
        }

        // Kiểm tra hiển thị & tồn kho từng đầu sách, đồng thời tính tạm tính theo đơn giá thực tế
        BigDecimal tamTinh = BigDecimal.ZERO;
        List<Sach> modifiedBooks = new ArrayList<>();

        for (GioHang row : selectedRows) {
            Sach sach = row.getSach();
            if (!Boolean.TRUE.equals(sach.getHienThi())) {
                throw new BusinessException("Sách \"" + sach.getTieuDe() + "\" hiện đã ngừng kinh doanh.");
            }
            int currentStock = safeInt(sach.getTonKho());
            int qty = safeInt(row.getSoLuong());
            if (qty <= 0) {
                throw new BusinessException("Số lượng đặt mua của \"" + sach.getTieuDe() + "\" không hợp lệ.");
            }
            if (currentStock <= 0) {
                throw new BusinessException("Sách \"" + sach.getTieuDe() + "\" hiện đã hết hàng.");
            }
            if (qty > currentStock) {
                throw new BusinessException(
                        "Sách \"" + sach.getTieuDe() + "\" chỉ còn " + currentStock
                                + " cuốn trong kho, không đủ số lượng " + qty + "."
                );
            }

            BigDecimal unitPrice = resolveEffectivePrice(sach);
            tamTinh = tamTinh.add(unitPrice.multiply(BigDecimal.valueOf(qty)));

            sach.setTonKho(currentStock - qty);
            sach.setDaBan(safeInt(sach.getDaBan()) + qty);
            modifiedBooks.add(sach);
        }

        // Tính phí vận chuyển & kiểm tra lại Voucher ở phía Backend để chống giả mạo tiền giảm
        BigDecimal phiVanChuyen = tamTinh.compareTo(NGUONG_MIEN_PHI_SHIP) >= 0
                ? BigDecimal.ZERO
                : PHI_VAN_CHUYEN_MAC_DINH;
        BigDecimal tienGiamGia = BigDecimal.ZERO;
        String appliedVoucherCode = null;

        if (request.maGiamGiaCode() != null && !request.maGiamGiaCode().isBlank()) {
            VoucherValidationResponse validation = voucherService.validateVoucher(
                    nguoiDungId,
                    new ApplyVoucherRequest(request.maGiamGiaCode().trim(), null, null)
            );
            appliedVoucherCode = validation.voucher().maCode();
            tienGiamGia = validation.tienGiamGia();
            phiVanChuyen = validation.phiVanChuyen();

            MaGiamGia voucher = maGiamGiaRepository.findByMaCodeWithDanhMucIgnoreCase(appliedVoucherCode)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy mã giảm giá."));
            voucher.setDaSuDung(safeInt(voucher.getDaSuDung()) + 1);
            maGiamGiaRepository.saveAndFlush(voucher);
        }

        BigDecimal tongThanhToan = tamTinh.add(phiVanChuyen).subtract(tienGiamGia);
        if (tongThanhToan.compareTo(BigDecimal.ZERO) < 0) {
            tongThanhToan = BigDecimal.ZERO;
        }

        // Lưu bảng don_hang
        LocalDateTime now = LocalDateTime.now();
        DonHang order = new DonHang();
        order.setId("dh_" + shortUuid());
        order.setMaDonHang(generateUniqueOrderCode());
        order.setNguoiDung(customer);
        order.setTenKhachHang(shippingAddress.getHoTenNguoiNhan());
        order.setSdtKhachHang(shippingAddress.getSoDienThoai());
        order.setDiaChiGiaoHang(formatFullAddress(shippingAddress));
        order.setTamTinh(tamTinh);
        order.setPhiVanChuyen(phiVanChuyen);
        order.setTienGiamGia(tienGiamGia);
        order.setMaGiamGiaCode(appliedVoucherCode);
        order.setTongThanhToan(tongThanhToan);
        order.setPhuongThucThanhToan(request.phuongThucThanhToan());
        order.setTrangThaiThanhToan(
                request.phuongThucThanhToan() == PhuongThucThanhToan.TIEN_MAT_COD
                        ? TrangThaiThanhToan.CHUA_THANH_TOAN
                        : TrangThaiThanhToan.DA_THANH_TOAN
        );
        order.setTrangThaiDon(TrangThaiDon.CHO_XAC_NHAN);
        order.setGhiChu(normalizeBlankToNull(request.ghiChu()));
        order.setDaDanhGia(Boolean.FALSE);
        order.setNgayTao(now);

        donHangRepository.saveAndFlush(order);
        sachRepository.saveAllAndFlush(modifiedBooks);

        // Lưu chi_tiet_don_hang
        List<ChiTietDonHang> orderItems = new ArrayList<>();
        for (GioHang row : selectedRows) {
            Sach sach = row.getSach();
            ChiTietDonHang item = new ChiTietDonHang();
            item.setId("ctdh_" + shortUuid());
            item.setDonHang(order);
            item.setSach(sach);
            item.setTieuDeSach(sach.getTieuDe());
            item.setAnhBiaUrl(sach.getAnhBiaUrl());
            item.setTenTacGia(sach.getTacGia().getTenTacGia());
            item.setDonGia(resolveEffectivePrice(sach));
            item.setSoLuong(row.getSoLuong());
            orderItems.add(item);
            // Cách B: Tự động ghi nhận hành vi MUA_HANG (+10 điểm) cho từng cuốn sách khách đặt mua
            recommendationTrackingService.recordAutoAction(customer, sach, LoaiHanhVi.MUA_HANG);
        }
        chiTietDonHangRepository.saveAllAndFlush(orderItems);

        // Lưu mốc Timeline đầu tiên
        LichSuDonHang timeline = appendTimeline(
                order,
                TrangThaiDon.CHO_XAC_NHAN,
                "Đặt hàng thành công",
                "Đơn hàng " + order.getMaDonHang() + " đã được tiếp nhận và đang chờ xác nhận.",
                now
        );

        // Cộng tích lũy chi tiêu & số đơn cho khách hàng
        BigDecimal currentSpent = customer.getTongChiTieu() != null ? customer.getTongChiTieu() : BigDecimal.ZERO;
        customer.setTongChiTieu(currentSpent.add(tongThanhToan));
        customer.setSoDonHang(safeInt(customer.getSoDonHang()) + 1);
        tkNguoiDungRepository.saveAndFlush(customer);

        // Chỉ xóa các món đã tick chọn đặt hàng; giữ nguyên các món khách bỏ tick trong giỏ
        gioHangRepository.deleteAllInBatch(selectedRows);
        gioHangRepository.flush();

        // Tạo thông báo đặt hàng cho khách
        createOrderNotification(
                customer,
                "Đặt hàng thành công " + order.getMaDonHang(),
                "Đơn hàng " + order.getMaDonHang() + " trị giá " + tongThanhToan.toPlainString()
                        + "đ đã được ghi nhận và đang chờ xác nhận.",
                now
        );

        return toOrderDto(order, orderItems, List.of(timeline));
    }

    /**
     * Đặt đơn hàng trực tiếp cho 1 cuốn sách cụ thể từ Tư vấn viên AI (Bước 10 - Quy tắc A2):
     * - KHÔNG đụng vào các món đang nằm sẵn trong bảng gio_hang của khách.
     * - Giao về địa chỉ mặc định của khách với phương thức thanh toán COD.
     * - Vẫn áp dụng đầy đủ @Transactional: kiểm tra & trừ ton_kho, tăng da_ban, tạo DonHang + ChiTietDonHang
     *   + LichSuDonHang, cộng chi tiêu, gửi ThongBao và tự động ghi hành vi MUA_HANG (+10 điểm).
     */
    @Transactional
    public OrderDto createDirectOrderForAi(
            String nguoiDungId,
            String sachId,
            int soLuong,
            String ghiChu
    ) {
        return createDirectOrderForAi(nguoiDungId, sachId, soLuong, null, true, ghiChu);
    }

    /**
     * Đặt đơn hàng COD trực tiếp từ Tư vấn viên AI kèm tự động săn & áp dụng mã giảm giá tốt nhất (nếu đủ điều kiện).
     * - Nếu khách chỉ định `requestedVoucherCode`: kiểm tra và áp đúng mã đó.
     * - Nếu `requestedVoucherCode` rỗng và `autoBestVoucher = true`: tự quét các mã đang mở và áp mã giảm nhiều nhất.
     * - Giữ nguyên 100% các món đang nằm sẵn trong bảng `gio_hang`.
     */
    @Transactional
    public OrderDto createDirectOrderForAi(
            String nguoiDungId,
            String sachId,
            int soLuong,
            String requestedVoucherCode,
            boolean autoBestVoucher,
            String ghiChu
    ) {
        TkNguoiDung customer = requireActiveCustomer(nguoiDungId);
        SoDiaChi shippingAddress = resolveCustomerAddress(nguoiDungId, null);

        if (soLuong <= 0) {
            throw new BusinessException("Số lượng đặt mua phải lớn hơn 0.");
        }

        Sach sach = sachRepository.findByIdWithDetails(sachId.trim())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy cuốn sách cần đặt."));
        if (!Boolean.TRUE.equals(sach.getHienThi())) {
            throw new BusinessException("Sách \"" + sach.getTieuDe() + "\" hiện đã ngừng kinh doanh.");
        }

        int currentStock = safeInt(sach.getTonKho());
        if (currentStock <= 0) {
            throw new BusinessException("Sách \"" + sach.getTieuDe() + "\" hiện đã hết hàng.");
        }
        if (soLuong > currentStock) {
            throw new BusinessException(
                    "Sách \"" + sach.getTieuDe() + "\" chỉ còn " + currentStock + " cuốn trong kho."
            );
        }

        BigDecimal unitPrice = resolveEffectivePrice(sach);
        BigDecimal tamTinh = unitPrice.multiply(BigDecimal.valueOf(soLuong));
        BigDecimal phiVanChuyen = tamTinh.compareTo(NGUONG_MIEN_PHI_SHIP) >= 0
                ? BigDecimal.ZERO
                : PHI_VAN_CHUYEN_MAC_DINH;

        BigDecimal tienGiamGia = BigDecimal.ZERO;
        String appliedVoucherCode = null;
        String normalizedCode = normalizeBlankToNull(requestedVoucherCode);

        if (normalizedCode != null && !"AUTO".equalsIgnoreCase(normalizedCode)) {
            VoucherValidationResponse vRes = voucherService.validateVoucher(
                    nguoiDungId,
                    new ApplyVoucherRequest(normalizedCode, tamTinh, sach.getDanhMuc().getId())
            );
            appliedVoucherCode = vRes.voucher().maCode();
            tienGiamGia = vRes.tienGiamGia();
            phiVanChuyen = vRes.phiVanChuyen();
        } else if (autoBestVoucher || "AUTO".equalsIgnoreCase(normalizedCode)) {
            var bestOpt = voucherService.findBestVoucherForSingleBook(tamTinh, sach.getDanhMuc());
            if (bestOpt.isPresent()) {
                VoucherValidationResponse best = bestOpt.get();
                appliedVoucherCode = best.voucher().maCode();
                tienGiamGia = best.tienGiamGia();
                phiVanChuyen = best.phiVanChuyen();
            }
        }

        if (appliedVoucherCode != null) {
            MaGiamGia voucher = maGiamGiaRepository.findByMaCodeWithDanhMucIgnoreCase(appliedVoucherCode)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy mã giảm giá."));
            voucher.setDaSuDung(safeInt(voucher.getDaSuDung()) + 1);
            maGiamGiaRepository.saveAndFlush(voucher);
        }

        BigDecimal tongThanhToan = tamTinh.add(phiVanChuyen).subtract(tienGiamGia);
        if (tongThanhToan.compareTo(BigDecimal.ZERO) < 0) {
            tongThanhToan = BigDecimal.ZERO;
        }

        sach.setTonKho(currentStock - soLuong);
        sach.setDaBan(safeInt(sach.getDaBan()) + soLuong);
        sachRepository.saveAndFlush(sach);

        LocalDateTime now = LocalDateTime.now();
        DonHang order = new DonHang();
        order.setId("dh_" + shortUuid());
        order.setMaDonHang(generateUniqueOrderCode());
        order.setNguoiDung(customer);
        order.setTenKhachHang(shippingAddress.getHoTenNguoiNhan());
        order.setSdtKhachHang(shippingAddress.getSoDienThoai());
        order.setDiaChiGiaoHang(formatFullAddress(shippingAddress));
        order.setTamTinh(tamTinh);
        order.setPhiVanChuyen(phiVanChuyen);
        order.setTienGiamGia(tienGiamGia);
        order.setMaGiamGiaCode(appliedVoucherCode);
        order.setTongThanhToan(tongThanhToan);
        order.setPhuongThucThanhToan(PhuongThucThanhToan.TIEN_MAT_COD);
        order.setTrangThaiThanhToan(TrangThaiThanhToan.CHUA_THANH_TOAN);
        order.setTrangThaiDon(TrangThaiDon.CHO_XAC_NHAN);
        order.setGhiChu(normalizeBlankToNull(ghiChu) != null ? ghiChu.trim() : "Đặt nhanh qua Tư vấn viên AI");
        order.setDaDanhGia(Boolean.FALSE);
        order.setNgayTao(now);
        donHangRepository.saveAndFlush(order);

        ChiTietDonHang item = new ChiTietDonHang();
        item.setId("ctdh_" + shortUuid());
        item.setDonHang(order);
        item.setSach(sach);
        item.setTieuDeSach(sach.getTieuDe());
        item.setAnhBiaUrl(sach.getAnhBiaUrl());
        item.setTenTacGia(sach.getTacGia().getTenTacGia());
        item.setDonGia(unitPrice);
        item.setSoLuong(soLuong);
        chiTietDonHangRepository.saveAndFlush(item);

        LichSuDonHang timeline = appendTimeline(
                order,
                TrangThaiDon.CHO_XAC_NHAN,
                "Đặt hàng thành công (qua Tư vấn viên AI)",
                "Đơn hàng " + order.getMaDonHang() + " đã được Tư vấn viên AI tạo theo xác nhận của khách hàng.",
                now
        );

        BigDecimal currentSpent = customer.getTongChiTieu() != null ? customer.getTongChiTieu() : BigDecimal.ZERO;
        customer.setTongChiTieu(currentSpent.add(tongThanhToan));
        customer.setSoDonHang(safeInt(customer.getSoDonHang()) + 1);
        tkNguoiDungRepository.saveAndFlush(customer);

        recommendationTrackingService.recordAutoAction(customer, sach, LoaiHanhVi.MUA_HANG);

        createOrderNotification(
                customer,
                "Đặt hàng thành công " + order.getMaDonHang(),
                "Đơn hàng " + order.getMaDonHang() + " trị giá " + tongThanhToan.toPlainString()
                        + "đ đã được đặt qua Tư vấn viên AI.",
                now
        );

        return toOrderDto(order, List.of(item), List.of(timeline));
    }

    // =========================================================================
    // PHẦN 2: KHÁCH HÀNG XEM ĐƠN, TIMELINE & HỦY ĐƠN (MOBILE)
    // =========================================================================

    /**
     * Lấy danh sách đơn hàng của chính khách hàng đang đăng nhập (có thể lọc theo tab trạng thái).
     */
    @Transactional(readOnly = true)
    public List<OrderDto> getCustomerOrders(String nguoiDungId, TrangThaiDon trangThai) {
        requireActiveCustomer(nguoiDungId);
        List<DonHang> orders = donHangRepository.findByNguoiDungIdOrdered(nguoiDungId)
                .stream()
                .filter(order -> trangThai == null || order.getTrangThaiDon() == trangThai)
                .toList();
        return mapOrdersBatch(orders);
    }

    /**
     * Xem chi tiết 1 đơn hàng + hành trình Timeline (chặn xem đơn của khách khác - chống IDOR).
     */
    @Transactional(readOnly = true)
    public OrderDto getCustomerOrderDetail(String nguoiDungId, String orderId) {
        requireActiveCustomer(nguoiDungId);
        DonHang order = donHangRepository.findByIdAndNguoiDungId(orderId, nguoiDungId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng hoặc bạn không có quyền xem đơn hàng này."
                ));
        List<ChiTietDonHang> items = chiTietDonHangRepository.findByDonHangIdWithSach(order.getId());
        List<LichSuDonHang> timeline = lichSuDonHangRepository.findByDonHangIdOrdered(order.getId());
        return toOrderDto(order, items, timeline);
    }

    /**
     * Khách hàng hủy đơn hàng:
     * - Chỉ cho phép hủy khi đơn đang ở CHO_XAC_NHAN.
     * - Hoàn lại đầy đủ tồn kho, đã bán, lượt voucher, tổng chi tiêu/số đơn và trạng thái hoàn tiền nếu đã thanh toán trước.
     */
    @Transactional
    public OrderDto cancelCustomerOrder(String nguoiDungId, String orderId, CancelOrderRequest request) {
        requireActiveCustomer(nguoiDungId);
        DonHang order = donHangRepository.findByIdAndNguoiDungId(orderId, nguoiDungId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng hoặc bạn không có quyền hủy đơn hàng này."
                ));

        if (order.getTrangThaiDon() != TrangThaiDon.CHO_XAC_NHAN) {
            throw new BusinessException(
                    "Chỉ được phép hủy đơn hàng khi đơn đang ở trạng thái Chờ xác nhận."
            );
        }

        String reason = request.lyDo().trim();
        return executeOrderCancellation(order, "Khách hàng hủy đơn. Lý do: " + reason);
    }

    /**
     * Phương án 1 — Khách hàng bấm "Đã nhận được hàng" trên Mobile App:
     * - Chỉ cho phép chính chủ đơn hàng (anti-IDOR qua findByIdAndNguoiDungId) và chỉ khi đơn đang DANG_GIAO.
     * - Tự động chuyển trangThaiDon -> DA_GIAO, nếu thanh toán COD thì tự đổi sang DA_THANH_TOAN,
     *   ghi mốc Timeline xác nhận của khách và gửi thông báo để khách có thể Đánh giá / Đổi trả ngay.
     */
    @Transactional
    public OrderDto confirmOrderReceived(String nguoiDungId, String orderId) {
        requireActiveCustomer(nguoiDungId);
        DonHang order = donHangRepository.findByIdAndNguoiDungId(orderId, nguoiDungId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng hoặc bạn không có quyền xác nhận đơn hàng này."
                ));

        if (order.getTrangThaiDon() != TrangThaiDon.DANG_GIAO) {
            throw new BusinessException(
                    "Chỉ có thể xác nhận đã nhận hàng khi đơn đang ở trạng thái Đang giao hàng."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        completeDelivery(
                order,
                "Giao hàng thành công",
                "Khách hàng xác nhận đã nhận đủ sách qua ứng dụng.",
                now
        );

        List<ChiTietDonHang> items = chiTietDonHangRepository.findByDonHangIdWithSach(order.getId());
        List<LichSuDonHang> timeline = lichSuDonHangRepository.findByDonHangIdOrdered(order.getId());
        return toOrderDto(order, items, timeline);
    }

    /**
     * Phương án 2 — Hệ thống tự động hoàn tất các đơn DANG_GIAO quá hạn (được OrderAutoCompleteScheduler gọi định kỳ):
     * - Tìm tất cả đơn hàng đang ở trạng thái DANG_GIAO.
     * - Xác định thời điểm đơn bắt đầu chuyển sang DANG_GIAO trong lich_su_don_hang (nếu không có thì lấy ngayTao).
     * - Nếu thời gian hiện tại >= thời điểm giao + delayMinutes -> tự chốt DA_GIAO, đổi COD thành DA_THANH_TOAN,
     *   ghi mốc Timeline tự động và gửi thông báo tới khách hàng.
     *
     * @return Số lượng đơn hàng đã được tự động chốt thành công trong lần quét này.
     */
    @Transactional
    public int autoCompleteOverdueShippingOrders(long delayMinutes) {
        long safeDelayMinutes = Math.max(1L, delayMinutes);
        List<DonHang> shippingOrders = donHangRepository.findByTrangThaiDonWithUser(TrangThaiDon.DANG_GIAO);
        if (shippingOrders.isEmpty()) {
            return 0;
        }

        // Nạp trước toàn bộ lịch sử đơn hàng để tránh query lặp N+1 trong vòng lặp kiểm tra
        List<String> orderIds = shippingOrders.stream().map(DonHang::getId).toList();
        Map<String, List<LichSuDonHang>> timelinesByOrder = new HashMap<>();
        for (LichSuDonHang event : lichSuDonHangRepository.findByDonHangIdInOrdered(orderIds)) {
            timelinesByOrder.computeIfAbsent(event.getDonHang().getId(), k -> new ArrayList<>()).add(event);
        }

        LocalDateTime now = LocalDateTime.now();
        int completedCount = 0;

        for (DonHang order : shippingOrders) {
            LocalDateTime shippedAt = resolveShippedAt(order, timelinesByOrder.getOrDefault(order.getId(), List.of()));
            if (!shippedAt.plusMinutes(safeDelayMinutes).isAfter(now)) {
                completeDelivery(
                        order,
                        "Giao hàng thành công",
                        "Hệ thống tự động hoàn tất đơn hàng sau thời gian chờ xác nhận nhận hàng.",
                        now
                );
                completedCount++;
            }
        }

        return completedCount;
    }

    // =========================================================================
    // PHẦN 3: ADMIN / KHO / CSKH QUẢN LÝ & DUYỆT TRẠNG THÁI ĐƠN (WEB ADMIN)
    // =========================================================================

    /**
     * Lấy toàn bộ đơn hàng cho Web Admin (hỗ trợ lọc theo trạng thái và tìm theo mã đơn/tên/SĐT khách).
     */
    @Transactional(readOnly = true)
    public List<OrderDto> getAdminOrders(TrangThaiDon trangThai, String keyword) {
        String normalizedKeyword = normalizeBlankToNull(keyword);
        String lowerKeyword = normalizedKeyword == null ? null : normalizedKeyword.toLowerCase(Locale.ROOT);

        List<DonHang> orders = donHangRepository.findAllOrdered().stream()
                .filter(order -> trangThai == null || order.getTrangThaiDon() == trangThai)
                .filter(order -> {
                    if (lowerKeyword == null) {
                        return true;
                    }
                    return containsIgnoreCase(order.getMaDonHang(), lowerKeyword)
                            || containsIgnoreCase(order.getTenKhachHang(), lowerKeyword)
                            || containsIgnoreCase(order.getSdtKhachHang(), lowerKeyword);
                })
                .toList();
        return mapOrdersBatch(orders);
    }

    /**
     * Xem chi tiết 1 đơn hàng phía Web Admin.
     */
    @Transactional(readOnly = true)
    public OrderDto getAdminOrderDetail(String orderId) {
        DonHang order = donHangRepository.findByIdWithUser(orderId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
        List<ChiTietDonHang> items = chiTietDonHangRepository.findByDonHangIdWithSach(order.getId());
        List<LichSuDonHang> timeline = lichSuDonHangRepository.findByDonHangIdOrdered(order.getId());
        return toOrderDto(order, items, timeline);
    }

    /**
     * Chuyển trạng thái đơn hàng 1 chiều theo đúng ma trận phân quyền RBAC trên Web Admin:
     * - ADMIN: toàn quyền mọi chuyển trạng thái hợp lệ + hủy ở CHO_XAC_NHAN hoặc DANG_DONG_GOI.
     * - WAREHOUSE_MANAGER: chỉ được chuyển CHO_XAC_NHAN -> DANG_DONG_GOI và DANG_DONG_GOI -> DANG_GIAO (403 với DA_GIAO / DA_HUY).
     * - CSKH: được xác nhận đơn (CHO_XAC_NHAN -> DANG_DONG_GOI), hủy đơn ở CHO_XAC_NHAN/DANG_DONG_GOI và xác nhận DA_GIAO;
     *         bị chặn 403 khi tự xuất kho DANG_DONG_GOI -> DANG_GIAO.
     */
    @Transactional
    public OrderDto updateOrderStatusByStaff(
            String orderId,
            UpdateOrderStatusRequest request,
            VaiTro actorRole
    ) {
        DonHang order = donHangRepository.findByIdWithUser(orderId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));

        TrangThaiDon current = order.getTrangThaiDon();
        TrangThaiDon next = request.trangThaiMoi();

        if (current == next) {
            throw new BusinessException("Đơn hàng đã ở trạng thái " + current.name() + ".");
        }
        if (current == TrangThaiDon.DA_GIAO || current == TrangThaiDon.DA_HUY) {
            throw new BusinessException(
                    "Đơn hàng đã ở trạng thái kết thúc (" + current.name() + "), không thể cập nhật thêm."
            );
        }

        // Nhánh hủy đơn phía Admin / CSKH
        if (next == TrangThaiDon.DA_HUY) {
            if (actorRole == VaiTro.WAREHOUSE_MANAGER) {
                throw new BusinessException(
                        HttpStatus.FORBIDDEN,
                        "Quản lý Kho không có quyền hủy đơn hàng."
                );
            }
            if (current != TrangThaiDon.CHO_XAC_NHAN && current != TrangThaiDon.DANG_DONG_GOI) {
                throw new BusinessException(
                        "Không thể hủy đơn hàng khi đơn đã bàn giao vận chuyển (" + current.name() + ")."
                );
            }
            String note = normalizeBlankToNull(request.ghiChu());
            String reason = note != null ? "Nhân sự hủy đơn: " + note : "Đơn hàng đã bị hủy bởi quản trị viên.";
            return executeOrderCancellation(order, reason);
        }

        // Kiểm tra quy tắc chuyển trạng thái 1 chiều tuần tự
        boolean validForwardEdge =
                (current == TrangThaiDon.CHO_XAC_NHAN && next == TrangThaiDon.DANG_DONG_GOI)
                || (current == TrangThaiDon.DANG_DONG_GOI && next == TrangThaiDon.DANG_GIAO)
                || (current == TrangThaiDon.DANG_GIAO && next == TrangThaiDon.DA_GIAO);

        if (!validForwardEdge) {
            throw new BusinessException(
                    "Không thể chuyển trạng thái đơn hàng từ " + current.name() + " sang " + next.name()
                            + ". Quy trình chỉ cho phép đi 1 chiều tuần tự."
            );
        }

        // Siết phân quyền RBAC chi tiết theo từng bước
        if (next == TrangThaiDon.DANG_GIAO && actorRole == VaiTro.CSKH) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Nhân viên CSKH không có quyền chuyển đơn sang Đang giao hàng (thuộc nghiệp vụ Kho)."
            );
        }
        if (next == TrangThaiDon.DA_GIAO && actorRole == VaiTro.WAREHOUSE_MANAGER) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Quản lý Kho không có quyền xác nhận giao hàng thành công."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        String note = normalizeBlankToNull(request.ghiChu());
        String title = resolveMilestoneTitle(next);
        String description = note != null ? note : resolveDefaultMilestoneDescription(next);

        if (next == TrangThaiDon.DA_GIAO) {
            completeDelivery(order, title, description, now);
        } else {
            order.setTrangThaiDon(next);
            donHangRepository.saveAndFlush(order);
            appendTimeline(order, next, title, description, now);
            createOrderNotification(
                    order.getNguoiDung(),
                    title + " - " + order.getMaDonHang(),
                    "Đơn hàng " + order.getMaDonHang() + ": " + description,
                    now
            );
        }

        List<ChiTietDonHang> items = chiTietDonHangRepository.findByDonHangIdWithSach(order.getId());
        List<LichSuDonHang> timeline = lichSuDonHangRepository.findByDonHangIdOrdered(order.getId());
        return toOrderDto(order, items, timeline);
    }

    // =========================================================================
    // HÀM DÙNG CHUNG: CHỐT GIAO THÀNH CÔNG, HOÀN DỮ LIỆU KHI HỦY ĐƠN & DỰNG DTO
    // =========================================================================

    /**
     * Dùng chung cho cả 3 luồng chốt DA_GIAO (Khách bấm Đã nhận hàng, Scheduler tự động, Admin/CSKH xác nhận):
     * 1. Chuyển trangThaiDon = DA_GIAO.
     * 2. Nếu là đơn COD đang CHUA_THANH_TOAN -> tự động đổi sang DA_THANH_TOAN.
     * 3. Ghi nhận 1 mốc Timeline vào lich_su_don_hang và gửi 1 ThongBao tới khách hàng.
     */
    private void completeDelivery(
            DonHang order,
            String milestoneTitle,
            String milestoneDescription,
            LocalDateTime timestamp
    ) {
        order.setTrangThaiDon(TrangThaiDon.DA_GIAO);
        if (order.getTrangThaiThanhToan() == TrangThaiThanhToan.CHUA_THANH_TOAN) {
            order.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
        }
        donHangRepository.saveAndFlush(order);

        appendTimeline(order, TrangThaiDon.DA_GIAO, milestoneTitle, milestoneDescription, timestamp);
        createOrderNotification(
                order.getNguoiDung(),
                milestoneTitle + " - " + order.getMaDonHang(),
                "Đơn hàng " + order.getMaDonHang() + ": " + milestoneDescription,
                timestamp
        );
    }

    private LocalDateTime resolveShippedAt(DonHang order, List<LichSuDonHang> events) {
        LocalDateTime latestShippingTime = null;
        for (LichSuDonHang event : events) {
            if (TrangThaiDon.DANG_GIAO.name().equals(event.getTrangThai())) {
                LocalDateTime parsed = parseTimelineTimestamp(event.getThoiGian());
                if (parsed != null && (latestShippingTime == null || parsed.isAfter(latestShippingTime))) {
                    latestShippingTime = parsed;
                }
            }
        }
        return latestShippingTime != null ? latestShippingTime : order.getNgayTao();
    }

    private LocalDateTime parseTimelineTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        try {
            return LocalDateTime.parse(trimmed, TIMELINE_TIME_FMT);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(trimmed, LEGACY_TIMELINE_TIME_FMT);
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    /**
     * Hoàn toàn bộ dữ liệu khi hủy đơn (áp dụng thống nhất cho cả khách tự hủy và Admin/CSKH hủy):
     * 1. Cộng lại sach.tonKho và trừ lại sach.daBan.
     * 2. Hoàn 1 lượt maGiamGia.daSuDung (nếu đơn có áp dụng voucher).
     * 3. Trừ lại tongChiTieu và soDonHang của khách hàng (không âm).
     * 4. Nếu đơn đã thanh toán trước (DA_THANH_TOAN), chuyển sang DA_HOAN_TIEN.
     * 5. Đổi trangThaiDon = DA_HUY, ghi mốc LichSuDonHang và tạo ThongBao cho khách.
     */
    private OrderDto executeOrderCancellation(DonHang order, String cancelDescription) {
        List<ChiTietDonHang> items = chiTietDonHangRepository.findByDonHangIdWithSach(order.getId());
        List<Sach> restoredBooks = new ArrayList<>();

        for (ChiTietDonHang item : items) {
            Sach sach = item.getSach();
            int qty = safeInt(item.getSoLuong());
            sach.setTonKho(safeInt(sach.getTonKho()) + qty);
            sach.setDaBan(Math.max(0, safeInt(sach.getDaBan()) - qty));
            restoredBooks.add(sach);
        }
        sachRepository.saveAllAndFlush(restoredBooks);

        if (order.getMaGiamGiaCode() != null && !order.getMaGiamGiaCode().isBlank()) {
            maGiamGiaRepository.findByMaCodeWithDanhMucIgnoreCase(order.getMaGiamGiaCode())
                    .ifPresent(voucher -> {
                        voucher.setDaSuDung(Math.max(0, safeInt(voucher.getDaSuDung()) - 1));
                        maGiamGiaRepository.saveAndFlush(voucher);
                    });
        }

        TkNguoiDung customer = order.getNguoiDung();
        BigDecimal currentSpent = customer.getTongChiTieu() != null ? customer.getTongChiTieu() : BigDecimal.ZERO;
        BigDecimal reducedSpent = currentSpent.subtract(
                order.getTongThanhToan() != null ? order.getTongThanhToan() : BigDecimal.ZERO
        );
        customer.setTongChiTieu(reducedSpent.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : reducedSpent);
        customer.setSoDonHang(Math.max(0, safeInt(customer.getSoDonHang()) - 1));
        tkNguoiDungRepository.saveAndFlush(customer);

        if (order.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
            order.setTrangThaiThanhToan(TrangThaiThanhToan.DA_HOAN_TIEN);
        }
        order.setTrangThaiDon(TrangThaiDon.DA_HUY);
        donHangRepository.saveAndFlush(order);

        LocalDateTime now = LocalDateTime.now();
        appendTimeline(order, TrangThaiDon.DA_HUY, "Đơn hàng đã hủy", cancelDescription, now);
        createOrderNotification(
                customer,
                "Đơn hàng " + order.getMaDonHang() + " đã hủy",
                cancelDescription,
                now
        );

        List<LichSuDonHang> timeline = lichSuDonHangRepository.findByDonHangIdOrdered(order.getId());
        return toOrderDto(order, items, timeline);
    }

    private List<OrderDto> mapOrdersBatch(List<DonHang> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<String> orderIds = orders.stream().map(DonHang::getId).toList();

        Map<String, List<ChiTietDonHang>> itemsByOrder = new HashMap<>();
        for (ChiTietDonHang item : chiTietDonHangRepository.findByDonHangIdInWithSach(orderIds)) {
            itemsByOrder.computeIfAbsent(item.getDonHang().getId(), k -> new ArrayList<>()).add(item);
        }

        Map<String, List<LichSuDonHang>> timelineByOrder = new HashMap<>();
        for (LichSuDonHang event : lichSuDonHangRepository.findByDonHangIdInOrdered(orderIds)) {
            timelineByOrder.computeIfAbsent(event.getDonHang().getId(), k -> new ArrayList<>()).add(event);
        }

        Map<String, YeuCauDoiTra> returnByOrder = new HashMap<>();
        for (YeuCauDoiTra req : yeuCauDoiTraRepository.findByDonHangIdInWithOrder(orderIds)) {
            returnByOrder.put(req.getDonHang().getId(), req);
        }

        return orders.stream()
                .map(order -> toOrderDto(
                        order,
                        itemsByOrder.getOrDefault(order.getId(), List.of()),
                        timelineByOrder.getOrDefault(order.getId(), List.of()),
                        returnByOrder.get(order.getId())
                ))
                .toList();
    }

    private OrderDto toOrderDto(
            DonHang order,
            List<ChiTietDonHang> items,
            List<LichSuDonHang> timeline
    ) {
        YeuCauDoiTra returnReq = yeuCauDoiTraRepository.findByDonHangIdWithOrder(order.getId()).orElse(null);
        return toOrderDto(order, items, timeline, returnReq);
    }

    private OrderDto toOrderDto(
            DonHang order,
            List<ChiTietDonHang> items,
            List<LichSuDonHang> timeline,
            YeuCauDoiTra returnRequest
    ) {
        List<OrderItemDto> itemDtos = items.stream()
                .map(item -> {
                    BigDecimal donGia = item.getDonGia() != null ? item.getDonGia() : BigDecimal.ZERO;
                    int qty = safeInt(item.getSoLuong());
                    return new OrderItemDto(
                            item.getId(),
                            item.getSach().getId(),
                            item.getTieuDeSach(),
                            item.getAnhBiaUrl(),
                            item.getTenTacGia(),
                            donGia,
                            qty,
                            donGia.multiply(BigDecimal.valueOf(qty))
                    );
                })
                .toList();

        List<OrderTimelineDto> timelineDtos = timeline.stream()
                .map(event -> new OrderTimelineDto(
                        event.getId(),
                        event.getTrangThai(),
                        event.getTieuDe(),
                        event.getMoTa(),
                        event.getThoiGian()
                ))
                .toList();

        ReturnRequestDto returnDto = returnRequest == null ? null : new ReturnRequestDto(
                returnRequest.getId(),
                order.getId(),
                order.getMaDonHang(),
                order.getTenKhachHang(),
                returnRequest.getLyDo(),
                returnRequest.getMoTa(),
                splitUrls(returnRequest.getAnhMinhChungUrls()),
                returnRequest.getTrangThai(),
                returnRequest.getGhiChuAdmin(),
                returnRequest.getNgayTao(),
                returnRequest.getNgayXuLy()
        );

        return new OrderDto(
                order.getId(),
                order.getMaDonHang(),
                order.getNguoiDung().getId(),
                order.getTenKhachHang(),
                order.getSdtKhachHang(),
                order.getDiaChiGiaoHang(),
                order.getTamTinh(),
                order.getPhiVanChuyen(),
                order.getTienGiamGia(),
                order.getMaGiamGiaCode(),
                order.getTongThanhToan(),
                order.getPhuongThucThanhToan(),
                order.getTrangThaiThanhToan(),
                order.getTrangThaiDon(),
                order.getGhiChu(),
                Boolean.TRUE.equals(order.getDaDanhGia()),
                order.getNgayTao(),
                itemDtos,
                timelineDtos,
                returnDto
        );
    }

    private List<String> splitUrls(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private LichSuDonHang appendTimeline(
            DonHang order,
            TrangThaiDon status,
            String title,
            String description,
            LocalDateTime timestamp
    ) {
        LichSuDonHang event = new LichSuDonHang();
        event.setId("ls_" + shortUuid());
        event.setDonHang(order);
        event.setTrangThai(status.name());
        event.setTieuDe(title);
        event.setMoTa(description);
        event.setThoiGian(timestamp.format(TIMELINE_TIME_FMT));
        return lichSuDonHangRepository.saveAndFlush(event);
    }

    private void createOrderNotification(
            TkNguoiDung customer,
            String title,
            String content,
            LocalDateTime timestamp
    ) {
        ThongBao notification = new ThongBao();
        notification.setId("tb_" + shortUuid());
        notification.setNguoiDung(customer);
        notification.setTieuDe(title);
        notification.setNoiDung(content);
        notification.setLoaiThongBao(LoaiThongBao.DON_HANG);
        notification.setDaDoc(Boolean.FALSE);
        notification.setNgayTao(timestamp);
        thongBaoRepository.saveAndFlush(notification);
    }

    private TkNguoiDung requireActiveCustomer(String nguoiDungId) {
        TkNguoiDung user = tkNguoiDungRepository.findById(nguoiDungId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "Phiên đăng nhập không hợp lệ hoặc tài khoản không tồn tại."
                ));
        if (Boolean.TRUE.equals(user.getBiKhoa())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ CSKH để được hỗ trợ."
            );
        }
        return user;
    }

    private SoDiaChi resolveCustomerAddress(String nguoiDungId, String soDiaChiId) {
        String normalizedId = normalizeBlankToNull(soDiaChiId);
        if (normalizedId != null) {
            return soDiaChiRepository.findByIdAndNguoiDungId(normalizedId, nguoiDungId)
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "Không tìm thấy địa chỉ giao hàng hoặc địa chỉ không thuộc về bạn."
                    ));
        }

        List<SoDiaChi> addresses = soDiaChiRepository.findByNguoiDungIdOrderByMacDinhDescIdAsc(nguoiDungId);
        if (addresses.isEmpty()) {
            throw new BusinessException(
                    "Bạn chưa có địa chỉ nhận hàng. Vui lòng thêm địa chỉ trước khi đặt hàng."
            );
        }
        return addresses.get(0);
    }

    private String formatFullAddress(SoDiaChi address) {
        return String.join(", ",
                address.getDiaChiChiTiet(),
                address.getPhuongXa(),
                address.getQuanHuyen(),
                address.getTinhThanh()
        );
    }

    private BigDecimal resolveEffectivePrice(Sach sach) {
        if (Boolean.TRUE.equals(sach.getDangFlashSale())
                && sach.getGiaFlashSale() != null
                && sach.getGiaFlashSale().compareTo(BigDecimal.ZERO) > 0) {
            return sach.getGiaFlashSale();
        }
        return sach.getGiaBan() != null ? sach.getGiaBan() : BigDecimal.ZERO;
    }

    private String generateUniqueOrderCode() {
        String candidate;
        do {
            candidate = "DH-" + shortUuid().substring(0, 6).toUpperCase(Locale.ROOT);
        } while (donHangRepository.existsByMaDonHangIgnoreCase(candidate));
        return candidate;
    }

    private String resolveMilestoneTitle(TrangThaiDon status) {
        return switch (status) {
            case CHO_XAC_NHAN -> "Đặt hàng thành công";
            case DANG_DONG_GOI -> "Đang đóng gói";
            case DANG_GIAO -> "Đang giao hàng";
            case DA_GIAO -> "Giao hàng thành công";
            case DA_HUY -> "Đơn hàng đã hủy";
        };
    }

    private String resolveDefaultMilestoneDescription(TrangThaiDon status) {
        return switch (status) {
            case CHO_XAC_NHAN -> "Đơn hàng đang chờ xác nhận.";
            case DANG_DONG_GOI -> "Kho đã xác nhận và đang tiến hành đóng gói sách.";
            case DANG_GIAO -> "Đơn hàng đã bàn giao cho đơn vị vận chuyển.";
            case DA_GIAO -> "Đơn hàng đã được giao thành công tới người nhận.";
            case DA_HUY -> "Đơn hàng đã bị hủy.";
        };
    }

    private String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String normalizeBlankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean containsIgnoreCase(String value, String lowerKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
