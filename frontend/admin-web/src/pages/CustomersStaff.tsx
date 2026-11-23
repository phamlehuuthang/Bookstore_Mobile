import React, { useCallback, useEffect, useState } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Lock,
  Shield,
  Unlock,
  UserPlus,
} from 'lucide-react';
import { User, UserRole, formatCurrencyVND } from '@shared/types';
import { useAdminStaff } from '@/layouts/AdminLayout';
import { adminSystemService } from '@/services/adminSystemService';
import { ColumnDef, DataTable } from '@/components/DataTable';
import { FilterBar } from '@/components/FilterBar';

const ROLE_LABELS: Record<UserRole, string> = {
  CUSTOMER: 'Khách hàng (Mobile App)',
  ADMIN: 'Quản trị viên (Full quyền)',
  WAREHOUSE_MANAGER: 'Quản lý Kho',
  CSKH: 'Chăm sóc Khách hàng (CSKH)',
};

export const CustomersStaff: React.FC = () => {
  const currentStaff = useAdminStaff();
  const isAdmin = currentStaff.role === 'ADMIN';

  const [customers, setCustomers] = useState<User[]>([]);
  const [staffMembers, setStaffMembers] = useState<User[]>([]);
  const [tab, setTab] = useState<'CUSTOMERS' | 'STAFF'>('CUSTOMERS');
  const [keyword, setKeyword] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Form cấp tài khoản nhân sự mới (để trống mặc định, không điền sẵn chữ mẫu)
  const [staffName, setStaffName] = useState('');
  const [staffEmail, setStaffEmail] = useState('');
  const [staffPhone, setStaffPhone] = useState('');
  const [staffPassword, setStaffPassword] = useState('');
  const [staffRole, setStaffRole] = useState<UserRole>('WAREHOUSE_MANAGER');

  // Nạp danh sách từ Backend theo đúng quyền RBAC (CSKH chỉ gọi API khách hàng)
  const loadUsers = useCallback(async () => {
    setIsLoading(true);
    setErrorMsg(null);
    try {
      const [customerList, staffList] = await Promise.all([
        adminSystemService.getCustomers(keyword),
        isAdmin ? adminSystemService.getStaff(keyword) : Promise.resolve([]),
      ]);
      setCustomers(customerList);
      setStaffMembers(staffList);
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Không thể tải danh sách tài khoản.'
      );
    } finally {
      setIsLoading(false);
    }
  }, [isAdmin, keyword]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  // Nếu vai trò không phải ADMIN (vd CSKH), luôn khóa ở tab Khách hàng
  useEffect(() => {
    if (!isAdmin && tab === 'STAFF') {
      setTab('CUSTOMERS');
    }
  }, [isAdmin, tab]);

  const handleToggleLock = async (userId: string) => {
    if (!isAdmin) return;
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const updated = await adminSystemService.toggleUserLock(userId);
      setSuccessMsg(
        updated.isLocked
          ? `Đã khóa tài khoản ${updated.fullName}.`
          : `Đã mở khóa tài khoản ${updated.fullName}.`
      );
      await loadUsers();
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Không thể cập nhật trạng thái khóa.'
      );
    }
  };

  const handleChangeRole = async (userId: string, role: UserRole) => {
    if (!isAdmin) return;
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const updated = await adminSystemService.updateStaffRole(userId, role);
      setSuccessMsg(
        `Đã chuyển vai trò của ${updated.fullName} sang ${ROLE_LABELS[updated.role]}.`
      );
      await loadUsers();
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Không thể thay đổi vai trò nhân sự.'
      );
    }
  };

  const handleCreateStaff = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isAdmin || isSubmitting) return;
    setErrorMsg(null);
    setSuccessMsg(null);

    if (!staffName.trim() || !staffEmail.trim() || !staffPhone.trim()) {
      setErrorMsg('Vui lòng nhập đầy đủ họ tên, email công vụ và số điện thoại.');
      return;
    }

    setIsSubmitting(true);
    try {
      const created = await adminSystemService.createStaffAccount({
        fullName: staffName.trim(),
        email: staffEmail.trim(),
        phone: staffPhone.trim(),
        role: staffRole,
        password: staffPassword.trim() || undefined,
      });
      setStaffName('');
      setStaffEmail('');
      setStaffPhone('');
      setStaffPassword('');
      setSuccessMsg(
        `Đã cấp tài khoản nhân sự cho ${created.fullName} (${created.email}).`
      );
      await loadUsers();
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Không thể tạo tài khoản nhân sự.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const baseColumns: ColumnDef<User>[] = [
    {
      key: 'name',
      header: 'Họ và tên / Email',
      render: (u) => (
        <div>
          <div className="font-bold text-slate-900">{u.fullName}</div>
          <div className="text-xs text-slate-500">
            {u.email} • {u.phone}
          </div>
        </div>
      ),
    },
    {
      key: 'role',
      header: 'Vai trò (RBAC)',
      render: (u) =>
        u.role === 'CUSTOMER' ? (
          <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-700">
            {ROLE_LABELS[u.role]}
          </span>
        ) : (
          <select
            value={u.role}
            disabled={!isAdmin || u.id === currentStaff.id}
            onChange={(e) => handleChangeRole(u.id, e.target.value as UserRole)}
            className="px-2.5 py-1.5 rounded-lg border border-slate-300 text-xs font-bold bg-emerald-50 text-emerald-800 disabled:opacity-60"
          >
            <option value="ADMIN">Admin (Toàn quyền)</option>
            <option value="WAREHOUSE_MANAGER">Quản lý Kho</option>
            <option value="CSKH">Nhân viên CSKH</option>
          </select>
        ),
    },
    {
      key: 'spend',
      header: 'Tổng chi tiêu / Đơn hàng',
      render: (u) =>
        u.role === 'CUSTOMER' ? (
          <div>
            <div className="font-bold text-emerald-700">
              {formatCurrencyVND(u.totalSpent)}
            </div>
            <div className="text-xs text-slate-500">{u.orderCount} đơn đã mua</div>
          </div>
        ) : (
          <span className="text-xs text-slate-400">Nhân sự nội bộ</span>
        ),
    },
    {
      key: 'status',
      header: 'Trạng thái Tài khoản',
      render: (u) => (
        <span
          className={`px-2.5 py-1 rounded-full text-xs font-bold ${
            u.isLocked
              ? 'bg-rose-100 text-rose-700'
              : 'bg-emerald-100 text-emerald-700'
          }`}
        >
          {u.isLocked ? 'Đang bị khóa' : 'Đang hoạt động'}
        </span>
      ),
    },
  ];

  // Chỉ vai trò ADMIN mới thấy cột & nút thao tác Khóa / Mở khóa tài khoản
  const columns: ColumnDef<User>[] = isAdmin
    ? [
        ...baseColumns,
        {
          key: 'actions',
          header: 'Khóa / Mở khóa',
          render: (u) =>
            u.id === currentStaff.id ? (
              <span className="text-xs text-slate-400 italic">Đang đăng nhập</span>
            ) : (
              <button
                type="button"
                onClick={() => handleToggleLock(u.id)}
                className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold cursor-pointer ${
                  u.isLocked
                    ? 'bg-emerald-700 text-white hover:bg-emerald-800'
                    : 'border border-rose-300 text-rose-700 hover:bg-rose-50'
                }`}
              >
                {u.isLocked ? (
                  <>
                    <Unlock className="w-3.5 h-3.5" /> Mở khóa
                  </>
                ) : (
                  <>
                    <Lock className="w-3.5 h-3.5" /> Khóa TK
                  </>
                )}
              </button>
            ),
        },
      ]
    : baseColumns;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">
          Quản lý Khách hàng & Phân quyền Nhân sự (RBAC)
        </h1>
        <p className="text-sm text-slate-500">
          {isAdmin
            ? 'Theo dõi tổng chi tiêu độc giả, khóa/mở tài khoản và cấp/phân quyền nhân viên vận hành'
            : 'Tra cứu thông tin khách hàng để hỗ trợ đơn hàng (Vai trò CSKH chỉ được phép xem)'}
        </p>
      </div>

      {errorMsg && (
        <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {successMsg && (
        <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs font-medium flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 shrink-0" />
          <span>{successMsg}</span>
        </div>
      )}

      <div className="flex gap-2">
        <button
          type="button"
          onClick={() => setTab('CUSTOMERS')}
          className={`px-4 py-2 rounded-lg text-xs font-bold cursor-pointer ${
            tab === 'CUSTOMERS'
              ? 'bg-emerald-700 text-white'
              : 'bg-white border border-slate-200 text-slate-700'
          }`}
        >
          Khách hàng ({customers.length})
        </button>
        {isAdmin && (
          <button
            type="button"
            onClick={() => setTab('STAFF')}
            className={`px-4 py-2 rounded-lg text-xs font-bold cursor-pointer inline-flex items-center gap-1.5 ${
              tab === 'STAFF'
                ? 'bg-emerald-700 text-white'
                : 'bg-white border border-slate-200 text-slate-700'
            }`}
          >
            <Shield className="w-3.5 h-3.5" />
            Nhân sự & Phân quyền ({staffMembers.length})
          </button>
        )}
      </div>

      <FilterBar
        searchPlaceholder="Tìm theo họ tên, email hoặc số điện thoại..."
        searchValue={keyword}
        onSearchChange={setKeyword}
      />

      {isAdmin && tab === 'STAFF' && (
        <form
          onSubmit={handleCreateStaff}
          className="bg-white rounded-xl border border-slate-200 p-5 grid grid-cols-1 sm:grid-cols-6 gap-3 items-end"
        >
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Họ tên nhân viên *
            </label>
            <input
              required
              type="text"
              value={staffName}
              onChange={(e) => setStaffName(e.target.value)}
              placeholder="Nhập họ tên"
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Email công vụ *
            </label>
            <input
              required
              type="email"
              value={staffEmail}
              onChange={(e) => setStaffEmail(e.target.value)}
              placeholder="Nhập email công vụ"
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Số điện thoại *
            </label>
            <input
              required
              type="text"
              value={staffPhone}
              onChange={(e) => setStaffPhone(e.target.value)}
              placeholder="Nhập số điện thoại"
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Mật khẩu khởi tạo
            </label>
            <input
              type="password"
              value={staffPassword}
              onChange={(e) => setStaffPassword(e.target.value)}
              placeholder="Để trống mặc định 123456"
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Vai trò phân quyền
            </label>
            <select
              value={staffRole}
              onChange={(e) => setStaffRole(e.target.value as UserRole)}
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 bg-white"
            >
              <option value="ADMIN">Admin</option>
              <option value="WAREHOUSE_MANAGER">Quản lý Kho</option>
              <option value="CSKH">Nhân viên CSKH</option>
            </select>
          </div>
          <button
            type="submit"
            disabled={isSubmitting}
            className="py-2 px-4 rounded-lg bg-emerald-700 hover:bg-emerald-800 disabled:opacity-60 text-white text-sm font-semibold flex items-center justify-center gap-1.5 cursor-pointer"
          >
            <UserPlus className="w-4 h-4" />
            {isSubmitting ? 'Đang tạo...' : 'Cấp tài khoản'}
          </button>
        </form>
      )}

      <DataTable
        columns={columns}
        data={tab === 'CUSTOMERS' ? customers : staffMembers}
        rowKey={(u) => u.id}
        isLoading={isLoading}
        error={errorMsg}
        onRetry={loadUsers}
      />
    </div>
  );
};
