import java.io.*;
import java.util.Scanner;
import java.awt.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.*;
import javax.swing.*;

public class Abyte0_YanilleChaos extends Abyte0_Script
{
	int fMode = 2;	
	int foodId = 373;
	int inventoryCount = 25;
	
	int mindRunes = 35;
	int chaosRunes = 41;
	
	int sinisterKey = 932;
	int halfKey1 = 526;
	int halfKey2 = 527;
	
	int WhiteBerries = 471;
	
	int HalfDragonSquareShield1 = 1276;
	int HalfDragonSquareShield2 = 1277;
	
	int FireRune = 31;
	int WaterRune = 32;
	int AirRune = 33;
	int EarthRune = 34;
	int deathRunes = 38;
	int nature = 40;
	int law = 42;
	int CosmicRune = 46;
	
	int guam = 165;
	int tarromin = 436;
	int harralander = 437;
	int irit = 439;
	int kwuarm = 441;
	int dwarfWeed = 443;
	int limpwrut = 220;
	
	int groundUnicornHorn = 473;
	
	int foodRequired = 1;
	
	int superDef1 = 497;
	int walkBx = 581;
	int walkBy = 3587;
	int emptyVial = 465;
	int waterVial = 464;
	
	int[] priorityItems = {HalfDragonSquareShield1,HalfDragonSquareShield2,sinisterKey,halfKey1,halfKey2, 438, irit, 440, kwuarm, 442, dwarfWeed, 815, 817, 819, 821, 823, 933};

	int[] items = {law,deathRunes,CosmicRune,FireRune,AirRune,WhiteBerries,EarthRune,nature,WaterRune,superDef1, guam, tarromin, harralander, groundUnicornHorn, limpwrut, emptyVial, waterVial};

	String[] fModeName = {"Attack","Defence","Strength", "Controlled"};
	int[] fModeIdList = {2,3,1,0};
	
	String[] foodName = {"None","Tunas","Lobs","Swordfish","Sharks","Cake"};
	int[] foodIdList = {-1,367,373,370,546,330};
	
	int[] chaosDruids = {270,555};
	
	boolean devilMode = false;
	
	public Abyte0_YanilleChaos(Extension e) {		super(e);	}
	public void init(String params)
	{
		Frame frame = new Frame("Select Fighting Mode");
		String choiceF = (String)JOptionPane.showInputDialog(frame,		"Items:\n", "Fighting Mode Selection",		JOptionPane.PLAIN_MESSAGE, null, fModeName, null);
		for(int i = 0; i < fModeName.length; i++)
		{
			if (fModeName[i].equals(choiceF))
			{
				fMode = fModeIdList[i];
				break;
			}
		}
		print("fMode = " + choiceF);
		print("--");
		
		if(params.equals("alt"))
		{
			print("ALT");
			walkBx = 584;
			walkBy = 3584;
		}
		
		if(params.equals("dev"))
		{
			print("DEV");
			devilMode = true;
			InitializeLastKnownPositions();
		}
		
		Frame frameFood = new Frame("Select Food");
		String choiceFood = (String)JOptionPane.showInputDialog(frameFood,		"Items:\n", "Food Selection",		JOptionPane.PLAIN_MESSAGE, null, foodName, null);
		for(int i = 0; i < foodName.length; i++)
		{
			if (foodName[i].equals(choiceFood))
			{
				foodId = foodIdList[i];
				break;
			}
		}
		print("Food = " + choiceFood);
		print("--");
		
		print("Yanille chaos druid killer by: Abyte0");
		print("Version 0");
	}
	public int main()
	{
		if(getFightMode() != fMode)
			setFightMode(fMode);
		if(getFatigue() > 90)
		{
			useSleepingBag();
			return 3000;
		}

		if(isBanking()) 
		{
			for(int h = 0; h < priorityItems.length; h++) 
			{
				if(getInventoryCount(priorityItems[h]) > 0) 
				{
					deposit(priorityItems[h], getInventoryCount(priorityItems[h]));
					return random(1000, 1500);				
				}
			}
			for(int h = 0; h < items.length; h++) 
			{
				if(getInventoryCount(items[h]) > 0) 
				{
					deposit(items[h], getInventoryCount(items[h]));
					return random(1000, 1500);				
				}
			}
			
			if(getInventoryCount(foodId) < foodRequired && foodId != -1)
			{
				withdraw(foodId, foodRequired);
				return random(1000, 1500);
			}
			closeBank();		
		}
		//If we need to go bank
		if(!isStillHavingFood() || getInventoryCount() == 30)
		{
			//print("We must go Bank");
			//if we in small house near bank we mmust go outside
			if(getX() >= 589 && getX() <= 593 && getY() >= 761 && getY() <= 764)
			{
				//If not next to door we walk to it
				if(getX() != 591 && getY() != 764)
				{
					walkTo(591,764);
					return random(100,400);
				}
				//Else we open the door if open
				int[] doorObj = getWallObjectById(2);
				if(doorObj[0] != -1)
				{
					if (isAtApproxCoords(doorObj[1], doorObj[2], 2))
					{					
						atWallObject(doorObj[1], doorObj[2]);
						return random(200,500);
					}
				}
				//Else we walk outside
				walkTo(591,765);
				return random(100, 1500);
			}
			//IF WE NEAR BANK BUT OUTSIDE HOUSE
			if(isAtApproxCoords(585, 753, 30))
			{		
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
			
			//downstair near bank we want to go upstair
			if(getX() >= 591 && getX() <= 596 && getY() >= 3590 && getY() <= 3597)
			{
				int[] stair = getObjectById(43);			
				if(stair[0] != -1)
					atObject(stair[1], stair[2]);
				return random(300,400);
			}
			
			//Chaos Druids JUNK SPOT
			if(getX() >= 582 && getX() <= 583 && getY() == 3589)
			{
				//walk to center room
				walkTo(585,3585);
				return random(300,400);
			}
			//Chaos Druids rooom
			if(getX() >= 576 && getX() <= 598 && getY() >= 3580 && getY() <= 3589)
			{
				if(getX() >= 585)
				{
					//door side lest pick lock door
					int[] door = getWallObjectById(162);			
					if(door[0] != -1)
						atWallObject2(door[1], door[2]);
					return random(300,400);
				}
				else
				{
					//Stair Side lest walk to center room
					walkTo(585,3585);
					return random(300,400);
				}
			}
			
		}
		else
		{
			//we can go to war
	
			//Chaos Druids rooom
			if(getX() >= 576 && getX() <= 598 && getY() >= 3580 && getY() <= 3589)
			{				
				//if we are low life and want to use food
				if(getHpPercent() < 60 && foodId != -1)
				{
					if(inCombat())
					{
						RunFromCombat();
						return random(200,400);
					}
					EatFood();
					return random(400,1000);
				}
				else if(getHpPercent() < 40 && foodId == -1)
				{
					if(inCombat())
					{
						RunFromCombat();
						print("You should use food parameter, waiting for heal to restore...");
						return random(2200,4400);
					}
				}
				if(getFatigue() > 80)
				{
					useSleepingBag();
					return 3000;
				}
			
				if(!inCombat())
				{
					
					//We need to Pick Up the Item if any on floor
					for(int h = 0; h < priorityItems.length; h++)
					{
						int[] groundItems = getItemById(priorityItems[h]);
						if(groundItems[0] != -1)
						{
							if(isAtApproxCoords(groundItems[1], groundItems[2], 8))
							{
								pickupItem(groundItems[0], groundItems[1], groundItems[2]);
								return random(1000, 1500);
							}
						}
					}
					
					if(devilMode == true)
					{
						GetNextNPC();
						return 300;
					}
					
					//int[] druids = getAllNpcById(salarinId);
					int[] druids = getNpcById(chaosDruids);
					if(druids[0] != -1 && druids[2] > 3581) 
					{
						attackNpc(druids[0]);
						return random(1200, 1400);
					}
						
					//We need to Pick Up the Item if any on floor
					for(int h = 0; h < items.length; h++)
					{
						int[] groundItems = getItemById(items[h]);
						if(groundItems[0] != -1)
						{
							if(isAtApproxCoords(groundItems[1], groundItems[2], 8))
							{
								pickupItem(groundItems[0], groundItems[1], groundItems[2]);
								return random(1000, 1500);
							}
						}
					}
					if(getX() != walkBx || getY() != walkBy)
					{
						//walkback
						walkTo(walkBx, walkBy);
						return random(100,350);
					}
				}
			}


			
			//if we in front of the room door we want to open and enter
			if(getX() == 591 && getY() == 765)
			{
				//we open the door
				int[] doorObj = getWallObjectById(2);
				if(doorObj[0] != -1)
				{
					if(isAtApproxCoords(doorObj[1], doorObj[2], 2))
					{					
						atWallObject(doorObj[1], doorObj[2]);
						return random(200,500);
					}
				}
				//Else we walk inside
				walkTo(591,764);
				return random(100, 1500);
			}
			
			//if we in small house near bank we mmust go outside
			if(getX() >= 589 && getX() <= 593 && getY() >= 761 && getY() <= 764)
			{
				//we in the hosue so we need ot get downstairs
				int[] stairObj = getObjectById(42);
				if(stairObj[0] != -1)
				{				
					atObject(stairObj[1], stairObj[2]);
					return random(200,500);
				}
			}
			//IF WE NEAR BANK BUT OUTSIDE HOUSE
			if(isAtApproxCoords(585, 753, 30))
			{		
				//we want to walk to the house
				walkTo(591,765);
				return random(400,500);
			}
			
			//downstair near bank we want to get to chaos
			if(getX() >= 591 && getX() <= 596 && getY() >= 3590 && getY() <= 3597)
			{
				int[] door = getWallObjectById(162);			
				if(door[0] != -1)
					atWallObject2(door[1], door[2]);
				return random(300,400);
			}
		
		}
		return random(200, 500);
	}
	
	public final void EatFood()
	{
		if(foodId == -1) return;
		
		if(foodId == 330)
		{
			EatCake();
		}
		else
		{
			int foodIndex = getInventoryIndex(foodId);
			useItem(foodIndex);
		}
	}
	
	private void EatCake()
	{
		int part1 = getInventoryIndex(335);
		int part2 = getInventoryIndex(333);
		int part3 = getInventoryIndex(330);
		if(part1 != -1)
		{
			useItem(part1);
		}
		else if(part2 != -1)
		{
			useItem(part2);
		}
		else if(part3 != -1)
		{
			useItem(part3);
		}
	}
	
	public void RunFromCombat()
	{
		walkTo(getX(),getY());
	}
	
	private boolean isStillHavingFood()
	{
		if(foodId == -1) return true;
		if(foodId == 330)
			return getInventoryCount(foodId,333,335) > 0;
		else
			return getInventoryCount(foodId) > 0;
	}
	
	int maximumNpc = 5;
	int[][] lastKnownPositions = new int[maximumNpc][];
	int[] selectedNPC = {270};
	
	private void GetNextNPC()
	{
		int[] availableNPC = getNpcById(selectedNPC);
		
		if(availableNPC[0] != -1)
		{
			attackNpc(availableNPC[0]);
		}
		
		if(lastKnownPositions[0][0] != -1 && getX() != lastKnownPositions[0][1] && getY() != lastKnownPositions[0][2])
		{
			walkTo(lastKnownPositions[0][1],lastKnownPositions[0][2]);
		}
		
		UpdateStack();
	}
	
	private void UpdateStack()
	{
		//print("UpdateStack");
		int[][] allNpc = getAllNpcsById(selectedNPC);
		
		//printHowmnayArentNull();
		
		if(allNpc.length > 0)
		{
			//print("allNpc.length "+ allNpc.length);
			for(int i = 0; i < allNpc.length; i++)
			{
				if(allNpc[i] == null) continue;
				
				//print("allNpc[i]="+allNpc[i]);	
				//print("allNpc[i][1]="+allNpc[i][1]+" [2]="+allNpc[i][2]);	
				//print("last was = "+lastKnownPositions[maximumNpc-1][1]+","+lastKnownPositions[maximumNpc-1][2]);
					
				if(lastKnownPositions[maximumNpc-1][1] == allNpc[i][1] && lastKnownPositions[maximumNpc-1][2] == allNpc[i][2])
					continue;
										
				boolean shifted = false;
				//print("i " + i + ", lastKnownPositions.length "+ lastKnownPositions.length);
				for(int epos = 0; epos < lastKnownPositions.length-1; epos++) //We do not need to update if its last
				{
					//print("lastKnownPositions[epos][1]="+lastKnownPositions[epos][1]+" [2]="+lastKnownPositions[epos][2]);
					if(isApproxCord(lastKnownPositions[epos],allNpc[i]))
					{
						SetNextAsFirst(epos);
						shifted = true;
						continue;
					}
				}
				
				if(!shifted)
					SetNextAsFirst(0);
				
				SetAsLast(allNpc[i]);
			}
		}
		
	}
	
	private boolean isApproxCord(int[] saved,int[] npc)
	{
		int difX = saved[1] - npc[1];
		int difY = saved[2] - npc[2];
		
		if(difX < 3 && difX > -3 && difY < 3 && difY > -3) return true;
		
		return false;
	}		
	
	
	private void InitializeLastKnownPositions()
	{
		for(int epos = 0; epos < lastKnownPositions.length; epos++)
		{
			lastKnownPositions[epos] = new int[]{-1,-1,-1};
		}
	}
	
	private void printHowmnayArentNull()
	{
		int cpt = 0;
		for(int epos = 0; epos < lastKnownPositions.length; epos++)
		{
			print("inv"+ cpt + " : " +lastKnownPositions[epos][1]+"," +lastKnownPositions[epos][2]);
			cpt++;
		}
	}
	
	private void ShiftUpFrom(int position)
	{
		print("----------  shift from "+ position);
		for(int ep = position; ep < maximumNpc-1; ep++)
		{
			lastKnownPositions[ep] = lastKnownPositions[ep+1];
		}
		
		lastKnownPositions[maximumNpc-1] = new int[]{-1,-1,-1};
	}
	
	private void SetNextAsFirst(int startPosition)
	{
		print("======");
		//printHowmnayArentNull();
		int cpt = 0;
		print("----------  Set from "+ startPosition); 
		for(int ep = startPosition; ep < maximumNpc-1; ep++)
		{
			lastKnownPositions[cpt] = lastKnownPositions[ep+1];
			cpt++;
		}
		for(int rest = cpt; rest < maximumNpc; rest++)
		{
			lastKnownPositions[rest] = new int[]{-1,-1,-1};
		}
		//printHowmnayArentNull();
	}
	
	private void SetAsLast(int[] npc)
	{
		//print("setting last npc to " + npc[1] + "," + npc[2]);
		lastKnownPositions[maximumNpc-1] = npc;
	}
	
}