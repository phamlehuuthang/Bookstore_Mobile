import { ADMIN_API_BASE_URL, clearAdminToken, getAdminToken } from './adminAuthService';

interface ApiResponseEnvelope<T> {
  success: boolean;
  message?: string;
  data: T;
}

/**
 * Trích xuất thông báo lỗi tiếng Việt từ ApiResponse của Spring Boot (kể cả lỗi @Valid từng field HTTP 400).
 */
async function extractBackendError(res: Response): Promise<string> {
  const payload = (await res.json().catch(() => null)) as ApiResponseEnvelope<unknown> | null;
  if (payload?.data && typeof payload.data === 'object' && !Array.isArray(payload.data)) {
    const fieldMsgs = Object.values(payload.data as Record<string, unknown>).filter(
      (v): v is string => typeof v === 'string' && v.trim().length > 0
    );
    if (fieldMsgs.length > 0) {
      return fieldMsgs.join(' ');
    }
  }
  return payload?.message || `Yêu cầu thất bại (HTTP ${res.status}).`;
}

/**
 * Client HTTP dùng chung cho toàn bộ các trang Web Admin gọi tới Backend Spring Boot (localhost:8080):
 * - Tự động đính kèm JWT từ localStorage/sessionStorage vào header Authorization: Bearer <token>.
 * - Nếu gặp 401 (hết hạn / mất phiên) thì dọn sạch token cũ.
 */
export async function adminRequest<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getAdminToken();
  const headers = new Headers(options.headers);
  headers.set('Accept', 'application/json');

  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  let res: Response;
  try {
    res = await fetch(`${ADMIN_API_BASE_URL}${path}`, {
      ...options,
      headers,
    });
  } catch {
    throw new Error(
      `Không thể kết nối Backend (${ADMIN_API_BASE_URL}). Vui lòng kiểm tra server Spring Boot đã chạy chưa.`
    );
  }

  if (!res.ok) {
    if (res.status === 401) {
      clearAdminToken();
    }
    throw new Error(await extractBackendError(res));
  }

  const payload = (await res.json().catch(() => null)) as ApiResponseEnvelope<T> | null;
  if (!payload || payload.success === false) {
    throw new Error(payload?.message || 'Phản hồi từ máy chủ không hợp lệ.');
  }
  return payload.data;
}
