package com.aposbot._default;

public interface IScript {

    void init(String params);

    int main();

    void paint();

    void onServerMessage(String str);

    void onTradeRequest(String name);

    void onChatMessage(String msg, String name, boolean mod,
                       boolean admin);

    void onPrivateMessage(String msg, String name, boolean mod,
                          boolean admin);

    void onKeyPress(int keycode);

    void onPlayerDamaged(Object player);

    void onNpcDamaged(Object npc);

    void onNpcSpawned(Object npc);

    void onNpcProjectileDamaged(Object player);

    void onDeath();

    void onGroundItemSpawned(int groundItemIndex);

    void onGroundItemDespawned(int id, int x, int y);

    boolean isSleeping();

    int getFatigue();

    boolean isTricking();

    void logout();
}
