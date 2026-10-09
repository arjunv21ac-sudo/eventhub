package com.eventhub.service;

import com.eventhub.dto.AuthDtos.AuthResponse;
import com.eventhub.dto.AuthDtos.LoginRequest;
import com.eventhub.dto.AuthDtos.RegisterRequest;
import com.eventhub.entity.Role;
import com.eventhub.entity.User;
import com.eventhub.exception.ConflictException;
import com.eventhub.exception.ResourceNotFoundException;
import com.eventhub.repository.UserRepository;
import com.eventhub.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role() == null ? Role.USER : request.role());
        userRepository.save(user);

        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        // Checks the BCrypt hash; throws BadCredentialsException (-> 401) if wrong
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), user.getName(), user.getEmail(), user.getRole());
    }
}
