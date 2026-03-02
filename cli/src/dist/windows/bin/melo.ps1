$meloHome = Split-Path -Parent $PSScriptRoot
& java -jar "$meloHome\melo.jar" @args

