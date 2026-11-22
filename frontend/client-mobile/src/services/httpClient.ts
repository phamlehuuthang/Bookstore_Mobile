import { Platform } from 'react-native';

const envBase = process.env.EXPO_PUBLIC_API_URL?.trim();

/**
 * Xác định địa chỉ gốc của Backend Spring Boot:
 * - Nếu có biến môi trường EXPO_PUBLIC_API_URL (ví dụ http://192.168.1.10:8080 khi test máy thật qua Wi-Fi) -> ưu tiên dùng.
 * - Trên Android Emulator: "localhost" trỏ vào chính máy ảo Android, nên phải dùng IP đặc biệt 10.0.2.2 để trỏ ra localhost của máy tính Windows.
 * - Trên Web (http://localhost:8081) hoặc iOS Simulator -> gọi thẳng http://localhost:8080.
 */
export const API_BASE_URL =
  envBase && envBase.length > 0
    ? envBase.replace(/\/+$/, '')
    : Platform.OS === 'android'
      ? 'http://10.0.2.2:8080'
      : 'http://localhost:8080';

const TOKEN_STORAGE_KEY = 'bookstore_mobile_token';

// Giữ JWT trong RAM để mọi request đọc nhanh ngay lập tức, kết hợp localStorage khi chạy trên trình duyệt Web
let memoryToken: string | null = null;

// Khuôn JSON chuẩn mà mọi API Backend đều trả về qua class ApiResponse<T>
interface ApiResponseEnvelope<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
}

// Lỗi API tùy chỉnh có mang kèm HTTP status (400, 401, 403, 404...) để tầng Service xử lý rẽ nhánh chính xác
export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export function getAuthToken(): string | null {
  if (memoryToken) {
    return memoryToken;
  }
  // Kiểm tra typeof window để tránh lỗi crash khi chạy ở môi trường Native hoặc SSR không có DOM/localStorage
  if (typeof window !== 'undefined' && typeof window.localStorage !== 'undefined') {
    try {
      memoryToken = window.localStorage.getItem(TOKEN_STORAGE_KEY);
    } catch {
      return null;
    }
  }
  return memoryToken;
}

export function setAuthToken(token: string): void {
  memoryToken = token;
  if (typeof window !== 'undefined' && typeof window.localStorage !== 'undefined') {
    try {
      window.localStorage.setItem(TOKEN_STORAGE_KEY, token);
    } catch {
      // Bỏ qua nếu trình duyệt chặn lưu localStorage (ví dụ chế độ riêng tư quá chặt)
    }
  }
}

export function clearAuthToken(): void {
  memoryToken = null;
  if (typeof window !== 'undefined' && typeof window.localStorage !== 'undefined') {
    try {
      window.localStorage.removeItem(TOKEN_STORAGE_KEY);
    } catch {
      // Bỏ qua lỗi storage
    }
  }
}

/**
 * Trích xuất thông báo lỗi dễ đọc bằng tiếng Việt từ response của Backend:
 * - Khi vi phạm @Valid (HTTP 400), GlobalExceptionHandler trả map lỗi từng trường trong payload.data
 *   (ví dụ: { email: "Email không đúng định dạng.", matKhau: "Mật khẩu phải từ 6 đến 100 ký tự." }).
 * - Các lỗi nghiệp vụ khác (BusinessException 401/403/404) nằm thẳng ở payload.message.
 */
function extractErrorMessage(payload: ApiResponseEnvelope<unknown> | null, status: number): string {
  if (payload?.data && typeof payload.data === 'object' && !Array.isArray(payload.data)) {
    const fieldMessages = Object.values(payload.data as Record<string, unknown>).filter(
      (val): val is string => typeof val === 'string' && val.trim().length > 0
    );
    if (fieldMessages.length > 0) {
      return fieldMessages.join(' ');
    }
  }
  if (payload?.message && payload.message.trim().length > 0) {
    return payload.message;
  }
  return `Yêu cầu thất bại (HTTP ${status}).`;
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  // Đặt withAuth: false cho API công khai (login/register) để không gửi nhầm token cũ nếu có
  withAuth?: boolean;
}

/**
 * Hàm gọi HTTP dùng chung cho toàn bộ Mobile App:
 * 1. Tự gắn header Accept/Content-Type JSON và Bearer JWT nếu đã đăng nhập.
 * 2. Bắt lỗi mất mạng / chưa bật Backend Spring Boot (status = 0).
 * 3. Parse JSON theo chuẩn ApiResponse<T>; nếu lỗi thì ném ApiError chứa thông báo tiếng Việt.
 * 4. Nếu thành công thì bóc sẵn trường `data` trả về cho Service gọi phía trên.
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, withAuth = true } = options;
  const headers: Record<string, string> = {
    Accept: 'application/json',
  };

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  if (withAuth) {
    const token = getAuthToken();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(
      `Không thể kết nối máy chủ (${API_BASE_URL}). Vui lòng kiểm tra Backend Spring Boot đã bật chưa.`,
      0
    );
  }

  const payload = (await response.json().catch(() => null)) as ApiResponseEnvelope<T> | null;

  if (!response.ok || (payload && payload.success === false)) {
    throw new ApiError(extractErrorMessage(payload, response.status), response.status);
  }

  if (!payload) {
    throw new ApiError('Phản hồi từ máy chủ không đúng định dạng JSON.', response.status);
  }

  return payload.data;
}
