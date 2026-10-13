# CoupleApp

Monorepo khởi đầu: logic KMP dùng chung, UI Android Compose và iOS SwiftUI riêng, Go API, Next.js landing/admin.

## Phạm vi hiện tại

- Feature đầu tiên: đếm ngày lịch, ngày bắt đầu tính là ngày 1; từ chối ngày tương lai.
- Android và iOS quan sát `CounterSession`; logic nằm trong `feature/lovecounter/domain`, dữ liệu local qua repository của feature.
- Go có `/healthz`, GET/PUT `/v1/me/counter`, admin session gate và metadata support endpoint có audit. Next.js có landing, trợ giúp, trang thông tin dữ liệu và admin read-only được bảo vệ.
- Đã thêm Room KMP: lưu bộ đếm và outbox cùng transaction, đọc lại khi mở app. Android đã kiểm chứng lưu offline, sync Anonymous, pull, hai lựa chọn conflict và chặn editor cũ ghi đè dữ liệu mới. APK và framework KMP iOS build thành công; app SwiftUI đã build/cài/khởi động trên iOS Simulator; 17 native tests qua, gồm Room persistence. Thao tác UI và sync iOS end-to-end chưa kiểm chứng. Xem [bằng chứng triển khai](docs/phase-01-progress.md).
- Android debug đã kiểm chứng Firebase Anonymous và gửi outbox tới Go/Firestore Emulator, gồm sửa offline rồi mở lại có mạng. Mặc định chưa cấu hình vẫn local-only; iOS Auth end-to-end, liên kết Google/Apple và cloud thật chưa hoàn tất. Admin session/metadata/audit đã kiểm chứng bằng emulator, còn thiếu popup Google với cấu hình project hiện có. Xem [cách bật kiểm thử local](firebase/README.md).
- Mobile analytics và crash reporting dùng công tắc riêng, mặc định tắt, không hoạt động khi chạy emulator local hoặc thiếu cấu hình Firebase từ project có sẵn. Event chỉ chứa các trường đã cho phép; xem [quy ước observability](docs/mobile-observability.md).
- Mobile là dự án KMP [CoupleRewind](CoupleRewind/README.md), có Gradle root riêng. `dev.coupleapp.local` và Go module `example.com/coupleapp/backend` vẫn là định danh local tạm thời. Chốt định danh thật trước khi đăng ký Firebase/OAuth/signing.

Kết quả kiểm chứng thực tế được ghi trong [Báo cáo khởi tạo](docs/bootstrap-status.md). Cấu hình build không đồng nghĩa đã chạy thành công trên thiết bị.

Kết quả mới nhất: 41 feature + 12 shared JVM tests đã qua; Android APK và framework KMP iOS build qua. Go có kiểm thử tích hợp Auth/Firestore Emulator. Next.js typecheck/build và kiểm tra browser desktop/mobile đã qua. Các kết quả và giới hạn mới nhất nằm trong [tiến độ phase 01](docs/phase-01-progress.md); bằng chứng startup iOS được ghi riêng với 17 native tests; chưa đồng nghĩa toàn bộ UI/auth iOS đã nghiệm thu.

## Cấu trúc

```text
CoupleRewind/
  androidApp/                  # Compose, Navigation 3, presentation theo feature
  iosApp/                      # SwiftUI, NavigationStack, presentation theo feature
  shared/                      # Facade nhỏ cho Swift/Android; không chứa UI
  feature/lovecounter/         # Domain và test dùng chung
backend/
  cmd/api/                     # Composition/entry point
  internal/platform/httpapi/   # HTTP adapter
web/src/app/                   # Next.js App Router; landing và admin read-only
contracts/openapi.yaml         # API thực sự đã có
firebase/                     # Hướng dẫn cấu hình giai đoạn tiếp theo
docs/                         # Quy tắc kiến trúc và kết quả kiểm chứng
```

Không tạo repository hoặc data layer rỗng khi feature chưa truy cập dữ liệu. Khi thêm Firebase/API sẽ đặt adapter vào feature tương ứng, giữ domain độc lập SDK.

## Mobile

Yêu cầu JDK 17+, Android SDK Platform 37.0 và Build Tools 36.1.0, cùng Xcode trên Mac để build iOS. Phiên bản Kotlin/Android/library được khóa trong `CoupleRewind/gradle/libs.versions.toml`; Gradle wrapper có checksum distribution.

Tạo `CoupleRewind/local.properties` chứa đường dẫn SDK của máy, hoặc cấu hình `ANDROID_HOME`. File này không commit.

```sh
cd CoupleRewind
./gradlew :feature:lovecounter:jvmTest :shared:jvmTest
./gradlew :androidApp:assembleDebug
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

Mở project `CoupleRewind` bằng Android Studio. iOS: mở `CoupleRewind/iosApp/CoupleRewind.xcodeproj`, chọn simulator và Run. Build phase của Xcode tạo `CoupleShared` từ KMP; Gradle/JDK phải truy cập được trong môi trường Xcode. Thiết bị thật cần signing/team riêng.

`CoupleRewind.xcodeproj` là project Xcode thường, chỉnh trực tiếp trong Xcode (thêm file, Swift package, build setting); không dùng công cụ sinh project. Build script mặc định dùng wrapper; khi kiểm chứng trên máy này có thể đặt `COUPLE_GRADLE_EXECUTABLE` tới executable Gradle 9.4.1 có sẵn để dùng cùng phiên bản.

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

Backend có unit tests cho version/idempotency và HTTP boundaries. Test tích hợp chỉ chạy khi cấu hình cả Auth/Firestore Emulator local; nếu không chúng được skip. Xem [cách chạy emulator](firebase/README.md) và [hợp đồng sync](docs/counter-sync-contract.md). Không có `FIREBASE_PROJECT_ID` thì route bộ đếm trả 503, health vẫn hoạt động.

## Web

```sh
cd web
npm ci
npm run dev
```

Kiểm tra build bằng `npm run build`, TypeScript bằng `npm run typecheck`. Dùng lockfile trong repo để tái lập dependency. Trang landing không tự tạo Anonymous user. Admin fail-closed khi thiếu cấu hình và không tự tạo Firebase project.

## Công việc kế tiếp

Theo quyết định offline-first: database local → bộ đếm lưu/mở lại khi mất mạng → Firebase Anonymous khi online → hàng đợi đồng bộ qua Go. Sau đó hoàn thiện provider linking và màn admin có quyền. Không dùng UID tự nhập hoặc mock token làm xác thực production.

## Research và kế hoạch

Tài liệu ghi lại nghiên cứu và quyết định tại thời điểm lập kế hoạch; tính năng đề xuất không đồng nghĩa đã triển khai. Trạng thái source thực tế nằm trong [báo cáo khởi tạo](docs/bootstrap-status.md).

- [Tính năng, luồng người dùng, log và analytics](docs/couple-app-product-operations-notes.md)
- [Mobile analytics và crash reporting](docs/mobile-observability.md)
- [Kế hoạch phát triển](docs/couple-app-development-plan.md)
- [Research Clean Architecture theo nền tảng](docs/couple-app-clean-architecture.md)
- [Research và đề xuất thư viện](docs/couple-app-library-recommendations.md)
- [Kế hoạch sprint 01](docs/couple-app-sprint-01.md)

- [Research thành phần nền, UX và giữ chân người dùng](docs/couple-app-ux-retention-research.md)

- [Quyết định offline-first và hợp đồng đồng bộ đề xuất](docs/couple-app-offline-first.md)

## Kế hoạch triển khai hiện hành

[Phase 01: 6 màn native + offline-first + auth/sync + web tối thiểu](docs/couple-app-phase-01-implementation-plan.md) — 8 tuần cơ sở (320 giờ, gồm dự phòng), một người 40 giờ/tuần. Demo offline mục tiêu tuần 3; beta cloud mục tiêu tuần 8. Đây là ước lượng mới, thay lịch sprint 80 giờ trước yêu cầu offline-first.
