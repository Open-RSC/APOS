import java.io.*;
import java.util.Scanner;
import java.awt.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.*;
import javax.swing.*;

/*
	By: 			Abyte0
	Date:			2012-03-30
	Private Release: 	2012-04-03
	Public Release: 	2012-04-10
	Use:			Go to Wizard Tower north from Make Over Mage
	2022-01-12		2.0		[Require Abyte_Script 1.6 ]		Talk and only open door sometime to get stuck + stats added on #' keypress
*/

public class Abyte0_DarkWizard extends Abyte0_Script
{
	
	public String SCRIPT_VERSION = "2.0";
	
	int fMode = 2;	//default attack
	int Darkwizard[] = {60,57}; //60 = lvl25; 57 = lvl13

	int[] room0 = {362,570};
	int[] room1 = {362,1514};
	int[] room2 = {362,2458};
	
	int initialXp = 0;
	long initialTime = 0;
	
	//------------- ITEMS
	int halfKey1 = 526;
	int halfKey2 = 527;
	
	int FireRune = 31;
	int WaterRune = 32;
	int AirRune = 33;
	int EarthRune = 34;
	int mindRunes = 35;
	int deathRunes = 38;
	int blood = 619;
	int nature = 40;
	int chaosRunes = 41;
	int law = 42;
	int CosmicRune = 46;
	//-------------------------
	
	int[] items = {halfKey1,halfKey2,law,deathRunes,CosmicRune,chaosRunes,FireRune,AirRune,EarthRune,nature,blood,WaterRune,mindRunes};
	
	int side = 0;
	
	String[] fModeName = {"Attack","Defence","Strength", "Controlled"};
	int[] fModeIdList = {2,3,1,0};
	
	@Override
	public String[] getRandomQuotes()
	{
		String[] result = {
			"How many Law you got?",
			"Die!",
			"...and he think he is a wizard...",
			"That's some serious ladder trainning",
			"Legs day!",
			"Let call it a legs day",
			"I wish there was a lift",
			"fking bots!",
			"This guy is stuck outside",
			"Keep the door open, we nbeed fresh air...",
			"Open the door! @red@Let him out!",
			"@whi@I saw a ghost upstair!",
			"don't go upstair, there is a dead body...",
			"bones down the ladder, I guess someone falled after all those up and down"};
		return result;
	}

	
	public Abyte0_DarkWizard(Extension e) {		super(e);	}
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
		print("--");
		print("fMode = " + choiceF);
		print("--");
		
		
		print("DarkWizard killer by: Abyte0");
		print("Version " + SCRIPT_VERSION);
		print("--");
		
		
		initialXp = getFmodeXp();
		initialTime = System.currentTimeMillis();
	}
	public int main()
	{
		SayRandomQuote();
		
		if(getFightMode() != fMode)
			setFightMode(fMode);
		if(getFatigue() > 90)
		{
			useSleepingBag();
			return 3000;
		}
		
		if(isAtApproxCoords(room0[0],room0[1], 30))
			side = 2;
		else if(isAtApproxCoords(room2[0],room2[1], 30))
			side = 0;
			
		//if we are low life
		if(getHpPercent() < 20)
		{
			if(inCombat())
			{
				RunFromCombat();
				return random(200,400);
			}
			stopScript();
			return random(600,1000);
		}
		if(!inCombat())
		{
			// If we see closed door we must open
			int[] door = getWallObjectById(2);
			if(door[0] != -1)
			{
				boolean ignoreOrNotDice = random(0,5) > 2;
				
				if(isAtApproxCoords(door[1], door[2], 4) && ignoreOrNotDice)
				{	
					//print("Open Door");
					atWallObject(door[1], door[2]);
					return 1000;
				}
			}	
			//We need to Pick Up the Item if any on floor
			for(int h = 0; h < items.length; h++)
			{
				int[] groundItems = getItemById(items[h]);
				if(groundItems[0] != -1)
				{
					//if in combat we run else we pickup
					if(inCombat())
					{
						RunFromCombat();
						return random(500,800);
					}
					if(isAtApproxCoords(room0[0],room0[1],15) || isAtApproxCoords(room1[0],room1[1],15) || isAtApproxCoords(room2[0],room2[1],15)) //If near tower to avoid getting lured
					{
						if(isAtApproxCoords(groundItems[1], groundItems[2], 15))
						{
							//print("PickUp");
							pickupItem(groundItems[0], groundItems[1], groundItems[2]);
							return random(1000, 1500);
						}
					}
				}
			}
			//we need ot fight the npc
			int[] npc = getNpcById(Darkwizard[0]);
			if(npc[0] != -1) 
			{
				//print("Attack level 25");
				attackNpc(npc[0]);
				return random(400,700);
			}
			npc = getNpcById(Darkwizard[1]);
			if(npc[0] != -1) 
			{
				//print("Attack level 13");
				attackNpc(npc[0]);
				return random(400,700);
			}
			//if no monster when we may want to use ladder
			
			//if we in on floor 0, we want to go to floor 2
			if(isAtApproxCoords(room0[0],room0[1], 30))
			{
				//print("Floor 0 to 1");
				int i = goFloor0to1();
				if(i != 0)
					return i;
			}
			//if we in on floor 1 AND want to go to floor 2
			if(isAtApproxCoords(room1[0],room1[1], 30) && side == 2)
			{	
				//print("Floor 1 to 2");
				int i = goFloor1to2();
				if(i != 0)
					return i;
			}
			
			//if we in on floor 2, we want to go to floor 1
			if(isAtApproxCoords(room2[0],room2[1], 30))
			{	
				//print("Floor 2 to 1");
				int i = goFloor2to1();
				if(i != 0)
					return i;
			}
			
			//if we in on floor 1 AND want to go to floor 0
			if(isAtApproxCoords(room1[0],room1[1], 30) && side == 0)
			{	
				//print("Floor 1 to 0");
				int i = goFloor1to0();
				if(i != 0)
					return i;
			}
			print("Not in combat waitting");
			return random(1400,2000);
		}
		//if(getX() >= MIN && getX() <= MAX && getY() >= MIN && getY() <= MAX)
		return random(1500, 2000);
	}

	public int goFloor0to1()
	{
		int[] ladder = getObjectById(5);
		if(ladder[0] != -1)
		{					
			atObject(ladder[1], ladder[2]);
			return random(900,1400);
		}	
		return 0;
	}
	
	public int goFloor1to2()
	{
		int[] ladder = getObjectById(5);
		if(ladder[0] != -1)
		{					
			atObject(ladder[1], ladder[2]);
			return random(900,1400);
		}	
		return 0;
	}
	
	public int goFloor2to1()
	{
		int[] ladder = getObjectById(6);
		if(ladder[0] != -1)
		{					
			atObject(ladder[1], ladder[2]);
			return random(900,1400);
		}
		return 0;
	}
	
	public int goFloor1to0()
	{
		int[] ladder = getObjectById(6);
		if(ladder[0] != -1)
		{					
			atObject(ladder[1], ladder[2]);
			return random(900,1400);
		}
		return 0;
	}
	
	public void RunFromCombat()
	{
		walkTo(getX(),getY());
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
		initialXp = getFmodeXp();
		initialTime = System.currentTimeMillis();
	}
	
	protected void reportXpChange()
	{
		
		int xpDifference = getFmodeXp() - initialXp;
		long timeSpan = System.currentTimeMillis() - initialTime;
		long secondSpan = timeSpan / 1000;
		long xpRatio = xpDifference * 3600L / secondSpan; //The L set 3600 as long variable Forcing to calculate as long to avoid overflow
		

		print("=================================");
		print("initialXp: " + initialXp);
		print("initialTime: " + initialTime);
		print("total fMode xp gained: " + xpDifference);
		print("time running: " + secondSpan + " s");
		print("xpRatio: " + xpRatio + "/h");
		print("=================================");
	}
	
	private int getFmodeXp()
	{
		if(fMode == 1)
			return getXpForLevel(2);
		if(fMode == 2)
			return getXpForLevel(0);
		if(fMode == 3)
			return getXpForLevel(1);
		
		return getXpForLevel(0) + getXpForLevel(1) + getXpForLevel(2);
	}

	@Override
	public String getSctiptVersion()
	{
		return SCRIPT_VERSION;
	}
	
}