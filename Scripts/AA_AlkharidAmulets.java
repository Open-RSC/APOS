import com.aposbot.Constants;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * Crafts gemstone amulets at Alkharid furnace.
 * <p>
 * Requirements:
 * Start at Alkharid bank with sleeping bag and amulet mould in inventory.
 * The script will try to make all amulets if no gem parameter is provided.
 * <p>
 * Optional Parameter:
 * -g,--gem <sapphire|emerald|ruby|diamond|dragonstone>
 * <p>
 * Author: Chomp
 */
public class AA_AlkharidAmulets extends AA_Script {
	private static final int ITEM_ID_AMULET_MOULD = 294;
	private static final int ITEM_ID_GOLD_BAR = 172;

	private static final int SKILL_INDEX_CRAFTING = 12;

	private static final int MAXIMUM_FATIGUE = 98;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;

	private static final int INITIAL_INVENTORY_SIZE = 2;
	private static final int WITHDRAW_COUNT = 14;

	private Gem gem;
	private Iterator<Gem> iterator;
	private Instant startTime;

	private double initialCraftingXp;

	private long depositTimeout;
	private long withdrawGoldTimeout;
	private long withdrawGemTimeout;
	private long optionMenuTimeout;

	private int playerX;
	private int playerY;

	private int amuletsSmelted;
	private int gemsRemaining;

	public AA_AlkharidAmulets(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-g":
					case "--gem":
						this.gem = Gem.valueOf(args[++i].toUpperCase());
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!this.hasInventoryItem(ITEM_ID_AMULET_MOULD)) {
			throw new IllegalStateException("Amulet mould missing from inventory.");
		}

		if (this.gem == null) {
			this.iterator = EnumSet.allOf(Gem.class).iterator();
			this.gem = this.iterator.next();
		}

		this.initialCraftingXp = this.getAccurateXpForLevel(SKILL_INDEX_CRAFTING);
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		this.playerX = this.getX();
		this.playerY = this.getY();

		final int goldIndex = this.getInventoryIndex(ITEM_ID_GOLD_BAR);

		if (goldIndex != -1 && this.hasInventoryItem(this.gem.id)) {
			return this.craft(goldIndex);
		}

		return this.bank(goldIndex);
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("amulet")) {
			this.amuletsSmelted++;
			if (this.gemsRemaining > 0) {
				this.gemsRemaining--;
			}
			this.optionMenuTimeout = 0L;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Alkharid Amulets", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

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

		this.drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s amulets@cya@/@whi@hr@cya@)",
				this.gem, this.amuletsSmelted, getUnitsPerHour(this.amuletsSmelted, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Remaining: @whi@%d", this.gemsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Time remaining: @whi@%s",
				getTTL(this.amuletsSmelted, this.gemsRemaining, secondsElapsed)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
	}

	private int craft(final int goldIndex) {
		if (Area.FURNACE.contains(this.playerX, this.playerY)) {
			if (this.isQuestMenu()) {
				switch (this.getQuestMenuOption(0)) {
					case "ring":
						this.answer(2);
						break;
					case "Yes":
						this.answer(0);
						break;
					default:
						this.answer(this.gem.menuIndex);
						break;
				}

				this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= this.optionMenuTimeout) {
				return 0;
			}

			if (this.getFatigue() >= MAXIMUM_FATIGUE) {
				return this.sleep();
			}

			if (this.playerX != Object.FURNACE.coordinate.getX() - 1 ||
				this.playerY != Object.FURNACE.coordinate.getY()) {
				this.walkTo(Object.FURNACE.coordinate.getX() - 1, Object.FURNACE.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			this.extension.displayMessage("@gre@Crafting...");
			this.useFurnace(goldIndex);
			this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (Area.BANK.contains(this.playerX, this.playerY) &&
			this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(Object.FURNACE.coordinate.getX() - 1, Object.FURNACE.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int bank(final int goldIndex) {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (!this.isBanking()) {
				return this.openBank();
			}

			if (goldIndex != -1 && this.getInventoryCount() != MAX_INV_SIZE) {
				if (System.currentTimeMillis() <= this.withdrawGemTimeout) {
					return 0;
				}

				final int gemsRemaining = this.bankCount(this.gem.id);

				if (gemsRemaining == 0) {
					if (this.iterator == null || !this.iterator.hasNext()) {
						return this.exit(String.format("Ran out of %ss.", getItemNameId(this.gem.id)));
					}

					this.gem = this.iterator.next();
					this.amuletsSmelted = 0;
					this.initialCraftingXp = this.getAccurateXpForLevel(SKILL_INDEX_CRAFTING);
					this.startTime = Instant.now();

					if (this.getLevel(SKILL_INDEX_CRAFTING) < this.gem.level) {
						return this.exit(String.format("Lvl %d Crafting required to make %s.",
							this.gem.level, getItemNameId(this.gem.amuletId)));
					}

					return 0;
				}

				final int goldBarsRemaining = this.bankCount(ITEM_ID_GOLD_BAR);

				this.gemsRemaining = Math.min(gemsRemaining, goldBarsRemaining);
				this.withdraw(this.gem.id, WITHDRAW_COUNT);
				this.withdrawGemTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= this.withdrawGoldTimeout) {
				return 0;
			}

			if (this.getInventoryCount() == INITIAL_INVENTORY_SIZE) {
				if (!this.hasBankItem(ITEM_ID_GOLD_BAR)) {
					return this.exit("Ran out of gold bars.");
				}

				this.withdraw(ITEM_ID_GOLD_BAR, WITHDRAW_COUNT);
				this.withdrawGoldTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= this.depositTimeout) {
				return 0;
			}

			final int itemId = this.getInventoryId(INITIAL_INVENTORY_SIZE);
			this.deposit(itemId, MAX_INV_SIZE);
			this.depositTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (this.distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
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

	private void useFurnace(final int inventoryIndex) {
		this.extension.createPacket(Constants.OP_OBJECT_USEWITH);
		this.extension.put2(Object.FURNACE.coordinate.getX());
		this.extension.put2(Object.FURNACE.coordinate.getY());
		this.extension.put2(inventoryIndex);
		this.extension.finishPacket();
	}

	private enum Gem {
		SAPPHIRE(164, 297, 13, 0, "Sapphire"),
		EMERALD(163, 298, 30, 1, "Emerald"),
		RUBY(162, 299, 50, 2, "Ruby"),
		DIAMOND(161, 300, 70, 3, "Diamond"),
		DRAGONSTONE(523, 522, 80, 4, "Dragonstone");

		private final int id;
		private final int amuletId;
		private final int level;
		private final int menuIndex;
		private final String name;

		Gem(final int id, final int amuletId, final int level, final int menuIndex, final String name) {
			this.id = id;
			this.amuletId = amuletId;
			this.level = level;
			this.menuIndex = menuIndex;
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(87, 689), new Coordinate(93, 700)),
		FURNACE(new Coordinate(82, 678), new Coordinate(86, 681));

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
		FURNACE(118, new Coordinate(85, 679)),
		BANK_DOORS(64, new Coordinate(86, 695));

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
