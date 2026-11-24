import {
  Order,
  OrderStatus,
  PaymentMethod,
  PaymentStatus,
  ReturnRequest,
  ReturnStatus,
} from '@shared/types';
import { apiRequest } from './httpClient';

// Khuôn dữ liệu OrderDto trả về từ Backend Java (/api/v1/orders/**)
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

export interface BackendReturnRequestDto {
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

export interface BackendOrderDto {
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

export function mapBackendOrderStatus(
  status: BackendOrderDto['trangThaiDon']
): OrderStatus {
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

function mapBackendPaymentMethod(
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

export function toBackendPaymentMethod(
  method: PaymentMethod
): BackendOrderDto['phuongThucThanhToan'] {
  switch (method) {
    case 'COD':
      return 'TIEN_MAT_COD';
    case 'BANK_QR':
      return 'CHUYEN_KHOAN_QR';
    case 'EWALLET':
      return 'VI_DIEN_TU';
  }
}

function mapBackendPaymentStatus(
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

function mapBackendReturnStatus(
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
    status: mapBackendReturnStatus(dto.trangThai),
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
    paymentMethod: mapBackendPaymentMethod(dto.phuongThucThanhToan),
    paymentStatus: mapBackendPaymentStatus(dto.trangThaiThanhToan),
    status: mapBackendOrderStatus(dto.trangThaiDon),
    note: dto.ghiChu ?? undefined,
    timeline: (dto.timeline || []).map((step) => ({
      status: mapBackendOrderStatus(
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
 * Service Đơn hàng phía Khách hàng Mobile App (Bước 6 - Phần 1, 2 + Phương án 1):
 * - Đặt hàng thật qua POST /api/v1/orders (trừ kho, xóa món đã chọn, tăng lượt voucher, tạo timeline).
 * - Xem danh sách & chi tiết hành trình đơn qua GET /api/v1/orders.
 * - Hủy đơn CHO_XAC_NHAN qua POST /api/v1/orders/{id}/cancel.
 * - Bấm "Đã nhận được hàng" khi đơn DANG_GIAO qua POST /api/v1/orders/{id}/confirm-received.
 */
export const orderService = {
  async getCustomerOrders(
      _userId ?: string,
      status ?: OrderStatus
    ): Promise < Order[] > {
      const query = status
        ? `?trangThai=${encodeURIComponent(
          status === 'PENDING'
            ? 'CHO_XAC_NHAN'
            : status === 'PREPARING'
              ? 'DANG_DONG_GOI'
              : status === 'SHIPPING'
                ? 'DANG_GIAO'
                : status === 'DELIVERED'
                  ? 'DA_GIAO'
                  : 'DA_HUY'
        )}`
        : '';
      const rows = await apiRequest<BackendOrderDto[]>(`/api/v1/orders${query}`);
      return rows.map(mapOrder);
    },

        async getCustomerOrderDetail(orderId: string): Promise<Order> {
          const dto = await apiRequest<BackendOrderDto>(
            `/api/v1/orders/${encodeURIComponent(orderId)}`
          );
          return mapOrder(dto);
        },

        async createOrder(payload: {
          addressId?: string;
          voucherCode?: string;
          paymentMethod: PaymentMethod;
          note?: string;
        }): Promise<Order> {
            const dto = await apiRequest<BackendOrderDto>('/api/v1/orders', {
              method: 'POST',
              body: {
                soDiaChiId: payload.addressId || undefined,
                phuongThucThanhToan: toBackendPaymentMethod(payload.paymentMethod),
                maGiamGiaCode: payload.voucherCode?.trim() || undefined,
                ghiChu: payload.note?.trim() || undefined,
              },
            });
            return mapOrder(dto);
          },

            async cancelCustomerOrder(orderId: string, reason: string): Promise < Order > {
    const dto = await apiRequest<BackendOrderDto>(
                  `/api/v1/orders/${encodeURIComponent(orderId)}/cancel`,
                  {
                    method: 'POST',
                    body: { lyDo: reason.trim() },
                  }
                );
              return mapOrder(dto);
            },

            async confirmOrderReceived(orderId: string): Promise < Order > {
              const dto = await apiRequest<BackendOrderDto>(
                `/api/v1/orders/${encodeURIComponent(orderId)}/confirm-received`,
                {
                  method: 'POST',
                }
              );
              return mapOrder(dto);
            },

            // Bước 7 - Phần 2: Gửi yêu cầu đổi trả thật qua POST /api/v1/returns
            async submitReturnRequest(payload: {
              orderId: string;
              reason: string;
              description: string;
              evidenceImages: string[];
            }): Promise<ReturnRequest> {
              const dto = await apiRequest<BackendReturnRequestDto>('/api/v1/returns', {
                method: 'POST',
                body: {
                  donHangId: payload.orderId,
                  lyDo: payload.reason.trim(),
                  moTa: payload.description.trim(),
                  anhMinhChungUrls: payload.evidenceImages.filter((u) => u && u.trim()),
                },
              });
              return mapReturnRequest(dto);
            },
};
