REM ------------------------------------------------
REM Command-line args:
REM --no-console		Launches the bot without console
REM --username:username	Default account name for launcher. Must already be added as account.
REM --height:558		Also: --h. Window height.
REM --width:670			Also: --w. Window width.
REM --x:200				X Location on screen
REM --y:200				Y Location on screen
REM ------------------------------------------------

@echo off

IF NOT EXIST bot.jar (
    echo Missing bot.jar ...
    echo.
    call compile_all.cmd
)

start javaw -jar bot.jar --username:myuser
exit
