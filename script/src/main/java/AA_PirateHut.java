import java.util.*;
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
 * <controlled|attack|strength|defense> (default strength)
 * <p>
 * Notes:
 * PathWalker is used to return to the Pirate Hut if pked.
 * Protect item is enabled if hits drop below 35.
 * Eats the half a red berry pie spawn to heal.
 * <p>
 *
 * @Author Chomp
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

	private static final int FOOD_HEAL_AMOUNT = 3;
	private static final int MINIMUM_HITS_PROTECT_ITEM = 35;
	private static final int MAXIMUM_FATIGUE = 99;

	private final Set<String> pkers = new HashSet<>(3);
	private final Map<Integer, Spawn> spawnMap = new HashMap<>();

	private Coordinate nextRespawn;
	private String attackers = "";
	private State state;
	private PathWalker pathWalker;

	private double initialCombatXp;

	private long startTime;
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
		if (!parameters.isEmpty()) combatStyle = CombatStyle.valueOf(parameters.toUpperCase());
		setCombatStyle(combatStyle.getIndex());
		eatAt = getBaseHits() - FOOD_HEAL_AMOUNT;
		initialCombatXp = getTotalCombatXp();
		setState(State.KILL);
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (died) {
			if (isDead()) return 0;
			died = false;
		}

		playerX = getX();
		playerY = getY();

		if (bot.getCombatStyle() != combatStyle.getIndex()) setCombatStyle(combatStyle.getIndex());

		switch (state) {
			case KILL:
				return kill();
			case BANK:
				return bank();
			case WALKBACK:
				return walkback();
			default:
				return exit("Invalid script state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("Warning")) {
			if (attacked) return;
			final Matcher matcher = PATTERN_PROJECTILE_SHOT.matcher(message);
			if (!matcher.matches()) return;
			final String rsn = matcher.group(1);
			setAttacked(rsn);
		} else if (message.startsWith("eat", 4) ||
			message.startsWith("fail", 4) ||
			message.startsWith("manage", 4) ||
			message.endsWith("it") ||
			message.endsWith("web") ||
			message.endsWith("lever") ||
			message.endsWith("shut") ||
			message.endsWith("open") ||
			message.endsWith("door")) {
			actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void onDeath() {
		System.out.printf("[%s] Oh dear! You are dead...%n", this);
		attacked = false;
		deathCount++;
		died = true;
		resetSpawns();
		setState(State.BANK);
	}

	private void resetSpawns() {
		spawnMap.clear();
		nextRespawn = null;
	}

	private void setAttacked(final String rsn) {
		System.out.printf("[%s] Attacked by: %s%n", this, rsn);

		attacked = true;
		attackedCount++;

		if (!pkers.contains(rsn)) {
			pkers.add(rsn);
			attackers += rsn + " ";
		}
	}

	private int kill() {
		if (Area.PIRATE_HUT.contains(playerX, playerY)) {
			if (inCombat()) return 0;

			if (getCurrentHits() <= eatAt) {
				final int pieIndex = getInventoryIndex(ITEM_ID_HALF_A_REDBERRY_PIE);

				if (pieIndex != -1) {
					if (System.currentTimeMillis() <= actionTimeout) return 0;
					useItem(pieIndex);
					actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
					return 0;
				}

				if (!collectPies) collectPies = true;
			}

			final int pieDishIndex = getInventoryIndex(ITEM_ID_PIE_DISH);

			if (pieDishIndex != -1) {
				dropItem(pieDishIndex);
				return SLEEP_ONE_TICK;
			}

			final int[] loot = getItemById(ITEM_IDS_LOOT);

			if (loot[0] != -1) {
				pickupItem(loot[0], loot[1], loot[2]);
				return SLEEP_ONE_TICK;
			}

			if (getFatigue() >= MAXIMUM_FATIGUE) {
				if (System.currentTimeMillis() <= actionTimeout) return 0;
				final Coordinate door = Object.PIRATE_HUT_DOOR.getCoordinate();
				atWallObject(door.getX(), door.getY());
				actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (collectPies) {
				if (getInventoryCount() == MAX_INV_SIZE) {
					collectPies = false;
				} else if (isItemAt(ITEM_ID_HALF_A_REDBERRY_PIE,
					COORDINATE_HALF_A_REDBERRY_PIE.getX(), COORDINATE_HALF_A_REDBERRY_PIE.getY())) {
					pickupItem(ITEM_ID_HALF_A_REDBERRY_PIE,
						COORDINATE_HALF_A_REDBERRY_PIE.getX(), COORDINATE_HALF_A_REDBERRY_PIE.getY());
					return SLEEP_ONE_TICK;
				}
			}

			final int[] pirate = getNpcById(NPC_ID_PIRATE);

			if (pirate[0] != -1) {
				attackNpc(pirate[0]);
				return SLEEP_ONE_TICK;
			}

			if (nextRespawn != null && (playerX != nextRespawn.getX() || playerY != nextRespawn.getY())) {
				walkTo(nextRespawn.getX(), nextRespawn.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (playerX <= COORDINATE_LOAD_PIRATE_HUT.getX()) {
			final Coordinate door = Object.PIRATE_HUT_DOOR.getCoordinate();

			if (playerX == door.getX() && playerY == door.getY()) {
				if (getFatigue() >= MAXIMUM_FATIGUE) return sleep();
				if (System.currentTimeMillis() <= actionTimeout) return 0;
				atWallObject2(door.getX(), door.getY());
				actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			walkTo(door.getX(), door.getY());
			return SLEEP_ONE_TICK;
		}

		final Coordinate gate = Object.GATE.getCoordinate();

		if (playerY < gate.getY()) {
			walkTo(COORDINATE_LOAD_PIRATE_HUT.getX(), COORDINATE_LOAD_PIRATE_HUT.getY());
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(gate.getX(), gate.getY()) <= 1) {
			if (inCombat()) {
				walkTo(playerX, playerY);
				return SLEEP_ONE_TICK;
			}

			atObject(gate.getX(), gate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(gate.getX(), gate.getY());
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (!isBanking()) return openBank();

			if (System.currentTimeMillis() <= actionTimeout) return 0;

			final int lootIndex = getInventoryIndex(ITEM_IDS_LOOT);

			if (lootIndex != -1) {
				deposit(lootIndex, 1);
				return SLEEP_ONE_TICK;
			}

			if (!hasInventoryItem(ITEM_ID_RUBY_AMULET_OF_STRENGTH) &&
				hasBankItem(ITEM_ID_RUBY_AMULET_OF_STRENGTH)) {
				amuletCount++;
				withdraw(ITEM_ID_RUBY_AMULET_OF_STRENGTH, 1);
				actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (!hasInventoryItem(ITEM_ID_LEGENDS_CAPE) &&
				hasBankItem(ITEM_ID_LEGENDS_CAPE)) {
				withdraw(ITEM_ID_LEGENDS_CAPE, 1);
				actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (!hasInventoryItem(ITEM_ID_LOCKPICK)) {
				if (!hasBankItem(ITEM_ID_LOCKPICK)) return exit("Out of lockpicks.");
				withdraw(ITEM_ID_LOCKPICK, 1);
				actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
				if (!hasBankItem(ITEM_ID_SLEEPING_BAG)) return exit("Out of sleeping bags.");
				withdraw(ITEM_ID_SLEEPING_BAG, 1);
				actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			closeBank();
			setState(State.WALKBACK);
			return 0;
		}

		if (playerX >= COORDINATE_LOAD_DRAYNOR_BANK.getX()) {
			final Coordinate doors = Object.BANK_DOORS.getCoordinate();

			if (distanceTo(doors.getX(), doors.getY()) <= 1) {
				if (getObjectIdFromCoords(doors.getX(), doors.getY()) == Object.BANK_DOORS.id) {
					atObject(doors.getX(), doors.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(doors.getX(), doors.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			walkTo(doors.getX(), doors.getY());
			return SLEEP_ONE_TICK;
		}

		if (isPrayerEnabled(PRAYER_INDEX_PROTECT_ITEMS)) {
			disablePrayer(PRAYER_INDEX_PROTECT_ITEMS);
			return SLEEP_ONE_TICK;
		}

		walkTo(COORDINATE_LOAD_DRAYNOR_BANK.getX(), COORDINATE_LOAD_DRAYNOR_BANK.getY());
		return SLEEP_ONE_TICK;
	}

	private int walkback() {
		if (pathWalker != null) {
			if (pathWalker.walkPath()) return 0;
			pathWalker = null;
			setState(State.KILL);
			return 0;
		}

		if (playerX == COORDINATE_WALKBACK_DRAYNOR.getX() && playerY == COORDINATE_WALKBACK_DRAYNOR.getY()) {
			pathWalker = new PathWalker(bot);
			pathWalker.init(null);

			final PathWalker.Path path = pathWalker.calcPath(COORDINATE_WALKBACK_DRAYNOR.getX(),
				COORDINATE_WALKBACK_DRAYNOR.getY(), COORDINATE_WALKBACK_ICE_PLATEAU.getX(),
				COORDINATE_WALKBACK_ICE_PLATEAU.getY());

			if (path == null) return exit("Failed to calculate path from Draynor to Ice Plateau.");

			pathWalker.setPath(path);
			return 0;
		}

		if (Area.BANK.contains(playerX, playerY)) {
			final int legendsCapeIndex = getInventoryIndex(ITEM_ID_LEGENDS_CAPE);

			if (legendsCapeIndex != -1 && !isItemEquipped(legendsCapeIndex)) {
				wearItem(legendsCapeIndex);
				return SLEEP_ONE_TICK;
			}

			final int steelGauntletsIndex = getInventoryIndex(ITEM_ID_STEEL_GAUNTLETS);

			if (steelGauntletsIndex != -1 && !isItemEquipped(steelGauntletsIndex)) {
				wearItem(steelGauntletsIndex);
				return SLEEP_ONE_TICK;
			}

			final int amuletIndex = getInventoryIndex(ITEM_ID_RUBY_AMULET_OF_STRENGTH);

			if (amuletIndex != -1 && !isItemEquipped(amuletIndex)) {
				wearItem(amuletIndex);
				return SLEEP_ONE_TICK;
			}

			final Coordinate doors = Object.BANK_DOORS.getCoordinate();

			if (getObjectIdFromCoords(doors.getX(), doors.getY()) == Object.BANK_DOORS.id) {
				atObject(doors.getX(), doors.getY());
				return SLEEP_ONE_SECOND;
			}
		}

		walkTo(COORDINATE_WALKBACK_DRAYNOR.getX(), COORDINATE_WALKBACK_DRAYNOR.getY());
		return SLEEP_ONE_TICK;
	}

	private void setState(final State state) {
		this.state = state;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Pirate Hut", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		final int kills = (int) xpGained / NPC_XP_PIRATE;

		drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, toUnitsPerHour(kills, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (attackers.isEmpty()) return;

		drawString(String.format("@yel@Attacked by: @whi@%s", attackers),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);
	}

	@Override
	public void onPlayerDamaged(final java.lang.Object player) {
		if (!inCombat() || player != bot.getPlayer()) return;

		if (bot.getPlayerCount() > 1 && !attacked) {
			final String pkerName = getPkerName();
			if (pkerName != null) setAttacked(pkerName);
		}

		if (getCurrentHits() <= MINIMUM_HITS_PROTECT_ITEM &&
			getCurrentLevel(Skill.PRAYER.getIndex()) > 0 &&
			!isPrayerEnabled(PRAYER_INDEX_PROTECT_ITEMS)) {
			enablePrayer(PRAYER_INDEX_PROTECT_ITEMS);
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (bot.getNpcId(npc) != NPC_ID_PIRATE) return;

		final int npcX = getX(npc);
		final int npcY = getY(npc);

		final int serverIndex = getServerIndex(npc);

		final Spawn spawn = spawnMap.get(serverIndex);

		if (spawn != null) {
			spawn.getCoordinate().set(npcX, npcY);
			spawn.setTimestamp(Long.MAX_VALUE);
		} else {
			spawnMap.put(serverIndex, new Spawn(new Coordinate(npcX, npcY), Long.MAX_VALUE));
		}

		nextRespawn = getNextRespawn();
	}

	@Override
	public void onNpcDespawned(final java.lang.Object npc) {
		final int serverIndex = getServerIndex(npc);
		final Spawn spawn = spawnMap.get(serverIndex);
		if (spawn == null) return;
		spawn.setTimestamp(System.currentTimeMillis());
		nextRespawn = getNextRespawn();
	}

	private Coordinate getNextRespawn() {
		if (spawnMap.isEmpty()) return null;
		return spawnMap.values().stream().min(Comparator.naturalOrder()).get().getCoordinate();
	}

	private String getPkerName() {
		for (int index = 0; index < bot.getPlayerCount(); index++) {
			final java.lang.Object player = bot.getPlayer(index);

			if (player == bot.getPlayer() ||
				!bot.isMobInCombat(player) ||
				getPlayerX(index) != playerX ||
				getPlayerY(index) != playerY) {
				continue;
			}

			return getPlayerName(index);
		}

		return null;
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
			return lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return upperBoundingCoordinate;
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
			return id;
		}

		public Coordinate getCoordinate() {
			return coordinate;
		}
	}
}
