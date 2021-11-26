import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * Identifies herbs.
 * <p>
 * Requirements:
 * Start at a bank or Shantay Pass with a sleeping bag in inventory.
 * The script will try to identify all herbs if no herb parameter is provided.
 * <p>
 * Optional Parameter:
 * -h,--herb <guam|ranarr_weed|irit_leaf|...>
 * <p>
 * Herbs:
 * guam
 * marrentill
 * tarromin
 * harralander
 * ranarr_weed
 * irit_leaf
 * avantoe
 * kwuarm
 * cadantine
 * dwarf_weed
 * torstol
 * <p>
 * Author: Chomp
 */
public class AA_HerbIdentifier extends AA_Script {
    private static final Coordinate COORDINATE_SHANTAY_BANK_CHEST = new Coordinate(58, 731);

    private static final long IDENTIFY_DELAY = 1210L; // +- based on latency

    private static final int SKILL_INDEX_HERBLAW = 15;
    private static final int MAXIMUM_FATIGUE = 99;

    private Herb herb;
    private Iterator<Herb> iterator;
    private Instant startTime;

    private double initialHerblawXp;

    private long openTimeout;
    private long depositTimeout;
    private long withdrawTimeout;
    private long identifyTimeout;

    private int herbsIdentified;
    private int herbsRemaining;

    private boolean shantayBanking;
    private boolean idle;

    public AA_HerbIdentifier(final Extension extension) {
        super(extension);
    }

    @Override
    public void init(final String parameters) {
        if (!parameters.isEmpty()) {
            final String[] args = parameters.split(" ");

            for (int i = 0; i < args.length; i++) {
                switch (args[i].toLowerCase()) {
                    case "-h":
                    case "--herb":
                        this.herb = Herb.valueOf(args[++i].toUpperCase());
                        break;
                    default:
                        throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
                }
            }
        }

        if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
            throw new IllegalStateException("Sleeping bag missing from inventory.");
        }

        if (this.herb == null) {
            this.iterator = EnumSet.allOf(Herb.class).iterator();
            this.herb = this.iterator.next();
        }

        if (this.getLevel(SKILL_INDEX_HERBLAW) < this.herb.level) {
            throw new IllegalStateException(String.format("Lvl %d Herblaw required to identify %s",
                    this.herb.level, getItemNameId(this.herb.id)));
        }

        this.shantayBanking = this.distanceTo(COORDINATE_SHANTAY_BANK_CHEST.getX(), COORDINATE_SHANTAY_BANK_CHEST.getY()) < 10;
        this.initialHerblawXp = this.getAccurateXpForLevel(SKILL_INDEX_HERBLAW);
        this.startTime = Instant.now();
    }

    @Override
    public int main() {
        if (this.idle) {
            return this.idle();
        }

        if (this.getFatigue() >= MAXIMUM_FATIGUE) {
            return this.sleep();
        }

        if (System.currentTimeMillis() <= this.identifyTimeout) {
            return 0;
        }

        if (this.getInventoryCount() == 1 ||
                this.getInventoryId(1) != this.herb.id) {
            return this.bank();
        }

        this.extension.displayMessage("@gre@Identifying ...");
        this.useItem(1);
        this.identifyTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
        return 0;
    }

    @Override
    public void onServerMessage(final String message) {
        if (message.startsWith("herb", 5)) {
            this.herbsIdentified++;
            if (this.herbsRemaining > 0) {
                this.herbsRemaining--;
            }
            this.identifyTimeout = System.currentTimeMillis() + IDENTIFY_DELAY;
        } else if (message.endsWith("men.") || message.endsWith("you.")) {
            this.openTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
        } else if (message.endsWith("area")) {
            if (!this.shantayBanking) {
                return;
            }
            this.idle = true;
        } else {
            super.onServerMessage(message);
        }
    }

    @Override
    public void paint() {
        int y = PAINT_OFFSET_Y;

        this.drawString("@yel@Herb Identifier", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

        if (this.startTime == null) {
            return;
        }

        final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

        this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        final double xpGained = this.getAccurateXpForLevel(SKILL_INDEX_HERBLAW) - this.initialHerblawXp;

        this.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
                        DECIMAL_FORMAT.format(xpGained), getUnitsPerHour(xpGained, secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
                        this.herb, this.herbsIdentified, getUnitsPerHour(this.herbsIdentified, secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString(String.format("@yel@Remaining: @whi@%d", this.herbsRemaining),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString(String.format("@yel@Time remaining: @whi@%s",
                        getTTL(this.herbsIdentified, this.herbsRemaining, secondsElapsed)),
                PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
    }

    private int bank() {
        if (!this.isBanking()) {
            if (this.shantayBanking) {
                if (System.currentTimeMillis() <= this.openTimeout) {
                    return 0;
                }

                this.atObject(COORDINATE_SHANTAY_BANK_CHEST.getX(), COORDINATE_SHANTAY_BANK_CHEST.getY());
                this.openTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
                return 0;
            }

            return this.openBank();
        }

        if (this.getInventoryCount() == 1) {
            if (System.currentTimeMillis() <= this.withdrawTimeout) {
                return 0;
            }

            if (!this.hasBankItem(this.herb.id)) {
                if (this.iterator == null || !this.iterator.hasNext()) {
                    return this.exit(String.format("Out of %s.", getItemNameId(this.herb.id)));
                }

                this.herb = this.iterator.next();
                this.herbsIdentified = 0;
                this.initialHerblawXp = this.getAccurateXpForLevel(SKILL_INDEX_HERBLAW);
                this.startTime = Instant.now();

                if (this.getLevel(SKILL_INDEX_HERBLAW) < this.herb.level) {
                    return this.exit(String.format("Lvl %d Herblaw required to identify %s.",
                            this.herb.level, getItemNameId(this.herb.id)));
                }

                return 0;
            }

            this.herbsRemaining = this.bankCount(this.herb.id);
            this.withdraw(this.herb.id, MAX_INV_SIZE - 1);
            this.withdrawTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
            return 0;
        }

        if (System.currentTimeMillis() <= this.depositTimeout) {
            return 0;
        }

        final int itemId = this.getInventoryId(1);
        this.deposit(itemId, MAX_INV_SIZE);
        this.depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
        return 0;
    }

    private int idle() {
        if (this.getX() == COORDINATE_SHANTAY_BANK_CHEST.getX() + 2 &&
                this.getY() == COORDINATE_SHANTAY_BANK_CHEST.getY()) {
            this.idle = false;
            return 0;
        }

        this.walkTo(COORDINATE_SHANTAY_BANK_CHEST.getX() + 2, COORDINATE_SHANTAY_BANK_CHEST.getY());
        return SLEEP_ONE_TICK;
    }

    private enum Herb {
        GUAM(165, 3, "Guam"),
        MARRENTILL(435, 5, "Marrentill"),
        TARROMIN(436, 12, "Tarromin"),
        HARRALANDER(437, 22, "Harralander"),
        RANARR_WEED(438, 30, "Ranarr"),
        IRIT_LEAF(439, 45, "Irit"),
        AVANTOE(440, 50, "Avantoe"),
        KWUARM(441, 55, "Kwuarm"),
        CADANTINE(442, 66, "Cadantine"),
        DWARF_WEED(443, 72, "Dwarfweed"),
        TORSTOL(933, 78, "Torstol");

        private final int id;
        private final int level;
        private final String name;

        Herb(final int id, final int level, final String name) {
            this.id = id;
            this.level = level;
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
