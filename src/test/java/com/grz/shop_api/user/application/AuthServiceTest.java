package com.grz.shop_api.user.application;

import com.grz.shop_api.common.exception.ConflictException;
import com.grz.shop_api.common.security.JwtService;
import com.grz.shop_api.common.security.UserPrincipal;
import com.grz.shop_api.user.domain.Role;
import com.grz.shop_api.user.domain.User;
import com.grz.shop_api.user.infrastructure.UserRepository;
import com.grz.shop_api.user.web.UserMapper;
import com.grz.shop_api.user.web.dto.AuthResponse;
import com.grz.shop_api.user.web.dto.LoginRequest;
import com.grz.shop_api.user.web.dto.RegisterRequest;
import com.grz.shop_api.user.web.dto.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitarios - Servicio de Autenticación y Usuarios")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Registro: Debe encriptar contraseña, guardar usuario y retornar token JWT")
    void register_shouldCreateUserAndReturnToken_whenEmailIsAvailable() {
        // GIVEN:
        RegisterRequest request = new RegisterRequest("nuevo@shop.com", "pass123", "Ana", "Gomez", Role.ROLE_CUSTOMER);
        User entity = User.builder().email("nuevo@shop.com").build();
        User savedEntity = User.builder().id(UUID.randomUUID()).email("nuevo@shop.com").role(Role.ROLE_CUSTOMER).active(true).build();

        when(userRepository.existsByEmail("nuevo@shop.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedEntity);
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("jwt.token.mock");
        when(userMapper.toResponse(savedEntity)).thenReturn(mock(UserResponse.class));

        // WHEN:
        AuthResponse response = authService.register(request);

        // THEN:
        assertNotNull(response);
        assertEquals("jwt.token.mock", response.accessToken());
        verify(passwordEncoder).encode("pass123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Registro: Debe arrojar ConflictException si el email ya existe")
    void register_shouldThrowConflictException_whenEmailAlreadyExists() {
        // GIVEN:
        RegisterRequest request = new RegisterRequest("existente@shop.com", "pass123", "Juan", "Perez", Role.ROLE_CUSTOMER);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // WHEN & THEN:
        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login: Debe autenticar credenciales y generar nuevo token JWT")
    void login_shouldAuthenticateAndReturnToken_whenCredentialsAreValid() {
        // GIVEN:
        LoginRequest request = new LoginRequest("user@shop.com", "pass123");
        User user = User.builder().id(UUID.randomUUID()).email("user@shop.com").role(Role.ROLE_CUSTOMER).build();
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(principal);
        when(jwtService.generateToken(principal)).thenReturn("jwt.token.login");
        when(userRepository.findByEmail("user@shop.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(mock(UserResponse.class));

        // WHEN:
        AuthResponse response = authService.login(request);

        // THEN:
        assertEquals("jwt.token.login", response.accessToken());
    }
}
