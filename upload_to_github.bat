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
if not errorlevel 1 goto :remote_exists

echo.
echo [2/5] Configuring remote repository...
set "REPO_URL=https://github.com/Lorvance/LorMusic.git"
echo Default repo URL: %REPO_URL%
set "USER_INPUT="
set /p "USER_INPUT=Press Enter to use default, or enter new URL: "
if not "%USER_INPUT%"=="" set "REPO_URL=%USER_INPUT%"
git remote remove origin >nul 2>&1
git remote add origin "%REPO_URL%"
echo Remote origin set to: %REPO_URL%
goto :remote_done

:remote_exists
for /f "tokens=*" %%u in ('git remote get-url origin') do echo [2/5] Remote origin: %%u

:remote_done
echo.
set "COMMIT_MSG=Release v1.5.5.2"
set "USER_MSG="
set /p "USER_MSG=[3/5] Commit message [Enter for 'Release v1.5.5.2']: "
if not "%USER_MSG%"=="" set "COMMIT_MSG=%USER_MSG%"

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
echo   If GitHub requires authentication:
echo     Sign in with GitHub CLI (gh auth login) or Personal Access Token.
echo.
echo   If GitHub repository already has existing commits:
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