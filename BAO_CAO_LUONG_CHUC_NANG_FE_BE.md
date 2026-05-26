# BAO CAO LUONG HOAT DONG CHUC NANG (FE -> BE)

## 1. Pham vi va nguon doi chieu

Bao cao nay tong hop luong chuc nang cua du an Android `Wao_fe` tu FE den BE dua tren:

- FE code trong `app/src/main/java/com/example/wao_fe/**`
- Hop dong API trong `app/src/main/java/com/example/wao_fe/network/ApiService.kt`
- DTO FE trong `app/src/main/java/com/example/wao_fe/network/models/Models.kt`
- Tai lieu API: `API_REFERENCE.md`
- Context backend snapshot: `PROJECT_CONTEXT.md` (co controller + dto BE)

Luu y quan trong:
- Workspace hien tai la FE, khong co source BE day du dang module runnable.
- Phan BE trong bao cao duoc suy ra tu `API_REFERENCE.md` va cac doan code BE nhung da duoc embed trong `PROJECT_CONTEXT.md`.

---

## 2. Tong quan kien truc xu ly

### 2.1 Lop FE

- UI layer: cac `Activity` (vd: `MainActivity`, `FoodDiaryActivity`, `MealPlanActivity`, `WorkoutTrackingActivity`, `SettingsActivity`, `ChatbotActivity`)
- ViewModel layer:
  - `MealPlanViewModel`
  - `SettingsViewModel`
- Data access layer:
  - `UserRepository`
  - `ChatbotRepository`
  - `NamStatisticsRepository`
  - goi truc tiep `NetworkClient.apiService` o mot so Activity
- External/local integrations:
  - Health Connect (`HealthConnectManager`, `HealthConnectRepository`)
  - OpenFoodFacts (`OpenFoodFactsApi`)
  - Alarm/Notification (`ReminderManager`, `NotificationHelper`, `AlarmReceiver`, `BootReceiver`)

### 2.2 Lop BE (theo tai lieu + context)

- REST Controllers (theo `PROJECT_CONTEXT.md`):
  - `UserController`, `HealthProfileController`, `FoodController`, `FoodLogController`, `MealPlanController`
  - `ExerciseController`, `WorkoutProgramController`, `WorkoutLogController`
  - `StepLogController`, `WaterLogController`, `DailySummaryController`
- DTOs dau vao/dau ra tuong ung (`UserDto`, `HealthProfileDto`, `FoodDto`, `...`)
- Business service layer (theo ten service trong controller):
  - `UserService`, `HealthProfileService`, `FoodLogService`, `DailySummaryService`, ...

### 2.3 Luong chung FE -> BE

1. UI trigger (click button, onResume, pull data, save)
2. FE tao request model (`CreateFoodLogRequest`, `CreateHealthProfileRequest`, ...)
3. Goi API qua Retrofit (`ApiService`)
4. Nhan response model (`DailySummaryResponse`, `MealPlanResponse`, ...)
5. FE map len UI state + render
6. Neu loi: hien `Toast`/fallback gia tri mac dinh

---

## 3. Cau hinh ket noi va xu ly loi

### 3.1 Base URL

- FE dung `NetworkClient.BASE_URL = "http://10.0.2.2:8080/"` (emulator Android -> localhost may host)

### 3.2 Error handling FE

- `UserRepository`/`ChatbotRepository`: tra ve `ApiResult.Success` / `ApiResult.Error`
- Nhieu Activity goi truc tiep `apiService` thi dung `runCatching {}` + `HttpException`
- Phia BE (theo `API_REFERENCE.md`) tra loi format:
  - `status`, `message`, `fieldErrors`, `timestamp`

---

## 4. Bang endpoint va model vao/ra (FE contract)

Bang duoi day la endpoint quan trong dang duoc FE su dung:

### 4.1 User + Auth

- `POST /api/users/register`
  - FE goi: `ApiService.registerUser(request: RegisterUserRequest)`
  - Input: `email`, `password`, `fullName`
  - Output: `UserResponse { id, email, fullName, status }`

- `POST /api/users/google-login`
  - FE goi: `ApiService.loginWithGoogle(request: GoogleLoginRequest)`
  - Input: `idToken`
  - Output: `UserResponse`

- `POST /api/users/verify`
  - FE goi: `ApiService.verifyEmail(request: VerifyEmailRequest)`
  - Input: `email`, `code`
  - Output: `VerifyEmailResponse { message, userId? }`

- `GET /api/users`, `GET /api/users/{id}`, `PUT /api/users/{id}`, `DELETE /api/users/{id}`
  - FE dung cho login by email, profile, settings.

### 4.2 Health profile

- `POST /api/users/{userId}/health-profiles`
  - FE goi: `createHealthProfile(userId, CreateHealthProfileRequest)`
  - Input chinh: `gender`, `dob`, `heightCm`, `weightKg`, `activityLevel`, `goalType`, `desiredWeightKg`, `targetDays`, `allergies`, `preferenceVector?`
  - Output: `HealthProfileResponse` (co `targetCalories`, `dailyCalories`, `dailyCalorieBreakdown`)

- `GET /api/users/{userId}/health-profiles/latest`
- `GET /api/users/{userId}/health-profiles/history`

### 4.3 Food + Food log

- `GET /api/foods/search?name=`
- `GET /api/foods/{id}`
- `POST /api/foods` (multipart: part `food` JSON + `images[]`)
- `PUT /api/foods/{id}` (multipart)

- `POST /api/users/{userId}/food-logs`
  - Input: `CreateFoodLogRequest { foodId, mealType, servingQty, logDate }`
  - Output: `FoodLogResponse`

- `GET /api/users/{userId}/food-logs/by-date?date=...`
- `DELETE /api/users/{userId}/food-logs/{logId}`

### 4.4 Meal plan

- `POST /api/meal-plans/generate?userId=&date=`
- `POST /api/meal-plans`
- `PUT /api/meal-plans/{id}`
- `GET /api/meal-plans/user/{userId}`
- `GET /api/meal-plans/{id}`
- `DELETE /api/meal-plans/{id}`
- `POST /api/meal-plans/{mealPlanId}/apply`
  - FE body: `ApplyMealPlanRequest { userId, logDate, transientFoods? }`

### 4.5 Workout / Exercise / Programs

- `GET /api/exercises?name=`
- `POST /api/exercises`
- `POST /api/users/{userId}/workout-logs`
  - Input: `CreateWorkoutLogRequest { exerciseId?, programId?, durationMin, caloriesBurned?, logDate, note? }`
  - Output: `WorkoutLogResponse`

### 4.6 Step / Water / Daily summary

- `POST /api/users/{userId}/step-logs`
- `GET /api/users/{userId}/step-logs/date?date=`
- `GET /api/users/{userId}/step-logs?from=&to=`

- `POST /api/users/{userId}/water-logs`
- `GET /api/users/{userId}/water-logs?date=`
- `GET /api/users/{userId}/water-logs/total?date=`
- `DELETE /api/users/{userId}/water-logs/{logId}`

- `GET /api/users/{userId}/daily-summaries/today`
- `GET /api/users/{userId}/daily-summaries?date=`
- `GET /api/users/{userId}/daily-summaries/history?from=&to=`
- `POST /api/users/{userId}/daily-summaries/refresh?date=`

### 4.7 Statistics (Nam)

- `GET /api/users/{userId}/statistics/nutrition/daily?date=`
- `GET /api/users/{userId}/statistics/nutrition?from=&to=&groupBy=`
- `GET /api/users/{userId}/statistics/weight?from=&to=&groupBy=`
- `GET /api/users/{userId}/statistics/weight/latest`
- `POST /api/users/{userId}/statistics/weight/logs`

### 4.8 Chatbot

- `GET /api/users/{userId}/chat/conversations`
- `GET /api/users/{userId}/chat/conversations/{conversationId}`
- `DELETE /api/users/{userId}/chat/conversations/{conversationId}`
- `POST /api/users/{userId}/chat/messages`
  - Input: `ChatbotSendMessageRequest { conversationId?, message }`
  - Output: `ChatbotSendMessageResponse { conversationId, assistantMessageId, answer, createdAt }`

---

## 5. Luong chuc nang chi tiet theo man hinh

## 5.1 Splash + Auth

### A. Splash

- File: `SplashActivity.kt`
- Ham chinh:
  - `animateProgress()`
  - `routeUser()`
- Input:
  - `SharedPreferences AppPrefs.USER_ID`
- Output dieu huong:
  - `USER_ID != -1` -> `MainActivity`
  - nguoc lai -> `LoginActivity`

### B. Dang ky

- File: `RegisterActivity.kt`
- Ham chinh:
  - `validateInputs()`
  - `performRegister()`
- Luong:
  1. Validate fullname/email/password/confirm
  2. Goi `userRepository.register(RegisterUserRequest)`
  3. Thanh cong -> mo `VerifyEmailActivity` + pass `email`

### C. Verify email

- File: `VerifyEmailActivity.kt`
- Ham chinh:
  - `performVerification(code)`
- Input:
  - `email` tu Intent + `code` user nhap
- API:
  - `userRepository.verifyEmail(email, code)`
- Output:
  - thanh cong -> ve `LoginActivity` (clear task)

### D. Dang nhap email / Google

- File: `LoginActivity.kt`
- Ham chinh:
  - `performLogin()` (email)
  - `performGoogleLogin(idToken)`
  - `checkHealthProfileAndNavigate(userId, fullName)`
- Luong:
  1. Email login hien tai khong goi endpoint login password, ma goi `getUsers()` roi tim email (`loginByEmail`)
  2. Google login goi `POST /api/users/google-login`
  3. Save session vao `AppPrefs`
  4. Goi `getLatestHealthProfile(userId)`:
     - co profile -> `MainActivity`
     - chua co profile -> `UserInfoActivity`

---

## 5.2 Onboarding ho so suc khoe

Chuoi man hinh:
`UserInfoActivity -> BodyIndicesActivity -> GoalSelectionActivity -> AllergiesActivity -> FinalSetupActivity`

### A. UserInfoActivity

- Thu thong tin gioi tinh + tuoi (swipe picker)
- Truyen tiep qua Intent: `USER_ID`, `GENDER_ID`, `AGE`

### B. BodyIndicesActivity

- Thu `HEIGHT`, `WEIGHT` + tinh BMI local (`calculateAndDisplayBMI()`)
- Chua goi API tai day
- Continue -> `GoalSelectionActivity`

### C. GoalSelectionActivity

- Thu muc tieu + `DESIRED_WEIGHT`
- Validate local theo rule:
  - giam can: desired < current
  - giu can: within +-2%
  - tang can: desired > current
- Continue -> `AllergiesActivity`

### D. AllergiesActivity

- Thu list allergy chips, join chuoi `ALLERGIES`
- Continue -> `FinalSetupActivity`

### E. FinalSetupActivity

- Ham chinh: `createHealthProfile()`
- Mapping input:
  - `genderId -> Gender`
  - `goalId -> GoalType`
  - `selectedActivityIndex -> ActivityLevel`
  - `age -> dob` (tam thoi `birthYear-01-01`)
  - `selectedDurationWeeks * 7 -> targetDays`
- API goi:
  - `POST /api/users/{userId}/health-profiles`
- Output hien thi:
  - `targetCalories`
  - `dailyCalories`
  - `dailyCalorieBreakdown { difficultyLevel, note }`
- Sau khi tinh xong user bam lan 2 de vao `MainActivity`

---

## 5.3 Home Dashboard + Health Connect + Notification

### A. Tai dashboard

- File: `MainActivity.kt`
- Ham chinh: `fetchDashboardData()`
- API chain:
  1. `getLatestHealthProfile` -> lay `targetCaloriesGoal`
  2. `getFoodLogs(today)` + `getFoods()` -> tinh macros
  3. `getTodaySummary` -> set:
     - `totalCalIn`, `totalCalOut`, `netCalories`, `totalWater`, `totalSteps`
- Xu ly UI:
  - progress calories
  - canh bao vuot calorie bang `NotificationHelper.showAlertNotification(...)`

### B. Nuoc uong

- Ham:
  - `addWaterLog(amount)` -> `POST /water-logs`
  - `removeLastWaterLog()` -> `GET /water-logs` roi `DELETE log moi nhat`

### C. Health Connect

- Ham trung tam: `checkHealthConnectAccess(promptIfMissing, initiatedByUser)`
- Luong:
  1. check SDK status
  2. neu can install -> dialog mo Play Store
  3. neu available -> check granted permissions
  4. du quyen -> `loadHealthConnectMetrics()`
- Doc metric qua `HealthConnectRepository.readTodaySnapshot()`:
  - `stepsToday`
  - `activeCaloriesBurnedToday`
  - `latestHeartRateBpm`
- Render:
  - `renderStepsCard()` uu tien Health Connect, fallback backend
  - `renderHeartRateCard()` co/khong co data

### D. Notification reminder

- `askNotificationPermission()` xin POST_NOTIFICATIONS
- `ReminderManager.setupAllReminders()` dat exact alarms
- `AlarmReceiver` nhan alarm -> hien notification + re-schedule ngay hom sau
- `BootReceiver` dat lai reminders sau reboot

---

## 5.4 Food Diary + Food Search + Add Food + Food Detail

### A. FoodDiaryActivity

- Ham chinh:
  - `loadDiaryData()`
  - `fetchTargetCalories()`
  - `fetchFoods()`
  - `fetchLogsByDate()`
  - `updateTopCard()`, `renderMeals()`, `updateMacros()`
- API:
  - `getLatestHealthProfile`, `getFoods`, `getFoodLogs`, `getTodaySummary`
- Delete mon:
  - `deleteFoodLog(userId, logId)`

### B. FoodSearchActivity

- Ham chinh:
  - `loadFoods()`
  - `renderResults()`
  - `addFoodToMeal(food)`
- Input quan trong:
  - `EXTRA_USER_ID`, `EXTRA_MEAL_TYPE`
- API save log:
  - `createFoodLog(CreateFoodLogRequest)`

### C. AddFoodActivity

- Ham chinh: `submitCreateFood()`
- Input:
  - name, servingSize, calories, carbs, protein, fat, imageUri?
- Xu ly:
  - build part `food` JSON (`FoodRequest`)
  - build part `images[]` neu co
- API:
  - `createFood(multipart)`

### D. FoodDetailActivity

- Input:
  - `EXTRA_FOOD_ID`, `EXTRA_FOOD_NAME`
- API:
  - `getFoodById(foodId)`
- Output:
  - show calories/macros/serving/image

### E. Barcode/OpenFoodFacts flow (Main)

- `startBarcodeScanner()` -> scan
- `fetchProductInfo(barcode)` goi `OpenFoodFactsApi`
- `checkAndShowFoodDialog(...)` check trung ten voi BE `getFoods(name)`
- Hanh dong:
  - them food vao CSDL (`createFood`) hoac
  - them va log ngay (`createFoodLog`)

---

## 5.5 Meal Plan (AI suggestion + draft + save + apply)

### A. State machine

- File: `MealPlanViewModel.kt`
- States:
  - `Idle`, `Loading`, `SuggestionReady`, `DraftReady`, `SavingDraft`, `DraftSaved`, `Error`

### B. Tao goi y

- `generateMealPlan(userId)`
  - date = today `yyyy-MM-dd`
  - API: `generateMealPlan(userId, date)`
  - output: `MealPlanResponse`

### C. Ap dung vao draft

- `applySuggestionToDraft(userId)`
  - convert suggestion -> `MealPlanDraft`

### D. Luu draft

- `saveDraftMealPlan()`
  - neu edit mode: `updateMealPlan(mealPlanId, MealPlanRequest)`
  - neu tao moi: `createMealPlan(MealPlanRequest)`

### E. Quan ly danh sach da luu (MealPlanActivity)

- `loadSavedMealPlans()` -> `getUserMealPlans(userId)`
- cac action:
  - View detail -> `MealPlanDetailActivity`
  - Adjust -> `editSavedMealPlan(...)`
  - Delete -> `deleteMealPlan(id)`
  - Apply to diary -> `applyMealPlan(mealPlanId, ApplyMealPlanRequest{userId, logDate})`

### F. Chi tiet meal plan

- `MealPlanDetailActivity.loadMealPlanDetail(mealPlanId)`
  - API 1: `getMealPlanById`
  - API 2: song song `getFoodById` theo tung foodId de bo sung thong tin

---

## 5.6 Workout tracking (Feature 2)

### A. Entry

- `MainActivity.setupWorkoutShortcuts()` mo `WorkoutTrackingActivity` voi extra `WorkoutType.EXTRA_WORKOUT_TYPE`

### B. WorkoutType presets

- File: `WorkoutType.kt`
- Moi type co:
  - `baseSpeedKmh`, `speedVarianceKmh`, `caloriesPerMinute`, `usesStepMetric`, `stepsPerKm`, `estimatedHeartRateRange`, `exerciseName`

### C. Session state machine

- File: `WorkoutTrackingActivity.kt`
- States:
  - `IDLE`, `RUNNING`, `PAUSED`, `SAVING`
- Ham logic:
  - `startSession()`, `pauseSession()`, `resumeSession()`
  - `startTickLoop()` cap nhat moi giay:
    - tang duration
    - tinh speed, distance, calories estimate
  - hold-to-finish 3s:
    - `startHoldToFinish()`, `completeHoldToFinish()`

### D. Rule save

- `attemptFinishSession()`:
  - neu < 180s -> popup khong cho luu
  - neu >= 180s -> `saveWorkout()`

### E. Save workout len BE

- `saveWorkout()`:
  1. `findOrCreateExerciseId()`:
     - tim `getExercises(workoutType.exerciseName)`
     - neu khong co -> `createExercise(ExerciseRequest)`
  2. `createWorkoutLog(CreateWorkoutLogRequest)`
     - input:
       - `exerciseId`
       - `durationMin`
       - `caloriesBurned` (uu tien gia tri max estimate vs Health Connect delta)
       - `logDate`
       - `note`

### F. Health Connect fallback/uu tien

- neu co quyen:
  - `refreshHealthSnapshot()` cap nhat steps/calories/hr
- tinh hien thi:
  - `displayedCaloriesBurned()` = max(local estimate, health delta)
  - `displayedSteps()` = max(health delta, estimated steps)
  - `displayedHeartRate()` = hr that neu co, neu khong thi estimate

---

## 5.7 Statistics dashboard + cap nhat can nang

### A. StatisticsDashboardActivity

- Du lieu qua `NamStatisticsRepository`
- 3 mode:
  - DAY -> `loadDailySnapshot(userId, date)`
  - WEEK/MONTH -> `loadRangeSnapshot(userId, DateRange)`
- API dung:
  - `getDailyNutrition`
  - `getLatestHealthProfile`
  - `getWeightSeries`
  - (overview co the lay `getTodaySummary`, `getUserById`)
- Chart metric:
  - `WEIGHT`, `PROTEIN`, `CARBS`, `FAT`, `CALORIES`

### B. Weight update

- `UpdateWeightBottomSheet`:
  - API `createWeightLog(userId, CreateWeightLogRequest{date, newWeight})`
  - API `getLatestWeightInfo`
- `WeightLogUpdateActivity` cung cap luong cap nhat can nang day du hon (co note)

---

## 5.8 Chatbot

- File: `ChatbotActivity.kt`
- Repository: `ChatbotRepository`
- Luong:
  1. load history: `getConversations(userId)`
  2. mo conversation: `getConversationDetail(userId, conversationId)`
  3. gui tin: `sendMessage(userId, conversationId?, message)`
     - output co `conversationId` de tiep tuc thread
  4. cap nhat UI typing + append assistant message

---

## 5.9 Settings + Edit profile

### A. Settings

- `SettingsViewModel.loadData(userId)`:
  - `getUserById`
  - `getLatestHealthProfile`
  - `getHealthProfileHistory` (lay oldest profile cho completion date)
- UI hien:
  - goal type, current/target weight, weekly goal, activity level, target calories, completion date
- Logout: clear `AppPrefs`

### B. Edit profile

- `saveChanges()`:
  1. update user name qua `updateUser(userId, UpdateUserRequest)`
  2. update health profile bang cach tao profile moi `createHealthProfile(...)`
     - giu mot so field cu (vd `weightKg`, `preferenceVector`)

---

## 6. Mapping FE ham -> BE endpoint -> input/output

### 6.1 Cum auth/user

- `RegisterActivity.performRegister()`
  - FE ham: `userRepository.register(RegisterUserRequest)`
  - BE endpoint: `POST /api/users/register`
  - In: `email,password,fullName`
  - Out: `UserResponse`

- `VerifyEmailActivity.performVerification()`
  - BE: `POST /api/users/verify`
  - In: `email,code`
  - Out: `VerifyEmailResponse`

- `LoginActivity.performGoogleLogin()`
  - BE: `POST /api/users/google-login`
  - In: `idToken`
  - Out: `UserResponse`

### 6.2 Cum health profile

- `FinalSetupActivity.createHealthProfile()`
  - BE: `POST /api/users/{userId}/health-profiles`
  - In: `CreateHealthProfileRequest`
  - Out: `HealthProfileResponse` (targetCalories/dayCalories/breakdown)

### 6.3 Cum meal plan

- `MealPlanViewModel.generateMealPlan()` -> `POST /api/meal-plans/generate`
- `MealPlanViewModel.saveDraftMealPlan()` -> `POST /api/meal-plans` hoac `PUT /api/meal-plans/{id}`
- `MealPlanActivity.applyMealPlanToDiary()` -> `POST /api/meal-plans/{mealPlanId}/apply`

### 6.4 Cum food diary

- `FoodSearchActivity.addFoodToMeal()` -> `POST /api/users/{userId}/food-logs`
- `FoodDiaryActivity.deleteFoodLog()` -> `DELETE /api/users/{userId}/food-logs/{logId}`
- `AddFoodActivity.submitCreateFood()` -> `POST /api/foods` (multipart)

### 6.5 Cum workout

- `WorkoutTrackingActivity.findOrCreateExerciseId()`
  - `GET /api/exercises?name=`
  - fallback `POST /api/exercises`
- `WorkoutTrackingActivity.saveWorkout()`
  - `POST /api/users/{userId}/workout-logs`

---

## 7. Logic BE suy ra tu controller (tu PROJECT_CONTEXT)

Theo doan backend trong `PROJECT_CONTEXT.md`:

- `FoodLogController.log()`
  - Sau khi tao food log se goi `dailySummaryService.buildAndSave(userId, req.logDate)`
  - Nghia la dashboard tong hop duoc refresh tu dong sau log mon an.

- `WaterLogController.log()/delete()`
  - Sau log/xoa nuoc deu refresh daily summary.

- `WorkoutLogController.log()/delete()`
  - Sau log/xoa workout deu refresh daily summary.

- `StepLogController.log()`
  - upsert theo ngay + refresh daily summary.

- `DailySummaryController`
  - cung cap lay today, theo ngay, theo lich su, va refresh manual.

=> Ket luan: tam BE duoc thiet ke theo huong event sau moi thao tac log se cap nhat bang tong hop ngay (`daily_summaries`).

---

## 8. Diem can luu y va risk ky thuat

1. Login email hien tai khong verify password tren FE (`loginByEmail` chi tim email trong `getUsers()`).
2. Co do lech nho endpoint docs vs FE:
   - FE dung `GET /api/foods/search`
   - mot so cho docs/controller ghi `GET /api/foods` voi query name.
3. `MainActivity.renderCaloriesSummary()` dang dung `backendCaloriesOut`, chua dung `healthConnectActiveCaloriesBurned` de thay the dong bo (du co state).
4. Nhieu luong goi truc tiep `NetworkClient.apiService` o Activity => logic data bi phan tan, kho unit test.
5. FE co 2 luong cap nhat can nang (`UpdateWeightBottomSheet` va `WeightLogUpdateActivity`) can thong nhat UX neu release.
6. Source BE day du khong nam trong workspace nay, nen cac buoc service/repository DB chi co the mo ta o muc hop dong + controller snapshot.

---

## 9. De xuat tiep theo de hoan thien tai lieu ky thuat

1. Tao sequence diagram cho 5 luong lon:
   - Auth
   - Onboarding profile
   - Food diary
   - Meal plan
   - Workout tracking
2. Chuan hoa endpoint docs va `ApiService.kt` (dac biet `/foods`, `/food-logs` path aliases).
3. Tach data layer o cac Activity dang goi truc tiep `apiService` sang repository de de maintain.
4. Bo sung ma tran request/response mau (JSON sample) cho tung endpoint dang dung trong app.
5. Dong bo FE login voi BE auth that (password/token) truoc khi production.

---

## 10. Phu luc: file FE quan trong theo module

- Auth/onboarding:
  - `SplashActivity.kt`, `LoginActivity.kt`, `RegisterActivity.kt`, `VerifyEmailActivity.kt`
  - `UserInfoActivity.kt`, `BodyIndicesActivity.kt`, `GoalSelectionActivity.kt`, `AllergiesActivity.kt`, `FinalSetupActivity.kt`
- Home/dashboard:
  - `MainActivity.kt`
- Nutrition:
  - `FoodDiaryActivity.kt`, `FoodSearchActivity.kt`, `AddFoodActivity.kt`, `FoodDetailActivity.kt`
- Meal plan:
  - `MealPlanActivity.kt`, `MealPlanDetailActivity.kt`, `SavedMealPlansActivity.kt`, `MealPlanViewModel.kt`
- Workout:
  - `WorkoutType.kt`, `WorkoutTrackingActivity.kt`
- Chat:
  - `ChatbotActivity.kt`, `ChatbotRepository.kt`
- Settings/profile/statistics:
  - `SettingsActivity.kt`, `SettingsViewModel.kt`, `EditProfileActivity.kt`
  - `NamStatisticsRepository.kt`, `StatisticsDashboardActivity.kt`, `UpdateWeightBottomSheet.kt`, `WeightLogUpdateActivity.kt`
- Infrastructure:
  - `ApiService.kt`, `Models.kt`, `NetworkClient.kt`, `UserRepository.kt`
  - `HealthConnectManager.kt`, `HealthConnectRepository.kt`
  - `ReminderManager.kt`, `NotificationHelper.kt`, `AlarmReceiver.kt`, `BootReceiver.kt`

---

## 11. Phu luc bo sung: Tac dung cua cac ham chinh theo tung luong

Muc nay bo sung truc tiep cho yeu cau "ham chinh co tac dung gi" trong moi luong. Noi dung o day tap trung vao vai tro ham o FE va diem no ket noi sang BE.

### 11.1 Luong Auth (Dang ky / Xac minh / Dang nhap)

- `RegisterActivity.validateInputs()`
  - Tac dung: chan loi o FE truoc khi goi API (email sai format, password ngan, confirm password khong khop).
- `RegisterActivity.performRegister()`
  - Tac dung: dong goi `RegisterUserRequest`, goi API tao user, neu thanh cong chuyen sang man xac minh email.
- `VerifyEmailActivity.performVerification(code)`
  - Tac dung: gui ma OTP/email code len BE de kich hoat tai khoan, thanh cong thi reset stack ve `LoginActivity`.
- `LoginActivity.performLogin()`
  - Tac dung: xu ly login bang email theo logic hien tai FE (tim user theo email), sau do dieu huong tiep theo.
- `LoginActivity.performGoogleLogin(idToken)`
  - Tac dung: gui Google ID token len BE de dang nhap/tao user qua Google.
- `LoginActivity.checkHealthProfileAndNavigate(userId, fullName)`
  - Tac dung: luu session vao `AppPrefs`, kiem tra user da co health profile hay chua de quyet dinh vao `MainActivity` hay onboarding.

### 11.2 Luong Onboarding health profile

- `UserInfoActivity.updateGenderUI()`
  - Tac dung: dong bo trang thai gioi tinh da chon voi UI card.
- `BodyIndicesActivity.calculateAndDisplayBMI()`
  - Tac dung: tinh BMI local de feedback nhanh cho user, chua goi BE.
- `GoalSelectionActivity.validateDesiredWeightForGoal()`
  - Tac dung: enforce business rule muc tieu can nang ngay tai FE truoc khi gui server.
- `AllergiesActivity.goToFinalSetup()`
  - Tac dung: tong hop danh sach di ung da chon va chuyen het du lieu onboarding sang buoc cuoi.
- `FinalSetupActivity.createHealthProfile()`
  - Tac dung: map toan bo input onboarding thanh `CreateHealthProfileRequest`, goi BE tinh `targetCalories` va luu ho so.
- `FinalSetupActivity.showCalories(profile)`
  - Tac dung: render ket qua tinh toan tu BE (target/day calories + difficulty) de user ra quyet dinh tiep tuc.

### 11.3 Luong Home dashboard

- `MainActivity.fetchDashboardData()`
  - Tac dung: ham tong hop dashboard; keo profile, logs, summary, macros va cap nhat card tong quan.
- `MainActivity.renderCaloriesSummary()`
  - Tac dung: tinh va hien thi calories card (cal in/out/remaining/progress) theo state hien tai.
- `MainActivity.addWaterLog(amount)`
  - Tac dung: ghi nhanh luong nuoc user vua them len BE.
- `MainActivity.removeLastWaterLog()`
  - Tac dung: tim log nuoc moi nhat trong ngay va xoa de ho tro nut "-".
- `MainActivity.fetchWeightTrendData()`
  - Tac dung: tai chuoi can nang de ve trend chart + hien thi thay doi gan nhat.

### 11.4 Luong Health Connect

- `MainActivity.checkHealthConnectAccess(promptIfMissing, initiatedByUser)`
  - Tac dung: ham dieu phoi trung tam cho 3 nhanh: khong ho tro, can cai dat, da san sang + check permission.
- `MainActivity.loadHealthConnectMetrics(healthConnectClient)`
  - Tac dung: doc snapshot metric tu Health Connect va cap nhat state UI.
- `HealthConnectRepository.readTodaySnapshot()`
  - Tac dung: doc steps + active calories (aggregate) va heart rate moi nhat (readRecords), tra ve 1 snapshot thong nhat.
- `MainActivity.renderStepsCard()`
  - Tac dung: uu tien hien thi steps tu Health Connect, fallback backend neu chua co du lieu.
- `MainActivity.renderHeartRateCard()`
  - Tac dung: hien thi nhip tim that neu co; neu khong co thi thong bao trang thai du lieu/quyen.

### 11.5 Luong Food diary

- `FoodDiaryActivity.loadDiaryData()`
  - Tac dung: orchestrate data cho man diary (target calo, food catalog, logs hom nay, summary card, macros).
- `FoodDiaryActivity.fetchLogsByDate(date)`
  - Tac dung: lay nhat ky an theo ngay, co fallback rong neu 404.
- `FoodDiaryActivity.renderMeals(logs)`
  - Tac dung: nhom logs theo `MealType` va do vao tung section bua an.
- `FoodDiaryActivity.updateMacros(logs)`
  - Tac dung: tinh tong protein/carbs/fat da an va so sanh voi target.
- `FoodDiaryActivity.deleteFoodLog(logId)`
  - Tac dung: xoa 1 mon khoi nhat ky va refresh toan man.

### 11.6 Luong tim mon va them mon

- `FoodSearchActivity.loadFoods()`
  - Tac dung: tai catalog foods tu BE lam datasource cho tim kiem.
- `FoodSearchActivity.renderResults()`
  - Tac dung: loc theo keyword + filter chip (all/popular/healthy), render list ket qua.
- `FoodSearchActivity.addFoodToMeal(food)`
  - Tac dung: ghi nhanh food duoc chon vao meal log cua bua an hien tai.
- `AddFoodActivity.submitCreateFood()`
  - Tac dung: validate form, tao payload multipart (`food` + `images`), goi API tao food moi.
- `MainActivity.fetchProductInfo(barcode)`
  - Tac dung: goi OpenFoodFacts lay du lieu dinh duong de ho tro them/log mon tu barcode.

### 11.7 Luong Meal plan

- `MealPlanViewModel.generateMealPlan(userId)`
  - Tac dung: goi AI suggestion endpoint va dua state ve `SuggestionReady`.
- `MealPlanViewModel.applySuggestionToDraft(userId)`
  - Tac dung: bien goi y AI thanh draft editable cua user.
- `MealPlanViewModel.saveDraftMealPlan()`
  - Tac dung: quyet dinh create moi hay update meal plan dang sua roi luu len BE.
- `MealPlanActivity.loadSavedMealPlans()`
  - Tac dung: tai danh sach meal plan da luu cua user de quan ly.
- `MealPlanActivity.applyMealPlanToDiary(mealPlan)`
  - Tac dung: apply toan bo meal plan vao food logs cua ngay duoc chon.
- `MealPlanDetailActivity.loadMealPlanDetail(mealPlanId)`
  - Tac dung: lay meal plan chi tiet va enrich thong tin mon an theo `foodId`.

### 11.8 Luong Workout tracking

- `WorkoutTrackingActivity.startSession()`
  - Tac dung: chuyen state sang `RUNNING`, capture baseline Health Connect va khoi dong loop cap nhat.
- `WorkoutTrackingActivity.startTickLoop()`
  - Tac dung: moi giay cap nhat duration, speed, distance, calories estimate.
- `WorkoutTrackingActivity.startHoldToFinish()`
  - Tac dung: bat dau co che nhan giu 3s de tranh ket thuc nham buoi tap.
- `WorkoutTrackingActivity.attemptFinishSession()`
  - Tac dung: enforce rule >= 3 phut moi duoc luu.
- `WorkoutTrackingActivity.findOrCreateExerciseId()`
  - Tac dung: dam bao co `exerciseId` hop le truoc khi tao workout log.
- `WorkoutTrackingActivity.saveWorkout()`
  - Tac dung: dong goi `CreateWorkoutLogRequest`, goi BE luu buoi tap, xu ly retry neu that bai.

### 11.9 Luong Statistics + weight

- `NamStatisticsRepository.loadDailySnapshot(userId, date)`
  - Tac dung: hop nhat nutrition + profile + weight theo 1 ngay.
- `NamStatisticsRepository.loadRangeSnapshot(userId, range)`
  - Tac dung: lay du lieu chuoi ngay cho week/month chart, co fallback rong de man hinh van render.
- `StatisticsDashboardActivity.updateMetricChart()`
  - Tac dung: doi metric hien thi (weight/protein/carbs/fat/calories) va cap nhat chart + detail.
- `UpdateWeightBottomSheet` (nut save)
  - Tac dung: gui cap nhat can nang nhanh ngay trong home flow.
- `WeightLogUpdateActivity.submitWeightLog()`
  - Tac dung: ghi weight log day du hon (co note), xu ly thong bao loi chi tiet.

### 11.10 Luong Chatbot

- `ChatbotActivity.loadHistoryList()`
  - Tac dung: tai danh sach cuoc tro chuyen cua user de render drawer history.
- `ChatbotActivity.loadConversationDetail(id)`
  - Tac dung: tai toan bo message cua 1 conversation cu the.
- `ChatbotActivity.sendMessage(message)`
  - Tac dung: append local user message, goi API chat, nhan va append assistant message.
- `ChatbotRepository.sendMessage(userId, conversationId, message)`
  - Tac dung: dong goi request chuan hoa va tra ket qua theo `ApiResult`.

### 11.11 Luong Settings + Edit profile

- `SettingsViewModel.loadData(userId)`
  - Tac dung: tai data tong hop cho man settings (user + profile + history profile).
- `SettingsActivity.updateCompletionDate()`
  - Tac dung: tinh ngay du kien hoan thanh muc tieu dua tren `targetDays` va moc profile.
- `EditProfileActivity.loadCurrentProfile()`
  - Tac dung: prefill thong tin hien tai de user sua.
- `EditProfileActivity.saveChanges()`
  - Tac dung: cap nhat ten user + tao ban health profile moi de cap nhat muc tieu va thong so.

Ghi chu: de doi chieu endpoint cu the cho tung ham, xem them muc `6. Mapping FE ham -> BE endpoint -> input/output`.

