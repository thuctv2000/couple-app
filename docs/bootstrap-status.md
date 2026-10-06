# Khởi tạo source — cập nhật 06/10/2026

## Đã tạo

- Git repository local, nhánh `main`, chưa commit hoặc đẩy remote.
- Monorepo `mobile`, `backend`, `web`, `contracts`, `firebase`, `docs`.
- KMP feature `lovecounter` và shared facade; hai UI riêng gọi cùng facade.
- 9 ca test: ngày đầu, năm nhuận, đổi năm, ngày tương lai, múi giờ, DST, chuỗi ngày sai, ngày nhuận không hợp lệ, múi giờ sai.
- Project Android, Xcode project/scheme và Gradle wrapper có checksum.
- Go API `/healthz`, structured logging và graceful shutdown.
- Next.js landing tối thiểu, lockfile npm và OpenAPI phản ánh endpoint thực sự đã có.

## Môi trường kiểm tra

| Thành phần | Giá trị |
|---|---|
| Kotlin/KMP | 2.4.20 trong version catalog, đã resolve plugin |
| Navigation Android | Navigation 3 1.2.0 |
| UI iOS | SwiftUI + NavigationStack, deployment target prototype iOS 17 |
| Gradle | 9.4.1, wrapper cùng phiên bản; kiểm chứng dùng bản Gradle có sẵn trên máy |
| Android Gradle Plugin | 9.2.1, tương thích Gradle 9.4.1 |
| Android SDK | compile 37, target 36, min 26; Platform 37.0 bổ sung trong workspace, Build Tools 36.1.0 có sẵn |
| JDK thực tế chạy Gradle | 17.0.10 |
| Xcode / Swift | Xcode 27.0 / Swift 6.4 |
| Go | 1.26.2 |
| Node / npm | 25.9.0 / 11.12.1 |
| Next / React | 16.3.8 / 19.3.0 |

## Kết quả kiểm chứng

| Kiểm tra | Trạng thái |
|---|---|
| Go compile, `go test ./...`, `go vet ./...` | Qua; hiện chưa có Go unit test vì chỉ có health endpoint |
| Go HTTP smoke check | Qua: HTTP 200 và JSON health đúng; server dừng bình thường |
| Next production build | Qua |
| TypeScript | Qua |
| Next production HTTP smoke check | Qua: HTTP 200, nội dung landing có trong response |
| Xcode project/scheme | Đọc được target/scheme bằng `xcodebuild -list` |
| Import boundary domain, JSON, whitespace | Qua kiểm tra ban đầu |
| KMP JVM tests | Qua: 9 tests, 0 failures, 0 errors; gồm 6 domain tests và 3 facade tests |
| Android APK | Qua: `:androidApp:assembleDebug`; APK tại `mobile/androidApp/build/outputs/apk/debug/androidApp-debug.apk` |
| iOS KMP framework | Qua: `:shared:linkDebugFrameworkIosSimulatorArm64`, tạo `CoupleShared.framework` |
| Xcode tích hợp framework | Qua: build phase `:shared:embedAndSignAppleFrameworkForXcode` hoàn tất |
| iOS SwiftUI app | Chưa qua: compiler macro `SwiftUIMacros.StateMacro` bị chặn bởi `sandbox-exec: sandbox_apply: Operation not permitted`; Xcode kết thúc với mã 65 |

## Lưu ý phạm vi

- Đây là source cho bước nền móng, chưa hoàn tất S01-02/S01-03 hoặc sprint 01 nếu các tiêu chí build/chạy hai app chưa đạt.
- Firebase Auth, dữ liệu bền vững, link Google/Apple, admin và AI chưa triển khai. Source không chứa account hoặc secret thật.
- `dev.coupleapp.local` và tên CoupleApp là placeholder dùng local, cần đổi trước cấu hình provider thật.
- Chưa thử thiết bị thật hoặc ký phát hành. UI hiện chỉ tính khi nhấn nút, chưa tự cập nhật qua nửa đêm và chưa lưu dữ liệu.
- Không thay cấu hình JDK/Xcode/SDK mặc định toàn máy. Cache build mới đặt trong workspace; cache Gradle cũ được sao chép riêng để dùng lại dependency.
- Bảng tương thích KMP tham chiếu Xcode 26.4; máy có Xcode 27.0. Framework simulator arm64 đã build trên máy này; chưa thể kết luận toàn bộ app tương thích hoặc chạy được khi bước SwiftUI còn bị chặn.

## Kiểm chứng iOS còn lại

Chạy bằng Terminal hoặc Xcode trên máy phát triển ngoài môi trường công cụ đang bị giới hạn. Không cần signing để kiểm tra build simulator:

```sh
cd mobile
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/CoupleApp.xcodeproj \
  -scheme CoupleApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath build/ios-derived-data \
  CODE_SIGNING_ALLOWED=NO ARCHS=arm64 ONLY_ACTIVE_ARCH=YES build
```

Sau khi build qua, mở app trên simulator để thử ngày đầu, ngày trong quá khứ, thay đổi ngày và quay lại màn hình. Hiện chưa có bằng chứng chạy UI Android hoặc iOS. Lỗi macro trên là giới hạn của lượt kiểm tra hiện tại; không sửa bỏ `@State` hoặc tắt sandbox trong source để che lỗi.

## Các vấn đề đã xử lý

- Gradle mặc định không ghi được cache trong sandbox: dùng cache nằm trong workspace.
- AGP 9.3 yêu cầu Gradle 9.5: chọn AGP 9.2.1 phù hợp Gradle 9.4.1; giữ Kotlin và Navigation theo yêu cầu.
- Build đầu tìm Build Tools 36.0.0 chưa cài: chỉ định bản 36.1.0 đã có trên máy.
- Navigation 3/Compose mới yêu cầu compile SDK 37: tải Platform 37.0 từ Google, kiểm tra checksum, cài riêng vào SDK workspace và nâng compileSdk lên 37. Android build đã qua; chưa nâng targetSdk/minSdk theo thay đổi compileSdk này.
- Tải dependency lần đầu chậm; tái sử dụng bản sao cache, không tắt kiểm tra dependency để giả lập kết quả thành công.
- Kotlin daemon không ghi được thư mục mặc định trong sandbox; lượt test đã fallback sang compiler trong tiến trình và hoàn tất. Lượt build tiếp theo dùng `-Pkotlin.compiler.execution.strategy=in-process`.
- Kotlin/Native 2.4.20 được tải từ Maven Central, đối chiếu SHA-1 do kho công bố và đặt trong workspace. Cấu hình `kotlin.native.home` chỉ nằm ở cache local, không đưa đường dẫn máy vào source.
- Gradle cần socket nội bộ để điều phối build. Sau khi cấp quyền mạng cho lượt chạy, build phase KMP trong Xcode đã qua; bước SwiftUI vẫn bị giới hạn sandbox như ghi ở trên.
