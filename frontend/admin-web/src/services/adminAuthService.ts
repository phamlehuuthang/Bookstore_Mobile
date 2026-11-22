import { UserRole } from '@shared/types';

// Web Admin chỉ cho phép 3 vai trò nhân sự nội bộ (loại trừ CUSTOMER)
export type StaffRole = Exclude<UserRole, 'CUSTOMER'>;

export interface StaffAccount {
  id: string;
  fullName: string;
  email: string;
  role: StaffRole;
  roleLabel: string;
  avatarCode: string;
}

// Khuôn dữ liệu UserDto trả về từ Backend Java (tên field tiếng Việt không dấu)
interface BackendUserDto {
  id: string;
  hoTen: string;
  email: string;
  soDienThoai: string | null;
  vaiTro: UserRole;
  biKhoa: boolean;
}

interface BackendAuthResponse {
  accessToken: string;
  tokenType: string;
  user: BackendUserDto;
}

interface ApiResponseEnvelope<T> {
  success: boolean;
  message?: string;
  data: T;
}

// Đọc VITE_API_URL nếu có cấu hình file .env cho Vite; mặc định gọi thẳng Backend localhost:8080
const viteEnv = (import.meta as unknown as { env?: Record<string, string | undefined> }).env;
export const ADMIN_API_BASE_URL =
  viteEnv?.VITE_API_URL?.trim()?.replace(/\/+$/, '') || 'http://localhost:8080';

const ADMIN_TOKEN_KEY = 'bookstore_admin_token';

function toRoleLabel(role: StaffRole): string {
  switch (role) {
    case 'ADMIN':
      return 'QUẢN TRỊ VIÊN (ADMIN)';
    case 'WAREHOUSE_MANAGER':
      return 'QUẢN LÝ KHO (WAREHOUSE)';
    case 'CSKH':
      return 'CHĂM SÓC KHÁCH HÀNG (CSKH)';
  }
}

// Lấy 2 chữ cái đầu của tên để hiển thị trên vòng tròn avatar góc phải header (ví dụ: Phạm Minh Quân -> MQ)
function toAvatarCode(fullName: string): string {
  const words = fullName.trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) return 'NV';
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return words
    .slice(-2)
    .map((w) => w[0].toUpperCase())
    .join('');
}

function mapStaff(dto: BackendUserDto): StaffAccount {
  const role = dto.vaiTro as StaffRole;
  return {
    id: dto.id,
    fullName: dto.hoTen,
    email: dto.email,
    role,
    roleLabel: toRoleLabel(role),
    avatarCode: toAvatarCode(dto.hoTen),
  };
}

/**
 * Đọc JWT từ cả 2 kho lưu trữ (localStorage khi tick "Duy trì đăng nhập", sessionStorage khi chỉ giữ trong phiên tab).
 */
export function getAdminToken(): string | null {
  try {
    return (
      window.localStorage.getItem(ADMIN_TOKEN_KEY) ||
      window.sessionStorage.getItem(ADMIN_TOKEN_KEY)
    );
  } catch {
    return null;
  }
}

/**
 * Lưu JWT theo lựa chọn của người dùng:
 * - rememberMe = true  -> lưu localStorage (tắt trình duyệt mở lại vẫn còn phiên).
 * - rememberMe = false -> lưu sessionStorage (đóng tab là tự đăng xuất).
 * Luôn xóa cả 2 kho trước khi ghi để tránh sót 1 token cũ nằm ở kho còn lại.
 */
function saveAdminToken(token: string, rememberMe: boolean): void {
  try {
    window.localStorage.removeItem(ADMIN_TOKEN_KEY);
    window.sessionStorage.removeItem(ADMIN_TOKEN_KEY);
    if (rememberMe) {
      window.localStorage.setItem(ADMIN_TOKEN_KEY, token);
    } else {
      window.sessionStorage.setItem(ADMIN_TOKEN_KEY, token);
    }
  } catch {
    // Bỏ qua nếu trình duyệt chặn Storage
  }
}

export function clearAdminToken(): void {
  try {
    window.localStorage.removeItem(ADMIN_TOKEN_KEY);
    window.sessionStorage.removeItem(ADMIN_TOKEN_KEY);
  } catch {
    // Bỏ qua lỗi Storage
  }
}

// Đọc thông báo lỗi từ ApiResponse của Backend (bao gồm cả lỗi @Valid từng field nếu HTTP 400)
async function parseAdminError(res: Response): Promise<string> {
  const payload = (await res.json().catch(() => null)) as ApiResponseEnvelope<unknown> | null;
  if (payload?.data && typeof payload.data === 'object' && !Array.isArray(payload.data)) {
    const fieldMsgs = Object.values(payload.data as Record<string, unknown>).filter(
      (v): v is string => typeof v === 'string' && v.trim().length > 0
    );
    if (fieldMsgs.length > 0) {
      return fieldMsgs.join(' ');
    }
  }
  return payload?.message || `Đăng nhập thất bại (HTTP ${res.status}).`;
}

/**
 * Service Xác thực Web Admin Dashboard (Bước 2 - Phần 1):
 * - Gọi POST /api/admin/auth/login (Platform Guard phía Backend chặn CUSTOMER và tài khoản bị khóa).
 * - Khôi phục phiên từ JWT qua GET /api/v1/auth/me mỗi lần F5 trình duyệt.
 */
export const adminAuthService = {
  async loginAdmin(email: string, password: string, rememberMe: boolean): Promise<StaffAccount> {
    let res: Response;
    try {
      res = await fetch(`${ADMIN_API_BASE_URL}/api/admin/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
        },
        body: JSON.stringify({
          email: email.trim(),
          matKhau: password,
        }),
      });
    } catch {
      throw new Error(
        `Không thể kết nối Backend (${ADMIN_API_BASE_URL}). Vui lòng kiểm tra server Spring Boot đã chạy chưa.`
      );
    }

    if (!res.ok) {
      throw new Error(await parseAdminError(res));
    }

    const payload = (await res.json()) as ApiResponseEnvelope<BackendAuthResponse>;
    saveAdminToken(payload.data.accessToken, rememberMe);
    return mapStaff(payload.data.user);
  },

  /**
   * Kiểm tra lại token đang lưu mỗi khi F5 hoặc mở lại Web Admin:
   * - Gửi GET /api/v1/auth/me kèm Authorization Bearer token.
   * - Nếu Backend báo 401/403 (hết hạn hoặc bị khóa) hoặc là CUSTOMER -> xóa sạch token và trả null để quay về màn Login.
   */
  async restoreAdminSession(): Promise<StaffAccount | null> {
    const token = getAdminToken();
    if (!token) {
      return null;
    }

    try {
      const res = await fetch(`${ADMIN_API_BASE_URL}/api/v1/auth/me`, {
        method: 'GET',
        headers: {
          Accept: 'application/json',
          Authorization: `Bearer ${token}`,
        },
      });

      if (!res.ok) {
        if (res.status === 401 || res.status === 403) {
          clearAdminToken();
        }
        return null;
      }

      const payload = (await res.json()) as ApiResponseEnvelope<BackendUserDto>;
      if (payload.data.vaiTro === 'CUSTOMER') {
        clearAdminToken();
        return null;
      }
      return mapStaff(payload.data);
    } catch {
      return null;
    }
  },

  logoutAdmin(): void {
    clearAdminToken();
  },
};
