import com.rsc.client.RSCFrame;

import javax.swing.*;
import java.awt.*;

import com.rsc.e;

import java.awt.event.InputEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Test_RSCE extends RSCFrame{
	public e player;
	public static void main(final String[] argv) {
		JFrame var2 = new JFrame("RSCEmulation");
		JButton testWalk = new JButton("Test Walk");
		Test_RSCE var1;
		(var1 = new Test_RSCE()).setPreferredSize(new Dimension(511, 342));
		var2.getContentPane().setLayout(new BorderLayout());
		var2.setDefaultCloseOperation(3);
		var2.setIconImage(Toolkit.getDefaultToolkit().getImage(com.rsc.a.a + File.separator + "RuneScape.png"));
		var2.getContentPane().add(var1, BorderLayout.CENTER);
		var2.getContentPane().add(testWalk, BorderLayout.SOUTH);
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
			walk_to_edgeville(var1.player);
		});

        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("player x = " + var1.player.t);



	}

	// To walk, use this in player(com.rsc.e) class
	// var10000.a(var10000.h, a.t, a.u);
	// eg path = Location("Edgeville", 215, 450, true),
	static synchronized void walk_to_edgeville(e player){
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
			player.t = 215;
			player.u = 450;
			player.h = 1;
			player.s = player.h;
			player.r = 0;
			player.a(player.h, player.t, player.u);
			System.out.println("player_vars: " + player.t);
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
