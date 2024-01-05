import static Util.*;

static void Main(Object arg) {
    try {
        arg;
    } catch (Exception e) {
        e.printStackTrace();
        System.out.println();
    }
}

void test() {
    def player = ((Test_RSCE)var1).player
    Gprint(player.t);
}

Main(test())
