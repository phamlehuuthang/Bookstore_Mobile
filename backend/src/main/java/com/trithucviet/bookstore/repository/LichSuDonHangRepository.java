package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.LichSuDonHang;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LichSuDonHangRepository extends JpaRepository<LichSuDonHang, String> {

    /**
     * Lấy Timeline hành trình của 1 đơn hàng theo đúng thứ tự phát sinh.
     */
    @Query("""
            select l from LichSuDonHang l
            where l.donHang.id = :donHangId
            order by l.thoiGian asc, l.id asc
            """)
    List<LichSuDonHang> findByDonHangIdOrdered(@Param("donHangId") String donHangId);

    /**
     * Nạp hàng loạt Timeline cho nhiều đơn hàng cùng lúc để tránh lỗi truy vấn N+1.
     */
    @Query("""
            select l from LichSuDonHang l
            where l.donHang.id in :donHangIds
            order by l.thoiGian asc, l.id asc
            """)
    List<LichSuDonHang> findByDonHangIdInOrdered(@Param("donHangIds") List<String> donHangIds);
}