import java.util.HashMap;
import java.util.Map;

/**
 * Collects red spiders eggs in Edgeville Dungeon.
 * Banks at Edgeville Bank. Walks back to Edgeville on death.
 * <p>
 * Required:
 * Start script at Edgeville Bank or at Deadly Red Spiders.
 * Optional: Sleeping bag, equipment, weapon.
 * <p>
 * Optional Parameter:
 * <controlled|attack|strength|defense> (default strength)
 * <p>
 *
 * @Author Chomp
 */
public class AA_RedSpidersEggs extends AA_Script {
	private static final Coordinate COORD_LUMBRIDGE = new Coordinate(120, 648);
	private static final Coordinate COORD_EDGEVILLE = new Coordinate(213, 456);
	private static final Coordinate COORD_WILD_GATE = new Coordinate(198, 3248);

	private static final int[] NPC_IDS_BLOCKING = new int[]{23, 40, 46};

	private static final int COORD_Y_DUNGEON = 3000;

	private final Map<RedSpidersEggs, Long> spawnMap = new HashMap<>();

	private Coordinate nextEggSpawn;
	private PathWalker pathWalker;

	private long startTime;

	private int playerX;
	private int playerY;

	private int initialEggsCount;
	private int eggsCollected;
	private int eggsBanked;

	private int deathCount;

	private boolean died;
	private boolean banking;

	public AA_RedSpidersEggs(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) combatStyle = CombatStyle.valueOf(parameters.toUpperCase());
		setCombatStyle(combatStyle.getIndex());
		initialEggsCount = getInventoryCount(RedSpidersEggs.ITEM_ID);
		banking = getInventoryCount() == MAX_INV_SIZE;
		startTime = System.currentTimeMillis();
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

		return banking ? bank() : collectEggs();
	}

	@Override
	public void onDeath() {
		System.out.printf("[%s] Oh dear, you are dead.%n", this);

		if (pathWalker == null) {
			pathWalker = new PathWalker(bot);
			pathWalker.init(null);
		}

		final PathWalker.Path path = pathWalker.calcPath(COORD_LUMBRIDGE.getX(),
			COORD_LUMBRIDGE.getY(),
			COORD_EDGEVILLE.getX(),
			COORD_EDGEVILLE.getY());

		if (path != null) {
			pathWalker.setPath(path);
			deathCount++;
			died = true;
		} else {
			exit("Failed to calculate path from Lumbridge to Edgeville.");
		}
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (!isBanking()) {
				return openBank();
			}

			if (hasInventoryItem(RedSpidersEggs.ITEM_ID)) {
				deposit(RedSpidersEggs.ITEM_ID, MAX_INV_SIZE);
				return SLEEP_ONE_TICK;
			}

			if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG) && hasBankItem(ITEM_ID_SLEEPING_BAG)) {
				withdraw(ITEM_ID_SLEEPING_BAG, 1);
				return SLEEP_TWO_SECONDS;
			}

			eggsBanked = bankCount(RedSpidersEggs.ITEM_ID);
			banking = false;
			return 0;
		}

		if (Area.LADDER_ROOM.contains(playerX, playerY)) {
			if (isWallObjectClosed(Object.LADDER_DOOR)) {
				atWallObject(Object.LADDER_DOOR.coordinate.getX(), Object.LADDER_DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.LADDER_DOOR.coordinate.getX(), Object.LADDER_DOOR.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (!isInDungeon()) {
			if (isWithinReach(Object.BANK_DOORS)) {
				if (isObjectClosed(Object.BANK_DOORS)) {
					atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (isInWilderness()) {
			if (playerY < COORD_WILD_GATE.getY()) {
				walkTo(COORD_WILD_GATE.getX(), COORD_WILD_GATE.getY());
				return SLEEP_ONE_TICK;
			}

			if (isWithinReach(Object.WILDERNESS_GATE)) {
				atObject(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.WILDERNESS_GATE.coordinate.getX() + 1, Object.WILDERNESS_GATE.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (!inCombat()) {
			final int blockingNpcIndex = getBlockingNpcIndex(playerX, playerY);

			if (blockingNpcIndex != -1) {
				attackNpc(blockingNpcIndex);
				return SLEEP_ONE_TICK;
			}
		}

		if (playerX < Object.DUNGEON_GATE.coordinate.getX()) {
			if (isWithinReach(Object.DUNGEON_GATE)) {
				if (isObjectClosed(Object.DUNGEON_GATE)) {
					atObject(Object.DUNGEON_GATE.coordinate.getX(), Object.DUNGEON_GATE.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(Object.DUNGEON_GATE.coordinate.getX(), Object.DUNGEON_GATE.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			walkTo(Object.DUNGEON_GATE.coordinate.getX() - 1, Object.DUNGEON_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (isWithinReach(Object.LADDER_UP)) {
			atObject(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int collectEggs() {
		if (Area.RED_SPIDERS.contains(playerX, playerY)) {
			if (getInventoryCount() == MAX_INV_SIZE) {
				eggsCollected += getInventoryCount(RedSpidersEggs.ITEM_ID);
				banking = true;
				return 0;
			}

			final RedSpidersEggs redSpidersEggs = getNearestRedSpidersEggs();

			if (redSpidersEggs != null) {
				if (inCombat()) {
					walkTo(redSpidersEggs.coordinate.getX(), redSpidersEggs.coordinate.getY());
					return SLEEP_ONE_TICK;
				}

				pickupItem(RedSpidersEggs.ITEM_ID, redSpidersEggs.coordinate.getX(), redSpidersEggs.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			if (nextEggSpawn != null &&
				(playerX != nextEggSpawn.getX() || playerY != nextEggSpawn.getY())) {
				walkTo(nextEggSpawn.getX(), nextEggSpawn.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (isInDungeon()) {
			if (isInWilderness()) {
				walkTo(Area.RED_SPIDERS.lowerBoundingCoordinate.getX(), Area.RED_SPIDERS.upperBoundingCoordinate.getY());
				return SLEEP_ONE_TICK;
			}

			if (!inCombat()) {
				final int blockingNpcIndex = getBlockingNpcIndex(playerX, playerY);

				if (blockingNpcIndex != -1) {
					attackNpc(blockingNpcIndex);
					return SLEEP_ONE_TICK;
				}
			}

			if (playerX < Object.DUNGEON_GATE.coordinate.getX()) {
				if (getFatigue() != 0 && hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
					return sleep();
				}

				if (isWithinReach(Object.WILDERNESS_GATE)) {
					atObject(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			if (isWithinReach(Object.DUNGEON_GATE)) {
				if (isObjectClosed(Object.DUNGEON_GATE)) {
					atObject(Object.DUNGEON_GATE.coordinate.getX(), Object.DUNGEON_GATE.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(Object.DUNGEON_GATE.coordinate.getX() - 1, Object.DUNGEON_GATE.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			walkTo(Object.DUNGEON_GATE.coordinate.getX(), Object.DUNGEON_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.LADDER_ROOM.contains(playerX, playerY)) {
			atObject(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (Area.BANK.contains(playerX, playerY)) {
			if (isObjectClosed(Object.BANK_DOORS)) {
				atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.LADDER_DOOR.coordinate.getX(), Object.LADDER_DOOR.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (isWithinReach(Object.LADDER_DOOR)) {
			if (isWallObjectClosed(Object.LADDER_DOOR)) {
				atWallObject(Object.LADDER_DOOR.coordinate.getX(), Object.LADDER_DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.LADDER_DOOR.coordinate.getX(), Object.LADDER_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.LADDER_DOOR.coordinate.getX(), Object.LADDER_DOOR.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private boolean isWallObjectClosed(final Object wallObject) {
		return getWallObjectIdFromCoords(wallObject.coordinate.getX(), wallObject.coordinate.getY()) == wallObject.id;
	}

	private boolean isInDungeon() {
		return playerY >= COORD_Y_DUNGEON;
	}

	private boolean isWithinReach(final Object object) {
		return distanceTo(object.coordinate.getX(), object.coordinate.getY()) <= 1;
	}

	private boolean isObjectClosed(final Object object) {
		return getObjectIdFromCoords(object.coordinate.getX(), object.coordinate.getY()) == object.id;
	}

	private boolean isInWilderness() {
		return playerY < Object.WILDERNESS_GATE.coordinate.getY();
	}

	private int getBlockingNpcIndex(final int playerX, final int playerY) {
		final int direction = bot.getMobDirection(bot.getPlayer());

		for (int index = 0; index < bot.getNpcCount(); index++) {
			if (!inArray(NPC_IDS_BLOCKING, getNpcId(index))) {
				continue;
			}

			final int npcX = getNpcX(index);
			final int npcY = getNpcY(index);

			if (npcY == playerY) {
				if (npcX == playerX - 1 && (direction == DIR_EAST || direction == DIR_NORTHEAST)) {
					return index;
				}

				if (npcX == playerX + 1 && (direction == DIR_WEST || direction == DIR_SOUTHWEST)) {
					return index;
				}
			}

			if (npcX == playerX) {
				if (npcY == playerY - 1 &&
					(direction == DIR_NORTH ||
						direction == DIR_NORTHWEST ||
						direction == DIR_EAST ||
						direction == DIR_NORTHEAST)) {
					return index;
				}

				if (npcY == playerY + 1 && direction >= DIR_WEST && direction <= DIR_SOUTHEAST) {
					return index;
				}
			}
		}

		return -1;
	}

	private RedSpidersEggs getNearestRedSpidersEggs() {
		RedSpidersEggs redSpidersEggs = null;

		int minimumDistance = Integer.MAX_VALUE;

		for (final RedSpidersEggs spawn : RedSpidersEggs.SPAWNS) {
			if (!isItemAt(RedSpidersEggs.ITEM_ID, spawn.coordinate.getX(), spawn.coordinate.getY())) {
				continue;
			}

			final int distance = distanceTo(spawn.coordinate.getX(), spawn.coordinate.getY());

			if (distance >= minimumDistance) {
				continue;
			}

			minimumDistance = distance;
			redSpidersEggs = spawn;
		}

		return redSpidersEggs;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Red Spiders Eggs", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final int count = Math.max(0, eggsCollected + getInventoryCount(RedSpidersEggs.ITEM_ID) - initialEggsCount);

		drawString(String.format("@yel@Collected: @whi@%s @cya@(@whi@%s eggs@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(count), toUnitsPerHour(count, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		if (eggsBanked > 0) {
			drawString(String.format("@gre@Banked: @whi@%s", DECIMAL_FORMAT.format(eggsBanked)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}

		if (deathCount > 0) {
			drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
			drawString(String.format("@red@Deaths: @whi@%d", deathCount),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}
	}

	@Override
	public void onGroundItemDespawned(final int groundItemIndex) {
		if (bot.getGroundItemId(groundItemIndex) != RedSpidersEggs.ITEM_ID) {
			return;
		}

		final int x = bot.getGroundItemLocalX(groundItemIndex) + bot.getAreaX();
		final int y = bot.getGroundItemLocalY(groundItemIndex) + bot.getAreaY();

		final RedSpidersEggs redSpidersEggs = RedSpidersEggs.fromCoord(x, y);

		if (redSpidersEggs == null) {
			return;
		}

		spawnMap.put(redSpidersEggs, System.currentTimeMillis());
		nextEggSpawn = spawnMap.entrySet().stream().min(Map.Entry.comparingByValue()).get().getKey().coordinate;
	}

	private enum RedSpidersEggs implements Comparable<RedSpidersEggs> {
		NORTH(new Coordinate(204, 3232)),
		EAST(new Coordinate(201, 3234)),
		SOUTH(new Coordinate(208, 3240)),
		WEST(new Coordinate(209, 3236));

		private static final RedSpidersEggs[] SPAWNS = RedSpidersEggs.values();
		private static final int ITEM_ID = 219;

		private final Coordinate coordinate;

		RedSpidersEggs(final Coordinate coordinate) {
			this.coordinate = coordinate;
		}

		private static RedSpidersEggs fromCoord(final int x, final int y) {
			for (final RedSpidersEggs eggs : SPAWNS) {
				if (x == eggs.coordinate.getX() && y == eggs.coordinate.getY()) {
					return eggs;
				}
			}

			return null;
		}
	}

	private enum Area implements RSArea {
		RED_SPIDERS(new Coordinate(197, 3225), new Coordinate(210, 3243)),
		BANK(new Coordinate(212, 448), new Coordinate(220, 453)),
		LADDER_ROOM(new Coordinate(214, 465), new Coordinate(218, 469));

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
		DUNGEON_GATE(57, new Coordinate(211, 3272)),
		WILDERNESS_GATE(305, new Coordinate(196, 3266)),
		BANK_DOORS(64, new Coordinate(217, 447)),
		LADDER_DOOR(2, new Coordinate(218, 465)),
		LADDER_UP(5, new Coordinate(215, 3300)),
		LADDER_DOWN(6, new Coordinate(215, 468));

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
