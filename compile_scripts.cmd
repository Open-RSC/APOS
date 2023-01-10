@echo off

SET antpath="apache-ant-1.10.12\bin\"

IF NOT EXIST bot.jar (
    call compile_all.cmd
) ELSE (
    echo Compiling scripts ...
    echo.
    call %antpath%ant -f build.xml compile-scripts
)
