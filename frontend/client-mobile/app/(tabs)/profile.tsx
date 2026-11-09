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
import { formatCurrencyVND } from '@shared/types';
import { INITIAL_BOOKS, INITIAL_VOUCHERS } from '@shared/api-client/mockData';
import { colors } from '@/theme/colors';
import { useAuth } from '@/hooks/useAuth';

type ProfileSection = 'ADDRESSES' | 'VOUCHERS' | 'WISHLIST' | 'NOTIFICATIONS' | 'FAQ';
type AuthMode = 'LOGIN' | 'REGISTER' | 'FORGOT_OTP';

export default function ProfileScreen() {
  const router = useRouter();
  const {
    user,
    wishlistBookIds,
    notifications,
    login,
    register,
    logout,
    saveAddress,
    deleteAddress,
    toggleWishlist,
    markAllNotificationsRead,
  } = useAuth();

  const [activeSection, setActiveSection] = useState<ProfileSection>('ADDRESSES');

  // Auth Form State (When logged out or testing Role Guard)
  const [authMode, setAuthMode] = useState<AuthMode>('LOGIN');
  const [email, setEmail] = useState('minhtuan@gmail.com');
  const [password, setPassword] = useState('123456');
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [authError, setAuthError] = useState<string | null>(null);
  const [authSuccess, setAuthSuccess] = useState<string | null>(null);

  // New Address State
  const [showAddAddress, setShowAddAddress] = useState(false);
  const [addrName, setAddrName] = useState('');
  const [addrPhone, setAddrPhone] = useState('');
  const [addrStreet, setAddrStreet] = useState('');
  const [addrDistrict, setAddrDistrict] = useState('Quận 3');
  const [addrProvince, setAddrProvince] = useState('TP. Hồ Chí Minh');

  const handleAuthSubmit = async () => {
    setAuthError(null);
    setAuthSuccess(null);
    try {
      if (authMode === 'LOGIN') {
        await login(email, password);
      } else if (authMode === 'REGISTER') {
        await register({
          fullName: fullName || 'Độc giả mới',
          email,
          phone: phone || '0901234567',
          password,
        });
      } else {
        setAuthSuccess(
          `Mã OTP 6 số đã gửi tới ${email}. Mật khẩu của bạn đã được đặt lại thành công!`
        );
        setAuthMode('LOGIN');
      }
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : 'Đăng nhập thất bại');
    }
  };

  const handleSaveNewAddress = async () => {
    if (!addrStreet.trim()) return;
    await saveAddress({
      fullName: addrName.trim() || user?.fullName || 'Khách hàng',
      phone: addrPhone.trim() || user?.phone || '0908123456',
      province: addrProvince,
      district: addrDistrict,
      ward: 'Phường Võ Thị Sáu',
      streetAddress: addrStreet.trim(),
      isDefault: true,
    });
    setAddrStreet('');
    setShowAddAddress(false);
  };

  const wishlistBooks = INITIAL_BOOKS.filter((b) => wishlistBookIds.includes(b.id));

  // IF NOT LOGGED IN: SHOW AUTHENTICATION VIEW (WITH ROLE GUARD VERIFICATION)
  if (!user) {
    return (
      <SafeAreaView edges={['top']} style={styles.safeArea}>
        <KeyboardAvoidingView
          behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
          style={{ flex: 1 }}
        >
          <ScrollView
            keyboardShouldPersistTaps="handled"
            contentContainerStyle={styles.authContainer}
          >
            <View style={styles.authHero}>
              <View style={styles.logoCircle}>
                <Ionicons name="book" size={32} color="#FFFFFF" />
              </View>
              <Text style={styles.authTitle}>Tri Thức Việt BookStore</Text>
              <Text style={styles.authSubtitle}>
                Ứng dụng mua sách bản quyền dành riêng cho Khách hàng
              </Text>
            </View>

            {/* Auth Mode Switcher */}
            <View style={styles.authTabs}>
              <Pressable
                onPress={() => {
                  setAuthMode('LOGIN');
                  setAuthError(null);
                }}
                style={[styles.authTab, authMode === 'LOGIN' && styles.authTabActive]}
              >
                <Text
                  style={[
                    styles.authTabText,
                    authMode === 'LOGIN' && styles.authTabTextActive,
                  ]}
                >
                  Đăng nhập
                </Text>
              </Pressable>
              <Pressable
                onPress={() => {
                  setAuthMode('REGISTER');
                  setAuthError(null);
                }}
                style={[styles.authTab, authMode === 'REGISTER' && styles.authTabActive]}
              >
                <Text
                  style={[
                    styles.authTabText,
                    authMode === 'REGISTER' && styles.authTabTextActive,
                  ]}
                >
                  Đăng ký
                </Text>
              </Pressable>
              <Pressable
                onPress={() => {
                  setAuthMode('FORGOT_OTP');
                  setAuthError(null);
                }}
                style={[styles.authTab, authMode === 'FORGOT_OTP' && styles.authTabActive]}
              >
                <Text
                  style={[
                    styles.authTabText,
                    authMode === 'FORGOT_OTP' && styles.authTabTextActive,
                  ]}
                >
                  Quên MK (OTP)
                </Text>
              </Pressable>
            </View>

            {authError ? (
              <View style={styles.errorAlert}>
                <Ionicons name="alert-circle" size={20} color={colors.danger} />
                <Text selectable style={styles.errorAlertText}>
                  {authError}
                </Text>
              </View>
            ) : null}

            {authSuccess ? (
              <View style={styles.successAlert}>
                <Ionicons name="checkmark-circle" size={20} color={colors.primary} />
                <Text style={styles.successAlertText}>{authSuccess}</Text>
              </View>
            ) : null}

            <View style={styles.formCard}>
              {authMode === 'REGISTER' ? (
                <>
                  <Text style={styles.inputLabel}>Họ và tên</Text>
                  <TextInput
                    value={fullName}
                    onChangeText={setFullName}
                    placeholder="Nguyễn Minh Tuấn"
                    style={styles.input}
                  />
                  <Text style={styles.inputLabel}>Số điện thoại</Text>
                  <TextInput
                    value={phone}
                    onChangeText={setPhone}
                    placeholder="0908123456"
                    keyboardType="phone-pad"
                    style={styles.input}
                  />
                </>
              ) : null}

              <Text style={styles.inputLabel}>Địa chỉ Email</Text>
              <TextInput
                value={email}
                onChangeText={setEmail}
                placeholder="nhapemail@gmail.com"
                autoCapitalize="none"
                keyboardType="email-address"
                style={styles.input}
              />

              {authMode !== 'FORGOT_OTP' ? (
                <>
                  <Text style={styles.inputLabel}>Mật khẩu</Text>
                  <TextInput
                    value={password}
                    onChangeText={setPassword}
                    placeholder="••••••••"
                    secureTextEntry
                    style={styles.input}
                  />
                </>
              ) : (
                <>
                  <Text style={styles.inputLabel}>Mã xác thực OTP (6 chữ số)</Text>
                  <TextInput
                    value={otpCode}
                    onChangeText={setOtpCode}
                    placeholder="Nhập mã OTP gửi về điện thoại"
                    keyboardType="numeric"
                    style={styles.input}
                  />
                </>
              )}

              <Pressable onPress={handleAuthSubmit} style={styles.primaryBtn}>
                <Text style={styles.primaryBtnText}>
                  {authMode === 'LOGIN'
                    ? 'Đăng nhập ngay'
                    : authMode === 'REGISTER'
                    ? 'Tạo tài khoản mới'
                    : 'Xác nhận OTP & Đặt lại mật khẩu'}
                </Text>
              </Pressable>
            </View>
          </ScrollView>
        </KeyboardAvoidingView>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView edges={['top']} style={styles.safeArea}>
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        contentContainerStyle={styles.scrollContent}
      >
        {/* User Profile Header Card */}
        <View style={styles.profileCard}>
          <View style={styles.avatarCircle}>
            <Text style={styles.avatarInitials}>
              {user.fullName
                .split(' ')
                .slice(-2)
                .map((w) => w[0])
                .join('')}
            </Text>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.profileName}>{user.fullName}</Text>
            <Text style={styles.profileMeta}>
              {user.phone} • {user.email}
            </Text>
            <View style={styles.memberBadge}>
              <Ionicons name="shield-checkmark" size={12} color={colors.primary} />
              <Text style={styles.memberBadgeText}>
                Thành viên Vàng • Chi tiêu {formatCurrencyVND(user.totalSpent)}
              </Text>
            </View>
          </View>
          <Pressable onPress={logout} style={styles.logoutBtn}>
            <Ionicons name="log-out-outline" size={20} color={colors.danger} />
          </Pressable>
        </View>

        {/* Sub-navigation Pills */}
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.sectionTabs}
        >
          {(
            [
              { key: 'ADDRESSES', label: 'Sổ địa chỉ', icon: 'location-outline' },
              { key: 'VOUCHERS', label: 'Ví Voucher', icon: 'ticket-outline' },
              {
                key: 'WISHLIST',
                label: `Yêu thích (${wishlistBooks.length})`,
                icon: 'heart-outline',
              },
              {
                key: 'NOTIFICATIONS',
                label: `Thông báo (${notifications.length})`,
                icon: 'notifications-outline',
              },
              { key: 'FAQ', label: 'CSKH & FAQ', icon: 'help-buoy-outline' },
            ] as const
          ).map((tab) => {
            const active = activeSection === tab.key;
            return (
              <Pressable
                key={tab.key}
                onPress={() => setActiveSection(tab.key)}
                style={[styles.secChip, active && styles.secChipActive]}
              >
                <Ionicons
                  name={tab.icon}
                  size={16}
                  color={active ? '#FFFFFF' : colors.textSecondary}
                />
                <Text style={[styles.secChipText, active && styles.secChipTextActive]}>
                  {tab.label}
                </Text>
              </Pressable>
            );
          })}
        </ScrollView>

        {/* SECTION 1: ADDRESS BOOK */}
        {activeSection === 'ADDRESSES' ? (
          <View style={styles.sectionBlock}>
            <View style={styles.sectionTitleRow}>
              <Text style={styles.sectionTitle}>Sổ địa chỉ nhận hàng</Text>
              <Pressable onPress={() => setShowAddAddress(!showAddAddress)}>
                <Text style={styles.actionLink}>
                  {showAddAddress ? 'Đóng' : '+ Thêm địa chỉ'}
                </Text>
              </Pressable>
            </View>

            {showAddAddress ? (
              <View style={styles.addAddrCard}>
                <TextInput
                  value={addrName}
                  onChangeText={setAddrName}
                  placeholder="Họ tên người nhận"
                  style={styles.input}
                />
                <TextInput
                  value={addrPhone}
                  onChangeText={setAddrPhone}
                  placeholder="Số điện thoại"
                  keyboardType="phone-pad"
                  style={styles.input}
                />
                <TextInput
                  value={addrStreet}
                  onChangeText={setAddrStreet}
                  placeholder="Số nhà, tên đường, tòa nhà..."
                  style={styles.input}
                />
                <View style={{ flexDirection: 'row', gap: 8 }}>
                  <TextInput
                    value={addrDistrict}
                    onChangeText={setAddrDistrict}
                    placeholder="Quận/Huyện"
                    style={[styles.input, { flex: 1 }]}
                  />
                  <TextInput
                    value={addrProvince}
                    onChangeText={setAddrProvince}
                    placeholder="Tỉnh/Thành phố"
                    style={[styles.input, { flex: 1 }]}
                  />
                </View>
                <Pressable onPress={handleSaveNewAddress} style={styles.primaryBtn}>
                  <Text style={styles.primaryBtnText}>Lưu địa chỉ mặc định</Text>
                </Pressable>
              </View>
            ) : null}

            {user.addresses.map((addr) => (
              <View key={addr.id} style={styles.itemCard}>
                <View style={styles.itemCardHeader}>
                  <Text style={styles.itemCardTitle}>
                    {addr.fullName} • {addr.phone}
                  </Text>
                  {addr.isDefault ? (
                    <View style={styles.defaultTag}>
                      <Text style={styles.defaultTagText}>Mặc định</Text>
                    </View>
                  ) : null}
                </View>
                <Text style={styles.itemCardSub}>
                  {addr.streetAddress}, {addr.ward}, {addr.district}, {addr.province}
                </Text>
                <View style={styles.addrActions}>
                  {!addr.isDefault ? (
                    <Pressable
                      onPress={() => saveAddress({ ...addr, isDefault: true })}
                    >
                      <Text style={styles.actionLink}>Đặt làm mặc định</Text>
                    </Pressable>
                  ) : null}
                  <Pressable onPress={() => deleteAddress(addr.id)}>
                    <Text style={[styles.actionLink, { color: colors.danger }]}>Xóa</Text>
                  </Pressable>
                </View>
              </View>
            ))}
          </View>
        ) : null}

        {/* SECTION 2: VOUCHER WALLET */}
        {activeSection === 'VOUCHERS' ? (
          <View style={styles.sectionBlock}>
            <Text style={styles.sectionTitle}>Ví Voucher Ưu Đãi Của Bạn</Text>
            {INITIAL_VOUCHERS.map((v) => (
              <View key={v.id} style={styles.voucherCard}>
                <View style={styles.voucherLeft}>
                  <Ionicons name="ticket" size={24} color="#FFFFFF" />
                  <Text style={styles.voucherCodeText}>{v.code}</Text>
                </View>
                <View style={{ flex: 1, gap: 4 }}>
                  <Text style={styles.itemCardTitle}>{v.title}</Text>
                  <Text style={styles.itemCardSub}>{v.description}</Text>
                  <Text style={styles.voucherExpiry}>
                    Đơn tối thiểu {formatCurrencyVND(v.minOrderValue)} • HSD: {v.endDate}
                  </Text>
                </View>
                <Pressable
                  onPress={() => router.push('/cart')}
                  style={styles.useVoucherBtn}
                >
                  <Text style={styles.useVoucherBtnText}>Dùng ngay</Text>
                </Pressable>
              </View>
            ))}
          </View>
        ) : null}

        {/* SECTION 3: WISHLIST */}
        {activeSection === 'WISHLIST' ? (
          <View style={styles.sectionBlock}>
            <Text style={styles.sectionTitle}>Tủ sách yêu thích</Text>
            {wishlistBooks.map((book) => (
              <Pressable
                key={book.id}
                onPress={() => router.push(`/book/${book.id}`)}
                style={styles.itemCard}
              >
                <View style={styles.itemCardHeader}>
                  <Text style={[styles.itemCardTitle, { flex: 1 }]} numberOfLines={1}>
                    {book.title}
                  </Text>
                  <Pressable onPress={() => toggleWishlist(book.id)} hitSlop={8}>
                    <Ionicons name="heart" size={20} color={colors.danger} />
                  </Pressable>
                </View>
                <Text style={styles.itemCardSub}>{book.authorName}</Text>
                <Text style={{ fontSize: 14, fontWeight: '700', color: colors.primary }}>
                  {formatCurrencyVND(book.flashSalePrice ?? book.price)}
                </Text>
              </Pressable>
            ))}
          </View>
        ) : null}

        {/* SECTION 4: NOTIFICATIONS */}
        {activeSection === 'NOTIFICATIONS' ? (
          <View style={styles.sectionBlock}>
            <View style={styles.sectionTitleRow}>
              <Text style={styles.sectionTitle}>Thông báo đơn hàng & Khuyến mãi</Text>
              <Pressable onPress={markAllNotificationsRead}>
                <Text style={styles.actionLink}>Đánh dấu đã đọc</Text>
              </Pressable>
            </View>
            {notifications.map((n) => (
              <View
                key={n.id}
                style={[
                  styles.itemCard,
                  !n.isRead && { backgroundColor: colors.primarySoft },
                ]}
              >
                <Text style={styles.itemCardTitle}>{n.title}</Text>
                <Text style={styles.itemCardSub}>{n.message}</Text>
                <Text style={styles.voucherExpiry}>{n.createdAt}</Text>
              </View>
            ))}
          </View>
        ) : null}

        {/* SECTION 5: CSKH & FAQ */}
        {activeSection === 'FAQ' ? (
          <View style={styles.sectionBlock}>
            <Text style={styles.sectionTitle}>Trung tâm Hỗ trợ Khách hàng (CSKH)</Text>
            <View style={styles.itemCard}>
              <Text style={styles.itemCardTitle}>Hotline CSKH: 1900 6886 (8:00 - 21:00)</Text>
              <Text style={styles.itemCardSub}>
                Email: hotro@bookstore.vn • Cam kết sách bản quyền 100% từ NXB Trẻ, NXB Tổng Hợp.
              </Text>
            </View>
            <View style={styles.itemCard}>
              <Text style={styles.itemCardTitle}>
                1. Khi nào đơn hàng được miễn phí vận chuyển?
              </Text>
              <Text style={styles.itemCardSub}>
                Mọi đơn hàng từ 300.000 đ trở lên được Freeship tự động toàn quốc.
              </Text>
            </View>
            <View style={styles.itemCard}>
              <Text style={styles.itemCardTitle}>
                2. Chính sách đổi trả sách bị lỗi in ấn như thế nào?
              </Text>
              <Text style={styles.itemCardSub}>
                Đổi mới miễn phí 100% trong vòng 7 ngày kể từ khi nhận hàng tại mục Đơn hàng.
              </Text>
            </View>
          </View>
        ) : null}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.background,
  },
  authContainer: {
    padding: 20,
    paddingBottom: 100,
    gap: 16,
  },
  authHero: {
    alignItems: 'center',
    gap: 6,
    marginTop: 12,
  },
  logoCircle: {
    width: 64,
    height: 64,
    borderRadius: 20,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
  },
  authTitle: {
    fontSize: 22,
    fontWeight: '800',
    color: colors.textPrimary,
  },
  authSubtitle: {
    fontSize: 13,
    color: colors.textSecondary,
    textAlign: 'center',
  },
  authTabs: {
    flexDirection: 'row',
    backgroundColor: colors.surfaceMuted,
    padding: 4,
    borderRadius: 12,
  },
  authTab: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 10,
    alignItems: 'center',
  },
  authTabActive: {
    backgroundColor: colors.surface,
  },
  authTabText: {
    fontSize: 13,
    fontWeight: '600',
    color: colors.textSecondary,
  },
  authTabTextActive: {
    color: colors.primary,
    fontWeight: '700',
  },
  errorAlert: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    padding: 14,
    borderRadius: 12,
    backgroundColor: colors.dangerBg,
    borderWidth: 1,
    borderColor: '#FCA5A5',
  },
  errorAlertText: {
    flex: 1,
    fontSize: 13,
    fontWeight: '700',
    color: colors.danger,
    lineHeight: 19,
  },
  successAlert: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    padding: 14,
    borderRadius: 12,
    backgroundColor: colors.primarySoft,
  },
  successAlertText: {
    flex: 1,
    fontSize: 13,
    fontWeight: '600',
    color: colors.primaryDark,
  },
  formCard: {
    backgroundColor: colors.surface,
    padding: 16,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.border,
    gap: 10,
  },
  inputLabel: {
    fontSize: 13,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  input: {
    minHeight: 44,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.background,
    paddingHorizontal: 12,
    fontSize: 14,
    color: colors.textPrimary,
  },
  primaryBtn: {
    minHeight: 46,
    borderRadius: 12,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 6,
  },
  primaryBtnText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '700',
  },
  demoBox: {
    marginTop: 12,
    paddingTop: 12,
    borderTopWidth: 1,
    borderTopColor: colors.borderSubtle,
    gap: 8,
  },
  demoTitle: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.textSecondary,
  },
  demoBtnRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  demoChip: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: colors.primary,
    backgroundColor: colors.background,
  },
  demoChipText: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.primary,
  },
  scrollContent: {
    padding: 16,
    paddingBottom: 100,
    gap: 16,
  },
  profileCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 16,
    borderRadius: 16,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
  },
  avatarCircle: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarInitials: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '800',
  },
  profileName: {
    fontSize: 17,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  profileMeta: {
    fontSize: 12,
    color: colors.textSecondary,
    marginTop: 2,
  },
  memberBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginTop: 6,
  },
  memberBadgeText: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.primary,
  },
  logoutBtn: {
    width: 40,
    height: 40,
    borderRadius: 10,
    backgroundColor: colors.dangerBg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sectionTabs: {
    gap: 8,
  },
  secChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 14,
    paddingVertical: 9,
    borderRadius: 999,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
  },
  secChipActive: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  secChipText: {
    fontSize: 13,
    fontWeight: '600',
    color: colors.textSecondary,
  },
  secChipTextActive: {
    color: '#FFFFFF',
  },
  sectionBlock: {
    gap: 10,
  },
  sectionTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  actionLink: {
    fontSize: 13,
    fontWeight: '600',
    color: colors.primary,
  },
  addAddrCard: {
    backgroundColor: colors.surface,
    padding: 14,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: colors.border,
    gap: 8,
  },
  itemCard: {
    backgroundColor: colors.surface,
    padding: 14,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: colors.border,
    gap: 4,
  },
  itemCardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  itemCardTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  itemCardSub: {
    fontSize: 13,
    color: colors.textSecondary,
    lineHeight: 19,
  },
  defaultTag: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
    backgroundColor: colors.primarySoft,
  },
  defaultTagText: {
    fontSize: 11,
    fontWeight: '700',
    color: colors.primary,
  },
  addrActions: {
    flexDirection: 'row',
    gap: 16,
    marginTop: 6,
  },
  voucherCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 12,
    borderRadius: 14,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
  },
  voucherLeft: {
    width: 84,
    height: 76,
    borderRadius: 10,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
  },
  voucherCodeText: {
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '700',
  },
  voucherExpiry: {
    fontSize: 11,
    color: colors.textMuted,
  },
  useVoucherBtn: {
    paddingHorizontal: 10,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: colors.primarySoft,
  },
  useVoucherBtnText: {
    fontSize: 12,
    fontWeight: '700',
    color: colors.primary,
  },
});

