import { db } from '@shared/index';
import { Book, Category, Review } from '@shared/types';
import { apiRequest } from '@/services/httpClient';

export type UserTrackingAction =
  | 'XEM_CHI_TIET'
  | 'TIM_KIEM'
  | 'THEM_YEU_THICH'
  | 'THEM_GIO_HANG'
  | 'MUA_HANG';

export type CatalogSortBy =
  | 'popular'
  | 'price_asc'
  | 'price_desc'
  | 'discount_desc'
  | 'rating'
  | 'newest'
  | 'title_asc'
  | 'title_desc';

export interface CategoryTreeItem extends Category {
  children: Category[];
}

export interface BookPageResult {
  items: Book[];
  // Số trang 1-based dùng trực tiếp trên giao diện (Trang 1, Trang 2, ...)
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  appliedSort: CatalogSortBy;
}

export interface HomeBooksResult {
  flashSaleBooks: Book[];
  bestSellerBooks: Book[];
  totalFlashSale: number;
  totalBestSeller: number;
  totalVisibleBooks: number;
}

// --- Khuôn DTO gốc trả về từ Backend Spring Boot (Bước 3 - Phần 1 & 2) ---

interface BackendCategoryDto {
  id: string;
  tenDanhMuc: string;
  duongDan: string;
  danhMucChaId: string | null;
  moTa: string | null;
  tenBieuTuong: string | null;
  thuTuHienThi: number | null;
  soLuongSach: number;
  danhMucCon: BackendCategoryDto[];
}

interface BackendBookDto {
  id: string;
  maIsbn: string;
  tieuDe: string;
  duongDan: string;
  danhMucId: string;
  tenDanhMuc: string;
  tenDanhMucCon: string | null;
  tacGiaId: string;
  tenTacGia: string;
  nhaXuatBanId: string;
  tenNhaXuatBan: string;
  giaBan: number | string;
  giaGoc: number | string;
  dangFlashSale: boolean;
  giaFlashSale: number | string | null;
  donGiaApDung: number | string;
  phanTramGiam: number;
  tonKho: number;
  daBan: number;
  diemDanhGiaTb: number | string;
  soLuotDanhGia: number;
  loaiBia: string | null;
  namXuatBan: number | null;
  anhBiaUrl: string;
  tomTatNgan: string | null;
  banChay: boolean;
}

interface BackendBookDetailDto extends BackendBookDto {
  tieuSuTacGia: string | null;
  soTrang: number | null;
  anhChiTietUrls: string[];
  moTaChiTiet: string | null;
  tuKhoa: string[];
  ngayTao: string | null;
  sachCungChuDe: BackendBookDto[];
}

interface BackendHomeBooksResponse {
  flashSaleBooks: BackendBookDto[];
  bestSellerBooks: BackendBookDto[];
  tongSoSachFlashSale: number;
  tongSoSachBanChay: number;
  tongSoSachHienThi: number;
}

interface BackendBookPageResponse {
  items: BackendBookDto[];
  // Backend Spring trả page 0-based -> frontend quy đổi +1 thành 1-based
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  appliedSort: CatalogSortBy;
}

interface BackendReviewDto {
  id: string;
  sachId: string;
  nguoiDungId: string;
  tenNguoiDung: string;
  soSao: number;
  binhLuan: string;
  anhDanhGiaUrls: string[];
  ngayTao: string | null;
}

// --- Hàm ánh xạ dữ liệu tiếng Việt từ Backend sang model dùng chung của Frontend ---

function mapCategory(dto: BackendCategoryDto): CategoryTreeItem {
  return {
    id: dto.id,
    name: dto.tenDanhMuc,
    slug: dto.duongDan,
    parentId: dto.danhMucChaId,
    description: dto.moTa || '',
    iconName: dto.tenBieuTuong || 'book-outline',
    bookCount: Number(dto.soLuongSach || 0),
    isActive: true,
    displayOrder: Number(dto.thuTuHienThi || 1),
    children: (dto.danhMucCon || []).map(mapCategory),
  };
}

function mapBook(dto: BackendBookDto): Book {
  const isFlash = Boolean(dto.dangFlashSale) && dto.giaFlashSale != null && Number(dto.giaFlashSale) > 0;
  return {
    id: dto.id,
    isbn: dto.maIsbn,
    title: dto.tieuDe,
    slug: dto.duongDan,
    authorId: dto.tacGiaId,
    authorName: dto.tenTacGia,
    publisherId: dto.nhaXuatBanId,
    publisherName: dto.tenNhaXuatBan,
    categoryId: dto.danhMucId,
    categoryName: dto.tenDanhMuc,
    SubCategoryName: dto.tenDanhMucCon || undefined,
    price: Number(dto.giaBan || 0),
    originalPrice: Number(dto.giaGoc || 0),
    stock: Number(dto.tonKho || 0),
    soldCount: Number(dto.daBan || 0),
    ratingAvg: Number(dto.diemDanhGiaTb || 0),
    ratingCount: Number(dto.soLuotDanhGia || 0),
    pageCount: 0,
    publishYear: Number(dto.namXuatBan || 0),
    coverFormat: dto.loaiBia === 'Bìa cứng' ? 'Bìa cứng' : 'Bìa mềm',
    coverUrl: dto.anhBiaUrl,
    galleryUrls: dto.anhBiaUrl ? [dto.anhBiaUrl] : [],
    shortSummary: dto.tomTatNgan || '',
    description: dto.tomTatNgan || '',
    isFlashSale: isFlash,
    flashSalePrice: isFlash ? Number(dto.giaFlashSale) : undefined,
    isBestSeller: Boolean(dto.banChay),
    isVisible: true,
    createdAt: new Date().toISOString(),
  };
}

function mapBookDetail(dto: BackendBookDetailDto): Book {
  const base = mapBook(dto);
  return {
    ...base,
    pageCount: Number(dto.soTrang || 0),
    galleryUrls: dto.anhChiTietUrls?.length ? dto.anhChiTietUrls : base.galleryUrls,
    description: dto.moTaChiTiet || dto.tomTatNgan || '',
    createdAt: dto.ngayTao || base.createdAt,
  };
}

function mapReview(dto: BackendReviewDto, bookTitle = ''): Review {
  return {
    id: dto.id,
    bookId: dto.sachId,
    bookTitle,
    userId: dto.nguoiDungId,
    userName: dto.tenNguoiDung,
    orderId: '',
    rating: Number(dto.soSao || 5),
    comment: dto.binhLuan,
    imageUrls: dto.anhDanhGiaUrls || [],
    createdAt: dto.ngayTao || new Date().toISOString(),
    isHidden: false,
  };
}

// --- Phần User Tracking cục bộ tạm thời (sẽ thay bằng API Bước 8 ở giai đoạn sau) ---

interface UserBehaviorEvent {
  id: string;
  bookId: string;
  categoryId: string;
  categoryName: string;
  action: UserTrackingAction;
  dwellSeconds: number;
  weightScore: number;
  timestamp: number;
}

const behaviorLogs: UserBehaviorEvent[] = [
  {
    id: 'hv-seed-1',
    bookId: 'sach_113',
    categoryId: 'dm_cong_nghe',
    categoryName: 'Công nghệ & Lập trình',
    action: 'XEM_CHI_TIET',
    dwellSeconds: 28,
    weightScore: 5,
    timestamp: Date.now() - 1000 * 60 * 15,
  },
  {
    id: 'hv-seed-2',
    bookId: 'sach_045',
    categoryId: 'dm_kinh_te',
    categoryName: 'Kinh tế & Quản trị',
    action: 'THEM_YEU_THICH',
    dwellSeconds: 10,
    weightScore: 4,
    timestamp: Date.now() - 1000 * 60 * 45,
  },
];

const ignoredImpressionsByCategory = new Map<string, number>();

function computeWeight(action: UserTrackingAction, dwellSeconds = 0): number {
  switch (action) {
    case 'MUA_HANG':
      return 10;
    case 'THEM_GIO_HANG':
      return 6;
    case 'THEM_YEU_THICH':
      return 4;
    case 'TIM_KIEM':
      return 2;
    case 'XEM_CHI_TIET':
    default:
      if (dwellSeconds >= 15) return 5;
      if (dwellSeconds >= 5) return 3;
      return 1;
  }
}

function computeTimeDecayFactor(action: UserTrackingAction, timestamp: number): number {
  const ageHours = Math.max(0, (Date.now() - timestamp) / (1000 * 60 * 60));
  const halfLifeHours =
    action === 'MUA_HANG'
      ? 24 * 30
      : action === 'THEM_GIO_HANG' || action === 'THEM_YEU_THICH'
        ? 24 * 14
        : 48;

  if (action === 'XEM_CHI_TIET' && ageHours > 24 * 7) {
    return 0;
  }
  return Math.pow(0.5, ageHours / halfLifeHours);
}

/**
 * Service Sách & Danh mục (Bước 3 - Phần 1 & 2) gọi API thật từ Backend Spring Boot (186 cuốn sách PostgreSQL).
 */
export const bookService = {
  async getCategories(): Promise<CategoryTreeItem[]> {
    const res = await apiRequest<BackendCategoryDto[]>('/api/v1/categories', {
      withAuth: false,
    });
    return res.map(mapCategory);
  },

  async getHomeBooks(limit = 12): Promise<HomeBooksResult> {
    const res = await apiRequest<BackendHomeBooksResponse>(
      `/api/v1/books/home?limit=${encodeURIComponent(String(limit))}`,
      { withAuth: false }
    );
    return {
      flashSaleBooks: res.flashSaleBooks.map(mapBook),
      bestSellerBooks: res.bestSellerBooks.map(mapBook),
      totalFlashSale: Number(res.tongSoSachFlashSale || 0),
      totalBestSeller: Number(res.tongSoSachBanChay || 0),
      totalVisibleBooks: Number(res.tongSoSachHienThi || 0),
    };
  },

  /**
   * Tìm kiếm, lọc đa tiêu chí, sắp xếp chuẩn sàn TMĐT và phân trang (số trang 1-based bắt đầu từ 1).
   * Tầng UI truyền `page = 1, 2, 3...`, hàm tự trừ 1 (`page - 1`) để gửi đúng 0-based lên Spring Boot,
   * rồi cộng lại +1 khi trả về cho UI hiển thị.
   */
  async getCatalogBooksPage(params?: {
    search?: string;
    categoryId?: string;
    minPrice?: number;
    maxPrice?: number;
    minRating?: number;
    flashSaleOnly?: boolean;
    bestSellerOnly?: boolean;
    inStockOnly?: boolean;
    coverType?: string;
    sortBy?: CatalogSortBy;
    page?: number;
    size?: number;
  }): Promise<BookPageResult> {
    const query = new URLSearchParams();
    if (params?.search?.trim()) query.set('search', params.search.trim());
    if (params?.categoryId && params.categoryId !== 'ALL') {
      query.set('categoryId', params.categoryId);
    }
    if (params?.minPrice !== undefined) query.set('minPrice', String(params.minPrice));
    if (params?.maxPrice !== undefined) query.set('maxPrice', String(params.maxPrice));
    if (params?.minRating !== undefined) query.set('minRating', String(params.minRating));
    if (params?.flashSaleOnly) query.set('flashSaleOnly', 'true');
    if (params?.bestSellerOnly) query.set('bestSellerOnly', 'true');
    if (params?.inStockOnly) query.set('inStockOnly', 'true');
    if (params?.coverType?.trim()) query.set('coverType', params.coverType.trim());
    if (params?.sortBy) query.set('sortBy', params.sortBy);

    const oneBasedPage = Math.max(1, Math.floor(params?.page ?? 1));
    const pageSize = Math.max(1, Math.floor(params?.size ?? 20));
    query.set('page', String(oneBasedPage - 1));
    query.set('size', String(pageSize));

    const res = await apiRequest<BackendBookPageResponse>(`/api/v1/books?${query.toString()}`, {
      withAuth: false,
    });

    return {
      items: res.items.map(mapBook),
      page: res.totalElements === 0 ? 1 : res.page + 1,
      size: res.size,
      totalElements: Number(res.totalElements || 0),
      totalPages: Number(res.totalPages || 0),
      hasNext: Boolean(res.hasNext),
      hasPrevious: Boolean(res.hasPrevious),
      appliedSort: res.appliedSort || 'popular',
    };
  },

  async getCatalogBooks(params?: {
    search?: string;
    categoryId?: string;
    minPrice?: number;
    maxPrice?: number;
    sortBy?: CatalogSortBy;
  }): Promise<Book[]> {
    const res = await this.getCatalogBooksPage({
      ...params,
      page: 1,
      size: 200,
    });
    return res.items;
  },

  async getBookDetailWithRelated(
    bookId: string
  ): Promise<{ book: Book; relatedBooks: Book[] }> {
    const dto = await apiRequest<BackendBookDetailDto>(
      `/api/v1/books/${encodeURIComponent(bookId)}`,
      { withAuth: false }
    );
    return {
      book: mapBookDetail(dto),
      relatedBooks: (dto.sachCungChuDe || []).map(mapBook),
    };
  },

  async getBookDetail(bookId: string): Promise<Book | undefined> {
    try {
      const { book } = await this.getBookDetailWithRelated(bookId);
      return book;
    } catch {
      return undefined;
    }
  },

  async getBookReviews(bookId: string, bookTitle = ''): Promise<Review[]> {
    const dtos = await apiRequest<BackendReviewDto[]>(
      `/api/v1/books/${encodeURIComponent(bookId)}/reviews`,
      { withAuth: false }
    );
    return dtos.map((r) => mapReview(r, bookTitle));
  },

  trackBookBehavior(payload: {
    book: Book;
    action: UserTrackingAction;
    dwellSeconds?: number;
  }): void {
    const dwell = Math.max(0, Math.round(payload.dwellSeconds || 0));
    const weightScore = computeWeight(payload.action, dwell);

    ignoredImpressionsByCategory.set(payload.book.categoryId, 0);

    behaviorLogs.unshift({
      id: `hv-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
      bookId: payload.book.id,
      categoryId: payload.book.categoryId,
      categoryName: payload.book.categoryName,
      action: payload.action,
      dwellSeconds: dwell,
      weightScore,
      timestamp: Date.now(),
    });
    if (behaviorLogs.length > 80) {
      behaviorLogs.length = 80;
    }
  },

  async getPersonalizedRecommendations(options?: {
    excludeBookIds?: string[];
  }): Promise<{
    reasonLabel: string;
    topCategories: string[];
    books: Book[];
  }> {
    // Lấy Top 60 sách bán chạy thật từ PostgreSQL để gợi ý bằng dữ liệu 186 sách thật
    const { items: allBooks } = await this.getCatalogBooksPage({
      sortBy: 'popular',
      page: 1,
      size: 60,
    });
    const excluded = new Set<string>(options?.excludeBookIds || []);

    if (behaviorLogs.length === 0) {
      return {
        reasonLabel: 'Gợi ý theo xu hướng đọc tuần này',
        topCategories: [],
        books: allBooks.filter((b) => !excluded.has(b.id)).slice(0, 6),
      };
    }

    const categoryScores = new Map<string, { name: string; score: number; lastTs: number }>();

    behaviorLogs.forEach((ev, idx) => {
      const decay = computeTimeDecayFactor(ev.action, ev.timestamp);
      if (decay <= 0) return;

      const recencyMultiplier = idx === 0 ? 1.8 : idx < 3 ? 1.35 : 1.0;
      const ignoredCount = ignoredImpressionsByCategory.get(ev.categoryId) || 0;
      const fatiguePenalty =
        ignoredCount > 3 ? Math.pow(0.8, ignoredCount - 3) : 1.0;

      const effectiveScore = ev.weightScore * decay * recencyMultiplier * fatiguePenalty;
      const current = categoryScores.get(ev.categoryId) || {
        name: ev.categoryName,
        score: 0,
        lastTs: ev.timestamp,
      };
      current.score += effectiveScore;
      if (ev.timestamp > current.lastTs) {
        current.lastTs = ev.timestamp;
      }
      categoryScores.set(ev.categoryId, current);
    });

    const sortedCategories = Array.from(categoryScores.entries())
      .filter(([, meta]) => meta.score > 0.2)
      .sort((a, b) => b[1].score - a[1].score || b[1].lastTs - a[1].lastTs);

    if (sortedCategories.length === 0) {
      return {
        reasonLabel: 'Gợi ý theo xu hướng đọc tuần này',
        topCategories: [],
        books: allBooks.filter((b) => !excluded.has(b.id)).slice(0, 6),
      };
    }

    const quotas = [3, 2, 1];
    const recommended: Book[] = [];
    const seenBookIds = new Set<string>(excluded);
    const activeCategoryNames: string[] = [];

    sortedCategories.slice(0, 3).forEach(([catId, meta], idx) => {
      const quota = quotas[idx] ?? 1;
      const candidates = allBooks.filter(
        (b) => b.categoryId === catId && !seenBookIds.has(b.id)
      );
      let addedForCat = 0;
      for (const b of candidates) {
        if (addedForCat >= quota || recommended.length >= 6) break;
        seenBookIds.add(b.id);
        recommended.push(b);
        addedForCat++;
      }
      if (addedForCat > 0) {
        activeCategoryNames.push(meta.name);
        ignoredImpressionsByCategory.set(
          catId,
          (ignoredImpressionsByCategory.get(catId) || 0) + 1
        );
      }
    });

    for (const b of allBooks) {
      if (recommended.length >= 6) break;
      if (!seenBookIds.has(b.id)) {
        seenBookIds.add(b.id);
        recommended.push(b);
      }
    }

    const reasonLabel =
      activeCategoryNames.length >= 2
        ? `Theo sở thích: ${activeCategoryNames.slice(0, 2).join(' • ')}`
        : activeCategoryNames.length === 1
          ? `Vì bạn quan tâm đến ${activeCategoryNames[0]}`
          : 'Gợi ý phù hợp với bạn';

    return {
      reasonLabel,
      topCategories: activeCategoryNames,
      books: recommended,
    };
  },

  async submitReview(payload: {
    bookId: string;
    bookTitle: string;
    userId: string;
    userName: string;
    orderId: string;
    rating: number;
    comment: string;
    imageUrls?: string[];
  }): Promise<Review> {
    const dto = await apiRequest<BackendReviewDto>('/api/v1/reviews', {
      method: 'POST',
      body: {
        donHangId: payload.orderId,
        sachId: payload.bookId,
        soSao: payload.rating,
        binhLuan: payload.comment.trim(),
        anhDanhGiaUrls: payload.imageUrls?.filter((u) => u && u.trim()) || [],
      },
    });
    return {
      id: dto.id,
      bookId: dto.sachId,
      bookTitle: payload.bookTitle,
      userId: dto.nguoiDungId,
      userName: dto.tenNguoiDung || payload.userName,
      orderId: payload.orderId,
      rating: Number(dto.soSao ?? payload.rating),
      comment: dto.binhLuan,
      imageUrls: dto.anhDanhGiaUrls || [],
      createdAt: dto.ngayTao || new Date().toISOString(),
      isHidden: false,
    };
  },
};
