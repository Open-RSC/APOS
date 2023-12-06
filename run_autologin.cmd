REM ------------------------------------------------
REM Command-line args:
REM --no-console		Launches the bot without console
REM --username:username	Default account name for launcher. Must already be added as account.
REM --height:558		Also: --h. Window height. Height and Width must both be set together.
REM --width:670			Also: --w. Window width. Height and Width must both be set together.
REM --x:200				X Location on screen. X and Y must both be set together.
REM --y:200				Y Location on screen. X and Y must both be set together.
REM ------------------------------------------------

@echo off

IF NOT EXIST bot.jar (
    echo Missing bot.jar ...
    echo.
    call compile_all.cmd
)

start javaw -jar bot.jar --username:myuser
exit
