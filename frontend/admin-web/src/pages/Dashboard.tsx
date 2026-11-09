import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowRight,
  BadgeDollarSign,
  ShoppingBag,
  TrendingUp,
  UserPlus,
} from 'lucide-react';
import { Book, DashboardStats, formatCurrencyVND } from '@shared/types';
import { adminStatsService } from '@/services/adminStatsService';
import { StatCard } from '@/components/StatCard';
import { RevenueChart } from '@/components/RevenueChart';

export const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [topBooks, setTopBooks] = useState<Book[]>([]);
  const [lowStockBooks, setLowStockBooks] = useState<Book[]>([]);

  useEffect(() => {
    adminStatsService.getDashboardStats().then((res) => {
      setStats(res.stats);
      setTopBooks(res.topSellingBooks);
      setLowStockBooks(res.lowStockBooks);
    });
  }, []);

  if (!stats) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="h-32 bg-white rounded-xl border border-slate-200 animate-pulse" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div>
          <h1 className="text-xl font-bold text-slate-900">
            Dashboard Quản Trị & Thống Kê Doanh Nghiệp
          </h1>
          <p className="text-sm text-slate-500">
            Số liệu kinh doanh thời gian thực của hệ sinh thái Tri Thức Việt BookStore
          </p>
        </div>
      </div>

      {/* KPI STAT CARDS */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard
          title="Doanh thu tháng này"
          value={formatCurrencyVND(stats.totalRevenue)}
          growth={stats.revenueGrowth}
          subtitle="so với tháng trước"
          icon={BadgeDollarSign}
          tone="emerald"
        />
        <StatCard
          title="Tổng đơn hàng hoàn tất"
          value={`${stats.totalOrders} đơn`}
          growth={stats.ordersGrowth}
          subtitle="tỷ lệ giao thành công 98.4%"
          icon={ShoppingBag}
          tone="blue"
        />
        <StatCard
          title="Khách hàng mới đăng ký"
          value={`${stats.newCustomers} độc giả`}
          growth={stats.customersGrowth}
          subtitle="từ Mobile App Android/iOS"
          icon={UserPlus}
          tone="emerald"
        />
        <StatCard
          title="Cảnh báo Sách sắp hết (< 5)"
          value={`${stats.lowStockCount} đầu sách`}
          subtitle={`${stats.pendingReturnsCount} yêu cầu đổi trả chờ duyệt`}
          icon={AlertTriangle}
          tone="amber"
        />
      </div>

      {/* REVENUE CHART + LOW STOCK ALERTS */}
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <div className="xl:col-span-2">
          <RevenueChart
            byDay={stats.revenueByDay}
            byMonth={stats.revenueByMonth}
            byYear={stats.revenueByYear}
          />
        </div>

        {/* Low Stock Alert Card */}
        <div className="bg-white rounded-xl border border-slate-200 p-5 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-amber-600" />
                <h3 className="text-base font-bold text-slate-900">
                  Cảnh báo Tồn kho (&lt; 5 cuốn)
                </h3>
              </div>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-100 text-amber-800">
                {lowStockBooks.length} đầu sách
              </span>
            </div>

            <div className="space-y-3">
              {lowStockBooks.map((book) => (
                <div
                  key={book.id}
                  className="p-3 rounded-lg border border-amber-200 bg-amber-50/50 flex items-center justify-between gap-3"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <img
                      src={book.coverUrl}
                      alt={book.title}
                      className="w-10 h-14 rounded object-cover shrink-0 border border-slate-200"
                    />
                    <div className="min-w-0">
                      <div className="text-sm font-semibold text-slate-900 truncate">
                        {book.title}
                      </div>
                      <div className="text-xs text-slate-500">ISBN: {book.isbn}</div>
                    </div>
                  </div>
                  <div className="text-right shrink-0">
                    <span className="inline-block px-2 py-1 rounded bg-rose-100 text-rose-700 text-xs font-bold">
                      Còn {book.stock} cuốn
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <button
            type="button"
            onClick={() => navigate('/inventory')}
            className="mt-4 w-full py-2.5 px-4 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-sm font-semibold flex items-center justify-center gap-2 cursor-pointer transition-colors"
          >
            <span>Tạo phiếu nhập kho ngay</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* TOP SELLING BOOKS TABLE */}
      <div className="bg-white rounded-xl border border-slate-200 p-5">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <TrendingUp className="w-5 h-5 text-emerald-700" />
            <h3 className="text-base font-bold text-slate-900">
              Top 5 Sách Bán Chạy Nhất Tháng
            </h3>
          </div>
          <button
            type="button"
            onClick={() => navigate('/books')}
            className="text-xs font-semibold text-emerald-700 hover:underline cursor-pointer"
          >
            Quản lý toàn bộ kho sách →
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse min-w-[720px]">
            <thead>
              <tr className="border-b border-slate-200 text-xs font-bold uppercase text-slate-500 bg-slate-50">
                <th className="py-3 px-4">Tựa sách</th>
                <th className="py-3 px-4">Danh mục</th>
                <th className="py-3 px-4">Giá bán</th>
                <th className="py-3 px-4">Số lượng đã bán</th>
                <th className="py-3 px-4">Doanh thu ước tính</th>
                <th className="py-3 px-4">Tồn kho</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 text-sm">
              {topBooks.map((book) => {
                const unitPrice = book.flashSalePrice ?? book.price;
                return (
                  <tr key={book.id} className="hover:bg-slate-50">
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-3">
                        <img
                          src={book.coverUrl}
                          alt={book.title}
                          className="w-9 h-12 rounded object-cover border border-slate-200 shrink-0"
                        />
                        <div>
                          <div className="font-semibold text-slate-900">{book.title}</div>
                          <div className="text-xs text-slate-500">{book.authorName}</div>
                        </div>
                      </div>
                    </td>
                    <td className="py-3 px-4 text-slate-600">{book.categoryName}</td>
                    <td className="py-3 px-4 font-semibold text-emerald-700">
                      {formatCurrencyVND(unitPrice)}
                    </td>
                    <td className="py-3 px-4 font-bold text-slate-900">
                      {book.soldCount.toLocaleString('vi-VN')} cuốn
                    </td>
                    <td className="py-3 px-4 font-semibold text-slate-800">
                      {formatCurrencyVND(unitPrice * book.soldCount)}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`px-2.5 py-1 rounded-full text-xs font-bold ${
                          book.stock < 5
                            ? 'bg-rose-100 text-rose-700'
                            : 'bg-emerald-50 text-emerald-700'
                        }`}
                      >
                        {book.stock} cuốn
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

