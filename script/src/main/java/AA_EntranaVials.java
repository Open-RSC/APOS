/**
 * Buys, fills, and banks vials from Entrana Herblaw Shop.
 * <p>
 * Requirements:
 * Start script at Entrana with coins in inventory.
 * <p>
 *
 * @Author Chomp
 */
public class AA_EntranaVials extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_PORT_SARIM_TO_BANK = new Coordinate(268, 640);
	private static final Coordinate COORDINATE_LOAD_PORT_SARIM_TO_ENTRANA = new Coordinate(274, 608);
	private static final Coordinate COORDINATE_LOAD_FALADOR = new Coordinate(274, 592);

	private static final int NPC_ID_FRINCOS = 297;
	private static final int ITEM_ID_COINS = 10;
	private static final int ITEM_ID_EMPTY_VIAL = 465;
	private static final int ITEM_ID_WATER_VIAL = 464;
	private static final int SHOP_INDEX_EMPTY_VIAL = 0;
	private static final int MINIMUM_COINS = 5;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;
	private static final int COORDINATE_X_ENTRANA = 396;

	private State state;

	private long startTime;
	private long doorTimeout;
	private long fillVialTimeout;
	private long shipTimeout;
	private long optionMenuTimeout;

	private int playerX;
	private int playerY;
	private int vialCount;

	public AA_EntranaVials(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (getInventoryItemId(0) != ITEM_ID_COINS) {
			throw new IllegalStateException("Coins must be in inventory slot 1.");
		}

		if (getInventoryItemIdCount(ITEM_ID_WATER_VIAL) == MAX_INVENTORY_SIZE - 1) {
			state = State.BANK;
		} else if (hasInventoryItem(ITEM_ID_EMPTY_VIAL)) {
			state = State.FILL;
		} else {
			state = State.BUY;
		}

		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getPlayerX();
		playerY = getPlayerY();

		switch (state) {
			case BUY:
				return buy();
			case FILL:
				return fill();
			case BANK:
				return bank();
			default:
				return exit("Invalid state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("fountain")) {
			vialCount++;
			fillVialTimeout = System.currentTimeMillis() + (TIMEOUT_ONE_TICK * 2);
		} else if (message.endsWith("Entrana") || message.endsWith("Sarim")) {
			shipTimeout = 0L;
			optionMenuTimeout = 0L;
		} else if (message.endsWith("shut") || message.endsWith("open")) {
			doorTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	private int buy() {
		if (playerX >= COORDINATE_X_ENTRANA) {
			if (Area.SHOP.contains(playerX, playerY)) {
				if (!isShopOpen()) {
					return openShop(NPC_ID_FRINCOS);
				}

				if (hasInventoryItem(ITEM_ID_EMPTY_VIAL)) {
					state = State.FILL;
					return 0;
				}

				final int vialCount = getShopItemCount(SHOP_INDEX_EMPTY_VIAL);

				if (vialCount == 0) {
					return 0;
				}

				if (getInventoryItemId(0) != ITEM_ID_COINS ||
					getInventoryItemCount(0) < MINIMUM_COINS) {
					return exit("Out of coins.");
				}

				buyShopItem(SHOP_INDEX_EMPTY_VIAL, getInventoryEmptyCount());
				return SLEEP_TWO_SECONDS;
			}

			if (getWallObjectId(Object.SHOP_DOOR.coordinate.getX(), Object.SHOP_DOOR.coordinate.getY()) ==
				Object.SHOP_DOOR.id) {
				if (System.currentTimeMillis() <= doorTimeout) {
					return 0;
				}

				atWallObject(Object.SHOP_DOOR.coordinate.getX(), Object.SHOP_DOOR.coordinate.getY());
				doorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			walkTo(Object.SHOP_DOOR.coordinate.getX(), Object.SHOP_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BANK.contains(playerX, playerY)) {
			if (getObjectId(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) ==
				Object.BANK_DOORS.id) {
				atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		if (playerY < COORDINATE_LOAD_PORT_SARIM_TO_ENTRANA.getY()) {
			walkTo(COORDINATE_LOAD_PORT_SARIM_TO_ENTRANA.getX(), COORDINATE_LOAD_PORT_SARIM_TO_ENTRANA.getY());
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(Object.SHIP_FROM_PORT_SARIM.coordinate.getX(),
			Object.SHIP_FROM_PORT_SARIM.coordinate.getY()) > 1) {
			walkTo(Object.SHIP_FROM_PORT_SARIM.coordinate.getX(),
				Object.SHIP_FROM_PORT_SARIM.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (isOptionMenuOpen()) {
			answerOptionMenu(1);
			optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= optionMenuTimeout) {
			return 0;
		}

		atObject(Object.SHIP_FROM_PORT_SARIM.coordinate.getX(), Object.SHIP_FROM_PORT_SARIM.coordinate.getY());
		optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
		return 0;
	}

	private int fill() {
		if (playerX == Object.FOUNTAIN.coordinate.getX() && playerY == Object.FOUNTAIN.coordinate.getY() + 2) {
			if (System.currentTimeMillis() <= fillVialTimeout) {
				return 0;
			}

			final int vialIndex = getInventoryItemIndex(ITEM_ID_EMPTY_VIAL);

			if (vialIndex == -1) {
				state = isInventoryFull() ? State.BANK : State.BUY;
				return 0;
			}

			useItemOnObject(vialIndex, Object.FOUNTAIN.coordinate.getX(), Object.FOUNTAIN.coordinate.getY());
			fillVialTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
			return 0;
		}

		if (Area.SHOP.contains(playerX, playerY) &&
			getWallObjectId(Object.SHOP_DOOR.coordinate.getX(), Object.SHOP_DOOR.coordinate.getY()) ==
				Object.SHOP_DOOR.id) {
			if (System.currentTimeMillis() <= doorTimeout) {
				return 0;
			}

			atWallObject(Object.SHOP_DOOR.coordinate.getX(), Object.SHOP_DOOR.coordinate.getY());
			doorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		walkTo(Object.FOUNTAIN.coordinate.getX(), Object.FOUNTAIN.coordinate.getY() + 2);
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (playerX < COORDINATE_X_ENTRANA) {
			if (Area.BANK.contains(playerX, playerY)) {
				if (!isBankOpen()) {
					return openBank();
				}

				if (isInventoryEmpty()) {
					return exit("Out of coins.");
				}

				if (getInventoryItemCount() == 1) {
					state = State.BUY;
					return 0;
				}

				final int itemId = getInventoryItemId(1);
				deposit(itemId, getInventoryItemIdCount(itemId));
				return SLEEP_ONE_SECOND;
			}

			if (playerY > COORDINATE_LOAD_PORT_SARIM_TO_BANK.getY()) {
				walkTo(COORDINATE_LOAD_PORT_SARIM_TO_BANK.getX(), COORDINATE_LOAD_PORT_SARIM_TO_BANK.getY());
				return SLEEP_ONE_TICK;
			}

			if (playerY > COORDINATE_LOAD_FALADOR.getY()) {
				walkTo(COORDINATE_LOAD_FALADOR.getX(), COORDINATE_LOAD_FALADOR.getY());
				return SLEEP_ONE_TICK;
			}

			if (distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) > 1) {
				walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			if (getObjectId(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) ==
				Object.BANK_DOORS.id) {
				atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.BANK_DOORS.coordinate.getX() - 1, Object.BANK_DOORS.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= shipTimeout) {
			return 0;
		}

		atObject(Object.SHIP_FROM_ENTRANA.coordinate.getX(), Object.SHIP_FROM_ENTRANA.coordinate.getY());
		shipTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Entrana Vials", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (vialCount > 0) {
			bot.drawString(String.format("@yel@Vials: @whi@%d @cya@(@whi@%s@cya@ / @whi@hr@cya@)",
					vialCount, toUnitsPerHour(vialCount, startTime)),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);
		}
	}

	private enum State {
		BUY,
		FILL,
		BANK
	}

	private enum Area implements RSArea {
		SHOP(new Coordinate(434, 561), new Coordinate(437, 567)),
		BANK(new Coordinate(280, 564), new Coordinate(286, 573)),
		ENTRANA_DOCK(new Coordinate(413, 567), new Coordinate(423, 570));

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
		SHOP_DOOR(2, new Coordinate(434, 563)),
		FOUNTAIN(26, new Coordinate(428, 563)),
		BANK_DOORS(64, new Coordinate(287, 571)),
		SHIP_FROM_ENTRANA(242, new Coordinate(419, 571)),
		SHIP_FROM_PORT_SARIM(238, new Coordinate(266, 661));

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
