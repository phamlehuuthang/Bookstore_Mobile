import { formatCurrencyVND } from '@shared/types';
import { ActiveVoucherItem, CartEntry } from '@/services/cartWishlistVoucherService';

export interface VoucherEvaluation {
  isEligible: boolean;
  eligibleSubtotal: number;
  estimatedSaving: number;
  scopeLabel: string;
  minOrderLabel: string;
  missingReason: string | null;
}

/**
 * Đánh giá điều kiện áp dụng Voucher phía Client (khớp quy tắc VoucherService ở Backend)
 * để hiển thị rõ ràng theo chuẩn sàn TMĐT (Shopee / Tiki / Fahasa):
 * - Kiểm tra phạm vi danh mục (nếu mã chỉ áp dụng cho danh mục cụ thể).
 * - Kiểm tra giá trị đơn tối thiểu trên phần tiền hàng đủ điều kiện.
 * - Tính số tiền tiết kiệm dự kiến hoặc lý do chưa đủ điều kiện (thiếu bao nhiêu tiền / sai danh mục).
 */
export function evaluateVoucher(
  voucher: ActiveVoucherItem,
  selectedItems: CartEntry[],
  subtotal: number
): VoucherEvaluation {
  const scopeLabel = voucher.categoryName
    ? `Riêng ${voucher.categoryName}`
    : 'Toàn cửa hàng';

  const minOrderLabel =
    voucher.minOrderValue > 0
      ? `Đơn từ ${formatCurrencyVND(voucher.minOrderValue)}`
      : 'Mọi đơn hàng';

  if (selectedItems.length === 0) {
    return {
      isEligible: false,
      eligibleSubtotal: 0,
      estimatedSaving: 0,
      scopeLabel,
      minOrderLabel,
      missingReason: 'Chọn sách trong giỏ để dùng mã',
    };
  }

  let eligibleSubtotal = subtotal;
  if (voucher.categoryId) {
    const matchingTotal = selectedItems.reduce((sum, entry) => {
      const matchesCat =
        entry.book.categoryId === voucher.categoryId ||
        (voucher.categoryName &&
          entry.book.categoryName?.toLowerCase() === voucher.categoryName.toLowerCase());
      if (!matchesCat) return sum;
      const unitPrice = entry.book.flashSalePrice ?? entry.book.price;
      return sum + unitPrice * entry.quantity;
    }, 0);

    if (matchingTotal <= 0) {
      return {
        isEligible: false,
        eligibleSubtotal: 0,
        estimatedSaving: 0,
        scopeLabel,
        minOrderLabel,
        missingReason: `Cần sách danh mục ${voucher.categoryName || 'áp dụng'}`,
      };
    }
    eligibleSubtotal = matchingTotal;
  }

  if (eligibleSubtotal < voucher.minOrderValue) {
    const missing = voucher.minOrderValue - eligibleSubtotal;
    return {
      isEligible: false,
      eligibleSubtotal,
      estimatedSaving: 0,
      scopeLabel,
      minOrderLabel,
      missingReason: voucher.categoryName
        ? `Mua thêm ${formatCurrencyVND(missing)} sách ${voucher.categoryName}`
        : `Mua thêm ${formatCurrencyVND(missing)}`,
    };
  }

  const rawDiscount =
    voucher.discountType === 'FIXED'
      ? voucher.discountValue
      : Math.round((eligibleSubtotal * voucher.discountValue) / 100);
  const capped =
    voucher.discountType === 'PERCENTAGE' && voucher.maxDiscountAmount
      ? Math.min(rawDiscount, voucher.maxDiscountAmount)
      : rawDiscount;
  const estimatedSaving = Math.min(capped, eligibleSubtotal);

  return {
    isEligible: true,
    eligibleSubtotal,
    estimatedSaving,
    scopeLabel,
    minOrderLabel,
    missingReason: null,
  };
}
