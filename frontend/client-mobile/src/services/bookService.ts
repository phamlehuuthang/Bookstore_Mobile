import { apiClient, db } from '@shared/index';
import { Book, Category, Review } from '@shared/types';

export type UserTrackingAction =
  | 'XEM_CHI_TIET'
  | 'TIM_KIEM'
  | 'THEM_YEU_THICH'
  | 'THEM_GIO_HANG'
  | 'MUA_HANG';

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

// Lưu lịch sử hành vi người dùng (đa chủ đề để mô phỏng thực tế Shopee / Tiki)
const behaviorLogs: UserBehaviorEvent[] = [
  {
    id: 'hv-seed-1',
    bookId: 'book-1',
    categoryId: 'cat-tech',
    categoryName: 'Công nghệ & Lập trình',
    action: 'XEM_CHI_TIET',
    dwellSeconds: 28,
    weightScore: 5,
    timestamp: Date.now() - 1000 * 60 * 15, // 15 phút trước
  },
  {
    id: 'hv-seed-2',
    bookId: 'book-2',
    categoryId: 'cat-econ',
    categoryName: 'Kinh tế & Quản trị',
    action: 'THEM_YEU_THICH',
    dwellSeconds: 10,
    weightScore: 4,
    timestamp: Date.now() - 1000 * 60 * 45, // 45 phút trước
  },
];

// Đếm số lần 1 chủ đề đã được hiển thị trên kệ Đề xuất nhưng khách KHÔNG bấm vào đọc (Impression Fatigue)
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

// Hệ số suy giảm theo thời gian (Time Decay):
// - Hành vi xem chi tiết giảm nhanh (chu kỳ bán rã 48h, hết hạn sau 7 ngày)
// - Hành vi thả tim / thêm giỏ / mua hàng giữ lâu hơn (chu kỳ bán rã 14–30 ngày)
function computeTimeDecayFactor(action: UserTrackingAction, timestamp: number): number {
  const ageHours = Math.max(0, (Date.now() - timestamp) / (1000 * 60 * 60));
  const halfLifeHours =
    action === 'MUA_HANG'
      ? 24 * 30
      : action === 'THEM_GIO_HANG' || action === 'THEM_YEU_THICH'
        ? 24 * 14
        : 48;

  if (action === 'XEM_CHI_TIET' && ageHours > 24 * 7) {
    return 0; // Quá 7 ngày chỉ xem mà không mua/yêu thích thì tự hết hạn
  }
  return Math.pow(0.5, ageHours / halfLifeHours);
}

export const bookService = {
  async getCatalogBooks(params?: {
    search?: string;
    categoryId?: string;
    minPrice?: number;
    maxPrice?: number;
    sortBy?: 'popular' | 'price_asc' | 'price_desc' | 'newest' | 'rating';
  }): Promise<Book[]> {
    return apiClient.getBooks({ ...params, onlyVisible: true });
  },

  async getBookDetail(bookId: string): Promise<Book | undefined> {
    return apiClient.getBookById(bookId);
  },

  async getCategories(): Promise<Category[]> {
    return apiClient.getCategories();
  },

  async getBookReviews(bookId: string): Promise<Review[]> {
    return db.reviews.filter((r) => r.bookId === bookId && !r.isHidden);
  },

  trackBookBehavior(payload: {
    book: Book;
    action: UserTrackingAction;
    dwellSeconds?: number;
  }): void {
    const dwell = Math.max(0, Math.round(payload.dwellSeconds || 0));
    const weightScore = computeWeight(payload.action, dwell);

    // Khi người dùng thực sự tương tác với chủ đề này, xóa bộ đếm "bỏ qua" (reset Impression Fatigue)
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
    const allBooks = await apiClient.getBooks({ onlyVisible: true, sortBy: 'popular' });
    const excluded = new Set<string>(options?.excludeBookIds || []);

    if (behaviorLogs.length === 0) {
      return {
        reasonLabel: 'Gợi ý theo xu hướng đọc tuần này',
        topCategories: [],
        books: allBooks.filter((b) => !excluded.has(b.id)).slice(0, 6),
      };
    }

    // 1. Tính điểm từng chủ đề có áp dụng Time Decay + phạt Impression Fatigue nếu bị lướt qua nhiều lần
    const categoryScores = new Map<string, { name: string; score: number; lastTs: number }>();

    behaviorLogs.forEach((ev, idx) => {
      const decay = computeTimeDecayFactor(ev.action, ev.timestamp);
      if (decay <= 0) return;

      const recencyMultiplier = idx === 0 ? 1.8 : idx < 3 ? 1.35 : 1.0;
      const ignoredCount = ignoredImpressionsByCategory.get(ev.categoryId) || 0;
      // Nếu chủ đề đã hiện > 3 lần liên tiếp mà khách không bấm vào xem, giảm 20% điểm mỗi lần tiếp theo
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

    // 2. Phân bổ đa chủ đề (Multi-Interest Mixing) cho tối đa 6 cuốn trên kệ Đề xuất:
    // - Chủ đề #1: tối đa 3 cuốn
    // - Chủ đề #2: tối đa 2 cuốn
    // - Chủ đề #3 (hoặc sách khám phá mới): 1 cuốn
    // Đồng thời LOẠI TRỪ những cuốn khách đã mua / đã bỏ giỏ / đã thả tim (chỉ gợi ý sách CÙNG LOẠI chưa có)
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
        // Tăng bộ đếm hiển thị; nếu khách bấm vào đọc thì trackBookBehavior sẽ reset về 0
        ignoredImpressionsByCategory.set(
          catId,
          (ignoredImpressionsByCategory.get(catId) || 0) + 1
        );
      }
    });

    // Bù thêm sách bán chạy khác nếu chưa đủ 6 cuốn (Exploration 15%)
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
    const newReview: Review = {
      id: `rev-${Date.now()}`,
      bookId: payload.bookId,
      bookTitle: payload.bookTitle,
      userId: payload.userId,
      userName: payload.userName,
      orderId: payload.orderId,
      rating: payload.rating,
      comment: payload.comment,
      imageUrls: payload.imageUrls || [],
      createdAt: new Date().toISOString(),
      isHidden: false,
    };
    db.reviews.unshift(newReview);
    const order = db.orders.find((o) => o.id === payload.orderId);
    if (order) {
      order.isReviewed = true;
    }
    return newReview;
  },
};
