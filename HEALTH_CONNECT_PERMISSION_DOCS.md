# Health Connect Permission Docs For Wao

## 1. Mục tiêu của phần mình vừa thêm

Phần này chưa đọc dữ liệu Health Connect thật sự.

Mục tiêu hiện tại là:

- Làm cho Wao xuất hiện như một app hợp lệ trong Health Connect.
- Cho Wao xin quyền đọc `Steps`, `Active calories burned`, và `Heart rate`.
- Cho user một chỗ rõ ràng trong màn Home để kết nối lại nếu chưa cấp quyền.
- Chuẩn bị sẵn nền để bước tiếp theo chỉ cần đọc data và đổ ra UI.

Nói ngắn gọn:

`Wao app -> kiểm tra Health Connect -> xin quyền -> nhận kết quả -> cập nhật trạng thái trên Home`

---

## 2. Luồng tổng quát sau khi mình thêm code

1. User mở `MainActivity`.
2. App gọi `checkHealthConnectAccess(...)`.
3. App hỏi: máy này có hỗ trợ Health Connect không?
4. Nếu chưa cài hoặc cần update Health Connect, app hiện dialog mở Play Store.
5. Nếu Health Connect có sẵn, app kiểm tra xem Wao đã có quyền đọc chưa.
6. Nếu chưa có quyền, app hiện dialog "Connect Health Connect".
7. User bấm `Grant access`.
8. Health Connect mở màn hình hệ thống để user bật quyền.
9. Kết quả quyền được trả về trong callback của `registerForActivityResult(...)`.
10. App cập nhật text trạng thái trong ô bước chân.

Điểm quan trọng:

- Health Connect không đi qua `NetworkClient.kt`.
- Đây là API local trên máy, không phải API server.

---

## 3. Từng file mình đã thêm hoặc sửa

## 3.1 `app/build.gradle.kts`

Mình thêm 2 thứ:

- `minSdk = 26`
- `implementation("androidx.health.connect:connect-client:1.1.0")`

### Tác dụng

`connect-client` là SDK chính thức để app Android nói chuyện với Health Connect.

Nếu không có dependency này:

- App không import được `HealthConnectClient`
- Không có `PermissionController`
- Không có `HealthPermission`

### Vì sao phải tăng `minSdk`

Health Connect không dành cho các API quá thấp. Phần SDK này yêu cầu mức Android cao hơn phần app cũ của bạn.

Nếu không tăng `minSdk`:

- Build rất dễ lỗi do dependency không tương thích.
- Dù code có viết xong, app cũng không thể support đúng luồng Health Connect.

### Ghi nhớ

- `compileSdk` là mức để compile.
- `minSdk` là mức Android thấp nhất app cho phép cài.
- `targetSdk` là mức Android app tối ưu và cam kết hành vi.

---

## 3.2 `app/src/main/AndroidManifest.xml`

Mình thêm các phần sau:

- `android.permission.health.READ_STEPS`
- `android.permission.health.READ_HEART_RATE`
- `<queries>` cho package `com.google.android.apps.healthdata`
- `PermissionsRationaleActivity`
- `ViewPermissionUsageActivity` (activity-alias)

### 1) `READ_STEPS`

Cho biết Wao muốn xin quyền đọc dữ liệu bước chân.

Nếu thiếu:

- Health Connect sẽ không coi Wao là app cần quyền này.
- Wao không xin được permission `Steps`.

### 2) `READ_HEART_RATE`

Cho biết Wao muốn xin quyền đọc dữ liệu nhịp tim.

Nếu thiếu:

- App không xin được quyền đọc nhịp tim.

### 3) `<queries>`

Cho phép app query xem package `Health Connect` có tồn tại trên máy hay không.

Nếu thiếu:

- Một số máy/phiên bản Android có thể không cho app nhìn thấy package đó.
- Luồng check cài đặt/update Health Connect sẽ thiếu ổn định.

### 4) `PermissionsRationaleActivity`

Đây là màn giải thích cho user biết:

- Wao dùng dữ liệu gì
- Dùng để làm gì
- Có ghi dữ liệu ngược lại hay không

Health Connect có thể mở màn này khi user xem thông tin quyền.

Nếu thiếu:

- Trải nghiệm quyền sẽ thiếu chuẩn.
- Có thể khó giải thích cho user hoặc bị thiếu phần thông tin mà Health Connect mong đợi.

### 5) `ViewPermissionUsageActivity` alias

Alias này giúp hệ thống/Health Connect điều hướng vào activity giải thích quyền của app.

Nếu thiếu:

- Có thể user vào từ Health Connect nhưng app không mở đúng màn giải thích.

### Ghi nhớ

Manifest là nơi khai báo "app muốn làm gì".
Runtime code là nơi app thật sự xin quyền.
Cả 2 đều cần. Chỉ có một trong hai là chưa đủ.

---

## 3.3 `app/src/main/java/com/example/wao_fe/health/HealthConnectManager.kt`

Mình tạo file này để gom các hằng số và helper chung của Health Connect.

### `providerPackageName`

```kotlin
const val providerPackageName = "com.google.android.apps.healthdata"
```

Tác dụng:

- Đây là package chính của Health Connect app/provider.
- Dùng khi kiểm tra trạng thái SDK.
- Dùng khi mở Play Store.

### `readPermissions`

```kotlin
val readPermissions = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class),
)
```

Tác dụng:

- Gom tất cả quyền cần xin vào một chỗ.
- Tránh việc một chỗ xin `Steps + Heart rate` nhưng chỗ khác chỉ check `Steps`.

Nếu không gom:

- Code dễ lệch logic.
- Khó bảo trì khi sau này thêm `Sleep`, `Calories`, `Distance`...

### `getSdkStatus(context)`

Tác dụng:

- Kiểm tra Health Connect có dùng được trên máy không.
- Kiểm tra có cần cài/update provider không.

Bạn có thể hiểu nó như câu hỏi:

"Máy này đã sẵn sàng để dùng Health Connect chưa?"

### `buildInstallIntent(...)`

Tác dụng:

- Tạo `Intent` mở Play Store đúng trang Health Connect.

### `buildBrowserFallbackIntent()`

Tác dụng:

- Nếu máy không mở được Play Store intent đặc biệt, app fallback sang link web Play Store.

### Vì sao phải tách ra file riêng

Vì nếu để hết trong `MainActivity`:

- File activity sẽ phình rất nhanh.
- Logic UI và logic tích hợp hệ thống bị trộn vào nhau.
- Sau này tái sử dụng sang màn Settings hoặc onboarding sẽ khó hơn.

---

## 3.4 `PermissionsRationaleActivity.kt`

File này rất đơn giản:

- mở layout giải thích quyền
- có nút `Close`

### Tác dụng

Đây không phải màn xin quyền.

Đây là màn giải thích:

- app đọc gì
- đọc để làm gì
- user có thể thu hồi quyền ở đâu

Đây là một ý rất quan trọng:

- `request permission screen` là màn hệ thống của Health Connect
- `rationale screen` là màn của app mình để giải thích trước/sau khi xin quyền

2 màn này khác nhau.

---

## 3.5 `activity_permissions_rationale.xml`

Layout này là phần UI cho `PermissionsRationaleActivity`.

Mình để nội dung ngắn và đúng trọng tâm:

- Wao đọc bước chân và nhịp tim
- Chỉ đọc trong flow hiện tại
- Có thể revoke quyền trong Health Connect

### Vì sao phần này cần ngắn

Vì đây là màn giải thích nhanh.

Không nên:

- nhồi quá nhiều text
- biến nó thành màn onboarding dài
- làm user khó hiểu trước lúc cấp quyền

---

## 3.6 `activity_main.xml`

Mình sửa phần card bước chân:

- thêm `id` cho card là `cardSteps`
- thêm `tvHealthConnectStatus`
- cho card có thể click

### `cardSteps`

Tác dụng:

- Có một chỗ rõ ràng để user bấm kết nối lại Health Connect.

Nếu không có:

- Bạn chỉ còn cách auto-popup trên app launch.
- User không biết chạm vào đâu để xin lại quyền sau này.

### `tvHealthConnectStatus`

Tác dụng:

- Hiển thị trạng thái hiện tại.

Ví dụ:

- `Tap to connect Health Connect`
- `Health Connect connected`
- `Install or update Health Connect`

Nếu không có:

- User khó biết app đang thiếu gì.
- Debug bằng mắt rất khó.

---

## 3.7 `MainActivity.kt`

Đây là chỗ quan trọng nhất của luồng permission.

### A. `requestHealthPermissions`

Đây là đoạn bạn đang chọn:

```kotlin
private val requestHealthPermissions = registerForActivityResult(
    PermissionController.createRequestPermissionResultContract()
) { granted ->
    if (granted.containsAll(HealthConnectManager.readPermissions)) {
        Toast.makeText(this, "Health Connect connected", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(this, "Health Connect permission was not granted", Toast.LENGTH_SHORT).show()
    }
    checkHealthConnectAccess(promptIfMissing = false, initiatedByUser = false)
}
```

#### Giải thích từng phần

### `registerForActivityResult(...)`

Đây là API hiện đại của Android để:

- mở một flow bên ngoài
- chờ kết quả trả về
- nhận kết quả bằng callback

Trong case này, flow bên ngoài là màn cấp quyền của Health Connect.

### Vì sao phải khai báo ở cấp `Activity`

Vì launcher này cần được đăng ký sớm, gắn với lifecycle của Activity.

Không nên tạo nó tạm trong `onClick`.

Nếu tạo sai chỗ:

- rất dễ crash
- hoặc callback không chạy đúng lifecycle

### `PermissionController.createRequestPermissionResultContract()`

Đây là contract mà Health Connect SDK cung cấp.

Bạn có thể hiểu:

- contract = mẫu chuẩn để Android biết "mình sắp request loại kết quả nào"

Ở đây nó nói với hệ thống:

"Tôi sắp mở màn Health Connect permission và tôi muốn nhận lại tập permission user đã grant."

### Callback `{ granted -> ... }`

Biến `granted` là tập quyền mà user đã cho phép sau khi thoát khỏi màn Health Connect.

Nó không phải `Boolean`.

Nó là `Set<String>`.

Lý do:

- User có thể cấp một phần quyền
- Ví dụ cấp `Steps` nhưng không cấp `Heart rate`

### `granted.containsAll(HealthConnectManager.readPermissions)`

Dòng này kiểm tra:

- app đã được cấp đủ toàn bộ quyền mà mình cần chưa?

Mình dùng `containsAll(...)` thay vì check từng quyền riêng vì:

- ngắn gọn
- đúng với tư duy "đủ bộ quyền"
- dễ mở rộng nếu sau này thêm 1 quyền nữa

### `Toast.makeText(...)`

Tác dụng:

- phản hồi nhanh cho user biết cấp quyền thành công hay chưa

### `checkHealthConnectAccess(promptIfMissing = false, initiatedByUser = false)`

Dòng này rất quan trọng.

Nó giúp app:

- re-check lại trạng thái sau khi callback chạy xong
- cập nhật `tvHealthConnectStatus`
- tránh việc UI bị cũ

Vì sao để `promptIfMissing = false`:

- vì user vừa đi qua màn cấp quyền xong
- mình chỉ muốn refresh trạng thái
- không muốn popup xin quyền lặp lại ngay lập tức

### B. `checkHealthConnectAccess(...)`

Đây là hàm trung tâm của cả flow.

Nó xử lý 3 case:

### Case 1: `SDK_UNAVAILABLE`

Nghĩa là:

- máy không support Health Connect

App sẽ:

- hiện text báo không support
- nếu user vừa bấm tay vào card thì show toast

### Case 2: `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED`

Nghĩa là:

- cần cài hoặc update Health Connect

App sẽ:

- đổi status text
- mở dialog cài đặt nếu `promptIfMissing = true`

### Case 3: `SDK_AVAILABLE`

Nghĩa là:

- Health Connect dùng được

App sẽ:

1. tạo `HealthConnectClient`
2. lấy `grantedPermissions`
3. so với `HealthConnectManager.readPermissions`
4. nếu đủ thì hiện `connected`
5. nếu thiếu thì hiện `Tap to connect...` và có thể mở dialog xin quyền

### C. `cardSteps.setOnClickListener`

Tác dụng:

- user có điểm chạm để xin quyền lại sau này

Đây là quyết định UX quan trọng.

Nếu chỉ xin quyền tự động trong `onCreate()`:

- user không biết quay lại xin quyền bằng cách nào
- trải nghiệm khó kiểm soát hơn

### D. `onCreate()` và `onResume()`

Mình dùng cả hai:

### Trong `onCreate()`

Mục tiêu:

- kiểm tra lần đầu khi màn hình được tạo

### Trong `onResume()`

Mục tiêu:

- user có thể vừa từ Health Connect app quay lại
- lúc đó cần refresh trạng thái mới nhất

Nếu chỉ check trong `onCreate()`:

- status text có thể bị cũ khi user quay lại từ màn permission/settings

---

## 4. Tại sao mình chưa đọc dữ liệu Health Connect ngay lúc này

Vì phần này đang tách riêng mục tiêu:

- làm permission flow đúng trước
- rồi mới đọc dữ liệu ở bước tiếp theo

Đây là cách làm an toàn hơn khi học và debug.

Nếu vừa xin quyền vừa đọc data ngay từ đầu:

- khó biết app hỏng ở permission hay hỏng ở query data

Tách 2 bước ra sẽ dễ học hơn nhiều.

---

## 5. Những lỗi hay gặp và cách hiểu

## 5.1 App không xuất hiện trong Health Connect

Nguyên nhân thường gặp:

- thiếu permission trong manifest
- chưa chạy flow xin quyền lần nào
- app chưa được cài lại sau khi sửa manifest

### Cách nghĩ đúng

Health Connect không tự liệt kê app "cho vui".
App phải khai báo nhu cầu quyền thì mới được coi là app hợp lệ cho flow đó.

---

## 5.2 Dialog xin quyền không hiện

Nguyên nhân có thể là:

- Health Connect chưa cài
- Health Connect cần update
- user đã deny trước đó và đang ở trạng thái phải bật lại thủ công

---

## 5.3 Build shell bị lỗi JDK

Trong lần mình kiểm tra:

- Gradle đã qua bước dependency
- shell dừng ở Java 17 toolchain

Điều này nghĩa là:

- lỗi hiện tại không chỉ thẳng vào code Health Connect
- mà là môi trường build shell chưa có JDK đúng version

Nếu bạn build bằng Android Studio với Gradle JDK phù hợp thì đó là nơi nên test tiếp.

---

## 5.4 `tvSteps` hiện vẫn là dữ liệu backend

Rất quan trọng:

Hiện tại mình mới làm phần permission và trạng thái kết nối.

`tvSteps` vẫn đang được set từ summary backend trong `fetchDashboardData()`.

Nghĩa là:

- Wao đã sẵn sàng xin quyền Health Connect
- nhưng chưa đọc step count thật từ Health Connect để thay thế dữ liệu backend

Đây là bước tiếp theo.

---

## 6. Khi nào nên tách manager riêng, khi nào viết thẳng trong Activity

Với Health Connect, nên tách manager/repository khi:

- có nhiều permission
- có nhiều màn hình cùng dùng
- có logic install/update/check status
- có thêm logic đọc record thật

Không nên nhét hết vào `MainActivity` vì:

- file rất nhanh thành "god activity"
- khó test
- khó đọc
- khó tái sử dụng

---

## 7. Bước tiếp theo mình đề xuất

Sau permission flow, bước tiếp theo là:

1. đọc `StepsRecord` từ Health Connect
2. đọc `HeartRateRecord`
3. đổ vào UI
4. quyết định ưu tiên nguồn nào:
   - Health Connect
   - backend
   - hoặc merge hai nguồn

Với repo hiện tại, hợp lý nhất là:

- giữ backend cho summary khác
- riêng `steps` và `heart rate` lấy trực tiếp từ Health Connect

---

## 8. Tóm tắt ngắn để nhớ

- `Gradle` để thêm SDK.
- `Manifest` để khai báo app muốn xin quyền gì.
- `HealthConnectManager` để gom logic hệ thống chung.
- `PermissionsRationaleActivity` để giải thích cho user.
- `MainActivity` để điều phối flow xin quyền.
- `activity_main.xml` để cho user một điểm chạm và nhìn thấy trạng thái.

Nếu bạn nhớ được đúng 1 câu, hãy nhớ câu này:

`Manifest khai báo nhu cầu, MainActivity xin quyền runtime, callback cập nhật lại trạng thái UI.`

---

## 9. Phần mới: đọc dữ liệu thật từ Health Connect

Sau bước permission, mình đã làm thêm bước đọc dữ liệu thật và show lên màn Home.

Hiện tại Home đọc:

- `Steps` của hôm nay từ Health Connect
- `Active calories burned` của hôm nay từ Health Connect
- `Nhịp tim gần nhất` của hôm nay từ Health Connect

---

## 10. File mới: `HealthConnectRepository.kt`

File: `app/src/main/java/com/example/wao_fe/health/HealthConnectRepository.kt`

File này có 2 nhiệm vụ:

- đọc bước chân hôm nay
- đọc active calories burned hôm nay
- đọc nhịp tim gần nhất hôm nay

Mình tạo thêm data class:

```kotlin
data class HealthConnectSnapshot(
    val stepsToday: Long,
    val activeCaloriesBurnedToday: Double,
    val latestHeartRateBpm: Long?,
    val latestHeartRateTime: Instant?,
)
```

### Tác dụng

Đây là object gói dữ liệu thô thành một snapshot UI-friendly.

Thay vì `MainActivity` phải tự xử lý nhiều biến rời:

- steps
- bpm
- active calories burned
- thời gian đo

thì activity chỉ cần nhận một object duy nhất.

### Vì sao bước chân dùng `aggregate`

Code:

```kotlin
AggregateRequest(
    metrics = setOf(StepsRecord.COUNT_TOTAL),
    timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
)
```

Lý do:

`Steps` là dữ liệu cộng dồn.

Nếu dùng `readRecords()` rồi tự cộng tay:

- dễ cộng trùng
- dễ sai nếu có nhiều nguồn dữ liệu
- khó bảo trì hơn

`aggregate()` là cách đúng hơn cho loại dữ liệu cumulative.

### Vì sao active calories burned cũng dùng `aggregate`

Vì đây cũng là dữ liệu cộng dồn theo ngày.

Mình lấy metric:

- `ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL`

Sau đó đổi về kcal bằng:

```kotlin
?.inKilocalories
```

### Vì sao nhịp tim dùng `readRecords`

Code:

```kotlin
ReadRecordsRequest(
    recordType = HeartRateRecord::class,
    timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
    ascendingOrder = false,
    pageSize = 10,
)
```

Lý do:

Nhịp tim không phải dữ liệu cộng dồn.

Mình cần:

- đọc các record nhịp tim
- lấy sample gần nhất

Nên `readRecords()` phù hợp hơn.

### Vì sao lấy `sample` gần nhất thay vì `record` gần nhất

Một `HeartRateRecord` có thể chứa nhiều sample bên trong.

Vì vậy mình làm:

```kotlin
records
    .flatMap { it.samples }
    .maxByOrNull { it.time }
```

Để đảm bảo:

- lấy đúng lần đo mới nhất
- không phụ thuộc hoàn toàn vào thứ tự record bên ngoài

---

## 11. MainActivity bây giờ render dữ liệu như thế nào

Mình thêm một số state trong `MainActivity`:

- `backendStepsToday`
- `healthConnectStepsToday`
- `latestHeartRateBpm`
- `latestHeartRateMeasuredAtText`
- `hasHealthConnectReadAccess`
- `healthConnectActiveCaloriesBurned`

### Vì sao cần tách state backend và Health Connect

Trước đây `tvSteps` bị set trực tiếp từ backend trong `fetchDashboardData()`.

Nếu bây giờ mình cũng set trực tiếp từ Health Connect:

- hai coroutine có thể ghi đè lẫn nhau
- UI sẽ lúc backend, lúc Health Connect

Nên mình đổi cách làm:

- backend chỉ cập nhật vào biến state
- Health Connect cũng cập nhật vào biến state
- sau đó UI render từ state

Đây là ý tưởng rất quan trọng khi làm app nhiều nguồn dữ liệu.

### Hàm `renderCaloriesSummary()`

Logic:

- `totalCalIn` vẫn lấy từ backend
- `totalCalOut` ưu tiên `Active calories burned` từ Health Connect
- nếu Health Connect chưa có dữ liệu thì fallback backend
- `remaining` và progress được tính lại từ giá trị `caloriesOut` đã chọn

Ý nghĩa:

App không chỉ đổi đúng mỗi `tvCalOut`, mà còn giữ cho toàn bộ card calories nhất quán.

### Hàm `renderStepsCard()`

Logic:

- nếu đã có steps từ Health Connect thì ưu tiên dùng
- nếu chưa thì fallback sang backend
- nếu cả hai chưa có thì hiện `0/10000`

### Hàm `renderHeartRateCard()`

Logic:

- có nhịp tim thì show `xx bpm`
- có cả thời gian đo thì show `Cập nhật lúc HH:mm`
- nếu đã có quyền nhưng chưa có dữ liệu thì báo chưa có nhịp tim hôm nay
- nếu chưa có quyền thì nhắc kết nối Health Connect

---

## 12. Card nhịp tim trên UI

Mình thay placeholder card trống bằng card thật:

- `cardHeartRate`
- `tvHeartRate`
- `tvHeartRateMeta`

### Tác dụng

- user nhìn thấy nhịp tim riêng, không phải nhét chung vào status steps
- card này cũng click được để mở lại flow kết nối Health Connect

Đây là một quyết định UX tốt hơn vì:

- user có 2 điểm chạm rõ ràng
- phần Health Connect không bị giấu

---

## 13. Hàm `loadHealthConnectMetrics(...)`

Đây là hàm nối giữa quyền và dữ liệu.

Nó làm:

1. tạo `HealthConnectRepository`
2. gọi `readTodaySnapshot()`
3. cập nhật state local
4. render lại UI

Nếu lỗi:

- ghi log
- xóa snapshot Health Connect cũ
- giữ app không crash
- đổi status để biết app đã connect nhưng đọc data chưa thành công

### Vì sao dùng `runCatching`

Để gom phần success/failure vào một chỗ dễ đọc hơn:

- `onSuccess`
- `onFailure`

Khi học Android, đây là một pattern khá sạch cho các tác vụ bất đồng bộ nhỏ.

---

## 14. `showHealthConnectLoadingState()`

Mình thêm state loading nhẹ cho card nhịp tim.

Tác dụng:

- khi app vừa xác nhận đã có quyền
- nhưng dữ liệu vẫn đang đọc

thì user không bị hiểu nhầm là app vẫn chưa connect.

Đây là khác biệt nhỏ nhưng rất đáng học:

- `permission granted` không có nghĩa là `data already loaded`

Nên UI nên phân biệt:

- đang chờ cấp quyền
- đã có quyền
- đang đọc dữ liệu
- đã đọc xong

---

## 15. Điều gì vẫn chưa làm trong bước này

Phần này hiện mới đọc dữ liệu và show ra màn hình.

Mình chưa làm:

- đồng bộ ngược data Health Connect lên backend
- ghi dữ liệu từ Wao sang Health Connect
- đọc lịch sử nhiều ngày
- đọc nhịp tim nghỉ, calorie, ngủ, distance...

Nghĩa là hiện tại flow là:

`Health Connect -> Wao UI`

chưa phải:

`Wao <-> Health Connect <-> Backend`

---

## 16. Một bài học quan trọng từ bước này

Khi tích hợp dữ liệu hệ thống vào app, hãy tách thành 3 lớp suy nghĩ:

1. `Can I access it?`
   - permission, provider, sdk status
2. `Can I read it correctly?`
   - aggregate hay readRecords
3. `Can I render it consistently?`
   - state nào ưu tiên, fallback nào dùng, tránh ghi đè nhau

Nếu tách được 3 lớp này, bạn sẽ debug dễ hơn rất nhiều.
