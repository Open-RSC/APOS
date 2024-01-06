import org.apache.commons.lang3.builder.DiffBuilder
import org.apache.commons.lang3.builder.DiffResult

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
    def vars = Test_RSCE.get_class_a_e_variables_as_string(player)
//    def myFile = new File('player_vars.txt')
//    myFile.write(vars)
    //testDiff(player)
    println player.t

}




