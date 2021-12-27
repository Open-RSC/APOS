import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
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
 * -f,--fightmode <controlled|attack|strength|defense> (default strength)
 * <p>
 * Author: Chomp
 */
public class AA_RedSpidersEggs extends AA_Script {
	private static final Coordinate COORDINATE_LUMBRIDGE_DEATH_WALK = new Coordinate(120, 648);
	private static final Coordinate COORDINATE_EDGEVILLE_DEATH_WALK = new Coordinate(213, 456);
	private static final Coordinate COORDINATE_LOAD_WILDERNESS_GATE = new Coordinate(198, 3248);

	private static final int[] NPC_IDS_BLOCKING = new int[]{23, 40, 46};

	private static final int COORDINATE_Y_DUNGEON = 3000;

	private final Map<RedSpidersEggs, Long> spawnMap = new HashMap<>();

	private Coordinate nextEggSpawn;
	private Instant startTime;
	private PathWalker pathWalker;

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
		this.initialEggsCount = this.getInventoryCount(RedSpidersEggs.ITEM_ID);
		this.banking = this.getInventoryCount() == MAX_INV_SIZE;
		this.startTime = Instant.now();
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

		return this.banking ? this.bank() : this.collectEggs();
	}

	@Override
	public void onDeath() {
		System.out.printf("[%s] Oh dear, you are dead.%n", this);

		if (this.pathWalker == null) {
			this.pathWalker = new PathWalker(this.extension);
			this.pathWalker.init(null);
		}

		final PathWalker.Path path = this.pathWalker.calcPath(COORDINATE_LUMBRIDGE_DEATH_WALK.getX(),
			COORDINATE_LUMBRIDGE_DEATH_WALK.getY(),
			COORDINATE_EDGEVILLE_DEATH_WALK.getX(),
			COORDINATE_EDGEVILLE_DEATH_WALK.getY());

		if (path != null) {
			this.pathWalker.setPath(path);
			this.deathCount++;
			this.died = true;
		} else {
			this.exit("Failed to calculate path from Lumbridge to Edgeville.");
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Red Spiders Eggs", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final int count = Math.max(0, this.eggsCollected +
			this.getInventoryCount(RedSpidersEggs.ITEM_ID) -
			this.initialEggsCount);

		this.drawString(String.format("@yel@Collected: @whi@%s @cya@(@whi@%s eggs@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(count), getUnitsPerHour(count, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.eggsBanked > 0) {
			this.drawString(String.format("@gre@Banked: @whi@%s", DECIMAL_FORMAT.format(this.eggsBanked)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}

		if (this.deathCount > 0) {
			this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
			this.drawString(String.format("@red@Deaths: @whi@%d", this.deathCount),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}
	}

	@Override
	public void onGroundItemDespawned(final int id, final int x, final int y) {
		if (id != RedSpidersEggs.ITEM_ID) {
			return;
		}

		final RedSpidersEggs redSpidersEggs = RedSpidersEggs.fromCoord(x, y);

		if (redSpidersEggs == null) {
			return;
		}

		this.spawnMap.put(redSpidersEggs, System.currentTimeMillis());
		this.nextEggSpawn = this.spawnMap.entrySet().stream().min(Map.Entry.comparingByValue()).get().getKey().coordinate;
	}

	private int collectEggs() {
		if (Area.RED_SPIDERS.contains(this.playerX, this.playerY)) {
			if (this.getInventoryCount() == MAX_INV_SIZE) {
				this.eggsCollected += this.getInventoryCount(RedSpidersEggs.ITEM_ID);
				this.banking = true;
				return 0;
			}

			final RedSpidersEggs redSpidersEggs = this.getNearestRedSpidersEggs();

			if (redSpidersEggs != null) {
				if (this.inCombat()) {
					this.walkTo(redSpidersEggs.coordinate.getX(), redSpidersEggs.coordinate.getY());
					return SLEEP_ONE_TICK;
				}

				this.pickupItem(RedSpidersEggs.ITEM_ID, redSpidersEggs.coordinate.getX(), redSpidersEggs.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			if (this.nextEggSpawn != null &&
				(this.playerX != this.nextEggSpawn.getX() || this.playerY != this.nextEggSpawn.getY())) {
				this.walkTo(this.nextEggSpawn.getX(), this.nextEggSpawn.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (this.isInDungeon()) {
			if (this.isInWilderness()) {
				this.walkTo(Area.RED_SPIDERS.lowerBoundingCoordinate.getX(), Area.RED_SPIDERS.upperBoundingCoordinate.getY());
				return SLEEP_ONE_TICK;
			}

			if (!this.inCombat()) {
				final int blockingNpcIndex = this.getBlockingNpcIndex(this.playerX, this.playerY);

				if (blockingNpcIndex != -1) {
					this.attackNpc(blockingNpcIndex);
					return SLEEP_ONE_TICK;
				}
			}

			if (this.playerX < Object.DUNGEON_GATE.coordinate.getX()) {
				if (this.getFatigue() != 0 && this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
					return this.sleep();
				}

				if (this.isWithinReach(Object.WILDERNESS_GATE)) {
					this.atObject(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				this.walkTo(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			if (this.isWithinReach(Object.DUNGEON_GATE)) {
				if (this.isObjectClosed(Object.DUNGEON_GATE)) {
					this.atObject(Object.DUNGEON_GATE.coordinate.getX(), Object.DUNGEON_GATE.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				this.walkTo(Object.DUNGEON_GATE.coordinate.getX() - 1, Object.DUNGEON_GATE.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			this.walkTo(Object.DUNGEON_GATE.coordinate.getX(), Object.DUNGEON_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.LADDER_ROOM.contains(this.playerX, this.playerY)) {
			this.atObject(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (this.isObjectClosed(Object.BANK_DOORS)) {
				this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.LADDER_DOOR.coordinate.getX(), Object.LADDER_DOOR.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (this.isWithinReach(Object.LADDER_DOOR)) {
			if (this.isWallObjectClosed(Object.LADDER_DOOR)) {
				this.atWallObject(Object.LADDER_DOOR.coordinate.getX(), Object.LADDER_DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.LADDER_DOOR.coordinate.getX(), Object.LADDER_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.LADDER_DOOR.coordinate.getX(), Object.LADDER_DOOR.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (!this.isBanking()) {
				return this.openBank();
			}

			if (this.hasInventoryItem(RedSpidersEggs.ITEM_ID)) {
				this.deposit(RedSpidersEggs.ITEM_ID, MAX_INV_SIZE);
				return SLEEP_ONE_TICK;
			}

			if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG) && this.hasBankItem(ITEM_ID_SLEEPING_BAG)) {
				this.withdraw(ITEM_ID_SLEEPING_BAG, 1);
				return SLEEP_TWO_SECONDS;
			}

			this.eggsBanked = this.bankCount(RedSpidersEggs.ITEM_ID);
			this.banking = false;
			return 0;
		}

		if (Area.LADDER_ROOM.contains(this.playerX, this.playerY)) {
			if (this.isWallObjectClosed(Object.LADDER_DOOR)) {
				this.atWallObject(Object.LADDER_DOOR.coordinate.getX(), Object.LADDER_DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.LADDER_DOOR.coordinate.getX(), Object.LADDER_DOOR.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (!this.isInDungeon()) {
			if (this.isWithinReach(Object.BANK_DOORS)) {
				if (this.isObjectClosed(Object.BANK_DOORS)) {
					this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.isInWilderness()) {
			if (this.playerY < COORDINATE_LOAD_WILDERNESS_GATE.getY()) {
				this.walkTo(COORDINATE_LOAD_WILDERNESS_GATE.getX(), COORDINATE_LOAD_WILDERNESS_GATE.getY());
				return SLEEP_ONE_TICK;
			}

			if (this.isWithinReach(Object.WILDERNESS_GATE)) {
				this.atObject(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.WILDERNESS_GATE.coordinate.getX() + 1, Object.WILDERNESS_GATE.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (!this.inCombat()) {
			final int blockingNpcIndex = this.getBlockingNpcIndex(this.playerX, this.playerY);

			if (blockingNpcIndex != -1) {
				this.attackNpc(blockingNpcIndex);
				return SLEEP_ONE_TICK;
			}
		}

		if (this.playerX < Object.DUNGEON_GATE.coordinate.getX()) {
			if (this.isWithinReach(Object.DUNGEON_GATE)) {
				if (this.isObjectClosed(Object.DUNGEON_GATE)) {
					this.atObject(Object.DUNGEON_GATE.coordinate.getX(), Object.DUNGEON_GATE.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				this.walkTo(Object.DUNGEON_GATE.coordinate.getX(), Object.DUNGEON_GATE.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			this.walkTo(Object.DUNGEON_GATE.coordinate.getX() - 1, Object.DUNGEON_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.isWithinReach(Object.LADDER_UP)) {
			this.atObject(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private boolean isWithinReach(final Object object) {
		return this.distanceTo(object.coordinate.getX(), object.coordinate.getY()) <= 1;
	}

	private boolean isWallObjectClosed(final Object wallObject) {
		return this.getWallObjectIdFromCoords(wallObject.coordinate.getX(), wallObject.coordinate.getY()) == wallObject.id;
	}

	private boolean isObjectClosed(final Object object) {
		return this.getObjectIdFromCoords(object.coordinate.getX(), object.coordinate.getY()) == object.id;
	}

	private boolean isInDungeon() {
		return this.playerY >= COORDINATE_Y_DUNGEON;
	}

	private boolean isInWilderness() {
		return this.playerY < Object.WILDERNESS_GATE.coordinate.getY();
	}

	private RedSpidersEggs getNearestRedSpidersEggs() {
		RedSpidersEggs redSpidersEggs = null;

		int minimumDistance = Integer.MAX_VALUE;

		for (final RedSpidersEggs spawn : RedSpidersEggs.SPAWNS) {
			if (!this.isItemAt(RedSpidersEggs.ITEM_ID, spawn.coordinate.getX(), spawn.coordinate.getY())) {
				continue;
			}

			final int distance = this.distanceTo(spawn.coordinate.getX(), spawn.coordinate.getY());

			if (distance >= minimumDistance) {
				continue;
			}

			minimumDistance = distance;
			redSpidersEggs = spawn;
		}

		return redSpidersEggs;
	}

	private int getBlockingNpcIndex(final int playerX, final int playerY) {
		final int direction = this.extension.getMobDirection(this.extension.getPlayer());

		for (int index = 0; index < this.extension.getNpcCount(); index++) {
			if (!inArray(NPC_IDS_BLOCKING, this.getNpcId(index))) {
				continue;
			}

			final int npcX = this.getNpcX(index);
			final int npcY = this.getNpcY(index);

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
			return this.lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return this.upperBoundingCoordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
		}
	}
}
