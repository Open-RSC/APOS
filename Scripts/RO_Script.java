import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;


public abstract class RO_Script extends Script {

    protected static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###.##");

    protected static final int SLEEP_HALF_TICK = 325;
    protected static final int SLEEP_ONE_TICK = 650;
    protected static final int SLEEP_ZERO_SECONDS = 0;
    protected static final int SLEEP_ONE_SECOND = 1000;
    protected static final int SLEEP_TWO_SECONDS = 2000;
    protected static final int SLEEP_FIVE_SECONDS = 5000;

    protected static final int PAINT_OFFSET_Y_FULL_INCREMENT = 14;
    protected static final int PAINT_OFFSET_Y_HALF_INCREMENT = 7;
    protected static final int PAINT_FONT_BOLD = 1;
    protected static final int PAINT_COLOR = 0xFFFFFF;

    private long optionMenuTimeout;

    protected Extension extension;

    protected RO_Script(Extension ex) {
        super(ex);
        this.extension = ex;
    }

    @Override
    public void onServerMessage(final String message) {
        if (message.endsWith("moment")) {
            this.optionMenuTimeout = 0L;
        }
    }

    protected String getScriptName() {
        return getClass().getSimpleName();
    }

    protected static String getCountPerHour(double count, long startTime) {
        double amount, secs;

        if (count == 0)
            return "0";

        amount = count * 60.0 * 60.0;
        secs = (System.currentTimeMillis() - startTime) / 1000.0;
        return DECIMAL_FORMAT.format(amount / secs);
    }

    protected static String getRunningTime(long t) {
        long millis = (System.currentTimeMillis() - t) / 1000;
        long second = millis % 60;
        long minute = (millis / 60) % 60;
        long hour = millis / (60 * 60);

        return String.format("%d:%02d:%02d", hour, minute, second);
    }

    protected Spawn getNextSpawn(Spawn[] spawns) {
        if (spawns.length == 0) {
            throw new IllegalArgumentException("Missing spawns");
        }

        return Arrays.stream(spawns)
                .min(Comparator.comparingLong(o -> o.time))
                .get();
    }

    protected void drawString(int x, int y, String str, Object...args) {
        drawString(String.format(str, args), x, y, PAINT_FONT_BOLD, PAINT_COLOR);
    }

    protected boolean inArea(RSArea...areas) {
        return Arrays.stream(areas).anyMatch(it -> it.contains(getX(), getY()));
    }

    protected boolean inArea(Position position, RSArea...areas) {
        return Arrays.stream(areas).anyMatch(it -> it.contains(position.getX(), position.getY()));
    }

    protected void runCombat() {
        walkTo(getX(), getY());
    }

    protected void walkTo(Position pos) {
        walkTo(pos.getX(), pos.getY());
    }

    protected void useItemOnObjectByPosition(int itemId, RSObject object) {
        useItemOnObject(itemId, object.getPosition().getX(), object.getPosition().getY());
    }

    protected boolean isAtApproxPosition(Position position, int radius) {
        return isAtApproxCoords(position.getX(), position.getY(), radius);
    }

    protected boolean isObjectReachable(RSObject object) {
        return getObjectIdFromCoords(object.getPosition().getX(), object.getPosition().getY()) == object.getId();
    }

    protected void atObject(RSObject object) {
        atObject(object.getPosition().getX(), object.getPosition().getY());
    }

    protected boolean isWallObjectReachable(RSObject object) {
        return getWallObjectIdFromCoords(object.getPosition().getX(), object.getPosition().getY()) == object.getId();
    }

    protected void atWallObject(RSObject object) {
        atWallObject(object.getPosition().getX(), object.getPosition().getY());
    }

    protected boolean isInventoryFull() {
        return getInventoryCount() == MAX_INV_SIZE;
    }

    private int openGenericInterface(final int[] npcs) {
        if (this.isQuestMenu()) {
            this.answer(0);
            this.optionMenuTimeout = System.currentTimeMillis() + SLEEP_FIVE_SECONDS;
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
        this.optionMenuTimeout = System.currentTimeMillis() + SLEEP_FIVE_SECONDS;
        return 0;
    }

    protected int openBank() {
        return openGenericInterface(BANKERS);
    }

    public enum Food {
        NONE(-1, 0, "None"),
        SHRIMP(350, 3, "Shrimp"),
        ANCHOVIES(352, 1, "Anchovies"),
        SARDINE(355, 4, "Sardine"),
        HERRING(362, 5, "Herring"),
        GIANT_CARP(718, 6, "Giant Carp"),
        MACKEREL(553, 6, "Mackerel"),
        TROUT(359, 7, "Trout"),
        COD(551, 7, "Cod"),
        PIKE(364, 8, "Pike"),
        SALMON(357, 9, "Salmon"),
        TUNA(367, 10, "Tuna"),
        LOBSTER(373, 12, "Lobster"),
        BASS(555, 13, "Bass"),
        SWORDFISH(370, 14, "Swordfish"),
        SHARK(546, 20, "Shark"),
        SEA_TURTLE(1193, 20, "Sea Turtle"),
        MANTA_RAY(1191, 20, "Manta Ray");

        private final int id;
        private final int healAmount;
        private final String name;

        Food(final int id, final int healAmount, final String name) {
            this.id = id;
            this.healAmount = healAmount;
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public int getId() {
            return this.id;
        }

        public int getHealAmount() {
            return this.healAmount;
        }
    }

    protected static class RSArea {
        private final Position firstBoundingPosition;
        private final Position secondBoundingPosition;

        public RSArea(Position firstBoundingPosition, Position secondBoundingPosition) {
            this.firstBoundingPosition = firstBoundingPosition;
            this.secondBoundingPosition = secondBoundingPosition;
        }

        public boolean contains(final int x, final int y) {
            Position firstArea = firstBoundingPosition;
            Position secondArea = secondBoundingPosition;

            int minX = Math.min(firstArea.getX(), secondArea.getX());
            int maxX = Math.max(firstArea.getX(), secondArea.getX());

            int minY = Math.min(firstArea.getY(), secondArea.getY());
            int maxY = Math.max(firstArea.getY(), secondArea.getY());

            return minX <= x && x <= maxX && minY <= y && y <= maxY;
        }

        public Position getFirstBoundingPosition() {
            return firstBoundingPosition;
        }

        public Position getSecondBoundingPosition() {
            return secondBoundingPosition;
        }
    }

    protected static class RSObject {
        private final int id;
        private final Position position;

        public RSObject(int id, Position position) {
            this.id = id;
            this.position = position;
        }

        public int getId() {
            return id;
        }

        public Position getPosition() {
            return position;
        }
    }

    protected static class Spawn {
        private final Integer serverIndex;
        private final Position position;
        private long time;

        public Spawn(Position position) {
            this(-1, position);
        }

        public Spawn(Integer serverIndex, Position position) {
            this.serverIndex = serverIndex;
            this.position = position;
        }

        public void updateTime() {
            time = System.currentTimeMillis();
        }

        public Position getPosition() {
            return position;
        }

        public Integer getServerIndex() {
            return serverIndex;
        }
    }

    protected static final class Position {
        private final int x;
        private final int y;

        public Position(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return this.x;
        }

        public int getY() {
            return this.y;
        }

        public Position addX(int x) {
            return new Position(this.x + x, this.y);
        }

        public Position addY(int y) {
            return new Position(this.x, this.y + y);
        }

        @Override
        public String toString() {
            return String.format("(%d, %d)", x, y);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Position position = (Position) o;
            return x == position.x && y == position.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        public static Position createFrom(int[] triple) {
            if (triple[0] == -1) {
                throw new IllegalArgumentException("Missing values");
            }

            return new Position(triple[1], triple[2]);
        }
    }

    protected static final class RSItem {
        private final int id;
        private boolean isSelected;
        private boolean isCountUpdated;

        private int count;

        public RSItem(int id) {
            this(id, true);
        }

        public RSItem(int id, boolean isSelected) {
            this.id = id;
            this.isSelected = isSelected;
        }

        public void resetCountUpdateFlag() {
            isCountUpdated = false;
        }

        public void incrementCount(int count) {
            if (isCountUpdated) {
                return;
            }

            this.count += count;
            isCountUpdated = true;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getId() {
            return id;
        }

        public String getItemName() {
            return getItemNameId(id);
        }

        public boolean isStackable() {
            return isItemStackableId(id);
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }

        public int getCount() {
            return count;
        }
    }

    protected final class Path {
        private final RO_Script script;
        private final Position[] steps;
        private Integer nextStepIndex;

        public Path(RO_Script script, Position...steps) {
            if (steps.length < 1) {
                throw new IllegalArgumentException("Path must have at least 1 step!");
            }
            this.steps = steps;
            this.script = script;
        }

        private double getDistanceToStep(Position stepPosition) {
            int x1 = getX();
            int y1 = getY();
            int x2 = stepPosition.getX();
            int y2 = stepPosition.getY();
            return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
        }

        private Position getFinalStep() {
            return steps[steps.length - 1];
        }

        private Position getNextStep() {
            return steps[nextStepIndex];
        }

        private int getNearestStepIndex() {
            int index = 0;
            double minDistance = Double.MAX_VALUE;
            for (int i = 0; i < steps.length; i++) {
                double dist = getDistanceToStep(steps[i]);
                if (dist < minDistance) {
                    index = i;
                    minDistance = dist;
                }
            }
            return index;
        }

        public int walk() {
            if (nextStepIndex == null) {
                int nearestStepIndex = getNearestStepIndex();
                Position nearestStep = steps[nearestStepIndex];
                if (script.isReachable(nearestStep.x, nearestStep.y)) {
                    nextStepIndex = nearestStepIndex;
                } else {
                    System.out.println("Step " + nearestStep + " is unreachable - stopping script");
                    script.setAutoLogin(false);
                    script.stopScript();
                }
            }

            if (nextStepIndex < steps.length - 1) {
                if (script.inCombat()) {
                    script.runCombat();
                    return 400;
                }

                boolean changed = false;
                Position nextStep = getNextStep();
                if (script.getX() == nextStep.getX() && script.getY() == nextStep.getY()) {
                    ++nextStepIndex;
                    changed = true;
                }

                if (nextStepIndex < steps.length - 1) {
                    if (!script.isWalking() || changed) {
                        script.walkTo(getNextStep());
                    }
                    return SLEEP_ONE_TICK;
                }
            }

            script.walkTo(getFinalStep());
            return SLEEP_ONE_TICK;
        }

        public Path createReversePath(RO_Script script) {
            List<Position> list = Arrays.stream(steps)
                .map(it -> new Position(it.getX(), it.getY()))
                .collect(Collectors.toList());

            Collections.reverse(list);
            return new Path(script, list.toArray(new Position[0]));
        }

        public void reset() {
            nextStepIndex = 0;
        }

        @Override
        public String toString() {
            return Arrays
                .stream(steps)
                .map(Position::toString)
                .reduce("", (partialString, element) -> partialString + " -> " + element);
        }
    }
}
