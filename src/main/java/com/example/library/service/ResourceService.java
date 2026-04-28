package com.example.library.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.library.model.Resource;
import com.example.library.repository.ResourceRepository;

@Service
public class ResourceService {
	@Autowired
    private ResourceRepository repository;

    // Get all resources
    public List<Resource> getAllResources() {
        return repository.findAll();
    }

    // Save resource
    public Resource saveResource(Resource resource) {
        return repository.save(resource);
    }

    // Delete resource
    public void deleteResource(Long id) {
        repository.deleteById(id);
    }

    public Resource getResourceById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Optional<Resource> findById(Long id) {
        return repository.findById(id);
    }
}
