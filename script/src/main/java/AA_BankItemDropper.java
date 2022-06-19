import java.util.Arrays;

/**
 * Withdraws and drops item from bank.
 * Start at Shantay Pass or at a Banker.
 * <p>
 * Parameters:
 * id1,id2,id3
 * <p>
 *
 * @Author Chomp
 */
public class AA_BankItemDropper extends AA_Script {
	private static final Coordinate COORDINATE_BANK_CHEST = new Coordinate(58, 731);

	private static final int[] ITEM_IDS_EDIBLE = new int[]{
		350, 352, 355, 362, 718, 553, 359, 551, 364, 357, 367, 373, 555, 370, 590, 1193, 546, 1191
	};

	private long startTime;

	private long bankOpenTimeout;
	private long bankCloseTimeout;
	private long withdrawTimeout;
	private long eatTimeout;

	private int itemId;
	private int itemIdIndex;
	private int itemsRemaining;

	private int previousPlayerY;

	private boolean idle;
	private boolean shantayBanking;

	private int[] itemIds;

	private boolean eat;

	public AA_BankItemDropper(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) printInstructions();

		itemIds = Arrays.stream(parameters.split(",")).mapToInt(Integer::parseInt).toArray();
		itemId = itemIds[itemIdIndex];
		shantayBanking = distanceTo(COORDINATE_BANK_CHEST.getX(), COORDINATE_BANK_CHEST.getY()) < 10;
		eat = isInventoryEmpty() && inArray(ITEM_IDS_EDIBLE, itemId);
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (isInventoryEmpty() || isBanking()) {
			return bank();
		}

		if (idle) {
			return idle();
		}

		if (eat) {
			if (System.currentTimeMillis() <= eatTimeout) {
				return 0;
			}

			useItem(0);
			eatTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		dropItem(0);
		return SLEEP_ONE_TICK;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("men.") || message.endsWith("you.")) {
			bankOpenTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		} else if (message.startsWith("eat", 4)) {
			eatTimeout = 0L;
			itemsRemaining--;
		} else if (message.endsWith("area") && shantayBanking) {
			idle = true;
			previousPlayerY = getY();
		} else {
			super.onServerMessage(message);
		}
	}

	private int bank() {
		if (!isBanking()) {
			if (shantayBanking) {
				return openShantayBank();
			}

			return openBank();
		}

		if (isInventoryEmpty()) {
			if (System.currentTimeMillis() <= withdrawTimeout) {
				return 0;
			}

			itemsRemaining = bankCount(itemId);

			if (itemsRemaining == 0) {
				return getNextItem();
			}

			withdraw(itemId, MAX_INV_SIZE);
			withdrawTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= bankCloseTimeout) {
			return 0;
		}

		closeBank();
		bankCloseTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int idle() {
		if (getY() != previousPlayerY) {
			idle = false;
			return 0;
		}

		if (!shantayBanking) {
			walkTo(getX(), getY() + 1);
			return SLEEP_ONE_TICK;
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

	private int getNextItem() {
		if (++itemIdIndex == itemIds.length) {
			return exit("Out of items.");
		}

		itemId = itemIds[itemIdIndex];
		eat = inArray(ITEM_IDS_EDIBLE, itemId);
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Bank Item Dropper", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Item: @whi@%s", getItemNameId(itemId)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (itemsRemaining > 0) {
			bot.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

			bot.drawString(String.format("@yel@Remaining: @whi@%d", itemsRemaining),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}
	}
}
