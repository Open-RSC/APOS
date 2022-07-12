/**
 * Kills Darkwizards and collects runes/coins at the Dark Wizard Tower.
 * Start at the tower with sleeping bag in inventory.
 * <p>
 * Optional Parameter:
 * --no-doors (Disables closing doors)
 * -f,--fightmode <controlled|strength|attack|defense>
 * <p>
 *
 * @Author Chomp
 */
public class AA_DarkWizardTower extends AA_Script {
	private static final int[] NPC_IDS_DARK_WIZARD = new int[]{57, 60};

	private static final int[] ITEM_IDS_LOOT = new int[]{10, 31, 32, 33, 34, 35, 36, 38, 40, 41, 42, 46, 619};

	private static final int MAXIMUM_FATIGUE = 99;

	private final int[] groundItem = new int[4];

	private long startTime;

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
						combatStyle = CombatStyle.valueOf(args[++i].toUpperCase());
						break;
					case "--no-doors":
						doors = false;
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		setCombatStyle(combatStyle.getIndex());
		initialCombatXp = getTotalCombatXp();
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (bot.getCombatStyle() != combatStyle.getIndex()) setCombatStyle(combatStyle.getIndex());

		if (inCombat()) {
			return 0;
		}

		if (getFatigue() >= MAXIMUM_FATIGUE) {
			return sleep();
		}

		playerX = getX();
		playerY = getY();

		return Floor.F2.contains(playerX, playerY) || Floor.F3.contains(playerX, playerY) ?
			handleF2F3() :
			handleF1();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("ladder")) {
			ladderTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("open") || message.endsWith("shut")) {
			doorTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	private int handleF2F3() {
		final int[] wizard = getNpcById(NPC_IDS_DARK_WIZARD);

		if (wizard[0] != -1) {
			attackNpc(wizard[0]);
			return SLEEP_ONE_TICK;
		}

		updateGroundItem(false);

		if (groundItem[0] != -1) {
			pickupItem(groundItem[3], groundItem[1], groundItem[2]);
			return SLEEP_ONE_TICK;
		}

		return nextFloor();
	}

	private int handleF1() {
		final int[] wizard = getNpcById(NPC_IDS_DARK_WIZARD);

		if (wizard[0] != -1) {
			if (Floor.F1.contains(playerX, playerY) != Floor.F1.contains(wizard[1], wizard[2])) {
				final Door door = getNearestDoorTo(wizard[1], wizard[2]);

				final int doorX = door.coordinate.getX();
				final int doorY = door.coordinate.getY();

				if (getWallObjectIdFromCoords(doorX, doorY) == door.id) {
					if (System.currentTimeMillis() <= doorTimeout) {
						return 0;
					}

					atWallObject(doorX, doorY);
					doorTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
					return 0;
				}
			}

			attackNpc(wizard[0]);
			return SLEEP_ONE_TICK;
		}

		updateGroundItem(true);

		if (groundItem[0] != -1) {
			pickupItem(groundItem[3], groundItem[1], groundItem[2]);
			return SLEEP_ONE_TICK;
		}

		if (!Floor.F1.contains(playerX, playerY)) {
			return enterF1();
		}

		if (doors) {
			for (final Door door : Door.DOORS) {
				final int doorX = door.coordinate.getX();
				final int doorY = door.coordinate.getY();

				if (getWallObjectIdFromCoords(doorX, doorY) != door.id) {
					if (System.currentTimeMillis() <= doorTimeout) {
						return 0;
					}

					atWallObject2(doorX, doorY);
					doorTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
					return 0;
				}
			}
		}

		return nextFloor();
	}

	private void updateGroundItem(final boolean bottomFloor) {
		groundItem[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < getGroundItemCount(); index++) {
			final int groundItemId = getGroundItemId(index);

			if (!inArray(ITEM_IDS_LOOT, groundItemId)) {
				continue;
			}

			final int groundItemX = getItemX(index);
			final int groundItemY = getItemY(index);

			if (bottomFloor && !Floor.F1.contains(groundItemX, groundItemY)) {
				continue;
			}

			final int distance = distanceTo(groundItemX, groundItemY);

			if (distance < currentDistance) {
				groundItem[0] = index;
				groundItem[1] = groundItemX;
				groundItem[2] = groundItemY;
				groundItem[3] = groundItemId;

				currentDistance = distance;
			}
		}
	}

	private int nextFloor() {
		if (System.currentTimeMillis() <= ladderTimeout) {
			return 0;
		}

		if (Floor.F1.contains(playerX, playerY)) {
			checkTopFloor = true;

			atObject(Ladder.F1_UP.coordinate.getX(),
				Ladder.F1_UP.coordinate.getY());
		} else if (Floor.F2.contains(playerX, playerY)) {
			if (checkTopFloor) {
				atObject(Ladder.F2_UP.coordinate.getX(),
					Ladder.F2_UP.coordinate.getY());
			} else {
				atObject(Ladder.F2_DOWN.coordinate.getX(),
					Ladder.F2_DOWN.coordinate.getY());
			}
		} else if (Floor.F3.contains(playerX, playerY)) {
			checkTopFloor = false;

			atObject(Ladder.F3_DOWN.coordinate.getX(),
				Ladder.F3_DOWN.coordinate.getY());
		} else {
			return SLEEP_ONE_TICK;
		}

		ladderTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
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

	private int enterF1() {
		final Door door = getNearestDoorTo(playerX, playerY);

		final int doorX = door.coordinate.getX();
		final int doorY = door.coordinate.getY();

		if (getWallObjectIdFromCoords(doorX, doorY) != door.id) {
			walkTo(doorX, doorY);
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= doorTimeout) {
			return 0;
		}

		atWallObject(doorX, doorY);
		doorTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Dark Wizard Tower", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);
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
			return lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return upperBoundingCoordinate;
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
			return id;
		}

		public Coordinate getCoordinate() {
			return coordinate;
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
			return id;
		}

		public Coordinate getCoordinate() {
			return coordinate;
		}
	}
}
