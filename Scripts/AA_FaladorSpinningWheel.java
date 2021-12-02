import com.aposbot.Constants;

import java.awt.Font;
import java.time.Duration;
import java.time.Instant;

/**
 * Spins wool or flax at the Falador Spinning Wheel.
 * <p>
 * Required:
 * Start at Falador East Bank with an empty inventory.
 * <p>
 * Required Parameter:
 * -i,--item <wool|flax>
 * <p>
 * Author: Chomp
 */
public class AA_FaladorSpinningWheel extends AA_Script {
	private static final int MAXIMUM_FATIGUE = 100;
	private static final int SKILL_INDEX_CRAFTING = 12;

	private Item item;
	private Instant startTime;

	private double initialCraftingXp;

	private long spinTimeout;
	private long withdrawTimeout;
	private long depositTimeout;

	private int playerX;
	private int playerY;

	private int spinCount;
	private int materialRemaining;

	public AA_FaladorSpinningWheel(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) {
			throw new IllegalArgumentException("Empty script parameter. Must specify Item parameter.");
		}

		final String[] args = parameters.split(" ");

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
				case "-i":
				case "--item":
					this.item = Item.valueOf(args[++i].toUpperCase());
					break;
				default:
					throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
			}
		}

		this.initialCraftingXp = this.getAccurateXpForLevel(SKILL_INDEX_CRAFTING);
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		this.playerX = this.getX();
		this.playerY = this.getY();

		if (this.getInventoryCount() > 0 && this.getInventoryId(0) == this.item.id) {
			return this.spin();
		}

		return this.bank();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("wool") || message.endsWith("string")) {
			this.spinCount++;
			if (this.materialRemaining > 0) {
				this.materialRemaining--;
			}
			this.spinTimeout = 0L;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Falador Spinning Wheel", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final double xpGained = this.getAccurateXpForLevel(SKILL_INDEX_CRAFTING) - this.initialCraftingXp;

		this.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), getUnitsPerHour(xpGained, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@%s: @whi@%s @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				this.item.productName, this.spinCount, getUnitsPerHour(this.spinCount, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Remaining: @whi@%d", this.materialRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Time remaining: @whi@%s",
				getTTL(this.spinCount, this.materialRemaining, secondsElapsed)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
	}

	private int spin() {
		if (Area.HOUSE.contains(this.playerX, this.playerY)) {
			if (this.getFatigue() >= MAXIMUM_FATIGUE) {
				if (this.playerX != Object.BED.coordinate.getX() - 1 ||
					this.playerY != Object.BED.coordinate.getY() - 1) {
					this.walkTo(Object.BED.coordinate.getX() - 1, Object.BED.coordinate.getY() - 1);
					return SLEEP_ONE_TICK;
				}

				this.useBed();
				return SLEEP_ONE_SECOND;
			}

			if (System.currentTimeMillis() <= this.spinTimeout) {
				return 0;
			}

			if (this.playerX != Object.SPINNING_WHEEL.coordinate.getX() + 1 ||
				this.playerY != Object.SPINNING_WHEEL.coordinate.getY() - 1) {
				this.walkTo(Object.SPINNING_WHEEL.coordinate.getX() + 1, Object.SPINNING_WHEEL.coordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			this.extension.displayMessage("@gre@Spinning...");
			this.useSpinningWheel();
			this.spinTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
			return 0;
		}

		if (Area.BANK.contains(this.playerX, this.playerY) &&
			this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (this.getWallObjectIdFromCoords(Object.HOUSE_DOOR.coordinate.getX(), Object.HOUSE_DOOR.coordinate.getY()) == Object.HOUSE_DOOR.id) {
			this.atWallObject(Object.HOUSE_DOOR.coordinate.getX(), Object.HOUSE_DOOR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(Object.SPINNING_WHEEL.coordinate.getX() + 1, Object.SPINNING_WHEEL.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (!this.isBanking()) {
				return this.openBank();
			}

			if (this.getInventoryCount() == 0) {
				if (System.currentTimeMillis() <= this.withdrawTimeout) {
					return 0;
				}

				this.materialRemaining = this.bankCount(this.item.id);

				if (this.materialRemaining == 0) {
					return this.exit(String.format("Out of %s.", this.item.name));
				}

				this.withdraw(this.item.id, MAX_INV_SIZE);
				this.withdrawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= this.depositTimeout) {
				return 0;
			}

			final int itemId = this.getInventoryId(0);
			this.deposit(itemId, MAX_INV_SIZE);
			this.depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (Area.HOUSE.contains(this.playerX, this.playerY)) {
			if (this.getWallObjectIdFromCoords(Object.HOUSE_DOOR.coordinate.getX(), Object.HOUSE_DOOR.coordinate.getY()) == Object.HOUSE_DOOR.id) {
				this.atWallObject(Object.HOUSE_DOOR.coordinate.getX(), Object.HOUSE_DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(Object.BANK_DOORS.coordinate.getX() - 2, Object.BANK_DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private void useSpinningWheel() {
		this.extension.createPacket(Constants.OP_OBJECT_USEWITH);
		this.extension.put2(Object.SPINNING_WHEEL.coordinate.getX());
		this.extension.put2(Object.SPINNING_WHEEL.coordinate.getY());
		this.extension.put2(0);
		this.extension.finishPacket();
	}

	private void useBed() {
		this.extension.createPacket(Constants.OP_OBJECT_ACTION1);
		this.extension.put2(Object.BED.coordinate.getX());
		this.extension.put2(Object.BED.coordinate.getY());
		this.extension.finishPacket();
	}

	private enum Item {
		WOOL(145, "Wool", "Balls"),
		FLAX(675, "Flax", "String");

		private final int id;
		private final String name;
		private final String productName;

		Item(final int id, final String name, final String productName) {
			this.id = id;
			this.name = name;
			this.productName = productName;
		}
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(280, 564), new Coordinate(286, 573)),
		HOUSE(new Coordinate(295, 577), new Coordinate(299, 580));

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
		SPINNING_WHEEL(121, new Coordinate(295, 579)),
		BED(15, new Coordinate(298, 579)),
		BANK_DOORS(64, new Coordinate(287, 571)),
		HOUSE_DOOR(2, new Coordinate(297, 577));

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
