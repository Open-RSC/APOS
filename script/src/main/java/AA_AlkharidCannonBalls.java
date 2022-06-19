/**
 * Smiths cannonballs at Alkharid furnace.
 * <p>
 * Requirements:
 * Start at Alkharid bank with sleeping bag and cannonball mould in inventory.
 * <p>
 *
 * @Author Chomp
 */
public class AA_AlkharidCannonBalls extends AA_Script {
	private static final int ITEM_ID_CANNON_AMMO_MOULD = 1057;
	private static final int ITEM_ID_STEEL_BAR = 171;
	private static final int ITEM_ID_CANNON_BALL = 1041;

	private static final int INITIAL_INVENTORY_SIZE = 2;

	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;
	private static final int MAXIMUM_SLEEP_WALK_FATIGUE = 80;
	private static final int MAXIMUM_FATIGUE = 99;

	private long startTime;

	private double initialSmithingXp;

	private long depositTimeout;
	private long withdrawTimeout;
	private long smeltTimeout;

	private int inventoryItemCount;

	private int playerX;
	private int playerY;

	private int cannonBallsSmelted;
	private int steelBarsRemaining;

	public AA_AlkharidCannonBalls(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!hasInventoryItem(ITEM_ID_CANNON_AMMO_MOULD)) {
			throw new IllegalStateException("Cannon ammo mould missing from inventory.");
		}

		initialSmithingXp = getSkillExperience(Skill.SMITHING.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getPlayerX();
		playerY = getPlayerY();
		inventoryItemCount = getInventoryItemCount();

		return inventoryItemCount == INITIAL_INVENTORY_SIZE ||
			getInventoryItemId(INITIAL_INVENTORY_SIZE) != ITEM_ID_STEEL_BAR ? bank() : smelt();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("heavy")) {
			cannonBallsSmelted++;

			if (steelBarsRemaining > 0) {
				steelBarsRemaining--;
			}

			smeltTimeout = 0L;
		} else {
			super.onServerMessage(message);
		}
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (!isBankOpen()) {
				return openBank();
			}

			if (inventoryItemCount == INITIAL_INVENTORY_SIZE) {
				if (System.currentTimeMillis() <= withdrawTimeout) {
					return 0;
				}

				steelBarsRemaining = getBankItemIdCount(ITEM_ID_STEEL_BAR);

				if (steelBarsRemaining == 0) {
					return exit("Out of steel bars.");
				}

				withdraw(ITEM_ID_STEEL_BAR, MAX_INVENTORY_SIZE - INITIAL_INVENTORY_SIZE);
				withdrawTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= depositTimeout) {
				return 0;
			}

			deposit(ITEM_ID_CANNON_BALL, MAX_INVENTORY_SIZE - INITIAL_INVENTORY_SIZE);
			depositTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		final Coordinate doors = Object.BANK_DOORS.getCoordinate();

		if (distanceTo(doors.getX(), doors.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			if (getObjectId(doors.getX(), doors.getY()) == Object.BANK_DOORS.id) {
				atObject(doors.getX(), doors.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(doors.getX() + 1, doors.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(doors.getX(), doors.getY());
		return SLEEP_ONE_TICK;
	}

	private int smelt() {
		if (Area.FURNACE.contains(playerX, playerY)) {
			if (getFatiguePercent() >= MAXIMUM_FATIGUE) {
				return sleep();
			}

			if (System.currentTimeMillis() <= smeltTimeout) {
				return 0;
			}

			final Coordinate furnace = Object.FURNACE.getCoordinate();

			if (playerX != furnace.getX() - 1 || playerY != furnace.getY()) {
				walkTo(furnace.getX() - 1, furnace.getY());
				return SLEEP_ONE_TICK;
			}

			bot.displayMessage("@gre@Smelting...");
			useWithObject(INITIAL_INVENTORY_SIZE, Object.FURNACE.getCoordinate().getX(), Object.FURNACE.getCoordinate().getY());
			smeltTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
			return 0;
		}

		final Coordinate doors = Object.BANK_DOORS.getCoordinate();

		if (Area.BANK.contains(playerX, playerY) &&
			getObjectId(doors.getX(), doors.getY()) == Object.BANK_DOORS.id) {
			atObject(doors.getX(), doors.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(Object.FURNACE.coordinate.getX() - 1, Object.FURNACE.coordinate.getY());

		if (getFatiguePercent() >= MAXIMUM_SLEEP_WALK_FATIGUE &&
			isWalking()) {
			return sleep();
		}

		return SLEEP_ONE_TICK;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Alkharid Cannon Balls", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getSkillExperience(Skill.SMITHING.getIndex()) - initialSmithingXp;

		bot.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Cballs: @whi@%d @cya@(@whi@%s balls@cya@/@whi@hr@cya@)",
				cannonBallsSmelted, toUnitsPerHour(cannonBallsSmelted, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Remaining: @whi@%d", steelBarsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(cannonBallsSmelted, steelBarsRemaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(87, 689), new Coordinate(93, 700)),
		FURNACE(new Coordinate(82, 678), new Coordinate(86, 681));

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
		FURNACE(118, new Coordinate(85, 679)),
		BANK_DOORS(64, new Coordinate(86, 695));

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
