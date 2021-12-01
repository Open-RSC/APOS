import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * Mixes unfinished potions.
 * <p>
 * Requirements:
 * Start at a bank or Shantay Pass with an empty inventory.
 * The script will try to mix all herbs if no herb parameter is provided.
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
public class AA_UnfinishedPotionMixer extends AA_Script {
    private static final Coordinate COORDINATE_SHANTAY_BANK_CHEST = new Coordinate(58, 731);

    private static final long MIX_DELAY = 1210L; // +- based on latency

    private static final int ITEM_ID_WATER_FILLED_VIAL = 464;
    private static final int WITHDRAW_COUNT = 15;
    private static final int SKILL_INDEX_HERBLAW = 15;

    private Herb herb;
    private Iterator<Herb> iterator;
    private Instant startTime;

    private long openTimeout;
    private long depositTimeout;
    private long withdrawVialTimeout;
    private long withdrawHerbTimeout;
    private long mixTimeout;

    private int inventoryCount;

    private int potionsMixed;
    private int materialsRemaining;

    private boolean shantayBanking;
    private boolean idle;

    public AA_UnfinishedPotionMixer(final Extension extension) {
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

        if (this.herb == null) {
            this.iterator = EnumSet.allOf(Herb.class).iterator();
            this.herb = this.iterator.next();
        }

        if (this.getLevel(SKILL_INDEX_HERBLAW) < this.herb.level) {
            throw new IllegalStateException(String.format("Lvl %d Herblaw required to mix %s",
                    this.herb.level, getItemNameId(this.herb.id)));
        }

        this.shantayBanking = this.distanceTo(COORDINATE_SHANTAY_BANK_CHEST.getX(), COORDINATE_SHANTAY_BANK_CHEST.getY()) < 10;
        this.startTime = Instant.now();
    }

    @Override
    public int main() {
        if (this.idle) {
            return this.idle();
        }

        if (System.currentTimeMillis() <= this.mixTimeout) {
            return 0;
        }

        this.inventoryCount = this.getInventoryCount();
        final int herbIndex = this.getInventoryIndex(this.herb.id);

        if (this.inventoryCount == 0 ||
                this.getInventoryId(0) != ITEM_ID_WATER_FILLED_VIAL ||
                herbIndex == -1) {
            return this.bank();
        }

        this.extension.displayMessage("@gre@Mixing ...");
        this.useItemWithItem(0, herbIndex);
        this.mixTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
        return 0;
    }

    @Override
    public void onServerMessage(final String message) {
        if (message.endsWith("water")) {
            this.potionsMixed++;
            if (this.materialsRemaining > 0) {
                this.materialsRemaining--;
            }
            this.mixTimeout = System.currentTimeMillis() + MIX_DELAY;
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

        this.drawString("@yel@Unfinished Potion Mixer", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

        if (this.startTime == null) {
            return;
        }

        final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

        this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s pots@cya@/@whi@hr@cya@)",
                        this.herb, this.potionsMixed, getUnitsPerHour(this.potionsMixed, secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString(String.format("@yel@Remaining: @whi@%d", this.materialsRemaining),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString(String.format("@yel@Time remaining: @whi@%s",
                        getTTL(this.potionsMixed, this.materialsRemaining, secondsElapsed)),
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

        if (this.inventoryCount > 0 &&
                this.getInventoryId(0) == ITEM_ID_WATER_FILLED_VIAL &&
                this.inventoryCount < MAX_INV_SIZE) {
            if (System.currentTimeMillis() <= this.withdrawHerbTimeout) {
                return 0;
            }

            final int herbsRemaining = this.bankCount(this.herb.id);

            if (herbsRemaining == 0) {
                if (this.iterator == null || !this.iterator.hasNext()) {
                    return this.exit(String.format("Out of %s.", getItemNameId(this.herb.id)));
                }

                this.herb = this.iterator.next();
                this.potionsMixed = 0;
                this.startTime = Instant.now();

                if (this.getLevel(SKILL_INDEX_HERBLAW) < this.herb.level) {
                    return this.exit(String.format("Lvl %d Herblaw required to mix %s.",
                            this.herb.level, getItemNameId(this.herb.id)));
                }

                return 0;
            }

            final int vialsRemaining = this.bankCount(ITEM_ID_WATER_FILLED_VIAL);

            this.materialsRemaining = Math.min(herbsRemaining, vialsRemaining);
            this.withdraw(this.herb.id, WITHDRAW_COUNT);
            this.withdrawHerbTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
            return 0;
        }

        if (System.currentTimeMillis() <= this.withdrawVialTimeout) {
            return 0;
        }

        if (this.inventoryCount == 0) {
            if (!this.hasBankItem(ITEM_ID_WATER_FILLED_VIAL)) {
                return this.exit("Out of water-filled vials.");
            }

            this.withdraw(ITEM_ID_WATER_FILLED_VIAL, WITHDRAW_COUNT);
            this.withdrawVialTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
            return 0;
        }

        if (System.currentTimeMillis() <= this.depositTimeout) {
            return 0;
        }

        final int itemId = this.getInventoryId(0);
        this.deposit(itemId, MAX_INV_SIZE);
        this.depositTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
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
        GUAM(444, 3, "Guam"),
        MARRENTILL(445, 5, "Marrentill"),
        TARROMIN(446, 12, "Tarromin"),
        HARRALANDER(447, 22, "Harralander"),
        RANARR_WEED(448, 30, "Ranarr"),
        IRIT_LEAF(449, 45, "Irit"),
        AVANTOE(450, 50, "Avantoe"),
        KWUARM(451, 55, "Kwuarm"),
        CADANTINE(452, 66, "Cadantine"),
        DWARF_WEED(453, 72, "Dwarfweed"),
        TORSTOL(934, 78, "Torstol");

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
