/**
 * Buys rune plate mail legs, skirts, and chains from the Champions Guild.
 * Start script at the Champions Guild with coins in inventory.
 * Enable the ::togglereceipts command in-game.
 * <p>
 * Optional Parameters:
 * -l <#> - Number of legs to buy
 * -c <#> - Number of chains to buy
 * <p>
 *
 * @Author Chomp
 */
public class AA_ScavvoShopBuyer extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_BANK = new Coordinate(156, 530);

	private static final int ITEM_ID_COINS = 10;
	private static final int ITEM_ID_RUNE_CHAIN_MAIL_BODY = 400;
	private static final int ITEM_ID_RUNE_PLATE_MAIL_LEGS = 402;
	private static final int ITEM_ID_RUNE_SKIRT = 406;
	private static final int NPC_ID_SCAVVO = 183;
	private static final int COST_LEGS = 64000;
	private static final int COST_CHAIN = 50000;
	private static final int SHOP_INDEX_SKIRT = 0;
	private static final int SHOP_INDEX_LEGS = 1;
	private static final int SHOP_INDEX_CHAIN = 3;

	private Coordinate prevCoord;

	private long startTime;
	private long buyTimeout;

	private int playerX;
	private int playerY;

	private int legsBought;
	private int chainsBought;
	private int coinsSpent;

	private int legLimit;
	private int chainLimit;

	private boolean buyLegs;
	private boolean buyChains;
	private boolean banking;
	private boolean idle;

	public AA_ScavvoShopBuyer(final Extension bot) {
		super(bot);
	}

	@Override
	public void init(final String parameters) {
		if (isInventoryEmpty() || getInventoryItemId(0) != ITEM_ID_COINS) {
			throw new IllegalStateException("Coins missing from inventory slot 1.");
		}

		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-l":
					case "--legs":
						buyLegs = true;
						legLimit = Integer.parseInt(args[++i]);
						break;
					case "-c":
					case "--chains":
						buyChains = true;
						chainLimit = Integer.parseInt(args[++i]);
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		} else {
			legLimit = chainLimit = Integer.MAX_VALUE;
			buyLegs = buyChains = true;
		}

		banking = isInventoryFull();
		startTime = System.currentTimeMillis();
		bot.displayMessage("@ran@This script requires ::togglereceipts turned on.");
		System.out.printf("[%s] This script requires ::togglereceipts turned on.%n", this);
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
			if (message.startsWith("64000", 10)) {
				if (++legsBought >= legLimit) {
					buyLegs = false;
				}
				coinsSpent += COST_LEGS;
				buyTimeout = 0L;
			} else if (message.startsWith("50000", 10)) {
				if (++chainsBought >= chainLimit) {
					buyChains = false;
				}
				coinsSpent += COST_CHAIN;
				buyTimeout = 0L;
			} else if (message.startsWith("don't", 4)) {
				banking = true;
				return;
			}

			if (!buyLegs && !buyChains) {
				banking = true;
			}
		} else if (message.endsWith("area")) {
			idle = true;
			prevCoord = new Coordinate(getPlayerX(), getPlayerY());
		}
	}

	private int bank() {
		if (isBankOpen()) {
			if (!buyLegs && !buyChains) {
				return exit(String.format("Bought %d legs and %d chains.", legsBought, chainsBought));
			}

			if (isInventoryEmpty() ||
				getInventoryItemId(0) != ITEM_ID_COINS ||
				(buyLegs && getInventoryItemCount(0) < COST_LEGS) ||
				(buyChains && getInventoryItemCount(0) < COST_CHAIN)) {
				return exit("Out of coins.");
			}

			if (getInventoryItemCount() == 1) {
				banking = false;
				return 0;
			}

			deposit(getInventoryItemId(1), MAX_INVENTORY_SIZE);
			return SLEEP_ONE_TICK;
		}

		if (Area.BANK.contains(playerX, playerY)) {
			return openBank();
		}

		if (Area.GUILD.contains(playerX, playerY)) {
			final Coordinate door = Object.DOOR.getCoordinate();
			atWallObject(door.getX(), door.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.UPSTAIRS.contains(playerX, playerY)) {
			final Coordinate stairs = Object.STAIRS_DOWN.getCoordinate();
			atObject(stairs.getX(), stairs.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerY <= COORDINATE_LOAD_BANK.getY()) {
			final Coordinate doors = Object.BANK_DOORS.getCoordinate();

			if (distanceTo(doors.getX(), doors.getY()) <= 1) {
				if (getObjectId(doors.getX(), doors.getY()) == Object.BANK_DOORS.getId()) {
					atObject(doors.getX(), doors.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(doors.getX() + 1, doors.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			walkTo(doors.getX() + 1, doors.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(COORDINATE_LOAD_BANK.getX(), COORDINATE_LOAD_BANK.getY());
		return SLEEP_ONE_TICK;
	}

	private int buy() {
		if (isShopOpen()) {
			if (isInventoryFull()) {
				banking = true;
				return 0;
			}

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

			if (System.currentTimeMillis() <= buyTimeout) {
				return 0;
			}

			if (buyLegs) {
				int stock = getShopItemCount(SHOP_INDEX_SKIRT);

				if (stock > 0) {
					buyShopItem(SHOP_INDEX_SKIRT, 1);
					buyTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
					return 0;
				}

				stock = getShopItemCount(SHOP_INDEX_LEGS);

				if (stock > 0) {
					buyShopItem(SHOP_INDEX_LEGS, 1);
					buyTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
					return 0;
				}
			}

			if (buyChains) {
				final int stock = getShopItemCount(SHOP_INDEX_CHAIN);

				if (stock > 0) {
					buyShopItem(SHOP_INDEX_CHAIN, 1);
					buyTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
					return 0;
				}
			}

			return 0;
		}

		if (Area.UPSTAIRS.contains(playerX, playerY)) {
			return openShop(1, new int[]{NPC_ID_SCAVVO});
		}

		if (Area.GUILD.contains(playerX, playerY)) {
			final Coordinate stairs = Object.STAIRS_UP.getCoordinate();
			atObject(stairs.getX(), stairs.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BANK.contains(playerX, playerY)) {
			final Coordinate doors = Object.BANK_DOORS.getCoordinate();

			if (getObjectId(doors.getX(), doors.getY()) == Object.BANK_DOORS.getId()) {
				atObject(doors.getX(), doors.getY());
				return SLEEP_ONE_SECOND;
			}
		}

		final Coordinate door = Object.DOOR.getCoordinate();

		if (distanceTo(door.getX(), door.getY()) <= 1) {
			atWallObject(door.getX(), door.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(door.getX(), door.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@cya@Scavvo Shop Buyer", PAINT_OFFSET_X, y, 0, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 0, 0);

		bot.drawString(String.format("@yel@Spent: @whi@%s @cya@(@whi@%s gp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(coinsSpent), toUnitsPerHour(coinsSpent, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 0, 0);

		if (legsBought != 0) {
			bot.drawString(String.format("@yel@Legs: @whi@%s @cya@(@whi@%s legs@cya@/@whi@hr@cya@)",
					DECIMAL_FORMAT.format(legsBought), toUnitsPerHour(legsBought, startTime)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 0, 0);
		}

		if (chainsBought != 0) {
			bot.drawString(String.format("@yel@Chains: @whi@%s @cya@(@whi@%s chains@cya@/@whi@hr@cya@)",
					DECIMAL_FORMAT.format(chainsBought), toUnitsPerHour(chainsBought, startTime)),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 0, 0);
		}
	}

	private enum Object implements RSObject {
		STAIRS_UP(41, new Coordinate(151, 558)),
		STAIRS_DOWN(42, new Coordinate(151, 1502)),
		BANK_DOORS(64, new Coordinate(150, 507)),
		DOOR(44, new Coordinate(150, 554));

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
		BANK(new Coordinate(147, 498), new Coordinate(153, 506)),
		GUILD(new Coordinate(148, 554), new Coordinate(152, 562)),
		UPSTAIRS(new Coordinate(148, 1498), new Coordinate(152, 1507));

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
