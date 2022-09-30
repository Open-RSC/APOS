/** * Based on Any Thiever from yomama` *//** 
*  pickpockets anything, eats/banks (cakes) * 
*	- yomama` */
//Version 5.0 Updated to OpenRSC 2021-06-29
//Version 5.1 Support Hero
//Version 6.0 2022-01-28 [Require Abyte0_Script 1.7.3+] Auto move + commands added
//Version 6.1 - 2022-02-14 - Updated to work with abyte0_Script 1.8  [require abyte0_Script 1.8+]
public class Abyte0_ArdThiever extends Abyte0_Script 
{
	public String SCRIPT_VERSION = "6.1.1";
	
	int MAX_INVENTORY_SIZE = 30;
	int freeInventrySlotsRequired = 22;
	int fightMode = 3;
	
	@Override
	public String[] getRandomQuotes()
	{
		String[] result = {"Thieving level?",
		"Once a thief, always a thief",
		"give me that wallet!",
		"Wanna buy a watch?",
		"I stole a million from that guy over there!",
		"I stole his keys, kill him you'll see he wont drop any!",
		"Money!",
		"Who dropped that orb?",
		"Press 666 if you dont bot",
		"Drop party?"};
		return result;
	}

	int goldId = 152;
	int chaosId = 41;
	int deathId = 38;
	int bloodId = 619;
	int wineId = 142;
	int diamonId = 161;
	
	int[] npcIDs = new int[]
	{ 
		324
	};		
	int[] dropIDs = new int[]
	{		
		140, //jug		
		612, //fire orb		
		714, //lockpick		
		559 //Poisoned Iron dagger
	};
	int[] foodIDs = new int[] 
	{				
		330, //cake 3/3
		333, //cake 2/3
		335, //cake 1/3
		895, //Swamp Toad		
		897, //King worm		
		138, //bread		
		wineId  //wine	
	};
	int[] bankIDs = new int[] 
	{			
		diamonId, //diamond		
		goldId,  //gold		
		wineId  //wine	
	};
		
	public Abyte0_ArdThiever(Extension e) {super(e);}		
	public void init(String params) 
	{		
		print("ArdThiever fmode,npc,npc,npc...  paladin is 323, hero 324");
		
		print("Version " + SCRIPT_VERSION);
		
		print("type '--help' in public chat to view help");
		
		if(!params.equals(""))
		{	
			String[] in = params.split(",");
			fightMode = Integer.parseInt(in[0]);
			if(in.length > 1)
			{
				npcIDs = new int[in.length - 1];		
				for(int i = 0; i < npcIDs.length; i++)
				{
					npcIDs[i] = Integer.parseInt(in[i + 1]);
					if(npcIDs[i] == hero) isDoingHero = true;
				}
			}
		}
		for(int i = 0; i < npcIDs.length; i++)
		{
			if(npcIDs[i] == hero) isDoingHero = true;
		}
		
		printParams();
		
		initialXp = getThievingXp();
		initialTime = System.currentTimeMillis();
	}	
		
	public int main() 
	{
		SayRandomQuote();
		if(getFightMode() != fightMode)			
			setFightMode(fightMode);				
		if(inCombat())
		{			
			walkTo(getX(), getY());			
			return 1000;		
		}				
		if(getFatigue() > 90) 
		{
			useSleepingBag();
			return 1000;
		}
		if(getInventoryCount(dropIDs) > 0) {			
			dropItem(getInventoryIndex(dropIDs));		
		}		
		if(isBanking()) 
		{			
			//deposit money and keep 1 gp
			if(getInventoryCount(10) > 1) 
			{				
				deposit(10,getInventoryCount(10)-1);
				return 1000;
			}
			else if(getInventoryCount(10) < 1) 
			{				
				withdraw(10,1);
				return 1000;
			}
			//deposit chaos and keep 1 chaos
			if(getInventoryCount(chaosId) > 1) 
			{				
				deposit(chaosId,getInventoryCount(chaosId)-1);
				return 1000;
			}
			if(getInventoryCount(deathId) > 1) 
			{				
				deposit(deathId,getInventoryCount(deathId));
				return 1000;
			}
			if(getInventoryCount(bloodId) > 1) 
			{				
				deposit(bloodId,getInventoryCount(bloodId));
				return 1000;
			}
			if(getInventoryCount(goldId) > 0) 
			{				
				deposit(goldId,getInventoryCount(goldId));
				return 1000;
			}
			if(getInventoryCount(wineId) > 0) 
			{				
				deposit(wineId,getInventoryCount(wineId));
				return 1000;
			}
			if(getInventoryCount(diamonId) > 0) 
			{				
				deposit(diamonId,getInventoryCount(diamonId));
				return 1000;
			}
			//else if(getInventoryCount(41) < 1) 
			//{				
			//	withdraw(41,1);
			//	return random(500, 600);			
			//}
			
			if((getInventoryCount() >= MAX_INVENTORY_SIZE - 4 && !isDoingHero) || getInventoryCount() >= MAX_INVENTORY_SIZE - freeInventrySlotsRequired && isDoingHero) 
			{				
				closeBank();				
				return 1000;	
			}
			
			if(isDoingHero)
			{
				withdraw(330, MAX_INVENTORY_SIZE - freeInventrySlotsRequired - getInventoryCount());
				return 1000;
			}
			else
			{
				withdraw(330, MAX_INVENTORY_SIZE - getInventoryCount() - 2);
				return 1000;
			}
				
		}				
		if(isQuestMenu()) 
		{			
			answer(0);			
			return random(5500, 5600);		
		}	
		
		
		
		//verrification si on est trapper
		if(getX() >= 555 && getX() <= 558 && getY() >= 587 && getY() <= 590)
		{
			//si la la clauture dans la garnde cloture
			int[] Door = getObjectById(57);
			if(Door[0] != -1)
			{
				atObject(Door[1], Door[2]);
				return random(800,900);
			}
		}
		else if(getX() >= 555 && getX() <= 560 && getY() >= 577 && getY() <= 586)
		{
			//si  la garnde cloture
			int[] Door = getObjectById(57);
			if(Door[0] != -1)
			{
				atObject(Door[1], Door[2]);
				return random(800,900);
			}
		}
		else if(getX() >= 542 && getX() <= 548 && getY() >= 576 && getY() <= 580)
		{
			//si  la teleport house
			int[] Door = getWallObjectById(2);
			if(Door[0] != -1)
			{
				atWallObject(Door[1], Door[2]);
				return random(800,900);
			}
		}
		else if(getX() >= 552 && getX() <= 556 && getY() >= 568 && getY() <= 572)
		{
			//si  la bed house
			int[] Door = getWallObjectById(2);
			if(Door[0] != -1)
			{
				atWallObject(Door[1], Door[2]);
				return random(800,900);
			}
		}
		else if(getY() >= 608)
		{
			//si proche banque on ouvre la porte de la banque
			int[] Door = getObjectById(64);//BankDoor
			if(Door[0] != -1)
			{
				atObject(Door[1], Door[2]);
				return random(800,900);
			}
		}
		else if(getX() >= 553 && getX() <= 558 && getY() >= 599 && getY() <= 606)
		{
			//si proche banque on ouvre la porte de la house
			int[] Door = getWallObjectById(2);
			if(Door[0] != -1)
			{
				atWallObject(Door[1], Door[2]);
				return random(800,900);
			}
		}
		
		
		if(getInventoryCount(foodIDs) == 0 || (getInventoryCount() >= MAX_INVENTORY_SIZE && isDoingHero))
		{			

			int banker[] = getNpcByIdNotTalk(BANKERS);	        
			if(banker[0] != -1)
			{			
				talkToNpc(banker[0]);	        
				return 3000;		
			}
			else if(getX() == 549 && getY() == 596)
			{
				//si au "WalkingCenter" on walk "WalkingNearBank"
				walkTo(548,608);//WalkingNearBank     
				return 3000;		
			}
			else
			{
				walkTo(549,596);//WalkingCenter     
				return 3000;		
			}
		}
		if(getHpPercent() < 70) 
		{	
			int idx = getInventoryIndex(foodIDs);
			if(idx == -1) 
			{
				if(getHpPercent() < 30) 
				{
					System.out.println("hp is dangerously low with no food.");	    		
					walkTo(549,596);    
					return 1000;
				}
				else
				{
					return random(60000,90000);
				}
			}	    	
			useItem(idx); 	    	
			return random(1500, 1600);    	
		}				
		int[] npc = getAllNpcById(npcIDs);		
		if(npc[0] != -1)
		{		
			thieveNpc(npc[0]);		
			return random(500, 1000);
		}
		else
		{
			walkTo(549,596);//WalkingCenter
		}
		return 500;
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
    
	private void resetCounters()
	{
		initialXp = getThievingXp();
		initialTime = System.currentTimeMillis();
	}
	
	@Override
	protected void reportXpChange()
	{
		
		int xpDifference = getThievingXp() - initialXp;
		long timeSpan = System.currentTimeMillis() - initialTime;
		long secondSpan = timeSpan / 1000;
		long xpRatio = xpDifference * 3600L / secondSpan; //The L set 3600 as long variable Forcing to calculate as long to avoid overflow
		long thieveCount = (long)(xpDifference / 273.25);

		print("=================================");
		print("initialXp: " + initialXp);
		print("total Thieving xp gained: " + xpDifference);
		print("time running: " + secondSpan + " s");
		print("xpRatio: " + xpRatio + "/h");
		print("=================================");
		print("Chaos: " + thieveCount);
	}
	
	public int getThievingXp()
	{
		return getXpForLevel(17);
	}

	@Override
	public String getSctiptVersion()
	{
		return SCRIPT_VERSION;
	}
	
    @Override
    public void onServerMessage(String s) {

        if (s.contains("standing here for 5 mins!")) {
			needToMoveFromX = getX();
			needToMoveFromY = getY();
			needToMove = true;
        }
		
    }

    @Override
    public void onChatMessage(String msg, String name, boolean pmod, boolean jmod) {
		
		//String receivedLC = msg.toLowerCase();
		//
		//final String lname = client.getPlayerName(client.getPlayer());		
        //if(name.equalsIgnoreCase(lname))
		//{
		//}
		
		super.onChatMessage(msg, name, pmod, jmod);
    }

	@Override
	protected void printParams()
	{
		print("fightMode is " + FIGHTMODES[fightMode]);
		
		if(npcIDs.length > 0)
		{
			String npcs = "Script will fight ";
			for(int i = 0; i < npcIDs.length; i++)
				npcs += npcIDs[i] + " ";
			print(npcs);
		}
	}
	
	@Override
	protected void printHelp()
	{
		print("Press # or ' or type --status to display xp stats");
		print("Press F2 to reset stats");
		
		print("type @mag@--help@whi@ in public chat to view help");
		print("type @mag@--param@whi@ to view currently running parameters");
		print("type @mag@--version@whi@ to view currently running script version");
		print("type @mag@version@whi@ to view other players running script version");
		print("type @mag@base version@whi@ to view other players Abyte0_Script version");
		
		print("param are fightmode,npc,npc,npc,etc");
		print("param example: 3,324 would be defence on hero");
	}
	
	private void changePosition()
	{
		int pid = getSelfPid();
		if(pid < 250 && pid > 15)
		{
			print("bad pid, we can relog");
			needToMove = false;
		}
			
		if(getX() != needToMoveFromX || getY() != needToMoveFromY)
			needToMove = false;
		else
			walkTo(needToMoveFromX + random(0,2) - 1, needToMoveFromY + random(0,2) - 1);
	}
	
	int hero = 324;
	int needToMoveFromX = -1;
	int needToMoveFromY = -1;
	boolean needToMove = false;
	boolean isDoingHero = false;
	int initialXp = 0;
	long initialTime = 0;
}