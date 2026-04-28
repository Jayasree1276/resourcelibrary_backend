package com.example.library.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.library.model.User;
import com.example.library.repository.UserRepository;

@Service
public class UserService {

    private static final int VERIFICATION_TOKEN_HOURS = 24;
    private static final int RESET_TOKEN_MINUTES = 60;

    @Autowired
    private UserRepository repository;

    @Autowired
    private EmailService emailService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public List<User> getAllUsers() {
        return repository.findAll();
    }

    public User getUserById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public User saveUser(User user) {
        return repository.save(user);
    }

    @Transactional
    public User registerUser(User user) {
        Optional<User> existingUser = repository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Account with this email already exists.");
        }

        user.setEmailVerified(Boolean.FALSE);
        issueVerificationToken(user);

        User savedUser = repository.save(user);
        sendVerificationEmail(savedUser);
        return savedUser;
    }

    public User loginUser(String email, String password, String role) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Account does not exist. Please register first."));

        String storedRole = user.getRole() == null ? "user" : user.getRole().trim().toLowerCase();
        String requestedRole = role == null ? "user" : role.trim().toLowerCase();

        if (!user.getPassword().equals(password) || !storedRole.equals(requestedRole)) {
            throw new IllegalArgumentException("Invalid email/password or role combination.");
        }

        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("Please verify your email before logging in.");
        }

        return user;
    }

    @Transactional
    public String forgotPassword(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        repository.findByEmail(email.trim()).ifPresent(user -> {
            issueResetPasswordToken(user);
            repository.save(user);
            sendResetPasswordEmail(user);
        });

        return "If an account exists with this email, a password reset link has been sent.";
    }

    @Transactional
    public String resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Reset token is required.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required.");
        }

        User user = repository.findByResetPasswordToken(token.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token."));

        if (isExpired(user.getResetPasswordTokenExpiry())) {
            throw new IllegalArgumentException("Password reset token has expired.");
        }

        user.setPassword(newPassword);
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        repository.save(user);
        return "Password has been reset successfully.";
    }

    @Transactional
    public String verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Verification token is required.");
        }

        User user = repository.findByVerificationToken(token.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email verification token."));

        if (isExpired(user.getVerificationTokenExpiry())) {
            throw new IllegalArgumentException("Verification token has expired.");
        }

        user.setEmailVerified(Boolean.TRUE);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        repository.save(user);
        return "Email verified successfully.";
    }

    @Transactional
    public String resendVerification(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        User user = repository.findByEmail(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Account does not exist. Please register first."));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return "Email is already verified.";
        }

        issueVerificationToken(user);
        repository.save(user);
        sendVerificationEmail(user);
        return "Verification email sent successfully.";
    }

    public void deleteUser(Long id) {
        repository.deleteById(id);
    }

    private void issueVerificationToken(User user) {
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(VERIFICATION_TOKEN_HOURS));
    }

    private void issueResetPasswordToken(User user) {
        user.setResetPasswordToken(UUID.randomUUID().toString());
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(RESET_TOKEN_MINUTES));
    }

    private boolean isExpired(LocalDateTime expiresAt) {
        return expiresAt == null || expiresAt.isBefore(LocalDateTime.now());
    }

    private void sendVerificationEmail(User user) {
        String verificationLink = buildLink("/verify-email?token=", user.getVerificationToken());
        String body = "Hi " + user.getName() + ",\n\n"
                + "Please verify your email by opening the link below:\n"
                + verificationLink + "\n\n"
                + "This link expires in " + VERIFICATION_TOKEN_HOURS + " hours.";
        emailService.sendEmail(user.getEmail(), "Verify your email", body);
    }

    private void sendResetPasswordEmail(User user) {
        String resetLink = buildLink("/reset-password?token=", user.getResetPasswordToken());
        String body = "Hi " + user.getName() + ",\n\n"
                + "You can reset your password using the link below:\n"
                + resetLink + "\n\n"
                + "This link expires in " + RESET_TOKEN_MINUTES + " minutes.";
        emailService.sendEmail(user.getEmail(), "Reset your password", body);
    }

    private String buildLink(String path, String token) {
        return frontendUrl + path + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
