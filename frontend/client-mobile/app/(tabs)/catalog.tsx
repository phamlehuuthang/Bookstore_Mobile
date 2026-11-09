import React, { useEffect, useState } from 'react';
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
import { Book, Category } from '@shared/types';
import { colors } from '@/theme/colors';
import { bookService } from '@/services/bookService';
import { BookCard } from '@/components/common/BookCard';
import {
  BookGridSkeleton,
  EmptyStateView,
  ErrorStateView,
} from '@/components/common/StateViews';

type PriceRangeKey = 'ALL' | 'UNDER_150K' | '150K_300K' | 'OVER_300K';
type SortOptionKey = 'popular' | 'price_asc' | 'price_desc' | 'rating' | 'newest';

export default function CatalogScreen() {
  const params = useLocalSearchParams<{ categoryId?: string }>();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>(
    params.categoryId || 'ALL'
  );
  const [priceRange, setPriceRange] = useState<PriceRangeKey>('ALL');
  const [sortBy, setSortBy] = useState<SortOptionKey>('popular');

  const [categories, setCategories] = useState<Category[]>([]);
  const [books, setBooks] = useState<Book[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (params.categoryId) {
      setSelectedCategory(params.categoryId);
    }
  }, [params.categoryId]);

  const fetchCatalog = async () => {
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

      const [catList, bookList] = await Promise.all([
        bookService.getCategories(),
        bookService.getCatalogBooks({
          search: searchQuery,
          categoryId: selectedCategory,
          minPrice,
          maxPrice,
          sortBy,
        }),
      ]);
      setCategories(catList.filter((c) => c.parentId === null));
      setBooks(bookList);
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'Không thể tải danh mục sách');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchCatalog();
  }, [searchQuery, selectedCategory, priceRange, sortBy]);

  const resetFilters = () => {
    setSearchQuery('');
    setSelectedCategory('ALL');
    setPriceRange('ALL');
    setSortBy('popular');
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
            onChangeText={setSearchQuery}
            placeholder="Nhập tên sách, tác giả, mã ISBN..."
            placeholderTextColor={colors.textMuted}
            style={styles.searchInput}
            returnKeyType="search"
          />
          {searchQuery.length > 0 ? (
            <Pressable onPress={() => setSearchQuery('')} hitSlop={8}>
              <Ionicons name="close-circle" size={18} color={colors.textMuted} />
            </Pressable>
          ) : null}
        </View>
      </View>

      {/* Multi-Criteria Filter Bar */}
      <View style={styles.filterContainer}>
        {/* Category Filter */}
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.filterScroll}
        >
          <Pressable
            onPress={() => setSelectedCategory('ALL')}
            style={[
              styles.chip,
              selectedCategory === 'ALL' && styles.chipActive,
            ]}
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
          {categories.map((cat) => (
            <Pressable
              key={cat.id}
              onPress={() => setSelectedCategory(cat.id)}
              style={[
                styles.chip,
                selectedCategory === cat.id && styles.chipActive,
              ]}
            >
              <Text
                style={[
                  styles.chipText,
                  selectedCategory === cat.id && styles.chipTextActive,
                ]}
              >
                {cat.name}
              </Text>
            </Pressable>
          ))}
        </ScrollView>

        {/* Price & Sort Pills */}
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.filterScroll}
        >
          {(
            [
              { key: 'ALL', label: 'Mọi mức giá' },
              { key: 'UNDER_150K', label: 'Dưới 150.000 đ' },
              { key: '150K_300K', label: '150k - 300k' },
              { key: 'OVER_300K', label: 'Trên 300k' },
            ] as { key: PriceRangeKey; label: string }[]
          ).map((item) => (
            <Pressable
              key={item.key}
              onPress={() => setPriceRange(item.key)}
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

          {(
            [
              { key: 'popular', label: 'Bán chạy' },
              { key: 'price_asc', label: 'Giá thấp → cao' },
              { key: 'price_desc', label: 'Giá cao → thấp' },
              { key: 'rating', label: 'Đánh giá ★' },
            ] as { key: SortOptionKey; label: string }[]
          ).map((s) => (
            <Pressable
              key={s.key}
              onPress={() => setSortBy(s.key)}
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
        contentInsetAdjustmentBehavior="automatic"
        keyboardShouldPersistTaps="handled"
        contentContainerStyle={styles.resultsScroll}
      >
        <View style={styles.resultMetaRow}>
          <Text style={styles.resultCountText}>
            Tìm thấy <Text style={{ fontWeight: '700' }}>{books.length}</Text> đầu sách phù hợp
          </Text>
          {(searchQuery || selectedCategory !== 'ALL' || priceRange !== 'ALL') && (
            <Pressable onPress={resetFilters}>
              <Text style={styles.resetText}>Xóa bộ lọc</Text>
            </Pressable>
          )}
        </View>

        {isLoading ? (
          <BookGridSkeleton count={6} />
        ) : errorMsg ? (
          <ErrorStateView message={errorMsg} onRetry={fetchCatalog} />
        ) : books.length === 0 ? (
          <EmptyStateView
            icon="search-outline"
            title="Không tìm thấy cuốn sách phù hợp"
            description="Hãy thử từ khóa khác hoặc mở rộng khoảng giá lọc để xem thêm nhiều tựa sách hay."
            actionLabel="Xóa bộ lọc tìm kiếm"
            onAction={resetFilters}
          />
        ) : (
          <View style={styles.grid}>
            {books.map((book) => (
              <View key={book.id} style={styles.gridCol}>
                <BookCard book={book} />
              </View>
            ))}
          </View>
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
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 12,
    backgroundColor: colors.surface,
    gap: 10,
  },
  screenTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  searchBox: {
    minHeight: 44,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 12,
    borderRadius: 12,
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.border,
  },
  searchInput: {
    flex: 1,
    fontSize: 14,
    color: colors.textPrimary,
  },
  filterContainer: {
    backgroundColor: colors.surface,
    paddingBottom: 10,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
    gap: 8,
  },
  filterScroll: {
    paddingHorizontal: 16,
    gap: 8,
    alignItems: 'center',
  },
  chip: {
    minHeight: 36,
    paddingHorizontal: 14,
    paddingVertical: 8,
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
    fontSize: 13,
    fontWeight: '600',
    color: colors.textSecondary,
  },
  chipTextActive: {
    color: '#FFFFFF',
  },
  subChip: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
    backgroundColor: colors.surfaceMuted,
  },
  subChipActive: {
    backgroundColor: colors.primarySoft,
    borderWidth: 1,
    borderColor: colors.primary,
  },
  subChipText: {
    fontSize: 12,
    fontWeight: '500',
    color: colors.textSecondary,
  },
  subChipTextActive: {
    color: colors.primary,
    fontWeight: '700',
  },
  divider: {
    width: 1,
    height: 18,
    backgroundColor: colors.border,
    marginHorizontal: 4,
  },
  resultsScroll: {
    paddingBottom: 100,
  },
  resultMetaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  resultCountText: {
    fontSize: 13,
    color: colors.textSecondary,
  },
  resetText: {
    fontSize: 13,
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
});

