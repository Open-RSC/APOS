import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A script for killing Fire Giants in the Wilderness.
 * Start script at Mage Arena Bank.
 * Modify the Equipment enum values in the script as required.
 * <p>
 * Optional Parameters:
 * -f <shrimp|...|lobster|shark> (default shark)
 * -c <controlled|attack|strength|defense> (default strength)
 * <p>
 * Keybinds:
 * p - toggle paint
 * <p>
 *
 * @Author Chomp
 */
public class AA_FireGiantsWilderness extends AA_Script {
	private static final Pattern PATTERN_PROJECTILE_SHOT = Pattern.compile("Warning! (.+) is shooting at you!");

	private static final Coordinate COORD_LUMBRIDGE = new Coordinate(120, 648);
	private static final Coordinate COORD_ICE_PLATEAU = new Coordinate(330, 160);
	private static final Coordinate COORD_LOAD_WEBS = new Coordinate(241, 117);
	private static final Coordinate COORD_LOAD_STAIRS = new Coordinate(289, 141);

	private static final int[] ITEM_IDS_LOOT = new int[]{
		31, 38, 40, 41, 42, 81, 93, 373, 398,
		403, 404, 405, 408, 413, 438, 439, 440, 441, 442, 443,
		518, 520, 523, 526, 527,
		615, 619, 795, 1092, 1277
	};
	private static final int[] ITEM_IDS_STACKABLE = new int[]{31, 38, 40, 41, 42, 619};

	private static final int ITEM_ID_BIG_BONES = 413;
	private static final int NPC_ID_FIREGIANT = 344;

	private static final int Y_COORD_DUNGEON = 2000;
	private static final int Y_COORD_BANK = 3000;
	private static final int X_COORD_WILD = 227;

	private static final int PAINT_LOOT_OFFSET_Y = PAINT_OFFSET_Y + (PAINT_OFFSET_Y_INCREMENT * 4);

	private static final int MAX_FATIGUE = 100;
	private static final int MAX_DIST = 18;

	private final Set<String> pkers = new HashSet<>();
	private final Map<Integer, Spawn> spawnMap = new HashMap<>();
	private final Map<Integer, Integer> lootMap = new TreeMap<>();

	private final int[] loot = new int[3];

	private int[] foodIds;

	private long startTime;
	private PathWalker pathWalker;
	private Coordinate nextSpawn;
	private State state;
	private Food food = Food.SHARK;
	private String attackers = "";

	private double initialCombatXp;

	private long actionTimeout;
	private long depositTimeout;
	private long withdrawTimeout;

	private int playerX;
	private int playerY;

	private int eatAt;

	private int attackCount;
	private int deathCount;

	private boolean attacked;
	private boolean died;

	private boolean paintLoot = true;

	public AA_FireGiantsWilderness(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-f":
						food = Food.valueOf(args[++i].toUpperCase());
						break;
					case "-c":
						combatStyle = CombatStyle.valueOf(args[++i].toUpperCase());
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		foodIds = new int[]{Food.LOBSTER.getId(), food.getId()};
		eatAt = getBaseHits() - food.getHealAmount();

		setCombatStyle(combatStyle.getIndex());

		if (hasInventoryItem(foodIds) && hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			setState(State.KILL);
		} else {
			setState(State.BANK);
		}

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

		switch (state) {
			case KILL:
				return kill();
			case BANK:
				return bank();
			case WALKBACK:
				return walkback();
			default:
				return exit("Invalid script state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("eat", 4) ||
			message.endsWith("it") ||
			message.endsWith("web") ||
			message.endsWith("lever") ||
			message.endsWith("shut") ||
			message.endsWith("open") ||
			message.endsWith("door")) {
			actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.startsWith("Warning")) {
			if (attacked) {
				return;
			}

			final Matcher matcher = PATTERN_PROJECTILE_SHOT.matcher(message);

			if (!matcher.matches()) {
				return;
			}

			final String playerName = matcher.group(1);

			setAttacked(playerName);
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void onDeath() {
		System.out.printf("[%s] Oh dear! You are dead...%n", this);

		pathWalker = new PathWalker(bot);
		pathWalker.init(null);

		final PathWalker.Path path = pathWalker.calcPath(COORD_LUMBRIDGE.getX(), COORD_LUMBRIDGE.getY(),
			COORD_ICE_PLATEAU.getX(), COORD_ICE_PLATEAU.getY());

		if (path == null) {
			exit("Failed to calculate path: Lumbridge -> Ice Plateau");
		} else {
			pathWalker.setPath(path);

			deathCount++;
			died = true;
			setState(State.WALKBACK);
		}
	}

	private void setAttacked(final String playerName) {
		System.out.printf("[%s] Attacked by: %s%n", this, playerName);

		if (!pkers.contains(playerName)) {
			pkers.add(playerName);
			attackers += playerName + " ";
		}

		attacked = true;
		attackCount++;

		setState(State.BANK);
	}

	private void setState(final State state) {
		this.state = state;

		if (this.state == State.BANK) {
			updateLootMap();
			resetSpawns();
		}
	}

	private void updateLootMap() {
		for (int index = 0; index < bot.getInventorySize(); index++) {
			if (bot.isEquipped(index)) {
				continue;
			}

			final int itemId = bot.getInventoryId(index);

			if (!inArray(ITEM_IDS_LOOT, itemId)) {
				continue;
			}

			final int count = bot.getInventoryStack(index);

			lootMap.merge(itemId, count, Integer::sum);
		}
	}

	private void resetSpawns() {
		spawnMap.clear();
		nextSpawn = null;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Fire Giants Wilderness", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@State: @whi@%s", state),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getTotalCombatXp() - initialCombatXp;

		bot.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (!paintLoot || lootMap.isEmpty()) {
			return;
		}

		y = PAINT_LOOT_OFFSET_Y;
		int index = 0;
		int xOffset = PAINT_OFFSET_X_LOOT;

		for (final Map.Entry<Integer, Integer> entry : lootMap.entrySet()) {
			final int itemId = entry.getKey();

			if (++index == 15) {
				xOffset = PAINT_OFFSET_X;
				y = PAINT_LOOT_OFFSET_Y;
			}

			bot.drawString(String.format("%s: @whi@%d", getItemNameId(itemId), entry.getValue()),
				xOffset, y += PAINT_OFFSET_Y_INCREMENT, 1, getItemColor(itemId));
		}
	}

	@Override
	public void onKeyPress(final KeyEvent keyEvent) {
		if (keyEvent.getKeyCode() == KeyEvent.VK_P) {
			paintLoot = !paintLoot;
			keyEvent.consume();
		}
	}

	@Override
	public void onPlayerDamaged(final java.lang.Object player) {
		if (player != bot.getPlayer() || !inCombat()) {
			return;
		}

		if (bot.getPlayerCount() > 1 && !attacked) {
			final java.lang.Object pker = getPker();

			if (pker != null) {
				final String playerName = getName(pker);
				setAttacked(playerName);
				return;
			}
		}

		setCombatStyle(combatStyle.getIndex());
	}

	@Override
	public void onNpcDamaged(final Object npc) {
		if (bot.getCombatStyle() == CombatStyle.DEFENSE.getIndex() ||
			!inCombat() ||
			getWaypointX(npc) != bot.getPlayerWaypointX() ||
			getWaypointY(npc) != bot.getPlayerWaypointY()) {
			return;
		}

		setCombatStyle(CombatStyle.DEFENSE.getIndex());
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (bot.getNpcId(npc) != NPC_ID_FIREGIANT || !FGArea.FIRE_GIANTS.contains(playerX, playerY)) {
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

		nextSpawn = spawnMap.isEmpty() ? null : spawnMap.values().stream().sorted().findFirst().get().getCoordinate();
	}

	private java.lang.Object getPker() {
		for (int index = 1; index < bot.getPlayerCount(); index++) {
			final java.lang.Object player = bot.getPlayer(index);

			if (!isPlayerInCombat(index) || getPlayerX(index) != playerX || getPlayerY(index) != playerY) {
				continue;
			}

			return player;
		}

		return null;
	}

	private static int getItemColor(final int itemId) {
		switch (itemId) {
			case 31:
			case 38:
			case 40:
			case 41:
			case 42:
			case 619:
				return 0xD2691E;
			case 81:
			case 93:
			case 398:
			case 403:
			case 404:
			case 405:
			case 408:
			case 1092:
				return 0x4682B4;
			case 438:
			case 439:
			case 440:
			case 441:
			case 442:
			case 443:
				return 0x6B8E23;
			case 523:
			case 526:
			case 527:
				return 0x663399;
			case 795:
			case 1277:
				return 0xB22222;
			default:
				return 0xDCDCDC;
		}
	}

	private int kill() {
		if (FGArea.FIRE_GIANTS.contains(playerX, playerY)) {
			if (inCombat()) {
				if (getCurrentHits() <= eatAt) {
					walkTo(playerX, playerY);
					return SLEEP_ONE_TICK;
				}

				return 0;
			}

			if (getCurrentHits() <= eatAt) {
				final int foodIndex = getInventoryIndex(foodIds);

				if (foodIndex == -1) {
					setState(State.BANK);
					return 0;
				}

				return consume(foodIndex);
			}

			final int bonesIndex = getInventoryIndex(ITEM_ID_BIG_BONES);

			if (bonesIndex != -1) {
				return consume(bonesIndex);
			}

			updateLoot();

			if (loot[0] != -1 && (loot[0] != Food.LOBSTER.getId() || !isInventoryFull())) {
				if (!isInventoryFull() || (inArray(ITEM_IDS_STACKABLE, loot[0]) &&
					hasInventoryItem(loot[0]))) {
					pickupItem(loot[0], loot[1], loot[2]);
					return SLEEP_ONE_TICK;
				}

				final int foodIndex = getInventoryIndex(foodIds);

				if (foodIndex != -1) {
					return consume(foodIndex);
				}
			}

			if (getFatigue() >= MAX_FATIGUE) {
				return sleep();
			}

			final int[] fireGiant = getNpcById(NPC_ID_FIREGIANT);

			if (fireGiant[0] != -1) {
				attackNpc(fireGiant[0]);
				return SLEEP_ONE_TICK;
			}

			if (nextSpawn != null && (playerX != nextSpawn.getX() || playerY != nextSpawn.getY())) {
				walkTo(nextSpawn.getX(), nextSpawn.getY());
				return SLEEP_ONE_TICK;
			}

			return 0;
		}

		if (getCurrentHits() <= eatAt && !inCombat()) {
			return eatOrBank();
		}

		if (FGArea.CHAOS_DWARVES.contains(playerX, playerY)) {
			final Coordinate gate = FGObject.GATE_WEST.getCoordinate();

			if (inCombat()) {
				walkTo(gate.getX(), gate.getY());
				return SLEEP_ONE_TICK;
			}

			if (getObjectIdFromCoords(gate.getX(), gate.getY()) == FGObject.GATE_WEST.getId()) {
				atObject(gate.getX(), gate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(gate.getX() + 1, gate.getY());
			return SLEEP_ONE_TICK;
		}

		if (FGArea.GIANTS.contains(playerX, playerY)) {
			final Coordinate gate = FGObject.GATE_EAST.getCoordinate();

			if (inCombat()) {
				walkTo(gate.getX(), gate.getY());
				return SLEEP_ONE_TICK;
			}

			if (getObjectIdFromCoords(gate.getX(), gate.getY()) == FGObject.GATE_EAST.getId()) {
				atObject(gate.getX(), gate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(gate.getX() + 1, gate.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerY > Y_COORD_BANK) {
			if (wieldEquipment()) {
				return SLEEP_ONE_TICK;
			}

			final Coordinate ladder = FGObject.LADDER_UP.getCoordinate();
			atObject(ladder.getX(), ladder.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerY > Y_COORD_DUNGEON) // Spiders
		{
			final Coordinate gate = FGObject.GATE_NORTH.getCoordinate();

			if (inCombat()) {
				walkTo(gate.getX(), gate.getY());
				return SLEEP_ONE_TICK;
			}

			if (getObjectIdFromCoords(gate.getX(), gate.getY()) == FGObject.GATE_NORTH.getId()) {
				atObject(gate.getX(), gate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(gate.getX() - 1, gate.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerX > X_COORD_WILD) {
			final Coordinate stairs = FGObject.STAIRS_DOWN.getCoordinate();

			if (inCombat()) {
				walkTo(stairs.getX(), stairs.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			if (distanceTo(stairs.getX(), stairs.getY()) <= MAX_DIST) {
				atObject(stairs.getX(), stairs.getY());
			} else {
				walkTo(stairs.getX(), stairs.getY() - 1);
			}

			return SLEEP_ONE_TICK;
		}

		if (inCombat()) {
			walkTo(playerX, playerY);
			return SLEEP_ONE_TICK;
		}

		if (playerY < FGObject.WEB_NORTH.getCoordinate().getY()) {
			final Coordinate door = FGObject.DOOR_NORTH.getCoordinate();

			if (getWallObjectIdFromCoords(door.getX(), door.getY()) == FGObject.DOOR_NORTH.getId()) {
				atWallObject(door.getX(), door.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(door.getX() + 1, door.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (playerY < FGObject.WEB_SOUTH.getCoordinate().getY()) {
			final Coordinate web = FGObject.WEB_NORTH.getCoordinate();

			if (getWallObjectIdFromCoords(web.getX(), web.getY()) == FGObject.WEB_NORTH.getId()) {
				return slashWeb(web);
			}

			final Coordinate door = FGObject.DOOR_NORTH.getCoordinate();

			walkTo(door.getX(), door.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerX >= FGObject.DOOR_SOUTH.getCoordinate().getX()) {
			Coordinate web = FGObject.WEB_SOUTH.getCoordinate();

			if (getWallObjectIdFromCoords(web.getX(), web.getY()) == FGObject.WEB_SOUTH.getId()) {
				return slashWeb(web);
			}

			web = FGObject.WEB_NORTH.getCoordinate();

			walkTo(web.getX(), web.getY());
			return SLEEP_ONE_TICK;
		}

		final Coordinate door = FGObject.DOOR_SOUTH.getCoordinate();

		if (getWallObjectIdFromCoords(door.getX(), door.getY()) == FGObject.DOOR_SOUTH.getId()) {
			atWallObject(door.getX(), door.getY());
			return SLEEP_ONE_SECOND;
		}

		final Coordinate web = FGObject.WEB_SOUTH.getCoordinate();

		walkTo(web.getX(), web.getY());
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (playerY > Y_COORD_BANK) {
			if (getCurrentHits() != getBaseHits()) {
				final int foodIndex = getInventoryIndex(foodIds);

				if (foodIndex != -1) {
					return consume(foodIndex);
				}
			}

			if (!isBanking()) {
				return openBank();
			}

			for (int index = 0; index < bot.getInventorySize(); index++) {
				final int itemId = bot.getInventoryId(index);

				if (Equipment.isEquipment(itemId)) {
					final int invCount = getInventoryCount(itemId);

					if (invCount == 1) {
						continue;
					}

					if (System.currentTimeMillis() <= depositTimeout) {
						return 0;
					}

					deposit(itemId, invCount - 1);
					depositTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
					return 0;
				}

				if (inArray(ITEM_IDS_LOOT, itemId)) {
					deposit(itemId, getInventoryCount(itemId));
					return SLEEP_ONE_TICK;
				}
			}

			for (final Equipment equipment : Equipment.VALUES) {
				if (hasInventoryItem(equipment.id)) {
					continue;
				}

				if (System.currentTimeMillis() <= equipment.timeout) {
					return 0;
				}

				if (!hasBankItem(equipment.id)) {
					continue;
				}

				withdraw(equipment.id, 1);
				equipment.timeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			if (!isInventoryFull()) {
				if (System.currentTimeMillis() <= withdrawTimeout) {
					return 0;
				}

				if (!hasBankItem(food.getId())) {
					return exit("Out of food.");
				}

				withdraw(food.getId(), getInventoryEmptyCount());
				withdrawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			setState(State.KILL);
			return 0;
		}

		if (getCurrentHits() <= eatAt && !inCombat()) {
			final int foodIndex = getInventoryIndex(foodIds);

			if (foodIndex != -1) {
				return consume(foodIndex);
			}
		}

		if (FGArea.FIRE_GIANTS.contains(playerX, playerY)) {
			Coordinate gate = FGObject.GATE_NORTH.getCoordinate();

			if (inCombat()) {
				walkTo(gate.getX() - 1, gate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			if (getObjectIdFromCoords(gate.getX(), gate.getY()) == FGObject.GATE_NORTH.getId()) {
				atObject(gate.getX(), gate.getY());
				return SLEEP_ONE_SECOND;
			}

			gate = FGObject.GATE_WEST.getCoordinate();
			walkTo(gate.getX() + 1, gate.getY());
			return SLEEP_ONE_TICK;
		}

		if (FGArea.CHAOS_DWARVES.contains(playerX, playerY)) {
			final Coordinate gate = FGObject.GATE_EAST.getCoordinate();

			if (inCombat()) {
				walkTo(gate.getX() + 1, gate.getY());
				return SLEEP_ONE_TICK;
			}

			if (getObjectIdFromCoords(gate.getX(), gate.getY()) == FGObject.GATE_EAST.getId()) {
				atObject(gate.getX(), gate.getY());
				return SLEEP_ONE_SECOND;
			}

			final Coordinate stairs = FGObject.STAIRS_UP.getCoordinate();
			walkTo(stairs.getX(), stairs.getY() + 3);
			return SLEEP_ONE_TICK;
		}

		if (FGArea.GIANTS.contains(playerX, playerY)) {
			final Coordinate stairs = FGObject.STAIRS_UP.getCoordinate();

			if (inCombat()) {
				walkTo(stairs.getX(), stairs.getY() + 3);
				return SLEEP_ONE_TICK;
			}

			atObject(stairs.getX(), stairs.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerY > Y_COORD_DUNGEON) // Spiders
		{
			Coordinate gate = FGObject.GATE_WEST.getCoordinate();

			if (inCombat()) {
				walkTo(gate.getX() + 1, gate.getY());
				return SLEEP_ONE_TICK;
			}

			if (getObjectIdFromCoords(gate.getX(), gate.getY()) == FGObject.GATE_WEST.getId()) {
				atObject(gate.getX(), gate.getY());
				return SLEEP_ONE_SECOND;
			}

			gate = FGObject.GATE_EAST.getCoordinate();
			walkTo(gate.getX() + 1, gate.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerX <= X_COORD_WILD) {
			if (inCombat()) {
				walkTo(playerX, playerY);
				return SLEEP_ONE_TICK;
			}

			if (playerX < FGObject.DOOR_SOUTH.getCoordinate().getX()) {
				final Coordinate ladder = FGObject.LADDER_DOWN.getCoordinate();
				atObject(ladder.getX(), ladder.getY());
				return SLEEP_ONE_TICK;
			}

			if (playerY >= FGObject.WEB_SOUTH.getCoordinate().getY()) {
				final Coordinate door = FGObject.DOOR_SOUTH.getCoordinate();

				if (getWallObjectIdFromCoords(door.getX(), door.getY()) == FGObject.DOOR_SOUTH.getId()) {
					atWallObject(door.getX(), door.getY());
					return SLEEP_ONE_SECOND;
				}

				final Coordinate ladder = FGObject.LADDER_DOWN.getCoordinate();
				atObject(ladder.getX(), ladder.getY());
				return SLEEP_ONE_TICK;
			}

			if (playerY >= FGObject.WEB_NORTH.getCoordinate().getY()) {
				final Coordinate web = FGObject.WEB_SOUTH.getCoordinate();

				if (getWallObjectIdFromCoords(web.getX(), web.getY()) == FGObject.WEB_SOUTH.getId()) {
					return slashWeb(web);
				}

				final Coordinate door = FGObject.DOOR_SOUTH.getCoordinate();
				walkTo(door.getX(), door.getY());
				return SLEEP_ONE_TICK;
			}

			if (playerY >= FGObject.DOOR_NORTH.getCoordinate().getY()) {
				Coordinate web = FGObject.WEB_NORTH.getCoordinate();

				if (getWallObjectIdFromCoords(web.getX(), web.getY()) == FGObject.WEB_NORTH.getId()) {
					return slashWeb(web);
				}

				web = FGObject.WEB_SOUTH.getCoordinate();
				walkTo(web.getX(), web.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			final Coordinate door = FGObject.DOOR_NORTH.getCoordinate();

			if (getWallObjectIdFromCoords(door.getX(), door.getY()) == FGObject.DOOR_NORTH.getId()) {
				atWallObject(door.getX(), door.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(door.getX(), door.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerX > COORD_LOAD_WEBS.getX()) {
			walkTo(COORD_LOAD_WEBS.getX(), COORD_LOAD_WEBS.getY());
			return SLEEP_ONE_TICK;
		}

		final Coordinate door = FGObject.DOOR_NORTH.getCoordinate();
		walkTo(door.getX(), door.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int walkback() {
		if (pathWalker != null) {
			if (playerY > COORD_ICE_PLATEAU.getY() && pathWalker.walkPath()) {
				return 0;
			}

			pathWalker = null;
		}

		final Coordinate gate = FGObject.GATE_ICE_PLATEAU.getCoordinate();

		if (playerY < gate.getY()) {
			if (playerX <= COORD_LOAD_STAIRS.getX()) {
				setState(State.BANK);
				return 0;
			}

			walkTo(COORD_LOAD_STAIRS.getX(), COORD_LOAD_STAIRS.getY());
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(gate.getX(), gate.getY()) <= 1) {
			atObject(gate.getX(), gate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(gate.getX(), gate.getY());
		return SLEEP_ONE_TICK;
	}

	private boolean wieldEquipment() {
		for (final Equipment equipment : Equipment.VALUES) {
			if (equipment == Equipment.SLEEPING_BAG) {
				continue;
			}

			final int equipmentIndex = getInventoryIndex(equipment.id);

			if (equipmentIndex != -1 && !bot.isEquipped(equipmentIndex)) {
				wearItem(equipmentIndex);
				return true;
			}
		}

		return false;
	}

	private void updateLoot() {
		loot[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < bot.getGroundItemCount(); index++) {
			final int groundItemId = bot.getGroundItemId(index);

			if (!inArray(ITEM_IDS_LOOT, groundItemId)) {
				continue;
			}

			final int groundItemX = getItemX(index);
			final int groundItemY = getItemY(index);

			if (!FGArea.FIRE_GIANTS.contains(groundItemX, groundItemY)) {
				continue;
			}

			final int distance = distanceTo(groundItemX, groundItemY);

			if (distance < currentDistance) {
				loot[0] = groundItemId;
				loot[1] = groundItemX;
				loot[2] = groundItemY;

				currentDistance = distance;
			}
		}
	}

	private int eatOrBank() {
		final int foodIndex = getInventoryIndex(foodIds);

		if (foodIndex == -1) {
			setState(State.BANK);
			return 0;
		}

		return consume(foodIndex);
	}

	private int consume(final int inventoryIndex) {
		if (System.currentTimeMillis() <= actionTimeout) {
			return 0;
		}

		useItem(inventoryIndex);
		actionTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private int slashWeb(final Coordinate web) {
		if (System.currentTimeMillis() <= actionTimeout) {
			return 0;
		}

		final int weaponIndex = getInventoryIndex(Equipment.WEAPON.id);

		if (weaponIndex == -1) {
			return exit("Weapon missing from inventory.");
		}

		useItemOnWallObject(weaponIndex, web.getX(), web.getY());
		actionTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private enum FGObject implements RSObject {
		LADDER_UP(1187, new Coordinate(446, 3367)),
		LADDER_DOWN(1188, new Coordinate(223, 110)),
		WEB_SOUTH(24, new Coordinate(227, 109)),
		WEB_NORTH(24, new Coordinate(227, 107)),
		DOOR_SOUTH(2, new Coordinate(226, 110)),
		DOOR_NORTH(2, new Coordinate(227, 106)),
		STAIRS_DOWN(42, new Coordinate(268, 128)),
		STAIRS_UP(41, new Coordinate(268, 2960)),
		GATE_EAST(57, new Coordinate(272, 2972)),
		GATE_WEST(57, new Coordinate(281, 2969)),
		GATE_NORTH(57, new Coordinate(274, 2952)),
		GATE_ICE_PLATEAU(346, new Coordinate(331, 142));

		private final int id;
		private final Coordinate coordinate;

		FGObject(final int id, final Coordinate coordinate) {
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

	private enum FGArea implements RSArea {
		GIANTS(new Coordinate(266, 2961), new Coordinate(272, 2974)),
		CHAOS_DWARVES(new Coordinate(273, 2967), new Coordinate(281, 2974)),
		FIRE_GIANTS(new Coordinate(264, 2947), new Coordinate(273, 2956));

		private final Coordinate lowerBoundingCoordinate;
		private final Coordinate upperBoundingCoordinate;

		FGArea(final Coordinate lowerBoundingCoordinate, final Coordinate upperBoundingCoordinate) {
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

	/**
	 * Modify these values as required
	 */
	private enum Equipment {
		SLEEPING_BAG(1263),
		HELMET(111),
		BODY(401),
		LEGS(402),
		SHIELD(131),
		WEAPON(594),
		AMULET(316);

		private static final Equipment[] VALUES = Equipment.values();

		private final int id;
		private long timeout;

		Equipment(final int id) {
			this.id = id;
		}

		private static boolean isEquipment(final int id) {
			for (final Equipment equipment : VALUES) {
				if (id == equipment.id) {
					return true;
				}
			}

			return false;
		}
	}

	private enum State {
		KILL("Kill"),
		BANK("Bank"),
		WALKBACK("Walkback");

		private final String desc;

		State(final String desc) {
			this.desc = desc;
		}

		@Override
		public String toString() {
			return desc;
		}
	}
}
