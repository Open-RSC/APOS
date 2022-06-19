@echo off
SET antpath="apache-ant-1.10.12\bin\"

echo Compiling bot and scripts ...
echo.
call %antpath%ant -f build.xml compile-all
pause
