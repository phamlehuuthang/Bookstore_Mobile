package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.YeuCauDoiTra;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface YeuCauDoiTraRepository extends JpaRepository<YeuCauDoiTra, String> {

    @Query("select count(r) > 0 from YeuCauDoiTra r where r.donHang.id = :donHangId")
    boolean existsByDonHangId(@Param("donHangId") String donHangId);

    @Query("""
            select r from YeuCauDoiTra r
            join fetch r.donHang d
            join fetch d.nguoiDung
            where d.id = :donHangId
            """)
    Optional<YeuCauDoiTra> findByDonHangIdWithOrder(@Param("donHangId") String donHangId);

    /**
     * Nạp hàng loạt yêu cầu đổi trả theo danh sách donHangId để gắn kèm vào OrderDto mà không phát sinh N+1.
     */
    @Query("""
            select r from YeuCauDoiTra r
            join fetch r.donHang d
            join fetch d.nguoiDung
            where d.id in :donHangIds
            """)
    List<YeuCauDoiTra> findByDonHangIdInWithOrder(@Param("donHangIds") List<String> donHangIds);

    @Query("""
            select r from YeuCauDoiTra r
            join fetch r.donHang d
            join fetch d.nguoiDung
            order by r.ngayTao desc, r.id desc
            """)
    List<YeuCauDoiTra> findAllWithOrderOrdered();
}