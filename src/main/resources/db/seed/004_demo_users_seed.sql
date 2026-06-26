-- 냉장고 반상회 MVP demo users seed
-- 기준:
-- - 이 파일은 해커톤 시연용 계정 데이터다.
-- - password_hash는 인증 구현 시 사용하는 PasswordEncoder 정책에 맞춰 교체해야 한다.
-- - 실제 서비스 계정 seed로 사용하지 않는다.

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(80) NOT NULL,
    cooking_skill VARCHAR(20) NOT NULL,
    cash INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_allergies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    allergen_tag VARCHAR(80) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_allergies_user_tag (user_id, allergen_tag)
);

INSERT INTO users (
    id,
    email,
    password_hash,
    nickname,
    cooking_skill,
    cash,
    created_at,
    updated_at
) VALUES
    (1, 'demo1@inha.edu', 'DEMO_ONLY_REPLACE_WITH_PASSWORD_HASH', '감자손질러', 'LOW', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'demo2@inha.edu', 'DEMO_ONLY_REPLACE_WITH_PASSWORD_HASH', '양파볶는중', 'MEDIUM', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'demo3@inha.edu', 'DEMO_ONLY_REPLACE_WITH_PASSWORD_HASH', '두부장인', 'HIGH', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'demo4@inha.edu', 'DEMO_ONLY_REPLACE_WITH_PASSWORD_HASH', '대파총총', 'MEDIUM', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 'demo5@inha.ac.kr', 'DEMO_ONLY_REPLACE_WITH_PASSWORD_HASH', '김치보관함', 'LOW', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (6, 'demo6@inha.ac.kr', 'DEMO_ONLY_REPLACE_WITH_PASSWORD_HASH', '프라이팬요정', 'HIGH', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (7, 'demo7@inha.ac.kr', 'DEMO_ONLY_REPLACE_WITH_PASSWORD_HASH', '밥한공기', 'MEDIUM', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (8, 'demo8@inha.ac.kr', 'DEMO_ONLY_REPLACE_WITH_PASSWORD_HASH', '채소비우기', 'LOW', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    nickname = VALUES(nickname),
    cooking_skill = VALUES(cooking_skill),
    cash = VALUES(cash),
    updated_at = VALUES(updated_at);

INSERT INTO user_allergies (
    id,
    user_id,
    allergen_tag
) VALUES
    (1, 2, 'crustacean_shellfish'),
    (2, 4, 'milk'),
    (3, 6, 'egg')
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    allergen_tag = VALUES(allergen_tag);
