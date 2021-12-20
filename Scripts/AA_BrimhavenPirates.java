import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
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
 * Author: Chomp
 */
public class AA_BrimhavenPirates extends AA_Script {
	private static final int[] ITEM_IDS_LOOT = new int[]{1277, 1092, 526, 527, 42, 40, 41, 31, 33, 34};
	private static final int[] ITEM_IDS_STACKABLE = new int[]{31, 33, 34, 40, 41, 42};

	private static final int NPC_ID_PIRATE = 264;
	private static final int NPC_XP_PIRATE = 82;

	private static final int MAXIMUM_FATIGUE = 99;
	private static final int MAXIMUM_DISTANCE_FROM_NPC = 3;

	private static final int SKILL_INDEX_ATTACK = 0;
	private static final int SKILL_INDEX_DEFENCE = 1;
	private static final int SKILL_INDEX_STRENGTH = 2;

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();

	private final int[] pirate = new int[3];
	private final int[] loot = new int[3];

	private Coordinate nextRespawn;
	private Instant startTime;

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
		if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-f":
					case "--fightmode":
						this.combatStyle = CombatStyle.valueOf(args[++i].toUpperCase());
						break;
					case "-t":
					case "--tavern":
						this.tavernOnly = true;
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

		if (this.inCombat()) {
			return 0;
		}

		if (this.getFatigue() >= MAXIMUM_FATIGUE) {
			return this.sleep();
		}

		this.updateLoot();

		if (this.loot[0] != -1 && (this.getInventoryCount() != MAX_INV_SIZE ||
			(inArray(ITEM_IDS_STACKABLE, this.loot[0]) && this.hasInventoryItem(this.loot[0])))) {
			if (this.needToOpenDoor(this.loot[1], this.loot[2])) {
				return this.useDoor(true);
			}

			this.pickupItem(this.loot[0], this.loot[1], this.loot[2]);
			return SLEEP_ONE_TICK;
		}

		this.updatePirate();

		if (this.pirate[0] != -1) {
			if (this.needToOpenDoor(this.pirate[1], this.pirate[2])) {
				return this.useDoor(true);
			}

			if (this.distanceTo(this.pirate[1], this.pirate[2]) > MAXIMUM_DISTANCE_FROM_NPC) {
				this.walkTo(this.pirate[1], this.pirate[2]);
			} else {
				this.attackNpc(this.pirate[0]);
			}

			return SLEEP_ONE_TICK;
		}

		if (!Area.TAVERN.contains(this.playerX, this.playerY)) {
			if (this.isDoorClosed()) {
				return this.useDoor(true);
			}

			this.walkTo(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (!this.isDoorClosed()) {
			return this.useDoor(false);
		}

		if (this.nextRespawn != null &&
			(this.playerX != this.nextRespawn.getX() || this.playerY != this.nextRespawn.getY())) {
			this.walkTo(this.nextRespawn.getX(), this.nextRespawn.getY());
			return SLEEP_ONE_TICK;
		}

		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("shut") || message.endsWith("open")) {
			this.doorTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Brimhaven Pirates", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

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

		final int kills = (int) xpGained / NPC_XP_PIRATE;

		this.drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, getUnitsPerHour(kills, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.nextRespawn != null) {
			this.drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)", this.nextRespawn.getX(), this.nextRespawn.getY()),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (this.extension.getNpcId(npc) != NPC_ID_PIRATE) {
			return;
		}

		final int npcX = this.extension.getMobLocalX(npc) + this.extension.getAreaX();
		final int npcY = this.extension.getMobLocalY(npc) + this.extension.getAreaY();

		if (!Area.TAVERN.contains(npcX, npcY)) {
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

	private boolean isDoorClosed() {
		return this.getWallObjectIdFromCoords(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY()) == Object.DOOR.id;
	}

	private boolean needToOpenDoor(final int x, final int y) {
		if (!this.isDoorClosed()) {
			return false;
		}

		return Area.TAVERN.contains(this.playerX, this.playerY) != Area.TAVERN.contains(x, y);
	}

	private int useDoor(final boolean open) {
		if (System.currentTimeMillis() <= this.doorTimeout) {
			return 0;
		}

		if (open) {
			this.atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
		} else {
			this.atWallObject2(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
		}

		this.doorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private void updatePirate() {
		this.pirate[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < this.extension.getNpcCount(); index++) {
			if (this.getNpcId(index) != NPC_ID_PIRATE || this.isNpcInCombat(index)) {
				continue;
			}

			final int npcX = this.getNpcX(index);
			final int npcY = this.getNpcY(index);

			if ((this.tavernOnly && !Area.TAVERN.contains(npcX, npcY)) || !Area.ISLAND.contains(npcX, npcY)) {
				continue;
			}

			final int distance = distanceTo(npcX, npcY, this.playerX, this.playerY);

			if (distance < currentDistance) {
				this.pirate[0] = index;
				this.pirate[1] = npcX;
				this.pirate[2] = npcY;
				currentDistance = distance;
			}
		}
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

			if ((this.tavernOnly && !Area.TAVERN.contains(groundItemX, groundItemY)) ||
				!Area.ISLAND.contains(groundItemX, groundItemY)) {
				continue;
			}

			final int distance = distanceTo(groundItemX, groundItemY, this.playerX, this.playerY);

			if (distance < currentDistance) {
				this.loot[0] = groundItemId;
				this.loot[1] = groundItemX;
				this.loot[2] = groundItemY;
				currentDistance = distance;
			}
		}
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
			return this.lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return this.upperBoundingCoordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
		}
	}
}
