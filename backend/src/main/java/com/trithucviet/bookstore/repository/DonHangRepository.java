package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.DonHang;
import com.trithucviet.bookstore.entity.enums.TrangThaiDon;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DonHangRepository extends JpaRepository<DonHang, String> {

    /**
     * Lấy danh sách đơn hàng của 1 khách hàng (mới nhất lên đầu), tải sẵn nguoiDung tránh N+1.
     */
    @Query("""
            select d from DonHang d
            join fetch d.nguoiDung
            where d.nguoiDung.id = :nguoiDungId
            order by d.ngayTao desc, d.id desc
            """)
    List<DonHang> findByNguoiDungIdOrdered(@Param("nguoiDungId") String nguoiDungId);

    /**
     * Tìm 1 đơn hàng thuộc đúng chủ sở hữu để chống IDOR.
     */
    @Query("""
            select d from DonHang d
            join fetch d.nguoiDung
            where d.id = :id and d.nguoiDung.id = :nguoiDungId
            """)
    Optional<DonHang> findByIdAndNguoiDungId(
            @Param("id") String id,
            @Param("nguoiDungId") String nguoiDungId
    );

    /**
     * Lấy toàn bộ đơn hàng cho Web Admin (mới nhất lên đầu).
     */
    @Query("""
            select d from DonHang d
            join fetch d.nguoiDung
            order by d.ngayTao desc, d.id desc
            """)
    List<DonHang> findAllOrdered();

    /**
     * Tìm chi tiết 1 đơn hàng kèm thông tin tài khoản người đặt.
     */
    @Query("""
            select d from DonHang d
            join fetch d.nguoiDung
            where d.id = :id
            """)
    Optional<DonHang> findByIdWithUser(@Param("id") String id);

    /**
     * Tìm toàn bộ đơn hàng đang ở một trạng thái cụ thể (tải sẵn nguoiDung để phục vụ Scheduler
     * tự động chốt các đơn DANG_GIAO quá hạn mà không bị LazyInitializationException).
     */
    @Query("""
            select d from DonHang d
            join fetch d.nguoiDung
            where d.trangThaiDon = :trangThaiDon
            order by d.ngayTao asc, d.id asc
            """)
    List<DonHang> findByTrangThaiDonWithUser(@Param("trangThaiDon") TrangThaiDon trangThaiDon);

    @Query("select count(d) > 0 from DonHang d where upper(d.maDonHang) = upper(:maDonHang)")
    boolean existsByMaDonHangIgnoreCase(@Param("maDonHang") String maDonHang);
}