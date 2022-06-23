package com.aposbot._default;

import com.aposbot.gui.BotFrame;

public interface ILoginListener {
	boolean isEnabled();

	void setEnabled(boolean b);

	void setAccount(String username, String password);

	void setBotFrame(BotFrame botFrame);
}
