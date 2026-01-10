Set WshShell = CreateObject("WScript.Shell")
WshShell.Run """" & WScript.ScriptFullName & "\..\start-labreport.bat""", 0, False
