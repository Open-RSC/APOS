//player = ((Test_RSCE)var1).player
//println(player.t)
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
    Demo demo = (Demo) var1;
    demo.s = "success-java-r-f";
    Gprint(demo.s);
}

Main(test())
