#!/usr/bin/env python3
"""Run Java tools through the cloud's existing proxy with certificate verification intact."""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys
import urllib.parse

root = Path('/workspace/toolchains')
root.mkdir(parents=True, exist_ok=True)
environment = os.environ.copy()
jdk = root / 'jdk-21.0.13+9'
if jdk.is_dir() and not environment.get('JAVA_HOME'):
    environment['JAVA_HOME'] = str(jdk)
    environment['PATH'] = str(jdk / 'bin') + os.pathsep + environment.get('PATH', '')
environment.setdefault('ANDROID_USER_HOME', str(root / 'android-user'))
environment.setdefault('ANDROID_EMULATOR_HOME', str(root / 'android-user'))
Path(environment['ANDROID_USER_HOME']).mkdir(parents=True, exist_ok=True)
# This cloud uses software emulation; ddmlib's 2-second property timeout is too short.
options = ['-Dddmlib.getprop.timeout.sec=30']
proxy = environment.get('HTTPS_PROXY') or environment.get('HTTP_PROXY')
if proxy:
    parsed = urllib.parse.urlparse(proxy)
    if parsed.username or parsed.password:
        raise SystemExit('Authenticated proxy needs platform-supported Java configuration.')
    for protocol in ('http', 'https'):
        options.extend([f'-D{protocol}.proxyHost={parsed.hostname}',
                        f'-D{protocol}.proxyPort={parsed.port or 80}'])
    options.append('-Dhttp.nonProxyHosts=localhost|127.*|[::1]')

base = Path('/etc/ssl/certs/java/cacerts')
certificates = sorted(Path('/usr/local/share/ca-certificates').glob('*.crt'))
if base.exists() and certificates:
    digest = hashlib.sha256(base.read_bytes() + b''.join(p.read_bytes() for p in certificates)).hexdigest()
    truststore = root / 'java-cacerts'
    stamp = root / 'java-cacerts.sha256'
    if not truststore.exists() or not stamp.exists() or stamp.read_text() != digest:
        shutil.copyfile(base, truststore)
        for index, certificate in enumerate(certificates):
            subprocess.run(['keytool', '-importcert', '-noprompt', '-alias', f'cloud-local-{index}',
                            '-file', str(certificate), '-keystore', str(truststore),
                            '-storepass', 'changeit'], check=True, capture_output=True)
        stamp.write_text(digest)
    options.append(f'-Djavax.net.ssl.trustStore={truststore}')

environment['JAVA_TOOL_OPTIONS'] = ' '.join(filter(None, [environment.get('JAVA_TOOL_OPTIONS'), *options]))
if len(sys.argv) < 2:
    raise SystemExit('Usage: cloud-java.py command [arguments ...]')
raise SystemExit(subprocess.call(sys.argv[1:], env=environment))
