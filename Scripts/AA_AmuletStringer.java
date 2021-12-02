import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * Strings amulets with balls of wool.
 * <p>
 * Requirements:
 * Start at a bank or Shantay Pass with an empty inventory.
 * The script will try to string all amulets if no amulet parameter is provided.
 * <p>
 * Optional Parameter:
 * -a,--amulet <sapphire|emerald|ruby|diamond|dragonstone>
 * <p>
 * Author: Chomp
 */
public class AA_AmuletStringer extends AA_Script {
	private static final Coordinate COORDINATE_SHANTAY_BANK_CHEST = new Coordinate(58, 731);

	private static final long STRING_DELAY = 550L; // +- based on latency

	private static final int ITEM_ID_BALL_OF_WOOL = 207;
	private static final int WITHDRAW_COUNT = 15;

	private Amulet amulet;
	private Iterator<Amulet> iterator;
	private Instant startTime;

	private long openTimeout;
	private long depositTimeout;
	private long withdrawWoolTimeout;
	private long withdrawAmuletTimeout;
	private long stringTimeout;

	private int inventoryItemCount;

	private int amuletsStrung;
	private int materialsRemaining;

	private boolean shantayBanking;
	private boolean idle;

	public AA_AmuletStringer(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-a":
					case "--amulet":
						this.amulet = Amulet.valueOf(args[++i].toUpperCase());
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		if (this.amulet == null) {
			this.iterator = EnumSet.allOf(Amulet.class).iterator();
			this.amulet = this.iterator.next();
		}

		this.shantayBanking = this.distanceTo(COORDINATE_SHANTAY_BANK_CHEST.getX(), COORDINATE_SHANTAY_BANK_CHEST.getY()) < 10;
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		if (this.idle) {
			return this.idle();
		}

		if (System.currentTimeMillis() <= this.stringTimeout) {
			return 0;
		}

		this.inventoryItemCount = this.getInventoryCount();
		final int amuletIndex = this.getInventoryIndex(this.amulet.id);

		if (this.inventoryItemCount == 0 ||
			this.getInventoryId(0) != ITEM_ID_BALL_OF_WOOL ||
			amuletIndex == -1) {
			return this.bank();
		}

		this.extension.displayMessage("@gre@Stringing ...");
		this.useItemWithItem(0, amuletIndex);
		this.stringTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("amulet")) {
			this.amuletsStrung++;

			if (this.materialsRemaining > 0) {
				this.materialsRemaining--;
			}

			this.stringTimeout = System.currentTimeMillis() + STRING_DELAY;
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

		this.drawString("@yel@Amulet Stringer", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s amulets@cya@/@whi@hr@cya@)",
				this.amulet, this.amuletsStrung, getUnitsPerHour(this.amuletsStrung, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Remaining: @whi@%d", this.materialsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Time remaining: @whi@%s",
				getTTL(this.amuletsStrung, this.materialsRemaining, secondsElapsed)),
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

		if (this.inventoryItemCount > 0 &&
			this.getInventoryId(0) == ITEM_ID_BALL_OF_WOOL &&
			this.inventoryItemCount < MAX_INV_SIZE) {
			if (System.currentTimeMillis() <= this.withdrawAmuletTimeout) {
				return 0;
			}

			final int amuletsRemaining = this.bankCount(this.amulet.id);

			if (amuletsRemaining == 0) {
				if (this.iterator == null || !this.iterator.hasNext()) {
					return this.exit(String.format("Out of %ss.", getItemNameId(this.amulet.id)));
				}

				this.amulet = this.iterator.next();
				this.amuletsStrung = 0;
				this.startTime = Instant.now();
				return 0;
			}

			final int ballsOfWoolRemaining = this.bankCount(ITEM_ID_BALL_OF_WOOL);

			this.materialsRemaining = Math.min(amuletsRemaining, ballsOfWoolRemaining);
			this.withdraw(this.amulet.id, WITHDRAW_COUNT);
			this.withdrawAmuletTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= this.withdrawWoolTimeout) {
			return 0;
		}

		if (this.inventoryItemCount == 0) {
			if (!this.hasBankItem(ITEM_ID_BALL_OF_WOOL)) {
				return this.exit("Out of balls of wool.");
			}

			this.withdraw(ITEM_ID_BALL_OF_WOOL, WITHDRAW_COUNT);
			this.withdrawWoolTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= this.depositTimeout) {
			return 0;
		}

		final int itemId = this.getInventoryId(0);
		this.deposit(itemId, MAX_INV_SIZE);
		this.depositTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
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

	private enum Amulet {
		SAPPHIRE(297, "Sapphire"),
		EMERALD(298, "Emerald"),
		RUBY(299, "Ruby"),
		DIAMOND(300, "Diamond"),
		DRAGONSTONE(524, "Dragonstone");

		private final int id;
		private final String name;

		Amulet(final int id, final String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}
