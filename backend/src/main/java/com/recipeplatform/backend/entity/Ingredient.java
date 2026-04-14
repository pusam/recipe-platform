package com.recipeplatform.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ingredient")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    @JsonBackReference
    private Recipe recipe;

    @Column(nullable = false)
    private String name;

    private String amount;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
