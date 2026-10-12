# Quyết định: Offline-first

- Chốt theo yêu cầu người dùng ngày 07/10/2026.
- Áp dụng Android/iOS. Next.js landing/admin và các thao tác cần server không có cam kết hoạt động offline đầy đủ.
- Trạng thái: quyết định kiến trúc/UX, **chưa triển khai persistence hoặc sync trong source**.
- Tài liệu này thay thế các mô tả cũ chỉ cho phép draft khi offline và để offline tới giai đoạn làm kỷ niệm.

## 1. Hành vi sản phẩm

Lưu trên máy trước, đọc từ dữ liệu trên máy, đồng bộ khi có thể. Mạng hoặc Firebase không phải điều kiện để hoàn thành công việc cá nhân cốt lõi.

| Hoạt động | Khi offline | Khi có mạng |
|---|---|---|
| Lần mở đầu | Tạo không gian local, đặt ngày/biệt danh, lưu thật và dùng ngay | Khôi phục hoặc tạo Firebase Anonymous rồi đồng bộ |
| Bộ đếm/ngày đặc biệt | Đọc/sửa từ database, tính ngày local | Đồng bộ thay đổi; xử lý xung đột nếu có |
| Kỷ niệm cá nhân | Tạo/sửa/xóa, đọc ảnh đã có trên máy | Đẩy thay đổi và tải phần dữ liệu chưa có |
| Kỷ niệm chung đã tải | Xem bản đã lưu; thay đổi được đánh dấu chờ kiểm quyền khi gửi | Server kiểm quyền hiện tại rồi nhận hoặc từ chối |
| Ảnh mới | Sao chép vào vùng lưu trữ app; giữ file gốc phục vụ upload | Upload có retry; chỉ báo ảnh đã đồng bộ sau xác nhận |
| Nhắc dịp | Nhắc local đã lập lịch nếu hệ điều hành/quyền cho phép | Cập nhật lịch; đồng bộ tùy chọn, tránh nhắc trùng |
| Ghép đôi/link Google/Apple | Xem thông tin đã biết, giải thích cần kết nối để hoàn tất | Xác thực và xác nhận kết quả từ server |
| Feed/phản hồi | Đọc nội dung đã tải hoặc soạn draft; không báo đã gửi | Làm mới/publish sau kiểm quyền và trạng thái hiện tại |
| AI cloud | Xem kết quả đã lưu, soạn yêu cầu; không tự gửi nội dung nhạy cảm khi nối mạng lại | Người dùng chủ động chạy yêu cầu |
| Unpair/xóa account cloud | Có thể ẩn nội dung/tắt nhắc trên máy ngay; chưa khẳng định đã thu hồi/xóa ở server | Gửi yêu cầu, hiện trạng thái và xác nhận từ server |

Offline không có nghĩa toàn bộ ảnh hoặc nội dung server đã nằm trên máy. Có chính sách tải trước, dung lượng cache, mục “Lưu trên thiết bị” khi cần; file còn chờ upload không được dọn như cache.

## 2. Luồng dữ liệu đã chọn

```text
Compose / SwiftUI
       ↓ lệnh ghi / quan sát dữ liệu
Use cases + repository ports (KMP domain)
       ↓
Repository adapter theo feature
       ↓ transaction
Local database: dữ liệu + outbox
       ↕ sync coordinator
Go API: Firebase token + quyền + version + idempotency
       ↕
Firestore qua server adapter
```

Local database là nguồn đọc cho UI. Server là nơi quyết định quyền truy cập và phiên bản đã chấp nhận của dữ liệu chia sẻ. Cập nhật từ mạng phải đi vào database trước khi UI thấy; không để response HTTP và local cache cạnh tranh điều khiển cùng một màn.

Đây là hướng phù hợp với hướng dẫn offline-first của Android về local source, repository và hàng đợi ghi. Chi tiết protocol bên dưới là thiết kế đề xuất của dự án. [Android offline-first](https://developer.android.com/topic/architecture/data-layer/offline-first)

Giữ lựa chọn **Room KMP** đã có trong danh mục thư viện, đưa vào bước persistence đầu tiên thay vì đợi feature kỷ niệm. Khóa phiên bản sau bài thử tương thích Kotlin/Android/iOS và migration; chưa thêm dependency chỉ bằng quyết định tài liệu. Settings nhỏ có thể dùng DataStore, không dùng settings store thay database/outbox.

Firebase vẫn phục vụ Auth và backend storage theo kế hoạch. Firestore offline persistence áp dụng khi app dùng client SDK truy cập Firestore; nó không tự tạo hàng đợi cho request mobile → Go API. Vì vậy không dựa vào cache Firestore để thực hiện kiến trúc này. [Firestore offline](https://firebase.google.com/docs/firestore/manage-data/enable-offline)

## 3. Local guest và Firebase Anonymous là hai trạng thái khác nhau

- **Local guest:** một không gian dữ liệu trên thiết bị, có local owner ID bền vững; chưa phải Firebase user và không có quyền gọi API đã xác thực.
- **Anonymous:** Firebase đã tạo user và SDK giữ phiên. Việc tạo mới cần kết nối dịch vụ; khi offline vẫn mở dữ liệu local mà không chờ token refresh.
- **Linked:** user đã thêm phương thức đăng nhập. Link thành công thông thường giữ UID; đăng nhập tài khoản khác không tương đương link.

Lần đầu offline: tạo local space → lưu thiết lập/kỷ niệm → mở lại vẫn còn. Khi online: khôi phục phiên trước; nếu không có phiên mới tạo Anonymous, ràng buộc local space với UID và đồng bộ theo thao tác idempotent.

Không tạo Firebase UID giả, không gửi local owner ID như bằng chứng danh tính. Nếu người dùng đăng nhập account cũ hoặc có local space thuộc UID khác, tách dữ liệu và cho xem trước việc nhập/chuyển dữ liệu được phép; không tự gộp. Các mục chờ gửi phải gắn với đúng tài khoản và không chạy sang tài khoản vừa đăng nhập.

Cloud sync thất bại không làm mất dữ liệu local. Thiết bị hỏng, mất app data hoặc gỡ app vẫn có thể làm mất phần chưa đồng bộ; link account không đồng nghĩa mọi dữ liệu đã được backup. [Firebase Anonymous/link](https://firebase.google.com/docs/auth/android/anonymous-auth)

## 4. Hợp đồng sync tối thiểu đề xuất

1. Ghi entity và outbox trong cùng transaction. Chỉ hiện “Đã lưu trên máy” sau khi transaction thành công; hết dung lượng là lỗi lưu thật.
2. Mỗi operation có ID ổn định, entity ID, local owner/account scope, base server version, loại thao tác, payload version và trạng thái. Không lưu ID token trong outbox.
3. Khi gửi, lấy token hợp lệ; server suy UID từ token và kiểm quyền hiện tại. Idempotency key được scope theo UID; cùng key khác payload phải bị từ chối.
4. Go ghi thay đổi và kết quả idempotency nguyên tử trong phạm vi cần thiết. Retry cùng operation trả lại kết quả cũ, không tạo bản ghi/notification trùng. Nếu side effect gửi push cần retry thì dùng cơ chế bền vững riêng ở server.
5. ACK trả server version. Local ghi ACK và trạng thái outbox nguyên tử; ACK cũ không được xóa mất bản sửa mới hơn đang chờ gửi.
6. Pull theo cursor server, có phân trang và bản ghi xóa. Cursor và phần dữ liệu đã áp dụng cập nhật cùng transaction; không lấy giờ trên điện thoại làm nguồn xác định thứ tự.
7. Retry backoff có jitter; mạng lỗi/5xx retry, 401 thử refresh có giới hạn, 403 dừng và giải thích mất quyền, 409 mở xử lý xung đột, lỗi validation không retry vô hạn.
8. Push chỉ là tín hiệu có thể làm mới; reconnect/foreground vẫn pull để không phụ thuộc việc notification có tới hay không.

Ưu tiên thử một entity “thiết lập bộ đếm” trước. Hợp đồng endpoint sync chính thức và giới hạn lưu idempotency/cursor phải được chốt khi code; chưa sửa OpenAPI để giả rằng endpoint đã tồn tại. Cursor hết hạn cần full resync có giữ nguyên thay đổi local chưa gửi.

### Chính sách xung đột

| Loại | Chính sách khởi đầu |
|---|---|
| Tạo kỷ niệm độc lập | ID riêng; retry giữ cùng ID để tránh trùng |
| Cùng sửa một trường/ngày yêu | Base version khác thì báo conflict, giữ bản local và server cho người dùng chọn |
| Sửa trường không giao nhau | Ban đầu vẫn ưu tiên báo conflict; chỉ merge tự động khi đã có rule/test cấp field |
| Xóa so với sửa cũ | Tombstone/version ngăn bản offline cũ làm sống lại nội dung đã xóa; có thể lưu thành mục cá nhân mới qua thao tác rõ ràng |
| Membership/quyền/unpair | Server quyết định; dữ liệu offline không được tự cấp lại quyền |

Không dùng last-write-wins dựa trên giờ thiết bị cho mọi dữ liệu; đồng hồ lệch và sửa trên hai thiết bị có thể gây mất nội dung.

## 5. Lịch chạy và trải nghiệm

- Sync khi app vào foreground, có kết nối lại, sau thao tác ghi, hoặc người dùng chọn “Thử đồng bộ”. Khả năng kết nối chỉ là tín hiệu thử, không đảm bảo request thành công.
- Dùng scheduler phù hợp mỗi nền tảng khi triển khai; background là cơ hội do hệ điều hành cấp, không cam kết chạy đúng giờ hoặc ngay sau khi người dùng đóng app.
- Shared KMP giữ quy tắc và trạng thái sync; host Android/iOS giữ lifecycle và scheduler. Không giữ UI loading cho tới khi cloud xong.
- Ảnh upload lớn có tùy chọn chỉ Wi-Fi; dữ liệu chữ nhỏ được đồng bộ theo lựa chọn người dùng.
- Trạng thái người dùng thấy: **Đã lưu trên máy**, **Đang chờ đồng bộ**, **Đang đồng bộ**, **Đã đồng bộ**, **Cần xử lý**. Dùng “Đã đồng bộ” ở mức từng mục hoặc snapshot rõ ràng.
- Hướng dẫn cụ thể: “Ảnh đã lưu trên máy. Sẽ tải lên khi có mạng”; không báo chung chung “Có lỗi” khi dữ liệu vẫn an toàn local.
- Giữ riêng `saved_local` và `sync_confirmed` trong analytics. Activation local thành công không cần chờ server; cloud protection là một chỉ số khác.

## 6. Riêng tư và vòng đời

Offline không cho phép thu hồi ngay bản nội dung đã tải trên thiết bị đang ngắt mạng. Khi phát hiện mất quyền/unpair, dừng các operation thuộc shared scope và xóa/ẩn cache theo chính sách. Không hứa xóa được bản người kia đã xuất hoặc chụp màn hình.

Ràng buộc sync với tài khoản và membership hiện tại. Khi logout/chuyển account, dừng worker cũ, phân vùng local data và không hiện nội dung account trước. Không tự upload các chỉnh sửa bị server từ chối sang người dùng/cặp đôi mới. Xử lý dữ liệu chung bị từ chối theo quyền sở hữu và lựa chọn rõ ràng.

Local data dùng vùng riêng của app và cơ chế bảo vệ của nền tảng; không tự nhận là mã hóa đầu-cuối. Chính sách backup hệ điều hành và bảo vệ ảnh/database phải được kiểm tra khi triển khai.

## 7. Nghiệm thu bắt buộc

1. Cài mới, bật airplane mode: nhập ngày, lưu, đóng/mở lại vẫn đúng; không cần Firebase user.
2. Mất mạng giữa lúc ghi: dữ liệu local và outbox nhất quán; crash/restart không mất thao tác đã báo lưu.
3. Server đã nhận nhưng ACK thất lạc: retry không tạo bản ghi hoặc side effect trùng.
4. Hai thiết bị sửa cùng phiên bản: không âm thầm mất một bản; resolve xong mới đánh dấu đồng bộ.
5. ACK/pull cũ đến sau bản sửa mới: giữ phần thay đổi còn chờ gửi.
6. Token hết hạn, đổi account, credential conflict: không tải dữ liệu sang sai UID.
7. Unpair trên thiết bị khác khi offline: reconnect từ chối thao tác mất quyền và thu hồi cache đúng chính sách.
8. Xóa rồi reconnect thiết bị cũ: nội dung không sống lại; full resync không xóa phần chưa gửi hợp lệ.
9. Ảnh đang upload, máy hết dung lượng hoặc app đóng: trạng thái đúng, không mất file chờ gửi.
10. Nâng version database: migration giữ data/outbox; không dùng destructive migration cho dữ liệu người dùng.

## 8. Thay đổi thứ tự triển khai

**Local database + repository → lưu/mở lại bộ đếm offline → Anonymous khi online → outbox + Go idempotency/version → kiểm thử reconnect → provider linking.**

Offline-first trở thành tiêu chí nền của S01-03/04/05/06/09. Ước lượng 80 giờ trong sprint cũ được giữ làm lịch sử, chưa phải cam kết cho phạm vi mới; cần ước lượng lại sau bài thử Room + một entity sync trên hai nền tảng. Không đưa social, media sync đầy đủ hoặc conflict phức tạp vào cùng một sprint để giữ con số cũ.
