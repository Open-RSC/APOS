import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
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
 * Author: Chomp
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
	private Instant startTime;

	private double initialCombatXp;

	private long openGateTimeout;

	private int playerX;
	private int playerY;

	public AA_TaverleyChaosDruids(final Extension extension) {
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
	public void onServerMessage(final String message) {
		if (message.endsWith("gate")) {
			this.openGateTimeout = 0L;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Taverley Chaos Druids", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

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

		this.drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s kills@cya@/@whi@hr@cya@)",
				kills, getUnitsPerHour(kills, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.nextRespawn != null) {
			this.drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)",
					this.nextRespawn.getX(), this.nextRespawn.getY()),
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
		if (this.extension.getNpcId(npc) != NPC_ID_CHAOS_DRUID) {
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
		if (Area.CHAOS_DRUIDS.contains(this.playerX, this.playerY)) {
			if (this.inCombat()) {
				return 0;
			}

			if (this.getFatigue() >= MAXIMUM_FATIGUE) {
				return this.sleep();
			}

			final int[] druid = this.getNpcById(NPC_ID_CHAOS_DRUID);

			if (druid[0] != -1) {
				this.attackNpc(druid[0]);
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

		if (this.playerY > COORDINATE_Y_DUNGEON) {
			if (!this.inCombat()) {
				final int blockingNpc = this.getBlockingNpc();

				if (blockingNpc != -1) {
					this.attackNpc(blockingNpc);
					return SLEEP_ONE_TICK;
				}
			}

			if (!this.spawnMap.isEmpty()) {
				this.resetSpawns();
			}

			this.walkTo(COORDINATE_CHAOS_DRUIDS.getX(), COORDINATE_CHAOS_DRUIDS.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.playerX > Object.MEMBERS_GATE.coordinate.getX()) {
			if (this.distanceTo(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				this.atObject(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.LADDER_DOWN.coordinate.getX(), Object.LADDER_DOWN.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		if (this.distanceTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			if (System.currentTimeMillis() <= this.openGateTimeout) {
				return 0;
			}

			this.atObject(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
			this.openGateTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
			return 0;
		}

		if (this.playerY <= COORDINATE_LOAD_MEMBERS_GATE.getY()) {
			this.walkTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BANK.contains(this.playerX, this.playerY) &&
			this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(COORDINATE_LOAD_MEMBERS_GATE.getX(), COORDINATE_LOAD_MEMBERS_GATE.getY());
		return SLEEP_ONE_TICK;
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
			if (this.playerX <= Object.MEMBERS_GATE.coordinate.getX()) {
				if (this.playerY >= COORDINATE_LOAD_FALADOR.getY()) {
					if (this.distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
						if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
							this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
							return SLEEP_ONE_SECOND;
						}

						this.walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
						return SLEEP_ONE_TICK;
					}

					this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_TICK;
				}

				this.walkTo(COORDINATE_LOAD_FALADOR.getX(), COORDINATE_LOAD_FALADOR.getY());
				return SLEEP_ONE_TICK;
			}

			if (this.distanceTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (System.currentTimeMillis() <= this.openGateTimeout) {
					return 0;
				}

				this.atObject(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
				this.openGateTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
				return 0;
			}

			this.walkTo(Object.MEMBERS_GATE.coordinate.getX() + 1, Object.MEMBERS_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (!this.inCombat()) {
			final int blockingNpc = this.getBlockingNpc();

			if (blockingNpc != -1) {
				this.attackNpc(blockingNpc);
				return SLEEP_ONE_TICK;
			}
		}

		if (this.playerY >= COORDINATE_LOAD_LADDER.getY()) {
			this.atObject(Object.LADDER_UP.coordinate.getX(), Object.LADDER_UP.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(COORDINATE_LOAD_LADDER.getX(), COORDINATE_LOAD_LADDER.getY());
		return SLEEP_ONE_TICK;
	}

	private int getBlockingNpc() {
		final int direction = this.extension.getMobDirection(this.extension.getPlayer());

		for (int index = 0; index < this.extension.getNpcCount(); index++) {
			if (!inArray(NPC_IDS_BLOCKING, this.getNpcId(index))) {
				continue;
			}

			final int npcX = this.getNpcX(index);
			final int npcY = this.getNpcY(index);

			if (npcY == this.playerY &&
				((npcX == this.playerX - 1 && direction == DIR_EAST) || (npcX == this.playerX + 1 && direction == DIR_WEST))) {
				return index;
			}

			if (npcX == this.playerX &&
				((npcY == this.playerY - 1 && (direction == DIR_NORTH || direction == DIR_NORTHEAST)) || (npcY == this.playerY + 1 && (direction == DIR_SOUTH || direction == DIR_SOUTHWEST)))) {
				return index;
			}
		}

		return -1;
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

			if (!Area.CHAOS_DRUIDS.contains(groundItemX, groundItemY)) {
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
			return this.lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return this.upperBoundingCoordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
		}
	}
}
