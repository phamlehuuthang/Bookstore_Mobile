package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.DanhGia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DanhGiaRepository extends JpaRepository<DanhGia, String> {
}