/**
 * A script for Maging npcs.
 * Start script near the npc to be maged.
 * <p>
 * Required Parameter:
 * -s,--spell <WIND_STRIKE|WATER_STRIKE|...|FIRE_WAVE>
 * -i,--id <npcId>
 * <p>
 *
 * @Author Chomp
 */
public class AA_SpellCaster extends AA_Script {
	private static final long CAST_DELAY = 1200L; // +- based on latency
	private static final int MAXIMUM_FATIGUE = 100;

	private double initialMagicXp;

	private long startTime;
	private long castTimeout;

	private Spell spell;

	private int npcId;

	private int startX;
	private int startY;

	private boolean idle;

	public AA_SpellCaster(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) printInstructions();

		final String[] args = parameters.split(" ");

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
				case "-s":
				case "--spell":
					spell = Spell.valueOf(args[++i].toUpperCase());
					break;
				case "-i":
				case "--id":
					npcId = Integer.parseInt(args[++i]);
					break;
				default:
					throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
			}
		}

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		startX = getPlayerX();
		startY = getPlayerY();

		initialMagicXp = getSkillExperience(Skill.MAGIC.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (idle) {
			if (getPlayerX() == startX && getPlayerY() == startY) {
				final Coordinate coordinate = getWalkableCoordinate();

				if (coordinate != null) {
					walkTo(coordinate.getX(), coordinate.getY());
					return SLEEP_ONE_TICK;
				}
			}

			idle = false;
		}

		if (getPlayerX() != startX || getPlayerY() != startY) {
			walkTo(startX, startY);
			return SLEEP_ONE_TICK;
		}

		if (getFatiguePercent() >= MAXIMUM_FATIGUE) {
			return sleep();
		}

		if (System.currentTimeMillis() <= castTimeout) {
			return 0;
		}

		final Object npc = getNearestNpc(npcId, false, false);

		if (npc == null) {
			return 0;
		}

		bot.displayMessage("@cya@Casting ...");
		castOnNpc(spell.getId(), npc);
		castTimeout = System.currentTimeMillis() + SLEEP_TWO_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("successfully")) {
			castTimeout = System.currentTimeMillis() + CAST_DELAY;
		} else if (message.startsWith("reagents", 23)) {
			exit("Out of runes.");
		} else if (message.endsWith("spell")) {
			castTimeout = 0L;
		} else if (message.endsWith("seconds")) {
			castTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS * 2;
		} else if (message.endsWith("area")) {
			idle = true;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Spell Caster", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getSkillExperience(Skill.MAGIC.getIndex()) - initialMagicXp;

		bot.drawString(String.format("@red@Spell: @whi@%s", spell),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@cya@Magic Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	public enum Spell {
		WIND_STRIKE("Wind Strike", 0),
		WATER_STRIKE("Water Strike", 2),
		EARTH_STRIKE("Earth Strike", 4),
		FIRE_STRIKE("Fire Strike", 6),
		WIND_BOLT("Wind Bolt", 8),
		WATER_BOLT("Water Bolt", 11),
		EARTH_BOLT("Earth Bolt", 14),
		FIRE_BOLT("Fire Bolt", 17),
		CRUMBLE_UNDEAD("Crumble Undead", 19),
		WIND_BLAST("Wind Blast", 20),
		WATER_BLAST("Water Blast", 23),
		EARTH_BLAST("Earth Blast", 27),
		FIRE_BLAST("Fire Blast", 32),
		CLAWS_OF_GUTHIX("Claws of Guthix", 33),
		SARADOMIN_STRIKE("Saradomin Strike", 34),
		FLAMES_OF_ZAMORAK("Flames of Zamorak", 35),
		WIND_WAVE("Wind Wave", 37),
		WATER_WAVE("Water Wave", 39),
		EARTH_WAVE("Earth Wave", 43),
		FIRE_WAVE("Fire Wave", 45);

		private final String name;
		private final int id;

		Spell(final String name, final int id) {
			this.name = name;
			this.id = id;
		}

		@Override
		public String toString() {
			return name;
		}

		public String getName() {
			return name;
		}

		public int getId() {
			return id;
		}
	}
}
