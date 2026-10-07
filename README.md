# CoupleApp

Monorepo khởi đầu: logic KMP dùng chung, UI Android Compose và iOS SwiftUI riêng, Go API, Next.js landing/admin.

## Phạm vi hiện tại

- Feature đầu tiên: đếm ngày lịch, ngày bắt đầu tính là ngày 1; từ chối ngày tương lai.
- Android và iOS gọi cùng `LoveCounterFacade`; logic nằm trong `feature/lovecounter/domain`.
- Go có `/healthz`; Next.js có trang giới thiệu tối thiểu.
- Chưa tích hợp Firebase, persistence, liên kết tài khoản, admin hoặc triển khai cloud. Màn hình mobile ghi rõ đây là bản thử chưa lưu dữ liệu.
- `CoupleApp`, `dev.coupleapp.local` và Go module `example.com/coupleapp/backend` là định danh local tạm thời. Chốt định danh thật trước khi đăng ký Firebase/OAuth/signing.

Kết quả kiểm chứng thực tế được ghi trong [Báo cáo khởi tạo](docs/bootstrap-status.md). Cấu hình build không đồng nghĩa đã chạy thành công trên thiết bị.

Đã kiểm chứng: 9 KMP JVM tests, Android APK, Go build/health, Next production build và framework KMP iOS. Build ứng dụng SwiftUI còn bị chặn tại macro của Apple trong sandbox; xem báo cáo để chạy lại trên máy phát triển. Chưa kiểm thử UI trên thiết bị/emulator.

## Cấu trúc

```text
mobile/
  androidApp/                  # Compose, Navigation 3, presentation theo feature
  iosApp/                      # SwiftUI, NavigationStack, presentation theo feature
  shared/                      # Facade nhỏ cho Swift/Android; không chứa UI
  feature/lovecounter/         # Domain và test dùng chung
backend/
  cmd/api/                     # Composition/entry point
  internal/platform/httpapi/   # HTTP adapter
web/src/app/                   # Next.js App Router; chưa có admin
contracts/openapi.yaml         # API thực sự đã có
firebase/                     # Hướng dẫn cấu hình giai đoạn tiếp theo
docs/                         # Quy tắc kiến trúc và kết quả kiểm chứng
```

Không tạo repository hoặc data layer rỗng khi feature chưa truy cập dữ liệu. Khi thêm Firebase/API sẽ đặt adapter vào feature tương ứng, giữ domain độc lập SDK.

## Mobile

Yêu cầu JDK 17+, Android SDK Platform 37.0 và Build Tools 36.1.0, cùng Xcode trên Mac để build iOS. Phiên bản Kotlin/Android/library được khóa trong `mobile/gradle/libs.versions.toml`; Gradle wrapper có checksum distribution.

Tạo `mobile/local.properties` chứa đường dẫn SDK của máy, hoặc cấu hình `ANDROID_HOME`. File này không commit.

```sh
cd mobile
./gradlew :feature:lovecounter:jvmTest :shared:jvmTest
./gradlew :androidApp:assembleDebug
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

Mở project `mobile` bằng Android Studio. iOS: mở `mobile/iosApp/CoupleApp.xcodeproj`, chọn simulator và Run. Build phase của Xcode tạo `CoupleShared` từ KMP; Gradle/JDK phải truy cập được trong môi trường Xcode. Thiết bị thật cần signing/team riêng.

Project Xcode đã được sinh và đưa vào repo. Build script mặc định dùng wrapper; khi kiểm chứng trên máy này có thể đặt `COUPLE_GRADLE_EXECUTABLE` tới executable Gradle 9.4.1 có sẵn để dùng cùng phiên bản. Nếu sửa `project.yml`, dùng XcodeGen để sinh lại:

```sh
cd mobile/iosApp
xcodegen generate
```

Android min SDK 26, iOS deployment target 17 là mặc định kỹ thuật của prototype, chưa phải quyết định phạm vi thiết bị phát hành. SwiftUI NavigationStack đi cùng SDK Apple; Navigation 3 chỉ nằm ở Android host.

## Backend

```sh
cd backend
go run ./cmd/api
```

Mặc định chỉ lắng nghe `127.0.0.1:8080`. Kiểm tra `http://127.0.0.1:8080/healthz`. Đổi bằng `HTTP_ADDR` nếu cần kết nối từ emulator/thiết bị trong giai đoạn tích hợp; hiện mobile chưa gọi API.

```sh
go test ./...
go vet ./...
```

Backend chưa có nghiệp vụ nên chưa có unit test; kiểm tra health HTTP nằm trong bước smoke check. Thêm chi và Firebase Admin khi triển khai route auth/profile thay vì cài toàn bộ thư viện ngay từ scaffold.

## Web

```sh
cd web
npm ci
npm run dev
```

Kiểm tra build bằng `npm run build`, TypeScript bằng `npm run typecheck`. Dùng lockfile trong repo để tái lập dependency. Trang landing không tự tạo Anonymous user; route admin chưa tồn tại.

## Công việc kế tiếp

Tích hợp Firebase Anonymous native → shared auth contract → Go xác minh ID token. Giữ UID khi mở lại; sau đó thêm profile/bộ đếm bền vững và màn admin có quyền. Không dùng UID tự nhập hoặc mock token làm xác thực production.

## Research và kế hoạch

Tài liệu ghi lại nghiên cứu và quyết định tại thời điểm lập kế hoạch; tính năng đề xuất không đồng nghĩa đã triển khai. Trạng thái source thực tế nằm trong [báo cáo khởi tạo](docs/bootstrap-status.md).

- [Tính năng, luồng người dùng, log và analytics](docs/couple-app-product-operations-notes.md)
- [Kế hoạch phát triển](docs/couple-app-development-plan.md)
- [Research Clean Architecture theo nền tảng](docs/couple-app-clean-architecture.md)
- [Research và đề xuất thư viện](docs/couple-app-library-recommendations.md)
- [Kế hoạch sprint 01](docs/couple-app-sprint-01.md)
