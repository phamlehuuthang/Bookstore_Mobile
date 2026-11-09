import React from 'react';
import { View } from 'react-native';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AuthProvider } from '@/hooks/useAuth';
import { CartProvider } from '@/hooks/useCart';
import { AiChatProvider } from '@/hooks/useAiChat';
import { FloatingAiChatbot } from '@/features/ai-chatbot/FloatingAiChatbot';
import { colors } from '@/theme/colors';

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <AuthProvider>
        <CartProvider>
          <AiChatProvider>
            <View style={{ flex: 1, backgroundColor: colors.background }}>
              <StatusBar style="dark" />
              <Stack
                screenOptions={{
                  headerStyle: { backgroundColor: colors.surface },
                  headerTintColor: colors.textPrimary,
                  headerTitleStyle: { fontWeight: '700', fontSize: 17 },
                  headerShadowVisible: false,
                  contentStyle: { backgroundColor: colors.background },
                }}
              >
                <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
                <Stack.Screen
                  name="book/[id]"
                  options={{ title: 'Chi tiết sách' }}
                />
                <Stack.Screen
                  name="cart"
                  options={{ title: 'Giỏ hàng của bạn' }}
                />
                <Stack.Screen
                  name="checkout"
                  options={{ title: 'Thanh toán & Đặt hàng' }}
                />
              </Stack>

              {/* GLOBAL FLOATING SUPPORT CHAT WIDGET & MODAL SHEET */}
              <FloatingAiChatbot />
            </View>
          </AiChatProvider>
        </CartProvider>
      </AuthProvider>
    </SafeAreaProvider>
  );
}

