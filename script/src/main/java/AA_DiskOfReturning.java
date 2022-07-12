/**
 * Buys Disks of Returning from Thordur.
 * Banks at Falador East Bank.
 * <p>
 * Required:
 * Start at Falador East Bank or at Thordur.
 * Inventory: Staff of Air (equipped), Water Runes, Law Runes, Coins.
 * <p>
 *
 * @Author Chomp
 */
public class AA_DiskOfReturning extends AA_Script {
	private static final String[] DIALOG = new String[]{
		"Who are you?",
		"So what do you do",
		"Sounds good, how can I visit it?",
		"So about this disk, can I buy it?",
		"Yes please"
	};

	private static final Coordinate[] PATH_TO_THORDUR = new Coordinate[]{
		new Coordinate(284, 3347),
		new Coordinate(309, 3347)
	};

	private static final Coordinate COORDINATE_LOAD_BANK = new Coordinate(304, 552);

	private static final int[] REQUIRED_ITEM_IDS = new int[]{10, 32, 42, 101};

	private static final int COORDINATE_Y_DWARVEN_MINES = 3000;

	private static final int NPC_ID_THORDUR = 175;
	private static final int ITEM_ID_DISK_OF_RETURNING = 387;
	private static final int SPELL_ID_FALADOR_TELEPORT = 18;
	private static final int MINIMUM_MAGIC_LVL = 37;

	private long startTime;

	private long timeout;

	private int playerX;
	private int playerY;
	private int previousPlayerX;

	private int disksBought;
	private int disksBanked;

	private boolean idle;
	private boolean banking;

	public AA_DiskOfReturning(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (getLevel(Skill.MAGIC.getIndex()) < MINIMUM_MAGIC_LVL) {
			throw new IllegalStateException(String.format("L%d magic level required.", MINIMUM_MAGIC_LVL));
		}

		for (final int itemId : REQUIRED_ITEM_IDS) {
			if (!hasInventoryItem(itemId)) {
				throw new IllegalStateException("Missing item: " + getItemNameId(itemId));
			}
		}

		banking = getInventoryCount() == MAX_INV_SIZE;
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		return banking ? bank() : buyDisks();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("disk")) {
			disksBought++;
		} else if (message.endsWith("area")) {
			previousPlayerX = getX();
			idle = true;
		} else if (message.endsWith("money")) {
			exit("Out of coins.");
		} else if (message.endsWith("spell")) {
			exit("Out of runes.");
		}
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (!isBanking()) {
				return openBank();
			}

			if (hasInventoryItem(ITEM_ID_DISK_OF_RETURNING)) {
				deposit(ITEM_ID_DISK_OF_RETURNING, MAX_INV_SIZE);
				return SLEEP_ONE_TICK;
			}

			disksBanked = bankCount(ITEM_ID_DISK_OF_RETURNING);
			banking = false;
			return 0;
		}

		if (!inDwarvenMines()) {
			if (playerX > COORDINATE_LOAD_BANK.getX()) {
				walkTo(COORDINATE_LOAD_BANK.getX(), COORDINATE_LOAD_BANK.getY());
				return SLEEP_ONE_TICK;
			}

			if (!isWithinReach(Object.BANK_DOORS)) {
				walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			if (isObjectClosed(Object.BANK_DOORS)) {
				atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.BANK_DOORS.coordinate.getX() - 1, Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		castOnSelf(SPELL_ID_FALADOR_TELEPORT);
		return SLEEP_ONE_SECOND;
	}

	private int buyDisks() {
		if (Area.THORDUR.contains(playerX, playerY)) {
			if (getInventoryCount() == MAX_INV_SIZE) {
				banking = true;
				return 0;
			}

			return talkToThordur();
		}

		if (inDwarvenMines()) {
			for (final Coordinate coordinate : PATH_TO_THORDUR) {
				if (playerX < coordinate.getX()) {
					walkTo(coordinate.getX(), coordinate.getY());
					break;
				}
			}

			return SLEEP_ONE_TICK;
		}

		if (Area.BUILDING.contains(playerX, playerY)) {
			atObject(Object.STAIRS.coordinate.getX(), Object.STAIRS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (Area.BANK.contains(playerX, playerY) && isObjectClosed(Object.BANK_DOORS)) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (isWithinReach(Object.DOOR)) {
			if (isWallObjectClosed(Object.DOOR)) {
				atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private boolean inDwarvenMines() {
		return playerY >= COORDINATE_Y_DWARVEN_MINES;
	}

	private boolean isWithinReach(final Object object) {
		return distanceTo(object.coordinate.getX(), object.coordinate.getY()) <= 1;
	}

	private boolean isObjectClosed(final Object object) {
		return getObjectIdFromCoords(object.coordinate.getX(), object.coordinate.getY()) == object.id;
	}

	private int talkToThordur() {
		if (idle) {
			return idle();
		}

		if (isQuestMenu()) {
			int index;

			for (final String dialog : DIALOG) {
				index = getMenuIndex(dialog);

				if (index == -1) {
					continue;
				}

				answer(index);
				timeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
				return 0;
			}

			return SLEEP_ONE_SECOND;
		}

		if (System.currentTimeMillis() <= timeout) {
			return 0;
		}

		final int[] thordur = getNpcById(NPC_ID_THORDUR);

		if (thordur[0] == -1) {
			return 0;
		}

		talkToNpc(thordur[0]);
		timeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private boolean isWallObjectClosed(final Object wallObject) {
		return getWallObjectIdFromCoords(wallObject.coordinate.getX(), wallObject.coordinate.getY()) == wallObject.id;
	}

	private int idle() {
		final int playerX = getX();

		if (playerX != previousPlayerX) {
			idle = false;
			return 0;
		}

		walkTo(playerX - 1, getY());
		return SLEEP_ONE_TICK;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Disk of Returning", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Bought: @whi@%s @cya@(@whi@%s disks@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(disksBought), toUnitsPerHour(disksBought, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		if (disksBanked <= 0) return;

		drawString(String.format("@gre@Total Banked: @whi@%s", DECIMAL_FORMAT.format(disksBanked)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	@Override
	public void onChatMessage(final String message, final String playerName, final boolean moderator, final boolean administrator) {
		if (message.endsWith("here")) {
			timeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		}
	}

	private enum Area implements RSArea {
		THORDUR(new Coordinate(308, 3343), new Coordinate(314, 3351)),
		BUILDING(new Coordinate(249, 535), new Coordinate(253, 540)),
		BANK(new Coordinate(280, 564), new Coordinate(286, 573));

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
		DOOR(2, new Coordinate(252, 541)),
		STAIRS(44, new Coordinate(251, 537)),
		BANK_DOORS(64, new Coordinate(287, 571));

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
