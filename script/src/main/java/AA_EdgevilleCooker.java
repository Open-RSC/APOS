import com.aposbot.Constants;

/**
 * Cooks food in Edgeville using a mule.
 * Start script at range if Master or at Bank if Mule.
 * Inventory: Gauntlets of Cooking (optional)
 * <p>
 * Required Parameters:
 * -f <TUNA|LOBSTER|BASS|SWORDFISH|SHARK> (master/mule)
 * -m <muleRsn> (master only)
 * <p>
 *
 * @Author Chomp
 */
public class AA_EdgevilleCooker extends AA_Script {
	private static final long MAX_TRADE_DURATION = 5000L;

	private static final int MAX_FATIGUE = 99;
	private static final int ITEM_ID_COOKING_GAUNTLETS = 700;

	private Food food;
	private String master;
	private String mule;

	private double initialCookingXp;

	private long startTime;
	private long cookTimeout;
	private long bankDepositTimeout;
	private long tradeRequestTimeout;
	private long tradeTimeout;

	private int foodCooked;
	private int foodBurnt;
	private int foodProcessed;
	private int foodTraded;
	private int foodRemaining;

	private int invSize;

	private boolean trading;

	public AA_EdgevilleCooker(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) printInstructions();

		final String[] args = parameters.split(" ");

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
				case "-f":
					food = Food.valueOf(args[++i].toUpperCase());
					break;
				case "-m":
					mule = args[++i].replace("_", " ");
					break;
				default:
					throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
			}
		}

		if (isMaster()) {
			invSize = !isInventoryEmpty() && getInventoryItemId(0) == ITEM_ID_COOKING_GAUNTLETS ? 1 : 0;
			trading = getInventoryItemCount() == invSize || getInventoryItemId(invSize) != food.rawId;
			initialCookingXp = getSkillExperience(Skill.COOKING.getIndex());
		} else {
			trading = !isInventoryEmpty() && getInventoryItemId(0) == food.rawId;
		}

		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (mule == null) return trading ? trade() : bank();
		return trading ? trade() : cook();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("stove")) {
			foodProcessed++;
		} else if (message.endsWith("cooked")) {
			foodCooked++;
			cookTimeout = 0L;
		} else if (message.startsWith("burn", 17)) {
			foodBurnt++;
			cookTimeout = 0L;
		} else if (message.endsWith("successfully")) {
			tradeTimeout = 0L;
			tradeRequestTimeout = 0L;

			if (mule == null) {
				foodTraded += MAX_TRADE_SIZE;
				foodRemaining -= MAX_TRADE_SIZE;
			}
		} else if (message.startsWith("declined", 17)) {
			tradeTimeout = 0L;
			tradeRequestTimeout = 0L;
		} else if (message.endsWith("objects")) {
			exit("Inventory is krangled.");
		} else {
			super.onServerMessage(message);
		}
	}

	private int trade() {
		if (Area.RANGE.contains(getPlayerX(), getPlayerY())) {
			if (isTradeOpen() && System.currentTimeMillis() > tradeTimeout) {
				declineTrade();
				return SLEEP_ONE_TICK;
			}

			if (bot.isInTradeConfirm()) {
				if (!bot.hasLocalConfirmedTrade()) {
					bot.createPacket(Constants.OP_TRADE_CONFIRM);
					bot.finishPacket();
					bot.Vi = true;
				}

				return 0;
			}

			if (bot.isInTradeOffer()) {
				if (isMaster()) {
					if (bot.getLocalTradeItemCount() == 0 && getInventoryItemCount() > invSize &&
						getInventoryItemId(invSize) != food.rawId) {
						bot.offerItemTrade(invSize, MAX_TRADE_SIZE);
						return SLEEP_ONE_TICK;
					}
				} else {
					if (bot.getLocalTradeItemCount() == 0 && !isInventoryEmpty() &&
						getInventoryItemId(0) == food.rawId) {
						bot.offerItemTrade(0, MAX_TRADE_SIZE);
						return SLEEP_ONE_TICK;
					}
				}

				if (!hasLocalAcceptedTrade()) {
					bot.createPacket(Constants.OP_TRADE_ACCEPT);
					bot.finishPacket();
					bot.Mi = true;
				}

				return 0;
			}

			if (!isMaster()) {
				if (isInventoryEmpty() || getInventoryItemId(0) != food.rawId) trading = false;
				return 0;
			}

			if (getInventoryItemIdCount(food.rawId) == MAX_TRADE_SIZE * 2) {
				trading = false;
				return 0;
			}

			final int lastIndex = getInventoryItemCount() - 1;

			if (lastIndex >= invSize && getInventoryItemId(lastIndex) == food.burntId) {
				dropItem(lastIndex);
				return SLEEP_ONE_TICK;
			}

			if (bot.getPlayerCount() == 1 || System.currentTimeMillis() <= tradeRequestTimeout) return 0;

			final java.lang.Object player = getPlayerFromName(mule);

			if (player == null) return 0;

			return requestTrade(player);
		}

		if (Area.SHOP.contains(getPlayerX(), getPlayerY())) {
			final Coordinate ladder = Object.LADDER_UP.getCoordinate();
			atObject(ladder.getX(), ladder.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BANK.contains(getPlayerX(), getPlayerY())) {
			final Coordinate doors = Object.BANK_DOORS.getCoordinate();
			if (getObjectId(doors.getX(), doors.getY()) == Object.BANK_DOORS.id) {
				atObject(doors.getX(), doors.getY());
				return SLEEP_ONE_SECOND;
			}
		}

		final Coordinate door = Object.DOOR.getCoordinate();

		if (getWallObjectId(door.getX(), door.getY()) == Object.DOOR.id) {
			atWallObject(door.getX(), door.getY());
			return SLEEP_ONE_SECOND;
		}

		final Coordinate ladder = Object.LADDER_UP.getCoordinate();
		walkTo(ladder.getX(), ladder.getY() + 1);
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (Area.BANK.contains(getPlayerX(), getPlayerY())) {
			if (!isBankOpen()) return openBank();

			if (isInventoryEmpty()) {
				if ((foodRemaining = getBankItemIdCount(food.rawId)) < MAX_TRADE_SIZE * 2) {
					return exit("Out of food.");
				}

				withdraw(food.rawId, MAX_TRADE_SIZE * 2);
				return SLEEP_THREE_SECONDS;
			}

			if (getInventoryItemCount() == MAX_TRADE_SIZE * 2 &&
				getInventoryItemIdCount(food.rawId) == MAX_TRADE_SIZE * 2) {
				trading = true;
				return 0;
			}

			if (System.currentTimeMillis() <= bankDepositTimeout) return 0;

			deposit(getInventoryItemId(0), MAX_INVENTORY_SIZE);
			bankDepositTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (Area.SHOP.contains(getPlayerX(), getPlayerY())) {
			final Coordinate door = Object.DOOR.getCoordinate();
			if (getWallObjectId(door.getX(), door.getY()) == Object.DOOR.id) {
				atWallObject(door.getX(), door.getY());
				return SLEEP_ONE_SECOND;
			}
		}

		if (Area.RANGE.contains(getPlayerX(), getPlayerY())) {
			final Coordinate ladder = Object.LADDER_DOWN.getCoordinate();
			atObject(ladder.getX(), ladder.getY());
			return SLEEP_ONE_TICK;
		}

		final Coordinate doors = Object.BANK_DOORS.getCoordinate();
		if (getObjectId(doors.getX(), doors.getY()) == Object.BANK_DOORS.id) {
			atObject(doors.getX(), doors.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(doors.getX() + 1, doors.getY() + 1);
		return SLEEP_ONE_TICK;
	}

	private int cook() {
		if (getInventoryItemCount() == invSize) {
			trading = true;
			return 0;
		}

		final int lastIndex = getInventoryItemCount() - 1;

		if (lastIndex >= invSize && getInventoryItemId(lastIndex) == food.burntId) {
			dropItem(lastIndex);
			return SLEEP_ONE_TICK;
		}

		if (getInventoryItemId(invSize) != food.rawId) {
			trading = true;
			return 0;
		}

		if (getFatiguePercent() >= MAX_FATIGUE) {
			final Coordinate bed = Object.BED.getCoordinate();

			if (getPlayerX() != bed.getX() - 1 || getPlayerY() != bed.getY() + 1) {
				walkTo(bed.getX() - 1, bed.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			bot.createPacket(Constants.OP_OBJECT_ACTION1);
			bot.put2(bed.getX());
			bot.put2(bed.getY());
			bot.finishPacket();

			return SLEEP_ONE_SECOND;
		}

		if (System.currentTimeMillis() <= cookTimeout) return 0;

		final Coordinate range = Object.RANGE.getCoordinate();

		if (getPlayerX() != range.getX() + 1 ||
			getPlayerY() != range.getY() + 2) {
			walkTo(range.getX() + 1, range.getY() + 2);
			return SLEEP_ONE_TICK;
		}

		bot.displayMessage("@red@Cooking ...");

		bot.createPacket(Constants.OP_OBJECT_USEWITH);
		bot.put2(range.getX());
		bot.put2(range.getY());
		bot.put2(invSize);
		bot.finishPacket();

		cookTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	private java.lang.Object getPlayerFromName(final String playerName) {
		java.lang.Object player;

		for (int index = 0; index < bot.getPlayerCount(); index++) {
			player = bot.getPlayer(index);

			if (playerName.equalsIgnoreCase(getName(player))) {
				return player;
			}
		}

		return null;
	}

	private int requestTrade(final java.lang.Object player) {
		if (System.currentTimeMillis() <= tradeRequestTimeout) return 0;
		sendTradeRequest(player);
		tradeTimeout = System.currentTimeMillis() + MAX_TRADE_DURATION;
		tradeRequestTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private void sendTradeRequest(final java.lang.Object player) {
		bot.createPacket(Constants.OP_PLAYER_TRADE);
		bot.put2(bot.getMobServerIndex(player));
		bot.finishPacket();
	}

	private boolean isMaster() {
		return mule != null;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Edgeville Cooker", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (mule == null) {
			bot.drawString(String.format("@yel@Traded: @whi@%d", foodTraded),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

			if (foodRemaining > 0) {
				bot.drawString(String.format("@yel@Remaining: @whi@%d", foodRemaining),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

				bot.drawString(String.format("@yel@Time Remaining: @whi@%s",
						toTimeToCompletion(foodTraded, foodRemaining, startTime)),
					PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
			}
		} else {
			final double xp = getSkillExperience(Skill.COOKING.getIndex());

			final double xpGained = xp - initialCookingXp;

			bot.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
					DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

			bot.drawString(String.format("@yel@Cooked: @whi@%s @cya@(@whi@%s food@cya@/@whi@hr@cya@)",
					foodProcessed, toUnitsPerHour(foodProcessed, startTime)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

			if (foodProcessed > 0) {
				bot.drawString(String.format("@gr1@Success Rate: @whi@%.2f%%", (foodCooked / (float) foodProcessed) * 100),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

				bot.drawString(String.format("Fail Rate: @whi@%.2f%%", (foodBurnt / (float) foodProcessed) * 100),
					PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0xE0142D);
			}
		}
	}

	@Override
	public void onTradeRequest(final String playerName) {
		if (mule != null) return;
		if (master == null) master = playerName;

		if (isInventoryEmpty() || getInventoryItemId(0) != food.rawId || !playerName.equals(master)) {
			return;
		}

		final java.lang.Object player = getPlayerFromName(master);
		if (player != null) requestTrade(player);
	}

	public enum Food {
		TUNA(366, 368, "Tuna"),
		LOBSTER(372, 374, "Lobster"),
		BASS(554, 368, "Bass"),
		SWORDFISH(369, 371, "Swordfish"),
		SHARK(545, 547, "Shark");

		private final int rawId;
		private final int burntId;
		private final String name;

		Food(final int rawId, final int burntId, final String name) {
			this.rawId = rawId;
			this.burntId = burntId;
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private enum Area implements RSArea {
		SHOP(new Coordinate(222, 439), new Coordinate(227, 443)),
		RANGE(new Coordinate(222, 1383), new Coordinate(227, 1387)),
		BANK(new Coordinate(212, 448), new Coordinate(220, 453));

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
		RANGE(11, new Coordinate(222, 1385)),
		BED(15, new Coordinate(226, 1386)),
		LADDER_UP(5, new Coordinate(226, 439)),
		LADDER_DOWN(6, new Coordinate(226, 1383)),
		DOOR(2, new Coordinate(225, 444)),
		BANK_DOORS(64, new Coordinate(217, 447));

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
