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
 * <sapphire|emerald|ruby|diamond|dragonstone>
 * <p>
 *
 * @Author Chomp
 */
public class AA_AmuletStringer extends AA_Script {
	private static final Coordinate COORD_CHEST = new Coordinate(58, 731);

	private static final long STRING_DELAY = 550L; // +- based on latency

	private static final int ITEM_ID_BALL_OF_WOOL = 207;
	private static final int WITHDRAW_COUNT = 15;

	private Amulet amulet;
	private Iterator<Amulet> iterator;
	private long startTime;

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
		if (parameters.isEmpty()) {
			iterator = EnumSet.allOf(Amulet.class).iterator();
			amulet = iterator.next();
		} else {
			amulet = Amulet.valueOf(parameters.toUpperCase());
		}

		shantayBanking = distanceTo(COORD_CHEST.getX(), COORD_CHEST.getY()) < 10;
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (idle) {
			return idle();
		}

		if (System.currentTimeMillis() <= stringTimeout) {
			return 0;
		}

		inventoryItemCount = getInventoryCount();
		final int amuletIndex = getInventoryIndex(amulet.id);

		if (inventoryItemCount == 0 ||
			getInventoryId(0) != ITEM_ID_BALL_OF_WOOL ||
			amuletIndex == -1) {
			return bank();
		}

		bot.displayMessage("@gre@Stringing ...");
		useItemWithItem(0, amuletIndex);
		stringTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("amulet")) {
			amuletsStrung++;

			if (materialsRemaining > 0) {
				materialsRemaining--;
			}

			stringTimeout = System.currentTimeMillis() + STRING_DELAY;
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
		if (getX() == COORD_CHEST.getX() + 2 &&
			getY() == COORD_CHEST.getY()) {
			idle = false;
			return 0;
		}

		walkTo(COORD_CHEST.getX() + 2, COORD_CHEST.getY());
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (!isBanking()) {
			if (shantayBanking) {
				if (System.currentTimeMillis() <= openTimeout) {
					return 0;
				}

				atObject(COORD_CHEST.getX(), COORD_CHEST.getY());
				openTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			return openBank();
		}

		if (inventoryItemCount > 0 &&
			getInventoryId(0) == ITEM_ID_BALL_OF_WOOL &&
			inventoryItemCount < MAX_INV_SIZE) {
			if (System.currentTimeMillis() <= withdrawAmuletTimeout) {
				return 0;
			}

			final int amuletsRemaining = bankCount(amulet.id);

			if (amuletsRemaining == 0) {
				if (iterator == null || !iterator.hasNext()) {
					return exit(String.format("Out of %ss.", getItemNameId(amulet.id)));
				}

				amulet = iterator.next();
				amuletsStrung = 0;
				startTime = System.currentTimeMillis();
				return 0;
			}

			final int ballsOfWoolRemaining = bankCount(ITEM_ID_BALL_OF_WOOL);

			materialsRemaining = Math.min(amuletsRemaining, ballsOfWoolRemaining);
			withdraw(amulet.id, WITHDRAW_COUNT);
			withdrawAmuletTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= withdrawWoolTimeout) {
			return 0;
		}

		if (inventoryItemCount == 0) {
			if (!hasBankItem(ITEM_ID_BALL_OF_WOOL)) {
				return exit("Out of balls of wool.");
			}

			withdraw(ITEM_ID_BALL_OF_WOOL, WITHDRAW_COUNT);
			withdrawWoolTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= depositTimeout) {
			return 0;
		}

		final int itemId = getInventoryId(0);
		deposit(itemId, MAX_INV_SIZE);
		depositTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Amulet Stringer", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s amulets@cya@/@whi@hr@cya@)",
				amulet, amuletsStrung, toUnitsPerHour(amuletsStrung, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Remaining: @whi@%d", materialsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(amuletsStrung, materialsRemaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
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
			return name;
		}
	}
}
