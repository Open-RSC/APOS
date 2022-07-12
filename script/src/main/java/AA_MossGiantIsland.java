import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Kills Moss Giants on Moss Giant Island at Brimhaven.
 * Buries big bones. Banks at Ardougne South Bank.
 * <p>
 * Required:
 * Start script at Moss Giants or at Ardougne South Bank.
 * Bank: coins, food
 * <p>
 * Optional Parameters:
 * -m,--fightmode <controlled|attack|strength|defense> (default strength)
 * -f,--food <shrimp|...|lobster|shark> (default lobster)
 * -c,--food-count <#> (default 30)
 * -n,--no-bury (default false)
 * <p>
 *
 * @Author Chomp
 */
public class AA_MossGiantIsland extends AA_Script {
	private static final Coordinate
		COORDINATE_LOAD_BRIMHAVEN_DOCKS = new Coordinate(481, 669),
		COORDINATE_LOAD_MOSS_GIANTS = new Coordinate(508, 669);

	private static final String[] MENU_OPTIONS_CUSTOMS_OFFICIAL = new String[]{
		"Can I board this ship?", "Search away I have nothing to hide", "Ok"
	};

	private static final int[]
		ITEM_IDS_LOOT = new int[]{33, 34, 38, 40, 41, 42, 46, 619,
		165, 435, 436, 437, 438, 439, 440, 441, 442, 443,
		10, 413, 526, 527, 1092, 1277},
		ITEM_IDS_LOOT_NOTABLE = new int[]{38, 40, 41, 42, 46, 619, 438, 439, 441, 442, 443, 526, 527, 1092, 1277},
		ITEM_IDS_STACKABLE = new int[]{10, 33, 34, 38, 40, 41, 42, 46, 619};

	private static final int
		NPC_ID_MOSS_GIANT = 594,
		NPC_XP_MOSS_GIANT = 144,
		NPC_ID_CAPTAIN_BARNABY = 316,
		NPC_ID_CUSTOMS_OFFICIAL = 317,
		ITEM_ID_BIG_BONES = 413,
		ITEM_ID_COINS = 10,
		COIN_COUNT = 60,
		MAXIMUM_FATIGUE = 99,
		QUEST_INDEX_DRAGON_SLAYER = 16;

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();
	private final Map<Integer, Integer> notableLoot = new TreeMap<>();

	private final int[] loot = new int[3];

	private Food food = Food.LOBSTER;
	private Coordinate nextRespawn;
	private long startTime;

	private double initialCombatXp, initialPrayerXp;

	private long actionTimeout, optionMenuTimeout;

	private int playerX, playerY, eatAt, foodCount, foodEaten, bonesBuried, bankTrips, menuIndex;

	private boolean banking, burying = true;

	public AA_MossGiantIsland(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-m":
					case "--fightmode":
						combatStyle = CombatStyle.valueOf(args[++i].toUpperCase());
						break;
					case "-f":
					case "--food":
						food = Food.valueOf(args[++i].toUpperCase());
						break;
					case "-c":
					case "--food-count":
						foodCount = Integer.parseInt(args[++i]);
						break;
					case "-n":
					case "--no-bury":
						burying = false;
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		initialCombatXp = getTotalCombatXp();
		initialPrayerXp = getAccurateXpForLevel(Skill.PRAYER.getIndex());
		eatAt = getBaseHits() - food.getHealAmount();
		banking = !hasInventoryItem(food.getId());
		menuIndex = isQuestComplete(QUEST_INDEX_DRAGON_SLAYER) ? 0 : 1;
		setCombatStyle(combatStyle.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		if (bot.getCombatStyle() != combatStyle.getIndex()) setCombatStyle(combatStyle.getIndex());

		return banking ? bank() : kill();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("eat", 4)) {
			foodEaten++;
			actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("bones")) {
			bonesBuried++;
			actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("board") || message.endsWith("30 gold") || message.endsWith("legal")) {
			optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		} else if (message.endsWith("Ardougne")) {
			bankTrips++;
			updateNotableLoot();
		} else {
			super.onServerMessage(message);
		}
	}

	private void updateNotableLoot() {
		for (int index = 0; index < getInventoryCount(); index++) {
			final int itemId = getInventoryId(index);

			if (!inArray(ITEM_IDS_LOOT_NOTABLE, itemId)) {
				continue;
			}

			notableLoot.merge(itemId, getInventoryStack(index), Integer::sum);
		}
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			return useBank();
		}

		if (!isOnBrimhaven()) {
			return enterBank();
		}

		if (Area.BRIMHAVEN_DOCKS.contains(playerX, playerY)) {
			return talkToCustomsOfficial();
		}

		if (Area.MOSS_GIANTS.contains(playerX, playerY)) {
			return leaveMossGiants();
		}

		if (playerX > COORDINATE_LOAD_BRIMHAVEN_DOCKS.getX()) {
			walkTo(COORDINATE_LOAD_BRIMHAVEN_DOCKS.getX(), COORDINATE_LOAD_BRIMHAVEN_DOCKS.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(Area.BRIMHAVEN_DOCKS.upperBoundingCoordinate.getX(),
			Area.BRIMHAVEN_DOCKS.upperBoundingCoordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int kill() {
		if (Area.MOSS_GIANTS.contains(playerX, playerY)) {
			return combat();
		}

		if (isOnBrimhaven()) {
			return walkToMossGiants();
		}

		if (Area.ARDOUGNE_DOCKS.contains(playerX, playerY)) {
			return talkToCaptainBarnaby();
		}

		if (Area.BANK.contains(playerX, playerY) &&
			getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) ==
				Object.BANK_DOORS.id) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(Area.ARDOUGNE_DOCKS.upperBoundingCoordinate.getX(),
			Area.ARDOUGNE_DOCKS.upperBoundingCoordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int useBank() {
		if (getCurrentHits() != getBaseHits()) {
			final int foodIndex = getInventoryIndex(food.getId());

			if (foodIndex != -1) {
				return consume(foodIndex);
			}
		}

		if (!isBanking()) {
			return openBank();
		}

		for (final int itemId : ITEM_IDS_LOOT) {
			if (itemId == ITEM_ID_COINS || !hasInventoryItem(itemId)) {
				continue;
			}

			final int itemCount = getInventoryCount(itemId);

			deposit(itemId, itemCount);
			return SLEEP_ONE_TICK;
		}

		final int coinCount = getInventoryCount(ITEM_ID_COINS);

		if (coinCount < COIN_COUNT) {
			if (!hasBankItem(ITEM_ID_COINS)) {
				return exit("Out of coins.");
			}

			withdraw(ITEM_ID_COINS, COIN_COUNT - coinCount);
			return SLEEP_ONE_TICK;
		}

		if (foodCount == 0) {
			final int emptyCount = MAX_INV_SIZE - getInventoryCount();

			if (emptyCount != 0) {
				final int bankCount = bankCount(food.getId());

				if (bankCount < emptyCount) {
					return exit("Out of food.");
				}

				withdraw(food.getId(), emptyCount);
				return SLEEP_ONE_TICK;
			}
		} else {
			final int foodCount = getInventoryCount(food.getId());

			if (foodCount < this.foodCount) {
				final int bankCount = bankCount(food.getId());

				if (bankCount < this.foodCount - foodCount) {
					return exit("Out of food.");
				}

				withdraw(food.getId(), this.foodCount - foodCount);
				return SLEEP_ONE_TICK;
			}
		}

		banking = false;
		return 0;
	}

	private boolean isOnBrimhaven() {
		return playerY >= Area.BRIMHAVEN_DOCKS.lowerBoundingCoordinate.getY();
	}

	private int enterBank() {
		if (distanceTo(Object.BANK_DOORS.coordinate.getX(),
			Object.BANK_DOORS.coordinate.getY()) <= 1) {
			if (getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) ==
				Object.BANK_DOORS.id) {
				atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int talkToCustomsOfficial() {
		if (isQuestMenu()) {
			int index;

			for (final String menuOption : MENU_OPTIONS_CUSTOMS_OFFICIAL) {
				index = getMenuIndex(menuOption);

				if (index == -1) {
					continue;
				}

				answer(index);
				optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				return 0;
			}

			return 0;
		}

		if (System.currentTimeMillis() <= optionMenuTimeout) {
			return 0;
		}

		final int[] customsOfficial = getNpcByIdNotTalk(NPC_ID_CUSTOMS_OFFICIAL);

		if (customsOfficial[0] == -1) {
			return 0;
		}

		talkToNpc(customsOfficial[0]);
		optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	private int leaveMossGiants() {
		if (inCombat()) {
			walkTo(playerX, playerY);
			return SLEEP_ONE_TICK;
		}

		if (Area.HUT.contains(playerX, playerY) &&
			getWallObjectIdFromCoords(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY()) ==
				Object.DOOR.id) {
			atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		atObject(Object.TREE_WEST.coordinate.getX(), Object.TREE_WEST.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int combat() {
		if (inCombat()) {
			if (getCurrentHits() <= eatAt) {
				walkTo(playerX, playerY);
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		final int foodIndex = getInventoryIndex(food.getId());

		if (foodIndex == -1) {
			banking = true;
			return 0;
		}

		if (getCurrentHits() <= eatAt) {
			return consume(foodIndex);
		}

		if (getFatigue() >= MAXIMUM_FATIGUE) {
			if (!Area.HUT.contains(playerX, playerY) &&
				getWallObjectIdFromCoords(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY()) ==
					Object.DOOR.id) {
				atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			atObject(Object.BED.coordinate.getX(), Object.BED.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (burying) {
			final int bonesIndex = getInventoryIndex(ITEM_ID_BIG_BONES);

			if (bonesIndex != -1) {
				return consume(bonesIndex);
			}
		}

		updateLoot();

		if (loot[0] != -1) {
			if (canPickupLoot()) {
				if (needToOpenDoor(loot[1], loot[2])) {
					atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				pickupItem(loot[0], loot[1], loot[2]);
				return SLEEP_ONE_TICK;
			}

			return consume(foodIndex);
		}

		final int[] mossGiant = getNpcById(NPC_ID_MOSS_GIANT);

		if (mossGiant[0] != -1) {
			if (needToOpenDoor(getNpcX(mossGiant[0]), getNpcY(mossGiant[0]))) {
				atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			attackNpc(mossGiant[0]);
			return SLEEP_ONE_TICK;
		}

		if (nextRespawn != null &&
			(playerX != nextRespawn.getX() || playerY != nextRespawn.getY())) {
			if (needToOpenDoor(nextRespawn.getX(), nextRespawn.getY())) {
				atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(nextRespawn.getX(), nextRespawn.getY());
			return SLEEP_ONE_TICK;
		}

		return 0;
	}

	private int walkToMossGiants() {
		if (playerX >= COORDINATE_LOAD_MOSS_GIANTS.getX()) {
			atObject(Object.TREE_EAST.coordinate.getX(), Object.TREE_EAST.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(COORDINATE_LOAD_MOSS_GIANTS.getX(), COORDINATE_LOAD_MOSS_GIANTS.getY());
		return SLEEP_ONE_TICK;
	}

	private int talkToCaptainBarnaby() {
		if (isQuestMenu()) {
			answer(menuIndex);
			optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= optionMenuTimeout) {
			return 0;
		}

		final int[] captainBarnaby = getNpcByIdNotTalk(NPC_ID_CAPTAIN_BARNABY);

		if (captainBarnaby[0] == -1) {
			return 0;
		}

		talkToNpc(captainBarnaby[0]);
		optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	private int consume(final int inventoryIndex) {
		if (System.currentTimeMillis() <= actionTimeout) {
			return 0;
		}

		useItem(inventoryIndex);
		actionTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
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

			if (!Area.MOSS_GIANTS.contains(groundItemX, groundItemY)) {
				continue;
			}

			final int distance = distanceTo(groundItemX, groundItemY);

			if (distance >= currentDistance) {
				continue;
			}

			currentDistance = distance;

			loot[0] = groundItemId;
			loot[1] = groundItemX;
			loot[2] = groundItemY;
		}
	}

	private boolean canPickupLoot() {
		return getInventoryCount() != MAX_INV_SIZE ||
			(inArray(ITEM_IDS_STACKABLE, loot[0]) && hasInventoryItem(loot[0]));
	}

	private boolean needToOpenDoor(final int x, final int y) {
		if (getWallObjectIdFromCoords(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY()) !=
			Object.DOOR.id) {
			return false;
		}

		return Area.HUT.contains(playerX, playerY) != Area.HUT.contains(x, y);
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Moss Giant Island", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double combatXpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@red@Cmb Xp: @whi@%s @red@(@whi@%s xp@red@/@whi@hr@red@)",
				DECIMAL_FORMAT.format(combatXpGained), toUnitsPerHour((int) combatXpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		final int kills = (int) combatXpGained / NPC_XP_MOSS_GIANT;

		drawString(String.format("@red@Kills: @whi@%d @red@(@whi@%s kills@red@/@whi@hr@red@)",
				kills, toUnitsPerHour(kills, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (bonesBuried > 0) {
			final double prayerXpGained = getAccurateXpForLevel(Skill.PRAYER.getIndex()) - initialPrayerXp;

			drawString(String.format("@cya@Prayer Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
					DECIMAL_FORMAT.format(prayerXpGained), toUnitsPerHour((int) prayerXpGained, startTime)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

			drawString(String.format("@cya@Buried: @whi@%d @cya@(@whi@%s bones@cya@/@whi@hr@cya@)",
					bonesBuried, toUnitsPerHour(kills, startTime)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}

		drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (foodEaten > 0) {
			drawString(String.format("@or1@%s: @whi@%d @or1@(@whi@%s food@or1@/@whi@hr@or1@)",
					food, foodEaten, toUnitsPerHour(kills, startTime)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}

		if (bankTrips > 0) {
			drawString(String.format("@or1@Banked: @whi@%d @or1@(@whi@%s trips@or1@/@whi@hr@or1@)",
					bankTrips, toUnitsPerHour(kills, startTime)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}

		if (!notableLoot.isEmpty()) {
			drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

			for (final Map.Entry<Integer, Integer> entry : notableLoot.entrySet()) {
				drawString(String.format("@gre@%s: @whi@%d", getItemNameId(entry.getKey()), entry.getValue()),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
			}
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (bot.getNpcId(npc) != NPC_ID_MOSS_GIANT) {
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

		nextRespawn = spawnMap.isEmpty() ?
			null :
			spawnMap.values().stream().sorted().findFirst().get().getCoordinate();
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(551, 609), new Coordinate(554, 616)),
		ARDOUGNE_DOCKS(new Coordinate(530, 615), new Coordinate(542, 616)),
		BRIMHAVEN_DOCKS(new Coordinate(467, 646), new Coordinate(467, 656)),
		MOSS_GIANTS(new Coordinate(511, 658), new Coordinate(523, 670)),
		HUT(new Coordinate(518, 663), new Coordinate(521, 666));

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
		BANK_DOORS(64, new Coordinate(550, 612)),
		TREE_EAST(694, new Coordinate(509, 670)),
		TREE_WEST(695, new Coordinate(510, 668)),
		DOOR(2, new Coordinate(520, 663)),
		BED(15, new Coordinate(519, 665));

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
