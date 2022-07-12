import com.aposbot.Constants;

/**
 * A script for telegrabbing wines of zamorak.
 * <p>
 * Start script between Falador West Bank and the Chaos Temple.
 * Inventory: sleeping bag, staff of air, water runes, law runes
 * <p>
 *
 * @author Chomp
 */
public class AA_WineOfZamorak extends AA_Script {
	private static final Coordinate COORDINATE_WINE = new Coordinate(333, 434);

	private static final Coordinate[] PATH_TO_CHAOS_TEMPLE = new Coordinate[]{
		new Coordinate(323, 528),
		new Coordinate(310, 480),
		new Coordinate(328, 435)
	};

	private static final int ITEM_ID_WINE_OF_ZAMORAK = 501;
	private static final int ITEM_ID_STAFF_OF_AIR = 101;
	private static final int ITEM_ID_LAW_RUNE = 42;
	private static final int ITEM_ID_WATER_RUNE = 32;

	private static final int SPELL_ID_TELEKINETIC_GRAB = 16;
	private static final int SPELL_ID_TELEPORT_TO_FALADOR = 18;

	private static final int COORDINATE_Y_FALADOR = 528;
	private static final int COORDINATE_Y_CHAOS_TEMPLE = 435;

	private static final int MAX_PLAYER_DISTANCE = 5;
	private static final int MAX_FATIGUE = 100;

	private long startTime;

	private long logoutTimeout;
	private long depositTimeout;

	private int playerX;
	private int playerY;

	private int staffOfAirIndex;
	private int waterRuneIndex;
	private int lawRuneIndex;

	private int winesCollected;
	private int winesBanked;

	private boolean banking;
	private boolean idle;

	public AA_WineOfZamorak(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if ((staffOfAirIndex = getInventoryIndex(ITEM_ID_STAFF_OF_AIR)) == -1) {
			throw new IllegalStateException("Staff of air missing from inventory.");
		}

		if ((waterRuneIndex = getInventoryIndex(ITEM_ID_WATER_RUNE)) == -1) {
			throw new IllegalStateException("Water runes missing from inventory.");
		}

		if ((lawRuneIndex = getInventoryIndex(ITEM_ID_LAW_RUNE)) == -1) {
			throw new IllegalStateException("Law runes missing from inventory.");
		}

		banking = bot.getInventorySize() == MAX_INV_SIZE;
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		return banking ? bank() : collect();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("area")) {
			idle = true;
		} else if (message.endsWith("successful")) {
			winesCollected++;
		} else {
			super.onServerMessage(message);
		}
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			return depositWines();
		}

		if (playerY <= COORDINATE_Y_FALADOR) {
			return teleportToFalador();
		}

		return enterBank();
	}

	private int collect() {
		if (playerY <= COORDINATE_Y_CHAOS_TEMPLE) {
			return teleGrabWineOfZamorak();
		}

		return enterChaosTemple();
	}

	private int depositWines() {
		if (!bot.isBankVisible()) {
			return openBank();
		}

		if (hasInventoryItem(ITEM_ID_WINE_OF_ZAMORAK)) {
			if (System.currentTimeMillis() <= depositTimeout) {
				return 0;
			}

			deposit(ITEM_ID_WINE_OF_ZAMORAK, MAX_INV_SIZE);
			depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		winesBanked = bankCount(ITEM_ID_WINE_OF_ZAMORAK);
		banking = false;
		return 0;
	}

	private int teleportToFalador() {
		if (bot.getInventoryId(lawRuneIndex) != ITEM_ID_LAW_RUNE) {
			return exit("Law runes missing from inventory.");
		}

		if (bot.getInventoryId(waterRuneIndex) != ITEM_ID_WATER_RUNE) {
			return exit("Water runes missing from inventory.");
		}

		if (bot.getInventoryId(staffOfAirIndex) != ITEM_ID_STAFF_OF_AIR) {
			return exit("Staff of air missing from inventory.");
		}

		if (!bot.isEquipped(staffOfAirIndex)) {
			wearItem(staffOfAirIndex);
			return SLEEP_ONE_TICK;
		}

		castOnSelf(SPELL_ID_TELEPORT_TO_FALADOR);
		return SLEEP_ONE_SECOND;
	}

	private int enterBank() {
		if (distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= 1) {
			if (getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
				atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int teleGrabWineOfZamorak() {
		if (bot.getInventorySize() == MAX_INV_SIZE) {
			banking = true;
			return 0;
		}

		if (idle) {
			if (playerX == COORDINATE_WINE.getX() - 1 && playerY == COORDINATE_WINE.getY()) {
				walkTo(COORDINATE_WINE.getX() - 1, COORDINATE_WINE.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			idle = false;
		}

		if (playerX != COORDINATE_WINE.getX() - 1 || playerY != COORDINATE_WINE.getY()) {
			walkTo(COORDINATE_WINE.getX() - 1, COORDINATE_WINE.getY());
			return SLEEP_ONE_TICK;
		}

		if (isItemAt(ITEM_ID_WINE_OF_ZAMORAK, COORDINATE_WINE.getX(), COORDINATE_WINE.getY())) {
			if (bot.getInventoryId(lawRuneIndex) != ITEM_ID_LAW_RUNE) {
				return exit("Law runes missing from inventory.");
			}

			if (bot.getInventoryId(staffOfAirIndex) != ITEM_ID_STAFF_OF_AIR) {
				return exit("Staff of air missing from inventory.");
			}

			if (!bot.isEquipped(staffOfAirIndex)) {
				wearItem(staffOfAirIndex);
				return SLEEP_ONE_TICK;
			}

			bot.createPacket(Constants.OP_GITEM_CAST);
			bot.put2(COORDINATE_WINE.getX());
			bot.put2(COORDINATE_WINE.getY());
			bot.put2(ITEM_ID_WINE_OF_ZAMORAK);
			bot.put2(SPELL_ID_TELEKINETIC_GRAB);
			bot.finishPacket();

			return SLEEP_ONE_SECOND;
		}

		if (getFatigue() == MAX_FATIGUE) {
			return sleep();
		}

		return 0;
	}

	private int enterChaosTemple() {
		if (Area.BANK.contains(playerX, playerY) &&
			getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		for (final Coordinate coordinate : PATH_TO_CHAOS_TEMPLE) {
			if (playerY > coordinate.getY()) {
				walkTo(coordinate.getX(), coordinate.getY());
				break;
			}
		}

		return SLEEP_ONE_TICK;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Wine of Zamorak", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (winesCollected == 0) return;

		bot.drawString(String.format("@yel@Collected: @whi@%d @cya@(@whi@%s wines@cya@/@whi@hr@cya@)",
				winesCollected, toUnitsPerHour(winesCollected, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Banked: @whi@%d", winesBanked),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final int lawRuneCount = bot.getInventoryStack(lawRuneIndex);

		bot.drawString(String.format("@yel@Law Runes: @whi@%d", lawRuneCount),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(winesCollected, lawRuneCount, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(328, 549), new Coordinate(334, 557));

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
		BANK_DOORS(64, new Coordinate(327, 552));

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
