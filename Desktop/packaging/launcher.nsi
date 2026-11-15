Unicode true
Name "LorMusic"
OutFile "windows/LorMusic.exe"
Icon "lormusic.ico"
RequestExecutionLevel user
SilentInstall silent
AutoCloseWindow true
!include "FileFunc.nsh"
VIProductVersion "1.8.0.0"
VIAddVersionKey "ProductName" "LorMusic Desktop"
VIAddVersionKey "FileDescription" "LorMusic Desktop Player"
VIAddVersionKey "FileVersion" "1.8.0"
VIAddVersionKey "LegalCopyright" "LorMusic contributors"
Section
 ${GetParameters} $0
 SetOutPath "$EXEDIR"
 Exec '"$EXEDIR\runtime\bin\javaw.exe" "-Dlormusic.home=$EXEDIR" "-Dcompose.application.resources.dir=$EXEDIR" "-Djna.library.path=$EXEDIR\vlc" "-Dfile.encoding=UTF-8" -Xmx1024m -cp "$EXEDIR\app\*" com.lormusic.desktop.MainKt $0'
SectionEnd
