//Abyte0
//2012-01-18
//Gnome City Bow String Maker
//2021-07-06 Tested with OpenRSC

public class Abyte0_Flax extends Abyte0_Script
{
	int idFlax = 675;
	int idString = 676;
	int idBanker = 540;
	boolean power = false;
	boolean isDroppingNow = false;

	public Abyte0_Flax(Extension e)
	{ super(e); }
	
	public void init(String param) 
	{		
		print("USAGE Abyte0_Flax nothing = banking OR power = powerflax");
		print("String Maker for Gnome City");
		print("Version 1.0 OpenRSC Compatible");
		print("Make sure you are strong to fight a gnome level 23 bare, or get 47 cb");
		print("Do not wear armor/weapon, only sleeping bag must be in your inv.");
		
		if(param.equals("power") || param.equals("Power") || param.equals("POWER"))
		{
			power = true;
			print("Dropping @mag@-WHY WOULD YOU DO THAT!? @red@BANK THEM!-");
		}
		else
		{
			power = false;
			print("Banking the -Bow String-");
		}
	}	
	
	public int main()
	{
		// sleep if needed   
		if(getFatigue() > 90)
		{
			useSleepingBag();
			return 1000;
		}
		if(isBanking()) 
		{			
			//On Depose les FLAX
			if(getInventoryCount(idString) > 0) 
			{				
				deposit(idString,getInventoryCount(idString));
				return random(500, 600);			
			}
			//Si il reste uniquement le Bag
			if(getInventoryCount() == 1) 
			{				
				closeBank();				
				return random(500, 600);			
			}
			return random(500, 600);		
		}				
		if(isQuestMenu()) 
		{			
			answer(0);			
			return random(3500, 3600);
		}
		
		if(isDroppingNow)
		{
				if(getInventoryCount(idString) != 0)	
					dropItem(getInventoryIndex(idString));
				else 
					isDroppingNow = false;
				
				return random(300,400);
		}
		
		//Si plein de String
		if(getInventoryCount(idString) == 29)
		{
			if(power)
			{
				isDroppingNow = true;
				return random(300,400);
			}
			//si pres des bankers
			if(isAtApproxCoords(715,1452,16))
			{
				int banker[] = getNpcByIdNotTalk(idBanker);	        
				if(banker[0] != -1)
				{			
					talkToNpc(banker[0]);	        
					return random(2400,2600);		
				}
				return 500;
			}
			//si Pres du Spinner
			if(isAtApproxCoords(692,1468,16))
			{
				atObject(692, 1469);
				return random(500, 600);
			}
			//si sur le chemin public
			if(isAtApproxCoords(702,522,30))
			{
				if(getX() < 702)
					walkTo(702,522);
				else
					atObject(714,516);
				return 500;
			}
			//print("lost FULL");
			return random(100,200);
		}

		//Si PAS PLEIN
		if(getInventoryCount() < 30)
		{
			//si pres des bankers
			if(isAtApproxCoords(715,1452,16))
			{
				atObject(714,1460);
				return random(400,600);
			}
			//si Pres du Spinner
			if(isAtApproxCoords(692,1468,16))
			{
				atObject(692, 1469);
				return random(500, 600);
			}
			//si sur le chemin public
			if(isAtApproxCoords(702,522,35))///was 30 trying 35 to fix bug
			{
				//print("if i dont move its cause i cant reach 693 524, im at "+getX() + ","+ getY());
				atObject2(698, 521);
				return random(300,400);
			}
			print("lost EMPTY");
			return random(100,200);
		}
		
		//Si plein de Flax
		if(getInventoryCount(idFlax) == 29 || getInventoryCount(idString) + getInventoryCount(idFlax) == 29)
		{
			//si pres des bankers
			if(isAtApproxCoords(715,1452,16))
			{
				atObject(714,1460);
				return random(400,600);
			}
			//si Pres du Spinner
			if(isAtApproxCoords(692,1468,16))
			{
				useItemOnObject(675, 121);
				return random(555, 666);
			}
			//si sur le chemin public
			if(isAtApproxCoords(702,522,30))
			{
				if(getX() > 702)
					walkTo(701,522);
				else
					atObject(692, 525);
				return 500;
			}
			return random(100,200);
		}
		return 100;
	}
}