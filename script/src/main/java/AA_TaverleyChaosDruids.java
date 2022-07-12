import java.util.Comparator;
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
 * <controlled|attack|strength|defense> (default strength)
 * <p>
 *
 * @Author Chomp
 */
public class AA_TaverleyChaosDruids extends AA_Script {
	private static final Coordinate COORD_CHAOS_DRUIDS = new Coordinate(345, 3318);
	private static final Coordinate COORD_LADDER = new Coordinate(376, 3336);
	private static final Coordinate COORD_FALADOR = new Coordinate(324, 512);
	private static final Coordinate COORD_MEMBERS_GATE = new Coordinate(326, 544);

	private static final int[] ITEM_IDS_LOOT = new int[]{
		33, 34, 35, 36, 40, 42,
		157, 158, 159, 160, 165,
		435, 436, 437, 438, 439, 440, 441, 442, 443, 464, 469,
		526, 527, 1092, 1277};
	private static final int[] ITEM_IDS_PREMIUM_LOOT = new int[]{438, 439, 441, 442, 443, 469, 526, 527, 1092, 1277};
	private static final int[] NPC_IDS_BLOCKING = new int[]{43, 46, 53};

	private static final int NPC_ID_CHAOS_DRUID = 270;
	private static final int NPC_XP_CHAOS_DRUID = 58;

	private static final int COORD_Y_DUNGEON = 3000;
	private static final int MAX_FATIGUE = 99;
	private static final int MAX_DIST_FROM_OBJ = 18;

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

		if (!parameters.isEmpty()) combatStyle = CombatStyle.valueOf(parameters.toUpperCase());

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

		if (playerY < COORD_Y_DUNGEON) {
			final Coordinate gate = Object.MEMBERS_GATE.getCoordinate();

			if (playerX <= gate.getX()) {
				if (playerY >= COORD_FALADOR.getY()) {
					final Coordinate doors = Object.BANK_DOORS.getCoordinate();

					if (distanceTo(doors.getX(), doors.getY()) <= MAX_DIST_FROM_OBJ) {
						if (getObjectIdFromCoords(doors.getX(), doors.getY()) == Object.BANK_DOORS.id) {
							atObject(doors.getX(), doors.getY());
							return SLEEP_ONE_SECOND;
						}

						walkTo(doors.getX() + 1, doors.getY());
						return SLEEP_ONE_TICK;
					}

					walkTo(doors.getX(), doors.getY());
					return SLEEP_ONE_TICK;
				}

				walkTo(COORD_FALADOR.getX(), COORD_FALADOR.getY());
				return SLEEP_ONE_TICK;
			}

			if (distanceTo(gate.getX(), gate.getY()) <= MAX_DIST_FROM_OBJ) {
				if (System.currentTimeMillis() <= openGateTimeout) {
					return 0;
				}

				atObject(gate.getX(), gate.getY());
				openGateTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
				return 0;
			}

			walkTo(gate.getX() + 1, gate.getY());
			return SLEEP_ONE_TICK;
		}

		if (!inCombat()) {
			final int blockingNpc = getBlockingNpc();

			if (blockingNpc != -1) {
				attackNpc(blockingNpc);
				return SLEEP_ONE_TICK;
			}
		}

		if (playerY >= COORD_LADDER.getY()) {
			final Coordinate ladder = Object.LADDER_UP.getCoordinate();
			atObject(ladder.getX(), ladder.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(COORD_LADDER.getX(), COORD_LADDER.getY());
		return SLEEP_ONE_TICK;
	}

	private int kill() {
		if (Area.CHAOS_DRUIDS.contains(playerX, playerY)) {
			if (inCombat()) {
				return 0;
			}

			if (getFatigue() >= MAX_FATIGUE) {
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

		if (playerY > COORD_Y_DUNGEON) {
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

			walkTo(COORD_CHAOS_DRUIDS.getX(), COORD_CHAOS_DRUIDS.getY());
			return SLEEP_ONE_TICK;
		}

		final Coordinate gate = Object.MEMBERS_GATE.getCoordinate();

		if (playerX > gate.getX()) {
			final Coordinate ladder = Object.LADDER_DOWN.getCoordinate();

			if (distanceTo(ladder.getX(), ladder.getY()) <= MAX_DIST_FROM_OBJ) {
				atObject(ladder.getX(), ladder.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(ladder.getX(), ladder.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(gate.getX(), gate.getY()) <= MAX_DIST_FROM_OBJ) {
			if (System.currentTimeMillis() <= openGateTimeout) {
				return 0;
			}

			atObject(gate.getX(), gate.getY());
			openGateTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
			return 0;
		}

		if (playerY <= COORD_MEMBERS_GATE.getY()) {
			walkTo(gate.getX(), gate.getY());
			return SLEEP_ONE_TICK;
		}

		final Coordinate doors = Object.BANK_DOORS.getCoordinate();

		if (Area.BANK.contains(playerX, playerY) &&
			getObjectIdFromCoords(doors.getX(), doors.getY()) == Object.BANK_DOORS.id) {
			atObject(doors.getX(), doors.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(COORD_MEMBERS_GATE.getX(), COORD_MEMBERS_GATE.getY());
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
				((npcY == playerY - 1 && (direction == DIR_NORTH || direction == DIR_NORTHEAST)) ||
					(npcY == playerY + 1 && (direction == DIR_SOUTH || direction == DIR_SOUTHWEST)))) {
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

		final double xpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

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
		if (bot.getNpcId(npc) != NPC_ID_CHAOS_DRUID) return;

		final int npcX = getX(npc);
		final int npcY = getY(npc);

		final int serverIndex = getServerIndex(npc);

		final Spawn spawn = spawnMap.get(serverIndex);

		if (spawn != null) {
			spawn.getCoordinate().set(npcX, npcY);
			spawn.setTimestamp(Long.MAX_VALUE);
		} else {
			spawnMap.put(serverIndex, new Spawn(new Coordinate(npcX, npcY), Long.MAX_VALUE));
		}

		nextRespawn = getNextRespawn();
	}

	@Override
	public void onNpcDespawned(final java.lang.Object npc) {
		final int serverIndex = getServerIndex(npc);
		final Spawn spawn = spawnMap.get(serverIndex);
		if (spawn == null) return;
		spawn.setTimestamp(System.currentTimeMillis());
		nextRespawn = getNextRespawn();
	}

	private Coordinate getNextRespawn() {
		if (spawnMap.isEmpty()) return null;
		return spawnMap.values().stream().min(Comparator.naturalOrder()).get().getCoordinate();
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
