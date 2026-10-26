#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"
export ANDROID_HOME="${ANDROID_HOME:-/workspace/toolchains/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_USER_HOME=/workspace/toolchains/android-user
export ANDROID_EMULATOR_HOME="$ANDROID_USER_HOME"
export ANDROID_AVD_HOME=/workspace/toolchains/android-avd
export XDG_CACHE_HOME=/workspace/toolchains/cache
mkdir -p "$ANDROID_USER_HOME" "$ANDROID_AVD_HOME" "$XDG_CACHE_HOME"
if [[ ! -f "$ANDROID_AVD_HOME/whistle-api30.avd/config.ini" ]]; then
  printf 'no\n' | python3 scripts/cloud-java.py \
    "$ANDROID_HOME/cmdline-tools/19.0/bin/avdmanager" create avd \
    --name whistle-api30 --package 'system-images;android-30;default;x86_64' --device pixel_2
fi
if ! "$ANDROID_HOME/platform-tools/adb" -s emulator-5554 get-state >/dev/null 2>&1; then
  nohup "$ANDROID_HOME/emulator/emulator" @whistle-api30 \
    -port 5554 -no-window -no-audio -no-boot-anim -no-snapshot \
    -gpu swiftshader_indirect -accel off -cores 2 -memory 2048 \
    -camera-back none -camera-front none \
    > /workspace/toolchains/emulator.log 2>&1 < /dev/null &
fi
python3 - <<'PY'
import os, subprocess, time
adb = os.environ['ANDROID_HOME'] + '/platform-tools/adb'
deadline = time.monotonic() + 600
while time.monotonic() < deadline:
    result = subprocess.run([adb, '-s', 'emulator-5554', 'shell', 'getprop', 'sys.boot_completed'],
                            capture_output=True, text=True, timeout=10)
    if result.returncode == 0 and result.stdout.strip() == '1':
        subprocess.run([adb, '-s', 'emulator-5554', 'shell', 'input', 'keyevent', '82'], check=True)
        print('Android API 30 emulator booted and ready.')
        break
    time.sleep(3)
else:
    raise SystemExit('Emulator not ready after 600s. Inspect /workspace/toolchains/emulator.log.')
PY
