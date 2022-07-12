import com.aposbot.Constants;

/**
 * Chops and burns logs.
 * <p>
 * Required:
 * Have sleeping bag, axe, and tinderbox in inventory.
 * Inventory must be full so that cut logs drop to the ground.
 * <p>
 * Optional Radius Parameter:
 * <#> (default 10 radius)
 * <p>
 *
 * @Author Chomp
 */
public class AA_Firemaking extends AA_Script {
	private static final int[] ITEM_IDS_AXES = new int[]{12, 87, 88, 203, 204, 405, 428};

	private static final int ITEM_ID_LOGS = 14;
	private static final int ITEM_ID_TINDERBOX = 166;

	private static final int MAXIMUM_FATIGUE = 99;

	private final int[] nearestTile = new int[]{-1, -1};
	private final int[] nearestTree = new int[]{-1, -1};
	private final int[] previousTree = new int[]{-1, -1};

	private long startTime;

	private double initialWoodcuttingXp;
	private double initialFiremakingXp;

	private long burnTimeout;
	private long swingTimeout;
	private long walkTimeout;

	private int radius = 10;

	private int startX;
	private int startY;

	private int playerX;
	private int playerY;

	private int tinderboxIndex;

	private int logsChopped;
	private int logsBurned;

	public AA_Firemaking(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) radius = Integer.parseInt(parameters);

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!hasInventoryItem(ITEM_IDS_AXES)) {
			throw new IllegalStateException("Axe missing from inventory.");
		}

		tinderboxIndex = getInventoryIndex(ITEM_ID_TINDERBOX);

		if (tinderboxIndex == -1) {
			throw new IllegalStateException("Tinderbox missing from inventory.");
		}

		initialFiremakingXp = getAccurateXpForLevel(Skill.FIREMAKING.getIndex());
		initialWoodcuttingXp = getAccurateXpForLevel(Skill.WOODCUT.getIndex());

		startX = getX();
		startY = getY();

		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (getFatigue() >= MAXIMUM_FATIGUE) {
			return sleep();
		}

		playerX = getX();
		playerY = getY();

		if (!isObjectAt(playerX, playerY) &&
			isItemAt(ITEM_ID_LOGS, playerX, playerY)) {
			if (System.currentTimeMillis() <= burnTimeout) {
				return 0;
			}

			bot.displayMessage("@or1@Burn...");

			bot.createPacket(Constants.OP_GITEM_USEWITH);
			bot.put2(playerX);
			bot.put2(playerY);
			bot.put2(ITEM_ID_LOGS);
			bot.put2(tinderboxIndex);
			bot.finishPacket();

			burnTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
			return 0;
		}

		if (System.currentTimeMillis() <= swingTimeout) {
			return 0;
		}

		updateNearestTree();

		if (nearestTree[0] != -1) {
			if (playerX != nearestTile[0] || playerY != nearestTile[1]) {
				if (System.currentTimeMillis() <= walkTimeout) {
					return 0;
				}

				walkTo(nearestTile[0], nearestTile[1]);
				walkTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
				return 0;
			}

			bot.displayMessage("@gre@Chop...");
			useObject1(nearestTree[0], nearestTree[1]);
			swingTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
			return 0;
		}

		if (playerX != startX || playerY != startY) {
			walkTo(startX, startY);
			return SLEEP_ONE_TICK;
		}

		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("You swing")) {
			swingTimeout += TIMEOUT_TWO_SECONDS;
		} else if (message.startsWith("You slip")) {
			swingTimeout = 0L;
		} else if (message.endsWith("wood")) {
			logsChopped++;
			previousTree[0] = nearestTree[0];
			previousTree[1] = nearestTree[1];
			swingTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.startsWith("You attempt")) {
			burnTimeout += TIMEOUT_TWO_SECONDS;
		} else if (message.endsWith("You fail")) {
			burnTimeout = 0L;
		} else if (message.endsWith("burn")) {
			logsBurned++;
			burnTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("happens") || message.endsWith("here")) {
			swingTimeout = 0L;
			burnTimeout = 0L;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void walkTo(final int x, final int y) {
		final int destLX = x - bot.getAreaX();
		final int destLY = y - bot.getAreaY();
		bot.walkDirectly(destLX, destLY, false);
		bot.setActionInd(24);
	}

	private void updateNearestTree() {
		nearestTree[0] = -1;
		nearestTile[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < getObjectCount(); index++) {
			final int objectId = getObjectId(index);

			if (objectId > 1) {
				continue;
			}

			final int treeX = getObjectX(index);
			final int treeY = getObjectY(index);

			if ((treeX == previousTree[0] && treeY == previousTree[1]) ||
				distanceTo(startX, startY, treeX, treeY) > radius) {
				continue;
			}

			final int distance = distanceTo(playerX, playerY, treeX, treeY);

			if (distance >= currentDistance) {
				continue;
			}

			updateNearestTile(treeX, treeY);

			if (nearestTile[0] == -1) {
				continue;
			}

			nearestTree[0] = treeX;
			nearestTree[1] = treeY;

			currentDistance = distance;
		}
	}

	private void updateNearestTile(final int treeX, final int treeY) {
		int currentDistance = Integer.MAX_VALUE;

		int x;
		int y;

		for (int i = 1; i >= -1; i--) {
			x = treeX + i;

			for (int j = 1; j >= -1; j--) {
				y = treeY + j;

				if (isObjectAt(x, y)) {
					continue;
				}

				if (!isReachable(x, y)) {
					continue;
				}

				final int distance = distanceTo(playerX, playerY, x, y);

				if (distance >= currentDistance) {
					continue;
				}

				nearestTile[0] = x;
				nearestTile[1] = y;

				currentDistance = distance;
			}
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Firemaking", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double woodcutXpGained = getAccurateXpForLevel(Skill.WOODCUT.getIndex()) - initialWoodcuttingXp;

		drawString(String.format("@gr3@Wc Xp: @whi@%s @gr3@(@whi@%s xp@gr3@/@whi@hr@gr3@)",
				DECIMAL_FORMAT.format(woodcutXpGained), toUnitsPerHour((int) woodcutXpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		final double firemakingXpGained = getAccurateXpForLevel(Skill.FIREMAKING.getIndex()) - initialFiremakingXp;

		drawString(String.format("@or1@Fm Xp: @whi@%s @or1@(@whi@%s xp@or1@/@whi@hr@or1@)",
				DECIMAL_FORMAT.format(firemakingXpGained), toUnitsPerHour((int) firemakingXpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@or1@Burned: @whi@%s @or1@(@whi@%s logs@or1@/@whi@hr@or1@)",
				DECIMAL_FORMAT.format(logsBurned), toUnitsPerHour(logsBurned, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}
}
