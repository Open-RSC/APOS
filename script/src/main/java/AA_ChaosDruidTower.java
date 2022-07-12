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
 * <controlled|attack|strength|defense> (default strength)
 * <p>
 *
 * @Author Chomp
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

	private static final int MAXIMUM_FATIGUE = 99;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;
	private static final int MINIMUM_THIEVING = 46;

	private final int[] loot = new int[3];

	private final Map<Integer, Integer> premiumLoot = new TreeMap<>();
	private final Map<Integer, Spawn> spawnMap = new HashMap<>();

	private Coordinate nextRespawn;
	private long startTime;

	private double initialCombatXp;

	private long towerDoorTimeout;

	private int playerX;
	private int playerY;

	public AA_ChaosDruidTower(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (getLevel(Skill.THIEVING.getIndex()) < MINIMUM_THIEVING) {
			throw new IllegalStateException(String.format("You must have L%d+ thieving to use this script.", MINIMUM_THIEVING));
		}

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

		if (getInventoryCount() == MAX_INV_SIZE || isBanking()) {
			return bank();
		}

		return kill();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("fail", 4) || message.startsWith("go", 4)) {
			towerDoorTimeout = 0L;
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

		if (!Area.CHAOS_DRUIDS.contains(playerX, playerY)) {
			if (playerY > COORDINATE_LOAD_BANK.getY()) {
				walkTo(COORDINATE_LOAD_BANK.getX(), COORDINATE_LOAD_BANK.getY());
			} else {
				walkTo(Area.BANK.upperBoundingCoordinate.getX(), Area.BANK.lowerBoundingCoordinate.getY());
			}
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= towerDoorTimeout) {
			return 0;
		}

		atWallObject(Object.TOWER_DOOR.coordinate.getX(), Object.TOWER_DOOR.coordinate.getY());
		towerDoorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int kill() {
		if (Area.CHAOS_DRUIDS.contains(playerX, playerY)) {
			if (inCombat()) {
				return 0;
			}

			if (getFatigue() >= MAXIMUM_FATIGUE) {
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

		if (playerX >= COORDINATE_LOAD_TOWER.getX()) {
			if (playerX == Object.TOWER_DOOR.coordinate.getX() && playerY == Object.TOWER_DOOR.coordinate.getY()) {
				if (System.currentTimeMillis() <= towerDoorTimeout) {
					return 0;
				}

				atWallObject2(Object.TOWER_DOOR.coordinate.getX(), Object.TOWER_DOOR.coordinate.getY());
				towerDoorTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			walkTo(Object.TOWER_DOOR.coordinate.getX(), Object.TOWER_DOOR.coordinate.getY());

			if (getFatigue() != 0 && isWalking()) {
				return sleep();
			}

			return SLEEP_ONE_TICK;
		}

		walkTo(COORDINATE_LOAD_TOWER.getX(), COORDINATE_LOAD_TOWER.getY());
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

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Chaos Druid Tower", PAINT_OFFSET_X, y, 1, 0);

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

		if (premiumLoot.isEmpty()) return;

		drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		for (final Map.Entry<Integer, Integer> entry : premiumLoot.entrySet()) {
			drawString(String.format("@gre@%s: @whi@%d", getItemNameId(entry.getKey()), entry.getValue()),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (bot.getNpcId(npc) != NPC_ID_CHAOS_DRUID) {
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
			return lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return upperBoundingCoordinate;
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
			return id;
		}

		public Coordinate getCoordinate() {
			return coordinate;
		}
	}
}
