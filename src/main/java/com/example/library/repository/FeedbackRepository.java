package com.example.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.library.model.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
