package com.grz.shop_api.user.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.grz.shop_api.common.exception.ConflictException;
import com.grz.shop_api.common.exception.GlobalExceptionHandler;
import com.grz.shop_api.user.application.AuthService;
import com.grz.shop_api.user.domain.Role;
import com.grz.shop_api.user.web.dto.AuthResponse;
import com.grz.shop_api.user.web.dto.LoginRequest;
import com.grz.shop_api.user.web.dto.RegisterRequest;
import com.grz.shop_api.user.web.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de Capa Web - AuthController (MockMvc Standalone)")
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private UserResponse userResponse;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        userResponse = new UserResponse(
                UUID.randomUUID(),
                "juan@example.com",
                "Juan",
                "Perez",
                Role.ROLE_CUSTOMER,
                true,
                Instant.now()
        );

        authResponse = AuthResponse.of("mocked-jwt-token-xyz", userResponse);
    }

    @Test
    @DisplayName("POST /api/v1/auth/register: Debe retornar 201 Created cuando el registro es exitoso")
    void register_shouldReturn201Created_whenValid() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "juan@example.com",
                "password123",
                "Juan",
                "Perez",
                Role.ROLE_CUSTOMER
        );

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mocked-jwt-token-xyz"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("juan@example.com"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register: Debe retornar 400 Bad Request cuando los datos son inválidos")
    void register_shouldReturn400_whenValidationFails() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "invalid-email",
                "123", // Too short (min 6)
                "",
                "",
                null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register: Debe retornar 409 Conflict si el email ya existe")
    void register_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "existing@example.com",
                "password123",
                "Juan",
                "Perez",
                Role.ROLE_CUSTOMER
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ConflictException("El email ya se encuentra en uso: existing@example.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El email ya se encuentra en uso: existing@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login: Debe retornar 200 OK cuando las credenciales son válidas")
    void login_shouldReturn200Ok_whenCredentialsValid() throws Exception {
        LoginRequest request = new LoginRequest("juan@example.com", "password123");

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mocked-jwt-token-xyz"))
                .andExpect(jsonPath("$.user.email").value("juan@example.com"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login: Debe retornar 401 Unauthorized cuando las credenciales son incorrectas")
    void login_shouldReturn401_whenCredentialsInvalid() throws Exception {
        LoginRequest request = new LoginRequest("juan@example.com", "wrong-password");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("El correo o la contraseña son incorrectos"));
    }
}
