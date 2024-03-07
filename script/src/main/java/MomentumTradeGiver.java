import java.util.Arrays;
import java.util.Locale;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.aposbot.Constants;
import com.aposbot.StandardCloseHandler;

public final class MomentumTradeGiver extends Script
        implements ActionListener {

private boolean[] itm_offered;
private int[] start_count;
private int[] item_count;
private String name;
private long bank_time;
private int shantywalk = 0;
private int ptr;
private boolean move_to;
private long menu_time;
private long start_time;
private boolean firstBank = true;
private int tFields = 0;
private int frameWidth = 400;
private TextField tf_username;
private ArrayList<TextField> item_id_fields = new ArrayList<>();
private ArrayList<TextField> item_qty_fields = new ArrayList<>();
private int[] item_ids;
private int[] item_qty;

private Panel uPanel = new Panel();
private Panel pInput = new Panel();
private Panel buttonPanel = new Panel();
private Frame frame;


private final DecimalFormat int_format = new DecimalFormat("#,##0");

public MomentumTradeGiver(Extension ex) {
        super(ex);
}

public static void main(String[] argv) {
        new MomentumTradeGiver(null).init(null);
}

@Override
public void init(String params) {
        if (frame == null) {

                uPanel.setLayout(new GridLayout(0,2,0,2));

                uPanel.add(new Label("Username:"));
                uPanel.add(tf_username = new TextField());

                uPanel.add(new Label(""));
                Button addButton = new Button("Add Item");
                addButton.addActionListener(this);
                uPanel.add(addButton);

                pInput.setLayout(new GridLayout(0, 2, 0, 2));


                pInput.add(new Label("Item ID"));
                pInput.add(new Label("Quantity (0 for unlimited)"));
                item_id_fields.add(new TextField());
                item_qty_fields.add(new TextField("0"));
                pInput.add(item_id_fields.get(tFields));
                pInput.add(item_qty_fields.get(tFields));

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
                frame.add(uPanel, BorderLayout.NORTH);
                frame.add(pInput, BorderLayout.CENTER);
                frame.add(buttonPanel, BorderLayout.SOUTH);
                frame.setResizable(false);
                frame.pack();
                frame.setSize(frameWidth,frame.getSize().height);
        }
        frame.setLocationRelativeTo(null);
        frame.toFront();
        frame.requestFocus();
        frame.setVisible(true);
        ptr = 0;
        menu_time = -1L;
        bank_time = -1L;
        move_to = false;
        start_time = System.currentTimeMillis();
}

@Override
public int main() {
        if (isQuestMenu()) {
                menu_time = -1L;
                answer(0);
                bank_time = System.currentTimeMillis();
                return random(600, 800);
        } else if (menu_time != -1L) {
                if (System.currentTimeMillis() >= (menu_time + 8000L)) {
                        menu_time = -1L;
                }
                return random(300, 400);
        }
        if (isBanking()) {
                if (firstBank) {
                        for (int bitem=0; bitem < item_ids.length; bitem++) {
                                start_count[bitem] = bankCount(item_ids[bitem]) + getInventoryCount(item_ids[bitem]);
                                item_count[bitem] = bankCount(item_ids[bitem]);
                        }
                        firstBank = false;
                } else {
                        for (int bitem=0; bitem < item_ids.length; bitem++) {
                                item_count[bitem] = bankCount(item_ids[bitem]);
                        }
                }
                bank_time = -1L;
                if (getInventoryCount(item_ids[ptr]) != 0) {
                        if (item_qty[ptr] > 0) {
                                if (getInventoryCount(item_ids[ptr]) > (item_qty[ptr] - (start_count[ptr] - item_count[ptr]))) {
                                        int putBack = getInventoryCount(item_ids[ptr]) - (item_qty[ptr] - (start_count[ptr] - item_count[ptr]));
                                        deposit(item_ids[ptr], putBack);
                                        return random(1000,1050);
                                }
                                for (int bitem=0; bitem < item_ids.length; bitem++) {
                                        item_count[bitem] = bankCount(item_ids[bitem]);
                                }
                        }
                        if (!isItemStackableId(item_ids[ptr])) {
                                if (getInventoryCount(item_ids[ptr]) > 24) {
                                        deposit(item_ids[ptr], (getInventoryCount(item_ids[ptr]) - 24));
                                        return random(1000,1050);
                                } else {
                                        closeBank();
                                }
                        }else {
                                closeBank();
                        }
                        return random(600, 800);
                }
                if (item_qty[ptr] > 0 && ((start_count[ptr] - item_count[ptr]) >= item_qty[ptr])) {
                        if (ptr == (item_ids.length - 1)) {
                                System.out.println("Finished Transferring " + item_qty[ptr] + " of " + getItemNameId(item_ids[ptr]));
                                System.out.println("Finished Transferring All Items.");
                                stopScript();
                        } else {
                                System.out.println("Finished Transferring " + item_qty[ptr] + " of " + getItemNameId(item_ids[ptr]));
                                ptr++;
                                System.out.println("Moving on to " + getItemNameId(item_ids[ptr]));
                        }
                }
                int w = getEmptySlots();
                if (w > 24) w = 24;
                int bankc = bankCount(item_ids[ptr]);
                while (bankc <= 0) {
                        if (ptr >= (item_ids.length - 1)) {
                                System.out.println("FINISIHED: Out of items to give.");
                                stopScript();
                                return 0;
                        }
                        System.out.println("Out of " + getItemNameId(item_ids[ptr]));
                        bankc = bankCount(item_ids[++ptr]);
                        System.out.println("Moving on to " + getItemNameId(item_ids[ptr]));
                }
                if (w > bankc) w = bankc;
                if (item_qty[ptr] > 0) {
                        if (w > (item_qty[ptr] - (start_count[ptr] - item_count[ptr]))) w = (item_qty[ptr] - (start_count[ptr] - item_count[ptr]));
                }
                if (isItemStackableId(item_ids[ptr])) {
                        int fullcount = bankCount(item_ids[ptr]);
                        withdraw(item_ids[ptr], fullcount);
                } else {
                        withdraw(item_ids[ptr], w);
                }
                for (int bitem=0; bitem < item_ids.length; bitem++) {
                        item_count[bitem] = bankCount(item_ids[bitem]);
                }
                return random(800, 950);
        } else if (bank_time != -1L) {
                if (System.currentTimeMillis() >= (bank_time + 8000L)) {
                        bank_time = -1L;
                }
                return random(300, 400);
        }
        if (getInventoryCount(item_ids[ptr]) <= 0) {
                int[] banker = getNpcByIdNotTalk(BANKERS);
                if (getX() > 0 && getX() < 100 && getY() > 700 && getY() < 800) {
                        atObject(58,731);
                        return random(1800,2000);
                } else {
                        if (getX() > 440 && getX() < 515 && getY() > 3300 && getY() < 3400) {
                                banker = getNpcByIdNotTalk(792);
                        }

                        if (banker[0] != -1) {
                                menu_time = System.currentTimeMillis();
                                talkToNpc(banker[0]);
                        }
                }
                return random(600, 800);
        }
        if (isInTradeConfirm()) {
                confirmTrade();
                return random(1000, 2000);
        }
        if (isInTradeOffer()) {
                if (getLocalTradeItemCount() <= 0) {
                        int index = getInventoryIndex(item_ids[ptr]);
                        if (index != -1) {
                                int count = getInventoryCount(item_ids[ptr]);
                                if (count > 12) count = 12;
                                if (isItemStackableId(item_ids[ptr])) {
                                        count = getInventoryCount(item_ids[ptr]);
                                }
                                offerItemTrade(index, count);
                                if (!itm_offered[ptr]) {
                                        itm_offered[ptr] = true;
                                }
                                return random(1000, 2000);
                        }
                }
                acceptTrade();
                return random(1000, 2000);
        }
        int[] player = getPlayerByName(name);
        if (player[0] == -1) {
                System.out.println("ERROR: Couldn't find player: " + name);
                System.out.println(
                        "Make sure you entered their name properly.");
                return random(1000, 1500);
        }
        if (!isWalking()) {
                if (move_to) {
                        walkTo(player[1], player[2]);
                        move_to = false;
                } else {
                        Arrays.fill(itm_offered, false);
                        if (getX() > 0 && getX() < 100 && getY() > 700 && getY() < 800) {
                                if (shantywalk > 4) {
                                        walkTo(61,730);
                                        shantywalk = 0;
                                } else {
                                        shantywalk++;
                                }
                        }
                        sendTradeRequest(getPlayerPID(player[0]));
                        return random(2000, 3000);
                }
        }
        return random(1000, 2000);
}

@Override
public void onServerMessage(String str) {
        str = str.toLowerCase(Locale.ENGLISH);
        if (str.contains("not near") || str.contains("obstacle")) {
                move_to = true;
        } else if (str.contains("busy")) {
                menu_time = -1L;
        }
}

@Override
public void paint() {
        final int white = 0xFFFFFF;
        int y = 25;
        drawString("S Trade Giver: Momentum Version", 25, y, 1, white);
        y += 15;
        int num = item_ids.length;
        int totalitems = 0;
        for (int i = 0; i < num; ++i) {
                if ((start_count[i] - item_count[i]) <= 0) {
                        continue;
                }
                String perh = per_hour((start_count[i] - item_count[i]));
                drawString("Given " + getItemNameId(item_ids[i]) + ": " + int_format(start_count[i] - item_count[i]) + " (" + perh + " /hr)",
                           25, y, 1, white);
                totalitems += (start_count[i] - item_count[i]);
                y += 15;
        }
        if (totalitems > 0) {
                String tperh = per_hour(totalitems);
                drawString("Total Item Count: " + int_format(totalitems) + ": " + " (" + tperh + " /hr)",
                           25, y, 1, white);
                y+=15;
        }
}


@Override
public String getPlayerName(int local_index) {
        // did I seriously never fix this? fuck me.
        return super.getPlayerName(local_index)
               .replace((char) 160, ' ');
}

@Override
public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals("OK")) {
                try {
                        item_ids = new int[tFields + 1];
                        for (int idF = 0; idF < item_id_fields.size(); idF++) {
                                if (Integer.parseInt(item_id_fields.get(idF).getText().trim()) < 1290) {
                                        item_ids[idF] = Integer.parseInt(item_id_fields.get(idF).getText().trim());
                                } else {
                                        int badNum = idF + 1;
                                        System.out.println("Item #" + badNum + " with value " + item_id_fields.get(idF).getText().trim() + " is not an item ID");
                                        item_ids = new int[0];
                                        item_qty = new int[0];
                                }
                        }
                } catch (Throwable t) {
                        System.out.println("Couldn't parse item ids. " + t);
                        item_ids = new int[0];
                        item_qty = new int[0];
                }
                try {
                        item_qty = new int[tFields + 1];
                        for (int qtyF = 0; qtyF < item_qty_fields.size(); qtyF++) {
                                if (Integer.parseInt(item_qty_fields.get(qtyF).getText().trim()) > 30 || Integer.parseInt(item_qty_fields.get(qtyF).getText().trim()) == 0) {
                                        item_qty[qtyF] = Integer.parseInt(item_qty_fields.get(qtyF).getText().trim());
                                } else {
                                        System.out.println("Do not choose a quantity limit less than 30. The script won't function properly ");
                                        item_ids = new int[0];
                                        item_qty = new int[0];
                                }
                        }
                } catch (Throwable t) {
                        System.out.println("Couldn't parse item qtys. " + t);
                        item_ids = new int[0];
                        item_qty = new int[0];
                }
                try {
                        name = tf_username.getText().trim();
                } catch (Throwable t) {
                        System.out.println("Couldn't Username");
                        name = "";
                }
                start_count = new int[item_ids.length];
                item_count = new int[item_ids.length];
                itm_offered = new boolean[item_ids.length];
                frame.setVisible(false);
                System.out.print("Giving ");
                System.out.print(Arrays.toString(item_ids));
                System.out.print(" to ");
                System.out.println(name);
        }
        if (event.getActionCommand().equals("Cancel")) {
                frame.setVisible(false);
        }
        if (event.getActionCommand().equals("Add Item")) {
                tFields++;
                item_id_fields.add(new TextField());
                item_qty_fields.add(new TextField("0"));
                pInput.add(item_id_fields.get(tFields));
                pInput.add(item_qty_fields.get(tFields));
                frame.pack();
                frame.setSize(frameWidth,frame.getSize().height);
                frame.revalidate();
                frame.repaint();
        }
}

private String per_hour(int total)
{

        if (total == 0) return "0";
        double amount = total * 60.0 * 60.0;
        double secs = (System.currentTimeMillis() - start_time) / 1000.0;
        return int_format((long) (amount / secs));
}

private String int_format(long l)
{
        return int_format.format(l);
}

private String get_runtime()
{
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
