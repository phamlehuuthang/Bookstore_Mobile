import React, { useCallback, useEffect, useState } from 'react';
import {
  AlertCircle,
  Building2,
  CheckCircle2,
  Edit3,
  FolderPlus,
  Plus,
  Trash2,
  UserCheck,
} from 'lucide-react';
import { Author, Category, Publisher } from '@shared/types';
import { useAdminStaff } from '@/layouts/AdminLayout';
import { adminBookService } from '@/services/adminBookService';

export const Categories: React.FC = () => {
  const currentStaff = useAdminStaff();
  const isAdmin = currentStaff.role === 'ADMIN';
  const canEditCatalog =
    currentStaff.role === 'ADMIN' || currentStaff.role === 'WAREHOUSE_MANAGER';

  const [categories, setCategories] = useState<Category[]>([]);
  const [authors, setAuthors] = useState<Author[]>([]);
  const [publishers, setPublishers] = useState<Publisher[]>([]);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Form states (đều trống mặc định)
  const [editingCatId, setEditingCatId] = useState<string | null>(null);
  const [catName, setCatName] = useState('');
  const [catParentId, setCatParentId] = useState<string>('');
  const [catDesc, setCatDesc] = useState('');

  const [editingAuthorId, setEditingAuthorId] = useState<string | null>(null);
  const [authorName, setAuthorName] = useState('');
  const [authorBio, setAuthorBio] = useState('');

  const [editingPubId, setEditingPubId] = useState<string | null>(null);
  const [pubName, setPubName] = useState('');
  const [pubAddress, setPubAddress] = useState('');

  const loadAll = useCallback(async () => {
    setErrorMsg(null);
    try {
      const [cList, aList, pList] = await Promise.all([
        adminBookService.getCategories(),
        adminBookService.getAuthors(),
        adminBookService.getPublishers(),
      ]);
      setCategories(cList);
      setAuthors(aList);
      setPublishers(pList);
    } catch (err) {
      setErrorMsg(
        err instanceof Error
          ? err.message
          : 'Không thể tải danh mục, tác giả và nhà xuất bản.'
      );
    }
  }, []);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const resetCategoryForm = () => {
    setEditingCatId(null);
    setCatName('');
    setCatParentId('');
    setCatDesc('');
  };

  const handleSaveCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canEditCatalog || !catName.trim()) return;
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const saved = await adminBookService.saveCategory({
        id: editingCatId || undefined,
        name: catName.trim(),
        parentId: catParentId || null,
        description: catDesc.trim(),
      });
      resetCategoryForm();
      setSuccessMsg(`Đã lưu danh mục "${saved.name}".`);
      await loadAll();
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'Không thể lưu danh mục.');
    }
  };

  const handleDeleteCategory = async (cat: Category) => {
    if (!isAdmin) return;
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      await adminBookService.deleteCategory(cat.id);
      if (editingCatId === cat.id) resetCategoryForm();
      setSuccessMsg(`Đã xóa danh mục "${cat.name}".`);
      await loadAll();
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'Không thể xóa danh mục.');
    }
  };

  const resetAuthorForm = () => {
    setEditingAuthorId(null);
    setAuthorName('');
    setAuthorBio('');
  };

  const handleSaveAuthor = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canEditCatalog || !authorName.trim()) return;
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const saved = await adminBookService.saveAuthor({
        id: editingAuthorId || undefined,
        name: authorName.trim(),
        bio: authorBio.trim(),
      });
      resetAuthorForm();
      setSuccessMsg(`Đã lưu tác giả "${saved.name}".`);
      await loadAll();
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'Không thể lưu tác giả.');
    }
  };

  const handleDeleteAuthor = async (author: Author) => {
    if (!isAdmin) return;
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      await adminBookService.deleteAuthor(author.id);
      if (editingAuthorId === author.id) resetAuthorForm();
      setSuccessMsg(`Đã xóa tác giả "${author.name}".`);
      await loadAll();
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'Không thể xóa tác giả.');
    }
  };

  const resetPublisherForm = () => {
    setEditingPubId(null);
    setPubName('');
    setPubAddress('');
  };

  const handleSavePublisher = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canEditCatalog || !pubName.trim()) return;
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const saved = await adminBookService.savePublisher({
        id: editingPubId || undefined,
        name: pubName.trim(),
        address: pubAddress.trim(),
      });
      resetPublisherForm();
      setSuccessMsg(`Đã lưu nhà xuất bản "${saved.name}".`);
      await loadAll();
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Không thể lưu nhà xuất bản.'
      );
    }
  };

  const handleDeletePublisher = async (pub: Publisher) => {
    if (!isAdmin) return;
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      await adminBookService.deletePublisher(pub.id);
      if (editingPubId === pub.id) resetPublisherForm();
      setSuccessMsg(`Đã xóa nhà xuất bản "${pub.name}".`);
      await loadAll();
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Không thể xóa nhà xuất bản.'
      );
    }
  };

  const parentCategories = categories.filter((c) => c.parentId === null);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">
          Quản lý Danh mục Cha / Con, Tác giả & Nhà Xuất Bản
        </h1>
        <p className="text-sm text-slate-500">
          {isAdmin
            ? 'Cấu trúc phân cấp danh mục đa tầng và thông tin đối tác xuất bản (Toàn quyền)'
            : currentStaff.role === 'WAREHOUSE_MANAGER'
              ? 'Thêm mới & chỉnh sửa danh mục, tác giả, nhà xuất bản phục vụ nhập sách'
              : 'Tra cứu cây danh mục, tác giả & đối tác xuất bản (CSKH chỉ xem)'}
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

      {/* 1. HIERARCHICAL CATEGORIES */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {canEditCatalog && (
          <form
            onSubmit={handleSaveCategory}
            className="bg-white rounded-xl border border-slate-200 p-5 space-y-3 h-fit"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 font-bold text-slate-900">
                <FolderPlus className="w-5 h-5 text-emerald-700" />
                <span>
                  {editingCatId ? 'Cập nhật Danh mục' : 'Thêm Danh mục Sách'}
                </span>
              </div>
              {editingCatId && (
                <button
                  type="button"
                  onClick={resetCategoryForm}
                  className="text-xs text-slate-500 hover:text-slate-800 cursor-pointer"
                >
                  Hủy sửa
                </button>
              )}
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Tên danh mục *
              </label>
              <input
                required
                type="text"
                value={catName}
                onChange={(e) => setCatName(e.target.value)}
                placeholder="Nhập tên danh mục"
                className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Thuộc danh mục cha (Tùy chọn)
              </label>
              <select
                value={catParentId}
                onChange={(e) => setCatParentId(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 bg-white"
              >
                <option value="">-- Là Danh mục Gốc (Cha) --</option>
                {parentCategories
                  .filter((p) => p.id !== editingCatId)
                  .map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Mô tả ngắn
              </label>
              <input
                type="text"
                value={catDesc}
                onChange={(e) => setCatDesc(e.target.value)}
                placeholder="Mô tả chuyên mục sách"
                className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
              />
            </div>
            <button
              type="submit"
              className="w-full py-2 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-sm font-semibold cursor-pointer"
            >
              {editingCatId ? 'Cập nhật danh mục' : 'Lưu danh mục'}
            </button>
          </form>
        )}

        <div
          className={`${
            canEditCatalog ? 'lg:col-span-2' : 'lg:col-span-3'
          } bg-white rounded-xl border border-slate-200 p-5 space-y-3`}
        >
          <h3 className="text-base font-bold text-slate-900">
            Cây Phân Cấp Danh Mục ({categories.length})
          </h3>
          <div className="divide-y divide-slate-200">
            {parentCategories.map((parent) => {
              const children = categories.filter((c) => c.parentId === parent.id);
              return (
                <div key={parent.id} className="py-3 space-y-2">
                  <div className="flex items-center justify-between gap-2">
                    <div>
                      <span className="font-bold text-slate-900">
                        {parent.name}
                      </span>
                      <p className="text-xs text-slate-500">
                        {parent.description}
                      </p>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700">
                        {parent.bookCount} đầu sách
                      </span>
                      {canEditCatalog && (
                        <button
                          type="button"
                          onClick={() => {
                            setEditingCatId(parent.id);
                            setCatName(parent.name);
                            setCatParentId('');
                            setCatDesc(parent.description);
                          }}
                          className="p-1 rounded border border-slate-200 hover:bg-slate-100 text-slate-600 cursor-pointer"
                          title="Sửa danh mục"
                        >
                          <Edit3 className="w-3.5 h-3.5" />
                        </button>
                      )}
                      {isAdmin && (
                        <button
                          type="button"
                          onClick={() => handleDeleteCategory(parent)}
                          className="p-1 rounded border border-rose-200 hover:bg-rose-50 text-rose-600 cursor-pointer"
                          title="Xóa danh mục"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>
                  </div>
                  {children.length > 0 && (
                    <div className="pl-5 space-y-1.5 border-l-2 border-emerald-200 ml-2">
                      {children.map((sub) => (
                        <div
                          key={sub.id}
                          className="flex items-center justify-between gap-2 text-xs bg-slate-50 p-2 rounded-lg"
                        >
                          <div>
                            <span className="font-semibold text-slate-800">
                              ↳ {sub.name}
                            </span>
                            {sub.description && (
                              <span className="text-slate-500 ml-2">
                                ({sub.description})
                              </span>
                            )}
                          </div>
                          <div className="flex items-center gap-2 shrink-0">
                            <span className="text-slate-600 font-semibold">
                              {sub.bookCount} sách
                            </span>
                            {canEditCatalog && (
                              <button
                                type="button"
                                onClick={() => {
                                  setEditingCatId(sub.id);
                                  setCatName(sub.name);
                                  setCatParentId(sub.parentId || '');
                                  setCatDesc(sub.description);
                                }}
                                className="p-1 rounded border border-slate-200 hover:bg-white text-slate-600 cursor-pointer"
                                title="Sửa danh mục con"
                              >
                                <Edit3 className="w-3 h-3" />
                              </button>
                            )}
                            {isAdmin && (
                              <button
                                type="button"
                                onClick={() => handleDeleteCategory(sub)}
                                className="p-1 rounded border border-rose-200 hover:bg-rose-50 text-rose-600 cursor-pointer"
                                title="Xóa danh mục con"
                              >
                                <Trash2 className="w-3 h-3" />
                              </button>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* 2. AUTHORS & PUBLISHERS */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Authors */}
        <div className="bg-white rounded-xl border border-slate-200 p-5 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 font-bold text-slate-900">
              <UserCheck className="w-5 h-5 text-emerald-700" />
              <span>Quản lý Tác giả ({authors.length})</span>
            </div>
            {editingAuthorId && (
              <button
                type="button"
                onClick={resetAuthorForm}
                className="text-xs text-slate-500 hover:text-slate-800 cursor-pointer"
              >
                Hủy sửa
              </button>
            )}
          </div>
          {canEditCatalog && (
            <form onSubmit={handleSaveAuthor} className="flex gap-2">
              <input
                required
                type="text"
                value={authorName}
                onChange={(e) => setAuthorName(e.target.value)}
                placeholder="Tên tác giả..."
                className="flex-1 px-3 py-2 text-sm rounded-lg border border-slate-300"
              />
              <input
                type="text"
                value={authorBio}
                onChange={(e) => setAuthorBio(e.target.value)}
                placeholder="Tiểu sử ngắn..."
                className="flex-1 px-3 py-2 text-sm rounded-lg border border-slate-300"
              />
              <button
                type="submit"
                className="px-3 py-2 rounded-lg bg-emerald-700 text-white text-xs font-semibold cursor-pointer shrink-0 flex items-center gap-1"
              >
                <Plus className="w-4 h-4" />
                {editingAuthorId ? 'Lưu' : 'Thêm'}
              </button>
            </form>
          )}
          <div className="space-y-2 max-h-64 overflow-y-auto">
            {authors.map((a) => (
              <div
                key={a.id}
                className="p-2.5 rounded-lg border border-slate-100 bg-slate-50 flex items-center justify-between gap-2 text-xs"
              >
                <div>
                  <div className="font-bold text-slate-900">{a.name}</div>
                  <div className="text-slate-500">{a.bio}</div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <span className="font-semibold text-emerald-700">
                    {a.bookCount} sách
                  </span>
                  {canEditCatalog && (
                    <button
                      type="button"
                      onClick={() => {
                        setEditingAuthorId(a.id);
                        setAuthorName(a.name);
                        setAuthorBio(a.bio);
                      }}
                      className="p-1 rounded border border-slate-200 hover:bg-white text-slate-600 cursor-pointer"
                      title="Sửa tác giả"
                    >
                      <Edit3 className="w-3 h-3" />
                    </button>
                  )}
                  {isAdmin && (
                    <button
                      type="button"
                      onClick={() => handleDeleteAuthor(a)}
                      className="p-1 rounded border border-rose-200 hover:bg-rose-50 text-rose-600 cursor-pointer"
                      title="Xóa tác giả"
                    >
                      <Trash2 className="w-3 h-3" />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Publishers */}
        <div className="bg-white rounded-xl border border-slate-200 p-5 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 font-bold text-slate-900">
              <Building2 className="w-5 h-5 text-emerald-700" />
              <span>Quản lý Nhà Xuất Bản ({publishers.length})</span>
            </div>
            {editingPubId && (
              <button
                type="button"
                onClick={resetPublisherForm}
                className="text-xs text-slate-500 hover:text-slate-800 cursor-pointer"
              >
                Hủy sửa
              </button>
            )}
          </div>
          {canEditCatalog && (
            <form onSubmit={handleSavePublisher} className="flex gap-2">
              <input
                required
                type="text"
                value={pubName}
                onChange={(e) => setPubName(e.target.value)}
                placeholder="Tên Nhà Xuất Bản..."
                className="flex-1 px-3 py-2 text-sm rounded-lg border border-slate-300"
              />
              <input
                type="text"
                value={pubAddress}
                onChange={(e) => setPubAddress(e.target.value)}
                placeholder="Địa chỉ trụ sở..."
                className="flex-1 px-3 py-2 text-sm rounded-lg border border-slate-300"
              />
              <button
                type="submit"
                className="px-3 py-2 rounded-lg bg-emerald-700 text-white text-xs font-semibold cursor-pointer shrink-0 flex items-center gap-1"
              >
                <Plus className="w-4 h-4" />
                {editingPubId ? 'Lưu' : 'Thêm'}
              </button>
            </form>
          )}
          <div className="space-y-2 max-h-64 overflow-y-auto">
            {publishers.map((p) => (
              <div
                key={p.id}
                className="p-2.5 rounded-lg border border-slate-100 bg-slate-50 flex items-center justify-between gap-2 text-xs"
              >
                <div>
                  <div className="font-bold text-slate-900">{p.name}</div>
                  <div className="text-slate-500">{p.address}</div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <span className="font-semibold text-emerald-700">
                    {p.bookCount} sách
                  </span>
                  {canEditCatalog && (
                    <button
                      type="button"
                      onClick={() => {
                        setEditingPubId(p.id);
                        setPubName(p.name);
                        setPubAddress(p.address);
                      }}
                      className="p-1 rounded border border-slate-200 hover:bg-white text-slate-600 cursor-pointer"
                      title="Sửa nhà xuất bản"
                    >
                      <Edit3 className="w-3 h-3" />
                    </button>
                  )}
                  {isAdmin && (
                    <button
                      type="button"
                      onClick={() => handleDeletePublisher(p)}
                      className="p-1 rounded border border-rose-200 hover:bg-rose-50 text-rose-600 cursor-pointer"
                      title="Xóa nhà xuất bản"
                    >
                      <Trash2 className="w-3 h-3" />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
