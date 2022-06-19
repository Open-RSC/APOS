package com.aposbot._default;

import java.awt.event.KeyEvent;

public interface IScript {
	void init(String params);

	int main();

	void paint();

	void onServerMessage(String str);

	void onTradeRequest(String name);

	void onChatMessage(String msg, String name, boolean mod, boolean admin);

	void onPrivateMessage(String msg, String name, boolean mod, boolean admin);

	boolean onCommand(String command);

	void onKeyPress(KeyEvent e);

	void onPlayerDamaged(Object player);

	void onNpcDamaged(Object npc);

	void onNpcSpawned(Object npc);

	void onNpcDespawned(Object npc);

	void onDeath();

	void onGroundItemSpawned(int groundItemIndex);

	void onGroundItemDespawned(int groundItemIndex);

	void onObjectSpawned(int objectIndex);

	void onObjectDespawned(int objectIndex);

	void onPlayerCoord(int x, int y);
}
