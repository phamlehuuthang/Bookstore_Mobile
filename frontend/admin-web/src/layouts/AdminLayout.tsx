import React, { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  BarChart3,
  Bell,
  BookOpen,
  Boxes,
  Download,
  FolderTree,
  LayoutDashboard,
  PanelLeftClose,
  PanelLeftOpen,
  ShoppingBag,
  Sparkles,
  TicketPercent,
  Users,
} from 'lucide-react';
import { db } from '@shared/index';
import { adminStatsService } from '@/services/adminStatsService';

const NAV_ITEMS = [
  { to: '/', label: 'Tổng quan & KPI', icon: LayoutDashboard, end: true },
  { to: '/books', label: 'Quản lý Sách', icon: BookOpen },
  { to: '/categories', label: 'Danh mục & NXB', icon: FolderTree },
  { to: '/orders', label: 'Đơn hàng & Đổi trả', icon: ShoppingBag },
  { to: '/inventory', label: 'Quản lý Kho', icon: Boxes },
  { to: '/customers', label: 'Khách hàng & Nhân sự', icon: Users },
  { to: '/vouchers', label: 'Marketing & Voucher', icon: TicketPercent },
  { to: '/reports', label: 'Báo cáo & Cấu hình', icon: BarChart3 },
];

export const AdminLayout: React.FC = () => {
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);

  const lowStockBooks = db.books.filter(
    (b) => b.stock < db.storeConfig.lowStockThreshold
  );
  const pendingReturns = db.orders.filter(
    (o) => o.returnRequest?.status === 'PENDING'
  );
  const alertCount = lowStockBooks.length + pendingReturns.length;

  return (
    <div className="min-h-screen flex bg-slate-50 text-slate-900">
      {/* FIXED DESKTOP SIDEBAR */}
      <aside
        className={`${
          collapsed ? 'w-20' : 'w-64'
        } bg-slate-900 text-slate-100 flex flex-col justify-between transition-all duration-200 shrink-0 border-r border-slate-800`}
      >
        <div>
          {/* Brand Logo Header */}
          <div className="h-16 px-4 flex items-center justify-between border-b border-slate-800">
            <div className="flex items-center gap-3 overflow-hidden">
              <div className="w-9 h-9 rounded-lg bg-emerald-600 flex items-center justify-center shrink-0 font-bold text-white">
                BS
              </div>
              {!collapsed && (
                <div className="truncate">
                  <div className="text-sm font-bold tracking-tight text-white truncate">
                    Tri Thức Việt ERP
                  </div>
                  <div className="text-[11px] text-emerald-400 font-medium">
                    Enterprise Admin v2.6
                  </div>
                </div>
              )}
            </div>
            <button
              type="button"
              onClick={() => setCollapsed(!collapsed)}
              className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 cursor-pointer"
              aria-label="Thu gọn thanh điều hướng"
            >
              {collapsed ? (
                <PanelLeftOpen className="w-4 h-4" />
              ) : (
                <PanelLeftClose className="w-4 h-4" />
              )}
            </button>
          </div>

          {/* Navigation Menu */}
          <nav className="p-3 space-y-1">
            {NAV_ITEMS.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors duration-150 ${
                      isActive
                        ? 'bg-emerald-600 text-white shadow-sm'
                        : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                    }`
                  }
                >
                  <Icon className="w-5 h-5 shrink-0" />
                  {!collapsed && <span className="truncate">{item.label}</span>}
                </NavLink>
              );
            })}
          </nav>
        </div>

        {/* Low Stock Warehouse Alert Footer inside Sidebar */}
        {!collapsed && lowStockBooks.length > 0 && (
          <div className="m-3 p-3 rounded-xl bg-amber-950/60 border border-amber-700/50 text-amber-200 text-xs space-y-1.5">
            <div className="flex items-center gap-1.5 font-bold text-amber-300">
              <AlertTriangle className="w-4 h-4 shrink-0" />
              <span>Cảnh báo tồn kho ({lowStockBooks.length})</span>
            </div>
            <p className="text-[11px] text-amber-200/90 leading-relaxed">
              Có {lowStockBooks.length} đầu sách dưới định mức {db.storeConfig.lowStockThreshold}{' '}
              cuốn cần nhập bổ sung.
            </p>
            <button
              type="button"
              onClick={() => navigate('/inventory')}
              className="w-full py-1.5 rounded-md bg-amber-600 hover:bg-amber-500 text-white font-semibold cursor-pointer"
            >
              Tạo phiếu nhập kho
            </button>
          </div>
        )}
      </aside>

      {/* MAIN CONTENT AREA */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* TOP HEADER */}
        <header className="h-16 bg-white border-b border-slate-200 px-6 flex items-center justify-between sticky top-0 z-30">
          <div className="flex items-center gap-3">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
              <Sparkles className="w-3.5 h-3.5" />
              Web Dashboard Máy Tính
            </span>
            <span className="text-xs text-slate-500 hidden md:inline">
              Đồng bộ thời gian thực với Ứng dụng Di động Khách hàng
            </span>
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => adminStatsService.exportRevenueExcelCsv()}
              className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg border border-slate-300 bg-white hover:bg-slate-50 text-xs font-semibold text-slate-700 cursor-pointer transition-colors"
            >
              <Download className="w-4 h-4 text-emerald-700" />
              Xuất báo cáo Excel (CSV)
            </button>

            {/* Notification Bell */}
            <div className="relative">
              <button
                type="button"
                onClick={() => setNotifOpen(!notifOpen)}
                className="relative p-2 rounded-lg border border-slate-200 hover:bg-slate-100 text-slate-700 cursor-pointer"
                aria-label="Thông báo hệ thống"
              >
                <Bell className="w-5 h-5" />
                {alertCount > 0 && (
                  <span className="absolute -top-1 -right-1 w-5 h-5 rounded-full bg-rose-600 text-white text-[10px] font-bold flex items-center justify-center">
                    {alertCount}
                  </span>
                )}
              </button>

              {notifOpen && (
                <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl border border-slate-200 shadow-lg p-3 z-50 space-y-2">
                  <div className="text-xs font-bold text-slate-900 pb-2 border-b border-slate-100">
                    Cảnh báo Vận hành & Kho ({alertCount})
                  </div>
                  {lowStockBooks.map((b) => (
                    <div
                      key={b.id}
                      onClick={() => {
                        setNotifOpen(false);
                        navigate('/inventory');
                      }}
                      className="p-2 rounded-lg hover:bg-slate-50 cursor-pointer text-xs"
                    >
                      <div className="font-semibold text-amber-700">
                        ⚠️ Sắp hết hàng: {b.title}
                      </div>
                      <div className="text-slate-500">
                        Tồn kho thực tế chỉ còn {b.stock} cuốn
                      </div>
                    </div>
                  ))}
                  {pendingReturns.map((o) => (
                    <div
                      key={o.id}
                      onClick={() => {
                        setNotifOpen(false);
                        navigate('/orders');
                      }}
                      className="p-2 rounded-lg hover:bg-slate-50 cursor-pointer text-xs"
                    >
                      <div className="font-semibold text-rose-700">
                        🔄 Yêu cầu đổi trả mới: Đơn {o.orderCode}
                      </div>
                      <div className="text-slate-500">
                        Khách hàng: {o.customerName}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Admin Profile Badge */}
            <div className="flex items-center gap-2.5 pl-3 border-l border-slate-200">
              <div className="w-9 h-9 rounded-full bg-slate-900 text-white text-xs font-bold flex items-center justify-center">
                QB
              </div>
              <div className="hidden sm:block text-left">
                <div className="text-xs font-bold text-slate-900">
                  Phạm Quốc Bảo
                </div>
                <div className="text-[11px] text-emerald-700 font-semibold">
                  ROLE: ADMIN / OWNER
                </div>
              </div>
            </div>
          </div>
        </header>

        {/* SCROLLABLE PAGE CONTENT */}
        <main className="flex-1 p-6 overflow-y-auto max-w-[1600px] w-full mx-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

