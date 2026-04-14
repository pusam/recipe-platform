package com.recipeplatform.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "favorite_recipe",
        uniqueConstraints = @UniqueConstraint(name = "uk_fav_user_recipe", columnNames = {"user_id", "recipe_id"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FavoriteRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
