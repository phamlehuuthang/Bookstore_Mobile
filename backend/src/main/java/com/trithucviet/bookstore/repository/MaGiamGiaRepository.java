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

    /**
     * Lấy toàn bộ voucher cho Web Admin (kể cả mã tạm ngưng hoặc đã hết hạn),
     * ưu tiên các mã đang bật lên đầu danh sách.
     */
    @Query("""
            select m from MaGiamGia m
            left join fetch m.danhMuc
            order by m.dangHoatDong desc, m.ngayKetThuc desc, m.id asc
            """)
    List<MaGiamGia> findAllWithDanhMuc();

    // Lấy chi tiết 1 voucher theo id kèm danhMuc áp dụng (dùng cho Admin/CSKH xem & sửa)
    @Query("""
            select m from MaGiamGia m
            left join fetch m.danhMuc
            where m.id = :id
            """)
    Optional<MaGiamGia> findByIdWithDanhMuc(@Param("id") String id);

    // Kiểm tra trùng mã code khi ADMIN phát hành voucher mới (không phân biệt hoa/thường)
    @Query("select count(m) > 0 from MaGiamGia m where upper(m.maCode) = upper(:maCode)")
    boolean existsByMaCodeIgnoreCase(@Param("maCode") String maCode);

    // Kiểm tra trùng mã code khi ADMIN cập nhật voucher (bỏ qua chính bản ghi đang sửa)
    @Query("select count(m) > 0 from MaGiamGia m where upper(m.maCode) = upper(:maCode) and m.id <> :id")
    boolean existsByMaCodeIgnoreCaseAndIdNot(@Param("maCode") String maCode, @Param("id") String id);
}