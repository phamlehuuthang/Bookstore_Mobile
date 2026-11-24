import React, { useCallback, useEffect, useState } from 'react';
import {
  AlertCircle,
  BellRing,
  CheckCircle2,
  Edit3,
  Eye,
  EyeOff,
  MessageSquareWarning,
  TicketPlus,
} from 'lucide-react';
import { Category, DiscountType, Review, formatCurrencyVND } from '@shared/types';
import { useAdminStaff } from '@/layouts/AdminLayout';
import { adminBookService } from '@/services/adminBookService';
import { AdminVoucher, adminSystemService } from '@/services/adminSystemService';

export const Vouchers: React.FC = () => {
  const currentStaff = useAdminStaff();
  const isAdmin = currentStaff.role === 'ADMIN';

  const [vouchers, setVouchers] = useState<AdminVoucher[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);

  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Voucher Form (để trống mặc định, không điền sẵn dữ liệu mẫu)
  const [editingVoucherId, setEditingVoucherId] = useState<string | null>(null);
  const [code, setCode] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [discountType, setDiscountType] = useState<DiscountType>('FIXED');
  const [discountValue, setDiscountValue] = useState<string>('');
  const [maxDiscountAmount, setMaxDiscountAmount] = useState<string>('');
  const [minOrderValue, setMinOrderValue] = useState<string>('0');
  const [usageLimit, setUsageLimit] = useState<string>('');
  const [categoryId, setCategoryId] = useState<string>('');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');

  // Push Notification Broadcaster
  const [pushTitle, setPushTitle] = useState('');
  const [pushMsg, setPushMsg] = useState('');
  const [pushFeedback, setPushFeedback] = useState<string | null>(null);

  const loadAll = useCallback(async () => {
    setIsLoading(true);
    setErrorMsg(null);
    try {
      const activeParam =
        statusFilter === 'ALL' ? undefined : statusFilter === 'ACTIVE';
      const [vList, cList, rList] = await Promise.all([
        adminSystemService.getVouchers({ active: activeParam, keyword }),
        adminBookService.getCategories(),
        adminSystemService.getReviews(),
      ]);
      setVouchers(vList);
      setCategories(cList);
      setReviews(rList);
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Không thể tải danh sách mã giảm giá.'
      );
    } finally {
      setIsLoading(false);
    }
  }, [keyword, statusFilter]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const resetVoucherForm = () => {
    setEditingVoucherId(null);
    setCode('');
    setTitle('');
    setDescription('');
    setDiscountType('FIXED');
    setDiscountValue('');
    setMaxDiscountAmount('');
    setMinOrderValue('0');
    setUsageLimit('');
    setCategoryId('');
    setStartDate('');
    setEndDate('');
  };

  const handleStartEditVoucher = (v: AdminVoucher) => {
    if (!isAdmin) return;
    setErrorMsg(null);
    setSuccessMsg(null);
    setEditingVoucherId(v.id);
    setCode(v.code);
    setTitle(v.title);
    setDescription(v.description || '');
    setDiscountType(v.discountType);
    setDiscountValue(String(v.discountValue));
    setMaxDiscountAmount(
      v.maxDiscountAmount != null ? String(v.maxDiscountAmount) : ''
    );
    setMinOrderValue(String(v.minOrderValue ?? 0));
    setUsageLimit(String(v.usageLimit));
    setCategoryId(v.categoryId || '');
    setStartDate(v.startDate || '');
    setEndDate(v.endDate || '');
  };

  const handleSaveVoucher = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isAdmin || isSubmitting) return;
    setErrorMsg(null);
    setSuccessMsg(null);

    if (!code.trim() || !title.trim() || !discountValue || !usageLimit || !endDate) {
      setErrorMsg(
        'Vui lòng nhập đầy đủ mã voucher, tiêu đề, mức giảm, giới hạn lượt dùng và ngày hết hạn.'
      );
      return;
    }

    setIsSubmitting(true);
    try {
      const saved = await adminSystemService.saveVoucher({
        id: editingVoucherId || undefined,
        code: code.trim().toUpperCase(),
        title: title.trim(),
        description: description.trim() || undefined,
        discountType,
        discountValue: Number(discountValue),
        maxDiscountAmount:
          discountType === 'PERCENTAGE' && maxDiscountAmount.trim()
            ? Number(maxDiscountAmount)
            : undefined,
        minOrderValue: minOrderValue.trim() ? Number(minOrderValue) : 0,
        usageLimit: Number(usageLimit),
        categoryId: categoryId || null,
        startDate: startDate || undefined,
        endDate,
      });
      resetVoucherForm();
      setSuccessMsg(`Đã lưu mã giảm giá "${saved.code}" thành công.`);
      await loadAll();
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'Không thể lưu mã giảm giá.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleToggleVoucher = async (v: AdminVoucher) => {
    if (!isAdmin) return;
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const updated = await adminSystemService.toggleVoucherActive(v.id);
      setSuccessMsg(
        updated.isActive
          ? `Đã kích hoạt lại mã "${updated.code}".`
          : `Đã tạm ngưng mã "${updated.code}".`
      );
      await loadAll();
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Không thể cập nhật trạng thái voucher.'
      );
    }
  };

  const handleSendPush = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!pushTitle.trim() || !pushMsg.trim()) return;
    setErrorMsg(null);
    setPushFeedback(null);
    try {
      const result = await adminSystemService.sendPushNotification(
        pushTitle.trim(),
        pushMsg.trim()
      );
      setPushFeedback(
        `Đã phát sóng thông báo "${pushTitle.trim()}" tới ${result.recipients} khách hàng!`
      );
      setPushTitle('');
      setPushMsg('');
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Không thể phát sóng thông báo lúc này.'
      );
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">
          Marketing, Mã Giảm Giá (Voucher), Kiểm duyệt Đánh giá & Thông báo
        </h1>
        <p className="text-sm text-slate-500">
          {isAdmin
            ? 'Phát hành, chỉnh sửa, bật/tắt mã giảm giá, kiểm duyệt đánh giá và gửi thông báo khuyến mãi'
            : 'Tra cứu mã giảm giá đang hoạt động để tư vấn khách hàng, kiểm duyệt bình luận vi phạm và phát thông báo hỗ trợ'}
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

      {/* 1. VOUCHER MANAGEMENT */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {isAdmin && (
          <form
            onSubmit={handleSaveVoucher}
            className="bg-white rounded-xl border border-slate-200 p-5 space-y-3 h-fit"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 font-bold text-slate-900">
                <TicketPlus className="w-5 h-5 text-emerald-700" />
                <span>
                  {editingVoucherId ? 'Cập nhật Voucher' : 'Tạo Voucher Khuyến Mãi'}
                </span>
              </div>
              {editingVoucherId && (
                <button
                  type="button"
                  onClick={resetVoucherForm}
                  className="text-xs text-slate-500 hover:text-slate-800 cursor-pointer"
                >
                  Hủy sửa
                </button>
              )}
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
                placeholder="Nhập mã voucher"
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
                placeholder="Nhập tiêu đề khuyến mãi"
                className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Phạm vi danh mục áp dụng
              </label>
              <select
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 bg-white"
              >
                <option value="">Toàn bộ sách trong cửa hàng</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.parentId ? `↳ ${c.name}` : c.name}
                  </option>
                ))}
              </select>
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
                  Mức giảm *
                </label>
                <input
                  required
                  type="number"
                  min={1}
                  max={discountType === 'PERCENTAGE' ? 100 : undefined}
                  value={discountValue}
                  onChange={(e) => setDiscountValue(e.target.value)}
                  placeholder={discountType === 'PERCENTAGE' ? '1 - 100' : 'Số tiền (đ)'}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                />
              </div>
            </div>
            {discountType === 'PERCENTAGE' && (
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Giảm tối đa (đ)
                </label>
                <input
                  type="number"
                  min={0}
                  value={maxDiscountAmount}
                  onChange={(e) => setMaxDiscountAmount(e.target.value)}
                  placeholder="Không giới hạn nếu bỏ trống"
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                />
              </div>
            )}
            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Đơn tối thiểu (đ)
                </label>
                <input
                  type="number"
                  min={0}
                  value={minOrderValue}
                  onChange={(e) => setMinOrderValue(e.target.value)}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Giới hạn lượt dùng *
                </label>
                <input
                  required
                  type="number"
                  min={1}
                  value={usageLimit}
                  onChange={(e) => setUsageLimit(e.target.value)}
                  placeholder="Nhập số lượt"
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Ngày bắt đầu
                </label>
                <input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Ngày hết hạn *
                </label>
                <input
                  required
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                />
              </div>
            </div>
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-2.5 rounded-lg bg-emerald-700 hover:bg-emerald-800 disabled:opacity-60 text-white text-sm font-semibold cursor-pointer"
            >
              {isSubmitting
                ? 'Đang lưu...'
                : editingVoucherId
                  ? 'Cập nhật Voucher'
                  : 'Phát hành Voucher'}
            </button>
          </form>
        )}

        <div
          className={`${
            isAdmin ? 'lg:col-span-2' : 'lg:col-span-3'
          } bg-white rounded-xl border border-slate-200 p-5 space-y-4`}
        >
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h3 className="text-base font-bold text-slate-900">
              Danh sách Mã Giảm Giá ({vouchers.length})
            </h3>
            <div className="flex flex-wrap items-center gap-2">
              <input
                type="text"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="Tìm mã hoặc tiêu đề..."
                className="px-3 py-1.5 text-xs rounded-lg border border-slate-300"
              />
              <select
                value={statusFilter}
                onChange={(e) =>
                  setStatusFilter(e.target.value as 'ALL' | 'ACTIVE' | 'INACTIVE')
                }
                className="px-3 py-1.5 text-xs rounded-lg border border-slate-300 bg-white"
              >
                <option value="ALL">Tất cả trạng thái</option>
                <option value="ACTIVE">Đang kích hoạt</option>
                <option value="INACTIVE">Đã tạm ngưng</option>
              </select>
            </div>
          </div>

          {isLoading ? (
            <div className="space-y-3">
              {Array.from({ length: 3 }).map((_, idx) => (
                <div
                  key={idx}
                  className="h-20 bg-slate-100 animate-pulse rounded-xl"
                />
              ))}
            </div>
          ) : vouchers.length === 0 ? (
            <div className="p-8 text-center text-xs text-slate-500 border border-dashed border-slate-200 rounded-xl">
              Chưa có mã giảm giá nào phù hợp bộ lọc.
            </div>
          ) : (
            <div className="space-y-3">
              {vouchers.map((v) => (
                <div
                  key={v.id}
                  className="p-4 rounded-xl border border-slate-200 flex items-center justify-between gap-4"
                >
                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="px-2.5 py-1 rounded bg-emerald-700 text-white text-xs font-bold font-mono">
                        {v.code}
                      </span>
                      <span className="font-bold text-slate-900 text-sm">
                        {v.title}
                      </span>
                      {v.categoryName && (
                        <span className="px-2 py-0.5 rounded-full bg-amber-50 text-amber-800 border border-amber-200 text-[11px] font-semibold">
                          Chỉ {v.categoryName}
                        </span>
                      )}
                    </div>
                    <div className="text-xs text-slate-500">
                      Giảm:{' '}
                      <strong>
                        {v.discountType === 'PERCENTAGE'
                          ? `${v.discountValue}%${
                              v.maxDiscountAmount
                                ? ` (tối đa ${formatCurrencyVND(v.maxDiscountAmount)})`
                                : ''
                            }`
                          : formatCurrencyVND(v.discountValue)}
                      </strong>{' '}
                      • Đơn tối thiểu: {formatCurrencyVND(v.minOrderValue)} • Đã
                      dùng:{' '}
                      <strong>
                        {v.usedCount}/{v.usageLimit}
                      </strong>{' '}
                      • HSD: {v.endDate}
                    </div>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    {isAdmin && (
                      <button
                        type="button"
                        onClick={() => handleStartEditVoucher(v)}
                        className="p-1.5 rounded-lg border border-slate-200 hover:bg-slate-100 text-slate-700 cursor-pointer"
                        title="Chỉnh sửa voucher"
                      >
                        <Edit3 className="w-3.5 h-3.5" />
                      </button>
                    )}
                    {isAdmin ? (
                      <button
                        type="button"
                        onClick={() => handleToggleVoucher(v)}
                        className={`px-3 py-1.5 rounded-lg text-xs font-bold cursor-pointer ${
                          v.isActive
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-slate-100 text-slate-500'
                        }`}
                      >
                        {v.isActive ? 'Đang kích hoạt' : 'Đã tạm ngưng'}
                      </button>
                    ) : (
                      <span
                        className={`px-3 py-1.5 rounded-lg text-xs font-bold ${
                          v.isActive
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-slate-100 text-slate-500'
                        }`}
                      >
                        {v.isActive ? 'Đang kích hoạt' : 'Đã tạm ngưng'}
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
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
                  r.isHidden
                    ? 'bg-slate-100 border-slate-200 opacity-60'
                    : 'bg-white border-slate-200'
                } flex items-start justify-between gap-3`}
              >
                <div className="space-y-1">
                  <div className="text-xs font-bold text-slate-900">
                    {r.userName} •{' '}
                    <span className="text-amber-500">{'★'.repeat(r.rating)}</span>
                  </div>
                  <div className="text-xs text-emerald-700 font-medium">
                    {r.bookTitle}
                  </div>
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
            Gửi thông báo ưu đãi trực tiếp tới mục Thông báo trên Mobile App của
            toàn bộ khách hàng.
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
              placeholder="Nhập tiêu đề thông báo"
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
              placeholder="Nhập nội dung thông báo khuyến mãi hoặc hỗ trợ"
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
