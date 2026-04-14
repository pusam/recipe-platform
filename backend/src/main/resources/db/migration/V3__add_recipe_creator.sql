ALTER TABLE recipe ADD COLUMN creator_id BIGINT NULL;
ALTER TABLE recipe ADD CONSTRAINT fk_recipe_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_recipe_creator ON recipe(creator_id);
