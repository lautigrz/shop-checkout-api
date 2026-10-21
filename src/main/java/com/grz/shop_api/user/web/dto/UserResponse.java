package com.grz.shop_api.user.web.dto;

import com.grz.shop_api.user.domain.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Role role,
        boolean active,
        Instant createdAt
) {}
