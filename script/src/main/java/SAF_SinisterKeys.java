import com.aposbot.Constants;
import com.aposbot.StandardCloseHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public final class SAF_SinisterKeys extends Script
	implements ActionListener {
	/**
	 * Using PathWalker
	 */
	private final PathWalker pw;
	private PathWalker.Path bank_from_north;
	private PathWalker.Path bank_from_south;
	private PathWalker.Path north_stairs;
	private PathWalker.Path south_stairs;

	private long time;
	private final int
		GUAM_LEAF_UNID = 434,
		GUAM_LEAF = 444,
		MARRENTILL_UNID = 435,
		MARRENTILL = 445,
		TARROMIN_UNID = 436,
		TARROMIN = 446,
		HARRALANDER_UNID = 437,
		HARRALANDER = 447,
		RANARR_WEED_UNID = 438,
		RANARR_WEED = 448,
		IRIT_LEAF_UNID = 439,
		IRIT_LEAF = 449,
		AVANTOE_UNID = 440,
		AVANTOE = 450,
		KWUARM_UNID = 441,
		KWUARM = 451,
		CANDANTINE_UNID = 442,
		CANDANTINE = 452,
		DWARF_WEED_UNID = 443,
		DWARF_WEED = 453,
		SNAKE_WEED_UNID = 815,
		SNAKE_WEED = 816,
		ARDRIGAL_UNID = 817,
		ARDRIGAL = 818,
		SITO_FOIL_UNID = 819,
		SITO_FOIL = 820,
		VOLENCIA_MOSS_UNID = 821,
		VOLENCIA_MOSS = 822,
		ROGUES_PURSE_UNID = 823,
		ROGUES_PURSE = 824,
		TORSTOL_UNID = 933,
		TORSTOL = 934;

	private int
		FOOD = 373;
	private int HEAL_AT = 0;
	private int KEY_COUNT = 0;
	private int GUAM_LEAF_COUNT = 0;
	private int MARRENTILL_COUNT = 0;
	private final int TARROMIN_COUNT = 0;
	private int HARRALANDER_COUNT = 0;
	private int RANARR_WEED_COUNT = 0;
	private int IRIT_LEAF_COUNT = 0;
	private int AVANTOE_COUNT = 0;
	private int KWUARM_COUNT = 0;
	private int CANDANTINE_COUNT = 0;
	private int DWARF_WEED_COUNT = 0;
	private int SNAKE_WEED_COUNT = 0;
	private int ARDRIGAL_COUNT = 0;
	private int SITO_FOIL_COUNT = 0;
	private int VOLENCIA_MOSS_COUNT = 0;
	private int ROGUES_PURSE_COUNT = 0;
	private int TORSTOL_COUNT = 0;

	private final int
		SINISTER_CHEST = 645,
		SINISTER_KEY = 932;

	private final int[]
		LOG1 = {601, 3557},
		LOG2 = {601, 3563},
		BANK = {587, 752},
		PIPE1 = {605, 3568},
		PIPE2 = {607, 3568},
		SOUTH_STAIRS1 = {590, 762},
		SOUTH_STAIRS2 = {590, 762},
		NORTH_STAIRS1 = {603, 725},
		NORTH_STAIRS2 = {603, 3553},
		FAIL_STAIRS = {605, 3509},
		LOCKED_DOOR = {593, 3590},
		CHEST = {617, 3567},
		FOOD_IDS = {373, 370, 546},
		DROP_IDS = {GUAM_LEAF_UNID, MARRENTILL_UNID, TARROMIN_UNID, HARRALANDER_UNID, RANARR_WEED_UNID,
			IRIT_LEAF_UNID, AVANTOE_UNID, KWUARM_UNID, CANDANTINE_UNID, DWARF_WEED_UNID, ARDRIGAL_UNID,
			SITO_FOIL_UNID, VOLENCIA_MOSS_UNID, ROGUES_PURSE_UNID, TORSTOL_UNID};

	private static int
		FIGHTMODE = 1;

	private Frame frame;
	private final Choice combat_choice = new Choice();
	private final Choice food_choice = new Choice();
	private final Choice entrance_choice = new Choice();

	private final String[]
		MODE_NAME = {"Strength", "Attack", "Defense"},
		FOOD_NAME = {"Lobsters", "Swordfish", "Sharks"},
		ENTRANCE_NAME = {"North Entrance", "South Entrance"};

	private boolean init = false;
	private boolean south_entrance = false;

	public SAF_SinisterKeys(Extension e) {
		super(e);
		this.pw = new PathWalker(e);
	}

	@Override
	public void init(String s) {
		pw.init(null);
		if (frame == null) {

			for (String str : MODE_NAME) {
				combat_choice.add(str);
			}

			for (String str : FOOD_NAME) {
				food_choice.add(str);
			}

			for (String str : ENTRANCE_NAME) {
				entrance_choice.add(str);
			}

			Panel col_pane = new Panel(new GridLayout(0, 2, 2, 2));
			col_pane.add(new Label("Food type:"));
			col_pane.add(food_choice);
			food_choice.select("Sharks");
			col_pane.add(new Label("Combat style:"));
			col_pane.add(combat_choice);
			combat_choice.select("Strength");

			col_pane.add(new Label("Entrance:"));
			col_pane.add(entrance_choice);
			entrance_choice.select("North Entrance");

			Panel button_pane = new Panel();
			Button button = new Button("OK");
			button.addActionListener(this);
			button_pane.add(button);
			button = new Button("Cancel");
			button.addActionListener(this);
			button_pane.add(button);

			frame = new Frame(getClass().getSimpleName());
			frame.addWindowListener(
				new StandardCloseHandler(frame, StandardCloseHandler.HIDE)
			);
			frame.setIconImages(Constants.ICONS);
			frame.setLayout(new BoxLayout(frame, BoxLayout.Y_AXIS));
			frame.add(col_pane);
			frame.add(button_pane);
			frame.pack();
			frame.setResizable(false);
		}
		frame.setLocationRelativeTo(null);
		frame.toFront();
		frame.requestFocus();
		frame.setVisible(true);
	}

	@Override
	public int main() {
		if (pw.walkPath()) {
			return 100;
		}
		if (!init) {
			init = true;
		}
		if (isBanking()) {
			for (int drop : DROP_IDS) {
				int drop_count = getInventoryCount(drop);
				if (drop_count > 0) {
					switch (drop) {
						case GUAM_LEAF_UNID:
							GUAM_LEAF_COUNT = GUAM_LEAF_COUNT + drop_count;
							break;
						case MARRENTILL_UNID:
							MARRENTILL_COUNT = MARRENTILL_COUNT + drop_count;
							break;
						case HARRALANDER_UNID:
							HARRALANDER_COUNT = HARRALANDER_COUNT + drop_count;
							break;
						case RANARR_WEED_UNID:
							RANARR_WEED_COUNT = RANARR_WEED_COUNT + drop_count;
							break;
						case IRIT_LEAF_UNID:
							IRIT_LEAF_COUNT = IRIT_LEAF_COUNT + drop_count;
							break;
						case AVANTOE_UNID:
							AVANTOE_COUNT = AVANTOE_COUNT + drop_count;
							break;
						case KWUARM_UNID:
							KWUARM_COUNT = KWUARM_COUNT + drop_count;
							break;
						case CANDANTINE_UNID:
							CANDANTINE_COUNT = CANDANTINE_COUNT + drop_count;
							break;
						case DWARF_WEED_UNID:
							DWARF_WEED_COUNT = DWARF_WEED_COUNT + drop_count;
							break;
						case SNAKE_WEED_UNID:
							SNAKE_WEED_COUNT = SNAKE_WEED_COUNT + drop_count;
							break;
						case ARDRIGAL_UNID:
							ARDRIGAL_COUNT = ARDRIGAL_COUNT + drop_count;
							break;
						case SITO_FOIL_UNID:
							SITO_FOIL_COUNT = SITO_FOIL_COUNT + drop_count;
							break;
						case VOLENCIA_MOSS_UNID:
							VOLENCIA_MOSS_COUNT = VOLENCIA_MOSS_COUNT + drop_count;
							break;
						case ROGUES_PURSE_UNID:
							ROGUES_PURSE_COUNT = ROGUES_PURSE_COUNT + drop_count;
							break;
						case TORSTOL_UNID:
							TORSTOL_COUNT = TORSTOL_COUNT + drop_count;
							break;
					}
					deposit(drop, drop_count);
					return random(1000, 1200);
				}
			}

			if (!hasBankItem(FOOD) || !hasBankItem(SINISTER_KEY)) {
				stopScript();
				return 0;
			}
			if (hasBankItem(FOOD) && getInventoryCount(FOOD) < 5) {
				withdraw(FOOD, 5 - getInventoryCount(FOOD));
				return 1000;
			}
			if (hasBankItem(SINISTER_KEY) && getInventoryCount(SINISTER_KEY) < 2) {
				KEY_COUNT = KEY_COUNT + 2;
				withdraw(SINISTER_KEY, 2 - getInventoryCount(SINISTER_KEY));
				return 1000;
			}

			closeBank();

			if (getCurrentLevel(3) < HEAL_AT) {
				if (inCombat()) {
					walkTo(getX(), getY());
					return 600;
				}
				if (hasInventoryItem(FOOD)) {
					useItem(getInventoryIndex(FOOD));
					return 400;
				}
			} else {
				if (south_entrance == true) {
					pw.setPath(south_stairs);
				} else {
					pw.setPath(north_stairs);
				}
			}
			return 1000;
		}
		/**
		 *Inside of ardy bank.
		 */

		if (getCurrentLevel(3) < HEAL_AT) {
			if (inCombat()) {
				walkTo(getX(), getY());
				return 600;
			}
			if (hasInventoryItem(FOOD)) {
				useItem(getInventoryIndex(FOOD));
				return 400;
			}
		}

		if (getFatigue() > 90) {
			useSleepingBag();
			return random(921, 1000);
		}

		if (isAtApproxCoords(BANK[0], BANK[1], 5)) {
			if (isQuestMenu()) {
				answer(0);
				return random(2000, 3000);
			}
			if (!isBanking()) {
				int[] banker = getNpcByIdNotTalk(BANKERS);
				if (banker[0] != -1) {
					talkToNpc(banker[0]);
					return random(3000, 3500);
				}
			}
		}

		if (south_entrance == true) {
			pw.setPath(bank_from_south);
		} else {

			if (getY() > 3509 && getY() < 3530) {
				walkTo(FAIL_STAIRS[0], FAIL_STAIRS[1] + 3);
				if (getX() == FAIL_STAIRS[0] && getY() == FAIL_STAIRS[1] + 3) {
					atObject(FAIL_STAIRS[0], FAIL_STAIRS[1]);
				}
			}

			if (hasInventoryItem(932)) {
				if (isAtApproxCoords(NORTH_STAIRS1[0], NORTH_STAIRS1[1], 2)) {
					atObject(NORTH_STAIRS1[0], NORTH_STAIRS1[1] - 3);
				}
				if (getY() <= 3557 && getY() > 3550) {
					atObject(LOG1[0], LOG1[1] + 1);
				}
				if (getY() > 3557) {
					atObject(PIPE1[0] + 1, PIPE1[1]);
				}
				if (getX() >= PIPE2[0] && getY() >= PIPE2[1] - 2) {
					if (inCombat()) {
						walkTo(CHEST[0] - 1, CHEST[1]);
					}
					useItemOnObject(SINISTER_KEY, SINISTER_CHEST);
				}
			} else {
				if (getX() >= PIPE2[0] && getY() >= PIPE2[1] - 2) {
					if (inCombat()) {
						walkTo(PIPE2[0], PIPE2[1]);
					}
					atObject(PIPE2[0], PIPE2[1]);
				}
				if (getX() < PIPE2[0] && getY() >= LOG2[1]) {
					if (inCombat()) {
						walkTo(getX(), getY());
					}
					atObject(LOG2[0], LOG2[1] - 1);
				}
				if (getY() <= 3557 && getY() > 3550) {
					atObject(NORTH_STAIRS2[0], NORTH_STAIRS2[1] + 1);
				}
				if (getY() < 3000) {
					pw.setPath(bank_from_north);
				}
			}
		}
		return random(100, 600);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("OK")) {
			try {
				FIGHTMODE = (combat_choice.getSelectedIndex() + 1);
				if (entrance_choice.getSelectedIndex() == 1) {
					south_entrance = true;
				}
				for (int i = 0; i < FOOD_IDS.length; i++) {
					if (FOOD_NAME[i].equals(food_choice.getSelectedItem())) {
						FOOD = FOOD_IDS[i];
					}
				}
				switch (FOOD) {
					case 546:
						HEAL_AT = getCurrentLevel(3) - 20;
						break;
					case 370:
						HEAL_AT = getCurrentLevel(3) - 14;
						break;
					case 373:
						HEAL_AT = getCurrentLevel(3) - 12;
						break;
				}
				bank_from_north = pw.calcPath(NORTH_STAIRS1[0], NORTH_STAIRS1[1], BANK[0], BANK[1]);
				bank_from_south = pw.calcPath(SOUTH_STAIRS1[0], SOUTH_STAIRS1[1], BANK[0], BANK[1]);
				north_stairs = pw.calcPath(BANK[0], BANK[1], NORTH_STAIRS1[0], NORTH_STAIRS1[1]);
				south_stairs = pw.calcPath(BANK[0], BANK[1], SOUTH_STAIRS1[0], SOUTH_STAIRS1[1]);
				time = System.currentTimeMillis();
			} catch (Throwable t) {
				System.out.println("Error parsing field. Script cannot start. Check your inputs.");
			}
		}
		frame.setVisible(false);
	}


	@Override
	public void paint() {
		int x = 325;
		int y = 65;
		drawString("Herbs Collected", x - 10, y, 4, 0xFFAF2E);
		y += 15;
		if (GUAM_LEAF_COUNT < 1 && MARRENTILL_COUNT < 1 && TARROMIN_COUNT < 1 && HARRALANDER_COUNT < 1
			&& RANARR_WEED_COUNT < 1 && IRIT_LEAF_COUNT < 1 && AVANTOE_COUNT < 1 && KWUARM_COUNT < 1
			&& CANDANTINE_COUNT < 1 && DWARF_WEED_COUNT < 1 && SNAKE_WEED_COUNT < 1 && ARDRIGAL_COUNT < 1
			&& SITO_FOIL_COUNT < 1 && VOLENCIA_MOSS_COUNT < 1 && ROGUES_PURSE_COUNT < 1 && TORSTOL_COUNT < 1) {
			drawString("no items yet", x, y, 1, 0xFFFFFF);
			y += 15;
		} else {
			if (GUAM_LEAF_COUNT > 1) {
				drawString("Guam Leaf: @gre@" + GUAM_LEAF_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
			if (MARRENTILL_COUNT > 1) {
				drawString("Marretill: @gre@" + MARRENTILL_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
			if (TARROMIN_COUNT > 1) {
				drawString("Tarromin: @gre@" + TARROMIN_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
			if (HARRALANDER_COUNT > 1) {
				drawString("Harralander: @gre@" + HARRALANDER_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
			if (RANARR_WEED_COUNT > 1) {
				drawString("Ranarr Weed: @gre@" + RANARR_WEED_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
			if (IRIT_LEAF_COUNT > 1) {
				drawString("Irit Leaf: @gre@" + IRIT_LEAF_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
			if (AVANTOE_COUNT > 1) {
				drawString("Avantoe: @gre@" + AVANTOE_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
			if (KWUARM_COUNT > 1) {
				drawString("Kwuarm: @gre@" + KWUARM_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
			if (CANDANTINE_COUNT > 1) {
				drawString("Candantine: @gre@" + CANDANTINE_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
			if (DWARF_WEED_COUNT > 1) {
				drawString("Dwarf Weed: @gre@" + DWARF_WEED_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
			if (SNAKE_WEED_COUNT > 1) {
				drawString("Snake Weed: @gre@" + SNAKE_WEED_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
			if (ARDRIGAL_COUNT > 1) {
				drawString("Ardrigal: @gre@" + ARDRIGAL_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
			if (SITO_FOIL_COUNT > 1) {
				drawString("Sito Foil: @gre@" + SITO_FOIL_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
			if (VOLENCIA_MOSS_COUNT > 1) {
				drawString("Volencia Moss: @gre@" + VOLENCIA_MOSS_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
			if (ROGUES_PURSE_COUNT > 1) {
				drawString("Rogues Purse: @gre@" + ROGUES_PURSE_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
			if (TORSTOL_COUNT > 1) {
				drawString("Torstol: @gre@" + TORSTOL_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
				y += 15;
			}
		}
		drawString("Runtime", x - 10, y, 4, 0xFFAF2E);
		y += 15;
		drawString(getTimeRunning(), x, y, 1, 0xFFFFFF);
		y += 15;
		drawString("Keys Withdrawn: @gre@" + KEY_COUNT + "@whi@", x, y, 1, 0xFFFFFF);
	}


	private String getTimeRunning() {
		long time = ((System.currentTimeMillis() - this.time) / 1000);
		if (time >= 7200) {
			return (time / 3600) + " hours, " + ((time % 3600) / 60) + " minutes";
		}
		if (time >= 3600 && time < 7200) {
			return (time / 3600) + " hour, " + ((time % 3600) / 60) + " minutes";
		}
		if (time >= 60) {
			return time / 60 + " minutes, " + (time % 60) + " seconds";
		}
		return time + " seconds";
	}
}
