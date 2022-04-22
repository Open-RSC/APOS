//Abyte0
//2012-01-25
//Make Oak,Willow,Yew,Magic Longbows

/*
To Cut tree + Cut Log + Bank Untrung
abyte0_fletch oak
abyte0_fletch willow
abyte0_fletch yew
abyte0_fletch magic

To Withdraw Untring Add String Bank Bows
abyte0_fletch o
abyte0_fletch w
abyte0_fletch y
abyte0_fletch m
*/

//2022-04-15 Version 1.0 - Added statistics -{- Require [Abyte0_Script 1.8.2.4+] -}-

public class Abyte0_Fletch extends Abyte0_Script 
{
	private final String SCRIPT_VERSION = "1.0";
	
	int oakTree = 306;
	int oakLog = 632;
	int oakLongBow = 648;
	int oakLongBowU = 658;
	
	int willowTree = 307;
	int willowLog = 633;
	int willowLongBow = 650;
	int willowLongBowU = 660;
	
	int yewTree = 309;
	int yewLog = 635;
	int yewLongBow = 654;
	int yewLongBowU = 664;
	
	int magicTree = 310;
	int magicLog = 636;
	int magicLongBow = 656;
	int magicLongBowU = 666;
	
	int bowString = 676;
	int knife = 13;
	
	@Override
	public String[] getRandomQuotes()
	{
		String[] result = {"Woodcut level?","Fletch level?","I wonder what is your fletching lvl?","Nice Magic Bow!","Rune wc axe is the best!","I should sharpen that axe","Shit I just cut my finger!","Look at that knife!","Fire level?","Do you bank logs or unstrung bow?","WOW you are a good lumberjack","Can't wait to be 99 woodcut","I wish fletching was slower xp..."};
		return result;
	}

	int xSpot,ySpot;
	
	boolean isCutting = true;
	int cuttingType; //0 = Oak,1=Willow,2=Yew,3=Magic
	
	int itemToDeposit;// = new int[]{};
	int itemToWithdraw;
	int treeId;
	int logId;
	
	int fightMode = 3;
	
	public Abyte0_Fletch(Extension e) {super(e);}    
	
	public void init(String param)
	{
		print("Seer Fletcher by Abyte0");
		print("Version " + SCRIPT_VERSION);
		//r4 = added Logging Bank
			
		itemToDeposit = yewLongBowU;
		cuttingType = 2;
		xSpot = 518;
		ySpot = 473;
		treeId = yewTree;
		logId = yewLog;
		isCutting = true;
			
		if(param.equals("yew"))
		{
			print("Doing Yew");
		}
		else if(param.equals("oak"))
		{
			print("Doing Oak");
			itemToDeposit = oakLongBowU;
			cuttingType = 0;
			xSpot = 511;
			ySpot = 444;
			treeId = oakTree;
			logId = oakLog;
			isCutting = true;
		}
		else if(param.equals("willow"))
		{
			print("Doing Willow");
			itemToDeposit = willowLongBowU;
			cuttingType = 1;
			xSpot = 511;
			ySpot = 444;
			treeId = willowTree;
			logId = willowLog;
			isCutting = true;
		}
		else if(param.equals("magic"))
		{
			print("Doing Magic");
			itemToDeposit = magicLongBowU;
			cuttingType = 3;
			xSpot = 521;
			ySpot = 491;
			treeId = magicTree;
			logId = magicLog;
			isCutting = true;
		}
		else if(param.equals("o"))
		{
			print("Adding String to Oaks");
			cuttingType = -1;
			itemToDeposit = oakLongBow;
			itemToWithdraw = oakLongBowU;
			isCutting = false;
		}
		else if(param.equals("w"))
		{
			print("Adding String to Willows");
			cuttingType = -1;
			itemToDeposit = willowLongBow;
			itemToWithdraw = willowLongBowU;
			isCutting = false;
		}
		else if(param.equals("y"))
		{
			print("Adding String to Yews");
			cuttingType = -1;
			itemToDeposit = yewLongBow;
			itemToWithdraw = yewLongBowU;
			isCutting = false;
		}
		else if(param.equals("m"))
		{
			print("Adding String to Magics");
			cuttingType = -1;
			itemToDeposit = magicLongBow;
			itemToWithdraw = magicLongBowU;
			isCutting = false;
		}
		else
		{
			print("Default = Yew LongBow");
		}
		
		
		resetCounters();
	}    
	
	public int main() 
	{        
		SayRandomQuote();
		if(getFightMode() != fightMode) 
		{            
			setFightMode(fightMode);            
			return 500;        
		}        
		if(getFatigue() > 85) 
		{
			useSleepingBag();            
			return 1000;
		}        
		if(isBanking())
		{
			if(getInventoryCount(itemToDeposit) > 0) 
			{
				deposit(itemToDeposit,getInventoryCount(itemToDeposit));
				return 1000;            
			}
			if(!isCutting)
			{
				if(getInventoryCount(bowString) < 14) 
				{                
					withdraw(bowString,14-getInventoryCount(bowString));                
					return 1500;            
				}
				else if(getInventoryCount(bowString) > 14) 
				{                
					deposit(bowString,getInventoryCount(bowString));                
					return 1000;            
				}
				if(getInventoryCount(itemToWithdraw) < 14) 
				{                
					withdraw(itemToWithdraw,14-getInventoryCount(itemToWithdraw));                
					return 1500;            
				}
				else if(getInventoryCount(itemToWithdraw) > 14) 
				{                
					deposit(itemToWithdraw,getInventoryCount(itemToWithdraw));                
					return 1000;            
				}
			}
			closeBank();  
			return 1500;
		}        
		if(isQuestMenu()) 
		{            
			if(getX() >= 498 && getX() <= 504 && getY() >= 447 && getY() <= 453) 
			{
				//si dans la banque
				answer(0);                
				return 1500;            
			}            
			else 
			{                
				answer(1);
				return 1500;				
			}        
		}
		
		if(!isCutting)
		{
			if(getInventoryCount(itemToWithdraw) == 0 || getInventoryCount(bowString) == 0)
			{
				return entreBanque();
			}
			else
			{
				useItemWithItem(getInventoryIndex(itemToWithdraw), getInventoryIndex(bowString));
				return random(500,600);
			}
		}
		else
		{
			if(getInventoryCount() < 30)
			{
				int banque = sortirBanque();
				if(banque != 0)
					return banque;
				if(isAtApproxCoords(xSpot, ySpot, 8))
				{
					//si on est au spot
					int[] tree = getObjectById(treeId);
					if(tree[0] != -1)
					{
						atObject(tree[1], tree[2]);
						return random(500,600);
					}
					else
					{
						int c = cut();
						if(c != 0)
							return c;
					}
				}
				else if(isAtApproxCoords(xSpot, ySpot, 30))
				{
					walkTo(xSpot,ySpot);
					return random(1000,3000);
				}
				else if(getY() < 465)
				{
					//between Buildings
					walkTo(510,465);
					return random(999,2000);
				}
				else
				{
					//near Corner Railling
					walkTo(513,479);
					return random(998,2000);
				}
			}
			else
			{
				//Si on veut aller a la banque
				//On cut en premier
				int c = cut();
				if(c != 0)
					return c;
				
				//ensuite on essaie de banquer
				int banque = entreBanque();
				if(banque != 0)
					return banque;
				
				//si on est trop loin on se rapproche
				if(getY() > 479)
				{
					//near Corner Railling
					walkTo(513,479);
					return random(998,2000);
				}
				else if(getY() > 465)
				{
					//between Buildings
					walkTo(511,465);
					return random(999,2000);
				}
				else
				{
					//near bank
					walkTo(500,454);
					return random(200,500);
				}
			}
		}
		return 1000;    
	}
	
	public int cut()
	{
		if(getInventoryCount(logId) > 0)
		{
			useItemWithItem(getInventoryIndex(knife), getInventoryIndex(logId));
			return random(500,600);
		}
		return 0;
	}
	
	
	public int sortirBanque()
	{
		if(getX() == 500 && getY() == 453)
		{
			//si devant porte on ouvre et on sort de la banque
			atObject(500,454);
			walkTo(500,454);
			return random(400, 2500);
		}
		if(getX() >= 498 && getX() <= 504 && getY() >= 447 && getY() <= 453)
		{
			//Si dans la banque
			//print("Walking to Door");	
			walkTo(500,453);
			return random(240, 2500);
		}
		return 0;
	}
	
	public int entreBanque()
	{
		if(getX() >= 498 && getX() <= 504 && getY() >= 447 && getY() <= 453)
		{
			//Si dans la banque
			int banker[] = getNpcByIdNotTalk(new int[]{95});
			if (banker[0] != -1 && !isBanking())
				talkToNpc(banker[0]);
			else
				print("No banker!"); 
			return random(240, 2500);
		}
		if(getX() == 500 && getY() == 454)
		{
			//si devant porte on ouvre et on entre dans la banque
			atObject(500,454);
			walkTo(500,453);
			return random(400, 2500);
		}
		return 0;
	}
	
	private void resetCounters()
	{
		initialWoodcuttingXp = getWoodcuttingXp();
		initialFletchingXp = getFletchingXp();
		initialTime = System.currentTimeMillis();
	}
	
	@Override
	public String getSctiptVersion()
	{
		return SCRIPT_VERSION;
	}
	
	@Override
	protected void printHelp()
	{
		super.printHelp();
		
		print("Commands parameter to cut: oak willow yew magic");
		print("Commands parameter to string: o w y m");
	}
	
	@Override
	protected void printParams()
	{
		print("Fmode is @or3@" + FIGHTMODES[fightMode]);
		
		if(cuttingType == -1)
			print("Adding string to Unstrung bow");
		else
			print("Cutting tree to bank unstrung");
		
		if(itemToDeposit == magicLongBow)
			print("Magic Bows");
		else if(itemToDeposit == yewLongBow)
			print("Yew Bows");
		else if(itemToDeposit == willowLongBow)
			print("Willow Bows");
		else if(itemToDeposit == oakLongBow)
			print("Oak Bows");
		
		if(treeId == magicTree)
			print("Magic Tree");
		else if(treeId == yewTree)
			print("Yew Tree");
		else if(treeId == willowTree)
			print("Willow Tree");
		else if(treeId == oakTree)
			print("Oak Tree");
	}
	
	@Override
	protected void reportXpChange()
	{
		
		int xpDifference1 = getWoodcuttingXp() - initialWoodcuttingXp;
		int xpDifference2 = getFletchingXp() - initialFletchingXp;
		long timeSpan = System.currentTimeMillis() - initialTime;
		long secondSpan = timeSpan / 1000;
		long xpRatio1 = xpDifference1 * 3600L / secondSpan; //The L set 3600 as long variable Forcing to calculate as long to avoid overflow
		long xpRatio2 = xpDifference2 * 3600L / secondSpan; //The L set 3600 as long variable Forcing to calculate as long to avoid overflow

		print("=================================");
		print("initial woodcutting Xp: " + initialWoodcuttingXp);
		print("initial fletchcing Xp: " + initialFletchingXp);
		print("@gre@total Woodcutting xp gained: " + xpDifference1 + " : " +xpRatio1 + "/h");
		print("@gre@total Fletchcing xp gained: " + xpDifference2 + " : " +xpRatio2 + "/h");
		print("time running: " + secondSpan + " s");
		print("=================================");
	}
	
    public void onKeyPress(int keyCode) {
		if (keyCode == 192 || keyCode == 128) { //# or '
			reportXpChange();
        }
		//if (keyCode == 107) { //+
		//	increaseDelay();
        //}
		//if (keyCode == 109) { //-
		//	decreaseDelay();
        //}
		if (keyCode == 113) { //F2
			resetCounters();
        }
		
		
		//print(""+keyCode);
    }
    
	int initialWoodcuttingXp = 0;
	int initialFletchingXp = 0;
	long initialTime = 0;
}