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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Ya existe una cuenta registrada con el email: " + request.email());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        if (user.getRole() == null) {
            user.setRole(Role.ROLE_CUSTOMER);
        }

        User savedUser = userRepository.save(user);
        UserPrincipal principal = UserPrincipal.create(savedUser);
        String token = jwtService.generateToken(principal);

        return AuthResponse.of(token, userMapper.toResponse(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no existe en la base de datos"));

        return AuthResponse.of(token, userMapper.toResponse(user));
    }
}
