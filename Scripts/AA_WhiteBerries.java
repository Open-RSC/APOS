import com.aposbot.Constants;

import java.awt.Font;
import java.time.Duration;
import java.time.Instant;

/**
 * Collects White Berries from the Red Dragon Isle and banks at the Mage Arena Bank.
 * <p>
 * Required:
 * Start script at Red Dragon Isle with a weapon in inventory slot 1.
 * <p>
 * Optional Parameters:
 * -s,--spawn <south|north> (Default south spawn)
 * <p>
 * Author: Chomp
 */
public class AA_WhiteBerries extends AA_Script {
	private static final Coordinate COORDINATE_LUMBRIDGE_DEATH_WALK = new Coordinate(120, 648);
	private static final Coordinate COORDINATE_ISLE_DEATH_WALK = new Coordinate(142, 174);

	private static final Coordinate COORDINATE_LOAD_MAGE_BANK = new Coordinate(190, 140);
	private static final Coordinate COORDINATE_LOAD_LEVER_TO_BANK = new Coordinate(128, 140);
	private static final Coordinate COORDINATE_LOAD_LEVER_TO_ISLE = new Coordinate(192, 140);
	private static final Coordinate COORDINATE_LOAD_WILDERNESS_GATE_TO_BANK = new Coordinate(114, 159);
	private static final Coordinate COORDINATE_LOAD_WILDERNESS_GATE_TO_ISLE = new Coordinate(144, 140);

	private static final int NPC_ID_GUNDAI = 792;
	private static final int COORDINATE_Y_MAGE_BANK = 3000;
	private static final int ITEM_ID_WHITEBERRIES = 471;

	private WhiteBerries whiteBerries;
	private Instant startTime;
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
		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-s":
					case "--spawn":
						this.whiteBerries = WhiteBerries.valueOf(args[++i].toUpperCase());
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		if (this.whiteBerries == null) {
			this.whiteBerries = WhiteBerries.SOUTH;
		}

		this.initialWhiteBerryCount = this.getInventoryCount(ITEM_ID_WHITEBERRIES);
		this.banking = this.getInventoryCount() == MAX_INV_SIZE || this.isBanking();
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		if (this.died) {
			if (this.isDead()) {
				return 0;
			}

			if (this.pathWalker != null) {
				if (this.pathWalker.walkPath()) {
					return 0;
				}

				this.pathWalker = null;
			}

			this.died = false;
		}

		this.playerX = this.getX();
		this.playerY = this.getY();

		if (this.inCombat()) {
			this.walkTo(this.playerX, this.playerY);
			return SLEEP_ONE_TICK;
		}

		return this.banking ? this.bank() : this.collect();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("it") ||
			message.endsWith("web") ||
			message.endsWith("shut") ||
			message.endsWith("open")) {
			this.timeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void onDeath() {
		if (this.pathWalker == null) {
			this.pathWalker = new PathWalker(this.extension);
			this.pathWalker.init(null);
		}

		final PathWalker.Path path = this.pathWalker.calcPath(COORDINATE_LUMBRIDGE_DEATH_WALK.getX(), COORDINATE_LUMBRIDGE_DEATH_WALK.getY(),
			COORDINATE_ISLE_DEATH_WALK.getX(), COORDINATE_ISLE_DEATH_WALK.getY());

		if (path != null) {
			this.pathWalker.setPath(path);
			this.deathCount++;
			this.died = true;
			this.banking = false;
		} else {
			this.exit("Failed to calculate path from Lumbridge to Red Dragon Isle.");
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@White Berries", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final int count = Math.max(0, this.whiteBerriesCollected + this.getInventoryCount() - this.initialWhiteBerryCount - 1);

		this.drawString(String.format("@yel@Berries: @whi@%s @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(count), getUnitsPerHour(count, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.whiteBerriesBanked > 0) {
			this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			this.drawString(String.format("@gre@Total Banked: @whi@%s", DECIMAL_FORMAT.format(this.whiteBerriesBanked)),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}
	}

	private int bank() {
		if (this.playerY >= COORDINATE_Y_MAGE_BANK) {
			if (!this.isBanking()) {
				return this.openShop(NPC_ID_GUNDAI);
			}

			if (this.getInventoryCount() == 1) {
				this.whiteBerriesCollected += MAX_INV_SIZE - 1;
				this.whiteBerriesBanked = this.bankCount(ITEM_ID_WHITEBERRIES);

				this.closeBank();
				this.banking = false;
				return 0;
			}

			if (System.currentTimeMillis() <= this.depositTimeout) {
				return 0;
			}

			this.deposit(ITEM_ID_WHITEBERRIES, MAX_INV_SIZE);
			this.depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (this.playerY > Object.ISLE_GATE.coordinate.getY()) {
			if (this.distanceTo(Object.ISLE_GATE.coordinate.getX(), Object.ISLE_GATE.coordinate.getY()) > 1) {
				this.walkTo(Object.ISLE_GATE.coordinate.getX(), Object.ISLE_GATE.coordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			this.atObject(Object.ISLE_GATE.coordinate.getX(), Object.ISLE_GATE.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (this.playerY > COORDINATE_LOAD_WILDERNESS_GATE_TO_BANK.getY()) {
			this.walkTo(COORDINATE_LOAD_WILDERNESS_GATE_TO_BANK.getX(), COORDINATE_LOAD_WILDERNESS_GATE_TO_BANK.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.playerY >= Object.WILDERNESS_GATE.coordinate.getY()) {
			if (this.distanceTo(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY()) > 1) {
				this.walkTo(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			this.atObject(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (this.playerX < COORDINATE_LOAD_LEVER_TO_BANK.getX()) {
			this.walkTo(COORDINATE_LOAD_LEVER_TO_BANK.getX(), COORDINATE_LOAD_LEVER_TO_BANK.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.playerX < COORDINATE_LOAD_MAGE_BANK.getX()) {
			this.walkTo(COORDINATE_LOAD_MAGE_BANK.getX(), COORDINATE_LOAD_MAGE_BANK.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.LADDER_ROOM.contains(this.playerX, this.playerY)) {
			this.atObject(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BELOW_SOUTH_WEB.contains(this.playerX, this.playerY)) {
			if (this.wallExists(Object.DOOR_SOUTH)) {
				return this.openDoor(Object.DOOR_SOUTH);
			}

			this.walkTo(Object.LADDER_DOWN.coordinate.getX() + 1, Object.LADDER_DOWN.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BETWEEN_NORTH_AND_SOUTH_WEBS.contains(this.playerX, this.playerY)) {
			if (this.wallExists(Object.WEB_SOUTH)) {
				return this.cutWeb(Object.WEB_SOUTH);
			}

			this.walkTo(Object.DOOR_SOUTH.coordinate.getX(), Object.DOOR_SOUTH.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.ABOVE_NORTH_WEB.contains(this.playerX, this.playerY)) {
			if (this.wallExists(Object.WEB_NORTH)) {
				return this.cutWeb(Object.WEB_NORTH);
			}

			this.walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (this.distanceTo(Object.DOOR_NORTH.coordinate.getX(), Object.DOOR_NORTH.coordinate.getY()) > 1) {
			this.walkTo(Object.DOOR_NORTH.coordinate.getX(), Object.DOOR_NORTH.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (this.wallExists(Object.DOOR_NORTH)) {
			return this.openDoor(Object.DOOR_NORTH);
		}

		this.walkTo(Object.WEB_NORTH.coordinate.getX(), Object.WEB_NORTH.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int collect() {
		if (this.playerY > COORDINATE_Y_MAGE_BANK) {
			this.atObject(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.playerY > Object.ISLE_GATE.coordinate.getY()) {
			if (this.getInventoryCount() == MAX_INV_SIZE) {
				this.banking = true;
				return 0;
			}

			final int whiteBerriesX = this.whiteBerries.coordinate.getX();
			final int whiteBerriesY = this.whiteBerries.coordinate.getY();

			if (this.playerX != whiteBerriesX || this.playerY != whiteBerriesY) {
				this.walkTo(whiteBerriesX, whiteBerriesY);
				return SLEEP_ONE_TICK;
			}

			if (this.isItemAt(ITEM_ID_WHITEBERRIES, whiteBerriesX, whiteBerriesY)) {
				this.extension.createPacket(Constants.OP_GITEM_TAKE);
				this.extension.put2(whiteBerriesX);
				this.extension.put2(whiteBerriesY);
				this.extension.put2(ITEM_ID_WHITEBERRIES);
				this.extension.finishPacket();
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (this.playerY >= Object.WILDERNESS_GATE.coordinate.getY()) {
			if (this.distanceTo(Object.ISLE_GATE.coordinate.getX(), Object.ISLE_GATE.coordinate.getY()) > 1) {
				this.walkTo(Object.ISLE_GATE.coordinate.getX(), Object.ISLE_GATE.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			this.atObject(Object.ISLE_GATE.coordinate.getX(), Object.ISLE_GATE.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (this.playerX <= COORDINATE_LOAD_WILDERNESS_GATE_TO_ISLE.getX()) {
			if (this.distanceTo(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY()) > 1) {
				this.walkTo(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			this.atObject(Object.WILDERNESS_GATE.coordinate.getX(), Object.WILDERNESS_GATE.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (this.playerX <= COORDINATE_LOAD_LEVER_TO_ISLE.getX()) {
			this.walkTo(COORDINATE_LOAD_WILDERNESS_GATE_TO_ISLE.getX(), COORDINATE_LOAD_WILDERNESS_GATE_TO_ISLE.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.ABOVE_NORTH_WEB.contains(this.playerX, this.playerY)) {
			if (this.wallExists(Object.DOOR_NORTH)) {
				return this.openDoor(Object.DOOR_NORTH);
			}

			this.walkTo(Object.DOOR_NORTH.coordinate.getX(), Object.DOOR_NORTH.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (Area.BETWEEN_NORTH_AND_SOUTH_WEBS.contains(this.playerX, this.playerY)) {
			if (this.wallExists(Object.WEB_NORTH)) {
				return this.cutWeb(Object.WEB_NORTH);
			}

			this.walkTo(Object.DOOR_NORTH.coordinate.getX(), Object.DOOR_NORTH.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BELOW_SOUTH_WEB.contains(this.playerX, this.playerY)) {
			if (this.wallExists(Object.WEB_SOUTH)) {
				return this.cutWeb(Object.WEB_SOUTH);
			}

			this.walkTo(Object.WEB_NORTH.coordinate.getX(), Object.WEB_NORTH.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.LADDER_ROOM.contains(this.playerX, this.playerY)) {
			if (this.wallExists(Object.DOOR_SOUTH)) {
				return this.openDoor(Object.DOOR_SOUTH);
			}

			this.walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(COORDINATE_LOAD_LEVER_TO_ISLE.getX(), COORDINATE_LOAD_LEVER_TO_ISLE.getY());
		return SLEEP_ONE_TICK;
	}

	private boolean wallExists(final Object object) {
		return this.getWallObjectIdFromCoords(object.coordinate.getX(), object.coordinate.getY()) == object.id;
	}

	private int cutWeb(final Object web) {
		if (System.currentTimeMillis() <= this.timeout) {
			return 0;
		}

		this.useItemOnWallObject(0, web.coordinate.getX(), web.coordinate.getY());
		this.timeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private int openDoor(final Object door) {
		if (System.currentTimeMillis() <= this.timeout) {
			return 0;
		}

		this.atWallObject(door.coordinate.getX(), door.coordinate.getY());
		this.timeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		return 0;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
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
			return this.lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return this.upperBoundingCoordinate;
		}
	}
}
