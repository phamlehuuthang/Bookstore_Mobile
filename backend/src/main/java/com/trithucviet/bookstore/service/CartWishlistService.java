package com.trithucviet.bookstore.service;

import com.trithucviet.bookstore.dto.request.CartItemRequest;
import com.trithucviet.bookstore.dto.request.CartUpdateRequest;
import com.trithucviet.bookstore.dto.response.CartResponseDto;
import com.trithucviet.bookstore.dto.response.WishlistItemResponse;
import com.trithucviet.bookstore.dto.response.WishlistToggleResponse;
import com.trithucviet.bookstore.entity.GioHang;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.entity.SachYeuThich;
import com.trithucviet.bookstore.entity.TkNguoiDung;
import com.trithucviet.bookstore.entity.enums.LoaiHanhVi;
import com.trithucviet.bookstore.exception.BusinessException;
import com.trithucviet.bookstore.repository.GioHangRepository;
import com.trithucviet.bookstore.repository.SachRepository;
import com.trithucviet.bookstore.repository.SachYeuThichRepository;
import com.trithucviet.bookstore.repository.TkNguoiDungRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ Tủ sách yêu thích (Bước 4 - phần 1) và Giỏ hàng (Bước 4 - phần 2).
 */
@Service
public class CartWishlistService {

    // Mốc miễn phí vận chuyển tiêu chuẩn và phí ship mặc định theo PHAN_TICH_NGHIEP_VU_VA_CSDL.md
    private static final BigDecimal NGUONG_MIEN_PHI_SHIP = BigDecimal.valueOf(300_000L);
    private static final BigDecimal PHI_VAN_CHUYEN_MAC_DINH = BigDecimal.valueOf(25_000L);

    private final SachYeuThichRepository sachYeuThichRepository;
    private final GioHangRepository gioHangRepository;
    private final SachRepository sachRepository;
    private final TkNguoiDungRepository tkNguoiDungRepository;
    private final RecommendationTrackingService recommendationTrackingService;

    public CartWishlistService(
            SachYeuThichRepository sachYeuThichRepository,
            GioHangRepository gioHangRepository,
            SachRepository sachRepository,
            TkNguoiDungRepository tkNguoiDungRepository,
            RecommendationTrackingService recommendationTrackingService
    ) {
        this.sachYeuThichRepository = sachYeuThichRepository;
        this.gioHangRepository = gioHangRepository;
        this.sachRepository = sachRepository;
        this.tkNguoiDungRepository = tkNguoiDungRepository;
        this.recommendationTrackingService = recommendationTrackingService;
    }

    // readOnly = true giúp Hibernate bỏ qua cơ chế dirty-checking, tối ưu hiệu năng cho thao tác chỉ đọc
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(String nguoiDungId) {
        getActiveCustomer(nguoiDungId);
        return sachYeuThichRepository.findAllWithSachByNguoiDungId(nguoiDungId)
                .stream()
                .map(this::toItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WishlistToggleResponse getStatus(String nguoiDungId, String sachId) {
        getActiveCustomer(nguoiDungId);
        getVisibleBook(sachId);
        boolean daYeuThich = sachYeuThichRepository.existsById(new SachYeuThich.SachYeuThichId(nguoiDungId, sachId));
        return new WishlistToggleResponse(sachId, daYeuThich, sachYeuThichRepository.countByNguoiDungId(nguoiDungId));
    }

    /**
     * Đổi trạng thái yêu thích: thả tim nếu chưa có, bỏ tim nếu đã có.
     * Toàn bộ thao tác nằm trong 1 @Transactional để đảm bảo kiểm tra + ghi/xóa + đếm lại nhất quán.
     */
    @Transactional
    public WishlistToggleResponse toggle(String nguoiDungId, String sachId) {
        TkNguoiDung nguoiDung = getActiveCustomer(nguoiDungId);
        // Bảng sach_yeu_thich dùng khóa chính kép (nguoi_dung_id, sach_id) -> vừa chống trùng, vừa tra cứu O(1)
        SachYeuThich.SachYeuThichId id = new SachYeuThich.SachYeuThichId(nguoiDungId, sachId);

        boolean daYeuThich;
        if (sachYeuThichRepository.existsById(id)) {
            // Cố tình cho bỏ tim ngay mà không kiểm tra sach.hienThi:
            // nếu sách từng được thích trước đây rồi sau đó Admin ẩn đi, khách vẫn gỡ được khỏi tủ của mình.
            sachYeuThichRepository.deleteById(id);
            daYeuThich = false;
        } else {
            // Chỉ khi thêm mới (thả tim) mới bắt buộc sách phải tồn tại và còn đang kinh doanh (hienThi = true)
            Sach sach = getVisibleBook(sachId);
            SachYeuThich yeuThich = new SachYeuThich();
            yeuThich.setId(id);
            yeuThich.setNguoiDung(nguoiDung);
            yeuThich.setSach(sach);
            sachYeuThichRepository.save(yeuThich);
            // Cách B: Tự động ghi nhận hành vi THEM_YEU_THICH (+4 điểm) vào bảng hanh_vi_nguoi_dung
            recommendationTrackingService.recordAutoAction(nguoiDung, sach, LoaiHanhVi.THEM_YEU_THICH);
            daYeuThich = true;
        }

        return new WishlistToggleResponse(sachId, daYeuThich, sachYeuThichRepository.countByNguoiDungId(nguoiDungId));
    }

    /**
     * Xóa tường minh 1 cuốn sách khỏi tủ yêu thích (endpoint RESTful bổ sung bên cạnh toggle).
     * Khác với toggle: nếu khách lỡ bấm đúp 2 lần khi mạng chậm sẽ báo 404 chứ không tự thêm ngược lại.
     */
    @Transactional
    public WishlistToggleResponse remove(String nguoiDungId, String sachId) {
        getActiveCustomer(nguoiDungId);
        SachYeuThich.SachYeuThichId id = new SachYeuThich.SachYeuThichId(nguoiDungId, sachId);
        if (!sachYeuThichRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Cuốn sách này không có trong Tủ sách yêu thích của bạn.");
        }
        sachYeuThichRepository.deleteById(id);
        return new WishlistToggleResponse(sachId, false, sachYeuThichRepository.countByNguoiDungId(nguoiDungId));
    }

    // =========================================================================
    // PHẦN 2: GIỎ HÀNG (BƯỚC 4 - MỤC 5.1)
    // =========================================================================

    // readOnly = true tối ưu hiệu năng khi chỉ đọc giỏ hàng và tính lại bảng tạm tính theo giá/tồn kho mới nhất
    @Transactional(readOnly = true)
    public CartResponseDto getCart(String nguoiDungId) {
        getActiveCustomer(nguoiDungId);
        return buildCartResponse(nguoiDungId);
    }

    /**
     * Thêm sách vào giỏ hàng:
     * - Nếu cuốn sách chưa có trong giỏ: tạo dòng mới và mặc định tick chọn (daChon = true).
     * - Nếu cuốn sách đã có trong giỏ: cộng dồn số lượng mới vào số lượng cũ, bật lại daChon = true
     *   (giống hành vi useCart.tsx trên Mobile) và kiểm tra tổng cộng dồn không được vượt quá sach.tonKho.
     */
    @Transactional
    public CartResponseDto addToCart(String nguoiDungId, CartItemRequest request) {
        TkNguoiDung nguoiDung = getActiveCustomer(nguoiDungId);
        String sachId = request.sachId().trim();
        Sach sach = getVisibleBook(sachId);

        GioHang.GioHangId id = new GioHang.GioHangId(nguoiDungId, sachId);
        GioHang item = gioHangRepository.findById(id).orElse(null);

        int soLuongCu = item == null ? 0 : item.getSoLuong();
        int soLuongMoi = soLuongCu + request.soLuong();
        // Chặn ngay nếu sách hết hàng hoặc tổng số lượng sau khi cộng dồn vượt quá tồn kho thực tế
        validateStockForCart(sach, soLuongMoi);

        if (item == null) {
            item = new GioHang();
            item.setId(id);
            item.setNguoiDung(nguoiDung);
            item.setSach(sach);
        }
        item.setSoLuong(soLuongMoi);
        item.setDaChon(true);
        item.setNgayCapNhat(LocalDateTime.now());
        // saveAndFlush đẩy thay đổi xuống DB ngay để câu JOIN FETCH kế tiếp đọc được dữ liệu vừa ghi
        gioHangRepository.saveAndFlush(item);
        // Cách B: Tự động ghi nhận hành vi THEM_GIO_HANG (+6 điểm) vào bảng hanh_vi_nguoi_dung
        recommendationTrackingService.recordAutoAction(nguoiDung, sach, LoaiHanhVi.THEM_GIO_HANG);

        return buildCartResponse(nguoiDungId);
    }

    /**
     * Cập nhật số lượng (>= 1) hoặc trạng thái tick chọn (daChon) của 1 cuốn sách trong giỏ.
     * Quy tắc tách bạch với DELETE:
     * - Nếu sửa soLuong hoặc bật tick chọn (daChon = true) -> bắt buộc sách phải còn kinh doanh và đủ tồn kho.
     * - Nếu khách chỉ bỏ tick (daChon = false) -> luôn cho phép, kể cả khi sách trong kho vừa hết hàng/bị ẩn.
     */
    @Transactional
    public CartResponseDto updateCartItem(String nguoiDungId, String sachId, CartUpdateRequest request) {
        getActiveCustomer(nguoiDungId);
        if (request.soLuong() == null && request.daChon() == null) {
            throw new BusinessException("Vui lòng truyền số lượng hoặc trạng thái chọn cần cập nhật.");
        }

        GioHang.GioHangId id = new GioHang.GioHangId(nguoiDungId, sachId);
        GioHang item = gioHangRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Cuốn sách này không có trong giỏ hàng của bạn."));

        if (request.soLuong() != null || Boolean.TRUE.equals(request.daChon())) {
            Sach sach = getVisibleBook(sachId);
            int soLuongMucTieu = request.soLuong() != null ? request.soLuong() : item.getSoLuong();
            validateStockForCart(sach, soLuongMucTieu);
        }

        if (request.soLuong() != null) {
            item.setSoLuong(request.soLuong());
        }
        if (request.daChon() != null) {
            item.setDaChon(request.daChon());
        }
        item.setNgayCapNhat(LocalDateTime.now());
        gioHangRepository.saveAndFlush(item);

        return buildCartResponse(nguoiDungId);
    }

    /**
     * Chọn tất cả hoặc bỏ chọn tất cả sản phẩm trong giỏ hàng bằng 1 câu UPDATE hàng loạt.
     * Khi bấm "Chọn tất cả" (daChon = true), hệ thống rà soát trước để không cho chọn nhầm sách
     * đã ngừng kinh doanh hoặc vượt quá tồn kho hiện tại.
     */
    @Transactional
    public CartResponseDto selectAllCartItems(String nguoiDungId, boolean daChon) {
        getActiveCustomer(nguoiDungId);
        List<GioHang> items = gioHangRepository.findAllWithSachByNguoiDungId(nguoiDungId);

        if (!items.isEmpty()) {
            if (daChon) {
                for (GioHang item : items) {
                    Sach sach = item.getSach();
                    if (!Boolean.TRUE.equals(sach.getHienThi())) {
                        throw new BusinessException(
                                "Sách '" + sach.getTieuDe() + "' hiện đã ngừng kinh doanh, vui lòng xóa khỏi giỏ trước khi chọn thanh toán."
                        );
                    }
                    validateStockForCart(sach, item.getSoLuong());
                }
            }
            gioHangRepository.updateDaChonByNguoiDungId(nguoiDungId, daChon);
        }

        return buildCartResponse(nguoiDungId);
    }

    /**
     * Xóa 1 cuốn sách khỏi giỏ hàng (không kiểm tra tồn kho hay hienThi để khách luôn dọn được món cũ).
     */
    @Transactional
    public CartResponseDto removeCartItem(String nguoiDungId, String sachId) {
        getActiveCustomer(nguoiDungId);
        GioHang.GioHangId id = new GioHang.GioHangId(nguoiDungId, sachId);
        if (!gioHangRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Cuốn sách này không có trong giỏ hàng của bạn.");
        }
        gioHangRepository.deleteById(id);
        gioHangRepository.flush();
        return buildCartResponse(nguoiDungId);
    }

    // =========================================================================
    // CÁC HÀM HỖ TRỢ DÙNG CHUNG
    // =========================================================================

    /**
     * Dù JWT đã xác thực thành công ở Filter, vẫn truy vấn lại DB để kiểm tra trạng thái mới nhất:
     * tránh trường hợp tài khoản vừa bị Admin khóa (biKhoa = true) nhưng token cũ chưa hết hạn vẫn dùng được.
     */
    private TkNguoiDung getActiveCustomer(String nguoiDungId) {
        TkNguoiDung nguoiDung = tkNguoiDungRepository.findById(nguoiDungId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại. Vui lòng đăng nhập lại."));
        if (Boolean.TRUE.equals(nguoiDung.getBiKhoa())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ CSKH để được hỗ trợ.");
        }
        return nguoiDung;
    }

    /**
     * Tách riêng 2 mã lỗi để frontend xử lý đúng:
     * - Không có id trong bảng sach -> 404 Not Found.
     * - Sách đã bị Admin ẩn (hienThi = false) -> 400 Bad Request ("ngừng kinh doanh").
     */
    private Sach getVisibleBook(String sachId) {
        Sach sach = sachRepository.findById(sachId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy cuốn sách có mã " + sachId + "."));
        if (!Boolean.TRUE.equals(sach.getHienThi())) {
            throw new BusinessException("Cuốn sách này hiện đã ngừng kinh doanh.");
        }
        return sach;
    }

    // Kiểm tra tồn kho thực tế trước khi lưu vào giỏ để tránh tình trạng giữ chỗ vượt quá số lượng kho có
    private void validateStockForCart(Sach sach, int soLuongYeuCau) {
        int tonKho = sach.getTonKho() == null ? 0 : sach.getTonKho();
        if (tonKho <= 0) {
            throw new BusinessException("Sách '" + sach.getTieuDe() + "' hiện đã hết hàng.");
        }
        if (soLuongYeuCau > tonKho) {
            throw new BusinessException("Sách '" + sach.getTieuDe() + "' chỉ còn " + tonKho + " cuốn trong kho.");
        }
    }

    /**
     * Quy tắc 2.2 (PHAN_TICH_NGHIEP_VU_VA_CSDL.md):
     * nếu dangFlashSale = true và giaFlashSale > 0 thì lấy giaFlashSale làm đơn giá tính tiền; ngược lại lấy giaBan.
     */
    private BigDecimal resolveEffectivePrice(Sach sach) {
        if (Boolean.TRUE.equals(sach.getDangFlashSale())
                && sach.getGiaFlashSale() != null
                && sach.getGiaFlashSale().compareTo(BigDecimal.ZERO) > 0) {
            return sach.getGiaFlashSale();
        }
        return sach.getGiaBan();
    }

    /**
     * Tổng hợp toàn bộ bảng tính giỏ hàng từ DB:
     * - tamTinh chỉ cộng những món đang daChon = true.
     * - phiVanChuyen = 0đ nếu chưa chọn món nào (tránh hiện phí ship 25k vô lý khi khách chưa tick gì)
     *   hoặc tamTinh >= 300.000đ (Freeship); ngược lại là 25.000đ.
     */
    private CartResponseDto buildCartResponse(String nguoiDungId) {
        List<GioHang> rows = gioHangRepository.findAllWithSachByNguoiDungId(nguoiDungId);
        List<CartResponseDto.CartItemDto> itemDtos = rows.stream()
                .map(this::toCartItemDto)
                .toList();

        int tongSoLuongTrongGio = 0;
        int soMonDaChon = 0;
        int tongSoLuongDaChon = 0;
        BigDecimal tamTinh = BigDecimal.ZERO;

        for (CartResponseDto.CartItemDto item : itemDtos) {
            tongSoLuongTrongGio += item.soLuong();
            if (Boolean.TRUE.equals(item.daChon())) {
                soMonDaChon++;
                tongSoLuongDaChon += item.soLuong();
                tamTinh = tamTinh.add(item.thanhTien());
            }
        }

        boolean daChonTatCa = !itemDtos.isEmpty() && soMonDaChon == itemDtos.size();
        BigDecimal phiVanChuyen = (soMonDaChon == 0 || tamTinh.compareTo(NGUONG_MIEN_PHI_SHIP) >= 0)
                ? BigDecimal.ZERO
                : PHI_VAN_CHUYEN_MAC_DINH;
        BigDecimal conThieuDeFreeship = tamTinh.compareTo(NGUONG_MIEN_PHI_SHIP) >= 0
                ? BigDecimal.ZERO
                : NGUONG_MIEN_PHI_SHIP.subtract(tamTinh);
        BigDecimal tongDuKien = tamTinh.add(phiVanChuyen);

        return new CartResponseDto(
                itemDtos,
                itemDtos.size(),
                tongSoLuongTrongGio,
                soMonDaChon,
                tongSoLuongDaChon,
                daChonTatCa,
                tamTinh,
                phiVanChuyen,
                NGUONG_MIEN_PHI_SHIP,
                conThieuDeFreeship,
                tongDuKien
        );
    }

    // Đọc getTacGia() và getDanhMuc() an toàn vì GioHangRepository đã JOIN FETCH sẵn
    private CartResponseDto.CartItemDto toCartItemDto(GioHang row) {
        Sach sach = row.getSach();
        BigDecimal donGiaApDung = resolveEffectivePrice(sach);
        BigDecimal thanhTien = donGiaApDung.multiply(BigDecimal.valueOf(row.getSoLuong()));
        return new CartResponseDto.CartItemDto(
                sach.getId(),
                sach.getTieuDe(),
                sach.getTacGia().getTenTacGia(),
                sach.getDanhMuc().getTenDanhMuc(),
                sach.getAnhBiaUrl(),
                sach.getGiaBan(),
                sach.getGiaGoc(),
                sach.getDangFlashSale(),
                sach.getGiaFlashSale(),
                donGiaApDung,
                row.getSoLuong(),
                thanhTien,
                sach.getTonKho(),
                sach.getHienThi(),
                row.getDaChon(),
                row.getNgayCapNhat()
        );
    }

    // Đọc getTacGia() và getDanhMuc() ở đây an toàn không dính lỗi Lazy vì đã được JOIN FETCH sẵn từ Repository
    private WishlistItemResponse toItemResponse(SachYeuThich yeuThich) {
        Sach sach = yeuThich.getSach();
        return new WishlistItemResponse(
                sach.getId(),
                sach.getTieuDe(),
                sach.getTacGia().getTenTacGia(),
                sach.getDanhMuc().getTenDanhMuc(),
                sach.getGiaBan(),
                sach.getGiaGoc(),
                sach.getDangFlashSale(),
                sach.getGiaFlashSale(),
                sach.getTonKho(),
                sach.getDiemDanhGiaTb(),
                sach.getAnhBiaUrl(),
                yeuThich.getNgayTao()
        );
    }
}
