CREATE TABLE recipe_rating (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    score TINYINT NOT NULL,
    comment VARCHAR(2000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rating_recipe FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE,
    CONSTRAINT fk_rating_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_rating_user_recipe UNIQUE (user_id, recipe_id),
    CONSTRAINT ck_rating_score CHECK (score BETWEEN 1 AND 5)
);

CREATE INDEX idx_rating_recipe ON recipe_rating(recipe_id);
CREATE INDEX idx_rating_user ON recipe_rating(user_id);

-- recipe 테이블에 집계 캐시 추가 (평균/개수)
ALTER TABLE recipe ADD COLUMN avg_score DECIMAL(3,2);
ALTER TABLE recipe ADD COLUMN rating_count INT NOT NULL DEFAULT 0;

-- 평균 점수 기준 정렬용 인덱스 (추후 "인기순" 정렬에 활용)
CREATE INDEX idx_recipe_avg_score ON recipe(avg_score);
