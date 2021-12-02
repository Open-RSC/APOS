import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Kills Druids at the Druidic Circle and banks at Falador West Bank.
 * <p>
 * Required:
 * Start script at Druids or Falador West Bank and have sleeping bag in inventory.
 * <p>
 * Optional Parameters:
 * -a,--alt <altName>
 * -f,--fightmode <controlled|attack|strength|defense> (default strength)
 * <p>
 * Notes:
 * Specifying names of alts will enable spawn synchronization between accounts via PM.
 * Alt accounts need to have each other added as friends.
 * e.g. -f attack -a bot02 -a bot03
 * <p>
 * Author: Chomp
 */
public class AA_DruidsCircle extends AA_Script {
	private static final Coordinate COORDINATE_LOAD_SOUTH_MEMBERS_GATE = new Coordinate(346, 479);
	private static final Coordinate COORDINATE_LOAD_NORTH_MEMBERS_GATE = new Coordinate(326, 544);
	private static final Coordinate COORDINATE_LOAD_FALADOR = new Coordinate(324, 512);

	private static final int[] ITEM_IDS_LOOT = new int[]{10, 31, 32, 34, 41, 42, 220, 165, 435, 436, 437, 438, 439, 440, 441, 442, 443, 566};
	private static final int[] ITEM_IDS_PREMIUM_LOOT = new int[]{41, 42, 220, 438, 439, 440, 441, 442, 443, 566};

	private static final int NPC_ID_DRUID = 200;
	private static final int NPC_XP_DRUID = 78;

	private static final int MAXIMUM_FATIGUE = 99;
	private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;

	private final int[] loot = new int[3];

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();
	private final Map<Integer, Integer> premiumLoot = new TreeMap<>();

	private String[] alts;
	private Spawn nextSpawn;
	private Iterator<Map.Entry<Integer, Spawn>> syncDataIterator;
	private String syncPlayerName;
	private Instant startTime;

	private double initialCombatXp;

	private long gateTimeout;
	private long syncRequestTimeout;

	private int playerX;
	private int playerY;

	private boolean syncWithAlt;

	public AA_DruidsCircle(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!parameters.isEmpty()) {
			List<String> alts = null;

			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-a":
					case "--alt":
						if (alts == null) {
							alts = new ArrayList<>();
						}

						final String altName = args[++i];

						if (!this.isFriend(altName)) {
							this.addFriend(altName);
						}

						alts.add(altName);
						break;
					case "-f":
					case "--fightmode":
						this.combatStyle = CombatStyle.valueOf(args[++i].toUpperCase());
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}

			if (alts != null) {
				this.alts = alts.toArray(new String[0]);
			}
		}

		this.setFightMode(this.combatStyle.getIndex());
		this.initialCombatXp = this.getTotalCombatXp();
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		this.playerX = this.getX();
		this.playerY = this.getY();

		return this.getInventoryCount() == MAX_INV_SIZE || this.isBanking() ? this.bank() : this.kill();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("gate") || message.endsWith("shut") || message.endsWith("open")) {
			this.gateTimeout = 0L;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Druids Circle", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

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

		final int kills = (int) xpGained / NPC_XP_DRUID;

		this.drawString(String.format("@yel@Kills: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				kills, getUnitsPerHour(kills, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.nextSpawn != null) {
			this.drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)",
					this.nextSpawn.getCoordinate().getX(), this.nextSpawn.getCoordinate().getY()),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}

		if (!this.premiumLoot.isEmpty()) {
			this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			for (final Map.Entry<Integer, Integer> entry : this.premiumLoot.entrySet()) {
				this.drawString(String.format("@gre@%s: @whi@%d", getItemNameId(entry.getKey()), entry.getValue()),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
			}
		}
	}

	@Override
	public void onPrivateMessage(final String message, final String playerName, final boolean moderator, final boolean administrator) {
		if (this.alts == null || !this.isAnAlt(playerName)) {
			return;
		}

		if (message.equalsIgnoreCase("sync")) {
			if (this.syncWithAlt || this.spawnMap.isEmpty()) {
				return;
			}

			this.syncWithAlt = true;
			this.syncPlayerName = playerName;
			this.syncDataIterator = new HashMap<>(this.spawnMap).entrySet().iterator();
		} else {
			final String[] syncData = message.split(",");
			final int serverIndex = Integer.parseInt(syncData[0]);

			if (this.spawnMap.containsKey(serverIndex)) {
				return;
			}

			final Coordinate coordinate = new Coordinate(Integer.parseInt(syncData[1]), Integer.parseInt(syncData[2]));
			final Spawn spawn = new Spawn(coordinate, Long.parseLong(syncData[3]));

			this.spawnMap.put(serverIndex, spawn);
			this.nextSpawn = this.getOldestSpawn();
		}
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		final int npcId = this.extension.getNpcId(npc);

		if (npcId != NPC_ID_DRUID) {
			return;
		}

		if (this.spawnMap.isEmpty() && this.alts != null) {
			this.requestSyncWithAlt();
		}

		final int npcX = this.extension.getMobLocalX(npc) + this.extension.getAreaX();
		final int npcY = this.extension.getMobLocalY(npc) + this.extension.getAreaY();

		if (!Area.DRUIDS.contains(npcX, npcY)) {
			return;
		}

		final int serverIndex = this.extension.getMobServerIndex(npc);

		final Spawn spawn = this.spawnMap.get(serverIndex);

		if (spawn != null) {
			spawn.getCoordinate().set(npcX, npcY);
			spawn.setTimestamp(System.currentTimeMillis());
		} else {
			this.spawnMap.put(serverIndex, new Spawn(new Coordinate(npcX, npcY), System.currentTimeMillis()));
		}

		this.nextSpawn = this.getOldestSpawn();
	}

	private int kill() {
		if (Area.DRUIDS.contains(this.playerX, this.playerY)) {
			return this.combatCycle();
		}

		if (this.playerX > Object.MEMBERS_GATE.coordinate.getX()) {
			if (!this.spawnMap.isEmpty()) {
				this.resetSpawns();

				if (this.alts != null) {
					this.resetSync();
				}
			}

			this.walkTo(Area.DRUIDS.lowerBoundingCoordinate.getX(), Area.DRUIDS.lowerBoundingCoordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.playerY <= COORDINATE_LOAD_NORTH_MEMBERS_GATE.getY()) {
			if (this.distanceTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
				if (System.currentTimeMillis() <= this.gateTimeout) {
					return 0;
				}

				this.atObject(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
				this.gateTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				return 0;
			}

			this.walkTo(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (Area.BANK.contains(this.playerX, this.playerY) &&
			this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(COORDINATE_LOAD_NORTH_MEMBERS_GATE.getX(), COORDINATE_LOAD_NORTH_MEMBERS_GATE.getY());
		return SLEEP_ONE_TICK;
	}

	private int combatCycle() {
		if (this.inCombat()) {
			if (this.syncWithAlt) {
				return this.syncWithAlt();
			}

			return 0;
		}

		if (this.getFatigue() >= MAXIMUM_FATIGUE) {
			return this.sleep();
		}

		final int[] druid = this.getNpcById(NPC_ID_DRUID);

		if (druid[0] != -1) {
			this.attackNpc(druid[0]);
			return SLEEP_ONE_TICK;
		}

		this.updateLoot();

		if (this.loot[0] != -1) {
			this.pickupItem(this.loot[0], this.loot[1], this.loot[2]);
			return SLEEP_ONE_TICK;
		}

		if (this.nextSpawn == null) {
			return 0;
		}

		final Coordinate spawnCoordinate = this.nextSpawn.getCoordinate();

		if (this.playerX != spawnCoordinate.getX() || this.playerY != spawnCoordinate.getY()) {
			if (this.alts != null && this.isSpawnCoordinateOccupied(spawnCoordinate)) {
				this.nextSpawn.setTimestamp(System.currentTimeMillis());
				this.nextSpawn = this.getOldestSpawn();
				return 0;
			}

			this.walkTo(spawnCoordinate.getX(), spawnCoordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.syncWithAlt) {
			return this.syncWithAlt();
		}

		return 0;
	}

	private int bank() {
		if (Area.BANK.contains(this.playerX, this.playerY)) {
			if (!this.isBanking()) {
				return this.openBank();
			}

			for (int index = 0; index < this.getInventoryCount(); index++) {
				final int itemId = this.getInventoryId(index);

				if (!inArray(ITEM_IDS_LOOT, itemId)) {
					continue;
				}

				this.deposit(itemId, this.getInventoryCount(itemId));
				return SLEEP_ONE_TICK;
			}

			this.updateBankLoot();
			this.closeBank();
			return SLEEP_ONE_SECOND;
		}

		if (this.playerX <= Object.MEMBERS_GATE.coordinate.getX()) {
			if (this.playerY >= COORDINATE_LOAD_FALADOR.getY()) {
				if (this.distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT &&
					this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
					this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				this.walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			this.walkTo(COORDINATE_LOAD_FALADOR.getX(), COORDINATE_LOAD_FALADOR.getY());
			return SLEEP_ONE_TICK;
		}

		if (this.playerY >= COORDINATE_LOAD_SOUTH_MEMBERS_GATE.getY()) {
			if (System.currentTimeMillis() <= this.gateTimeout) {
				return 0;
			}

			this.atObject(Object.MEMBERS_GATE.coordinate.getX(), Object.MEMBERS_GATE.coordinate.getY());
			this.gateTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		this.walkTo(COORDINATE_LOAD_SOUTH_MEMBERS_GATE.getX(), COORDINATE_LOAD_SOUTH_MEMBERS_GATE.getY());
		return SLEEP_ONE_TICK;
	}

	private void updateLoot() {
		this.loot[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < this.getGroundItemCount(); index++) {
			final int groundItemId = this.getGroundItemId(index);

			if (!inArray(ITEM_IDS_LOOT, groundItemId)) {
				continue;
			}

			final int groundItemX = this.getItemX(index);
			final int groundItemY = this.getItemY(index);

			if (!Area.DRUIDS.contains(groundItemX, groundItemY)) {
				continue;
			}

			final int distance = this.distanceTo(groundItemX, groundItemY);

			if (distance < currentDistance) {
				this.loot[0] = groundItemId;
				this.loot[1] = groundItemX;
				this.loot[2] = groundItemY;

				currentDistance = distance;
			}
		}
	}

	private Spawn getOldestSpawn() {
		if (this.spawnMap.isEmpty()) {
			return null;
		}

		return this.spawnMap.values().stream().sorted().findFirst().get();
	}

	private void requestSyncWithAlt() {
		if (System.currentTimeMillis() <= this.syncRequestTimeout) {
			return;
		}

		for (final String alt : this.alts) {
			this.sendPrivateMessage("sync", alt);
		}

		this.syncRequestTimeout = System.currentTimeMillis() + (TIMEOUT_TEN_SECONDS * 6);
	}

	private int syncWithAlt() {
		if (this.syncDataIterator.hasNext()) {
			final Map.Entry<Integer, Spawn> entry = this.syncDataIterator.next();

			final int serverIndex = entry.getKey();
			final Spawn spawn = entry.getValue();

			final Coordinate coordinate = spawn.getCoordinate();
			final long timestamp = spawn.getTimestamp();

			this.sendPrivateMessage(String.format("%d,%d,%d,%d", serverIndex, coordinate.getX(), coordinate.getY(), timestamp), this.syncPlayerName);
			return SLEEP_ONE_TICK;
		}

		this.resetSync();
		return 0;
	}

	private boolean isSpawnCoordinateOccupied(final Coordinate spawnCoordinate) {
		for (int index = 0; index < this.extension.getPlayerCount(); index++) {
			final String playerName = this.getPlayerName(index);

			if (this.isAnAlt(playerName) &&
				this.getPlayerX(index) == spawnCoordinate.getX() &&
				this.getPlayerY(index) == spawnCoordinate.getY()) {
				return true;
			}
		}

		return false;
	}

	private boolean isAnAlt(final String playerName) {
		for (final String alt : this.alts) {
			if (alt.equalsIgnoreCase(playerName)) {
				return true;
			}
		}

		return false;
	}

	private void resetSpawns() {
		this.spawnMap.clear();
		this.nextSpawn = null;
	}

	private void resetSync() {
		this.syncWithAlt = false;
		this.syncPlayerName = null;
		this.syncDataIterator = null;
	}

	private void updateBankLoot() {
		for (final int itemId : ITEM_IDS_PREMIUM_LOOT) {
			final int bankCount = this.bankCount(itemId);

			if (bankCount == 0) {
				continue;
			}

			this.premiumLoot.put(itemId, bankCount);
		}
	}

	private enum Area implements RSArea {
		DRUIDS(new Coordinate(350, 451), new Coordinate(375, 472)),
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
		MEMBERS_GATE(137, new Coordinate(341, 487)),
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
