# Couple App — Tính năng, luồng người dùng, analytics và vận hành

- Ngày cập nhật: 28/09/2026.
- Trạng thái: tài liệu nghiên cứu và định hướng; chưa phải đặc tả triển khai cuối cùng.
- Phạm vi: tổng hợp danh sách tính năng, nghiên cứu sản phẩm tham khảo, phạm vi MVP, luồng sử dụng, log, analytics, AI và các thành phần vận hành.
- Nguồn: trao đổi sản phẩm và nghiên cứu tài liệu chính thức trong cuộc trò chuyện.

## 1. Bối cảnh sản phẩm

Ứng dụng dành cho cặp đôi: đếm ngày yêu, lưu kỷ niệm, tương tác hằng ngày, quản lý kế hoạch chung, chia sẻ kiểu mạng xã hội và sử dụng AI để hỗ trợ giao tiếp, lên kế hoạch.

Định hướng trải nghiệm: **đếm ngày → lưu khoảnh khắc → quan tâm nhau → chia sẻ chọn lọc**.

Ba đơn vị cần quản lý và đo lường riêng:

1. Cá nhân: tài khoản, nội dung riêng, trải nghiệm trước khi ghép đôi.
2. Cặp đôi: thành viên, kỷ niệm chung, lịch và tương tác hai chiều.
3. Cộng đồng: bạn bè, bài đăng, bình luận, báo cáo và kiểm duyệt.

Giả định nghiên cứu ban đầu là người dùng Việt Nam từ 18 tuổi, đang yêu hoặc yêu xa, sử dụng iOS/Android. Đây là giả định cần xác nhận, không phải quyết định đã chốt.

### 1.1. Nghiên cứu sản phẩm tham khảo

| Sản phẩm | Tính năng được mô tả trên nguồn chính thức | Bài học cho sản phẩm |
|---|---|---|
| Between | Không gian riêng, ảnh/video/ghi chú, ngày kỷ niệm, lịch chung | Gắn đếm ngày với câu chuyện và sinh hoạt cặp đôi. [Nguồn](https://between.us/?lang=th) |
| Paired | Câu hỏi, quiz, trò chơi cho hai người, hoạt động khoảng 5 phút | Hoạt động ngắn, dễ hoàn thành tạo cơ hội quay lại. [Nguồn](https://www.paired.com/frequently-asked-questions) |
| Lovewick | Ý tưởng hẹn hò, wishlist, ghi nhớ sở thích, timeline | Kết nối tìm ý tưởng → lên kế hoạch → trải nghiệm → lưu kỷ niệm. [Nguồn](https://lovewick.com/features/) |
| Locket | Ảnh trên widget với nhóm bạn thân, không hiển thị bộ đếm reaction | Chia sẻ thân mật và nhanh, giảm áp lực thành tích. [Nguồn](https://apps.apple.com/us/app/locket-widget/id1600525061) |
| Flamme | Câu hỏi hằng ngày, tương tác cảm xúc, AI hỗ trợ tình cảm | AI cần hoàn thành công việc cụ thể, không chỉ có chatbot. [Nguồn](https://flamme.app/frequently-asked-questions-faq) |

Cơ hội định vị đề xuất: kết nối **kỷ niệm riêng tư → hành động quan tâm → chia sẻ tự nguyện**, với tiếng Việt tự nhiên. Đây là giả thuyết cần kiểm chứng, chưa phải kết luận về khoảng trống thị trường.

### 1.2. Danh sách tính năng đầy đủ

Danh sách dưới đây lưu lại các nhóm tính năng đã trao đổi và được chủ sản phẩm đánh giá phù hợp. Mức ưu tiên vẫn là đề xuất để chốt phạm vi triển khai:

- **P0:** cần có trong phiên bản ra mắt.
- **P1:** phát triển sau khi kiểm chứng nhu cầu.
- **P2:** mở rộng khi sản phẩm có người dùng ổn định.

#### A. Tài khoản và kết nối cặp đôi

| Mã | Feature | Trải nghiệm đề xuất | Ưu tiên |
|---|---|---|---|
| A1 | Bắt đầu nhanh | Firebase Anonymous trước; nhập biệt danh/ngày yêu và dùng ngay. Liên kết Google/Apple khi muốn, giữ dữ liệu hiện tại khi link thông thường thành công. | P0 |
| A2 | Mời người yêu | Link hoặc QR; người nhận xác nhận trước khi tạo không gian chung. | P0 |
| A3 | Dùng khi chưa ghép đôi | Đếm ngày, nhật ký và kế hoạch cá nhân hoạt động khi đối phương chưa cài app. | P0 |
| A4 | Hồ sơ cặp đôi | Ảnh, biệt danh, ngày kỷ niệm; mỗi người có tài khoản và quyền kiểm soát riêng. | P0 |
| A5 | Cá nhân hóa hoàn cảnh | Chọn mới yêu, yêu lâu, yêu xa, sống chung để điều chỉnh gợi ý; cho bỏ qua. | P1 |

#### B. Đếm ngày và ngày đặc biệt

| Mã | Feature | Trải nghiệm đề xuất | Ưu tiên |
|---|---|---|---|
| B1 | Bộ đếm ngày yêu | Tổng số ngày hoặc năm–tháng–ngày; chọn ngày bắt đầu là ngày 0 hoặc ngày 1. | P0 |
| B2 | Nhiều cột mốc | Ngày gặp đầu, chính thức yêu, đính hôn, sinh nhật; đếm tới dịp sắp đến. | P0 |
| B3 | Nhắc kỷ niệm | Chọn thời điểm nhắc trước; dẫn tới lên lịch hẹn hoặc chuẩn bị thiệp. | P0 |
| B4 | Widget | Số ngày, ảnh chung và dịp sắp tới trên màn hình điện thoại. | P0 |
| B5 | Giao diện cá nhân | Theme cơ bản đẹp, dễ đọc; đổi ảnh nền và màu chủ đạo. | P0 |
| B6 | Thiệp chia sẻ | Tạo ảnh kỷ niệm như “100 ngày bên nhau”, xem trước rồi chia sẻ trong/ngoài app. | P0 |

Phân biệt “tròn một năm” và “đủ 365 ngày”; xử lý năm nhuận, múi giờ và giải thích cách tính. Ngày đặc biệt cả ngày cần phân biệt với sự kiện có giờ bắt đầu/kết thúc. [Tham khảo Between](https://help.between.us/hc/en-us/articles/115007425868-What-is-the-difference-between-Event-and-Special-Day)

#### C. Kỷ niệm và nhật ký chung

| Mã | Feature | Trải nghiệm đề xuất | Ưu tiên |
|---|---|---|---|
| C1 | Timeline tình yêu | Ảnh, ngày, đoạn viết ngắn; thêm được kỷ niệm trước khi cài app. | P0 |
| C2 | Đóng góp từ hai người | Cùng thêm nội dung, bình luận, reaction; thấy rõ người đăng. | P0 |
| C3 | Quyền xem từng nội dung | Chỉ mình tôi / Hai chúng mình / Bạn bè / Công khai; mặc định riêng tư. | P0 |
| C4 | Nhắc lại kỷ niệm | “Ngày này năm trước”; tắt theo kỷ niệm hoặc giai đoạn. | P1 |
| C5 | Album theo sự kiện | Gom ảnh chuyến đi, sinh nhật, buổi hẹn; tìm theo ngày/nhãn. | P1 |
| C6 | Thư gửi tương lai | Viết thư để mở vào ngày kỷ niệm hoặc ngày tùy chọn. | P1 |
| C7 | Recap | Bản nháp album/video tháng hoặc năm để người dùng duyệt. | P2 |

Nội dung riêng không tự trở thành nội dung chung khi ghép đôi; việc chia sẻ rộng hơn tuân theo F6.

#### D. Tương tác hằng ngày

| Mã | Feature | Trải nghiệm đề xuất | Ưu tiên |
|---|---|---|---|
| D1 | Lời quan tâm nhanh | Một chạm gửi “Nhớ bạn”, “Ôm một cái” hoặc lời nhắn ngắn. | P0 |
| D2 | Câu hỏi hôm nay | Một câu nhẹ nhàng; hai người trả lời rồi xem cùng nhau; cho bỏ qua. | P1 |
| D3 | Check-in cảm xúc | Tự chọn tâm trạng, nhu cầu được nghe/cần yên tĩnh/muốn nói chuyện; tự chọn chia sẻ. | P1 |
| D4 | Mini game hiểu nhau | Đoán sở thích, A/B, kể kỷ niệm; không dùng kết quả chấm điểm tình yêu. | P1 |
| D5 | Hoạt động chung | Gợi ý đi bộ hoặc nói lời biết ơn; đổi/bỏ qua tự nguyện. | P1 |

Tránh ép streak hoặc thông báo gây tội lỗi. Mục tiêu là giúp người dùng dễ quan tâm nhau hơn.

#### E. Lịch và quản lý cuộc sống đôi lứa

“Quản lý” ở đây là quản lý lịch, kế hoạch, kỷ niệm và công việc chung.

| Mã | Feature | Trải nghiệm đề xuất | Ưu tiên |
|---|---|---|---|
| E1 | Lịch chung cơ bản | Tạo buổi hẹn, nhắc giờ; đối phương chấp nhận hoặc đề xuất giờ khác. | P1 |
| E2 | Wishlist / bucket list | Lưu quán muốn đi, phim muốn xem, chuyến đi muốn thực hiện. | P1 |
| E3 | Sổ tay quan tâm | Món thích, điều không thích, quà; tách ghi chú cá nhân với thông tin chia sẻ. | P1 |
| E4 | Việc cần làm | Checklist chuyến đi/kỷ niệm, phân công tự nguyện. | P1 |
| E5 | Chế độ yêu xa | Hai múi giờ, đếm ngược ngày gặp lại, khung giờ gọi phù hợp. | P1 |
| E6 | Ngân sách hoạt động chung | Ngân sách hẹn hò/chuyến đi và ghi khoản chi; chưa cần liên kết ngân hàng. | P2 |

#### F. Chia sẻ kiểu mạng xã hội

Ra mắt với bảng tin bạn bè quy mô nhỏ; mở cộng đồng khám phá sau.

| Mã | Feature | Trải nghiệm đề xuất | Ưu tiên |
|---|---|---|---|
| F1 | Hồ sơ chia sẻ | Trang cá nhân; bật trang cặp đôi khi cả hai đồng ý. | P0 |
| F2 | Bài đăng | Ảnh, đoạn viết, thiệp kỷ niệm; thấy rõ người xem trước khi đăng. | P0 |
| F3 | Kết nối bạn bè | Tên/link mời, chấp nhận kết bạn; không bắt buộc truy cập danh bạ. | P0 |
| F4 | Bảng tin bạn bè | Theo thời gian; ẩn bài và bỏ theo dõi. | P0 |
| F5 | Reaction và bình luận | Cảm xúc, lời chúc; chủ bài được tắt bình luận. | P0 |
| F6 | Duyệt chia sẻ kỷ niệm chung | Chuyển nội dung chung sang bạn bè/công khai cần đối phương đồng ý. | P0 |
| F7 | Lưu ý tưởng từ bài đăng | Lưu buổi hẹn hay vào wishlist của mình. | P1 |
| F8 | Khám phá theo chủ đề | Hẹn hò tiết kiệm, yêu xa, địa điểm theo thành phố; không cần vị trí chính xác. | P2 |
| F9 | Thử thách cộng đồng | Chủ đề ảnh/hoạt động chung, tham gia tự nguyện. | P2 |

Điểm khác biệt cần thử: bài đăng trở thành kế hoạch của cặp đôi khác. Khi cộng đồng còn nhỏ, đếm ngày, kỷ niệm và thiệp chia sẻ ra ngoài vẫn phải tạo giá trị độc lập.

#### G. AI tư vấn và hỗ trợ

| Mã | Feature | Tình huống và kết quả | Ưu tiên |
|---|---|---|---|
| G1 | Lên kế hoạch hẹn hò | Từ ngân sách, thời gian, sở thích → vài phương án với thời lượng/chi phí ước tính. | P0 |
| G2 | Hỗ trợ diễn đạt | Lời cảm ơn, xin lỗi, mở đầu trò chuyện khó; người dùng sửa và tự gửi. | P0 |
| G3 | Tư vấn giao tiếp theo tình huống | Hỏi thêm bối cảnh, tách sự việc–cảm xúc–mong muốn, đề xuất cách trao đổi. | P1 |
| G4 | Trợ lý kế hoạch | Từ yêu cầu kỷ niệm → checklist và lịch nháp để xác nhận. | P1 |
| G5 | Gợi ý quà phù hợp | Dựa trên sở thích được phép dùng, ngân sách và dịp tặng. | P1 |
| G6 | Tìm kỷ niệm bằng ngôn ngữ tự nhiên | Tìm chuyến đi/khoảnh khắc, trả về nội dung có thật trong kho được cấp quyền. | P2 |
| G7 | Tổng kết tuần | Tóm tắt hoạt động và cảm xúc tự khai báo, dẫn dữ liệu gốc, cho sửa sai. | P2 |
| G8 | Hỗ trợ kiểm duyệt | Phát hiện spam/quấy rối/nội dung có thể vi phạm; chuyển trường hợp khó cho quản trị. | P1 |

G1 và G2 được ưu tiên vì phạm vi rõ và dễ đánh giá. G1 ở MVP trả gợi ý để lưu/sao chép; tự tạo lịch/checklist tích hợp thuộc G4 ở P1.

Nguyên tắc:

- Chat AI cá nhân không tự hiển thị cho người yêu.
- Người dùng chọn dữ liệu AI được dùng, xem và xóa thông tin AI ghi nhớ.
- Không tự gửi tin nhắn, đăng bài, đặt lịch hoặc mua quà.
- Không kết luận ngoại tình, chẩn đoán tâm lý hoặc chấm điểm chung thủy.
- Thừa nhận thiếu bối cảnh khi chỉ biết một phía; không ép hòa giải khi có dấu hiệu bạo lực.
- Địa điểm, giá, giờ mở cửa cụ thể cần nguồn cập nhật; tách thông tin xác minh với ước tính.
- Khác biệt hướng tới chất lượng tiếng Việt và khả năng chuyển gợi ý thành hành động hữu ích.

Flamme là ví dụ AI hỗ trợ tình cảm/giao tiếp và nêu rõ giới hạn không phải nhà trị liệu. [Nguồn](https://flamme.app/flammeai)

#### H. Quyền riêng tư và vận hành sản phẩm

| Mã | Feature | Yêu cầu | Ưu tiên |
|---|---|---|---|
| H1 | Khóa và ẩn nội dung | Khóa bằng sinh trắc học; tùy chọn ẩn nội dung thông báo/widget. | P0 |
| H2 | Ngắt ghép đôi | Tự ngắt, thu hồi quyền truy cập chung trên hệ thống, giải thích dữ liệu giữ/xóa; không cần đối phương duyệt. | P0 |
| H3 | Tạm dừng nhắc tình yêu | Tắt kỷ niệm, recap và lời nhắc khi cần khoảng riêng. | P0 |
| H4 | Xuất và xóa dữ liệu | Tải dữ liệu của mình, xóa tài khoản/nội dung theo quyền sở hữu; chính sách rõ với dữ liệu chung. | P0 |
| H5 | Chặn và báo cáo | Báo cáo bài, bình luận, tài khoản và câu trả lời AI; chặn quấy rối. | P0 |
| H6 | Trang quản trị | Xử lý báo cáo, nội dung, khiếu nại và lịch sử xử lý. | P0 |
| H7 | Đồng bộ đáng tin cậy | Trạng thái lưu/tải, retry; tránh mất bản nháp hoặc đăng trùng. | P0 |

Với tính năng P1/P2 như recap, yêu cầu tắt tương ứng được áp dụng khi tính năng đó ra mắt. Việc thu hồi truy cập không thu hồi được bản sao người khác đã tải/chụp trước đó.

Các chức năng báo cáo/chặn và báo cáo nội dung AI cũng liên quan chính sách nền tảng. [Google Play UGC](https://support.google.com/googleplay/android-developer/answer/16313518?hl=en-IN) · [Google Play AI](https://android-developers.googleblog.com/2024/06/enabling-safe-ai-experiences.html)

### 1.3. Cấu trúc màn hình đề xuất

| Mục điều hướng | Nội dung |
|---|---|
| Hôm nay | Đếm ngày, dịp sắp đến, lời quan tâm, một gợi ý hành động |
| Chúng mình | Timeline và, khi triển khai, album/lịch/wishlist |
| Bạn bè | Bảng tin và bài đăng |
| Trợ lý | Ý tưởng hẹn hò, hỗ trợ viết; mở rộng tư vấn ở giai đoạn sau |

Hồ sơ/cài đặt qua avatar. Mỗi màn hình ưu tiên một hành động chính. Không hiển thị mục chức năng chưa triển khai như thể đã dùng được.

### 1.4. Phạm vi MVP và lộ trình

| Phần | Phạm vi ra mắt |
|---|---|
| Tài khoản | Bắt đầu nhanh, dùng cá nhân, ghép đôi, hồ sơ |
| Cốt lõi | Đếm ngày, cột mốc, nhắc ngày, widget, theme, thiệp |
| Không gian đôi | Timeline ảnh/chữ, đóng góp, reaction/bình luận, quyền xem |
| Tương tác | Lời quan tâm nhanh |
| Social nhỏ | Kết bạn, bảng tin thời gian, bài đăng, reaction/bình luận, duyệt chia sẻ |
| AI gọn | Gợi ý hẹn hò và hỗ trợ viết lời nhắn |
| Nền tảng | Khóa/ẩn, tạm dừng nhắc, ngắt ghép đôi, xuất/xóa, báo cáo/chặn, quản trị, đồng bộ |

- **P1:** câu hỏi và check-in, mini game, lịch/wishlist/checklist, yêu xa, album, thư tương lai, AI giao tiếp và trợ lý kế hoạch, hỗ trợ kiểm duyệt.
- **P2:** khám phá cộng đồng, thử thách, recap, ngân sách chung, tìm kỷ niệm bằng AI và tổng kết tuần.

Chưa đưa vào MVP: video ngắn, livestream, chat/gọi đầy đủ, theo dõi vị trí trực tiếp, thú cưng ảo phức tạp và tư vấn tâm lý chuyên sâu. Chưa có quyết định loại vĩnh viễn các ý tưởng này; cần chứng minh nhu cầu trước khi mở rộng.

### 1.5. Kiểm chứng với người dùng

Đề xuất phỏng vấn/thử prototype với khoảng 12–15 người, gồm người chủ động tải và người được mời, thuộc nhóm mới yêu, yêu lâu và yêu xa. Đây là nghiên cứu định tính, không đại diện thống kê.

Các câu hỏi cần kiểm chứng:

- Có giá trị trước khi đối phương tham gia không?
- Có phân biệt được nội dung riêng, chung và công khai không?
- Có muốn lưu kỷ niệm lần thứ hai không?
- AI có tạo kế hoạch/lời nhắn thực sự sử dụng được không?
- Social khuyến khích chia sẻ hay tạo áp lực so sánh?

Chỉ số và cách thu thập được trình bày ở mục 5–6. Không coi lượt dùng app là bằng chứng mối quan hệ được cải thiện.

## 2. Định hướng công nghệ từ chủ sản phẩm

| Hạng mục | Thông tin hiện có | Trạng thái |
|---|---|---|
| Ứng dụng | Dự định dùng KMP; hiểu là Kotlin Multiplatform | Định hướng của chủ sản phẩm |
| Giao diện mobile | Chưa xác định cách tổ chức giao diện và phần logic dùng chung | Chưa chốt |
| Backend | Go (Golang) | Đã được chủ sản phẩm xác nhận |
| Web | Next.js: landing page và trang quản trị | Đã được chủ sản phẩm xác nhận |
| Framework backend, cơ sở dữ liệu và hạ tầng | Chưa xác định | Chưa chốt |
| Trang quản trị | Triển khai bằng Next.js | Đã được chủ sản phẩm xác nhận |
| Nguồn lực | Một người phát triển, có AI hỗ trợ | Đã được chủ sản phẩm xác nhận |
| Thời gian | Khoảng 40 giờ/tuần | Đã được chủ sản phẩm xác nhận |
| Nền tảng app | Android và iOS | Đã được chủ sản phẩm xác nhận |
| Auth | Firebase Anonymous trước; liên kết Google/Apple tự nguyện | Đã được chủ sản phẩm xác nhận |
| Dịch vụ | Ưu tiên Firebase; nền tảng khác chỉ đề xuất khi cần triển khai | Đã được chủ sản phẩm xác nhận |
| Tổ chức mã nguồn | Đề xuất cùng repository cho KMP, Go, Next.js, build/deploy riêng | Xem kế hoạch triển khai |
| Kiến trúc source | Clean Architecture, chia theo feature | Yêu cầu của chủ sản phẩm; chi tiết đề xuất trong tài liệu kiến trúc |

Chủ sản phẩm đã đính chính: backend là Go (Golang). Flutter là thông tin gõ nhầm, không nằm trong định hướng công nghệ đã xác nhận.

Các đề xuất sản phẩm, sự kiện và phân quyền độc lập với framework. Quyết định mới ưu tiên Firebase thay cho danh sách nhiều nhà cung cấp ở bản nghiên cứu ban đầu. Chỉ đề xuất nền tảng bổ sung khi bắt đầu phần việc có nhu cầu cụ thể; kiểm tra tích hợp KMP qua từng nền tảng.

## 3. Phân biệt các loại dữ liệu

| Thành phần | Câu hỏi cần trả lời | Ví dụ |
|---|---|---|
| Dữ liệu nghiệp vụ | Người dùng đã tạo và lưu gì? | Kỷ niệm, ngày yêu, bài đăng, lịch hẹn |
| Product analytics | Người dùng sử dụng tính năng như thế nào? | Tỷ lệ tạo kỷ niệm đầu tiên rồi quay lại |
| Log kỹ thuật | Thao tác lỗi ở đâu, vì sao? | Tải ảnh thất bại do mất kết nối |
| Metrics hệ thống | Hệ thống nhanh và ổn định ra sao? | Tỷ lệ lỗi, độ trễ, chi phí AI |
| Trace | Một thao tác đi qua những bước nào? | Đọc ảnh → gọi AI → lưu recap |
| Audit log | Ai thay đổi điều quan trọng, khi nào? | Đổi quyền xem, ngắt ghép đôi, gỡ bài |

Nội dung tâm sự, ảnh và kỷ niệm thuộc hệ thống dữ liệu sản phẩm. Analytics chỉ nhận sự kiện và thuộc tính cần thiết; không sao chép nội dung riêng tư vào hệ thống đo lường.

OpenTelemetry chuẩn hóa các tín hiệu như logs, metrics và traces, nhưng không phải một hệ thống lưu trữ/hiển thị giám sát hoàn chỉnh. [Nguồn](https://opentelemetry.io/docs/what-is-opentelemetry/)

## 4. Luồng người dùng

### 4.1. Người mới tự tải app

Mở app → khôi phục phiên hoặc tạo Firebase Anonymous → nhập ngày yêu và biệt danh → xem bộ đếm → lưu kỷ niệm → mời người yêu hoặc tiếp tục dùng cá nhân. Liên kết Google/Apple là lựa chọn ở Hồ sơ và các lời gợi ý phù hợp, không phải bước chặn lưu dữ liệu/ghép đôi.

Yêu cầu trải nghiệm:

- Giữ bản nháp và danh tính/dữ liệu hiện tại khi link provider thông thường thành công.
- Nếu lần mở đầu không có mạng, cho dùng bản nháp cục bộ và thông báo chưa đồng bộ; tạo phiên khi có mạng.
- Không tạo Anonymous mới mỗi lần mở. Người dùng đã có tài khoản có đường đăng nhập Google/Apple để quay lại.
- Xin quyền ảnh, thông báo và lịch đúng lúc cần dùng.
- Nội dung tạo trước khi ghép đôi không tự động trở thành nội dung chung.
- Người dùng nhận được giá trị trước khi gặp giới thiệu gói trả phí.
- Không chặn trải nghiệm chỉ vì đối phương chưa cài app.

Đo: tỷ lệ tạo phiên Anonymous thành công, xem bộ đếm, thời gian tới giá trị đầu tiên, lưu kỷ niệm và liên kết provider. Không đếm link như một người dùng mới.

### 4.2. Người được mời

Mở link → xem thông tin lời mời tối thiểu → dùng phiên hiện có hoặc tạo Anonymous → xác nhận đúng người → chấp nhận → chọn thông tin chia sẻ. Có tùy chọn đăng nhập tài khoản đã liên kết; không bắt buộc link Google/Apple để ghép đôi.

| Trường hợp | Cách xử lý |
|---|---|
| Chưa cài app | Hướng dẫn cài, có mã mời dự phòng nếu không giữ được link qua bước cài |
| Link hết hạn/bị thu hồi | Giải thích và hướng dẫn xin lời mời mới |
| Đang ghép đôi với người khác | Không tự thay thế kết nối hiện tại |
| Không muốn chấp nhận | Cho từ chối/bỏ qua và tiếp tục dùng cá nhân |
| Hai người cùng gửi lời mời | Tránh tạo hai không gian chung trùng nhau |

Đo: lời mời được mở, tỷ lệ chấp nhận, thời gian ghép đôi, lỗi và nguyên nhân thất bại.

### 4.3. Sử dụng hằng ngày

Mở app/widget/thông báo → xem Hôm nay → thực hiện một hành động ngắn → nhận phản hồi khi đối phương tương tác.

Hành động: gửi lời quan tâm, lưu ảnh, phản hồi kỷ niệm hoặc xác nhận lịch hẹn.

Màn hình Hôm nay ưu tiên một hành động chính theo hoàn cảnh. Tránh cùng lúc đẩy nhiều tính năng hoặc dùng lời nhắc gây cảm giác tội lỗi.

### 4.4. Lưu kỷ niệm và chia sẻ

Chọn ảnh → viết nội dung → chọn người xem → xem trước → lưu/đăng.

Khi đưa kỷ niệm từ không gian chung ra bạn bè/công khai:

Tạo bản nháp chia sẻ → đối phương duyệt → kiểm tra quyền hiện tại → đăng.

- Sự đồng ý gắn với đúng phiên bản nội dung và phạm vi người xem.
- Thay ảnh hoặc mở rộng phạm vi sau khi duyệt cần duyệt lại.
- Phân biệt rõ “Chỉ mình tôi”, “Hai chúng mình”, “Bạn bè”, “Công khai”.
- Không tự mở rộng quyền xem khi thay đổi trạng thái tài khoản hoặc ghép đôi.

### 4.5. Sử dụng AI

Chọn việc cần giúp → nhập yêu cầu → chọn dữ liệu được phép dùng → nhận gợi ý → sửa → xác nhận hành động.

Ví dụ: yêu cầu kế hoạch kỷ niệm với ngân sách 700.000đ → AI đưa ba phương án → chọn một → tạo lịch nháp → xác nhận.

Trạng thái cần có: đang xử lý, hủy, timeout, thử lại, hoàn tất và báo cáo câu trả lời. AI không tự gửi tin nhắn, đăng bài hoặc thực hiện giao dịch.

### 4.6. Tạm dừng, ngắt ghép đôi và xóa tài khoản

| Lựa chọn | Hành vi |
|---|---|
| Tạm dừng | Tắt nhắc nhở, giữ dữ liệu và kết nối |
| Ngắt ghép đôi | Dừng quyền truy cập chung theo chính sách dữ liệu đã công bố |
| Xóa tài khoản | Xử lý nội dung và dữ liệu liên quan, thông báo tiến độ |

Khi ngắt ghép đôi: hủy lời mời đang chờ, lịch thông báo liên quan; kiểm tra lại quyền của tác vụ đang chạy. Người dùng có thể tự ngắt kết nối, không cần đối phương duyệt.

Chính sách phải giải thích rõ quyền giữ/xóa nội dung cá nhân và nội dung chung. Không hứa thu hồi ảnh mà người khác đã tải xuống hoặc chụp màn hình.

### 4.7. Trạng thái giao diện chung

Mỗi luồng cần thiết kế trạng thái trống, đang tải, thành công, lỗi, mất mạng, không còn quyền truy cập, nội dung đã xóa và thử lại. Không chỉ thiết kế đường đi thành công.

## 5. Hệ thống analytics

### 5.1. Nhóm chỉ số

| Nhóm | Chỉ số | Quyết định hỗ trợ |
|---|---|---|
| Bắt đầu | Hoàn tất thiết lập, kỷ niệm đầu tiên, thời gian tới giá trị | Rút gọn onboarding |
| Ghép đôi | Mở/chấp nhận lời mời, ghép đôi thành công | Cải thiện luồng mời |
| Gắn bó | Retention tuần 1/4, hoạt động có ý nghĩa, dùng đơn/chung | Ưu tiên tính năng |
| Social | Người đăng, nhận phản hồi, lưu ý tưởng thành kế hoạch | Đánh giá giá trị bảng tin |
| AI | Hoàn tất yêu cầu, áp dụng kết quả, hữu ích, chi phí | Đánh giá hiệu quả AI |
| Chất lượng | Lỗi, độ trễ, tắt thông báo, báo cáo | Phát hiện trải nghiệm xuống cấp |

Funnel xác định bước người dùng rời đi. Retention đo quay lại theo sự kiện bắt đầu và sự kiện quay lại đã định nghĩa. [Charts](https://www.amplitude.com/docs/analytics/charts) · [Retention](https://www.amplitude.com/docs/analytics/charts/retention-analysis/retention-analysis-build)

### 5.2. Chỉ số chính đề xuất

**Số cặp đôi có tương tác chung có ý nghĩa mỗi tuần.**

Một cặp đủ điều kiện khi cả hai tham gia ít nhất một hoạt động liên kết trong tuần, chẳng hạn:

- A tạo kỷ niệm, B phản hồi kỷ niệm đó.
- A đề xuất lịch hẹn, B xác nhận.
- Cả hai hoàn thành cùng một câu hỏi.

Hai người chỉ mở app trong tuần chưa đủ điều kiện. Chỉ số này không đại diện cho hạnh phúc hoặc sức khỏe mối quan hệ. Theo dõi người dùng cá nhân chưa ghép đôi bằng bộ chỉ số riêng.

### 5.3. Định nghĩa khởi điểm

| Chỉ số | Định nghĩa đề xuất |
|---|---|
| Kích hoạt cá nhân | Hoàn tất thiết lập và lưu kỷ niệm đầu tiên trong 24 giờ từ lần mở đầu |
| Kích hoạt cặp đôi | Ghép đôi và có tương tác chung đầu tiên trong 7 ngày từ lúc ghép đôi |
| Retention tuần 1 | Tỷ lệ trong nhóm đã kích hoạt có hành động có ý nghĩa vào ngày 7–13 sau kích hoạt |
| AI được sử dụng | Kết quả được lưu thành kế hoạch, sao chép hoặc áp dụng; báo cáo từng loại riêng |
| Social có ích | Bài đăng dẫn tới lưu ý tưởng/tạo kế hoạch, bên cạnh reaction/bình luận |

Đây là giả thuyết đo lường, chưa phải benchmark. Chốt rõ múi giờ báo cáo, cửa sổ thời gian và đơn vị người/cặp cho từng biểu đồ. Chỉ đưa các nhóm đã đủ thời gian quan sát vào phép tính retention tương ứng.

Sao chép lời nhắn không chứng minh người dùng đã gửi hoặc thấy hữu ích. Không lấy thời lượng chat AI hay thời gian lướt bảng tin làm mục tiêu tối ưu duy nhất.

## 6. Tracking plan khởi điểm

### 6.1. Danh mục sự kiện

| Nhóm | Sự kiện tiêu biểu |
|---|---|
| Bắt đầu | `onboarding_started`, `anonymous_account_created`, `counter_setup_completed` |
| Liên kết/đăng nhập | `auth_link_started`, `auth_link_completed`, `auth_link_failed`, `sign_in_completed` |
| Ghép đôi | `invite_created`, `invite_opened`, `pairing_completed`, `pairing_failed` |
| Kỷ niệm | `memory_create_started`, `memory_created`, `memory_create_failed`, `memory_reacted` |
| Social | `share_approval_requested`, `share_approved`, `post_published`, `post_reported` |
| Kế hoạch | `date_plan_created`, `date_plan_accepted`, `date_plan_completed` |
| AI | `ai_request_started`, `ai_response_completed`, `ai_request_failed`, `ai_result_applied`, `ai_feedback_submitted` |
| Thông báo | `notification_permission_updated`, `notification_opened`, `notification_preferences_updated` |
| Vòng đời | `pairing_ended`, `data_export_requested`, `account_deletion_requested` |

Mỗi sự kiện cần chủ sở hữu, mô tả, điều kiện phát sinh, thuộc tính, nguồn ghi, phiên bản và quy tắc riêng tư. `memory_created` chỉ phát sinh khi lưu thành công; bấm nút không đồng nghĩa thành công.

### 6.2. Thuộc tính

| Thuộc tính | Mục đích |
|---|---|
| `event_id` | Loại sự kiện trùng khi gửi lại |
| `occurred_at`, `received_at` | Tách thời điểm xảy ra và nhận dữ liệu |
| `user_id` nội bộ | Nhận biết tài khoản trên nhiều thiết bị |
| `couple_id` khi phù hợp | Phân tích hoạt động chung |
| `app_version`, `platform` | Khoanh vùng theo phiên bản/nền tảng |
| `entry_point` | Widget, thông báo, bảng tin, màn hình chính |
| `result`, `error_code` | Kết quả và nguyên nhân thất bại |
| `schema_version` | Quản lý thay đổi định nghĩa |

Mã nội bộ vẫn có khả năng liên kết, không đồng nghĩa với dữ liệu ẩn danh hoàn toàn.

### 6.3. Quy tắc chất lượng và riêng tư

- Một thao tác thành công có một nguồn ghi nhận chính, tránh đếm hai lần từ app và máy chủ.
- Ghép đôi mới tạo không gian mới; không gán lịch sử cũ sang cặp mới.
- Không đưa tên, caption, ảnh, nội dung chat hoặc yêu cầu AI vào analytics.
- Tách môi trường phát triển, thử nghiệm và sản xuất; loại tài khoản thử khỏi dashboard kinh doanh.
- Kiểm tra đăng nhập lại, đổi thiết bị, offline, gửi lại, đổi tài khoản và ngắt ghép đôi.
- Quản lý lựa chọn thu thập dữ liệu của người dùng và quy trình xóa tại các nhà cung cấp liên quan.
- Đo sai hoặc thiếu do giới hạn thu thập phải được thể hiện; không coi mẫu quan sát là toàn bộ người dùng.

## 7. Log, audit và xử lý sự cố

### 7.1. Log kỹ thuật

Trường cần thiết: thời gian, môi trường, phiên bản, thao tác, kết quả, mã lỗi, thời lượng, mã yêu cầu/trace để liên kết các bước.

Ví dụ: “Tải ảnh thất bại — yêu cầu X — Android — phiên bản 1.2 — mất kết nối — đã thử lại một lần”.

Không ghi mật khẩu, access token, khóa bí mật hoặc nội dung riêng tư. Làm sạch dữ liệu trước khi gửi sang dịch vụ log. [OWASP Logging](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)

### 7.2. Audit log

Ghi nhận các hành động:

- Chấp nhận/ngắt ghép đôi.
- Thay đổi quyền xem và duyệt chia sẻ.
- Xuất/xóa dữ liệu.
- Quản trị viên truy cập hồ sơ hỗ trợ hoặc xử lý báo cáo.
- Thay đổi quyền lợi trả phí khi tính năng được triển khai.

Lưu người thực hiện, hành động, đối tượng bằng mã, trạng thái trước/sau cần thiết và lý do. Hạn chế quyền truy cập, bảo vệ khỏi chỉnh sửa không được phép; không sao chép nguyên nội dung riêng tư vào audit log.

### 7.3. Cảnh báo và phản ứng

| Sự cố | Cách xử lý |
|---|---|
| Ghép đôi thất bại tăng | Kiểm tra bản phát hành và dịch vụ lời mời |
| Tải ảnh lỗi hàng loạt | Giữ bản nháp, báo trạng thái, khôi phục dịch vụ |
| AI chậm/chi phí tăng | Giới hạn lưu lượng, phương án dự phòng hoặc tạm tắt |
| Truy cập sai quyền | Ưu tiên xử lý ngay, hạn chế chức năng bị ảnh hưởng, điều tra |
| Hàng đợi xóa dữ liệu kẹt | Cảnh báo người phụ trách; không báo hoàn tất khi chưa xong |

Mỗi cảnh báo cần người phụ trách và hướng dẫn xử lý. Ngưỡng lỗi thông thường có cửa sổ thời gian và lượng mẫu tối thiểu để tránh báo động nhiễu.

### 7.4. Lưu giữ dữ liệu

Khởi điểm để cân nhắc: log vận hành 14–30 ngày, analytics chi tiết 90 ngày, số liệu tổng hợp lâu hơn. Đây là đề xuất thiết kế, không phải yêu cầu pháp lý hay quyết định cuối cùng.

Audit, thanh toán, sao lưu và dữ liệu cần xóa phải có chính sách riêng theo mục đích, thị trường và nghĩa vụ áp dụng. Kiểm soát quyền truy cập và khả năng xóa ở cả các bên cung cấp dịch vụ.

## 8. Đánh giá và vận hành AI

| Mặt cần đo | Dữ liệu |
|---|---|
| Hoàn thành công việc | Yêu cầu tạo ra kết quả dùng được |
| Hữu ích | Phản hồi, áp dụng kết quả, sửa lại |
| Hiệu năng | Thời gian phản hồi đầu tiên và hoàn tất |
| Chi phí | Theo loại tác vụ, lượt thử lại, kết quả được áp dụng |
| Ổn định | Timeout, lỗi nhà cung cấp, kết quả thiếu cấu trúc |
| Chất lượng tư vấn | Suy diễn, đổ lỗi, phán xét, thiếu bối cảnh |
| Phân quyền | Đúng dữ liệu được phép; ngừng sử dụng sau thu hồi quyền |

Metadata cần có: mô hình, phiên bản hướng dẫn, loại tác vụ, thời lượng và chi phí. Không mặc định lưu nguyên cuộc trò chuyện vào log.

Trước thay đổi lớn, đánh giá bằng bộ tình huống tiếng Việt giả lập: yêu xa, thiếu bối cảnh, mâu thuẫn, mỉa mai, ngân sách hạn chế, dấu hiệu bạo lực. Dùng tiêu chí rõ ràng thay vì chỉ cảm nhận câu trả lời hay.

Lịch sử chat phục vụ người dùng và dữ liệu đánh giá AI quản lý riêng. Chat cá nhân không tự chia sẻ với đối phương. AI không kết luận ngoại tình, chấm điểm chung thủy hoặc chẩn đoán tâm lý từ dữ liệu ứng dụng.

## 9. Thông báo và thử nghiệm

### 9.1. Trung tâm thông báo

- Bật/tắt theo nhóm: kỷ niệm, hoạt động của người yêu, social, gợi ý.
- Giờ yên tĩnh và múi giờ theo từng người.
- Giới hạn tần suất, gộp phản hồi liên tiếp.
- Hủy thông báo hết hiệu lực sau xóa bài/ngắt ghép đôi.
- Mở đúng nội dung; xử lý trường hợp không còn quyền xem.
- Tránh hiện tâm sự nhạy cảm trên màn hình khóa.

Phân biệt: yêu cầu gửi được tiếp nhận → thiết bị nhận → hiển thị → người dùng mở. Không phải nền tảng nào cũng đo được toàn bộ chuỗi. FCM nêu rõ một số chỉ số nhận/hiển thị chỉ có trên Android. [Nguồn](https://firebase.google.com/docs/cloud-messaging/understand-delivery)

### 9.2. Thử nghiệm A/B và rollout

Có thể thử thời điểm mời người yêu, bố cục Hôm nay hoặc cách trình bày gợi ý hẹn hò.

- Tính năng chung nên phân nhóm theo cặp đôi để trải nghiệm hai người tương thích.
- Đo tỷ lệ hoàn thành cùng lỗi, tắt thông báo và phản hồi tiêu cực.
- Khi ít người dùng, ưu tiên phỏng vấn và prototype; không kết luận từ chênh lệch nhỏ.
- Có công tắc tạm tắt tính năng và kế hoạch quay lại cấu hình trước.

## 10. Các thành phần hệ thống và vận hành

P0: cần cho phiên bản ra mắt. P1: bổ sung sau khi kiểm chứng nhu cầu hoặc khi quy mô tăng.

| Thành phần | Trách nhiệm | Ưu tiên |
|---|---|---|
| Tài khoản/phân quyền | Kiểm tra quyền từng ảnh, kỷ niệm, lịch, bài ở máy chủ | P0 |
| Trạng thái cặp đôi | Chưa ghép, chờ, đã ghép, đã ngắt; xử lý đồng thời | P0 |
| Lưu trữ ảnh | Nén, thumbnail, dung lượng, retry, truy cập theo quyền | P0 |
| Đồng bộ/offline | Bản nháp, trạng thái lưu, xung đột chỉnh sửa | P0 |
| Tác vụ nền | Nhắc nhở, xử lý ảnh, xuất/xóa; retry không tạo trùng | P0 |
| Quản trị | Báo cáo, hỗ trợ, tình trạng dịch vụ, phân quyền nhân sự | P0 |
| Quản lý nội dung | Câu hỏi, ý tưởng hẹn hò, theme và phiên bản | P0 nếu có trong MVP |
| Công tắc tính năng | Rollout nhóm nhỏ, tạm tắt khi lỗi | P0 |
| Hỗ trợ/góp ý | Mã chẩn đoán, FAQ, trạng thái xử lý | P0 |
| Sao lưu/phục hồi | Sao lưu và diễn tập khôi phục | P0 |
| Khả năng tiếp cận | Chữ lớn, tương phản, đọc màn hình, giảm chuyển động | P0 |
| Thuê bao | Mua, khôi phục, gia hạn, hết hạn, hoàn tiền | Khi thu phí |
| Tìm kiếm/gợi ý | Tìm kỷ niệm, khám phá, cá nhân hóa | P1 |
| Kho phân tích tập trung | Kết hợp sản phẩm, doanh thu, chi phí | P1 |

Nhân viên không mặc định được xem nhật ký/chat AI. Quyền hỗ trợ giới hạn theo công việc và có audit.

Nếu có gói hai người: chốt chủ thuê bao và quyền lợi sau ngắt ghép đôi. Trạng thái thanh toán phải được xác minh và đồng bộ, không chỉ dựa trên giao diện báo mua thành công.

## 11. Công cụ tham khảo

Hướng hiện tại: ưu tiên Firebase; chưa thêm các nền tảng khác từ bản nghiên cứu cũ. Chọn tích hợp khi đi đến đúng phần việc.

| Nhu cầu | Ứng viên | Ghi chú |
|---|---|---|
| Danh tính | Firebase Authentication | Anonymous, Google, Apple và account linking |
| Funnel/retention | Google Analytics for Firebase | Kiểm chứng cách tổng hợp theo cặp đôi, giữ tracking plan riêng |
| Crash mobile | Firebase Crashlytics | Gom nhóm lỗi, xem ảnh hưởng theo phiên bản |
| Giám sát backend | Chưa chốt nơi lưu log | Giữ thiết kế log/metrics/trace; đề xuất khi triển khai |
| Rollout | Firebase Remote Config | Mở dần và quay lại cấu hình trước |
| Thông báo | Firebase Cloud Messaging | Tích hợp APNs cho iOS |
| Ảnh | Cloud Storage for Firebase | Quyền truy cập và quy trình tải theo nghiệp vụ |
| Dữ liệu | Đánh giá Firestore trước | Chưa chốt database ngoài Firebase |
| Web Next.js | Xem xét Firebase App Hosting | Chọn lúc triển khai; chưa kích hoạt billing |
| Thuê bao/AI/dịch vụ khác | Để đến giai đoạn tương ứng | Chỉ đề xuất thêm khi có nhu cầu rõ |

Nguồn chính thức:

- [Firebase Anonymous và linking](https://firebase.google.com/docs/auth/android/anonymous-auth)
- [Google Analytics for Firebase](https://firebase.google.com/docs/analytics)
- [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics)
- [OpenTelemetry](https://opentelemetry.io/docs/what-is-opentelemetry/)
- [Firebase Remote Config Rollouts](https://firebase.google.com/docs/remote-config/rollouts)
- [Firebase App Hosting cho Next.js](https://firebase.google.com/docs/app-hosting/frameworks-tooling)

Chưa bật session replay hoặc chọn nhà cung cấp cho tính năng này trong MVP. Nếu dùng sau, chỉ ghi các màn hình đã duyệt và kiểm tra che dữ liệu thực tế trên từng nền tảng. Tài liệu [PostHog](https://github.com/PostHog/posthog.com/blob/master/contents/docs/session-replay/index.mdx) là tham khảo nghiên cứu về khả năng masking, không phải quyết định tích hợp.

## 12. Bộ tài liệu cần chốt trước triển khai

| Tài liệu | Nội dung |
|---|---|
| Bản đồ luồng | Đường đi chính, trạng thái trống/tải/lỗi/khôi phục |
| Ma trận quyền | Ai xem/sửa/xóa/chia sẻ từng loại dữ liệu, trước/sau ngắt ghép đôi |
| Tracking plan | Sự kiện, điều kiện, thuộc tính, nguồn ghi, người phụ trách |
| Dashboard | Kích hoạt, gắn bó, social, AI, vận hành |
| Kế hoạch vận hành | Cảnh báo, quản trị, hỗ trợ, sao lưu, xuất/xóa |
| Tiêu chí phát hành | Luồng bắt buộc, kiểm tra phân quyền, tắt/khôi phục khi lỗi |

Ưu tiên chốt luồng người dùng và ma trận quyền dữ liệu trước. Hai phần này quyết định cấu trúc màn hình, ghép đôi, dữ liệu AI được phép sử dụng và phạm vi analytics.

## 13. Các quyết định còn mở

- [x] Chốt định hướng công nghệ: ứng dụng KMP, backend Go (Golang), web Next.js.
- [x] Android và iOS; một người phát triển khoảng 40 giờ/tuần với AI hỗ trợ.
- [x] Firebase Anonymous trước, link Google/Apple khi người dùng muốn.
- [x] Ưu tiên Firebase; chỉ đề xuất dịch vụ ngoài khi cần triển khai.
- [x] Ngày 02/10/2026: UI riêng Android/iOS; KMP chia sẻ logic/data. Đề xuất Android Compose + Navigation 3 và iOS SwiftUI + NavigationStack; Firebase SDK qua adapter nền tảng. Xem [kiến trúc cập nhật](couple-app-clean-architecture.md).
- [ ] Chốt đối tượng, độ tuổi và thị trường.
- [ ] Chốt chính sách sở hữu nội dung chung sau ngắt ghép đôi.
- [ ] Chốt phạm vi social ở bản đầu và nguồn lực kiểm duyệt.
- [ ] Chốt framework Go, lưu trữ, hạ tầng, ngân sách AI và công cụ đo lường.
- [ ] Chốt lựa chọn thu thập dữ liệu, thời gian lưu và quy trình xóa.
- [ ] Chốt mô hình miễn phí/trả phí và quyền lợi gói hai người nếu có.

Các đề xuất trong tài liệu cần được kiểm chứng bằng thử nghiệm người dùng và dữ liệu thực tế; không coi chúng là kết luận thị trường hay cam kết triển khai tất cả trong MVP.

## 14. Kế hoạch triển khai

Xem [Kế hoạch phát triển KMP + Go + Next.js](couple-app-development-plan.md) để biết phạm vi phát hành, đầu việc, phụ thuộc, mốc thời gian, nguồn lực và tiêu chí nghiệm thu. Danh mục 53 feature ở mục 1.2 vẫn là nguồn đối chiếu tính năng; kế hoạch không đồng nghĩa tất cả feature được làm cùng lúc.

Xem [Clean Architecture theo feature](couple-app-clean-architecture.md) để biết cấu trúc source cho từng stack, hướng phụ thuộc, adapter Firebase, cách tổ chức test và kiểm soát kiến trúc.
