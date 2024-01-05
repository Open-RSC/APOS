import com.thoughtworks.xstream.security.AnyTypePermission;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.GroovyException;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.runtime.typehandling.GroovyCastException;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class TestGroovy {

	public static void main(final String[] argv) {
		//runMe();
		testLib();
	}

	static void testLib(){
		Demo x = new Demo();
		x.s = "cloned";
		Demo y = (Demo) Test_RSCE.cloneMe(x);
		x.s = "original";
		System.out.println("I am " + y.s);
		System.out.println("I am " + x.s);
	}

	private static void runMe() {
		Binding sharedInstance = new Binding();
		GroovyShell shell = new GroovyShell(sharedInstance);
		Demo var1 = new Demo();
		sharedInstance.setProperty("var1", var1);
		Scanner scan = new Scanner(System.in);
		while (true) {
			try {
				System.out.print("$groovy-shell: ");
				String java_code_as_string = scan.nextLine();
				//Object result = shell.evaluate(java_code_as_string);
				try {
					Script script = shell.parse(new File("client/src/main/java/test.groovy"));
					System.out.print("Loading Script...");
					script.run();
				} catch (GroovyCastException | IOException e) {
					e.printStackTrace();
				}
            } catch (Exception e) {
				e.printStackTrace();
			}

        }
	}

}

class Demo{
	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public String getS() {
		return s;
	}

	public void setS(String s) {
		this.s = s;
	}

	int x = 0;
	String s = "";
}
