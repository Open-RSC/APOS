/**
 * Picks flax at the Tree Gnome Stronghold.
 * <p>
 * Requirements:
 * Start at the Tree Gnome Stronghold. If spinning flax have sleeping bag in inventory.
 * The script will spin and drop flax by default.
 * <p>
 * Optional Parameters:
 * -b,--bank (bank bow strings)
 * -n,--no-spin (bank only flax)
 * <p>
 *
 * @Author Chomp
 */
public class AA_GnomeFlax extends AA_Script {
	private static final int ITEM_ID_FLAX = 675;

	private static final int MAXIMUM_SLEEP_WALK_FATIGUE = 85;
	private static final int MAXIMUM_FATIGUE = 100;

	private State state;
	private long startTime;

	private double initialCraftingXp;

	private long depositTimeout;
	private long ladderTimeout;
	private long flaxTimeout;

	private int playerX;
	private int playerY;

	private int flaxPicked;

	private boolean banking;
	private boolean spinning = true;

	public AA_GnomeFlax(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (final String arg : args) {
				switch (arg.toLowerCase()) {
					case "-b":
					case "--bank":
						banking = true;
						break;
					case "-n":
					case "--no-spin":
						spinning = false;
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		if (spinning) {
			if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
				throw new IllegalStateException("Sleeping bag missing from inventory.");
			}
		} else {
			banking = true;
		}

		state = spinning ? State.PICK_TREE : State.PICK_BANK;
		initialCraftingXp = getAccurateXpForLevel(Skill.CRAFTING.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		switch (state) {
			case PICK_TREE:
				return pickTree();
			case PICK_BANK:
				return pickBank();
			case SPIN:
				return spin();
			case DROP:
				return drop();
			case BANK_SOUTH:
				return bankSouth();
			case BANK_NORTH:
				return bankNorth();
			default:
				return exit("Invalid script state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("plant")) {
			flaxPicked++;
			flaxTimeout = 0L;
		} else if (message.endsWith("string")) {
			flaxTimeout = 0L;
		} else if (message.endsWith("ladder")) {
			ladderTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	private int pickTree() {
		if (playerX == Object.FLAX_TREE.coordinate.getX() && playerY == Object.FLAX_TREE.coordinate.getY() + 1) {
			if (getInventoryCount() == MAX_INV_SIZE) {
				state = State.SPIN;
				return 0;
			}

			if (System.currentTimeMillis() <= flaxTimeout) {
				return 0;
			}

			bot.displayMessage("@gre@Picking ...");
			final Coordinate flax = Object.FLAX_TREE.getCoordinate();
			useObject2(flax.getX(), flax.getY());
			flaxTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
			return 0;
		}

		if (Area.TREE.contains(playerX, playerY)) {
			if (System.currentTimeMillis() <= ladderTimeout) {
				return 0;
			}

			final Coordinate ladder = Object.LADDER_DOWN_TREE.getCoordinate();
			useObject1(ladder.getX(), ladder.getY());
			ladderTimeout = System.currentTimeMillis() + SLEEP_TWO_SECONDS;
			return 0;
		}

		if (Area.BANK.contains(playerX, playerY)) {
			final Coordinate ladder = Object.LADDER_DOWN_SOUTH_BANK.getCoordinate();

			if (playerX == ladder.getX() - 1 && playerY == ladder.getY()) {
				if (System.currentTimeMillis() <= ladderTimeout) {
					return 0;
				}

				useObject1(ladder.getX(), ladder.getY());
				ladderTimeout = System.currentTimeMillis() + SLEEP_TWO_SECONDS;
				return 0;
			}

			walkTo(ladder.getX() - 1, ladder.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.FLAX_TREE.coordinate.getX(), Object.FLAX_TREE.coordinate.getY() + 1);

		if (getFatigue() >= MAXIMUM_SLEEP_WALK_FATIGUE && isWalking()) {
			return sleep();
		}

		return SLEEP_ONE_TICK;
	}

	private int pickBank() {
		if (playerX == Object.FLAX_BANK.coordinate.getX() && playerY == Object.FLAX_BANK.coordinate.getY() - 1) {
			if (getInventoryCount() == MAX_INV_SIZE) {
				state = State.BANK_NORTH;
				return 0;
			}

			if (System.currentTimeMillis() <= flaxTimeout) {
				return 0;
			}

			bot.displayMessage("@gre@Picking ...");
			final Coordinate flax = Object.FLAX_BANK.getCoordinate();
			useObject2(flax.getX(), flax.getY());
			flaxTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
			return 0;
		}

		if (Area.BANK.contains(playerX, playerY)) {
			final Coordinate ladder = Object.LADDER_DOWN_NORTH_BANK.getCoordinate();

			if (playerX == ladder.getX() && playerY == ladder.getY() + 1) {
				if (System.currentTimeMillis() <= ladderTimeout) {
					return 0;
				}

				useObject1(ladder.getX(), ladder.getY());
				ladderTimeout = System.currentTimeMillis() + SLEEP_TWO_SECONDS;
				return 0;
			}

			walkTo(Object.LADDER_DOWN_NORTH_BANK.coordinate.getX(), Object.LADDER_DOWN_NORTH_BANK.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.FLAX_BANK.coordinate.getX(), Object.FLAX_BANK.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int spin() {
		if (Area.TREE.contains(playerX, playerY)) {
			if (getInventoryId(1) != ITEM_ID_FLAX) {
				state = banking ? State.BANK_SOUTH : State.DROP;
				return 0;
			}

			if (getFatigue() >= MAXIMUM_FATIGUE) {
				return sleep();
			}

			if (System.currentTimeMillis() <= flaxTimeout) {
				return 0;
			}

			final Coordinate wheel = Object.SPINNING_WHEEL.getCoordinate();

			if (playerX == wheel.getX() - 1 && playerY == wheel.getY()) {
				bot.displayMessage("@gre@Spinning ...");
				useWithObject(1, wheel.getX(), wheel.getY());
				flaxTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
				return 0;
			}

			walkTo(wheel.getX() - 1, wheel.getY());
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= ladderTimeout) {
			return 0;
		}

		final Coordinate ladder = Object.LADDER_UP_TREE.getCoordinate();

		if (playerX == ladder.getX() + 1 && playerY == ladder.getY()) {
			useObject1(ladder.getX(), ladder.getY());
			ladderTimeout = System.currentTimeMillis() + SLEEP_TWO_SECONDS;
			return 0;
		}

		walkTo(ladder.getX() + 1, ladder.getY());
		return SLEEP_ONE_TICK;
	}

	private int drop() {
		if (getInventoryCount() == 1) {
			state = State.PICK_TREE;
			return 0;
		}

		bot.displayMessage("@whi@Dropping ...");
		dropItem(1);
		return SLEEP_ONE_TICK;
	}

	private int bankSouth() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (getInventoryCount() != MAX_INV_SIZE) {
				state = State.PICK_TREE;
				return 0;
			}

			if (!isBanking()) {
				return openBank();
			}

			if (System.currentTimeMillis() <= depositTimeout) {
				return 0;
			}

			final int itemId = getInventoryId(1);
			deposit(itemId, MAX_INV_SIZE);
			depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (Area.TREE.contains(playerX, playerY)) {
			if (System.currentTimeMillis() <= ladderTimeout) {
				return 0;
			}

			final Coordinate ladder = Object.LADDER_DOWN_TREE.getCoordinate();
			useObject1(ladder.getX(), ladder.getY());
			ladderTimeout = System.currentTimeMillis() + SLEEP_TWO_SECONDS;
			return 0;
		}

		final Coordinate ladder = Object.LADDER_UP_SOUTH_BANK.getCoordinate();

		if (playerX == ladder.getX() - 1 && playerY == ladder.getY()) {
			if (System.currentTimeMillis() <= ladderTimeout) {
				return 0;
			}

			useObject1(ladder.getX(), ladder.getY());
			ladderTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		walkTo(ladder.getX() - 1, ladder.getY());
		return SLEEP_ONE_TICK;
	}

	private int bankNorth() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (getInventoryCount() != MAX_INV_SIZE) {
				state = State.PICK_BANK;
				return 0;
			}

			if (!isBanking()) {
				return openBank();
			}

			if (System.currentTimeMillis() <= depositTimeout) {
				return 0;
			}

			final int itemId = getInventoryId(0);
			deposit(itemId, MAX_INV_SIZE);
			depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		final Coordinate ladder = Object.LADDER_UP_NORTH_BANK.getCoordinate();

		if (playerX == ladder.getX() && playerY == ladder.getY() + 1) {
			if (System.currentTimeMillis() <= ladderTimeout) {
				return 0;
			}

			useObject1(ladder.getX(), ladder.getY());
			ladderTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		walkTo(ladder.getX(), ladder.getY() + 1);
		return SLEEP_ONE_TICK;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Gnome Flax", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (spinning) {
			final double xpGained = getAccurateXpForLevel(Skill.CRAFTING.getIndex()) - initialCraftingXp;

			drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
					DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}

		drawString(String.format("@yel@Flax: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				flaxPicked, toUnitsPerHour(flaxPicked, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Mode: @whi@%s", banking ? "Banking" : "Powerflax"),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum State {
		PICK_TREE,
		PICK_BANK,
		SPIN,
		DROP,
		BANK_SOUTH,
		BANK_NORTH
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(712, 1142), new Coordinate(716, 1463)),
		TREE(new Coordinate(691, 1468), new Coordinate(693, 1470));

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
		LADDER_DOWN_SOUTH_BANK(6, new Coordinate(714, 1460)),
		LADDER_UP_SOUTH_BANK(5, new Coordinate(714, 516)),
		LADDER_DOWN_NORTH_BANK(6, new Coordinate(714, 1444)),
		LADDER_UP_NORTH_BANK(5, new Coordinate(714, 500)),
		LADDER_DOWN_TREE(6, new Coordinate(692, 1469)),
		LADDER_UP_TREE(5, new Coordinate(692, 525)),
		SPINNING_WHEEL(121, new Coordinate(694, 1469)),
		FLAX_TREE(313, new Coordinate(693, 524)),
		FLAX_BANK(313, new Coordinate(714, 502));

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
