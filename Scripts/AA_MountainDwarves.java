import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
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
 * Author: Chomp
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
	private Instant startTime;

	private double initialCombatXp;

	private int playerX;
	private int playerY;

	public AA_MountainDwarves(final Extension extension) {
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
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Mountain Dwarves", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

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

		final int kills = (int) xpGained / NPC_XP_MOUNTAIN_DWARF;

		this.drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, getUnitsPerHour(kills, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.nextRespawn != null) {
			this.drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)", this.nextRespawn.getX(), this.nextRespawn.getY()),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}

		if (!this.bankedLoot.isEmpty()) {
			this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			for (final Map.Entry<Integer, Integer> entry : this.bankedLoot.entrySet()) {
				this.drawString(String.format("@gre@%s: @whi@%d", getItemNameId(entry.getKey()), entry.getValue()),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
			}
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (this.extension.getNpcId(npc) != NPC_ID_MOUNTAIN_DWARF) {
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

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
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

		if (this.playerY < COORDINATE_Y_DUNGEON) {
			if (this.distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
					this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.TAVERN.contains(this.playerX, this.playerY)) {
			if (this.inCombat()) {
				this.walkTo(this.playerX, this.playerY);
				return SLEEP_ONE_TICK;
			}

			if (this.getObjectIdFromCoords(Object.TAVERN_DOORS.coordinate.getX(), Object.TAVERN_DOORS.coordinate.getY()) == Object.TAVERN_DOORS.id) {
				this.atObject(Object.TAVERN_DOORS.coordinate.getX(), Object.TAVERN_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.STAIRS_UP.coordinate.getX(), Object.STAIRS_UP.coordinate.getY() + 3);
			return SLEEP_ONE_TICK;
		}

		if (this.distanceTo(Object.STAIRS_UP.coordinate.getX(), Object.STAIRS_UP.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			this.atObject(Object.STAIRS_UP.coordinate.getX(), Object.STAIRS_UP.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.STAIRS_UP.coordinate.getX(), Object.STAIRS_UP.coordinate.getY() + 3);
		return SLEEP_ONE_TICK;
	}

	private int kill() {
		if (this.playerY > COORDINATE_Y_DUNGEON) {
			if (Area.TAVERN.contains(this.playerX, this.playerY)) {
				return this.combatCycle();
			}

			if (this.distanceTo(Object.TAVERN_DOORS.coordinate.getX(), Object.TAVERN_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (this.getObjectIdFromCoords(Object.TAVERN_DOORS.coordinate.getX(), Object.TAVERN_DOORS.coordinate.getY()) == Object.TAVERN_DOORS.id) {
					this.atObject(Object.TAVERN_DOORS.coordinate.getX(), Object.TAVERN_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				if (!this.spawnMap.isEmpty()) {
					this.spawnMap.clear();
					this.nextRespawn = null;
				}

				this.walkTo(Object.TAVERN_DOORS.coordinate.getX() + 1, Object.TAVERN_DOORS.coordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			this.walkTo(Object.TAVERN_DOORS.coordinate.getX() + 1, Object.TAVERN_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
				this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.STAIRS_DOWN.coordinate.getX(), Object.STAIRS_DOWN.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (this.distanceTo(Object.STAIRS_DOWN.coordinate.getX(), Object.STAIRS_DOWN.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			this.atObject(Object.STAIRS_DOWN.coordinate.getX(), Object.STAIRS_DOWN.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.STAIRS_DOWN.coordinate.getX(), Object.STAIRS_DOWN.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
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

		final int[] mountainDwarf = this.getNpcById(NPC_ID_MOUNTAIN_DWARF);

		if (mountainDwarf[0] != -1) {
			if (this.distanceTo(mountainDwarf[1], mountainDwarf[2]) > MAXIMUM_DISTANCE_FROM_NPC) {
				this.walkTo(mountainDwarf[1], mountainDwarf[2]);
				return SLEEP_ONE_TICK;
			}

			this.attackNpc(mountainDwarf[0]);
			return SLEEP_ONE_TICK;
		}

		if (this.nextRespawn != null &&
			(this.playerX != this.nextRespawn.getX() || this.playerY != this.nextRespawn.getY())) {
			this.walkTo(this.nextRespawn.getX(), this.nextRespawn.getY());
			return SLEEP_ONE_TICK;
		}

		return 0;
	}

	private void updateLoot() {
		this.loot[0] = -1;

		for (final int itemId : ITEM_IDS_LOOT) {
			for (int index = 0; index < this.getGroundItemCount(); index++) {
				final int groundItemId = this.getGroundItemId(index);

				if (groundItemId != itemId) {
					continue;
				}

				final int groundItemX = this.getItemX(index);
				final int groundItemY = this.getItemY(index);

				if (!Area.TAVERN.contains(groundItemX, groundItemY)) {
					continue;
				}

				this.loot[0] = groundItemId;
				this.loot[1] = groundItemX;
				this.loot[2] = groundItemY;
				return;
			}
		}
	}

	private void updateBankLoot() {
		for (final int itemId : ITEM_IDS_LOOT) {
			final int bankCount = this.bankCount(itemId);

			if (bankCount == 0) {
				continue;
			}

			this.bankedLoot.put(itemId, bankCount);
		}
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
			return this.lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return this.upperBoundingCoordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
		}
	}
}
