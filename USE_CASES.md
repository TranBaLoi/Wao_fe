# Use Cases — WAO (FE + BE)

Tài liệu tóm tắt tất cả use case chính trong dự án WAO (frontend Android + backend). Mỗi use case gồm: mô tả ngắn, actor, luồng chính, endpoint backend liên quan (nếu có) và tham chiếu file FE/BE trong repository.

---

Hướng dẫn: Nếu cần mức chi tiết hơn (user story + mock request/response hoặc mapping file → dòng code), nói tôi sẽ mở rộng.

Mục lục
- Authentication & Account
- User Management
- Health Profile
- Foods & Nutrition
- Food Logs
- Meal Plans
- Exercises
- Workout Programs
- Workout Tracking (Live)
- Workout Logs (Saved)
- Workout Journal & History
- Steps Tracking & Step Chart
- Water Logs
- Daily Summaries
- Statistics
- Media / Avatar Upload
- Admin / Moderation
- Integrations & Permissions

---

1) Authentication & Account
- Mô tả: Đăng ký, đăng nhập (email/password), Google login/verify/reset, đổi mật khẩu, xóa tài khoản.
- Actor: Người dùng
- Luồng chính:
  - Đăng ký: gửi email/password/fullName → tạo user
  - Đăng nhập: gửi email/password → trả về user (hiện chưa có token trong code)
  - Google login: gửi idToken → backend xác thực
  - Thay đổi mật khẩu / verify / reset
- Endpoints (BE):
  - POST `/api/users/register`
  - POST `/api/users/google-login`
  - POST `/api/users/verify`
  - PUT `/api/users/{id}/password`
  - GET/PUT/DELETE `/api/users`, `/api/users/{id}`
- FE files: `SplashActivity.kt`, `LoginActivity.kt`, `RegisterActivity.kt`, layouts: `activity_login.xml`, `activity_register.xml`.
- BE files: `UserController` (`src/main/java/.../controller/UserController.java`) — tham khảo `API_REFERENCE.md` và `PROJECT_CONTEXT.md`.

2) User Management (profile)
- Mô tả: Xem/sửa thông tin user, upload avatar.
- Actor: Người dùng
- Luồng chính: xem profile, cập nhật tên/status, upload avatar (multipart)
- Endpoints:
  - GET `/api/users/{id}`
  - PUT `/api/users/{id}`
  - POST `/api/users/upload-avatar` or POST `/api/users/{id}/avatar`
- FE files: `SettingsActivity.kt` (mục Account), profile screens.
- BE: `UserController`

3) Health Profile
- Mô tả: Tạo/cập nhật hồ sơ sức khỏe (giới tính, DOB, chiều cao, cân nặng, mức hoạt động, goal) và tính toán caloric targets (BMR/TDEE/dailyCalories).
- Actor: Người dùng
- Luồng chính: user gửi health profile → backend tính `targetCalories`, `dailyCalories`, `dailyCalorieBreakdown` → lưu và trả về
- Endpoints:
  - POST `/api/users/{userId}/health-profiles`
  - GET `/api/users/{userId}/health-profiles/latest`
  - GET `/api/users/{userId}/health-profiles/history`
- FE files: Forms nơi user nhập thông tin (FE health profile screen)
- BE: `HealthProfileController` + logic tính toán (BMR/TDEE) — tham khảo `API_REFERENCE.md` (phần Chi tiet tinh toan Health Profile)

4) Foods & Nutrition (Food catalog)
- Mô tả: Tạo, duyệt, tìm kiếm, cập nhật, xóa món ăn (kèm dinh dưỡng, featureVector, allergens)
- Actor: Người dùng / Admin
- Luồng chính: Tạo food (user tạo => isVerified=false; admin tạo => isVerified=true); tìm kiếm bằng tên; CRUD.
- Endpoints:
  - POST `/api/foods` (user)
  - POST `/api/foods/admin` (admin)
  - GET `/api/foods?name=` (search)
  - GET `/api/foods/{id}`
  - PUT `/api/foods/{id}`
  - DELETE `/api/foods/{id}`
- FE files: Food search & add screens (FE food related files)
- BE: `FoodController`

5) Food Logs (Nhật ký ăn uống)
- Mô tả: Ghi nhận bữa ăn hàng ngày (liên kết food, mealType, servingQty, logDate) → ảnh hưởng DailySummary
- Actor: Người dùng
- Luồng chính: user chọn food + mealType + servingQty + logDate → POST → backend tính `totalCalories` và trả về; hệ thống cập nhật `daily_summaries`
- Endpoints:
  - POST `/api/users/{userId}/food-logs`
  - GET `/api/users/{userId}/food-logs` (by date)
  - DELETE `/api/users/{userId}/food-logs/{logId}`
- FE files: Eat/Log screens
- BE: `FoodLogController`

6) Meal Plans
- Mô tả: Tạo meal plan hệ thống hoặc người dùng; AI generate meal plan từ health profile; apply meal plan → tạo nhiều food logs cho 1 ngày
- Actor: Người dùng, System
- Luồng chính: 
  - Generate: POST `/api/meal-plans/generate?userId=&date=` → backend lấy health profile mới nhất và trả meal plan gợi ý
  - Create: POST `/api/meal-plans` (USER_CUSTOM or SYSTEM_SUGGESTION)
  - Get all/system/user/{userId}/by id, delete
  - Apply: POST `/api/meal-plans/{mealPlanId}/apply?userId=&date=` → apply foods as food logs
- Endpoints: nhiều như trên (xem `API_REFERENCE.md`)
- FE files: Meal plan screens
- BE: `MealPlanController`

7) Exercises
- Mô tả: Quản lý bài tập (name, category, video, caloriesPerMin)
- Actor: Người dùng / Admin
- Luồng chính: CRUD exercises, tìm kiếm, lấy theo category
- Endpoints:
  - POST `/api/exercises`
  - GET `/api/exercises?name=`; `/api/exercises/category/{categoryId}`; `/api/exercises/{id}`
  - DELETE `/api/exercises/{id}`
- FE files: mapping exercise list UI
- BE: `ExerciseController`

8) Workout Programs
- Mô tả: Tạo chương trình tập (chứa nhiều exercises với sets/reps/rest)
- Actor: Người dùng / Admin
- Luồng chính: CRUD workout programs, filter by level
- Endpoints:
  - POST `/api/workout-programs`
  - GET `/api/workout-programs` (optional level)
  - GET `/api/workout-programs/{id}`
  - DELETE `/api/workout-programs/{id}`
- FE files: Program UI
- BE: `WorkoutProgramController`

9) Workout Tracking (Live mobile tracking)
- Mô tả: FE live-tracking cho 4 shortcuts: Di bo (outdoor walking), Chay bo ngoai troi (outdoor running), Chay bo trong nha (indoor running), Dap xe (cycling). Ghi distance, speed, steps, heart rate, estimate calories. State machine: IDLE → RUNNING → PAUSED → SAVING. Hold 3s to finish. Short sessions <3min show popup.
- Actor: Người dùng (mobile device sensors)
- Luồng chính:
  - Mở `WorkoutTrackingActivity` với `WorkoutType`
  - Xin permissions: location, activity recognition, read steps (Health Connect), heart rate access
  - Start: record GPS or step sensor/Health Connect stream
  - Pause/Resume
  - Hold to finish → if >=3min → save; else popup “too short”
  - Save: POST `/api/users/{userId}/workout-logs` with contract containing `workoutType`, `startedAt`, `endedAt`, `distanceMeters`, `durationMin`, `caloriesBurned`, `stepCount`, `avgSpeedKmh`, `note` (metadata)
- FE files: `WorkoutTrackingActivity.kt`, `WorkoutType.kt`, layouts `activity_workout_tracking.xml`
- BE: `WorkoutLogController` (POST `/api/users/{userId}/workout-logs`)
- Notes: Outdoor uses GPS for distance/speed; indoor uses step counter/estimate via stride length factor; heart rate from Health Connect; calories prefer active calories from Health Connect else estimate (prefixed ~)

10) Workout Logs (Saved)
- Mô tả: Lưu và quản lý logs đã ghi (exerciseId or programId optional in new contract); backend supports saving new FE contract.
- Actor: Người dùng
- Luồng chính: Save workout → view list by date → delete
- Endpoints:
  - POST `/api/users/{userId}/workout-logs`
  - GET `/api/users/{userId}/workout-logs?date=`
  - DELETE `/api/users/{userId}/workout-logs/{logId}`
- FE files: Save flow in `WorkoutTrackingActivity`, journal/histories
- BE files: `WorkoutLogController`

11) Workout Journal & History
- Mô tả: FE tổng hợp logs theo exercise để hiển thị journal (30 ngày gần nhất) và history per exercise
- Actor: Người dùng
- Luồng chính: FE đọc từ backend workout logs by date; vì BE hiện chỉ hỗ trợ per-day logs, FE assembles journal from last 30 days and may parse metadata in `note` for display
- FE files: `WorkoutJournalActivity.kt`, `WorkoutHistoryActivity.kt`, `WorkoutJournalRepository.kt`
- BE: `/api/users/{userId}/workout-logs` endpoints

12) Steps Tracking & Step Chart
- Mô tả: Hiển thị biểu đồ bước chân (today / yesterday) theo timeline/giờ
- Actor: Người dùng
- Luồng chính: FE đọc step timeline từ Health Connect via `HealthConnectRepository` → render chart in `StepsTrendActivity`.
- Permissions: READ_STEPS (Health Connect)
- FE files: `StepsTrendActivity.kt`, `health/HealthConnectRepository.kt`, `health/HealthConnectManager.kt`
- BE: None required (FE-only flow)

13) Water Logs
- Mô tả: Ghi nước uống (ml + logTime) và lấy tổng trong ngày
- Actor: Người dùng
- Luồng chính:
  - POST `/api/users/{userId}/water-logs` (amountMl, logTime)
  - GET `/api/users/{userId}/water-logs?date=`
  - GET `/api/users/{userId}/water-logs/total?date=`
  - DELETE `/api/users/{userId}/water-logs/{logId}`
- FE files: water log screens
- BE: `WaterLogController`

14) Daily Summaries (Dashboard quick)
- Mô tả: Tổng kết ngày (cal in/out, net, water, steps, goal) — dùng cho dashboard
- Actor: Người dùng
- Luồng chính:
  - GET `/api/users/{userId}/daily-summaries/today`
  - GET `/api/users/{userId}/daily-summaries?date=`
  - GET `/api/users/{userId}/daily-summaries/history?from=&to=`
  - POST `/api/users/{userId}/daily-summaries/refresh?date=` (recalculate)
- BE: `DailySummaryController`
- FE files: `MainActivity.kt` dashboard areas `tvCalIn`, `tvCalOut`, `tvCalRemaining`, `pbCalories` (muốn hiển thị fast summary)

15) Statistics
- Mô tả: Thống kê nutrition (daily/grouped) và weight series
- Actor: Người dùng
- Luồng chính: GET series endpoints for charting
- Endpoints:
  - GET `/api/users/{userId}/statistics/nutrition/daily?date=`
  - GET `/api/users/{userId}/statistics/nutrition?from=&to=&groupBy=`
  - GET `/api/users/{userId}/statistics/weight?from=&to=&groupBy=`
- FE files: chart screens
- BE: Statistics controllers (được mô tả trong API_REFERENCE)

16) Media / Avatar Upload
- Mô tả: Upload avatar multipart
- Actor: Người dùng
- Endpoints:
  - POST `/api/users/upload-avatar` (form-data file)
  - POST `/api/users/{id}/avatar` (form-data file)
- BE: `UserController` supports upload endpoints

17) Admin / Moderation
- Mô tả: Admin tạo/verify foods (`/api/foods/admin`), quản lý exercises/programs
- Actor: Admin
- Endpoints: admin variants (see Foods, Exercises, Workout Programs controllers)

18) Integrations & Permissions
- Google Sign-in: `google-services.json` present (client ids & api key) — FE supports Google login flows (endpoint `/api/users/google-login`)
- Health Connect: quyền READ_STEPS, Heart Rate, Active calories — FE uses `HealthConnectManager` + `HealthConnectRepository`
- GPS / Location: required for outdoor distance/speed
- Activity Recognition: required for indoor/outdoor classification
- Files: `app/src/main/src/.../health/*`, `FEATURE_2_WORKOUT_TRACKING_DOCS.md`, `android manifest` entries

19) Misc / Settings
- Mô tả: Settings, navigation between screens (MainActivity, SettingsActivity), theme, design system
- FE files: `MainActivity.kt`, `SettingsActivity.kt`, themes/colors in resources

---

Ghi chú & Next steps
- Đây là bản tóm tắt toàn diện ở mức feature/use case. Nếu bạn muốn tôi chuyển sang bước tiếp theo (ví dụ: tạo `USE_CASES.json` / generate user stories + acceptance criteria / mapping file → endpoint → line number), chọn 1 trong các lựa chọn sau:
  - A: Xuất user stories chi tiết + request/response mẫu cho mỗi use case (recommended)
  - B: Tạo mapping file `USE_CASES_FULL_MAP.md` với đường dẫn file FE → controller BE cho từng use case
  - C: Export JSON/CSV để import vào công cụ quản lý yêu cầu

Nếu OK, tôi sẽ tiếp tục tạo file bổ sung theo lựa chọn của bạn.

