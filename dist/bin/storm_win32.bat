@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Launch Zomboid Storm
@rem
@rem ##########################################################################

@rem Read Zomboid Version before changing directory
for /f "delims=" %%x in ('type versionFile.txt') do set STORM_VERSION=%%x

@rem Assume launch script is in 'pz/storm/bin' directory
@rem and declare storm and zomboid root directories
set STORM_HOME=%~dp0\..\
pushd %STORM_HOME% 2>nul
set "STORM_HOME=%CD%"

set "PZ_HOME=%STORM_HOME%\..\"

@rem Save current directory and change to target directory
pushd %PZ_HOME% 2>nul
if %ERRORLEVEL% == 1 goto dirNotFoundError

@rem Save value of current directory
set "PZ_HOME=%CD%"

if exist "%PZ_HOME%" goto checkValidDir

:dirNotFoundError
echo.
echo ERROR: directory %PZ_HOME% does not exist or is not accessible
echo.
goto finish

:checkValidDir
if exist "%PZ_HOME%\ProjectZomboid32.exe" goto findJava
echo.
echo ERROR: Unable to find Project Zomboid installation directory.
echo Searched in directory '%PZ_HOME%' as relative to Storm installation directory '%STORM_HOME%\..\'
echo.
goto finish

@rem Find java.exe
:findJava

@rem Search for Java executable in game directory first
set "JAVA_EXE=%PZ_HOME%\jre\bin\java.exe"
if exist "%JAVA_EXE%" goto validateJavaVersion

if defined JAVA_HOME goto findJavaFromJavaHome

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto finish

:findJavaFromJavaHome
set "JAVA_HOME=%JAVA_HOME:"=%"
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

if exist "%JAVA_EXE%" goto validateJavaVersion

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto finish

:validateJavaVersion
@rem Validate java version
"%JAVA_EXE%" -version 2> version.tmp
set /p JAVA_VERSION_INFO=<version.tmp
del version.tmp

set JAVA_VERSION=%JAVA_VERSION_INFO:"=%
for /f "tokens=3 delims=., " %%v in ("%JAVA_VERSION%") do (
	@rem Only valid version is java 17
    if not %%v == 17 goto wrongJavaVersion
)
if "%ERRORLEVEL%" == "0" goto execute

:wrongJavaVersion
echo.
echo ERROR: JAVA_HOME points to a wrong Java version (%JAVA_VERSION%).
echo.
echo Please set your JAVA_HOME variable in your environment to match the
echo location of Java version 1.8 installation.

goto finish

:execute
echo Executing with %JAVA_VERSION_INFO:"=%

@rem Path to Storm jar library directory
set LIB_PATH=%STORM_HOME%\lib

@rem Classpath of Project Zomboid libraries
set PZ_CLASSPATH=%PZ_HOME%\commons-compress-1.18.jar; ^
%PZ_HOME%\istack-commons-runtime.jar; ^
%PZ_HOME%\jassimp.jar; ^
%PZ_HOME%\javacord-2.0.17-shaded.jar; ^
%PZ_HOME%\javax.activation-api.jar; ^
%PZ_HOME%\jaxb-api.jar; ^
%PZ_HOME%\jaxb-runtime.jar; ^
%PZ_HOME%\lwjgl.jar; ^
%PZ_HOME%\lwjgl-natives-windows.jar; ^
%PZ_HOME%\lwjgl-glfw.jar; ^
%PZ_HOME%\lwjgl-glfw-natives-windows.jar; ^
%PZ_HOME%\lwjgl-jemalloc.jar; ^
%PZ_HOME%\lwjgl-jemalloc-natives-windows.jar; ^
%PZ_HOME%\lwjgl-opengl.jar; ^
%PZ_HOME%\lwjgl-opengl-natives-windows.jar; ^
%PZ_HOME%\lwjgl_util.jar; ^
%PZ_HOME%\sqlite-jdbc-3.27.2.1.jar; ^
%PZ_HOME%\trove-3.0.3.jar; ^
%PZ_HOME%\uncommons-maths-1.2.3.jar; ^
%PZ_HOME%

@rem Setup the command line
set CLASSPATH=%LIB_PATH%\storm-@stormVersion@.jar; ^
%LIB_PATH%\asm-9.1.jar; ^
%LIB_PATH%\asm-analysis-9.1.jar; ^
%LIB_PATH%\asm-tree-9.1.jar; ^
%LIB_PATH%\asm-util-9.1.jar; ^
%LIB_PATH%\guava-31.1-jre.jar; ^
%LIB_PATH%\log4j-api-2.17.2.jar; ^
%LIB_PATH%\log4j-core-2.17.2.jar; ^
%PZ_CLASSPATH%


echo Launching Zomboid Storm %STORM_VERSION%...
echo.
"%JAVA_EXE%" -Dzomboid.steam=1 -Dzomboid.znetlog=1 -XX:-CreateCoredumpOnCrash -XX:-OmitStackTraceInFastThrow -Xms768m -Xmx1200m -Djava.library.path="%PZ_HOME%/win32/;%PZ_HOME%" -classpath "%CLASSPATH%" io.pzstorm.storm.core.StormLauncher %*

:finish
exit /b 0
