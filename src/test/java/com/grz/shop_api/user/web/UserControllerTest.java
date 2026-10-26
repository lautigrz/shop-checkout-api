package com.grz.shop_api.user.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.grz.shop_api.common.exception.GlobalExceptionHandler;
import com.grz.shop_api.common.exception.ResourceNotFoundException;
import com.grz.shop_api.common.security.UserPrincipal;
import com.grz.shop_api.user.application.UserService;
import com.grz.shop_api.user.domain.Role;
import com.grz.shop_api.user.web.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de Capa Web - UserController (MockMvc Standalone)")
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UUID userId;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        userId = UUID.randomUUID();
        userResponse = new UserResponse(
                userId,
                "maria@example.com",
                "Maria",
                "Lopez",
                Role.ROLE_ADMIN,
                true,
                Instant.now()
        );

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(UserPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return new UserPrincipal(userId, "maria@example.com", "pass", Role.ROLE_ADMIN, true, List.of());
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/users/me: Debe retornar los datos del usuario autenticado actual")
    void getProfile_shouldReturnCurrentUserData() throws Exception {
        when(userService.getById(userId)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("maria@example.com"))
                .andExpect(jsonPath("$.firstName").value("Maria"));

        verify(userService).getById(userId);
    }

    @Test
    @DisplayName("GET /api/v1/users/{id}: Debe retornar el usuario solicitado")
    void getById_shouldReturnUserById() throws Exception {
        when(userService.getById(userId)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/{id}", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));

        verify(userService).getById(userId);
    }

    @Test
    @DisplayName("GET /api/v1/users/{id}: Debe retornar 404 Not Found si el usuario no existe")
    void getById_shouldReturn404_whenNotFound() throws Exception {
        UUID nonExisting = UUID.randomUUID();
        when(userService.getById(nonExisting))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado con ID: " + nonExisting));

        mockMvc.perform(get("/api/v1/users/{id}", nonExisting)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Usuario no encontrado con ID: " + nonExisting));
    }

    @Test
    @DisplayName("GET /api/v1/users: Debe listar todos los usuarios")
    void getAll_shouldReturnUserList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(userResponse));

        mockMvc.perform(get("/api/v1/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].email").value("maria@example.com"));

        verify(userService).getAllUsers();
    }
}
