# Feature 2 Workout Tracking Docs

## 1. Scope

File nay dung de note toan bo thay doi lien quan toi feature 2:

- khu vuc `Hoat dong tap luyen` o Home
- man `WorkoutTrackingActivity`
- cac man profile lien quan den workout journal va step chart

Rule tu 2026-04-01:

- Moi lan sua code cho feature 2 phai cap nhat file nay.
- Moi logic kho doc phai co comment ngan, ro nghia ngay tai cho code.
- Neu business rule thay doi, phai cap nhat ca `Change Log` va `Regression Checklist`.

## 2. Files Thuoc Feature 2

- `app/src/main/java/com/example/wao_fe/MainActivity.kt`
  - Dieu huong tu home sang tracking.
- `app/src/main/java/com/example/wao_fe/SettingsActivity.kt`
  - Entry point tu man `Tai khoan` sang `Nhat ki tap luyen` va `Bieu do so buoc`.
- `app/src/main/java/com/example/wao_fe/WorkoutType.kt`
  - Mapping mon tap, icon, GPS/step rules, MET config.
- `app/src/main/java/com/example/wao_fe/WorkoutTrackingActivity.kt`
  - UI tracking, state machine, permission flow, save workout.
- `app/src/main/java/com/example/wao_fe/WorkoutJournalActivity.kt`
  - Danh sach cac mon da tap tu profile.
- `app/src/main/java/com/example/wao_fe/WorkoutHistoryActivity.kt`
  - Lich su buoi tap theo mon.
- `app/src/main/java/com/example/wao_fe/WorkoutJournalRepository.kt`
  - Tong hop history tu API workout-log hien tai va parse metadata tam trong `note`.
- `app/src/main/java/com/example/wao_fe/StepsTrendActivity.kt`
  - Bieu do so buoc Hom nay / Hom qua tu Health Connect.
- `app/src/main/java/com/example/wao_fe/health/HealthConnectRepository.kt`
  - Snapshot today va timeline so buoc theo gio.
- `app/src/main/java/com/example/wao_fe/health/HealthConnectManager.kt`
  - Health Connect permission sets.
- `app/src/main/res/layout/activity_main.xml`
  - Grid 4 mon o home.
- `app/src/main/res/layout/activity_workout_tracking.xml`
  - Layout dark/fullscreen cua tracking.
- `app/src/main/res/layout/activity_workout_journal.xml`
  - Layout journal workout.
- `app/src/main/res/layout/activity_workout_history.xml`
  - Layout history theo mon.
- `app/src/main/res/layout/activity_steps_trend.xml`
  - Layout step chart.
- `app/src/main/res/layout/item_workout_sport_summary.xml`
  - Card item cho tung mon tap trong journal.
- `app/src/main/AndroidManifest.xml`
  - Dang ky activities, permissions, hardware features.
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values-night/themes.xml`
- `app/src/main/res/values/colors.xml`
  - Theme va mau cho tracking/profile workout screens.

## 3. Change Log

### 2026-03-31 to 2026-04-01: initial implementation

- Rut gon khu vuc `Hoat dong tap luyen` o home con 4 mon:
  - Di bo
  - Chay bo ngoai troi
  - Chay bo trong nha
  - Dap xe
- Moi o o home mo sang `WorkoutTrackingActivity` voi `WorkoutType` tuong ung.
- Tao man tracking dark/fullscreen:
  - Header trai la ten mon
  - Header phai la icon more
  - Main metric o giua la tong so km
  - 4 thong so phu dang grid 2 cot
- Mapping metric:
  - Di bo: tong km, toc do, kcal, nhip tim
  - Chay bo ngoai troi: tong km, toc do, kcal, nhip tim
  - Dap xe: tong km, toc do, kcal, nhip tim
  - Chay bo trong nha: tong km, toc do, kcal, so buoc
- Them state machine:
  - `IDLE`
  - `RUNNING`
  - `PAUSED`
  - `SAVING`
- Button flow:
  - Idle: `Start`
  - Running: `Pause`
  - Paused: `Resume`
- Them hold 3 giay de ket thuc buoi tap:
  - co progress ring
  - tha tay som thi huy
- Rule ket thuc:
  - `< 3 phut`: popup `Buoi tap qua ngan`
  - `>= 3 phut`: cho phep save
- Save flow:
  - lookup `exercise`
  - neu chua co thi tao moi
  - goi `createWorkoutLog`

### 2026-04-01: merge conflict resolution in `MainActivity`

- Resolve conflict trong `fetchDashboardData()` sau khi pull git.
- Giu backend state update de khong vo helper/state cu.
- Chon cach hien thi calories summary giong nhanh teammate:
  - `tvCalIn`
  - `tvCalOut`
  - `tvCalRemaining`
  - `pbCalories`
- Dong bo `renderCaloriesSummary()` ve cung cong thuc hien thi.

### 2026-04-01: duplicate property fix in `MainActivity`

- Xoa khai bao trung `bottomNavigationView` con sot sau merge.

### 2026-04-01: phase 1 live tracking data upgrade

- Outdoor walking/running/cycling lay distance va speed that hon tu GPS thay vi waveform fake.
- Indoor running uu tien step-counter sensor, fallback sang Health Connect step delta.
- Heart rate live chi hien thi du lieu that tu Health Connect; neu khong co thi hien `-- bpm`.
- Calories uu tien Active Calories tu Health Connect; neu khong co thi fallback local MET-based estimate co dau `~`.
- Them runtime permission flow cho location va activity recognition trong `WorkoutTrackingActivity`.
- Them manifest permissions/features cho location, activity recognition, GPS va step counter.

### 2026-04-01: manifest sync after pull

- Re-add permissions va hardware features cua workout tracking trong `AndroidManifest.xml` sau khi pull lam mat location/activity-recognition declarations.

### 2026-04-01: walking business refinement

- Giu `Di bo` la outdoor workout, van dung GPS cho `Tong so km` va `Toc do`.
- UI walking doi metric thu 4 tu `Nhip tim` sang `So buoc`.
- Walking step data la signal phu tu step sensor hoac Health Connect, khong block flow outdoor neu thieu quyen step.
- Chua them Google Maps route vi project hien chua co Maps SDK dependency va API key.

### 2026-04-01: profile workout journal and step chart

- `SettingsActivity`:
  - `Tap luyen` mo sang `WorkoutJournalActivity`
  - `So buoc` mo sang `StepsTrendActivity`
- Them `WorkoutJournalActivity`:
  - hien cac mon da tap trong cua so gan day
  - tong hop so buoi, quang duong, thoi gian, calories theo mon
- Them `WorkoutHistoryActivity`:
  - bam vao 1 mon de xem lich su cac buoi tap
  - hien date, time neu co, distance, duration, calories, step/speed
- Them `StepsTrendActivity`:
  - show bieu do so buoc theo gio cho `Hom nay` va `Hom qua`
  - data lay truc tiep tu Health Connect
- Them `WorkoutJournalRepository`:
  - do backend hien chi co API workout log theo tung ngay, FE tam assemble journal trong cua so 30 ngay
  - parse metadata tam tu `note`
- Update `WorkoutTrackingActivity` save flow:
  - tam ghi metadata co cau truc vao `note`:
    - `typeKey`
    - `distanceKm`
    - `avgSpeedKmh`
    - `steps`
    - `startedAt`
    - `endedAt`
  - muc dich la de FE journal/history co the render tot hon truoc khi BE mo field rieng
- Update `HealthConnectRepository`:
  - them reader timeline so buoc theo gio
- Update `HealthConnectManager`:
  - tach `stepReadPermissions` de man step chart chi xin `READ_STEPS`

### 2026-04-01: direct workout-log save contract for mobile tracking

- Bo flow FE tu lookup/create `exercise` truoc khi save workout.
- `WorkoutTrackingActivity` gio save truc tiep vao:
  - `POST /api/users/{userId}/workout-logs`
- Payload FE map theo `workoutType` backend:
  - `Di bo` -> `OUTDOOR_WALKING`
  - `Chay bo ngoai troi` -> `OUTDOOR_RUNNING`
  - `Chay bo trong nha` -> `INDOOR_RUNNING`
  - `Dap xe` -> `CYCLING`
- Payload save moi uu tien cac field tracking that:
  - `workoutType`
  - `startedAt`
  - `endedAt`
  - `distanceMeters`
  - `durationMin`
  - `caloriesBurned`
  - `stepCount`
  - `avgSpeedKmh`
  - `note`
- FE log ro khi save:
  - HTTP method
  - full URL
  - request body
  - response code
  - response body
- Dialog save fail gio parse `response.message` tu backend neu co, thay vi chi hien `HTTP 404`.
- `WorkoutJournalRepository` uu tien doc field first-class tu `WorkoutLogResponse` truoc, sau do moi fallback sang metadata trong `note`.

## 4. Current Assumptions

- Mobile tracking save workout truc tiep bang `workoutType`, khong con tu tao `exercise` hay fallback `categoryId`.
- Backend `createWorkoutLog` can support contract moi voi cac field optional/null thay vi bat FE phai gui `exerciseId`/`programId`.
- Heart rate live card chi hien thi du lieu that tu Health Connect; neu thieu du lieu thi hien `-- bpm`.
- Indoor distance suy ra tu `heightCm * strideLengthFactor`, nen do chinh xac con phu thuoc vao profile moi nhat.
- Workout journal FE hien chi tai `30 ngay gan nhat` vi backend chua co range endpoint rieng cho workout history.
- Cac field nhu `distance`, `steps`, `startedAt`, `endedAt` trong history hien dang duoc FE tam parse tu `note` cho cac log moi.
- Man `So buoc` la FE-only flow, phu thuoc Health Connect co san sang va duoc cap `READ_STEPS`.
- Full compile trong sandbox chua verify duoc vi moi truong hien tai chi co JDK 21 trong khi project yeu cau `jvmToolchain(17)`.

## 5. Regression Checklist

- Home chi con dung 4 shortcut mon tap.
- Bam tung shortcut mo dung ten mon o header tracking.
- Start chuyen sang running state.
- Pause chuyen sang paused state.
- Resume tiep tuc session truoc do.
- Giu 3 giay de ket thuc co progress ring.
- Tha tay truoc 3 giay thi khong ket thuc.
- Session duoi 3 phut hien dung popup:
  - `Thoat khong luu`
  - `Tiep tuc tap`
- Session tu 3 phut tro len goi save.
- Khi save workout khong con goi `/api/exercises`.
- Save workout goi dung `POST /api/users/{userId}/workout-logs`.
- Neu backend tra JSON loi co `message`, dialog save fail hien dung message do.
- Chay bo trong nha hien `So buoc`, cac mon con lai hien `Nhip tim`.
- Man tracking giu dark theme/fullscreen.
- Outdoor workout xin quyen vi tri khi can va cap nhat `Tong so km` / `Toc do` tu GPS.
- `Di bo` van la outdoor workout va hien `So buoc` o metric thu 4.
- Indoor running xin quyen hoat dong khi can va cap nhat `So buoc` / `Tong so km` tu step sensor hoac Health Connect.
- `Kcal` co dau `~` khi dang la estimate local, va bo dau `~` khi da co delta tu Health Connect.
- `Nhip tim` hien `-- bpm` neu thiet bi/chinh sach quyen chua cung cap duoc du lieu that.
- `Tai khoan` > `Tap luyen` mo man journal.
- Journal chi hien cac mon co log trong cua so FE hien tai.
- Bam vao 1 mon trong journal mo dung man history cua mon do.
- `Tai khoan` > `So buoc` mo man step chart.
- Step chart chuyen duoc giua `Hom nay` va `Hom qua`.
- Neu chua co Health Connect hoac chua cap `READ_STEPS`, man step chart hien CTA dung thay vi crash.

## 6. Update Rule For Future Changes

Moi lan sua feature 2 trong cac lan sau, phai lam du 3 viec:

1. Sua code.
2. Them hoac cap nhat comment o doan logic moi / logic kho.
3. Cap nhat file `FEATURE_2_WORKOUT_TRACKING_DOCS.md`:
   - them ngay sua
   - mo ta file bi cham
   - mo ta thay doi business/UI/API
   - cap nhat checklist neu behavior thay doi
