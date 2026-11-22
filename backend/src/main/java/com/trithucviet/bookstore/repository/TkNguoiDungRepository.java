package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.TkNguoiDung;
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
}