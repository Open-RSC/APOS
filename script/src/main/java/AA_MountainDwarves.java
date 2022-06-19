import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Kills Mountain Dwarves in the Dwarven Tunnel under White Wolf Mountain.
 * Banks loot in Catherby.
 * <p>
 * Required:
 * Start script at Catherby bank or at the dwarves with sleeping bag in inventory.
 * <p>
 * Optional Parameter:
 * -f,--fightmode <controlled|attack|strength|defense> (default strength)
 * <p>
 *
 * @Author Chomp
 */
public class AA_MountainDwarves extends AA_Script {
	private static final int[] ITEM_IDS_LOOT = new int[]{1277, 1092, 526, 527, 40, 41, 157, 158, 159, 160};

	private static final int NPC_ID_MOUNTAIN_DWARF = 356;
	private static final int NPC_XP_MOUNTAIN_DWARF = 56;

	private static final int MAXIMUM_FATIGUE = 99;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;
	private static final int MAXIMUM_DISTANCE_FROM_NPC = 3;

	private static final int COORDINATE_Y_DUNGEON = 3000;

	private final Map<Integer, Integer> bankedLoot = new TreeMap<>();
	private final Map<Integer, Spawn> spawnMap = new HashMap<>();

	private final int[] loot = new int[3];

	private Coordinate nextRespawn;
	private long startTime;

	private double initialCombatXp;

	private int playerX;
	private int playerY;

	public AA_MountainDwarves(final Extension extension) {
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

		return getInventoryCount() == MAX_INV_SIZE || isBanking() ? bank() : kill();
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (!isBanking()) {
				return openBank();
			}

			for (int index = 0; index < getInventoryCount(); index++) {
				final int itemId = getInventoryId(index);

				if (!inArray(ITEM_IDS_LOOT, itemId)) {
					continue;
				}

				deposit(itemId, getInventoryCount(itemId));
				return SLEEP_ONE_TICK;
			}

			updateBankLoot();
			closeBank();
			return SLEEP_ONE_SECOND;
		}

		if (playerY < COORDINATE_Y_DUNGEON) {
			if (distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
					atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.TAVERN.contains(playerX, playerY)) {
			if (inCombat()) {
				walkTo(playerX, playerY);
				return SLEEP_ONE_TICK;
			}

			if (getObjectIdFromCoords(Object.TAVERN_DOORS.coordinate.getX(), Object.TAVERN_DOORS.coordinate.getY()) == Object.TAVERN_DOORS.id) {
				atObject(Object.TAVERN_DOORS.coordinate.getX(), Object.TAVERN_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.STAIRS_UP.coordinate.getX(), Object.STAIRS_UP.coordinate.getY() + 3);
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(Object.STAIRS_UP.coordinate.getX(), Object.STAIRS_UP.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			atObject(Object.STAIRS_UP.coordinate.getX(), Object.STAIRS_UP.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.STAIRS_UP.coordinate.getX(), Object.STAIRS_UP.coordinate.getY() + 3);
		return SLEEP_ONE_TICK;
	}

	private int kill() {
		if (playerY > COORDINATE_Y_DUNGEON) {
			if (Area.TAVERN.contains(playerX, playerY)) {
				return combatCycle();
			}

			if (distanceTo(Object.TAVERN_DOORS.coordinate.getX(), Object.TAVERN_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (getObjectIdFromCoords(Object.TAVERN_DOORS.coordinate.getX(), Object.TAVERN_DOORS.coordinate.getY()) == Object.TAVERN_DOORS.id) {
					atObject(Object.TAVERN_DOORS.coordinate.getX(), Object.TAVERN_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				if (!spawnMap.isEmpty()) {
					spawnMap.clear();
					nextRespawn = null;
				}

				walkTo(Object.TAVERN_DOORS.coordinate.getX() + 1, Object.TAVERN_DOORS.coordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			walkTo(Object.TAVERN_DOORS.coordinate.getX() + 1, Object.TAVERN_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BANK.contains(playerX, playerY)) {
			if (getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
				atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.STAIRS_DOWN.coordinate.getX(), Object.STAIRS_DOWN.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(Object.STAIRS_DOWN.coordinate.getX(), Object.STAIRS_DOWN.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			atObject(Object.STAIRS_DOWN.coordinate.getX(), Object.STAIRS_DOWN.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.STAIRS_DOWN.coordinate.getX(), Object.STAIRS_DOWN.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private void updateBankLoot() {
		for (final int itemId : ITEM_IDS_LOOT) {
			final int bankCount = bankCount(itemId);

			if (bankCount == 0) {
				continue;
			}

			bankedLoot.put(itemId, bankCount);
		}
	}

	private int combatCycle() {
		if (inCombat()) {
			return 0;
		}

		if (getFatigue() >= MAXIMUM_FATIGUE) {
			return sleep();
		}

		updateLoot();

		if (loot[0] != -1) {
			pickupItem(loot[0], loot[1], loot[2]);
			return SLEEP_ONE_TICK;
		}

		final int[] mountainDwarf = getNpcById(NPC_ID_MOUNTAIN_DWARF);

		if (mountainDwarf[0] != -1) {
			if (distanceTo(mountainDwarf[1], mountainDwarf[2]) > MAXIMUM_DISTANCE_FROM_NPC) {
				walkTo(mountainDwarf[1], mountainDwarf[2]);
				return SLEEP_ONE_TICK;
			}

			attackNpc(mountainDwarf[0]);
			return SLEEP_ONE_TICK;
		}

		if (nextRespawn != null &&
			(playerX != nextRespawn.getX() || playerY != nextRespawn.getY())) {
			walkTo(nextRespawn.getX(), nextRespawn.getY());
			return SLEEP_ONE_TICK;
		}

		return 0;
	}

	private void updateLoot() {
		loot[0] = -1;

		for (final int itemId : ITEM_IDS_LOOT) {
			for (int index = 0; index < getGroundItemCount(); index++) {
				final int groundItemId = getGroundItemId(index);

				if (groundItemId != itemId) {
					continue;
				}

				final int groundItemX = getItemX(index);
				final int groundItemY = getItemY(index);

				if (!Area.TAVERN.contains(groundItemX, groundItemY)) {
					continue;
				}

				loot[0] = groundItemId;
				loot[1] = groundItemX;
				loot[2] = groundItemY;
				return;
			}
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Mountain Dwarves", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Pid: @whi@%d", bot.getMobServerIndex(bot.getPlayer())),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final int kills = (int) xpGained / NPC_XP_MOUNTAIN_DWARF;

		drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, toUnitsPerHour(kills, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (nextRespawn != null) {
			drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)", nextRespawn.getX(), nextRespawn.getY()),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}

		if (!bankedLoot.isEmpty()) {
			drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

			for (final Map.Entry<Integer, Integer> entry : bankedLoot.entrySet()) {
				drawString(String.format("@gre@%s: @whi@%d", getItemNameId(entry.getKey()), entry.getValue()),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
			}
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (bot.getNpcId(npc) != NPC_ID_MOUNTAIN_DWARF) {
			return;
		}

		final int npcX = bot.getMobLocalX(npc) + bot.getAreaX();
		final int npcY = bot.getMobLocalY(npc) + bot.getAreaY();

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
		BANK(new Coordinate(437, 491), new Coordinate(443, 496)),
		TAVERN(new Coordinate(390, 3295), new Coordinate(402, 3302));

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
		STAIRS_UP(43, new Coordinate(426, 3290)),
		STAIRS_DOWN(359, new Coordinate(426, 458)),
		BANK_DOORS(64, new Coordinate(439, 497)),
		TAVERN_DOORS(64, new Coordinate(397, 3294));

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
