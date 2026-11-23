package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.TkNguoiDung;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TkNguoiDungRepository extends JpaRepository<TkNguoiDung, String> {

    /**
     * Tìm tài khoản theo email (không phân biệt chữ hoa/thường) phục vụ Đăng nhập và kiểm tra trùng Email.
     * Dùng @Query + @Param("email") tường minh để chạy ổn định cả khi nhấn Run trong IDE.
     */
    @Query("select u from TkNguoiDung u where upper(u.email) = upper(:email)")
    Optional<TkNguoiDung> findByEmailIgnoreCase(@Param("email") String email);

    /**
     * Kiểm tra email đã tồn tại trong bảng tk_nguoi_dung hay chưa trước khi tạo tài khoản mới.
     */
    @Query("select count(u) > 0 from TkNguoiDung u where upper(u.email) = upper(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    /**
     * Lấy danh sách tài khoản theo vai trò (dùng cho tab Khách hàng trên Web Admin),
     * sắp xếp tài khoản mới tạo lên trước.
     */
    @Query("select u from TkNguoiDung u where u.vaiTro = :vaiTro order by u.ngayTao desc, u.id asc")
    List<TkNguoiDung> findByVaiTroOrdered(@Param("vaiTro") VaiTro vaiTro);

    /**
     * Lấy toàn bộ danh sách nhân sự nội bộ (ADMIN, WAREHOUSE_MANAGER, CSKH), loại trừ CUSTOMER.
     */
    @Query("""
            select u from TkNguoiDung u
            where u.vaiTro <> com.trithucviet.bookstore.entity.enums.VaiTro.CUSTOMER
            order by u.ngayTao desc, u.id asc
            """)
    List<TkNguoiDung> findStaffOrdered();
}