import React, { useEffect, useState } from 'react';
import { CheckCircle2, Download, Settings, Store } from 'lucide-react';
import { StoreConfig } from '@shared/types';
import { adminSystemService } from '@/services/adminSystemService';
import { adminStatsService } from '@/services/adminStatsService';

export const ReportsSettings: React.FC = () => {
  const [config, setConfig] = useState<StoreConfig | null>(null);
  const [savedBanner, setSavedBanner] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    adminSystemService
      .getStoreConfig()
      .then(setConfig)
      .catch((err) =>
        setSaveError(err instanceof Error ? err.message : 'Không thể tải cấu hình cửa hàng.')
      );
  }, []);

  const handleSaveConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!config || isSaving) return;
    setSaveError(null);
    setIsSaving(true);
    try {
      const updated = await adminSystemService.updateStoreConfig(config);
      setConfig(updated);
      setSavedBanner(true);
      setTimeout(() => setSavedBanner(false), 3000);
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : 'Lưu cấu hình thất bại.');
    } finally {
      setIsSaving(false);
    }
  };

  const [exportError, setExportError] = useState<string | null>(null);
  const [exportingKey, setExportingKey] = useState<'revenue' | 'orders' | 'inventory' | null>(
    null
  );

  const handleExportCsv = async (type: 'revenue' | 'orders' | 'inventory') => {
    if (exportingKey) return;
    setExportError(null);
    setExportingKey(type);
    try {
      if (type === 'revenue') {
        await adminStatsService.exportRevenueExcelCsv();
      } else if (type === 'orders') {
        await adminStatsService.exportOrdersExcelCsv();
      } else {
        await adminStatsService.exportInventoryExcelCsv();
      }
    } catch (err) {
      setExportError(
        err instanceof Error ? err.message : 'Không thể tải file báo cáo.'
      );
    } finally {
      setExportingKey(null);
    }
  };

  if (!config) {
    return saveError ? (
      <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-sm font-semibold">
        {saveError}
      </div>
    ) : null;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-slate-900">
            Xuất Báo Cáo Excel & Cấu Hình Cửa Hàng
          </h1>
          <p className="text-sm text-slate-500">
            Trích xuất 3 loại báo cáo CSV/Excel từ PostgreSQL và thiết lập thông tin thương hiệu, VietQR
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            disabled={exportingKey !== null}
            onClick={() => handleExportCsv('revenue')}
            className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg bg-emerald-700 hover:bg-emerald-800 disabled:opacity-60 text-white text-xs font-semibold cursor-pointer"
          >
            <Download className="w-4 h-4" />
            {exportingKey === 'revenue' ? 'Đang xuất...' : 'BC Doanh Thu (.csv)'}
          </button>
          <button
            type="button"
            disabled={exportingKey !== null}
            onClick={() => handleExportCsv('orders')}
            className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg bg-slate-800 hover:bg-slate-900 disabled:opacity-60 text-white text-xs font-semibold cursor-pointer"
          >
            <Download className="w-4 h-4" />
            {exportingKey === 'orders' ? 'Đang xuất...' : 'BC Đơn Hàng (.csv)'}
          </button>
          <button
            type="button"
            disabled={exportingKey !== null}
            onClick={() => handleExportCsv('inventory')}
            className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 disabled:opacity-60 text-white text-xs font-semibold cursor-pointer"
          >
            <Download className="w-4 h-4" />
            {exportingKey === 'inventory' ? 'Đang xuất...' : 'BC Tồn Kho (.csv)'}
          </button>
        </div>
      </div>

      {(exportError || saveError) && (
        <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-semibold">
          {exportError || saveError}
        </div>
      )}

      {savedBanner && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center gap-2 text-sm font-semibold text-emerald-800">
          <CheckCircle2 className="w-5 h-5 text-emerald-600" />
          Đã lưu cấu hình hệ thống cửa hàng thành công!
        </div>
      )}

      <form
        onSubmit={handleSaveConfig}
        className="bg-white rounded-xl border border-slate-200 p-6 space-y-6"
      >
        <div className="flex items-center gap-2 font-bold text-slate-900 pb-3 border-b border-slate-200">
          <Store className="w-5 h-5 text-emerald-700" />
          <span>1. Thông tin Thương hiệu & Liên hệ</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Tên cửa hàng
            </label>
            <input
              type="text"
              value={config.storeName}
              onChange={(e) => setConfig({ ...config, storeName: e.target.value })}
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Khẩu hiệu (Tagline)
            </label>
            <input
              type="text"
              value={config.tagline}
              onChange={(e) => setConfig({ ...config, tagline: e.target.value })}
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Hotline CSKH
            </label>
            <input
              type="text"
              value={config.hotline}
              onChange={(e) => setConfig({ ...config, hotline: e.target.value })}
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Email hỗ trợ
            </label>
            <input
              type="email"
              value={config.supportEmail}
              onChange={(e) => setConfig({ ...config, supportEmail: e.target.value })}
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div className="md:col-span-2">
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Địa chỉ trụ sở chính / Kho trung tâm
            </label>
            <input
              type="text"
              value={config.headquartersAddress}
              onChange={(e) =>
                setConfig({ ...config, headquartersAddress: e.target.value })
              }
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
        </div>

        <div className="flex items-center gap-2 font-bold text-slate-900 pb-3 border-b border-slate-200 pt-2">
          <Settings className="w-5 h-5 text-emerald-700" />
          <span>2. Cấu hình Vận chuyển, Kho & Tài khoản Ngân hàng Nhận tiền (VietQR)</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Phí vận chuyển mặc định (VND)
            </label>
            <input
              type="number"
              value={config.defaultShippingFee}
              onChange={(e) =>
                setConfig({ ...config, defaultShippingFee: Number(e.target.value) })
              }
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Ngưỡng miễn phí vận chuyển (Freeship VND)
            </label>
            <input
              type="number"
              value={config.freeShippingThreshold}
              onChange={(e) =>
                setConfig({ ...config, freeShippingThreshold: Number(e.target.value) })
              }
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Định mức cảnh báo sắp hết kho (cuốn)
            </label>
            <input
              type="number"
              value={config.lowStockThreshold}
              onChange={(e) =>
                setConfig({ ...config, lowStockThreshold: Number(e.target.value) })
              }
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Ngân hàng thụ hưởng
            </label>
            <input
              type="text"
              value={config.bankName}
              onChange={(e) => setConfig({ ...config, bankName: e.target.value })}
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Số tài khoản nhận thanh toán QR
            </label>
            <input
              type="text"
              value={config.bankAccountNumber}
              onChange={(e) =>
                setConfig({ ...config, bankAccountNumber: e.target.value })
              }
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Chủ tài khoản
            </label>
            <input
              type="text"
              value={config.bankAccountHolder}
              onChange={(e) =>
                setConfig({ ...config, bankAccountHolder: e.target.value })
              }
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
        </div>

        <div className="flex justify-end pt-3 border-t border-slate-200">
          <button
            type="submit"
            className="px-6 py-2.5 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-sm font-bold cursor-pointer"
          >
            Lưu Cấu Hình Cửa Hàng
          </button>
        </div>
      </form>
    </div>
  );
};

