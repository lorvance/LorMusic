Unicode true
!include "MUI2.nsh"
Name "LorMusic Desktop"
!ifndef OUTPUTFILE
!define OUTPUTFILE "LorMusic-1.8.0-Windows-x64-Setup.exe"
!endif
OutFile "${OUTPUTFILE}"
InstallDir "$LOCALAPPDATA\Programs\LorMusic"
InstallDirRegKey HKCU "Software\LorMusicDesktop" "InstallDir"
RequestExecutionLevel user
ManifestDPIAware true
SetCompressor /SOLID lzma
BrandingText "LorMusic · Material 3 Expressive"
!define MUI_ICON "lormusic.ico"
!define MUI_UNICON "lormusic.ico"
!define MUI_WELCOMEFINISHPAGE_BITMAP "welcome.bmp"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "header.bmp"
!define MUI_ABORTWARNING
!define MUI_FINISHPAGE_RUN "$INSTDIR\LorMusic.exe"
!define MUI_WELCOMEPAGE_TITLE "LorMusic для компьютера"
!define MUI_WELCOMEPAGE_TEXT "Ваша музыка в выразительном интерфейсе Material 3.$\r$\n$\r$\nЛокальная коллекция, YouTube Music, SoundCloud, плейлисты, тексты и эквалайзер.$\r$\n$\r$\nJava и аудиодвижок уже включены в установщик."
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "../LICENSE"
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE "Russian"
!insertmacro MUI_LANGUAGE "English"
!insertmacro MUI_LANGUAGE "German"
!insertmacro MUI_LANGUAGE "Ukrainian"
!insertmacro MUI_LANGUAGE "Polish"
VIProductVersion "1.8.0.0"
VIAddVersionKey "ProductName" "LorMusic Desktop"
VIAddVersionKey "FileDescription" "LorMusic Desktop Installer"
VIAddVersionKey "FileVersion" "1.8.0"
VIAddVersionKey "LegalCopyright" "LorMusic contributors · GPL-3.0"
Section "LorMusic" Main
 SetOutPath "$INSTDIR"
 File /r "windows/*"
 WriteRegStr HKCU "Software\LorMusicDesktop" "InstallDir" "$INSTDIR"
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe" "FriendlyAppName" "LorMusic"
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\shell\open\command" "" '"$INSTDIR\LorMusic.exe" "%1"'
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".mp3" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".flac" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".m4a" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".aac" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".ogg" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".opus" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".wav" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".aiff" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".wma" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".m3u" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".m3u8" ""
 WriteUninstaller "$INSTDIR\Uninstall.exe"
 CreateDirectory "$SMPROGRAMS\LorMusic"
 CreateShortcut "$SMPROGRAMS\LorMusic\LorMusic.lnk" "$INSTDIR\LorMusic.exe" "" "$INSTDIR\LorMusic.exe"
 CreateShortcut "$DESKTOP\LorMusic.lnk" "$INSTDIR\LorMusic.exe" "" "$INSTDIR\LorMusic.exe"
 WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop" "DisplayName" "LorMusic Desktop"
 WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop" "UninstallString" '"$INSTDIR\Uninstall.exe"'
 WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop" "DisplayIcon" "$INSTDIR\LorMusic.exe"
 WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop" "DisplayVersion" "1.8.0"
 WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop" "Publisher" "LorMusic"
 WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop" "NoModify" 1
 WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop" "NoRepair" 1
SectionEnd
Section "Uninstall"
 Delete "$DESKTOP\LorMusic.lnk"
 RMDir /r "$SMPROGRAMS\LorMusic"
 RMDir /r "$INSTDIR"
 DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop"
 DeleteRegKey HKCU "Software\LorMusicDesktop"
 DeleteRegKey HKCU "Software\Classes\Applications\LorMusic.exe"
SectionEnd
