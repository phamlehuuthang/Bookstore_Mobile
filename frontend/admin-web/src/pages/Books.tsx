import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Edit3,
  Eye,
  EyeOff,
  Plus,
  Trash2,
} from 'lucide-react';
import {
  Author,
  Book,
  Category,
  Publisher,
  formatCurrencyVND,
} from '@shared/types';
import { useAdminStaff } from '@/layouts/AdminLayout';
import { adminBookService } from '@/services/adminBookService';
import { ColumnDef, DataTable } from '@/components/DataTable';
import { FilterBar } from '@/components/FilterBar';
import { ImageUploader } from '@/components/ImageUploader';

const PAGE_SIZE = 20;

export const Books: React.FC = () => {
  const currentStaff = useAdminStaff();
  const isAdmin = currentStaff.role === 'ADMIN';
  const canEditBooks =
    currentStaff.role === 'ADMIN' || currentStaff.role === 'WAREHOUSE_MANAGER';

  const [books, setBooks] = useState<Book[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [authors, setAuthors] = useState<Author[]>([]);
  const [publishers, setPublishers] = useState<Publisher[]>([]);

  const [search, setSearch] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [visibilityFilter, setVisibilityFilter] = useState<'ALL' | 'VISIBLE' | 'HIDDEN'>('ALL');
  // Phân trang luôn bắt đầu từ trang 1 theo quy ước giao diện
  const [currentPage, setCurrentPage] = useState(1);

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Modal Create/Edit State
  const [editingBook, setEditingBook] = useState<Partial<Book> | null>(null);

  const loadBooks = useCallback(async () => {
    setIsLoading(true);
    setErrorMsg(null);
    try {
      const visibleParam =
        visibilityFilter === 'ALL'
          ? undefined
          : visibilityFilter === 'VISIBLE';
      const [bList, cList, aList, pList] = await Promise.all([
        adminBookService.getAllBooks(search, selectedCategory, visibleParam),
        adminBookService.getCategories(),
        adminBookService.getAuthors(),
        adminBookService.getPublishers(),
      ]);
      setBooks(bList);
      setCategories(cList);
      setAuthors(aList);
      setPublishers(pList);
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Không thể tải dữ liệu sách.'
      );
    } finally {
      setIsLoading(false);
    }
  }, [search, selectedCategory, visibilityFilter]);

  useEffect(() => {
    loadBooks();
  }, [loadBooks]);

  // Reset về trang 1 mỗi khi bộ lọc thay đổi
  useEffect(() => {
    setCurrentPage(1);
  }, [search, selectedCategory, visibilityFilter]);

  const totalPages = Math.max(1, Math.ceil(books.length / PAGE_SIZE));
  const pagedBooks = useMemo(() => {
    const safePage = Math.min(currentPage, totalPages);
    const start = (safePage - 1) * PAGE_SIZE;
    return books.slice(start, start + PAGE_SIZE);
  }, [books, currentPage, totalPages]);

  const handleOpenCreateModal = () => {
    if (!canEditBooks) return;
    setErrorMsg(null);
    setEditingBook({
      title: '',
      isbn: '',
      authorId: authors[0]?.id || '',
      authorName: authors[0]?.name || '',
      publisherId: publishers[0]?.id || '',
      publisherName: publishers[0]?.name || '',
      categoryId: categories[0]?.id || '',
      price: undefined,
      originalPrice: undefined,
      stock: 0,
      coverFormat: 'Bìa mềm',
      coverUrl: '',
      shortSummary: '',
      description: '',
      isFlashSale: false,
      flashSalePrice: undefined,
      isVisible: true,
    });
  };

  const handleSaveBook = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canEditBooks || isSaving || !editingBook) return;
    setErrorMsg(null);
    setSuccessMsg(null);

    const priceNum = Number(editingBook.price);
    if (!editingBook.title?.trim()) {
      setErrorMsg('Vui lòng nhập tên cuốn sách.');
      return;
    }
    if (!editingBook.categoryId) {
      setErrorMsg('Vui lòng chọn danh mục sách.');
      return;
    }
    if (!Number.isFinite(priceNum) || priceNum <= 0) {
      setErrorMsg('Vui lòng nhập giá bán hợp lệ (> 0).');
      return;
    }

    setIsSaving(true);
    try {
      const saved = await adminBookService.saveBook({
        ...editingBook,
        title: editingBook.title.trim(),
        price: priceNum,
        originalPrice: Number(editingBook.originalPrice || priceNum),
      });
      setEditingBook(null);
      setSuccessMsg(`Đã lưu thông tin sách "${saved.title}".`);
      await loadBooks();
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Không thể lưu thông tin sách.'
      );
    } finally {
      setIsSaving(false);
    }
  };

  const handleToggleVisible = async (bookId: string) => {
    if (!isAdmin) return;
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const updated = await adminBookService.toggleBookVisibility(bookId);
      setSuccessMsg(
        updated.isVisible
          ? `Đã bật hiển thị "${updated.title}" trên Mobile App.`
          : `Đã ẩn "${updated.title}" khỏi Mobile App.`
      );
      await loadBooks();
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Không thể thay đổi trạng thái hiển thị.'
      );
    }
  };

  const handleDelete = async (book: Book) => {
    if (!isAdmin) return;
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      await adminBookService.deleteBook(book.id);
      setSuccessMsg(`Đã xóa đầu sách "${book.title}".`);
      await loadBooks();
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'Không thể xóa đầu sách.');
    }
  };

  const isPriceLocked =
    currentStaff.role === 'WAREHOUSE_MANAGER' && Boolean(editingBook?.id);

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
      render: (b) => {
        const effectivePrice =
          b.isFlashSale && b.flashSalePrice && b.flashSalePrice > 0
            ? b.flashSalePrice
            : b.price;
        return (
          <div>
            <div className="font-bold text-emerald-700 flex items-center gap-1.5">
              <span>{formatCurrencyVND(effectivePrice)}</span>
              {b.isFlashSale && (
                <span className="px-1.5 py-0.5 rounded bg-amber-100 text-amber-800 text-[10px] font-bold">
                  FLASH
                </span>
              )}
            </div>
            {b.originalPrice > effectivePrice && (
              <div className="text-xs text-slate-400 line-through">
                {formatCurrencyVND(b.originalPrice)}
              </div>
            )}
          </div>
        );
      },
    },
    {
      key: 'stock',
      header: 'Tồn kho',
      render: (b) => (
        <span
          className={`px-2.5 py-1 rounded-full text-xs font-bold ${
            b.stock <= 5
              ? 'bg-rose-100 text-rose-700'
              : 'bg-emerald-50 text-emerald-700'
          }`}
        >
          {b.stock} cuốn {b.stock <= 5 ? '⚠️' : ''}
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Hiển thị App',
      render: (b) =>
        isAdmin ? (
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
        ) : (
          <span
            className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${
              b.isVisible
                ? 'bg-emerald-50 text-emerald-700'
                : 'bg-slate-100 text-slate-500'
            }`}
          >
            {b.isVisible ? 'Đang bán' : 'Đang ẩn'}
          </span>
        ),
    },
    ...(canEditBooks
      ? [
          {
            key: 'actions',
            header: 'Thao tác',
            render: (b: Book) => (
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => {
                    setErrorMsg(null);
                    setEditingBook({ ...b });
                  }}
                  className="p-1.5 rounded-lg border border-slate-200 hover:bg-slate-100 text-slate-700 cursor-pointer"
                  title="Chỉnh sửa sách"
                >
                  <Edit3 className="w-4 h-4" />
                </button>
                {isAdmin && (
                  <button
                    type="button"
                    onClick={() => handleDelete(b)}
                    className="p-1.5 rounded-lg border border-rose-200 hover:bg-rose-50 text-rose-600 cursor-pointer"
                    title="Xóa sách"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                )}
              </div>
            ),
          },
        ]
      : []),
  ];

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-900">
            Quản lý Kho Sách ({books.length} đầu sách)
          </h1>
          <p className="text-sm text-slate-500">
            {isAdmin
              ? 'Thêm mới, cập nhật thông tin, giá bán, Flash Sale, hiển thị App và xóa đầu sách'
              : currentStaff.role === 'WAREHOUSE_MANAGER'
                ? 'Thêm/cập nhật thông tin đầu sách và số lượng tồn kho (Quản lý Kho không chỉnh giá bán / Flash Sale)'
                : 'Tra cứu thông tin đầu sách, giá bán và tồn kho để tư vấn khách hàng (CSKH chỉ xem)'}
          </p>
        </div>
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
        searchPlaceholder="Tìm theo tên sách, tác giả, nhà xuất bản, mã ISBN..."
        searchValue={search}
        onSearchChange={setSearch}
        actions={
          canEditBooks ? (
            <button
              type="button"
              onClick={handleOpenCreateModal}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-sm font-semibold cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              Thêm sách mới
            </button>
          ) : undefined
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
              {c.parentId ? `↳ ${c.name}` : c.name}
            </option>
          ))}
        </select>

        <select
          value={visibilityFilter}
          onChange={(e) =>
            setVisibilityFilter(e.target.value as 'ALL' | 'VISIBLE' | 'HIDDEN')
          }
          aria-label="Lọc trạng thái hiển thị"
          className="px-3 py-2 text-sm rounded-lg border border-slate-300 bg-white focus:outline-none focus:border-emerald-600"
        >
          <option value="ALL">Tất cả trạng thái</option>
          <option value="VISIBLE">Đang bán trên App</option>
          <option value="HIDDEN">Đang ẩn</option>
        </select>
      </FilterBar>

      <DataTable
        columns={columns}
        data={pagedBooks}
        rowKey={(b) => b.id}
        isLoading={isLoading}
        error={errorMsg}
        onRetry={loadBooks}
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={setCurrentPage}
      />

      {/* CREATE / EDIT BOOK MODAL */}
      {editingBook && (
        <div className="fixed inset-0 bg-slate-900/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 max-w-2xl w-full max-h-[90vh] overflow-y-auto p-6 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-200">
              <h3 className="text-lg font-bold text-slate-900">
                {editingBook.id
                  ? 'Chỉnh sửa thông tin Sách'
                  : 'Thêm đầu sách mới vào hệ thống'}
              </h3>
              <button
                type="button"
                onClick={() => setEditingBook(null)}
                className="text-slate-400 hover:text-slate-700 text-sm font-bold cursor-pointer"
              >
                Đóng ✕
              </button>
            </div>

            {isPriceLocked && (
              <div className="p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-800 text-xs">
                Theo quy định phân quyền, <b>Quản lý Kho</b> chỉ sửa thông tin
                mô tả, danh mục, tác giả, NXB và tồn kho; giá bán & Flash Sale
                được giữ nguyên.
              </div>
            )}

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
                    placeholder="Nhập tên sách"
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Mã ISBN (để trống nếu muốn tự sinh)
                  </label>
                  <input
                    type="text"
                    value={editingBook.isbn || ''}
                    onChange={(e) =>
                      setEditingBook({ ...editingBook, isbn: e.target.value })
                    }
                    placeholder="Nhập mã ISBN"
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Danh mục sách *
                  </label>
                  <select
                    required
                    value={editingBook.categoryId || ''}
                    onChange={(e) =>
                      setEditingBook({
                        ...editingBook,
                        categoryId: e.target.value,
                      })
                    }
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 bg-white"
                  >
                    <option value="">-- Chọn danh mục --</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.parentId ? `↳ ${c.name}` : c.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Tác giả *
                  </label>
                  <select
                    required
                    value={editingBook.authorId || ''}
                    onChange={(e) => {
                      const found = authors.find((a) => a.id === e.target.value);
                      setEditingBook({
                        ...editingBook,
                        authorId: e.target.value,
                        authorName: found?.name || '',
                      });
                    }}
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 bg-white"
                  >
                    <option value="">-- Chọn tác giả --</option>
                    {authors.map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Nhà xuất bản *
                  </label>
                  <select
                    required
                    value={editingBook.publisherId || ''}
                    onChange={(e) => {
                      const found = publishers.find(
                        (p) => p.id === e.target.value
                      );
                      setEditingBook({
                        ...editingBook,
                        publisherId: e.target.value,
                        publisherName: found?.name || '',
                      });
                    }}
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 bg-white"
                  >
                    <option value="">-- Chọn nhà xuất bản --</option>
                    {publishers.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Giá bán thực tế (VND) *
                  </label>
                  <input
                    required
                    type="number"
                    min={1000}
                    disabled={isPriceLocked}
                    value={editingBook.price ?? ''}
                    onChange={(e) =>
                      setEditingBook({
                        ...editingBook,
                        price: Number(e.target.value),
                      })
                    }
                    placeholder="Nhập giá bán"
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 disabled:bg-slate-100"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Giá gốc niêm yết (VND)
                  </label>
                  <input
                    type="number"
                    min={1000}
                    disabled={isPriceLocked}
                    value={editingBook.originalPrice ?? ''}
                    onChange={(e) =>
                      setEditingBook({
                        ...editingBook,
                        originalPrice: e.target.value
                          ? Number(e.target.value)
                          : undefined,
                      })
                    }
                    placeholder="Bằng giá bán nếu bỏ trống"
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 disabled:bg-slate-100"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Số lượng tồn kho
                  </label>
                  <input
                    type="number"
                    min={0}
                    value={editingBook.stock ?? 0}
                    onChange={(e) =>
                      setEditingBook({
                        ...editingBook,
                        stock: Number(e.target.value),
                      })
                    }
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Loại bìa
                  </label>
                  <select
                    value={editingBook.coverFormat || 'Bìa mềm'}
                    onChange={(e) =>
                      setEditingBook({
                        ...editingBook,
                        coverFormat: e.target.value as 'Bìa mềm' | 'Bìa cứng',
                      })
                    }
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 bg-white"
                  >
                    <option value="Bìa mềm">Bìa mềm</option>
                    <option value="Bìa cứng">Bìa cứng</option>
                  </select>
                </div>

                {isAdmin && (
                  <div className="sm:col-span-2 grid grid-cols-1 sm:grid-cols-2 gap-4 p-3 rounded-xl bg-amber-50/60 border border-amber-200">
                    <label className="flex items-center gap-2 text-xs font-semibold text-slate-800 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={Boolean(editingBook.isFlashSale)}
                        onChange={(e) =>
                          setEditingBook({
                            ...editingBook,
                            isFlashSale: e.target.checked,
                            flashSalePrice: e.target.checked
                              ? editingBook.flashSalePrice
                              : undefined,
                          })
                        }
                        className="rounded border-slate-300 text-emerald-600"
                      />
                      <span>Bật khuyến mãi Flash Sale</span>
                    </label>
                    {editingBook.isFlashSale && (
                      <div>
                        <label className="block text-xs font-semibold text-slate-700 mb-1">
                          Giá Flash Sale (VND) *
                        </label>
                        <input
                          required
                          type="number"
                          min={1000}
                          value={editingBook.flashSalePrice ?? ''}
                          onChange={(e) =>
                            setEditingBook({
                              ...editingBook,
                              flashSalePrice: Number(e.target.value),
                            })
                          }
                          placeholder="Nhỏ hơn giá bán thường"
                          className="w-full px-3 py-1.5 text-sm rounded-lg border border-amber-300 bg-white"
                        />
                      </div>
                    )}
                  </div>
                )}
              </div>

              <ImageUploader
                value={editingBook.coverUrl || ''}
                onChange={(url) =>
                  setEditingBook({ ...editingBook, coverUrl: url })
                }
              />

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Mô tả chi tiết nội dung
                </label>
                <textarea
                  rows={3}
                  value={editingBook.description || ''}
                  onChange={(e) =>
                    setEditingBook({
                      ...editingBook,
                      description: e.target.value,
                    })
                  }
                  placeholder="Nhập giới thiệu nội dung sách..."
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
                  disabled={isSaving}
                  className="px-5 py-2 rounded-lg bg-emerald-700 hover:bg-emerald-800 disabled:opacity-60 text-white text-sm font-semibold cursor-pointer"
                >
                  {isSaving ? 'Đang lưu...' : 'Lưu thay đổi'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
