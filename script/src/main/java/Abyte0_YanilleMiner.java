/*
	By: 			Abyte0
	Version 0.1			2022-04-02		Support Clay, Tin, Copper / Bank Or Power / Tasking
*/
public class Abyte0_YanilleMiner extends Abyte0_Script implements ITaskScript
{
	
	private final String SCRIPT_VERSION = "0.1";
	
	int fightMode = 3;
	
	@Override
	public String[] getRandomQuotes()
	{
		String[] result = {"Mining level?","Smithing level?","Mining lvl?","Smithing lvl?","Soon I,ll be able to mine runite","This rock is hard","Should I mine iron or clay?","Banking or not, this is the question","How do you mine steal?"};
		return result;
	}

	public Abyte0_YanilleMiner(Extension e)
	{
		super(e);
	}    
	public void init( String params )
	{
		mined = 0;
		print("Abyte0 : Yanille Miner!");
		print("Version " + SCRIPT_VERSION);
		
		print("Press # for stats");
		print("Press F2 to reset stats");
		
		print("'Power' as param = Power Mining...");
		
		if(params.contains(TaskParameters.TASKING_HEADER))
		{
			isRunningAsTask = true;
			print("Abyte0_YanilleMiner [Running as task]");
			
			taskingDetails = TaskParameters.buildFromString(params);
			
			String[] taskingExtraParams = taskingDetails.TaskExtraInputParams.split(",");
			if(taskingExtraParams.length > 0)
			{
				if(taskingExtraParams[0].contains("targetQuantity="))
				{
					targetQuantity = Integer.parseInt(taskingExtraParams[0].substring(15));
				}
			}
			
			params = taskingDetails.TaskInputParams;
			print("param left:"+params);
		}
		
		String[] in = params.split(",");
		
		if(in.length >= 1)
		{
			fightMode = Integer.parseInt(in[0]);
			if(in.length > 1)
			{
				String typeOfRock = in[1].toLowerCase();
				
				if(typeOfRock.contains("iron"))
				{
					oreID = ironOreId;
					RockIds = ironRockIds;
				}
				if(typeOfRock.contains("tin"))
				{
					oreID = tinOreId;
					RockIds = tinRockIds;
				}
				if(typeOfRock.contains("cop") || typeOfRock.contains("coop"))
				{
					oreID = copperOreId;
					RockIds = copperRockIds;
				}
				if(typeOfRock.contains("clay"))
				{
					oreID = clayOreId;
					RockIds = clayRockIds;
				}
			}
			
			String receivedLC = params.toLowerCase();	
			if(receivedLC.contains("power"))
				cptInventaireToBank = 60;
			else
				cptInventaireToBank = 30;
		
			printParams();
			
		}
		else
		{
			print("@red@--- ERROR Params ---");
			printHelp();
			print("@red@--- ERROR Params ---");
		}
		
		initialXp = getMiningXp();
		initialTime = System.currentTimeMillis();
	}    
	public int main()
	{
		SayRandomQuote();
		
		if(getFightMode() != fightMode)
		{
			setFightMode(fightMode);
		}        
		if(getFatigue() > 90)
		{
			useSleepingBag();
			return 1000;
		}
		
		int insideBankResult = doInsideBankActions();
		if(insideBankResult > 0) return insideBankResult;
		
		int isWalkingResult = walkBetweenBankAndMineActions();
		if(isWalkingResult > 0) return isWalkingResult;
		
		
		int miningResult = tryToMine();
		if(miningResult > 0) return miningResult;
		
		return 1000;
	}    
	
	private int doInsideBankActions()	
	{
		if(!isInYanilleBank())
			return -1;

		if(isBanking())
		{
			if(isRunningAsTask && hasCompleted()) { endTask(); return 666;}
		
			if(getInventoryCount(pickaxes) > 1)
			{
				depositAll();
				return 1000;
			}
			
			// Deposit gems and ores
			if(getInventoryCount(gem4) > 0)
			{
				deposit(gem4,1);
				return 1000;
			}
			if(getInventoryCount(gem3) > 0)
			{
				deposit(gem3,1);
				return 1000;
			}
			if(getInventoryCount(gem2) > 0)
			{
				deposit(gem2,1);
				return 1000;
			}
			if(getInventoryCount(gem1) > 0)
			{
				deposit(gem1,1);
				return 1000;
			}
			if(getInventoryCount(oreID) > 0)
			{
				mined += getInventoryCount(oreID);
				deposit(oreID,getInventoryCount(oreID));
				print("Mined " + mined + " ore so far");
				return 3000;
			}
			
			if(getInventoryCount() > 2)
			{                
				depositAll();
				return 3000;
			}
			
			if(getInventoryCount(pickaxes) != 1)
			{
				for(int i = 0; i < pickaxes.length; i++)
				{
					if(bankCount(pickaxes[i]) > 0)
					{
						withdraw(pickaxes[i],1);
						return 3000;
					}
				}
			}
			if(getInventoryCount(1263) < 1)
			{                
				withdraw(1263,1);
				return 3000;
			}
			
			closeBank();
			return 1000;
		}
		
		if(isQuestMenu())
		{
			answer(0);
			return 3500;
		}
		
		if(getInventoryCount() >= cptInventaireToBank || getInventoryCount(1263) < 1 || getInventoryCount(pickaxes) != 1)
		{
			int banker[] = getNpcByIdNotTalk(new int[]{95});
			if (banker[0] != -1 && !isBanking())
			{
				talkToNpc(banker[0]);
				return 3000;
			}
		}
		
		return 0;
	}
	
	private int walkBetweenBankAndMineActions()
	{
		if(isInYanilleBank() && getInventoryCount() >= cptInventaireToBank) return 0;
		if(isInYanilleMine() && getInventoryCount() < cptInventaireToBank) return 0;
		
		if(getInventoryCount() >= cptInventaireToBank)
		{
			if(getY() < 717)
			{                
				walkTo(575,717);//Near Ores
				return 1000;
			}
			if(getY() < 729)
			{
				walkTo(579,729); //Path 1
				return 1000;
			}
			if(getY() < 739)
			{
				walkTo(580,739); //Path 2
				return 1000;
			}
			if(getY() < 750)
			{
				walkTo(583,750); //Near Bank
				return 1000;
			}
			
			walkTo(585,752);//Bank
			return 1000;
		}
		else
		{
			if(getY() > 750)
			{
				walkTo(583,750);//Near bank
				return 1000;
			}
			if(getY() > 739)
			{
				walkTo(580,739);//Path 2
				return 1000;
			}
			if(getY() > 729)
			{
				walkTo(579,729);//Path 1
				return 1000;
			}
			if(getY() > 717)
			{
				walkTo(575,717);//Near Ores
				return 1000;
			}
			
			walkTo(574,717); //Mine
			return 1000;
			
		}
	}
	
	public int tryToMine()
	{
		if(getInventoryCount() >= cptInventaireToBank) return -1;

		if(!isInYanilleMine()) return -1;
	
		int[] rock = getObjectById(RockIds);
		if(rock[0] == -1) return 500; //No rock ready
		
		atObject(rock[1],rock[2]);				
		return 500;
	}
	
	public boolean isInYanilleBank()
	{
		if(getX() >= 585 && getX() <= 590 && getY() >= 750 && getY() <= 758) return true;
		
		return false;
	}
	
	public boolean isInYanilleMine()
	{
		if(getX() >= 550 && getX() <= 577 && getY() >= 686 && getY() <= 719) return true;
		
		return false;
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
		initialXp = getMiningXp();
		initialTime = System.currentTimeMillis();
	}
	
    @Override
    public void onChatMessage(String msg, String name, boolean pmod, boolean jmod) {
		
		super.onChatMessage(msg, name, pmod, jmod);
    }

	@Override
	protected void printHelp()
	{
		super.printHelp();
		
		print("Params = fmode,typeOfRock,power(optional)");
		print("Example= 3,iron for def and iron banking or 1,clay,power to use strenght and power miner clay");
		
	}
	
	@Override
	protected void printParams()
	{
		print("Fmode is @or3@" + FIGHTMODES[fightMode]);
		print("Mining " + oreID);
		
		if(isRunningAsTask)
		{
			print("Runnign as Task");
			print("Target Quantity " + targetQuantity);
		}
	}
	
	@Override
	protected void reportXpChange()
	{
		
		int xpDifference = getMiningXp() - initialXp;
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
	}
	
	private int getMiningXp()
	{
		return getXpForLevel(14);
	}

	@Override
	public String getSctiptVersion()
	{
		return SCRIPT_VERSION;
	}
	
	
	private boolean isRunningAsTask = false;
	private TaskParameters taskingDetails;
	
	@Override
	public boolean hasCompleted()
	{
		if(bankCount(oreID) >= targetQuantity) return true;
		
		return false;
	}
	
	@Override
	public void endTask()
	{
		depositAll();
		print("Task ended : Mining " + targetQuantity + " of type : " + oreID);
		
		switchToScript(taskingDetails.MainScriptName,taskingDetails.MainScriptParams);
	}
	
	int initialXp = 0;
	long initialTime = 0;
	
	int oreID;
	int[] RockIds;
	
	// Iron    
	int banker = 95; // Banker    // uncut gem id's    
	int gem1 = 160; // sapph   
	int gem2 = 159; // emerald    
	int gem3 = 158; // ruby    
	int gem4 = 157; // diamond
	
	int clayOreId = 149;
	int tinOreId = 202;
	int copperOreId = 150;
	int ironOreId = 151;
	
	int[] clayRockIds = new int[]{114,115};
	int[] tinRockIds = new int[]{104,105};
	int[] copperRockIds = new int[]{100,101};
	int[] ironRockIds = new int[]{102,103};
	
	int mined;
	int cptTry = 0;
	int cptInventaireToBank = 30;
	int targetQuantity = 1000000;
	
	int[] pickaxes = new int[]{156,1258,1259,1260,1261,1262};
}