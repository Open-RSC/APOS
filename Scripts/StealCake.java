//2021-06-24 Update by Abyte0 : "xp" parameter will not bank, else banking
/** * This script is used to help you gain theiving levels. * 
* if you run the script with the parameter "bank" * all the cakes you thieve will be deposited into * your bank. otherwise you will only be power training. *
 * the script will run if you're in combat and will eat * cakes if needed. * * change your fight mode if you don't want str... *  * v 1.0 * *	- yomama` */
 public class StealCake extends Script
 {	
	int fmode = 2;
	int stallID = 322;	
	int[] bakeStart1 = new int[]{544, 601};	
	int[] bakeStart2 = new int[]{543, 600};	
	int[] bankArea = new int[]{551, 612};	
	boolean power = false;
	
	public StealCake(Extension e) {		super(e);	}
	
	public void init(String params)
	{
		System.out.println("USAGE: default will bank, if param is `xp` it will not bank");
		System.out.println("Validate the fight mode, set=" + fmode);
		if(params.equals("xp"))
			power = true;
	}

	public int main()
	{
		if(getFightMode() != fmode)
			setFightMode(fmode);
		if(getFatigue() > 90)
		{
			useSleepingBag();
			return 1000;
		}	
		if(inCombat())
		{
			if(getY() == bakeStart2[1])
				walkTo(bakeStart1[0], bakeStart1[1]);
			else
				walkTo(bakeStart2[0], bakeStart2[1]);
			return random(500,600);
		}
	
		if(getHpPercent() < 80)
		{
			int idx = getInventoryIndex(335);
			if(idx == -1)
				idx = getInventoryIndex(333);
			if(idx == -1)
				idx = getInventoryIndex(330);
			if(idx == -1)
			{
				System.out.println("hp is dangerously low with no food.");
				stopScript();
				return 0;
			}	
			useItem(idx);
			return random(500, 600); 
		}
		if(isBanking())
		{
			if(getInventoryCount() != 30)
			{ 
				closeBank();
				return random(500, 600);
			}
		
			deposit(330, getInventoryCount(330) - 2);
			return random(500, 600);
		}
		if(isQuestMenu())
		{
			answer(0);
			return random(500, 600);
		}
		if(getInventoryCount() != 30 && !power)
		{
			if(getX()!=543 && getX()!=600)
			{
				walkTo(543,600);
				return 2000;
			}
			int[] stall = getObjectById(stallID);
			if( stall[0] != -1 )
			{
				atObject2(stall[1], stall[2]);
			}
			return 1000+random(500, 733);
		}
		if(distanceTo(bankArea[0], bankArea[1]) < 10)
		{
			int banker[] = getNpcByIdNotTalk(95);
			if(banker[0] != -1)
				talkToNpc(banker[0]);
	        return random(1222,3000);
		}
		else
			walkTo(bankArea[0], bankArea[1]);
	 
		return random(500, 1000);
	}
 }