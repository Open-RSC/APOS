import java.util.EnumSet;
import java.util.Iterator;

/**
 * Cuts gems.
 * <p>
 * Requirements:
 * Start at a bank or Shantay Pass with a sleeping bag and chisel in inventory.
 * The script will try to cut all gems if no gem parameter is provided.
 * <p>
 * Optional Parameter:
 * <sapphire|emerald|ruby|diamond|dragonstone>
 * <p>
 *
 * @Author Chomp
 */
public class AA_GemCutter extends AA_Script {
	private static final Coordinate COORDINATE_SHANTAY_BANK_CHEST = new Coordinate(58, 731);

	private static final int ITEM_ID_CHISEL = 167;
	private static final int INITIAL_INVENTORY_SIZE = 2;
	private static final int MAXIMUM_FATIGUE = 99;

	private Gem gem;
	private Iterator<Gem> iterator;
	private long startTime;

	private double initialCraftingXp;

	private long openTimeout;
	private long depositTimeout;
	private long withdrawTimeout;
	private long chiselTimeout;

	private int chiselIndex;
	private int gemsCut;
	private int gemsRemaining;

	private boolean shantayBanking;
	private boolean idle;

	public AA_GemCutter(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) gem = Gem.valueOf(parameters.toUpperCase());

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		chiselIndex = getInventoryIndex(ITEM_ID_CHISEL);

		if (chiselIndex == -1) {
			throw new IllegalStateException("Chisel missing from inventory.");
		}

		if (gem == null) {
			iterator = EnumSet.allOf(Gem.class).iterator();
			gem = iterator.next();
		}

		if (getLevel(Skill.CRAFTING.getIndex()) < gem.level) {
			throw new IllegalStateException(String.format("Lvl %d Crafting required to cut %s",
				gem.level, getItemNameId(gem.id)));
		}

		shantayBanking = distanceTo(COORDINATE_SHANTAY_BANK_CHEST.getX(), COORDINATE_SHANTAY_BANK_CHEST.getY()) < 10;
		initialCraftingXp = getAccurateXpForLevel(Skill.CRAFTING.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (idle) {
			return idle();
		}

		if (getFatigue() >= MAXIMUM_FATIGUE) {
			return sleep();
		}

		if (System.currentTimeMillis() <= chiselTimeout) {
			return 0;
		}

		if (getInventoryCount() == INITIAL_INVENTORY_SIZE ||
			getInventoryId(INITIAL_INVENTORY_SIZE) != gem.id) {
			return bank();
		}

		bot.displayMessage("@gre@Cutting ...");
		useItemWithItem(chiselIndex, INITIAL_INVENTORY_SIZE);
		chiselTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("cut", 4)) {
			gemsCut++;
			if (gemsRemaining > 0) {
				gemsRemaining--;
			}
			chiselTimeout = 0L;
		} else if (message.endsWith("men.") || message.endsWith("you.")) {
			openTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		} else if (message.endsWith("area")) {
			if (!shantayBanking) {
				return;
			}
			idle = true;
		} else {
			super.onServerMessage(message);
		}
	}

	private int idle() {
		if (getX() == COORDINATE_SHANTAY_BANK_CHEST.getX() + 2 && getY() == COORDINATE_SHANTAY_BANK_CHEST.getY()) {
			idle = false;
			return 0;
		}

		walkTo(COORDINATE_SHANTAY_BANK_CHEST.getX() + 2, COORDINATE_SHANTAY_BANK_CHEST.getY());
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (!isBanking()) {
			if (shantayBanking) {
				if (System.currentTimeMillis() <= openTimeout) {
					return 0;
				}

				atObject(COORDINATE_SHANTAY_BANK_CHEST.getX(), COORDINATE_SHANTAY_BANK_CHEST.getY());
				openTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			return openBank();
		}

		if (getInventoryCount() == INITIAL_INVENTORY_SIZE) {
			if (System.currentTimeMillis() <= withdrawTimeout) {
				return 0;
			}

			if (!hasBankItem(gem.id)) {
				if (iterator == null || !iterator.hasNext()) {
					return exit(String.format("Out of %s.", getItemNameId(gem.id)));
				}

				gem = iterator.next();
				gemsCut = 0;
				initialCraftingXp = getAccurateXpForLevel(Skill.CRAFTING.getIndex());
				startTime = System.currentTimeMillis();

				if (getLevel(Skill.CRAFTING.getIndex()) < gem.level) {
					return exit(String.format("Lvl %d Crafting required to cut %s.",
						gem.level, getItemNameId(gem.id)));
				}

				return 0;
			}

			gemsRemaining = bankCount(gem.id);
			withdraw(gem.id, MAX_INV_SIZE - INITIAL_INVENTORY_SIZE);
			withdrawTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= depositTimeout) {
			return 0;
		}

		final int itemId = getInventoryId(INITIAL_INVENTORY_SIZE);
		deposit(itemId, MAX_INV_SIZE);
		depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Gem Cutter", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getAccurateXpForLevel(Skill.CRAFTING.getIndex()) - initialCraftingXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				gem, gemsCut, toUnitsPerHour(gemsCut, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Remaining: @whi@%d", gemsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(gemsCut, gemsRemaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum Gem {
		SAPPHIRE(160, 20, "Sapphire"),
		EMERALD(159, 27, "Emerald"),
		RUBY(158, 34, "Ruby"),
		DIAMOND(157, 43, "Diamond"),
		DRAGONSTONE(542, 55, "Dragonstone");

		private final int id;
		private final int level;
		private final String name;

		Gem(final int id, final int level, final String name) {
			this.id = id;
			this.level = level;
			this.name = name;
		}

		public String toString() {
			return name;
		}
	}
}
