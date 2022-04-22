//Based on the Script HARRY provided on forum (fletch)
//Edited by Abyte0
//2012-01-24 - Added WalkBack
//2022-03-28 V1.0 - Support Tasking

public class Abyte0_Shaft extends Abyte0_Script implements ITaskScript
{
	
	boolean doingBow = false;
	private int radiusLimit = 25;
	
	private final String SCRIPT_VERSION = "1.1";
	int x,y;
	int fmode = 2;

	public Abyte0_Shaft ( Extension e )
	{
		super(e);
	}

	public void init (String params)
	{
		if(params.contains(TaskParameters.TASKING_HEADER))
		{
			isRunningAsTask = true;
			print("Abyte0_Shaft [Running as task]");
			
			taskingDetails = TaskParameters.buildFromString(params);
			
			
			params = taskingDetails.TaskInputParams;
			print("param left:"+params);
			
			doingBow = false;
		}
		
		String str[]=params.toLowerCase().split(",");

		if(str.length >= 1)
		{
			fmode = Integer.parseInt(str[0]);
			print("fmode set to : " + FIGHTMODES[fmode]);
			
			if(str.length > 1)
			{
				doingBow = str[1].equals("bow") || str[1].equals("bows");
				if(doingBow)
					print("doing bows");
				else
					print("doing shaft");
			}
		}
		
		x = getX();
		y = getY();
		print("Version " + SCRIPT_VERSION);
		print("WalkBack at " + x + "," + y);
	}

	public int main( )
	{
		if(getFightMode() != fmode)
		{
			setFightMode(fmode);
		}   
		if( getFatigue() > 90 )
		{
			useSleepingBag();
			return random(1000,2000);
		}
		
		if(isRunningAsTask && hasCompleted()) { endTask(); return 666;}
		
		if ( getInventoryIndex(277) > 0 ) //Shortbow
		{
			dropItem(getInventoryIndex(277));
			return 1000;
		}
      
		if ( getInventoryIndex(276) > 0 ) //Longbow
		{
			dropItem(getInventoryIndex(276));
			return 1000;
		}
      
		if(isQuestMenu())
		{
			if(doingBow && getLevel(9) >= 10)
				answer(2);
			else if(doingBow && getLevel(9) < 10)
				answer(1);
			else
				answer(0);
		
			return 500;
		}
      
      if ( getInventoryIndex(14) > 0 )
      {
         useItemWithItem(getInventoryIndex(13), getInventoryIndex(14));
         return 1000;
      }
        
      int[] tree = getObjectById(new int[]{0, 1});  

      if( tree[0] != -1 && getInventoryCount(14) <= 0)
      {
		if(getX() < x-radiusLimit || getX() > x+radiusLimit || getY() < y-radiusLimit || getY() > y+radiusLimit)
		{
			walkTo(x,y);
			return 1000;
		}
		else
			atObject(tree[1], tree[2]);
         return 500;
      }
            
      return random(1000,2000);
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
		if(getLevel(8) >= 36) return true;
		
		return false;
	}
	
	@Override
	public void endTask()
	{
		print("Task ended : Woodcut to level 36");
		
		switchToScript(taskingDetails.MainScriptName,taskingDetails.MainScriptParams);
	}
}