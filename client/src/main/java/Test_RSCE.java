import com.rsc.client.RSCFrame;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class Test_RSCE {

	public static void main(final String[] argv) {
		JFrame var2 = new JFrame("RSCEmulation");
		RSCFrame var1;
		(var1 = new RSCFrame()).setPreferredSize(new Dimension(511, 342));
		var2.getContentPane().setLayout(new BorderLayout());
		var2.setDefaultCloseOperation(3);
		var2.setIconImage(Toolkit.getDefaultToolkit().getImage(com.rsc.a.a + File.separator + "RuneScape.png"));
		var2.getContentPane().add(getDemoUI().add(var1));
		var2.setResizable(true);
		var2.setVisible(true);
		var2.setBackground(Color.black);
		var2.setMinimumSize(new Dimension(511, 342));
		var2.pack();
		var2.setLocationRelativeTo((Component)null);
		var1.init();
		var1.start();
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
