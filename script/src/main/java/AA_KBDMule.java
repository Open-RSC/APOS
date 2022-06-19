import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Trades supplies and banks loot.
 * <p>
 * Instructions:
 * Start script at Edgeville Bank with an empty inventory.
 * <p>
 * Banked (multiple):
 * runechain, runelegs, antidshield, charged dstone amulet,
 * sharks, regular/super set pots, cure/anti poison pots
 * <p>
 * Required Parameter:
 * name of player running AA_KBDMaster script
 * <p>
 * Notes:
 * Modify the Equipment enum directly to change item ids.
 * <p>
 *
 * @Author Chomp
 */
public class AA_KBDMule extends AA_Script {
	private static final Coordinate[] PATH_TO_KBD = new Coordinate[]{
		new Coordinate(239, 432),
		new Coordinate(240, 396),
		new Coordinate(266, 363),
		new Coordinate(279, 336),
		new Coordinate(299, 288),
		new Coordinate(296, 241),
		new Coordinate(285, 193),
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
		COORDINATE_TRADE_SPOT = new Coordinate(565, 3331);

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

	private static final int
		NPC_ID_POISON_SPIDER = 292,
		NPC_ID_LESSER_DEMON = 22,
		ITEM_ID_CHARGED_DRAGONSTONE_AMULET = 597,
		ITEM_ID_DRAGONSTONE_AMULET = 522,
		ITEM_ID_EMPTY_VIAL = 465,
		MINIMUM_SHARKS = 10,
		PAINT_OFFSET_X_ALT = 185;

	private final Map<Integer, Integer> notableLoot = new TreeMap<>();

	private String master;
	private State state;
	private SupplyRequest supplyRequest;
	private PathWalker pathWalker;
	private long startTime;

	private long actionTimeout, withdrawFoodTimeout, optionMenuTimeout, tradeOfferTimeout;

	private int playerX, playerY, eatAt, deaths;

	private String
		sharksRemaining = "n/a",
		amuletsRemaining = "n/a",
		antidotesRemaining = "n/a",
		superSetsRemaining = "n/a";

	private boolean poisoned, died, idle, abort;

	public AA_KBDMule(final Extension extension) {
		super(extension);
		combatStyle = CombatStyle.DEFENSE;
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) printInstructions();

		master = parameters.replace('_', ' ');

		final State initialState;

		if (Area.BANK.contains(getX(), getY())) {
			initialState = State.BANK;
		} else if (Area.DRAGON.contains(getX(), getY())) {
			initialState = State.TRADE;
		} else {
			initialState = State.WALK_TO_DRAGON;
		}

		setCombatStyle(combatStyle.getIndex());
		eatAt = getBaseHits() / 2;
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

		if (bot.getCombatStyle() != combatStyle.getIndex()) setCombatStyle(combatStyle.getIndex());

		switch (state) {
			case BANK:
				return bank();
			case WALK_TO_DRAGON:
				return walkToDragon();
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
			actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("poison potion") || message.endsWith("antidote potion")) {
			poisoned = false;
		} else if (message.endsWith("left") ||
			message.startsWith("finished", 9) ||
			message.endsWith("Vial")) {
			actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.startsWith("poisioned", 23)) {
			poisoned = true;
		} else if (message.startsWith("declined", 17) || message.endsWith("successfully")) {
			supplyRequest = null;
		} else if (message.endsWith("area")) {
			idle = true;
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
	}

	private void abort(final String reason) {
		sendPrivateMessage("abort", master);
		exit(reason);
	}

	private void setState(final State state) {
		this.state = state;

		if (this.state == State.BANK) {
			if (abort) {
				exit("Trip aborted.");
				return;
			}

			updateNotableLoot();
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

			for (final Entry<Integer, Integer> entry : notableLoot.entrySet()) {
				drawString(String.format("@gr1@%s: @whi@%d", getItemNameId(entry.getKey()), entry.getValue()),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
			}
		}

		y = 0;

		drawString("@yel@Supplies Remaining",
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@or1@Sharks: @whi@%s", sharksRemaining),
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@ora@Amulets: @whi@%s", amuletsRemaining),
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@or2@Antidotes: @whi@%s", antidotesRemaining),
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@or3@Super sets: @whi@%s", superSetsRemaining),
			PAINT_OFFSET_X_ALT, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	@Override
	public void onTradeRequest(final String playerName) {
		if (state != State.TRADE || !playerName.equalsIgnoreCase(master)) {
			return;
		}

		final int[] master = getPlayerByName(this.master);

		if (master[0] == -1) {
			return;
		}

		supplyRequest = null;
		sendTradeRequest(getPlayerPID(master[0]));
	}

	@Override
	public void onPrivateMessage(final String message, final String playerName, final boolean moderator,
								 final boolean administrator) {
		if (!playerName.equalsIgnoreCase(master)) {
			return;
		}

		if (message.equalsIgnoreCase("abort")) {
			abort = true;
			return;
		}

		try {
			supplyRequest = SupplyRequest.from(message);
		} catch (final IllegalArgumentException e) {
			System.err.println(e.getMessage());
		}
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

			if (inArray(ITEM_IDS_LOOT, itemId) ||
				itemId == ITEM_ID_DRAGONSTONE_AMULET ||
				itemId == ITEM_ID_EMPTY_VIAL) {
				deposit(itemId, getInventoryCount(itemId));
				return SLEEP_ONE_TICK;
			}
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
			final int potionCount = getInventoryCount(potion.ids);

			if (potionCount == 1) {
				continue;
			}

			if (System.currentTimeMillis() <= potion.timeout) {
				return 0;
			}

			if (potionCount == 0) {
				for (final int id : potion.ids) {
					if (!hasBankItem(id)) {
						continue;
					}

					withdraw(id, 1);
					potion.timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
					return 0;
				}
			} else {
				for (final int id : potion.ids) {
					if (!hasInventoryItem(id)) {
						continue;
					}

					deposit(id, 1);
					potion.timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
					return 0;
				}
			}
		}

		if (!isInventoryFull()) {
			if (System.currentTimeMillis() <= withdrawFoodTimeout) {
				return 0;
			}

			if (!hasBankItem(Food.SHARK.getId())) {
				abort(String.format("Out of item: %s.", Food.SHARK));
			}

			withdraw(Food.SHARK.getId(), getInventoryEmptyCount());
			withdrawFoodTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		updateSuppliesRemaining();
		setState(State.WALK_TO_DRAGON);
		return 0;
	}

	private int walkToDragon() {
		if (Area.DRAGON.contains(playerX, playerY)) {
			setState(State.TRADE);
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
			if (distanceTo(Object.LADDER_DOWN.coordinate.getX(),
				Object.LADDER_DOWN.coordinate.getY()) <= 1) {
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
			final int equipmentIndex = getInventoryIndex(equipment.id);

			if (equipmentIndex != -1 && !isItemEquipped(equipmentIndex)) {
				wearItem(equipmentIndex);
				return true;
			}
		}

		return false;
	}

	private int trade() {
		final int sharkCount = getInventoryCount(Food.SHARK.getId());

		if (sharkCount == 0) {
			setState(State.TELEPORT_TO_BANK);
			return 0;
		}

		final boolean lowOnSupplies = sharkCount < MINIMUM_SHARKS ||
			!hasInventoryItem(Potion.SUPER_ATTACK.ids) ||
			!hasInventoryItem(Potion.SUPER_DEFENSE.ids) ||
			!hasInventoryItem(Potion.SUPER_STRENGTH.ids);

		if (lowOnSupplies) {
			final int[] master = getPlayerByName(this.master);

			if (master[0] == -1 || !Area.TRADE.contains(master[1], master[2])) {
				setState(State.TELEPORT_TO_BANK);
				return 0;
			}
		}

		if (inCombat()) {
			walkTo(playerX, playerY);
			return 0;
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
		}

		final int vialIndex = getInventoryIndex(ITEM_ID_EMPTY_VIAL);

		if (vialIndex != -1) {
			if (System.currentTimeMillis() <= actionTimeout) {
				return 0;
			}

			dropItem(vialIndex);
			actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
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
			if (playerY == COORDINATE_TRADE_SPOT.getY()) {
				walkTo(COORDINATE_TRADE_SPOT.getX(), COORDINATE_TRADE_SPOT.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			idle = false;
		}

		if (playerX != COORDINATE_TRADE_SPOT.getX() || playerY != COORDINATE_TRADE_SPOT.getY()) {
			walkTo(COORDINATE_TRADE_SPOT.getX(), COORDINATE_TRADE_SPOT.getY());
		}

		if (abort) {
			setState(State.TELEPORT_TO_BANK);
		}

		return SLEEP_ONE_TICK;
	}

	private int handleAcceptTrade() {
		if (hasLocalAcceptedTrade() || supplyRequest == null) {
			return 0;
		}

		for (final Potion potion : Potion.values()) {
			if (!supplyRequest.isPotionRequested(potion) || !hasInventoryItem(potion.ids)) {
				continue;
			}

			if (hasTradeItem(potion.ids)) {
				continue;
			}

			if (System.currentTimeMillis() <= potion.timeout) {
				return 0;
			}

			offerItemTrade(getInventoryIndex(potion.ids), 1);
			potion.timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		final int sharkTradeCount = getTradeItemIdCount(Food.SHARK.getId());
		final int sharkInvCount = getInventoryCount(Food.SHARK.getId());

		final int requiredCount = Math.min(sharkInvCount, supplyRequest.sharks);

		if (sharkTradeCount == requiredCount) {
			acceptTrade();
			return 0;
		}

		if (System.currentTimeMillis() <= tradeOfferTimeout) {
			return 0;
		}

		if (sharkTradeCount > requiredCount) {
			removeTradeItemId(Food.SHARK.getId(), sharkTradeCount - requiredCount);
		} else {
			offerTradeItemId(Food.SHARK.getId(), requiredCount - sharkTradeCount);
		}

		tradeOfferTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
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

	private int consume(final int inventoryIndex) {
		if (System.currentTimeMillis() <= actionTimeout) {
			return 0;
		}

		useItem(inventoryIndex);
		actionTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private void updateSuppliesRemaining() {
		int antidoteCount = 0, supAtkCount = 0, supStrCount = 0, supDefCount = 0;

		int itemId;

		for (int index = 0; index < getBankSize(); index++) {
			itemId = getBankId(index);

			if (itemId == Food.SHARK.getId()) {
				sharksRemaining = DECIMAL_FORMAT.format(getBankStack(index));
			} else if (itemId == Equipment.AMULET.id) {
				amuletsRemaining = DECIMAL_FORMAT.format(getBankStack(index));
			} else if (inArray(Potion.ANTIDOTE.ids, itemId)) {
				antidoteCount += getBankStack(index);
			} else if (inArray(Potion.SUPER_ATTACK.ids, itemId)) {
				supAtkCount += getBankStack(index);
			} else if (inArray(Potion.SUPER_DEFENSE.ids, itemId)) {
				supDefCount += getBankStack(index);
			} else if (inArray(Potion.SUPER_STRENGTH.ids, itemId)) {
				supStrCount += getBankStack(index);
			}
		}

		antidotesRemaining = DECIMAL_FORMAT.format(antidoteCount);
		superSetsRemaining = DECIMAL_FORMAT.format(Math.min(supAtkCount, Math.min(supDefCount, supStrCount)));
	}

	private enum State {
		BANK("Bank"),
		WALK_TO_DRAGON("Walk to Dragon"),
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
		BODY(400),
		LEGS(402),
		SHIELD(420),
		AMULET(597);

		private static final Equipment[] VALUES = Equipment.values();

		private final int id;
		private long timeout;

		Equipment(final int id) {
			this.id = id;
		}
	}

	private static final class SupplyRequest {
		private static final int NUMBER_OF_ARGUMENTS = 5;

		private final boolean antidote;
		private final boolean superAtk;
		private final boolean superDef;
		private final boolean superStr;
		private final int sharks;

		private SupplyRequest(final boolean antidote, final boolean superAtk, final boolean superDef,
							  final boolean superStr, final int sharks) {
			this.antidote = antidote;
			this.superAtk = superAtk;
			this.superDef = superDef;
			this.superStr = superStr;
			this.sharks = sharks;
		}

		private static SupplyRequest from(final String request) throws IllegalArgumentException {
			final String[] data = request.split(",");

			if (data.length != NUMBER_OF_ARGUMENTS) {
				throw new IllegalArgumentException(String.format("Invalid supply request: %s", request));
			}

			final SupplyRequest supplyRequest;

			try {
				supplyRequest = new SupplyRequest(SupplyRequest.parse(data[0]),
					SupplyRequest.parse(data[1]),
					SupplyRequest.parse(data[2]),
					SupplyRequest.parse(data[3]),
					Integer.parseInt(data[4]));
			} catch (final NumberFormatException e) {
				throw new IllegalArgumentException(String.format("Malformed supply request: %s", request), e);
			}

			return supplyRequest;
		}

		private static boolean parse(final String str) {
			return Integer.parseInt(str) == 1;
		}

		private boolean isPotionRequested(final Potion potion) {
			switch (potion) {
				case ANTIDOTE:
					return antidote;
				case SUPER_ATTACK:
					return superAtk;
				case SUPER_DEFENSE:
					return superDef;
				case SUPER_STRENGTH:
					return superStr;
				default:
					return false;
			}
		}
	}
}
