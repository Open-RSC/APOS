import java.awt.*;
import java.time.Duration;
import java.time.Instant;

/**
 * Kills Darkwizards and collects runes/coins at the Dark Wizard Tower.
 * Start at the tower with sleeping bag in inventory.
 * <p>
 * Optional Parameter:
 * --no-doors (Disables closing doors)
 * -f,--fightmode <controlled|strength|attack|defense>
 * <p>
 * Author: Chomp
 */
public class AA_DarkWizardTower extends AA_Script {
	private static final int[] NPC_IDS_DARK_WIZARD = new int[]{57, 60};

	private static final int[] ITEM_IDS_LOOT = new int[]{10, 31, 32, 33, 34, 35, 38, 40, 41, 42, 46, 619};

	private static final int SKILL_INDEX_ATTACK = 0;
	private static final int SKILL_INDEX_DEFENCE = 1;
	private static final int SKILL_INDEX_STRENGTH = 2;
	private static final int SKILL_INDEX_HITS = 3;

	private static final int MAXIMUM_FATIGUE = 99;

	private final int[] groundItem = new int[4];

	private Instant startTime;

	private double initialCombatXp;

	private long doorTimeout;
	private long ladderTimeout;

	private int playerX;
	private int playerY;

	private boolean checkTopFloor;
	private boolean doors = true;

	public AA_DarkWizardTower(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-f":
					case "--fightmode":
						this.combatStyle = CombatStyle.valueOf(args[++i].toUpperCase());
						break;
					case "--no-doors":
						this.doors = false;
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		this.setFightMode(this.combatStyle.getIndex());
		this.initialCombatXp = this.getTotalCombatXp();
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		if (this.inCombat()) {
			return 0;
		}

		if (this.getFatigue() >= MAXIMUM_FATIGUE) {
			return this.sleep();
		}

		this.playerX = this.getX();
		this.playerY = this.getY();

		return Floor.F2.contains(this.playerX, this.playerY) || Floor.F3.contains(this.playerX, this.playerY) ?
			this.handleF2F3() :
			this.handleF1();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("ladder")) {
			this.ladderTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("open") || message.endsWith("shut")) {
			this.doorTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Dark Wizard Tower", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final double gainedXp = this.getTotalCombatXp() - this.initialCombatXp;

		this.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(gainedXp), getUnitsPerHour(gainedXp, secondsElapsed)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
	}

	private int handleF1() {
		final int[] wizard = this.getNpcById(NPC_IDS_DARK_WIZARD);

		if (wizard[0] != -1) {
			if (Floor.F1.contains(this.playerX, this.playerY) != Floor.F1.contains(wizard[1], wizard[2])) {
				final Door door = this.getNearestDoorTo(wizard[1], wizard[2]);

				final int doorX = door.coordinate.getX();
				final int doorY = door.coordinate.getY();

				if (this.getWallObjectIdFromCoords(doorX, doorY) == door.id) {
					if (System.currentTimeMillis() <= this.doorTimeout) {
						return 0;
					}

					this.atWallObject(doorX, doorY);
					this.doorTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
					return 0;
				}
			}

			this.attackNpc(wizard[0]);
			return SLEEP_ONE_TICK;
		}

		this.updateGroundItem(true);

		if (this.groundItem[0] != -1) {
			this.pickupItem(this.groundItem[3], this.groundItem[1], this.groundItem[2]);
			return SLEEP_ONE_TICK;
		}

		if (!Floor.F1.contains(this.playerX, this.playerY)) {
			return this.enterF1();
		}

		if (this.doors) {
			for (final Door door : Door.DOORS) {
				final int doorX = door.coordinate.getX();
				final int doorY = door.coordinate.getY();

				if (this.getWallObjectIdFromCoords(doorX, doorY) != door.id) {
					if (System.currentTimeMillis() <= this.doorTimeout) {
						return 0;
					}

					this.atWallObject2(doorX, doorY);
					this.doorTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
					return 0;
				}
			}
		}

		return this.nextFloor();
	}

	private int enterF1() {
		final Door door = this.getNearestDoorTo(this.playerX, this.playerY);

		final int doorX = door.coordinate.getX();
		final int doorY = door.coordinate.getY();

		if (this.getWallObjectIdFromCoords(doorX, doorY) != door.id) {
			this.walkTo(doorX, doorY);
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= this.doorTimeout) {
			return 0;
		}

		this.atWallObject(doorX, doorY);
		this.doorTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	private int handleF2F3() {
		final int[] wizard = this.getNpcById(NPC_IDS_DARK_WIZARD);

		if (wizard[0] != -1) {
			this.attackNpc(wizard[0]);
			return SLEEP_ONE_TICK;
		}

		this.updateGroundItem(false);

		if (this.groundItem[0] != -1) {
			this.pickupItem(this.groundItem[3], this.groundItem[1], this.groundItem[2]);
			return SLEEP_ONE_TICK;
		}

		return this.nextFloor();
	}

	private int nextFloor() {
		if (System.currentTimeMillis() <= this.ladderTimeout) {
			return 0;
		}

		if (Floor.F1.contains(this.playerX, this.playerY)) {
			this.checkTopFloor = true;

			this.atObject(Ladder.F1_UP.coordinate.getX(),
				Ladder.F1_UP.coordinate.getY());
		} else if (Floor.F2.contains(this.playerX, this.playerY)) {
			if (this.checkTopFloor) {
				this.atObject(Ladder.F2_UP.coordinate.getX(),
					Ladder.F2_UP.coordinate.getY());
			} else {
				this.atObject(Ladder.F2_DOWN.coordinate.getX(),
					Ladder.F2_DOWN.coordinate.getY());
			}
		} else if (Floor.F3.contains(this.playerX, this.playerY)) {
			this.checkTopFloor = false;

			this.atObject(Ladder.F3_DOWN.coordinate.getX(),
				Ladder.F3_DOWN.coordinate.getY());
		} else {
			return SLEEP_ONE_TICK;
		}

		this.ladderTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private void updateGroundItem(final boolean bottomFloor) {
		this.groundItem[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < this.getGroundItemCount(); index++) {
			final int groundItemId = this.getGroundItemId(index);

			if (!inArray(ITEM_IDS_LOOT, groundItemId)) {
				continue;
			}

			final int groundItemX = this.getItemX(index);
			final int groundItemY = this.getItemY(index);

			if (bottomFloor && !Floor.F1.contains(groundItemX, groundItemY)) {
				continue;
			}

			final int distance = this.distanceTo(groundItemX, groundItemY);

			if (distance < currentDistance) {
				this.groundItem[0] = index;
				this.groundItem[1] = groundItemX;
				this.groundItem[2] = groundItemY;
				this.groundItem[3] = groundItemId;

				currentDistance = distance;
			}
		}
	}

	private Door getNearestDoorTo(final int x, final int y) {
		Door closestDoor = null;

		int currentDistance = Integer.MAX_VALUE;

		for (final Door door : Door.DOORS) {
			final int distance = distanceTo(x, y, door.coordinate.getX(), door.coordinate.getY());

			if (distance < currentDistance) {
				currentDistance = distance;
				closestDoor = door;
			}
		}

		return closestDoor;
	}

	private enum Floor implements RSArea {
		F1(new Coordinate(360, 568), new Coordinate(363, 573)) {
			public boolean contains(final int x, final int y) {
				return super.contains(x, y) || ((x == 364 || x == 359) && y > 568 && y < 573);
			}
		},
		F2(new Coordinate(360, 1512), new Coordinate(363, 1517)) {
			public boolean contains(final int x, final int y) {
				return super.contains(x, y) || ((x == 359 || x == 364) && y > 1513 && y < 1516);
			}
		},
		F3(new Coordinate(360, 2457), new Coordinate(364, 2460));

		private final Coordinate lowerBoundingCoordinate;
		private final Coordinate upperBoundingCoordinate;

		Floor(final Coordinate lowerBoundingCoordinate, final Coordinate upperBoundingCoordinate) {
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

	private enum Ladder implements RSObject {
		F1_UP(5, new Coordinate(360, 570)),
		F2_DOWN(6, new Coordinate(360, 1514)),
		F2_UP(5, new Coordinate(363, 1514)),
		F3_DOWN(6, new Coordinate(363, 2458));

		private final int id;
		private final Coordinate coordinate;

		Ladder(final int id, final Coordinate coordinate) {
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

	private enum Door implements RSObject {
		NE(2, new Coordinate(360, 568)),
		NW(2, new Coordinate(364, 569)),
		SE(2, new Coordinate(359, 572)),
		SW(2, new Coordinate(363, 573));

		private static final Door[] DOORS = Door.values();

		private final int id;
		private final Coordinate coordinate;

		Door(final int id, final Coordinate coordinate) {
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
