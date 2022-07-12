/**
 * Enchants amulets.
 * <p>
 * Requirements:
 * Start at a bank or Shantay Pass with sleeping bag, staff, cosmic runes.
 * <p>
 * Required Parameter:
 * <sapphire|emerald|ruby|diamond|dragonstone>
 * <p>
 *
 * @Author Chomp
 */
public class AA_AmuletEnchanter extends AA_Script {
	private static final Coordinate COORD_CHEST = new Coordinate(58, 731);

	private static final long CAST_DELAY = 1280L; // +- based on latency

	private static final int ITEM_ID_COSMIC_RUNE = 46;
	private static final int ITEM_ID_WATER_RUNE = 32;
	private static final int ITEM_ID_EARTH_RUNE = 34;
	private static final int INV_IDX_COSMIC_RUNE = 2;
	private static final int INV_IDX_DRAGONSTONE_RUNE = 3;
	private static final int DRAGONSTONE_RUNE_COUNT = 15;
	private static final int MAXIMUM_FATIGUE = 99;

	private Amulet amulet;
	private long startTime;

	private double initialMagicXp;

	private long openTimeout;
	private long depositTimeout;
	private long withdrawTimeout;
	private long castTimeout;

	private int initialInventoryCount;

	private int amuletsEnchanted;
	private int materialsRemaining;

	private boolean shantayBanking;
	private boolean idle;

	public AA_AmuletEnchanter(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) printInstructions();

		amulet = Amulet.valueOf(parameters.toUpperCase());

		if (getInventoryIndex(ITEM_ID_SLEEPING_BAG) != 0) {
			throw new IllegalStateException("Sleeping bag missing from 1st inv slot.");
		}

		final int staffIndex = getInventoryIndex(amulet.staffIds);

		if (staffIndex != 1 || !isItemEquipped(staffIndex)) {
			throw new IllegalStateException("Staff unequipped/missing from 2nd inv slot.");
		}

		if (getInventoryIndex(ITEM_ID_COSMIC_RUNE) != INV_IDX_COSMIC_RUNE) {
			throw new IllegalStateException("Cosmic runes missing from 3rd inv slot.");
		}

		initialInventoryCount = amulet == Amulet.DRAGONSTONE ? 4 : 3;
		shantayBanking = distanceTo(COORD_CHEST.getX(), COORD_CHEST.getY()) < 10;
		initialMagicXp = getAccurateXpForLevel(Skill.MAGIC.getIndex());
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

		if (System.currentTimeMillis() <= castTimeout) {
			return 0;
		}

		if (getInventoryId(INV_IDX_COSMIC_RUNE) != ITEM_ID_COSMIC_RUNE) {
			return exit("Out of cosmic runes.");
		}

		if (amulet == Amulet.DRAGONSTONE) {
			final int runeId = getInventoryId(INV_IDX_DRAGONSTONE_RUNE);

			if (runeId != ITEM_ID_WATER_RUNE && runeId != ITEM_ID_EARTH_RUNE) {
				return exit("Out of water/earth runes.");
			}
		}

		if (getInventoryCount() == initialInventoryCount ||
			getInventoryId(initialInventoryCount) != amulet.id) {
			return bank();
		}

		bot.displayMessage("@gre@Casting ...");
		castOnItem(amulet.spellId, initialInventoryCount);
		castTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("amulet")) {
			amuletsEnchanted++;

			if (materialsRemaining > 0) {
				materialsRemaining--;
			}

			castTimeout = System.currentTimeMillis() + CAST_DELAY;
		} else if (message.startsWith("reagents", 23)) {
			exit("Misconfigured runes in inventory.");
		} else if (message.endsWith("spell")) {
			castTimeout = 0L;
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
		if (getX() == COORD_CHEST.getX() + 2 &&
			getY() == COORD_CHEST.getY()) {
			idle = false;
			return 0;
		}

		walkTo(COORD_CHEST.getX() + 2, COORD_CHEST.getY());
		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (!isBanking()) {
			if (shantayBanking) {
				if (System.currentTimeMillis() <= openTimeout) {
					return 0;
				}

				atObject(COORD_CHEST.getX(), COORD_CHEST.getY());
				openTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			return openBank();
		}

		if (getInventoryCount() == initialInventoryCount) {
			if (System.currentTimeMillis() <= withdrawTimeout) {
				return 0;
			}

			final int amuletsRemaining = bankCount(amulet.id);

			if (amuletsRemaining == 0) {
				return exit(String.format("Out of %ss.", getItemNameId(amulet.id)));
			}

			final int cosmicsRemaining = getInventoryStack(INV_IDX_COSMIC_RUNE);

			final int runesRemaining;

			if (amulet == Amulet.DRAGONSTONE) {
				final int dragonStoneRunesRemaining = getInventoryStack(INV_IDX_DRAGONSTONE_RUNE);
				runesRemaining = Math.min(cosmicsRemaining, dragonStoneRunesRemaining / DRAGONSTONE_RUNE_COUNT);
			} else {
				runesRemaining = cosmicsRemaining;
			}

			materialsRemaining = Math.min(amuletsRemaining, runesRemaining);

			withdraw(amulet.id, MAX_INV_SIZE - initialInventoryCount);
			withdrawTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= depositTimeout) {
			return 0;
		}

		final int itemId = getInventoryId(initialInventoryCount);
		deposit(itemId, MAX_INV_SIZE);
		depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Amulet Enchanter", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s",
				toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getAccurateXpForLevel(Skill.MAGIC.getIndex()) - initialMagicXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@%s: @whi@%s @cya@(@whi@%s casts@cya@/@whi@hr@cya@)",
				amulet, amuletsEnchanted, toUnitsPerHour(amuletsEnchanted, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Remaining: @whi@%d", materialsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(amuletsEnchanted, materialsRemaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum Amulet {
		SAPPHIRE(302, 3, new int[]{102, 616, 683}, "Sapphire"),
		EMERALD(303, 13, new int[]{101, 617, 684}, "Emerald"),
		RUBY(304, 24, new int[]{197, 615, 682}, "Ruby"),
		DIAMOND(305, 30, new int[]{103, 618, 685}, "Diamond"),
		DRAGONSTONE(610, 42, new int[]{102, 616, 683, 103, 618, 685}, "Dragonstone");

		private final int id;
		private final int spellId;
		private final int[] staffIds;
		private final String name;

		Amulet(final int id, final int spellId, final int[] staffIds, final String name) {
			this.id = id;
			this.spellId = spellId;
			this.staffIds = staffIds;
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
