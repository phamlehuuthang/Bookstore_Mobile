package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.ChiTietDonHang;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, String> {

    /**
     * Kiểm tra cuốn sách đã từng phát sinh trong đơn hàng chưa (chặn xóa cứng để bảo toàn lịch sử đơn).
     */
    @Query("select count(c) > 0 from ChiTietDonHang c where c.sach.id = :sachId")
    boolean existsBySachId(@Param("sachId") String sachId);

    /**
     * Lấy danh sách dòng sản phẩm của 1 đơn hàng kèm đối tượng Sach (dùng khi hoàn kho hoặc dựng DTO).
     */
    @Query("""
            select c from ChiTietDonHang c
            join fetch c.sach
            where c.donHang.id = :donHangId
            order by c.id asc
            """)
    List<ChiTietDonHang> findByDonHangIdWithSach(@Param("donHangId") String donHangId);

    /**
     * Nạp hàng loạt dòng sản phẩm cho nhiều đơn hàng cùng lúc để tránh lỗi truy vấn N+1.
     */
    @Query("""
            select c from ChiTietDonHang c
            join fetch c.sach
            where c.donHang.id in :donHangIds
            order by c.id asc
            """)
    List<ChiTietDonHang> findByDonHangIdInWithSach(@Param("donHangIds") List<String> donHangIds);
}