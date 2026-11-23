package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.TacGia;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TacGiaRepository extends JpaRepository<TacGia, String> {

    @Query("select t from TacGia t order by t.tenTacGia asc, t.id asc")
    List<TacGia> findAllOrdered();

    @Query("select t from TacGia t where upper(t.tenTacGia) = upper(:tenTacGia)")
    Optional<TacGia> findFirstByTenTacGiaIgnoreCase(@Param("tenTacGia") String tenTacGia);
}