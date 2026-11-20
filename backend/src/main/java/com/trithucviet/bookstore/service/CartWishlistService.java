package com.trithucviet.bookstore.service;

import com.trithucviet.bookstore.dto.response.WishlistItemResponse;
import com.trithucviet.bookstore.dto.response.WishlistToggleResponse;
import com.trithucviet.bookstore.entity.Sach;
import com.trithucviet.bookstore.entity.SachYeuThich;
import com.trithucviet.bookstore.entity.TkNguoiDung;
import com.trithucviet.bookstore.exception.BusinessException;
import com.trithucviet.bookstore.repository.SachRepository;
import com.trithucviet.bookstore.repository.SachYeuThichRepository;
import com.trithucviet.bookstore.repository.TkNguoiDungRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ Tủ sách yêu thích (Bước 4 - phần 1).
 * Phần Giỏ hàng sẽ được bổ sung vào chính service này ở Bước 4 - phần 2.
 */
@Service
public class CartWishlistService {

    private final SachYeuThichRepository sachYeuThichRepository;
    private final SachRepository sachRepository;
    private final TkNguoiDungRepository tkNguoiDungRepository;

    public CartWishlistService(
            SachYeuThichRepository sachYeuThichRepository,
            SachRepository sachRepository,
            TkNguoiDungRepository tkNguoiDungRepository
    ) {
        this.sachYeuThichRepository = sachYeuThichRepository;
        this.sachRepository = sachRepository;
        this.tkNguoiDungRepository = tkNguoiDungRepository;
    }

    // readOnly = true giúp Hibernate bỏ qua cơ chế dirty-checking, tối ưu hiệu năng cho thao tác chỉ đọc
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(String nguoiDungId) {
        getActiveCustomer(nguoiDungId);
        return sachYeuThichRepository.findAllWithSachByNguoiDungId(nguoiDungId)
                .stream()
                .map(this::toItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WishlistToggleResponse getStatus(String nguoiDungId, String sachId) {
        getActiveCustomer(nguoiDungId);
        getVisibleBook(sachId);
        boolean daYeuThich = sachYeuThichRepository.existsById(new SachYeuThich.SachYeuThichId(nguoiDungId, sachId));
        return new WishlistToggleResponse(sachId, daYeuThich, sachYeuThichRepository.countByNguoiDungId(nguoiDungId));
    }

    /**
     * Đổi trạng thái yêu thích: thả tim nếu chưa có, bỏ tim nếu đã có.
     * Toàn bộ thao tác nằm trong 1 @Transactional để đảm bảo kiểm tra + ghi/xóa + đếm lại nhất quán.
     */
    @Transactional
    public WishlistToggleResponse toggle(String nguoiDungId, String sachId) {
        TkNguoiDung nguoiDung = getActiveCustomer(nguoiDungId);
        // Bảng sach_yeu_thich dùng khóa chính kép (nguoi_dung_id, sach_id) -> vừa chống trùng, vừa tra cứu O(1)
        SachYeuThich.SachYeuThichId id = new SachYeuThich.SachYeuThichId(nguoiDungId, sachId);

        boolean daYeuThich;
        if (sachYeuThichRepository.existsById(id)) {
            // Cố tình cho bỏ tim ngay mà không kiểm tra sach.hienThi:
            // nếu sách từng được thích trước đây rồi sau đó Admin ẩn đi, khách vẫn gỡ được khỏi tủ của mình.
            sachYeuThichRepository.deleteById(id);
            daYeuThich = false;
        } else {
            // Chỉ khi thêm mới (thả tim) mới bắt buộc sách phải tồn tại và còn đang kinh doanh (hienThi = true)
            Sach sach = getVisibleBook(sachId);
            SachYeuThich yeuThich = new SachYeuThich();
            yeuThich.setId(id);
            yeuThich.setNguoiDung(nguoiDung);
            yeuThich.setSach(sach);
            sachYeuThichRepository.save(yeuThich);
            daYeuThich = true;
        }

        return new WishlistToggleResponse(sachId, daYeuThich, sachYeuThichRepository.countByNguoiDungId(nguoiDungId));
    }

    /**
     * Xóa tường minh 1 cuốn sách khỏi tủ yêu thích (endpoint RESTful bổ sung bên cạnh toggle).
     * Khác với toggle: nếu khách lỡ bấm đúp 2 lần khi mạng chậm sẽ báo 404 chứ không tự thêm ngược lại.
     */
    @Transactional
    public WishlistToggleResponse remove(String nguoiDungId, String sachId) {
        getActiveCustomer(nguoiDungId);
        SachYeuThich.SachYeuThichId id = new SachYeuThich.SachYeuThichId(nguoiDungId, sachId);
        if (!sachYeuThichRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Cuốn sách này không có trong Tủ sách yêu thích của bạn.");
        }
        sachYeuThichRepository.deleteById(id);
        return new WishlistToggleResponse(sachId, false, sachYeuThichRepository.countByNguoiDungId(nguoiDungId));
    }

    /**
     * Dù JWT đã xác thực thành công ở Filter, vẫn truy vấn lại DB để kiểm tra trạng thái mới nhất:
     * tránh trường hợp tài khoản vừa bị Admin khóa (biKhoa = true) nhưng token cũ chưa hết hạn vẫn dùng được.
     */
    private TkNguoiDung getActiveCustomer(String nguoiDungId) {
        TkNguoiDung nguoiDung = tkNguoiDungRepository.findById(nguoiDungId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại. Vui lòng đăng nhập lại."));
        if (Boolean.TRUE.equals(nguoiDung.getBiKhoa())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ CSKH để được hỗ trợ.");
        }
        return nguoiDung;
    }

    /**
     * Tách riêng 2 mã lỗi để frontend xử lý đúng:
     * - Không có id trong bảng sach -> 404 Not Found.
     * - Sách đã bị Admin ẩn (hienThi = false) -> 400 Bad Request ("ngừng kinh doanh").
     */
    private Sach getVisibleBook(String sachId) {
        Sach sach = sachRepository.findById(sachId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy cuốn sách có mã " + sachId + "."));
        if (!Boolean.TRUE.equals(sach.getHienThi())) {
            throw new BusinessException("Cuốn sách này hiện đã ngừng kinh doanh.");
        }
        return sach;
    }

    // Đọc getTacGia() và getDanhMuc() ở đây an toàn không dính lỗi Lazy vì đã được JOIN FETCH sẵn từ Repository
    private WishlistItemResponse toItemResponse(SachYeuThich yeuThich) {
        Sach sach = yeuThich.getSach();
        return new WishlistItemResponse(
                sach.getId(),
                sach.getTieuDe(),
                sach.getTacGia().getTenTacGia(),
                sach.getDanhMuc().getTenDanhMuc(),
                sach.getGiaBan(),
                sach.getGiaGoc(),
                sach.getDangFlashSale(),
                sach.getGiaFlashSale(),
                sach.getTonKho(),
                sach.getDiemDanhGiaTb(),
                sach.getAnhBiaUrl(),
                yeuThich.getNgayTao()
        );
    }
}
