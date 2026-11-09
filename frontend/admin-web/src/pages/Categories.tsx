import React, { useEffect, useState } from 'react';
import { Building2, FolderPlus, Plus, UserCheck } from 'lucide-react';
import { Author, Category, Publisher } from '@shared/types';
import { adminBookService } from '@/services/adminBookService';

export const Categories: React.FC = () => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [authors, setAuthors] = useState<Author[]>([]);
  const [publishers, setPublishers] = useState<Publisher[]>([]);

  // Form states
  const [catName, setCatName] = useState('');
  const [catParentId, setCatParentId] = useState<string>('');
  const [catDesc, setCatDesc] = useState('');

  const [authorName, setAuthorName] = useState('');
  const [authorBio, setAuthorBio] = useState('');

  const [pubName, setPubName] = useState('');
  const [pubAddress, setPubAddress] = useState('');

  const loadAll = async () => {
    const [cList, aList, pList] = await Promise.all([
      adminBookService.getCategories(),
      adminBookService.getAuthors(),
      adminBookService.getPublishers(),
    ]);
    setCategories(cList);
    setAuthors(aList);
    setPublishers(pList);
  };

  useEffect(() => {
    loadAll();
  }, []);

  const handleAddCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!catName.trim()) return;
    await adminBookService.saveCategory({
      name: catName.trim(),
      parentId: catParentId || null,
      description: catDesc.trim() || 'Danh mục chuyên đề',
    });
    setCatName('');
    setCatDesc('');
    await loadAll();
  };

  const handleAddAuthor = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!authorName.trim()) return;
    await adminBookService.saveAuthor(
      authorName.trim(),
      authorBio.trim() || 'Tác giả sách bản quyền'
    );
    setAuthorName('');
    setAuthorBio('');
    await loadAll();
  };

  const handleAddPublisher = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!pubName.trim()) return;
    await adminBookService.savePublisher(
      pubName.trim(),
      pubAddress.trim() || 'TP. Hồ Chí Minh'
    );
    setPubName('');
    setPubAddress('');
    await loadAll();
  };

  const parentCategories = categories.filter((c) => c.parentId === null);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">
          Quản lý Danh mục Cha / Con, Tác giả & Nhà Xuất Bản
        </h1>
        <p className="text-sm text-slate-500">
          Cấu trúc phân cấp danh mục đa tầng và thông tin đối tác xuất bản
        </p>
      </div>

      {/* 1. HIERARCHICAL CATEGORIES */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <form
          onSubmit={handleAddCategory}
          className="bg-white rounded-xl border border-slate-200 p-5 space-y-3 h-fit"
        >
          <div className="flex items-center gap-2 font-bold text-slate-900">
            <FolderPlus className="w-5 h-5 text-emerald-700" />
            <span>Thêm Danh mục Sách</span>
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
              placeholder="VD: Khoa học dữ liệu & Mạng máy tính"
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
              {parentCategories.map((p) => (
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
              placeholder="Mô tả chuyên mục sách..."
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300"
            />
          </div>
          <button
            type="submit"
            className="w-full py-2 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-sm font-semibold cursor-pointer"
          >
            Lưu danh mục
          </button>
        </form>

        <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 p-5 space-y-3">
          <h3 className="text-base font-bold text-slate-900">
            Cây Phân Cấp Danh Mục ({categories.length})
          </h3>
          <div className="divide-y divide-slate-200">
            {parentCategories.map((parent) => {
              const children = categories.filter((c) => c.parentId === parent.id);
              return (
                <div key={parent.id} className="py-3 space-y-2">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="font-bold text-slate-900">{parent.name}</span>
                      <p className="text-xs text-slate-500">{parent.description}</p>
                    </div>
                    <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700">
                      {parent.bookCount} đầu sách
                    </span>
                  </div>
                  {children.length > 0 && (
                    <div className="pl-5 space-y-1.5 border-l-2 border-emerald-200 ml-2">
                      {children.map((sub) => (
                        <div
                          key={sub.id}
                          className="flex items-center justify-between text-xs bg-slate-50 p-2 rounded-lg"
                        >
                          <div>
                            <span className="font-semibold text-slate-800">
                              ↳ {sub.name}
                            </span>
                            <span className="text-slate-500 ml-2">({sub.description})</span>
                          </div>
                          <span className="text-slate-600 font-semibold">
                            {sub.bookCount} sách
                          </span>
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
          <div className="flex items-center gap-2 font-bold text-slate-900">
            <UserCheck className="w-5 h-5 text-emerald-700" />
            <span>Quản lý Tác giả ({authors.length})</span>
          </div>
          <form onSubmit={handleAddAuthor} className="flex gap-2">
            <input
              type="text"
              value={authorName}
              onChange={(e) => setAuthorName(e.target.value)}
              placeholder="Tên tác giả mới..."
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
              <Plus className="w-4 h-4" /> Thêm
            </button>
          </form>
          <div className="space-y-2 max-h-64 overflow-y-auto">
            {authors.map((a) => (
              <div
                key={a.id}
                className="p-2.5 rounded-lg border border-slate-100 bg-slate-50 flex items-center justify-between text-xs"
              >
                <div>
                  <div className="font-bold text-slate-900">{a.name}</div>
                  <div className="text-slate-500">{a.bio}</div>
                </div>
                <span className="font-semibold text-emerald-700 shrink-0 ml-2">
                  {a.bookCount} sách
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Publishers */}
        <div className="bg-white rounded-xl border border-slate-200 p-5 space-y-4">
          <div className="flex items-center gap-2 font-bold text-slate-900">
            <Building2 className="w-5 h-5 text-emerald-700" />
            <span>Quản lý Nhà Xuất Bản ({publishers.length})</span>
          </div>
          <form onSubmit={handleAddPublisher} className="flex gap-2">
            <input
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
              <Plus className="w-4 h-4" /> Thêm
            </button>
          </form>
          <div className="space-y-2 max-h-64 overflow-y-auto">
            {publishers.map((p) => (
              <div
                key={p.id}
                className="p-2.5 rounded-lg border border-slate-100 bg-slate-50 flex items-center justify-between text-xs"
              >
                <div>
                  <div className="font-bold text-slate-900">{p.name}</div>
                  <div className="text-slate-500">{p.address}</div>
                </div>
                <span className="font-semibold text-emerald-700 shrink-0 ml-2">
                  {p.bookCount} sách
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

