import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trains at the Pirate Hut west of the Mage Arena.
 * <p>
 * Required:
 * Start script at the Pirate Hut.
 * Inventory: lockpick, sleeping bag, weapon, body, legs/shield, str ammy, cape of legends.
 * Bank: lockpicks, sleeping bags, str ammies, capes of legends.
 * Amulet and cape are optional.
 * <p>
 * Optional Parameter:
 * -f,--fightmode <controlled|attack|strength|defense> (default strength)
 * <p>
 * Notes:
 * PathWalker is used to return to the Pirate Hut if pked.
 * Protect item is enabled if hits drop below 35.
 * Eats the half a red berry pie spawn to heal.
 * <p>
 * Author: Chomp
 */
public class AA_PirateHut extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_PIRATE_HUT = new Coordinate(291, 141);
	private static final Coordinate COORDINATE_LOAD_DRAYNOR_BANK = new Coordinate(176, 642);
	private static final Coordinate COORDINATE_HALF_A_REDBERRY_PIE = new Coordinate(265, 102);
	private static final Coordinate COORDINATE_WALKBACK_DRAYNOR = new Coordinate(214, 632);
	private static final Coordinate COORDINATE_WALKBACK_ICE_PLATEAU = new Coordinate(330, 160);

	private static final Pattern PATTERN_PROJECTILE_SHOT = Pattern.compile("Warning! (.+) is shooting at you!");

	private static final int[] ITEM_IDS_LOOT = new int[]{1277, 1092};

	private static final int NPC_ID_PIRATE = 137;
	private static final int NPC_XP_PIRATE = 76;

	private static final int ITEM_ID_LOCKPICK = 714;
	private static final int ITEM_ID_LEGENDS_CAPE = 1288;
	private static final int ITEM_ID_STEEL_GAUNTLETS = 698;
	private static final int ITEM_ID_RUBY_AMULET_OF_STRENGTH = 316;
	private static final int ITEM_ID_HALF_A_REDBERRY_PIE = 262;
	private static final int ITEM_ID_PIE_DISH = 251;

	private static final int PRAYER_INDEX_PROTECT_ITEMS = 8;

	private static final int SKILL_INDEX_HITS = 3;
	private static final int SKILL_INDEX_PRAYER = 5;

	private static final int FOOD_HEAL_AMOUNT = 3;
	private static final int MINIMUM_HITS_PROTECT_ITEM = 35;
	private static final int MAXIMUM_FATIGUE = 99;

	private final Set<String> pkers = new HashSet<>(3);
	private final Map<Integer, Spawn> spawnMap = new HashMap<>();

	private Coordinate nextRespawn;
	private String attackers = "";
	private State state;
	private Instant startTime;
	private PathWalker pathWalker;

	private double initialCombatXp;

	private long actionTimeout;

	private int playerX;
	private int playerY;

	private int eatAt;

	private int amuletCount;
	private int attackedCount;
	private int deathCount;

	private boolean attacked;
	private boolean died;
	private boolean collectPies;

	public AA_PirateHut(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-f":
					case "--fightmode":
						this.combatStyle = CombatStyle.valueOf(args[++i].toUpperCase());
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		this.setFightMode(this.combatStyle.getIndex());
		this.eatAt = this.getLevel(SKILL_INDEX_HITS) - FOOD_HEAL_AMOUNT;
		this.initialCombatXp = this.getTotalCombatXp();
		this.setState(State.KILL);
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		if (this.died) {
			if (this.isDead()) {
				return 0;
			}

			this.died = false;
		}

		this.playerX = this.getX();
		this.playerY = this.getY();

		switch (this.state) {
			case KILL:
				return this.kill();
			case BANK:
				return this.bank();
			case WALKBACK:
				return this.walkback();
			default:
				return this.exit("Invalid script state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("Warning")) {
			if (this.attacked) {
				return;
			}

			final Matcher matcher = PATTERN_PROJECTILE_SHOT.matcher(message);

			if (!matcher.matches()) {
				return;
			}

			final String rsn = matcher.group(1);

			this.setAttacked(rsn);
		} else if (message.startsWith("eat", 4) ||
			message.startsWith("fail", 4) ||
			message.startsWith("manage", 4) ||
			message.endsWith("it") ||
			message.endsWith("web") ||
			message.endsWith("lever") ||
			message.endsWith("shut") ||
			message.endsWith("open") ||
			message.endsWith("door")) {
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void onPlayerDamaged(final java.lang.Object player) {
		if (!this.inCombat() || player != this.extension.getPlayer()) {
			return;
		}

		if (this.extension.getPlayerCount() > 1 && !this.attacked) {
			final String pkerName = this.getPkerName();

			if (pkerName != null) {
				this.setAttacked(pkerName);
			}
		}

		if (this.getCurrentLevel(SKILL_INDEX_HITS) <= MINIMUM_HITS_PROTECT_ITEM &&
			this.getCurrentLevel(SKILL_INDEX_PRAYER) > 0 &&
			!this.isPrayerEnabled(PRAYER_INDEX_PROTECT_ITEMS)) {
			this.enablePrayer(PRAYER_INDEX_PROTECT_ITEMS);
		}
	}

	@Override
	public void onDeath() {
		System.out.printf("[%s] Oh dear! You are dead...%n", this);
		this.attacked = false;
		this.deathCount++;
		this.died = true;
		this.resetSpawns();
		this.setState(State.BANK);
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Pirate Hut", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Pid: @whi@%d", this.extension.getMobServerIndex(this.extension.getPlayer())),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final double xpGained = this.getTotalCombatXp() - this.initialCombatXp;

		this.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), getUnitsPerHour(xpGained, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final int kills = (int) xpGained / NPC_XP_PIRATE;

		this.drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, getUnitsPerHour(kills, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.nextRespawn != null) {
			this.drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)",
					this.nextRespawn.getX(), this.nextRespawn.getY()),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}

		if (!this.attackers.isEmpty()) {
			this.drawString(String.format("@yel@Attacked by: @whi@%s", this.attackers),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (this.extension.getNpcId(npc) != NPC_ID_PIRATE) {
			return;
		}

		final int npcX = this.extension.getMobLocalX(npc) + this.extension.getAreaX();
		final int npcY = this.extension.getMobLocalY(npc) + this.extension.getAreaY();

		final int serverIndex = this.extension.getMobServerIndex(npc);

		final Spawn spawn = this.spawnMap.get(serverIndex);

		if (spawn != null) {
			spawn.getCoordinate().set(npcX, npcY);
			spawn.setTimestamp(System.currentTimeMillis());
		} else {
			this.spawnMap.put(serverIndex, new Spawn(new Coordinate(npcX, npcY), System.currentTimeMillis()));
		}

		this.nextRespawn = this.spawnMap.isEmpty() ? null : this.spawnMap.values().stream().sorted().findFirst().get().getCoordinate();
	}

	private int kill() {
		if (Area.PIRATE_HUT.contains(this.playerX, this.playerY)) {
			if (this.inCombat()) {
				return 0;
			}

			if (this.getCurrentLevel(SKILL_INDEX_HITS) <= this.eatAt) {
				final int pieIndex = this.getInventoryIndex(ITEM_ID_HALF_A_REDBERRY_PIE);

				if (pieIndex != -1) {
					if (System.currentTimeMillis() <= this.actionTimeout) {
						return 0;
					}

					this.useItem(pieIndex);
					this.actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
					return 0;
				}

				if (!this.collectPies) {
					this.collectPies = true;
				}
			}

			final int pieDishIndex = this.getInventoryIndex(ITEM_ID_PIE_DISH);

			if (pieDishIndex != -1) {
				this.dropItem(pieDishIndex);
				return SLEEP_ONE_TICK;
			}

			final int[] loot = this.getItemById(ITEM_IDS_LOOT);

			if (loot[0] != -1) {
				this.pickupItem(loot[0], loot[1], loot[2]);
				return SLEEP_ONE_TICK;
			}

			if (this.getFatigue() >= MAXIMUM_FATIGUE) {
				if (System.currentTimeMillis() <= this.actionTimeout) {
					return 0;
				}

				this.atWallObject(Object.PIRATE_HUT_DOOR.coordinate.getX(), Object.PIRATE_HUT_DOOR.coordinate.getY());
				this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (this.collectPies) {
				if (this.getInventoryCount() == MAX_INV_SIZE) {
					this.collectPies = false;
				} else if (this.isItemAt(ITEM_ID_HALF_A_REDBERRY_PIE,
					COORDINATE_HALF_A_REDBERRY_PIE.getX(), COORDINATE_HALF_A_REDBERRY_PIE.getY())) {
					this.pickupItem(ITEM_ID_HALF_A_REDBERRY_PIE,
						COORDINATE_HALF_A_REDBERRY_PIE.getX(), COORDINATE_HALF_A_REDBERRY_PIE.getY());
					return SLEEP_ONE_TICK;
				}
			}

			final int[] pirate = this.getNpcById(NPC_ID_PIRATE);

			if (pirate[0] != -1) {
				this.attackNpc(pirate[0]);
				return SLEEP_ONE_TICK;
			}

			if (this.nextRespawn != null &&
				(this.playerX != this.nextRespawn.getX() || this.playerY != this.nextRespawn.getY())) {
				this.walkTo(this.nextRespawn.getX(), this.nextRespawn.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (this.playerX <= COORDINATE_LOAD_PIRATE_HUT.getX()) {
			if (this.playerX == Object.PIRATE_HUT_DOOR.coordinate.getX() &&
				this.playerY == Object.PIRATE_HUT_DOOR.coordinate.getY()) {
				if (this.getFatigue() >= MAXIMUM_FATIGUE) {
					return this.sleep();
				}

				if (System.currentTimeMillis() <= this.actionTimeout) {
					return 0;
				}

				this.atWallObject2(Object.PIRATE_HUT_DOOR.coordinate.getX(), Object.PIRATE_HUT_DOOR.coordinate.getY());
				this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			this.walkTo(Object.PIRATE_HUT_DOOR.coordinate.getX(), Object.PIRATE_HUT_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.playerY < Object.GATE.coordinate.getY()) {
			this.walkTo(COORDINATE_LOAD_PIRATE_HUT.getX(), COORDINATE_LOAD_PIRATE_HUT.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.distanceTo(Object.GATE.coordinate.getX(), Object.GATE.coordinate.getY()) <= 1) {
			if (this.inCombat()) {
				this.walkTo(this.playerX, this.playerY);
				return SLEEP_ONE_TICK;
			}

			this.atObject(Object.GATE.coordinate.getX(), Object.GATE.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(Object.GATE.coordinate.getX(), Object.GATE.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (!this.isBanking()) {
				return this.openBank();
			}

			if (System.currentTimeMillis() <= this.actionTimeout) {
				return 0;
			}

			final int lootIndex = this.getInventoryIndex(ITEM_IDS_LOOT);

			if (lootIndex != -1) {
				this.deposit(lootIndex, 1);
				return SLEEP_ONE_TICK;
			}

			if (!this.hasInventoryItem(ITEM_ID_RUBY_AMULET_OF_STRENGTH) &&
				this.hasBankItem(ITEM_ID_RUBY_AMULET_OF_STRENGTH)) {
				this.amuletCount++;
				this.withdraw(ITEM_ID_RUBY_AMULET_OF_STRENGTH, 1);
				this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (!this.hasInventoryItem(ITEM_ID_LEGENDS_CAPE) &&
				this.hasBankItem(ITEM_ID_LEGENDS_CAPE)) {
				this.withdraw(ITEM_ID_LEGENDS_CAPE, 1);
				this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (!this.hasInventoryItem(ITEM_ID_LOCKPICK)) {
				if (!this.hasBankItem(ITEM_ID_LOCKPICK)) {
					return this.exit("Out of lockpicks.");
				}

				this.withdraw(ITEM_ID_LOCKPICK, 1);
				this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
				if (!this.hasBankItem(ITEM_ID_SLEEPING_BAG)) {
					return this.exit("Out of sleeping bags.");
				}

				this.withdraw(ITEM_ID_SLEEPING_BAG, 1);
				this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			this.closeBank();
			this.setState(State.WALKBACK);
			return 0;
		}

		if (this.playerX >= COORDINATE_LOAD_DRAYNOR_BANK.getX()) {
			if (this.distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= 1) {
				if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY())
					== Object.BANK_DOORS.id) {
					this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.isPrayerEnabled(PRAYER_INDEX_PROTECT_ITEMS)) {
			this.disablePrayer(PRAYER_INDEX_PROTECT_ITEMS);
			return SLEEP_ONE_TICK;
		}

		this.walkTo(COORDINATE_LOAD_DRAYNOR_BANK.getX(), COORDINATE_LOAD_DRAYNOR_BANK.getY());
		return SLEEP_ONE_TICK;
	}

	private int walkback() {
		if (this.pathWalker != null) {
			if (this.pathWalker.walkPath()) {
				return 0;
			}

			this.pathWalker = null;
			this.setState(State.KILL);
			return 0;
		}

		if (this.playerX == COORDINATE_WALKBACK_DRAYNOR.getX() && this.playerY == COORDINATE_WALKBACK_DRAYNOR.getY()) {
			this.pathWalker = new PathWalker(this.extension);
			this.pathWalker.init(null);

			final PathWalker.Path path = this.pathWalker.calcPath(COORDINATE_WALKBACK_DRAYNOR.getX(),
				COORDINATE_WALKBACK_DRAYNOR.getY(), COORDINATE_WALKBACK_ICE_PLATEAU.getX(),
				COORDINATE_WALKBACK_ICE_PLATEAU.getY());

			if (path == null) {
				return this.exit("Failed to calculate path from Draynor to Ice Plateau.");
			}

			this.pathWalker.setPath(path);
			return 0;
		}

		if (Area.BANK.contains(this.playerX, this.playerY)) {
			final int legendsCapeIndex = this.getInventoryIndex(ITEM_ID_LEGENDS_CAPE);

			if (legendsCapeIndex != -1 && !this.isItemEquipped(legendsCapeIndex)) {
				this.wearItem(legendsCapeIndex);
				return SLEEP_ONE_TICK;
			}

			final int steelGauntletsIndex = this.getInventoryIndex(ITEM_ID_STEEL_GAUNTLETS);

			if (steelGauntletsIndex != -1 && !this.isItemEquipped(steelGauntletsIndex)) {
				this.wearItem(steelGauntletsIndex);
				return SLEEP_ONE_TICK;
			}

			final int amuletIndex = this.getInventoryIndex(ITEM_ID_RUBY_AMULET_OF_STRENGTH);

			if (amuletIndex != -1 && !this.isItemEquipped(amuletIndex)) {
				this.wearItem(amuletIndex);
				return SLEEP_ONE_TICK;
			}

			if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) ==
				Object.BANK_DOORS.id) {
				this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}
		}

		this.walkTo(COORDINATE_WALKBACK_DRAYNOR.getX(), COORDINATE_WALKBACK_DRAYNOR.getY());
		return SLEEP_ONE_TICK;
	}

	private void resetSpawns() {
		this.spawnMap.clear();
		this.nextRespawn = null;
	}

	private String getPkerName() {
		for (int index = 0; index < this.extension.getPlayerCount(); index++) {
			final java.lang.Object player = this.extension.getPlayer(index);

			if (player == this.extension.getPlayer() ||
				!this.extension.isMobInCombat(player) ||
				this.getPlayerX(index) != this.playerX ||
				this.getPlayerY(index) != this.playerY) {
				continue;
			}

			return this.getPlayerName(index);
		}

		return null;
	}

	private void setAttacked(final String rsn) {
		System.out.printf("[%s] Attacked by: %s%n", this, rsn);

		this.attacked = true;
		this.attackedCount++;

		if (!this.pkers.contains(rsn)) {
			this.pkers.add(rsn);
			this.attackers += rsn + " ";
		}
	}

	private void setState(final State state) {
		this.state = state;
	}

	private enum State {
		KILL,
		BANK,
		WALKBACK
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(216, 634), new Coordinate(223, 638)),
		PIRATE_HUT(new Coordinate(263, 100), new Coordinate(269, 110));

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
		BANK_DOORS(64, new Coordinate(219, 633)),
		PIRATE_HUT_DOOR(99, new Coordinate(270, 104)),
		GATE(346, new Coordinate(331, 142));

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
}
