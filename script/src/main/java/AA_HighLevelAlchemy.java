/**
 * Casts high level alchemy on an item.
 * <p>
 * Requirements:
 * Start at a bank or Shantay Pass with sleeping bag, staff of fire, nature runes, and coins in inventory.
 * <p>
 * Required Parameter:
 * <itemId>
 * <p>
 *
 * @Author Chomp
 */
public class AA_HighLevelAlchemy extends AA_Script {
	private static final Coordinate COORDINATE_SHANTAY_BANK_CHEST = new Coordinate(58, 731);

	private static final long CAST_DELAY = 1280L; // +- based on latency

	private static final int[] ITEM_IDS_STAFF_OF_FIRE = new int[]{197, 615, 682};

	private static final int SPELL_ID_HIGH_ALCHEMY = 28;

	private static final int ITEM_ID_NATURE_RUNE = 40;
	private static final int ITEM_ID_COINS = 10;

	private static final int MAXIMUM_FATIGUE = 99;
	private static final int INITIAL_INVENTORY_SIZE = 4;

	private long startTime;

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
		if (parameters.isEmpty()) printInstructions();

		itemId = Integer.parseInt(parameters);

		if (getInventoryIndex(ITEM_ID_SLEEPING_BAG) != 0) {
			throw new IllegalStateException("Sleeping bag missing from 1st inv slot.");
		}

		final int staffOfFireIndex = getInventoryIndex(ITEM_IDS_STAFF_OF_FIRE);

		if (staffOfFireIndex != 1 || !isItemEquipped(staffOfFireIndex)) {
			throw new IllegalStateException("Staff of fire unequipped/missing from 2nd inv slot.");
		}

		natureRunesIndex = getInventoryIndex(ITEM_ID_NATURE_RUNE);

		if (natureRunesIndex != 2) {
			throw new IllegalStateException("Nature runes missing from 3rd inv slot.");
		}

		coinsIndex = getInventoryIndex(ITEM_ID_COINS);

		if (coinsIndex != 3) {
			throw new IllegalStateException("Coin missing from 4th inv slot.");
		}

		initialCoinCount = getInventoryStack(coinsIndex);
		shantayBanking = distanceTo(COORDINATE_SHANTAY_BANK_CHEST.getX(), COORDINATE_SHANTAY_BANK_CHEST.getY()) < 10;
		initialMagicXp = getAccurateXpForLevel(Skill.MAGIC.getIndex());
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

		if (System.currentTimeMillis() <= alchTimeout) {
			return 0;
		}

		if (getInventoryId(natureRunesIndex) != ITEM_ID_NATURE_RUNE) {
			return exit("Out of nature runes.");
		}

		if (getInventoryCount() == INITIAL_INVENTORY_SIZE ||
			getInventoryId(INITIAL_INVENTORY_SIZE) != itemId) {
			return bank();
		}

		bot.displayMessage("@gre@Casting ...");
		castOnItem(SPELL_ID_HIGH_ALCHEMY, INITIAL_INVENTORY_SIZE);
		alchTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("Alchemy")) {
			alchCount++;
			if (alchsRemaining > 0) {
				alchsRemaining--;
			}
			alchTimeout = System.currentTimeMillis() + CAST_DELAY;
		} else if (message.endsWith("spell")) {
			alchTimeout = 0L;
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
		if (getX() == COORDINATE_SHANTAY_BANK_CHEST.getX() + 2 &&
			getY() == COORDINATE_SHANTAY_BANK_CHEST.getY()) {
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

		if (System.currentTimeMillis() <= withdrawTimeout) {
			return 0;
		}

		final int itemsRemaining = bankCount(itemId);

		if (itemsRemaining == 0) {
			return exit(String.format("Out of %ss.", getItemNameId(itemId)));
		}

		final int natureRunesRemaining = getInventoryStack(natureRunesIndex);

		alchsRemaining = Math.min(natureRunesRemaining, itemsRemaining);
		withdraw(itemId, MAX_INV_SIZE - INITIAL_INVENTORY_SIZE);
		withdrawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@High Level Alchemy", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s",
				toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getAccurateXpForLevel(Skill.MAGIC.getIndex()) - initialMagicXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@Alchs: @whi@%s @cya@(@whi@%s casts@cya@/@whi@hr@cya@)",
				alchCount, toUnitsPerHour(alchCount, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final int coins = getInventoryStack(coinsIndex) - initialCoinCount;

		drawString(String.format("@yel@Gold: @whi@%s @cya@(@whi@%s coins@cya@/@whi@hr@cya@)",
				coins, toUnitsPerHour(coins, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Remaining: @whi@%d", alchsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(alchCount, alchsRemaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}
}
