//Made by Abyte0

		//0.4 Fix Stair Hideout
		//0.5 Pick Up Scimitar
		//0.6 Bank Fix
		//0.7 Added Food Param Comment + Stair Eating
		//0.8 Fixed Legit Trick With Door
		//0.9 2013-01-25 - PickUp Mith Bar
		//0.9.1 2013-01-25 - Can Eat To Pick Bar...
		//0.9.2 2021-06-30 - Updated to OpenRSC
		//0.9.4 2021-12-26 - Safety to avoid going down the ladder after chest spawn near chicken and die
		//1.0.0 2021-12-30 - Camelot teleport if available
		//1.1.0 2022-01-02 - # for thieving stats
		//2.0 2022-01-08 -{- Require [Abyte0_Script 1.6+] -}-  If no param are provided, it will use any food in bank and fightmode = Defence
		//2.0.1 2022-01-08 - Bank half full wine if you drink wines as food, support 330 as full cake
		//2.0.2 2022-01-08 - Pickup rares from ground, will stay in inv...
		//2.1 2022-01-27 - Will try to move to avoid reloggin [Require Abyte0_Script 1.7.3+]
		//2.2 2022-01-28 - degelated some task to the super class [Require Abyte0_Script 1.7.4+]
		//2.3 2022-02-14 - Updated to work with abyte0_Script 1.8  [require abyte0_Script 1.8+]
		//2.3.1 2022-10-16 - Fixed need to move to only work in the paladin room + escale chest room when food
		//2.3.2 2024-03-09 - Fixed bug after server reboot that get stock on the other castle door
		//2.4.0 - 2024-05-01 - Fix botting word to add extras T to avoid triggering admins warning
		
public class Abyte0_Paladin extends Abyte0_Script 
{
	//============ CONFIG START ============//	
	boolean eatFoodToPickMITH_BAR_ID = false; //change this to true if you want to eat food to be able to pick mith bar....
	boolean keepOneExtraInventorySpace = true; //This will try to leave inventory with one remaining empty space
	int fightMode = 3; //0 Controlled, 1 Strength, 2 Attack, 3 Defence
	//============= CONFIG END= ============//
	
	private final String SCRIPT_VERSION = "2.4.0";
	
	@Override
	public String[] getRandomQuotes()
	{
		String[] result = {
			"Thieving level?",
			"You got nice wallet in my picket",
			"Once a thief, always a thief",
			"give me that wallet!",
			"fking bots!",
			"Wanna buy a watch?",
			"I stole a million from that guy over there!",
			"I stole his keys, kill him you'll see he wont drop any!",
			"nice xp!",
			"Sup!",
			"I am what I am, but I'm not a thief",
			"Nice hat bro",
			"is that a rune armore?",
			"what food do you use?",
			"this is so quick xp",
			"anyone here not botting?",
			"why such low pid?",
			"so hard to thieve these with so many bots",
			"almost 96 thieving legit",
			"I bet your thief lvl is like 99?!",
			"Welcome back bro"};
		return result;
	}

	public Abyte0_Paladin(Extension e) {super(e);}		
	public void init(String params) 
	{
		print("Abyte0_Paladin Thiever for Paladin Tower in Ardougne");
		print("Version " + SCRIPT_VERSION);

		printHelp();
		
		if(params.equals(""))
		{
			loadAllFoods();
			print("@red@WARNING - Using default values :@whi@ " + FIGHTMODES[fightMode] + " + Any food");
		}
		else
		{
			String[] in = params.split(",");		
			fightMode = Integer.parseInt(in[0]);
			if(in.length > 1)
			{
				if(in.length == 2 && Integer.parseInt(in[1]) == 330)
				{
					print("Parsing 330 as full cake");
					foodIDs = new int[]{335,333,330};
				}
				else
				{
					foodIDs = new int[in.length - 1];		
					for(int i = 0; i < foodIDs.length; i++)
						foodIDs[i] = Integer.parseInt(in[i + 1]);
				}
			}
			else
			{
				loadAllFoods();
				print("@or3@No food id provided, script will use any known food found in bank");
			}
		}
		
		initialXp = getThievingXp();
		initialTime = System.currentTimeMillis();
		
		//Do not change
		hasStatistics = true;
	}	
	
	private void loadAllFoods()
	{
		foodIDs = new int[ALL_KNOWN_FOODS.length];		
		for(int i = 0; i < ALL_KNOWN_FOODS.length; i++)
			foodIDs[i] = ALL_KNOWN_FOODS[i];			
	}
	
	public int main() 
	{
		SayRandomQuote();
		
		if(getFightMode() != fightMode)			
			setFightMode(fightMode);
		
		if(getCurrentLevel(3) <= 10)
		{
			tryTeleportingToCamelot();
			return 3000;
		}		
		
		if(inCombat())
		{			
			walkTo(getX(), getY());			
			return 800;		
		}
		
		if(getFatigue() > 90) 
		{			
			useSleepingBag();			
			return 1000;		
		}
		
		if(getInventoryCount(JUG_ID) > 0) {			
			dropItem(getInventoryIndex(JUG_ID));
			return 2000;
		}		
		
		if(isBanking()) 
		{
			//deposit money and keep 1 gp
			if(getInventoryCount(10) > 1) 
			{				
				deposit(10,getInventoryCount(10)-1);
				return 1800;
			}
			else if(getInventoryCount(10) < 1) 
			{				
				withdraw(10,1);
				return 1800;
			}
			//deposit chaos and keep 1 chaos
			if(getInventoryCount(41) > 1) 
			{				
				deposit(41,getInventoryCount(41)-1);
				return 1800;
			}
			else if(getInventoryCount(41) < 1) 
			{				
				withdraw(41,1);
				return 1800;
			}
			
			//On Depose Scimitar
			if(getInventoryCount(427) > 0) 
			{				
				deposit(427,getInventoryCount(427));
				return 500;			
			}
			//On Depose Raw Shark
			if(getInventoryCount(545) > 0) 
			{				
				deposit(545,getInventoryCount(545));
				return 500;			
			}
			//On Depose Uncut Saphire
			if(getInventoryCount(160) > 0) 
			{				
				deposit(160,getInventoryCount(160));
				return 500;
			}
			//On Depose Addy Ore
			if(getInventoryCount(154) > 0) 
			{				
				deposit(154,getInventoryCount(154));
				return 500;			
			}
			//On Depose Steel Bar
			if(getInventoryCount(STEEL_BAR_ID) > 0) 
			{				
				deposit(STEEL_BAR_ID,getInventoryCount(STEEL_BAR_ID));
				return 500;			
			}
			//On Depose Mith Bar
			if(getInventoryCount(MITH_BAR_ID) > 0) 
			{				
				deposit(MITH_BAR_ID,getInventoryCount(MITH_BAR_ID));
				return 500;			
			}
			//Lets deposite a generated half wine
			if(getInventoryCount(halfFullWine) > 0) 
			{				
				deposit(halfFullWine,getInventoryCount(halfFullWine));
				print("Half full wine banked!");
				Say("Half wine banked by Abyte0_Paladin script version " + SCRIPT_VERSION);
				return 500;			
			}
			
			//Lets withdraw foods
			int i = 0;
			while(getInventoryCount() < getMaxInventoryCount())
			{
				if(getInventoryCount() >= getMaxInventoryCount())
				{
					closeBank();
					return 1000;
				}
				
				if(foodIDs.length <= i)
				{
					print("No more food....");
					return 10000;
				}
				
				if(bankCount(foodIDs[i]) > 0)
				{
					withdraw(foodIDs[i], getMaxInventoryCount() - getInventoryCount());
					return 2500;
				}
				
				i++;
			}
		}
		
		if(isQuestMenu())
		{
			answer(0);
			return 2500;
		}
		
		//On verifie si on a de la nourriture		
		if(getInventoryCount(foodIDs) > 0)
		{
			if(getHpPercent() < 70) 
			{	
				return eatFood();
			}
			//on regarde si on est dans la sale des paladins
			if(getX() >= 602 && getX() <= 615 && getY() >= 1548 && getY() <= 2000)
			{
				int rareItemsFoundResult = pickupRareItems(4);
				if(rareItemsFoundResult > 0) return rareItemsFoundResult;
				
				if(needToMove)
				{
					changePosition();
					return 500;
				}
		
				//On regarde si on peut ramasser des mith bars
				if(getInventoryCount() < 30)
				{
					
					//y a t il des bars sur le plancher
					int[] groundItemSteel = getItemById(STEEL_BAR_ID);
					if(groundItemSteel[0] != -1)
					{
						pickupItem(groundItemSteel[0], groundItemSteel[1], groundItemSteel[2]);
						return 1000;
					}
					//y a t il des bars sur le plancher
					int[] groundItemMith = getItemById(MITH_BAR_ID);
					if(groundItemMith[0] != -1)
					{
						pickupItem(groundItemMith[0], groundItemMith[1], groundItemMith[2]);
						return 1000;
					}
				}
				else
				{
					if(eatFoodToPickMITH_BAR_ID)
						return eatFood();
				}
				
				//On thieve les paladins
				int[] npc = getNpcById(npcID);		
				if(npc[0] != -1)
				{		
					thieveNpc(npc[0]);
					isChestReady = true;
					return 300;
				}
			}
			else
			{
				int[] doorObj;
				int[] stairs;
				
				//Si on est en haut ou il y a le chest
				if(isAtApproxCoords(611,2491,10))
				{
					atObject(611,2495);
					return 2000;
				}
				
				//Si pret a SORTIR de la BANQUE
				if(getX() == 551 && getY() == 612)
				{
					stairs = getObjectById(64);				
					if(stairs[0] != -1)
					{
						atObject(stairs[1], stairs[2]);
						return 1000;
					}
					walkTo(550,612);
					return 600;
				}
				
				//si on est cacher derriere les marches on veut monte
				if(getX() == 613 && getY() == 601)
				{
					stairs = getObjectById(342);				
					if(stairs[0] != -1)
					{
						atObject(stairs[1], stairs[2]);
						return 2000;
					}
				}
				
				//Si DANS la banque
				if(getX() >= 551 && getX() <= 554 && getY() >= 609 && getY() <= 616)
				{
					walkTo(551,612);
					return 600;
				}
				
				//Si on est en bas de smarche on veut aller a coter se cacher
				if(getX() >= 608 && getY() >= 597 && getY() <= 609)
				{
					walkTo(613,601);
					return 400;
				}
				//Si on est en haut des marche et on veut entrer
				if(getX() >= 602 && getX() <= 615 && getY() > 1500 && getY() < 1548)
				{
					int[] door = getWallObjectById(97);			
					if(door[0] != -1 && isAtApproxCoords(door[1], door[2],5))
					{
						atWallObject2(door[1], door[2]);
						return 1000;
					}
				}

				//On va vers le zoo
				if(getX() < 567)
				{
					walkTo(567,606);
					return 500;
				}
				//On va vers  le magasin general
				if(getX() < 580)
				{
					walkTo(580,606);
					return 500;
				}
				//On va a la porte
				if(getX() < 598)
				{
					walkTo(598,604);
					return 500;
				}
				if(getX() < 599)
				{
					//Passing the Metal Gate
					stairs = getObjectById(57);				
					if(stairs[0] != -1 && isAtApproxCoords(stairs[1], stairs[2],10))
					{
						atObject(stairs[1], stairs[2]);
						return 800;
					}
					walkTo(599,604);
					return 500;
				}
				if(getX() < 608)
				{
					//Passing the Wooden Door
					stairs = getObjectById(64);				
					if(stairs[0] != -1)
					{
						if(stairs[1] >= 605 && stairs[1] <= 610 && stairs[2] >= 600 && stairs[2] <= 608)
						{
							atObject(stairs[1], stairs[2]);
							return 800;
						}
					}
					walkTo(608,604);
					return 500;
				}
			}
			return 500;
		}
		else
		{
			int[] doorObj;
			int[] stairs;
			//On regarde si on est dans la sale des paladins
			if(getX() >= 602 && getX() <= 615 && getY() >= 1548 && getY() <= 1648)
			{
				//Si le chest est pret on monte pour se teleporter
				if(isChestReady)
				{
					//On Monde lechele
					stairs = getObjectById(5);				
					if(stairs[0] != -1 && isAtApproxCoords(stairs[1], stairs[2],10))
					{
						atObject(stairs[1], stairs[2]);
						return 1500;
					}
				}
				else
				{
					doorObj = getWallObjectById(97);
					if(doorObj[0] != -1 && isAtApproxCoords(doorObj[1], doorObj[2],10))
					{			
						atWallObject(doorObj[1], doorObj[2]);
						return 1000;
					}	
				}
				return 800;
			}
			
			//Si devant Banque
			if(getX() == 550)
			{
				stairs = getObjectById(64);				
				if(stairs[0] != -1 && isAtApproxCoords(stairs[1], stairs[2],5))
				{
					atObject(stairs[1], stairs[2]);
					return 1000;
				}
				walkTo(551,612);
				return 600;
			}
			//Si DANS la banque
			if(getX() >= 551 && getX() <= 554 && getY() >= 609 && getY() <= 616)
			{
				int banker[] = getNpcByIdNotTalk(BANKERS);	        
				if(banker[0] != -1)
				{			
					talkToNpc(banker[0]);	        
					return 3000;		
				}
			}
			
			//-----
			//Walking To Bank Manualy
			//-----
			
			//Si on est en haut ou il y a le chest
			if(isAtApproxCoords(611,2491,10))
			{
				int[] groundItem = getItemById(427);
				if(groundItem[0] != -1)
				{
					pickupItem(groundItem[0], groundItem[1], groundItem[2]);
					return 1000;
				}
				//Si le chest n'est aps pret on dessend
				if(getObjectIdFromCoords(610,2487) != 338)
				{
					isChestReady = false;
					stairs = getObjectById(6);
					if(stairs[0] != -1 && stairs[1] == 611 && stairs[2] == 2495)
					{
						atObject(stairs[1], stairs[2]);
						return 1000;
					}
				}
				else
				{
					atObject2(610,2487);
					return 2000;
				}
			}
			//Si on est a coter des escalier et on veut descendre
			if(getX() >= 602 && getX() <= 615 && getY() > 1500 && getY() < 1548) //Added safet to avoid going down after chest
			{
				stairs = getObjectById(44);				
				if(stairs[0] != -1)
				{
					atObject(stairs[1], stairs[2]);
					return 1000;
				}
			}
			//Si on est proche des escalier pour sortir du castle
			if(getX() >= 608 && getY() >= 597 && getY() <= 609)
			{
				//Passing the Wooden Door
				int closedDoor = getObjectIdFromCoords(607,603); //Updated to avoid getting the other castle door if both are closed
				if(closedDoor == 64 && isAtApproxCoords(607, 603,5))
				{		
					atObject(607, 603);
					return 800;
				}
				walkTo(607,604);
				return 500;
			}
			//Si on est proche des Warriors
			if(getX() >= 599)
			{
				//Passing the Metal Gate
				stairs = getObjectById(57);				
				if(stairs[0] != -1 && isAtApproxCoords(stairs[1], stairs[2],10))
				{
					atObject(stairs[1], stairs[2]);
					return 800;
				}
				walkTo(598,604);
				return 500;
			}
			//Si on est proche du Pont
			if(getX() >= 585)
			{
				walkTo(584,606);
				return 500;
			}
			//Si on est proche du Zoo
			if(getX() >= 570)
			{
				walkTo(569,606);
				return 500;
			}
			//Si on est proche du Zoo2
			if(getX() >= 551)
			{
				walkTo(550,608);
				return 500;
			}
			
			//-----
			//----- Walking to Bank By Teleport
			//-----
			
			//Si on est proche du Teleport
			if(isAtApproxCoords(523,606,5))
			{
				walkTo(528,615);
				return 500;
			}
			//Si on est proche des Chickens
			if(getX() <= 542)
			{
				walkTo(543,615);
				return 500;
			}
			//Si on est proche du bateau on marche pres de la banque
			if(getX() <= 549)
			{
				walkTo(550,612);
				return 500;
			}
			return 500;
		}
	}
	
	private void tryTeleportingToCamelot()
	{
		if(getCurrentLevel(6) < 45) return; //magic is too low
		if(getInventoryCount(LAW_RUNE_ID) < 1) return; //not enoug law runes
		if(getInventoryCount(AIR_RUNE_ID) < 5 && getInventoryCount(AIR_STAFF_ID) < 1) return; //No air
		
		if(getInventoryCount(AIR_STAFF_ID) >= 1)
			wearItem(getInventoryIndex(AIR_STAFF_ID));
			
		castOnSelf(camelotTeleport);
		castOnSelf(camelotTeleport);
		castOnSelf(camelotTeleport);
		
		print("Emergency teleport exit and stop");
		
		setAutoLogin(false);
		logout();
		//stopScript();
	}
	
	private int eatFood()
	{
		int idx = getInventoryIndex(foodIDs);	    	
		if(idx == -1) 
		{
			if(getHpPercent() < 30) 
			{
				tryTeleportingToCamelot();
				print("hp is dangerously low with no food.");	    		
				stopScript();	    		
				return 0;
			}
			else
			{
				return 10000;
			}
		}
		
		EatFood(foodIDs);
		
		if(getInventoryCount(halfFullWine) > 0)
			Say("Woot! Half-Full-Wine generated!");
				
		return 800;
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
	
	private int getMaxInventoryCount()
	{
		if(keepOneExtraInventorySpace) return 29;
		return 30;
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
		
		super.onChatMessage(msg, name, pmod, jmod);
    }

	@Override
	protected void printHelp()
	{
		super.printHelp();
		
		print("Params = fmode,foodid,foodid,food,etc");
		print("Example= 3,330,333,335,373 would use defence to use full cake and lobsters");
	}
	
	@Override
	protected void printParams()
	{
		print("Fmode is @or3@" + FIGHTMODES[fightMode]);
		
		double eatAt = getLevel(3) * 0.70;
	
		if(foodIDs.length > 7)
			print("Script use most known food to eat @or3@when hp <= " + eatAt);
		else if(foodIDs.length > 1)
		{
			String foods = "Script use ";
			for(int i = 0; i < foodIDs.length; i++)
				foods += foodIDs[i] + " ";
			print(foods + "to eat @or3@when hp <= " + eatAt);
		}
		else
			print("Script use food : " + foodIDs[0] + " to eat @or3@when hp <= " + eatAt);

		if(eatFoodToPickMITH_BAR_ID)
			print("Script will eat fod to pickup item from ground");

	}
	
	@Override
	protected void reportXpChange()
	{
		
		int xpDifference = getThievingXp() - initialXp;
		long timeSpan = System.currentTimeMillis() - initialTime;
		long secondSpan = timeSpan / 1000;
		long xpRatio = xpDifference * 3600L / secondSpan; //The L set 3600 as long variable Forcing to calculate as long to avoid overflow
		long thieveCount = (long)(xpDifference / 151.75);

		print("=================================");
		print("initialXp: " + initialXp);
		print("total Thieving xp gained: " + xpDifference);
		print("time running: " + secondSpan + " s");
		print("xpRatio: " + xpRatio + "/h");
		print("=================================");
		print("Chaos: " + thieveCount + " && coins: " + (thieveCount * 80));
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
	
	int[] foodIDs;
	int camelotTeleport = 22;
	int AIR_RUNE_ID = 33;
	int AIR_STAFF_ID = 101;
	int LAW_RUNE_ID = 42;
	boolean isChestReady = true;
	int[] npcID = new int[]
	{ 
		323 //Paladin
	};		
	int STEEL_BAR_ID = 171;
	int MITH_BAR_ID = 173;
	int halfFullWine = 246;
	int JUG_ID = 140;
	int needToMoveFromX = -1;
	int needToMoveFromY = -1;
	boolean needToMove = false;
	int initialXp = 0;
	long initialTime = 0;
	
}