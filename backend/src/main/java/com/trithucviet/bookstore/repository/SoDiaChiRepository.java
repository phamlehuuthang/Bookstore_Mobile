package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.SoDiaChi;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SoDiaChiRepository extends JpaRepository<SoDiaChi, String> {

    /**
     * Lấy toàn bộ sổ địa chỉ của 1 khách hàng, luôn ưu tiên đưa địa chỉ mặc định lên đầu danh sách.
     * Dùng @Query + @Param tường minh để chạy ổn định khi nhấn Run trực tiếp từ IDE.
     */
    @Query("""
            select d from SoDiaChi d
            where d.nguoiDung.id = :nguoiDungId
            order by d.macDinh desc, d.id asc
            """)
    List<SoDiaChi> findByNguoiDungIdOrderByMacDinhDescIdAsc(@Param("nguoiDungId") String nguoiDungId);

    /**
     * Tìm 1 địa chỉ kèm điều kiện khớp nguoiDungId nhằm chống lỗi bảo mật IDOR (sửa/xóa nhầm địa chỉ người khác).
     */
    @Query("""
            select d from SoDiaChi d
            where d.id = :id and d.nguoiDung.id = :nguoiDungId
            """)
    Optional<SoDiaChi> findByIdAndNguoiDungId(
            @Param("id") String id,
            @Param("nguoiDungId") String nguoiDungId
    );

    /**
     * Đếm số địa chỉ hiện có của khách hàng.
     */
    @Query("select count(d) from SoDiaChi d where d.nguoiDung.id = :nguoiDungId")
    long countByNguoiDungId(@Param("nguoiDungId") String nguoiDungId);
}