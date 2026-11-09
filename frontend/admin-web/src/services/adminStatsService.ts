import { db } from '@shared/index';
import { Book, DashboardStats, formatCurrencyVND } from '@shared/types';

export const adminStatsService = {
  async getDashboardStats(): Promise<{
    stats: DashboardStats;
    topSellingBooks: Book[];
    lowStockBooks: Book[];
  }> {
    const lowStockBooks = db.books.filter(
      (b) => b.stock < db.storeConfig.lowStockThreshold
    );
    const pendingOrdersCount = db.orders.filter((o) => o.status === 'PENDING').length;
    const pendingReturnsCount = db.orders.filter(
      (o) => o.returnRequest?.status === 'PENDING'
    ).length;

    const topSellingBooks = [...db.books]
      .sort((a, b) => b.soldCount - a.soldCount)
      .slice(0, 5);

    return {
      stats: {
        ...db.stats,
        lowStockCount: lowStockBooks.length,
        pendingOrdersCount,
        pendingReturnsCount,
      },
      topSellingBooks,
      lowStockBooks,
    };
  },

  exportRevenueExcelCsv(): void {
    const rows = [
      ['Kỳ báo cáo', 'Doanh thu (VND)', 'Số đơn hàng', 'Lợi nhuận gộp (VND)'],
      ...db.stats.revenueByMonth.map((r) => [
        r.label,
        r.revenue.toString(),
        r.orders.toString(),
        r.profit.toString(),
      ]),
      [],
      ['Top Sách Bán Chạy', 'Mã ISBN', 'Giá bán', 'Đã bán', 'Tồn kho'],
      ...db.books.map((b) => [
        `"${b.title}"`,
        b.isbn,
        formatCurrencyVND(b.flashSalePrice ?? b.price),
        b.soldCount.toString(),
        b.stock.toString(),
      ]),
    ];

    const csvContent =
      '\uFEFF' + rows.map((row) => row.join(',')).join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute(
      'download',
      `Bao_Cao_Doanh_Thu_BookStore_${new Date().toISOString().slice(0, 10)}.csv`
    );
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  },
};

