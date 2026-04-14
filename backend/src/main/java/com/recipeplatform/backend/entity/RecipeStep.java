package com.recipeplatform.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recipe_step")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RecipeStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    @JsonBackReference
    private Recipe recipe;

    @Column(name = "step_no", nullable = false)
    private Integer stepNo;

    @Lob
    @Column(nullable = false)
    private String instruction;

    @Lob
    private String tip;
}
