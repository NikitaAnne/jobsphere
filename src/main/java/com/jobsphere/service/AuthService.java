package com.jobsphere.service;

import com.jobsphere.dto.auth.AuthResponse;
import com.jobsphere.dto.auth.LoginRequest;
import com.jobsphere.dto.auth.RegisterRequest;
import com.jobsphere.entity.Role;
import com.jobsphere.entity.User;
import com.jobsphere.exception.BadRequestException;
import com.jobsphere.repository.RoleRepository;
import com.jobsphere.repository.UserRepository;
import com.jobsphere.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }

        // Map requested role string → Role entity
        String roleStr = "ROLE_" + request.getRole().toUpperCase();
        Role.RoleName roleName;
        try {
            roleName = Role.RoleName.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + request.getRole() + ". Must be RECRUITER or CANDIDATE.");
        }

        if (roleName == Role.RoleName.ROLE_ADMIN) {
            throw new BadRequestException("Admin accounts cannot be created via registration.");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BadRequestException("Role not configured: " + roleName));

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .location(request.getLocation())
                .roles(Set.of(role))
                .build();

        userRepository.save(user);
        log.info("Registered new user: {} with role: {}", user.getEmail(), roleName);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return buildAuthResponse(user, userDetails);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // This throws AuthenticationException if credentials are wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user, userDetails);
    }

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!jwtUtil.isTokenValid(refreshToken, userDetails)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        return buildAuthResponse(user, userDetails);
    }

    private AuthResponse buildAuthResponse(User user, UserDetails userDetails) {
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .build();
    }
}
