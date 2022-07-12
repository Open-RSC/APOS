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
 * <controlled|attack|strength|defense> (default strength)
 * <p>
 *
 * @Author Chomp
 */
public class AA_BlessedSpiders extends AA_Script {
	private static final Coordinate RAILINGS = new Coordinate(727, 607);
	private static final Coordinate LOAD_FOOD = new Coordinate(727, 608);

	private static final int ITEM_ID_BOWL = 341;

	private static final int NPC_ID_BLESSED_SPIDER = 631;
	private static final int NPC_XP_BLESSED_SPIDER = 92;

	private static final int MIN_HITS = 15;
	private static final int MAX_FATIGUE = 99;

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();

	private Coordinate nextRespawn;

	private double initCmbXp;

	private long startTime;
	private long timeout;

	private int playerX;
	private int playerY;

	private boolean healing;

	public AA_BlessedSpiders(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) combatStyle = CombatStyle.valueOf(parameters.toUpperCase());

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		setCombatStyle(combatStyle.getIndex());
		healing = getCurrentHits() <= MIN_HITS;
		initCmbXp = getTotalCombatXp();
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
		if (message.startsWith("eat", 4)) timeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
	}

	@Override
	protected int distanceTo(final Object mob) {
		return Math.abs(playerX - getX(mob)) + Math.abs(playerY - getY(mob));
	}

	private int heal() {
		if (playerY >= RAILINGS.getY()) {
			if (playerY < LOAD_FOOD.getY()) {
				walkTo(LOAD_FOOD.getX(), LOAD_FOOD.getY());
				return SLEEP_ONE_TICK;
			}

			final int lastIndex = bot.getInventorySize() - 1;
			final int itemId = getInventoryItemId(lastIndex);

			if (itemId == ITEM_ID_BOWL) {
				dropItem(lastIndex);
				return SLEEP_TWO_SECONDS;
			}

			if (getCurrentHits() == getBaseHits()) {
				healing = false;
				return 0;
			}

			for (final FoodSpawn foodSpawn : FoodSpawn.VALUES) {
				if (itemId == foodSpawn.getId()) {
					if (System.currentTimeMillis() <= timeout) return 0;
					useItem(lastIndex);
					timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
					return 0;
				}

				final Coordinate coord = foodSpawn.getCoordinate();
				if (!isItemAt(foodSpawn.id, coord.getX(), coord.getY())) continue;
				pickupItem(foodSpawn.id, coord.getX(), coord.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (playerX == RAILINGS.getX() && playerY == RAILINGS.getY() - 1) return passRailings();
		walkTo(RAILINGS.getX(), RAILINGS.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int kill() {
		if (playerY < RAILINGS.getY()) {
			if (getCurrentHits() <= MIN_HITS) {
				healing = true;
				return 0;
			}

			if (inCombat()) return 0;
			if (getFatigue() >= MAX_FATIGUE) return sleep();

			final Object spider = getNearestNpcNotInCombat(NPC_ID_BLESSED_SPIDER);

			if (spider != null) {
				if (isNearSwamp() && distanceTo(spider) > 1) {
					walkTo(spider);
				} else {
					attackNpc(spider);
				}

				return SLEEP_ONE_TICK;
			}

			if (nextRespawn != null && (playerX != nextRespawn.getX() || playerY != nextRespawn.getY())) {
				walkTo(nextRespawn.getX(), nextRespawn.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (playerX == RAILINGS.getX() && playerY == RAILINGS.getY()) return passRailings();
		walkTo(RAILINGS.getX(), RAILINGS.getY());
		return SLEEP_ONE_TICK;
	}

	private int passRailings() {
		if (isOptionMenuOpen()) {
			answerOptionMenu(1);
			timeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= timeout) return 0;
		atWallObject2(RAILINGS.getX(), RAILINGS.getY());
		timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	private boolean isNearSwamp() {
		return (playerX == 725 && playerY == 580) || (playerX == 726 && playerY == 581) ||
			(playerX == 728 && playerY == 582) || (playerX == 729 && playerY == 583);
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Blessed Spiders", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getTotalCombatXp() - initCmbXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		final int kills = (int) xpGained / NPC_XP_BLESSED_SPIDER;

		drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s spiders@cya@/@whi@hr@cya@)",
				kills, toUnitsPerHour(kills, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	@Override
	public void onNpcSpawned(final Object npc) {
		if (bot.getNpcId(npc) != NPC_ID_BLESSED_SPIDER) return;

		final int serverIndex = bot.getMobServerIndex(npc);

		final Spawn spawn = spawnMap.get(serverIndex);

		if (spawn != null) {
			spawn.getCoordinate().set(getX(npc), getY(npc));
			spawn.setTimestamp(System.currentTimeMillis());
		} else {
			spawnMap.put(serverIndex, new Spawn(new Coordinate(getX(npc), getY(npc)), System.currentTimeMillis()));
		}

		nextRespawn = spawnMap.isEmpty() ? null : spawnMap.values().stream().sorted().findFirst().get().getCoordinate();
	}

	private enum FoodSpawn implements RSObject {
		STEW(346, new Coordinate(742, 619)),
		COOKED_MEAT(132, new Coordinate(743, 625)),
		SALMON(357, new Coordinate(745, 626));

		private static final FoodSpawn[] VALUES = FoodSpawn.values();

		private final int id;
		private final Coordinate coordinate;

		FoodSpawn(final int id, final Coordinate coordinate) {
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
