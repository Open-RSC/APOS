import com.aposbot.Constants;
import com.aposbot.StandardCloseHandler;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Locale;

public class EdgeGiants extends Script implements ActionListener {

    // locations and pathing
    private PathWalker.Path PATH_TO_HUT, PATH_TO_BANK;
    private final Point HUT = new Point(202,486);
    private final Point BANK = new Point(154,508);
    private final Point BANK_DOOR = new Point(150,507);
    private final Point HUT_DOOR = new Point(202,485);
    private final Point DUNGEON_GATE = new Point(208, 3317);
    private PathWalker pw;

    // ids
    private final int GIANT = 61;
    private final int BRASS_KEY = 99;

    // configurable
    private boolean bury;
    private int food, foodCount;
    private int[] pickup;

    // tracking
    private long startTime = -1L;
    private int[] startXp = new int[] { 0, 0 }; // { combat,  pray }
    private int[] currentXp = new int[] { 0, 0}; // { combat,  pray }
    private final DecimalFormat iformat = new DecimalFormat("#,##0");

    // ui
    private Frame frame;
    private TextField tf_eatAt, tf_foodId, tf_foodQuantity, tf_pickup;
    private Choice ch_buryBones;

    public EdgeGiants(Extension ex) {
        super(ex);
        pw = new PathWalker(ex);
    }

    @Override
    public void init(String params) {

        pw.init(null);
        PATH_TO_BANK = pw.calcPath(HUT.x, HUT.y, BANK.x, BANK.y);
        PATH_TO_HUT = pw.calcPath(BANK.x, BANK.y, HUT.x, HUT.y);

        if (getInventoryCount(BRASS_KEY) == 0) {
            System.out.println("This script banks via brass key hut and you don't have a brass key!");
            stopScript();
            return;
        }

        if (frame == null) {

            Panel pInput = new Panel(new GridLayout(0, 2, 0, 2));

            // bone bury
            ch_buryBones = new Choice();
            ch_buryBones.add("Yes");
            ch_buryBones.add("No");
            pInput.add(new Label("Bury Bones"));
            pInput.add(ch_buryBones);

            // Eating options
            pInput.add(new Label("Food ID"));
            tf_foodId = new TextField();
            tf_foodId.setText(Integer.toString(370));
            pInput.add(tf_foodId);

            pInput.add(new Label("Food Quantity"));
            tf_foodQuantity = new TextField();
            tf_foodQuantity.setText(Integer.toString(1));
            pInput.add(tf_foodQuantity);

            // looting
            pInput.add(new Label("Loot Table (csv)"));
            tf_pickup = new TextField();
            tf_pickup.setText("413,526,527,1277,220,438,439,440,441,442,443,1092");
            pInput.add(tf_pickup);

            Button button;
            Panel pButtons = new Panel();
            button = new Button("OK");
            button.addActionListener(this);
            pButtons.add(button);
            button = new Button("Cancel");
            button.addActionListener(this);
            pButtons.add(button);

            frame = new Frame(getClass().getSimpleName());
            frame.addWindowListener(
                    new StandardCloseHandler(frame, StandardCloseHandler.HIDE)
            );
            frame.setIconImages(Constants.ICONS);
            frame.add(pInput, BorderLayout.NORTH);
            frame.add(pButtons, BorderLayout.SOUTH);
            frame.setResizable(false);
            frame.pack();
        }
        frame.setLocationRelativeTo(null);
        frame.toFront();
        frame.requestFocus();
        frame.setVisible(true);

    }

    @Override
    public int main() {

        if (startTime == -1L) {
            startTime = System.currentTimeMillis();
            startXp[0] = getXpForLevel(0) +  getXpForLevel(1) +  getXpForLevel(2);
            startXp[1] =  getXpForLevel(5);
        }

        currentXp[0] = getXpForLevel(0) +  getXpForLevel(1) +  getXpForLevel(2);
        currentXp[1] = getXpForLevel(5);

        if (pw.walkPath()) return 100;

        if (getFatigue() > 50) {
            useSleepingBag();
            return 1000;
        }

        // bury bones
        if (getInventoryCount(413) > 0 && !inCombat() && bury) {
            System.out.println("Burying bones");
            useItem(getInventoryIndex(413));
            return 1200;
        }

        if (isQuestMenu()) {
            answer(0);
            return 2000;
        }


        if (isBanking()) {
            if (getInventoryCount(pickup) > 0) {
                for(int id : pickup) {
                    if (getInventoryCount(id) > 0) {
                        System.out.println("Depositing " + getItemNameId(id));
                        deposit(id, getInventoryCount(id));
                        return 1000;
                    }
                }
            }
            if (foodCount > 0 && getInventoryCount(food) < foodCount) {
                System.out.println("Withdrawing food");
                withdraw(food, foodCount - getInventoryCount(food));
                return 1000;
            }
            closeBank();
            return 1000;
        }

        if (getY() > 3000) { // down
            if (getInventoryCount(food) > 0 && (getHpPercent() <= 50)) {
                if (inCombat()) return 100;
                System.out.println("Eating");
                useItem(getInventoryIndex(food));
                return 1000;
            }
            if (getInventoryCount(food) == 0 || getInventoryCount() == MAX_INV_SIZE) { // go up ladder if no food or inv full
                if(isReachable(204,3314)) {
                    if (inCombat()) {
                        System.out.println("Walking to ladder");
                        walkTo(204,3314);
                        return 1000;
                    }
                    System.out.println("Using ladder");
                    atObject(203,3314);
                    return 2000;
                } else {
                    if (inCombat()) {
                        walkTo(getX(), getY());
                        return 1000;
                    }
                    if (getObjectIdFromCoords(DUNGEON_GATE.x, DUNGEON_GATE.y) == 57) {
                        System.out.println("Opening dungeon gate");
                        atObject(DUNGEON_GATE.x, DUNGEON_GATE.y);
                        return 1000;
                    }
                }
            } else {
                if (inCombat()) return 1000;
                int[] item = getGroundItems();
                if (item[0] != -1) {
                    System.out.println("Pickup item");
                    pickupItem(item[0], item[1], item[2]);
                    return 500;
                }
                int[] npc = getNearestNpc(new int[] {GIANT});
                if (npc[0] != -1) {
                    if (isReachable(npc[1], npc[2])) {
                        System.out.println("Attack npc");
                        attackNpc(npc[0]);
                        return 2000;
                    } else {
                        if (getObjectIdFromCoords(DUNGEON_GATE.x, DUNGEON_GATE.y) == 57) {
                            System.out.println("Open gate");
                            atObject(DUNGEON_GATE.x, DUNGEON_GATE.y);
                            return 1000;
                        }
                    }
                }
            }
        } else { // up
            if (getInventoryCount(food) == 0 || getInventoryCount() == MAX_INV_SIZE) { // go to bank
                int[] banker = getNpcByIdNotTalk(BANKERS);
                if (banker[0] != -1) {
                    if (isReachable(banker[1], banker[2])) {
                        System.out.println("Getting banker");
                        talkToNpc(banker[0]);
                        return 2000;
                    } else {
                        if (getObjectIdFromCoords(BANK_DOOR.x, BANK_DOOR.y) == 2) {
                            System.out.println("Opening bank door");
                            atObject(BANK_DOOR.x, BANK_DOOR.y);
                            return 1000;
                        }
                    }
                }
                if (getY() < HUT_DOOR.y) {
                    System.out.println("Leaving hut door");
                    useItemOnWallObject(getInventoryIndex(BRASS_KEY), HUT_DOOR.x, HUT_DOOR.y);
                    return 2000;
                } else {
                    System.out.println("Walk to bank");
                    pw.setPath(PATH_TO_BANK);
                    return 1000;
                }

            } else {
                if (distanceTo(HUT_DOOR.x, HUT_DOOR.y) < 5) {
                    if (getY() >= HUT_DOOR.y) {
                        System.out.println("Entering hut door");
                        useItemOnWallObject(getInventoryIndex(BRASS_KEY), HUT_DOOR.x, HUT_DOOR.y);
                        return 2000;
                    } else {
                        System.out.println("Going down ladder");
                        atObject(203, 482);
                        return 2000;
                    }
                } else {
                    System.out.println("Walk to hut");
                    pw.setPath(PATH_TO_HUT);
                    return 1000;
                }
            }
        }

        return 1000;
    }

    private int[] getNearestNpc(int[] ids) {
        int[] attack = new int[] { -1, -1, -1};
        int mindist = Integer.MAX_VALUE;
        int count = countNpcs();
        for (int i = 0; i < count; i++) {
            if (isNpcInCombat(i)) continue;
            if (getNpcId(i) == GIANT) {
                int y = getNpcY(i);
                int x = getNpcX(i);
                int dist = distanceTo(x, y, getX(), getY());
                if (dist < mindist) {
                    attack[0] = i;
                    attack[1] = x;
                    attack[2] = y;
                    mindist = dist;
                }
            }
        }
        return attack;
    }

    private int[] getGroundItems() {
        int[] item = new int[] {
                -1, -1, -1
        };
        int count = getGroundItemCount();
        int max_dist = Integer.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            int id = getGroundItemId(i);
            if (inArray(pickup, id)) {
                int x = getItemX(i);
                int y = getItemY(i);
                if (!isReachable(x, y)) continue;
                int dist = distanceTo(x, y, getX(), getY());
                if (dist < max_dist) {
                    item[0] = id;
                    item[1] = x;
                    item[2] = y;
                    max_dist = dist;
                }
            }
        }
        return item;
    }


    @Override
    public void paint() {
        int y = 25;
        drawString("Edge Dungeon", 190, y, 1, 0xFFFFFF);
        y += 13;
        drawString("Runtime: " + get_time_since(startTime), 190, y, 1, 0xFFFFFF);
        y += 13;
        drawString("Combat Xp/Hr: " + per_hour(currentXp[0] - startXp[0], startTime), 190, y, 1, 0xFFFFFF);
        y += 13;
        drawString("Prayer Xp/Hr: " + per_hour(currentXp[1] - startXp[1], startTime), 190, y, 1, 0xFFFFFF);
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

    // ripped from Shantay_Trader
    private String per_hour(double count, long time) {
        double amount, secs;

        if (count == 0) return "0";
        amount = count * 60.0 * 60.0;
        secs = (System.currentTimeMillis() - time) / 1000.0;
        return iformat.format(amount / secs);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("OK")) {
            bury = ch_buryBones.getSelectedItem().equalsIgnoreCase("yes") ? true : false;
            try {
                food = Integer.parseInt(tf_foodId.getText().trim());
                foodCount = Integer.parseInt(tf_foodQuantity.getText().trim());
                String[] loots = tf_pickup.getText().trim().split(",");
                pickup = new int[loots.length];
                for (int i = 0; i < loots.length; i++) {
                    pickup[i] = Integer.parseInt(loots[i]);
                }
            } catch (Throwable t) {
                System.out.println("Error parsing config values");
                System.out.println(t);
            }
        }
        frame.setVisible(false);
    }
}
