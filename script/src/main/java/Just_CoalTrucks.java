import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Just_CoalTrucks extends Script {


	/* rock selection improvements inspired by aero */

	public Just_CoalTrucks(Extension ex) {
		super(ex);
		pw = new PathWalker(ex);
	}

	private static final int SLEEPING_BAG = 1263;
	private static final int CHISEL = 167;
	private static final int GNOME_BALL = 981;
	private static final int SKILL_MINING = 14;

	private static final int[] pickaxes = {
		1262, 1261, 1260, 1259, 1258, 156
	};

	private static final int[] bank_ids = {
		149, 383, 152, 155, 202, 150, 151, 153, 154, 409,
		160, 159, 158, 157, 542, 889, 890, 891,
		161, 162, 163, 164, 523, 892, 893, 894,
	};

	private static final int[] gems = {
		160, 159, 158, 157, 542, 889, 890, 891,
	};

	private static final Map<String, int[]> map_rocks = new HashMap<>();

	private final int[] banked_count = new int[bank_ids.length];
	private final boolean[] has_banked = new boolean[bank_ids.length];
	private int[][] rocks;
	private int[] coal_rocks;
	private int last_x;
	private int last_y;
	private long last_bat_check_passed;
	private long sleep_time;
	private long start_time;
	private long bank_time;
	private long move_time;
	private long click_time;
	private long last_mining_action;
	private boolean init_path;
	private boolean died;
	private boolean can_mine;
	private boolean continue_mining;
	private boolean truck_ready;

	private PathWalker.Location bank;
	private final PathWalker pw;
	private PathWalker.Path from_bank_to_mine;
	private PathWalker.Path to_bank_from_mine;
	private PathWalker.Path from_bank_to_trucks;
	private PathWalker.Path to_bank_from_trucks;

	private long level_time;
	private long cur_fails;
	private long total_fails;
	private long cur_success;
	private long total_success;

	private int levels_gained;

	private final int[] last_used = new int[2];
	private final int[] last_mined = new int[2];
	private long dont_remine;

	private final DecimalFormat iformat = new DecimalFormat("#,##0");

	public static class Config {
		public final static int MINE_X = 591; //Mine arrival for pathwalker
		public final static int MINE_Y = 458;
		public final static int BANK_X = 501; //Bank arrival point for pathwalker
		public final static int BANK_Y = 452;
		public final static int TRUCK_X = 519; //Truck arrival point for pathwalker
		public final static int TRUCK_Y = 444;
		public final static int X_MIN_BANK = 498; //Boundaries for bank area
		public final static int Y_MIN_BANK = 447;
		public final static int X_MAX_BANK = 504;
		public final static int Y_MAX_BANK = 453;
		public final static int X_MIN_TRUCK = 518; //Boundaries for truck area
		public final static int Y_MIN_TRUCK = 440;
		public final static int X_MAX_TRUCK = 525;
		public final static int Y_MAX_TRUCK = 445;
		public final static int FIGHT_MODE = 1;
		public final static int COAL_TRUCK = 383;
		public final static int COAL_ITEM = 155;
		public final static int MOVE_TIMEOUT = 10000;
	}

	@Override
	public void init(String params) {
		died = false;
		can_mine = true;
		continue_mining = true;
		truck_ready = true;
		sleep_time = -1L;
		bank_time = -1L;
		move_time = -1L;
		start_time = -1L;
		click_time = -1L;
		last_mining_action = -1L;
		last_x = -1;
		last_y = -1;
		last_bat_check_passed = System.currentTimeMillis();
		coal_rocks = new int[]{110, 111};
		pw.init(null);
		from_bank_to_mine = pw.calcPath(Config.BANK_X, Config.BANK_Y, Config.MINE_X, Config.MINE_Y);
		to_bank_from_mine = pw.calcPath(Config.MINE_X, Config.MINE_Y, Config.BANK_X, Config.BANK_Y);
		from_bank_to_trucks = pw.calcPath(Config.BANK_X, Config.BANK_Y, Config.TRUCK_X, Config.TRUCK_Y);
		to_bank_from_trucks = pw.calcPath(Config.TRUCK_X, Config.TRUCK_Y, Config.BANK_X, Config.BANK_Y);
	}

	@Override
	public int main() {
		int ret = _premain();
		if (ret != -1) {
			return ret;
		}
		if (isQuestMenu()) {
			answer(0);
			bank_time = System.currentTimeMillis();
			return random(2000, 3000);
		}
		if (isBanking()) {
			bank_time = -1L;
			int len = bank_ids.length;
			for (int i = 0; i < len; ++i) {
				int count = getInventoryCount(bank_ids[i]);
				if (count > 0) {
					deposit(bank_ids[i], count);
					if (!has_banked[i]) {
						banked_count[i] += count;
						has_banked[i] = true;
					}
					return random(1000, 1500);
				}
			}
			if (getInventoryIndex(pickaxes) == -1) {
				len = pickaxes.length;
				for (int i = 0; i < len; ++i) {
					if (bankCount(pickaxes[i]) <= 0) {
						continue;
					}
					System.out.println("Withdrawing pickaxe");
					withdraw(pickaxes[i], 1);
					return random(1700, 3200);
				}
				return _end("Error: no pickaxe!");
			}
			if (!hasInventoryItem(SLEEPING_BAG)) {
				if (bankCount(SLEEPING_BAG) <= 0) {
					return _end("Error: no sleeping bag!");
				}
				System.out.println("Withdrawing sleeping bag");
				withdraw(SLEEPING_BAG, 1);
				return random(1700, 3200);
			}
			Arrays.fill(has_banked, false);
			closeBank();
			if (truck_ready) {
				pw.setPath(from_bank_to_mine);
				can_mine = true;
			} else {
				pw.setPath(from_bank_to_trucks);
			}
			return random(1000, 2000);
		} else if (bank_time != -1L) {
			if (System.currentTimeMillis() >= (bank_time + 8000L)) {
				bank_time = -1L;
			}
			return random(300, 400);
		}
		if (pw.walkPath()) return 0;
		if (can_mine) {
			// System.out.println("Can mine");
			if (getInventoryCount() == 30) {
				if (!truck_ready) {
					can_mine = false;
					return random(600, 800);
				}
				int coal_index = getInventoryIndex(Config.COAL_ITEM);
				int[] coal_truck = getObjectById(Config.COAL_TRUCK);
				if (coal_index != -1 && coal_truck[0] != -1) {
					useItemOnObject(Config.COAL_ITEM, Config.COAL_TRUCK); //Must listen for "full" truck message
					return random(2500, 3000);
				}
				//Load coal onto truck here
				//Note: Must "use" coal onto truck. One use loads all coal from inventory at once.
				//Must set can_mine = false once inventory and coal truck is full
			}
			if (getX() >= 598) {
				// System.out.println("Mining");
				return mine_rocks();
			}
			if (getX() >= 588) {
				// System.out.println("Near log");
				if (isAtApproxCoords(592, 458, 1)) {
					atObject(593, 458);
					return random(5000, 6000);
				} else {
					walkTo(592, 458);
					return random(500, 600);
				}

			}
			if (isWithinBounds(getX(), getY(), Config.X_MIN_BANK, Config.X_MAX_BANK, Config.Y_MIN_BANK, Config.Y_MAX_BANK)) { //Should only happen if starting script with coal in inventory
				// System.out.println("Inside bank");
				int coal_index = getInventoryIndex(Config.COAL_ITEM);
				if (coal_index != -1) {
					can_mine = false;
					truck_ready = false;
					ret = talk_to_banker();
					if (ret != -1) {
						return ret;
					}
				} else {
					pw.setPath(from_bank_to_mine);
					System.out.println("Starting script in bank!");

				}
			}
		} else {
			//Need to make a loop to withdraw coal from truck until it's empty
			//Bank entrance: 501 454, bank exit from inside: 501 453, bank doors closed: 64 (500, 454)
			//Position to access truck: 520 444. Need to repeat "atobject" on truck one at a time until inventory full, then return to bank
			if (isWithinBounds(getX(), getY(), Config.X_MIN_BANK, Config.X_MAX_BANK, Config.Y_MIN_BANK, Config.Y_MAX_BANK)) {
				ret = talk_to_banker();
				if (ret != -1) {
					return ret;
				}
			}
			if (isWithinBounds(getX(), getY(), Config.X_MIN_TRUCK, Config.X_MAX_TRUCK, Config.Y_MIN_TRUCK, Config.Y_MAX_TRUCK)) {
				if (getInventoryCount() < 30 && !truck_ready) {
					ret = take_from_truck();
					if (ret != -1) {
						return ret;
					}
				} else {
					pw.setPath(to_bank_from_trucks);
				}
			}
			if (getX() >= 598) {
				if (isAtApproxCoords(598, 458, 1)) {
					atObject(597, 458);
					return random(5000, 6000);
				} else {
					walkTo(598, 458);
					return random(500, 600);
				}

			}
			if (getX() >= 588) {
				pw.setPath(to_bank_from_mine);
			}
		}
		return random(800, 1000);
	}


	private int talk_to_banker() {
		int[] banker = getNpcByIdNotTalk(BANKERS);
		if (banker[0] != -1) {
			if (distanceTo(banker[1], banker[2]) > 5) {
				walk_approx(banker[1], banker[2]);
				return random(1500, 2500);
			}
			talkToNpc(banker[0]);
			return random(3000, 3500);
		}
		return -1;
	}

	private int take_from_truck() {
		int[] truck = getObjectById(Config.COAL_TRUCK);
		if (truck[0] != -1) {
			if (distanceTo(truck[1], truck[2]) > 5) {
				walk_approx(truck[1], truck[2]);
				return random(600, 800);
			}
			atObject(truck[1], truck[2]);
			return random(800, 1000);
		}
		return -1;
	}

	private int _premain() {
		if (getFightMode() != Config.FIGHT_MODE) {
			setFightMode(Config.FIGHT_MODE);
			return random(600, 800);
		}

		if (start_time == -1L) {
			start_time = System.currentTimeMillis();
			level_time = start_time;
		}

		if (inCombat()) {
			pw.resetWait();
			walkTo(getX(), getY());
			return random(400, 600);
		}

		if (click_time != -1L) {
			if (System.currentTimeMillis() >= click_time) {
				click_time = -1L;
			}
			return 0;
		}

		if (sleep_time != -1L) {
			if (System.currentTimeMillis() >= sleep_time) {
				int bag = getInventoryIndex(SLEEPING_BAG);
				if (bag != -1) {
					useItem(bag);
				}
				sleep_time = -1L;
				return random(1500, 2500);
			}
			return 0;
		}

		if (move_time != -1L) {
			if (System.currentTimeMillis() >= move_time) {
				System.out.println("Forcing movement...");
				walk_approx(getX(), getY());
				move_time = -1L;
				return random(1500, 2500);
			}
			return 0;
		}

		int cur_x = getX();
		int cur_y = getY();
		if (is_giantbat_beside(cur_x, cur_y) && cur_x == last_x && cur_y == last_y) {
			if ((System.currentTimeMillis() - last_bat_check_passed) > Config.MOVE_TIMEOUT && System.currentTimeMillis() - last_mining_action > Config.MOVE_TIMEOUT) {
				move_time = System.currentTimeMillis() + random(1500, 1800);
			}
		} else {
			last_x = cur_x;
			last_y = cur_y;
			last_bat_check_passed = System.currentTimeMillis();
		}

		int chisel = getInventoryIndex(CHISEL);
		if (chisel != -1) {
			int gem = getInventoryIndex(gems);
			if (gem != -1) {
				useItemWithItem(chisel, gem);
				return random(700, 900);
			}
		}

		int ball = getInventoryIndex(GNOME_BALL);
		if (ball != -1) {
			System.out.println("Gnome ball!");
			dropItem(ball);
			return random(1200, 2000);
		}
		// System.out.println("Premain");
		return -1;
	}

	private boolean is_giantbat_beside(int x, int y) {
		int[] bat = getNpcInRadius(43, x, y, 1);
		return bat[0] != -1;
	}

	private boolean is_player_beside(int x, int y) {
		int count = countPlayers();
		for (int i = 1; i < count; ++i) {
			int dist = Math.abs(getPlayerX(i) - x) +
				Math.abs(getPlayerY(i) - y);
			if (dist <= 1) {
				return true;
			}
		}
		return false;
	}

	private int[] get_closest_rock(int... ids) {
		int[] rock = new int[]{-1, -1, -1};
		int best_dist = Integer.MAX_VALUE;
		int count = getObjectCount();
		int my_x = getX();
		int my_y = getY();
		for (int i = 0; i < count; ++i) {
			int rock_id = getObjectId(i);
			if (!inArray(ids, rock_id)) {
				continue;
			}
			int rock_x = getObjectX(i);
			int rock_y = getObjectY(i);
			if (last_mined[0] == rock_x &&
				last_mined[1] == rock_y &&
				System.currentTimeMillis() < dont_remine) {
				continue;
			}
			if (is_player_beside(rock_x, rock_y)) {
				continue;
			}
			int dist = Math.abs(rock_x - my_x) +
				Math.abs(rock_y - my_y);
			if (dist < 30 && dist < best_dist) {
				best_dist = dist;
				rock[0] = rock_id;
				rock[1] = rock_x;
				rock[2] = rock_y;
			}
		}
		/* fall back to rocks with players near them */
		if (rock[0] == -1) {
			for (int i = 0; i < count; ++i) {
				int rock_id = getObjectId(i);
				if (!inArray(ids, rock_id)) {
					continue;
				}
				int rock_x = getObjectX(i);
				int rock_y = getObjectY(i);
				if (last_mined[0] == rock_x &&
					last_mined[1] == rock_y &&
					System.currentTimeMillis() < dont_remine) {
					continue;
				}
				int dist = Math.abs(rock_x - my_x) +
					Math.abs(rock_y - my_y);
				if (dist < 30 && dist < best_dist) {
					best_dist = dist;
					rock[0] = rock_id;
					rock[1] = rock_x;
					rock[2] = rock_y;
				}
			}
		}
		return rock;
	}

	private int mine_rocks() {
		int array_sz = coal_rocks.length;
		for (int i = 0; i < array_sz; ++i) {
			int[] rock = get_closest_rock(coal_rocks[i]);
			if (rock[0] == -1) {
				continue;
			}
			int dist = distanceTo(rock[1], rock[2]);
			if (dist > 5) {
				walk_approx(rock[1], rock[2]);
				return random(1500, 2500);
			}
			atObject(rock[1], rock[2]);
			last_used[0] = rock[1];
			last_used[1] = rock[2];
			return random(750, 950);
		}
		return random(100, 700);
	}

	public boolean should_bank() {
		if (getInventoryCount() == MAX_INV_SIZE) {
			return true;
		}
		if (getInventoryIndex(pickaxes) == -1) {
			return true;
		}
		return getInventoryIndex(SLEEPING_BAG) == -1;
	}

	private void walk_approx(int x, int y) {
		int dx, dy;
		int loop = 0;
		do {
			dx = x + random(-1, 1);
			dy = y + random(-1, 1);
			if ((++loop) > 100) return;
		} while (!isReachable(dx, dy) ||
			(dx == getX() && dy == getY()));
		walkTo(dx, dy);
	}

	@Override
	public void paint() {
		if (start_time == -1L) {
			return;
		}
		final int orangey = 0xFFD900;
		final int white = 0xFFFFFF;
		int x = (getGameWidth() / 2) - 125;
		int y = 50;
		drawString("Just_CoalTrucks", x, y, 1, orangey);
		y += 15;
		drawString("Runtime: " + get_time_since(start_time),
			x + 10, y, 1, white);
		y += 15;
		drawString(String.format("Stats for current level (%d gained):",
			levels_gained), x, y, 1, orangey);
		y += 15;
		drawString(String.format("Successful attempts: %s (%s/h)",
				iformat.format(cur_success),
				per_hour(cur_success, level_time)),
			x + 10, y, 1, white);
		y += 15;
		drawString(String.format("Failed attempts: %s (%s/h)",
				iformat.format(cur_fails),
				per_hour(cur_fails, level_time)),
			x + 10, y, 1, white);
		y += 15;
		drawString("Fail rate: " + (float)
				((double) cur_fails / (double) cur_success),
			x + 10, y, 1, white);
		y += 15;
		if (levels_gained > 0) {
			drawString("Total:", x, y, 1, orangey);
			y += 15;
			drawString(String.format("Successful attempts: %s",
					iformat.format(total_success)),
				x + 10, y, 1, white);
			y += 15;
			drawString(String.format("Failed attempts: %s",
					iformat.format(total_fails)),
				x + 10, y, 1, white);
			y += 15;
		}
		// if (!cb_bank.getState()) return;
		boolean header = false;
		int len = bank_ids.length;
		for (int i = 0; i < len; ++i) {
			if (banked_count[i] <= 0) {
				continue;
			}
			if (!header) {
				drawString("Banked items:", x, y, 1, orangey);
				y += 15;
				header = true;
			}
			drawString(String.format("%s %s",
					iformat.format(banked_count[i]),
					getItemNameId(bank_ids[i])),
				x + 10, y, 1, white);
			y += 15;
		}
	}

	private String per_hour(long count, long time) {
		double amount, secs;

		if (count == 0) return "0";
		amount = count * 60.0 * 60.0;
		secs = (System.currentTimeMillis() - time) / 1000.0;
		return iformat.format(amount / secs);
	}

	private static String get_time_since(long t) {
		long millis = (System.currentTimeMillis() - t) / 1000;
		long second = millis % 60;
		long minute = (millis / 60) % 60;
		long hour = (millis / (60 * 60)) % 24;
		long day = (millis / (60 * 60 * 24));

		if (day > 0L) {
			return String.format("%02d days, %02d hrs, %02d mins",
				day, hour, minute);
		}
		if (hour > 0L) {
			return String.format("%02d hours, %02d mins, %02d secs",
				hour, minute, second);
		}
		if (minute > 0L) {
			return String.format("%02d minutes, %02d seconds",
				minute, second);
		}
		return String.format("%02d seconds", second);
	}

	@Override
	public void onServerMessage(String str) {
		str = str.toLowerCase(Locale.ENGLISH);
		if (str.contains("standing here")) {
			move_time = System.currentTimeMillis() +
				random(1500, 1800);
		} else if (str.contains("swing")) {
			click_time = System.currentTimeMillis() +
				random(5000, 7000);
			last_mining_action = System.currentTimeMillis();
		} else if (str.contains("scratch") ||
			str.contains("fail") || str.contains("no ore")) {
			click_time = System.currentTimeMillis() +
				random(100, 200);
			++cur_fails;
			++total_fails;
		} else if (str.contains("advanced")) {
			System.out.println("You just advanced a level.");
			level_time = System.currentTimeMillis();
			print_out();
			cur_fails = 0;
			cur_success = 0;
			++levels_gained;
		} else if (str.contains("manage") || str.contains("gem") ||
			str.contains("just mined")) {
			click_time = System.currentTimeMillis() +
				random(100, 200);
			++cur_success;
			++total_success;
			last_mined[0] = last_used[0];
			last_mined[1] = last_used[1];
			dont_remine = System.currentTimeMillis() + 5000L;
		} else if (str.contains("found")) {
			click_time = System.currentTimeMillis() +
				random(100, 200);
			++cur_success;
			++total_success;
			last_mined[0] = last_used[0];
			last_mined[1] = last_used[1];
		} else if (str.contains("tired")) {
			sleep_time = System.currentTimeMillis() +
				random(800, 2500);
		} else if (str.contains("full")) {
			truck_ready = false;
		} else if (str.contains("no coal")) {
			truck_ready = true;
		}
	}

	private void print_out() {
		System.out.print("Runtime: ");
		System.out.println(get_time_since(start_time));
		System.out.print("Old success count: ");
		System.out.println(cur_success);
		System.out.print("Old fail count: ");
		System.out.println(cur_fails);
		System.out.print("Old fail rate: ");
		System.out.println((double) cur_fails / (double) cur_success);
		System.out.print("Fail total: ");
		System.out.println(total_fails);
		System.out.print("Success total: ");
		System.out.println(total_success);
		if (true) {
			// if (cb_bank.getState()) {
			for (int i = 0; i < bank_ids.length; ++i) {
				if (banked_count[i] <= 0) continue;
				System.out.println("Banked " + getItemNameId(bank_ids[i]) + ": " + banked_count[i]);
			}
		}
	}

	private int _end(String reason) {
		print_out();
		System.out.println(reason);
		stopScript();
		setAutoLogin(false);
		return 0;
	}

	private boolean isWithinBounds(int x_test, int y_test, int x_min, int x_max, int y_min, int y_max) {
		if (x_test >= x_min &&
			x_test <= x_max &&
			y_test >= y_min &&
			y_test <= y_max) {
			return true;
		}
		return false;
	}
}
