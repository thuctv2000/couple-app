# Couple App — Thành phần nền, trải nghiệm và giữ chân người dùng

> Quyết định 07/10/2026: **offline-first**, dùng và lưu dữ liệu cá nhân bền vững ngay lần mở đầu không có mạng. [Đặc tả offline-first](couple-app-offline-first.md) thay thế mô tả chỉ giữ draft hoặc trì hoãn offline bên dưới. Cloud sync và auth không chặn luồng local; đây là quyết định, chưa phải code đã có.

- Ngày nghiên cứu: 07/10/2026.
- Phạm vi: các thành phần quanh tính năng chính, UX trên Android/iOS, activation, retention, chia sẻ, hỗ trợ và vận hành.
- Bối cảnh: một người + AI, 40 giờ/tuần; KMP logic chung, Compose/SwiftUI riêng, Go, Next.js landing/admin; ưu tiên Firebase.
- Đây là nghiên cứu và đề xuất, chưa phải tính năng đã triển khai hoặc cam kết lịch. Không thay thế [danh mục tính năng](couple-app-product-operations-notes.md) hay tự mở rộng [sprint 01](couple-app-sprint-01.md).
- Phân biệt rõ: **quan sát từ nguồn**, **suy luận áp dụng**, **giả thuyết cần thử với người dùng**.

## 1. Kết luận sản phẩm

Đề xuất ưu tiên: **vào app thấy ngay giá trị → tin rằng dữ liệu được giữ → có một việc nhỏ để làm cùng nhau → quay lại vì câu chuyện chung**.

App đếm ngày có thể hữu ích dù không được mở hằng ngày. Widget và nhắc dịp giúp người dùng nhận giá trị ngoài app; đừng ép tăng số phiên bằng thông báo hoặc streak. Với người dùng chỉ cần bộ đếm, dùng ít không tự động nghĩa là sản phẩm thất bại.

Ba nhóm nhu cầu phải được thiết kế riêng:

| Nhóm | Công việc cần hoàn thành | Lý do quay lại đề xuất |
|---|---|---|
| Dùng một mình/chưa ghép đôi | Xem ngày, lưu kỷ niệm, chuẩn bị dịp đặc biệt | Bộ đếm, kỷ niệm cá nhân, nhắc dịp |
| Hai người đã ghép | Chia sẻ và phản hồi trong không gian riêng | Khoảnh khắc mới, câu hỏi ngắn, kế hoạch chung |
| Muốn chia sẻ ra ngoài | Tạo nội dung đẹp, kiểm soát người xem | Thiệp cột mốc, recap, phản hồi phù hợp |

Không bắt nhóm đầu tiên mời người yêu mới được dùng. Không đưa mọi người vào feed cộng đồng mặc định.

## 2. Nguồn nghiên cứu và mức độ tiếp cận

| Nguồn | Đã kiểm tra trong lượt này | Bài học hoặc cách dùng |
|---|---|---|
| [Mobbin](https://mobbin.com/) | Trang công khai mô tả thư viện screens, flows, UI elements; chưa truy cập đầy đủ bộ flow sau đăng nhập | Tra pattern theo tình huống, sau đó xem cả luồng. Không coi screenshot đẹp là bằng chứng tăng retention |
| [Mobbin — mô tả flow](https://mobbin.com/?trk=public_post_share-update_update-text) | Trang công khai nêu video, prototype và collections | Khi có quyền truy cập, lưu flow, trạng thái, ngày capture và ghi chú lý do chọn, không chỉ gom ảnh |
| [Page Flows — Notification settings](https://pageflows.com/screens/CoupleRewind/view/notification-settings/) | Có kết quả được lập chỉ mục; công cụ không tải được trang đầy đủ | Nguồn bổ sung để xem chuỗi tương tác; chưa khẳng định đã xem video hoặc màn hình cụ thể |
| [UserOnboard](https://www.useronboard.com/) | Trang công khai và danh mục teardown | Phù hợp tìm cách đưa người mới tới kết quả đầu tiên; chưa audit mọi teardown |
| [Growth.Design — Hopper](https://growth.design/case-studies/hopper-permission-requests-ux) | Đọc nội dung case study | Prompt theo hành động và lợi ích người dùng; hỏi đúng ngữ cảnh vẫn có thể quá sớm khi chưa tạo niềm tin |
| [Growth.Design — Spotify Wrapped](https://growth.design/case-studies/spotify-wrapped-psychology) | Đọc nội dung case study | Recap có thể kể câu chuyện từ dữ liệu cá nhân; đề xuất áp dụng vào kỷ niệm của cặp đôi |
| [Growth.Design — Typeform offboarding](https://growth.design/case-studies/typeform-offboarding) | Đọc nội dung case study | Giảm rào cản khi rời dịch vụ, tránh dùng cảm giác tội lỗi để giữ khách |
| [NN/g — Empty states](https://www.nngroup.com/articles/empty-state-interface-design/) | Nội dung bài nghiên cứu UX | Trạng thái trống nên giải thích hiện trạng và đưa bước tiếp theo |
| [Apple — Onboarding](https://developer.apple.com/design/human-interface-guidelines/onboarding) | Nội dung được lập chỉ mục | Onboarding ngắn, tùy chọn; hướng dẫn gần chỗ người dùng thao tác |
| [Paired FAQ](https://www.paired.com/frequently-asked-questions) và [pairing](https://support.paired.com/en/articles/164636-how-do-i-pair-with-my-partner) | Nội dung nguồn sản phẩm | Hoạt động ngắn và lời mời xuất hiện ở nhiều điểm. Không sao chép toàn bộ mô hình yêu cầu hai người của Paired vào app này |
| [Duolingo — cải tiến streak](https://blog.duolingo.com/improving-the-streak/) | Bài viết của đội sản phẩm | Họ báo cáo A/B test giảm yêu cầu để duy trì streak, tăng D14 retention 3,3%. Đây là kết quả riêng của Duolingo, không phải dự báo cho app cặp đôi |

Giới hạn: lượt này là desk research, không phải benchmark hình ảnh toàn diện trên Mobbin/Page Flows, không có dữ liệu retention của app mình, chưa phỏng vấn người dùng. Các case study có thể phản ánh phiên bản lịch sử. Không tải hoặc sao chép kho screenshot có giới hạn truy cập.

## 3. Phân loại mức ưu tiên

- **Nền:** cần trước khi beta của luồng liên quan mở cho người thật.
- **Kèm feature:** triển khai đồng thời với feature phụ thuộc. Ví dụ report/block đi cùng social, không thể ra social rồi trì hoãn xử lý lạm dụng.
- **Thử sau:** thử khi hành vi cốt lõi ổn định và đã có tín hiệu nhu cầu.

Đây là ưu tiên cho lớp UX bổ sung, không đánh lại P0/P1/P2 trong tài liệu cũ. Không có nghĩa mọi dòng “Nền” phải hoàn thành trong sprint 01.

## 4. Danh mục 40 hạng mục cần thiết kế

### A. Bắt đầu và bảo toàn tài khoản

| ID | Hạng mục | Hành vi đề xuất và điều kiện nghiệm thu | Ưu tiên |
|---|---|---|---|
| UX01 | Bắt đầu nhanh | Nhập ngày yêu → thấy bộ đếm; ảnh/biệt danh cho phép bổ sung sau. Không bắt xem carousel giới thiệu dài | Nền |
| UX02 | Thiết lập dở dang | Quay lại vẫn giữ ngày/biệt danh đã nhập; tiến/lùi không mất dữ liệu. Lần đầu offline lưu bền vững trong local space và dùng ngay, không giả báo Anonymous đã tạo | Nền |
| UX03 | Giải thích Anonymous | Gọi là “Dùng ngay”; trong tài khoản giải thích chưa có phương thức khôi phục. Anonymous không đồng nghĩa dữ liệu không thể liên hệ tới người dùng | Nền |
| UX04 | Link và khôi phục | Đề nghị Google/Apple sau lần lưu có giá trị; có “Để sau”. Phân biệt link với đăng nhập tài khoản cũ; xung đột không ghi đè hoặc tự gộp | Nền |
| UX05 | Bảo vệ trước khi đăng xuất | Cảnh báo nguy cơ không khôi phục được khách chưa link; không hứa dữ liệu luôn trở lại sau xóa/cài app. Không chặn đường xóa tài khoản | Nền |

Firebase hỗ trợ nâng cấp Anonymous bằng liên kết thông tin đăng nhập. UX khôi phục và xung đột vẫn phải thiết kế riêng. [Firebase Anonymous Auth](https://firebase.google.com/docs/auth/android/anonymous-auth)

### B. Điều hướng và trạng thái hệ thống

| ID | Hạng mục | Hành vi đề xuất và điều kiện nghiệm thu | Ưu tiên |
|---|---|---|---|
| UX06 | Home có một việc chính | Bộ đếm dễ đọc; một CTA phù hợp trạng thái như “Lưu khoảnh khắc” hoặc “Thêm ngày đặc biệt”. Không đồng thời đẩy link, invite, review và paywall | Nền |
| UX07 | Navigation/restoration | Back đúng native; giữ draft và vị trí đọc khi đổi tab/quay lại. Chưa có feature thì không dựng tab chết | Nền |
| UX08 | Loading/empty/error khác nhau | Loading có phản hồi; trống có bước bắt đầu; lỗi có lý do và retry; tìm kiếm không kết quả có bỏ bộ lọc. Không biến API lỗi thành danh sách trống | Nền |
| UX09 | Offline và đồng bộ | Đọc cache; ghi rõ “Đang chờ đồng bộ”, “Đã đồng bộ”, “Chưa lưu được”. Retry không tạo bản ghi trùng; xung đột dữ liệu chung có cách giải quyết | Nền khi có lưu |
| UX10 | Form, xác nhận, hoàn tác | Date picker native, lỗi ngay tại field; không mất nội dung khi mạng lỗi. Undo cho thao tác có thể đảo; xác nhận rõ cho xóa không thể hoàn tác | Nền |
| UX11 | Deep link | Link mở đúng nội dung, giữ đích sau auth; xử lý đã xóa, mất quyền và hết hạn. Nội dung riêng vẫn kiểm quyền phía server | Kèm invite/share/push |

Mỗi feature phải có ma trận trạng thái: khởi tạo, tải, có dữ liệu, trống, offline, lỗi, không có quyền, thành công. Chỉ dùng trạng thái thực sự có nghĩa; không tạo tám màn riêng nếu một component đủ thể hiện.

### C. Quyền, thông báo và cài đặt

| ID | Hạng mục | Hành vi đề xuất và điều kiện nghiệm thu | Ưu tiên |
|---|---|---|---|
| UX12 | Xin quyền theo ý định | Xin notification khi bật nhắc dịp; truy cập ảnh qua picker lúc chọn ảnh. Từ chối vẫn dùng phần còn lại; có hướng dẫn mở Settings khi cần | Kèm feature |
| UX13 | Trung tâm thông báo trong app | Danh sách lời mời/phản hồi quan trọng, đánh dấu đã đọc; bấm mở đúng đích. Có nguồn xem lại khi push bị tắt | Kèm tương tác hai người |
| UX14 | Tùy chọn nhắc | Tách kỷ niệm, hoạt động từ người yêu, gợi ý sản phẩm; giờ yên lặng, múi giờ, tạm dừng. Không yêu cầu email/SMS marketing để dùng nhắc dịp | Kèm notification |
| UX15 | Chống thông báo trùng/cũ | Một sự kiện không phát nhiều lần; gửi lại có idempotency, loại lời mời hết hạn, kiểm tra unpair trước khi gửi. Push mặc định giấu nội dung nhạy cảm | Kèm notification |
| UX16 | Cài đặt dễ hiểu | Tài khoản, dữ liệu, người xem, thông báo, giao diện, trợ giúp; hiện quyền hệ thống đang tắt. Dùng nhãn người dùng hiểu, không đưa tên SDK vào UI | Nền |

Android khuyến nghị cân nhắc vị trí xin quyền trong hành trình và cho người dùng kiểm soát thông báo. Các ngưỡng tần suất dưới đây là đề xuất sản phẩm, không phải chuẩn nền tảng. [Android notification design](https://developer.android.com/design/ui/CoupleRewind/guides/home-screen/notifications)

### D. Ghép đôi và vòng đời mối quan hệ

| ID | Hạng mục | Hành vi đề xuất và điều kiện nghiệm thu | Ưu tiên |
|---|---|---|---|
| UX17 | Dùng solo có giá trị | Không gian cá nhân đầy đủ bộ đếm/kỷ niệm; không có banner “chưa hoàn thành” thường trực chỉ vì chưa mời người yêu | Nền |
| UX18 | Mời và nhận lời mời | Xem trước người mời, chấp nhận/từ chối; link/mã dự phòng; hết hạn, dùng rồi, tự mời, đã ghép người khác đều có thông báo riêng | Kèm pairing |
| UX19 | Chờ đối phương | Có “Tiếp tục dùng app”; xem lại/hủy lời mời. Không tự gửi lại hoặc đổ lỗi khi người kia chưa tham gia | Kèm pairing |
| UX20 | Ranh giới riêng/chung | Nội dung gắn nhãn “Chỉ mình tôi”, “Hai bạn” hoặc người xem khác. Nội dung cá nhân cũ không tự chuyển thành chung khi ghép | Kèm shared data |
| UX21 | Ngừng chia sẻ/unpair | Một người có thể ngừng chia sẻ, không cần người kia duyệt. Dừng push chung, thu hồi quyền, giải thích dữ liệu nào còn được giữ; không xóa toàn bộ nội dung của người kia | Kèm pairing |

Đề xuất giải quyết trước khi code shared data: phân biệt người tạo, người xem và nội dung chung; unpair không đồng nghĩa xóa tài khoản. Cache phía thiết bị và widget cũng phải được cập nhật/thu hồi phù hợp; không hứa có thể thu hồi ảnh người khác đã tải xuống.

### E. Vòng lặp giá trị và giữ chân

| ID | Hạng mục | Hành vi đề xuất và điều kiện nghiệm thu | Ưu tiên |
|---|---|---|---|
| UX22 | Khoảnh khắc nhanh | Thêm ảnh/ghi chú ít bước, lưu draft; xác nhận lưu thật và hiển thị ngay trong timeline | Kèm memories |
| UX23 | Nhắc dịp dẫn tới hành động | “Sắp 100 ngày” → tạo thiệp/lên kế hoạch, thay vì notification chỉ kéo về Home | Kèm milestones |
| UX24 | Widget | Bộ đếm/dịp tới, mở đúng màn; chế độ giấu ảnh/tên. Thiết kế refresh theo hệ điều hành, không hứa cập nhật tức thời liên tục | Kèm widget |
| UX25 | Câu hỏi hoặc check-in ngắn | Một việc tự nguyện trong khoảng 30–90 giây; bỏ qua chủ đề; trạng thái chờ phản hồi thân thiện; không chấm điểm tình yêu | Thử sau |
| UX26 | Kỷ niệm ngày này/recap | Chỉ hiện khi có dữ liệu thật; cho ẩn ngày/ảnh/người không muốn gợi lại; xem trước trước khi chia sẻ | Thử sau |
| UX27 | Nhịp tuần linh hoạt | Thử mục tiêu như ba khoảnh khắc mỗi tuần, tự chọn hoặc tắt; nghỉ không mất lịch sử. Không mặc định streak liên tục | Thử sau |
| UX28 | Quay lại sau nghỉ | Giữ nguyên dữ liệu, một gợi ý bắt đầu lại; không phê bình, không hiển thị backlog nhiệm vụ “nợ”. Người đã pause/unpair không nhận chiến dịch gọi về cặp đôi | Thử sau |

### F. Chia sẻ, cộng đồng và AI

| ID | Hạng mục | Hành vi đề xuất và điều kiện nghiệm thu | Ưu tiên |
|---|---|---|---|
| UX29 | Thiệp chia sẻ | Preview và chọn nội dung; có thể ẩn tên/ngày/ảnh. Gọi share sheet không đồng nghĩa đã đăng thành công | Kèm share |
| UX30 | Feed có chủ đích | Hiển thị rõ phạm vi người xem, filter/follow, ẩn chủ đề; trạng thái không có bài mới. Không đưa kỷ niệm riêng vào feed tự động | Kèm social |
| UX31 | Report, block, kiểm duyệt | Báo cáo từ nội dung và hồ sơ; lý do, xác nhận tiếp nhận; block cắt tương tác và có xử lý phía server; admin có hàng đợi và audit | Kèm social, trước beta |
| UX32 | AI có ngữ cảnh được chọn | Người dùng chọn ghi chú/ngày dịp để AI gợi ý; hiển thị dữ liệu sẽ dùng, cho sửa rồi mới lưu/gửi. AI không tự nhắn người yêu hoặc đọc nội dung riêng của họ | Kèm AI |
| UX33 | AI thất bại và phản hồi | Hủy, retry, lưu draft, báo lỗi/tốc độ giới hạn; nút đánh giá gợi ý hữu ích/không phù hợp. Không suy diễn “mức độ yêu” từ tần suất phản hồi | Kèm AI |

Khi ra mắt nội dung cộng đồng, report/block và vận hành xử lý không phải phần trang trí. Apple mô tả yêu cầu cho UGC, gồm lọc nội dung, báo cáo, xử lý và chặn người lạm dụng. Cần rà lại chính sách store khi chuẩn bị phát hành. [App Review — UGC](https://developer.apple.com/app-store/review/guidelines/#user-generated-content)

### G. Chất lượng và niềm tin

| ID | Hạng mục | Hành vi đề xuất và điều kiện nghiệm thu | Ưu tiên |
|---|---|---|---|
| UX34 | Accessibility | Font lớn không cắt nút, screen reader đọc đúng thứ tự, không chỉ dùng màu để báo lỗi; giảm chuyển động và kiểm tra dark mode | Nền |
| UX35 | Ngày, ngôn ngữ, định dạng | Tiếng Việt nhất quán; date-only khác timestamp; ghi rõ ngày đầu tính là 1; đổi múi giờ không âm thầm đổi ngày kỷ niệm | Nền |
| UX36 | Trợ giúp và feedback | FAQ theo ngữ cảnh, gửi vấn đề kèm mã lỗi/version tùy chọn; có trạng thái đã nhận. Không bắt user công khai nhật ký để được hỗ trợ | Nền |
| UX37 | Quản lý/xóa dữ liệu | Xem phạm vi dữ liệu và yêu cầu xóa trong app; phân biệt unpair, đăng xuất, xóa account. Export dữ liệu cá nhân được phép khi có persistence phù hợp | Xóa: trước beta có account; export: kèm dữ liệu |
| UX38 | Subscription/restore | Nếu thu phí: giá/chu kỳ rõ, restore, trạng thái hết hạn, quản lý/hủy qua store. Không dùng paywall để chặn xóa tài khoản hoặc xuất dữ liệu đã cam kết | Kèm trả phí |
| UX39 | Feedback/review đúng lúc | Hỏi một câu sau khi hoàn thành việc; xin review thưa và theo cơ chế nền tảng. Không thưởng để đổi review hoặc chỉ đưa người hài lòng tới store | Thử sau |
| UX40 | Bảo trì và cập nhật | Mạng chậm khác bảo trì; update mềm có “Để sau”; chỉ bắt buộc khi phiên bản không còn an toàn/tương thích; giữ dữ liệu chờ gửi | Nền cơ bản; nâng dần |

Apple có hướng dẫn về kích thước chữ và chuyển động dễ tiếp cận; app có tạo tài khoản phải cho bắt đầu xóa tài khoản trong app. [Accessible appearance](https://developer.apple.com/documentation/swiftui/accessible-appearance) · [Account deletion](https://developer.apple.com/help/app-review/guideline-reference/5-1-1-account-deletion)

## 5. Năm luồng nên thiết kế trước

### 5.1. Lần mở đầu

Mở app → “Bắt đầu” → nhập ngày yêu → thấy bộ đếm → lưu thiết lập → xác nhận trạng thái lưu → gợi ý một việc tiếp theo.

Tên/ảnh có thể để sau. Không hỏi notification, link tài khoản, mời người yêu và đánh giá store liên tiếp. Lúc chưa có mạng, lưu local bền vững và dùng đầy đủ bộ đếm; khi mạng trở lại mới tạo/khôi phục phiên Anonymous và đồng bộ. Phân biệt kết quả tính local với việc lưu thành công.

Giả thuyết thử nghiệm: người dùng lần đầu tự tới bộ đếm trong khoảng một phút. Đây là mục tiêu thiết kế thử, chưa phải số liệu hoặc SLA.

### 5.2. Lưu và bảo vệ dữ liệu

Lưu kỷ niệm đầu → xem kỷ niệm → card nhỏ “Thêm cách đăng nhập để dễ khôi phục dữ liệu” → chọn Google/Apple hoặc để sau → link → giữ UID và dữ liệu.

Nếu credential đã thuộc account khác: giữ nguyên phiên/draft hiện tại, giải thích lựa chọn đăng nhập account cũ; không tự xóa account khách để “sửa lỗi”. Cần đặc tả riêng việc chuyển dữ liệu có xác nhận, không triển khai merge ngầm.

### 5.3. Ghép đôi

Mời → chọn cách chia sẻ → người nhận mở link → xem lời mời → xác thực → chấp nhận → server tạo liên kết → cả hai thấy trạng thái mới.

Nếu chưa cài app: landing Next.js giải thích lời mời và hướng dẫn cài; có mã để nhập lại. Không hứa link tự khôi phục ngữ cảnh sau cài nếu chưa làm deferred linking. Không ghi mã/token lời mời vào analytics hoặc log URL đầy đủ.

Firebase Dynamic Links đã ngừng hoạt động ngày 25/08/2025, nên không đưa vào thiết kế mới. Đề xuất Universal Links/App Links trên domain mình quản lý + mã nhập dự phòng; chưa cần dịch vụ khác. [Firebase Dynamic Links FAQ](https://firebase.google.com/support/dynamic-links-faq)

### 5.4. Quay lại vì điều có ích

Một dịp gần đến hoặc khoảnh khắc mới → thông báo được cho phép → mở đúng nội dung → hành động ngắn → xác nhận → có thể rời app.

Mẫu nội dung đề xuất: “Sắp tới một ngày đặc biệt của hai bạn. Bạn muốn chuẩn bị một tấm thiệp?” Khi chưa đồng ý hiện nội dung ở màn hình khóa, dùng thông báo chung như “Bạn có cập nhật mới”.

Mặc định thử nghiệm: gợi ý không khẩn cấp tối đa một lần/ngày, có giờ yên lặng; ghép các tương tác gần nhau và ngừng chiến dịch khi người dùng tắt. Đây là trần khởi đầu để thử, không phải chỉ tiêu phải gửi đủ.

### 5.5. Tạm nghỉ hoặc rời đi

Cài đặt → chọn tạm dừng nhắc/ngừng chia sẻ/xóa account → giải thích tác động cụ thể → xác nhận khi cần → trạng thái hoàn tất hoặc đang xử lý có thời hạn công bố.

Câu hỏi lý do chỉ tùy chọn. Không dùng “người yêu sẽ buồn” hoặc mất streak để cản. Nếu server còn đang xóa, không hiển thị “đã xóa toàn bộ” trước khi hoàn tất.

## 6. Các vòng lặp giữ chân nên thử

Đây là giả thuyết cho sản phẩm này, không phải kết quả đã chứng minh từ nguồn khác.

| Vòng lặp | Tác nhân → hành động → giá trị | Đo điều gì | Điều kiện dừng/sửa |
|---|---|---|---|
| Kỷ niệm | Khoảnh khắc thật → lưu → timeline lớn dần → muốn lưu tiếp | Tỷ lệ có kỷ niệm thứ hai; tuần có lưu/xem lại | Upload thất bại hoặc user lo người xem |
| Hai chiều | Người A chia sẻ → B phản hồi → A nhận được sự quan tâm | Số cặp có tương tác từ cả hai trong tuần | Khiếu nại áp lực, gửi nhắc quá nhiều |
| Dịp đặc biệt | Nhắc đúng lúc → chuẩn bị thiệp/kế hoạch → hoàn thành ngoài đời | Tỷ lệ nhắc dẫn đến lưu kế hoạch/thiệp | Múi giờ sai, thông báo không đúng mong muốn |
| Câu hỏi ngắn | Gợi ý phù hợp → trả lời tự nguyện → trò chuyện | Tỷ lệ quay lại hoạt động tuần sau, phản hồi hữu ích | Chủ đề khó chịu, bỏ qua nhiều, chỉ bấm cho xong |
| Recap | Có đủ dữ liệu → xem lại → chọn khoảnh khắc → lưu/chia sẻ | Xem xong recap, lưu lại; không chỉ share sheet opened | Nhắc kỷ niệm không muốn thấy hoặc lộ dữ liệu |
| Quay lại | Người dùng tự mở lại → tiếp tục dễ dàng → giá trị mới | Tỷ lệ có hành động giá trị sau quay lại | Nội dung trách móc, nhiệm vụ cũ gây ngợp |

**Chưa ưu tiên:** bảng xếp hạng cặp đôi, điểm “độ yêu”, streak chung bị mất vì một người bận, thông báo ghen tuông, feed vô hạn, tự tạo tương tác giả bằng AI. Chúng không giải quyết nhu cầu đã xác nhận trong giai đoạn này.

## 7. Thành phần vận hành phía sau UX

| Thành phần | Cần có để hỗ trợ UX | Hướng phù hợp stack đã chốt |
|---|---|---|
| Analytics taxonomy | Phân biệt screen view, click, thành công thật và thất bại | Firebase Analytics ở native adapter; schema event thống nhất |
| Crash/performance | Phát hiện phiên lỗi và đoạn chậm ở flow chính | Firebase Crashlytics/Performance khi tích hợp; lỗi Go theo request ID |
| Cấu hình/kill switch | Tắt gợi ý lỗi hoặc feature đang gây vấn đề | Firebase Remote Config cho client; quyền/auth luôn do Go thực thi |
| Notification orchestration | Preferences, giờ gửi, chống trùng, đối tượng hợp lệ | Go quyết định gửi, FCM vận chuyển; nhắc local dùng API native khi phù hợp |
| Nội dung gợi ý | Câu hỏi, chủ đề, lịch dùng, phiên bản, ngừng phát nội dung | Trang admin Next.js tối thiểu; không cần xây CMS tổng quát ngay |
| Support | Mã lỗi, phiên bản app, tình trạng ticket và phản hồi | Form/FAQ web + endpoint Go; không chọn thêm dịch vụ trước khi có nhu cầu |
| Moderation | Reports, ưu tiên xử lý, quyền hạn, lý do quyết định | Next.js admin + Go + audit; truy cập nội dung theo mục đích/quyền |
| Xóa và unpair | Thu hồi truy cập, job xóa, trạng thái, retry, thông báo phù hợp | Go điều phối; không chỉ xóa user Firebase rồi bỏ lại dữ liệu |
| Release quality | Bản beta, rollback, tương thích dữ liệu và API | Checklist build/test hai native UI; chạy thử flow lỗi trước phát hành |
| Thử nghiệm | Biến thể, exposure, mục tiêu và giới hạn tác hại | Remote Config/A/B Testing khi đủ dữ liệu; pair-level cần thiết kế riêng |

Firebase Remote Config hỗ trợ thay đổi cấu hình client; Firebase A/B Testing hỗ trợ thử biến thể với Analytics. Không dùng client flag làm phân quyền. Với thử nghiệm liên quan hai người, random theo couple ở backend để tránh hai thành viên nhận hành vi xung đột; không giả định Firebase tự xử lý đơn vị cặp đôi. [Remote Config](https://firebase.google.com/docs/remote-config) · [A/B Testing](https://firebase.google.com/docs/ab-testing)

## 8. Đo lường để tránh giữ chân trên giấy

### Định nghĩa trước khi dashboard

- **Giá trị đầu tiên:** đã xem bộ đếm từ ngày hợp lệ; đo riêng preview local và cấu hình đã lưu thành công.
- **Activation bền vững:** cấu hình đã lưu và khôi phục được ở phiên tiếp theo. Link account là chỉ số bảo vệ dữ liệu, không phải điều kiện mặc định để mọi người được tính activation.
- **Hoạt động có giá trị:** lưu/xem lại kỷ niệm, quản lý dịp, phản hồi cho người yêu, hoàn thành check-in; loại auto refresh và notification delivered.
- **Cặp hoạt động hai chiều mỗi tuần:** cả hai thành viên có ít nhất một hành động thuộc tập trên trong cùng tuần; chỉ tính cặp đã ghép, báo riêng số cặp mới/hiện hữu/unpair.
- **D7/D30 exact-day retention:** tỷ lệ người thuộc cohort có hoạt động đủ điều kiện đúng ngày thứ 7/30 tính từ mốc đã chọn. Cố định múi giờ phân tích; không trộn với “quay lại bất cứ lúc nào từ ngày 7 trở đi”.
- **W1 retention:** tỷ lệ cohort có hoạt động trong ngày 7–13; thường dễ diễn giải hơn cho người chỉ quan tâm dịp/kỷ niệm. Chỉ báo cohort đã đủ tuổi quan sát.
- **Solo retention:** đo riêng người chưa ghép; không coi họ thất bại vì chưa thành cặp.
- **Widget:** không suy ra lượt người nhìn widget từ số lần refresh. Dùng số lượt mở từ widget, khả năng cập nhật và phản hồi người dùng; ghi rõ giới hạn quan sát.

### Bộ event tối thiểu đề xuất

| Event | Khi ghi | Thuộc tính không chứa nội dung riêng |
|---|---|---|
| onboarding_started / counter_previewed | Vào thiết lập / hiển thị kết quả hợp lệ | platform, app_version, entry_point |
| setup_saved / setup_restored | Lưu thành công / mở lại dữ liệu đã lưu | storage_state, auth_state, duration_bucket |
| account_link_started / succeeded / failed | Trạng thái link được xác nhận | provider, error_code đã chuẩn hóa |
| invite_created / accepted / expired | Kết quả nghiệp vụ server | flow_version, entry_point; không có invite token |
| memory_saved / save_failed | Kết quả thật; hàng chờ local phải là event khác | media_type, sync_state, error_code; không có caption |
| reminder_enabled / notification_opened | User bật nhắc / mở từ notification | category, destination_type |
| checkin_completed / partner_response_viewed | Hành động trong feature | content_category; không chứa câu trả lời |
| recap_viewed / share_sheet_opened | Xem recap / mở share sheet | template_id; không giả là published |
| unpair_completed / deletion_requested | Sự kiện nghiệp vụ | status_code, app_version |

Chọn nơi phát event thành công duy nhất hoặc deduplicate; không double-count cùng một hành động từ Go và mobile. Dùng request/correlation ID cho debug với thời hạn lưu phù hợp; không đưa token, ảnh, nội dung nhật ký, hội thoại AI, ngày sinh hoặc URL riêng vào analytics. Khi user không đồng ý analytics tùy chọn, vẫn cho dùng luồng chính theo chính sách đã công bố.

### Năm thử nghiệm ưu tiên

| Giả thuyết | Cách thử nhỏ | Chỉ số chính | Guardrail |
|---|---|---|---|
| Thấy bộ đếm sớm giúp hiểu giá trị | Ngày yêu trước so với form nhiều field | Time-to-value và setup_saved | Khôi phục sau mở lại, tỷ lệ ngày nhập sai |
| Mời link sau khi lưu hiệu quả hơn lúc mở app | Card ngữ cảnh so với lời mời đầu onboarding | Link thành công và retention cohort | Bỏ ngang, hiểu sai về khôi phục |
| CTA cụ thể giúp empty state | “Lưu khoảnh khắc đầu tiên” so với hướng dẫn chung | Kỷ niệm đầu/tiếp theo được lưu | Lưu nhầm, xóa ngay, save_failed |
| Nhắc dịp tốt hơn thông báo kéo về app | Thử nhóm không nhắc/nhắc có hành động với người đã opt-in | Hành động có giá trị sau nhắc | Tắt thông báo, report khó chịu |
| Nhịp tuần phù hợp hơn streak | Prototype mục tiêu tuần trước; chỉ A/B nếu đáng làm | W1/W4 meaningful retention | Áp lực, unpair, phản hồi tiêu cực |

Ở giai đoạn ít người: thử nhiệm vụ với 5–8 người, gồm solo và cặp đôi; quan sát chỗ họ mắc, không coi mẫu nhỏ là thống kê thị trường. Khi chạy A/B: chốt trước metric, đơn vị random, thời gian và cỡ mẫu theo baseline; không công bố thắng chỉ vì vài phần trăm sau hai ngày. Retention cao ở người tự chọn bật widget/link không chứng minh widget/link gây tăng retention.

## 9. Cách làm board tham khảo Mobbin/Page Flows

Các từ khóa sau là truy vấn đề xuất, không phải danh sách flow đã xem. Không khẳng định ứng dụng nào có sẵn trong tài khoản/thư viện hiện tại.

| Collection đề xuất | Từ khóa tìm | Cần ghi lại |
|---|---|---|
| 01 — First value | onboarding, guest, skip sign up, account setup | Bao nhiêu bước bắt buộc trước kết quả đầu tiên |
| 02 — Account recovery | link account, sign in, account exists, restore | Có giữ dữ liệu, có đường quay lại an toàn không |
| 03 — Empty/error | empty state, no results, offline, retry, upload failed | Hành động sửa lỗi và cách giữ draft |
| 04 — Permissions | notification permission, photo picker, denied | Hành động nào làm xuất hiện prompt, từ chối đi đâu |
| 05 — Invitation | invite, accept invitation, pending, expired | Sender/receiver, chờ, hủy và fallback |
| 06 — Return value | daily activity, reminder, milestone, recap, widget | Giá trị sau khi mở, khả năng bỏ qua/tạm nghỉ |
| 07 — Privacy/exit | audience, block, report, delete account, cancel | Hiểu được người xem và tác động khi rời đi |
| 08 — AI/support | AI loading, retry, feedback, help center | Cancel, sửa kết quả, biết dữ liệu nào đang dùng |

Mỗi mẫu lưu: URL gốc; app/nền tảng; ngày xem và phiên bản nếu có; luồng bắt đầu/kết thúc; screenshot do thư viện cho phép; điều học được; điều không phù hợp app cặp đôi; giả thuyết đo lường. Xem trước–trong–sau thao tác, không chỉ màn đẹp nhất. Hai mẫu tốt và một phản ví dụ cho mỗi luồng quan trọng là điểm khởi đầu vừa sức.

## 10. Thứ tự đưa vào backlog

1. **Trong nền móng hiện tại:** UX01–10, UX16–17, UX34–36; áp dụng vừa đủ vào flow Anonymous → thiết lập → lưu → mở lại. Chuẩn bị việc xóa account UX37 trước beta có người dùng thật.
2. **Khi thêm pairing/memories/milestones:** UX11–15, UX18–24, UX29. Đừng tách unpair, quyền xem, lỗi invite và đồng bộ khỏi feature chính.
3. **Khi có dữ liệu sử dụng:** thử UX25–28, UX39; trước hết xem người dùng có lưu lần hai/quay lại tự nhiên không.
4. **Khi mở social/AI/trả phí:** hoàn thiện UX30–33, UX38 và vận hành tương ứng. Đây là phạm vi tăng thêm, không nằm trong scaffold hiện tại.
5. **Xuyên suốt:** UX40 và đo chất lượng/lỗi; không chờ tới có nhiều người mới quan tâm mất dữ liệu.

Không cộng 40 dòng thành 40 feature độc lập: nhiều dòng là tiêu chí nghiệm thu của feature đã có trong danh mục 53 feature. Mỗi ticket triển khai nên gắn ID UX liên quan và cập nhật ước lượng của ticket đó, tránh đếm đôi thời gian.

Bước thiết kế tiếp theo phù hợp: wireframe năm luồng ở mục 5, kèm trạng thái lỗi/solo/pending; thử với người dùng trước khi đầu tư nhiều theme, motion hoặc cơ chế streak.
