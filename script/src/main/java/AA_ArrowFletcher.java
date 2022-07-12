/**
 * A script for combining arrow heads and headless arrows.
 * Inventory: Sleeping bag, arrow heads, headless arrows.
 * Optional: Start near a bed.
 * <p>
 *
 * @Author Chomp
 */
public class AA_ArrowFletcher extends AA_Script {
	private static final int[] ITEM_IDS_ARROW_HEAD = new int[]{669, 670, 671, 672, 673, 674};

	private static final int ITEM_ID_HEADLESS_ARROW = 637;
	private static final int MAX_FATIGUE = 97;

	private long startTime;

	private Coordinate idleCoord;
	private Coordinate standCoord;
	private Coordinate bedCoord;

	private double initialFletchingXp;

	private long fletchTimeout;

	private int arrowHeadsId;
	private int arrowHeadsIndex;
	private int headlessArrowsIndex;

	private int arrowsMade;

	private boolean idle;

	public AA_ArrowFletcher(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (!bot.isLoggedIn()) {
			throw new IllegalStateException("Must be logged in to start this script.");
		}

		standCoord = new Coordinate(getX(), getY());
		idleCoord = getWalkableCoordinate();

		if (idleCoord == null) {
			throw new IllegalStateException("Unable to find a walkable tile to move to when idle.");
		}

		headlessArrowsIndex = getInventoryIndex(ITEM_ID_HEADLESS_ARROW);

		if (headlessArrowsIndex == -1) {
			throw new IllegalStateException("Headless arrows missing from inventory.");
		}

		arrowHeadsIndex = getInventoryIndex(ITEM_IDS_ARROW_HEAD);

		if (arrowHeadsIndex == -1) {
			throw new IllegalStateException("Arrow heads missing from inventory.");
		}

		arrowHeadsId = getInventoryId(arrowHeadsIndex);

		final int[] bed = getObjectById(OBJECT_IDS_BED);

		if (bed == null || distanceTo(bed[1], bed[2]) > 5) {
			if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
				throw new IllegalStateException("Sleeping bag missing from inventory.");
			}
		} else {
			bedCoord = new Coordinate(bed[1], bed[2]);
		}

		initialFletchingXp = bot.getExperience(Skill.FLETCHING.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (idle) {
			return idle();
		}

		if (System.currentTimeMillis() <= fletchTimeout) {
			return 0;
		}

		if (getFatigue() >= MAX_FATIGUE) {
			if (bedCoord == null) {
				return sleep();
			}

			atObject(bedCoord.getX(), bedCoord.getY());
			return SLEEP_TWO_SECONDS;
		}

		final int invCount = bot.getInventorySize();

		if (invCount <= arrowHeadsIndex || getInventoryId(arrowHeadsIndex) != arrowHeadsId) {
			return exit("Out of arrow heads.");
		}

		if (invCount <= headlessArrowsIndex || getInventoryId(headlessArrowsIndex) != ITEM_ID_HEADLESS_ARROW) {
			return exit("Out of headless arrows.");
		}

		if (getX() != standCoord.getX() || getY() != standCoord.getY()) {
			walkTo(standCoord.getX(), standCoord.getY());
			return SLEEP_ONE_TICK;
		}

		bot.displayMessage("@gre@Fletching ...");
		useItemWithItem(arrowHeadsIndex, headlessArrowsIndex);
		fletchTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("arrows")) {
			arrowsMade += 10;
			fletchTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("area")) {
			idle = true;
		} else {
			super.onServerMessage(message);
		}
	}

	private int idle() {
		if (getX() != standCoord.getX() || getY() != standCoord.getY()) {
			idle = false;
			return 0;
		}

		walkTo(idleCoord.getX(), idleCoord.getY());
		return SLEEP_ONE_TICK;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Arrow Heads", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = bot.getExperience(Skill.FLETCHING.getIndex()) - initialFletchingXp;

		bot.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Heads: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				arrowsMade, toUnitsPerHour(arrowsMade, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final int remaining = Math.min(bot.getInventoryStack(arrowHeadsIndex), bot.getInventoryStack(headlessArrowsIndex));

		bot.drawString(String.format("@yel@Remaining: @whi@%d", remaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(arrowsMade, remaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}
}
