import java.util.Map;
import java.util.TreeMap;

/**
 * Kills the King Black Dragon.
 * <p>
 * Instructions:
 * Start script at Edgeville Bank with an empty inventory.
 * <p>
 * Banked (multiple):
 * dmed, dsquare, dsword, runepl8, runelegs, antidshield, charged dstone amulet, cape of legends,
 * sharks, regular/super set pots, cure/anti poison pots
 * <p>
 * Optional Parameter:
 * name of player running AA_KBDMule script
 * <p>
 * Notes:
 * Modify the Equipment enum directly to change item ids.
 * <p>
 *
 * @Author Chomp
 */
public class AA_KBDMaster extends AA_Script {
	private static final Coordinate[] PATH_TO_KBD = new Coordinate[]{
		new Coordinate(239, 432),
		new Coordinate(285, 384),
		new Coordinate(285, 336),
		new Coordinate(284, 288),
		new Coordinate(286, 240),
		new Coordinate(285, 192),
		new Coordinate(285, 185)
	};

	private static final Coordinate[] PATH_TO_EDGEVILLE = new Coordinate[]{
		new Coordinate(285, 234),
		new Coordinate(284, 283),
		new Coordinate(277, 332),
		new Coordinate(272, 381),
		new Coordinate(240, 422),
		new Coordinate(217, 447)
	};

	private static final Coordinate
		COORDINATE_DEATH_WALK_LUMBRIDGE = new Coordinate(120, 648),
		COORDINATE_DEATH_WALK_EDGEVILLE = new Coordinate(220, 446),
		COORDINATE_KBD_RESPAWN = new Coordinate(567, 3321),
		COORDINATE_TRADE_SPOT = new Coordinate(569, 3331);

	/**
	 * Excluded drops:
	 * 10 (coins)
	 * 91 (Mithril battle Axe)
	 * 157, 158, 159, 160 (gems)
	 * 204 (Adamantite Axe)
	 **/
	private static final int[] ITEM_IDS_LOOT = new int[]{
		11, 31, 33, 38, 40, 42,
		75, 81, 93, 120, 174,
		316, 403, 404, 405, 408,
		517, 518, 520, 523, 526, 527, 546,
		619, 638, 711, 793, 795, 814, 1092, 1277
	};
	private static final int[] ITEM_IDS_LOOT_NOTABLE = new int[]{
		38, 75, 81, 93,
		403, 404, 405, 408,
		523, 526, 527, 619,
		711, 795, 814, 1092, 1277
	};
	private static final int[] ITEM_IDS_STACKABLE = new int[]{11, 31, 33, 38, 40, 42, 517, 518, 520, 619, 638, 711};

	private static final int[] COMBAT_STYLE_BREAKPOINTS = new int[]{102, 106, 111, 115};

	private static final int
		NPC_ID_KING_BLACK_DRAGON = 477,
		NPC_ID_POISON_SPIDER = 292,
		NPC_ID_LESSER_DEMON = 22,
		ITEM_ID_CHARGED_DRAGONSTONE_AMULET = 597,
		ITEM_ID_DRAGONSTONE_AMULET = 522,
		ITEM_ID_EMPTY_VIAL = 465,
		MINIMUM_ATK = 104,
		MINIMUM_STR = 104,
		MINIMUM_DEF = 104,
		MINIMUM_HITS = 49,
		MINIMUM_HITS_KBD = 60,
		MINIMUM_SHARKS = 10,
		PAINT_OFFSET_X_ALT = 185;

	private final Map<Integer, Integer> notableLoot = new TreeMap<>();

	private final int[] loot = new int[3];

	private String mule;
	private State state;
	private PathWalker pathWalker;
	private long startTime;

	private long
		actionTimeout,
		optionMenuTimeout,
		depositTimeout,
		withdrawTimeout,
		tradeRequestTimeout,
		privateMessageTimeout;

	private int
		playerX,
		playerY,
		previousY,
		eatAt,
		superStrsUsed,
		superAtksUsed,
		superDefsUsed,
		sharksEaten,
		deaths;

	private boolean
		abort,
		poisoned,
		flee,
		died,
		idle,
		wieldShield;

	public AA_KBDMaster(final Extension extension) {
		super(extension);
		combatStyle = CombatStyle.ATTACK;
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			mule = parameters.replace('_', ' ');
		}

		final State initialState;

		if (Area.BANK.contains(getX(), getY())) {
			initialState = State.BANK;
		} else if (Area.DRAGON.contains(getX(), getY())) {
			initialState = State.KILL;
		} else if (hasInventoryItem(Food.SHARK.getId())) {
			initialState = State.WALK_TO_DRAGON;
		} else {
			initialState = State.WALK_TO_BANK;
		}

		setCombatStyle(combatStyle.getIndex());
		eatAt = getBaseHits() - Food.SHARK.getHealAmount();
		startTime = System.currentTimeMillis();
		setState(initialState);
	}

	@Override
	public int main() {
		if (died) {
			if (isDead() || (pathWalker != null && pathWalker.walkPath())) {
				return 0;
			}

			pathWalker = null;
			died = false;
		}

		playerX = getX();
		playerY = getY();

		switch (state) {
			case BANK:
				return bank();
			case WALK_TO_DRAGON:
				return walkToDragon();
			case KILL:
				return kill();
			case TRADE:
				return trade();
			case TELEPORT_TO_BANK:
				return teleportToBank();
			case WALK_TO_BANK:
				return walkToBank();
			default:
				return exit("Invalid script state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("shark")) {
			sharksEaten++;
			actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("flames")) {
			wieldShield = true;
		} else if (message.endsWith("successfully") || message.startsWith("declined", 17)) {
			resetTradeTimeouts();
		} else if (message.endsWith("poison potion") || message.endsWith("antidote potion")) {
			poisoned = false;
		} else if (message.endsWith("attack potion")) {
			superAtksUsed++;
		} else if (message.endsWith("defense potion")) {
			superDefsUsed++;
		} else if (message.endsWith("strength potion")) {
			superStrsUsed++;
		} else if (message.endsWith("left") ||
			message.startsWith("finished", 9) ||
			message.endsWith("Vial")) {
			actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.startsWith("poisioned", 23)) {
			poisoned = true;
		} else if (message.endsWith("area")) {
			idle = true;
			previousY = getY();
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void onDeath() {
		if (pathWalker == null) {
			pathWalker = new PathWalker(bot);
			pathWalker.init(null);
		}

		final PathWalker.Path path = pathWalker.calcPath(COORDINATE_DEATH_WALK_LUMBRIDGE.getX(),
			COORDINATE_DEATH_WALK_LUMBRIDGE.getY(),
			COORDINATE_DEATH_WALK_EDGEVILLE.getX(),
			COORDINATE_DEATH_WALK_EDGEVILLE.getY());

		if (path == null) {
			abort("Failed to calculate path from Lumbridge to Edgeville.");
		}

		pathWalker.setPath(path);

		setState(State.BANK);

		died = true;
		deaths++;
		idle = false;
		poisoned = false;
		flee = false;
		wieldShield = false;
	}

	private void abort(final String reason) {
		if (mule != null) {
			sendPrivateMessage("abort", mule);
		}

		exit(reason);
	}

	private void resetTradeTimeouts() {
		tradeRequestTimeout = 0L;
		privateMessageTimeout = 0L;
	}

	private void setState(final State state) {
		this.state = state;

		switch (this.state) {
			case BANK:
				if (abort) {
					exit("Trip aborted.");
					return;
				}
				updateNotableLoot();
				break;
			case TRADE:
				updateNotableLoot();
				break;
		}
	}

	private void updateNotableLoot() {
		for (int index = 0; index < getInventoryCount(); index++) {
			final int itemId = getInventoryId(index);

			if (!inArray(ITEM_IDS_LOOT_NOTABLE, itemId)) {
				continue;
			}

			notableLoot.merge(itemId, getInventoryStack(index), Integer::sum);
		}

		for (final Equipment equipment : Equipment.VALUES) {
			if (inArray(ITEM_IDS_LOOT_NOTABLE, equipment.id) && getInventoryCount(equipment.id) > 1) {
				notableLoot.merge(equipment.id, -1, Integer::sum);
			}
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@King Black Dragon", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@State: @cya@%s", state),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (!notableLoot.isEmpty()) {
			drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

			for (final Map.Entry<Integer, Integer> entry : notableLoot.entrySet()) {
				drawString(String.format("@gr1@%s: @whi@%d", getItemNameId(entry.getKey()), entry.getValue()),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
			}
		}

		y = 0;

		drawString("@yel@Supplies Used",
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@or1@Strs: @whi@%s @cya@(@whi@%s p@cya@/@whi@hr@cya@)",
				superStrsUsed, toUnitsPerHour(superStrsUsed, startTime)),
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@ora@Atks: @whi@%s @cya@(@whi@%s p@cya@/@whi@hr@cya@)",
				superAtksUsed, toUnitsPerHour(superAtksUsed, startTime)),
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@or2@Defs: @whi@%s @cya@(@whi@%s p@cya@/@whi@hr@cya@)",
				superDefsUsed, toUnitsPerHour(superDefsUsed, startTime)),
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@or3@Sharks: @whi@%s @cya@(@whi@%s p@cya@/@whi@hr@cya@)",
				sharksEaten, toUnitsPerHour(sharksEaten, startTime)),
			PAINT_OFFSET_X_ALT, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	@Override
	public void onPrivateMessage(final String message, final String playerName, final boolean moderator,
								 final boolean administrator) {
		if (playerName.equalsIgnoreCase(mule) && message.equalsIgnoreCase("abort")) {
			mule = null;
			abort = true;
		}
	}

	@Override
	public void onPlayerDamaged(final java.lang.Object player) {
		if (!inCombat() || player != bot.getPlayer()) {
			return;
		}

		if (flee) {
			walkTo(playerX, playerY);
			return;
		}

		flee = getCurrentHits() < MINIMUM_HITS ||
			(poisoned && hasInventoryItem(Potion.ANTIDOTE.ids));

		final int combatStyle = getCombatStyle();

		if (bot.getCombatStyle() != combatStyle) setCombatStyle(combatStyle);
	}

	@Override
	public void onNpcDamaged(final java.lang.Object npc) {
		if (!inCombat() ||
			getWaypointX(npc) != getWaypointX(bot.getPlayer()) ||
			getWaypointY(npc) != getWaypointY(bot.getPlayer())) {
			return;
		}

		if (flee) {
			walkTo(playerX, playerY);
			return;
		}

		if (getCombatStyle() != CombatStyle.DEFENSE.getIndex()) setCombatStyle(CombatStyle.DEFENSE.getIndex());
	}

	private int getCombatStyle() {
		return inArray(COMBAT_STYLE_BREAKPOINTS, getCurrentLevel(Skill.ATTACK.getIndex())) ?
			CombatStyle.STRENGTH.getIndex() : CombatStyle.ATTACK.getIndex();
	}

	private int bank() {
		if (getCurrentHits() != getBaseHits()) {
			final int sharkIndex = getInventoryIndex(Food.SHARK.getId());

			if (sharkIndex != -1) {
				return consume(sharkIndex);
			}
		}

		if (poisoned) {
			final int potionIndex = getInventoryIndex(Potion.ANTIDOTE.ids);

			if (potionIndex != -1) {
				return consume(potionIndex);
			}
		}

		if (!isBanking()) {
			return openBank();
		}

		for (int index = 0; index < getInventoryCount(); index++) {
			final int itemId = getInventoryId(index);

			if (itemId == Food.SHARK.getId()) {
				continue;
			}

			if (Equipment.isEquipment(itemId)) {
				final int invCount = getInventoryCount(itemId);

				if (invCount == 1) {
					continue;
				}

				if (System.currentTimeMillis() <= depositTimeout) {
					return 0;
				}

				deposit(itemId, invCount - 1);
				depositTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (!inArray(ITEM_IDS_LOOT, itemId) &&
				itemId != ITEM_ID_DRAGONSTONE_AMULET &&
				itemId != ITEM_ID_EMPTY_VIAL) {
				continue;
			}

			deposit(itemId, getInventoryCount(itemId));
			return SLEEP_ONE_TICK;
		}

		for (final Equipment equipment : Equipment.VALUES) {
			if (hasInventoryItem(equipment.id)) {
				continue;
			}

			if (System.currentTimeMillis() <= equipment.timeout) {
				return 0;
			}

			if (!hasBankItem(equipment.id)) {
				abort(String.format("Out of item: %s.", getItemNameId(equipment.id)));
			}

			withdraw(equipment.id, 1);
			equipment.timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		for (final Potion potion : Potion.VALUES) {
			if (hasInventoryItem(potion.ids)) {
				continue;
			}

			if (System.currentTimeMillis() <= potion.timeout) {
				return 0;
			}

			for (final int id : potion.ids) {
				if (!hasBankItem(id)) {
					continue;
				}

				withdraw(id, 1);
				potion.timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}
		}

		if (!isInventoryFull()) {
			if (System.currentTimeMillis() <= withdrawTimeout) {
				return 0;
			}

			if (!hasBankItem(Food.SHARK.getId())) {
				abort(String.format("Out of item: %s.", Food.SHARK));
			}

			withdraw(Food.SHARK.getId(), getInventoryEmptyCount());
			withdrawTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		setState(State.WALK_TO_DRAGON);
		return 0;
	}

	private int walkToDragon() {
		if (Area.DRAGON.contains(playerX, playerY)) {
			setState(State.KILL);
			return 0;
		}

		if (!inCombat()) {
			if (poisoned) {
				final int potionIndex = getInventoryIndex(Potion.ANTIDOTE.ids);

				if (potionIndex != -1) {
					return consume(potionIndex);
				}
			}

			if (getCurrentHits() <= eatAt) {
				final int sharkIndex = getInventoryIndex(Food.SHARK.getId());

				if (sharkIndex != -1) {
					return consume(sharkIndex);
				}
			}
		}

		if (Area.SPIDERS.contains(playerX, playerY)) {
			if (distanceTo(Object.LEVER_SPIDER.coordinate.getX(), Object.LEVER_SPIDER.coordinate.getY()) <= 1) {
				if (inCombat()) {
					walkTo(playerX, playerY);
					return SLEEP_ONE_TICK;
				}

				useObject1(Object.LEVER_SPIDER.coordinate.getX(), Object.LEVER_SPIDER.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			if (!inCombat()) {
				final int poisonSpider = getBlockingPoisonSpider();

				if (poisonSpider != -1) {
					attackNpc(poisonSpider);
					return SLEEP_ONE_TICK;
				}
			}

			walkTo(Object.LEVER_SPIDER.coordinate.getX(), Object.LEVER_SPIDER.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.LESSERS.contains(playerX, playerY)) {
			if (distanceTo(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY()) <= 1) {
				if (inCombat()) {
					walkTo(playerX, playerY);
					return SLEEP_ONE_TICK;
				}

				useObject1(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			if (!inCombat()) {
				final int lesserDemon = getBlockingLesserDemon();

				if (lesserDemon != -1) {
					attackNpc(lesserDemon);
					return SLEEP_ONE_TICK;
				}
			}

			walkTo(Object.LADDER_DOWN.coordinate.getX() + 1, Object.LADDER_DOWN.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(Object.GATE.coordinate.getX(), Object.GATE.coordinate.getY()) <= 1) {
			if (inCombat()) {
				walkTo(playerX, playerY);
				return SLEEP_ONE_TICK;
			}

			useObject1(Object.GATE.coordinate.getX(), Object.GATE.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (Area.BANK.contains(playerX, playerY)) {
			if (wieldEquipment()) {
				return SLEEP_ONE_TICK;
			}

			if (getObjectIdFromCoords(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY()) ==
				Object.DOORS.id) {
				atObject(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}
		}

		for (final Coordinate coordinate : PATH_TO_KBD) {
			if (playerY > coordinate.getY()) {
				walkTo(coordinate.getX(), coordinate.getY());
				break;
			}
		}

		return SLEEP_ONE_TICK;
	}

	private boolean wieldEquipment() {
		for (final Equipment equipment : Equipment.VALUES) {
			if (equipment == Equipment.ANTI_SHIELD) {
				continue;
			}

			final int equipmentIndex = getInventoryIndex(equipment.id);

			if (equipmentIndex != -1 && !isItemEquipped(equipmentIndex)) {
				wearItem(equipmentIndex);
				return true;
			}
		}

		return false;
	}

	private int kill() {
		if (inCombat()) {
			if (wieldShield) {
				final int shieldIndex = getInventoryIndex(Equipment.SHIELD.id);

				if (!isItemEquipped(shieldIndex)) {
					wearItem(shieldIndex);
					return SLEEP_ONE_TICK;
				}

				wieldShield = false;
			}

			return 0;
		}

		if (flee) {
			flee = false;
		}

		final int antiShieldIndex = getInventoryIndex(Equipment.ANTI_SHIELD.id);

		if (antiShieldIndex == -1) {
			setState(State.TELEPORT_TO_BANK);
			return 0;
		}

		if (!isItemEquipped(antiShieldIndex)) {
			wearItem(antiShieldIndex);
			return SLEEP_ONE_TICK;
		}

		if (poisoned) {
			final int potionIndex = getInventoryIndex(Potion.ANTIDOTE.ids);

			if (potionIndex != -1) {
				return consume(potionIndex);
			}
		}

		if (getCurrentHits() <= eatAt) {
			final int sharkIndex = getInventoryIndex(Food.SHARK.getId());

			if (sharkIndex == -1) {
				setState(mule != null ? State.TRADE : State.TELEPORT_TO_BANK);
				return 0;
			}

			return consume(sharkIndex);
		}

		final int vialIndex = getInventoryIndex(ITEM_ID_EMPTY_VIAL);

		if (vialIndex != -1) {
			return dropVial(vialIndex);
		}

		updateLoot();

		if (loot[0] != -1 && (loot[0] != Food.SHARK.getId() || !isInventoryFull())) {
			if (!isInventoryFull() ||
				(inArray(ITEM_IDS_STACKABLE, loot[0]) && hasInventoryItem(loot[0]))) {
				pickupItem(loot[0], loot[1], loot[2]);
				return SLEEP_ONE_TICK;
			}

			final int sharkIndex = getInventoryIndex(Food.SHARK.getId());

			if (sharkIndex != -1) {
				return consume(sharkIndex);
			}
		}

		final int[] dragon = getAllNpcById(NPC_ID_KING_BLACK_DRAGON);

		if (dragon[0] == -1) {
			if (mule != null && isMissingSupplies()) {
				setState(State.TRADE);
				return 0;
			}

			if (playerX != COORDINATE_KBD_RESPAWN.getX() || playerY != COORDINATE_KBD_RESPAWN.getY()) {
				walkTo(COORDINATE_KBD_RESPAWN.getX(), COORDINATE_KBD_RESPAWN.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (isNpcInCombat(dragon[0])) {
			if (distanceTo(dragon[1], dragon[2]) > 1) {
				walkTo(dragon[1], dragon[2]);
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		final int npcHits = getCurrentHits(bot.getNpc(dragon[0]));

		if (npcHits == 0 || npcHits >= MINIMUM_HITS_KBD) {
			if (canDrinkSuperAttack()) {
				final int potionIndex = getInventoryIndex(Potion.SUPER_ATTACK.ids);

				if (potionIndex != -1) {
					return consume(potionIndex);
				}
			}

			if (canDrinkSuperDefense()) {
				final int potionIndex = getInventoryIndex(Potion.SUPER_DEFENSE.ids);

				if (potionIndex != -1) {
					return consume(potionIndex);
				}
			}

			if (canDrinkSuperStrength()) {
				final int potionIndex = getInventoryIndex(Potion.SUPER_STRENGTH.ids);

				if (potionIndex != -1) {
					return consume(potionIndex);
				}
			}
		}

		attackNpc(dragon[0]);
		return SLEEP_ONE_TICK;
	}

	private boolean isMissingSupplies() {
		if (getInventoryCount(Food.SHARK.getId()) < MINIMUM_SHARKS) {
			return true;
		}

		if (canDrinkSuperAttack() && !hasInventoryItem(Potion.SUPER_ATTACK.ids)) {
			return true;
		}

		if (canDrinkSuperDefense() && !hasInventoryItem(Potion.SUPER_DEFENSE.ids)) {
			return true;
		}

		if (canDrinkSuperStrength() && !hasInventoryItem(Potion.SUPER_STRENGTH.ids)) {
			return true;
		}

		return poisoned;
	}

	private int trade() {
		if (inCombat()) {
			walkTo(playerX, playerY);
			return SLEEP_ONE_TICK;
		}

		if (poisoned) {
			final int potionIndex = getInventoryIndex(Potion.ANTIDOTE.ids);

			if (potionIndex != -1) {
				return consume(potionIndex);
			}
		}

		if (getCurrentHits() <= eatAt) {
			final int sharkIndex = getInventoryIndex(Food.SHARK.getId());

			if (sharkIndex != -1) {
				return consume(sharkIndex);
			}

			if (Area.TRADE.contains(playerX, playerY) &&
				(getCurrentHits() < MINIMUM_HITS || getPlayerByName(mule)[0] == -1)) {
				final int[] kbd = getAllNpcById(NPC_ID_KING_BLACK_DRAGON);

				if (kbd[0] != -1 && Area.TRADE.contains(kbd[1], kbd[2])) {
					setState(State.TELEPORT_TO_BANK);
					return 0;
				}
			}
		}

		final int vialIndex = getInventoryIndex(ITEM_ID_EMPTY_VIAL);

		if (vialIndex != -1) {
			return dropVial(vialIndex);
		}

		if (!Area.TRADE.contains(playerX, playerY) &&
			getAllNpcById(NPC_ID_KING_BLACK_DRAGON)[0] == -1) {
			updateLoot();

			if (loot[0] != -1 && (loot[0] != Food.SHARK.getId() || !isInventoryFull())) {
				if (!isInventoryFull() ||
					(inArray(ITEM_IDS_STACKABLE, loot[0]) && hasInventoryItem(loot[0]))) {
					pickupItem(loot[0], loot[1], loot[2]);
					return SLEEP_ONE_TICK;
				}

				final int sharkIndex = getInventoryIndex(Food.SHARK.getId());

				if (sharkIndex != -1) {
					return consume(sharkIndex);
				}
			}
		}

		if (isInTradeConfirm()) {
			if (!bot.hasLocalConfirmedTrade()) {
				confirmTrade();
			}
			return 0;
		}

		if (isInTradeOffer()) {
			return handleAcceptTrade();
		}

		if (idle) {
			if (playerY == previousY) {
				walkTo(playerX, playerY - 1);
				return SLEEP_ONE_TICK;
			}

			idle = false;
		}

		if (playerX != COORDINATE_TRADE_SPOT.getX() || playerY != COORDINATE_TRADE_SPOT.getY()) {
			walkTo(COORDINATE_TRADE_SPOT.getX(), COORDINATE_TRADE_SPOT.getY());
			return SLEEP_ONE_TICK;
		}

		final int[] mule = getPlayerByName(this.mule);

		if (isMissingSupplies() || (!isInventoryFull() && mule[0] != -1)) {
			if (mule[0] == -1 || System.currentTimeMillis() <= tradeRequestTimeout) {
				return 0;
			}

			sendTradeRequest(getPlayerPID(mule[0]));
			tradeRequestTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		setState(State.KILL);
		return 0;
	}

	private int handleAcceptTrade() {
		final int totalTradeSize = Math.min(getInventoryLootCount() - getEquipmentLootCount(),
			MAX_TRADE_SIZE);

		if (getRemoteTradeItemCount() == 0 &&
			System.currentTimeMillis() > privateMessageTimeout) {
			sendPrivateMessage(createSupplyRequestMessage(totalTradeSize), mule);
			privateMessageTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (bot.getLocalTradeItemCount() == totalTradeSize) {
			if (!hasLocalAcceptedTrade()) {
				acceptTrade();
			}

			return 0;
		}

		for (int index = 0; index < getInventoryCount(); index++) {
			final int itemId = getInventoryId(index);

			if (itemId == Food.SHARK.getId()) {
				continue;
			}

			if (Equipment.isEquipment(itemId)) {
				final int invCount = getInventoryCount(itemId);

				if (invCount == 1) {
					continue;
				}

				final int tradeCount = getTradeItemIdCount(itemId);

				if (tradeCount == invCount - 1) {
					continue;
				}

				if (tradeCount == invCount) {
					removeTradeItemId(itemId, 1);
				} else {
					offerItemTrade(index, 1);
				}

				continue;
			}

			if (inArray(Potion.ANTIDOTE.ids, itemId)) {
				if (!poisoned) {
					offerItemTrade(index, 1);
				}

				continue;
			}

			if (inArray(ITEM_IDS_LOOT, itemId)) {
				final int tradeCount = getTradeItemIdCount(itemId);
				final int invCount = getInventoryCount(itemId);

				if (tradeCount == invCount) {
					continue;
				}

				offerItemTrade(index, invCount);
			}
		}

		return SLEEP_ONE_TICK;
	}

	private int getInventoryLootCount() {
		int count = 0;

		for (int index = 0; index < getInventoryCount(); index++) {
			final int itemId = getInventoryId(index);

			if (itemId == Food.SHARK.getId()) {
				continue;
			}

			if (inArray(ITEM_IDS_LOOT, itemId) ||
				(inArray(Potion.ANTIDOTE.ids, itemId) && !poisoned)) {
				count++;
			}
		}

		return count;
	}

	private int getEquipmentLootCount() {
		int count = 0;

		for (final Equipment equipment : Equipment.VALUES) {
			if (inArray(ITEM_IDS_LOOT, equipment.id) &&
				hasInventoryItem(equipment.id)) {
				count++;
			}
		}

		return count;
	}

	private String createSupplyRequestMessage(final int tradeSize) {
		final int maxTradeSpace = Math.min(tradeSize + getInventoryEmptyCount(), MAX_TRADE_SIZE);

		final int antidote = poisoned && !hasInventoryItem(Potion.ANTIDOTE.ids) ? 1 : 0;
		final int superAtk = !hasInventoryItem(Potion.SUPER_ATTACK.ids) ? 1 : 0;
		final int superDef = !hasInventoryItem(Potion.SUPER_DEFENSE.ids) ? 1 : 0;
		final int superStr = !hasInventoryItem(Potion.SUPER_STRENGTH.ids) ? 1 : 0;

		final int totalPotions = antidote + superAtk + superDef + superStr;

		final int sharks = maxTradeSpace - totalPotions;

		return String.format("%d,%d,%d,%d,%d", antidote, superAtk, superDef, superStr, sharks);
	}

	private int teleportToBank() {
		if (Area.BANK.contains(playerX, playerY)) {
			setState(State.BANK);
			return 0;
		}

		if (Area.DRAGON.contains(playerX, playerY)) {
			if (isQuestMenu()) {
				answer(0);
				optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (inCombat()) {
				walkTo(playerX, playerY);
				return SLEEP_ONE_TICK;
			}

			if (getCurrentHits() <= eatAt) {
				final int sharkIndex = getInventoryIndex(Food.SHARK.getId());

				if (sharkIndex != -1) {
					return consume(sharkIndex);
				}
			}

			if (System.currentTimeMillis() <= optionMenuTimeout) {
				return 0;
			}

			final int amuletIndex = getInventoryIndex(ITEM_ID_CHARGED_DRAGONSTONE_AMULET);

			if (amuletIndex == -1) {
				setState(State.WALK_TO_BANK);
				return 0;
			}

			useItem(amuletIndex);
			optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (distanceTo(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY()) <= 1) {
			if (getObjectIdFromCoords(Object.DOORS.coordinate.getX(),
				Object.DOORS.coordinate.getY()) == Object.DOORS.id) {
				useObject1(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int walkToBank() {
		if (Area.BANK.contains(playerX, playerY)) {
			setState(State.BANK);
			return 0;
		}

		if (distanceTo(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY()) <= 1) {
			if (getObjectIdFromCoords(Object.DOORS.coordinate.getX(),
				Object.DOORS.coordinate.getY()) == Object.DOORS.id) {
				useObject1(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		if (!inCombat()) {
			if (poisoned) {
				final int potionIndex = getInventoryIndex(Potion.ANTIDOTE.ids);

				if (potionIndex != -1) {
					return consume(potionIndex);
				}
			}

			if (getCurrentHits() <= eatAt) {
				final int sharkIndex = getInventoryIndex(Food.SHARK.getId());

				if (sharkIndex != -1) {
					return consume(sharkIndex);
				}
			}
		}

		if (Area.LESSERS.contains(playerX, playerY)) {
			if (distanceTo(Object.GATE.coordinate.getX(), Object.GATE.coordinate.getY()) <= 1) {
				if (inCombat()) {
					walkTo(playerX, playerY);
					return SLEEP_ONE_TICK;
				}

				useObject1(Object.GATE.coordinate.getX(), Object.GATE.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			if (!inCombat()) {
				final int lesserDemon = getBlockingLesserDemon();

				if (lesserDemon != -1) {
					attackNpc(lesserDemon);
					return SLEEP_ONE_TICK;
				}
			}

			walkTo(Object.GATE.coordinate.getX() - 1, Object.GATE.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		if (Area.SPIDERS.contains(playerX, playerY)) {
			if (distanceTo(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY()) <= 1) {
				if (inCombat()) {
					walkTo(playerX, playerY);
					return SLEEP_ONE_TICK;
				}

				useObject1(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			if (!inCombat()) {
				final int poisonSpider = getBlockingPoisonSpider();

				if (poisonSpider != -1) {
					attackNpc(poisonSpider);
					return SLEEP_ONE_TICK;
				}
			}

			walkTo(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		if (Area.DRAGON.contains(playerX, playerY)) {
			if (distanceTo(Object.LEVER_DRAGON.coordinate.getX(), Object.LEVER_DRAGON.coordinate.getY()) <= 1) {
				if (inCombat()) {
					walkTo(playerX, playerY);
					return SLEEP_ONE_TICK;
				}

				useObject1(Object.LEVER_DRAGON.coordinate.getX(), Object.LEVER_DRAGON.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.LEVER_DRAGON.coordinate.getX(), Object.LEVER_DRAGON.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		for (final Coordinate coordinate : PATH_TO_EDGEVILLE) {
			if (playerY < coordinate.getY()) {
				walkTo(coordinate.getX(), coordinate.getY());
				break;
			}
		}

		return SLEEP_ONE_TICK;
	}

	private int getBlockingPoisonSpider() {
		for (int index = 0; index < bot.getNpcCount(); index++) {
			final java.lang.Object npc = bot.getNpc(index);

			if (bot.getNpcId(npc) != NPC_ID_POISON_SPIDER || bot.isMobInCombat(npc)) {
				continue;
			}

			final int npcX = getNpcX(index);
			final int npcY = getNpcY(index);

			if (npcX != playerX) {
				continue;
			}

			if (npcY == playerY + (state == State.WALK_TO_DRAGON ? 1 : -1)) {
				return index;
			}
		}

		return -1;
	}

	private int getBlockingLesserDemon() {
		for (int index = 0; index < bot.getNpcCount(); index++) {
			final java.lang.Object npc = bot.getNpc(index);

			if (bot.getNpcId(npc) != NPC_ID_LESSER_DEMON || bot.isMobInCombat(npc)) {
				continue;
			}

			final int npcX = getNpcX(index);
			final int npcY = getNpcY(index);

			if (npcY != playerY) {
				continue;
			}

			if (npcX == playerX + (state == State.WALK_TO_DRAGON ? -1 : 1)) {
				return index;
			}
		}

		return -1;
	}

	private boolean canDrinkSuperAttack() {
		return getCurrentLevel(Skill.ATTACK.getIndex()) <= MINIMUM_ATK;
	}

	private boolean canDrinkSuperDefense() {
		return getCurrentLevel(Skill.DEFENSE.getIndex()) <= MINIMUM_DEF;
	}

	private boolean canDrinkSuperStrength() {
		return getCurrentLevel(Skill.STRENGTH.getIndex()) <= MINIMUM_STR;
	}

	private void updateLoot() {
		loot[0] = -1;

		int groundItemId;

		for (int index = 0; index < getGroundItemCount(); index++) {
			groundItemId = getGroundItemId(index);

			if (!inArray(ITEM_IDS_LOOT, groundItemId)) {
				continue;
			}

			loot[0] = groundItemId;
			loot[1] = getItemX(index);
			loot[2] = getItemY(index);
			break;
		}
	}

	private int consume(final int inventoryIndex) {
		if (System.currentTimeMillis() <= actionTimeout) {
			return 0;
		}

		useItem(inventoryIndex);
		actionTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private int dropVial(final int inventoryIndex) {
		if (System.currentTimeMillis() <= actionTimeout) {
			return 0;
		}

		dropItem(inventoryIndex);
		actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private enum State {
		BANK("Bank"),
		WALK_TO_DRAGON("Walk to Dragon"),
		KILL("Slay Dragon"),
		TRADE("Trade"),
		TELEPORT_TO_BANK("Teleport to Bank"),
		WALK_TO_BANK("Walk to Bank");

		private final String description;

		State(final String description) {
			this.description = description;
		}

		@Override
		public String toString() {
			return description;
		}
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(212, 448), new Coordinate(220, 453)),
		LESSERS(new Coordinate(278, 181), new Coordinate(284, 187)) {
			@Override
			public boolean contains(final int x, final int y) {
				return super.contains(x, y) || (x >= 280 && x <= 283 && y >= 188 && y <= 189);
			}
		},
		SPIDERS(new Coordinate(279, 3014), new Coordinate(283, 3020)),
		TRADE(new Coordinate(565, 3329), new Coordinate(569, 3331)),
		DRAGON(new Coordinate(563, 3315), new Coordinate(571, 3331));

		private final Coordinate lowerBoundingCoordinate;
		private final Coordinate upperBoundingCoordinate;

		Area(final Coordinate lowerBoundingCoordinate, final Coordinate upperBoundingCoordinate) {
			this.lowerBoundingCoordinate = lowerBoundingCoordinate;
			this.upperBoundingCoordinate = upperBoundingCoordinate;
		}

		public Coordinate getLowerBoundingCoordinate() {
			return lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return upperBoundingCoordinate;
		}
	}

	private enum Object implements RSObject {
		DOORS(64, new Coordinate(217, 447)),
		GATE(508, new Coordinate(285, 185)),
		LADDER_DOWN(6, new Coordinate(282, 185)),
		LADDER_UP(5, new Coordinate(282, 3017)),
		LEVER_SPIDER(487, new Coordinate(282, 3020)),
		LEVER_DRAGON(488, new Coordinate(567, 3331));

		private final int id;
		private final Coordinate coordinate;

		Object(final int id, final Coordinate coordinate) {
			this.id = id;
			this.coordinate = coordinate;
		}

		public int getId() {
			return id;
		}

		public Coordinate getCoordinate() {
			return coordinate;
		}
	}

	private enum Potion {
		ANTIDOTE(new int[]{568, 571, 567, 570, 566, 569}),
		SUPER_ATTACK(new int[]{486, 487, 488, 474, 475, 476}),
		SUPER_DEFENSE(new int[]{495, 496, 497, 480, 481, 482}),
		SUPER_STRENGTH(new int[]{492, 493, 494, 221, 222, 223, 224});

		private static final Potion[] VALUES = Potion.values();

		private final int[] ids;
		private long timeout;

		Potion(final int[] ids) {
			this.ids = ids;
		}
	}

	private enum Equipment {
		HELMET(795),
		BODY(401),
		LEGS(402),
		SHIELD(1278),
		ANTI_SHIELD(420),
		WEAPON(593),
		AMULET(597),
		CAPE(1288);

		private static final Equipment[] VALUES = Equipment.values();

		private final int id;
		private long timeout;

		Equipment(final int id) {
			this.id = id;
		}

		private static boolean isEquipment(final int id) {
			for (final Equipment equipment : VALUES) {
				if (id == equipment.id) {
					return true;
				}
			}

			return false;
		}
	}
}
