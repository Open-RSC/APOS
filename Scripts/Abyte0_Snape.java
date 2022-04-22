//Made by ??
//Edited by Abyte0

public class Abyte0_Snape extends Script 
{	
	int step = 0;	
	int foodId = 373;
	int fmode = 2;
	int[] path = null;	int loop = 0;
	//client extension;	
	public Abyte0_Snape(Extension e) 
	{		
		super(e);	
		//extension = new client();
	}	
	public void init(String params) 
	{		
		String str[]=params.split(",");

		if(str.length >= 2)
		{
			fmode = Integer.parseInt(str[0]);
			foodId = Integer.parseInt(str[1]);
		}
		else
		{
			print("You should use: Abyte0_GetSnape Fmode,FoodId      lobs = 373");
			print("It does eat at Bank....");
		}
		print("=-=-=-=-=-=-=-=-=");print(" ");
		print("Snape / Limp Collector - By mofo");	
		print("Edited by Abyte0 to eat and chose F Mode and FoodId");	
		print("-");
		print("V2");
		print("-");
		print("Fmode = " + fmode);
		print("foodId = " + foodId);
		//extension.System.out.println("Lost connection");
	}	
	public int main() 
	{		
		if(getFightMode() != fmode)			
			setFightMode(fmode);		
		if(getFatigue() > 90) 
		{			
			useSleepingBag();			
			return 1000;		
		}		
		if(isBanking()) 
		{			
			if(getInventoryCount(220) > 0) 
			{				
				deposit(220, getInventoryCount(220));				
				return random(1000, 1500);			
			}			
			if(getInventoryCount(469) > 0) 
			{				
				deposit(469, getInventoryCount(469));				
				return random(1000, 1500);			
			}	
			if(getHpPercent() < 70)
			{
				if(getInventoryCount(foodId) < 1) 
				{	
					
					withdraw(foodId,1);
					return random(1010, 1300);
				}
				return 400;
			}	
			closeBank();		
		}		
		if(isQuestMenu()) 
		{			
			answer(0);			
			return random(6000, 6500);		
		}		
		if(isAtApproxCoords(364, 608, 20) && getInventoryCount() != 30) 
		{			
			step = 0;			
			path = new int[] {351, 621, 336, 620, 320, 607, 307, 594, 294, 584, 282, 569};			
			int[] limp = getItemById(220);			
			int[] snape = getItemById(469);						
			if(limp[0] != -1) 
			{				
				pickupItem(limp[0], limp[1], limp[2]);				
				return random(1000, 1500);			
			}	
			if(snape[0] != -1) 
			{				
				pickupItem(snape[0], snape[1], snape[2]);				
				return random(1000, 1500);			
			}			
			return 1000;		
		}		
		if(isAtApproxCoords(282, 569, 4) && (getInventoryCount(469) > 0 || getInventoryCount(220) > 0)) 
		{			
			int[] bankdoor = getObjectById(64);			
			if(bankdoor[0] != -1) 
			{				
				atObject(bankdoor[1], bankdoor[2]);				
				return random(1000, 1500);		    
			}			
			step = 0;			
			path = new int[] {294, 584, 307, 594, 320, 607, 336, 620, 351, 621, 364, 608};			
			int[] npc = getNpcById(95);			
			if(npc[0] != -1) 
			{				
				if(getHpPercent() < 60)
				{
					EatFood();
					return random(400,1000);
				}
				talkToNpc(npc[0]);				
				return random(2000, 2700);			
			}		
		}		
		if((step + 1) < path.length) 
		{			
			int[] bankdoor = getObjectById(64);			
			if(bankdoor[0] != -1) 
			{				
				atObject(bankdoor[1], bankdoor[2]);				
				return random(1000, 1500);		    
			}			
			if(isAtApproxCoords(path[step], path[step + 1], 2))				
				step = step + 2;			
			walkTo(path[step] + random(-2, 2), path[step + 1] + random(-2, 2));			
			return random(2000, 2500);		
		}		
		loop++;		
		if(loop > 10) 
		{			
			if(isAtApproxCoords(282, 569, 10))				
				path = new int[] {294, 584, 307, 594, 320, 607, 336, 620, 351, 621, 364, 608};			
			step = 0;			
			loop = 0;		
		}		
		return random(1000, 1200);	
	}
	
	public final void print(String gameText)
	{
		System.out.println(gameText);
	}
	
	public final void EatFood()
	{
		int foodIndex = getInventoryIndex(foodId);
		useItem(foodIndex);
	}
}
