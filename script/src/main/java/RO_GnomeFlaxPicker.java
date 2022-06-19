/**
 * Gnome Stronghold flax picker and banker.
 * Picks flax from under the bank tree.
 *
 * Author: Rene Ott
 */
public final class RO_GnomeFlaxPicker extends RO_Script {

    private final RSObject LADDER_DOWN = new RSObject(6, new Position(714, 1460));
    private final RSObject LADDER_UP = new RSObject(5, new Position(714, 516));
    private final RSObject GROUND_FLAX = new RSObject(313, new Position(712, 517));
    private final RSItem FLAX = new RSItem(675);

    private static final int PAINT_OFFSET_X = 312;
    private static final int PAINT_OFFSET_Y = 50;

    private long startTime;

    public RO_GnomeFlaxPicker(Extension ex) {
        super(ex);
    }

    @Override
    public void init(String params) {
        startTime = System.currentTimeMillis();
    }

    @Override
    public int main() {
        switch (getState()) {
            case PICK:
                return pickFlax();
            case WALK_TO_BANK:
                return walkToBank();
            case BANK:
                return bank();
            case WALK_TO_PICK:
                return walkToPick();
            default:
                throw new IllegalStateException("Invalid state");
        }
    }

    private int walkToPick() {
        if (isObjectReachable(LADDER_DOWN)) {
            atObject(LADDER_DOWN);
        }
        return SLEEP_ONE_SECOND;
    }

    private int bank() {
        if (isBanking() && isInventoryEmptyOfFlax()) {
            FLAX.resetCountUpdateFlag();
            closeBank();
            return SLEEP_ONE_TICK;
        }

        if (isBanking()) {
            int inventoryCount = getInventoryCount(FLAX.getId());
            if (inventoryCount > 0) {
                FLAX.incrementCount(inventoryCount);
                deposit(FLAX.getId(), inventoryCount);
            }
            return SLEEP_ONE_TICK;
        }

        return openBank();
    }

    private int walkToBank() {
        if (isObjectReachable(LADDER_UP)) {
            atObject(LADDER_UP);
        }
        return SLEEP_ONE_SECOND;
    }

    private int pickFlax() {
        if (isObjectReachable(GROUND_FLAX)) {
            atObject(GROUND_FLAX);
        }
        return SLEEP_HALF_TICK;
    }

    private State getState() {
        if (onBankFloor()) {
            if (isInventoryEmptyOfFlax() && !isBanking()) {
                return State.WALK_TO_PICK;
            } else {
                return State.BANK;
            }
        } else if (isInventoryFull()) {
            return State.WALK_TO_BANK;
        } else if (onGroundFloor()) {
            return State.PICK;
        } else {
            return State.WALK_TO_PICK;
        }
    }

    @Override
    public void paint() {
        int x = PAINT_OFFSET_X;
        int y = PAINT_OFFSET_Y;

        drawString(x, y, "@gre@%s (@whi@%s@gre@)", getScriptName(), getRunningTime(startTime));

        y += PAINT_OFFSET_Y_HALF_INCREMENT;
        drawHLine(x, y, 180, PAINT_COLOR);
        y += PAINT_OFFSET_Y_FULL_INCREMENT;

        drawString(x, y, "@gre@Flax banked: @whi@%s @gre@(@whi@%s/h@gre@)",
                FLAX.getCount(), getCountPerHour(FLAX.getCount(), startTime));
    }

    private boolean isInventoryEmptyOfFlax() {
        return getInventoryCount(FLAX.getId()) == 0;
    }

    private boolean onGroundFloor() {
        return getY() < 1000;
    }

    private boolean onBankFloor() {
        return !onGroundFloor();
    }

    private enum State {
        BANK,
        WALK_TO_BANK,
        WALK_TO_PICK,
        PICK
    }
}