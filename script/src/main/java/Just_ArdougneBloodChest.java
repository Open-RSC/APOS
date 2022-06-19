/*
	Just_ArdougneBloodChest by Just Monika#9999

	Requirements (all):
	1. 59 Thieving minimum to picklock Ardougne Chaos Druid Tower and thieve from chest
	2. Armor sufficient to handle Ogres (level 58)*
	3. Weapon sufficient to handle Ogres (level 58)*
	4. Food in bank: Lobsters, Sharks, or Swordfish
	5. At least 2 available spaces in inventory for Blood Runes and Coins
	6. Sleeping bag.

	* Essential for combat level 116 and lower since Ogres will be aggressive. Still recommended for 100% safety purposes since bot will attack ogre to access idling spot while waiting for chest respawn

	How script works:
	Picklocks door to open the chaos druid tower in Ardougne, thieves from one of two chests downstairs, returns to tower after being teleported out.
	Inside the tower, bot will attack any chaos druid that blocks it from reaching the down-ladder.

	Once underground:
	- (If combat level 117+): Bot will move towards first available chest, or idle on tile between the two chests if both have already been looted. Will attempt to move around any Ogre blocking its path. If the bot is trying
	to move to the spot between two chests and an Ogre is standing at that spot, the bot will attack the Ogre to reach the spot.
	- (If combat level 116-): Bot will attack any Ogre within tile and standing in its path to the nearest chest/idling spot. Proactively attacking instead of bumping into Ogres and getting aggroed should help with reaching chests faster.
	Once in combat, will attempt to run directly to the destination (instead of running on same position to escape combat).

	Configuration:
	Press F2 to check or change settings on-the-go.
	Fight mode: Auto selects fight mode (prioritizes Strength > Attack > Defense based on whether it's maxed, moves on to the next if yes). The rest are self-explanatory.
	Heal at HP: The Hits value on which the bot will consume a food item.
	Food type: Select from Shark, Lobster, or Swordfish. Script will autoselect from the list if it detects the food already in inventory. (Don't keep more than one food type in your inventory).
	Chest waiting: Choose where the bot will idle if both chests have been looted - Either at the tile between the two chests (default), or at the ladder (useful for conserving food). World hopping option will be here when/if ORSC supports it.
	Minimum food amount: Bot will bank and restock on food if food in inventory drops below this amount. Bot won't bank for food or withdraw food if set to zero.
	Chests per bank: Bot will bank once the defined number of chests have been looted. Leave at zero for continuous thieving without banking, unless food amount drops below value defined above.

	Commands:
	Press F3 to open command input window.
	"stop" or "stopnext": Script will end on next bank completion
	"banknow": Script will travel to bank after next chest looted
	"pause": Script will end after being teleported out. Start the script again by using the Start Script button.

	Inventory management:
	Script will fill available inventory space with user-defined food if "minimum food amount" config value is set to any number above zero, but reserve 2 spaces for Blood Runes and Coins. If user defines a nonzero "chests per bank value", script will
	go to bank every number defined looted chests and deposit all gathered loot, as well as refill inventory with food.

	Improved fast banking
	Improved obstacle handling

*/

import com.aposbot.Constants;
import com.aposbot.StandardCloseHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

public class Just_ArdougneBloodChest extends Script implements ActionListener {
	//Internal use
	private static boolean auto_fightmode;
	private static boolean restocking;
	private static boolean pausing;
	private static boolean foundShieldHalf;
	private static boolean shieldHalfNotifyTest;
	private static boolean isVet;
	private static boolean stop_flag;
	private static int fight_mode;
	private static int times_banked;
	private static int times_hopped;
	private static int chests_looted;
	private static int bank_every;
	private static int bank_counter;
	private static int[] drops_count;
	private static int food_id;
	private static int idle_spot;
	private static int maintain_food_amount;
	private static int maintain_hp_amount;
	private static int extra_items_count;
	private static int paint_mode;
	private static int home_world;
	private static int fail_hop;    //Fix for world-skipping when logout fails
	private static int x_offset;
	private static int last_inv_count;
	private static int current_action;
	private static int monitored_id;
	private static int expected_invcount;
	private static boolean[] drops_banked;
	private static boolean extra_items_banked;
	private static boolean[] req_items_checked;
	private static boolean debugging;
	private static boolean stop_next_bank;
	private static boolean full_paint;
	private static boolean foundExtraItems;
	// private static boolean made_space;
	private static long bank_time;
	private static long menu_time;
	private static long item_time;
	private static long bankaction_time;
	private static long click_time;
	private static long hop_time;
	private static long hop_delay;
	private static int[] skill_levels;
	private static int total_profits;    //Profits for entire script run time
	private static int trip_profits;    //Profits for current trip
	private static boolean hop_enabled;
	private static boolean no_friendly_hop;
	private static boolean update_fmode;
	private static boolean logout_flag;
	private final HashMap<Integer, Integer> item_prices = new HashMap<Integer, Integer>(64);

	//Menu
	private static Frame frame;        //Used for fightmode, food type, food amount
	private static Frame ch_frame;    //Used for fightmode selection, food
	private Choice ch_fightmode;
	private Choice ch_foodtype;
	private Choice ch_idlespot;
	private Choice ch_roommode;
	private TextField tf_min_food_amount;
	private TextField tf_bank_every;
	private TextField tf_heal_at_hp;

	//Item use rate limiter
	private static int last_item_id;
	private static int last_item_amount;

	//Pathwalker
	private final PathWalker pw;
	private PathWalker.Path from_bank;
	private PathWalker.Path to_bank;
	private PathWalker.Path to_tower;

	private final String[] fight_modes = {
		"Controlled",
		"Strength",
		"Attack",
		"Defense",
		"Auto Select"
	};

	private final String[] foodOptions = {
		"Shark",
		"Lobster",
		"Swordfish"
	};

	private final int[] foodList = {
		SHARK,
		LOBSTER,
		SWORDFISH
	};

	private final String[] idleSpotChoices = {
		"At chests",
		"At ladder" //"Use world hop" when/if ORSC implements this
	};

	//Points
	private static final Point last_pos = new Point(-1, -1);
	private static final Point idling_at = new Point(-1, -1);
	private static final Point move_around = new Point(-1, -1);
	private static final Point bank_pos = new Point(581, 573);
	private static final Point tower_pos = new Point(618, 557);
	private static final Point house_pos = new Point(612, 574);
	private static final Point chest1_pos = new Point(614, 3399);
	private static final Point chest2_pos = new Point(614, 3401);

	//Boundaries
	private static final Point bank_min = new Point(576, 571);
	private static final Point bank_max = new Point(586, 577);
	private static final Point area_min = new Point(615, 550);
	private static final Point area_max = new Point(620, 555);
	private static final Point house_min = new Point(611, 568);
	private static final Point house_max = new Point(617, 572);
	private static final Point tower_min = new Point(615, 550);
	private static final Point tower_max = new Point(620, 555);

	private static int startxp;
	private static long start_time;
	private static long start_time_exp;
	private static long move_time;
	private static long last_stuck_check_passed;

	private double startprayerxp = 0.0D;

	private final DecimalFormat int_format = new DecimalFormat("#,##0");
	private static final int
		SLEEPING_BAG = 1263,
		GNOME_BALL = 981,
		SHARK = 546,
		LOBSTER = 373,
		SWORDFISH = 370,
		BLOOD_RUNE = 619,
		COINS = 10;        //Semicolon for last item

	private static final int[] items = {
		BLOOD_RUNE,
		COINS
	};

	private static final int[] req_items = {    //Items absolutely required for script to function. Will be kept in inventory.
		SLEEPING_BAG
	};

	private static final int[] no_deposit = {    //Items to be kept in inventory and never banked
	};

	private static final int[] drop_items = {    //Items to be dropped if in inventory
		GNOME_BALL
	};

	public Just_ArdougneBloodChest(Extension e) {
		super(e);
		pw = new PathWalker(e);
	}

	public void init(String params) {
		monitored_id = expected_invcount = -1;
		current_action = 0;
		last_inv_count = -1;
		times_banked = 0;
		times_hopped = 0;
		startxp = 0;
		maintain_food_amount = 5;
		maintain_hp_amount = 30;
		extra_items_count = 0;
		paint_mode = 0;
		fail_hop = -1;
		bank_every = 0;
		idle_spot = 0;
		bank_counter = 0;
		auto_fightmode = false;
		debugging = false;
		stop_next_bank = false;
		extra_items_banked = false;
		foundExtraItems = false;
		restocking = false;
		pausing = false;
		foundShieldHalf = false;
		shieldHalfNotifyTest = false;
		isVet = false;
		hop_enabled = false;
		update_fmode = false;
		no_friendly_hop = false;
		logout_flag = false;
		move_time = -1L;
		bank_time = -1L;
		menu_time = -1L;
		item_time = bankaction_time = -1L;
		click_time = -1L;
		last_stuck_check_passed = System.currentTimeMillis();
		hop_time = System.currentTimeMillis();
		hop_delay = 15000L;
		home_world = -1;
		skill_levels = new int[SKILL.length];
		x_offset = 200;

		//Set item prices
		item_prices.put(-1, 0);
		item_prices.put(COINS, 1);
		item_prices.put(BLOOD_RUNE, 1000);

		//Pathwalker
		pw.init(null);
		from_bank = pw.calcPath(bank_pos.x, bank_pos.y, tower_pos.x, tower_pos.y);    //Bank to tower
		to_bank = pw.calcPath(house_pos.x, house_pos.y, bank_pos.x, bank_pos.y);    //house to bank
		to_tower = pw.calcPath(house_pos.x, house_pos.y, tower_pos.x, tower_pos.y);    //house to tower

		for (int i = 0; i < skill_levels.length; ++i) {
			skill_levels[i] = getLevel(i);
		}

		start_time = start_time_exp = System.currentTimeMillis();
		startxp += getAccurateXpForLevel(0);
		startxp += getAccurateXpForLevel(1);
		startxp += getAccurateXpForLevel(2);
		startxp += getAccurateXpForLevel(3);
		startprayerxp += getAccurateXpForLevel(5);
		drops_count = new int[items.length];
		drops_banked = new boolean[items.length];
		req_items_checked = new boolean[req_items.length];
		Arrays.fill(drops_count, 0);
		Arrays.fill(drops_banked, false);
		Arrays.fill(req_items_checked, false);

		showMenu();
	}

	public int main() {
		if (getFightMode() != fight_mode || update_fmode) {
			if (update_fmode) {
				update_fmode = false;
				fight_mode = getNewFightMode();
			}
			System.out.println("Changed fight mode from: " + getFightMode() + " to: " + fight_mode);
			setFightMode(fight_mode);
			return random(320, 640);
		}

		if (inCombat()) {
			if (hasHealthReached(maintain_hp_amount) || (idling_at.x != -1 && idling_at.y != -1)) {
				walkTo(getX(), getY());
				return random(400, 600);
			}

			resetLastPosition();

			if (isUnderground()) {
				if (isNpcAggressive(Config.TARGET_NPC)) {
					if (getFatigue() < Config.SLEEP_AT) {
						int result = runToChestInCombat();
						if (result > 0) return result;
					} else {
						walkTo(618, 3384);    //Run towards ladder from combat if fatigue is too high. Might be unable to teleport out by chest if fatigue is high.
						return random(Config.SLEEPFAST_MIN, Config.SLEEPFAST_MAX);
					}
					return random(200, 300);
				} else {
					int[] chest_object = getObjectById(Config.LOOTABLE_CHEST_ID); //If non-aggressive, bot will only attack an ogre standing between the two chests in order to stand on that tile. So, only run from combat if the chest spawns. Unlikely for fatigue to be a consideration.
					if (chest_object[0] != -1) {
						walkTo(getX(), getY());
						return random(Config.SLEEPFAST_MIN, Config.SLEEPFAST_MAX);
					}
				}
			}
			if (isInsideTower()) {
				walkTo(618, 552);
				return random(400, 600);
			}

			return random(500, 700);
		}

		if (hasHealthReached(maintain_hp_amount)) {
			int food = getInventoryIndex(food_id);
			if (food != -1) {
				useInvItem(food);
				return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
			}
		}

		if (isAtApproxCoords(128, 640, 40)) {
			return stopNow("Died. Stopping script.", false);
		}

		if (pw.walkPath()) {
			return 1;
		}

		if (isQuestMenu()) {
			answer(0);
			menu_time = -1L;
			bank_time = System.currentTimeMillis();
			return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
		} else if (menu_time != -1L) {
			if (System.currentTimeMillis() >= (menu_time + 8000L)) {
				menu_time = -1L;
			}
			return random(Config.SLEEPFAST_MIN, Config.SLEEPFAST_MAX);
		}

		if (isBanking()) {
			bank_time = -1L;
			int banking_result = handleBanking();
			if (banking_result > 0) return banking_result;
		} else if (bank_time != -1L) {
			if (System.currentTimeMillis() >= (bank_time + 8000L)) {
				bank_time = -1L;
			}
			return random(Config.SLEEPFAST_MIN, Config.SLEEPFAST_MAX);
		}

		int dropped_item = dropTrashItems();
		if (dropped_item > 0) return dropped_item;

		if (move_time != -1L) {
			if (System.currentTimeMillis() >= move_time) {
				if (isAtCoords(last_pos.x, last_pos.y)) {
					boolean move_result = false;
					if (move_around.x != -1 && move_around.y != -1) {
						move_result = move_around_npc(move_around.x, move_around.y);
					} else {
						move_result = move_around_npc();
					}
					//move_around_npc();
					if (move_result) return random(Config.SLEEPLONG_MIN, Config.SLEEPLONG_MAX);
				} else if (!isWalking()) {
					resetLastPosition();
				}
			} else {
				return 1;
			}
		}

		if (idling_at.x != -1 && idling_at.y != -1) {
			if (isAtCoords(idling_at.x, idling_at.y)) {
				walk_approx(idling_at.x, idling_at.y);
				return random(Config.SLEEPLONG_MIN, Config.SLEEPLONG_MAX);
			} else {
				idling_at.x = -1;
				idling_at.y = -1;
			}
		}

		if (inBank()) {
			if (isDamageAtLeast(getFoodHealAmount(food_id) - 2)) {    //Waste up to 2 points of healing to top off health before next trip
				int food = getInventoryIndex(food_id);
				if (food != -1) {
					useInvItem(food);
					return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
				}
			}
			int free_food_space = getEmptySlots();
			if (free_food_space > 0) {
				if (!hasInventoryItem(BLOOD_RUNE)) {
					free_food_space--;
				}
				if (!hasInventoryItem(COINS)) {
					free_food_space--;
				}
			}
			if (restocking || getInventoryCount(BLOOD_RUNE) > 1 || getInventoryCount(COINS) > 499 || getInventoryCount(food_id) < maintain_food_amount || free_food_space > 0) {
				int[] banker = getNpcByIdNotTalk(95);
				if (banker[0] != -1) {
					if (distanceTo(banker[1], banker[2]) > 4) {
						walk_approx(banker[1], banker[2]);
						return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
					}
					talkToNpc(banker[0]);
					menu_time = System.currentTimeMillis();
					return random(600, 1000);
				}
			} else {
				if (getFatigue() > 10) {
					useSleepingBagIfExists();
					return 3000;
				}
				pw.setPath(to_tower);
				return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
			}
		}
		if (isUnderground()) {
			if (getFatigue() > Config.SLEEP_AT) {
				if (distanceTo(618, 3384) > 1) {
					checkIfStuckNpc(618, 3384);
					walkTo(618, 3384);    //position to use ladder
					return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
				} else {
					atObject(618, 3383); //ladder position
					return random(Config.SLEEPLONG_MIN, Config.SLEEPLONG_MAX);
				}
			}
			int loot_result = attemptLootChest();
			if (loot_result > 0) {
				return loot_result;
			} else {
				if (isNpcAggressive(Config.TARGET_NPC)) {
					if (!isAtCoords(618, 3384)) {
						if (attackBlockingNpc(new Point(618, 3384))) {
							return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
						}
						checkIfStuckNpc(618, 3384);
						walkTo(618, 3384);    //position to use ladder
					}
				}
				return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
			}
		}

		if (isInsideTower()) {
			if (getFatigue() > 10) {
				useSleepingBagIfExists();
				return 3000;
			}
			int[] ladder = getObjectById(6);
			if (ladder[0] != -1) {
				if (!isWalking()) {
					if (attackBlockingNpc(new Point(ladder[1], ladder[2]))) {
						return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
					}
				}
				checkIfStuckNpc(ladder[1], ladder[2]);
				atObject(ladder[1], ladder[2]);
				return random(Config.SLEEPLONG_MIN, Config.SLEEPLONG_MAX);
			}
		} else if (distanceTo(617, 556) < 5) {
			int[] door = getWallObjectById(Config.DOOR_ID);
			atWallObject2(617, 556);
			return random(Config.SLEEPLONG_MIN, Config.SLEEPLONG_MAX);
		}

		if (isInsideHouse()) {
			if (pausing) {
				pausing = false;
				return stopNow("Pause command was enabled, stopping script now. Start script again to continue.", true);
			}
			if (getWallObjectIdFromCoords(612, 573) == 2) {    //Door
				if (distanceTo(612, 573) > 1) {
					walkTo(612, 574);
					return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
				}
				atWallObject(612, 573);
				return random(Config.SLEEPLONG_MIN, Config.SLEEPLONG_MAX);
			}
			walkTo(612, 574);
			return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
		} else {
			if (getInventoryCount(food_id) >= maintain_food_amount && (bank_counter < bank_every || bank_every == -1) && !restocking) {
				pw.setPath(to_tower);
			} else {
				if (bank_every > 0) {
					bank_counter = 0;
				}
				pw.setPath(to_bank);
			}
			return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
		}
		//To do: Pathwalk to Ardougne north bank
	}

	private static int contains_index(String str, String[] options, int count) {    //Taken from S_TreeFletch
		str = str.toLowerCase(Locale.ENGLISH);
		for (int i = 0; i < count; ++i) {
			if (options[i].toLowerCase(Locale.ENGLISH).contains(str)) {
				return i;
			}
		}
		return -1;
	}

	private int handleBanking() {
		if (bankaction_time != -1L) {
			if (isItemStackableId(monitored_id) ? getInventoryCount(monitored_id) != expected_invcount : getInventoryCount() != expected_invcount) {
				if (System.currentTimeMillis() >= (bankaction_time + 5000L)) {
					bankaction_time = -1L;
				}
				return random(Config.SLEEPFAST_MIN, Config.SLEEPFAST_MAX);
			} else {
				bankaction_time = -1L;
			}
		}

		for (int i = 0; i < items.length; i++) {
			if (drops_banked[i]) continue;
			drops_banked[i] = true;
			int item_count = getInventoryCount(items[i]);
			int item_index = getInventoryIndex(items[i]);
			if (item_index != -1 && isItemEquipped(item_index)) { //Dont deposit equipped items! Decrement to keep worn item.
				item_count--;
			}
			if (item_count > 0) {
				if (item_count <= 0 || items[i] == food_id) continue;
				doBankAction(items[i], item_count, false);
				return random(Config.SLEEPFAST_MIN, Config.SLEEPFAST_MAX);
			}
		}

		if (stop_next_bank) {
			stop_next_bank = false;
			stop_flag = false;
			restocking = false;
			Arrays.fill(drops_banked, false);        //Items will be checked next time script is started
			if (logout_flag) {
				logout_flag = false;
				setAutoLogin(false);
				logout();
			}
			return stopNow("Stop after banking was enabled - stopping script now.", true);
		}

		//withdraw food
		int withdraw_food = 0;
		if (maintain_food_amount > 0) {
			int reserve_slots = 0;
			if (getInventoryCount(BLOOD_RUNE) == 0) reserve_slots++;
			if (getInventoryCount(COINS) == 0) reserve_slots++;
			withdraw_food = getEmptySlots() - reserve_slots;
		}

		if (withdraw_food > 0) {    //If we still have minimum amount of food, but none left in bank, consider this OK to continue. Take amount needed to satisfy maintain_food_amount as long as at least 1 food remains in bank.
			int food_in_bank = bankCount(food_id);
			if (withdraw_food >= food_in_bank) {
				withdraw_food = food_in_bank - 1; //Save one food in bank to preserve order.
			}
			if ((withdraw_food + getInventoryCount(food_id)) >= maintain_food_amount) {
				//withdrawItem(food_id, withdraw_food);
				//return random(1000, 1500);
				doBankAction(food_id, withdraw_food, true);
				return random(Config.SLEEPFAST_MIN, Config.SLEEPFAST_MAX);
			} else {
				return stopNow("Not enough food remaining in bank, stopping script.", false);
			}
		}

		if (isDamageAtLeast(getFoodHealAmount(food_id) - 2)) {
			restocking = true;
			closeBank();
			return random(Config.SLEEPLONG_MIN, Config.SLEEPLONG_MAX);
		}

		restocking = false;
		stop_next_bank = false;
		stop_flag = false;

		Arrays.fill(drops_banked, false);
		Arrays.fill(req_items_checked, false);

		System.out.println("Profits from trip " + times_banked + ": " + String.format("%,d", trip_profits) + " (Total: " + String.format("%,d", total_profits) + ")");
		trip_profits = 0;
		times_banked++;

		closeBank();
		return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
	}

	private Point getChestPos() {
		Point chest_pos = new Point(-1, -1);
		int[] chest_object = getObjectById(Config.LOOTABLE_CHEST_ID);
		if (chest_object[0] != -1) {
			chest_pos.x = chest_object[1];
			chest_pos.y = chest_object[2];
			current_action = 1;
		} else if (idle_spot != 1) {
			chest_pos.x = 614;
			chest_pos.y = 3400;
			current_action = 0;
		}

		return chest_pos;
	}

	private int dropTrashItems() {
		if (!isNpcAt(Config.TARGET_NPC, getX(), getY())) {
			int index = getInventoryIndex(drop_items);
			if (index != -1) {
				if (System.currentTimeMillis() >= (item_time + (last_item_id == getInventoryId(index) ? 1000L : 800L))) {
					last_item_id = getInventoryId(index);
					dropItem(index);
					item_time = System.currentTimeMillis();
				}
				return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
			}
		}
		return 0;
	}

	private int attemptLootChest() {
		Point chest_pos = getChestPos();
		if (chest_pos.x != -1) {
			if (distanceTo(chest_pos.x, chest_pos.y) > 1) {
				if (isNpcAggressive(Config.TARGET_NPC)) {
					if (attackBlockingNpc(chest_pos)) {
						return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
					}
				}
				checkIfStuckNpc(chest_pos.x, chest_pos.y);
				if (getObjectIdFromCoords(chest_pos.x, chest_pos.y) == -1) {
					walkTo(chest_pos.x, chest_pos.y);
				} else {
					walk_approx(chest_pos.x, chest_pos.y);
				}
				return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
			}
			if (getObjectIdFromCoords(chest_pos.x, chest_pos.y) == Config.LOOTABLE_CHEST_ID) {
				atObject2(chest_pos.x, chest_pos.y);
				return random(Config.SLEEPLONG_MIN, Config.SLEEPLONG_MAX);
			} else if (getObjectIdFromCoords(chest_pos.x, chest_pos.y) == -1) {
				if (!isAtCoords(chest_pos.x, chest_pos.y) && isReachable(chest_pos.x, chest_pos.y)) {
					if (attackBlockingNpc(chest_pos)) {
						return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
					}
					walkTo(chest_pos.x, chest_pos.y);
				}
				return random(Config.SLEEP_MIN, Config.SLEEP_MAX);
			}

		}
		return 0;
	}

	private int runToChestInCombat() { //Note: Don't use attack commands here since this is only invoked during combat
		Point chest_pos = getChestPos();

		if (chest_pos.x != -1) {
			if (getObjectIdFromCoords(chest_pos.x, chest_pos.y) == -1) {
				//if (getX() == chest_pos.x && getY() == chest_pos.y) return 0;
				if (isAtCoords(chest_pos.x, chest_pos.y)) return 0;
				walkTo(chest_pos.x, chest_pos.y);
			} else {
				if (distanceTo(chest_pos.x, chest_pos.y) > 1) {
					walk_approx(chest_pos.x, chest_pos.y);
				} else {
					walkTo(getX(), getY());
				}
			}
			return random(Config.SLEEPFAST_MIN, Config.SLEEPFAST_MAX);
		}
		walkTo(getX(), getY());
		return random(Config.SLEEPFAST_MIN, Config.SLEEPFAST_MAX);
	}

	private int[] getNearestValidNpcId(int id) {
		int[] npc = new int[]{-1, -1, -1};
		int count = countNpcs();
		int max_dist = Integer.MAX_VALUE;
		for (int i = 0; i < count; i++) {
			if (isNpcInCombat(i)) continue;
			if (getNpcId(i) == id) {
				int x = getNpcX(i);
				int y = getNpcY(i);
				int dist = distanceTo(x, y, getX(), getY());
				if (dist < max_dist && !isPlayerAt(x, y, 0) && inValidArea(x, y)) {
					npc[0] = i;
					npc[1] = x;
					npc[2] = y;
					if (dist == 0) break;
					max_dist = dist;
				}
			}
		}
		return npc;
	}

	private int[] getAdjacentEnemy() {
		int[] npc = new int[]{-1, -1, -1};
		int count = countNpcs();
		int max_dist = 2;
		for (int i = 0; i < count; i++) {
			if (isNpcInCombat(i)) continue;
			int x = getNpcX(i);
			int y = getNpcY(i);
			int dist = distanceTo(x, y, getX(), getY());
			if (dist < max_dist && !isPlayerAt(x, y, 0) && inValidArea(x, y)) {
				int npc_id = getNpcId(i);
				if (npc_id == Config.TARGET_NPC) {
					npc[0] = i;
					npc[1] = x;
					npc[2] = y;
					if (dist == 0) break;
					max_dist = dist;
				}
			}
		}
		return npc;
	}

	private int[] getNearestEnemy() {
		int cur_x = getX();
		int cur_y = getY();
		int[] npc = getNearestValidNpcId(Config.TARGET_NPC);
		if (npc[0] != -1) return npc;    //NPC pos already validated
		return new int[]{-1, -1, -1};
	}

	private int getArrayIndex(int[] haystack, int needle) {
		int count = haystack.length;
		for (int i = 0; i < count; i++) {
			if (haystack[i] == needle) {
				return i;
			}
		}
		return -1;
	}

	private int getItemPrice(int id) {
		if (item_prices.containsKey(id)) {
			return item_prices.get(id);
		} else {
			return getItemBasePriceId(id);
		}
	}

	private int getFoodHealAmount(int id) {
		switch (id) {
			case SHARK:
				return 20;
			case LOBSTER:
				return 12;
			case SWORDFISH:
				return 16;
		}
		return 0;
	}

	public int getNewFightMode() {
		if (getLevel(2) < 99) return 1;    //If strength less than 99, use aggressive fight mode
		if (getLevel(0) < 99) return 2;    //If attack less than 99, use accurate fight mode
		if (getLevel(1) < 99) return 3;    //If defence less than 99, use defensive fight mode
		return 1;    //Default to aggressive if strength/attack/defence are 99
	}

	public int stopNow(String reason, boolean autologin) {
		System.out.println(reason);
		setAutoLogin(autologin);
		stopScript();
		return 0;
	}

	private void doBankAction(int id, int amount, boolean isWithdrawing) {
		if (isWithdrawing) {
			withdraw(id, amount);
		} else {
			deposit(id, amount);
		}

		update_profits((isWithdrawing ? -amount : amount) * getItemPrice(id));
		if (inArray(items, id)) {
			int index = getArrayIndex(items, id);
			if (index != -1) {
				drops_count[index] += (isWithdrawing ? -amount : amount);
			}
		}

		monitored_id = id;
		expected_invcount = (isItemStackableId(id) ? getInventoryCount(id) : getInventoryCount()) + (isWithdrawing ? amount : -amount);

		bankaction_time = System.currentTimeMillis();
		//if (System.currentTimeMillis() >= (bankaction_time + 1500L) || isItemStackableId(monitored_id) ? getInventoryCount(monitored_id) == expected_invcount : getInventoryCount() == expected_invcount) {

		//System.out.println("doBankAction waiting. id: " + id + ", amount: " + amount + ", withdrawing: " + isWithdrawing);
	}

	private void withdrawItem(int id, int amount) {
		withdraw(id, amount);
		update_profits(-amount * getItemPrice(id));
		if (inArray(items, id)) {
			int index = getArrayIndex(items, id);
			if (index != -1) {
				drops_count[index] -= amount;
			}
		}
	}

	private void depositItem(int id, int amount) {
		deposit(id, amount);
		update_profits(amount * getItemPrice(id));
		if (inArray(items, id)) {
			int index = getArrayIndex(items, id);
			if (index != -1) {
				drops_count[index] += amount;
			}
		}
	}

	private void update_profits(int amount) {
		if (amount == 0) return;
		long total = (long) trip_profits + (long) amount;
		if (total < Integer.MAX_VALUE) {
			trip_profits += amount;
		} else {
			trip_profits = Integer.MAX_VALUE;
		}
		total = (long) total_profits + (long) amount;
		if (total < Integer.MAX_VALUE) {
			total_profits += amount;
		} else {
			total_profits = Integer.MAX_VALUE;
		}
	}

	private void showMenu() {
		if (ch_frame == null) {
			ch_fightmode = new Choice();
			for (int i = 0; i < fight_modes.length; i++) {
				ch_fightmode.add(fight_modes[i]);
			}
			try {
				if (getFightMode() != 0) {
					ch_fightmode.select(getFightMode());
				} else {
					ch_fightmode.select(4);
				}
			} catch (NullPointerException e) {
			}

			ch_foodtype = new Choice();
			for (int i = 0; i < foodOptions.length; i++) {
				ch_foodtype.add(foodOptions[i]);
			}

			ch_idlespot = new Choice();
			for (int i = 0; i < idleSpotChoices.length; i++) {
				ch_idlespot.add(idleSpotChoices[i]);
			}

			Panel pInput = new Panel();
			pInput.setLayout(new GridLayout(0, 2, 0, 2));

			pInput.add(new Label("Fight mode"));
			pInput.add(ch_fightmode);

			pInput.add(new Label("Heal at HP:"));
			pInput.add(tf_heal_at_hp = new TextField(Integer.toString(getLevel(3) - getFoodHealAmount(foodList[0]) - (int) Math.round(Config.HEAL_MARGIN * getFoodHealAmount(foodList[0])))));

			pInput.add(new Label("Food type"));
			pInput.add(ch_foodtype);
			try {
				if (hasInventoryItem(foodList[0])) {
					ch_foodtype.select(0);
				} else if (hasInventoryItem(foodList[1])) {
					ch_foodtype.select(1);
				} else if (hasInventoryItem(foodList[2])) {
					ch_foodtype.select(2);
				}
			} catch (NullPointerException e) {
			}

			pInput.add(new Label("Chest waiting:"));
			pInput.add(ch_idlespot);

			pInput.add(new Label("Minimum food amount:"));
			pInput.add(tf_min_food_amount = new TextField(Integer.toString(isNpcAggressive(Config.TARGET_NPC) ? Config.DEFAULT_FOOD_AMOUNT : 0)));

			pInput.add(new Label("Chests per bank:"));
			pInput.add(tf_bank_every = new TextField("-1"));

			Panel cbPanel = new Panel();
			cbPanel.setLayout(new GridLayout(0, 1));

			Panel buttonPanel = new Panel();
			Button ok = new Button("OK");
			ok.addActionListener(this);
			buttonPanel.add(ok);
			Button cancel = new Button("Cancel");
			cancel.addActionListener(this);
			buttonPanel.add(cancel);

			ch_frame = new Frame(getClass().getSimpleName());
			ch_frame.setIconImages(Constants.ICONS);
			ch_frame.addWindowListener(
				new StandardCloseHandler(ch_frame, StandardCloseHandler.HIDE)
			);
			ch_frame.add(pInput, BorderLayout.NORTH);
			ch_frame.add(cbPanel, BorderLayout.CENTER);
			ch_frame.add(buttonPanel, BorderLayout.SOUTH);
			ch_frame.setResizable(false);
			ch_frame.pack();
		}
		ch_frame.setLocationRelativeTo(null);
		ch_frame.toFront();
		ch_frame.requestFocus();
		ch_frame.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getActionCommand().equals("OK")) {
			if (ch_fightmode.getSelectedIndex() == 4) {                            //Select fight mode
				fight_mode = getNewFightMode();
			} else {
				fight_mode = ch_fightmode.getSelectedIndex();
			}
			food_id = foodList[ch_foodtype.getSelectedIndex()];
			idle_spot = ch_idlespot.getSelectedIndex();
			try {
				maintain_hp_amount = Integer.parseInt(tf_heal_at_hp.getText().trim());    //Get heal point
			} catch (Throwable t) {
				System.out.println("Couldn't parse minimum hp amount");
			}
			try {
				maintain_food_amount = Integer.parseInt(tf_min_food_amount.getText().trim());    //Get heal point
			} catch (Throwable t) {
				System.out.println("Couldn't parse minimum food amount");
			}
			try {
				bank_every = Integer.parseInt(tf_bank_every.getText().trim());//Get food amount
			} catch (Throwable t) {
				System.out.println("Couldn't parse chests per bank");
			}
			System.out.println("Fight mode: " + fight_modes[ch_fightmode.getSelectedIndex()]);
			System.out.println("Food: " + getItemNameId(food_id));
			System.out.println("Idle spot: " + idleSpotChoices[ch_idlespot.getSelectedIndex()]);
			System.out.println("HP to maintain: " + maintain_hp_amount);
			System.out.println("Minimum food: " + maintain_food_amount);
			System.out.println("Chests looted to deposit: " + bank_every);
		}
		ch_frame.setVisible(false);
	}

	public void onKeyPress(int keycode) {
		Frame frame;
		switch (keycode) {
			case KeyEvent.VK_F2:
				showMenu();

				break;
			case KeyEvent.VK_F3:
				String command = JOptionPane.showInputDialog(null, "Command: ", "");
				if (command == null || command.equals("")) {
					//Do nothing
				} else if (command.equals("stopnext")) {    //Stop next banking
					stop_next_bank = !stop_next_bank;
					if (stop_next_bank) {
						System.out.println("Enabled stop on next banking. Script will end after depositing all loot");
					} else {
						System.out.println("Cancelled stop on next banking. Script will continue running");
					}
				} else if (command.equals("banknow")) {
					restocking = true;
					stop_flag = true;
					if (stop_next_bank) stop_next_bank = false;
					System.out.println("Restocking flag set");
				} else if (command.equals("stop") || command.equals("stopnow")) {
					restocking = true;
					stop_next_bank = true;
					stop_flag = true;
					System.out.println("Immediate stop flag set");
				} else if (command.equals("pause")) {
					pausing = !pausing;
					if (pausing) {
						System.out.println("Script will pause after next chest looted.");
					} else {
						System.out.println("Pause command cancelled.");
					}
				}
				break;
			case KeyEvent.VK_F4:
				paint_mode++;
				break;
			case KeyEvent.VK_F5:
				paint_mode--;
				break;
			case KeyEvent.VK_F6:
				break;
			case KeyEvent.VK_F7:
				break;
			case KeyEvent.VK_F8:
				break;
			case KeyEvent.VK_F9:
				break;
			case KeyEvent.VK_F11:
				try {
					x_offset = Integer.parseInt(JOptionPane.showInputDialog(null, "x offset: ", x_offset));
					if (x_offset < 0) {
						x_offset = 200;
					}
				} catch (NumberFormatException e) {
					x_offset = 200;
				}
				break;
		}
	}

	private void checkIfStuckNpc() {
		int cur_x = getX();
		int cur_y = getY();
		if (!isWalking() && isAnyNpcAt(cur_x, cur_y, 1) && cur_x == last_pos.x && cur_y == last_pos.y) {
			if ((System.currentTimeMillis() - last_stuck_check_passed) > Config.MOVE_TIMEOUT) {
				move_time = System.currentTimeMillis() + random(Config.SLEEPLONG_MIN, Config.SLEEPLONG_MAX);
			}
		} else {
			last_pos.setLocation(getX(), getY());
			last_stuck_check_passed = System.currentTimeMillis();
		}
	}

	private void checkIfStuckNpc(int x, int y) {
		int cur_x = getX();
		int cur_y = getY();
		if (!isWalking() && isAnyNpcAt(cur_x, cur_y, 1) && cur_x == last_pos.x && cur_y == last_pos.y) {
			if ((System.currentTimeMillis() - last_stuck_check_passed) > Config.MOVE_TIMEOUT) {
				move_time = System.currentTimeMillis() + random(Config.SLEEPLONG_MIN, Config.SLEEPLONG_MAX);
				move_around.x = x;
				move_around.y = y;
			}
		} else {
			last_pos.setLocation(getX(), getY());
			last_stuck_check_passed = System.currentTimeMillis();
			move_around.x = -1;
			move_around.y = -1;
		}
	}

	private void resetLastPosition() {
		last_pos.setLocation(getX(), getY());
		move_time = -1L;
		move_around.x = -1;
		move_around.y = -1;
		last_stuck_check_passed = System.currentTimeMillis();
	}

	private boolean move_around_npc() {
		if (isWalking()) return false;
		int count = countNpcs();
		int p_x = getX();
		int p_y = getY();
		for (int i = 0; i < count; i++) {
			int npc_x = getNpcX(i);
			int npc_y = getNpcY(i);
			if (distanceTo(npc_x, npc_y, p_x, p_y) == 1) {
				int dir = (Math.random() < 0.5 ? 1 : -1);
				if (npc_x == p_x) {
					if (isReachable(p_x + dir, p_y)) {
						walkTo(p_x + dir, p_y);
					} else if (isReachable(p_x + dir * -1, p_y)) {
						walkTo(p_x + dir * -1, p_y);
					} else {
						walk_approx(p_x, p_y);
					}
					return true;
				}
				if (npc_y == p_y) {
					if (isReachable(p_x, p_y + dir)) {
						walkTo(p_x, p_y + dir);
					} else if (isReachable(p_x, p_y + dir * -1)) {
						walkTo(p_x, p_y + dir * -1);
					} else {
						walk_approx(p_x, p_y);
					}
					return true;
				}
			}
		}
		return false;
	}

	private boolean move_around_npc(int goal_x, int goal_y) {
		if (isWalking()) return false;
		int count = countNpcs();
		int p_x = getX();
		int p_y = getY();
		for (int i = 0; i < count; i++) {
			int npc_x = getNpcX(i);
			int npc_y = getNpcY(i);
			if (distanceTo(npc_x, npc_y, p_x, p_y) == 1 && (distanceTo(npc_x, npc_y, goal_x, goal_y) < distanceTo(p_x, p_y, goal_x, goal_y))) {
				int dir = (Math.random() < 0.5 ? 1 : -1);
				if (npc_x == p_x) {
					if (isReachable(p_x + dir, p_y) && !isAnyNpcAt(p_x + dir, p_y, 0) && (distanceTo(p_x + dir, p_y, goal_x, goal_y) <= distanceTo(p_x + dir * -1, p_y, goal_x, goal_y))) {
						walkTo(p_x + dir, p_y);
					} else if (isReachable(p_x + dir * -1, p_y) && !isAnyNpcAt(p_x + dir * -1, p_y, 0)) {
						walkTo(p_x + dir * -1, p_y);
					} else {
						walk_approx(p_x, p_y);
					}
					return true;
				}
				if (npc_y == p_y) {
					if (isReachable(p_x, p_y + dir) && !isAnyNpcAt(p_x, p_y + dir, 0) && (distanceTo(p_x, p_y + dir, goal_x, goal_y) <= distanceTo(p_x, p_y + dir * -1, goal_x, goal_y))) {
						walkTo(p_x, p_y + dir);
					} else if (isReachable(p_x, p_y + dir * -1) && !isAnyNpcAt(p_x, p_y + dir * -1, 0)) {
						walkTo(p_x, p_y + dir * -1);
					} else {
						walk_approx(p_x, p_y);
					}
					return true;
				}
			}
		}
		return false;
	}

	private boolean isNpcAggressive(int id) {
		try {
			return (getPlayerCombatLevel(0) <= (getNpcCombatLevelId(Config.TARGET_NPC) * 2));
		} catch (NullPointerException e) {
			return true;
		}
		//return (getPlayerCombatLevel(0) <= (getNpcCombatLevelId(Config.TARGET_NPC) * 2));
		//return false;
	}

	private boolean isAtCoords(int x, int y) {
		return (getX() == x && getY() == y);
	}

	private boolean hasHealthReached(int hits) {
		return (getCurrentLevel(3) <= hits);
	}

	private boolean isDamageAtLeast(int damage) {
		return (getCurrentLevel(3) <= (getLevel(3) - damage));
	}

	private boolean attackNearestEnemy() {
		int[] npc = getNearestEnemy();
		if (npc[0] != -1) {
			// checkIfStuckPlayer();
			checkIfStuckNpc(npc[1], npc[2]);
			if (distanceTo(npc[1], npc[2]) > 5) {
				walk_approx(npc[1], npc[2]);
			} else {
				attackNpc(npc[0]);
			}
			return true;
		}
		return false;
	}

	private boolean attackBlockingNpc(Point goal_pos) {
		int count = countNpcs();
		for (int i = 0; i < count; i++) {
			if (isNpcInCombat(i)) continue;
			int x = getNpcX(i);
			int y = getNpcY(i);
			if (distanceTo(x, y) == 1 && isWithinBounds(x, y, Math.min(getX(), goal_pos.x), Math.min(getY(), goal_pos.y), Math.max(getX(), goal_pos.x), Math.max(getY(), goal_pos.y)) && (distanceTo(x, y, goal_pos.x, goal_pos.y) < distanceTo(getX(), getY(), goal_pos.x, goal_pos.y))) {
				attackNpc(i);
				return true;
			}
		}
		return false;
	}

	private boolean attackAdjacentEnemy() {
		if (isWalking() || getCurrentLevel(3) <= maintain_hp_amount) return false;
		int[] npc = getAdjacentEnemy();
		if (npc[0] != -1) {
			attackNpc(npc[0]);
			return true;
		}
		return false;
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

	private boolean inValidArea(int x, int y) {
		if (!isReachable(x, y)) return false;
		return isWithinBounds(x, y, area_min.x, area_min.y, area_max.x, area_max.y);
	}

	private boolean isAnyNpcAt(int x, int y, int dist) {
		// if (isWalking()) return false;
		int count = countNpcs();
		for (int i = 0; i < count; i++) {
			int p_x = getNpcX(i);
			int p_y = getNpcY(i);
			if (isReachable(p_x, p_y) && distanceTo(p_x, p_y, x, y) <= dist) return true;

		}
		return false;
	}

	private boolean isFightingNpcId(int id) {
		int count = countNpcs();
		for (int i = 0; i < count; i++) {
			int p_id = getNpcId(i);
			if (p_id == id) {
				int p_x = getNpcX(i);
				int p_y = getNpcY(i);
				if (isNpcInCombat(i) && isAtCoords(p_x, p_y)) return true;
			}
		}
		return false;
	}

	private boolean isNpcAt(int id, int x, int y) {
		int count = countNpcs();
		for (int i = 0; i < count; i++) {
			int p_id = getNpcId(i);
			if (p_id == id) {
				int p_x = getNpcX(i);
				int p_y = getNpcY(i);
				if (x == p_x && y == p_y) return true;
			}
		}
		return false;
	}

	private boolean isNpcInCombatAt(int id, int x, int y, int dist) {
		int count = countNpcs();
		for (int i = 0; i < count; i++) {
			int p_id = getNpcId(i);
			if (p_id == id && isNpcInCombat(i)) {
				int p_x = getNpcX(i);
				int p_y = getNpcY(i);
				if (distanceTo(x, y, p_x, p_y) <= dist) return true;
			}
		}
		return false;
	}

	private boolean isPlayerInCombatAt(int x, int y, int dist) {    //Checks for players in combat with blue dragon within range (exclude self)
		// if (isWalking()) return false;
		int count = countPlayers();
		for (int i = 1; i < count; i++) {
			if (isPlayerInCombat(i)) {
				int p_x = getPlayerX(i);
				int p_y = getPlayerY(i);
				if (distanceTo(p_x, p_y, x, y) <= dist && isNpcInCombatAt(Config.TARGET_NPC, p_x, p_y, 0)) return true;
			}
		}
		return false;
	}

	private boolean isPlayerAt(int x, int y, int dist) {
		int count = countPlayers();
		for (int i = 1; i < count; i++) {
			int p_x = getPlayerX(i);
			int p_y = getPlayerY(i);
			if (isReachable(p_x, p_y) && distanceTo(x, y, p_x, p_y) <= dist) return true;
		}
		return false;
	}

	private boolean inBank() {
		return isWithinBounds(getX(), getY(), bank_min.x, bank_min.y, bank_max.x, bank_max.y);
	}

	private boolean isInsideHouse() {
		return (isReachable(614, 568) && isWithinBounds(getX(), getY(), house_min.x, house_min.y, house_max.x, house_max.y));
	}

	private boolean isInsideTower() {
		return (isReachable(618, 552) && isWithinBounds(getX(), getY(), tower_min.x, tower_min.y, tower_max.x, tower_max.y));
	}

	private boolean isUnderground() {
		return (isReachable(618, 3384) && getY() > 3382);
	}

	private boolean useInvItem(int index) {
		if (System.currentTimeMillis() >= (item_time + 1500L) || getInventoryCount() == (last_inv_count - 1)) {
			if (!isItemStackable(index)) {
				last_inv_count = getInventoryCount();
			}
			item_time = System.currentTimeMillis();
			useItem(index);
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

		drawString("Just_ArdougneBloodChest", x - 4, y - 17, 4, green);
		y += 15;
		drawString("Runtime: " + getRunTime(), x, y, 1, yellow);
		//y += 15;
		//drawString("Combat EXP rate: " + (int)((getAccurateXpForLevel(2) + getAccurateXpForLevel(1) + getAccurateXpForLevel(0) + getAccurateXpForLevel(3) - startxp) * 60.0D * 60.0D / (double) var1) + "/h", x, y, 1, yellow);
		// y += 15;
		// drawString("Prayer XP rate: " + (int)((getAccurateXpForLevel(5) - startprayerxp) * 60.0D * 60.0D / (double) var1)+ "/h", x, y, 1, yellow);
		if (hop_enabled) {
			y += 15;
			drawString("Home world: " + home_world + ". Vet: " + isVet, x, y, 1, yellow);
		}
		y += 15;
		drawString("Chests looted: " + chests_looted, x, y, 1, yellow);
		if (bank_every > 0) {
			y += 15;
			drawString("Chests until next bank: " + (bank_every - bank_counter), x, y, 1, yellow);
			y += 15;
			drawString("Times banked: " + times_banked, x, y, 1, yellow);
		}
		if (pausing) {
			y += 15;
			drawString("Pause on next chest looted", x, y, 1, red);
		}
		if (stop_next_bank) {
			y += 15;
			drawString("Stopping on next banking", x, y, 1, red);
		}
		x = getGameWidth() - x_offset;
		y = 50;
		if (paint_mode == 0) {
			try {
				if (getY() > 3300) {
					boolean chest1_available = getObjectIdFromCoords(chest1_pos.x, chest1_pos.y) == Config.LOOTABLE_CHEST_ID;
					boolean chest2_available = getObjectIdFromCoords(chest2_pos.x, chest2_pos.y) == Config.LOOTABLE_CHEST_ID;
					drawString("Chest status: ", x, y, 1, yellow);
					y += 15;
					drawString(String.format("North: %s", chest1_available ? "Available" : "Looted"), x, y, 1, chest1_available ? green : white);
					y += 15;
					drawString(String.format("South: %s", chest2_available ? "Available" : "Looted"), x, y, 1, chest2_available ? green : white);
					y += 20;
				}
			} catch (NullPointerException e) {
			}

			drawString("Loot collected: ", x, y, 1, yellow);
			y += 15;
			for (int i = 0; i < items.length; ++i) {
				int inv_count = getInventoryCount(items[i]);
				if (drops_count[i] <= 0 && inv_count <= 0) continue;
				drawString(getItemNameId(items[i]) + ": " + (ifmt(drops_count[i] + inv_count)), x, y, 1, getItemBasePriceId(items[i]) < Config.RARE_MIN_VALUE ? white : green);
				y += 15;
			}
		} else if (paint_mode == 1) {
			drawString("Levels gained: ", x, y, 1, yellow);
			y += 15;
			for (int i = 0; i < skill_levels.length; i++) {
				int gain = getLevel(i) - skill_levels[i];
				if (gain == 0) continue;
				drawString(String.format("%s: %d", SKILL[i], gain), x, y, 1, white);
				y += 15;
			}
		} else if (paint_mode < 0) {
			paint_mode = 1;
		} else {
			paint_mode = 0;
		}
	}

	private static String get_time_since(long t) {    //Taken from S_Catherby
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

	private String per_hour(long count, long start_time) {    //Taken from S_Catherby
		double amount, secs;

		if (count == 0) return "0";
		amount = count * 60.0 * 60.0;
		secs = (System.currentTimeMillis() - start_time) / 1000.0;
		return int_format.format(amount / secs);
	}

	private String ifmt(long l) {
		return int_format.format(l);
	}

	@Override
	public void onServerMessage(String str) {
		str = str.toLowerCase(Locale.ENGLISH);
		if (str.contains("advanced")) {            //On levelup
			if (auto_fightmode && (str.contains("strength") || str.contains("attack") || str.contains("defence"))) {
				update_fmode = true;
			}
			System.out.println(str);
		} else if (str.contains("treasure")) {
			chests_looted++;
			bank_counter++;
		} else if (str.contains("busy")) {
			menu_time = -1L;
		} else if (str.contains("welcome")) {
			if (fail_hop != -1) {
				fail_hop = -1;
			}
			times_hopped++;
			hop_time = System.currentTimeMillis();
		} else if (str.contains("have been standing")) {
			idling_at.x = getX();
			idling_at.y = getY();
		} else if (str.contains("sorry")) {
			if (fail_hop == -1) {
				fail_hop = getWorld();    //Prevent skipped worlds.
			}
			hop_time = System.currentTimeMillis() - 5000L;    //Reduce hop timer if failed logout.
		}
	}

	private void useSleepingBagIfExists() {
		if (hasInventoryItem(SLEEPING_BAG)) {
			useSleepingBag();
			resetLastPosition();
		}
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

	private void walk_approx(int x, int y, int dist) {
		int dx, dy;
		int loop = 0;
		do {
			dx = x + random(-dist, dist);
			dy = y + random(-dist, dist);
			if ((++loop) > 100) return;
		} while (!isReachable(dx, dy) ||
			(dx == getX() && dy == getY()));
		walkTo(dx, dy);
	}

	public void setNewFightMode(int fight_mode) {
		Just_ArdougneBloodChest.fight_mode = fight_mode;
	}

	public static class Config {
		public static final int SLEEP_AT = 90;    //Sleep at fatigue level
		public static final int MOVE_TIMEOUT = 1600;
		public static final int DEFAULT_FOOD_AMOUNT = 5;
		public static final int TARGET_NPC = 312;
		public static final int RARE_MIN_VALUE = 3000;
		public static final int DOOR_ID = 96;
		public static final int LOOTABLE_CHEST_ID = 337;
		public static final int SLEEP_MIN = 640;
		public static final int SLEEP_MAX = 800;
		public static final int SLEEPFAST_MIN = 300;
		public static final int SLEEPFAST_MAX = 400;
		public static final int SLEEPLONG_MIN = 1000;
		public static final int SLEEPLONG_MAX = 1200;
		public static final double HEAL_MARGIN = 0.3;
	}
}
