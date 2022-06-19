/**
 * This script is used to cook your food in the desert. *
 * start at alkarhid bank, pick the ores you wanna smelt * into bars, make sure you only have a sleeping bag * in your inventory... and have fun... * * * v 1.0 * *   - yomama`
 */
public class AlKharidSmelter extends Script {
    int[] smeltArea = new int[]{82, 679};
    int[] bankArea = new int[]{85, 694};
    int[] withdraw1 = null;
    int[] withdraw2 = null;
    int barID = 0;

    public AlKharidSmelter(Extension e) {
        super(e);
    }

    public void init(String params) {
        if (params.equalsIgnoreCase("bronze")) {
            withdraw1 = new int[]{202, 14}; // 14 tin
            withdraw2 = new int[]{150, 14}; // 14 copper
            barID = 169;
        } else if (params.equalsIgnoreCase("iron")) {
            withdraw1 = new int[]{151, 29}; // 29 iron
            withdraw2 = new int[]{-1, 0};
            barID = 170;
        } else if (params.equalsIgnoreCase("silver")) {
            withdraw1 = new int[]{383, 29}; // 29 silver
            withdraw2 = new int[]{-1, 0};
            barID = 384;
        } else if (params.equalsIgnoreCase("steel")) {
            withdraw1 = new int[]{155, 18}; // 18 coal
            withdraw2 = new int[]{151, 9}; //  9 iron
            barID = 171;
        } else if (params.equalsIgnoreCase("gold")) {
            withdraw1 = new int[]{152, 29}; // 29 gold
            withdraw2 = new int[]{-1, 0};
            barID = 172;
        } else if (params.equalsIgnoreCase("mith")) {
            withdraw1 = new int[]{153, 5}; //  5 mith
            withdraw2 = new int[]{155, 20}; // 20 coal
            barID = 173;
        } else if (params.equalsIgnoreCase("adam")) {
            withdraw1 = new int[]{150, 4}; //  4 adam
            withdraw2 = new int[]{155, 24}; // 24 coal
            barID = 174;
        } else if (params.equalsIgnoreCase("rune")) {
            withdraw1 = new int[]{409, 3}; //  3 rune
            withdraw2 = new int[]{155, 24}; // 24 coal
            barID = 408;
        } else {
            System.out.println("Invalid parameters... instead try: " + "\n\tbronze\n\tiron\n\tsilver\n\tsteel\n\tgold\n\tmith\n\tadam\n\trune");
            stopScript();
        }
    }

    public int main() {
        if (getFatigue() > 90) {
            useSleepingBag();
            return 1000;
        }
        if (isBanking()) {
            if (getInventoryCount(barID) != 0) {
                deposit(barID, getInventoryCount(barID));
                return random(500, 600);
            }
            if (getInventoryCount() == (1 + withdraw1[1] + withdraw2[1])) {
                closeBank();
                return random(500, 600);
            }
            if (getInventoryCount(withdraw1[0]) == 0)
                withdraw(withdraw1[0], withdraw1[1]);
            if ((withdraw2[0] != -1) && getInventoryCount(withdraw2[0]) == 0)
                withdraw(withdraw2[0], withdraw2[1]);
            return random(500, 600);
        }
        if (isQuestMenu()) {
            answer(0);
            return random(500, 600);
        }
        if (getInventoryCount(withdraw1[0]) != 0) {
            if (distanceTo(smeltArea[0], smeltArea[1]) < 10) {
                useItemOnObject(withdraw1[0], 118);
                return random(500, 600);
            }
            walkTo(smeltArea[0], smeltArea[1]);
            return random(500, 600);
        }
        if (isAtApproxCoords(86, 695, 20)) {
            int[] bankGate = getObjectById(64);
            if (bankGate[0] != -1) {
                atObject(bankGate[1], bankGate[2]);
                return random(200, 300);
            }
        }
        if (distanceTo(bankArea[0], bankArea[1]) < 10) {
            int[] banker = getNpcByIdNotTalk(BANKERS);
            if (banker[0] != -1)
                talkToNpc(banker[0]);
            return 1000;
        } else
            walkTo(bankArea[0], bankArea[1]);
        return random(500, 1000);
    }
}
