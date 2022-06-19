/**
 * Fishes at Catherby.
 * Can bank raw fish, cook fish, sell fish to shop, or powerfish.
 * <p>
 * Requirements:
 * Start at Catherby with sleeping bag and fishing tool in inventory.
 * By default the script will fish, cook, and bank sharks.
 * <p>
 * Optional Parameters:
 * -f,--fish <shrimp|tuna|lobster|shark>
 * -m,--mode <bank|cook|sell|powerfish>
 * <p>
 *
 * @Author Chomp
 */
public class AA_CatherbyFisher extends AA_Script {
	private static final Coordinate LOAD_COORD = new Coordinate(416, 499);

	private static final int NPC_ID_HARRY = 250;
	private static final int ITEM_ID_COINS = 10;
	private static final int MAXIMUM_FATIGUE = 99;
	private static final int INIT_INV_SIZE = 2;

	private Coordinate prevCoord;
	private State state;

	private long startTime;
	private long clickTimeout;
	private long doorTimeout;

	private double initFishXp;
	private double initCookXp;

	private int coinsIndex = -1;
	private int initCoinCount;

	private int fishBanked;
	private int fishCaught;
	private int fishCooked;
	private int fishBurnt;

	private int playerX;
	private int playerY;

	private boolean idle;

	private Fish fish = Fish.SHARK;
	private Mode mode = Mode.COOK;

	public AA_CatherbyFisher(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-f":
					case "--fish":
						String fish = args[++i].toUpperCase();
						if (fish.equals("SWORDFISH")) {
							fish = "TUNA";
						} else if (fish.equals("ANCHOVIES")) {
							fish = "SHRIMP";
						}
						this.fish = Fish.valueOf(fish);
						break;
					case "-m":
					case "--mode":
						String mode = args[++i].toUpperCase();
						if (mode.equals("POWER")) {
							mode = "POWERFISH";
						}
						this.mode = Mode.valueOf(mode);
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		if (getInventoryItemId(0) != ITEM_ID_SLEEPING_BAG) {
			throw new IllegalStateException("Sleeping bag missing from inventory slot 1.");
		}

		if (getInventoryItemId(1) != fish.toolId) {
			throw new IllegalStateException(String.format("%s missing from inventory slot 2.",
				getItemName(fish.toolId)));
		}

		if (isInventoryFull()) {
			switch (mode) {
				case BANK:
					state = State.BANK;
					break;
				case COOK:
					state = State.COOK;
					break;
				case SELL:
					state = State.SELL;
					break;
				case POWERFISH:
					state = State.FISH;
					break;
			}
		} else {
			if (Area.RANGE.contains(getPlayerX(), getPlayerY())) {
				state = State.COOK;
			} else {
				state = State.FISH;
			}
		}

		initCoinCount = getInventoryItemIdCount(ITEM_ID_COINS);
		initFishXp = getSkillExperience(Skill.FISHING.getIndex());
		initCookXp = getSkillExperience(Skill.COOKING.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getPlayerX();
		playerY = getPlayerY();

		if (idle) {
			return idle();
		}

		switch (state) {
			case FISH:
				return fish();
			case COOK:
				return cook();
			case SELL:
				return sell();
			case BANK:
				return bank();
			default:
				return exit("Invalid state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("fail", 4)) {
			clickTimeout = 0;
		} else if (message.startsWith("catch", 4)) {
			fishCaught++;
			clickTimeout = 0;
		} else if (message.startsWith("burn", 17)) {
			fishBurnt++;
			clickTimeout = 0;
		} else if (message.endsWith("cooked")) {
			fishCooked++;
			clickTimeout = 0;
		} else if (message.endsWith("shut") || message.endsWith("open")) {
			doorTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("area")) {
			prevCoord = new Coordinate(getPlayerX(), getPlayerY());
			idle = true;
		} else {
			super.onServerMessage(message);
		}
	}

	private int idle() {
		if (playerX != prevCoord.getX() || playerY != prevCoord.getY()) {
			idle = false;
			return 0;
		}

		walkTo(prevCoord.getX(), prevCoord.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int fish() {
		if (isInventoryFull()) {
			if (mode == Mode.BANK) {
				state = State.BANK;
				return 0;
			}

			if (mode != Mode.POWERFISH) {
				state = mode == Mode.COOK ? State.COOK : State.SELL;
				return 0;
			}
		}

		final Coordinate fishSpot = fish.coordinate;

		if (playerX == fishSpot.getX() + 1 && playerY == fishSpot.getY() - 1) {
			if (getFatiguePercent() >= MAXIMUM_FATIGUE) {
				return sleep();
			}

			if (System.currentTimeMillis() <= clickTimeout) {
				return 0;
			}

			if (fish.firstMenuEntry) {
				useObject1(fishSpot.getX(), fishSpot.getY());
			} else {
				useObject2(fishSpot.getX(), fishSpot.getY());
			}

			clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (Area.BANK.contains(playerX, playerY) && isObjectClosed(Object.BANK_DOORS)) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (Area.SHOP.contains(playerX, playerY) && isWallObjectClosed(Object.SHOP_DOOR)) {
			return openWallObject(Object.SHOP_DOOR);
		}

		walkTo(fishSpot.getX() + 1, fishSpot.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int cook() {
		if (Area.RANGE.contains(playerX, playerY)) {
			if (getInventoryItemCount() > INIT_INV_SIZE) {
				final int lastIndex = getInventoryItemCount() - 1;
				final int lastItemId = getInventoryItemId(lastIndex);

				if (inArray(fish.burntIds, lastItemId)) {
					dropItem(lastIndex);
					return SLEEP_ONE_TICK;
				}

				final int firstItemId = getInventoryItemId(INIT_INV_SIZE);

				if (inArray(fish.rawIds, firstItemId)) {
					if (getFatiguePercent() >= MAXIMUM_FATIGUE) {
						return sleep();
					}

					if (System.currentTimeMillis() <= clickTimeout) {
						return 0;
					}

					final Coordinate range = Object.RANGE.getCoordinate();

					if (playerX != range.getX() + 1 || playerY != range.getY() + 2) {
						walkTo(range.getX() + 1, range.getY() + 2);
						return SLEEP_ONE_TICK;
					}

					useWithObject(INIT_INV_SIZE, range.getX(), range.getY());
					clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
					return 0;
				}
			}

			state = State.BANK;
			return 0;
		}

		if (playerX >= LOAD_COORD.getX()) {
			if (isInReach(Object.RANGE_DOOR)) {
				if (isWallObjectClosed(Object.RANGE_DOOR)) {
					return openWallObject(Object.RANGE_DOOR);
				}

				walkTo(Object.RANGE.coordinate.getX() + 1, Object.RANGE.coordinate.getY() + 2);
				return SLEEP_ONE_TICK;
			}

			walkTo(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(LOAD_COORD.getX(), LOAD_COORD.getY());
		return SLEEP_ONE_TICK;
	}

	private int sell() {
		if (Area.SHOP.contains(playerX, playerY)) {
			if (!isShopOpen()) {
				return openShop(NPC_ID_HARRY);
			}

			final int fishIndex = getInventoryItemIndex(fish.rawIds);

			if (fishIndex == -1) {
				coinsIndex = getInventoryItemIndex(ITEM_ID_COINS);
				state = State.FISH;
				return 0;
			}

			final int itemId = getInventoryItemId(fishIndex);
			final int shopIndex = getShopItemIndex(itemId);
			sellShopItem(shopIndex, MAX_INVENTORY_SIZE);
			return SLEEP_ONE_TICK;
		}

		final Coordinate door = Object.SHOP_DOOR.getCoordinate();

		if (isInReach(Object.SHOP_DOOR)) {
			if (isWallObjectClosed(Object.SHOP_DOOR)) {
				return openWallObject(Object.SHOP_DOOR);
			}

			walkTo(door.getX(), door.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		walkTo(door.getX(), door.getY());
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (!isBankOpen()) {
				return openBank();
			}

			if (getInventoryItemCount() == INIT_INV_SIZE) {
				fishBanked = getBankItemIdCount(
					mode != Mode.BANK && mode == Mode.COOK ? fish.cookedIds : fish.rawIds
				);
				state = State.FISH;
				return 0;
			}

			final int itemId = getInventoryItemId(INIT_INV_SIZE);
			deposit(itemId, MAX_INVENTORY_SIZE);
			return SLEEP_ONE_TICK;
		}

		if (Area.RANGE.contains(playerX, playerY)) {
			final int burntIndex = getInventoryItemIndex(fish.burntIds);

			if (burntIndex != -1) {
				dropItem(burntIndex);
				return SLEEP_ONE_TICK;
			}

			if (isWallObjectClosed(Object.RANGE_DOOR)) {
				return openWallObject(Object.RANGE_DOOR);
			}
		}

		if (playerX >= LOAD_COORD.getX()) {
			final Coordinate doors = Object.BANK_DOORS.getCoordinate();

			if (isInReach(Object.BANK_DOORS)) {
				if (isObjectClosed(Object.BANK_DOORS)) {
					atObject(doors.getX(), doors.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(doors.getX(), doors.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			walkTo(doors.getX(), doors.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(LOAD_COORD.getX(), LOAD_COORD.getY());
		return SLEEP_ONE_TICK;
	}

	private boolean isObjectClosed(final Object object) {
		return getObjectId(object.coordinate.getX(), object.coordinate.getY()) == object.id;
	}

	private boolean isWallObjectClosed(final Object wallObject) {
		return getWallObjectId(wallObject.coordinate.getX(), wallObject.coordinate.getY()) == wallObject.id;
	}

	private int openWallObject(final Object wallObject) {
		if (System.currentTimeMillis() <= doorTimeout) {
			return 0;
		}

		atWallObject(wallObject.coordinate.getX(), wallObject.coordinate.getY());
		doorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private boolean isInReach(final Object object) {
		return distanceTo(object.coordinate.getX(), object.coordinate.getY()) <= 1;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Catherby Fisher", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Catching: @gr1@%s", fish),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Mode: @gr1@%s", mode),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double fishingXpGained = getSkillExperience(Skill.FISHING.getIndex()) - initFishXp;

		bot.drawString(String.format("@cya@Fishing Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(fishingXpGained), toUnitsPerHour((int) fishingXpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@cya@Fish Caught: @whi@%s @cya@(@whi@%s fish@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(fishCaught), toUnitsPerHour(fishCaught, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (mode != Mode.BANK) {
			if (mode == Mode.COOK) {
				final double cookingXpGained = getSkillExperience(Skill.COOKING.getIndex()) -
					initCookXp;

				bot.drawString(String.format("@or1@Cooking Xp: @whi@%s @or1@(@whi@%s xp@or1@/@whi@hr@or1@)",
						DECIMAL_FORMAT.format(cookingXpGained), toUnitsPerHour((int) cookingXpGained, startTime)),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

				bot.drawString(String.format("@or1@Fish Cooked: @whi@%s",
						DECIMAL_FORMAT.format(fishCooked)),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

				if (fishBurnt > 0) {
					bot.drawString(String.format("@or1@Fish Burnt: @whi@%s",
							DECIMAL_FORMAT.format(fishBurnt)),
						PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
				}
			} else if (mode == Mode.SELL && coinsIndex != -1) {
				final int coins = getInventoryItemCount(coinsIndex) - initCoinCount;

				bot.drawString(String.format("@yel@Coins: @whi@%s @yel@(@whi@%s gold@yel@/@whi@hr@yel@)",
						DECIMAL_FORMAT.format(coins), toUnitsPerHour(coins, startTime)),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);
			}
		}

		if (fishBanked > 0) {
			bot.drawString(String.format("@gre@Total Banked: @whi@%s",
					DECIMAL_FORMAT.format(fishBanked)),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);
		}
	}

	private enum State {
		FISH,
		COOK,
		SELL,
		BANK
	}

	public enum Mode {
		BANK("Banking"),
		COOK("Cooking"),
		SELL("Selling"),
		POWERFISH("Powerfishing");

		private final String desc;

		Mode(final String desc) {
			this.desc = desc;
		}

		@Override
		public String toString() {
			return desc;
		}
	}

	public enum Fish {
		SHRIMP("Shrimp/Anchovies", new int[]{349, 351}, new int[]{350, 352}, new int[]{353}, 376, new Coordinate(418, 500), true),
		TUNA("Tuna/Swordfish", new int[]{366, 369}, new int[]{367, 370}, new int[]{368, 371}, 379, new Coordinate(409, 504), true),
		LOBSTER("Lobster", new int[]{372}, new int[]{373}, new int[]{374}, 375, new Coordinate(409, 504), false),
		SHARK("Shark", new int[]{545}, new int[]{546}, new int[]{547}, 379, new Coordinate(406, 505), false);

		private final String name;

		private final int[] rawIds;
		private final int[] cookedIds;
		private final int[] burntIds;
		private final int toolId;

		private final Coordinate coordinate;

		private final boolean firstMenuEntry;

		Fish(final String name, final int[] rawIds, final int[] cookedIds, final int[] burntIds, final int toolId, final Coordinate coordinate, final boolean firstMenuEntry) {
			this.name = name;
			this.rawIds = rawIds;
			this.cookedIds = cookedIds;
			this.burntIds = burntIds;
			this.toolId = toolId;
			this.coordinate = coordinate;
			this.firstMenuEntry = firstMenuEntry;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private enum Area implements RSArea {
		RANGE(new Coordinate(432, 480), new Coordinate(436, 485)),
		SHOP(new Coordinate(416, 484), new Coordinate(421, 488)),
		BANK(new Coordinate(437, 491), new Coordinate(443, 496));

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
		RANGE(11, new Coordinate(432, 480)),
		RANGE_DOOR(2, new Coordinate(435, 486)),
		SHOP_DOOR(2, new Coordinate(418, 489)),
		BANK_DOORS(64, new Coordinate(439, 497));

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
