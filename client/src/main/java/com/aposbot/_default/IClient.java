package com.aposbot._default;

import javax.script.Invocable;
import java.applet.AppletStub;
import java.awt.*;
import java.awt.event.KeyEvent;

public interface IClient {
	Dimension getPreferredSize();

	void keyPressed(KeyEvent event);

	boolean isRendering();

	void setRendering(boolean enabled);

	boolean isKeysDisabled();

	void setKeysDisabled(boolean b);

	void stopScript();

	void takeScreenshot(String fileName);

        int getCameraRotation();

        void setCameraPosX(int i);

        int getCameraPosX();

        void setCameraPosY(int i);

        int getCameraPosY();

	int getCameraNSOffset();

	void setCameraNSOffset(int i);

	int getCameraEWOffset();

	void setCameraEWOffset(int i);

	int getCameraHeight();

	void setCameraHeight(int i);

	void setActionInd(int i);

	void closeWelcomeBox();

	boolean isDeathScreen();

	int getLocalX();

	int getLocalY();

	int getPlayerWaypointX();

	int getPlayerWaypointY();

	void logout();

	void setLogoutTimer(int i);

	int getAreaX();

	int getAreaY();

	int getBaseLevel(int skill);

	int getCurrentLevel(int skill);

	double getExperience(int skill);

	int getCombatStyle();

	void setCombatStyle(int i);

	String[] getDialogOptions();

	int getDialogOptionCount();

	boolean isDialogVisible();

	void setDialogVisible(boolean b);

	boolean isSleeping();

	void setSleeping(boolean sleeping);

	int getInventorySize();

	int getInventoryId(int i);

	int getInventoryStack(int i);

	boolean isBankVisible();

	void setBankVisible(boolean b);

	int getBankSize();

	int getBankId(int i);

	int getBankStack(int i);

	int getGroundItemCount();

	int getGroundItemLocalX(int i);

	int getGroundItemLocalY(int i);

	int getGroundItemId(int i);

	int getObjectCount();

	int getObjectLocalX(int i);

	int getObjectLocalY(int i);

	int getObjectId(int i);

	int getObjectDir(int i);

	int getBoundCount();

	int getBoundLocalX(int i);

	int getBoundLocalY(int i);

	int getBoundDir(int i);

	int getBoundId(int i);

	void typeChar(char key_char);

	boolean isShopVisible();

	void setShopVisible(boolean b);

	int getShopSize();

	int getShopId(int i);

	int getShopStack(int i);

	boolean isEquipped(int slot);

	boolean isPrayerEnabled(int id);

	void setPrayerEnabled(int i, boolean flag);

	boolean isInTradeOffer();

	void setInTradeOffer(boolean b);

	boolean isInTradeConfirm();

	void setInTradeConfirm(boolean b);

	boolean hasLocalAcceptedTrade();

	boolean hasLocalConfirmedTrade();

	boolean hasRemoteAcceptedTrade();

	int getLocalTradeItemCount();

	int getLocalTradeItemId(int i);

	int getLocalTradeItemStack(int i);

	int getRemoteTradeItemCount();

	int getRemoteTradeItemId(int i);

	int getRemoteTradeItemStack(int i);

	int[] getPixels();

	int[][] getAdjacency();

	void drawString(String str, int x, int y, int size, int colour);

	void displayMessage(String str);

	void offerItemTrade(int slot, int amount);

	void login(String username, String password);

	void walkDirectly(int destLX, int destLY, boolean action);

	void walkAround(int destLX, int destLY);

	void walkToBound(int destLX, int destLY, int dir);

	void walkToObject(int destLX, int destLY, int dir, int id);

	void createPacket(int opcode);

	void put1(int val);

	void put2(int val);

	void put4(int val);

	void finishPacket();

	void sendCAPTCHA(String str);

	boolean isSkipLines();

	void setSkipLines(boolean flag);

	void sendMessage(String message);

	void pressKey(char key);

	void sendPrivateMessage(String msg, String name);

	void addFriend(String str);

	void addIgnore(String str);

	void removeIgnore(String str);

	void removeFriend(String str);

	boolean isLoggedIn();

	int getQuestCount();

	String getQuestName(int i);

	boolean isQuestComplete(int i);

	Image getImage();

	double getAccurateFatigue();

	int getFatigue();

	int getSleepFatigue();

	boolean isFatigueTraining();

	void setFatigueTraining(boolean fatigueTraining);

	int getPlayerCount();
	/**
	*Get the object mob for your player
	 */
	Object getPlayer();
	/**
	 *Get the object mob for a different player, using server index values
	 */
	Object getPlayer(int index);

	String getPlayerName(Object mob);

	int getPlayerCombatLevel(Object mob);

	int getNpcCount();

	int getNpcCacheCount();

	Object getNpc(int index);

	Object getNpcCached(int npcIndex);

	int getNpcId(Object mob);

	int getProjectileDamagedNpcServerIndex();

	int getProjectileDamagedPlayerServerIndex();

	boolean isMobWalking(Object mob);

	boolean isMobTalking(Object mob);

	boolean isHeadIconVisible(Object mob);

	boolean isHpBarVisible(Object mob);

	int getMobBaseHitpoints(Object mob);

	int getMobServerIndex(Object mob);

	int getMobLocalX(Object mob);

	int getMobLocalY(Object mob);

	int getMobDirection(Object mob);

	boolean isMobInCombat(Object mob);

	void setAutoLogin(boolean b);

	IScript createInvocableScript(Invocable inv, String name);

	void stop();

	void init();

	void start();

	void setStub(AppletStub stub);

	void setFont(String name);

	int getCombatTimer();

	void resizeGame(int width, int height);

	int getGameWidth();

	int getGameHeight();

	void onLoggedIn();

	String getServerAddress();

	void setServerAddress(String serverAddress);

	void setAccount(String username, String password);
}
