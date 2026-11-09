import {
  Author,
  Book,
  Category,
  ChatMessage,
  DashboardStats,
  InventoryReceipt,
  NotificationItem,
  Order,
  OrderStatus,
  Publisher,
  ReturnStatus,
  Review,
  StoreConfig,
  User,
  UserRole,
  Voucher,
  formatCurrencyVND,
} from '../types';
import {
  INITIAL_AUTHORS,
  INITIAL_BOOKS,
  INITIAL_CATEGORIES,
  INITIAL_DASHBOARD_STATS,
  INITIAL_INVENTORY_RECEIPTS,
  INITIAL_NOTIFICATIONS,
  INITIAL_ORDERS,
  INITIAL_PUBLISHERS,
  INITIAL_REVIEWS,
  INITIAL_STORE_CONFIG,
  INITIAL_USERS,
  INITIAL_VOUCHERS,
} from './mockData';

// ============================================================================
// IN-MEMORY ENTERPRISE STORE & MOCK REST API ADAPTER
// Provides consistent, stateful API contracts for both Mobile & Web Admin
// ============================================================================

class MockDatabase {
  books: Book[] = [...INITIAL_BOOKS];
  categories: Category[] = [...INITIAL_CATEGORIES];
  authors: Author[] = [...INITIAL_AUTHORS];
  publishers: Publisher[] = [...INITIAL_PUBLISHERS];
  users: User[] = [...INITIAL_USERS];
  vouchers: Voucher[] = [...INITIAL_VOUCHERS];
  orders: Order[] = [...INITIAL_ORDERS];
  reviews: Review[] = [...INITIAL_REVIEWS];
  inventoryReceipts: InventoryReceipt[] = [...INITIAL_INVENTORY_RECEIPTS];
  notifications: NotificationItem[] = [...INITIAL_NOTIFICATIONS];
  storeConfig: StoreConfig = { ...INITIAL_STORE_CONFIG };
  stats: DashboardStats = { ...INITIAL_DASHBOARD_STATS };
}

export const db = new MockDatabase();

const simulateDelay = (ms = 220): Promise<void> =>
  new Promise((resolve) => setTimeout(resolve, ms));

export const apiClient = {
  // --------------------------------------------------------------------------
  // 1. AUTHENTICATION & PLATFORM ROLE GUARD
  // --------------------------------------------------------------------------
  async login(
    email: string,
    _password: string,
    platform: 'MOBILE_CLIENT' | 'ADMIN_WEB'
  ): Promise<{ user: User; token: string }> {
    await simulateDelay(300);
    const normalizedEmail = email.trim().toLowerCase();
    const foundUser =
      db.users.find((u) => u.email.toLowerCase() === normalizedEmail) ||
      (normalizedEmail.includes('admin')
        ? db.users.find((u) => u.role === 'ADMIN')
        : db.users[0]);

    if (!foundUser) {
      throw new Error('Tài khoản hoặc mật khẩu không chính xác.');
    }

    if (foundUser.isLocked) {
      throw new Error('Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hotline 1900 6886.');
    }

    // STRICT ENTERPRISE PLATFORM GUARD:
    // Reject Admin/Staff roles on Client Mobile App!
    if (platform === 'MOBILE_CLIENT' && foundUser.role !== 'CUSTOMER') {
      throw new Error(
        'Tài khoản quản trị viên vui lòng đăng nhập trên hệ thống Web Dashboard máy tính'
      );
    }

    if (platform === 'ADMIN_WEB' && foundUser.role === 'CUSTOMER') {
      throw new Error(
        'Tài khoản khách hàng không có quyền truy cập hệ thống Quản trị Web Dashboard.'
      );
    }

    return {
      user: foundUser,
      token: `jwt-token-${foundUser.id}-${Date.now()}`,
    };
  },

  // --------------------------------------------------------------------------
  // 2. BOOKS & CATALOG API
  // --------------------------------------------------------------------------
  async getBooks(params?: {
    search?: string;
    categoryId?: string;
    minPrice?: number;
    maxPrice?: number;
    onlyVisible?: boolean;
    sortBy?: 'popular' | 'price_asc' | 'price_desc' | 'newest' | 'rating';
  }): Promise<Book[]> {
    await simulateDelay(180);
    let result = [...db.books];

    if (params?.onlyVisible) {
      result = result.filter((b) => b.isVisible);
    }
    if (params?.categoryId && params.categoryId !== 'ALL') {
      result = result.filter((b) => b.categoryId === params.categoryId);
    }
    if (params?.search && params.search.trim().length > 0) {
      const q = params.search.trim().toLowerCase();
      result = result.filter(
        (b) =>
          b.title.toLowerCase().includes(q) ||
          b.authorName.toLowerCase().includes(q) ||
          b.isbn.toLowerCase().includes(q) ||
          b.categoryName.toLowerCase().includes(q)
      );
    }
    if (typeof params?.minPrice === 'number') {
      result = result.filter((b) => (b.flashSalePrice ?? b.price) >= params.minPrice!);
    }
    if (typeof params?.maxPrice === 'number') {
      result = result.filter((b) => (b.flashSalePrice ?? b.price) <= params.maxPrice!);
    }

    switch (params?.sortBy) {
      case 'price_asc':
        result.sort((a, b) => (a.flashSalePrice ?? a.price) - (b.flashSalePrice ?? b.price));
        break;
      case 'price_desc':
        result.sort((a, b) => (b.flashSalePrice ?? b.price) - (a.flashSalePrice ?? a.price));
        break;
      case 'rating':
        result.sort((a, b) => b.ratingAvg - a.ratingAvg);
        break;
      case 'newest':
        result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        break;
      case 'popular':
      default:
        result.sort((a, b) => b.soldCount - a.soldCount);
        break;
    }

    return result;
  },

  async getBookById(id: string): Promise<Book | undefined> {
    await simulateDelay(120);
    return db.books.find((b) => b.id === id);
  },

  async saveBook(bookData: Partial<Book> & { title: string; price: number }): Promise<Book> {
    await simulateDelay(250);
    if (bookData.id) {
      const idx = db.books.findIndex((b) => b.id === bookData.id);
      if (idx !== -1) {
        db.books[idx] = { ...db.books[idx], ...bookData };
        return db.books[idx];
      }
    }
    const category = db.categories.find((c) => c.id === bookData.categoryId) || db.categories[0];
    const newBook: Book = {
      id: `book-${Date.now()}`,
      isbn: bookData.isbn || `978-604-1-${Math.floor(10000 + Math.random() * 89999)}-1`,
      title: bookData.title,
      slug: bookData.title.toLowerCase().replace(/\s+/g, '-'),
      authorId: bookData.authorId || 'auth-1',
      authorName: bookData.authorName || 'Nhiều tác giả',
      publisherId: bookData.publisherId || 'pub-1',
      publisherName: bookData.publisherName || 'NXB Trẻ',
      categoryId: category.id,
      categoryName: category.name,
      price: Number(bookData.price),
      originalPrice: Number(bookData.originalPrice || bookData.price * 1.2),
      stock: Number(bookData.stock ?? 20),
      soldCount: 0,
      ratingAvg: 5.0,
      ratingCount: 1,
      pageCount: Number(bookData.pageCount || 280),
      publishYear: Number(bookData.publishYear || 2026),
      coverFormat: bookData.coverFormat || 'Bìa mềm',
      coverUrl:
        bookData.coverUrl ||
        'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=600&q=80',
      galleryUrls: [
        bookData.coverUrl ||
          'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=600&q=80',
      ],
      shortSummary: bookData.shortSummary || 'Sách mới phát hành tại hệ thống Tri Thức Việt BookStore.',
      description: bookData.description || 'Ấn phẩm chính hãng chất lượng cao.',
      isFlashSale: Boolean(bookData.isFlashSale),
      flashSalePrice: bookData.flashSalePrice,
      isBestSeller: Boolean(bookData.isBestSeller),
      isVisible: bookData.isVisible ?? true,
      createdAt: new Date().toISOString(),
    };
    db.books.unshift(newBook);
    return newBook;
  },

  async deleteBook(bookId: string): Promise<void> {
    await simulateDelay(200);
    db.books = db.books.filter((b) => b.id !== bookId);
  },

  async getCategories(): Promise<Category[]> {
    await simulateDelay(100);
    return [...db.categories].sort((a, b) => a.displayOrder - b.displayOrder);
  },

  // --------------------------------------------------------------------------
  // 3. ONLINE BOOK CONSULTANT SERVICE (NATURAL LANGUAGE SEARCH & SUMMARY)
  // --------------------------------------------------------------------------
  async askAiAssistant(prompt: string): Promise<ChatMessage> {
    await simulateDelay(380);
    const q = prompt.toLowerCase();

    if (q.includes('lập trình') || q.includes('code') || q.includes('người mới') || q.includes('cntt')) {
      const techBooks = db.books.filter((b) => b.categoryId === 'cat-tech').slice(0, 3);
      return {
        id: `bot-${Date.now()}`,
        sender: 'BOT',
        timestamp: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
        text:
          'Chào bạn! Với người mới bắt đầu học lập trình hoặc muốn nâng tầm tư duy kỹ thuật, mình gợi ý 3 cuốn sách nền tảng dễ đọc và thực tiễn nhất hiện nay:\n\n' +
          '1. **Code Dạo Ký Sự** (135.000 đ) – Định hướng lộ trình từ sinh viên tới Fullstack Developer.\n' +
          '2. **Clean Code** (245.000 đ Flash Sale) – Chuẩn mực viết code sạch dễ bảo trì.\n' +
          '3. **Clean Architecture** (285.000 đ) – Tư duy thiết kế hệ thống chuyên nghiệp.',
        suggestedBooks: techBooks,
      };
    }

    if (q.includes('kinh tế') || q.includes('150k') || q.includes('tài chính') || q.includes('tiền')) {
      const econUnder150k = db.books.filter(
        (b) => b.categoryId === 'cat-econ' && (b.flashSalePrice ?? b.price) <= 150000
      );
      return {
        id: `bot-${Date.now()}`,
        sender: 'BOT',
        timestamp: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
        text:
          'Mình đã lọc ngay các tựa **sách Kinh tế & Tài chính cá nhân dưới 150.000 đ** được độc giả đánh giá cao nhất tại cửa hàng:\n\n' +
          econUnder150k
            .map(
              (b, idx) =>
                `${idx + 1}. **${b.title}** – Giá ưu đãi: **${formatCurrencyVND(
                  b.flashSalePrice ?? b.price
                )}** (★ ${b.ratingAvg})`
            )
            .join('\n'),
        suggestedBooks: econUnder150k,
      };
    }

    if (q.includes('đắc nhân tâm') || q.includes('tóm tắt')) {
      const dntBook = db.books.find((b) => b.id === 'book-3');
      return {
        id: `bot-${Date.now()}`,
        sender: 'BOT',
        timestamp: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
        text:
          '📖 **Tóm tắt cốt lõi cuốn "Đắc Nhân Tâm" (Dale Carnegie):**\n\n' +
          '• **Nguyên tắc 1 – Ứng xử căn bản:** Không chỉ trích, oán trách hay than phiền; hãy thành thật khen ngợi và ghi nhận giá trị của người khác.\n' +
          '• **Nguyên tắc 2 – Gây thiện cảm:** Luôn mỉm cười, nhớ tên người đối diện và biết lắng nghe chân thành.\n' +
          '• **Nguyên tắc 3 – Dẫn dắt tư duy:** Tôn trọng ý kiến người khác, khơi gợi để họ tự nhận ra giải pháp thay vì áp đặt.\n\n' +
          'Hiện bản Đặc Biệt bìa cứng đang có Flash Sale chỉ **89.000 đ**!',
        suggestedBooks: dntBook ? [dntBook] : [],
      };
    }

    if (q.includes('vận chuyển') || q.includes('đổi trả') || q.includes('chính sách') || q.includes('ship')) {
      return {
        id: `bot-${Date.now()}`,
        sender: 'BOT',
        timestamp: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
        text:
          '🚚 **Chính sách Vận chuyển & Đổi trả tại Tri Thức Việt BookStore:**\n\n' +
          '• **Phí giao hàng:** Đồng giá 25.000 đ toàn quốc. **Miễn phí vận chuyển (Freeship)** cho đơn từ 300.000 đ hoặc khi áp mã `FREESHIP25K`.\n' +
          '• **Thời gian giao:** Nội thành TP.HCM & Hà Nội từ 24h – 48h (bọc chống sốc 3 lớp).\n' +
          '• **Đổi trả miễn phí 7 ngày:** Hỗ trợ đổi mới 100% nếu sách bị lỗi in ấn, thiếu trang hoặc móp góc do vận chuyển ngay trong mục *Đơn hàng -> Yêu cầu đổi trả*.',
      };
    }

    // Fallback smart search across books
    const matchedBooks = db.books
      .filter(
        (b) =>
          b.title.toLowerCase().includes(q) ||
          b.authorName.toLowerCase().includes(q) ||
          b.categoryName.toLowerCase().includes(q)
      )
      .slice(0, 3);

    const recommended = matchedBooks.length > 0 ? matchedBooks : db.books.filter((b) => b.isBestSeller).slice(0, 3);

    return {
      id: `bot-${Date.now()}`,
      sender: 'BOT',
      timestamp: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
      text:
        matchedBooks.length > 0
          ? `Mình tìm thấy ${matchedBooks.length} cuốn sách phù hợp nhất với yêu cầu "${prompt}" của bạn:`
          : `Dựa trên nhu cầu "${prompt}", mình đề xuất 3 tựa sách Best-Seller đang được yêu thích nhất tuần này:`,
      suggestedBooks: recommended,
    };
  },

  // --------------------------------------------------------------------------
  // 4. ORDERS, VOUCHERS, REVIEWS & RETURNS API
  // --------------------------------------------------------------------------
  async getOrders(userId?: string): Promise<Order[]> {
    await simulateDelay(160);
    if (userId) {
      return db.orders.filter((o) => o.userId === userId);
    }
    return [...db.orders];
  },

  async updateOrderStatus(orderId: string, newStatus: OrderStatus, description?: string): Promise<Order> {
    await simulateDelay(200);
    const order = db.orders.find((o) => o.id === orderId);
    if (!order) throw new Error('Không tìm thấy đơn hàng');

    order.status = newStatus;
    order.updatedAt = new Date().toISOString();
    const statusTitles: Record<OrderStatus, string> = {
      PENDING: 'Chờ duyệt đơn hàng',
      PREPARING: 'Kho đang đóng gói',
      SHIPPING: 'Đang giao hàng',
      DELIVERED: 'Đã giao thành công',
      CANCELLED: 'Đơn hàng đã hủy',
    };

    order.timeline.push({
      status: newStatus,
      title: statusTitles[newStatus],
      description: description || `Cập nhật trạng thái sang ${statusTitles[newStatus]}`,
      timestamp: new Date().toLocaleString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      }),
    });

    if (newStatus === 'DELIVERED') {
      order.paymentStatus = 'PAID';
    }
    return { ...order };
  },

  async resolveReturnRequest(
    orderId: string,
    status: ReturnStatus,
    adminNote: string
  ): Promise<Order> {
    await simulateDelay(220);
    const order = db.orders.find((o) => o.id === orderId);
    if (!order || !order.returnRequest) {
      throw new Error('Không tìm thấy yêu cầu đổi trả');
    }
    order.returnRequest = {
      ...order.returnRequest,
      status,
      adminNote,
      resolvedAt: new Date().toISOString(),
    };
    return { ...order };
  },
};

