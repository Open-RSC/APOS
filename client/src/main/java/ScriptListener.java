import com.aposbot._default.IClient;
import com.aposbot._default.IScript;
import com.aposbot._default.IScriptListener;
import com.aposbot.gui.BotFrame;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Listens for client events to pass to running scripts.
 * Calls the main script loop every game tick.
 */
public final class ScriptListener implements IScriptListener {
	private static final String ERROR_MESSAGE = "Error processing script. Send this output to the script's author:";

	private static final ScriptListener instance = new ScriptListener();

	private final Set<Object> despawnedNpcs = new HashSet<>();

	private final IClient client;
	private final SleepListener sleepListener;
	private long nextRefresh = -1;
	private long nextDeRefresh = -1;
	private IScript script;
	private BotFrame botFrame;

	private long next;

	private boolean running;

	private ScriptListener() {
		client = Extension.getInstance();
		sleepListener = SleepListener.getInstance();
	}

	public static IScriptListener getInstance() {
		return instance;
	}

	static void runScriptHook() {
		instance.onRunScript();
	}

	private void onRunScript() {

		if (!running) return;

		if (client.isSleeping()) {
			sleepListener.onGameTick();
			return;
		}
		if (System.currentTimeMillis() < next) return;

		try {
			next = System.currentTimeMillis() + script.main();
		} catch (final Throwable t) {
			System.err.println(ERROR_MESSAGE);
			t.printStackTrace();
		}
	}

	static void gameMessageHook(final boolean flag, final String s1, final int i1, final String s2, final int j1, final int k1, final String s3,
								final String s4) {
		instance.onGameMessage(flag, s1, i1, s2, j1, k1, s3, s4);
	}

	private void onGameMessage(final boolean flag, String s1, final int i1, final String s2, final int j1, final int k1, final String s3,
							   final String s4) {
		if(!client.isRendering() && (System.currentTimeMillis() > nextRefresh) && (nextRefresh != -1)) {
				client.setRendering(true);
				nextDeRefresh = System.currentTimeMillis() + 20L;
				nextRefresh = System.currentTimeMillis() + 60000L; //wait for 1 min till refreshing
				//System.out.println("Next screen refresh in: " + ((nextRefresh - System.currentTimeMillis())/1000L) + "s");
		}
		if (running) {
			if (s1 != null) {
				s1 = s1.replace((char) 160, ' ');
			}
			try {
				if (j1 == 1) {
					script.onPrivateMessage(s2, s1, k1 == 1, k1 >= 2);
				} else if (j1 == 0 || j1 == 3) {
					script.onServerMessage(s2);
				} else if (j1 == 4) {
					script.onChatMessage(s2, s1, k1 == 1, k1 >= 2);
				} else if (j1 == 6) {
					script.onTradeRequest(s1);
				}
			} catch (final Throwable t) {
				System.err.println(ERROR_MESSAGE);
				t.printStackTrace();
			}
		}
	}

	static void playerDamagedHook(final ta player) {
		instance.onPlayerDamaged(player);
	}
	/**
	 * Executes when a player is damaged.
	 *
	 * @param  player  the player object that was damaged
	 */
	private void onPlayerDamaged(final Object player) {
		if (!running) {
			return;
		}

		try {
			script.onPlayerDamaged(player);
		} catch (final Throwable t) {
			System.err.println(ERROR_MESSAGE);
			t.printStackTrace();
		}
	}

	static void npcDamagedHook(final ta npc) {
		instance.onNpcDamaged(npc);
	}

	private void onNpcDamaged(final Object npc) {
		if (!running) {
			return;
		}

		try {
			script.onNpcDamaged(npc);
		} catch (final Throwable t) {
			System.err.println(ERROR_MESSAGE);
			t.printStackTrace();
		}
	}

	static void npcSpawnedHook(final ta npc) {
		instance.onNpcSpawned(npc);
	}

	private void onNpcSpawned(final Object npc) {
		if (!running) {
			return;
		}

		try {
			script.onNpcSpawned(npc);
		} catch (final Throwable t) {
			System.err.println(ERROR_MESSAGE);
			t.printStackTrace();
		}
	}

	static void npcUpdateHook() {
		instance.onNpcUpdate();
	}

	private void onNpcUpdate() {
		if (!running) {
			return;
		}

		for (int i = 0; i < client.getNpcCacheCount(); i++) {
			despawnedNpcs.add(client.getNpcCached(i));
		}

		for (int i = 0; i < client.getNpcCount(); i++) {
			despawnedNpcs.remove(client.getNpc(i));
		}

		if (despawnedNpcs.isEmpty()) {
			return;
		}

		try {
			for (final Object npc : despawnedNpcs) {
				script.onNpcDespawned(npc);
			}
		} catch (final Throwable t) {
			System.err.println(ERROR_MESSAGE);
			t.printStackTrace();
		} finally {
			despawnedNpcs.clear();
		}
	}

	static void deathHook() {
		instance.onDeath();
	}

	private void onDeath() {
		if (!running) {
			return;
		}

		try {
			script.onDeath();
		} catch (final Throwable t) {
			System.err.println(ERROR_MESSAGE);
			t.printStackTrace();
		}
	}

	static void groundItemSpawnedHook(final int groundItemIndex) {
		instance.onGroundItemSpawned(groundItemIndex);
	}

	private void onGroundItemSpawned(final int groundItemIndex) {
		if (!running) {
			return;
		}

		try {
			script.onGroundItemSpawned(groundItemIndex);
		} catch (final Throwable t) {
			System.err.println(ERROR_MESSAGE);
			t.printStackTrace();
		}
	}

	static void groundItemDespawnedHook(final int groundItemIndex) {
		instance.onGroundItemDespawned(groundItemIndex);
	}

	private void onGroundItemDespawned(final int groundItemIndex) {
		if (!running) {
			return;
		}

		try {
			script.onGroundItemDespawned(groundItemIndex);
		} catch (final Throwable t) {
			System.err.println(ERROR_MESSAGE);
			t.printStackTrace();
		}
	}

	static void objectSpawnedHook(final int objectIndex) {
		instance.onObjectSpawned(objectIndex);
	}

	private void onObjectSpawned(final int objectIndex) {
		if (!running) return;

		try {
			script.onObjectSpawned(objectIndex);
		} catch (final Throwable t) {
			System.err.println(ERROR_MESSAGE);
			t.printStackTrace();
		}
	}

	static void objectDespawnedHook(final int objectIndex) {
		instance.onObjectDespawned(objectIndex);
	}

	private void onObjectDespawned(final int objectIndex) {
		if (!running) return;

		try {
			script.onObjectDespawned(objectIndex);
		} catch (final Throwable t) {
			System.err.println(ERROR_MESSAGE);
			t.printStackTrace();
		}
	}

	static void playerCoordHook(final int x, final int y) {
		instance.onPlayerCoord(x, y);
	}

	private void onPlayerCoord(final int x, final int y) {
		if (!running) {
			return;
		}
		try {
			script.onPlayerCoord(x + client.getAreaX(), y + client.getAreaY());
		} catch (final Throwable t) {
			System.err.println(ERROR_MESSAGE);
			t.printStackTrace();
		}
	}

	@Override
	public void setBotFrame(final BotFrame botFrame) {
		this.botFrame = botFrame;
	}

	@Override
	public void onPaintTick() {
		if(System.currentTimeMillis() > nextDeRefresh && (nextDeRefresh != -1) && (nextRefresh != -1)) {
			client.setRendering(false);
			nextDeRefresh = -1;
		}
		if (running) {
			try {
				script.paint();
			} catch (final Throwable t) {
				System.err.println(ERROR_MESSAGE);
				t.printStackTrace();
			}
		}
	}

	@Override
	public void onKeyPress(final KeyEvent e) {
		if (running) {
			script.onKeyPress(e);
		}
	}

	@Override
	public boolean onCommand(final String command) {
		if (!running) return true;

		try {
			return script.onCommand(command);
		} catch (final Throwable t) {
			System.err.println(ERROR_MESSAGE);
			t.printStackTrace();
			return true;
		}
	}

	@Override
	public void setIScript(final IScript script) {
		this.script = script;
	}

	@Override
	public boolean isScriptRunning() {
		return running;
	}

	@Override
	public void setScriptRunning(final boolean b) {
		running = b;
		botFrame.updateStartButton(running);
		if (!running) {
			client.setKeysDisabled(false);
			client.setFatigueTraining(false);
		}
	}

	@Override
	public String getScriptName() {
		if (script == null) {
			return "null";
		}
		return script.toString();
	}

	@Override
	public boolean hasScript() {
		return script != null;
	}
}
