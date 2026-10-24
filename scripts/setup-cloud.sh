#!/usr/bin/env bash
set -euo pipefail
cd /workspace/DACS3
onboarding_dir=/workspace/.onboarding/DACS3
toolchain_dir=/workspace/.toolchains
jdk_dir="$toolchain_dir/java-root/usr/lib/jvm/java-21-openjdk-amd64"
mkdir -p "$onboarding_dir" "$toolchain_dir" /workspace/.cache/gradle "$HOME/.android"

# The emulator normally creates this local token itself. Never print or save its value.
python3 - <<'CONSOLE_AUTH'
from pathlib import Path
import os
import secrets
token = Path.home() / '.emulator_console_auth_token'
if not token.exists():
    descriptor = os.open(token, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(descriptor, 'w') as output:
        output.write(secrets.token_hex(16) + '\n')
CONSOLE_AUTH

if [ ! -x "$jdk_dir/bin/javac" ]; then
  mkdir -p "$onboarding_dir/apt.conf.d" "$onboarding_dir/apt.sources.d" \
    /workspace/.cache/apt/lists/partial /workspace/.cache/apt/archives/partial /workspace/.cache/apt/debs
  cat > "$onboarding_dir/debian.sources.list" <<'SOURCES'
deb [signed-by=/usr/share/keyrings/debian-archive-keyring.gpg] https://deb.debian.org/debian trixie main
deb [signed-by=/usr/share/keyrings/debian-archive-keyring.gpg] https://security.debian.org/debian-security trixie-security main
SOURCES
  cat > "$onboarding_dir/apt.conf" <<'APTCONF'
Dir::Etc::main "/dev/null";
Dir::Etc::parts "/workspace/.onboarding/DACS3/apt.conf.d";
Dir::Etc::sourcelist "/workspace/.onboarding/DACS3/debian.sources.list";
Dir::Etc::sourceparts "/workspace/.onboarding/DACS3/apt.sources.d";
Dir::State::lists "/workspace/.cache/apt/lists";
Dir::Cache "/workspace/.cache/apt";
Debug::NoLocking "true";
APTCONF
  APT_CONFIG="$onboarding_dir/apt.conf" apt-get update
  (
    jdk_download_dir=$(mktemp -d /workspace/.cache/apt/jdk.XXXXXX)
    cd "$jdk_download_dir"
    APT_CONFIG="$onboarding_dir/apt.conf" apt-get download openjdk-21-jdk-headless openjdk-21-jre-headless
    for package in openjdk-21-*.deb; do
      dpkg-deb --extract "$package" "$toolchain_dir/java-root"
    done
  )
fi

# Use the stable Java-based CLI; CLI 21's native launcher requires extra home-directory writes.
python3 - <<'ANDROID_TOOLS'
from pathlib import Path
import hashlib
import shutil
import subprocess
import xml.etree.ElementTree as ET

sdk = Path('/workspace/.toolchains/android-sdk')
base = Path('/workspace/.onboarding/DACS3')
if not (sdk / 'cmdline-tools/20.0/bin/sdkmanager').exists():
    metadata = base / 'android-repository.xml'
    subprocess.run(['curl', '--fail', '--silent', '--show-error', '--location', '--retry', '2',
                    '--max-time', '90', 'https://dl.google.com/android/repository/repository2-1.xml',
                    '-o', str(metadata)], check=True)
    package = next(p for p in ET.parse(metadata).getroot().findall('remotePackage')
                   if p.attrib['path'] == 'cmdline-tools;20.0')
    archive = next(a for a in package.findall('archives/archive') if a.findtext('host-os') == 'linux')
    url = archive.findtext('complete/url')
    expected = archive.findtext('complete/checksum')
    download = Path('/workspace/.cache') / url
    if not download.exists():
        subprocess.run(['curl', '--fail', '--silent', '--show-error', '--location', '--retry', '2',
                        '--max-time', '240', 'https://dl.google.com/android/repository/' + url,
                        '-o', str(download)], check=True)
    with download.open('rb') as handle:
        actual = hashlib.file_digest(handle, 'sha1').hexdigest()
    if actual != expected:
        raise RuntimeError('Official Android command-line tools checksum mismatch')
    unpacked = base / 'android-tools-20-unpacked'
    unpacked.mkdir(exist_ok=True)
    subprocess.run(['unzip', '-q', str(download), '-d', str(unpacked)], check=True)
    (sdk / 'cmdline-tools').mkdir(parents=True, exist_ok=True)
    shutil.move(str(unpacked / 'cmdline-tools'), str(sdk / 'cmdline-tools/20.0'))
ANDROID_TOOLS

# The complete configuration helpers are inserted here when the script is saved.
cat > "$onboarding_dir/configure.py" <<'DACS3_CONFIGURE_PY'
"""Refresh this instance's Java TLS/proxy configuration without storing secrets."""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
from urllib.parse import urlparse

base = Path('/workspace/.onboarding/DACS3')
gradle = Path('/workspace/.cache/gradle')
gradle.mkdir(parents=True, exist_ok=True)
for directory in ('android', 'android/avd', 'java-home'):
    Path('/workspace/.cache', directory).mkdir(parents=True, exist_ok=True)
# Native emulator and Java ddmlib must refer to the same local console identity.
native_token = Path.home() / '.emulator_console_auth_token'
java_token = Path('/workspace/.cache/java-home/.emulator_console_auth_token')
if native_token.is_file() and not os.path.lexists(java_token):
    java_token.symlink_to(native_token)
system_ca = Path('/etc/ssl/certs/java/cacerts')
proxy_ca = os.environ.get('CODEX_PROXY_CERT')
cert = Path(proxy_ca) if proxy_ca and Path(proxy_ca).is_file() else None
digest = hashlib.sha256(system_ca.read_bytes() + (cert.read_bytes() if cert else b'')).hexdigest()
trust = base / 'cacerts'
stamp = base / 'cacerts.source.sha256'
if not trust.exists() or not stamp.exists() or stamp.read_text().strip() != digest:
    shutil.copyfile(system_ca, trust)
    if cert:
        subprocess.run([
            '/usr/lib/jvm/java-21-openjdk-amd64/bin/keytool', '-importcert', '-noprompt',
            '-alias', 'codex-environment-proxy', '-file', str(cert),
            '-keystore', str(trust), '-storepass', 'changeit',
        ], check=True, stdout=subprocess.DEVNULL)
    stamp.write_text(digest + '\n')

managed = {
    'systemProp.javax.net.ssl.trustStore': str(trust),
    'systemProp.http.nonProxyHosts': 'localhost|127.*|[::1]',
}
for protocol in ('http', 'https'):
    value = os.environ.get(protocol.upper() + '_PROXY') or os.environ.get(protocol + '_proxy')
    if value:
        proxy = urlparse(value)
        if proxy.username or proxy.password:
            raise RuntimeError('Authenticated proxy URLs must not be persisted in setup files')
        managed['systemProp.' + protocol + '.proxyHost'] = proxy.hostname
        managed['systemProp.' + protocol + '.proxyPort'] = str(proxy.port or 80)
properties = gradle / 'gradle.properties'
old = properties.read_text().splitlines() if properties.exists() else []
keys = set(managed) | {
    'systemProp.http.proxyHost', 'systemProp.http.proxyPort',
    'systemProp.https.proxyHost', 'systemProp.https.proxyPort',
}
kept = [line for line in old if line.split('=', 1)[0].strip() not in keys]
if not any(line.startswith('org.gradle.workers.max=') for line in kept):
    kept.append('org.gradle.workers.max=3')
properties.write_text('\n'.join(kept + [key + '=' + value for key, value in managed.items()]) + '\n')
DACS3_CONFIGURE_PY
cat > "$onboarding_dir/env.sh" <<'DACS3_ENV_SH'
# Source this file in each task before Gradle or Android commands.
export JAVA_HOME=/workspace/.toolchains/java-root/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/workspace/.toolchains/android-sdk
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_USER_HOME=/workspace/.cache/android
export ANDROID_EMULATOR_HOME="$ANDROID_USER_HOME"
export ANDROID_AVD_HOME="$ANDROID_USER_HOME/avd"
export GRADLE_USER_HOME=/workspace/.cache/gradle
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/20.0/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
case " ${JAVA_TOOL_OPTIONS:-} " in
  *' -Duser.home=/workspace/.cache/java-home '*) ;;
  *) export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }-Duser.home=/workspace/.cache/java-home" ;;
esac
python3 /workspace/.onboarding/DACS3/configure.py
DACS3_ENV_SH
cat > "$onboarding_dir/start-emulator.py" <<'DACS3_START_EMULATOR_PY'
"""Start only the onboarding AVD; run after sourcing env.sh."""
import os
from pathlib import Path
import subprocess

base = Path('/workspace/.onboarding/DACS3')
serial = 'emulator-5554'
devices = subprocess.run(['adb', 'devices'], check=True, capture_output=True, text=True).stdout
if any(line.split() and line.split()[0] == serial for line in devices.splitlines()[1:]):
    print(serial, 'already exists; reuse it only after checking its AVD name.')
    raise SystemExit(0)
pid_file = base / 'emulator.pid'
if pid_file.exists():
    pid = pid_file.read_text().strip()
    command = Path('/proc', pid, 'cmdline')
    if command.exists() and b'dacs3-api35' in command.read_bytes():
        print('The onboarding emulator is already starting; PID', pid)
        raise SystemExit(0)
acceleration = 'on' if os.access('/dev/kvm', os.R_OK | os.W_OK) else 'off'
with (base / 'emulator.log').open('ab') as log:
    process = subprocess.Popen([
        'emulator', '-avd', 'dacs3-api35', '-no-window', '-no-audio', '-no-boot-anim',
        '-gpu', 'swiftshader', '-accel', acceleration, '-memory', '2048', '-cores', '2',
        '-no-snapshot', '-port', '5554', '-skin', '480x800',
    ], stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
pid_file.write_text(str(process.pid) + '\n')
print('Started dacs3-api35 with', acceleration, 'acceleration; PID', process.pid)
DACS3_START_EMULATOR_PY
cat > "$onboarding_dir/wait-emulator.py" <<'DACS3_WAIT_EMULATOR_PY'
"""Check boot completion and Android's package service, rather than only an open port."""
import subprocess
import time
import os

deadline = time.monotonic() + 1200
previous_pid = None
watchdog_configured = False
hardware_configured = os.access('/dev/kvm', os.R_OK | os.W_OK)
watchdog_timeout = '60000'
while time.monotonic() < deadline:
    try:
        avd = subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'getprop', 'ro.boot.qemu.avd_name'],
                             capture_output=True, text=True, timeout=10)
        if avd.returncode or not avd.stdout.strip():
            time.sleep(3)
            continue
        if avd.stdout.strip() != 'dacs3-api35':
            raise RuntimeError('Port 5554 belongs to another AVD: ' + avd.stdout.strip())
        if not hardware_configured:
            multiplier = subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'getprop',
                                         'ro.hw_timeout_multiplier'],
                                        check=True, capture_output=True, text=True, timeout=15)
            if multiplier.stdout.strip() != '10':
                # This is a debuggable SDK test image. Root applies only inside this AVD.
                subprocess.run(['adb', '-s', 'emulator-5554', 'root'],
                               check=True, capture_output=True, text=True, timeout=30)
                subprocess.run(['adb', '-s', 'emulator-5554', 'wait-for-device'],
                               check=True, capture_output=True, text=True, timeout=30)
                subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'setprop',
                                'ro.hw_timeout_multiplier', '10'],
                               check=True, capture_output=True, text=True, timeout=30)
                effective = subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'getprop',
                                            'ro.hw_timeout_multiplier'],
                                           check=True, capture_output=True, text=True, timeout=15)
                if effective.stdout.strip() != '10':
                    raise RuntimeError('The SDK test image did not accept its software-emulator timeout setting')
                zygote = subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'pidof', 'zygote64'],
                                        capture_output=True, text=True, timeout=15)
                if zygote.stdout.strip():
                    subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'setprop', 'sys.boot_completed', '0'],
                                   check=True, capture_output=True, text=True, timeout=30)
                    subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'stop'],
                                   check=True, capture_output=True, text=True, timeout=30)
                    subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'start'],
                                   check=True, capture_output=True, text=True, timeout=30)
            hardware_configured = True
            print('Android software-emulator timeout multiplier: 10', flush=True)
        if not watchdog_configured:
            setting = subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'settings', 'get',
                                      'global', 'watchdog_timeout_millis'],
                                     capture_output=True, text=True, timeout=15)
            if setting.stdout.strip() != watchdog_timeout:
                subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'settings', 'put',
                                'global', 'watchdog_timeout_millis', watchdog_timeout],
                               capture_output=True, text=True, timeout=15)
                time.sleep(3)
                continue
            watchdog_configured = True
            print('Configured Android framework watchdog timeout for this host:', watchdog_timeout, flush=True)
        boot = subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'getprop', 'sys.boot_completed'],
                              capture_output=True, text=True, timeout=10)
        if boot.returncode == 0 and boot.stdout.strip() == '1':
            package = subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'pm', 'path', 'android'],
                                     capture_output=True, text=True, timeout=15)
            if package.returncode == 0 and 'package:' in package.stdout:
                server = subprocess.run(['adb', '-s', 'emulator-5554', 'shell', 'pidof', 'system_server'],
                                        check=True, capture_output=True, text=True, timeout=10).stdout.strip()
                if not server or server != previous_pid:
                    previous_pid = server
                    time.sleep(5)
                    continue
                print('dacs3-api35: boot completed and Android package service is ready')
                raise SystemExit(0)
    except subprocess.TimeoutExpired:
        pass
    time.sleep(3)
raise SystemExit('Android readiness timed out; inspect the onboarding emulator log and process state')
DACS3_WAIT_EMULATOR_PY
cat > "$onboarding_dir/smoke-app.py" <<'DACS3_SMOKE_APP_PY'
"""Install the development APK and verify the application stays in the foreground."""
from pathlib import Path
import subprocess
import time
import sys

apk = Path('/workspace/DACS3/app/build/outputs/apk/debug/app-debug.apk')
if not apk.exists():
    raise SystemExit('Build :app:assembleDebug first')
adb = ['adb', '-s', 'emulator-5554']
if '--skip-install' not in sys.argv[1:]:
    subprocess.run(adb + ['install', '-r', str(apk)], check=True, timeout=240)
subprocess.run(adb + ['shell', 'am', 'force-stop', 'com.example.dacs3'], check=True, timeout=30)
subprocess.run(adb + ['logcat', '-c'], check=True, timeout=30)
subprocess.run(adb + ['shell', 'wm', 'dismiss-keyguard'], check=True, timeout=120)
launch = subprocess.run(adb + ['shell', 'am', 'start', '-W', '-n', 'com.example.dacs3/.MainActivity'],
                        check=True, capture_output=True, text=True, timeout=240)
print(launch.stdout)
if 'Status: ok' not in launch.stdout:
    raise SystemExit('Android did not confirm a successful activity launch')
time.sleep(5)
pid = subprocess.run(adb + ['shell', 'pidof', 'com.example.dacs3'],
                     capture_output=True, text=True, timeout=30)
activity = subprocess.run(adb + ['shell', 'dumpsys', 'activity', 'activities'],
                          check=True, capture_output=True, text=True, timeout=60)
resumed = [line.strip() for line in activity.stdout.splitlines()
           if ('mResumedActivity' in line or 'topResumedActivity' in line)
           and 'com.example.dacs3/' in line]
crash = subprocess.run(adb + ['logcat', '-b', 'crash', '-d'],
                       check=True, capture_output=True, text=True, timeout=30)
if pid.returncode or not resumed or ('Process: com.example.dacs3' in crash.stdout):
    raise SystemExit('Application startup failed; inspect this emulator crash buffer locally')
print('Application process is alive and foreground activity is:', '\n'.join(resumed))
DACS3_SMOKE_APP_PY


source "$onboarding_dir/env.sh"
python3 - <<'ANDROID_PACKAGES'
from pathlib import Path
import os
import subprocess
from urllib.parse import urlparse

sdk = Path(os.environ['ANDROID_HOME'])
packages = {
    'platform-tools': sdk / 'platform-tools/adb',
    'platforms;android-35': sdk / 'platforms/android-35/android.jar',
    'build-tools;34.0.0': sdk / 'build-tools/34.0.0/aapt2',
    'build-tools;35.0.0': sdk / 'build-tools/35.0.0/aapt2',
    'emulator': sdk / 'emulator/emulator',
    'system-images;android-35;default;x86_64': sdk / 'system-images/android-35/default/x86_64/system.img',
}
missing = [name for name, path in packages.items() if not path.exists()]
if missing:
    args = [str(sdk / 'cmdline-tools/20.0/bin/sdkmanager'), '--sdk_root=' + str(sdk)]
    value = os.environ.get('HTTPS_PROXY') or os.environ.get('https_proxy')
    if value:
        proxy = urlparse(value)
        args += ['--proxy=http', '--proxy_host=' + proxy.hostname, '--proxy_port=' + str(proxy.port or 80)]
    environment = os.environ.copy()
    environment['JAVA_TOOL_OPTIONS'] += ' -Djavax.net.ssl.trustStore=/workspace/.onboarding/DACS3/cacerts'
    log_path = Path('/workspace/.onboarding/DACS3/android-sdk-install.log')
    with log_path.open('w') as log:
        result = subprocess.run(args + missing, input='y\n' * 100, text=True,
                                env=environment, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode:
        print('Android SDK installation failed; inspect', log_path)
        raise SystemExit(result.returncode)
    if any(not path.exists() for path in packages.values()):
        raise RuntimeError('Android SDK installation returned without required packages')
ANDROID_PACKAGES

if [ ! -f "$ANDROID_AVD_HOME/dacs3-api35.ini" ]; then
  printf 'no\n' | avdmanager create avd --name dacs3-api35 \
    --package 'system-images;android-35;default;x86_64' --device pixel_2
fi
python3 - <<'AVD_DISPLAY'
from pathlib import Path
import os
config = Path(os.environ['ANDROID_AVD_HOME']) / 'dacs3-api35.avd/config.ini'
display = {'hw.lcd.width': '480', 'hw.lcd.height': '800', 'hw.lcd.density': '240'}
lines = [line for line in config.read_text().splitlines() if line.split('=', 1)[0].strip() not in display]
config.write_text('\n'.join(lines + [key + '=' + value for key, value in display.items()]) + '\n')
AVD_DISPLAY
javac -version
bash gradlew --version
adb version
sdkmanager --sdk_root="$ANDROID_HOME" --list_installed
bash gradlew --no-daemon --console=plain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
echo 'Toolchain, dependency installation, APK builds, unit tests and lint are complete.'
