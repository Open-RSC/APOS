import com.aposbot.Constants;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public abstract class AA_Script extends Script {
	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0");

	public static final int[] NPC_IDS_BANKER = new int[]{95, 224, 268, 485, 540, 617, 792};
	public static final int[] OBJECT_IDS_BED = new int[]{14, 15, 1035, 1162, 1171};

	public static final long TIMEOUT_ONE_TICK = 650L;
	public static final long TIMEOUT_ONE_SECOND = 1000L;
	public static final long TIMEOUT_TWO_SECONDS = 2000L;
	public static final long TIMEOUT_THREE_SECONDS = 3000L;
	public static final long TIMEOUT_FIVE_SECONDS = 5000L;
	public static final long TIMEOUT_TEN_SECONDS = 10000L;

	public static final int SLEEP_ONE_TICK = 650;
	public static final int SLEEP_ONE_SECOND = 1000;
	public static final int SLEEP_TWO_SECONDS = 2000;
	public static final int SLEEP_THREE_SECONDS = 3000;
	public static final int SLEEP_FIVE_SECONDS = 5000;
	public static final int SLEEP_TEN_SECONDS = 10000;

	public static final int PAINT_OFFSET_X = 312;
	public static final int PAINT_OFFSET_X_LOOT = 140;
	public static final int PAINT_OFFSET_Y = 48;
	public static final int PAINT_OFFSET_Y_INCREMENT = 14;

	public static final int MAX_INVENTORY_SIZE = 30;
	public static final int MAX_TRADE_SIZE = 12;

	public static final int ITEM_ID_SLEEPING_BAG = 1263;

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

	protected final Extension bot;

	protected CombatStyle combatStyle = CombatStyle.STRENGTH;

	private long optionMenuTimeout;

	public AA_Script(final Extension bot) {
		super(bot);
		this.bot = bot;
	}

	public static String toUnitsPerHour(final int processed, final long time) {
		if (processed == 0) {
			return "0";
		}

		final double amount = processed * 60.0 * 60.0;
		final double seconds = (System.currentTimeMillis() - time) / 1000.0;
		return DECIMAL_FORMAT.format(amount / seconds);
	}

	public static String toTimeToCompletion(final int processed, final int remaining, final long time) {
		if (processed == 0) {
			return "0:00:00";
		}

		final double seconds = (System.currentTimeMillis() - time) / 1000.0;
		final double secondsPerItem = seconds / processed;
		final long ttl = (long) (secondsPerItem * remaining);
		return String.format("%d:%02d:%02d", ttl / 3600, (ttl % 3600) / 60, (ttl % 60));
	}

	public static String toDuration(final long time) {
		final long seconds = (System.currentTimeMillis() - time) / 1000;
		return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
	}

	public static String getLocalDateTime() {
		return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DATE_TIME_FORMATTER);
	}

	@Override
	public abstract void init(final String parameters);

	@Override
	public abstract int main();

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("moment")) {
			optionMenuTimeout = 0L;
		}
	}

	@Override
	public void onDeath() {
		setAutoLogin(false);
		stopScript();
		System.out.println("Oh dear, you are dead.");
	}
	/**
	 * Overrides walkTo and walks to the given tile.
	 *
	 * @param  x	the x coordinate to walk to
	 * @param  y	the y coordinate to walk to
	 */
	@Override
	public void walkTo(final int x, final int y) {
		bot.walkDirectly(x - bot.getAreaX(), y - bot.getAreaY(), false);
		bot.setActionInd(24);
	}

	@Override
	public void useItemOnObject(final int inventoryIndex, final int x, final int y) {
		final int localX = x - bot.getAreaX();
		final int localY = y - bot.getAreaY();

		int objectId = -1;
		int objectDirection = -1;

		for (int index = 0; index < bot.getObjectCount(); index++) {
			if (bot.getObjectLocalX(index) == localX && bot.getObjectLocalY(index) == localY) {
				objectId = bot.getObjectId(index);
				objectDirection = bot.getObjectDir(index);
				break;
			}
		}

		if (objectId == -1 || objectDirection == -1) {
			return;
		}

		bot.walkToObject(localX, localY, objectDirection, objectId);
		bot.createPacket(Constants.OP_OBJECT_USEWITH);
		bot.put2(x);
		bot.put2(y);
		bot.put2(inventoryIndex);
		bot.finishPacket();
	}

	@Override
	public void atObject(final int x, final int y) {
		final int index = getObjectIndex(x, y);

		if (index == -1) {
			return;
		}

		bot.walkToObject(x - bot.getAreaX(), y - bot.getAreaY(), bot.getObjectDir(index),
			bot.getObjectId(index));

		bot.createPacket(Constants.OP_OBJECT_ACTION1);
		bot.put2(x);
		bot.put2(y);
		bot.finishPacket();
	}

	@Override
	public void atObject2(final int x, final int y) {
		final int index = getObjectIndex(x, y);

		if (index == -1) {
			return;
		}

		bot.walkToObject(x - bot.getAreaX(), y - bot.getAreaY(), bot.getObjectDir(index),
			bot.getObjectId(index));

		bot.createPacket(Constants.OP_OBJECT_ACTION2);
		bot.put2(x);
		bot.put2(y);
		bot.finishPacket();
	}

	@Override
	public void atWallObject(final int x, final int y) {
		final int wallObjectIndex = getWallObjectIndex(x, y);

		if (wallObjectIndex == -1) {
			return;
		}

		final int dir = bot.getBoundDir(wallObjectIndex);

		bot.walkToBound(x - bot.getAreaX(), y - bot.getAreaY(), dir);

		bot.createPacket(Constants.OP_BOUND_ACTION1);
		bot.put2(x);
		bot.put2(y);
		bot.put1(dir);
		bot.finishPacket();
	}

	@Override
	public void atWallObject2(final int x, final int y) {
		final int wallObjectIndex = getWallObjectIndex(x, y);

		if (wallObjectIndex == -1) {
			return;
		}

		final int dir = bot.getBoundDir(wallObjectIndex);

		bot.walkToBound(x - bot.getAreaX(), y - bot.getAreaY(), dir);

		bot.createPacket(Constants.OP_BOUND_ACTION2);
		bot.put2(x);
		bot.put2(y);
		bot.put1(dir);
		bot.finishPacket();
	}

	@Override
	public void deposit(final int itemId, final int amount) {
		bot.createPacket(Constants.OP_BANK_DEPOSIT);
		bot.put2(itemId);
		bot.put4(amount);
		bot.put4(-2023406815);
		bot.finishPacket();
	}

	@Override
	public void withdraw(final int itemId, final int amount) {
		bot.createPacket(Constants.OP_BANK_WITHDRAW);
		bot.put2(itemId);
		bot.put4(amount);
		bot.put4(305419896);
		bot.finishPacket();
	}

	@Override
	public boolean hasBankItem(final int itemId) {
		for (int index = 0; index < bot.getBankSize(); index++) {
			if (bot.getBankId(index) == itemId && bot.getBankStack(index) > 0) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void closeBank() {
		bot.createPacket(Constants.OP_BANK_CLOSE);
		bot.finishPacket();
		bot.setBankVisible(false);
	}

	@Override
	public void buyShopItem(final int shopIndex, final int amount) {
		bot.createPacket(Constants.OP_SHOP_BUY);
		bot.put2(bot.getShopId(shopIndex));
		bot.put2(bot.getShopStack(shopIndex));
		bot.put2(amount);
		bot.finishPacket();
	}

	@Override
	public void sellShopItem(final int shopIndex, final int amount) {
		bot.createPacket(Constants.OP_SHOP_SELL);
		bot.put2(bot.getShopId(shopIndex));
		bot.put2(bot.getShopStack(shopIndex));
		bot.put2(amount);
		bot.finishPacket();
	}

	@Override
	public void acceptTrade() {
		bot.createPacket(Constants.OP_TRADE_ACCEPT);
		bot.finishPacket();
		bot.Mi = true;
	}

	@Override
	public void confirmTrade() {
		bot.createPacket(Constants.OP_TRADE_CONFIRM);
		bot.finishPacket();
		bot.Vi = true;
	}

	@Override
	public void declineTrade() {
		bot.createPacket(Constants.OP_TRADE_DECLINE);
		bot.finishPacket();
	}

	@Override
	public String getItemName(final int itemId) {
		return getItemNameId(itemId);
	}

	protected int getWallObjectIndex(final int x, final int y) {
		for (int index = 0; index < bot.getBoundCount(); index++) {
			if (bot.getBoundLocalX(index) == (x - bot.getAreaX()) && bot.getBoundLocalY(index) == (y - bot.getAreaY())) {
				return index;
			}
		}

		return -1;
	}

	protected int getObjectIndex(final int x, final int y) {
		for (int index = 0; index < bot.getObjectCount(); index++) {
			if (bot.getObjectLocalX(index) == (x - bot.getAreaX()) && bot.getObjectLocalY(index) == (y - bot.getAreaY())) {
				return index;
			}
		}

		return -1;
	}

	protected boolean isOnFriendList(final String playerName) {
		for (int index = 0; index < getFriendCount(); ++index) {
			if (getFriendName(index).equalsIgnoreCase(playerName)) return true;
		}
		return false;
	}

	protected void addToFriendList(String playerName) {
		playerName = playerName.replace(' ', (char) 160);
		bot.Bj = 0;
		bot.e = "";
		bot.Cb = "";
		bot.b(114, playerName);
	}

	protected void offerTradeItem(final int inventoryIndex, final int amount) {
		bot.offerItemTrade(inventoryIndex, amount);
	}

	protected boolean isTradeOpen() {
		return bot.isInTradeOffer() || bot.isInTradeConfirm();
	}

	protected boolean isTradeConfirmOpen() {
		return bot.isInTradeConfirm();
	}

	protected boolean isTradeAcceptOpen() {
		return bot.isInTradeOffer();
	}

	protected boolean isTradeRecipientAccepted() {
		return bot.hasRemoteAcceptedTrade();
	}

	protected boolean isTradeAccepted() {
		return bot.hasLocalAcceptedTrade();
	}

	public void setCombatStyle(final int combatStyle) {
		bot.setCombatStyle(combatStyle);
		bot.createPacket(Constants.OP_SET_COMBAT_STYLE);
		bot.put1(combatStyle);
		bot.finishPacket();
	}

	protected void printInstructions() {
		throw new IllegalArgumentException(String.format("Please see the %s.java file for instructions!%n", this));
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName();
	}

	protected boolean isAtCoordinate(final Coordinate coordinate) {
		return getPlayerX() == coordinate.getX() && getPlayerY() == coordinate.getY();
	}

	protected int getPlayerX() {
		return bot.getLocalX() + bot.getAreaX();
	}

	protected int getPlayerY() {
		return bot.getLocalY() + bot.getAreaY();
	}

	protected int getServerIndex(final java.lang.Object mob) {
		return bot.getMobServerIndex(mob);
	}

	protected boolean isDead() {
		return bot.isDeathScreen() ||
			bot.getLocalX() < 0 || bot.getLocalX() > 96 ||
			bot.getLocalY() < 0 || bot.getLocalY() > 96;
	}

	protected double getTotalCombatXp() {
		int total = 0;

		for (int i = 0; i < 4; i++) {
			total += bot.getExperience(i);
		}

		return total;
	}

	protected boolean isInventoryFull() {
		return bot.getInventorySize() == MAX_INV_SIZE;
	}

	protected boolean isInventoryEmpty() {
		return bot.getInventorySize() == 0;
	}

	protected int getInventoryEmptyCount() {
		return MAX_INV_SIZE - bot.getInventorySize();
	}

	protected boolean hasInventoryItem(final int[] itemIds) {
		for (final int itemId : itemIds) {
			for (int index = 0; index < bot.getInventorySize(); index++) {
				if (bot.getInventoryId(index) == itemId) {
					return true;
				}
			}
		}

		return false;
	}

	protected boolean isItemIdEquipped(final int itemId) {
		for (int index = 0; index < bot.getInventorySize(); index++) {
			if (bot.getInventoryId(index) == itemId) {
				return bot.isEquipped(index);
			}
		}

		return false;
	}

	protected void equipItem(final int inventoryIndex) {
		bot.createPacket(Constants.OP_INV_EQUIP);
		bot.put2(inventoryIndex);
		bot.finishPacket();
	}

	protected int openBank() {
		return openInterface(0, NPC_IDS_BANKER);
	}

	private int openInterface(final int optionIndex, final int[] npcs) {
		if (bot.isDialogVisible()) {
			bot.createPacket(Constants.OP_DIALOG_ANSWER);
			bot.put1(optionIndex);
			bot.finishPacket();
			bot.setDialogVisible(false);
			optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= optionMenuTimeout) {
			return 0;
		}

		final Object npc = getNearestNpcNotTalking(npcs);

		if (npc == null) {
			return 0;
		}

		if (distanceTo(npc) > 2) {
			walkTo(npc);
			return SLEEP_ONE_TICK;
		}

		talkToNpc(npc);
		optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	protected Object getNearestNpcNotTalking(final int[] npcIds) {
		return getNearestNpc(npcIds, false, true);
	}

	protected int distanceTo(final Object character) {
		return distanceTo(getX(character), getY(character), getPlayerX(), getPlayerY());
	}

	protected void walkTo(final Object character) {
		bot.walkDirectly(bot.getMobLocalX(character), bot.getMobLocalY(character), true);
		bot.setActionInd(24);
	}

	protected void talkToNpc(final Object npc) {
		bot.walkDirectly(bot.getMobLocalX(npc), bot.getMobLocalY(npc), true);
		bot.createPacket(Constants.OP_NPC_TALK);
		bot.put2(bot.getMobServerIndex(npc));
		bot.finishPacket();
	}

	protected Object getNearestNpc(final int[] npcIds, final boolean notInCombat, final boolean notTalking) {
		Object nearestNpc = null;

		int currentDistance = Integer.MAX_VALUE;

		final int playerX = getPlayerX();
		final int playerY = getPlayerY();

		for (int index = 0; index < bot.getNpcCount(); index++) {
			final Object npc = bot.getNpc(index);

			if (!inArray(npcIds, bot.getNpcId(npc)) ||
				(notInCombat && isInCombat(npc)) ||
				(notTalking && isTalking(npc))) {
				continue;
			}

			final int npcX = bot.getMobLocalX(npc) + bot.getAreaX();
			final int npcY = bot.getMobLocalY(npc) + bot.getAreaY();

			final int distance = distanceTo(npcX, npcY, playerX, playerY);

			if (distance < currentDistance) {
				nearestNpc = npc;
				currentDistance = distance;
			}
		}

		return nearestNpc;
	}

	protected int getX(final Object character) {
		return bot.getMobLocalX(character) + bot.getAreaX();
	}

	protected int getY(final Object character) {
		return bot.getMobLocalY(character) + bot.getAreaY();
	}

	protected boolean isInCombat(final Object character) {
		return bot.isMobInCombat(character);
	}

	protected boolean isTalking(final Object character) {
		return bot.isMobTalking(character);
	}

	protected int openShop(final int... shopkeepers) {
		return openInterface(0, shopkeepers);
	}

	protected int openShop(final int optionIndex, final int[] shopkeepers) {
		return openInterface(optionIndex, shopkeepers);
	}

	protected int sleep() {
		final int index = getInventoryItemIndex(ITEM_ID_SLEEPING_BAG);

		if (index == -1) {
			return exit("Sleeping bag missing from inventory.");
		}

		useItem(index);
		return SLEEP_ONE_SECOND;
	}

	protected int getInventoryItemIndex(final int itemId) {
		for (int index = 0; index < bot.getInventorySize(); index++) {
			if (bot.getInventoryId(index) == itemId) {
				return index;
			}
		}

		return -1;
	}

	protected int exit(final String reason) {
		bot.setAutoLogin(false);
		bot.stopScript();
		System.err.println(reason);
		return 0;
	}

	protected int getInventoryItemIndex(final int[] itemIds) {
		for (final int itemId : itemIds) {
			for (int index = 0; index < bot.getInventorySize(); index++) {
				if (bot.getInventoryId(index) == itemId) {
					return index;
				}
			}
		}

		return -1;
	}

	protected int getBaseHits() {
		return bot.getBaseLevel(3);
	}

	protected int getCurrentHits() {
		return bot.getCurrentLevel(3);
	}

	protected int getCurrentHits(final java.lang.Object character) {
		return ((ta) character).B;
	}

	protected Object getPlayerObjByName(final String playerName) {
		Object player;

		for (int index = 0; index < bot.getPlayerCount(); index++) {
			player = bot.getPlayer(index);
			if (playerName.equalsIgnoreCase(getName(player))) return player;
		}

		return null;
	}

	protected String getName(final java.lang.Object character) {
		final String name = ((ta) character).c;

		if (name == null) {
			return null;
		}

		return name.replace((char) 160, ' ');
	}

	protected int getWaypointX(final java.lang.Object character) {
		return ((ta) character).i;
	}

	protected int getWaypointY(final java.lang.Object character) {
		return ((ta) character).K;
	}

	protected void useObject1(final int x, final int y) {
		bot.createPacket(Constants.OP_OBJECT_ACTION1);
		bot.put2(x);
		bot.put2(y);
		bot.finishPacket();
	}

	protected void useObject2(final int x, final int y) {
		bot.createPacket(Constants.OP_OBJECT_ACTION2);
		bot.put2(x);
		bot.put2(y);
		bot.finishPacket();
	}

	protected void useWithObject(final int inventoryIndex, final int x, final int y) {
		bot.createPacket(Constants.OP_OBJECT_USEWITH);
		bot.put2(x);
		bot.put2(y);
		bot.put2(inventoryIndex);
		bot.finishPacket();
	}

	protected void takeGroundItem(final int itemId, final int x, final int y) {
		bot.createPacket(Constants.OP_GITEM_TAKE);
		bot.put2(x);
		bot.put2(y);
		bot.put2(itemId);
		bot.finishPacket();
	}

	protected int getGroundItemX(final int groundItemIndex) {
		return bot.getGroundItemLocalX(groundItemIndex) + bot.getAreaX();
	}

	protected int getGroundItemY(final int groundItemIndex) {
		return bot.getGroundItemLocalY(groundItemIndex) + bot.getAreaY();
	}

	protected boolean isTradeConfirmed() {
		return bot.hasLocalConfirmedTrade();
	}

	protected boolean hasTradeItem(final int[] itemIds) {
		for (final int itemId : itemIds) {
			for (int index = 0; index < bot.getLocalTradeItemCount(); index++) {
				if (bot.getLocalTradeItemId(index) == itemId) {
					return true;
				}
			}
		}

		return false;
	}

	protected int getTradeItemCount() {
		return bot.getLocalTradeItemCount();
	}

	protected int getTradeItemIdCount(final int itemId) {
		int count = 0;

		for (int index = 0; index < bot.getLocalTradeItemCount(); index++) {
			if (bot.getLocalTradeItemId(index) == itemId) {
				count += bot.getLocalTradeItemStack(index);
			}
		}

		return count;
	}

	protected void offerTradeItemId(final int itemId, final int amount) {
		final int inventoryIndex = getInventoryIndex(itemId);

		if (inventoryIndex == -1) {
			return;
		}

		bot.offerItemTrade(inventoryIndex, amount);
	}

	protected void removeTradeItemId(final int itemId, final int amount) {
		final int tradeIndex = getTradeItemIndex(itemId);

		if (tradeIndex == -1) {
			return;
		}

		removeTradeItem(tradeIndex, amount);
	}

	protected int getTradeItemIndex(final int itemId) {
		for (int index = 0; index < bot.getLocalTradeItemCount(); index++) {
			if (bot.getLocalTradeItemId(index) == itemId) {
				return index;
			}
		}

		return -1;
	}

	protected void removeTradeItem(final int tradeIndex, final int amount) {
		bot.c(amount, (byte) 124, tradeIndex);
	}

	protected double getSkillExperience(final int skillId) {
		return bot.getExperience(skillId);
	}

	protected int getCurrentSkillLevel(final int skillId) {
		return bot.oh[skillId];
	}

	protected int getFatiguePercent() {
		return (int) bot.getAccurateFatigue();
	}

	protected boolean isBankOpen() {
		return bot.isBankVisible();
	}

	protected boolean isInCombat() {
		return bot.getCombatTimer() == 499;
	}

	protected int getBankItemIdCount(final int itemId) {
		int count = 0;

		for (int index = 0; index < bot.getBankSize(); index++) {
			if (bot.getBankId(index) == itemId) {
				count += bot.getBankStack(index);
				break;
			}
		}

		return count;
	}

	protected int getBankItemIdCount(final int[] itemIds) {
		int count = 0;

		for (final int itemId : itemIds) {
			for (int index = 0; index < bot.getBankSize(); index++) {
				if (bot.getBankId(index) == itemId) {
					count += bot.getBankStack(index);
					break;
				}
			}
		}

		return count;
	}

	protected int getBaseSkillLevel(final int skillId) {
		return bot.getBaseLevel(skillId);
	}

	protected int getObjectId(final int x, final int y) {
		for (int index = 0; index < bot.getObjectCount(); index++) {
			if (bot.getObjectLocalX(index) == (x - bot.getAreaX()) && bot.getObjectLocalY(index) == (y - bot.getAreaY())) {
				return bot.getObjectId(index);
			}
		}

		return -1;
	}

	protected int getWallObjectId(final int x, final int y) {
		for (int index = 0; index < bot.getBoundCount(); index++) {
			if (bot.getBoundLocalX(index) == (x - bot.getAreaX()) && bot.getBoundLocalY(index) == (y - bot.getAreaY())) {
				return bot.getBoundId(index);
			}
		}

		return -1;
	}

	protected void answerOptionMenu(final int menuIndex) {
		bot.createPacket(Constants.OP_DIALOG_ANSWER);
		bot.put1(menuIndex);
		bot.finishPacket();
		bot.setDialogVisible(false);
	}

	protected Object getNearestNpcNotInCombat(final int npcId) {
		return getNearestNpc(npcId, true, false);
	}

	protected Object getNearestNpc(final int npcId, final boolean notInCombat, final boolean notTalking) {
		Object nearestNpc = null;

		int currentDistance = Integer.MAX_VALUE;

		final int playerX = getPlayerX();
		final int playerY = getPlayerY();

		for (int index = 0; index < bot.getNpcCount(); index++) {
			final Object npc = bot.getNpc(index);

			if (bot.getNpcId(npc) != npcId ||
				(notInCombat && isInCombat(npc)) ||
				(notTalking && isTalking(npc))) {
				continue;
			}

			final int npcX = bot.getMobLocalX(npc) + bot.getAreaX();
			final int npcY = bot.getMobLocalY(npc) + bot.getAreaY();

			final int distance = distanceTo(npcX, npcY, playerX, playerY);

			if (distance < currentDistance) {
				nearestNpc = npc;
				currentDistance = distance;
			}
		}

		return nearestNpc;
	}

	protected Object getNearestNpcNotInCombat(final int[] npcIds) {
		return getNearestNpc(npcIds, true, false);
	}

	protected Object getNearestNpcNotTalking(final int npcId) {
		return getNearestNpc(npcId, false, true);
	}

	protected boolean isOptionMenuOpen() {
		return bot.isDialogVisible();
	}

	protected void castOnNpc(final int spellId, final Object npc) {
		bot.walkDirectly(bot.getMobLocalX(npc), bot.getMobLocalY(npc), true);
		bot.createPacket(Constants.OP_NPC_CAST);
		bot.put2(bot.getMobServerIndex(npc));
		bot.put2(spellId);
		bot.finishPacket();
	}

	protected void attackNpc(final Object npc) {
		bot.walkDirectly(bot.getMobLocalX(npc), bot.getMobLocalY(npc), true);
		bot.createPacket(Constants.OP_NPC_ATTACK);
		bot.put2(bot.getMobServerIndex(npc));
		bot.finishPacket();
	}

	protected int getInventoryItemIdCount(final int itemId) {
		int count = 0;

		for (int index = 0; index < bot.getInventorySize(); index++) {
			if (bot.getInventoryId(index) == itemId) {
				if (isItemStackableId(itemId)) {
					count += bot.getInventoryStack(index);
				} else {
					count++;
				}
			}
		}

		return count;
	}

	protected int getInventoryItemIdCount(final int[] itemIds) {
		int count = 0;

		for (final int itemId : itemIds) {
			for (int index = 0; index < getInventoryItemCount(); index++) {
				if (getInventoryItemId(index) == itemId) count += getInventoryItemCount(index);
			}
		}

		return count;
	}

	protected int getInventoryItemCount() {
		return bot.getInventorySize();
	}

	protected int getInventoryItemId(final int inventoryIndex) {
		return bot.getInventoryId(inventoryIndex);
	}

	protected int getInventoryItemCount(final int inventoryIndex) {
		return bot.getInventoryStack(inventoryIndex);
	}

	protected int getShopItemCount(final int shopIndex) {
		return bot.getShopStack(shopIndex);
	}

	protected int getOptionMenuCount() {
		return bot.getDialogOptionCount();
	}

	protected void useItemOnNpc(final int inventoryIndex, final Object npc) {
		bot.walkDirectly(bot.getMobLocalX(npc), bot.getMobLocalY(npc), true);
		bot.createPacket(Constants.OP_NPC_USEWITH);
		bot.put2(bot.getMobServerIndex(npc));
		bot.put2(inventoryIndex);
		bot.finishPacket();
	}

	protected int getShopItemIndex(final int itemId) {
		for (int index = 0; index < bot.getShopSize(); index++) {
			if (bot.getShopId(index) == itemId) {
				return index;
			}
		}

		return -1;
	}

	protected Coordinate getWalkableCoordinate() {
		int x;
		int y;

		for (int i = -1; i <= 1; i++) {
			x = getX() + i;

			for (int j = -1; j <= 1; j++) {
				y = getY() + j;

				if (i == 0 && j == 0) {
					continue;
				}

				if (isReachable(x, y) && !isObjectAt(x, y)) {
					return new Coordinate(x, y);
				}
			}
		}

		return null;
	}

	protected enum CombatStyle {
		CONTROLLED(0),
		STRENGTH(1),
		ATTACK(2),
		DEFENSE(3);

		private final int index;

		CombatStyle(final int index) {
			this.index = index;
		}

		@Override
		public String toString() {
			return name().charAt(0) + name().substring(1).toLowerCase();
		}

		public int getIndex() {
			return index;
		}
	}

	protected enum Skill {
		ATTACK(0),
		DEFENSE(1),
		STRENGTH(2),
		HITS(3),
		RANGED(4),
		PRAYER(5),
		MAGIC(6),
		COOKING(7),
		WOODCUT(8),
		FLETCHING(9),
		FISHING(10),
		FIREMAKING(11),
		CRAFTING(12),
		SMITHING(13),
		MINING(14),
		HERBLAW(15),
		AGILITY(16),
		THIEVING(17);

		private final int index;

		Skill(final int index) {
			this.index = index;
		}

		@Override
		public String toString() {
			return name().charAt(0) + name().substring(1).toLowerCase();
		}

		public int getIndex() {
			return index;
		}
	}

	protected enum Food {
		NONE(-1, 0, "None"),
		SHRIMP(350, 3, "Shrimp"),
		ANCHOVIES(352, 1, "Anchovies"),
		SARDINE(355, 4, "Sardine"),
		HERRING(362, 5, "Herring"),
		GIANT_CARP(718, 6, "Giant Carp"),
		MACKEREL(553, 6, "Mackerel"),
		TROUT(359, 7, "Trout"),
		COD(551, 7, "Cod"),
		PIKE(364, 8, "Pike"),
		SALMON(357, 9, "Salmon"),
		TUNA(367, 10, "Tuna"),
		LOBSTER(373, 12, "Lobster"),
		BASS(555, 13, "Bass"),
		SWORDFISH(370, 14, "Swordfish"),
		SHARK(546, 20, "Shark"),
		SEA_TURTLE(1193, 20, "Sea Turtle"),
		MANTA_RAY(1191, 20, "Manta Ray");

		private final int id;
		private final int healAmount;
		private final String name;

		Food(final int id, final int healAmount, final String name) {
			this.id = id;
			this.healAmount = healAmount;
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}

		public int getId() {
			return id;
		}

		public int getHealAmount() {
			return healAmount;
		}
	}

	protected interface RSObject {
		int getId();

		Coordinate getCoordinate();
	}

	protected interface RSArea {
		default boolean contains(final int x, final int y) {
			return x >= getLowerBoundingCoordinate().getX() && x <= getUpperBoundingCoordinate().getX() &&
				y >= getLowerBoundingCoordinate().getY() && y <= getUpperBoundingCoordinate().getY();
		}

		Coordinate getLowerBoundingCoordinate();

		Coordinate getUpperBoundingCoordinate();
	}

	public static final class Coordinate {
		private int x;

		private int y;

		public Coordinate(final int x, final int y) {
			this.x = x;
			this.y = y;
		}

		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof Coordinate)) {
				return false;
			}
			final Coordinate other = (Coordinate) o;
			if (getX() != other.getX()) {
				return false;
			}
			return getY() == other.getY();
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			result = result * PRIME + getX();
			result = result * PRIME + getY();
			return result;
		}

		public String toString() {
			return "AA_Script.Coordinate(x=" + getX() + ", y=" + getY() + ")";
		}

		public int getX() {
			return x;
		}

		public void setX(final int x) {
			this.x = x;
		}

		public int getY() {
			return y;
		}

		public void setY(final int y) {
			this.y = y;
		}

		public void set(final int x, final int y) {
			this.x = x;
			this.y = y;
		}
	}

	public static final class Spawn implements Comparable<Spawn> {
		private final Coordinate coordinate;

		private long timestamp;

		public Spawn(final Coordinate coordinate, final long timestamp) {
			this.coordinate = coordinate;
			this.timestamp = timestamp;
		}

		@Override
		public int compareTo(final Spawn spawn) {
			return Long.compare(timestamp, spawn.timestamp);
		}

		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof Spawn)) {
				return false;
			}
			final Spawn other = (Spawn) o;
			final Object this$coordinate = getCoordinate();
			final Object other$coordinate = other.getCoordinate();
			if (!Objects.equals(this$coordinate, other$coordinate)) {
				return false;
			}
			return getTimestamp() == other.getTimestamp();
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $coordinate = getCoordinate();
			result = result * PRIME + ($coordinate == null ? 43 : $coordinate.hashCode());
			final long $timestamp = getTimestamp();
			result = result * PRIME + (int) ($timestamp >>> 32 ^ $timestamp);
			return result;
		}

		public String toString() {
			return "AA_Script.Spawn(coordinate=" + getCoordinate() + ", timestamp=" + getTimestamp() + ")";
		}

		public Coordinate getCoordinate() {
			return coordinate;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(final long timestamp) {
			this.timestamp = timestamp;
		}
	}
}
