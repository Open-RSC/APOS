import com.aposbot.Constants;
import com.aposbot.StandardCloseHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;

public class Just_OtherworldlyBeings extends Script implements ActionListener {
	private static boolean update_fmode;
	private static boolean restocking = false;
	private static boolean foundShieldHalf = false;
	private static int fight_mode;
	private static int times_banked;
	private static int[] drops_count;
	private static int food_id;
	private static int food_amount;
	private static int sleep_fatigue;
	private static int paint_mode;
	private static int super_checked;
	private static boolean withdraw_prayer;
	private static boolean withdraw_strength;
	private static boolean[] drops_banked;
	private static boolean extra_items_banked;
	private static boolean[] req_items_checked;
	private static boolean stop_next_bank;
	private static boolean logout_flag;
	private static boolean stop_flag;
	private static long bank_time;
	private static long menu_time;
	private static long item_time;
	private static long last_spell_fail;
	private static long last_hop_attempt;
	private static long[] last_seen_npc;
	private static String alchemy_text;
	private static int[] skill_levels;
	private static int total_profits;    //Profits for entire script run time
	private static int trip_profits;    //Profits for current trip
	private static int disconnect_count;
	private static int rare_value;
	private static int alch_income_total;
	private static int alch_income_queue;
	private static long disconnect_time;
	private static long last_spawn_check;
	private static long last_player_check;
	private static List<String> players_seen = new ArrayList<>();
	private static List<Long> players_time = new ArrayList<>();
	private final List<Integer> unlisted_items = new ArrayList<>();
	private final List<Integer> unlisted_amount = new ArrayList<>();
	private final HashMap<Integer, Integer> item_prices = new HashMap<Integer, Integer>(64);

	//Server hop
	private static int times_hopped;
	private static int fail_hop;
	private static long hop_time;
	private static long hop_delay;

	//Menu
	private static Frame frame;        //Used for fightmode, food type, food amount
	private static Frame ch_frame;    //Used for fightmode selection, food
	private static Frame cb_frame;    //Used for option selection
	private Choice ch_fightmode;
	private Choice ch_foodtype;
	private Choice ch_roommode;
	private TextField tf_food_amount;
	private static Checkbox cb_supers;
	private static Checkbox cb_defense;
	private static Checkbox cb_prayers;
	private static Checkbox cb_bury;
	private static Checkbox cb_alchemy;
	private static Checkbox cb_identify;
	private static Checkbox cb_nosleep;

	//Timing
	private static int sleep_min = 600;
	private static int sleep_max = 800;

	//Item use rate limiter
	private static int last_item_id;

	//Points
	private static final Point tele_check = new Point(-1, -1);    //Used for checking if bot was unexpectedly is_returning
	private static final Point last_pos = new Point(-1, -1);    //Used for checking if bot is blocked by an npc
	private static final Point bank_min0 = new Point(172, 3521);
	private static final Point bank_max0 = new Point(176, 3528);
	private static final Point bank_min1 = new Point(170, 3521);
	private static final Point bank_max1 = new Point(171, 3525);
	private static final Point bank_min2 = new Point(172, 3529);
	private static final Point bank_max2 = new Point(174, 3529);
	private static final Point area_min = new Point(171, 3541);
	private static final Point area_max = new Point(180, 3550);
	private static final Point bank_interior = new Point(173, 3528);
	private static final Point bank_exterior = new Point(170, 3528);
	private static final Point dungeon_entrance = new Point(172, 3542);

	private static final Point[] spawn_positions = {
		new Point(178, 3545),
		new Point(178, 3543),
		new Point(176, 3549),
		new Point(174, 3548),
		new Point(172, 3546),
		new Point(178, 3547),
		new Point(175, 3542)
	};

	private static final Point[] tele_whitelist = {
	};

	private static Point home_pos = spawn_positions[0];

	//Prayers;
	private static final int PRAY_SUPER_STRENGTH = 4;
	private static final int PRAY_IMP_REFLEX = 5;
	private static final int PRAY_ULT_STRENGTH = 10;
	private static final int PRAY_INC_REFLEX = 11;
	private static final int PRAY_ROCK_SKIN = 3;
	private static final int PRAY_STEEL_SKIN = 9;

	//Pathwalker
	private final PathWalker pw;
	private PathWalker.Path dungeon_to_bank;
	private PathWalker.Path bank_to_dungeon;

	private final String[] fight_modes = {
		"Controlled",
		"Strength",
		"Attack",
		"Defense",
		"Auto Select"    //Check if fight mode needs to be changed upon starting each trip.
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

	private static int startxp;
	private static long start_time;
	private static long start_time_exp;
	private static long move_time;
	private static long door_time;
	private static long last_stuck_check_passed;
	private static int x_offset;

	private double startprayerxp = 0.0D;

	private final DecimalFormat int_format = new DecimalFormat("#,##0");
	private final DecimalFormat iformat = int_format;

	private static final int
		COINS = 10,
		NORMAL_BONES = 20,
		BIG_BONES = 413,
		SLEEPING_BAG = 1263,
		AIR_RUNE = 33,
		FIRE_RUNE = 31,
		WATER_RUNE = 32,
		EARTH_RUNE = 34,
		NATURE_RUNE = 40,
		LAW_RUNE = 42,        //Ice giant
		DEATH_RUNE = 38,    //Ice giant
		COSMIC_RUNE = 46,    //Ice giant
		CHAOS_RUNE = 41,
		BLOOD_RUNE = 619,    //Ice giant
		MIND_RUNE = 35,        //Ice giant
		BODY_RUNE = 36,
		UNID_GUAM = 165,
		UNID_MARRENTILL = 435,
		UNID_TARROMIN = 436,
		UNID_HARRALANDER = 437,
		UNID_RANARR = 438,
		UNID_IRIT = 439,
		UNID_AVANTOE = 440,
		UNID_KWUARM = 441,
		UNID_CADANTINE = 442,
		UNID_DWARFWEED = 443,
		ID_GUAM = 444,
		ID_MARRENTILL = 445,
		ID_TARROMIN = 446,
		ID_HARRALANDER = 447,
		ID_RANARR = 448,
		ID_IRIT = 449,
		ID_AVANTOE = 450,
		ID_KWUARM = 451,
		ID_CADANTINE = 452,
		ID_DWARF = 453,
		GNOME_BALL = 981,
		EMPTY_VIAL = 465,
		UNCUT_DIAMOND = 157,
		UNCUT_RUBY = 158,
	// UNCUT_EMERALD = 159,
	// UNCUT_SAPPHIRE = 158,
	CRYSTAL_KEY = 525,
		KEY_HALF1 = 526,            //Teeth
		KEY_HALF2 = 527,            //Loop
		MITH_SQ = 126,                //Fire giant
		SHARK = 546,
		LOBSTER = 373,                //Fire giant
		SWORDFISH = 370,
		ROPE = 237,
		RUNE_BAR = 408,                //Fire giant
		DRAGON_MED = 795,            //Fire giant
		RUNE_KITE = 404,            //Fire giant
		RUNE_SQUARE = 403,            //Fire giant
		RUNE_AXE = 405,                //Fire giant
		RUNE_BAXE = 93,            //Fire giant
		RUNE_SCIMITAR = 398,        //Fire giant
		RUNE_2H = 81,                //Fire giant
		BSTAFF_OF_FIRE = 615,        //Fire giant
		STRENGTH_POTION_1D = 224,    //Fire giant
		STRENGTH_POTION_2D = 223,    //Fire giant
		DRAGONSTONE = 523,            //Fire giant
		STEEL_BAR = 171,            //Fire giant
		COAL_CERT = 518,            //Fire giant (stacks of 20)
		SILVER_CERT = 520,            //Fire giant (stacks of 20)
		BRONZE_ARROWS = 11,            //Fire giant (stacks of 300)
		DSQ_HALF = 1277,
		RUNE_SPEAR = 1092,
		DRAGON_AXE = 594,
		DRAGON_SWORD = 593,
		STAFF_OF_AIR = 101,
		STAFF_OF_FIRE = 197,
		DRAGON_AMULET_U = 522,    //Uncharged dragonstone amulet
		DRAGON_AMULET_C = 597,    //Charged dragonstone amulet
		DIAMOND_AMULET = 317,    //Diamond Amulet of power
		GLARIAL_AMULET = 782,    //Glarial's Amulet
		COOKING_GAUNTLETS = 700,
		HARPOON = 379,
		RAW_SHARK = 545,
		COOKED_SHARK = 546,
		S_DEFENSE_POTION_1D = 497,
		WHITEBERRIES = 471,
		LIMPWURT_ROOT = 220,
		VIAL_WATER = 464,
		SNAPE_GRASS = 469,
		RUBY_RING = 286,
		BURNT_SHARK = 547;
	//DSQ_HALF = 10;	//Testing

	private static final int[] STRENGTH_POTIONS = {            //Strength potions in order of increasing dose
		STRENGTH_POTION_1D,
		STRENGTH_POTION_2D
	};

	private static final int[] SUPER_STRENGTH_POTIONS = {    //Super potions in order of increasing dose
		494, 493, 492
	};

	private static final int[] SUPER_ATTACK_POTIONS = {        //Super potions in order of increasing dose
		488, 487, 486
	};

	private static final int[] SUPER_DEFENSE_POTIONS = {    //Super potions in order of increasing dose
		497, 496, 495
	};

	private static final int[] PRAYER_POTIONS = {            //Prayer potions in order of increasing dose
		485, 484, 483
	};

	private static final int[] items = {    //Items to be picked up and banked
		DSQ_HALF,
		CRYSTAL_KEY,
		KEY_HALF1,
		KEY_HALF2,
		// COINS,
		LAW_RUNE,
		DEATH_RUNE,
		COSMIC_RUNE,
		// BLOOD_RUNE,
		// NATURE_RUNE,
		// FIRE_RUNE,
		CHAOS_RUNE,
		// SHARK,				//in case of death
		// LOBSTER,
		// UNCUT_DIAMOND,
		// UNCUT_RUBY,
		// UNCUT_EMERALD,
		// UNCUT_SAPPHIRE,
		// WHITEBERRIES,
		// LIMPWURT_ROOT,
		// SNAPE_GRASS,
		// VIAL_WATER,
		RUBY_RING,
		UNID_RANARR,
		UNID_IRIT,
		UNID_AVANTOE,
		UNID_KWUARM,
		UNID_CADANTINE,
		UNID_DWARFWEED
	};

	private static final int[] req_items = {    //Items absolutely required for script to function.
		SLEEPING_BAG
	};

	private static final int[] drop_items = {    //Items to be dropped if in inventory
		ID_GUAM,
		ID_MARRENTILL,
		ID_HARRALANDER,
		ID_TARROMIN,
		ID_RANARR,            //Script sometimes IDs herbs that aren't supposed to be IDed. Drop these.
		ID_IRIT,
		ID_AVANTOE,
		ID_KWUARM,
		ID_CADANTINE,
		ID_DWARF,
		GNOME_BALL,
		EMPTY_VIAL
	};

	private static final int[] use_items = {    //Items to be used immediately on pickup as long as Fatigue is below certain amount (for fatigue-increasing items). These items won't be banked.
		S_DEFENSE_POTION_1D,
		NORMAL_BONES,
		UNID_GUAM,
		UNID_MARRENTILL,
		UNID_HARRALANDER,
		UNID_TARROMIN
	};

	private static final int[] alch_items = {    //Items to be high-alched immediately on pickup if have required runes/staff
	};

	public Just_OtherworldlyBeings(Extension e) {
		super(e);
		pw = new PathWalker(e);
	}

	public void init(String params) {
		times_banked = 0;
		times_hopped = 0;
		startxp = 0;
		food_amount = Config.DEFAULT_FOOD_AMOUNT;
		paint_mode = 0;
		super_checked = 0;
		disconnect_count = 0;
		total_profits = trip_profits = 0;
		logout_flag = false;
		stop_flag = false;
		stop_next_bank = false;
		extra_items_banked = false;
		update_fmode = false;
		withdraw_prayer = true;
		withdraw_strength = true;
		sleep_fatigue = Config.SLEEP_AT;
		alchemy_text = "";
		move_time = -1L;
		bank_time = -1L;
		menu_time = -1L;
		door_time = -1L;
		item_time = -1L;
		last_spell_fail = -1L;
		last_stuck_check_passed = System.currentTimeMillis();
		disconnect_time = -1L;
		last_hop_attempt = -1L;
		skill_levels = new int[SKILL.length];
		last_seen_npc = new long[spawn_positions.length];
		x_offset = 200;
		rare_value = -1;
		hop_time = System.currentTimeMillis();
		hop_delay = 15000L;
		fail_hop = -1;
		last_item_id = 0;
		alch_income_total = alch_income_queue = 0;

		//Set item prices
		item_prices.put(-1, 0);
		item_prices.put(COINS, 1);
		item_prices.put(KEY_HALF1, 200000);
		item_prices.put(KEY_HALF2, 200000);
		item_prices.put(DSQ_HALF, 20000000);
		item_prices.put(FIRE_RUNE, 35);
		item_prices.put(CHAOS_RUNE, 80);
		item_prices.put(LAW_RUNE, 1000);
		item_prices.put(DEATH_RUNE, 1000);
		item_prices.put(BLOOD_RUNE, 1000);
		item_prices.put(NATURE_RUNE, 600);
		item_prices.put(DRAGON_MED, 1700000);
		item_prices.put(DRAGON_AMULET_C, 450000);
		item_prices.put(DRAGON_AMULET_U, 450000);
		item_prices.put(DRAGON_AXE, 200000);
		item_prices.put(DRAGON_SWORD, 100000);
		item_prices.put(DRAGONSTONE, 450000);
		item_prices.put(RUNE_KITE, 70000);
		item_prices.put(RUNE_2H, 70000);
		item_prices.put(RUNE_BAXE, 70000);
		item_prices.put(RUNE_AXE, 45000);
		item_prices.put(RUNE_BAR, 25000);
		item_prices.put(COAL_CERT, 1500);
		item_prices.put(SHARK, 600);
		item_prices.put(LOBSTER, 200);
		item_prices.put(SWORDFISH, 300);
		item_prices.put(STEEL_BAR, 1400);
		item_prices.put(RUNE_SPEAR, 3000000);
		item_prices.put(BRONZE_ARROWS, 10);
		item_prices.put(UNID_RANARR, 5000);
		item_prices.put(UNID_IRIT, 2500);
		item_prices.put(UNID_AVANTOE, 1000);
		item_prices.put(UNID_KWUARM, 3500);
		item_prices.put(UNID_CADANTINE, 3500);
		item_prices.put(UNID_DWARFWEED, 2000);
		item_prices.put(WHITEBERRIES, 2000);
		item_prices.put(LIMPWURT_ROOT, 2000);
		item_prices.put(VIAL_WATER, 100);
		item_prices.put(BIG_BONES, 0);
		item_prices.put(SNAPE_GRASS, 1000);
		item_prices.put(486, 3000);    //Super attack potions
		item_prices.put(483, 3000);    //Restore prayer
		item_prices.put(492, 6000);    //Super strength
		item_prices.put(495, 8000);    //Super defense
		item_prices.put(387, Integer.MAX_VALUE);    //Disk of returning
		item_prices.put(422, Integer.MAX_VALUE);    //Pumpkin
		item_prices.put(575, Integer.MAX_VALUE);    //Christmas cracker
		item_prices.put(576, Integer.MAX_VALUE);    //Party Hat
		item_prices.put(577, Integer.MAX_VALUE);    //Party Hat
		item_prices.put(578, Integer.MAX_VALUE);    //Party Hat
		item_prices.put(579, Integer.MAX_VALUE);    //Party Hat
		item_prices.put(580, Integer.MAX_VALUE);    //Party Hat
		item_prices.put(581, Integer.MAX_VALUE);    //Party Hat
		item_prices.put(677, Integer.MAX_VALUE);    //Easter egg
		item_prices.put(828, Integer.MAX_VALUE);    //Halloween mask
		item_prices.put(831, Integer.MAX_VALUE);    //Halloween mask
		item_prices.put(832, Integer.MAX_VALUE);    //Halloween mask
		item_prices.put(246, Integer.MAX_VALUE);    //Half wine
		item_prices.put(971, Integer.MAX_VALUE);    //Santa hat
		item_prices.put(1278, 20000000);            //DSQ shield

		//Pathwalker
		pw.init(null);
		dungeon_to_bank = pw.calcPath(dungeon_entrance.x, dungeon_entrance.y, bank_exterior.x, bank_exterior.y);
		bank_to_dungeon = pw.calcPath(bank_exterior.x, bank_exterior.y, dungeon_entrance.x, dungeon_entrance.y);

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
		Arrays.fill(last_seen_npc, System.currentTimeMillis());

		if (params == null || params.isEmpty()) {
			showMenu();
		} else if (params.equals("default")) {
			fight_mode = getNewFightMode();
			food_id = foodList[0];
			food_amount = Config.DEFAULT_FOOD_AMOUNT;
			System.out.println("Default settings loaded");
		}
	}

	public int main() {
		teleportCheck();
		if (getFightMode() != fight_mode || update_fmode) {
			if (update_fmode) {
				update_fmode = false;
				fight_mode = getNewFightMode();
			}
			setFightMode(fight_mode);
		}
		if (inFarmingArea()) {
			if ((System.currentTimeMillis() - last_spawn_check) >= 600L) {
				checkSpawns();
				last_spawn_check = System.currentTimeMillis();
			}

			if ((System.currentTimeMillis() - last_player_check) >= 5000L) {
				checkPlayers();
				last_player_check = System.currentTimeMillis();
			}
		}

		if (inCombat()) {
			if ((hasHealthReached(isReachablePlayerAt(getX(), getY(), 4) ? Config.RUN_AT : Config.HEAL_AT) && getInventoryIndex(foodList) != -1) || needToPickup() || !canContinue()) {
				walkTo(getX(), getY());
				return random(400, 600);
			}
			resetLastPosition();
			return random(500, 700);
		}

		if (hasHealthReached(Config.HEAL_AT)) {
			int food = getInventoryIndex(food_id);
			if (food != -1) {
				useInvItem(food);
				return random(sleep_min, sleep_max);
			}
		}

		if (isAtApproxCoords(128, 640, 40)) {
			return stopNow("Died. Stopping script.", false);
		}

		if (pw.walkPath()) {
			return 0;
		}

		if (isQuestMenu()) {
			answer(0);
			menu_time = -1L;
			bank_time = System.currentTimeMillis();
			return random(600, 800);
		} else if (menu_time != -1L) {
			if (System.currentTimeMillis() >= (menu_time + 8000L)) {
				menu_time = -1L;
			}
			return random(300, 400);
		}

		if (isBanking()) {
			bank_time = -1L;
			int banking_result = handleBanking();
			if (banking_result > 0) return banking_result;
		} else if (bank_time != -1L) {
			if (System.currentTimeMillis() >= (bank_time + 8000L)) {
				bank_time = -1L;
			}
			return random(300, 400);
		}

		int dropped_item = dropTrashItems();
		if (dropped_item > 0) return dropped_item;

		if (move_time != -1L) {
			if (System.currentTimeMillis() >= move_time) {
				if (isAtCoords(last_pos.x, last_pos.y)) {
					move_around_npc();
					return random(800, 1000);
				} else if (!isWalking()) {
					resetLastPosition();
				}
			} else {
				return 0;
			}
		}

		if (canContinue()) {
			if (inFarmingArea()) {
				int result = pickUpLoot();
				if (result > 0) {
					return result;
				}
				if (!hasInventoryItem(food_id)) {
					if (needToPickup()) {
						result = moveToLoot();
						if (result > 0) {
							return result;
						}
					}
					return random(600, 800);
				}
				if (getFatigue() >= sleep_fatigue) {
					useSleepingBag2();
					return 3000;
				}
				boolean attackedAdjacent = attackAdjacentEnemy();
				if (attackedAdjacent) {
					return random(600, 1000);
				}
				result = moveToLoot();
				if (result > 0) {
					return result;
				}
				int checked_items = checkUsableItems();
				if (checked_items > 0) {
					return checked_items;
				}
				boolean fightWasInitiated = attackNearestEnemy();
				if (fightWasInitiated) {
					return random(600, 1000);
				}
				int moved = moveToOldestSpawn();
				if (moved > 0) return moved;

				return random(800, 1000);
			}

			if (inBank()) {
				if (isDamageAtLeast(getFoodHealAmount(food_id) - 2)) {    //Waste up to 2 points of healing to top off health before next trip
					int food = getInventoryIndex(food_id);
					if (food != -1) {
						useInvItem(food);
						return random(sleep_min, sleep_max);
					}
				}
				if (isReadyToFarm()) {
					if (getFatigue() > 10) {
						useSleepingBag2();
						return 2000;
					}
					if (isReachable(bank_exterior.x, bank_exterior.y)) {
						walkTo(bank_exterior.x, bank_exterior.y);
						return random(600, 800);
					} else {
						int gate = getObjectIdFromCoords(171, 3527);
						if (gate == 57) {
							atObject(171, 3527);
							return random(1000, 1200);
						}
					}
				} else {
					restocking = true;
				}
				return random(600, 800);
			} else {
				pw.setPath(bank_to_dungeon);
				return random(600, 800);
			}
		} else {
			if (inBank()) {
				if (!isBanking()) {
					if (getFatigue() >= sleep_fatigue) {
						useSleepingBag2();
						return 3000;
					}
					int made_key = makeCrystalKey();
					if (made_key > 0) {
						return made_key;
					}
					int checked_items = checkUsableItems();
					if (checked_items > 0) {
						return checked_items;
					}
					if (isDamageAtLeast(getFoodHealAmount(food_id) - 2)) {    //Waste up to 2 points of healing to top off health before next trip
						int food = getInventoryIndex(food_id);
						if (food != -1) {
							useInvItem(food);
							return random(sleep_min, sleep_max);
						}
					}
					int[] banker = getNpcByIdNotTalk(224);
					if (banker[0] != -1) {
						if (distanceTo(banker[1], banker[2]) > 4) {
							walk_approx(banker[1], banker[2]);
							return random(600, 800);
						}
						talkToNpc(banker[0]);
						menu_time = System.currentTimeMillis();
						return random(600, 1000);
					}
				}
			} else {
				if (!stop_flag) {
					int result = pickUpLoot();
					if (result > 0) {
						return result;
					}
				}
				if (isAtApproxCoords(bank_exterior.x, bank_exterior.y, 1)) {
					if (isReachable(bank_interior.x, bank_interior.y)) {
						walkTo(bank_interior.x, bank_interior.y);
						return random(600, 800);
					} else {
						int gate = getObjectIdFromCoords(171, 3527);
						if (gate == 57) {
							atObject(171, 3527);
							return random(1000, 1200);
						}
					}
				}
				if (isAtApproxCoords(dungeon_entrance.x, dungeon_entrance.y, 1)) {
					pw.setPath(dungeon_to_bank);
					return random(600, 800);
				} else if (isReachable(dungeon_entrance.x, dungeon_entrance.y)) {
					checkIfStuckNpc();
					walkTo(dungeon_entrance.x, dungeon_entrance.y);
					return random(600, 800);
				}
			}
		}
		return random(800, 1000);
	}

	private int getInventoryWeapon() {
		int index = -1;
		if (isQuestComplete(19)) {
			index = getInventoryIndex(DRAGON_AXE);
			if (index == -1) index = getInventoryIndex(DRAGON_SWORD);
		} else {
			index = getInventoryIndex(DRAGON_SWORD);
		}
		return index;
	}

	private int getInventoryAmulet() {
		int index = getInventoryIndex(DRAGON_AMULET_C);
		if (index == -1) index = getInventoryIndex(DRAGON_AMULET_U);
		if (index == -1) index = getInventoryIndex(DIAMOND_AMULET);
		return index;
	}

	private int equipInventoryWeapon() {
		int weapon = getInventoryWeapon();
		if (weapon != -1 && !isItemEquipped(weapon)) {
			wearItem(weapon);
			return random(800, 1000);
		}
		return 0;
	}

	private int equipInventoryAmulet() {
		int amulet = getInventoryAmulet();
		if (amulet != -1 && !isItemEquipped(amulet)) {
			wearItem(amulet);
			return random(800, 1000);
		}
		return 0;
	}

	private int getInventoryLimit(int id) {
		if (isItemStackableId(id) && hasInventoryItem(id)) return (MAX_INV_SIZE + 1);
		return MAX_INV_SIZE;
	}

	private int returnToBank() {
		return 0;
	}

	private int pickUpLoot() {
		int item_count = getGroundItemCount();
		int max_dist = 2;
		int[] loot_item = new int[]{
			-1, -1, -1
		};
		for (int i = 0; i < item_count; i++) {
			int x = getItemX(i);
			int y = getItemY(i);
			if (!isReachable(x, y)) continue;
			int id = getGroundItemId(i);
			// if (inArray(use_items, id) && !canUseItem(id)) continue;
			if (inArray(items, id) || inArray(use_items, id) && canUseItem(id) || getItemPrice(id) >= getRareValue()) {
				int dist = distanceTo(x, y);
				if ((dist < max_dist || (dist <= max_dist && getItemPrice(id) > getItemPrice(loot_item[0]))) && (getInventoryCount() < getInventoryLimit(id) || canMakeSpace(id))) {
					loot_item[0] = id;
					loot_item[1] = x;
					loot_item[2] = y;
					max_dist = dist;
				}
			}
		}
		if (loot_item[0] != -1) {
			int id = loot_item[0];
			if (getInventoryCount() < getInventoryLimit(id) || (isItemStackableId(id) && hasInventoryItem(id))) {
				pickupItem(loot_item[0], loot_item[1], loot_item[2]);
				return random(800, 1000);
			}
			if (makeInvSpace(id)) return random(sleep_min, sleep_max);
		}
		return 0;
	}

	private int moveToLoot() {
		int item_count = getGroundItemCount();
		int max_dist = 8;
		int[] walk_to = new int[]{
			-1, -1
		};
		for (int i = 0; i < item_count; i++) {
			int id = getGroundItemId(i);
			int x = getItemX(i);
			int y = getItemY(i);
			if (inValidArea(x, y)) {
				if (getItemPrice(id) >= getRareValue() && (getInventoryCount() < Config.MAX_ITEM_AMOUNT || hasInventoryItem(food_id))) {
					walk_to[0] = x;
					walk_to[1] = y;
					break;
				}
				// if (inArray(use_items, id) && !canUseItem(id)) continue;
				if (inArray(items, id) || inArray(use_items, id) && canUseItem(id)) {
					if (getInventoryCount() < getInventoryLimit(id) || isItemStackableId(id) && hasInventoryItem(id)) {
						int dist = distanceTo(x, y);
						if (dist < max_dist) {
							walk_to[0] = x;
							walk_to[1] = y;
							if (dist == 0) break;
							max_dist = dist;
							continue;
						}
					}
				}
			}
		}
		if (walk_to[0] != -1) {
			checkIfStuckNpc();
			walkTo(walk_to[0], walk_to[1]);
			return random(800, 1000);
		}
		return 0;
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
				if (dist < max_dist && !isReachablePlayerAt(x, y, 0) && inValidArea(x, y)) {
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
			if (dist < max_dist && !isReachablePlayerAt(x, y, 0) && inValidArea(x, y)) {
				if (getNpcId(i) == Config.OTHERWORLDLY_BEING) {
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
		int[] npc = getNearestValidNpcId(Config.OTHERWORLDLY_BEING);
		if (npc[0] != -1) return npc;    //NPC pos already validated

		return new int[]{-1, -1, -1};
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

	private int getItemPrice(int id) {
		if (item_prices.containsKey(id)) {
			return item_prices.get(id);
		} else {
			return getItemBasePriceId(id);
		}
	}

	private int moveToOldestSpawn() {    //replaces moveToHomePos()
		if (!isAtCoords(home_pos.x, home_pos.y)) {
			checkIfStuckNpc();
			walkTo(home_pos.x, home_pos.y);
			return random(600, 800);
		}
		resetLastPosition();
		return 0;
	}

	private int getRareValue() {
		if (rare_value > 0) return rare_value;
		return Config.RARE_MIN_VALUE;
	}

	public int getNewFightMode() {
		if (getLevel(2) < 99) return 1;    //If strength less than 99, use aggressive fight mode
		if (getLevel(0) < 99) return 2;    //If attack less than 99, use accurate fight mode
		if (getLevel(1) < 99) return 3;    //If defense less than 99, use defensive fight mode
		return 2;    //Default to accurate if strength/attack/defense are 99
	}

	public int stopNow(String reason, boolean autologin) {
		System.out.println(reason);
		if (!autologin) {
			setAutoLogin(autologin);
			logout();
		}
		stopScript();
		return 0;
	}

	private int getAnyNpcIndexAt(int x, int y, int dist) {
		int count = countNpcs();
		for (int i = 0; i < count; i++) {
			int p_x = getNpcX(i);
			int p_y = getNpcY(i);
			if (isReachable(p_x, p_y) && distanceTo(p_x, p_y, x, y) <= dist) return i;
		}
		return -1;
	}

	private int getNpcIdIndexAt(int id, int x, int y, int dist) {
		int count = countNpcs();
		for (int i = 0; i < count; i++) {
			if (getNpcId(i) == id) {
				int p_x = getNpcX(i);
				int p_y = getNpcY(i);
				if (isReachable(p_x, p_y) && distanceTo(p_x, p_y, x, y) <= dist) return i;
			}
		}
		return -1;
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

	private int use_super_potions() {
		if ((getCurrentLevel(2) < getLevel(2) + 5)) {                //Strength
			boolean used_potion = useItemList(SUPER_STRENGTH_POTIONS);
			if (used_potion) return random(sleep_min, sleep_max);
		}
		if ((getCurrentLevel(0) < getLevel(0) + 5)) {                //Attack
			boolean used_potion = useItemList(SUPER_ATTACK_POTIONS);
			if (used_potion) return random(sleep_min, sleep_max);
		}
		if ((getCurrentLevel(1) < getLevel(1) + 5)) {                //Defense
			boolean used_potion = useItemList(SUPER_DEFENSE_POTIONS);
			if (used_potion) return random(sleep_min, sleep_max);
		}
		return 0;
	}

	private int use_strength_potion() {
		if ((getCurrentLevel(2) < getLevel(2) + 5)) {                //Strength
			boolean used_potion = useItemList(STRENGTH_POTIONS);
			if (used_potion) return random(sleep_min, sleep_max);
		}
		return 0;
	}

	private int use_prayer_potion() {
		if (getCurrentLevel(5) <= 5) {
			boolean used_potion = useItemList(PRAYER_POTIONS);
			if (used_potion) return random(sleep_min, sleep_max);
		}
		return 0;
	}

	private int dropTrashItems() {
		if (!isNpcAt(Config.OTHERWORLDLY_BEING, getX(), getY())) {
			int index = getInventoryIndex(drop_items);
			if (index != -1) {
				if (System.currentTimeMillis() >= (item_time + (last_item_id == getInventoryId(index) ? 1000L : 800L))) {
					last_item_id = getInventoryId(index);
					dropItem(index);
					item_time = System.currentTimeMillis();
				}
				return random(sleep_min, sleep_max);
			}
		}
		return 0;
	}

	private int checkUsableItems() {
		int count = getInventoryCount();
		// for (int i = 0; i < count; i++) {
		for (int i = count - 1; i >= 0; i--) {
			int id = getInventoryId(i);
			if (id == S_DEFENSE_POTION_1D) {
				useInvItem(i);
				return random(sleep_min, sleep_max);
			}
			if (getFatigue() < 100 && canUseItem(id) && !isNpcAt(Config.OTHERWORLDLY_BEING, getX(), getY())) {    //Check fatigue before using items that increase fatigue
				useInvItem(i);
				return random(sleep_min, sleep_max);
			}
		}
		return 0;
	}

	private int makeCrystalKey() {
		int count = getInventoryCount();
		for (int i = 0; i < count; i++) {
			if (getInventoryId(i) == KEY_HALF1) {
				int index = getInventoryIndex(KEY_HALF2);
				if (index != -1) {
					useItemWithItem(i, index);
					return random(sleep_min, sleep_max);
				}
			}
		}
		return 0;
	}

	private int handleBanking() {
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
				depositItem(items[i], item_count);
				if (getItemPrice(items[i]) >= getRareValue() || getItemPrice(items[i]) >= getRareValue()) {
					System.out.format("Deposited %d %s\n", item_count, getItemNameId(items[i]));
					if (items[i] == DSQ_HALF) reportShieldHalf();
				}
				return random(600, 800);
			}
		}
		//Deposit other items not specified in loot table, as long as they're not "important" items
		if (!extra_items_banked) {
			int count = getInventoryCount();
			for (int i = 0; i < count; i++) {
				int id = getInventoryId(i);
				int item_count = getInventoryCount(id);
				if (isItemEquipped(i)) {
					if (item_count == 1) continue;
					item_count--;
				}
				if (id == food_id || inArray(req_items, id) || inArray(items, id)) continue;
				if (canUseItem(id) || inArray(drop_items, id)) {
					closeBank();    //Try to let script use or drop items before attempting to bank again
					return random(800, 1000);
				}
				depositItem(id, item_count);
				System.out.format("Deposited %d %s\n", item_count, getItemNameId(id));
				return random(1000, 1200);
			}
			extra_items_banked = true;
		}
		if (stop_next_bank) {
			stop_next_bank = false;
			stop_flag = false;
			Arrays.fill(drops_banked, false);        //Items will be checked next time script is started
			Arrays.fill(req_items_checked, false);
			extra_items_banked = false;
			if (logout_flag) {
				logout_flag = false;
				setAutoLogin(false);
				logout();
			}
			return stopNow("Stop after banking was enabled - stopping script now.", true);
		}

		//Check if we have the items required for the script to continue.
		for (int i = 0; i < req_items.length; i++) {
			if (req_items_checked[i]) continue;
			req_items_checked[i] = true;
			if (!hasInventoryItem(req_items[i])) {
				if (bankCount(req_items[i]) > 0) {
					withdrawItem(req_items[i], 1);
					return random(600, 800);
				} else {
					return stopNow("No " + getItemNameId(req_items[i]) + " in bank, stopping", false);
				}
			}
		}

		//withdraw food
		int withdraw_food = food_amount - getInventoryCount(food_id);
		if (withdraw_food > getEmptySlots()) {
			withdraw_food = getEmptySlots();
		}
		if (withdraw_food > 0) {
			if (bankCount(food_id) >= food_amount) {
				withdrawItem(food_id, withdraw_food);
				return random(1000, 1500);
			} else {
				return stopNow("No more food in bank, stopping", false);
			}
		} else if (withdraw_food < 0) {
			depositItem(food_id, getInventoryCount(food_id) - food_amount);
			return random(1000, 1500);
		}

		if (isDamageAtLeast(getFoodHealAmount(food_id) - 2)) {
			restocking = true;
			closeBank();                //Close bank to allow topping off health
			return random(800, 1000);
		}

		restocking = false;
		stop_flag = false;
		extra_items_banked = false;
		withdraw_prayer = true;
		withdraw_strength = true;

		Arrays.fill(drops_banked, false);
		Arrays.fill(req_items_checked, false);
		super_checked = 0;

		System.out.println("Profits from trip " + times_banked + ": " + String.format("%,d", trip_profits) + " (Total: " + String.format("%,d", total_profits) + ")");
		trip_profits = 0;
		times_banked++;

		closeBank();
		return random(600, 800);
	}

	private void updateHomePos() {
		long oldest_age = Long.MAX_VALUE;
		int oldest_spawn = -1;
		for (int i = 0; i < last_seen_npc.length; i++) {
			if (isFriendAt(spawn_positions[i].x, spawn_positions[i].y)) continue;
			long compared_to = last_seen_npc[i];
			if (compared_to < oldest_age) {
				oldest_age = compared_to;
				oldest_spawn = i;
				home_pos = spawn_positions[i];
			}
		}
	}

	private void checkSpawns() {
		for (int i = 0; i < spawn_positions.length; i++) {
			if (isNpcAt(Config.OTHERWORLDLY_BEING, spawn_positions[i].x, spawn_positions[i].y, 1)) {
				last_seen_npc[i] = System.currentTimeMillis();
			}
		}
		updateHomePos();
	}

	private void checkPlayers() {
		int count = countPlayers();
		for (int i = 1; i < count; i++) {
			try {
				String name = getPlayerName(i);
				if (isFriend(name)) continue;
				if (stringInList(players_seen, name)) {
					int index = players_seen.indexOf(name);
					if (index != -1) {
						players_time.set(index, System.currentTimeMillis());
					}
				} else {
					players_seen.add(name);
					players_time.add(System.currentTimeMillis());
				}
			} catch (NullPointerException e) {
			}
		}
	}

	private void checkIfStuckNpc() {
		int cur_x = getX();
		int cur_y = getY();
		if (!isWalking() && isAnyNpcAt(cur_x, cur_y, 1) && cur_x == last_pos.x && cur_y == last_pos.y) {
			if ((System.currentTimeMillis() - last_stuck_check_passed) > Config.MOVE_TIMEOUT) {
				move_time = System.currentTimeMillis() + random(1500, 1800);
			}
		} else {
			last_pos.setLocation(getX(), getY());
			last_stuck_check_passed = System.currentTimeMillis();
		}
	}

	private void resetLastPosition() {
		last_pos.setLocation(getX(), getY());
		move_time = -1L;
		last_stuck_check_passed = System.currentTimeMillis();
	}

	private void reportShieldHalf() {
		if (!foundShieldHalf) {
			foundShieldHalf = true;
			System.out.println("\n*** Found dragon shield half! ***\n");
			Thread t = new Thread(new Runnable() {    //Method that doesn't freeze script and make you lose a DSQ
				public void run() {
					int input = JOptionPane.showOptionDialog(null, getPlayerName(0) + " found dragon shield half!", "Rare drop!", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null, null);
					if (input == JOptionPane.OK_OPTION) {
						foundShieldHalf = false;
					}
				}
			});
			t.start();
		}
	}

	private void move_around_npc() {
		if (isWalking()) return;
		if (attacked_blocking_npc()) return;

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
					return;
				}
				if (npc_y == p_y) {
					if (isReachable(p_x, p_y + dir)) {
						walkTo(p_x, p_y + dir);
					} else if (isReachable(p_x, p_y + dir * -1)) {
						walkTo(p_x, p_y + dir * -1);
					} else {
						walk_approx(p_x, p_y);
					}
					return;
				}
			}
		}
		resetLastPosition();
	}

	private boolean attacked_blocking_npc() {
		int[] npc = new int[]{-1, -1, -1};
		int count = countNpcs();
		int max_dist = 2;
		for (int i = 0; i < count; i++) {
			if (isNpcInCombat(i)) continue;
			int x = getNpcX(i);
			int y = getNpcY(i);
			int dist = distanceTo(x, y, getX(), getY());
			if (dist < max_dist) {
				npc[0] = i;
				npc[1] = x;
				npc[2] = y;
				if (dist == 0) break;
				max_dist = dist;
			}
		}
		if (npc[0] != -1) {
			attackNpc(npc[0]);
			return true;
		}
		return false;
	}

	private boolean isAtCoords(int x, int y) {
		return (getX() == x && getY() == y);
	}

	private boolean isReadyToFarm() {
		for (int i = 0; i < req_items.length; i++) {
			int id = getInventoryId(i);
			if (!hasInventoryItem(id)) {
				// System.out.println("Missing " + getItemNameId(id));
				return false;
			}
		}
		// System.out.println("Missing " + getItemNameId(food_id));
		return (getInventoryCount(food_id) >= food_amount) || (getEmptySlots() <= 0);
	}

	private boolean attackNearestEnemy() {
		// int[] npc = getNearestEnemy();
		int[] npc = getNearestValidNpcId(Config.OTHERWORLDLY_BEING);
		if (npc[0] != -1) {
			checkIfStuckNpc();
			if (distanceTo(npc[1], npc[2]) > 5) {
				walkTo(npc[1], npc[2]);
			} else {
				attackNpc(npc[0]);
			}
			return true;
		}
		return false;
	}

	private boolean attackNpcIdInArea(int id, int x_min, int y_min, int x_max, int y_max) {
		int[] npc = new int[]{-1, -1, -1};
		int count = countNpcs();
		int max_dist = Integer.MAX_VALUE;
		for (int i = 0; i < count; i++) {
			int p_x = getNpcX(i);
			int p_y = getNpcY(i);
			if (getNpcId(i) == id) {
				if (isNpcInCombat(i)) continue;
				int x = getNpcX(i);
				int y = getNpcY(i);
				int dist = distanceTo(x, y, getX(), getY());
				if (dist < max_dist && !isReachablePlayerAt(x, y, 0) && inValidArea(x, y)) {
					npc[0] = i;
					npc[1] = x;
					npc[2] = y;
					if (dist == 0) break;
					max_dist = dist;
				}
			}
		}
		if (npc[0] != -1) {
			attackNpc(npc[0]);
			return true;
		}
		return false;
	}

	private boolean attackAdjacentEnemy() {
		if (needToPickup() || hasHealthReached(Config.HEAL_AT)) return false;
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
		int p_x = getX();
		int p_y = getY();
		if (isWithinBounds(p_x, p_y, area_min.x, area_min.y, area_max.x, area_max.y))
			return isWithinBounds(x, y, area_min.x, area_min.y, area_max.x, area_max.y);
		return false;
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

	private boolean isPlayerInCombatAt(int x, int y, int dist) {    //Checks for in-combat players within range
		// if (isWalking()) return false;
		int count = countPlayers();
		for (int i = 1; i < count; i++) {
			if (isPlayerInCombat(i)) {
				int p_x = getPlayerX(i);
				int p_y = getPlayerY(i);
				if (isReachable(p_x, p_y) && distanceTo(p_x, p_y, x, y) <= dist) return true;
			}
		}
		return false;
	}

	private boolean isReachablePlayerAt(int x, int y, int dist) {
		int count = countPlayers();
		for (int i = 1; i < count; i++) {
			int p_x = getPlayerX(i);
			int p_y = getPlayerY(i);
			if (isReachable(p_x, p_y) && distanceTo(x, y, p_x, p_y) <= dist) return true;
		}
		return false;
	}

	private boolean isPlayerStandingAt(int x, int y, int dist) {    //Same as isReachablePlayerAt, but only for non-moving players
		int count = countPlayers();
		for (int i = 1; i < count; i++) {
			if (isPlayerWalking(i)) continue;
			int p_x = getPlayerX(i);
			int p_y = getPlayerY(i);
			if (isReachable(p_x, p_y) && distanceTo(x, y, p_x, p_y) <= dist) return true;
		}
		return false;
	}

	private boolean makeInvSpace(int id) {
		if (inArray(use_items, id) && getEmptySlots() == 0 && getInventoryCount(food_id) == 1 && isDamageAtLeast(getFoodHealAmount(food_id)))
			return false;
		int index = getInventoryIndex(use_items);
		if (index != -1) {
			if (getFatigue() < 100 || getItemPrice(id) >= getRareValue()) {
				useInvItem(index);
				return true;
			} else {
				index = getInventoryIndex(S_DEFENSE_POTION_1D);
				if (index != -1) {
					useInvItem(index);
					return true;
				}
			}
		}
		index = getInventoryIndex(food_id);    //Check if we can eat food to clear up inventory space, but only if we have low enough HP to not waste it.
		if (index == -1) return false;
		if
		(isDamageAtLeast(getFoodHealAmount(food_id)) ||
			getItemPrice(id) >= getRareValue() ||
			id == DSQ_HALF) {
			useInvItem(index);    //Dont return value returned by useInvItem - don't want to accidentally proceed to attack an enemy if we haven't actually picked up the item yet.
			return true;
		}
		return false;
	}

	private boolean canMakeSpace(int id) {        //Checks if makeInvSpace is possible, doesn't perform any actions
		if (isItemStackableId(id) && hasInventoryItem(id)) return true;
		if (inArray(use_items, id) && getEmptySlots() == 0 && getInventoryCount(food_id) == 1 && isDamageAtLeast(getFoodHealAmount(food_id)))
			return false;
		int index = getInventoryIndex(use_items);
		if (index != -1) {
			if (getFatigue() < 100 || getItemPrice(id) >= getRareValue()) {
				return true;
			} else {
				index = getInventoryIndex(S_DEFENSE_POTION_1D);
				if (index != -1) {
					return true;
				}
			}
		}
		index = getInventoryIndex(food_id);    //Check if we can eat food to clear up inventory space, but only if we have low enough HP to not waste it. (Assuming food = shark).
		if (index == -1) return false;
		return isDamageAtLeast(getFoodHealAmount(food_id)) ||
			getItemPrice(id) >= getRareValue() ||
			id == DSQ_HALF;
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

	private boolean isNpcAt(int id, int x, int y, int dist) {
		int count = countNpcs();
		for (int i = 0; i < count; i++) {
			int p_id = getNpcId(i);
			if (p_id == id) {
				int p_x = getNpcX(i);
				int p_y = getNpcY(i);
				if (distanceTo(x, y, p_x, p_y) <= dist) return true;
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

	private boolean isNpcNotCombatAt(int id, int x, int y, int dist) {
		int count = countNpcs();
		for (int i = 0; i < count; i++) {
			int p_id = getNpcId(i);
			if (p_id == id && !isNpcInCombat(i)) {
				int p_x = getNpcX(i);
				int p_y = getNpcY(i);
				if (distanceTo(x, y, p_x, p_y) <= dist) return true;
			}
		}
		return false;
	}

	private boolean isFriendAt(int x, int y) {
		int count = countPlayers();
		for (int i = 1; i < count; i++) {
			try {
				String name = getPlayerName(i);
				if (isFriend(name)) {
					if (!isPlayerWalking(i) && x == getPlayerX(i) && y == getPlayerY(i)) {
						return true;
					}
				}
			} catch (NullPointerException e) {
				return false;
			}
		}
		return false;
	}

	private boolean canContinue() {
		if (hasHealthReached(Config.CRITICAL_HP) || stop_flag) return false;
		if ((getCurrentLevel(3) > Config.ABORT_AT) && (needToPickup() || (isNpcAt(Config.OTHERWORLDLY_BEING, getX(), getY()) && getEmptySlots() > 0)))
			return true;
		if (restocking) return false;
		if (!hasInventoryItem(food_id)) return false;
		if (getEmptySlots() == 0) {
			return (isDamageAtLeast(getFoodHealAmount(food_id)) && getInventoryCount(food_id) > 0) || getInventoryIndex(use_items) != -1 || getInventoryIndex(drop_items) != -1;
		}
		return true;
	}

	private boolean needToPickup() {
		int item_count = getGroundItemCount();
		for (int i = 0; i < item_count; i++) {
			int id = getGroundItemId(i);
			if (getItemX(i) == getX() && getItemY(i) == getY() || getItemPrice(id) >= getRareValue() && inValidArea(getItemX(i), getItemY(i))) {
				// if (inArray(use_items, id) && !canUseItem(id)) continue;
				if (getItemPrice(id) >= getRareValue() && (getInventoryCount() < MAX_INV_SIZE || hasInventoryItem(food_id)))
					return true;
				if (inArray(items, id)) {
					if (getInventoryCount() < Config.MAX_ITEM_AMOUNT || isItemStackableId(id) && hasInventoryItem(id)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isPlayerApproaching() { //Check if some idiot crasher's bot is outside the fire giant doors
		int count = countPlayers();
		for (int i = 1; i < count; i++) {
			int x = getPlayerX(i);
			if (x == 659 || x == 660) {
				int y = getPlayerY(i);
				if (y >= 3289) return true;
			}
		}
		return false;
	}

	private boolean useInvItem(int index) {
		if (index != -1) {
			// if (System.currentTimeMillis() >= (item_time + 1500L)) {
			if (System.currentTimeMillis() >= (item_time + (last_item_id == getInventoryId(index) ? 2000L : 1500L))) {
				last_item_id = getInventoryId(index);
				useItem(index);
				item_time = System.currentTimeMillis();
				return true;
			}
		}
		return false;
	}

	private boolean useItemList(int... itemList) {
		for (int i = 0; i < itemList.length; i++) {
			int index = getInventoryIndex(itemList[i]);
			if (index != -1) {
				useInvItem(index);
				return true;
			}
		}
		return false;
	}

	private boolean withdrawPrioritizedItem(int... itemList) {
		if (getEmptySlots() == 0) return false;
		for (int i = itemList.length - 1; i >= 0; i--) {        //Note reversed priority
			int count = bankCount(itemList[i]);
			if (count > 1) {                                //Do not withdraw last item
				withdrawItem(itemList[i], 1);
				return true;
			}
		}
		return false;
	}

	private boolean inFarmingArea() {
		return isWithinBounds(getX(), getY(), area_min.x, area_min.y, area_max.x, area_max.y);
	}

	private boolean inBank() {
		return (
			isWithinBounds(getX(), getY(), bank_min0.x, bank_min0.y, bank_max0.x, bank_max0.y) ||
				isWithinBounds(getX(), getY(), bank_min1.x, bank_min1.y, bank_max1.x, bank_max1.y) ||
				isWithinBounds(getX(), getY(), bank_min2.x, bank_min2.y, bank_max2.x, bank_max2.y)
		);
	}

	private boolean canUseItem(int id) {    //Intended to prevent unid herbs from being used if herb level is not high enough
		// if (getEmptySlots() == 0 && getInventoryCount(food_id) == 1 && isDamageAtLeast(getFoodHealAmount(food_id))) return false;
		switch (id) {
			case S_DEFENSE_POTION_1D:
				// return (cb_defense.getState() && getCurrentLevel(1) < (getLevel(1) + Config.SDP_MIN_BOOST));
				return false;
			case NORMAL_BONES:
				return (cb_bury.getState() && getLevel(5) < 99);
			case UNID_GUAM:
				return (cb_identify.getState() && inArray(use_items, id) && getLevel(15) >= 3 && getLevel(15) < 99);
			case UNID_MARRENTILL:
				return (cb_identify.getState() && inArray(use_items, id) && getLevel(15) >= 5 && getLevel(15) < 99);
			case UNID_TARROMIN:
				return (cb_identify.getState() && inArray(use_items, id) && getLevel(15) >= 11 && getLevel(15) < 99);
			case UNID_HARRALANDER:
				return (cb_identify.getState() && inArray(use_items, id) && getLevel(15) >= 20 && getLevel(15) < 99);
			case UNID_RANARR:
				// return (cb_identify.getState() && getLevel(15) >= 25 && getLevel(15) < 99);
			case UNID_IRIT:
				// return (cb_identify.getState() && getLevel(15) >= 40 && getLevel(15) < 99);
			case UNID_AVANTOE:
				// return (cb_identify.getState() && getLevel(15) >= 48 && getLevel(15) < 99);
			case UNID_KWUARM:
				// return (cb_identify.getState() && getLevel(15) >= 54 && getLevel(15) < 99);
			case UNID_CADANTINE:
				// return (cb_identify.getState() && getLevel(15) >= 65 && getLevel(15) < 99);
			case UNID_DWARFWEED:
				// return (cb_identify.getState() && getLevel(15) >= 70 && getLevel(15) < 99);
				return false;    //Don't ID high level herbs
		}
		return false;
	}

	public boolean inList(List<Integer> haystack, int needle) {
		for (final int element : haystack) {
			if (element == needle) {
				return true;
			}
		}
		return false;
	}

	public int getListIndex(List<Integer> haystack, int needle) {
		for (int i = 0; i < haystack.size(); i++) {
			Integer element = haystack.get(i);
			if (element == needle) {
				return i;
			}
		}
		return -1;
	}

	private boolean isNpcIdInArea(int id, int x_min, int y_min, int x_max, int y_max) {
		int count = countNpcs();
		for (int i = 0; i < count; i++) {
			int x = getNpcX(i);
			int y = getNpcY(i);
			if (getNpcId(i) == id && !isReachablePlayerAt(x, y, 0) && isWithinBounds(x, y, x_min, y_min, x_max, y_max))
				return true;
		}
		return false;
	}

	private boolean isRareItemInArea(int x_min, int y_min, int x_max, int y_max) {
		int item_count = getGroundItemCount();
		for (int i = 0; i < item_count; i++) {
			int id = getGroundItemId(i);
			if (getItemPrice(id) >= getRareValue() && isWithinBounds(getItemX(i), getItemY(i), x_min, y_min, x_max, y_max)) {
				if ((getInventoryCount() < MAX_INV_SIZE || hasInventoryItem(food_id))) return true;
			}
		}
		return false;
	}

	private boolean hasHealthReached(int hits) {
		return (getCurrentLevel(3) <= hits);
	}

	private boolean isDamageAtLeast(int damage) {
		return (getCurrentLevel(3) <= (getLevel(3) - damage));
	}

	private boolean isTelePoint(int x, int y) {
		for (int i = 0; i < tele_whitelist.length; i++) {
			if (isAtApproxCoords(tele_whitelist[i].x, tele_whitelist[i].y, 10)) return true;
		}
		return false;
	}

	public static boolean stringInList(List<String> haystack, String needle) {
		return haystack.contains(needle);
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

	private String getTimeSince(long initial_time) {
		if (initial_time == -1L) return "N/A";
		long millis = (System.currentTimeMillis() - initial_time) / 1000;
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

		drawString("Just_OtherworldlyBeings", x - 4, y - 17, 4, green);
		y += 15;
		drawString("Runtime: " + getRunTime(), x, y, 1, yellow);
		if (sleep_fatigue < 100) {
			y += 15;
			drawString("Combat EXP rate: " + (int) ((getAccurateXpForLevel(2) + getAccurateXpForLevel(1) + getAccurateXpForLevel(0) + getAccurateXpForLevel(3) - startxp) * 60.0D * 60.0D / (double) var1) + "/h", x, y, 1, yellow);
			y += 15;
			drawString("Prayer XP rate: " + (int) ((getAccurateXpForLevel(5) - startprayerxp) * 60.0D * 60.0D / (double) var1) + "/h", x, y, 1, yellow);
		} else {
			y += 15;
			drawString("No-sleep mode enabled", x, y, 1, green);
		}
		y += 15;
		drawString("Times banked: " + times_banked, x, y, 1, yellow);
		if (stop_next_bank) {
			y += 15;
			drawString("Stopping on next banking", x, y, 1, red);
		}
		x = getGameWidth() - x_offset;
		y = 50;
		if (paint_mode == 0) {
			drawString("Loot banked: ", x, y, 1, yellow);
			y += 15;
			for (int i = 0; i < items.length; ++i) {
				if (drops_count[i] <= 0) continue;
				drawString(getItemNameId(items[i]) + ": " + ifmt(drops_count[i]), x, y, 1, getItemPrice(items[i]) < getRareValue() ? white : green);
				y += 15;
			}
			for (int i = 0; i < unlisted_items.size(); ++i) {
				if (unlisted_amount.get(i) <= 0) continue;
				drawString(getItemNameId(unlisted_items.get(i)) + ": " + ifmt(unlisted_amount.get(i)), x, y, 1, getItemPrice(unlisted_items.get(i)) < getRareValue() ? white : green);
				y += 15;
			}
		} else if (paint_mode == 1) {
			drawString("Items withdrawn: ", x, y, 1, yellow);
			y += 15;
			for (int i = 0; i < items.length; ++i) {
				if (drops_count[i] >= 0) continue;
				drawString(getItemNameId(items[i]) + ": " + ifmt(-drops_count[i]), x, y, 1, white);
				y += 15;
			}
			for (int i = 0; i < unlisted_items.size(); ++i) {
				if (unlisted_amount.get(i) >= 0) continue;
				drawString(getItemNameId(unlisted_items.get(i)) + ": " + ifmt(-unlisted_amount.get(i)), x, y, 1, white);
				y += 15;
			}
		} else if (paint_mode == 2) {
			drawString("Levels gained: ", x, y, 1, yellow);
			y += 15;
			for (int i = 0; i < skill_levels.length; i++) {
				int gain = getLevel(i) - skill_levels[i];
				if (gain == 0) continue;
				drawString(String.format("%s: %d", SKILL[i], gain), x, y, 1, white);
				y += 15;
			}
			y += 5;
			drawString("Total profits: " + String.format("%,d", total_profits), x, y, 1, yellow);
			y += 15;
			drawString("Alchemy income: " + String.format("%,d", alch_income_total), x, y, 1, yellow);
			y += 15;
			drawString("Disconnects: " + disconnect_count, x, y, 1, yellow);
			y += 15;
			drawString("Last disconnect: " + getTimeSince(disconnect_time), x, y, 1, yellow);
		} else if (paint_mode == 3) {
			drawString("Players seen: ", x, y, 1, yellow);
			y += 15;
			for (int i = 0; i < players_seen.size(); ++i) {
				drawString(String.format("%s (%s)", players_seen.get(i), getTimeHours(players_time.get(i))),
					x, y, 1, white);
				y += 15;
			}
		} else if (paint_mode < 0) {
			paint_mode = 3;
		} else {
			paint_mode = 0;
		}
	}

	private String getTimeHours(long time) {    //Hours
		return String.format("%.2f hours", ((System.currentTimeMillis() - time) / (double) 3600000));
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
			System.out.println(str);
			if (ch_fightmode.getSelectedIndex() == 4 && (str.contains("strength") || str.contains("attack") || str.contains("defence"))) {
				update_fmode = true;
			}
		} else if (!alchemy_text.equals("") && str.contains("alchemy")) {
			alch_income_total += alch_income_queue;
			System.out.println(alchemy_text);
			alchemy_text = "";
		} else if (str.contains("spell fails!")) {
			last_spell_fail = System.currentTimeMillis();
		} else if (str.contains("welcome")) {
			if (System.currentTimeMillis() - hop_time > 5000L) {
				disconnect_count++;
				disconnect_time = System.currentTimeMillis();
				tele_check.setLocation(getX(), getY());
			} else {
				if (fail_hop != -1) {
					fail_hop = -1;
				}
				times_hopped++;
				hop_time = System.currentTimeMillis();
			}
		} else if (str.startsWith("the dragon")) {
			restocking = true;
			stop_flag = true;
			logout_flag = true;
			System.out.println("Player was hit by dragonfire!");
		} else if (str.contains("sorry")) {

		} else if (str.contains("busy")) {
			menu_time = -1L;
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
		if (cb_frame == null) {
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

			Panel pInput = new Panel();
			pInput.setLayout(new GridLayout(0, 2, 0, 2));

			pInput.add(new Label("Fight mode"));
			pInput.add(ch_fightmode);

			pInput.add(new Label("Food type"));
			pInput.add(ch_foodtype);

			pInput.add(new Label("Food amount:"));
			pInput.add(tf_food_amount = new TextField(Integer.toString(Config.DEFAULT_FOOD_AMOUNT)));

			Panel cbPanel = new Panel();
			cbPanel.setLayout(new GridLayout(0, 1));
			cbPanel.add(cb_bury = new Checkbox("Bury bones"));
			cbPanel.add(cb_identify = new Checkbox("ID + drop low herbs"));
			// cbPanel.add(cb_alchemy = new Checkbox("Use high level alchemy"));
			// cbPanel.add(cb_prayers = new Checkbox("Use prayers"));
			// cbPanel.add(cb_supers = new Checkbox("Use super potions"));
			// cbPanel.add(cb_defense = new Checkbox("Pickup + use super defense potions"));
			cbPanel.add(cb_nosleep = new Checkbox("No sleeping"));

			//Disables
			/*
			cb_bury.setEnabled(false);
			cb_alchemy.setEnabled(false);
			cb_prayers.setEnabled(false);
			cb_supers.setEnabled(false);
			cb_defense.setEnabled(false);
			*/

			Panel buttonPanel = new Panel();
			Button ok = new Button("OK");
			ok.addActionListener(this);
			buttonPanel.add(ok);
			Button cancel = new Button("Cancel");
			cancel.addActionListener(this);
			buttonPanel.add(cancel);

			cb_frame = new Frame(getClass().getSimpleName());
			cb_frame.setIconImages(Constants.ICONS);
			cb_frame.addWindowListener(
				new StandardCloseHandler(cb_frame, StandardCloseHandler.HIDE)
			);
			cb_frame.add(pInput, BorderLayout.NORTH);
			cb_frame.add(cbPanel, BorderLayout.CENTER);
			cb_frame.add(buttonPanel, BorderLayout.SOUTH);
			cb_frame.setResizable(false);
			cb_frame.pack();
		}
		cb_frame.setLocationRelativeTo(null);
		cb_frame.toFront();
		cb_frame.requestFocus();
		cb_frame.setVisible(true);
	}

	private void depositItem(int id, int amount) {
		deposit(id, amount);
		update_profits(amount * getItemPrice(id));
		if (inArray(items, id)) {
			int index = getArrayIndex(items, id);
			if (index != -1) {
				drops_count[index] += amount;
			}
		} else {
			int index = getListIndex(unlisted_items, id);
			if (index != -1) {
				unlisted_amount.set(index, (unlisted_amount.get(index) + amount));
			} else {
				unlisted_items.add(id);
				unlisted_amount.add(amount);
				// System.out.println("Deposited " + amount + " " + getItemNameId(id) + "(s).");
			}
		}
	}

	private void withdrawItem(int id, int amount) {
		withdraw(id, amount);
		update_profits(-amount * getItemPrice(id));
		if (inArray(items, id)) {
			int index = getArrayIndex(items, id);
			if (index != -1) {
				drops_count[index] -= amount;
			}
		} else {
			int index = getListIndex(unlisted_items, id);
			if (index != -1) {
				unlisted_amount.set(index, (unlisted_amount.get(index) - amount));
			} else {
				unlisted_items.add(id);
				unlisted_amount.add(-amount);
				// System.out.println("Withdrew " + amount + " " + getItemNameId(id) + "(s).");
			}
		}
	}

	private void useSleepingBag2() {
		if (hasInventoryItem(SLEEPING_BAG)) {
			useSleepingBag();
			resetLastPosition();
		} else {
			sleep_fatigue = 101;
			System.out.println("No sleeping bag in inventory. Sleeping disabled");
		}
	}

	private void teleportCheck() {
		if (tele_check.x != -1 && distanceTo(getX(), getY(), tele_check.x, tele_check.y) > 10) {
			if (!isTelePoint(getX(), getY()) && !inBank()) {
				System.out.println("Player was teleported from " + tele_check.toString() + " to " + getX() + ", " + getY());
				if (canLogout()) {
					tele_check.setLocation(getX(), getY());
					System.out.println("Stopping script and logging out.");
					setAutoLogin(false);
					logout();
					stopScript();
				}
				return;
			}
		}
		tele_check.setLocation(getX(), getY());
	}

	public void setNewFightMode(int fight_mode) {
		Just_OtherworldlyBeings.fight_mode = fight_mode;
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
			try {
				food_amount = Integer.parseInt(tf_food_amount.getText().trim());//Get food amount
			} catch (Throwable t) {
				System.out.println("Couldn't parse food amount");
			}
			if (cb_nosleep.getState()) {
				sleep_fatigue = 101;
			}
			System.out.println("Bury bones: " + cb_bury.getState());
			// System.out.println("Pickup + use super defense potions: " + cb_defense.getState());
			System.out.println("ID + drop low herbs: " + cb_identify.getState());
			System.out.println("No sleeping: " + cb_nosleep.getState());
		}
		cb_frame.setVisible(false);
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
				} else if (command.equals("stop") || command.equals("stopnext")) {    //Stop next banking
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
				} else if (command.equals("stopnow")) {
					restocking = true;
					stop_next_bank = true;
					stop_flag = true;
					System.out.println("Immediate stop flag set");
				} else if (command.equals("emergency")) {
					restocking = true;
					stop_next_bank = true;
					logout_flag = true;
					stop_flag = true;
				} else if (command.equals("rarevalue")) {
					try {
						rare_value = Integer.parseInt(JOptionPane.showInputDialog(null, "Enter new rare value: ", rare_value));
					} catch (NumberFormatException e) {
						rare_value = -1;
					}
				} else if (command.equals("nosleep")) {
					if (sleep_fatigue == Config.SLEEP_AT) {
						sleep_fatigue = 101;
						System.out.println("Sleeping disabled");
					} else {
						sleep_fatigue = Config.SLEEP_AT;
						System.out.println("Sleeping re-enabled");
					}
				} else if (command.equals("resetstats")) {    //Reset stats
					start_time_exp = System.currentTimeMillis();
					startxp = 0;
					startprayerxp = 0;
					startxp += getAccurateXpForLevel(0);
					startxp += getAccurateXpForLevel(1);
					startxp += getAccurateXpForLevel(2);
					startxp += getAccurateXpForLevel(3);
					startprayerxp += getAccurateXpForLevel(5);
					System.out.println("Reset stats counter");
				} else if (command.equals("dsqtest")) {    //Toggle DSQ notify test
					reportShieldHalf();
				} else if (command.equals("resetplayers")) {
					players_seen = new ArrayList<>();
					players_time = new ArrayList<>();
					System.out.println("Reset player history");
				} else if (command.equals("modifysleep")) {
					int new_sleep = Integer.parseInt(JOptionPane.showInputDialog(null, "Enter new min sleep: ", sleep_min));
					sleep_min = new_sleep;
					sleep_max = new_sleep + 200;
				} else if (command.equals("modifyloot")) {    //Non functional
					String loot_string = Arrays.toString(items).replace(" ", "").replace("[", "").replace("]", "");
					String input_string = (JOptionPane.showInputDialog(null, "Modify loot list", loot_string));
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

	public static class Config {
		public static final int SLEEP_AT = 90;    //Sleep at fatigue level
		public static final int HEAL_AT = 65;    //Eat at hits level
		public static final int RUN_AT = 55;    //Run at hits level
		public static final int ABORT_AT = 40;    //HP to ignore all drops and teleport out, if no food remaining
		public static final int CRITICAL_HP = 30;    //HP to ignore all drops and teleport out, regardless of remaining food
		public static final int OTHERWORLDLY_BEING = 298;
		public static final int MOVE_TIMEOUT = 3000;
		public static final int DOOR_TIMEOUT = 2500;
		public static final int ALCHEMY_SPELL = 28;
		public static final int KEEP_NATURES = 10;    //Minimum amount of nature runes to keep in inventory for high alching
		public static final int RARE_MIN_VALUE = 25000;    //Minimum item price to be considered a rare item (check items.txt for prices)
		public static final int MAX_ITEM_AMOUNT = 29;    //1 less than MAX_INV_SIZE to free up a space for bones
		public static final int HIGH_ALCH_LEVEL = 55;    //Minimum level to cast high level alchemy
		public static final int DEFAULT_FOOD_AMOUNT = 10;
		public static final int SDP_MIN_BOOST = 15;        //Minimum defense boost until next SDP pickup
	}
}
