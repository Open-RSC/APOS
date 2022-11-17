/**
 * Buys dragon swords from Jakut in Zanaris.
 * Script parameter: # of dragon swords to buy
 * <p>
 * Start script near the bank or the market place.
 * Have at least 100,000 coins and 2 diamonds in bank and/or inventory.
 * <p>
 *
 * @author Pomch
 */
public class AA_JakutShopBuyer extends AA_Script {
	private static final Coordinate[] PATH_MARKETPLACE = new Coordinate[]{
		new Coordinate(144, 3530),
		new Coordinate(117, 3538)
	};

	private static final Coordinate[] PATH_BANK = new Coordinate[]{
		new Coordinate(143, 3527),
		new Coordinate(171, 3527)
	};

	// true -> buy 29 swords/trip (drop/pick-up 1 sword for space/risk losing 100k if there are server issues)
	// false -> buy 28 swords/trip
	private static final boolean MAXIMIZE_TRIPS = true;

	private static final int ITEM_ID_COINS = 10;
	private static final int ITEM_ID_DIAMOND = 161;
	private static final int ITEM_ID_DRAGON_SWORD = 593;
	private static final int NPC_ID_JAKUT = 220;
	private static final int COST_DRAGON_SWORD = 100000;
	private static final int REQUIRED_NUM_DIAMONDS = 2;

	private Coordinate prevCoord;

	private long startTime;
	private long timeout;

	private int swordsBought;
	private int coinsSpent;
	private int limit;
	private int remaining;
	private int maxSwordsPerInv;

	private boolean banking;
	private boolean idle;
	private boolean initialized;

	public AA_JakutShopBuyer(final Extension bot) {
		super(bot);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) {
			throw new IllegalStateException("Empty script parameter. Enter the number of dragon swords to buy.");
		}

		try {
			limit = Integer.parseInt(parameters);
			if (limit <= 0) throw new IllegalStateException("Script parameter must be a positive number.");
			remaining = limit - swordsBought;
		} catch (final NumberFormatException e) {
			throw new IllegalStateException("Enter the number of swords to buy as a script parameter.", e);
		}

		if (ScriptArea.MARKETPLACE.contains(getPlayerX(), getPlayerY()) &&
			!hasInventoryItem(ITEM_ID_DIAMOND)) {
			throw new IllegalStateException("Cannot start script inside the marketplace without a diamond to exit with.");
		}

		maxSwordsPerInv = MAXIMIZE_TRIPS ? MAX_INVENTORY_SIZE - 1 : MAX_INVENTORY_SIZE - 2;
		banking = getInventoryItemIdCount(ITEM_ID_COINS) < COST_DRAGON_SWORD ||
			isInventoryFull() ||
			(!ScriptArea.MARKETPLACE.contains(getPlayerX(), getPlayerY()) &&
				getInventoryItemIdCount(ITEM_ID_DIAMOND) != REQUIRED_NUM_DIAMONDS);
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		return !initialized ? initialize() : idle ? idle() : banking ? bank() : buy();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("enough coins")) {
			banking = true;
		} else if (message.endsWith("with me")) {
			if (ScriptArea.MARKETPLACE.contains(getPlayerX(), getPlayerY())) {
				exit("Stuck in the marketplace without a diamond.");
				return;
			}

			banking = true;
		} else if (message.endsWith("coins")) {
			swordsBought++;
			coinsSpent = swordsBought * COST_DRAGON_SWORD;
			remaining = limit - swordsBought;
			if (swordsBought >= limit) banking = true;
		} else if (message.startsWith("Doorman") || message.endsWith("pay?") || message.equals("Okay")) {
			timeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		} else if (message.endsWith("area")) {
			idle = true;
			prevCoord = new Coordinate(getPlayerX(), getPlayerY());
		} else if (message.startsWith("You will now get receipts")) {
			initialized = true;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@red@Jakut Shop Buyer", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Spent: @whi@%s gp", DECIMAL_FORMAT.format(coinsSpent)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Bought: @whi@%s @cya@(@whi@%s swords@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(swordsBought), toUnitsPerHour(swordsBought, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Remaining: @whi@%d swords", remaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(swordsBought, remaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private int initialize() {
		if (System.currentTimeMillis() <= timeout) return 0;
		setTypeLine("::togglereceipts");
		while (!next()) ;
		timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int idle() {
		if (getPlayerX() != prevCoord.getX() || getPlayerY() != prevCoord.getY()) {
			idle = false;
			return 0;
		}

		final Coordinate walkableCoord = getWalkableCoordinate();

		if (walkableCoord == null) {
			System.err.println("Error handling idle movement. Could not find a tile to walk to.");
			idle = false;
			return 0;
		}

		walkTo(walkableCoord.getX(), walkableCoord.getY());
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (isBankOpen()) {
			for (int idx = 0; idx < getInventoryItemCount(); idx++) {
				final int itemId = getInventoryItemId(idx);

				if (itemId != ITEM_ID_COINS && itemId != ITEM_ID_DIAMOND) {
					deposit(itemId, getInventoryItemIdCount(itemId));
					return SLEEP_ONE_TICK;
				}
			}

			if (swordsBought >= limit) {
				return exit(String.format("Finished buying %d/%d dragon swords.",
					swordsBought, limit));
			}

			final int coinCount = getInventoryItemIdCount(ITEM_ID_COINS);
			int reqCoins = Math.min(maxSwordsPerInv * COST_DRAGON_SWORD, (limit - swordsBought) * COST_DRAGON_SWORD);

			if (coinCount > reqCoins) {
				if (System.currentTimeMillis() <= timeout) return 0;
				deposit(ITEM_ID_COINS, coinCount - reqCoins);
				timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (coinCount < reqCoins) {
				if (System.currentTimeMillis() <= timeout) return 0;

				final int bankedCoins = (getBankItemIdCount(ITEM_ID_COINS) / COST_DRAGON_SWORD) *
					COST_DRAGON_SWORD;

				if (bankedCoins < COST_DRAGON_SWORD) {
					return exit(String.format("Out of coins. Bought %d/%d dragon swords.",
						swordsBought, limit));
				}

				reqCoins = Math.min(reqCoins, bankedCoins);

				if (coinCount < reqCoins) {
					withdraw(ITEM_ID_COINS, reqCoins - coinCount);
					timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
					return 0;
				}
			}

			final int invDiamondCount = getInventoryItemIdCount(ITEM_ID_DIAMOND);

			if (invDiamondCount == REQUIRED_NUM_DIAMONDS) {
				banking = false;
				closeBank();
				return 0;
			}

			if (invDiamondCount > REQUIRED_NUM_DIAMONDS) {
				if (System.currentTimeMillis() <= timeout) return 0;
				deposit(ITEM_ID_DIAMOND, invDiamondCount - REQUIRED_NUM_DIAMONDS);
				timeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
				return 0;
			}

			final int bankDiamondCount = getBankItemIdCount(ITEM_ID_DIAMOND);

			if (bankDiamondCount < REQUIRED_NUM_DIAMONDS - invDiamondCount) {
				return exit(String.format("Out of diamonds. Bought %d/%d dragon swords.",
					swordsBought, limit));
			}

			withdraw(ITEM_ID_DIAMOND, REQUIRED_NUM_DIAMONDS - invDiamondCount);
			return SLEEP_ONE_SECOND;
		}

		if (ScriptArea.BANK.contains(getPlayerX(), getPlayerY())) return openBank();

		if (ScriptArea.MARKETPLACE.contains(getPlayerX(), getPlayerY())) {
			if (isOptionMenuOpen()) {
				answerOptionMenu(0);
				timeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= timeout) return 0;

			if (!isInventoryFull()) {
				final int[] sword = getItemById(ITEM_ID_DRAGON_SWORD);

				if (sword[0] != -1 && ScriptArea.MARKETPLACE.contains(sword[1], sword[2])) {
					pickupItem(ITEM_ID_DRAGON_SWORD, sword[1], sword[2]);
					return SLEEP_ONE_TICK;
				}
			}

			final Coordinate door = ScriptObject.DOOR.getCoordinate();

			atWallObject(door.getX(), door.getY());
			timeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		final Coordinate gate = ScriptObject.GATE.getCoordinate();

		if (getPlayerX() == gate.getX() && getPlayerY() == gate.getY()) {
			if (getObjectId(gate.getX(), gate.getY()) == ScriptObject.GATE.getId()) {
				atObject(gate.getX(), gate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(gate.getX() + 1, gate.getY());
			return SLEEP_ONE_TICK;
		}

		for (final Coordinate coordinate : PATH_BANK) {
			if (getPlayerX() < coordinate.getX()) {
				walkTo(coordinate.getX(), coordinate.getY());
				break;
			}
		}

		return SLEEP_ONE_TICK;
	}

	private int buy() {
		if (isShopOpen()) {
			final int coinCount = getInventoryItemIdCount(ITEM_ID_COINS);

			if (coinCount < COST_DRAGON_SWORD) {
				banking = true;
				return 0;
			}

			if (isInventoryFull()) {
				if (coinCount > COST_DRAGON_SWORD) {
					banking = true;
					return 0;
				}

				final int swordIdx = getInventoryItemIndex(ITEM_ID_DRAGON_SWORD);

				if (swordIdx == -1) {
					banking = true;
					return 0;
				}

				dropItem(swordIdx);
				return SLEEP_ONE_SECOND;
			}

			if (getShopItemCount(0) <= 1) return 0;
			buyShopItem(0, 1);
			return SLEEP_TWO_SECONDS;
		}

		if (ScriptArea.MARKETPLACE.contains(getPlayerX(), getPlayerY())) {
			final Object jakut = getNearestNpcNotTalking(NPC_ID_JAKUT);
			if (jakut == null) return 0;
			return openShop(NPC_ID_JAKUT);
		}

		if (ScriptArea.BANK.contains(getPlayerX(), getPlayerY())) {
			final Coordinate gate = ScriptObject.GATE.getCoordinate();

			if (getObjectId(gate.getX(), gate.getY()) == ScriptObject.GATE.getId()) {
				atObject(gate.getX(), gate.getY());
				return SLEEP_ONE_SECOND;
			}
		}

		final Coordinate door = ScriptObject.DOOR.getCoordinate();

		if (getPlayerX() <= door.getX()) {
			if (isOptionMenuOpen()) {
				answerOptionMenu(0);
				timeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= timeout) return 0;

			atWallObject(door.getX(), door.getY());
			timeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		for (final Coordinate coordinate : PATH_MARKETPLACE) {
			if (getPlayerX() > coordinate.getX()) {
				walkTo(coordinate.getX(), coordinate.getY());
				break;
			}
		}

		return SLEEP_ONE_TICK;
	}

	private enum ScriptObject implements RSObject {
		GATE(57, new Coordinate(171, 3527)),
		DOOR(67, new Coordinate(117, 3539));

		private final int id;
		private final Coordinate coordinate;

		ScriptObject(int id, Coordinate coordinate) {
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

	private enum ScriptArea implements RSArea {
		BANK(new Coordinate(170, 3521), new Coordinate(176, 3525)) {
			@Override
			public boolean contains(final int x, final int y) {
				return super.contains(x, y) || (x >= 172 && x <= 176 && y >= 3526 && y <= 3529);
			}
		},
		MARKETPLACE(new Coordinate(97, 3536), new Coordinate(115, 3550)) {
			@Override
			public boolean contains(final int x, final int y) {
				return super.contains(x, y) || (x >= 116 && x <= 118 && y >= 3539 && y <= 3550);
			}
		};

		private final Coordinate lowerBoundingCoordinate;
		private final Coordinate upperBoundingCoordinate;

		ScriptArea(Coordinate lowerBoundingCoordinate, Coordinate upperBoundingCoordinate) {
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
}
