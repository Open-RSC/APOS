//SimpleAutofighter Script Provided by RLN
//Edited by Abyte0
//2012-01-24 - Version 0 - Added Open Doors/Gates at 10 squares from you if closed
//2012-01-24 - Version 0 - Added Eating multiple food like Yomama Scripts
//2012-01-24 - Version 0 - Will Add Pick && Bury BigBones
//2022-01-04 - Version 1.0 Talk
//2011-01-20 - Version 2.0 Fixed more default food, fixed specific food, fixed normal bones [require abyte0_Script 1.7+]
//2011-01-21 - Version 2.1 Support multiple npc separated by a dot [require abyte0_Script 1.7+]

public class Abyte0_SAF extends Abyte0_Script
{

	private final String SCRIPT_VERSION = "2.1.1";
	
	@Override
	public String[] getRandomQuotes()
	{
		String[] result = {"Wow!, 0 - 0 - 0 - 0 - 0 - Dead!","Bam! @mag@1 hit that shit!","Hit!","Anybody out there?","They are so strong!","Whats does the Attack option does?","You so fast!","I saw a bot at chickens last week","Noob!","Why are you using a scimitar?","Do you like r2h?","Almost 99 strength","Are you pure?"};
		return result;
	}

	//default Config
    boolean walkBack = false;
	boolean bury = false;
	int targetFmodeLevel = 100;

    //abyte0_saf f=3,n=321,r=25,h=20,e=373,w=1,b=1
	//f=0,n=3,w=1,p=381  for Chicken controlled

    public Abyte0_SAF (Extension e) {
        super(e);
    }

    public void init(String params) {

		print("Abyte0_SAF Version " + SCRIPT_VERSION);
		
		printHelp();
		loadAllFoods();
	
        String[] in;

        try {

            in = params.trim().toLowerCase().split(",");

            for(int i = 0; i < in.length; i++) {

                if(in[i].startsWith("f=")) {

                    fmode = Integer.parseInt(in[i].substring(2));

                    if(fmode < 0 || fmode > 3)

                        throw new Exception("Invalid fight mode (" + fmode + ")");

                        
					print("fmode set " + fmode);

                    continue;

                }

                if(in[i].startsWith("n=")) {

					String[] npcs = (in[i].substring(2)).split("[.]");
					npcIDs = new int[npcs.length];
				
					for(int j = 0; j < npcs.length; j++)
					{
						npcIDs[j] = Integer.parseInt(npcs[j]);
					}

                    continue;

                }

				if(in[i].startsWith("p=")) {

                    int flag = Integer.parseInt(in[i].substring(2));

                    if(flag < 0 || flag > 10000)
                        throw new Exception("Invalid pickup id ");

                    pickupID = flag;
					isPickupEnabled = true;
					
					print("Pickup enabled for item " + pickupID);
					
                    continue;
                }

                if(in[i].startsWith("r=")) {

                    radius = Integer.parseInt(in[i].substring(2));

					print("radius set " + radius);
					
                    continue;

                }
				
                if(in[i].startsWith("stopat=")) {

					print("Reached " );
                    targetFmodeLevel = Integer.parseInt(in[i].substring(7));

					print("Script will stop once " + FIGHTMODES[fmode] + " will have reached " + targetFmodeLevel);
					print("@ran@DO NOT USE THIS 'StopAt' FEATURE ON AGRESSIVE NPC");
					
                    continue;

                }
				
                if(in[i].startsWith("s=")) {

                    sleepAt = Integer.parseInt(in[i].substring(2));

                    if(sleepAt < 0 || sleepAt > 100)

                        throw new Exception("Invalid sleep at (" + sleepAt + ")");

                        
					print("sleepAt set " + sleepAt);

                    continue;

                }

                if(in[i].startsWith("h=")) {

                    eatAt = Integer.parseInt(in[i].substring(2));

                    if(eatAt < 0 || eatAt > 100)

                        throw new Exception("Invalid eat at (" + eatAt + ")");

                    
					print("eatAt set " + eatAt);


                    continue;

                }

                if(in[i].startsWith("e=")) {

                    foodIDs = new int[] {Integer.parseInt(in[i].substring(2))};

					print("foodIDs set " + in[i].substring(2));
					
                    continue;

                }
				
				if(in[i].startsWith("b=")) {

                    int flag = Integer.parseInt(in[i].substring(2));

                    if(flag != 0 && flag != 1)

                        throw new Exception("Invalid bury flag (" + flag + ") must be 1 for true, 0 for false");

                    bury = flag == 1;
					if(bury)
						print("we bury bones");
					else
						print("we DO NOT bury bones");
					
					continue;
					 
                }

                if(in[i].startsWith("w=")) {

                    int flag = Integer.parseInt(in[i].substring(2));

                    if(flag != 0 && flag != 1)

                        throw new Exception("Invalid walkback flag (" + flag + ")");

                    walkBack = (flag == 0 ? false : true);

					print("walkBack set " + walkBack);
					
					continue;
					
                }

                throw new Exception("parsing fucked up");

            }
			
        } catch (Exception _ex) {

            System.out.println("Error while initiating simple autofighter, invalid input");

            System.out.println("Type in: \t\"n=\" for npc id");

            System.out.println("\t\t\"f=\" for fight mode");

            System.out.println("\t\t\"s=\" to sleep at specified fatigue (default is 90)");

            System.out.println("\t\t\"r=\" for radius (don't include if you don't need)");

            System.out.println("\t\t\"h=\" to eat at specified hp percent, ie: 5/10 hp is 50% so the param is 50 (must be set in order to autoeat)");

            System.out.println("\t\t\"e=\" to set food ID (default is lobsters)");

            System.out.println("\t\t\"w=\" for walkback (0 = off, 1 = on - will walk back to coords that the script started at)");

            System.out.println("when you are typing your input, separate your variables with a comma, ie:");

            System.out.println("\"simpleautofighter n=11,f=1,r=20\" \n ^^ that would fight men (11), in aggressive mode(1) (no spaces, case insensitive, in any order)");

            System.out.println("Error message: " + _ex.getMessage());

            stopScript();

            in = null;

        }

		setAutoLogin(true);
	
        if(fmode == -1) {

            System.out.println("No fight mode set, type f=fmode in the params");

			setAutoLogin(false);
            stopScript();

        }

        if(npcIDs == null) {

            System.out.println("No npc id set, type n=npcid in the params, support multiple with dot");

			setAutoLogin(false);
            stopScript();

        }
		
	
		initialXp = getFmodeXp(fmode);
		initialTime = System.currentTimeMillis();
		
		print("@mag@type --help in public chat to view help while script is running");
	
    }

	public final void BuryBone(int boneId)
	{
		int boneIndex = getInventoryIndex(boneId);
		System.out.println("Bury Bones at position : " + boneIndex);
		useItem(boneIndex);
	}
	
	public int TryPickup()
	{
		if(isPickupEnabled && getInventoryCount() < 30)
		{
			int[] item = getItemById(pickupID);
			if(item[0] != -1)
			{
				
				int difX = item[1] - startX;
				int difY = item[2] - startY;
				
				if(difX <= radius && difX >= -radius && difY <= radius && difY >= -radius)
				{
					pickupItem(item[0], item[1], item[2]);
					return random(200,300);
				}
			}
		}
		return -1;
	}
	
	public int TryBury()
	{
		if(bury)
		{
			int anyBigBones = tryBury(bigBonesID);
			if(anyBigBones > 0) return anyBigBones;
			
			int anyNormalBones = tryBury(normalBonesID);
			if(anyNormalBones > 0) return anyNormalBones;
		}
		return -1;
	}
	
	private int tryBury(int boneId)
	{
		
		if(getInventoryCount(boneId) > 0)
		{
			if(inCombat())
			{
				RunFromCombat();
				return 150;
			}
			BuryBone(boneId);
			return 250;
		}
		int[] groundBone = getItemById(boneId);
		if(groundBone[0] != -1)
		{
			if(groundBone[1] == getX() && groundBone[2] == getY())
			{
				if(inCombat())
				{
					RunFromCombat();
					return 150;
				}
				
				pickupItem(groundBone[0], groundBone[1], groundBone[2]);
				return 250;
			}
		}
		
		return -1;
	}
	
    public int getUntrapped()
    {
		for(int i = 1; i < 8; i++)
		{
			int result = getUntrappedByMaxDistance(i);
			if(result != 0) return result;
		}
		
		return 0;
	}
	
	public int getUntrappedByMaxDistance(int maxDistance)
    {
        //Gate
        int[] Gate = getObjectById(57);
        if(Gate[0] != -1)
        {
			if(isAtApproxCoords(Gate[1], Gate[2],maxDistance))
			{
				atObject(Gate[1], Gate[2]);
				return random(800,900);
			}
        }
        //Chicken gate lumb
        int[] ChickGate = getObjectById(60);
        if(ChickGate[0] != -1)
        {
			if(isAtApproxCoords(ChickGate[1], ChickGate[2],maxDistance))
			{
				atObject(ChickGate[1], ChickGate[2]);
				return random(800,900);
			}
        }
        //BankDoor
        int[] BankDoor = getObjectById(64);
        if(BankDoor[0] != -1)
        {
			if(isAtApproxCoords(BankDoor[1], BankDoor[2],maxDistance))
			{
				atObject(BankDoor[1], BankDoor[2]);
				return random(800,900);
			}
        }

		//Regular Door
		int[] Door = getWallObjectById(2);
			if(Door[0] != -1)
			{
				if(isAtApproxCoords(Door[1], Door[2],maxDistance))
				{
					atWallObject(Door[1], Door[2]);
					return random(800,900);
				}
			}

	//if no door found to be closed we continue
	return 0;
    }

	
	

    public int main() {

		SayRandomQuote();
        if(startX == -1 || startY == -1) {

            startX = getX();

            startY = getY();

        }
		//print("fmode is :" + fmode);
		//print("get fmode return :" + getFightMode());
        if(getFightMode() != fmode) {

            setFightMode(fmode);

            return 100;

        }

        if(getFatigue() >= sleepAt) {

            useSleepingBag();

            return 1;

        }

        if(!inCombat()) {

            if(getCurrentLevel(3) <= eatAt)
			{
				if(EatFood(foodIDs))
					return 1500;
				else //no food found
				{
					if(getHpPercent() < 10) 
					{
						System.out.println("hp is dangerously low with no food.");
						setAutoLogin(false);
						stopScript();                
						return 0;
					}
					else
					{
						print("Waiting a minute to heal");
						return 60000;
					}
				}
            }

            //We look if trapped else we continue
            int unTrap = getUntrapped();
            if(unTrap != 0)
                return unTrap;
	
			int nombre = TryBury();
			if(nombre != -1)
				return nombre;

			int pickReturn = TryPickup();
			if(pickReturn != -1)
				return pickReturn;

			if(targetFmodeLevel == getFmodeLevel(fmode))
			{
				setAutoLogin(false);
				stopScript();
			}
		
            int npc[];
			for(int i = 0; i < npcIDs.length; i++)
			{
				if(radius != Integer.MAX_VALUE)
					npc = getNpcInRadius(npcIDs[i], startX, startY, radius);
				else
					npc = getNpcById(npcIDs[i]);

				if(npc[0] != -1)
				{
					attackNpc(npc[0]);

					return 300;
				}
			}

            if(!isAtApproxCoords(startX, startY, 0) && walkBack) {

                walkTo(startX, startY);

                return random(900, 1100);

            }

        }

        return 200;

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
    public void onChatMessage(String msg, String name, boolean pmod, boolean jmod) {
		
		//Do not reply to yourself
		//final String lname = client.getPlayerName(client.getPlayer());		
        //if(name.equalsIgnoreCase(lname)) return;
		
		String receivedLC = msg.toLowerCase();
		
		final String lname = client.getPlayerName(client.getPlayer());		
        if(name.equalsIgnoreCase(lname))
		{
			
		}
		
		super.onChatMessage(msg, name, pmod, jmod);
    }

	@Override
	public String getSctiptVersion()
	{
		return SCRIPT_VERSION;
	}
	
	@Override
	protected void printHelp()
	{
		print("Press # or ' or type --status to display xp stats");
		
		print("type --help in public chat to view help");
		print("type --param in public chat to view currently runnign parameters");
		
		print("USAGE = abyte0_saf f=3,stopAt=98,n=321,r=25,h=20,e=373,w=1,b=1");
		print("Result= Train Def to lvl 98 on guard in radius of 25 + eat lob at 20hp + bury + walkback");
	}
	
	@Override
	protected void printParams()
	{
		print("Fmode is " + FIGHTMODES[fmode]);
		if(targetFmodeLevel < 100)
			print(FIGHTMODES[fmode] + " will stop once reached " + targetFmodeLevel);
		
		if(bury)
			print("Script bury normal & big bones");
		
		if(eatAt < 100)
		{
			if(foodIDs.length > 1)
				print("Script use most known food to eat when hp <= " + eatAt);
			else
				print("Script use food : " + foodIDs[0] + " to eat when hp <= " + eatAt);
		}
		else
			print("Script won't eat");
		
		if(pickupID > 0)
		{
			print("Script will pickup " + pickupID);
		}
		
		if(npcIDs.length > 0)
		{
			String npcs = "Script will fight ";
			for(int i = 0; i < npcIDs.length; i++)
				npcs += npcIDs[i] + " ";
			print(npcs);
		}
		
		if(walkBack)
		{
			print("Script will walkback to initial position when waiting");
		}
		
		print("Script npc scan radius set to : " + radius);
		
	}
	
	private void loadAllFoods()
	{
		foodIDs = new int[ALL_KNOWN_FOODS.length];		
		for(int i = 0; i < ALL_KNOWN_FOODS.length; i++)
			foodIDs[i] = ALL_KNOWN_FOODS[i];			
	}
	
    int fmode = -1;
    int[] npcIDs;
    int sleepAt = 90;
    int radius = Integer.MAX_VALUE;
    int startX = -1;
    int startY = -1;
    int eatAt = 5;
    int normalBonesID = 20;
    int bigBonesID = 413;
	int pickupID = -1;
	boolean isPickupEnabled = false;
    int[] foodIDs;

	int initialXp = 0;
	long initialTime = 0;
	
}