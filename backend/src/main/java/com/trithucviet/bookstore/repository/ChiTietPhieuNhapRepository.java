package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.ChiTietPhieuNhap;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChiTietPhieuNhapRepository extends JpaRepository<ChiTietPhieuNhap, String> {

    /**
     * Nạp hàng loạt chi tiết phiếu nhập kèm thông tin Sách để tránh N+1 query.
     */
    @Query("""
            select c from ChiTietPhieuNhap c
            join fetch c.phieuNhap
            join fetch c.sach
            where c.phieuNhap.id in :receiptIds
            order by c.id asc
            """)
    List<ChiTietPhieuNhap> findByPhieuNhapIdInWithSach(@Param("receiptIds") Collection<String> receiptIds);

    /**
     * Lấy toàn bộ chi tiết phiếu nhập sắp xếp theo ngày lập phiếu mới nhất
     * để tra cứu giá nhập gần nhất của từng đầu sách khi tính lợi nhuận gộp.
     */
    @Query("""
            select c from ChiTietPhieuNhap c
            join fetch c.phieuNhap p
            join fetch c.sach
            order by p.ngayTao desc, c.id desc
            """)
    List<ChiTietPhieuNhap> findAllWithSachOrderedByReceiptDateDesc();
}
