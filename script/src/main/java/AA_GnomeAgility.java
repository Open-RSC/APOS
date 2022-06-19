/**
 * A script for the Gnome Agility Course.
 * Start script at the Gnome Agility Course with sleeping bag in inventory.
 * <p>
 *
 * @Author Chomp
 */
public class AA_GnomeAgility extends AA_Script {
	private static final int MAXIMUM_FATIGUE = 99;

	private long startTime;

	private double initialAgilityXp;

	private int lapCount;

	public AA_GnomeAgility(final Extension ex) {
		super(ex);
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
		if (getFatiguePercent() >= MAXIMUM_FATIGUE) {
			return sleep();
		}

		final int playerY = getPlayerY();
		final int playerX = getPlayerX();

		final Obstacle obstacle;

		if (playerY > 2393) {
			if (playerX > 688) {
				obstacle = Obstacle.ROPESWING;
			} else {
				obstacle = Obstacle.WATCHTOWER_DOWN;
			}
		} else if (playerY > 1447) {
			obstacle = Obstacle.WATCHTOWER_UP;
		} else if (playerY < 496 || (playerX == 692 && playerY == 498)) {
			obstacle = Obstacle.LOG;
		} else if (playerX > 688) {
			obstacle = Obstacle.NET_ENTER;
		} else if (playerY < 502) {
			obstacle = Obstacle.PIPE;
		} else {
			obstacle = Obstacle.NET_EXIT;
		}

		final int obstacleX = obstacle.coordinate.getX();
		final int obstacleY = obstacle.coordinate.getY();

		if (distanceTo(playerX, playerY, obstacleX, obstacleY) > 1) {
			walkTo(obstacle.adjacentCoordinate.getX(), obstacle.adjacentCoordinate.getY());
		}

		useObject1(obstacleX, obstacleY);
		return SLEEP_ONE_TICK;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("into it")) {
			lapCount++;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Gnome Agility", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getSkillExperience(Skill.AGILITY.getIndex()) - initialAgilityXp;

		bot.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Lap: @whi@%d @cya@(@whi@%s laps@cya@/@whi@hr@cya@)",
				lapCount, toUnitsPerHour(lapCount, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum Obstacle implements RSObject {
		LOG(655, new Coordinate(692, 495), new Coordinate(691, 494)),
		NET_ENTER(647, new Coordinate(692, 503), new Coordinate(692, 502)),
		WATCHTOWER_UP(648, new Coordinate(693, 1452), new Coordinate(693, 1451)),
		ROPESWING(650, new Coordinate(689, 2395), new Coordinate(690, 2395)),
		WATCHTOWER_DOWN(649, new Coordinate(683, 2396), new Coordinate(684, 2396)),
		NET_EXIT(653, new Coordinate(683, 502), new Coordinate(683, 503)),
		PIPE(654, new Coordinate(683, 497), new Coordinate(683, 498));

		private final int id;
		private final Coordinate coordinate;
		private final Coordinate adjacentCoordinate;

		Obstacle(final int id, final Coordinate coordinate, final Coordinate adjacentCoordinate) {
			this.id = id;
			this.coordinate = coordinate;
			this.adjacentCoordinate = adjacentCoordinate;
		}

		public int getId() {
			return id;
		}

		public Coordinate getCoordinate() {
			return coordinate;
		}

		public Coordinate getAdjacentCoordinate() {
			return adjacentCoordinate;
		}
	}
}
