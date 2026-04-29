package com.example.library.controller;

import com.example.library.model.AuthMessageResponse;
import com.example.library.model.EmailRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.library.model.LoginRequest;
import com.example.library.model.ResetPasswordRequest;
import com.example.library.model.TokenRequest;
import com.example.library.model.User;
import com.example.library.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        return userService.registerUser(user);
    }

    @PostMapping("/login")
    public User login(@RequestBody LoginRequest request) {
        return userService.loginUser(request.getEmail(), request.getPassword(), request.getRole());
    }

    @PostMapping("/forgot-password")
    public AuthMessageResponse forgotPassword(@RequestBody EmailRequest request) {
        return new AuthMessageResponse(userService.forgotPassword(request.getEmail()));
    }

    @PostMapping("/reset-password")
    public AuthMessageResponse resetPassword(@RequestBody ResetPasswordRequest request) {
        return new AuthMessageResponse(userService.resetPassword(
                request.getEmail(),
                request.getToken(),
                request.getNewPassword()));
    }

    @PostMapping("/verify-email")
    public AuthMessageResponse verifyEmail(@RequestBody TokenRequest request) {
        return new AuthMessageResponse(userService.verifyEmail(request.getToken()));
    }

    @PostMapping("/resend-verification")
    public AuthMessageResponse resendVerification(@RequestBody EmailRequest request) {
        return new AuthMessageResponse(userService.resendVerification(request.getEmail()));
    }
}
