import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
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
 * Author: Chomp
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
		MINIMUM_SHARKS = 10;

	private final Map<Integer, Integer> notableLoot = new TreeMap<>();

	private String master;
	private State state;
	private SupplyRequest supplyRequest;
	private PathWalker pathWalker;
	private Instant startTime;

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
		this.combatStyle = CombatStyle.DEFENSE;
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) {
			throw new IllegalArgumentException("Empty parameters.");
		}

		this.master = parameters.replace('_', ' ');

		final State initialState;

		if (Area.BANK.contains(this.getX(), this.getY())) {
			initialState = State.BANK;
		} else if (Area.DRAGON.contains(this.getX(), this.getY())) {
			initialState = State.TRADE;
		} else {
			initialState = State.WALK_TO_DRAGON;
		}

		this.setFightMode(this.combatStyle.getIndex());
		this.eatAt = this.getBaseHits() / 2;
		this.startTime = Instant.now();
		this.setState(initialState);
	}

	@Override
	public int main() {
		if (this.died) {
			if (this.isDead() || (this.pathWalker != null && this.pathWalker.walkPath())) {
				return 0;
			}

			this.pathWalker = null;
			this.died = false;
		}

		this.playerX = this.getX();
		this.playerY = this.getY();

		switch (this.state) {
			case BANK:
				return this.bank();
			case WALK_TO_DRAGON:
				return this.walkToDragon();
			case TRADE:
				return this.trade();
			case TELEPORT_TO_BANK:
				return this.teleportToBank();
			case WALK_TO_BANK:
				return this.walkToBank();
			default:
				return this.exit("Invalid script state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("shark")) {
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("poison potion") || message.endsWith("antidote potion")) {
			this.poisoned = false;
		} else if (message.endsWith("left") ||
			message.startsWith("finished", 9) ||
			message.endsWith("Vial")) {
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.startsWith("poisioned", 23)) {
			this.poisoned = true;
		} else if (message.startsWith("declined", 17) || message.endsWith("successfully")) {
			this.supplyRequest = null;
		} else if (message.endsWith("area")) {
			this.idle = true;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void onDeath() {
		if (this.pathWalker == null) {
			this.pathWalker = new PathWalker(this.extension);
			this.pathWalker.init(null);
		}

		final PathWalker.Path path = this.pathWalker.calcPath(COORDINATE_DEATH_WALK_LUMBRIDGE.getX(),
			COORDINATE_DEATH_WALK_LUMBRIDGE.getY(),
			COORDINATE_DEATH_WALK_EDGEVILLE.getX(),
			COORDINATE_DEATH_WALK_EDGEVILLE.getY());

		if (path == null) {
			this.abort("Failed to calculate path from Lumbridge to Edgeville.");
		}

		this.pathWalker.setPath(path);

		this.setState(State.BANK);

		this.died = true;
		this.deaths++;
		this.idle = false;
		this.poisoned = false;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@King Black Dragon", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@State: @cya@%s", this.state),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (!this.notableLoot.isEmpty()) {
			this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			for (final Entry<Integer, Integer> entry : this.notableLoot.entrySet()) {
				this.drawString(String.format("@gr1@%s: @whi@%d", getItemNameId(entry.getKey()), entry.getValue()),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
			}
		}

		y = 0;

		this.drawString("@yel@Supplies Remaining",
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@or1@Sharks: @whi@%s", this.sharksRemaining),
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@ora@Amulets: @whi@%s", this.amuletsRemaining),
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@or2@Antidotes: @whi@%s", this.antidotesRemaining),
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@or3@Super sets: @whi@%s", this.superSetsRemaining),
			PAINT_OFFSET_X_ALT, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
	}

	@Override
	public void onTradeRequest(final String playerName) {
		if (this.state != State.TRADE || !playerName.equalsIgnoreCase(this.master)) {
			return;
		}

		final int[] master = this.getPlayerByName(this.master);

		if (master[0] == -1) {
			return;
		}

		this.supplyRequest = null;
		this.sendTradeRequest(this.getPlayerPID(master[0]));
	}

	@Override
	public void onPrivateMessage(final String message, final String playerName, final boolean moderator,
								 final boolean administrator) {
		if (!playerName.equalsIgnoreCase(this.master)) {
			return;
		}

		if (message.equalsIgnoreCase("abort")) {
			this.abort = true;
			return;
		}

		try {
			this.supplyRequest = SupplyRequest.from(message);
		} catch (final IllegalArgumentException e) {
			System.err.println(e.getMessage());
		}
	}

	private void setState(final State state) {
		this.state = state;

		if (this.state == State.BANK) {
			if (this.abort) {
				this.exit("Trip aborted.");
				return;
			}

			this.updateNotableLoot();
		}
	}

	private int bank() {
		if (this.getCurrentHits() != this.getBaseHits()) {
			final int sharkIndex = this.getInventoryIndex(Food.SHARK.getId());

			if (sharkIndex != -1) {
				return this.consume(sharkIndex);
			}
		}

		if (this.poisoned) {
			final int potionIndex = this.getInventoryIndex(Potion.ANTIDOTE.ids);

			if (potionIndex != -1) {
				return this.consume(potionIndex);
			}
		}

		if (!this.isBanking()) {
			return this.openBank();
		}

		for (int index = 0; index < this.getInventoryCount(); index++) {
			final int itemId = this.getInventoryId(index);

			if (itemId == Food.SHARK.getId()) {
				continue;
			}

			if (inArray(ITEM_IDS_LOOT, itemId) ||
				itemId == ITEM_ID_DRAGONSTONE_AMULET ||
				itemId == ITEM_ID_EMPTY_VIAL) {
				this.deposit(itemId, this.getInventoryCount(itemId));
				return SLEEP_ONE_TICK;
			}
		}

		for (final Equipment equipment : Equipment.VALUES) {
			if (this.hasInventoryItem(equipment.id)) {
				continue;
			}

			if (System.currentTimeMillis() <= equipment.timeout) {
				return 0;
			}

			if (!this.hasBankItem(equipment.id)) {
				this.abort(String.format("Out of item: %s.", getItemNameId(equipment.id)));
			}

			this.withdraw(equipment.id, 1);
			equipment.timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		for (final Potion potion : Potion.VALUES) {
			final int potionCount = this.getInventoryCount(potion.ids);

			if (potionCount == 1) {
				continue;
			}

			if (System.currentTimeMillis() <= potion.timeout) {
				return 0;
			}

			if (potionCount == 0) {
				for (final int id : potion.ids) {
					if (!this.hasBankItem(id)) {
						continue;
					}

					this.withdraw(id, 1);
					potion.timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
					return 0;
				}
			} else {
				for (final int id : potion.ids) {
					if (!this.hasInventoryItem(id)) {
						continue;
					}

					this.deposit(id, 1);
					potion.timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
					return 0;
				}
			}
		}

		if (!this.isInventoryFull()) {
			if (System.currentTimeMillis() <= this.withdrawFoodTimeout) {
				return 0;
			}

			if (!this.hasBankItem(Food.SHARK.getId())) {
				this.abort(String.format("Out of item: %s.", Food.SHARK));
			}

			this.withdraw(Food.SHARK.getId(), this.getInventoryEmptyCount());
			this.withdrawFoodTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		this.updateSuppliesRemaining();
		this.setState(State.WALK_TO_DRAGON);
		return 0;
	}

	private int walkToDragon() {
		if (Area.DRAGON.contains(this.playerX, this.playerY)) {
			this.setState(State.TRADE);
			return 0;
		}

		if (!this.inCombat()) {
			if (this.poisoned) {
				final int potionIndex = this.getInventoryIndex(Potion.ANTIDOTE.ids);

				if (potionIndex != -1) {
					return this.consume(potionIndex);
				}
			}

			if (this.getCurrentHits() <= this.eatAt) {
				final int sharkIndex = this.getInventoryIndex(Food.SHARK.getId());

				if (sharkIndex != -1) {
					return this.consume(sharkIndex);
				}
			}
		}

		if (Area.SPIDERS.contains(this.playerX, this.playerY)) {
			if (this.distanceTo(Object.LEVER_SPIDER.coordinate.getX(), Object.LEVER_SPIDER.coordinate.getY()) <= 1) {
				if (this.inCombat()) {
					this.walkTo(this.playerX, this.playerY);
					return SLEEP_ONE_TICK;
				}

				this.useObject1(Object.LEVER_SPIDER.coordinate.getX(), Object.LEVER_SPIDER.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			if (!this.inCombat()) {
				final int poisonSpider = this.getBlockingPoisonSpider();

				if (poisonSpider != -1) {
					this.attackNpc(poisonSpider);
					return SLEEP_ONE_TICK;
				}
			}

			this.walkTo(Object.LEVER_SPIDER.coordinate.getX(), Object.LEVER_SPIDER.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.LESSERS.contains(this.playerX, this.playerY)) {
			if (this.distanceTo(Object.LADDER_DOWN.coordinate.getX(),
				Object.LADDER_DOWN.coordinate.getY()) <= 1) {
				if (this.inCombat()) {
					this.walkTo(this.playerX, this.playerY);
					return SLEEP_ONE_TICK;
				}

				this.useObject1(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			if (!this.inCombat()) {
				final int lesserDemon = this.getBlockingLesserDemon();

				if (lesserDemon != -1) {
					this.attackNpc(lesserDemon);
					return SLEEP_ONE_TICK;
				}
			}

			this.walkTo(Object.LADDER_DOWN.coordinate.getX() + 1, Object.LADDER_DOWN.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.distanceTo(Object.GATE.coordinate.getX(), Object.GATE.coordinate.getY()) <= 1) {
			if (this.inCombat()) {
				this.walkTo(this.playerX, this.playerY);
				return SLEEP_ONE_TICK;
			}

			this.useObject1(Object.GATE.coordinate.getX(), Object.GATE.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (this.wieldEquipment()) {
				return SLEEP_ONE_TICK;
			}

			if (this.getObjectIdFromCoords(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY()) ==
				Object.DOORS.id) {
				this.atObject(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}
		}

		for (final Coordinate coordinate : PATH_TO_KBD) {
			if (this.playerY > coordinate.getY()) {
				this.walkTo(coordinate.getX(), coordinate.getY());
				break;
			}
		}

		return SLEEP_ONE_TICK;
	}

	private boolean wieldEquipment() {
		for (final Equipment equipment : Equipment.VALUES) {
			final int equipmentIndex = this.getInventoryIndex(equipment.id);

			if (equipmentIndex != -1 && !this.isItemEquipped(equipmentIndex)) {
				this.wearItem(equipmentIndex);
				return true;
			}
		}

		return false;
	}

	private int trade() {
		final int sharkCount = this.getInventoryCount(Food.SHARK.getId());

		if (sharkCount == 0) {
			this.setState(State.TELEPORT_TO_BANK);
			return 0;
		}

		final boolean lowOnSupplies = sharkCount < MINIMUM_SHARKS ||
			!this.hasInventoryItem(Potion.SUPER_ATTACK.ids) ||
			!this.hasInventoryItem(Potion.SUPER_DEFENSE.ids) ||
			!this.hasInventoryItem(Potion.SUPER_STRENGTH.ids);

		if (lowOnSupplies) {
			final int[] master = this.getPlayerByName(this.master);

			if (master[0] == -1 || !Area.TRADE.contains(master[1], master[2])) {
				this.setState(State.TELEPORT_TO_BANK);
				return 0;
			}
		}

		if (this.inCombat()) {
			this.walkTo(this.playerX, this.playerY);
			return 0;
		}

		if (this.poisoned) {
			final int potionIndex = this.getInventoryIndex(Potion.ANTIDOTE.ids);

			if (potionIndex != -1) {
				return this.consume(potionIndex);
			}
		}

		if (this.getCurrentHits() <= this.eatAt) {
			final int sharkIndex = this.getInventoryIndex(Food.SHARK.getId());

			if (sharkIndex != -1) {
				return this.consume(sharkIndex);
			}
		}

		final int vialIndex = this.getInventoryIndex(ITEM_ID_EMPTY_VIAL);

		if (vialIndex != -1) {
			if (System.currentTimeMillis() <= this.actionTimeout) {
				return 0;
			}

			this.dropItem(vialIndex);
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (this.isInTradeConfirm()) {
			if (!this.extension.hasLocalConfirmedTrade()) {
				this.confirmTrade();
			}

			return 0;
		}

		if (this.isInTradeOffer()) {
			return this.handleAcceptTrade();
		}

		if (this.idle) {
			if (this.playerY == COORDINATE_TRADE_SPOT.getY()) {
				this.walkTo(COORDINATE_TRADE_SPOT.getX(), COORDINATE_TRADE_SPOT.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			this.idle = false;
		}

		if (this.playerX != COORDINATE_TRADE_SPOT.getX() || this.playerY != COORDINATE_TRADE_SPOT.getY()) {
			this.walkTo(COORDINATE_TRADE_SPOT.getX(), COORDINATE_TRADE_SPOT.getY());
		}

		if (this.abort) {
			this.setState(State.TELEPORT_TO_BANK);
		}

		return SLEEP_ONE_TICK;
	}

	private int handleAcceptTrade() {
		if (this.hasLocalAcceptedTrade() || this.supplyRequest == null) {
			return 0;
		}

		for (final Potion potion : Potion.values()) {
			if (!this.supplyRequest.isPotionRequested(potion) || !this.hasInventoryItem(potion.ids)) {
				continue;
			}

			if (this.hasTradeItem(potion.ids)) {
				continue;
			}

			if (System.currentTimeMillis() <= potion.timeout) {
				return 0;
			}

			this.offerItemTrade(this.getInventoryIndex(potion.ids), 1);
			potion.timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		final int sharkTradeCount = this.getTradeItemIdCount(Food.SHARK.getId());
		final int sharkInvCount = this.getInventoryCount(Food.SHARK.getId());

		final int requiredCount = Math.min(sharkInvCount, this.supplyRequest.sharks);

		if (sharkTradeCount == requiredCount) {
			this.acceptTrade();
			return 0;
		}

		if (System.currentTimeMillis() <= this.tradeOfferTimeout) {
			return 0;
		}

		if (sharkTradeCount > requiredCount) {
			this.removeTradeItemId(Food.SHARK.getId(), sharkTradeCount - requiredCount);
		} else {
			this.offerTradeItemId(Food.SHARK.getId(), requiredCount - sharkTradeCount);
		}

		this.tradeOfferTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int teleportToBank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			this.setState(State.BANK);
			return 0;
		}

		if (Area.DRAGON.contains(this.playerX, this.playerY)) {
			if (this.isQuestMenu()) {
				this.answer(0);
				this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (this.inCombat()) {
				this.walkTo(this.playerX, this.playerY);
				return SLEEP_ONE_TICK;
			}

			if (this.getCurrentHits() <= this.eatAt) {
				final int sharkIndex = this.getInventoryIndex(Food.SHARK.getId());

				if (sharkIndex != -1) {
					return this.consume(sharkIndex);
				}
			}

			if (System.currentTimeMillis() <= this.optionMenuTimeout) {
				return 0;
			}

			final int amuletIndex = this.getInventoryIndex(ITEM_ID_CHARGED_DRAGONSTONE_AMULET);

			if (amuletIndex == -1) {
				this.setState(State.WALK_TO_BANK);
				return 0;
			}

			this.useItem(amuletIndex);
			this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (this.distanceTo(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY()) <= 1) {
			if (this.getObjectIdFromCoords(Object.DOORS.coordinate.getX(),
				Object.DOORS.coordinate.getY()) == Object.DOORS.id) {
				this.useObject1(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int walkToBank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			this.setState(State.BANK);
			return 0;
		}

		if (this.distanceTo(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY()) <= 1) {
			if (this.getObjectIdFromCoords(Object.DOORS.coordinate.getX(),
				Object.DOORS.coordinate.getY()) == Object.DOORS.id) {
				this.useObject1(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.DOORS.coordinate.getX(), Object.DOORS.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		if (!this.inCombat()) {
			if (this.poisoned) {
				final int potionIndex = this.getInventoryIndex(Potion.ANTIDOTE.ids);

				if (potionIndex != -1) {
					return this.consume(potionIndex);
				}
			}

			if (this.getCurrentHits() <= this.eatAt) {
				final int sharkIndex = this.getInventoryIndex(Food.SHARK.getId());

				if (sharkIndex != -1) {
					return this.consume(sharkIndex);
				}
			}
		}

		if (Area.LESSERS.contains(this.playerX, this.playerY)) {
			if (this.distanceTo(Object.GATE.coordinate.getX(), Object.GATE.coordinate.getY()) <= 1) {
				if (this.inCombat()) {
					this.walkTo(this.playerX, this.playerY);
					return SLEEP_ONE_TICK;
				}

				this.useObject1(Object.GATE.coordinate.getX(), Object.GATE.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			if (!this.inCombat()) {
				final int lesserDemon = this.getBlockingLesserDemon();

				if (lesserDemon != -1) {
					this.attackNpc(lesserDemon);
					return SLEEP_ONE_TICK;
				}
			}

			this.walkTo(Object.GATE.coordinate.getX() - 1, Object.GATE.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		if (Area.SPIDERS.contains(this.playerX, this.playerY)) {
			if (this.distanceTo(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY()) <= 1) {
				if (this.inCombat()) {
					this.walkTo(this.playerX, this.playerY);
					return SLEEP_ONE_TICK;
				}

				this.useObject1(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			if (!this.inCombat()) {
				final int poisonSpider = this.getBlockingPoisonSpider();

				if (poisonSpider != -1) {
					this.attackNpc(poisonSpider);
					return SLEEP_ONE_TICK;
				}
			}

			this.walkTo(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		if (Area.DRAGON.contains(this.playerX, this.playerY)) {
			if (this.distanceTo(Object.LEVER_DRAGON.coordinate.getX(), Object.LEVER_DRAGON.coordinate.getY()) <= 1) {
				if (this.inCombat()) {
					this.walkTo(this.playerX, this.playerY);
					return SLEEP_ONE_TICK;
				}

				this.useObject1(Object.LEVER_DRAGON.coordinate.getX(), Object.LEVER_DRAGON.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.LEVER_DRAGON.coordinate.getX(), Object.LEVER_DRAGON.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		for (final Coordinate coordinate : PATH_TO_EDGEVILLE) {
			if (this.playerY < coordinate.getY()) {
				this.walkTo(coordinate.getX(), coordinate.getY());
				break;
			}
		}

		return SLEEP_ONE_TICK;
	}

	private int getBlockingPoisonSpider() {
		for (int index = 0; index < this.extension.getNpcCount(); index++) {
			final java.lang.Object npc = this.extension.getNpc(index);

			if (this.extension.getNpcId(npc) != NPC_ID_POISON_SPIDER || this.extension.isMobInCombat(npc)) {
				continue;
			}

			final int npcX = this.getNpcX(index);
			final int npcY = this.getNpcY(index);

			if (npcX != this.playerX) {
				continue;
			}

			if (npcY == this.playerY + (this.state == State.WALK_TO_DRAGON ? 1 : -1)) {
				return index;
			}
		}

		return -1;
	}

	private int getBlockingLesserDemon() {
		for (int index = 0; index < this.extension.getNpcCount(); index++) {
			final java.lang.Object npc = this.extension.getNpc(index);

			if (this.extension.getNpcId(npc) != NPC_ID_LESSER_DEMON || this.extension.isMobInCombat(npc)) {
				continue;
			}

			final int npcX = this.getNpcX(index);
			final int npcY = this.getNpcY(index);

			if (npcY != this.playerY) {
				continue;
			}

			if (npcX == this.playerX + (this.state == State.WALK_TO_DRAGON ? -1 : 1)) {
				return index;
			}
		}

		return -1;
	}

	private int consume(final int inventoryIndex) {
		if (System.currentTimeMillis() <= this.actionTimeout) {
			return 0;
		}

		this.useItem(inventoryIndex);
		this.actionTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private void updateNotableLoot() {
		for (int index = 0; index < this.getInventoryCount(); index++) {
			final int itemId = this.getInventoryId(index);

			if (!inArray(ITEM_IDS_LOOT_NOTABLE, itemId)) {
				continue;
			}

			this.notableLoot.merge(itemId, this.getInventoryStack(index), Integer::sum);
		}
	}

	private void updateSuppliesRemaining() {
		int antidoteCount = 0, supAtkCount = 0, supStrCount = 0, supDefCount = 0;

		int itemId;

		for (int index = 0; index < this.getBankSize(); index++) {
			itemId = this.getBankId(index);

			if (itemId == Food.SHARK.getId()) {
				this.sharksRemaining = DECIMAL_FORMAT.format(this.getBankStack(index));
			} else if (itemId == Equipment.AMULET.id) {
				this.amuletsRemaining = DECIMAL_FORMAT.format(this.getBankStack(index));
			} else if (inArray(Potion.ANTIDOTE.ids, itemId)) {
				antidoteCount += this.getBankStack(index);
			} else if (inArray(Potion.SUPER_ATTACK.ids, itemId)) {
				supAtkCount += this.getBankStack(index);
			} else if (inArray(Potion.SUPER_DEFENSE.ids, itemId)) {
				supDefCount += this.getBankStack(index);
			} else if (inArray(Potion.SUPER_STRENGTH.ids, itemId)) {
				supStrCount += this.getBankStack(index);
			}
		}

		this.antidotesRemaining = DECIMAL_FORMAT.format(antidoteCount);
		this.superSetsRemaining = DECIMAL_FORMAT.format(Math.min(supAtkCount, Math.min(supDefCount, supStrCount)));
	}

	private void abort(final String reason) {
		this.sendPrivateMessage("abort", this.master);
		this.exit(reason);
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
			return this.description;
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
			return this.lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return this.upperBoundingCoordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
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
					return this.antidote;
				case SUPER_ATTACK:
					return this.superAtk;
				case SUPER_DEFENSE:
					return this.superDef;
				case SUPER_STRENGTH:
					return this.superStr;
				default:
					return false;
			}
		}
	}
}
