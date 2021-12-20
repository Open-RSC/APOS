import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
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
 * Author: Chomp
 */
public class AA_BlessedSpiders extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_FOOD = new Coordinate(727, 608);

	private static final int ITEM_ID_BOWL = 341;

	private static final int NPC_ID_BLESSED_SPIDER = 631;
	private static final int NPC_XP_BLESSED_SPIDER = 92;

	private static final int MINIMUM_HITS = 15;
	private static final int MAXIMUM_FATIGUE = 99;
	private static final int MAXIMUM_DISTANCE_FROM_NPC = 3;

	private static final int SKILL_INDEX_HITS = 3;

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();

	private Coordinate nextRespawn;
	private Instant startTime;

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

		this.setFightMode(this.combatStyle.getIndex());
		this.healing = this.getCurrentLevel(SKILL_INDEX_HITS) <= MINIMUM_HITS;
		this.initialCombatXp = this.getTotalCombatXp();
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		this.playerX = this.getX();
		this.playerY = this.getY();

		return this.healing ? this.heal() : this.kill();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("eat", 4)) {
			this.timeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Blessed Spiders", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

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

		final int kills = (int) xpGained / NPC_XP_BLESSED_SPIDER;

		this.drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, getUnitsPerHour(kills, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.nextRespawn != null) {
			this.drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)", this.nextRespawn.getX(), this.nextRespawn.getY()),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (this.extension.getNpcId(npc) != NPC_ID_BLESSED_SPIDER) {
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

	private int kill() {
		if (this.getY() < Object.RAILINGS.coordinate.getY()) {
			if (this.getCurrentLevel(SKILL_INDEX_HITS) <= MINIMUM_HITS) {
				this.healing = true;
				return 0;
			}

			if (this.inCombat()) {
				return 0;
			}

			if (this.getFatigue() >= MAXIMUM_FATIGUE) {
				return this.sleep();
			}

			final int[] blessedSpider = this.getNpcById(NPC_ID_BLESSED_SPIDER);

			if (blessedSpider[0] != -1) {
				final int npcX = this.getNpcX(blessedSpider[0]);
				final int npcY = this.getNpcY(blessedSpider[0]);

				if (this.distanceTo(npcX, npcY) > MAXIMUM_DISTANCE_FROM_NPC) {
					this.walkTo(npcX, npcY);
				} else {
					this.attackNpc(blessedSpider[0]);
				}

				return SLEEP_ONE_TICK;
			}

			if (this.nextRespawn != null &&
				(this.playerX != this.nextRespawn.getX() || this.playerY != this.nextRespawn.getY())) {
				this.walkTo(this.nextRespawn.getX(), this.nextRespawn.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (this.playerX == Object.RAILINGS.coordinate.getX() && this.playerY == Object.RAILINGS.coordinate.getY()) {
			return this.passRailings();
		}

		this.walkTo(Object.RAILINGS.coordinate.getX(), Object.RAILINGS.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	private int heal() {
		if (this.getY() >= Object.RAILINGS.coordinate.getY()) {
			final int bowlIndex = this.getInventoryIndex(ITEM_ID_BOWL);

			if (bowlIndex != -1) {
				this.dropItem(bowlIndex);
				return SLEEP_TWO_SECONDS;
			}

			if (this.getCurrentLevel(SKILL_INDEX_HITS) == this.getLevel(SKILL_INDEX_HITS)) {
				this.healing = false;
				return 0;
			}

			if (this.playerY < COORDINATE_LOAD_FOOD.getY()) {
				this.walkTo(COORDINATE_LOAD_FOOD.getX(), COORDINATE_LOAD_FOOD.getY());
				return SLEEP_ONE_TICK;
			}

			for (final GroundFood food : GroundFood.VALUES) {
				final int foodIndex = this.getInventoryIndex(food.id);

				if (foodIndex != -1) {
					if (System.currentTimeMillis() <= this.timeout) {
						return 0;
					}

					this.useItem(foodIndex);
					this.timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
					return 0;
				}

				if (!this.isItemAt(food.id, food.coordinate.getX(), food.coordinate.getY())) {
					continue;
				}

				this.pickupItem(food.id, food.coordinate.getX(), food.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (this.playerX == Object.RAILINGS.coordinate.getX() && this.playerY == Object.RAILINGS.coordinate.getY() - 1) {
			return this.passRailings();
		}

		this.walkTo(Object.RAILINGS.coordinate.getX(), Object.RAILINGS.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int passRailings() {
		if (this.isQuestMenu()) {
			this.answer(1);
			this.timeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= this.timeout) {
			return 0;
		}

		this.atWallObject2(Object.RAILINGS.coordinate.getX(), Object.RAILINGS.coordinate.getY());
		this.timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
		}
	}
}
