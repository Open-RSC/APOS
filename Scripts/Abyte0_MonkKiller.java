//Version 0.1 - 2021-11-28 Initial Beta
//Version 1.0 - Sleep, bones added
//Version 1.1 - Use strength prayer
//Version 2.0 - Goes up the ladder if 31+ prayer
//Version 2.1 - Toggle prayer when close to the monk before fight
//Version 2.2 - Xp stats with pressing of # or '
//Version 2.3 - Heal to almost full + optional param stopAt= + added command ::params  [require abyte0_Script 1.6.2+]
//Version 2.4 - 2022-02-14 - Updated to work with abyte0_Script 1.8  [require abyte0_Script 1.8+]


public class Abyte0_MonkKiller extends Abyte0_Script
{
	// ===== DEFAULT CONFIG ===== //
	int fmode = 1;
	int targetPrayerLevel = 31;
	int targetFmodeLevel = 100;
	// === END DEFAULT CONFIG === //
	int MONK_ID = 93;
	private final String SCRIPT_VERSION = "2.4.1";
	
	int initialXp = 0;
	long initialTime = 0;
	boolean needToHealTrigger = false;
	
	@Override
	public String[] getRandomQuotes()
	{
		String[] result = {"Have you seen Buddha?","Where is Bodhidharma?","Botter!","Nice sword!","How far are you from level 99?","Are you pure?","Die!","lets one hit that npc!","Monk not found","Lets pray!","Beautiful church","How do I become a Monk?","Pking level?"};
		return result;
	}

	public Abyte0_MonkKiller(Extension e)
	{
		super(e);
	}

	public void init(String params)
	{
		String str[]=params.split(",");
		
		if(!params.equals(""))
		{
			fmode = Integer.parseInt(str[0]);
			targetPrayerLevel = Integer.parseInt(str[1]);
			if(str.length > 2)
			{
				for(int i = 2; i < str.length; i++)
				{
					if(str[i].toLowerCase().startsWith("stopAt="))
					{
						targetFmodeLevel = Integer.parseInt(str[i].substring(7));
					}
				}
			}
		}
		else
			print("Param are fmode,targetPrayer : using 1,31");
		
		print("Version @mag@"+SCRIPT_VERSION+" : @whi@- Goes up the ladder if 31+ prayer");
		printParams();
		printHelp();
		//foodId = Integer.parseInt(str[2]);
		
		initialXp = getFmodeXp(fmode);
		initialTime = System.currentTimeMillis();
		
		print("@mag@type --help in public chat to view help while script is running");
	
	}

	public int main()
	{
		updateHealTrigger();
		SayRandomQuote();
		if(getFightMode() != fmode)
		{
			setFightMode(fmode);
			return 500;
		}
		togglePrayer();
		if(getFatigue() > 90) 
		{			
			useSleepingBag();			
			return 2000;		
		}
		if(isQuestMenu()) 
		{            
			answer(0);  
			return 6000;
		}
		if(TryBury() > -1) return 1000;
		
		int[] Door = getWallObjectById(2);
		int[] BankDoor = getObjectById(64);
		if(Door[0] != -1)
		{
			if(Door[1] == 253 && Door[2] == 464)
			{
				if(getX() <= 258 && getX() > 253)
				{
					walkTo(253,464);
					return 2000;
				}
				if(getX() <= 253)
				{
					atWallObject(Door[1], Door[2]);
					return random(800,900);
				}
			}
		}
        if(BankDoor[0] != -1)
        {
			if(BankDoor[1] == 261 && BankDoor[2] == 469)
			{
				if(getX() < 259)
					walkTo(261,469);
				else
					atObject(BankDoor[1], BankDoor[2]);
				return 2000;
			}
        }
		
		if(needToRecharge()) return 1000;
		
		if(needToHealTrigger)
		{
			if(inCombat())
			{
				RunFromCombat();
				return 500;
			}
			int abbot[] = getNpcByIdNotTalk(new int[]{174});
			if (abbot[0] != -1)
			{
				talkToNpc(abbot[0]);
				return 2000;
			}
			else
			{
				if(isUpstair())				
				{
					int upDownResult = tryToMoveUpDown();
					if(upDownResult != -1)
						return upDownResult;
					
					walkTo(252,1411);
				}
				walkTo(254,464);
				return 2000;
			}
		}
		
		if(inCombat()) return 100;
		
		if(targetFmodeLevel <= getFmodeLevel(fmode))
		{
			setAutoLogin(false);
			stopScript();
		}
		
		int[] npcs = getNpcInRadius(MONK_ID,getX(),getY(),15);
		if(npcs[0] != -1)
		{
			attackNpc(npcs[0]);
			return 500;
		}
		
		//No NPC, let try to move up or down
		int upDownResult = tryToMoveUpDown();
		if(upDownResult != -1)
			return upDownResult;
		
		return 2000;
	}
	
	private int tryToMoveUpDown()
	{
		if(getLevel(5) < 31) return -1; //Not high enoug to go upstair
		
		int[] ladder = getObjectById(198);
		if(isUpstair())
		{
			ladder = getObjectById(6);
		}
		
		if(ladder[0] == -1) return -1;
		
		atObject(ladder[1], ladder[2]);
		return 1000;
	}
	
	public void togglePrayer()
	{
		if(getLevel(5) >= 4 && getCurrentLevel(5) > 0)
		{
			int prayer = getStrengthPrayer();
			int[] npcs = getNpcInRadius(MONK_ID,getX(),getY(),2);
			boolean isNpcNextToPlayer = npcs[0] != -1;
			
			if((inCombat() || isNpcNextToPlayer) && !isPrayerEnabled(prayer))
				enablePrayer(prayer);
			if(!inCombat() && !isNpcNextToPlayer && isPrayerEnabled(prayer))
				disablePrayer(prayer);
		}	
	}
	
	private int getStrengthPrayer()
	{
		if(getLevel(5) >= 31)
			return 10;
		if(getLevel(5) >= 13)
			return 4;
		if(getLevel(5) >= 4)
			return 1;
		
		return -1;
	}
	
	public boolean needToRecharge()
	{
		if(getLevel(5) < 4) return false;
		if(getCurrentLevel(5) <= 2)
		{
			if(isInPriestRoom())
			{
				int[] altar = getObjectById(19);
				atObject(altar[1], altar[2]);
				return true;
			}
			if(isInPriestRoom2())
			{
				int[] monkAltar = getObjectById(200);
				atObject(monkAltar[1], monkAltar[2]);
				return true;
			}
			
		}
		return false;
	}
	
	private boolean isUpstair()
	{
		int x = getX();
		int y = getY();
		
		if(x >= 249 && x <= 265 && y >= 1402 && y <= 1412) return true;
		
		return false;
	}
		
	private boolean isInPriestRoom()
	{
		if(getX() >= 259 && getY() <= 468) return true;
		
		return false;
	}
	
	private boolean isInPriestRoom2()
	{
		if(getX() >= 259 && getY() >= 1406) return true;
		
		return false;
	}
	
	public void updateHealTrigger()
	{
		if(getCurrentLevel(3) + 4 >= getLevel(3))
			needToHealTrigger = false;
				
		if(getLevel(3) <= 15)
		{
			if(getCurrentLevel(3) <= 6)
				needToHealTrigger =  true;
		}
		else if(getLevel(3) < 30)
		{
			if(getCurrentLevel(3) <= 12)
				needToHealTrigger = true;
		}
		else if(getLevel(3) >= 30)
		{
			if(getCurrentLevel(3) <= 20)
				needToHealTrigger = true;
		}
	}
	
    public void onKeyPress(int keyCode) {
		if (keyCode == 192) { //#
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
		initialXp = getFmodeXp(fmode);
		initialTime = System.currentTimeMillis();
	}
	
	@Override
	protected void reportXpChange()
	{
		
		int xpDifference = getFmodeXp(fmode) - initialXp;
		long timeSpan = System.currentTimeMillis() - initialTime;
		long secondSpan = timeSpan / 1000;
		long xpRatio = xpDifference * 3600L / secondSpan; //The L set 3600 as long variable Forcing to calculate as long to avoid overflow
		

		print("=================================");
		print("initialXp: " + initialXp);
		print("initialTime: " + initialTime);
		print("total " + FIGHTMODES[fmode] + " xp gained: " + xpDifference);
		print("time running: " + secondSpan + " s");
		print("xpRatio: " + xpRatio + "/h");
		print("=================================");
	}
	
    @Override
    public void onServerMessage(String s) {
	
		//print("OnServerMessage:"+s);
        //if (isMage && s.contains("charge fades")) {
        //    needToCharge = true;
        //    return;
        //}
        //if (isMage && s.contains("You feel charged with magic power")) {
        //    needToCharge = false;
        //    return;
        //}
    }

	public int TryBury()
	{
		int bonesId = 20;
		if(getLevel(5) < targetPrayerLevel)
		{
			if(getInventoryCount(bonesId)>0)
			{
				if(inCombat())
				{
					RunFromCombat();
					return random(100,200);
				}
				
				int boneIndex = getInventoryIndex(bonesId);
				useItem(boneIndex);
				
				return random(200,300);
			}
			int[] bones = getItemById(bonesId);
			if(bones[0] != -1)
			{
				if(bones[1] == getX() && bones[2] == getY())
				{
					if(inCombat())
					{
						RunFromCombat();
						return random(100,200);
					}
					
					pickupItem(bones[0], bones[1], bones[2]);
					return random(200,300);
				}
			}
		}
		return -1;
	}
	
	
    @Override
    public void onChatMessage(String msg, String name, boolean pmod, boolean jmod) {
		
		super.onChatMessage(msg, name, pmod, jmod);
    }

	@Override
	public String getSctiptVersion()
	{
		return SCRIPT_VERSION;
	}
	
	@Override
	protected void printParams()
	{
		print("Fmode is " + FIGHTMODES[fmode]);
		if(targetFmodeLevel < 100)
			print(FIGHTMODES[fmode] + " will stop once reached " + targetFmodeLevel);
		if(targetPrayerLevel < 100)
			print("Prayer will stop once reached " + targetPrayerLevel);
	}
	
	@Override
	protected void printHelp()
	{
		print("Press # or ' or type --status to display xp stats");
		
		print("type --help in public chat to view help");
		print("type --param in public chat to view currently runnign parameters");
		
		print("3 ways to run it:");
		print("1: without parameters = unlimited strenght and bury until 31 prayer");
		print("2: 'fmode,targetPrayer' : ie 1,31  => unlimited strenght and bury until 31 prayer");
		print("3: bonus param 'stopAt=DesiredFmode' : ie 1,31,stopAt=50  => script stop at 50 str");
	}
	//11.9k xp str/h at 40 20 93 w 40p
}

