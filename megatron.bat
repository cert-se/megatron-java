@echo off & setlocal ENABLEDELAYEDEXPANSION

set MEGATRON_HOME=%~p0
cd %MEGATRON_HOME%

set MEGATRON_JAVA=java
set MEGATRON_JAVA_OPTIONS=-Xmx256M -showversion
REM set MEGATRON_CLASSPATH=classes-eclipse;conf/dev;conf/hibernate-mapping
set MEGATRON_CLASSPATH=dist/sitic-megatron.jar;conf/dev;conf/hibernate-mapping
for %%1 in (lib\*.jar) do set MEGATRON_CLASSPATH=!MEGATRON_CLASSPATH!;%%1

echo %date% %time% :: Megatron Starts. 
%MEGATRON_JAVA% %MEGATRON_JAVA_OPTIONS% -cp %MEGATRON_CLASSPATH% -Dmegatron.configfile=conf/dev/megatron-globals.properties Megatron %*
echo %date% %time% :: Megatron Finished.
