# Quy tắc source ban đầu

1. Chia theo feature. KMP domain không import Compose, Swift, Firebase hoặc HTTP DTO.
2. Logic ngày dùng ngày lịch và múi giờ được truyền tường minh; ngày đầu bằng 1. `CountLoveDaysAtInstant` chuyển instant thành ngày theo múi giờ; domain không đọc đồng hồ hệ thống.
3. `LoveCounterFacade` là biên của host: đọc đồng hồ, kiểm tra chuỗi đầu vào, gọi domain, trả primitive/result ổn định cho Swift. Swift không viết lại thuật toán ngày.
4. Hai UI và back stack riêng; chưa có data source nên không thêm repository/factory vô nghĩa. Local state hiện chỉ phục vụ thao tác đồng bộ rất nhỏ; thêm ViewModel khi có auth/API và lifecycle bất đồng bộ.
5. Bộ đếm hiện tính khi nhấn nút. Tự cập nhật lúc qua nửa đêm, persistence, date picker Android và đầy đủ accessibility nằm ở bước hoàn thiện feature, không được coi đã có.
6. Go hiện có HTTP adapter và entry point. Khi có nghiệp vụ, thêm `internal/<feature>/{domain,application,adapter}`; interface ở phía sử dụng.
7. Next.js giữ landing tĩnh đơn giản. Admin sẽ dùng feature directories và server-only adapter khi được triển khai; không chứa cơ sở dữ liệu nghiệp vụ thứ hai.
8. Các file Firebase/signing local bị bỏ qua bởi Git; không có secret thật trong scaffold.

## Nguồn kiểm tra phiên bản ngày 05/10/2026

- [Kotlin releases](https://kotlinlang.org/docs/releases.html), [KMP compatibility](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html).
- [Android KMP plugin](https://developer.android.com/kotlin/multiplatform/plugin), [Navigation 3](https://developer.android.com/jetpack/androidx/releases/navigation3).
- Artifact metadata của Maven Central/Google Maven được kiểm tra trước khi ghi version; npm registry dùng cho Next/React.
- AGP chọn 9.2.1 tương thích Gradle 9.4.1 hiện có; AGP 9.3 cần Gradle 9.5 trở lên. Kotlin 2.4.20 và Navigation 3 1.2.0 giữ theo yêu cầu bản stable mới nhất. [AGP 9.2 compatibility](https://developer.android.com/build/releases/agp-9-2-0-release-notes)
- Xcode hiện có trên máy là 27.0; bảng tương thích Kotlin tham chiếu 26.4. Chỉ ghi thành công sau khi thực sự build framework/app, không suy ra từ sự hiện diện của Xcode.
