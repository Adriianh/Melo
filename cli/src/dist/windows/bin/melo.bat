@echo off
set MELO_HOME=%~dp0..
java --enable-native-access=ALL-UNNAMED -XX:+UseSerialGC -Xms16m -Xmx256m -XX:TieredStopAtLevel=1 -jar "%MELO_HOME%\melo.jar" %*
