Unicode true
!include "MUI2.nsh"
!include "nsDialogs.nsh"
!include "LogicLib.nsh"
Var SetupPage
Var SetupFolder
Var SetupShortcut
Var MakeShortcut
Var SetupRun
Name "LorMusic"
!ifndef OUTPUTFILE
!define OUTPUTFILE "LorMusic-1.8.3-Windows-x64-Setup.exe"
!endif
OutFile "${OUTPUTFILE}"
InstallDir "$LOCALAPPDATA\Programs\LorMusic"
InstallDirRegKey HKCU "Software\LorMusicDesktop" "InstallDir"
RequestExecutionLevel user
ManifestDPIAware true
SetCompressor zlib
BrandingText "LorMusic · Material 3 Expressive"
!define MUI_ICON "lormusic.ico"
!define MUI_UNICON "lormusic.ico"
!define MUI_WELCOMEFINISHPAGE_BITMAP "welcome.bmp"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "header.bmp"
!define MUI_ABORTWARNING
!define MUI_LANGDLL_ALLLANGUAGES
!define MUI_LANGDLL_REGISTRY_ROOT "HKCU"
!define MUI_LANGDLL_REGISTRY_KEY "Software\LorMusicDesktop"
!define MUI_LANGDLL_REGISTRY_VALUENAME "InstallerLanguage"
!define MUI_FINISHPAGE_RUN "$INSTDIR\LorMusic.exe"
!define MUI_WELCOMEPAGE_TITLE "$(WelcomeTitle)"
!define MUI_WELCOMEPAGE_TEXT "$(WelcomeText)"
Page custom ExpressiveWelcome
!insertmacro MUI_PAGE_LICENSE "../LICENSE"
Page custom ExpressiveOptions ExpressiveOptionsLeave
!insertmacro MUI_PAGE_INSTFILES
Page custom ExpressiveFinish ExpressiveFinishLeave
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE "Russian"
!insertmacro MUI_LANGUAGE "English"
!insertmacro MUI_LANGUAGE "German"
!insertmacro MUI_LANGUAGE "Ukrainian"
!insertmacro MUI_LANGUAGE "Polish"
LangString WelcomeTitle ${LANG_RUSSIAN} "LorMusic для компьютера"
LangString WelcomeText ${LANG_RUSSIAN} "Ваша музыка в интерфейсе Material 3.$\r$\n$\r$\nЛокальная коллекция, YouTube Music, SoundCloud, плейлисты, тексты и эквалайзер.$\r$\n$\r$\nJava и аудиодвижок включены в установщик."
LangString WelcomeTitle ${LANG_ENGLISH} "LorMusic for your computer"
LangString WelcomeText ${LANG_ENGLISH} "Your music in a Material 3 interface.$\r$\n$\r$\nLocal library, YouTube Music, SoundCloud, playlists, lyrics and equalizer.$\r$\n$\r$\nJava and the audio engine are included."
LangString WelcomeTitle ${LANG_GERMAN} "LorMusic für deinen Computer"
LangString WelcomeText ${LANG_GERMAN} "Deine Musik mit Material 3.$\r$\n$\r$\nLokale Sammlung, YouTube Music, SoundCloud, Playlists, Liedtexte und Equalizer.$\r$\n$\r$\nJava und die Audio-Engine sind enthalten."
LangString WelcomeTitle ${LANG_UKRAINIAN} "LorMusic для комп’ютера"
LangString WelcomeText ${LANG_UKRAINIAN} "Ваша музика в інтерфейсі Material 3.$\r$\n$\r$\nЛокальна колекція, YouTube Music, SoundCloud, плейлисти, тексти й еквалайзер.$\r$\n$\r$\nJava та аудіорушій включені до інсталятора."
LangString WelcomeTitle ${LANG_POLISH} "LorMusic na komputer"
LangString WelcomeText ${LANG_POLISH} "Twoja muzyka w interfejsie Material 3.$\r$\n$\r$\nLokalna kolekcja, YouTube Music, SoundCloud, playlisty, teksty i korektor.$\r$\n$\r$\nJava i silnik audio są dołączone."

LangString OptionsTitle ${LANG_RUSSIAN} "Параметры установки"
LangString OptionsTitle ${LANG_ENGLISH} "Installation options"
LangString OptionsTitle ${LANG_GERMAN} "Installationsoptionen"
LangString OptionsTitle ${LANG_UKRAINIAN} "Параметри встановлення"
LangString OptionsTitle ${LANG_POLISH} "Opcje instalacji"
LangString FolderLabel ${LANG_RUSSIAN} "Папка установки"
LangString FolderLabel ${LANG_ENGLISH} "Install location"
LangString FolderLabel ${LANG_GERMAN} "Installationsordner"
LangString FolderLabel ${LANG_UKRAINIAN} "Папка встановлення"
LangString FolderLabel ${LANG_POLISH} "Folder instalacji"
LangString PickFolder ${LANG_RUSSIAN} "Обзор…"
LangString PickFolder ${LANG_ENGLISH} "Browse…"
LangString PickFolder ${LANG_GERMAN} "Durchsuchen…"
LangString PickFolder ${LANG_UKRAINIAN} "Огляд…"
LangString PickFolder ${LANG_POLISH} "Przeglądaj…"
LangString ShortcutLabel ${LANG_RUSSIAN} "Создать ярлык на рабочем столе"
LangString ShortcutLabel ${LANG_ENGLISH} "Create a desktop shortcut"
LangString ShortcutLabel ${LANG_GERMAN} "Desktopverknüpfung erstellen"
LangString ShortcutLabel ${LANG_UKRAINIAN} "Створити ярлик на стільниці"
LangString ShortcutLabel ${LANG_POLISH} "Utwórz skrót na pulpicie"
LangString DoneTitle ${LANG_RUSSIAN} "LorMusic установлен"
LangString DoneTitle ${LANG_ENGLISH} "LorMusic is installed"
LangString DoneTitle ${LANG_GERMAN} "LorMusic wurde installiert"
LangString DoneTitle ${LANG_UKRAINIAN} "LorMusic встановлено"
LangString DoneTitle ${LANG_POLISH} "LorMusic jest zainstalowany"
LangString RunLabel ${LANG_RUSSIAN} "Открыть LorMusic"
LangString RunLabel ${LANG_ENGLISH} "Open LorMusic"
LangString RunLabel ${LANG_GERMAN} "LorMusic öffnen"
LangString RunLabel ${LANG_UKRAINIAN} "Відкрити LorMusic"
LangString RunLabel ${LANG_POLISH} "Otwórz LorMusic"
LangString BadFolder ${LANG_RUSSIAN} "Выберите отдельную папку для LorMusic."
LangString BadFolder ${LANG_ENGLISH} "Choose a dedicated folder for LorMusic."
LangString BadFolder ${LANG_GERMAN} "Wähle einen eigenen Ordner für LorMusic."
LangString BadFolder ${LANG_UKRAINIAN} "Виберіть окрему папку для LorMusic."
LangString BadFolder ${LANG_POLISH} "Wybierz osobny folder dla LorMusic."

VIProductVersion "1.8.3.0"
VIAddVersionKey "ProductName" "LorMusic"
VIAddVersionKey "FileDescription" "LorMusic Installer"
VIAddVersionKey "FileVersion" "1.8.3"
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
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".mp4" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".mkv" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".webm" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".mov" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".avi" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".m4v" ""
 WriteRegStr HKCU "Software\Classes\Applications\LorMusic.exe\SupportedTypes" ".ts" ""
 WriteUninstaller "$INSTDIR\Uninstall.exe"
 CreateDirectory "$SMPROGRAMS\LorMusic"
 CreateShortcut "$SMPROGRAMS\LorMusic\LorMusic.lnk" "$INSTDIR\LorMusic.exe" "" "$INSTDIR\LorMusic.exe"
 ${If} $MakeShortcut == ${BST_CHECKED}
  CreateShortcut "$DESKTOP\LorMusic.lnk" "$INSTDIR\LorMusic.exe" "" "$INSTDIR\LorMusic.exe"
 ${EndIf}
 WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop" "DisplayName" "LorMusic"
 WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop" "UninstallString" '"$INSTDIR\Uninstall.exe"'
 WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop" "DisplayIcon" "$INSTDIR\LorMusic.exe"
 WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop" "DisplayVersion" "1.8.3"
 WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop" "Publisher" "LorMusic"
 WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop" "NoModify" 1
 WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop" "NoRepair" 1
SectionEnd
Section "Uninstall"
 Delete "$DESKTOP\LorMusic.lnk"
 RMDir /r "$SMPROGRAMS\LorMusic"
 RMDir /r "$INSTDIR\app"
 RMDir /r "$INSTDIR\runtime"
 RMDir /r "$INSTDIR\vlc"
 RMDir /r "$INSTDIR\third-party-licenses"
 Delete "$INSTDIR\LorMusic.exe"
 Delete "$INSTDIR\Uninstall.exe"
 Delete "$INSTDIR\README.md"
 Delete "$INSTDIR\LICENSE"
 Delete "$INSTDIR\THIRD_PARTY_NOTICES.md"
 RMDir "$INSTDIR"
 DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\LorMusicDesktop"
 DeleteRegKey HKCU "Software\LorMusicDesktop"
 DeleteRegKey HKCU "Software\Classes\Applications\LorMusic.exe"
SectionEnd

Function .onInit
 StrCpy $MakeShortcut ${BST_CHECKED}
 !insertmacro MUI_LANGDLL_DISPLAY
FunctionEnd

Function un.onInit
 !insertmacro MUI_UNGETLANGUAGE
FunctionEnd

!macro ExpressivePage heading
 nsDialogs::Create 1018
 Pop $SetupPage
 ${If} $SetupPage == error
  Abort
 ${EndIf}
 SetCtlColors $SetupPage 172237 EDF3FF
 ${NSD_CreateLabel} 12u 8u 94% 17u "LorMusic  ·  1.8.3"
 Pop $0
 SetCtlColors $0 315FAD EDF3FF
 ${NSD_CreateLabel} 12u 34u 94% 27u "${heading}"
 Pop $0
 CreateFont $1 "Segoe UI" 17 700
 SendMessage $0 ${WM_SETFONT} $1 1
 SetCtlColors $0 172237 EDF3FF
!macroend

Function ExpressiveWelcome
 !insertmacro ExpressivePage "$(WelcomeTitle)"
 ${NSD_CreateLabel} 12u 70u 92% 72u "$(WelcomeText)"
 Pop $0
 SetCtlColors $0 34486F EDF3FF
 Push $0
 Call ExpressiveRoundCard
 nsDialogs::Show
FunctionEnd

Function ExpressiveOptions
 !insertmacro ExpressivePage "$(OptionsTitle)"
 ${NSD_CreateLabel} 12u 65u 92% 13u "$(FolderLabel)"
 Pop $0
 SetCtlColors $0 34486F EDF3FF
 ${NSD_CreateDirRequest} 12u 80u 67% 15u "$INSTDIR"
 Pop $SetupFolder
 ${NSD_CreateBrowseButton} 81% 79u 16% 17u "$(PickFolder)"
 Pop $0
 ${NSD_OnClick} $0 ExpressiveBrowse
 ${NSD_CreateCheckbox} 12u 108u 92% 15u "$(ShortcutLabel)"
 Pop $SetupShortcut
 SetCtlColors $SetupShortcut 34486F EDF3FF
 ${NSD_SetState} $SetupShortcut $MakeShortcut
 nsDialogs::Show
FunctionEnd

Function ExpressiveBrowse
 ${NSD_GetText} $SetupFolder $0
 nsDialogs::SelectFolderDialog "$(FolderLabel)" "$0"
 Pop $0
 ${If} $0 != error
  ${NSD_SetText} $SetupFolder "$0"
 ${EndIf}
FunctionEnd

Function ExpressiveOptionsLeave
 ${NSD_GetText} $SetupFolder $0
 StrCpy $1 $0 "" -1
 ${If} $0 == ""
 ${OrIf} $0 == "$WINDIR"
 ${OrIf} $0 == "$PROFILE"
 ${OrIf} $0 == "$DESKTOP"
 ${OrIf} $1 == ":"
 ${OrIf} $0 == "$LOCALAPPDATA"
  MessageBox MB_OK|MB_ICONEXCLAMATION "$(BadFolder)"
  Abort
 ${EndIf}
 GetFullPathName $INSTDIR "$0"
 ${NSD_GetState} $SetupShortcut $MakeShortcut
FunctionEnd

Function ExpressiveFinish
 !insertmacro ExpressivePage "$(DoneTitle)"
 ${NSD_CreateCheckbox} 12u 72u 92% 18u "$(RunLabel)"
 Pop $SetupRun
 SetCtlColors $SetupRun 34486F EDF3FF
 ${NSD_Check} $SetupRun
 nsDialogs::Show
FunctionEnd

Function ExpressiveFinishLeave
 ${NSD_GetState} $SetupRun $0
 ${If} $0 == ${BST_CHECKED}
  Exec '"$INSTDIR\LorMusic.exe"'
 ${EndIf}
FunctionEnd

Function ExpressiveRoundCard
 Exch $0
 System::Alloc 16
 Pop $2
 System::Call 'user32::GetClientRect(p r0, p r2) i .r1'
 ${If} $1 != 0
  System::Call '*$2(i .r3, i .r4, i .r5, i .r6)'
  System::Call 'gdi32::CreateRoundRectRgn(i 0, i 0, i r5, i r6, i 22, i 22) p .r1'
  System::Call 'user32::SetWindowRgn(p r0, p r1, i 1) i .r3'
  ${If} $3 == 0
   System::Call 'gdi32::DeleteObject(p r1)'
  ${EndIf}
 ${EndIf}
 System::Free $2
 Pop $0
FunctionEnd
