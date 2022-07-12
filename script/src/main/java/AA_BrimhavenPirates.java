import java.util.HashMap;
import java.util.Map;

/**
 * Kills L30 Pirates at The Dead Man's Chest tavern on Brimhaven.
 * <p>
 * Required:
 * Start script at the tavern with sleeping bag in inventory.
 * <p>
 * Optional Parameters:
 * -f,--fightmode <controlled|attack|strength|defense> (default strength)
 * -t,--tavern (only fight pirates in the tavern)
 * <p>
 *
 * @Author Chomp
 */
public class AA_BrimhavenPirates extends AA_Script {
	private static final int[] ITEM_IDS_LOOT = new int[]{1277, 1092, 526, 527, 42, 40, 41, 31, 33, 34};
	private static final int[] ITEM_IDS_STACKABLE = new int[]{31, 33, 34, 40, 41, 42};

	private static final int NPC_ID_PIRATE = 264;
	private static final int NPC_XP_PIRATE = 82;

	private static final int MAXIMUM_FATIGUE = 99;
	private static final int MAXIMUM_DISTANCE_FROM_NPC = 3;

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();

	private final int[] pirate = new int[3];
	private final int[] loot = new int[3];

	private Coordinate nextRespawn;
	private long startTime;

	private double initialCombatXp;

	private long doorTimeout;

	private int playerX;
	private int playerY;

	private boolean tavernOnly;

	public AA_BrimhavenPirates(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-f":
					case "--fightmode":
						combatStyle = CombatStyle.valueOf(args[++i].toUpperCase());
						break;
					case "-t":
					case "--tavern":
						tavernOnly = true;
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		setCombatStyle(combatStyle.getIndex());
		initialCombatXp = getTotalCombatXp();
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		if (bot.getCombatStyle() != combatStyle.getIndex()) setCombatStyle(combatStyle.getIndex());

		if (inCombat()) {
			return 0;
		}

		if (getFatigue() >= MAXIMUM_FATIGUE) {
			return sleep();
		}

		updateLoot();

		if (loot[0] != -1 && (getInventoryCount() != MAX_INV_SIZE ||
			(inArray(ITEM_IDS_STACKABLE, loot[0]) && hasInventoryItem(loot[0])))) {
			if (needToOpenDoor(loot[1], loot[2])) {
				return useDoor(true);
			}

			pickupItem(loot[0], loot[1], loot[2]);
			return SLEEP_ONE_TICK;
		}

		updatePirate();

		if (pirate[0] != -1) {
			if (needToOpenDoor(pirate[1], pirate[2])) {
				return useDoor(true);
			}

			if (distanceTo(pirate[1], pirate[2]) > MAXIMUM_DISTANCE_FROM_NPC) {
				walkTo(pirate[1], pirate[2]);
			} else {
				attackNpc(pirate[0]);
			}

			return SLEEP_ONE_TICK;
		}

		if (!Area.TAVERN.contains(playerX, playerY)) {
			if (isDoorClosed()) {
				return useDoor(true);
			}

			walkTo(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (!isDoorClosed()) {
			return useDoor(false);
		}

		if (nextRespawn != null &&
			(playerX != nextRespawn.getX() || playerY != nextRespawn.getY())) {
			walkTo(nextRespawn.getX(), nextRespawn.getY());
			return SLEEP_ONE_TICK;
		}

		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("shut") || message.endsWith("open")) {
			doorTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	private void updateLoot() {
		loot[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < getGroundItemCount(); index++) {
			final int groundItemId = getGroundItemId(index);

			if (!inArray(ITEM_IDS_LOOT, groundItemId)) {
				continue;
			}

			final int groundItemX = getItemX(index);
			final int groundItemY = getItemY(index);

			if ((tavernOnly && !Area.TAVERN.contains(groundItemX, groundItemY)) ||
				!Area.ISLAND.contains(groundItemX, groundItemY)) {
				continue;
			}

			final int distance = distanceTo(groundItemX, groundItemY, playerX, playerY);

			if (distance < currentDistance) {
				loot[0] = groundItemId;
				loot[1] = groundItemX;
				loot[2] = groundItemY;
				currentDistance = distance;
			}
		}
	}

	private boolean needToOpenDoor(final int x, final int y) {
		if (!isDoorClosed()) {
			return false;
		}

		return Area.TAVERN.contains(playerX, playerY) != Area.TAVERN.contains(x, y);
	}

	private int useDoor(final boolean open) {
		if (System.currentTimeMillis() <= doorTimeout) {
			return 0;
		}

		if (open) {
			atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
		} else {
			atWallObject2(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
		}

		doorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private void updatePirate() {
		pirate[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < bot.getNpcCount(); index++) {
			if (getNpcId(index) != NPC_ID_PIRATE || isNpcInCombat(index)) {
				continue;
			}

			final int npcX = getNpcX(index);
			final int npcY = getNpcY(index);

			if ((tavernOnly && !Area.TAVERN.contains(npcX, npcY)) || !Area.ISLAND.contains(npcX, npcY)) {
				continue;
			}

			final int distance = distanceTo(npcX, npcY, playerX, playerY);

			if (distance < currentDistance) {
				pirate[0] = index;
				pirate[1] = npcX;
				pirate[2] = npcY;
				currentDistance = distance;
			}
		}
	}

	private boolean isDoorClosed() {
		return getWallObjectIdFromCoords(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY()) == Object.DOOR.id;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Brimhaven Pirates", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		final int kills = (int) xpGained / NPC_XP_PIRATE;

		drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, toUnitsPerHour(kills, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (bot.getNpcId(npc) != NPC_ID_PIRATE) {
			return;
		}

		final int npcX = bot.getMobLocalX(npc) + bot.getAreaX();
		final int npcY = bot.getMobLocalY(npc) + bot.getAreaY();

		if (!Area.TAVERN.contains(npcX, npcY)) {
			return;
		}

		final int serverIndex = bot.getMobServerIndex(npc);
		final Spawn spawn = spawnMap.get(serverIndex);

		if (spawn != null) {
			spawn.getCoordinate().set(npcX, npcY);
			spawn.setTimestamp(System.currentTimeMillis());
		} else {
			spawnMap.put(serverIndex, new Spawn(new Coordinate(npcX, npcY), System.currentTimeMillis()));
		}

		nextRespawn = spawnMap.isEmpty() ? null : spawnMap.values().stream().sorted().findFirst().get().getCoordinate();
	}

	private enum Area implements RSArea {
		ISLAND(new Coordinate(444, 694), new Coordinate(459, 709)),
		TAVERN(new Coordinate(449, 696), new Coordinate(454, 706));

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
		DOOR(2, new Coordinate(449, 699));

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
