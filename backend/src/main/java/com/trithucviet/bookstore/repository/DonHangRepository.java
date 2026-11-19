package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.DonHang;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonHangRepository extends JpaRepository<DonHang, String> {
}