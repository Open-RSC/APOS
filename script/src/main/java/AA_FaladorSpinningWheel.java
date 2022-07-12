/**
 * Spins wool or flax at the Falador Spinning Wheel.
 * <p>
 * Required:
 * Start at Falador East Bank with an empty inventory.
 * <p>
 * Required Parameter:
 * <wool|flax>
 * <p>
 *
 * @Author Chomp
 */
public class AA_FaladorSpinningWheel extends AA_Script {
	private static final int MAXIMUM_FATIGUE = 100;

	private Item item;
	private long startTime;

	private double initialCraftingXp;

	private long spinTimeout;
	private long withdrawTimeout;
	private long depositTimeout;

	private int playerX;
	private int playerY;

	private int spinCount;
	private int materialRemaining;

	public AA_FaladorSpinningWheel(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) printInstructions();
		item = Item.valueOf(parameters.toUpperCase());
		initialCraftingXp = getAccurateXpForLevel(Skill.CRAFTING.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		if (getInventoryCount() > 0 && getInventoryId(0) == item.id) {
			return spin();
		}

		return bank();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("wool") || message.endsWith("string")) {
			spinCount++;
			if (materialRemaining > 0) {
				materialRemaining--;
			}
			spinTimeout = 0L;
		} else {
			super.onServerMessage(message);
		}
	}

	private int spin() {
		if (Area.HOUSE.contains(playerX, playerY)) {
			if (getFatigue() >= MAXIMUM_FATIGUE) {
				if (playerX != Object.BED.coordinate.getX() - 1 ||
					playerY != Object.BED.coordinate.getY() - 1) {
					walkTo(Object.BED.coordinate.getX() - 1, Object.BED.coordinate.getY() - 1);
					return SLEEP_ONE_TICK;
				}

				final Coordinate bed = Object.BED.getCoordinate();
				useObject1(bed.getX(), bed.getY());
				return SLEEP_ONE_SECOND;
			}

			if (System.currentTimeMillis() <= spinTimeout) {
				return 0;
			}

			final Coordinate wheel = Object.SPINNING_WHEEL.getCoordinate();

			if (playerX != wheel.getX() + 1 || playerY != wheel.getY() - 1) {
				walkTo(wheel.getX() + 1, wheel.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			bot.displayMessage("@gre@Spinning...");
			useWithObject(0, wheel.getX(), wheel.getY());
			spinTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
			return 0;
		}

		if (Area.BANK.contains(playerX, playerY) &&
			getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (getWallObjectIdFromCoords(Object.HOUSE_DOOR.coordinate.getX(), Object.HOUSE_DOOR.coordinate.getY()) == Object.HOUSE_DOOR.id) {
			atWallObject(Object.HOUSE_DOOR.coordinate.getX(), Object.HOUSE_DOOR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(Object.SPINNING_WHEEL.coordinate.getX() + 1, Object.SPINNING_WHEEL.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (!isBanking()) {
				return openBank();
			}

			if (getInventoryCount() == 0) {
				if (System.currentTimeMillis() <= withdrawTimeout) {
					return 0;
				}

				materialRemaining = bankCount(item.id);

				if (materialRemaining == 0) {
					return exit(String.format("Out of %s.", item.name));
				}

				withdraw(item.id, MAX_INV_SIZE);
				withdrawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= depositTimeout) {
				return 0;
			}

			final int itemId = getInventoryId(0);
			deposit(itemId, MAX_INV_SIZE);
			depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (Area.HOUSE.contains(playerX, playerY)) {
			if (getWallObjectIdFromCoords(Object.HOUSE_DOOR.coordinate.getX(), Object.HOUSE_DOOR.coordinate.getY()) == Object.HOUSE_DOOR.id) {
				atWallObject(Object.HOUSE_DOOR.coordinate.getX(), Object.HOUSE_DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		if (getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(Object.BANK_DOORS.coordinate.getX() - 2, Object.BANK_DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Falador Spinning Wheel", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getAccurateXpForLevel(Skill.CRAFTING.getIndex()) - initialCraftingXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@%s: @whi@%s @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				item.productName, spinCount, toUnitsPerHour(spinCount, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Remaining: @whi@%d", materialRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(spinCount, materialRemaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum Item {
		WOOL(145, "Wool", "Balls"),
		FLAX(675, "Flax", "String");

		private final int id;
		private final String name;
		private final String productName;

		Item(final int id, final String name, final String productName) {
			this.id = id;
			this.name = name;
			this.productName = productName;
		}
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(280, 564), new Coordinate(286, 573)),
		HOUSE(new Coordinate(295, 577), new Coordinate(299, 580));

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
		SPINNING_WHEEL(121, new Coordinate(295, 579)),
		BED(15, new Coordinate(298, 579)),
		BANK_DOORS(64, new Coordinate(287, 571)),
		HOUSE_DOOR(2, new Coordinate(297, 577));

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
