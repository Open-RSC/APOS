import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * Mixes potions.
 * <p>
 * Requirements:
 * Start at a bank or Shantay Pass with sleeping bag in inventory.
 * The script will try to mix all potions if no potion parameter is provided.
 * <p>
 * Optional Parameter:
 * -p,--potion <attack_potion|restore_prayer_potion|...>
 * <p>
 * Potions:
 * attack_potion
 * cure_poison_potion
 * strength_potion
 * stat_restoration_potion
 * restore_prayer_potion
 * super_attack_potion
 * poison_antidote
 * fishing_potion
 * super_strength_potion
 * weapon_poision
 * super_defense_potion
 * ranging_potion
 * potion_of_zamorak
 * <p>
 * Author: Chomp
 */
public class AA_PotionMixer extends AA_Script {
    private static final Coordinate COORDINATE_SHANTAY_BANK_CHEST = new Coordinate(58, 731);

    private static final long MIX_DELAY = 1210L; // +- based on latency

    private static final int SKILL_INDEX_HERBLAW = 15;
    private static final int WITHDRAW_COUNT = 14;
    private static final int MAXIMUM_FATIGUE = 99;

    private Potion potion;
    private Iterator<Potion> iterator;
    private Instant startTime;

    private double initialHerblawXp;

    private long openTimeout;
    private long depositTimeout;
    private long withdrawUnfTimeout;
    private long withdrawSecondaryTimeout;
    private long mixTimeout;

    private int potionsMixed;
    private int materialsRemaining;
    private int inventoryCount;

    private boolean shantayBanking;
    private boolean idle;

    public AA_PotionMixer(final Extension extension) {
        super(extension);
    }

    @Override
    public void init(final String parameters) {
        if (!parameters.isEmpty()) {
            final String[] args = parameters.split(" ");

            for (int i = 0; i < args.length; i++) {
                switch (args[i].toLowerCase()) {
                    case "-p":
                    case "--potion":
                        this.potion = Potion.valueOf(args[++i].toUpperCase());
                        break;
                    default:
                        throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
                }
            }
        }

        if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
            throw new IllegalStateException("Sleeping bag missing from inventory.");
        }

        if (this.potion == null) {
            this.iterator = EnumSet.allOf(Potion.class).iterator();
            this.potion = this.iterator.next();
        }

        if (this.getLevel(SKILL_INDEX_HERBLAW) < this.potion.level) {
            throw new IllegalStateException(String.format("Lvl %d Herblaw required to mix %s.",
                    this.potion.level, getItemNameId(this.potion.id)));
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

        if (System.currentTimeMillis() <= this.mixTimeout) {
            return 0;
        }

        this.inventoryCount = this.getInventoryCount();
        final int secondaryIndex = this.getInventoryIndex(this.potion.secondaryId);

        if (this.inventoryCount == 0 ||
                this.getInventoryId(1) != this.potion.unfinishedPotionId ||
                secondaryIndex == -1) {
            return this.bank();
        }

        this.extension.displayMessage("@gre@Mixing ...");
        this.useItemWithItem(1, secondaryIndex);
        this.mixTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
        return 0;
    }

    @Override
    public void onServerMessage(final String message) {
        if (message.endsWith("potion")) {
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

        this.drawString("@yel@Potion Mixer", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

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

        this.drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s pots@cya@/@whi@hr@cya@)",
                        this.potion, this.potionsMixed, getUnitsPerHour(this.potionsMixed, secondsElapsed)),
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
                this.getInventoryId(1) == this.potion.unfinishedPotionId &&
                this.inventoryCount < MAX_INV_SIZE) {
            if (System.currentTimeMillis() <= this.withdrawSecondaryTimeout) {
                return 0;
            }

            final int secondariesRemaining = this.bankCount(this.potion.secondaryId);

            if (secondariesRemaining == 0) {
                if (this.iterator == null || !this.iterator.hasNext()) {
                    return this.exit(String.format("Out of %s.", getItemNameId(this.potion.secondaryId)));
                }

                this.potion = this.iterator.next();
                this.potionsMixed = 0;
                this.initialHerblawXp = this.getAccurateXpForLevel(SKILL_INDEX_HERBLAW);
                this.startTime = Instant.now();

                if (this.getLevel(SKILL_INDEX_HERBLAW) < this.potion.level) {
                    return this.exit(String.format("Lvl %d Herblaw required to mix %s.",
                            this.potion.level, getItemNameId(this.potion.id)));
                }

                return 0;
            }

            final int unfsRemaining = this.bankCount(this.potion.unfinishedPotionId);

            this.materialsRemaining = Math.min(secondariesRemaining, unfsRemaining);
            this.withdraw(this.potion.secondaryId, WITHDRAW_COUNT);
            this.withdrawSecondaryTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
            return 0;
        }

        if (System.currentTimeMillis() <= this.withdrawUnfTimeout) {
            return 0;
        }

        if (this.inventoryCount == 1) {
            if (!this.hasBankItem(this.potion.unfinishedPotionId)) {
                if (this.iterator == null || !this.iterator.hasNext()) {
                    return this.exit(String.format("Out of %s.", getItemNameId(this.potion.unfinishedPotionId)));
                }

                this.potion = this.iterator.next();
                this.potionsMixed = 0;
                this.initialHerblawXp = this.getAccurateXpForLevel(SKILL_INDEX_HERBLAW);
                this.startTime = Instant.now();

                if (this.getLevel(SKILL_INDEX_HERBLAW) < this.potion.level) {
                    return this.exit(String.format("Lvl %d Herblaw required to mix %s.",
                            this.potion.level, getItemNameId(this.potion.id)));
                }

                return 0;
            }

            this.withdraw(this.potion.unfinishedPotionId, WITHDRAW_COUNT);
            this.withdrawUnfTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
            return 0;
        }

        if (System.currentTimeMillis() <= this.depositTimeout) {
            return 0;
        }

        final int itemId = this.getInventoryId(1);
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

    private enum Potion {
        ATTACK_POTION(3, 474, 454, 270, "Attack"),
        CURE_POISON_POTION(5, 566, 455, 473, "Cure Poison"),
        STRENGTH_POTION(12, 222, 456, 220, "Strength"),
        STAT_RESTORATION_POTION(22, 477, 457, 219, "Stat Res."),
        RESTORE_PRAYER_POTION(38, 483, 458, 469, "Prayer"),
        SUPER_ATTACK_POTION(45, 486, 459, 270, "Super Atk"),
        POISON_ANTIDOTE(48, 569, 459, 473, "Antidote"),
        FISHING_POTION(50, 489, 460, 469, "Fishing"),
        SUPER_STRENGTH_POTION(55, 492, 461, 220, "Super Str"),
        WEAPON_POISION(60, 572, 461, 472, "Weapon Poison"),
        SUPER_DEFENSE_POTION(66, 495, 462, 471, "Super Def"),
        RANGING_POTION(72, 498, 463, 501, "Ranging"),
        POTION_OF_ZAMORAK(78, 963, 935, 936, "Zamorak");

        private final int level;
        private final int id;
        private final int unfinishedPotionId;
        private final int secondaryId;
        private final String name;

        Potion(final int level, final int id, final int unfinishedPotionId, final int secondaryId, final String name) {
            this.level = level;
            this.id = id;
            this.unfinishedPotionId = unfinishedPotionId;
            this.secondaryId = secondaryId;
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
