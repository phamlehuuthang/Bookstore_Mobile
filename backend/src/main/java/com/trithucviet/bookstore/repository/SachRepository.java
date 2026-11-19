package com.trithucviet.bookstore.repository;

import com.trithucviet.bookstore.entity.Sach;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SachRepository extends JpaRepository<Sach, String> {
}