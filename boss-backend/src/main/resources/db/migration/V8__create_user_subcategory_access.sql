CREATE TABLE user_subcategories (
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    subcategory VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, subcategory)
);
