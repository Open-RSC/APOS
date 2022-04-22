/*
Paid & Share By: Angjulak
Made By: Abyte0
2012-01-28 - Version 0
*/

public class Abyte0_Agility extends Script
{
    int fmode = 3;
    int sleepAt = 90;

    boolean walkBack = false;
	boolean bury = true;

    public Abyte0_Agility (Extension e) {
        super(e);
    }

    public void init(String param) {

	System.out.println("Abyte0_Agility");
	System.out.println("Version 0");
	System.out.println("USAGE = abyte0_agility fmode");
	System.out.println("-");
	
	if(!param.equals(""))
		fmode = Integer.parseInt(param);
	
	if(fmode < 0 || fmode > 3) {
		System.out.println("No fight mode set, type the number as params 0=controled,1=str,2=attack,3=def");
		stopScript();
	}
		
	System.out.println("= - - - - - - - - - - - - - - - - - - - - - - - - - - =");
	System.out.println("Paid & Share By: Angjulak Thanks Him!");
	System.out.println("Paid & Share By: Angjulak Thanks Him!");
	System.out.println("= - - - - - - - - - - - - - - - - - - - - - - - - - - =");
    }

    public int main()
	{
        if(getFightMode() != fmode) {
            setFightMode(fmode);
            return random(200, 300);
        }
        if(getFatigue() >= sleepAt) {
            useSleepingBag();
            return 1;
        }
        int[] wall = getWallObjectById(163);
        if(wall[0] != -1)
        {
			atWallObject(wall[1], wall[2]);
			return random(200,250);
        }
		return random(100,150);
    }
}