//2022-03-26 0.1 Beta working if we have clays
//2022-04-02 0.2 Added Auto Mining Support with Any Pickaxe
//2022-04-03 0.3 Support Any Woodcut Axe
public class Abyte1_Prep_LostCity extends Abyte0_Script
{
	
	private final String SCRIPT_VERSION = "0.3.2";
	
	private PathWalker pw;
	private PathWalker.Path pathNodes;
	int fightMode = 1;
	
	int bankerId = 95;
	int bobId = 1;
	
	int clay = 149;
	int softClay = 243;
	int knife = 13;
	int[] axes = new int[]{87,12,428,88,203,204,405};
	int requiredClayQuantity = 100;
	
	public Abyte1_Prep_LostCity(Extension e)
	{
		super(e);
	}    
	public void init( String params )
	{
		String str[]=params.split(",");

		if(str.length >= 1)
		{
			fightMode = Integer.parseInt(str[0]);
			if(str.length > 1 && str[1].contains("initialTime="))
			{
				initialTime = Long.parseLong(str[1].substring(12));
			}
		}
		
		print("Require in bank: Sleepbag, Bucker or Water Bucket, Woodcut axe, Knife, Pickaxe");
		printParams();
		
		if(initialTime== -1)
			initialTime = System.currentTimeMillis();
	}
    
	private String getScriptParameters()
	{
		return ""+fightMode+",initialTime="+initialTime;
	}
	
	private boolean isWalking = false;
	
	public int main()
	{
		if(getFightMode() != fightMode)
		{
			setFightMode(fightMode);
		}
		
		
		if(getLevel(12) < 10)
		{
			if(isInArdougneNorthBank())
			{
				isWalking = false;
				pw = null;
				pathNodes = null;
				
				if(!isBanking())
				{
					if(isQuestMenu())
					{
						answer(0);
						return 3000;
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
				
				if(getInventoryCount(softClay) > 0)
				{
					deposit(softClay,50);
					return 3000;
				}
				if(getInventoryCount(clay) > 0)
				{
					deposit(clay,50);
					return 3000;
				}
				
				if(bankCount(softClay) >= requiredClayQuantity)
				{
					
					print("Switching to Task : Crafting Pots");
					switchToScript("Abyte0_CraftPot", TaskParameters.TASKING_HEADER + "MainScriptName=Abyte1_Prep_LostCity;MainScriptParams=" + getScriptParameters() + ";TaskInputParams=" + fightMode);
					
					return 666;
				}
				
				if(bankCount(clay) >= requiredClayQuantity)
				{
					print("Switching to Task : Soft Clay");
					switchToScript("Abyte0_SoftClay", TaskParameters.TASKING_HEADER + "MainScriptName=Abyte1_Prep_LostCity;MainScriptParams=" + getScriptParameters() + ";TaskInputParams=" + fightMode + ";TaskExtraInputParams=targetQuantity="+requiredClayQuantity);
				
					return 666;
				}
				
				print("Walking to Yanille bank");		
				pw = new PathWalker(client);
				pw.init(null);
				pathNodes = pw.calcPath(getX(), getY(), 585, 752);
				pw.setPath(pathNodes);
				isWalking = true;
				walkTo(581,570);
				return 5000;
				
			}
			if(isInYanilleBank())
			{
				isWalking = false;
				pw = null;
				pathNodes = null;
				
				if(!isBanking())
				{
					if(isQuestMenu())
					{
						answer(0);
						return 3000;
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
				
				if(bankCount(clay) < requiredClayQuantity)
				{
					print("Switching to Task : Mining Clay");
					switchToScript("Abyte0_YanilleMiner", TaskParameters.TASKING_HEADER + "MainScriptName=Abyte1_Prep_LostCity;MainScriptParams=" + getScriptParameters() + ";TaskInputParams=" + fightMode + ",clay;TaskExtraInputParams=targetQuantity="+requiredClayQuantity);
					return 666;
				}
				else
				{
					walkTo(584,752);
					return 5000;
				}
			}
			
			if(!isWalking)
			{
				print("Walking to Ardougne bank");		
				pw = new PathWalker(client);
				pw.init(null);
				pathNodes = pw.calcPath(getX(), getY(), 581, 574);
				pw.setPath(pathNodes);
				isWalking = true;
				return 5000;
			}
			else
			{
				pw.walkPath();
				return 100;
			}
		}
		
		if(getLevel(12) < 31)
		{
			if(isInArdougneNorthBank() && (getInventoryCount(1263) != 1 || getInventoryCount() != 1))
			{
				if(!isBanking())
				{
					if(isQuestMenu())
					{
						answer(0);
						return 3000;
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
				if(getInventoryCount(1263) < 1)
				{
					withdraw(1263,1);
					return 500;
				}
				
				if(getInventoryCount() != 1)
				{
					depositAll();
					return 1000;
				}
				
				closeBank();
				return 300;
			}
			
			if(isNearGnomes())
			{
				
				isWalking = false;
				pw = null;
				pathNodes = null;
				
				print("Switching to Task : Power Flaxing to 31");
				switchToScript("Abyte0_Flax", TaskParameters.TASKING_HEADER + "MainScriptName=Abyte1_Prep_LostCity;MainScriptParams=" + getScriptParameters() + ";TaskInputParams=power");
				
				return 666;
				
			}
			else
			{
				if(!isWalking)
				{
					print("Walking to gnomes");		
					pw = new PathWalker(client);
					pw.init(null);
					pathNodes = pw.calcPath(getX(), getY(), 696, 522);
					pw.setPath(pathNodes);
					isWalking = true;
					return 5000;
				}
				else
				{
					if(isQuestMenu())
					{
						answer(1);
						return 5000;
					}
					pw.walkPath();
					return 100;
				}
			}
		}
		
		if(getLevel(8) < 36)
		{
			if(getInventoryCount(1263) >= 1 && getInventoryCount(axes) >= 1 && getInventoryCount(knife) >= 1 && getInventoryCount() < 25)
			{
				if(isNearShaftingSpot())
				{
					isWalking = false;
					pw = null;
					pathNodes = null;
					
					print("Switching to Task : Shafting");
					switchToScript("Abyte0_Shaft", TaskParameters.TASKING_HEADER + "MainScriptName=Abyte1_Prep_LostCity;MainScriptParams=" + getScriptParameters() + ";TaskInputParams=" + fightMode);
					
					return 666;
				}
				else
				{
					//walk to shafting spot
					if(!isWalking)
					{
						print("Walking to Shafting Spot");		
						pw = new PathWalker(client);
						pw.init(null);
						pathNodes = pw.calcPath(getX(), getY(), 180, 666);
						pw.setPath(pathNodes);
						isWalking = true;
						return 5000;
					}
					else
					{
						pw.walkPath();
					}
					
					return 100;
				}
			}
			
			if(isNearDrainorBank())
			{
				isWalking = false;
				pw = null;
				pathNodes = null;
				
				if(!isBanking())
				{
					if(isQuestMenu())
					{
						answer(0);
						return 3000;
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
				
				if(getInventoryCount(1263) < 1)
				{
					withdraw(1263,1);
					return 700;
				}
				
				if(getInventoryCount(knife) < 1)
				{
					withdraw(knife,1);
					return 700;
				}
				
				if(getInventoryCount(axes) != 1)
				{
					for(int i = 0; i < axes.length; i++)
					{
						if(bankCount(axes[i]) > 0)
						{
							withdraw(axes[i],1);
							return 1000;
						}
					}
				}
				
				if(getInventoryCount() != 3)
				{
					depositAll();
					return 1000;
				}
				
				closeBank();
				return 300;
			}
			else
			{
				//walkto drainor bank
				if(!isWalking)
				{
					print("Walking to Dranor bank");		
					pw = new PathWalker(client);
					pw.init(null);
					pathNodes = pw.calcPath(getX(), getY(), 218, 636);
					pw.setPath(pathNodes);
					isWalking = true;
					return 5000;
				}
				else
				{
					pw.walkPath();
					return 100;
				}
			}
		}
		
		print("@or3@SCRIPT COMPLETED TASKS");
		reportXpChange();
		setAutoLogin(false);
		logout();
		stopScript();
		
		return 500;
	}
	
	private boolean isInArdougneNorthBank()
	{
		if(getX() >= 577 && getX() <= 585 && getY() >= 572 && getY() <= 576) return true;
		
		return false;
	}
	
	public boolean isInYanilleBank()
	{
		if(getX() >= 585 && getX() <= 590 && getY() >= 750 && getY() <= 758) return true;
		
		return false;
	}
	
	private boolean isNearGnomes()
	{
		return isAtApproxCoords(696, 522, 6);
	}
	
	private boolean isNearDrainorBank()
	{
		return isAtApproxCoords(219, 636, 15);
	}
	
	private boolean isNearShaftingSpot()
	{
		return isAtApproxCoords(180, 666, 15);
	}
	
    public void onKeyPress(int keyCode) {
		if (keyCode == 192 || keyCode == 128) { //# or '
			reportXpChange();
        }
    }
    
    @Override
    public void onChatMessage(String msg, String name, boolean pmod, boolean jmod) {
		
		super.onChatMessage(msg, name, pmod, jmod);
    }

	@Override
	protected void reportXpChange()
	{
		long timeSpan = System.currentTimeMillis() - initialTime;
		long secondSpan = timeSpan / 1000;
		//long xpRatio = xpDifference * 3600L / secondSpan; //The L set 3600 as long variable Forcing to calculate as long to avoid overflow
		//long thieveCount = (long)(xpDifference / 151.75);

		long minutesSpan = secondSpan / 60L;
		print("=================================");
		print("time running: " + minutesSpan + " minutes");
		print("=================================");
	}
	
	@Override
	protected void printHelp()
	{
		super.printHelp();
		
		print("Params = fmodeid");
		print("Example= 3 for defence");
		
	}
	
	@Override
	protected void printParams()
	{
		print("Fmode is @or3@" + FIGHTMODES[fightMode]);
	}
	
	@Override
	public String getSctiptVersion()
	{
		return SCRIPT_VERSION;
	}
	
	long initialTime = -1;
}