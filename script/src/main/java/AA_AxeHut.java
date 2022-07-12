import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trains at the Axe Hut.
 * <p>
 * Required:
 * Start script at Ardougne North Bank with three most valuable items (body, legs/shield, weapon) equipped.
 * Have multiple sleeping bags, lockpicks, food, law runes, and water runes in your bank.
 * Have 51+ magic and Ardougne Teleport unlocked.
 * <p>
 * Optional:
 * Steel gauntlets (inventory), ruby amulets of strength, capes of legends.
 * <p>
 * Optional Parameters:
 * -f,--food <shrimp|...|lobster|shark> (default Lobster)
 * -m,--fightmode <controlled|attack|strength|defense> (default Strength)
 * <p>
 * Notes:
 * If pked, the script banks in Draynor for runes, teleports to ardougne, and then re-gears up at Ardougne North Bank.
 * The script will return to Ardougne North Bank if attacked/pked and idle for 10 minutes to discourage pkers.
 * Protect item is enabled if hits drop below 50.
 * <p>
 *
 * @Author Chomp
 */
public class AA_AxeHut extends AA_Script {
	private static final Coordinate COORDINATE_ARDOUGNE_BANK = new Coordinate(585, 576);
	private static final Coordinate COORDINATE_LOAD_ARDOUGNE_BANK = new Coordinate(573, 592);
	private static final Coordinate COORDINATE_LOAD_DRAYNOR_BANK = new Coordinate(176, 642);
	private static final Coordinate COORDINATE_SLEEP = new Coordinate(160, 102);

	private static final Pattern PATTERN_PROJECTILE_SHOT = Pattern.compile("Warning! (.+) is shooting at you!");

	private static final String[] WEAPON_NAMES = {"battle axe", "2-handed", "dragon axe", "dragon sword"};

	private static final int NPC_ID_ANIMATED_AXE = 295;
	private static final int NPC_XP_ANIMATED_AXE = 112;

	private static final int ITEM_ID_LOCKPICK = 714;
	private static final int ITEM_ID_LEGENDS_CAPE = 1288;
	private static final int ITEM_ID_STEEL_GAUNTLETS = 698;
	private static final int ITEM_ID_RUBY_AMULET_OF_STRENGTH = 316;
	private static final int ITEM_ID_LAW_RUNE = 42;
	private static final int ITEM_ID_WATER_RUNE = 32;

	private static final int PRAYER_INDEX_PROTECT_ITEMS = 8;
	private static final int SPELL_INDEX_ARDOUGNE_TELEPORT = 26;
	private static final int REQUIRED_PRAYER_LEVEL = 25;

	private static final int RUNE_COUNT = 2;
	private static final int MINIMUM_HITS_PROTECT_ITEM = 50;
	private static final int MAXIMUM_FATIGUE = 99;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;

	private static final long COOL_DOWN_TIMEOUT = 1000 * 60 * 10;

	private final Set<String> pkers = new HashSet<>(3);
	private final Map<Integer, Spawn> spawnMap = new HashMap<>();

	private Food food = Food.LOBSTER;

	private String attackers = "";
	private Coordinate nextRespawn;
	private State state;
	private long startTime;

	private double initialCombatXp;

	private long actionTimeout;
	private long cooldownTimeout;

	private int playerX;
	private int playerY;

	private int eatAt;

	private int foodCount;
	private int amuletCount;
	private int attackedCount;
	private int deathCount;

	private boolean attackedByPker;
	private boolean died;

	public AA_AxeHut(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
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

		if (getWeaponIndex() == -1) {
			throw new IllegalStateException("Weapon missing from inventory.");
		}

		eatAt = getBaseHits() - food.getHealAmount();

		if (!hasInventoryItem(food.getId()) ||
			!hasInventoryItem(ITEM_ID_SLEEPING_BAG) ||
			!hasInventoryItem(ITEM_ID_LOCKPICK)) {
			state = State.BANK;
		} else {
			state = State.KILL;
		}

		setCombatStyle(combatStyle.getIndex());
		initialCombatXp = getTotalCombatXp();
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (died) {
			if (isDead()) {
				return 0;
			}

			died = false;
		}

		playerX = getX();
		playerY = getY();

		if (bot.getCombatStyle() != combatStyle.getIndex()) setCombatStyle(combatStyle.getIndex());

		switch (state) {
			case BANK:
				return bank();
			case ALTAR:
				return altar();
			case KILL:
				return kill();
			case COOLDOWN:
				return cooldown();
			case FATIGUED:
				return fatigued();
			case WALKBACK:
				return walkback();
			default:
				return exit("Invalid script state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("Warning")) {
			if (attackedByPker) {
				return;
			}

			final Matcher matcher = PATTERN_PROJECTILE_SHOT.matcher(message);

			if (!matcher.matches()) {
				return;
			}

			final String rsn = matcher.group(1);

			setAttackedByPker(rsn);
		} else if (message.startsWith("eat", 4) ||
			message.startsWith("fail", 4) ||
			message.startsWith("manage", 4) ||
			message.endsWith("it") ||
			message.endsWith("web") ||
			message.endsWith("lever") ||
			message.endsWith("shut") ||
			message.endsWith("open") ||
			message.endsWith("door")) {
			actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void onDeath() {
		System.out.printf("[%s] Oh dear! You are dead...%n", this);
		deathCount++;
		died = true;
		state = State.WALKBACK;
	}

	private void setAttackedByPker(final String rsn) {
		System.out.printf("[%s] Attacked by: %s%n", this, rsn);

		if (!pkers.contains(rsn)) {
			pkers.add(rsn);
			attackers += rsn + " ";
		}

		attackedByPker = true;
		attackedCount++;

		state = State.BANK;
	}

	private int getWeaponIndex() {
		for (int index = 0; index < getInventoryCount(); index++) {
			final int itemId = getInventoryId(index);

			final String itemName = getItemNameId(itemId).toLowerCase();

			for (final String weaponName : WEAPON_NAMES) {
				if (itemName.contains(weaponName)) {
					return index;
				}
			}
		}

		return -1;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Axe Hut", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		final int kills = (int) xpGained / NPC_XP_ANIMATED_AXE;

		drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, toUnitsPerHour(kills, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (attackers.isEmpty()) return;

		drawString(String.format("@yel@Attacked by: @whi@%s", attackers),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);
	}

	@Override
	public void onPlayerDamaged(final java.lang.Object player) {
		if (player != bot.getPlayer() || !inCombat()) {
			return;
		}

		if (bot.getPlayerCount() > 1 && !attackedByPker) {
			final String pkerName = getPkerName();

			if (pkerName != null) {
				setAttackedByPker(pkerName);
			}
		}

		if (getLevel(Skill.PRAYER.getIndex()) >= REQUIRED_PRAYER_LEVEL &&
			getCurrentHits() <= MINIMUM_HITS_PROTECT_ITEM &&
			getCurrentLevel(Skill.PRAYER.getIndex()) > 0 &&
			!isPrayerEnabled(PRAYER_INDEX_PROTECT_ITEMS)) {
			enablePrayer(PRAYER_INDEX_PROTECT_ITEMS);
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (bot.getNpcId(npc) != NPC_ID_ANIMATED_AXE) {
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

	private String getPkerName() {
		for (int index = 0; index < bot.getPlayerCount(); index++) {
			final java.lang.Object player = bot.getPlayer(index);

			if (player == bot.getPlayer() ||
				!bot.isMobInCombat(player) ||
				getPlayerX(index) != playerX ||
				getPlayerY(index) != playerY) {
				continue;
			}

			return getPlayerName(index);
		}

		return null;
	}

	private int bank() {
		if (Area.ARDOUGNE_BANK.contains(playerX, playerY)) {
			if (attackedByPker) {
				attackedByPker = false;
				state = State.COOLDOWN;
				cooldownTimeout = System.currentTimeMillis() + COOL_DOWN_TIMEOUT;
				return 0;
			}

			return withdrawArdougneBank();
		}

		if (!Area.WILDERNESS.contains(playerX, playerY)) {
			return enterArdougneBank();
		}

		return exitWilderness();
	}

	private int withdrawArdougneBank() {
		if (getCurrentHits() <= eatAt) {
			final int foodIndex = getInventoryIndex(food.getId());

			if (foodIndex != -1) {
				return eat(foodIndex);
			}
		}

		if (getInventoryCount() == MAX_INV_SIZE) {
			final int legendsCapeIndex = getInventoryIndex(ITEM_ID_LEGENDS_CAPE);

			if (legendsCapeIndex != -1 && !isItemEquipped(legendsCapeIndex)) {
				wearItem(legendsCapeIndex);
				return SLEEP_ONE_TICK;
			}

			final int steelGauntletsIndex = getInventoryIndex(ITEM_ID_STEEL_GAUNTLETS);

			if (steelGauntletsIndex != -1 && !isItemEquipped(steelGauntletsIndex)) {
				wearItem(steelGauntletsIndex);
				return SLEEP_ONE_TICK;
			}

			final int amuletIndex = getInventoryIndex(ITEM_ID_RUBY_AMULET_OF_STRENGTH);

			if (amuletIndex != -1 && !isItemEquipped(amuletIndex)) {
				wearItem(amuletIndex);
				return SLEEP_ONE_TICK;
			}

			if (getCurrentLevel(Skill.PRAYER.getIndex()) != getLevel(Skill.PRAYER.getIndex())) {
				state = State.ALTAR;
			} else {
				state = State.KILL;
			}

			return 0;
		}

		if (!isBanking()) {
			return openBank();
		}

		if (System.currentTimeMillis() <= actionTimeout) {
			return 0;
		}

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			if (!hasBankItem(ITEM_ID_SLEEPING_BAG)) {
				return exit("Out of sleeping bags.");
			}

			withdraw(ITEM_ID_SLEEPING_BAG, 1);
			actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (!hasInventoryItem(ITEM_ID_LOCKPICK)) {
			if (!hasBankItem(ITEM_ID_LOCKPICK)) {
				return exit("Out of lockpicks.");
			}

			withdraw(ITEM_ID_LOCKPICK, 1);
			actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (!hasInventoryItem(ITEM_ID_LEGENDS_CAPE) &&
			hasBankItem(ITEM_ID_LEGENDS_CAPE)) {
			withdraw(ITEM_ID_LEGENDS_CAPE, 1);
			actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (!hasInventoryItem(ITEM_ID_RUBY_AMULET_OF_STRENGTH) &&
			hasBankItem(ITEM_ID_RUBY_AMULET_OF_STRENGTH)) {
			amuletCount++;
			withdraw(ITEM_ID_RUBY_AMULET_OF_STRENGTH, 1);
			actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (!hasBankItem(food.getId())) {
			return exit("Out of food.");
		}

		withdraw(food.getId(), MAX_INV_SIZE - getInventoryCount());
		actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int enterArdougneBank() {
		if (Area.ARDOUGNE_LEVER_HUT.contains(playerX, playerY) && wallExists(Object.ARDOUGNE_LEVER_HUT_DOOR)) {
			return openDoor(Object.ARDOUGNE_LEVER_HUT_DOOR, false);
		}

		if (Area.ARDOUGNE_ZOO.contains(playerX, playerY)) {
			return exitArdougneZoo();
		}

		if (playerX < COORDINATE_LOAD_ARDOUGNE_BANK.getX() && playerY > COORDINATE_LOAD_ARDOUGNE_BANK.getY()) {
			walkTo(COORDINATE_LOAD_ARDOUGNE_BANK.getX(), COORDINATE_LOAD_ARDOUGNE_BANK.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(COORDINATE_ARDOUGNE_BANK.getX(), COORDINATE_ARDOUGNE_BANK.getY());
		return SLEEP_ONE_TICK;
	}

	private int exitArdougneZoo() {
		if (getObjectIdFromCoords(Object.ARDOUGNE_ZOO_GATE.coordinate.getX(), Object.ARDOUGNE_ZOO_GATE.coordinate.getY()) == (Object.ARDOUGNE_ZOO_GATE.id)) {
			atObject(Object.ARDOUGNE_ZOO_GATE.coordinate.getX(), Object.ARDOUGNE_ZOO_GATE.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(Object.ARDOUGNE_ZOO_GATE.coordinate.getX() + 1, Object.ARDOUGNE_ZOO_GATE.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int exitWilderness() {
		if (getCurrentHits() <= eatAt && !inCombat()) {
			final int foodIndex = getInventoryIndex(food.getId());

			if (foodIndex != -1) {
				foodCount++;
				return eat(foodIndex);
			}
		}

		if (Area.WILDERNESS_LEVER_HUT.contains(playerX, playerY)) {
			return pullWildernessLever();
		}

		if (playerY >= Object.WEB_SOUTH.coordinate.getY()) {
			return enterWildernessLeverHut();
		}

		if (Area.AXE_HUT.contains(playerX, playerY)) {
			return exitAxeHut();
		}

		if (playerY >= Object.WEB_NORTH.coordinate.getY()) {
			return exitSouthWeb();
		}

		return exitNorthWeb();
	}

	private int pullWildernessLever() {
		if (inCombat() ||
			distanceTo(Object.WILDERNESS_LEVER.coordinate.getX(), Object.WILDERNESS_LEVER.coordinate.getY()) > 1) {
			walkTo(Object.WILDERNESS_LEVER.coordinate.getX(), Object.WILDERNESS_LEVER.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		return pullLever(Object.WILDERNESS_LEVER);
	}

	private int enterWildernessLeverHut() {
		final boolean door = wallExists(Object.WILDERNESS_LEVER_HUT_DOOR);

		if (inCombat()) {
			if (door) {
				walkTo(Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getX(), Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getY() - 1);
			} else {
				walkTo(Object.WILDERNESS_LEVER.coordinate.getX(), Object.WILDERNESS_LEVER.coordinate.getY() - 1);
			}

			return SLEEP_ONE_TICK;
		}

		if (door) {
			if (playerX != Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getX() ||
				playerY != Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getY() - 1) {
				walkTo(Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getX(), Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			return openDoor(Object.WILDERNESS_LEVER_HUT_DOOR, false);
		}

		walkTo(Object.WILDERNESS_LEVER.coordinate.getX(), Object.WILDERNESS_LEVER.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int exitSouthWeb() {
		final boolean web = wallExists(Object.WEB_SOUTH);

		if (inCombat()) {
			if (web) {
				walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY() - 1);
			} else {
				walkTo(Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getX(), Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getY() - 1);
			}

			return SLEEP_ONE_TICK;
		}

		if (web) {
			if (playerX != Object.WEB_SOUTH.coordinate.getX() || playerY != Object.WEB_SOUTH.coordinate.getY() - 1) {
				walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			return cutWeb(Object.WEB_SOUTH);
		}

		walkTo(Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getX(), Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int exitNorthWeb() {
		final boolean web = wallExists(Object.WEB_NORTH);

		if (inCombat()) {
			if (web) {
				walkTo(Object.WEB_NORTH.coordinate.getX(), Object.WEB_NORTH.coordinate.getY() - 1);
			} else {
				walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY() - 1);
			}

			return SLEEP_ONE_TICK;
		}

		if (web) {
			if (playerX != Object.WEB_NORTH.coordinate.getX() || playerY != Object.WEB_NORTH.coordinate.getY() - 1) {
				walkTo(Object.WEB_NORTH.coordinate.getX(), Object.WEB_NORTH.coordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			return cutWeb(Object.WEB_NORTH);
		}

		walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int exitAxeHut() {
		if (inCombat() ||
			playerX != Object.AXE_HUT_DOOR.coordinate.getX() ||
			playerY != Object.AXE_HUT_DOOR.coordinate.getY()) {
			walkTo(Object.AXE_HUT_DOOR.coordinate.getX(), Object.AXE_HUT_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		return openDoor(Object.AXE_HUT_DOOR, false);
	}

	private int altar() {
		if (getCurrentLevel(Skill.PRAYER.getIndex()) == getLevel(Skill.PRAYER.getIndex())) {
			if (!Area.ARDOUGNE_CHAPEL.contains(playerX, playerY)) {
				state = State.KILL;
				return 0;
			}

			if (getObjectIdFromCoords(Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getX(), Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getY()) == Object.ARDOUGNE_CHAPEL_DOORS.id) {
				atObject(Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getX(), Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getX(), Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.ARDOUGNE_CHAPEL.contains(playerX, playerY)) {
			if (isPrayerEnabled(PRAYER_INDEX_PROTECT_ITEMS)) {
				disablePrayer(PRAYER_INDEX_PROTECT_ITEMS);
				return SLEEP_ONE_TICK;
			}

			atObject(Object.ARDOUGNE_CHAPEL_ALTAR.coordinate.getX(), Object.ARDOUGNE_CHAPEL_ALTAR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (distanceTo(Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getX(), Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			if (getObjectIdFromCoords(Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getX(), Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getY()) == Object.ARDOUGNE_CHAPEL_DOORS.id) {
				atObject(Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getX(), Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.ARDOUGNE_CHAPEL_ALTAR.coordinate.getX() + 1, Object.ARDOUGNE_CHAPEL_ALTAR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getX(), Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int kill() {
		if (Area.AXE_HUT.contains(playerX, playerY)) {
			return combatCycle();
		}

		if (playerY < Object.WEB_NORTH.coordinate.getY()) {
			return enterAxeHut();
		}

		if (playerY < Object.WEB_SOUTH.coordinate.getY()) {
			return enterNorthWeb();
		}

		if (Area.WILDERNESS.contains(playerX, playerY)) {
			if (!Area.WILDERNESS_LEVER_HUT.contains(playerX, playerY)) {
				return enterSouthWeb();
			}

			return exitWildernessLeverHut();
		}

		return enterWilderness();
	}

	private int combatCycle() {
		final int foodIndex = getInventoryIndex(food.getId());

		if (foodIndex == -1) {
			state = State.BANK;
			return 0;
		}

		if (inCombat()) {
			return 0;
		}

		if (getCurrentHits() <= eatAt) {
			return eat(foodIndex);
		}

		if (getFatigue() >= MAXIMUM_FATIGUE) {
			state = State.FATIGUED;
			return 0;
		}

		final int[] animatedAxe = getNpcById(NPC_ID_ANIMATED_AXE);

		if (animatedAxe[0] != -1) {
			attackNpc(animatedAxe[0]);
			return SLEEP_ONE_TICK;
		}

		if (nextRespawn != null &&
			(playerX != nextRespawn.getX() || playerY != nextRespawn.getY())) {
			walkTo(nextRespawn.getX(), nextRespawn.getY());
			return SLEEP_ONE_TICK;
		}

		return 0;
	}

	private int enterAxeHut() {
		if (playerX != Object.AXE_HUT_DOOR.coordinate.getX() || playerY != Object.AXE_HUT_DOOR.coordinate.getY() - 1) {
			walkTo(Object.AXE_HUT_DOOR.coordinate.getX(), Object.AXE_HUT_DOOR.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (!spawnMap.isEmpty()) {
			resetSpawns();
		}

		return openDoor(Object.AXE_HUT_DOOR, true);
	}

	private int enterNorthWeb() {
		if (wallExists(Object.WEB_NORTH)) {
			if (playerX != Object.WEB_NORTH.coordinate.getX() || playerY != Object.WEB_NORTH.coordinate.getY()) {
				walkTo(Object.WEB_NORTH.coordinate.getX(), Object.WEB_NORTH.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			return cutWeb(Object.WEB_NORTH);
		}

		walkTo(Object.AXE_HUT_DOOR.coordinate.getX(), Object.AXE_HUT_DOOR.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int enterSouthWeb() {
		if (wallExists(Object.WEB_SOUTH)) {
			if (playerX != Object.WEB_SOUTH.coordinate.getX() || playerY != Object.WEB_SOUTH.coordinate.getY()) {
				walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			return cutWeb(Object.WEB_SOUTH);
		}

		walkTo(Object.WEB_NORTH.coordinate.getX(), Object.WEB_NORTH.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int exitWildernessLeverHut() {
		if (wallExists(Object.WILDERNESS_LEVER_HUT_DOOR)) {
			if (playerX != Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getX() ||
				playerY != Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getY()) {
				walkTo(Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getX(), Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			return openDoor(Object.WILDERNESS_LEVER_HUT_DOOR, false);
		}

		walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int enterWilderness() {
		if (Area.ARDOUGNE_LEVER_HUT.contains(playerX, playerY)) {
			return pullArdougneLever();
		}

		return enterArdougneLeverHut();
	}

	private int pullArdougneLever() {
		if (distanceTo(Object.ARDOUGNE_LEVER.coordinate.getX(), Object.ARDOUGNE_LEVER.coordinate.getY()) > 1) {
			walkTo(Object.ARDOUGNE_LEVER.coordinate.getX() - 1, Object.ARDOUGNE_LEVER.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		return pullLever(Object.ARDOUGNE_LEVER);
	}

	private int enterArdougneLeverHut() {
		if (distanceTo(Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getX(), Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			if (wallExists(Object.ARDOUGNE_LEVER_HUT_DOOR)) {
				if (playerX != Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getX() - 1 ||
					playerY != Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getY()) {
					walkTo(Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getX() - 1, Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getY());
					return SLEEP_ONE_TICK;
				}

				return openDoor(Object.ARDOUGNE_LEVER_HUT_DOOR, false);
			}

			walkTo(Object.ARDOUGNE_LEVER.coordinate.getX() - 1, Object.ARDOUGNE_LEVER.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getX() - 1, Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int cooldown() {
		if (System.currentTimeMillis() <= cooldownTimeout) {
			return SLEEP_ONE_SECOND * 30;
		}

		state = State.BANK;
		return 0;
	}

	private int fatigued() {
		if (!Area.AXE_HUT.contains(playerX, playerY)) {
			if (getFatigue() == 0) {
				state = State.KILL;
				return 0;
			}

			return sleep();
		}

		return exitAxeHut();
	}

	private int walkback() {
		if (Area.ARDOUGNE_ZOO.contains(playerX, playerY)) {
			state = State.BANK;
			return 0;
		}

		if (Area.DRAYNOR_BANK.contains(playerX, playerY)) {
			if (getInventoryCount(ITEM_ID_LAW_RUNE) >= RUNE_COUNT &&
				getInventoryCount(ITEM_ID_WATER_RUNE) >= RUNE_COUNT) {
				castOnSelf(SPELL_INDEX_ARDOUGNE_TELEPORT);
				return SLEEP_ONE_SECOND;
			}

			return withdrawDraynorBank();
		}

		if (playerX >= COORDINATE_LOAD_DRAYNOR_BANK.getX()) {
			if (distanceTo(Object.DRAYNOR_BANK_DOORS.coordinate.getX(), Object.DRAYNOR_BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (getObjectIdFromCoords(Object.DRAYNOR_BANK_DOORS.coordinate.getX(), Object.DRAYNOR_BANK_DOORS.coordinate.getY()) == Object.DRAYNOR_BANK_DOORS.id) {
					atObject(Object.DRAYNOR_BANK_DOORS.coordinate.getX(), Object.DRAYNOR_BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(Object.DRAYNOR_BANK_DOORS.coordinate.getX(), Object.DRAYNOR_BANK_DOORS.coordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			walkTo(Object.DRAYNOR_BANK_DOORS.coordinate.getX(), Object.DRAYNOR_BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(COORDINATE_LOAD_DRAYNOR_BANK.getX(), COORDINATE_LOAD_DRAYNOR_BANK.getY());
		return SLEEP_ONE_TICK;
	}

	private int withdrawDraynorBank() {
		if (!isBanking()) {
			return openBank();
		}

		if (System.currentTimeMillis() <= actionTimeout) {
			return 0;
		}

		if (!hasInventoryItem(ITEM_ID_LAW_RUNE)) {
			if (!hasBankItem(ITEM_ID_LAW_RUNE)) {
				return exit("Out of law runes.");
			}

			withdraw(ITEM_ID_LAW_RUNE, RUNE_COUNT);
			actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (!hasBankItem(ITEM_ID_WATER_RUNE)) {
			return exit("Out of water runes.");
		}

		withdraw(ITEM_ID_WATER_RUNE, RUNE_COUNT);
		actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int eat(final int inventoryIndex) {
		if (System.currentTimeMillis() <= actionTimeout) {
			return 0;
		}

		foodCount++;
		useItem(inventoryIndex);
		actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		return 0;
	}

	private boolean wallExists(final Object object) {
		return getWallObjectIdFromCoords(object.coordinate.getX(), object.coordinate.getY()) == object.id;
	}

	private int openDoor(final Object door, final boolean picklock) {
		if (System.currentTimeMillis() <= actionTimeout) {
			return 0;
		}

		if (picklock) {
			atWallObject2(door.coordinate.getX(), door.coordinate.getY());
		} else {
			atWallObject(door.coordinate.getX(), door.coordinate.getY());
		}

		actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int cutWeb(final Object web) {
		if (System.currentTimeMillis() <= actionTimeout) {
			return 0;
		}

		final int weaponIndex = getWeaponIndex();

		useItemOnWallObject(weaponIndex, web.coordinate.getX(), web.coordinate.getY());
		actionTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private int pullLever(final Object lever) {
		if (System.currentTimeMillis() <= actionTimeout) {
			return 0;
		}

		atObject(lever.coordinate.getX(), lever.coordinate.getY());
		actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		return 0;
	}

	private void resetSpawns() {
		spawnMap.clear();
		nextRespawn = null;
	}

	private enum State {
		BANK,
		ALTAR,
		KILL,
		COOLDOWN,
		FATIGUED,
		WALKBACK
	}

	private enum Area implements RSArea {
		DRAYNOR_BANK(new Coordinate(216, 634), new Coordinate(223, 638)),
		ARDOUGNE_ZOO(new Coordinate(564, 608), new Coordinate(590, 623)),
		ARDOUGNE_BANK(new Coordinate(577, 572), new Coordinate(585, 576)),
		ARDOUGNE_CHAPEL(new Coordinate(577, 589), new Coordinate(586, 595)),
		ARDOUGNE_LEVER_HUT(new Coordinate(619, 595), new Coordinate(621, 598)),
		WILDERNESS(new Coordinate(157, 96), new Coordinate(186, 131)),
		WILDERNESS_LEVER_HUT(new Coordinate(179, 127), new Coordinate(181, 129)),
		AXE_HUT(new Coordinate(157, 103), new Coordinate(163, 107));

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
		DRAYNOR_BANK_DOORS(64, new Coordinate(219, 633)),
		ARDOUGNE_ZOO_GATE(57, new Coordinate(567, 607)),
		ARDOUGNE_CHAPEL_DOORS(64, new Coordinate(587, 592)),
		ARDOUGNE_CHAPEL_ALTAR(19, new Coordinate(577, 591)),
		ARDOUGNE_LEVER_HUT_DOOR(2, new Coordinate(619, 596)),
		ARDOUGNE_LEVER(348, new Coordinate(621, 596)),
		WILDERNESS_LEVER(349, new Coordinate(180, 129)),
		WILDERNESS_LEVER_HUT_DOOR(2, new Coordinate(180, 127)),
		WEB_SOUTH(24, new Coordinate(181, 122)),
		WEB_NORTH(24, new Coordinate(176, 107)),
		AXE_HUT_DOOR(100, new Coordinate(160, 103));

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
