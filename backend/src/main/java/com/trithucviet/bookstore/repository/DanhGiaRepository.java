package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.DanhGia;
import java.util.List;
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
}