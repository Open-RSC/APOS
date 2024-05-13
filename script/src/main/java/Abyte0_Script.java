/*
	Abyte0
	2012-02-16
	Version 1.3 - 2012-05-04
	Version 1.4 - 2021-06-22 Update to OpenApos
	Version 1.5 - 2021-12-01 Talk + DateTime
	Version 1.5.1 - 2021-12-29 Call the base class then override the general chat
	Version 1.6 - 2022-01-08 Script version
	Version 1.6.1 - 2022-01-12 support query about base script version
	Version 1.6.2 - 2022-01-20 fmode features
	Version 1.7 - 2022-01-20 support eating multiple food by providing an array including single id for multi parts like cake 330
	Version 1.7.2 - Shark was forgotten
	Version 1.7.3 - Will reply to question 1/10 chance
	Version 1.8 - More features for user
	Version 1.8.1 - Allow swiching script
	Version 1.8.2 - Much less auto talk
	Version 1.9 - 2023-03-18 - Herblaw stuff added
	Version 2.1 - 2024-05-01 - Trying to avoid saying botting or report
	Version 2.1.1 - 2024-05-11 - Added some rares to pickup list + printin the found id
*/
import java.net.*; 
import java.io.*;
import java.util.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import com.aposbot.*;
import com.aposbot._default.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Abyte0_Script extends Storm_Script 
{
	//BEGIN Configuration
	long minimumSecondsBetweenReplys = 60; //300 mean it won't auto repply more than once ever 300s (5 minutes)
	int delayDice = 50000; //The greater the number, the less often the acocunt will randomly talk
	//END Configuration
	
	private String BASE_SCRIPT_VERSION = "2.1.1";
	
    public Extension client;
	
	public int[] ALL_KNOWN_FOODS = new int[] 
	{
		335, //cake 1/3
		333, //cake 2/3
		330, //cake 3/3
		336, // Chocolate Slice
		334, // Partial Chocolate Cake
		332, // Chocolate Cake
		895, //Swamp Toad		
		897, //King worm		
		138, //bread
		132, //Meat
		142, //wine
		373, //Lobs
		357, //Salmon
		325, // Plain Pizza
		326, // Meat Pizza
		327, // Anchovie Pizza
		328, // Half Meat Pizza
		329, // Half Anchovie Pizza
		346, // Stew
		355, // Sardine
		357, // Salmon
		359, // Trout
		362, // Herring
		364, // Pike
		367, //Tunas
		373, //Lobs
		370, //Swordfish
		546, //Shark
		551, //Cod
		553, //Mackerel
		555, //Bass
		1191, //Manta ray
		1193 //Sea turtle
	};		
		
	int oakTree = 306;
	int oakLog = 632;
	int oakLongBow = 648;
	int oakLongBowU = 658;
	
	int willowTree = 307;
	int willowLog = 633;
	int willowLongBow = 650;
	int willowLongBowU = 660;
	
	int yewTree = 309;
	int yewLog = 635;
	int yewLongBow = 654;
	int yewLongBowU = 664;
	
	int magicTree = 310;
	int magicLog = 636;
	int magicLongBow = 656;
	int magicLongBowU = 666;
	
	int bowString = 676;
			
	long lastReplyTime = 0;
	boolean waitingBeforeLastDrop = false;
	
	public static String[] PROPERTY_NAMES = new String[]{"nom", 
		"money",
	    "feathers",
	    "chaosRunes",
	    "natureRunes",
	    "ironOres",
	    "coalOres",
	    "mithOres",
	    "addyOres",
	    "runiteOres",
	    "ironBars",
		"steelBars",
		"runiteBars",
		"bowStrings",
		"yewLongU",
		"yewLong",
		"magicLongU",
		"magicLong",
		"rawLobs",
		"cookLobs",
		"rawSharks",
		"cookSharks"};
	
	public static String[] PROPERTY_NAMES_STATS = new String[]{"nom" 
		,"attack"
		,"defence"
		,"strength"
		,"hits"
		,"prayer"
		,"magic"
		,"ranged"
		,"fishing"
		,"cooking"
		,"mining"
		,"smithing"
		,"woodcut"
		,"fletching"
		,"agility"
		,"firemaking"
		,"crafting"
		,"herblaw"
		,"thieving"};
	
	
	public Abyte0_Script(Extension e) {
		super(e);
        this.client = e;
		}    
	
	public void useSleepingBag()
	{
		//sendPosition(AutoLogin.user,getX(),getY());
		printInventory();
		printStats();
		printStatsXp();
		//printBot("@mag@Thieving Xp: @or3@" + getExperience(17));
		super.useSleepingBag();
	}

	public void buyItemIdFromShop(int id, int amount)
	{
		int position = getShopItemById(id);
		if(position == -1) return;
		
		buyShopItem(position, amount);
	}
	public int getShopItemIdAmount(int id)
	{
		int position = getShopItemById(id);
		
		return getShopItemAmount(position);
	}
	
	public void print(String gameText)
	{
		System.out.println(gameText);
		printBot(gameText);
	}
	
	//* BUILDS METHODS *//
	
	public String[] getRandomQuotes()
	{
		String[] result = {""};
		return result;
	}
	
	protected boolean switchUserForNextRelog(String name) {
        
		if (name == null) {
            System.out.println("You didn't enter an account to use with autologin.");
            System.out.println("You can still use APOS, but it won't be able to log you back in if you disconnect.");
            return false;
        }
        final Properties p = new Properties();
        try (FileInputStream stream = new FileInputStream("." + File.separator + "Accounts" + File.separator + name + ".properties")) {
            p.load(stream);

			//AutoLogin login = (AutoLogin)client.getAutoLogin();
			//login.setAccount(p.getProperty("username"), p.getProperty("password"));		
			
        } catch (final Throwable t) {
            return false;
        }
		
		return true;
    }
	
    public IScript initJavaScript(String name) {
        //ScriptFrame scriptLoader = new ScriptFrame(client);
		//return scriptLoader.initJavaScript(name);
		return null;
    }

	protected boolean switchUserForNextRelog(String name, String password) {
        //clientInit.getLoginListener().setAccount(
			//AutoLogin login = (AutoLogin)client.getAutoLogin();
			//login.setAccount(name, password);
			
		return true;
    }
	
	public void SayRandomQuote()
	{
		String[] results = getRandomQuotes();
		
		if(random(0,delayDice) != 1)
			return;
		
		int selectedQuote = random(1,results.length) -1;
		
		if("".equals(results[selectedQuote]))
			return;
		
		Say(results[selectedQuote]);
		
		return;
	}
	
	public void Say(String content)
	{
		setTypeLine(content.replaceAll("(?i)report","reppppport").replaceAll("(?i)botting","botttting").replaceAll("(?i)bot ","bottt "));
		while(!next());
	}
	
    @Override
    public void onChatMessage(String msg, String name, boolean pmod, boolean jmod) {
		
		String receivedLC = msg.toLowerCase();
		
		final String lname = client.getPlayerName(client.getPlayer());		
        if(name.equalsIgnoreCase(lname))
		{
			if (receivedLC.equals("--params") || receivedLC.equals("--param"))
				printParams();
			if (receivedLC.equals("--help"))
				printHelp();
			if (receivedLC.equals("--status"))
				reportXpChange();
			if (receivedLC.equals("--version"))
				print("Version " + getSctiptVersion());
			if (receivedLC.equals("--base version"))
				print("Base Version " + BASE_SCRIPT_VERSION);
		}
		
		
		if (receivedLC.equals("base version"))
			ReplyMessage("using Abyte0_Script " + BASE_SCRIPT_VERSION);
		if (receivedLC.equals("version") && !getSctiptVersion().equals(""))
			ReplyMessage("Script Version " + getSctiptVersion());
		
		//Do not reply to yourself for the code bellow this
        if(name.equalsIgnoreCase(lname)) return;
		
		int oddsToReply = random(0,10);
		boolean wantToReply = oddsToReply == 1;
		
        if (wantToReply && ((receivedLC.contains("level") || receivedLC.contains("lvl")) && receivedLC.contains("?"))) {
			
			if(random(0,50)== 0)
				ReplyMessage("i cant tell you its a secret");
			else
			{
				if (receivedLC.contains("cook"))
					ReplyMessage("I am " + getLevel(7));
				if (receivedLC.contains("wood") || receivedLC.contains("wc"))
					ReplyMessage("I am " + getLevel(8));
				if (receivedLC.contains("fletch"))
					ReplyMessage("I am " + getLevel(9));
				if (receivedLC.contains("fish"))
					ReplyMessage("I am " + getLevel(10));
				if (receivedLC.contains("fire"))
					ReplyMessage("I am " + getLevel(11));
				if (receivedLC.contains("craft"))
					ReplyMessage("I am " + getLevel(12));
				if (receivedLC.contains("smith"))
					ReplyMessage("I am " + getLevel(13));
				if (receivedLC.contains("mining")|| receivedLC.contains("mine"))
					ReplyMessage("I am " + getLevel(14));
				if (receivedLC.contains("herb"))
					ReplyMessage("I am " + getLevel(15));
				if (receivedLC.contains("agility"))
					ReplyMessage("I am " + getLevel(16));
				if (receivedLC.contains("thieving") || receivedLC.contains("thieve") || receivedLC.contains("thief"))
					ReplyMessage("I am " + getLevel(17));
			}
			//https://stackoverflow.com/questions/2286648/named-placeholders-in-string-formatting
        }
		
        if (wantToReply && receivedLC.contains("press") && (receivedLC.contains("macro") || receivedLC.contains("bot"))) {
			
				Pattern pattern = Pattern.compile("\\d{3,6}");
				Matcher matcher = pattern.matcher(receivedLC);
				if(matcher.find())
				{
					if(random(0,1)== 0)
						ReplyMessage(matcher.group(0));
					else
						ReplyMessage("i dont bot so " + matcher.group(0));
				}
        }
		
		
		super.onChatMessage(msg, name, pmod, jmod);
    }

	private void ReplyMessage(String content)
	{
		if((System.currentTimeMillis() - lastReplyTime) < (minimumSecondsBetweenReplys*1000L))
		{
			print("wont reply because " + (System.currentTimeMillis() - lastReplyTime) + " is smaller than " + (minimumSecondsBetweenReplys*1000L));
			return;
		}
		
		Say(content);
		lastReplyTime = System.currentTimeMillis();
	}
	
	public void printInventory()
	{
		//String nom = AutoLogin.user;
		int money = 10;
		int feathers = 381;
		int chaosRunes = 41;
		int natureRunes = 40;
		int ironOres = 151;
	    int coalOres = 155;
	    int mithOres = 153;
	    int addyOres = 154;
	    int runiteOres = 409;
	    int ironBars = 170;
		int steelBars = 171;
		int runiteBars = 408;
		int bowStrings = 676;
		int yewLongU = 664;
		int yewLong = 654;
		int magicLongU = 666;
		int magicLong = 656;
		int rawLobs = 372;
		int cookLobs = 373;
		int rawSharks = 545;
		int cookSharks = 546;
		
		int[] ids = new int[]{money,feathers,chaosRunes,natureRunes,
		ironOres,coalOres,mithOres,addyOres,runiteOres,ironBars,steelBars,runiteBars,
		bowStrings,yewLongU,yewLong,magicLongU,magicLong,rawLobs,cookLobs,rawSharks,cookSharks};
	
		String[] valeurs = new String[22];
		//valeurs[0] = nom;
		
		for(int i = 0; i < 21; i++)
		{
			int[] bk = new int[]{ids[i]};
			
			valeurs[i+1] = getInventoryCount(bk)+"";
		}
	
		sendInventory(valeurs);
	}
	
	protected int getSelfPid()
	{
		ta player = (ta)client.getPlayer(0);
		return client.getMobServerIndex(player);
	}
	
	public void printStats()
	{			
		//String nom = AutoLogin.user;
		
		int attack = getLevel(0);
		int defence = getLevel(1);
		int strength = getLevel(2);
		int hits = getLevel(3);
		int prayer = getLevel(5);
		int magic = getLevel(6);
		int ranged = getLevel(4);
		
		int fishing = getLevel(10);
		int cooking = getLevel(7);
		int mining = getLevel(14);
		int smithing = getLevel(13);
		int woodcut = getLevel(8);
		int fletching = getLevel(9);
		int agility = getLevel(16);
		int firemaking = getLevel(11);
		int crafting = getLevel(12);
		int herblaw = getLevel(15);
		int thieving = getLevel(17);
		
		int[] ids = new int[]{attack, defence, strength, hits, prayer, magic, ranged, fishing, cooking, mining, smithing, woodcut, fletching, agility, firemaking, crafting, herblaw, thieving};
	
		String[] valeurs = new String[19];
		//valeurs[0] = nom;
		
		for(int i = 0; i < 18; i++)
		{
			valeurs[i+1] = ids[i]+"";
		}
	
		sendStats(valeurs);
	}
	
	public void printStatsXp()
	{			
		//String nom = AutoLogin.user;
		
		int attack = getExperience(0);
		int defence = getExperience(1);
		int strength = getExperience(2);
		int hits = getExperience(3);
		int prayer = getExperience(5);
		int magic = getExperience(6);
		int ranged = getExperience(4);
		
		int fishing = getExperience(10);
		int cooking = getExperience(7);
		int mining = getExperience(14);
		int smithing = getExperience(13);
		int woodcut = getExperience(8);
		int fletching = getExperience(9);
		int agility = getExperience(16);
		int firemaking = getExperience(11);
		int crafting = getExperience(12);
		int herblaw = getExperience(15);
		int thieving = getExperience(17);
		
		int[] ids = new int[]{attack, defence, strength, hits, prayer, magic, ranged, fishing, cooking, mining, smithing, woodcut, fletching, agility, firemaking, crafting, herblaw, thieving};
	
		String[] valeurs = new String[19];
		//valeurs[0] = nom;
		
		for(int i = 0; i < 18; i++)
		{
			valeurs[i+1] = ids[i]+"";
		}
	
		sendStatsXp(valeurs);
	}

	//* SEND METHODS *//
	public void sendPosition(String name,int x, int y) 
	{
	} 
	
	public void sendInventory(String[] propertyUsed, String[] values) 
	{
	} 
	
	public void sendStats(String[] propertyUsed, String[] values) 
	{
	} 
	
	public void sendStatsXp(String[] propertyUsed, String[] values) 
	{
	} 
	
	public void sendStats(String[] values)
	{
		sendStats(PROPERTY_NAMES_STATS, values);
	}
	
	public void sendStatsXp(String[] values)
	{
		sendStatsXp(PROPERTY_NAMES_STATS, values);
	}
	
	public void sendInventory(String[] values)
	{
		sendInventory(PROPERTY_NAMES, values);
	}
	
	public void createAccount(String name) 
	{
	}
	
	public int getExperience(int skill) {
        return (int) client.getExperience(skill);
    }	
	
	    /**
     * Returns the position of the item with the given ID in the client's
     * inventory.
     *
     * @param ids the identifiers of the items to search for.
     * @return the position of the first item with the given id(s). May range
     * from 0 to MAX_INV_SIZE.
     */
    public int getLastInventoryIndex(int... ids) {
        for (int i = getInventoryCount()-1; i >=0 ; i--) {
            if (inArray(ids, client.getInventoryId(i))) {
                return i;
            }
        }
        return -1;
    }

	    /**
     * Drop the 
     */
    public int dropItemIdOrWait(int id) {
		
        int firstInstanceIndex = getInventoryIndex(id);
		if(firstInstanceIndex == -1)
			return -1;
		
        int lastInstanceIndex = getLastInventoryIndex(id);
		
		if(!waitingBeforeLastDrop && firstInstanceIndex == lastInstanceIndex) //Lets wait a bit before dropping the last one
		{
			waitingBeforeLastDrop = true;
			return 2000;
		}
		
		dropItem(firstInstanceIndex);
		waitingBeforeLastDrop = false;
		
		return 1500;
    }

	public int[][] getAllNpcsById(int... ids)
	{
		int cpt = 0;
		for (int i = 0; i < client.getNpcCount(); i++) {
			if (inArray(ids, client.getNpcId(client.getNpc(i))))
				cpt++;
		}
		
		int[][] npcS = new int[cpt][];
		
		int cptAdded = 0;

		for (int i = 0; i < client.getNpcCount(); i++) {
			if (inArray(ids, client.getNpcId(client.getNpc(i)))) {
				final int x = client.getMobLocalX(client.getNpc(i)) + client.getAreaX();
				final int y = client.getMobLocalY(client.getNpc(i)) + client.getAreaY();
				final int dist = distanceTo(x, y, getX(), getY());
				if (dist < 10)
				{
					final int[] npc = new int[]{-1, -1, -1};
					
					npc[0] = i;
					npc[1] = x;
					npc[2] = y;
					
					npcS[cptAdded]  = npc;
				}
			}
		}
		return npcS;
	}
	
	public void RunFromCombat()
	{
		walkTo(getX(),getY());
	}
	
	public boolean IsStillHavingFood(int[] foodIDs)
	{
		for(int i = 0; i < foodIDs.length; i++)
		{
			if(IsStillHavingFood(foodIDs[i]))
				return true;			
		}
		return false;
	}
	
	public boolean IsStillHavingFood(int foodId)
	{
		if(foodId == -1) return true;
		if(foodId == 330 || foodId == 333 || foodId == 335)
			return getInventoryCount(330,333,335) > 0;
		else if(foodId == 332 || foodId == 334 || foodId == 336)
			return getInventoryCount(332,334,336) > 0;
		else
			return getInventoryCount(foodId) > 0;
	}
	
	public final boolean EatFood(int[] foodIDs)
	{
		for(int i = 0; i < foodIDs.length; i++)
		{
			if(IsStillHavingFood(foodIDs[i]))
			{
				if(EatFood(foodIDs[i]))
					return true;
			}
		}
		
		return false;
	}
	
	public final boolean EatFood(int foodId)
	{
		if(foodId == -1) return false;
		
		if(foodId == 330 || foodId == 333 || foodId == 335 || foodId == 332 || foodId == 334 || foodId == 336)
		{
			if(EatCakes())
				return true;
		}
		else
		{
			int foodIndex = getInventoryIndex(foodId);
			if(foodIndex == -1)
				return false;
			
			useItem(foodIndex);
			return true;
		}
		
		return false;
	}
	
	private boolean EatCakes()
	{
		if(EatMultiParts(335,333,330)) return true;
		if(EatMultiParts(336,334,332)) return true;
		
		return false;
	}
	
	private boolean EatMultiParts(int part1Id, int part2Id, int part3Id)
	{
		int part1 = getInventoryIndex(part1Id);
		int part2 = getInventoryIndex(part2Id);
		int part3 = getInventoryIndex(part3Id);
		if(part1 != -1)
		{
			useItem(part1);
			return true;
		}
		else if(part2 != -1)
		{
			useItem(part2);
			return true;
		}
		else if(part3 != -1)
		{
			useItem(part3);
			return true;
		}
		
		return false;
	}
	
	public String getDateTime()
	{
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		return dtf.format(now);
	}
	
	public String getSctiptVersion()
	{
		return "";
	}
	
	private int[] raresItems = new int[]{112,522,526,527,1276,1277,1278,795,575,576,577,578,579,580,581,597,677,828,831,832,1092,971};
	
	public int pickupRareItems(int maxDistance)
	{
		for(int h = 0; h < raresItems.length; h++)
		{
			int[] groundItems = getItemById(raresItems[h]);
			if(groundItems[0] != -1)
			{
				if(inCombat())
				{
					RunFromCombat();
					return 500;
				}

				if(isAtApproxCoords(groundItems[1], groundItems[2], maxDistance))
				{
					pickupItem(groundItems[0], groundItems[1], groundItems[2]);
					print("Try to pickup a rare item : " + groundItems[0]);
					Say("WHAT! " + groundItems[0] + " on ground");
					return 300;
				}
			}
		}
		
		return -1;
	}
	
	protected int getFmodeXp(int fmode)
	{
		if(fmode == 1)
			return getXpForLevel(2);
		if(fmode == 2)
			return getXpForLevel(0);
		if(fmode == 3)
			return getXpForLevel(1);
		
		return getXpForLevel(0) + getXpForLevel(1) + getXpForLevel(2);
	}

	protected int getThievingXp()
	{
		return getXpForLevel(17);
	}

	protected int getFishingXp()
	{
		return getXpForLevel(10);
	}

	protected int getWoodcuttingXp()
	{
		return getXpForLevel(8);
	}

	protected int getFletchingXp()
	{
		return getXpForLevel(9);
	}

	protected int getFmodeLevel(int fmode)
	{
		if(fmode == 1)
			return getLevel(2);
		if(fmode == 2)
			return getLevel(0);
		if(fmode == 3)
			return getLevel(1);
		
		return 0;
	}

	protected void printHelp()
	{
		if(hasStatistics)
		{
			print("Press # or ' or type --status to display statistics");
			print("Press F2 to reset statistics");
		}
		
		print("type @mag@--help@whi@ in public chat to view help");
		print("type @mag@--param@whi@ to view currently running parameters");
		print("type @mag@--version@whi@ to view currently running script version");
		print("type @mag@version@whi@ to view other players running script version");
		print("type @mag@base version@whi@ to view other players Abyte0_Script version");
	}
	
	protected void printParams()
	{

	}
	
	protected void reportXpChange()
	{
	}
	
	protected boolean hasStatistics = false;
	
	
	
	protected void switchToScript(String scriptName, String scriptParams)
	{
		//print("{scriptName}="+scriptName);
		//print("{scriptParams}="+scriptParams);
		//
		//ScriptListener listener = (ScriptListener)client.getScriptListener();
		//IScript script = initJavaScript(scriptName + ".class");
		//listener.setIScript(script);
		//script.init(scriptParams);
		//
		//print("Running script Changed : "+listener.getScriptName());
	}
	
	//HERBLAW SECTION
	boolean doHerbMixing = true;
	int waterVial = 464;
	
	int dirtyGuam = 165;
	int dirtyMarrentil = 435;
	int dirtyTarromin = 436;
	int dirtyHarralander = 437;
	int dirtyRanarr = 438;
	
	int cleanGuam = 444;
	int cleanMarrentil = 445;
	int cleanTarromin = 446;
	int cleanHarralander = 447;
	int cleanRanarr = 448;
	
	int unfinishGuam = 454;
	int unfinishMarrentil = 455;
	int unfinishTarromin = 456;
	int unfinishHarralander = 457;
	int unfinishRanarr = 458;
	
	protected int MakeUnfinishPotion()
	{
		if(getLevel(15) < 3) return -1;
		
		if(getInventoryCount(dirtyGuam) > 0)
		{
			useItem(getInventoryIndex(dirtyGuam));
			return 1000;
		}
		if(getInventoryCount(dirtyMarrentil) > 0 && getLevel(15) >= 5)
		{
			useItem(getInventoryIndex(dirtyMarrentil));
			return 1000;
		}
		if(getInventoryCount(dirtyTarromin) > 0 && getLevel(15) >= 11)
		{
			useItem(getInventoryIndex(dirtyTarromin));
			return 1000;
		}
		if(getInventoryCount(dirtyHarralander) > 0 && getLevel(15) >= 20)
		{
			useItem(getInventoryIndex(dirtyHarralander));
			return 1000;
		}
		if(getInventoryCount(dirtyRanarr) > 0 && getLevel(15) >= 25)
		{
			useItem(getInventoryIndex(dirtyRanarr));
			return 1000;
		}
		
		if(getInventoryCount(waterVial) == 0) return -1;
		
		int firstWaterVialPosition = getInventoryIndex(waterVial);
		if(getInventoryCount(cleanGuam) > 0)
		{
			useItemWithItem(firstWaterVialPosition,getInventoryIndex(cleanGuam));
			return 1000;
		}
		if(getInventoryCount(cleanTarromin) > 0 && getLevel(15) >= 12)
		{
			useItemWithItem(firstWaterVialPosition,getInventoryIndex(cleanTarromin));
			return 1000;
		}
		if(getInventoryCount(cleanHarralander) > 0 && getLevel(15) >= 22)
		{
			useItemWithItem(firstWaterVialPosition,getInventoryIndex(cleanHarralander));
			return 1000;
		}
		if(getInventoryCount(cleanRanarr) > 0 && getLevel(15) >= 30)
		{
			useItemWithItem(firstWaterVialPosition,getInventoryIndex(cleanRanarr));
			return 1000;
		}
		
		return -1;
	}
	
	
}