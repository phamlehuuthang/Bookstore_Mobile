import React from 'react';
import { Pressable, View } from 'react-native';
import { Stack, useRouter } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { Ionicons } from '@expo/vector-icons';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AuthProvider } from '@/hooks/useAuth';
import { CartProvider } from '@/hooks/useCart';
import { AiChatProvider } from '@/hooks/useAiChat';
import { FloatingAiChatbot } from '@/features/ai-chatbot/FloatingAiChatbot';
import { colors } from '@/theme/colors';

export default function RootLayout() {
  const router = useRouter();

  const renderBackButton = () => (
    <Pressable
      onPress={() => {
        if (router.canGoBack()) {
          router.back();
        } else {
          router.replace('/(tabs)');
        }
      }}
      hitSlop={10}
      style={{
        marginRight: 10,
        padding: 4,
        borderRadius: 8,
        alignItems: 'center',
        justifyContent: 'center',
      }}
      accessibilityLabel="Quay lại"
    >
      <Ionicons name="arrow-back" size={22} color={colors.textPrimary} />
    </Pressable>
  );

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
                  headerTitleStyle: { fontWeight: '700', fontSize: 15.5 },
                  headerShadowVisible: false,
                  contentStyle: { backgroundColor: colors.background },
                }}
              >
                <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
                <Stack.Screen
                  name="book/[id]"
                  options={{
                    title: 'Chi tiết sách',
                    headerLeft: renderBackButton,
                  }}
                />
                <Stack.Screen
                  name="cart"
                  options={{
                    title: 'Giỏ hàng của bạn',
                    headerLeft: renderBackButton,
                  }}
                />
                <Stack.Screen
                  name="checkout"
                  options={{
                    title: 'Thanh toán & Đặt hàng',
                    headerLeft: renderBackButton,
                  }}
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

