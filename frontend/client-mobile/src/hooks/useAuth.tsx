import React, { createContext, useContext, useEffect, useState } from 'react';
import { Address, NotificationItem, User } from '@shared/types';
import { INITIAL_NOTIFICATIONS } from '@shared/api-client/mockData';
import { authService } from '@/services/authService';

interface AuthContextValue {
  user: User | null;
  isAuthenticated: boolean;
  // Cờ báo hiệu đang kiểm tra JWT lúc mới mở app/F5 để màn Profile hiển thị loading thay vì nháy form Login
  isHydrating: boolean;
  wishlistBookIds: string[];
  notifications: NotificationItem[];
  login: (email: string, password: string) => Promise<void>;
  register: (payload: {
    fullName: string;
    email: string;
    phone: string;
    password: string;
  }) => Promise<void>;
  logout: () => void;
  updateProfile: (fullName: string, phone: string) => Promise<void>;
  changePassword: (currentPassword: string, newPassword: string) => Promise<void>;
  saveAddress: (address: Omit<Address, 'id'> & { id?: string }) => Promise<void>;
  setDefaultAddress: (addressId: string) => Promise<void>;
  deleteAddress: (addressId: string) => Promise<void>;
  toggleWishlist: (bookId: string) => void;
  markAllNotificationsRead: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

/**
 * AuthProvider bọc ngoài toàn bộ ứng dụng tại app/_layout.tsx để chia sẻ phiên đăng nhập `user`
 * và sổ địa chỉ `user.addresses` cho tất cả màn hình (Profile, Checkout, Home, Orders).
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isHydrating, setIsHydrating] = useState<boolean>(true);

  // Phần Yêu thích & Thông báo tạm giữ nguyên dữ liệu mẫu cho đến khi nối Bước 4 & Bước 7 lên frontend
  const [wishlistBookIds, setWishlistBookIds] = useState<string[]>(['book-1', 'book-4', 'book-8']);
  const [notifications, setNotifications] = useState<NotificationItem[]>(INITIAL_NOTIFICATIONS);

  // Tự động khôi phục phiên từ JWT đã lưu mỗi lần ứng dụng khởi động
  useEffect(() => {
    // Biến cờ `active` tránh gọi setState sau khi component đã bị unmount (tránh rò rỉ bộ nhớ / warning React)
    let active = true;
    authService
      .restoreSession()
      .then((restoredUser) => {
        if (active && restoredUser) {
          setUser(restoredUser);
        }
      })
      .finally(() => {
        if (active) {
          setIsHydrating(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const login = async (email: string, password: string) => {
    const res = await authService.loginCustomer(email, password);
    setUser(res.user);
  };

  const register = async (payload: {
    fullName: string;
    email: string;
    phone: string;
    password: string;
  }) => {
    const res = await authService.registerCustomer(payload);
    setUser(res.user);
  };

  const logout = () => {
    authService.logout();
    setUser(null);
  };

  const updateProfile = async (fullName: string, phone: string) => {
    if (!user) return;
    const updatedUser = await authService.updateProfile(fullName, phone, user.addresses);
    setUser(updatedUser);
  };

  const changePassword = async (currentPassword: string, newPassword: string) => {
    if (!user) return;
    await authService.changePassword(currentPassword, newPassword);
  };

  // Dùng callback `setUser((prev) => ...)` khi cập nhật addresses để luôn lấy state mới nhất, tránh ghi đè dữ liệu cũ
  const saveAddress = async (address: Omit<Address, 'id'> & { id?: string }) => {
    if (!user) return;
    const updatedAddresses = await authService.saveAddress(address);
    setUser((prev) => (prev ? { ...prev, addresses: updatedAddresses } : null));
  };

  const setDefaultAddress = async (addressId: string) => {
    if (!user) return;
    const updatedAddresses = await authService.setDefaultAddress(addressId);
    setUser((prev) => (prev ? { ...prev, addresses: updatedAddresses } : null));
  };

  const deleteAddress = async (addressId: string) => {
    if (!user) return;
    const updatedAddresses = await authService.deleteAddress(addressId);
    setUser((prev) => (prev ? { ...prev, addresses: updatedAddresses } : null));
  };

  const toggleWishlist = (bookId: string) => {
    setWishlistBookIds((prev) =>
      prev.includes(bookId) ? prev.filter((id) => id !== bookId) : [...prev, bookId]
    );
  };

  const markAllNotificationsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: Boolean(user),
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
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
