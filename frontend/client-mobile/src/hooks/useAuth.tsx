import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { Address, NotificationItem, User } from '@shared/types';
import { authService } from '@/services/authService';
import {
  cartWishlistVoucherService,
  WishlistBookItem,
} from '@/services/cartWishlistVoucherService';
import { notificationService } from '@/services/notificationService';

interface AuthContextValue {
  user: User | null;
  isAuthenticated: boolean;
  // Cờ báo hiệu đang kiểm tra JWT lúc mới mở app/F5 để màn Profile hiển thị loading thay vì nháy form Login
  isHydrating: boolean;
  wishlistItems: WishlistBookItem[];
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
  // Trả về false nếu khách chưa đăng nhập để màn hình UI chủ động dẫn sang đăng nhập
  toggleWishlist: (bookId: string) => Promise<boolean>;
  refreshWishlist: () => Promise<void>;
  refreshNotifications: () => Promise<void>;
  markNotificationRead: (notificationId: string) => Promise<void>;
  markAllNotificationsRead: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

/**
 * AuthProvider bọc ngoài toàn bộ ứng dụng tại app/_layout.tsx để chia sẻ phiên đăng nhập `user`,
 * sổ địa chỉ `user.addresses` và Tủ sách yêu thích cho toàn bộ màn hình.
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isHydrating, setIsHydrating] = useState<boolean>(true);
  const [wishlistItems, setWishlistItems] = useState<WishlistBookItem[]>([]);

  // Danh sách thông báo thật từ PostgreSQL (GET /api/v1/notifications - Bước 7)
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);

  // Tự động khôi phục phiên từ JWT đã lưu mỗi lần ứng dụng khởi động
  useEffect(() => {
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

  // Mỗi khi user thay đổi (đăng nhập / khôi phục phiên / đăng xuất), tự đồng bộ Tủ sách yêu thích & Thông báo từ PostgreSQL
  useEffect(() => {
    if (!user) {
      setWishlistItems([]);
      setNotifications([]);
      return;
    }

    let active = true;
    cartWishlistVoucherService
      .getWishlist()
      .then((items) => {
        if (active) {
          setWishlistItems(items);
        }
      })
      .catch(() => {
        if (active) {
          setWishlistItems([]);
        }
      });

    notificationService
      .getNotifications()
      .then((list) => {
        if (active) {
          setNotifications(list);
        }
      })
      .catch(() => {
        if (active) {
          setNotifications([]);
        }
      });

    return () => {
      active = false;
    };
  }, [user?.id]);

  const wishlistBookIds = useMemo(
    () => wishlistItems.map((item) => item.sachId),
    [wishlistItems]
  );

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
    setWishlistItems([]);
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

  const refreshWishlist = async () => {
    if (!user) {
      setWishlistItems([]);
      return;
    }
    const items = await cartWishlistVoucherService.getWishlist();
    setWishlistItems(items);
  };

  const toggleWishlist = async (bookId: string): Promise<boolean> => {
    if (!user) {
      return false;
    }
    await cartWishlistVoucherService.toggleWishlist(bookId);
    await refreshWishlist();
    return true;
  };

  const refreshNotifications = async () => {
    if (!user) {
      setNotifications([]);
      return;
    }
    const list = await notificationService.getNotifications();
    setNotifications(list);
  };

  const markNotificationRead = async (notificationId: string) => {
    if (!user) return;
    const updated = await notificationService.markAsRead(notificationId);
    setNotifications((prev) =>
      prev.map((n) => (n.id === notificationId ? { ...n, isRead: updated.isRead } : n))
    );
  };

  const markAllNotificationsRead = async () => {
    if (!user) return;
    await notificationService.markAllAsRead();
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: Boolean(user),
        isHydrating,
        wishlistItems,
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
        refreshWishlist,
        refreshNotifications,
        markNotificationRead,
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
