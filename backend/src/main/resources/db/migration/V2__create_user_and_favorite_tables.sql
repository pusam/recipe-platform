CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(190) NOT NULL,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE favorite_recipe (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_fav_recipe FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE,
    CONSTRAINT uk_fav_user_recipe UNIQUE (user_id, recipe_id)
);

CREATE INDEX idx_fav_user ON favorite_recipe(user_id);
CREATE INDEX idx_fav_recipe ON favorite_recipe(recipe_id);
