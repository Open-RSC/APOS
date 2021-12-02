import java.awt.*;
import java.time.Duration;
import java.time.Instant;

/**
 * Mines iron and coal ore and smelts steel bars.
 * <p>
 * Requirements:
 * Start at Ardougne with pickaxe and sleeping bag in inventory.
 * Mininum combat lvl 53.
 * <p>
 * Author: Chomp
 */
public class AA_ArdougneSteel extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_MINE = new Coordinate(532, 592);
	private static final Coordinate COORDINATE_LOAD_SMELT = new Coordinate(560, 595);

	private static final int[] OBJECT_IDS_ROCK_COAL = new int[]{110, 111};
	private static final int[] OBJECT_IDS_ROCK_IRON = new int[]{102, 103};

	private static final int ITEM_ID_IRON_ORE = 151;
	private static final int ITEM_ID_COAL_ORE = 155;
	private static final int ITEM_ID_STEEL_BAR = 171;

	private static final int ORE_COUNT_IRON = 10;
	private static final int ORE_COUNT_COAL = 18;

	private static final int INITIAL_INVENTORY_SIZE = 2;

	private static final int MAXIMUM_FATIGUE = 99;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;

	private static final int SKILL_INDEX_SMITHING = 13;
	private static final int SKILL_INDEX_MINING = 14;

	private final int[] nearestRock = new int[2];
	private final int[] previousRock = new int[]{-1, -1};

	private State state;
	private Instant startTime;

	private double initialMiningXp;
	private double initialSmithingXp;

	private long clickTimeout;
	private long previousRockTimeout;

	private int steelBarCount;

	private int playerX;
	private int playerY;

	public AA_ArdougneSteel(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		boolean pickaxeMissing = true;

		for (final Pickaxe pickaxe : Pickaxe.values()) {
			if (this.hasInventoryItem(pickaxe.id)) {
				pickaxeMissing = false;
				break;
			}
		}

		if (pickaxeMissing) {
			throw new IllegalStateException("Pickaxe missing from inventory.");
		}

		this.playerX = this.getX();
		this.playerY = this.getY();

		if (Area.MINE.contains(this.playerX, this.playerY)) {
			this.state = State.MINE;
		} else if (Area.SMELT.contains(this.playerX, this.playerY)) {
			this.state = State.SMELT;
		} else {
			if (this.getInventoryCount() == MAX_INV_SIZE) {
				this.state = State.SMELT;
			} else if (this.getInventoryCount() == INITIAL_INVENTORY_SIZE) {
				this.state = State.MINE;
			} else {
				this.state = State.BANK;
			}
		}

		this.initialMiningXp = this.getAccurateXpForLevel(SKILL_INDEX_MINING);
		this.initialSmithingXp = this.getAccurateXpForLevel(SKILL_INDEX_SMITHING);

		this.startTime = Instant.now();
	}

	@Override
	public int main() {

		this.playerX = this.getX();
		this.playerY = this.getY();

		switch (this.state) {
			case MINE:
				return this.mine();
			case SMELT:
				return this.smelt();
			case BANK:
				return this.bank();
			default:
				throw new IllegalStateException("Invalid script state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("You only") ||
			message.startsWith("There is")) {
			this.clickTimeout = 0;
		} else if (message.endsWith("steel ")) {
			this.steelBarCount++;
			this.clickTimeout = 0;
		} else if (message.startsWith("You manage") || message.startsWith("You just")) {
			this.clickTimeout = 0;
			this.previousRock[0] = this.nearestRock[0];
			this.previousRock[1] = this.nearestRock[1];
			this.previousRockTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Ardougne Steel", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final double miningXpGained = this.getAccurateXpForLevel(SKILL_INDEX_MINING) - this.initialMiningXp;

		this.drawString(String.format("@yel@Mining: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(miningXpGained), getUnitsPerHour(miningXpGained, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final double smithingXpGained = this.getAccurateXpForLevel(SKILL_INDEX_SMITHING) - this.initialSmithingXp;

		this.drawString(String.format("@yel@Smithing: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(smithingXpGained), getUnitsPerHour(smithingXpGained, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Steel: @whi@%s @cya@(@whi@%s bars@cya@/@whi@hr@cya@)",
				this.steelBarCount, getUnitsPerHour(this.steelBarCount, secondsElapsed)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
	}

	private int mine() {
		if (Area.MINE.contains(this.playerX, this.playerY)) {
			if (this.getInventoryCount() == MAX_INV_SIZE) {
				this.state = State.SMELT;
				return 0;
			}

			if (this.getFatigue() >= MAXIMUM_FATIGUE) {
				return this.sleep();
			}

			if (this.clickTimeout != 0 && System.currentTimeMillis() <= this.clickTimeout) {
				return 0;
			}

			this.updateNearestRock();

			if (this.nearestRock[0] == -1) {
				return 0;
			}

			this.atObject(this.nearestRock[0], this.nearestRock[1]);
			this.clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (Area.BANK.contains(this.playerX, this.playerY) &&
			this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (this.playerX > COORDINATE_LOAD_MINE.getX() || this.playerY > COORDINATE_LOAD_MINE.getY()) {
			this.walkTo(COORDINATE_LOAD_MINE.getX(), COORDINATE_LOAD_MINE.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Area.MINE.upperBoundingCoordinate.getX(), Area.MINE.upperBoundingCoordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int smelt() {
		if (Area.SMELT.contains(this.playerX, this.playerY)) {
			final int ironOreIndex = this.getInventoryIndex(ITEM_ID_IRON_ORE);

			if (ironOreIndex == -1 || this.getInventoryCount(ITEM_ID_COAL_ORE) < 2) {
				this.state = State.BANK;
				return 0;
			}

			if (this.getFatigue() >= MAXIMUM_FATIGUE) {
				return this.sleep();
			}

			if (this.clickTimeout != 0 && System.currentTimeMillis() <= this.clickTimeout) {
				return 0;
			}

			this.useItemOnObject(ITEM_ID_IRON_ORE, Object.FURNACE.coordinate.getX(), Object.FURNACE.coordinate.getY());
			this.clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (this.playerX < COORDINATE_LOAD_SMELT.getX()) {
			this.walkTo(COORDINATE_LOAD_SMELT.getX(), COORDINATE_LOAD_SMELT.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.distanceTo(Object.FURNACE_DOOR.coordinate.getX(), Object.FURNACE_DOOR.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT &&
			this.getWallObjectIdFromCoords(Object.FURNACE_DOOR.coordinate.getX(), Object.FURNACE_DOOR.coordinate.getY()) == Object.FURNACE_DOOR.id) {
			this.atWallObject(Object.FURNACE_DOOR.coordinate.getX(), Object.FURNACE_DOOR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(Object.FURNACE.coordinate.getX(), Object.FURNACE.coordinate.getY() + 2);
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (this.getInventoryCount() == INITIAL_INVENTORY_SIZE) {
				this.closeBank();
				this.state = State.MINE;
				return 0;
			}

			if (!this.isBanking()) {
				return this.openBank();
			}

			final int itemId = this.getInventoryId(INITIAL_INVENTORY_SIZE);
			final int itemCount = this.getInventoryCount(itemId);

			this.deposit(itemId, itemCount);
			return SLEEP_ONE_TICK;
		}

		if (Area.SMELT.contains(this.playerX, this.playerY) &&
			this.getWallObjectIdFromCoords(Object.FURNACE_DOOR.coordinate.getX(), Object.FURNACE_DOOR.coordinate.getY()) == Object.FURNACE_DOOR.id) {
			this.atWallObject(Object.FURNACE_DOOR.coordinate.getX(), Object.FURNACE_DOOR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (this.distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT &&
			this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private void updateNearestRock() {
		this.nearestRock[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		final boolean haveCoal = this.getInventoryCount(ITEM_ID_COAL_ORE) >= ORE_COUNT_COAL;
		final boolean haveIron = this.getInventoryCount(ITEM_ID_IRON_ORE) >= ORE_COUNT_IRON;

		for (int index = 0; index < this.getObjectCount(); index++) {
			final int objectId = this.getObjectId(index);

			if (inArray(OBJECT_IDS_ROCK_COAL, objectId)) {
				if (haveCoal) {
					continue;
				}
			} else if (inArray(OBJECT_IDS_ROCK_IRON, objectId)) {
				if (haveIron) {
					continue;
				}
			} else {
				continue;
			}

			final int objectX = this.getObjectX(index);
			final int objectY = this.getObjectY(index);

			if (objectX == this.previousRock[0] &&
				objectY == this.previousRock[1] &&
				System.currentTimeMillis() <= this.previousRockTimeout) {
				continue;
			}

			final int distance = this.distanceTo(objectX, objectY);

			if (distance < currentDistance) {
				this.nearestRock[0] = objectX;
				this.nearestRock[1] = objectY;

				currentDistance = distance;
			}
		}
	}

	private enum State {
		MINE,
		SMELT,
		BANK
	}

	private enum Pickaxe {
		RUNE(1262),
		ADAMANTITE(1261),
		MITHRIL(1260),
		STEEL(1259),
		IRON(1258),
		BRONZE(156);

		private final int id;

		Pickaxe(final int id) {
			this.id = id;
		}
	}

	private enum Area implements RSArea {
		MINE(new Coordinate(513, 567), new Coordinate(526, 575)),
		SMELT(new Coordinate(589, 589), new Coordinate(592, 594)),
		BANK(new Coordinate(551, 609), new Coordinate(554, 616));

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
		BANK_DOORS(64, new Coordinate(550, 612)),
		FURNACE_DOOR(2, new Coordinate(590, 595)),
		FURNACE(118, new Coordinate(591, 590));

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
