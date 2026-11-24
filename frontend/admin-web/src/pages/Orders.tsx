import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  AlertCircle,
  Ban,
  CheckCircle2,
  RefreshCw,
  RotateCcw,
  Truck,
  XCircle,
} from 'lucide-react';
import {
  ORDER_STATUS_LABELS,
  Order,
  OrderStatus,
  PAYMENT_METHOD_LABELS,
  PaymentStatus,
  UserRole,
  formatCurrencyVND,
} from '@shared/types';

const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  UNPAID: 'Chưa thanh toán',
  PAID: 'Đã thanh toán',
  REFUNDED: 'Đã hoàn tiền',
};
import { useAdminStaff } from '@/layouts/AdminLayout';
import { adminOrderService } from '@/services/adminOrderService';
import { ColumnDef, DataTable } from '@/components/DataTable';
import { FilterBar } from '@/components/FilterBar';

  interface NextActionRule {
    next: OrderStatus;
label: string;
allowedRoles: Exclude < UserRole, 'CUSTOMER' > [];
}

// Quy tắc chuyển trạng thái 1 chiều + RBAC khớp 100% với OrderService & PHAN_QUYEN_WEB_ADMIN.md
const NEXT_STATUS_MAP: Partial<Record<OrderStatus, NextActionRule>> = {
  PENDING: {
    next: 'PREPARING',
    label: 'Duyệt đơn & Đóng gói',
    allowedRoles: ['ADMIN', 'WAREHOUSE_MANAGER', 'CSKH'],
  },
  PREPARING: {
    next: 'SHIPPING',
    label: 'Bàn giao Vận chuyển',
    allowedRoles: ['ADMIN', 'WAREHOUSE_MANAGER'],
  },
  SHIPPING: {
    next: 'DELIVERED',
    label: 'Xác nhận Đã giao',
    allowedRoles: ['ADMIN', 'CSKH'],
  },
};

// Chu kỳ tự động polling ngầm danh sách đơn trên Web Admin (5 giây/lần)
const ORDER_POLL_INTERVAL_MS = 5000;

export const Orders: React.FC = () => {
  const currentStaff = useAdminStaff();
  const [orders, setOrders] = useState<Order[]>([]);
  const [statusFilter, setStatusFilter] = useState<'ALL' | OrderStatus | 'RETURNS'>('ALL');
  const [keyword, setKeyword] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isManualRefreshing, setIsManualRefreshing] = useState(false);
  const [busyOrderId, setBusyOrderId] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  // Cờ khóa tránh gọi chồng chéo khi lần polling/làm mới trước chưa xong
  const isFetchingRef = useRef(false);

  const [inspectingReturnOrder, setInspectingReturnOrder] = useState<Order | null>(null);
  const [adminNote, setAdminNote] = useState('Đồng ý đổi mới sách miễn phí qua bưu điện.');

  /**
   * Tải danh sách đơn hàng cho Web Admin:
   * - silent = false: lần mở trang hoặc đổi bộ lọc -> hiện skeleton tải dữ liệu.
   * - silent = true: chạy ngầm (polling 5s hoặc quay lại tab) -> cập nhật hoàn toàn âm thầm, không nhấp nháy UI.
   * - manual = true: chính nhân viên bấm nút Làm mới -> chỉ xoay nhẹ icon ở nút Làm mới.
   */
  const loadOrders = useCallback(
    async (silent = false, manual = false) => {
      if (isFetchingRef.current) return;
      isFetchingRef.current = true;

      if (!silent) {
        setIsLoading(true);
        setErrorMsg(null);
      } else if (manual) {
        setIsManualRefreshing(true);
        setErrorMsg(null);
      }

      try {
        const apiStatus =
          statusFilter === 'ALL' || statusFilter === 'RETURNS'
            ? undefined
            : statusFilter;
        const list = await adminOrderService.getAllOrders(apiStatus, keyword);
        setOrders(list);
      } catch (err) {
        if (!silent || manual) {
          setErrorMsg(
            err instanceof Error ? err.message : 'Không thể tải danh sách đơn hàng.'
          );
        }
      } finally {
        isFetchingRef.current = false;
        if (!silent) {
          setIsLoading(false);
        }
        if (manual) {
          setIsManualRefreshing(false);
        }
      }
    },
    [statusFilter, keyword]
  );

  useEffect(() => {
    loadOrders(false);
  }, [loadOrders]);

  // Kết hợp Mức 1 (quay lại tab trình duyệt) + Mức 2 (tự động polling ngầm mỗi 5 giây khi tab đang mở)
  useEffect(() => {
    const timer = setInterval(() => {
      if (document.visibilityState === 'visible') {
        loadOrders(true);
      }
    }, ORDER_POLL_INTERVAL_MS);

    const handleFocus = () => loadOrders(true);
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        loadOrders(true);
      }
    };

    window.addEventListener('focus', handleFocus);
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      clearInterval(timer);
      window.removeEventListener('focus', handleFocus);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [loadOrders]);

  const handleTransition = async (order: Order, nextStatus: OrderStatus) => {
    if (busyOrderId) return;
    setErrorMsg(null);
    setSuccessMsg(null);
    setBusyOrderId(order.id);
    try {
      const updated = await adminOrderService.updateStatus(order.id, nextStatus);
      setSuccessMsg(
        `Đã chuyển đơn ${updated.orderCode} sang "${ORDER_STATUS_LABELS[updated.status]}".`
      );
      await loadOrders();
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Không thể cập nhật trạng thái đơn hàng.'
      );
    } finally {
      setBusyOrderId(null);
    }
  };

  const handleResolveReturn = async (decision: 'APPROVED' | 'REJECTED') => {
    if (!inspectingReturnOrder) return;
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      await adminOrderService.handleReturnRequest(
        inspectingReturnOrder.id,
        decision,
        adminNote
      );
      setSuccessMsg(
        decision === 'APPROVED'
          ? `Đã phê duyệt đổi sách mới cho đơn ${inspectingReturnOrder.orderCode}.`
          : `Đã từ chối yêu cầu đổi trả của đơn ${inspectingReturnOrder.orderCode}.`
      );
      setInspectingReturnOrder(null);
      await loadOrders(true);
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Không thể xử lý yêu cầu đổi trả.'
      );
    }
  };

  const filtered =
    statusFilter === 'RETURNS'
      ? orders.filter((o) => Boolean(o.returnRequest))
      : orders;

  const canCancelByStaff = (order: Order): boolean =>
    (currentStaff.role === 'ADMIN' || currentStaff.role === 'CSKH') &&
    (order.status === 'PENDING' || order.status === 'PREPARING');

  const badgeColors: Record<OrderStatus, string> = {
    PENDING: 'bg-amber-100 text-amber-800',
    PREPARING: 'bg-blue-100 text-blue-800',
    SHIPPING: 'bg-cyan-100 text-cyan-800',
    DELIVERED: 'bg-emerald-100 text-emerald-800',
    CANCELLED: 'bg-rose-100 text-rose-800',
  };

  const columns: ColumnDef<Order>[] = [
    {
      key: 'code',
      header: 'Mã đơn & Thời gian',
      render: (o) => (
        <div>
          <div className="font-bold text-slate-900">{o.orderCode}</div>
          <div className="text-xs text-slate-500">
            {o.timeline[0]?.timestamp || o.createdAt.slice(0, 16).replace('T', ' ')}
          </div>
        </div>
      ),
    },
    {
      key: 'customer',
      header: 'Khách hàng & Địa chỉ giao',
      render: (o) => {
        const fullAddr = [
            o.shippingAddress.streetAddress,
            o.shippingAddress.ward,
            o.shippingAddress.district,
            o.shippingAddress.province,
            ]
            .filter(Boolean)
            .join(', ');
            return (
            <div className="max-w-xs">
              <div className="font-semibold text-slate-900">
                {o.customerName} • {o.customerPhone}
              </div>
              <div className="text-xs text-slate-500 truncate" title={fullAddr}>
                {fullAddr}
              </div>
            </div>
          );
      },
    },
          {
            key: 'items',
          header: 'Sản phẩm đặt',
      render: (o) => (
          <div className="space-y-1 max-w-xs">
            {o.items.map((item) => (
              <div key={item.bookId} className="text-xs text-slate-700 truncate">
                • {item.title} <strong>(x{item.quantity})</strong>
              </div>
            ))}
          </div>
          ),
    },
          {
            key: 'total',
          header: 'Tổng tiền & Thanh toán',
      render: (o) => (
          <div>
            <div className="font-bold text-emerald-700">
              {formatCurrencyVND(o.totalAmount)}
            </div>
            <div className="text-[11px] text-slate-500">
              {PAYMENT_METHOD_LABELS[o.paymentMethod]} •{' '}
              <span className="font-semibold text-slate-700">
                {PAYMENT_STATUS_LABELS[o.paymentStatus]}
              </span>
            </div>
          </div>
          ),
    },
          {
            key: 'status',
          header: 'Trạng thái Vận chuyển',
      render: (o) => (
          <div className="space-y-1">
            <span
              className={`inline-block px-2.5 py-1 rounded-full text-xs font-bold ${badgeColors[o.status]
                }`}
            >
              {ORDER_STATUS_LABELS[o.status]}
            </span>
            {o.returnRequest && (
              <div>
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-rose-50 text-rose-700 border border-rose-200 text-[11px] font-semibold">
                  <RotateCcw className="w-3 h-3" />
                  Khiếu nại Đổi trả ({o.returnRequest.status})
                </span>
              </div>
            )}
          </div>
          ),
    },
          {
            key: 'actions',
          header: 'Xử lý Nghiệp vụ',
      render: (o) => {
        const nextAction = NEXT_STATUS_MAP[o.status];
          const canRunNext =
          nextAction && nextAction.allowedRoles.includes(currentStaff.role);
          const allowCancel = canCancelByStaff(o);

          return (
          <div className="flex flex-wrap items-center gap-2">
              { canRunNext && nextAction && (
                <button
                  type="button"
                  disabled={busyOrderId === o.id}
                  onClick={() => handleTransition(o, nextAction.next)}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-700 hover:bg-emerald-800 disabled:opacity-60 text-white text-xs font-semibold cursor-pointer"
                >
                  <Truck className="w-3.5 h-3.5" />
                  {nextAction.label}
                </button>
              )}
            {allowCancel && (
              <button
                type="button"
                disabled={busyOrderId === o.id}
                onClick={() => handleTransition(o, 'CANCELLED')}
                className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg border border-rose-300 text-rose-700 hover:bg-rose-50 disabled:opacity-60 text-xs font-semibold cursor-pointer"
              >
                <Ban className="w-3.5 h-3.5" />
                Hủy đơn
              </button>
            )}
            {o.returnRequest && (currentStaff.role === 'ADMIN' || currentStaff.role === 'CSKH') && (
              <button
                type="button"
                onClick={() => {
                  setAdminNote(
                    o.returnRequest?.adminNote || 'Đồng ý đổi mới sách miễn phí qua bưu điện.'
                  );
                  setInspectingReturnOrder(o);
                }}
                className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-xs font-semibold cursor-pointer"
              >
                {o.returnRequest.status === 'PENDING'
                  ? 'Xem ảnh & Duyệt đổi trả'
                  : 'Xem kết quả đổi trả'}
              </button>
            )}
          </div>
          );
      },
    },
          ];

          return (
          <div className="space-y-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h1 className="text-xl font-bold text-slate-900">
                  Quản lý Đơn hàng & Xử lý Khiếu nại Đổi trả
                </h1>
                <p className="text-sm text-slate-500">
                  {currentStaff.role === 'ADMIN'
                    ? 'Toàn quyền duyệt đơn, đóng gói, xuất kho, xác nhận giao hàng và hủy đơn trước vận chuyển'
                    : currentStaff.role === 'WAREHOUSE_MANAGER'
                      ? 'Xác nhận đóng gói và bàn giao vận chuyển (Quản lý Kho không hủy đơn hoặc chốt đã giao)'
                      : 'Xác nhận đơn, hỗ trợ hủy đơn trước khi giao và chốt hoàn tất giao hàng'}
                </p>
              </div>

              <button
                type="button"
                onClick={() => loadOrders(true, true)}
                disabled={isManualRefreshing}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-300 bg-white hover:bg-slate-50 disabled:opacity-60 text-xs font-semibold text-slate-700 cursor-pointer"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isManualRefreshing ? 'animate-spin' : ''}`} />
                {isManualRefreshing ? 'Đang làm mới...' : 'Làm mới'}
              </button>
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

            <FilterBar
              searchPlaceholder="Tìm theo mã đơn hàng, tên khách hàng hoặc SĐT..."
              searchValue={keyword}
              onSearchChange={setKeyword}
            />

            {/* Filter Pills */}
            <div className="flex flex-wrap items-center gap-2">
              {(
                [
                  { key: 'ALL', label: `Tất cả${statusFilter === 'ALL' ? ` (${orders.length})` : ''}` },
                  { key: 'PENDING', label: 'Chờ duyệt' },
                  { key: 'PREPARING', label: 'Đang chuẩn bị' },
                  { key: 'SHIPPING', label: 'Đang giao' },
                  { key: 'DELIVERED', label: 'Đã giao' },
                  { key: 'CANCELLED', label: 'Đã hủy' },
                  { key: 'RETURNS', label: '🔄 Yêu cầu đổi trả' },
                ] as const
              ).map((tab) => (
                <button
                  key={tab.key}
                  type="button"
                  onClick={() => setStatusFilter(tab.key)}
                  className={`px-3.5 py-2 rounded-lg text-xs font-bold cursor-pointer transition-colors ${statusFilter === tab.key
                      ? 'bg-emerald-700 text-white'
                      : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-100'
                    }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            <DataTable
              columns={columns}
              data={filtered}
              rowKey={(o) => o.id}
              isLoading={isLoading}
              error={errorMsg}
              onRetry={() => loadOrders(false)}
            />

            {/* MODAL XỬ LÝ KHIẾU NẠI ĐỔI TRẢ (XEM ẢNH MINH CHỨNG CỦA KHÁCH) */}
            {inspectingReturnOrder && inspectingReturnOrder.returnRequest && (
              <div className="fixed inset-0 bg-slate-900/50 z-50 flex items-center justify-center p-4">
                <div className="bg-white rounded-2xl border border-slate-200 max-w-lg w-full p-6 space-y-4">
                  <div className="flex items-center justify-between pb-3 border-b border-slate-200">
                    <h3 className="text-base font-bold text-slate-900">
                      Xử lý Khiếu nại Đổi trả — Đơn {inspectingReturnOrder.orderCode}
                    </h3>
                    <button
                      type="button"
                      onClick={() => setInspectingReturnOrder(null)}
                      className="text-slate-400 hover:text-slate-700 text-sm font-bold cursor-pointer"
                    >
                      ✕
                    </button>
                  </div>

                  <div className="space-y-2 text-sm">
                    <div>
                      <span className="text-slate-500">Khách hàng: </span>
                      <strong className="text-slate-900">
                        {inspectingReturnOrder.customerName} ({inspectingReturnOrder.customerPhone})
                      </strong>
                    </div>
                    <div>
                      <span className="text-slate-500">Lý do khiếu nại: </span>
                      <strong className="text-rose-700">
                        {inspectingReturnOrder.returnRequest.reason}
                      </strong>
                    </div>
                    <p className="p-3 rounded-lg bg-slate-50 border border-slate-200 text-xs text-slate-700">
                      "{inspectingReturnOrder.returnRequest.description}"
                    </p>

                    <div>
                      <span className="block text-xs font-semibold text-slate-700 mb-1.5">
                        Hình ảnh minh chứng do khách hàng gửi:
                      </span>
                      <div className="flex gap-3">
                        {inspectingReturnOrder.returnRequest.evidenceImages.map((img, i) => (
                          <img
                            key={i}
                            src={img}
                            alt="Ảnh minh chứng đổi trả"
                            className="w-28 h-36 rounded-lg object-cover border border-slate-300"
                          />
                        ))}
                      </div>
                    </div>

                    <div>
                      <label className="block text-xs font-semibold text-slate-700 mb-1">
                        Ghi chú phản hồi của Admin gửi tới khách hàng:
                      </label>
                      <textarea
                        rows={2}
                        value={adminNote}
                        onChange={(e) => setAdminNote(e.target.value)}
                        className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                      />
                    </div>
                  </div>

                  <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-200">
                    {inspectingReturnOrder.returnRequest.status === 'PENDING' ? (
                      <>
                        <button
                          type="button"
                          onClick={() => handleResolveReturn('REJECTED')}
                          className="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg border border-rose-300 text-rose-700 hover:bg-rose-50 text-xs font-bold cursor-pointer"
                        >
                          <XCircle className="w-4 h-4" />
                          Từ chối yêu cầu
                        </button>
                        <button
                          type="button"
                          onClick={() => handleResolveReturn('APPROVED')}
                          className="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold cursor-pointer"
                        >
                          <CheckCircle2 className="w-4 h-4" />
                          Phê duyệt Đổi mới
                        </button>
                      </>
                    ) : (
                      <span className="text-xs font-bold text-slate-600">
                        Trạng thái đã xử lý:{' '}
                        {inspectingReturnOrder.returnRequest.status === 'APPROVED'
                          ? 'Đã chấp thuận đổi mới'
                          : 'Đã từ chối'}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
          );
};
