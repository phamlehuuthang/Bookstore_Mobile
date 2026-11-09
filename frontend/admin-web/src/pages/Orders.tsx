import React, { useEffect, useState } from 'react';
import { CheckCircle2, RotateCcw, Truck, XCircle } from 'lucide-react';
import {
  ORDER_STATUS_LABELS,
  Order,
  OrderStatus,
  PAYMENT_METHOD_LABELS,
  formatCurrencyVND,
} from '@shared/types';
import { adminOrderService } from '@/services/adminOrderService';
import { ColumnDef, DataTable } from '@/components/DataTable';

const NEXT_STATUS_MAP: Partial<Record<OrderStatus, { next: OrderStatus; label: string }>> = {
  PENDING: { next: 'PREPARING', label: 'Duyệt đơn & Đóng gói' },
  PREPARING: { next: 'SHIPPING', label: 'Bàn giao Vận chuyển' },
  SHIPPING: { next: 'DELIVERED', label: 'Xác nhận Đã giao' },
};

export const Orders: React.FC = () => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [statusFilter, setStatusFilter] = useState<'ALL' | OrderStatus | 'RETURNS'>('ALL');
  const [inspectingReturnOrder, setInspectingReturnOrder] = useState<Order | null>(null);
  const [adminNote, setAdminNote] = useState('Đồng ý đổi mới sách miễn phí qua bưu điện.');

  const loadOrders = async () => {
    const list = await adminOrderService.getAllOrders();
    setOrders(list);
  };

  useEffect(() => {
    loadOrders();
  }, []);

  const handleTransition = async (orderId: string, nextStatus: OrderStatus) => {
    await adminOrderService.updateStatus(orderId, nextStatus);
    await loadOrders();
  };

  const handleResolveReturn = async (decision: 'APPROVED' | 'REJECTED') => {
    if (!inspectingReturnOrder) return;
    await adminOrderService.handleReturnRequest(
      inspectingReturnOrder.id,
      decision,
      adminNote
    );
    setInspectingReturnOrder(null);
    await loadOrders();
  };

  const filtered = orders.filter((o) => {
    if (statusFilter === 'ALL') return true;
    if (statusFilter === 'RETURNS') return Boolean(o.returnRequest);
    return o.status === statusFilter;
  });

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
            {o.timeline[0]?.timestamp || o.createdAt.slice(0, 10)}
          </div>
        </div>
      ),
    },
    {
      key: 'customer',
      header: 'Khách hàng & Địa chỉ giao',
      render: (o) => (
        <div className="max-w-xs">
          <div className="font-semibold text-slate-900">
            {o.customerName} • {o.customerPhone}
          </div>
          <div className="text-xs text-slate-500 truncate">
            {o.shippingAddress.streetAddress}, {o.shippingAddress.district},{' '}
            {o.shippingAddress.province}
          </div>
        </div>
      ),
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
            {PAYMENT_METHOD_LABELS[o.paymentMethod]}
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
            className={`inline-block px-2.5 py-1 rounded-full text-xs font-bold ${
              badgeColors[o.status]
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
        return (
          <div className="flex flex-wrap items-center gap-2">
            {nextAction && (
              <button
                type="button"
                onClick={() => handleTransition(o.id, nextAction.next)}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-semibold cursor-pointer"
              >
                <Truck className="w-3.5 h-3.5" />
                {nextAction.label}
              </button>
            )}
            {o.returnRequest && (
              <button
                type="button"
                onClick={() => setInspectingReturnOrder(o)}
                className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-xs font-semibold cursor-pointer"
              >
                Xem ảnh & Duyệt đổi trả
              </button>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-xl font-bold text-slate-900">
          Quản lý Đơn hàng & Xử lý Khiếu nại Đổi trả
        </h1>
        <p className="text-sm text-slate-500">
          Duyệt đơn xuất kho, chuyển trạng thái giao nhận và kiểm tra minh chứng đổi trả hàng
        </p>
      </div>

      {/* Filter Pills */}
      <div className="flex flex-wrap items-center gap-2">
        {(
          [
            { key: 'ALL', label: `Tất cả (${orders.length})` },
            { key: 'PENDING', label: 'Chờ duyệt' },
            { key: 'PREPARING', label: 'Đang chuẩn bị' },
            { key: 'SHIPPING', label: 'Đang giao' },
            { key: 'DELIVERED', label: 'Đã giao' },
            { key: 'RETURNS', label: '🔄 Yêu cầu đổi trả' },
          ] as const
        ).map((tab) => (
          <button
            key={tab.key}
            type="button"
            onClick={() => setStatusFilter(tab.key)}
            className={`px-3.5 py-2 rounded-lg text-xs font-bold cursor-pointer transition-colors ${
              statusFilter === tab.key
                ? 'bg-emerald-700 text-white'
                : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-100'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <DataTable columns={columns} data={filtered} rowKey={(o) => o.id} />

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
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

