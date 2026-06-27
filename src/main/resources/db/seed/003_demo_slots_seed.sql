-- 냉장고 반상회 MVP demo slots seed
-- 기준:
-- - 슬롯은 메뉴 방이 아니라 장소/날짜/시간/화구 스테이션 단위다.
-- - 인하대 조리실습실 화구 6개를 A-F 스테이션으로 나눈다.
-- - 16:00-18:00, 18:00-20:00 두 타임을 열어 하루 12개 슬롯을 만든다.

CREATE TABLE IF NOT EXISTS slots (
    id BIGINT PRIMARY KEY,
    `date` DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    place_name VARCHAR(120) NOT NULL,
    station_code VARCHAR(10) NOT NULL,
    capacity INT NOT NULL DEFAULT 4,
    status VARCHAR(40) NOT NULL,
    candidates_json JSON NULL,
    selected_menu_json JSON NULL,
    cooking_plan_json JSON NULL,
    recommendation_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_slots_date_time_station (`date`, start_time, end_time, station_code)
);

INSERT INTO slots (
    id,
    `date`,
    start_time,
    end_time,
    place_name,
    station_code,
    capacity,
    status,
    recommendation_count,
    created_at,
    updated_at
) VALUES
    (1, '2026-06-29', '16:00:00', '18:00:00', '인하대 조리실습실', 'A', 4, 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2026-06-29', '16:00:00', '18:00:00', '인하대 조리실습실', 'B', 4, 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, '2026-06-29', '16:00:00', '18:00:00', '인하대 조리실습실', 'C', 4, 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, '2026-06-29', '16:00:00', '18:00:00', '인하대 조리실습실', 'D', 4, 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, '2026-06-29', '16:00:00', '18:00:00', '인하대 조리실습실', 'E', 4, 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (6, '2026-06-29', '16:00:00', '18:00:00', '인하대 조리실습실', 'F', 4, 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (7, '2026-06-29', '18:00:00', '20:00:00', '인하대 조리실습실', 'A', 4, 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (8, '2026-06-29', '18:00:00', '20:00:00', '인하대 조리실습실', 'B', 4, 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9, '2026-06-29', '18:00:00', '20:00:00', '인하대 조리실습실', 'C', 4, 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (10, '2026-06-29', '18:00:00', '20:00:00', '인하대 조리실습실', 'D', 4, 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (11, '2026-06-29', '18:00:00', '20:00:00', '인하대 조리실습실', 'E', 4, 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (12, '2026-06-29', '18:00:00', '20:00:00', '인하대 조리실습실', 'F', 4, 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    `date` = VALUES(`date`),
    start_time = VALUES(start_time),
    end_time = VALUES(end_time),
    place_name = VALUES(place_name),
    station_code = VALUES(station_code),
    capacity = VALUES(capacity),
    status = VALUES(status),
    candidates_json = NULL,
    selected_menu_json = NULL,
    cooking_plan_json = NULL,
    recommendation_count = VALUES(recommendation_count),
    updated_at = VALUES(updated_at);
