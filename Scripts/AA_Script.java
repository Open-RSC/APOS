import java.text.DecimalFormat;

public abstract class AA_Script extends Script {
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0");

    public static final int[] NPC_IDS_BANKER = new int[]{95, 224, 268, 485, 540, 617};

    public static final int ITEM_ID_SLEEPING_BAG = 1263;

    public static final long TIMEOUT_HALF_TICK = 325L;
    public static final long TIMEOUT_ONE_TICK = 650L;
    public static final long TIMEOUT_ONE_SECOND = 1000L;
    public static final long TIMEOUT_TWO_SECONDS = 2000L;
    public static final long TIMEOUT_THREE_SECONDS = 3000L;
    public static final long TIMEOUT_FIVE_SECONDS = 5000L;
    public static final long TIMEOUT_TEN_SECONDS = 10000L;

    public static final int SLEEP_HALF_TICK = 325;
    public static final int SLEEP_ONE_TICK = 650;
    public static final int SLEEP_ONE_SECOND = 1000;
    public static final int SLEEP_TWO_SECONDS = 2000;
    public static final int SLEEP_THREE_SECONDS = 3000;
    public static final int SLEEP_FIVE_SECONDS = 5000;

    public static final int PAINT_OFFSET_X = 312;
    public static final int PAINT_OFFSET_Y = 48;
    public static final int PAINT_OFFSET_Y_INCREMENT = 14;
    public static final int PAINT_COLOR = 0xFFFFFF;

    protected final Extension extension;
    protected boolean fatigued;
    private long optionMenuTimeout;

    public AA_Script(final Extension extension) {
        super(extension);
        this.extension = extension;
    }

    public static String getFormattedElapsedSeconds(final long seconds) {
        return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
    }

    public static String getFormattedUnitsPerHour(final double processed, final long seconds) {
        return processed == 0 ? "0" : DECIMAL_FORMAT.format((processed * 60.0 * 60.0) / seconds);
    }

    public static String getFormattedTTL(final double processed, final int remaining, final long elapsedSeconds) {
        return processed == 0 ? "∞" : getFormattedElapsedSeconds((long) ((remaining / ((processed * 60 * 60) / elapsedSeconds)) * 60 * 60));
    }

    @Override
    public abstract void init(final String parameters);

    @Override
    public abstract int main();

    @Override
    public void onServerMessage(final String message) {
        if (message.endsWith("moment")) {
            this.optionMenuTimeout = 0L;
        } else if (message.endsWith("rest!") || message.startsWith("tired", 12)) {
            this.fatigued = true;
        }
    }

    @Override
    public void onDeath() {
        this.setAutoLogin(false);
        this.stopScript();
        System.out.println("Oh dear, you are dead.");
    }

    @Override
    public final String toString() {
        return this.getClass().getSimpleName();
    }

    protected double getTotalCombatXp() {
        int total = 0;

        for (int i = 0; i < 4; i++) {
            total += this.getAccurateXpForLevel(i);
        }

        return total;
    }

    protected final int openBank() {
        return this.openGenericInterface(NPC_IDS_BANKER);
    }

    protected final int openShop(final int... shopkeepers) {
        return this.openGenericInterface(shopkeepers);
    }

    protected final int sleep() {
        if (this.getFatigue() == 0) {
            this.fatigued = false;
            return 0;
        }

        final int index = this.getInventoryIndex(ITEM_ID_SLEEPING_BAG);

        if (index == -1) {
            this.setAutoLogin(false);
            this.stopScript();
            System.out.println("Sleeping bag missing from inventory.");
        }

        this.useItem(index);
        return SLEEP_ONE_SECOND;
    }

    private int openGenericInterface(final int[] npcs) {
        if (this.isQuestMenu()) {
            this.answer(0);
            this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
            return 0;
        }

        if (System.currentTimeMillis() <= this.optionMenuTimeout) {
            return 0;
        }

        final int[] npc = this.getNpcByIdNotTalk(npcs);

        if (npc[0] == -1) {
            return 0;
        }

        if (this.distanceTo(npc[1], npc[2]) > 2) {
            this.walkTo(npc[1], npc[2]);
            return SLEEP_ONE_TICK;
        }

        this.talkToNpc(npc[0]);
        this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
        return 0;
    }

    public interface RSObject {
        int getId();

        Coordinate getCoordinate();
    }

    public interface RSArea {
        Coordinate getLowerBoundingCoordinate();

        Coordinate getUpperBoundingCoordinate();

        default boolean contains(final int x, final int y) {
            return x >= this.getLowerBoundingCoordinate().getX() && x <= this.getUpperBoundingCoordinate().getX() &&
                    y >= this.getLowerBoundingCoordinate().getY() && y <= this.getUpperBoundingCoordinate().getY();
        }
    }

    public static final class Coordinate {
        private int x;

        private int y;

        public Coordinate(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        public boolean equals(final Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Coordinate)) {
                return false;
            }
            final Coordinate other = (Coordinate) o;
            if (this.getX() != other.getX()) {
                return false;
            }
            return this.getY() == other.getY();
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + this.getX();
            result = result * PRIME + this.getY();
            return result;
        }

        public String toString() {
            return "AA_Script.Coordinate(x=" + this.getX() + ", y=" + this.getY() + ")";
        }

        public void set(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return this.x;
        }

        public void setX(final int x) {
            this.x = x;
        }

        public int getY() {
            return this.y;
        }

        public void setY(final int y) {
            this.y = y;
        }
    }
}
