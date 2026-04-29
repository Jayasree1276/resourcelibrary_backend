package com.example.library.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.library.model.User;
import com.example.library.repository.UserRepository;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final int VERIFICATION_TOKEN_HOURS = 24;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

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
        if (user == null) {
            throw new IllegalArgumentException("Registration details are required.");
        }

        normalizeRegistrationInput(user);
        validateRegistrationInput(user);

        try {
            Optional<User> existingUser = repository.findByEmail(user.getEmail());
            if (existingUser.isPresent()) {
                throw new IllegalArgumentException("Account with this email already exists.");
            }
        } catch (DataAccessException exception) {
            logger.error("Failed to check for existing account during registration for {}", user.getEmail(), exception);
            throw new IllegalStateException("Registration is temporarily unavailable. Please try again shortly.");
        }

        user.setEmailVerified(Boolean.FALSE);
        issueVerificationToken(user);

        User savedUser;
        try {
            savedUser = repository.save(user);
        } catch (DataIntegrityViolationException exception) {
            logger.warn("Registration rejected by database constraints for {}", user.getEmail(), exception);
            throw new IllegalArgumentException("Unable to register account. Please check the submitted details.");
        } catch (DataAccessException exception) {
            logger.error("Database error while registering {}", user.getEmail(), exception);
            throw new IllegalStateException("Registration is temporarily unavailable. Please try again shortly.");
        }

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

        return user;
    }

    @Transactional
    public String forgotPassword(String email) {
        validateEmail(email);
        repository.findByEmail(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Account does not exist. Please register first."));
        return "Account found. You can set a new password now.";
    }

    @Transactional
    public String resetPassword(String email, String token, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required.");
        }

        User user;

        if (email != null && !email.isBlank()) {
            validateEmail(email);
            user = repository.findByEmail(email.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Account does not exist. Please register first."));
        } else if (token != null && !token.isBlank()) {
            user = repository.findByResetPasswordToken(token.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token."));

            if (isExpired(user.getResetPasswordTokenExpiry())) {
                throw new IllegalArgumentException("Password reset token has expired.");
            }
        } else {
            throw new IllegalArgumentException("Email is required to reset the password.");
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

    private boolean isExpired(LocalDateTime expiresAt) {
        return expiresAt == null || expiresAt.isBefore(LocalDateTime.now());
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
    }

    private void validateRegistrationInput(User user) {
        validateEmail(user.getEmail());

        if (user.getName() == null || user.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
    }

    private void normalizeRegistrationInput(User user) {
        user.setName(user.getName() == null ? null : user.getName().trim());
        user.setEmail(normalizeEmail(user.getEmail()));
        user.setPassword(user.getPassword() == null ? null : user.getPassword().trim());

        String normalizedRole = user.getRole() == null ? "" : user.getRole().trim().toLowerCase();
        user.setRole(normalizedRole.isEmpty() ? "user" : normalizedRole);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private void sendVerificationEmail(User user) {
        String verificationLink = buildLink("/verify-email?token=", user.getVerificationToken());
        String body = "Hi " + user.getName() + ",\n\n"
                + "Please verify your email by opening the link below:\n"
                + verificationLink + "\n\n"
                + "This link expires in " + VERIFICATION_TOKEN_HOURS + " hours.";
        emailService.sendEmail(user.getEmail(), "Verify your email", body);
    }

    private String buildLink(String path, String token) {
        return frontendUrl + path + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
