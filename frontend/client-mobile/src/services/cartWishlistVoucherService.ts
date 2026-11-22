import { Book, DiscountType, Voucher } from '@shared/types';
import { apiRequest } from '@/services/httpClient';

// --- Kiểu dữ liệu dùng trên Mobile App ---

export interface WishlistBookItem {
  sachId: string;
  tieuDe: string;
  tenTacGia: string;
  tenDanhMuc: string;
  giaBan: number;
  giaGoc: number;
  dangFlashSale: boolean;
  giaFlashSale?: number;
  donGiaApDung: number;
  tonKho: number;
  diemDanhGiaTb: number;
  anhBiaUrl: string;
  ngayThem: string;
}

export interface CartEntry {
  book: Book;
  quantity: number;
  selected: boolean;
}

export interface CartSnapshot {
  entries: CartEntry[];
  totalDistinctBooks: number;
  totalItemsCount: number;
  selectedBooksCount: number;
  selectedUnitsCount: number;
  allSelected: boolean;
  subtotal: number;
  shippingFee: number;
  freeShipThreshold: number;
  amountToFreeShip: number;
  estimatedTotalBeforeVoucher: number;
}

export interface ActiveVoucherItem extends Voucher {
  remainingUses: number;
  categoryId: string | null;
  categoryName: string | null;
}

export interface VoucherValidateResult {
  voucher: ActiveVoucherItem;
  subtotal: number;
  eligibleSubtotal: number;
  shippingFee: number;
  discountAmount: number;
  finalTotal: number;
}

// --- DTO gốc trả về từ Backend (Bước 4 - Phần 1, 2, 3) ---

interface BackendWishlistItemResponse {
  sachId: string;
  tieuDe: string;
  tenTacGia: string;
  tenDanhMuc: string;
  giaBan: number | string;
  giaGoc: number | string;
  dangFlashSale: boolean;
  giaFlashSale: number | string | null;
  tonKho: number;
  diemDanhGiaTb: number | string;
  anhBiaUrl: string;
  ngayThem: string;
}

interface BackendWishlistToggleResponse {
  sachId: string;
  daYeuThich: boolean;
  tongSoSachYeuThich: number;
}

interface BackendCartItemDto {
  sachId: string;
  tieuDe: string;
  tenTacGia: string;
  tenDanhMuc: string;
  anhBiaUrl: string;
  giaBan: number | string;
  giaGoc: number | string;
  dangFlashSale: boolean;
  giaFlashSale: number | string | null;
  donGiaApDung: number | string;
  soLuong: number;
  thanhTien: number | string;
  tonKho: number;
  hienThi: boolean;
  daChon: boolean;
  ngayCapNhat: string;
}

interface BackendCartResponseDto {
  items: BackendCartItemDto[];
  tongSoDauSach: number;
  tongSoLuongTrongGio: number;
  soMonDaChon: number;
  tongSoLuongDaChon: number;
  daChonTatCa: boolean;
  tamTinh: number | string;
  phiVanChuyen: number | string;
  nguongMienPhiShip: number | string;
  conThieuDeFreeship: number | string;
  tongDuKien: number | string;
}

interface BackendVoucherDto {
  id: string;
  maCode: string;
  tieuDe: string;
  moTa: string | null;
  loaiGiam: 'CO_DINH' | 'PHAN_TRAM';
  giaTriGiam: number | string;
  giamToiDa: number | string | null;
  donToiThieu: number | string;
  gioiHanLuot: number;
  daSuDung: number;
  conLaiLuot: number;
  ngayBatDau: string;
  ngayKetThuc: string;
  dangHoatDong: boolean;
  danhMucId: string | null;
  tenDanhMucApDung: string | null;
}

interface BackendVoucherValidationResponse {
  voucher: BackendVoucherDto;
  tamTinh: number | string;
  tamTinhApDungVoucher: number | string;
  phiVanChuyen: number | string;
  tienGiamGia: number | string;
  tongThanhToanDuKien: number | string;
}

// --- Hàm ánh xạ dữ liệu Backend -> Frontend ---

function mapWishlistItem(dto: BackendWishlistItemResponse): WishlistBookItem {
  const isFlash =
    Boolean(dto.dangFlashSale) &&
    dto.giaFlashSale != null &&
    Number(dto.giaFlashSale) > 0;
  const price = Number(dto.giaBan || 0);
  const flashPrice = isFlash ? Number(dto.giaFlashSale) : undefined;
  return {
    sachId: dto.sachId,
    tieuDe: dto.tieuDe,
    tenTacGia: dto.tenTacGia,
    tenDanhMuc: dto.tenDanhMuc,
    giaBan: price,
    giaGoc: Number(dto.giaGoc || 0),
    dangFlashSale: isFlash,
    giaFlashSale: flashPrice,
    donGiaApDung: flashPrice ?? price,
    tonKho: Number(dto.tonKho || 0),
    diemDanhGiaTb: Number(dto.diemDanhGiaTb || 0),
    anhBiaUrl: dto.anhBiaUrl,
    ngayThem: dto.ngayThem,
  };
}

function mapCartItem(dto: BackendCartItemDto): CartEntry {
  const isFlash =
    Boolean(dto.dangFlashSale) &&
    dto.giaFlashSale != null &&
    Number(dto.giaFlashSale) > 0;
  const book: Book = {
    id: dto.sachId,
    isbn: '',
    title: dto.tieuDe,
    slug: dto.sachId,
    authorId: '',
    authorName: dto.tenTacGia,
    publisherId: '',
    publisherName: '',
    categoryId: '',
    categoryName: dto.tenDanhMuc,
    price: Number(dto.giaBan || 0),
    originalPrice: Number(dto.giaGoc || 0),
    stock: Number(dto.tonKho || 0),
    soldCount: 0,
    ratingAvg: 5,
    ratingCount: 0,
    pageCount: 0,
    publishYear: 0,
    coverFormat: 'Bìa mềm',
    coverUrl: dto.anhBiaUrl,
    galleryUrls: dto.anhBiaUrl ? [dto.anhBiaUrl] : [],
    shortSummary: '',
    description: '',
    isFlashSale: isFlash,
    flashSalePrice: isFlash ? Number(dto.giaFlashSale) : undefined,
    isBestSeller: false,
    isVisible: Boolean(dto.hienThi),
    createdAt: dto.ngayCapNhat || new Date().toISOString(),
  };
  return {
    book,
    quantity: Number(dto.soLuong || 1),
    selected: Boolean(dto.daChon),
  };
}

function mapCartSnapshot(res: BackendCartResponseDto): CartSnapshot {
  return {
    entries: (res.items || []).map(mapCartItem),
    totalDistinctBooks: Number(res.tongSoDauSach || 0),
    totalItemsCount: Number(res.tongSoLuongTrongGio || 0),
    selectedBooksCount: Number(res.soMonDaChon || 0),
    selectedUnitsCount: Number(res.tongSoLuongDaChon || 0),
    allSelected: Boolean(res.daChonTatCa),
    subtotal: Number(res.tamTinh || 0),
    shippingFee: Number(res.phiVanChuyen || 0),
    freeShipThreshold: Number(res.nguongMienPhiShip || 300000),
    amountToFreeShip: Number(res.conThieuDeFreeship || 0),
    estimatedTotalBeforeVoucher: Number(res.tongDuKien || 0),
  };
}

function mapVoucher(dto: BackendVoucherDto): ActiveVoucherItem {
  const discountType: DiscountType =
    dto.loaiGiam === 'PHAN_TRAM' ? 'PERCENTAGE' : 'FIXED';
  return {
    id: dto.id,
    code: dto.maCode,
    title: dto.tieuDe,
    description: dto.moTa || '',
    discountType,
    discountValue: Number(dto.giaTriGiam || 0),
    maxDiscountAmount:
      dto.giamToiDa != null ? Number(dto.giamToiDa) : undefined,
    minOrderValue: Number(dto.donToiThieu || 0),
    usageLimit: Number(dto.gioiHanLuot || 0),
    usedCount: Number(dto.daSuDung || 0),
    remainingUses: Number(dto.conLaiLuot || 0),
    startDate: dto.ngayBatDau,
    endDate: dto.ngayKetThuc,
    isActive: Boolean(dto.dangHoatDong),
    categoryId: dto.danhMucId || null,
    categoryName: dto.tenDanhMucApDung || null,
  };
}

/**
 * Service Tủ sách yêu thích, Giỏ hàng & Voucher cho Mobile App (Bước 4).
 * Tất cả các API thao tác giỏ hàng/yêu thích/áp voucher đều yêu cầu JWT CUSTOMER.
 */
export const cartWishlistVoucherService = {
  // --- 1. Tủ sách yêu thích ---
  async getWishlist(): Promise<WishlistBookItem[]> {
    const dtos = await apiRequest<BackendWishlistItemResponse[]>('/api/v1/wishlists');
    return dtos.map(mapWishlistItem);
  },

  async toggleWishlist(bookId: string): Promise<BackendWishlistToggleResponse> {
    return apiRequest<BackendWishlistToggleResponse>(
      `/api/v1/wishlists/${encodeURIComponent(bookId)}/toggle`,
      { method: 'POST' }
    );
  },

  async removeWishlist(bookId: string): Promise<BackendWishlistToggleResponse> {
    return apiRequest<BackendWishlistToggleResponse>(
      `/api/v1/wishlists/${encodeURIComponent(bookId)}`,
      { method: 'DELETE' }
    );
  },

  // --- 2. Giỏ hàng ---
  async getCart(): Promise<CartSnapshot> {
    const res = await apiRequest<BackendCartResponseDto>('/api/v1/cart');
    return mapCartSnapshot(res);
  },

  async addToCart(sachId: string, soLuong = 1): Promise<CartSnapshot> {
    const res = await apiRequest<BackendCartResponseDto>('/api/v1/cart', {
      method: 'POST',
      body: { sachId, soLuong },
    });
    return mapCartSnapshot(res);
  },

  async updateCartItem(
    sachId: string,
    payload: { soLuong?: number; daChon?: boolean }
  ): Promise<CartSnapshot> {
    const res = await apiRequest<BackendCartResponseDto>(
      `/api/v1/cart/${encodeURIComponent(sachId)}`,
      {
        method: 'PUT',
        body: payload,
      }
    );
    return mapCartSnapshot(res);
  },

  async selectAllCartItems(daChon: boolean): Promise<CartSnapshot> {
    const res = await apiRequest<BackendCartResponseDto>('/api/v1/cart/select-all', {
      method: 'PATCH',
      body: { daChon },
    });
    return mapCartSnapshot(res);
  },

  async removeCartItem(sachId: string): Promise<CartSnapshot> {
    const res = await apiRequest<BackendCartResponseDto>(
      `/api/v1/cart/${encodeURIComponent(sachId)}`,
      { method: 'DELETE' }
    );
    return mapCartSnapshot(res);
  },

  // --- 3. Mã giảm giá (Voucher) ---
  async getActiveVouchers(): Promise<ActiveVoucherItem[]> {
    const dtos = await apiRequest<BackendVoucherDto[]>('/api/v1/vouchers/active', {
      withAuth: false,
    });
    return dtos.map(mapVoucher);
  },

  /**
   * Kiểm tra mã giảm giá dựa trên chính những sản phẩm đang được tick chọn trong giỏ hàng PostgreSQL
   * (khi không truyền tamTinh, Backend tự tính đúng cả mã toàn shop lẫn mã riêng theo danh mục).
   */
  async validateVoucher(
    maCode: string,
    tamTinh?: number
  ): Promise<VoucherValidateResult> {
    const body =
      tamTinh !== undefined ? { maCode: maCode.trim(), tamTinh } : { maCode: maCode.trim() };
    const res = await apiRequest<BackendVoucherValidationResponse>(
      '/api/v1/vouchers/validate',
      {
        method: 'POST',
        body,
      }
    );
    return {
      voucher: mapVoucher(res.voucher),
      subtotal: Number(res.tamTinh || 0),
      eligibleSubtotal: Number(res.tamTinhApDungVoucher || 0),
      shippingFee: Number(res.phiVanChuyen || 0),
      discountAmount: Number(res.tienGiamGia || 0),
      finalTotal: Number(res.tongThanhToanDuKien || 0),
    };
  },
};
