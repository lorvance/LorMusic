#!/usr/bin/env python3
"""Cross-package a Windows bundle with NSIS. Dependencies are supplied explicitly."""
import argparse,pathlib,shutil,subprocess,zipfile,os
p=argparse.ArgumentParser();p.add_argument('--jre',type=pathlib.Path,required=True);p.add_argument('--vlc',type=pathlib.Path,required=True);p.add_argument('--skiko',type=pathlib.Path,required=True);p.add_argument('--makensis',default='makensis');p.add_argument('--output',type=pathlib.Path,default=pathlib.Path('dist'));a=p.parse_args()
root=pathlib.Path(__file__).resolve().parent.parent;pack=root/'packaging';win=pack/'windows';shutil.rmtree(win,ignore_errors=True);win.mkdir()
for archive,target in [(a.jre,'runtime'),(a.vlc,'vlc')]:
 with zipfile.ZipFile(archive) as z:
  for obj in z.infolist():
   parts=pathlib.PurePosixPath(obj.filename).parts[1:]
   if obj.is_dir() or not parts:continue
   if '..' in parts:raise ValueError('Unsafe archive entry')
   out=win/target/pathlib.Path(*parts);out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(z.read(obj))
app=win/'app';app.mkdir()
for jar in (root/'build/desktop-runtime').glob('*.jar'):
 if 'runtime-linux-' not in jar.name and 'runtime-macos-' not in jar.name and 'runtime-windows-' not in jar.name:shutil.copy(jar,app/jar.name)
if not list(app.glob('LorMusicDesktop*.jar')):raise SystemExit('Run ./gradlew exportRuntime first')
shutil.copy(a.skiko,app/'skiko-awt-runtime-windows-x64-0.8.18.jar')
for name in ['LICENSE','README.md','THIRD_PARTY_NOTICES.md']:shutil.copy(root/name,win/name)
if (root/'third-party-licenses').exists():shutil.copytree(root/'third-party-licenses',win/'third-party-licenses')
a.output=a.output.resolve();a.output.mkdir(parents=True,exist_ok=True)
subprocess.run([a.makensis,'-V2','launcher.nsi'],cwd=pack,check=True)
subprocess.run([a.makensis,'-V2','-DOUTPUTFILE='+str(a.output/'LorMusic-1.8.0-Windows-x64-Setup.exe'),'installer.nsi'],cwd=pack,check=True)
print(a.output/'LorMusic-1.8.0-Windows-x64-Setup.exe')
