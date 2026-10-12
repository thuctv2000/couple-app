# Mockup UI phase 01

Tạo ngày 08/10/2026 bằng công cụ imagegen tích hợp, theo hướng nền kem/hồng đất, nhật ký ấm áp và offline-first. Đây là concept sáu màn chính, dùng chrome iOS để thống nhất bản trình bày; chưa phải bộ thiết kế riêng hoàn chỉnh cho cả Android và iOS.

[Xem cả bộ](index.html) · [Prompt từng màn](prompts.json)

- [Bắt đầu](01-bat-dau.png)
- [Chọn ngày](02-chon-ngay.png)
- [Home solo](03-home-solo.png)
- [Chỉnh thông tin](04-chinh-thong-tin.png)
- [Tài khoản](05-tai-khoan.png)
- [Đồng bộ](06-dong-bo.png)

## Ghi chú khi chuyển sang UI thật

- Chuẩn hóa typography: ảnh tạo sinh có dùng serif ở bộ đếm/lịch; chốt token font trước khi code.
- Màn chọn ngày có thêm ba chấm tiến trình trong ảnh; bỏ khi triển khai luồng chỉ có một bước thiết lập bắt buộc.
- Home: giảm ưu tiên nút thêm tài khoản so với hành động chính; đổi nội dung khôi phục thành “Khôi phục dữ liệu đã đồng bộ khi đổi máy”.
- Dùng component Apple/Google chính thức khi triển khai đăng nhập; các nút trong ảnh chỉ mô tả vị trí và thứ tự.
- Màn offline có biểu tượng Wi-Fi trên status bar minh họa; UI thật lấy trạng thái từ hệ điều hành. Có Wi-Fi không bảo đảm truy cập được dịch vụ.
- 129 ngày minh họa tương ứng 02/06/2026 tới 08/10/2026, ngày đầu tính là 1.
- Giữ phân biệt local guest, Firebase Anonymous và tài khoản linked. Bộ ảnh thể hiện local guest trước lần sync đầu.
- Các màn lỗi/xung đột, provider sheet, dark mode và biến thể Android chưa nằm trong bộ sáu ảnh này.
