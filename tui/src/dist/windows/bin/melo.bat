@echo off
if "%~1"=="--gui" (
    shift
    if exist "%LOCALAPPDATA%\Programs\Melo\Melo.exe" (
        start "" "%LOCALAPPDATA%\Programs\Melo\Melo.exe" %*
        exit /b 0
    ) else (
        echo Error: Melo GUI is not installed.
        exit /b 1
    )
)
if "%~1"=="-g" (
    shift
    if exist "%LOCALAPPDATA%\Programs\Melo\Melo.exe" (
        start "" "%LOCALAPPDATA%\Programs\Melo\Melo.exe" %*
        exit /b 0
    ) else (
        echo Error: Melo GUI is not installed.
        exit /b 1
    )
)
if "%~1"=="--tui" shift
if "%~1"=="-t" shift
"%~dp0..\melo.exe" %*
