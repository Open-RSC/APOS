import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Kills Chaos Druids at Yanille Dungeon and banks loot at Yanille Bank.
 * <p>
 * Requirements:
 * Start script at Yanille Bank with sleeping bag and lockpick in inventory.
 * <p>
 * Optional Parameter
 * <controlled|attack|strength|defense> (default strength)
 * <p>
 *
 * @Author Chomp
 */
public class AA_YanilleChaosDruids extends AA_Script {
	private static final Coordinate COORDINATE_BANK = new Coordinate(587, 753);

	private static final int[] ITEM_IDS_LOOT = new int[]{
		31, 33, 34, 40, 42,
		157, 158, 159, 160, 165, 220,
		435, 436, 437, 438, 439, 440, 441, 442, 443, 464, 469, 471, 473, 497,
		526, 527, 1092, 1277};
	private static final int[] ITEM_IDS_PREMIUM_LOOT = new int[]{
		220, 438, 439, 441, 442, 443, 469, 471, 473, 526, 527, 1092, 1277};
	private static final int[] NPC_IDS_CHAOS_DRUID = new int[]{270, 555};

	private static final int NPC_XP_CHAOS_DRUID = 58;
	private static final int NPC_ID_GIANT_BAT = 43;
	private static final int ITEM_ID_LOCKPICK = 714;
	private static final int MINIMUM_THIEVING = 82;
	private static final int MAXIMUM_FATIGUE = 99;
	private static final int MAXIMUM_DISTANCE_FROM_LOOT = 2;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 2;
	private static final int COORDINATE_Y_DUNGEON = 3000;

	private final int[] loot = new int[3];

	private final Map<Integer, Integer> premiumLoot = new TreeMap<>();
	private final Map<Integer, Spawn> spawnMap = new HashMap<>();

	private Coordinate nextRespawn;
	private long startTime;

	private double initialCombatXp;

	private long doorTimeout;
	private long picklockTimeout;
	private long syncRequestTimeout;

	private int playerX;
	private int playerY;

	public AA_YanilleChaosDruids(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (getLevel(Skill.THIEVING.getIndex()) < MINIMUM_THIEVING) {
			throw new IllegalStateException(String.format("You must have L%d+ thieving to use this script.", MINIMUM_THIEVING));
		}

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!hasInventoryItem(ITEM_ID_LOCKPICK)) {
			throw new IllegalStateException("Lockpick missing from inventory.");
		}

		if (!parameters.isEmpty()) combatStyle = CombatStyle.valueOf(parameters.toUpperCase());

		setCombatStyle(combatStyle.getIndex());
		initialCombatXp = getTotalCombatXp();
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		if (bot.getCombatStyle() != combatStyle.getIndex()) setCombatStyle(combatStyle.getIndex());

		return getInventoryCount() == MAX_INV_SIZE || isBanking() ? bank() : kill();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("fail", 4) || message.startsWith("go", 4)) {
			picklockTimeout = 0L;
		} else if (message.endsWith("shut") || message.endsWith("open")) {
			doorTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			return depositLoot();
		}

		if (Area.HUT.contains(playerX, playerY)) {
			return exitHut();
		}

		if (Area.CHAOS_DRUIDS.contains(playerX, playerY)) {
			return exitPicklockDoor();
		}

		if (playerY > COORDINATE_Y_DUNGEON) {
			return climbStairs();
		}

		walkTo(COORDINATE_BANK.getX(), COORDINATE_BANK.getY());
		return SLEEP_ONE_TICK;
	}

	private int kill() {
		if (Area.CHAOS_DRUIDS.contains(playerX, playerY)) {
			return combatCycle();
		}

		if (playerY > COORDINATE_Y_DUNGEON) {
			return enterPicklockDoor();
		}

		if (Area.HUT.contains(playerX, playerY)) {
			atObject(Object.STAIRS_DOWN.coordinate.getX(), Object.STAIRS_DOWN.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		return enterHut();
	}

	private int depositLoot() {
		if (!isBanking()) {
			return openBank();
		}

		for (int index = 0; index < getInventoryCount(); index++) {
			final int itemId = getInventoryId(index);

			if (!inArray(ITEM_IDS_LOOT, itemId)) {
				continue;
			}

			deposit(itemId, getInventoryCount(itemId));
			return SLEEP_ONE_TICK;
		}

		updateBankLoot();
		closeBank();
		return SLEEP_ONE_SECOND;
	}

	private int exitHut() {
		if (getWallObjectIdFromCoords(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY()) == Object.HUT_DOOR.id) {
			if (System.currentTimeMillis() <= doorTimeout) {
				return 0;
			}

			atWallObject(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY());
			doorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		walkTo(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int exitPicklockDoor() {
		if (playerX != Object.PICKLOCK_DOOR.coordinate.getX() || playerY != Object.PICKLOCK_DOOR.coordinate.getY() - 1) {
			walkTo(Object.PICKLOCK_DOOR.coordinate.getX(), Object.PICKLOCK_DOOR.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= picklockTimeout) {
			return 0;
		}

		atWallObject2(Object.PICKLOCK_DOOR.coordinate.getX(), Object.PICKLOCK_DOOR.coordinate.getY());
		picklockTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int climbStairs() {
		if (inCombat()) {
			walkTo(Object.STAIRS_UP.coordinate.getX() + 2, Object.STAIRS_UP.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		final int blockingNpc = getBlockingNpc(playerX, playerY);

		if (blockingNpc != -1) {
			attackNpc(blockingNpc);
			return SLEEP_ONE_TICK;
		}

		if (playerX != Object.STAIRS_UP.coordinate.getX() + 2 || playerY != Object.STAIRS_UP.coordinate.getY() - 1) {
			walkTo(Object.STAIRS_UP.coordinate.getX() + 2, Object.STAIRS_UP.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		atObject(Object.STAIRS_UP.coordinate.getX(), Object.STAIRS_UP.coordinate.getY());
		return SLEEP_ONE_SECOND;
	}

	private int combatCycle() {
		if (inCombat()) {
			return 0;
		}

		if (getFatigue() >= MAXIMUM_FATIGUE) {
			return sleep();
		}

		updateLoot();

		if (loot[0] != -1) {
			pickupItem(loot[0], loot[1], loot[2]);
			return SLEEP_ONE_TICK;
		}

		final int druid = getNearestChaosDruid();

		if (druid != -1) {
			attackNpc(druid);
			return SLEEP_ONE_TICK;
		}

		if (nextRespawn != null && (playerX != nextRespawn.getX() || playerY != nextRespawn.getY())) {
			walkTo(nextRespawn.getX(), nextRespawn.getY());
			return SLEEP_ONE_TICK;
		}

		return 0;
	}

	private int enterPicklockDoor() {
		if (System.currentTimeMillis() <= picklockTimeout) {
			return 0;
		}

		if (inCombat()) {
			walkTo(Object.PICKLOCK_DOOR.coordinate.getX(), Object.PICKLOCK_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		final int blockingNpc = getBlockingNpc(playerX, playerY);

		if (blockingNpc != -1) {
			attackNpc(blockingNpc);
			return SLEEP_ONE_TICK;
		}

		if (playerX != Object.PICKLOCK_DOOR.coordinate.getX() || playerY != Object.PICKLOCK_DOOR.coordinate.getY()) {
			walkTo(Object.PICKLOCK_DOOR.coordinate.getX(), Object.PICKLOCK_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (!spawnMap.isEmpty()) {
			resetSpawns();
		}

		atWallObject2(Object.PICKLOCK_DOOR.coordinate.getX(), Object.PICKLOCK_DOOR.coordinate.getY());
		picklockTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int enterHut() {
		if (distanceTo(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			if (getWallObjectIdFromCoords(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY()) == Object.HUT_DOOR.id) {
				if (System.currentTimeMillis() <= doorTimeout) {
					return 0;
				}

				atWallObject(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY());
				doorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			walkTo(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private void updateBankLoot() {
		for (final int itemId : ITEM_IDS_PREMIUM_LOOT) {
			final int bankCount = bankCount(itemId);

			if (bankCount == 0) {
				continue;
			}

			premiumLoot.put(itemId, bankCount);
		}
	}

	private int getBlockingNpc(final int playerX, final int playerY) {
		final int direction = bot.getMobDirection(bot.getPlayer());

		for (int index = 0; index < bot.getNpcCount(); index++) {
			if (getNpcId(index) != NPC_ID_GIANT_BAT) {
				continue;
			}

			final int npcX = getNpcX(index);
			final int npcY = getNpcY(index);

			if ((direction == DIR_NORTH || direction == DIR_NORTHEAST) &&
				((npcX == playerX && npcY == playerY - 1) ||
					(npcY == playerY && npcX == playerX - 1))) {
				return index;
			}

			if (direction == DIR_SOUTH && npcX == playerX && npcY == playerY + 1) {
				return index;
			}
		}

		return -1;
	}

	private void updateLoot() {
		loot[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < getGroundItemCount(); index++) {
			final int groundItemId = getGroundItemId(index);

			if (!inArray(ITEM_IDS_LOOT, groundItemId)) {
				continue;
			}

			final int groundItemX = getItemX(index);
			final int groundItemY = getItemY(index);

			if (distanceTo(groundItemX, groundItemY) > MAXIMUM_DISTANCE_FROM_LOOT ||
				!Area.CHAOS_DRUIDS.contains(groundItemX, groundItemY)) {
				continue;
			}

			final int distance = distanceTo(groundItemX, groundItemY);

			if (distance < currentDistance) {
				loot[0] = groundItemId;
				loot[1] = groundItemX;
				loot[2] = groundItemY;

				currentDistance = distance;
			}
		}
	}

	private int getNearestChaosDruid() {
		int nearestChaosDruid = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < bot.getNpcCount(); index++) {
			final int npcId = getNpcId(index);

			if (isNpcInCombat(index) ||
				(npcId != NPC_IDS_CHAOS_DRUID[0] && npcId != NPC_IDS_CHAOS_DRUID[1])) {
				continue;
			}

			final int npcX = getNpcX(index);
			final int npcY = getNpcY(index);

			if (!Area.CHAOS_DRUIDS.contains(npcX, npcY)) {
				continue;
			}

			final int distance = distanceTo(npcX, npcY, playerX, playerY);

			if (distance < currentDistance) {
				nearestChaosDruid = index;
				currentDistance = distance;
			}
		}

		return nearestChaosDruid;
	}

	private void resetSpawns() {
		spawnMap.clear();
		nextRespawn = null;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Yanille Chaos Druids", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		final int kills = (int) xpGained / NPC_XP_CHAOS_DRUID;

		drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, toUnitsPerHour(kills, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (nextRespawn != null) {
			drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)",
					nextRespawn.getX(), nextRespawn.getY()),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}

		if (!premiumLoot.isEmpty()) {
			drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

			for (final Map.Entry<Integer, Integer> entry : premiumLoot.entrySet()) {
				drawString(String.format("@gre@%s: @whi@%d", getItemNameId(entry.getKey()), entry.getValue()),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
			}
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		final int npcId = bot.getNpcId(npc);

		if (npcId != NPC_IDS_CHAOS_DRUID[0] && npcId != NPC_IDS_CHAOS_DRUID[1]) return;

		final int npcX = getX(npc);
		final int npcY = getY(npc);

		if (!Area.CHAOS_DRUIDS.contains(npcX, npcY)) return;

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

	private enum Area implements RSArea {
		CHAOS_DRUIDS(new Coordinate(576, 3585), new Coordinate(598, 3589)) {
			@Override
			public boolean contains(final int x, final int y) {
				return super.contains(x, y) || (x >= 576 && x <= 594 && y >= 3580 && y <= 3584);
			}
		},
		BANK(new Coordinate(585, 750), new Coordinate(590, 758)),
		HUT(new Coordinate(589, 761), new Coordinate(593, 764));

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
		HUT_DOOR(2, new Coordinate(591, 765)),
		PICKLOCK_DOOR(162, new Coordinate(593, 3590)),
		STAIRS_UP(43, new Coordinate(591, 3593)),
		STAIRS_DOWN(42, new Coordinate(591, 761));

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
