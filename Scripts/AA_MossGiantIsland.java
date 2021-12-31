import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
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
 * Author: Chomp
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
		SKILL_INDEX_PRAYER = 5,
		SKILL_INDEX_HITS = 3,
		QUEST_INDEX_DRAGON_SLAYER = 16;

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();
	private final Map<Integer, Integer> notableLoot = new TreeMap<>();

	private final int[] loot = new int[3];

	private Food food = Food.LOBSTER;
	private Coordinate nextRespawn;
	private Instant startTime;

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
						this.combatStyle = CombatStyle.valueOf(args[++i].toUpperCase());
						break;
					case "-f":
					case "--food":
						this.food = Food.valueOf(args[++i].toUpperCase());
						break;
					case "-c":
					case "--food-count":
						this.foodCount = Integer.parseInt(args[++i]);
						break;
					case "-n":
					case "--no-bury":
						this.burying = false;
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		this.initialCombatXp = this.getTotalCombatXp();
		this.initialPrayerXp = this.getAccurateXpForLevel(SKILL_INDEX_PRAYER);
		this.eatAt = this.getLevel(SKILL_INDEX_HITS) - this.food.getHealAmount();
		this.banking = !this.hasInventoryItem(this.food.getId());
		this.menuIndex = this.isQuestComplete(QUEST_INDEX_DRAGON_SLAYER) ? 0 : 1;
		this.setFightMode(this.combatStyle.getIndex());
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		this.playerX = this.getX();
		this.playerY = this.getY();

		return this.banking ? this.bank() : this.kill();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("eat", 4)) {
			this.foodEaten++;
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("bones")) {
			this.bonesBuried++;
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("board") || message.endsWith("30 gold") || message.endsWith("legal")) {
			this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		} else if (message.endsWith("Ardougne")) {
			this.bankTrips++;
			this.updateNotableLoot();
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Moss Giant Island", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Pid: @whi@%d",
				this.extension.getMobServerIndex(this.extension.getPlayer())),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final double combatXpGained = this.getTotalCombatXp() - this.initialCombatXp;

		this.drawString(String.format("@red@Cmb Xp: @whi@%s @red@(@whi@%s xp@red@/@whi@hr@red@)",
				DECIMAL_FORMAT.format(combatXpGained), getUnitsPerHour(combatXpGained, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final int kills = (int) combatXpGained / NPC_XP_MOSS_GIANT;

		this.drawString(String.format("@red@Kills: @whi@%d @red@(@whi@%s kills@red@/@whi@hr@red@)",
				kills, getUnitsPerHour(kills, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.bonesBuried > 0) {
			this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			final double prayerXpGained = this.getAccurateXpForLevel(SKILL_INDEX_PRAYER) - this.initialPrayerXp;

			this.drawString(String.format("@cya@Prayer Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
					DECIMAL_FORMAT.format(prayerXpGained), getUnitsPerHour(prayerXpGained, secondsElapsed)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			this.drawString(String.format("@cya@Buried: @whi@%d @cya@(@whi@%s bones@cya@/@whi@hr@cya@)",
					this.bonesBuried, getUnitsPerHour(kills, secondsElapsed)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.foodEaten > 0) {
			this.drawString(String.format("@or1@%s: @whi@%d @or1@(@whi@%s food@or1@/@whi@hr@or1@)",
					this.food, this.foodEaten, getUnitsPerHour(kills, secondsElapsed)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}

		if (this.bankTrips > 0) {
			this.drawString(String.format("@or1@Banked: @whi@%d @or1@(@whi@%s trips@or1@/@whi@hr@or1@)",
					this.bankTrips, getUnitsPerHour(kills, secondsElapsed)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}

		if (!this.notableLoot.isEmpty()) {
			this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			for (final Map.Entry<Integer, Integer> entry : this.notableLoot.entrySet()) {
				this.drawString(String.format("@gre@%s: @whi@%d", getItemNameId(entry.getKey()), entry.getValue()),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
			}
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (this.extension.getNpcId(npc) != NPC_ID_MOSS_GIANT) {
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

		this.nextRespawn = this.spawnMap.isEmpty() ?
			null :
			this.spawnMap.values().stream().sorted().findFirst().get().getCoordinate();
	}

	private int kill() {
		if (Area.MOSS_GIANTS.contains(this.playerX, this.playerY)) {
			return this.combat();
		}

		if (this.isOnBrimhaven()) {
			return this.walkToMossGiants();
		}

		if (Area.ARDOUGNE_DOCKS.contains(this.playerX, this.playerY)) {
			return this.talkToCaptainBarnaby();
		}

		if (Area.BANK.contains(this.playerX, this.playerY) &&
			this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) ==
				Object.BANK_DOORS.id) {
			this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(Area.ARDOUGNE_DOCKS.upperBoundingCoordinate.getX(),
			Area.ARDOUGNE_DOCKS.upperBoundingCoordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int combat() {
		if (this.inCombat()) {
			if (this.getFightMode() != this.combatStyle.getIndex()) {
				this.setFightMode(this.combatStyle.getIndex());
				return SLEEP_ONE_TICK;
			}

			if (this.getCurrentLevel(SKILL_INDEX_HITS) <= this.eatAt) {
				this.walkTo(this.playerX, this.playerY);
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		final int foodIndex = this.getInventoryIndex(this.food.getId());

		if (foodIndex == -1) {
			this.banking = true;
			return 0;
		}

		if (this.getCurrentLevel(SKILL_INDEX_HITS) <= this.eatAt) {
			return this.consume(foodIndex);
		}

		if (this.getFatigue() >= MAXIMUM_FATIGUE) {
			if (!Area.HUT.contains(this.playerX, this.playerY) &&
				this.getWallObjectIdFromCoords(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY()) ==
					Object.DOOR.id) {
				this.atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.atObject(Object.BED.coordinate.getX(), Object.BED.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (this.burying) {
			final int bonesIndex = this.getInventoryIndex(ITEM_ID_BIG_BONES);

			if (bonesIndex != -1) {
				return this.consume(bonesIndex);
			}
		}

		this.updateLoot();

		if (this.loot[0] != -1) {
			if (this.canPickupLoot()) {
				if (this.needToOpenDoor(this.loot[1], this.loot[2])) {
					this.atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				this.pickupItem(this.loot[0], this.loot[1], this.loot[2]);
				return SLEEP_ONE_TICK;
			}

			return this.consume(foodIndex);
		}

		final int[] mossGiant = this.getNpcById(NPC_ID_MOSS_GIANT);

		if (mossGiant[0] != -1) {
			if (this.needToOpenDoor(this.getNpcX(mossGiant[0]), this.getNpcY(mossGiant[0]))) {
				this.atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.attackNpc(mossGiant[0]);
			return SLEEP_ONE_TICK;
		}

		if (this.nextRespawn != null &&
			(this.playerX != this.nextRespawn.getX() || this.playerY != this.nextRespawn.getY())) {
			if (this.needToOpenDoor(this.nextRespawn.getX(), this.nextRespawn.getY())) {
				this.atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(this.nextRespawn.getX(), this.nextRespawn.getY());
			return SLEEP_ONE_TICK;
		}

		return 0;
	}

	private int walkToMossGiants() {
		if (this.playerX >= COORDINATE_LOAD_MOSS_GIANTS.getX()) {
			this.atObject(Object.TREE_EAST.coordinate.getX(), Object.TREE_EAST.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(COORDINATE_LOAD_MOSS_GIANTS.getX(), COORDINATE_LOAD_MOSS_GIANTS.getY());
		return SLEEP_ONE_TICK;
	}

	private int talkToCaptainBarnaby() {
		if (this.isQuestMenu()) {
			this.answer(this.menuIndex);
			this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= this.optionMenuTimeout) {
			return 0;
		}

		final int[] captainBarnaby = this.getNpcByIdNotTalk(NPC_ID_CAPTAIN_BARNABY);

		if (captainBarnaby[0] == -1) {
			return 0;
		}

		this.talkToNpc(captainBarnaby[0]);
		this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			return this.useBank();
		}

		if (!this.isOnBrimhaven()) {
			return this.enterBank();
		}

		if (Area.BRIMHAVEN_DOCKS.contains(this.playerX, this.playerY)) {
			return this.talkToCustomsOfficial();
		}

		if (Area.MOSS_GIANTS.contains(this.playerX, this.playerY)) {
			return this.leaveMossGiants();
		}

		if (this.playerX > COORDINATE_LOAD_BRIMHAVEN_DOCKS.getX()) {
			this.walkTo(COORDINATE_LOAD_BRIMHAVEN_DOCKS.getX(), COORDINATE_LOAD_BRIMHAVEN_DOCKS.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Area.BRIMHAVEN_DOCKS.upperBoundingCoordinate.getX(),
			Area.BRIMHAVEN_DOCKS.upperBoundingCoordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int useBank() {
		if (this.getCurrentLevel(SKILL_INDEX_HITS) != this.getLevel(SKILL_INDEX_HITS)) {
			final int foodIndex = this.getInventoryIndex(this.food.getId());

			if (foodIndex != -1) {
				return this.consume(foodIndex);
			}
		}

		if (!this.isBanking()) {
			return this.openBank();
		}

		for (final int itemId : ITEM_IDS_LOOT) {
			if (itemId == ITEM_ID_COINS || !this.hasInventoryItem(itemId)) {
				continue;
			}

			final int itemCount = this.getInventoryCount(itemId);

			this.deposit(itemId, itemCount);
			return SLEEP_ONE_TICK;
		}

		final int coinCount = this.getInventoryCount(ITEM_ID_COINS);

		if (coinCount < COIN_COUNT) {
			if (!this.hasBankItem(ITEM_ID_COINS)) {
				return this.exit("Out of coins.");
			}

			this.withdraw(ITEM_ID_COINS, COIN_COUNT - coinCount);
			return SLEEP_ONE_TICK;
		}

		if (this.foodCount == 0) {
			final int emptyCount = MAX_INV_SIZE - this.getInventoryCount();

			if (emptyCount != 0) {
				final int bankCount = this.bankCount(this.food.getId());

				if (bankCount < emptyCount) {
					return this.exit("Out of food.");
				}

				this.withdraw(this.food.getId(), emptyCount);
				return SLEEP_ONE_TICK;
			}
		} else {
			final int foodCount = this.getInventoryCount(this.food.getId());

			if (foodCount < this.foodCount) {
				final int bankCount = this.bankCount(this.food.getId());

				if (bankCount < this.foodCount - foodCount) {
					return this.exit("Out of food.");
				}

				this.withdraw(this.food.getId(), this.foodCount - foodCount);
				return SLEEP_ONE_TICK;
			}
		}

		this.banking = false;
		return 0;
	}

	private int enterBank() {
		if (this.distanceTo(Object.BANK_DOORS.coordinate.getX(),
			Object.BANK_DOORS.coordinate.getY()) <= 1) {
			if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) ==
				Object.BANK_DOORS.id) {
				this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int talkToCustomsOfficial() {
		if (this.isQuestMenu()) {
			int index;

			for (final String menuOption : MENU_OPTIONS_CUSTOMS_OFFICIAL) {
				index = this.getMenuIndex(menuOption);

				if (index == -1) {
					continue;
				}

				this.answer(index);
				this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				return 0;
			}

			return 0;
		}

		if (System.currentTimeMillis() <= this.optionMenuTimeout) {
			return 0;
		}

		final int[] customsOfficial = this.getNpcByIdNotTalk(NPC_ID_CUSTOMS_OFFICIAL);

		if (customsOfficial[0] == -1) {
			return 0;
		}

		this.talkToNpc(customsOfficial[0]);
		this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	private int leaveMossGiants() {
		if (this.inCombat()) {
			this.walkTo(this.playerX, this.playerY);
			return SLEEP_ONE_TICK;
		}

		if (Area.HUT.contains(this.playerX, this.playerY) &&
			this.getWallObjectIdFromCoords(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY()) ==
				Object.DOOR.id) {
			this.atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.atObject(Object.TREE_WEST.coordinate.getX(), Object.TREE_WEST.coordinate.getY());
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

			if (!Area.MOSS_GIANTS.contains(groundItemX, groundItemY)) {
				continue;
			}

			final int distance = this.distanceTo(groundItemX, groundItemY);

			if (distance >= currentDistance) {
				continue;
			}

			currentDistance = distance;

			this.loot[0] = groundItemId;
			this.loot[1] = groundItemX;
			this.loot[2] = groundItemY;
		}
	}

	private int consume(final int inventoryIndex) {
		if (System.currentTimeMillis() <= this.actionTimeout) {
			return 0;
		}

		this.useItem(inventoryIndex);
		this.actionTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private boolean needToOpenDoor(final int x, final int y) {
		if (this.getWallObjectIdFromCoords(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY()) !=
			Object.DOOR.id) {
			return false;
		}

		return Area.HUT.contains(this.playerX, this.playerY) != Area.HUT.contains(x, y);
	}

	private boolean canPickupLoot() {
		return this.getInventoryCount() != MAX_INV_SIZE ||
			(inArray(ITEM_IDS_STACKABLE, this.loot[0]) && this.hasInventoryItem(this.loot[0]));
	}

	private boolean isOnBrimhaven() {
		return this.playerY >= Area.BRIMHAVEN_DOCKS.lowerBoundingCoordinate.getY();
	}

	private void updateNotableLoot() {
		for (int index = 0; index < this.getInventoryCount(); index++) {
			final int itemId = this.getInventoryId(index);

			if (!inArray(ITEM_IDS_LOOT_NOTABLE, itemId)) {
				continue;
			}

			this.notableLoot.merge(itemId, this.getInventoryStack(index), Integer::sum);
		}
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
			return this.lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return this.upperBoundingCoordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
		}
	}
}
