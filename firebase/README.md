# Firebase

Chưa liên kết Firebase project hoặc tài khoản thật. Go đã có adapter Firebase Admin/Firestore cho bộ đếm; mobile Anonymous Auth chưa nối. Xem [hợp đồng sync](../docs/counter-sync-contract.md) và [tiến độ kiểm chứng](../docs/phase-01-progress.md).

- Giữ SDK Firebase Android/Apple trong adapter nền tảng.
- Chốt application ID/bundle ID trước khi đăng ký app thật; `dev.coupleapp.local` là placeholder.
- Không đưa service-account private key, Apple key hoặc signing key vào repo.
- Không tạo quyền admin hoặc bật billing bằng scaffold này.
- Quyết định của chủ dự án 09/10/2026: không tạo thêm project trên tài khoản. Chỉ dùng project cloud có sẵn do chủ dự án chỉ định. `demo-couple-app` chỉ dùng local emulator, không đăng ký project Firebase/Google Cloud.
- `firestore.rules` chặn truy cập trực tiếp từ client, vì mobile đi qua Go. Chưa deploy rules; Admin SDK dùng IAM phía server.
- Go đọc `FIREBASE_PROJECT_ID` và Application Default Credentials. Không có config thì route bộ đếm trả 503; không dùng token giả thay Firebase.

## Kiểm thử local

Chạy từ repo root với Firebase CLI và Java hỗ trợ Firestore Emulator (đã kiểm chứng CLI 15.15.0, Java 25):

```sh
firebase emulators:start --config firebase/firebase.json --project demo-couple-app --only auth,firestore
```

Giữ emulator chạy và mở terminal thứ hai:

```sh
cd backend
FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:19099 \
FIRESTORE_EMULATOR_HOST=127.0.0.1:18081 \
go test -race -count=1 -v ./...
```

Các test tích hợp chỉ cho phép loopback; project dùng cố định `demo-couple-app`. Dữ liệu/tài khoản của từng test được dọn riêng sau test. Không gọi lệnh tạo project hoặc deploy. Port mặc định riêng để tránh dịch vụ đang dùng 8081.

Chạy API nối emulator:

```sh
cd backend
APP_ENV=local FIREBASE_PROJECT_ID=demo-couple-app \
FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:19099 \
FIRESTORE_EMULATOR_HOST=127.0.0.1:18081 \
go run ./cmd/api
```

## Android chạy cùng emulator local

Go API cần `HTTP_ADDR=127.0.0.1:18080` cùng các biến môi trường demo ở trên. Build Android từ thư mục `mobile`:

```sh
./gradlew -Pcouple.emulator=true :androidApp:assembleDebug
```

Android emulator truy cập máy host qua `10.0.2.2`: Auth cổng 19099, Go cổng 18080. Chỉ debug dùng cờ này; release luôn tắt. Không có cờ thì app vẫn lưu offline. Không phải cấu hình dành cho thiết bị thật hoặc cloud production.

Đã kiểm chứng Anonymous, gửi outbox, sửa offline và gửi lại khi mở app có mạng trên Android. Google/Apple và cấu hình project thật chưa hoàn tất.

## iOS Simulator — cấu hình đang kiểm chứng

Project XcodeGen có FirebaseCore/Auth qua SPM 12.19.2. App SwiftUI với adapter Firebase đã build/cài/khởi động trên iOS Simulator ngày 12/10/2026; native persistence tests qua. Anonymous/sync qua các màn iOS chưa có bằng chứng end-to-end. Sau khi resolve package, đặt biến môi trường scheme `COUPLE_FIREBASE_EMULATOR=1` cho Debug Simulator để dùng Auth `127.0.0.1:19099`, Go `127.0.0.1:18080`. Không có biến này thì app local-only; release bỏ qua cấu hình emulator.

Tại lần đối chiếu ngày 11/10/2026, Firebase 13 có cảnh báo chưa có Google Sign-In SPM tương thích trong [release notes](https://firebase.google.com/support/release-notes/ios); vì Google login thuộc phạm vi dự án nên hiện pin 12.19.2. Không thêm Firebase Analytics hay sản phẩm không cần thiết.
