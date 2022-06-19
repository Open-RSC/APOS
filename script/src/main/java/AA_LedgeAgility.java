/**
 * A script for crossing the Yanille Agility Dungeon ledge.
 * Start script at the ledge with sleeping bag in inventory.
 * <p>
 *
 * @Author Chomp
 */
public class AA_LedgeAgility extends AA_Script {
	private double initialAgilityXp;

	private long startTime;
	private long timeout;

	private int crossCount;
	private int prevY;

	public AA_LedgeAgility(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		initialAgilityXp = getSkillExperience(Skill.AGILITY.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (getFatiguePercent() == 100) {
			timeout = 0L;
			return sleep();
		}

		final int playerY = getPlayerY();

		if (prevY != playerY) {
			timeout = 0L;
		} else if (System.currentTimeMillis() <= timeout) {
			return 0;
		}

		final Coordinate ledge = playerY < Obstacle.LEDGE_NORTH.getCoordinate().getY() ?
			Obstacle.LEDGE_NORTH.getCoordinate() :
			Obstacle.LEDGE_SOUTH.getCoordinate();

		bot.displayMessage("@cya@Crossing ...");
		prevY = playerY;
		atObject(ledge.getX(), ledge.getY());
		timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("across")) {
			timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		} else if (message.endsWith("hole")) {
			crossCount++;
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Ledge Agility", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getSkillExperience(Skill.AGILITY.getIndex()) - initialAgilityXp;

		bot.drawString(String.format("@yel@Count: @whi@%s @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(crossCount), toUnitsPerHour(crossCount, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum Obstacle implements RSObject {
		LEDGE_NORTH(614, new Coordinate(601, 3558)),
		LEDGE_SOUTH(615, new Coordinate(601, 3562));

		private final int id;
		private final Coordinate coordinate;

		Obstacle(final int id, final Coordinate coordinate) {
			this.id = id;
			this.coordinate = coordinate;
		}

		public int getId() {
			return id;
		}

		public Coordinate getCoordinate() {
			return coordinate;
		}
	}
}
