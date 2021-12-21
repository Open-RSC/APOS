import java.awt.Font;
import java.time.Duration;
import java.time.Instant;

/**
 * Enchants dragonstone amulets at the Hero's Guild and banks at Falador West Bank.
 * <p>
 * Required:
 * Start script at Falador West Bank.
 * Have staff of air equipped. Have law runes/water runes/amulets banked.
 * <p>
 * Author: Chomp
 */
public class AA_HerosGuildEnchanter extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_MEMBERS_GATE = new Coordinate(326, 544);

	private static final int ITEM_ID_STAFF_OF_AIR = 101;
	private static final int ITEM_ID_WATER_RUNE = 32;
	private static final int ITEM_ID_LAW_RUNE = 42;
	private static final int ITEM_ID_DRAGONSTONE_AMULET = 522;
	private static final int ITEM_ID_CHARGED_DRAGONSTONE_AMULET = 597;

	private static final int SKILL_INDEX_MAGIC = 6;
	private static final int SPELL_ID_FALADOR_TELEPORT = 18;
	private static final int QUEST_ID_HEROS = 19;
	private static final int MINIMUM_MAGIC_LVL = 37;

	private Instant startTime;

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
		if (!this.isQuestComplete(QUEST_ID_HEROS)) {
			throw new IllegalStateException("Must have Hero's Quest completed to run this script.");
		}

		if (this.getLevel(SKILL_INDEX_MAGIC) < MINIMUM_MAGIC_LVL) {
			throw new IllegalStateException(String.format("Must have at least L%d magic to run this script.",
				MINIMUM_MAGIC_LVL));
		}

		final int staffIndex = this.getInventoryIndex(ITEM_ID_STAFF_OF_AIR);

		if (staffIndex == -1 || !this.isItemEquipped(staffIndex)) {
			throw new IllegalStateException("Staff of air missing/unequipped.");
		}

		this.banking = !this.hasInventoryItem(ITEM_ID_DRAGONSTONE_AMULET) ||
			!this.hasInventoryItem(ITEM_ID_LAW_RUNE) ||
			!this.hasInventoryItem(ITEM_ID_WATER_RUNE);
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		this.playerX = this.getX();
		this.playerY = this.getY();

		return this.banking ? this.bank() : this.enchant();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("fountain")) {
			this.timeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
		} else if (message.endsWith("gate") || message.endsWith("shut") || message.endsWith("open")) {
			this.timeout = 0L;
		} else if (message.endsWith("mining")) {
			this.amuletsEnchanted++;
			this.amuletsRemaining--;
			this.timeout = 0L;
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Heros Guild Enchanter", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Amulets: @whi@%d @cya@(@whi@%s enchants@cya@/@whi@hr@cya@)",
				this.amuletsEnchanted, getUnitsPerHour(this.amuletsEnchanted, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.amuletsRemaining > 0) {
			this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			this.drawString(String.format("@yel@Remaining: @whi@%d", this.amuletsRemaining),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			this.drawString(String.format("@yel@Time remaining: @whi@%s",
					getTTL(this.amuletsEnchanted, this.amuletsRemaining, secondsElapsed)),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}
	}

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (!this.isBanking()) {
				return this.openBank();
			}

			if (this.hasInventoryItem(ITEM_ID_CHARGED_DRAGONSTONE_AMULET)) {
				if (System.currentTimeMillis() <= this.depositTimeout) {
					return 0;
				}

				this.deposit(ITEM_ID_CHARGED_DRAGONSTONE_AMULET, MAX_INV_SIZE);
				this.depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
				return 0;
			}

			if (!this.hasInventoryItem(ITEM_ID_LAW_RUNE)) {
				if (System.currentTimeMillis() <= this.withdrawLawTimeout) {
					return 0;
				}

				if (!this.hasBankItem(ITEM_ID_LAW_RUNE)) {
					return this.exit("Out of law runes.");
				}

				this.withdraw(ITEM_ID_LAW_RUNE, 1);
				this.withdrawLawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (!this.hasInventoryItem(ITEM_ID_WATER_RUNE)) {
				if (System.currentTimeMillis() <= this.withdrawWaterTimeout) {
					return 0;
				}

				if (!this.hasBankItem(ITEM_ID_WATER_RUNE)) {
					return this.exit("Out of water runes.");
				}

				this.withdraw(ITEM_ID_WATER_RUNE, 1);
				this.withdrawWaterTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (!this.hasInventoryItem(ITEM_ID_DRAGONSTONE_AMULET)) {
				if (System.currentTimeMillis() <= this.withdrawAmuletTimeout) {
					return 0;
				}

				if (!this.hasBankItem(ITEM_ID_DRAGONSTONE_AMULET)) {
					return this.exit("Out of dragonstone amulets.");
				}

				this.withdraw(ITEM_ID_DRAGONSTONE_AMULET, MAX_INV_SIZE - this.getInventoryCount());
				this.withdrawAmuletTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
				return 0;
			}

			this.amuletsRemaining = this.bankCount(ITEM_ID_DRAGONSTONE_AMULET) +
				this.getInventoryCount(ITEM_ID_DRAGONSTONE_AMULET);
			this.banking = false;
			return 0;
		}

		if (Area.GUILD_GARDEN.contains(this.playerX, this.playerY)) {
			if (System.currentTimeMillis() <= this.timeout) {
				return 0;
			}

			this.castOnSelf(SPELL_ID_FALADOR_TELEPORT);
			this.timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (this.distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= 1) {
			if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) ==
				Object.BANK_DOORS.id) {
				this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int enchant() {
		if (Area.GUILD_GARDEN.contains(this.playerX, this.playerY)) {
			if (System.currentTimeMillis() <= this.timeout) {
				return 0;
			}

			final int index = this.getInventoryIndex(ITEM_ID_DRAGONSTONE_AMULET);

			if (index == -1) {
				this.banking = true;
				return 0;
			}

			this.useItemOnObject(ITEM_ID_DRAGONSTONE_AMULET, Object.FOUNTAIN_OF_HEROS.coordinate.getX(),
				Object.FOUNTAIN_OF_HEROS.coordinate.getY());
			this.timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (Area.GUILD_ENTRANCE.contains(this.playerX, this.playerY)) {
			final int wallX = Object.GUILD_DOOR_GARDEN.coordinate.getX();
			final int wallY = Object.GUILD_DOOR_GARDEN.coordinate.getY();

			if (this.getWallObjectIdFromCoords(wallX, wallY) == Object.GUILD_DOOR_GARDEN.id) {
				this.atWallObject(wallX, wallY);
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(wallX, wallY);
			return SLEEP_ONE_TICK;
		}

		if (this.playerX > Object.MEMBERS_GATE.coordinate.getX()) {
			if (this.distanceTo(Object.GUILD_DOOR_MAIN.coordinate.getX(),
				Object.GUILD_DOOR_MAIN.coordinate.getY()) <= 1) {
				this.atWallObject(Object.GUILD_DOOR_MAIN.coordinate.getX(), Object.GUILD_DOOR_MAIN.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.GUILD_DOOR_MAIN.coordinate.getX(), Object.GUILD_DOOR_MAIN.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.playerY <= COORDINATE_LOAD_MEMBERS_GATE.getY()) {
			if (this.distanceTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY()) <= 1) {
				if (System.currentTimeMillis() <= this.timeout) {
					return 0;
				}

				this.atObject(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
				this.timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			this.walkTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BANK.contains(this.playerX, this.playerY) &&
			this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) ==
				Object.BANK_DOORS.id) {
			this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(COORDINATE_LOAD_MEMBERS_GATE.getX(), COORDINATE_LOAD_MEMBERS_GATE.getY());
		return SLEEP_ONE_TICK;
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
			return this.lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return this.upperBoundingCoordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
		}
	}
}
