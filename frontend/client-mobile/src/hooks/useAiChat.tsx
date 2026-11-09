import React, { createContext, useContext, useState } from 'react';
import { ChatMessage } from '@shared/types';
import { aiService, QUICK_PROMPT_CHIPS } from '@/services/aiService';

interface AiChatContextValue {
  isOpen: boolean;
  isTyping: boolean;
  messages: ChatMessage[];
  quickChips: string[];
  openChat: () => void;
  minimizeChat: () => void;
  sendMessage: (text: string) => Promise<void>;
}

const AiChatContext = createContext<AiChatContextValue | undefined>(undefined);

export function AiChatProvider({ children }: { children: React.ReactNode }) {
  const [isOpen, setIsOpen] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome-msg',
      sender: 'BOT',
      timestamp: 'Vừa xong',
      text:
        'Xin chào! **Nhà sách Tri Thức Việt** có thể giúp gì cho bạn hôm nay?\nMình sẵn sàng tư vấn sách theo sở thích, lọc sách theo ngân sách hoặc tóm tắt nhanh nội dung sách cho bạn!',
    },
  ]);

  const openChat = () => setIsOpen(true);
  // Minimize preserves the entire conversation history in memory
  const minimizeChat = () => setIsOpen(false);

  const sendMessage = async (text: string) => {
    const trimmed = text.trim();
    if (!trimmed || isTyping) return;

    const userMsg: ChatMessage = {
      id: `usr-${Date.now()}`,
      sender: 'USER',
      text: trimmed,
      timestamp: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    setIsTyping(true);

    try {
      const reply = await aiService.sendPrompt(trimmed);
      setMessages((prev) => [...prev, reply]);
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
        quickChips: QUICK_PROMPT_CHIPS,
        openChat,
        minimizeChat,
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

