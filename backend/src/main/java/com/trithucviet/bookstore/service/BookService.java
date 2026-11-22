package com.trithucviet.bookstore.service;

import com.trithucviet.bookstore.dto.response.BookDetailDto;
import com.trithucviet.bookstore.dto.response.BookDto;
import com.trithucviet.bookstore.dto.response.BookPageResponse;
import com.trithucviet.bookstore.dto.response.CategoryDto;
import com.trithucviet.bookstore.dto.response.HomeBooksResponse;
import com.trithucviet.bookstore.dto.response.ReviewDto;
import com.trithucviet.bookstore.entity.DanhGia;
import com.trithucviet.bookstore.entity.DanhMuc;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.exception.BusinessException;
import com.trithucviet.bookstore.repository.DanhGiaRepository;
import com.trithucviet.bookstore.repository.DanhMucRepository;
import com.trithucviet.bookstore.repository.SachRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ Danh mục, Trang chủ, Tìm kiếm, Lọc, Sắp xếp & Chi tiết Sách (Bước 3 - phần 1 & phần 2).
 *
 * Lưu ý kỹ thuật:
 * - Giá áp dụng thực tế (donGiaApDung) luôn tuân thủ Quy tắc 2.2: ưu tiên giaFlashSale nếu dangFlashSale = true và giaFlashSale > 0.
 * - Khi lọc theo danh mục cha (ví dụ dm_van_hoc), tự động gom cả sách nằm trong các danh mục con (dm_trinh_tham, dm_vien_tuong).
 * - Hỗ trợ đầy đủ 8 kiểu sắp xếp chuẩn sàn TMĐT: popular, price_asc, price_desc, discount_desc, rating, newest, title_asc, title_desc.
 */
@Service
public class BookService {

    private static final int DEFAULT_HOME_LIMIT = 12;
    private static final int MAX_HOME_LIMIT = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int RELATED_BOOKS_LIMIT = 6;

    private final DanhMucRepository danhMucRepository;
    private final SachRepository sachRepository;
    private final DanhGiaRepository danhGiaRepository;

    public BookService(
            DanhMucRepository danhMucRepository,
            SachRepository sachRepository,
            DanhGiaRepository danhGiaRepository
    ) {
        this.danhMucRepository = danhMucRepository;
        this.sachRepository = sachRepository;
        this.danhGiaRepository = danhGiaRepository;
    }

    /**
     * Lấy cây danh mục Cha - Con đang hoạt động kèm số lượng sách đang hiển thị.
     * Danh mục cha tự động cộng gộp số sách của chính nó và tất cả danh mục con đang hoạt động.
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> getActiveCategoryTree() {
        List<DanhMuc> activeCategories = danhMucRepository.findActiveCategoriesOrdered();
        List<Sach> visibleBooks = sachRepository.findAllVisibleWithDetails();

        // Đếm số sách trực tiếp thuộc từng danh mục
        Map<String, Long> directBookCounts = new HashMap<>();
        for (Sach book : visibleBooks) {
            String catId = book.getDanhMuc().getId();
            directBookCounts.merge(catId, 1L, Long::sum);
        }

        // Nhóm các danh mục con theo danhMucCha.id
        Map<String, List<DanhMuc>> childrenByParent = new HashMap<>();
        List<DanhMuc> rootCategories = new ArrayList<>();
        for (DanhMuc category : activeCategories) {
            if (category.getDanhMucCha() == null) {
                rootCategories.add(category);
            } else {
                childrenByParent
                        .computeIfAbsent(category.getDanhMucCha().getId(), k -> new ArrayList<>())
                        .add(category);
            }
        }

        List<CategoryDto> result = new ArrayList<>();
        for (DanhMuc root : rootCategories) {
            List<DanhMuc> children = childrenByParent.getOrDefault(root.getId(), List.of());
            List<CategoryDto> childDtos = new ArrayList<>();
            long totalCount = directBookCounts.getOrDefault(root.getId(), 0L);

            for (DanhMuc child : children) {
                long childCount = directBookCounts.getOrDefault(child.getId(), 0L);
                totalCount += childCount;
                childDtos.add(new CategoryDto(
                        child.getId(),
                        child.getTenDanhMuc(),
                        child.getDuongDan(),
                        root.getId(),
                        child.getMoTa(),
                        child.getTenBieuTuong(),
                        child.getThuTuHienThi(),
                        childCount,
                        List.of()
                ));
            }

            result.add(new CategoryDto(
                    root.getId(),
                    root.getTenDanhMuc(),
                    root.getDuongDan(),
                    null,
                    root.getMoTa(),
                    root.getTenBieuTuong(),
                    root.getThuTuHienThi(),
                    totalCount,
                    childDtos
            ));
        }
        return result;
    }

    /**
     * Lấy 2 kệ sách nổi bật cho Trang chủ: Flash Sale (ưu tiên % giảm giá cao nhất) & Bán chạy (ưu tiên daBan cao nhất).
     * Mặc định trả tối đa 12 cuốn mỗi kệ (có thể đổi qua tham số limit từ 1..100).
     */
    @Transactional(readOnly = true)
    public HomeBooksResponse getHomeBooks(Integer limitParam) {
        int limit = normalizeLimit(limitParam, DEFAULT_HOME_LIMIT, MAX_HOME_LIMIT);
        List<Sach> visibleBooks = sachRepository.findAllVisibleWithDetails();

        List<Sach> allFlashSale = visibleBooks.stream()
                .filter(s -> Boolean.TRUE.equals(s.getDangFlashSale()))
                .sorted(buildComparator("discount_desc"))
                .toList();

        List<Sach> allBestSeller = visibleBooks.stream()
                .filter(s -> Boolean.TRUE.equals(s.getBanChay()))
                .sorted(buildComparator("popular"))
                .toList();

        List<BookDto> flashSaleDtos = allFlashSale.stream().limit(limit).map(this::toBookDto).toList();
        List<BookDto> bestSellerDtos = allBestSeller.stream().limit(limit).map(this::toBookDto).toList();

        return new HomeBooksResponse(
                flashSaleDtos,
                bestSellerDtos,
                allFlashSale.size(),
                allBestSeller.size(),
                visibleBooks.size()
        );
    }

    /**
     * Tìm kiếm, lọc đa tiêu chí, sắp xếp chuẩn sàn TMĐT và phân trang danh sách sách cho Mobile App.
     */
    @Transactional(readOnly = true)
    public BookPageResponse searchAndFilterBooks(
            String search,
            String categoryId,
            String authorId,
            String publisherId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            Boolean flashSaleOnly,
            Boolean bestSellerOnly,
            Boolean inStockOnly,
            String coverType,
            String sortBy,
            Integer pageParam,
            Integer sizeParam
    ) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException("Giá tối thiểu không được lớn hơn giá tối đa.");
        }

        int page = (pageParam == null || pageParam < 0) ? 0 : pageParam;
        int size = normalizeLimit(sizeParam, DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        String normalizedSort = normalizeSortBy(sortBy);

        String normalizedSearch = normalizeBlankToNull(search);
        String normalizedCategoryId = normalizeCategoryFilter(categoryId);
        String normalizedAuthorId = normalizeBlankToNull(authorId);
        String normalizedPublisherId = normalizeBlankToNull(publisherId);
        String normalizedCoverType = normalizeBlankToNull(coverType);

        // Nếu lọc theo danh mục cha -> gom luôn danh sách ID của danh mục cha và mọi danh mục con đang hoạt động
        Set<String> targetCategoryIds = resolveCategoryFilterIds(normalizedCategoryId);

        List<Sach> filtered = sachRepository.findAllVisibleWithDetails()
                .stream()
                .filter(book -> matchesSearch(book, normalizedSearch))
                .filter(book -> targetCategoryIds == null || targetCategoryIds.contains(book.getDanhMuc().getId()))
                .filter(book -> normalizedAuthorId == null || book.getTacGia().getId().equalsIgnoreCase(normalizedAuthorId))
                .filter(book -> normalizedPublisherId == null || book.getNhaXuatBan().getId().equalsIgnoreCase(normalizedPublisherId))
                .filter(book -> matchesPriceRange(book, minPrice, maxPrice))
                .filter(book -> minRating == null || safeDecimal(book.getDiemDanhGiaTb()).compareTo(minRating) >= 0)
                .filter(book -> !Boolean.TRUE.equals(flashSaleOnly) || Boolean.TRUE.equals(book.getDangFlashSale()))
                .filter(book -> !Boolean.TRUE.equals(bestSellerOnly) || Boolean.TRUE.equals(book.getBanChay()))
                .filter(book -> !Boolean.TRUE.equals(inStockOnly) || (book.getTonKho() != null && book.getTonKho() > 0))
                .filter(book -> normalizedCoverType == null
                        || (book.getLoaiBia() != null && book.getLoaiBia().trim().equalsIgnoreCase(normalizedCoverType)))
                .sorted(buildComparator(normalizedSort))
                .toList();

        long totalElements = filtered.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());

        List<BookDto> pageItems = filtered.subList(fromIndex, toIndex)
                .stream()
                .map(this::toBookDto)
                .toList();

        return new BookPageResponse(
                pageItems,
                page,
                size,
                totalElements,
                totalPages,
                page + 1 < totalPages,
                page > 0 && totalPages > 0,
                normalizedSort
        );
    }

    /**
     * Lấy chi tiết 1 cuốn sách đang hiển thị cùng tối đa 6 cuốn sách cùng chủ đề (cùng danh mục hoặc cùng danh mục cha).
     */
    @Transactional(readOnly = true)
    public BookDetailDto getBookDetail(String bookId) {
        Sach book = sachRepository.findVisibleByIdWithDetails(bookId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy sách."));

        String catId = book.getDanhMuc().getId();
        String rootCatId = book.getDanhMuc().getDanhMucCha() != null
                ? book.getDanhMuc().getDanhMucCha().getId()
                : catId;

        List<Sach> allVisible = sachRepository.findAllVisibleWithDetails();
        List<BookDto> related = allVisible.stream()
                .filter(other -> !other.getId().equals(book.getId()))
                .filter(other -> {
                    String otherCatId = other.getDanhMuc().getId();
                    String otherRootId = other.getDanhMuc().getDanhMucCha() != null
                            ? other.getDanhMuc().getDanhMucCha().getId()
                            : otherCatId;
                    return otherCatId.equals(catId) || otherRootId.equals(rootCatId);
                })
                // Ưu tiên sách cùng danh mục trực tiếp trước, sau đó đến bán chạy nhất
                .sorted(Comparator
                        .comparing((Sach s) -> !s.getDanhMuc().getId().equals(catId))
                        .thenComparing(buildComparator("popular")))
                .limit(RELATED_BOOKS_LIMIT)
                .map(this::toBookDto)
                .toList();

        BigDecimal effectivePrice = computeEffectivePrice(book);
        int discountPercent = computeDiscountPercent(book.getGiaGoc(), effectivePrice);

        return new BookDetailDto(
                book.getId(),
                book.getMaIsbn(),
                book.getTieuDe(),
                book.getDuongDan(),
                book.getDanhMuc().getId(),
                book.getDanhMuc().getTenDanhMuc(),
                book.getTenDanhMucCon(),
                book.getTacGia().getId(),
                book.getTacGia().getTenTacGia(),
                book.getTacGia().getTieuSu(),
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
                book.getSoTrang(),
                book.getNamXuatBan(),
                book.getLoaiBia(),
                book.getAnhBiaUrl(),
                splitCommaSeparated(book.getAnhChiTietUrls()),
                book.getTomTatNgan(),
                book.getMoTaChiTiet(),
                splitCommaSeparated(book.getTuKhoa()),
                Boolean.TRUE.equals(book.getBanChay()),
                book.getNgayTao(),
                related
        );
    }

    /**
     * Lấy danh sách đánh giá công khai (biAn = false) của 1 cuốn sách đang hiển thị.
     */
    @Transactional(readOnly = true)
    public List<ReviewDto> getBookReviews(String bookId) {
        sachRepository.findVisibleByIdWithDetails(bookId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy sách."));

        return danhGiaRepository.findVisibleReviewsBySachId(bookId)
                .stream()
                .map(this::toReviewDto)
                .toList();
    }

    // --- Các hàm tiện ích lọc, sắp xếp & ánh xạ DTO ---

    private Set<String> resolveCategoryFilterIds(String categoryId) {
        if (categoryId == null) {
            return null;
        }
        Set<String> ids = new HashSet<>();
        ids.add(categoryId);
        for (DanhMuc category : danhMucRepository.findActiveCategoriesOrdered()) {
            if (category.getDanhMucCha() != null
                    && categoryId.equalsIgnoreCase(category.getDanhMucCha().getId())) {
                ids.add(category.getId());
            }
        }
        return ids;
    }

    private boolean matchesSearch(Sach book, String keyword) {
        if (keyword == null) {
            return true;
        }
        String q = keyword.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(book.getTieuDe(), q)
                || containsIgnoreCase(book.getMaIsbn(), q)
                || containsIgnoreCase(book.getTacGia().getTenTacGia(), q)
                || containsIgnoreCase(book.getNhaXuatBan().getTenNxb(), q)
                || containsIgnoreCase(book.getDanhMuc().getTenDanhMuc(), q)
                || containsIgnoreCase(book.getTenDanhMucCon(), q)
                || containsIgnoreCase(book.getTuKhoa(), q);
    }

    private boolean matchesPriceRange(Sach book, BigDecimal minPrice, BigDecimal maxPrice) {
        BigDecimal effectivePrice = computeEffectivePrice(book);
        if (minPrice != null && effectivePrice.compareTo(minPrice) < 0) {
            return false;
        }
        if (maxPrice != null && effectivePrice.compareTo(maxPrice) > 0) {
            return false;
        }
        return true;
    }

    /**
     * Tạo bộ so sánh cho 8 kiểu sắp xếp chuẩn sàn TMĐT.
     * Luôn có tie-breaker theo id tăng dần để phân trang giữa các trang (page 0, 1, 2...) không bị nhảy/trùng sách.
     */
    private Comparator<Sach> buildComparator(String sortBy) {
        Collator viCollator = Collator.getInstance(Locale.forLanguageTag("vi-VN"));
        viCollator.setStrength(Collator.PRIMARY);

        return switch (sortBy) {
            case "price_asc" -> Comparator
                    .comparing(this::computeEffectivePrice)
                    .thenComparing(s -> safeInt(s.getDaBan()), Comparator.reverseOrder())
                    .thenComparing(Sach::getId);
            case "price_desc" -> Comparator
                    .comparing(this::computeEffectivePrice, Comparator.reverseOrder())
                    .thenComparing(s -> safeInt(s.getDaBan()), Comparator.reverseOrder())
                    .thenComparing(Sach::getId);
            case "discount_desc" -> Comparator
                    .comparingInt((Sach s) -> computeDiscountPercent(s.getGiaGoc(), computeEffectivePrice(s)))
                    .reversed()
                    .thenComparing(s -> safeInt(s.getDaBan()), Comparator.reverseOrder())
                    .thenComparing(Sach::getId);
            case "rating" -> Comparator
                    .comparing((Sach s) -> safeDecimal(s.getDiemDanhGiaTb()), Comparator.reverseOrder())
                    .thenComparing(s -> safeInt(s.getSoLuotDanhGia()), Comparator.reverseOrder())
                    .thenComparing(s -> safeInt(s.getDaBan()), Comparator.reverseOrder())
                    .thenComparing(Sach::getId);
            case "newest" -> Comparator
                    .comparing((Sach s) -> safeInt(s.getNamXuatBan()), Comparator.reverseOrder())
                    .thenComparing(Sach::getNgayTao, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Sach::getId);
            case "title_asc" -> Comparator
                    .comparing(Sach::getTieuDe, viCollator)
                    .thenComparing(Sach::getId);
            case "title_desc" -> Comparator
                    .comparing(Sach::getTieuDe, viCollator.reversed())
                    .thenComparing(Sach::getId);
            default -> Comparator // popular (bán chạy nhất)
                    .comparing((Sach s) -> safeInt(s.getDaBan()), Comparator.reverseOrder())
                    .thenComparing(s -> safeDecimal(s.getDiemDanhGiaTb()), Comparator.reverseOrder())
                    .thenComparing(Sach::getId);
        };
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "popular";
        }
        String normalized = sortBy.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "popular", "price_asc", "price_desc", "discount_desc", "rating", "newest", "title_asc", "title_desc" ->
                    normalized;
            default -> throw new BusinessException(
                    "Kiểu sắp xếp '" + sortBy + "' không hợp lệ. Các giá trị hỗ trợ: "
                            + "popular, price_asc, price_desc, discount_desc, rating, newest, title_asc, title_desc."
            );
        };
    }

    /**
     * Quy tắc 2.2: ưu tiên giá Flash Sale khi sách đang bật Flash Sale và có giaFlashSale > 0.
     */
    private BigDecimal computeEffectivePrice(Sach book) {
        if (Boolean.TRUE.equals(book.getDangFlashSale())
                && book.getGiaFlashSale() != null
                && book.getGiaFlashSale().compareTo(BigDecimal.ZERO) > 0) {
            return book.getGiaFlashSale();
        }
        return safeDecimal(book.getGiaBan());
    }

    private int computeDiscountPercent(BigDecimal originalPrice, BigDecimal effectivePrice) {
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (effectivePrice.compareTo(originalPrice) >= 0) {
            return 0;
        }
        BigDecimal saved = originalPrice.subtract(effectivePrice);
        return saved.multiply(BigDecimal.valueOf(100))
                .divide(originalPrice, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private BookDto toBookDto(Sach book) {
        BigDecimal effectivePrice = computeEffectivePrice(book);
        int discountPercent = computeDiscountPercent(book.getGiaGoc(), effectivePrice);
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

    private ReviewDto toReviewDto(DanhGia review) {
        return new ReviewDto(
                review.getId(),
                review.getSach().getId(),
                review.getNguoiDung().getId(),
                review.getNguoiDung().getHoTen(),
                review.getSoSao(),
                review.getBinhLuan(),
                splitCommaSeparated(review.getAnhDanhGiaUrls()),
                review.getNgayTao()
        );
    }

    private List<String> splitCommaSeparated(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private int normalizeLimit(Integer value, int defaultValue, int maxValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return Math.min(value, maxValue);
    }

    private String normalizeBlankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeCategoryFilter(String categoryId) {
        String normalized = normalizeBlankToNull(categoryId);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private boolean containsIgnoreCase(String source, String lowerKeyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
