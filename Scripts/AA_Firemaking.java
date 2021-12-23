import com.aposbot.Constants;

import java.awt.Font;
import java.time.Duration;
import java.time.Instant;

/**
 * Chops and burns logs.
 * <p>
 * Required:
 * Have sleeping bag, axe, and tinderbox in inventory.
 * Inventory must be full so that cut logs drop to the ground.
 * <p>
 * Optional Parameter:
 * -r,--radius <#> (default 10)
 * <p>
 * Author: Chomp
 */
public class AA_Firemaking extends AA_Script {
	private static final int[] ITEM_IDS_AXES = new int[]{12, 87, 88, 203, 204, 405, 428};

	private static final int ITEM_ID_LOGS = 14;
	private static final int ITEM_ID_TINDERBOX = 166;

	private static final int SKILL_INDEX_WOODCUTTING = 8;
	private static final int SKILL_INDEX_FIREMAKING = 11;

	private static final int MAXIMUM_FATIGUE = 99;

	private final int[] nearestTile = new int[]{-1, -1};
	private final int[] nearestTree = new int[]{-1, -1};
	private final int[] previousTree = new int[]{-1, -1};

	private Instant startTime;

	private double initialWoodcuttingXp;
	private double initialFiremakingXp;

	private long burnTimeout;
	private long swingTimeout;

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
		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-r":
					case "--radius":
						this.radius = Integer.parseInt(args[++i]);
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		boolean noAxe = true;

		for (final int itemId : ITEM_IDS_AXES) {
			if (this.hasInventoryItem(itemId)) {
				noAxe = false;
				break;
			}
		}

		if (noAxe) {
			throw new IllegalStateException("Axe missing from inventory.");
		}

		this.tinderboxIndex = this.getInventoryIndex(ITEM_ID_TINDERBOX);

		if (this.tinderboxIndex == -1) {
			throw new IllegalStateException("Tinderbox missing from inventory.");
		}

		this.initialFiremakingXp = this.getAccurateXpForLevel(SKILL_INDEX_FIREMAKING);
		this.initialWoodcuttingXp = this.getAccurateXpForLevel(SKILL_INDEX_WOODCUTTING);

		this.startX = this.getX();
		this.startY = this.getY();

		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		if (this.getFatigue() >= MAXIMUM_FATIGUE) {
			return this.sleep();
		}

		this.playerX = this.getX();
		this.playerY = this.getY();

		if (!this.isObjectAt(this.playerX, this.playerY) &&
			this.isItemAt(ITEM_ID_LOGS, this.playerX, this.playerY)) {
			if (System.currentTimeMillis() <= this.burnTimeout) {
				return 0;
			}

			return this.burnLogs();
		}

		if (System.currentTimeMillis() <= this.swingTimeout) {
			return 0;
		}

		this.updateNearestTree();

		if (this.nearestTree[0] != -1) {
			if (this.playerX != this.nearestTile[0] || this.playerY != this.nearestTile[1]) {
				this.walkTo(this.nearestTile[0], this.nearestTile[1]);
				return SLEEP_ONE_TICK;
			}

			return this.chopTree();
		}

		if (this.playerX != this.startX || this.playerY != this.startY) {
			this.walkTo(this.startX, this.startY);
			return SLEEP_ONE_TICK;
		}

		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("You swing")) {
			this.swingTimeout += TIMEOUT_TWO_SECONDS;
		} else if (message.startsWith("You slip")) {
			this.swingTimeout = 0L;
		} else if (message.endsWith("wood")) {
			this.logsChopped++;
			this.previousTree[0] = this.nearestTree[0];
			this.previousTree[1] = this.nearestTree[1];
			this.swingTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.startsWith("You attempt")) {
			this.burnTimeout += TIMEOUT_TWO_SECONDS;
		} else if (message.endsWith("You fail")) {
			this.burnTimeout = 0L;
		} else if (message.endsWith("burn")) {
			this.logsBurned++;
			this.burnTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("happens") || message.endsWith("here")) {
			this.swingTimeout = 0L;
			this.burnTimeout = 0L;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Firemaking", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final double woodcutXpGained = this.getAccurateXpForLevel(SKILL_INDEX_WOODCUTTING) - this.initialWoodcuttingXp;

		this.drawString(String.format("@gr3@Wc Xp: @whi@%s @gr3@(@whi@%s xp@gr3@/@whi@hr@gr3@)",
				DECIMAL_FORMAT.format(woodcutXpGained), getUnitsPerHour(woodcutXpGained, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final double firemakingXpGained = this.getAccurateXpForLevel(SKILL_INDEX_FIREMAKING) - this.initialFiremakingXp;

		this.drawString(String.format("@or1@Fm Xp: @whi@%s @or1@(@whi@%s xp@or1@/@whi@hr@or1@)",
				DECIMAL_FORMAT.format(firemakingXpGained), getUnitsPerHour(firemakingXpGained, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@or1@Burned: @whi@%s @or1@(@whi@%s logs@or1@/@whi@hr@or1@)",
				DECIMAL_FORMAT.format(this.logsBurned), getUnitsPerHour(this.logsBurned, secondsElapsed)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
	}

	@Override
	public void walkTo(final int x, final int y) {
		final int destLX = x - this.extension.getAreaX();
		final int destLY = y - this.extension.getAreaY();
		this.extension.walkDirectly(destLX, destLY, false);
		this.extension.setActionInd(24);
	}

	private int burnLogs() {
		this.extension.displayMessage("@or1@Burn...");

		this.extension.createPacket(Constants.OP_GITEM_USEWITH);
		this.extension.put2(this.playerX);
		this.extension.put2(this.playerY);
		this.extension.put2(ITEM_ID_LOGS);
		this.extension.put2(this.tinderboxIndex);
		this.extension.finishPacket();

		this.burnTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		return 0;
	}

	private int chopTree() {
		this.extension.displayMessage("@gre@Chop...");

		this.extension.createPacket(Constants.OP_OBJECT_ACTION1);
		this.extension.put2(this.nearestTree[0]);
		this.extension.put2(this.nearestTree[1]);
		this.extension.finishPacket();

		this.swingTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		return 0;
	}

	private void updateNearestTree() {
		this.nearestTree[0] = -1;
		this.nearestTile[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < this.getObjectCount(); index++) {
			final int objectId = this.getObjectId(index);

			if (objectId > 1) {
				continue;
			}

			final int treeX = this.getObjectX(index);
			final int treeY = this.getObjectY(index);

			if ((treeX == this.previousTree[0] && treeY == this.previousTree[1]) ||
				distanceTo(this.startX, this.startY, treeX, treeY) > this.radius) {
				continue;
			}

			final int distance = distanceTo(this.playerX, this.playerY, treeX, treeY);

			if (distance >= currentDistance) {
				continue;
			}

			this.updateNearestTile(treeX, treeY);

			if (this.nearestTile[0] == -1) {
				continue;
			}

			this.nearestTree[0] = treeX;
			this.nearestTree[1] = treeY;

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

				if (this.isObjectAt(x, y)) {
					continue;
				}

				if (!this.isReachable(x, y)) {
					continue;
				}

				final int distance = distanceTo(this.playerX, this.playerY, x, y);

				if (distance >= currentDistance) {
					continue;
				}

				this.nearestTile[0] = x;
				this.nearestTile[1] = y;

				currentDistance = distance;
			}
		}
	}
}
