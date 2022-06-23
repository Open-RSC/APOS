package com.aposbot._default;

import com.aposbot.gui.BotFrame;

import java.awt.event.KeyEvent;

public interface IScriptListener {
	void onPaintTick();

	void onKeyPress(KeyEvent e);

	boolean onCommand(String command);

	void setIScript(IScript script);

	boolean isScriptRunning();

	void setScriptRunning(boolean b);

	String getScriptName();

	boolean hasScript();

	void setBotFrame(BotFrame botFrame);
}
