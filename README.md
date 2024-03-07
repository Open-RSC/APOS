APOS for RSC#234
================
[![pipeline status](https://gitlab.com/open-runescape-classic/APOS/badges/master/pipeline.svg)](https://gitlab.com/open-runescape-classic/APOS/-/commits/master)

APOS was developed by RLN and yomama (2009-2011).

APOS was then developed by Stormy from 2012 until 2016.

This source code was originally released by Stormy in 2016.

![client](/assets/img/client.png)

Requirements
------------

[Java JDK 8](https://adoptium.net/temurin/releases)

**Use the `.msi` installer if you are unsure how to manually set environment variables.**

Either the `JAVA_HOME` env variable must be set to the home directory of JDK,

or the JDK bin directory must be set in `PATH`.

Instructions
------------

1. Use the `compile_all.cmd` file to compile the `bot.jar` client and the included scripts (Windows OS).

2. The bot may be started by double-clicking `bot.jar` or using the `run.cmd` file.

Alternatively, use `ant` on the command-line/terminal to compile and run the client (Windows/Unix OS).

You can also run the client via cli using `java -jar bot.jar`

In-game Commands
--------------

* `::roofs` Show/hide roofs
* `::menu` Show/hide combat style menu
* `::debug` Show/hide npc/object/wall/item ids
* `::window` Show/hide window title debugging for window size and location

Keyboard stuff
--------------

* Arrow Keys - Camera (hold shift to free-roam).
* Middle Mouse Button - Rotate camera.
* Escape - Resets the camera.
* F12 - Takes a screenshot, saves it to the screenshots directory.
* Ctrl+V - Paste text into the chat.

Bot Properties
------------------

These settings relate to the `bot.properties` file within the APOS folder. Create, or edit the file to use the below values.

For custom fonts (replacing Comic Neue with your chosen font. RSC by default tries to use Helvetica/Arial, but can't if you don't have it installed.):

```text
font=Comic Neue
```

To disable the "Sent CAPTCHA: <word>" logging, enter:

```text
captcha_logging=false
```

Using command line arguments
------------------

Execute the bot passing in the arguments below. An account must already be setup and added to the bot
before using the auto launch --username: parameter.

```
--no-console		Launches the bot without console
--username=username	Default account name for launcher. Must already be added as account.
--height=558		Also: --h. Window height. Height and Width must both be set together.
--width=670			Also: --w. Window width. Height and Width must both be set together.
--x=200				X Location on screen. X and Y must both be set together.
--y=200				Y Location on screen. X and Y must both be set together.
```
