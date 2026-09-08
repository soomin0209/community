-- 1. 사용자 데이터
-- 비밀번호는 모두 "password123@" (BCrypt 암호화)
INSERT INTO users (id, login_id, nickname, password, role, visit_count, post_count, comment_count, suspended_at, suspended_by, suspended_reason, suspension_day, created_at, updated_at, deleted_at, deleted_by) VALUES
-- 일반 회원 (BRONZE, SILVER, GOLD)
(1, 'bronze_user', '브론즈유저', '$2a$12$PS0wXUMabHcAVn2wbSxgXe2wFavAt068BSsW36HATsSj1fFYJbGHy', 'BRONZE', 5, 2, 5, NULL, NULL, NULL, 0, '2026-09-01 10:00:00', '2026-09-01 10:00:00', NULL, NULL),
(2, 'silver_user', '실버유저', '$2a$12$PS0wXUMabHcAVn2wbSxgXe2wFavAt068BSsW36HATsSj1fFYJbGHy', 'SILVER', 15, 5, 12, NULL, NULL, NULL, 0, '2026-08-25 10:00:00', '2026-08-25 10:00:00', NULL, NULL),
(3, 'gold_user', '골드유저', '$2a$12$PS0wXUMabHcAVn2wbSxgXe2wFavAt068BSsW36HATsSj1fFYJbGHy', 'GOLD', 35, 12, 35, NULL, NULL, NULL, 0, '2026-08-15 10:00:00', '2026-08-15 10:00:00', NULL, NULL),
-- 운영진
(4, 'manager', '매니저', '$2a$12$PS0wXUMabHcAVn2wbSxgXe2wFavAt068BSsW36HATsSj1fFYJbGHy', 'MANAGER', 100, 50, 100, NULL, NULL, NULL, 0, '2026-07-01 10:00:00', '2026-07-01 10:00:00', NULL, NULL),
(5, 'admin', '관리자', '$2a$12$PS0wXUMabHcAVn2wbSxgXe2wFavAt068BSsW36HATsSj1fFYJbGHy', 'ADMIN', 200, 80, 150, NULL, NULL, NULL, 0, '2026-06-01 10:00:00', '2026-06-01 10:00:00', NULL, NULL),
-- 정지된 회원
(6, 'suspended_user', '정지된유저', '$2a$12$PS0wXUMabHcAVn2wbSxgXe2wFavAt068BSsW36HATsSj1fFYJbGHy', 'BRONZE', 3, 1, 0, '2026-09-07 14:00:00', 4, '욕설 및 부적절한 게시물 작성', 7, '2026-09-05 10:00:00', '2026-09-07 14:00:00', NULL, NULL),
-- 탈퇴한 회원
(7, 'deleted_user', '탈퇴한유저', '$2a$12$PS0wXUMabHcAVn2wbSxgXe2wFavAt068BSsW36HATsSj1fFYJbGHy', 'BRONZE', 10, 3, 8, NULL, NULL, NULL, 0, '2026-08-20 10:00:00', '2026-09-01 15:00:00', '2026-09-01 15:00:00', NULL);

-- 2. 게시판 데이터
INSERT INTO boards (id, name, min_role, created_at, updated_at) VALUES
(1, '자유게시판', NULL, '2026-06-01 09:00:00', '2026-06-01 09:00:00'),
(2, '질문게시판', NULL, '2026-06-01 09:00:00', '2026-06-01 09:00:00'),
(3, '공지사항', NULL, '2026-06-01 09:00:00', '2026-06-01 09:00:00'),
(4, '자료실', 'SILVER', '2026-06-01 09:00:00', '2026-06-01 09:00:00'),
(5, '운영게시판', 'MANAGER', '2026-06-01 09:00:00', '2026-06-01 09:00:00');

-- 3. 게시물 데이터
INSERT INTO posts (id, user_id, board_id, title, content, type, view_count, is_pinned, pinned_at, created_at, updated_at, deleted_at, deleted_by) VALUES
-- 고정 공지글
(1, 5, 3, '[필독] 커뮤니티 이용 규칙', '커뮤니티 이용 시 지켜주셔야 할 규칙입니다.\n1. 상호 존중\n2. 욕설 및 비방 금지\n3. 저작권 침해 금지', 'NOTICE', 250, TRUE, '2026-06-01 10:00:00', '2026-06-01 10:00:00', '2026-06-01 10:00:00', NULL, NULL),
(2, 5, 3, '[공지] 9월 정기 점검 안내', '9월 15일 새벽 2시~4시 정기 점검이 예정되어 있습니다.', 'NOTICE', 180, TRUE, '2026-09-01 09:00:00', '2026-09-01 09:00:00', '2026-09-01 09:00:00', NULL, NULL),
-- 일반 게시물
(3, 3, 1, '오늘 날씨 정말 좋네요!', '가을 날씨가 정말 상쾌합니다. 다들 좋은 하루 보내세요~', 'GENERAL', 45, FALSE, NULL, '2026-09-08 08:30:00', '2026-09-08 08:30:00', NULL, NULL),
(4, 2, 1, '점심 메뉴 추천해주세요', '오늘 점심 뭐 먹을지 고민입니다. 추천 부탁드려요!', 'GENERAL', 32, FALSE, NULL, '2026-09-08 11:20:00', '2026-09-08 11:20:00', NULL, NULL),
(5, 1, 1, '처음 가입했습니다!', '안녕하세요! 잘 부탁드립니다.', 'GENERAL', 28, FALSE, NULL, '2026-09-07 15:00:00', '2026-09-07 15:00:00', NULL, NULL),
-- 질문 게시물
(6, 2, 2, 'Spring Boot 질문있습니다', 'JPA N+1 문제를 어떻게 해결하나요?', 'QUESTION', 67, FALSE, NULL, '2026-09-06 14:00:00', '2026-09-06 14:00:00', NULL, NULL),
(7, 1, 2, 'Git 사용법 질문', 'Git rebase와 merge의 차이가 뭔가요?', 'QUESTION', 54, FALSE, NULL, '2026-09-05 16:00:00', '2026-09-05 16:00:00', NULL, NULL),
-- 삭제된 게시물
(8, 6, 1, '부적절한 게시물', '욕설 내용...', 'GENERAL', 15, FALSE, NULL, '2026-09-07 13:00:00', '2026-09-07 14:00:00', '2026-09-07 14:00:00', 4),
-- 자료실
(9, 3, 4, 'Spring Boot 학습 자료', '유용한 Spring Boot 학습 자료 모음입니다.', 'GENERAL', 120, FALSE, NULL, '2026-09-03 10:00:00', '2026-09-03 10:00:00', NULL, NULL),
-- 운영자 게시물
(10, 4, 5, '[운영] 9월 회원 정지 처리 내역', '9월 1주차 회원 정지 처리 현황입니다.\n\n- suspended_user: 욕설 및 부적절한 게시물 작성 (7일 정지)\n- 처리일: 2026-09-07\n- 처리자: manager\n\n추가 조치 필요 시 댓글 남겨주세요.','GENERAL', 8, FALSE, NULL, '2026-09-07 15:00:00', '2026-09-07 15:00:00', NULL, NULL),
(11, 5, 5, '[안건] 게시판 운영 정책 개선 논의', '최근 자유게시판에 질문글이 많이 올라오고 있습니다.\n\n개선 방안:\n1. 게시판별 가이드라인 명확화\n2. 부적절한 게시판 작성 시 이동 안내\n3. 신고 기능 강화\n\n의견 있으시면 댓글로 남겨주세요.', 'GENERAL', 12, FALSE, NULL, '2026-09-06 10:00:00', '2026-09-06 10:00:00', NULL, NULL);

-- 4. 댓글 데이터
INSERT INTO comments (id, post_id, user_id, parent_id, content, depth, created_at, updated_at, deleted_at, deleted_by) VALUES
-- 게시물 3번의 댓글
(1, 3, 2, NULL, '맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ', 0, '2026-09-08 08:45:00', '2026-09-08 08:45:00', NULL, NULL),
(2, 3, 1, 1, '저도 나가볼까 봐요!', 1, '2026-09-08 09:00:00', '2026-09-08 09:00:00', NULL, NULL),
(3, 3, 3, NULL, '날씨 좋을 때 야외 활동 추천합니다', 0, '2026-09-08 09:30:00', '2026-09-08 09:30:00', NULL, NULL),
-- 게시물 4번의 댓글
(4, 4, 3, NULL, '제육볶음 어떠세요?', 0, '2026-09-08 11:25:00', '2026-09-08 11:25:00', NULL, NULL),
(5, 4, 1, 4, '좋은 의견이네요! 감사합니다', 1, '2026-09-08 11:30:00', '2026-09-08 11:30:00', NULL, NULL),
(6, 4, 4, NULL, '오늘 급식 메뉴 괜찮던데요', 0, '2026-09-08 11:35:00', '2026-09-08 11:35:00', NULL, NULL),
-- 게시물 5번의 댓글
(7, 5, 2, NULL, '환영합니다!', 0, '2026-09-07 15:10:00', '2026-09-07 15:10:00', NULL, NULL),
(8, 5, 3, NULL, '반갑습니다~', 0, '2026-09-07 15:15:00', '2026-09-07 15:15:00', NULL, NULL),
-- 게시물 6번의 댓글 (기술 답변)
(9, 6, 3, NULL, 'Fetch Join을 사용하면 해결됩니다!', 0, '2026-09-06 14:30:00', '2026-09-06 14:30:00', NULL, NULL),
(10, 6, 4, NULL, '@BatchSize 어노테이션도 고려해보세요', 0, '2026-09-06 15:00:00', '2026-09-06 15:00:00', NULL, NULL),
(11, 6, 2, 9, '감사합니다! 도움이 되었어요', 1, '2026-09-06 16:00:00', '2026-09-06 16:00:00', NULL, NULL),
-- 삭제된 댓글
(12, 3, 6, NULL, '부적절한 댓글...', 0, '2026-09-07 13:30:00', '2026-09-07 14:00:00', '2026-09-07 14:00:00', 4);

-- 5. 반응 데이터
INSERT INTO reactions (id, post_id, user_id, type) VALUES
-- 게시물 1번 (공지)
(1, 1, 1, 'LIKE'),
(2, 1, 2, 'LIKE'),
(3, 1, 3, 'LIKE'),
-- 게시물 3번
(4, 3, 1, 'LIKE'),
(5, 3, 2, 'LIKE'),
-- 게시물 4번
(6, 4, 3, 'LIKE'),
(7, 4, 4, 'LIKE'),
-- 게시물 6번 (질문글 - 도움됨)
(8, 6, 1, 'LIKE'),
(9, 6, 2, 'LIKE'),
(10, 6, 3, 'LIKE'),
-- 게시물 7번에 싫어요
(11, 7, 3, 'DISLIKE');

-- Auto Increment 값 재설정 (다음 ID부터 시작)
ALTER TABLE users AUTO_INCREMENT = 8;
ALTER TABLE boards AUTO_INCREMENT = 6;
ALTER TABLE posts AUTO_INCREMENT = 12;
ALTER TABLE comments AUTO_INCREMENT = 13;
ALTER TABLE reactions AUTO_INCREMENT = 12;
