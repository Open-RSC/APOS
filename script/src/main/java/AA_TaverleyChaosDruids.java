import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Kills Chaos Druids at Taverley Dungeon and banks loot at Falador West Bank.
 * <p>
 * Requirements:
 * Start script at Taverley Dungeon or Falador West Bank with sleeping bag in inventory.
 * <p>
 * Optional Parameter
 * -f,--fightmode <controlled|attack|strength|defense>
 * <p>
 *
 * @Author Chomp
 */
public class AA_TaverleyChaosDruids extends AA_Script {
	private static final Coordinate COORDINATE_CHAOS_DRUIDS = new Coordinate(345, 3318);
	private static final Coordinate COORDINATE_LOAD_LADDER = new Coordinate(376, 3336);
	private static final Coordinate COORDINATE_LOAD_FALADOR = new Coordinate(324, 512);
	private static final Coordinate COORDINATE_LOAD_MEMBERS_GATE = new Coordinate(326, 544);

	private static final int[] ITEM_IDS_LOOT = new int[]{
		33, 34, 35, 36, 40, 42,
		157, 158, 159, 160, 165,
		435, 436, 437, 438, 439, 440, 441, 442, 443, 464, 469,
		526, 527, 1092, 1277};
	private static final int[] ITEM_IDS_PREMIUM_LOOT = new int[]{438, 439, 441, 442, 443, 469, 526, 527, 1092, 1277};
	private static final int[] NPC_IDS_BLOCKING = new int[]{43, 46, 53};

	private static final int NPC_ID_CHAOS_DRUID = 270;
	private static final int NPC_XP_CHAOS_DRUID = 58;

	private static final int COORDINATE_Y_DUNGEON = 3000;
	private static final int MAXIMUM_FATIGUE = 99;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;

	private final int[] loot = new int[3];

	private final Map<Integer, Integer> premiumLoot = new TreeMap<>();
	private final Map<Integer, Spawn> spawnMap = new HashMap<>();

	private Coordinate nextRespawn;
	private long startTime;

	private double initialCombatXp;

	private long openGateTimeout;

	private int playerX;
	private int playerY;

	public AA_TaverleyChaosDruids(final Extension extension) {
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

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("gate")) {
			openGateTimeout = 0L;
		} else {
			super.onServerMessage(message);
		}
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
			if (playerX <= Object.MEMBERS_GATE.coordinate.getX()) {
				if (playerY >= COORDINATE_LOAD_FALADOR.getY()) {
					if (distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
						if (getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
							atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
							return SLEEP_ONE_SECOND;
						}

						walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
						return SLEEP_ONE_TICK;
					}

					walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_TICK;
				}

				walkTo(COORDINATE_LOAD_FALADOR.getX(), COORDINATE_LOAD_FALADOR.getY());
				return SLEEP_ONE_TICK;
			}

			if (distanceTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (System.currentTimeMillis() <= openGateTimeout) {
					return 0;
				}

				atObject(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
				openGateTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
				return 0;
			}

			walkTo(Object.MEMBERS_GATE.coordinate.getX() + 1, Object.MEMBERS_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (!inCombat()) {
			final int blockingNpc = getBlockingNpc();

			if (blockingNpc != -1) {
				attackNpc(blockingNpc);
				return SLEEP_ONE_TICK;
			}
		}

		if (playerY >= COORDINATE_LOAD_LADDER.getY()) {
			atObject(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(COORDINATE_LOAD_LADDER.getX(), COORDINATE_LOAD_LADDER.getY());
		return SLEEP_ONE_TICK;
	}

	private int kill() {
		if (Area.CHAOS_DRUIDS.contains(playerX, playerY)) {
			if (inCombat()) {
				return 0;
			}

			if (getFatigue() >= MAXIMUM_FATIGUE) {
				return sleep();
			}

			final int[] druid = getNpcById(NPC_ID_CHAOS_DRUID);

			if (druid[0] != -1) {
				attackNpc(druid[0]);
				return SLEEP_ONE_TICK;
			}

			updateLoot();

			if (loot[0] != -1) {
				pickupItem(loot[0], loot[1], loot[2]);
				return SLEEP_ONE_TICK;
			}

			if (nextRespawn != null &&
				(playerX != nextRespawn.getX() || playerY != nextRespawn.getY())) {
				walkTo(nextRespawn.getX(), nextRespawn.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (playerY > COORDINATE_Y_DUNGEON) {
			if (!inCombat()) {
				final int blockingNpc = getBlockingNpc();

				if (blockingNpc != -1) {
					attackNpc(blockingNpc);
					return SLEEP_ONE_TICK;
				}
			}

			if (!spawnMap.isEmpty()) {
				resetSpawns();
			}

			walkTo(COORDINATE_CHAOS_DRUIDS.getX(), COORDINATE_CHAOS_DRUIDS.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerX > Object.MEMBERS_GATE.coordinate.getX()) {
			if (distanceTo(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				atObject(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			if (System.currentTimeMillis() <= openGateTimeout) {
				return 0;
			}

			atObject(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
			openGateTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
			return 0;
		}

		if (playerY <= COORDINATE_LOAD_MEMBERS_GATE.getY()) {
			walkTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BANK.contains(playerX, playerY) &&
			getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(COORDINATE_LOAD_MEMBERS_GATE.getX(), COORDINATE_LOAD_MEMBERS_GATE.getY());
		return SLEEP_ONE_TICK;
	}

	private void updateBankLoot() {
		for (final int itemId : ITEM_IDS_PREMIUM_LOOT) {
			final int bankCount = bankCount(itemId);

			if (bankCount == 0) {
				continue;
			}

			premiumLoot.put(itemId, bankCount);
		}
	}

	private int getBlockingNpc() {
		final int direction = bot.getMobDirection(bot.getPlayer());

		for (int index = 0; index < bot.getNpcCount(); index++) {
			if (!inArray(NPC_IDS_BLOCKING, getNpcId(index))) {
				continue;
			}

			final int npcX = getNpcX(index);
			final int npcY = getNpcY(index);

			if (npcY == playerY &&
				((npcX == playerX - 1 && direction == DIR_EAST) || (npcX == playerX + 1 && direction == DIR_WEST))) {
				return index;
			}

			if (npcX == playerX &&
				((npcY == playerY - 1 && (direction == DIR_NORTH || direction == DIR_NORTHEAST)) || (npcY == playerY + 1 && (direction == DIR_SOUTH || direction == DIR_SOUTHWEST)))) {
				return index;
			}
		}

		return -1;
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

			if (!Area.CHAOS_DRUIDS.contains(groundItemX, groundItemY)) {
				continue;
			}

			final int distance = distanceTo(groundItemX, groundItemY);

			if (distance < currentDistance) {
				loot[0] = groundItemId;
				loot[1] = groundItemX;
				loot[2] = groundItemY;

				currentDistance = distance;
			}
		}
	}

	private void resetSpawns() {
		spawnMap.clear();
		nextRespawn = null;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Taverley Chaos Druids", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Pid: @whi@%d", bot.getMobServerIndex(bot.getPlayer())),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final int kills = (int) xpGained / NPC_XP_CHAOS_DRUID;

		drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s kills@cya@/@whi@hr@cya@)",
				kills, toUnitsPerHour(kills, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (nextRespawn != null) {
			drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)",
					nextRespawn.getX(), nextRespawn.getY()),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}

		if (!premiumLoot.isEmpty()) {
			drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

			for (final Map.Entry<Integer, Integer> entry : premiumLoot.entrySet()) {
				drawString(String.format("@gre@%s: @whi@%d", getItemNameId(entry.getKey()), entry.getValue()),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
			}
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (bot.getNpcId(npc) != NPC_ID_CHAOS_DRUID) {
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
		CHAOS_DRUIDS(new Coordinate(341, 3314), new Coordinate(353, 3325)),
		BANK(new Coordinate(328, 549), new Coordinate(334, 557));

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
		LADDER_UP(5, new Coordinate(376, 3352)),
		LADDER_DOWN(6, new Coordinate(376, 520)),
		MEMBERS_GATE(137, new Coordinate(341, 487)),
		BANK_DOORS(64, new Coordinate(327, 552));

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
