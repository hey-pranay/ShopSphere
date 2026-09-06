package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.auth.LoginRequest;
import com.shopsphere.ecommerce.dto.auth.LoginResponse;
import com.shopsphere.ecommerce.dto.auth.RegisterRequest;
import com.shopsphere.ecommerce.dto.auth.UserResponse;
import com.shopsphere.ecommerce.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(
            UserService userService
    ) {
        this.userService = userService;
    }


    @PostMapping("/register")
    public UserResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return userService.login(request);
    }

}