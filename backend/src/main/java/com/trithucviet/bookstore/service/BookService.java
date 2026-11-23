package com.trithucviet.bookstore.service;

import com.trithucviet.bookstore.dto.request.AuthorUpsertRequest;
import com.trithucviet.bookstore.dto.request.BookUpsertRequest;
import com.trithucviet.bookstore.dto.request.CategoryUpsertRequest;
import com.trithucviet.bookstore.dto.request.PublisherUpsertRequest;
import com.trithucviet.bookstore.dto.response.AdminBookDto;
import com.trithucviet.bookstore.dto.response.AdminCategoryDto;
import com.trithucviet.bookstore.dto.response.AuthorDto;
import com.trithucviet.bookstore.dto.response.BookDetailDto;
import com.trithucviet.bookstore.dto.response.BookDto;
import com.trithucviet.bookstore.dto.response.BookPageResponse;
import com.trithucviet.bookstore.dto.response.CategoryDto;
import com.trithucviet.bookstore.dto.response.HomeBooksResponse;
import com.trithucviet.bookstore.dto.response.PublisherDto;
import com.trithucviet.bookstore.dto.response.ReviewDto;
import com.trithucviet.bookstore.entity.DanhGia;
import com.trithucviet.bookstore.entity.DanhMuc;
import com.trithucviet.bookstore.entity.NhaXuatBan;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.entity.TacGia;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import com.trithucviet.bookstore.exception.BusinessException;
import com.trithucviet.bookstore.repository.ChiTietDonHangRepository;
import com.trithucviet.bookstore.repository.DanhGiaRepository;
import com.trithucviet.bookstore.repository.DanhMucRepository;
import com.trithucviet.bookstore.repository.NhaXuatBanRepository;
import com.trithucviet.bookstore.repository.SachRepository;
import com.trithucviet.bookstore.repository.TacGiaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ Danh mục, Trang chủ, Tìm kiếm, Lọc, Sắp xếp, Chi tiết Sách (Bước 3 - phần 1 & 2)
 * và Quản trị Sách, Danh mục, Tác giả, NXB trên Web Admin (Bước 3 - phần 3 & 4).
 *
 * Lưu ý kỹ thuật:
 * - Giá áp dụng thực tế (donGiaApDung) luôn tuân thủ Quy tắc 2.2: ưu tiên giaFlashSale nếu dangFlashSale = true và giaFlashSale > 0.
 * - Khi lọc theo danh mục cha (ví dụ dm_van_hoc), tự động gom cả sách nằm trong các danh mục con (dm_trinh_tham, dm_vien_tuong).
 * - Hỗ trợ đầy đủ 8 kiểu sắp xếp chuẩn sàn TMĐT: popular, price_asc, price_desc, discount_desc, rating, newest, title_asc, title_desc.
 * - Quản lý Kho (WAREHOUSE_MANAGER) được phép thêm/sửa thông tin sách, tồn kho, danh mục, tác giả, NXB
 *   nhưng bị chặn chỉnh giá bán, Flash Sale và xóa sách.
 */
@Service
public class BookService {

    private static final int DEFAULT_HOME_LIMIT = 12;
    private static final int MAX_HOME_LIMIT = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int RELATED_BOOKS_LIMIT = 6;
    private static final String DEFAULT_COVER_URL =
            "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=600&q=80";

    private final DanhMucRepository danhMucRepository;
    private final SachRepository sachRepository;
    private final DanhGiaRepository danhGiaRepository;
    private final TacGiaRepository tacGiaRepository;
    private final NhaXuatBanRepository nhaXuatBanRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;

    public BookService(
            DanhMucRepository danhMucRepository,
            SachRepository sachRepository,
            DanhGiaRepository danhGiaRepository,
            TacGiaRepository tacGiaRepository,
            NhaXuatBanRepository nhaXuatBanRepository,
            ChiTietDonHangRepository chiTietDonHangRepository
    ) {
        this.danhMucRepository = danhMucRepository;
        this.sachRepository = sachRepository;
        this.danhGiaRepository = danhGiaRepository;
        this.tacGiaRepository = tacGiaRepository;
        this.nhaXuatBanRepository = nhaXuatBanRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
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

    // =========================================================================
    // NGHIỆP VỤ QUẢN TRỊ SÁCH, DANH MỤC, TÁC GIẢ & NXB (BƯỚC 3 - PHẦN 3 & 4)
    // =========================================================================

    /**
     * Lấy toàn bộ danh sách sách cho Web Admin (gồm cả sách đang ẩn hienThi = false),
     * hỗ trợ lọc theo từ khóa (tên sách, tác giả, ISBN), danh mục (gồm danh mục con) và trạng thái hiển thị.
     */
    @Transactional(readOnly = true)
    public List<AdminBookDto> getAdminBooks(String keyword, String danhMucId, Boolean hienThi) {
        String normalizedKeyword = normalizeBlankToNull(keyword);
        String lowerKeyword = normalizedKeyword == null ? null : normalizedKeyword.toLowerCase(Locale.ROOT);
        String normalizedCategory = normalizeCategoryFilter(danhMucId);
        Set<String> targetCategoryIds = normalizedCategory == null
                ? null
                : collectCategoryAndDescendants(danhMucRepository.findAllOrdered(), normalizedCategory);

        return sachRepository.findAllWithDetails().stream()
                .filter(book -> hienThi == null || hienThi.equals(Boolean.TRUE.equals(book.getHienThi())))
                .filter(book -> targetCategoryIds == null || targetCategoryIds.contains(book.getDanhMuc().getId()))
                .filter(book -> {
                    if (lowerKeyword == null) {
                        return true;
                    }
                    return containsIgnoreCase(book.getTieuDe(), lowerKeyword)
                            || containsIgnoreCase(book.getMaIsbn(), lowerKeyword)
                            || containsIgnoreCase(book.getTacGia().getTenTacGia(), lowerKeyword)
                            || containsIgnoreCase(book.getNhaXuatBan().getTenNxb(), lowerKeyword)
                            || containsIgnoreCase(book.getDanhMuc().getTenDanhMuc(), lowerKeyword);
                })
                .map(this::toAdminBookDto)
                .toList();
    }

    /**
     * Xem chi tiết 1 cuốn sách phía Web Admin (không lọc hienThi = true).
     */
    @Transactional(readOnly = true)
    public AdminBookDto getAdminBookById(String bookId) {
        Sach book = sachRepository.findByIdWithDetails(bookId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy cuốn sách yêu cầu."
                ));
        return toAdminBookDto(book);
    }

    /**
     * Thêm mới 1 cuốn sách vào hệ thống (ADMIN hoặc WAREHOUSE_MANAGER).
     * - Quản lý Kho không được tự ý bật cấu hình Flash Sale khi tạo sách.
     * - Tự sinh mã ISBN / đường dẫn slug duy nhất nếu quản trị viên để trống.
     * - Cho phép chọn Tác giả / NXB có sẵn theo ID hoặc tự tạo nhanh theo tên nếu chưa có.
     */
    @Transactional
    public AdminBookDto createBook(BookUpsertRequest request, VaiTro actorRole) {
        if (actorRole == VaiTro.WAREHOUSE_MANAGER && Boolean.TRUE.equals(request.dangFlashSale())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Quản lý Kho không có quyền cấu hình Flash Sale."
            );
        }

        BigDecimal giaBan = request.giaBan();
        if (giaBan == null || giaBan.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Giá bán thực tế của sách phải lớn hơn 0."
            );
        }
        BigDecimal giaGoc = request.giaGoc() != null ? request.giaGoc() : giaBan;
        validateBookPricing(giaBan, giaGoc, Boolean.TRUE.equals(request.dangFlashSale()), request.giaFlashSale());

        int tonKho = request.tonKho() == null ? 0 : request.tonKho();
        if (tonKho < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Số lượng tồn kho không được âm.");
        }

        DanhMuc danhMuc = findCategoryOrThrow(request.danhMucId());
        TacGia tacGia = resolveAuthor(request.tacGiaId(), request.tenTacGia());
        NhaXuatBan nhaXuatBan = resolvePublisher(request.nhaXuatBanId(), request.tenNhaXuatBan());

        String maIsbn = resolveUniqueIsbnForCreate(request.maIsbn());
        String duongDan = resolveUniqueBookSlug(request.tieuDe(), null);

        Sach book = new Sach();
        book.setId("sach_" + shortUuid());
        book.setMaIsbn(maIsbn);
        book.setTieuDe(request.tieuDe().trim());
        book.setDuongDan(duongDan);
        book.setDanhMuc(danhMuc);
        book.setTenDanhMucCon(normalizeBlankToNull(request.tenDanhMucCon()));
        book.setTacGia(tacGia);
        book.setNhaXuatBan(nhaXuatBan);
        book.setGiaBan(giaBan);
        book.setGiaGoc(giaGoc);

        boolean isFlashSale = actorRole == VaiTro.ADMIN && Boolean.TRUE.equals(request.dangFlashSale());
        book.setDangFlashSale(isFlashSale);
        book.setGiaFlashSale(isFlashSale ? request.giaFlashSale() : null);

        book.setTonKho(tonKho);
        book.setDaBan(0);
        book.setDiemDanhGiaTb(new BigDecimal("5.00"));
        book.setSoLuotDanhGia(0);
        book.setSoTrang(request.soTrang());
        book.setNamXuatBan(request.namXuatBan());
        book.setLoaiBia(normalizeBlankToNull(request.loaiBia()) != null ? request.loaiBia().trim() : "Bìa mềm");
        book.setAnhBiaUrl(normalizeBlankToNull(request.anhBiaUrl()) != null ? request.anhBiaUrl().trim() : DEFAULT_COVER_URL);
        book.setTomTatNgan(normalizeBlankToNull(request.tomTatNgan()));
        book.setMoTaChiTiet(normalizeBlankToNull(request.moTaChiTiet()));
        book.setTuKhoa(normalizeBlankToNull(request.tuKhoa()));
        book.setBanChay(actorRole == VaiTro.ADMIN && Boolean.TRUE.equals(request.banChay()));
        book.setHienThi(actorRole == VaiTro.ADMIN ? (request.hienThi() == null || request.hienThi()) : Boolean.TRUE);
        book.setNgayTao(LocalDateTime.now());

        sachRepository.saveAndFlush(book);
        return getAdminBookById(book.getId());
    }

    /**
     * Cập nhật thông tin đầu sách:
     * - ADMIN: sửa được toàn bộ thông tin, giá bán, giá gốc, Flash Sale, hiển thị.
     * - WAREHOUSE_MANAGER: chỉ được sửa thông tin sách và tồn kho; bị chặn 403 nếu cố tình đổi giá bán/giá gốc/Flash Sale.
     */
    @Transactional
    public AdminBookDto updateBook(String bookId, BookUpsertRequest request, VaiTro actorRole) {
        Sach existing = sachRepository.findByIdWithDetails(bookId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy cuốn sách cần cập nhật."
                ));

        if (actorRole == VaiTro.WAREHOUSE_MANAGER && isPriceOrFlashSaleModified(request, existing)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Quản lý Kho không có quyền chỉnh sửa giá bán hoặc cấu hình Flash Sale."
            );
        }

        BigDecimal nextGiaBan = request.giaBan() != null ? request.giaBan() : existing.getGiaBan();
        BigDecimal nextGiaGoc = request.giaGoc() != null ? request.giaGoc() : existing.getGiaGoc();
        boolean nextFlashSale = request.dangFlashSale() != null
                ? request.dangFlashSale()
                : Boolean.TRUE.equals(existing.getDangFlashSale());
        BigDecimal nextFlashPrice = nextFlashSale
                ? (request.giaFlashSale() != null ? request.giaFlashSale() : existing.getGiaFlashSale())
                : null;

        if (actorRole == VaiTro.ADMIN) {
            validateBookPricing(nextGiaBan, nextGiaGoc, nextFlashSale, nextFlashPrice);
        }

        if (request.tonKho() != null && request.tonKho() < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Số lượng tồn kho không được âm.");
        }

        String requestedIsbn = normalizeBlankToNull(request.maIsbn());
        if (requestedIsbn != null) {
            if (sachRepository.existsByMaIsbnIgnoreCaseAndIdNot(requestedIsbn, existing.getId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Mã ISBN này đã tồn tại trên hệ thống.");
            }
            existing.setMaIsbn(requestedIsbn);
        }

        String nextTitle = request.tieuDe().trim();
        if (!nextTitle.equals(existing.getTieuDe())) {
            existing.setDuongDan(resolveUniqueBookSlug(nextTitle, existing.getId()));
        }
        existing.setTieuDe(nextTitle);
        existing.setDanhMuc(findCategoryOrThrow(request.danhMucId()));
        existing.setTenDanhMucCon(normalizeBlankToNull(request.tenDanhMucCon()));

        if (normalizeBlankToNull(request.tacGiaId()) != null || normalizeBlankToNull(request.tenTacGia()) != null) {
            existing.setTacGia(resolveAuthor(request.tacGiaId(), request.tenTacGia()));
        }
        if (normalizeBlankToNull(request.nhaXuatBanId()) != null || normalizeBlankToNull(request.tenNhaXuatBan()) != null) {
            existing.setNhaXuatBan(resolvePublisher(request.nhaXuatBanId(), request.tenNhaXuatBan()));
        }

        if (actorRole == VaiTro.ADMIN) {
            existing.setGiaBan(nextGiaBan);
            existing.setGiaGoc(nextGiaGoc);
            existing.setDangFlashSale(nextFlashSale);
            existing.setGiaFlashSale(nextFlashPrice);
            if (request.banChay() != null) {
                existing.setBanChay(request.banChay());
            }
            if (request.hienThi() != null) {
                existing.setHienThi(request.hienThi());
            }
        }

        if (request.tonKho() != null) {
            existing.setTonKho(request.tonKho());
        }
        if (request.soTrang() != null) {
            existing.setSoTrang(request.soTrang());
        }
        if (request.namXuatBan() != null) {
            existing.setNamXuatBan(request.namXuatBan());
        }
        if (normalizeBlankToNull(request.loaiBia()) != null) {
            existing.setLoaiBia(request.loaiBia().trim());
        }
        if (normalizeBlankToNull(request.anhBiaUrl()) != null) {
            existing.setAnhBiaUrl(request.anhBiaUrl().trim());
        }
        existing.setTomTatNgan(normalizeBlankToNull(request.tomTatNgan()));
        existing.setMoTaChiTiet(normalizeBlankToNull(request.moTaChiTiet()));
        existing.setTuKhoa(normalizeBlankToNull(request.tuKhoa()));

        sachRepository.saveAndFlush(existing);
        return getAdminBookById(existing.getId());
    }

    /**
     * Bật / tắt nhanh trạng thái hiển thị sách trên Mobile App (chỉ ADMIN).
     */
    @Transactional
    public AdminBookDto toggleBookVisibility(String bookId) {
        Sach book = sachRepository.findByIdWithDetails(bookId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy cuốn sách cần thay đổi trạng thái."
                ));
        book.setHienThi(!Boolean.TRUE.equals(book.getHienThi()));
        sachRepository.saveAndFlush(book);
        return toAdminBookDto(book);
    }

    /**
     * Xóa sách khỏi hệ thống (chỉ ADMIN):
     * - Bắt buộc chặn nếu sách đã từng nằm trong chi_tiet_don_hang để bảo toàn dữ liệu hóa đơn/lịch sử mua.
     */
    @Transactional
    public void deleteBook(String bookId) {
        Sach book = sachRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy cuốn sách cần xóa."
                ));

        if (chiTietDonHangRepository.existsBySachId(bookId)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể xóa sách đã phát sinh đơn hàng. Vui lòng chuyển sang trạng thái Ẩn hiển thị."
            );
        }

        sachRepository.delete(book);
        sachRepository.flush();
    }

    /**
     * Lấy danh sách danh mục phẳng cho Web Admin (kèm danh mục tạm ẩn và tổng số sách thuộc từng danh mục/cây con).
     */
    @Transactional(readOnly = true)
    public List<AdminCategoryDto> getAdminCategories() {
        List<DanhMuc> allCategories = danhMucRepository.findAllOrdered();
        List<Sach> allBooks = sachRepository.findAllWithDetails();

        Map<String, Long> directCounts = new HashMap<>();
        for (Sach book : allBooks) {
            directCounts.merge(book.getDanhMuc().getId(), 1L, Long::sum);
        }

        Map<String, Long> childSumByParent = new HashMap<>();
        for (DanhMuc cat : allCategories) {
            if (cat.getDanhMucCha() != null) {
                long count = directCounts.getOrDefault(cat.getId(), 0L);
                childSumByParent.merge(cat.getDanhMucCha().getId(), count, Long::sum);
            }
        }

        return allCategories.stream()
                .map(cat -> {
                    String parentId = cat.getDanhMucCha() != null ? cat.getDanhMucCha().getId() : null;
                    long totalBooks = directCounts.getOrDefault(cat.getId(), 0L)
                            + childSumByParent.getOrDefault(cat.getId(), 0L);
                    return new AdminCategoryDto(
                            cat.getId(),
                            cat.getTenDanhMuc(),
                            cat.getDuongDan(),
                            parentId,
                            cat.getMoTa(),
                            cat.getTenBieuTuong(),
                            cat.getThuTuHienThi(),
                            Boolean.TRUE.equals(cat.getDangHoatDong()),
                            totalBooks
                    );
                })
                .toList();
    }

    @Transactional
    public AdminCategoryDto createCategory(CategoryUpsertRequest request) {
        DanhMuc parent = resolveParentCategory(request.danhMucChaId(), null);

        DanhMuc category = new DanhMuc();
        category.setId("dm_" + shortUuid());
        category.setTenDanhMuc(request.tenDanhMuc().trim());
        category.setDuongDan(resolveUniqueCategorySlug(request.tenDanhMuc(), null));
        category.setDanhMucCha(parent);
        category.setMoTa(normalizeBlankToNull(request.moTa()));
        category.setTenBieuTuong(normalizeBlankToNull(request.tenBieuTuong()) != null
                ? request.tenBieuTuong().trim()
                : "BookOpen");
        category.setThuTuHienThi(request.thuTuHienThi() != null && request.thuTuHienThi() > 0
                ? request.thuTuHienThi()
                : (int) danhMucRepository.count() + 1);
        category.setDangHoatDong(request.dangHoatDong() == null || request.dangHoatDong());

        danhMucRepository.saveAndFlush(category);
        return toAdminCategoryDto(category, 0L);
    }

    @Transactional
    public AdminCategoryDto updateCategory(String categoryId, CategoryUpsertRequest request) {
        DanhMuc existing = findCategoryOrThrow(categoryId);
        DanhMuc parent = resolveParentCategory(request.danhMucChaId(), existing.getId());

        String nextName = request.tenDanhMuc().trim();
        if (!nextName.equals(existing.getTenDanhMuc())) {
            existing.setDuongDan(resolveUniqueCategorySlug(nextName, existing.getId()));
        }
        existing.setTenDanhMuc(nextName);
        existing.setDanhMucCha(parent);
        existing.setMoTa(normalizeBlankToNull(request.moTa()));
        if (normalizeBlankToNull(request.tenBieuTuong()) != null) {
            existing.setTenBieuTuong(request.tenBieuTuong().trim());
        }
        if (request.thuTuHienThi() != null && request.thuTuHienThi() > 0) {
            existing.setThuTuHienThi(request.thuTuHienThi());
        }
        if (request.dangHoatDong() != null) {
            existing.setDangHoatDong(request.dangHoatDong());
        }

        danhMucRepository.saveAndFlush(existing);
        return getAdminCategories().stream()
                .filter(item -> item.id().equals(existing.getId()))
                .findFirst()
                .orElse(toAdminCategoryDto(existing, 0L));
    }

    @Transactional
    public void deleteCategory(String categoryId) {
        DanhMuc existing = findCategoryOrThrow(categoryId);
        if (danhMucRepository.existsByDanhMucChaId(categoryId)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể xóa danh mục đang chứa danh mục con."
            );
        }
        if (sachRepository.existsByDanhMucId(categoryId)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể xóa danh mục đang có sách liên kết."
            );
        }
        danhMucRepository.delete(existing);
        danhMucRepository.flush();
    }

    @Transactional(readOnly = true)
    public List<AuthorDto> getAdminAuthors() {
        Map<String, Long> bookCounts = new HashMap<>();
        for (Sach book : sachRepository.findAllWithDetails()) {
            bookCounts.merge(book.getTacGia().getId(), 1L, Long::sum);
        }
        return tacGiaRepository.findAllOrdered().stream()
                .map(author -> new AuthorDto(
                        author.getId(),
                        author.getTenTacGia(),
                        author.getTieuSu(),
                        bookCounts.getOrDefault(author.getId(), 0L)
                ))
                .toList();
    }

    @Transactional
    public AuthorDto createAuthor(AuthorUpsertRequest request) {
        TacGia author = new TacGia();
        author.setId("tg_" + shortUuid());
        author.setTenTacGia(request.tenTacGia().trim());
        author.setTieuSu(normalizeBlankToNull(request.tieuSu()));
        tacGiaRepository.saveAndFlush(author);
        return new AuthorDto(author.getId(), author.getTenTacGia(), author.getTieuSu(), 0L);
    }

    @Transactional
    public AuthorDto updateAuthor(String authorId, AuthorUpsertRequest request) {
        TacGia author = tacGiaRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy tác giả."));
        author.setTenTacGia(request.tenTacGia().trim());
        author.setTieuSu(normalizeBlankToNull(request.tieuSu()));
        tacGiaRepository.saveAndFlush(author);
        long bookCount = sachRepository.findAllWithDetails().stream()
                .filter(b -> b.getTacGia().getId().equals(author.getId()))
                .count();
        return new AuthorDto(author.getId(), author.getTenTacGia(), author.getTieuSu(), bookCount);
    }

    @Transactional
    public void deleteAuthor(String authorId) {
        TacGia author = tacGiaRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy tác giả."));
        if (sachRepository.existsByTacGiaId(authorId)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể xóa tác giả đang có sách liên kết."
            );
        }
        tacGiaRepository.delete(author);
        tacGiaRepository.flush();
    }

    @Transactional(readOnly = true)
    public List<PublisherDto> getAdminPublishers() {
        Map<String, Long> bookCounts = new HashMap<>();
        for (Sach book : sachRepository.findAllWithDetails()) {
            bookCounts.merge(book.getNhaXuatBan().getId(), 1L, Long::sum);
        }
        return nhaXuatBanRepository.findAllOrdered().stream()
                .map(pub -> new PublisherDto(
                        pub.getId(),
                        pub.getTenNxb(),
                        pub.getDiaChi(),
                        bookCounts.getOrDefault(pub.getId(), 0L)
                ))
                .toList();
    }

    @Transactional
    public PublisherDto createPublisher(PublisherUpsertRequest request) {
        NhaXuatBan publisher = new NhaXuatBan();
        publisher.setId("nxb_" + shortUuid());
        publisher.setTenNxb(request.tenNxb().trim());
        publisher.setDiaChi(normalizeBlankToNull(request.diaChi()));
        nhaXuatBanRepository.saveAndFlush(publisher);
        return new PublisherDto(publisher.getId(), publisher.getTenNxb(), publisher.getDiaChi(), 0L);
    }

    @Transactional
    public PublisherDto updatePublisher(String publisherId, PublisherUpsertRequest request) {
        NhaXuatBan publisher = nhaXuatBanRepository.findById(publisherId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhà xuất bản."));
        publisher.setTenNxb(request.tenNxb().trim());
        publisher.setDiaChi(normalizeBlankToNull(request.diaChi()));
        nhaXuatBanRepository.saveAndFlush(publisher);
        long bookCount = sachRepository.findAllWithDetails().stream()
                .filter(b -> b.getNhaXuatBan().getId().equals(publisher.getId()))
                .count();
        return new PublisherDto(publisher.getId(), publisher.getTenNxb(), publisher.getDiaChi(), bookCount);
    }

    @Transactional
    public void deletePublisher(String publisherId) {
        NhaXuatBan publisher = nhaXuatBanRepository.findById(publisherId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhà xuất bản."));
        if (sachRepository.existsByNhaXuatBanId(publisherId)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể xóa nhà xuất bản đang có sách liên kết."
            );
        }
        nhaXuatBanRepository.delete(publisher);
        nhaXuatBanRepository.flush();
    }

    // --- Các hàm tiện ích nội bộ cho phân hệ Quản trị ---

    private Set<String> collectCategoryAndDescendants(List<DanhMuc> allCategories, String rootCategoryId) {
        Set<String> ids = new HashSet<>();
        ids.add(rootCategoryId);
        boolean added;
        do {
            added = false;
            for (DanhMuc category : allCategories) {
                if (category.getDanhMucCha() != null
                        && ids.contains(category.getDanhMucCha().getId())
                        && ids.add(category.getId())) {
                    added = true;
                }
            }
        } while (added);
        return ids;
    }

    private boolean isPriceOrFlashSaleModified(BookUpsertRequest request, Sach existing) {
        boolean giaBanChanged = request.giaBan() != null && request.giaBan().compareTo(existing.getGiaBan()) != 0;
        boolean giaGocChanged = request.giaGoc() != null && request.giaGoc().compareTo(existing.getGiaGoc()) != 0;
        boolean currentFlashSale = Boolean.TRUE.equals(existing.getDangFlashSale());
        boolean flashFlagChanged = request.dangFlashSale() != null && request.dangFlashSale() != currentFlashSale;
        boolean flashPriceChanged = request.giaFlashSale() != null
                && !sameDecimal(request.giaFlashSale(), existing.getGiaFlashSale());
        return giaBanChanged || giaGocChanged || flashFlagChanged || flashPriceChanged;
    }

    private boolean sameDecimal(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }

    private void validateBookPricing(
            BigDecimal giaBan,
            BigDecimal giaGoc,
            boolean dangFlashSale,
            BigDecimal giaFlashSale
    ) {
        if (giaBan == null || giaBan.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Giá bán sách phải lớn hơn 0.");
        }
        if (giaGoc == null || giaGoc.compareTo(giaBan) < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Giá gốc phải lớn hơn hoặc bằng giá bán.");
        }
        if (dangFlashSale) {
            if (giaFlashSale == null || giaFlashSale.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Vui lòng nhập giá Flash Sale hợp lệ (> 0).");
            }
            if (giaFlashSale.compareTo(giaBan) >= 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Giá Flash Sale phải nhỏ hơn giá bán thông thường.");
            }
        }
    }

    private DanhMuc findCategoryOrThrow(String categoryId) {
        return danhMucRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục sách."));
    }

    private DanhMuc resolveParentCategory(String parentId, String selfId) {
        String normalized = normalizeBlankToNull(parentId);
        if (normalized == null) {
            return null;
        }
        if (selfId != null && selfId.equals(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Một danh mục không thể chọn chính nó làm danh mục cha.");
        }
        return findCategoryOrThrow(normalized);
    }

    private TacGia resolveAuthor(String tacGiaId, String tenTacGia) {
        String normalizedId = normalizeBlankToNull(tacGiaId);
        if (normalizedId != null) {
            return tacGiaRepository.findById(normalizedId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy tác giả đã chọn."));
        }
        String normalizedName = normalizeBlankToNull(tenTacGia);
        if (normalizedName == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Vui lòng chọn hoặc nhập tên tác giả.");
        }
        return tacGiaRepository.findFirstByTenTacGiaIgnoreCase(normalizedName)
                .orElseGet(() -> {
                    TacGia created = new TacGia();
                    created.setId("tg_" + shortUuid());
                    created.setTenTacGia(normalizedName);
                    created.setTieuSu("Tác giả được tạo khi thêm đầu sách mới");
                    return tacGiaRepository.saveAndFlush(created);
                });
    }

    private NhaXuatBan resolvePublisher(String nhaXuatBanId, String tenNhaXuatBan) {
        String normalizedId = normalizeBlankToNull(nhaXuatBanId);
        if (normalizedId != null) {
            return nhaXuatBanRepository.findById(normalizedId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhà xuất bản đã chọn."));
        }
        String normalizedName = normalizeBlankToNull(tenNhaXuatBan);
        if (normalizedName == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Vui lòng chọn hoặc nhập tên nhà xuất bản.");
        }
        return nhaXuatBanRepository.findFirstByTenNxbIgnoreCase(normalizedName)
                .orElseGet(() -> {
                    NhaXuatBan created = new NhaXuatBan();
                    created.setId("nxb_" + shortUuid());
                    created.setTenNxb(normalizedName);
                    created.setDiaChi("Việt Nam");
                    return nhaXuatBanRepository.saveAndFlush(created);
                });
    }

    private String resolveUniqueIsbnForCreate(String requestedIsbn) {
        String normalized = normalizeBlankToNull(requestedIsbn);
        if (normalized != null) {
            if (sachRepository.existsByMaIsbnIgnoreCase(normalized)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Mã ISBN này đã tồn tại trên hệ thống.");
            }
            return normalized;
        }
        String candidate;
        do {
            candidate = "978-604-" + (100000 + Math.abs(UUID.randomUUID().hashCode()) % 900000);
        } while (sachRepository.existsByMaIsbnIgnoreCase(candidate));
        return candidate;
    }

    private String resolveUniqueBookSlug(String title, String excludeId) {
        String baseSlug = toSlug(title);
        String candidate = baseSlug;
        int suffix = 2;
        while (excludeId == null
                ? sachRepository.existsByDuongDanIgnoreCase(candidate)
                : sachRepository.existsByDuongDanIgnoreCaseAndIdNot(candidate, excludeId)) {
            candidate = baseSlug + "-" + suffix++;
        }
        return candidate;
    }

    private String resolveUniqueCategorySlug(String name, String excludeId) {
        String baseSlug = toSlug(name);
        String candidate = baseSlug;
        int suffix = 2;
        while (excludeId == null
                ? danhMucRepository.existsByDuongDanIgnoreCase(candidate)
                : danhMucRepository.existsByDuongDanIgnoreCaseAndIdNot(candidate, excludeId)) {
            candidate = baseSlug + "-" + suffix++;
        }
        return candidate;
    }

    private String toSlug(String raw) {
        String normalized = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isEmpty() ? "item-" + shortUuid() : normalized;
    }

    private String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private AdminBookDto toAdminBookDto(Sach book) {
        BigDecimal effectivePrice = computeEffectivePrice(book);
        int discountPercent = computeDiscountPercent(book.getGiaGoc(), effectivePrice);
        return new AdminBookDto(
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
                book.getSoTrang(),
                book.getNamXuatBan(),
                book.getLoaiBia(),
                book.getAnhBiaUrl(),
                book.getTomTatNgan(),
                book.getMoTaChiTiet(),
                book.getTuKhoa(),
                Boolean.TRUE.equals(book.getBanChay()),
                Boolean.TRUE.equals(book.getHienThi()),
                book.getNgayTao()
        );
    }

    private AdminCategoryDto toAdminCategoryDto(DanhMuc cat, long bookCount) {
        return new AdminCategoryDto(
                cat.getId(),
                cat.getTenDanhMuc(),
                cat.getDuongDan(),
                cat.getDanhMucCha() != null ? cat.getDanhMucCha().getId() : null,
                cat.getMoTa(),
                cat.getTenBieuTuong(),
                cat.getThuTuHienThi(),
                Boolean.TRUE.equals(cat.getDangHoatDong()),
                bookCount
        );
    }
}
