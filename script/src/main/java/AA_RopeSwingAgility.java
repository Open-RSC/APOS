/**
 * A script for rope swinging in the Yanille Agility Dungeon.
 * Start script at the ropeswing with sleeping bag in inventory.
 * <p>
 *
 * @Author Chomp
 */
public class AA_RopeSwingAgility extends AA_Script {
	private double initialAgilityXp;

	private long startTime;
	private long timeout;

	private int swingCount;
	private int prevY;

	public AA_RopeSwingAgility(final Extension bot) {
		super(bot);
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

		final int playerX = getPlayerX();
		final int playerY = getPlayerY();

		if (prevY != playerY) {
			timeout = 0L;
		} else if (System.currentTimeMillis() <= timeout) {
			return 0;
		}

		Coordinate ledge = Obstacle.ROPE_NORTH.getCoordinate();

		if (playerY < ledge.getY()) {
			if (playerX != ledge.getX() - 1 || playerY != ledge.getY() - 1) {
				walkTo(ledge.getX() - 1, ledge.getY() - 1);
			}
		} else {
			ledge = Obstacle.ROPE_SOUTH.getCoordinate();

			if (playerX != ledge.getX() + 1 || playerY != ledge.getY() + 1) {
				walkTo(ledge.getX() + 1, ledge.getY() + 1);
			}
		}

		bot.displayMessage("@cya@Swinging ...");
		prevY = playerY;
		useObject1(ledge.getX(), ledge.getY());
		timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("across")) {
			timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		} else if (message.endsWith("hole")) {
			swingCount++;
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Rope Swing Agility", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getSkillExperience(Skill.AGILITY.getIndex()) - initialAgilityXp;

		bot.drawString(String.format("@yel@Count: @whi@%s @cya@(@whi@%s swings@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(swingCount), toUnitsPerHour(swingCount, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum Obstacle implements RSObject {
		ROPE_NORTH(627, new Coordinate(598, 3582)),
		ROPE_SOUTH(628, new Coordinate(596, 3584));

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
