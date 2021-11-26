import java.awt.Font;
import java.time.Duration;
import java.time.Instant;

public class AA_AmuletEnchanter extends AA_Script {
    private static final Coordinate COORDINATE_SHANTAY_BANK_CHEST = new Coordinate(58, 731);

    private static final long CAST_DELAY = 550L; // +- based on latency

    private static final int ITEM_ID_COSMIC_RUNE = 46;
    private static final int ITEM_ID_WATER_RUNE = 32;
    private static final int ITEM_ID_EARTH_RUNE = 34;
    private static final int INVENTORY_INDEX_COSMIC_RUNE = 2;
    private static final int INVENTORY_INDEX_DRAGONSTONE_RUNE = 3;
    private static final int DRAGONSTONE_RUNE_COUNT = 15;
    private static final int SKILL_INDEX_MAGIC = 6;
    private static final int MAXIMUM_FATIGUE = 99;

    private Amulet amulet;
    private Instant startTime;

    private double initialMagicXp;

    private long openTimeout;
    private long depositTimeout;
    private long withdrawTimeout;
    private long castTimeout;

    private int initialInventoryCount;

    private int amuletsEnchanted;
    private int materialsRemaining;

    private boolean shantayBanking;
    private boolean idle;

    public AA_AmuletEnchanter(final Extension extension) {
        super(extension);
    }

    @Override
    public void init(final String parameters) {
        if (parameters.isEmpty()) {
            throw new IllegalStateException("Empty script parameters.");
        }

        final String[] args = parameters.split(" ");

        for (int i = 0; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "-a":
                case "--amulet":
                    this.amulet = Amulet.valueOf(args[++i].toUpperCase());
                    break;
                default:
                    throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
            }
        }

        if (this.getInventoryIndex(ITEM_ID_SLEEPING_BAG) != 0) {
            throw new IllegalStateException("Sleeping bag missing from 1st inv slot.");
        }

        final int staffIndex = this.getInventoryIndex(this.amulet.staffIds);

        if (staffIndex != 1 || !this.isItemEquipped(staffIndex)) {
            throw new IllegalStateException("Staff unequipped/missing from 2nd inv slot.");
        }

        if (this.getInventoryIndex(ITEM_ID_COSMIC_RUNE) != INVENTORY_INDEX_COSMIC_RUNE) {
            throw new IllegalStateException("Cosmic runes missing from 3rd inv slot.");
        }

        this.initialInventoryCount = this.amulet == Amulet.DRAGONSTONE ? 4 : 3;
        this.shantayBanking = this.distanceTo(COORDINATE_SHANTAY_BANK_CHEST.getX(), COORDINATE_SHANTAY_BANK_CHEST.getY()) < 10;
        this.initialMagicXp = this.getAccurateXpForLevel(SKILL_INDEX_MAGIC);
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

        if (System.currentTimeMillis() <= this.castTimeout) {
            return 0;
        }

        if (this.getInventoryId(INVENTORY_INDEX_COSMIC_RUNE) != ITEM_ID_COSMIC_RUNE) {
            return this.exit("Out of cosmic runes.");
        }

        if (this.amulet == Amulet.DRAGONSTONE) {
            final int runeId = this.getInventoryId(INVENTORY_INDEX_DRAGONSTONE_RUNE);

            if (runeId != ITEM_ID_WATER_RUNE && runeId != ITEM_ID_EARTH_RUNE) {
                return this.exit("Out of water/earth runes.");
            }
        }

        if (this.getInventoryCount() == this.initialInventoryCount ||
                this.getInventoryId(this.initialInventoryCount) != this.amulet.id) {
            return this.bank();
        }

        this.extension.displayMessage("@gre@Casting ...");
        this.castOnItem(this.amulet.spellId, this.initialInventoryCount);
        this.castTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
        return 0;
    }

    @Override
    public void onServerMessage(final String message) {
        if (message.endsWith("amulet")) {
            this.amuletsEnchanted++;

            if (this.materialsRemaining > 0) {
                this.materialsRemaining--;
            }

            this.castTimeout = System.currentTimeMillis() + CAST_DELAY;
        } else if (message.startsWith("reagents", 23)) {
            this.exit("Misconfigured runes in inventory.");
        } else if (message.endsWith("spell")) {
            this.castTimeout = 0L;
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

        this.drawString("@yel@Amulet Enchanter", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

        if (this.startTime == null) {
            return;
        }

        final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

        this.drawString(String.format("@yel@Runtime: @whi@%s",
                        getElapsedSeconds(secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        final double xpGained = this.getAccurateXpForLevel(SKILL_INDEX_MAGIC) - this.initialMagicXp;

        this.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
                        DECIMAL_FORMAT.format(xpGained), getUnitsPerHour(xpGained, secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString(String.format("@yel@%s: @whi@%s @cya@(@whi@%s casts@cya@/@whi@hr@cya@)",
                        this.amulet, this.amuletsEnchanted, getUnitsPerHour(this.amuletsEnchanted, secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString(String.format("@yel@Remaining: @whi@%d", this.materialsRemaining),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString(String.format("@yel@Time remaining: @whi@%s",
                        getTTL(this.amuletsEnchanted, this.materialsRemaining, secondsElapsed)),
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

        if (this.getInventoryCount() == this.initialInventoryCount) {
            if (System.currentTimeMillis() <= this.withdrawTimeout) {
                return 0;
            }

            final int amuletsRemaining = this.bankCount(this.amulet.id);

            if (amuletsRemaining == 0) {
                return this.exit(String.format("Out of %ss.", getItemNameId(this.amulet.id)));
            }

            final int cosmicsRemaining = this.getInventoryStack(INVENTORY_INDEX_COSMIC_RUNE);

            final int runesRemaining;

            if (this.amulet == Amulet.DRAGONSTONE) {
                final int dragonStoneRunesRemaining = this.getInventoryStack(INVENTORY_INDEX_DRAGONSTONE_RUNE);
                runesRemaining = Math.min(cosmicsRemaining, dragonStoneRunesRemaining / DRAGONSTONE_RUNE_COUNT);
            } else {
                runesRemaining = cosmicsRemaining;
            }

            this.materialsRemaining = Math.min(amuletsRemaining, runesRemaining);

            this.withdraw(this.amulet.id, MAX_INV_SIZE - this.initialInventoryCount);
            this.withdrawTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
            return 0;
        }

        if (System.currentTimeMillis() <= this.depositTimeout) {
            return 0;
        }

        final int itemId = this.getInventoryId(this.initialInventoryCount);
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

    private enum Amulet {
        SAPPHIRE(302, 3, new int[]{102, 616, 683}, "Sapphire"),
        EMERALD(303, 13, new int[]{101, 617, 684}, "Emerald"),
        RUBY(304, 24, new int[]{197, 615, 682}, "Ruby"),
        DIAMOND(305, 30, new int[]{103, 618, 685}, "Diamond"),
        DRAGONSTONE(610, 42, new int[]{102, 616, 683, 103, 618, 685}, "Dragonstone");

        private final int id;
        private final int spellId;
        private final int[] staffIds;
        private final String name;

        Amulet(final int id, final int spellId, final int[] staffIds, final String name) {
            this.id = id;
            this.spellId = spellId;
            this.staffIds = staffIds;
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
