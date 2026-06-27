-- 냉장고 반상회 MVP demo flow seed
-- 구성:
-- - 슬롯 1: 데모 촬영자가 4번째로 참여하면 바로 추천 요청 가능
-- - 슬롯 2: 참여자 4명이 모여 메뉴 후보가 생성된 상태
-- - 슬롯 12: 완료 리포트와 개인 누적 리포트를 확인할 수 있는 상태
-- - 슬롯 101/102: 이전 달 리포트 추이 확인용 완료 세션

DELETE FROM consumption_record_items
WHERE record_id IN (
    SELECT id
    FROM consumption_records
    WHERE slot_id BETWEEN 1 AND 12 OR slot_id IN (101, 102)
);

DELETE FROM consumption_records
WHERE slot_id BETWEEN 1 AND 12 OR slot_id IN (101, 102);

DELETE FROM menu_votes
WHERE slot_id BETWEEN 1 AND 12 OR slot_id IN (101, 102);

DELETE FROM session_ingredients
WHERE slot_id BETWEEN 1 AND 12 OR slot_id IN (101, 102);

DELETE FROM session_participants
WHERE slot_id BETWEEN 1 AND 12 OR slot_id IN (101, 102);

DELETE FROM slots
WHERE id IN (101, 102);

UPDATE slots
SET status = 'OPEN',
    candidates_json = NULL,
    selected_menu_json = NULL,
    cooking_plan_json = NULL,
    recommendation_count = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE id BETWEEN 1 AND 12;

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
    selected_menu_json,
    created_at,
    updated_at
) VALUES
    (101, '2026-05-20', '18:00:00', '20:00:00', '인하대 조리실습실', 'PAST-MAY', 4, 'COMPLETED', 1, '{"candidateLabel":"C","menuName":"감자 두부 양배추 볶음","menuType":"LOW_CARBON","usedLeftoverIngredients":[],"commonKitItems":["간장","식용유","후추"],"purchaseItems":[],"cookingTimeMinutes":35,"difficulty":"중","recommendationReason":"과거 5월 리포트 추이 확인용 저탄소 완료 세션입니다.","cookingOutlineSteps":["감자와 양배추를 손질합니다.","두부를 구운 뒤 채소와 볶습니다."],"rolePlanSummary":["감자 손질","두부 굽기","채소 볶기","간 맞춤"]}', '2026-05-20 17:00:00', '2026-05-20 20:00:00'),
    (102, '2026-04-18', '18:00:00', '20:00:00', '인하대 조리실습실', 'PAST-APR', 4, 'COMPLETED', 1, '{"candidateLabel":"D","menuName":"김치 밥 채소 볶음","menuType":"LOW_CARBON","usedLeftoverIngredients":[],"commonKitItems":["간장","식용유","참기름"],"purchaseItems":[],"cookingTimeMinutes":30,"difficulty":"하","recommendationReason":"과거 4월 리포트 추이 확인용 저탄소 완료 세션입니다.","cookingOutlineSteps":["밥과 김치를 준비합니다.","채소를 손질한 뒤 함께 볶습니다."],"rolePlanSummary":["밥 준비","김치 손질","채소 볶기","담기"]}', '2026-04-18 17:00:00', '2026-04-18 20:00:00');

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 1, id, TRUE, '2026-06-29 15:20:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
WHERE email = 'demo1@inha.edu';

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 1, id, FALSE, '2026-06-29 15:22:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
WHERE email = 'demo2@inha.edu';

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 1, id, TRUE, '2026-06-29 15:24:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
WHERE email = 'demo3@inha.edu';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 1, sp.id, 8, 2.00, NULL, 300.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 1 AND u.email = 'demo1@inha.edu';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 1, sp.id, 3, 1.00, NULL, 200.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 1 AND u.email = 'demo1@inha.edu';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 1, sp.id, 12, 0.50, NULL, 350.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 1 AND u.email = 'demo2@inha.edu';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 1, sp.id, 21, 1.00, NULL, 80.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 1 AND u.email = 'demo2@inha.edu';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 1, sp.id, 44, 1.00, NULL, 300.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 1 AND u.email = 'demo3@inha.edu';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 1, sp.id, 16, 2.00, NULL, 300.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 1 AND u.email = 'demo3@inha.edu';

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 2, id, FALSE, '2026-06-29 15:05:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
WHERE email = 'demo4@inha.edu';

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 2, id, TRUE, '2026-06-29 15:07:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
WHERE email = 'demo5@inha.ac.kr';

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 2, id, FALSE, '2026-06-29 15:09:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
WHERE email = 'demo6@inha.ac.kr';

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 2, id, TRUE, '2026-06-29 15:11:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
WHERE email = 'demo7@inha.ac.kr';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 2, sp.id, 7, 2.00, NULL, 420.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 2 AND u.email = 'demo4@inha.edu';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 2, sp.id, 34, 2.00, NULL, 300.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 2 AND u.email = 'demo4@inha.edu';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 2, sp.id, 41, 3.00, NULL, 180.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 2 AND u.email = 'demo5@inha.ac.kr';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 2, sp.id, 3, 1.00, NULL, 200.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 2 AND u.email = 'demo5@inha.ac.kr';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 2, sp.id, 12, 0.50, NULL, 350.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 2 AND u.email = 'demo6@inha.ac.kr';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 2, sp.id, 21, 1.00, NULL, 80.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 2 AND u.email = 'demo6@inha.ac.kr';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 2, sp.id, 9, 1.00, NULL, 120.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 2 AND u.email = 'demo7@inha.ac.kr';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 2, sp.id, 44, 1.00, NULL, 300.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 2 AND u.email = 'demo7@inha.ac.kr';

UPDATE slots
SET status = 'MENU_PROPOSED',
    recommendation_count = 1,
    candidates_json = '[{"candidateLabel":"A","menuName":"닭가슴살 야채 덮밥","menuType":"GENERAL","usedLeftoverIngredients":[{"ingredientId":7,"nameKo":"밥","availableGrams":420.00,"plannedUseGrams":420.00,"estimatedUseRatio":1.00},{"ingredientId":12,"nameKo":"양배추","availableGrams":350.00,"plannedUseGrams":260.00,"estimatedUseRatio":0.74},{"ingredientId":3,"nameKo":"양파","availableGrams":200.00,"plannedUseGrams":160.00,"estimatedUseRatio":0.80}],"commonKitItems":["간장","식용유","후추"],"purchaseItems":[{"name":"닭가슴살","category":"MEAT","quantityGrams":400.00,"allergenTags":[],"assignedToNickname":"김치보관함","estimatedCost":6000}],"cookingTimeMinutes":45,"difficulty":"중","recommendationReason":"밥과 채소를 많이 쓰면서 일반식으로 포만감을 만들 수 있습니다.","cookingOutlineSteps":["밥과 채소를 준비합니다.","닭가슴살을 익힌 뒤 채소와 볶습니다.","간장 양념으로 덮밥을 완성합니다."],"rolePlanSummary":["밥 준비","채소 손질","메인 조리","간 맞춤"]},{"candidateLabel":"B","menuName":"김치 계란 볶음밥","menuType":"GENERAL","usedLeftoverIngredients":[{"ingredientId":7,"nameKo":"밥","availableGrams":420.00,"plannedUseGrams":420.00,"estimatedUseRatio":1.00},{"ingredientId":34,"nameKo":"김치","availableGrams":300.00,"plannedUseGrams":270.00,"estimatedUseRatio":0.90},{"ingredientId":41,"nameKo":"계란","availableGrams":180.00,"plannedUseGrams":180.00,"estimatedUseRatio":1.00}],"commonKitItems":["식용유","간장","참기름"],"purchaseItems":[],"cookingTimeMinutes":30,"difficulty":"하","recommendationReason":"추가구매 없이 익숙한 메뉴로 밥과 김치를 빠르게 소진할 수 있습니다.","cookingOutlineSteps":["김치를 잘게 썹니다.","계란을 스크램블합니다.","밥과 함께 볶습니다."],"rolePlanSummary":["김치 손질","계란 조리","볶음 담당","담기"]},{"candidateLabel":"C","menuName":"두부 야채 볶음","menuType":"LOW_CARBON","usedLeftoverIngredients":[{"ingredientId":44,"nameKo":"두부","availableGrams":300.00,"plannedUseGrams":300.00,"estimatedUseRatio":1.00},{"ingredientId":12,"nameKo":"양배추","availableGrams":350.00,"plannedUseGrams":300.00,"estimatedUseRatio":0.86},{"ingredientId":9,"nameKo":"당근","availableGrams":120.00,"plannedUseGrams":120.00,"estimatedUseRatio":1.00}],"commonKitItems":["간장","참기름","후추"],"purchaseItems":[],"cookingTimeMinutes":35,"difficulty":"중","recommendationReason":"동물성 추가구매 없이 두부와 채소 중심으로 소진율을 높입니다.","cookingOutlineSteps":["두부 물기를 제거합니다.","채소를 같은 두께로 손질합니다.","두부와 채소를 따로 익힌 뒤 합칩니다."],"rolePlanSummary":["두부 손질","채소 손질","볶음 담당","간 맞춤"]},{"candidateLabel":"D","menuName":"양배추 두부 덮밥","menuType":"LOW_CARBON","usedLeftoverIngredients":[{"ingredientId":7,"nameKo":"밥","availableGrams":420.00,"plannedUseGrams":420.00,"estimatedUseRatio":1.00},{"ingredientId":44,"nameKo":"두부","availableGrams":300.00,"plannedUseGrams":300.00,"estimatedUseRatio":1.00},{"ingredientId":12,"nameKo":"양배추","availableGrams":350.00,"plannedUseGrams":300.00,"estimatedUseRatio":0.86},{"ingredientId":21,"nameKo":"대파","availableGrams":80.00,"plannedUseGrams":60.00,"estimatedUseRatio":0.75}],"commonKitItems":["간장","식용유","참기름"],"purchaseItems":[],"cookingTimeMinutes":40,"difficulty":"중","recommendationReason":"추가구매 없이 밥과 두부, 채소를 한 그릇 메뉴로 묶을 수 있습니다.","cookingOutlineSteps":["밥을 데우고 두부를 굽습니다.","양배추와 대파를 볶습니다.","양념을 넣고 덮밥으로 담습니다."],"rolePlanSummary":["밥 준비","두부 굽기","채소 볶기","플레이팅"]}]',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 2;

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 12, id, TRUE, '2026-06-29 17:30:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
WHERE email = 'demo1@inha.edu';

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 12, id, FALSE, '2026-06-29 17:32:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
WHERE email = 'demo3@inha.edu';

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 12, id, TRUE, '2026-06-29 17:34:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
WHERE email = 'demo5@inha.ac.kr';

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 12, id, FALSE, '2026-06-29 17:36:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
WHERE email = 'demo8@inha.ac.kr';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 12, sp.id, 8, 2.00, NULL, 300.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 12 AND u.email = 'demo1@inha.edu';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 12, sp.id, 44, 1.00, NULL, 300.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 12 AND u.email = 'demo3@inha.edu';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 12, sp.id, 34, 2.00, NULL, 300.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 12 AND u.email = 'demo5@inha.ac.kr';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 12, sp.id, 3, 1.00, NULL, 200.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 12 AND u.email = 'demo8@inha.ac.kr';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 12, sp.id, 9, 1.00, NULL, 120.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 12 AND u.email = 'demo8@inha.ac.kr';

UPDATE slots
SET status = 'COMPLETED',
    recommendation_count = 1,
    candidates_json = '[{"candidateLabel":"C","menuName":"두부 감자 야채볶음","menuType":"LOW_CARBON","usedLeftoverIngredients":[{"ingredientId":8,"nameKo":"감자","availableGrams":300.00,"plannedUseGrams":300.00,"estimatedUseRatio":1.00},{"ingredientId":44,"nameKo":"두부","availableGrams":300.00,"plannedUseGrams":225.00,"estimatedUseRatio":0.75},{"ingredientId":34,"nameKo":"김치","availableGrams":300.00,"plannedUseGrams":300.00,"estimatedUseRatio":1.00},{"ingredientId":3,"nameKo":"양파","availableGrams":200.00,"plannedUseGrams":150.00,"estimatedUseRatio":0.75},{"ingredientId":9,"nameKo":"당근","availableGrams":120.00,"plannedUseGrams":90.00,"estimatedUseRatio":0.75}],"commonKitItems":["간장","식용유","참기름","후추"],"purchaseItems":[],"cookingTimeMinutes":35,"difficulty":"중","recommendationReason":"추가구매 없이 감자, 두부, 김치를 높은 비율로 소진한 저탄소 메뉴입니다.","cookingOutlineSteps":["감자와 채소를 채썹니다.","두부를 노릇하게 굽습니다.","김치와 채소를 볶아 한 접시로 완성합니다."],"rolePlanSummary":["감자 손질","두부 굽기","김치 볶기","간 맞춤"]}]',
    selected_menu_json = '{"candidateLabel":"C","menuName":"두부 감자 야채볶음","menuType":"LOW_CARBON","usedLeftoverIngredients":[{"ingredientId":8,"nameKo":"감자","availableGrams":300.00,"plannedUseGrams":300.00,"estimatedUseRatio":1.00},{"ingredientId":44,"nameKo":"두부","availableGrams":300.00,"plannedUseGrams":225.00,"estimatedUseRatio":0.75},{"ingredientId":34,"nameKo":"김치","availableGrams":300.00,"plannedUseGrams":300.00,"estimatedUseRatio":1.00},{"ingredientId":3,"nameKo":"양파","availableGrams":200.00,"plannedUseGrams":150.00,"estimatedUseRatio":0.75},{"ingredientId":9,"nameKo":"당근","availableGrams":120.00,"plannedUseGrams":90.00,"estimatedUseRatio":0.75}],"commonKitItems":["간장","식용유","참기름","후추"],"purchaseItems":[],"cookingTimeMinutes":35,"difficulty":"중","recommendationReason":"추가구매 없이 감자, 두부, 김치를 높은 비율로 소진한 저탄소 메뉴입니다.","cookingOutlineSteps":["감자와 채소를 채썹니다.","두부를 노릇하게 굽습니다.","김치와 채소를 볶아 한 접시로 완성합니다."],"rolePlanSummary":["감자 손질","두부 굽기","김치 볶기","간 맞춤"]}',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 12;

INSERT INTO menu_votes (
    slot_id,
    voter_id,
    candidate_label,
    vote_type,
    reason_text,
    recommendation_count,
    created_at,
    updated_at
)
SELECT 12, id, 'C', 'C', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
WHERE email = 'demo1@inha.edu';

INSERT INTO menu_votes (
    slot_id,
    voter_id,
    candidate_label,
    vote_type,
    reason_text,
    recommendation_count,
    created_at,
    updated_at
)
SELECT 12, id, 'C', 'C', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
WHERE email = 'demo3@inha.edu';

INSERT INTO menu_votes (
    slot_id,
    voter_id,
    candidate_label,
    vote_type,
    reason_text,
    recommendation_count,
    created_at,
    updated_at
)
SELECT 12, id, 'C', 'C', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
WHERE email = 'demo5@inha.ac.kr';

INSERT INTO consumption_records (
    slot_id,
    submitted_by,
    finished_food_rate,
    cooked_photo_url,
    after_photo_url,
    total_used_grams,
    avg_ingredient_use_rate,
    estimated_carbon_saved_kgco2e,
    low_carbon_selected,
    refund_score,
    refund_amount_per_user,
    created_at,
    updated_at
)
SELECT
    12,
    u.id,
    100,
    'https://kirozero-demo-assets.s3.ap-northeast-2.amazonaws.com/cooked-demo.jpg',
    'https://kirozero-demo-assets.s3.ap-northeast-2.amazonaws.com/after-demo.jpg',
    1065.00,
    85,
    1.1157,
    TRUE,
    96,
    960,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'demo1@inha.edu';

INSERT INTO consumption_record_items (
    record_id,
    session_ingredient_id,
    use_rate,
    used_grams,
    estimated_carbon_saved_kgco2e,
    created_at,
    updated_at
)
SELECT cr.id, si.id, 100, 300.00, 0.1380, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM consumption_records cr
JOIN session_ingredients si ON si.slot_id = cr.slot_id
WHERE cr.slot_id = 12 AND si.ingredient_id = 8;

INSERT INTO consumption_record_items (
    record_id,
    session_ingredient_id,
    use_rate,
    used_grams,
    estimated_carbon_saved_kgco2e,
    created_at,
    updated_at
)
SELECT cr.id, si.id, 75, 225.00, 0.7110, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM consumption_records cr
JOIN session_ingredients si ON si.slot_id = cr.slot_id
WHERE cr.slot_id = 12 AND si.ingredient_id = 44;

INSERT INTO consumption_record_items (
    record_id,
    session_ingredient_id,
    use_rate,
    used_grams,
    estimated_carbon_saved_kgco2e,
    created_at,
    updated_at
)
SELECT cr.id, si.id, 100, 300.00, 0.1530, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM consumption_records cr
JOIN session_ingredients si ON si.slot_id = cr.slot_id
WHERE cr.slot_id = 12 AND si.ingredient_id = 34;

INSERT INTO consumption_record_items (
    record_id,
    session_ingredient_id,
    use_rate,
    used_grams,
    estimated_carbon_saved_kgco2e,
    created_at,
    updated_at
)
SELECT cr.id, si.id, 75, 150.00, 0.0750, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM consumption_records cr
JOIN session_ingredients si ON si.slot_id = cr.slot_id
WHERE cr.slot_id = 12 AND si.ingredient_id = 3;

INSERT INTO consumption_record_items (
    record_id,
    session_ingredient_id,
    use_rate,
    used_grams,
    estimated_carbon_saved_kgco2e,
    created_at,
    updated_at
)
SELECT cr.id, si.id, 75, 90.00, 0.0387, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM consumption_records cr
JOIN session_ingredients si ON si.slot_id = cr.slot_id
WHERE cr.slot_id = 12 AND si.ingredient_id = 9;

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 101, id, TRUE, '2026-05-20 17:30:00', '2026-05-20 17:30:00', '2026-05-20 17:30:00'
FROM users
WHERE email = 'demo1@inha.edu';

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 101, id, FALSE, '2026-05-20 17:32:00', '2026-05-20 17:32:00', '2026-05-20 17:32:00'
FROM users
WHERE email = 'demo4@inha.edu';

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 101, id, FALSE, '2026-05-20 17:34:00', '2026-05-20 17:34:00', '2026-05-20 17:34:00'
FROM users
WHERE email = 'demo6@inha.ac.kr';

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 101, id, TRUE, '2026-05-20 17:36:00', '2026-05-20 17:36:00', '2026-05-20 17:36:00'
FROM users
WHERE email = 'demo7@inha.ac.kr';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 101, sp.id, 8, 2.00, NULL, 300.00, '2026-05-20 17:40:00', '2026-05-20 17:40:00'
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 101 AND u.email = 'demo1@inha.edu';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 101, sp.id, 12, 0.50, NULL, 350.00, '2026-05-20 17:40:00', '2026-05-20 17:40:00'
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 101 AND u.email = 'demo4@inha.edu';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 101, sp.id, 3, 1.00, NULL, 200.00, '2026-05-20 17:40:00', '2026-05-20 17:40:00'
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 101 AND u.email = 'demo6@inha.ac.kr';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 101, sp.id, 44, 1.00, NULL, 300.00, '2026-05-20 17:40:00', '2026-05-20 17:40:00'
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 101 AND u.email = 'demo7@inha.ac.kr';

INSERT INTO consumption_records (
    slot_id,
    submitted_by,
    finished_food_rate,
    cooked_photo_url,
    after_photo_url,
    total_used_grams,
    avg_ingredient_use_rate,
    estimated_carbon_saved_kgco2e,
    low_carbon_selected,
    refund_score,
    refund_amount_per_user,
    created_at,
    updated_at
)
SELECT
    101,
    u.id,
    100,
    'https://kirozero-demo-assets.s3.ap-northeast-2.amazonaws.com/cooked-demo-may.jpg',
    'https://kirozero-demo-assets.s3.ap-northeast-2.amazonaws.com/after-demo-may.jpg',
    912.50,
    81,
    0.8459,
    TRUE,
    94,
    940,
    '2026-05-20 20:05:00',
    '2026-05-20 20:05:00'
FROM users u
WHERE u.email = 'demo1@inha.edu';

INSERT INTO consumption_record_items (
    record_id,
    session_ingredient_id,
    use_rate,
    used_grams,
    estimated_carbon_saved_kgco2e,
    created_at,
    updated_at
)
SELECT cr.id, si.id, 100, 300.00, 0.1380, '2026-05-20 20:06:00', '2026-05-20 20:06:00'
FROM consumption_records cr
JOIN session_ingredients si ON si.slot_id = cr.slot_id
WHERE cr.slot_id = 101 AND si.ingredient_id = 8;

INSERT INTO consumption_record_items (
    record_id,
    session_ingredient_id,
    use_rate,
    used_grams,
    estimated_carbon_saved_kgco2e,
    created_at,
    updated_at
)
SELECT cr.id, si.id, 75, 262.50, 0.1339, '2026-05-20 20:06:00', '2026-05-20 20:06:00'
FROM consumption_records cr
JOIN session_ingredients si ON si.slot_id = cr.slot_id
WHERE cr.slot_id = 101 AND si.ingredient_id = 12;

INSERT INTO consumption_record_items (
    record_id,
    session_ingredient_id,
    use_rate,
    used_grams,
    estimated_carbon_saved_kgco2e,
    created_at,
    updated_at
)
SELECT cr.id, si.id, 100, 200.00, 0.1000, '2026-05-20 20:06:00', '2026-05-20 20:06:00'
FROM consumption_records cr
JOIN session_ingredients si ON si.slot_id = cr.slot_id
WHERE cr.slot_id = 101 AND si.ingredient_id = 3;

INSERT INTO consumption_record_items (
    record_id,
    session_ingredient_id,
    use_rate,
    used_grams,
    estimated_carbon_saved_kgco2e,
    created_at,
    updated_at
)
SELECT cr.id, si.id, 50, 150.00, 0.4740, '2026-05-20 20:06:00', '2026-05-20 20:06:00'
FROM consumption_records cr
JOIN session_ingredients si ON si.slot_id = cr.slot_id
WHERE cr.slot_id = 101 AND si.ingredient_id = 44;

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 102, id, TRUE, '2026-04-18 17:30:00', '2026-04-18 17:30:00', '2026-04-18 17:30:00'
FROM users
WHERE email = 'demo1@inha.edu';

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 102, id, FALSE, '2026-04-18 17:32:00', '2026-04-18 17:32:00', '2026-04-18 17:32:00'
FROM users
WHERE email = 'demo2@inha.edu';

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 102, id, TRUE, '2026-04-18 17:34:00', '2026-04-18 17:34:00', '2026-04-18 17:34:00'
FROM users
WHERE email = 'demo5@inha.ac.kr';

INSERT INTO session_participants (
    slot_id,
    user_id,
    can_purchase,
    joined_at,
    created_at,
    updated_at
)
SELECT 102, id, FALSE, '2026-04-18 17:36:00', '2026-04-18 17:36:00', '2026-04-18 17:36:00'
FROM users
WHERE email = 'demo8@inha.ac.kr';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 102, sp.id, 7, 2.00, NULL, 420.00, '2026-04-18 17:40:00', '2026-04-18 17:40:00'
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 102 AND u.email = 'demo1@inha.edu';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 102, sp.id, 34, 2.00, NULL, 300.00, '2026-04-18 17:40:00', '2026-04-18 17:40:00'
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 102 AND u.email = 'demo2@inha.edu';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 102, sp.id, 9, 1.00, NULL, 120.00, '2026-04-18 17:40:00', '2026-04-18 17:40:00'
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 102 AND u.email = 'demo5@inha.ac.kr';

INSERT INTO session_ingredients (
    slot_id,
    participant_id,
    ingredient_id,
    count,
    known_grams,
    estimated_grams,
    created_at,
    updated_at
)
SELECT 102, sp.id, 21, 1.00, NULL, 80.00, '2026-04-18 17:40:00', '2026-04-18 17:40:00'
FROM session_participants sp
JOIN users u ON u.id = sp.user_id
WHERE sp.slot_id = 102 AND u.email = 'demo8@inha.ac.kr';

INSERT INTO consumption_records (
    slot_id,
    submitted_by,
    finished_food_rate,
    cooked_photo_url,
    after_photo_url,
    total_used_grams,
    avg_ingredient_use_rate,
    estimated_carbon_saved_kgco2e,
    low_carbon_selected,
    refund_score,
    refund_amount_per_user,
    created_at,
    updated_at
)
SELECT
    102,
    u.id,
    75,
    'https://kirozero-demo-assets.s3.ap-northeast-2.amazonaws.com/cooked-demo-apr.jpg',
    'https://kirozero-demo-assets.s3.ap-northeast-2.amazonaws.com/after-demo-apr.jpg',
    745.00,
    75,
    0.6789,
    TRUE,
    86,
    860,
    '2026-04-18 20:05:00',
    '2026-04-18 20:05:00'
FROM users u
WHERE u.email = 'demo1@inha.edu';

INSERT INTO consumption_record_items (
    record_id,
    session_ingredient_id,
    use_rate,
    used_grams,
    estimated_carbon_saved_kgco2e,
    created_at,
    updated_at
)
SELECT cr.id, si.id, 75, 315.00, 0.4672, '2026-04-18 20:06:00', '2026-04-18 20:06:00'
FROM consumption_records cr
JOIN session_ingredients si ON si.slot_id = cr.slot_id
WHERE cr.slot_id = 102 AND si.ingredient_id = 7;

INSERT INTO consumption_record_items (
    record_id,
    session_ingredient_id,
    use_rate,
    used_grams,
    estimated_carbon_saved_kgco2e,
    created_at,
    updated_at
)
SELECT cr.id, si.id, 100, 300.00, 0.1530, '2026-04-18 20:06:00', '2026-04-18 20:06:00'
FROM consumption_records cr
JOIN session_ingredients si ON si.slot_id = cr.slot_id
WHERE cr.slot_id = 102 AND si.ingredient_id = 34;

INSERT INTO consumption_record_items (
    record_id,
    session_ingredient_id,
    use_rate,
    used_grams,
    estimated_carbon_saved_kgco2e,
    created_at,
    updated_at
)
SELECT cr.id, si.id, 75, 90.00, 0.0387, '2026-04-18 20:06:00', '2026-04-18 20:06:00'
FROM consumption_records cr
JOIN session_ingredients si ON si.slot_id = cr.slot_id
WHERE cr.slot_id = 102 AND si.ingredient_id = 9;

INSERT INTO consumption_record_items (
    record_id,
    session_ingredient_id,
    use_rate,
    used_grams,
    estimated_carbon_saved_kgco2e,
    created_at,
    updated_at
)
SELECT cr.id, si.id, 50, 40.00, 0.0200, '2026-04-18 20:06:00', '2026-04-18 20:06:00'
FROM consumption_records cr
JOIN session_ingredients si ON si.slot_id = cr.slot_id
WHERE cr.slot_id = 102 AND si.ingredient_id = 21;

UPDATE users
SET cash = CASE email
        WHEN 'demo1@inha.edu' THEN 2760
        WHEN 'demo2@inha.edu' THEN 860
        WHEN 'demo3@inha.edu' THEN 960
        WHEN 'demo4@inha.edu' THEN 940
        WHEN 'demo5@inha.ac.kr' THEN 1820
        WHEN 'demo6@inha.ac.kr' THEN 940
        WHEN 'demo7@inha.ac.kr' THEN 940
        WHEN 'demo8@inha.ac.kr' THEN 1820
        ELSE cash
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE email IN (
    'demo1@inha.edu',
    'demo2@inha.edu',
    'demo3@inha.edu',
    'demo4@inha.edu',
    'demo5@inha.ac.kr',
    'demo6@inha.ac.kr',
    'demo7@inha.ac.kr',
    'demo8@inha.ac.kr'
);
