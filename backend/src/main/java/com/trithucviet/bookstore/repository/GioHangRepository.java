package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.GioHang;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GioHangRepository extends JpaRepository<GioHang, GioHang.GioHangId> {
}