# Couple App — Nghiên cứu và đề xuất thư viện

- Ngày đối chiếu tài liệu: 01/10/2026; cập nhật quyết định UI ngày 02/10/2026.
- Quyết định mới: UI Android/iOS riêng; KMP chỉ chia sẻ logic/data. Kotlin/KMP stable 2.4.20; Android Navigation 3 stable 1.2.0 theo tài liệu ngày 02/10. iOS dùng SwiftUI NavigationStack, API đi cùng SDK Apple. [Kotlin](https://kotlinlang.org/docs/releases.html) · [Navigation 3](https://developer.android.com/jetpack/androidx/releases/navigation3) · [NavigationStack](https://developer.apple.com/documentation/swiftui/navigationstack)
- Phạm vi: KMP Android/iOS, Go backend, Next.js landing/admin; Clean Architecture chia theo feature.
- Ràng buộc: một người, khoảng 40 giờ/tuần; Firebase Anonymous trước, liên kết Google/Apple tự nguyện; ưu tiên các dịch vụ Firebase đã chọn.
- Trạng thái cập nhật 05/10/2026: đã đưa một phần thư viện vào [source khởi đầu](../README.md), kiểm chứng KMP/JVM, Android và Next.js. Xem [báo cáo build](bootstrap-status.md); các thư viện cho auth/data/admin chưa được coi là đã tích hợp.
- Tài liệu liên quan: [Kiến trúc](couple-app-clean-architecture.md) · [Kế hoạch](couple-app-development-plan.md) · [53 tính năng và vận hành](couple-app-product-operations-notes.md).

## 1. Khuyến nghị tổng thể

Chọn thư viện theo vấn đề cần giải quyết, khả năng bảo trì và sự phù hợp Android/iOS. “Nâng cao” ở dự án này nên tạo ra khả năng kiểm thử, hợp đồng API rõ ràng, quản lý trạng thái và quan sát lỗi tốt hơn.

| Phần | Bộ thư viện đề xuất ban đầu | Bổ sung theo tính năng |
|---|---|---|
| KMP | Coroutines, Serialization, Ktor, Koin; navigation riêng ở host | Datetime cho đếm ngày; Coil cho ảnh; DataStore cho settings; Room cho dữ liệu offline |
| Firebase trên mobile | Đề xuất SDK Firebase Android/Apple chính thức qua adapter; GitLive không còn là bước bắt buộc | Analytics, Crashlytics, FCM, Remote Config, Storage theo feature |
| Go | `net/http` + chi, Firebase Admin, `log/slog`; oapi-codegen khi dựng API đầu tiên | Validator nếu cần; OpenTelemetry khi triển khai theo dõi backend |
| Next.js | shadcn/ui cho admin; openapi-typescript + openapi-fetch cho API | React Hook Form + Zod cho form; TanStack Table/Query cho màn quản trị cần chúng |
| Chất lượng | KMP: coroutines-test/Turbine, detekt; Go: testing/httptest, Testify, golangci-lint, govulncheck; web: Vitest/Testing Library, Playwright | Konsist khi có các module đầu tiên cần giữ ranh giới |

Đây là danh mục mục tiêu, không phải yêu cầu cài toàn bộ ngay ngày đầu. Giữ constructor injection trong code nghiệp vụ; Koin chỉ nối các implementation ở lớp ngoài. Go tiếp tục nối dependency thủ công.

## 2. “Được cộng đồng tin dùng” được đánh giá thế nào?

Đối chiếu nguồn của tác giả/maintainer: tài liệu hỗ trợ nền tảng, kho mã và phát hành, khả năng tích hợp framework, dấu hiệu ngừng duy trì. Không lấy số sao GitHub làm tiêu chí quyết định và không coi độ phổ biến là bằng chứng đã phù hợp app này.

| Nhóm bằng chứng | Ví dụ được kiểm tra | Ý nghĩa khi chọn |
|---|---|---|
| Hệ sinh thái chính thức | Kotlin/Ktor; AndroidX Room/DataStore; Firebase Admin | Ưu tiên khi đáp ứng nhu cầu, vẫn kiểm tra phiên bản và target |
| Dự án cộng đồng có tài liệu chuyên biệt | Koin có hướng dẫn KMP/Compose; Coil có Compose Multiplatform | Có đường tích hợp trực tiếp, ít phải tự ghép giải pháp |
| Tích hợp được framework hướng dẫn | Next.js có hướng dẫn Vitest/Playwright | Có cơ sở lựa chọn công cụ test cho App Router |
| Maintainer công bố sử dụng thực tế | README chi nêu các đơn vị sử dụng production | Tín hiệu tham khảo từ maintainer, không phải khảo sát độc lập |
| Có rủi ro bảo trì rõ ràng | Google Wire đã được archive | Loại khỏi lựa chọn cho dự án mới dù từng phổ biến |

Nguồn: [Koin KMP](https://insert-koin.io/docs/reference/koin-core/kmp-setup/), [Coil](https://coil-kt.github.io/coil/), [Next.js testing](https://nextjs.org/docs/app/guides/testing), [chi](https://github.com/go-chi/chi), [Wire](https://github.com/google/wire).

Các mức bên dưới: **Ban đầu** = đưa vào nền móng khi có phần code sử dụng; **Theo feature** = thêm khi triển khai chức năng; **Thử trước** = cần bài thử Android/iOS hoặc kiểm tra tương thích rồi mới chốt.

## 3. KMP — Android và iOS

### 3.1. Danh mục đề xuất

| Thư viện | Mức | Dùng cho app này | Vị trí và lưu ý |
|---|---|---|---|
| [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | Ban đầu | Tác vụ bất đồng bộ, Flow/StateFlow cho auth và UI state | Theo lifecycle; hủy tác vụ khi đổi phiên; không tự tạo global scope khắp feature |
| [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) | Ban đầu | Chuyển JSON API thành DTO; serialization cần thiết cho navigation | DTO ở data; không biến domain entity thành model Firebase/HTTP |
| [Ktor Client](https://ktor.io/docs/client-supported-platforms.html) | Ban đầu | Gọi API Go, xác thực, timeout, ánh xạ lỗi | Client chung tại `core/network`; Android dùng OkHttp engine, iOS dùng Darwin engine |
| [Koin](https://insert-koin.io/docs/reference/koin-compose/compose/) | Ban đầu | Nối repository, use case, ViewModel theo từng feature | Module DI ở `wiring`; domain không import Koin; khởi đầu bằng DSL, chưa cần compiler plugin |
| [Navigation 3 Android](https://developer.android.com/jetpack/androidx/releases/navigation3) | Ban đầu | Điều hướng Compose Android; deep link lời mời | Bản stable 1.2.0 theo ngày 02/10; đặt tại Android host; kiểm thử restoration/predictive back |
| [SwiftUI NavigationStack](https://developer.apple.com/documentation/swiftui/navigationstack) | Ban đầu | Điều hướng iOS native | Đi cùng SDK Apple, không có phiên bản package riêng; kiểm thử back gesture, route/path và callback auth |
| [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime) | Theo feature: đếm ngày | Ngày bắt đầu yêu, ngày kỷ niệm, quy tắc múi giờ | Dùng `LocalDate` cho ngày lịch; chọn quy tắc tính ngày đầu là 0 hay 1 trong đặc tả sản phẩm |
| [Coil 3](https://coil-kt.github.io/coil/) | Theo feature: ảnh | Avatar, ảnh kỷ niệm, thumbnail feed, cache ảnh | Dùng Coil ở UI Android; không dùng Coil Compose cho SwiftUI. Chọn giải pháp ảnh iOS khi làm media; cache phải xử lý đổi tài khoản/mất quyền |
| [DataStore KMP](https://developer.android.com/kotlin/multiplatform/datastore) | Theo feature: settings | Theme, trạng thái hướng dẫn, tùy chọn hiển thị cục bộ | Không dùng như kho bí mật để tự lưu token; quyền thông báo hệ điều hành vẫn là nguồn xác thực |
| [Room KMP](https://developer.android.com/kotlin/multiplatform/room) | Theo feature: offline | Draft kỷ niệm, dữ liệu cần xem offline, hàng đợi thay đổi nếu được thiết kế | Chỉ ở data/local; có schema/migration; không tự giải quyết sync và xung đột với server |
| [Kermit](https://kermit.touchlab.co/) | Theo feature: logging chung | Ghi log dùng chung, writer riêng cho Android/iOS | Một đầu mối logging, lọc dữ liệu nhạy cảm; không thay Analytics hoặc Crashlytics |
| [GitLive Firebase Kotlin SDK](https://github.com/GitLiveApp/firebase-kotlin-sdk) | Phương án tham khảo, chưa chọn | Wrapper Firebase giúp gọi nhiều API từ common code | SDK cộng đồng; giữ sau adapter để có thể thay bằng tích hợp SDK native |

Ktor hỗ trợ engine theo nền tảng; Darwin sử dụng cơ chế networking của Apple. Khi triển khai, đặt timeout, che Authorization/cookie trong log, và chỉ tự retry thao tác an toàn hoặc có idempotency key. [Ktor engines](https://ktor.io/docs/client-engines.html)

Đếm ngày phải dựa trên ngày lịch và múi giờ đã chọn, không lấy số mili giây chia cho 86.400.000. Với bộ Kotlin hiện tại, kiểm tra hướng dẫn chuyển đổi giữa `kotlinx.datetime` và `kotlin.time.Instant` trước khi chép code ví dụ. Đây là yêu cầu thiết kế của app, không phải hành vi được thư viện tự quyết định. [Datetime](https://github.com/Kotlin/kotlinx-datetime)

### 3.2. Điều kiện chọn Koin, Navigation và Room

**Koin:** đề xuất bổ sung cho kế hoạch constructor/factory trước đó để giảm mã nối dependency khi số feature tăng. Constructor vẫn nhận dependency tường minh. Không gọi `get()` hoặc truy cập container bên trong domain/use case. Với demo nhỏ, factory thủ công vẫn hợp lệ. [Koin KMP](https://insert-koin.io/docs/reference/koin-core/kmp-setup/)

**Navigation:** chuyển sang hai triển khai native theo quyết định 02/10: Android dùng Navigation 3; iOS dùng NavigationStack. Không áp dụng Navigation 3 Compose Multiplatform vào SwiftUI. Hai host dùng cùng quy ước deep link/ID nhưng giữ back stack và UI state riêng. Bản stable Android được chọn là 1.2.0; bản alpha không phải mặc định. [Android releases](https://developer.android.com/jetpack/androidx/releases/navigation3) · [Apple](https://developer.apple.com/documentation/swiftui/navigationstack)

**Room:** ưu tiên nhánh stable hỗ trợ KMP đã kiểm chứng. Tại thời điểm đọc, trang hướng dẫn KMP có ví dụ Room 3 alpha, trong khi trang phát hành còn có nhánh stable Room 2.8.x. Không sao chép dependency alpha từ ví dụ vào production theo thói quen. Room chỉ là lưu trữ local; nếu chọn Firestore sau này phải xác định rõ nguồn dữ liệu và tránh hai cache cùng điều khiển UI. [Room KMP](https://developer.android.com/kotlin/multiplatform/room) · [Room releases](https://developer.android.com/jetpack/androidx/releases/room)

### 3.3. Firebase native: thử đúng luồng sản phẩm; GitLive là phương án phụ

Với hai UI native, đề xuất Firebase Android SDK trong adapter Android và Firebase Apple SDK trong adapter Swift; shared nhận contract/facade, không nhận SDK types. Xác nhận của người dùng là dùng Firebase; lựa chọn SDK native ở đây là đề xuất triển khai. [Firebase Android](https://firebase.google.com/docs/android/setup) · [Firebase Apple](https://firebase.google.com/docs/ios/setup)

GitLive là wrapper cộng đồng để tham khảo nếu cầu nối native phát sinh nhiều mã lặp, không phải bước thử bắt buộc hoặc SDK KMP chính thức của Google. [Kho GitLive](https://github.com/GitLiveApp/firebase-kotlin-sdk)

Tiêu chí nghiệm thu trên cả Android và iOS:

1. Tạo anonymous và khôi phục phiên sau khi mở lại app.
2. Liên kết Google/Apple thành công giữ nguyên UID và dữ liệu.
3. Người dùng hủy hoặc mất mạng không làm mất anonymous session.
4. Credential đã thuộc tài khoản khác đi vào luồng xung đột; không tự merge/xóa dữ liệu.
5. Làm mới ID token và gọi API Go thành công; xử lý phiên hết hiệu lực.
6. Reauthentication/xóa tài khoản hoạt động theo đặc tả; cache local được xử lý đúng khi chuyển account.
7. Build Android/iOS với bộ phiên bản Firebase native đã khóa.

Nếu bridge Swift/Kotlin không đạt, điều chỉnh contract/adapter trước khi triển khai các feature tiếp theo. Các feature gọi cùng interface nghiệp vụ. Analytics, Crashlytics, FCM và Storage cũng được kiểm tra riêng; không suy ra auth hoạt động thì mọi dịch vụ đều hoạt động.

### 3.4. Công cụ kiểm thử và giữ kiến trúc

| Công cụ | Đề xuất sử dụng |
|---|---|
| [kotlinx-coroutines-test](https://github.com/Kotlin/kotlinx.coroutines) | Điều khiển dispatcher/thời gian trong test; kiểm tra hủy tác vụ và trạng thái khi mạng lỗi |
| [Turbine](https://github.com/cashapp/turbine) | Kiểm tra thứ tự Flow emission: guest → linking → linked; lỗi link không làm mất trạng thái trước |
| [detekt](https://detekt.dev/) | Static analysis Kotlin; chọn rule có ích, kiểm tra tương thích Kotlin compiler |
| [Konsist](https://github.com/LemonAppDev/konsist) | Kiểm tra quy ước/import của source Kotlin bằng test chạy JVM; không phải dependency runtime KMP |

Konsist hỗ trợ kiểm tra source, không thay thế hoàn toàn compiler hoặc kiểm tra dependency graph. Khi một feature chung chứa domain/data trong một Gradle module, cần rule cấm domain import data, Firebase, Ktor và Compose; chỉ có thư mục riêng chưa đủ bảo vệ ranh giới.

## 4. Backend Go

| Thư viện/công cụ | Mức | Dùng cho app này | Giới hạn áp dụng |
|---|---|---|---|
| [chi](https://github.com/go-chi/chi) | Ban đầu | Nhóm route theo auth/couple/memory/admin, middleware dùng chung | Dựa trên `net/http`; ở adapter HTTP/bootstrap; không đưa router/context riêng vào domain |
| [Firebase Admin SDK](https://firebase.google.com/docs/admin/setup) | Ban đầu | Xác minh danh tính và thao tác Firebase phía server | SDK nằm trong adapter; kiểm tra quyền thành viên/cặp đôi/admin trong nghiệp vụ |
| [oapi-codegen](https://github.com/oapi-codegen/oapi-codegen) | Ban đầu khi có API | Sinh Go DTO và interface HTTP từ hợp đồng OpenAPI | Sinh vào thư mục adapter/generated; không sinh domain entity; runtime validation và authorization vẫn phải triển khai |
| [log/slog](https://pkg.go.dev/log/slog) | Ban đầu | Structured log: request ID, route, mã lỗi, thời gian xử lý | Thư viện chuẩn Go; đủ cho giai đoạn đầu, chưa cần thêm Zap/Zerolog |
| [go-playground/validator](https://github.com/go-playground/validator) | Theo nhu cầu | Kiểm tra định dạng request như giới hạn độ dài | Có thể bỏ nếu validation OpenAPI đã đủ; không thay quy tắc nghiệp vụ hoặc kiểm tra quyền |
| [Testify](https://github.com/stretchr/testify) | Khi viết test | Assertion rõ ràng bên cạnh `testing` và `httptest` | Dùng fake nhỏ tại port khi cần, không mock mọi struct |
| [golangci-lint](https://golangci-lint.run/) | CI ban đầu | Lint Go tập trung, chọn nhóm rule thống nhất | Công cụ phát triển, không phải dependency runtime; không bật mọi rule mặc định |
| [govulncheck](https://go.dev/doc/security/vuln/) | CI ban đầu | Đối chiếu lỗ hổng đã biết với mã và dependency Go | Không thay thế review auth/ACL và kiểm thử nghiệp vụ |
| [OpenTelemetry Go](https://opentelemetry.io/docs/languages/go/) | Theo feature vận hành | Trace HTTP → service → external call; metrics latency/error | Chọn exporter khi chốt môi trường triển khai; chưa bổ sung nhà cung cấp quan sát mới |

**Chọn chi cho dự án này:** grouping và middleware theo feature mang lại lợi ích khi cùng API phục vụ mobile và admin. Đây là điều chỉnh có lý do từ phương án `net/http` thuần trước đó; nghiệp vụ vẫn độc lập framework. Không cần chuyển sang Gin/Fiber chỉ để tăng số tính năng framework. [chi](https://github.com/go-chi/chi)

**Không chọn Wire:** repository của Google đã archive ngày 25/08/2025. Tiếp tục dùng constructor injection thủ công ở `internal/bootstrap`; dependency graph của modular monolith giai đoạn này chưa cần container. [Wire repository](https://github.com/google/wire)

**Hợp đồng API:** dùng release oapi-codegen được khóa phiên bản và tập con OpenAPI mà release đó thực sự hỗ trợ. README nhánh phát triển có thể mô tả tính năng chưa phát hành; không suy luận mọi version đều hỗ trợ như nhau. DTO được sinh phải map vào domain model. [oapi-codegen](https://github.com/oapi-codegen/oapi-codegen)

Chưa chọn ORM/SQL driver, Redis hoặc job queue. Quyết định chúng khi triển khai persistence, tải nền hoặc retry bền vững; không mặc định PostgreSQL khi phương án Firebase/Firestore còn đang được đánh giá.

## 5. Next.js — landing page và admin

| Thư viện | Mức | Giá trị thực tế | Cách dùng đề xuất |
|---|---|---|---|
| [shadcn/ui](https://ui.shadcn.com/docs) | Khi dựng admin | Form controls, dialog, menu, bảng và bố cục quản trị đồng nhất | Component source nằm trong project; cần tự quản lý cập nhật và kiểm tra accessibility sau tùy biến |
| [React Hook Form](https://github.com/react-hook-form/react-hook-form) + [Zod](https://zod.dev/) | Theo feature: form | Form cấu hình, lý do xử lý report, kiểm tra dữ liệu nhập | Với form nhiều field; validation client giúp UX, backend vẫn kiểm tra lại |
| [openapi-typescript + openapi-fetch](https://openapi-ts.dev/openapi-fetch/) | Khi gọi API Go | Types và client theo cùng OpenAPI dùng ở Go | Ở `lib/api` hoặc adapter feature; type-safe lúc compile không đồng nghĩa kiểm tra response lúc runtime |
| [TanStack Table](https://github.com/TanStack/table) | Theo feature: danh sách quản trị | Sort/filter/pagination cho reports, users, audit records | Headless table; backend phân trang và kiểm tra quyền, không tải toàn bộ dữ liệu riêng tư xuống browser |
| [TanStack Query](https://github.com/TanStack/query) | Theo feature: tương tác động | Mutation, cập nhật sau xử lý report, polling hoặc cache client có nhu cầu rõ | Trang đọc dữ liệu đơn giản dùng Server Components/fetch; tránh hai cache cùng quyết định một dữ liệu |
| [Vitest + React Testing Library](https://nextjs.org/docs/app/guides/testing/vitest) | Khi viết test | Hàm thuần, validation và tương tác component đồng bộ | Async Server Components cần đường kiểm thử khác theo hướng dẫn Next.js |
| [Playwright](https://nextjs.org/docs/app/guides/testing/playwright) | Khi có admin auth | End-to-end đăng nhập, từ chối quyền, xử lý report, session hết hạn | Chạy với app/backend test; ưu tiên luồng rủi ro thay vì chụp mọi giao diện |

Với admin, khởi đầu bằng Next.js Server Components và adapter gọi Go phía server. Firebase Admin/credential chỉ ở mã server; nếu sau này browser gọi trực tiếp API thì thiết kế riêng cách cấp token, CORS và quyền. Không coi việc ẩn nút hoặc route group admin là kiểm soát quyền.

TanStack Query được thêm khi màn hình thực sự cần cache/mutation phía client. Tách query key theo phiên và phạm vi dữ liệu, xóa cache khi logout, và vẫn kiểm tra quyền trên từng request. Không tự đưa Redux/Zustand vào toàn bộ admin khi local state và server state đã đủ.

Không bổ sung một hệ auth thứ hai, ORM hoặc cơ sở dữ liệu nghiệp vụ trong Next.js. Firebase phụ trách danh tính; Go phụ trách quy tắc, quyền và dữ liệu nghiệp vụ. Landing page cũng không cần đầy đủ repository/use case chỉ để render nội dung tĩnh.

## 6. Vị trí thư viện trong Clean Architecture

Các đường dẫn dưới đây là vị trí dự kiến trong repository khi triển khai, chưa phải source đã tạo.

```text
mobile/
  core/network/                 # Ktor, config engine/token/error mapping
  core/observability/           # Kermit + adapter analytics/crash khi cần
  androidApp/                   # Compose, Android ViewModel, Navigation 3 theo feature
  iosApp/                       # SwiftUI, Swift ViewModel, NavigationStack theo feature
  shared/                       # composition và facade Swift, không chứa UI/navigation
  feature/auth/
    domain/                     # AuthRepository, quy tắc/trạng thái thuần
    data/                       # port/adapter Firebase native và API
    wiring/                     # Koin module
  feature/memory/data/local/    # Room khi làm offline

backend/
  internal/bootstrap/          # nối constructor và chi routes
  internal/auth/adapter/       # Firebase Admin auth adapter
  internal/<feature>/adapter/httpapi/
  internal/platform/httpapi/generated/  # Go DTO/interfaces từ OpenAPI
  internal/platform/observability/      # slog, OTel khi cần

web/src/
  app/                         # route/layout/page mỏng
  components/ui/               # component shadcn
  lib/api/generated/           # OpenAPI types
  features/<feature>/
    data/                      # openapi-fetch adapter
    presentation/              # form/table/query hook nếu cần
    server.ts                  # exports server, bảo vệ server-only
    client.ts                  # exports client riêng

contracts/openapi.yaml         # hợp đồng giữa Go, web và mobile
```

Chỉ tạo module/folder dùng chung khi đã có nhu cầu. Mã sinh từ OpenAPI không import ngược feature domain; adapter viết tay chuyển DTO thành giá trị nghiệp vụ. Mobile ban đầu có thể dùng Ktor với DTO viết tay và contract test; chỉ chọn generator Kotlin sau khi thử output KMP Android/iOS, không giả định mọi generator Kotlin đều phù hợp.

## 7. Log, analytics và quyền riêng tư

| Nhu cầu | Lựa chọn | Quy tắc dành cho app |
|---|---|---|
| Log khi debug mobile | Kermit hoặc logger native sau một interface | Không ghi token, ảnh, nội dung tâm sự hoặc lời nhắc AI |
| Lỗi/crash mobile | Firebase Crashlytics đã nằm trong định hướng | Chỉ gửi thông tin kỹ thuật cần thiết; kiểm tra báo lỗi native và shared code |
| Hành vi người dùng | Firebase Analytics đã nằm trong định hướng | Event có schema; ví dụ `account_link_succeeded`, không gửi credential/nội dung kỷ niệm |
| Log backend | `slog` JSON và request ID | Lọc dữ liệu nhạy cảm tại đầu ghi log, không phụ thuộc dev nhớ che từng lần |
| Trace/metrics backend | OpenTelemetry khi chốt hosting | Đo latency/error theo route; không dùng user ID làm nhãn metric gây quá nhiều chuỗi |
| Nhật ký quản trị | Audit record nghiệp vụ do Go ghi | Phân biệt với debug log; có actor, action, target, kết quả và chính sách truy cập |

Kermit là thư viện logging; OpenTelemetry là công cụ instrumentation. Chúng không tự cung cấp toàn bộ nơi lưu trữ, dashboard hoặc chính sách bảo mật. Việc chọn exporter/backend quan sát được để lại khi thực sự triển khai vận hành. [Kermit](https://kermit.touchlab.co/) · [OpenTelemetry Go](https://opentelemetry.io/docs/languages/go/)

## 8. Thứ tự đưa vào dự án và ước lượng công việc

Các con số là ước lượng kỹ thuật cho một người, không phải benchmark. Công việc dưới đây thuộc giai đoạn nền móng trong kế hoạch hiện có; không cộng cơ học toàn bộ vào lịch cũ 36–44 tuần; lịch đó cũng cần ước lượng lại cho UI riêng.

| Đợt | Công việc | Kết quả phải có | Ước lượng |
|---|---|---|---|
| A | Khóa bộ phiên bản KMP + Android Compose + Xcode; thử Firebase native, bridge Swift và hai navigation | Android/iOS chạy anonymous và link; ghi nhận UID/xung đột; có đường fallback | 16–28 giờ |
| B | Ktor/Koin cho feature auth; chi/Firebase Admin/slog; một hợp đồng API | Một luồng app → Go có xác thực, test lỗi chính, không lộ token trong log | 12–20 giờ |
| C | Dựng nền admin; OpenAPI client; một trang có quyền; test/lint tối thiểu | Tài khoản không có quyền bị từ chối; client/server code tách rõ | 12–20 giờ |

Ước lượng cũ khoảng 40–68 giờ cho nền móng được lập trước khi chốt hai UI riêng. Cần đo lại bridge Swift và hai UI auth/home trước khi xác nhận thời gian; các giờ ở bảng là baseline tham khảo, chưa gồm toàn bộ UX auth và mọi ca production. Nếu iOS/auth làm phát sinh lỗi tích hợp, cập nhật ước lượng thay vì đánh dấu xong chỉ vì Android đã chạy.

Ngày 1–2 nên chốt danh mục thư viện và bắt đầu bài thử rủi ro cao; không cam kết hoàn thành mọi tích hợp trong hai ngày. Sau đó thêm Datetime khi làm đếm ngày, Coil/Room khi làm kỷ niệm, Table/Query khi có màn admin phù hợp, và OTel khi triển khai backend.

## 9. Điều kiện nhận một dependency vào source

1. Có feature hoặc bài kiểm chứng đang cần thư viện đó và biết nó thuộc layer nào.
2. Chọn release ổn định, đọc migration guide, kiểm tra license và yêu cầu nền tảng của đúng release.
3. KMP: xác nhận Kotlin/Compose/AGP/KSP/Xcode/Firebase iOS tương thích; build cả Android và iOS. Không trộn phiên bản mới nhất của từng thư viện theo cảm tính.
4. Go/web: kiểm tra phiên bản Go, Node/React/Next và code generator; khóa version trong các file quản lý dependency tương ứng.
5. Test luồng quan trọng sau thêm thư viện; kiểm tra generated diff và ranh giới import trong CI.
6. Có đường thay thế qua adapter nếu thư viện cộng đồng ngừng duy trì, đặc biệt GitLive.

Phần nghiên cứu ngày 01–02/10 không bao gồm build hoặc benchmark. Từ 05/10 đã có version catalog, lockfile web và kiểm chứng một phần trên source; xem báo cáo build để biết phạm vi thực sự đã đạt. Chưa audit toàn bộ dependency chuyển tiếp. Không bổ sung dịch vụ SaaS/AI framework mới trước khi triển khai chức năng cần đến chúng.
