import { NotificationItem } from '@shared/types';
import { apiRequest } from './httpClient';

interface BackendNotificationDto {
  id: string;
  nguoiDungId: string | null;
  tieuDe: string;
  noiDung: string;
  loaiThongBao: 'DON_HANG' | 'KHUYEN_MAI' | 'HE_THONG';
  daDoc: boolean;
  ngayTao: string;
}

function mapNotificationType(
  type: BackendNotificationDto['loaiThongBao']
): NotificationItem['type'] {
  switch (type) {
    case 'DON_HANG':
      return 'ORDER';
    case 'KHUYEN_MAI':
      return 'PROMO';
    default:
      return 'SYSTEM';
  }
}

function formatNotifTime(iso: string | null | undefined): string {
  if (!iso) return 'Vừa xong';
  return iso.slice(0, 16).replace('T', ' ');
}

function mapNotification(dto: BackendNotificationDto): NotificationItem {
  return {
    id: dto.id,
    title: dto.tieuDe,
    message: dto.noiDung,
    type: mapNotificationType(dto.loaiThongBao),
    isRead: Boolean(dto.daDoc),
    createdAt: formatNotifTime(dto.ngayTao),
  };
}

/**
 * Service Thông báo đẩy cho Mobile App (Bước 7 - Phần 3):
 * - GET /api/v1/notifications: lấy thông báo cá nhân + thông báo chung toàn shop.
 * - PATCH /api/v1/notifications/{id}/read: đánh dấu đã đọc 1 thông báo.
 * - PATCH /api/v1/notifications/read-all: đánh dấu đã đọc tất cả thông báo cá nhân.
 */
export const notificationService = {
  async getNotifications(): Promise<NotificationItem[]> {
    const rows = await apiRequest<BackendNotificationDto[]>('/api/v1/notifications');
    return rows.map(mapNotification);
  },

  async markAsRead(notificationId: string): Promise<NotificationItem> {
    const dto = await apiRequest<BackendNotificationDto>(
      `/api/v1/notifications/${encodeURIComponent(notificationId)}/read`,
      { method: 'PATCH' }
    );
    return mapNotification(dto);
  },

  async markAllAsRead(): Promise<number> {
    const res = await apiRequest<{ soThongBaoDaDoc: number }>(
      '/api/v1/notifications/read-all',
      { method: 'PATCH' }
    );
    return Number(res?.soThongBaoDaDoc ?? 0);
  },
};
