/** * Based on Any Thiever from yomama` *//** 
*  pickpockets anything, eats/banks (cakes) * 
*	- yomama` */
public class Abyte0_Thiever extends Script 
{		
	int fightMode = 3;	
	int[] npcID = new int[]
	{
		322, //Knight
		323  //Paladin
	};		
	int[] dropIDs = new int[]
	{		
		140, //jug		
		612, //fire orb		
		714, //lockpick		
		559, //Poisoned Iron dagger		
		161, //diamond		
		152  //gold	
	};
	int[] foodIDs = new int[] 
	{		
		330, //cake 3/3		
		333, //cake 2/3		
		335, //cake 1/3		
		895, //Swamp Toad		
		897, //King worm		
		138, //bread		
		142, //wine	
		373, //Lobs
		367, //Tunas
		370, //Swordy
		546,  //Sharks
		350  //Shrimp
	};		
		
	public Abyte0_Thiever(Extension e) {super(e);}		
	public void init(String params) 
	{		
		System.out.println("Abyte0_Thiever fmode,npc,npc,npc...");
		System.out.println("Guards: 321? Knight 322? Pally 323?");
		
		System.out.println("Abyte0_Thiever V0");
		String[] in = params.split(",");		
		fightMode = Integer.parseInt(in[0]);		
		npcID = new int[in.length - 1];		
		for(int i = 0; i < npcID.length; i++)			
			npcID[i] = Integer.parseInt(in[i + 1]);	
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
		if(getInventoryCount(dropIDs) > 0) {			
			dropItem(getInventoryIndex(dropIDs));		
		}		
		

		if(getHpPercent() < 70) 
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
					return random(60000,90000);
				}
			}	    	
			useItem(idx); 	    	
			return random(500, 600);    	
		}				
		int[] npc = getNpcById(npcID);		
		if(npc[0] != -1)
		{		
			thieveNpc(npc[0]);		
			return random(600, 1111);
		}
		return random(500, 1000);
	}
}