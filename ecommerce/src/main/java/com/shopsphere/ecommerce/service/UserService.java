package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.admin.AdminRequest;
import com.shopsphere.ecommerce.dto.auth.LoginRequest;
import com.shopsphere.ecommerce.dto.auth.LoginResponse;
import com.shopsphere.ecommerce.dto.auth.RegisterRequest;
import com.shopsphere.ecommerce.dto.auth.UserResponse;
import com.shopsphere.ecommerce.entity.Role;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.exception.DuplicateCategoryException;
import com.shopsphere.ecommerce.exception.InvalidCredentialsException;
import com.shopsphere.ecommerce.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateCategoryException("Email already registered");
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        String hashedPassword =
                passwordEncoder.encode(request.getPassword());

        user.setPassword(hashedPassword);
        user.setRole(Role.CUSTOMER);
        User savedUser = userRepository.save(user);


        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Invalid email or password"
                ));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        return new LoginResponse(
                token,
                userResponse
        );
    }


    public UserResponse createAdmin(AdminRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateCategoryException("Email already registered");
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        String hashedPassword =
                passwordEncoder.encode(request.getPassword());

        user.setPassword(hashedPassword);

        user.setRole(Role.ADMIN);
        user.setActive(true);

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getRole()
        );
    }
}