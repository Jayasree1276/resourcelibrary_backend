package com.example.library.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.library.model.User;
import com.example.library.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    public List<User> getAllUsers() {
        return repository.findAll();
    }

    public User getUserById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public User saveUser(User user) {
        return repository.save(user);
    }

    public User registerUser(User user) {
        Optional<User> existingUser = repository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Account with this email already exists.");
        }
        return repository.save(user);
    }

    public User loginUser(String email, String password, String role) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Account does not exist. Please register first."));

        String storedRole = user.getRole() == null ? "user" : user.getRole().trim().toLowerCase();
        String requestedRole = role == null ? "user" : role.trim().toLowerCase();

        if (!user.getPassword().equals(password) || !storedRole.equals(requestedRole)) {
            throw new IllegalArgumentException("Invalid email/password or role combination.");
        }

        return user;
    }

    public void deleteUser(Long id) {
        repository.deleteById(id);
    }
}
