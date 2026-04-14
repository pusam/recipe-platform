CREATE TABLE recipe (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    youtube_url VARCHAR(500) NOT NULL,
    video_id VARCHAR(64),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(500),
    channel_name VARCHAR(255),
    summary TEXT,
    servings VARCHAR(100),
    cooking_time VARCHAR(100),
    difficulty VARCHAR(50),
    tags VARCHAR(500),
    raw_transcript LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ingredient (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount VARCHAR(100),
    sort_order INT DEFAULT 0,
    CONSTRAINT fk_ingredient_recipe FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
);

CREATE TABLE recipe_step (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    step_no INT NOT NULL,
    instruction TEXT NOT NULL,
    tip TEXT,
    CONSTRAINT fk_step_recipe FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
);

CREATE INDEX idx_recipe_created_at ON recipe(created_at);
CREATE INDEX idx_ingredient_recipe ON ingredient(recipe_id);
CREATE INDEX idx_step_recipe ON recipe_step(recipe_id);
