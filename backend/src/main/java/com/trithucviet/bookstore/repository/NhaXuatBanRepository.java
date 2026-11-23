package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.NhaXuatBan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NhaXuatBanRepository extends JpaRepository<NhaXuatBan, String> {

    @Query("select n from NhaXuatBan n order by n.tenNxb asc, n.id asc")
    List<NhaXuatBan> findAllOrdered();

    @Query("select n from NhaXuatBan n where upper(n.tenNxb) = upper(:tenNxb)")
    Optional<NhaXuatBan> findFirstByTenNxbIgnoreCase(@Param("tenNxb") String tenNxb);
}