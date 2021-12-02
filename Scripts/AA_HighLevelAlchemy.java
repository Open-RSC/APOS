import java.awt.*;
import java.time.Duration;
import java.time.Instant;

/**
 * Casts high level alchemy on an item.
 * <p>
 * Requirements:
 * Start at a bank or Shantay Pass with sleeping bag, staff of fire, nature runes, and coins in inventory.
 * <p>
 * Required Parameter:
 * -i,--item <itemId>
 * <p>
 * Author: Chomp
 */
public class AA_HighLevelAlchemy extends AA_Script {
	private static final Coordinate COORDINATE_SHANTAY_BANK_CHEST = new Coordinate(58, 731);

	private static final long CAST_DELAY = 550L; // +- based on latency

	private static final int[] ITEM_IDS_STAFF_OF_FIRE = new int[]{197, 615, 682};

	private static final int SPELL_ID_HIGH_ALCHEMY = 28;
	private static final int SKILL_INDEX_MAGIC = 6;

	private static final int ITEM_ID_NATURE_RUNE = 40;
	private static final int ITEM_ID_COINS = 10;

	private static final int MAXIMUM_FATIGUE = 99;
	private static final int INITIAL_INVENTORY_SIZE = 4;

	private Instant startTime;

	private double initialMagicXp;

	private long openTimeout;
	private long withdrawTimeout;
	private long alchTimeout;

	private int initialCoinCount;

	private int coinsIndex;
	private int natureRunesIndex;

	private int alchCount;
	private int alchsRemaining;

	private boolean shantayBanking;
	private boolean idle;

	private int itemId;

	public AA_HighLevelAlchemy(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) {
			throw new IllegalArgumentException("Missing item id parameter.");
		}

		final String[] args = parameters.split(" ");

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
				case "-i":
				case "--item":
					this.itemId = Integer.parseInt(args[++i]);
					break;
				default:
					throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
			}
		}

		if (this.getInventoryIndex(ITEM_ID_SLEEPING_BAG) != 0) {
			throw new IllegalStateException("Sleeping bag missing from 1st inv slot.");
		}

		final int staffOfFireIndex = this.getInventoryIndex(ITEM_IDS_STAFF_OF_FIRE);

		if (staffOfFireIndex != 1 || !this.isItemEquipped(staffOfFireIndex)) {
			throw new IllegalStateException("Staff of fire unequipped/missing from 2nd inv slot.");
		}

		this.natureRunesIndex = this.getInventoryIndex(ITEM_ID_NATURE_RUNE);

		if (this.natureRunesIndex != 2) {
			throw new IllegalStateException("Nature runes missing from 3rd inv slot.");
		}

		this.coinsIndex = this.getInventoryIndex(ITEM_ID_COINS);

		if (this.coinsIndex != 3) {
			throw new IllegalStateException("Coin missing from 4th inv slot.");
		}

		this.initialCoinCount = this.getInventoryStack(this.coinsIndex);
		this.shantayBanking = this.distanceTo(COORDINATE_SHANTAY_BANK_CHEST.getX(), COORDINATE_SHANTAY_BANK_CHEST.getY()) < 10;
		this.initialMagicXp = this.getAccurateXpForLevel(SKILL_INDEX_MAGIC);
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

		if (System.currentTimeMillis() <= this.alchTimeout) {
			return 0;
		}

		if (this.getInventoryId(this.natureRunesIndex) != ITEM_ID_NATURE_RUNE) {
			return this.exit("Out of nature runes.");
		}

		if (this.getInventoryCount() == INITIAL_INVENTORY_SIZE ||
			this.getInventoryId(INITIAL_INVENTORY_SIZE) != this.itemId) {
			return this.bank();
		}

		this.extension.displayMessage("@gre@Casting ...");
		this.castOnItem(SPELL_ID_HIGH_ALCHEMY, INITIAL_INVENTORY_SIZE);
		this.alchTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("Alchemy")) {
			this.alchCount++;
			if (this.alchsRemaining > 0) {
				this.alchsRemaining--;
			}
			this.alchTimeout = System.currentTimeMillis() + CAST_DELAY;
		} else if (message.endsWith("spell")) {
			this.alchTimeout = 0L;
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

		this.drawString("@yel@High Level Alchemy", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s",
				getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final double xpGained = this.getAccurateXpForLevel(SKILL_INDEX_MAGIC) - this.initialMagicXp;

		this.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), getUnitsPerHour(xpGained, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Alchs: @whi@%s @cya@(@whi@%s casts@cya@/@whi@hr@cya@)",
				this.alchCount, getUnitsPerHour(this.alchCount, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final int coins = this.getInventoryStack(this.coinsIndex) - this.initialCoinCount;

		this.drawString(String.format("@yel@Gold: @whi@%s @cya@(@whi@%s coins@cya@/@whi@hr@cya@)",
				coins, getUnitsPerHour(coins, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Remaining: @whi@%d", this.alchsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Time remaining: @whi@%s",
				getTTL(this.alchCount, this.alchsRemaining, secondsElapsed)),
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

		if (System.currentTimeMillis() <= this.withdrawTimeout) {
			return 0;
		}

		final int itemsRemaining = this.bankCount(this.itemId);

		if (itemsRemaining == 0) {
			return this.exit(String.format("Out of %ss.", getItemNameId(this.itemId)));
		}

		final int natureRunesRemaining = this.getInventoryStack(this.natureRunesIndex);

		this.alchsRemaining = Math.min(natureRunesRemaining, itemsRemaining);
		this.withdraw(this.itemId, MAX_INV_SIZE - INITIAL_INVENTORY_SIZE);
		this.withdrawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int idle() {
		if (this.getX() == COORDINATE_SHANTAY_BANK_CHEST.getX() + 2 &&
			this.getY() == COORDINATE_SHANTAY_BANK_CHEST.getY()) {
			this.idle = false;
			return 0;
		}

		this.walkTo(COORDINATE_SHANTAY_BANK_CHEST.getX() + 2, COORDINATE_SHANTAY_BANK_CHEST.getY());
		return SLEEP_ONE_TICK;
	}
}
