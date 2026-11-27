import { Address, User, UserRole } from '@shared/types';
import {
  ApiError,
  apiRequest,
  clearAuthToken,
  getAuthToken,
  setAuthToken,
} from '@/services/httpClient';

// Kiểu dữ liệu JSON gốc trả về từ Backend (field đặt tên tiếng Việt không dấu theo DTO Java)
interface BackendUserDto {
  id: string;
  hoTen: string;
  email: string;
  soDienThoai: string | null;
  vaiTro: UserRole;
  biKhoa: boolean;
  tongChiTieu: number | string | null;
  soDonHang: number | null;
  ngayTao: string | null;
}

interface BackendAuthResponse {
  accessToken: string;
  tokenType: string;
  user: BackendUserDto;
}

interface BackendAddressDto {
  id: string;
  hoTenNguoiNhan: string;
  soDienThoai: string;
  tinhThanh: string;
  quanHuyen: string;
  phuongXa: string;
  diaChiChiTiet: string;
  diaChiDayDu?: string;
  macDinh: boolean;
}

/**
 * Chuyển đổi 1 địa chỉ từ Backend DTO (hoTenNguoiNhan, diaChiChiTiet...)
 * sang interface Address tiếng Anh dùng chung ở Mobile App & Checkout.
 */
function mapAddress(dto: BackendAddressDto): Address {
  return {
    id: dto.id,
    fullName: dto.hoTenNguoiNhan,
    phone: dto.soDienThoai,
    province: dto.tinhThanh,
    district: dto.quanHuyen,
    ward: dto.phuongXa,
    streetAddress: dto.diaChiChiTiet,
    isDefault: Boolean(dto.macDinh),
  };
}

/**
 * Chuyển đổi thông tin User từ Backend DTO sang interface User dùng chung của Frontend.
 * Ép kiểu Number() cho tongChiTieu vì BigDecimal khi qua JSON đôi lúc có thể là chuỗi số hoặc null.
 */
function mapUser(dto: BackendUserDto, addresses: Address[] = []): User {
  return {
    id: dto.id,
    fullName: dto.hoTen,
    email: dto.email,
    phone: dto.soDienThoai || '',
    role: dto.vaiTro,
    isLocked: Boolean(dto.biKhoa),
    totalSpent: Number(dto.tongChiTieu || 0),
    orderCount: Number(dto.soDonHang || 0),
    createdAt: dto.ngayTao || new Date().toISOString(),
    addresses,
  };
}

/**
 * Service Xác thực, Hồ sơ cá nhân & Sổ địa chỉ (Bước 2 - Phần 1 & Phần 2) kết nối trực tiếp Backend Spring Boot.
 */
export const authService = {
  /**
   * Đăng nhập Mobile App:
   * 1. Gửi email/mật khẩu tới POST /api/v1/auth/login (withAuth: false vì chưa có token).
   * 2. Lưu JWT vừa nhận vào bộ nhớ + localStorage để các request sau tự động mang theo token.
   * 3. Gọi tiếp GET /api/v1/addresses để nạp luôn sổ địa chỉ thật cho màn Profile & Checkout.
   */
  async loginCustomer(email: string, password: string): Promise<{ user: User; token: string }> {
    const res = await apiRequest<BackendAuthResponse>('/api/v1/auth/login', {
      method: 'POST',
      withAuth: false,
      body: {
        email: email.trim(),
        matKhau: password,
      },
    });

    setAuthToken(res.accessToken);
    const addresses = await this.getAddresses();
    return {
      user: mapUser(res.user, addresses),
      token: res.accessToken,
    };
  },

  /**
   * Đăng ký khách hàng mới: Backend tự cố định vai trò CUSTOMER và trả về JWT đăng nhập luôn.
   */
  async registerCustomer(payload: {
    fullName: string;
    email: string;
    phone: string;
    password: string;
  }): Promise<{ user: User; token: string }> {
    const res = await apiRequest<BackendAuthResponse>('/api/v1/auth/register', {
      method: 'POST',
      withAuth: false,
      body: {
        hoTen: payload.fullName.trim(),
        email: payload.email.trim(),
        soDienThoai: payload.phone.trim(),
        matKhau: payload.password,
      },
    });

    setAuthToken(res.accessToken);
    return {
      user: mapUser(res.user, []),
      token: res.accessToken,
    };
  },

  /**
   * Khôi phục phiên đăng nhập khi người dùng mở lại app hoặc nhấn F5:
   * - Nếu chưa có token trong storage -> trả null ngay, không gọi mạng.
   * - Nếu có token -> dùng Promise.all gọi song song /auth/me và /addresses để giảm thời gian chờ.
   * - Nếu server trả 401/403 (token hết hạn hoặc tài khoản bị khóa) -> xóa token cũ để về màn Đăng nhập.
   */
  async restoreSession(): Promise<User | null> {
    if (!getAuthToken()) {
      return null;
    }

    try {
      const [me, addressDtos] = await Promise.all([
        apiRequest<BackendUserDto>('/api/v1/auth/me'),
        apiRequest<BackendAddressDto[]>('/api/v1/addresses'),
      ]);

      if (me.vaiTro !== 'CUSTOMER') {
        clearAuthToken();
        return null;
      }

      return mapUser(me, addressDtos.map(mapAddress));
    } catch (err) {
      if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
        clearAuthToken();
      }
      return null;
    }
  },

  /**
   * Cập nhật họ tên & số điện thoại của người dùng đang đăng nhập; giữ nguyên mảng addresses hiện tại.
   */
  async updateProfile(fullName: string, phone: string, currentAddresses: Address[]): Promise<User> {
    const updated = await apiRequest<BackendUserDto>('/api/v1/auth/profile', {
      method: 'PUT',
      body: {
        hoTen: fullName.trim(),
        soDienThoai: phone.trim(),
      },
    });
    return mapUser(updated, currentAddresses);
  },

  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await apiRequest<void>('/api/v1/auth/password', {
      method: 'PUT',
      body: {
        matKhauCu: currentPassword,
        matKhauMoi: newPassword,
      },
    });
  },

  async requestPasswordReset(email: string): Promise<{ message: string; demoOtp?: string }> {
    return apiRequest<{ message: string; demoOtp?: string }>('/api/v1/auth/forgot-password', {
      method: 'POST',
      withAuth: false,
      body: { email: email.trim() },
    });
  },

  async resetPasswordWithOtp(
    email: string,
    otp: string,
    newPassword: string
  ): Promise<void> {
    await apiRequest<void>('/api/v1/auth/reset-password', {
      method: 'POST',
      withAuth: false,
      body: {
        email: email.trim(),
        otp: otp.trim(),
        matKhauMoi: newPassword,
      },
    });
  },

  async getPublicStoreConfig(): Promise<{
    storeName: string;
    tagline: string;
    hotline: string;
    supportEmail: string;
    headquartersAddress: string;
    defaultShippingFee: number;
    freeShippingThreshold: number;
    bankName: string;
    bankAccountNumber: string;
    bankAccountHolder: string;
  }> {
    const dto = await apiRequest<{
      tenCuaHang: string;
      khauHieu: string;
      hotline: string;
      emailHoTro: string;
      diaChiTruSo: string;
      phiShipMacDinh: number;
      mienPhiShipTu: number;
      nganHangTen: string;
      nganHangSoTk: string;
      nganHangChuTk: string;
    }>('/api/v1/store-config', { withAuth: false });

    return {
      storeName: dto.tenCuaHang,
      tagline: dto.khauHieu || '',
      hotline: dto.hotline,
      supportEmail: dto.emailHoTro,
      headquartersAddress: dto.diaChiTruSo,
      defaultShippingFee: Number(dto.phiShipMacDinh ?? 25000),
      freeShippingThreshold: Number(dto.mienPhiShipTu ?? 300000),
      bankName: dto.nganHangTen,
      bankAccountNumber: dto.nganHangSoTk,
      bankAccountHolder: dto.nganHangChuTk,
    };
  },

  async getAddresses(): Promise<Address[]> {
    const dtos = await apiRequest<BackendAddressDto[]>('/api/v1/addresses');
    return dtos.map(mapAddress);
  },

  /**
   * Lưu địa chỉ (dùng chung cho cả Thêm mới và Chỉnh sửa):
   * - Nếu có `address.id` -> gọi PUT để cập nhật địa chỉ đó.
   * - Nếu chưa có `address.id` -> gọi POST để tạo địa chỉ mới.
   * - Luôn gọi lại getAddresses() sau khi lưu để đảm bảo địa chỉ mặc định nằm đầu & khớp 100% với PostgreSQL.
   */
  async saveAddress(address: Omit<Address, 'id'> & { id?: string }): Promise<Address[]> {
    const payload = {
      hoTenNguoiNhan: address.fullName.trim(),
      soDienThoai: address.phone.trim(),
      tinhThanh: address.province.trim(),
      quanHuyen: address.district.trim(),
      phuongXa: address.ward.trim(),
      diaChiChiTiet: address.streetAddress.trim(),
      macDinh: address.isDefault,
    };

    if (address.id) {
      await apiRequest<BackendAddressDto>(
        `/api/v1/addresses/${encodeURIComponent(address.id)}`,
        {
          method: 'PUT',
          body: payload,
        }
      );
    } else {
      await apiRequest<BackendAddressDto>('/api/v1/addresses', {
        method: 'POST',
        body: payload,
      });
    }

    return this.getAddresses();
  },

  async setDefaultAddress(addressId: string): Promise<Address[]> {
    await apiRequest<BackendAddressDto>(
      `/api/v1/addresses/${encodeURIComponent(addressId)}/default`,
      {
        method: 'PATCH',
      }
    );
    return this.getAddresses();
  },

  /**
   * Xóa địa chỉ: API Backend trả trực tiếp danh sách mới nhất sau khi đã xử lý Quy tắc 1A
   * (nếu vừa xóa địa chỉ mặc định mà vẫn còn địa chỉ khác -> tự đôn 1 địa chỉ lên mặc định).
   */
  async deleteAddress(addressId: string): Promise<Address[]> {
    const dtos = await apiRequest<BackendAddressDto[]>(
      `/api/v1/addresses/${encodeURIComponent(addressId)}`,
      {
        method: 'DELETE',
      }
    );
    return dtos.map(mapAddress);
  },

  logout(): void {
    clearAuthToken();
  },
};
