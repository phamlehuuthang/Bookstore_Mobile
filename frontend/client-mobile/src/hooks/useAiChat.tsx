import React, { createContext, useContext, useState } from 'react';
import { ChatMessage } from '@shared/types';
import { aiService, QUICK_PROMPT_CHIPS } from '@/services/aiService';
import { useCart } from '@/hooks/useCart';
import { useAuth } from '@/hooks/useAuth';

interface AiChatContextValue {
  isOpen: boolean;
  isTyping: boolean;
  messages: ChatMessage[];
  quickChips: string[];
  openChat: () => void;
  minimizeChat: () => void;
  resetChat: () => void;
  sendMessage: (text: string) => Promise<void>;
}

const WELCOME_MESSAGE: ChatMessage = {
  id: 'welcome-msg',
  sender: 'BOT',
  timestamp: 'Vừa xong',
  text:
    'Xin chào! Mình là **Trợ lý AI Tri Thức Việt** ✨\nMình có thể hỗ trợ bạn ngay:\n• **Tự động đặt sách** kèm săn mã giảm giá tốt nhất\n• **Gợi ý sách tiếng Việt** theo chủ đề, hợp gu hoặc ghép combo\n• **Quản lý giỏ hàng**, yêu thích & tra cứu/hủy đơn nhanh',
};

const AiChatContext = createContext<AiChatContextValue | undefined>(undefined);

export function AiChatProvider({ children }: { children: React.ReactNode }) {
  const { refreshCart } = useCart();
  const { refreshWishlist, refreshNotifications } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([WELCOME_MESSAGE]);
  const [quickChips, setQuickChips] = useState<string[]>(QUICK_PROMPT_CHIPS);

  const openChat = () => setIsOpen(true);
  const minimizeChat = () => setIsOpen(false);

  const resetChat = () => {
    setMessages([{ ...WELCOME_MESSAGE, timestamp: 'Vừa xong' }]);
    setQuickChips(QUICK_PROMPT_CHIPS);
  };

  const sendMessage = async (text: string) => {
    const trimmed = text.trim();
    if (!trimmed || isTyping) return;

    const historySnapshot = messages
      .filter((m) => m.id !== 'welcome-msg')
      .slice(-8)
      .map((m) => ({ sender: m.sender, text: m.text }));

    const userMsg: ChatMessage = {
      id: `usr-${Date.now()}`,
      sender: 'USER',
      text: trimmed,
      timestamp: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    setIsTyping(true);

    try {
      const result = await aiService.sendPrompt(trimmed, historySnapshot);
      setMessages((prev) => [...prev, result.chatMessage]);

      if (result.dynamicSuggestions.length > 0) {
        setQuickChips([
          ...result.dynamicSuggestions,
          ...QUICK_PROMPT_CHIPS.filter((c) => !result.dynamicSuggestions.includes(c)).slice(0, 3),
        ]);
      }

      const syncTasks: Promise<unknown>[] = [];
      if (result.cartUpdated) {
        syncTasks.push(refreshCart());
      }
      if (result.wishlistUpdated) {
        syncTasks.push(refreshWishlist());
      }
      if (result.notificationsUpdated) {
        syncTasks.push(refreshNotifications());
      }
      if (syncTasks.length > 0) {
        await Promise.allSettled(syncTasks);
      }
    } catch (err) {
      const errMsg: ChatMessage = {
        id: `bot-err-${Date.now()}`,
        sender: 'BOT',
        timestamp: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
        text:
          err instanceof Error
            ? `Xin lỗi, hiện chưa thể kết nối Trợ lý AI (${err.message}). Bạn thử lại sau ít giây nhé!`
            : 'Xin lỗi, hiện chưa thể kết nối Trợ lý AI. Bạn thử lại sau ít giây nhé!',
      };
      setMessages((prev) => [...prev, errMsg]);
    } finally {
      setIsTyping(false);
    }
  };

  return (
    <AiChatContext.Provider
      value={{
        isOpen,
        isTyping,
        messages,
        quickChips,
        openChat,
        minimizeChat,
        resetChat,
        sendMessage,
      }}
    >
      {children}
    </AiChatContext.Provider>
  );
}

export function useAiChat(): AiChatContextValue {
  const ctx = useContext(AiChatContext);
  if (!ctx) {
    throw new Error('useAiChat must be used within an AiChatProvider');
  }
  return ctx;
}
