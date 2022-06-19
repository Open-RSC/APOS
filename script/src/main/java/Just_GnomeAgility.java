/**
 * Trains agility at the gnome agility arena. start anywhere
 * along the agility route to begin.
 * <p>
 * v1.1
 * <p>
 * - yomama`
 */

import java.awt.*;
import java.util.Locale;

public class Just_GnomeAgility extends Script {

	int fmode = 1;

	private static long start_time;
	private static long start_time_exp;
	private static long action_time;
	private static int obstacle_count;
	private double startagilityexp = 0.0D;

	private static final Point[] log_hop = {
		new Point(681, 492),
		new Point(696, 495),
		new Point(692, 495)
	};

	private static final Point[] net_climb = {
		new Point(690, 499),
		new Point(695, 502),
		new Point(692, 502)
	};

	private static final Point[] tower_climbup = {
		new Point(690, 1448),
		new Point(695, 1451),
		new Point(693, 1450)
	};

	private static final Point[] rope_swing = {
		new Point(690, 2394),
		new Point(693, 2396),
		new Point(690, 2395)
	};

	private static final Point[] tower_climbdown = {
		new Point(682, 2395),
		new Point(685, 2397),
		new Point(684, 2396)
	};

	private static final Point[] net_climb2 = {
		new Point(681, 503),
		new Point(686, 507),
		new Point(683, 503),
		new Point(683, 502)
	};

	private static final Point[] pipe_enter = {
		new Point(681, 497),
		new Point(686, 501),
		new Point(683, 498)
	};

	private static final Point[] action_point = {
		new Point(692, 499),
		new Point(692, 1448),
		new Point(693, 2394),
		new Point(685, 2396),
		new Point(683, 506),
		new Point(683, 501),
		new Point(683, 494)
	};

	public Just_GnomeAgility(Extension e) {
		super(e);
	}

	public void init(String params) {
		if (!params.equals(""))
			fmode = Integer.parseInt(params);

		action_time = -1L;
		obstacle_count = 0;
		startagilityexp += getAccurateXpForLevel(16);
		start_time = start_time_exp = System.currentTimeMillis();
	}

	public int main() {
		if (getFightMode() != fmode) {
			setFightMode(fmode);
			return random(640, 800);
		}

		if (action_time != -1L) {
			if (System.currentTimeMillis() >= (action_time + 8000L)) {
				action_time = -1L;
			}
			for (int i = 0; i < action_point.length; i++) {
				if (getX() == action_point[i].x && getY() == action_point[i].y) {
					action_time = -1L;
					obstacle_count++;
					// System.out.println("At action point " + i + ": (" + action_point[i].x + ", " + action_point[i].y + ").");
					break;
				}
			}
			if (action_time != -1L) return random(200, 300);
		}

		if (getFatigue() > 90) {
			useSleepingBag();
			return 3000;
		}

		if (isWithinBounds(getX(), getY(), log_hop[0].x, log_hop[0].y, log_hop[1].x, log_hop[1].y)) {
			int object_id = Config.LOG_BALANCE_ID;
			int[] object = getObjectById(object_id);
			if (object[0] != -1) {
				if (distanceTo(log_hop[2].x, log_hop[2].y) > 1) {
					walkTo(log_hop[2].x, log_hop[2].y);
					return random(640, 800);
				}
				atObject(object[1], object[2]);
				action_time = System.currentTimeMillis();
				return random(800, 1000);
			}
			// System.out.println("Can't find object: " + getObjectName(object_id));
			return random(320, 400);
		}

		if (isWithinBounds(getX(), getY(), net_climb[0].x, net_climb[0].y, net_climb[1].x, net_climb[1].y)) {
			int object_id = Config.NET_CLIMB_ID;
			int[] object = getObjectById(object_id);
			if (object[0] != -1) {
				if (distanceTo(net_climb[2].x, net_climb[2].y) > 1) {
					walkTo(net_climb[2].x, net_climb[2].y);
					return random(640, 800);
				}
				atObject(object[1], object[2]);
				action_time = System.currentTimeMillis();
				return random(800, 1000);
			}
			// System.out.println("Can't find object: " + getObjectName(object_id));
			return random(320, 400);
		}

		if (isWithinBounds(getX(), getY(), tower_climbup[0].x, tower_climbup[0].y, tower_climbup[1].x, tower_climbup[1].y)) {
			int object_id = Config.TOWER_CLIMBUP_ID;
			int[] object = getObjectById(object_id);
			if (object[0] != -1) {
				if (distanceTo(tower_climbup[2].x, tower_climbup[2].y) > 1) {
					walkTo(tower_climbup[2].x, tower_climbup[2].y);
					return random(640, 800);
				}
				atObject(object[1], object[2]);
				action_time = System.currentTimeMillis();
				return random(800, 1000);
			}
			// System.out.println("Can't find object: " + getObjectName(object_id));
			return random(320, 400);
		}

		if (isWithinBounds(getX(), getY(), rope_swing[0].x, rope_swing[0].y, rope_swing[1].x, rope_swing[1].y)) {
			int object_id = Config.ROPESWING_ID;
			int[] object = getObjectById(object_id);
			if (object[0] != -1) {
				if (distanceTo(rope_swing[2].x, rope_swing[2].y) > 1) {
					walkTo(rope_swing[2].x, rope_swing[2].y);
					return random(640, 800);
				}
				atObject(object[1], object[2]);
				action_time = System.currentTimeMillis();
				return random(800, 1000);
			}
			// System.out.println("Can't find object: " + getObjectName(object_id));
			return random(320, 400);
		}

		if (isWithinBounds(getX(), getY(), tower_climbdown[0].x, tower_climbdown[0].y, tower_climbdown[1].x, tower_climbdown[1].y)) {
			int object_id = Config.TOWER_CLIMBDOWN_ID;
			int[] object = getObjectById(object_id);
			if (object[0] != -1) {
				if (distanceTo(tower_climbdown[2].x, tower_climbdown[2].y) > 1) {
					walkTo(tower_climbdown[2].x, tower_climbdown[2].y);
					return random(640, 800);
				}
				atObject(object[1], object[2]);
				action_time = System.currentTimeMillis();
				return random(800, 1000);
			}
			// System.out.println("Can't find object: " + getObjectName(object_id));
			return random(320, 400);
		}

		if (isWithinBounds(getX(), getY(), net_climb2[0].x, net_climb2[0].y, net_climb2[1].x, net_climb2[1].y)) {
			//int object_id = Config.NET_CLIMB2_ID;
			int object_id = getObjectIdFromCoords(net_climb2[3].x, net_climb2[3].y);        //Special case because getObjectById doesn't grab closest one
			if (object_id == Config.NET_CLIMB2_ID) {
				if (distanceTo(net_climb2[2].x, net_climb2[2].y) > 1) {
					walkTo(net_climb2[2].x, net_climb2[2].y);
					return random(640, 800);
				}
				atObject(net_climb2[3].x, net_climb2[3].y);
				action_time = System.currentTimeMillis();
				return random(800, 1000);
			}
			// System.out.println("Can't find object: " + getObjectName(object_id));
			return random(320, 400);
		}

		if (isWithinBounds(getX(), getY(), pipe_enter[0].x, pipe_enter[0].y, pipe_enter[1].x, pipe_enter[1].y)) {
			int object_id = Config.PIPE_ENTER_ID;
			int[] object = getObjectById(object_id);
			if (object[0] != -1) {
				if (distanceTo(pipe_enter[2].x, pipe_enter[2].y) > 1) {
					walkTo(pipe_enter[2].x, pipe_enter[2].y);
					return random(640, 800);
				}
				atObject(object[1], object[2]);
				action_time = System.currentTimeMillis();
				return random(800, 1000);
			}
			// System.out.println("Can't find object: " + getObjectName(object_id));
			return random(320, 400);
		}

		System.out.println("Unhandled case at (" + getX() + ", " + getY() + ").");
		return 3000;
	}

	private boolean isWithinBounds(int x_test, int y_test, int x_min, int y_min, int x_max, int y_max) {
		if (x_test >= x_min &&
			x_test <= x_max &&
			y_test >= y_min &&
			y_test <= y_max) {
			return true;
		}
		return false;
	}

	private long getTime() {
		long secondsSinceStarted = ((System.currentTimeMillis() - start_time) / 1000);
		if (secondsSinceStarted <= 0) {
			return 1L;
		}
		return secondsSinceStarted;
	}

	private String getRunTime() {
		long millis = getTime();
		long second = millis % 60;
		long minute = (millis / 60) % 60;
		long hour = (millis / (60 * 60)) % 24;
		long day = (millis / (60 * 60 * 24));

		if (day > 0L) return String.format("%02d days, %02d hrs, %02d mins", day, hour, minute);
		if (hour > 0L) return String.format("%02d hours, %02d mins, %02d secs", hour, minute, second);
		if (minute > 0L) return String.format("%02d minutes, %02d seconds", minute, second);
		return String.format("%02d seconds", second);
	}

	@Override
	public void onServerMessage(String str) {
		str = str.toLowerCase(Locale.ENGLISH);
		if (str.contains("advanced 1 agility")) {
			System.out.println(str + " (Level: " + getCurrentLevel(16) + ")");
		} else if (str.contains("advanced")) {
			System.out.println(str);
		}
	}

	@Override
	public void paint() {
		final int white = 0xFFFFFF;
		final int green = 0x00FF00;
		final int yellow = 0xFFFF00;
		final int red = 0xFF0000;

		int x = 12;
		int y = 50;

		long var1 = (System.currentTimeMillis() - start_time_exp) / 1000L;
		if (var1 < 1L) {
			var1 = 1L;
		}

		drawString("Just_GnomeAgility", x, y, 4, green);
		y += 30;
		drawString("Runtime: " + getRunTime(), x, y, 1, yellow);
		y += 15;
		drawString("Cleared " + obstacle_count + " obstacles.", x, y, 1, yellow);
		y += 15;
		drawString("Agility XP rate: " + (int) ((getAccurateXpForLevel(16) - startagilityexp) * 60.0D * 60.0D / (double) var1) + "/h", x, y, 1, yellow);
	}

	public static class Config {
		public static final int LOG_BALANCE_ID = 655;
		public static final int NET_CLIMB_ID = 647;
		public static final int NET_CLIMB2_ID = 653;
		public static final int TOWER_CLIMBUP_ID = 648;
		public static final int ROPESWING_ID = 650;
		public static final int TOWER_CLIMBDOWN_ID = 649;
		public static final int PIPE_ENTER_ID = 654;
	}
}
