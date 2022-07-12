import java.util.HashMap;
import java.util.Map;

/**
 * A script for using a cannon.
 * Inventory: Sleeping Bag, Cballs, Cannon parts
 * <p>
 *
 * @author Chomp
 */
public class AA_Cannon extends AA_Script {
	private static final int MAXIMUM_FATIGUE = 95;

	private Coordinate idleCoord;
	private Coordinate cannonCoord;

	private double initialRangedXp;

	private long startTime;
	private long searchTimeout;
	private long setUpTimeout;

	private int cannonballIndex;
	private int initialCannonballCount;

	private boolean idle;

	public AA_Cannon(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (!isLoggedIn()) {
			throw new IllegalStateException("Must start script logged in.");
		}

		if (getInventoryIndex(ITEM_ID_SLEEPING_BAG) != 0) {
			throw new IllegalStateException("Sleeping bag missing from 1st inventory slot.");
		}

		cannonballIndex = getInventoryIndex(MultiCannon.ITEM_ID_CANNON_BALL);

		if (cannonballIndex != 1) {
			throw new IllegalStateException("Cannon balls missing from 2nd inventory slot.");
		}

		initialCannonballCount = getInventoryStack(cannonballIndex);
		cannonCoord = new Coordinate(getX(), getY());
		initialRangedXp = getAccurateXpForLevel(Skill.RANGED.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (getFatigue() >= MAXIMUM_FATIGUE) {
			final int index = getInventoryIndex(ITEM_ID_SLEEPING_BAG);

			if (index == -1) {
				System.err.println("Sleeping bag missing from inventory.");
				stopScript();
				return 0;
			}

			useItem(index);
			return SLEEP_ONE_SECOND;
		}

		if (idle) {
			return idle();
		}

		if (inCombat() || System.currentTimeMillis() <= searchTimeout) {
			return 0;
		}

		if (getX() != cannonCoord.getX() || getY() != cannonCoord.getY()) {
			walkTo(cannonCoord.getX(), cannonCoord.getY());
			return SLEEP_ONE_TICK;
		}

		final int objectId = getObjectIdFromCoords(cannonCoord.getX(), cannonCoord.getY());

		if (objectId == -1) {
			if (System.currentTimeMillis() <= setUpTimeout) {
				return 0;
			}

			if (outOfCannonBalls()) {
				System.err.println("Out of cannonballs.");
				stopScript();
				return 0;
			}

			final int baseIndex = getInventoryIndex(MultiCannon.FULL.nextPartId);

			if (baseIndex == -1) {
				System.err.println("Lost cannon base.");
				stopScript();
				return 0;
			}

			useItem(baseIndex);
			setUpTimeout = System.currentTimeMillis() + 1000L;
			return 0;
		}

		final MultiCannon multiCannon = MultiCannon.byId(objectId);

		switch (multiCannon) {
			case FULL:
				if (outOfCannonBalls()) {
					atObject2(cannonCoord.getX(), cannonCoord.getY());
					return 2000;
				}

				atObject(cannonCoord.getX(), cannonCoord.getY());
				searchTimeout = System.currentTimeMillis() + 2000L;
				return 0;
			case BASE:
			case STAND:
			case BARRELS:
				final int nextPartIndex = getInventoryIndex(multiCannon.nextPartId);

				if (nextPartIndex != -1) {
					useItemOnObject(nextPartIndex, cannonCoord.getX(), cannonCoord.getY());
				} else {
					atObject(cannonCoord.getX(), cannonCoord.getY());
				}

				return SLEEP_ONE_SECOND;
			default:
				System.err.println("An object is obstructing cannon placement.");
				stopScript();
				return 0;
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("targets")) {
			searchTimeout = System.currentTimeMillis() + 1000L;
		} else if (message.endsWith("ammo") || message.endsWith("target")) {
			searchTimeout = 0L;
		} else if (message.endsWith("ground")) {
			setUpTimeout = System.currentTimeMillis() + 3000L;
		} else if (message.endsWith("area")) {
			idleCoord = getWalkableCoordinate();
			idle = true;
		} else {
			super.onServerMessage(message);
		}
	}

	private int idle() {
		if (idleCoord == null || getX() != cannonCoord.getX() || getY() != cannonCoord.getY()) {
			idle = false;
			return 0;
		}

		walkTo(idleCoord.getX(), idleCoord.getY());
		return SLEEP_ONE_TICK;
	}

	private boolean outOfCannonBalls() {
		return getInventoryCount() <= cannonballIndex ||
			getInventoryId(cannonballIndex) != MultiCannon.ITEM_ID_CANNON_BALL;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Cannon", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getAccurateXpForLevel(Skill.RANGED.getIndex()) - initialRangedXp;

		bot.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		final int remaining = getInventoryStack(cannonballIndex);

		final int used = initialCannonballCount - remaining;

		bot.drawString(String.format("@yel@Cballs: @whi@%d @cya@(@whi@%s balls@cya@/@whi@hr@cya@)",
				used, toUnitsPerHour(used, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Remaining: @whi@%d", remaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Time remaining: @whi@%s", toTimeToCompletion(used, remaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum MultiCannon {
		FULL(943, 1032),
		BASE(946, 1033),
		STAND(947, 1034),
		BARRELS(948, 1035);

		private static final Map<Integer, MultiCannon> cannonPartById = new HashMap<>();

		private static final int ITEM_ID_CANNON_BALL = 1041;

		static {
			for (final MultiCannon multiCannon : MultiCannon.values()) {
				cannonPartById.put(multiCannon.objectId, multiCannon);
			}
		}

		private final int objectId;
		private final int nextPartId;

		MultiCannon(final int objectId, final int nextPartId) {
			this.objectId = objectId;
			this.nextPartId = nextPartId;
		}

		private static MultiCannon byId(final int id) {
			return cannonPartById.get(id);
		}
	}
}
