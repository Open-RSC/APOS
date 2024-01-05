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
    println Test_RSCE.player_last_version.t
    println player.t

}

void testDiff(com.rsc.e player) {
// Use DiffBuilder to compare the two objects
    def diff = DiffBuilder.compare(Test_RSCE.player_last_version).with(player).build()

// Get the differences
    DiffResult differences = diff.getResult()

// Print the differences
    println("Differences:")
    differences.differences.forEach { d ->
        println("${d.getFieldName()} - expected: ${d.getLeft()}, actual: ${d.getRight()}")
    }
}




