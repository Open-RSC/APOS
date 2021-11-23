import java.awt.Font;
import java.time.Duration;
import java.time.Instant;

/**
 * Smelts bars at Alkharid furnace.
 * <p>
 * Requirements:
 * Start at Alkharid bank with sleeping bag in inventory.
 * <p>
 * Required Parameters:
 * -b,--bar <bronze|iron|silver|steel|gold|mithril|adamantite|runite>
 * <p>
 * Author: Chomp
 */
public class AA_AlkharidSmelter extends AA_Script {
    private static final int SKILL_INDEX_SMITHING = 13;
    private static final int MAXIMUM_DISTANCE_FROM_OBJECT = 18;
    private static final int MAXIMUM_FATIGUE = 99;

    private Bar bar;
    private Instant startTime;

    private double initialSmithingXp;

    private long depositTimeout;
    private long withdrawPrimaryOreTimeout;
    private long withdrawSecondaryOreTimeout;
    private long closeTimeout;
    private long smeltTimeout;

    private int barsSmelted;
    private int playerX;
    private int playerY;
    private int primaryOreRemaining;

    public AA_AlkharidSmelter(final Extension extension) {
        super(extension);
    }

    @Override
    public void init(final String parameters) {
        if (parameters.isEmpty()) {
            throw new IllegalArgumentException("Missing bar type parameter.");
        }

        final String[] args = parameters.split(" ");

        for (int i = 0; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "-b":
                case "--bar":
                    this.bar = Bar.valueOf(args[++i].toUpperCase());
                    break;
                default:
                    throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
            }
        }

        if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
            throw new IllegalStateException("Sleeping bag missing from inventory.");
        }

        this.initialSmithingXp = this.getAccurateXpForLevel(SKILL_INDEX_SMITHING);
        this.startTime = Instant.now();
    }

    @Override
    public int main() {
        this.playerX = this.getX();
        this.playerY = this.getY();

        if (this.getInventoryCount() == 1 ||
                this.getInventoryId(1) != this.bar.primaryOreId ||
                !this.hasInventoryItem(this.bar.secondaryOreId) ||
                this.isBanking()) {
            return this.bank();
        }

        return this.smelt();
    }

    @Override
    public void onServerMessage(final String message) {
        if (message.startsWith("bar", 15)) {
            this.barsSmelted += 1;
            this.smeltTimeout = 0L;
        } else if (message.startsWith("impure", 15)) {
            this.smeltTimeout = 0L;
        } else {
            super.onServerMessage(message);
        }
    }

    @Override
    public void paint() {
        int y = PAINT_OFFSET_Y;

        this.drawString("@yel@Alkharid Smelter", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

        if (this.startTime == null) {
            return;
        }

        final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

        this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        final double xpGained = this.getAccurateXpForLevel(SKILL_INDEX_SMITHING) - this.initialSmithingXp;

        this.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
                        DECIMAL_FORMAT.format(xpGained), getUnitsPerHour(xpGained, secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s bars@cya@/@whi@hr@cya@)",
                        this.bar, this.barsSmelted, getUnitsPerHour(this.barsSmelted, secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString(String.format("@yel@Time remaining: @whi@%s",
                        getTTL(this.barsSmelted, this.primaryOreRemaining, secondsElapsed)),
                PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
    }

    private int smelt() {
        if (Area.FURNACE.contains(this.playerX, this.playerY)) {
            if (this.getFatigue() >= MAXIMUM_FATIGUE) {
                return this.sleep();
            }

            if (System.currentTimeMillis() <= this.smeltTimeout) {
                return 0;
            }

            this.useSlotOnObject(1, Object.FURNACE.coordinate.getX(), Object.FURNACE.coordinate.getY());
            this.smeltTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
            return 0;
        }

        if (Area.BANK.contains(this.playerX, this.playerY) &&
                this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
            this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
            return SLEEP_ONE_SECOND;
        }

        this.walkTo(Object.FURNACE.coordinate.getX() - 1, Object.FURNACE.coordinate.getY());
        return SLEEP_ONE_TICK;
    }

    private int bank() {
        if (Area.BANK.contains(this.playerX, this.playerY)) {
            if (!this.isBanking()) {
                return this.openBank();
            }

            if (this.getInventoryId(1) == this.bar.primaryOreId &&
                    this.getInventoryCount(this.bar.primaryOreId) == this.bar.primaryOreCount) {
                if (this.getInventoryCount(this.bar.secondaryOreId) == this.bar.secondaryOreCount) {
                    if (System.currentTimeMillis() <= this.closeTimeout) {
                        return 0;
                    }

                    this.primaryOreRemaining = this.bankCount(this.bar.primaryOreId);
                    this.closeBank();
                    this.closeTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
                    return 0;
                }

                if (System.currentTimeMillis() <= this.withdrawSecondaryOreTimeout) {
                    return 0;
                }

                if (this.bankCount(this.bar.secondaryOreId) < this.bar.secondaryOreCount) {
                    System.err.printf("Ran out of %s.%n", getItemNameId(this.bar.secondaryOreId));
                    this.setAutoLogin(false);
                    this.stopScript();
                }

                this.withdraw(this.bar.secondaryOreId, this.bar.secondaryOreCount);
                this.withdrawSecondaryOreTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
                return 0;
            }

            if (System.currentTimeMillis() <= this.withdrawPrimaryOreTimeout) {
                return 0;
            }

            if (this.getInventoryCount() == 1) {
                if (this.bankCount(this.bar.primaryOreId) < this.bar.primaryOreCount) {
                    System.err.printf("Ran out of %s.%n", getItemNameId(this.bar.primaryOreId));
                    this.setAutoLogin(false);
                    this.stopScript();
                }

                this.withdraw(this.bar.primaryOreId, this.bar.primaryOreCount);
                this.withdrawPrimaryOreTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
                return 0;
            }

            if (System.currentTimeMillis() <= this.depositTimeout) {
                return 0;
            }

            final int itemId = this.getInventoryId(1);
            final int itemCount = this.getInventoryCount(itemId);

            this.deposit(itemId, itemCount);
            this.depositTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
            return 0;
        }

        if (this.distanceTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) <= MAXIMUM_DISTANCE_FROM_OBJECT) {
            if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
                this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
                return SLEEP_ONE_SECOND;
            }

            this.walkTo(Object.BANK_DOORS.coordinate.getX() + 1, Object.BANK_DOORS.coordinate.getY());
            return SLEEP_ONE_TICK;
        }

        this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
        return SLEEP_ONE_TICK;
    }

    private enum Bar {
        BRONZE(169, 150, 202, 14, 14),
        IRON(170, 151, 151, 29, 29),
        SILVER(384, 383, 383, 29, 29),
        STEEL(171, 151, 155, 9, 18),
        GOLD(172, 152, 152, 29, 29),
        MITHRIL(173, 153, 155, 5, 20),
        ADAMANTITE(174, 154, 155, 4, 24),
        RUNITE(408, 409, 155, 3, 24);

        private final int id;
        private final int primaryOreId;
        private final int secondaryOreId;
        private final int primaryOreCount;
        private final int secondaryOreCount;

        Bar(final int id, final int primaryOreId, final int secondaryOreId, final int primaryOreCount, final int secondaryOreCount) {
            this.id = id;
            this.primaryOreId = primaryOreId;
            this.secondaryOreId = secondaryOreId;
            this.primaryOreCount = primaryOreCount;
            this.secondaryOreCount = secondaryOreCount;
        }

        @Override
        public String toString() {
            return this.name().charAt(0) + this.name().substring(1).toLowerCase();
        }
    }

    private enum Area implements RSArea {
        BANK(new Coordinate(87, 689), new Coordinate(93, 700)),
        FURNACE(new Coordinate(82, 678), new Coordinate(86, 681));

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
        FURNACE(118, new Coordinate(85, 679)),
        BANK_DOORS(64, new Coordinate(86, 695));

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
