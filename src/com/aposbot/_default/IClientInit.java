package com.aposbot._default;

import com.aposbot.BotFrame;

public interface IClientInit {

    public IClient createClient(BotFrame frame);

    public IAutoLogin getAutoLogin();

    public ISleepListener getSleepListener();

	public IScriptListener getScriptListener();
	
	public IPaintListener getPaintListener();
	
	public void setRSAKey(String key);
	
	public void setRSAExponent(String exponent);
}
