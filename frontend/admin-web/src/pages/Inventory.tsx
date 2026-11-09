import React, { useEffect, useState } from 'react';
import { AlertTriangle, FilePlus2, PackagePlus } from 'lucide-react';
import { Book, InventoryReceipt, formatCurrencyVND } from '@shared/types';
import { adminBookService } from '@/services/adminBookService';
import { adminSystemService } from '@/services/adminSystemService';
import { ColumnDef, DataTable } from '@/components/DataTable';

export const Inventory: React.FC = () => {
  const [books, setBooks] = useState<Book[]>([]);
  const [receipts, setReceipts] = useState<InventoryReceipt[]>([]);
  const [onlyLowStock, setOnlyLowStock] = useState(false);

  // Import Receipt Form
  const [showImportModal, setShowImportModal] = useState(false);
  const [selectedBookId, setSelectedBookId] = useState('book-5');
  const [importQty, setImportQty] = useState(25);
  const [importPrice, setImportPrice] = useState(65000);
  const [supplierName, setSupplierName] = useState('Nhà Phát Hành FAHASA TP.HCM');
  const [note, setNote] = useState('Nhập bổ sung sách dưới định mức tồn kho an toàn');

  const loadData = async () => {
    const [bList, rList] = await Promise.all([
      adminBookService.getAllBooks(),
      adminSystemService.getInventoryReceipts(),
    ]);
    setBooks(bList);
    setReceipts(rList);
  };

  useEffect(() => {
    loadData();
  }, []);

  const lowStockBooks = books.filter((b) => b.stock < 5);
  const displayedBooks = onlyLowStock ? lowStockBooks : books;

  const handleCreateReceipt = async (e: React.FormEvent) => {
    e.preventDefault();
    const targetBook = books.find((b) => b.id === selectedBookId);
    if (!targetBook) return;

    await adminSystemService.createInventoryReceipt({
      supplierName,
      createdBy: 'Đỗ Thanh Tùng (Quản Lý Kho)',
      note,
      items: [
        {
          bookId: targetBook.id,
          bookTitle: targetBook.title,
          isbn: targetBook.isbn,
          quantity: Number(importQty),
          importPrice: Number(importPrice),
        },
      ],
    });
    setShowImportModal(false);
    await loadData();
  };

  const stockColumns: ColumnDef<Book>[] = [
    {
      key: 'book',
      header: 'Đầu sách & Mã ISBN',
      render: (b) => (
        <div>
          <div className="font-bold text-slate-900">{b.title}</div>
          <div className="text-xs text-slate-500">
            ISBN: {b.isbn} • {b.publisherName}
          </div>
        </div>
      ),
    },
    {
      key: 'sold',
      header: 'Tổng đã xuất bán',
      render: (b) => (
        <span className="font-semibold text-slate-700">{b.soldCount} cuốn</span>
      ),
    },
    {
      key: 'stock',
      header: 'Tồn kho thực tế',
      render: (b) => (
        <span
          className={`px-3 py-1 rounded-full text-xs font-bold ${
            b.stock < 5
              ? 'bg-rose-100 text-rose-700 border border-rose-300'
              : 'bg-emerald-50 text-emerald-700'
          }`}
        >
          {b.stock} cuốn {b.stock < 5 ? '• CẢNH BÁO SẮP HẾT' : '• An toàn'}
        </span>
      ),
    },
    {
      key: 'quickImport',
      header: 'Nhập kho nhanh',
      render: (b) => (
        <button
          type="button"
          onClick={() => {
            setSelectedBookId(b.id);
            setImportPrice(Math.round(b.price * 0.65));
            setShowImportModal(true);
          }}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-200 text-xs font-bold cursor-pointer"
        >
          <PackagePlus className="w-3.5 h-3.5" />
          + Nhập thêm hàng
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-slate-900">
            Quản lý Tồn Kho & Phiếu Nhập Kho
          </h1>
          <p className="text-sm text-slate-500">
            Giám sát định mức tồn kho tự động (&lt; 5 cuốn) và lập chứng từ nhập hàng
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowImportModal(true)}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-sm font-semibold cursor-pointer"
        >
          <FilePlus2 className="w-4 h-4" />
          Tạo Phiếu Nhập Kho Mới
        </button>
      </div>

      {/* AUTOMATED LOW STOCK WARNING BANNER */}
      {lowStockBooks.length > 0 && (
        <div className="p-4 rounded-xl bg-amber-50 border border-amber-300 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-amber-100 text-amber-700 flex items-center justify-center shrink-0">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <div className="text-sm font-bold text-amber-900">
                Cảnh báo tự động: Phát hiện {lowStockBooks.length} đầu sách sắp hết hàng (&lt; 5 cuốn)!
              </div>
              <div className="text-xs text-amber-800">
                Các đầu sách: {lowStockBooks.map((b) => `${b.title} (còn ${b.stock})`).join(' • ')}
              </div>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setOnlyLowStock(!onlyLowStock)}
            className="px-3.5 py-2 rounded-lg bg-amber-700 hover:bg-amber-800 text-white text-xs font-bold shrink-0 cursor-pointer"
          >
            {onlyLowStock ? 'Hiển thị toàn bộ kho' : 'Chỉ lọc sách sắp hết (< 5)'}
          </button>
        </div>
      )}

      <DataTable
        columns={stockColumns}
        data={displayedBooks}
        rowKey={(b) => b.id}
      />

      {/* LỊCH SỬ PHIẾU NHẬP KHO */}
      <div className="bg-white rounded-xl border border-slate-200 p-5 space-y-3">
        <h3 className="text-base font-bold text-slate-900">
          Lịch sử Phiếu Nhập Kho ({receipts.length})
        </h3>
        <div className="space-y-3">
          {receipts.map((r) => (
            <div
              key={r.id}
              className="p-4 rounded-xl border border-slate-200 bg-slate-50 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
            >
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="font-bold text-slate-900">{r.receiptCode}</span>
                  <span className="text-xs text-slate-500">• Nhà cung cấp: {r.supplierName}</span>
                </div>
                <div className="text-xs text-slate-600">
                  Người lập: <strong>{r.createdBy}</strong> • Ghi chú: {r.note}
                </div>
                <div className="text-xs text-emerald-700 font-semibold">
                  Chi tiết:{' '}
                  {r.items
                    .map((i) => `${i.bookTitle} (+${i.quantity} cuốn)`)
                    .join(', ')}
                </div>
              </div>
              <div className="text-right shrink-0">
                <div className="text-sm font-bold text-slate-900">
                  Tổng giá trị nhập: {formatCurrencyVND(r.totalCost)}
                </div>
                <div className="text-xs text-slate-500">
                  Tổng số lượng: +{r.totalQuantity} cuốn
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* MODAL TẠO PHIẾU NHẬP KHO */}
      {showImportModal && (
        <div className="fixed inset-0 bg-slate-900/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 max-w-lg w-full p-6 space-y-4">
            <h3 className="text-lg font-bold text-slate-900">
              Lập Phiếu Nhập Kho Hàng Hóa
            </h3>
            <form onSubmit={handleCreateReceipt} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Chọn đầu sách cần nhập kho
                </label>
                <select
                  value={selectedBookId}
                  onChange={(e) => setSelectedBookId(e.target.value)}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 bg-white"
                >
                  {books.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.title} (Tồn hiện tại: {b.stock} cuốn)
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Số lượng nhập thêm
                  </label>
                  <input
                    required
                    type="number"
                    min={1}
                    value={importQty}
                    onChange={(e) => setImportQty(Number(e.target.value))}
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Đơn giá nhập (VND)
                  </label>
                  <input
                    required
                    type="number"
                    min={1000}
                    value={importPrice}
                    onChange={(e) => setImportPrice(Number(e.target.value))}
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                  />
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Nhà cung cấp / Nhà phát hành
                </label>
                <input
                  required
                  type="text"
                  value={supplierName}
                  onChange={(e) => setSupplierName(e.target.value)}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Ghi chú chứng từ
                </label>
                <input
                  type="text"
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                />
              </div>
              <div className="flex justify-end gap-2 pt-3 border-t border-slate-200">
                <button
                  type="button"
                  onClick={() => setShowImportModal(false)}
                  className="px-4 py-2 rounded-lg border border-slate-300 text-xs font-semibold text-slate-700 cursor-pointer"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold cursor-pointer"
                >
                  Xác nhận Nhập Kho (+{importQty} cuốn)
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

