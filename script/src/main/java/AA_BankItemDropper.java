import com.aposbot.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Withdraws items from bank and drops them.
 * Start script at Shantay Bank Chest.
 * If no parameters are passed, all items will be dropped.
 * <p>
 * Parameters (Optional)
 * id1,id2,id3...
 * <p>
 *
 * @Author Chomp
 */
public class AA_BankItemDropper extends AA_Script {
	private static final Coordinate COORDINATE_BANK_CHEST = new Coordinate(58, 731);

	private Iterator<Integer> itemIds;

	private long startTime;

	private long bankOpenTimeout;

	private int itemId;
	private int remaining;
	private int dropped;
	private int prevY;

	private boolean idle;
	private boolean initialized;

	public AA_BankItemDropper(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			itemIds = Arrays.stream(parameters.split(",")).mapToInt(Integer::parseInt).iterator();
			itemId = itemIds.next();
			initialized = true;
		}

		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (isInventoryEmpty()) return bank();
		if (idle) return idle();
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Bank Item Dropper", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Item: @whi@%s", getItemName(itemId)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		if (remaining <= 0) return;

		bot.drawString(String.format("@yel@Remaining: @whi@%d", remaining),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("men.") || message.endsWith("you.")) {
			bankOpenTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		} else if (message.endsWith("area")) {
			idle = true;
			prevY = getPlayerY();
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void onPlayerCoord(final int x, final int y) {
		if (isInventoryEmpty() || idle) return;
		dropItem(0);
	}

	private int bank() {
		if (!isBankOpen()) return openShantayBank();

		if (!initialized) parseBank();

		remaining = getBankItemIdCount(itemId);

		if (remaining == 0) {
			if (!itemIds.hasNext()) return exit("Out of items to drop.");
			itemId = itemIds.next();
			return 0;
		}

		final int withdrawAmount = Math.min(remaining, MAX_INVENTORY_SIZE);
		withdraw(itemId, withdrawAmount);
		closeBank();
		dropped += withdrawAmount;
		return SLEEP_THREE_SECONDS;
	}

	private int openShantayBank() {
		if (System.currentTimeMillis() <= bankOpenTimeout) return 0;

		if (getPlayerX() - COORDINATE_BANK_CHEST.getX() > 1) {
			walkTo(COORDINATE_BANK_CHEST.getX() + 1, COORDINATE_BANK_CHEST.getY());
			return SLEEP_ONE_TICK;
		}

		bot.createPacket(Constants.OP_OBJECT_ACTION1);
		bot.put2(COORDINATE_BANK_CHEST.getX());
		bot.put2(COORDINATE_BANK_CHEST.getY());
		bot.finishPacket();

		bankOpenTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private void parseBank() {
		if (bot.getBankSize() == 0) {
			exit("Bank is empty!");
			return;
		}

		final List<Integer> itemIds = new ArrayList<>();

		for (int i = 0; i < bot.getBankSize(); i++) {
			itemIds.add(bot.getBankId(i));
		}

		this.itemIds = itemIds.iterator();
		itemId = this.itemIds.next();
		initialized = true;
	}

	private int idle() {
		if (getPlayerY() != prevY) {
			idle = false;
			return 0;
		}

		if (prevY == COORDINATE_BANK_CHEST.getY()) {
			walkTo(COORDINATE_BANK_CHEST.getX() + 1, COORDINATE_BANK_CHEST.getY() + 1);
		} else {
			walkTo(COORDINATE_BANK_CHEST.getX() + 1, COORDINATE_BANK_CHEST.getY());
		}

		return SLEEP_ONE_TICK;
	}
}
