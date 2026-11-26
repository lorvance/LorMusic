#!/usr/bin/env python3
"""Build a type-2 AppImage with Java and the VLC audio stack. Host glibc stays external."""
import argparse, pathlib, shutil, subprocess, os, re, json
p=argparse.ArgumentParser();p.add_argument('--tool',type=pathlib.Path,required=True);p.add_argument('--runtime',type=pathlib.Path);p.add_argument('--output',type=pathlib.Path,default=pathlib.Path('dist'));a=p.parse_args()
root=pathlib.Path(__file__).resolve().parent.parent;image=root/'build/compose/binaries/main/app/lormusic';assert(image/'bin/lormusic').is_file(),'Run createDistributable first'
appdir=root/'build/LorMusic.AppDir';shutil.rmtree(appdir,ignore_errors=True);shutil.copytree(image,appdir/'opt/lormusic')
def write(name,text,mode=0o644):
 out=appdir/name;out.parent.mkdir(parents=True,exist_ok=True);out.write_text(text);out.chmod(mode)
write('AppRun','''#!/bin/sh
set -e
lormusic_runtime_appdir="${APPDIR:-}"
APPDIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
export APPDIR
LORMUSIC_APPIMAGE=""
if [ "$lormusic_runtime_appdir" = "$APPDIR" ] && [ -n "${APPIMAGE:-}" ]; then LORMUSIC_APPIMAGE="$APPIMAGE"; fi
export LORMUSIC_APPIMAGE
export PATH="$APPDIR/usr/bin:$PATH"
export LD_LIBRARY_PATH="$APPDIR/usr/lib:$APPDIR/usr/lib/pulseaudio${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export VLC_PLUGIN_PATH="$APPDIR/usr/lib/vlc/plugins"
exec "$APPDIR/opt/lormusic/bin/lormusic" "$@"
''',0o755)
write('usr/bin/cvlc','''#!/bin/sh
exec "$APPDIR/usr/bin/vlc" -I dummy "$@"
''',0o755)
bin_dir=appdir/'usr/bin';shutil.copy2('/usr/bin/vlc',bin_dir/'vlc')
plugins=pathlib.Path('/usr/lib/x86_64-linux-gnu/vlc/plugins')
for group in ['access','access_output','audio_filter','audio_mixer','audio_output','codec','control','demux','gui','keystore','logger','meta_engine','misc','mux','packetizer','stream_filter','stream_out','text_renderer','video_chroma','video_filter','video_output']:
 if(plugins/group).exists():shutil.copytree(plugins/group,appdir/'usr/lib/vlc/plugins'/group)
libs=appdir/'usr/lib';libs.mkdir(parents=True,exist_ok=True)
# Do not bundle the libc/loader or graphics driver stack. They must match the host kernel/drivers.
exclude=re.compile(r'^(?:ld-linux|libc\.|libm\.|libpthread\.|libdl\.|librt\.|libresolv\.|libnss_|libGL(?:X|dispatch)?\.|libEGL\.|libdrm|libgbm|libvulkan)')
queue=[pathlib.Path('/usr/lib/x86_64-linux-gnu/libvlc.so.5'),pathlib.Path('/usr/lib/x86_64-linux-gnu/libvlccore.so.9'),bin_dir/'vlc']+list((libs/'vlc/plugins').rglob('*.so'))
seen=set();bundled=[]
while queue:
 f=queue.pop()
 key=str(f.resolve())
 if key in seen:continue
 seen.add(key)
 if not str(f).startswith(str(appdir)) and not exclude.match(f.name):
  target=libs/f.name
  if not target.exists():shutil.copy2(f.resolve(),target);bundled.append(str(f))
 result=subprocess.run(['ldd',str(f)],text=True,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL)
 for line in result.stdout.splitlines():
  m=re.search(r'=>\s+(/\S+)',line)
  if not m:continue
  dep=pathlib.Path(m.group(1))
  if not exclude.match(dep.name):queue.append(dep)
for name,versioned in [('libvlc.so','libvlc.so.5'),('libvlccore.so','libvlccore.so.9')]:
 if (libs/versioned).exists() and not (libs/name).exists():(libs/name).symlink_to(versioned)
write('lormusic.desktop','''[Desktop Entry]
Type=Application
Name=LorMusic
Comment=Music, beautifully organized
Comment[ru]=Ваша музыка в выразительном интерфейсе
Comment[uk]=Ваша музика у виразному інтерфейсі
Comment[pl]=Twoja muzyka w nowoczesnym interfejsie
Comment[de]=Deine Musik in einer ausdrucksstarken Oberfläche
Exec=AppRun %F
MimeType=audio/mpeg;audio/flac;audio/ogg;audio/opus;audio/x-wav;audio/mp4;video/mp4;video/webm;video/x-matroska;video/quicktime;video/x-msvideo;
Icon=lormusic
Terminal=false
Categories=AudioVideo;Audio;Player;
''')
shutil.copy2(root/'packaging/lormusic.png',appdir/'lormusic.png');shutil.copy2(appdir/'lormusic.png',appdir/'.DirIcon')
for name in ['LICENSE','THIRD_PARTY_NOTICES.md','RELEASE_1.8.3.md']:
 if(root/name).exists():out=appdir/'usr/share/doc/lormusic'/name;out.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(root/name,out)
# Include the distribution copyright/license records of all bundled shared-library packages.
packages=set()
for lib in bundled+['/usr/bin/vlc']:
 result=subprocess.run(['dpkg-query','-S',lib],text=True,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL)
 for line in result.stdout.splitlines():
  if ': ' in line:packages.add(line.split(': ',1)[0].split(':')[0])
for package in packages:
 copyright=pathlib.Path('/usr/share/doc')/package/'copyright'
 if copyright.is_file():out=appdir/'usr/share/doc/lormusic/bundled-licenses'/f'{package}.txt';out.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(copyright,out)
write('usr/share/doc/lormusic/AppImage-provenance.json',json.dumps({'version':'1.8.3','bundled_packages':sorted(packages),'bundled_libraries':bundled,'build_distribution':pathlib.Path('/etc/os-release').read_text(),'host_requirements':'x86_64 Linux with a glibc ABI compatible with the build distribution and desktop X11/Wayland libraries; Java and VLC are bundled.'},indent=2))
args_output=a.output.resolve();args_output.mkdir(parents=True,exist_ok=True);out=args_output/'LorMusic-1.8.3-Linux-x86_64.AppImage'
env=os.environ.copy();env['ARCH']='x86_64'
subprocess.run([str(a.tool.resolve())]+(['--runtime-file',str(a.runtime.resolve())] if a.runtime else [])+['--no-appstream','--mksquashfs-opt','-processors','--mksquashfs-opt','2',str(appdir),str(out)],env=env,check=True)
out.chmod(0o755);assert out.read_bytes()[8:11]==b'AI\x02';print(out)
