import { db } from '@shared/index';
import {
  DiscountType,
  InventoryReceipt,
  Review,
  StoreConfig,
  User,
  UserRole,
  Voucher,
} from '@shared/types';
import { adminRequest } from './adminHttpClient';

// Mở rộng Voucher dùng cho Web Admin để hiển thị phạm vi Danh mục áp dụng từ Backend
export interface AdminVoucher extends Voucher {
  categoryId?: string | null;
  categoryName?: string | null;
  remainingUses?: number;
}

// Khuôn dữ liệu VoucherDto trả về từ Backend Java (/api/admin/vouchers/**)
interface BackendVoucherDto {
  id: string;
  maCode: string;
  tieuDe: string;
  moTa: string | null;
  loaiGiam: 'CO_DINH' | 'PHAN_TRAM';
  giaTriGiam: number;
  giamToiDa: number | null;
  donToiThieu: number | null;
  gioiHanLuot: number;
  daSuDung: number;
  conLaiLuot: number;
  ngayBatDau: string;
  ngayKetThuc: string;
  dangHoatDong: boolean;
  danhMucId: string | null;
  tenDanhMucApDung: string | null;
}

function mapBackendVoucher(dto: BackendVoucherDto): AdminVoucher {
  const discountType: DiscountType =
    dto.loaiGiam === 'PHAN_TRAM' ? 'PERCENTAGE' : 'FIXED';
  return {
    id: dto.id,
    code: dto.maCode,
    title: dto.tieuDe,
    description: dto.moTa || '',
    discountType,
    discountValue: Number(dto.giaTriGiam ?? 0),
    maxDiscountAmount: dto.giamToiDa != null ? Number(dto.giamToiDa) : undefined,
    minOrderValue: Number(dto.donToiThieu ?? 0),
    usageLimit: Number(dto.gioiHanLuot ?? 0),
    usedCount: Number(dto.daSuDung ?? 0),
    remainingUses: Number(dto.conLaiLuot ?? 0),
    startDate: dto.ngayBatDau,
    endDate: dto.ngayKetThuc,
    isActive: Boolean(dto.dangHoatDong),
    categoryId: dto.danhMucId,
    categoryName: dto.tenDanhMucApDung,
  };
}

// Khuôn dữ liệu UserDto từ Backend Java (/api/admin/users/**)
interface BackendUserDto {
  id: string;
  hoTen: string;
  email: string;
  soDienThoai: string | null;
  vaiTro: UserRole;
  biKhoa: boolean;
  tongChiTieu: number | null;
  soDonHang: number | null;
  ngayTao: string;
}

function mapBackendUser(dto: BackendUserDto): User {
  return {
    id: dto.id,
    fullName: dto.hoTen,
    email: dto.email,
    phone: dto.soDienThoai || '',
    role: dto.vaiTro,
    isLocked: Boolean(dto.biKhoa),
    totalSpent: Number(dto.tongChiTieu ?? 0),
    orderCount: Number(dto.soDonHang ?? 0),
    createdAt: dto.ngayTao,
    addresses: [],
  };
}

export const adminSystemService = {
  // 1. INVENTORY & IMPORT RECEIPTS (Bước 8 - Khối A: kết nối Backend thật)
  async getInventoryReceipts(): Promise<InventoryReceipt[]> {
    const rows = await adminRequest<
      Array<{
        id: string;
        maPhieu: string;
        tenNhaCungCap: string;
        nguoiTao: string;
        ghiChu: string | null;
        tongSoLuong: number;
        tongTienNhap: number;
        ngayTao: string;
        items: Array<{
          id: string;
          sachId: string;
          tieuDeSach: string;
          maIsbn: string;
          soLuong: number;
          giaNhap: number;
        }>;
      }>
    >('/api/admin/inventory/receipts');

    return rows.map((r) => ({
      id: r.id,
      receiptCode: r.maPhieu,
      supplierName: r.tenNhaCungCap,
      createdBy: r.nguoiTao,
      note: r.ghiChu || '',
      totalQuantity: Number(r.tongSoLuong ?? 0),
      totalCost: Number(r.tongTienNhap ?? 0),
      createdAt: r.ngayTao,
      items: (r.items || []).map((i) => ({
        bookId: i.sachId,
        bookTitle: i.tieuDeSach,
        isbn: i.maIsbn,
        quantity: Number(i.soLuong ?? 0),
        importPrice: Number(i.giaNhap ?? 0),
      })),
    }));
  },

  async createInventoryReceipt(payload: {
    supplierName: string;
    note?: string;
    items: Array<{
      bookId: string;
      quantity: number;
      importPrice: number;
    }>;
  }): Promise<InventoryReceipt> {
    const saved = await adminRequest<{
      id: string;
      maPhieu: string;
      tenNhaCungCap: string;
      nguoiTao: string;
      ghiChu: string | null;
      tongSoLuong: number;
      tongTienNhap: number;
      ngayTao: string;
      items: Array<{
        id: string;
        sachId: string;
        tieuDeSach: string;
        maIsbn: string;
        soLuong: number;
        giaNhap: number;
      }>;
    }>('/api/admin/inventory/receipts', {
      method: 'POST',
      body: JSON.stringify({
        tenNhaCungCap: payload.supplierName.trim(),
        ghiChu: payload.note?.trim() || undefined,
        items: payload.items.map((i) => ({
          sachId: i.bookId,
          soLuong: Number(i.quantity),
          giaNhap: Number(i.importPrice),
        })),
      }),
    });

    return {
      id: saved.id,
      receiptCode: saved.maPhieu,
      supplierName: saved.tenNhaCungCap,
      createdBy: saved.nguoiTao,
      note: saved.ghiChu || '',
      totalQuantity: Number(saved.tongSoLuong ?? 0),
      totalCost: Number(saved.tongTienNhap ?? 0),
      createdAt: saved.ngayTao,
      items: (saved.items || []).map((i) => ({
        bookId: i.sachId,
        bookTitle: i.tieuDeSach,
        isbn: i.maIsbn,
        quantity: Number(i.soLuong ?? 0),
        importPrice: Number(i.giaNhap ?? 0),
      })),
    };
  },

  // 2. CUSTOMERS & STAFF RBAC (Bước 2 - Phần 3: kết nối Backend thật)
  async getCustomers(keyword?: string): Promise<User[]> {
    const query = keyword?.trim()
      ? `?keyword=${encodeURIComponent(keyword.trim())}`
      : '';
    const rows = await adminRequest<BackendUserDto[]>(
      `/api/admin/users/customers${query}`
    );
    return rows.map(mapBackendUser);
  },

  async getStaff(keyword?: string): Promise<User[]> {
    const query = keyword?.trim()
      ? `?keyword=${encodeURIComponent(keyword.trim())}`
      : '';
    const rows = await adminRequest<BackendUserDto[]>(
      `/api/admin/users/staff${query}`
    );
    return rows.map(mapBackendUser);
  },

  async getUsers(includeStaff = true): Promise<User[]> {
    const customers = await this.getCustomers();
    if (!includeStaff) {
      return customers;
    }
    const staff = await this.getStaff();
    return [...customers, ...staff];
  },

  async toggleUserLock(userId: string): Promise<User> {
    const updated = await adminRequest<BackendUserDto>(
      `/api/admin/users/${encodeURIComponent(userId)}/lock`,
      { method: 'PATCH' }
    );
    return mapBackendUser(updated);
  },

  async updateStaffRole(userId: string, role: UserRole): Promise<User> {
    const updated = await adminRequest<BackendUserDto>(
      `/api/admin/users/${encodeURIComponent(userId)}/role`,
      {
        method: 'PATCH',
        body: JSON.stringify({ vaiTro: role }),
      }
    );
    return mapBackendUser(updated);
  },

  async createStaffAccount(payload: {
    fullName: string;
    email: string;
    phone: string;
    role: UserRole;
    password?: string;
  }): Promise<User> {
    const created = await adminRequest<BackendUserDto>('/api/admin/users/staff', {
      method: 'POST',
      body: JSON.stringify({
        hoTen: payload.fullName.trim(),
        email: payload.email.trim(),
        soDienThoai: payload.phone.trim(),
        vaiTro: payload.role,
        matKhau: payload.password?.trim() || undefined,
      }),
    });
    return mapBackendUser(created);
  },

  // 3. VOUCHERS (Bước 4 - Phần 3: kết nối Backend thật), REVIEWS & PUSH NOTIFICATIONS
  async getVouchers(params?: {
    active?: boolean;
    keyword?: string;
  }): Promise<AdminVoucher[]> {
    const queryParams = new URLSearchParams();
    if (typeof params?.active === 'boolean') {
      queryParams.set('dangHoatDong', String(params.active));
    }
    if (params?.keyword?.trim()) {
      queryParams.set('keyword', params.keyword.trim());
    }
    const query = queryParams.toString() ? `?${queryParams.toString()}` : '';
    const rows = await adminRequest<BackendVoucherDto[]>(
      `/api/admin/vouchers${query}`
    );
    return rows.map(mapBackendVoucher);
  },

  async saveVoucher(
    payload: Partial<AdminVoucher> & { code: string; title: string }
  ): Promise<AdminVoucher> {
    const body = {
      maCode: payload.code.trim().toUpperCase(),
      tieuDe: payload.title.trim(),
      moTa: payload.description?.trim() || undefined,
      loaiGiam:
        payload.discountType === 'PERCENTAGE' ? 'PHAN_TRAM' : 'CO_DINH',
      giaTriGiam: Number(payload.discountValue),
      giamToiDa:
        payload.discountType === 'PERCENTAGE' && payload.maxDiscountAmount != null
          ? Number(payload.maxDiscountAmount)
          : null,
      donToiThieu:
        payload.minOrderValue != null ? Number(payload.minOrderValue) : 0,
      danhMucId: payload.categoryId?.trim() || null,
      gioiHanLuot: Number(payload.usageLimit),
      ngayBatDau: payload.startDate || undefined,
      ngayKetThuc: payload.endDate,
      dangHoatDong: payload.isActive,
    };

    const saved = payload.id
      ? await adminRequest<BackendVoucherDto>(
          `/api/admin/vouchers/${encodeURIComponent(payload.id)}`,
          { method: 'PUT', body: JSON.stringify(body) }
        )
      : await adminRequest<BackendVoucherDto>('/api/admin/vouchers', {
          method: 'POST',
          body: JSON.stringify(body),
        });
    return mapBackendVoucher(saved);
  },

  async toggleVoucherActive(voucherId: string): Promise<AdminVoucher> {
    const updated = await adminRequest<BackendVoucherDto>(
      `/api/admin/vouchers/${encodeURIComponent(voucherId)}/toggle`,
      { method: 'PATCH' }
    );
    return mapBackendVoucher(updated);
  },

  // 3. KIỂM DUYỆT ĐÁNH GIÁ & PHÁT SÓNG THÔNG BÁO (Bước 7 - Phần 1 & 3)
  async getReviews(keyword?: string): Promise<Review[]> {
    const query = keyword?.trim()
      ? `?keyword=${encodeURIComponent(keyword.trim())}`
      : '';
    const rows = await adminRequest<
      Array<{
        id: string;
        sachId: string;
        tieuDeSach: string;
        nguoiDungId: string;
        tenNguoiDung: string;
        donHangId: string | null;
        soSao: number;
        binhLuan: string;
        anhDanhGiaUrls: string[];
        biAn: boolean;
        ngayTao: string;
      }>
    >(`/api/admin/reviews${query}`);
    return rows.map((r) => ({
      id: r.id,
      bookId: r.sachId,
      bookTitle: r.tieuDeSach,
      userId: r.nguoiDungId,
      userName: r.tenNguoiDung,
      orderId: r.donHangId || '',
      rating: Number(r.soSao ?? 5),
      comment: r.binhLuan,
      imageUrls: r.anhDanhGiaUrls || [],
      isHidden: Boolean(r.biAn),
      createdAt: r.ngayTao,
    }));
  },

  async toggleReviewHidden(reviewId: string): Promise<void> {
    await adminRequest(`/api/admin/reviews/${encodeURIComponent(reviewId)}/toggle-hidden`, {
      method: 'PATCH',
    });
  },

  async sendPushNotification(
    title: string,
    message: string
  ): Promise<{ recipients: number }> {
    const res = await adminRequest<{ soKhachHangNhan: number }>(
      '/api/admin/notifications/broadcast',
      {
        method: 'POST',
        body: JSON.stringify({
          tieuDe: title.trim(),
          noiDung: message.trim(),
          loaiThongBao: 'KHUYEN_MAI',
        }),
      }
    );
    return { recipients: Number(res?.soKhachHangNhan ?? 0) };
  },

  // 4. STORE CONFIGURATION
  async getStoreConfig(): Promise<StoreConfig> {
    return { ...db.storeConfig };
  },

  async updateStoreConfig(nextConfig: StoreConfig): Promise<StoreConfig> {
    db.storeConfig = { ...nextConfig };
    return { ...db.storeConfig };
  },
};

