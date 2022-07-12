/**
 * A script for combining feathers and arrow shafts.
 * Inventory: Sleeping bag, feathers, arrow shafts.
 * Optional: Start near a bed.
 * <p>
 *
 * @Author Chomp
 */
public class AA_HeadlessArrows extends AA_Script {
	private static final int ITEM_ID_FEATHERS = 381;
	private static final int ITEM_ID_SHAFTS = 280;
	private static final int MAX_FATIGUE = 99;

	private long startTime;
	private Coordinate standCoord;
	private Coordinate idleCoord;
	private Coordinate bedCoord;

	private double initialFletchingXp;

	private long fletchTimeout;

	private int feathersIndex;
	private int shaftsIndex;

	private int arrowsMade;

	private boolean idle;

	public AA_HeadlessArrows(final Extension ex) {
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

		feathersIndex = getInventoryIndex(ITEM_ID_FEATHERS);

		if (feathersIndex == -1) {
			throw new IllegalStateException("Feathers missing from inventory.");
		}

		shaftsIndex = getInventoryIndex(ITEM_ID_SHAFTS);

		if (shaftsIndex == -1) {
			throw new IllegalStateException("Shafts missing from inventory.");
		}

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

		if (invCount <= feathersIndex || getInventoryId(feathersIndex) != ITEM_ID_FEATHERS) {
			return exit("Out of feathers.");
		}

		if (invCount <= shaftsIndex || getInventoryId(shaftsIndex) != ITEM_ID_SHAFTS) {
			return exit("Out of shafts.");
		}

		if (getX() != standCoord.getX() || getY() != standCoord.getY()) {
			walkTo(standCoord.getX(), standCoord.getY());
			return SLEEP_ONE_TICK;
		}

		bot.displayMessage("@gre@Fletching ...");
		useItemWithItem(feathersIndex, shaftsIndex);
		fletchTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("shafts")) {
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

		bot.drawString("@yel@Headless Arrows", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = bot.getExperience(Skill.FLETCHING.getIndex()) - initialFletchingXp;

		bot.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Arrows: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				arrowsMade, toUnitsPerHour(arrowsMade, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final int remaining = Math.min(bot.getInventoryStack(feathersIndex), bot.getInventoryStack(shaftsIndex));

		bot.drawString(String.format("@yel@Remaining: @whi@%d", remaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(arrowsMade, remaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}
}
