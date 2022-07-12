import java.util.HashMap;
import java.util.Map;

/**
 * Collects snape grass at the Hobgoblin Peninsula and banks at Falador East Bank.
 * <p>
 * Start with an empty inventory at Falador East Bank or at the snape grass spawns.
 * <p>
 *
 * @Author Chomp
 */
public class AA_SnapeGrass extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_PATH = new Coordinate(352, 619);
	private static final Coordinate COORDINATE_LOAD_BANK = new Coordinate(294, 581);
	private static final Coordinate COORDINATE_LOAD_HOBGOBLINS = new Coordinate(334, 616);

	private final Map<SnapeGrass, Long> spawnMap = new HashMap<>();

	private final Coordinate snapeGrass = new Coordinate(-1, -1);

	private Coordinate nextRespawn;
	private long startTime;

	private long closeTimeout;

	private int playerX;
	private int playerY;

	private int initialSnapeGrassCount;
	private int snapeGrassCollected;
	private int snapeGrassBanked;

	public AA_SnapeGrass(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		initialSnapeGrassCount = getInventoryCount(SnapeGrass.ITEM_ID);
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		return getInventoryCount() == MAX_INV_SIZE || isBanking() ? bank() : collect();
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (!isBanking()) {
				return openBank();
			}

			if (getInventoryCount() == 0) {
				if (System.currentTimeMillis() <= closeTimeout) {
					return 0;
				}

				snapeGrassCollected += MAX_INV_SIZE;
				snapeGrassBanked = bankCount(SnapeGrass.ITEM_ID);
				closeBank();
				closeTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
				return 0;
			}

			deposit(getInventoryId(0), MAX_INV_SIZE);
			return SLEEP_ONE_TICK;
		}

		if (playerX > COORDINATE_LOAD_PATH.getX()) {
			walkTo(COORDINATE_LOAD_PATH.getX(), COORDINATE_LOAD_PATH.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerX > COORDINATE_LOAD_BANK.getX()) {
			walkTo(COORDINATE_LOAD_BANK.getX(), COORDINATE_LOAD_BANK.getY());
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) > 1) {
			walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(Object.BANK_DOORS.coordinate.getX() - 1, Object.BANK_DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int collect() {
		if (Area.HOBGOBLINS.contains(playerX, playerY)) {
			updateSnapeGrass();

			if (snapeGrass.getX() != -1) {
				pickupItem(SnapeGrass.ITEM_ID, snapeGrass.getX(), snapeGrass.getY());
				return SLEEP_ONE_TICK;
			}

			if (nextRespawn != null &&
				(playerX != nextRespawn.getX() || playerY != nextRespawn.getY())) {
				walkTo(nextRespawn.getX(), nextRespawn.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (playerX < COORDINATE_LOAD_HOBGOBLINS.getX()) {
			walkTo(COORDINATE_LOAD_HOBGOBLINS.getX(), COORDINATE_LOAD_HOBGOBLINS.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(Area.HOBGOBLINS.lowerBoundingCoordinate.getX(), Area.HOBGOBLINS.upperBoundingCoordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private void updateSnapeGrass() {
		snapeGrass.setX(-1);

		int minimumDistance = Integer.MAX_VALUE;

		for (int index = 0; index < getGroundItemCount(); index++) {
			if (getGroundItemId(index) != SnapeGrass.ITEM_ID) {
				continue;
			}

			final int groundItemX = getItemX(index);
			final int groundItemY = getItemY(index);

			if (!Area.HOBGOBLINS.contains(groundItemX, groundItemY)) {
				continue;
			}

			final int distance = distanceTo(groundItemX, groundItemY);

			if (distance < minimumDistance) {
				snapeGrass.set(groundItemX, groundItemY);
				minimumDistance = distance;
			}
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Snape Grass", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final int count = Math.max(0, snapeGrassCollected + getInventoryCount() - initialSnapeGrassCount - 1);

		drawString(String.format("@yel@Snapes: @whi@%s @cya@(@whi@%s grass@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(count), toUnitsPerHour(count, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		if (snapeGrassBanked <= 0) return;

		drawString(String.format("@gre@Total Banked: @whi@%s", DECIMAL_FORMAT.format(snapeGrassBanked)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);
	}

	@Override
	public void onGroundItemDespawned(final int groundItemIndex) {
		if (bot.getGroundItemId(groundItemIndex) != SnapeGrass.ITEM_ID) {
			return;
		}

		final int x = bot.getGroundItemLocalX(groundItemIndex) + bot.getAreaX();
		final int y = bot.getGroundItemLocalY(groundItemIndex) + bot.getAreaY();

		final SnapeGrass snapeGrass = SnapeGrass.fromCoord(x, y);

		if (snapeGrass == null) {
			return;
		}

		spawnMap.put(snapeGrass, System.currentTimeMillis());
		nextRespawn = spawnMap.entrySet().stream().min(Map.Entry.comparingByValue()).get().getKey().coordinate;
	}

	private enum SnapeGrass implements Comparable<SnapeGrass> {
		NORTH(new Coordinate(362, 602)),
		EAST(new Coordinate(361, 603)),
		SOUTH(new Coordinate(358, 615));

		private static final SnapeGrass[] SPAWNS = SnapeGrass.values();
		private static final int ITEM_ID = 469;

		private final Coordinate coordinate;

		SnapeGrass(final Coordinate coordinate) {
			this.coordinate = coordinate;
		}

		private static SnapeGrass fromCoord(final int x, final int y) {
			for (final SnapeGrass snapeGrass : SPAWNS) {
				if (x == snapeGrass.coordinate.getX() && y == snapeGrass.coordinate.getY()) {
					return snapeGrass;
				}
			}

			return null;
		}
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(280, 564), new Coordinate(286, 573)),
		HOBGOBLINS(new Coordinate(356, 601), new Coordinate(367, 615));

		private final Coordinate lowerBoundingCoordinate;
		private final Coordinate upperBoundingCoordinate;

		Area(final Coordinate lowerBoundingCoordinate, final Coordinate upperBoundingCoordinate) {
			this.lowerBoundingCoordinate = lowerBoundingCoordinate;
			this.upperBoundingCoordinate = upperBoundingCoordinate;
		}

		public Coordinate getLowerBoundingCoordinate() {
			return lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return upperBoundingCoordinate;
		}
	}

	private enum Object implements RSObject {
		BANK_DOORS(64, new Coordinate(287, 571));

		private final int id;
		private final Coordinate coordinate;

		Object(final int id, final Coordinate coordinate) {
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
