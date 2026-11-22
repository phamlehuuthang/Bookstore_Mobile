package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.DanhMuc;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}