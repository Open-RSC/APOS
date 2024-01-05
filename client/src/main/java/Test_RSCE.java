import com.rsc.client.RSCFrame;

import javax.swing.*;
import java.awt.*;

import com.rsc.e;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.AnyTypePermission;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.runtime.typehandling.GroovyCastException;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Scanner;

public class Test_RSCE extends RSCFrame{
	public e player;
	public static e player_last_version;

	static XStream xstream = new XStream();
	static Object cloneMe(Object object) {
		Test_RSCE.xstream.addPermission(AnyTypePermission.ANY);
		String xml = xstream.toXML(object);
        return xstream.fromXML(xml);
	}

	public static void main(final String[] argv) {
		JFrame var2 = new JFrame("RSCEmulation");

		JButton testWalk = new JButton("Test Walk");

		Test_RSCE var1;
		(var1 = new Test_RSCE()).setPreferredSize(new Dimension(511, 342));
		var2.getContentPane().setLayout(new BorderLayout());
		var2.setDefaultCloseOperation(3);
		var2.setIconImage(Toolkit.getDefaultToolkit().getImage(com.rsc.a.a + File.separator + "RuneScape.png"));
		var2.getContentPane().add(var1, BorderLayout.CENTER);
		var2.getContentPane().add(testWalk, BorderLayout.EAST);


		var2.getContentPane().add(getConsoleWidget(var1), BorderLayout.SOUTH);
		var2.getContentPane().add(getInputWidget(var1), BorderLayout.NORTH);

		var2.setResizable(true);
		var2.setVisible(true);
		var2.setBackground(Color.black);
		var2.setMinimumSize(new Dimension(511, 342));
		var2.pack();
		var2.setLocationRelativeTo((Component)null);
		var1.init();
		var1.start();

		var1.player = getClass_a_e_static_var();
		String player_vars = get_class_a_e_variables_as_string(var1.player);
		//System.out.println(player_vars);
		testWalk.addActionListener(e -> {
			System.out.println("Testing walk, current x = " + var1.player.t);
			walk_to_coords(var1.player, 215, 450);
		});

        /*try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("player x = " + var1.player.t);*/

		Binding sharedInstance = new Binding();
		GroovyShell shell = new GroovyShell(sharedInstance);
		sharedInstance.setProperty("var1", var1);
		sharedInstance.setProperty("player_last_version", player_last_version);
		String script_line = "3*5";
		int mul = (int) shell.evaluate(script_line);
		System.out.println(mul);
		Scanner scan = new Scanner(System.in);
		while (true) {
			try {
				player_last_version = (com.rsc.e) cloneMe(var1.player);
				System.out.print("$groovy-shell: ");
				String java_code_as_string = scan.nextLine();
				//Object result = shell.evaluate(java_code_as_string);
				try {
					Script script = shell.parse(new File("client/src/main/java/gvy_test_rsce.groovy"));
					System.out.print("Loading Script...");
					script.run();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	static JPanel getInputWidget(Test_RSCE frame){
		JPanel setXYPanel = new JPanel();
		JLabel x_label = new JLabel("X: ");
		JTextField x = new JTextField("00000");

		JLabel y_label = new JLabel("Y: ");
		JTextField y = new JTextField("00000");
		JButton goToXY = new JButton("Go to XY");
		goToXY.addActionListener(e -> {
			try {
				int inputX = Integer.parseInt(x.getText());
				int inputY = Integer.parseInt(y.getText());
				System.out.println("Going to dest, X and Y = " + inputX + ", " + inputY);
				walk_to_coords(frame.player, inputX, inputY);
			} catch (RuntimeException ex) {
				throw new RuntimeException(ex);
			}
		});

		setXYPanel.add(x_label);
		setXYPanel.add(x);
		setXYPanel.add(y_label);
		setXYPanel.add(y);
		setXYPanel.add(goToXY);

		return setXYPanel;
	}
	 static JPanel getConsoleWidget(Test_RSCE frame){
		 String baseOut = "Console: \n";
		 JPanel console = new JPanel(new FlowLayout(FlowLayout.CENTER));
		 JButton getXY = new JButton("Get XY");
		 JTextArea consoleOut = new JTextArea("" + baseOut);
		 //  add xy button to panel
		 console.add(getXY);
		 console.add(consoleOut);

		 getXY.addActionListener(e -> {
			 int[] data = getPlayerCoords(frame.player);
			 //String currentOut = consoleOut.getText();
			 String newVal =baseOut + "Player Coords: \n";
			 newVal = newVal + "X: " + data[0] + "\n";
			 newVal = newVal + "Y: " + data[1] + "\n";
			 consoleOut.setText(newVal);
		 });

		 return  console;
	 }

	 static int getPlayerX(e player){
		return player.t;
	 }

	static int getPlayerY(e player){
		return player.u;
	}

	synchronized static int[] getPlayerCoords(e player){
		int[] coords = new int[2];
		coords[0] = getPlayerX(player);
		coords[1] = getPlayerY(player);
		return coords;
	}

	// To walk, use this in player(com.rsc.e) class
	// var10000.a(var10000.h, a.t, a.u);
	// eg path = Location("Edgeville", 215, 450, true),
	static synchronized void walk_to_coords(e player, int x, int y){
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
			player.t = x;
			player.u = y;
			player.h = 1;
			player.s = player.h;
			player.r = 0;
			player.a(player.h, player.t, player.u);
			//System.out.println("player_vars: " + player.t);
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
	}
	public static e getClass_a_e_static_var()
	{
		try
		{
			Class<?> forName = Class.forName("com.rsc.client.a");
			Field[] declaredFields = forName.getDeclaredFields();
			/*for(Field field: declaredFields)
			{
				String name = field.getName();
				System.out.println(name);
				Class<?> type = field.getType();
				System.out.println(type);
				field.setAccessible(true);
				Object object = field.get(type);
				System.out.println(object);
			}*/
			Field field = declaredFields[0];
			//System.out.println(field.getName());
			System.out.println(field.getType());
			field.setAccessible(true);
			e result = (e) field.get(field.getType());
			System.out.println(result);
			return result;

		}
		catch (Exception exp)
		{
			exp.printStackTrace();
		}
		return null;
	}

    static String get_class_a_e_variables_as_string(e obj) {
		try
		{
			StringBuffer buffer = new StringBuffer();
			Field[] fields = obj.getClass().getDeclaredFields();
			for (Field f : fields) {
				if (!Modifier.isStatic(f.getModifiers())) {
					f.setAccessible(true);
					Object value = f.get(obj);
					buffer.append(f.getType().getName());
					buffer.append(" ");
					buffer.append(f.getName());
					buffer.append("=");
					buffer.append("" + value);
					buffer.append("\n");
				}
			}
			return buffer.toString();

		}
		catch (Exception exp)
		{
			exp.printStackTrace();
		}
		return null;
	}

    public static JPanel getDemoUI(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		JLabel label = new JLabel("JFrame By Example");
		JButton button = new JButton();
		button.setText("Button");
		panel.add(label);
		panel.add(button);
		return panel;
	}

}
