import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
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
 * Author: Chomp
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
		MINIMUM_SHARKS = 10;

	private final Map<Integer, Integer> notableLoot = new TreeMap<>();

	private final int[] loot = new int[3];

	private String mule;
	private State state;
	private PathWalker pathWalker;
	private Instant startTime;

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
		this.combatStyle = CombatStyle.ATTACK;
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			this.mule = parameters.replace('_', ' ');
		}

		final State initialState;

		if (Area.BANK.contains(this.getX(), this.getY())) {
			initialState = State.BANK;
		} else if (Area.DRAGON.contains(this.getX(), this.getY())) {
			initialState = State.KILL;
		} else if (this.hasInventoryItem(Food.SHARK.getId())) {
			initialState = State.WALK_TO_DRAGON;
		} else {
			initialState = State.WALK_TO_BANK;
		}

		this.setFightMode(this.combatStyle.getIndex());
		this.eatAt = this.getBaseHits() - Food.SHARK.getHealAmount();
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
			case KILL:
				return this.kill();
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
			this.sharksEaten++;
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("flames")) {
			this.wieldShield = true;
		} else if (message.endsWith("successfully") || message.startsWith("declined", 17)) {
			this.resetTradeTimeouts();
		} else if (message.endsWith("poison potion") || message.endsWith("antidote potion")) {
			this.poisoned = false;
		} else if (message.endsWith("attack potion")) {
			this.superAtksUsed++;
		} else if (message.endsWith("defense potion")) {
			this.superDefsUsed++;
		} else if (message.endsWith("strength potion")) {
			this.superStrsUsed++;
		} else if (message.endsWith("left") ||
			message.startsWith("finished", 9) ||
			message.endsWith("Vial")) {
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.startsWith("poisioned", 23)) {
			this.poisoned = true;
		} else if (message.endsWith("area")) {
			this.idle = true;
			this.previousY = this.getY();
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void onPlayerDamaged(final java.lang.Object player) {
		if (!this.inCombat() || player != this.extension.getPlayer()) {
			return;
		}

		if (this.flee) {
			this.walkTo(this.playerX, this.playerY);
			return;
		}

		this.flee = this.getCurrentHits() < MINIMUM_HITS ||
			(this.poisoned && this.hasInventoryItem(Potion.ANTIDOTE.ids));

		final int combatStyle = this.getCombatStyle();

		if (this.getFightMode() != combatStyle) {
			this.setFightMode(combatStyle);
		}
	}

	@Override
	public void onNpcDamaged(final java.lang.Object npc) {
		if (!this.inCombat() ||
			this.getWaypointX(npc) != this.getWaypointX(this.extension.getPlayer()) ||
			this.getWaypointY(npc) != this.getWaypointY(this.extension.getPlayer())) {
			return;
		}

		if (this.flee) {
			this.walkTo(this.playerX, this.playerY);
			return;
		}

		if (this.getCombatStyle() != CombatStyle.DEFENSE.getIndex()) {
			this.setFightMode(CombatStyle.DEFENSE.getIndex());
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
		this.flee = false;
		this.wieldShield = false;
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

			for (final Map.Entry<Integer, Integer> entry : this.notableLoot.entrySet()) {
				this.drawString(String.format("@gr1@%s: @whi@%d", getItemNameId(entry.getKey()), entry.getValue()),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
			}
		}

		y = 0;

		this.drawString("@yel@Supplies Used",
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@or1@Strs: @whi@%s @cya@(@whi@%s p@cya@/@whi@hr@cya@)",
				this.superStrsUsed, getUnitsPerHour(this.superStrsUsed, secondsElapsed)),
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@ora@Atks: @whi@%s @cya@(@whi@%s p@cya@/@whi@hr@cya@)",
				this.superAtksUsed, getUnitsPerHour(this.superAtksUsed, secondsElapsed)),
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@or2@Defs: @whi@%s @cya@(@whi@%s p@cya@/@whi@hr@cya@)",
				this.superDefsUsed, getUnitsPerHour(this.superDefsUsed, secondsElapsed)),
			PAINT_OFFSET_X_ALT, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@or3@Sharks: @whi@%s @cya@(@whi@%s p@cya@/@whi@hr@cya@)",
				this.sharksEaten, getUnitsPerHour(this.sharksEaten, secondsElapsed)),
			PAINT_OFFSET_X_ALT, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
	}

	@Override
	public void onPrivateMessage(final String message, final String playerName, final boolean moderator,
								 final boolean administrator) {
		if (playerName.equalsIgnoreCase(this.mule) && message.equalsIgnoreCase("abort")) {
			this.mule = null;
			this.abort = true;
		}
	}

	private void setState(final State state) {
		this.state = state;

		switch (this.state) {
			case BANK:
				if (this.abort) {
					this.exit("Trip aborted.");
					return;
				}
				this.updateNotableLoot();
				break;
			case TRADE:
				this.updateNotableLoot();
				break;
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

			if (Equipment.isEquipment(itemId)) {
				final int invCount = this.getInventoryCount(itemId);

				if (invCount == 1) {
					continue;
				}

				if (System.currentTimeMillis() <= this.depositTimeout) {
					return 0;
				}

				this.deposit(itemId, invCount - 1);
				this.depositTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (!inArray(ITEM_IDS_LOOT, itemId) &&
				itemId != ITEM_ID_DRAGONSTONE_AMULET &&
				itemId != ITEM_ID_EMPTY_VIAL) {
				continue;
			}

			this.deposit(itemId, this.getInventoryCount(itemId));
			return SLEEP_ONE_TICK;
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
			if (this.hasInventoryItem(potion.ids)) {
				continue;
			}

			if (System.currentTimeMillis() <= potion.timeout) {
				return 0;
			}

			for (final int id : potion.ids) {
				if (!this.hasBankItem(id)) {
					continue;
				}

				this.withdraw(id, 1);
				potion.timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}
		}

		if (!this.isInventoryFull()) {
			if (System.currentTimeMillis() <= this.withdrawTimeout) {
				return 0;
			}

			if (!this.hasBankItem(Food.SHARK.getId())) {
				this.abort(String.format("Out of item: %s.", Food.SHARK));
			}

			this.withdraw(Food.SHARK.getId(), this.getInventoryEmptyCount());
			this.withdrawTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		this.setState(State.WALK_TO_DRAGON);
		return 0;
	}

	private int walkToDragon() {
		if (Area.DRAGON.contains(this.playerX, this.playerY)) {
			this.setState(State.KILL);
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
			if (this.distanceTo(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY()) <= 1) {
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
			if (equipment == Equipment.ANTI_SHIELD) {
				continue;
			}

			final int equipmentIndex = this.getInventoryIndex(equipment.id);

			if (equipmentIndex != -1 && !this.isItemEquipped(equipmentIndex)) {
				this.wearItem(equipmentIndex);
				return true;
			}
		}

		return false;
	}

	private int kill() {
		if (this.inCombat()) {
			if (this.wieldShield) {
				final int shieldIndex = this.getInventoryIndex(Equipment.SHIELD.id);

				if (!this.isItemEquipped(shieldIndex)) {
					this.wearItem(shieldIndex);
					return SLEEP_ONE_TICK;
				}

				this.wieldShield = false;
			}

			return 0;
		}

		if (this.flee) {
			this.flee = false;
		}

		final int antiShieldIndex = this.getInventoryIndex(Equipment.ANTI_SHIELD.id);

		if (antiShieldIndex == -1) {
			this.setState(State.TELEPORT_TO_BANK);
			return 0;
		}

		if (!this.isItemEquipped(antiShieldIndex)) {
			this.wearItem(antiShieldIndex);
			return SLEEP_ONE_TICK;
		}

		if (this.poisoned) {
			final int potionIndex = this.getInventoryIndex(Potion.ANTIDOTE.ids);

			if (potionIndex != -1) {
				return this.consume(potionIndex);
			}
		}

		if (this.getCurrentHits() <= this.eatAt) {
			final int sharkIndex = this.getInventoryIndex(Food.SHARK.getId());

			if (sharkIndex == -1) {
				this.setState(this.mule != null ? State.TRADE : State.TELEPORT_TO_BANK);
				return 0;
			}

			return this.consume(sharkIndex);
		}

		final int vialIndex = this.getInventoryIndex(ITEM_ID_EMPTY_VIAL);

		if (vialIndex != -1) {
			return this.dropVial(vialIndex);
		}

		this.updateLoot();

		if (this.loot[0] != -1 && (this.loot[0] != Food.SHARK.getId() || !this.isInventoryFull())) {
			if (!this.isInventoryFull() ||
				(inArray(ITEM_IDS_STACKABLE, this.loot[0]) && this.hasInventoryItem(this.loot[0]))) {
				this.pickupItem(this.loot[0], this.loot[1], this.loot[2]);
				return SLEEP_ONE_TICK;
			}

			final int sharkIndex = this.getInventoryIndex(Food.SHARK.getId());

			if (sharkIndex != -1) {
				return this.consume(sharkIndex);
			}
		}

		final int[] dragon = this.getAllNpcById(NPC_ID_KING_BLACK_DRAGON);

		if (dragon[0] == -1) {
			if (this.mule != null && this.isMissingSupplies()) {
				this.setState(State.TRADE);
				return 0;
			}

			if (this.playerX != COORDINATE_KBD_RESPAWN.getX() || this.playerY != COORDINATE_KBD_RESPAWN.getY()) {
				this.walkTo(COORDINATE_KBD_RESPAWN.getX(), COORDINATE_KBD_RESPAWN.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (this.isNpcInCombat(dragon[0])) {
			if (this.distanceTo(dragon[1], dragon[2]) > 1) {
				this.walkTo(dragon[1], dragon[2]);
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		final int npcHits = this.getCurrentHits(this.extension.getNpc(dragon[0]));

		if (npcHits == 0 || npcHits >= MINIMUM_HITS_KBD) {
			if (this.canDrinkSuperAttack()) {
				final int potionIndex = this.getInventoryIndex(Potion.SUPER_ATTACK.ids);

				if (potionIndex != -1) {
					return this.consume(potionIndex);
				}
			}

			if (this.canDrinkSuperDefense()) {
				final int potionIndex = this.getInventoryIndex(Potion.SUPER_DEFENSE.ids);

				if (potionIndex != -1) {
					return this.consume(potionIndex);
				}
			}

			if (this.canDrinkSuperStrength()) {
				final int potionIndex = this.getInventoryIndex(Potion.SUPER_STRENGTH.ids);

				if (potionIndex != -1) {
					return this.consume(potionIndex);
				}
			}
		}

		this.attackNpc(dragon[0]);
		return SLEEP_ONE_TICK;
	}

	private boolean isMissingSupplies() {
		if (this.getInventoryCount(Food.SHARK.getId()) < MINIMUM_SHARKS) {
			return true;
		}

		if (this.canDrinkSuperAttack() && !this.hasInventoryItem(Potion.SUPER_ATTACK.ids)) {
			return true;
		}

		if (this.canDrinkSuperDefense() && !this.hasInventoryItem(Potion.SUPER_DEFENSE.ids)) {
			return true;
		}

		if (this.canDrinkSuperStrength() && !this.hasInventoryItem(Potion.SUPER_STRENGTH.ids)) {
			return true;
		}

		return this.poisoned;
	}

	private int trade() {
		if (this.inCombat()) {
			this.walkTo(this.playerX, this.playerY);
			return SLEEP_ONE_TICK;
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

			if (Area.TRADE.contains(this.playerX, this.playerY) &&
				(this.getCurrentHits() < MINIMUM_HITS || this.getPlayerByName(this.mule)[0] == -1)) {
				final int[] kbd = this.getAllNpcById(NPC_ID_KING_BLACK_DRAGON);

				if (kbd[0] != -1 && Area.TRADE.contains(kbd[1], kbd[2])) {
					this.setState(State.TELEPORT_TO_BANK);
					return 0;
				}
			}
		}

		final int vialIndex = this.getInventoryIndex(ITEM_ID_EMPTY_VIAL);

		if (vialIndex != -1) {
			return this.dropVial(vialIndex);
		}

		if (!Area.TRADE.contains(this.playerX, this.playerY) &&
			this.getAllNpcById(NPC_ID_KING_BLACK_DRAGON)[0] == -1) {
			this.updateLoot();

			if (this.loot[0] != -1 && (this.loot[0] != Food.SHARK.getId() || !this.isInventoryFull())) {
				if (!this.isInventoryFull() ||
					(inArray(ITEM_IDS_STACKABLE, this.loot[0]) && this.hasInventoryItem(this.loot[0]))) {
					this.pickupItem(this.loot[0], this.loot[1], this.loot[2]);
					return SLEEP_ONE_TICK;
				}

				final int sharkIndex = this.getInventoryIndex(Food.SHARK.getId());

				if (sharkIndex != -1) {
					return this.consume(sharkIndex);
				}
			}
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
			if (this.playerY == this.previousY) {
				this.walkTo(this.playerX, this.playerY - 1);
				return SLEEP_ONE_TICK;
			}

			this.idle = false;
		}

		if (this.playerX != COORDINATE_TRADE_SPOT.getX() || this.playerY != COORDINATE_TRADE_SPOT.getY()) {
			this.walkTo(COORDINATE_TRADE_SPOT.getX(), COORDINATE_TRADE_SPOT.getY());
			return SLEEP_ONE_TICK;
		}

		final int[] mule = this.getPlayerByName(this.mule);

		if (this.isMissingSupplies() || (!this.isInventoryFull() && mule[0] != -1)) {
			if (mule[0] == -1 || System.currentTimeMillis() <= this.tradeRequestTimeout) {
				return 0;
			}

			this.sendTradeRequest(this.getPlayerPID(mule[0]));
			this.tradeRequestTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		this.setState(State.KILL);
		return 0;
	}

	private int handleAcceptTrade() {
		final int totalTradeSize = Math.min(this.getInventoryLootCount() - this.getEquipmentLootCount(),
			MAX_TRADE_SIZE);

		if (this.getRemoteTradeItemCount() == 0 &&
			System.currentTimeMillis() > this.privateMessageTimeout) {
			this.sendPrivateMessage(this.createSupplyRequestMessage(totalTradeSize), this.mule);
			this.privateMessageTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (this.extension.getLocalTradeItemCount() == totalTradeSize) {
			if (!this.hasLocalAcceptedTrade()) {
				this.acceptTrade();
			}

			return 0;
		}

		for (int index = 0; index < this.getInventoryCount(); index++) {
			final int itemId = this.getInventoryId(index);

			if (itemId == Food.SHARK.getId()) {
				continue;
			}

			if (Equipment.isEquipment(itemId)) {
				final int invCount = this.getInventoryCount(itemId);

				if (invCount == 1) {
					continue;
				}

				final int tradeCount = this.getTradeItemIdCount(itemId);

				if (tradeCount == invCount - 1) {
					continue;
				}

				if (tradeCount == invCount) {
					this.removeTradeItemId(itemId, 1);
				} else {
					this.offerItemTrade(index, 1);
				}

				continue;
			}

			if (inArray(Potion.ANTIDOTE.ids, itemId)) {
				if (!this.poisoned) {
					this.offerItemTrade(index, 1);
				}

				continue;
			}

			if (inArray(ITEM_IDS_LOOT, itemId)) {
				final int tradeCount = this.getTradeItemIdCount(itemId);
				final int invCount = this.getInventoryCount(itemId);

				if (tradeCount == invCount) {
					continue;
				}

				this.offerItemTrade(index, invCount);
			}
		}

		return SLEEP_ONE_TICK;
	}

	private int getInventoryLootCount() {
		int count = 0;

		for (int index = 0; index < this.getInventoryCount(); index++) {
			final int itemId = this.getInventoryId(index);

			if (itemId == Food.SHARK.getId()) {
				continue;
			}

			if (inArray(ITEM_IDS_LOOT, itemId) ||
				(inArray(Potion.ANTIDOTE.ids, itemId) && !this.poisoned)) {
				count++;
			}
		}

		return count;
	}

	private int getEquipmentLootCount() {
		int count = 0;

		for (final Equipment equipment : Equipment.VALUES) {
			if (inArray(ITEM_IDS_LOOT, equipment.id) &&
				this.hasInventoryItem(equipment.id)) {
				count++;
			}
		}

		return count;
	}

	private String createSupplyRequestMessage(final int tradeSize) {
		final int maxTradeSpace = Math.min(tradeSize + this.getInventoryEmptyCount(), MAX_TRADE_SIZE);

		final int antidote = this.poisoned && !this.hasInventoryItem(Potion.ANTIDOTE.ids) ? 1 : 0;
		final int superAtk = !this.hasInventoryItem(Potion.SUPER_ATTACK.ids) ? 1 : 0;
		final int superDef = !this.hasInventoryItem(Potion.SUPER_DEFENSE.ids) ? 1 : 0;
		final int superStr = !this.hasInventoryItem(Potion.SUPER_STRENGTH.ids) ? 1 : 0;

		final int totalPotions = antidote + superAtk + superDef + superStr;

		final int sharks = maxTradeSpace - totalPotions;

		return String.format("%d,%d,%d,%d,%d", antidote, superAtk, superDef, superStr, sharks);
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

	private boolean canDrinkSuperAttack() {
		return this.getCurrentLevel(Skill.ATTACK.getIndex()) <= MINIMUM_ATK;
	}

	private boolean canDrinkSuperDefense() {
		return this.getCurrentLevel(Skill.DEFENSE.getIndex()) <= MINIMUM_DEF;
	}

	private boolean canDrinkSuperStrength() {
		return this.getCurrentLevel(Skill.STRENGTH.getIndex()) <= MINIMUM_STR;
	}

	private void updateLoot() {
		this.loot[0] = -1;

		int groundItemId;

		for (int index = 0; index < this.getGroundItemCount(); index++) {
			groundItemId = this.getGroundItemId(index);

			if (!inArray(ITEM_IDS_LOOT, groundItemId)) {
				continue;
			}

			this.loot[0] = groundItemId;
			this.loot[1] = this.getItemX(index);
			this.loot[2] = this.getItemY(index);
			break;
		}
	}

	private void updateNotableLoot() {
		for (int index = 0; index < this.getInventoryCount(); index++) {
			final int itemId = this.getInventoryId(index);

			if (!inArray(ITEM_IDS_LOOT_NOTABLE, itemId)) {
				continue;
			}

			this.notableLoot.merge(itemId, this.getInventoryStack(index), Integer::sum);
		}

		for (final Equipment equipment : Equipment.VALUES) {
			if (inArray(ITEM_IDS_LOOT_NOTABLE, equipment.id) && this.getInventoryCount(equipment.id) > 1) {
				this.notableLoot.merge(equipment.id, -1, Integer::sum);
			}
		}
	}

	private int getCombatStyle() {
		return inArray(COMBAT_STYLE_BREAKPOINTS, this.getCurrentLevel(Skill.ATTACK.getIndex())) ?
			CombatStyle.STRENGTH.getIndex() : CombatStyle.ATTACK.getIndex();
	}

	private void resetTradeTimeouts() {
		this.tradeRequestTimeout = 0L;
		this.privateMessageTimeout = 0L;
	}

	private int consume(final int inventoryIndex) {
		if (System.currentTimeMillis() <= this.actionTimeout) {
			return 0;
		}

		this.useItem(inventoryIndex);
		this.actionTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private int dropVial(final int inventoryIndex) {
		if (System.currentTimeMillis() <= this.actionTimeout) {
			return 0;
		}

		this.dropItem(inventoryIndex);
		this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private void abort(final String reason) {
		if (this.mule != null) {
			this.sendPrivateMessage("abort", this.mule);
		}

		this.exit(reason);
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
