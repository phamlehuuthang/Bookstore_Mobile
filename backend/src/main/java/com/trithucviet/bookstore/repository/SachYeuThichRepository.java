package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.SachYeuThich;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SachYeuThichRepository extends JpaRepository<SachYeuThich, SachYeuThich.SachYeuThichId> {
}