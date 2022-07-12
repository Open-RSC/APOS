import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kills Chaos Druids in Edgeville Dungeon and banks in Edgeville.
 * <p>
 * Required:
 * Start script at Chaos Druids or at Edgeville Bank.
 * <p>
 * Optional Parameters:
 * -a,--alt <altName>
 * -f,--fightmode <controlled|attack|strength|defense> (default strength)
 * <p>
 * Notes:
 * Sleeping bags will be withdrawn from bank, if there are any, after death walkback.
 * Specifying names of alts will enable spawn synchronization between accounts via PM.
 * Alt accounts need to have each other added as friends.
 * Replace any spaces in an rsn with an underscore _.
 * e.g. -f attack -a bot02 -a bot03
 * <p>
 *
 * @Author Chomp
 */
public class AA_EdgevilleChaosDruids extends AA_Script {
	private static final Coordinate COORD_LUMBRIDGE = new Coordinate(120, 648);
	private static final Coordinate COORD_EDGEVILLE = new Coordinate(215, 450);
	private static final Coordinate COORD_CHAOS_DRUIDS = new Coordinate(213, 3255);

	private static final Pattern PATTERN_PROJECTILE_SHOT = Pattern.compile("Warning! (.+) is shooting at you!");

	private static final int[] ITEM_IDS_LOOT = new int[]{
		10, 33, 34, 35, 36, 40, 42,
		157, 158, 159, 160, 165,
		435, 436, 437, 438, 439, 440, 441, 442, 443, 464, 469,
		526, 527, 1092, 1277};
	private static final int[] ITEM_IDS_PREMIUM_LOOT = new int[]{438, 439, 441, 442, 443, 469, 526, 527, 1092, 1277};
	private static final int[] NPC_IDS_BLOCKING = new int[]{23, 40, 46};

	private static final int NPC_ID_CHAOS_DRUID = 270;
	private static final int NPC_XP_CHAOS_DRUID = 58;

	private static final int COORD_Y_DUNGEON = 3000;

	private static final int DIST_GATE_TO_WILD = 7;
	private static final int MAX_DIST_FROM_LOOT = 4;
	private static final int MAX_DIST_FROM_OBJECT = 2;
	private static final int MAX_FATIGUE = 100;

	private final int[] loot = new int[3];

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();
	private final Map<Integer, Integer> premiumLoot = new TreeMap<>();
	private final Set<String> pkers = new HashSet<>();

	private Iterator<Map.Entry<Integer, Spawn>> syncDataIterator;
	private String syncPlayerName;
	private String attackers = "";
	private Spawn nextSpawn;
	private PathWalker pathWalker;
	private State state;

	private double initialCombatXp;

	private long startTime;
	private long syncRequestTimeout;
	private long doorTimeout;

	private int playerX;
	private int playerY;

	private int fleeAt;
	private int deathCount;

	private boolean syncWithAlt;
	private boolean attackedByPker;

	private boolean died;

	private String[] alts;

	public AA_EdgevilleChaosDruids(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			List<String> alts = null;

			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-a":
					case "--alt":
						if (alts == null) alts = new ArrayList<>();
						final String altName = args[++i].replace('_', ' ');
						if (!isFriend(altName)) addFriend(altName);
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

			if (alts != null) this.alts = alts.toArray(new String[0]);
		}

		setCombatStyle(combatStyle.getIndex());
		state = getInventoryCount() != MAX_INV_SIZE && !isBanking() ? State.KILL : State.BANK;
		fleeAt = (int) (getBaseHits() * 0.80);
		initialCombatXp = getTotalCombatXp();
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (died) {
			if (isDead()) return 0;

			if (pathWalker != null) {
				if (pathWalker.walkPath()) return 0;
				pathWalker = null;
			}

			died = false;
		}

		playerX = getX();
		playerY = getY();

		if (bot.getCombatStyle() != combatStyle.getIndex()) setCombatStyle(combatStyle.getIndex());

		return state == State.KILL ? kill() : bank();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("shut") || message.endsWith("open")) {
			doorTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.startsWith("Warning")) {
			if (attackedByPker || getCurrentHits() > fleeAt) return;
			final Matcher matcher = PATTERN_PROJECTILE_SHOT.matcher(message);
			if (!matcher.matches()) return;
			final String rsn = matcher.group(1);
			setAttackedByPker(rsn);
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
			COORD_EDGEVILLE.getX(), COORD_EDGEVILLE.getY());

		if (path != null) {
			pathWalker.setPath(path);
			resetSpawns();
			if (alts != null) resetSync();
			deathCount++;
			died = true;
			state = State.BANK;
		} else {
			exit(String.format("Failed to calculate path Lumbridge (%d, %d) -> Edgeville (%d,%d).",
				COORD_LUMBRIDGE.getX(), COORD_LUMBRIDGE.getY(),
				COORD_EDGEVILLE.getX(), COORD_EDGEVILLE.getY()));
		}
	}

	private void setAttackedByPker(final String rsn) {
		System.out.printf("[%s] Attacked by: %s%n", this, rsn);
		if (!pkers.contains(rsn)) {
			pkers.add(rsn);
			attackers += rsn + " ";
		}
		attackedByPker = true;
		state = State.BANK;
	}

	private int kill() {
		if (Area.CHAOS_DRUIDS.contains(playerX, playerY)) {
			if (getInventoryCount() == MAX_INV_SIZE) {
				state = State.BANK;
				return 0;
			}

			return combatCycle();
		}

		if (Area.THUGS.contains(playerX, playerY)) {
			if (!spawnMap.isEmpty()) {
				resetSpawns();
				if (alts != null) resetSync();
			}

			walkTo(COORD_CHAOS_DRUIDS.getX(), COORD_CHAOS_DRUIDS.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerY > COORD_Y_DUNGEON) return traverseDungeonToDruids();

		if (Area.LADDER_ROOM.contains(playerX, playerY)) {
			final Coordinate ladder = Object.LADDER_DOWN.getCoordinate();
			atObject(ladder.getX(), ladder.getY());
			return SLEEP_ONE_SECOND;
		}

		if (Area.BANK.contains(playerX, playerY)) {
			Coordinate door = Object.BANK_DOORS.getCoordinate();

			if (getObjectIdFromCoords(door.getX(), door.getY()) == Object.BANK_DOORS.id) {
				atObject(door.getX(), door.getY());
				return SLEEP_ONE_SECOND;
			}

			door = Object.DOOR.getCoordinate();
			walkTo(door.getX(), door.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		final Coordinate door = Object.DOOR.getCoordinate();

		if (distanceTo(door.getX(), door.getY()) <= MAX_DIST_FROM_OBJECT) {
			if (getWallObjectIdFromCoords(door.getX(), door.getY()) == Object.DOOR.id) {
				if (System.currentTimeMillis() <= doorTimeout) return 0;
				atWallObject(door.getX(), door.getY());
				doorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			walkTo(door.getX(), door.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(door.getX(), door.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (attackedByPker) attackedByPker = false;
			if (!isBanking()) return openBank();

			for (int index = 0; index < getInventoryCount(); index++) {
				final int itemId = getInventoryId(index);
				if (!inArray(ITEM_IDS_LOOT, itemId)) continue;
				deposit(itemId, getInventoryCount(itemId));
				return SLEEP_ONE_TICK;
			}

			if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG) && hasBankItem(ITEM_ID_SLEEPING_BAG)) {
				withdraw(ITEM_ID_SLEEPING_BAG, 1);
				return SLEEP_TWO_SECONDS;
			}

			updateBankLoot();
			closeBank();
			state = State.KILL;
			return SLEEP_ONE_SECOND;
		}

		if (Area.LADDER_ROOM.contains(playerX, playerY)) {
			final Coordinate door = Object.DOOR.getCoordinate();

			if (getWallObjectIdFromCoords(door.getX(), door.getY()) == Object.DOOR.id) {
				if (System.currentTimeMillis() <= doorTimeout) return 0;
				atWallObject(door.getX(), door.getY());
				doorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			walkTo(door.getX(), door.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (playerY < COORD_Y_DUNGEON) {
			final Coordinate doors = Object.BANK_DOORS.getCoordinate();

			if (distanceTo(doors.getX(), doors.getY()) <= MAX_DIST_FROM_OBJECT) {
				if (getObjectIdFromCoords(doors.getX(), doors.getY()) == Object.BANK_DOORS.id) {
					atObject(doors.getX(), doors.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(doors.getX(), doors.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			walkTo(doors.getX(), doors.getY());
			return SLEEP_ONE_TICK;
		}

		return traverseDungeonToBank();
	}

	private int combatCycle() {
		if (inCombat()) {
			if (syncWithAlt) return syncWithAlt();
			return 0;
		}

		if (getFatigue() >= MAX_FATIGUE && hasInventoryItem(ITEM_ID_SLEEPING_BAG)) return sleep();

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

		if (nextSpawn == null) return 0;

		final Coordinate spawnCoordinate = nextSpawn.getCoordinate();

		if (playerX != spawnCoordinate.getX() || playerY != spawnCoordinate.getY()) {
			if (alts != null && isSpawnCoordinateOccupied(spawnCoordinate)) {
				nextSpawn.setTimestamp(Long.MAX_VALUE);
				nextSpawn = getNextRespawn();
				return 0;
			}

			walkTo(spawnCoordinate.getX(), spawnCoordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (syncWithAlt) return syncWithAlt();
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

	private int traverseDungeonToDruids() {
		if (!inCombat()) {
			final int blockingNpc = getBlockingNpc(playerX, playerY);

			if (blockingNpc != -1) {
				attackNpc(blockingNpc);
				return SLEEP_ONE_TICK;
			}
		}

		Coordinate gate = Object.GATE_1.getCoordinate();

		if (playerX < gate.getX()) {
			if (getFatigue() != 0 && hasInventoryItem(ITEM_ID_SLEEPING_BAG)) return sleep();

			gate = Object.GATE_2.getCoordinate();

			if (distanceTo(gate.getX(), gate.getY()) <= MAX_DIST_FROM_OBJECT) {
				atObject(gate.getX(), gate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(gate.getX(), gate.getY());
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(gate.getX(), gate.getY()) <= MAX_DIST_FROM_OBJECT) {
			if (getObjectIdFromCoords(gate.getX(), gate.getY()) == Object.GATE_1.id) {
				atObject(gate.getX(), gate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(gate.getX() - 1, gate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(gate.getX(), gate.getY());
		return SLEEP_ONE_TICK;
	}

	private void updateBankLoot() {
		for (final int itemId : ITEM_IDS_PREMIUM_LOOT) {
			final int bankCount = bankCount(itemId);
			if (bankCount == 0) continue;
			premiumLoot.put(itemId, bankCount);
		}
	}

	private int traverseDungeonToBank() {
		Coordinate gate = Object.GATE_2.getCoordinate();

		if (playerY < gate.getY()) {
			if (distanceTo(gate.getX(), gate.getY()) <= DIST_GATE_TO_WILD) {
				atObject(gate.getX(), gate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(gate.getX() + 1, gate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (!inCombat()) {
			final int blockingNpc = getBlockingNpc(playerX, playerY);

			if (blockingNpc != -1) {
				attackNpc(blockingNpc);
				return SLEEP_ONE_TICK;
			}
		}

		gate = Object.GATE_1.getCoordinate();

		if (playerX < gate.getX()) {
			if (distanceTo(gate.getX(), gate.getY()) <= MAX_DIST_FROM_OBJECT) {
				if (getObjectIdFromCoords(gate.getX(), gate.getY()) == Object.GATE_1.id) {
					atObject(gate.getX(), gate.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(gate.getX(), gate.getY());
				return SLEEP_ONE_TICK;
			}

			walkTo(gate.getX() - 1, gate.getY());
			return SLEEP_ONE_TICK;
		}

		final Coordinate ladder = Object.LADDER_UP.getCoordinate();

		if (distanceTo(ladder.getX(), ladder.getY()) <= MAX_DIST_FROM_OBJECT) {
			atObject(ladder.getX(), ladder.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(ladder.getX(), ladder.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int syncWithAlt() {
		if (syncDataIterator.hasNext()) {
			final Map.Entry<Integer, Spawn> entry = syncDataIterator.next();

			final int serverIndex = entry.getKey();
			final Spawn spawn = entry.getValue();

			final Coordinate coordinate = spawn.getCoordinate();
			final long timestamp = spawn.getTimestamp();

			sendPrivateMessage(
				String.format("%d,%d,%d,%d", serverIndex, coordinate.getX(), coordinate.getY(), timestamp),
				syncPlayerName
			);

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

			if (!inArray(ITEM_IDS_LOOT, groundItemId)) continue;

			final int groundItemX = getItemX(index);
			final int groundItemY = getItemY(index);

			if (distanceTo(groundItemX, groundItemY) > MAX_DIST_FROM_LOOT ||
				!Area.CHAOS_DRUIDS.contains(groundItemX, groundItemY)) {
				continue;
			}

			final int distance = distanceTo(groundItemX, groundItemY);

			if (distance >= currentDistance) continue;
			currentDistance = distance;

			loot[0] = groundItemId;
			loot[1] = groundItemX;
			loot[2] = groundItemY;
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

	private Spawn getNextRespawn() {
		if (spawnMap.isEmpty()) return null;
		return spawnMap.values().stream().min(Comparator.naturalOrder()).get();
	}

	private int getBlockingNpc(final int playerX, final int playerY) {
		final int direction = bot.getMobDirection(bot.getPlayer());

		for (int index = 0; index < bot.getNpcCount(); index++) {
			if (!inArray(NPC_IDS_BLOCKING, getNpcId(index))) continue;

			final int npcX = getNpcX(index);
			final int npcY = getNpcY(index);

			if ((npcY == playerY &&
				((npcX == playerX - 1 && (direction == DIR_EAST || direction == DIR_NORTHEAST)) ||
					(npcX == playerX + 1 && (direction == DIR_WEST || direction == DIR_SOUTHWEST))))) {
				return index;
			}

			if (npcX == playerX &&
				((npcY == playerY - 1 && (direction == DIR_NORTH || direction == DIR_NORTHWEST ||
					direction == DIR_EAST || direction == DIR_NORTHEAST)) ||
					(npcY == playerY + 1 && direction >= DIR_WEST && direction <= DIR_SOUTHEAST))) {
				return index;
			}
		}

		return -1;
	}

	private boolean isAnAlt(final String playerName) {
		for (final String alt : alts) {
			if (alt.equalsIgnoreCase(playerName)) return true;
		}
		return false;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Edgeville Chaos Druids", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		final int kills = (int) xpGained / NPC_XP_CHAOS_DRUID;

		drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, toUnitsPerHour(kills, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (nextSpawn != null) {
			drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)",
					nextSpawn.getCoordinate().getX(), nextSpawn.getCoordinate().getY()),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}

		if (!attackers.isEmpty()) {
			drawString(String.format("@yel@Attacked by: @whi@%s", attackers),
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
	public void onPrivateMessage(final String message, final String playerName, final boolean moderator,
								 final boolean administrator) {
		if (alts == null || !isAnAlt(playerName)) return;

		if (message.equalsIgnoreCase("sync")) {
			if (syncWithAlt || spawnMap.isEmpty()) return;
			syncWithAlt = true;
			syncPlayerName = playerName;
			syncDataIterator = new HashMap<>(spawnMap).entrySet().iterator();
		} else {
			final String[] syncData = message.split(",");
			final int serverIndex = Integer.parseInt(syncData[0]);
			if (spawnMap.containsKey(serverIndex)) return;
			final Coordinate coordinate = new Coordinate(Integer.parseInt(syncData[1]), Integer.parseInt(syncData[2]));
			final Spawn spawn = new Spawn(coordinate, Long.parseLong(syncData[3]));
			spawnMap.put(serverIndex, spawn);
			nextSpawn = getNextRespawn();
		}
	}

	@Override
	public void onPlayerDamaged(final java.lang.Object player) {
		if (player != bot.getPlayer() ||
			!inCombat() ||
			getCurrentHits() > fleeAt ||
			attackedByPker) {
			return;
		}

		final String pkerName = getPkerName();
		if (pkerName != null) setAttackedByPker(pkerName);
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (bot.getNpcId(npc) != NPC_ID_CHAOS_DRUID || !Area.CHAOS_DRUIDS.contains(playerX, playerY)) return;

		if (spawnMap.isEmpty() && alts != null) requestSyncWithAlt();

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

		nextSpawn = getNextRespawn();
	}

	@Override
	public void onNpcDespawned(final java.lang.Object npc) {
		final int serverIndex = getServerIndex(npc);
		final Spawn spawn = spawnMap.get(serverIndex);
		if (spawn == null) return;
		spawn.setTimestamp(System.currentTimeMillis());
		nextSpawn = getNextRespawn();
	}


	private void requestSyncWithAlt() {
		if (System.currentTimeMillis() <= syncRequestTimeout) return;

		for (final String alt : alts) {
			sendPrivateMessage("sync", alt);
		}

		syncRequestTimeout = System.currentTimeMillis() + (TIMEOUT_TEN_SECONDS * 6);
	}

	private String getPkerName() {
		for (int index = 0; index < bot.getPlayerCount(); index++) {
			final java.lang.Object player = bot.getPlayer(index);

			if (player == bot.getPlayer() ||
				!bot.isMobInCombat(player) ||
				getPlayerX(index) != playerX ||
				getPlayerY(index) != playerY) {
				continue;
			}

			final String playerName = getPlayerName(index);

			for (final String friend : alts) {
				if (friend.equalsIgnoreCase(playerName)) return null;
			}

			return playerName;
		}

		return null;
	}

	private enum State {
		KILL,
		BANK
	}

	private enum Area implements RSArea {
		CHAOS_DRUIDS(new Coordinate(203, 3241), new Coordinate(219, 3257)),
		THUGS(new Coordinate(195, 3241), new Coordinate(202, 3265)),
		BANK(new Coordinate(212, 448), new Coordinate(220, 453)),
		LADDER_ROOM(new Coordinate(214, 465), new Coordinate(218, 469));

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
		GATE_1(57, new Coordinate(211, 3272)),
		GATE_2(305, new Coordinate(196, 3266)),
		BANK_DOORS(64, new Coordinate(217, 447)),
		LADDER_UP(5, new Coordinate(215, 3300)),
		LADDER_DOWN(6, new Coordinate(215, 468)),
		DOOR(2, new Coordinate(218, 465));

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
