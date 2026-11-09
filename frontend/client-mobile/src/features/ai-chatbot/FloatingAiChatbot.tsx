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

export function FloatingAiChatbot() {
  const router = useRouter();
  const { isOpen, isTyping, messages, quickChips, openChat, minimizeChat, sendMessage } =
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
      {/* FLOATING ACTION BUTTON (FAB) - Fixed bottom right, zIndex: 999, above TabBar */}
      <View pointerEvents="box-none" style={styles.fabContainer}>
        <Pressable
          onPress={openChat}
          style={({ pressed }) => [
            styles.fabButton,
            pressed && { transform: [{ scale: 0.94 }], opacity: 0.92 },
          ]}
          accessibilityLabel="Mở hộp thoại Tư vấn Sách"
        >
          <View style={styles.sparkleBadge}>
            <Ionicons name="chatbubbles" size={11} color="#FEF3C7" />
          </View>
          <Ionicons name="chatbubble-ellipses" size={24} color="#FFFFFF" />
          <Text style={styles.fabLabel}>Tư vấn</Text>
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
            {/* Grabber Handle */}
            <View style={styles.grabberRow}>
              <View style={styles.grabberBar} />
            </View>

            {/* Sheet Header with Minimize & Close preserving chat history */}
            <View style={styles.header}>
              <View style={styles.headerLeft}>
                <View style={styles.aiAvatar}>
                  <Ionicons name="headset" size={18} color="#FFFFFF" />
                </View>
                <View>
                  <Text style={styles.headerTitle}>Tư vấn viên Tri Thức Việt</Text>
                  <View style={styles.onlineRow}>
                    <View style={styles.onlineDot} />
                    <Text style={styles.onlineText}>Hỗ trợ chọn sách & giải đáp 24/7</Text>
                  </View>
                </View>
              </View>

              <View style={styles.headerActions}>
                <Pressable
                  onPress={minimizeChat}
                  hitSlop={8}
                  style={styles.headerIconBtn}
                  accessibilityLabel="Thu nhỏ cửa sổ chat"
                >
                  <Ionicons name="remove-outline" size={22} color={colors.textPrimary} />
                </Pressable>
                <Pressable
                  onPress={minimizeChat}
                  hitSlop={8}
                  style={styles.headerIconBtn}
                  accessibilityLabel="Đóng cửa sổ chat"
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
                      <Text
                        selectable
                        style={[
                          styles.msgText,
                          isUser ? styles.msgTextUser : styles.msgTextAi,
                        ]}
                      >
                        {msg.text}
                      </Text>

                      {/* Suggested Books inside consultant response */}
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
                                <Text style={styles.suggestedPrice}>
                                  {formatCurrencyVND(book.flashSalePrice ?? book.price)}
                                </Text>
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
                                    <Ionicons name="cart-outline" size={14} color="#FFFFFF" />
                                    <Text style={styles.quickAddBtnText}>Thêm giỏ</Text>
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
                      Đang tìm kiếm thông tin sách phù hợp...
                    </Text>
                  </View>
                </View>
              ) : null}
            </ScrollView>

            {/* PROMPT QUICK CHIPS */}
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
                    style={({ pressed }) => [
                      styles.chipBtn,
                      pressed && { backgroundColor: colors.primarySoft, opacity: 0.85 },
                    ]}
                  >
                    <Text style={styles.chipText}>{chip}</Text>
                  </Pressable>
                ))}
              </ScrollView>
            </View>

            {/* Input Bar */}
            <View style={styles.inputBar}>
              <TextInput
                value={inputText}
                onChangeText={setInputText}
                placeholder="Nhập câu hỏi hoặc tên sách cần tư vấn..."
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
    bottom: 84, // Positioned cleanly above TabBar so it never covers TabBar controls
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
    height: '82%',
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
    maxWidth: '88%',
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
    lineHeight: 20,
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
  suggestedPrice: {
    fontSize: 13,
    fontWeight: '700',
    color: colors.primary,
  },
  suggestedBtnRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginTop: 4,
  },
  viewBookBtn: {
    paddingHorizontal: 10,
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
    gap: 4,
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 6,
    backgroundColor: colors.primary,
  },
  quickAddBtnText: {
    fontSize: 11,
    fontWeight: '600',
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

