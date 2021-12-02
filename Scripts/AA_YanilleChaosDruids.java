import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Kills Chaos Druids at Yanille Dungeon and banks loot at Yanille Bank.
 * <p>
 * Requirements:
 * Start script at Yanille Bank with sleeping bag and lockpick in inventory.
 * <p>
 * Optional Parameter
 * -f,--fightmode <controlled|attack|strength|defense>
 * <p>
 * Author: Chomp
 */
public class AA_YanilleChaosDruids extends AA_Script {
	private static final Coordinate COORDINATE_BANK = new Coordinate(587, 753);

	private static final int[] ITEM_IDS_LOOT = new int[]{
		31, 33, 34, 40, 42,
		157, 158, 159, 160, 165, 220,
		435, 436, 437, 438, 439, 440, 441, 442, 443, 464, 469, 471, 473, 497,
		526, 527, 1092, 1277};
	private static final int[] ITEM_IDS_PREMIUM_LOOT = new int[]{220, 438, 439, 441, 442, 443, 469, 471, 473, 526, 527, 1092, 1277};
	private static final int[] NPC_IDS_CHAOS_DRUID = new int[]{270, 555};

	private static final int NPC_XP_CHAOS_DRUID = 58;
	private static final int NPC_ID_GIANT_BAT = 43;
	private static final int ITEM_ID_LOCKPICK = 714;
	private static final int SKILL_INDEX_THIEVING = 17;
	private static final int MINIMUM_THIEVING = 82;
	private static final int MAXIMUM_FATIGUE = 99;
	private static final int MAXIMUM_DISTANCE_FROM_LOOT = 2;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 2;
	private static final int COORDINATE_Y_DUNGEON = 3000;

	private final int[] loot = new int[3];

	private final Map<Integer, Integer> premiumLoot = new TreeMap<>();
	private final Map<Integer, Spawn> spawnMap = new HashMap<>();

	private Coordinate nextRespawn;
	private Instant startTime;

	private double initialCombatXp;

	private long doorTimeout;
	private long picklockTimeout;
	private long syncRequestTimeout;

	private int playerX;
	private int playerY;

	public AA_YanilleChaosDruids(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (this.getLevel(SKILL_INDEX_THIEVING) < MINIMUM_THIEVING) {
			throw new IllegalStateException(String.format("You must have L%d+ thieving to use this script.", MINIMUM_THIEVING));
		}

		if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!this.hasInventoryItem(ITEM_ID_LOCKPICK)) {
			throw new IllegalStateException("Lockpick missing from inventory.");
		}

		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-f":
					case "--fightmode":
						this.combatStyle = CombatStyle.valueOf(args[++i].toUpperCase());
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		this.setFightMode(this.combatStyle.getIndex());
		this.initialCombatXp = this.getTotalCombatXp();
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		this.playerX = this.getX();
		this.playerY = this.getY();

		return this.getInventoryCount() == MAX_INV_SIZE || this.isBanking() ? this.bank() : this.kill();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("fail", 4) || message.startsWith("go", 4)) {
			this.picklockTimeout = 0L;
		} else if (message.endsWith("shut") || message.endsWith("open")) {
			this.doorTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Yanille Chaos Druids", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Pid: @whi@%d", this.extension.getMobServerIndex(this.extension.getPlayer())),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final double xpGained = this.getTotalCombatXp() - this.initialCombatXp;

		this.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), getUnitsPerHour(xpGained, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final int kills = (int) xpGained / NPC_XP_CHAOS_DRUID;

		this.drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, getUnitsPerHour(kills, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.nextRespawn != null) {
			this.drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)", this.nextRespawn.getX(), this.nextRespawn.getY()),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}

		if (!this.premiumLoot.isEmpty()) {
			this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			for (final Map.Entry<Integer, Integer> entry : this.premiumLoot.entrySet()) {
				this.drawString(String.format("@gre@%s: @whi@%d", getItemNameId(entry.getKey()), entry.getValue()),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
			}
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		final int npcId = this.extension.getNpcId(npc);

		if (npcId != NPC_IDS_CHAOS_DRUID[0] && npcId != NPC_IDS_CHAOS_DRUID[1]) {
			return;
		}

		final int npcX = this.extension.getMobLocalX(npc) + this.extension.getAreaX();
		final int npcY = this.extension.getMobLocalY(npc) + this.extension.getAreaY();

		if (!Area.CHAOS_DRUIDS.contains(npcX, npcY)) {
			return;
		}

		final int serverIndex = this.extension.getMobServerIndex(npc);
		final Spawn spawn = this.spawnMap.get(serverIndex);

		if (spawn != null) {
			spawn.getCoordinate().set(npcX, npcY);
			spawn.setTimestamp(System.currentTimeMillis());
		} else {
			this.spawnMap.put(serverIndex, new Spawn(new Coordinate(npcX, npcY), System.currentTimeMillis()));
		}

		this.nextRespawn = this.spawnMap.isEmpty() ? null : this.spawnMap.values().stream().sorted().findFirst().get().getCoordinate();
	}

	private int kill() {
		if (Area.CHAOS_DRUIDS.contains(this.playerX, this.playerY)) {
			return this.combatCycle();
		}

		if (this.playerY > COORDINATE_Y_DUNGEON) {
			return this.enterPicklockDoor();
		}

		if (Area.HUT.contains(this.playerX, this.playerY)) {
			this.atObject(Object.STAIRS_DOWN.coordinate.getX(), Object.STAIRS_DOWN.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		return this.enterHut();
	}

	private int combatCycle() {
		if (this.inCombat()) {
			return 0;
		}

		if (this.getFatigue() >= MAXIMUM_FATIGUE) {
			return this.sleep();
		}

		this.updateLoot();

		if (this.loot[0] != -1) {
			this.pickupItem(this.loot[0], this.loot[1], this.loot[2]);
			return SLEEP_ONE_TICK;
		}

		final int druid = this.getNearestChaosDruid();

		if (druid != -1) {
			this.attackNpc(druid);
			return SLEEP_ONE_TICK;
		}

		if (this.nextRespawn != null &&
			(this.playerX != this.nextRespawn.getX() || this.playerY != this.nextRespawn.getY())) {
			this.walkTo(this.nextRespawn.getX(), this.nextRespawn.getY());
			return SLEEP_ONE_TICK;
		}

		return 0;
	}

	private int enterPicklockDoor() {
		if (System.currentTimeMillis() <= this.picklockTimeout) {
			return 0;
		}

		if (this.inCombat()) {
			this.walkTo(Object.PICKLOCK_DOOR.coordinate.getX(), Object.PICKLOCK_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		final int blockingNpc = this.getBlockingNpc(this.playerX, this.playerY);

		if (blockingNpc != -1) {
			this.attackNpc(blockingNpc);
			return SLEEP_ONE_TICK;
		}

		if (this.playerX != Object.PICKLOCK_DOOR.coordinate.getX() || this.playerY != Object.PICKLOCK_DOOR.coordinate.getY()) {
			this.walkTo(Object.PICKLOCK_DOOR.coordinate.getX(), Object.PICKLOCK_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (!this.spawnMap.isEmpty()) {
			this.resetSpawns();
		}

		this.atWallObject2(Object.PICKLOCK_DOOR.coordinate.getX(), Object.PICKLOCK_DOOR.coordinate.getY());
		this.picklockTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int enterHut() {
		if (this.distanceTo(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			if (this.getWallObjectIdFromCoords(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY()) == Object.HUT_DOOR.id) {
				if (System.currentTimeMillis() <= this.doorTimeout) {
					return 0;
				}

				this.atWallObject(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY());
				this.doorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			this.walkTo(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			return this.depositLoot();
		}

		if (Area.HUT.contains(this.playerX, this.playerY)) {
			return this.exitHut();
		}

		if (Area.CHAOS_DRUIDS.contains(this.playerX, this.playerY)) {
			return this.exitPicklockDoor();
		}

		if (this.playerY > COORDINATE_Y_DUNGEON) {
			return this.climbStairs();
		}

		this.walkTo(COORDINATE_BANK.getX(), COORDINATE_BANK.getY());
		return SLEEP_ONE_TICK;
	}

	private int depositLoot() {
		if (!this.isBanking()) {
			return this.openBank();
		}

		for (int index = 0; index < this.getInventoryCount(); index++) {
			final int itemId = this.getInventoryId(index);

			if (!inArray(ITEM_IDS_LOOT, itemId)) {
				continue;
			}

			this.deposit(itemId, this.getInventoryCount(itemId));
			return SLEEP_ONE_TICK;
		}

		this.updateBankLoot();
		this.closeBank();
		return SLEEP_ONE_SECOND;
	}

	private int exitHut() {
		if (this.getWallObjectIdFromCoords(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY()) == Object.HUT_DOOR.id) {
			if (System.currentTimeMillis() <= this.doorTimeout) {
				return 0;
			}

			this.atWallObject(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY());
			this.doorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		this.walkTo(Object.HUT_DOOR.coordinate.getX(), Object.HUT_DOOR.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int exitPicklockDoor() {
		if (this.playerX != Object.PICKLOCK_DOOR.coordinate.getX() || this.playerY != Object.PICKLOCK_DOOR.coordinate.getY() - 1) {
			this.walkTo(Object.PICKLOCK_DOOR.coordinate.getX(), Object.PICKLOCK_DOOR.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= this.picklockTimeout) {
			return 0;
		}

		this.atWallObject2(Object.PICKLOCK_DOOR.coordinate.getX(), Object.PICKLOCK_DOOR.coordinate.getY());
		this.picklockTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int climbStairs() {
		if (this.inCombat()) {
			this.walkTo(Object.STAIRS_UP.coordinate.getX() + 2, Object.STAIRS_UP.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		final int blockingNpc = this.getBlockingNpc(this.playerX, this.playerY);

		if (blockingNpc != -1) {
			this.attackNpc(blockingNpc);
			return SLEEP_ONE_TICK;
		}

		if (this.playerX != Object.STAIRS_UP.coordinate.getX() + 2 || this.playerY != Object.STAIRS_UP.coordinate.getY() - 1) {
			this.walkTo(Object.STAIRS_UP.coordinate.getX() + 2, Object.STAIRS_UP.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		this.atObject(Object.STAIRS_UP.coordinate.getX(), Object.STAIRS_UP.coordinate.getY());
		return SLEEP_ONE_SECOND;
	}

	private int getNearestChaosDruid() {
		int nearestChaosDruid = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < this.extension.getNpcCount(); index++) {
			final int npcId = this.getNpcId(index);

			if (this.isNpcInCombat(index) ||
				(npcId != NPC_IDS_CHAOS_DRUID[0] && npcId != NPC_IDS_CHAOS_DRUID[1])) {
				continue;
			}

			final int npcX = this.getNpcX(index);
			final int npcY = this.getNpcY(index);

			if (!Area.CHAOS_DRUIDS.contains(npcX, npcY)) {
				continue;
			}

			final int distance = distanceTo(npcX, npcY, this.playerX, this.playerY);

			if (distance < currentDistance) {
				nearestChaosDruid = index;
				currentDistance = distance;
			}
		}

		return nearestChaosDruid;
	}

	private int getBlockingNpc(final int playerX, final int playerY) {
		final int direction = this.extension.getMobDirection(this.extension.getPlayer());

		for (int index = 0; index < this.extension.getNpcCount(); index++) {
			if (this.getNpcId(index) != NPC_ID_GIANT_BAT) {
				continue;
			}

			final int npcX = this.getNpcX(index);
			final int npcY = this.getNpcY(index);

			if ((direction == DIR_NORTH || direction == DIR_NORTHEAST) &&
				((npcX == playerX && npcY == playerY - 1) ||
					(npcY == playerY && npcX == playerX - 1))) {
				return index;
			}

			if (direction == DIR_SOUTH && npcX == playerX && npcY == playerY + 1) {
				return index;
			}
		}

		return -1;
	}

	private void updateLoot() {
		this.loot[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < this.getGroundItemCount(); index++) {
			final int groundItemId = this.getGroundItemId(index);

			if (!inArray(ITEM_IDS_LOOT, groundItemId)) {
				continue;
			}

			final int groundItemX = this.getItemX(index);
			final int groundItemY = this.getItemY(index);

			if (this.distanceTo(groundItemX, groundItemY) > MAXIMUM_DISTANCE_FROM_LOOT ||
				!Area.CHAOS_DRUIDS.contains(groundItemX, groundItemY)) {
				continue;
			}

			final int distance = this.distanceTo(groundItemX, groundItemY);

			if (distance < currentDistance) {
				this.loot[0] = groundItemId;
				this.loot[1] = groundItemX;
				this.loot[2] = groundItemY;

				currentDistance = distance;
			}
		}
	}

	private void resetSpawns() {
		this.spawnMap.clear();
		this.nextRespawn = null;
	}

	private void updateBankLoot() {
		for (final int itemId : ITEM_IDS_PREMIUM_LOOT) {
			final int bankCount = this.bankCount(itemId);

			if (bankCount == 0) {
				continue;
			}

			this.premiumLoot.put(itemId, bankCount);
		}
	}

	private enum Area implements RSArea {
		CHAOS_DRUIDS(new Coordinate(576, 3585), new Coordinate(598, 3589)) {
			@Override
			public boolean contains(final int x, final int y) {
				return super.contains(x, y) || (x >= 576 && x <= 594 && y >= 3580 && y <= 3584);
			}
		},
		BANK(new Coordinate(585, 750), new Coordinate(590, 758)),
		HUT(new Coordinate(589, 761), new Coordinate(593, 764));

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

	private enum Object implements RSObject {
		HUT_DOOR(2, new Coordinate(591, 765)),
		PICKLOCK_DOOR(162, new Coordinate(593, 3590)),
		STAIRS_UP(43, new Coordinate(591, 3593)),
		STAIRS_DOWN(42, new Coordinate(591, 761));

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
}
