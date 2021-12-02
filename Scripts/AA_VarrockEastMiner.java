import com.aposbot.Constants;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;

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
 * Author: Chomp
 */
public class AA_VarrockEastMiner extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_BANK = new Coordinate(77, 544);
	private static final Coordinate COORDINATE_LOAD_QUARRY = new Coordinate(75, 536);

	private static final int SKILL_MINING_INDEX = 14;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;
	private static final int MAXIMUM_FATIGUE = 99;
	private static final int INITIAL_INVENTORY_SIZE = 2;

	private Instant startTime;
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
		if (parameters.isEmpty()) {
			throw new IllegalArgumentException("Empty parameters. Must specify at least ore type.");
		}

		final String[] args = parameters.split(" ");

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
				case "-o":
				case "--ore":
					this.ore = Ore.valueOf(args[++i].toUpperCase());
					break;
				case "-e":
				case "--equal":
					this.equal = true;
					break;
				case "-p":
				case "--power":
				case "--powermine":
					this.powermine = true;
					break;
				default:
					throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
			}
		}

		if (this.ore == Ore.IRON && this.equal) {
			throw new IllegalArgumentException("Error: equal option is invalid with iron ore type.");
		}

		if (this.powermine && this.equal) {
			throw new IllegalArgumentException("Error: equal option is invalid with powermining option.");
		}

		if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		for (final Pickaxe pickaxe : Pickaxe.values()) {
			if (this.hasInventoryItem(pickaxe.id)) {
				this.pickaxe = pickaxe;
				break;
			}
		}

		if (this.pickaxe == null) {
			throw new IllegalStateException("Pickaxe missing from inventory");
		}

		switch (this.ore) {
			case TIN:
				this.rocks = TinRock.values();
				this.adjacentCoordinate = TinRock.COORDINATE_ADJACENT;
				break;
			case COPPER:
				this.rocks = CopperRock.values();
				this.adjacentCoordinate = CopperRock.COORDINATE_ADJACENT;
				break;
			case IRON:
				this.rocks = IronRock.values();
				this.adjacentCoordinate = IronRock.COORDINATE_ADJACENT;
				break;
		}

		this.initialMiningXp = this.getAccurateXpForLevel(SKILL_MINING_INDEX);
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		this.playerX = this.getX();
		this.playerY = this.getY();

		if (this.idle) {
			return this.idle();
		}

		if (this.isBanking() || (this.getInventoryCount() == MAX_INV_SIZE && !this.powermine)) {
			return this.bank();
		}

		return this.mine();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("You only") || message.startsWith("There is")) {
			this.clickTimeout = 0;
		} else if (message.startsWith("You manage") || message.startsWith("You just")) {
			this.oresMined++;
			this.clickTimeout = 0;
			this.previousRock = this.currentRock;
			this.previousRockTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		} else if (message.endsWith("area")) {
			this.idle = true;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Varrock East Miner", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final double xpGained = this.getAccurateXpForLevel(SKILL_MINING_INDEX) - this.initialMiningXp;

		this.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), getUnitsPerHour(xpGained, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@%s: @whi@%s @cya@(@whi@%s ore@cya@/@whi@hr@cya@)",
				this.ore, this.oresMined, getUnitsPerHour(this.oresMined, secondsElapsed)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
	}

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (!this.isBanking()) {
				return this.openBank();
			}

			if (this.getInventoryCount() == INITIAL_INVENTORY_SIZE) {
				if (System.currentTimeMillis() <= this.closeBankTimeout) {
					return 0;
				}

				if (this.equal) {
					this.switchOres();
				}

				this.closeBank();
				this.closeBankTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			final int itemId = this.getInventoryId(INITIAL_INVENTORY_SIZE);
			this.deposit(itemId, MAX_INV_SIZE);
			return SLEEP_ONE_TICK;
		}

		if (this.playerY <= COORDINATE_LOAD_BANK.getY()) {
			if (this.distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
					this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(COORDINATE_LOAD_BANK.getX(), COORDINATE_LOAD_BANK.getY());
		return SLEEP_ONE_TICK;
	}

	private int mine() {
		if (this.playerY >= COORDINATE_LOAD_QUARRY.getY()) {
			if (this.getFatigue() >= MAXIMUM_FATIGUE) {
				return this.sleep();
			}

			if (this.playerX != this.adjacentCoordinate.getX() || this.playerY != this.adjacentCoordinate.getY()) {
				this.walkTo(this.adjacentCoordinate.getX(), this.adjacentCoordinate.getY());
				return SLEEP_ONE_TICK;
			}

			if (this.clickTimeout != 0 && System.currentTimeMillis() <= this.clickTimeout) {
				return 0;
			}

			for (final RSObject rock : this.rocks) {
				if (this.getObjectIdFromCoords(rock.getCoordinate().getX(), rock.getCoordinate().getY()) != rock.getId() ||
					(this.previousRock == rock && System.currentTimeMillis() <= this.previousRockTimeout)) {
					continue;
				}

				this.currentRock = rock;
				this.mineRock(rock);
				this.clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				break;
			}

			return 0;
		}

		if (Area.BANK.contains(this.playerX, this.playerY) &&
			this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(COORDINATE_LOAD_QUARRY.getX(), COORDINATE_LOAD_QUARRY.getY());
		return SLEEP_ONE_TICK;
	}

	private int idle() {
		switch (this.ore) {
			case TIN:
				if (this.playerX == this.adjacentCoordinate.getX() &&
					this.playerY == this.adjacentCoordinate.getY() - 1) {
					this.idle = false;
					return 0;
				}

				this.walkTo(this.adjacentCoordinate.getX(), this.adjacentCoordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			case COPPER:
				if (this.playerX == this.adjacentCoordinate.getX() - 1 &&
					this.playerY == this.adjacentCoordinate.getY()) {
					this.idle = false;
					return 0;
				}

				this.walkTo(this.adjacentCoordinate.getX() - 1, this.adjacentCoordinate.getY());
				return SLEEP_ONE_TICK;
			case IRON:
				if (this.playerX == this.adjacentCoordinate.getX() &&
					this.playerY == this.adjacentCoordinate.getY() + 1) {
					this.idle = false;
					return 0;
				}

				this.walkTo(this.adjacentCoordinate.getX(), this.adjacentCoordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			default:
				throw new IllegalStateException("Invalid ore type");
		}
	}

	private void switchOres() {
		switch (this.ore) {
			case TIN:
				this.ore = Ore.COPPER;
				this.rocks = CopperRock.values();
				this.adjacentCoordinate = CopperRock.COORDINATE_ADJACENT;
				break;
			case COPPER:
				this.ore = Ore.TIN;
				this.rocks = TinRock.values();
				this.adjacentCoordinate = TinRock.COORDINATE_ADJACENT;
				break;
			default:
				throw new IllegalStateException("Switching ores only supported for tin and copper");
		}
	}

	private void mineRock(final RSObject rock) {
		this.extension.createPacket(Constants.OP_OBJECT_ACTION1);
		this.extension.put2(rock.getCoordinate().getX());
		this.extension.put2(rock.getCoordinate().getY());
		this.extension.finishPacket();
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
			return this.name;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
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
			return this.lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return this.upperBoundingCoordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
		}
	}
}
