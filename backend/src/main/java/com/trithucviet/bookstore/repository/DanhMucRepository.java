package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.DanhMuc;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DanhMucRepository extends JpaRepository<DanhMuc, String> {

    /**
     * Lấy toàn bộ danh mục đang hoạt động kèm thông tin danh mục cha (LEFT JOIN FETCH để tránh N+1
     * và tránh LazyInitializationException), sắp xếp theo thứ tự hiển thị tăng dần.
     */
    @Query("""
            select d from DanhMuc d
            left join fetch d.danhMucCha
            where d.dangHoatDong = true
            order by d.thuTuHienThi asc, d.id asc
            """)
    List<DanhMuc> findActiveCategoriesOrdered();

    /**
     * Lấy toàn bộ danh mục (bao gồm cả danh mục tạm ẩn) cho Web Admin quản lý.
     */
    @Query("""
            select d from DanhMuc d
            left join fetch d.danhMucCha
            order by d.thuTuHienThi asc, d.id asc
            """)
    List<DanhMuc> findAllOrdered();

    @Query("select count(d) > 0 from DanhMuc d where upper(d.duongDan) = upper(:duongDan)")
    boolean existsByDuongDanIgnoreCase(@Param("duongDan") String duongDan);

    @Query("select count(d) > 0 from DanhMuc d where upper(d.duongDan) = upper(:duongDan) and d.id <> :id")
    boolean existsByDuongDanIgnoreCaseAndIdNot(@Param("duongDan") String duongDan, @Param("id") String id);

    @Query("select count(d) > 0 from DanhMuc d where d.danhMucCha.id = :parentId")
    boolean existsByDanhMucChaId(@Param("parentId") String parentId);
}