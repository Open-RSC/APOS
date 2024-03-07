import java.util.Arrays;
import java.util.Locale;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.text.DecimalFormat;

public final class MomentumUniversalAcceptor extends Script {

private int[] itm;
private int[] itm_count;
private boolean[] itm_banked;
private int shantywalk = 0;
private String name;
private long bank_time;
private long menu_time;
private boolean move_to;
private int[]  player;
private ArrayList<Integer> itemcount = new ArrayList<>();
private int player_id;
private int lastiid;
private boolean everybank = false;
public int[] lastplayer;
private long time;
private long start_time;

private final DecimalFormat int_format = new DecimalFormat("#,##0");

public MomentumUniversalAcceptor(Extension ex) {
        super(ex);
}

@Override
public void init(String params) {
        Object[] voptions = {"Bank Everything", "Selective Bank"};
        String bank_catch = (String)JOptionPane.showInputDialog(null, "Selective Bank?(did you put params?)", "The Momentum's Universal Acceptor", JOptionPane.PLAIN_MESSAGE, null, voptions, voptions[0]);
        if (bank_catch.equals("Selective Bank")) {

                try {
                        if (params == null || params.isEmpty()) {
                                throw new Exception();
                        }
                        String[] split = params.split(",");
                        int len = split.length;
                        if (len == 1) throw new Exception();
                        itm = new int[len];
                        itm_count = new int[len];
                        itm_banked = new boolean[len];
                        for (int i = 0; i < len; ++i) {
                                itm[i] = Integer.parseInt(split[i]);
                        }
                } catch (Throwable t) {
                        System.out.println("ERROR: Failed to parse parameters.");
                        System.out.println("Example: itemid1,id2,id3...");
                        return;
                }
                menu_time = -1L;
                bank_time = -1L;
                move_to = false;
                System.out.print("Taking ");
                System.out.print(Arrays.toString(itm));
                System.out.print(" from everyone.");
        } else {
                menu_time = -1L;
                bank_time = -1L;
                move_to = false;
                everybank = true;
                for (int ii =0; ii <= 1289; ii++) {
                        itemcount.add(0);
                }
                System.out.print("Taking everything");
                System.out.print(" from EVERYONE.");
        }
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
        if (isBanking() && !everybank) {
                bank_time = -1L;
                int itm_sz = itm.length;
                for (int i = 0; i < itm_sz; ++i) {
                        int id = itm[i];
                        int count = getInventoryCount(id);
                        if (count > 0) {
                                if (!itm_banked[i]) {
                                        itm_count[i] += count;
                                        itm_banked[i] = true;
                                }
                                deposit(id, count);
                                return 0;
                        } else {

                                closeBank();
                                return random(600, 800);
                        }
                }
        } else if (isBanking() && everybank) {
                bank_time = -1L;
                int lastslot = 29 - getEmptySlots();
                if (lastslot >= 0) {
                        int iid = getInventoryId(lastslot);

                        int icount = getInventoryCount(iid);
                        if (icount > 0) {
                                if (itemcount.size() > 1) {
                                        if ( itemcount.get(iid) != null && lastiid != iid) {
                                                itemcount.set(iid, (itemcount.get(iid) + icount));
                                        }
                                } else {
                                        if (lastiid != iid) {
                                                itemcount.set(iid, (icount));
                                        }
                                }
                                deposit(iid, icount);
                                lastiid = iid;
                                return 0;
                        } else {
                                closeBank();
                                return random(600, 800);
                        }
                } else {
                        closeBank();
                        return random(600, 800);
                }
        }

        if (bank_time != -1L) {
                if (System.currentTimeMillis() >= (bank_time + 8000L)) {
                        bank_time = -1L;
                }
                return random(300, 400);
        }
	 if (getX() > 0 && getX() < 100 && getY() > 700 && getY() < 800){
                if (shantywalk > 30){
                walkTo(61,730);
                shantywalk = 0;
                } else {
                shantywalk++;
                }
	}
        if (getInventoryCount() > 18) {
                int[] banker = getNpcByIdNotTalk(BANKERS);
                if (getX() > 0 && getX() < 100 && getY() > 700 && getY() < 800) {
                        atObject(58,731);
                        return random(1700, 1800);
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
                if (System.currentTimeMillis() - time > 30000) {
                        declineTrade();
                        return random(1000,2000);
                }
                confirmTrade();
                return random(1000, 2000);
        }
        if (isInTradeOffer()) {
                if (System.currentTimeMillis() - time > 30000) {
                        declineTrade();
                        return random(1000,2000);
                }
                acceptTrade();
                return random(1000, 2000);
        }
        if (!isWalking()) {
                if (move_to) {
                        walkTo(lastplayer[1], lastplayer[2]);
                        move_to = false;
                } else {
                        if (!everybank) {
                                Arrays.fill(itm_banked, false);
                        }
                        return random(2000, 3000);
                }
        }
        return random(1000, 2000);
}

@Override
public void onServerMessage(String str) {
        str = str.toLowerCase(Locale.ENGLISH);
        if (str.contains("nnt near") || str.contains("obstacle")) {
                move_to = true;
        } else if (str.contains("busy")) {
                menu_time = -1L;
        }
        if (str.contains("welcome to")){
          if (isInTradeConfirm() || isInTradeOffer()){
            declineTrade();
          }
        }
}

@Override
public void paint() {
        final int white = 0xFFFFFF;
        int y = 25;
        drawString("Momentum Universal Acceptor", 25, y, 1, white);
        y += 15;
        if (!everybank) {
                int num = itm.length;
                int totalitems = 0;
                for (int i = 0; i < num; ++i) {
                        if (itm_count[i] <= 0) {
                                continue;
                        }
                        String perh = per_hour(itm_count[i]);
                        drawString("Banked " + getItemNameId(itm[i]) + ": " + itm_count[i] + " (" + perh + " /hr)",
                                   25, y, 1, white);
                        totalitems += itm_count[i];
                        y += 15;
                }
                if (totalitems > 0){
                String tperh = per_hour(totalitems);
                drawString("Total Item Count: " + totalitems + ": " + " (" + tperh + " /hr)",
                           25, y, 1, white);
                           y+=15;
                }
        } else {

                if (itemcount.size() > 0) {
                        int num = itemcount.size();
                        int totalitems = 0;
                        for (int i = 0; i < num; i++ ) {

                                if (itemcount.get(i) <= 0) {
                                        continue;
                                }
                                String perh = per_hour(itemcount.get(i));
                                drawString("Banked " + getItemNameId(i) + ": " + int_format(itemcount.get(i)) + " (" + perh + " /hr)", 25, y, 1, white);
                                totalitems += itemcount.get(i);
                                y += 15;
                        }
                        if (totalitems > 0){
                        String tperh = per_hour(totalitems);
                        drawString("Total Item Count: " + int_format(totalitems) + ": " + " (" + tperh + " /hr)",
                                   25, y, 1, white);
                                   y+=15;
                        }
                }
        }
}


//When trade request received
public void onTradeRequest(String name)
{
        if (!isBanking() && !isInTradeConfirm() && !isInTradeOffer() && !isQuestMenu()) {
                player = getPlayerByName(name);
                player_id = getPlayerPID(player[0]);
                lastplayer = getPlayerByName(name);
                lastiid = -1;
                if (getInventoryCount() < 24) {
                        time = System.currentTimeMillis();
                        sendTradeRequest(player_id);
                }
        }
}

@Override
public String getPlayerName(int local_index) {
        // did I seriously never fix this? fuck me.
        return super.getPlayerName(local_index)
               .replace((char) 160, ' ');
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
