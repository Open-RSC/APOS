import java.util.HashMap;
import java.util.Map;

/**
 * Kills Blessed Spiders in Kalrag's Nest.
 * Collects food spawns near the Soulless to heal.
 * <p>
 * Required:
 * Start script at Blessed Spiders. Have sleeping bag.
 * <p>
 * Optional Parameter:
 * -f,--fightmode <controlled|attack|strength|defense> (default strength)
 * <p>
 *
 * @Author Chomp
 */
public class AA_BlessedSpiders extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_FOOD = new Coordinate(727, 608);

	private static final int ITEM_ID_BOWL = 341;

	private static final int NPC_ID_BLESSED_SPIDER = 631;
	private static final int NPC_XP_BLESSED_SPIDER = 92;

	private static final int MINIMUM_HITS = 15;
	private static final int MAXIMUM_FATIGUE = 99;
	private static final int MAXIMUM_DISTANCE_FROM_NPC = 3;

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();

	private Coordinate nextRespawn;
	private long startTime;

	private double initialCombatXp;

	private long timeout;

	private int playerX;
	private int playerY;

	private boolean healing;

	public AA_BlessedSpiders(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
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

		setCombatStyle(combatStyle.getIndex());
		healing = getCurrentHits() <= MINIMUM_HITS;
		initialCombatXp = getTotalCombatXp();
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		if (bot.getCombatStyle() != combatStyle.getIndex()) setCombatStyle(combatStyle.getIndex());

		return healing ? heal() : kill();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("eat", 4)) {
			timeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	private int heal() {
		if (getY() >= Object.RAILINGS.coordinate.getY()) {
			final int bowlIndex = getInventoryIndex(ITEM_ID_BOWL);

			if (bowlIndex != -1) {
				dropItem(bowlIndex);
				return SLEEP_TWO_SECONDS;
			}

			if (getCurrentHits() == getBaseHits()) {
				healing = false;
				return 0;
			}

			if (playerY < COORDINATE_LOAD_FOOD.getY()) {
				walkTo(COORDINATE_LOAD_FOOD.getX(), COORDINATE_LOAD_FOOD.getY());
				return SLEEP_ONE_TICK;
			}

			for (final GroundFood food : GroundFood.VALUES) {
				final int foodIndex = getInventoryIndex(food.id);

				if (foodIndex != -1) {
					if (System.currentTimeMillis() <= timeout) {
						return 0;
					}

					useItem(foodIndex);
					timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
					return 0;
				}

				if (!isItemAt(food.id, food.coordinate.getX(), food.coordinate.getY())) {
					continue;
				}

				pickupItem(food.id, food.coordinate.getX(), food.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (playerX == Object.RAILINGS.coordinate.getX() && playerY == Object.RAILINGS.coordinate.getY() - 1) {
			return passRailings();
		}

		walkTo(Object.RAILINGS.coordinate.getX(), Object.RAILINGS.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int kill() {
		if (getY() < Object.RAILINGS.coordinate.getY()) {
			if (getCurrentHits() <= MINIMUM_HITS) {
				healing = true;
				return 0;
			}

			if (inCombat()) {
				return 0;
			}

			if (getFatigue() >= MAXIMUM_FATIGUE) {
				return sleep();
			}

			final int[] blessedSpider = getNpcById(NPC_ID_BLESSED_SPIDER);

			if (blessedSpider[0] != -1) {
				final int npcX = getNpcX(blessedSpider[0]);
				final int npcY = getNpcY(blessedSpider[0]);

				if (distanceTo(npcX, npcY) > MAXIMUM_DISTANCE_FROM_NPC) {
					walkTo(npcX, npcY);
				} else {
					attackNpc(blessedSpider[0]);
				}

				return SLEEP_ONE_TICK;
			}

			if (nextRespawn != null &&
				(playerX != nextRespawn.getX() || playerY != nextRespawn.getY())) {
				walkTo(nextRespawn.getX(), nextRespawn.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (playerX == Object.RAILINGS.coordinate.getX() && playerY == Object.RAILINGS.coordinate.getY()) {
			return passRailings();
		}

		walkTo(Object.RAILINGS.coordinate.getX(), Object.RAILINGS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int passRailings() {
		if (isQuestMenu()) {
			answer(1);
			timeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= timeout) {
			return 0;
		}

		atWallObject2(Object.RAILINGS.coordinate.getX(), Object.RAILINGS.coordinate.getY());
		timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Blessed Spiders", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Pid: @whi@%d", bot.getMobServerIndex(bot.getPlayer())),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getTotalCombatXp() - initialCombatXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final int kills = (int) xpGained / NPC_XP_BLESSED_SPIDER;

		drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, toUnitsPerHour(kills, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (nextRespawn != null) {
			drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)", nextRespawn.getX(), nextRespawn.getY()),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (bot.getNpcId(npc) != NPC_ID_BLESSED_SPIDER) {
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

	private enum Object implements RSObject {
		RAILINGS(171, new Coordinate(727, 607));

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

	private enum GroundFood implements RSObject {
		STEW(346, new Coordinate(742, 619)),
		COOKED_MEAT(132, new Coordinate(743, 625)),
		SALMON(357, new Coordinate(745, 626));

		private static final GroundFood[] VALUES = GroundFood.values();

		private final int id;
		private final Coordinate coordinate;

		GroundFood(final int id, final Coordinate coordinate) {
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
