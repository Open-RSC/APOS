/**
 * A script for fly fishing.
 * Start script standing beside a fly fishing spot.
 * <p>
 * Inventory: Sleeping Bag, Fly Fishing Rod, Feathers
 * <p>
 *
 * @author Chomp
 */
public class AA_FlyFishing extends AA_Script {
	private static final int OBJECT_ID_FISHING_SPOT = 192;

	private static final int ITEM_ID_FEATHERS = 381;
	private static final int ITEM_ID_FLY_FISHING_ROD = 378;

	private static final int MAXIMUM_FATIGUE = 100;

	private long startTime;

	private Coordinate idleCoord;
	private Coordinate fishingSpotCoordinate;
	private Coordinate standSpotCoordinate;

	private double initialFishingXp;
	private double gainedFishingXp;
	private double totalFishCaught;
	private double fishAttempts;

	private long fishingTimeout;

	private int feathersIndex;
	private int salmonCaught;
	private int troutCaught;
	private int failCount;
	private int feathersUsed;

	private boolean idle;

	public AA_FlyFishing(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (!isLoggedIn()) {
			throw new IllegalStateException("Must be logged in to start this script.");
		}

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!hasInventoryItem(ITEM_ID_FLY_FISHING_ROD)) {
			throw new IllegalStateException("Fly fishing rod missing from inventory.");
		}

		feathersIndex = getInventoryIndex(ITEM_ID_FEATHERS);

		if (feathersIndex == -1) {
			throw new IllegalStateException("Feathers missing from inventory.");
		}

		final int[] fishingSpot = getObjectById(OBJECT_ID_FISHING_SPOT);

		if (fishingSpot[0] == -1 || distanceTo(fishingSpot[1], fishingSpot[2]) > 1) {
			throw new IllegalStateException("Stand beside the fishing spot.");
		}

		fishingSpotCoordinate = new Coordinate(fishingSpot[1], fishingSpot[2]);
		standSpotCoordinate = new Coordinate(getX(), getY());
		initialFishingXp = getAccurateXpForLevel(Skill.FISHING.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (getFatigue() >= MAXIMUM_FATIGUE) {
			if (isSkilling()) {
				return 0;
			}

			final int index = getInventoryIndex(ITEM_ID_SLEEPING_BAG);

			if (index == -1) {
				System.err.println("Sleeping bag missing from inventory.");
				stopScript();
				return 0;
			}

			useItem(index);
			return 1000;
		}

		if (idle) {
			return idle();
		}

		if (System.currentTimeMillis() <= fishingTimeout) {
			return 0;
		}

		if (getInventoryId(feathersIndex) != ITEM_ID_FEATHERS) {
			System.err.println("Out of feathers.");
			stopScript();
			return 0;
		}

		if (getX() != standSpotCoordinate.getX() ||
			getY() != standSpotCoordinate.getY()) {
			walkTo(standSpotCoordinate.getX(), standSpotCoordinate.getY());
			return SLEEP_ONE_TICK;
		}

		bot.displayMessage("@cya@Casting ...");
		useObject1(fishingSpotCoordinate.getX(), fishingSpotCoordinate.getY());
		fishingTimeout = System.currentTimeMillis() + 3000L;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("attempt", 4)) {
			fishAttempts++;
			fishingTimeout = System.currentTimeMillis() + 5000L;
		} else if (message.startsWith("catch", 4)) {
			if (message.endsWith("salmon")) {
				salmonCaught++;
			} else {
				troutCaught++;
			}

			feathersUsed++;
			totalFishCaught++;
			gainedFishingXp = getAccurateXpForLevel(Skill.FISHING.getIndex()) - initialFishingXp;
			fishingTimeout = 0L;
		} else if (message.startsWith("fail", 4)) {
			failCount++;
			fishingTimeout = 0L;
		} else if (message.endsWith("area")) {
			idleCoord = getWalkableCoordinate();
			idle = true;
		}
	}

	private int idle() {
		if (idleCoord == null || getX() != standSpotCoordinate.getX() || getY() != standSpotCoordinate.getY()) {
			idle = false;
			return 0;
		}

		walkTo(idleCoord.getX(), idleCoord.getY());
		return SLEEP_ONE_TICK;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Fly Fishing", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(gainedFishingXp), toUnitsPerHour((int) gainedFishingXp, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0x00AEFF);

		bot.drawString(String.format("Caught: @whi@%s @cya@(@whi@%s fish@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(feathersUsed), toUnitsPerHour(feathersUsed, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0x00AEFF);

		if (totalFishCaught > 0) {
			bot.drawString(String.format("Salmon: @whi@%d @cya@(@whi@%.2f%%@cya@)",
					salmonCaught, (salmonCaught / totalFishCaught) * 100),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0xCE8181);

			bot.drawString(String.format("Trout: @whi@%d @cya@(@whi@%.2f%%@cya@)",
					troutCaught, (troutCaught / totalFishCaught) * 100),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0xCECECE);

			bot.drawString(String.format("@gr1@Success Rate: @whi@%.2f%%", (totalFishCaught / fishAttempts) * 100),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

			bot.drawString(String.format("Fail Rate: @whi@%.2f%%", (failCount / fishAttempts) * 100),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0xE0142D);
		}

		final int feathersRemaining = bot.getInventoryStack(feathersIndex);

		bot.drawString(String.format("Feathers: @whi@%d", feathersRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0xC160FF);

		bot.drawString(String.format("Time Remaining: @whi@%s",
				toTimeToCompletion(feathersUsed, feathersRemaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0xC160FF);
	}
}
