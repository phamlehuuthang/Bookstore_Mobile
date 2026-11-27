package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.HanhViNguoiDung;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HanhViNguoiDungRepository extends JpaRepository<HanhViNguoiDung, String> {

    /**
     * Tải toàn bộ lịch sử hành vi của 1 khách hàng theo thứ tự mới nhất lên đầu,
     * join fetch sẵn danhMuc và sach để tính điểm Affinity Score mà không bị N+1 query.
     */
    @Query("""
            select h from HanhViNguoiDung h
            join fetch h.danhMuc
            left join fetch h.sach
            where h.nguoiDung.id = :nguoiDungId
            order by h.ngayTao desc, h.id desc
            """)
    List<HanhViNguoiDung> findByNguoiDungIdWithDetailsOrdered(@Param("nguoiDungId") String nguoiDungId);
}
