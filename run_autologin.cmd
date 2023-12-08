@echo off

IF NOT EXIST bot.jar (
    echo Missing bot.jar ...
    echo.
    call compile_all.cmd
)

start javaw -jar bot.jar --username=myuser
exit
