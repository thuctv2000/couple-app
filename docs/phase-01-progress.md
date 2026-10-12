# Triển khai phase 01 — nhật ký bằng chứng

Cập nhật 11/10/2026. Nhánh `feat/offline-first-phase-01`.

## Đang thực hiện

- Thêm Room KMP stable 2.8.5, KSP 2.3.12, bundled SQLite 2.7.1; build Android/JVM/iOS framework đã qua.
- Local space, counter settings, outbox và conflict snapshot schema v3, migration v1/v2 giữ dữ liệu. Ghi settings + operation trong một transaction; bỏ qua lần lưu không đổi; giữ payload từng revision.
- Repository port/domain validation; adapters local cho Android, iOS và JVM test.
- Shared session quan sát dữ liệu, đếm theo ngày/múi giờ và làm mới định kỳ trong lúc màn đang quan sát.
- Compose và SwiftUI có bố cục sáu màn, lưu local; account/cloud ghi rõ chưa tích hợp, không giả báo đồng bộ thành công.
- Test JVM: reopen giữ owner/data/queue, invalid input không ghi, no-op save không thêm operation, concurrent writes dùng một owner và revision liên tục; outbox insert lỗi phải rollback counter.
- Go: domain decision cho version/idempotency, adapter Firestore transaction, Firebase Admin verifier và GET/PUT `/v1/me/counter`. Toàn bộ Go build/tests và hai test tích hợp Firebase Emulator đã qua với race detector. [Hợp đồng](counter-sync-contract.md).

## Bằng chứng ngày 09/10/2026

- Gradle: `:feature:lovecounter:jvmTest :shared:jvmTest :androidApp:assembleDebug :shared:linkDebugFrameworkIosSimulatorArm64` thành công.
- 13 tests, không failure/error: 6 tính ngày, 4 persistence/transaction, 3 facade.
- Go: 5 test functions domain (bao gồm bảng 10 dữ liệu không hợp lệ) và 4 HTTP test functions qua `go test -race`; verifier/repository là test doubles trong HTTP tests, không thay thế integration Firebase.
- Android emulator `emulator-5554`: cài APK, mở welcome, chọn ngày 2026-10-09. Tắt Wi-Fi và mobile data trước khi lưu; home hiện ngày 1, một operation chờ đồng bộ. Force-stop rồi mở lại khi vẫn tắt mạng: ngày bắt đầu, bộ đếm và pending count giữ nguyên. Đã khôi phục Wi-Fi/mobile data về trạng thái ban đầu.
- [UI tree sau reopen](verification/android-offline-reopen.xml), [ảnh home thực tế](verification/android-offline-home.png). Không phải ảnh concept.
- Xcode build SwiftUI thất bại ở `SwiftUIMacros.StateMacro`: `sandbox-exec: sandbox_apply: Operation not permitted`. Framework KMP build thành công không thay thế việc chạy app iOS.

## Chưa được chứng minh

- Toàn bộ luồng UI/khả năng tiếp cận Android; UI chạy thực tế trên iOS; crash giữa transaction, lỗi hết dung lượng.
- Firebase/config cloud thật, mobile sync worker, Google/Apple, admin, account deletion.
- Đây chưa phải mốc M1/M2/M3 hoàn tất. Xem [plan](couple-app-phase-01-implementation-plan.md).

## Bằng chứng ngày 10/10/2026 — Go và Firebase Emulator

- `go test -race -count=1 -v ./...` với Auth tại `127.0.0.1:19099`, Firestore tại `127.0.0.1:18081` đã qua. [Output](verification/go-emulator-tests.txt).
- Test tích hợp không bị skip: tạo Anonymous local, xác thực token qua Admin SDK, ghi/đọc Firestore thật trong emulator, retry không tăng version, account khác không đọc được bộ đếm, token tài khoản đã xóa bị từ chối.
- Bốn thao tác cùng base version chạy đồng thời: đúng một thao tác thắng, một receipt được lưu, version là 1.
- Token client đọc/ghi Firestore trực tiếp đều bị rules từ chối 403. Không deploy rules hay tạo project cloud.
- Lỗi dịch vụ xác thực tạm thời trả 503 thay vì 401 để client không hiểu nhầm phải đăng nhập lại; có HTTP test riêng.
- Chỉ kiểm chứng local emulator; chưa kiểm tra provider Google/Apple, IAM hoặc vận hành cloud thật. Mobile vẫn chưa gửi outbox.

## Bằng chứng ngày 10/10/2026 — hàng đợi mobile

- Schema v2 lưu server version và revision đã ACK; migration v1 → v2 không xóa dữ liệu. Fixture SQL của test được xuất từ schema v1 đã lưu trong repo.
- Gắn UID một lần, từ chối tự chuyển sang UID khác. Chuẩn bị thao tác lưu bền base version trước lần gửi đầu; mở lại app vẫn nhận đúng operation/body cũ.
- ACK kiểm tra UID, operation và version; cập nhật cursor + xóa đúng operation trong cùng transaction. ACK lặp không xóa bản sửa mới, bản sửa tạo trong lúc gửi được giữ nguyên.
- 3 test mới qua: retry/reopen/ACK cùng bản sửa mới, sai tài khoản/ACK sai không đổi queue, migration giữ dữ liệu. Tổng feature suite 13 tests qua. [Kết quả](verification/counter-sync-queue-tests.xml).
- Build APK và iOS simulator framework qua. Cài đè APK lên emulator đang có dữ liệu v1: home vẫn ngày bắt đầu 2026-10-09, bộ đếm 2 và một thay đổi đang chờ. [UI sau nâng cấp](verification/android-after-schema-v2.xml).
- Port `CounterSyncQueue` và local adapter đã có; native Auth, network worker, conflict resolution và retry scheduler chưa nối. Không coi bước này là sync end-to-end.

## Bằng chứng ngày 10/10/2026 — điều phối sync dùng chung

- `SyncCounter` dùng queue/identity/remote ports; serialize các lần chạy bằng mutex, giới hạn số thao tác mỗi lượt. Token không nằm trong outbox hay `toString()` của credential.
- Kiểm tra UID trước khi gửi và trước ACK; 401 chỉ refresh token và thử lại một lần cho thao tác đó. Retry giữ nguyên operation/body đã chuẩn bị.
- ACK phải khớp operation ID, version kế tiếp và toàn bộ payload. Conflict, forbidden, validation, lỗi mạng mô phỏng, đổi account và cancellation giữ queue.
- 4 tests worker qua với Room thực và identity/remote test doubles: sửa trong lúc gửi, refresh giới hạn, ACK sai/đổi account, các nhóm lỗi và cancellation. [Kết quả](verification/sync-worker-tests.xml).
- Feature JVM suite hiện 17 tests qua; iOS simulator framework build qua. Chưa có adapter HTTP/native Firebase, chưa có backoff scheduler, chưa gọi worker từ UI; không coi đây là sync mạng thực.

## HTTP adapter — kiểm chứng 10/10/2026

- Thêm Ktor 3.6.0, JSON serialization, engine OkHttp/Android, Darwin/iOS và CIO/JVM. Version đối chiếu [release chính thức](https://ktor.io/docs/releases.html); APK Android, feature JVM và iOS simulator framework build thành công.
- Gửi PUT theo hợp đồng Go, Bearer token chỉ nằm trong header; 409 phân biệt version conflict và operation ID bị tái sử dụng. Phản hồi JSON lỗi trả `InvalidResponse`, không ACK queue.
- HTTPS mặc định; HTTP cần bật rõ ràng và chỉ cho phép địa chỉ local/emulator. Client mặc định không follow redirect, có timeout; không cài HTTP logging.
- 3 test adapter với MockEngine cho request/ACK, mã lỗi và sai UID/origin đã qua. Feature suite tổng 20 tests, 0 failure/error/skip. [Kết quả HTTP tests](verification/http-counter-tests.xml).
- Native Auth và UI vẫn chưa gọi worker. Đây là tiến độ triển khai, không phải bằng chứng sync end-to-end.

## Giới hạn bản UI đang làm

- Dùng component native đơn giản theo bố cục concept, chưa tái hiện toàn bộ minh họa/polish.
- Múi giờ chọn từ danh sách; vẫn cần rà localized labels và trải nghiệm tìm kiếm.
- iOS editor chưa lưu draft khi process bị hủy; Android rememberSaveable không phải thay thế database draft.
- Foreground refresh hook đã thêm, cần kiểm chứng runtime; background scheduler, retry database-open và error recovery chưa hoàn thiện.
- Chưa có cloud thật. Android debug có thể gửi tới Go + Firebase Emulator khi bật rõ cấu hình; mặc định local-only. iOS chưa nối Auth/sync.

## Bằng chứng ngày 11/10/2026 — Android Anonymous và sync thực trên emulator

- Native Firebase Auth dùng BoM 35.0.0, tạo/khôi phục Anonymous qua SDK; token được lấy theo UID hiện tại. Không đăng ký project cloud. [Tài liệu Anonymous](https://firebase.google.com/docs/auth/android/anonymous-auth).
- `-Pcouple.emulator=true` bật cấu hình demo chỉ ở debug; release luôn tắt. Dữ liệu demo và API key placeholder chỉ dùng emulator. Internet permission đã thêm; HTTP cleartext chỉ cho `10.0.2.2` trong debug.
- Shared session thử sync sau khi lưu local, khi foreground và khi người dùng bấm thử lại. UI thể hiện syncing/retry/conflict/error thực; ghi rõ máy chủ local không phải backup cloud.
- APK debug có SDK build qua; 20 feature tests và 3 shared tests qua. Trên Android đang có một operation local: Anonymous → Go → Firestore thành công; queue về 0, server version 1, ngày 2026-10-09 giữ nguyên.
- Tắt Wi-Fi/mobile data, sửa tên thành Mai, lưu: dữ liệu hiển thị ngay, pending=1 và báo chưa kết nối. Bật lại mạng, force-stop/mở lại: tên còn nguyên, pending=0; Firestore có đúng một counter, version=2 và tên Mai. Đã khôi phục trạng thái mạng ban đầu.
- Bằng chứng: [offline UI](verification/android-offline-edit.xml), [reconnect UI](verification/android-reconnect.xml), [server đã đối chiếu](verification/android-emulator-sync.json).
- Giới hạn: chưa auto-retry khi kết nối đổi mà app vẫn foreground, chưa background scheduler/backoff, chưa pull/resolve conflict, chưa Google/Apple hay iOS Auth. M2 chưa hoàn tất.

## iOS Anonymous — đang kiểm chứng 11/10/2026

- Thêm native FirebaseAuth Swift adapter và callback bridge `NativeCounterIdentity`; UID/token qua bridge để worker KMP gửi HTTP. Callback đến sau cancellation bị bỏ qua.
- Framework iOS build thành công; hai bridge tests JVM qua (refresh/native error và late callback sau cancellation). Shared suite có 5 tests qua.
- XcodeGen thêm FirebaseCore/Auth bằng SPM, pin 12.19.2. Firebase 13 mới nhất hiện cảnh báo Google Sign-In qua SPM chưa tương thích; giữ 12.19.2 để chuẩn bị yêu cầu Google login. [Release notes](https://firebase.google.com/support/release-notes/ios).
- Debug iOS Simulator chỉ bật emulator nếu biến môi trường `COUPLE_FIREBASE_EMULATOR=1`; dùng Auth 127.0.0.1:19099 và Go 127.0.0.1:18080. Mặc định vẫn offline; release không bật đoạn emulator.
- UI đã nối trạng thái và thử lại, chưa kiểm chứng chạy. SPM fetch đã kết thúc với lỗi ghi diagnostics vào `/Users/antkingmini/Library/Caches/org.swift.swiftpm/manifests/ManifestLoading`: Operation not permitted. Yêu cầu quyền ghi cache chưa được cấp. CoreSimulator cũng báo không kết nối dịch vụ. Chưa có build SwiftUI mới hoàn tất; không dùng kết quả framework để thay bằng chứng app iOS.

## Retry foreground — kiểm chứng 11/10/2026

- Lỗi transient được thử lại với exponential backoff + jitter, tối đa 6 lần retry, khoảng chờ tối đa 60 giây. Các lỗi conflict/account/permission/validation không bị retry vô hạn.
- Hết batch tiếp tục batch kế tiếp; manual retry hoặc foreground có thể đánh thức backoff. Android onStop và iOS scene inactive hủy retry foreground; lưu hoàn tất sau khi xuống nền không khởi động lại retry ngoài ý muốn.
- Hai policy tests và hai session tests với virtual clock đã qua: tự gửi tiếp không cần thao tác mới; xuống nền dừng, yêu cầu mới khi paused không gửi; foreground tiếp tục.
- [Policy tests](verification/retry-policy-tests.xml), [session tests](verification/session-retry-tests.xml). Feature suite 22 tests và shared suite 7 tests qua; APK Android và framework iOS build qua.
- Chưa có Android WorkManager/iOS background task. Chưa kiểm chứng lại retry tự động bằng thao tác bật/tắt mạng trên thiết bị; bằng chứng Android reconnect trước đó có đóng/mở app.

## Nền tảng xử lý conflict — kiểm chứng 11/10/2026

- HTTP GET `/v1/me/counter` đọc snapshot trong scope token hiện tại; phân biệt 404, lỗi quyền/auth, transient và JSON/version/payload sai. Không tự ghi đè local.
- Schema v3 lưu server snapshot riêng cùng local revision đã hiển thị. Queue dừng khi có conflict đã lưu; mở lại giữ cả bản local và snapshot server.
- Chọn local tạo operation ID mới ở base server version đã chọn. Chọn server cập nhật local và dọn queue của owner. Toàn bộ thao tác trong transaction, kiểm tra UID và revision/version chống lựa chọn cũ ghi đè bản sửa mới.
- Bốn conflict-store tests qua, gồm migration v2 → v3 giữ UID/cursor/outbox; test migration v1 cũ cũng qua theo chuỗi v1 → v2 → v3. Hai HTTP GET tests bổ sung qua. Feature suite 28 và shared suite 7 tests, 0 lỗi; APK Android và framework iOS build qua.
- [Conflict tests](verification/conflict-store-tests.xml), [HTTP tests](verification/http-counter-tests.xml).
- Chưa nối tự tải snapshot khi 409, chưa có UI chọn hai bản và chưa kiểm thử conflict end-to-end trên thiết bị. Đây mới là storage/transport và quy tắc resolve, không phải màn conflict hoàn chỉnh.


## Conflict trong ứng dụng — kiểm chứng 11/10/2026

- Shared session gặp 409 tải snapshot, kiểm tra UID trước/sau request; 401 refresh đúng một lần. Snapshot được lưu riêng, không tự thay local. Lỗi tải transient đi qua backoff hiện có.
- Android Compose và iOS SwiftUI có bản so sánh tên/ngày/múi giờ, hai lựa chọn và hộp xác nhận. Chọn bản server giải thích việc thay local/dọn hàng chờ; giữ local giải thích việc gửi lại và có thể gặp conflict mới.
- Lựa chọn mang revision/version đã hiển thị; sửa local sau preview sẽ vô hiệu hóa nút và yêu cầu cập nhật bản so sánh. Transaction kiểm tra lại khi xác nhận. Session dừng worker trước resolve; giữ local tiếp tục gửi sau resolve.
- 5 tests tải conflict và 4 tests session mới qua: refresh giới hạn, đổi UID, 404, cancellation, hai lựa chọn, sửa sau preview và đổi account trước lựa chọn. Tổng 33 feature + 11 shared tests, không failure/error/skip. [Loader](verification/load-conflict-tests.xml), [session](verification/session-conflict-tests.xml).
- Android APK build/cài đè thành công. Framework KMP iOS build qua; SwiftUI mới chưa được build/chạy do giới hạn môi trường iOS đã ghi phía trên.
- Runtime Android: mô phỏng thiết bị khác trên Firestore Emulator nâng version 2 → 3, sửa local nhận conflict và hiển thị cả hai bản. Chọn server: queue về 0, đóng/mở lại vẫn tên Server Mai. Mô phỏng thay đổi tiếp version 4, sửa local rồi chọn giữ local: Go ACK version 5, Firestore tên Server MaiLocal và queue về 0.
- Bằng chứng: [so sánh](verification/android-conflict-comparison.xml), [chọn server](verification/android-conflict-server-choice.xml), [mở lại](verification/android-conflict-server-reopen.xml), [giữ local](verification/android-conflict-local-choice.xml), [đối chiếu server](verification/android-conflict-result.json).
- Chỉ dùng project demo của emulator. Không tạo project cloud hay project mới trên tài khoản.
- Chưa hoàn tất M2: còn pull bình thường khi không có pending, Google/Apple/link account, background sync, cấu hình Firebase thật và kiểm chứng app iOS. Không coi màn conflict Android đã chạy là bằng chứng toàn phase hoàn tất.


## Nhận dữ liệu khi foreground — kiểm chứng 11/10/2026

- Shared session tải GET khi mở app/thử đồng bộ, kể cả outbox bằng 0; sau khi gửi hết queue cũng đối chiếu server. Chưa có identity thì không tạo Anonymous chỉ vì pull và không gọi mạng.
- Transaction áp dụng snapshot kiểm tra UID, phiên bản, conflict và queue. Chỉ cập nhật local/cursor cùng lúc khi không có thay đổi chưa ACK. Snapshot cũ hoặc cùng version nhưng payload khác bị từ chối; snapshot giống nhau không tăng local revision.
- Bản sửa tạo trong lúc GET đang chạy được giữ nguyên cùng queue/base version trước đó; session chuyển lại sang gửi và xử lý 409 nếu cần. GET 404 không xóa dữ liệu local. Xóa tài khoản/server vẫn cần luồng riêng.
- 6 tests dùng Room thật qua: restore và reopen, cursor của lần sửa sau restore, dữ liệu cũ, sửa trong lúc tải, đổi UID/404, không có identity và refresh 401/conflict đã lưu. Một session test qua cho pull khi queue trống, retry transient và pause/foreground. Tổng feature 39 + shared 12 tests, 0 lỗi. [Storage/domain](verification/pull-counter-tests.xml), [session](verification/session-pull-tests.xml).
- Build Android APK và framework KMP iOS qua. Android cài đè: sửa trực tiếp dữ liệu emulator giả lập thiết bị khác từ version 5 lên 6, mở app nhận tên Mai synced dù queue bằng 0. Tắt Wi-Fi/mobile data, đóng/mở app: dữ liệu đã nhận vẫn hiện ngay và báo chưa kết nối. Mạng đã được bật lại như trước test.
- [Foreground UI](verification/android-pull-foreground.xml), [offline reopen](verification/android-pull-offline-reopen.xml). Chỉ local emulator, không tạo project cloud. Native iOS vẫn chưa có bằng chứng build/runtime.
- Pull này chạy lúc foreground/manual và sau push; chưa phải realtime subscription hay background scheduler. Google/Apple/link account, deletion, web/admin và các gate beta vẫn còn trong phạm vi cần làm.


## Bảo vệ bản nhập khi đồng bộ cập nhật — kiểm chứng 11/10, ghi nhận 12/10/2026

- Android/iOS editor giữ local revision lúc mở. Lưu kiểm tra revision trong cùng transaction ghi counter/outbox; thay đổi từ pull/editor khác trả `edit_changed`, không ghi đè và không tạo operation.
- UI giữ draft, hiện bản mới và cho chọn “Dùng bản mới để sửa” sau xác nhận. Không tự đổi revision của draft cũ để tiếp tục ghi.
- Hai tests Room bổ sung qua: pull khi editor mở; hai editor cùng revision chỉ một lần ghi thành công. Tổng 41 feature + 12 shared tests, 0 lỗi. [Repository tests](verification/counter-repository-tests.xml).
- Android APK/framework KMP iOS build qua. Runtime Android: draft Mai syncedDraft được giữ khi server đổi 6 → 7 ở nền; lưu bị chặn. Xác nhận dùng bản Updated elsewhere rồi lưu không đổi: home đúng, queue 0, server vẫn 7.
- [Lưu bị chặn](verification/android-editor-stale-save.xml), [nạp bản mới](verification/android-editor-reload.xml). SwiftUI chưa có native build/runtime. Ghi chú bị gián đoạn bởi giới hạn sử dụng hôm 11/10; bổ sung từ bằng chứng hiện có, không chạy lại kiểm thử không cần thiết.


## Web landing/help/data — kiểm chứng 12/10/2026

- Next.js có `/`, `/help`, `/privacy`; header/footer dùng chung, landing nằm trong feature riêng. Thiết kế responsive màu kem/hồng, typography hệ thống, không tải font ngoài. Minh họa counter ghi rõ là minh họa.
- Nội dung phản ánh bản hiện tại: offline-first, phân biệt local với backup, conflict/editor stale, trạng thái chưa có bản tải công khai; Google/Apple, AI/social và deletion ghi rõ đang hoàn thiện. Không dựng form thu email, store link, đăng nhập hay admin giả.
- Trang dữ liệu là mô tả bản thử nghiệm, không tuyên bố là chính sách phát hành hoàn chỉnh. Còn thiếu đơn vị vận hành, liên hệ hỗ trợ, retention/deletion và cấu hình deployment trước khi phát hành.
- `npm run typecheck` và `npm run build` qua trong `web`; ba route prerendered. Bản production local chạy `npm run start -- --port 13000` tại 127.0.0.1.
- Browser QA: desktop và viewport 390×844; landing/privacy có contentWidth=clientWidth=375 (phần còn lại là scrollbar), không tràn ngang. Mở FAQ backup, dùng Return đóng được; link landing→help→privacy hoạt động. Đã quan sát screenshot cả ba trang, reset viewport sau test.
- [Biên bản kiểm tra web](verification/web-public-pages.md). Chưa triển khai public, chưa tạo project mới; admin auth/RBAC/audit còn chưa làm. P01-19 có nội dung/UI thực, chưa hoàn tất điều kiện phát hành/privacy cuối cùng.

## Kiểm chứng lại iOS sau thay đổi quyền — 12/10/2026, đang chạy

- `simctl list` truy cập được iPhone 17/iOS 27 đang boot. Quyền filesystem mới cho phép Xcode tiếp tục bước dependency trước đây bị chặn.
- Đã chạy build app SwiftUI Debug bằng `xcodebuild`, destination simulator có sẵn, signing tắt cho simulator; không đăng ký project cloud hay thay đổi tài khoản.
- SwiftPM đã tạo `Package.resolved` gồm 13 dependencies, Firebase 12.19.2. Tiến trình vẫn live khi kiểm tra; thư mục artifacts tăng từ 18 MB lên 164 MB. Chưa có `BUILD SUCCEEDED` hoặc `BUILD FAILED`, không coi package resolution là build thành công.
- Phiên build cần tiếp tục quan sát: tool session 88475; log local `work/ios-full-build.log` tại workspace, derived data `work/ios-derived-data`. Không khởi động build khác khi tiến trình này còn chạy.
- Công cụ thao tác Device Hub đang chờ quyền Accessibility/Screen Recording của macOS. Chưa có kiểm chứng UI iOS; không thay thế bằng kết quả Android hoặc framework KMP.

## Backend access log — 12/10/2026

- Thêm middleware quanh Go router: UUID do server tạo trong `X-Request-ID`, route pattern, method đã giới hạn, status và duration; JSON stdout dùng logger sẵn có. Không lấy request ID do caller gửi.
- Không ghi URL/query/body/header/IP/UID. Tests cài dữ liệu nhạy cảm giả trong từng vị trí và xác nhận không lọt log; kiểm tra status đầu tiên và cấp INFO/WARN/ERROR.
- `go test -race ./...` và `go vet ./...` qua. Emulator integration không bật trong lượt này; không thay thế bằng chứng integration trước. [Kết quả focused tests](verification/go-access-log-tests.txt), [quy ước và giới hạn](backend-observability.md).
- Chưa có metrics/tracing, panic reporting, admin audit, mobile crash/analytics hay retention cấu hình production. Không tạo dịch vụ/project mới.
- Build iOS session 88475 vẫn live tại kiểm tra cuối; artifacts tăng lên 416 MB, chưa có kết luận build. Tiếp tục quan sát cùng tiến trình.

## Admin server gate — 12/10/2026

- Có `GET /v1/admin/session`: Firebase token verified/revocation checked, đọc current custom claim `admin` kiểu boolean từ user record. Không tin query/header/role của UI, trả no-store và không lộ lỗi upstream.
- Thu hồi role được áp dụng ở request kế tiếp dù dùng lại token còn hạn. Chưa có endpoint cấp quyền hay tự nâng quyền tài khoản.
- Unit/race suite và vet qua. Integration thật với local Auth Emulator: invalid token 401, user thường 403, role cấp cho test user 200, user khác 403, xóa role rồi token cũ 403. Chỉ tạo/xóa dữ liệu test của chính test trong emulator.
- [Kết quả emulator](verification/go-admin-emulator-tests.txt), [contract/giới hạn](admin-access-contract.md). Còn Next.js admin sign-in/session, trang bảo vệ, metadata/audit và quyền từng thao tác; P01-20 chưa hoàn tất. Không tạo project/tài khoản cloud.


## iOS app build và native persistence — kiểm chứng 12/10/2026

- Build session 88475 đã kết thúc lỗi Swift tại hàm lấy token. Đối chiếu source Firebase 12.19.2, đổi sang `getIDToken(forcingRefresh:)`; build lại session 17777 **thành công**, bao gồm SwiftUI và Firebase Auth.
- Cài app vào iPhone 17/iOS 27 simulator, khởi động local-only: tiến trình sống, Room tạo schema 3 đúng và settings trống. Relaunch bật emulator cũng giữ tiến trình chạy; chưa coi đây là bằng chứng sync mạng.
- Thêm native Room persistence test trong `iosTest`, chỉ dùng thư mục tạm riêng: save/reopen, giữ operation đã chuẩn bị, ACK, pull và reopen bản mới. Chạy `:feature:lovecounter:iosSimulatorArm64Test :shared:iosSimulatorArm64Test` qua: **14 feature + 3 shared**, 0 failure/error/skip. JVM 53 tests trước đó là lượt chạy riêng, không cộng thành số test duy nhất.
- [Báo cáo build/startup](verification/ios-build-and-launch.md), [native test XML](verification/ios-native-tests/TEST-iosSimulatorArm64Test.dev.coupleapp.lovecounter.data.local.NativeCounterPersistenceTest.xml). Xcode Package.resolved giữ 13 dependencies.
- Chưa kiểm chứng thao tác màn hình iOS do quyền Computer Use; provider login/link, iOS sync end-to-end và beta vẫn còn. Không tạo project cloud. App đang chạy bản Debug emulator trong simulator, console session 2579; đây là tiến trình app, không phải build còn chờ.

## Next.js admin session — kiểm chứng 12/10/2026

- Thêm login Google/Firebase, `/api/admin/session`, `/admin`, logout và cấu hình môi trường fail-closed. Cookie HttpOnly/SameSite Strict, HTTPS Secure/__Host-, hết hạn tối đa 55 phút; mỗi server render hỏi lại Go, không xuất token trong HTML.
- Mutation kiểm tra Origin cấu hình, giới hạn body streamed 4 KiB; backend URL cố định, no-store, không theo redirect và có timeout. Emulator không được trộn project thật. Trang thiếu config báo rõ không sẵn sàng.
- Firebase Web SDK 13.0.0 đã cài và pin exact. Ba tests logic cấu hình/Origin/verify, TypeScript và production build đều qua; route build gồm `/admin`, `/admin/login`, session và logout dạng dynamic.
- Integration xuyên Next.js → Go → Firebase Auth Emulator đã qua: chưa đăng nhập redirect; thiếu/sai Origin, body quá 4 KiB và JSON dư field bị từ chối; user thường 403; admin nhận cookie đúng HttpOnly/SameSite/path/TTL; HTML không lộ token; xóa role chặn nội dung ngay với cùng cookie; logout xóa cookie.
- Trang login khi thiếu Firebase Web config hiển thị “Đăng nhập chưa được cấu hình”; `/admin` redirect về login. Chưa kiểm chứng popup Google vì chưa có cấu hình web app của Firebase project hiện có. Không tạo project cloud.
- [Kết quả integration](verification/next-admin-emulator-tests.txt), [contract và giới hạn](admin-access-contract.md). Popup Google vẫn cần cấu hình Firebase Web app hiện có; P01-20 chưa hoàn tất toàn bộ.

## Admin metadata và audit — kiểm chứng 12/10/2026

- Go có endpoint read-only `/v1/admin/users/{uid}/metadata`; mỗi request kiểm tra token, revocation và current admin claim. Response chỉ có trạng thái account/provider, timestamp Firebase, trạng thái/version counter và yêu cầu xóa.
- Không trả email, số điện thoại, display name, provider UID, payload counter hay nội dung quan hệ. Mỗi lookup đã xác thực ghi `adminAudit` gồm actor, target, action, outcome và thời gian server; audit lỗi thì lookup trả 503.
- Next.js có bridge same-origin `/api/admin/lookup` với body streamed tối đa 1024 bytes, HttpOnly cookie, upstream cố định/no-store/no redirect/timeout và kiểm tra chặt response. `/admin` có form read-only và ghi rõ hành động được audit.
- Integration backend qua Auth/Firestore Emulator đạt, gồm success/not-found audit, không lộ dữ liệu riêng và thu hồi role tức thì. [Kết quả backend](verification/go-admin-metadata-emulator-tests.txt).
- Integration Next.js → Go → Emulator đạt, gồm body quá giới hạn, sai Origin, metadata thành công, audit được ghi và cookie cũ bị từ chối sau thu hồi role. [Kết quả web](verification/next-admin-emulator-tests.txt).
- `go test -race ./...`, `go vet ./...`, web tests, TypeScript, production build và OpenAPI YAML đều đạt. Chỉ dùng `demo-couple-app` cục bộ; không tạo hay sửa Firebase cloud project.
- Chưa có cấp/thu hồi role qua UI, sửa/xóa user, xem nội dung riêng tư hay export. Google popup cần cấu hình web app thuộc project hiện có; không tự tạo project để kiểm thử.
