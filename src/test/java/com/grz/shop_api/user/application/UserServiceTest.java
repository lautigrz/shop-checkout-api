package com.grz.shop_api.user.application;

import com.grz.shop_api.common.exception.ResourceNotFoundException;
import com.grz.shop_api.user.domain.Role;
import com.grz.shop_api.user.domain.User;
import com.grz.shop_api.user.infrastructure.UserRepository;
import com.grz.shop_api.user.web.UserMapper;
import com.grz.shop_api.user.web.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitarios - Servicio de Gestión de Usuarios (UserService)")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User sampleUser;
    private UserResponse sampleResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .email("lucas@example.com")
                .password("hashed")
                .firstName("Lucas")
                .lastName("Gomez")
                .role(Role.ROLE_CUSTOMER)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sampleResponse = new UserResponse(
                userId,
                "lucas@example.com",
                "Lucas",
                "Gomez",
                Role.ROLE_CUSTOMER,
                true,
                Instant.now()
        );
    }

    @Test
    @DisplayName("getById: Debe retornar UserResponse si el usuario existe")
    void getById_shouldReturnUserResponse_whenFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userMapper.toResponse(sampleUser)).thenReturn(sampleResponse);

        UserResponse result = userService.getById(userId);

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("lucas@example.com");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("getById: Debe lanzar ResourceNotFoundException si el usuario no existe")
    void getById_shouldThrowResourceNotFoundException_whenNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado con ID");
    }

    @Test
    @DisplayName("findEntityByEmail: Debe retornar User cuando existe el email")
    void findEntityByEmail_shouldReturnUser_whenFound() {
        when(userRepository.findByEmail("lucas@example.com")).thenReturn(Optional.of(sampleUser));

        User user = userService.findEntityByEmail("lucas@example.com");

        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("lucas@example.com");
        verify(userRepository).findByEmail("lucas@example.com");
    }

    @Test
    @DisplayName("findEntityByEmail: Debe lanzar ResourceNotFoundException si el email no existe")
    void findEntityByEmail_shouldThrowResourceNotFoundException_whenNotFound() {
        when(userRepository.findByEmail("inexistente@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findEntityByEmail("inexistente@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado con email");
    }

    @Test
    @DisplayName("getAllUsers: Debe retornar la lista de todos los usuarios mapeados")
    void getAllUsers_shouldReturnUserResponseList() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));
        when(userMapper.toResponse(sampleUser)).thenReturn(sampleResponse);

        List<UserResponse> list = userService.getAllUsers();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).firstName()).isEqualTo("Lucas");
        verify(userRepository).findAll();
    }
}
