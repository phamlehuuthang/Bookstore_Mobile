package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.GioHang;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GioHangRepository extends JpaRepository<GioHang, GioHang.GioHangId> {

    /**
     * Lấy toàn bộ các dòng trong giỏ hàng của 1 khách hàng.
     * Dùng JOIN FETCH tải sẵn sach + tacGia + danhMuc (+ danhMucCha) trong 1 câu SQL duy nhất
     * để tránh LazyInitializationException và N+1 khi dựng CartResponseDto hoặc kiểm tra Voucher theo danh mục.
     * Sắp xếp theo ngayCapNhat giảm dần để cuốn khách vừa thêm/sửa mới nhất nổi lên đầu danh sách.
     */
    @Query("""
            select g from GioHang g
            join fetch g.sach s
            join fetch s.tacGia
            join fetch s.danhMuc d
            left join fetch d.danhMucCha
            where g.id.nguoiDungId = :nguoiDungId
            order by g.ngayCapNhat desc, g.id.sachId asc
            """)
    List<GioHang> findAllWithSachByNguoiDungId(@Param("nguoiDungId") String nguoiDungId);

    /**
     * Cập nhật hàng loạt trạng thái da_chon (Chọn tất cả / Bỏ chọn tất cả) bằng 1 câu UPDATE duy nhất.
     * - flushAutomatically = true: đẩy mọi thay đổi đang chờ xuống DB trước khi chạy UPDATE.
     * - clearAutomatically = true: xóa Persistence Context sau khi UPDATE để câu SELECT kế tiếp đọc lại dữ liệu mới từ DB,
     *   tránh lỗi Hibernate trả về giá trị daChon cũ đang lưu trong cache tầng 1.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update GioHang g
            set g.daChon = :daChon
            where g.id.nguoiDungId = :nguoiDungId
            """)
    int updateDaChonByNguoiDungId(
            @Param("nguoiDungId") String nguoiDungId,
            @Param("daChon") boolean daChon
    );
}