# 🌿 WAO — Ứng dụng Theo dõi Sức khỏe

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/Min%20SDK-24-4CAF50?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Target%20SDK-36-4CAF50?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Version-1.0-69F0AE?style=for-the-badge"/>
</p>

> **WAO** là ứng dụng Android theo dõi sức khỏe cá nhân, được xây dựng bằng **Kotlin** với giao diện tone màu **xanh lá non** tươi mát, thân thiện và hiện đại.

---

## 📱 Màn hình hiện tại

| Splash Screen | Đăng nhập | Đăng ký |
|:---:|:---:|:---:|
| Logo WAO + animation chấm nhấp nháy | Form email & mật khẩu | Form họ tên, email, mật khẩu |

---

## 🛠️ Yêu cầu môi trường

| Công cụ                            | Phiên bản tối thiểu           |
|------------------------------------|-------------------------------|
| **Android Studio**                 | Hedgehog (2023.1.1) trở lên   |
| **JDK**                            | 11 trở lên (đang dùng 21)     |
| **Android SDK**                    | API 24 (Android 7.0) trở lên  |
| **Gradle**                         | 8.x (tự động tải qua wrapper) |
| **Kotlin**                         | 2.0.21                        |
| **AGP** (Android Gradle Plugin)    | 8.12.3                        |
| **Device**  | Medium Phone API 36.1         |

---

## 🚀 Hướng dẫn cài đặt & chạy dự án

### 1. Clone dự án

```bash
git clone https://github.com/TranBaLoi/Wao_fe.git
cd Wao_fe
```

### 2. Mở bằng Android Studio

1. Mở **Android Studio**
2. Chọn **File → Open**
3. Trỏ đến thư mục vừa clone `Wao_fe/`
4. Nhấn **OK** và đợi Gradle sync xong

> ⚠️ Lần đầu mở có thể mất vài phút để Android Studio tải dependencies.

### 3. Cấu hình SDK (nếu chưa có)

1. Vào **File → Project Structure → SDK Location**
2. Đảm bảo **Android SDK** đã được cài đặt
3. Nếu chưa có: vào **Tools → SDK Manager** → tải **Android SDK API 36**

### 4. Tạo file `local.properties` (nếu chưa có)

File này thường được tự tạo bởi Android Studio. Nếu chưa có, tạo thủ công tại thư mục gốc:

```properties
# Windows
sdk.dir=C\:\\Users\\<TênUser>\\AppData\\Local\\Android\\Sdk

# macOS / Linux
sdk.dir=/Users/<TênUser>/Library/Android/sdk
```

> Thay `<TênUser>` bằng tên user máy tính của bạn.

### 5. Sync Gradle

Sau khi mở project, Android Studio sẽ tự động sync. Nếu không:

- Nhấn **File → Sync Project with Gradle Files**
- Hoặc nhấn nút 🐘 **Sync Now** trên thanh thông báo

### 6. Chạy ứng dụng

#### Trên thiết bị thật:
1. Bật **Chế độ nhà phát triển** trên điện thoại
2. Bật **USB Debugging**
3. Kết nối điện thoại qua USB
4. Nhấn nút ▶️ **Run** trong Android Studio

#### Trên máy ảo (Emulator):
1. Vào **Tools → Device Manager**
2. Tạo AVD mới: chọn thiết bị bất kỳ, API Level **24+**
3. Nhấn ▶️ để khởi động emulator
4. Nhấn nút ▶️ **Run** trong Android Studio

#### Build bằng command line:
```bash
# Debug APK
./gradlew assembleDebug

# APK đầu ra tại:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📂 Cấu trúc dự án

```
Wao_fe/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/example/wao_fe/
│           │   ├── SplashActivity.kt       # Màn hình khởi động (Launcher)
│           │   ├── LoginActivity.kt        # Màn hình đăng nhập
│           │   └── RegisterActivity.kt     # Màn hình đăng ký
│           ├── res/
│           │   ├── layout/
│           │   │   ├── activity_splash.xml
│           │   │   ├── activity_login.xml
│           │   │   └── activity_register.xml
│           │   ├── drawable/
│           │   │   ├── splash_background.xml   # Gradient xanh lá
│           │   │   ├── bg_auth.xml             # Background trang auth
│           │   │   ├── bg_logo_circle.xml      # Logo tròn
│           │   │   ├── bg_button_green.xml     # Button gradient
│           │   │   ├── bg_card_white.xml       # Card trắng bo góc
│           │   │   ├── ic_email.xml            # Icon email
│           │   │   ├── ic_lock.xml             # Icon khóa
│           │   │   └── ic_person.xml           # Icon người dùng
│           │   ├── values/
│           │   │   ├── colors.xml      # Bảng màu xanh lá non
│           │   │   ├── strings.xml     # Chuỗi tiếng Việt
│           │   │   └── themes.xml      # Theme ứng dụng
│           │   └── values-night/
│           │       └── themes.xml      # Theme ban đêm
│           └── AndroidManifest.xml
├── gradle/
│   └── libs.versions.toml      # Quản lý phiên bản dependencies
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## 📦 Dependencies

| Thư viện | Phiên bản | Mục đích |
|----------|-----------|----------|
| `androidx.core:core-ktx` | 1.10.1 | Core Kotlin Extensions |
| `androidx.appcompat:appcompat` | 1.6.1 | Backward compatibility |
| `com.google.android.material:material` | 1.10.0 | Material Design UI Components |
| `junit:junit` | 4.13.2 | Unit Testing |
| `androidx.test.ext:junit` | 1.1.5 | Android Unit Testing |
| `androidx.test.espresso:espresso-core` | 3.5.1 | UI Testing |

---

## 🎨 Design System

### Bảng màu chính

| Tên | Mã màu | Dùng cho |
|-----|--------|----------|
| `green_primary` | `#4CAF50` | Màu chính, button, icon |
| `green_primary_dark` | `#2E7D32` | Status bar, gradient tối |
| `green_primary_light` | `#A5D6A7` | Gradient sáng |
| `green_accent` | `#69F0AE` | Màu nhấn |
| `green_light_bg` | `#E8F5E9` | Background nhẹ |

### Luồng màn hình

```
SplashActivity (2.5s)
        ↓
LoginActivity
        ↓ (chưa có tài khoản)
RegisterActivity
        ↓ (đăng ký xong)
LoginActivity
        ↓ (TODO: sau khi đăng nhập)
MainActivity (chưa phát triển)
```

---

## 🔧 Tính năng đã hoàn thành

- [x] Splash Screen với animation dots
- [x] Màn hình Đăng nhập
- [x] Màn hình Đăng ký
- [x] Validate form (email, mật khẩu, họ tên)
- [x] Hiển thị/ẩn mật khẩu
- [x] Điều hướng giữa các màn hình
- [x] Giao diện Material Design tone xanh lá

## 🚧 Tính năng đang phát triển

- [ ] Kết nối API backend (đăng nhập / đăng ký thật)
- [ ] Màn hình chính (Dashboard sức khỏe)
- [ ] Theo dõi bước chân, calo, nhịp tim
- [ ] Lịch sử hoạt động
- [ ] Thông báo nhắc nhở
- [ ] Quên mật khẩu

---

## 👨‍💻 Tác giả

**Trần Bá Lợi**
- GitHub: [@TranBaLoi](https://github.com/TranBaLoi)
- Repository: [Wao_fe](https://github.com/TranBaLoi/Wao_fe)

---

## 📄 License

Dự án này được phát triển cho mục đích học tập.

---

<p align="center">Made with 💚 by Trần Bá Lợi</p>

