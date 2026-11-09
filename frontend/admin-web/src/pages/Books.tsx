import React, { useEffect, useState } from 'react';
import { Edit3, Eye, EyeOff, Plus, Trash2 } from 'lucide-react';
import { Book, Category, formatCurrencyVND } from '@shared/types';
import { adminBookService } from '@/services/adminBookService';
import { ColumnDef, DataTable } from '@/components/DataTable';
import { FilterBar } from '@/components/FilterBar';
import { ImageUploader } from '@/components/ImageUploader';

export const Books: React.FC = () => {
  const [books, setBooks] = useState<Book[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [search, setSearch] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [isLoading, setIsLoading] = useState(true);

  // Modal Create/Edit State
  const [editingBook, setEditingBook] = useState<Partial<Book> | null>(null);

  const loadBooks = async () => {
    setIsLoading(true);
    const [bList, cList] = await Promise.all([
      adminBookService.getAllBooks(search, selectedCategory),
      adminBookService.getCategories(),
    ]);
    setBooks(bList);
    setCategories(cList.filter((c) => c.parentId === null));
    setIsLoading(false);
  };

  useEffect(() => {
    loadBooks();
  }, [search, selectedCategory]);

  const handleSaveBook = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingBook?.title || !editingBook?.price) return;
    await adminBookService.saveBook({
      ...editingBook,
      title: editingBook.title,
      price: Number(editingBook.price),
    });
    setEditingBook(null);
    await loadBooks();
  };

  const handleToggleVisible = async (bookId: string) => {
    await adminBookService.toggleBookVisibility(bookId);
    await loadBooks();
  };

  const handleDelete = async (bookId: string) => {
    await adminBookService.deleteBook(bookId);
    await loadBooks();
  };

  const columns: ColumnDef<Book>[] = [
    {
      key: 'book',
      header: 'Thông tin Sách & ISBN',
      render: (b) => (
        <div className="flex items-center gap-3 max-w-md">
          <img
            src={b.coverUrl}
            alt={b.title}
            className="w-11 h-15 rounded object-cover border border-slate-200 shrink-0"
          />
          <div className="min-w-0">
            <div className="font-bold text-slate-900 line-clamp-1">{b.title}</div>
            <div className="text-xs text-slate-500">
              {b.authorName} • {b.publisherName}
            </div>
            <div className="text-[11px] font-mono text-slate-400 mt-0.5">
              ISBN: {b.isbn}
            </div>
          </div>
        </div>
      ),
    },
    {
      key: 'category',
      header: 'Danh mục',
      render: (b) => (
        <span className="px-2.5 py-1 rounded-md bg-slate-100 text-slate-700 text-xs font-semibold">
          {b.categoryName}
        </span>
      ),
    },
    {
      key: 'price',
      header: 'Giá bán / Giá gốc',
      render: (b) => (
        <div>
          <div className="font-bold text-emerald-700">
            {formatCurrencyVND(b.flashSalePrice ?? b.price)}
          </div>
          {b.originalPrice > (b.flashSalePrice ?? b.price) && (
            <div className="text-xs text-slate-400 line-through">
              {formatCurrencyVND(b.originalPrice)}
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'stock',
      header: 'Tồn kho',
      render: (b) => (
        <span
          className={`px-2.5 py-1 rounded-full text-xs font-bold ${
            b.stock < 5
              ? 'bg-rose-100 text-rose-700'
              : 'bg-emerald-50 text-emerald-700'
          }`}
        >
          {b.stock} cuốn {b.stock < 5 ? '⚠️' : ''}
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Hiển thị App',
      render: (b) => (
        <button
          type="button"
          onClick={() => handleToggleVisible(b.id)}
          className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold cursor-pointer ${
            b.isVisible
              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
              : 'bg-slate-100 text-slate-500 border border-slate-200'
          }`}
        >
          {b.isVisible ? (
            <>
              <Eye className="w-3.5 h-3.5" /> Đang bán
            </>
          ) : (
            <>
              <EyeOff className="w-3.5 h-3.5" /> Đang ẩn
            </>
          )}
        </button>
      ),
    },
    {
      key: 'actions',
      header: 'Thao tác',
      render: (b) => (
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setEditingBook({ ...b })}
            className="p-1.5 rounded-lg border border-slate-200 hover:bg-slate-100 text-slate-700 cursor-pointer"
            title="Chỉnh sửa sách"
          >
            <Edit3 className="w-4 h-4" />
          </button>
          <button
            type="button"
            onClick={() => handleDelete(b.id)}
            className="p-1.5 rounded-lg border border-rose-200 hover:bg-rose-50 text-rose-600 cursor-pointer"
            title="Xóa sách"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-900">
            Quản lý Kho Sách (CRUD, Giá bán & Hiển thị)
          </h1>
          <p className="text-sm text-slate-500">
            Thêm mới, cập nhật giá bán, ảnh bìa, ISBN và trạng thái hiển thị trên Mobile App
          </p>
        </div>
      </div>

      <FilterBar
        searchPlaceholder="Tìm theo tên sách, tác giả, mã ISBN..."
        searchValue={search}
        onSearchChange={setSearch}
        actions={
          <button
            type="button"
            onClick={() =>
              setEditingBook({
                title: '',
                isbn: '978-604-1-88999-1',
                authorName: 'Nguyễn Nhật Ánh',
                publisherName: 'NXB Trẻ',
                categoryId: categories[0]?.id || 'cat-tech',
                price: 145000,
                originalPrice: 180000,
                stock: 25,
                coverFormat: 'Bìa mềm',
                coverUrl:
                  'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=600&q=80',
                shortSummary: '',
                description: '',
                isVisible: true,
              })
            }
            className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-sm font-semibold cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            Thêm sách mới
          </button>
        }
      >
        <select
          value={selectedCategory}
          onChange={(e) => setSelectedCategory(e.target.value)}
          aria-label="Lọc theo danh mục"
          className="px-3 py-2 text-sm rounded-lg border border-slate-300 bg-white focus:outline-none focus:border-emerald-600"
        >
          <option value="ALL">Tất cả danh mục</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      </FilterBar>

      <DataTable
        columns={columns}
        data={books}
        rowKey={(b) => b.id}
        isLoading={isLoading}
      />

      {/* CREATE / EDIT BOOK MODAL */}
      {editingBook && (
        <div className="fixed inset-0 bg-slate-900/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 max-w-2xl w-full max-h-[90vh] overflow-y-auto p-6 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-200">
              <h3 className="text-lg font-bold text-slate-900">
                {editingBook.id ? 'Chỉnh sửa thông tin Sách' : 'Thêm đầu sách mới vào hệ thống'}
              </h3>
              <button
                type="button"
                onClick={() => setEditingBook(null)}
                className="text-slate-400 hover:text-slate-700 text-sm font-bold cursor-pointer"
              >
                Đóng ✕
              </button>
            </div>

            <form onSubmit={handleSaveBook} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="sm:col-span-2">
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Tên cuốn sách *
                  </label>
                  <input
                    required
                    type="text"
                    value={editingBook.title || ''}
                    onChange={(e) =>
                      setEditingBook({ ...editingBook, title: e.target.value })
                    }
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Mã ISBN
                  </label>
                  <input
                    type="text"
                    value={editingBook.isbn || ''}
                    onChange={(e) =>
                      setEditingBook({ ...editingBook, isbn: e.target.value })
                    }
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Danh mục sách
                  </label>
                  <select
                    value={editingBook.categoryId || 'cat-tech'}
                    onChange={(e) =>
                      setEditingBook({ ...editingBook, categoryId: e.target.value })
                    }
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 bg-white"
                  >
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Tác giả
                  </label>
                  <input
                    type="text"
                    value={editingBook.authorName || ''}
                    onChange={(e) =>
                      setEditingBook({ ...editingBook, authorName: e.target.value })
                    }
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Nhà xuất bản
                  </label>
                  <input
                    type="text"
                    value={editingBook.publisherName || ''}
                    onChange={(e) =>
                      setEditingBook({ ...editingBook, publisherName: e.target.value })
                    }
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Giá bán thực tế (VND) *
                  </label>
                  <input
                    required
                    type="number"
                    value={editingBook.price ?? 120000}
                    onChange={(e) =>
                      setEditingBook({ ...editingBook, price: Number(e.target.value) })
                    }
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Số lượng tồn kho
                  </label>
                  <input
                    type="number"
                    value={editingBook.stock ?? 20}
                    onChange={(e) =>
                      setEditingBook({ ...editingBook, stock: Number(e.target.value) })
                    }
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                  />
                </div>
              </div>

              <ImageUploader
                value={editingBook.coverUrl || ''}
                onChange={(url) => setEditingBook({ ...editingBook, coverUrl: url })}
              />

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Mô tả tóm tắt nội dung
                </label>
                <textarea
                  rows={3}
                  value={editingBook.description || ''}
                  onChange={(e) =>
                    setEditingBook({ ...editingBook, description: e.target.value })
                  }
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-200">
                <button
                  type="button"
                  onClick={() => setEditingBook(null)}
                  className="px-4 py-2 rounded-lg border border-slate-300 text-sm font-semibold text-slate-700 cursor-pointer"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-sm font-semibold cursor-pointer"
                >
                  Lưu thay đổi
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

