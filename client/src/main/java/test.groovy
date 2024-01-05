import java.nio.file.Paths

//player = ((Test_RSCE)var1).player
//println(player.t)

/*public static void Gprint(Object obj) {
    System.out.print("Done...");
    System.out.println("Execution Started..");
    System.out.println("-----------------------------------");
    System.out.println(obj);
    System.out.println("-----------------------------------");
}*/
//import static Util.*;
current_path = Paths.get("").toAbsolutePath().toString()
def tools = new GroovyScriptEngine( '.' ).with {
    loadScriptByName( 'test_util.groovy' )
}
this.metaClass.mixin tools
try {
    Demo demo = (Demo)var1;
    demo.s = "success-java-r-f";
    tools.Gprint(demo.s);
} catch (Exception e) {
    e.printStackTrace();
    System.out.println();
}

return

