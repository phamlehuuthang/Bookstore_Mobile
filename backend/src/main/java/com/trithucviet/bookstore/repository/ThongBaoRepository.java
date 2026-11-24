package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.ThongBao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ThongBaoRepository extends JpaRepository<ThongBao, String> {

    /**
     * Lấy toàn bộ thông báo của khách hàng: gồm thông báo cá nhân (nguoiDung.id = :nguoiDungId)
     * và các thông báo chung cũ (nguoiDung IS NULL), mới nhất lên đầu.
     */
    @Query("""
            select t from ThongBao t
            left join fetch t.nguoiDung
            where t.nguoiDung.id = :nguoiDungId or t.nguoiDung is null
            order by t.ngayTao desc, t.id desc
            """)
    List<ThongBao> findForCustomerOrdered(@Param("nguoiDungId") String nguoiDungId);

    @Query("""
            select t from ThongBao t
            left join fetch t.nguoiDung
            where t.id = :id and t.nguoiDung.id = :nguoiDungId
            """)
    Optional<ThongBao> findByIdAndNguoiDungId(
            @Param("id") String id,
            @Param("nguoiDungId") String nguoiDungId
    );

    /**
     * Đánh dấu đã đọc toàn bộ thông báo riêng của khách hàng trong 1 câu UPDATE (không chạm tới dòng chung nguoiDung IS NULL).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ThongBao t
            set t.daDoc = true
            where t.nguoiDung.id = :nguoiDungId and t.daDoc = false
            """)
    int markAllReadByNguoiDungId(@Param("nguoiDungId") String nguoiDungId);
}