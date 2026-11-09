import { apiClient, db } from '@shared/index';
import { Address, Order, OrderItem, PaymentMethod, Voucher } from '@shared/types';

export const orderService = {
  async getCustomerOrders(userId: string): Promise<Order[]> {
    return apiClient.getOrders(userId);
  },

  async getAvailableVouchers(): Promise<Voucher[]> {
    return db.vouchers.filter((v) => v.isActive && v.usedCount < v.usageLimit);
  },

  async createOrder(payload: {
    userId: string;
    customerName: string;
    customerPhone: string;
    shippingAddress: Address;
    items: OrderItem[];
    subtotal: number;
    shippingFee: number;
    discountAmount: number;
    voucherCode?: string;
    paymentMethod: PaymentMethod;
    note?: string;
  }): Promise<Order> {
    const newOrder: Order = {
      id: `ord-${Date.now()}`,
      orderCode: `BS-${Math.floor(100000 + Math.random() * 899999)}`,
      userId: payload.userId,
      customerName: payload.customerName,
      customerPhone: payload.customerPhone,
      shippingAddress: payload.shippingAddress,
      items: payload.items,
      subtotal: payload.subtotal,
      shippingFee: payload.shippingFee,
      discountAmount: payload.discountAmount,
      voucherCode: payload.voucherCode,
      totalAmount: Math.max(0, payload.subtotal + payload.shippingFee - payload.discountAmount),
      paymentMethod: payload.paymentMethod,
      paymentStatus: payload.paymentMethod === 'COD' ? 'UNPAID' : 'PAID',
      status: 'PENDING',
      note: payload.note,
      timeline: [
        {
          status: 'PENDING',
          title: 'Đặt hàng thành công',
          description: 'Hệ thống đã tiếp nhận đơn hàng và đang chờ kho xác nhận.',
          timestamp: new Date().toLocaleString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          }),
        },
      ],
      isReviewed: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    // Deduct stock
    payload.items.forEach((item) => {
      const book = db.books.find((b) => b.id === item.bookId);
      if (book) {
        book.stock = Math.max(0, book.stock - item.quantity);
        book.soldCount += item.quantity;
      }
    });

    db.orders.unshift(newOrder);
    return newOrder;
  },

  async cancelCustomerOrder(orderId: string, reason: string): Promise<Order> {
    return apiClient.updateOrderStatus(
      orderId,
      'CANCELLED',
      `Khách hàng hủy đơn. Lý do: ${reason}`
    );
  },

  async submitReturnRequest(payload: {
    orderId: string;
    reason: string;
    description: string;
    evidenceImages: string[];
  }): Promise<Order> {
    const order = db.orders.find((o) => o.id === payload.orderId);
    if (!order) {
      throw new Error('Không tìm thấy đơn hàng');
    }
    order.returnRequest = {
      id: `ret-${Date.now()}`,
      orderId: order.id,
      orderCode: order.orderCode,
      customerName: order.customerName,
      reason: payload.reason,
      description: payload.description,
      evidenceImages:
        payload.evidenceImages.length > 0
          ? payload.evidenceImages
          : ['https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=600&q=80'],
      status: 'PENDING',
      createdAt: new Date().toISOString(),
    };
    return { ...order };
  },
};

