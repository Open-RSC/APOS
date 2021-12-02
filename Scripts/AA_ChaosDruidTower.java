import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Kills Chaos Druids at the Chaos Druid Tower and banks loot at Ardougne North Bank.
 * <p>
 * Requirements:
 * Start script at the Chaos Druid Tower or Ardougne North Bank with sleeping bag in inventory.
 * <p>
 * Optional Parameter
 * -f,--fightmode <controlled|attack|strength|defense>
 * <p>
 * Author: Chomp
 */
public class AA_ChaosDruidTower extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_BANK = new Coordinate(587, 592);
	private static final Coordinate COORDINATE_LOAD_TOWER = new Coordinate(600, 591);

	private static final int[] ITEM_IDS_LOOT = new int[]{
		10, 33, 34, 35, 36, 40, 42,
		157, 158, 159, 160, 165,
		435, 436, 437, 438, 439, 440, 441, 442, 443, 464, 469,
		526, 527, 1092, 1277};
	private static final int[] ITEM_IDS_PREMIUM_LOOT = new int[]{438, 439, 441, 442, 443, 469, 526, 527, 1092, 1277};

	private static final int NPC_ID_CHAOS_DRUID = 270;
	private static final int NPC_XP_CHAOS_DRUID = 58;

	private static final int SKILL_INDEX_THIEVING = 17;

	private static final int MAXIMUM_FATIGUE = 99;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;
	private static final int MINIMUM_THIEVING = 46;

	private final int[] loot = new int[3];

	private final Map<Integer, Integer> premiumLoot = new TreeMap<>();
	private final Map<Integer, Spawn> spawnMap = new HashMap<>();

	private Coordinate nextRespawn;
	private Instant startTime;

	private double initialCombatXp;

	private long towerDoorTimeout;

	private int playerX;
	private int playerY;

	public AA_ChaosDruidTower(final Extension extension) {
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

		if (this.getInventoryCount() == MAX_INV_SIZE || this.isBanking()) {
			return this.bank();
		}

		return this.kill();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("fail", 4) || message.startsWith("go", 4)) {
			this.towerDoorTimeout = 0L;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Chaos Druid Tower", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

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

		if (this.playerX >= COORDINATE_LOAD_TOWER.getX()) {
			if (this.playerX == Object.TOWER_DOOR.coordinate.getX() && this.playerY == Object.TOWER_DOOR.coordinate.getY()) {
				if (System.currentTimeMillis() <= this.towerDoorTimeout) {
					return 0;
				}

				this.atWallObject2(Object.TOWER_DOOR.coordinate.getX(), Object.TOWER_DOOR.coordinate.getY());
				this.towerDoorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			this.walkTo(Object.TOWER_DOOR.coordinate.getX(), Object.TOWER_DOOR.coordinate.getY());

			if (this.getFatigue() != 0 && this.isWalking()) {
				return this.sleep();
			}

			return SLEEP_ONE_TICK;
		}

		this.walkTo(COORDINATE_LOAD_TOWER.getX(), COORDINATE_LOAD_TOWER.getY());
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

		if (!Area.CHAOS_DRUIDS.contains(this.playerX, this.playerY)) {
			if (this.playerY > COORDINATE_LOAD_BANK.getY()) {
				this.walkTo(COORDINATE_LOAD_BANK.getX(), COORDINATE_LOAD_BANK.getY());
			} else {
				this.walkTo(Area.BANK.upperBoundingCoordinate.getX(), Area.BANK.lowerBoundingCoordinate.getY());
			}
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= this.towerDoorTimeout) {
			return 0;
		}

		this.atWallObject(Object.TOWER_DOOR.coordinate.getX(), Object.TOWER_DOOR.coordinate.getY());
		this.towerDoorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
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
		CHAOS_DRUIDS(new Coordinate(616, 550), new Coordinate(619, 555)) {
			@Override
			public boolean contains(final int x, final int y) {
				return super.contains(x, y) || ((x == 620 || x == 615) && y > 551 && y < 554);
			}
		},
		BANK(new Coordinate(577, 572), new Coordinate(585, 576));

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
		TOWER_DOOR(96, new Coordinate(617, 556));

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
