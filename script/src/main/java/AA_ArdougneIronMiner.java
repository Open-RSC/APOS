import java.awt.*;

/**
 * Mines iron ore south of Ardougne and banks at the south bank.
 * Start script at the mine or at the bank.
 * <p>
 * Parameters:
 * p - enable powermining (no banking)
 * l - enable looting iron ore off the ground
 * s - use the south spot (3 rocks)
 * <p>
 *
 * @Author Chomp
 */
public class AA_ArdougneIronMiner extends AA_Script {
	private static final Coordinate COORDINATE_LOAD = new Coordinate(576, 624);

	private static final int[] ITEM_IDS_PICKAXE = new int[]{1262, 1261, 1260, 1259, 1258, 156};

	private static final int ITEM_ID_IRON_ORE = 151;
	private static final int MAX_FATIGUE = 99;
	private static final int MAX_DIST = 12;
	private static final int INIT_INV_SIZE = 2;

	private RSObject[] rocks;
	private Coordinate standCoord;

	private RSObject currentRock;
	private RSObject previousRock;

	private long startTime;

	private double initialMiningXp;
	private double swingAttempts;

	private long mineTimeout;
	private long depositTimeout;
	private long previousRockTimeout;

	private final int[] ironOre = new int[]{-1, -1};

	private int playerX;
	private int playerY;

	private int success;
	private int fail;

	private boolean idle;
	private boolean powerMining;
	private boolean looting;
	private boolean banking;

	public AA_ArdougneIronMiner(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (getInventoryIndex(ITEM_ID_SLEEPING_BAG) != 0) {
			throw new IllegalStateException("Sleeping bag missing from inventory slot 1.");
		}

		if (bot.getInventorySize() < INIT_INV_SIZE || !inArray(ITEM_IDS_PICKAXE, bot.getInventoryId(1))) {
			throw new IllegalStateException("Pickaxe missing from inventory slot 2.");
		}

		if (parameters.contains("s")) {
			rocks = SouthRock.values();
			standCoord = SouthRock.COORDINATE_STAND;
		} else {
			rocks = NorthRock.values();
			standCoord = NorthRock.COORDINATE_STAND;
		}

		looting = parameters.contains("l");
		banking = !(powerMining = parameters.contains("p")) && isInventoryFull();
		initialMiningXp = bot.getExperience(Skill.MINING.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		return banking ? bank() : mine();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("swing", 4)) {
			swingAttempts++;
		} else if (message.startsWith("scratching", 20)) {
			fail++;
			mineTimeout = 0L;
		} else if (message.startsWith("no ore", 19)) {
			fail++;
			mineTimeout = 0L;
			previousRock = currentRock;
			previousRockTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		} else if (message.endsWith("ore")) {
			success++;
			mineTimeout = 0L;
			previousRock = currentRock;
			previousRockTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		} else if (message.startsWith("found", 9)) {
			success++;
			mineTimeout = 0L;
		} else if (message.endsWith("area")) {
			idle = true;
		} else {
			super.onServerMessage(message);
		}
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (bot.getInventorySize() == INIT_INV_SIZE) {
				banking = false;
				return 0;
			}

			if (!bot.isBankVisible()) {
				return openBank();
			}

			for (final UncutGem uncutGem : UncutGem.VALUES) {
				if (!hasInventoryItem(uncutGem.id)) {
					continue;
				}

				if (System.currentTimeMillis() <= uncutGem.depositTimeout) {
					return 0;
				}

				deposit(uncutGem.id, MAX_INV_SIZE);
				uncutGem.depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= depositTimeout) {
				return 0;
			}

			deposit(bot.getInventoryId(INIT_INV_SIZE), MAX_INV_SIZE);
			depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (playerY <= COORDINATE_LOAD.getY()) {
			final Coordinate doors = Object.BANK_DOORS.getCoordinate();

			if (distanceTo(doors.getX(), doors.getY()) <= MAX_DIST) {
				if (isPresent(Object.BANK_DOORS)) {
					atObject(doors.getX(), doors.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(doors.getX() + 1, doors.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			walkTo(doors.getX(), doors.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(COORDINATE_LOAD.getX(), COORDINATE_LOAD.getY());
		return SLEEP_ONE_TICK;
	}

	private int mine() {
		if (playerY >= COORDINATE_LOAD.getY()) {
			if (!powerMining && isInventoryFull()) {
				banking = true;
				return 0;
			}

			if (idle) {
				return idle();
			}

			if (looting && !isInventoryFull()) {
				updateNearestIronOre();

				if (ironOre[0] != -1) {
					pickupItem(ITEM_ID_IRON_ORE, ironOre[0], ironOre[1]);
					return SLEEP_ONE_TICK;
				}
			}

			if (playerX != standCoord.getX() || playerY != standCoord.getY()) {
				walkTo(standCoord.getX(), standCoord.getY());
				return SLEEP_ONE_TICK;
			}

			if (getFatigue() >= MAX_FATIGUE) {
				if (bot.isHeadIconVisible(bot.getPlayer())) {
					return SLEEP_ONE_TICK;
				}

				return sleep();
			}

			if (mineTimeout != 0L && System.currentTimeMillis() <= mineTimeout) {
				return 0;
			}

			for (final RSObject rock : rocks) {
				if (!isPresent(rock) || (previousRock == rock && System.currentTimeMillis() <= previousRockTimeout)) {
					continue;
				}

				currentRock = rock;
				useObject1(rock.getCoordinate().getX(), rock.getCoordinate().getY());
				mineTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				bot.displayMessage("@dre@Swing ...");
				break;
			}

			return 0;
		}

		if (Area.BANK.contains(playerX, playerY) && isPresent(Object.BANK_DOORS)) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(COORDINATE_LOAD.getX(), COORDINATE_LOAD.getY());
		return SLEEP_ONE_TICK;
	}

	private boolean isPresent(final RSObject o) {
		return getObjectIdFromCoords(o.getCoordinate().getX(), o.getCoordinate().getY()) == o.getId();
	}

	private int idle() {
		if (playerX != standCoord.getX() || playerY != standCoord.getY()) {
			idle = false;
			return 0;
		}

		walkTo(standCoord.getX(), standCoord.getY() - 3);
		return SLEEP_ONE_TICK;
	}

	private void updateNearestIronOre() {
		ironOre[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int i = 0; i < bot.getGroundItemCount(); i++) {
			final int itemId = bot.getGroundItemId(i);

			if (itemId != ITEM_ID_IRON_ORE) {
				continue;
			}

			final int x = getItemX(i);
			final int y = getItemY(i);

			if (!Area.MINE.contains(x, y)) {
				continue;
			}

			final int distance = distanceTo(x, y);

			if (currentDistance <= distance) {
				continue;
			}

			currentDistance = distance;
			ironOre[0] = x;
			ironOre[1] = y;
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Ardougne Iron Miner", PAINT_OFFSET_X, y, 1, 0);
		bot.drawString(String.format("@yel@State: @whi@%s", powerMining ? "Powermining" : banking ? "Banking" : "Mining"),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = bot.getExperience(Skill.MINING.getIndex()) - initialMiningXp;

		bot.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/ @whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Iron ore: @whi@%s @cya@(@whi@%s ores@cya@/ @whi@hr@cya@)",
				success, toUnitsPerHour(success, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (swingAttempts > 0) {
			bot.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

			bot.drawString(String.format("@gr1@Success Rate: @whi@%.2f%%", (success / swingAttempts) * 100),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

			bot.drawString(String.format("Fail Rate: @whi@%.2f%%", (fail / swingAttempts) * 100),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0xE0142D);
		}
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(551, 609), new Coordinate(554, 616)),
		MINE(new Coordinate(615, 654), new Coordinate(620, 659));

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
		BANK_DOORS(64, new Coordinate(550, 612));

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

	private enum NorthRock implements RSObject {
		NORTH_WEST(102, new Coordinate(618, 655)),
		NORTH(102, new Coordinate(617, 655)),
		EAST(102, new Coordinate(616, 656)),
		SOUTH_EAST(102, new Coordinate(616, 657));

		private static final Coordinate COORDINATE_STAND = new Coordinate(617, 656);

		private final int id;
		private final Coordinate coordinate;

		NorthRock(final int id, final Coordinate coordinate) {
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

	private enum SouthRock implements RSObject {
		WEST(102, new Coordinate(619, 657)),
		SOUTH(102, new Coordinate(618, 658)),
		SOUTH_EAST(102, new Coordinate(617, 658));

		private static final Coordinate COORDINATE_STAND = new Coordinate(618, 657);

		private final int id;
		private final Coordinate coordinate;

		SouthRock(final int id, final Coordinate coordinate) {
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

	private enum UncutGem {
		SAPPHIRE(160),
		EMERALD(159),
		RUBY(158),
		DIAMOND(157);

		private static final UncutGem[] VALUES = UncutGem.values();

		private final int id;
		private long depositTimeout;

		UncutGem(final int id) {
			this.id = id;
		}
	}
}
