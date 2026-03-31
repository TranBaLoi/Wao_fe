# API Reference - Wao_be

Tai lieu nay tong hop toan bo REST API hien co trong du an, bao gom:
- Endpoint + HTTP method
- Du lieu can gui (request)
- Du lieu tra ve (response)
- Enum gia tri hop le
- Loi thuong gap

## 1) Thong tin chung

- Base URL local: `http://localhost:8080`
- Content-Type: `application/json`
- Hien tai chua co auth token trong code (cac API goi truc tiep).

### Error response chung

Tat ca loi duoc tra theo format tu `GlobalExceptionHandler`:

```json
{
  "status": 400,
  "message": "Validation failed",
  "fieldErrors": {
    "email": "must be a well-formed email address"
  },
  "timestamp": "2026-03-21T10:00:00"
}
```

- `400`: `IllegalArgumentException`, validation loi (`@Valid`)
- `404`: `EntityNotFoundException`
- `500`: loi he thong khac

## 2) Enum su dung trong API

- `UserStatus`: `ACTIVE`, `INACTIVE`, `BANNED`
- `Gender`: `MALE`, `FEMALE`, `OTHER`
- `ActivityLevel`: `SEDENTARY`, `LIGHTLY_ACTIVE`, `MODERATELY_ACTIVE`, `VERY_ACTIVE`, `EXTRA_ACTIVE`
  - `SEDENTARY` (1.2x): Ít vận động (chỉ ngồi làm việc)
  - `LIGHTLY_ACTIVE` (1.375x): 1-3 ngày/tuần tập luyện
  - `MODERATELY_ACTIVE` (1.55x): 3-5 ngày/tuần tập luyện
  - `VERY_ACTIVE` (1.725x): 6-7 ngày/tuần tập luyện
  - `EXTRA_ACTIVE` (1.9x): Cực kỳ vận động (tập luyện 2 lần/ngày)
- `GoalType`: `LOSE_WEIGHT`, `GAIN_WEIGHT`, `MAINTAIN`
  - `LOSE_WEIGHT`: Giảm cân (desiredWeight < currentWeight)
  - `GAIN_WEIGHT`: Tăng cân (desiredWeight > currentWeight)
  - `MAINTAIN`: Duy trì cân nặng (desiredWeight ≈ currentWeight ±2%)
- `MealType`: `BREAKFAST`, `LUNCH`, `DINNER`, `SNACK`
- `MealPlanType`: `SYSTEM_SUGGESTION`, `USER_CUSTOM`
- `ProgramLevel`: `BEGINNER`, `INTERMEDIATE`, `PRO`

## 3) Users API

### POST `/api/users/register`
Request body:
```json
{
  "email": "user@example.com",
  "password": "123456",
  "fullName": "Nguyen Van A"
}
```
Response `201`:
```json
{
  "id": 1,
  "email": "user@example.com",
  "fullName": "Nguyen Van A",
  "status": "ACTIVE"
}
```

### POST `/api/users/google-login`
Request body:
```json
{
  "idToken": "google_id_token_here"
}
```
Response `200`: `UserDto.Response`

### POST `/api/users/verify`
Dùng để verify hoặc reset password (tuỳ logic thực tế, thông thường nhận mã OTP/Email).
Response `200`

### GET `/api/users`
Response `200`: `UserDto.Response[]`

### GET `/api/users/{id}`
Path param: `id` (Long)
Response `200`: `UserDto.Response`

### PUT `/api/users/{id}`
Path param: `id` (Long)
Request body (co the gui 1 hoac nhieu field):
```json
{
  "fullName": "Nguyen Van B",
  "status": "INACTIVE",
  "img": "https://avatar-url.com/image.jpg"
}
```
Response `200`: `UserDto.Response`

### PUT `/api/users/{id}/password`
Path param: `id` (Long)
Request body:
```json
{
  "oldPassword": "old_password_here",
  "newPassword": "new_password_here"
}
```
Response `200` (khong body)

### POST `/api/users/upload-avatar`
Request form-data:
- `file` (MultipartFile): File ảnh đại diện cần upload.
Response `200`: `{"imgUrl": "https://avatar-url.com/image.jpg"}`

### POST `/api/users/{id}/avatar`
Path param: `id` (Long)
Request form-data:
- `file` (MultipartFile): File ảnh đại diện cần upload.
Response `200`: `{"imgUrl": "https://avatar-url.com/image.jpg"}`

### DELETE `/api/users/{id}`
Path param: `id` (Long)
Response `204` (khong body)

## 4) Health Profile API

### POST `/api/users/{userId}/health-profiles`
Path param: `userId` (Long)

Request body:
```json
{
  "gender": "MALE",
  "dob": "1998-01-20",
  "heightCm": 172,
  "weightKg": 70,
  "activityLevel": "MODERATELY_ACTIVE",
  "goalType": "LOSE_WEIGHT",
  "desiredWeightKg": 65,
  "targetDays": 90,
  "preferenceVector": "0.8, 0.2, 0.5",
  "allergies": "PEANUT, MILK"
}
```

**Fields explanation:**
- `gender`: `MALE`, `FEMALE`, `OTHER`
- `dob`: Ngày sinh (ISO format: YYYY-MM-DD)
- `heightCm`: Chiều cao (cm)
- `weightKg`: Cân nặng hiện tại (kg)
- `activityLevel`: Mức hoạt động (`SEDENTARY`, `LIGHTLY_ACTIVE`, `MODERATELY_ACTIVE`, `VERY_ACTIVE`, `EXTRA_ACTIVE`)
- `goalType`: Mục tiêu (`LOSE_WEIGHT`, `GAIN_WEIGHT`, `MAINTAIN`)
- `desiredWeightKg` **[NEW]**: Cân nặng mong muốn (kg)
  - **LOSE_WEIGHT**: phải < `weightKg` (ví dụ: 70 → 65)
  - **GAIN_WEIGHT**: phải > `weightKg` (ví dụ: 70 → 75)
  - **MAINTAIN**: phải ≈ `weightKg` (trong ±2%, ví dụ: 70 → 70.5)
- `targetDays` **[NEW]**: Số ngày để đạt mục tiêu (ngày, phải > 0)
- `preferenceVector` **[NEW]**: Mảng đặc trưng sở thích người dùng dùng cho thuật toán AI (vd: "0.8, 0.2, 0.5")
- `allergies` **[NEW]**: Danh sách dị ứng của người dùng (vd: "PEANUT, MILK")

Response `201`:
```json
{
  "id": 10,
  "userId": 1,
  "gender": "MALE",
  "dob": "1998-01-20",
  "heightCm": 172.0,
  "weightKg": 70.0,
  "activityLevel": "MODERATELY_ACTIVE",
  "goalType": "LOSE_WEIGHT",
  "desiredWeightKg": 65.0,
  "targetDays": 90,
  "targetCalories": 1986.22,
  "dailyCalories": 427.78,
  "dailyCalorieBreakdown": {
    "dailyCalories": 427.78,
    "difficultyLevel": "MEDIUM",
    "note": "Muc tieu trung binh, can theo doi va ky luat"
  },
  "preferenceVector": "0.8, 0.2, 0.5",
  "allergies": "PEANUT, MILK"
}
```

**Response fields:**
- `targetCalories` **[UPDATED]**: Tong luong calo/ngay ma user CAN AN VAO de dat muc tieu
  - Cong thuc: `TDEE` hien tai `+/- dailyCalories`
- `dailyCalories` **[NEW]**: Luong calo can bu/tru them moi ngay de dat muc tieu
  - Cong thuc: `(|desiredWeightKg - weightKg| * 7700) / targetDays`
- `dailyCalorieBreakdown` **[NEW]**: Nhan xet muc do kho cua lo trinh
  - `difficultyLevel`: `EASY` | `MEDIUM` | `HARD`
  - `note`: Ghi chu de user hieu muc do de/kho

**Validation errors:**
```json
{
  "status": 400,
  "message": "Desired weight must be less than current weight for LOSE_WEIGHT goal",
  "timestamp": "2026-03-26T10:00:00"
}
```

### GET `/api/users/{userId}/health-profiles/latest`
Path param: `userId` (Long)

Response `200`: `HealthProfileDto.Response`

Trả về health profile mới nhất (đầy đủ tất cả thông tin bao gồm breakdown calories)

### GET `/api/users/{userId}/health-profiles/history`
Path param: `userId` (Long)

Response `200`: `HealthProfileDto.Response[]`

Trả về danh sách tất cả health profiles của user (sắp xếp từ mới nhất đến cũ nhất)

## 5) Foods API

### POST `/api/foods`
Tao food user (auto `isVerified=false`)

Request body:
```json
{
  "name": "Com ga",
  "servingSize": "1 dia",
  "calories": 500,
  "protein": 25,
  "carbs": 60,
  "fat": 15,
  "featureVector": "0.8, 0.5, 0.2",
  "suitableMealTypes": "LUNCH,DINNER",
  "ingredients": "gạo, thịt gà, muối",
  "containsAllergens": "SEAFOOD, PEANUT"
}
```
*Note: `featureVector` (đặc trưng món ăn) và `suitableMealTypes` (các bữa ăn phù hợp, vd: "BREAKFAST,LUNCH") dùng cho tính năng định tuyến AI gợi ý.*
*Note 2: `ingredients` và `containsAllergens` dùng để lọc an toàn cho người dùng bị dị ứng.*

Response `201`:
```json
{
  "id": 1,
  "name": "Com ga",
  "servingSize": "1 dia",
  "calories": 500,
  "protein": 25,
  "carbs": 60,
  "fat": 15,
  "isVerified": false,
  "images": [],
  "featureVector": "0.8, 0.5, 0.2",
  "suitableMealTypes": "LUNCH,DINNER",
  "ingredients": "gạo, thịt gà, muối",
  "containsAllergens": "SEAFOOD, PEANUT"
}
```

### POST `/api/foods/admin`
Tao food admin (auto `isVerified=true`)
Request body giong `POST /api/foods`
Response `201`: `FoodDto.Response`

### GET `/api/foods/search?name={keyword}`
Query param: `name` (optional)
Response `200`: `FoodDto.Response[]`

### GET `/api/foods/{id}`
Path param: `id` (Long)
Response `200`: `FoodDto.Response`

### PUT `/api/foods/{id}`
Path param: `id` (Long)
Request body giong tao food
Response `200`: `FoodDto.Response`

### DELETE `/api/foods/{id}`
Path param: `id` (Long)
Response `204`

## 6) Food Logs API

### POST `/api/users/{userId}/food-logs`
Path param: `userId` (Long)
Request body:
```json
{
  "foodId": 2,
  "mealType": "LUNCH",
  "servingQty": 1.5,
  "logDate": "2026-03-21"
}
```
Response `201`:
```json
{
  "id": 99,
  "userId": 1,
  "foodId": 2,
  "foodName": "Com ga",
  "mealType": "LUNCH",
  "servingQty": 1.5,
  "totalCalories": 750.0,
  "logDate": "2026-03-21"
}
```
Note: sau khi log, he thong tu dong refresh `daily_summaries`.

### GET `/api/users/{userId}/food-logs/by-date?date=yyyy-MM-dd`
Path param: `userId` (Long)
Query param: `date` (LocalDate)
Response `200`: `FoodLogDto.Response[]`

### GET `/api/users/{userId}/food-logs/by-meal-type?mealType={mealType}`
Path param: `userId` (Long)
Query param: `mealType` (Enum: BREAKFAST, LUNCH, DINNER, SNACK)
Response `200`: `FoodLogDto.Response[]`

### GET `/api/users/{userId}/food-logs/by-date-and-meal-type?date=yyyy-MM-dd&mealType={mealType}`
Path param: `userId` (Long)
Query param: `date` (LocalDate), `mealType` (Enum)
Response `200`: `FoodLogDto.Response[]`

### DELETE `/api/users/{userId}/food-logs/{logId}`
Path param: `userId`, `logId`
Response `204`

## 7) Exercises API

### POST `/api/exercises`
Request body:
```json
{
  "name": "Jumping Jack",
  "categoryId": 1,
  "videoUrl": "https://...",
  "caloriesPerMin": 8.5,
  "description": "Cardio co ban"
}
```
Response `201`: `ExerciseDto.Response`

### GET `/api/exercises?name={keyword}`
Query param: `name` (optional)
Response `200`: `ExerciseDto.Response[]`

### GET `/api/exercises/category/{categoryId}`
Path param: `categoryId` (Long)
Response `200`: `ExerciseDto.Response[]`

### GET `/api/exercises/{id}`
Path param: `id`
Response `200`: `ExerciseDto.Response`

### DELETE `/api/exercises/{id}`
Path param: `id`
Response `204`

## 8) Workout Programs API

### POST `/api/workout-programs`
Request body:
```json
{
  "name": "Fat Burn 30m",
  "level": "BEGINNER",
  "estimatedDuration": 30,
  "description": "Chuong trinh dot mo",
  "exercises": [
    {
      "exerciseId": 1,
      "orderIndex": 1,
      "sets": 3,
      "reps": 15,
      "restTimeSec": 45
    }
  ]
}
```
Response `201`: `WorkoutProgramDto.Response`

### GET `/api/workout-programs`
Query param optional: `level` (`BEGINNER|INTERMEDIATE|PRO`)
Response `200`: `WorkoutProgramDto.Response[]`

### GET `/api/workout-programs/{id}`
Path param: `id`
Response `200`: `WorkoutProgramDto.Response`

### DELETE `/api/workout-programs/{id}`
Path param: `id`
Response `204`

## 9) Meal Plans API

### POST `/api/meal-plans/generate?userId={userId}&date={YYYY-MM-DD}`
Tự động tạo Meal Plan ứng dụng AI Recommendation dựa theo hồ sơ sức khoẻ (hệ thống tự lấy health profile mới nhất).

Query param:
- `userId` (Long): ID của user
- `date` (LocalDate): Ngày áp dụng gợi ý (vd: "2026-03-21")

Response `201`: `MealPlanDto.Response` (trả về danh sách foods gồm Sáng, Trưa, Tối đã được AI tính toán phù hợp).

### POST `/api/meal-plans`
Request body (system suggestion):
```json
{
  "name": "Low Carb Day",
  "description": "Mau an cho 1 ngay",
  "type": "SYSTEM_SUGGESTION",
  "foods": [
    {
      "foodId": 2,
      "mealType": "BREAKFAST",
      "servingQty": 1.0
    }
  ]
}
```
Request body (user custom, can `userId`):
```json
{
  "name": "Ke hoach cua toi",
  "description": "Danh cho cat mo",
  "type": "USER_CUSTOM",
  "userId": 1,
  "foods": [
    {
      "foodId": 5,
      "mealType": "LUNCH",
      "servingQty": 1.5
    }
  ]
}
```
Response `201`: `MealPlanDto.Response`

Rule: neu `type = USER_CUSTOM` thi bat buoc co `userId`.

### GET `/api/meal-plans`
Response `200`: `MealPlanDto.Response[]`

### GET `/api/meal-plans/system`
Response `200`: `MealPlanDto.Response[]`

### GET `/api/meal-plans/user/{userId}`
Path param: `userId`
Response `200`: `MealPlanDto.Response[]`

### GET `/api/meal-plans/{id}`
Path param: `id`
Response `200`: `MealPlanDto.Response`

### DELETE `/api/meal-plans/{id}`
Path param: `id`
Response `204`

### POST `/api/meal-plans/{mealPlanId}/apply?userId={userId}&date={YYYY-MM-DD}`
Path param: `mealPlanId`
Query param: `userId` (Long), `date` (LocalDate)
Response `200`: Ap dung tat ca thuc an trong meal plan vao nhat ky an uong (Food Logs) cua ngay chi dinh.

## 10) Workout Logs API

### POST `/api/users/{userId}/workout-logs`
Path param: `userId`
Request body (tap bai le):
```json
{
  "exerciseId": 1,
  "durationMin": 30,
  "caloriesBurned": null,
  "logDate": "2026-03-21",
  "note": "Tap buoi sang"
}
```
Request body (tap theo program):
```json
{
  "programId": 2,
  "durationMin": 45,
  "logDate": "2026-03-21",
  "note": "Theo giao an"
}
```
Response `201`: `WorkoutLogDto.Response`

Rule: phai co it nhat 1 trong 2 field `exerciseId` hoac `programId`.

### GET `/api/users/{userId}/workout-logs?date=yyyy-MM-dd`
Path param: `userId`
Query param: `date`
Response `200`: `WorkoutLogDto.Response[]`

### DELETE `/api/users/{userId}/workout-logs/{logId}`
Path param: `userId`, `logId`
Response `204`

## 11) Step Logs API

### POST `/api/users/{userId}/step-logs`
Path param: `userId`
Request body:
```json
{
  "stepCount": 8500,
  "logDate": "2026-03-21"
}
```
Response `201`: `StepLogDto.Response`

Note: voi cung `userId + logDate`, he thong update ban ghi cu (khong tao moi).

### GET `/api/users/{userId}/step-logs/date?date=yyyy-MM-dd`
Path param: `userId`
Query param: `date`
Response `200`: `StepLogDto.Response`

### GET `/api/users/{userId}/step-logs?from=yyyy-MM-dd&to=yyyy-MM-dd`
Path param: `userId`
Query param: `from`, `to`
Response `200`: `StepLogDto.Response[]`

## 12) Water Logs API

### POST `/api/users/{userId}/water-logs`
Path param: `userId`
Request body:
```json
{
  "amountMl": 250,
  "logTime": "2026-03-21T09:30:00"
}
```
Response `201`:
```json
{
  "id": 120,
  "userId": 1,
  "amountMl": 250,
  "logTime": "2026-03-21T09:30:00",
  "logDate": "2026-03-21"
}
```

### GET `/api/users/{userId}/water-logs?date=yyyy-MM-dd`
Path param: `userId`
Query param: `date`
Response `200`: `WaterLogDto.Response[]`

### GET `/api/users/{userId}/water-logs/total?date=yyyy-MM-dd`
Path param: `userId`
Query param: `date`
Response `200`:
```json
2000
```

### DELETE `/api/users/{userId}/water-logs/{logId}`
Path param: `userId`, `logId`
Response `204`

## 13) Daily Summaries API

### GET `/api/users/{userId}/daily-summaries/today`
Path param: `userId`
Response `200`: `DailySummaryDto`

### GET `/api/users/{userId}/daily-summaries?date=yyyy-MM-dd`
Path param: `userId`
Query param: `date`
Response `200`: `DailySummaryDto`

### GET `/api/users/{userId}/daily-summaries/history?from=yyyy-MM-dd&to=yyyy-MM-dd`
Path param: `userId`
Query param: `from`, `to`
Response `200`: `DailySummaryDto[]`

### POST `/api/users/{userId}/daily-summaries/refresh?date=yyyy-MM-dd`
Path param: `userId`
Query param: `date` (optional, mac dinh hom nay)
Response `200`: `DailySummaryDto`

`DailySummaryDto` mau:
```json
{
  "userId": 1,
  "logDate": "2026-03-21",
  "totalCalIn": 1800.0,
  "totalCalOut": 350.0,
  "netCalories": 1450.0,
  "totalWater": 2200,
  "totalSteps": 9000,
  "isGoalAchieved": true
}
```

---

## 14) Statistics API

### GET `/api/users/{userId}/statistics/nutrition/daily?date=yyyy-MM-dd`
Path param: `userId`
Query param: `date`
Response `200`: `StatisticsDto.DailyNutritionResponse`
Dùng để lấy chi tiết dinh dưỡng (Protein, Carbs, Fat) của 1 ngày.

### GET `/api/users/{userId}/statistics/nutrition?from=yyyy-MM-dd&to=yyyy-MM-dd&groupBy={DAY/WEEK/MONTH}`
Path param: `userId`
Query param: `from`, `to`, `groupBy` (mặc định DAY)
Response `200`: `StatisticsDto.NutritionSeriesResponse`
Dùng để vẽ đồ thị dinh dưỡng (lượng calo in) qua từng thời kỳ.

### GET `/api/users/{userId}/statistics/weight?from=yyyy-MM-dd&to=yyyy-MM-dd&groupBy={DAY/WEEK/MONTH}`
Path param: `userId`
Query param: `from`, `to`, `groupBy` (mặc định DAY)
Response `200`: `StatisticsDto.WeightSeriesResponse`
Dùng để vẽ đồ thị thống kê cân nặng qua từng thời kỳ.

---

## 15) Ghi chu nhanh de test API

- Cac API theo user deu can `userId` ton tai truoc.
- Cac API co FK (`foodId`, `exerciseId`, `programId`) can record ton tai.
- Nhieu API ghi log se auto cap nhat `daily_summaries` ngay sau khi tao/xoa log.
- De test nhanh, thu tu nen la: tao user -> tao health profile -> tao food/exercise/program -> tao logs -> lay daily summary.

---

## 16) Chi tiet tinh toan Health Profile (NEW)

### 16.1) Cong thuc chinh

**BMR (Mifflin-St Jeor):**
(Su dung can nang HIEN TAI `weightKg`, khong phai desired)
```
Nam: BMR = 10 * W + 6.25 * H - 5 * A + 5
Nu:  BMR = 10 * W + 6.25 * H - 5 * A - 161
```
Trong do:
- `W`: `weightKg` hien tai
- `H`: `heightCm`
- `A`: tuoi

**TDEE (Luong calo duy tri can nang hien tai):**
```
TDEE = BMR * ActivityLevelMultiplier
```

**Daily calories (luong calo tham hut/du thua can thiet 1 ngay):**
```
dailyCalories = (abs(desiredWeightKg - weightKg) * 7700) / targetDays
```

**Target calories (tong luong calo nap vao moi ngay):**
```
LOSE_WEIGHT: targetCalories = TDEE - dailyCalories
GAIN_WEIGHT: targetCalories = TDEE + dailyCalories
MAINTAIN:    targetCalories = TDEE
```

Vi du giong theo yeu cau:
Nam, 22 tuoi, 60kg, 170cm, hoat dong Vua phai, muc tieu Tang can (len 62kg trong 38 ngay):
```
BMR  = 10*60 + 6.25*170 - 5*22 + 5 = 1557.5 kcal
TDEE = 1557.5 * 1.55 = 2414.12 kcal
dailyCalories = (|62 - 60| * 7700) / 38.5 ~ 400 kcal (luong calo nap VUO TDEE)
targetCalories = 2414.12 + 400 = 2814.12 kcal/ngay (luong an vao toi thieu)
```

### 16.2) Danh gia muc do kho tu theo luong calo thieu/du (dailyCalories)

- `EASY`: `dailyCalories <= 300` kcal
- `MEDIUM`: `300 < dailyCalories <= 700` kcal
- `HARD`: `dailyCalories > 700` kcal

Response object:
```json
"dailyCalorieBreakdown": {
  "dailyCalories": 400.0,
  "difficultyLevel": "MEDIUM",
  "note": "Muc tieu trung binh, can theo doi va ky luat"
}
```

### 16.3) Validation theo goal

- `LOSE_WEIGHT`: `desiredWeightKg < weightKg`
- `GAIN_WEIGHT`: `desiredWeightKg > weightKg`
- `MAINTAIN`: `abs(desiredWeightKg - weightKg) <= weightKg * 0.02`

### 16.4) Vi du nhanh (TANG CAN)

Input:
```json
{
  "goalType": "GAIN_WEIGHT",
  "gender": "MALE",
  "dob": "2004-01-01",
  "heightCm": 170,
  "weightKg": 60,
  "activityLevel": "MODERATELY_ACTIVE",
  "desiredWeightKg": 62,
  "targetDays": 38
}
```

Vi du ket qua:
```json
{
  "targetCalories": 2819.38,
  "dailyCalories": 405.26,
  "dailyCalorieBreakdown": {
    "dailyCalories": 405.26,
    "difficultyLevel": "MEDIUM",
    "note": "Muc tieu trung binh, can theo doi va ky luat"
  }
}
```

### 16.5) Loi thuong gap

```json
{
  "status": 400,
  "message": "Desired weight must be less than current weight for LOSE_WEIGHT goal",
  "timestamp": "2026-03-26T10:00:00"
}
```

```json
{
  "status": 400,
  "message": "targetDays is required and must be greater than 0"
}
```
