import java.awt.Font;
import java.time.Duration;
import java.time.Instant;

/**
 * Smiths items at the Varrock West anvils.
 * <p>
 * Requirements:
 * Start at Varrock West with sleeping bag and hammer in inventory.
 * <p>
 * Required Parameters:
 * -b,--bar <bronze|iron|steel|mithril|adamantite|runite>
 * -p,--product <plate_mail_body|kite_shield|two_handed_sword|...>
 * <p>
 * Products:
 * dagger
 * throwing_knife
 * short_sword
 * long_sword
 * scimitar
 * two_handed_sword
 * hatchet
 * battle_axe
 * mace
 * medium_helmet
 * large_helmet
 * square_shield
 * kite_shield
 * chain_mail_body
 * plate_mail_body
 * plate_mail_legs
 * plated_skirt
 * arrow_heads
 * bronze_wire
 * nails
 * <p>
 * Author: Chomp
 */
public class AA_VarrockWestSmither extends AA_Script {
    private static final int ITEM_ID_HAMMER = 168;
    private static final int INITIAL_INVENTORY_SIZE = 2;
    private static final int SKILL_INDEX_SMITHING = 13;
    private static final int MAXIMUM_FATIGUE = 99;

    private Bar bar;
    private Product product;
    private Instant startTime;

    private double initialSmithingXp;

    private long bankCloseTimeout;
    private long bankWithdrawTimeout;
    private long doorOpenTimeout;
    private long optionMenuTimeout;

    private int barWithdrawCount;
    private int barsSmithed;
    private int barsRemaining;
    private int playerX;
    private int playerY;

    public AA_VarrockWestSmither(final Extension extension) {
        super(extension);
    }

    @Override
    public void init(final String parameters) {
        if (parameters.isEmpty()) {
            throw new IllegalArgumentException("Missing bar type and product parameters.");
        }

        final String[] args = parameters.split(" ");

        for (int i = 0; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "-b":
                case "--bar":
                    this.bar = Bar.valueOf(args[++i].toUpperCase());
                    break;
                case "-p":
                case "--product":
                    this.product = Product.valueOf(args[++i].toUpperCase());
                    break;
                default:
                    throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
            }
        }

        if (this.bar == null) {
            throw new IllegalArgumentException("Missing bar type parameter.");
        }

        if (this.product == null) {
            throw new IllegalArgumentException("Missing product parameter.");
        }

        if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
            throw new IllegalStateException("Sleeping bag missing from inventory.");
        }

        if (!this.hasInventoryItem(ITEM_ID_HAMMER)) {
            throw new IllegalStateException("Hammer missing from inventory.");
        }

        final int inventorySize = MAX_INV_SIZE - INITIAL_INVENTORY_SIZE;

        this.barWithdrawCount = inventorySize - (inventorySize % this.product.getBarCount());
        this.initialSmithingXp = this.getAccurateXpForLevel(SKILL_INDEX_SMITHING);
        this.startTime = Instant.now();
    }

    @Override
    public int main() {
        this.playerX = this.getX();
        this.playerY = this.getY();

        if (this.getInventoryCount(this.bar.id) < this.product.barCount || this.isBanking()) {
            return this.bank();
        }

        return this.smith();
    }

    @Override
    public void onServerMessage(final String message) {
        if (message.startsWith("hammer", 4)) {
            this.barsSmithed += this.product.barCount;
            this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
        } else if (message.endsWith("shut") || message.endsWith("open")) {
            this.doorOpenTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
        } else {
            super.onServerMessage(message);
        }
    }

    @Override
    public void paint() {
        int y = PAINT_OFFSET_Y;

        this.drawString("@yel@Varrock West Smither", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

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
                        this.bar, this.barsSmithed, getUnitsPerHour(this.barsSmithed, secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString(String.format("@yel@Time remaining: @whi@%s",
                        getTTL(this.barsSmithed, this.barsRemaining, secondsElapsed)),
                PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
    }

    private int bank() {
        if (Area.BANK.contains(this.playerX, this.playerY)) {
            if (!this.isBanking()) {
                return this.openBank();
            }

            return this.useBank();
        }

        if (Area.ANVIL_HOUSE.contains(this.playerX, this.playerY)) {
            if (this.getWallObjectIdFromCoords(Object.ANVIL_DOOR.coordinate.getX(), Object.ANVIL_DOOR.coordinate.getY()) == Object.ANVIL_DOOR.id) {
                if (System.currentTimeMillis() <= this.doorOpenTimeout) {
                    return 0;
                }

                this.atWallObject(Object.ANVIL_DOOR.coordinate.getX(), Object.ANVIL_DOOR.coordinate.getY());
                this.doorOpenTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
                return 0;
            }

            this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
            return SLEEP_ONE_TICK;
        }

        if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
            if (System.currentTimeMillis() <= this.doorOpenTimeout) {
                return 0;
            }

            this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
            this.doorOpenTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
            return 0;
        }

        this.walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY() - 1);
        return SLEEP_ONE_TICK;
    }

    private int useBank() {
        if (this.getInventoryCount(this.bar.id) >= this.product.barCount) {
            if (System.currentTimeMillis() <= this.bankCloseTimeout) {
                return 0;
            }

            this.barsRemaining = this.bankCount(this.bar.id);
            this.closeBank();
            this.bankCloseTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
            return 0;
        }

        if (System.currentTimeMillis() <= this.bankWithdrawTimeout) {
            return 0;
        }

        if (this.getInventoryCount() > INITIAL_INVENTORY_SIZE) {
            final int itemId = this.getInventoryId(INITIAL_INVENTORY_SIZE);
            this.deposit(itemId, this.getInventoryCount(itemId));
            return SLEEP_ONE_TICK;
        }

        if (this.bankCount(this.bar.id) < this.product.barCount) {
            System.err.println("Out of bars.");
            this.setAutoLogin(false);
            this.stopScript();
        }

        this.withdraw(this.bar.id, this.barWithdrawCount);
        this.bankWithdrawTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
        return 0;
    }

    private int smith() {
        if (Area.ANVIL_HOUSE.contains(this.playerX, this.playerY)) {
            if (this.getFatigue() >= MAXIMUM_FATIGUE) {
                return this.sleep();
            }

            return this.useAnvil();
        }

        if (Area.BANK.contains(this.playerX, this.playerY)) {
            if (this.getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
                if (System.currentTimeMillis() <= this.doorOpenTimeout) {
                    return 0;
                }

                this.atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
                this.doorOpenTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
                return 0;
            }

            this.walkTo(Object.ANVIL_DOOR.coordinate.getX(), Object.ANVIL_DOOR.coordinate.getY());
            return SLEEP_ONE_TICK;
        }

        if (this.getWallObjectIdFromCoords(Object.ANVIL_DOOR.coordinate.getX(), Object.ANVIL_DOOR.coordinate.getY()) == Object.ANVIL_DOOR.id) {
            if (System.currentTimeMillis() <= this.doorOpenTimeout) {
                return 0;
            }

            this.atWallObject(Object.ANVIL_DOOR.coordinate.getX(), Object.ANVIL_DOOR.coordinate.getY());
            this.doorOpenTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
            return 0;
        }

        this.walkTo(Object.ANVIL_DOOR.coordinate.getX() - 1, Object.ANVIL_DOOR.coordinate.getY());
        return SLEEP_ONE_TICK;
    }

    private int useAnvil() {
        if (this.isQuestMenu()) {
            int index;

            for (final String menuOption : this.product.getMenuOptions()) {
                index = this.getMenuIndex(menuOption);

                if (index != -1) {
                    this.answer(index);
                    this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
                    return 0;
                }
            }

            return SLEEP_ONE_SECOND;
        }

        if (System.currentTimeMillis() <= this.optionMenuTimeout) {
            return 0;
        }

        this.useSlotOnObject(INITIAL_INVENTORY_SIZE, Object.ANVIL.coordinate.getX(), Object.ANVIL.coordinate.getY());
        this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
        return 0;
    }

    private enum Bar {
        BRONZE(169),
        IRON(170),
        STEEL(171),
        MITHRIL(173),
        ADAMANTITE(174),
        RUNITE(408);

        private final int id;

        Bar(final int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return this.name().charAt(0) + this.name().substring(1).toLowerCase();
        }

        public int getId() {
            return this.id;
        }
    }

    private enum Product {
        DAGGER(1, new String[]{"Make Weapon", "Dagger"}),
        THROWING_KNIFE(1, new String[]{"Make Weapon", "Throwing Knife"}),
        SHORT_SWORD(1, new String[]{"Make Weapon", "Sword", "Short sword"}),
        LONG_SWORD(2, new String[]{"Make Weapon", "Sword", "Long sword (2 bars)"}),
        SCIMITAR(2, new String[]{"Make Weapon", "Sword", "Scimitar (2 bars)"}),
        TWO_HANDED_SWORD(3, new String[]{"Make Weapon", "Sword", "2-handed sword (3 bars)"}),
        HATCHET(1, new String[]{"Make Weapon", "Axe", "Hatchet"}),
        BATTLE_AXE(3, new String[]{"Make Weapon", "Axe", "Battle Axe (3 bars)"}),
        MACE(1, new String[]{"Make Weapon", "Mace"}),
        MEDIUM_HELMET(1, new String[]{"Make Armour", "Helmet", "Medium Helmet"}),
        LARGE_HELMET(2, new String[]{"Make Armour", "Helmet", "Large Helmet (2 bars)"}),
        SQUARE_SHIELD(2, new String[]{"Make Armour", "Shield", "Square Shield (2 bars)"}),
        KITE_SHIELD(3, new String[]{"Make Armour", "Shield", "Kite Shield (3 bars)"}),
        CHAIN_MAIL_BODY(3, new String[]{"Make Armour", "Armour", "Chain mail body (3 bars)"}),
        PLATE_MAIL_BODY(5, new String[]{"Make Armour", "Armour", "Plate mail body (5 bars)"}),
        PLATE_MAIL_LEGS(3, new String[]{"Make Armour", "Armour", "Plate mail legs (3 bars)"}),
        PLATED_SKIRT(3, new String[]{"Make Armour", "Armour", "Plated Skirt (3 bars)"}),
        ARROW_HEADS(1, new String[]{"Make Missile Heads", "Make Arrow Heads."}),
        BRONZE_WIRE(1, new String[]{"Make Craft Item", "Bronze Wire(1 bar)"}),
        NAILS(1, new String[]{"Make Nails"});

        private final int barCount;

        private final String[] menuOptions;

        Product(final int barCount, final String[] menuOptions) {
            this.barCount = barCount;
            this.menuOptions = menuOptions;
        }

        public int getBarCount() {
            return this.barCount;
        }

        public String[] getMenuOptions() {
            return this.menuOptions;
        }
    }

    private enum Area implements RSArea {
        BANK(new Coordinate(147, 498), new Coordinate(153, 506)),
        ANVIL_HOUSE(new Coordinate(145, 510), new Coordinate(148, 516));

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
        BANK_DOORS(64, new Coordinate(150, 507)),
        ANVIL_DOOR(2, new Coordinate(149, 512)),
        ANVIL(50, new Coordinate(148, 513));

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
