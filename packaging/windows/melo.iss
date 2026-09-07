; =====================================================================
; Inno Setup Script for Melo Music Player (Windows x64)
; =====================================================================

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
  #define OutputDir "Output"
#endif

[Setup]
AppId={#MyAppId}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}/issues
AppUpdatesURL={#MyAppURL}/releases
DefaultDirName={autopf}\{#MyAppName}
AllowNoIcons=yes
OutputDir={#OutputDir}
OutputBaseFilename=Melo-Setup
Compression=lzma2/ultra64
SolidCompression=yes

; Modern visual styling and system theme adaptation
WizardStyle=modern dynamic
WizardResizable=no
WizardImageFile=assets\wizard_large.bmp
WizardSmallImageFile=assets\wizard_small.bmp
SetupIconFile=assets\melo.ico
UninstallDisplayIcon={app}\{#MyAppExeName}

; Streamlined modern UX (clean and fast installation)
DisableWelcomePage=no
DisableProgramGroupPage=yes
DisableReadyPage=yes

; User-mode installation by default (no UAC prompt required, defaults to %LocalAppData%\Programs)
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog commandline

ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
CloseApplications=yes
CloseApplicationsFilter=*.exe
RestartApplications=no

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "{#AppSourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[Code]
// Prompt the user during uninstall whether they want to purge user settings and cache
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  DataDir: String;
  MsgResult: Integer;
begin
  if CurUninstallStep = usUninstall then
  begin
    MsgResult := MsgBox(
      'Do you also want to delete all your Melo settings, playback history, and cache?' + #13#10#13#10 +
      'If you select "No", your preferences and library will be preserved in case you reinstall the application.',
      mbConfirmation, MB_YESNO or MB_DEFBUTTON2
    );
    if MsgResult = IDYES then
    begin
      DataDir := ExpandConstant('{localappdata}\Melo');
      if DirExists(DataDir) then
        DelTree(DataDir, True, True, True);

      DataDir := ExpandConstant('{userappdata}\Melo');
      if DirExists(DataDir) then
        DelTree(DataDir, True, True, True);
    end;
  end;
end;
