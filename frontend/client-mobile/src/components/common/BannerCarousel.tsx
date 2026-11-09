import React, { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme/colors';

interface BannerItem {
  id: string;
  tag: string;
  title: string;
  subtitle: string;
  voucherCode: string;
  bgColor: string;
}

const BANNERS: BannerItem[] = [
  {
    id: 'bn-1',
    tag: 'TUẦN LỄ SÁCH CÔNG NGHỆ & KINH TẾ',
    title: 'Giảm ngay 50.000 đ cho đơn từ 300K',
    subtitle: 'Nhập mã CHAOHE50K • Freeship toàn quốc • Bọc sách 3 lớp',
    voucherCode: 'CHAOHE50K',
    bgColor: '#065F46',
  },
  {
    id: 'bn-2',
    tag: 'ĐẶC QUYỀN ĐỘC GIẢ MỚI',
    title: 'Ưu đãi 15% Tủ Sách Tinh Hoa Thế Giới',
    subtitle: 'Nhập mã MOTSACH15 • Áp dụng cho Clean Code, Tâm Lý Học Về Tiền',
    voucherCode: 'MOTSACH15',
    bgColor: '#1E293B',
  },
];

export function BannerCarousel({ onSelectCategory }: { onSelectCategory?: (id: string) => void }) {
  const [activeIndex, setActiveIndex] = useState(0);

  return (
    <View style={styles.container}>
      <ScrollView
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        onScroll={(e) => {
          const idx = Math.round(
            e.nativeEvent.contentOffset.x / Math.max(1, e.nativeEvent.layoutMeasurement.width)
          );
          setActiveIndex(idx);
        }}
        scrollEventThrottle={32}
        contentContainerStyle={styles.scrollContent}
      >
        {BANNERS.map((banner) => (
          <View
            key={banner.id}
            style={[styles.bannerCard, { backgroundColor: banner.bgColor }]}
          >
            <View style={styles.tagPill}>
              <Ionicons name="sparkles" size={12} color="#FDE68A" />
              <Text style={styles.tagText}>{banner.tag}</Text>
            </View>
            <Text style={styles.bannerTitle} numberOfLines={2}>
              {banner.title}
            </Text>
            <Text style={styles.bannerSub} numberOfLines={2}>
              {banner.subtitle}
            </Text>
            <View style={styles.bannerActionRow}>
              <Pressable
                onPress={() => onSelectCategory?.('cat-tech')}
                style={({ pressed }) => [
                  styles.bannerBtn,
                  pressed && { opacity: 0.9, transform: [{ scale: 0.98 }] },
                ]}
              >
                <Text style={styles.bannerBtnText}>Khám phá ngay</Text>
                <Ionicons name="arrow-forward" size={14} color="#065F46" />
              </Pressable>
              <View style={styles.codeBadge}>
                <Text style={styles.codeBadgeText}>Mã: {banner.voucherCode}</Text>
              </View>
            </View>
          </View>
        ))}
      </ScrollView>

      <View style={styles.dotsRow}>
        {BANNERS.map((b, idx) => (
          <View
            key={b.id}
            style={[styles.dot, idx === activeIndex && styles.dotActive]}
          />
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginVertical: 8,
  },
  scrollContent: {
    paddingHorizontal: 16,
    gap: 12,
  },
  bannerCard: {
    width: 332,
    borderRadius: 16,
    padding: 16,
    gap: 8,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
  },
  tagPill: {
    alignSelf: 'flex-start',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: 'rgba(255,255,255,0.14)',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
  },
  tagText: {
    color: '#FDE68A',
    fontSize: 11,
    fontWeight: '700',
  },
  bannerTitle: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '700',
    lineHeight: 24,
  },
  bannerSub: {
    color: '#E2E8F0',
    fontSize: 13,
    lineHeight: 18,
  },
  bannerActionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 6,
  },
  bannerBtn: {
    minHeight: 38,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 10,
  },
  bannerBtnText: {
    color: '#065F46',
    fontSize: 13,
    fontWeight: '700',
  },
  codeBadge: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(253, 230, 138, 0.45)',
    backgroundColor: 'rgba(0,0,0,0.2)',
  },
  codeBadgeText: {
    color: '#FDE68A',
    fontSize: 12,
    fontWeight: '600',
  },
  dotsRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 6,
    marginTop: 8,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#CBD5E1',
  },
  dotActive: {
    width: 18,
    backgroundColor: colors.primary,
  },
});

