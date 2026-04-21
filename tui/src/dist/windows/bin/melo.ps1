$meloHome = Split-Path -Parent $PSScriptRoot
& java --enable-native-access=ALL-UNNAMED -XX:+UseSerialGC -Xms16m -Xmx256m -XX:TieredStopAtLevel=1 -jar "$meloHome\melo.jar" @args
