//Craft your soft clay to get quickly to 10+ crafting
//2021-01-01 V0.1 added PieDish at level 4
//2022-03-28 V1.0 Support Tasking

public class Abyte0_CraftPot extends Abyte0_Script implements ITaskScript
{
	private final String SCRIPT_VERSION = "1.1";
	
	int fmode = 2;
	int bucket = 21;
	int clay = 149;
	int sink = 48;
	int waterBucket = 50;
	int softClay = 243;
	int unfiredPot = 279;
	int unfiredPieDish = 278;
	int cookedPot = 135;
	int cookedPieDish = 251;
	int potterWheel = 179;
	int potteryOven = 178;
	
	int bankerId = 95;
	
	String barType;
	
	int[] doorObj;

	public Abyte0_CraftPot(Extension e)
	{
		super(e);
	}    
	public void init( String params )
	{
		if(params.contains(TaskParameters.TASKING_HEADER))
		{
			isRunningAsTask = true;
			print("Abyte0_SoftClay [Running as task]");
			
			taskingDetails = TaskParameters.buildFromString(params);
			
			
			params = taskingDetails.TaskInputParams;
			print("param left:"+params);
		}
		
		String str[]=params.split(",");

		if(str.length == 1)
		{
			fmode = Integer.parseInt(str[0]);
			print("fmode set to : " + FIGHTMODES[fmode]);
		}
		
		print("Abyte0_CraftPot Ardougne Soft Clay Crafter");
		print("Version " + SCRIPT_VERSION);
	}
    
	public int main()
	{
		if(getFightMode() != fmode)
		{
			setFightMode(fmode);
		}
		
		if(isInBank())
		{
			if(isBanking())
			{
				if(isRunningAsTask && hasCompleted()) { endTask(); return 666;}
				
				if(getInventoryCount(1263) < 1)
				{                
					withdraw(1263,1);
					return 1000;
				}
				if(getInventoryCount(cookedPot) > 0)
				{                
					deposit(cookedPot,getInventoryCount(cookedPot));
					return 200;
				}
				if(getInventoryCount(cookedPieDish) > 0)
				{                
					deposit(cookedPieDish,getInventoryCount(cookedPieDish));
					return 200;
				}
				if(getInventoryCount() < 30)
				{                
					withdraw(softClay,50);
					return 200;
				}
				
				if(getInventoryCount(softClay) > 0)
				{
					closeBank();
					return 1000;
				}
			}
			
			if(isQuestMenu())
			{
				answer(0);
				return 3500;
			}   
			
			if(getInventoryCount(softClay) > 0)
			{
				walkTo(586,586);
				return 1000;
			}
			else
			{		
				int banker[] = getNpcByIdNotTalk(new int[]{bankerId});										
				if (banker[0] != -1 && !isBanking())
				{						
					talkToNpc(banker[0]);						
					return 2500;
				}
			}
		}
		else if(isInPotteryRoom())
		{ 
			if(getFatigue() > 70)
			{
				useSleepingBag();
				return 1000;
			}
			
			if(getInventoryCount(softClay) > 0)
			{
				if(!isQuestMenu()) 
				{
					useItemOnObject(softClay, potterWheel);
					return 1000;		
				} 
				else 
				{
					if(getLevel(12) >= 4)
						answer(0);
					else
						answer(1);
					return 3000;
				}	
			}
			
			if(getInventoryCount(unfiredPot) > 0)
			{
				useItemOnObject(unfiredPot, potteryOven);
				return 500;
			}
			if(getInventoryCount(unfiredPieDish) > 0)
			{
				useItemOnObject(unfiredPieDish, potteryOven);
				return 500;
			}
			
			//Done on this batch, walkout
			walkTo(606,593);
			return 500;
		}
		else
		{
			if(getInventoryCount(softClay) > 0 || getInventoryCount(unfiredPieDish) > 0 || getInventoryCount(unfiredPot) > 0)
			{
				//Walk to Pottery
				
				if(getX() < 594)// East Side
				{
					if(getY() < 586)
					{
						walkTo(586,586);//Sink Building Door
						return 300;
					}
					
					if(getY() > 603)
					{
						walkTo(594,603);//Bridge
						return 300;
					}
				}
				
				walkTo(607,591); //Pottery room
				return 300;
			}
			else
			{
				//WalkToBank
				if(getX() > 594)
				{
					walkTo(594,603);//Bridge
					return 300;
				}
				if(getY() > 586)
				{
					walkTo(586,586);//Sink Building Door
					return 300;
				}
				
				walkTo(581,573); //Bank
				return 300;
			}
			
		}
		return 300;
	}
	
	private boolean isInBank()
	{
		if(getX() >= 577 && getX() <= 585 && getY() >= 572 && getY() <= 576) return true;
		
		return false;
	}
	
	private boolean isInPotteryRoom()
	{
		if(getX() >= 604 && getX() <= 610 && getY() >= 589 && getY() <= 592) return true;
		
		return false;
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
		if(getLevel(12) >= 10) return true;
		
		return false;
	}
	
	@Override
	public void endTask()
	{
		depositAll();
		print("Task ended : Crafting to level 10");
		
		switchToScript(taskingDetails.MainScriptName,taskingDetails.MainScriptParams);
	}
}