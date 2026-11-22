import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { Book } from '@shared/types';
import { useAuth } from '@/hooks/useAuth';
import {
  ActiveVoucherItem,
  CartEntry,
  CartSnapshot,
  cartWishlistVoucherService,
} from '@/services/cartWishlistVoucherService';

export type { CartEntry };

interface CartContextValue {
  items: CartEntry[];
  activeVouchers: ActiveVoucherItem[];
  appliedVoucher: ActiveVoucherItem | null;
  voucherError: string | null;
  cartError: string | null;
  isLoadingCart: boolean;
  totalItemsCount: number;
  selectedItems: CartEntry[];
  subtotal: number;
  shippingFee: number;
  discountAmount: number;
  finalTotal: number;
  refreshCart: () => Promise<void>;
  clearCartError: () => void;
  addToCart: (book: Book, quantity?: number) => Promise<boolean>;
  updateQuantity: (bookId: string, quantity: number) => Promise<void>;
  removeFromCart: (bookId: string) => Promise<void>;
  toggleSelect: (bookId: string) => Promise<void>;
  toggleSelectAll: (selectAll: boolean) => Promise<void>;
  applyVoucherCode: (code: string) => Promise<boolean>;
  clearVoucher: () => void;
  clearSelectedAfterCheckout: () => void;
}

const CartContext = createContext<CartContextValue | undefined>(undefined);

/**
 * CartProvider kết nối Giỏ hàng & Voucher thật qua Backend (Bước 4 - Phần 2):
 * - Khách chưa đăng nhập -> giỏ hàng luôn trống (`items = []`), không tự nạp hàng giả nữa.
 * - Khi đăng nhập -> tự tải giỏ hàng của chính khách hàng từ PostgreSQL.
 * - Mọi thao tác thêm/sửa/xóa/tick chọn đều chạy qua API và kiểm tra tồn kho ở Backend.
 */
export function CartProvider({ children }: { children: React.ReactNode }) {
  const { user, isAuthenticated } = useAuth();

  const [items, setItems] = useState<CartEntry[]>([]);
  const [totalItemsCount, setTotalItemsCount] = useState(0);
  const [subtotal, setSubtotal] = useState(0);
  const [shippingFee, setShippingFee] = useState(0);

  const [activeVouchers, setActiveVouchers] = useState<ActiveVoucherItem[]>([]);
  const [appliedVoucher, setAppliedVoucher] = useState<ActiveVoucherItem | null>(null);
  const [discountAmount, setDiscountAmount] = useState(0);
  const [voucherError, setVoucherError] = useState<string | null>(null);
  const [cartError, setCartError] = useState<string | null>(null);
  const [isLoadingCart, setIsLoadingCart] = useState(false);

  // Tải danh sách Voucher đang mở công khai (permitAll) ngay khi app khởi động
  useEffect(() => {
    let active = true;
    cartWishlistVoucherService
      .getActiveVouchers()
      .then((list) => {
        if (active) {
          setActiveVouchers(list);
        }
      })
      .catch(() => {
        if (active) {
          setActiveVouchers([]);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const applyCartSnapshot = useCallback((snapshot: CartSnapshot) => {
    setItems(snapshot.entries);
    setTotalItemsCount(snapshot.totalItemsCount);
    setSubtotal(snapshot.subtotal);
    setShippingFee(snapshot.shippingFee);
  }, []);

  // Mỗi khi thay đổi trạng thái giỏ hàng, nếu đang áp voucher thì kiểm tra lại điều kiện voucher ở Backend
  const syncSnapshotAndRevalidateVoucher = useCallback(
    async (snapshot: CartSnapshot, currentVoucher: ActiveVoucherItem | null) => {
      applyCartSnapshot(snapshot);
      if (!currentVoucher) {
        return;
      }

      const hasSelected = snapshot.entries.some((entry) => entry.selected);
      if (!hasSelected || snapshot.subtotal <= 0) {
        setDiscountAmount(0);
        setVoucherError(null);
        return;
      }

      try {
        const validated = await cartWishlistVoucherService.validateVoucher(currentVoucher.code);
        setAppliedVoucher(validated.voucher);
        setDiscountAmount(validated.discountAmount);
        setVoucherError(null);
      } catch (err) {
        setDiscountAmount(0);
        setVoucherError(
          err instanceof Error
            ? err.message
            : 'Giỏ hàng vừa thay đổi nên mã giảm giá tạm chưa đủ điều kiện.'
        );
      }
    },
    [applyCartSnapshot]
  );

  const refreshCart = useCallback(async () => {
    if (!isAuthenticated) {
      setItems([]);
      setTotalItemsCount(0);
      setSubtotal(0);
      setShippingFee(0);
      setAppliedVoucher(null);
      setDiscountAmount(0);
      setVoucherError(null);
      setCartError(null);
      return;
    }

    setIsLoadingCart(true);
    try {
      const snapshot = await cartWishlistVoucherService.getCart();
      applyCartSnapshot(snapshot);
      setCartError(null);
    } catch (err) {
      setCartError(err instanceof Error ? err.message : 'Không thể tải giỏ hàng.');
    } finally {
      setIsLoadingCart(false);
    }
  }, [applyCartSnapshot, isAuthenticated]);

  // Đồng bộ giỏ hàng mỗi khi đăng nhập / khôi phục phiên / đăng xuất
  useEffect(() => {
    if (!isAuthenticated) {
      setItems([]);
      setTotalItemsCount(0);
      setSubtotal(0);
      setShippingFee(0);
      setAppliedVoucher(null);
      setDiscountAmount(0);
      setVoucherError(null);
      setCartError(null);
      setIsLoadingCart(false);
      return;
    }
    refreshCart();
  }, [isAuthenticated, user?.id, refreshCart]);

  const clearCartError = () => setCartError(null);

  const addToCart = async (book: Book, quantity = 1): Promise<boolean> => {
    if (!isAuthenticated) {
      return false;
    }
    setCartError(null);
    try {
      const snapshot = await cartWishlistVoucherService.addToCart(book.id, quantity);
      await syncSnapshotAndRevalidateVoucher(snapshot, appliedVoucher);
      return true;
    } catch (err) {
      setCartError(err instanceof Error ? err.message : 'Không thể thêm sách vào giỏ hàng.');
      return false;
    }
  };

  const updateQuantity = async (bookId: string, quantity: number) => {
    if (!isAuthenticated) return;
    setCartError(null);
    const clampedQty = Math.max(1, quantity);
    try {
      const snapshot = await cartWishlistVoucherService.updateCartItem(bookId, {
        soLuong: clampedQty,
      });
      await syncSnapshotAndRevalidateVoucher(snapshot, appliedVoucher);
    } catch (err) {
      setCartError(err instanceof Error ? err.message : 'Không thể cập nhật số lượng.');
    }
  };

  const removeFromCart = async (bookId: string) => {
    if (!isAuthenticated) return;
    setCartError(null);
    try {
      const snapshot = await cartWishlistVoucherService.removeCartItem(bookId);
      await syncSnapshotAndRevalidateVoucher(snapshot, appliedVoucher);
    } catch (err) {
      setCartError(err instanceof Error ? err.message : 'Không thể xóa sách khỏi giỏ hàng.');
    }
  };

  const toggleSelect = async (bookId: string) => {
    if (!isAuthenticated) return;
    const target = items.find((entry) => entry.book.id === bookId);
    if (!target) return;
    setCartError(null);
    try {
      const snapshot = await cartWishlistVoucherService.updateCartItem(bookId, {
        daChon: !target.selected,
      });
      await syncSnapshotAndRevalidateVoucher(snapshot, appliedVoucher);
    } catch (err) {
      setCartError(err instanceof Error ? err.message : 'Không thể cập nhật trạng thái chọn.');
    }
  };

  const toggleSelectAll = async (selectAll: boolean) => {
    if (!isAuthenticated) return;
    setCartError(null);
    try {
      const snapshot = await cartWishlistVoucherService.selectAllCartItems(selectAll);
      await syncSnapshotAndRevalidateVoucher(snapshot, appliedVoucher);
    } catch (err) {
      setCartError(err instanceof Error ? err.message : 'Không thể cập nhật chọn tất cả.');
    }
  };

  const applyVoucherCode = async (code: string): Promise<boolean> => {
    if (!isAuthenticated) {
      setVoucherError('Vui lòng đăng nhập để kiểm tra và áp dụng mã giảm giá.');
      return false;
    }
    const trimmed = code.trim();
    if (!trimmed) {
      setVoucherError('Vui lòng nhập mã giảm giá.');
      return false;
    }

    setVoucherError(null);
    try {
      const res = await cartWishlistVoucherService.validateVoucher(trimmed);
      setAppliedVoucher(res.voucher);
      setDiscountAmount(res.discountAmount);
      return true;
    } catch (err) {
      setAppliedVoucher(null);
      setDiscountAmount(0);
      setVoucherError(err instanceof Error ? err.message : 'Mã giảm giá chưa hợp lệ.');
      return false;
    }
  };

  const clearVoucher = () => {
    setAppliedVoucher(null);
    setDiscountAmount(0);
    setVoucherError(null);
  };

  // Khi nối Bước 6 đặt hàng, checkout vẫn tạm gọi hàm này để dọn các món đã chọn ở client
  const clearSelectedAfterCheckout = () => {
    setItems((prev) => prev.filter((item) => !item.selected));
    setAppliedVoucher(null);
    setDiscountAmount(0);
    setVoucherError(null);
  };

  const selectedItems = useMemo(() => items.filter((i) => i.selected), [items]);

  const finalTotal = useMemo(
    () => Math.max(0, subtotal + shippingFee - discountAmount),
    [subtotal, shippingFee, discountAmount]
  );

  return (
    <CartContext.Provider
      value={{
        items,
        activeVouchers,
        appliedVoucher,
        voucherError,
        cartError,
        isLoadingCart,
        totalItemsCount,
        selectedItems,
        subtotal,
        shippingFee,
        discountAmount,
        finalTotal,
        refreshCart,
        clearCartError,
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
