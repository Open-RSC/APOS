import javax.swing.*;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.io.File;

public class Test_UI_Event extends Applet implements ComponentListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener, ImageObserver, ImageProducer {

	static void Gprint(Object obj) {
		System.out.println("Execution Started..");
		System.out.println("-----------------------------------");
		System.out.println(obj);
		System.out.println("-----------------------------------");
	}

	@Override
	public void init() {
		addKeyListener(this);
		addMouseListener(this);
		addComponentListener(this);
		setFocusable(true);
		requestFocusInWindow();
	}
	public static void main(final String[] argv) {
		Test_UI_Event var1;
		(var1 = new Test_UI_Event()).setPreferredSize(new Dimension(511, 342));
		JFrame var2 = new JFrame("Test Events");
		JButton testWalk = new JButton("Test Walk");
		//var2.getContentPane().add(testWalk, BorderLayout.EAST);

		var2.getContentPane().setLayout(new BorderLayout());
		var2.setDefaultCloseOperation(3);
		var2.setIconImage(Toolkit.getDefaultToolkit().getImage(com.rsc.a.a + File.separator + "RuneScape.png"));
		var2.getContentPane().add(var1, BorderLayout.CENTER);
		var2.getContentPane().add(testWalk, BorderLayout.EAST);
		var2.setResizable(true);
		var2.setVisible(true);
		var2.setBackground(Color.black);
		var2.setMinimumSize(new Dimension(511, 342));
		var2.pack();
		var2.setLocationRelativeTo((Component)null);
		var1.init();
		var1.start();
	}

	@Override
	public void componentResized(ComponentEvent e) {

	}

	@Override
	public void componentMoved(ComponentEvent e) {

	}

	@Override
	public void componentShown(ComponentEvent e) {

	}

	@Override
	public void componentHidden(ComponentEvent e) {

	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {
		Gprint("KeyEvent Char: " + e.getKeyChar());
		Gprint("KeyEvent Code: " + e.getKeyCode());
		e.setKeyChar((char) KeyEvent.VK_DOWN);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		//Gprint("KeyEvent: " + e.getKeyCode());
	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {
		Gprint("MouseEvent: " + e.getX());
	}

	@Override
	public void mouseReleased(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void mouseDragged(MouseEvent e) {

	}

	@Override
	public void mouseMoved(MouseEvent e) {

	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {

	}

	@Override
	public void addConsumer(ImageConsumer ic) {

	}

	@Override
	public boolean isConsumer(ImageConsumer ic) {
		return false;
	}

	@Override
	public void removeConsumer(ImageConsumer ic) {

	}

	@Override
	public void startProduction(ImageConsumer ic) {

	}

	@Override
	public void requestTopDownLeftRightResend(ImageConsumer ic) {

	}
}
