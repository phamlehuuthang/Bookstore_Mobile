import React, { useState } from 'react';
import {
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
import {
  Order,
  PAYMENT_METHOD_LABELS,
  PaymentMethod,
  formatCurrencyVND,
} from '@shared/types';
import { colors } from '@/theme/colors';
import { useCart } from '@/hooks/useCart';
import { useAuth } from '@/hooks/useAuth';
import { orderService } from '@/services/orderService';

export default function CheckoutScreen() {
  const router = useRouter();
  const { user } = useAuth();
  const {
    selectedItems,
    appliedVoucher,
    subtotal,
    shippingFee,
    discountAmount,
    finalTotal,
    clearSelectedAfterCheckout,
  } = useCart();

  const addresses = user?.addresses || [];
  const [selectedAddressId, setSelectedAddressId] = useState<string>(
    addresses.find((a) => a.isDefault)?.id || addresses[0]?.id || ''
  );
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('COD');
  const [orderNote, setOrderNote] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [createdOrder, setCreatedOrder] = useState<Order | null>(null);

  const chosenAddress =
    addresses.find((a) => a.id === selectedAddressId) || {
      id: 'addr-default',
      fullName: user?.fullName || 'Nguyễn Minh Tuấn',
      phone: user?.phone || '0908123456',
      province: 'TP. Hồ Chí Minh',
      district: 'Quận 1',
      ward: 'Phường Bến Nghé',
      streetAddress: 'Tòa nhà Bitexco, Số 2 Hải Triều',
      isDefault: true,
    };

  const handlePlaceOrder = async () => {
    if (selectedItems.length === 0 || isSubmitting) return;
    setIsSubmitting(true);
    try {
      const newOrder = await orderService.createOrder({
        userId: user?.id || 'user-cust-1',
        customerName: chosenAddress.fullName,
        customerPhone: chosenAddress.phone,
        shippingAddress: chosenAddress,
        items: selectedItems.map((entry) => ({
          bookId: entry.book.id,
          title: entry.book.title,
          coverUrl: entry.book.coverUrl,
          authorName: entry.book.authorName,
          unitPrice: entry.book.flashSalePrice ?? entry.book.price,
          quantity: entry.quantity,
        })),
        subtotal,
        shippingFee,
        discountAmount,
        voucherCode: appliedVoucher?.code,
        paymentMethod,
        note: orderNote.trim() || undefined,
      });
      clearSelectedAfterCheckout();
      setCreatedOrder(newOrder);
    } finally {
      setIsSubmitting(false);
    }
  };

  // ORDER SUCCESS CELEBRATION SCREEN
  if (createdOrder) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.successContainer}>
          <View style={styles.successCircle}>
            <Ionicons name="checkmark-done" size={44} color="#FFFFFF" />
          </View>
          <Text style={styles.successTitle}>Đặt hàng thành công!</Text>
          <Text selectable style={styles.successCode}>
            Mã đơn hàng: {createdOrder.orderCode}
          </Text>
          <Text style={styles.successDesc}>
            Cảm ơn {createdOrder.customerName}! Kiện sách của bạn đang được kho đóng gói
            chống sốc 3 lớp và sẽ sớm giao tới {createdOrder.shippingAddress.district},{' '}
            {createdOrder.shippingAddress.province}.
          </Text>

          <View style={styles.successSummaryBox}>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>Tổng thanh toán</Text>
              <Text style={styles.summaryHighlight}>
                {formatCurrencyVND(createdOrder.totalAmount)}
              </Text>
            </View>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>Hình thức thanh toán</Text>
              <Text style={styles.summaryValue}>
                {PAYMENT_METHOD_LABELS[createdOrder.paymentMethod]}
              </Text>
            </View>
          </View>

          <View style={styles.successActions}>
            <Pressable
              onPress={() => router.replace('/(tabs)/orders')}
              style={styles.primaryBtn}
            >
              <Text style={styles.primaryBtnText}>Theo dõi đơn hàng</Text>
            </Pressable>
            <Pressable
              onPress={() => router.replace('/(tabs)')}
              style={styles.outlineBtn}
            >
              <Text style={styles.outlineBtnText}>Tiếp tục mua sắm</Text>
            </Pressable>
          </View>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView edges={['bottom']} style={styles.safeArea}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={{ flex: 1 }}
      >
        <ScrollView
          contentInsetAdjustmentBehavior="automatic"
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={styles.scrollContent}
        >
          {/* 1. Delivery Address Selection */}
          <View style={styles.card}>
            <View style={styles.cardHeader}>
              <Ionicons name="location" size={18} color={colors.primary} />
              <Text style={styles.cardTitle}>Địa chỉ nhận hàng</Text>
            </View>

            {addresses.map((addr) => {
              const active = addr.id === chosenAddress.id;
              return (
                <Pressable
                  key={addr.id}
                  onPress={() => setSelectedAddressId(addr.id)}
                  style={[
                    styles.addressOption,
                    active && styles.addressOptionActive,
                  ]}
                >
                  <Ionicons
                    name={active ? 'radio-button-on' : 'radio-button-off'}
                    size={20}
                    color={active ? colors.primary : colors.textMuted}
                  />
                  <View style={{ flex: 1 }}>
                    <Text style={styles.addrName}>
                      {addr.fullName} • {addr.phone}
                    </Text>
                    <Text style={styles.addrStreet}>
                      {addr.streetAddress}, {addr.ward}, {addr.district}, {addr.province}
                    </Text>
                  </View>
                </Pressable>
              );
            })}
          </View>

          {/* 2. Payment Method Selection */}
          <View style={styles.card}>
            <View style={styles.cardHeader}>
              <Ionicons name="card-outline" size={18} color={colors.primary} />
              <Text style={styles.cardTitle}>Phương thức thanh toán</Text>
            </View>

            {(
              [
                {
                  key: 'COD',
                  title: 'Thanh toán khi nhận hàng (COD)',
                  sub: 'Kiểm tra sách nguyên seal trước khi thanh toán tiền mặt',
                  icon: 'cash-outline',
                },
                {
                  key: 'BANK_QR',
                  title: 'Chuyển khoản ngân hàng (Mã VietQR)',
                  sub: 'Vietcombank • Tự động xác nhận trong 5 giây',
                  icon: 'qr-code-outline',
                },
                {
                  key: 'EWALLET',
                  title: 'Ví điện tử MoMo / ZaloPay',
                  sub: 'Liên kết ví an toàn, hoàn tiền ưu đãi',
                  icon: 'wallet-outline',
                },
              ] as const
            ).map((pm) => {
              const active = paymentMethod === pm.key;
              return (
                <Pressable
                  key={pm.key}
                  onPress={() => setPaymentMethod(pm.key)}
                  style={[
                    styles.addressOption,
                    active && styles.addressOptionActive,
                  ]}
                >
                  <Ionicons
                    name={active ? 'radio-button-on' : 'radio-button-off'}
                    size={20}
                    color={active ? colors.primary : colors.textMuted}
                  />
                  <Ionicons name={pm.icon} size={20} color={colors.textPrimary} />
                  <View style={{ flex: 1 }}>
                    <Text style={styles.addrName}>{pm.title}</Text>
                    <Text style={styles.addrStreet}>{pm.sub}</Text>
                  </View>
                </Pressable>
              );
            })}

            {paymentMethod === 'BANK_QR' ? (
              <View style={styles.qrPreviewBox}>
                <Ionicons name="qr-code" size={56} color={colors.primary} />
                <View style={{ flex: 1, gap: 2 }}>
                  <Text style={styles.addrName}>
                    Vietcombank • 0071001234567
                  </Text>
                  <Text style={styles.addrStreet}>
                    Chủ TK: CONG TY CP SACH TRI THUC VIET
                  </Text>
                  <Text style={{ fontSize: 12, fontWeight: '700', color: colors.primary }}>
                    Số tiền: {formatCurrencyVND(finalTotal)}
                  </Text>
                </View>
              </View>
            ) : null}
          </View>

          {/* 3. Order Note */}
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Ghi chú cho đơn vị vận chuyển</Text>
            <TextInput
              value={orderNote}
              onChangeText={setOrderNote}
              placeholder="Ví dụ: Giao giờ hành chính, gọi trước khi giao 15 phút..."
              placeholderTextColor={colors.textMuted}
              multiline
              style={styles.noteInput}
            />
          </View>

          {/* 4. Final Cost Summary */}
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Tóm tắt đơn hàng</Text>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>
                Tiền sách ({selectedItems.length} cuốn)
              </Text>
              <Text style={styles.summaryValue}>{formatCurrencyVND(subtotal)}</Text>
            </View>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>Phí giao hàng</Text>
              <Text style={styles.summaryValue}>
                {shippingFee === 0 ? 'Miễn phí' : formatCurrencyVND(shippingFee)}
              </Text>
            </View>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>Voucher giảm giá</Text>
              <Text style={[styles.summaryValue, { color: colors.primary }]}>
                -{formatCurrencyVND(discountAmount)}
              </Text>
            </View>
            <View style={[styles.summaryRow, styles.totalDivider]}>
              <Text style={styles.cardTitle}>Tổng thanh toán</Text>
              <Text style={styles.summaryHighlight}>{formatCurrencyVND(finalTotal)}</Text>
            </View>
          </View>
        </ScrollView>

        {/* Bottom Confirm Order Bar */}
        <View style={styles.bottomBar}>
          <View style={{ flex: 1 }}>
            <Text style={styles.summaryLabel}>Tổng thanh toán</Text>
            <Text style={styles.summaryHighlight}>{formatCurrencyVND(finalTotal)}</Text>
          </View>
          <Pressable
            onPress={handlePlaceOrder}
            disabled={isSubmitting}
            style={({ pressed }) => [
              styles.confirmBtn,
              pressed && { opacity: 0.9, transform: [{ scale: 0.98 }] },
            ]}
          >
            <Text style={styles.confirmBtnText}>
              {isSubmitting ? 'Đang xử lý...' : 'Xác nhận đặt hàng'}
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
  scrollContent: {
    padding: 16,
    paddingBottom: 110,
    gap: 14,
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: 14,
    padding: 16,
    borderWidth: 1,
    borderColor: colors.border,
    gap: 10,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  cardTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  addressOption: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    padding: 12,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.background,
  },
  addressOptionActive: {
    borderColor: colors.primary,
    backgroundColor: colors.primarySoft,
  },
  addrName: {
    fontSize: 13,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  addrStreet: {
    fontSize: 12,
    color: colors.textSecondary,
    marginTop: 2,
  },
  qrPreviewBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 12,
    borderRadius: 12,
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.primaryBorder,
  },
  noteInput: {
    minHeight: 72,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.background,
    padding: 12,
    fontSize: 13,
    color: colors.textPrimary,
    textAlignVertical: 'top',
  },
  summaryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  summaryLabel: {
    fontSize: 13,
    color: colors.textSecondary,
  },
  summaryValue: {
    fontSize: 13,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  summaryHighlight: {
    fontSize: 19,
    fontWeight: '800',
    color: colors.primary,
  },
  totalDivider: {
    paddingTop: 10,
    borderTopWidth: 1,
    borderTopColor: colors.borderSubtle,
  },
  bottomBar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: colors.surface,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  confirmBtn: {
    minHeight: 46,
    paddingHorizontal: 22,
    borderRadius: 12,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  confirmBtnText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '700',
  },
  successContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
    gap: 12,
  },
  successCircle: {
    width: 84,
    height: 84,
    borderRadius: 42,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  successTitle: {
    fontSize: 22,
    fontWeight: '800',
    color: colors.textPrimary,
  },
  successCode: {
    fontSize: 15,
    fontWeight: '700',
    color: colors.primary,
  },
  successDesc: {
    fontSize: 14,
    lineHeight: 21,
    color: colors.textSecondary,
    textAlign: 'center',
  },
  successSummaryBox: {
    width: '100%',
    padding: 16,
    borderRadius: 14,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    gap: 8,
    marginVertical: 8,
  },
  successActions: {
    width: '100%',
    gap: 10,
  },
  primaryBtn: {
    minHeight: 46,
    borderRadius: 12,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  primaryBtnText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '700',
  },
  outlineBtn: {
    minHeight: 46,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  outlineBtnText: {
    fontSize: 15,
    fontWeight: '700',
    color: colors.textPrimary,
  },
});

