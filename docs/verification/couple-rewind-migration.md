# Chuyển mobile sang CoupleRewind — 13/10/2026

Toàn bộ thư mục `mobile/`, bao gồm source UI đang chỉnh, chuyển sang `CoupleRewind/` trong repo hiện tại. Dự án có Gradle root độc lập, wrapper, version catalog và các module `androidApp`, `shared`, `feature:lovecounter`; iOS host SwiftUI nằm ở `iosApp`.

Đã đổi tên Gradle root, Xcode project/target/scheme, Swift app entry và Android display label thành CoupleRewind. Xcode project được sinh lại từ `project.yml`; Firebase `Package.resolved` được giữ. Đường dẫn build shared vẫn tương đối với project root.

App ID/bundle ID `dev.coupleapp.local`, namespace Kotlin và framework `CoupleShared` được giữ để không đổi identity/database của bản cài hiện hữu. Backend Go và web Next.js vẫn nằm ở repo cha. Không tạo project cloud hoặc tài khoản mới.

Kiểm chứng từ đường dẫn mới:

- `gradle clean :androidApp:assembleDebug :shared:jvmTest :feature:lovecounter:jvmTest --no-daemon -Pkotlin.compiler.execution.strategy=in-process`: BUILD SUCCESSFUL, 79 tasks chạy lại.
- XcodeGen sinh project/scheme CoupleRewind thành công.
- `plutil -lint` cho Xcode project và Info.plist qua.
- Swift parser kiểm tra app entry/UI qua.
- `git diff --check` qua.

Full iOS build/runtime sau chuyển chưa kiểm chứng do giới hạn quyền Xcode/SwiftPM/CoreSimulator đã ghi ở lượt trước. Kết quả Android không thay thế kết quả iOS.
