/**
 * Mines mithril and/or adamantite at the Desert Mining Camp.
 * <p>
 * Requirements:
 * Tourist Trap quest completed. Start script at Shantay Pass.
 * Inventory: pickaxe, slave robe top, slave robe bottom, metal key, kiteshield (optional)
 * Bank: sleeping bag, coins, shantay passes (optional)
 * <p>
 * Optional Parameters:
 * --no-mithril
 * --no-adamantite
 * <p>
 *
 * @Author Chomp
 */
public class AA_DesertMiningCamp extends AA_Script {
	private static final Coordinate COORDINATE_BANK_CHEST = new Coordinate(58, 731);
	private static final Coordinate COORDINATE_LOAD_MINING_CAMP = new Coordinate(94, 766);
	private static final Coordinate COORDINATE_LOAD_BANK = new Coordinate(63, 775);
	private static final Coordinate COORDINATE_ENCLOSED_ADAMANTITE_ROCK = new Coordinate(50, 3604);
	private static final Coordinate COORDINATE_ENCLOSED_MITHRIL_ROCK = new Coordinate(54, 3604);

	private static final int[] OBJECT_IDS_ADAMANTITE_ROCK = new int[]{108, 109};
	private static final int[] OBJECT_IDS_MITHRIL_ROCK = new int[]{106, 107};
	private static final int[] ITEM_IDS_GEMS = new int[]{157, 158, 159, 160};

	private static final int COORDINATE_X_UNREACHABLE_ADAMANTITE_ROCK = 59;

	private static final int ITEM_ID_COINS = 10;
	private static final int ITEM_ID_MITHRIL_ORE = 153;
	private static final int ITEM_ID_ADAMANTITE_ORE = 154;
	private static final int ITEM_ID_SHANTAY_DISCLAIMER = 1099;
	private static final int ITEM_ID_SHANTAY_PASS = 1030;
	private static final int ITEM_ID_SLAVE_ROBE_BOTTOM = 1022;
	private static final int ITEM_ID_SLAVE_ROBE_TOP = 1023;
	private static final int ITEM_ID_METAL_KEY = 1021;
	private static final int ITEM_ID_WROUGHT_IRON_KEY = 1097;
	private static final int ITEM_ID_CELL_DOOR_KEY = 1098;

	private static final int NPC_ID_ROWDY_SLAVE = 718;
	private static final int NPC_ID_ASSISTANT = 720;

	private static final int MAXIMUM_SHANTAY_PASS_STOCK = 20;
	private static final int MINIMUM_SHANTAY_PASS_COST = 6;
	private static final int TOTAL_SHANTAY_PASS_COST = 139;

	private static final int SKILL_MINING_INDEX = 14;
	private static final int SHOP_INDEX_SHANTAY_PASS = 13;
	private static final int QUEST_INDEX_TOURIST_TRAP = 43;

	private final int[] nearestRock = new int[2];

	private long startTime;
	private Pickaxe pickaxe;
	private State state;

	private double initialMiningXp;

	private long clickTimeout;
	private long openBankTimeout;
	private long withdrawTimeout;

	private int playerX;
	private int playerY;

	private int mithrilOreCount;
	private int adamantiteOreCount;

	private boolean mithril = true;
	private boolean adamantite = true;

	public AA_DesertMiningCamp(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!isQuestComplete(QUEST_INDEX_TOURIST_TRAP)) {
			throw new IllegalStateException("Tourist Trap quest has not been completed");
		}

		if (!parameters.isEmpty()) {
			final String[] args = parameters.split(" ");

			for (final String arg : args) {
				switch (arg.toLowerCase()) {
					case "--no-mithril":
						mithril = false;
						break;
					case "--no-adamantite":
						adamantite = false;
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}
		}

		if (!mithril && !adamantite) {
			throw new IllegalArgumentException("Cannot exclude both mithril and adamantite!");
		}

		for (final Pickaxe pickaxe : Pickaxe.values()) {
			if (hasInventoryItem(pickaxe.id)) {
				this.pickaxe = pickaxe;
				break;
			}
		}

		if (pickaxe == null) {
			throw new IllegalStateException("Pickaxe missing from inventory");
		}

		if (!hasInventoryItem(ITEM_ID_SLAVE_ROBE_BOTTOM) ||
			!hasInventoryItem(ITEM_ID_SLAVE_ROBE_TOP)) {
			throw new IllegalStateException("Slave robes missing from inventory");
		}

		if (!hasInventoryItem(ITEM_ID_METAL_KEY)) {
			throw new IllegalStateException("Metal key missing from inventory");
		}

		setInitialState();
		initialMiningXp = getAccurateXpForLevel(SKILL_MINING_INDEX);
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		switch (state) {
			case ENTER_BANK:
				return enterBank();
			case ENTER_CAMP_L1:
				return enterCampL1();
			case ENTER_CAMP_L2:
				return enterCampL2();
			case ENTER_CAMP_L3:
				return enterCampL3();
			case ENTER_CAMP_L4:
				return enterCampL4();
			case ENTER_CAMP_MINE:
				return enterCampMine();
			case EXIT_BANK:
				return exitBank();
			case EXIT_CAMP_L1:
				return exitCampL1();
			case EXIT_CAMP_L2:
				return exitCampL2();
			case EXIT_CAMP_L3:
				return exitCampL3();
			case EXIT_CAMP_L4:
				return exitCampL4();
			case EXIT_CAMP_MINE:
				return exitCampMine();
			case MINE:
				return mine();
			case BANK:
				return bank();
			case SLEEP:
				return useSleepBag();
			case GET_MINE_KEY:
				return getWroughtIronKey();
			case BUY_SHANTAY_PASS:
				return buyShantayPass();
			default:
				throw new IllegalStateException("Invalid script state");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("You only") ||
			message.startsWith("There is") ||
			message.endsWith("iron key.") ||
			message.endsWith("moment") ||
			message.endsWith("behind you.")) {
			clickTimeout = 0;
		} else if (message.endsWith("mithril ore")) {
			mithrilOreCount++;
			clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("adamantite ore")) {
			adamantiteOreCount++;
			clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.startsWith("You just") || message.endsWith("ladder")) {
			clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("men.") || message.endsWith("you.")) {
			openBankTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		}
	}

	private int enterBank() {
		if (Area.BANK.contains(playerX, playerY)) {
			state = State.BANK;
			return 0;
		}

		if (playerY > COORDINATE_LOAD_BANK.getY()) {
			walkTo(COORDINATE_LOAD_BANK.getX(), COORDINATE_LOAD_BANK.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerX != Object.STONE_GATE.coordinate.getX() || playerY != Object.STONE_GATE.coordinate.getY() + 2) {
			walkTo(Object.STONE_GATE.coordinate.getX(), Object.STONE_GATE.coordinate.getY() + 2);
			return SLEEP_ONE_TICK;
		}

		atObject(Object.STONE_GATE.coordinate.getX(), Object.STONE_GATE.coordinate.getY());
		return SLEEP_THREE_SECONDS;
	}

	private int enterCampL1() {
		if (Area.CAMP_L1.contains(playerX, playerY)) {
			state = State.ENTER_CAMP_L2;
			return 0;
		}

		if (playerY < COORDINATE_LOAD_MINING_CAMP.getY()) {
			walkTo(COORDINATE_LOAD_MINING_CAMP.getX(), COORDINATE_LOAD_MINING_CAMP.getY());
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(Object.IRON_GATE.coordinate.getX(), Object.IRON_GATE.coordinate.getY()) > 1) {
			walkTo(Object.IRON_GATE.coordinate.getX() + 1, Object.IRON_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		final int slaveRobeTopIndex = getInventoryIndex(ITEM_ID_SLAVE_ROBE_TOP);

		if (isItemEquipped(slaveRobeTopIndex)) {
			removeItem(slaveRobeTopIndex);
			return SLEEP_ONE_TICK;
		}

		final int slaveRobeBottomIndex = getInventoryIndex(ITEM_ID_SLAVE_ROBE_BOTTOM);

		if (isItemEquipped(slaveRobeBottomIndex)) {
			removeItem(slaveRobeBottomIndex);
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= clickTimeout) {
			return 0;
		}

		atObject(Object.IRON_GATE.coordinate.getX(), Object.IRON_GATE.coordinate.getY());
		clickTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
		return 0;
	}

	private int enterCampL2() {
		if (Area.CAMP_L2.contains(playerX, playerY)) {
			state = State.ENTER_CAMP_L3;
			return 0;
		}

		if (!hasInventoryItem(ITEM_ID_WROUGHT_IRON_KEY)) {
			state = State.GET_MINE_KEY;
			return 0;
		}

		final int slaveRobeTopIndex = getInventoryIndex(ITEM_ID_SLAVE_ROBE_TOP);

		if (!isItemEquipped(slaveRobeTopIndex)) {
			wearItem(slaveRobeTopIndex);
			return SLEEP_ONE_TICK;
		}

		final int slaveRobeBottomIndex = getInventoryIndex(ITEM_ID_SLAVE_ROBE_BOTTOM);

		if (!isItemEquipped(slaveRobeBottomIndex)) {
			wearItem(slaveRobeBottomIndex);
			return SLEEP_ONE_TICK;
		}

		atObject(Object.CAMP_L1_WOODEN_DOORS.coordinate.getX(), Object.CAMP_L1_WOODEN_DOORS.coordinate.getY());
		return SLEEP_ONE_SECOND;
	}

	private int enterCampL3() {
		if (Area.CAMP_L3.contains(playerX, playerY)) {
			state = State.ENTER_CAMP_L4;
			return 0;
		}

		if (!inCombat()) {
			final int npcIndex = getBlockingNpc();

			if (npcIndex != -1) {
				attackNpc(npcIndex);
				return SLEEP_ONE_TICK;
			}
		}

		if (distanceTo(Object.CAMP_L2_MINING_CAVE.coordinate.getX(), Object.CAMP_L2_MINING_CAVE.coordinate.getY()) > 1 ||
			inCombat()) {
			walkTo(Object.CAMP_L2_MINING_CAVE.coordinate.getX() + 1, Object.CAMP_L2_MINING_CAVE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		atObject(Object.CAMP_L2_MINING_CAVE.coordinate.getX(), Object.CAMP_L2_MINING_CAVE.coordinate.getY());
		return SLEEP_ONE_SECOND;
	}

	private int enterCampL4() {
		if (Area.CAMP_L4.contains(playerX, playerY)) {
			state = State.ENTER_CAMP_MINE;
			return 0;
		}

		if (inCombat() ||
			playerX != Object.CAMP_L3_MINE_CART.coordinate.getX() ||
			playerY != Object.CAMP_L3_MINE_CART.coordinate.getY() - 1) {
			walkTo(Object.CAMP_L3_MINE_CART.coordinate.getX(), Object.CAMP_L3_MINE_CART.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (isQuestMenu()) {
			answer(0);
			clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
			return 0;
		}

		if (System.currentTimeMillis() <= clickTimeout) {
			return 0;
		}

		atObject2(Object.CAMP_L3_MINE_CART.coordinate.getX(), Object.CAMP_L3_MINE_CART.coordinate.getY());
		clickTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int enterCampMine() {
		if (Area.CAMP_MINE.contains(playerX, playerY)) {
			final int wroughtIronKeyIndex = getInventoryIndex(ITEM_ID_WROUGHT_IRON_KEY);

			if (wroughtIronKeyIndex != -1) {
				dropItem(wroughtIronKeyIndex);
				return SLEEP_ONE_SECOND;
			}

			state = State.MINE;
			return 0;
		}

		if (inCombat() ||
			distanceTo(Object.STURDY_IRON_GATE.coordinate.getX(), Object.STURDY_IRON_GATE.coordinate.getY()) > 1) {
			walkTo(Object.STURDY_IRON_GATE.coordinate.getX(), Object.STURDY_IRON_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		atWallObject(Object.STURDY_IRON_GATE.coordinate.getX(), Object.STURDY_IRON_GATE.coordinate.getY());
		return SLEEP_TWO_SECONDS;
	}

	private int exitBank() {
		if (!Area.BANK.contains(playerX, playerY)) {
			final int shantayDisclaimerIndex = getInventoryIndex(ITEM_ID_SHANTAY_DISCLAIMER);

			if (shantayDisclaimerIndex != -1) {
				dropItem(shantayDisclaimerIndex);
				return SLEEP_ONE_SECOND;
			}

			state = State.ENTER_CAMP_L1;
			return 0;
		}

		if (isQuestMenu()) {
			answer(0);
			clickTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= clickTimeout) {
			return 0;
		}

		atObject(Object.STONE_GATE.coordinate.getX(), Object.STONE_GATE.coordinate.getY());
		clickTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
		return 0;
	}

	private int exitCampL1() {
		if (!Area.CAMP_L1.contains(playerX, playerY)) {
			state = State.ENTER_BANK;
			return 0;
		}

		if (distanceTo(Object.IRON_GATE.coordinate.getX(), Object.IRON_GATE.coordinate.getY()) > 1) {
			walkTo(Object.IRON_GATE.coordinate.getX() - 1, Object.IRON_GATE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		final int slaveRobeTopIndex = getInventoryIndex(ITEM_ID_SLAVE_ROBE_TOP);

		if (isItemEquipped(slaveRobeTopIndex)) {
			removeItem(slaveRobeTopIndex);
			return SLEEP_ONE_TICK;
		}

		final int slaveRobeBottomIndex = getInventoryIndex(ITEM_ID_SLAVE_ROBE_BOTTOM);

		if (isItemEquipped(slaveRobeBottomIndex)) {
			removeItem(slaveRobeBottomIndex);
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= clickTimeout) {
			return 0;
		}

		atObject(Object.IRON_GATE.coordinate.getX(), Object.IRON_GATE.coordinate.getY());
		clickTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
		return 0;
	}

	private int exitCampL2() {
		if (Area.CAMP_L1.contains(playerX, playerY)) {
			state = State.EXIT_CAMP_L1;
			return 0;
		}

		if (!inCombat()) {
			final int npcIndex = getBlockingNpc();

			if (npcIndex != -1) {
				attackNpc(npcIndex);
				return SLEEP_ONE_TICK;
			}
		}

		if (distanceTo(Object.CAMP_L2_WOODEN_DOORS.coordinate.getX(), Object.CAMP_L2_WOODEN_DOORS.coordinate.getY()) > 1 ||
			inCombat()) {
			walkTo(Object.CAMP_L2_WOODEN_DOORS.coordinate.getX(), Object.CAMP_L2_WOODEN_DOORS.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		atObject(Object.CAMP_L2_WOODEN_DOORS.coordinate.getX(), Object.CAMP_L2_WOODEN_DOORS.coordinate.getY());
		return SLEEP_ONE_SECOND;
	}

	private int exitCampL3() {
		if (Area.CAMP_L2.contains(playerX, playerY)) {
			state = State.EXIT_CAMP_L2;
			return 0;
		}

		if (inCombat() ||
			distanceTo(Object.CAMP_L3_MINING_CAVE.coordinate.getX(), Object.CAMP_L3_MINING_CAVE.coordinate.getY()) > 1) {
			walkTo(Object.CAMP_L3_MINING_CAVE.coordinate.getX() - 1, Object.CAMP_L3_MINING_CAVE.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		atObject(Object.CAMP_L3_MINING_CAVE.coordinate.getX(), Object.CAMP_L3_MINING_CAVE.coordinate.getY());
		return SLEEP_ONE_SECOND;
	}

	private int exitCampL4() {
		if (Area.CAMP_L3.contains(playerX, playerY)) {
			state = State.EXIT_CAMP_L3;
			return 0;
		}

		if (inCombat() ||
			playerX != Object.CAMP_L4_MINE_CART.coordinate.getX() - 1 ||
			playerY != Object.CAMP_L4_MINE_CART.coordinate.getY()) {
			walkTo(Object.CAMP_L4_MINE_CART.coordinate.getX() - 1, Object.CAMP_L4_MINE_CART.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (isQuestMenu()) {
			answer(0);
			clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
			return 0;
		}

		if (System.currentTimeMillis() <= clickTimeout) {
			return 0;
		}

		atObject2(Object.CAMP_L4_MINE_CART.coordinate.getX(), Object.CAMP_L4_MINE_CART.coordinate.getY());
		clickTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int exitCampMine() {
		if (Area.CAMP_L4.contains(playerX, playerY)) {
			state = State.EXIT_CAMP_L4;
			return 0;
		}

		if (distanceTo(Object.STURDY_IRON_GATE.coordinate.getX(), Object.STURDY_IRON_GATE.coordinate.getY()) > 1) {
			walkTo(Object.STURDY_IRON_GATE.coordinate.getX(), Object.STURDY_IRON_GATE.coordinate.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		atWallObject(Object.STURDY_IRON_GATE.coordinate.getX(), Object.STURDY_IRON_GATE.coordinate.getY());
		return SLEEP_TWO_SECONDS;
	}

	private int mine() {
		if (getInventoryCount() == MAX_INV_SIZE) {
			final int gemIndex = getInventoryIndex(ITEM_IDS_GEMS);

			if (gemIndex != -1) {
				dropItem(gemIndex);
				return SLEEP_ONE_SECOND;
			}

			if (adamantite && mithril) {
				updateNearestRock(OBJECT_IDS_ADAMANTITE_ROCK);

				if (nearestRock[0] != -1) {
					final int mithrilOreIndex = getInventoryIndex(ITEM_ID_MITHRIL_ORE);

					if (mithrilOreIndex != -1) {
						dropItem(mithrilOreIndex);
						return SLEEP_ONE_SECOND;
					}
				}
			}

			state = State.EXIT_CAMP_MINE;
			return 0;
		}

		if (adamantite) {
			updateNearestRock(OBJECT_IDS_ADAMANTITE_ROCK);

			if (nearestRock[0] != -1) {
				if (nearestRock[0] == COORDINATE_ENCLOSED_ADAMANTITE_ROCK.getX() &&
					nearestRock[1] == COORDINATE_ENCLOSED_ADAMANTITE_ROCK.getY() &&
					(playerX != COORDINATE_ENCLOSED_ADAMANTITE_ROCK.getX() + 1 || playerY != COORDINATE_ENCLOSED_ADAMANTITE_ROCK.getY() + 1)) {
					walkTo(COORDINATE_ENCLOSED_ADAMANTITE_ROCK.getX() + 1, COORDINATE_ENCLOSED_ADAMANTITE_ROCK.getY() + 1);
					return SLEEP_ONE_TICK;
				}

				if (clickTimeout != 0 && System.currentTimeMillis() <= clickTimeout) {
					return 0;
				}

				atObject(nearestRock[0], nearestRock[1]);
				clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				return 0;
			}
		}

		if (mithril) {
			final int[] groundItemMithrilOre = getItemById(ITEM_ID_MITHRIL_ORE);

			if (groundItemMithrilOre[0] != -1) {
				pickupItem(ITEM_ID_MITHRIL_ORE, groundItemMithrilOre[1], groundItemMithrilOre[2]);
				return SLEEP_ONE_TICK;
			}

			updateNearestRock(OBJECT_IDS_MITHRIL_ROCK);

			if (nearestRock[0] != -1) {
				if (nearestRock[0] == COORDINATE_ENCLOSED_MITHRIL_ROCK.getX() &&
					nearestRock[1] == COORDINATE_ENCLOSED_MITHRIL_ROCK.getY() &&
					(playerX != COORDINATE_ENCLOSED_MITHRIL_ROCK.getX() + 1 || playerY != COORDINATE_ENCLOSED_MITHRIL_ROCK.getY() + 1)) {
					walkTo(COORDINATE_ENCLOSED_MITHRIL_ROCK.getX() + 1, COORDINATE_ENCLOSED_MITHRIL_ROCK.getY() + 1);
					return SLEEP_ONE_TICK;
				}

				if (clickTimeout != 0 && System.currentTimeMillis() <= clickTimeout) {
					return 0;
				}

				atObject(nearestRock[0], nearestRock[1]);
				clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				return 0;
			}
		}

		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (!isBanking()) {
			return useBankChest();
		}

		final int adamantiteOreInventoryCount = getInventoryCount(ITEM_ID_ADAMANTITE_ORE);

		if (adamantiteOreInventoryCount != 0) {
			deposit(ITEM_ID_ADAMANTITE_ORE, adamantiteOreInventoryCount);
			return SLEEP_ONE_TICK;
		}

		final int mithrilOreInventoryCount = getInventoryCount(ITEM_ID_MITHRIL_ORE);

		if (mithrilOreInventoryCount != 0) {
			deposit(ITEM_ID_MITHRIL_ORE, mithrilOreInventoryCount);
			return SLEEP_ONE_TICK;
		}

		final int shantayPassInventoryCount = getInventoryCount(ITEM_ID_SHANTAY_PASS);

		if (shantayPassInventoryCount != 1) {
			if (System.currentTimeMillis() <= withdrawTimeout) {
				return 0;
			}

			if (shantayPassInventoryCount == 0) {
				if (!hasBankItem(ITEM_ID_SHANTAY_PASS)) {
					state = State.BUY_SHANTAY_PASS;
					return 0;
				}

				withdraw(ITEM_ID_SHANTAY_PASS, 1);
				withdrawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			deposit(ITEM_ID_SHANTAY_PASS, shantayPassInventoryCount);
			return SLEEP_TWO_SECONDS;
		}

		final int coinInventoryCount = getInventoryCount(ITEM_ID_COINS);

		if (coinInventoryCount != 0) {
			deposit(ITEM_ID_COINS, coinInventoryCount);
			return SLEEP_ONE_TICK;
		}

		state = State.SLEEP;
		return 0;
	}

	private int useSleepBag() {
		final int sleepingBagIndex = getInventoryIndex(ITEM_ID_SLEEPING_BAG);

		if (getFatigue() != 0 && sleepingBagIndex != -1) {
			useItem(sleepingBagIndex);
			return SLEEP_ONE_SECOND;
		}

		if (!isBanking()) {
			return useBankChest();
		}

		if (getFatigue() == 0) {
			if (sleepingBagIndex == -1) {
				closeBank();
				state = State.EXIT_BANK;
				return SLEEP_ONE_TICK;
			}

			deposit(ITEM_ID_SLEEPING_BAG, 1);
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= withdrawTimeout) {
			return 0;
		}

		withdraw(ITEM_ID_SLEEPING_BAG, 1);
		withdrawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int getWroughtIronKey() {
		if (Area.CAMP_JAIL_DOWNSTAIRS.contains(playerX, playerY)) {
			if (hasInventoryItem(ITEM_ID_WROUGHT_IRON_KEY)) {
				if (getWallObjectIdFromCoords(Object.JAIL_DOOR.coordinate.getX(), Object.JAIL_DOOR.coordinate.getY()) == Object.JAIL_DOOR.id) {
					atWallObject(Object.JAIL_DOOR.coordinate.getX(), Object.JAIL_DOOR.coordinate.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(Object.CAMP_L1_WOODEN_DOORS.coordinate.getX() + 1, Object.CAMP_L1_WOODEN_DOORS.coordinate.getY());
				return SLEEP_ONE_TICK;
			}

			if (System.currentTimeMillis() <= clickTimeout) {
				return 0;
			}

			atObject(Object.JAIL_LADDER_UP.coordinate.getX(), Object.JAIL_LADDER_UP.coordinate.getY());
			clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (Area.CAMP_JAIL_UPSTAIRS.contains(playerX, playerY)) {
			final int cellDoorKeyIndex = getInventoryIndex(ITEM_ID_CELL_DOOR_KEY);

			if (cellDoorKeyIndex != -1) {
				dropItem(cellDoorKeyIndex);
				return SLEEP_ONE_SECOND;
			}

			if (hasInventoryItem(ITEM_ID_WROUGHT_IRON_KEY)) {
				if (System.currentTimeMillis() <= clickTimeout) {
					return 0;
				}

				atObject(Object.JAIL_LADDER_DOWN.coordinate.getX(), Object.JAIL_LADDER_DOWN.coordinate.getY());
				clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= clickTimeout) {
				return 0;
			}

			atObject2(Object.JAIL_DESK.coordinate.getX(), Object.JAIL_DESK.coordinate.getY());
			clickTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
			return 0;
		}

		if (hasInventoryItem(ITEM_ID_WROUGHT_IRON_KEY)) {
			state = State.ENTER_CAMP_L2;
			return 0;
		}

		if (getWallObjectIdFromCoords(Object.JAIL_DOOR.coordinate.getX(), Object.JAIL_DOOR.coordinate.getY()) == Object.JAIL_DOOR.id) {
			atWallObject(Object.JAIL_DOOR.coordinate.getX(), Object.JAIL_DOOR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(Object.JAIL_LADDER_UP.coordinate.getX(), Object.JAIL_LADDER_UP.coordinate.getY() + 1);
		return SLEEP_ONE_TICK;
	}

	private int buyShantayPass() {
		if (hasInventoryItem(ITEM_ID_SHANTAY_PASS)) {
			closeShop();
			state = State.BANK;
			return 0;
		}

		if (isShopOpen()) {
			final int shantayPassShopCount = getShopItemAmount(SHOP_INDEX_SHANTAY_PASS);

			if (shantayPassShopCount == MAXIMUM_SHANTAY_PASS_STOCK) {
				buyShopItem(SHOP_INDEX_SHANTAY_PASS, MAXIMUM_SHANTAY_PASS_STOCK);
				return SLEEP_TWO_SECONDS;
			}

			return 0;
		}

		if (!hasInventoryItem(ITEM_ID_COINS)) {
			if (!isBanking()) {
				return useBankChest();
			}

			if (System.currentTimeMillis() <= withdrawTimeout) {
				return 0;
			}

			final int coinBankCount = bankCount(ITEM_ID_COINS);

			if (coinBankCount < MINIMUM_SHANTAY_PASS_COST) {
				return exit("Out of coins. Cannot buy shantay passes.");
			}

			withdraw(ITEM_ID_COINS, TOTAL_SHANTAY_PASS_COST);
			withdrawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		return openShantayShop();
	}

	private int getBlockingNpc() {
		final int direction = bot.getMobDirection(bot.getPlayer());

		for (int index = 0; index < countNpcs(); index++) {
			if (getNpcId(index) != NPC_ID_ROWDY_SLAVE) {
				continue;
			}

			final int npcX = getNpcX(index);
			final int npcY = getNpcY(index);

			if (npcX == playerX &&
				((npcY == playerY - 1 && direction == 0) ||
					(npcY == playerY + 1 && direction == 4))) {
				return index;
			}

			if (npcY == playerY &&
				((npcX == playerX + 1 && direction == 2) ||
					(npcX == playerX - 1 && direction == 6))) {
				return index;
			}
		}

		return -1;
	}

	private void updateNearestRock(final int[] rockIds) {
		nearestRock[0] = -1;

		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < getObjectCount(); index++) {
			final int objectId = getObjectId(index);

			if (!inArray(rockIds, objectId)) {
				continue;
			}

			final int objectX = getObjectX(index);
			final int objectY = getObjectY(index);

			if (objectX == COORDINATE_X_UNREACHABLE_ADAMANTITE_ROCK) {
				continue;
			}

			final int distance = distanceTo(objectX, objectY);

			if (distance < currentDistance) {
				nearestRock[0] = objectX;
				nearestRock[1] = objectY;

				currentDistance = distance;
			}
		}
	}

	private int useBankChest() {
		if (System.currentTimeMillis() <= openBankTimeout) {
			return 0;
		}

		atObject(COORDINATE_BANK_CHEST.getX(), COORDINATE_BANK_CHEST.getY());
		openBankTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int openShantayShop() {
		if (isQuestMenu()) {
			answer(1);
			clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= clickTimeout) {
			return 0;
		}

		final int[] assistant = getNpcByIdNotTalk(NPC_ID_ASSISTANT);

		if (assistant[0] == -1) {
			return SLEEP_ONE_TICK;
		}

		talkToNpc(assistant[0]);
		clickTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
		return 0;
	}

	private void setInitialState() {
		playerX = getX();
		playerY = getY();

		if (Area.BANK.contains(playerX, playerY)) {
			state = State.BANK;
			return;
		}

		if (Area.CAMP_MINE.contains(playerX, playerY)) {
			state = State.MINE;
			return;
		}

		if (getInventoryCount() == MAX_INV_SIZE) {
			if (Area.CAMP_L4.contains(playerX, playerY)) {
				state = State.EXIT_CAMP_L4;
			} else if (Area.CAMP_L3.contains(playerX, playerY)) {
				state = State.EXIT_CAMP_L3;
			} else if (Area.CAMP_L2.contains(playerX, playerY)) {
				state = State.EXIT_CAMP_L2;
			} else if (Area.CAMP_L1.contains(playerX, playerY)) {
				state = State.EXIT_CAMP_L1;
			} else {
				state = State.ENTER_BANK;
			}
		} else {
			if (Area.CAMP_L4.contains(playerX, playerY)) {
				state = State.ENTER_CAMP_MINE;
			} else if (Area.CAMP_L3.contains(playerX, playerY)) {
				state = State.ENTER_CAMP_L4;
			} else if (Area.CAMP_L2.contains(playerX, playerY)) {
				state = State.ENTER_CAMP_L3;
			} else if (Area.CAMP_JAIL_DOWNSTAIRS.contains(playerX, playerY) ||
				Area.CAMP_JAIL_UPSTAIRS.contains(playerX, playerY)) {
				state = State.GET_MINE_KEY;
			} else if (Area.CAMP_L1.contains(playerX, playerY)) {
				state = State.ENTER_CAMP_L2;
			} else {
				state = State.ENTER_CAMP_L1;
			}
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Desert Mining Camp", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@State: @whi@%s", state),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		final double xpGained = getAccurateXpForLevel(SKILL_MINING_INDEX) - initialMiningXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (mithril) {
			drawString(String.format("@yel@Mithril: @whi@%s @cya@(@whi@%s ore@cya@/@whi@hr@cya@)",
					mithrilOreCount, toUnitsPerHour(mithrilOreCount, startTime)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}

		if (adamantite) {
			drawString(String.format("@yel@Adamantite: @whi@%s @cya@(@whi@%s ore@cya@/@whi@hr@cya@)",
					adamantiteOreCount, toUnitsPerHour(adamantiteOreCount, startTime)),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}
	}

	private enum State {
		ENTER_BANK("Enter Bank"),
		ENTER_CAMP_L1("Enter Camp L1"),
		ENTER_CAMP_L2("Enter Camp L2"),
		ENTER_CAMP_L3("Enter Camp L3"),
		ENTER_CAMP_L4("Enter Camp L4"),
		ENTER_CAMP_MINE("Enter Camp Mine"),
		EXIT_BANK("Exit Bank"),
		EXIT_CAMP_L1("Exit Camp L1"),
		EXIT_CAMP_L2("Exit Camp L2"),
		EXIT_CAMP_L3("Exit Camp L3"),
		EXIT_CAMP_L4("Exit Camp L4"),
		EXIT_CAMP_MINE("Exit Camp Mine"),
		MINE("Mine"),
		BANK("Bank"),
		SLEEP("Sleep"),
		GET_MINE_KEY("Get Mine Key"),
		BUY_SHANTAY_PASS("Buy Shantay Pass");

		private final String description;

		State(final String description) {
			this.description = description;
		}

		@Override
		public String toString() {
			return description;
		}
	}

	private enum Pickaxe {
		RUNE(1262),
		ADAMANTITE(1261),
		MITHRIL(1260),
		STEEL(1259),
		IRON(1258),
		BRONZE(156);

		private final int id;

		Pickaxe(final int id) {
			this.id = id;
		}
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(59, 722), new Coordinate(65, 734)),
		CAMP_JAIL_DOWNSTAIRS(new Coordinate(84, 799), new Coordinate(87, 803)),
		CAMP_JAIL_UPSTAIRS(new Coordinate(84, 1743), new Coordinate(89, 1747)),
		CAMP_L1(new Coordinate(80, 799), new Coordinate(91, 812)),
		CAMP_L2(new Coordinate(81, 3616), new Coordinate(92, 3644)),
		CAMP_L3(new Coordinate(56, 3634), new Coordinate(76, 3644)),
		CAMP_L4(new Coordinate(50, 3617), new Coordinate(71, 3633)),
		CAMP_MINE(new Coordinate(50, 3604), new Coordinate(58, 3616));

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
		JAIL_LADDER_UP(5, new Coordinate(86, 799)),
		JAIL_LADDER_DOWN(6, new Coordinate(86, 1743)),
		JAIL_DESK(1023, new Coordinate(86, 1746)),
		JAIL_DOOR(2, new Coordinate(85, 804)),
		CAMP_L1_WOODEN_DOORS(958, new Coordinate(81, 801)),
		CAMP_L2_WOODEN_DOORS(958, new Coordinate(81, 3633)),
		CAMP_L2_MINING_CAVE(963, new Coordinate(82, 3639)),
		CAMP_L3_MINING_CAVE(964, new Coordinate(77, 3639)),
		CAMP_L3_MINE_CART(976, new Coordinate(62, 3639)),
		CAMP_L4_MINE_CART(976, new Coordinate(56, 3631)),
		STURDY_IRON_GATE(200, new Coordinate(51, 3617)),
		STONE_GATE(916, new Coordinate(62, 733)),
		IRON_GATE(932, new Coordinate(92, 807));

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
