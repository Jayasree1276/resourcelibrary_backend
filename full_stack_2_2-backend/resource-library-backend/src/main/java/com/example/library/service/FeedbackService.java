package com.example.library.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.library.model.Feedback;
import com.example.library.repository.FeedbackRepository;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository repository;

    public List<Feedback> getAllFeedback() {
        return repository.findAll();
    }

    public Feedback saveFeedback(Feedback feedback) {
        return repository.save(feedback);
    }
}
