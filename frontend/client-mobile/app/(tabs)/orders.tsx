import React, { useEffect, useState } from 'react';
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
  OrderStatus,
  PAYMENT_METHOD_LABELS,
  formatCurrencyVND,
} from '@shared/types';
import { ORDER_STATUS_BADGE, colors } from '@/theme/colors';
import { orderService } from '@/services/orderService';
import { bookService } from '@/services/bookService';
import { useAuth } from '@/hooks/useAuth';
import { EmptyStateView } from '@/components/common/StateViews';

const STATUS_TABS: { key: 'ALL' | OrderStatus; label: string }[] = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'PENDING', label: 'Chờ duyệt' },
  { key: 'PREPARING', label: 'Đang chuẩn bị' },
  { key: 'SHIPPING', label: 'Đang giao' },
  { key: 'DELIVERED', label: 'Đã giao' },
  { key: 'CANCELLED', label: 'Đã hủy' },
];

export default function OrdersScreen() {
  const router = useRouter();
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<'ALL' | OrderStatus>('ALL');
  const [orders, setOrders] = useState<Order[]>([]);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);

  // Review Modal State
  const [reviewOrder, setReviewOrder] = useState<Order | null>(null);
  const [rating, setRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [includePhoto, setIncludePhoto] = useState(true);

  // Return Request Modal State
  const [returnOrder, setReturnOrder] = useState<Order | null>(null);
  const [returnReason, setReturnReason] = useState('Sách bị móp góc do vận chuyển');
  const [returnDesc, setReturnDesc] = useState('');

  // Status Banner Feedback
  const [bannerMessage, setBannerMessage] = useState<string | null>(null);

  const loadOrders = async () => {
    if (!user) {
      setOrders([]);
      return;
    }
    const list = await orderService.getCustomerOrders(user.id);
    setOrders(list);
  };

  useEffect(() => {
    loadOrders();
  }, [user]);

  const filteredOrders =
    activeTab === 'ALL'
      ? orders
      : orders.filter((o) => o.status === activeTab);

  const handleCancelOrder = async (order: Order) => {
    await orderService.cancelCustomerOrder(
      order.id,
      'Khách hàng thay đổi nhu cầu đặt sách'
    );
    setBannerMessage(`Đã hủy đơn hàng ${order.orderCode} thành công.`);
    await loadOrders();
    setSelectedOrder(null);
  };

  const handleSubmitReview = async () => {
    if (!reviewOrder || !user) return;
    const firstItem = reviewOrder.items[0];
    await bookService.submitReview({
      bookId: firstItem.bookId,
      bookTitle: firstItem.title,
      userId: user.id,
      userName: user.fullName,
      orderId: reviewOrder.id,
      rating,
      comment:
        reviewComment.trim() ||
        'Sách đóng gói rất cẩn thận, nội dung hay và giao hàng đúng hẹn!',
      imageUrls: includePhoto ? [firstItem.coverUrl] : [],
    });
    setReviewOrder(null);
    setReviewComment('');
    setBannerMessage(`Cảm ơn bạn đã đánh giá ${rating} sao cho đơn hàng ${reviewOrder.orderCode}!`);
    await loadOrders();
  };

  const handleSubmitReturn = async () => {
    if (!returnOrder) return;
    await orderService.submitReturnRequest({
      orderId: returnOrder.id,
      reason: returnReason,
      description:
        returnDesc.trim() ||
        'Mong cửa hàng kiểm tra hình ảnh minh chứng và hỗ trợ đổi cuốn mới giúp mình.',
      evidenceImages: [returnOrder.items[0]?.coverUrl || ''],
    });
    setReturnOrder(null);
    setReturnDesc('');
    setBannerMessage(
      `Đã gửi yêu cầu đổi trả cho đơn ${returnOrder.orderCode}. Bộ phận CSKH sẽ phản hồi trong 24h.`
    );
    await loadOrders();
  };

  return (
    <SafeAreaView edges={['top']} style={styles.safeArea}>
      <View style={styles.header}>
        <Text style={styles.screenTitle}>Theo dõi đơn hàng</Text>
      </View>

      {/* Status Filter Tabs */}
      <View style={styles.tabsWrapper}>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.tabsScroll}
        >
          {STATUS_TABS.map((tab) => {
            const isActive = activeTab === tab.key;
            return (
              <Pressable
                key={tab.key}
                onPress={() => setActiveTab(tab.key)}
                style={[styles.tabPill, isActive && styles.tabPillActive]}
              >
                <Text style={[styles.tabText, isActive && styles.tabTextActive]}>
                  {tab.label}
                </Text>
              </Pressable>
            );
          })}
        </ScrollView>
      </View>

      {bannerMessage ? (
        <View style={styles.feedbackBanner}>
          <Ionicons name="checkmark-circle" size={18} color={colors.primary} />
          <Text style={styles.feedbackText}>{bannerMessage}</Text>
          <Pressable onPress={() => setBannerMessage(null)}>
            <Ionicons name="close" size={18} color={colors.textSecondary} />
          </Pressable>
        </View>
      ) : null}

      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        contentContainerStyle={styles.listContent}
      >
        {!user ? (
          <EmptyStateView
            icon="person-circle-outline"
            title="Bạn chưa đăng nhập"
            description="Vui lòng đăng nhập tài khoản khách hàng để theo dõi tiến độ giao sách."
            actionLabel="Đăng nhập ngay"
            onAction={() => router.push('/(tabs)/profile')}
          />
        ) : filteredOrders.length === 0 ? (
          <EmptyStateView
            icon="receipt-outline"
            title="Chưa có đơn hàng nào"
            description="Bạn chưa có đơn hàng nào ở trạng thái này. Khám phá kho sách ngay nhé!"
            actionLabel="Khám phá kho sách"
            onAction={() => router.push('/(tabs)/catalog')}
          />
        ) : (
          filteredOrders.map((order) => {
            const badge = ORDER_STATUS_BADGE[order.status];
            return (
              <View key={order.id} style={styles.orderCard}>
                {/* Order Card Header */}
                <View style={styles.orderHeader}>
                  <View style={styles.orderCodeRow}>
                    <Ionicons name="cube-outline" size={16} color={colors.primary} />
                    <Text selectable style={styles.orderCode}>
                      Mã đơn: {order.orderCode}
                    </Text>
                  </View>
                  <View style={[styles.statusBadge, { backgroundColor: badge.bg }]}>
                    <Text style={[styles.statusBadgeText, { color: badge.text }]}>
                      {badge.label}
                    </Text>
                  </View>
                </View>

                {/* Order Items */}
                {order.items.map((item) => (
                  <View key={item.bookId} style={styles.itemRow}>
                    <Image source={{ uri: item.coverUrl }} style={styles.itemCover} />
                    <View style={{ flex: 1, gap: 3 }}>
                      <Text style={styles.itemTitle} numberOfLines={2}>
                        {item.title}
                      </Text>
                      <Text style={styles.itemAuthor}>{item.authorName}</Text>
                      <View style={styles.itemPriceRow}>
                        <Text style={styles.itemPrice}>
                          {formatCurrencyVND(item.unitPrice)}
                        </Text>
                        <Text style={styles.itemQty}>x{item.quantity}</Text>
                      </View>
                    </View>
                  </View>
                ))}

                {/* Latest Shipping Milestone Preview */}
                {order.timeline.length > 0 ? (
                  <Pressable
                    onPress={() => setSelectedOrder(order)}
                    style={styles.timelinePreview}
                  >
                    <Ionicons name="locate-outline" size={16} color={colors.primary} />
                    <View style={{ flex: 1 }}>
                      <Text style={styles.timelineTitle}>
                        {order.timeline[order.timeline.length - 1].title}
                      </Text>
                      <Text style={styles.timelineDesc} numberOfLines={1}>
                        {order.timeline[order.timeline.length - 1].description}
                      </Text>
                    </View>
                    <Text style={styles.viewTimelineLink}>Hành trình ›</Text>
                  </Pressable>
                ) : null}

                {/* Return Request Status if present */}
                {order.returnRequest ? (
                  <View style={styles.returnStatusBox}>
                    <Ionicons name="sync-circle" size={16} color={colors.warning} />
                    <Text style={styles.returnStatusText}>
                      Yêu cầu đổi trả:{' '}
                      {order.returnRequest.status === 'PENDING'
                        ? 'Đang chờ duyệt'
                        : order.returnRequest.status === 'APPROVED'
                        ? 'Đã chấp thuận đổi mới'
                        : 'Từ chối'}
                    </Text>
                  </View>
                ) : null}

                {/* Order Total & Action Buttons */}
                <View style={styles.orderFooter}>
                  <View>
                    <Text style={styles.totalLabel}>
                      Tổng thanh toán ({order.items.length} sản phẩm):
                    </Text>
                    <Text style={styles.totalValue}>
                      {formatCurrencyVND(order.totalAmount)}
                    </Text>
                  </View>

                  <View style={styles.actionRow}>
                    <Pressable
                      onPress={() => setSelectedOrder(order)}
                      style={styles.outlineBtn}
                    >
                      <Text style={styles.outlineBtnText}>Chi tiết</Text>
                    </Pressable>

                    {order.status === 'PENDING' ? (
                      <Pressable
                        onPress={() => handleCancelOrder(order)}
                        style={[styles.outlineBtn, { borderColor: colors.danger }]}
                      >
                        <Text style={[styles.outlineBtnText, { color: colors.danger }]}>
                          Hủy đơn
                        </Text>
                      </Pressable>
                    ) : null}

                    {order.status === 'DELIVERED' && !order.isReviewed ? (
                      <Pressable
                        onPress={() => {
                          setRating(5);
                          setReviewOrder(order);
                        }}
                        style={styles.primaryBtn}
                      >
                        <Text style={styles.primaryBtnText}>Đánh giá ★</Text>
                      </Pressable>
                    ) : null}

                    {order.status === 'DELIVERED' && !order.returnRequest ? (
                      <Pressable
                        onPress={() => setReturnOrder(order)}
                        style={styles.outlineBtn}
                      >
                        <Text style={styles.outlineBtnText}>Đổi trả</Text>
                      </Pressable>
                    ) : null}
                  </View>
                </View>
              </View>
            );
          })
        )}
      </ScrollView>

      {/* MODAL 1: CHI TIẾT HÀNH TRÌNH ĐƠN HÀNG (SHIPPING TIMELINE) */}
      <Modal
        visible={Boolean(selectedOrder)}
        animationType="slide"
        transparent
        onRequestClose={() => setSelectedOrder(null)}
      >
        <View style={styles.modalBackdrop}>
          <View style={styles.modalSheet}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>
                Hành trình đơn {selectedOrder?.orderCode}
              </Text>
              <Pressable onPress={() => setSelectedOrder(null)} hitSlop={8}>
                <Ionicons name="close" size={22} color={colors.textPrimary} />
              </Pressable>
            </View>

            {selectedOrder ? (
              <ScrollView contentContainerStyle={{ padding: 16, gap: 14 }}>
                <View style={styles.infoBlock}>
                  <Text style={styles.infoBlockTitle}>Địa chỉ nhận hàng</Text>
                  <Text style={styles.infoBlockText}>
                    {selectedOrder.shippingAddress.fullName} •{' '}
                    {selectedOrder.shippingAddress.phone}
                  </Text>
                  <Text style={styles.infoBlockSub}>
                    {selectedOrder.shippingAddress.streetAddress},{' '}
                    {selectedOrder.shippingAddress.ward},{' '}
                    {selectedOrder.shippingAddress.district},{' '}
                    {selectedOrder.shippingAddress.province}
                  </Text>
                  <Text style={styles.infoBlockSub}>
                    Phương thức: {PAYMENT_METHOD_LABELS[selectedOrder.paymentMethod]}
                  </Text>
                </View>

                <Text style={styles.infoBlockTitle}>Timeline vận chuyển</Text>
                {selectedOrder.timeline
                  .slice()
                  .reverse()
                  .map((step, idx) => (
                    <View key={idx} style={styles.timelineStep}>
                      <View style={styles.timelineDotCol}>
                        <View
                          style={[
                            styles.timelineDot,
                            idx === 0 && { backgroundColor: colors.primary },
                          ]}
                        />
                        {idx < selectedOrder.timeline.length - 1 ? (
                          <View style={styles.timelineLine} />
                        ) : null}
                      </View>
                      <View style={{ flex: 1, paddingBottom: 14 }}>
                        <Text
                          style={[
                            styles.stepTitle,
                            idx === 0 && { color: colors.primary },
                          ]}
                        >
                          {step.title}
                        </Text>
                        <Text style={styles.stepDesc}>{step.description}</Text>
                        <Text style={styles.stepTime}>{step.timestamp}</Text>
                      </View>
                    </View>
                  ))}
              </ScrollView>
            ) : null}
          </View>
        </View>
      </Modal>

      {/* MODAL 2: ĐÁNH GIÁ SẢN PHẨM (CHẤM SAO, NHẬN XÉT, ĐÍNH KÈM ẢNH) */}
      <Modal
        visible={Boolean(reviewOrder)}
        animationType="slide"
        transparent
        onRequestClose={() => setReviewOrder(null)}
      >
        <KeyboardAvoidingView
          behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
          style={styles.modalBackdrop}
        >
          <View style={styles.modalSheet}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Đánh giá chất lượng sách</Text>
              <Pressable onPress={() => setReviewOrder(null)}>
                <Ionicons name="close" size={22} color={colors.textPrimary} />
              </Pressable>
            </View>
            <ScrollView
              keyboardShouldPersistTaps="handled"
              contentContainerStyle={{ padding: 16, gap: 14 }}
            >
              <Text style={styles.formLabel}>Chọn mức độ hài lòng của bạn:</Text>
              <View style={styles.starsRow}>
                {[1, 2, 3, 4, 5].map((star) => (
                  <Pressable
                    key={star}
                    onPress={() => setRating(star)}
                    hitSlop={8}
                  >
                    <Ionicons
                      name={star <= rating ? 'star' : 'star-outline'}
                      size={34}
                      color={colors.star}
                    />
                  </Pressable>
                ))}
              </View>

              <Text style={styles.formLabel}>Nhận xét chi tiết:</Text>
              <TextInput
                value={reviewComment}
                onChangeText={setReviewComment}
                placeholder="Chia sẻ cảm nhận về nội dung sách, chất lượng giấy in và đóng gói..."
                placeholderTextColor={colors.textMuted}
                multiline
                style={styles.textArea}
              />

              <Pressable
                onPress={() => setIncludePhoto(!includePhoto)}
                style={styles.photoAttachBtn}
              >
                <Ionicons
                  name={includePhoto ? 'checkbox' : 'square-outline'}
                  size={20}
                  color={colors.primary}
                />
                <Text style={styles.photoAttachText}>
                  Đính kèm hình ảnh thực tế sản phẩm đã nhận
                </Text>
              </Pressable>

              <Pressable onPress={handleSubmitReview} style={styles.submitBtn}>
                <Text style={styles.submitBtnText}>Gửi đánh giá ngay</Text>
              </Pressable>
            </ScrollView>
          </View>
        </KeyboardAvoidingView>
      </Modal>

      {/* MODAL 3: YÊU CẦU ĐỔI TRẢ HÀNG */}
      <Modal
        visible={Boolean(returnOrder)}
        animationType="slide"
        transparent
        onRequestClose={() => setReturnOrder(null)}
      >
        <KeyboardAvoidingView
          behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
          style={styles.modalBackdrop}
        >
          <View style={styles.modalSheet}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Gửi yêu cầu đổi trả hàng</Text>
              <Pressable onPress={() => setReturnOrder(null)}>
                <Ionicons name="close" size={22} color={colors.textPrimary} />
              </Pressable>
            </View>
            <ScrollView
              keyboardShouldPersistTaps="handled"
              contentContainerStyle={{ padding: 16, gap: 12 }}
            >
              <Text style={styles.formLabel}>Lý do đổi trả:</Text>
              {[
                'Sách bị móp góc do vận chuyển',
                'Sách lỗi in ấn / thiếu trang',
                'Giao sai tựa sách đã đặt',
              ].map((r) => (
                <Pressable
                  key={r}
                  onPress={() => setReturnReason(r)}
                  style={styles.reasonOption}
                >
                  <Ionicons
                    name={returnReason === r ? 'radio-button-on' : 'radio-button-off'}
                    size={20}
                    color={colors.primary}
                  />
                  <Text style={styles.reasonText}>{r}</Text>
                </Pressable>
              ))}

              <Text style={styles.formLabel}>Mô tả tình trạng & Ảnh minh chứng:</Text>
              <TextInput
                value={returnDesc}
                onChangeText={setReturnDesc}
                placeholder="Mô tả chi tiết tình trạng sách để cửa hàng đổi mới nhanh nhất..."
                placeholderTextColor={colors.textMuted}
                multiline
                style={styles.textArea}
              />

              <Pressable onPress={handleSubmitReturn} style={styles.submitBtn}>
                <Text style={styles.submitBtnText}>Xác nhận gửi yêu cầu đổi trả</Text>
              </Pressable>
            </ScrollView>
          </View>
        </KeyboardAvoidingView>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.background,
  },
  header: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: colors.surface,
  },
  screenTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  tabsWrapper: {
    backgroundColor: colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
    paddingBottom: 10,
  },
  tabsScroll: {
    paddingHorizontal: 16,
    gap: 8,
  },
  tabPill: {
    minHeight: 36,
    paddingHorizontal: 14,
    paddingVertical: 7,
    borderRadius: 999,
    backgroundColor: colors.surfaceMuted,
    justifyContent: 'center',
  },
  tabPillActive: {
    backgroundColor: colors.primary,
  },
  tabText: {
    fontSize: 13,
    fontWeight: '600',
    color: colors.textSecondary,
  },
  tabTextActive: {
    color: '#FFFFFF',
  },
  feedbackBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginHorizontal: 16,
    marginTop: 12,
    padding: 12,
    borderRadius: 12,
    backgroundColor: colors.primarySoft,
    borderWidth: 1,
    borderColor: colors.primaryBorder,
  },
  feedbackText: {
    flex: 1,
    fontSize: 13,
    fontWeight: '600',
    color: colors.primaryDark,
  },
  listContent: {
    padding: 16,
    paddingBottom: 100,
    gap: 14,
  },
  orderCard: {
    backgroundColor: colors.surface,
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    borderColor: colors.border,
    gap: 12,
  },
  orderHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingBottom: 10,
    borderBottomWidth: 1,
    borderBottomColor: colors.borderSubtle,
  },
  orderCodeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  orderCode: {
    fontSize: 14,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  statusBadge: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
  },
  statusBadgeText: {
    fontSize: 12,
    fontWeight: '700',
  },
  itemRow: {
    flexDirection: 'row',
    gap: 12,
  },
  itemCover: {
    width: 56,
    height: 76,
    borderRadius: 8,
    backgroundColor: colors.surfaceMuted,
  },
  itemTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: colors.textPrimary,
    lineHeight: 19,
  },
  itemAuthor: {
    fontSize: 12,
    color: colors.textSecondary,
  },
  itemPriceRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 2,
  },
  itemPrice: {
    fontSize: 14,
    fontWeight: '700',
    color: colors.primary,
  },
  itemQty: {
    fontSize: 13,
    fontWeight: '600',
    color: colors.textSecondary,
  },
  timelinePreview: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    padding: 10,
    borderRadius: 10,
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.border,
  },
  timelineTitle: {
    fontSize: 12,
    fontWeight: '700',
    color: colors.primary,
  },
  timelineDesc: {
    fontSize: 12,
    color: colors.textSecondary,
  },
  viewTimelineLink: {
    fontSize: 12,
    fontWeight: '700',
    color: colors.primary,
  },
  returnStatusBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    padding: 8,
    borderRadius: 8,
    backgroundColor: colors.warningBg,
  },
  returnStatusText: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.warning,
  },
  orderFooter: {
    paddingTop: 10,
    borderTopWidth: 1,
    borderTopColor: colors.borderSubtle,
    gap: 10,
  },
  totalLabel: {
    fontSize: 12,
    color: colors.textSecondary,
  },
  totalValue: {
    fontSize: 16,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  actionRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'flex-end',
    gap: 8,
  },
  outlineBtn: {
    minHeight: 38,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: colors.border,
    justifyContent: 'center',
  },
  outlineBtnText: {
    fontSize: 13,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  primaryBtn: {
    minHeight: 38,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 10,
    backgroundColor: colors.primary,
    justifyContent: 'center',
  },
  primaryBtnText: {
    fontSize: 13,
    fontWeight: '600',
    color: '#FFFFFF',
  },
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.5)',
    justifyContent: 'flex-end',
  },
  modalSheet: {
    maxHeight: '80%',
    backgroundColor: colors.surface,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    paddingBottom: 24,
  },
  modalHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  modalTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  infoBlock: {
    padding: 12,
    borderRadius: 12,
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.border,
    gap: 4,
  },
  infoBlockTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  infoBlockText: {
    fontSize: 13,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  infoBlockSub: {
    fontSize: 12,
    color: colors.textSecondary,
  },
  timelineStep: {
    flexDirection: 'row',
    gap: 12,
  },
  timelineDotCol: {
    alignItems: 'center',
    width: 16,
  },
  timelineDot: {
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: '#94A3B8',
  },
  timelineLine: {
    flex: 1,
    width: 2,
    backgroundColor: '#E2E8F0',
    marginVertical: 2,
  },
  stepTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  stepDesc: {
    fontSize: 13,
    color: colors.textSecondary,
    marginTop: 2,
  },
  stepTime: {
    fontSize: 11,
    color: colors.textMuted,
    marginTop: 4,
  },
  formLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  starsRow: {
    flexDirection: 'row',
    gap: 12,
    justifyContent: 'center',
    paddingVertical: 6,
  },
  textArea: {
    minHeight: 96,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.background,
    padding: 12,
    fontSize: 14,
    color: colors.textPrimary,
    textAlignVertical: 'top',
  },
  photoAttachBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  photoAttachText: {
    fontSize: 13,
    color: colors.textSecondary,
  },
  reasonOption: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingVertical: 6,
  },
  reasonText: {
    fontSize: 14,
    color: colors.textPrimary,
  },
  submitBtn: {
    minHeight: 46,
    borderRadius: 12,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 8,
  },
  submitBtnText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '700',
  },
});

