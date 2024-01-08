import org.apache.commons.lang3.builder.DiffBuilder
import org.apache.commons.lang3.builder.DiffResult

import javax.imageio.ImageIO
import java.awt.Graphics2D
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.lang.reflect.Method

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
    //def vars = Test_RSCE.get_class_a_e_variables_as_string(player)
    //println vars
    /*def myFile = new File('player_vars.txt')
    myFile.write(vars)*/
    //zoomIn(player)
    //zoomOut(player)
//    Class c = Class.forName("com.rsc.e");
//    Method m =c.getDeclaredMethod("a", new Class[]{int.class,int.class,int.class});
//    m.setAccessible(true);
//    m.invoke(player, 2, 250, 250);
    mouse_move_to_coords(player, 421, 251)
    //player.b(100,100)
    //println "set player down key"
    println Test_RSCE.getPlayerCoords(player)

}

static synchronized void walk_to_coords(com.rsc.e player, int x, int y){
    // actual source code
    /*a((InputEvent)var1);
    a.t = var1.getX() - a.v;
    a.u = var1.getY();
    if (!SwingUtilities.isRightMouseButton(var1)) {
        a.h = 1; // when left click is pressed, means to walk
    } else {
        a.h = 2;
    }

    e var10000 = a;
    var10000.s = var10000.h;
    a.r = 0;
    var10000 = a;
    var10000.a(var10000.h, a.t, a.u);*/
    try {
        player.g = false;
        player.w = false;
        player.t = x - player.v;
        player.u = y;
        player.h = 2;
        player.s = player.h;
        player.r = 0;
        //player.a(player.h, player.t, player.u);
        Class c = Class.forName("com.rsc.e");
        Method m =c.getDeclaredMethod("a", new Class[]{int.class,int.class,int.class});
        m.setAccessible(true);
        m.invoke(player, player.h, player.t, player.u);
        //System.out.println("player_vars: " + player.t);
    } catch (Exception exp) {
        throw new RuntimeException(exp);
    }
}
static synchronized void mouse_move_to_coords(com.rsc.e player, int x, int y){
    // actual source code
    /*a((InputEvent)var1);
    a.t = var1.getX() - a.v;
    a.u = var1.getY();
    if (!SwingUtilities.isRightMouseButton(var1)) {
        a.h = 1; // when left click is pressed, means to walk
    } else {
        a.h = 2;
    }

    e var10000 = a;
    var10000.s = var10000.h;
    a.r = 0;
    var10000 = a;
    var10000.a(var10000.h, a.t, a.u);*/

    // Success for mouse movement change on UI
    // emulate mouse movement to update objects details on left top corner in applet
    try {
        player.g = false;
        player.w = false;
        player.t = x - player.v;
        player.u = y;
        player.r = 0;
        player.h = 0;
        //System.out.println("player_vars: " + player.t);
    } catch (Exception exp) {
        //throw new RuntimeException(exp);
        exp.printStackTrace();
    }
}

static void zoomOut(com.rsc.e player){
    int downKey = KeyEvent.VK_DOWN;
    player.a((byte)126, downKey);
    player.r = 0;
    player.o = true
    // controls how fast it zooms, higher sleep time
    // more faster zoom out
    Thread.sleep(50)
    player.o = false
}

static void zoomIn(com.rsc.e player){
    int downKey = KeyEvent.VK_UP;
    player.a((byte)126, downKey);
    player.r = 0;
    player.n = true
    // controls how fast it zooms, higher sleep time
    // more faster zoom out
    Thread.sleep(50)
    player.n = false
}




