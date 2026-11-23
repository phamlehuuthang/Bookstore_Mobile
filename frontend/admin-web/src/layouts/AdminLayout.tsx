import React, { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate, useOutletContext } from 'react-router-dom';
import {
  AlertTriangle,
  BarChart3,
  Bell,
  BookOpen,
  Boxes,
  CheckCircle2,
  Download,
  Eye,
  EyeOff,
  FolderTree,
  LayoutDashboard,
  Lock,
  LogOut,
  Mail,
  PanelLeftClose,
  PanelLeftOpen,
  ShieldAlert,
  ShieldCheck,
  ShoppingBag,
  Sparkles,
  TicketPercent,
  Users,
} from 'lucide-react';
import { db } from '@shared/index';
import { UserRole } from '@shared/types';
import { adminStatsService } from '@/services/adminStatsService';
import { StaffAccount, adminAuthService } from '@/services/adminAuthService';

interface NavItemConfig {
  to: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  end?: boolean;
  allowedRoles: Exclude<UserRole, 'CUSTOMER'>[];
}

const NAV_ITEMS: NavItemConfig[] = [
  {
    to: '/',
    label: 'Tổng quan & KPI',
    icon: LayoutDashboard,
    end: true,
    allowedRoles: ['ADMIN', 'WAREHOUSE_MANAGER', 'CSKH'],
  },
  {
    to: '/books',
    label: 'Quản lý Sách',
    icon: BookOpen,
    allowedRoles: ['ADMIN', 'WAREHOUSE_MANAGER', 'CSKH'],
  },
  {
    to: '/categories',
    label: 'Danh mục & NXB',
    icon: FolderTree,
    allowedRoles: ['ADMIN', 'WAREHOUSE_MANAGER', 'CSKH'],
  },
  {
    to: '/orders',
    label: 'Đơn hàng & Đổi trả',
    icon: ShoppingBag,
    allowedRoles: ['ADMIN', 'WAREHOUSE_MANAGER', 'CSKH'],
  },
  {
    to: '/inventory',
    label: 'Quản lý Kho',
    icon: Boxes,
    allowedRoles: ['ADMIN', 'WAREHOUSE_MANAGER'],
  },
  {
    to: '/customers',
    label: 'Khách hàng & Nhân sự',
    icon: Users,
    allowedRoles: ['ADMIN', 'CSKH'],
  },
  {
    to: '/vouchers',
    label: 'Marketing & Voucher',
    icon: TicketPercent,
    allowedRoles: ['ADMIN', 'CSKH'],
  },
  {
    to: '/reports',
    label: 'Báo cáo & Cấu hình',
    icon: BarChart3,
    allowedRoles: ['ADMIN'],
  },
];

export interface AdminOutletContext {
  currentStaff: StaffAccount;
}

/**
 * Hook giúp các trang con (Books, Categories, CustomersStaff...) đọc thông tin nhân sự và vai trò RBAC hiện tại.
 */
export function useAdminStaff(): StaffAccount {
  return useOutletContext<AdminOutletContext>().currentStaff;
}

export const AdminLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);

  // Trạng thái phiên nhân sự: bắt đầu là null và kiểm tra JWT cũ qua /auth/me để tránh tự đăng nhập tài khoản cứng
  const [currentStaff, setCurrentStaff] = useState<StaffAccount | null>(null);
  const [isCheckingSession, setIsCheckingSession] = useState(true);
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [loginError, setLoginError] = useState<string | null>(null);
  const [forgotModalInfo, setForgotModalInfo] = useState(false);

  // Chạy 1 lần lúc mount layout (hoặc sau khi F5) để khôi phục phiên từ token lưu trong localStorage/sessionStorage
  useEffect(() => {
    // Biến `mounted` ngăn việc gọi setState nếu người dùng đã rời khỏi component trước khi fetch xong
    let mounted = true;
    adminAuthService
      .restoreAdminSession()
      .then((staff) => {
        if (mounted && staff) {
          setCurrentStaff(staff);
        }
      })
      .finally(() => {
        if (mounted) {
          setIsCheckingSession(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  const lowStockBooks = db.books.filter(
    (b) => b.stock < db.storeConfig.lowStockThreshold
  );
  const pendingReturns = db.orders.filter(
    (o) => o.returnRequest?.status === 'PENDING'
  );
  const alertCount = lowStockBooks.length + pendingReturns.length;

  // Xử lý submit form đăng nhập: validate sơ bộ ở client rồi gọi POST /api/admin/auth/login để Backend xác thực + check vai trò
  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isLoggingIn) return;
    setLoginError(null);
    setForgotModalInfo(false);
    const normalized = loginEmail.trim();

    if (!normalized) {
      setLoginError('Vui lòng nhập địa chỉ email công vụ.');
      return;
    }

    if (!loginPassword.trim()) {
      setLoginError('Vui lòng nhập mật khẩu.');
      return;
    }

    setIsLoggingIn(true);
    try {
      const staff = await adminAuthService.loginAdmin(
        normalized,
        loginPassword,
        rememberMe
      );
      setCurrentStaff(staff);
      navigate('/');
    } catch (err) {
      // Hiển thị nguyên văn thông báo lỗi từ Backend (401 sai mật khẩu, 403 chặn CUSTOMER hoặc tài khoản bị khóa)
      setLoginError(
        err instanceof Error
          ? err.message
          : 'Đăng nhập thất bại. Vui lòng thử lại.'
      );
    } finally {
      setIsLoggingIn(false);
    }
  };

  // Đăng xuất: xóa token khỏi localStorage/sessionStorage và đưa giao diện về màn hình Đăng nhập
  const handleLogout = () => {
    adminAuthService.logoutAdmin();
    setNotifOpen(false);
    setCurrentStaff(null);
    setLoginPassword('');
    setLoginError(null);
    setForgotModalInfo(false);
  };

  // Màn hình chờ ngắn khi vừa F5: giữ trang trắng gọn thay vì nháy màn hình Đăng nhập 0.2s rồi biến mất
  if (isCheckingSession) {
    return (
      <div className="min-h-screen bg-slate-100 flex items-center justify-center p-4">
        <div className="text-sm font-medium text-slate-600">
          Đang kiểm tra phiên đăng nhập...
        </div>
      </div>
    );
  }

  // Nếu chưa có currentStaff (chưa đăng nhập hoặc token hết hạn) -> chặn toàn bộ Dashboard và chỉ render màn Login
  if (!currentStaff) {
    return (
      <div className="min-h-screen bg-slate-100 flex items-center justify-center p-4 md:p-8">
        <div className="max-w-4xl w-full bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden grid grid-cols-1 md:grid-cols-12">
          {/* Cột trái: Thông tin thương hiệu & hệ thống nội bộ */}
          <div className="md:col-span-5 bg-slate-900 text-white p-8 flex flex-col justify-between">
            <div className="space-y-6">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-emerald-600 flex items-center justify-center font-bold text-base tracking-tight">
                  BS
                </div>
                <div>
                  <div className="text-base font-bold tracking-tight">
                    Tri Thức Việt ERP
                  </div>
                  <div className="text-xs text-emerald-400 font-medium">
                    Hệ thống Quản trị Nhà sách
                  </div>
                </div>
              </div>

              <div className="space-y-3 pt-4">
                <h2 className="text-xl font-bold leading-snug">
                  Cổng điều hành & Quản lý vận hành nội bộ
                </h2>
                <p className="text-xs text-slate-300 leading-relaxed">
                  Hệ thống dành riêng cho Ban Quản trị, Bộ phận Quản lý Kho và Bộ phận
                  Chăm sóc Khách hàng của Nhà sách Tri Thức Việt.
                </p>
              </div>

              <div className="space-y-2.5 pt-2 text-xs text-slate-300">
                <div className="flex items-center gap-2.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>Quản lý danh mục, giá bán & tồn kho thời gian thực</span>
                </div>
                <div className="flex items-center gap-2.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>Xử lý đơn đặt hàng, phiếu nhập kho & yêu cầu đổi trả</span>
                </div>
                <div className="flex items-center gap-2.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>Phân quyền truy cập bảo mật theo từng vị trí nhân sự</span>
                </div>
              </div>
            </div>

            <div className="pt-8 border-t border-slate-800 text-[11px] text-slate-400">
              © 2026 Công ty Cổ phần Sách Tri Thức Việt. Bảo lưu mọi quyền.
            </div>
          </div>

          {/* Cột phải: Form đăng nhập thực tế */}
          <div className="md:col-span-7 p-8 md:p-10 flex flex-col justify-center">
            <div className="max-w-sm w-full mx-auto space-y-6">
              <div>
                <h1 className="text-2xl font-bold text-slate-900">Đăng nhập</h1>
                <p className="text-xs text-slate-500 mt-1">
                  Nhập tài khoản nhân sự của bạn để tiếp tục vào bảng điều khiển.
                </p>
              </div>

              {loginError && (
                <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-start gap-2.5">
                  <ShieldAlert className="w-4 h-4 shrink-0 mt-0.5 text-rose-600" />
                  <div className="space-y-0.5">
                    <div className="font-bold text-rose-900">Không thể đăng nhập</div>
                    <div className="leading-relaxed text-rose-700">{loginError}</div>
                  </div>
                </div>
              )}

              {forgotModalInfo && (
                <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs leading-relaxed">
                  Vui lòng liên hệ Quản trị viên hệ thống hoặc phòng CNTT nội bộ để được cấp lại mật khẩu nhân viên.
                </div>
              )}

              <form onSubmit={handleLoginSubmit} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                    Email đăng nhập
                  </label>
                  <div className="relative">
                    <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="email"
                      value={loginEmail}
                      onChange={(e) => setLoginEmail(e.target.value)}
                      placeholder="Nhập email công vụ"
                      autoComplete="username"
                      className="w-full pl-10 pr-3.5 py-2.5 rounded-lg border border-slate-300 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:border-emerald-600"
                    />
                  </div>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <label className="block text-xs font-semibold text-slate-700">
                      Mật khẩu
                    </label>
                    <button
                      type="button"
                      onClick={() => setForgotModalInfo(!forgotModalInfo)}
                      className="text-xs font-semibold text-emerald-700 hover:text-emerald-800 cursor-pointer"
                    >
                      Quên mật khẩu?
                    </button>
                  </div>
                  <div className="relative">
                    <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={loginPassword}
                      onChange={(e) => setLoginPassword(e.target.value)}
                      placeholder="Nhập mật khẩu"
                      autoComplete="current-password"
                      className="w-full pl-10 pr-10 py-2.5 rounded-lg border border-slate-300 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:border-emerald-600"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 cursor-pointer"
                      aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                    >
                      {showPassword ? (
                        <EyeOff className="w-4 h-4" />
                      ) : (
                        <Eye className="w-4 h-4" />
                      )}
                    </button>
                  </div>
                </div>

                <div className="flex items-center justify-between pt-1">
                  <label className="inline-flex items-center gap-2 text-xs text-slate-600 cursor-pointer select-none">
                    <input
                      type="checkbox"
                      checked={rememberMe}
                      onChange={(e) => setRememberMe(e.target.checked)}
                      className="rounded border-slate-300 text-emerald-600 focus:ring-emerald-600"
                    />
                    <span>Duy trì đăng nhập trên thiết bị này</span>
                  </label>
                </div>

                <button
                  type="submit"
                  disabled={isLoggingIn}
                  className="w-full py-2.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 disabled:opacity-60 text-white font-semibold text-sm shadow-sm transition-colors cursor-pointer"
                >
                  {isLoggingIn ? 'Đang đăng nhập...' : 'Đăng nhập'}
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Lọc danh sách mục hiển thị trên thanh menu bên trái theo đúng vai trò nhận từ JWT Backend
  const visibleNavItems = NAV_ITEMS.filter((item) =>
    item.allowedRoles.includes(currentStaff.role)
  );

  // Chốt bảo vệ URL tầng Route: nếu nhân sự cố tình gõ đường dẫn không thuộc quyền (vd: CSKH gõ /reports), giao diện sẽ hiện cảnh báo từ chối
  const currentNavRule = NAV_ITEMS.find((item) =>
    item.end
      ? location.pathname === item.to
      : location.pathname.startsWith(item.to)
  );
  const isAuthorizedRoute =
    !currentNavRule || currentNavRule.allowedRoles.includes(currentStaff.role);

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
                    Vai trò: {currentStaff.role}
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

          {/* Navigation Menu (Filtered by Role) */}
          <nav className="p-3 space-y-1">
            {visibleNavItems.map((item) => {
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

        {/* Sidebar Footer: Warehouse Alert + Logout Button */}
        <div className="p-3 space-y-2 border-t border-slate-800">
          {!collapsed &&
            lowStockBooks.length > 0 &&
            (currentStaff.role === 'ADMIN' ||
              currentStaff.role === 'WAREHOUSE_MANAGER') && (
              <div className="p-3 rounded-xl bg-amber-950/60 border border-amber-700/50 text-amber-200 text-xs space-y-1.5">
                <div className="flex items-center gap-1.5 font-bold text-amber-300">
                  <AlertTriangle className="w-4 h-4 shrink-0" />
                  <span>Cảnh báo tồn kho ({lowStockBooks.length})</span>
                </div>
                <p className="text-[11px] text-amber-200/90 leading-relaxed">
                  Có {lowStockBooks.length} đầu sách dưới định mức{' '}
                  {db.storeConfig.lowStockThreshold} cuốn cần nhập bổ sung.
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

          <button
            type="button"
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-2.5 px-3 py-2.5 rounded-lg bg-rose-600/15 hover:bg-rose-600 text-rose-300 hover:text-white border border-rose-500/30 text-xs font-bold transition-colors cursor-pointer"
            title="Đăng xuất khỏi Web Admin"
          >
            <LogOut className="w-4 h-4 shrink-0" />
            {!collapsed && <span>Đăng xuất tài khoản</span>}
          </button>
        </div>
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
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-700">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
              {currentStaff.roleLabel}
            </span>
          </div>

          <div className="flex items-center gap-3">
            {currentStaff.role === 'ADMIN' && (
              <button
                type="button"
                onClick={() => adminStatsService.exportRevenueExcelCsv()}
                className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg border border-slate-300 bg-white hover:bg-slate-50 text-xs font-semibold text-slate-700 cursor-pointer transition-colors"
              >
                <Download className="w-4 h-4 text-emerald-700" />
                Xuất báo cáo Excel (CSV)
              </button>
            )}

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

            {/* Admin Profile Badge + Logout Action */}
            <div className="flex items-center gap-3 pl-3 border-l border-slate-200">
              <div className="w-9 h-9 rounded-full bg-slate-900 text-white text-xs font-bold flex items-center justify-center">
                {currentStaff.avatarCode}
              </div>
              <div className="hidden sm:block text-left">
                <div className="text-xs font-bold text-slate-900">
                  {currentStaff.fullName}
                </div>
                <div className="text-[11px] text-emerald-700 font-semibold">
                  {currentStaff.email}
                </div>
              </div>
              <button
                type="button"
                onClick={handleLogout}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-rose-200 bg-rose-50 hover:bg-rose-600 text-rose-700 hover:text-white text-xs font-bold transition-colors cursor-pointer"
                title="Đăng xuất tài khoản"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>Đăng xuất</span>
              </button>
            </div>
          </div>
        </header>

        {/* SCROLLABLE PAGE CONTENT */}
        <main className="flex-1 p-6 overflow-y-auto max-w-[1600px] w-full mx-auto">
          {isAuthorizedRoute ? (
            <Outlet context={{ currentStaff } satisfies AdminOutletContext} />
          ) : (
            <div className="bg-white rounded-2xl border border-rose-200 p-8 text-center max-w-lg mx-auto mt-12 space-y-3">
              <ShieldAlert className="w-12 h-12 text-rose-600 mx-auto" />
              <h2 className="text-lg font-bold text-slate-900">
                Vai trò {currentStaff.role} không có quyền truy cập trang này
              </h2>
              <p className="text-xs text-slate-600 leading-relaxed">
                Theo quy định tại tài liệu Phân quyền Web Admin, tài khoản{' '}
                <b>{currentStaff.fullName}</b> ({currentStaff.roleLabel}) không được
                phép sử dụng chức năng này.
              </p>
              <button
                type="button"
                onClick={() => navigate('/')}
                className="px-4 py-2 rounded-lg bg-emerald-600 text-white text-xs font-bold cursor-pointer"
              >
                Quay về Tổng quan
              </button>
            </div>
          )}
        </main>
      </div>
    </div>
  );
};
