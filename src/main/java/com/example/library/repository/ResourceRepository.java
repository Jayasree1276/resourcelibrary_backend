package com.example.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.library.model.Resource;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}