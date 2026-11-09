import { db } from '@shared/index';
import {
  InventoryReceipt,
  InventoryReceiptItem,
  NotificationItem,
  Review,
  StoreConfig,
  User,
  UserRole,
  Voucher,
} from '@shared/types';

export const adminSystemService = {
  // 1. INVENTORY & IMPORT RECEIPTS
  async getInventoryReceipts(): Promise<InventoryReceipt[]> {
    return [...db.inventoryReceipts];
  },

  async createInventoryReceipt(payload: {
    supplierName: string;
    createdBy: string;
    note: string;
    items: InventoryReceiptItem[];
  }): Promise<InventoryReceipt> {
    const totalQuantity = payload.items.reduce((acc, i) => acc + i.quantity, 0);
    const totalCost = payload.items.reduce(
      (acc, i) => acc + i.quantity * i.importPrice,
      0
    );

    const receipt: InventoryReceipt = {
      id: `inv-${Date.now()}`,
      receiptCode: `PNK-202611-${String(db.inventoryReceipts.length + 1).padStart(2, '0')}`,
      supplierName: payload.supplierName,
      createdBy: payload.createdBy,
      note: payload.note,
      items: payload.items,
      totalQuantity,
      totalCost,
      createdAt: new Date().toISOString(),
    };

    // Increment real book stock in DB
    payload.items.forEach((item) => {
      const book = db.books.find((b) => b.id === item.bookId);
      if (book) {
        book.stock += item.quantity;
      }
    });

    db.inventoryReceipts.unshift(receipt);
    return receipt;
  },

  // 2. CUSTOMERS & STAFF RBAC
  async getUsers(): Promise<User[]> {
    return [...db.users];
  },

  async toggleUserLock(userId: string): Promise<User | undefined> {
    const user = db.users.find((u) => u.id === userId);
    if (user) {
      user.isLocked = !user.isLocked;
    }
    return user;
  },

  async updateStaffRole(userId: string, role: UserRole): Promise<User | undefined> {
    const user = db.users.find((u) => u.id === userId);
    if (user) {
      user.role = role;
    }
    return user;
  },

  async createStaffAccount(payload: {
    fullName: string;
    email: string;
    phone: string;
    role: UserRole;
  }): Promise<User> {
    const newStaff: User = {
      id: `user-staff-${Date.now()}`,
      fullName: payload.fullName,
      email: payload.email,
      phone: payload.phone,
      role: payload.role,
      isLocked: false,
      totalSpent: 0,
      orderCount: 0,
      createdAt: new Date().toISOString(),
      addresses: [],
    };
    db.users.push(newStaff);
    return newStaff;
  },

  // 3. VOUCHERS, REVIEWS & PUSH NOTIFICATIONS
  async getVouchers(): Promise<Voucher[]> {
    return [...db.vouchers];
  },

  async saveVoucher(payload: Partial<Voucher> & { code: string; title: string }): Promise<Voucher> {
    if (payload.id) {
      const idx = db.vouchers.findIndex((v) => v.id === payload.id);
      if (idx !== -1) {
        db.vouchers[idx] = { ...db.vouchers[idx], ...payload };
        return db.vouchers[idx];
      }
    }
    const newVoucher: Voucher = {
      id: `vch-${Date.now()}`,
      code: payload.code.toUpperCase(),
      title: payload.title,
      description: payload.description || 'Chương trình khuyến mãi đặc biệt',
      discountType: payload.discountType || 'FIXED',
      discountValue: Number(payload.discountValue || 30000),
      maxDiscountAmount: payload.maxDiscountAmount,
      minOrderValue: Number(payload.minOrderValue || 150000),
      usageLimit: Number(payload.usageLimit || 200),
      usedCount: 0,
      startDate: payload.startDate || '2026-11-01',
      endDate: payload.endDate || '2026-12-31',
      isActive: payload.isActive ?? true,
    };
    db.vouchers.unshift(newVoucher);
    return newVoucher;
  },

  async toggleVoucherActive(voucherId: string): Promise<void> {
    const v = db.vouchers.find((item) => item.id === voucherId);
    if (v) v.isActive = !v.isActive;
  },

  async getReviews(): Promise<Review[]> {
    return [...db.reviews];
  },

  async toggleReviewHidden(reviewId: string): Promise<void> {
    const rev = db.reviews.find((r) => r.id === reviewId);
    if (rev) rev.isHidden = !rev.isHidden;
  },

  async sendPushNotification(title: string, message: string): Promise<NotificationItem> {
    const notif: NotificationItem = {
      id: `notif-${Date.now()}`,
      title,
      message,
      type: 'PROMO',
      isRead: false,
      createdAt: 'Vừa phát sóng',
    };
    db.notifications.unshift(notif);
    return notif;
  },

  // 4. STORE CONFIGURATION
  async getStoreConfig(): Promise<StoreConfig> {
    return { ...db.storeConfig };
  },

  async updateStoreConfig(nextConfig: StoreConfig): Promise<StoreConfig> {
    db.storeConfig = { ...nextConfig };
    return { ...db.storeConfig };
  },
};

