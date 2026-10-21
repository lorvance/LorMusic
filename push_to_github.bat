@echo off
title Push LorMusic to GitHub
cd /d "%~dp0"
echo ==============================================
echo   Pushing LorMusic repository to GitHub...
echo ==============================================
git add .
git commit -m "Update"
git push -u origin main
echo.
pause