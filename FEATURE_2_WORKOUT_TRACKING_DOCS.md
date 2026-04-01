# Feature 2 Workout Tracking Docs

## 1. Scope

File này dùng để note toàn bộ thay đổi liên quan tới tính năng 2: khu vực `Hoạt động tập luyện` ở home và màn `WorkoutTrackingActivity`.

Từ ngày 2026-04-01 trở đi:

- Mỗi lần sửa code cho feature 2, phải cập nhật file này.
- Mỗi thay đổi logic khó đọc phải có comment ngắn, rõ nghĩa ngay tại chỗ code.
- Nếu có thay đổi business rule, phải cập nhật cả phần `Change Log` và `Regression Checklist`.

## 2. Files đang thuộc feature 2

- `app/src/main/res/layout/activity_main.xml`
  - Grid 4 môn ở home.
- `app/src/main/java/com/example/wao_fe/MainActivity.kt`
  - Điều hướng từ home sang màn tracking.
- `app/src/main/java/com/example/wao_fe/WorkoutType.kt`
  - Mapping môn tập, icon, tốc độ mặc định, calories, heart-rate range.
- `app/src/main/java/com/example/wao_fe/WorkoutTrackingActivity.kt`
  - UI tracking, state machine, hold-to-finish, rule save, Health Connect fallback.
- `app/src/main/res/layout/activity_workout_tracking.xml`
  - Layout dark/fullscreen của màn tracking.
- `app/src/main/res/drawable/ic_walk.xml`
  - Icon đi bộ.
- `app/src/main/res/drawable/bg_workout_shortcut.xml`
  - Background shortcut card ở home.
- `app/src/main/AndroidManifest.xml`
  - Register activity mới cho feature 2.
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values-night/themes.xml`
- `app/src/main/res/values/colors.xml`
  - Theme và màu riêng cho màn tracking.

## 3. Change Log

### 2026-03-31 to 2026-04-01: initial implementation

- Rút gọn khu vực `Hoạt động tập luyện` ở home còn đúng 4 môn:
  - Đi bộ
  - Chạy bộ ngoài trời
  - Chạy bộ trong nhà
  - Đạp xe
- Mỗi ô ở home mở sang `WorkoutTrackingActivity` với `WorkoutType` tương ứng.
- Tạo màn tracking dark/fullscreen, tối giản:
  - Header trái là tên môn.
  - Header phải là icon more.
  - Main metric ở giữa là tổng số km.
  - 4 thông số phụ dạng grid 2 cột.
- Mapping metric theo môn:
  - Đi bộ: tổng số km, tốc độ, kcal, nhịp tim.
  - Chạy bộ ngoài trời: tổng số km, tốc độ, kcal, nhịp tim.
  - Đạp xe: tổng số km, tốc độ, kcal, nhịp tim.
  - Chạy bộ trong nhà: tổng số km, tốc độ, kcal, số bước.
- Thêm state machine cho session:
  - `IDLE`
  - `RUNNING`
  - `PAUSED`
  - `SAVING`
- Button flow:
  - Idle: `Start`
  - Running: `Pause`
  - Paused: `Resume`
- Kết thúc buổi tập bằng cách nhấn giữ 3 giây:
  - Có progress ring.
  - Thả tay sớm thì hủy.
- Business rule khi kết thúc:
  - Nếu `< 3 phút`: popup `Buổi tập quá ngắn`
  - Nếu `>= 3 phút`: cho phép save workout log.
- Save flow:
  - Tìm `exercise` theo tên.
  - Nếu chưa có thì tự tạo `exercise`.
  - Sau đó gọi `createWorkoutLog`.
- Health Connect integration trong tracking:
  - Ưu tiên heart rate thật nếu app đã có quyền.
  - Với treadmill, ưu tiên steps delta từ Health Connect nếu có.
  - Nếu không có dữ liệu Health Connect thì fallback sang estimate nội bộ để session vẫn chạy.

## 4. Current Assumptions

- `exerciseId` là bắt buộc khi save workout log, nên app sẽ tự lookup hoặc create exercise.
- `categoryId` fallback đang dùng từ exercise đầu tiên lấy được từ backend; nếu backend không có exercise nào thì fallback về `1`.
- Heart rate fallback hiện là estimate cho mục đích UI/session continuity, không phải dữ liệu đo y tế.
- Compile đầy đủ trong sandbox hiện chưa verify được vì môi trường đang chỉ có JDK 21, trong khi project yêu cầu `jvmToolchain(17)`.

## 5. Regression Checklist

- Home chỉ còn đúng 4 shortcut môn tập.
- Bấm từng shortcut mở đúng tên môn ở header tracking.
- Start chuyển sang running state.
- Pause chuyển sang paused state.
- Resume tiếp tục session trước đó.
- Giữ 3 giây để kết thúc có progress ring.
- Thả tay trước 3 giây thì không kết thúc.
- Session dưới 3 phút hiện đúng popup:
  - `Thoát không lưu`
  - `Tiếp tục tập`
- Session từ 3 phút trở lên gọi save.
- Chạy bộ trong nhà hiển thị `Số bước`, các môn còn lại hiển thị `Nhịp tim`.
- Màn tracking giữ dark theme/fullscreen.

## 6. Update Rule For Future Changes

Khi sửa feature 2 trong các lần sau, phải làm đủ 3 việc:

1. Sửa code.
2. Thêm hoặc cập nhật comment ở đoạn logic mới / logic khó.
3. Cập nhật file `FEATURE_2_WORKOUT_TRACKING_DOCS.md`:
   - thêm ngày sửa
   - mô tả file bị chạm
   - mô tả thay đổi business/UI/API
   - cập nhật checklist nếu behavior thay đổi
