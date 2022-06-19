/**
 * Buys rune plate mail bodies from Oziach and banks in Edgeville.
 * Start script at Oziach or Edgeville with coins in inventory.
 * <p>
 * Optional Parameter:
 * <#> - max # of plates to buy
 * <p>
 * Notes:
 * Use the ::togglereceipts command to track plates bought and coins spent.
 * <p>
 *
 * @Author Chomp
 */
public class AA_OziachShopBuyer extends AA_Script {
	private static final int ITEM_ID_COINS = 10;
	private static final int ITEM_ID_RUNE_PLATE_MAIL_BODY = 401;
	private static final int NPC_ID_OZIACH = 187;
	private static final int COST = 65000;

	private Coordinate prevCoord;

	private long startTime;

	private int playerX;
	private int playerY;

	private int platesBought;
	private int coinsSpent;

	private int limit;

	private boolean banking;
	private boolean idle;

	public AA_OziachShopBuyer(final Extension bot) {
		super(bot);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			limit = Integer.parseInt(parameters);
		}

		if (isInventoryEmpty() || getInventoryItemId(0) != ITEM_ID_COINS) {
			throw new IllegalStateException("Coins missing from inventory slot 1.");
		}

		banking = isInventoryFull();
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getPlayerX();
		playerY = getPlayerY();

		return banking ? bank() : buy();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("coins")) {
			coinsSpent += COST;
			platesBought++;

			if (limit > 0 && platesBought >= limit) {
				exit("Limit reached.");
			}
		} else if (message.endsWith("area")) {
			idle = true;
			prevCoord = new Coordinate(getPlayerX(), getPlayerY());
		}
	}

	private int bank() {
		if (isBankOpen()) {
			if (getInventoryItemId(0) != ITEM_ID_COINS ||
				getInventoryItemCount(0) < COST) {
				return exit("Out of coins.");
			}

			if (getInventoryItemCount() == 1) {
				banking = false;
				return 0;
			}

			deposit(ITEM_ID_RUNE_PLATE_MAIL_BODY, getInventoryItemCount() - 1);
			return SLEEP_ONE_TICK;
		}

		if (Area.BANK.contains(playerX, playerY)) {
			return openBank();
		}

		if (Area.OZIACH.contains(playerX, playerY)) {
			final Coordinate door = Object.DOOR.getCoordinate();

			if (getWallObjectId(door.getX(), door.getY()) == Object.DOOR.getId()) {
				atWallObject(door.getX(), door.getY());
				return SLEEP_ONE_SECOND;
			}
		}

		final Coordinate doors = Object.BANK_DOORS.getCoordinate();

		if (distanceTo(doors.getX(), doors.getY()) <= 1) {
			if (getObjectId(doors.getX(), doors.getY()) == Object.BANK_DOORS.getId()) {
				atObject(doors.getX(), doors.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(doors.getX() + 1, doors.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		walkTo(doors.getX() + 1, doors.getY());
		return SLEEP_ONE_TICK;
	}

	private int buy() {
		if (bot.isShopVisible()) {
			if (idle) {
				if (getPlayerX() == prevCoord.getX() && getPlayerY() == prevCoord.getY()) {
					final Coordinate coord = getWalkableCoordinate();

					if (coord != null) {
						walkTo(coord.getX(), coord.getY());
						return SLEEP_ONE_TICK;
					}
				}

				idle = false;
			}

			if (isInventoryFull() ||
				getInventoryItemId(0) != ITEM_ID_COINS ||
				getInventoryItemCount(0) < COST) {
				banking = true;
				return 0;
			}

			final int stock = getShopItemCount(0);

			if (stock > 1) {
				buyShopItem(0, 1);
				return SLEEP_TWO_SECONDS;
			}

			return 0;
		}

		final Coordinate door = Object.DOOR.getCoordinate();

		if (distanceTo(door.getX(), door.getY()) <= 5) {
			final java.lang.Object oziach = getNearestNpcNotTalking(NPC_ID_OZIACH);

			if (oziach == null) {
				return 0;
			}

			if (Area.OZIACH.contains(getX(oziach), getY(oziach)) !=
				Area.OZIACH.contains(playerX, playerY) &&
				getWallObjectId(door.getX(), door.getY()) == Object.DOOR.getId()) {
				atWallObject(door.getX(), door.getY());
				return SLEEP_ONE_SECOND;
			}

			return openShop(NPC_ID_OZIACH);
		}

		if (Area.BANK.contains(playerX, playerY)) {
			final Coordinate doors = Object.BANK_DOORS.getCoordinate();

			if (getObjectId(doors.getX(), doors.getY()) == Object.BANK_DOORS.getId()) {
				atObject(doors.getX(), doors.getY());
				return SLEEP_ONE_SECOND;
			}
		}

		walkTo(door.getX() - 1, door.getY());
		return SLEEP_ONE_TICK;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@cya@Oziach Shop Buyer", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Coins: @whi@%s @cya@(@whi@%s gold@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(coinsSpent), toUnitsPerHour(coinsSpent, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Plates: @whi@%s @cya@(@whi@%s bodies@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(platesBought), toUnitsPerHour(platesBought, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum Object implements RSObject {
		BANK_DOORS(64, new Coordinate(217, 447)),
		DOOR(2, new Coordinate(242, 443));

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

	private enum Area implements RSArea {
		BANK(new Coordinate(212, 448), new Coordinate(220, 453)),
		OZIACH(new Coordinate(242, 442), new Coordinate(246, 445));

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

}
