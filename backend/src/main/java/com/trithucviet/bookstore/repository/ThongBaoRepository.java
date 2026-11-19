package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.ThongBao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThongBaoRepository extends JpaRepository<ThongBao, String> {
}