import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
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
 * -f,--fightmode <controlled|attack|strength|defense> (default strength)
 * <p>
 * Notes:
 * Sleeping bags will be withdrawn from bank, if there are any, after death walkback.
 * <p>
 * Author: Chomp
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

	private static final int SKILL_INDEX_HITS = 3;

	private final int[] loot = new int[3];

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();
	private final Map<Integer, Integer> premiumLoot = new TreeMap<>();
	private final Set<String> pkers = new HashSet<>();

	private String attackers = "";
	private Coordinate nextRespawn;
	private PathWalker pathWalker;
	private Instant startTime;
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
		this.state = this.getInventoryCount() != MAX_INV_SIZE && !this.isBanking() ? State.KILL : State.BANK;
		this.fleeAt = (int) (this.getLevel(SKILL_INDEX_HITS) * 0.80);
		this.initialCombatXp = this.getTotalCombatXp();
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		if (this.died) {
			if (this.isDead()) {
				return 0;
			}

			if (this.pathWalker != null) {
				if (this.pathWalker.walkPath()) {
					return 0;
				}

				this.pathWalker = null;
			}

			this.died = false;
		}

		this.playerX = this.getX();
		this.playerY = this.getY();

		return this.state == State.KILL ? this.kill() : this.bank();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("shut") || message.endsWith("open")) {
			this.doorTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.startsWith("Warning")) {
			if (this.attackedByPker || this.getCurrentLevel(SKILL_INDEX_HITS) > this.fleeAt) {
				return;
			}

			final Matcher matcher = PATTERN_PROJECTILE_SHOT.matcher(message);

			if (!matcher.matches()) {
				return;
			}

			final String rsn = matcher.group(1);

			this.setAttackedByPker(rsn);
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void onPlayerDamaged(final java.lang.Object player) {
		if (player != this.extension.getPlayer() ||
			!this.inCombat() ||
			this.getCurrentLevel(SKILL_INDEX_HITS) > this.fleeAt ||
			this.attackedByPker) {
			return;
		}

		final String pkerName = this.getPkerName();

		if (pkerName != null) {
			this.setAttackedByPker(pkerName);
		}
	}

	@Override
	public void onDeath() {
		if (this.pathWalker == null) {
			this.pathWalker = new PathWalker(this.extension);
			this.pathWalker.init(null);
		}

		final PathWalker.Path path = this.pathWalker.calcPath(COORDINATE_LUMBRIDGE_DEATH_WALK.getX(), COORDINATE_LUMBRIDGE_DEATH_WALK.getY(),
			COORDINATE_EDGEVILLE_DEATH_WALK.getX(), COORDINATE_EDGEVILLE_DEATH_WALK.getY());

		if (path != null) {
			this.pathWalker.setPath(path);

			this.resetSpawns();
			this.deathCount++;
			this.died = true;
			this.state = State.BANK;

		} else {
			this.exit(String.format("Failed to calculate path Lumbridge (%d, %d) -> Edgeville (%d,%d).",
				COORDINATE_LUMBRIDGE_DEATH_WALK.getX(), COORDINATE_LUMBRIDGE_DEATH_WALK.getY(),
				COORDINATE_EDGEVILLE_DEATH_WALK.getX(), COORDINATE_EDGEVILLE_DEATH_WALK.getY()));
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Edgeville Thugs", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

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

		final int kills = (int) xpGained / NPC_XP_THUGS;

		this.drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, getUnitsPerHour(kills, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.nextRespawn != null) {
			this.drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)",
					this.nextRespawn.getX(), this.nextRespawn.getY()),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}

		if (!this.attackers.isEmpty()) {
			this.drawString(String.format("@yel@Attacked by: @whi@%s", this.attackers),
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
		if (this.extension.getNpcId(npc) != NPC_ID_THUG) {
			return;
		}

		final int npcX = this.extension.getMobLocalX(npc) + this.extension.getAreaX();
		final int npcY = this.extension.getMobLocalY(npc) + this.extension.getAreaY();

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
		if (Area.THUGS.contains(this.playerX, this.playerY)) {
			if (this.getInventoryCount() == MAX_INV_SIZE) {
				this.state = State.BANK;
				return 0;
			}

			if (this.inCombat()) {
				return 0;
			}

			final int[] thug = this.getNpcById(NPC_ID_THUG);

			if (thug[0] != -1) {
				this.attackNpc(thug[0]);
				return SLEEP_ONE_TICK;
			}

			this.updateLoot();

			if (this.loot[0] != -1) {
				this.pickupItem(this.loot[0], this.loot[1], this.loot[2]);
				return SLEEP_ONE_TICK;
			}

			if (this.nextRespawn != null &&
				(this.playerX != this.nextRespawn.getX() || this.playerY != this.nextRespawn.getY())) {
				this.walkTo(this.nextRespawn.getX(), this.nextRespawn.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (!this.spawnMap.isEmpty()) {
			this.resetSpawns();
		}

		if (this.playerY > COORDINATE_Y_INSIDE_DUNGEON) {
			return this.traverseDungeonToDruids();
		}

		if (Area.LADDER_ROOM.contains(this.playerX, this.playerY)) {
			this.atObject(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
				this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (this.distanceTo(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			if (this.getWallObjectIdFromCoords(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY()) == Object.DOOR.id) {
				if (System.currentTimeMillis() <= this.doorTimeout) {
					return 0;
				}

				this.atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
				this.doorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			this.walkTo(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int traverseDungeonToDruids() {
		if (!this.inCombat()) {
			final int blockingNpc = this.getBlockingNpc(this.playerX, this.playerY);

			if (blockingNpc != -1) {
				this.attackNpc(blockingNpc);
				return SLEEP_ONE_TICK;
			}
		}

		if (this.playerX < Object.GATE_1.coordinate.getX()) {
			if (this.getFatigue() != 0 && this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
				return this.sleep();
			}

			if (this.distanceTo(Object.GATE_2.coordinate.getX(), Object.GATE_2.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				this.atObject(Object.GATE_2.coordinate.getX(), Object.GATE_2.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.GATE_2.coordinate.getX(), Object.GATE_2.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.distanceTo(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			if (this.getObjectIdFromCoords(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY()) == Object.GATE_1.id) {
				this.atObject(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.GATE_1.coordinate.getX() - 1, Object.GATE_1.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (this.attackedByPker) {
				this.attackedByPker = false;
			}

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

			if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG) && this.hasBankItem(ITEM_ID_SLEEPING_BAG)) {
				this.withdraw(ITEM_ID_SLEEPING_BAG, 1);
				return SLEEP_TWO_SECONDS;
			}

			this.updateBankLoot();
			this.closeBank();
			this.state = State.KILL;
			return SLEEP_ONE_SECOND;
		}

		if (Area.LADDER_ROOM.contains(this.playerX, this.playerY)) {
			if (this.getWallObjectIdFromCoords(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY()) == Object.DOOR.id) {
				if (System.currentTimeMillis() <= this.doorTimeout) {
					return 0;
				}

				this.atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
				this.doorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			this.walkTo(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (this.playerY < COORDINATE_Y_INSIDE_DUNGEON) {
			if (this.distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
					this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		return this.traverseDungeonToBank();
	}

	private int traverseDungeonToBank() {
		if (this.playerY < Object.GATE_2.coordinate.getY()) {
			if (this.distanceTo(Object.GATE_2.coordinate.getX(), Object.GATE_2.coordinate.getY()) <= DISTANCE_FROM_GATE_TO_WILDERNESS) {
				this.atObject(Object.GATE_2.coordinate.getX(), Object.GATE_2.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.GATE_2.coordinate.getX() + 1, Object.GATE_2.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (!this.inCombat()) {
			final int blockingNpc = this.getBlockingNpc(this.playerX, this.playerY);

			if (blockingNpc != -1) {
				this.attackNpc(blockingNpc);
				return SLEEP_ONE_TICK;
			}
		}

		if (this.playerX < Object.GATE_1.coordinate.getX()) {
			if (this.distanceTo(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (this.getObjectIdFromCoords(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY()) == Object.GATE_1.id) {
					this.atObject(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				this.walkTo(Object.GATE_1.coordinate.getX(), Object.GATE_1.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			this.walkTo(Object.GATE_1.coordinate.getX() - 1, Object.GATE_1.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.distanceTo(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			this.atObject(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
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

			if (!Area.THUGS.contains(groundItemX, groundItemY)) {
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

	private int getBlockingNpc(final int playerX, final int playerY) {
		final int direction = this.extension.getMobDirection(this.extension.getPlayer());

		for (int index = 0; index < this.extension.getNpcCount(); index++) {
			if (!inArray(NPC_IDS_BLOCKING, this.getNpcId(index))) {
				continue;
			}

			final int npcX = this.getNpcX(index);
			final int npcY = this.getNpcY(index);

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

	private void setAttackedByPker(final String rsn) {
		System.out.printf("[%s] Attacked by: %s%n", this, rsn);
		if (!this.pkers.contains(rsn)) {
			this.pkers.add(rsn);
			this.attackers += rsn + " ";
		}
		this.attackedByPker = true;
		this.state = State.BANK;
	}

	private String getPkerName() {
		for (int index = 0; index < this.extension.getPlayerCount(); index++) {
			final java.lang.Object player = this.extension.getPlayer(index);

			if (player == this.extension.getPlayer() ||
				!this.extension.isMobInCombat(player) ||
				this.getPlayerX(index) != this.playerX ||
				this.getPlayerY(index) != this.playerY) {
				continue;
			}

			return this.getPlayerName(index);
		}

		return null;
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
			return this.lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return this.upperBoundingCoordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
		}
	}
}
