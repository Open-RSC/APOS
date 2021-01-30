package com.aposbot._default;

import java.applet.AppletStub;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.KeyEvent;

import javax.script.Invocable;

public interface IClient {
	
	public void setParentInit(IClientInit init);
	
	public IClientInit getParentInit();

	public Dimension getPreferredSize();

	public void keyPressed(KeyEvent event);

	public void setRendering(boolean enabled);

	public boolean isRendering();

	public void setAutoLogin(boolean b);

	public void setKeysDisabled(boolean b);

	public void stopScript();

	public void takeScreenshot(String fileName);

	public void setServer(int id);

	public int getServer();

	public int getCameraNSOffset();

	public void setCameraNSOffset(int i);

	public int getCameraEWOffset();

	public void setCameraEWOffset(int i);

	public int getCameraHeight();

	public void setCameraHeight(int i);

	public void setActionInd(int i);

	public void closeWelcomeBox();

	public int getLocalX();

	public int getLocalY();

	public void setLogoutTimer(int i);

	public int getAreaX();

	public int getAreaY();

	public int getBaseLevel(int skill);

	public int getCurrentLevel(int skill);

	public double getExperience(int skill);

	public int getCombatStyle();

	public void setCombatStyle(int i);
	
	public String[] getDialogOptions();

	public int getDialogOptionCount();

	public boolean isDialogVisible();

	public void setDialogVisible(boolean b);

	public boolean isSleeping();

	public int getInventorySize();

	public int getInventoryId(int i);

	public int getInventoryStack(int i);

	public boolean isBankVisible();

	public void setBankVisible(boolean b);

	public int getBankSize();

	public int getBankId(int i);

	public int getBankStack(int i);

	public int getGroundItemCount();

	public int getGroundItemLocalX(int i);

	public int getGroundItemLocalY(int i);

	public int getGroundItemId(int i);

	public int getObjectCount();

	public int getObjectLocalX(int i);

	public int getObjectLocalY(int i);

	public int getObjectId(int i);

	public int getObjectDir(int i);

	public int getBoundCount();

	public int getBoundLocalX(int i);

	public int getBoundLocalY(int i);

	public int getBoundDir(int i);

	public int getBoundId(int i);

	public void typeChar(char key_char, int key_code);

	public boolean isShopVisible();

	public void setShopVisible(boolean b);

	public int getShopSize();

	public int getShopId(int i);

	public int getShopStack(int i);

	public boolean isEquipped(int slot);

	public boolean isPrayerEnabled(int id);

	public void setPrayerEnabled(int i, boolean flag);

	public boolean isInTradeOffer();

	public void setInTradeOffer(boolean b);

	public boolean isInTradeConfirm();

	public void setInTradeConfirm(boolean b);

	public boolean hasLocalAcceptedTrade();

	public boolean hasLocalConfirmedTrade();

	public boolean hasRemoteAcceptedTrade();

	public int getLocalTradeItemCount();

	public int getLocalTradeItemId(int i);

	public int getLocalTradeItemStack(int i);

	public int getRemoteTradeItemCount();

	public int getRemoteTradeItemId(int i);

	public int getRemoteTradeItemStack(int i);

	public int[] getPixels();

	public int[][] getAdjacency();

	public void drawString(String str, int x, int y, int size, int colour);

	public void displayMessage(String str);

	public void offerItemTrade(int slot, int amount);

	public void login(String username, String password);

	public void walkDirectly(int destLX, int destLY, boolean action);

	public void walkAround(int destLX, int destLY);

	public void walkToBound(int destLX, int destLY, int dir);

	public void walkToObject(int destLX, int destLY, int dir, int id);

	public void createPacket(int opcode);

	public void put1(int val);

	public void put2(int val);

	public void put4(int val);

	public void finishPacket();

	public void sendCAPTCHA(String str);

	public boolean isSkipLines();

	public void setSkipLines(boolean flag);

	public void sendPrivateMessage(String msg, String name);

	public void addFriend(String str);

	public void addIgnore(String str);

	public void removeIgnore(String str);

	public void removeFriend(String str);

	public boolean isLoggedIn();

	public int getQuestCount();

	public String getQuestName(int i);

	public boolean isQuestComplete(int i);

	public Image getImage();

	public double getFatigue();

	public double getSleepingFatigue();

	public int getPlayerCount();

	public Object getPlayer();

	public Object getPlayer(int index);

	public String getPlayerName(Object mob);

	public int getPlayerCombatLevel(Object mob);

	public int getNpcCount();

	public Object getNpc(int index);

	public int getNpcId(Object mob);

	public boolean isMobWalking(Object mob);

	public boolean isMobTalking(Object mob);

	public boolean isHeadIconVisible(Object mob);

	public boolean isHpBarVisible(Object mob);

	public int getMobBaseHitpoints(Object mob);

	public int getMobServerIndex(Object mob);

	public int getMobLocalX(Object mob);

	public int getMobLocalY(Object mob);

	public int getMobDirection(Object mob);

	public boolean isMobInCombat(Object mob);

	public IStaticAccess getStatic();

	public IScriptListener getScriptListener();

	public IAutoLogin getAutoLogin();

	public IPaintListener getPaintListener();

	public IJokerFOCR getJoker();
	
	public IScript createInvocableScript(Invocable inv, String name);

	public void stop();

	public void init();

	public void start();

	public void setStub(AppletStub stub);

	public void setFont(String name);

	public int getCombatTimer();

	public void resizeGame(int width, int height);

	public int getGameWidth();

	public int getGameHeight();

	public void onLoggedIn();
	
	public void setServerAddress(String serverAddress);
	
	public String getServerAddress();
}
