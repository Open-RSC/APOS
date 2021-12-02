import java.awt.*;
import java.time.Duration;
import java.time.Instant;
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
 * -g,--gem <sapphire|emerald|ruby|diamond|dragonstone>
 * <p>
 * Author: Chomp
 */
public class AA_GemCutter extends AA_Script {
	private static final Coordinate COORDINATE_SHANTAY_BANK_CHEST = new Coordinate(58, 731);

	private static final int ITEM_ID_CHISEL = 167;
	private static final int INITIAL_INVENTORY_SIZE = 2;
	private static final int SKILL_INDEX_CRAFTING = 12;
	private static final int MAXIMUM_FATIGUE = 99;

	private Gem gem;
	private Iterator<Gem> iterator;
	private Instant startTime;

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

		this.chiselIndex = this.getInventoryIndex(ITEM_ID_CHISEL);

		if (this.chiselIndex == -1) {
			throw new IllegalStateException("Chisel missing from inventory.");
		}

		if (this.gem == null) {
			this.iterator = EnumSet.allOf(Gem.class).iterator();
			this.gem = this.iterator.next();
		}

		if (this.getLevel(SKILL_INDEX_CRAFTING) < this.gem.level) {
			throw new IllegalStateException(String.format("Lvl %d Crafting required to cut %s",
				this.gem.level, getItemNameId(this.gem.id)));
		}

		this.shantayBanking = this.distanceTo(COORDINATE_SHANTAY_BANK_CHEST.getX(), COORDINATE_SHANTAY_BANK_CHEST.getY()) < 10;
		this.initialCraftingXp = this.getAccurateXpForLevel(SKILL_INDEX_CRAFTING);
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		if (this.idle) {
			return this.idle();
		}

		if (this.getFatigue() >= MAXIMUM_FATIGUE) {
			return this.sleep();
		}

		if (System.currentTimeMillis() <= this.chiselTimeout) {
			return 0;
		}

		if (this.getInventoryCount() == INITIAL_INVENTORY_SIZE ||
			this.getInventoryId(INITIAL_INVENTORY_SIZE) != this.gem.id) {
			return this.bank();
		}

		this.extension.displayMessage("@gre@Cutting ...");
		this.useItemWithItem(this.chiselIndex, INITIAL_INVENTORY_SIZE);
		this.chiselTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("cut", 4)) {
			this.gemsCut++;
			if (this.gemsRemaining > 0) {
				this.gemsRemaining--;
			}
			this.chiselTimeout = 0L;
		} else if (message.endsWith("men.") || message.endsWith("you.")) {
			this.openTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		} else if (message.endsWith("area")) {
			if (!this.shantayBanking) {
				return;
			}
			this.idle = true;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Gem Cutter", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

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

		this.drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				this.gem, this.gemsCut, getUnitsPerHour(this.gemsCut, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Remaining: @whi@%d", this.gemsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Time remaining: @whi@%s",
				getTTL(this.gemsCut, this.gemsRemaining, secondsElapsed)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
	}

	private int bank() {
		if (!this.isBanking()) {
			if (this.shantayBanking) {
				if (System.currentTimeMillis() <= this.openTimeout) {
					return 0;
				}

				this.atObject(COORDINATE_SHANTAY_BANK_CHEST.getX(), COORDINATE_SHANTAY_BANK_CHEST.getY());
				this.openTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			return this.openBank();
		}

		if (this.getInventoryCount() == INITIAL_INVENTORY_SIZE) {
			if (System.currentTimeMillis() <= this.withdrawTimeout) {
				return 0;
			}

			if (!this.hasBankItem(this.gem.id)) {
				if (this.iterator == null || !this.iterator.hasNext()) {
					return this.exit(String.format("Out of %s.", getItemNameId(this.gem.id)));
				}

				this.gem = this.iterator.next();
				this.gemsCut = 0;
				this.initialCraftingXp = this.getAccurateXpForLevel(SKILL_INDEX_CRAFTING);
				this.startTime = Instant.now();

				if (this.getLevel(SKILL_INDEX_CRAFTING) < this.gem.level) {
					return this.exit(String.format("Lvl %d Crafting required to cut %s.",
						this.gem.level, getItemNameId(this.gem.id)));
				}

				return 0;
			}

			this.gemsRemaining = this.bankCount(this.gem.id);
			this.withdraw(this.gem.id, MAX_INV_SIZE - INITIAL_INVENTORY_SIZE);
			this.withdrawTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= this.depositTimeout) {
			return 0;
		}

		final int itemId = this.getInventoryId(INITIAL_INVENTORY_SIZE);
		this.deposit(itemId, MAX_INV_SIZE);
		this.depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private int idle() {
		if (this.getX() == COORDINATE_SHANTAY_BANK_CHEST.getX() + 2 && this.getY() == COORDINATE_SHANTAY_BANK_CHEST.getY()) {
			this.idle = false;
			return 0;
		}

		this.walkTo(COORDINATE_SHANTAY_BANK_CHEST.getX() + 2, COORDINATE_SHANTAY_BANK_CHEST.getY());
		return SLEEP_ONE_TICK;
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
			return this.name;
		}
	}
}
