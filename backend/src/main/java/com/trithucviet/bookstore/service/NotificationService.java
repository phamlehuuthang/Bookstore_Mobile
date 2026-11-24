package com.trithucviet.bookstore.service;

import com.trithucviet.bookstore.dto.request.CreateNotificationRequest;
import com.trithucviet.bookstore.dto.response.NotificationDto;
import com.trithucviet.bookstore.entity.ThongBao;
import com.trithucviet.bookstore.entity.TkNguoiDung;
import com.trithucviet.bookstore.entity.enums.LoaiThongBao;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import com.trithucviet.bookstore.exception.BusinessException;
import com.trithucviet.bookstore.repository.ThongBaoRepository;
import com.trithucviet.bookstore.repository.TkNguoiDungRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ Thông báo đẩy cho Mobile App & Phát sóng thông báo trên Web Admin (Bước 7 - Phần 3).
 *
 * Quy tắc xử lý trạng thái đã đọc (daDoc):
 * - Khi ADMIN / CSKH phát sóng thông báo mới (broadcastToAllCustomers), hệ thống tạo 1 bản ghi riêng
 *   cho từng tài khoản CUSTOMER để mỗi khách quản lý trạng thái đã đọc độc lập mà không cần thêm bảng mới.
 * - Các dòng thông báo chung cũ trong DB (nguoi_dung_id IS NULL, như tb_02) vẫn hiển thị cho mọi khách
 *   nhưng KHÔNG bị sửa cờ da_doc khi 1 khách bấm đã đọc để tránh ảnh hưởng tới khách hàng khác.
 */
@Service
public class NotificationService {

    private final ThongBaoRepository thongBaoRepository;
    private final TkNguoiDungRepository tkNguoiDungRepository;

    public NotificationService(
            ThongBaoRepository thongBaoRepository,
            TkNguoiDungRepository tkNguoiDungRepository
    ) {
        this.thongBaoRepository = thongBaoRepository;
        this.tkNguoiDungRepository = tkNguoiDungRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getCustomerNotifications(String nguoiDungId) {
        requireActiveCustomer(nguoiDungId);
        return thongBaoRepository.findForCustomerOrdered(nguoiDungId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Đánh dấu đã đọc 1 thông báo:
     * - Nếu là thông báo riêng của khách -> cập nhật daDoc = true trong DB.
     * - Nếu là thông báo chung cũ (nguoiDung == null) -> trả về trạng thái daDoc = true cho client mà không sửa dòng dùng chung.
     * - Nếu thuộc về khách khác hoặc không tồn tại -> báo 404 (chống IDOR).
     */
    @Transactional
    public NotificationDto markAsRead(String nguoiDungId, String notificationId) {
        requireActiveCustomer(nguoiDungId);
        ThongBao notif = thongBaoRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo."));

        if (notif.getNguoiDung() == null) {
            return new NotificationDto(
                    notif.getId(),
                    null,
                    notif.getTieuDe(),
                    notif.getNoiDung(),
                    notif.getLoaiThongBao(),
                    Boolean.TRUE,
                    notif.getNgayTao()
            );
        }

        if (!notif.getNguoiDung().getId().equals(nguoiDungId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo.");
        }

        if (!Boolean.TRUE.equals(notif.getDaDoc())) {
            notif.setDaDoc(Boolean.TRUE);
            thongBaoRepository.saveAndFlush(notif);
        }
        return toDto(notif);
    }

    @Transactional
    public int markAllAsRead(String nguoiDungId) {
        requireActiveCustomer(nguoiDungId);
        return thongBaoRepository.markAllReadByNguoiDungId(nguoiDungId);
    }

    /**
     * Phát sóng thông báo khuyến mãi / hệ thống tới toàn bộ tài khoản CUSTOMER (ADMIN & CSKH).
     * Tạo bản ghi riêng cho từng khách để mỗi khách có trạng thái daDoc độc lập.
     */
    @Transactional
    public int broadcastToAllCustomers(CreateNotificationRequest request) {
        List<TkNguoiDung> customers = tkNguoiDungRepository.findByVaiTroOrdered(VaiTro.CUSTOMER);
        if (customers.isEmpty()) {
            return 0;
        }

        LoaiThongBao type = request.loaiThongBao() != null
                ? request.loaiThongBao()
                : LoaiThongBao.KHUYEN_MAI;
        LocalDateTime now = LocalDateTime.now();
        String title = request.tieuDe().trim();
        String content = request.noiDung().trim();

        List<ThongBao> batch = new ArrayList<>(customers.size());
        for (TkNguoiDung customer : customers) {
            ThongBao notif = new ThongBao();
            notif.setId("tb_" + shortUuid());
            notif.setNguoiDung(customer);
            notif.setTieuDe(title);
            notif.setNoiDung(content);
            notif.setLoaiThongBao(type);
            notif.setDaDoc(Boolean.FALSE);
            notif.setNgayTao(now);
            batch.add(notif);
        }

        thongBaoRepository.saveAllAndFlush(batch);
        return batch.size();
    }

    private TkNguoiDung requireActiveCustomer(String nguoiDungId) {
        TkNguoiDung user = tkNguoiDungRepository.findById(nguoiDungId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "Phiên đăng nhập không hợp lệ hoặc tài khoản không tồn tại."
                ));
        if (Boolean.TRUE.equals(user.getBiKhoa())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ CSKH để được hỗ trợ."
            );
        }
        return user;
    }

    private NotificationDto toDto(ThongBao notif) {
        return new NotificationDto(
                notif.getId(),
                notif.getNguoiDung() != null ? notif.getNguoiDung().getId() : null,
                notif.getTieuDe(),
                notif.getNoiDung(),
                notif.getLoaiThongBao(),
                Boolean.TRUE.equals(notif.getDaDoc()),
                notif.getNgayTao()
        );
    }

    private String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
