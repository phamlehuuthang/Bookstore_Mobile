package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.SachYeuThich;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SachYeuThichRepository extends JpaRepository<SachYeuThich, SachYeuThich.SachYeuThichId> {

    /**
     * Lấy toàn bộ tủ sách yêu thích của 1 khách hàng.
     * Dùng JOIN FETCH để tải luôn sach + tacGia + danhMuc trong 1 câu SQL duy nhất:
     * - Tránh lỗi LazyInitializationException khi mapping sang DTO sau khi đóng session JPA.
     * - Tránh lỗi truy vấn N+1 (với mỗi cuốn sách lại query thêm 2 câu lấy tác giả & danh mục).
     * - Sắp xếp theo ngayTao giảm dần để sách khách vừa thả tim mới nhất luôn hiển thị lên đầu.
     */
    @Query("""
            select y from SachYeuThich y
            join fetch y.sach s
            join fetch s.tacGia
            join fetch s.danhMuc
            where y.id.nguoiDungId = :nguoiDungId
            order by y.ngayTao desc
            """)
    List<SachYeuThich> findAllWithSachByNguoiDungId(@Param("nguoiDungId") String nguoiDungId);

    /**
     * Đếm trực tiếp số lượng sách đang yêu thích trong DB (SELECT COUNT),
     * dùng để trả về tổng số hiển thị lên badge "Yêu thích (N)" trên app mà không cần tải hết danh sách.
     */
    @Query("select count(y) from SachYeuThich y where y.id.nguoiDungId = :nguoiDungId")
    long countByNguoiDungId(@Param("nguoiDungId") String nguoiDungId);
}
