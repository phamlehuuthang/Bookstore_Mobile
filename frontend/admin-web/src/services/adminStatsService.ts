import { Book, DashboardStats } from '@shared/types';
import { ADMIN_API_BASE_URL, getAdminToken } from './adminAuthService';
import { adminRequest } from './adminHttpClient';

interface BackendDashboardBookItemDto {
  id: string;
  maIsbn: string;
  tieuDe: string;
  tenTacGia: string;
  tenDanhMuc: string;
  tenNhaXuatBan: string;
  anhBiaUrl: string;
  giaBan: number;
  giaGoc: number;
  dangFlashSale: boolean;
  giaFlashSale: number | null;
  tonKho: number;
  daBan: number;
}

interface BackendRevenuePointDto {
  label: string;
  revenue: number;
  orders: number;
  profit: number;
}

interface BackendAdminDashboardDto {
  totalRevenue: number | null;
  revenueGrowth: number | null;
  totalOrders: number;
  ordersGrowth: number;
  newCustomers: number;
  customersGrowth: number;
  pendingOrdersCount: number;
  pendingReturnsCount: number;
  lowStockCount: number;
  lowStockThreshold: number;
  revenueByDay: BackendRevenuePointDto[] | null;
  revenueByMonth: BackendRevenuePointDto[] | null;
  revenueByYear: BackendRevenuePointDto[] | null;
  topSellingBooks: BackendDashboardBookItemDto[];
  lowStockBooks: BackendDashboardBookItemDto[];
}

function mapDashboardBook(dto: BackendDashboardBookItemDto): Book {
  const isFlash =
    Boolean(dto.dangFlashSale) && dto.giaFlashSale != null && Number(dto.giaFlashSale) > 0;
  return {
    id: dto.id,
    isbn: dto.maIsbn,
    title: dto.tieuDe,
    slug: dto.id,
    authorId: '',
    authorName: dto.tenTacGia,
    publisherId: '',
    publisherName: dto.tenNhaXuatBan,
    categoryId: '',
    categoryName: dto.tenDanhMuc,
    price: Number(dto.giaBan ?? 0),
    originalPrice: Number(dto.giaGoc ?? 0),
    stock: Number(dto.tonKho ?? 0),
    soldCount: Number(dto.daBan ?? 0),
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
    isVisible: true,
    createdAt: '',
  };
}

/**
 * Service Thống kê Dashboard (theo RBAC) & Xuất Báo cáo Doanh thu CSV thật từ Backend (Bước 8).
 */
export const adminStatsService = {
  async getDashboardStats(): Promise<{
    stats: DashboardStats;
    canViewFinance: boolean;
    lowStockThreshold: number;
    topSellingBooks: Book[];
    lowStockBooks: Book[];
  }> {
    const dto = await adminRequest<BackendAdminDashboardDto>('/api/admin/dashboard');
    const canViewFinance = dto.totalRevenue != null && dto.revenueByMonth != null;

    return {
      canViewFinance,
      lowStockThreshold: Number(dto.lowStockThreshold ?? 5),
      stats: {
        totalRevenue: Number(dto.totalRevenue ?? 0),
        revenueGrowth: Number(dto.revenueGrowth ?? 0),
        totalOrders: Number(dto.totalOrders ?? 0),
        ordersGrowth: Number(dto.ordersGrowth ?? 0),
        newCustomers: Number(dto.newCustomers ?? 0),
        customersGrowth: Number(dto.customersGrowth ?? 0),
        lowStockCount: Number(dto.lowStockCount ?? 0),
        pendingOrdersCount: Number(dto.pendingOrdersCount ?? 0),
        pendingReturnsCount: Number(dto.pendingReturnsCount ?? 0),
        revenueByDay: (dto.revenueByDay || []).map((p) => ({
          label: p.label,
          revenue: Number(p.revenue ?? 0),
          orders: Number(p.orders ?? 0),
          profit: Number(p.profit ?? 0),
        })),
        revenueByMonth: (dto.revenueByMonth || []).map((p) => ({
          label: p.label,
          revenue: Number(p.revenue ?? 0),
          orders: Number(p.orders ?? 0),
          profit: Number(p.profit ?? 0),
        })),
        revenueByYear: (dto.revenueByYear || []).map((p) => ({
          label: p.label,
          revenue: Number(p.revenue ?? 0),
          orders: Number(p.orders ?? 0),
          profit: Number(p.profit ?? 0),
        })),
      },
      topSellingBooks: (dto.topSellingBooks || []).map(mapDashboardBook),
      lowStockBooks: (dto.lowStockBooks || []).map(mapDashboardBook),
    };
  },

  async exportRevenueExcelCsv(): Promise<void> {
    await downloadAdminCsv(
      '/api/admin/reports/revenue-export',
      `Bao_Cao_Doanh_Thu_BookStore_${new Date().toISOString().slice(0, 10)}.csv`,
      'Không thể xuất báo cáo doanh thu'
    );
  },

  async exportOrdersExcelCsv(): Promise<void> {
    await downloadAdminCsv(
      '/api/admin/reports/orders-export',
      `Bao_Cao_Don_Hang_BookStore_${new Date().toISOString().slice(0, 10)}.csv`,
      'Không thể xuất báo cáo đơn hàng'
    );
  },

  async exportInventoryExcelCsv(): Promise<void> {
    await downloadAdminCsv(
      '/api/admin/reports/inventory-export',
      `Bao_Cao_Ton_Kho_BookStore_${new Date().toISOString().slice(0, 10)}.csv`,
      'Không thể xuất báo cáo tồn kho'
    );
  },
};

async function downloadAdminCsv(
  endpoint: string,
  filename: string,
  errorPrefix: string
): Promise<void> {
  const token = getAdminToken();
  const res = await fetch(`${ADMIN_API_BASE_URL}${endpoint}`, {
    method: 'GET',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    throw new Error(`${errorPrefix} (HTTP ${res.status}).`);
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
