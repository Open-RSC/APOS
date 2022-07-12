import java.util.Map;
import java.util.TreeMap;

/**
 * Kills blue dragon at Hero's Guild and banks at Falador West Bank.
 * <p>
 * Required:
 * Start script at Falador West Bank.
 * Inventory: Sleeping bag, staff of air, dragon sword/axe, antidragonshield, amulet, gloves, monk robes.
 * Bank: Law runes, water runes, food
 * <p>
 * Optional Parameters:
 * -f,--food        <tuna|lobster|swordfish|bass|shark>  (default lobster)
 * -m,--fightmode   <controlled|attack|defense|strength> (default attack)
 * -b,--bury                                             (bury bones)
 * --min-hits       <#>                                  (default 15)
 * --min-pray       <#>                                  (default 1)
 * --food-count     <#>                                  (default 1)
 * --no-atk                                              (no incredible reflexes prayer)
 * --no-str                                              (no ultimate strength prayer)
 * <p>
 *
 * @Author Chomp
 */
public class AA_HerosGuildBlueDragon extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_MEMBERS_GATE = new Coordinate(326, 544);
	private static final Coordinate COORDINATE_BLUE_DRAGON_SPAWN = new Coordinate(376, 3270);

	private static final int[] ITEM_IDS_LOOT = new int[]{
		31, 32, 40, 42,
		396, 438, 439, 441, 442, 443,
		526, 527, 555,
		814, 1092, 1277
	};
	private static final int[] ITEM_IDS_PAINT_LOOT = new int[]{40, 438, 439, 441, 442, 443, 526, 527, 814, 1092, 1277};
	private static final int[] ITEM_IDS_STACKABLE_LOOT = new int[]{31, 32, 40, 42};
	private static final int[] ITEM_IDS_DRAGON_WEAPON = new int[]{593, 594};

	private static final int ITEM_ID_DRAGON_BONES = 814;
	private static final int ITEM_ID_ANTI_DRAGON_SHIELD = 420;
	private static final int ITEM_ID_STAFF_OF_AIR = 101;
	private static final int ITEM_ID_WATER_RUNE = 32;
	private static final int ITEM_ID_LAW_RUNE = 42;

	private static final int NPC_ID_BLUE_DRAGON = 202;
	private static final int NPC_XP_BLUE_DRAGON = 230;

	private static final int SPELL_ID_FALADOR_TELEPORT = 18;

	private final int[] groundItem = new int[3];

	private final Map<Integer, Integer> paintLoot = new TreeMap<>();

	private State state;
	private long startTime;

	private double initialCombatXp;
	private double initialPrayerXp;

	private long gateTimeout;
	private long consumeTimeout;
	private long withdrawFoodTimeout;
	private long withdrawLawTimeout;
	private long withdrawWaterTimeout;

	private int[] foodIds;

	private int staffOfAirIndex;
	private int weaponIndex;

	private int eatThreshold;
	private int bonesBuried;

	private int playerX;
	private int playerY;

	private boolean useIncredibleReflexes = true;
	private boolean useUltimateStrength = true;
	private boolean buryBones;

	private int minimumHits = 15;
	private int minimumPrayer = 1;
	private int foodCount = 1;

	private Food food = Food.LOBSTER;

	public AA_HerosGuildBlueDragon(final Extension extension) {
		super(extension);
		combatStyle = CombatStyle.ATTACK;
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "--min-hits":
						minimumHits = Integer.parseInt(args[++i]);
						break;
					case "--min-pray":
						minimumPrayer = Integer.parseInt(args[++i]);
						break;
					case "--no-atk":
						useIncredibleReflexes = false;
						break;
					case "--no-str":
						useUltimateStrength = false;
						break;
					case "-b":
					case "--bury":
						buryBones = true;
						break;
					case "--food-count":
						foodCount = Integer.parseInt(args[++i]);
						break;
					case "-f":
					case "--food":
						food = Food.valueOf(args[++i].toUpperCase());
						break;
					case "-m":
					case "--fightmode":
						combatStyle = CombatStyle.valueOf(args[++i].toUpperCase());
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		staffOfAirIndex = getInventoryIndex(ITEM_ID_STAFF_OF_AIR);

		if (staffOfAirIndex == -1) {
			throw new IllegalStateException("Staff of air missing from inventory.");
		}

		weaponIndex = getInventoryIndex(ITEM_IDS_DRAGON_WEAPON);

		if (weaponIndex == -1) {
			throw new IllegalStateException("Dragon weapon missing from inventory.");
		}

		if (!isItemIdEquipped(ITEM_ID_ANTI_DRAGON_SHIELD)) {
			throw new IllegalStateException("Equipped anti dragon breath shield missing from inventory.");
		}

		foodIds = new int[]{Food.BASS.getId(), food.getId()};
		eatThreshold = getBaseHits() -
			Math.max(food.getHealAmount(), Food.BASS.getHealAmount());
		initialCombatXp = getTotalCombatXp();
		initialPrayerXp = getAccurateXpForLevel(Skill.PRAYER.getIndex());
		setCombatStyle(combatStyle.getIndex());

		if (Area.BANK.contains(getX(), getY()) ||
			getCurrentHits() <= minimumHits ||
			!hasInventoryItem(ITEM_ID_LAW_RUNE) ||
			!hasInventoryItem(ITEM_ID_WATER_RUNE) ||
			(isInventoryFull() && !hasInventoryItem(foodIds))) {
			setState(State.BANK);
		} else {
			setState(State.SLAY);
		}

		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		if (bot.getCombatStyle() != combatStyle.getIndex()) setCombatStyle(combatStyle.getIndex());

		switch (state) {
			case SLAY:
				return slay();
			case BANK:
				return bank();
			case PRAY:
				return pray();
			default:
				return exit("Invalid script state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("eat", 4)) {
			consumeTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("bones")) {
			bonesBuried++;
			consumeTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("gate") || message.endsWith("shut") || message.endsWith("open")) {
			gateTimeout = 0L;
		} else {
			super.onServerMessage(message);
		}
	}

	private void setState(final State state) {
		this.state = state;

		if (this.state == State.BANK) {
			updatePaintLoot();
		}
	}

	private void updatePaintLoot() {
		for (final int itemId : ITEM_IDS_PAINT_LOOT) {
			final int itemCount = getInventoryCount(itemId);

			if (itemCount == 0) {
				continue;
			}

			paintLoot.merge(itemId, itemCount, Integer::sum);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Hero's Guild Blue Dragon", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@State: %s", state.description),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double combatXpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@yel@Combat Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(combatXpGained), toUnitsPerHour((int) combatXpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		final int kills = (int) combatXpGained / NPC_XP_BLUE_DRAGON;

		drawString(String.format("@yel@Slain: @whi@%d @cya@(@whi@%s kills@cya@/@whi@hr@cya@)",
				kills, toUnitsPerHour(kills, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (bonesBuried > 0) {
			final double prayerXpGained = getAccurateXpForLevel(Skill.PRAYER.getIndex()) - initialPrayerXp;

			drawString(String.format("@yel@Prayer Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
					DECIMAL_FORMAT.format(prayerXpGained), toUnitsPerHour((int) prayerXpGained, startTime)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

			drawString(String.format("@yel@Buried: @whi@%d @cya@(@whi@%s bones@cya@/@whi@hr@cya@)",
					bonesBuried, toUnitsPerHour(kills, startTime)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}

		if (!paintLoot.isEmpty()) {
			drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

			for (final Map.Entry<Integer, Integer> loot : paintLoot.entrySet()) {
				drawString(String.format("@or1@%s: @whi@%d", getItemNameId(loot.getKey()), loot.getValue()),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
			}
		}
	}

	@Override
	public void onPlayerDamaged(final java.lang.Object player) {
	}

	@Override
	public void onNpcDamaged(final java.lang.Object npc) {
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			return restock();
		}

		if (Area.GUILD_BASEMENT.contains(playerX, playerY)) {
			return teleportToFalador();
		}

		return enterBank();
	}

	private int restock() {
		if (getCurrentHits() <= eatThreshold) {
			final int foodIndex = getInventoryIndex(foodIds);

			if (foodIndex != -1) {
				return consume(foodIndex);
			}
		}

		if (!isBanking()) {
			return openBank();
		}

		for (int index = 0; index < getInventoryCount(); index++) {
			final int itemId = getInventoryId(index);

			if (itemId == ITEM_ID_LAW_RUNE ||
				itemId == ITEM_ID_WATER_RUNE ||
				!inArray(ITEM_IDS_LOOT, itemId)) {
				continue;
			}

			deposit(itemId, getInventoryCount(itemId));
			return SLEEP_ONE_TICK;
		}

		if (!hasInventoryItem(ITEM_ID_LAW_RUNE)) {
			if (System.currentTimeMillis() <= withdrawLawTimeout) {
				return 0;
			}

			if (!hasBankItem(ITEM_ID_LAW_RUNE)) {
				return exit("Out of law runes.");
			}

			withdraw(ITEM_ID_LAW_RUNE, 1);
			withdrawLawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (!hasInventoryItem(ITEM_ID_WATER_RUNE)) {
			if (System.currentTimeMillis() <= withdrawWaterTimeout) {
				return 0;
			}

			if (!hasBankItem(ITEM_ID_WATER_RUNE)) {
				return exit("Out of water runes.");
			}

			withdraw(ITEM_ID_WATER_RUNE, 1);
			withdrawWaterTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		final int foodInventoryCount = getInventoryCount(food.getId());

		if (foodInventoryCount < foodCount) {
			if (System.currentTimeMillis() <= withdrawFoodTimeout) {
				return 0;
			}

			if (!hasBankItem(food.getId())) {
				return exit("Out of food.");
			}

			withdraw(food.getId(), foodCount - foodInventoryCount);
			withdrawFoodTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		setState(State.SLAY);
		return 0;
	}

	private int teleportToFalador() {
		if (inCombat()) {
			walkTo(playerX, playerY);
			return SLEEP_ONE_TICK;
		}

		if (!isItemEquipped(staffOfAirIndex)) {
			wearItem(staffOfAirIndex);
			return SLEEP_ONE_TICK;
		}

		castOnSelf(SPELL_ID_FALADOR_TELEPORT);
		return SLEEP_TWO_SECONDS;
	}

	private int enterBank() {
		if (distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= 1) {
			if (getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) ==
				Object.BANK_DOORS.id) {
				atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (!isItemEquipped(weaponIndex)) {
			wearItem(weaponIndex);
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int slay() {
		if (Area.GUILD_BASEMENT.contains(playerX, playerY)) {
			return combat();
		}

		if (Area.GUILD_ENTRANCE.contains(playerX, playerY)) {
			if (!isFullPrayer()) {
				setState(State.PRAY);
				return 0;
			}

			if (isWalking() && getFatigue() != 0) {
				return sleep();
			}

			atObject(Object.GUILD_STAIRS_DOWN.coordinate.getX(), Object.GUILD_STAIRS_DOWN.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.GUILD_CHAPEL.contains(playerX, playerY)) {
			atObject(Object.GUILD_LADDER_DOWN.coordinate.getX(), Object.GUILD_LADDER_DOWN.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		return enterGuild();
	}

	private int combat() {
		if (getCurrentHits() <= minimumHits ||
			(isInventoryFull() && !hasInventoryItem(foodIds))) {
			setState(State.BANK);
			return 0;
		}

		if (getCurrentLevel(Skill.PRAYER.getIndex()) <= minimumPrayer) {
			setState(State.PRAY);
			return 0;
		}

		if (inCombat()) {
			if (!isPrayerEnabled(Prayer.PARALYZE_MONSTER.id)) {
				enablePrayer(Prayer.PARALYZE_MONSTER.id);
			} else if (useIncredibleReflexes && !isPrayerEnabled(Prayer.INCREDIBLE_REFLEXES.id)) {
				enablePrayer(Prayer.INCREDIBLE_REFLEXES.id);
			} else if (useUltimateStrength && !isPrayerEnabled(Prayer.ULTIMATE_STRENGTH.id)) {
				enablePrayer(Prayer.ULTIMATE_STRENGTH.id);
			}

			return SLEEP_ONE_TICK;
		}

		if (getCurrentHits() <= eatThreshold) {
			final int foodIndex = getInventoryIndex(foodIds);

			if (foodIndex != -1) {
				return consume(foodIndex);
			}
		}

		final int[] blueDragon = getNpcById(NPC_ID_BLUE_DRAGON);

		if (blueDragon[0] != -1) {
			final int npcX = getNpcX(blueDragon[0]);
			final int npcY = getNpcY(blueDragon[0]);

			if (needToOpenGate(npcX, npcY)) {
				return openGate();
			}

			if (!isPrayerEnabled(Prayer.PARALYZE_MONSTER.id)) {
				enablePrayer(Prayer.PARALYZE_MONSTER.id);
			}

			attackNpc(blueDragon[0]);
			return SLEEP_ONE_TICK;
		}

		if (isPrayerEnabled(Prayer.PARALYZE_MONSTER.id)) {
			disablePrayer(Prayer.PARALYZE_MONSTER.id);
			return SLEEP_ONE_TICK;
		}

		if (isPrayerEnabled(Prayer.INCREDIBLE_REFLEXES.id)) {
			disablePrayer(Prayer.INCREDIBLE_REFLEXES.id);
			return SLEEP_ONE_TICK;
		}

		if (isPrayerEnabled(Prayer.ULTIMATE_STRENGTH.id)) {
			disablePrayer(Prayer.ULTIMATE_STRENGTH.id);
			return SLEEP_ONE_TICK;
		}

		if (buryBones) {
			final int bonesIndex = getInventoryIndex(ITEM_ID_DRAGON_BONES);

			if (bonesIndex != -1) {
				return consume(bonesIndex);
			}
		}

		updateGroundItem();

		if (groundItem[0] != -1) {
			if (isInventoryFull() &&
				(!inArray(ITEM_IDS_STACKABLE_LOOT, groundItem[0]) || !hasInventoryItem(groundItem[0]))) {
				return consume(getInventoryIndex(foodIds));
			}

			if (needToOpenGate(groundItem[1], groundItem[2])) {
				return openGate();
			}

			pickupItem(groundItem[0], groundItem[1], groundItem[2]);
			return SLEEP_ONE_TICK;
		}

		if (!isFullPrayer()) {
			setState(State.PRAY);
			return 0;
		}

		if (playerX != COORDINATE_BLUE_DRAGON_SPAWN.getX() ||
			playerY != COORDINATE_BLUE_DRAGON_SPAWN.getY()) {
			if (!Area.GUILD_CAGE.contains(playerX, playerY) && isGateClosed()) {
				return openGate();
			}

			walkTo(COORDINATE_BLUE_DRAGON_SPAWN.getX(), COORDINATE_BLUE_DRAGON_SPAWN.getY());
		}

		return SLEEP_ONE_TICK;
	}

	private int enterGuild() {
		if (playerX > Object.MEMBERS_GATE.coordinate.getX()) {
			if (distanceTo(Object.GUILD_DOOR.coordinate.getX(), Object.GUILD_DOOR.coordinate.getY()) <= 1) {
				atWallObject(Object.GUILD_DOOR.coordinate.getX(), Object.GUILD_DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.GUILD_DOOR.coordinate.getX(), Object.GUILD_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerY <= COORDINATE_LOAD_MEMBERS_GATE.getY()) {
			if (distanceTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY()) <= 1) {
				if (System.currentTimeMillis() <= gateTimeout) {
					return 0;
				}

				atObject(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
				gateTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			walkTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BANK.contains(playerX, playerY) &&
			getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) ==
				Object.BANK_DOORS.id) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(COORDINATE_LOAD_MEMBERS_GATE.getX(), COORDINATE_LOAD_MEMBERS_GATE.getY());
		return SLEEP_ONE_TICK;
	}

	private int pray() {
		if (Area.GUILD_CHAPEL.contains(playerX, playerY)) {
			if (isFullPrayer()) {
				setState(State.SLAY);
				return 0;
			}

			if (isPrayerEnabled(Prayer.PARALYZE_MONSTER.id)) {
				disablePrayer(Prayer.PARALYZE_MONSTER.id);
				return SLEEP_ONE_TICK;
			}

			if (isPrayerEnabled(Prayer.INCREDIBLE_REFLEXES.id)) {
				disablePrayer(Prayer.INCREDIBLE_REFLEXES.id);
				return SLEEP_ONE_TICK;
			}

			if (isPrayerEnabled(Prayer.ULTIMATE_STRENGTH.id)) {
				disablePrayer(Prayer.ULTIMATE_STRENGTH.id);
				return SLEEP_ONE_TICK;
			}

			atObject(Object.ALTAR.coordinate.getX(), Object.ALTAR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (Area.GUILD_ENTRANCE.contains(playerX, playerY)) {
			atObject(Object.GUILD_LADDER_UP.coordinate.getX(), Object.GUILD_LADDER_UP.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (Area.GUILD_CAGE.contains(playerX, playerY)) {
			if (inCombat()) {
				walkTo(Object.GUILD_CAGE_GATE.coordinate.getX(), Object.GUILD_CAGE_GATE.coordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			if (getCurrentHits() <= eatThreshold) {
				final int foodIndex = getInventoryIndex(foodIds);

				if (foodIndex != -1) {
					return consume(foodIndex);
				}
			}

			if (isGateClosed()) {
				return openGate();
			}

			walkTo(Object.GUILD_STAIRS_UP.coordinate.getX() + 1, Object.GUILD_STAIRS_UP.coordinate.getY() + 3);
			return SLEEP_ONE_TICK;
		}

		if (inCombat()) {
			walkTo(Object.GUILD_STAIRS_UP.coordinate.getX() + 1, Object.GUILD_STAIRS_UP.coordinate.getY() + 3);
			return SLEEP_ONE_TICK;
		}

		if (getCurrentHits() <= eatThreshold) {
			final int foodIndex = getInventoryIndex(foodIds);

			if (foodIndex != -1) {
				return consume(foodIndex);
			}
		}

		atObject(Object.GUILD_STAIRS_UP.coordinate.getX(), Object.GUILD_STAIRS_UP.coordinate.getY());
		return SLEEP_ONE_SECOND;
	}

	private int consume(final int inventoryIndex) {
		if (System.currentTimeMillis() <= consumeTimeout) {
			return 0;
		}

		useItem(inventoryIndex);
		consumeTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private void updateGroundItem() {
		groundItem[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < getGroundItemCount(); index++) {
			final int groundItemId = getGroundItemId(index);

			if (!inArray(ITEM_IDS_LOOT, groundItemId)) {
				continue;
			}

			final int groundItemX = getItemX(index);

			if (groundItemX < Object.GUILD_STAIRS_UP.coordinate.getX()) {
				continue;
			}

			final int groundItemY = getItemY(index);

			final int distance = distanceTo(groundItemX, groundItemY);

			if (distance < currentDistance) {
				groundItem[0] = groundItemId;
				groundItem[1] = groundItemX;
				groundItem[2] = groundItemY;

				currentDistance = distance;
			}
		}
	}

	private int openGate() {
		if (System.currentTimeMillis() <= gateTimeout) {
			return 0;
		}

		atObject(Object.GUILD_CAGE_GATE.coordinate.getX(), Object.GUILD_CAGE_GATE.coordinate.getY());
		gateTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		return 0;
	}

	private boolean needToOpenGate(final int x, final int y) {
		if (!isGateClosed()) {
			return false;
		}

		return Area.GUILD_CAGE.contains(playerX, playerY) != Area.GUILD_CAGE.contains(x, y);
	}

	private boolean isGateClosed() {
		return getObjectIdFromCoords(Object.GUILD_CAGE_GATE.coordinate.getX(),
			Object.GUILD_CAGE_GATE.coordinate.getY()) == Object.GUILD_CAGE_GATE.id;
	}

	private boolean isFullPrayer() {
		return getCurrentLevel(Skill.PRAYER.getIndex()) == getLevel(Skill.PRAYER.getIndex());
	}

	private enum State {
		BANK("@mag@Bank Loot"),
		PRAY("@cya@Restore Prayer"),
		SLAY("@red@Slay Dragon");

		private final String description;

		State(final String description) {
			this.description = description;
		}
	}

	private enum Prayer {
		ULTIMATE_STRENGTH(10),
		INCREDIBLE_REFLEXES(11),
		PARALYZE_MONSTER(12);

		private final int id;

		Prayer(final int id) {
			this.id = id;
		}
	}

	private enum Area implements RSArea {
		GUILD_ENTRANCE(new Coordinate(368, 434), new Coordinate(377, 440)),
		GUILD_BASEMENT(new Coordinate(352, 3270), new Coordinate(376, 3283)),
		GUILD_CHAPEL(new Coordinate(369, 1379), new Coordinate(376, 1383)),
		GUILD_CAGE(new Coordinate(373, 3270), new Coordinate(376, 3275)),
		BANK(new Coordinate(328, 549), new Coordinate(334, 557));

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
		GUILD_DOOR(74, new Coordinate(372, 441)),
		GUILD_LADDER_UP(5, new Coordinate(375, 438)),
		GUILD_LADDER_DOWN(6, new Coordinate(375, 1382)),
		GUILD_STAIRS_UP(41, new Coordinate(368, 3270)),
		GUILD_STAIRS_DOWN(42, new Coordinate(368, 438)),
		MEMBERS_GATE(137, new Coordinate(341, 487)),
		GUILD_CAGE_GATE(57, new Coordinate(374, 3276)),
		ALTAR(19, new Coordinate(369, 1381)),
		BANK_DOORS(64, new Coordinate(327, 552));

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
