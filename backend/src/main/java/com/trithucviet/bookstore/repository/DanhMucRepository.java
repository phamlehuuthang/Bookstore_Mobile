package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.DanhMuc;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DanhMucRepository extends JpaRepository<DanhMuc, String> {
}