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
 * <sapphire|emerald|ruby|diamond|dragonstone>
 * <p>
 *
 * @Author Chomp
 */
public class AA_AlkharidAmulets extends AA_Script {
	private static final int ITEM_ID_AMULET_MOULD = 294;
	private static final int ITEM_ID_GOLD_BAR = 172;

	private static final int MAXIMUM_SLEEP_WALK_FATIGUE = 80;
	private static final int MAXIMUM_FATIGUE = 98;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;

	private static final int INITIAL_INVENTORY_SIZE = 2;
	private static final int WITHDRAW_COUNT = 14;

	private Gem gem;
	private Iterator<Gem> iterator;
	private long startTime;

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
		if (parameters.isEmpty()) {
			iterator = EnumSet.allOf(Gem.class).iterator();
			gem = iterator.next();
		} else {
			gem = Gem.valueOf(parameters.toUpperCase());
		}

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!hasInventoryItem(ITEM_ID_AMULET_MOULD)) {
			throw new IllegalStateException("Amulet mould missing from inventory.");
		}

		initialCraftingXp = getAccurateXpForLevel(Skill.CRAFTING.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		final int goldIndex = getInventoryIndex(ITEM_ID_GOLD_BAR);

		if (goldIndex != -1 && hasInventoryItem(gem.id)) {
			return craft(goldIndex);
		}

		return bank(goldIndex);
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("amulet")) {
			amuletsSmelted++;
			if (gemsRemaining > 0) {
				gemsRemaining--;
			}
			optionMenuTimeout = 0L;
		} else {
			super.onServerMessage(message);
		}
	}

	private int craft(final int goldIndex) {
		if (Area.FURNACE.contains(playerX, playerY)) {
			if (isQuestMenu()) {
				switch (getQuestMenuOption(0)) {
					case "ring":
						answer(2);
						break;
					case "Yes":
						answer(0);
						break;
					default:
						answer(gem.menuIndex);
						break;
				}

				optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= optionMenuTimeout) {
				return 0;
			}

			if (getFatigue() >= MAXIMUM_FATIGUE) {
				return sleep();
			}

			final Coordinate furnace = Object.FURNACE.getCoordinate();

			if (playerX != furnace.getX() - 1 || playerY != furnace.getY()) {
				walkTo(furnace.getX() - 1, furnace.getY());
				return SLEEP_ONE_TICK;
			}

			bot.displayMessage("@gre@Crafting...");
			useWithObject(goldIndex, furnace.getX(), furnace.getY());
			optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (Area.BANK.contains(playerX, playerY) &&
			getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(Object.FURNACE.coordinate.getX() - 1, Object.FURNACE.coordinate.getY());

		if (getFatigue() >= MAXIMUM_SLEEP_WALK_FATIGUE && isWalking()) {
			return sleep();
		}

		return SLEEP_ONE_TICK;
	}

	private int bank(final int goldIndex) {
		if (Area.BANK.contains(playerX, playerY)) {
			if (!isBanking()) {
				return openBank();
			}

			if (goldIndex != -1 && getInventoryCount() != MAX_INV_SIZE) {
				if (System.currentTimeMillis() <= withdrawGemTimeout) {
					return 0;
				}

				final int gemsRemaining = bankCount(gem.id);

				if (gemsRemaining == 0) {
					if (iterator == null || !iterator.hasNext()) {
						return exit(String.format("Ran out of %ss.", getItemNameId(gem.id)));
					}

					gem = iterator.next();
					amuletsSmelted = 0;
					initialCraftingXp = getAccurateXpForLevel(Skill.CRAFTING.getIndex());
					startTime = System.currentTimeMillis();

					if (getLevel(Skill.CRAFTING.getIndex()) < gem.level) {
						return exit(String.format("Lvl %d Crafting required to make %s.",
							gem.level, getItemNameId(gem.amuletId)));
					}

					return 0;
				}

				final int goldBarsRemaining = bankCount(ITEM_ID_GOLD_BAR);

				this.gemsRemaining = Math.min(gemsRemaining, goldBarsRemaining);
				withdraw(gem.id, WITHDRAW_COUNT);
				withdrawGemTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= withdrawGoldTimeout) {
				return 0;
			}

			if (getInventoryCount() == INITIAL_INVENTORY_SIZE) {
				if (!hasBankItem(ITEM_ID_GOLD_BAR)) {
					return exit("Ran out of gold bars.");
				}

				withdraw(ITEM_ID_GOLD_BAR, WITHDRAW_COUNT);
				withdrawGoldTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= depositTimeout) {
				return 0;
			}

			final int itemId = getInventoryId(INITIAL_INVENTORY_SIZE);
			deposit(itemId, MAX_INV_SIZE);
			depositTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
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

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Alkharid Amulets", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getAccurateXpForLevel(Skill.CRAFTING.getIndex()) - initialCraftingXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s amulets@cya@/@whi@hr@cya@)",
				gem, amuletsSmelted, toUnitsPerHour(amuletsSmelted, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Remaining: @whi@%d", gemsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(amuletsSmelted, gemsRemaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
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
			return name;
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
			return lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return upperBoundingCoordinate;
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
			return id;
		}

		public Coordinate getCoordinate() {
			return coordinate;
		}
	}
}
