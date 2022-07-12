import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Mines runite, adamanatite, mithril ore at the Hero's Guild.
 * Start script with sleeping bag, pickaxe, anti-dragonfire shield in inventory.
 * <p>
 * Features:
 * Add other accounts to friends list to sync mining actions.
 * </p>
 *
 * @Author Chomp
 */
public class AA_HerosGuildMiner extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_SOUTH_MEMBERS_GATE = new Coordinate(346, 479);
	private static final Coordinate COORDINATE_LOAD_NORTH_MEMBERS_GATE = new Coordinate(326, 544);
	private static final Coordinate COORDINATE_LOAD_FALADOR = new Coordinate(324, 512);
	private static final Coordinate COORDINATE_SAFE_SPOT = new Coordinate(363, 3280);

	private static final int[] ITEM_IDS_PICKAXE = new int[]{1262, 1261, 1260, 1259, 1258, 156};

	private static final int ITEM_ID_ANTI_DRAGON_SHIELD = 420;
	private static final int INITIAL_INVENTORY_SIZE = 3;
	private static final int TIME_TO_BANK = 1000 * 60 * 4;
	private static final int NPC_ID_BLUE_DRAGON = 202;
	private static final int MAXIMUM_FATIGUE = 97;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;
	private static final int MINIMUM_DISTANCE_FROM_DRAGON = 5;

	private final TimedRock[] timedRocks = EnumSet.allOf(Rock.class).stream().map(TimedRock::new).toArray(TimedRock[]::new);
	private final Map<Integer, Integer> minedOres = new HashMap<>(Rock.ORE_IDS.length + 1, 1.0f);

	private String myName;
	private long startTime;

	private long gateTimeout;
	private long mineTimeout;
	private long optionMenuTimeout;

	private int playerX;
	private int playerY;

	private boolean banking;

	public AA_HerosGuildMiner(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		final int shieldIndex = getInventoryIndex(ITEM_ID_ANTI_DRAGON_SHIELD);

		if (shieldIndex == -1) {
			throw new IllegalStateException("Anti-dragon shield missing from inventory.");
		}

		if (!isItemEquipped(shieldIndex)) {
			wearItem(shieldIndex);
		}

		if (!hasInventoryItem(ITEM_IDS_PICKAXE)) {
			throw new IllegalStateException("Pickaxe missing from inventory.");
		}

		for (final int oreId : Rock.ORE_IDS) {
			minedOres.put(oreId, getInventoryCount(oreId));
		}

		myName = bot.getPlayerName(bot.getPlayer());
		banking = isInventoryFull() || isBanking();
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		return banking ? bank() : mine();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("You only") || message.startsWith("There is")) {
			mineTimeout = System.currentTimeMillis() + 650L;
		} else if (message.endsWith("mithril ore")) {
			mineTimeout = System.currentTimeMillis() + 650L;
			minedOres.merge(Rock.MITHRIL.oreId, 1, Integer::sum);
		} else if (message.endsWith("adamantite ore")) {
			mineTimeout = System.currentTimeMillis() + 650L;
			minedOres.merge(Rock.ADAMANTITE.oreId, 1, Integer::sum);
		} else if (message.endsWith("runite ore")) {
			mineTimeout = System.currentTimeMillis() + 650L;
			minedOres.merge(Rock.RUNITE.oreId, 1, Integer::sum);
		} else if (message.endsWith("gate") || message.endsWith("shut") || message.endsWith("open")) {
			gateTimeout = 0L;
		} else {
			super.onServerMessage(message);
		}
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (!isBanking()) {
				return openBank();
			}

			if (getInventoryCount() == INITIAL_INVENTORY_SIZE) {
				closeBank();
				banking = false;
				return 0;
			}

			final int itemId = getInventoryId(INITIAL_INVENTORY_SIZE);
			final int itemCount = getInventoryCount(itemId);

			deposit(itemId, itemCount);
			return 650;
		}

		if (!Area.GUILD_BASEMENT.contains(playerX, playerY)) {
			if (playerX <= Object.MEMBERS_GATE.coordinate.getX()) {
				if (playerY >= COORDINATE_LOAD_FALADOR.getY()) {
					if (distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT &&
						getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
						atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
						return 1000;
					}

					walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
					return 650;
				}

				walkTo(COORDINATE_LOAD_FALADOR.getX(), COORDINATE_LOAD_FALADOR.getY());
				return 650;
			}

			if (playerY >= COORDINATE_LOAD_SOUTH_MEMBERS_GATE.getY()) {
				if (System.currentTimeMillis() <= gateTimeout) {
					return 0;
				}

				atObject(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
				gateTimeout = System.currentTimeMillis() + 5000L;
				return 0;
			}

			if (playerY >= Object.GUILD_DOOR.coordinate.getY()) {
				walkTo(COORDINATE_LOAD_SOUTH_MEMBERS_GATE.getX(), COORDINATE_LOAD_SOUTH_MEMBERS_GATE.getY());
				return 650;
			}

			atWallObject(Object.GUILD_DOOR.coordinate.getX(), Object.GUILD_DOOR.coordinate.getY());
			return 1000;
		}

		if (inCombat()) {
			walkTo(Object.GUILD_STAIRS_UP.coordinate.getX() + 1, Object.GUILD_STAIRS_UP.coordinate.getY() + 3);
			return 650;
		}

		atObject(Object.GUILD_STAIRS_UP.coordinate.getX(), Object.GUILD_STAIRS_UP.coordinate.getY());
		return 1000;
	}

	private int mine() {
		if (Area.GUILD_BASEMENT.contains(playerX, playerY)) {
			if (isInventoryFull()) {
				for (final TimedRock timedRock : timedRocks) {
					if (timedRock.rock != Rock.RUNITE) {
						continue;
					}

					if (timedRock.respawnTime - System.currentTimeMillis() > TIME_TO_BANK) {
						break;
					}

					final int mithrilOreIndex = getInventoryIndex(Rock.MITHRIL.oreId);

					if (mithrilOreIndex == -1) {
						break;
					}

					dropItem(mithrilOreIndex);
					return 1000;
				}

				resetTimedRocks();
				banking = true;
				return 0;
			}

			if (getFatigue() >= MAXIMUM_FATIGUE) {
				if (playerX > COORDINATE_SAFE_SPOT.getX()) {
					walkTo(COORDINATE_SAFE_SPOT.getX(), COORDINATE_SAFE_SPOT.getY());
					return 650;
				}

				return sleep();
			}

			final int[] blueDragon = getAllNpcById(NPC_ID_BLUE_DRAGON);

			updateTimedRocks();

			TimedRock nextRock = null;

			for (final TimedRock timedRock : timedRocks) {
				final Coordinate coordinate = timedRock.rock.adjacentCoordinate;

				if (blueDragon[0] != -1 &&
					distanceTo(blueDragon[1], blueDragon[2], coordinate.getX(), coordinate.getY()) <
						MINIMUM_DISTANCE_FROM_DRAGON) {
					continue;
				}

				nextRock = timedRock;
				break;
			}

			if (nextRock == null) {
				return 0;
			}

			final Coordinate coordinate = nextRock.rock.adjacentCoordinate;

			if (playerX != coordinate.getX() || playerY != coordinate.getY()) {
				walkTo(coordinate.getX(), coordinate.getY());
				return 650;
			}

			if (nextRock.spawned && isAllFriendsAtCoordinate(coordinate) && System.currentTimeMillis() > mineTimeout) {
				atObject(nextRock.rock.coordinate.getX(), nextRock.rock.coordinate.getY());
				mineTimeout = System.currentTimeMillis() + 5000L;
				return 0;
			}

			return 0;
		}

		if (Area.GUILD.contains(playerX, playerY)) {
			atObject(Object.GUILD_STAIRS_DOWN.coordinate.getX(), Object.GUILD_STAIRS_DOWN.coordinate.getY());
			return 1000;
		}

		if (playerX > Object.MEMBERS_GATE.coordinate.getX()) {
			if (distanceTo(Object.GUILD_DOOR.coordinate.getX(), Object.GUILD_DOOR.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				atWallObject(Object.GUILD_DOOR.coordinate.getX(), Object.GUILD_DOOR.coordinate.getY());
				return 1000;
			}

			walkTo(Object.GUILD_DOOR.coordinate.getX(), Object.GUILD_DOOR.coordinate.getY());
			return 650;
		}

		if (playerY <= COORDINATE_LOAD_NORTH_MEMBERS_GATE.getY()) {
			if (distanceTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (System.currentTimeMillis() <= gateTimeout) {
					return 0;
				}

				atObject(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
				gateTimeout = System.currentTimeMillis() + 5000L;
				return 0;
			}

			walkTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
			return 650;
		}

		if (Area.BANK.contains(playerX, playerY) &&
			getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return 1000;
		}

		walkTo(COORDINATE_LOAD_NORTH_MEMBERS_GATE.getX(), COORDINATE_LOAD_NORTH_MEMBERS_GATE.getY());
		return 650;
	}

	private void resetTimedRocks() {
		for (final TimedRock timedRock : timedRocks) {
			if (timedRock.rock == Rock.RUNITE) {
				continue;
			}

			timedRock.spawned = false;
			timedRock.respawnTime = Long.MAX_VALUE;
		}

		Arrays.sort(timedRocks);
	}

	private void updateTimedRocks() {
		boolean updated = false;

		for (final TimedRock timedRock : timedRocks) {
			final Coordinate coordinate = timedRock.rock.getCoordinate();

			if (getObjectIdFromCoords(coordinate.getX(), coordinate.getY()) == timedRock.rock.getId()) {
				if (!timedRock.spawned) {
					timedRock.spawned = true;
					timedRock.respawnTime = Long.MAX_VALUE;

					if (!updated) {
						updated = true;
					}
				}
			} else {
				if (timedRock.spawned) {
					timedRock.spawned = false;
					timedRock.respawnTime = System.currentTimeMillis() + (timedRock.rock.respawnTime * 1000);

					if (!updated) {
						updated = true;
					}
				}
			}
		}

		if (updated) {
			Arrays.sort(timedRocks);
		}
	}

	private boolean isAllFriendsAtCoordinate(final Coordinate coordinate) {
		for (int index = 0; index < bot.getPlayerCount(); index++) {
			final java.lang.Object player = bot.getPlayer(index);

			final String playerName = bot.getPlayerName(player);

			if (myName.equalsIgnoreCase(playerName)) {
				continue;
			}

			final int playerX = bot.getMobLocalX(player) + bot.getAreaX();
			final int playerY = bot.getMobLocalY(player) + bot.getAreaY();

			if (isAFriend(playerName)) {
				if (playerX != coordinate.getX() || playerY != coordinate.getY()) {
					return false;
				}
			} else {
				if (distanceTo(playerX, playerY, coordinate.getX(), coordinate.getY()) <= 1) {
					return true;
				}
			}
		}

		return true;
	}

	private boolean isAFriend(final String playerName) {
		for (int index = 0; index < getFriendCount(); index++) {
			final String friendName = getFriendName(index);

			if (friendName.equalsIgnoreCase(playerName)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Hero's Guild Miner", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		String status;
		long timeTillSpawn = 0;

		for (final TimedRock timedRock : timedRocks) {
			if (timedRock.spawned) {
				status = "ready";
			} else if (timedRock.respawnTime == Long.MAX_VALUE) {
				status = "n/a";
			} else {
				timeTillSpawn = timedRock.respawnTime - System.currentTimeMillis();

				if (timeTillSpawn < 0) {
					status = "respawning";
				} else {
					status = null;
				}
			}

			if (status != null) {
				drawString(String.format("%s rock: @whi@%s", timedRock.rock, status),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
			} else {
				drawString(String.format("%s rock: @whi@%02d:%02d", timedRock.rock, (timeTillSpawn / 1000) / 60, (timeTillSpawn / 1000) % 60),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
			}
		}

		drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		for (final int oreId : Rock.ORE_IDS) {
			drawString(String.format("@dre@%s: @whi@%d", getItemNameId(oreId), minedOres.get(oreId)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}
	}

	private enum Area implements RSArea {
		GUILD(new Coordinate(368, 434), new Coordinate(377, 440)),
		GUILD_BASEMENT(new Coordinate(352, 3270), new Coordinate(376, 3283)),
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
		GUILD_STAIRS_UP(41, new Coordinate(368, 3270)),
		GUILD_STAIRS_DOWN(42, new Coordinate(368, 438)),
		MEMBERS_GATE(137, new Coordinate(341, 487)),
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

	private enum Rock implements RSObject {
		RUNITE("@cya@Runite", 210, 409, 900, new Coordinate(353, 3277), new Coordinate(354, 3277)),
		ADAMANTITE("@gr2@Adamant", 108, 154, 220, new Coordinate(373, 3281), new Coordinate(373, 3280)),
		MITHRIL("@blu@Mithril", 106, 153, 110, new Coordinate(371, 3281), new Coordinate(371, 3280));

		private static final int[] ORE_IDS = Arrays.stream(Rock.values()).mapToInt(r -> r.oreId).toArray();
		private final String name;
		private final int id;
		private final int oreId;
		private final long respawnTime;
		private final Coordinate coordinate;
		private final Coordinate adjacentCoordinate;

		Rock(final String name, final int id, final int oreId, final long respawnTime, final Coordinate coordinate, final Coordinate adjacentCoordinate) {
			this.name = name;
			this.id = id;
			this.oreId = oreId;
			this.respawnTime = respawnTime;
			this.coordinate = coordinate;
			this.adjacentCoordinate = adjacentCoordinate;
		}

		@Override
		public String toString() {
			return name;
		}

		public String getName() {
			return name;
		}

		public int getId() {
			return id;
		}

		public Coordinate getCoordinate() {
			return coordinate;
		}

		public int getOreId() {
			return oreId;
		}

		public long getRespawnTime() {
			return respawnTime;
		}

		public Coordinate getAdjacentCoordinate() {
			return adjacentCoordinate;
		}
	}

	private static final class TimedRock implements Comparable<TimedRock> {
		private final Rock rock;
		private boolean spawned;
		private long respawnTime = Long.MAX_VALUE;

		public TimedRock(final Rock rock) {
			this.rock = rock;
		}

		@Override
		public int compareTo(final TimedRock timedRock) {
			if (!spawned && !timedRock.spawned) {
				return respawnTime == timedRock.respawnTime
					? Integer.compare(rock.id, timedRock.rock.id) * -1
					: respawnTime < timedRock.respawnTime
					? -1
					: 1;
			}

			if (spawned && timedRock.spawned) {
				return Integer.compare(rock.id, timedRock.rock.id) * -1;
			}

			return spawned ? -1 : 1;
		}
	}
}
