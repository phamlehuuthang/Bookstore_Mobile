import React, { useState } from 'react';
import {
  Image,
  KeyboardAvoidingView,
  Modal,
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
  Voucher,
  formatCurrencyVND,
} from '@shared/types';
import { INITIAL_VOUCHERS } from '@shared/api-client/mockData';
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
    voucherError,
    subtotal,
    shippingFee,
    discountAmount,
    finalTotal,
    updateQuantity,
    applyVoucherCode,
    clearVoucher,
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

  // State cho Cửa sổ chọn Voucher (Modal)
  const [voucherModalVisible, setVoucherModalVisible] = useState(false);
  const [voucherInput, setVoucherInput] = useState(appliedVoucher?.code || '');

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

  // Tính số tiền giảm dự kiến của từng Voucher đối với đơn hiện tại
  const calcEstimatedSaving = (v: Voucher): number => {
    if (subtotal < v.minOrderValue) return 0;
    if (v.discountType === 'FIXED') return v.discountValue;
    const raw = (subtotal * v.discountValue) / 100;
    return v.maxDiscountAmount ? Math.min(raw, v.maxDiscountAmount) : raw;
  };

  const handleSelectVoucher = (v: Voucher) => {
    setVoucherInput(v.code);
    const ok = applyVoucherCode(v.code);
    if (ok) {
      setVoucherModalVisible(false);
    }
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
        voucherCode: discountAmount > 0 ? appliedVoucher?.code : undefined,
        paymentMethod,
        note: orderNote.trim() || undefined,
      });
      clearSelectedAfterCheckout();
      setCreatedOrder(newOrder);
    } finally {
      setIsSubmitting(false);
    }
  };

  // MÀN HÌNH ĐẶT HÀNG THÀNH CÔNG
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
            {createdOrder.discountAmount > 0 ? (
              <View style={styles.summaryRow}>
                <Text style={styles.summaryLabel}>Đã tiết kiệm (Voucher)</Text>
                <Text style={[styles.summaryValue, { color: colors.primary }]}>
                  -{formatCurrencyVND(createdOrder.discountAmount)} ({createdOrder.voucherCode})
                </Text>
              </View>
            ) : null}
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
          {/* 1. ĐỊA CHỈ NHẬN HÀNG */}
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

          {/* 2. SẢN PHẨM ĐẶT MUA (Có chỉnh số lượng trực tiếp như Shopee/Tiki) */}
          <View style={styles.card}>
            <View style={styles.cardHeader}>
              <Ionicons name="book-outline" size={18} color={colors.primary} />
              <Text style={styles.cardTitle}>
                Sản phẩm đặt mua ({selectedItems.reduce((s, i) => s + i.quantity, 0)} cuốn)
              </Text>
            </View>

            {selectedItems.map(({ book, quantity }) => {
              const unitPrice = book.flashSalePrice ?? book.price;
              return (
                <View key={book.id} style={styles.orderItemRow}>
                  <Image source={{ uri: book.coverUrl }} style={styles.itemCover} />
                  <View style={{ flex: 1, gap: 3 }}>
                    <Text style={styles.itemTitle} numberOfLines={2}>
                      {book.title}
                    </Text>
                    <Text style={styles.itemAuthor}>{book.authorName}</Text>
                    <View style={styles.itemPriceRow}>
                      <Text style={styles.itemPrice}>{formatCurrencyVND(unitPrice)}</Text>
                      <View style={styles.qtyStepper}>
                        <Pressable
                          onPress={() => updateQuantity(book.id, Math.max(1, quantity - 1))}
                          style={styles.qtyBtn}
                        >
                          <Ionicons name="remove" size={14} color={colors.textPrimary} />
                        </Pressable>
                        <Text style={styles.qtyText}>{quantity}</Text>
                        <Pressable
                          onPress={() => updateQuantity(book.id, Math.min(book.stock, quantity + 1))}
                          style={styles.qtyBtn}
                        >
                          <Ionicons name="add" size={14} color={colors.textPrimary} />
                        </Pressable>
                      </View>
                    </View>
                  </View>
                </View>
              );
            })}
          </View>

          {/* 3. CHỌN HOẶC NHẬP VOUCHER GIẢM GIÁ (Thiết kế chuẩn App TMĐT thực tế) */}
          <View style={styles.card}>
            <Pressable
              onPress={() => setVoucherModalVisible(true)}
              style={styles.voucherHeaderRow}
            >
              <View style={styles.cardHeader}>
                <View style={styles.voucherIconBadge}>
                  <Ionicons name="ticket" size={16} color="#FFFFFF" />
                </View>
                <View>
                  <Text style={styles.cardTitle}>Tri Thức Việt Voucher</Text>
                  <Text style={styles.voucherSubHint}>
                    Chọn mã giảm giá hoặc trợ giá vận chuyển
                  </Text>
                </View>
              </View>

              <View style={styles.voucherRightAction}>
                {appliedVoucher && discountAmount > 0 ? (
                  <View style={styles.activeDiscountTag}>
                    <Text style={styles.activeDiscountTagText}>
                      -{formatCurrencyVND(discountAmount)}
                    </Text>
                  </View>
                ) : (
                  <Text style={styles.selectVoucherPrompt}>Chọn hoặc nhập mã</Text>
                )}
                <Ionicons name="chevron-forward" size={18} color={colors.textSecondary} />
              </View>
            </Pressable>

            {/* Hiển thị trạng thái Voucher đang chọn */}
            {appliedVoucher && discountAmount > 0 ? (
              <View style={styles.appliedBanner}>
                <Ionicons name="checkmark-circle" size={18} color={colors.primary} />
                <View style={{ flex: 1 }}>
                  <Text style={styles.appliedBannerTitle}>
                    Đang áp dụng mã {appliedVoucher.code} (-{formatCurrencyVND(discountAmount)})
                  </Text>
                  <Text style={styles.appliedBannerDesc}>{appliedVoucher.title}</Text>
                </View>
                <Pressable onPress={clearVoucher} hitSlop={8}>
                  <Ionicons name="close-circle" size={20} color={colors.textSecondary} />
                </Pressable>
              </View>
            ) : appliedVoucher && subtotal < appliedVoucher.minOrderValue ? (
              <View style={styles.warningVoucherBanner}>
                <Ionicons name="information-circle" size={18} color={colors.accent} />
                <Text style={styles.warningVoucherText}>
                  Mua thêm {formatCurrencyVND(appliedVoucher.minOrderValue - subtotal)} để dùng mã{' '}
                  {appliedVoucher.code} (hoặc chạm vào các mã đủ điều kiện bên dưới)
                </Text>
              </View>
            ) : null}

            {/* Dải Thẻ Voucher Chọn Nhanh 1-Chạm */}
            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.quickTicketScroll}
            >
              {INITIAL_VOUCHERS.map((v) => {
                const isEligible = subtotal >= v.minOrderValue;
                const isSelected = appliedVoucher?.code === v.code && discountAmount > 0;
                return (
                  <Pressable
                    key={v.id}
                    onPress={() => {
                      if (isSelected) {
                        clearVoucher();
                      } else if (isEligible) {
                        applyVoucherCode(v.code);
                      } else {
                        setVoucherModalVisible(true);
                      }
                    }}
                    style={[
                      styles.miniTicket,
                      isSelected && styles.miniTicketSelected,
                      !isEligible && styles.miniTicketDisabled,
                    ]}
                  >
                    <View style={styles.miniTicketTop}>
                      <Text
                        style={[
                          styles.miniTicketCode,
                          isSelected && { color: colors.primary },
                        ]}
                      >
                        {v.code}
                      </Text>
                      <View
                        style={[
                          styles.miniTicketStatusPill,
                          isSelected
                            ? { backgroundColor: colors.primary }
                            : isEligible
                            ? { backgroundColor: colors.accent }
                            : { backgroundColor: colors.border },
                        ]}
                      >
                        <Text
                          style={[
                            styles.miniTicketStatusText,
                            !isEligible && { color: colors.textSecondary },
                          ]}
                        >
                          {isSelected ? 'Đang dùng' : isEligible ? 'Dùng ngay' : 'Chưa đủ ĐK'}
                        </Text>
                      </View>
                    </View>
                    <Text style={styles.miniTicketTitle} numberOfLines={1}>
                      {v.title}
                    </Text>
                    <Text style={styles.miniTicketCondition}>
                      {isEligible
                        ? `Tiết kiệm ${formatCurrencyVND(calcEstimatedSaving(v))}`
                        : `Thiếu ${formatCurrencyVND(v.minOrderValue - subtotal)}`}
                    </Text>
                  </Pressable>
                );
              })}
            </ScrollView>
          </View>

          {/* 4. PHƯƠNG THỨC THANH TOÁN */}
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

          {/* 5. GHI CHÚ ĐƠN HÀNG */}
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

          {/* 6. TÓM TẮT CHI PHÍ THANH TOÁN */}
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Chi tiết thanh toán</Text>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>
                Tổng tiền hàng ({selectedItems.reduce((s, i) => s + i.quantity, 0)} cuốn)
              </Text>
              <Text style={styles.summaryValue}>{formatCurrencyVND(subtotal)}</Text>
            </View>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>Phí vận chuyển</Text>
              <Text style={styles.summaryValue}>
                {shippingFee === 0 ? 'Miễn phí (0 đ)' : formatCurrencyVND(shippingFee)}
              </Text>
            </View>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>
                Giảm giá Voucher {appliedVoucher && discountAmount > 0 ? `(${appliedVoucher.code})` : ''}
              </Text>
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

        {/* THANH ĐÁY XÁC NHẬN ĐẶT HÀNG (CHUẨN SHOPEE) */}
        <View style={styles.bottomBar}>
          <View style={styles.bottomPriceCol}>
            <View style={styles.bottomTotalRow}>
              <Text style={styles.bottomTotalLabel}>Tổng thanh toán </Text>
              <Text numberOfLines={1} style={styles.bottomTotalValue}>
                {formatCurrencyVND(finalTotal)}
              </Text>
            </View>
            {discountAmount > 0 ? (
              <View style={styles.bottomSavedRow}>
                <Text style={styles.bottomSavedLabel}>Tiết kiệm </Text>
                <Text numberOfLines={1} style={styles.bottomSavedValue}>
                  {formatCurrencyVND(discountAmount)}
                </Text>
              </View>
            ) : null}
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
              {isSubmitting ? 'Đang xử lý...' : 'Đặt hàng'}
            </Text>
          </Pressable>
        </View>

        {/* MODAL CHỌN VOUCHER TRỰC QUAN (CHUẨN SHOPEE / FAHASA) */}
        <Modal
          visible={voucherModalVisible}
          transparent
          animationType="slide"
          onRequestClose={() => setVoucherModalVisible(false)}
        >
          <View style={styles.modalBackdrop}>
            <View style={styles.modalSheet}>
              {/* Header Modal */}
              <View style={styles.modalHeader}>
                <Text style={styles.modalTitle}>Chọn Tri Thức Việt Voucher</Text>
                <Pressable onPress={() => setVoucherModalVisible(false)} hitSlop={8}>
                  <Ionicons name="close" size={24} color={colors.textPrimary} />
                </Pressable>
              </View>

              {/* Ô nhập mã Voucher thủ công */}
              <View style={styles.modalInputBar}>
                <TextInput
                  value={voucherInput}
                  onChangeText={setVoucherInput}
                  placeholder="Nhập mã Voucher (VD: BANMOI20K, CHAOHE50K)"
                  placeholderTextColor={colors.textMuted}
                  autoCapitalize="characters"
                  style={styles.modalTextInput}
                />
                <Pressable
                  onPress={() => {
                    const ok = applyVoucherCode(voucherInput);
                    if (ok) setVoucherModalVisible(false);
                  }}
                  style={styles.modalApplyBtn}
                >
                  <Text style={styles.modalApplyBtnText}>Áp dụng</Text>
                </Pressable>
              </View>

              {voucherError ? (
                <Text style={styles.modalErrorText}>{voucherError}</Text>
              ) : null}

              {/* Danh sách tất cả Voucher */}
              <ScrollView contentContainerStyle={styles.modalVoucherList}>
                {INITIAL_VOUCHERS.map((v) => {
                  const isEligible = subtotal >= v.minOrderValue;
                  const isSelected = appliedVoucher?.code === v.code && discountAmount > 0;
                  const saving = calcEstimatedSaving(v);

                  return (
                    <Pressable
                      key={v.id}
                      onPress={() => {
                        if (isEligible) {
                          handleSelectVoucher(v);
                        }
                      }}
                      style={[
                        styles.ticketCard,
                        isSelected && styles.ticketCardSelected,
                        !isEligible && styles.ticketCardDisabled,
                      ]}
                    >
                      {/* Cuống vé bên trái */}
                      <View
                        style={[
                          styles.ticketLeftStub,
                          !isEligible && { backgroundColor: '#94A3B8' },
                        ]}
                      >
                        <Ionicons name="pricetag" size={20} color="#FFFFFF" />
                        <Text style={styles.ticketStubText}>
                          {v.discountType === 'PERCENTAGE'
                            ? `-${v.discountValue}%`
                            : `-${v.discountValue / 1000}K`}
                        </Text>
                        <Text style={styles.ticketStubSub}>VOUCHER</Text>
                      </View>

                      {/* Nội dung vé ở giữa */}
                      <View style={styles.ticketBody}>
                        <View style={styles.ticketTopRow}>
                          <Text style={styles.ticketCodeBadge}>{v.code}</Text>
                          <Text style={styles.ticketExp}>HSD: {v.endDate}</Text>
                        </View>
                        <Text style={styles.ticketTitleText}>{v.title}</Text>
                        <Text style={styles.ticketDescText}>{v.description}</Text>

                        {isEligible ? (
                          <Text style={styles.ticketEligibleText}>
                            ✓ Đủ điều kiện • Giảm ngay {formatCurrencyVND(saving)}
                          </Text>
                        ) : (
                          <Text style={styles.ticketMissingText}>
                            Mua thêm {formatCurrencyVND(v.minOrderValue - subtotal)} để dùng mã này
                          </Text>
                        )}
                      </View>

                      {/* Nút chọn bên phải */}
                      <View style={styles.ticketRightCol}>
                        <Ionicons
                          name={isSelected ? 'radio-button-on' : 'radio-button-off'}
                          size={22}
                          color={
                            isSelected
                              ? colors.primary
                              : isEligible
                              ? colors.textSecondary
                              : colors.border
                          }
                        />
                      </View>
                    </Pressable>
                  );
                })}
              </ScrollView>

              {/* Footer Modal */}
              <View style={styles.modalFooter}>
                {appliedVoucher ? (
                  <Pressable
                    onPress={() => {
                      clearVoucher();
                      setVoucherModalVisible(false);
                    }}
                    style={styles.modalClearBtn}
                  >
                    <Text style={styles.modalClearBtnText}>Bỏ dùng Voucher</Text>
                  </Pressable>
                ) : null}
                <Pressable
                  onPress={() => setVoucherModalVisible(false)}
                  style={[styles.modalDoneBtn, { flex: 1 }]}
                >
                  <Text style={styles.modalDoneBtnText}>Đồng ý</Text>
                </Pressable>
              </View>
            </View>
          </View>
        </Modal>
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
    padding: 12,
    paddingBottom: 96,
    gap: 10,
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: 10,
    padding: 12,
    borderWidth: 1,
    borderColor: colors.border,
    gap: 8,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  cardTitle: {
    fontSize: 13.5,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  addressOption: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    padding: 10,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.background,
  },
  addressOptionActive: {
    borderColor: colors.primary,
    backgroundColor: colors.primarySoft,
  },
  addrName: {
    fontSize: 12.5,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  addrStreet: {
    fontSize: 11.5,
    color: colors.textSecondary,
    marginTop: 1,
  },
  orderItemRow: {
    flexDirection: 'row',
    gap: 10,
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: colors.borderSubtle,
  },
  itemCover: {
    width: 52,
    height: 72,
    borderRadius: 6,
    backgroundColor: colors.background,
  },
  itemTitle: {
    fontSize: 12.5,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  itemAuthor: {
    fontSize: 11,
    color: colors.textSecondary,
  },
  itemPriceRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 3,
  },
  itemPrice: {
    fontSize: 13,
    fontWeight: '700',
    color: colors.accent,
  },
  qtyStepper: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 6,
    backgroundColor: colors.background,
  },
  qtyBtn: {
    paddingHorizontal: 7,
    paddingVertical: 3,
  },
  qtyText: {
    paddingHorizontal: 7,
    fontSize: 12,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  voucherHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  voucherIconBadge: {
    width: 26,
    height: 26,
    borderRadius: 6,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  voucherSubHint: {
    fontSize: 11,
    color: colors.textSecondary,
    marginTop: 1,
  },
  voucherRightAction: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  selectVoucherPrompt: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.primary,
  },
  activeDiscountTag: {
    backgroundColor: '#FFF7ED',
    borderWidth: 1,
    borderColor: colors.accent,
    paddingHorizontal: 7,
    paddingVertical: 2,
    borderRadius: 4,
  },
  activeDiscountTagText: {
    fontSize: 11.5,
    fontWeight: '700',
    color: colors.accent,
  },
  appliedBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    padding: 8,
    borderRadius: 8,
    backgroundColor: colors.primarySoft,
    borderWidth: 1,
    borderColor: colors.primaryBorder,
  },
  appliedBannerTitle: {
    fontSize: 12,
    fontWeight: '700',
    color: colors.primary,
  },
  appliedBannerDesc: {
    fontSize: 11,
    color: colors.textSecondary,
  },
  warningVoucherBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    padding: 8,
    borderRadius: 8,
    backgroundColor: '#FFF7ED',
    borderWidth: 1,
    borderColor: '#FED7AA',
  },
  warningVoucherText: {
    flex: 1,
    fontSize: 11.5,
    color: '#C2410C',
    fontWeight: '600',
  },
  quickTicketScroll: {
    gap: 8,
    paddingTop: 2,
  },
  miniTicket: {
    width: 175,
    padding: 8,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.background,
    gap: 2,
  },
  miniTicketSelected: {
    borderColor: colors.primary,
    backgroundColor: colors.primarySoft,
  },
  miniTicketDisabled: {
    opacity: 0.65,
  },
  miniTicketTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  miniTicketCode: {
    fontSize: 11.5,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  miniTicketStatusPill: {
    paddingHorizontal: 5,
    paddingVertical: 1.5,
    borderRadius: 4,
  },
  miniTicketStatusText: {
    fontSize: 9.5,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  miniTicketTitle: {
    fontSize: 11,
    color: colors.textSecondary,
  },
  miniTicketCondition: {
    fontSize: 10.5,
    fontWeight: '600',
    color: colors.primary,
  },
  qrPreviewBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    padding: 10,
    borderRadius: 10,
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.primaryBorder,
  },
  noteInput: {
    minHeight: 60,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.background,
    padding: 10,
    fontSize: 12.5,
    color: colors.textPrimary,
    textAlignVertical: 'top',
  },
  summaryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
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
  summaryHighlight: {
    fontSize: 15,
    fontWeight: '700',
    color: colors.primary,
  },
  savedSubText: {
    fontSize: 11,
    fontWeight: '600',
    color: colors.primary,
  },
  totalDivider: {
    paddingTop: 8,
    borderTopWidth: 1,
    borderTopColor: colors.borderSubtle,
  },
  bottomBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
    gap: 12,
    paddingHorizontal: 12,
    paddingVertical: 8,
    backgroundColor: colors.surface,
    borderTopWidth: 1,
    borderTopColor: colors.border,
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
  bottomTotalLabel: {
    fontSize: 12,
    color: colors.textSecondary,
  },
  bottomTotalValue: {
    fontSize: 15.5,
    fontWeight: '700',
    color: colors.primary,
  },
  bottomSavedRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 1,
  },
  bottomSavedLabel: {
    fontSize: 11,
    color: colors.textSecondary,
  },
  bottomSavedValue: {
    fontSize: 11,
    fontWeight: '600',
    color: colors.primary,
  },
  confirmBtn: {
    height: 40,
    paddingHorizontal: 20,
    borderRadius: 8,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  confirmBtnText: {
    color: '#FFFFFF',
    fontSize: 13.5,
    fontWeight: '600',
  },
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.55)',
    justifyContent: 'flex-end',
  },
  modalSheet: {
    backgroundColor: colors.surface,
    borderTopLeftRadius: 22,
    borderTopRightRadius: 22,
    maxHeight: '82%',
    paddingBottom: 20,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  modalTitle: {
    fontSize: 16,
    fontWeight: '800',
    color: colors.textPrimary,
  },
  modalInputBar: {
    flexDirection: 'row',
    gap: 8,
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 6,
  },
  modalTextInput: {
    flex: 1,
    height: 42,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.background,
    paddingHorizontal: 12,
    fontSize: 13,
    color: colors.textPrimary,
  },
  modalApplyBtn: {
    paddingHorizontal: 16,
    height: 42,
    borderRadius: 10,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  modalApplyBtnText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 13,
  },
  modalErrorText: {
    paddingHorizontal: 16,
    fontSize: 12,
    color: colors.danger,
    marginTop: 4,
  },
  modalVoucherList: {
    padding: 16,
    gap: 12,
  },
  ticketCard: {
    flexDirection: 'row',
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: colors.border,
    backgroundColor: colors.background,
    overflow: 'hidden',
  },
  ticketCardSelected: {
    borderColor: colors.primary,
    backgroundColor: colors.primarySoft,
  },
  ticketCardDisabled: {
    opacity: 0.65,
  },
  ticketLeftStub: {
    width: 78,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 12,
    gap: 2,
  },
  ticketStubText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '900',
  },
  ticketStubSub: {
    color: '#CCFBF1',
    fontSize: 9,
    fontWeight: '700',
  },
  ticketBody: {
    flex: 1,
    padding: 10,
    gap: 3,
  },
  ticketTopRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  ticketCodeBadge: {
    fontSize: 12,
    fontWeight: '800',
    color: colors.primary,
  },
  ticketExp: {
    fontSize: 10.5,
    color: colors.textMuted,
  },
  ticketTitleText: {
    fontSize: 13,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  ticketDescText: {
    fontSize: 11.5,
    color: colors.textSecondary,
  },
  ticketEligibleText: {
    fontSize: 11.5,
    fontWeight: '700',
    color: colors.primary,
    marginTop: 2,
  },
  ticketMissingText: {
    fontSize: 11.5,
    fontWeight: '700',
    color: colors.accent,
    marginTop: 2,
  },
  ticketRightCol: {
    justifyContent: 'center',
    paddingRight: 12,
  },
  modalFooter: {
    flexDirection: 'row',
    gap: 10,
    paddingHorizontal: 16,
    paddingTop: 10,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  modalClearBtn: {
    height: 44,
    paddingHorizontal: 16,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  modalClearBtnText: {
    fontSize: 13,
    fontWeight: '700',
    color: colors.textSecondary,
  },
  modalDoneBtn: {
    height: 44,
    borderRadius: 10,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  modalDoneBtnText: {
    fontSize: 14,
    fontWeight: '700',
    color: '#FFFFFF',
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
