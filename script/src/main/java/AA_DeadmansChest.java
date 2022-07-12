import java.util.HashMap;
import java.util.Map;

/**
 * Kills pirates at the Brimhaven pub.
 * Uncerts lobster or swordfish certs for food.
 * <p>
 * Required:
 * Start script at the pub or near the certer.
 * Inventory: sleeping bag, lobster or swordfish certs.
 * <p>
 * Optional Parameters:
 * --min-hits <#> (default 50)
 * -f,--fightmode <controlled|attack|strength|defense> (default Strength)
 * <p>
 * Notes:
 * --min-hits is the hits to flee combat at.
 * <p>
 *
 * @Author Chomp
 */
public class AA_DeadmansChest extends AA_Script {
	private static final Coordinate COORDINATE_CERTER = new Coordinate(445, 680);
	private static final Coordinate COORDINATE_SLEEP = new Coordinate(452, 696);

	private static final int[] ITEM_IDS_LOOT = new int[]{1277, 1092, 526, 527, 42, 40, 41, 31, 33, 34, 11, 10};
	private static final int[] ITEM_IDS_STACKABLE = new int[]{10, 11, 31, 33, 34, 40, 41, 42};

	private static final int NPC_ID_SETH = 267;
	private static final int NPC_ID_PIRATE = 264;
	private static final int NPC_XP_PIRATE = 82;

	private static final int ITEM_ID_HALF_KEY_TEETH = 526;
	private static final int ITEM_ID_HALF_KEY_LOOP = 527;

	private static final int MAXIMUM_FATIGUE = 99;
	private static final int MINIMUM_COMBAT_LVL_NON_AGGRO = 61;

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();
	private final int[] loot = new int[3];

	private Cert cert;
	private Coordinate nextRespawn;
	private long startTime;

	private double initialCombatXp;

	private long timeout;

	private int playerX;
	private int playerY;

	private int healThreshold;

	private int minimumHits = 50;

	private boolean uncerting;
	private boolean aggro;

	public AA_DeadmansChest(final Extension extension) {
		super(extension);
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
					case "-f":
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

		for (final Cert cert : Cert.VALUES) {
			if (hasInventoryItem(cert.id)) {
				this.cert = cert;
				break;
			}
		}

		if (cert == null) {
			throw new IllegalStateException("Certs missing from inventory.");
		}

		System.out.printf("[%s] Using %ss and %s combat style.%n",
			this, getItemNameId(cert.id), combatStyle);

		setCombatStyle(combatStyle.getIndex());
		initialCombatXp = getTotalCombatXp();
		healThreshold = getBaseHits() - cert.healAmount;
		uncerting = !hasInventoryItem(cert.foodId);
		aggro = bot.getPlayerCombatLevel(bot.getPlayer()) < MINIMUM_COMBAT_LVL_NON_AGGRO;
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		if (bot.getCombatStyle() != combatStyle.getIndex()) setCombatStyle(combatStyle.getIndex());

		return uncerting ? uncert() : kill();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("eat", 4) || message.endsWith("shut") || message.endsWith("open")) {
			timeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.equals("You don't have that many certificates")) {
			exit("Out of certificates");
		} else {
			super.onServerMessage(message);
		}
	}

	private int uncert() {
		if (isNearCerter()) {
			if (hasInventoryItem(cert.foodId)) {
				uncerting = false;
				return 0;
			}

			return talkToCerter();
		}

		if (Area.TAVERN.contains(playerX, playerY)) {
			return exitTavern();
		}

		return walkToCerter();
	}

	private int kill() {
		if (inCombat()) {
			if (getCurrentHits() < minimumHits) {
				walkTo(playerX, playerY);
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (getCurrentHits() <= healThreshold) {
			final int foodIndex = getInventoryIndex(cert.foodId);

			if (foodIndex == -1) {
				uncerting = true;
				return 0;
			}

			return eat(foodIndex);
		}

		if (!Area.TAVERN.contains(playerX, playerY)) {
			return enterTavern();
		}

		if (!isDoorClosed()) {
			return closeDoor();
		}

		final int teethIndex = getInventoryIndex(ITEM_ID_HALF_KEY_TEETH);

		if (teethIndex != -1) {
			final int loopIndex = getInventoryIndex(ITEM_ID_HALF_KEY_LOOP);

			if (loopIndex != -1) {
				useItemWithItem(teethIndex, loopIndex);
				return SLEEP_ONE_TICK;
			}
		}

		updateLoot();

		if (loot[0] != -1) {
			if (!isInventoryFull() || canPickUpStackableLoot()) {
				pickupItem(loot[0], loot[1], loot[2]);
				return SLEEP_ONE_TICK;
			}

			final int foodIndex = getInventoryIndex(cert.foodId);

			if (foodIndex != -1) {
				return eat(foodIndex);
			}
		}

		if (getFatigue() >= MAXIMUM_FATIGUE) {
			if (aggro &&
				(playerX != COORDINATE_SLEEP.getX() || playerY != COORDINATE_SLEEP.getY())) {
				walkTo(COORDINATE_SLEEP.getX(), COORDINATE_SLEEP.getY());
				return SLEEP_ONE_TICK;
			}

			return sleep();
		}

		final int pirateIndex = getNearestPirateIndex();

		if (pirateIndex != -1) {
			attackNpc(pirateIndex);
			return SLEEP_ONE_TICK;
		}

		if (nextRespawn != null &&
			(playerX != nextRespawn.getX() || playerY != nextRespawn.getY())) {
			walkTo(nextRespawn.getX(), nextRespawn.getY());
			return SLEEP_ONE_TICK;
		}

		return 0;
	}

	private boolean isNearCerter() {
		return playerY <= COORDINATE_CERTER.getY();
	}

	private int talkToCerter() {
		final String certAmount = Cert.getCertAmount(getInventoryEmptyCount());

		if (certAmount == null) {
			return exit("Out of inventory space.");
		}

		if (isQuestMenu()) {
			int index;

			if ((index = getQuestMenuIndex("I have some certificates to trade in")) != -1 ||
				(index = getQuestMenuIndex(cert.name)) != -1 ||
				(index = getQuestMenuIndex(certAmount)) != -1) {
				answer(index);
				timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			}

			return 0;
		}

		if (System.currentTimeMillis() <= timeout) {
			return 0;
		}

		final int[] certer = getNpcByIdNotTalk(NPC_ID_SETH);

		if (certer[0] == -1) {
			return 0;
		}

		talkToNpc(certer[0]);
		timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int exitTavern() {
		if (isDoorClosed()) {
			if (inCombat()) {
				walkTo(Object.TAVERN_DOOR.coordinate.getX(), Object.TAVERN_DOOR.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			return openDoor();
		}

		walkTo(Object.TAVERN_DOOR.coordinate.getX() - 1, Object.TAVERN_DOOR.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int walkToCerter() {
		if (getCurrentHits() > minimumHits && !isDoorClosed() && isNearDoor()) {
			if (inCombat()) {
				walkTo(Object.TAVERN_DOOR.coordinate.getX() - 1,
					Object.TAVERN_DOOR.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			return closeDoor();
		}

		walkTo(COORDINATE_CERTER.getX(), COORDINATE_CERTER.getY());
		return SLEEP_ONE_TICK;
	}

	private int eat(final int foodIndex) {
		if (System.currentTimeMillis() <= timeout) {
			return 0;
		}

		useItem(foodIndex);
		timeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private int enterTavern() {
		final int doorX = Object.TAVERN_DOOR.coordinate.getX();
		final int doorY = Object.TAVERN_DOOR.coordinate.getY();

		if (inCombat() || distanceTo(doorX, doorY) > 1) {
			walkTo(doorX - 1, doorY);
			return SLEEP_ONE_TICK;
		}

		if (isDoorClosed()) {
			return openDoor();
		}

		walkTo(doorX, doorY);
		return SLEEP_ONE_TICK;
	}

	private boolean isDoorClosed() {
		return getWallObjectIdFromCoords(Object.TAVERN_DOOR.coordinate.getX(),
			Object.TAVERN_DOOR.coordinate.getY()) == Object.TAVERN_DOOR.id;
	}

	private int closeDoor() {
		if (System.currentTimeMillis() <= timeout) {
			return 0;
		}

		atWallObject2(Object.TAVERN_DOOR.coordinate.getX(), Object.TAVERN_DOOR.coordinate.getY());
		timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private void updateLoot() {
		loot[0] = -1;

		for (final int itemId : ITEM_IDS_LOOT) {
			for (int index = 0; index < getGroundItemCount(); index++) {
				final int groundItemId = getGroundItemId(index);

				if (groundItemId != itemId) {
					continue;
				}

				final int groundItemX = getItemX(index);
				final int groundItemY = getItemY(index);

				if (!Area.TAVERN.contains(groundItemX, groundItemY)) {
					continue;
				}

				loot[0] = groundItemId;
				loot[1] = groundItemX;
				loot[2] = groundItemY;
				return;
			}
		}
	}

	private boolean canPickUpStackableLoot() {
		return inArray(ITEM_IDS_STACKABLE, loot[0]) && hasInventoryItem(loot[0]);
	}

	private int getNearestPirateIndex() {
		int pirateIndex = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < bot.getNpcCount(); index++) {
			final java.lang.Object npc = bot.getNpc(index);

			if (bot.getNpcId(npc) != NPC_ID_PIRATE || bot.isMobInCombat(npc)) {
				continue;
			}

			final int npcX = bot.getMobLocalX(npc) + bot.getAreaX();
			final int npcY = bot.getMobLocalY(npc) + bot.getAreaY();

			if (!Area.TAVERN.contains(npcX, npcY)) {
				continue;
			}

			final int distance = distanceTo(npcX, npcY, playerX, playerY);

			if (distance >= currentDistance) {
				continue;
			}

			pirateIndex = index;
			currentDistance = distance;
		}

		return pirateIndex;
	}

	public int getQuestMenuIndex(final String option) {
		final String[] openMenyItems = questMenuOptions();

		for (int index = 0; index < questMenuCount(); index++) {
			if (openMenyItems[index].equals(option)) {
				return index;
			}
		}

		return -1;
	}

	private int openDoor() {
		if (System.currentTimeMillis() <= timeout) {
			return 0;
		}

		atWallObject(Object.TAVERN_DOOR.coordinate.getX(), Object.TAVERN_DOOR.coordinate.getY());
		timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private boolean isNearDoor() {
		return distanceTo(Object.TAVERN_DOOR.coordinate.getX(),
			Object.TAVERN_DOOR.coordinate.getY()) <= 1;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Deadman's Chest", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		final int kills = (int) xpGained / NPC_XP_PIRATE;

		drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, toUnitsPerHour(kills, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (nextRespawn != null) {
			drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)",
					nextRespawn.getX(), nextRespawn.getY()),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (bot.getNpcId(npc) != NPC_ID_PIRATE) {
			return;
		}

		final int npcX = bot.getMobLocalX(npc) + bot.getAreaX();
		final int npcY = bot.getMobLocalY(npc) + bot.getAreaY();

		if (!Area.TAVERN.contains(npcX, npcY)) {
			return;
		}

		final int serverIndex = bot.getMobServerIndex(npc);

		final Spawn spawn = spawnMap.get(serverIndex);

		if (spawn != null) {
			spawn.getCoordinate().set(npcX, npcY);
			spawn.setTimestamp(System.currentTimeMillis());
		} else {
			spawnMap.put(serverIndex, new Spawn(new Coordinate(npcX, npcY), System.currentTimeMillis()));
		}

		nextRespawn = spawnMap.isEmpty() ?
			null : spawnMap.values().stream().sorted().findFirst().get().getCoordinate();
	}

	private enum Cert {
		LOBSTER(533, 373, 12, "Lobster"),
		SWORDFISH(535, 370, 14, "Swordfish");

		private static final Cert[] VALUES = Cert.values();

		private final int id;
		private final int foodId;
		private final int healAmount;

		private final String name;

		Cert(final int id, final int foodId, final int healAmount, final String name) {
			this.id = id;
			this.foodId = foodId;
			this.healAmount = healAmount;
			this.name = name;
		}

		private static String getCertAmount(final int inventorySpaces) {
			final int amount = inventorySpaces / 5;

			switch (amount) {
				case 0:
					return null;
				case 1:
					return "One";
				case 2:
					return "two";
				case 3:
					return "Three";
				case 4:
					return "four";
				default:
					return "five";
			}
		}
	}

	private enum Area implements RSArea {
		TAVERN(new Coordinate(449, 696), new Coordinate(454, 706)),
		BRIMHAVEN(new Coordinate(444, 694), new Coordinate(459, 709));

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
		TAVERN_DOOR(2, new Coordinate(449, 699));

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
