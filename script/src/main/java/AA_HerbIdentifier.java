import java.util.EnumSet;
import java.util.Iterator;

/**
 * Identifies herbs.
 * <p>
 * Requirements:
 * Start at a bank or Shantay Pass with a sleeping bag in inventory.
 * The script will try to identify all herbs if no herb parameter is provided.
 * <p>
 * Optional Parameter:
 * <guam|ranarr_weed|irit_leaf|...>
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
 *
 * @Author Chomp
 */
public class AA_HerbIdentifier extends AA_Script {
	private static final Coordinate COORDINATE_SHANTAY_BANK_CHEST = new Coordinate(58, 731);

	private static final long IDENTIFY_DELAY = 1210L; // +- based on latency

	private static final int MAXIMUM_FATIGUE = 99;

	private Herb herb;
	private Iterator<Herb> iterator;
	private long startTime;

	private double initialHerblawXp;

	private long openTimeout;
	private long depositTimeout;
	private long withdrawTimeout;
	private long identifyTimeout;

	private int herbsIdentified;
	private int herbsRemaining;

	private boolean shantayBanking;
	private boolean idle;

	public AA_HerbIdentifier(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) herb = Herb.valueOf(parameters.toUpperCase());

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (herb == null) {
			iterator = EnumSet.allOf(Herb.class).iterator();
			herb = iterator.next();
		}

		if (getLevel(Skill.HERBLAW.getIndex()) < herb.level) {
			throw new IllegalStateException(String.format("Lvl %d Herblaw required to identify %s",
				herb.level, getItemNameId(herb.id)));
		}

		shantayBanking = distanceTo(COORDINATE_SHANTAY_BANK_CHEST.getX(), COORDINATE_SHANTAY_BANK_CHEST.getY()) < 10;
		initialHerblawXp = getAccurateXpForLevel(Skill.HERBLAW.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (idle) {
			return idle();
		}

		if (getFatigue() >= MAXIMUM_FATIGUE) {
			return sleep();
		}

		if (System.currentTimeMillis() <= identifyTimeout) {
			return 0;
		}

		if (getInventoryCount() == 1 ||
			getInventoryId(1) != herb.id) {
			return bank();
		}

		bot.displayMessage("@gre@Identifying ...");
		useItem(1);
		identifyTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("herb", 5)) {
			herbsIdentified++;
			if (herbsRemaining > 0) {
				herbsRemaining--;
			}
			identifyTimeout = System.currentTimeMillis() + IDENTIFY_DELAY;
		} else if (message.endsWith("men.") || message.endsWith("you.")) {
			openTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		} else if (message.endsWith("area")) {
			if (!shantayBanking) {
				return;
			}
			idle = true;
		} else {
			super.onServerMessage(message);
		}
	}

	private int idle() {
		if (getX() == COORDINATE_SHANTAY_BANK_CHEST.getX() + 2 &&
			getY() == COORDINATE_SHANTAY_BANK_CHEST.getY()) {
			idle = false;
			return 0;
		}

		walkTo(COORDINATE_SHANTAY_BANK_CHEST.getX() + 2, COORDINATE_SHANTAY_BANK_CHEST.getY());
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (!isBanking()) {
			if (shantayBanking) {
				if (System.currentTimeMillis() <= openTimeout) {
					return 0;
				}

				atObject(COORDINATE_SHANTAY_BANK_CHEST.getX(), COORDINATE_SHANTAY_BANK_CHEST.getY());
				openTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			return openBank();
		}

		if (getInventoryCount() == 1) {
			if (System.currentTimeMillis() <= withdrawTimeout) {
				return 0;
			}

			if (!hasBankItem(herb.id)) {
				if (iterator == null || !iterator.hasNext()) {
					return exit(String.format("Out of %s.", getItemNameId(herb.id)));
				}

				herb = iterator.next();
				herbsIdentified = 0;
				initialHerblawXp = getAccurateXpForLevel(Skill.HERBLAW.getIndex());
				startTime = System.currentTimeMillis();

				if (getLevel(Skill.HERBLAW.getIndex()) < herb.level) {
					return exit(String.format("Lvl %d Herblaw required to identify %s.",
						herb.level, getItemNameId(herb.id)));
				}

				return 0;
			}

			herbsRemaining = bankCount(herb.id);
			withdraw(herb.id, MAX_INV_SIZE - 1);
			withdrawTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= depositTimeout) {
			return 0;
		}

		final int itemId = getInventoryId(1);
		deposit(itemId, MAX_INV_SIZE);
		depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Herb Identifier", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getAccurateXpForLevel(Skill.HERBLAW.getIndex()) - initialHerblawXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				herb, herbsIdentified, toUnitsPerHour(herbsIdentified, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Remaining: @whi@%d", herbsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(herbsIdentified, herbsRemaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum Herb {
		GUAM(165, 3, "Guam"),
		MARRENTILL(435, 5, "Marrentill"),
		TARROMIN(436, 12, "Tarromin"),
		HARRALANDER(437, 22, "Harralander"),
		RANARR_WEED(438, 30, "Ranarr"),
		IRIT_LEAF(439, 45, "Irit"),
		AVANTOE(440, 50, "Avantoe"),
		KWUARM(441, 55, "Kwuarm"),
		CADANTINE(442, 66, "Cadantine"),
		DWARF_WEED(443, 72, "Dwarfweed"),
		TORSTOL(933, 78, "Torstol");

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
			return name;
		}
	}
}
