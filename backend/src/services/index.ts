// ============================================================================
// BACKEND BUSINESS SERVICES (Order, Payment, Inventory, Support)
// ============================================================================

import { apiClient, db } from '../../../shared';
import { Book, Order, OrderStatus } from '../../../shared/types';

export const backendBookService = {
  getAllBooks: () => apiClient.getBooks(),
  getBookDetail: (id: string) => apiClient.getBookById(id),
  upsertBook: (payload: Partial<Book> & { title: string; price: number }) => apiClient.saveBook(payload),
};

export const backendOrderService = {
  listOrders: (userId?: string): Promise<Order[]> => apiClient.getOrders(userId),
  transitionStatus: (orderId: string, status: OrderStatus, note?: string) =>
    apiClient.updateOrderStatus(orderId, status, note),
};

export const backendInventoryService = {
  getLowStockBooks: (threshold = 5): Book[] => {
    return db.books.filter((b) => b.stock < threshold);
  },
};

export const backendAiService = {
  askAssistant: (prompt: string) => apiClient.askAiAssistant(prompt),
};

