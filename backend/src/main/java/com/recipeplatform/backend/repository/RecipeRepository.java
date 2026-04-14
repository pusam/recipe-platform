package com.recipeplatform.backend.repository;

import com.recipeplatform.backend.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    Optional<Recipe> findFirstByVideoIdOrderByIdDesc(String videoId);

    @Query("""
           SELECT r FROM Recipe r
           WHERE (:q IS NULL OR :q = '' OR LOWER(r.title) LIKE LOWER(CONCAT('%', :q, '%')))
             AND (:tag IS NULL OR :tag = '' OR LOWER(r.tags) LIKE LOWER(CONCAT('%', :tag, '%')))
           ORDER BY r.createdAt DESC
           """)
    Page<Recipe> search(@Param("q") String q, @Param("tag") String tag, Pageable pageable);
}
