import java.util.ArrayList;
import java.util.List;

/**
 * A script for Ranging npcs with a walkback radius.
 * Start script near the npc to be ranged.
 * <p>
 * Parameters:
 * -n <npcId>...
 * -r <radius> (Default 2)
 * <p>
 *
 * @Author Chomp
 */
public class AA_Ranged extends AA_Script {
	private static final int MAXIMUM_FATIGUE = 100;

	private Coordinate startCoord;

	private double initialRangedXp;

	private long startTime;
	private long currentTick;
	private long nextTick;

	private int[] npcIds;

	private int radius = 2;

	private boolean idle;
	private boolean wait;

	public AA_Ranged(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) printInstructions();

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		final String[] args = parameters.split(" ");

		final List<Integer> npcIds = new ArrayList<>();

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
				case "-n":
				case "--npc":
					do {
						npcIds.add(Integer.parseInt(args[++i]));
					} while ((i + 1 < args.length) && !args[i + 1].startsWith("-"));
					break;
				case "-r":
				case "--radius":
					radius = Integer.parseInt(args[++i]);
					break;
				default:
					throw new IllegalArgumentException("Malformed parameters. Try again ...");
			}
		}

		this.npcIds = npcIds.stream().distinct().mapToInt(i -> i).toArray();
		startCoord = new Coordinate(getPlayerX(), getPlayerY());
		initialRangedXp = getSkillExperience(Skill.RANGED.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (idle) {
			if (isAtCoordinate(startCoord)) {
				final Coordinate coordinate = getWalkableCoordinate();

				if (coordinate != null) {
					walkTo(coordinate.getX(), coordinate.getY());
					return SLEEP_ONE_TICK;
				}
			}

			idle = false;
		}

		if (getFatiguePercent() >= MAXIMUM_FATIGUE) return sleep();

		if (distanceTo(startCoord.getX(), startCoord.getY()) > radius) {
			walkTo(startCoord.getX(), startCoord.getY());
			return SLEEP_ONE_TICK;
		}

		if (wait) {
			if (nextTick > currentTick) return 0;
			wait = false;
			walkTo(getPlayerX(), getPlayerY());
			return SLEEP_ONE_TICK;
		}

		final Object npc = getNearestNpc(npcIds, false, false);
		if (npc == null) return 0;

		wait = true;
		nextTick = currentTick + 1;
		attackNpc(npc);
		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("area")) {
			idle = true;
		} else if (message.endsWith("ammo!")) {
			exit("Out of ammo.");
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Ranged", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getSkillExperience(Skill.RANGED.getIndex()) - initialRangedXp;

		bot.drawString(String.format("@gre@Ranged Xp: @whi@%s @gre@(@whi@%s xp@gre@/@whi@hr@gre@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);
	}

	@Override
	public void onPlayerCoord(final int x, final int y) {
		currentTick++;
	}
}
