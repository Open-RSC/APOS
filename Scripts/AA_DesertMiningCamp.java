import java.awt.Font;
import java.time.Duration;
import java.time.Instant;

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
 * Author: Chomp
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

    private Instant startTime;
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
        if (!this.isQuestComplete(QUEST_INDEX_TOURIST_TRAP)) {
            throw new IllegalStateException("Tourist Trap quest has not been completed");
        }

        if (!parameters.isEmpty()) {
            final String[] args = parameters.split(" ");

            for (final String arg : args) {
                switch (arg.toLowerCase()) {
                    case "--no-mithril":
                        this.mithril = false;
                        break;
                    case "--no-adamantite":
                        this.adamantite = false;
                        break;
                    default:
                        throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
                }
            }
        }

        if (!this.mithril && !this.adamantite) {
            throw new IllegalArgumentException("Cannot exclude both mithril and adamantite!");
        }

        for (final Pickaxe pickaxe : Pickaxe.values()) {
            if (this.hasInventoryItem(pickaxe.id)) {
                this.pickaxe = pickaxe;
                break;
            }
        }

        if (this.pickaxe == null) {
            throw new IllegalStateException("Pickaxe missing from inventory");
        }

        if (!this.hasInventoryItem(ITEM_ID_SLAVE_ROBE_BOTTOM) ||
                !this.hasInventoryItem(ITEM_ID_SLAVE_ROBE_TOP)) {
            throw new IllegalStateException("Slave robes missing from inventory");
        }

        if (!this.hasInventoryItem(ITEM_ID_METAL_KEY)) {
            throw new IllegalStateException("Metal key missing from inventory");
        }

        this.setInitialState();
        this.initialMiningXp = this.getAccurateXpForLevel(SKILL_MINING_INDEX);
        this.startTime = Instant.now();
    }

    @Override
    public int main() {
        this.playerX = this.getX();
        this.playerY = this.getY();

        switch (this.state) {
            case ENTER_BANK:
                return this.enterBank();
            case ENTER_CAMP_L1:
                return this.enterCampL1();
            case ENTER_CAMP_L2:
                return this.enterCampL2();
            case ENTER_CAMP_L3:
                return this.enterCampL3();
            case ENTER_CAMP_L4:
                return this.enterCampL4();
            case ENTER_CAMP_MINE:
                return this.enterCampMine();
            case EXIT_BANK:
                return this.exitBank();
            case EXIT_CAMP_L1:
                return this.exitCampL1();
            case EXIT_CAMP_L2:
                return this.exitCampL2();
            case EXIT_CAMP_L3:
                return this.exitCampL3();
            case EXIT_CAMP_L4:
                return this.exitCampL4();
            case EXIT_CAMP_MINE:
                return this.exitCampMine();
            case MINE:
                return this.mine();
            case BANK:
                return this.bank();
            case SLEEP:
                return this.useSleepBag();
            case GET_MINE_KEY:
                return this.getWroughtIronKey();
            case BUY_SHANTAY_PASS:
                return this.buyShantayPass();
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
            this.clickTimeout = 0;
        } else if (message.endsWith("mithril ore")) {
            this.mithrilOreCount++;
            this.clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
        } else if (message.endsWith("adamantite ore")) {
            this.adamantiteOreCount++;
            this.clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
        } else if (message.startsWith("You just") || message.endsWith("ladder")) {
            this.clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
        } else if (message.endsWith("men.") || message.endsWith("you.")) {
            this.openBankTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
        }
    }

    @Override
    public void paint() {
        int y = PAINT_OFFSET_Y;

        this.drawString("@yel@Desert Mining Camp", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

        if (this.startTime == null) {
            return;
        }

        final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

        this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString(String.format("@yel@State: @whi@%s", this.state),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        final double xpGained = this.getAccurateXpForLevel(SKILL_MINING_INDEX) - this.initialMiningXp;

        this.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
                        DECIMAL_FORMAT.format(xpGained), getUnitsPerHour(xpGained, secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        if (this.mithril) {
            this.drawString(String.format("@yel@Mithril: @whi@%s @cya@(@whi@%s ore@cya@/@whi@hr@cya@)",
                            this.mithrilOreCount, getUnitsPerHour(this.mithrilOreCount, secondsElapsed)),
                    PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
        }

        if (this.adamantite) {
            this.drawString(String.format("@yel@Adamantite: @whi@%s @cya@(@whi@%s ore@cya@/@whi@hr@cya@)",
                            this.adamantiteOreCount, getUnitsPerHour(this.adamantiteOreCount, secondsElapsed)),
                    PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
        }
    }

    private int enterBank() {
        if (Area.BANK.contains(this.playerX, this.playerY)) {
            this.state = State.BANK;
            return 0;
        }

        if (this.playerY > COORDINATE_LOAD_BANK.getY()) {
            this.walkTo(COORDINATE_LOAD_BANK.getX(), COORDINATE_LOAD_BANK.getY());
            return SLEEP_ONE_TICK;
        }

        if (this.playerX != Object.STONE_GATE.coordinate.getX() || this.playerY != Object.STONE_GATE.coordinate.getY() + 2) {
            this.walkTo(Object.STONE_GATE.coordinate.getX(), Object.STONE_GATE.coordinate.getY() + 2);
            return SLEEP_ONE_TICK;
        }

        this.atObject(Object.STONE_GATE.coordinate.getX(), Object.STONE_GATE.coordinate.getY());
        return SLEEP_THREE_SECONDS;
    }

    private int enterCampL1() {
        if (Area.CAMP_L1.contains(this.playerX, this.playerY)) {
            this.state = State.ENTER_CAMP_L2;
            return 0;
        }

        if (this.playerY < COORDINATE_LOAD_MINING_CAMP.getY()) {
            this.walkTo(COORDINATE_LOAD_MINING_CAMP.getX(), COORDINATE_LOAD_MINING_CAMP.getY());
            return SLEEP_ONE_TICK;
        }

        if (this.distanceTo(Object.IRON_GATE.coordinate.getX(), Object.IRON_GATE.coordinate.getY()) > 1) {
            this.walkTo(Object.IRON_GATE.coordinate.getX() + 1, Object.IRON_GATE.coordinate.getY());
            return SLEEP_ONE_TICK;
        }

        final int slaveRobeTopIndex = this.getInventoryIndex(ITEM_ID_SLAVE_ROBE_TOP);

        if (this.isItemEquipped(slaveRobeTopIndex)) {
            this.removeItem(slaveRobeTopIndex);
            return SLEEP_ONE_TICK;
        }

        final int slaveRobeBottomIndex = this.getInventoryIndex(ITEM_ID_SLAVE_ROBE_BOTTOM);

        if (this.isItemEquipped(slaveRobeBottomIndex)) {
            this.removeItem(slaveRobeBottomIndex);
            return SLEEP_ONE_TICK;
        }

        if (System.currentTimeMillis() <= this.clickTimeout) {
            return 0;
        }

        this.atObject(Object.IRON_GATE.coordinate.getX(), Object.IRON_GATE.coordinate.getY());
        this.clickTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
        return 0;
    }

    private int enterCampL2() {
        if (Area.CAMP_L2.contains(this.playerX, this.playerY)) {
            this.state = State.ENTER_CAMP_L3;
            return 0;
        }

        if (!this.hasInventoryItem(ITEM_ID_WROUGHT_IRON_KEY)) {
            this.state = State.GET_MINE_KEY;
            return 0;
        }

        final int slaveRobeTopIndex = this.getInventoryIndex(ITEM_ID_SLAVE_ROBE_TOP);

        if (!this.isItemEquipped(slaveRobeTopIndex)) {
            this.wearItem(slaveRobeTopIndex);
            return SLEEP_ONE_TICK;
        }

        final int slaveRobeBottomIndex = this.getInventoryIndex(ITEM_ID_SLAVE_ROBE_BOTTOM);

        if (!this.isItemEquipped(slaveRobeBottomIndex)) {
            this.wearItem(slaveRobeBottomIndex);
            return SLEEP_ONE_TICK;
        }

        this.atObject(Object.CAMP_L1_WOODEN_DOORS.coordinate.getX(), Object.CAMP_L1_WOODEN_DOORS.coordinate.getY());
        return SLEEP_ONE_SECOND;
    }

    private int enterCampL3() {
        if (Area.CAMP_L3.contains(this.playerX, this.playerY)) {
            this.state = State.ENTER_CAMP_L4;
            return 0;
        }

        if (!this.inCombat()) {
            final int npcIndex = this.getBlockingNpc();

            if (npcIndex != -1) {
                this.attackNpc(npcIndex);
                return SLEEP_ONE_TICK;
            }
        }

        if (this.distanceTo(Object.CAMP_L2_MINING_CAVE.coordinate.getX(), Object.CAMP_L2_MINING_CAVE.coordinate.getY()) > 1 ||
                this.inCombat()) {
            this.walkTo(Object.CAMP_L2_MINING_CAVE.coordinate.getX() + 1, Object.CAMP_L2_MINING_CAVE.coordinate.getY());
            return SLEEP_ONE_TICK;
        }

        this.atObject(Object.CAMP_L2_MINING_CAVE.coordinate.getX(), Object.CAMP_L2_MINING_CAVE.coordinate.getY());
        return SLEEP_ONE_SECOND;
    }

    private int enterCampL4() {
        if (Area.CAMP_L4.contains(this.playerX, this.playerY)) {
            this.state = State.ENTER_CAMP_MINE;
            return 0;
        }

        if (this.inCombat() ||
                this.playerX != Object.CAMP_L3_MINE_CART.coordinate.getX() ||
                this.playerY != Object.CAMP_L3_MINE_CART.coordinate.getY() - 1) {
            this.walkTo(Object.CAMP_L3_MINE_CART.coordinate.getX(), Object.CAMP_L3_MINE_CART.coordinate.getY() - 1);
            return SLEEP_ONE_TICK;
        }

        if (this.isQuestMenu()) {
            this.answer(0);
            this.clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
            return 0;
        }

        if (System.currentTimeMillis() <= this.clickTimeout) {
            return 0;
        }

        this.atObject2(Object.CAMP_L3_MINE_CART.coordinate.getX(), Object.CAMP_L3_MINE_CART.coordinate.getY());
        this.clickTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
        return 0;
    }

    private int enterCampMine() {
        if (Area.CAMP_MINE.contains(this.playerX, this.playerY)) {
            final int wroughtIronKeyIndex = this.getInventoryIndex(ITEM_ID_WROUGHT_IRON_KEY);

            if (wroughtIronKeyIndex != -1) {
                this.dropItem(wroughtIronKeyIndex);
                return SLEEP_ONE_SECOND;
            }

            this.state = State.MINE;
            return 0;
        }

        if (this.inCombat() ||
                this.distanceTo(Object.STURDY_IRON_GATE.coordinate.getX(), Object.STURDY_IRON_GATE.coordinate.getY()) > 1) {
            this.walkTo(Object.STURDY_IRON_GATE.coordinate.getX(), Object.STURDY_IRON_GATE.coordinate.getY());
            return SLEEP_ONE_TICK;
        }

        this.atWallObject(Object.STURDY_IRON_GATE.coordinate.getX(), Object.STURDY_IRON_GATE.coordinate.getY());
        return SLEEP_TWO_SECONDS;
    }

    private int exitBank() {
        if (!Area.BANK.contains(this.playerX, this.playerY)) {
            final int shantayDisclaimerIndex = this.getInventoryIndex(ITEM_ID_SHANTAY_DISCLAIMER);

            if (shantayDisclaimerIndex != -1) {
                this.dropItem(shantayDisclaimerIndex);
                return SLEEP_ONE_SECOND;
            }

            this.state = State.ENTER_CAMP_L1;
            return 0;
        }

        if (this.isQuestMenu()) {
            this.answer(0);
            this.clickTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
            return 0;
        }

        if (System.currentTimeMillis() <= this.clickTimeout) {
            return 0;
        }

        this.atObject(Object.STONE_GATE.coordinate.getX(), Object.STONE_GATE.coordinate.getY());
        this.clickTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
        return 0;
    }

    private int exitCampL1() {
        if (!Area.CAMP_L1.contains(this.playerX, this.playerY)) {
            this.state = State.ENTER_BANK;
            return 0;
        }

        if (this.distanceTo(Object.IRON_GATE.coordinate.getX(), Object.IRON_GATE.coordinate.getY()) > 1) {
            this.walkTo(Object.IRON_GATE.coordinate.getX() - 1, Object.IRON_GATE.coordinate.getY());
            return SLEEP_ONE_TICK;
        }

        final int slaveRobeTopIndex = this.getInventoryIndex(ITEM_ID_SLAVE_ROBE_TOP);

        if (this.isItemEquipped(slaveRobeTopIndex)) {
            this.removeItem(slaveRobeTopIndex);
            return SLEEP_ONE_TICK;
        }

        final int slaveRobeBottomIndex = this.getInventoryIndex(ITEM_ID_SLAVE_ROBE_BOTTOM);

        if (this.isItemEquipped(slaveRobeBottomIndex)) {
            this.removeItem(slaveRobeBottomIndex);
            return SLEEP_ONE_TICK;
        }

        if (System.currentTimeMillis() <= this.clickTimeout) {
            return 0;
        }

        this.atObject(Object.IRON_GATE.coordinate.getX(), Object.IRON_GATE.coordinate.getY());
        this.clickTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
        return 0;
    }

    private int exitCampL2() {
        if (Area.CAMP_L1.contains(this.playerX, this.playerY)) {
            this.state = State.EXIT_CAMP_L1;
            return 0;
        }

        if (!this.inCombat()) {
            final int npcIndex = this.getBlockingNpc();

            if (npcIndex != -1) {
                this.attackNpc(npcIndex);
                return SLEEP_ONE_TICK;
            }
        }

        if (this.distanceTo(Object.CAMP_L2_WOODEN_DOORS.coordinate.getX(), Object.CAMP_L2_WOODEN_DOORS.coordinate.getY()) > 1 ||
                this.inCombat()) {
            this.walkTo(Object.CAMP_L2_WOODEN_DOORS.coordinate.getX(), Object.CAMP_L2_WOODEN_DOORS.coordinate.getY() - 1);
            return SLEEP_ONE_TICK;
        }

        this.atObject(Object.CAMP_L2_WOODEN_DOORS.coordinate.getX(), Object.CAMP_L2_WOODEN_DOORS.coordinate.getY());
        return SLEEP_ONE_SECOND;
    }

    private int exitCampL3() {
        if (Area.CAMP_L2.contains(this.playerX, this.playerY)) {
            this.state = State.EXIT_CAMP_L2;
            return 0;
        }

        if (this.inCombat() ||
                this.distanceTo(Object.CAMP_L3_MINING_CAVE.coordinate.getX(), Object.CAMP_L3_MINING_CAVE.coordinate.getY()) > 1) {
            this.walkTo(Object.CAMP_L3_MINING_CAVE.coordinate.getX() - 1, Object.CAMP_L3_MINING_CAVE.coordinate.getY());
            return SLEEP_ONE_TICK;
        }

        this.atObject(Object.CAMP_L3_MINING_CAVE.coordinate.getX(), Object.CAMP_L3_MINING_CAVE.coordinate.getY());
        return SLEEP_ONE_SECOND;
    }

    private int exitCampL4() {
        if (Area.CAMP_L3.contains(this.playerX, this.playerY)) {
            this.state = State.EXIT_CAMP_L3;
            return 0;
        }

        if (this.inCombat() ||
                this.playerX != Object.CAMP_L4_MINE_CART.coordinate.getX() - 1 ||
                this.playerY != Object.CAMP_L4_MINE_CART.coordinate.getY()) {
            this.walkTo(Object.CAMP_L4_MINE_CART.coordinate.getX() - 1, Object.CAMP_L4_MINE_CART.coordinate.getY());
            return SLEEP_ONE_TICK;
        }

        if (this.isQuestMenu()) {
            this.answer(0);
            this.clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
            return 0;
        }

        if (System.currentTimeMillis() <= this.clickTimeout) {
            return 0;
        }

        this.atObject2(Object.CAMP_L4_MINE_CART.coordinate.getX(), Object.CAMP_L4_MINE_CART.coordinate.getY());
        this.clickTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
        return 0;
    }

    private int exitCampMine() {
        if (Area.CAMP_L4.contains(this.playerX, this.playerY)) {
            this.state = State.EXIT_CAMP_L4;
            return 0;
        }

        if (this.distanceTo(Object.STURDY_IRON_GATE.coordinate.getX(), Object.STURDY_IRON_GATE.coordinate.getY()) > 1) {
            this.walkTo(Object.STURDY_IRON_GATE.coordinate.getX(), Object.STURDY_IRON_GATE.coordinate.getY() - 1);
            return SLEEP_ONE_TICK;
        }

        this.atWallObject(Object.STURDY_IRON_GATE.coordinate.getX(), Object.STURDY_IRON_GATE.coordinate.getY());
        return SLEEP_TWO_SECONDS;
    }

    private int mine() {
        if (this.getInventoryCount() == MAX_INV_SIZE) {
            final int gemIndex = this.getInventoryIndex(ITEM_IDS_GEMS);

            if (gemIndex != -1) {
                this.dropItem(gemIndex);
                return SLEEP_ONE_SECOND;
            }

            if (this.adamantite && this.mithril) {
                this.updateNearestRock(OBJECT_IDS_ADAMANTITE_ROCK);

                if (this.nearestRock[0] != -1) {
                    final int mithrilOreIndex = this.getInventoryIndex(ITEM_ID_MITHRIL_ORE);

                    if (mithrilOreIndex != -1) {
                        this.dropItem(mithrilOreIndex);
                        return SLEEP_ONE_SECOND;
                    }
                }
            }

            this.state = State.EXIT_CAMP_MINE;
            return 0;
        }

        if (this.adamantite) {
            this.updateNearestRock(OBJECT_IDS_ADAMANTITE_ROCK);

            if (this.nearestRock[0] != -1) {
                if (this.nearestRock[0] == COORDINATE_ENCLOSED_ADAMANTITE_ROCK.getX() &&
                        this.nearestRock[1] == COORDINATE_ENCLOSED_ADAMANTITE_ROCK.getY() &&
                        (this.playerX != COORDINATE_ENCLOSED_ADAMANTITE_ROCK.getX() + 1 || this.playerY != COORDINATE_ENCLOSED_ADAMANTITE_ROCK.getY() + 1)) {
                    this.walkTo(COORDINATE_ENCLOSED_ADAMANTITE_ROCK.getX() + 1, COORDINATE_ENCLOSED_ADAMANTITE_ROCK.getY() + 1);
                    return SLEEP_ONE_TICK;
                }

                if (this.clickTimeout != 0 && System.currentTimeMillis() <= this.clickTimeout) {
                    return 0;
                }

                this.atObject(this.nearestRock[0], this.nearestRock[1]);
                this.clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
                return 0;
            }
        }

        if (this.mithril) {
            final int[] groundItemMithrilOre = this.getItemById(ITEM_ID_MITHRIL_ORE);

            if (groundItemMithrilOre[0] != -1) {
                this.pickupItem(ITEM_ID_MITHRIL_ORE, groundItemMithrilOre[1], groundItemMithrilOre[2]);
                return SLEEP_ONE_TICK;
            }

            this.updateNearestRock(OBJECT_IDS_MITHRIL_ROCK);

            if (this.nearestRock[0] != -1) {
                if (this.nearestRock[0] == COORDINATE_ENCLOSED_MITHRIL_ROCK.getX() &&
                        this.nearestRock[1] == COORDINATE_ENCLOSED_MITHRIL_ROCK.getY() &&
                        (this.playerX != COORDINATE_ENCLOSED_MITHRIL_ROCK.getX() + 1 || this.playerY != COORDINATE_ENCLOSED_MITHRIL_ROCK.getY() + 1)) {
                    this.walkTo(COORDINATE_ENCLOSED_MITHRIL_ROCK.getX() + 1, COORDINATE_ENCLOSED_MITHRIL_ROCK.getY() + 1);
                    return SLEEP_ONE_TICK;
                }

                if (this.clickTimeout != 0 && System.currentTimeMillis() <= this.clickTimeout) {
                    return 0;
                }

                this.atObject(this.nearestRock[0], this.nearestRock[1]);
                this.clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
                return 0;
            }
        }

        return SLEEP_ONE_TICK;
    }

    private int bank() {
        if (!this.isBanking()) {
            return this.useBankChest();
        }

        final int adamantiteOreInventoryCount = this.getInventoryCount(ITEM_ID_ADAMANTITE_ORE);

        if (adamantiteOreInventoryCount != 0) {
            this.deposit(ITEM_ID_ADAMANTITE_ORE, adamantiteOreInventoryCount);
            return SLEEP_ONE_TICK;
        }

        final int mithrilOreInventoryCount = this.getInventoryCount(ITEM_ID_MITHRIL_ORE);

        if (mithrilOreInventoryCount != 0) {
            this.deposit(ITEM_ID_MITHRIL_ORE, mithrilOreInventoryCount);
            return SLEEP_ONE_TICK;
        }

        final int shantayPassInventoryCount = this.getInventoryCount(ITEM_ID_SHANTAY_PASS);

        if (shantayPassInventoryCount != 1) {
            if (System.currentTimeMillis() <= this.withdrawTimeout) {
                return 0;
            }

            if (shantayPassInventoryCount == 0) {
                if (!this.hasBankItem(ITEM_ID_SHANTAY_PASS)) {
                    this.state = State.BUY_SHANTAY_PASS;
                    return 0;
                }

                this.withdraw(ITEM_ID_SHANTAY_PASS, 1);
                this.withdrawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
                return 0;
            }

            this.deposit(ITEM_ID_SHANTAY_PASS, shantayPassInventoryCount);
            return SLEEP_TWO_SECONDS;
        }

        final int coinInventoryCount = this.getInventoryCount(ITEM_ID_COINS);

        if (coinInventoryCount != 0) {
            this.deposit(ITEM_ID_COINS, coinInventoryCount);
            return SLEEP_ONE_TICK;
        }

        this.state = State.SLEEP;
        return 0;
    }

    private int useSleepBag() {
        final int sleepingBagIndex = this.getInventoryIndex(ITEM_ID_SLEEPING_BAG);

        if (this.getFatigue() != 0 && sleepingBagIndex != -1) {
            this.useItem(sleepingBagIndex);
            return SLEEP_ONE_SECOND;
        }

        if (!this.isBanking()) {
            return this.useBankChest();
        }

        if (this.getFatigue() == 0) {
            if (sleepingBagIndex == -1) {
                this.closeBank();
                this.state = State.EXIT_BANK;
                return SLEEP_ONE_TICK;
            }

            this.deposit(ITEM_ID_SLEEPING_BAG, 1);
            return SLEEP_ONE_TICK;
        }

        if (System.currentTimeMillis() <= this.withdrawTimeout) {
            return 0;
        }

        this.withdraw(ITEM_ID_SLEEPING_BAG, 1);
        this.withdrawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
        return 0;
    }

    private int getWroughtIronKey() {
        if (Area.CAMP_JAIL_DOWNSTAIRS.contains(this.playerX, this.playerY)) {
            if (this.hasInventoryItem(ITEM_ID_WROUGHT_IRON_KEY)) {
                if (this.getWallObjectIdFromCoords(Object.JAIL_DOOR.coordinate.getX(), Object.JAIL_DOOR.coordinate.getY()) == Object.JAIL_DOOR.id) {
                    this.atWallObject(Object.JAIL_DOOR.coordinate.getX(), Object.JAIL_DOOR.coordinate.getY());
                    return SLEEP_ONE_SECOND;
                }

                this.walkTo(Object.CAMP_L1_WOODEN_DOORS.coordinate.getX() + 1, Object.CAMP_L1_WOODEN_DOORS.coordinate.getY());
                return SLEEP_ONE_TICK;
            }

            if (System.currentTimeMillis() <= this.clickTimeout) {
                return 0;
            }

            this.atObject(Object.JAIL_LADDER_UP.coordinate.getX(), Object.JAIL_LADDER_UP.coordinate.getY());
            this.clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
            return 0;
        }

        if (Area.CAMP_JAIL_UPSTAIRS.contains(this.playerX, this.playerY)) {
            final int cellDoorKeyIndex = this.getInventoryIndex(ITEM_ID_CELL_DOOR_KEY);

            if (cellDoorKeyIndex != -1) {
                this.dropItem(cellDoorKeyIndex);
                return SLEEP_ONE_SECOND;
            }

            if (this.hasInventoryItem(ITEM_ID_WROUGHT_IRON_KEY)) {
                if (System.currentTimeMillis() <= this.clickTimeout) {
                    return 0;
                }

                this.atObject(Object.JAIL_LADDER_DOWN.coordinate.getX(), Object.JAIL_LADDER_DOWN.coordinate.getY());
                this.clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
                return 0;
            }

            if (System.currentTimeMillis() <= this.clickTimeout) {
                return 0;
            }

            this.atObject2(Object.JAIL_DESK.coordinate.getX(), Object.JAIL_DESK.coordinate.getY());
            this.clickTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
            return 0;
        }

        if (this.hasInventoryItem(ITEM_ID_WROUGHT_IRON_KEY)) {
            this.state = State.ENTER_CAMP_L2;
            return 0;
        }

        if (this.getWallObjectIdFromCoords(Object.JAIL_DOOR.coordinate.getX(), Object.JAIL_DOOR.coordinate.getY()) == Object.JAIL_DOOR.id) {
            this.atWallObject(Object.JAIL_DOOR.coordinate.getX(), Object.JAIL_DOOR.coordinate.getY());
            return SLEEP_ONE_SECOND;
        }

        this.walkTo(Object.JAIL_LADDER_UP.coordinate.getX(), Object.JAIL_LADDER_UP.coordinate.getY() + 1);
        return SLEEP_ONE_TICK;
    }

    private int buyShantayPass() {
        if (this.hasInventoryItem(ITEM_ID_SHANTAY_PASS)) {
            this.closeShop();
            this.state = State.BANK;
            return 0;
        }

        if (this.isShopOpen()) {
            final int shantayPassShopCount = this.getShopItemAmount(SHOP_INDEX_SHANTAY_PASS);

            if (shantayPassShopCount == MAXIMUM_SHANTAY_PASS_STOCK) {
                this.buyShopItem(SHOP_INDEX_SHANTAY_PASS, MAXIMUM_SHANTAY_PASS_STOCK);
                return SLEEP_TWO_SECONDS;
            }

            return 0;
        }

        if (!this.hasInventoryItem(ITEM_ID_COINS)) {
            if (!this.isBanking()) {
                return this.useBankChest();
            }

            if (System.currentTimeMillis() <= this.withdrawTimeout) {
                return 0;
            }

            final int coinBankCount = this.bankCount(ITEM_ID_COINS);

            if (coinBankCount < MINIMUM_SHANTAY_PASS_COST) {
                System.err.println("Out of coins. Cannot buy shantay passes.");
                this.setAutoLogin(false);
                this.stopScript();
            }

            this.withdraw(ITEM_ID_COINS, TOTAL_SHANTAY_PASS_COST);
            this.withdrawTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
            return 0;
        }

        return this.openShantayShop();
    }

    private void updateNearestRock(final int[] rockIds) {
        this.nearestRock[0] = -1;

        int currentDistance = Integer.MAX_VALUE;

        for (int index = 0; index < this.getObjectCount(); index++) {
            final int objectId = this.getObjectId(index);

            if (!inArray(rockIds, objectId)) {
                continue;
            }

            final int objectX = this.getObjectX(index);
            final int objectY = this.getObjectY(index);

            if (objectX == COORDINATE_X_UNREACHABLE_ADAMANTITE_ROCK) {
                continue;
            }

            final int distance = this.distanceTo(objectX, objectY);

            if (distance < currentDistance) {
                this.nearestRock[0] = objectX;
                this.nearestRock[1] = objectY;

                currentDistance = distance;
            }
        }
    }

    private int useBankChest() {
        if (System.currentTimeMillis() <= this.openBankTimeout) {
            return 0;
        }

        this.atObject(COORDINATE_BANK_CHEST.getX(), COORDINATE_BANK_CHEST.getY());
        this.openBankTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
        return 0;
    }

    private int openShantayShop() {
        if (this.isQuestMenu()) {
            this.answer(1);
            this.clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
            return 0;
        }

        if (System.currentTimeMillis() <= this.clickTimeout) {
            return 0;
        }

        final int[] assistant = this.getNpcByIdNotTalk(NPC_ID_ASSISTANT);

        if (assistant[0] == -1) {
            return SLEEP_ONE_TICK;
        }

        this.talkToNpc(assistant[0]);
        this.clickTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
        return 0;
    }

    private int getBlockingNpc() {
        final int direction = this.extension.getMobDirection(this.extension.getPlayer());

        for (int index = 0; index < this.countNpcs(); index++) {
            if (this.getNpcId(index) != NPC_ID_ROWDY_SLAVE) {
                continue;
            }

            final int npcX = this.getNpcX(index);
            final int npcY = this.getNpcY(index);

            if (npcX == this.playerX &&
                    ((npcY == this.playerY - 1 && direction == 0) ||
                            (npcY == this.playerY + 1 && direction == 4))) {
                return index;
            }

            if (npcY == this.playerY &&
                    ((npcX == this.playerX + 1 && direction == 2) ||
                            (npcX == this.playerX - 1 && direction == 6))) {
                return index;
            }
        }

        return -1;
    }

    private void setInitialState() {
        this.playerX = this.getX();
        this.playerY = this.getY();

        if (Area.BANK.contains(this.playerX, this.playerY)) {
            this.state = State.BANK;
            return;
        }

        if (Area.CAMP_MINE.contains(this.playerX, this.playerY)) {
            this.state = State.MINE;
            return;
        }

        if (this.getInventoryCount() == MAX_INV_SIZE) {
            if (Area.CAMP_L4.contains(this.playerX, this.playerY)) {
                this.state = State.EXIT_CAMP_L4;
            } else if (Area.CAMP_L3.contains(this.playerX, this.playerY)) {
                this.state = State.EXIT_CAMP_L3;
            } else if (Area.CAMP_L2.contains(this.playerX, this.playerY)) {
                this.state = State.EXIT_CAMP_L2;
            } else if (Area.CAMP_L1.contains(this.playerX, this.playerY)) {
                this.state = State.EXIT_CAMP_L1;
            } else {
                this.state = State.ENTER_BANK;
            }
        } else {
            if (Area.CAMP_L4.contains(this.playerX, this.playerY)) {
                this.state = State.ENTER_CAMP_MINE;
            } else if (Area.CAMP_L3.contains(this.playerX, this.playerY)) {
                this.state = State.ENTER_CAMP_L4;
            } else if (Area.CAMP_L2.contains(this.playerX, this.playerY)) {
                this.state = State.ENTER_CAMP_L3;
            } else if (Area.CAMP_JAIL_DOWNSTAIRS.contains(this.playerX, this.playerY) ||
                    Area.CAMP_JAIL_UPSTAIRS.contains(this.playerX, this.playerY)) {
                this.state = State.GET_MINE_KEY;
            } else if (Area.CAMP_L1.contains(this.playerX, this.playerY)) {
                this.state = State.ENTER_CAMP_L2;
            } else {
                this.state = State.ENTER_CAMP_L1;
            }
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
            return this.description;
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
            return this.lowerBoundingCoordinate;
        }

        public Coordinate getUpperBoundingCoordinate() {
            return this.upperBoundingCoordinate;
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
            return this.id;
        }

        public Coordinate getCoordinate() {
            return this.coordinate;
        }
    }
}
