import { apiClient } from '@shared/index';
import { Order, OrderStatus, ReturnStatus } from '@shared/types';

export const adminOrderService = {
  async getAllOrders(): Promise<Order[]> {
    return apiClient.getOrders();
  },

  async updateStatus(orderId: string, status: OrderStatus, note?: string): Promise<Order> {
    return apiClient.updateOrderStatus(orderId, status, note);
  },

  async handleReturnRequest(
    orderId: string,
    status: ReturnStatus,
    adminNote: string
  ): Promise<Order> {
    return apiClient.resolveReturnRequest(orderId, status, adminNote);
  },
};

