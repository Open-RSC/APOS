import java.util.*;

/**
 * Kills Druids at the Druidic Circle and banks at Falador West Bank.
 * <p>
 * Required:
 * Start script at Druids or Falador West Bank and have sleeping bag in inventory.
 * <p>
 * Optional Parameters:
 * -a,--alt <altName>
 * -f,--fightmode <controlled|attack|strength|defense> (default strength)
 * <p>
 * Notes:
 * Specifying names of alts will enable spawn synchronization between accounts via PM.
 * Alt accounts need to have each other added as friends.
 * Replace any spaces in an rsn with an underscore _.
 * e.g. -f attack -a bot02 -a bot03
 * <p>
 *
 * @Author Chomp
 */
public class AA_DruidsCircle extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_SOUTH_MEMBERS_GATE = new Coordinate(346, 479);
	private static final Coordinate COORDINATE_LOAD_NORTH_MEMBERS_GATE = new Coordinate(326, 544);
	private static final Coordinate COORDINATE_LOAD_FALADOR = new Coordinate(324, 512);

	private static final int[] ITEM_IDS_LOOT = new int[]{10, 31, 32, 34, 41, 42, 220, 165, 435, 436, 437, 438, 439, 440, 441, 442, 443, 566};
	private static final int[] ITEM_IDS_PREMIUM_LOOT = new int[]{41, 42, 220, 438, 439, 440, 441, 442, 443, 566};

	private static final int NPC_ID_DRUID = 200;
	private static final int NPC_XP_DRUID = 78;

	private static final int MAXIMUM_FATIGUE = 99;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;

	private final int[] loot = new int[3];

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();
	private final Map<Integer, Integer> premiumLoot = new TreeMap<>();

	private String[] alts;
	private Spawn nextSpawn;
	private Iterator<Map.Entry<Integer, Spawn>> syncDataIterator;
	private String syncPlayerName;
	private long startTime;

	private double initialCombatXp;

	private long gateTimeout;
	private long syncRequestTimeout;

	private int playerX;
	private int playerY;

	private boolean syncWithAlt;

	public AA_DruidsCircle(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!parameters.isEmpty()) {
			List<String> alts = null;

			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-a":
					case "--alt":
						if (alts == null) {
							alts = new ArrayList<>();
						}

						final String altName = args[++i].replace('_', ' ');

						if (!isFriend(altName)) {
							addFriend(altName);
						}

						alts.add(altName);
						break;
					case "-f":
					case "--fightmode":
						combatStyle = CombatStyle.valueOf(args[++i].toUpperCase());
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}

			if (alts != null) {
				this.alts = alts.toArray(new String[0]);
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
		if (message.endsWith("gate") || message.endsWith("shut") || message.endsWith("open")) {
			gateTimeout = 0L;
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

		if (playerX <= Object.MEMBERS_GATE.coordinate.getX()) {
			if (playerY >= COORDINATE_LOAD_FALADOR.getY()) {
				if (distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT &&
					getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
					atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			walkTo(COORDINATE_LOAD_FALADOR.getX(), COORDINATE_LOAD_FALADOR.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerY >= COORDINATE_LOAD_SOUTH_MEMBERS_GATE.getY()) {
			if (System.currentTimeMillis() <= gateTimeout) {
				return 0;
			}

			atObject(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
			gateTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		walkTo(COORDINATE_LOAD_SOUTH_MEMBERS_GATE.getX(), COORDINATE_LOAD_SOUTH_MEMBERS_GATE.getY());
		return SLEEP_ONE_TICK;
	}

	private int kill() {
		if (Area.DRUIDS.contains(playerX, playerY)) {
			return combatCycle();
		}

		if (playerX > Object.MEMBERS_GATE.coordinate.getX()) {
			if (!spawnMap.isEmpty()) {
				resetSpawns();

				if (alts != null) {
					resetSync();
				}
			}

			walkTo(Area.DRUIDS.lowerBoundingCoordinate.getX(), Area.DRUIDS.lowerBoundingCoordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerY <= COORDINATE_LOAD_NORTH_MEMBERS_GATE.getY()) {
			if (distanceTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (System.currentTimeMillis() <= gateTimeout) {
					return 0;
				}

				atObject(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
				gateTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				return 0;
			}

			walkTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BANK.contains(playerX, playerY) &&
			getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(COORDINATE_LOAD_NORTH_MEMBERS_GATE.getX(), COORDINATE_LOAD_NORTH_MEMBERS_GATE.getY());
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

	private int combatCycle() {
		if (inCombat()) {
			if (syncWithAlt) {
				return syncWithAlt();
			}

			return 0;
		}

		if (getFatigue() >= MAXIMUM_FATIGUE) {
			return sleep();
		}

		final int[] druid = getNpcById(NPC_ID_DRUID);

		if (druid[0] != -1) {
			attackNpc(druid[0]);
			return SLEEP_ONE_TICK;
		}

		updateLoot();

		if (loot[0] != -1) {
			pickupItem(loot[0], loot[1], loot[2]);
			return SLEEP_ONE_TICK;
		}

		if (nextSpawn == null) {
			return 0;
		}

		final Coordinate spawnCoordinate = nextSpawn.getCoordinate();

		if (playerX != spawnCoordinate.getX() || playerY != spawnCoordinate.getY()) {
			if (alts != null && isSpawnCoordinateOccupied(spawnCoordinate)) {
				nextSpawn.setTimestamp(System.currentTimeMillis());
				nextSpawn = getOldestSpawn();
				return 0;
			}

			walkTo(spawnCoordinate.getX(), spawnCoordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (syncWithAlt) {
			return syncWithAlt();
		}

		return 0;
	}

	private void resetSpawns() {
		spawnMap.clear();
		nextSpawn = null;
	}

	private void resetSync() {
		syncWithAlt = false;
		syncPlayerName = null;
		syncDataIterator = null;
	}

	private int syncWithAlt() {
		if (syncDataIterator.hasNext()) {
			final Map.Entry<Integer, Spawn> entry = syncDataIterator.next();

			final int serverIndex = entry.getKey();
			final Spawn spawn = entry.getValue();

			final Coordinate coordinate = spawn.getCoordinate();
			final long timestamp = spawn.getTimestamp();

			sendPrivateMessage(String.format("%d,%d,%d,%d", serverIndex, coordinate.getX(), coordinate.getY(), timestamp), syncPlayerName);
			return SLEEP_ONE_TICK;
		}

		resetSync();
		return 0;
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

			if (!Area.DRUIDS.contains(groundItemX, groundItemY)) {
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

	private boolean isSpawnCoordinateOccupied(final Coordinate spawnCoordinate) {
		for (int index = 0; index < bot.getPlayerCount(); index++) {
			final String playerName = getPlayerName(index);

			if (isAnAlt(playerName) &&
				getPlayerX(index) == spawnCoordinate.getX() &&
				getPlayerY(index) == spawnCoordinate.getY()) {
				return true;
			}
		}

		return false;
	}

	private Spawn getOldestSpawn() {
		if (spawnMap.isEmpty()) {
			return null;
		}

		return spawnMap.values().stream().sorted().findFirst().get();
	}

	private boolean isAnAlt(final String playerName) {
		for (final String alt : alts) {
			if (alt.equalsIgnoreCase(playerName)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Druids Circle", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		final int kills = (int) xpGained / NPC_XP_DRUID;

		drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, toUnitsPerHour(kills, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (premiumLoot.isEmpty()) return;

		drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		for (final Map.Entry<Integer, Integer> entry : premiumLoot.entrySet()) {
			drawString(String.format("@gre@%s: @whi@%d", getItemNameId(entry.getKey()), entry.getValue()),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}
	}

	@Override
	public void onPrivateMessage(final String message, final String playerName, final boolean moderator, final boolean administrator) {
		if (alts == null || !isAnAlt(playerName)) {
			return;
		}

		if (message.equalsIgnoreCase("sync")) {
			if (syncWithAlt || spawnMap.isEmpty()) {
				return;
			}

			syncWithAlt = true;
			syncPlayerName = playerName;
			syncDataIterator = new HashMap<>(spawnMap).entrySet().iterator();
		} else {
			final String[] syncData = message.split(",");
			final int serverIndex = Integer.parseInt(syncData[0]);

			if (spawnMap.containsKey(serverIndex)) {
				return;
			}

			final Coordinate coordinate = new Coordinate(Integer.parseInt(syncData[1]), Integer.parseInt(syncData[2]));
			final Spawn spawn = new Spawn(coordinate, Long.parseLong(syncData[3]));

			spawnMap.put(serverIndex, spawn);
			nextSpawn = getOldestSpawn();
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		final int npcId = bot.getNpcId(npc);

		if (npcId != NPC_ID_DRUID) {
			return;
		}

		if (spawnMap.isEmpty() && alts != null) {
			requestSyncWithAlt();
		}

		final int npcX = bot.getMobLocalX(npc) + bot.getAreaX();
		final int npcY = bot.getMobLocalY(npc) + bot.getAreaY();

		if (!Area.DRUIDS.contains(npcX, npcY)) {
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

		nextSpawn = getOldestSpawn();
	}

	private void requestSyncWithAlt() {
		if (System.currentTimeMillis() <= syncRequestTimeout) {
			return;
		}

		for (final String alt : alts) {
			sendPrivateMessage("sync", alt);
		}

		syncRequestTimeout = System.currentTimeMillis() + (TIMEOUT_TEN_SECONDS * 6);
	}

	private enum Area implements RSArea {
		DRUIDS(new Coordinate(350, 451), new Coordinate(375, 472)),
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
