/**
 * Collects White Berries from the Red Dragon Isle and banks at the Mage Arena Bank.
 * <p>
 * Required:
 * Start script at Red Dragon Isle with a weapon in inventory slot 1.
 * <p>
 * Optional Parameters:
 * <south|north> (Default south spawn)
 * <p>
 *
 * @Author Chomp
 */
public class AA_WhiteBerries extends AA_Script {
	private static final Coordinate COORD_LUMBRIDGE = new Coordinate(120, 648);
	private static final Coordinate COORD_ISLE = new Coordinate(142, 174);

	private static final Coordinate COORD_MAGE_BANK = new Coordinate(190, 140);
	private static final Coordinate CORD_LEVER_TO_BANK = new Coordinate(128, 140);
	private static final Coordinate COORD_LEVER_TO_ISLE = new Coordinate(192, 140);
	private static final Coordinate COORD_WILD_GATE_TO_BANK = new Coordinate(114, 159);
	private static final Coordinate COORD_WILD_GATE_TO_ISLE = new Coordinate(144, 140);

	private static final int NPC_ID_GUNDAI = 792;
	private static final int COORD_Y_MAGE_BANK = 3000;
	private static final int ITEM_ID_WHITEBERRIES = 471;

	private WhiteBerries whiteBerries;
	private long startTime;
	private PathWalker pathWalker;

	private long timeout;
	private long depositTimeout;

	private int playerX;
	private int playerY;

	private int initialWhiteBerryCount;
	private int whiteBerriesCollected;
	private int whiteBerriesBanked;

	private int deathCount;

	private boolean banking;
	private boolean died;

	public AA_WhiteBerries(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) {
			whiteBerries = WhiteBerries.SOUTH;
		} else {
			whiteBerries = WhiteBerries.valueOf(parameters.toUpperCase());
		}

		initialWhiteBerryCount = getInventoryCount(ITEM_ID_WHITEBERRIES);
		banking = getInventoryCount() == MAX_INV_SIZE || isBanking();
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (died) {
			if (isDead()) {
				return 0;
			}

			if (pathWalker != null) {
				if (pathWalker.walkPath()) {
					return 0;
				}

				pathWalker = null;
			}

			died = false;
		}

		playerX = getX();
		playerY = getY();

		if (inCombat()) {
			walkTo(playerX, playerY);
			return SLEEP_ONE_TICK;
		}

		return banking ? bank() : collect();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("it") ||
			message.endsWith("web") ||
			message.endsWith("shut") ||
			message.endsWith("open")) {
			timeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void onDeath() {
		if (pathWalker == null) {
			pathWalker = new PathWalker(bot);
			pathWalker.init(null);
		}

		final PathWalker.Path path = pathWalker.calcPath(COORD_LUMBRIDGE.getX(), COORD_LUMBRIDGE.getY(),
			COORD_ISLE.getX(), COORD_ISLE.getY());

		if (path != null) {
			pathWalker.setPath(path);
			deathCount++;
			died = true;
			banking = false;
		} else {
			exit("Failed to calculate path from Lumbridge to Red Dragon Isle.");
		}
	}

	private int bank() {
		if (playerY >= COORD_Y_MAGE_BANK) {
			if (!isBanking()) {
				return openShop(NPC_ID_GUNDAI);
			}

			if (getInventoryCount() == 1) {
				whiteBerriesCollected += MAX_INV_SIZE - 1;
				whiteBerriesBanked = bankCount(ITEM_ID_WHITEBERRIES);

				closeBank();
				banking = false;
				return 0;
			}

			if (System.currentTimeMillis() <= depositTimeout) {
				return 0;
			}

			deposit(ITEM_ID_WHITEBERRIES, MAX_INV_SIZE);
			depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (playerY > Object.ISLE_GATE.coordinate.getY()) {
			if (distanceTo(Object.ISLE_GATE.coordinate.getX(), Object.ISLE_GATE.coordinate.getY()) > 1) {
				walkTo(Object.ISLE_GATE.coordinate.getX(), Object.ISLE_GATE.coordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			atObject(Object.ISLE_GATE.coordinate.getX(), Object.ISLE_GATE.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (playerY > COORD_WILD_GATE_TO_BANK.getY()) {
			walkTo(COORD_WILD_GATE_TO_BANK.getX(), COORD_WILD_GATE_TO_BANK.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerY >= Object.WILDERNESS_GATE.coordinate.getY()) {
			if (distanceTo(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY()) > 1) {
				walkTo(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			atObject(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (playerX < CORD_LEVER_TO_BANK.getX()) {
			walkTo(CORD_LEVER_TO_BANK.getX(), CORD_LEVER_TO_BANK.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerX < COORD_MAGE_BANK.getX()) {
			walkTo(COORD_MAGE_BANK.getX(), COORD_MAGE_BANK.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.LADDER_ROOM.contains(playerX, playerY)) {
			atObject(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BELOW_SOUTH_WEB.contains(playerX, playerY)) {
			if (wallExists(Object.DOOR_SOUTH)) {
				return openDoor(Object.DOOR_SOUTH);
			}

			walkTo(Object.LADDER_DOWN.coordinate.getX() + 1, Object.LADDER_DOWN.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BETWEEN_NORTH_AND_SOUTH_WEBS.contains(playerX, playerY)) {
			if (wallExists(Object.WEB_SOUTH)) {
				return cutWeb(Object.WEB_SOUTH);
			}

			walkTo(Object.DOOR_SOUTH.coordinate.getX(), Object.DOOR_SOUTH.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.ABOVE_NORTH_WEB.contains(playerX, playerY)) {
			if (wallExists(Object.WEB_NORTH)) {
				return cutWeb(Object.WEB_NORTH);
			}

			walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(Object.DOOR_NORTH.coordinate.getX(), Object.DOOR_NORTH.coordinate.getY()) > 1) {
			walkTo(Object.DOOR_NORTH.coordinate.getX(), Object.DOOR_NORTH.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (wallExists(Object.DOOR_NORTH)) {
			return openDoor(Object.DOOR_NORTH);
		}

		walkTo(Object.WEB_NORTH.coordinate.getX(), Object.WEB_NORTH.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int collect() {
		if (playerY > COORD_Y_MAGE_BANK) {
			atObject(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerY > Object.ISLE_GATE.coordinate.getY()) {
			if (getInventoryCount() == MAX_INV_SIZE) {
				banking = true;
				return 0;
			}

			final int whiteBerriesX = whiteBerries.coordinate.getX();
			final int whiteBerriesY = whiteBerries.coordinate.getY();

			if (playerX != whiteBerriesX || playerY != whiteBerriesY) {
				walkTo(whiteBerriesX, whiteBerriesY);
				return SLEEP_ONE_TICK;
			}

			if (isItemAt(ITEM_ID_WHITEBERRIES, whiteBerriesX, whiteBerriesY)) {
				takeGroundItem(ITEM_ID_WHITEBERRIES, whiteBerriesX, whiteBerriesY);
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (playerY >= Object.WILDERNESS_GATE.coordinate.getY()) {
			if (distanceTo(Object.ISLE_GATE.coordinate.getX(), Object.ISLE_GATE.coordinate.getY()) > 1) {
				walkTo(Object.ISLE_GATE.coordinate.getX(), Object.ISLE_GATE.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			atObject(Object.ISLE_GATE.coordinate.getX(), Object.ISLE_GATE.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (playerX <= COORD_WILD_GATE_TO_ISLE.getX()) {
			if (distanceTo(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY()) > 1) {
				walkTo(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			atObject(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (playerX <= COORD_LEVER_TO_ISLE.getX()) {
			walkTo(COORD_WILD_GATE_TO_ISLE.getX(), COORD_WILD_GATE_TO_ISLE.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.ABOVE_NORTH_WEB.contains(playerX, playerY)) {
			if (wallExists(Object.DOOR_NORTH)) {
				return openDoor(Object.DOOR_NORTH);
			}

			walkTo(Object.DOOR_NORTH.coordinate.getX(), Object.DOOR_NORTH.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (Area.BETWEEN_NORTH_AND_SOUTH_WEBS.contains(playerX, playerY)) {
			if (wallExists(Object.WEB_NORTH)) {
				return cutWeb(Object.WEB_NORTH);
			}

			walkTo(Object.DOOR_NORTH.coordinate.getX(), Object.DOOR_NORTH.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BELOW_SOUTH_WEB.contains(playerX, playerY)) {
			if (wallExists(Object.WEB_SOUTH)) {
				return cutWeb(Object.WEB_SOUTH);
			}

			walkTo(Object.WEB_NORTH.coordinate.getX(), Object.WEB_NORTH.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.LADDER_ROOM.contains(playerX, playerY)) {
			if (wallExists(Object.DOOR_SOUTH)) {
				return openDoor(Object.DOOR_SOUTH);
			}

			walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(COORD_LEVER_TO_ISLE.getX(), COORD_LEVER_TO_ISLE.getY());
		return SLEEP_ONE_TICK;
	}

	private boolean wallExists(final Object object) {
		return getWallObjectIdFromCoords(object.coordinate.getX(), object.coordinate.getY()) == object.id;
	}

	private int openDoor(final Object door) {
		if (System.currentTimeMillis() <= timeout) {
			return 0;
		}

		atWallObject(door.coordinate.getX(), door.coordinate.getY());
		timeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		return 0;
	}

	private int cutWeb(final Object web) {
		if (System.currentTimeMillis() <= timeout) {
			return 0;
		}

		useItemOnWallObject(0, web.coordinate.getX(), web.coordinate.getY());
		timeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@White Berries", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final int count = Math.max(0, whiteBerriesCollected + getInventoryCount() - initialWhiteBerryCount - 1);

		drawString(String.format("@yel@Berries: @whi@%s @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(count), toUnitsPerHour(count, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		if (whiteBerriesBanked <= 0) return;

		drawString(String.format("@gre@Total Banked: @whi@%s", DECIMAL_FORMAT.format(whiteBerriesBanked)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);
	}

	private enum WhiteBerries implements RSObject {
		NORTH(ITEM_ID_WHITEBERRIES, new Coordinate(131, 205)),
		SOUTH(ITEM_ID_WHITEBERRIES, new Coordinate(137, 213));

		private final int id;
		private final Coordinate coordinate;

		WhiteBerries(final int id, final Coordinate coordinate) {
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

	private enum Object implements RSObject {
		WILDERNESS_GATE(347, new Coordinate(111, 142)),
		ISLE_GATE(93, new Coordinate(140, 180)),
		LADDER_DOWN(1188, new Coordinate(223, 110)),
		LADDER_UP(1187, new Coordinate(446, 3367)),
		DOOR_NORTH(2, new Coordinate(227, 106)),
		DOOR_SOUTH(2, new Coordinate(226, 110)),
		WEB_NORTH(24, new Coordinate(227, 107)),
		WEB_SOUTH(24, new Coordinate(227, 109));

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

	private enum Area implements RSArea {
		ABOVE_NORTH_WEB(new Coordinate(227, 106), new Coordinate(227, 106)),
		BETWEEN_NORTH_AND_SOUTH_WEBS(new Coordinate(227, 107), new Coordinate(227, 108)),
		BELOW_SOUTH_WEB(new Coordinate(226, 109), new Coordinate(227, 110)),
		LADDER_ROOM(new Coordinate(220, 108), new Coordinate(225, 111));

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
}
