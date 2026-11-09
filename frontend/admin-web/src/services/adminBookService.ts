import { apiClient, db } from '@shared/index';
import { Author, Book, Category, Publisher } from '@shared/types';

export const adminBookService = {
  async getAllBooks(search?: string, categoryId?: string): Promise<Book[]> {
    return apiClient.getBooks({ search, categoryId, onlyVisible: false });
  },

  async saveBook(bookData: Partial<Book> & { title: string; price: number }): Promise<Book> {
    return apiClient.saveBook(bookData);
  },

  async toggleBookVisibility(bookId: string): Promise<Book | undefined> {
    const book = db.books.find((b) => b.id === bookId);
    if (book) {
      book.isVisible = !book.isVisible;
    }
    return book;
  },

  async deleteBook(bookId: string): Promise<void> {
    return apiClient.deleteBook(bookId);
  },

  async getCategories(): Promise<Category[]> {
    return apiClient.getCategories();
  },

  async saveCategory(cat: Partial<Category> & { name: string }): Promise<Category> {
    if (cat.id) {
      const idx = db.categories.findIndex((c) => c.id === cat.id);
      if (idx !== -1) {
        db.categories[idx] = { ...db.categories[idx], ...cat };
        return db.categories[idx];
      }
    }
    const newCat: Category = {
      id: `cat-${Date.now()}`,
      name: cat.name,
      slug: cat.name.toLowerCase().replace(/\s+/g, '-'),
      parentId: cat.parentId ?? null,
      description: cat.description || 'Danh mục sách chuyên đề',
      iconName: 'BookOpen',
      bookCount: 0,
      isActive: cat.isActive ?? true,
      displayOrder: db.categories.length + 1,
    };
    db.categories.push(newCat);
    return newCat;
  },

  async getAuthors(): Promise<Author[]> {
    return [...db.authors];
  },

  async saveAuthor(name: string, bio: string): Promise<Author> {
    const newAuthor: Author = {
      id: `auth-${Date.now()}`,
      name,
      bio,
      bookCount: 1,
    };
    db.authors.unshift(newAuthor);
    return newAuthor;
  },

  async getPublishers(): Promise<Publisher[]> {
    return [...db.publishers];
  },

  async savePublisher(name: string, address: string): Promise<Publisher> {
    const newPub: Publisher = {
      id: `pub-${Date.now()}`,
      name,
      address,
      bookCount: 1,
    };
    db.publishers.unshift(newPub);
    return newPub;
  },
};

