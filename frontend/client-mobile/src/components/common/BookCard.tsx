import React from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Book, formatCurrencyVND } from '@shared/types';
import { colors } from '@/theme/colors';
import { useCart } from '@/hooks/useCart';

interface BookCardProps {
  book: Book;
  compact?: boolean;
}

export function BookCard({ book, compact = false }: BookCardProps) {
  const router = useRouter();
  const { addToCart } = useCart();
  const effectivePrice = book.flashSalePrice ?? book.price;
  const discountPercent =
    book.originalPrice > effectivePrice
      ? Math.round(((book.originalPrice - effectivePrice) / book.originalPrice) * 100)
      : 0;

  return (
    <Pressable
      onPress={() => router.push(`/book/${book.id}`)}
      style={({ pressed }) => [
        styles.card,
        compact && styles.cardCompact,
        pressed && { transform: [{ scale: 0.98 }], opacity: 0.94 },
      ]}
    >
      <View style={styles.coverWrapper}>
        <Image source={{ uri: book.coverUrl }} style={styles.cover} resizeMode="cover" />
        {book.isFlashSale ? (
          <View style={styles.flashBadge}>
            <Ionicons name="flash" size={11} color="#FFFFFF" />
            <Text style={styles.flashBadgeText}>FLASH SALE</Text>
          </View>
        ) : discountPercent > 0 ? (
          <View style={styles.discountBadge}>
            <Text style={styles.discountBadgeText}>-{discountPercent}%</Text>
          </View>
        ) : null}
      </View>

      <View style={styles.content}>
        <Text style={styles.categoryLabel} numberOfLines={1}>
          {book.categoryName}
        </Text>
        <Text style={styles.title} numberOfLines={2}>
          {book.title}
        </Text>
        <Text style={styles.author} numberOfLines={1}>
          {book.authorName}
        </Text>

        <View style={styles.metaRow}>
          <View style={styles.ratingBox}>
            <Ionicons name="star" size={12} color={colors.star} />
            <Text style={styles.ratingText}>{book.ratingAvg.toFixed(1)}</Text>
          </View>
          <Text style={styles.soldText}>Đã bán {book.soldCount}</Text>
        </View>

        <View style={styles.footerRow}>
          <View style={{ flex: 1 }}>
            <Text style={styles.price}>{formatCurrencyVND(effectivePrice)}</Text>
            {book.originalPrice > effectivePrice ? (
              <Text style={styles.originalPrice}>{formatCurrencyVND(book.originalPrice)}</Text>
            ) : null}
          </View>
          <Pressable
            hitSlop={8}
            onPress={(e) => {
              e.stopPropagation();
              addToCart(book, 1);
            }}
            style={({ pressed }) => [
              styles.addCartBtn,
              pressed && { backgroundColor: colors.primaryDark, transform: [{ scale: 0.94 }] },
            ]}
            accessibilityLabel={`Thêm ${book.title} vào giỏ`}
          >
            <Ionicons name="cart-outline" size={18} color={colors.textInverse} />
          </Pressable>
        </View>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    flex: 1,
    minWidth: 155,
    backgroundColor: colors.surface,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.border,
    overflow: 'hidden',
  },
  cardCompact: {
    width: 164,
    flex: 0,
  },
  coverWrapper: {
    position: 'relative',
    width: '100%',
    height: 172,
    backgroundColor: colors.surfaceMuted,
  },
  cover: {
    width: '100%',
    height: '100%',
  },
  flashBadge: {
    position: 'absolute',
    top: 8,
    left: 8,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
    backgroundColor: colors.accent,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
  },
  flashBadgeText: {
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '700',
  },
  discountBadge: {
    position: 'absolute',
    top: 8,
    right: 8,
    backgroundColor: colors.danger,
    paddingHorizontal: 6,
    paddingVertical: 3,
    borderRadius: 6,
  },
  discountBadgeText: {
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '700',
  },
  content: {
    padding: 9,
    gap: 3,
  },
  categoryLabel: {
    fontSize: 10,
    fontWeight: '600',
    color: colors.primary,
  },
  title: {
    fontSize: 12.5,
    fontWeight: '600',
    color: colors.textPrimary,
    lineHeight: 16.5,
    minHeight: 33,
  },
  author: {
    fontSize: 11,
    color: colors.textSecondary,
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 2,
  },
  ratingBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
  },
  ratingText: {
    fontSize: 11,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  soldText: {
    fontSize: 10.5,
    color: colors.textMuted,
  },
  footerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 4,
    paddingTop: 5,
    borderTopWidth: 1,
    borderTopColor: colors.borderSubtle,
  },
  price: {
    fontSize: 13.5,
    fontWeight: '700',
    color: colors.accent,
  },
  originalPrice: {
    fontSize: 10.5,
    color: colors.textMuted,
    textDecorationLine: 'line-through',
  },
  addCartBtn: {
    width: 32,
    height: 32,
    borderRadius: 8,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
});

