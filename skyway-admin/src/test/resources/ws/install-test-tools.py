"""Install the pinned upstream releases in the disposable test image."""
import gzip
import hashlib
import io
import json
import pathlib
import shutil
import subprocess
import tarfile

base = pathlib.Path('/qa/bin')
base.mkdir()
arch = subprocess.check_output(['dpkg', '--print-architecture'], text=True).strip()
if arch not in ('amd64', 'arm64'):
    raise SystemExit('The transport fixture requires arm64 or amd64')

def download(url):
    return subprocess.check_output(['curl', '-fsSL', '--retry', '3', url])

for repo, version, filename, binary in [
    ('SagerNet/sing-box', '1.14.4', f'sing-box-1.14.4-linux-{arch}.tar.gz', 'sing-box'),
    ('caddyserver/caddy', '2.11.9', f'caddy_2.11.9_linux_{arch}.tar.gz', 'caddy'),
    ('MetaCubeX/mihomo', '1.19.33', f'mihomo-linux-{arch}-v1.19.33.gz', 'mihomo'),
    ('go-acme/lego', '4.26.0', f'lego_v4.26.0_linux_{arch}.tar.gz', 'lego'),
]:
    release = json.loads(download(f'https://api.github.com/repos/{repo}/releases/tags/v{version}'))
    asset = next(item for item in release['assets'] if item['name'] == filename)
    data = download(asset['browser_download_url'])
    if 'sha256:' + hashlib.sha256(data).hexdigest() != asset['digest']:
        raise ValueError(filename + ' digest mismatch')
    path = base / binary
    if binary == 'mihomo':
        path.write_bytes(gzip.decompress(data))
    else:
        with tarfile.open(fileobj=io.BytesIO(data)) as tar, path.open('wb') as out:
            member = next(m for m in tar.getmembers() if m.isfile() and pathlib.Path(m.name).name == binary)
            shutil.copyfileobj(tar.extractfile(member), out)
    path.chmod(0o755)

commit = '6082f1bf70ed4550a5fbd5c9fe147ba5228189d5'
source = download('https://codeload.github.com/233boy/sing-box/tar.gz/' + commit)
with tarfile.open(fileobj=io.BytesIO(source)) as tar:
    for member in tar.getmembers():
        parts = pathlib.PurePosixPath(member.name).parts[1:]
        if not member.isfile() or not parts or '..' in parts:
            continue
        target = pathlib.Path('/etc/sing-box/sh').joinpath(*parts)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(tar.extractfile(member).read())
