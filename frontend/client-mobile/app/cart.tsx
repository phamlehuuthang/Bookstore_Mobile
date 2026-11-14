import React, { useState } from 'react';
import {
  Image,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { formatCurrencyVND } from '@shared/types';
import { INITIAL_VOUCHERS } from '@shared/api-client/mockData';
import { colors } from '@/theme/colors';
import { useCart } from '@/hooks/useCart';
import { EmptyStateView } from '@/components/common/StateViews';

export default function CartScreen() {
  const router = useRouter();
  const {
    items,
    selectedItems,
    appliedVoucher,
    voucherError,
    subtotal,
    shippingFee,
    discountAmount,
    finalTotal,
    updateQuantity,
    removeFromCart,
    toggleSelect,
    toggleSelectAll,
    applyVoucherCode,
    clearVoucher,
  } = useCart();

  const [voucherInput, setVoucherInput] = useState(appliedVoucher?.code || '');

  const allSelected = items.length > 0 && items.every((i) => i.selected);

  if (items.length === 0) {
    return (
      <SafeAreaView edges={['bottom']} style={styles.safeArea}>
        <EmptyStateView
          icon="cart-outline"
          title="Giỏ hàng của bạn đang trống"
          description="Hãy khám phá các tựa sách Công nghệ, Kinh tế và Văn học bán chạy nhất hôm nay!"
          actionLabel="Khám phá kho sách ngay"
          onAction={() => router.replace('/(tabs)/catalog')}
        />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView edges={['bottom']} style={styles.safeArea}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={{ flex: 1 }}
      >
        {/* Top Select All Row */}
        <View style={styles.selectAllBar}>
          <Pressable
            onPress={() => toggleSelectAll(!allSelected)}
            style={styles.selectAllBtn}
          >
            <Ionicons
              name={allSelected ? 'checkbox' : 'square-outline'}
              size={22}
              color={colors.primary}
            />
            <Text style={styles.selectAllText}>
              Chọn tất cả ({items.length} đầu sách)
            </Text>
          </Pressable>
          <Text style={styles.freeshipHint}>
            {subtotal >= 300000
              ? '🎉 Đã đủ điều kiện Freeship'
              : `Mua thêm ${formatCurrencyVND(300000 - subtotal)} để Freeship`}
          </Text>
        </View>

        <ScrollView
          contentInsetAdjustmentBehavior="automatic"
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={styles.scrollContent}
        >
          {/* Cart Items */}
          {items.map(({ book, quantity, selected }) => {
            const unitPrice = book.flashSalePrice ?? book.price;
            return (
              <View key={book.id} style={styles.cartCard}>
                <Pressable
                  onPress={() => toggleSelect(book.id)}
                  hitSlop={8}
                  style={styles.checkWrap}
                >
                  <Ionicons
                    name={selected ? 'checkbox' : 'square-outline'}
                    size={22}
                    color={selected ? colors.primary : colors.textMuted}
                  />
                </Pressable>

                <Image source={{ uri: book.coverUrl }} style={styles.cover} />

                <View style={{ flex: 1, gap: 4 }}>
                  <Text style={styles.bookTitle} numberOfLines={2}>
                    {book.title}
                  </Text>
                  <Text style={styles.authorText}>{book.authorName}</Text>
                  <Text style={styles.unitPrice}>{formatCurrencyVND(unitPrice)}</Text>

                  <View style={styles.controlsRow}>
                    <View style={styles.stepper}>
                      <Pressable
                        onPress={() => updateQuantity(book.id, quantity - 1)}
                        style={styles.stepBtn}
                      >
                        <Ionicons name="remove" size={16} color={colors.textPrimary} />
                      </Pressable>
                      <Text style={styles.stepValue}>{quantity}</Text>
                      <Pressable
                        onPress={() => updateQuantity(book.id, quantity + 1)}
                        style={styles.stepBtn}
                      >
                        <Ionicons name="add" size={16} color={colors.textPrimary} />
                      </Pressable>
                    </View>

                    <Pressable
                      onPress={() => removeFromCart(book.id)}
                      hitSlop={8}
                      style={styles.deleteBtn}
                    >
                      <Ionicons name="trash-outline" size={18} color={colors.danger} />
                    </Pressable>
                  </View>
                </View>
              </View>
            );
          })}

          {/* Voucher Input & Quick Selection */}
          <View style={styles.voucherSection}>
            <Text style={styles.sectionTitle}>Mã giảm giá / Voucher</Text>
            <View style={styles.voucherInputRow}>
              <TextInput
                value={voucherInput}
                onChangeText={setVoucherInput}
                placeholder="Nhập mã giảm giá (VD: CHAOHE50K)"
                placeholderTextColor={colors.textMuted}
                autoCapitalize="characters"
                style={styles.voucherInput}
              />
              <Pressable
                onPress={() => applyVoucherCode(voucherInput)}
                style={styles.applyBtn}
              >
                <Text style={styles.applyBtnText}>Áp dụng</Text>
              </Pressable>
            </View>

            {voucherError ? (
              <Text style={styles.errorText}>{voucherError}</Text>
            ) : null}

            {appliedVoucher ? (
              <View style={styles.appliedPill}>
                <Ionicons name="pricetag" size={15} color={colors.primary} />
                <Text style={styles.appliedText}>
                  Đang áp dụng {appliedVoucher.code}: Giảm{' '}
                  {formatCurrencyVND(discountAmount)}
                </Text>
                <Pressable onPress={clearVoucher}>
                  <Ionicons name="close-circle" size={18} color={colors.textSecondary} />
                </Pressable>
              </View>
            ) : null}

            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={{ gap: 8, marginTop: 4 }}
            >
              {INITIAL_VOUCHERS.map((v) => (
                <Pressable
                  key={v.id}
                  onPress={() => {
                    setVoucherInput(v.code);
                    applyVoucherCode(v.code);
                  }}
                  style={styles.quickVoucherChip}
                >
                  <Text style={styles.quickVoucherCode}>{v.code}</Text>
                  <Text style={styles.quickVoucherDesc}>{v.title}</Text>
                </Pressable>
              ))}
            </ScrollView>
          </View>

          {/* Order Summary Breakdown */}
          <View style={styles.summaryCard}>
            <Text style={styles.sectionTitle}>Chi tiết thanh toán</Text>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>
                Tạm tính ({selectedItems.length} sản phẩm đã chọn)
              </Text>
              <Text style={styles.summaryValue}>{formatCurrencyVND(subtotal)}</Text>
            </View>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>Phí vận chuyển tiêu chuẩn</Text>
              <Text style={styles.summaryValue}>
                {shippingFee === 0 ? 'Miễn phí' : formatCurrencyVND(shippingFee)}
              </Text>
            </View>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>Giảm giá Voucher</Text>
              <Text style={[styles.summaryValue, { color: colors.primary }]}>
                -{formatCurrencyVND(discountAmount)}
              </Text>
            </View>
            <View style={[styles.summaryRow, styles.summaryTotalRow]}>
              <Text style={styles.totalLabel}>Tổng thanh toán</Text>
              <Text style={styles.totalValue}>{formatCurrencyVND(finalTotal)}</Text>
            </View>
          </View>
        </ScrollView>

        {/* Bottom Sticky Checkout Bar (Chuẩn Shopee) */}
        <View style={styles.bottomBar}>
          <Pressable
            onPress={() => toggleSelectAll(!allSelected)}
            style={styles.bottomSelectAll}
          >
            <Ionicons
              name={allSelected ? 'checkbox' : 'square-outline'}
              size={20}
              color={colors.primary}
            />
            <Text style={styles.bottomSelectAllText}>Tất cả</Text>
          </Pressable>

          <View style={styles.bottomPriceCol}>
            <View style={styles.bottomTotalRow}>
              <Text style={styles.bottomSub}>Tổng cộng </Text>
              <Text numberOfLines={1} style={styles.bottomTotal}>
                {formatCurrencyVND(finalTotal)}
              </Text>
            </View>
            {discountAmount > 0 ? (
              <Text numberOfLines={1} style={styles.bottomSaved}>
                Tiết kiệm {formatCurrencyVND(discountAmount)}
              </Text>
            ) : null}
          </View>

          <Pressable
            disabled={selectedItems.length === 0}
            onPress={() => router.push('/checkout')}
            style={({ pressed }) => [
              styles.checkoutBtn,
              selectedItems.length === 0 && { opacity: 0.45 },
              pressed && { transform: [{ scale: 0.98 }] },
            ]}
          >
            <Text style={styles.checkoutBtnText}>
              Mua hàng ({selectedItems.length})
            </Text>
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.background,
  },
  selectAllBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 12,
    paddingVertical: 9,
    backgroundColor: colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  selectAllBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  selectAllText: {
    fontSize: 12.5,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  freeshipHint: {
    fontSize: 11,
    fontWeight: '600',
    color: colors.primary,
  },
  scrollContent: {
    padding: 12,
    paddingBottom: 96,
    gap: 10,
  },
  cartCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    padding: 10,
    borderRadius: 10,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
  },
  checkWrap: {
    padding: 2,
  },
  cover: {
    width: 56,
    height: 76,
    borderRadius: 6,
    backgroundColor: colors.surfaceMuted,
  },
  bookTitle: {
    fontSize: 12.5,
    fontWeight: '600',
    color: colors.textPrimary,
    lineHeight: 17,
  },
  authorText: {
    fontSize: 11,
    color: colors.textSecondary,
  },
  unitPrice: {
    fontSize: 13.5,
    fontWeight: '700',
    color: colors.accent,
  },
  controlsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 2,
  },
  stepper: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 6,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.background,
  },
  stepBtn: {
    width: 26,
    height: 26,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepValue: {
    minWidth: 24,
    textAlign: 'center',
    fontSize: 12,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  deleteBtn: {
    padding: 4,
  },
  voucherSection: {
    padding: 12,
    borderRadius: 10,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    gap: 8,
  },
  sectionTitle: {
    fontSize: 13.5,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  voucherInputRow: {
    flexDirection: 'row',
    gap: 8,
  },
  voucherInput: {
    flex: 1,
    minHeight: 38,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.background,
    paddingHorizontal: 10,
    fontSize: 12,
    color: colors.textPrimary,
  },
  applyBtn: {
    minHeight: 38,
    paddingHorizontal: 14,
    borderRadius: 8,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  applyBtnText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '600',
  },
  errorText: {
    fontSize: 11.5,
    color: colors.danger,
    fontWeight: '600',
  },
  appliedPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    padding: 8,
    borderRadius: 8,
    backgroundColor: colors.primarySoft,
    borderWidth: 1,
    borderColor: colors.primaryBorder,
  },
  appliedText: {
    flex: 1,
    fontSize: 11.5,
    fontWeight: '600',
    color: colors.primaryDark,
  },
  quickVoucherChip: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.background,
  },
  quickVoucherCode: {
    fontSize: 11.5,
    fontWeight: '700',
    color: colors.primary,
  },
  quickVoucherDesc: {
    fontSize: 10.5,
    color: colors.textSecondary,
  },
  summaryCard: {
    padding: 12,
    borderRadius: 10,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    gap: 8,
  },
  summaryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  summaryLabel: {
    fontSize: 12,
    color: colors.textSecondary,
  },
  summaryValue: {
    fontSize: 12.5,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  summaryTotalRow: {
    paddingTop: 8,
    borderTopWidth: 1,
    borderTopColor: colors.borderSubtle,
  },
  totalLabel: {
    fontSize: 13.5,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  totalValue: {
    fontSize: 15,
    fontWeight: '700',
    color: colors.primary,
  },
  bottomBar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingHorizontal: 12,
    paddingVertical: 8,
    backgroundColor: colors.surface,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  bottomSelectAll: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
  },
  bottomSelectAllText: {
    fontSize: 12,
    color: colors.textSecondary,
  },
  bottomPriceCol: {
    flex: 1,
    alignItems: 'flex-end',
    justifyContent: 'center',
  },
  bottomTotalRow: {
    flexDirection: 'row',
    alignItems: 'baseline',
  },
  bottomSub: {
    fontSize: 11.5,
    color: colors.textSecondary,
  },
  bottomTotal: {
    fontSize: 15,
    fontWeight: '700',
    color: colors.primary,
  },
  bottomSaved: {
    fontSize: 10.5,
    fontWeight: '600',
    color: colors.primary,
    marginTop: 1,
  },
  checkoutBtn: {
    height: 40,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 16,
    borderRadius: 8,
    backgroundColor: colors.primary,
  },
  checkoutBtnText: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '600',
  },
});

