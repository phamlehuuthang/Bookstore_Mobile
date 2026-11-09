import React, { useEffect, useState } from 'react';
import { Lock, Shield, Unlock, UserPlus } from 'lucide-react';
import { User, UserRole, formatCurrencyVND } from '@shared/types';
import { adminSystemService } from '@/services/adminSystemService';
import { ColumnDef, DataTable } from '@/components/DataTable';

const ROLE_LABELS: Record<UserRole, string> = {
  CUSTOMER: 'Khách hàng (Mobile App)',
  ADMIN: 'Quản trị viên (Full quyền)',
  WAREHOUSE_MANAGER: 'Quản lý Kho',
  CSKH: 'Chăm sóc Khách hàng (CSKH)',
};

export const CustomersStaff: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [tab, setTab] = useState<'CUSTOMERS' | 'STAFF'>('CUSTOMERS');

  // New Staff Form
  const [staffName, setStaffName] = useState('');
  const [staffEmail, setStaffEmail] = useState('');
  const [staffPhone, setStaffPhone] = useState('');
  const [staffRole, setStaffRole] = useState<UserRole>('WAREHOUSE_MANAGER');

  const loadUsers = async () => {
    const list = await adminSystemService.getUsers();
    setUsers(list);
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleToggleLock = async (userId: string) => {
    await adminSystemService.toggleUserLock(userId);
    await loadUsers();
  };

  const handleChangeRole = async (userId: string, role: UserRole) => {
    await adminSystemService.updateStaffRole(userId, role);
    await loadUsers();
  };

  const handleCreateStaff = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!staffName.trim() || !staffEmail.trim()) return;
    await adminSystemService.createStaffAccount({
      fullName: staffName.trim(),
      email: staffEmail.trim(),
      phone: staffPhone.trim() || '0909112233',
      role: staffRole,
    });
    setStaffName('');
    setStaffEmail('');
    setStaffPhone('');
    await loadUsers();
  };

  const customers = users.filter((u) => u.role === 'CUSTOMER');
  const staffMembers = users.filter((u) => u.role !== 'CUSTOMER');

  const columns: ColumnDef<User>[] = [
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
            onChange={(e) => handleChangeRole(u.id, e.target.value as UserRole)}
            className="px-2.5 py-1.5 rounded-lg border border-slate-300 text-xs font-bold bg-emerald-50 text-emerald-800"
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
    {
      key: 'actions',
      header: 'Khóa / Mở khóa',
      render: (u) => (
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
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">
          Quản lý Khách hàng & Phân quyền Nhân sự (RBAC)
        </h1>
        <p className="text-sm text-slate-500">
          Theo dõi tổng chi tiêu độc giả, khóa/mở tài khoản và phân quyền nhân viên vận hành
        </p>
      </div>

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
      </div>

      {tab === 'STAFF' && (
        <form
          onSubmit={handleCreateStaff}
          className="bg-white rounded-xl border border-slate-200 p-5 grid grid-cols-1 sm:grid-cols-5 gap-3 items-end"
        >
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Họ tên nhân viên
            </label>
            <input
              required
              type="text"
              value={staffName}
              onChange={(e) => setStaffName(e.target.value)}
              placeholder="Nguyễn Văn A"
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Email công vụ
            </label>
            <input
              required
              type="email"
              value={staffEmail}
              onChange={(e) => setStaffEmail(e.target.value)}
              placeholder="nhanvien@bookstore.vn"
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Số điện thoại
            </label>
            <input
              type="text"
              value={staffPhone}
              onChange={(e) => setStaffPhone(e.target.value)}
              placeholder="0909..."
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
            className="py-2 px-4 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-sm font-semibold flex items-center justify-center gap-1.5 cursor-pointer"
          >
            <UserPlus className="w-4 h-4" />
            Cấp tài khoản
          </button>
        </form>
      )}

      <DataTable
        columns={columns}
        data={tab === 'CUSTOMERS' ? customers : staffMembers}
        rowKey={(u) => u.id}
      />
    </div>
  );
};

