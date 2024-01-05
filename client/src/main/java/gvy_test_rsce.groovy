import static Util.*;

static void Main(Object arg) {
    try {
        System.out.print("Done...");
        System.out.println("Execution Started..");
        System.out.println("-----------------------------------");
        arg;
        System.out.println("-----------------------------------");

    } catch (Exception e) {
        e.printStackTrace();
        System.out.println();
    }
}

Main(test())

void test() {
    def player = ((Test_RSCE)var1).player
    //println(player.u);
    Test_RSCE.walk_to_coords(player, 300, 400)
}


