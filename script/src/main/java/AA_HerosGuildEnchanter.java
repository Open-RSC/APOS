/**
 * Enchants dragonstone amulets at the Hero's Guild and banks at Falador West Bank.
 * <p>
 * Required:
 * Start script at Falador West Bank.
 * Have staff of air equipped. Have law runes/water runes/amulets banked.
 * <p>
 *
 * @Author Chomp
 */
public class AA_HerosGuildEnchanter extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_MEMBERS_GATE = new Coordinate(326, 544);

	private static final int ITEM_ID_STAFF_OF_AIR = 101;
	private static final int ITEM_ID_WATER_RUNE = 32;
	private static final int ITEM_ID_LAW_RUNE = 42;
	private static final int ITEM_ID_DRAGONSTONE_AMULET = 522;
	private static final int ITEM_ID_CHARGED_DRAGONSTONE_AMULET = 597;

	private static final int SPELL_ID_FALADOR_TELEPORT = 18;
	private static final int QUEST_ID_HEROS = 19;
	private static final int MINIMUM_MAGIC_LVL = 37;

	private long startTime;

	private long timeout;
	private long depositTimeout;
	private long withdrawAmuletTimeout;
	private long withdrawLawTimeout;
	private long withdrawWaterTimeout;

	private int playerX;
	private int playerY;

	private int amuletsEnchanted;
	private int amuletsRemaining;

	private boolean banking;

	public AA_HerosGuildEnchanter(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!isQuestComplete(QUEST_ID_HEROS)) {
			throw new IllegalStateException("Must have Hero's Quest completed to run this script.");
		}

		if (getLevel(Skill.MAGIC.getIndex()) < MINIMUM_MAGIC_LVL) {
			throw new IllegalStateException(String.format("Must have at least L%d magic to run this script.",
				MINIMUM_MAGIC_LVL));
		}

		final int staffIndex = getInventoryIndex(ITEM_ID_STAFF_OF_AIR);

		if (staffIndex == -1 || !isItemEquipped(staffIndex)) {
			throw new IllegalStateException("Staff of air missing/unequipped.");
		}

		banking = !hasInventoryItem(ITEM_ID_DRAGONSTONE_AMULET) ||
			!hasInventoryItem(ITEM_ID_LAW_RUNE) ||
			!hasInventoryItem(ITEM_ID_WATER_RUNE);
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		return banking ? bank() : enchant();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("fountain")) {
			timeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
		} else if (message.endsWith("gate") || message.endsWith("shut") || message.endsWith("open")) {
			timeout = 0L;
		} else if (message.endsWith("mining")) {
			amuletsEnchanted++;
			amuletsRemaining--;
			timeout = 0L;
		}
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (!isBanking()) {
				return openBank();
			}

			if (hasInventoryItem(ITEM_ID_CHARGED_DRAGONSTONE_AMULET)) {
				if (System.currentTimeMillis() <= depositTimeout) {
					return 0;
				}

				deposit(ITEM_ID_CHARGED_DRAGONSTONE_AMULET, MAX_INV_SIZE);
				depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
				return 0;
			}

			if (!hasInventoryItem(ITEM_ID_LAW_RUNE)) {
				if (System.currentTimeMillis() <= withdrawLawTimeout) {
					return 0;
				}

				if (!hasBankItem(ITEM_ID_LAW_RUNE)) {
					return exit("Out of law runes.");
				}

				withdraw(ITEM_ID_LAW_RUNE, 1);
				withdrawLawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (!hasInventoryItem(ITEM_ID_WATER_RUNE)) {
				if (System.currentTimeMillis() <= withdrawWaterTimeout) {
					return 0;
				}

				if (!hasBankItem(ITEM_ID_WATER_RUNE)) {
					return exit("Out of water runes.");
				}

				withdraw(ITEM_ID_WATER_RUNE, 1);
				withdrawWaterTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (!hasInventoryItem(ITEM_ID_DRAGONSTONE_AMULET)) {
				if (System.currentTimeMillis() <= withdrawAmuletTimeout) {
					return 0;
				}

				if (!hasBankItem(ITEM_ID_DRAGONSTONE_AMULET)) {
					return exit("Out of dragonstone amulets.");
				}

				withdraw(ITEM_ID_DRAGONSTONE_AMULET, MAX_INV_SIZE - getInventoryCount());
				withdrawAmuletTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
				return 0;
			}

			amuletsRemaining = bankCount(ITEM_ID_DRAGONSTONE_AMULET) +
				getInventoryCount(ITEM_ID_DRAGONSTONE_AMULET);
			banking = false;
			return 0;
		}

		if (Area.GUILD_GARDEN.contains(playerX, playerY)) {
			if (System.currentTimeMillis() <= timeout) {
				return 0;
			}

			castOnSelf(SPELL_ID_FALADOR_TELEPORT);
			timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= 1) {
			if (getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) ==
				Object.BANK_DOORS.id) {
				atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int enchant() {
		if (Area.GUILD_GARDEN.contains(playerX, playerY)) {
			if (System.currentTimeMillis() <= timeout) {
				return 0;
			}

			final int index = getInventoryIndex(ITEM_ID_DRAGONSTONE_AMULET);

			if (index == -1) {
				banking = true;
				return 0;
			}

			useItemOnObject(index, Object.FOUNTAIN_OF_HEROS.coordinate.getX(),
				Object.FOUNTAIN_OF_HEROS.coordinate.getY());
			timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (Area.GUILD_ENTRANCE.contains(playerX, playerY)) {
			final int wallX = Object.GUILD_DOOR_GARDEN.coordinate.getX();
			final int wallY = Object.GUILD_DOOR_GARDEN.coordinate.getY();

			if (getWallObjectIdFromCoords(wallX, wallY) == Object.GUILD_DOOR_GARDEN.id) {
				atWallObject(wallX, wallY);
				return SLEEP_ONE_SECOND;
			}

			walkTo(wallX, wallY);
			return SLEEP_ONE_TICK;
		}

		if (playerX > Object.MEMBERS_GATE.coordinate.getX()) {
			if (distanceTo(Object.GUILD_DOOR_MAIN.coordinate.getX(),
				Object.GUILD_DOOR_MAIN.coordinate.getY()) <= 1) {
				atWallObject(Object.GUILD_DOOR_MAIN.coordinate.getX(), Object.GUILD_DOOR_MAIN.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.GUILD_DOOR_MAIN.coordinate.getX(), Object.GUILD_DOOR_MAIN.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerY <= COORDINATE_LOAD_MEMBERS_GATE.getY()) {
			if (distanceTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY()) <= 1) {
				if (System.currentTimeMillis() <= timeout) {
					return 0;
				}

				atObject(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
				timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			walkTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BANK.contains(playerX, playerY) &&
			getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) ==
				Object.BANK_DOORS.id) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(COORDINATE_LOAD_MEMBERS_GATE.getX(), COORDINATE_LOAD_MEMBERS_GATE.getY());
		return SLEEP_ONE_TICK;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Heros Guild Enchanter", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Amulets: @whi@%d @cya@(@whi@%s enchants@cya@/@whi@hr@cya@)",
				amuletsEnchanted, toUnitsPerHour(amuletsEnchanted, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		if (amuletsRemaining <= 0) return;

		drawString(String.format("@yel@Remaining: @whi@%d", amuletsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(amuletsEnchanted, amuletsRemaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum Area implements RSArea {
		GUILD_ENTRANCE(new Coordinate(368, 434), new Coordinate(377, 440)),
		GUILD_GARDEN(new Coordinate(378, 434), new Coordinate(382, 440)),
		BANK(new Coordinate(328, 549), new Coordinate(334, 557));

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
		GUILD_DOOR_MAIN(74, new Coordinate(372, 441)),
		GUILD_DOOR_GARDEN(2, new Coordinate(378, 437)),
		FOUNTAIN_OF_HEROS(282, new Coordinate(381, 439)),
		MEMBERS_GATE(137, new Coordinate(341, 487)),
		BANK_DOORS(64, new Coordinate(327, 552));

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
