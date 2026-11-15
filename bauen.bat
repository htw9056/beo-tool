@echo off
REM Baut beo-tool.jar (Java 17+)
if exist build rmdir /s /q build
mkdir build
if not exist dist mkdir dist
javac --release 17 -encoding UTF-8 -d build src\beotool\*.java || goto fehler
xcopy /q /i src\beotool\bilder build\beotool\bilder >nul
xcopy /q /i src\beotool\hilfe build\beotool\hilfe >nul
jar cfm dist\beo-tool.jar MANIFEST.MF -C build . || goto fehler
echo Fertig: dist\beo-tool.jar
goto ende
:fehler
echo Fehler beim Bauen.
:ende
pause
