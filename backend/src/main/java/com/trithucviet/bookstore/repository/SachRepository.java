package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.Sach;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SachRepository extends JpaRepository<Sach, String> {

    /**
     * Tải toàn bộ sách đang hiển thị (hienThi = true) kèm Danh mục (và Danh mục cha), Tác giả, Nhà xuất bản
     * trong 1 câu truy vấn duy nhất để phục vụ tìm kiếm, lọc đa tiêu chí, sắp xếp theo giá thực tế / % giảm giá
     * và đếm số lượng sách theo cây danh mục mà không phát sinh lỗi N+1.
     */
    @Query("""
            select s from Sach s
            join fetch s.danhMuc dm
            left join fetch dm.danhMucCha
            join fetch s.tacGia
            join fetch s.nhaXuatBan
            where s.hienThi = true
            """)
    List<Sach> findAllVisibleWithDetails();

    /**
     * Tìm chi tiết 1 cuốn sách đang hiển thị kèm đầy đủ Danh mục, Tác giả, NXB.
     * Ghi tường minh @Param("id") để chạy ổn định khi nhấn Run trực tiếp từ IDE.
     */
    @Query("""
            select s from Sach s
            join fetch s.danhMuc dm
            left join fetch dm.danhMucCha
            join fetch s.tacGia
            join fetch s.nhaXuatBan
            where s.id = :id and s.hienThi = true
            """)
    Optional<Sach> findVisibleByIdWithDetails(@Param("id") String id);

    /**
     * Tải toàn bộ sách cho Web Admin (bao gồm cả sách đang ẩn hienThi = false).
     */
    @Query("""
            select s from Sach s
            join fetch s.danhMuc dm
            left join fetch dm.danhMucCha
            join fetch s.tacGia
            join fetch s.nhaXuatBan
            order by s.ngayTao desc, s.id asc
            """)
    List<Sach> findAllWithDetails();

    /**
     * Tìm chi tiết 1 cuốn sách theo ID cho Web Admin (không lọc theo hienThi).
     */
    @Query("""
            select s from Sach s
            join fetch s.danhMuc dm
            left join fetch dm.danhMucCha
            join fetch s.tacGia
            join fetch s.nhaXuatBan
            where s.id = :id
            """)
    Optional<Sach> findByIdWithDetails(@Param("id") String id);

    @Query("select count(s) > 0 from Sach s where upper(s.maIsbn) = upper(:maIsbn)")
    boolean existsByMaIsbnIgnoreCase(@Param("maIsbn") String maIsbn);

    @Query("select count(s) > 0 from Sach s where upper(s.maIsbn) = upper(:maIsbn) and s.id <> :id")
    boolean existsByMaIsbnIgnoreCaseAndIdNot(@Param("maIsbn") String maIsbn, @Param("id") String id);

    @Query("select count(s) > 0 from Sach s where upper(s.duongDan) = upper(:duongDan)")
    boolean existsByDuongDanIgnoreCase(@Param("duongDan") String duongDan);

    @Query("select count(s) > 0 from Sach s where upper(s.duongDan) = upper(:duongDan) and s.id <> :id")
    boolean existsByDuongDanIgnoreCaseAndIdNot(@Param("duongDan") String duongDan, @Param("id") String id);

    @Query("select count(s) > 0 from Sach s where s.danhMuc.id = :danhMucId")
    boolean existsByDanhMucId(@Param("danhMucId") String danhMucId);

    @Query("select count(s) > 0 from Sach s where s.tacGia.id = :tacGiaId")
    boolean existsByTacGiaId(@Param("tacGiaId") String tacGiaId);

    @Query("select count(s) > 0 from Sach s where s.nhaXuatBan.id = :nhaXuatBanId")
    boolean existsByNhaXuatBanId(@Param("nhaXuatBanId") String nhaXuatBanId);
}