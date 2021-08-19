//Made by Abyte0
public class Abyte0_Paladin extends Abyte0_Script 
{		
	int fightMode = 3;
	boolean chestReady = true;
	int[] npcID = new int[]
	{ 
		323 //Paladin
	};		
	int steelBar = 171;
	int mithBar = 173;
	
	//change this to true if you want to eat food to be able to pick mith bar....
	//I HIGLY don't suggest this but its up to you guys
	boolean eatFoodToPickMithBar = false;
	
	int[] foodIDs = new int[] 
	{		
		330, //cake 3/3		
		333, //cake 2/3		
		335, //cake 1/3		
		895, //Swamp Toad		
		897, //King worm		
		138, //bread		
		142, //wine
		373  //Lobs
	};		
		
	public Abyte0_Paladin(Extension e) {super(e);}		
	public void init(String params) 
	{		
		System.out.println("Abyte0_Paladin");
		System.out.println("Thiever for Paladin Tower in Ardougne");
		System.out.println("Version 0.9.3");
		
		System.out.println("Abyte0_paladin fmode,foodId,foodId,...");
		
		//0.4 Fix Stair Hideout
		//0.5 Pick Up Scimitar
		//0.6 Bank Fix
		//0.7 Added Food Param Comment + Stair Eating
		//0.8 Fixed Legit Trick With Door
		//0.9 2013-01-25 - PickUp Mith Bar
		//0.9.1 2013-01-25 - Can Eat To Pick Bar...
		//0.9.2 2021-06-30 - Updated to OpenRSC
		
		String[] in = params.split(",");		
		fightMode = Integer.parseInt(in[0]);
		if(in.length > 1)
		{
			foodIDs = new int[in.length - 1];		
			for(int i = 0; i < foodIDs.length; i++)			
				foodIDs[i] = Integer.parseInt(in[i + 1]);
		}
	}	
		
	public int main() 
	{
		if(getFightMode() != fightMode)			
			setFightMode(fightMode);				
		if(inCombat())
		{			
			walkTo(getX(), getY());			
			return random(800,1111);		
		}				
		if(getFatigue() > 90) 
		{			
			useSleepingBag();			
			return 1000;		
		}	
		if(isBanking()) 
		{			
			//deposit money and keep 1 gp
			if(getInventoryCount(10) > 1) 
			{				
				deposit(10,getInventoryCount(10)-1);
				return random(1800, 2100);
			}
			else if(getInventoryCount(10) < 1) 
			{				
				withdraw(10,1);
				return random(1800, 2100);
			}
			//deposit chaos and keep 1 chaos
			if(getInventoryCount(41) > 1) 
			{				
				deposit(41,getInventoryCount(41)-1);
				return random(1800, 2100);
			}
			else if(getInventoryCount(41) < 1) 
			{				
				withdraw(41,1);
				return random(1800, 2100);
			}
			
			//On Depose Scimitar
			if(getInventoryCount(427) > 0) 
			{				
				deposit(427,getInventoryCount(427));
				return random(500, 600);			
			}
			//On Depose Raw Shark
			if(getInventoryCount(545) > 0) 
			{				
				deposit(545,getInventoryCount(545));
				return random(500, 600);			
			}
			//On Depose Uncut Saphire
			if(getInventoryCount(160) > 0) 
			{				
				deposit(160,getInventoryCount(160));
				return random(500, 600);			
			}
			//On Depose Addy Ore
			if(getInventoryCount(154) > 0) 
			{				
				deposit(154,getInventoryCount(154));
				return random(500, 600);			
			}
			//On Depose Steel Bar
			if(getInventoryCount(steelBar) > 0) 
			{				
				deposit(steelBar,getInventoryCount(steelBar));
				return random(500, 600);			
			}
			//On Depose Mith Bar
			if(getInventoryCount(mithBar) > 0) 
			{				
				deposit(mithBar,getInventoryCount(mithBar));
				return random(500, 600);			
			}
			//On Withdraw la Food
			if(getInventoryCount() == 30) 
			{				
				closeBank();				
				return random(500, 600);			
			}
			withdraw(foodIDs[0], 30 - getInventoryCount());			
			return random(2500, 2600);		
		}				
		if(isQuestMenu()) 
		{			
			answer(0);			
			return random(2500, 2600);		
		}
		//On verifie si on a de la nourriture		
		if(getInventoryCount(foodIDs) > 0)
		{
			if(getHpPercent() < 70) 
			{	
				return eatFood();
			}
			//on regarde si on est dans la sale des paladins
			if(getX() >= 602 && getX() <= 615 && getY() >= 1548)
			{
				//On regarde si on peut ramasser des mith bars
				if(getInventoryCount() < 30)
				{
					//y a t il des bars sur le plancher
					int[] groundItemSteel = getItemById(steelBar);
					if(groundItemSteel[0] != -1)
					{
						pickupItem(groundItemSteel[0], groundItemSteel[1], groundItemSteel[2]);
						return random(1000, 1500);
					}
					//y a t il des bars sur le plancher
					int[] groundItemMith = getItemById(mithBar);
					if(groundItemMith[0] != -1)
					{
						pickupItem(groundItemMith[0], groundItemMith[1], groundItemMith[2]);
						return random(1000, 1500);
					}
				}
				else
				{
					if(eatFoodToPickMithBar)
						return eatFood();
				}
				
				//On thieve les paladins
				int[] npc = getNpcById(npcID);		
				if(npc[0] != -1)
				{		
					thieveNpc(npc[0]);
					chestReady = true;
					return random(500, 1000);
				}
			}
			else
			{
				int[] doorObj;
				int[] stairs;
				
				//Si pret a SORTIR de la BANQUE
				if(getX() == 551 && getY() == 612)
				{
					stairs = getObjectById(64);				
					if(stairs[0] != -1)
					{
						atObject(stairs[1], stairs[2]);
						return random(1000,1200);
					}
					walkTo(550,612);
					return random(600,800);
				}
				
				//si on est cacher derriere les marches on veut monte
				if(getX() == 613 && getY() == 601)
				{
					stairs = getObjectById(342);				
					if(stairs[0] != -1)
					{
						atObject(stairs[1], stairs[2]);
						return random(1500,3200);
					}
				}
				
				//Si DANS la banque
				if(getX() >= 551 && getX() <= 554 && getY() >= 609 && getY() <= 616)
				{
					walkTo(551,612);
					return random(600,800);
				}
				
				//Si on est en bas de smarche on veut aller a coter se cacher
				if(getX() >= 608 && getY() >= 597 && getY() <= 609)
				{
					walkTo(613,601);
					return random(400,1300);
				}
				//Si on est en haut des marche et on veut entrer
				if(getX() >= 602 && getX() <= 615 && getY() > 1500 && getY() < 1548)
				{
					int[] door = getWallObjectById(97);			
					if(door[0] != -1 && isAtApproxCoords(door[1], door[2],5))
					{
						atWallObject2(door[1], door[2]);
						return random(1000, 1500);
					}
				}

				//On va vers le zoo
				if(getX() < 567)
				{
					walkTo(567,606);
					return random(500,1000);
				}
				//On va vers  le magasin general
				if(getX() < 580)
				{
					walkTo(580,606);
					return random(500,1000);
				}
				//On va a la porte
				if(getX() < 598)
				{
					walkTo(598,604);
					return random(500,1000);
				}
				if(getX() < 599)
				{
					//Passing the Metal Gate
					stairs = getObjectById(57);				
					if(stairs[0] != -1 && isAtApproxCoords(stairs[1], stairs[2],10))
					{
						atObject(stairs[1], stairs[2]);
						return random(800,1000);
					}
					walkTo(599,604);
					return random(500,1000);
				}
				if(getX() < 608)
				{
					//Passing the Wooden Door
					stairs = getObjectById(64);				
					if(stairs[0] != -1)
					{
						if(stairs[1] >= 605 && stairs[1] <= 610 && stairs[2] >= 600 && stairs[2] <= 608)
						{
							atObject(stairs[1], stairs[2]);
							return random(800,1000);
						}
					}
					walkTo(608,604);
					return random(500,1000);
				}
			}
			return 500;
		}
		else
		{
			int[] doorObj;
			int[] stairs;
			//On regarde si on est dans la sale des paladins
			if(getX() >= 602 && getX() <= 615 && getY() >= 1548 && getY() <= 1648)
			{
				//Si le chest est pret on monte pour se teleporter
				if(chestReady)
				{
					//On Monde lechele
					stairs = getObjectById(5);				
					if(stairs[0] != -1 && isAtApproxCoords(stairs[1], stairs[2],10))
					{
						atObject(stairs[1], stairs[2]);
						return random(1500,3200);
					}
				}
				else
				{
					doorObj = getWallObjectById(97);
					if(doorObj[0] != -1 && isAtApproxCoords(doorObj[1], doorObj[2],10))
					{			
						atWallObject(doorObj[1], doorObj[2]);
						return random(1000,1200);
					}	
				}
				return random(800,900);
			}
			
			//Si devant Banque
			if(getX() == 550)
			{
				stairs = getObjectById(64);				
				if(stairs[0] != -1 && isAtApproxCoords(stairs[1], stairs[2],5))
				{
					atObject(stairs[1], stairs[2]);
					return random(1000,1200);
				}
				walkTo(551,612);
				return random(600,800);
			}
			//Si DANS la banque
			if(getX() >= 551 && getX() <= 554 && getY() >= 609 && getY() <= 616)
			{
				int banker[] = getNpcByIdNotTalk(BANKERS);	        
				if(banker[0] != -1)
				{			
					talkToNpc(banker[0]);	        
					return 3000;		
				}
			}
			
			//-----
			//Walking To Bank Manualy
			//-----
			
			//Si on est en haut ou il y a le chest
			if(isAtApproxCoords(611,2491,10))
			{
				int[] groundItem = getItemById(427);
				if(groundItem[0] != -1)
				{
					pickupItem(groundItem[0], groundItem[1], groundItem[2]);
					return random(1000, 1500);
				}
				//Si le chest n'est aps pret on dessend
				if(getObjectIdFromCoords(610,2487) != 338)
				{
					chestReady = false;
					stairs = getObjectById(6);				
					if(stairs[0] != -1)
					{
						atObject(stairs[1], stairs[2]);
						return random(1000,1200);
					}
				}
				else
				{
					System.out.println("loot the chest PLZ");
					atObject2(610,2487);
					return random(2000,3000);
				}
			}
			//Si on est a coter des escali et on veut descendre
			if(getX() >= 602 && getX() <= 615 && getY() > 1500 && getY() < 1548)
			{
				stairs = getObjectById(44);				
				if(stairs[0] != -1)
				{
					atObject(stairs[1], stairs[2]);
					return random(1000,1200);
				}
			}
			//Si on est proche des escalier pour sortir du castle
			if(getX() >= 608 && getY() >= 597 && getY() <= 609)
			{
				//Passing the Wooden Door
				stairs = getObjectById(64);				
				if(stairs[0] != -1 && isAtApproxCoords(stairs[1], stairs[2],5))
				{
					atObject(stairs[1], stairs[2]);
					return random(800,1000);
				}
				walkTo(607,604);
				return random(500,1000);
			}
			//Si on est proche des Warriors
			if(getX() >= 599)
			{
				//Passing the Metal Gate
				stairs = getObjectById(57);				
				if(stairs[0] != -1 && isAtApproxCoords(stairs[1], stairs[2],10))
				{
					atObject(stairs[1], stairs[2]);
					return random(800,1000);
				}
				walkTo(598,604);
				return random(500,1000);
			}
			//Si on est proche du Pont
			if(getX() >= 585)
			{
				walkTo(584,606);
				return random(500,1000);
			}
			//Si on est proche du Zoo
			if(getX() >= 570)
			{
				walkTo(569,606);
				return random(500,1000);
			}
			//Si on est proche du Zoo2
			if(getX() >= 551)
			{
				walkTo(550,608);
				return random(500,1000);
			}
			
			//-----
			//----- Walking to Bank By Teleport
			//-----
			
			//Si on est proche du Teleport
			if(isAtApproxCoords(523,606,5))
			{
				walkTo(528,615);
				return random(500,1000);
			}
			//Si on est proche des Chickens
			if(getX() <= 542)
			{
				walkTo(543,615);
				return random(500,1000);
			}
			//Si on est proche du bateau on marche pres de la banque
			if(getX() <= 549)
			{
				walkTo(550,612);
				return random(500,1000);
			}
			return 500;
		}
	}
	
	private int eatFood()
	{
		int idx = getInventoryIndex(foodIDs);	    	
		if(idx == -1) 
		{
			if(getHpPercent() < 30) 
			{
				System.out.println("hp is dangerously low with no food.");	    		
				stopScript();	    		
				return 0;
			}
			else
			{
				return random(10000,12000);
			}
		}	    	
		useItem(idx);
		return random(800,1000);
	}
}