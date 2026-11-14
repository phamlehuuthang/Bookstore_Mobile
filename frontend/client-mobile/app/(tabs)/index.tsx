import React, { useEffect, useMemo, useState } from 'react';
import {
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Book, Category } from '@shared/types';
import { colors } from '@/theme/colors';
import { bookService } from '@/services/bookService';
import { useCart } from '@/hooks/useCart';
import { useAuth } from '@/hooks/useAuth';
import { useAiChat } from '@/hooks/useAiChat';
import { BannerCarousel } from '@/components/common/BannerCarousel';
import { BookCard } from '@/components/common/BookCard';
import {
  BookGridSkeleton,
  EmptyStateView,
  ErrorStateView,
} from '@/components/common/StateViews';

export default function HomeScreen() {
  const router = useRouter();
  const { totalItemsCount } = useCart();
  const { user, notifications } = useAuth();
  const { openChat } = useAiChat();

  const [books, setBooks] = useState<Book[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [countdownSeconds, setCountdownSeconds] = useState(9845); // 02:44:05

  const loadData = async (refresh = false) => {
    try {
      if (refresh) setIsRefreshing(true);
      else setIsLoading(true);
      setErrorMsg(null);

      const [bookList, catList] = await Promise.all([
        bookService.getCatalogBooks({ sortBy: 'popular' }),
        bookService.getCategories(),
      ]);
      setBooks(bookList);
      setCategories(catList.filter((c) => c.parentId === null));
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'Lỗi tải dữ liệu trang chủ');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    const timer = setInterval(() => {
      setCountdownSeconds((prev) => (prev > 0 ? prev - 1 : 9845));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const hours = String(Math.floor(countdownSeconds / 3600)).padStart(2, '0');
  const mins = String(Math.floor((countdownSeconds % 3600) / 60)).padStart(2, '0');
  const secs = String(countdownSeconds % 60).padStart(2, '0');

  const flashSaleBooks = books.filter((b) => b.isFlashSale);
  const bestSellerBooks = books.filter((b) => b.isBestSeller);
  const unreadNotifCount = notifications.filter((n) => !n.isRead).length;

  const searchedBooks = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return [];
    return books.filter(
      (b) =>
        b.title.toLowerCase().includes(q) ||
        b.authorName.toLowerCase().includes(q) ||
        b.isbn.toLowerCase().includes(q) ||
        b.categoryName.toLowerCase().includes(q)
    );
  }, [books, searchQuery]);

  const isSearching = searchQuery.trim().length > 0;

  return (
    <SafeAreaView edges={['top']} style={styles.safeArea}>
      {/* Sticky Top Header */}
      <View style={styles.topHeader}>
        <View style={{ flex: 1 }}>
          <Text style={styles.greetingSub}>Tri Thức Việt BookStore</Text>
          <Text style={styles.greetingTitle} numberOfLines={1}>
            Chào {user ? user.fullName : 'Độc giả'} 👋
          </Text>
        </View>

        <View style={styles.headerRight}>
          <Pressable
            onPress={() => router.push('/(tabs)/profile')}
            style={styles.iconBtn}
            accessibilityLabel="Thông báo"
          >
            <Ionicons name="notifications-outline" size={22} color={colors.textPrimary} />
            {unreadNotifCount > 0 ? (
              <View style={styles.badge}>
                <Text style={styles.badgeText}>{unreadNotifCount}</Text>
              </View>
            ) : null}
          </Pressable>

          <Pressable
            onPress={() => router.push('/cart')}
            style={styles.iconBtn}
            accessibilityLabel="Giỏ hàng"
          >
            <Ionicons name="cart-outline" size={23} color={colors.textPrimary} />
            {totalItemsCount > 0 ? (
              <View style={styles.badge}>
                <Text style={styles.badgeText}>{totalItemsCount}</Text>
              </View>
            ) : null}
          </Pressable>
        </View>
      </View>

      {/* Real Interactive Search Input & Support Consultant Shortcut */}
      <View style={styles.searchBarRow}>
        <View style={styles.searchBox}>
          <Ionicons name="search-outline" size={18} color={colors.textMuted} />
          <TextInput
            value={searchQuery}
            onChangeText={setSearchQuery}
            placeholder="Tìm tên sách, tác giả, mã ISBN..."
            placeholderTextColor={colors.textMuted}
            style={styles.searchInput}
            returnKeyType="search"
          />
          {isSearching ? (
            <Pressable
              onPress={() => setSearchQuery('')}
              hitSlop={8}
              accessibilityLabel="Xóa từ khóa tìm kiếm"
            >
              <Ionicons name="close-circle" size={18} color={colors.textMuted} />
            </Pressable>
          ) : null}
        </View>

        <Pressable
          onPress={openChat}
          style={({ pressed }) => [
            styles.supportQuickBtn,
            pressed && { opacity: 0.85, transform: [{ scale: 0.97 }] },
          ]}
        >
          <Ionicons name="chatbubbles-outline" size={16} color={colors.primary} />
          <Text style={styles.supportQuickBtnText}>Tư vấn</Text>
        </Pressable>
      </View>

      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={isRefreshing}
            onRefresh={() => loadData(true)}
            tintColor={colors.primary}
          />
        }
        contentContainerStyle={styles.scrollContent}
      >
        {isSearching ? (
          /* LIVE SEARCH RESULTS DIRECTLY ON HOME SCREEN */
          <View style={{ paddingTop: 12, gap: 12 }}>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>
                Kết quả tìm kiếm ({searchedBooks.length})
              </Text>
              <Pressable onPress={() => setSearchQuery('')}>
                <Text style={styles.seeAllText}>Đóng tìm kiếm</Text>
              </Pressable>
            </View>

            {searchedBooks.length === 0 ? (
              <EmptyStateView
                icon="search-outline"
                title="Không tìm thấy cuốn sách phù hợp"
                description={`Không có kết quả nào khớp với từ khóa "${searchQuery}". Hãy thử tên sách hoặc tác giả khác.`}
                actionLabel="Xóa từ khóa"
                onAction={() => setSearchQuery('')}
              />
            ) : (
              <View style={styles.bookGrid}>
                {searchedBooks.map((book) => (
                  <View key={book.id} style={styles.gridCol}>
                    <BookCard book={book} />
                  </View>
                ))}
              </View>
            )}
          </View>
        ) : (
          <>
            {/* Hero Banner Carousel */}
            <BannerCarousel onSelectCategory={() => router.push('/(tabs)/catalog')} />

            {/* Quick Categories */}
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>Danh mục nổi bật</Text>
              <Pressable onPress={() => router.push('/(tabs)/catalog')}>
                <Text style={styles.seeAllText}>Xem tất cả</Text>
              </Pressable>
            </View>

            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.categoryRow}
            >
              {categories.map((cat) => (
                <Pressable
                  key={cat.id}
                  onPress={() =>
                    router.push({ pathname: '/(tabs)/catalog', params: { categoryId: cat.id } })
                  }
                  style={({ pressed }) => [
                    styles.categoryChip,
                    pressed && { backgroundColor: colors.primarySoft },
                  ]}
                >
                  <View style={styles.categoryIconBox}>
                    <Ionicons name="library-outline" size={18} color={colors.primary} />
                  </View>
                  <View>
                    <Text style={styles.categoryName}>{cat.name}</Text>
                    <Text style={styles.categoryCount}>{cat.bookCount} đầu sách</Text>
                  </View>
                </Pressable>
              ))}
            </ScrollView>

            {isLoading ? (
              <BookGridSkeleton count={4} />
            ) : errorMsg ? (
              <ErrorStateView message={errorMsg} onRetry={() => loadData()} />
            ) : books.length === 0 ? (
              <EmptyStateView
                title="Kho sách đang cập nhật"
                description="Hiện chưa có cuốn sách nào trong danh mục này."
                actionLabel="Tải lại trang"
                onAction={() => loadData()}
              />
            ) : (
              <>
                {/* Flash Sale Section with Countdown */}
                <View style={styles.flashSection}>
                  <View style={styles.flashHeader}>
                    <View style={styles.flashTitleRow}>
                      <Ionicons name="flash" size={20} color={colors.accent} />
                      <Text style={styles.flashTitle}>FLASH SALE GIỜ VÀNG</Text>
                    </View>
                    <View style={styles.timerRow}>
                      <View style={styles.timerBox}>
                        <Text style={styles.timerText}>{hours}</Text>
                      </View>
                      <Text style={styles.timerColon}>:</Text>
                      <View style={styles.timerBox}>
                        <Text style={styles.timerText}>{mins}</Text>
                      </View>
                      <Text style={styles.timerColon}>:</Text>
                      <View style={styles.timerBox}>
                        <Text style={styles.timerText}>{secs}</Text>
                      </View>
                    </View>
                  </View>

                  <ScrollView
                    horizontal
                    showsHorizontalScrollIndicator={false}
                    contentContainerStyle={styles.horizontalBookList}
                  >
                    {flashSaleBooks.map((book) => (
                      <BookCard key={book.id} book={book} compact />
                    ))}
                  </ScrollView>
                </View>

                {/* Best Sellers Grid */}
                <View style={styles.sectionHeader}>
                  <Text style={styles.sectionTitle}>Sách bán chạy nhất tuần</Text>
                  <Pressable onPress={() => router.push('/(tabs)/catalog')}>
                    <Text style={styles.seeAllText}>Khám phá thêm</Text>
                  </Pressable>
                </View>

                <View style={styles.bookGrid}>
                  {bestSellerBooks.map((book) => (
                    <View key={book.id} style={styles.gridCol}>
                      <BookCard book={book} />
                    </View>
                  ))}
                </View>
              </>
            )}
          </>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.background,
  },
  topHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 14,
    paddingVertical: 8,
    backgroundColor: colors.surface,
  },
  greetingSub: {
    fontSize: 11,
    fontWeight: '600',
    color: colors.primary,
  },
  greetingTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  iconBtn: {
    width: 38,
    height: 38,
    borderRadius: 10,
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badge: {
    position: 'absolute',
    top: 3,
    right: 3,
    minWidth: 16,
    height: 16,
    borderRadius: 8,
    paddingHorizontal: 3,
    backgroundColor: colors.danger,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeText: {
    color: '#FFFFFF',
    fontSize: 9.5,
    fontWeight: '700',
  },
  searchBarRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 14,
    paddingBottom: 10,
    backgroundColor: colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  searchBox: {
    flex: 1,
    minHeight: 38,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 12,
    borderRadius: 8,
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.border,
  },
  searchInput: {
    flex: 1,
    fontSize: 12.5,
    color: colors.textPrimary,
    paddingVertical: 6,
  },
  supportQuickBtn: {
    minHeight: 38,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    paddingHorizontal: 10,
    borderRadius: 8,
    backgroundColor: colors.primarySoft,
    borderWidth: 1,
    borderColor: colors.primaryBorder,
  },
  supportQuickBtnText: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.primary,
  },
  scrollContent: {
    paddingBottom: 90,
    gap: 12,
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 14,
    marginTop: 2,
  },
  sectionTitle: {
    fontSize: 14.5,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  seeAllText: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.primary,
  },
  categoryRow: {
    paddingHorizontal: 14,
    gap: 8,
  },
  categoryChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 10,
    paddingVertical: 8,
    borderRadius: 10,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
  },
  categoryIconBox: {
    width: 30,
    height: 30,
    borderRadius: 8,
    backgroundColor: colors.primarySoft,
    alignItems: 'center',
    justifyContent: 'center',
  },
  categoryName: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  categoryCount: {
    fontSize: 10.5,
    color: colors.textMuted,
  },
  flashSection: {
    marginHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 12,
    backgroundColor: '#FFFBEB',
    borderWidth: 1,
    borderColor: '#FDE68A',
    gap: 10,
  },
  flashHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 12,
  },
  flashTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
  },
  flashTitle: {
    fontSize: 13.5,
    fontWeight: '800',
    color: colors.accent,
  },
  timerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  timerBox: {
    minWidth: 28,
    paddingHorizontal: 6,
    paddingVertical: 4,
    borderRadius: 6,
    backgroundColor: colors.accent,
    alignItems: 'center',
  },
  timerText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '700',
    fontVariant: ['tabular-nums'],
  },
  timerColon: {
    fontSize: 13,
    fontWeight: '700',
    color: colors.accent,
  },
  horizontalBookList: {
    paddingHorizontal: 14,
    gap: 12,
  },
  bookGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: 16,
    gap: 12,
  },
  gridCol: {
    width: '48%',
  },
});
