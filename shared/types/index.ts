// ============================================================================
// SHARED DATA MODELS, ENUMS & DTOs FOR BOOKSTORE ENTERPRISE
// Used across Client Mobile App, Admin Web Dashboard, and Backend API
// ============================================================================

export type UserRole = 'CUSTOMER' | 'ADMIN' | 'WAREHOUSE_MANAGER' | 'CSKH';

export type OrderStatus =
  | 'PENDING'      // Chờ duyệt
  | 'PREPARING'    // Đang chuẩn bị
  | 'SHIPPING'     // Đang giao
  | 'DELIVERED'    // Đã giao
  | 'CANCELLED';   // Đã hủy

export type PaymentMethod = 'COD' | 'BANK_QR' | 'EWALLET';

export type PaymentStatus = 'UNPAID' | 'PAID' | 'REFUNDED';

export type ReturnStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export type DiscountType = 'PERCENTAGE' | 'FIXED';

export interface Address {
  id: string;
  fullName: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  streetAddress: string;
  isDefault: boolean;
}

export interface User {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  avatarUrl?: string;
  role: UserRole;
  isLocked: boolean;
  totalSpent: number;
  orderCount: number;
  createdAt: string;
  addresses: Address[];
}

export interface Category {
  id: string;
  name: string;
  slug: string;
  parentId: string | null;
  description: string;
  iconName: string;
  bookCount: number;
  isActive: boolean;
  displayOrder: number;
}

export interface Author {
  id: string;
  name: string;
  bio: string;
  bookCount: number;
}

export interface Publisher {
  id: string;
  name: string;
  address: string;
  bookCount: number;
}

export interface Book {
  id: string;
  isbn: string;
  title: string;
  slug: string;
  authorId: string;
  authorName: string;
  publisherId: string;
  publisherName: string;
  categoryId: string;
  categoryName: string;
  SubCategoryName?: string;
  price: number;
  originalPrice: number;
  stock: number;
  soldCount: number;
  ratingAvg: number;
  ratingCount: number;
  pageCount: number;
  publishYear: number;
  coverFormat: 'Bìa mềm' | 'Bìa cứng';
  coverUrl: string;
  galleryUrls: string[];
  shortSummary: string;
  description: string;
  isFlashSale: boolean;
  flashSalePrice?: number;
  isBestSeller: boolean;
  isVisible: boolean;
  createdAt: string;
}

export interface Review {
  id: string;
  bookId: string;
  bookTitle: string;
  userId: string;
  userName: string;
  userAvatar?: string;
  orderId: string;
  rating: number;
  comment: string;
  imageUrls: string[];
  createdAt: string;
  isHidden: boolean;
}

export interface Voucher {
  id: string;
  code: string;
  title: string;
  description: string;
  discountType: DiscountType;
  discountValue: number;
  maxDiscountAmount?: number;
  minOrderValue: number;
  usageLimit: number;
  usedCount: number;
  startDate: string;
  endDate: string;
  isActive: boolean;
}

export interface OrderItem {
  bookId: string;
  title: string;
  coverUrl: string;
  authorName: string;
  unitPrice: number;
  quantity: number;
}

export interface OrderTimelineEvent {
  status: OrderStatus;
  title: string;
  description: string;
  timestamp: string;
}

export interface ReturnRequest {
  id: string;
  orderId: string;
  orderCode: string;
  customerName: string;
  reason: string;
  description: string;
  evidenceImages: string[];
  status: ReturnStatus;
  adminNote?: string;
  createdAt: string;
  resolvedAt?: string;
}

export interface Order {
  id: string;
  orderCode: string;
  userId: string;
  customerName: string;
  customerPhone: string;
  shippingAddress: Address;
  items: OrderItem[];
  subtotal: number;
  shippingFee: number;
  discountAmount: number;
  voucherCode?: string;
  totalAmount: number;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  status: OrderStatus;
  note?: string;
  timeline: OrderTimelineEvent[];
  returnRequest?: ReturnRequest;
  isReviewed: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface InventoryReceiptItem {
  bookId: string;
  bookTitle: string;
  isbn: string;
  quantity: number;
  importPrice: number;
}

export interface InventoryReceipt {
  id: string;
  receiptCode: string;
  supplierName: string;
  createdBy: string;
  note: string;
  items: InventoryReceiptItem[];
  totalQuantity: number;
  totalCost: number;
  createdAt: string;
}

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  type: 'ORDER' | 'PROMO' | 'SYSTEM';
  isRead: boolean;
  createdAt: string;
}

export interface StoreConfig {
  storeName: string;
  tagline: string;
  logoUrl: string;
  hotline: string;
  supportEmail: string;
  headquartersAddress: string;
  defaultShippingFee: number;
  freeShippingThreshold: number;
  bankName: string;
  bankAccountNumber: string;
  bankAccountHolder: string;
  lowStockThreshold: number;
}

export interface RevenueDataPoint {
  label: string;
  revenue: number;
  orders: number;
  profit: number;
}

export interface DashboardStats {
  totalRevenue: number;
  revenueGrowth: number;
  totalOrders: number;
  ordersGrowth: number;
  newCustomers: number;
  customersGrowth: number;
  lowStockCount: number;
  pendingOrdersCount: number;
  pendingReturnsCount: number;
  revenueByDay: RevenueDataPoint[];
  revenueByMonth: RevenueDataPoint[];
  revenueByYear: RevenueDataPoint[];
}

export interface ChatMessage {
  id: string;
  sender: 'USER' | 'BOT';
  text: string;
  timestamp: string;
  suggestedBooks?: Book[];
}

// ============================================================================
// UTILITY FORMATTERS (VIETNAMESE LOCALE STANDARDS)
// ============================================================================

export function formatCurrencyVND(amount: number): string {
  return `${Math.round(amount).toLocaleString('vi-VN')}đ`;
}

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: 'Chờ duyệt',
  PREPARING: 'Đang chuẩn bị',
  SHIPPING: 'Đang giao',
  DELIVERED: 'Đã giao',
  CANCELLED: 'Đã hủy',
};

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  COD: 'Thanh toán khi nhận hàng (COD)',
  BANK_QR: 'Chuyển khoản VietQR',
  EWALLET: 'Ví điện tử MoMo / ZaloPay',
};

