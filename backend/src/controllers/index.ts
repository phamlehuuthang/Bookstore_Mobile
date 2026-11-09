// ============================================================================
// BACKEND REST CONTROLLERS (Serving both Mobile Client & Admin Web Dashboard)
// ============================================================================

import {
  backendAiService,
  backendBookService,
  backendInventoryService,
  backendOrderService,
} from '../services';
import { apiClient } from '../../../shared';

export const controllers = {
  auth: {
    login: (email: string, password: string, platform: 'MOBILE_CLIENT' | 'ADMIN_WEB') =>
      apiClient.login(email, password, platform),
  },
  books: {
    list: () => backendBookService.getAllBooks(),
    detail: (id: string) => backendBookService.getBookDetail(id),
  },
  orders: {
    list: (userId?: string) => backendOrderService.listOrders(userId),
  },
  inventory: {
    lowStockAlerts: () => backendInventoryService.getLowStockBooks(5),
  },
  ai: {
    chat: (prompt: string) => backendAiService.askAssistant(prompt),
  },
};

