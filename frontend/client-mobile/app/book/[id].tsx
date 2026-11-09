import React, { useEffect, useState } from 'react';
import {
  Image,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Book, Review, formatCurrencyVND } from '@shared/types';
import { colors } from '@/theme/colors';
import { bookService } from '@/services/bookService';
import { useCart } from '@/hooks/useCart';
import { useAuth } from '@/hooks/useAuth';
import { useAiChat } from '@/hooks/useAiChat';
import { EmptyStateView } from '@/components/common/StateViews';

export default function BookDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const { addToCart } = useCart();
  const { wishlistBookIds, toggleWishlist } = useAuth();
  const { openChat, sendMessage } = useAiChat();

  const [book, setBook] = useState<Book | undefined>(undefined);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [quantity, setQuantity] = useState(1);
  const [zoomVisible, setZoomVisible] = useState(false);
  const [addedToast, setAddedToast] = useState(false);

  useEffect(() => {
    const load = async () => {
      if (!id) return;
      const [detail, revList] = await Promise.all([
        bookService.getBookDetail(id),
        bookService.getBookReviews(id),
      ]);
      setBook(detail);
      setReviews(revList);
    };
    load();
  }, [id]);

  if (!book) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <EmptyStateView
          title="Đang tải thông tin sách..."
          description="Vui lòng chờ trong giây lát."
          actionLabel="Quay lại danh mục"
          onAction={() => router.back()}
        />
      </SafeAreaView>
    );
  }

  const effectivePrice = book.flashSalePrice ?? book.price;
  const isLiked = wishlistBookIds.includes(book.id);

  const handleAddToCart = () => {
    addToCart(book, quantity);
    setAddedToast(true);
    setTimeout(() => setAddedToast(false), 2200);
  };

  const handleBuyNow = () => {
    addToCart(book, quantity);
    router.push('/checkout');
  };

  const handleAskAiSummary = () => {
    openChat();
    sendMessage(`Tóm tắt cuốn ${book.title}`);
  };

  return (
    <SafeAreaView edges={['bottom']} style={styles.safeArea}>
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        contentContainerStyle={styles.scrollContent}
      >
        {/* Cover Image with Zoom Trigger */}
        <Pressable
          onPress={() => setZoomVisible(true)}
          style={styles.coverHero}
          accessibilityLabel="Chạm để phóng to bìa sách"
        >
          <Image source={{ uri: book.coverUrl }} style={styles.coverImg} resizeMode="contain" />
          <View style={styles.zoomHintPill}>
            <Ionicons name="expand-outline" size={14} color="#FFFFFF" />
            <Text style={styles.zoomHintText}>Chạm để phóng to ảnh bìa</Text>
          </View>
        </Pressable>

        {/* Main Book Info Card */}
        <View style={styles.card}>
          <View style={styles.badgeRow}>
            <View style={styles.categoryTag}>
              <Text style={styles.categoryTagText}>{book.categoryName}</Text>
            </View>
            {book.isFlashSale ? (
              <View style={styles.flashTag}>
                <Ionicons name="flash" size={12} color="#FFFFFF" />
                <Text style={styles.flashTagText}>FLASH SALE</Text>
              </View>
            ) : null}
            <View style={{ flex: 1 }} />
            <Pressable
              onPress={() => toggleWishlist(book.id)}
              hitSlop={10}
              style={styles.wishlistBtn}
            >
              <Ionicons
                name={isLiked ? 'heart' : 'heart-outline'}
                size={22}
                color={isLiked ? colors.danger : colors.textSecondary}
              />
            </Pressable>
          </View>

          <Text style={styles.bookTitle}>{book.title}</Text>
          <Text style={styles.authorText}>
            Tác giả: <Text style={{ fontWeight: '700', color: colors.primary }}>{book.authorName}</Text>
          </Text>

          <View style={styles.statsRow}>
            <View style={styles.ratingBadge}>
              <Ionicons name="star" size={14} color={colors.star} />
              <Text style={styles.ratingValue}>{book.ratingAvg.toFixed(1)}</Text>
              <Text style={styles.ratingCount}>({book.ratingCount} đánh giá)</Text>
            </View>
            <Text style={styles.dotSep}>•</Text>
            <Text style={styles.soldText}>Đã bán {book.soldCount}</Text>
            <Text style={styles.dotSep}>•</Text>
            <Text
              style={[
                styles.stockText,
                book.stock < 5 && { color: colors.warning, fontWeight: '700' },
              ]}
            >
              Kho còn: {book.stock} cuốn
            </Text>
          </View>

          <View style={styles.priceBox}>
            <Text style={styles.currentPrice}>{formatCurrencyVND(effectivePrice)}</Text>
            {book.originalPrice > effectivePrice ? (
              <Text style={styles.oldPrice}>{formatCurrencyVND(book.originalPrice)}</Text>
            ) : null}
          </View>

          {/* Quick Book Summary Button */}
          <Pressable onPress={handleAskAiSummary} style={styles.aiSummaryBtn}>
            <Ionicons name="book-outline" size={16} color={colors.primary} />
            <Text style={styles.aiSummaryBtnText}>
              Xem tóm tắt nhanh nội dung cuốn sách này
            </Text>
            <Ionicons name="chevron-forward" size={16} color={colors.primary} />
          </Pressable>
        </View>

        {/* Specifications Table */}
        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Thông tin xuất bản</Text>
          <View style={styles.specRow}>
            <Text style={styles.specLabel}>Mã ISBN</Text>
            <Text selectable style={styles.specValue}>
              {book.isbn}
            </Text>
          </View>
          <View style={styles.specRow}>
            <Text style={styles.specLabel}>Nhà xuất bản</Text>
            <Text style={styles.specValue}>{book.publisherName}</Text>
          </View>
          <View style={styles.specRow}>
            <Text style={styles.specLabel}>Hình thức bìa</Text>
            <Text style={styles.specValue}>{book.coverFormat}</Text>
          </View>
          <View style={styles.specRow}>
            <Text style={styles.specLabel}>Số trang / Năm XB</Text>
            <Text style={styles.specValue}>
              {book.pageCount} trang • {book.publishYear}
            </Text>
          </View>
        </View>

        {/* Detailed Book Description */}
        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Giới thiệu nội dung</Text>
          <Text style={styles.summaryHighlight}>{book.shortSummary}</Text>
          <Text style={styles.descriptionText}>{book.description}</Text>
        </View>

        {/* Customer Reviews */}
        <View style={styles.card}>
          <Text style={styles.sectionTitle}>
            Đánh giá từ độc giả ({reviews.length})
          </Text>
          {reviews.length === 0 ? (
            <Text style={styles.descriptionText}>
              Chưa có nhận xét nào cho cuốn sách này.
            </Text>
          ) : (
            reviews.map((rev) => (
              <View key={rev.id} style={styles.reviewItem}>
                <View style={styles.reviewHeader}>
                  <Text style={styles.reviewerName}>{rev.userName}</Text>
                  <View style={{ flexDirection: 'row', gap: 2 }}>
                    {Array.from({ length: rev.rating }).map((_, idx) => (
                      <Ionicons key={idx} name="star" size={12} color={colors.star} />
                    ))}
                  </View>
                </View>
                <Text style={styles.reviewComment}>{rev.comment}</Text>
              </View>
            ))
          )}
        </View>
      </ScrollView>

      {addedToast ? (
        <View style={styles.toastBanner}>
          <Ionicons name="checkmark-circle" size={18} color="#FFFFFF" />
          <Text style={styles.toastText}>Đã thêm {quantity} cuốn vào giỏ hàng!</Text>
        </View>
      ) : null}

      {/* STICKY BOTTOM ACTION BAR (IN THE THUMB ZONE) */}
      <View style={styles.bottomBar}>
        <View style={styles.qtyStepper}>
          <Pressable
            onPress={() => setQuantity((q) => Math.max(1, q - 1))}
            style={styles.qtyBtn}
          >
            <Ionicons name="remove" size={18} color={colors.textPrimary} />
          </Pressable>
          <Text style={styles.qtyValue}>{quantity}</Text>
          <Pressable
            onPress={() => setQuantity((q) => Math.min(book.stock, q + 1))}
            style={styles.qtyBtn}
          >
            <Ionicons name="add" size={18} color={colors.textPrimary} />
          </Pressable>
        </View>

        <Pressable
          onPress={handleAddToCart}
          style={({ pressed }) => [
            styles.cartBtn,
            pressed && { opacity: 0.9, transform: [{ scale: 0.98 }] },
          ]}
        >
          <Ionicons name="cart-outline" size={18} color={colors.primary} />
          <Text style={styles.cartBtnText}>Thêm vào giỏ</Text>
        </Pressable>

        <Pressable
          onPress={handleBuyNow}
          style={({ pressed }) => [
            styles.buyNowBtn,
            pressed && { opacity: 0.9, transform: [{ scale: 0.98 }] },
          ]}
        >
          <Text style={styles.buyNowBtnText}>Mua ngay</Text>
        </Pressable>
      </View>

      {/* IMAGE ZOOM MODAL */}
      <Modal
        visible={zoomVisible}
        transparent
        animationType="fade"
        onRequestClose={() => setZoomVisible(false)}
      >
        <View style={styles.zoomModalBg}>
          <Pressable
            onPress={() => setZoomVisible(false)}
            style={styles.zoomCloseBtn}
          >
            <Ionicons name="close-circle" size={36} color="#FFFFFF" />
          </Pressable>
          <Image
            source={{ uri: book.coverUrl }}
            style={styles.zoomFullImage}
            resizeMode="contain"
          />
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.background,
  },
  scrollContent: {
    paddingBottom: 110,
    gap: 12,
  },
  coverHero: {
    width: '100%',
    height: 290,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  coverImg: {
    width: 190,
    height: 250,
    borderRadius: 10,
  },
  zoomHintPill: {
    position: 'absolute',
    bottom: 12,
    right: 16,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 999,
    backgroundColor: 'rgba(15, 23, 42, 0.7)',
  },
  zoomHintText: {
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '600',
  },
  card: {
    marginHorizontal: 16,
    padding: 16,
    borderRadius: 16,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    gap: 10,
  },
  badgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  categoryTag: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 6,
    backgroundColor: colors.primarySoft,
  },
  categoryTagText: {
    fontSize: 12,
    fontWeight: '700',
    color: colors.primary,
  },
  flashTag: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
    backgroundColor: colors.accent,
  },
  flashTagText: {
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '700',
  },
  wishlistBtn: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: colors.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
  bookTitle: {
    fontSize: 19,
    fontWeight: '800',
    color: colors.textPrimary,
    lineHeight: 26,
  },
  authorText: {
    fontSize: 14,
    color: colors.textSecondary,
  },
  statsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: 6,
  },
  ratingBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  ratingValue: {
    fontSize: 13,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  ratingCount: {
    fontSize: 12,
    color: colors.textMuted,
  },
  dotSep: {
    color: colors.textMuted,
  },
  soldText: {
    fontSize: 13,
    color: colors.textSecondary,
  },
  stockText: {
    fontSize: 13,
    color: colors.primary,
    fontWeight: '600',
  },
  priceBox: {
    flexDirection: 'row',
    alignItems: 'baseline',
    gap: 10,
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 12,
    backgroundColor: colors.background,
  },
  currentPrice: {
    fontSize: 22,
    fontWeight: '800',
    color: colors.primary,
  },
  oldPrice: {
    fontSize: 14,
    color: colors.textMuted,
    textDecorationLine: 'line-through',
  },
  aiSummaryBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    padding: 12,
    borderRadius: 12,
    backgroundColor: colors.primarySoft,
    borderWidth: 1,
    borderColor: colors.primaryBorder,
  },
  aiSummaryBtnText: {
    flex: 1,
    fontSize: 13,
    fontWeight: '600',
    color: colors.primaryDark,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  specRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: colors.borderSubtle,
  },
  specLabel: {
    fontSize: 13,
    color: colors.textSecondary,
  },
  specValue: {
    fontSize: 13,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  summaryHighlight: {
    fontSize: 14,
    fontWeight: '600',
    color: colors.textPrimary,
    lineHeight: 21,
  },
  descriptionText: {
    fontSize: 14,
    color: colors.textSecondary,
    lineHeight: 22,
  },
  reviewItem: {
    paddingVertical: 10,
    borderTopWidth: 1,
    borderTopColor: colors.borderSubtle,
    gap: 4,
  },
  reviewHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  reviewerName: {
    fontSize: 13,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  reviewComment: {
    fontSize: 13,
    color: colors.textSecondary,
    lineHeight: 19,
  },
  toastBanner: {
    position: 'absolute',
    bottom: 84,
    left: 20,
    right: 20,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    padding: 12,
    borderRadius: 12,
    backgroundColor: colors.textPrimary,
  },
  toastText: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '600',
  },
  bottomBar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: colors.surface,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  qtyStepper: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.background,
  },
  qtyBtn: {
    width: 36,
    height: 42,
    alignItems: 'center',
    justifyContent: 'center',
  },
  qtyValue: {
    minWidth: 26,
    textAlign: 'center',
    fontSize: 14,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  cartBtn: {
    flex: 1,
    minHeight: 44,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: colors.primary,
    backgroundColor: colors.primarySoft,
  },
  cartBtnText: {
    fontSize: 14,
    fontWeight: '700',
    color: colors.primary,
  },
  buyNowBtn: {
    flex: 1,
    minHeight: 44,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 12,
    backgroundColor: colors.primary,
  },
  buyNowBtnText: {
    fontSize: 14,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  zoomModalBg: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.92)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  zoomCloseBtn: {
    position: 'absolute',
    top: 48,
    right: 24,
    zIndex: 10,
  },
  zoomFullImage: {
    width: '100%',
    height: '75%',
  },
});

