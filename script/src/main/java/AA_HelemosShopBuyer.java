/**
 * Buys dragon axes from Helemos at the Hero's Guild.
 * Script parameter: # of dragon axes to buy
 * <p>
 * Start script at Falador west bank or at the Hero's Guild.
 * Have at least 200,000 coins in bank and/or inventory.
 * <p>
 *
 * @author Pomch
 */
public class AA_HelemosShopBuyer extends AA_Script {
	private static final Coordinate COORD_LOAD_GATE_NORTH = new Coordinate(326, 544);
	private static final Coordinate COORD_LOAD_GATE_SOUTH = new Coordinate(346, 479);
	private static final Coordinate COORD_LOAD_FALADOR = new Coordinate(324, 512);

	private static final int ITEM_ID_COINS = 10;
	private static final int ITEM_ID_DRAGON_AXE = 594;
	private static final int NPC_ID_HELEMOS = 269;
	private static final int COST_DRAGON_AXE = 200000;
	private static final int MAX_DIST = 18;

	private Coordinate prevCoord;

	private long startTime;
	private long timeout;

	private int axesBought;
	private int coinsSpent;
	private int limit;
	private int remaining;

	private boolean banking;
	private boolean idle;
	private boolean initialized;

	public AA_HelemosShopBuyer(final Extension bot) {
		super(bot);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) {
			throw new IllegalStateException("Empty script parameter. Enter the number of dragon axes to buy.");
		}

		try {
			limit = Integer.parseInt(parameters);
			if (limit <= 0) throw new IllegalStateException("Script parameter must be a positive number.");
			remaining = limit - axesBought;
		} catch (final NumberFormatException e) {
			throw new IllegalStateException("Enter the number of axes to buy as a script parameter.", e);
		}

		final int coinCount = getInventoryItemIdCount(ITEM_ID_COINS);
		banking = coinCount < COST_DRAGON_AXE || (isInventoryFull() && coinCount > COST_DRAGON_AXE);
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
		} else if (message.endsWith("coins")) {
			axesBought++;
			coinsSpent = axesBought * COST_DRAGON_AXE;
			remaining = limit - axesBought;
			if (axesBought >= limit) banking = true;
		} else if (message.startsWith("Helemos") || message.endsWith("here?")) {
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

		bot.drawString("@red@Helemos Shop Buyer", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Spent: @whi@%s gp", DECIMAL_FORMAT.format(coinsSpent)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Bought: @whi@%s @cya@(@whi@%s axes@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(axesBought), toUnitsPerHour(axesBought, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Remaining: @whi@%d axes", remaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(axesBought, remaining, startTime)),
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

				if (itemId != ITEM_ID_COINS) {
					deposit(itemId, getInventoryItemIdCount(itemId));
					return SLEEP_ONE_TICK;
				}
			}

			if (axesBought >= limit) {
				return exit(String.format("Finished buying %d/%d dragon axes.",
					axesBought, limit));
			}

			final int coinCount = getInventoryItemIdCount(ITEM_ID_COINS);
			int reqCoins = Math.min(MAX_INVENTORY_SIZE * COST_DRAGON_AXE, (limit - axesBought) * COST_DRAGON_AXE);

			if (coinCount > reqCoins) {
				if (System.currentTimeMillis() <= timeout) return 0;
				deposit(ITEM_ID_COINS, coinCount - reqCoins);
				timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (coinCount < reqCoins) {
				if (System.currentTimeMillis() <= timeout) return 0;

				final int bankedCoins = (getBankItemIdCount(ITEM_ID_COINS) / COST_DRAGON_AXE) *
					COST_DRAGON_AXE;

				if (bankedCoins < COST_DRAGON_AXE) {
					return exit(String.format("Out of coins. Bought %d/%d dragon axes.",
						axesBought, limit));
				}

				reqCoins = Math.min(reqCoins, bankedCoins);

				if (coinCount < reqCoins) {
					withdraw(ITEM_ID_COINS, reqCoins - coinCount);
					timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
					return 0;
				}
			}

			banking = false;
			closeBank();
			return 0;
		}

		if (ScriptArea.BANK.contains(getPlayerX(), getPlayerY())) return openBank();

		if (ScriptArea.GUILD_ENTRANCE.contains(getPlayerX(), getPlayerY())) {
			if (!isInventoryFull()) {
				final int[] axe = getItemById(ITEM_ID_DRAGON_AXE);

				if (axe[0] != -1 && ScriptArea.GUILD_ENTRANCE.contains(axe[1], axe[2])) {
					pickupItem(ITEM_ID_DRAGON_AXE, axe[1], axe[2]);
					return SLEEP_ONE_TICK;
				}
			}

			final Coordinate guildDoor = ScriptObject.GUILD_DOOR.getCoordinate();
			atWallObject(guildDoor.getX(), guildDoor.getY());
			return SLEEP_ONE_SECOND;
		}

		if (getPlayerX() <= ScriptObject.MEMBERS_GATE.getCoordinate().getX()) {
			if (getPlayerY() < COORD_LOAD_FALADOR.getY()) {
				walkTo(COORD_LOAD_FALADOR.getX(), COORD_LOAD_FALADOR.getY());
				return SLEEP_ONE_TICK;
			}

			final Coordinate bankDoors = ScriptObject.BANK_DOORS.getCoordinate();

			if (distanceTo(bankDoors.getX(), bankDoors.getY()) > MAX_DIST) {
				walkTo(bankDoors.getX(), bankDoors.getY());
				return SLEEP_ONE_TICK;
			}

			if (getObjectId(bankDoors.getX(), bankDoors.getY()) == ScriptObject.BANK_DOORS.getId()) {
				atObject(bankDoors.getX(), bankDoors.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(bankDoors.getX() + 1, bankDoors.getY());
			return SLEEP_ONE_TICK;
		}

		if (getPlayerY() >= COORD_LOAD_GATE_SOUTH.getY()) {
			if (System.currentTimeMillis() <= timeout) return 0;
			final Coordinate membersGate = ScriptObject.MEMBERS_GATE.getCoordinate();
			atObject(membersGate.getX(), membersGate.getY());
			timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		walkTo(COORD_LOAD_GATE_SOUTH.getX(), COORD_LOAD_GATE_SOUTH.getY());
		return SLEEP_ONE_TICK;
	}

	private int buy() {
		if (isShopOpen()) {
			final int coinCount = getInventoryItemIdCount(ITEM_ID_COINS);

			if (coinCount < COST_DRAGON_AXE) {
				banking = true;
				return 0;
			}

			if (getShopItemCount(0) == 0) return 0;

			if (isInventoryFull()) {
				if (coinCount > COST_DRAGON_AXE) {
					banking = true;
					return 0;
				}

				final int axeIdx = getInventoryItemIndex(ITEM_ID_DRAGON_AXE);

				if (axeIdx == -1) {
					banking = true;
					return 0;
				}

				dropItem(axeIdx);
				return SLEEP_ONE_SECOND;
			}

			buyShopItem(0, 1);
			return SLEEP_TWO_SECONDS;
		}

		if (ScriptArea.GUILD_ENTRANCE.contains(getPlayerX(), getPlayerY())) {
			final Object helemos = getNearestNpcNotTalking(NPC_ID_HELEMOS);
			if (helemos == null) return 0;
			return openShop(NPC_ID_HELEMOS);
		}

		if (ScriptArea.BANK.contains(getPlayerX(), getPlayerY())) {
			final Coordinate bankDoors = ScriptObject.BANK_DOORS.getCoordinate();
			if (getObjectId(bankDoors.getX(), bankDoors.getY()) == ScriptObject.BANK_DOORS.getId()) {
				atObject(bankDoors.getX(), bankDoors.getY());
				return SLEEP_ONE_SECOND;
			}
		}

		if (getPlayerX() > ScriptObject.MEMBERS_GATE.getCoordinate().getX()) {
			final Coordinate guildDoor = ScriptObject.GUILD_DOOR.getCoordinate();

			if (distanceTo(guildDoor.getX(), guildDoor.getY()) <= 1) {
				atWallObject(guildDoor.getX(), guildDoor.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(guildDoor.getX(), guildDoor.getY());
			return SLEEP_ONE_TICK;
		}

		if (getPlayerY() <= COORD_LOAD_GATE_NORTH.getY()) {
			final Coordinate membersGate = ScriptObject.MEMBERS_GATE.getCoordinate();

			if (distanceTo(membersGate.getX(), membersGate.getY()) <= 1) {
				if (System.currentTimeMillis() <= timeout) return 0;
				atObject(membersGate.getX(), membersGate.getY());
				timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			walkTo(membersGate.getX(), membersGate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(COORD_LOAD_GATE_NORTH.getX(), COORD_LOAD_GATE_NORTH.getY());
		return SLEEP_ONE_TICK;
	}

	private enum ScriptArea implements RSArea {
		GUILD_ENTRANCE(new Coordinate(368, 434), new Coordinate(377, 440)),
		BANK(new Coordinate(328, 549), new Coordinate(334, 557));

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

	private enum ScriptObject implements RSObject {
		GUILD_DOOR(74, new Coordinate(372, 441)),
		MEMBERS_GATE(137, new Coordinate(341, 487)),
		BANK_DOORS(64, new Coordinate(327, 552));

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
}
