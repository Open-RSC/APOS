/*
	By: 			Abyte0
	Private Release: 	2011-??-??
	Public Release: 	2013-01-15
	Version 1.0			2022-01-05		Stats when #, F2 to reset stats, Talking, Version
*/
public class Abyte0_ArdMiner extends Abyte0_Script
{
	
	private final String VERSION = "1.0";
	
	@Override
	public String[] getRandomQuotes()
	{
		String[] result = {"Mining level?","Smithing level?","Mining lvl?","Smithing lvl?","Are you able to mine runite?","This rock is hard","Hi","yo!","sup","how are you doing today?","Which is better: Iron bars or Steel bars?","Banking or not, this is the question"};
		return result;
	}

	int initialXp = 0;
	long initialTime = 0;
	
	int oreID = 151;
	// Iron    
	int banker = 95; // Banker    // uncut gem id's    
	int gem1 = 160; // sapph   
	int gem2 = 159; // emerald    
	int gem3 = 158; // ruby    
	int gem4 = 157; // diamond
	int mined;
	int cptTry = 0;
	int fmode = 3;
	int cptInventaireToBank = 30;
	public Abyte0_ArdMiner(Extension e)
	{
		super(e);
	}    
	public void init( String params )
	{
		mined = 0;
		print("Abyte0 : Ardougne Miner!");
		print("Version " + VERSION);
		
		print("Press # for stats");
		print("Press F2 to reset stats");
		
		print("No param = Bank , Anything as param = Power Mining...");
		print("fmode="+fmode);
		if(params.equals(""))
			cptInventaireToBank = 30;
		else
			cptInventaireToBank = 60;
		
		initialXp = getMiningXp();
		initialTime = System.currentTimeMillis();
	}    
	public int main()
	{
		SayRandomQuote();
		
		return Mine();
	}    
	public int Mine()
	{
		if(getFightMode() != fmode)
		{
			setFightMode(fmode);
		}        
		if(getFatigue() > 90)
		{
			useSleepingBag();
			return 1000;
		}
		if(isBanking())
		{            
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
				print("Mined "+mined+" iron ore so far");
				return 1000;
			}              
			closeBank();
			return 1000;
		}
		if(isQuestMenu())
		{
			answer(0);
			return 1000 + random(300, 1200);
		}        
		// Open door       
		//if(distanceTo(getX(),getY(),287,571) < 5)            
		//	if(getObjectById(64).length > 0)
		//		atObject(getObjectById(64)[1],getObjectById(64)[2]);
		// Talk to banker        
		//if(distanceTo(286,571) < 6 && getInventoryCount() > 2)
		//{            
		//	talkToNpc(getNpcById(banker)[0]);
		//	return 2002 + random(100, 100);
		//} 
		if(getInventoryCount() == cptInventaireToBank)
		{
			if(getY() > 654)
			{                
				walkTo(619,654);
				return 1000 + random(300, 1200);
			}            
			if(getY() > 643)
			{
				// walk ver Foret
				walkTo(609,643);
				return 1000 + random(300, 1200);
			}
			if(getY() > 625)
			{
				//walk ver Environ A coter de la fin du zoo
				walkTo(601,625);
				return 1000 + random(300, 1200);
			}
			if(getY() > 609)
			{
				//walk ver Environ A coter du debut zoo
				walkTo(591,609); 
				return 1000 + random(300, 1200);
			}
			if(getY() > 591)
			{
				//On marche jusqua la porte du smelting room
				walkTo(588,591);
				return 1000 + random(300, 1200);
			}
			if(getY() > 581)
			{
				//On marche vers la petite porte des nats
				walkTo(586,581);
				return 1000 + random(300, 1200);
			}
			if(getY() > 576)
			{
				//Si avant banque on veut entrer....
				print("Walking Inside Stripped Bank");
				walkTo(581,573);
				return 1000 + random(300, 1200);
			}
			//if(getX() == 287 && getY() == 571)
			//{	
			//	//Si a coter de la porte a exterieur
			//	print("Step Inside Bank");	
			//	atObject(287,571);
			//	walkTo(286,571);
			//	return random(100, 1500);
			//}
			if(getX() >= 577 && getX() <= 585 && getY() >= 572 && getY() <= 576)
			{
				//Si dans la banque Ligné
				print("Talking to Banker");											
				if(!isBanking())
				{					
					int banker[] = getNpcByIdNotTalk(new int[]{95});										
					if (banker[0] != -1 && !isBanking())
					{						
						talkToNpc(banker[0]);						
						return random(2000,3000);
					}
				}	
				return random(231, 1500);
			}
			print("Walking to BANK LIGNE RANDOM");
			walkTo(581,573);//Bank
			return 1000 + random(300, 1200);
		}
		else
		{
			//if(getX() == 286 && getY() == 571)
			//{	
			//	//Si a coter de la porte a linterieur
			//	print("Step Outside Bank");	
			//	atObject(287,571);
			//	walkTo(287,571);
			//	return random(121, 3500);
			//}
			if(getX() >= 577 && getX() <= 585 && getY() >= 572 && getY() <= 576)
			{
				//Si dans la banque Ligné	
				walkTo(586,581);
				return random(240, 2500);
			}
			if(getY() < 581)
			{
				//On marche vers la petite porte des nats
				walkTo(586,581);
				return 1000 + random(300, 1200);
			}
			if(getY() < 591)
			{
				//On marche jusqua la porte du smelting room
				walkTo(588,591);
				return 1000 + random(300, 1200);
			}            
			if(getY() < 609)
			{
				//walk ver Environ A coter du debut zoo
				walkTo(591,609);                
				return 1000 + random(300, 1200);
			}
			if(getY() < 625)
			{
				//walk ver Environ A coter de la fin du zoo
				walkTo(601,625);
				return 1000 + random(300, 1200);
			}
			if(getY() < 643)
			{
				// walk ver Foret
				walkTo(609,643);                
				return 1000 + random(300, 1200);
			}
			if(getY() < 655)
			{
				//On walk vers le mining spot
				walkTo(619,655);                
				return 1000 + random(300, 1200);
			}
			if(getY() == 655)
			{
				//On Au mining
				print("Walking to rocks!");
				walkTo(619,656);                
				return 1000 + random(300, 1200);
			}
			if(distanceTo(618,656) < 5)
			{
				int[] rock = getObjectById(102);
				if(rock.length > 0)
				{
					if(getObjectIdFromCoords(616,656) == 102)
					{
						print("Mining Rock 1!");
						atObject(616,656);
					}
					else if(getObjectIdFromCoords(617,655) == 102)
					{
						print("Mining Rock 2!");
						atObject(617,655);
					}
					else if(getObjectIdFromCoords(618,655) == 102)
					{
						print("Mining Rock 3!");
						atObject(618,655);
					}
					else if(getObjectIdFromCoords(618,658) == 102)
					{
						print("Mining Rock 4!");
						atObject(618,658);
					}
					else if(getObjectIdFromCoords(619,657) == 102)
					{
						print("Mining Rock 5!");
						atObject(619,657);
					}
					return random(600,2014);
				}
				return 600 + random(300, 1200);          
			}
			print("ELSE");
			return random(400,1103);
		}
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
	public void reportXpChange()
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
    public void onChatMessage(String msg, String name, boolean pmod, boolean jmod) {
		
		//Do not reply to yourself
		//final String lname = client.getPlayerName(client.getPlayer());		
        //if(name.equalsIgnoreCase(lname)) return;
		
		String receivedLC = msg.toLowerCase();
		
		if (receivedLC.equals("version"))
			Say("Script Version " + VERSION);
		
		super.onChatMessage(msg, name, pmod, jmod);
    }

	
}