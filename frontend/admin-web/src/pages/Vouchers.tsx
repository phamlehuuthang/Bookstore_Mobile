import React, { useEffect, useState } from 'react';
import { BellRing, Eye, EyeOff, MessageSquareWarning, TicketPlus } from 'lucide-react';
import { DiscountType, Review, Voucher, formatCurrencyVND } from '@shared/types';
import { adminSystemService } from '@/services/adminSystemService';

export const Vouchers: React.FC = () => {
  const [vouchers, setVouchers] = useState<Voucher[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);

  // Voucher Form
  const [code, setCode] = useState('');
  const [title, setTitle] = useState('');
  const [discountType, setDiscountType] = useState<DiscountType>('FIXED');
  const [discountValue, setDiscountValue] = useState(30000);
  const [minOrderValue, setMinOrderValue] = useState(150000);
  const [usageLimit, setUsageLimit] = useState(200);
  const [endDate, setEndDate] = useState('2026-12-31');

  // Push Notification Broadcaster
  const [pushTitle, setPushTitle] = useState('');
  const [pushMsg, setPushMsg] = useState('');
  const [pushFeedback, setPushFeedback] = useState<string | null>(null);

  const loadAll = async () => {
    const [vList, rList] = await Promise.all([
      adminSystemService.getVouchers(),
      adminSystemService.getReviews(),
    ]);
    setVouchers(vList);
    setReviews(rList);
  };

  useEffect(() => {
    loadAll();
  }, []);

  const handleCreateVoucher = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!code.trim() || !title.trim()) return;
    await adminSystemService.saveVoucher({
      code: code.trim().toUpperCase(),
      title: title.trim(),
      discountType,
      discountValue: Number(discountValue),
      minOrderValue: Number(minOrderValue),
      usageLimit: Number(usageLimit),
      endDate,
    });
    setCode('');
    setTitle('');
    await loadAll();
  };

  const handleSendPush = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!pushTitle.trim() || !pushMsg.trim()) return;
    await adminSystemService.sendPushNotification(pushTitle.trim(), pushMsg.trim());
    setPushFeedback(`Đã phát thông báo đẩy "${pushTitle}" tới toàn bộ Mobile App khách hàng!`);
    setPushTitle('');
    setPushMsg('');
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">
          Marketing, Mã Giảm Giá (Voucher), Kiểm duyệt Đánh giá & Push Notification
        </h1>
        <p className="text-sm text-slate-500">
          Thiết lập chiến dịch khuyến mãi, lọc bình luận vi phạm và gửi thông báo đẩy
        </p>
      </div>

      {/* 1. VOUCHER MANAGEMENT */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <form
          onSubmit={handleCreateVoucher}
          className="bg-white rounded-xl border border-slate-200 p-5 space-y-3 h-fit"
        >
          <div className="flex items-center gap-2 font-bold text-slate-900">
            <TicketPlus className="w-5 h-5 text-emerald-700" />
            <span>Tạo Voucher Khuyến Mãi</span>
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Mã Voucher (In hoa) *
            </label>
            <input
              required
              type="text"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="VD: TRITHUC40K"
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 uppercase"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Tiêu đề chương trình *
            </label>
            <input
              required
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Giảm 40.000 đ cho đơn từ 250k"
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Loại giảm giá
              </label>
              <select
                value={discountType}
                onChange={(e) => setDiscountType(e.target.value as DiscountType)}
                className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 bg-white"
              >
                <option value="FIXED">Số tiền cố định (đ)</option>
                <option value="PERCENTAGE">Theo phần trăm (%)</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Mức giảm
              </label>
              <input
                required
                type="number"
                value={discountValue}
                onChange={(e) => setDiscountValue(Number(e.target.value))}
                className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Đơn tối thiểu (đ)
              </label>
              <input
                type="number"
                value={minOrderValue}
                onChange={(e) => setMinOrderValue(Number(e.target.value))}
                className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Giới hạn lượt dùng
              </label>
              <input
                type="number"
                value={usageLimit}
                onChange={(e) => setUsageLimit(Number(e.target.value))}
                className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Ngày hết hạn
            </label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <button
            type="submit"
            className="w-full py-2.5 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-sm font-semibold cursor-pointer"
          >
            Phát hành Voucher
          </button>
        </form>

        <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 p-5 space-y-3">
          <h3 className="text-base font-bold text-slate-900">
            Danh sách Mã Giảm Giá Đang Hoạt Động ({vouchers.length})
          </h3>
          <div className="space-y-3">
            {vouchers.map((v) => (
              <div
                key={v.id}
                className="p-4 rounded-xl border border-slate-200 flex items-center justify-between gap-4"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="px-2.5 py-1 rounded bg-emerald-700 text-white text-xs font-bold font-mono">
                      {v.code}
                    </span>
                    <span className="font-bold text-slate-900 text-sm">{v.title}</span>
                  </div>
                  <div className="text-xs text-slate-500">
                    Đơn tối thiểu: {formatCurrencyVND(v.minOrderValue)} • Đã dùng:{' '}
                    <strong>
                      {v.usedCount}/{v.usageLimit}
                    </strong>{' '}
                    • HSD: {v.endDate}
                  </div>
                </div>
                <button
                  type="button"
                  onClick={async () => {
                    await adminSystemService.toggleVoucherActive(v.id);
                    await loadAll();
                  }}
                  className={`px-3 py-1.5 rounded-lg text-xs font-bold cursor-pointer ${
                    v.isActive
                      ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                      : 'bg-slate-100 text-slate-500'
                  }`}
                >
                  {v.isActive ? 'Đang kích hoạt' : 'Đã tạm ngưng'}
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 2. REVIEW MODERATION & PUSH NOTIFICATION BROADCASTER */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Review Moderation */}
        <div className="bg-white rounded-xl border border-slate-200 p-5 space-y-3">
          <div className="flex items-center gap-2 font-bold text-slate-900">
            <MessageSquareWarning className="w-5 h-5 text-amber-600" />
            <span>Kiểm duyệt Đánh giá Sách ({reviews.length})</span>
          </div>
          <div className="space-y-3">
            {reviews.map((r) => (
              <div
                key={r.id}
                className={`p-3.5 rounded-xl border ${
                  r.isHidden ? 'bg-slate-100 border-slate-200 opacity-60' : 'bg-white border-slate-200'
                } flex items-start justify-between gap-3`}
              >
                <div className="space-y-1">
                  <div className="text-xs font-bold text-slate-900">
                    {r.userName} • <span className="text-amber-500">{'★'.repeat(r.rating)}</span>
                  </div>
                  <div className="text-xs text-emerald-700 font-medium">{r.bookTitle}</div>
                  <p className="text-xs text-slate-700">"{r.comment}"</p>
                </div>
                <button
                  type="button"
                  onClick={async () => {
                    await adminSystemService.toggleReviewHidden(r.id);
                    await loadAll();
                  }}
                  className="px-2.5 py-1.5 rounded-lg border border-slate-300 text-xs font-semibold flex items-center gap-1 shrink-0 cursor-pointer hover:bg-slate-50"
                >
                  {r.isHidden ? (
                    <>
                      <Eye className="w-3.5 h-3.5" /> Hiện lại
                    </>
                  ) : (
                    <>
                      <EyeOff className="w-3.5 h-3.5 text-rose-600" /> Ẩn vi phạm
                    </>
                  )}
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Push Notification Broadcaster */}
        <form
          onSubmit={handleSendPush}
          className="bg-white rounded-xl border border-slate-200 p-5 space-y-3 h-fit"
        >
          <div className="flex items-center gap-2 font-bold text-slate-900">
            <BellRing className="w-5 h-5 text-emerald-700" />
            <span>Phát Thông Báo Đẩy (Push Notification)</span>
          </div>
          <p className="text-xs text-slate-500">
            Gửi thông báo ưu đãi trực tiếp tới mục Thông báo trên Mobile App của toàn bộ khách hàng.
          </p>
          {pushFeedback && (
            <div className="p-3 rounded-lg bg-emerald-50 border border-emerald-200 text-xs font-semibold text-emerald-800">
              {pushFeedback}
            </div>
          )}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Tiêu đề thông báo *
            </label>
            <input
              required
              type="text"
              value={pushTitle}
              onChange={(e) => setPushTitle(e.target.value)}
              placeholder="🔥 Flash Sale Cuối Tuần Giảm Tới 40%!"
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Nội dung chi tiết *
            </label>
            <textarea
              required
              rows={3}
              value={pushMsg}
              onChange={(e) => setPushMsg(e.target.value)}
              placeholder="Nhập mã CHAOHE50K để được giảm ngay 50.000 đ và Freeship toàn quốc..."
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <button
            type="submit"
            className="w-full py-2.5 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-sm font-semibold cursor-pointer"
          >
            Phát sóng Thông báo ngay
          </button>
        </form>
      </div>
    </div>
  );
};

