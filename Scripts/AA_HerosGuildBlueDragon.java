import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
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
 * --no-pray                                             (no ultimate strength prayer)
 * <p>
 * Author: Chomp
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

	private static final int SKILL_INDEX_HITS = 3;
	private static final int SKILL_INDEX_PRAYER = 5;

	private final int[] groundItem = new int[3];

	private final Map<Integer, Integer> paintLoot = new TreeMap<>();

	private State state;
	private Instant startTime;

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
		this.combatStyle = CombatStyle.ATTACK;
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "--min-hits":
						this.minimumHits = Integer.parseInt(args[++i]);
						break;
					case "--min-pray":
						this.minimumPrayer = Integer.parseInt(args[++i]);
						break;
					case "--no-atk":
						this.useIncredibleReflexes = false;
						break;
					case "--no-str":
						this.useUltimateStrength = false;
						break;
					case "-b":
					case "--bury":
						this.buryBones = true;
						break;
					case "--food-count":
						this.foodCount = Integer.parseInt(args[++i]);
						break;
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

		if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		this.staffOfAirIndex = this.getInventoryIndex(ITEM_ID_STAFF_OF_AIR);

		if (this.staffOfAirIndex == -1) {
			throw new IllegalStateException("Staff of air missing from inventory.");
		}

		this.weaponIndex = this.getInventoryIndex(ITEM_IDS_DRAGON_WEAPON);

		if (this.weaponIndex == -1) {
			throw new IllegalStateException("Dragon weapon missing from inventory.");
		}

		if (!this.isItemIdEquipped(ITEM_ID_ANTI_DRAGON_SHIELD)) {
			throw new IllegalStateException("Equipped anti dragon breath shield missing from inventory.");
		}

		this.foodIds = new int[]{Food.BASS.getId(), this.food.getId()};
		this.eatThreshold = this.getLevel(SKILL_INDEX_HITS) -
			Math.max(this.food.getHealAmount(), Food.BASS.getHealAmount());
		this.initialCombatXp = this.getTotalCombatXp();
		this.initialPrayerXp = this.getAccurateXpForLevel(SKILL_INDEX_PRAYER);
		this.setFightMode(this.combatStyle.getIndex());

		if (Area.BANK.contains(this.getX(), this.getY()) ||
			this.getCurrentHits() <= this.minimumHits ||
			!this.hasInventoryItem(ITEM_ID_LAW_RUNE) ||
			!this.hasInventoryItem(ITEM_ID_WATER_RUNE) ||
			(this.isInventoryFull() && !this.hasInventoryItem(this.foodIds))) {
			this.setState(State.BANK);
		} else {
			this.setState(State.SLAY);
		}

		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		this.playerX = this.getX();
		this.playerY = this.getY();

		switch (this.state) {
			case SLAY:
				return this.slay();
			case BANK:
				return this.bank();
			case PRAY:
				return this.pray();
			default:
				return this.exit("Invalid script state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("eat", 4)) {
			this.consumeTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("bones")) {
			this.bonesBuried++;
			this.consumeTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("gate") || message.endsWith("shut") || message.endsWith("open")) {
			this.gateTimeout = 0L;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void onPlayerDamaged(final java.lang.Object player) {
	}

	@Override
	public void onNpcDamaged(final java.lang.Object npc) {
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Hero's Guild Blue Dragon", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@State: %s", this.state.description),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final double combatXpGained = this.getTotalCombatXp() - this.initialCombatXp;

		this.drawString(String.format("@yel@Combat Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(combatXpGained), getUnitsPerHour(combatXpGained, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final int kills = (int) combatXpGained / NPC_XP_BLUE_DRAGON;

		this.drawString(String.format("@yel@Slain: @whi@%d @cya@(@whi@%s kills@cya@/@whi@hr@cya@)",
				kills, getUnitsPerHour(kills, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.bonesBuried > 0) {
			this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			final double prayerXpGained = this.getAccurateXpForLevel(SKILL_INDEX_PRAYER) - this.initialPrayerXp;

			this.drawString(String.format("@yel@Prayer Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
					DECIMAL_FORMAT.format(prayerXpGained), getUnitsPerHour(prayerXpGained, secondsElapsed)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			this.drawString(String.format("@yel@Buried: @whi@%d @cya@(@whi@%s bones@cya@/@whi@hr@cya@)",
					this.bonesBuried, getUnitsPerHour(kills, secondsElapsed)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}

		if (!this.paintLoot.isEmpty()) {
			this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			for (final Map.Entry<Integer, Integer> loot : this.paintLoot.entrySet()) {
				this.drawString(String.format("@or1@%s: @whi@%d", getItemNameId(loot.getKey()), loot.getValue()),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
			}
		}
	}

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			return this.restock();
		}

		if (Area.GUILD_BASEMENT.contains(this.playerX, this.playerY)) {
			return this.teleportToFalador();
		}

		return this.enterBank();
	}

	private int restock() {
		if (this.getCurrentHits() <= this.eatThreshold) {
			final int foodIndex = this.getInventoryIndex(this.foodIds);

			if (foodIndex != -1) {
				return this.consume(foodIndex);
			}
		}

		if (!this.isBanking()) {
			return this.openBank();
		}

		for (int index = 0; index < this.getInventoryCount(); index++) {
			final int itemId = this.getInventoryId(index);

			if (itemId == ITEM_ID_LAW_RUNE ||
				itemId == ITEM_ID_WATER_RUNE ||
				!inArray(ITEM_IDS_LOOT, itemId)) {
				continue;
			}

			this.deposit(itemId, this.getInventoryCount(itemId));
			return SLEEP_ONE_TICK;
		}

		if (!this.hasInventoryItem(ITEM_ID_LAW_RUNE)) {
			if (System.currentTimeMillis() <= this.withdrawLawTimeout) {
				return 0;
			}

			if (!this.hasBankItem(ITEM_ID_LAW_RUNE)) {
				return this.exit("Out of law runes.");
			}

			this.withdraw(ITEM_ID_LAW_RUNE, 1);
			this.withdrawLawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (!this.hasInventoryItem(ITEM_ID_WATER_RUNE)) {
			if (System.currentTimeMillis() <= this.withdrawLawTimeout) {
				return 0;
			}

			if (!this.hasBankItem(ITEM_ID_WATER_RUNE)) {
				return this.exit("Out of water runes.");
			}

			this.withdraw(ITEM_ID_WATER_RUNE, 1);
			this.withdrawLawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		final int foodInventoryCount = this.getInventoryCount(this.food.getId());

		if (foodInventoryCount < this.foodCount) {
			if (System.currentTimeMillis() <= this.withdrawFoodTimeout) {
				return 0;
			}

			if (!this.hasBankItem(this.food.getId())) {
				return this.exit("Out of food.");
			}

			this.withdraw(this.food.getId(), this.foodCount - foodInventoryCount);
			this.withdrawFoodTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		this.setState(State.SLAY);
		return 0;
	}

	private int teleportToFalador() {
		if (this.inCombat()) {
			this.walkTo(this.playerX, this.playerY);
			return SLEEP_ONE_TICK;
		}

		if (!this.isItemEquipped(this.staffOfAirIndex)) {
			this.wearItem(this.staffOfAirIndex);
			return SLEEP_ONE_TICK;
		}

		this.castOnSelf(SPELL_ID_FALADOR_TELEPORT);
		return SLEEP_TWO_SECONDS;
	}

	private int enterBank() {
		if (this.distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= 1) {
			if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) ==
				Object.BANK_DOORS.id) {
				this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (!this.isItemEquipped(this.weaponIndex)) {
			this.wearItem(this.weaponIndex);
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int slay() {
		if (Area.GUILD_BASEMENT.contains(this.playerX, this.playerY)) {
			return this.combat();
		}

		if (Area.GUILD_ENTRANCE.contains(this.playerX, this.playerY)) {
			if (!this.isFullPrayer()) {
				this.setState(State.PRAY);
				return 0;
			}

			if (this.isWalking() && this.getFatigue() != 0) {
				return this.sleep();
			}

			this.atObject(Object.GUILD_STAIRS_DOWN.coordinate.getX(), Object.GUILD_STAIRS_DOWN.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.GUILD_CHAPEL.contains(this.playerX, this.playerY)) {
			this.atObject(Object.GUILD_LADDER_DOWN.coordinate.getX(), Object.GUILD_LADDER_DOWN.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		return this.enterGuild();
	}

	private int combat() {
		if (this.getCurrentHits() <= this.minimumHits ||
			(this.isInventoryFull() && !this.hasInventoryItem(this.foodIds))) {
			this.setState(State.BANK);
			return 0;
		}

		if (this.getCurrentLevel(SKILL_INDEX_PRAYER) <= this.minimumPrayer) {
			this.setState(State.PRAY);
			return 0;
		}

		if (this.inCombat()) {
			if (!this.isPrayerEnabled(Prayer.PARALYZE_MONSTER.id)) {
				this.enablePrayer(Prayer.PARALYZE_MONSTER.id);
			} else if (this.useIncredibleReflexes && !this.isPrayerEnabled(Prayer.INCREDIBLE_REFLEXES.id)) {
				this.enablePrayer(Prayer.INCREDIBLE_REFLEXES.id);
			} else if (this.useUltimateStrength && !this.isPrayerEnabled(Prayer.ULTIMATE_STRENGTH.id)) {
				this.enablePrayer(Prayer.ULTIMATE_STRENGTH.id);
			}

			return SLEEP_ONE_TICK;
		}

		if (this.getCurrentHits() <= this.eatThreshold) {
			final int foodIndex = this.getInventoryIndex(this.foodIds);

			if (foodIndex != -1) {
				return this.consume(foodIndex);
			}
		}

		final int[] blueDragon = this.getNpcById(NPC_ID_BLUE_DRAGON);

		if (blueDragon[0] != -1) {
			final int npcX = this.getNpcX(blueDragon[0]);
			final int npcY = this.getNpcY(blueDragon[0]);

			if (this.needToOpenGate(npcX, npcY)) {
				return this.openGate();
			}

			if (!this.isPrayerEnabled(Prayer.PARALYZE_MONSTER.id)) {
				this.enablePrayer(Prayer.PARALYZE_MONSTER.id);
			}

			this.attackNpc(blueDragon[0]);
			return SLEEP_ONE_TICK;
		}

		if (this.isPrayerEnabled(Prayer.PARALYZE_MONSTER.id)) {
			this.disablePrayer(Prayer.PARALYZE_MONSTER.id);
			return SLEEP_ONE_TICK;
		}

		if (this.isPrayerEnabled(Prayer.INCREDIBLE_REFLEXES.id)) {
			this.disablePrayer(Prayer.INCREDIBLE_REFLEXES.id);
			return SLEEP_ONE_TICK;
		}

		if (this.isPrayerEnabled(Prayer.ULTIMATE_STRENGTH.id)) {
			this.disablePrayer(Prayer.ULTIMATE_STRENGTH.id);
			return SLEEP_ONE_TICK;
		}

		if (this.buryBones) {
			final int bonesIndex = this.getInventoryIndex(ITEM_ID_DRAGON_BONES);

			if (bonesIndex != -1) {
				return this.consume(bonesIndex);
			}
		}

		this.updateGroundItem();

		if (this.groundItem[0] != -1) {
			if (this.isInventoryFull() &&
				(!inArray(ITEM_IDS_STACKABLE_LOOT, this.groundItem[0]) || !this.hasInventoryItem(this.groundItem[0]))) {
				return this.consume(this.getInventoryIndex(this.foodIds));
			}

			if (this.needToOpenGate(this.groundItem[1], this.groundItem[2])) {
				return this.openGate();
			}

			this.pickupItem(this.groundItem[0], this.groundItem[1], this.groundItem[2]);
			return SLEEP_ONE_TICK;
		}

		if (!this.isFullPrayer()) {
			this.setState(State.PRAY);
			return 0;
		}

		if (this.playerX != COORDINATE_BLUE_DRAGON_SPAWN.getX() ||
			this.playerY != COORDINATE_BLUE_DRAGON_SPAWN.getY()) {
			if (!Area.GUILD_CAGE.contains(this.playerX, this.playerY) && this.isGateClosed()) {
				return this.openGate();
			}

			this.walkTo(COORDINATE_BLUE_DRAGON_SPAWN.getX(), COORDINATE_BLUE_DRAGON_SPAWN.getY());
		}

		return SLEEP_ONE_TICK;
	}

	private int enterGuild() {
		if (this.playerX > Object.MEMBERS_GATE.coordinate.getX()) {
			if (this.distanceTo(Object.GUILD_DOOR.coordinate.getX(), Object.GUILD_DOOR.coordinate.getY()) <= 1) {
				this.atWallObject(Object.GUILD_DOOR.coordinate.getX(), Object.GUILD_DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.GUILD_DOOR.coordinate.getX(), Object.GUILD_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.playerY <= COORDINATE_LOAD_MEMBERS_GATE.getY()) {
			if (this.distanceTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY()) <= 1) {
				if (System.currentTimeMillis() <= this.gateTimeout) {
					return 0;
				}

				this.atObject(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
				this.gateTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			this.walkTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BANK.contains(this.playerX, this.playerY) &&
			this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) ==
				Object.BANK_DOORS.id) {
			this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(COORDINATE_LOAD_MEMBERS_GATE.getX(), COORDINATE_LOAD_MEMBERS_GATE.getY());
		return SLEEP_ONE_TICK;
	}

	private int pray() {
		if (Area.GUILD_CHAPEL.contains(this.playerX, this.playerY)) {
			if (this.isFullPrayer()) {
				this.setState(State.SLAY);
				return 0;
			}

			if (this.isPrayerEnabled(Prayer.PARALYZE_MONSTER.id)) {
				this.disablePrayer(Prayer.PARALYZE_MONSTER.id);
				return SLEEP_ONE_TICK;
			}

			if (this.isPrayerEnabled(Prayer.INCREDIBLE_REFLEXES.id)) {
				this.disablePrayer(Prayer.INCREDIBLE_REFLEXES.id);
				return SLEEP_ONE_TICK;
			}

			if (this.isPrayerEnabled(Prayer.ULTIMATE_STRENGTH.id)) {
				this.disablePrayer(Prayer.ULTIMATE_STRENGTH.id);
				return SLEEP_ONE_TICK;
			}

			this.atObject(Object.ALTAR.coordinate.getX(), Object.ALTAR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (Area.GUILD_ENTRANCE.contains(this.playerX, this.playerY)) {
			this.atObject(Object.GUILD_LADDER_UP.coordinate.getX(), Object.GUILD_LADDER_UP.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (Area.GUILD_CAGE.contains(this.playerX, this.playerY)) {
			if (this.inCombat()) {
				this.walkTo(Object.GUILD_CAGE_GATE.coordinate.getX(), Object.GUILD_CAGE_GATE.coordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			if (this.getCurrentHits() <= this.eatThreshold) {
				final int foodIndex = this.getInventoryIndex(this.foodIds);

				if (foodIndex != -1) {
					return this.consume(foodIndex);
				}
			}

			if (this.isGateClosed()) {
				return this.openGate();
			}

			this.walkTo(Object.GUILD_STAIRS_UP.coordinate.getX() + 1, Object.GUILD_STAIRS_UP.coordinate.getY() + 3);
			return SLEEP_ONE_TICK;
		}

		if (this.inCombat()) {
			this.walkTo(Object.GUILD_STAIRS_UP.coordinate.getX() + 1, Object.GUILD_STAIRS_UP.coordinate.getY() + 3);
			return SLEEP_ONE_TICK;
		}

		if (this.getCurrentHits() <= this.eatThreshold) {
			final int foodIndex = this.getInventoryIndex(this.foodIds);

			if (foodIndex != -1) {
				return this.consume(foodIndex);
			}
		}

		this.atObject(Object.GUILD_STAIRS_UP.coordinate.getX(), Object.GUILD_STAIRS_UP.coordinate.getY());
		return SLEEP_ONE_SECOND;
	}

	private int consume(final int inventoryIndex) {
		if (System.currentTimeMillis() <= this.consumeTimeout) {
			return 0;
		}

		this.useItem(inventoryIndex);
		this.consumeTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private void updatePaintLoot() {
		for (final int itemId : ITEM_IDS_PAINT_LOOT) {
			final int itemCount = this.getInventoryCount(itemId);

			if (itemCount == 0) {
				continue;
			}

			this.paintLoot.merge(itemId, itemCount, Integer::sum);
		}
	}

	private void updateGroundItem() {
		this.groundItem[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < this.getGroundItemCount(); index++) {
			final int groundItemId = this.getGroundItemId(index);

			if (!inArray(ITEM_IDS_LOOT, groundItemId)) {
				continue;
			}

			final int groundItemX = this.getItemX(index);

			if (groundItemX < Object.GUILD_STAIRS_UP.coordinate.getX()) {
				continue;
			}

			final int groundItemY = this.getItemY(index);

			final int distance = this.distanceTo(groundItemX, groundItemY);

			if (distance < currentDistance) {
				this.groundItem[0] = groundItemId;
				this.groundItem[1] = groundItemX;
				this.groundItem[2] = groundItemY;

				currentDistance = distance;
			}
		}
	}

	private int openGate() {
		if (System.currentTimeMillis() <= this.gateTimeout) {
			return 0;
		}

		this.atObject(Object.GUILD_CAGE_GATE.coordinate.getX(), Object.GUILD_CAGE_GATE.coordinate.getY());
		this.gateTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		return 0;
	}

	private int getCurrentHits() {
		return this.getCurrentLevel(SKILL_INDEX_HITS);
	}

	private boolean needToOpenGate(final int x, final int y) {
		if (!this.isGateClosed()) {
			return false;
		}

		return Area.GUILD_CAGE.contains(this.playerX, this.playerY) != Area.GUILD_CAGE.contains(x, y);
	}

	private boolean isGateClosed() {
		return this.getObjectIdFromCoords(Object.GUILD_CAGE_GATE.coordinate.getX(),
			Object.GUILD_CAGE_GATE.coordinate.getY()) == Object.GUILD_CAGE_GATE.id;
	}

	private boolean isFullPrayer() {
		return this.getCurrentLevel(SKILL_INDEX_PRAYER) == this.getLevel(SKILL_INDEX_PRAYER);
	}

	private boolean isInventoryFull() {
		return this.getInventoryCount() == MAX_INV_SIZE;
	}

	private boolean hasInventoryItem(final int[] itemIds) {
		for (final int itemId : itemIds) {
			for (int index = 0; index < this.getInventoryCount(); index++) {
				if (this.getInventoryId(index) == itemId) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean isItemIdEquipped(final int itemId) {
		for (int index = 0; index < this.getInventoryCount(); index++) {
			if (this.getInventoryId(index) == itemId) {
				return this.isItemEquipped(index);
			}
		}

		return false;
	}

	private void setState(final State state) {
		this.state = state;

		if (this.state == State.BANK) {
			this.updatePaintLoot();
		}
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
			return this.lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return this.upperBoundingCoordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
		}
	}
}
