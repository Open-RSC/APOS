/**
*  Catherby Lobster Fishing *
 *  v1.0 please report bugs if any *
 *   XcendroX` */
//Version 0 by XcendroX
//Version 1 by Abyte0
//2  Ways doors opener
//Lobs OR Tuna/Swordy
//2021-07-09 V1.2 Updated to OpenRSC + added Shark
//2021-07-14 V1.3 Fixed walking bug which would happen when map is loaded from reloggin at fishing spot
//2024-05-11 V1.4.3 Added xp report and 5 mins move instead of counting

public class Abyte0_CatherbyFisher extends Abyte0_Script
{
	int sleepAt = 80;
	int FishType = 372;
	int FishType2 = 369;
	boolean isDoingShark = false;
	
	
	private final String SCRIPT_VERSION = "1.4.3";
	
	@Override
	public String[] getRandomQuotes()
	{
		String[] result = {"Don't let me sink! @ran@HELP!","Let's take a swim...","Sharknado!","Are you botting?","How far are you from level 99?","Nice Lobster!","What a HUGE trout!","Cooking level?","Fish level?","lvl?","OMG","Got it!","Watchout! There are Sharks!","A bad day of fishing is still better than a good day at the office!", "Life is like a game, but fishing is serious...","I fish better with a lit cigar; some with talent...","Good things come to those who bait","Gotta catch em all"};
		return result;
	}

	public Abyte0_CatherbyFisher(Extension e)
	{      super(e);   }
	public void init(String params)
	{
		print("Selected Abyte0 Catherby Fisher");
		print("Version " + SCRIPT_VERSION);
		print("Param can be 'Tunas', 'Sharks' or 'Lobs', default Sharks");
		
		printHelp();
		
		if(params.equals("Lob") || params.equals("Lobs"))
		{
			FishType = 372;
			print("Doing Lobs!");
		}
		if(params.equals("Shark") || params.equals("Sharks"))
		{
			FishType = 545;
			isDoingShark = true;
			print("Doing Sharks!");
		}
		else if(params.equals("Tuna") || params.equals("Tunas"))
		{
			FishType = 366;
			print("Doing Tunas and Swordy!");
		}
		else
		{
			FishType = 545;
			isDoingShark = true;
			print("Doing Default Sharks!");
		}
		
		initialXp = getFishingXp();
		initialTime = System.currentTimeMillis();
		
		//Do not change
		hasStatistics = true;
	}
	
	public int main()
	{
		SayRandomQuote();
		if(getFatigue() > 80)
		{
			useSleepingBag();
			return 1000;
		}
		if(getInventoryCount() == 30)
		{
			if(isQuestMenu())
			{
				answer(0);
				return random(1410, 1987);
			}
			if(isBanking())
			{
				if(getInventoryCount(FishType) > 0 || getInventoryCount(FishType2) > 0)
				{
					if(getInventoryCount(FishType) > 0)
						deposit(FishType, getInventoryCount(FishType));
					if(getInventoryCount(FishType2) > 0)
						deposit(FishType2, getInventoryCount(FishType2));
				}	
				else
					closeBank();
				return random(523, 603);
			}
			if(getX() <	412)
			{
				walkTo(412, 501);
				return random(430, 1502);
			}
			if(getX() <	423)
			{
				walkTo(423, 495);
				return random(430, 1502);
			}
			if(getX() <	437)
			{
				walkTo(439, 497);
				return random(430, 1502);
			}
			if(getX() >	443)
			{
				walkTo(439, 497);
				return random(430, 1502);
			}
			if(getY() <	491)
			{
				walkTo(439, 497);
				return random(4030, 8502);
			}
			if(getX() == 439 && getY() == 497)
			{
				//System.out.println("Open + Step InSide Bank");
				atObject(439, 497);
				walkTo(439, 496);
				return random(100, 1500);
			}
			if(getY() == 497)
			{
				//si on est perdu sur le coter...
				walkTo(439, 497);
				return random(100, 1500);
			}
			int banker[] = getNpcByIdNotTalk(BANKERS);
			if(banker[0] != -1)
			{
				talkToNpc(banker[0]);
				return 1000+random(1423, 1501);
			}
			return random(400, 500);
		}
		else
		{
			if(getX() == 439 && getY() == 496)
			{
				//Si on ets a la porte on louvre et sort
				//print("Open + Step OutSide Bank");
				atObject(439, 497);
				walkTo(439, 498);
				return random(100, 1500);
			}
			if(getY() < 497 && getX() > 436)
			{
				//Si on est dans la banque on va a coter de la porte
				walkTo(439, 496);
				//print("Walk to Door");
				return 1000;
			}
			if(getX() == 439 && getY() == 497)
			{
				//print(".wait.");
				//NOTHING waitting to be at 439, 498
			}
			
			if(needToMove)
			{
				changePosition();
				return 500;
			}
		
			if(isDoingShark)
			{
				if(getX() != 406 && getY() != 504)
				{
					//print("Walk to Shark");
					//Si on est pas rndu au fishing on Marche
					walkTo(406,504);
					return 1000;
				}
				if(isAtApproxCoords(406,504, 10) && getInventoryCount() != 30)
				{
					int[] fish = getObjectById(new int[]{261});
					if( fish[0] != -1 )
					{
							atObject2(fish[1], fish[2]);
						return random(403,1721);
					}
				}
			}
			else //Tunas && Lobs
			{
				if(getX() != 409 && getY() != 503)
				{
					//print("Walk to Fish");
					//Si on est pas rndu au fishing on Marche
					walkTo(409,503);
					return 1000;
				}
				if(isAtApproxCoords(409,503, 10) && getInventoryCount() != 30)
				{
					int[] fish = getObjectById(new int[]{194});
					if( fish[0] != -1 )
					{
						if(FishType == 372)
							atObject2(fish[1], fish[2]);
						else
							atObject(fish[1], fish[2]);
						return random(403,1721);
					}
				}
			}
		}
	return random(400, 500);
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
		initialXp = getFishingXp();
		initialTime = System.currentTimeMillis();
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
		
		print("Param can be 'Tunas', 'Sharks' or 'Lobs', default Sharks");
	}
	
	@Override
	protected void printParams()
	{
		if(FishType == 545)
			print("Script is fishing Sharks");
		else if(FishType == 372)
			print("Script is fishing Lobs");

	}
	
	@Override
	protected void reportXpChange()
	{
		
		int xpDifference = getFishingXp() - initialXp;
		long timeSpan = System.currentTimeMillis() - initialTime;
		long secondSpan = timeSpan / 1000;
		long xpRatio = xpDifference * 3600L / secondSpan; //The L set 3600 as long variable Forcing to calculate as long to avoid overflow


		print("=================================");
		print("initialXp: " + initialXp);
		print("total Fishing xp gained: " + xpDifference);
		print("time running: " + secondSpan + " s");
		print("xpRatio: " + xpRatio + "/h");
		print("=================================");
		
		long fishCount = 0L;
		if(FishType == 545)
		{
			fishCount = (long)(xpDifference / 110);
			print("Sharks fished = " + fishCount);
		}
		else if(FishType == 372)
		{
			fishCount = (long)(xpDifference / 90);
			print("Lobs fished = " + fishCount);
		}
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
	
	int needToMoveFromX = -1;
	int needToMoveFromY = -1;
	boolean needToMove = false;
	int initialXp = 0;
	long initialTime = 0;
}