APOS for RSC#234
================
[![pipeline status](https://gitlab.com/open-runescape-classic/APOS/badges/master/pipeline.svg)](https://gitlab.com/open-runescape-classic/APOS/-/commits/master)

APOS was developed by RLN/nade/mofo (2009-2011).

APOS was then developed by Stormy from 2012 until 2016.

This source code was originally released by Stormy in 2016.

See also the APOS-Scripts repository, which includes some essential components like PathWalker.

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

* `::menu` Show/hide combat style menu
* `::debug` Show/hide npc/object/wall/item ids

Keyboard stuff
--------------

* Arrow Keys - Camera (hold shift to free-roam).
* Middle Mouse Button - Rotate camera.
* Escape - Resets the camera.
* F12 - Takes a screenshot, saves it to the screenshots directory.
* Ctrl+V - Paste text into the chat.

Using custom fonts
------------------

Edit bot.properties and add the following line:

```
font=Comic Neue
```

with "Comic Neue" replaced with the font you want to use. Unless you want to use Comic Neue.

RSC by default tries to use Helvetica/Arial, but can't if you don't have it installed.
