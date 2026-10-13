# CoupleRewind

Dự án Kotlin Multiplatform cho Android và iOS. Mở thư mục này bằng Android Studio; Gradle root và wrapper nằm ngay tại đây.

```text
CoupleRewind/
  androidApp/            # Android host, Compose và Navigation 3
  iosApp/                # iOS host, SwiftUI và NavigationStack
  shared/                # Composition, session và API bridge cho hai host
  feature/lovecounter/   # Domain, data và use case theo feature
  gradle/                # Version catalog và Gradle wrapper
  settings.gradle.kts
  build.gradle.kts
```

UI native riêng cho mỗi nền tảng. Domain và các quy tắc offline-first/sync dùng chung; domain không phụ thuộc Firebase hay UI SDK. Chỉ tạo thêm module khi một feature thực sự cần nó.

## Chạy Android

Yêu cầu JDK 17+, Android SDK Platform 37 và Build Tools 36.1.0. Khai báo SDK trong `local.properties` hoặc `ANDROID_HOME`.

```sh
./gradlew :androidApp:assembleDebug
./gradlew :feature:lovecounter:jvmTest :shared:jvmTest
```

## Chạy iOS

Mở `iosApp/CoupleRewind.xcodeproj` bằng Xcode 27, chọn scheme **CoupleRewind** và iPhone Simulator rồi Run. Build phase tạo framework `CoupleShared` từ Gradle. Cần JDK/Gradle trong môi trường Xcode.

`iosApp/project.yml` là nguồn cấu hình XcodeGen. Sau khi sửa cấu hình:

```sh
cd iosApp
xcodegen generate
```

## Định danh và dịch vụ

Tên Gradle root, Xcode project, target, scheme và tên hiển thị là **CoupleRewind**. Application ID/bundle ID vẫn là `dev.coupleapp.local`; package Kotlin và framework `CoupleShared` được giữ để bản cài tiếp tục đọc database local hiện có.

Go API và Next.js ở thư mục cha, không phải module Gradle. Cấu hình Firebase emulator được ghi tại `../firebase/README.md`. Chuyển dự án không tạo Firebase project hay đăng ký app mới trên tài khoản.

Lịch sử kiểm chứng trong `../docs` có thể dùng đường dẫn `mobile/` trước ngày chuyển; đường dẫn source hiện tại là `CoupleRewind/`.
