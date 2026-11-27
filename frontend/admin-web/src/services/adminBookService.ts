import { Author, Book, Category, Publisher } from '@shared/types';
import { adminRequest } from './adminHttpClient';

// Khuôn dữ liệu AdminBookDto trả về từ Backend Java (/api/admin/books/**)
interface BackendAdminBookDto {
  id: string;
  maIsbn: string;
  tieuDe: string;
  duongDan: string;
  danhMucId: string;
  tenDanhMuc: string;
  tenDanhMucCon: string | null;
  tacGiaId: string;
  tenTacGia: string;
  nhaXuatBanId: string;
  tenNhaXuatBan: string;
  giaBan: number;
  giaGoc: number;
  dangFlashSale: boolean;
  giaFlashSale: number | null;
  donGiaApDung: number;
  phanTramGiam: number;
  tonKho: number;
  daBan: number;
  diemDanhGiaTb: number;
  soLuotDanhGia: number;
  soTrang: number | null;
  namXuatBan: number | null;
  loaiBia: string | null;
  anhBiaUrl: string;
  tomTatNgan: string | null;
  moTaChiTiet: string | null;
  tuKhoa: string | null;
  banChay: boolean;
  hienThi: boolean;
  ngayTao: string;
}

interface BackendAdminCategoryDto {
  id: string;
  tenDanhMuc: string;
  duongDan: string;
  danhMucChaId: string | null;
  moTa: string | null;
  tenBieuTuong: string | null;
  thuTuHienThi: number;
  dangHoatDong: boolean;
  soLuongSach: number;
}

interface BackendAuthorDto {
  id: string;
  tenTacGia: string;
  tieuSu: string | null;
  soLuongSach: number;
}

interface BackendPublisherDto {
  id: string;
  tenNxb: string;
  diaChi: string | null;
  soLuongSach: number;
}

function mapBook(dto: BackendAdminBookDto): Book {
  return {
    id: dto.id,
    isbn: dto.maIsbn,
    title: dto.tieuDe,
    slug: dto.duongDan,
    authorId: dto.tacGiaId,
    authorName: dto.tenTacGia,
    publisherId: dto.nhaXuatBanId,
    publisherName: dto.tenNhaXuatBan,
    categoryId: dto.danhMucId,
    categoryName: dto.tenDanhMuc,
    SubCategoryName: dto.tenDanhMucCon ?? undefined,
    price: Number(dto.giaBan ?? 0),
    originalPrice: Number(dto.giaGoc ?? dto.giaBan ?? 0),
    stock: Number(dto.tonKho ?? 0),
    soldCount: Number(dto.daBan ?? 0),
    ratingAvg: Number(dto.diemDanhGiaTb ?? 5),
    ratingCount: Number(dto.soLuotDanhGia ?? 0),
    pageCount: Number(dto.soTrang ?? 0),
    publishYear: Number(dto.namXuatBan ?? 2026),
    coverFormat: dto.loaiBia === 'Bìa cứng' ? 'Bìa cứng' : 'Bìa mềm',
    coverUrl: dto.anhBiaUrl,
    galleryUrls: [],
    shortSummary: dto.tomTatNgan ?? '',
    description: dto.moTaChiTiet ?? '',
    isFlashSale: Boolean(dto.dangFlashSale),
    flashSalePrice: dto.giaFlashSale != null ? Number(dto.giaFlashSale) : undefined,
    isBestSeller: Boolean(dto.banChay),
    isVisible: Boolean(dto.hienThi),
    createdAt: dto.ngayTao,
  };
}

function mapCategory(dto: BackendAdminCategoryDto): Category {
  return {
    id: dto.id,
    name: dto.tenDanhMuc,
    slug: dto.duongDan,
    parentId: dto.danhMucChaId,
    description: dto.moTa ?? '',
    iconName: dto.tenBieuTuong ?? 'BookOpen',
    bookCount: Number(dto.soLuongSach ?? 0),
    isActive: Boolean(dto.dangHoatDong),
    displayOrder: Number(dto.thuTuHienThi ?? 1),
  };
}

function mapAuthor(dto: BackendAuthorDto): Author {
  return {
    id: dto.id,
    name: dto.tenTacGia,
    bio: dto.tieuSu ?? '',
    bookCount: Number(dto.soLuongSach ?? 0),
  };
}

function mapPublisher(dto: BackendPublisherDto): Publisher {
  return {
    id: dto.id,
    name: dto.tenNxb,
    address: dto.diaChi ?? '',
    bookCount: Number(dto.soLuongSach ?? 0),
  };
}

/**
 * Service Quản trị Sách, Danh mục, Tác giả & Nhà xuất bản trên Web Admin (Bước 3 - Phần 3 & 4).
 * Toàn bộ request đi qua adminRequest kèm JWT; phân quyền ADMIN / WAREHOUSE_MANAGER / CSKH được siết ở Backend.
 */
export const adminBookService = {
  async getAllBooks(
    search?: string,
    categoryId?: string,
    isVisible?: boolean
  ): Promise<Book[]> {
    const params = new URLSearchParams();
    if (search?.trim()) params.set('keyword', search.trim());
    if (categoryId && categoryId !== 'ALL') params.set('danhMucId', categoryId);
    if (typeof isVisible === 'boolean') params.set('hienThi', String(isVisible));

    const query = params.toString() ? `?${params.toString()}` : '';
    const rows = await adminRequest<BackendAdminBookDto[]>(`/api/admin/books${query}`);
    return rows.map(mapBook);
  },

  async saveBook(
    bookData: Partial<Book> & { title: string; price: number }
  ): Promise<Book> {
    const payload = {
      maIsbn: bookData.isbn?.trim() || undefined,
      tieuDe: bookData.title.trim(),
      danhMucId: bookData.categoryId || '',
      tenDanhMucCon: bookData.SubCategoryName?.trim() || undefined,
      tacGiaId: bookData.authorId?.trim() || undefined,
      tenTacGia: bookData.authorName?.trim() || undefined,
      nhaXuatBanId: bookData.publisherId?.trim() || undefined,
      tenNhaXuatBan: bookData.publisherName?.trim() || undefined,
      giaBan: Number(bookData.price),
      giaGoc: Number(bookData.originalPrice ?? bookData.price),
      dangFlashSale: Boolean(bookData.isFlashSale),
      giaFlashSale:
        bookData.isFlashSale && bookData.flashSalePrice != null
          ? Number(bookData.flashSalePrice)
          : null,
      tonKho: Number(bookData.stock ?? 0),
      soTrang: bookData.pageCount ? Number(bookData.pageCount) : undefined,
      namXuatBan: bookData.publishYear ? Number(bookData.publishYear) : undefined,
      loaiBia: bookData.coverFormat || 'Bìa mềm',
      anhBiaUrl: bookData.coverUrl?.trim() || undefined,
      tomTatNgan: bookData.shortSummary?.trim() || undefined,
      moTaChiTiet: bookData.description?.trim() || undefined,
      banChay: bookData.isBestSeller,
      hienThi: bookData.isVisible,
    };

    const saved = bookData.id
      ? await adminRequest<BackendAdminBookDto>(
          `/api/admin/books/${encodeURIComponent(bookData.id)}`,
          { method: 'PUT', body: JSON.stringify(payload) }
        )
      : await adminRequest<BackendAdminBookDto>('/api/admin/books', {
          method: 'POST',
          body: JSON.stringify(payload),
        });
    return mapBook(saved);
  },

  async toggleBookVisibility(bookId: string): Promise<Book> {
    const updated = await adminRequest<BackendAdminBookDto>(
      `/api/admin/books/${encodeURIComponent(bookId)}/visibility`,
      { method: 'PATCH' }
    );
    return mapBook(updated);
  },

  async deleteBook(bookId: string): Promise<void> {
    await adminRequest<void>(`/api/admin/books/${encodeURIComponent(bookId)}`, {
      method: 'DELETE',
    });
  },

  async getCategories(): Promise<Category[]> {
    const rows = await adminRequest<BackendAdminCategoryDto[]>('/api/admin/categories');
    return rows.map(mapCategory);
  },

  async saveCategory(cat: Partial<Category> & { name: string }): Promise<Category> {
    const payload = {
      tenDanhMuc: cat.name.trim(),
      danhMucChaId: cat.parentId || null,
      moTa: cat.description?.trim() || undefined,
      tenBieuTuong: cat.iconName || undefined,
      thuTuHienThi: cat.displayOrder,
      dangHoatDong: cat.isActive,
    };

    const saved = cat.id
      ? await adminRequest<BackendAdminCategoryDto>(
          `/api/admin/categories/${encodeURIComponent(cat.id)}`,
          { method: 'PUT', body: JSON.stringify(payload) }
        )
      : await adminRequest<BackendAdminCategoryDto>('/api/admin/categories', {
          method: 'POST',
          body: JSON.stringify(payload),
        });
    return mapCategory(saved);
  },

  async deleteCategory(categoryId: string): Promise<void> {
    await adminRequest<void>(
      `/api/admin/categories/${encodeURIComponent(categoryId)}`,
      { method: 'DELETE' }
    );
  },

  async getAuthors(): Promise<Author[]> {
    const rows = await adminRequest<BackendAuthorDto[]>('/api/admin/authors');
    return rows.map(mapAuthor);
  },

  async saveAuthor(
    authorOrName: string | { id?: string; name: string; bio?: string },
    bioArg?: string
  ): Promise<Author> {
    const id = typeof authorOrName === 'string' ? undefined : authorOrName.id;
    const name = typeof authorOrName === 'string' ? authorOrName : authorOrName.name;
    const bio = typeof authorOrName === 'string' ? bioArg : authorOrName.bio;

    const payload = {
      tenTacGia: name.trim(),
      tieuSu: bio?.trim() || undefined,
    };

    const saved = id
      ? await adminRequest<BackendAuthorDto>(
          `/api/admin/authors/${encodeURIComponent(id)}`,
          { method: 'PUT', body: JSON.stringify(payload) }
        )
      : await adminRequest<BackendAuthorDto>('/api/admin/authors', {
          method: 'POST',
          body: JSON.stringify(payload),
        });
    return mapAuthor(saved);
  },

  async deleteAuthor(authorId: string): Promise<void> {
    await adminRequest<void>(
      `/api/admin/authors/${encodeURIComponent(authorId)}`,
      { method: 'DELETE' }
    );
  },

  async getPublishers(): Promise<Publisher[]> {
    const rows = await adminRequest<BackendPublisherDto[]>('/api/admin/publishers');
    return rows.map(mapPublisher);
  },

  async savePublisher(
    pubOrName: string | { id?: string; name: string; address?: string },
    addressArg?: string
  ): Promise<Publisher> {
    const id = typeof pubOrName === 'string' ? undefined : pubOrName.id;
    const name = typeof pubOrName === 'string' ? pubOrName : pubOrName.name;
    const address = typeof pubOrName === 'string' ? addressArg : pubOrName.address;

    const payload = {
      tenNxb: name.trim(),
      diaChi: address?.trim() || undefined,
    };

    const saved = id
      ? await adminRequest<BackendPublisherDto>(
          `/api/admin/publishers/${encodeURIComponent(id)}`,
          { method: 'PUT', body: JSON.stringify(payload) }
        )
      : await adminRequest<BackendPublisherDto>('/api/admin/publishers', {
          method: 'POST',
          body: JSON.stringify(payload),
        });
    return mapPublisher(saved);
  },

  async deletePublisher(publisherId: string): Promise<void> {
    await adminRequest<void>(
      `/api/admin/publishers/${encodeURIComponent(publisherId)}`,
      { method: 'DELETE' }
    );
  },

  // Bước 10: Đồng bộ AI Vector 768 chiều (pgvector) cho toàn bộ kho sách (chỉ ADMIN)
  async syncAiVectors(force = false): Promise<{
    geminiConfigured: boolean;
    totalBooks: number;
    syncedCount: number;
    skippedCount: number;
    failedCount: number;
    note: string;
  }> {
    return adminRequest(`/api/admin/books/sync-vectors?force=${force ? 'true' : 'false'}`, {
      method: 'POST',
    });
  },
};
