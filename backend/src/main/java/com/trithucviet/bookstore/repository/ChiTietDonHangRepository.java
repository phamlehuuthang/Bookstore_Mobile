package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.ChiTietDonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, String> {

    /**
     * Kiểm tra cuốn sách đã từng phát sinh trong đơn hàng chưa (chặn xóa cứng để bảo toàn lịch sử đơn).
     */
    @Query("select count(c) > 0 from ChiTietDonHang c where c.sach.id = :sachId")
    boolean existsBySachId(@Param("sachId") String sachId);
}