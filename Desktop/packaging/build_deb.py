#!/usr/bin/env python3
"""Package a Compose app-image. Run after ./gradlew createDistributable."""
import argparse, pathlib, shutil, subprocess
parser=argparse.ArgumentParser();parser.add_argument('--output',type=pathlib.Path,default=pathlib.Path('dist'));args=parser.parse_args()
root=pathlib.Path(__file__).resolve().parent.parent
image=root/'build/compose/binaries/main/app/lormusic'
if not (image/'bin/lormusic').is_file():raise SystemExit('Run ./gradlew createDistributable first')
stage=root/'build/debian';shutil.rmtree(stage,ignore_errors=True)
shutil.copytree(image,stage/'opt/lormusic')
def write(name,content,mode=0o644):
 p=stage/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content);p.chmod(mode)
size=sum(p.stat().st_size for p in (stage/'opt').rglob('*') if p.is_file())//1024
write('DEBIAN/control',f'''Package: lormusic-desktop
Version: 1.8.0
Architecture: amd64
Maintainer: LorMusic maintainers <noreply@localhost>
Installed-Size: {size}
Depends: vlc (>= 3.0), libx11-6, libxi6, libxrender1, libxtst6, libfreetype6, fontconfig, libnss3, libgtk-3-0 | libgtk-3-0t64, libasound2 | libasound2t64
Section: sound
Priority: optional
Homepage: https://github.com/Lorvance/LorMusic
Description: Material 3 Expressive music player for desktop
 Local audio library, YouTube Music and SoundCloud, playlists, synchronized
 lyrics, tag editing, audio recognition, downloads and equalizer.
 Java runtime is included.
''')
write('usr/bin/lormusic','#!/bin/sh\nexec /opt/lormusic/bin/lormusic "$@"\n',0o755)
write('usr/share/applications/com.lormusic.desktop.desktop','''[Desktop Entry]
Version=1.0
Type=Application
Name=LorMusic
GenericName=Music Player
GenericName[ru]=Музыкальный плеер
Comment=Your collection, new discoveries, one expressive player
Comment[ru]=Вся ваша коллекция, новые открытия и выразительный плеер
Exec=lormusic %F
Icon=lormusic-desktop
Terminal=false
Categories=AudioVideo;Audio;Player;
MimeType=audio/mpeg;audio/flac;audio/ogg;audio/opus;audio/x-wav;audio/mp4;audio/x-mpegurl;
StartupNotify=true
StartupWMClass=com-lormusic-desktop-MainKt
''')
write('usr/share/metainfo/com.lormusic.desktop.metainfo.xml','''<?xml version="1.0" encoding="UTF-8"?>
<component type="desktop-application">
 <id>com.lormusic.desktop</id>
 <metadata_license>CC0-1.0</metadata_license>
 <project_license>GPL-3.0-only</project_license>
 <name>LorMusic</name>
 <developer id="com.lormusic"><name>LorMusic contributors</name></developer>
 <summary>Music, beautifully organized</summary>
 <summary xml:lang="ru">Ваша музыка в выразительном интерфейсе Material 3</summary>
 <description><p>A desktop music player with a local library, playlists, synchronized lyrics, tag editor, equalizer, YouTube Music and SoundCloud.</p><p xml:lang="ru">Музыкальный плеер с локальной медиатекой, плейлистами, текстами, редактором тегов, эквалайзером, YouTube Music и SoundCloud.</p></description>
 <launchable type="desktop-id">com.lormusic.desktop.desktop</launchable>
 <url type="homepage">https://github.com/Lorvance/LorMusic</url>
 <provides><binary>lormusic</binary></provides>
 <releases><release version="1.8.0" date="2026-11-15"/></releases>
 <content_rating type="oars-1.1"/>
</component>
''')
if (root/'third-party-licenses').exists():shutil.copytree(root/'third-party-licenses',stage/'usr/share/doc/lormusic-desktop/third-party-licenses')
icon=stage/'usr/share/icons/hicolor/512x512/apps/lormusic-desktop.png';icon.parent.mkdir(parents=True,exist_ok=True);shutil.copy(root/'packaging/lormusic.png',icon)
for name in ['LICENSE','THIRD_PARTY_NOTICES.md','README.md']:
 if (root/name).exists():
  p=stage/'usr/share/doc/lormusic-desktop'/name;p.parent.mkdir(parents=True,exist_ok=True);shutil.copy(root/name,p)
write('DEBIAN/postinst','''#!/bin/sh
set -e
if command -v update-desktop-database >/dev/null 2>&1; then update-desktop-database /usr/share/applications || true; fi
if command -v gtk-update-icon-cache >/dev/null 2>&1; then gtk-update-icon-cache -q -t -f /usr/share/icons/hicolor || true; fi
exit 0
''',0o755)
write('DEBIAN/postrm',(stage/'DEBIAN/postinst').read_text(),0o755)
args.output=args.output.resolve();args.output.mkdir(parents=True,exist_ok=True)
package=args.output/'LorMusic-1.8.0-Linux-amd64.deb'
subprocess.run(['dpkg-deb','--root-owner-group','--build',str(stage),str(package)],check=True)
print(package)
