$meloHome = Split-Path -Parent $PSScriptRoot
& java --enable-native-access=ALL-UNNAMED -jar "$meloHome\melo.jar" @args

