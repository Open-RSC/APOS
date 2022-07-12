/**
 * Mines iron, copper, tin at Varrock East Quarry.
 * <p>
 * Requirements:
 * Start at Varrock East with sleeping bag and pickaxe.
 * <p>
 * Parameters:
 * -o,--ore <iron|tin|copper>
 * -e,--equal
 * -p,--powermine
 * <p>
 *
 * @Author Chomp
 */
public class AA_VarrockEastMiner extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_BANK = new Coordinate(77, 544);
	private static final Coordinate COORDINATE_LOAD_QUARRY = new Coordinate(75, 536);

	private static final int SKILL_MINING_INDEX = 14;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;
	private static final int MAXIMUM_FATIGUE = 99;
	private static final int INITIAL_INVENTORY_SIZE = 2;

	private long startTime;
	private Coordinate adjacentCoordinate;
	private RSObject[] rocks;
	private RSObject currentRock;
	private RSObject previousRock;
	private Pickaxe pickaxe;
	private Ore ore;

	private double initialMiningXp;

	private long clickTimeout;
	private long previousRockTimeout;
	private long closeBankTimeout;

	private int playerX;
	private int playerY;

	private int oresMined;

	private boolean idle;
	private boolean powermine;
	private boolean equal;

	public AA_VarrockEastMiner(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) printInstructions();

		final String[] args = parameters.split(" ");

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
				case "-o":
				case "--ore":
					ore = Ore.valueOf(args[++i].toUpperCase());
					break;
				case "-e":
				case "--equal":
					equal = true;
					break;
				case "-p":
				case "--power":
				case "--powermine":
					powermine = true;
					break;
				default:
					throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
			}
		}

		if (ore == Ore.IRON && equal) {
			throw new IllegalArgumentException("Error: equal option is invalid with iron ore type.");
		}

		if (powermine && equal) {
			throw new IllegalArgumentException("Error: equal option is invalid with powermining option.");
		}

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		for (final Pickaxe pickaxe : Pickaxe.values()) {
			if (hasInventoryItem(pickaxe.id)) {
				this.pickaxe = pickaxe;
				break;
			}
		}

		if (pickaxe == null) {
			throw new IllegalStateException("Pickaxe missing from inventory");
		}

		switch (ore) {
			case TIN:
				rocks = TinRock.values();
				adjacentCoordinate = TinRock.COORDINATE_ADJACENT;
				break;
			case COPPER:
				rocks = CopperRock.values();
				adjacentCoordinate = CopperRock.COORDINATE_ADJACENT;
				break;
			case IRON:
				rocks = IronRock.values();
				adjacentCoordinate = IronRock.COORDINATE_ADJACENT;
				break;
		}

		initialMiningXp = getAccurateXpForLevel(SKILL_MINING_INDEX);
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		if (idle) {
			return idle();
		}

		if (isBanking() || (getInventoryCount() == MAX_INV_SIZE && !powermine)) {
			return bank();
		}

		return mine();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("You only") || message.startsWith("There is")) {
			clickTimeout = 0;
		} else if (message.startsWith("You manage") || message.startsWith("You just")) {
			oresMined++;
			clickTimeout = 0;
			previousRock = currentRock;
			previousRockTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		} else if (message.endsWith("area")) {
			idle = true;
		} else {
			super.onServerMessage(message);
		}
	}

	private int idle() {
		switch (ore) {
			case TIN:
				if (playerX == adjacentCoordinate.getX() &&
					playerY == adjacentCoordinate.getY() - 1) {
					idle = false;
					return 0;
				}

				walkTo(adjacentCoordinate.getX(), adjacentCoordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			case COPPER:
				if (playerX == adjacentCoordinate.getX() - 1 &&
					playerY == adjacentCoordinate.getY()) {
					idle = false;
					return 0;
				}

				walkTo(adjacentCoordinate.getX() - 1, adjacentCoordinate.getY());
				return SLEEP_ONE_TICK;
			case IRON:
				if (playerX == adjacentCoordinate.getX() &&
					playerY == adjacentCoordinate.getY() + 1) {
					idle = false;
					return 0;
				}

				walkTo(adjacentCoordinate.getX(), adjacentCoordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			default:
				throw new IllegalStateException("Invalid ore type");
		}
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (!isBanking()) {
				return openBank();
			}

			if (getInventoryCount() == INITIAL_INVENTORY_SIZE) {
				if (System.currentTimeMillis() <= closeBankTimeout) {
					return 0;
				}

				if (equal) {
					switchOres();
				}

				closeBank();
				closeBankTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			final int itemId = getInventoryId(INITIAL_INVENTORY_SIZE);
			deposit(itemId, MAX_INV_SIZE);
			return SLEEP_ONE_TICK;
		}

		if (playerY <= COORDINATE_LOAD_BANK.getY()) {
			if (distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
					atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());

			if (getFatigue() != 0 && isWalking()) {
				return sleep();
			}

			return SLEEP_ONE_TICK;
		}

		walkTo(COORDINATE_LOAD_BANK.getX(), COORDINATE_LOAD_BANK.getY());
		return SLEEP_ONE_TICK;
	}

	private int mine() {
		if (playerY >= COORDINATE_LOAD_QUARRY.getY()) {
			if (getFatigue() >= MAXIMUM_FATIGUE) {
				return sleep();
			}

			if (playerX != adjacentCoordinate.getX() || playerY != adjacentCoordinate.getY()) {
				walkTo(adjacentCoordinate.getX(), adjacentCoordinate.getY());
				return SLEEP_ONE_TICK;
			}

			if (clickTimeout != 0 && System.currentTimeMillis() <= clickTimeout) {
				return 0;
			}

			for (final RSObject rock : rocks) {
				if (getObjectIdFromCoords(rock.getCoordinate().getX(), rock.getCoordinate().getY()) != rock.getId() ||
					(previousRock == rock && System.currentTimeMillis() <= previousRockTimeout)) {
					continue;
				}

				currentRock = rock;
				final Coordinate coord = rock.getCoordinate();
				useObject1(coord.getX(), coord.getY());
				clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				break;
			}

			return 0;
		}

		if (Area.BANK.contains(playerX, playerY) &&
			getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(COORDINATE_LOAD_QUARRY.getX(), COORDINATE_LOAD_QUARRY.getY());
		return SLEEP_ONE_TICK;
	}

	private void switchOres() {
		switch (ore) {
			case TIN:
				ore = Ore.COPPER;
				rocks = CopperRock.values();
				adjacentCoordinate = CopperRock.COORDINATE_ADJACENT;
				break;
			case COPPER:
				ore = Ore.TIN;
				rocks = TinRock.values();
				adjacentCoordinate = TinRock.COORDINATE_ADJACENT;
				break;
			default:
				throw new IllegalStateException("Switching ores only supported for tin and copper");
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Varrock East Miner", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getAccurateXpForLevel(SKILL_MINING_INDEX) - initialMiningXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@%s: @whi@%s @cya@(@whi@%s ore@cya@/@whi@hr@cya@)",
				ore, oresMined, toUnitsPerHour(oresMined, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
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

	private enum Ore {
		TIN("Tin"),
		COPPER("Copper"),
		IRON("Iron");

		private final String name;

		Ore(final String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private enum IronRock implements RSObject {
		NORTH(102, new Coordinate(75, 543)),
		NORTH_WEST(102, new Coordinate(76, 543)),
		WEST(103, new Coordinate(76, 544));

		private static final Coordinate COORDINATE_ADJACENT = new Coordinate(75, 544);
		private final int id;
		private final Coordinate coordinate;

		IronRock(final int id, final Coordinate coordinate) {
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

	private enum TinRock implements RSObject {
		EAST(105, new Coordinate(78, 545)),
		SOUTH_EAST(105, new Coordinate(78, 546)),
		SOUTH(105, new Coordinate(79, 546));

		private static final Coordinate COORDINATE_ADJACENT = new Coordinate(79, 545);
		private final int id;
		private final Coordinate coordinate;

		TinRock(final int id, final Coordinate coordinate) {
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

	private enum CopperRock implements RSObject {
		NORTH(101, new Coordinate(73, 547)),
		EAST(101, new Coordinate(72, 549)),
		SOUTH(101, new Coordinate(73, 549)),
		WEST(101, new Coordinate(74, 549));

		private static final Coordinate COORDINATE_ADJACENT = new Coordinate(73, 548);
		private final int id;
		private final Coordinate coordinate;

		CopperRock(final int id, final Coordinate coordinate) {
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

	private enum Area implements RSArea {
		BANK(new Coordinate(98, 510), new Coordinate(106, 515));

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
		BANK_DOORS(64, new Coordinate(102, 509));

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
