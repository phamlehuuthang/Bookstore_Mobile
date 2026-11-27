package com.trithucviet.bookstore.service;

import com.trithucviet.bookstore.dto.request.TrackBehaviorRequest;
import com.trithucviet.bookstore.dto.response.BookDto;
import com.trithucviet.bookstore.dto.response.RecommendationResponse;
import com.trithucviet.bookstore.dto.response.TrackBehaviorResponse;
import com.trithucviet.bookstore.entity.ChiTietDonHang;
import com.trithucviet.bookstore.entity.DanhMuc;
import com.trithucviet.bookstore.entity.DonHang;
import com.trithucviet.bookstore.entity.GioHang;
import com.trithucviet.bookstore.entity.HanhViNguoiDung;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.entity.SachYeuThich;
import com.trithucviet.bookstore.entity.TkNguoiDung;
import com.trithucviet.bookstore.entity.enums.LoaiHanhVi;
import com.trithucviet.bookstore.entity.enums.TrangThaiDon;
import com.trithucviet.bookstore.exception.BusinessException;
import com.trithucviet.bookstore.repository.ChiTietDonHangRepository;
import com.trithucviet.bookstore.repository.DanhMucRepository;
import com.trithucviet.bookstore.repository.DonHangRepository;
import com.trithucviet.bookstore.repository.GioHangRepository;
import com.trithucviet.bookstore.repository.HanhViNguoiDungRepository;
import com.trithucviet.bookstore.repository.SachRepository;
import com.trithucviet.bookstore.repository.SachYeuThichRepository;
import com.trithucviet.bookstore.repository.TkNguoiDungRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ Theo dõi Thói quen Người dùng (User Tracking) & Đề xuất Sách Cá nhân hóa (Bước 8 Người A).
 *
 * Thực thi đầy đủ 4 Quy tắc Nghiệp vụ trong PHAN_TICH_NGHIEP_VU_VA_CSDL.md (Phân hệ 7):
 * - Quy tắc 7.1 (Dwell Time & Trọng số điểm):
 *   + XEM_CHI_TIET < 3 giây: bỏ qua (coi là bấm nhầm, không lưu DB).
 *   + XEM_CHI_TIET 3-4s: +1 | 5-14s: +3 | >= 15s: +5.
 *   + TIM_KIEM: +2 | THEM_YEU_THICH: +4 | THEM_GIO_HANG: +6 | MUA_HANG: +10 | BO_QUA_DE_XUAT: -1.
 * - Quy tắc 7.2 (Multi-Interest Mixing 3:2:1):
 *   + Phân bổ kệ 6 cuốn Đề xuất theo Top 3 chủ đề cao điểm nhất: Chủ đề #1 tối đa 3 cuốn,
 *     Chủ đề #2 tối đa 2 cuốn, Chủ đề #3 tối đa 1 cuốn.
 * - Quy tắc 7.3 (Exponential Time Decay - Suy giảm điểm theo thời gian):
 *   + XEM_CHI_TIET / TIM_KIEM: chu kỳ bán rã 48 giờ, quá 7 ngày (168 giờ) -> 0 điểm.
 *   + THEM_YEU_THICH / THEM_GIO_HANG: chu kỳ bán rã 14 ngày (336 giờ).
 *   + MUA_HANG: chu kỳ bán rã 30 ngày (720 giờ).
 * - Quy tắc 7.4 (Candidate Exclusion & Impression Fatigue):
 *   + Loại trừ toàn bộ sách khách đã Mua, đã Bỏ giỏ hoặc đã Thả tim khỏi kệ Đề xuất (nhưng vẫn cộng điểm chủ đề).
 *   + Nếu chủ đề bị ngó lơ (BO_QUA_DE_XUAT) quá 3 lần liên tiếp mà chưa có tương tác tích cực mới,
 *     điểm chủ đề bị nhân hệ số phạt 0.8^(IgnoredCount - 3); khi khách xem/mua lại chủ đề đó thì bộ đếm phạt tự về 0.
 */
@Service
public class RecommendationTrackingService {

    private static final int MAX_RECOMMENDATIONS = 6;
    private static final int[] TOPIC_QUOTAS = {3, 2, 1};

    private final HanhViNguoiDungRepository hanhViNguoiDungRepository;
    private final SachRepository sachRepository;
    private final DanhMucRepository danhMucRepository;
    private final TkNguoiDungRepository tkNguoiDungRepository;
    private final SachYeuThichRepository sachYeuThichRepository;
    private final GioHangRepository gioHangRepository;
    private final DonHangRepository donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;

    public RecommendationTrackingService(
            HanhViNguoiDungRepository hanhViNguoiDungRepository,
            SachRepository sachRepository,
            DanhMucRepository danhMucRepository,
            TkNguoiDungRepository tkNguoiDungRepository,
            SachYeuThichRepository sachYeuThichRepository,
            GioHangRepository gioHangRepository,
            DonHangRepository donHangRepository,
            ChiTietDonHangRepository chiTietDonHangRepository
    ) {
        this.hanhViNguoiDungRepository = hanhViNguoiDungRepository;
        this.sachRepository = sachRepository;
        this.danhMucRepository = danhMucRepository;
        this.tkNguoiDungRepository = tkNguoiDungRepository;
        this.sachYeuThichRepository = sachYeuThichRepository;
        this.gioHangRepository = gioHangRepository;
        this.donHangRepository = donHangRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
    }

    // =========================================================================
    // PHẦN 1: GHI NHẬN HÀNH VI NGƯỜI DÙNG (API + AUTO HOOK CÁCH B)
    // =========================================================================

    /**
     * Ghi nhận sự kiện hành vi gửi từ Mobile App (POST /api/v1/tracking/event).
     */
    @Transactional
    public TrackBehaviorResponse trackEvent(String nguoiDungId, TrackBehaviorRequest request) {
        TkNguoiDung customer = requireActiveCustomer(nguoiDungId);
        LoaiHanhVi action = request.loaiHanhVi();
        int dwellSeconds = request.thoiGianXemGiay() == null ? 0 : Math.max(0, request.thoiGianXemGiay());

        // Giải quyết sách và danh mục mục tiêu
        Sach targetBook = null;
        DanhMuc targetCategory;

        String rawBookId = normalizeBlankToNull(request.sachId());
        String rawCatId = normalizeBlankToNull(request.danhMucId());

        if (rawBookId != null) {
            targetBook = sachRepository.findByIdWithDetails(rawBookId)
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "Không tìm thấy cuốn sách với mã: " + rawBookId
                    ));
            targetCategory = targetBook.getDanhMuc();
        } else if (rawCatId != null) {
            targetCategory = danhMucRepository.findById(rawCatId)
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "Không tìm thấy danh mục với mã: " + rawCatId
                    ));
        } else {
            throw new BusinessException("Vui lòng truyền sachId hoặc danhMucId khi ghi nhận hành vi.");
        }

        // Quy tắc 7.1: Mở trang chi tiết sách nhưng thoát ra dưới 3 giây -> coi là bấm nhầm, bỏ qua không lưu DB
        if (action == LoaiHanhVi.XEM_CHI_TIET && dwellSeconds < 3) {
            return new TrackBehaviorResponse(
                    null,
                    customer.getId(),
                    targetBook != null ? targetBook.getId() : null,
                    targetCategory.getId(),
                    targetCategory.getTenDanhMuc(),
                    action,
                    dwellSeconds,
                    0,
                    true,
                    LocalDateTime.now()
            );
        }

        int weightScore = computeWeightScore(action, dwellSeconds);
        LocalDateTime now = LocalDateTime.now();

        HanhViNguoiDung entity = new HanhViNguoiDung();
        entity.setId("hv_" + shortUuid());
        entity.setNguoiDung(customer);
        entity.setSach(targetBook);
        entity.setDanhMuc(targetCategory);
        entity.setLoaiHanhVi(action);
        entity.setThoiGianXemGiay(dwellSeconds);
        entity.setTrongSoDiem(weightScore);
        entity.setNgayTao(now);

        hanhViNguoiDungRepository.saveAndFlush(entity);

        return new TrackBehaviorResponse(
                entity.getId(),
                customer.getId(),
                targetBook != null ? targetBook.getId() : null,
                targetCategory.getId(),
                targetCategory.getTenDanhMuc(),
                action,
                dwellSeconds,
                weightScore,
                false,
                now
        );
    }

    /**
     * Hàm Hook tự động (Cách B) được gọi trực tiếp từ CartWishlistService (khi Thả tim / Thêm giỏ)
     * và OrderService (khi Đặt hàng thành công) để đảm bảo mọi thao tác nghiệp vụ đều lưu hành vi chuẩn xác.
     */
    @Transactional
    public void recordAutoAction(TkNguoiDung customer, Sach book, LoaiHanhVi action) {
        if (customer == null || book == null || book.getDanhMuc() == null) {
            return;
        }
        HanhViNguoiDung entity = new HanhViNguoiDung();
        entity.setId("hv_" + shortUuid());
        entity.setNguoiDung(customer);
        entity.setSach(book);
        entity.setDanhMuc(book.getDanhMuc());
        entity.setLoaiHanhVi(action);
        entity.setThoiGianXemGiay(0);
        entity.setTrongSoDiem(computeWeightScore(action, 0));
        entity.setNgayTao(LocalDateTime.now());
        hanhViNguoiDungRepository.save(entity);
    }

    /**
     * Quy tắc 7.1: Bảng quy đổi trọng số điểm theo loại hành vi và Dwell Time.
     */
    public int computeWeightScore(LoaiHanhVi action, int dwellSeconds) {
        return switch (action) {
            case XEM_CHI_TIET -> {
                if (dwellSeconds < 3) yield 0;
                if (dwellSeconds <= 4) yield 1;
                if (dwellSeconds <= 14) yield 3;
                yield 5;
            }
            case TIM_KIEM -> 2;
            case THEM_YEU_THICH -> 4;
            case THEM_GIO_HANG -> 6;
            case MUA_HANG -> 10;
            case BO_QUA_DE_XUAT -> -1;
        };
    }

    // =========================================================================
    // PHẦN 2: THUẬT TOÁN ĐỀ XUẤT SÁCH CÁ NHÂN HÓA (GET /api/v1/books/recommendations)
    // =========================================================================

    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendations(String nullableNguoiDungId) {
        // Chỉ đề xuất sách đang kinh doanh (hienThi = true) và còn tồn kho (tonKho > 0), sắp xếp bán chạy lên đầu
        List<Sach> allCandidates = sachRepository.findAllVisibleWithDetails().stream()
                .filter(b -> (b.getTonKho() == null ? 0 : b.getTonKho()) > 0)
                .sorted(Comparator
                        .comparingInt((Sach s) -> s.getDaBan() == null ? 0 : s.getDaBan()).reversed()
                        .thenComparing((Sach s) -> s.getDiemDanhGiaTb() == null ? BigDecimal.ZERO : s.getDiemDanhGiaTb(), Comparator.reverseOrder())
                        .thenComparing(Sach::getId))
                .toList();

        // Khách vãng lai (chưa đăng nhập) -> gợi ý Top 6 sách bán chạy theo xu hướng
        if (nullableNguoiDungId == null || nullableNguoiDungId.isBlank()) {
            return buildTrendingFallback(allCandidates, Set.of());
        }

        // Quy tắc 7.4 (Candidate Exclusion): Tập hợp ID các cuốn sách khách đã Thả tim, Bỏ giỏ hoặc đã Mua
        Set<String> excludedBookIds = collectExcludedBookIds(nullableNguoiDungId);

        List<HanhViNguoiDung> history = hanhViNguoiDungRepository.findByNguoiDungIdWithDetailsOrdered(nullableNguoiDungId);
        if (history.isEmpty()) {
            return buildTrendingFallback(allCandidates, excludedBookIds);
        }

        // Bước 1: Đếm số lần ngó lơ liên tiếp (BO_QUA_DE_XUAT) tính từ sau tương tác tích cực gần nhất của từng chủ đề
        Map<String, Integer> consecutiveIgnoresByCategory = computeConsecutiveIgnores(history);

        // Bước 2: Tính tổng điểm quan tâm (Affinity Score) cho từng chủ đề theo Time Decay + RecencyBoost
        LocalDateTime now = LocalDateTime.now();
        Map<String, CategoryScoreEntry> scoreByCategory = new HashMap<>();
        int positiveEventIndex = 0;

        for (HanhViNguoiDung ev : history) {
            if (ev.getLoaiHanhVi() == LoaiHanhVi.BO_QUA_DE_XUAT) {
                continue;
            }
            int weight = ev.getTrongSoDiem() == null ? 0 : ev.getTrongSoDiem();
            if (weight <= 0) {
                continue;
            }

            double decay = computeTimeDecayFactor(ev.getLoaiHanhVi(), ev.getNgayTao(), now);
            if (decay <= 0.0) {
                positiveEventIndex++;
                continue;
            }

            double recencyBoost = positiveEventIndex == 0 ? 1.8 : (positiveEventIndex < 3 ? 1.35 : 1.0);
            positiveEventIndex++;

            String catId = ev.getDanhMuc().getId();
            CategoryScoreEntry entry = scoreByCategory.computeIfAbsent(
                    catId,
                    k -> new CategoryScoreEntry(catId, ev.getDanhMuc().getTenDanhMuc(), ev.getNgayTao())
            );
            entry.rawScore += weight * decay * recencyBoost;
            if (ev.getNgayTao() != null && (entry.latestTime == null || ev.getNgayTao().isAfter(entry.latestTime))) {
                entry.latestTime = ev.getNgayTao();
            }
        }

        // Bước 3: Áp dụng hệ số phạt Impression Fatigue 0.8^max(0, IgnoredCount - 3) (Quy tắc 7.4 & Công thức 6)
        List<CategoryScoreEntry> rankedCategories = new ArrayList<>();
        for (CategoryScoreEntry entry : scoreByCategory.values()) {
            int ignoredCount = consecutiveIgnoresByCategory.getOrDefault(entry.categoryId, 0);
            double fatigueMultiplier = ignoredCount > 3 ? Math.pow(0.8, ignoredCount - 3) : 1.0;
            entry.finalScore = entry.rawScore * fatigueMultiplier;
            if (entry.finalScore > 0.2) {
                rankedCategories.add(entry);
            }
        }

        rankedCategories.sort((a, b) -> {
            int cmp = Double.compare(b.finalScore, a.finalScore);
            if (cmp != 0) return cmp;
            if (a.latestTime == null || b.latestTime == null) return 0;
            return b.latestTime.compareTo(a.latestTime);
        });

        if (rankedCategories.isEmpty()) {
            return buildTrendingFallback(allCandidates, excludedBookIds);
        }

        // Bước 4: Pha trộn Đa chủ đề 3:2:1 (Quy tắc 7.2) + Loại trừ sách đã có (Quy tắc 7.4)
        List<Sach> pickedBooks = new ArrayList<>(MAX_RECOMMENDATIONS);
        Set<String> seenIds = new HashSet<>(excludedBookIds);
        List<String> activeTopicNames = new ArrayList<>();

        int topicLimit = Math.min(3, rankedCategories.size());
        for (int idx = 0; idx < topicLimit; idx++) {
            CategoryScoreEntry topic = rankedCategories.get(idx);
            int quota = TOPIC_QUOTAS[idx];
            int addedForTopic = 0;

            for (Sach candidate : allCandidates) {
                if (addedForTopic >= quota || pickedBooks.size() >= MAX_RECOMMENDATIONS) {
                    break;
                }
                if (seenIds.contains(candidate.getId())) {
                    continue;
                }
                if (candidate.getDanhMuc().getId().equals(topic.categoryId)) {
                    seenIds.add(candidate.getId());
                    pickedBooks.add(candidate);
                    addedForTopic++;
                }
            }

            if (addedForTopic > 0) {
                activeTopicNames.add(topic.categoryName);
            }
        }

        // Bù thêm sách bán chạy khác (nếu 3 chủ đề trên chưa đủ 6 cuốn)
        for (Sach candidate : allCandidates) {
            if (pickedBooks.size() >= MAX_RECOMMENDATIONS) {
                break;
            }
            if (!seenIds.contains(candidate.getId())) {
                seenIds.add(candidate.getId());
                pickedBooks.add(candidate);
            }
        }

        String reasonLabel;
        if (activeTopicNames.size() >= 2) {
            reasonLabel = "Theo sở thích: " + activeTopicNames.get(0) + " • " + activeTopicNames.get(1);
        } else if (activeTopicNames.size() == 1) {
            reasonLabel = "Vì bạn quan tâm đến " + activeTopicNames.get(0);
        } else {
            reasonLabel = "Gợi ý theo xu hướng đọc tuần này";
        }

        return new RecommendationResponse(
                reasonLabel,
                activeTopicNames,
                pickedBooks.stream().map(this::toBookDto).toList()
        );
    }

    // --- Các hàm tính toán nội bộ ---

    private Set<String> collectExcludedBookIds(String nguoiDungId) {
        Set<String> excluded = new LinkedHashSet<>();

        for (SachYeuThich w : sachYeuThichRepository.findAllWithSachByNguoiDungId(nguoiDungId)) {
            excluded.add(w.getSach().getId());
        }
        for (GioHang c : gioHangRepository.findAllWithSachByNguoiDungId(nguoiDungId)) {
            excluded.add(c.getSach().getId());
        }

        List<String> validOrderIds = donHangRepository.findByNguoiDungIdOrdered(nguoiDungId).stream()
                .filter(o -> o.getTrangThaiDon() != TrangThaiDon.DA_HUY)
                .map(DonHang::getId)
                .toList();
        if (!validOrderIds.isEmpty()) {
            for (ChiTietDonHang item : chiTietDonHangRepository.findByDonHangIdInWithSach(validOrderIds)) {
                excluded.add(item.getSach().getId());
            }
        }
        return excluded;
    }

    /**
     * Đếm số lần BO_QUA_DE_XUAT liên tiếp gần nhất của từng danh mục.
     * Vì history đã sắp xếp mới nhất -> cũ nhất, ngay khi gặp 1 hành vi tích cực của danh mục C
     * thì dừng đếm cho danh mục C (nhờ vậy khi khách bấm xem lại danh mục C, bộ đếm phạt tự động reset về 0).
     */
    private Map<String, Integer> computeConsecutiveIgnores(List<HanhViNguoiDung> orderedHistory) {
        Map<String, Integer> consecutiveIgnores = new HashMap<>();
        Set<String> lockedCategories = new HashSet<>();

        for (HanhViNguoiDung ev : orderedHistory) {
            String catId = ev.getDanhMuc().getId();
            if (lockedCategories.contains(catId)) {
                continue;
            }
            if (ev.getLoaiHanhVi() == LoaiHanhVi.BO_QUA_DE_XUAT) {
                consecutiveIgnores.merge(catId, 1, Integer::sum);
            } else {
                lockedCategories.add(catId);
            }
        }
        return consecutiveIgnores;
    }

    /**
     * Quy tắc 7.3: Suy giảm điểm theo thời gian (Exponential Time Decay).
     */
    public double computeTimeDecayFactor(LoaiHanhVi action, LocalDateTime eventTime, LocalDateTime now) {
        if (eventTime == null) {
            return 1.0;
        }
        double elapsedHours = Math.max(0.0, Duration.between(eventTime, now).toMinutes() / 60.0);

        return switch (action) {
            case XEM_CHI_TIET, TIM_KIEM -> {
                // Quá 7 ngày (168 giờ) không xem lại -> hết hạn (0 điểm)
                if (elapsedHours > 168.0) {
                    yield 0.0;
                }
                // Chu kỳ bán rã 48 giờ
                yield Math.pow(0.5, elapsedHours / 48.0);
            }
            case THEM_YEU_THICH, THEM_GIO_HANG -> Math.pow(0.5, elapsedHours / (14.0 * 24.0));
            case MUA_HANG -> Math.pow(0.5, elapsedHours / (30.0 * 24.0));
            case BO_QUA_DE_XUAT -> 0.0;
        };
    }

    private boolean belongsToCategoryOrParent(DanhMuc bookCategory, String targetCategoryId) {
        DanhMuc current = bookCategory;
        int guard = 0;
        while (current != null && guard++ < 5) {
            if (current.getId().equals(targetCategoryId)) {
                return true;
            }
            current = current.getDanhMucCha();
        }
        return false;
    }

    private RecommendationResponse buildTrendingFallback(List<Sach> allCandidates, Set<String> excludedIds) {
        List<BookDto> trending = allCandidates.stream()
                .filter(b -> !excludedIds.contains(b.getId()))
                .limit(MAX_RECOMMENDATIONS)
                .map(this::toBookDto)
                .toList();
        return new RecommendationResponse(
                "Gợi ý theo xu hướng đọc tuần này",
                List.of(),
                trending
        );
    }

    private BookDto toBookDto(Sach book) {
        BigDecimal effectivePrice = (Boolean.TRUE.equals(book.getDangFlashSale())
                && book.getGiaFlashSale() != null
                && book.getGiaFlashSale().compareTo(BigDecimal.ZERO) > 0)
                ? book.getGiaFlashSale()
                : (book.getGiaBan() == null ? BigDecimal.ZERO : book.getGiaBan());

        int discountPercent = 0;
        if (book.getGiaGoc() != null
                && book.getGiaGoc().compareTo(BigDecimal.ZERO) > 0
                && effectivePrice.compareTo(book.getGiaGoc()) < 0) {
            discountPercent = book.getGiaGoc().subtract(effectivePrice)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(book.getGiaGoc(), 0, RoundingMode.HALF_UP)
                    .intValue();
        }

        return new BookDto(
                book.getId(),
                book.getMaIsbn(),
                book.getTieuDe(),
                book.getDuongDan(),
                book.getDanhMuc().getId(),
                book.getDanhMuc().getTenDanhMuc(),
                book.getTenDanhMucCon(),
                book.getTacGia().getId(),
                book.getTacGia().getTenTacGia(),
                book.getNhaXuatBan().getId(),
                book.getNhaXuatBan().getTenNxb(),
                book.getGiaBan(),
                book.getGiaGoc(),
                Boolean.TRUE.equals(book.getDangFlashSale()),
                book.getGiaFlashSale(),
                effectivePrice,
                discountPercent,
                book.getTonKho(),
                book.getDaBan(),
                book.getDiemDanhGiaTb(),
                book.getSoLuotDanhGia(),
                book.getLoaiBia(),
                book.getNamXuatBan(),
                book.getAnhBiaUrl(),
                book.getTomTatNgan(),
                Boolean.TRUE.equals(book.getBanChay())
        );
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
                    "Tài khoản của bạn đã bị khóa."
            );
        }
        return user;
    }

    private String normalizeBlankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static class CategoryScoreEntry {
        final String categoryId;
        final String categoryName;
        LocalDateTime latestTime;
        double rawScore = 0.0;
        double finalScore = 0.0;

        CategoryScoreEntry(String categoryId, String categoryName, LocalDateTime latestTime) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.latestTime = latestTime;
        }
    }
}
