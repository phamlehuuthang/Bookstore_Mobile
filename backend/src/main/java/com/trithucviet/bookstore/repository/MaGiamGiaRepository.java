package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.MaGiamGia;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaGiamGiaRepository extends JpaRepository<MaGiamGia, String> {

    /**
     * Tìm voucher theo mã code (không phân biệt chữ hoa/thường), kèm left join fetch danhMuc áp dụng.
     * Dùng LEFT JOIN FETCH vì danh_muc_id có thể NULL (mã áp dụng toàn cửa hàng)
     * và để tránh lỗi LazyInitializationException khi đọc tenDanhMuc.
     */
    @Query("""
            select m from MaGiamGia m
            left join fetch m.danhMuc
            where upper(m.maCode) = upper(:maCode)
            """)
    Optional<MaGiamGia> findByMaCodeWithDanhMucIgnoreCase(@Param("maCode") String maCode);

    /**
     * Lấy danh sách mã giảm giá đang mở cho Mobile App (GET /api/v1/vouchers/active):
     * - Đang bật kích hoạt (dangHoatDong = true).
     * - Còn trong khoảng thời gian hiệu lực (ngayBatDau <= hôm nay <= ngayKetThuc).
     * - Vẫn còn lượt sử dụng (daSuDung < gioiHanLuot).
     */
    @Query("""
            select m from MaGiamGia m
            left join fetch m.danhMuc
            where m.dangHoatDong = true
              and m.ngayBatDau <= :homNay
              and m.ngayKetThuc >= :homNay
              and m.daSuDung < m.gioiHanLuot
            order by m.donToiThieu asc, m.id asc
            """)
    List<MaGiamGia> findActiveVouchers(@Param("homNay") LocalDate homNay);
}