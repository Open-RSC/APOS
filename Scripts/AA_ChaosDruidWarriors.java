import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Kills Chaos Druid Warriors in Yanille Dungeon and banks in Yanille.
 * <p>
 * Required:
 * Start script at Chaos Druid Warriors or at Yanille Bank with sleeping bag in inventory.
 * <p>
 * Optional Parameters:
 * -f,--food <none|shrimp|...|sea_turtle|manta_ray> (default none)
 * -c,--food-count <#> (default 2)
 * -a,--alt <altName>
 * -m,--fightmode <controlled|attack|strength|defense> (default strength)
 * <p>
 * Notes:
 * Specifying names of alts will enable spawn synchronization between accounts via PM.
 * Alt accounts need to have each other added as friends.
 * Replace any spaces in an rsn with an underscore _.
 * e.g. -f shark -c 1 -m defense -a bot02 -a bot03
 * <p>
 * Author: Chomp
 */
public class AA_ChaosDruidWarriors extends AA_Script {
	private static final int[] ITEM_IDS_LOOT = new int[]{
		31, 33, 34, 40, 42,
		157, 158, 159, 160, 165, 220,
		435, 436, 437, 438, 439, 440, 441, 442, 443, 464, 469, 471, 473, 497,
		526, 527, 1092, 1277};
	private static final int[] ITEM_IDS_PREMIUM_LOOT = new int[]{220, 438, 439, 441, 442, 443, 469, 471, 473, 526, 527, 1092, 1277};
	private static final int[] ITEM_IDS_STACKABLE = new int[]{31, 33, 34, 40, 42};

	private static final int NPC_ID_CHAOS_DRUID_WARRIOR = 555;
	private static final int NPC_XP_CHAOS_DRUID_WARRIOR = 110;
	private static final int NPC_ID_GIANT_BAT = 43;

	private static final int SKILL_INDEX_HITS = 3;
	private static final int SKILL_INDEX_AGILITY = 16;

	private static final int MINIMUM_HITS = 20;
	private static final int MINIMUM_AGILITY = 65;
	private static final int MAXIMUM_FATIGUE = 99;

	private final int[] loot = new int[3];

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();
	private final Map<Integer, Integer> premiumLoot = new TreeMap<>();

	private Iterator<Map.Entry<Integer, Spawn>> syncDataIterator;
	private String syncPlayerName;
	private Spawn nextSpawn;
	private Instant startTime;
	private Food food = Food.NONE;
	private String[] alts;

	private double initialCombatXp;

	private long syncRequestTimeout;
	private long clickTimeout;

	private int eatAt;
	private int foodEaten;
	private int foodRemaining;
	private int foodWithdrawCount = 2;

	private int playerX;
	private int playerY;

	private boolean syncWithAlt;
	private boolean banking;

	public AA_ChaosDruidWarriors(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (this.getLevel(SKILL_INDEX_AGILITY) < MINIMUM_AGILITY) {
			throw new IllegalStateException(String.format("You must have L%d+ agility to use this script.", MINIMUM_AGILITY));
		}

		if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!parameters.isEmpty()) {
			List<String> alts = null;

			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-f":
					case "--food":
						this.food = Food.valueOf(args[++i].toUpperCase());
						break;
					case "-c":
					case "--food-count":
						this.foodWithdrawCount = Integer.parseInt(args[++i]);
						break;
					case "-a":
					case "--alt":
						if (alts == null) {
							alts = new ArrayList<>();
						}

						final String altName = args[++i].replace('_', ' ');

						if (!this.isFriend(altName)) {
							this.addFriend(altName);
						}

						alts.add(altName);
						break;
					case "-m":
					case "--fightmode":
						this.combatStyle = CombatStyle.valueOf(args[++i].toUpperCase());
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}

			if (alts != null) {
				this.alts = alts.toArray(new String[0]);
			}
		}

		this.setFightMode(this.combatStyle.getIndex());
		this.banking = this.getCurrentLevel(SKILL_INDEX_HITS) <= MINIMUM_HITS ||
			(this.getInventoryCount() == MAX_INV_SIZE && (this.food == Food.NONE || !this.hasInventoryItem(this.food.getId())));
		this.eatAt = this.getLevel(SKILL_INDEX_HITS) - this.food.getHealAmount();
		this.initialCombatXp = this.getTotalCombatXp();
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		this.playerX = this.getX();
		this.playerY = this.getY();

		if (this.getFatigue() >= MAXIMUM_FATIGUE) {
			return this.sleep();
		}

		return this.banking ? this.bank() : this.kill();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("eat", 4)) {
			this.foodEaten++;
			if (this.foodRemaining > 0) {
				this.foodRemaining--;
			}
			this.clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Chaos Druid Warriors", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

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

		final int kills = (int) xpGained / NPC_XP_CHAOS_DRUID_WARRIOR;

		this.drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s kills@cya@/@whi@hr@cya@)",
				kills, getUnitsPerHour(kills, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.food != Food.NONE) {
			this.drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s used@cya@)",
					this.food, this.foodRemaining, this.foodEaten),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}

		if (this.nextSpawn != null) {
			this.drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)",
					this.nextSpawn.getCoordinate().getX(), this.nextSpawn.getCoordinate().getY()),
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
	public void onPrivateMessage(final String message, final String playerName, final boolean moderator, final boolean administrator) {
		if (this.alts == null || !this.isAnAlt(playerName)) {
			return;
		}

		if (message.equalsIgnoreCase("sync")) {
			if (this.syncWithAlt || this.spawnMap.isEmpty()) {
				return;
			}

			this.syncWithAlt = true;
			this.syncPlayerName = playerName;
			this.syncDataIterator = new HashMap<>(this.spawnMap).entrySet().iterator();
		} else {
			final String[] syncData = message.split(",");
			final int serverIndex = Integer.parseInt(syncData[0]);

			if (this.spawnMap.containsKey(serverIndex)) {
				return;
			}

			final Coordinate coordinate = new Coordinate(Integer.parseInt(syncData[1]), Integer.parseInt(syncData[2]));
			final Spawn spawn = new Spawn(coordinate, Long.parseLong(syncData[3]));

			this.spawnMap.put(serverIndex, spawn);
			this.nextSpawn = this.getOldestSpawn();
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (this.extension.getNpcId(npc) != NPC_ID_CHAOS_DRUID_WARRIOR ||
			!Area.CHAOS_DRUID_WARRIORS.contains(this.playerX, this.playerY)) {
			return;
		}

		if (this.spawnMap.isEmpty() && this.alts != null) {
			this.requestSyncWithAlt();
		}

		final int npcX = this.extension.getMobLocalX(npc) + this.extension.getAreaX();
		final int npcY = this.extension.getMobLocalY(npc) + this.extension.getAreaY();

		if (!Area.CHAOS_DRUID_WARRIORS.contains(npcX, npcY)) {
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

		this.nextSpawn = this.getOldestSpawn();
	}

	private int kill() {
		if (Area.CHAOS_DRUID_WARRIORS.contains(this.playerX, this.playerY)) {
			if (this.getCurrentLevel(SKILL_INDEX_HITS) <= MINIMUM_HITS ||
				(this.getInventoryCount() == MAX_INV_SIZE && (this.food == Food.NONE || !this.hasInventoryItem(this.food.getId())))) {
				this.banking = true;
				return 0;
			}

			return this.combatCycle();
		}

		if (Area.DUNGEON_ENTRANCE.contains(this.playerX, this.playerY)) {
			return this.enterLedge();
		}

		return this.enterDungeonEntrance();
	}

	private int combatCycle() {
		if (this.inCombat()) {
			if (this.syncWithAlt) {
				return this.syncWithAlt();
			}

			return 0;
		}

		if (this.getCurrentLevel(SKILL_INDEX_HITS) <= this.eatAt && this.food != Food.NONE) {
			final int foodIndex = this.getInventoryIndex(this.food.getId());

			if (foodIndex != -1) {
				return this.consume(foodIndex);
			}
		}

		this.updateLoot();

		if (this.loot[0] != -1) {
			if (this.getInventoryCount() != MAX_INV_SIZE ||
				(inArray(ITEM_IDS_STACKABLE, this.loot[0]) && this.hasInventoryItem(this.loot[0]))) {
				this.pickupItem(this.loot[0], this.loot[1], this.loot[2]);
				return SLEEP_ONE_TICK;
			}

			return this.consume(this.getInventoryIndex(this.food.getId()));
		}

		final int chaosDruidWarrior = this.getChaosDruidWarrior();

		if (chaosDruidWarrior != -1) {
			this.attackNpc(chaosDruidWarrior);
			return SLEEP_ONE_TICK;
		}

		if (this.nextSpawn == null) {
			return 0;
		}

		final Coordinate spawnCoordinate = this.nextSpawn.getCoordinate();

		if (this.playerX != spawnCoordinate.getX() || this.playerY != spawnCoordinate.getY()) {
			if (this.alts != null && this.isSpawnCoordinateOccupied(spawnCoordinate)) {
				this.nextSpawn.setTimestamp(System.currentTimeMillis());
				this.nextSpawn = this.getOldestSpawn();
				return 0;
			}

			this.walkTo(spawnCoordinate.getX(), spawnCoordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.syncWithAlt) {
			return this.syncWithAlt();
		}

		return 0;
	}

	private int enterLedge() {
		if (this.inCombat()) {
			this.walkTo(Object.LEDGE_ENTER.coordinate.getX(), Object.LEDGE_ENTER.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (this.playerX == Object.LEDGE_ENTER.coordinate.getX() &&
			this.playerY == Object.LEDGE_ENTER.coordinate.getY() - 1) {
			if (System.currentTimeMillis() <= this.clickTimeout) {
				return 0;
			}

			if (!this.spawnMap.isEmpty()) {
				this.resetSpawns();

				if (this.alts != null) {
					this.resetSync();
				}
			}

			this.atObject(Object.LEDGE_ENTER.coordinate.getX(), Object.LEDGE_ENTER.coordinate.getY());
			this.clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		final int giantBat = this.getGiantBat();

		if (giantBat != -1) {
			this.attackNpc(giantBat);
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.LEDGE_ENTER.coordinate.getX(), Object.LEDGE_ENTER.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int enterDungeonEntrance() {
		if (this.distanceTo(Object.STAIRS_DOWN.coordinate.getX(), Object.STAIRS_DOWN.coordinate.getY()) <= 5) {
			this.atObject(Object.STAIRS_DOWN.coordinate.getX(), Object.STAIRS_DOWN.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(Object.STAIRS_DOWN.coordinate.getX(), Object.STAIRS_DOWN.coordinate.getY() + 3);

		if (this.getFatigue() != 0 && this.isWalking()) {
			return this.sleep();
		}

		return SLEEP_ONE_TICK;
	}

	private int consume(final int foodIndex) {
		if (System.currentTimeMillis() <= this.clickTimeout) {
			return 0;
		}

		this.useItem(foodIndex);
		this.clickTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			return this.deposit();
		}

		if (Area.DUNGEON_ENTRANCE.contains(this.playerX, this.playerY)) {
			return this.exitDungeonEntrance();
		}

		if (Area.CHAOS_DRUID_WARRIORS.contains(this.playerX, this.playerY)) {
			return this.exitLedge();
		}

		this.walkTo(Area.BANK.upperBoundingCoordinate.getX(), Area.BANK.upperBoundingCoordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int deposit() {
		if (this.getCurrentLevel(SKILL_INDEX_HITS) <= this.eatAt && this.food != Food.NONE) {
			final int foodIndex = this.getInventoryIndex(this.food.getId());

			if (foodIndex != -1) {
				return this.consume(foodIndex);
			}
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

		final int foodInvCount;

		if (this.food == Food.NONE || (foodInvCount = this.getInventoryCount(this.food.getId())) == this.foodWithdrawCount) {
			this.updateBankLoot();
			this.banking = false;
			return 0;
		}

		if (System.currentTimeMillis() <= this.clickTimeout) {
			return 0;
		}

		this.foodRemaining = this.bankCount(this.food.getId());
		final int foodNeeded = this.foodWithdrawCount - foodInvCount;

		if (this.foodRemaining < foodNeeded) {
			return this.exit("Out of food.");
		}

		this.withdraw(this.food.getId(), foodNeeded);
		this.clickTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private int exitDungeonEntrance() {
		if (this.inCombat()) {
			this.walkTo(Object.STAIRS_UP.coordinate.getX() - 1, Object.STAIRS_UP.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.playerX == Object.STAIRS_UP.coordinate.getX() - 1 &&
			this.playerY == Object.STAIRS_UP.coordinate.getY()) {
			this.atObject(Object.STAIRS_UP.coordinate.getX(), Object.STAIRS_UP.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		final int giantBat = this.getGiantBat();

		if (giantBat != -1) {
			this.attackNpc(giantBat);
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.STAIRS_UP.coordinate.getX() - 1, Object.STAIRS_UP.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int exitLedge() {
		if (this.inCombat()) {
			this.walkTo(Object.LEDGE_EXIT.coordinate.getX(), Object.LEDGE_EXIT.coordinate.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		if (this.playerX == Object.LEDGE_EXIT.coordinate.getX() &&
			this.playerY == Object.LEDGE_EXIT.coordinate.getY() + 1) {
			if (System.currentTimeMillis() <= this.clickTimeout) {
				return 0;
			}

			this.atObject(Object.LEDGE_EXIT.coordinate.getX(), Object.LEDGE_EXIT.coordinate.getY());
			this.clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		this.walkTo(Object.LEDGE_EXIT.coordinate.getX(), Object.LEDGE_EXIT.coordinate.getY() + 1);
		return SLEEP_ONE_TICK;
	}

	private int getChaosDruidWarrior() {
		int nearestChaosDruid = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < this.extension.getNpcCount(); index++) {
			if (this.getNpcId(index) != NPC_ID_CHAOS_DRUID_WARRIOR || this.isNpcInCombat(index)) {
				continue;
			}

			final int npcX = this.getNpcX(index);
			final int npcY = this.getNpcY(index);

			if (!Area.CHAOS_DRUID_WARRIORS.contains(npcX, npcY)) {
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

			if (!Area.CHAOS_DRUID_WARRIORS.contains(groundItemX, groundItemY)) {
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

	private int getGiantBat() {
		for (int index = 0; index < this.extension.getNpcCount(); index++) {
			if (this.getNpcId(index) != NPC_ID_GIANT_BAT) {
				continue;
			}

			final int npcX = this.getNpcX(index);
			final int npcY = this.getNpcY(index);

			if (distanceTo(npcX, npcY, this.playerX, this.playerY) <= 1) {
				return index;
			}
		}

		return -1;
	}

	private Spawn getOldestSpawn() {
		if (this.spawnMap.isEmpty()) {
			return null;
		}

		return this.spawnMap.values().stream().sorted().findFirst().get();
	}

	private void requestSyncWithAlt() {
		if (System.currentTimeMillis() <= this.syncRequestTimeout) {
			return;
		}

		for (final String alt : this.alts) {
			this.sendPrivateMessage("sync", alt);
		}

		this.syncRequestTimeout = System.currentTimeMillis() + (TIMEOUT_TEN_SECONDS * 6);
	}

	private int syncWithAlt() {
		if (this.syncDataIterator.hasNext()) {
			final Map.Entry<Integer, Spawn> entry = this.syncDataIterator.next();

			final int serverIndex = entry.getKey();
			final Spawn spawn = entry.getValue();

			final Coordinate coordinate = spawn.getCoordinate();
			final long timestamp = spawn.getTimestamp();

			this.sendPrivateMessage(String.format("%d,%d,%d,%d", serverIndex, coordinate.getX(), coordinate.getY(), timestamp), this.syncPlayerName);
			return SLEEP_ONE_TICK;
		}

		this.resetSync();
		return 0;
	}

	private boolean isSpawnCoordinateOccupied(final Coordinate spawnCoordinate) {
		for (int index = 0; index < this.extension.getPlayerCount(); index++) {
			final String playerName = this.getPlayerName(index);

			if (this.isAnAlt(playerName) &&
				this.getPlayerX(index) == spawnCoordinate.getX() &&
				this.getPlayerY(index) == spawnCoordinate.getY()) {
				return true;
			}
		}

		return false;
	}

	private boolean isAnAlt(final String playerName) {
		for (final String alt : this.alts) {
			if (alt.equalsIgnoreCase(playerName)) {
				return true;
			}
		}

		return false;
	}

	private void resetSpawns() {
		this.spawnMap.clear();
		this.nextSpawn = null;
	}

	private void resetSync() {
		this.syncWithAlt = false;
		this.syncPlayerName = null;
		this.syncDataIterator = null;
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(585, 750), new Coordinate(590, 758)),
		DUNGEON_ENTRANCE(new Coordinate(600, 3552), new Coordinate(606, 3557)),
		CHAOS_DRUID_WARRIORS(new Coordinate(593, 3563), new Coordinate(608, 3581));

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
		STAIRS_DOWN(44, new Coordinate(603, 722)),
		STAIRS_UP(43, new Coordinate(603, 3554)),
		LEDGE_ENTER(614, new Coordinate(601, 3558)),
		LEDGE_EXIT(615, new Coordinate(601, 3562));

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
