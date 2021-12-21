import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
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
 * Author: Chomp
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

	private static final int SKILL_INDEX_HITS = 3;
	private static final int SKILL_INDEX_PRAYER = 5;
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
	private Instant startTime;

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
						this.food = Food.valueOf(args[++i].toUpperCase());
						break;
					case "-m":
					case "--fightmode":
						this.combatStyle = CombatStyle.valueOf(args[++i].toUpperCase());
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		if (this.getWeaponIndex() == -1) {
			throw new IllegalStateException("Weapon missing from inventory.");
		}

		this.eatAt = this.getLevel(SKILL_INDEX_HITS) - this.food.getHealAmount();

		if (!this.hasInventoryItem(this.food.getId()) ||
			!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG) ||
			!this.hasInventoryItem(ITEM_ID_LOCKPICK)) {
			this.state = State.BANK;
		} else {
			this.state = State.KILL;
		}

		this.setFightMode(this.combatStyle.getIndex());
		this.initialCombatXp = this.getTotalCombatXp();
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		if (this.died) {
			if (this.isDead()) {
				return 0;
			}

			this.died = false;
		}

		this.playerX = this.getX();
		this.playerY = this.getY();

		switch (this.state) {
			case BANK:
				return this.bank();
			case ALTAR:
				return this.altar();
			case KILL:
				return this.kill();
			case COOLDOWN:
				return this.cooldown();
			case FATIGUED:
				return this.fatigued();
			case WALKBACK:
				return this.walkback();
			default:
				return this.exit("Invalid script state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("Warning")) {
			if (this.attackedByPker) {
				return;
			}

			final Matcher matcher = PATTERN_PROJECTILE_SHOT.matcher(message);

			if (!matcher.matches()) {
				return;
			}

			final String rsn = matcher.group(1);

			this.setAttackedByPker(rsn);
		} else if (message.startsWith("eat", 4) ||
			message.startsWith("fail", 4) ||
			message.startsWith("manage", 4) ||
			message.endsWith("it") ||
			message.endsWith("web") ||
			message.endsWith("lever") ||
			message.endsWith("shut") ||
			message.endsWith("open") ||
			message.endsWith("door")) {
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void onPlayerDamaged(final java.lang.Object player) {
		if (player != this.extension.getPlayer() || !this.inCombat()) {
			return;
		}

		if (this.extension.getPlayerCount() > 1 && !this.attackedByPker) {
			final String pkerName = this.getPkerName();

			if (pkerName != null) {
				this.setAttackedByPker(pkerName);
			}
		}

		if (this.getLevel(SKILL_INDEX_PRAYER) >= REQUIRED_PRAYER_LEVEL &&
			this.getCurrentLevel(SKILL_INDEX_HITS) <= MINIMUM_HITS_PROTECT_ITEM &&
			this.getCurrentLevel(SKILL_INDEX_PRAYER) > 0 &&
			!this.isPrayerEnabled(PRAYER_INDEX_PROTECT_ITEMS)) {
			this.enablePrayer(PRAYER_INDEX_PROTECT_ITEMS);
		}
	}

	@Override
	public void onDeath() {
		System.out.printf("[%s] Oh dear! You are dead...%n", this);
		this.deathCount++;
		this.died = true;
		this.state = State.WALKBACK;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Axe Hut", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

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

		final int kills = (int) xpGained / NPC_XP_ANIMATED_AXE;

		this.drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, getUnitsPerHour(kills, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.nextRespawn != null) {
			this.drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)", this.nextRespawn.getX(), this.nextRespawn.getY()),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}

		if (!this.attackers.isEmpty()) {
			this.drawString(String.format("@yel@Attacked by: @whi@%s", this.attackers),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (this.extension.getNpcId(npc) != NPC_ID_ANIMATED_AXE) {
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
		if (Area.ARDOUGNE_BANK.contains(this.playerX, this.playerY)) {
			if (this.attackedByPker) {
				this.attackedByPker = false;
				this.state = State.COOLDOWN;
				this.cooldownTimeout = System.currentTimeMillis() + COOL_DOWN_TIMEOUT;
				return 0;
			}

			return this.withdrawArdougneBank();
		}

		if (!Area.WILDERNESS.contains(this.playerX, this.playerY)) {
			return this.enterArdougneBank();
		}

		return this.exitWilderness();
	}

	private int withdrawArdougneBank() {
		if (this.getCurrentLevel(SKILL_INDEX_HITS) <= this.eatAt) {
			final int foodIndex = this.getInventoryIndex(this.food.getId());

			if (foodIndex != -1) {
				return this.eat(foodIndex);
			}
		}

		if (this.getInventoryCount() == MAX_INV_SIZE) {
			final int legendsCapeIndex = this.getInventoryIndex(ITEM_ID_LEGENDS_CAPE);

			if (legendsCapeIndex != -1 && !this.isItemEquipped(legendsCapeIndex)) {
				this.wearItem(legendsCapeIndex);
				return SLEEP_ONE_TICK;
			}

			final int steelGauntletsIndex = this.getInventoryIndex(ITEM_ID_STEEL_GAUNTLETS);

			if (steelGauntletsIndex != -1 && !this.isItemEquipped(steelGauntletsIndex)) {
				this.wearItem(steelGauntletsIndex);
				return SLEEP_ONE_TICK;
			}

			final int amuletIndex = this.getInventoryIndex(ITEM_ID_RUBY_AMULET_OF_STRENGTH);

			if (amuletIndex != -1 && !this.isItemEquipped(amuletIndex)) {
				this.wearItem(amuletIndex);
				return SLEEP_ONE_TICK;
			}

			if (this.getCurrentLevel(SKILL_INDEX_PRAYER) != this.getLevel(SKILL_INDEX_PRAYER)) {
				this.state = State.ALTAR;
			} else {
				this.state = State.KILL;
			}

			return 0;
		}

		if (!this.isBanking()) {
			return this.openBank();
		}

		if (System.currentTimeMillis() <= this.actionTimeout) {
			return 0;
		}

		if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			if (!this.hasBankItem(ITEM_ID_SLEEPING_BAG)) {
				return this.exit("Out of sleeping bags.");
			}

			this.withdraw(ITEM_ID_SLEEPING_BAG, 1);
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (!this.hasInventoryItem(ITEM_ID_LOCKPICK)) {
			if (!this.hasBankItem(ITEM_ID_LOCKPICK)) {
				return this.exit("Out of lockpicks.");
			}

			this.withdraw(ITEM_ID_LOCKPICK, 1);
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (!this.hasInventoryItem(ITEM_ID_LEGENDS_CAPE) &&
			this.hasBankItem(ITEM_ID_LEGENDS_CAPE)) {
			this.withdraw(ITEM_ID_LEGENDS_CAPE, 1);
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (!this.hasInventoryItem(ITEM_ID_RUBY_AMULET_OF_STRENGTH) &&
			this.hasBankItem(ITEM_ID_RUBY_AMULET_OF_STRENGTH)) {
			this.amuletCount++;
			this.withdraw(ITEM_ID_RUBY_AMULET_OF_STRENGTH, 1);
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (!this.hasBankItem(this.food.getId())) {
			return this.exit("Out of food.");
		}

		this.withdraw(this.food.getId(), MAX_INV_SIZE - this.getInventoryCount());
		this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int enterArdougneBank() {
		if (Area.ARDOUGNE_LEVER_HUT.contains(this.playerX, this.playerY) && this.wallExists(Object.ARDOUGNE_LEVER_HUT_DOOR)) {
			return this.openDoor(Object.ARDOUGNE_LEVER_HUT_DOOR, false);
		}

		if (Area.ARDOUGNE_ZOO.contains(this.playerX, this.playerY)) {
			return this.exitArdougneZoo();
		}

		if (this.playerX < COORDINATE_LOAD_ARDOUGNE_BANK.getX() && this.playerY > COORDINATE_LOAD_ARDOUGNE_BANK.getY()) {
			this.walkTo(COORDINATE_LOAD_ARDOUGNE_BANK.getX(), COORDINATE_LOAD_ARDOUGNE_BANK.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(COORDINATE_ARDOUGNE_BANK.getX(), COORDINATE_ARDOUGNE_BANK.getY());
		return SLEEP_ONE_TICK;
	}

	private int exitArdougneZoo() {
		if (this.getObjectIdFromCoords(Object.ARDOUGNE_ZOO_GATE.coordinate.getX(), Object.ARDOUGNE_ZOO_GATE.coordinate.getY()) == (Object.ARDOUGNE_ZOO_GATE.id)) {
			this.atObject(Object.ARDOUGNE_ZOO_GATE.coordinate.getX(), Object.ARDOUGNE_ZOO_GATE.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(Object.ARDOUGNE_ZOO_GATE.coordinate.getX() + 1, Object.ARDOUGNE_ZOO_GATE.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int exitWilderness() {
		if (this.getCurrentLevel(SKILL_INDEX_HITS) <= this.eatAt && !this.inCombat()) {
			final int foodIndex = this.getInventoryIndex(this.food.getId());

			if (foodIndex != -1) {
				this.foodCount++;
				return this.eat(foodIndex);
			}
		}

		if (Area.WILDERNESS_LEVER_HUT.contains(this.playerX, this.playerY)) {
			return this.pullWildernessLever();
		}

		if (this.playerY >= Object.WEB_SOUTH.coordinate.getY()) {
			return this.enterWildernessLeverHut();
		}

		if (Area.AXE_HUT.contains(this.playerX, this.playerY)) {
			return this.exitAxeHut();
		}

		if (this.playerY >= Object.WEB_NORTH.coordinate.getY()) {
			return this.exitSouthWeb();
		}

		return this.exitNorthWeb();
	}

	private int pullWildernessLever() {
		if (this.inCombat() ||
			this.distanceTo(Object.WILDERNESS_LEVER.coordinate.getX(), Object.WILDERNESS_LEVER.coordinate.getY()) > 1) {
			this.walkTo(Object.WILDERNESS_LEVER.coordinate.getX(), Object.WILDERNESS_LEVER.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		return this.pullLever(Object.WILDERNESS_LEVER);
	}

	private int enterWildernessLeverHut() {
		final boolean door = this.wallExists(Object.WILDERNESS_LEVER_HUT_DOOR);

		if (this.inCombat()) {
			if (door) {
				this.walkTo(Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getX(), Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getY() - 1);
			} else {
				this.walkTo(Object.WILDERNESS_LEVER.coordinate.getX(), Object.WILDERNESS_LEVER.coordinate.getY() - 1);
			}

			return SLEEP_ONE_TICK;
		}

		if (door) {
			if (this.playerX != Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getX() ||
				this.playerY != Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getY() - 1) {
				this.walkTo(Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getX(), Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			return this.openDoor(Object.WILDERNESS_LEVER_HUT_DOOR, false);
		}

		this.walkTo(Object.WILDERNESS_LEVER.coordinate.getX(), Object.WILDERNESS_LEVER.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int exitSouthWeb() {
		final boolean web = this.wallExists(Object.WEB_SOUTH);

		if (this.inCombat()) {
			if (web) {
				this.walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY() - 1);
			} else {
				this.walkTo(Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getX(), Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getY() - 1);
			}

			return SLEEP_ONE_TICK;
		}

		if (web) {
			if (this.playerX != Object.WEB_SOUTH.coordinate.getX() || this.playerY != Object.WEB_SOUTH.coordinate.getY() - 1) {
				this.walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			return this.cutWeb(Object.WEB_SOUTH);
		}

		this.walkTo(Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getX(), Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int exitNorthWeb() {
		final boolean web = this.wallExists(Object.WEB_NORTH);

		if (this.inCombat()) {
			if (web) {
				this.walkTo(Object.WEB_NORTH.coordinate.getX(), Object.WEB_NORTH.coordinate.getY() - 1);
			} else {
				this.walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY() - 1);
			}

			return SLEEP_ONE_TICK;
		}

		if (web) {
			if (this.playerX != Object.WEB_NORTH.coordinate.getX() || this.playerY != Object.WEB_NORTH.coordinate.getY() - 1) {
				this.walkTo(Object.WEB_NORTH.coordinate.getX(), Object.WEB_NORTH.coordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			return this.cutWeb(Object.WEB_NORTH);
		}

		this.walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int exitAxeHut() {
		if (this.inCombat() ||
			this.playerX != Object.AXE_HUT_DOOR.coordinate.getX() ||
			this.playerY != Object.AXE_HUT_DOOR.coordinate.getY()) {
			this.walkTo(Object.AXE_HUT_DOOR.coordinate.getX(), Object.AXE_HUT_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		return this.openDoor(Object.AXE_HUT_DOOR, false);
	}

	private int altar() {
		if (this.getCurrentLevel(SKILL_INDEX_PRAYER) == this.getLevel(SKILL_INDEX_PRAYER)) {
			if (!Area.ARDOUGNE_CHAPEL.contains(this.playerX, this.playerY)) {
				this.state = State.KILL;
				return 0;
			}

			if (this.getObjectIdFromCoords(Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getX(), Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getY()) == Object.ARDOUGNE_CHAPEL_DOORS.id) {
				this.atObject(Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getX(), Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getX(), Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.ARDOUGNE_CHAPEL.contains(this.playerX, this.playerY)) {
			if (this.isPrayerEnabled(PRAYER_INDEX_PROTECT_ITEMS)) {
				this.disablePrayer(PRAYER_INDEX_PROTECT_ITEMS);
				return SLEEP_ONE_TICK;
			}

			this.atObject(Object.ARDOUGNE_CHAPEL_ALTAR.coordinate.getX(), Object.ARDOUGNE_CHAPEL_ALTAR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (this.distanceTo(Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getX(), Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			if (this.getObjectIdFromCoords(Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getX(), Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getY()) == Object.ARDOUGNE_CHAPEL_DOORS.id) {
				this.atObject(Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getX(), Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.ARDOUGNE_CHAPEL_ALTAR.coordinate.getX() + 1, Object.ARDOUGNE_CHAPEL_ALTAR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getX(), Object.ARDOUGNE_CHAPEL_DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int kill() {
		if (Area.AXE_HUT.contains(this.playerX, this.playerY)) {
			return this.combatCycle();
		}

		if (this.playerY < Object.WEB_NORTH.coordinate.getY()) {
			return this.enterAxeHut();
		}

		if (this.playerY < Object.WEB_SOUTH.coordinate.getY()) {
			return this.enterNorthWeb();
		}

		if (Area.WILDERNESS.contains(this.playerX, this.playerY)) {
			if (!Area.WILDERNESS_LEVER_HUT.contains(this.playerX, this.playerY)) {
				return this.enterSouthWeb();
			}

			return this.exitWildernessLeverHut();
		}

		return this.enterWilderness();
	}

	private int combatCycle() {
		final int foodIndex = this.getInventoryIndex(this.food.getId());

		if (foodIndex == -1) {
			this.state = State.BANK;
			return 0;
		}

		if (this.inCombat()) {
			return 0;
		}

		if (this.getCurrentLevel(SKILL_INDEX_HITS) <= this.eatAt) {
			return this.eat(foodIndex);
		}

		if (this.getFatigue() >= MAXIMUM_FATIGUE) {
			this.state = State.FATIGUED;
			return 0;
		}

		final int[] animatedAxe = this.getNpcById(NPC_ID_ANIMATED_AXE);

		if (animatedAxe[0] != -1) {
			this.attackNpc(animatedAxe[0]);
			return SLEEP_ONE_TICK;
		}

		if (this.nextRespawn != null &&
			(this.playerX != this.nextRespawn.getX() || this.playerY != this.nextRespawn.getY())) {
			this.walkTo(this.nextRespawn.getX(), this.nextRespawn.getY());
			return SLEEP_ONE_TICK;
		}

		return 0;
	}

	private int enterAxeHut() {
		if (this.playerX != Object.AXE_HUT_DOOR.coordinate.getX() || this.playerY != Object.AXE_HUT_DOOR.coordinate.getY() - 1) {
			this.walkTo(Object.AXE_HUT_DOOR.coordinate.getX(), Object.AXE_HUT_DOOR.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (!this.spawnMap.isEmpty()) {
			this.resetSpawns();
		}

		return this.openDoor(Object.AXE_HUT_DOOR, true);
	}

	private int enterNorthWeb() {
		if (this.wallExists(Object.WEB_NORTH)) {
			if (this.playerX != Object.WEB_NORTH.coordinate.getX() || this.playerY != Object.WEB_NORTH.coordinate.getY()) {
				this.walkTo(Object.WEB_NORTH.coordinate.getX(), Object.WEB_NORTH.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			return this.cutWeb(Object.WEB_NORTH);
		}

		this.walkTo(Object.AXE_HUT_DOOR.coordinate.getX(), Object.AXE_HUT_DOOR.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int enterSouthWeb() {
		if (this.wallExists(Object.WEB_SOUTH)) {
			if (this.playerX != Object.WEB_SOUTH.coordinate.getX() || this.playerY != Object.WEB_SOUTH.coordinate.getY()) {
				this.walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			return this.cutWeb(Object.WEB_SOUTH);
		}

		this.walkTo(Object.WEB_NORTH.coordinate.getX(), Object.WEB_NORTH.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int exitWildernessLeverHut() {
		if (this.wallExists(Object.WILDERNESS_LEVER_HUT_DOOR)) {
			if (this.playerX != Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getX() ||
				this.playerY != Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getY()) {
				this.walkTo(Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getX(), Object.WILDERNESS_LEVER_HUT_DOOR.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			return this.openDoor(Object.WILDERNESS_LEVER_HUT_DOOR, false);
		}

		this.walkTo(Object.WEB_SOUTH.coordinate.getX(), Object.WEB_SOUTH.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int enterWilderness() {
		if (Area.ARDOUGNE_LEVER_HUT.contains(this.playerX, this.playerY)) {
			return this.pullArdougneLever();
		}

		return this.enterArdougneLeverHut();
	}

	private int pullArdougneLever() {
		if (this.distanceTo(Object.ARDOUGNE_LEVER.coordinate.getX(), Object.ARDOUGNE_LEVER.coordinate.getY()) > 1) {
			this.walkTo(Object.ARDOUGNE_LEVER.coordinate.getX() - 1, Object.ARDOUGNE_LEVER.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		return this.pullLever(Object.ARDOUGNE_LEVER);
	}

	private int enterArdougneLeverHut() {
		if (this.distanceTo(Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getX(), Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
			if (this.wallExists(Object.ARDOUGNE_LEVER_HUT_DOOR)) {
				if (this.playerX != Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getX() - 1 ||
					this.playerY != Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getY()) {
					this.walkTo(Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getX() - 1, Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getY());
					return SLEEP_ONE_TICK;
				}

				return this.openDoor(Object.ARDOUGNE_LEVER_HUT_DOOR, false);
			}

			this.walkTo(Object.ARDOUGNE_LEVER.coordinate.getX() - 1, Object.ARDOUGNE_LEVER.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getX() - 1, Object.ARDOUGNE_LEVER_HUT_DOOR.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int cooldown() {
		if (System.currentTimeMillis() <= this.cooldownTimeout) {
			return SLEEP_ONE_SECOND * 30;
		}

		this.state = State.BANK;
		return 0;
	}

	private int fatigued() {
		if (!Area.AXE_HUT.contains(this.playerX, this.playerY)) {
			if (this.getFatigue() == 0) {
				this.state = State.KILL;
				return 0;
			}

			return this.sleep();
		}

		return this.exitAxeHut();
	}

	private int walkback() {
		if (Area.ARDOUGNE_ZOO.contains(this.playerX, this.playerY)) {
			this.state = State.BANK;
			return 0;
		}

		if (Area.DRAYNOR_BANK.contains(this.playerX, this.playerY)) {
			if (this.getInventoryCount(ITEM_ID_LAW_RUNE) >= RUNE_COUNT &&
				this.getInventoryCount(ITEM_ID_WATER_RUNE) >= RUNE_COUNT) {
				this.castOnSelf(SPELL_INDEX_ARDOUGNE_TELEPORT);
				return SLEEP_ONE_SECOND;
			}

			return this.withdrawDraynorBank();
		}

		if (this.playerX >= COORDINATE_LOAD_DRAYNOR_BANK.getX()) {
			if (this.distanceTo(Object.DRAYNOR_BANK_DOORS.coordinate.getX(), Object.DRAYNOR_BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (this.getObjectIdFromCoords(Object.DRAYNOR_BANK_DOORS.coordinate.getX(), Object.DRAYNOR_BANK_DOORS.coordinate.getY()) == Object.DRAYNOR_BANK_DOORS.id) {
					this.atObject(Object.DRAYNOR_BANK_DOORS.coordinate.getX(), Object.DRAYNOR_BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				this.walkTo(Object.DRAYNOR_BANK_DOORS.coordinate.getX(), Object.DRAYNOR_BANK_DOORS.coordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			this.walkTo(Object.DRAYNOR_BANK_DOORS.coordinate.getX(), Object.DRAYNOR_BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(COORDINATE_LOAD_DRAYNOR_BANK.getX(), COORDINATE_LOAD_DRAYNOR_BANK.getY());
		return SLEEP_ONE_TICK;
	}

	private int withdrawDraynorBank() {
		if (!this.isBanking()) {
			return this.openBank();
		}

		if (System.currentTimeMillis() <= this.actionTimeout) {
			return 0;
		}

		if (!this.hasInventoryItem(ITEM_ID_LAW_RUNE)) {
			if (!this.hasBankItem(ITEM_ID_LAW_RUNE)) {
				return this.exit("Out of law runes.");
			}

			this.withdraw(ITEM_ID_LAW_RUNE, RUNE_COUNT);
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (!this.hasBankItem(ITEM_ID_WATER_RUNE)) {
			return this.exit("Out of water runes.");
		}

		this.withdraw(ITEM_ID_WATER_RUNE, RUNE_COUNT);
		this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int eat(final int inventoryIndex) {
		if (System.currentTimeMillis() <= this.actionTimeout) {
			return 0;
		}

		this.foodCount++;
		this.useItem(inventoryIndex);
		this.actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		return 0;
	}

	private boolean wallExists(final Object object) {
		return this.getWallObjectIdFromCoords(object.coordinate.getX(), object.coordinate.getY()) == object.id;
	}

	private int openDoor(final Object door, final boolean picklock) {
		if (System.currentTimeMillis() <= this.actionTimeout) {
			return 0;
		}

		if (picklock) {
			this.atWallObject2(door.coordinate.getX(), door.coordinate.getY());
		} else {
			this.atWallObject(door.coordinate.getX(), door.coordinate.getY());
		}

		this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int cutWeb(final Object web) {
		if (System.currentTimeMillis() <= this.actionTimeout) {
			return 0;
		}

		final int weaponIndex = this.getWeaponIndex();

		this.useItemOnWallObject(weaponIndex, web.coordinate.getX(), web.coordinate.getY());
		this.actionTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private int pullLever(final Object lever) {
		if (System.currentTimeMillis() <= this.actionTimeout) {
			return 0;
		}

		this.atObject(lever.coordinate.getX(), lever.coordinate.getY());
		this.actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		return 0;
	}

	private void resetSpawns() {
		this.spawnMap.clear();
		this.nextRespawn = null;
	}

	private int getWeaponIndex() {
		for (int index = 0; index < this.getInventoryCount(); index++) {
			final int itemId = this.getInventoryId(index);

			final String itemName = getItemNameId(itemId).toLowerCase();

			for (final String weaponName : WEAPON_NAMES) {
				if (itemName.contains(weaponName)) {
					return index;
				}
			}
		}

		return -1;
	}

	private String getPkerName() {
		for (int index = 0; index < this.extension.getPlayerCount(); index++) {
			final java.lang.Object player = this.extension.getPlayer(index);

			if (player == this.extension.getPlayer() ||
				!this.extension.isMobInCombat(player) ||
				this.getPlayerX(index) != this.playerX ||
				this.getPlayerY(index) != this.playerY) {
				continue;
			}

			return this.getPlayerName(index);
		}

		return null;
	}

	private void setAttackedByPker(final String rsn) {
		System.out.printf("[%s] Attacked by: %s%n", this, rsn);

		if (!this.pkers.contains(rsn)) {
			this.pkers.add(rsn);
			this.attackers += rsn + " ";
		}

		this.attackedByPker = true;
		this.attackedCount++;

		this.state = State.BANK;
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
			return this.lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return this.upperBoundingCoordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
		}
	}
}
