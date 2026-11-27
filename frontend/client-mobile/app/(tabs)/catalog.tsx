import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useLocalSearchParams } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Book } from '@shared/types';
import { colors } from '@/theme/colors';
import {
  bookService,
  CatalogSortBy,
  CategoryTreeItem,
} from '@/services/bookService';
import { BookCard } from '@/components/common/BookCard';
import {
  BookGridSkeleton,
  EmptyStateView,
  ErrorStateView,
} from '@/components/common/StateViews';

type PriceRangeKey = 'ALL' | 'UNDER_150K' | '150K_300K' | 'OVER_300K';

const PAGE_SIZE = 20;

const TRENDING_KEYWORDS = [
  'Nguyễn Nhật Ánh',
  'Sherlock Holmes',
  'Lập trình',
  'Tâm lý',
  'Kinh tế',
  'Dế Mèn',
];

const SORT_OPTIONS: { key: CatalogSortBy; label: string }[] = [
  { key: 'popular', label: 'Bán chạy' },
  { key: 'price_asc', label: 'Giá thấp → cao' },
  { key: 'price_desc', label: 'Giá cao → thấp' },
  { key: 'discount_desc', label: 'Giảm sâu nhất' },
  { key: 'rating', label: 'Đánh giá ★' },
  { key: 'newest', label: 'Mới nhất' },
  { key: 'title_asc', label: 'Tên A → Z' },
  { key: 'title_desc', label: 'Tên Z → A' },
];

export default function CatalogScreen() {
  const params = useLocalSearchParams<{ categoryId?: string; flashSaleOnly?: string }>();
  const scrollRef = useRef<ScrollView>(null);

  const [searchQuery, setSearchQuery] = useState('');
  const [searchHistory, setSearchHistory] = useState<string[]>(() =>
    bookService.getSearchHistory()
  );
  const [selectedCategory, setSelectedCategory] = useState<string>(
    params.categoryId || 'ALL'
  );
  const [priceRange, setPriceRange] = useState<PriceRangeKey>('ALL');
  const [flashSaleOnly, setFlashSaleOnly] = useState(params.flashSaleOnly === 'true');
  const [inStockOnly, setInStockOnly] = useState(false);
  const [minRatingFourHalf, setMinRatingFourHalf] = useState(false);
  const [sortBy, setSortBy] = useState<CatalogSortBy>('popular');

  // Phân trang 1-based: luôn bắt đầu từ Trang 1 thay vì 0
  const [currentPage, setCurrentPage] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [categories, setCategories] = useState<CategoryTreeItem[]>([]);
  const [books, setBooks] = useState<Book[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (params.categoryId) {
      setSelectedCategory(params.categoryId);
      setCurrentPage(1);
    }
  }, [params.categoryId]);

  useEffect(() => {
    if (params.flashSaleOnly === 'true') {
      setFlashSaleOnly(true);
      setCurrentPage(1);
    }
  }, [params.flashSaleOnly]);

  // Tải cây danh mục Cha - Con 1 lần khi mở tab Danh mục
  useEffect(() => {
    let active = true;
    bookService
      .getCategories()
      .then((catList) => {
        if (active) {
          setCategories(catList.filter((c) => c.parentId === null));
        }
      })
      .catch(() => {
        // Lỗi danh mục sẽ hiển thị ở fetchBooks nếu Backend chưa bật
      });
    return () => {
      active = false;
    };
  }, []);

  // Tìm danh mục cha đang được chọn (dù người dùng bấm cha hay bấm 1 danh mục con) để hiển thị hàng chip con
  const activeRootCategory = useMemo(() => {
    if (selectedCategory === 'ALL') return undefined;
    return categories.find(
      (root) =>
        root.id === selectedCategory ||
        root.children.some((child) => child.id === selectedCategory)
    );
  }, [categories, selectedCategory]);

  const fetchBooks = async (targetPage = currentPage) => {
    try {
      setIsLoading(true);
      setErrorMsg(null);
      let minPrice: number | undefined;
      let maxPrice: number | undefined;
      if (priceRange === 'UNDER_150K') {
        maxPrice = 150000;
      } else if (priceRange === '150K_300K') {
        minPrice = 150000;
        maxPrice = 300000;
      } else if (priceRange === 'OVER_300K') {
        minPrice = 300000;
      }

      const res = await bookService.getCatalogBooksPage({
        search: searchQuery,
        categoryId: selectedCategory,
        minPrice,
        maxPrice,
        minRating: minRatingFourHalf ? 4.5 : undefined,
        flashSaleOnly,
        inStockOnly,
        sortBy,
        page: targetPage,
        size: PAGE_SIZE,
      });

      setBooks(res.items);
      setTotalElements(res.totalElements);
      setTotalPages(res.totalPages);
      setCurrentPage(res.page);
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'Không thể tải danh mục sách');
    } finally {
      setIsLoading(false);
    }
  };

  // Gọi API khi bộ lọc, sắp xếp hoặc số trang thay đổi (debounce nhẹ 200ms khi gõ từ khóa tìm kiếm)
  useEffect(() => {
    const delay = searchQuery.trim().length > 0 ? 200 : 0;
    const timer = setTimeout(() => {
      fetchBooks(currentPage);
    }, delay);
    return () => clearTimeout(timer);
  }, [
    searchQuery,
    selectedCategory,
    priceRange,
    flashSaleOnly,
    inStockOnly,
    minRatingFourHalf,
    sortBy,
    currentPage,
  ]);

  const handleSelectCategory = (categoryId: string) => {
    setSelectedCategory(categoryId);
    setCurrentPage(1);
  };

  const handleSelectPage = (nextPage: number) => {
    if (nextPage < 1 || (totalPages > 0 && nextPage > totalPages) || nextPage === currentPage) {
      return;
    }
    setCurrentPage(nextPage);
    scrollRef.current?.scrollTo({ y: 0, animated: true });
  };

  const resetFilters = () => {
    setSearchQuery('');
    setSelectedCategory('ALL');
    setPriceRange('ALL');
    setFlashSaleOnly(false);
    setInStockOnly(false);
    setMinRatingFourHalf(false);
    setSortBy('popular');
    setCurrentPage(1);
  };

  const hasActiveFilter =
    Boolean(searchQuery.trim()) ||
    selectedCategory !== 'ALL' ||
    priceRange !== 'ALL' ||
    flashSaleOnly ||
    inStockOnly ||
    minRatingFourHalf ||
    sortBy !== 'popular';

  // Tạo danh sách các nút số trang gọn đẹp bắt đầu từ 1 (hiện trang 1, trang cuối và vùng quanh trang hiện tại)
  const visiblePageNumbers = useMemo(() => {
    if (totalPages <= 1) return [];
    const pages = new Set<number>([1, totalPages]);
    for (let p = currentPage - 1; p <= currentPage + 1; p++) {
      if (p >= 1 && p <= totalPages) pages.add(p);
    }
    return Array.from(pages).sort((a, b) => a - b);
  }, [currentPage, totalPages]);

  const handleApplyKeyword = (keyword: string) => {
    const clean = keyword.trim();
    if (!clean) return;
    setSearchQuery(clean);
    setCurrentPage(1);
    setSearchHistory(bookService.addSearchHistory(clean));
  };

  return (
    <SafeAreaView edges={['top']} style={styles.safeArea}>
      {/* Search Header */}
      <View style={styles.header}>
        <Text style={styles.screenTitle}>Khám phá & Tìm kiếm Sách</Text>
        <View style={styles.searchBox}>
          <Ionicons name="search-outline" size={18} color={colors.textMuted} />
          <TextInput
            value={searchQuery}
            onChangeText={(text) => {
              setSearchQuery(text);
              setCurrentPage(1);
            }}
            onSubmitEditing={() => {
              if (searchQuery.trim().length >= 2) {
                setSearchHistory(bookService.addSearchHistory(searchQuery));
              }
            }}
            placeholder="Nhập tên sách, tác giả, mã ISBN..."
            placeholderTextColor={colors.textMuted}
            style={styles.searchInput}
            returnKeyType="search"
          />
          {searchQuery.length > 0 ? (
            <Pressable
              onPress={() => {
                setSearchQuery('');
                setCurrentPage(1);
              }}
              hitSlop={8}
            >
              <Ionicons name="close-circle" size={18} color={colors.textMuted} />
            </Pressable>
          ) : null}
        </View>

        {/* Lịch sử tìm kiếm & Từ khóa phổ biến */}
        {searchQuery.trim().length === 0 ? (
          <View style={{ gap: 6, marginTop: 8 }}>
            {searchHistory.length > 0 ? (
              <View style={{ gap: 4 }}>
                <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Text style={{ fontSize: 11, fontWeight: '700', color: colors.textSecondary }}>
                    Tìm kiếm gần đây
                  </Text>
                  <Pressable
                    onPress={() => {
                      bookService.clearSearchHistory();
                      setSearchHistory([]);
                    }}
                    hitSlop={6}
                  >
                    <Text style={{ fontSize: 11, color: colors.textMuted, fontWeight: '600' }}>
                      Xóa lịch sử
                    </Text>
                  </Pressable>
                </View>
                <ScrollView
                  horizontal
                  showsHorizontalScrollIndicator={false}
                  contentContainerStyle={{ gap: 6 }}
                >
                  {searchHistory.map((kw) => (
                    <View
                      key={`hist-${kw}`}
                      style={{
                        flexDirection: 'row',
                        alignItems: 'center',
                        gap: 4,
                        paddingLeft: 10,
                        paddingRight: 6,
                        paddingVertical: 4,
                        borderRadius: 999,
                        backgroundColor: colors.background,
                        borderWidth: 1,
                        borderColor: colors.border,
                      }}
                    >
                      <Pressable onPress={() => handleApplyKeyword(kw)}>
                        <Text style={{ fontSize: 11.5, color: colors.textPrimary }}>{kw}</Text>
                      </Pressable>
                      <Pressable
                        onPress={() => setSearchHistory(bookService.removeSearchHistoryItem(kw))}
                        hitSlop={4}
                      >
                        <Ionicons name="close" size={13} color={colors.textMuted} />
                      </Pressable>
                    </View>
                  ))}
                </ScrollView>
              </View>
            ) : null}

            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={{ gap: 6, alignItems: 'center' }}
            >
              <Ionicons name="flame-outline" size={14} color={colors.accent} />
              {TRENDING_KEYWORDS.map((kw) => (
                <Pressable
                  key={`trend-${kw}`}
                  onPress={() => handleApplyKeyword(kw)}
                  style={{
                    paddingHorizontal: 10,
                    paddingVertical: 4,
                    borderRadius: 999,
                    backgroundColor: '#FFF7ED',
                    borderWidth: 1,
                    borderColor: '#FED7AA',
                  }}
                >
                  <Text style={{ fontSize: 11.5, fontWeight: '600', color: '#C2410C' }}>
                    {kw}
                  </Text>
                </Pressable>
              ))}
            </ScrollView>
          </View>
        ) : null}
      </View>

      {/* Multi-Criteria Filter & Sort Bar */}
      <View style={styles.filterContainer}>
        {/* Hàng 1: Danh mục cha */}
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.filterScroll}
        >
          <Pressable
            onPress={() => handleSelectCategory('ALL')}
            style={[styles.chip, selectedCategory === 'ALL' && styles.chipActive]}
          >
            <Text
              style={[
                styles.chipText,
                selectedCategory === 'ALL' && styles.chipTextActive,
              ]}
            >
              Tất cả danh mục
            </Text>
          </Pressable>
          {categories.map((cat) => {
            const isSelected =
              selectedCategory === cat.id ||
              cat.children.some((ch) => ch.id === selectedCategory);
            return (
              <Pressable
                key={cat.id}
                onPress={() => handleSelectCategory(cat.id)}
                style={[styles.chip, isSelected && styles.chipActive]}
              >
                <Text style={[styles.chipText, isSelected && styles.chipTextActive]}>
                  {cat.name} ({cat.bookCount})
                </Text>
              </Pressable>
            );
          })}
        </ScrollView>

        {/* Hàng con (chỉ hiện khi danh mục cha đang chọn có danh mục con) */}
        {activeRootCategory && activeRootCategory.children.length > 0 ? (
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.filterScroll}
          >
            <Pressable
              onPress={() => handleSelectCategory(activeRootCategory.id)}
              style={[
                styles.subChip,
                selectedCategory === activeRootCategory.id && styles.subChipActive,
              ]}
            >
              <Text
                style={[
                  styles.subChipText,
                  selectedCategory === activeRootCategory.id && styles.subChipTextActive,
                ]}
              >
                Tất cả {activeRootCategory.name}
              </Text>
            </Pressable>
            {activeRootCategory.children.map((child) => (
              <Pressable
                key={child.id}
                onPress={() => handleSelectCategory(child.id)}
                style={[
                  styles.subChip,
                  selectedCategory === child.id && styles.subChipActive,
                ]}
              >
                <Text
                  style={[
                    styles.subChipText,
                    selectedCategory === child.id && styles.subChipTextActive,
                  ]}
                >
                  {child.name} ({child.bookCount})
                </Text>
              </Pressable>
            ))}
          </ScrollView>
        ) : null}

        {/* Hàng 2: Lọc khoảng giá & lọc nhanh TMĐT */}
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.filterScroll}
        >
          {(
            [
              { key: 'ALL', label: 'Mọi mức giá' },
              { key: 'UNDER_150K', label: 'Dưới 150k' },
              { key: '150K_300K', label: '150k - 300k' },
              { key: 'OVER_300K', label: 'Trên 300k' },
            ] as { key: PriceRangeKey; label: string }[]
          ).map((item) => (
            <Pressable
              key={item.key}
              onPress={() => {
                setPriceRange(item.key);
                setCurrentPage(1);
              }}
              style={[
                styles.subChip,
                priceRange === item.key && styles.subChipActive,
              ]}
            >
              <Text
                style={[
                  styles.subChipText,
                  priceRange === item.key && styles.subChipTextActive,
                ]}
              >
                {item.label}
              </Text>
            </Pressable>
          ))}

          <View style={styles.divider} />

          <Pressable
            onPress={() => {
              setFlashSaleOnly((prev) => !prev);
              setCurrentPage(1);
            }}
            style={[styles.subChip, flashSaleOnly && styles.subChipActive]}
          >
            <Text style={[styles.subChipText, flashSaleOnly && styles.subChipTextActive]}>
              ⚡ Flash Sale
            </Text>
          </Pressable>

          <Pressable
            onPress={() => {
              setInStockOnly((prev) => !prev);
              setCurrentPage(1);
            }}
            style={[styles.subChip, inStockOnly && styles.subChipActive]}
          >
            <Text style={[styles.subChipText, inStockOnly && styles.subChipTextActive]}>
              Còn hàng
            </Text>
          </Pressable>

          <Pressable
            onPress={() => {
              setMinRatingFourHalf((prev) => !prev);
              setCurrentPage(1);
            }}
            style={[styles.subChip, minRatingFourHalf && styles.subChipActive]}
          >
            <Text style={[styles.subChipText, minRatingFourHalf && styles.subChipTextActive]}>
              Từ 4.5★
            </Text>
          </Pressable>
        </ScrollView>

        {/* Hàng 3: 8 kiểu sắp xếp chuẩn sàn TMĐT */}
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.filterScroll}
        >
          <Text style={styles.sortLabel}>Sắp xếp:</Text>
          {SORT_OPTIONS.map((s) => (
            <Pressable
              key={s.key}
              onPress={() => {
                setSortBy(s.key);
                setCurrentPage(1);
              }}
              style={[styles.subChip, sortBy === s.key && styles.subChipActive]}
            >
              <Text
                style={[
                  styles.subChipText,
                  sortBy === s.key && styles.subChipTextActive,
                ]}
              >
                {s.label}
              </Text>
            </Pressable>
          ))}
        </ScrollView>
      </View>

      {/* Results List */}
      <ScrollView
        ref={scrollRef}
        contentInsetAdjustmentBehavior="automatic"
        keyboardShouldPersistTaps="handled"
        contentContainerStyle={styles.resultsScroll}
      >
        <View style={styles.resultMetaRow}>
          <Text style={styles.resultCountText}>
            Tìm thấy <Text style={{ fontWeight: '700' }}>{totalElements}</Text> đầu sách phù hợp
            {totalPages > 0 ? ` • Trang ${currentPage} / ${totalPages}` : ''}
          </Text>
          {hasActiveFilter ? (
            <Pressable onPress={resetFilters}>
              <Text style={styles.resetText}>Xóa bộ lọc</Text>
            </Pressable>
          ) : null}
        </View>

        {isLoading ? (
          <BookGridSkeleton count={6} />
        ) : errorMsg ? (
          <ErrorStateView message={errorMsg} onRetry={() => fetchBooks(currentPage)} />
        ) : books.length === 0 ? (
          <EmptyStateView
            icon="search-outline"
            title="Không tìm thấy cuốn sách phù hợp"
            description="Hãy thử từ khóa khác hoặc mở rộng khoảng giá lọc để xem thêm nhiều tựa sách hay."
            actionLabel="Xóa bộ lọc tìm kiếm"
            onAction={resetFilters}
          />
        ) : (
          <>
            <View style={styles.grid}>
              {books.map((book) => (
                <View key={book.id} style={styles.gridCol}>
                  <BookCard book={book} />
                </View>
              ))}
            </View>

            {/* Thanh phân trang bắt đầu từ số 1 */}
            {totalPages > 1 ? (
              <View style={styles.paginationBar}>
                <Pressable
                  onPress={() => handleSelectPage(currentPage - 1)}
                  disabled={currentPage <= 1}
                  style={[
                    styles.pageNavBtn,
                    currentPage <= 1 && styles.pageBtnDisabled,
                  ]}
                >
                  <Ionicons
                    name="chevron-back"
                    size={16}
                    color={currentPage <= 1 ? colors.textMuted : colors.textPrimary}
                  />
                  <Text
                    style={[
                      styles.pageNavText,
                      currentPage <= 1 && { color: colors.textMuted },
                    ]}
                  >
                    Trước
                  </Text>
                </Pressable>

                <View style={styles.pageNumbersWrap}>
                  {visiblePageNumbers.map((pageNum, idx) => {
                    const prevPageNum = visiblePageNumbers[idx - 1];
                    const showGap = prevPageNum !== undefined && pageNum - prevPageNum > 1;
                    const active = pageNum === currentPage;
                    return (
                      <React.Fragment key={pageNum}>
                        {showGap ? <Text style={styles.pageEllipsis}>…</Text> : null}
                        <Pressable
                          onPress={() => handleSelectPage(pageNum)}
                          style={[styles.pageNumBtn, active && styles.pageNumBtnActive]}
                        >
                          <Text
                            style={[styles.pageNumText, active && styles.pageNumTextActive]}
                          >
                            {pageNum}
                          </Text>
                        </Pressable>
                      </React.Fragment>
                    );
                  })}
                </View>

                <Pressable
                  onPress={() => handleSelectPage(currentPage + 1)}
                  disabled={currentPage >= totalPages}
                  style={[
                    styles.pageNavBtn,
                    currentPage >= totalPages && styles.pageBtnDisabled,
                  ]}
                >
                  <Text
                    style={[
                      styles.pageNavText,
                      currentPage >= totalPages && { color: colors.textMuted },
                    ]}
                  >
                    Sau
                  </Text>
                  <Ionicons
                    name="chevron-forward"
                    size={16}
                    color={currentPage >= totalPages ? colors.textMuted : colors.textPrimary}
                  />
                </Pressable>
              </View>
            ) : null}
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
  header: {
    paddingHorizontal: 14,
    paddingTop: 6,
    paddingBottom: 10,
    backgroundColor: colors.surface,
    gap: 8,
  },
  screenTitle: {
    fontSize: 15.5,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  searchBox: {
    minHeight: 38,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 10,
    borderRadius: 8,
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.border,
  },
  searchInput: {
    flex: 1,
    fontSize: 12.5,
    color: colors.textPrimary,
  },
  filterContainer: {
    backgroundColor: colors.surface,
    paddingBottom: 8,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
    gap: 6,
  },
  filterScroll: {
    paddingHorizontal: 14,
    gap: 6,
    alignItems: 'center',
  },
  sortLabel: {
    fontSize: 11,
    fontWeight: '700',
    color: colors.textSecondary,
    marginRight: 2,
  },
  chip: {
    minHeight: 30,
    paddingHorizontal: 11,
    paddingVertical: 5,
    borderRadius: 999,
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.border,
    justifyContent: 'center',
  },
  chipActive: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  chipText: {
    fontSize: 11.5,
    fontWeight: '600',
    color: colors.textSecondary,
  },
  chipTextActive: {
    color: '#FFFFFF',
  },
  subChip: {
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 6,
    backgroundColor: colors.surfaceMuted,
  },
  subChipActive: {
    backgroundColor: colors.primarySoft,
    borderWidth: 1,
    borderColor: colors.primary,
  },
  subChipText: {
    fontSize: 11,
    fontWeight: '500',
    color: colors.textSecondary,
  },
  subChipTextActive: {
    color: colors.primary,
    fontWeight: '700',
  },
  divider: {
    width: 1,
    height: 16,
    backgroundColor: colors.border,
    marginHorizontal: 4,
  },
  resultsScroll: {
    paddingBottom: 90,
  },
  resultMetaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 14,
    paddingVertical: 10,
  },
  resultCountText: {
    fontSize: 12,
    color: colors.textSecondary,
  },
  resetText: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.danger,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: 16,
    gap: 12,
  },
  gridCol: {
    width: '48%',
  },
  paginationBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingTop: 18,
    paddingBottom: 8,
    gap: 8,
  },
  pageNavBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 10,
    paddingVertical: 7,
    borderRadius: 8,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
  },
  pageBtnDisabled: {
    opacity: 0.45,
  },
  pageNavText: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  pageNumbersWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    flexWrap: 'wrap',
    justifyContent: 'center',
  },
  pageNumBtn: {
    minWidth: 32,
    height: 32,
    paddingHorizontal: 8,
    borderRadius: 8,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pageNumBtnActive: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  pageNumText: {
    fontSize: 12.5,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  pageNumTextActive: {
    color: '#FFFFFF',
    fontWeight: '700',
  },
  pageEllipsis: {
    fontSize: 13,
    color: colors.textMuted,
    paddingHorizontal: 2,
  },
});
