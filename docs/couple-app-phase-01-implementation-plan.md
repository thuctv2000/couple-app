# Kế hoạch triển khai phase 01 — Offline-first + 6 màn native

- Lập ngày 09/10/2026, dựa trên source và tài liệu hiện có.
- Nguồn lực: một người, AI hỗ trợ, 40 giờ/tuần; làm tuần tự, không giả định có đội mobile/backend/web riêng.
- Mục tiêu: beta nội bộ Android + iOS với bộ đếm offline-first, Firebase Anonymous, link Google/Apple, đồng bộ Go và web Next.js landing/admin tối thiểu.
- Ước lượng cơ sở: **8 tuần, 320 giờ**, gồm **264 giờ công việc và 56 giờ dự phòng**. Khoảng lập kế hoạch **8–10 tuần** nếu phát sinh tích hợp/provider/môi trường. Đây là ước lượng, không phải kết quả đo năng suất hay cam kết ngày ra mắt.
- Tuần 1 bắt đầu khi thực sự triển khai; thời gian chờ tài khoản, quyền hoặc store được ghi riêng. Không tự gán ngày kết thúc theo ngày viết plan.
- Kế hoạch này thay thế lịch 80 giờ của sprint 01 cho phạm vi mới. Không cộng hai lịch vào nhau. Không thay thế roadmap dài hạn của sản phẩm.

## 1. Hiện trạng đã đối chiếu

| Thành phần | Bằng chứng hiện tại | Việc còn thiếu |
|---|---|---|
| Logic chung KMP | `LoveCounterFacade` gọi domain tính ngày | Repository, database, observable state, sync |
| Android | Một màn nhập ngày/tính khi bấm; ghi rõ chưa lưu | 6 màn theo thiết kế, persistence, lifecycle, auth |
| iOS | SwiftUI gọi shared facade; báo cáo framework build qua | App build trước đây bị sandbox chặn macro; chưa có bằng chứng chạy UI |
| Go | Router chỉ có `GET /healthz` | Auth, Firestore adapter, quyền, version, idempotency, sync |
| Next.js | Landing tĩnh giới thiệu | Admin có xác thực/phân quyền và thông tin hỗ trợ |
| Thiết kế | 6 PNG và gallery trong `design/phase-01` | Tokens, trạng thái lỗi, dark mode, biến thể native |
| Kiểm chứng cũ | Báo cáo 9 JVM tests, Android/Go/Next build qua | Chưa phải kết quả kiểm thử offline/auth/sync; không chạy lại build chỉ để viết plan |
| Git | Commit trước đã push; còn research/offline-first/design local chưa commit | Gom tài liệu và thiết kế vào thay đổi dễ review trước khi triển khai |

Ảnh concept không phải acceptance pixel tuyệt đối. Dùng component native cho date picker, provider sign-in, sheet, back và bàn phím. [Mockup](../design/phase-01/README.md) · [Kết quả build trước](bootstrap-status.md)

## 2. Phạm vi phase này

### Có trong phase 01

- 6 màn: Bắt đầu, Chọn ngày, Home solo, Chỉnh thông tin, Tài khoản, Đồng bộ.
- Tên hiển thị, tên người ấy tùy chọn dạng văn bản, ngày bắt đầu, múi giờ; chưa tạo tài khoản/membership cho người ấy chỉ vì nhập tên.
- Đếm ngày từ local database; ngày đầu = 1; cập nhật khi qua nửa đêm và khi foreground, không ghi một bộ đếm tăng dần vào database.
- Cài mới offline vẫn lưu và mở lại được; không bắt tạo Firebase user trước.
- Room KMP theo feature, local guest, outbox bền vững, migration và phân vùng dữ liệu theo account.
- Firebase Anonymous khi online; link và đăng nhập Google/Apple trên cả hai nền tảng, xử lý hủy/lỗi/xung đột.
- Đồng bộ một aggregate hồ sơ/bộ đếm giữa thiết bị cùng tài khoản; Go xác minh token, kiểm quyền, chống trùng và version conflict.
- Xóa dữ liệu local và yêu cầu xóa account/cloud; không báo xóa server thành công khi còn offline.
- Landing và admin tối thiểu: đăng nhập, tra cứu metadata user/sync, xem trạng thái yêu cầu xóa; quyền server và audit.
- Trợ giúp ngắn, log không chứa token/nội dung riêng, event thiết yếu và crash reporting phù hợp cấu hình đồng ý.
- Kiểm tra chữ lớn, screen reader, dark mode, navigation/restoration, thiết bị thật và bản beta nội bộ.

### Theo phase sau, không bị bỏ khỏi sản phẩm

Pairing/unpair, kỷ niệm ảnh, upload media, nhiều ngày đặc biệt/nhắc dịp, widget, theme tùy chọn, feed/report/moderation, AI, subscription và recap. Mỗi feature đi kèm đầy đủ offline/permission/error theo tài liệu UX. Phase 01 chỉ sync bộ đếm/hồ sơ, chưa cam kết giải quyết toàn bộ media hoặc collaborative editing.

## 3. Quy tắc UI cho sáu màn

| Màn | Hành vi cần code | Trạng thái bắt buộc |
|---|---|---|
| Bắt đầu | Dùng ngay; đường riêng tới account đã có | Mở mới offline, khôi phục local, database lỗi có phục hồi |
| Chọn ngày | Date picker native, ngày đầu = 1, lưu local | Ngày không hợp lệ/tương lai, lưu thành công, lưu local thất bại |
| Home solo | Số ngày, ngày bắt đầu, chỉnh thông tin, account/status | Local-only, chờ sync, đã sync, cần xử lý; chưa cần tab bar |
| Chỉnh thông tin | Tên, ngày, timezone; ghi entity và outbox nguyên tử | Có chỉnh chưa lưu, lỗi field, hết dung lượng, xung đột sau sync |
| Tài khoản | Phân biệt local guest/Anonymous/linked; login khác link | Đang xử lý, hủy, credential đã dùng, hết phiên, logout/xóa |
| Đồng bộ | Pending count, kết quả gần nhất, retry, tiếp tục offline | Không mạng, đang gửi, auth cần xử lý, lỗi retryable, conflict |

Sheet xác nhận, conflict resolution, trợ giúp và xóa account là trạng thái/flow bổ sung, không coi sáu ảnh là toàn bộ đặc tả. Không dựng nút đăng nhập giả hoặc dùng UI loading để che việc chưa có backend.

Chỉnh trước khi code: bỏ ba chấm tiến trình dư ở chọn ngày; thống nhất font; hạ mức nổi bật card link trên Home; copy khôi phục phải nói rõ “dữ liệu đã đồng bộ”; status bar thực tế do OS quản lý. Dùng token màu/khoảng cách chung nhưng component Compose và SwiftUI riêng.

## 4. Lịch và backlog có thể thực hiện

Giờ bên dưới gồm coding, kiểm thử phù hợp, review và cập nhật tài liệu của ticket. Dự phòng để xử lý tích hợp, không phải chỗ nhét thêm feature. Mỗi tuần tối đa 40 giờ. Tất cả ticket đang **chưa làm**; tận dụng scaffold đã có, không dựng lại repo.

| Tuần | Ticket và công việc | Giờ việc | Dự phòng | Đầu ra kiểm chứng |
|---|---|---:|---:|---|
| 1 | P01-01: rà source/docs/định danh, ổn định build hai host (8h); P01-02: tokens và state spec 6 màn (12h); P01-03: spike Room trên hai nền tảng (12h) | 32 | 8 | App iOS/Android mở được; một dữ liệu Room ghi và đọc lại trên cả hai; quyết định phiên bản/migration |
| 2 | P01-04: schema local owner/counter/outbox, DAO/repository (12h); P01-05: use cases, observe/bridge/lifecycle và tính ngày tự cập nhật (12h); P01-06: test storage, kill/reopen, migration (8h) | 32 | 8 | Ghi local + outbox cùng transaction; offline mở lại không mất, không phụ thuộc Firebase |
| 3 | P01-07: Compose 6 màn và state adapters (12h); P01-08: SwiftUI 6 màn và state adapters (12h); P01-09: kiểm thử offline/navigation/accessibility, thống nhất hai host (8h) | 32 | 8 | Demo offline trên hai app; account/sync có trạng thái local-only trung thực; nút cloud chỉ hoạt động sau tích hợp |
| 4 | P01-10: Firebase dev + native Anonymous + map local owner (10h); P01-11: Go token verification/ownership/Firestore repository (14h); P01-12: API contract, version/idempotency, integration tests (10h) | 34 | 6 | Hai app có phiên dev thật; API từ chối sai token/quyền; ghi lặp không tạo trùng |
| 5 | P01-13: worker sync/outbox, ACK/pull/version (16h); P01-14: status UI và conflict giữ hai bản (10h); P01-15: fault tests, reconnect, app restart, foreground hooks (8h) | 34 | 6 | Đổi dữ liệu giữa hai thiết bị cùng account, retry an toàn; chưa dùng provider thì kiểm thử account dev kiểm soát |
| 6 | P01-16: Google link/login hai nền tảng (12h); P01-17: Apple link/login hai nền tảng (14h); P01-18: account switch, logout, credential conflict và khôi phục (8h) | 34 | 6 | Ma trận 2 provider × 2 nền tảng có kết quả; local pending data không sang nhầm account |
| 7 | P01-19: landing/help/privacy copy phản ánh tính năng thật (10h); P01-20: admin auth, RBAC server, metadata lookup/audit (14h); P01-21: log/events/crash và điều khiển môi trường dev (10h) | 34 | 6 | Next.js phục vụ landing/admin; tài khoản thường không truy cập admin; dữ liệu debug không lộ nội dung/token |
| 8 | P01-22: xóa local/cloud, status/retry và ngăn worker tái tạo data (12h); P01-23: regression hai OS, migration, CI và thiết bị thật (12h); P01-24: sửa lỗi nghiệm thu, hướng dẫn và đóng gói beta nội bộ (8h) | 32 | 8 | Đủ các cổng nghiệm thu mục 7; có bản cài/test report, không chỉ ảnh và build log |
| **Tổng** | **24 ticket** | **264** | **56** | **320 giờ = 8 tuần × 40 giờ** |

Ước lượng có độ tin cậy vừa/thấp ở Room bridge, provider và sync vì chưa có số liệu triển khai. Đánh giá lại cuối tuần 1 và 3. Nếu spike không qua trong timebox: ghi nguyên nhân, tiếp tục phần không phụ thuộc và cập nhật lịch; không gọi Android-only là hoàn thành hai nền tảng hoặc âm thầm bỏ sync.

### Dependency

- P01-01/02/03 → P01-04/05/06 → P01-07/08/09: lát cắt offline.
- Chuẩn bị tài khoản dev/provider từ tuần 1; P01-10/11/12 → P01-13/14/15: lát cắt cloud.
- P01-10 + API ổn định → P01-16/17/18: phương thức đăng nhập và khôi phục.
- P01-11 → P01-20; P01-18 + sync → P01-22; toàn bộ → P01-23/24.
- Security/ownership tests đi cùng API; tuần 8 là regression, không phải lúc đầu tiên kiểm thử quyền.

## 5. Cách chia source và trách nhiệm

| Khu vực | Phần phải thêm | Quy tắc |
|---|---|---|
| KMP `feature/lovecounter` | Model, repository port, đọc/sửa thiết lập; local/remote adapters | Domain không import Room/Firebase/UI |
| KMP auth/account | Domain session states và native identity gateway | Token ở SDK/platform boundary, không nhét vào outbox |
| Shared persistence/sync | Database wiring, transaction/outbox, coordinator tối thiểu | Rule xung đột theo feature; không xây sync framework tổng quát trước nhu cầu |
| Compose / SwiftUI | Màn, ViewModel/state, điều hướng, date picker/provider flows | Cùng use case, không viết lại logic ngày hoặc quy tắc sync |
| Go | Identity, counter/profile, sync, account deletion và admin adapters | UID từ token; kiểm quyền/version/idempotency ở server |
| Next.js | Landing + help + admin features | Không có web app cho người dùng cuối trong phase này; không thêm database nghiệp vụ khác |
| Contracts | OpenAPI theo endpoint đã triển khai, schema sync và lỗi | Đặc tả trước khi code API; chỉ đánh dấu endpoint có thật sau kiểm chứng |

Luồng ghi: UI → use case → local transaction (entity + outbox) → UI cập nhật; sync → Go → Firestore → ACK cập nhật local. Luồng đọc UI chỉ qua local repository. Server giữ thẩm quyền với dữ liệu cloud/quyền; local-first không có nghĩa tin UID hoặc timestamp người dùng gửi.

## 6. Đầu vào cần trước từng mốc

| Đầu vào | Cần trước | Nếu chưa có |
|---|---|---|
| Chạy Xcode ngoài giới hạn sandbox hiện tại, simulator/thiết bị kiểm thử | Nghiệm thu tuần 1 | Vẫn làm domain/Android; iOS gate chưa qua, ghi ảnh hưởng lịch |
| Tên định danh Android/iOS và Firebase project dev | P01-10 | Dùng placeholder để làm local; không đăng ký provider production bằng tên tạm |
| Cấu hình Google/Apple, callback và quyền tài khoản phát triển cần thiết | P01-16/17; chuẩn bị từ tuần 1 | Có thể dùng fake cho unit test nhưng không đánh dấu provider thực tế đã qua |
| Môi trường Go dev và credential server được cấp đúng cách | P01-11 và beta | Emulator phục vụ test có giới hạn; không coi emulator là production hoặc mở bypass auth |
| Danh sách admin dev được cấp quyền rõ ràng | P01-20 | Không mặc định mọi user Google/Apple thành admin |
| Thiết bị, signing/phân phối beta và người thử | P01-23/24 | Chưa tuyên bố beta sẵn sàng cài; tách thời gian chờ khỏi giờ dev |

Chưa cần chọn nhà cung cấp AI, payment hoặc dịch vụ analytics mới. Giữ Firebase theo quyết định đã có. Khi đụng triển khai, kiểm tra lại release stable và tương thích; không tự nâng toàn bộ dependency chỉ vì lập plan mới.

## 7. Cổng nghiệm thu

### M1 — Demo offline, mục tiêu cuối tuần 3

- Cài mới bật airplane mode → đặt ngày → lưu → force quit/mở lại vẫn đúng trên Android và iOS.
- Sửa ngày/múi giờ hợp lệ, qua nửa đêm hoặc foreground vẫn tính đúng; future date bị từ chối rõ ràng.
- Dữ liệu + outbox nhất quán sau restart; lỗi ghi local giữ nội dung nhập và không báo thành công giả.
- Sáu màn có bố cục native phù hợp, chữ lớn và Back; local-only rõ ràng, không có nút cloud giả thành công.
- Chỉ đóng M1 khi có kiểm thử UI thực tế hai host. JVM tests không thay thế gate này.

### M2 — Cloud sync, mục tiêu cuối tuần 5

- Tạo/khôi phục Anonymous khi online, bind local space đúng UID, không chặn bộ đếm khi token refresh lỗi.
- API từ chối token không hợp lệ và truy cập dữ liệu user khác.
- Server nhận nhưng ACK thất lạc → retry không trùng; ACK cũ không ghi đè chỉnh sửa local mới.
- Hai thiết bị sửa cùng base version → có conflict, chọn xong mới báo đồng bộ.
- Mạng chập chờn, restart và token hết hạn không mất thao tác; DB migration giữ outbox.
- Background sync không được hứa chạy đúng giờ; foreground luôn có đường catch-up.

### M3 — Beta nội bộ, mục tiêu cuối tuần 8

- Google và Apple: link, login, cancel, failure, credential conflict, reopen trên cả hai OS; ghi pass/fail/blocked theo từng ca.
- Account switch không hiển thị hoặc upload dữ liệu account cũ; chặn tự gộp dữ liệu không có xác nhận.
- Xóa local và xóa cloud phân biệt rõ; dừng pending worker, không tạo lại dữ liệu đã xóa sau reconnect.
- Admin kiểm quyền server, có audit tối thiểu; không lộ token/nhật ký/nội dung riêng trong logs.
- Build và regression phù hợp; ít nhất một thiết bị thật mỗi OS trước khi gọi đây là beta đã kiểm chứng trên thiết bị.
- Có bản phân phối, hướng dẫn chạy, giới hạn đã biết và test report. Store public release/duyệt store không nằm trong gate này.

Không có lỗi mở nghiêm trọng về mất dữ liệu, rò quyền hoặc sai account. Lỗi giao diện nhỏ có thể ghi backlog nếu không chặn thao tác/chữ lớn; không dùng điều này để bỏ gate iOS/auth/offline.

## 8. Rủi ro và điều chỉnh lịch

| Rủi ro | Phát hiện sớm | Ứng phó |
|---|---|---|
| iOS build bị môi trường chặn | Tuần 1, trước UI lớn | Kiểm chứng bằng môi trường phù hợp; không sửa bỏ state/sandbox để giả build qua |
| Room/KMP và bridge async | Spike cả hai host | Khóa bộ phiên bản tương thích, thử cancel/observe và migration |
| Auth provider cấu hình chậm | Chuẩn bị tuần 1 | Tiếp tục offline/API test; provider gate chờ được ghi riêng |
| Sync phức tạp hơn dự kiến | Một aggregate + fault injection | Tăng thời gian dự phòng/lịch, không bỏ idempotency/conflict |
| UI native tốn hơn mockup | Demo cuối mỗi tuần | Giảm trang trí, giữ hành vi/chữ lớn và chất lượng hai host |
| Admin vượt phạm vi | Chỉ metadata lookup + trạng thái xóa | Không dựng dashboard/social moderation trước feature tương ứng |

Nếu vượt 320 giờ, dùng dữ liệu ticket còn lại để lập lịch tuần 9–10. Có thể giảm animation, tranh minh họa, polish landing; không giảm offline persistence, quyền, provider đã cam kết hoặc kiểm chứng iOS để giữ ngày.

## 9. Công việc đầu tiên khi bắt đầu triển khai

1. Tạo branch triển khai riêng, bảo toàn các tài liệu/design chưa commit; nhóm thay đổi dễ review, không reset worktree.
2. Kiểm chứng build/chạy Android và iOS từ trạng thái hiện tại, xử lý gate iOS còn thiếu.
3. Spike một bản ghi Room KMP: lưu ngày + timezone, đóng/mở lại trên cả hai host.
4. Bổ sung use case/repository đọc local, sau đó đưa UI Home và chọn ngày lên cùng lát cắt.
5. Ghi kết quả thực tế/giờ đã dùng và cập nhật backlog trước khi mở rộng auth/sync.

Không cần chờ Firebase để bắt đầu các bước local trên. Trong lượt lập kế hoạch này chưa triển khai code, chưa thay dependency, chưa commit/push.

## Tài liệu liên quan

- [Quyết định offline-first](couple-app-offline-first.md)
- [Research UX và retention](couple-app-ux-retention-research.md)
- [Danh mục tính năng dài hạn](couple-app-product-operations-notes.md)
- [Kiến trúc theo feature](couple-app-clean-architecture.md)
- [Sprint 01 cũ — tham khảo lịch sử](couple-app-sprint-01.md)
