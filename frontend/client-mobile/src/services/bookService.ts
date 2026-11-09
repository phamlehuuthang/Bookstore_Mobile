import { apiClient, db } from '@shared/index';
import { Book, Category, Review } from '@shared/types';

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

