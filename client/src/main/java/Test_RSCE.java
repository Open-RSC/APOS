import com.rsc.client.RSCFrame;

import javax.swing.*;
import java.awt.*;

import com.rsc.e;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Test_RSCE {

	public static void main(final String[] argv) {
		JFrame var2 = new JFrame("RSCEmulation");
		RSCFrame var1;
		(var1 = new RSCFrame()).setPreferredSize(new Dimension(511, 342));
		var2.getContentPane().setLayout(new BorderLayout());
		var2.setDefaultCloseOperation(3);
		var2.setIconImage(Toolkit.getDefaultToolkit().getImage(com.rsc.a.a + File.separator + "RuneScape.png"));
		var2.getContentPane().add(var1);
		var2.setResizable(true);
		var2.setVisible(true);
		var2.setBackground(Color.black);
		var2.setMinimumSize(new Dimension(511, 342));
		var2.pack();
		var2.setLocationRelativeTo((Component)null);
		var1.init();
		//var1.start();
		e player = getClass_a_e_static_var();
		String player_vars = get_class_a_e_variables_as_string(player);
		System.out.println(player_vars);
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
