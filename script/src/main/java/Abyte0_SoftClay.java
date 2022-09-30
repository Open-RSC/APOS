//2022-03-27 V1.0 Support Tasking
public class Abyte0_SoftClay extends Abyte0_Script implements ITaskScript
{
	private final String SCRIPT_VERSION = "1.0";
	
	int fmode = 2;
	int gnomeBall = 981;
	
	int bankerId = 95;
	
	int bucket = 21;
	int clay = 149;
	int softClay = 243;
	int sink = 48;
	int waterBucket = 50;
	
	String barType;
	
	int[] doorObj;

	public Abyte0_SoftClay(Extension e)
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

		if(str.length >= 1)
		{
			fmode = Integer.parseInt(str[0]);
			print("fmode set to : " + FIGHTMODES[fmode]);
			
			String[] taskingExtraParams = taskingDetails.TaskExtraInputParams.split(",");
			if(taskingExtraParams[0].contains("targetQuantity="))
			{
				targetQuantity = Integer.parseInt(taskingExtraParams[0].substring(15));
			}
		}
		
		print("Abyte0_SoftClay Ardougne Soft Clay Maker");
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
				
				if(getInventoryCount(softClay) > 0)
				{                
					deposit(softClay,getInventoryCount(softClay));
					return 200;
				}
				if(getInventoryCount(bucket) == 0 && getInventoryCount(waterBucket) == 0)
				{                
					withdraw(bucket,1);
					withdraw(waterBucket,1);
					return 1000;
				}
				if(getInventoryCount() < 30)
				{                
					withdraw(clay,50);
					return 200;
				}
				if(getInventoryCount() == 30 && getInventoryCount(clay) < 25)
				{                
					depositAll();
					return 2000;
				}
				
				
				if(getInventoryCount(clay) > 0)
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
			
			if(getInventoryCount(clay) > 0)
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
		else
		{
			if(isInSinkRoom())
			{
				if(getInventoryCount(bucket) > 0)
				{
					useItemOnObject(bucket, sink);
					return random(640, 1809);
				}
				if(getInventoryCount(waterBucket) > 0 && getInventoryCount(clay) > 0)
				{
					useItemWithItem(getInventoryIndex(waterBucket), getInventoryIndex(clay));
					return random(640, 1809);
				}
				
				//GetOut
				boolean doorWasClosed = OpenDoorIfClosed(2,581,586);
				if(doorWasClosed) return 400;
				
				walkTo(583,586);
				return 500;				
			}
			
			
			if(isInMiddleRoom())
			{
				//ToToSink
				if(getInventoryCount(clay) > 0)
				{
					boolean doorWasClosed = OpenDoorIfClosed(2,581,586);
					if(doorWasClosed) return 400;
					
					walkTo(580,585);
					return 500;		
				}
				
				//Get Out
				boolean doorWasClosed = OpenDoorIfClosed(2,586,586);
				if(doorWasClosed) return 400;
				
				walkTo(586,586);
				return 500;				
			}
			
			//else between bank and sink building
			if(getInventoryCount(clay) > 0)
			{
				if(getX() == 586 && getY() == 586)
				{
					//Get inside
					boolean doorWasClosed = OpenDoorIfClosed(2,586,586);
					if(doorWasClosed) return 400;
					
					walkTo(585,586);
					return 500;		
				}
				walkTo(586,586);
			}
			else
			{
				//walk to bank
				walkTo(581,573);
				return 500;
			}
		}
		
		return random(400,1103);
	}
	
	public boolean OpenDoorIfClosed(int id, int x, int y)
	{
		int foundDoorId = getWallObjectIdFromCoords(x,y);
		if(foundDoorId == id)
		{
			atWallObject(x, y);
			return true;
		}
		
		return false;
	}
	
	private boolean isInBank()
	{
		if(getX() >= 577 && getX() <= 585 && getY() >= 572 && getY() <= 576) return true;
		
		return false;
	}
	
	private boolean isInSinkRoom()
	{
		if(getX() >= 578 && getX() <= 580 && getY() >= 584 && getY() <= 587) return true;
		
		return false;
	}
	
	private boolean isInMiddleRoom()
	{
		if(getX() >= 581 && getX() <= 585 && getY() >= 584 && getY() <= 587) return true;
		
		return false;
	}
	
	@Override
	public String getSctiptVersion()
	{
		return SCRIPT_VERSION;
	}
	
	private boolean isRunningAsTask = false;
	private TaskParameters taskingDetails;
	int targetQuantity = 1000000;
	
	@Override
	public boolean hasCompleted()
	{
		if(bankCount(softClay) >= targetQuantity) return true;
		if(bankCount(clay) == 0 && getInventoryCount(clay) == 0) return true;
		
		return false;
	}
	
	@Override
	public void endTask()
	{
		depositAll();
		print("Task ended : Soft Clay");
		
		switchToScript(taskingDetails.MainScriptName,taskingDetails.MainScriptParams);
	}
}