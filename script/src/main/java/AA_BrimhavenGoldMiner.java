/**
 * Mines gold ore at the Brimhaven quarry.
 * <p>
 * Requirements:
 * Start script at Ardougne south bank with sleeping bag and pickaxe.
 * <p>
 *
 * @Author Chomp
 */
public class AA_BrimhavenGoldMiner extends AA_Script {
	private static final Coordinate COORDINATE_GOLD_ORE_SPAWN = new Coordinate(495, 659);

	private static final String[] MENU_OPTIONS_CUSTOMS_OFFICIAL = new String[]{"Can I board this ship?", "Search away I have nothing to hide", "Ok"};

	private static final int[] ITEM_IDS_PICKAXE = new int[]{1262, 1261, 1260, 1259, 1258, 156};

	private static final int COORDINATE_Y_BRIMHAVEN = 644;
	private static final int OBJECT_ID_ROCK = 113;
	private static final int ITEM_ID_GOLD = 152;
	private static final int ITEM_ID_COINS = 10;
	private static final int NPC_ID_CAPTAIN_BARNABY = 316;
	private static final int NPC_ID_CUSTOMS_OFFICIAL = 317;
	private static final int MAXIMUM_FATIGUE = 99;
	private static final int INITIAL_INVENTORY_SIZE = 2;
	private static final int COIN_COUNT = 60;

	private long startTime;

	private double initialMiningXp;

	private long clickTimeout;
	private long optionMenuTimeout;

	private int playerX;
	private int playerY;
	private int oresMined;

	private boolean banking;

	public AA_BrimhavenGoldMiner(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!hasInventoryItem(ITEM_IDS_PICKAXE)) {
			throw new IllegalStateException("Pickaxe missing from inventory.");
		}

		banking = getInventoryCount() == MAX_INV_SIZE || !hasInventoryItem(ITEM_ID_COINS);
		initialMiningXp = getAccurateXpForLevel(Skill.MINING.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		return banking ? bank() : mine();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("You only") || message.startsWith("There is")) {
			clickTimeout = 0;
		} else if (message.startsWith("You just") || message.endsWith("some gold")) {
			oresMined++;
			clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("board") || message.endsWith("legal") || message.endsWith("30 gold")) {
			optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		} else {
			super.onServerMessage(message);
		}
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (!isBanking()) {
				return openBank();
			}

			final int coinsIndex = getInventoryIndex(ITEM_ID_COINS);

			if (coinsIndex != -1) {
				closeBank();
				banking = false;
				return 0;
			}

			if (getInventoryCount() == INITIAL_INVENTORY_SIZE) {
				if (bankCount(ITEM_ID_COINS) < COIN_COUNT) {
					return exit("Out of coins.");
				}

				withdraw(ITEM_ID_COINS, COIN_COUNT);
				return SLEEP_ONE_SECOND;
			}

			final int itemId = getInventoryId(INITIAL_INVENTORY_SIZE);
			final int itemCount = getInventoryCount(itemId);

			deposit(itemId, itemCount);
			return SLEEP_ONE_TICK;
		}

		if (playerY < COORDINATE_Y_BRIMHAVEN) {
			if (distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= 1) {
				if (getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
					atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BRIMHAVEN_DOCKS.contains(playerX, playerY)) {
			if (isQuestMenu()) {
				int index;

				for (final String menuOption : MENU_OPTIONS_CUSTOMS_OFFICIAL) {
					index = getMenuIndex(menuOption);

					if (index == -1) {
						continue;
					}

					answer(index);
					optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
					return 0;
				}

				return SLEEP_ONE_SECOND;
			}

			if (System.currentTimeMillis() <= optionMenuTimeout) {
				return 0;
			}

			final int[] customsOfficial = getNpcByIdNotTalk(NPC_ID_CUSTOMS_OFFICIAL);

			if (customsOfficial[0] == -1) {
				return SLEEP_ONE_TICK;
			}

			talkToNpc(customsOfficial[0]);
			optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		walkTo(Area.BRIMHAVEN_DOCKS.upperBoundingCoordinate.getX(), Area.BRIMHAVEN_DOCKS.upperBoundingCoordinate.getY());

		if (getFatigue() != 0 && isWalking()) {
			return sleep();
		}

		return SLEEP_ONE_TICK;
	}

	private int mine() {
		if (Area.MINE.contains(playerX, playerY)) {
			if (getInventoryCount() == MAX_INV_SIZE) {
				banking = true;
				return 0;
			}

			if (isItemAt(ITEM_ID_GOLD, COORDINATE_GOLD_ORE_SPAWN.getX(), COORDINATE_GOLD_ORE_SPAWN.getY())) {
				pickupItem(ITEM_ID_GOLD, COORDINATE_GOLD_ORE_SPAWN.getX(), COORDINATE_GOLD_ORE_SPAWN.getY());
				return SLEEP_ONE_TICK;
			}

			if (getFatigue() >= MAXIMUM_FATIGUE) {
				return sleep();
			}

			if (clickTimeout != 0 && System.currentTimeMillis() <= clickTimeout) {
				return 0;
			}

			final int[] rock = getObjectById(OBJECT_ID_ROCK);

			if (rock[0] == -1) {
				return SLEEP_ONE_TICK;
			}

			atObject(rock[1], rock[2]);
			clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (playerY >= COORDINATE_Y_BRIMHAVEN) {
			walkTo(Area.MINE.lowerBoundingCoordinate.getX(), Area.MINE.lowerBoundingCoordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.ARDOUGNE_DOCKS.contains(playerX, playerY)) {
			if (isQuestMenu()) {
				answer(getQuestMenuOption(0).startsWith("Yes") ? 0 : 1);
				optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= optionMenuTimeout) {
				return 0;
			}

			final int[] captainBarnaby = getNpcByIdNotTalk(NPC_ID_CAPTAIN_BARNABY);

			if (captainBarnaby[0] == -1) {
				return SLEEP_ONE_TICK;
			}

			talkToNpc(captainBarnaby[0]);
			optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (Area.BANK.contains(playerX, playerY) &&
			getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(Area.ARDOUGNE_DOCKS.upperBoundingCoordinate.getX(), Area.ARDOUGNE_DOCKS.upperBoundingCoordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Brimhaven Gold Miner", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getAccurateXpForLevel(Skill.MINING.getIndex()) - initialMiningXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Gold: @whi@%s @cya@(@whi@%s ore@cya@/@whi@hr@cya@)",
				oresMined, toUnitsPerHour(oresMined, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum Area implements RSArea {
		MINE(new Coordinate(491, 655), new Coordinate(498, 662)),
		BANK(new Coordinate(551, 609), new Coordinate(554, 616)),
		BRIMHAVEN_DOCKS(new Coordinate(467, 646), new Coordinate(467, 656)),
		ARDOUGNE_DOCKS(new Coordinate(530, 615), new Coordinate(542, 616));

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
		BANK_DOORS(64, new Coordinate(550, 612));

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
