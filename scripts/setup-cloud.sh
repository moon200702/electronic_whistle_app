#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"
command -v java >/dev/null
command -v python3 >/dev/null
command -v unzip >/dev/null
export ANDROID_HOME="${ANDROID_HOME:-/workspace/toolchains/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
mkdir -p /workspace/toolchains/downloads "$ANDROID_HOME/cmdline-tools"
if [[ ! -x /workspace/toolchains/jdk-21.0.13+9/bin/javac ]]; then
  jdk_base=https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9
  jdk_file=OpenJDK21U-jdk_x64_linux_hotspot_21.0.13_9.tar.gz
  curl --fail --location --silent --show-error --retry 2 "$jdk_base/$jdk_file" \
    -o /workspace/toolchains/downloads/jdk.tar.gz
  curl --fail --location --silent --show-error --retry 2 "$jdk_base/$jdk_file.sha256.txt" \
    -o /workspace/toolchains/downloads/jdk.sha256
  python3 - <<'PY'
import hashlib
from pathlib import Path
root = Path('/workspace/toolchains/downloads')
expected = (root / 'jdk.sha256').read_text().split()[0]
actual = hashlib.sha256((root / 'jdk.tar.gz').read_bytes()).hexdigest()
if actual != expected:
    raise SystemExit('JDK SHA-256 verification failed')
print('JDK official SHA-256 verified')
PY
  tar -xzf /workspace/toolchains/downloads/jdk.tar.gz -C /workspace/toolchains
fi
tools_zip=/workspace/toolchains/downloads/android-tools.zip
if [[ ! -x "$ANDROID_HOME/cmdline-tools/19.0/bin/sdkmanager" ]]; then
  curl --fail --location --silent --show-error --retry 2 \
    https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip -o "$tools_zip"
  # Google's official repository2-1.xml supplies this archive's SHA-1 checksum.
  printf '%s  %s\n' 5fdcc763663eefb86a5b8879697aa6088b041e70 "$tools_zip" | sha1sum --check
  staging_dir="$(mktemp -d /workspace/toolchains/downloads/android-tools.XXXXXX)"
  unzip -q "$tools_zip" -d "$staging_dir"
  mv "$staging_dir/cmdline-tools" "$ANDROID_HOME/cmdline-tools/19.0"
  rmdir "$staging_dir"
fi
python3 - "$@" <<'PY'
import os
import subprocess
import sys
sdk = os.environ['ANDROID_HOME']
packages = ['platform-tools', 'platforms;android-35', 'build-tools;35.0.0']
if '--with-emulator' in sys.argv:
    packages.extend(['emulator', 'system-images;android-30;default;x86_64'])
result = subprocess.run(['python3', 'scripts/cloud-java.py',
    sdk + '/cmdline-tools/19.0/bin/sdkmanager', '--sdk_root=' + sdk, *packages],
    input='y\n' * 100, text=True)
raise SystemExit(result.returncode)
PY
bash scripts/gradle-cloud.sh --no-daemon assembleDebug testDebugUnitTest lintDebug
