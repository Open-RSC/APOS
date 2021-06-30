/*
	Abyte0
	2012-02-16
	Version 1.3 - 2012-05-04
	Version 1.4 - 2021-06-22 Update to OpenApos
*/
import java.net.*; 
import java.io.*;  

public class Abyte0_Script extends Storm_Script 
{    

    public Extension client;
	
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

	public void print(String gameText)
	{
		System.out.println(gameText);
		printBot(gameText);
	}
	
	//* BUILDS METHODS *//
	
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
}