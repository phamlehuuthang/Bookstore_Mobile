import React, { createContext, useContext, useMemo, useState } from 'react';
import { Book, Voucher } from '@shared/types';
import { INITIAL_BOOKS, INITIAL_VOUCHERS } from '@shared/api-client/mockData';

export interface CartEntry {
  book: Book;
  quantity: number;
  selected: boolean;
}

interface CartContextValue {
  items: CartEntry[];
  appliedVoucher: Voucher | null;
  voucherError: string | null;
  totalItemsCount: number;
  selectedItems: CartEntry[];
  subtotal: number;
  shippingFee: number;
  discountAmount: number;
  finalTotal: number;
  addToCart: (book: Book, quantity?: number) => void;
  updateQuantity: (bookId: string, quantity: number) => void;
  removeFromCart: (bookId: string) => void;
  toggleSelect: (bookId: string) => void;
  toggleSelectAll: (selectAll: boolean) => void;
  applyVoucherCode: (code: string) => boolean;
  clearVoucher: () => void;
  clearSelectedAfterCheckout: () => void;
}

const CartContext = createContext<CartContextValue | undefined>(undefined);

export function CartProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<CartEntry[]>([
    { book: INITIAL_BOOKS[0], quantity: 1, selected: true },
    { book: INITIAL_BOOKS[3], quantity: 1, selected: true },
  ]);
  const [appliedVoucher, setAppliedVoucher] = useState<Voucher | null>(INITIAL_VOUCHERS[0]);
  const [voucherError, setVoucherError] = useState<string | null>(null);

  const addToCart = (book: Book, quantity = 1) => {
    setItems((prev) => {
      const existingIdx = prev.findIndex((entry) => entry.book.id === book.id);
      if (existingIdx !== -1) {
        const next = [...prev];
        next[existingIdx] = {
          ...next[existingIdx],
          quantity: Math.min(book.stock, next[existingIdx].quantity + quantity),
          selected: true,
        };
        return next;
      }
      return [{ book, quantity: Math.min(book.stock, quantity), selected: true }, ...prev];
    });
  };

  const updateQuantity = (bookId: string, quantity: number) => {
    if (quantity <= 0) {
      removeFromCart(bookId);
      return;
    }
    setItems((prev) =>
      prev.map((item) =>
        item.book.id === bookId
          ? { ...item, quantity: Math.min(item.book.stock, quantity) }
          : item
      )
    );
  };

  const removeFromCart = (bookId: string) => {
    setItems((prev) => prev.filter((item) => item.book.id !== bookId));
  };

  const toggleSelect = (bookId: string) => {
    setItems((prev) =>
      prev.map((item) =>
        item.book.id === bookId ? { ...item, selected: !item.selected } : item
      )
    );
  };

  const toggleSelectAll = (selectAll: boolean) => {
    setItems((prev) => prev.map((item) => ({ ...item, selected: selectAll })));
  };

  const selectedItems = useMemo(() => items.filter((i) => i.selected), [items]);

  const subtotal = useMemo(
    () =>
      selectedItems.reduce(
        (sum, entry) => sum + (entry.book.flashSalePrice ?? entry.book.price) * entry.quantity,
        0
      ),
    [selectedItems]
  );

  const shippingFee = useMemo(() => {
    if (selectedItems.length === 0) return 0;
    return subtotal >= 300000 ? 0 : 25000;
  }, [selectedItems.length, subtotal]);

  const discountAmount = useMemo(() => {
    if (!appliedVoucher || selectedItems.length === 0) return 0;
    if (subtotal < appliedVoucher.minOrderValue) return 0;
    if (appliedVoucher.discountType === 'FIXED') {
      return appliedVoucher.discountValue;
    }
    const rawPercent = (subtotal * appliedVoucher.discountValue) / 100;
    return appliedVoucher.maxDiscountAmount
      ? Math.min(rawPercent, appliedVoucher.maxDiscountAmount)
      : rawPercent;
  }, [appliedVoucher, selectedItems.length, subtotal]);

  const finalTotal = useMemo(
    () => Math.max(0, subtotal + shippingFee - discountAmount),
    [subtotal, shippingFee, discountAmount]
  );

  const applyVoucherCode = (code: string): boolean => {
    setVoucherError(null);
    const normalized = code.trim().toUpperCase();
    const found = INITIAL_VOUCHERS.find(
      (v) => v.code.toUpperCase() === normalized && v.isActive
    );
    if (!found) {
      setVoucherError('Mã giảm giá không tồn tại hoặc đã hết hạn.');
      return false;
    }
    setAppliedVoucher(found);
    if (subtotal > 0 && subtotal < found.minOrderValue) {
      setVoucherError(
        `Đơn hàng cần tối thiểu ${found.minOrderValue.toLocaleString('vi-VN')} đ để kích hoạt mã ${found.code}.`
      );
    }
    return true;
  };

  const clearVoucher = () => {
    setAppliedVoucher(null);
    setVoucherError(null);
  };

  const clearSelectedAfterCheckout = () => {
    setItems((prev) => prev.filter((item) => !item.selected));
    setAppliedVoucher(null);
  };

  const totalItemsCount = useMemo(
    () => items.reduce((acc, item) => acc + item.quantity, 0),
    [items]
  );

  return (
    <CartContext.Provider
      value={{
        items,
        appliedVoucher,
        voucherError,
        totalItemsCount,
        selectedItems,
        subtotal,
        shippingFee,
        discountAmount,
        finalTotal,
        addToCart,
        updateQuantity,
        removeFromCart,
        toggleSelect,
        toggleSelectAll,
        applyVoucherCode,
        clearVoucher,
        clearSelectedAfterCheckout,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart(): CartContextValue {
  const ctx = useContext(CartContext);
  if (!ctx) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return ctx;
}

