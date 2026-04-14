package com.recipeplatform.backend.security;

import com.recipeplatform.backend.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserPrincipal {
    private final User user;

    public Long getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }
}
