import java.util.EnumSet;
import java.util.Iterator;

/**
 * Mixes unfinished potions.
 * <p>
 * Requirements:
 * Start at a bank or Shantay Pass with an empty inventory.
 * The script will try to mix all herbs if no herb parameter is provided.
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
public class AA_UnfinishedPotionMixer extends AA_Script {
	private static final Coordinate COORDINATE_SHANTAY_BANK_CHEST = new Coordinate(58, 731);

	private static final long MIX_DELAY = 1210L; // +- based on latency

	private static final int ITEM_ID_WATER_FILLED_VIAL = 464;
	private static final int WITHDRAW_COUNT = 15;

	private Herb herb;
	private Iterator<Herb> iterator;
	private long startTime;

	private long openTimeout;
	private long depositTimeout;
	private long withdrawVialTimeout;
	private long withdrawHerbTimeout;
	private long mixTimeout;

	private int inventoryCount;

	private int potionsMixed;
	private int materialsRemaining;

	private boolean shantayBanking;
	private boolean idle;

	public AA_UnfinishedPotionMixer(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) {
			iterator = EnumSet.allOf(Herb.class).iterator();
			herb = iterator.next();
		} else {
			herb = Herb.valueOf(parameters.toUpperCase());
		}

		if (getLevel(Skill.HERBLAW.getIndex()) < herb.level) {
			throw new IllegalStateException(String.format("Lvl %d Herblaw required to mix %s",
				herb.level, getItemNameId(herb.id)));
		}

		shantayBanking = distanceTo(COORDINATE_SHANTAY_BANK_CHEST.getX(), COORDINATE_SHANTAY_BANK_CHEST.getY()) < 10;
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (idle) {
			return idle();
		}

		if (System.currentTimeMillis() <= mixTimeout) {
			return 0;
		}

		inventoryCount = getInventoryCount();
		final int herbIndex = getInventoryIndex(herb.id);

		if (inventoryCount == 0 ||
			getInventoryId(0) != ITEM_ID_WATER_FILLED_VIAL ||
			herbIndex == -1) {
			return bank();
		}

		bot.displayMessage("@gre@Mixing ...");
		useItemWithItem(0, herbIndex);
		mixTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("water")) {
			potionsMixed++;
			if (materialsRemaining > 0) {
				materialsRemaining--;
			}
			mixTimeout = System.currentTimeMillis() + MIX_DELAY;
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

		if (inventoryCount > 0 &&
			getInventoryId(0) == ITEM_ID_WATER_FILLED_VIAL &&
			inventoryCount < MAX_INV_SIZE) {
			if (System.currentTimeMillis() <= withdrawHerbTimeout) {
				return 0;
			}

			final int herbsRemaining = bankCount(herb.id);

			if (herbsRemaining == 0) {
				if (iterator == null || !iterator.hasNext()) {
					return exit(String.format("Out of %s.", getItemNameId(herb.id)));
				}

				herb = iterator.next();
				potionsMixed = 0;
				startTime = System.currentTimeMillis();

				if (getLevel(Skill.HERBLAW.getIndex()) < herb.level) {
					return exit(String.format("Lvl %d Herblaw required to mix %s.",
						herb.level, getItemNameId(herb.id)));
				}

				return 0;
			}

			final int vialsRemaining = bankCount(ITEM_ID_WATER_FILLED_VIAL);

			materialsRemaining = Math.min(herbsRemaining, vialsRemaining);
			withdraw(herb.id, WITHDRAW_COUNT);
			withdrawHerbTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= withdrawVialTimeout) {
			return 0;
		}

		if (inventoryCount == 0) {
			if (!hasBankItem(ITEM_ID_WATER_FILLED_VIAL)) {
				return exit("Out of water-filled vials.");
			}

			withdraw(ITEM_ID_WATER_FILLED_VIAL, WITHDRAW_COUNT);
			withdrawVialTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= depositTimeout) {
			return 0;
		}

		final int itemId = getInventoryId(0);
		deposit(itemId, MAX_INV_SIZE);
		depositTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Unfinished Potion Mixer", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s pots@cya@/@whi@hr@cya@)",
				herb, potionsMixed, toUnitsPerHour(potionsMixed, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@Remaining: @whi@%d", materialsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(potionsMixed, materialsRemaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum Herb {
		GUAM(444, 3, "Guam"),
		MARRENTILL(445, 5, "Marrentill"),
		TARROMIN(446, 12, "Tarromin"),
		HARRALANDER(447, 22, "Harralander"),
		RANARR_WEED(448, 30, "Ranarr"),
		IRIT_LEAF(449, 45, "Irit"),
		AVANTOE(450, 50, "Avantoe"),
		KWUARM(451, 55, "Kwuarm"),
		CADANTINE(452, 66, "Cadantine"),
		DWARF_WEED(453, 72, "Dwarfweed"),
		TORSTOL(934, 78, "Torstol");

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
