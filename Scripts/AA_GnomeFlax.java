import com.aposbot.Constants;

import java.awt.Font;
import java.time.Duration;
import java.time.Instant;

/**
 * Picks flax at the Tree Gnome Stronghold.
 * <p>
 * Requirements:
 * Start at the Tree Gnome Stronghold. If spinning flax have sleeping bag in inventory.
 * The script will spin and drop flax by default.
 * <p>
 * Optional Parameters:
 * -b,--bank (bank bow strings)
 * -n,--no-spin (bank only flax)
 * <p>
 * Author: Chomp
 */
public class AA_GnomeFlax extends AA_Script {
	private static final int ITEM_ID_FLAX = 675;

	private static final int SKILL_INDEX_CRAFTING = 12;
	private static final int MAXIMUM_SLEEP_WALK_FATIGUE = 85;
	private static final int MAXIMUM_FATIGUE = 100;

	private State state;
	private Instant startTime;

	private double initialCraftingXp;

	private long depositTimeout;
	private long ladderTimeout;
	private long flaxTimeout;

	private int playerX;
	private int playerY;

	private int flaxPicked;

	private boolean banking;
	private boolean spinning = true;

	public AA_GnomeFlax(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (final String arg : args) {
				switch (arg.toLowerCase()) {
					case "-b":
					case "--bank":
						this.banking = true;
						break;
					case "-n":
					case "--no-spin":
						this.spinning = false;
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		if (this.spinning) {
			if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
				throw new IllegalStateException("Sleeping bag missing from inventory.");
			}
		} else {
			this.banking = true;
		}

		this.state = State.PICK;
		this.initialCraftingXp = this.getAccurateXpForLevel(SKILL_INDEX_CRAFTING);
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		this.playerX = this.getX();
		this.playerY = this.getY();

		switch (this.state) {
			case PICK:
				return this.pick();
			case SPIN:
				return this.spin();
			case DROP:
				return this.drop();
			case BANK:
				return this.bank();
			default:
				return this.exit("Invalid script state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("plant")) {
			this.flaxPicked++;
			this.flaxTimeout = 0L;
		} else if (message.endsWith("string")) {
			this.flaxTimeout = 0L;
		} else if (message.endsWith("ladder")) {
			this.ladderTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Gnome Flax", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.spinning) {
			final double xpGained = this.getAccurateXpForLevel(SKILL_INDEX_CRAFTING) - this.initialCraftingXp;

			this.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
					DECIMAL_FORMAT.format(xpGained), getUnitsPerHour(xpGained, secondsElapsed)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}

		this.drawString(String.format("@yel@Flax: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				this.flaxPicked, getUnitsPerHour(this.flaxPicked, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Mode: @whi@%s", this.banking ? "Banking" : "Powerflax"),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
	}

	private int pick() {
		if (this.playerX == Object.FLAX.coordinate.getX() && this.playerY == Object.FLAX.coordinate.getY() + 1) {
			if (this.getInventoryCount() == MAX_INV_SIZE) {
				this.state = this.spinning ? State.SPIN : State.BANK;
				return 0;
			}

			if (System.currentTimeMillis() <= this.flaxTimeout) {
				return 0;
			}

			this.extension.displayMessage("@gre@Picking ...");
			this.pickFlax();
			this.flaxTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
			return 0;
		}

		if (Area.TREE.contains(this.playerX, this.playerY)) {
			if (System.currentTimeMillis() <= this.ladderTimeout) {
				return 0;
			}

			this.useLadder(Object.LADDER_DOWN_TREE.coordinate.getX(), Object.LADDER_DOWN_TREE.coordinate.getY());
			this.ladderTimeout = System.currentTimeMillis() + SLEEP_TWO_SECONDS;
			return 0;
		}

		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (this.playerX == Object.LADDER_DOWN_BANK.coordinate.getX() - 1 &&
				this.playerY == Object.LADDER_DOWN_BANK.coordinate.getY()) {
				if (System.currentTimeMillis() <= this.ladderTimeout) {
					return 0;
				}

				this.useLadder(Object.LADDER_DOWN_BANK.coordinate.getX(), Object.LADDER_DOWN_BANK.coordinate.getY());
				this.ladderTimeout = System.currentTimeMillis() + SLEEP_TWO_SECONDS;
				return 0;
			}

			this.walkTo(Object.LADDER_DOWN_BANK.coordinate.getX() - 1, Object.LADDER_DOWN_BANK.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.FLAX.coordinate.getX(), Object.FLAX.coordinate.getY() + 1);

		if (this.getFatigue() >= MAXIMUM_SLEEP_WALK_FATIGUE && this.isWalking()) {
			return this.sleep();
		}

		return SLEEP_ONE_TICK;
	}

	private int spin() {
		if (Area.TREE.contains(this.playerX, this.playerY)) {
			if (this.getInventoryId(1) != ITEM_ID_FLAX) {
				this.state = this.banking ? State.BANK : State.DROP;
				return 0;
			}

			if (this.getFatigue() >= MAXIMUM_FATIGUE) {
				return this.sleep();
			}

			if (System.currentTimeMillis() <= this.flaxTimeout) {
				return 0;
			}

			if (this.playerX == Object.SPINNING_WHEEL.coordinate.getX() - 1 &&
				this.playerY == Object.SPINNING_WHEEL.coordinate.getY()) {
				this.extension.displayMessage("@gre@Spinning ...");
				this.useSpinningWheel();
				this.flaxTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
				return 0;
			}

			this.walkTo(Object.SPINNING_WHEEL.coordinate.getX() - 1, Object.SPINNING_WHEEL.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= this.ladderTimeout) {
			return 0;
		}

		if (this.playerX == Object.LADDER_UP_TREE.coordinate.getX() + 1 &&
			this.playerY == Object.LADDER_UP_TREE.coordinate.getY()) {
			this.useLadder(Object.LADDER_UP_TREE.coordinate.getX(), Object.LADDER_UP_TREE.coordinate.getY());
			this.ladderTimeout = System.currentTimeMillis() + SLEEP_TWO_SECONDS;
			return 0;
		}

		this.walkTo(Object.LADDER_UP_TREE.coordinate.getX() + 1, Object.LADDER_UP_TREE.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int drop() {
		if (this.getInventoryCount() == 1) {
			this.state = State.PICK;
			return 0;
		}

		this.extension.displayMessage("@whi@Dropping ...");
		this.dropItem(1);
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (this.getInventoryCount() != MAX_INV_SIZE) {
				this.state = State.PICK;
				return 0;
			}

			if (!this.isBanking()) {
				return this.openBank();
			}

			if (System.currentTimeMillis() <= this.depositTimeout) {
				return 0;
			}

			final int itemId = this.getInventoryId(this.spinning ? 1 : 0);
			this.deposit(itemId, MAX_INV_SIZE);
			this.depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (Area.TREE.contains(this.playerX, this.playerY)) {
			if (System.currentTimeMillis() <= this.ladderTimeout) {
				return 0;
			}

			this.useLadder(Object.LADDER_DOWN_TREE.coordinate.getX(), Object.LADDER_DOWN_TREE.coordinate.getY());
			this.ladderTimeout = System.currentTimeMillis() + SLEEP_TWO_SECONDS;
			return 0;
		}

		if (this.playerX == Object.LADDER_UP_BANK.coordinate.getX() - 1 &&
			this.playerY == Object.LADDER_UP_BANK.coordinate.getY()) {
			if (System.currentTimeMillis() <= this.ladderTimeout) {
				return 0;
			}

			this.useLadder(Object.LADDER_UP_BANK.coordinate.getX(), Object.LADDER_UP_BANK.coordinate.getY());
			this.ladderTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		this.walkTo(Object.LADDER_UP_BANK.coordinate.getX() - 1, Object.LADDER_UP_BANK.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private void useSpinningWheel() {
		this.extension.createPacket(Constants.OP_OBJECT_USEWITH);
		this.extension.put2(Object.SPINNING_WHEEL.coordinate.getX());
		this.extension.put2(Object.SPINNING_WHEEL.coordinate.getY());
		this.extension.put2(1);
		this.extension.finishPacket();
	}

	private void pickFlax() {
		this.extension.createPacket(Constants.OP_OBJECT_ACTION2);
		this.extension.put2(Object.FLAX.coordinate.getX());
		this.extension.put2(Object.FLAX.coordinate.getY());
		this.extension.finishPacket();
	}

	private void useLadder(final int x, final int y) {
		this.extension.createPacket(Constants.OP_OBJECT_ACTION1);
		this.extension.put2(x);
		this.extension.put2(y);
		this.extension.finishPacket();
	}

	private enum State {
		PICK,
		SPIN,
		DROP,
		BANK
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(712, 1142), new Coordinate(716, 1463)),
		TREE(new Coordinate(691, 1468), new Coordinate(693, 1470));

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
		LADDER_DOWN_BANK(6, new Coordinate(714, 1460)),
		LADDER_UP_BANK(5, new Coordinate(714, 516)),
		LADDER_DOWN_TREE(6, new Coordinate(692, 1469)),
		LADDER_UP_TREE(5, new Coordinate(692, 525)),
		SPINNING_WHEEL(121, new Coordinate(694, 1469)),
		FLAX(313, new Coordinate(693, 524));

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
