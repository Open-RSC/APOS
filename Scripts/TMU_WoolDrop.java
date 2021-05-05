import java.awt.Point;

public class TMU_WoolDrop extends Script {

private WoolStage stage;
private boolean init;
private long time;
private int dropped;
private long startCraftXp;
private int startBag;
private int maxBag;
private int path_index;

// Sheep to Lumbridge castle
private final Point[] castle = {
        new Point(139, 629), new Point(133, 635), new Point(126, 642), new Point(118, 647), new Point(116, 658), new Point(127, 659)
};

// Castle to sheep
private final Point[] tosheep = {
   new Point(127, 659), new Point(116, 658),new Point(118, 647),  new Point(126, 642), new Point(133, 635), new Point(137, 630)
};

public TMU_WoolDrop(Extension e) {
        super(e);
}

public void init(String params) {
        writeLine("Start at the sheep or top of the tower");
        stage = WoolStage.GATHERING;
        init = false;
        dropped = 0;
        startCraftXp = getXpForLevel(12);
}

public int main() {
        if (!init) {
                time = System.currentTimeMillis();
                init = true;
                if (getY() > 2000){
                  maxBag = getInventoryCount(145) + getInventoryCount(207);
                  stage = WoolStage.SPINNING;
                }
        }
        if (isWalking())
                return 0;
        switch (stage) {
        case GATHERING:
                if (getInventoryCount() == 30) {
                        writeLine("Inventory full, spinning.");
                        maxBag = getInventoryCount(145);
                        startBag = 0;
                        stage = WoolStage.SPINNING;
                        path_index = 0;
                        return 0;
                }
                if (getX() > 215) {
                        int[] bank_doors = getObjectById(64);
                        if (bank_doors[0] != -1) {
                                atObject(bank_doors[1], bank_doors[2]);
                                return random(1000, 1500);
                        }
                        walkTo(213, 631);
                        return 1000;
                }
                // (135, 630) is the eastern gate
                if (distanceTo(135, 630) > 25) {
                        if (isAtApproxCoords(pathX(tosheep), pathY(tosheep), 2)) {
                                path_index++;
                        }
                        walkTo(pathX(tosheep) + random(-1, 1), pathY(tosheep) + random(-1, 1));
                        return random(600, 800);
                }
                int[] sheep = getNpcById(2);
                if (sheep[0] != -1) {
                        if (!isReachable(sheep[1], sheep[2])) {
                                checkGates();
                                return random(1000, 1300);
                        }
                        useOnNpc(sheep[0], getInventoryIndex(144));
                        return random(800, 900);
                }
                break;
        case SPINNING:
                if (getFatigue() >= 98) {
                        useSleepingBag();
                        return 1000;
                }
                if (getY() > 2000) {
                        if (!hasInventoryItem(145)) {
                                int ballCount = getInventoryCount(207);
                                if (ballCount > 0) {
                                        if (startBag <= maxBag){
                                          dropped++;
                                          startBag++;
                                        }
                                        dropItem(getInventoryIndex(207));
                                        return random(500,700);
                                }
                                writeLine("Out of wool, Going Back To Sheep.");
                                stage = WoolStage.TOSHEEP;
                                path_index = 0;
                                return 0;
                        }
                        useItemOnObject(145, 121);
                        return random(600, 800);
                }
                if (getY() > 1000) {
                        atObject(138, 1612);
                        return random(1000, 2000);
                }
                if (getY() < 656 || getX() < 125) {
                        if (isAtApproxCoords(pathX(castle), pathY(castle), 2)) {
                                path_index++;
                        }
                        int x = pathX(castle) + random(-1, 1);
                        int y = pathY(castle) + random(-1, 1);
                        if (!isReachable(x, y) && getY() < 634) {
                                checkGates();
                                return random(1000, 1500);
                        }
                        walkTo(x, y);
                        return random(600, 800);
                }
                int[] castle_doors = getObjectById(64);
                if (castle_doors[0] != -1) {
                        atObject(castle_doors[1], castle_doors[2]);
                        return random(1000, 1500);
                }
                atObject(139, 666);
                return random(1000, 2000);
        case TOSHEEP:
              if (distanceTo(137, 630) < 3) {
                writeLine("At Sheep, Switching to Gathering.");
                stage = WoolStage.GATHERING;
                path_index = 0;
                return 0;
                }
                if (getY() > 2000) {
                        atObject(138, 2556);
                        return random(1000, 2000);
                }
                if (getY() > 1000) {
                        atObject(139, 1610);
                        return random(1000, 2000);
                }
                if (getY() > 658) {
                        castle_doors = getObjectById(64);
                        if (castle_doors[0] != -1) {
                                atObject(castle_doors[1], castle_doors[2]);
                                return random(1000, 1500);
                        }
                        walkTo(127, 657);
                        return random(1000, 2000);
                }
                if (isAtApproxCoords(pathX(tosheep), pathY(tosheep), 2)) {
                        path_index++;
                }
                walkTo(pathX(tosheep) + random(-1, 1), pathY(tosheep) + random(-1, 1));
                return random(600, 800);
        }
        return random(150, 250);
}

public void paint() {
        long l = ((System.currentTimeMillis() - time) / 1000);
        long craftXP = getXpForLevel(12);
        long gainCraft = craftXP - startCraftXp;
        int y = 40;
        int x = 315;
        if (l > 0){
        drawBoxAlphaFill(315, y, 185, 110, 160, 0x000000);
        drawBoxOutline(315, y, 185, 110, 0xF7F7F7);
        y += 15;
        x += 3;
        drawString("Lumbridge Wool", x, y, 4, 0xFFFFFF);
        y += 10;
        drawHLine(x - 2, y, 183, 0xF7F7F7);
        y += 20;
        drawString("Time: " + getTimeRunning(), x, y, 1, 0xFFFFFF);
        y += 15;
        drawString("Dropped " + dropped + " balls of wool.", x, y, 1, 0xFFFFFF);
        y += 15;
        int perhr = (int)((long)(dropped * 3600) / l);
        drawString("Shearing " + perhr + " balls per hour.", x, y, 1, 0xFFFFFF);
        y += 15;
        drawString("Gained " + withSuffix(gainCraft) + " craft xp.", x, y, 1, 0xFFFFFF);
        y += 15;
        long xpPerHr = ((long)(gainCraft * 3600) / l);
        drawString("Getting " + withSuffix(xpPerHr) + " xp per hr.", x, y, 1, 0xFFFFFF);
      }

}

private static String withSuffix(long count) {
    if (count < 1000) return "" + count;
    int exp = (int) (Math.log(count) / Math.log(1000));
    return String.format("%.1f %c",
                         count / Math.pow(1000, exp),
                         "kMGTPE".charAt(exp-1));
}

private int pathX(Point[] path) {
        return (int) (path[path_index].getX());
}

private int pathY(Point[] path) {
        return (int) (path[path_index].getY());
}

private void checkGates() {
        int[] closed = getObjectById(60);
        if (closed[1] == 152 && closed[2] == 615) {
                writeLine("Opening gate at 152,615.");
                atObject(152, 615);
                return;
        }
        if (closed[1] == 135 && closed[2] == 630) {
                writeLine("Opening gate at 135,630.");
                atObject(135, 630);
                return;
        }
        writeLine("Blocked and no gates - wat do?");
        AutoLogin.setAutoLogin(false);
        logout();
}

private String getTimeRunning() {
        long l = ((System.currentTimeMillis() - time) / 1000);
        if (l > 3600) {
                int diff = (int)(l % 3600);
                int sec = diff % 60;
                int min = (diff - (diff % 60) / 60);
                int hr = (int)((l - diff) / 3600);
                return new String(hr + " hr " + min + " min " + sec + "sec.");
        }
        if (l > 60) {
                int sec = (int)(l % 60);
                int min = (int)((l - sec) / 60);
                return new String(min + " min" + sec + " sec.");
        }
        return new String(l + " seconds.");
}

private enum WoolStage {
        GATHERING, SPINNING, TOSHEEP
}
}
