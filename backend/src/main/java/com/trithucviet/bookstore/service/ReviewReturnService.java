package com.trithucviet.bookstore.service;

import com.trithucviet.bookstore.dto.request.CreateReturnRequest;
import com.trithucviet.bookstore.dto.request.CreateReviewRequest;
import com.trithucviet.bookstore.dto.request.ProcessReturnRequest;
import com.trithucviet.bookstore.dto.response.AdminReviewDto;
import com.trithucviet.bookstore.dto.response.ReturnRequestDto;
import com.trithucviet.bookstore.dto.response.ReviewDto;
import com.trithucviet.bookstore.entity.ChiTietDonHang;
import com.trithucviet.bookstore.entity.DanhGia;
import com.trithucviet.bookstore.entity.DonHang;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.entity.ThongBao;
import com.trithucviet.bookstore.entity.TkNguoiDung;
import com.trithucviet.bookstore.entity.YeuCauDoiTra;
import com.trithucviet.bookstore.entity.enums.LoaiThongBao;
import com.trithucviet.bookstore.entity.enums.TrangThaiDoiTra;
import com.trithucviet.bookstore.entity.enums.TrangThaiDon;
import com.trithucviet.bookstore.exception.BusinessException;
import com.trithucviet.bookstore.repository.ChiTietDonHangRepository;
import com.trithucviet.bookstore.repository.DanhGiaRepository;
import com.trithucviet.bookstore.repository.DonHangRepository;
import com.trithucviet.bookstore.repository.SachRepository;
import com.trithucviet.bookstore.repository.ThongBaoRepository;
import com.trithucviet.bookstore.repository.TkNguoiDungRepository;
import com.trithucviet.bookstore.repository.YeuCauDoiTraRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ Hậu mãi — Đánh giá Sách & Yêu cầu Đổi/Trả hàng (Bước 7 - Phần 1 & 2).
 *
 * Quy tắc nghiệp vụ áp dụng:
 * 1. Đánh giá 1–5 sao (Mobile):
 *    - Chỉ đơn hàng của chính khách hàng và đã ở trạng thái DA_GIAO mới được đánh giá.
 *    - Hỗ trợ truyền sachId thuộc đơn hàng (nếu để trống tự chọn cuốn đầu tiên); chặn đánh giá trùng (donHangId, sachId).
 *    - Tự tính lại sach.diemDanhGiaTb & sach.soLuotDanhGia từ các đánh giá công khai (biAn = false).
 *    - Khi toàn bộ đầu sách khác nhau trong đơn đã được đánh giá -> tự bật donHang.daDanhGia = true.
 * 2. Kiểm duyệt bình luận (Web Admin - ADMIN & CSKH):
 *    - Ẩn/hiện đánh giá vi phạm (biAn) và tự động tính lại diemDanhGiaTb & soLuotDanhGia của sách.
 * 3. Yêu cầu & Duyệt Đổi/Trả hàng (Mobile + Web Admin - ADMIN & CSKH):
 *    - Chỉ đơn DA_GIAO mới gửi được yêu cầu đổi trả; mỗi đơn tối đa 1 yêu cầu (ràng buộc UNIQUE 1-1).
 *    - Khi ADMIN / CSKH duyệt DA_CHAP_THUAN (đổi sách mới) hoặc TU_CHOI: cập nhật trạng thái, ghiChuAdmin,
 *      ngayXuLy và gửi ThongBao tới khách (không trừ kho hoặc đổi trạng thái thanh toán).
 */
@Service
public class ReviewReturnService {

    private static final String DEFAULT_EVIDENCE_URL =
            "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=600&q=80";

    private final DanhGiaRepository danhGiaRepository;
    private final YeuCauDoiTraRepository yeuCauDoiTraRepository;
    private final DonHangRepository donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final SachRepository sachRepository;
    private final TkNguoiDungRepository tkNguoiDungRepository;
    private final ThongBaoRepository thongBaoRepository;

    public ReviewReturnService(
            DanhGiaRepository danhGiaRepository,
            YeuCauDoiTraRepository yeuCauDoiTraRepository,
            DonHangRepository donHangRepository,
            ChiTietDonHangRepository chiTietDonHangRepository,
            SachRepository sachRepository,
            TkNguoiDungRepository tkNguoiDungRepository,
            ThongBaoRepository thongBaoRepository
    ) {
        this.danhGiaRepository = danhGiaRepository;
        this.yeuCauDoiTraRepository = yeuCauDoiTraRepository;
        this.donHangRepository = donHangRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.sachRepository = sachRepository;
        this.tkNguoiDungRepository = tkNguoiDungRepository;
        this.thongBaoRepository = thongBaoRepository;
    }

    // =========================================================================
    // PHẦN 1: ĐÁNH GIÁ SÁCH (MOBILE) & KIỂM DUYỆT ĐÁNH GIÁ (WEB ADMIN)
    // =========================================================================

    @Transactional
    public ReviewDto submitReview(String nguoiDungId, CreateReviewRequest request) {
        TkNguoiDung customer = requireActiveCustomer(nguoiDungId);
        DonHang order = donHangRepository.findByIdAndNguoiDungId(request.donHangId().trim(), nguoiDungId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng hoặc bạn không có quyền đánh giá đơn hàng này."
                ));

        if (order.getTrangThaiDon() != TrangThaiDon.DA_GIAO) {
            throw new BusinessException("Chỉ có thể gửi đánh giá sau khi đơn hàng đã giao thành công.");
        }

        List<ChiTietDonHang> orderItems = chiTietDonHangRepository.findByDonHangIdWithSach(order.getId());
        if (orderItems.isEmpty()) {
            throw new BusinessException("Đơn hàng không có sản phẩm hợp lệ để đánh giá.");
        }

        // Nếu khách truyền sachId -> đối chiếu đúng sách có nằm trong đơn; ngược lại chọn cuốn đầu tiên
        Sach targetBook;
        String requestedBookId = normalizeBlankToNull(request.sachId());
        if (requestedBookId == null) {
            targetBook = orderItems.get(0).getSach();
        } else {
            targetBook = orderItems.stream()
                    .map(ChiTietDonHang::getSach)
                    .filter(book -> book.getId().equals(requestedBookId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            "Cuốn sách được chọn không nằm trong đơn hàng này."
                    ));
        }

        if (danhGiaRepository.existsByDonHangIdAndSachId(order.getId(), targetBook.getId())) {
            throw new BusinessException("Bạn đã đánh giá cuốn sách này cho đơn hàng này rồi.");
        }

        DanhGia review = new DanhGia();
        review.setId("dg_" + shortUuid());
        review.setSach(targetBook);
        review.setNguoiDung(customer);
        review.setDonHang(order);
        review.setSoSao(request.soSao());
        review.setBinhLuan(request.binhLuan().trim());
        review.setAnhDanhGiaUrls(joinUrls(request.anhDanhGiaUrls()));
        review.setBiAn(Boolean.FALSE);
        review.setNgayTao(LocalDateTime.now());

        danhGiaRepository.saveAndFlush(review);

        // Tính lại điểm sao trung bình & tổng lượt đánh giá của sách
        recalculateBookRating(targetBook);

        // Khi tất cả đầu sách khác nhau trong đơn đã được đánh giá -> bật cờ daDanhGia = true
        long distinctBooksInOrder = orderItems.stream()
                .map(item -> item.getSach().getId())
                .distinct()
                .count();
        long reviewedBooksCount = danhGiaRepository.countDistinctReviewedBooksByDonHangId(order.getId());
        if (reviewedBooksCount >= distinctBooksInOrder && !Boolean.TRUE.equals(order.getDaDanhGia())) {
            order.setDaDanhGia(Boolean.TRUE);
            donHangRepository.saveAndFlush(order);
        }

        return new ReviewDto(
                review.getId(),
                targetBook.getId(),
                customer.getId(),
                customer.getHoTen(),
                review.getSoSao(),
                review.getBinhLuan(),
                splitUrls(review.getAnhDanhGiaUrls()),
                review.getNgayTao()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminReviewDto> getAdminReviews(Boolean biAn, String keyword) {
        String normalizedKeyword = normalizeBlankToNull(keyword);
        String lowerKeyword = normalizedKeyword == null ? null : normalizedKeyword.toLowerCase(Locale.ROOT);

        return danhGiaRepository.findAllWithDetails().stream()
                .filter(dg -> biAn == null || biAn.equals(Boolean.TRUE.equals(dg.getBiAn())))
                .filter(dg -> {
                    if (lowerKeyword == null) {
                        return true;
                    }
                    return containsIgnoreCase(dg.getSach().getTieuDe(), lowerKeyword)
                            || containsIgnoreCase(dg.getNguoiDung().getHoTen(), lowerKeyword)
                            || containsIgnoreCase(dg.getBinhLuan(), lowerKeyword);
                })
                .map(this::toAdminReviewDto)
                .toList();
    }

    /**
     * Ẩn hoặc hiện lại 1 bình luận vi phạm (ADMIN / CSKH), sau đó tính lại điểm sao trung bình của cuốn sách.
     */
    @Transactional
    public AdminReviewDto toggleReviewHidden(String reviewId) {
        DanhGia review = danhGiaRepository.findByIdWithDetails(reviewId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá."));

        review.setBiAn(!Boolean.TRUE.equals(review.getBiAn()));
        danhGiaRepository.saveAndFlush(review);

        recalculateBookRating(review.getSach());
        return toAdminReviewDto(review);
    }

    // =========================================================================
    // PHẦN 2: YÊU CẦU ĐỔI/TRẢ HÀNG (MOBILE) & DUYỆT ĐỔI TRẢ (WEB ADMIN)
    // =========================================================================

    @Transactional
    public ReturnRequestDto submitReturnRequest(String nguoiDungId, CreateReturnRequest request) {
        TkNguoiDung customer = requireActiveCustomer(nguoiDungId);
        DonHang order = donHangRepository.findByIdAndNguoiDungId(request.donHangId().trim(), nguoiDungId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng hoặc bạn không có quyền gửi yêu cầu đổi trả."
                ));

        if (order.getTrangThaiDon() != TrangThaiDon.DA_GIAO) {
            throw new BusinessException("Chỉ có thể gửi yêu cầu đổi trả sau khi đơn hàng đã giao thành công.");
        }

        if (yeuCauDoiTraRepository.existsByDonHangId(order.getId())) {
            throw new BusinessException("Đơn hàng này đã có yêu cầu đổi trả trước đó.");
        }

        String evidenceJoined = joinUrls(request.anhMinhChungUrls());
        if (evidenceJoined == null) {
            evidenceJoined = DEFAULT_EVIDENCE_URL;
        }

        LocalDateTime now = LocalDateTime.now();
        YeuCauDoiTra returnReq = new YeuCauDoiTra();
        returnReq.setId("dt_" + shortUuid());
        returnReq.setDonHang(order);
        returnReq.setLyDo(request.lyDo().trim());
        returnReq.setMoTa(request.moTa().trim());
        returnReq.setAnhMinhChungUrls(evidenceJoined);
        returnReq.setTrangThai(TrangThaiDoiTra.CHO_DUYET);
        returnReq.setNgayTao(now);

        yeuCauDoiTraRepository.saveAndFlush(returnReq);

        createOrderNotification(
                customer,
                "Đã tiếp nhận yêu cầu đổi trả " + order.getMaDonHang(),
                "Yêu cầu đổi trả đơn " + order.getMaDonHang() + " đang chờ bộ phận CSKH xem xét và phản hồi.",
                now
        );

        return toReturnRequestDto(returnReq);
    }

    @Transactional(readOnly = true)
    public List<ReturnRequestDto> getAdminReturnRequests(TrangThaiDoiTra trangThai) {
        return yeuCauDoiTraRepository.findAllWithOrderOrdered().stream()
                .filter(req -> trangThai == null || req.getTrangThai() == trangThai)
                .map(this::toReturnRequestDto)
                .toList();
    }

    /**
     * Phê duyệt (DA_CHAP_THUAN - đổi sách mới) hoặc từ chối (TU_CHOI) yêu cầu đổi trả theo mã đơn hàng.
     * Chỉ cho phép ADMIN và CSKH xử lý (WAREHOUSE_MANAGER bị chặn 403 ở tầng Controller).
     */
    @Transactional
    public ReturnRequestDto processReturnByOrderId(String orderId, ProcessReturnRequest request) {
        if (request.trangThai() != TrangThaiDoiTra.DA_CHAP_THUAN
                && request.trangThai() != TrangThaiDoiTra.TU_CHOI) {
            throw new BusinessException(
                    "Quyết định xử lý đổi trả chỉ chấp nhận DA_CHAP_THUAN hoặc TU_CHOI."
            );
        }

        YeuCauDoiTra returnReq = yeuCauDoiTraRepository.findByDonHangIdWithOrder(orderId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Đơn hàng này chưa có yêu cầu đổi trả nào."
                ));

        if (returnReq.getTrangThai() != TrangThaiDoiTra.CHO_DUYET) {
            throw new BusinessException("Yêu cầu đổi trả này đã được xử lý trước đó.");
        }

        LocalDateTime now = LocalDateTime.now();
        String note = normalizeBlankToNull(request.ghiChuAdmin());
        returnReq.setTrangThai(request.trangThai());
        returnReq.setGhiChuAdmin(note);
        returnReq.setNgayXuLy(now);
        yeuCauDoiTraRepository.saveAndFlush(returnReq);

        DonHang order = returnReq.getDonHang();
        boolean approved = request.trangThai() == TrangThaiDoiTra.DA_CHAP_THUAN;
        String notifTitle = approved
                ? "Yêu cầu đổi trả " + order.getMaDonHang() + " đã được chấp thuận"
                : "Yêu cầu đổi trả " + order.getMaDonHang() + " bị từ chối";
        String notifContent = note != null
                ? note
                : (approved
                        ? "Nhà sách đồng ý đổi sách mới cho đơn hàng " + order.getMaDonHang() + "."
                        : "Yêu cầu đổi trả cho đơn hàng " + order.getMaDonHang() + " chưa đủ điều kiện áp dụng.");

        createOrderNotification(order.getNguoiDung(), notifTitle, notifContent, now);
        return toReturnRequestDto(returnReq);
    }

    // --- Các hàm tiện ích dùng chung ---

    private void recalculateBookRating(Sach book) {
        List<DanhGia> visibleReviews = danhGiaRepository.findVisibleReviewsBySachId(book.getId());
        int count = visibleReviews.size();
        BigDecimal avg;
        if (count == 0) {
            avg = new BigDecimal("5.00");
        } else {
            int sum = visibleReviews.stream().mapToInt(dg -> dg.getSoSao() == null ? 0 : dg.getSoSao()).sum();
            avg = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }
        book.setSoLuotDanhGia(count);
        book.setDiemDanhGiaTb(avg);
        sachRepository.saveAndFlush(book);
    }

    private void createOrderNotification(
            TkNguoiDung customer,
            String title,
            String content,
            LocalDateTime now
    ) {
        ThongBao notif = new ThongBao();
        notif.setId("tb_" + shortUuid());
        notif.setNguoiDung(customer);
        notif.setTieuDe(title);
        notif.setNoiDung(content);
        notif.setLoaiThongBao(LoaiThongBao.DON_HANG);
        notif.setDaDoc(Boolean.FALSE);
        notif.setNgayTao(now);
        thongBaoRepository.saveAndFlush(notif);
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

    private AdminReviewDto toAdminReviewDto(DanhGia dg) {
        return new AdminReviewDto(
                dg.getId(),
                dg.getSach().getId(),
                dg.getSach().getTieuDe(),
                dg.getNguoiDung().getId(),
                dg.getNguoiDung().getHoTen(),
                dg.getDonHang() != null ? dg.getDonHang().getId() : null,
                dg.getSoSao(),
                dg.getBinhLuan(),
                splitUrls(dg.getAnhDanhGiaUrls()),
                Boolean.TRUE.equals(dg.getBiAn()),
                dg.getNgayTao()
        );
    }

    private ReturnRequestDto toReturnRequestDto(YeuCauDoiTra req) {
        DonHang order = req.getDonHang();
        return new ReturnRequestDto(
                req.getId(),
                order.getId(),
                order.getMaDonHang(),
                order.getTenKhachHang(),
                req.getLyDo(),
                req.getMoTa(),
                splitUrls(req.getAnhMinhChungUrls()),
                req.getTrangThai(),
                req.getGhiChuAdmin(),
                req.getNgayTao(),
                req.getNgayXuLy()
        );
    }

    private String joinUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return null;
        }
        String joined = urls.stream()
                .filter(u -> u != null && !u.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
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

    private String normalizeBlankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean containsIgnoreCase(String value, String lowerKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }

    private String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
