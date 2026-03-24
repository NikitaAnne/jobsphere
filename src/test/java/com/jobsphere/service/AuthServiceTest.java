package com.jobsphere.service;

import com.jobsphere.dto.auth.LoginRequest;
import com.jobsphere.dto.auth.RegisterRequest;
import com.jobsphere.entity.Role;
import com.jobsphere.entity.User;
import com.jobsphere.exception.BadRequestException;
import com.jobsphere.repository.RoleRepository;
import com.jobsphere.repository.UserRepository;
import com.jobsphere.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private Role candidateRole;

    @BeforeEach
    void setUp() {
        candidateRole = Role.builder()
                .id(1L)
                .name(Role.RoleName.ROLE_CANDIDATE)
                .build();
    }

    @Test
    @DisplayName("Should register a new candidate successfully")
    void register_shouldSucceed_whenEmailIsNew() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setRole("CANDIDATE");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(Role.RoleName.ROLE_CANDIDATE)).thenReturn(Optional.of(candidateRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u = User.builder().id(1L).email(u.getEmail()).fullName(u.getFullName())
                    .password(u.getPassword()).roles(Set.of(candidateRole)).build();
            return u;
        });

        var springUser = org.springframework.security.core.userdetails.User
                .withUsername("john@example.com").password("encoded_pass")
                .roles("CANDIDATE").build();
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(springUser);
        when(jwtUtil.generateAccessToken(any())).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh_token");

        // When
        var response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when email already exists")
    void register_shouldThrow_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setRole("CANDIDATE");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_shouldSucceed_withValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        User user = User.builder().id(1L).email("john@example.com")
                .fullName("John Doe").password("encoded")
                .roles(Set.of(candidateRole)).build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        var springUser = org.springframework.security.core.userdetails.User
                .withUsername("john@example.com").password("encoded").roles("CANDIDATE").build();
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(springUser);
        when(jwtUtil.generateAccessToken(any())).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh_token");

        var response = authService.login(request);

        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getAccessToken()).isNotBlank();
    }

    @Test
    @DisplayName("Should throw when login fails due to bad credentials")
    void login_shouldThrow_withInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
