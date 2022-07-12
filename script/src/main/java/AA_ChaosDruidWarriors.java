import java.util.*;

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
 *
 * @Author Chomp
 */
public class AA_ChaosDruidWarriors extends AA_Script {
	private static final int[] ITEM_IDS_LOOT = new int[]{
		31, 33, 34, 40, 42,
		157, 158, 159, 160, 165, 220,
		435, 436, 437, 438, 439, 440, 441, 442, 443, 464, 469, 471, 473, 497,
		526, 527, 1092, 1277};
	private static final int[] ITEM_IDS_PREMIUM_LOOT = new int[]{
		220, 438, 439, 441, 442, 443, 469, 471, 473, 526, 527, 1092, 1277};
	private static final int[] ITEM_IDS_STACKABLE = new int[]{31, 33, 34, 40, 42};

	private static final int NPC_ID_CHAOS_DRUID_WARRIOR = 555;
	private static final int NPC_XP_CHAOS_DRUID_WARRIOR = 110;
	private static final int NPC_ID_GIANT_BAT = 43;

	private static final int MINIMUM_HITS = 20;
	private static final int MINIMUM_AGILITY = 65;
	private static final int MAXIMUM_FATIGUE = 99;

	private final int[] loot = new int[3];

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();
	private final Map<Integer, Integer> premiumLoot = new TreeMap<>();

	private Iterator<Map.Entry<Integer, Spawn>> syncDataIterator;
	private String syncPlayerName;
	private Spawn nextSpawn;
	private Food food = Food.NONE;
	private String[] alts;

	private double initialCombatXp;

	private long startTime;
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
		if (getLevel(Skill.AGILITY.getIndex()) < MINIMUM_AGILITY) {
			throw new IllegalStateException(String.format("You must have L%d+ agility to use this script.", MINIMUM_AGILITY));
		}

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!parameters.isEmpty()) {
			List<String> alts = null;

			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-f":
					case "--food":
						food = Food.valueOf(args[++i].toUpperCase());
						break;
					case "-c":
					case "--food-count":
						foodWithdrawCount = Integer.parseInt(args[++i]);
						break;
					case "-a":
					case "--alt":
						if (alts == null) alts = new ArrayList<>();
						final String altName = args[++i].replace('_', ' ');
						if (!isFriend(altName)) addFriend(altName);
						alts.add(altName);
						break;
					case "-m":
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
		banking = getCurrentHits() <= MINIMUM_HITS ||
			(getInventoryCount() == MAX_INV_SIZE && (food == Food.NONE || !hasInventoryItem(food.getId())));
		eatAt = getBaseHits() - food.getHealAmount();
		initialCombatXp = getTotalCombatXp();
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();
		if (bot.getCombatStyle() != combatStyle.getIndex()) setCombatStyle(combatStyle.getIndex());
		if (getFatigue() >= MAXIMUM_FATIGUE) return sleep();
		return banking ? bank() : kill();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("eat", 4)) {
			foodEaten++;
			if (foodRemaining > 0) foodRemaining--;
			clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) return deposit();
		if (Area.DUNGEON_ENTRANCE.contains(playerX, playerY)) return exitDungeonEntrance();
		if (Area.CHAOS_DRUID_WARRIORS.contains(playerX, playerY)) return exitLedge();

		walkTo(Area.BANK.upperBoundingCoordinate.getX(), Area.BANK.upperBoundingCoordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int kill() {
		if (Area.CHAOS_DRUID_WARRIORS.contains(playerX, playerY)) {
			if (getCurrentHits() <= MINIMUM_HITS ||
				(getInventoryCount() == MAX_INV_SIZE && (food == Food.NONE || !hasInventoryItem(food.getId())))) {
				banking = true;
				return 0;
			}

			return combatCycle();
		}

		if (Area.DUNGEON_ENTRANCE.contains(playerX, playerY)) return enterLedge();

		return enterDungeonEntrance();
	}

	private int deposit() {
		if (getCurrentHits() <= eatAt && food != Food.NONE) {
			final int foodIndex = getInventoryIndex(food.getId());
			if (foodIndex != -1) return consume(foodIndex);
		}

		if (!isBanking()) return openBank();

		for (int index = 0; index < getInventoryCount(); index++) {
			final int itemId = getInventoryId(index);
			if (!inArray(ITEM_IDS_LOOT, itemId)) continue;
			deposit(itemId, getInventoryCount(itemId));
			return SLEEP_ONE_TICK;
		}

		final int foodInvCount;

		if (food == Food.NONE || (foodInvCount = getInventoryCount(food.getId())) == foodWithdrawCount) {
			updateBankLoot();
			banking = false;
			return 0;
		}

		if (System.currentTimeMillis() <= clickTimeout) return 0;

		foodRemaining = bankCount(food.getId());
		final int foodNeeded = foodWithdrawCount - foodInvCount;

		if (foodRemaining < foodNeeded) return exit("Out of food.");

		withdraw(food.getId(), foodNeeded);
		clickTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private int exitDungeonEntrance() {
		final Coordinate stairs = Object.STAIRS_UP.getCoordinate();

		if (inCombat()) {
			walkTo(stairs.getX() - 1, stairs.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerX == stairs.getX() - 1 && playerY == stairs.getY()) {
			atObject(stairs.getX(), stairs.getY());
			return SLEEP_ONE_SECOND;
		}

		final int giantBat = getGiantBat();

		if (giantBat != -1) {
			attackNpc(giantBat);
			return SLEEP_ONE_TICK;
		}

		walkTo(stairs.getX() - 1, stairs.getY());
		return SLEEP_ONE_TICK;
	}

	private int exitLedge() {
		final Coordinate ledge = Object.LEDGE_EXIT.getCoordinate();

		if (inCombat()) {
			walkTo(ledge.getX(), ledge.getY() + 1);
			return SLEEP_ONE_TICK;
		}

		if (playerX == ledge.getX() && playerY == ledge.getY() + 1) {
			if (System.currentTimeMillis() <= clickTimeout) return 0;
			atObject(ledge.getX(), ledge.getY());
			clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		walkTo(ledge.getX(), ledge.getY() + 1);
		return SLEEP_ONE_TICK;
	}

	private int combatCycle() {
		if (inCombat()) {
			if (syncWithAlt) return syncWithAlt();
			return 0;
		}

		if (getCurrentHits() <= eatAt && food != Food.NONE) {
			final int foodIndex = getInventoryIndex(food.getId());
			if (foodIndex != -1) return consume(foodIndex);
		}

		updateLoot();

		if (loot[0] != -1) {
			if (getInventoryCount() != MAX_INV_SIZE ||
				(inArray(ITEM_IDS_STACKABLE, loot[0]) && hasInventoryItem(loot[0]))) {
				pickupItem(loot[0], loot[1], loot[2]);
				return SLEEP_ONE_TICK;
			}

			return consume(getInventoryIndex(food.getId()));
		}

		final int chaosDruidWarrior = getChaosDruidWarrior();

		if (chaosDruidWarrior != -1) {
			attackNpc(chaosDruidWarrior);
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

	private int enterLedge() {
		final Coordinate ledge = Object.LEDGE_ENTER.getCoordinate();

		if (inCombat()) {
			walkTo(ledge.getX(), ledge.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (playerX == ledge.getX() && playerY == ledge.getY() - 1) {
			if (System.currentTimeMillis() <= clickTimeout) return 0;

			if (!spawnMap.isEmpty()) {
				resetSpawns();
				if (alts != null) resetSync();
			}

			atObject(ledge.getX(), ledge.getY());
			clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		final int giantBat = getGiantBat();

		if (giantBat != -1) {
			attackNpc(giantBat);
			return SLEEP_ONE_TICK;
		}

		walkTo(ledge.getX(), ledge.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int enterDungeonEntrance() {
		final Coordinate stairs = Object.STAIRS_DOWN.getCoordinate();

		if (distanceTo(stairs.getX(), stairs.getY()) <= 5) {
			atObject(stairs.getX(), stairs.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(stairs.getX(), stairs.getY() + 3);

		if (getFatigue() != 0 && isWalking()) return sleep();

		return SLEEP_ONE_TICK;
	}

	private int consume(final int foodIndex) {
		if (System.currentTimeMillis() <= clickTimeout) return 0;
		useItem(foodIndex);
		clickTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private void updateBankLoot() {
		for (final int itemId : ITEM_IDS_PREMIUM_LOOT) {
			final int bankCount = bankCount(itemId);
			if (bankCount == 0) continue;
			premiumLoot.put(itemId, bankCount);
		}
	}

	private int getGiantBat() {
		for (int index = 0; index < bot.getNpcCount(); index++) {
			if (getNpcId(index) != NPC_ID_GIANT_BAT) continue;
			final int npcX = getNpcX(index);
			final int npcY = getNpcY(index);
			if (distanceTo(npcX, npcY, playerX, playerY) <= 1) return index;
		}

		return -1;
	}

	private int syncWithAlt() {
		if (syncDataIterator.hasNext()) {
			final Map.Entry<Integer, Spawn> entry = syncDataIterator.next();

			final int serverIndex = entry.getKey();
			final Spawn spawn = entry.getValue();

			final Coordinate coordinate = spawn.getCoordinate();
			final long timestamp = spawn.getTimestamp();

			sendPrivateMessage(String.format("%d,%d,%d,%d", serverIndex, coordinate.getX(), coordinate.getY(), timestamp), syncPlayerName);
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

			if (!Area.CHAOS_DRUID_WARRIORS.contains(groundItemX, groundItemY)) continue;

			final int distance = distanceTo(groundItemX, groundItemY);
			if (distance >= currentDistance) continue;
			currentDistance = distance;

			loot[0] = groundItemId;
			loot[1] = groundItemX;
			loot[2] = groundItemY;
		}
	}

	private int getChaosDruidWarrior() {
		int nearestChaosDruid = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < bot.getNpcCount(); index++) {
			if (getNpcId(index) != NPC_ID_CHAOS_DRUID_WARRIOR || isNpcInCombat(index)) continue;

			final int npcX = getNpcX(index);
			final int npcY = getNpcY(index);

			if (!Area.CHAOS_DRUID_WARRIORS.contains(npcX, npcY)) continue;

			final int distance = distanceTo(npcX, npcY, playerX, playerY);

			if (distance >= currentDistance) continue;

			nearestChaosDruid = index;
			currentDistance = distance;
		}

		return nearestChaosDruid;
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

	private void resetSpawns() {
		spawnMap.clear();
		nextSpawn = null;
	}

	private void resetSync() {
		syncWithAlt = false;
		syncPlayerName = null;
		syncDataIterator = null;
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

		drawString("@yel@Chaos Druid Warriors", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		final int kills = (int) xpGained / NPC_XP_CHAOS_DRUID_WARRIOR;

		drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s kills@cya@/@whi@hr@cya@)",
				kills, toUnitsPerHour(kills, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (food != Food.NONE) {
			drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s used@cya@)",
					food, foodRemaining, foodEaten),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}

		if (nextSpawn != null) {
			drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)",
					nextSpawn.getCoordinate().getX(), nextSpawn.getCoordinate().getY()),
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
	public void onPrivateMessage(final String message, final String playerName, final boolean moderator, final boolean administrator) {
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
	public void onNpcSpawned(final java.lang.Object npc) {
		if (bot.getNpcId(npc) != NPC_ID_CHAOS_DRUID_WARRIOR || !Area.CHAOS_DRUID_WARRIORS.contains(playerX, playerY)) {
			return;
		}

		if (spawnMap.isEmpty() && alts != null) requestSyncWithAlt();

		final int npcX = getX(npc);
		final int npcY = getY(npc);

		if (!Area.CHAOS_DRUID_WARRIORS.contains(npcX, npcY)) return;

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

	private Spawn getNextRespawn() {
		if (spawnMap.isEmpty()) return null;
		return spawnMap.values().stream().min(Comparator.naturalOrder()).get();
	}

	private void requestSyncWithAlt() {
		if (System.currentTimeMillis() <= syncRequestTimeout) return;

		for (final String alt : alts) {
			sendPrivateMessage("sync", alt);
		}

		syncRequestTimeout = System.currentTimeMillis() + (TIMEOUT_TEN_SECONDS * 6);
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
			return lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return upperBoundingCoordinate;
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
			return id;
		}

		public Coordinate getCoordinate() {
			return coordinate;
		}
	}
}
