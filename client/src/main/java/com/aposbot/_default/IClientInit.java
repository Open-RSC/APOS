package com.aposbot._default;

public interface IClientInit {
	IClient getClient();

	ILoginListener getLoginListener();

	ISleepListener getSleepListener();

	IScriptListener getScriptListener();

	IPaintListener getPaintListener();

	IStaticAccess getStaticAccess();

	IJokerFOCR getJoker();
}
