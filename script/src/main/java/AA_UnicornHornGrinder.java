/**
 * A script for grinding unicorn horns at Shantay Bank.
 * Start with pestle and mortar in inventory.
 * <p>
 *
 * @Author Chomp
 */
public class AA_UnicornHornGrinder extends AA_Script {
	private static final Coordinate COORDINATE_BANK_CHEST = new Coordinate(58, 731);

	private static final int ITEM_ID_PESTLE = 468;
	private static final int ITEM_ID_HORN = 466;

	private long startTime;

	private long depositTimeout;
	private long withdrawTimeout;
	private long grindTimeout;
	private long bankOpenTimeout;

	private int previousPlayerY;
	private int hornsGround;
	private int hornsRemaining;

	private boolean idle;

	public AA_UnicornHornGrinder(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (bot.getInventoryId(0) != ITEM_ID_PESTLE) {
			throw new IllegalStateException("Pestle and mortar missing from inventory.");
		}

		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (idle) {
			return idle();
		}

		if (bot.getInventorySize() == 1 || bot.getInventoryId(1) != ITEM_ID_HORN) {
			if (!isBanking()) {
				return openShantayBank();
			}

			if (bot.getInventorySize() == 1) {
				if (System.currentTimeMillis() <= withdrawTimeout) {
					return 0;
				}

				hornsRemaining = bankCount(ITEM_ID_HORN);

				if (hornsRemaining == 0) {
					return exit("Out of horns.");
				}

				withdraw(ITEM_ID_HORN, MAX_INV_SIZE - 1);
				withdrawTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= depositTimeout) {
				return 0;
			}

			deposit(bot.getInventoryId(1), MAX_INV_SIZE - 1);
			depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= grindTimeout) {
			return 0;
		}

		useItemWithItem(0, 1);
		grindTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("dust")) {
			hornsGround++;
			hornsRemaining--;
			grindTimeout = 0L;
		} else if (message.endsWith("area")) {
			idle = true;
			previousPlayerY = getY();
		} else {
			super.onServerMessage(message);
		}
	}

	private int idle() {
		if (getY() != previousPlayerY) {
			idle = false;
			return 0;
		}

		if (previousPlayerY == COORDINATE_BANK_CHEST.getY()) {
			walkTo(COORDINATE_BANK_CHEST.getX() + 1, COORDINATE_BANK_CHEST.getY() + 1);
		} else {
			walkTo(COORDINATE_BANK_CHEST.getX() + 1, COORDINATE_BANK_CHEST.getY());
		}

		return SLEEP_ONE_TICK;
	}

	private int openShantayBank() {
		if (System.currentTimeMillis() <= bankOpenTimeout) {
			return 0;
		}

		if (getX() - COORDINATE_BANK_CHEST.getX() > 1) {
			walkTo(COORDINATE_BANK_CHEST.getX() + 1, COORDINATE_BANK_CHEST.getY());
			return SLEEP_ONE_TICK;
		}

		useObject1(COORDINATE_BANK_CHEST.getX(), COORDINATE_BANK_CHEST.getY());
		bankOpenTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Unicorn Horn Grinder", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Dust: @whi@%d @cya@(@whi@%s horns@cya@/@whi@hr@cya@)",
				hornsGround, toUnitsPerHour(hornsGround, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		if (hornsRemaining <= 0) return;

		bot.drawString(String.format("@yel@Remaining: @whi@%d", hornsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(hornsGround, hornsRemaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}
}
