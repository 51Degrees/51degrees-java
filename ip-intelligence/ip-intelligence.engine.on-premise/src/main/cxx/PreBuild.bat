@ECHO OFF

SETLOCAL enabledelayedexpansion

set argCount=0
for %%x in (%*) do set /A argCount+=1

if %argCount% equ 0 goto build

rem Extract arguments
set count=0
:while
rem Check valid flags
if "%1" equ "/t" (
    rem Do nothing
) else if "%1" equ "/w" (
    rem Do nothing
) else (
    echo "ERROR: Unexpected value. Usage:"
	echo "/t [Platform Toolset Version]"
    echo "/w [Windows SDK Version]"	
    exit /B 1
)

rem get option value
if "%1" equ "/t" (
    set toolsetVersion=%2
) else if "%1" equ "/w" (
	set winSDKVersion=%2
)
rem Go past flag
shift
rem Go past value
shift
set /A count+=2
if %count% lss %argCount% (
    goto while
)

rem Construct build flags. A value of "auto" (the pom default) or an empty
rem value means let CMake pick the toolset and SDK from the installed Visual
rem Studio, so the corresponding flag is omitted.
set additionalFlags=
if /I not "%toolsetVersion%"=="auto" if not "%toolsetVersion%"=="" (
    set additionalFlags=%additionalFlags% -DCMAKE_GENERATOR_TOOLSET=%toolsetVersion%
)

if /I not "%winSDKVersion%"=="auto" if not "%winSDKVersion%"=="" (
    set additionalFlags=%additionalFlags% -DCMAKE_SYSTEM_VERSION=%winSDKVersion%
)

rem Build binaries
:build
if not "%additionalFlags%"=="" (
    echo Build additional options: "%additionalFlags%"
)

rem Remove build folders
if exist target\build64 rmdir /S /Q target\build64

mkdir target\build64
cd target\build64
cmake ..\..\src\main\cxx -A x64 %additionalFlags%
cmake --build . --target fiftyone-ip-intelligence-java --config Release