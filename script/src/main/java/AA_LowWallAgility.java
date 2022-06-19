/**
 * Climbs the Low Wall repeatedly at the Barbarian Agility Course.
 * Start near the low wall with sleeping bag in inventory.
 * <p>
 *
 * @Author Chomp
 */
public class AA_LowWallAgility extends AA_Script {
	private static final int wallX = 495;

	private double initialAgilityXp;

	private long startTime;

	private long timeout;

	private int wallY;
	private int prevX;

	public AA_LowWallAgility(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		wallY = random(556, 559);
		initialAgilityXp = bot.getExperience(Skill.AGILITY.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (getFatiguePercent() == 100) {
			timeout = 0L;
			return sleep();
		}

		final int playerX = getX();

		if (prevX != playerX) {
			timeout = 0L;
		} else if (System.currentTimeMillis() <= timeout) {
			return 0;
		}

		bot.displayMessage("@cya@Jumping ...");
		prevX = playerX;
		atWallObject(wallX, wallY);
		timeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("wall")) {
			timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Low Wall Agility", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = bot.getExperience(Skill.AGILITY.getIndex()) - initialAgilityXp;

		bot.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}
}
