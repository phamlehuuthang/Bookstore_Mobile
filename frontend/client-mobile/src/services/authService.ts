import { apiClient, db } from '@shared/index';
import { Address, User } from '@shared/types';

export const authService = {
  /**
   * Mobile Client Login with strict Enterprise Role Guard:
   * Blocks ADMIN/STAFF accounts with the mandatory message:
   * "Tài khoản quản trị viên vui lòng đăng nhập trên hệ thống Web Dashboard máy tính"
   */
  async loginCustomer(email: string, password: string): Promise<{ user: User; token: string }> {
    return apiClient.login(email, password, 'MOBILE_CLIENT');
  },

  async registerCustomer(payload: {
    fullName: string;
    email: string;
    phone: string;
    password: string;
  }): Promise<{ user: User; token: string }> {
    const existing = db.users.find((u) => u.email.toLowerCase() === payload.email.toLowerCase());
    if (existing) {
      throw new Error('Email này đã được sử dụng. Vui lòng chọn email khác.');
    }
    const newUser: User = {
      id: `user-cust-${Date.now()}`,
      fullName: payload.fullName,
      email: payload.email,
      phone: payload.phone,
      role: 'CUSTOMER',
      isLocked: false,
      totalSpent: 0,
      orderCount: 0,
      createdAt: new Date().toISOString(),
      addresses: [
        {
          id: `addr-${Date.now()}`,
          fullName: payload.fullName,
          phone: payload.phone,
          province: 'TP. Hồ Chí Minh',
          district: 'Quận 1',
          ward: 'Phường Bến Nghé',
          streetAddress: 'Số 2 Hải Triều',
          isDefault: true,
        },
      ],
    };
    db.users.unshift(newUser);
    return { user: newUser, token: `jwt-token-${newUser.id}` };
  },

  async saveAddress(userId: string, address: Omit<Address, 'id'> & { id?: string }): Promise<Address[]> {
    const user = db.users.find((u) => u.id === userId);
    if (!user) throw new Error('Không tìm thấy tài khoản');

    if (address.isDefault) {
      user.addresses.forEach((a) => {
        a.isDefault = false;
      });
    }

    if (address.id) {
      const idx = user.addresses.findIndex((a) => a.id === address.id);
      if (idx !== -1) {
        user.addresses[idx] = { ...address, id: address.id };
      }
    } else {
      user.addresses.push({
        ...address,
        id: `addr-${Date.now()}`,
      });
    }
    return [...user.addresses];
  },

  async deleteAddress(userId: string, addressId: string): Promise<Address[]> {
    const user = db.users.find((u) => u.id === userId);
    if (!user) throw new Error('Không tìm thấy tài khoản');
    user.addresses = user.addresses.filter((a) => a.id !== addressId);
    if (user.addresses.length > 0 && !user.addresses.some((a) => a.isDefault)) {
      user.addresses[0].isDefault = true;
    }
    return [...user.addresses];
  },
};

