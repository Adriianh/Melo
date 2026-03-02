@echo off
set MELO_HOME=%~dp0..
java --enable-native-access=ALL-UNNAMED -jar "%MELO_HOME%\melo.jar" %*

