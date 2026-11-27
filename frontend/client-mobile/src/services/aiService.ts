import { Book, ChatMessage } from '@shared/types';
import { apiRequest } from './httpClient';

export const QUICK_PROMPT_CHIPS: string[] = [
  '✨ Gợi ý sách hợp gu của tôi',
  '🔥 Sách đang Flash Sale giảm sâu',
  '🎁 Ghép combo sách dưới 350k',
  '💻 Gợi ý sách chủ đề lập trình',
  '🛒 Xem giỏ hàng của tôi',
  '📦 Kiểm tra đơn hàng gần nhất',
  '🎟️ Xem mã giảm giá đang mở',
];

interface BackendAiBookDto {
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
  tonKho: number;
  daBan: number;
  diemDanhGiaTb: number;
  soLuotDanhGia: number;
  loaiBia: string;
  namXuatBan: number;
  anhBiaUrl: string;
  tomTatNgan: string | null;
  banChay: boolean;
}

interface BackendChatResponse {
  reply: string;
  suggestedBooks: BackendAiBookDto[];
  cartUpdated: boolean;
  wishlistUpdated?: boolean;
  notificationsUpdated?: boolean;
  createdOrderCode: string | null;
  dynamicSuggestions?: string[];
  providerMode: string;
}

function mapAiBook(dto: BackendAiBookDto): Book {
  const isFlash =
    Boolean(dto.dangFlashSale) && dto.giaFlashSale != null && Number(dto.giaFlashSale) > 0;
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
    SubCategoryName: dto.tenDanhMucCon || undefined,
    price: Number(dto.giaBan ?? 0),
    originalPrice: Number(dto.giaGoc ?? 0),
    stock: Number(dto.tonKho ?? 0),
    soldCount: Number(dto.daBan ?? 0),
    ratingAvg: Number(dto.diemDanhGiaTb ?? 5),
    ratingCount: Number(dto.soLuotDanhGia ?? 0),
    pageCount: 0,
    publishYear: Number(dto.namXuatBan ?? 0),
    coverFormat: dto.loaiBia === 'Bìa cứng' ? 'Bìa cứng' : 'Bìa mềm',
    coverUrl: dto.anhBiaUrl,
    galleryUrls: dto.anhBiaUrl ? [dto.anhBiaUrl] : [],
    shortSummary: dto.tomTatNgan || '',
    description: dto.tomTatNgan || '',
    isFlashSale: isFlash,
    flashSalePrice: isFlash ? Number(dto.giaFlashSale) : undefined,
    isBestSeller: Boolean(dto.banChay),
    isVisible: true,
    createdAt: '',
  };
}

export interface AiPromptResult {
  chatMessage: ChatMessage;
  cartUpdated: boolean;
  wishlistUpdated: boolean;
  notificationsUpdated: boolean;
  createdOrderCode: string | null;
  dynamicSuggestions: string[];
}

export const aiService = {
  async sendPrompt(
    prompt: string,
    history: Array<{ sender: 'USER' | 'BOT'; text: string }> = []
  ): Promise<AiPromptResult> {
    const cleanPrompt = prompt
      .replace(/^(?:✨|🔥|🎁|💻|💡|📚|🔍|🚚|🎟️|✅|⚡|🛒|📦|❌|❤️|💳)\s*/u, '')
      .trim();

    const dto = await apiRequest<BackendChatResponse>('/api/v1/chat/ask', {
      method: 'POST',
      body: {
        message: cleanPrompt,
        history: history.slice(-8),
      },
    });

    const chatMessage: ChatMessage = {
      id: `bot-${Date.now()}`,
      sender: 'BOT',
      text: dto.reply || 'Mình đã xử lý yêu cầu của bạn.',
      timestamp: new Date().toLocaleTimeString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit',
      }),
      suggestedBooks: (dto.suggestedBooks || []).map(mapAiBook),
      createdOrderCode: dto.createdOrderCode ?? undefined,
    };

    return {
      chatMessage,
      cartUpdated: Boolean(dto.cartUpdated),
      wishlistUpdated: Boolean(dto.wishlistUpdated),
      notificationsUpdated: Boolean(dto.notificationsUpdated),
      createdOrderCode: dto.createdOrderCode ?? null,
      dynamicSuggestions: Array.isArray(dto.dynamicSuggestions) ? dto.dynamicSuggestions : [],
    };
  },
};
