import { OrderStatus } from '@shared/types';

export const colors = {
  // Primary Literary Brand (Forest Emerald & Deep Slate)
  primary: '#047857',
  primaryDark: '#065F46',
  primarySoft: '#ECFDF5',
  primaryBorder: '#A7F3D0',

  // Secondary & Accent (Coffee Amber for Flash Sale / Ratings)
  accent: '#B45309',
  accentSoft: '#FEF3C7',
  star: '#F59E0B',

  // Neutral Surfaces (Warm Off-White & Crisp Slate)
  background: '#F8FAFC',
  surface: '#FFFFFF',
  surfaceMuted: '#F1F5F9',
  border: '#E2E8F0',
  borderSubtle: 'rgba(15, 23, 42, 0.06)',

  // Typography
  textPrimary: '#0F172A',
  textSecondary: '#475569',
  textMuted: '#64748B',
  textInverse: '#FFFFFF',

  // Semantic States
  success: '#047857',
  successBg: '#D1FAE5',
  warning: '#B45309',
  warningBg: '#FEF3C7',
  info: '#1D4ED8',
  infoBg: '#DBEAFE',
  shipping: '#0E7490',
  shippingBg: '#CFFAFE',
  danger: '#DC2626',
  dangerBg: '#FEE2E2',
};

export const ORDER_STATUS_BADGE: Record<
  OrderStatus,
  { bg: string; text: string; label: string }
> = {
  PENDING: { bg: '#FEF3C7', text: '#B45309', label: 'Chờ duyệt' },
  PREPARING: { bg: '#DBEAFE', text: '#1D4ED8', label: 'Đang chuẩn bị' },
  SHIPPING: { bg: '#CFFAFE', text: '#0E7490', label: 'Đang giao' },
  DELIVERED: { bg: '#D1FAE5', text: '#047857', label: 'Đã giao' },
  CANCELLED: { bg: '#FEE2E2', text: '#B91C1C', label: 'Đã hủy' },
};

