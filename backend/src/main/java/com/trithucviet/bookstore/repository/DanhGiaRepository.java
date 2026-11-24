package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.DanhGia;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DanhGiaRepository extends JpaRepository<DanhGia, String> {

    /**
     * Lấy danh sách đánh giá công khai (biAn = false) của 1 cuốn sách, sắp xếp mới nhất lên đầu,
     * JOIN FETCH nguoiDung để lấy tên độc giả trong 1 truy vấn.
     */
    @Query("""
            select dg from DanhGia dg
            join fetch dg.nguoiDung
            where dg.sach.id = :sachId and dg.biAn = false
            order by dg.ngayTao desc, dg.id desc
            """)
    List<DanhGia> findVisibleReviewsBySachId(@Param("sachId") String sachId);

    /**
     * Lấy toàn bộ đánh giá cho Web Admin kiểm duyệt (bao gồm cả bình luận đã bị ẩn biAn = true).
     */
    @Query("""
            select dg from DanhGia dg
            join fetch dg.sach
            join fetch dg.nguoiDung
            left join fetch dg.donHang
            order by dg.ngayTao desc, dg.id desc
            """)
    List<DanhGia> findAllWithDetails();

    /**
     * Tìm chi tiết 1 đánh giá kèm Sach, NguoiDung, DonHang phục vụ kiểm duyệt ẩn/hiện & tính lại điểm sao.
     */
    @Query("""
            select dg from DanhGia dg
            join fetch dg.sach
            join fetch dg.nguoiDung
            left join fetch dg.donHang
            where dg.id = :id
            """)
    Optional<DanhGia> findByIdWithDetails(@Param("id") String id);

    /**
     * Kiểm tra khách đã đánh giá cuốn sách này trong đơn hàng này chưa (chặn đánh giá trùng).
     */
    @Query("select count(dg) > 0 from DanhGia dg where dg.donHang.id = :donHangId and dg.sach.id = :sachId")
    boolean existsByDonHangIdAndSachId(
            @Param("donHangId") String donHangId,
            @Param("sachId") String sachId
    );

    /**
     * Đếm số đầu sách khác nhau trong đơn hàng đã được đánh giá để xác định khi nào bật don_hang.da_danh_gia = true.
     */
    @Query("select count(distinct dg.sach.id) from DanhGia dg where dg.donHang.id = :donHangId")
    long countDistinctReviewedBooksByDonHangId(@Param("donHangId") String donHangId);
}