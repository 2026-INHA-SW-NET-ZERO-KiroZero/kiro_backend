# KiroZero 백엔드 API 명세서

> 해커톤 데모용 (v0.2) — 인증 없음, 더미 유저 기반
>
> **v0.2 변경:** 운영자 대시보드를 발표용 임팩트 화면으로 재구성 (CO₂ 절감/추이/환산), 방에 `location` 좌표 추가, 개인 리포트에도 CO₂ 환산 적용

## 공통 규약

- **Base URL:** `http://localhost:8080`
- **인증:** 없음. 유저 식별이 필요한 엔드포인트는 헤더 `X-User-Id: {userId}` 로 전달
- **요청/응답 Content-Type:** `application/json`
- **에러 응답 포맷 (공통):**
  ```json
  { "code": "ROOM_FULL", "message": "방 정원이 가득 찼습니다." }
  ```
- **방 상태(Room.status):** `OPEN` → `CONFIRMED` → `COOKED` → `CLOSED`

---

## 1. 유저 / 내 정보

### 1.1 내 정보 조회

| | |
|---|---|
| Method | `GET` |
| Path | `/api/me` |
| Header | `X-User-Id: 1` |

**응답 200**
```json
{
  "id": 1,
  "nickname": "임준현",
  "email": "junhyeon@inha.ac.kr",
  "allergyTags": ["땅콩", "갑각류"]
}
```

---

## 2. 식재료 (개인 냉장고)

### 2.1 내 식재료 목록 조회

| | |
|---|---|
| Method | `GET` |
| Path | `/api/me/ingredients` |
| Header | `X-User-Id: 1` |

**응답 200**
```json
[
  {
    "id": 11,
    "name": "양파",
    "quantity": 2,
    "unit": "개",
    "expiresAt": "2026-06-30",
    "note": "껍질 깐 상태"
  },
  {
    "id": 12,
    "name": "돼지고기",
    "quantity": 300,
    "unit": "g",
    "expiresAt": "2026-06-28",
    "note": null
  }
]
```

### 2.2 식재료 추가

| | |
|---|---|
| Method | `POST` |
| Path | `/api/me/ingredients` |
| Header | `X-User-Id: 1` |

**요청**
```json
{
  "name": "대파",
  "quantity": 1,
  "unit": "단",
  "expiresAt": "2026-07-02",
  "note": null
}
```

**응답 201**
```json
{
  "id": 15,
  "name": "대파",
  "quantity": 1,
  "unit": "단",
  "expiresAt": "2026-07-02",
  "note": null
}
```

### 2.3 식재료 삭제

| | |
|---|---|
| Method | `DELETE` |
| Path | `/api/me/ingredients/{ingredientId}` |
| Header | `X-User-Id: 1` |

**응답 204** (본문 없음)

---

## 3. 방 (모임)

### 3.1 OPEN 방 목록 조회

| | |
|---|---|
| Method | `GET` |
| Path | `/api/rooms` |
| Query | `status=OPEN` (선택, 기본 OPEN) |

**응답 200**
```json
[
  {
    "id": 101,
    "title": "화요일 저녁 같이 요리해요",
    "scheduledAt": "2026-06-30T18:00:00",
    "place": "인하대 학생식당 조리실습실 A",
    "location": { "lat": 37.4499, "lng": 126.6566 },
    "capacity": 4,
    "participantCount": 2,
    "status": "OPEN"
  }
]
```

> `location` 은 프론트에서 캠퍼스 지도에 마커 표시용. 운영자가 방 개설 시 입력 (없으면 null).

### 3.2 방 상세 조회

| | |
|---|---|
| Method | `GET` |
| Path | `/api/rooms/{roomId}` |

**응답 200**
```json
{
  "id": 101,
  "title": "화요일 저녁 같이 요리해요",
  "scheduledAt": "2026-06-30T18:00:00",
  "place": "인하대 학생식당 조리실습실 A",
  "location": { "lat": 37.4499, "lng": 126.6566 },
  "capacity": 4,
  "status": "CONFIRMED",
  "participants": [
    { "userId": 1, "nickname": "임준현" },
    { "userId": 2, "nickname": "이주한" },
    { "userId": 3, "nickname": "차태훈" },
    { "userId": 4, "nickname": "김준호" }
  ],
  "aggregatedIngredients": [
    { "name": "양파", "totalQuantity": 3, "unit": "개" },
    { "name": "돼지고기", "totalQuantity": 500, "unit": "g" }
  ],
  "menuChoice": {
    "candidates": [
      {
        "name": "돼지고기 양파 볶음",
        "needed": [
          { "name": "양파", "quantity": 2, "unit": "개" },
          { "name": "돼지고기", "quantity": 400, "unit": "g" }
        ],
        "missing": [
          { "name": "간장", "quantity": 30, "unit": "ml" }
        ],
        "score": 0.82
      }
    ],
    "chosenIdx": null,
    "chosenName": null
  }
}
```

### 3.3 방 참여 (식재료 가져가기)

| | |
|---|---|
| Method | `POST` |
| Path | `/api/rooms/{roomId}/join` |
| Header | `X-User-Id: 1` |

**요청** — `userIngredientId` 별로 이번 모임에 가져갈 양 지정
```json
{
  "contributions": [
    { "userIngredientId": 11, "providedQuantity": 1 },
    { "userIngredientId": 12, "providedQuantity": 200 }
  ]
}
```

**응답 200**
```json
{
  "roomId": 101,
  "status": "OPEN",
  "participantCount": 3,
  "allergyWarnings": []
}
```

> 정원이 차면 `status: "CONFIRMED"` 로 반환됨.
> 다른 참여자의 알레르기와 충돌하는 재료가 있으면 `allergyWarnings` 에 사유가 담김 (블로킹 X).

**에러 예시**
| code | 의미 |
|---|---|
| `ROOM_NOT_OPEN` | 이미 마감된 방 |
| `ROOM_FULL` | 정원 초과 |
| `ALREADY_JOINED` | 중복 참여 |

### 3.4 방 나가기

| | |
|---|---|
| Method | `POST` |
| Path | `/api/rooms/{roomId}/leave` |
| Header | `X-User-Id: 1` |

**응답 200**
```json
{ "roomId": 101, "participantCount": 2 }
```

> `OPEN` 상태에서만 가능.

---

## 4. 메뉴 추천

> 내부적으로 `RecommendationClient` 인터페이스를 호출 (현재 Mock 구현체).
> Bedrock/Lambda 연동 방식은 추후 협의 후 교체.

### 4.1 메뉴 후보 생성

| | |
|---|---|
| Method | `POST` |
| Path | `/api/rooms/{roomId}/recommendations` |

> 방 상태가 `CONFIRMED` 일 때만 가능.

**요청 본문 없음**

**응답 200**
```json
{
  "roomId": 101,
  "candidates": [
    {
      "name": "돼지고기 양파 볶음",
      "needed": [
        { "name": "양파", "quantity": 2, "unit": "개" },
        { "name": "돼지고기", "quantity": 400, "unit": "g" }
      ],
      "missing": [
        { "name": "간장", "quantity": 30, "unit": "ml" }
      ],
      "score": 0.82
    },
    {
      "name": "야채 카레",
      "needed": [...],
      "missing": [...],
      "score": 0.71
    }
  ]
}
```

### 4.2 메뉴 선택

| | |
|---|---|
| Method | `POST` |
| Path | `/api/rooms/{roomId}/recommendations/choose` |

**요청**
```json
{ "chosenIdx": 0 }
```

**응답 200**
```json
{
  "roomId": 101,
  "chosenIdx": 0,
  "chosenName": "돼지고기 양파 볶음"
}
```

---

## 5. 정산

### 5.1 영수증 등록 (균등분배)

| | |
|---|---|
| Method | `POST` |
| Path | `/api/rooms/{roomId}/settlement` |

**요청**
```json
{
  "payerUserId": 1,
  "items": [
    { "name": "간장", "amount": 3000 },
    { "name": "돼지고기 추가", "amount": 8000 }
  ]
}
```

**응답 201**
```json
{
  "roomId": 101,
  "totalAmount": 11000,
  "perPersonShare": 2750,
  "payer": { "userId": 1, "nickname": "임준현" },
  "debtors": [
    { "userId": 2, "nickname": "이주한", "amount": 2750 },
    { "userId": 3, "nickname": "차태훈", "amount": 2750 },
    { "userId": 4, "nickname": "김준호", "amount": 2750 }
  ]
}
```

### 5.2 정산 조회

| | |
|---|---|
| Method | `GET` |
| Path | `/api/rooms/{roomId}/settlement` |

**응답 200** — 위 응답과 동일 포맷

---

## 6. 조리 완료 (사용량 기록)

### 6.1 사용량 등록

| | |
|---|---|
| Method | `POST` |
| Path | `/api/rooms/{roomId}/usage` |

**요청** — 방에 가져온 재료(roomIngredient)별 실제 사용량
```json
{
  "usages": [
    { "roomIngredientId": 501, "usedQuantity": 1.0 },
    { "roomIngredientId": 502, "usedQuantity": 180 }
  ]
}
```

**응답 200**
```json
{
  "roomId": 101,
  "status": "COOKED"
}
```

---

## 7. 리포트 (개인)

### 7.1 내 누적 리포트

| | |
|---|---|
| Method | `GET` |
| Path | `/api/me/report` |
| Header | `X-User-Id: 1` |

**응답 200**
```json
{
  "joinedRoomCount": 3,
  "totalProvided": 8.5,
  "totalUsed": 6.6,
  "consumptionRate": 77.6,
  "co2SavedKg": 16.5,
  "equivalents": {
    "treesYearAbsorption": 0.75,
    "carKm": 78.6
  }
}
```

**필드 설명**
- `consumptionRate` = `totalUsed / totalProvided × 100`
- `co2SavedKg` = `totalUsed × 2.5` (운영자 대시보드와 동일 계수)
- `equivalents`: 운영자 대시보드와 동일 환산 (개인은 나무·자동차 2개만 표시)

> 발표 시 "나도 한 명의 사용자로서 나무 0.75그루를 살린 셈" 같은 개인화 메시지로 활용 가능.

---

## 8. 운영자 (Admin)

> 인증 없음 — `/admin/*` 경로 분리만. 실서비스 전환 시 보호 필요.

### 8.1 방 사전 개설

| | |
|---|---|
| Method | `POST` |
| Path | `/admin/rooms` |

**요청**
```json
{
  "title": "수요일 점심 같이 먹어요",
  "scheduledAt": "2026-07-01T12:00:00",
  "place": "인하대 식품영양학과 조리실습실",
  "location": { "lat": 37.4505, "lng": 126.6570 },
  "capacity": 4
}
```

**응답 201**
```json
{
  "id": 102,
  "title": "수요일 점심 같이 먹어요",
  "scheduledAt": "2026-07-01T12:00:00",
  "place": "인하대 식품영양학과 조리실습실",
  "location": { "lat": 37.4505, "lng": 126.6570 },
  "capacity": 4,
  "status": "OPEN"
}
```

### 8.2 방 강제 종료

| | |
|---|---|
| Method | `POST` |
| Path | `/admin/rooms/{roomId}/close` |

**응답 200**
```json
{ "id": 102, "status": "CLOSED" }
```

### 8.3 운영자 대시보드 (발표용 임팩트 화면)

| | |
|---|---|
| Method | `GET` |
| Path | `/admin/dashboard` |
| Query | `range=all` (기본) / `range=week` / `range=month` |

**용도:** 해커톤 발표 클라이맥스 화면. "지금까지 우리 서비스가 만들어낸 환경 임팩트"를 한눈에.

**응답 200**
```json
{
  "summary": {
    "totalParticipants": 38,
    "totalCookedRooms": 7,
    "totalConsumedKg": 22.4,
    "totalCo2SavedKg": 56.0
  },
  "co2Trend": [
    { "date": "2026-06-20", "dailyCo2SavedKg": 4.5, "cumulativeCo2SavedKg": 18.0 },
    { "date": "2026-06-21", "dailyCo2SavedKg": 6.2, "cumulativeCo2SavedKg": 24.2 },
    { "date": "2026-06-22", "dailyCo2SavedKg": 5.0, "cumulativeCo2SavedKg": 29.2 },
    { "date": "2026-06-23", "dailyCo2SavedKg": 8.8, "cumulativeCo2SavedKg": 38.0 },
    { "date": "2026-06-24", "dailyCo2SavedKg": 7.4, "cumulativeCo2SavedKg": 45.4 },
    { "date": "2026-06-25", "dailyCo2SavedKg": 6.0, "cumulativeCo2SavedKg": 51.4 },
    { "date": "2026-06-26", "dailyCo2SavedKg": 4.6, "cumulativeCo2SavedKg": 56.0 }
  ],
  "equivalents": {
    "treesYearAbsorption": 2.5,
    "carKm": 266.7,
    "householdElectricityKwh": 140.0
  }
}
```

**필드 설명**

| 필드 | 의미 | 계산 방식 |
|---|---|---|
| `summary.totalParticipants` | 누적 참여자 수 (중복 제외) | `COUNT(DISTINCT participation.userId)` |
| `summary.totalCookedRooms` | 조리 완료된(COOKED) 방 수 | `COUNT(Room WHERE status >= COOKED)` |
| `summary.totalConsumedKg` | 누적 소진 식재료 (kg) | `SUM(UsageRecord.usedQuantity)` (단위 정규화 후) |
| `summary.totalCo2SavedKg` | 누적 CO₂ 절감 (kg) | `totalConsumedKg × 2.5` (FAO 식품 손실 평균 환산 계수) |
| `co2Trend[]` | 일별 CO₂ 절감 추이 | 날짜별 GROUP BY → 일일/누적 두 값 모두 제공 (선 그래프용) |
| `equivalents.treesYearAbsorption` | 어린 나무 1년 흡수량 환산 | `totalCo2SavedKg / 22` (나무 1그루 ≈ 22kg CO₂/년) |
| `equivalents.carKm` | 승용차 운행 km 환산 | `totalCo2SavedKg / 0.21` (1km ≈ 0.21kg CO₂) |
| `equivalents.householdElectricityKwh` | 가정 전기 사용 환산 | `totalCo2SavedKg / 0.4` (1kWh ≈ 0.4kg CO₂) |

**발표 스토리텔링 예시**
> "지금까지 KiroZero를 통해 38명의 학생이 22.4kg의 식재료를 살렸고, 이는 **56kg의 CO₂ 절감**, 즉 **어린 나무 2.5그루가 1년간 흡수하는 양**과 같습니다. 그리고 이 그래프처럼, 매일 성장하고 있습니다."

---

### 8.4 (선택) 환산 계수 출처

발표/문서 footnote 용:
- **CO₂ 환산 계수 (2.5 kg CO₂eq / kg)**: FAO "Food Wastage Footprint" 평균치 기반 단순화
- **나무 흡수량 (22 kg CO₂/년)**: 산림청 공시 평균
- **승용차 km (0.21 kg CO₂/km)**: 환경부 자동차 평균 배출량
- **가정 전기 (0.4 kg CO₂/kWh)**: 한전 전력 탄소배출계수

---

## 부록. 엔드포인트 요약표

| # | Method | Path | 설명 |
|---|---|---|---|
| 1 | GET | `/api/me` | 내 정보 조회 |
| 2 | GET | `/api/me/ingredients` | 내 식재료 목록 |
| 3 | POST | `/api/me/ingredients` | 식재료 추가 |
| 4 | DELETE | `/api/me/ingredients/{id}` | 식재료 삭제 |
| 5 | GET | `/api/rooms` | OPEN 방 목록 |
| 6 | GET | `/api/rooms/{id}` | 방 상세 |
| 7 | POST | `/api/rooms/{id}/join` | 방 참여 (식재료 가져가기) |
| 8 | POST | `/api/rooms/{id}/leave` | 방 나가기 |
| 9 | POST | `/api/rooms/{id}/recommendations` | 메뉴 후보 생성 |
| 10 | POST | `/api/rooms/{id}/recommendations/choose` | 메뉴 선택 |
| 11 | POST | `/api/rooms/{id}/settlement` | 영수증 등록 (균등분배) |
| 12 | GET | `/api/rooms/{id}/settlement` | 정산 조회 |
| 13 | POST | `/api/rooms/{id}/usage` | 조리 완료 (사용량 입력) |
| 14 | GET | `/api/me/report` | 내 누적 리포트 |
| 15 | POST | `/admin/rooms` | 방 사전 개설 |
| 16 | POST | `/admin/rooms/{id}/close` | 방 강제 종료 |
| 17 | GET | `/admin/dashboard` | 운영자 대시보드 |
