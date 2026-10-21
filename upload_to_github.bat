@echo off
chcp 65001 >nul

echo =======================================================
echo          LorMusic - GitHub Auto Upload Script         
echo =======================================================
echo.

where git >nul 2>&1
if errorlevel 1 goto :no_git

git config user.name >nul 2>&1
if errorlevel 1 git config user.name "Lorvance"

git config user.email >nul 2>&1
if errorlevel 1 git config user.email "Lorvance@users.noreply.github.com"

if not exist ".git" (
    echo [1/5] Git init...
    git init
    git branch -M main
) else (
    echo [1/5] Git repository detected.
)

git remote get-url origin >nul 2>&1
if errorlevel 1 (
    echo [2/5] Setting remote origin to https://github.com/Lorvance/LorMusic.git ...
    git remote add origin https://github.com/Lorvance/LorMusic.git
) else (
    for /f "tokens=*" %%u in ('git remote get-url origin') do echo [2/5] Remote origin: %%u
)

echo.
set "COMMIT_MSG=Release v1.5.5.2"
set "USER_MSG="
set /p "USER_MSG=[3/5] Commit message [Enter for 'Release v1.5.5.2']: "
if defined USER_MSG (
    for /f "tokens=* delims= " %%a in ("%USER_MSG%") do if not "%%a"=="" set "COMMIT_MSG=%%a"
)

echo.
echo [4/5] Adding files and creating commit...
git add .
git commit -m "%COMMIT_MSG%"

echo.
echo [5/5] Pushing to GitHub (main branch)...
git push -u origin main

if errorlevel 1 goto :push_error

echo.
echo =======================================================
echo   SUCCESS: Uploaded to GitHub successfully!
echo =======================================================
goto :end

:push_error
echo.
echo =======================================================
echo   PUSH FAILED / ОШИБКА ОТПРАВКИ
echo.
echo   If GitHub requires login:
echo     Please authenticate in Git (e.g. Personal Access Token or GitHub CLI).
echo.
echo   If GitHub already has commits:
echo     Run: git pull origin main --rebase
echo     Then run this script again.
echo =======================================================
goto :end

:no_git
echo [ERROR] Git is not installed on this system.
echo Please install Git from https://git-scm.com/
goto :end

:end
echo.
pause