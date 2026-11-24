import {
  Order,
  OrderStatus,
  PaymentMethod,
  PaymentStatus,
  ReturnRequest,
  ReturnStatus,
} from '@shared/types';
import { adminRequest } from './adminHttpClient';

interface BackendOrderItemDto {
  id: string;
  sachId: string;
  tieuDeSach: string;
  anhBiaUrl: string;
  tenTacGia: string;
  donGia: number;
  soLuong: number;
  thanhTien: number;
}

interface BackendOrderTimelineDto {
  id: string;
  trangThai: string;
  tieuDe: string;
  moTa: string | null;
  thoiGian: string;
}

interface BackendReturnRequestDto {
  id: string;
  donHangId: string;
  maDonHang: string;
  tenKhachHang: string;
  sdtKhachHang: string;
  lyDo: string;
  moTa: string;
  anhMinhChungUrls: string[];
  trangThai: 'CHO_DUYET' | 'DA_CHAP_THUAN' | 'TU_CHOI';
  ghiChuAdmin: string | null;
  ngayTao: string;
  ngayXuLy: string | null;
}

interface BackendOrderDto {
  id: string;
  maDonHang: string;
  nguoiDungId: string;
  tenKhachHang: string;
  sdtKhachHang: string;
  diaChiGiaoHang: string;
  tamTinh: number;
  phiVanChuyen: number;
  tienGiamGia: number;
  maGiamGiaCode: string | null;
  tongThanhToan: number;
  phuongThucThanhToan: 'TIEN_MAT_COD' | 'CHUYEN_KHOAN_QR' | 'VI_DIEN_TU';
  trangThaiThanhToan: 'CHUA_THANH_TOAN' | 'DA_THANH_TOAN' | 'DA_HOAN_TIEN';
  trangThaiDon:
  | 'CHO_XAC_NHAN'
  | 'DANG_DONG_GOI'
  | 'DANG_GIAO'
  | 'DA_GIAO'
  | 'DA_HUY';
  ghiChu: string | null;
  daDanhGia: boolean;
  ngayTao: string;
  items: BackendOrderItemDto[];
  timeline: BackendOrderTimelineDto[];
  returnRequest?: BackendReturnRequestDto | null;
}

function mapStatusToUi(status: BackendOrderDto['trangThaiDon']): OrderStatus {
  switch (status) {
    case 'CHO_XAC_NHAN':
      return 'PENDING';
    case 'DANG_DONG_GOI':
      return 'PREPARING';
    case 'DANG_GIAO':
      return 'SHIPPING';
    case 'DA_GIAO':
      return 'DELIVERED';
    case 'DA_HUY':
      return 'CANCELLED';
  }
}

function mapStatusToBackend(
  status: OrderStatus
): BackendOrderDto['trangThaiDon'] {
  switch (status) {
    case 'PENDING':
      return 'CHO_XAC_NHAN';
    case 'PREPARING':
      return 'DANG_DONG_GOI';
    case 'SHIPPING':
      return 'DANG_GIAO';
    case 'DELIVERED':
      return 'DA_GIAO';
    case 'CANCELLED':
      return 'DA_HUY';
  }
}

function mapPaymentMethod(
  method: BackendOrderDto['phuongThucThanhToan']
): PaymentMethod {
  switch (method) {
    case 'TIEN_MAT_COD':
      return 'COD';
    case 'CHUYEN_KHOAN_QR':
      return 'BANK_QR';
    case 'VI_DIEN_TU':
      return 'EWALLET';
  }
}

function mapPaymentStatus(
  status: BackendOrderDto['trangThaiThanhToan']
): PaymentStatus {
  switch (status) {
    case 'CHUA_THANH_TOAN':
      return 'UNPAID';
    case 'DA_THANH_TOAN':
      return 'PAID';
    case 'DA_HOAN_TIEN':
      return 'REFUNDED';
  }
}

function mapReturnStatusToUi(
  status: BackendReturnRequestDto['trangThai']
): ReturnStatus {
  switch (status) {
    case 'CHO_DUYET':
      return 'PENDING';
    case 'DA_CHAP_THUAN':
      return 'APPROVED';
    case 'TU_CHOI':
      return 'REJECTED';
  }
}

function mapReturnRequest(dto: BackendReturnRequestDto): ReturnRequest {
  return {
    id: dto.id,
    orderId: dto.donHangId,
    orderCode: dto.maDonHang,
    customerName: dto.tenKhachHang,
    reason: dto.lyDo,
    description: dto.moTa,
    evidenceImages: dto.anhMinhChungUrls || [],
    status: mapReturnStatusToUi(dto.trangThai),
    adminNote: dto.ghiChuAdmin ?? undefined,
    createdAt: dto.ngayTao,
    resolvedAt: dto.ngayXuLy ?? undefined,
  };
}

function mapOrder(dto: BackendOrderDto): Order {
  return {
    id: dto.id,
    orderCode: dto.maDonHang,
    userId: dto.nguoiDungId,
    customerName: dto.tenKhachHang,
    customerPhone: dto.sdtKhachHang,
    shippingAddress: {
      id: `addr-${dto.id}`,
      fullName: dto.tenKhachHang,
      phone: dto.sdtKhachHang,
      province: '',
      district: '',
      ward: '',
      streetAddress: dto.diaChiGiaoHang,
      isDefault: false,
    },
    items: (dto.items || []).map((item) => ({
      bookId: item.sachId,
      title: item.tieuDeSach,
      coverUrl: item.anhBiaUrl,
      authorName: item.tenTacGia,
      unitPrice: Number(item.donGia ?? 0),
      quantity: Number(item.soLuong ?? 1),
    })),
    subtotal: Number(dto.tamTinh ?? 0),
    shippingFee: Number(dto.phiVanChuyen ?? 0),
    discountAmount: Number(dto.tienGiamGia ?? 0),
    voucherCode: dto.maGiamGiaCode ?? undefined,
    totalAmount: Number(dto.tongThanhToan ?? 0),
    paymentMethod: mapPaymentMethod(dto.phuongThucThanhToan),
    paymentStatus: mapPaymentStatus(dto.trangThaiThanhToan),
    status: mapStatusToUi(dto.trangThaiDon),
    note: dto.ghiChu ?? undefined,
    timeline: (dto.timeline || []).map((step) => ({
      status: mapStatusToUi(
        (step.trangThai as BackendOrderDto['trangThaiDon']) || 'CHO_XAC_NHAN'
      ),
      title: step.tieuDe,
      description: step.moTa || '',
      timestamp: step.thoiGian,
    })),
    returnRequest: dto.returnRequest ? mapReturnRequest(dto.returnRequest) : undefined,
    isReviewed: Boolean(dto.daDanhGia),
    createdAt: dto.ngayTao,
    updatedAt: dto.ngayTao,
  };
}

/**
 * Service Quản lý Đơn hàng trên Web Admin (Bước 6 - Phần 3):
 * - Lấy toàn bộ đơn hàng thật từ GET /api/admin/orders (hỗ trợ lọc trạng thái & tìm kiếm).
 * - Chuyển trạng thái đơn 1 chiều hoặc hủy đơn qua PATCH /api/admin/orders/{id}/status.
 */
export const adminOrderService = {
  async getAllOrders(status ?: OrderStatus, keyword ?: string): Promise < Order[] > {
      const params = new URLSearchParams();
      if(status) params.set('trangThai', mapStatusToBackend(status));
      if(keyword?.trim()) params.set('keyword', keyword.trim());
    const query = params.toString() ? `?${params.toString()}` : '';
    const rows = await adminRequest<BackendOrderDto[]>(`/api/admin/orders${query}`);
    return rows.map(mapOrder);
  },

  async updateStatus(
      orderId: string,
      status: OrderStatus,
      note ?: string
    ): Promise < Order > {
      const updated = await adminRequest<BackendOrderDto>(
        `/api/admin/orders/${encodeURIComponent(orderId)}/status`,
        {
          method: 'PATCH',
          body: JSON.stringify({
            trangThaiMoi: mapStatusToBackend(status),
            ghiChu: note?.trim() || undefined,
          }),
        }
      );
      return mapOrder(updated);
    },

      // Bước 7 - Phần 2: Phê duyệt / Từ chối yêu cầu đổi trả qua PATCH /api/admin/orders/{id}/return
      async handleReturnRequest(
        orderId: string,
        status: ReturnStatus,
        adminNote: string
      ): Promise<ReturnRequest> {
        const dto = await adminRequest<BackendReturnRequestDto>(
          `/api/admin/orders/${encodeURIComponent(orderId)}/return`,
          {
            method: 'PATCH',
            body: JSON.stringify({
              trangThai: status === 'APPROVED' ? 'DA_CHAP_THUAN' : 'TU_CHOI',
              ghiChuAdmin: adminNote?.trim() || undefined,
            }),
          }
        );
        return mapReturnRequest(dto);
      },
};
