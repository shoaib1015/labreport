Set WshShell = CreateObject("WScript.Shell")

' 0 = hidden window
WshShell.Run """start-labreport.bat""", 0, False
