import React, { createContext, useContext, useState } from 'react';
import { Address, NotificationItem, User } from '@shared/types';
import { INITIAL_NOTIFICATIONS, INITIAL_USERS } from '@shared/api-client/mockData';
import { authService } from '@/services/authService';

interface AuthContextValue {
  user: User | null;
  isAuthenticated: boolean;
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
  updateProfile: (fullName: string, phone: string) => void;
  saveAddress: (address: Omit<Address, 'id'> & { id?: string }) => Promise<void>;
  deleteAddress: (addressId: string) => Promise<void>;
  toggleWishlist: (bookId: string) => void;
  markAllNotificationsRead: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  // Default logged-in customer: Nguyễn Minh Tuấn (ROLE: CUSTOMER)
  const [user, setUser] = useState<User | null>(INITIAL_USERS[0]);
  const [wishlistBookIds, setWishlistBookIds] = useState<string[]>(['book-1', 'book-4', 'book-8']);
  const [notifications, setNotifications] = useState<NotificationItem[]>(INITIAL_NOTIFICATIONS);

  const login = async (email: string, password: string) => {
    const res = await authService.loginCustomer(email, password);
    setUser({ ...res.user });
  };

  const register = async (payload: {
    fullName: string;
    email: string;
    phone: string;
    password: string;
  }) => {
    const res = await authService.registerCustomer(payload);
    setUser({ ...res.user });
  };

  const logout = () => {
    setUser(null);
  };

  const updateProfile = (fullName: string, phone: string) => {
    if (!user) return;
    setUser({ ...user, fullName, phone });
  };

  const saveAddress = async (address: Omit<Address, 'id'> & { id?: string }) => {
    if (!user) return;
    const updatedAddresses = await authService.saveAddress(user.id, address);
    setUser({ ...user, addresses: updatedAddresses });
  };

  const deleteAddress = async (addressId: string) => {
    if (!user) return;
    const updatedAddresses = await authService.deleteAddress(user.id, addressId);
    setUser({ ...user, addresses: updatedAddresses });
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
        wishlistBookIds,
        notifications,
        login,
        register,
        logout,
        updateProfile,
        saveAddress,
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

