# Thiết lập DACS3 trên Codex Cloud

Script [setup-cloud.sh](../scripts/setup-cloud.sh) tái lập môi trường Android đã được kiểm chứng trong phiên onboarding của repository này.
Script dành cho máy Debian 13 x86_64 của Codex Cloud, với checkout ở `/workspace/DACS3`, Java runtime 21, Python 3.11 trở lên, `apt-get`, `dpkg-deb`, `curl` và `unzip` có sẵn.
Các công cụ cần tải được cài vào `/workspace/.toolchains`; cache và cấu hình riêng cho máy nằm ngoài checkout.

Mỗi tác vụ đám mây đã được cô lập. Dùng checkout hiện có; chỉ tạo Git worktree khi người dùng yêu cầu.

## Cài đặt

```bash
cd /workspace/DACS3
bash scripts/setup-cloud.sh
source /workspace/.onboarding/DACS3/env.sh
```

Script chuẩn bị:

- OpenJDK 21 đầy đủ, gồm `javac`.
- Gradle 9.4.1 qua wrapper có sẵn của repository.
- Android SDK 35, Build Tools 34.0.0 và 35.0.0, Platform Tools và Emulator.
- Android command-line tools 20.0 và AVD `dacs3-api35`, Android 35 x86_64.
- Các helper để nạp môi trường, khởi động emulator, kiểm tra readiness và mở ứng dụng.

Script dùng chữ ký package Debian, checksum Android từ metadata chính thức và SHA-256 Gradle đã có trong repository. Việc kiểm tra TLS được giữ nguyên; chứng chỉ proxy được lấy từ máy hiện tại khi cần.
Giá trị proxy có thông tin đăng nhập sẽ bị từ chối ghi vào cấu hình. Không đưa token, khóa riêng, certificate của phiên làm việc hoặc cache vào Git.

Các công cụ Android tạo file xác thực cục bộ trong thư mục home tiêu chuẩn. Nếu sandbox chặn việc tạo `$HOME/.android` hoặc token console, cấp quyền ghi giới hạn cho các đường dẫn đó rồi chạy lại. Giữ nguyên biến `HOME`.

## Nạp môi trường ở mỗi phiên

```bash
cd /workspace/DACS3
source /workspace/.onboarding/DACS3/env.sh
```

`env.sh` chọn JDK và SDK đã cài, dùng cache có thể ghi, và cập nhật proxy cùng chứng chỉ Java từ phiên hiện tại.
Gradle dùng tối đa 3 worker. Dùng `bash gradlew` vì wrapper trong repository chưa có executable bit.
Script thiết lập giữ nguyên mã nguồn ứng dụng, test, khai báo dependency, lockfile và cấu hình Gradle được Git quản lý.

## Build và kiểm tra

Lệnh này cũng được thực thi ở cuối script cài đặt:

```bash
bash gradlew --no-daemon --console=plain \
  :app:assembleDebug \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebugAndroidTest
```

Kết quả nằm tại:

- APK debug: `app/build/outputs/apk/debug/app-debug.apk`.
- Unit-test XML: `app/build/test-results/testDebugUnitTest/`.
- Báo cáo lint: `app/build/reports/lint-results-debug.html`.

## Chạy ứng dụng và test trên Android

Sau khi nạp `env.sh`:

```bash
adb start-server
python3 /workspace/.onboarding/DACS3/start-emulator.py
python3 /workspace/.onboarding/DACS3/wait-emulator.py
python3 /workspace/.onboarding/DACS3/smoke-app.py
ANDROID_SERIAL=emulator-5554 bash gradlew --no-daemon --console=plain \
  :app:connectedDebugAndroidTest
```

Các tiến trình adb, emulator và ứng dụng phải được khởi động lại sau khi khôi phục môi trường. Chỉ khởi động thiết bị khi cần giao diện hoặc connected test; build và unit test không cần emulator.

AVD nằm ở `/workspace/.cache/android/avd`, dùng màn hình 480×800, mật độ 240. Helper kiểm tra tên AVD trước khi thay đổi cấu hình, kiểm tra boot completion, dịch vụ package và PID `system_server` ổn định qua các lần kiểm tra liên tiếp.

Máy onboarding không có KVM nên mô phỏng phần mềm chậm. Trên máy như vậy, helper dùng khả năng `adb root` của SDK test image để đặt `ro.hw_timeout_multiplier=10` trong Android và khởi động lại framework nếu cần. Quyền root chỉ áp dụng bên trong AVD. Watchdog cơ sở giữ mặc định 60 giây; Android áp dụng hệ số timeout dành cho phần cứng chậm. Trên máy có KVM, helper giữ hành vi phần cứng mặc định.

Helper chờ tối đa 20 phút và giữ nguyên các kiểm tra watchdog cùng assertion của test. Giữ lại dữ liệu AVD và tránh `-wipe-data` để tái sử dụng kết quả tối ưu hóa của Android. Chỉ dừng emulator do chính tác vụ này khởi động, sau khi kiểm tra PID và tên AVD.

`smoke-app.py` cài APK, mở `MainActivity`, kiểm tra kết quả launch, tiến trình, foreground activity và crash buffer của ứng dụng. Gradle dọn ứng dụng cùng APK test sau lượt instrumentation; chạy lại `smoke-app.py` khi muốn mở app sau đó.

Java ddmlib dùng liên kết tới token console chuẩn của emulator để nhận đúng AVD. Giá trị token không được sao chép vào script hay repository.

Báo cáo connected test nằm ở `app/build/reports/androidTests/connected/debug/`; XML kết quả nằm ở `app/build/outputs/androidTest-results/connected/debug/`. Log và helper của môi trường nằm ở `/workspace/.onboarding/DACS3/`.

## Kết quả phiên onboarding

Đã kiểm chứng trên mã ứng dụng tại commit `495c1887c39d39d3b9ca6262c6634c0821f95723`:

- Build APK debug và APK instrumentation thành công.
- Unit test: 1 test đạt, 0 thất bại, 0 bị bỏ qua.
- Instrumentation trên Android 35: 1 test đạt, 0 thất bại, 0 bị bỏ qua.
- Mở ứng dụng: `Status: ok`, tiến trình còn chạy và `MainActivity` ở foreground; giao diện được kiểm tra bằng ảnh chụp màn hình.
- APK debug có chữ ký v2 hợp lệ, application ID `com.example.dacs3`, target SDK 35.
- Lint hoàn tất với 0 lỗi chặn build và 226 cảnh báo hiện có, gồm `SecretInSource` trong `ChatAIActivity.kt`.

Các suite có sẵn là test ví dụ. Đăng nhập Firebase, dữ liệu tài khoản, tải ảnh và phản hồi Gemini cần kiểm tra chức năng riêng với quyền truy cập dịch vụ phù hợp.

Việc đẩy các file này lên GitHub lưu hướng dẫn và script trong repository. Để phát hành môi trường Codex Cloud đã chuẩn bị, vẫn cần xem lại, lưu và Publish trong cài đặt môi trường.
