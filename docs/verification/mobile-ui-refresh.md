# Mobile UI refresh — 13/10/2026

Mục tiêu: giảm cảm giác rời rạc và kích thước control nhỏ trên mobile, dựa trên concept Phase 01 đã lưu trong repo.

## Thay đổi

- Welcome, Home và Account trên Compose/SwiftUI dùng nền kem, màu rose/sage và nhóm nội dung trong card.
- Home có card bộ đếm nổi bật, số ngày lớn hơn, trạng thái lưu local thành một hàng tương tác rõ ràng.
- Hành động chính và phụ có chiều rộng đầy đủ; kích thước chữ nút tăng và vùng chạm được mở rộng.
- Account gom thông tin tài khoản/dữ liệu và lựa chọn chẩn đoán thành hai nhóm; lời giải thích về crash đã lưu cục bộ được giữ cạnh công tắc.
- Editor và Sync dùng nền/chiều cao hàng phù hợp với các màn chính; Android dùng chung component nút.

## Kiểm chứng

- Android Debug APK build thành công sau đợt chỉnh bố cục và component.
- Swift source parse thành công bằng compiler Xcode 27 sau chỉnh sửa.
- `git diff --check` qua.
- Chưa kiểm chứng hình ảnh thực tế của bản UI mới trên thiết bị, chữ lớn hoặc dark mode.
- Full iOS build bị chặn bởi quyền ghi SwiftPM cache và dịch vụ CoreSimulator của sandbox. Kết quả build iOS trước đợt UI này không chứng minh bản UI mới build thành công.

Đợt này là thay đổi UI local, chưa coi là nghiệm thu accessibility hoặc beta.
