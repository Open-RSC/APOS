import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Kills chickens and collects feathers.
 * <p>
 * Requirements:
 * Start script at a chicken pen and with sleeping bag in inventory.
 * <p>
 * Optional Parameter
 * -m,--fightmode <controlled|attack|strength|defense>
 * <p>
 * Author: Chomp
 */
public class AA_Chickens extends AA_Script {
    private static final int NPC_ID_CHICKEN = 3;
    private static final int ITEM_ID_FEATHER = 381;
    private static final int SKILL_INDEX_ATTACK = 0;
    private static final int SKILL_INDEX_DEFENCE = 1;
    private static final int SKILL_INDEX_STRENGTH = 2;
    private static final int SKILL_INDEX_HITS = 3;

    private final int[] groundItemFeather = new int[4];

    private final Map<Integer, Spawn> spawnMap = new HashMap<>();

    private Coordinate nextRespawn;
    private Instant startTime;
    private ChickenHandler chickenHandler;
    private Location location;

    private double initialCombatXp;

    private int initialFeatherCount;

    public AA_Chickens(final Extension extension) {
        super(extension);
    }

    @Override
    public void init(final String parameters) {
        if (!parameters.isEmpty()) {
            final String[] args = parameters.split(" ");

            for (int i = 0; i < args.length; i++) {
                switch (args[i].toLowerCase()) {
                    case "-m":
                    case "--fightmode":
                        this.combatStyle = CombatStyle.valueOf(args[++i].toUpperCase());
                        break;
                    default:
                        throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
                }
            }
        }

        for (final Location location : Location.values()) {
            if (this.isAtApproxCoords(location.coordinate.getX(), location.coordinate.getY(), 10)) {
                this.location = location;
                break;
            }
        }

        if (this.location == null) {
            throw new IllegalStateException("Location unsupported. Start script at another chicken pen.");
        }

        if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
            throw new IllegalStateException("Sleeping bag missing from inventory.");
        }

        switch (this.location) {
            case ARDOUGNE:
                this.chickenHandler = new ArdougneChickenHandler(this);
                break;
            case CHAMPION:
                this.chickenHandler = new ChampionGuildChickenHandler(this);
                break;
            case DRAYNOR:
                this.chickenHandler = new DraynorChickenHandler(this);
                break;
            case ENTRANA:
                this.chickenHandler = new EntranaChickenHandler(this);
                break;
            case FALADOR:
                this.chickenHandler = new FaladorChickenHandler(this);
                break;
            case HEMENSTER:
                this.chickenHandler = new HemensterChickenHandler(this);
                break;
            case LUMBRIDGE:
                this.chickenHandler = new LumbridgeChickenHandler(this);
                break;
        }

        this.setFightMode(this.combatStyle.getIndex());

        this.initialCombatXp = this.getTotalCombatXp();

        this.initialFeatherCount = this.getInventoryCount(ITEM_ID_FEATHER);

        this.startTime = Instant.now();
    }

    @Override
    public int main() {
        if (this.inCombat()) {
            return 0;
        }

        if (this.fatigued) {
            return this.sleep();
        }

        final int[] chicken = this.getNpcById(NPC_ID_CHICKEN);

        if (chicken[0] != -1) {
            return this.chickenHandler.kill(chicken);
        }

        this.scanGroundItems();

        if (this.groundItemFeather[0] != -1) {
            this.pickupItem(this.groundItemFeather[3], this.groundItemFeather[1], this.groundItemFeather[2]);
            return SLEEP_ONE_TICK;
        }

        if (this.nextRespawn != null &&
                (this.getX() != this.nextRespawn.getX() || this.getY() != this.nextRespawn.getY())) {
            this.walkTo(this.nextRespawn.getX(), this.nextRespawn.getY());
            return SLEEP_ONE_TICK;
        }

        return 0;
    }

    @Override
    public void paint() {
        int y = PAINT_OFFSET_Y;

        this.drawString("@yel@Chickens", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

        if (this.startTime == null) {
            return;
        }

        final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

        this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        final double gainedXp = this.getTotalCombatXp() - this.initialCombatXp;

        this.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
                        DECIMAL_FORMAT.format(gainedXp), getUnitsPerHour(gainedXp, secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        final int totalFeathers = this.getInventoryCount(ITEM_ID_FEATHER);
        final int gainedFeathers = totalFeathers - this.initialFeatherCount;

        this.drawString(String.format("@yel@Feathers: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
                        totalFeathers, getUnitsPerHour(gainedFeathers, secondsElapsed)),
                PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

        if (this.nextRespawn != null) {
            this.drawString(String.format("@yel@Next spawn: @cya@(@whi@%d@cya@, @whi@%d@cya@)", this.nextRespawn.getX(), this.nextRespawn.getY()),
                    PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
        }
    }

    @Override
    public void onNpcSpawned(final java.lang.Object npc) {
        if (this.extension.getNpcId(npc) != NPC_ID_CHICKEN) {
            return;
        }

        final int npcX = this.extension.getMobLocalX(npc) + this.extension.getAreaX();
        final int npcY = this.extension.getMobLocalY(npc) + this.extension.getAreaY();

        if (!this.chickenHandler.isNpcSpawned(npcX, npcY)) {
            return;
        }

        final int serverIndex = this.extension.getMobServerIndex(npc);

        final Spawn spawn = this.spawnMap.get(serverIndex);

        if (spawn != null) {
            spawn.getCoordinate().set(npcX, npcY);
            spawn.setTimestamp(System.currentTimeMillis());
        } else {
            this.spawnMap.put(serverIndex, new Spawn(new Coordinate(npcX, npcY), System.currentTimeMillis()));
        }

        this.nextRespawn = this.spawnMap.isEmpty() ? null : this.spawnMap.values().stream().sorted().findFirst().get().getCoordinate();
    }

    private void scanGroundItems() {
        this.groundItemFeather[0] = -1;

        int currentDistance = Integer.MAX_VALUE;

        for (int index = 0; index < this.extension.getGroundItemCount(); index++) {
            final int groundItemId = this.extension.getGroundItemId(index);

            if (groundItemId != ITEM_ID_FEATHER) {
                continue;
            }

            final int groundItemX = this.extension.getGroundItemLocalX(index) + this.extension.getAreaX();
            final int groundItemY = this.extension.getGroundItemLocalY(index) + this.extension.getAreaY();

            if (!this.chickenHandler.isLootReachable(groundItemX, groundItemY)) {
                continue;
            }

            final int distance = this.distanceTo(groundItemX, groundItemY);

            if (distance < currentDistance) {
                this.groundItemFeather[0] = index;
                this.groundItemFeather[1] = groundItemX;
                this.groundItemFeather[2] = groundItemY;
                this.groundItemFeather[3] = groundItemId;

                currentDistance = distance;
            }
        }
    }

    private enum Location {
        ARDOUGNE(new Coordinate(525, 617)),
        CHAMPION(new Coordinate(146, 560)),
        DRAYNOR(new Coordinate(159, 615)),
        ENTRANA(new Coordinate(407, 546)),
        FALADOR(new Coordinate(272, 604)),
        HEMENSTER(new Coordinate(559, 493)),
        LUMBRIDGE(new Coordinate(117, 607));

        private final Coordinate coordinate;

        Location(final Coordinate coordinate) {
            this.coordinate = coordinate;
        }

        @Override
        public String toString() {
            return this.name().charAt(0) + this.name().substring(1).toLowerCase();
        }
    }

    private static abstract class ChickenHandler {
        protected final Script script;

        public ChickenHandler(final Script script) {
            this.script = script;
        }

        abstract boolean isLootReachable(final int x, final int y);

        abstract boolean isNpcSpawned(final int x, final int y);

        abstract int kill(int[] npc);

        protected final boolean isGateOpen(final RSObject object) {
            return this.script.getObjectIdFromCoords(object.getCoordinate().getX(), object.getCoordinate().getY()) != object.getId();
        }

        protected final boolean isDoorOpen(final RSObject object) {
            return this.script.getWallObjectIdFromCoords(object.getCoordinate().getX(), object.getCoordinate().getY()) != object.getId();
        }
    }

    private static class ArdougneChickenHandler extends ChickenHandler {
        public ArdougneChickenHandler(final Script controller) {
            super(controller);
        }

        @Override
        public boolean isLootReachable(final int x, final int y) {
            return Area.PEN.contains(x, y);
        }

        @Override
        boolean isNpcSpawned(final int x, final int y) {
            return Area.PEN.contains(x, y);
        }

        @Override
        public int kill(final int[] npc) {
            this.script.attackNpc(npc[0]);
            return SLEEP_ONE_TICK;
        }

        private enum Area implements RSArea {
            PEN(new Coordinate(521, 612), new Coordinate(529, 622));

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
    }

    private static class ChampionGuildChickenHandler extends ChickenHandler {
        private int playerX;

        private int playerY;

        public ChampionGuildChickenHandler(final Script script) {
            super(script);
        }

        @Override
        public boolean isLootReachable(final int x, final int y) {
            return Area.PEN.contains(x, y) || Area.GUILD.contains(x, y);
        }

        @Override
        boolean isNpcSpawned(final int x, final int y) {
            return Area.PEN.contains(x, y);
        }

        @Override
        public int kill(final int[] npc) {
            this.playerX = this.script.getX();
            this.playerY = this.script.getY();

            if (this.needToOpenEastDoor(npc)) {
                this.script.atWallObject(Object.DOOR_EAST.coordinate.getX(), Object.DOOR_EAST.coordinate.getY());
                return SLEEP_ONE_SECOND;
            }

            if (this.needToOpenSouthDoor(npc)) {
                this.script.atWallObject(Object.DOOR_SOUTH.coordinate.getX(), Object.DOOR_SOUTH.coordinate.getY());
                return SLEEP_ONE_SECOND;
            }

            this.script.attackNpc(npc[0]);
            return SLEEP_ONE_TICK;
        }

        private boolean needToOpenEastDoor(final int[] npc) {
            if (this.isDoorOpen(Object.DOOR_EAST) ||
                    (!this.isDoorOpen(Object.DOOR_SOUTH) && Area.GUILD_SOUTH.contains(this.playerX, this.playerY))) {
                return false;
            }

            final boolean playerInGuild = Area.GUILD.contains(this.playerX, this.playerY);
            final boolean chickenInGuild = Area.GUILD.contains(npc[1], npc[2]);

            return playerInGuild != chickenInGuild;
        }

        private boolean needToOpenSouthDoor(final int[] npc) {
            if (this.isDoorOpen(Object.DOOR_SOUTH)) {
                return false;
            }

            final boolean playerInGuild = Area.GUILD_SOUTH.contains(this.playerX, this.playerY);
            final boolean chickenInGuild = Area.GUILD_SOUTH.contains(npc[1], npc[2]);

            return playerInGuild != chickenInGuild;
        }

        private enum Area implements RSArea {
            PEN(new Coordinate(145, 557), new Coordinate(147, 562)),
            GUILD(new Coordinate(148, 554), new Coordinate(152, 562)),
            GUILD_NORTH(new Coordinate(148, 554), new Coordinate(152, 560)),
            GUILD_SOUTH(new Coordinate(148, 561), new Coordinate(151, 562));

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
            DOOR_EAST(2, new Coordinate(148, 558)),
            DOOR_SOUTH(2, new Coordinate(150, 561));

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

    private static class DraynorChickenHandler extends ChickenHandler {
        private int playerX;

        private int playerY;

        public DraynorChickenHandler(final Script script) {
            super(script);
        }

        @Override
        public boolean isLootReachable(final int x, final int y) {
            return Area.LOOT.contains(x, y);
        }

        @Override
        boolean isNpcSpawned(final int x, final int y) {
            return Area.PEN.contains(x, y);
        }

        @Override
        public int kill(final int[] npc) {
            this.playerX = this.script.getX();
            this.playerY = this.script.getY();

            if (this.needToOpenNorthGate(npc)) {
                this.script.atObject(Object.GATE_NORTH.coordinate.getX(), Object.GATE_NORTH.coordinate.getY());
                return SLEEP_ONE_SECOND;
            }

            if (this.needToOpenPenGate(npc)) {
                this.script.atObject(Object.GATE_PEN.coordinate.getX(), Object.GATE_PEN.coordinate.getY());
                return SLEEP_ONE_SECOND;
            }

            if (this.needToOpenEastGate(npc)) {
                this.script.atObject(Object.GATE_EAST.coordinate.getX(), Object.GATE_EAST.coordinate.getY());
                return SLEEP_ONE_SECOND;
            }

            if (this.needToOpenDoor(npc)) {
                this.script.atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
                return SLEEP_ONE_SECOND;
            }

            this.script.attackNpc(npc[0]);
            return SLEEP_ONE_TICK;
        }

        private boolean needToOpenNorthGate(final int[] npc) {
            if (this.isGateOpen(Object.GATE_NORTH) ||
                    (!this.isGateOpen(Object.GATE_PEN) && (Area.PEN.contains(this.playerX, this.playerY) || Area.HOUSE.contains(this.playerX, this.playerY))) ||
                    (!this.isDoorOpen(Object.DOOR) && Area.HOUSE.contains(this.playerX, this.playerY)) ||
                    (!this.isGateOpen(Object.GATE_EAST) && Area.FIELD_EAST.contains(this.playerX, this.playerY))) {
                return false;
            }

            final boolean playerInNorthField = Area.FIELD_NORTH.contains(this.playerX, this.playerY);
            final boolean npcInNorthField = Area.FIELD_NORTH.contains(npc[1], npc[2]);

            return playerInNorthField != npcInNorthField;
        }

        private boolean needToOpenPenGate(final int[] npc) {
            if (this.isGateOpen(Object.GATE_PEN) ||
                    (!this.isDoorOpen(Object.DOOR) && Area.HOUSE.contains(this.playerX, this.playerY)) ||
                    (!this.isGateOpen(Object.GATE_EAST) && Area.FIELD_EAST.contains(this.playerX, this.playerY))) {
                return false;
            }

            final int npcX = npc[1];
            final int npcY = npc[2];

            final boolean playerInPen = Area.PEN.contains(this.playerX, this.playerY) || Area.HOUSE.contains(this.playerX, this.playerY);
            final boolean npcInPen = Area.PEN.contains(npcX, npcY) || Area.HOUSE.contains(npcX, npcY);

            return playerInPen != npcInPen;
        }

        private boolean needToOpenEastGate(final int[] npc) {
            if (this.isGateOpen(Object.GATE_EAST) ||
                    (!this.isDoorOpen(Object.DOOR) && Area.HOUSE.contains(this.playerX, this.playerY))) {
                return false;
            }

            final boolean playerInPen = Area.FIELD_EAST.contains(this.playerX, this.playerY);
            final boolean npcInPen = Area.FIELD_EAST.contains(npc[1], npc[2]);

            return playerInPen != npcInPen;
        }

        private boolean needToOpenDoor(final int[] npc) {
            if (this.isDoorOpen(Object.DOOR)) {
                return false;
            }

            final boolean playerInHouse = Area.HOUSE.contains(this.playerX, this.playerY);
            final boolean npcInHouse = Area.HOUSE.contains(npc[1], npc[2]);

            return playerInHouse != npcInHouse;
        }

        private enum Area implements RSArea {
            PEN(new Coordinate(157, 615), new Coordinate(161, 616)),
            HOUSE(new Coordinate(157, 617), new Coordinate(161, 620)),
            FIELD_NORTH(new Coordinate(157, 607), new Coordinate(163, 610)) {
                @Override
                public boolean contains(final int x, final int y) {
                    return super.contains(x, y) || (y == 611 && x >= 157 && x <= 160);
                }
            },
            FIELD_EAST(new Coordinate(150, 616), new Coordinate(156, 625)) {
                @Override
                public boolean contains(final int x, final int y) {
                    return super.contains(x, y) || (x >= 148 && x <= 149 && y >= 618 && y <= 625);
                }
            },
            LOOT(new Coordinate(148, 607), new Coordinate(169, 625));

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
            DOOR(2, new Coordinate(159, 617)),
            GATE_NORTH(60, new Coordinate(158, 612)),
            GATE_PEN(60, new Coordinate(158, 614)),
            GATE_EAST(60, new Coordinate(152, 615));

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

    private static class EntranaChickenHandler extends ChickenHandler {
        public EntranaChickenHandler(final Script script) {
            super(script);
        }

        @Override
        public boolean isLootReachable(final int x, final int y) {
            return Area.PEN.contains(x, y);
        }

        @Override
        boolean isNpcSpawned(final int x, final int y) {
            return Area.PEN.contains(x, y);
        }

        @Override
        public int kill(final int[] npc) {
            if (this.needToOpenDoor(npc)) {
                this.script.atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
                return SLEEP_ONE_SECOND;
            }

            this.script.attackNpc(npc[0]);
            return SLEEP_ONE_TICK;
        }

        private boolean needToOpenDoor(final int[] npc) {
            if (this.isDoorOpen(Object.DOOR)) {
                return false;
            }

            final boolean playerInHouse = Area.HOUSE.contains(this.script.getX(), this.script.getY());
            final boolean chickenInHouse = Area.HOUSE.contains(npc[1], npc[2]);

            return playerInHouse != chickenInHouse;
        }

        private enum Area implements RSArea {
            PEN(new Coordinate(405, 545), new Coordinate(412, 550)),
            HOUSE(new Coordinate(408, 547), new Coordinate(412, 550));

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
            DOOR(2, new Coordinate(408, 549));

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

    private static class FaladorChickenHandler extends ChickenHandler {
        private int playerX;

        private int playerY;

        public FaladorChickenHandler(final Script script) {
            super(script);
        }

        @Override
        public boolean isLootReachable(final int x, final int y) {
            return Area.PEN.contains(x, y) || Area.HOUSE.contains(x, y);
        }

        @Override
        boolean isNpcSpawned(final int x, final int y) {
            return Area.PEN.contains(x, y);
        }

        @Override
        public int kill(final int[] npc) {
            this.playerX = this.script.getX();
            this.playerY = this.script.getY();

            if (this.needToOpenGate(npc)) {
                this.script.atObject(Object.GATE.coordinate.getX(), Object.GATE.coordinate.getY());
                return SLEEP_ONE_SECOND;
            }

            if (this.needToOpenDoor(npc)) {
                this.script.atWallObject(Object.DOOR.coordinate.getX(), Object.DOOR.coordinate.getY());
                return SLEEP_ONE_SECOND;
            }

            this.script.attackNpc(npc[0]);
            return SLEEP_ONE_TICK;
        }

        private boolean needToOpenGate(final int[] npc) {
            if (this.isGateOpen(Object.GATE) ||
                    (!this.isDoorOpen(Object.DOOR) && Area.HOUSE.contains(this.playerX, this.playerY))) {
                return false;
            }

            final boolean playerOutsideGate = this.isOutsideGate(this.playerX, this.playerY);
            final boolean chickenOutsideGate = this.isOutsideGate(npc[1], npc[2]);

            return playerOutsideGate != chickenOutsideGate;
        }

        private boolean needToOpenDoor(final int[] npc) {
            if (this.isDoorOpen(Object.DOOR) ||
                    (!this.isGateOpen(Object.GATE) && this.isOutsideGate(this.playerX, this.playerY))) {
                return false;
            }

            final boolean playerInHouse = Area.HOUSE.contains(this.playerX, this.playerY);
            final boolean chickenInHouse = Area.HOUSE.contains(npc[1], npc[2]);

            return playerInHouse != chickenInHouse;
        }

        private boolean isOutsideGate(final int x, final int y) {
            return !Area.PEN.contains(x, y) && !Area.HOUSE.contains(x, y);
        }

        private enum Area implements RSArea {
            PEN(new Coordinate(271, 600), new Coordinate(273, 607)) {
                @Override
                public boolean contains(final int x, final int y) {
                    return super.contains(x, y) || (x >= 269 && x <= 270 && y >= 605 && y <= 607);
                }
            },
            HOUSE(new Coordinate(264, 600), new Coordinate(270, 604));

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
            GATE(60, new Coordinate(274, 603)),
            DOOR(2, new Coordinate(271, 602));

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

    private static class HemensterChickenHandler extends ChickenHandler {
        private int playerX;

        private int playerY;

        public HemensterChickenHandler(final Script script) {
            super(script);
        }

        @Override
        public boolean isLootReachable(final int x, final int y) {
            return Area.PEN.contains(x, y);
        }

        @Override
        boolean isNpcSpawned(final int x, final int y) {
            return Area.PEN.contains(x, y);
        }

        @Override
        public int kill(final int[] npc) {
            this.playerX = this.script.getX();
            this.playerY = this.script.getY();

            if (this.needToOpenNorthDoor(npc)) {
                this.script.atWallObject(Object.DOOR_NORTH.coordinate.getX(), Object.DOOR_NORTH.coordinate.getY());
                return SLEEP_ONE_SECOND;
            }

            if (this.needToOpenSouthDoor(npc)) {
                this.script.atWallObject(Object.DOOR_SOUTH.coordinate.getX(), Object.DOOR_SOUTH.coordinate.getY());
                return SLEEP_ONE_SECOND;
            }

            this.script.attackNpc(npc[0]);
            return SLEEP_ONE_TICK;
        }

        private boolean needToOpenNorthDoor(final int[] npc) {
            if (this.isDoorOpen(Object.DOOR_NORTH) ||
                    (Area.HOUSE_SOUTH.contains(this.playerX, this.playerY) && !this.isDoorOpen(Object.DOOR_SOUTH))) {
                return false;
            }

            final boolean playerInHouse = Area.HOUSE_NORTH.contains(this.playerX, this.playerY);
            final boolean chickenInHouse = Area.HOUSE_NORTH.contains(npc[1], npc[2]);

            return playerInHouse != chickenInHouse;
        }

        private boolean needToOpenSouthDoor(final int[] npc) {
            if (this.isDoorOpen(Object.DOOR_SOUTH)) {
                return false;
            }

            final boolean playerInHouse = Area.HOUSE_SOUTH.contains(this.playerX, this.playerY);
            final boolean chickenInHouse = Area.HOUSE_SOUTH.contains(npc[1], npc[2]);

            return playerInHouse != chickenInHouse;
        }

        private enum Area implements RSArea {
            PEN(new Coordinate(549, 482), new Coordinate(566, 504)),
            HOUSE_NORTH(new Coordinate(558, 483), new Coordinate(562, 486)),
            HOUSE_SOUTH(new Coordinate(562, 498), new Coordinate(566, 502));

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
            DOOR_NORTH(2, new Coordinate(560, 487)),
            DOOR_SOUTH(2, new Coordinate(562, 498));

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

    private static class LumbridgeChickenHandler extends ChickenHandler {
        public LumbridgeChickenHandler(final Script script) {
            super(script);
        }

        @Override
        public boolean isLootReachable(final int x, final int y) {
            return Area.PEN.contains(x, y);
        }

        @Override
        boolean isNpcSpawned(final int x, final int y) {
            return Area.PEN.contains(x, y);
        }

        @Override
        public int kill(final int[] npc) {
            if (this.needToOpenGate(npc)) {
                this.script.atObject(Object.GATE.coordinate.getX(), Object.GATE.coordinate.getY());
                return SLEEP_ONE_SECOND;
            }

            this.script.attackNpc(npc[0]);
            return SLEEP_ONE_TICK;
        }

        private boolean needToOpenGate(final int[] npc) {
            if (this.isGateOpen(Object.GATE)) {
                return false;
            }

            final boolean playerInPen = Area.PEN.contains(this.script.getX(), this.script.getY());
            final boolean chickenInPen = Area.PEN.contains(npc[1], npc[2]);

            return playerInPen != chickenInPen;
        }

        private enum Area implements RSArea {
            PEN(new Coordinate(115, 603), new Coordinate(122, 612));

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
            GATE(60, new Coordinate(114, 608));

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
}
