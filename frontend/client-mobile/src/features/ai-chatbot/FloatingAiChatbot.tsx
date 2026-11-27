import React, { useState } from 'react';
import {
  Image,
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { formatCurrencyVND } from '@shared/types';
import { colors } from '@/theme/colors';
import { useAiChat } from '@/hooks/useAiChat';
import { useCart } from '@/hooks/useCart';

/**
 * Hiển thị nội dung tin nhắn AI sạch đẹp, dễ đọc trên React Native:
 * - Tự động chuyển **in đậm**, *in nghiêng*, `mã code` thành style thật (không còn ký tự ** hay * thô).
 * - Tách danh sách gạch đầu dòng (•, -, 1.) thành từng hàng căn lề rõ ràng, thoáng mắt.
 * - Ẩn tag kỹ thuật [ID: sach_xxx] khỏi mắt người đọc.
 */
function renderInlineSegments(rawLine: string, isUser: boolean) {
  const cleaned = rawLine.replace(/\s*\[ID:\s*sach_\w+\]/gi, '');
  const tokenRegex = /(\*\*[^*]+\*\*|`[^`]+`|\*[^*\n]+\*)/g;
  const parts = cleaned.split(tokenRegex);

  return parts.map((part, idx) => {
    if (!part) return null;
    if (part.startsWith('**') && part.endsWith('**') && part.length > 4) {
      return (
        <Text
          key={idx}
          style={[
            styles.inlineBold,
            isUser ? { color: '#FFFFFF' } : { color: colors.textPrimary },
          ]}
        >
          {part.slice(2, -2)}
        </Text>
      );
    }
    if (part.startsWith('`') && part.endsWith('`') && part.length > 2) {
      return (
        <Text
          key={idx}
          style={isUser ? styles.inlineCodeUser : styles.inlineCodeAi}
        >
          {part.slice(1, -1)}
        </Text>
      );
    }
    if (part.startsWith('*') && part.endsWith('*') && part.length > 2) {
      return (
        <Text
          key={idx}
          style={[
            styles.inlineItalic,
            isUser ? { color: 'rgba(255,255,255,0.9)' } : { color: colors.textSecondary },
          ]}
        >
          {part.slice(1, -1)}
        </Text>
      );
    }
    return <React.Fragment key={idx}>{part}</React.Fragment>;
  });
}

function FormattedChatText({ text, isUser }: { text: string; isUser: boolean }) {
  const lines = text
    .replace(/\r\n/g, '\n')
    .split('\n')
    .map((l) => l.trimEnd());

  return (
    <View style={styles.formattedContainer}>
      {lines.map((line, idx) => {
        const trimmed = line.trim();
        if (!trimmed) {
          return <View key={idx} style={{ height: 4 }} />;
        }
        if (/^-{3,}$/.test(trimmed)) {
          return <View key={idx} style={styles.msgDivider} />;
        }

        const headingMatch = trimmed.match(/^#{1,3}\s+(.+)$/);
        if (headingMatch) {
          return (
            <Text
              key={idx}
              selectable
              style={[styles.msgHeading, isUser && { color: '#FFFFFF' }]}
            >
              {renderInlineSegments(headingMatch[1], isUser)}
            </Text>
          );
        }

        const numberedMatch = trimmed.match(/^(\d+)\.\s+(.+)$/);
        if (numberedMatch && !isUser) {
          return (
            <View key={idx} style={styles.listItemRow}>
              <View style={styles.numberBadge}>
                <Text style={styles.numberBadgeText}>{numberedMatch[1]}</Text>
              </View>
              <Text selectable style={[styles.msgText, styles.msgTextAi, { flex: 1 }]}>
                {renderInlineSegments(numberedMatch[2], false)}
              </Text>
            </View>
          );
        }

        const bulletMatch = trimmed.match(/^(?:[•\-*]|\+)\s+(.+)$/);
        if (bulletMatch) {
          const isSubBullet = /^\s{2,}/.test(line);
          return (
            <View
              key={idx}
              style={[styles.listItemRow, isSubBullet && { paddingLeft: 14 }]}
            >
              <Text
                style={[
                  styles.bulletDot,
                  isUser ? { color: '#FFFFFF' } : { color: colors.primary },
                ]}
              >
                {isSubBullet ? '◦' : '•'}
              </Text>
              <Text
                selectable
                style={[
                  styles.msgText,
                  isUser ? styles.msgTextUser : styles.msgTextAi,
                  { flex: 1 },
                ]}
              >
                {renderInlineSegments(bulletMatch[1], isUser)}
              </Text>
            </View>
          );
        }

        return (
          <Text
            key={idx}
            selectable
            style={[styles.msgText, isUser ? styles.msgTextUser : styles.msgTextAi]}
          >
            {renderInlineSegments(trimmed, isUser)}
          </Text>
        );
      })}
    </View>
  );
}

export function FloatingAiChatbot() {
  const router = useRouter();
  const { isOpen, isTyping, messages, quickChips, openChat, minimizeChat, resetChat, sendMessage } =
    useAiChat();
  const { addToCart } = useCart();
  const [inputText, setInputText] = useState('');

  const handleSend = async () => {
    if (!inputText.trim()) return;
    const text = inputText;
    setInputText('');
    await sendMessage(text);
  };

  return (
    <>
      {/* FLOATING ACTION BUTTON (FAB) */}
      <View pointerEvents="box-none" style={styles.fabContainer}>
        <Pressable
          onPress={openChat}
          style={({ pressed }) => [
            styles.fabButton,
            pressed && { transform: [{ scale: 0.94 }], opacity: 0.92 },
          ]}
          accessibilityLabel="Mở hộp thoại Trợ lý AI"
        >
          <View style={styles.sparkleBadge}>
            <Ionicons name="sparkles" size={11} color="#FEF3C7" />
          </View>
          <Ionicons name="chatbubble-ellipses" size={24} color="#FFFFFF" />
          <Text style={styles.fabLabel}>Trợ lý AI</Text>
        </Pressable>
      </View>

      {/* MODAL SHEET TRƯỢT TỪ ĐÁY LÊN */}
      <Modal
        visible={isOpen}
        animationType="slide"
        transparent
        onRequestClose={minimizeChat}
      >
        <KeyboardAvoidingView
          behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
          style={styles.modalBackdrop}
        >
          <Pressable style={styles.backdropDismiss} onPress={minimizeChat} />

          <View style={styles.sheetContainer}>
            <View style={styles.grabberRow}>
              <View style={styles.grabberBar} />
            </View>

            {/* Header */}
            <View style={styles.header}>
              <View style={styles.headerLeft}>
                <View style={styles.aiAvatar}>
                  <Ionicons name="sparkles" size={18} color="#FFFFFF" />
                </View>
                <View>
                  <Text style={styles.headerTitle}>Trợ lý AI Tri Thức Việt</Text>
                  <View style={styles.onlineRow}>
                    <View style={styles.onlineDot} />
                    <Text style={styles.onlineText}>Tự đặt sách • Săn mã • Tư vấn 24/7</Text>
                  </View>
                </View>
              </View>

              <View style={styles.headerActions}>
                <Pressable
                  onPress={resetChat}
                  hitSlop={8}
                  style={styles.headerIconBtn}
                  accessibilityLabel="Làm mới đoạn hội thoại"
                >
                  <Ionicons name="refresh-outline" size={19} color={colors.textPrimary} />
                </Pressable>
                <Pressable
                  onPress={minimizeChat}
                  hitSlop={8}
                  style={styles.headerIconBtn}
                  accessibilityLabel="Thu nhỏ cửa sổ chat"
                >
                  <Ionicons name="close" size={20} color={colors.textPrimary} />
                </Pressable>
              </View>
            </View>

            {/* Conversation History */}
            <ScrollView
              style={styles.chatScroll}
              contentContainerStyle={styles.chatContent}
              keyboardShouldPersistTaps="handled"
            >
              {messages.map((msg) => {
                const isUser = msg.sender === 'USER';
                return (
                  <View
                    key={msg.id}
                    style={[
                      styles.msgRow,
                      isUser ? styles.msgRowUser : styles.msgRowAi,
                    ]}
                  >
                    <View
                      style={[
                        styles.msgBubble,
                        isUser ? styles.msgBubbleUser : styles.msgBubbleAi,
                      ]}
                    >
                      <FormattedChatText text={msg.text} isUser={isUser} />

                      {/* Thẻ biên lai đơn hàng vừa được AI tự động tạo thành công */}
                      {msg.createdOrderCode ? (
                        <View style={styles.orderReceiptCard}>
                          <View style={styles.orderReceiptHeader}>
                            <Ionicons name="checkmark-circle" size={18} color="#059669" />
                            <Text style={styles.orderReceiptTitle}>
                              Đơn hàng {msg.createdOrderCode} đã tạo!
                            </Text>
                          </View>
                          <Text style={styles.orderReceiptSub}>
                            Đã tự động áp mã ưu đãi tốt nhất • Thanh toán COD
                          </Text>
                          <Pressable
                            onPress={() => {
                              minimizeChat();
                              router.push('/orders');
                            }}
                            style={styles.viewOrderBtn}
                          >
                            <Text style={styles.viewOrderBtnText}>Xem hành trình đơn hàng →</Text>
                          </Pressable>
                        </View>
                      ) : null}

                      {/* Suggested Books inside AI response */}
                      {msg.suggestedBooks && msg.suggestedBooks.length > 0 ? (
                        <View style={styles.suggestedList}>
                          {msg.suggestedBooks.map((book) => (
                            <View key={book.id} style={styles.suggestedBookCard}>
                              <Image
                                source={{ uri: book.coverUrl }}
                                style={styles.suggestedCover}
                              />
                              <View style={{ flex: 1, gap: 2 }}>
                                <Text style={styles.suggestedTitle} numberOfLines={2}>
                                  {book.title}
                                </Text>
                                <View style={styles.priceBadgeRow}>
                                  <Text style={styles.suggestedPrice}>
                                    {formatCurrencyVND(book.flashSalePrice ?? book.price)}
                                  </Text>
                                  {book.isFlashSale ? (
                                    <View style={styles.flashTag}>
                                      <Text style={styles.flashTagText}>FLASH SALE</Text>
                                    </View>
                                  ) : null}
                                </View>
                                <View style={styles.suggestedBtnRow}>
                                  <Pressable
                                    onPress={() => {
                                      minimizeChat();
                                      router.push(`/book/${book.id}`);
                                    }}
                                    style={styles.viewBookBtn}
                                  >
                                    <Text style={styles.viewBookBtnText}>Xem chi tiết</Text>
                                  </Pressable>
                                  <Pressable
                                    onPress={() => addToCart(book, 1)}
                                    style={styles.quickAddBtn}
                                  >
                                    <Ionicons name="cart-outline" size={13} color="#FFFFFF" />
                                    <Text style={styles.quickAddBtnText}>Thêm giỏ</Text>
                                  </Pressable>
                                  <Pressable
                                    onPress={() =>
                                      sendMessage(
                                        `⚡ Đặt ngay 1 cuốn ${book.title} [ID: ${book.id}]`
                                      )
                                    }
                                    disabled={isTyping}
                                    style={styles.quickOrderBtn}
                                  >
                                    <Ionicons name="flash" size={12} color="#FFFFFF" />
                                    <Text style={styles.quickOrderBtnText}>Đặt ngay</Text>
                                  </Pressable>
                                </View>
                              </View>
                            </View>
                          ))}
                        </View>
                      ) : null}

                      <Text
                        style={[
                          styles.msgTime,
                          isUser && { color: 'rgba(255,255,255,0.75)' },
                        ]}
                      >
                        {msg.timestamp}
                      </Text>
                    </View>
                  </View>
                );
              })}

              {isTyping ? (
                <View style={[styles.msgRow, styles.msgRowAi]}>
                  <View style={[styles.msgBubble, styles.msgBubbleAi]}>
                    <Text style={styles.typingText}>
                      Trợ lý AI đang xử lý yêu cầu của bạn...
                    </Text>
                  </View>
                </View>
              ) : null}
            </ScrollView>

            {/* DYNAMIC PROMPT QUICK CHIPS */}
            <View style={styles.chipsSection}>
              <ScrollView
                horizontal
                showsHorizontalScrollIndicator={false}
                contentContainerStyle={styles.chipsScroll}
                keyboardShouldPersistTaps="handled"
              >
                {quickChips.map((chip) => (
                  <Pressable
                    key={chip}
                    onPress={() => sendMessage(chip)}
                    disabled={isTyping}
                    style={({ pressed }) => [
                      styles.chipBtn,
                      pressed && { backgroundColor: colors.primarySoft, opacity: 0.85 },
                    ]}
                  >
                    <Text style={styles.chipText} numberOfLines={1}>
                      {chip}
                    </Text>
                  </Pressable>
                ))}
              </ScrollView>
            </View>

            {/* Input Bar */}
            <View style={styles.inputBar}>
              <TextInput
                value={inputText}
                onChangeText={setInputText}
                placeholder="VD: Đặt cuốn Dế Mèn Phiêu Lưu Ký, Sách hợp gu..."
                placeholderTextColor={colors.textMuted}
                style={styles.textInput}
                onSubmitEditing={handleSend}
                returnKeyType="send"
              />
              <Pressable
                onPress={handleSend}
                disabled={!inputText.trim() || isTyping}
                style={({ pressed }) => [
                  styles.sendBtn,
                  (!inputText.trim() || isTyping) && { opacity: 0.45 },
                  pressed && { transform: [{ scale: 0.95 }] },
                ]}
              >
                <Ionicons name="send" size={18} color="#FFFFFF" />
              </Pressable>
            </View>
          </View>
        </KeyboardAvoidingView>
      </Modal>
    </>
  );
}

const styles = StyleSheet.create({
  fabContainer: {
    position: 'absolute',
    right: 16,
    bottom: 84,
    zIndex: 999,
  },
  fabButton: {
    minWidth: 58,
    minHeight: 58,
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 29,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: '#FFFFFF',
    elevation: 6,
  },
  sparkleBadge: {
    position: 'absolute',
    top: -4,
    right: -2,
    width: 22,
    height: 22,
    borderRadius: 11,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1.5,
    borderColor: '#FFFFFF',
  },
  fabLabel: {
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '700',
    marginTop: 2,
  },
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.5)',
    justifyContent: 'flex-end',
  },
  backdropDismiss: {
    flex: 1,
  },
  sheetContainer: {
    height: '84%',
    backgroundColor: colors.background,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    overflow: 'hidden',
  },
  grabberRow: {
    alignItems: 'center',
    paddingTop: 10,
    paddingBottom: 4,
    backgroundColor: colors.surface,
  },
  grabberBar: {
    width: 42,
    height: 5,
    borderRadius: 3,
    backgroundColor: '#CBD5E1',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  headerLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  aiAvatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  onlineRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    marginTop: 2,
  },
  onlineDot: {
    width: 7,
    height: 7,
    borderRadius: 4,
    backgroundColor: '#10B981',
  },
  onlineText: {
    fontSize: 12,
    color: colors.textSecondary,
  },
  headerActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  headerIconBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: colors.surfaceMuted,
    alignItems: 'center',
    justifyContent: 'center',
  },
  chatScroll: {
    flex: 1,
  },
  chatContent: {
    padding: 16,
    gap: 12,
  },
  msgRow: {
    flexDirection: 'row',
    width: '100%',
  },
  msgRowUser: {
    justifyContent: 'flex-end',
  },
  msgRowAi: {
    justifyContent: 'flex-start',
  },
  msgBubble: {
    maxWidth: '89%',
    borderRadius: 16,
    padding: 12,
    gap: 6,
  },
  msgBubbleUser: {
    backgroundColor: colors.primary,
    borderBottomRightRadius: 4,
  },
  msgBubbleAi: {
    backgroundColor: colors.surface,
    borderBottomLeftRadius: 4,
    borderWidth: 1,
    borderColor: colors.border,
  },
  msgText: {
    fontSize: 14,
    lineHeight: 21,
  },
  formattedContainer: {
    gap: 5,
  },
  msgHeading: {
    fontSize: 14.5,
    fontWeight: '700',
    color: colors.primaryDark,
    marginTop: 2,
  },
  msgDivider: {
    height: 1,
    backgroundColor: colors.border,
    marginVertical: 4,
  },
  listItemRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 7,
  },
  bulletDot: {
    fontSize: 15,
    lineHeight: 21,
    fontWeight: '800',
  },
  numberBadge: {
    minWidth: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: colors.primarySoft,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 1,
    paddingHorizontal: 5,
  },
  numberBadgeText: {
    fontSize: 11,
    fontWeight: '700',
    color: colors.primaryDark,
  },
  inlineBold: {
    fontWeight: '700',
  },
  inlineItalic: {
    fontStyle: 'italic',
  },
  inlineCodeAi: {
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    fontSize: 12.5,
    fontWeight: '700',
    color: colors.primaryDark,
    backgroundColor: colors.primarySoft,
    paddingHorizontal: 5,
    paddingVertical: 1,
    borderRadius: 4,
  },
  inlineCodeUser: {
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    fontSize: 12.5,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  msgTextUser: {
    color: '#FFFFFF',
  },
  msgTextAi: {
    color: colors.textPrimary,
  },
  msgTime: {
    fontSize: 10,
    color: colors.textMuted,
    alignSelf: 'flex-end',
  },
  typingText: {
    fontSize: 13,
    fontStyle: 'italic',
    color: colors.primary,
  },
  orderReceiptCard: {
    marginTop: 6,
    padding: 10,
    borderRadius: 10,
    backgroundColor: '#ECFDF5',
    borderWidth: 1,
    borderColor: '#6EE7B7',
    gap: 4,
  },
  orderReceiptHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  orderReceiptTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: '#065F46',
  },
  orderReceiptSub: {
    fontSize: 11,
    color: '#047857',
  },
  viewOrderBtn: {
    marginTop: 4,
    alignSelf: 'flex-start',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 6,
    backgroundColor: '#059669',
  },
  viewOrderBtnText: {
    fontSize: 11,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  suggestedList: {
    gap: 8,
    marginTop: 6,
  },
  suggestedBookCard: {
    flexDirection: 'row',
    gap: 10,
    padding: 8,
    borderRadius: 10,
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.border,
  },
  suggestedCover: {
    width: 52,
    height: 72,
    borderRadius: 6,
    backgroundColor: colors.surfaceMuted,
  },
  suggestedTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  priceBadgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  suggestedPrice: {
    fontSize: 13,
    fontWeight: '700',
    color: colors.primary,
  },
  flashTag: {
    paddingHorizontal: 5,
    paddingVertical: 1,
    borderRadius: 4,
    backgroundColor: '#FEE2E2',
  },
  flashTagText: {
    fontSize: 9,
    fontWeight: '800',
    color: '#DC2626',
  },
  suggestedBtnRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    gap: 6,
    marginTop: 4,
  },
  viewBookBtn: {
    paddingHorizontal: 8,
    paddingVertical: 5,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: colors.primary,
  },
  viewBookBtnText: {
    fontSize: 11,
    fontWeight: '600',
    color: colors.primary,
  },
  quickAddBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
    paddingHorizontal: 8,
    paddingVertical: 5,
    borderRadius: 6,
    backgroundColor: colors.primary,
  },
  quickAddBtnText: {
    fontSize: 11,
    fontWeight: '600',
    color: '#FFFFFF',
  },
  quickOrderBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
    paddingHorizontal: 9,
    paddingVertical: 5,
    borderRadius: 6,
    backgroundColor: colors.accent,
  },
  quickOrderBtnText: {
    fontSize: 11,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  chipsSection: {
    backgroundColor: colors.surface,
    paddingTop: 10,
    paddingBottom: 6,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  chipsScroll: {
    paddingHorizontal: 12,
    gap: 8,
  },
  chipBtn: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 999,
    backgroundColor: colors.primarySoft,
    borderWidth: 1,
    borderColor: colors.primaryBorder,
    maxWidth: 280,
  },
  chipText: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.primaryDark,
  },
  inputBar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: colors.surface,
  },
  textInput: {
    flex: 1,
    minHeight: 44,
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 12,
    paddingHorizontal: 14,
    fontSize: 14,
    color: colors.textPrimary,
  },
  sendBtn: {
    width: 44,
    height: 44,
    borderRadius: 12,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
