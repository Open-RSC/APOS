import java.util.Locale;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.Checkbox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.*;
import java.util.ArrayList;
import java.net.*;
import java.io.*;
import com.aposbot.Constants;
import com.aposbot.StandardCloseHandler;
import static java.lang.System.currentTimeMillis;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;

public final class MomentumShopPurchase extends Script
        implements ActionListener {

private static final int AUBURY = 54;
private Frame frame;
private boolean init;
private int[] npcList = {};
private int[] npcShop = {-1,-1,-1};
private int shopNpcId;
private int shopAnswer;
private int[] item_ids;
private int[] start_bank_items;
private int[] bank_items;
private boolean firstBank = true;
private int maxGP;
private Choice ch_npcs;
private Choice ch_answer;
private TextField tf_items;
private TextField tf_max_gp;
private Checkbox cb_bank;
private int starting_gp;
private int stage     = 0;
private long startTime;
private long start_time;
private boolean debug = false;
private int[] debugCount = new int[100];
private long lastBuy;
private ArrayList<Integer> itemcount = new ArrayList<>();
private ArrayList<Integer> startitemcount = new ArrayList<>();
private long lastInShop;

private boolean bankingOn = false;

private final PathWalker pw;
private boolean pw_init = false;
private PathWalker.Location bank;
private PathWalker.Path to_bank;
private PathWalker.Path from_bank;
private PathWalker.Path stuck_to_npc;

private final DecimalFormat int_format = new DecimalFormat("#,##0");

public MomentumShopPurchase (Extension e) {
        super(e);
        pw = new PathWalker(e);
}

public static void main(String[] argv) {
        new MomentumShopPurchase(null).init(null);
}

@Override
public void init(String params){
        if (params.equals("debug")) {
                debug = true;
                for (int d=0; d < debugCount.length; d++) {
                        debugCount[d] = 0;
                }
        }
        init = false;
        if (frame == null) {
                ch_npcs = new Choice();
                int npcCount = countNpcs();
                for (int n = 0; n < npcCount; n++) {
                        int npcId = getNpcId(n);
                        String npcName = getNpcName(n);
                        ch_npcs.add(npcName);
                        if (npcList.length > 0) {
                                npcList = addToArray(npcList,npcId);
                        } else {
                                npcList = new int[] {npcId};
                        }
                }
                ch_answer = new Choice();
                ch_answer.add("1st Menu Response");
                ch_answer.add("2nd Menu Response");
                ch_answer.add("3rd Menu Response");
                ch_answer.add("4th Menu Response");

                Panel pInput = new Panel();
                pInput.setLayout(new GridLayout(0,2,0,2));
                pInput.add(new Label("Which NPC?"));
                pInput.add(ch_npcs);
                pInput.add(new Label("Which menu response opens the shop?"));
                pInput.add(ch_answer);
                pInput.add(new Label("Item's to buy? (1,2,3..)"));
                pInput.add(tf_items = new TextField());
                pInput.add(new Label("Max GP to spend?"));
                pInput.add(tf_max_gp = new TextField("0"));
                pInput.add(cb_bank = new Checkbox("Bank"));

                Panel buttonPanel = new Panel();
                Button ok = new Button("OK");
                ok.addActionListener(this);
                buttonPanel.add(ok);
                Button cancel = new Button("Cancel");
                cancel.addActionListener(this);
                buttonPanel.add(cancel);
                frame = new Frame(getClass().getSimpleName());
                frame.setIconImages(Constants.ICONS);
                frame.addWindowListener(
                        new StandardCloseHandler(frame, StandardCloseHandler.HIDE)
                        );
                frame.add(pInput, BorderLayout.NORTH);
                frame.add(buttonPanel, BorderLayout.SOUTH);
                frame.setResizable(false);
                frame.pack();
        }
        frame.setLocationRelativeTo(null);
        frame.toFront();
        frame.requestFocus();
        frame.setVisible(true);
        for (int ii =0; ii <= 1289; ii++) {
                itemcount.add(0);
                startitemcount.add(0);
        }
}



@Override
public int main()
{
        if (stage == 0) {
                startTime = System.currentTimeMillis();
                lastInShop = System.currentTimeMillis();
                lastBuy = System.currentTimeMillis();
                start_time = System.currentTimeMillis();
                if (debug && debugCount[0] > 15) {
                        System.out.println("DEBUG: Setting up script.");
                        debugCount[0] = 0;
                } else {
                        debugCount[0]++;
                }
                starting_gp = getInventoryCount(10);
                for(int buyItem = 0; buyItem < item_ids.length; buyItem++)
                {
                        startitemcount.set(item_ids[buyItem], getInventoryCount(item_ids[buyItem]));
                }
                if (cb_bank.getState()) {
                        boolean allStackable = true;
                        start_bank_items = new int[item_ids.length];
                        bank_items = new int[item_ids.length];
                        for(int buyItem = 0; buyItem < item_ids.length; buyItem++)
                        {
                                if (debug) {
                                        System.out.println("DEBUG: Stage 0. Checking if item:" + item_ids[buyItem] + " is stackable");
                                }
                                if (!isItemStackableId(item_ids[buyItem])) {
                                        allStackable = false;
                                }
                        }
                        if (allStackable) {
                                System.out.println("Disabling banking since all items are stackable!");
                        } else {
                                pw.init(null);
                                pw_init = true;
                                npcShop = getAllNpcById(shopNpcId);
                                bank = pw.getNearestBank(npcShop[1],npcShop[2]);
                                if (bank == null) {
                                        System.out.println("ERROR: No usable bank found!");
                                        start_time = -1L;
                                        setAutoLogin(false);
                                        stopScript();
                                        return 0;
                                }
                                bankingOn = true;
                                System.out.println("Nearest bank: " + bank.name);
                                to_bank = pw.calcPath(npcShop[1], npcShop[2], bank.x, bank.y);
                                from_bank = pw.calcPath(bank.x, bank.y, npcShop[1], npcShop[2]);
                        }
                }
                stage = 1;
        }
        if (pw_init) {
                if (pw.walkPath()) return 0;
        }
        if (maxGP > 0) {
                if ((starting_gp - getInventoryCount(10)) > maxGP ) {
                        System.out.println("Stopping Script. Over max GP spent: " + (starting_gp - getInventoryCount(10)));
                        stopScript();
                }
        }
        long buyTimeNow = currentTimeMillis();
        long noBuy = (buyTimeNow - lastBuy) / 1000;
        if (noBuy >= 400 && stage <= 4) {
                if (isShopOpen()) {
                        closeShop();
                        stage = 1;
                        lastBuy = currentTimeMillis() - (250 * 1000);
                }
        }

        if (!isQuestMenu() && !isShopOpen() && stage <= 4) {
                if (debug && debugCount[02] > 15) {
                        System.out.println("DEBUG: Stage 0. Out of shop timeout reset.");
                        debugCount[02] = 0;
                } else {
                        debugCount[02]++;
                }
                long timeNow = currentTimeMillis();
                long outOfShop = (timeNow - lastInShop);
                if ((outOfShop / 1000) > 60) {
                        stage = 1;
                }
        }

        if(stage == 1) {
                if (isShopOpen()) {
                        if (debug && debugCount[11] > 15) {
                                System.out.println("DEBUG: Stage 1.1. Shop is moving moving to purchase.");
                                debugCount[11] = 0;
                        } else {
                                debugCount[11]++;
                        }
                        stage = 3;
                        return random(200,500);
                }

                //System.out.println("stage_1");
                if (!isShopOpen() && !isQuestMenu()) {
                        if (debug && debugCount[12] > 15) {
                                System.out.println("DEBUG: Stage 1.2. Talking to NPC");
                                debugCount[12] = 0;
                        } else {
                                debugCount[12]++;
                        }
                        int[] shopNPC = getNpcById(shopNpcId);
                        talkToNpc(shopNPC[0]);
                        return random(1000, 2000);
                }

                if (isQuestMenu()) {
                        if (debug && debugCount[13] > 15) {
                                System.out.println("DEBUG: Stage 1.3. Moving to next stage.");
                                debugCount[13] = 0;
                        } else {
                                debugCount[13]++;
                        }
                        stage++;
                        return random(2000,3000);
                }
                if (!isQuestMenu() && !isShopOpen()) {
                        if (debug && debugCount[14] > 15) {
                                System.out.println("DEBUG: Stage 1.4. Out of shop timeout. Resetting.");
                                debugCount[14] = 0;
                        } else {
                                debugCount[14]++;
                        }
                        long timeNow = currentTimeMillis();
                        long outOfShop = (timeNow - lastInShop);
                        if ((outOfShop / 1000) > 200) {
                                stage = 1;
                        }
                }
                if (debug && debugCount[15] > 15) {
                        System.out.println("DEBUG ERROR: Stage 1.5. Reaching end of stage 1. Nothing will happen.");
                        debugCount[15] = 0;
                } else {
                        debugCount[15]++;
                }
                return random(3000,4000);

        }

        if(stage == 2)
        {
                if (isQuestMenu() && !isShopOpen()) {
                        if (debug && debugCount[21] > 15) {
                                System.out.println("DEBUG: Stage 2.1. Answering npc with first answer.");
                                debugCount[21] = 0;
                        } else {
                                debugCount[21]++;
                        }
                        //System.out.println("stage_2");
                        answer(shopAnswer);
                        return random(2500, 4000);
                }
                if (isShopOpen()) {
                        if (debug && debugCount[22] > 15) {
                                System.out.println("DEBUG: Stage 2.2. Shop is open moving to purchase.");
                                debugCount[22] = 0;
                        } else {
                                debugCount[22]++;
                        }
                        stage = 3;
                        return random(1000,2000);
                }
                if (!isQuestMenu() && !isShopOpen()) {
                        if (debug && debugCount[23] > 15) {
                                System.out.println("DEBUG: Stage 2.3. Out of shop timeout. resetting.");
                                debugCount[23] = 0;
                        } else {
                                debugCount[23]++;
                        }
                        long timeNow = currentTimeMillis();
                        long outOfShop = (timeNow - lastInShop);
                        if ((outOfShop / 1000) > 60) {
                                stage = 1;
                        }
                }
                if (debug && debugCount[24] > 15) {
                        System.out.println("DEBUG ERROR: Stage 2.4. Reaching end of stage 2. nothing will happen.");
                        debugCount[24] = 0;
                } else {
                        debugCount[24]++;
                }
                return random(1000,2000);
        }

        if(stage == 3) {
                //System.out.println("stage_3");
                //System.out.println("stage_3.1");
                if (isShopOpen()) {
                        if (debug && debugCount[31] > 15) {
                                System.out.println("DEBUG: Stage 3.1. Shop is open.");
                                debugCount[31] = 0;
                        } else {
                                debugCount[31]++;
                        }
                        lastInShop = currentTimeMillis();
                        for(int buyItem = 0; buyItem < item_ids.length; buyItem++)
                        {
                                int shopItem  = getShopItemById(item_ids[buyItem]);
                                int itemShopCount = getShopItemAmount(shopItem);
                                if (itemShopCount < 1)
                                {
                                        continue;
                                }
                                if (debug && debugCount[32] > 50) {
                                        System.out.println("DEBUG: Stage 3.2. Buying Item: " + item_ids[buyItem]);
                                        debugCount[32] = 0;
                                } else {
                                        debugCount[32]++;
                                }
                                lastBuy = currentTimeMillis();
                                if (bankingOn) {
                                        if (getInventoryCount() < MAX_INV_SIZE) {
                                                if (isItemStackableId(item_ids[buyItem])) {
                                                        buyShopItem(shopItem, itemShopCount);
                                                        if (debug && debugCount[60] > 5) {
                                                                System.out.println("DEBUG: Bank Mode On: Stage 6.0. Buying Stackable item.");
                                                                debugCount[60] = 0;
                                                        } else {
                                                                debugCount[60]++;
                                                        }
                                                } else {
                                                        if (itemShopCount < getEmptySlots()) {
                                                                buyShopItem(shopItem, itemShopCount);
                                                        } else {
                                                                buyShopItem(shopItem, 1);
                                                        }
                                                        if (debug && debugCount[61] > 5) {
                                                                System.out.println("DEBUG: Bank Mode On: Stage 6.1. Buying non-stackable item.");
                                                                debugCount[61] = 0;
                                                        } else {
                                                                debugCount[61]++;
                                                        }
                                                }
                                        } else {
                                                stage = 5;
                                                if (debug && debugCount[62] > 2) {
                                                        System.out.println("DEBUG: Bank Mode On: Stage 6.2. Inventory full. switching to banking.");
                                                        debugCount[62] = 0;
                                                } else {
                                                        debugCount[62]++;
                                                }
                                        }
                                } else {
                                        itemcount.set(item_ids[buyItem], getInventoryCount(item_ids[buyItem]));
                                        buyShopItem(shopItem, itemShopCount);
                                        if (debug && debugCount[63] > 15) {
                                                System.out.println("DEBUG: Bank Mode Off: Stage 6.3. Buying Stackable item.");
                                                debugCount[63] = 0;
                                        } else {
                                                debugCount[63]++;
                                        }
                                }
                                return random(400,500);
                        }
                        if (debug && debugCount[33] > 15) {
                                System.out.println("DEBUG: Stage 3.3. End of buy phase.");
                                debugCount[33] = 0;
                        } else {
                                debugCount[33]++;
                        }
                } else {
                        if (debug && debugCount[34] > 50) {
                                System.out.println("DEBUG: Stage 3.3. Not in shop. Resetting.");
                                debugCount[34] = 0;
                        } else {
                                debugCount[34]++;
                        }
                        stage = 1;
                }
                if (debug && debugCount[35] > 50) {
                        System.out.println("DEBUG ERROR: Stage 3.4. End of stage. Nothing will happen.");
                        debugCount[35] = 0;
                } else {
                        debugCount[35]++;
                }
                return random(1000,2000);
        }



        if(stage == 4) {
                if (debug && debugCount[41] > 15) {
                        System.out.println("DEBUG: Stage 4.1. Resetting back to stage 1.");
                        debugCount[41] = 0;
                } else {
                        debugCount[41]++;
                }
                stage = 1;
                return random(1500, 2000);
        }

        if (stage == 5) {
                if (needToBank()) {
                        if (!isAtApproxCoords(bank.x, bank.y,10)) {
                                pw.setPath(to_bank);
                                if (debug) {
                                        System.out.println("DEBUG: Full inventory. Setting Pathwalker to go to bank.");
                                }
                                return 0;
                        } else {
                                if (isBanking() && cb_bank.getState()) {
                                        return banking();
                                }
                                if (isQuestMenu() && cb_bank.getState()) {
                                        answer(0);
                                        return random(1500, 2000);
                                }
                                int[] banker = getNpcByIdNotTalk(BANKERS);
                                if (banker[0] != -1) {
                                        talkToNpc(banker[0]);
                                }
                                return random(1500, 2000);
                        }
                } else {
                        if (isAtApproxCoords(bank.x, bank.y,10)) {
                                pw.setPath(from_bank);
                                if (debug) {
                                        System.out.println("DEBUG: Finished Banking. Setting Pathwalker to go to bank.");
                                }
                                return 0;
                        } else {
                                if (isAtApproxCoords(npcShop[1],npcShop[2],10)) {
                                        stage = 1;
                                        return 0;
                                } else {
                                        stuck_to_npc = pw.calcPath(getX(), getY(), npcShop[1], npcShop[2]);
                                        pw.setPath(stuck_to_npc);
                                        return 0;
                                }
                        }
                }
        }
        return random(2000, 3000);
}

private boolean needToBank(){
        if (bankingOn) {
                if (getEmptySlots() < 1) {
                        return true;
                }
                for(int buyItem = 0; buyItem < item_ids.length; buyItem++)
                {
                        if (getInventoryCount(item_ids[buyItem]) > 0) {
                                return true;
                        }
                }
        }
        return false;
}

private int banking() {
        if (firstBank) {
                for (int i=0; i < item_ids.length; i++) {
                        start_bank_items[i] = bankCount(item_ids[i]);
                        bank_items[i] = bankCount(item_ids[i]);
                }
                firstBank = false;
        } else {
                for (int i=0; i < item_ids.length; i++) {
                        bank_items[i] = bankCount(item_ids[i]);
                }
        }
        for (int i = 0; i < item_ids.length; ++i) {
                int count = getInventoryCount(item_ids[i]);
                if (count > 0) {
                        if (debug) {
                                System.out.println("DEBUG: Banking: Depositing " + count + " " + getItemNameId(item_ids[i]));
                        }
                        deposit(item_ids[i], count);
                        return random(600, 800);
                }
        }
        closeBank();
        return random(600, 800);
}

@Override
public void onServerMessage(String str) {
        str = str.toLowerCase(Locale.ENGLISH);

        if (str.contains("Welcome")) {
                stage = 1;
        }
}

@Override
public void onKeyPress(int keycode) {
        if (keycode == KeyEvent.VK_F2) {
                if (debug) {
                        debug = false;
                } else {
                        debug = true;
                }
        } else {
                super.onKeyPress(keycode);
        }
}

public int[] addToArray(int[] srcArray, int elementToAdd) {
        int[] destArray = new int[srcArray.length+1];

        for(int i = 0; i < srcArray.length; i++) {
                destArray[i] = srcArray[i];
        }

        destArray[destArray.length - 1] = elementToAdd;
        return destArray;
}

@Override
public void paint() {
        long time = currentTimeMillis();
        float timeDiff = (float)(time - startTime);
        final int white = 0xFFFFFF;
        int y = 140;
        int x = 300;
        drawString("Momentum Universal Shop Buyer", x, y, 1, 0x005D7B);
        y += 15;
        drawString("Runtime: " + get_runtime(), x, y, 1, 0xFFFFFF);
        y += 15;
        int spentGP = (starting_gp - getInventoryCount(10));
        String gpPerHr = per_hour(spentGP);
        drawString("Spent Coins: " + spentGP + " (" + gpPerHr + " /hr)", x, y, 1, 0xFFA600);
        y += 15;
        if (bankingOn) {
                if (bank_items.length > 0){
                for (int b=0; b < bank_items.length; b++) {
                        int diff = bank_items[b] - start_bank_items[b];
                        if (diff > 0) {
                                String dperh = per_hour(diff);
                                drawString("Banked " + getItemNameId(item_ids[b]) + ": " + diff + " (" + dperh + "/hr)", x, y, 1, 0xFFA600);
                                y += 15;
                        }
                }
              }
        } else {
                if (itemcount.size() > 0) {
                        int num = itemcount.size();

                        for (int i = 0; i < num; i++ ) {

                                if ((itemcount.get(i) - startitemcount.get(i)) <= 0) {
                                        continue;
                                }
                                int purchased = (itemcount.get(i) - startitemcount.get(i));
                                int purchPerHour = (int)Math.round(((float)purchased / (timeDiff / 1000)) * 60 * 60);
                                drawString("Purchased " + getItemNameId(i) + ": " + purchased + " (" + purchPerHour + " /hr)", x, y, 1, 0xFFA600);
                                y += 15;
                        }
                }
        }
}

@Override
public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals("OK")) {
                try {
                        shopNpcId = npcList[ch_npcs.getSelectedIndex()];
                } catch (Throwable t) {
                        System.out.println("Error setting NPC id");
                        shopNpcId = 0;
                }
                try {
                        shopAnswer = ch_answer.getSelectedIndex();
                } catch (Throwable t) {
                        System.out.println("Error with shop answer");
                        shopAnswer = 0;
                }
                try {
                        String[] array = tf_items.getText().trim().split(",");
                        int array_sz = array.length;
                        item_ids = new int[array_sz];
                        for (int i = 0; i < array_sz; i++) {
                                item_ids[i] = Integer.parseInt(array[i]);
                        }
                } catch (Throwable t) {
                        System.out.println("Couldn't parse item ids");
                        item_ids = new int[0];
                }
                try {
                        maxGP = Integer.parseInt(tf_max_gp.getText().trim());
                } catch (Throwable t) {
                        System.out.println("Couldn't parse max GP");
                }
        }
        frame.setVisible(false);
}
private String per_hour(int total) {

        if (total == 0) return "0";
        double amount = total * 60.0 * 60.0;
        double secs = (System.currentTimeMillis() - start_time) / 1000.0;
        return int_format((long)(amount / secs));
}

private String int_format(long l) {
        return int_format.format(l);
}

private String get_runtime() {
        long millis = (System.currentTimeMillis() - start_time) / 1000;
        long second = millis % 60;
        long minute = (millis / 60) % 60;
        long hour = (millis / (60 * 60)) % 24;
        long day = (millis / (60 * 60 * 24));

        if (day > 0L) {
                return String.format("%02d d, %02d h, %02d m",
                                     day, hour, minute);
        }
        if (hour > 0L) {
                return String.format("%02d h, %02d m, %02d s",
                                     hour, minute, second);
        }
        if (minute > 0L) {
                return String.format("%02d min, %02d sec",
                                     minute, second);
        }
        return String.format("%02d seconds", second);
}
}
