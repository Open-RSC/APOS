import java.util.EnumSet;
import java.util.Iterator;

/**
 * Mixes potions.
 * <p>
 * Requirements:
 * Start at a bank or Shantay Pass with sleeping bag in inventory.
 * The script will try to mix all potions if no potion parameter is provided.
 * <p>
 * Optional Parameter:
 * <attack_potion|restore_prayer_potion|...>
 * <p>
 * Potions:
 * attack_potion
 * cure_poison_potion
 * strength_potion
 * stat_restoration_potion
 * restore_prayer_potion
 * super_attack_potion
 * poison_antidote
 * fishing_potion
 * super_strength_potion
 * weapon_poision
 * super_defense_potion
 * ranging_potion
 * potion_of_zamorak
 * <p>
 *
 * @Author Chomp
 */
public class AA_PotionMixer extends AA_Script {
	private static final Coordinate COORDINATE_SHANTAY_BANK_CHEST = new Coordinate(58, 731);

	private static final long MIX_DELAY = 1210L; // +- based on latency

	private static final int WITHDRAW_COUNT = 14;
	private static final int MAXIMUM_FATIGUE = 99;

	private Potion potion;
	private Iterator<Potion> iterator;
	private long startTime;

	private double initialHerblawXp;

	private long openTimeout;
	private long depositTimeout;
	private long withdrawUnfTimeout;
	private long withdrawSecondaryTimeout;
	private long mixTimeout;

	private int potionsMixed;
	private int materialsRemaining;
	private int inventoryCount;

	private boolean shantayBanking;
	private boolean idle;

	public AA_PotionMixer(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) potion = Potion.valueOf(parameters.toUpperCase());

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (potion == null) {
			iterator = EnumSet.allOf(Potion.class).iterator();
			potion = iterator.next();
		}

		if (getLevel(Skill.HERBLAW.getIndex()) < potion.level) {
			throw new IllegalStateException(String.format("Lvl %d Herblaw required to mix %s.",
				potion.level, getItemNameId(potion.id)));
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

		if (System.currentTimeMillis() <= mixTimeout) {
			return 0;
		}

		inventoryCount = getInventoryCount();
		final int secondaryIndex = getInventoryIndex(potion.secondaryId);

		if (inventoryCount == 1 ||
			getInventoryId(1) != potion.unfinishedPotionId ||
			secondaryIndex == -1) {
			return bank();
		}

		bot.displayMessage("@gre@Mixing ...");
		useItemWithItem(1, secondaryIndex);
		mixTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("potion")) {
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

		if (inventoryCount > 1 &&
			getInventoryId(1) == potion.unfinishedPotionId &&
			inventoryCount < MAX_INV_SIZE) {
			if (System.currentTimeMillis() <= withdrawSecondaryTimeout) {
				return 0;
			}

			final int secondariesRemaining = bankCount(potion.secondaryId);

			if (secondariesRemaining == 0) {
				if (iterator == null || !iterator.hasNext()) {
					return exit(String.format("Out of %s.", getItemNameId(potion.secondaryId)));
				}

				potion = iterator.next();
				potionsMixed = 0;
				initialHerblawXp = getAccurateXpForLevel(Skill.HERBLAW.getIndex());
				startTime = System.currentTimeMillis();

				if (getLevel(Skill.HERBLAW.getIndex()) < potion.level) {
					return exit(String.format("Lvl %d Herblaw required to mix %s.",
						potion.level, getItemNameId(potion.id)));
				}

				return 0;
			}

			final int unfsRemaining = bankCount(potion.unfinishedPotionId);

			materialsRemaining = Math.min(secondariesRemaining, unfsRemaining);
			withdraw(potion.secondaryId, WITHDRAW_COUNT);
			withdrawSecondaryTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= withdrawUnfTimeout) {
			return 0;
		}

		if (inventoryCount == 1) {
			if (!hasBankItem(potion.unfinishedPotionId)) {
				if (iterator == null || !iterator.hasNext()) {
					return exit(String.format("Out of %s.", getItemNameId(potion.unfinishedPotionId)));
				}

				potion = iterator.next();
				potionsMixed = 0;
				initialHerblawXp = getAccurateXpForLevel(Skill.HERBLAW.getIndex());
				startTime = System.currentTimeMillis();

				if (getLevel(Skill.HERBLAW.getIndex()) < potion.level) {
					return exit(String.format("Lvl %d Herblaw required to mix %s.",
						potion.level, getItemNameId(potion.id)));
				}

				return 0;
			}

			withdraw(potion.unfinishedPotionId, WITHDRAW_COUNT);
			withdrawUnfTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= depositTimeout) {
			return 0;
		}

		final int itemId = getInventoryId(1);
		deposit(itemId, MAX_INV_SIZE);
		depositTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Potion Mixer", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getAccurateXpForLevel(Skill.HERBLAW.getIndex()) - initialHerblawXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s pots@cya@/@whi@hr@cya@)",
				potion, potionsMixed, toUnitsPerHour(potionsMixed, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Remaining: @whi@%d", materialsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(potionsMixed, materialsRemaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum Potion {
		ATTACK_POTION(3, 474, 454, 270, "Attack"),
		CURE_POISON_POTION(5, 566, 455, 473, "Cure Poison"),
		STRENGTH_POTION(12, 222, 456, 220, "Strength"),
		STAT_RESTORATION_POTION(22, 477, 457, 219, "Stat Res."),
		RESTORE_PRAYER_POTION(38, 483, 458, 469, "Prayer"),
		SUPER_ATTACK_POTION(45, 486, 459, 270, "Super Atk"),
		POISON_ANTIDOTE(48, 569, 459, 473, "Antidote"),
		FISHING_POTION(50, 489, 460, 469, "Fishing"),
		SUPER_STRENGTH_POTION(55, 492, 461, 220, "Super Str"),
		WEAPON_POISION(60, 572, 461, 472, "Weapon Poison"),
		SUPER_DEFENSE_POTION(66, 495, 462, 471, "Super Def"),
		RANGING_POTION(72, 498, 463, 501, "Ranging"),
		POTION_OF_ZAMORAK(78, 963, 935, 936, "Zamorak");

		private final int level;
		private final int id;
		private final int unfinishedPotionId;
		private final int secondaryId;
		private final String name;

		Potion(final int level, final int id, final int unfinishedPotionId, final int secondaryId, final String name) {
			this.level = level;
			this.id = id;
			this.unfinishedPotionId = unfinishedPotionId;
			this.secondaryId = secondaryId;
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
