# Couple App — Checklist khởi động và sprint 01

- Cập nhật: 02/10/2026.
- Nguồn lực: một người, AI hỗ trợ, 40 giờ/tuần.
- Timebox: 2 tuần, 80 giờ gồm 66 giờ công việc và 14 giờ dự phòng tích hợp/sửa lỗi.
- Trạng thái cập nhật 05/10/2026: đã bắt đầu dựng [source local](../README.md), feature đếm ngày và hai UI native. Kết quả kiểm chứng tại [báo cáo khởi tạo](bootstrap-status.md); chưa cấu hình Firebase thật.
- Theo quyết định mới: Firebase; KMP chia sẻ logic/data; Android Compose + Navigation 3; iOS SwiftUI + NavigationStack; Go backend; Next.js landing/admin; Clean Architecture theo feature.
- Liên quan: [Kiến trúc](couple-app-clean-architecture.md), [Thư viện](couple-app-library-recommendations.md), [Lộ trình](couple-app-development-plan.md), [Danh mục tính năng](couple-app-product-operations-notes.md).

## 1. Mục tiêu demo cuối sprint

Trên Android và iOS, người dùng mở app → dùng Anonymous → đặt tên hiển thị và ngày bắt đầu yêu → xem bộ đếm → đóng/mở app → vẫn thấy đúng dữ liệu. Go xác minh phiên và lưu dữ liệu; web quản trị chỉ cho người có quyền xem metadata tài khoản tối thiểu.

Google/Apple vẫn là yêu cầu của v1. Sprint này kiểm chứng một đường link thực tế sớm, ghi kết quả và việc còn thiếu; sprint kế tiếp hoàn thiện ma trận hai provider trên hai nền tảng, xung đột và vòng đời tài khoản. Demo sprint 01 không đồng nghĩa auth đã sẵn sàng phát hành.

Đầu ra hữu hình:

- Android app và iOS app chạy được cùng luồng nghiệp vụ, mỗi app có UI native riêng.
- API Go với Firebase Auth và persistence thử nghiệm.
- Next.js có landing tối thiểu, đăng nhập admin và một màn tra cứu tài khoản theo UID.
- Hợp đồng API ban đầu, hướng dẫn chạy local, test rủi ro chính và bảng kết quả thử trên hai nền tảng.

## 2. Checklist trước khi dựng source

| Việc | Cách thực hiện | Có chặn công việc nào? |
|---|---|---|
| Tên app và định danh | Ghi tên làm việc, Android application ID, iOS bundle ID; nếu chưa có thương hiệu thì placeholder chỉ dùng local | Cần định danh cố định trước khi đăng ký Firebase/OAuth/signing thật |
| Công cụ phát triển | Kiểm tra Android Studio/JDK/Android SDK, Xcode, Go, Node/package manager; ghi phiên bản thực tế | Thiếu Xcode thì chưa thể nghiệm thu iOS |
| Bộ dependency | Đối chiếu bản stable mới nhất và tương thích; khóa version trong repo | Không đánh dấu xong chỉ vì đã ghi số phiên bản trong tài liệu |
| Firebase dev | Tách môi trường dev; Auth Anonymous, đăng ký Android/iOS và cấu hình provider khi đến bài thử | Không cần tạo production hay bật billing để hoàn thành phần local |
| Tài khoản/cấu hình Apple và Google | Kiểm tra cấu hình provider, callback, signing và quyền truy cập tài khoản phát triển | Nếu thiếu, đánh dấu bài thử provider bị chặn; tiếp tục shared/Go/Anonymous |
| Thiết bị | Android emulator và iOS simulator trước; ghi riêng kết quả trên máy thật khi có | Không gọi kiểm thử simulator là kiểm thử thiết bị thật |
| Repo | Một monorepo; mobile/backend/web build độc lập; secret ở cấu hình local/CI | Chưa cần cloud hosting để dựng demo local |

Đây là việc cần kiểm tra lúc triển khai, không phải yêu cầu người dùng gửi secret trong hội thoại. Tên app, tài khoản Firebase và signing chưa được xác nhận trong tài liệu hiện có.

## 3. Quy tắc sản phẩm dùng cho prototype

Các mặc định dưới đây là đề xuất triển khai có thể chỉnh, phân biệt với yêu cầu đã xác nhận.

| Chủ đề | Mặc định đề xuất | Tiêu chí kiểm chứng |
|---|---|---|
| Ngày đầu | Ngày bắt đầu = ngày 1 | Bắt đầu hôm nay hiển thị 1; hôm qua hiển thị 2 |
| Ngày lịch | Lưu `startDate` dạng YYYY-MM-DD và múi giờ IANA được chọn khi thiết lập | Không tính bằng milliseconds/24 giờ; test qua nửa đêm/năm nhuận |
| Múi giờ | Mặc định múi giờ thiết bị khi thiết lập, lưu lại; không tự đổi theo mỗi lần di chuyển | Đổi múi giờ thiết bị không âm thầm đổi cách đếm đã lưu |
| Ngày tương lai | Chưa hỗ trợ trong bộ đếm ngày yêu; báo lỗi rõ tại UI và API | UI/API đều từ chối, không hiển thị số âm |
| Chưa ghép đôi | Bộ đếm là dữ liệu cá nhân của user | Không tạo partner giả; chưa suy ra đã có quan hệ với tài khoản khác |
| Phiên có sẵn | Khôi phục, không tạo anonymous UID mới khi mở lại | Reopen giữ UID; không có profile trùng |
| Lần đầu offline | Cho nhập draft cục bộ, hiển thị chưa đồng bộ | Có mạng mới tạo/khôi phục phiên và lưu lên đúng tài khoản |
| Lưu thất bại | Giữ draft, có thao tác thử lại | Không giả báo đã lưu trên server |
| Chuyển tài khoản | Draft gắn với phiên/chủ sở hữu rõ ràng; không tự chuyển sang user mới | Nếu draft chưa có chủ, hỏi lựa chọn gắn dữ liệu trước khi đồng bộ vào tài khoản đã có |
| Admin | Chỉ metadata phục vụ hỗ trợ: UID, trạng thái tài khoản, thời gian tạo/cập nhật | Không hiển thị ngày yêu, nội dung riêng hoặc token trên màn admin đầu tiên |

Quy tắc ghép đôi, sửa ngày chung, ngắt ghép và quyền nội dung cần đặc tả riêng trước sprint couple. Không mở rộng quy tắc bộ đếm cá nhân thành quyền sửa dữ liệu chung.

## 4. Năm màn hình mobile đầu tiên

| Màn hình | Nội dung | Trạng thái phải thiết kế |
|---|---|---|
| Khởi động | Khôi phục phiên và dữ liệu | Đang tải, offline với dữ liệu cũ, lỗi có thể thử lại |
| Thiết lập | Tên hiển thị, ngày bắt đầu, múi giờ | Input sai, lưu draft, đang lưu, lưu thất bại |
| Trang chính | Số ngày, ngày bắt đầu, lối sửa thông tin/tài khoản | Chưa thiết lập, dữ liệu đã đồng bộ, draft chưa đồng bộ |
| Tài khoản | Trạng thái khách, lợi ích liên kết, lối đăng nhập tài khoản cũ | UI provider được hoàn thiện ở sprint auth; không phát hành nút bấm không hoạt động |
| Trạng thái link | Đang xử lý, thành công, hủy, xung đột | Prototype theo bài thử; không tự merge hoặc đăng xuất guest khi gặp lỗi |

Hai nền tảng thống nhất nội dung và hành vi; bố cục, navigation, sheet, bàn phím và back gesture theo native UI. Shared không chứa view, NavigationPath hoặc Android navigation object.

## 5. Backlog sprint 01 — 80 giờ

S01-02 và phần logic/bridge đồng bộ của S01-03 đã bắt đầu; xem báo cáo khởi tạo để phân biệt source đã viết với tiêu chí đã kiểm chứng. Các ticket còn lại chưa nghiệm thu. Thời lượng là ước lượng lập kế hoạch; không cộng thêm 80 giờ này lên lịch nền móng cũ vì đây là bản chi tiết thay thế.

Cập nhật 06/10/2026: Android APK, Go, Next.js, 9 KMP JVM tests và framework KMP iOS đã qua kiểm tra. Xcode build phase tích hợp framework đã qua; biên dịch SwiftUI bị sandbox chặn macro của Apple. Chưa chạy UI hai nền tảng, nên chưa đóng S01-02/S01-03.

| ID | Công việc | Giờ | Phụ thuộc | Nghiệm thu |
|---|---|---:|---|---|
| S01-01 | Chốt quy tắc prototype, wireframe và test cases | 4 | Không | Có các trạng thái ở mục 3–4; ghi rõ giả định còn mở |
| S01-02 | Dựng monorepo, cấu hình local, bộ phiên bản và build tối thiểu | 6 | Checklist môi trường | Android/iOS mở màn trống; Go có health; Next chạy local; không commit secret |
| S01-03 | Shared domain/data theo feature; thử cầu nối Swift | 8 | 02 | Cùng use case tính ngày chạy ở hai host; Swift nhận kết quả/lỗi và hủy tác vụ/observation đúng |
| S01-04 | API Go: xác minh Firebase, profile và bộ đếm; persistence dev | 10 | 01, 02 | Đọc/ghi theo UID đã xác minh; khởi tạo idempotent; có test truy cập sai quyền và lưu lỗi |
| S01-05 | Android Anonymous + thiết lập/home + Navigation 3 | 8 | 03, 04 | Reopen giữ phiên/dữ liệu; draft offline không mất; back stack hợp lý |
| S01-06 | iOS Anonymous + thiết lập/home + NavigationStack | 10 | 03, 04 | Cùng nghiệp vụ với Android; cập nhật UI đúng lifecycle; không nhân bản logic ngày |
| S01-07 | Bài thử link provider native và rà cấu hình hai provider | 6 | 05 hoặc 06; cấu hình provider | Thử một cặp provider/nền tảng thực tế; ghi UID trước/sau, hủy và vấn đề còn thiếu. Không đánh dấu cả ma trận hoàn thành |
| S01-08 | Next landing tối thiểu + admin session + tra cứu UID | 8 | 04 | Guest/user thường bị từ chối; admin xem metadata; Go kiểm tra quyền từng request |
| S01-09 | Test tích hợp, lint/build tự động và hướng dẫn demo | 6 | 04–08 | Test auth/ownership/ngày; chạy lại local được; bảng Android/iOS/provider có bằng chứng |
| S01-10 | Dự phòng tích hợp, sửa lỗi và demo/re-estimate | 14 | Khi phát sinh | Ưu tiên hoàn tất luồng chính; cập nhật backlog theo thời gian thực tế |
| **Tổng** | | **80** | | |

Nếu hết timebox: giảm độ bóng UI/landing, giữ admin ở một màn; bài thử provider có thể mang kết quả bị chặn sang sprint sau. Không bỏ kiểm tra quyền để đổi lấy demo đẹp. Nếu chưa hoàn tất luồng chính Android/iOS, ghi sprint chưa đạt và tính lại phần còn lại.

Lịch định hướng: tuần 1 làm 01–04 và bắt đầu Android/iOS; tuần 2 hoàn tất hai app, bài thử provider, admin và test. Với một người, làm lần lượt theo dependency, không giả định mobile/Go/web được ba người làm đồng thời.

## 6. Persistence và API đầu tiên

Đến bước lưu hồ sơ và ngày yêu, **đề xuất Firestore cho thử nghiệm đầu tiên**, đúng ưu tiên Firebase. Chưa coi đây là quyết định cuối cho mọi query mạng xã hội. Backend truy cập qua repository adapter để có thể đánh giá lại theo nhu cầu feed/search sau này.

Luồng dữ liệu: mobile → Go API → Firestore; mobile gọi Firebase Auth trực tiếp để quản lý phiên. Với collection nghiệp vụ do Go quản lý, client không được đọc/ghi trực tiếp. SDK server Firestore không chịu Security Rules như client SDK, nên Go phải kiểm tra quyền và cấu hình IAM phù hợp. [Firebase: server access và rules](https://firebase.google.com/docs/firestore/security/rules-conditions)

Hợp đồng dưới đây là bản nháp để chuyển thành OpenAPI ở S01-04, chưa phải endpoint đã tồn tại:

| API | Chủ thể | Hành vi |
|---|---|---|
| `GET /healthz` | Local/health check | Trả trạng thái tối thiểu, không lộ config |
| `PUT /v1/me/profile` | Firebase user, gồm Anonymous | Khởi tạo/cập nhật tên theo UID từ token; lặp lại không tạo user mới |
| `GET /v1/me` | Firebase user | Trả profile và trạng thái thiết lập; thiếu dữ liệu là trạng thái được định nghĩa, không ngụy trang thành lỗi server |
| `PUT /v1/me/love-counter` | Chủ tài khoản | Lưu ngày/múi giờ và version; từ chối ghi đè khi version cũ |
| `GET /v1/me/love-counter` | Chủ tài khoản | Trả thiết lập bộ đếm; client tính ngày từ use case chung |
| `POST /v1/admin/session` | Firebase user có quyền admin | Kiểm tra ID token, quyền và thời điểm đăng nhập; tạo session cookie theo thiết kế web |
| `DELETE /v1/admin/session` | Phiên admin | Đăng xuất/xóa cookie; bảo vệ CSRF theo luồng cookie |
| `GET /v1/admin/users/{uid}` | Admin được cấp quyền | Tra cứu metadata tối thiểu; không mở endpoint liệt kê mọi dữ liệu cá nhân |

Mobile dùng ID token; endpoint admin dùng Firebase session cookie và hàm xác minh tương ứng, không trộn hai loại token. Next chuyển request qua server cùng origin tới Go và chuyển cookie theo cấu hình nội bộ; Go vẫn xác minh phiên và quyền, không tin header `is-admin` do client gửi. Cookie production cần HttpOnly/Secure và thiết kế CSRF. [Firebase session cookies](https://firebase.google.com/docs/auth/admin/manage-cookies)

Mô hình dữ liệu thử nghiệm: profile theo UID, cấu hình bộ đếm theo UID, quyền admin do server cấp. Trường role/provider/UID không được client tự ghi qua API profile. Cập nhật bộ đếm kiểm tra version trong transaction; retry không tự ghi đè bản mới.

Tài khoản admin không có đường tự nâng quyền trong UI. Cấp quyền bằng công cụ quản trị riêng với tài khoản có thẩm quyền; môi trường local dùng fixture. Chưa tạo tài khoản hoặc cấp quyền thật ở bước lập kế hoạch này.

## 7. Bộ kiểm thử nghiệm thu

| Nhóm | Các ca quan trọng |
|---|---|
| Phiên | Mở lần đầu; mở lại; token hết hiệu lực; API không tạo profile trùng khi retry |
| Dữ liệu | Lưu/reopen; lưu lỗi; offline lần đầu; draft khi đổi account; cập nhật version cũ |
| Quyền | Thiếu token; token sai; user A không truy cập/sửa dữ liệu B; guest không vào admin; user thường không vào admin |
| Ngày | Ngày đầu, qua nửa đêm, năm nhuận, múi giờ, từ chối ngày tương lai |
| UI native | Android back/restoration; iOS back gesture; đóng màn khi đang load; mở lại khi có draft |
| Link provider | UID giữ nguyên khi thành công; hủy không mất phiên; lỗi mạng/xung đột có kết quả rõ; ghi phạm vi thực sự đã chạy |
| Log | Có request ID/error code; không chứa token, cookie, nội dung riêng hoặc toàn bộ request body |

Firebase Emulator Suite phục vụ nhiều ca local. Bài thử Google/Apple trên môi trường dev thực tế vẫn cần làm để kiểm chứng OAuth/config/callback; kết quả emulator không thay thế toàn bộ luồng provider thật. Không bật cấu hình chấp nhận token emulator trong production. [Auth emulator](https://firebase.google.com/docs/emulator-suite/connect_auth)

### Ma trận provider cần hoàn thiện trong sprint kế tiếp

| Provider / nền tảng | Link giữ UID | Hủy | Xung đột | Khôi phục tài khoản cũ |
|---|---|---|---|---|
| Google / Android | Chưa test | Chưa test | Chưa test | Chưa test |
| Apple / Android | Chưa test | Chưa test | Chưa test | Chưa test |
| Google / iOS | Chưa test | Chưa test | Chưa test | Chưa test |
| Apple / iOS | Chưa test | Chưa test | Chưa test | Chưa test |

Chỉ thay trạng thái khi có test thực tế; kèm build/device/môi trường. Không suy ra kết quả nền tảng này áp dụng cho nền tảng kia.

## 8. Sprint tiếp theo và giới hạn phạm vi

Sprint 02 ưu tiên Google/Apple trên cả hai nền tảng, đăng nhập tài khoản cũ, conflict UI, reauthentication/xóa tài khoản và dọn cache theo phiên. Ước lượng sau kết quả S01-07, không mặc định mọi phần này vừa một tuần.

Sau khi auth đạt yêu cầu mới làm couple/invite/permission matrix; tiếp đến kỷ niệm, ảnh, offline; social và AI theo roadmap. Các nhóm này vẫn nằm trong danh mục sản phẩm; việc chưa đưa vào sprint 01 không xóa tính năng khỏi v1.

Lịch đầy đủ trước đây 36–44 tuần dựa trên UI chung. Cuối sprint, ghi thời gian Android UI, iOS UI, bridge, shared, backend, admin và QA riêng; dùng số liệu đó ước lượng lại roadmap. Chưa đưa ra lịch mới chỉ bằng cách cộng một tỷ lệ tùy ý.

## 9. Việc bắt đầu ngay khi dựng repo

1. Kiểm tra môi trường thực tế và định danh app; tạo cấu trúc monorepo tối thiểu.
2. Dựng một use case `CalculateLoveDays` dùng chung và gọi từ Compose/SwiftUI để kiểm chứng bridge.
3. Thêm Anonymous và một API `/me` có xác thực; chạy xuyên suốt trên cả hai app.
4. Lưu bộ đếm, sau đó hoàn thiện admin và mở rộng bài thử provider.

Không tạo sẵn toàn bộ module cho 53 feature. Bắt đầu với auth, profile và love-counter; thêm module khi feature có code thực sự.
