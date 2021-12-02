import java.awt.Font;
import java.time.Duration;
import java.time.Instant;

/**
 * Mines gold ore at the Brimhaven quarry.
 * <p>
 * Requirements:
 * Start script at Ardougne south bank with sleeping bag and pickaxe.
 * <p>
 * Author: Chomp
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
	private static final int SKILL_INDEX_MINING = 14;

	private Instant startTime;

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
		if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		boolean missingPickaxe = true;

		for (final int pickaxeId : ITEM_IDS_PICKAXE) {
			if (this.hasInventoryItem(pickaxeId)) {
				missingPickaxe = false;
				break;
			}
		}

		if (missingPickaxe) {
			throw new IllegalStateException("Pickaxe missing from inventory.");
		}

		this.banking = this.getInventoryCount() == MAX_INV_SIZE || !this.hasInventoryItem(ITEM_ID_COINS);
		this.initialMiningXp = this.getAccurateXpForLevel(SKILL_INDEX_MINING);
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		this.playerX = this.getX();
		this.playerY = this.getY();

		return this.banking ? this.bank() : this.mine();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("You only") || message.startsWith("There is")) {
			this.clickTimeout = 0;
		} else if (message.startsWith("You just") || message.endsWith("some gold")) {
			this.oresMined++;
			this.clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("board") || message.endsWith("legal") || message.endsWith("30 gold")) {
			this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Brimhaven Gold Miner", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final double xpGained = this.getAccurateXpForLevel(SKILL_INDEX_MINING) - this.initialMiningXp;

		this.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), getUnitsPerHour(xpGained, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Gold: @whi@%s @cya@(@whi@%s ore@cya@/@whi@hr@cya@)",
				this.oresMined, getUnitsPerHour(this.oresMined, secondsElapsed)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
	}

	private int mine() {
		if (Area.MINE.contains(this.playerX, this.playerY)) {
			if (this.getInventoryCount() == MAX_INV_SIZE) {
				this.banking = true;
				return 0;
			}

			if (this.isItemAt(ITEM_ID_GOLD, COORDINATE_GOLD_ORE_SPAWN.getX(), COORDINATE_GOLD_ORE_SPAWN.getY())) {
				this.pickupItem(ITEM_ID_GOLD, COORDINATE_GOLD_ORE_SPAWN.getX(), COORDINATE_GOLD_ORE_SPAWN.getY());
				return SLEEP_ONE_TICK;
			}

			if (this.getFatigue() >= MAXIMUM_FATIGUE) {
				return this.sleep();
			}

			if (this.clickTimeout != 0 && System.currentTimeMillis() <= this.clickTimeout) {
				return 0;
			}

			final int[] rock = this.getObjectById(OBJECT_ID_ROCK);

			if (rock[0] == -1) {
				return SLEEP_ONE_TICK;
			}

			this.atObject(rock[1], rock[2]);
			this.clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (this.playerY >= COORDINATE_Y_BRIMHAVEN) {
			this.walkTo(Area.MINE.lowerBoundingCoordinate.getX(), Area.MINE.lowerBoundingCoordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.ARDOUGNE_DOCKS.contains(this.playerX, this.playerY)) {
			if (this.isQuestMenu()) {
				this.answer(this.getQuestMenuOption(0).startsWith("Yes") ? 0 : 1);
				this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= this.optionMenuTimeout) {
				return 0;
			}

			final int[] captainBarnaby = this.getNpcByIdNotTalk(NPC_ID_CAPTAIN_BARNABY);

			if (captainBarnaby[0] == -1) {
				return SLEEP_ONE_TICK;
			}

			this.talkToNpc(captainBarnaby[0]);
			this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (Area.BANK.contains(this.playerX, this.playerY) &&
			this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(Area.ARDOUGNE_DOCKS.upperBoundingCoordinate.getX(), Area.ARDOUGNE_DOCKS.upperBoundingCoordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (!this.isBanking()) {
				return this.openBank();
			}

			final int coinsIndex = this.getInventoryIndex(ITEM_ID_COINS);

			if (coinsIndex != -1) {
				this.closeBank();
				this.banking = false;
				return 0;
			}

			if (this.getInventoryCount() == INITIAL_INVENTORY_SIZE) {
				if (this.bankCount(ITEM_ID_COINS) < COIN_COUNT) {
					return this.exit("Out of coins.");
				}

				this.withdraw(ITEM_ID_COINS, COIN_COUNT);
				return SLEEP_ONE_SECOND;
			}

			final int itemId = this.getInventoryId(INITIAL_INVENTORY_SIZE);
			final int itemCount = this.getInventoryCount(itemId);

			this.deposit(itemId, itemCount);
			return SLEEP_ONE_TICK;
		}

		if (this.playerY < COORDINATE_Y_BRIMHAVEN) {
			if (this.distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= 1) {
				if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
					this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				this.walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BRIMHAVEN_DOCKS.contains(this.playerX, this.playerY)) {
			if (this.isQuestMenu()) {
				int index;

				for (final String menuOption : MENU_OPTIONS_CUSTOMS_OFFICIAL) {
					index = this.getMenuIndex(menuOption);

					if (index == -1) {
						continue;
					}

					this.answer(index);
					this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
					return 0;
				}

				return SLEEP_ONE_SECOND;
			}

			if (System.currentTimeMillis() <= this.optionMenuTimeout) {
				return 0;
			}

			final int[] customsOfficial = this.getNpcByIdNotTalk(NPC_ID_CUSTOMS_OFFICIAL);

			if (customsOfficial[0] == -1) {
				return SLEEP_ONE_TICK;
			}

			this.talkToNpc(customsOfficial[0]);
			this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		this.walkTo(Area.BRIMHAVEN_DOCKS.upperBoundingCoordinate.getX(), Area.BRIMHAVEN_DOCKS.upperBoundingCoordinate.getY());

		if (this.getFatigue() != 0 && this.isWalking()) {
			return this.sleep();
		}

		return SLEEP_ONE_TICK;
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
			return this.lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return this.upperBoundingCoordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
		}
	}
}
