-- 냉장고 반상회 MVP demo users seed
-- 기준:
-- - 이 파일은 해커톤 시연용 계정 데이터다.
-- - 모든 계정의 데모 비밀번호는 password123 이다.
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
    email,
    password_hash,
    nickname,
    cooking_skill,
    cash,
    created_at,
    updated_at
) VALUES
    ('demo1@inha.edu', 'pbkdf2$65536$a2lyb3plcm8tZGVtby0wMQ==$uikCXZkvrdWEhDcDYIkMGX1VWy+hjcrtvTG/euTV9pg=', '감자손질러', 'LOW', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('demo2@inha.edu', 'pbkdf2$65536$a2lyb3plcm8tZGVtby0wMQ==$uikCXZkvrdWEhDcDYIkMGX1VWy+hjcrtvTG/euTV9pg=', '양배추마스터', 'MEDIUM', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('demo3@inha.edu', 'pbkdf2$65536$a2lyb3plcm8tZGVtby0wMQ==$uikCXZkvrdWEhDcDYIkMGX1VWy+hjcrtvTG/euTV9pg=', '두부장인', 'HIGH', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('demo4@inha.edu', 'pbkdf2$65536$a2lyb3plcm8tZGVtby0wMQ==$uikCXZkvrdWEhDcDYIkMGX1VWy+hjcrtvTG/euTV9pg=', '대파총총', 'MEDIUM', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('demo5@inha.ac.kr', 'pbkdf2$65536$a2lyb3plcm8tZGVtby0wMQ==$uikCXZkvrdWEhDcDYIkMGX1VWy+hjcrtvTG/euTV9pg=', '김치보관함', 'LOW', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('demo6@inha.ac.kr', 'pbkdf2$65536$a2lyb3plcm8tZGVtby0wMQ==$uikCXZkvrdWEhDcDYIkMGX1VWy+hjcrtvTG/euTV9pg=', '프라이팬장인', 'HIGH', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('demo7@inha.ac.kr', 'pbkdf2$65536$a2lyb3plcm8tZGVtby0wMQ==$uikCXZkvrdWEhDcDYIkMGX1VWy+hjcrtvTG/euTV9pg=', '밥한공기', 'MEDIUM', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('demo8@inha.ac.kr', 'pbkdf2$65536$a2lyb3plcm8tZGVtby0wMQ==$uikCXZkvrdWEhDcDYIkMGX1VWy+hjcrtvTG/euTV9pg=', '채소비우기', 'LOW', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    nickname = VALUES(nickname),
    cooking_skill = VALUES(cooking_skill),
    cash = VALUES(cash),
    updated_at = VALUES(updated_at);

DELETE FROM user_allergies
WHERE user_id IN (
    SELECT id
    FROM users
    WHERE email IN (
        'demo1@inha.edu',
        'demo2@inha.edu',
        'demo3@inha.edu',
        'demo4@inha.edu',
        'demo5@inha.ac.kr',
        'demo6@inha.ac.kr',
        'demo7@inha.ac.kr',
        'demo8@inha.ac.kr'
    )
);

INSERT INTO user_allergies (
    user_id,
    allergen_tag
)
SELECT id, 'crustacean_shellfish'
FROM users
WHERE email = 'demo2@inha.edu'
ON DUPLICATE KEY UPDATE
    allergen_tag = VALUES(allergen_tag);

INSERT INTO user_allergies (
    user_id,
    allergen_tag
)
SELECT id, 'milk'
FROM users
WHERE email = 'demo4@inha.edu'
ON DUPLICATE KEY UPDATE
    allergen_tag = VALUES(allergen_tag);

INSERT INTO user_allergies (
    user_id,
    allergen_tag
)
SELECT id, 'egg'
FROM users
WHERE email = 'demo6@inha.ac.kr'
ON DUPLICATE KEY UPDATE
    allergen_tag = VALUES(allergen_tag);
