package com.trithucviet.bookstore.ai;

import com.trithucviet.bookstore.dto.request.CancelOrderRequest;
import com.trithucviet.bookstore.dto.request.CartItemRequest;
import com.trithucviet.bookstore.dto.request.CartUpdateRequest;
import com.trithucviet.bookstore.dto.request.CreateOrderRequest;
import com.trithucviet.bookstore.dto.response.AddressDto;
import com.trithucviet.bookstore.dto.response.BookDto;
import com.trithucviet.bookstore.dto.response.CartResponseDto;
import com.trithucviet.bookstore.dto.response.OrderDto;
import com.trithucviet.bookstore.dto.response.RecommendationResponse;
import com.trithucviet.bookstore.dto.response.VoucherDto;
import com.trithucviet.bookstore.dto.response.VoucherValidationResponse;
import com.trithucviet.bookstore.dto.response.WishlistItemResponse;
import com.trithucviet.bookstore.dto.response.WishlistToggleResponse;
import com.trithucviet.bookstore.entity.DanhGia;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.entity.enums.PhuongThucThanhToan;
import com.trithucviet.bookstore.repository.DanhGiaRepository;
import com.trithucviet.bookstore.repository.SachRepository;
import com.trithucviet.bookstore.service.CartWishlistService;
import com.trithucviet.bookstore.service.OrderService;
import com.trithucviet.bookstore.service.RecommendationTrackingService;
import com.trithucviet.bookstore.service.UserAddressService;
import com.trithucviet.bookstore.service.VoucherService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bộ 10 Công cụ (Agentic Function Calling Tools) cho Trợ lý Mua sắm AI (Bước 10
 * nâng cấp).
 *
 * Nguyên tắc bảo mật & toàn vẹn dữ liệu:
 * - AI KHÔNG BAO GIỜ tự sinh câu SQL trực tiếp.
 * - Toàn bộ thao tác đọc/ghi (tìm sách, săn Flash Sale, ghép combo, so sánh &
 * đọc review, đề xuất cá nhân hóa,
 * quản lý giỏ hàng, tủ yêu thích, tự đặt đơn kèm mã giảm giá tối ưu, thanh toán
 * giỏ, tra cứu & hủy đơn)
 * đều đi qua các `@Transactional` Service chuẩn để giữ nguyên 100% quy tắc tồn
 * kho, voucher, timeline và thông báo.
 */
@Component
public class BookstoreAiTools {

    private static final BigDecimal FREESHIP_THRESHOLD = BigDecimal.valueOf(300_000L);
    private static final Pattern SACH_ID_PATTERN = Pattern.compile("(sach_\\w+)");

    private static final Set<String> VIETNAMESE_STOP_WORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "goi", "y", "tim", "kiem", "sach", "cho", "nguoi", "moi", "bat", "dau", "minh", "toi", "tui",
            "ban", "em", "anh", "chi", "shop", "ad", "cuon", "quyen", "bo", "tap", "duoi", "tren",
            "khoang", "gia", "re", "ngan", "nghin", "dong", "vnd", "trieu", "di", "nao", "nua",
            "muon", "mua", "dat", "them", "vao", "gio", "hang", "giup",
            "voi", "tom", "tat", "noi", "dung", "ve", "co", "khong", "hay", "tot", "nhat",
            "theo", "chu", "de", "the", "loai", "linh", "vuc", "danh", "muc", "nhung", "cac",
            "mot", "vai", "khac", "cua", "trong", "ngoai", "la", "va", "hoac", "nha",
            "nhe", "luon", "ngay", "don", "nay", "do", "kia", "can", "tu", "van", "gioi", "thieu",
            "chay", "hot", "pho", "bien", "combo", "so", "sanh", "danh", "review", "binh", "luan")));

    private final SachRepository sachRepository;
    private final DanhGiaRepository danhGiaRepository;
    private final BookVectorService bookVectorService;
    private final VoucherService voucherService;
    private final UserAddressService userAddressService;
    private final CartWishlistService cartWishlistService;
    private final OrderService orderService;
    private final RecommendationTrackingService recommendationTrackingService;

    public BookstoreAiTools(
            SachRepository sachRepository,
            DanhGiaRepository danhGiaRepository,
            BookVectorService bookVectorService,
            VoucherService voucherService,
            UserAddressService userAddressService,
            CartWishlistService cartWishlistService,
            OrderService orderService,
            RecommendationTrackingService recommendationTrackingService) {
        this.sachRepository = sachRepository;
        this.danhGiaRepository = danhGiaRepository;
        this.bookVectorService = bookVectorService;
        this.voucherService = voucherService;
        this.userAddressService = userAddressService;
        this.cartWishlistService = cartWishlistService;
        this.orderService = orderService;
        this.recommendationTrackingService = recommendationTrackingService;
    }

    // =========================================================================
    // TOOL 1: TÌM KIẾM LAI (HYBRID PGVECTOR + TỪ KHÓA NGUYÊN TỪ + CHỦ ĐỀ + GIÁ)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<BookDto> searchBooksBySemanticOrAuthor(String query, BigDecimal maxPrice, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 6);
        List<Sach> visibleInStock = sachRepository.findAllVisibleWithDetails().stream()
                .filter(s -> (s.getTonKho() == null ? 0 : s.getTonKho()) > 0)
                .filter(s -> maxPrice == null || effectivePrice(s).compareTo(maxPrice) <= 0)
                .toList();

        if (visibleInStock.isEmpty()) {
            return List.of();
        }

        if (query != null) {
            Matcher idMatcher = SACH_ID_PATTERN.matcher(query);
            if (idMatcher.find()) {
                String exactId = idMatcher.group(1);
                Optional<Sach> exact = visibleInStock.stream().filter(s -> exactId.equalsIgnoreCase(s.getId()))
                        .findFirst();
                if (exact.isPresent()) {
                    return List.of(toBookDto(exact.get()));
                }
            }
        }

        String normalizedQuery = normalizeText(query);
        Set<String> matchedCategoryIds = inferCategoryIdsFromVietnameseQuery(normalizedQuery);
        List<String> queryTokens = extractMeaningfulTokens(normalizedQuery);
        String cleanedPhrase = String.join(" ", queryTokens);

        List<String> semanticIds = (query != null && !query.isBlank())
                ? bookVectorService.findSemanticMatchIds(query, safeLimit * 3)
                : List.of();

        if (matchedCategoryIds.isEmpty() && queryTokens.isEmpty()) {
            return pickDiverseBestsellersAcrossCategories(visibleInStock, safeLimit);
        }

        record ScoredBook(Sach book, double score) {
        }

        List<ScoredBook> scored = new ArrayList<>();
        for (Sach book : visibleInStock) {
            boolean inMatchedCategory = !matchedCategoryIds.isEmpty()
                    && matchedCategoryIds.contains(book.getDanhMuc().getId());

            double score = 0.0;
            if (inMatchedCategory) {
                score += 24.0;
            }

            int semIdx = semanticIds.indexOf(book.getId());
            if (semIdx >= 0) {
                double semBoost = (matchedCategoryIds.isEmpty() || inMatchedCategory)
                        ? (20.0 - semIdx * 1.2)
                        : Math.max(0.0, 6.0 - semIdx);
                score += Math.max(0.0, semBoost);
            }

            String titleNorm = normalizeText(book.getTieuDe());
            String authorNorm = normalizeText(book.getTacGia().getTenTacGia());
            String catNorm = normalizeText(book.getDanhMuc().getTenDanhMuc());
            String keywordNorm = normalizeText(book.getTuKhoa());
            String summaryNorm = normalizeText(book.getTomTatNgan());

            Set<String> titleWords = splitWords(titleNorm);
            Set<String> authorWords = splitWords(authorNorm);
            Set<String> catAndKeywordWords = new HashSet<>(splitWords(catNorm));
            catAndKeywordWords.addAll(splitWords(keywordNorm));
            Set<String> summaryWords = splitWords(summaryNorm);

            if (!cleanedPhrase.isBlank() && cleanedPhrase.length() >= 3) {
                if (titleNorm.equals(cleanedPhrase)) {
                    score += 70.0;
                } else if (titleNorm.contains(cleanedPhrase)) {
                    score += 45.0;
                }
                if (authorNorm.contains(cleanedPhrase))
                    score += 35.0;
            }

            List<String> significantTitleWords = titleWords.stream().filter(w -> w.length() >= 3).toList();
            if (!significantTitleWords.isEmpty() && queryTokens.containsAll(significantTitleWords)) {
                score += 55.0;
            }

            for (String token : queryTokens) {
                if (titleWords.contains(token)) {
                    score += 24.0;
                } else if (token.length() >= 5 && titleNorm.contains(token)) {
                    score += 10.0;
                }
                if (authorWords.contains(token)) {
                    score += 18.0;
                }
                if (catAndKeywordWords.contains(token)) {
                    score += 5.0;
                }
                if (summaryWords.contains(token)) {
                    score += 2.5;
                }
            }

            if (!matchedCategoryIds.isEmpty() && !inMatchedCategory) {
                continue;
            }

            if (score > 0.0) {
                score += Math.min(3.0, (book.getDaBan() == null ? 0 : book.getDaBan()) / 150.0);
                scored.add(new ScoredBook(book, score));
            }
        }

        List<Sach> result;
        if (!scored.isEmpty()) {
            result = scored.stream()
                    .sorted((a, b) -> {
                        int cmp = Double.compare(b.score(), a.score());
                        if (cmp != 0)
                            return cmp;
                        int soldA = a.book().getDaBan() == null ? 0 : a.book().getDaBan();
                        int soldB = b.book().getDaBan() == null ? 0 : b.book().getDaBan();
                        return Integer.compare(soldB, soldA);
                    })
                    .limit(safeLimit)
                    .map(ScoredBook::book)
                    .toList();
        } else {
            result = pickDiverseBestsellersAcrossCategories(visibleInStock, safeLimit).stream()
                    .map(dto -> visibleInStock.stream().filter(s -> s.getId().equals(dto.id())).findFirst()
                            .orElseThrow())
                    .toList();
        }

        return result.stream().map(this::toBookDto).toList();
    }

    // =========================================================================
    // TOOL 2: SĂN SÁCH FLASH SALE HOẶC TOP BÁN CHẠY NHẤT
    // =========================================================================

    @Transactional(readOnly = true)
    public List<BookDto> getFlashSaleOrBestsellerBooks(String mode, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 6);
        List<Sach> visibleInStock = sachRepository.findAllVisibleWithDetails().stream()
                .filter(s -> (s.getTonKho() == null ? 0 : s.getTonKho()) > 0)
                .toList();

        boolean flashOnly = mode != null && mode.toUpperCase(Locale.ROOT).contains("FLASH");
        if (flashOnly) {
            return visibleInStock.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getDangFlashSale())
                            && s.getGiaFlashSale() != null
                            && s.getGiaFlashSale().compareTo(BigDecimal.ZERO) > 0)
                    .map(this::toBookDto)
                    .sorted((a, b) -> {
                        int cmp = Integer.compare(b.phanTramGiam(), a.phanTramGiam());
                        return cmp != 0 ? cmp : Integer.compare(b.daBan(), a.daBan());
                    })
                    .limit(safeLimit)
                    .toList();
        }

        return visibleInStock.stream()
                .sorted((a, b) -> Integer.compare(
                        b.getDaBan() == null ? 0 : b.getDaBan(),
                        a.getDaBan() == null ? 0 : a.getDaBan()))
                .limit(safeLimit)
                .map(this::toBookDto)
                .toList();
    }

    // =========================================================================
    // TOOL 3: ĐỀ XUẤT SÁCH CÁ NHÂN HÓA "HỢP GU CỦA BẠN" (BƯỚC 8A MULTI-INTEREST)
    // =========================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getPersonalizedRecommendations(String nullableNguoiDungId) {
        RecommendationResponse rec = recommendationTrackingService.getRecommendations(nullableNguoiDungId);
        List<BookDto> topBooks = rec.books().stream().limit(4).toList();
        List<String> topTopicNames = rec.topCategories() == null ? List.of() : rec.topCategories();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("reasonLabel", rec.reasonLabel());
        out.put("personalized", !topTopicNames.isEmpty());
        out.put("favoriteTopics", topTopicNames);
        out.put("books", topBooks);
        return out;
    }

    // =========================================================================
    // TOOL 4: TƯ VẤN GHÉP COMBO 2-3 CUỐN SÁCH THEO NGÂN SÁCH / VỪA ĐỦ FREESHIP
    // =========================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> suggestBookComboByBudget(String topicOrQuery, BigDecimal budgetVnd, int itemCount) {
        BigDecimal targetBudget = (budgetVnd != null && budgetVnd.compareTo(BigDecimal.valueOf(50_000L)) >= 0)
                ? budgetVnd
                : BigDecimal.valueOf(350_000L);
        int desiredCount = Math.min(Math.max(itemCount, 2), 3);

        List<BookDto> pool = searchBooksBySemanticOrAuthor(topicOrQuery, targetBudget, 6);
        if (pool.size() < 2) {
            pool = getFlashSaleOrBestsellerBooks("BESTSELLER", 6).stream()
                    .filter(b -> b.donGiaApDung().compareTo(targetBudget) <= 0)
                    .toList();
        }

        // Chọn tổ hợp 2-3 cuốn có tổng tiền gần ngân sách mục tiêu nhất mà không vượt
        // quá targetBudget
        List<BookDto> bestCombo = new ArrayList<>();
        BigDecimal bestSum = BigDecimal.ZERO;

        List<BookDto> sortedByPriceAsc = pool.stream()
                .sorted(Comparator.comparing(BookDto::donGiaApDung))
                .toList();

        for (BookDto b : sortedByPriceAsc) {
            if (bestCombo.size() >= desiredCount)
                break;
            if (bestSum.add(b.donGiaApDung()).compareTo(targetBudget) <= 0) {
                bestCombo.add(b);
                bestSum = bestSum.add(b.donGiaApDung());
            }
        }

        if (bestCombo.isEmpty() && !sortedByPriceAsc.isEmpty()) {
            bestCombo.add(sortedByPriceAsc.get(0));
            bestSum = sortedByPriceAsc.get(0).donGiaApDung();
        }

        boolean qualifiesFreeship = bestSum.compareTo(FREESHIP_THRESHOLD) >= 0;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("budgetVnd", targetBudget);
        out.put("comboCount", bestCombo.size());
        out.put("comboSubtotalVnd", bestSum);
        out.put("qualifiesFreeship", qualifiesFreeship);
        out.put("shippingFeeVnd", qualifiesFreeship ? BigDecimal.ZERO : BigDecimal.valueOf(25_000L));
        out.put("books", bestCombo);
        return out;
    }

    // =========================================================================
    // TOOL 5: SO SÁNH SÁCH & ĐỌC ĐÁNH GIÁ THỰC TẾ CỦA ĐỘC GIẢ TRONG CSDL
    // =========================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> compareBooksOrReadReviews(String bookQuery1, String bookQuery2) {
        List<Map<String, Object>> details = new ArrayList<>();
        List<BookDto> matchedDtos = new ArrayList<>();

        for (String q : List.of(bookQuery1 == null ? "" : bookQuery1, bookQuery2 == null ? "" : bookQuery2)) {
            if (q.isBlank())
                continue;
            BookDto dto = resolveBookByIdOrQuery(q);
            if (dto == null || matchedDtos.stream().anyMatch(x -> x.id().equals(dto.id()))) {
                continue;
            }
            matchedDtos.add(dto);
            Sach entity = sachRepository.findByIdWithDetails(dto.id()).orElse(null);
            List<DanhGia> reviews = danhGiaRepository.findVisibleReviewsBySachId(dto.id()).stream().limit(3).toList();

            List<Map<String, Object>> reviewSummaries = reviews.stream().map(r -> {
                Map<String, Object> rm = new LinkedHashMap<>();
                rm.put("reviewer", r.getNguoiDung() != null ? r.getNguoiDung().getHoTen() : "Độc giả");
                rm.put("rating", r.getSoSao());
                rm.put("comment", r.getBinhLuan());
                return rm;
            }).toList();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sachId", dto.id());
            item.put("tieuDe", dto.tieuDe());
            item.put("tacGia", dto.tenTacGia());
            item.put("danhMuc", dto.tenDanhMuc());
            item.put("nhaXuatBan", dto.tenNhaXuatBan());
            item.put("giaApDungVnd", dto.donGiaApDung());
            item.put("giaGocVnd", dto.giaGoc());
            item.put("dangFlashSale", dto.dangFlashSale());
            item.put("loaiBia", dto.loaiBia());
            item.put("soTrang", entity != null ? entity.getSoTrang() : null);
            item.put("namXuatBan", dto.namXuatBan());
            item.put("diemDanhGiaTb", dto.diemDanhGiaTb());
            item.put("soLuotDanhGia", dto.soLuotDanhGia());
            item.put("daBan", dto.daBan());
            item.put("tonKho", dto.tonKho());
            item.put("tomTat", dto.tomTatNgan());
            item.put("recentReviews", reviewSummaries);
            details.add(item);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("foundCount", details.size());
        out.put("bookDetails", details);
        out.put("books", matchedDtos);
        return out;
    }

    // =========================================================================
    // TOOL 6: XEM VOUCHER ĐANG MỞ & ĐỊA CHỈ MẶC ĐỊNH CỦA KHÁCH
    // =========================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getActiveVouchersAndDefaultAddress(String nullableNguoiDungId) {
        List<VoucherDto> vouchers = voucherService.getActiveVouchers();
        AddressDto defaultAddress = null;

        if (nullableNguoiDungId != null && !nullableNguoiDungId.isBlank()) {
            List<AddressDto> addresses = userAddressService.getAddresses(nullableNguoiDungId);
            defaultAddress = addresses.stream()
                    .filter(a -> Boolean.TRUE.equals(a.macDinh()))
                    .findFirst()
                    .orElse(addresses.isEmpty() ? null : addresses.get(0));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activeVouchers", vouchers);
        result.put("loggedIn", nullableNguoiDungId != null && !nullableNguoiDungId.isBlank());
        result.put("defaultAddress", defaultAddress);
        return result;
    }

    // =========================================================================
    // TOOL 7: QUẢN LÝ GIỎ HÀNG TOÀN DIỆN (XEM / THÊM / ĐỔI SỐ LƯỢNG / XÓA)
    // =========================================================================

    public Map<String, Object> manageCustomerCart(
            String nullableNguoiDungId,
            String action,
            String sachIdOrTitle,
            int soLuong) {
        if (nullableNguoiDungId == null || nullableNguoiDungId.isBlank()) {
            return Map.of(
                    "success", false,
                    "requireLogin", true,
                    "message", "Bạn vui lòng đăng nhập tài khoản khách hàng trước để thao tác với giỏ hàng nhé.");
        }
        String act = action == null ? "VIEW" : action.trim().toUpperCase(Locale.ROOT);
        try {
            return switch (act) {
                case "ADD" -> addBookToCustomerCart(nullableNguoiDungId, sachIdOrTitle, soLuong);
                case "UPDATE_QTY" -> {
                    BookDto resolved = resolveBookByIdOrQuery(sachIdOrTitle);
                    if (resolved == null) {
                        yield Map.of("success", false, "message",
                                "Không tìm thấy cuốn sách \"" + sachIdOrTitle + "\".");
                    }
                    int qty = Math.max(1, soLuong);
                    CartResponseDto cart = cartWishlistService.updateCartItem(
                            nullableNguoiDungId,
                            resolved.id(),
                            new CartUpdateRequest(qty, true));
                    yield buildCartToolSummary(cart, true, resolved,
                            "Đã cập nhật số lượng \"" + resolved.tieuDe() + "\" trong giỏ thành " + qty + " cuốn.");
                }
                case "REMOVE" -> {
                    BookDto resolved = resolveBookByIdOrQuery(sachIdOrTitle);
                    if (resolved == null) {
                        yield Map.of("success", false, "message",
                                "Không tìm thấy cuốn sách \"" + sachIdOrTitle + "\".");
                    }
                    CartResponseDto cart = cartWishlistService.removeCartItem(nullableNguoiDungId, resolved.id());
                    yield buildCartToolSummary(cart, true, null,
                            "Đã xóa \"" + resolved.tieuDe() + "\" khỏi giỏ hàng của bạn.");
                }
                default -> {
                    CartResponseDto cart = cartWishlistService.getCart(nullableNguoiDungId);
                    yield buildCartToolSummary(cart, false, null, "Lấy thông tin giỏ hàng thành công.");
                }
            };
        } catch (Exception ex) {
            return Map.of("success", false, "message", ex.getMessage());
        }
    }

    public Map<String, Object> addBookToCustomerCart(String nullableNguoiDungId, String sachIdOrTitle, int soLuong) {
        if (nullableNguoiDungId == null || nullableNguoiDungId.isBlank()) {
            return Map.of(
                    "success", false,
                    "requireLogin", true,
                    "message",
                    "Bạn vui lòng đăng nhập tài khoản khách hàng trước để mình thêm sách vào giỏ giúp bạn nhé.");
        }
        try {
            BookDto resolved = resolveBookByIdOrQuery(sachIdOrTitle);
            if (resolved == null) {
                return Map.of("success", false, "message",
                        "Không tìm thấy cuốn sách \"" + sachIdOrTitle + "\" trong kho.");
            }
            int qty = Math.max(1, soLuong);
            CartResponseDto cart = cartWishlistService.addToCart(
                    nullableNguoiDungId,
                    new CartItemRequest(resolved.id(), qty));
            return buildCartToolSummary(cart, true, resolved,
                    "Đã thêm " + qty + " cuốn \"" + resolved.tieuDe() + "\" vào giỏ hàng thành công.");
        } catch (Exception ex) {
            return Map.of("success", false, "message", ex.getMessage());
        }
    }

    private Map<String, Object> buildCartToolSummary(
            CartResponseDto cart,
            boolean mutated,
            BookDto touchedBook,
            String msg) {
        BigDecimal missingForFreeship = cart.tamTinh().compareTo(FREESHIP_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : FREESHIP_THRESHOLD.subtract(cart.tamTinh());

        List<Map<String, Object>> items = cart.items().stream().map(it -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sachId", it.sachId());
            m.put("tieuDe", it.tieuDe());
            m.put("soLuong", it.soLuong());
            m.put("donGia", it.donGiaApDung());
            m.put("thanhTien", it.thanhTien());
            m.put("daChon", it.daChon());
            return m;
        }).toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("cartMutated", mutated);
        if (touchedBook != null) {
            out.put("book", touchedBook);
        }
        out.put("totalItemsInCart", cart.tongSoLuongTrongGio());
        out.put("items", items);
        out.put("subtotalVnd", cart.tamTinh());
        out.put("shippingFeeVnd", cart.phiVanChuyen());
        out.put("missingForFreeshipVnd", missingForFreeship);
        out.put("message", msg);
        return out;
    }

    // =========================================================================
    // TOOL 8: QUẢN LÝ TỦ SÁCH YÊU THÍCH (XEM / THẢ TIM / BỎ TIM)
    // =========================================================================

    public Map<String, Object> manageCustomerWishlist(
            String nullableNguoiDungId,
            String action,
            String sachIdOrTitle) {
        if (nullableNguoiDungId == null || nullableNguoiDungId.isBlank()) {
            return Map.of(
                    "success", false,
                    "requireLogin", true,
                    "message", "Bạn vui lòng đăng nhập tài khoản trước để lưu Tủ sách yêu thích nhé.");
        }
        String act = action == null ? "VIEW" : action.trim().toUpperCase(Locale.ROOT);
        try {
            if ("TOGGLE".equals(act) || "ADD".equals(act) || "REMOVE".equals(act)) {
                BookDto resolved = resolveBookByIdOrQuery(sachIdOrTitle);
                if (resolved == null) {
                    return Map.of("success", false, "message", "Không tìm thấy cuốn sách \"" + sachIdOrTitle + "\".");
                }
                WishlistToggleResponse toggleRes = cartWishlistService.toggle(nullableNguoiDungId, resolved.id());
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("success", true);
                out.put("wishlistMutated", true);
                out.put("book", resolved);
                out.put("liked", toggleRes.daYeuThich());
                out.put("totalWishlistCount", toggleRes.tongSoSachYeuThich());
                out.put("message", toggleRes.daYeuThich()
                        ? "Đã thêm \"" + resolved.tieuDe() + "\" vào Tủ sách yêu thích."
                        : "Đã gỡ \"" + resolved.tieuDe() + "\" khỏi Tủ sách yêu thích.");
                return out;
            }

            List<WishlistItemResponse> list = cartWishlistService.getWishlist(nullableNguoiDungId);
            List<BookDto> books = list.stream()
                    .map(w -> sachRepository.findByIdWithDetails(w.sachId()).map(this::toBookDto).orElse(null))
                    .filter(b -> b != null)
                    .limit(4)
                    .toList();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("wishlistMutated", false);
            out.put("totalWishlistCount", list.size());
            out.put("books", books);
            out.put("titles", list.stream().map(WishlistItemResponse::tieuDe).toList());
            return out;
        } catch (Exception ex) {
            return Map.of("success", false, "message", ex.getMessage());
        }
    }

    // =========================================================================
    // TOOL 9: TỰ ĐẶT ĐƠN NGAY KHI KHÁCH YÊU CẦU + TỰ SĂN MÃ GIẢM GIÁ TỐT NHẤT
    // =========================================================================

    public Map<String, Object> placeOrderForCustomer(String nullableNguoiDungId, String sachIdOrTitle, int soLuong) {
        return placeOrderOrCheckoutForCustomer(nullableNguoiDungId, "DIRECT_BOOK", sachIdOrTitle, soLuong, null);
    }

    public Map<String, Object> placeOrderOrCheckoutForCustomer(
            String nullableNguoiDungId,
            String mode,
            String sachIdOrTitle,
            int soLuong,
            String voucherCode) {
        if (nullableNguoiDungId == null || nullableNguoiDungId.isBlank()) {
            return Map.of(
                    "success", false,
                    "requireLogin", true,
                    "message",
                    "Bạn vui lòng đăng nhập tài khoản khách hàng trước để mình tự động kiểm tra địa chỉ mặc định và đặt đơn giúp bạn nhé!");
        }
        String orderMode = mode == null ? "DIRECT_BOOK" : mode.trim().toUpperCase(Locale.ROOT);
        try {
            if ("CART_CHECKOUT".equals(orderMode)) {
                String chosenCode = (voucherCode != null && !voucherCode.isBlank()
                        && !"AUTO".equalsIgnoreCase(voucherCode.trim()))
                                ? voucherCode.trim()
                                : voucherService.findBestVoucherForSelectedCart(nullableNguoiDungId)
                                        .map(VoucherValidationResponse::voucher)
                                        .map(VoucherDto::maCode)
                                        .orElse(null);

                OrderDto order = orderService.createOrder(
                        nullableNguoiDungId,
                        new CreateOrderRequest(
                                null,
                                PhuongThucThanhToan.TIEN_MAT_COD,
                                chosenCode,
                                "Thanh toán giỏ hàng qua Trợ lý AI"));
                return buildOrderSuccessPayload(order, true);
            }

            BookDto resolved = resolveBookByIdOrQuery(sachIdOrTitle);
            if (resolved == null) {
                return Map.of(
                        "success", false,
                        "message", "Mình chưa tìm thấy cuốn sách bạn muốn đặt (\"" + sachIdOrTitle
                                + "\"). Bạn cho mình xin tên sách rõ hơn nhé!");
            }
            int qty = Math.max(1, soLuong);
            OrderDto order = orderService.createDirectOrderForAi(
                    nullableNguoiDungId,
                    resolved.id(),
                    qty,
                    voucherCode,
                    true,
                    "Đặt tự động qua Trợ lý AI");
            Map<String, Object> payload = buildOrderSuccessPayload(order, false);
            payload.put("book", resolved);
            return payload;
        } catch (Exception ex) {
            return Map.of("success", false, "message", ex.getMessage());
        }
    }

    private Map<String, Object> buildOrderSuccessPayload(OrderDto order, boolean fromCart) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("fromCart", fromCart);
        out.put("orderId", order.id());
        out.put("orderCode", order.maDonHang());
        out.put("subtotalVnd", order.tamTinh());
        out.put("shippingFeeVnd", order.phiVanChuyen());
        out.put("discountVnd", order.tienGiamGia());
        out.put("appliedVoucherCode", order.maGiamGiaCode());
        out.put("totalAmount", order.tongThanhToan());
        out.put("paymentMethod", "Thanh toán khi nhận hàng (COD)");
        out.put("receiverName", order.tenKhachHang());
        out.put("receiverPhone", order.sdtKhachHang());
        out.put("shippingAddress", order.diaChiGiaoHang());
        out.put("items", order.items());
        out.put("message", "Đã đặt đơn hàng " + order.maDonHang() + " thành công!");
        return out;
    }

    // =========================================================================
    // TOOL 10: TRA CỨU HÀNH TRÌNH ĐƠN HÀNG & HỦY ĐƠN ĐANG CHỜ XÁC NHẬN
    // =========================================================================

    public Map<String, Object> lookupOrCancelCustomerOrder(
            String nullableNguoiDungId,
            String action,
            String maDonHangOrId,
            String cancelReason) {
        if (nullableNguoiDungId == null || nullableNguoiDungId.isBlank()) {
            return Map.of(
                    "success", false,
                    "requireLogin", true,
                    "message", "Bạn vui lòng đăng nhập tài khoản trước để kiểm tra hoặc hủy đơn hàng của mình nhé.");
        }
        String act = action == null ? "LIST_RECENT" : action.trim().toUpperCase(Locale.ROOT);
        try {
            List<OrderDto> allOrders = orderService.getCustomerOrders(nullableNguoiDungId, null);
            if (allOrders.isEmpty()) {
                return Map.of("success", true, "orderCount", 0, "orders", List.of(),
                        "message", "Tài khoản của bạn hiện chưa có đơn hàng nào.");
            }

            if ("CANCEL".equals(act)) {
                OrderDto target = findOrderByCodeOrLatestPending(allOrders, maDonHangOrId);
                if (target == null) {
                    return Map.of("success", false,
                            "message", "Không tìm thấy đơn hàng phù hợp đang ở trạng thái Chờ xác nhận để hủy.");
                }
                String reason = (cancelReason == null || cancelReason.isBlank())
                        ? "Khách yêu cầu hủy đơn qua Trợ lý AI"
                        : cancelReason.trim();
                OrderDto cancelled = orderService.cancelCustomerOrder(
                        nullableNguoiDungId,
                        target.id(),
                        new CancelOrderRequest(reason));
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("success", true);
                out.put("cancelledOrder", true);
                out.put("orderCode", cancelled.maDonHang());
                out.put("status", cancelled.trangThaiDon());
                out.put("message", "Đã hủy đơn hàng " + cancelled.maDonHang() + " và hoàn tồn kho thành công.");
                return out;
            }

            if ("DETAIL".equals(act) && maDonHangOrId != null && !maDonHangOrId.isBlank()) {
                OrderDto target = findOrderByCodeOrId(allOrders, maDonHangOrId.trim());
                if (target != null) {
                    return Map.of("success", true, "order", summarizeOrder(target));
                }
            }

            List<Map<String, Object>> recent = allOrders.stream()
                    .limit(3)
                    .map(this::summarizeOrder)
                    .toList();
            return Map.of("success", true, "orderCount", allOrders.size(), "recentOrders", recent);
        } catch (Exception ex) {
            return Map.of("success", false, "message", ex.getMessage());
        }
    }

    private OrderDto findOrderByCodeOrId(List<OrderDto> orders, String ref) {
        return orders.stream()
                .filter(o -> ref.equalsIgnoreCase(o.maDonHang()) || ref.equalsIgnoreCase(o.id()))
                .findFirst()
                .orElse(null);
    }

    private OrderDto findOrderByCodeOrLatestPending(List<OrderDto> orders, String ref) {
        if (ref != null && !ref.isBlank()) {
            return findOrderByCodeOrId(orders, ref.trim());
        }
        return orders.stream()
                .filter(o -> o.trangThaiDon() != null && "CHO_XAC_NHAN".equals(o.trangThaiDon().name()))
                .findFirst()
                .orElse(orders.get(0));
    }

    private Map<String, Object> summarizeOrder(OrderDto o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", o.id());
        m.put("orderCode", o.maDonHang());
        m.put("status", o.trangThaiDon());
        m.put("totalAmountVnd", o.tongThanhToan());
        m.put("discountVnd", o.tienGiamGia());
        m.put("voucherCode", o.maGiamGiaCode());
        m.put("shippingAddress", o.diaChiGiaoHang());
        m.put("bookTitles", o.items() == null ? List.of()
                : o.items().stream()
                        .map(i -> i.tieuDeSach() + " (x" + i.soLuong() + ")").toList());
        if (o.timeline() != null && !o.timeline().isEmpty()) {
            var latest = o.timeline().get(o.timeline().size() - 1);
            m.put("latestMilestone", latest.tieuDe() + " - " + latest.thoiGian());
        }
        return m;
    }

    // =========================================================================
    // CÁC HÀM PHÂN GIẢI & TIỆN ÍCH NỘI BỘ
    // =========================================================================

    @Transactional(readOnly = true)
    public BookDto resolveBookByIdOrQuery(String sachIdOrQuery) {
        if (sachIdOrQuery == null || sachIdOrQuery.isBlank()) {
            return null;
        }
        String trimmed = sachIdOrQuery.trim();
        Matcher m = SACH_ID_PATTERN.matcher(trimmed);
        if (m.find()) {
            Optional<Sach> byTag = sachRepository.findByIdWithDetails(m.group(1));
            if (byTag.isPresent()) {
                return toBookDto(byTag.get());
            }
        }
        Optional<Sach> direct = sachRepository.findByIdWithDetails(trimmed);
        if (direct.isPresent()) {
            return toBookDto(direct.get());
        }
        List<BookDto> searched = searchBooksBySemanticOrAuthor(trimmed, null, 1);
        return searched.isEmpty() ? null : searched.get(0);
    }

    private List<BookDto> pickDiverseBestsellersAcrossCategories(List<Sach> books, int limit) {
        List<Sach> sorted = books.stream()
                .sorted(Comparator.comparingInt((Sach s) -> s.getDaBan() == null ? 0 : s.getDaBan()).reversed())
                .toList();
        List<BookDto> picked = new ArrayList<>();
        Set<String> seenCategories = new HashSet<>();
        for (Sach s : sorted) {
            if (seenCategories.add(s.getDanhMuc().getId())) {
                picked.add(toBookDto(s));
                if (picked.size() >= limit) {
                    return picked;
                }
            }
        }
        for (Sach s : sorted) {
            if (picked.stream().noneMatch(b -> b.id().equals(s.getId()))) {
                picked.add(toBookDto(s));
                if (picked.size() >= limit) {
                    break;
                }
            }
        }
        return picked;
    }

    public Set<String> inferCategoryIdsFromVietnameseQuery(String q) {
        Set<String> cats = new LinkedHashSet<>();
        if (q == null || q.isBlank())
            return cats;

        if (containsAnyPhraseOrWord(q, "lap trinh", "cong nghe", "phan mem", "kien truc phan mem", "du lieu",
                "may tinh", "thuat toan", "code", "coding", "software", "java", "python", "javascript", "web", "mobile",
                "data", "devops", "clean code")) {
            cats.add("dm_cong_nghe");
            cats.add("dm_kien_truc_pm");
        }
        if (containsAnyPhraseOrWord(q, "kinh te", "quan tri", "kinh doanh", "lanh dao", "marketing", "ban hang",
                "doanh nghiep", "ceo", "quan ly")) {
            cats.add("dm_kinh_te");
            cats.add("dm_khoi_nghiep");
            cats.add("dm_tai_chinh");
        }
        if (containsAnyPhraseOrWord(q, "tai chinh", "dau tu", "chung khoan", "tien bac", "ke toan", "tiet kiem",
                "lam giau")) {
            cats.add("dm_tai_chinh");
        }
        if (containsAnyPhraseOrWord(q, "khoi nghiep", "doi moi", "startup", "sang tao")) {
            cats.add("dm_khoi_nghiep");
        }
        if (containsAnyPhraseOrWord(q, "trinh tham", "bi an", "pha an", "tham tu", "hinh su", "sherlock", "holmes",
                "agatha")) {
            cats.add("dm_trinh_tham");
        }
        if (containsAnyPhraseOrWord(q, "vien tuong", "sci fi", "scifi", "tuong lai", "ngoai hanh tinh", "du hanh",
                "gia tuong")) {
            cats.add("dm_vien_tuong");
        }
        if (containsAnyPhraseOrWord(q, "van hoc", "tieu thuyet", "kinh dien", "truyen ngan", "lang man", "tinh cam",
                "tho")) {
            cats.add("dm_van_hoc");
        }
        if (containsAnyPhraseOrWord(q, "ky nang", "tam ly", "chua lanh", "phat trien ban than", "thoi quen",
                "giao tiep", "cam xuc", "tu duy", "song dep", "hanh phuc", "self help")) {
            cats.add("dm_ky_nang");
        }
        if (containsAnyPhraseOrWord(q, "khoa hoc", "lich su", "vat ly", "tien hoa", "sinh hoc", "thien van", "vu tru",
                "nhan loai", "van minh")) {
            cats.add("dm_khoa_hoc");
        }
        if (containsAnyPhraseOrWord(q, "thieu nhi", "tre em", "tuoi moi lon", "co tich", "thieu nien", "be",
                "hoc sinh")) {
            cats.add("dm_thieu_nhi");
        }
        if (containsAnyPhraseOrWord(q, "ngoai ngu", "tieng anh", "hoc thuat", "tu vung", "ngu phap", "ielts", "toeic",
                "giao trinh")) {
            cats.add("dm_ngoai_ngu");
        }
        return cats;
    }

    private boolean containsAnyPhraseOrWord(String normalizedText, String... patterns) {
        Set<String> words = splitWords(normalizedText);
        for (String p : patterns) {
            if (p.contains(" ")) {
                if (normalizedText.contains(p)) {
                    return true;
                }
            } else if (words.contains(p)) {
                return true;
            }
        }
        return false;
    }

    private List<String> extractMeaningfulTokens(String normalized) {
        if (normalized == null || normalized.isBlank())
            return List.of();
        List<String> tokens = new ArrayList<>();
        for (String raw : normalized.split("[^a-z0-9]+")) {
            if (raw.length() >= 2 && !VIETNAMESE_STOP_WORDS.contains(raw) && !raw.matches("\\d+k?")) {
                tokens.add(raw);
            }
        }
        return tokens;
    }

    private Set<String> splitWords(String normalized) {
        if (normalized == null || normalized.isBlank())
            return Set.of();
        return Arrays.stream(normalized.split("[^a-z0-9]+"))
                .filter(w -> !w.isBlank())
                .collect(Collectors.toSet());
    }

    private String normalizeText(String input) {
        if (input == null)
            return "";
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        return decomposed
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private BigDecimal effectivePrice(Sach book) {
        if (Boolean.TRUE.equals(book.getDangFlashSale())
                && book.getGiaFlashSale() != null
                && book.getGiaFlashSale().compareTo(BigDecimal.ZERO) > 0) {
            return book.getGiaFlashSale();
        }
        return book.getGiaBan() == null ? BigDecimal.ZERO : book.getGiaBan();
    }

    private BookDto toBookDto(Sach book) {
        BigDecimal effective = effectivePrice(book);
        int discountPercent = 0;
        if (book.getGiaGoc() != null
                && book.getGiaGoc().compareTo(BigDecimal.ZERO) > 0
                && effective.compareTo(book.getGiaGoc()) < 0) {
            discountPercent = book.getGiaGoc().subtract(effective)
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
                effective,
                discountPercent,
                book.getTonKho(),
                book.getDaBan(),
                book.getDiemDanhGiaTb(),
                book.getSoLuotDanhGia(),
                book.getLoaiBia(),
                book.getNamXuatBan(),
                book.getAnhBiaUrl(),
                book.getTomTatNgan(),
                Boolean.TRUE.equals(book.getBanChay()));
    }
}
