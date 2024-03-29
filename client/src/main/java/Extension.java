import com.aposbot._default.IClient;
import com.aposbot._default.IScript;
import com.aposbot.handler.CameraHandler;
import com.aposbot.handler.KeyboardHandler;
import com.aposbot.handler.MouseHandler;
import com.aposbot.Constants;

import javax.script.Invocable;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.math.BigInteger;

/**
 * Extends the mudclient and provides human-readable accessors.
 */
public class Extension extends client implements IClient {
	private static final Extension instance = new Extension();

	private static final long serialVersionUID = 1L;

	private static final int MAX_FATIGUE = 750;

	// Injected
	public static BigInteger exponent;
	public static BigInteger modulus;

	private boolean disableKeys;

	private boolean fatigueTraining;

	private Extension() {
		super();
		// Increase the size of buffers to prevent overflowing on
		// the incredibly crowded worlds of RSC Uranium.
		// XXX this should be fixed on the server too...
		super.mg = new ja(70000);
		// overhead icons...
		super.je = new int[512];
		super.pe = new int[512];
		super.jd = new int[512];
		super.ak = new int[512];
	}

	public static void initHook() {
		client.il[237] = "Welcome to @cya@APOS @whi@v3.0.0";

		Extension.modulus = new BigInteger(Constants.RSAKEY_URANIUM_MEMB);
		Extension.exponent = new BigInteger(Constants.RSAEXPONENT_URANIUM_MEMB);

		instance.addMouseListener(MouseHandler.getInstance());
		instance.addMouseMotionListener(MouseHandler.getInstance());
		instance.addMouseWheelListener(MouseHandler.getInstance());
		instance.addKeyListener(KeyboardHandler.createInstance(instance));

		instance.setFocusTraversalKeysEnabled(false);

		CameraHandler.init(instance);
	}

	static Extension getInstance() {
		return instance;
	}

	@Override
	public boolean isRendering() {
		return PaintListener.renderGraphics;
	}

	@Override
	public void setRendering(final boolean rendering) {
		PaintListener.renderGraphics = rendering;
	}

	@Override
	public boolean isKeysDisabled() {
		return disableKeys;
	}

	@Override
	public void setKeysDisabled(final boolean b) {
		disableKeys = b;
	}

	@Override
	public void stopScript() {
		ScriptListener.getInstance().setScriptRunning(false);
	}

	@Override
	public void takeScreenshot(final String fileName) {
	}

	@Override
	public int getCameraRotation() {
		return ug;
	}

	@Override
	public int getCameraPosX() {
		return kg;
	}

	@Override
	public void setCameraPosX(int i) {
		kg = i;
	}

	@Override
	public int getCameraPosY() {
		return Si;
	}

	@Override
	public void setCameraPosY(int i) {
		Si = i;
	}

	@Override
	public int getCameraNSOffset() {
		return oc;
	}

	@Override
	public void setCameraNSOffset(final int i) {
		oc = i;
	}

	@Override
	public int getCameraEWOffset() {
		return Be;
	}

	@Override
	public void setCameraEWOffset(final int i) {
		Be = i;
	}

	@Override
	public int getCameraHeight() {
		return ac;
	}

	@Override
	public void setCameraHeight(final int i) {
		ac = i;
	}

	@Override
	public void setActionInd(final int i) {
		xh = i;
	}

	@Override
	public void closeWelcomeBox() {
		Oh = false;
	}

	@Override
	public boolean isDeathScreen() {
		return rk > 0;
	}

	@Override
	public int getLocalX() {
		return Lf;
	}

	@Override
	public int getLocalY() {
		return sh;
	}

	@Override
	public int getPlayerWaypointX() {
		return wi.i;
	}

	@Override
	public int getPlayerWaypointY() {
		return wi.K;
	}

	@Override
	public void logout() {
		createPacket(Constants.OP_LOGOUT);
		finishPacket();
		setLogoutTimer(1000);
	}

	@Override
	public void setLogoutTimer(final int i) {
		bj = i;
	}

	@Override
	public int getAreaX() {
		return Qg;
	}

	@Override
	public int getAreaY() {
		return zg;
	}

	@Override
	public int getBaseLevel(final int skill) {
		return cg[skill];
	}

	@Override
	public int getCurrentLevel(final int skill) {
		return oh[skill];
	}

	@Override
	public double getExperience(final int skill) {
		return Ak[skill] / 4.0;
	}

	@Override
	public int getCombatStyle() {
		return Fg;
	}

	@Override
	public void setCombatStyle(final int i) {
		Fg = i;
	}

	@Override
	public String[] getDialogOptions() {
		return ah;
	}

	@Override
	public int getDialogOptionCount() {
		return Id;
	}

	@Override
	public boolean isDialogVisible() {
		return Ph;
	}

	@Override
	public void setDialogVisible(final boolean b) {
		Ph = false;
	}

	@Override
	public boolean isSleeping() {
		return Qk;
	}

	@Override
	public void setSleeping(final boolean sleeping) {
		Qk = sleeping;
		Yk = false;
	}

	@Override
	public int getInventorySize() {
		return lc;
	}

	@Override
	public int getInventoryId(final int i) {
		return vf[i];
	}

	@Override
	public int getInventoryStack(final int i) {
		return xe[i];
	}

	@Override
	public boolean isBankVisible() {
		return Fe;
	}

	@Override
	public void setBankVisible(final boolean b) {
		Fe = b;
	}

	@Override
	public int getBankSize() {
		return vj;
	}

	@Override
	public int getBankId(final int i) {
		return ae[i];
	}

	@Override
	public int getBankStack(final int i) {
		return di[i];
	}

	@Override
	public int getGroundItemCount() {
		return Ah;
	}

	@Override
	public int getGroundItemLocalX(final int i) {
		return Zf[i];
	}

	@Override
	public int getGroundItemLocalY(final int i) {
		return Ni[i];
	}

	@Override
	public int getGroundItemId(final int i) {
		return Gj[i];
	}

	@Override
	public int getObjectCount() {
		return eh;
	}

	@Override
	public int getObjectLocalX(final int i) {
		return Se[i];
	}

	@Override
	public int getObjectLocalY(final int i) {
		return ye[i];
	}

	@Override
	public int getObjectId(final int i) {
		return vc[i];
	}

	@Override
	public int getObjectDir(final int i) {
		return bg[i];
	}

	@Override
	public int getBoundCount() {
		return hf;
	}

	@Override
	public int getBoundLocalX(final int i) {
		return Jd[i];
	}

	@Override
	public int getBoundLocalY(final int i) {
		return yk[i];
	}

	@Override
	public int getBoundDir(final int i) {
		return Hj[i];
	}

	@Override
	public int getBoundId(final int i) {
		return Ng[i];
	}

	@Override
	public void typeChar(final char key_char) {
		// TODO: keep shift down
		final boolean upper = Character.isUpperCase(key_char);
		final int key_code = KeyEvent.getExtendedKeyCodeForChar(key_char);
		int m = 0;
		if (upper) {
			super.keyPressed(new KeyEvent(this,
				KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
				0, KeyEvent.VK_SHIFT, KeyEvent.CHAR_UNDEFINED));
			m |= KeyEvent.VK_SHIFT;
		}
		super.keyPressed(new KeyEvent(this, KeyEvent.KEY_PRESSED,
			System.currentTimeMillis(), m, key_code, key_char));
		super.keyTyped(new KeyEvent(this, KeyEvent.KEY_TYPED,
			System.currentTimeMillis(), m,
			KeyEvent.VK_UNDEFINED, key_char));
		super.keyReleased(new KeyEvent(this, KeyEvent.KEY_RELEASED,
			System.currentTimeMillis(), m, key_code, key_char));
		if (upper) {
			super.keyReleased(new KeyEvent(this,
				KeyEvent.KEY_RELEASED, System.currentTimeMillis(),
				0, KeyEvent.VK_SHIFT, KeyEvent.CHAR_UNDEFINED));
		}
	}

	@Override
	public boolean isShopVisible() {
		return uk;
	}

	@Override
	public void setShopVisible(final boolean b) {
		uk = b;
	}

	@Override
	public int getShopSize() {
		return Rj.length;
	}

	@Override
	public int getShopId(final int i) {
		return Rj[i];
	}

	@Override
	public int getShopStack(final int i) {
		return Jf[i];
	}

	@Override
	public boolean isEquipped(final int slot) {
		return Aj[slot] == 1;
	}

	@Override
	public boolean isPrayerEnabled(final int id) {
		return bk[id];
	}

	@Override
	public void setPrayerEnabled(final int i, final boolean flag) {
		bk[i] = flag;
	}

	@Override
	public boolean isInTradeOffer() {
		return Hk;
	}

	@Override
	public void setInTradeOffer(final boolean b) {
		Hk = b;
	}

	@Override
	public boolean isInTradeConfirm() {
		return Xj;
	}

	@Override
	public void setInTradeConfirm(final boolean b) {
		Xj = b;
	}

	@Override
	public boolean hasLocalAcceptedTrade() {
		return Mi;
	}

	@Override
	public boolean hasLocalConfirmedTrade() {
		return Vi;
	}

	@Override
	public boolean hasRemoteAcceptedTrade() {
		return md;
	}

	@Override
	public int getLocalTradeItemCount() {
		return mf;
	}

	@Override
	public int getLocalTradeItemId(final int i) {
		return Qf[i];
	}

	@Override
	public int getLocalTradeItemStack(final int i) {
		return jj[i];
	}

	@Override
	public int getRemoteTradeItemCount() {
		return Lk;
	}

	@Override
	public int getRemoteTradeItemId(final int i) {
		return zj[i];
	}

	@Override
	public int getRemoteTradeItemStack(final int i) {
		return Dd[i];
	}

	@Override
	public int[] getPixels() {
		return getScreen().rb;
	}

	private ba getScreen() {
		return li;
	}

	@Override
	public int[][] getAdjacency() {
		return Hh.bb;
	}

	@Override
	public void drawString(final String str, final int x, final int y, final int font, final int colour) {
		/* use xb so our text has shadows */
		final boolean orig = getScreen().xb;
		getScreen().xb = true;
		getScreen().a(str, x, y, colour, false, font);
		getScreen().xb = orig;
	}

	@Override
	public void displayMessage(final String str) {
		a(false, null, 0, str, 7, 0, null, null);
	}

	@Override
	public void offerItemTrade(final int slot, final int amount) {
		a(amount, (byte) 9, slot);
	}

	@Override
	public void login(final String username, final String password) {
		Vh = 2;
		a(-12, password, username, false);
	}

	@Override
	public void walkDirectly(final int destLX, final int destLY, final boolean action) {
		a(destLY, destLX, sh, Lf, action, 8);
	}

	@Override
	public void walkAround(final int destLX, final int destLY) {
		a((byte) 10, sh, destLY, destLX, true, Lf);
	}

	@Override
	public void walkToBound(final int destLX, final int destLY, final int dir) {
		a(false, destLX, destLY, dir);
	}

	@Override
	public void walkToObject(final int destLX, final int destLY, final int dir, final int id) {
		b(5126, id, destLX, destLY, dir);
	}

	@Override
	public void createPacket(final int opcode) {
		Jh.b(opcode, 0);
	}

	@Override
	public void put1(final int val) {
		Jh.f.c(val, 115);
	}

	@Override
	public void put2(final int val) {
		Jh.f.e(393, val);
	}

	@Override
	public void put4(final int val) {
		Jh.f.b(-422797528, val);
	}

	@Override
	public void finishPacket() {
		Jh.b(21294);
	}

	@Override
	public void sendCAPTCHA(final String str) {
		Jh.b(45, 0);
		if (Yk) {
			Jh.f.c(1, -75);
		} else {
			Jh.f.c(0, -100);
			Yk = true;
		}
		Jh.f.a(str, 116);
		Jh.b(21294);
		e = "";
		Zj = il[436];
		Cb = "";
	}

	@Override
	public boolean isSkipLines() {
		return U;
	}

	@Override
	public void setSkipLines(final boolean flag) {
		U = flag;
	}

	@Override
	public void sendMessage(final String message) {
		for (final char c : message.toCharArray()) {
			pressKey(c);
		}

		pressKey('\n');
	}

	@Override
	public void pressKey(final char key) {
		keyPressed(new KeyEvent(this, KeyEvent.KEY_TYPED, 1, 0, KeyEvent.VK_UNDEFINED, key));
	}

	@Override
	public void sendPrivateMessage(final String msg, String name) {
		name = name.replace(' ', (char) 160);
		x = "";
		Bj = 0;
		Ob = "";
		Qd = name;
		a((byte) -76, name, msg);
	}

	@Override
	public void addFriend(String str) {
		str = str.replace(' ', (char) 160);
		Bj = 0;
		e = "";
		Cb = "";
		b(114, str);
	}

	@Override
	public void addIgnore(String str) {
		str = str.replace(' ', (char) 160);
		e = "";
		Bj = 0;
		Cb = "";
		a(str, (byte) 5);
	}

	@Override
	public void removeIgnore(String str) {
		str = str.replace(' ', (char) 160);
		//a((byte)-15, ia.a[j3]);
		a((byte) -15, str);
	}

	@Override
	public void removeFriend(String str) {
		str = str.replace(' ', (char) 160);
		//b(ua.h[i3], (byte)69);
		b(str, (byte) 69);
	}

	@Override
	public boolean isLoggedIn() {
		return getScreen() != null && qg == 1;
	}

	@Override
	public int getQuestCount() {
		return Te.length;
	}

	@Override
	public String getQuestName(final int i) {
		return Te[i];
	}

	@Override
	public boolean isQuestComplete(final int i) {
		return fi[i];
	}

	@Override
	public Image getImage() {
		return getScreen().Gb;
	}

	@Override
	public double getAccurateFatigue() {
		return (vg * 100.0) / 750.0;
	}

	@Override
	public int getFatigue() {
		return vg;
	}

	@Override
	public int getSleepFatigue() {
		return pg;
	}

	@Override
	public boolean isFatigueTraining() {
		return fatigueTraining;
	}

	@Override
	public void setFatigueTraining(final boolean fatigueTraining) {
		this.fatigueTraining = fatigueTraining;
	}

	@Override
	public int getPlayerCount() {
		return Yc;
	}

	@Override
	public Object getPlayer() {
		return wi;
	}

	@Override
	public Object getPlayer(final int index) {
		return rg[index];
	}

	@Override
	public String getPlayerName(final Object mob) {
		return ((ta) mob).c.replace((char) 160, ' ');
	}

	@Override
	public int getPlayerCombatLevel(final Object mob) {
		return ((ta) mob).s;
	}

	@Override
	public int getNpcCount() {
		return de;
	}

	@Override
	public int getNpcCacheCount() {
		return qj;
	}

	@Override
	public Object getNpc(final int index) {
		return Tb[index];
	}

	@Override
	public Object getNpcCached(final int npcIndex) {
		return Ff[npcIndex];
	}

	@Override
	public int getNpcId(final Object mob) {
		return ((ta) mob).t;
	}

	@Override
	public int getProjectileDamagedNpcServerIndex() {
		return wi.h;
	}

	@Override
	public int getProjectileDamagedPlayerServerIndex() {
		return wi.z;
	}

	@Override
	public boolean isMobWalking(final Object mob) {
		return ((ta) mob).e != (((ta) mob).o + 1) % 10;
	}

	@Override
	public boolean isMobTalking(final Object mob) {
		return ((ta) mob).I > 0;
	}

	@Override
	public boolean isHeadIconVisible(final Object mob) {
		return ((ta) mob).E > 0;
	}

	@Override
	public boolean isHpBarVisible(final Object mob) {
		return ((ta) mob).d > 0;
	}

	@Override
	public int getMobBaseHitpoints(final Object mob) {
		return ((ta) mob).G;
	}

	@Override
	public int getMobServerIndex(final Object mob) {
		return ((ta) mob).b;
	}

	@Override
	public int getMobLocalX(final Object mob) {
		return (((ta) mob).i - 64) / getMobLocDivide();
	}

	private int getMobLocDivide() {
		return Ug;
	}

	@Override
	public int getMobLocalY(final Object mob) {
		return (((ta) mob).K - 64) / getMobLocDivide();
	}

	@Override
	public int getMobDirection(final Object mob) {
		return ((ta) mob).y;
	}

	@Override
	public boolean isMobInCombat(final Object mob) {
		final int dir = getMobDirection(mob);
		return dir == 8 || dir == 9;
	}

	@Override
	public void setAutoLogin(final boolean b) {
		LoginListener.getInstance().setEnabled(b);
	}

	@Override
	public IScript createInvocableScript(final Invocable inv, final String name) {
		return new JavaxScriptInvocable(this, inv, name);
	}

	@Override
	public void setFont(final String name) {
		StaticAccess.loadFont(this, "h11p", name, 0);
		StaticAccess.loadFont(this, "h12b", name, 1);
		StaticAccess.loadFont(this, "h12p", name, 2);
		StaticAccess.loadFont(this, "h13b", name, 3);
		StaticAccess.loadFont(this, "h14b", name, 4);
		StaticAccess.loadFont(this, "h16b", name, 5);
		StaticAccess.loadFont(this, "h20b", name, 6);
		StaticAccess.loadFont(this, "h24b", name, 7);
	}

	@Override
	public int getCombatTimer() {
		return ai;
	}

	@Override
	public void resizeGame(final int width, final int height) {
		Wd = width;
		Oi = height - 12;

		Rh = width;
		Hf = height;
		Eb = (-Wd + Rh) / 2;
		K = 0;

		// set raster
		li.rb = new int[width * height];
		li.lb = li.u = width;
		li.Rb = li.k = height;
		li.fb.setDimensions(width, height);

		/*
		li.Gb = this.createImage(li);
		for (int i = 0; i < 3; ++i) {
			li.b(true);
			this.prepareImage(li.Gb, this);
		}

		System.out.println(li.Gb.getWidth(null) + " " + li.Gb.getHeight(null));
		*/

		// set scene
		Ek.pb = li.rb;
		Ek.A = width;
		Ek.wb = height;
		Ek.a(Oi / 2, true, Wd, Wd / 2, Oi / 2, qd, Wd / 2);

		// make interfaces
		O(56);

		Mc = new qa(li, 5);
		Ud = Mc.a(li.u - 199, 196, 90,
			true, 106, 500, 24 + 36, 1);

		zk = new qa(li, 5);
		Hi = zk.a(li.u - 199, 196, 126,
			true, 106, 500, 36 + 40, 1);

		fe = new qa(li, 5);
		lk = fe.a(li.u - 199, 196, 251,
			true, 106, 500, 24 + 36, 1);

		if ((width - 73) < 1000) {
			client.il[262] = String.format(
				"~%d~@whi@Remove         WWWWWWWWWW", width - 73);
		} else {
			client.il[262] = "";
		}

		Xb = getGraphics();
		//repaint();
	}

	@Override
	public int getGameWidth() {
		return Wd;
	}

	@Override
	public int getGameHeight() {
		return Oi + 12;
	}

	@Override
	public void onLoggedIn() {
	}

	@Override
	public String getServerAddress() {
		return Dh;
	}

	@Override
	public void setServerAddress(final String serverAddress) {
		Dh = serverAddress;
	}

	@Override
	public void setAccount(final String username, final String password) {
		LoginListener.getInstance().setAccount(username, password);
	}

	@Override
	public void keyPressed(final KeyEvent e) {
		ScriptListener.getInstance().onKeyPress(e);

		if (!disableKeys && !e.isConsumed()) {
			super.keyPressed(e);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(Wd, Oi + 12);
	}
}
