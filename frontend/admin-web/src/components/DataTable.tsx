import React from 'react';
import { AlertCircle, ChevronLeft, ChevronRight, Inbox, RefreshCw } from 'lucide-react';

export interface ColumnDef<T> {
  key: string;
  header: string;
  className?: string;
  render: (row: T) => React.ReactNode;
}

interface DataTableProps<T> {
  columns: ColumnDef<T>[];
  data: T[];
  rowKey: (row: T) => string;
  isLoading?: boolean;
  error?: string | null;
  onRetry?: () => void;
  emptyTitle?: string;
  emptyDescription?: string;
  currentPage?: number;
  totalPages?: number;
  onPageChange?: (page: number) => void;
}

export function DataTable<T>({
  columns,
  data,
  rowKey,
  isLoading = false,
  error = null,
  onRetry,
  emptyTitle = 'Không có dữ liệu phù hợp',
  emptyDescription = 'Hãy thử thay đổi bộ lọc tìm kiếm hoặc thêm bản ghi mới.',
  currentPage = 1,
  totalPages = 1,
  onPageChange,
}: DataTableProps<T>) {
  // 1. LOADING STATE (SKELETON PLACEHOLDERS)
  if (isLoading) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <div className="p-4 space-y-3">
          {Array.from({ length: 5 }).map((_, idx) => (
            <div
              key={idx}
              className="h-12 bg-slate-100 animate-pulse rounded-lg"
            />
          ))}
        </div>
      </div>
    );
  }

  // 2. ERROR STATE WITH RETRY BUTTON
  if (error) {
    return (
      <div className="bg-white rounded-xl border border-rose-200 p-10 text-center">
        <div className="w-12 h-12 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center mx-auto mb-3">
          <AlertCircle className="w-6 h-6" />
        </div>
        <h4 className="text-base font-bold text-slate-900">
          Không thể tải bảng dữ liệu
        </h4>
        <p className="text-sm text-slate-600 mt-1 mb-4">{error}</p>
        {onRetry && (
          <button
            type="button"
            onClick={onRetry}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-rose-600 text-white text-sm font-semibold hover:bg-rose-700 cursor-pointer"
          >
            <RefreshCw className="w-4 h-4" />
            Thử lại
          </button>
        )}
      </div>
    );
  }

  // 3. EMPTY STATE
  if (data.length === 0) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
        <div className="w-12 h-12 rounded-full bg-emerald-50 text-emerald-700 flex items-center justify-center mx-auto mb-3">
          <Inbox className="w-6 h-6" />
        </div>
        <h4 className="text-base font-bold text-slate-900">{emptyTitle}</h4>
        <p className="text-sm text-slate-500 mt-1">{emptyDescription}</p>
      </div>
    );
  }

  // 4. DATA CONTENT WITH INTERNAL HORIZONTAL SCROLL (NEVER OVERFLOWS VIEWPORT)
  return (
    <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse min-w-[780px]">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200">
              {columns.map((col) => (
                <th
                  key={col.key}
                  className={`py-3.5 px-4 text-xs font-bold uppercase tracking-wider text-slate-600 ${
                    col.className || ''
                  }`}
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200 text-sm">
            {data.map((row) => (
              <tr
                key={rowKey(row)}
                className="hover:bg-slate-50/80 transition-colors duration-150"
              >
                {columns.map((col) => (
                  <td
                    key={col.key}
                    className={`py-3.5 px-4 align-middle text-slate-800 ${
                      col.className || ''
                    }`}
                  >
                    {col.render(row)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      <div className="px-4 py-3 bg-slate-50 border-t border-slate-200 flex items-center justify-between text-xs text-slate-600">
        <span>
          Hiển thị <strong>{data.length}</strong> bản ghi • Trang{' '}
          <strong>{currentPage}</strong> / <strong>{Math.max(1, totalPages)}</strong>
        </span>

        {onPageChange && totalPages > 1 && (
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              disabled={currentPage <= 1}
              onClick={() => onPageChange(currentPage - 1)}
              className="p-1.5 rounded-md border border-slate-300 bg-white disabled:opacity-40 cursor-pointer hover:bg-slate-100"
              aria-label="Trang trước"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              type="button"
              disabled={currentPage >= totalPages}
              onClick={() => onPageChange(currentPage + 1)}
              className="p-1.5 rounded-md border border-slate-300 bg-white disabled:opacity-40 cursor-pointer hover:bg-slate-100"
              aria-label="Trang sau"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

