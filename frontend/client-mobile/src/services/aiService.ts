import { apiClient } from '@shared/index';
import { ChatMessage } from '@shared/types';

export const QUICK_PROMPT_CHIPS: string[] = [
  '💡 Gợi ý sách lập trình cho người mới',
  '📚 Tìm sách kinh tế dưới 150k',
  '🔍 Tóm tắt cuốn Đắc Nhân Tâm',
  '🚚 Chính sách vận chuyển & đổi trả thế nào?',
];

export const aiService = {
  async sendPrompt(prompt: string): Promise<ChatMessage> {
    const cleanPrompt = prompt.replace(/^[💡📚🔍🚚]\s*/, '');
    return apiClient.askAiAssistant(cleanPrompt);
  },
};

