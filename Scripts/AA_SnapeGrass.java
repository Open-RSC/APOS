import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Collects snape grass at the Hobgoblin Peninsula and banks at Falador East Bank.
 * <p>
 * Start with an empty inventory at Falador East Bank or at the snape grass spawns.
 * <p>
 * Author: Chomp
 */
public class AA_SnapeGrass extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_PATH = new Coordinate(352, 619);
	private static final Coordinate COORDINATE_LOAD_BANK = new Coordinate(294, 581);
	private static final Coordinate COORDINATE_LOAD_HOBGOBLINS = new Coordinate(334, 616);

	private final Map<SnapeGrass, Long> spawnMap = new HashMap<>();

	private final Coordinate snapeGrass = new Coordinate(-1, -1);

	private Coordinate nextRespawn;
	private Instant startTime;

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
		this.initialSnapeGrassCount = this.getInventoryCount(SnapeGrass.ITEM_ID);
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		this.playerX = this.getX();
		this.playerY = this.getY();

		return this.getInventoryCount() == MAX_INV_SIZE || this.isBanking() ? this.bank() : this.collect();
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Snape Grass", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final int count = Math.max(0, this.snapeGrassCollected + this.getInventoryCount() - this.initialSnapeGrassCount - 1);

		this.drawString(String.format("@yel@Snapes: @whi@%s @cya@(@whi@%s grass@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(count), getUnitsPerHour(count, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.snapeGrassBanked > 0) {
			this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			this.drawString(String.format("@gre@Total Banked: @whi@%s", DECIMAL_FORMAT.format(this.snapeGrassBanked)),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}
	}

	@Override
	public void onGroundItemDespawned(final int id, final int x, final int y) {
		if (id != SnapeGrass.ITEM_ID) {
			return;
		}

		final SnapeGrass snapeGrass = SnapeGrass.fromCoord(x, y);

		if (snapeGrass == null) {
			return;
		}

		this.spawnMap.put(snapeGrass, System.currentTimeMillis());
		this.nextRespawn = this.spawnMap.entrySet().stream().min(Map.Entry.comparingByValue()).get().getKey().coordinate;
	}

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (!this.isBanking()) {
				return this.openBank();
			}

			if (this.getInventoryCount() == 0) {
				if (System.currentTimeMillis() <= this.closeTimeout) {
					return 0;
				}

				this.snapeGrassCollected += MAX_INV_SIZE;
				this.snapeGrassBanked = this.bankCount(SnapeGrass.ITEM_ID);
				this.closeBank();
				this.closeTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
				return 0;
			}

			this.deposit(this.getInventoryId(0), MAX_INV_SIZE);
			return SLEEP_ONE_TICK;
		}

		if (this.playerX > COORDINATE_LOAD_PATH.getX()) {
			this.walkTo(COORDINATE_LOAD_PATH.getX(), COORDINATE_LOAD_PATH.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.playerX > COORDINATE_LOAD_BANK.getX()) {
			this.walkTo(COORDINATE_LOAD_BANK.getX(), COORDINATE_LOAD_BANK.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) > 1) {
			this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(Object.BANK_DOORS.coordinate.getX() - 1, Object.BANK_DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int collect() {
		if (Area.HOBGOBLINS.contains(this.playerX, this.playerY)) {
			this.updateSnapeGrass();

			if (this.snapeGrass.getX() != -1) {
				this.pickupItem(SnapeGrass.ITEM_ID, this.snapeGrass.getX(), this.snapeGrass.getY());
				return SLEEP_ONE_TICK;
			}

			if (this.nextRespawn != null &&
				(this.playerX != this.nextRespawn.getX() || this.playerY != this.nextRespawn.getY())) {
				this.walkTo(this.nextRespawn.getX(), this.nextRespawn.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (this.playerX < COORDINATE_LOAD_HOBGOBLINS.getX()) {
			this.walkTo(COORDINATE_LOAD_HOBGOBLINS.getX(), COORDINATE_LOAD_HOBGOBLINS.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Area.HOBGOBLINS.lowerBoundingCoordinate.getX(), Area.HOBGOBLINS.upperBoundingCoordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private void updateSnapeGrass() {
		this.snapeGrass.setX(-1);

		int minimumDistance = Integer.MAX_VALUE;

		for (int index = 0; index < this.getGroundItemCount(); index++) {
			if (this.getGroundItemId(index) != SnapeGrass.ITEM_ID) {
				continue;
			}

			final int groundItemX = this.getItemX(index);
			final int groundItemY = this.getItemY(index);

			if (!Area.HOBGOBLINS.contains(groundItemX, groundItemY)) {
				continue;
			}

			final int distance = this.distanceTo(groundItemX, groundItemY);

			if (distance < minimumDistance) {
				this.snapeGrass.set(groundItemX, groundItemY);
				minimumDistance = distance;
			}
		}
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
			return this.lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return this.upperBoundingCoordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
		}
	}
}
