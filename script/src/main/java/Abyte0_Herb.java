
//2023-03-18 - 3.0 Added Cleaning basic herbs and mixing with water when available

public class Abyte0_Herb extends Abyte0_Script
{
	private final String SCRIPT_VERSION = "3.0 - Mix Basic Herbs to save time";
	
	int fmode = 2;
	int loop = 0;	
	int i = 0;
	
	int FireRune = 31;
	int WaterRune = 32;
	int AirRune = 33;
	int EarthRune = 34;
	int MindRune = 35;
	int deathRunes = 38;
	int nature = 40;
	int law = 42;
	int Disk = 387;
	int Pumpkin = 422;
	int crystalKey = 525;
	int halfKey1 = 526;
	int halfKey2 = 527;
	int ChristmasCracker = 575;
	int PartyHat0 = 576; 
	int PartyHat1 = 577;
	int PartyHat2 = 578;
	int PartyHat3 = 579;
	int PartyHat4 = 580;
	int PartyHat5 = 581;
	int DragonMediumHelmet = 795;
	int halloweenMask0 = 828;
	int halloweenMask1 = 831;
	int halloweenMask2 = 832;
	int Santa = 971;
	int DragonBoneCertificate = 1270;
	int HalfDragonSquareShield1 = 1276;
	int HalfDragonSquareShield2 = 1277;
	int DragonSquareShield = 1278;
	
	int[] herbs = {ChristmasCracker,PartyHat0,PartyHat1,PartyHat2,PartyHat3,PartyHat4,PartyHat5,
	Disk,Pumpkin,halloweenMask0,halloweenMask1,halloweenMask2,DragonBoneCertificate,
	Santa,FireRune,WaterRune,AirRune,DragonMediumHelmet,HalfDragonSquareShield1,HalfDragonSquareShield2,DragonSquareShield,
	deathRunes,nature,law,crystalKey,halfKey1,halfKey2, 439, 440, 441, 442, 443, 815, 817, 819, 821, 823, 933, 526, 527, 157, 158, 465, waterVial, dirtyGuam, dirtyTarromin, dirtyHarralander, dirtyRanarr, unfinishGuam, unfinishTarromin, unfinishHarralander,unfinishRanarr, cleanGuam, cleanTarromin, cleanHarralander, cleanRanarr};//40 nature, 42 law, 157 158 uncut ruby diamon 526 527 key  (464 465 vials)
	int[] path = null;
	public Abyte0_Herb(Extension e) {		super(e);	}
	public void init(String params)
	{
		if(params.equals("1"))
			fmode = 1;
		else if(params.equals("2"))
			fmode = 2;
		else if(params.equals("3"))
			fmode = 3;
		else
			fmode = 0;
			
		System.out.println("Fmode = " + fmode);
		System.out.println("Herb Collector - By mofo");
		System.out.println("Edited by Abyte0");
		System.out.println("Version 3.0 - Added");
		//Version 0 by yomama
		//Version 1 by Abyte0
		//Pick Up Nats
		//Choose Fmode (Attack default)
		//Version 2 by Abyte0
		//Walk Back
		//r1 pickup vials
	}
	public int main()
	{
		if(getFightMode() != fmode)
		setFightMode(fmode);
	
		if(getCurrentLevel(3) <= 5)
	    {
            return random(60000,90000);
		}
	
		if(getFatigue() > 90)
		{
			useSleepingBag();
			return 3000;
		}
		if(isBanking()) 
		{
			i = 0;
			for(int h = 0; h < herbs.length; h++) 
			{
				if(getInventoryCount(herbs[h]) > 0) 
				{
					statusReport(getInventoryCount(herbs[h]), h);
					deposit(herbs[h], getInventoryCount(herbs[h]));
					return random(1000, 1500);				
				}
			}
			closeBank();		
		}
		if(isAtApproxCoords(581, 574, 5) && getInventoryCount() == 30)
		{			
			i = 0;
			if(!isQuestMenu()) 
			{
				int banker[] = getNpcByIdNotTalk(new int[]{95});
				if(banker[0] != -1) 
				{					
					talkToNpc(banker[0]);					
					return random(2000, 2700);				
				}				
				return 1000;			
			} 
			else 
			{				
				answer(0);				
				return 6000;			
			}		
		}		
		if(isAtApproxCoords(617, 558, 2) && getInventoryCount() != 30) 
		{			
			i = 0;			
			int[] door = getWallObjectById(96);			
			if(door[0] != -1)				
			atWallObject2(door[1], door[2]);			
			return random(1000, 1500);		
		}		
		if(isAtApproxCoords(617, 552, 3) && getInventoryCount() == 30) 
		{			
			int[] door = getWallObjectById(96);			
			if(door[0] != -1) 
			{
				atWallObject(door[1], door[2]);
				return random(800, 1200);			
			}
			return 1000;		
		}
		if(isAtApproxCoords(617, 552, 3) && getInventoryCount() != 30)
		{
			int[] druid = getNpcById(270);
			for(int h = 0; h < herbs.length; h++)
			{
				int[] groundHerbs = getItemById(herbs[h]);
				if(groundHerbs[0] != -1)
				{
					pickupItem(groundHerbs[0], groundHerbs[1], groundHerbs[2]);
					return random(1000, 1500);
				}
			}
			if(druid[0] != -1 && !inCombat())
			{
				attackNpc(druid[0]);
				return random(800, 1300);
			}
			if(druid[0] == -1 && !inCombat())
			{
				//walkback
				if(getX() != 617 || getY() != 553)
					walkTo(617,553);
			}
			if(doHerbMixing)
			{
				int potionResult = MakeUnfinishPotion();
				if(potionResult > -1) return potionResult;
			}
			return 300;
		}
		walk();
		return random(2000, 2500);
	}

	public void statusReport(int herbCount, int arrayPoint) 
	{
		if(arrayPoint < 6) 
		{
			total[arrayPoint] = total[arrayPoint] + herbCount;
			System.out.println("**Status Report**: Collected " + total[arrayPoint] + " " + herbNames[arrayPoint] + " so far!");
		}
	}

	public int walk()
	{
		if(isAtApproxCoords(581, 574, 5) && getInventoryCount() != 30)
			path = new int[] {581, 574, 590, 578, 588, 598, 600, 595, 603, 582, 609, 565, 617, 558};
		if(isAtApproxCoords(617, 558, 2) && getInventoryCount() == 30)
			path = new int[] {617, 558, 609, 565, 603, 582, 600, 595, 588, 598, 590, 578, 581, 574};
		if((i + 1) < path.length)
		{
			if(isAtApproxCoords(path[i], path[i + 1], 2))
				i = i + 2;
			walkTo(path[i] + random(-2, 2), path[i + 1] + random(-2, 2));			
			return random(1500, 2500);		
		}		
		loop++;		
		if(loop > 5)			
			walkTo(path[i], path[i + 1]);		
		return 1000;	
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
	
	public int[] total = {0, 0, 0, 0, 0, 0};	
	public String[] herbNames = {"Ranaar", "Irit", "Avantoe", "Kwuarm", "Cadantine", "Dwarf Weed"};
}