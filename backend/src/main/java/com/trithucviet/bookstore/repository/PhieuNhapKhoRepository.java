package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.PhieuNhapKho;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhieuNhapKhoRepository extends JpaRepository<PhieuNhapKho, String> {

    /**
     * Lấy toàn bộ phiếu nhập kho theo thứ tự mới nhất lên đầu.
     */
    @Query("select p from PhieuNhapKho p order by p.ngayTao desc, p.id desc")
    List<PhieuNhapKho> findAllOrdered();

    @Query("select count(p) > 0 from PhieuNhapKho p where upper(p.maPhieu) = upper(:maPhieu)")
    boolean existsByMaPhieuIgnoreCase(@Param("maPhieu") String maPhieu);
}
