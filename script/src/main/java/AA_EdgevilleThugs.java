import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kills Thugs in Edgeville Dungeon and banks in Edgeville.
 * <p>
 * Required:
 * Start script at Thugs or at Edgeville Bank.
 * <p>
 * Optional Parameter:
 * <controlled|attack|strength|defense> (default strength)
 * <p>
 * Notes:
 * Sleeping bags will be withdrawn from bank, if there are any, after death walkback.
 * <p>
 *
 * @Author Chomp
 */
public class AA_EdgevilleThugs extends AA_Script {
	private static final Coordinate COORDINATE_LUMBRIDGE_DEATH_WALK = new Coordinate(120, 648);
	private static final Coordinate COORDINATE_EDGEVILLE_DEATH_WALK = new Coordinate(215, 450);

	private static final Pattern PATTERN_PROJECTILE_SHOT = Pattern.compile("Warning! (.+) is shooting at you!");

	private static final int[] ITEM_IDS_LOOT = new int[]{
		10, 38, 40, 41, 42, 46,
		157, 158, 159, 160, 165, 170,
		435, 436, 437, 438, 439, 440, 441, 442, 443,
		526, 527, 1092, 1277};
	private static final int[] ITEM_IDS_PREMIUM_LOOT = new int[]{40, 438, 439, 441, 442, 443, 526, 527, 1092, 1277};
	private static final int[] NPC_IDS_BLOCKING = new int[]{23, 40, 46};

	private static final int NPC_ID_THUG = 251;
	private static final int NPC_XP_THUGS = 56;

	private static final int COORDINATE_Y_INSIDE_DUNGEON = 3000;

	private static final int MAXIMUM_DISTANCE_FROM_LOOT = 4;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 2;
	private static final int DISTANCE_FROM_GATE_TO_WILDERNESS = 7;

	private final int[] loot = new int[3];

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();
	private final Map<Integer, Integer> premiumLoot = new TreeMap<>();
	private final Set<String> pkers = new HashSet<>();

	private String attackers = "";
	private Coordinate nextRespawn;
	private PathWalker pathWalker;
	private long startTime;
	private State state;

	private double initialCombatXp;

	private long doorTimeout;

	private int playerX;
	private int playerY;

	private int fleeAt;
	private int deathCount;

	private boolean attackedByPker;
	private boolean died;

	public AA_EdgevilleThugs(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) combatStyle = CombatStyle.valueOf(parameters.toUpperCase());
		setCombatStyle(combatStyle.getIndex());
		state = getInventoryCount() != MAX_INV_SIZE && !isBanking() ? State.KILL : State.BANK;
		fleeAt = (int) (getBaseHits() * 0.80);
		initialCombatXp = getTotalCombatXp();
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

		if (bot.getCombatStyle() != combatStyle.getIndex()) setCombatStyle(combatStyle.getIndex());

		return state == State.KILL ? kill() : bank();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("shut") || message.endsWith("open")) {
			doorTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.startsWith("Warning")) {
			if (attackedByPker || getCurrentHits() > fleeAt) {
				return;
			}

			final Matcher matcher = PATTERN_PROJECTILE_SHOT.matcher(message);

			if (!matcher.matches()) {
				return;
			}

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

		final PathWalker.Path path = pathWalker.calcPath(COORDINATE_LUMBRIDGE_DEATH_WALK.getX(), COORDINATE_LUMBRIDGE_DEATH_WALK.getY(),
			COORDINATE_EDGEVILLE_DEATH_WALK.getX(), COORDINATE_EDGEVILLE_DEATH_WALK.getY());

		if (path != null) {
			pathWalker.setPath(path);

			resetSpawns();
			deathCount++;
			died = true;
			state = State.BANK;

		} else {
			exit(String.format("Failed to calculate path Lumbridge (%d, %d) -> Edgeville (%d,%d).",
				COORDINATE_LUMBRIDGE_DEATH_WALK.getX(), COORDINATE_LUMBRIDGE_DEATH_WALK.getY(),
				COORDINATE_EDGEVILLE_DEATH_WALK.getX(), COORDINATE_EDGEVILLE_DEATH_WALK.getY()));
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
		if (Area.THUGS.contains(playerX, playerY)) {
			if (getInventoryCount() == MAX_INV_SIZE) {
				state = State.BANK;
				return 0;
			}

			if (inCombat()) {
				return 0;
			}

			final int[] thug = getNpcById(NPC_ID_THUG);

			if (thug[0] != -1) {
				attackNpc(thug[0]);
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

		if (!spawnMap.isEmpty()) {
			resetSpawns();
		}

		if (playerY > COORDINATE_Y_INSIDE_DUNGEON) {
			return traverseDungeonToDruids();
		}

		if (Area.LADDER_ROOM.contains(playerX, playerY)) {
			atObject(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (Area.BANK.contains(playerX, playerY)) {
			if (getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
				atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			if (getWallObjectIdFromCoords(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY()) == Object.DOOR.id) {
				if (System.currentTimeMillis() <= doorTimeout) {
					return 0;
				}

				atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
				doorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			walkTo(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (attackedByPker) {
				attackedByPker = false;
			}

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
			if (getWallObjectIdFromCoords(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY()) == Object.DOOR.id) {
				if (System.currentTimeMillis() <= doorTimeout) {
					return 0;
				}

				atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
				doorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			walkTo(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (playerY < COORDINATE_Y_INSIDE_DUNGEON) {
			if (distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
					atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		return traverseDungeonToBank();
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

			if (!Area.THUGS.contains(groundItemX, groundItemY)) {
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

	private int traverseDungeonToDruids() {
		if (!inCombat()) {
			final int blockingNpc = getBlockingNpc(playerX, playerY);

			if (blockingNpc != -1) {
				attackNpc(blockingNpc);
				return SLEEP_ONE_TICK;
			}
		}

		if (playerX < Object.GATE_1.coordinate.getX()) {
			if (getFatigue() != 0 && hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
				return sleep();
			}

			if (distanceTo(Object.GATE_2.coordinate.getX(), Object.GATE_2.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				atObject(Object.GATE_2.coordinate.getX(), Object.GATE_2.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.GATE_2.coordinate.getX(), Object.GATE_2.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			if (getObjectIdFromCoords(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY()) == Object.GATE_1.id) {
				atObject(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.GATE_1.coordinate.getX() - 1, Object.GATE_1.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY());
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

	private int traverseDungeonToBank() {
		if (playerY < Object.GATE_2.coordinate.getY()) {
			if (distanceTo(Object.GATE_2.coordinate.getX(), Object.GATE_2.coordinate.getY()) <= DISTANCE_FROM_GATE_TO_WILDERNESS) {
				atObject(Object.GATE_2.coordinate.getX(), Object.GATE_2.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.GATE_2.coordinate.getX() + 1, Object.GATE_2.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (!inCombat()) {
			final int blockingNpc = getBlockingNpc(playerX, playerY);

			if (blockingNpc != -1) {
				attackNpc(blockingNpc);
				return SLEEP_ONE_TICK;
			}
		}

		if (playerX < Object.GATE_1.coordinate.getX()) {
			if (distanceTo(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (getObjectIdFromCoords(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY()) == Object.GATE_1.id) {
					atObject(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			walkTo(Object.GATE_1.coordinate.getX() - 1, Object.GATE_1.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			atObject(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int getBlockingNpc(final int playerX, final int playerY) {
		final int direction = bot.getMobDirection(bot.getPlayer());

		for (int index = 0; index < bot.getNpcCount(); index++) {
			if (!inArray(NPC_IDS_BLOCKING, getNpcId(index))) {
				continue;
			}

			final int npcX = getNpcX(index);
			final int npcY = getNpcY(index);

			if ((npcY == playerY &&
				((npcX == playerX - 1 && (direction == DIR_EAST || direction == DIR_NORTHEAST)) ||
					(npcX == playerX + 1 && (direction == DIR_WEST || direction == DIR_SOUTHWEST))))) {
				return index;
			}

			if (npcX == playerX &&
				((npcY == playerY - 1 && (direction == DIR_NORTH || direction == DIR_NORTHWEST || direction == DIR_EAST || direction == DIR_NORTHEAST)) ||
					(npcY == playerY + 1 && direction >= DIR_WEST && direction <= DIR_SOUTHEAST))) {
				return index;
			}
		}

		return -1;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Edgeville Thugs", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		final int kills = (int) xpGained / NPC_XP_THUGS;

		drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, toUnitsPerHour(kills, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (nextRespawn != null) {
			drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)",
					nextRespawn.getX(), nextRespawn.getY()),
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
	public void onPlayerDamaged(final java.lang.Object player) {
		if (player != bot.getPlayer() ||
			!inCombat() ||
			getCurrentHits() > fleeAt ||
			attackedByPker) {
			return;
		}

		final String pkerName = getPkerName();

		if (pkerName != null) {
			setAttackedByPker(pkerName);
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (bot.getNpcId(npc) != NPC_ID_THUG) {
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

	private String getPkerName() {
		for (int index = 0; index < bot.getPlayerCount(); index++) {
			final java.lang.Object player = bot.getPlayer(index);

			if (player == bot.getPlayer() ||
				!bot.isMobInCombat(player) ||
				getPlayerX(index) != playerX ||
				getPlayerY(index) != playerY) {
				continue;
			}

			return getPlayerName(index);
		}

		return null;
	}

	private enum State {
		KILL,
		BANK
	}

	private enum Area implements RSArea {
		THUGS(new Coordinate(195, 3242), new Coordinate(209, 3265)),
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
