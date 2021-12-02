//2021-12-01 - Initial Release by Abyte0
public class Abyte0_HideNSeekGiver extends Abyte0_Script {
    // ===== DEFAULT CONFIG ===== //
    String reminderText = "@cya@Hide N Seek game! @or3@Be the first to trade me!";
    int relogRequiredToAdvertise = 10;
    // === END DEFAULT CONFIG === //


    boolean isTrading = false;
    boolean isSucess = false;
    String privateMessageTarget = "Global$";
    String yourName = "";
    int idToGive = 0;
    int amoutToGive = 0;
    String prizeDescription = "";
    String lastOpponent = "";
    int relogCounter = 0;
    boolean isInDebug = false;
    boolean readyToAdvertise = false;
    boolean needToAdvertise = false;
    String startTime = "";
    boolean startedFirstIteration = false;

    @Override
    public String[] getRandomQuotes() {
        int doubleDice = random(1, 10);
        if (doubleDice > 5) {
            String[] temp = {""};
            return temp;
        }

        String[] result = {"Where am I?", "Nobody gona know!", "Pisst! I am here!", "5 - 4 - 3 - 2 - 1 Go! Find me!"};
        return result;
    }

    public Abyte0_HideNSeekGiver(Extension e) {
        super(e);
    }

    public void init(String params) {
        print("Version 0.2 - Initial Release");
        print("param order idToGive,amoutToGive,Prize description no comma,your name");

        if (params.equals("debug")) {
            isInDebug = true;
            idToGive = 10;
            amoutToGive = 1;
            prizeDescription = "1 coins";
            yourName = "Abyte0";
            relogRequiredToAdvertise = 2;

            privateMessageTarget = "Abytetest";

        } else {
            String[] str = params.split(",");
            if (!params.equals("")) {
                idToGive = Integer.parseInt(str[0]);
                amoutToGive = Integer.parseInt(str[1]);
                prizeDescription = str[2];
                yourName = str[3];

                if (getInventoryCount(idToGive) < amoutToGive) {
                    print("invalid quantity, you can't give what is not in your inventory");
                    stopScript();
                    return;
                }

            } else {
                print("example of param : 10,100000,100 000 coins,Abyte");
                stopScript();
                return;
            }

        }

        if (isInDebug)
            print("@or2@SET AS DEBUG");

    }

    public int main() {
        if (!startedFirstIteration) {

            startTime = getDateTime();
            print("Script started on " + startTime);

            if ("".equals(yourName))
                sendPrivateMessage("@ran@Game Start! : @cya@" + reminderText, privateMessageTarget);
            else
                sendPrivateMessage("@ran@" + yourName + " just started :@cya@ " + reminderText, privateMessageTarget);

            startedFirstIteration = true;
        }

        if (isInDebug) print("needToAdvertise = " + needToAdvertise);
        if (isInDebug) print("readyToAdvertise = " + readyToAdvertise);

        if (readyToAdvertise) {
            if (isInDebug) print("reminderText = " + reminderText);
            if (isInDebug) print("privateMessageTarget = " + privateMessageTarget);

            sendPrivateMessage(reminderText, privateMessageTarget);

            needToAdvertise = false;
            readyToAdvertise = false;
            return 1000;
        }
        if (needToAdvertise) {
            if (isInDebug)
                print("Scheduling an advertisement in 30 seconds!");
            readyToAdvertise = true;
            if (isInDebug) print("readyToAdvertise = " + readyToAdvertise);
            return 30000;
        }

        if (isSucess) {
            String successText = "@mag@[@gre@" + lastOpponent + "@mag@]@cya@Just won @red@" + prizeDescription + "@cya@ at the Hide N Seek game!";

            sendPrivateMessage(successText, privateMessageTarget);

            print(getDateTime() + " Stopping script because " + lastOpponent + " won!");

            print("it has started on : " + startTime);

            setAutoLogin(false);
            logout();
            stopScript();
        }

        if (isInTradeOffer()) {
            if (getLocalTradeItemCount() > 0) {
                acceptTrade();
                return 1000;
            }

            int itemIventoryPosition = getInventoryIndex(idToGive);
            offerItemTrade(itemIventoryPosition, amoutToGive);
            return 2000;
        }

        if (isInTradeConfirm()) {
            confirmTrade();
            return 1000;
        }

        return 10000;
    }

    @Override
    public void onTradeRequest(String name) {
        System.out.println(name + " wishes to trade with you.");

        lastOpponent = name;

        int[] opDetails = getPlayerByName(name);
        Object player = client.getPlayer(opDetails[0]);

        if (!isTrading)
            sendTradeRequest(client.getMobServerIndex(player));

        isTrading = true;
    }

    @Override
    public void onServerMessage(String s) {

        if (s.contains("declined trade")) {
            isTrading = false;

            if (isInDebug) print("isTrading is now false due to decline");

            return;
        }
        if (s.contains("Welcome to ")) {

            relogCounter++;

            if (isInDebug) print("counter is " + relogCounter + " of " + relogRequiredToAdvertise);

            isTrading = false;
            if (relogCounter >= relogRequiredToAdvertise) {
                needToAdvertise = true;
                print("counter going back to zero");
                relogCounter = 0;

                return;
            }

            return;
        }
        if (s.contains("Trade completed ")) {
            isSucess = true;

            if (isInDebug)
                print("isSucess is now true due to trade completed");

            return;
        }
        //if (s.contains("Closing welcome box.")) {
        //
        //}
        //if (s.contains("standing here for 5 mins!")) {
        //
        //}

    }

}

