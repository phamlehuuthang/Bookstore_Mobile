import React, { useState } from 'react';
import {
  ActivityIndicator,
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
import { Address, formatCurrencyVND } from '@shared/types';
import { INITIAL_BOOKS, INITIAL_VOUCHERS } from '@shared/api-client/mockData';
import { colors } from '@/theme/colors';
import { useAuth } from '@/hooks/useAuth';
import { ApiError } from '@/services/httpClient';

type ProfileSection = 'ADDRESSES' | 'VOUCHERS' | 'WISHLIST' | 'NOTIFICATIONS' | 'FAQ';
type AuthMode = 'LOGIN' | 'REGISTER' | 'FORGOT_OTP';
type AuthAlertKind = 'LOCKED' | 'RESTRICTED' | 'ERROR';

interface AuthAlertState {
  kind: AuthAlertKind;
  title: string;
  message: string;
}

export default function ProfileScreen() {
  const router = useRouter();
  const {
    user,
    isHydrating,
    wishlistBookIds,
    notifications,
    login,
    register,
    logout,
    updateProfile,
    changePassword,
    saveAddress,
    setDefaultAddress,
    deleteAddress,
    toggleWishlist,
    markAllNotificationsRead,
  } = useAuth();

  const [activeSection, setActiveSection] = useState<ProfileSection>('ADDRESSES');

  // Cụm 1: State cho màn hình Đăng nhập / Đăng ký (để trống hoàn toàn, không điền sẵn chữ mẫu)
  const [authMode, setAuthMode] = useState<AuthMode>('LOGIN');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [authError, setAuthError] = useState<AuthAlertState | null>(null);
  const [authSuccess, setAuthSuccess] = useState<string | null>(null);
  const [isAuthSubmitting, setIsAuthSubmitting] = useState(false);

  // Cụm 2: State cho 2 hộp Sửa hồ sơ & Đổi mật khẩu (chỉ mở 1 hộp tại 1 thời điểm)
  const [showEditProfile, setShowEditProfile] = useState(false);
  const [editFullName, setEditFullName] = useState('');
  const [editPhone, setEditPhone] = useState('');
  const [showChangePassword, setShowChangePassword] = useState(false);
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [profileNotice, setProfileNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [isProfileSubmitting, setIsProfileSubmitting] = useState(false);

  // Cụm 3: Form Sổ địa chỉ dùng chung cho cả Thêm mới (editingAddressId = null) và Sửa (editingAddressId = ID địa chỉ đang sửa)
  const [showAddressForm, setShowAddressForm] = useState(false);
  const [editingAddressId, setEditingAddressId] = useState<string | null>(null);
  const [addrName, setAddrName] = useState('');
  const [addrPhone, setAddrPhone] = useState('');
  const [addrStreet, setAddrStreet] = useState('');
  const [addrWard, setAddrWard] = useState('');
  const [addrDistrict, setAddrDistrict] = useState('');
  const [addrProvince, setAddrProvince] = useState('');
  const [addrIsDefault, setAddrIsDefault] = useState(true);
  const [addrNotice, setAddrNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [isAddrSubmitting, setIsAddrSubmitting] = useState(false);

  // Phân loại lỗi trả về từ Backend thành 3 trạng thái thông báo chuẩn ứng dụng TMĐT
  const buildAuthAlert = (err: unknown, mode: AuthMode): AuthAlertState => {
    const message = err instanceof Error ? err.message : 'Không thể xác thực lúc này. Vui lòng thử lại.';
    const lower = message.toLowerCase();

    if (lower.includes('tạm khóa') || lower.includes('bị khóa')) {
      return {
        kind: 'LOCKED',
        title: 'Tài khoản đang tạm khóa',
        message,
      };
    }

    if (
      (err instanceof ApiError && err.status === 403) ||
      lower.includes('không hỗ trợ đăng nhập') ||
      lower.includes('quản trị')
    ) {
      return {
        kind: 'RESTRICTED',
        title: 'Không thể đăng nhập bằng tài khoản này',
        message,
      };
    }

    return {
      kind: 'ERROR',
      title: mode === 'REGISTER' ? 'Đăng ký chưa thành công' : 'Đăng nhập chưa thành công',
      message,
    };
  };

  const switchAuthMode = (nextMode: AuthMode) => {
    setAuthMode(nextMode);
    setAuthError(null);
    setAuthSuccess(null);
    setPassword('');
    setShowPassword(false);
  };

  const handleAuthSubmit = async () => {
    if (isAuthSubmitting) return;
    setAuthError(null);
    setAuthSuccess(null);

    if (authMode === 'FORGOT_OTP') {
      setAuthError({
        kind: 'RESTRICTED',
        title: 'Tính năng đang được cập nhật',
        message:
          'Khôi phục mật khẩu qua OTP sẽ sớm được hỗ trợ. Nếu cần cấp lại mật khẩu, vui lòng liên hệ CSKH 1900 6886.',
      });
      return;
    }

    if (authMode === 'LOGIN' && (!email.trim() || !password)) {
      setAuthError({
        kind: 'ERROR',
        title: 'Thiếu thông tin đăng nhập',
        message: 'Vui lòng nhập đầy đủ địa chỉ email và mật khẩu.',
      });
      return;
    }

    if (authMode === 'REGISTER' && (!fullName.trim() || !email.trim() || !password)) {
      setAuthError({
        kind: 'ERROR',
        title: 'Thiếu thông tin đăng ký',
        message: 'Vui lòng nhập họ tên, địa chỉ email và mật khẩu (tối thiểu 6 ký tự).',
      });
      return;
    }

    setIsAuthSubmitting(true);
    try {
      if (authMode === 'LOGIN') {
        await login(email, password);
      } else {
        await register({
          fullName: fullName.trim(),
          email: email.trim(),
          phone: phone.trim(),
          password,
        });
      }
      setPassword('');
    } catch (err) {
      setAuthError(buildAuthAlert(err, authMode));
    } finally {
      setIsAuthSubmitting(false);
    }
  };

  const handleLogout = () => {
    logout();
    setPassword('');
    setAuthError(null);
    setAuthSuccess(null);
  };

  const openEditProfile = () => {
    if (!user) return;
    setEditFullName(user.fullName);
    setEditPhone(user.phone);
    setShowEditProfile((prev) => !prev);
    setShowChangePassword(false);
    setProfileNotice(null);
  };

  const openChangePassword = () => {
    setOldPassword('');
    setNewPassword('');
    setShowChangePassword((prev) => !prev);
    setShowEditProfile(false);
    setProfileNotice(null);
  };

  const handleSaveProfile = async () => {
    if (isProfileSubmitting) return;
    setProfileNotice(null);
    setIsProfileSubmitting(true);
    try {
      await updateProfile(editFullName, editPhone);
      setShowEditProfile(false);
      setProfileNotice({ type: 'success', text: 'Đã cập nhật thông tin hồ sơ thành công.' });
    } catch (err) {
      setProfileNotice({
        type: 'error',
        text: err instanceof Error ? err.message : 'Cập nhật hồ sơ thất bại.',
      });
    } finally {
      setIsProfileSubmitting(false);
    }
  };

  const handleChangePasswordSubmit = async () => {
    if (isProfileSubmitting) return;
    setProfileNotice(null);
    setIsProfileSubmitting(true);
    try {
      await changePassword(oldPassword, newPassword);
      setOldPassword('');
      setNewPassword('');
      setShowChangePassword(false);
      setProfileNotice({ type: 'success', text: 'Đổi mật khẩu thành công.' });
    } catch (err) {
      setProfileNotice({
        type: 'error',
        text: err instanceof Error ? err.message : 'Đổi mật khẩu thất bại.',
      });
    } finally {
      setIsProfileSubmitting(false);
    }
  };

  const openCreateAddress = () => {
    if (showAddressForm && !editingAddressId) {
      setShowAddressForm(false);
      return;
    }
    setEditingAddressId(null);
    setAddrName(user?.fullName || '');
    setAddrPhone(user?.phone || '');
    setAddrStreet('');
    setAddrWard('');
    setAddrDistrict('');
    setAddrProvince('');
    // Khi tạo mới mặc định bật checkbox đặt làm mặc định (Quy tắc 2A cũng tự ép true nếu đây là địa chỉ đầu tiên)
    setAddrIsDefault(true);
    setAddrNotice(null);
    setShowAddressForm(true);
  };

  const openEditAddress = (addr: Address) => {
    // Nạp lại dữ liệu của địa chỉ đang chọn vào form và gắn editingAddressId để handleSaveAddressForm gửi PUT thay vì POST
    setEditingAddressId(addr.id);
    setAddrName(addr.fullName);
    setAddrPhone(addr.phone);
    setAddrStreet(addr.streetAddress);
    setAddrWard(addr.ward);
    setAddrDistrict(addr.district);
    setAddrProvince(addr.province);
    setAddrIsDefault(addr.isDefault);
    setAddrNotice(null);
    setShowAddressForm(true);
  };

  const handleSaveAddressForm = async () => {
    if (isAddrSubmitting) return;
    setAddrNotice(null);
    setIsAddrSubmitting(true);
    try {
      // Nếu editingAddressId có giá trị -> authService sẽ gọi PUT /addresses/{id}; nếu undefined -> gọi POST /addresses
      await saveAddress({
        id: editingAddressId || undefined,
        fullName: addrName,
        phone: addrPhone,
        province: addrProvince,
        district: addrDistrict,
        ward: addrWard,
        streetAddress: addrStreet,
        isDefault: addrIsDefault,
      });
      setShowAddressForm(false);
      setEditingAddressId(null);
      setAddrStreet('');
      setAddrNotice({
        type: 'success',
        text: editingAddressId
          ? 'Đã cập nhật địa chỉ nhận hàng.'
          : 'Đã thêm địa chỉ nhận hàng mới.',
      });
    } catch (err) {
      setAddrNotice({
        type: 'error',
        text: err instanceof Error ? err.message : 'Không thể lưu địa chỉ.',
      });
    } finally {
      setIsAddrSubmitting(false);
    }
  };

  const handleSetDefault = async (addressId: string) => {
    if (isAddrSubmitting) return;
    setAddrNotice(null);
    setIsAddrSubmitting(true);
    try {
      await setDefaultAddress(addressId);
      setAddrNotice({ type: 'success', text: 'Đã đặt làm địa chỉ mặc định.' });
    } catch (err) {
      setAddrNotice({
        type: 'error',
        text: err instanceof Error ? err.message : 'Không thể đặt địa chỉ mặc định.',
      });
    } finally {
      setIsAddrSubmitting(false);
    }
  };

  const handleDeleteAddress = async (addressId: string) => {
    if (isAddrSubmitting) return;
    setAddrNotice(null);
    setIsAddrSubmitting(true);
    try {
      await deleteAddress(addressId);
      if (editingAddressId === addressId) {
        setEditingAddressId(null);
        setShowAddressForm(false);
      }
      setAddrNotice({ type: 'success', text: 'Đã xóa địa chỉ nhận hàng.' });
    } catch (err) {
      setAddrNotice({
        type: 'error',
        text: err instanceof Error ? err.message : 'Không thể xóa địa chỉ.',
      });
    } finally {
      setIsAddrSubmitting(false);
    }
  };

  const wishlistBooks = INITIAL_BOOKS.filter((b) => wishlistBookIds.includes(b.id));

  // Cổng chờ 1: khi app vừa mở và AuthProvider đang gọi /auth/me kiểm tra JWT cũ -> hiện vòng xoay thay vì nháy màn Login
  if (isHydrating) {
    return (
      <SafeAreaView edges={['top']} style={styles.safeArea}>
        <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', gap: 10 }}>
          <ActivityIndicator size="large" color={colors.primary} />
          <Text style={styles.authSubtitle}>Đang khôi phục phiên đăng nhập...</Text>
        </View>
      </SafeAreaView>
    );
  }

  // Cổng chờ 2: chưa đăng nhập (hoặc token đã hết hạn) -> hiển thị form Đăng nhập / Đăng ký
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
                onPress={() => switchAuthMode('LOGIN')}
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
                onPress={() => switchAuthMode('REGISTER')}
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
                onPress={() => switchAuthMode('FORGOT_OTP')}
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
              <View
                style={[
                  styles.authAlertCard,
                  authError.kind === 'RESTRICTED'
                    ? styles.authAlertAmber
                    : styles.authAlertDanger,
                ]}
              >
                <View
                  style={[
                    styles.authAlertIconWrap,
                    authError.kind === 'RESTRICTED'
                      ? styles.authAlertIconAmber
                      : styles.authAlertIconDanger,
                  ]}
                >
                  <Ionicons
                    name={
                      authError.kind === 'LOCKED'
                        ? 'lock-closed-outline'
                        : authError.kind === 'RESTRICTED'
                          ? 'information-circle-outline'
                          : 'alert-circle-outline'
                    }
                    size={19}
                    color={authError.kind === 'RESTRICTED' ? '#B45309' : colors.danger}
                  />
                </View>
                <View style={{ flex: 1, gap: 4 }}>
                  <Text
                    style={[
                      styles.authAlertTitle,
                      authError.kind === 'RESTRICTED' && { color: '#92400E' },
                    ]}
                  >
                    {authError.title}
                  </Text>
                  <Text
                    selectable
                    style={[
                      styles.authAlertBody,
                      authError.kind === 'RESTRICTED' && { color: '#78350F' },
                    ]}
                  >
                    {authError.message}
                  </Text>
                  {authError.kind === 'RESTRICTED' && authMode === 'LOGIN' ? (
                    <Pressable
                      onPress={() => switchAuthMode('REGISTER')}
                      style={styles.authAlertActionBtn}
                    >
                      <Text style={styles.authAlertActionText}>
                        Tạo tài khoản khách hàng mới →
                      </Text>
                    </Pressable>
                  ) : null}
                </View>
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
                    placeholder="Nhập họ và tên"
                    placeholderTextColor={colors.textMuted}
                    style={styles.input}
                  />
                  <Text style={styles.inputLabel}>Số điện thoại</Text>
                  <TextInput
                    value={phone}
                    onChangeText={setPhone}
                    placeholder="Nhập số điện thoại"
                    placeholderTextColor={colors.textMuted}
                    keyboardType="phone-pad"
                    style={styles.input}
                  />
                </>
              ) : null}

              <Text style={styles.inputLabel}>Địa chỉ Email</Text>
              <TextInput
                value={email}
                onChangeText={setEmail}
                placeholder="Nhập địa chỉ email"
                placeholderTextColor={colors.textMuted}
                autoCapitalize="none"
                autoCorrect={false}
                keyboardType="email-address"
                style={styles.input}
              />

              {authMode !== 'FORGOT_OTP' ? (
                <>
                  <Text style={styles.inputLabel}>Mật khẩu</Text>
                  <View style={styles.passwordWrap}>
                    <TextInput
                      value={password}
                      onChangeText={setPassword}
                      placeholder={
                        authMode === 'REGISTER'
                          ? 'Tạo mật khẩu (tối thiểu 6 ký tự)'
                          : 'Nhập mật khẩu'
                      }
                      placeholderTextColor={colors.textMuted}
                      secureTextEntry={!showPassword}
                      autoCapitalize="none"
                      style={styles.passwordInput}
                    />
                    <Pressable
                      onPress={() => setShowPassword((prev) => !prev)}
                      hitSlop={8}
                      style={styles.eyeBtn}
                      accessibilityLabel={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                    >
                      <Ionicons
                        name={showPassword ? 'eye-off-outline' : 'eye-outline'}
                        size={20}
                        color={colors.textSecondary}
                      />
                    </Pressable>
                  </View>
                </>
              ) : (
                <>
                  <Text style={styles.inputLabel}>Mã xác thực OTP (6 chữ số)</Text>
                  <TextInput
                    value={otpCode}
                    onChangeText={setOtpCode}
                    placeholder="Nhập mã OTP"
                    placeholderTextColor={colors.textMuted}
                    keyboardType="numeric"
                    style={styles.input}
                  />
                </>
              )}

              <Pressable
                onPress={handleAuthSubmit}
                disabled={isAuthSubmitting}
                style={[styles.primaryBtn, isAuthSubmitting && { opacity: 0.7 }]}
              >
                <Text style={styles.primaryBtnText}>
                  {isAuthSubmitting
                    ? 'Đang xử lý...'
                    : authMode === 'LOGIN'
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
                .trim()
                .split(/\s+/)
                .slice(-2)
                .map((w) => w[0])
                .join('')}
            </Text>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.profileName}>{user.fullName}</Text>
            <Text style={styles.profileMeta}>
              {user.phone || 'Chưa có SĐT'} • {user.email}
            </Text>
            <View style={styles.memberBadge}>
              <Ionicons name="shield-checkmark" size={12} color={colors.primary} />
              <Text style={styles.memberBadgeText}>
                {user.orderCount} đơn hàng • Chi tiêu {formatCurrencyVND(user.totalSpent)}
              </Text>
            </View>
            <View style={{ flexDirection: 'row', gap: 14, marginTop: 8 }}>
              <Pressable onPress={openEditProfile}>
                <Text style={styles.actionLink}>
                  {showEditProfile ? 'Đóng sửa hồ sơ' : 'Sửa hồ sơ'}
                </Text>
              </Pressable>
              <Pressable onPress={openChangePassword}>
                <Text style={styles.actionLink}>
                  {showChangePassword ? 'Đóng đổi mật khẩu' : 'Đổi mật khẩu'}
                </Text>
              </Pressable>
            </View>
          </View>
          <Pressable onPress={handleLogout} style={styles.logoutBtn}>
            <Ionicons name="log-out-outline" size={20} color={colors.danger} />
          </Pressable>
        </View>

        {profileNotice ? (
          <View
            style={
              profileNotice.type === 'error' ? styles.errorAlert : styles.successAlert
            }
          >
            <Ionicons
              name={profileNotice.type === 'error' ? 'alert-circle' : 'checkmark-circle'}
              size={18}
              color={profileNotice.type === 'error' ? colors.danger : colors.primary}
            />
            <Text
              style={
                profileNotice.type === 'error'
                  ? styles.errorAlertText
                  : styles.successAlertText
              }
            >
              {profileNotice.text}
            </Text>
          </View>
        ) : null}

        {showEditProfile ? (
          <View style={styles.addAddrCard}>
            <Text style={styles.sectionTitle}>Cập nhật hồ sơ cá nhân</Text>
            <TextInput
              value={editFullName}
              onChangeText={setEditFullName}
              placeholder="Họ và tên"
              style={styles.input}
            />
            <TextInput
              value={editPhone}
              onChangeText={setEditPhone}
              placeholder="Số điện thoại"
              keyboardType="phone-pad"
              style={styles.input}
            />
            <Pressable
              onPress={handleSaveProfile}
              disabled={isProfileSubmitting}
              style={[styles.primaryBtn, isProfileSubmitting && { opacity: 0.7 }]}
            >
              <Text style={styles.primaryBtnText}>
                {isProfileSubmitting ? 'Đang lưu...' : 'Lưu thông tin hồ sơ'}
              </Text>
            </Pressable>
          </View>
        ) : null}

        {showChangePassword ? (
          <View style={styles.addAddrCard}>
            <Text style={styles.sectionTitle}>Đổi mật khẩu đăng nhập</Text>
            <TextInput
              value={oldPassword}
              onChangeText={setOldPassword}
              placeholder="Mật khẩu hiện tại"
              secureTextEntry
              style={styles.input}
            />
            <TextInput
              value={newPassword}
              onChangeText={setNewPassword}
              placeholder="Mật khẩu mới (tối thiểu 6 ký tự)"
              secureTextEntry
              style={styles.input}
            />
            <Pressable
              onPress={handleChangePasswordSubmit}
              disabled={isProfileSubmitting}
              style={[styles.primaryBtn, isProfileSubmitting && { opacity: 0.7 }]}
            >
              <Text style={styles.primaryBtnText}>
                {isProfileSubmitting ? 'Đang cập nhật...' : 'Xác nhận đổi mật khẩu'}
              </Text>
            </Pressable>
          </View>
        ) : null}

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
              <Pressable onPress={openCreateAddress}>
                <Text style={styles.actionLink}>
                  {showAddressForm ? 'Đóng' : '+ Thêm địa chỉ'}
                </Text>
              </Pressable>
            </View>

            {addrNotice ? (
              <View
                style={
                  addrNotice.type === 'error' ? styles.errorAlert : styles.successAlert
                }
              >
                <Ionicons
                  name={addrNotice.type === 'error' ? 'alert-circle' : 'checkmark-circle'}
                  size={18}
                  color={addrNotice.type === 'error' ? colors.danger : colors.primary}
                />
                <Text
                  style={
                    addrNotice.type === 'error'
                      ? styles.errorAlertText
                      : styles.successAlertText
                  }
                >
                  {addrNotice.text}
                </Text>
              </View>
            ) : null}

            {showAddressForm ? (
              <View style={styles.addAddrCard}>
                <Text style={styles.inputLabel}>
                  {editingAddressId ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ nhận hàng mới'}
                </Text>
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
                <TextInput
                  value={addrWard}
                  onChangeText={setAddrWard}
                  placeholder="Phường/Xã"
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
                <Pressable
                  onPress={() => setAddrIsDefault((prev) => !prev)}
                  style={{ flexDirection: 'row', alignItems: 'center', gap: 8, paddingVertical: 4 }}
                >
                  <Ionicons
                    name={addrIsDefault ? 'checkbox' : 'square-outline'}
                    size={20}
                    color={colors.primary}
                  />
                  <Text style={styles.itemCardSub}>Đặt làm địa chỉ nhận hàng mặc định</Text>
                </Pressable>
                <Pressable
                  onPress={handleSaveAddressForm}
                  disabled={isAddrSubmitting}
                  style={[styles.primaryBtn, isAddrSubmitting && { opacity: 0.7 }]}
                >
                  <Text style={styles.primaryBtnText}>
                    {isAddrSubmitting
                      ? 'Đang lưu...'
                      : editingAddressId
                        ? 'Cập nhật địa chỉ'
                        : 'Lưu địa chỉ'}
                  </Text>
                </Pressable>
              </View>
            ) : null}

            {user.addresses.length === 0 ? (
              <View style={styles.itemCard}>
                <Text style={styles.itemCardSub}>
                  Bạn chưa lưu địa chỉ nhận hàng nào. Hãy bấm "+ Thêm địa chỉ" để tạo mới.
                </Text>
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
                      disabled={isAddrSubmitting}
                      onPress={() => handleSetDefault(addr.id)}
                    >
                      <Text style={styles.actionLink}>Đặt làm mặc định</Text>
                    </Pressable>
                  ) : null}
                  <Pressable
                    disabled={isAddrSubmitting}
                    onPress={() => openEditAddress(addr)}
                  >
                    <Text style={styles.actionLink}>Sửa</Text>
                  </Pressable>
                  <Pressable
                    disabled={isAddrSubmitting}
                    onPress={() => handleDeleteAddress(addr.id)}
                  >
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
  authAlertCard: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 12,
    padding: 14,
    borderRadius: 14,
    borderWidth: 1,
  },
  authAlertDanger: {
    backgroundColor: '#FEF2F2',
    borderColor: '#FECACA',
  },
  authAlertAmber: {
    backgroundColor: '#FFFBEB',
    borderColor: '#FDE68A',
  },
  authAlertIconWrap: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 1,
  },
  authAlertIconDanger: {
    backgroundColor: '#FEE2E2',
  },
  authAlertIconAmber: {
    backgroundColor: '#FEF3C7',
  },
  authAlertTitle: {
    fontSize: 13.5,
    fontWeight: '700',
    color: '#991B1B',
  },
  authAlertBody: {
    fontSize: 13,
    fontWeight: '400',
    color: '#7F1D1D',
    lineHeight: 19,
  },
  authAlertActionBtn: {
    alignSelf: 'flex-start',
    marginTop: 4,
    paddingVertical: 4,
  },
  authAlertActionText: {
    fontSize: 13,
    fontWeight: '700',
    color: '#B45309',
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
    fontWeight: '600',
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
  passwordWrap: {
    minHeight: 44,
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.background,
    paddingLeft: 12,
    paddingRight: 8,
  },
  passwordInput: {
    flex: 1,
    minHeight: 44,
    fontSize: 14,
    color: colors.textPrimary,
  },
  eyeBtn: {
    padding: 6,
    alignItems: 'center',
    justifyContent: 'center',
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

