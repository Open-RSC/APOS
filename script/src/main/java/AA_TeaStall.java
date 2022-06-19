/**
 * A script for stealing tea at Varrock East.
 * Start script at the Tea Stall with sleeping bag in inventory.
 * <p>
 * Optional Parameter:
 * <anything>
 * <p>
 * Notes:
 * Putting anything in the parameters box will make the account act as a decoy
 * by talking to the Tea Stall merchant, allowing 100% success rates for other accounts.
 * <p>
 *
 * @Author Chomp
 */
public class AA_TeaStall extends AA_Script {
	private static final Coordinate COORDINATE_STAND = new Coordinate(93, 520);

	private static final int NPC_ID_TEA_SELLER = 780;
	private static final int MAXIMUM_FATIGUE = 100;

	private double initialThievingXp;

	private long startTime;
	private long timeout;

	private int prevYCoord;

	private int success;
	private int fail;
	private int talks;

	private boolean decoy;
	private boolean idle;

	public AA_TeaStall(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		decoy = !parameters.isEmpty();
		if (!decoy && !hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}
		initialThievingXp = getSkillExperience(Skill.THIEVING.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (idle) {
			return idle();
		}

		return decoy ? decoy() : thieve();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("tea")) {
			success++;
			timeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("stall..")) {
			fail++;
			timeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK * 5;
		} else if (message.equals("Greetings!")) {
			talks++;
			timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		} else if (message.endsWith("area")) {
			if (decoy) {
				prevYCoord = getPlayerY();
			}

			idle = true;
		}
	}

	private int idle() {
		if (decoy) {
			if (getPlayerY() != prevYCoord) {
				idle = false;
				return 0;
			}

			walkTo(COORDINATE_STAND.getX(), COORDINATE_STAND.getY() - 10);
			return SLEEP_ONE_TICK;
		}

		if (getPlayerX() != COORDINATE_STAND.getX()) {
			idle = false;
			return 0;
		}

		walkTo(COORDINATE_STAND.getX() + 1, COORDINATE_STAND.getY());
		return SLEEP_ONE_TICK;
	}

	private int decoy() {
		if (isOptionMenuOpen()) {
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= timeout) {
			return 0;
		}

		final java.lang.Object teaSeller = getNearestNpcNotTalking(NPC_ID_TEA_SELLER);

		if (teaSeller == null) {
			return 0;
		}

		talkToNpc(teaSeller);
		timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	private int thieve() {
		if (System.currentTimeMillis() <= timeout) {
			return 0;
		}

		if (getFatiguePercent() >= MAXIMUM_FATIGUE) {
			return sleep();
		}

		if (getPlayerX() != COORDINATE_STAND.getX() || getPlayerY() != COORDINATE_STAND.getY()) {
			walkTo(COORDINATE_STAND.getX(), COORDINATE_STAND.getY());
			return SLEEP_ONE_TICK;
		}

		if (getObjectId(Object.TEA_STALL.coordinate.getX(), Object.TEA_STALL.coordinate.getY()) !=
			Object.TEA_STALL.id) {
			return 0;
		}

		bot.displayMessage("@red@Stealing...");
		useObject2(Object.TEA_STALL.coordinate.getX(), Object.TEA_STALL.coordinate.getY());
		timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Tea Stall", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getSkillExperience(Skill.THIEVING.getIndex()) - initialThievingXp;

		bot.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		if (success > 0) {
			final int totalPickpockets = success + fail;

			final double successRate = ((double) success / totalPickpockets) * 100;

			bot.drawString(String.format("@yel@Rate: @whi@%.2f%% @cya@(@whi@%d@cya@/@whi@%d@cya@)",
					successRate, success, totalPickpockets),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}

		if (talks > 0) {
			bot.drawString(String.format("@yel@Talk Count: @whi@%d", talks),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}
	}

	private enum Object implements RSObject {
		TEA_STALL(1183, new Coordinate(91, 518));

		private final int id;
		private final Coordinate coordinate;

		Object(final int id, final Coordinate coordinate) {
			this.id = id;
			this.coordinate = coordinate;
		}

		public int getId() {
			return id;
		}

		public Coordinate getCoordinate() {
			return coordinate;
		}
	}
}
