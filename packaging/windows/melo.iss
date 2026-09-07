; =====================================================================;
Inno Setup
Script for
Melo Music
Player (Windows
x64); =====================================================================

#define MyAppName "Melo"
#ifndef MyAppVersion
#define MyAppVersion "1.0.1"
#endif
#define MyAppPublisher "Adriianh"
#define MyAppURL "https://github.com/Adriianh/Melo"
#define MyAppExeName "Melo.exe"
#define MyAppId "{{8B6E32A1-19B4-4D4F-8E4A-21E68FDE31C5}}"

#ifndef AppSourceDir
#define AppSourceDir "..\..\composeApp\build\compose\binaries\main\app\Melo"
#endif
#ifndef OutputDir
#define OutputDir "..\..\build\dist"
#endif

[Setup]
AppId = {
#
        MyAppId}
AppName = {
#
        MyAppName}
AppVersion = {
#
        MyAppVersion}

AppVerName = {
#
        MyAppName} {
#MyAppVersion
}

AppPublisher = {
#
        MyAppPublisher}
AppPublisherURL = {
#
        MyAppURL}
AppSupportURL = {
#
        MyAppURL}
/
issues
        AppUpdatesURL = {
#
        MyAppURL}
/
releases
        DefaultDirName = {autopf}
\{
#MyAppName}
DefaultGroupName = {
#
        MyAppName}
AllowNoIcons = yes
OutputDir={
#OutputDir}
OutputBaseFilename = Melo - Setup - x64
Compression = lzma2 / ultra64
SolidCompression = yes
WizardStyle = modern
PrivilegesRequiredOverridesAllowed = dialog
commandline
        ArchitecturesAllowed = x64compatible
ArchitecturesInstallIn64BitMode = x64compatible
CloseApplications = yes
CloseApplicationsFilter = *
.
exe
        RestartApplications = no
UninstallDisplayIcon = {app}
\{
#MyAppExeName}

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "{#AppSourceDir}\*"; DestDir: "{app}"; Flags:
ignoreversion recursesubdirs
createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags:
nowait postinstall
skipifsilent

[Code]
// Prompt the user during uninstall whether they want to purge user settings and cache
procedure CurUninstallStepChanged(CurUninstallStep
: TUninstallStep);
var
        DataDir
:
String;
MsgResult:
Integer;
begin
if
CurUninstallStep = usUninstall
then
        begin
MsgResult := MsgBox(
'¿Deseas eliminar también todos tus datos de configuración, historial y caché de Melo?' + #13#10#13#10 +
'Si seleccionas "No", tus preferencias y biblioteca se mantendrán por si decides reinstalar la aplicación.',
mbConfirmation, MB_YESNO or MB_DEFBUTTON2
);
if
MsgResult = IDYES
then
        begin
DataDir := ExpandConstant('{localappdata}\Melo');
if
DirExists(DataDir)
then
        DelTree(DataDir, True, True, True);

DataDir := ExpandConstant('{userappdata}\Melo');
if
DirExists(DataDir)
then
        DelTree(DataDir, True, True, True);
end;
end;
end;