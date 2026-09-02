ALTER TABLE posts DROP FOREIGN KEY fk_post_user;
ALTER TABLE posts DROP FOREIGN KEY fk_post_board;

ALTER TABLE comments DROP FOREIGN KEY fk_comment_parent;
ALTER TABLE comments DROP FOREIGN KEY fk_comment_post;
ALTER TABLE comments DROP FOREIGN KEY fk_comment_user;

ALTER TABLE files DROP FOREIGN KEY fk_file_user;
ALTER TABLE files DROP FOREIGN KEY fk_file_post;

ALTER TABLE reactions DROP FOREIGN KEY fk_reaction_post;
ALTER TABLE reactions DROP FOREIGN KEY fk_reaction_user;


-- Post 테이블
ALTER TABLE posts
    ADD CONSTRAINT fk_post_user
    FOREIGN KEY (user_id)
    REFERENCES users (id);

ALTER TABLE posts
    ADD CONSTRAINT fk_post_board
    FOREIGN KEY (board_id)
    REFERENCES boards(id);


-- Comment 테이블
ALTER TABLE comments
    ADD CONSTRAINT fk_comment_parent
    FOREIGN KEY (parent_id)
    REFERENCES comments(id);

ALTER TABLE comments
    ADD CONSTRAINT fk_comment_post
    FOREIGN KEY (post_id)
    REFERENCES posts(id);

ALTER TABLE comments
    ADD CONSTRAINT fk_comment_user
    FOREIGN KEY (user_id)
    REFERENCES users(id);


-- File 테이블
ALTER TABLE files
    ADD CONSTRAINT fk_file_user
    FOREIGN KEY (user_id)
    REFERENCES users(id);

ALTER TABLE files
    ADD CONSTRAINT fk_file_post
    FOREIGN KEY (post_id)
    REFERENCES posts(id);


-- Reaction 테이블
ALTER TABLE reactions
    ADD CONSTRAINT fk_reaction_post
    FOREIGN KEY (post_id)
    REFERENCES posts(id);

ALTER TABLE reactions
    ADD CONSTRAINT fk_reaction_user
    FOREIGN KEY (user_id)
    REFERENCES users(id);
