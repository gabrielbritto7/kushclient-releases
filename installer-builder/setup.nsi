# -*- coding: utf-8 -*-
Unicode true
!include "MUI2.nsh"
!include "x64.nsh"
!include "LogicLib.nsh"
!include "FileFunc.nsh"
Name "KushClient"
OutFile "${OUTPUT}"
RequestExecutionLevel user
SilentInstall silent
SetCompressor zlib
Icon "${PAYLOAD}\assets\kushclient.ico"
UninstallIcon "${PAYLOAD}\assets\kushclient.ico"
VIProductVersion "${VERSION}.0"
VIAddVersionKey /LANG=1046 "ProductName" "KushClient"
VIAddVersionKey /LANG=1046 "FileDescription" "Instalador KushClient"
VIAddVersionKey /LANG=1046 "FileVersion" "${VERSION}"
VIAddVersionKey /LANG=1046 "ProductVersion" "${VERSION}"
VIAddVersionKey /LANG=1046 "LegalCopyright" "Kush Studio"
!define MUI_BGCOLOR "101010"
!define MUI_TEXTCOLOR "EEEEEE"
!define MUI_ICON "${PAYLOAD}\assets\kushclient.ico"
!define MUI_UNICON "${PAYLOAD}\assets\kushclient.ico"
!define MUI_UNCONFIRMPAGE_TEXT_TOP "Remover o programa KushClient deste computador?"
!define MUI_UNCONFIRMPAGE_TEXT_LOCATION "Seus mundos, contas e configurações serão preservados."
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE "PortugueseBR"
Var DesktopOption
Var MenuOption
Var LaunchOption
Var UpdateDirectory
Var WaitParentPID

Function .onInit
  ${IfNot} ${RunningX64}
    MessageBox MB_OK|MB_ICONSTOP "O KushClient precisa do Windows de 64 bits."
    Abort
  ${EndIf}
  ${GetOptions} $CMDLINE "/UPDATE=" $UpdateDirectory
  ${GetOptions} $CMDLINE "/WAITPID=" $WaitParentPID
  ${If} $UpdateDirectory != ""
    ${If} $WaitParentPID == ""
      SetErrorLevel 2
      Abort
    ${EndIf}
    System::Call 'kernel32::OpenProcess(i 0x00100000, i 0, i $WaitParentPID) p .r1'
    ${If} $1 != 0
      System::Call 'kernel32::WaitForSingleObject(p r1, i 120000) i .r2'
      System::Call 'kernel32::CloseHandle(p r1)'
      ${If} $2 != 0
        MessageBox MB_OK|MB_ICONSTOP "Feche o KushClient antes de atualizar. Seus arquivos foram preservados."
        SetErrorLevel 2
        Abort
      ${EndIf}
    ${EndIf}
  ${EndIf}
FunctionEnd

Section "Instalar"
  InitPluginsDir
  SetOutPath "$PLUGINSDIR\app"
  File /r "${PAYLOAD}\*"
  WriteUninstaller "$PLUGINSDIR\Uninstall.exe"
  ClearErrors
  ${If} $UpdateDirectory != ""
    ExecWait '"$PLUGINSDIR\app\runtime\pythonw.exe" "$PLUGINSDIR\app\installer.py" "$PLUGINSDIR\result.ini" --update "$UpdateDirectory"' $0
  ${Else}
    ExecWait '"$PLUGINSDIR\app\runtime\pythonw.exe" "$PLUGINSDIR\app\installer.py" "$PLUGINSDIR\result.ini"' $0
  ${EndIf}
  ${If} ${Errors}
    MessageBox MB_OK|MB_ICONSTOP "Não consegui abrir o instalador. Baixe o arquivo novamente."
    Abort
  ${EndIf}
  ${If} $0 != 0
    MessageBox MB_OK|MB_ICONSTOP "Não foi possível atualizar o KushClient. A instalação anterior e seus dados foram preservados."
    ${If} $UpdateDirectory != ""
      IfFileExists "$UpdateDirectory\KushClient.exe" 0 failed
      Exec '"$UpdateDirectory\KushClient.exe"'
    ${EndIf}
    failed:
    SetErrorLevel 2
    Abort
  ${EndIf}
  IfFileExists "$PLUGINSDIR\result.ini" 0 done
  ReadINIStr $INSTDIR "$PLUGINSDIR\result.ini" "Install" "directory"
  ReadINIStr $DesktopOption "$PLUGINSDIR\result.ini" "Install" "desktop"
  ReadINIStr $MenuOption "$PLUGINSDIR\result.ini" "Install" "startmenu"
  ReadINIStr $LaunchOption "$PLUGINSDIR\result.ini" "Install" "launch"
  IfFileExists "$INSTDIR\KushClient.exe" 0 done
  SetOutPath "$INSTDIR"
  CopyFiles /SILENT "$PLUGINSDIR\Uninstall.exe" "$INSTDIR\Uninstall.exe"
  SetRegView 64
  SetShellVarContext current
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\KushClient" "DisplayName" "KushClient"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\KushClient" "DisplayVersion" "${VERSION}"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\KushClient" "Publisher" "Kush Studio"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\KushClient" "DisplayIcon" "$INSTDIR\KushClient.exe,0"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\KushClient" "InstallLocation" "$INSTDIR"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\KushClient" "URLInfoAbout" "https://kush-archives.com.br/kushclient"
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\KushClient" "EstimatedSize" ${ESTIMATED_SIZE}
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\KushClient" "UninstallString" '$\"$INSTDIR\Uninstall.exe$\"'
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\KushClient" "NoModify" 1
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\KushClient" "NoRepair" 1
  ${If} $DesktopOption == "1"
    CreateShortCut "$DESKTOP\KushClient.lnk" "$INSTDIR\KushClient.exe" "" "$INSTDIR\KushClient.exe"
  ${EndIf}
  ${If} $MenuOption == "1"
    CreateDirectory "$SMPROGRAMS\KushClient"
    CreateShortCut "$SMPROGRAMS\KushClient\KushClient.lnk" "$INSTDIR\KushClient.exe" "" "$INSTDIR\KushClient.exe"
    CreateShortCut "$SMPROGRAMS\KushClient\Desinstalar.lnk" "$INSTDIR\Uninstall.exe"
  ${EndIf}
  ${If} $LaunchOption == "1"
    Exec '"$INSTDIR\KushClient.exe"'
  ${EndIf}
  done:
SectionEnd

Function un.onInit
  SetRegView 64
  SetShellVarContext current
FunctionEnd

Section "Uninstall"
  IfFileExists "$INSTDIR\.kushclient-install.json" 0 invalid
  !include "${REMOVE_INCLUDE}"
  Delete "$INSTDIR\.kushclient-install.json"
  Delete "$INSTDIR\Uninstall.exe"
  Delete "$DESKTOP\KushClient.lnk"
  Delete "$SMPROGRAMS\KushClient\KushClient.lnk"
  Delete "$SMPROGRAMS\KushClient\Desinstalar.lnk"
  RMDir "$SMPROGRAMS\KushClient"
  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\KushClient"
  RMDir "$INSTDIR"
  Goto un_done
  invalid:
  MessageBox MB_OK|MB_ICONSTOP "Não encontrei os arquivos desta instalação. Nenhum arquivo foi removido."
  un_done:
SectionEnd
