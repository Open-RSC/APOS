/**
* Edited by AByte0
*/
public class Abyte0_CombatMager extends Script 
{

	int spellID = 8;
	int[] npcs = new int[]{65};
	int foodID = 367;

	public Abyte0_CombatMager(Extension e) {
	super(e);
	}

	public void init(String params)
	{
		String[] in = params.split(",");
		spellID = Integer.parseInt(in[0]);
		foodID = Integer.parseInt(in[1]);
		//npcs = new int[in.length - 2];
		
		for(int i = 0; i < npcs.length; i++)
			npcs[i] = Integer.parseInt(in[i + 2]);
		System.out.println("casting " + SPELL[spellID] + " on:");
		for(int i = 0; i < npcs.length; i++) 
			System.out.println("\t" + npcs[i]);
	}

	public void RunFromCombat()
	{
		System.out.println("Begin Run to eat");
		int x = getX();
		int y = getY();
		walkTo(x,y);
	}
	
	public int main()
	{
		if(getHpPercent() < 50)
		{
			System.out.println("It need to eat!");
			if(inCombat())
			{
				RunFromCombat();
				return random(7, 125) + 2602;
			}
			if(hasInventoryItem(foodID))
			{
				System.out.println("Glurp!");
				int foodIndex = getInventoryIndex(foodID);
				System.out.println("food position" + foodIndex);
				useItem(foodIndex);
				return random(7, 125) + 2602;
			}
			else
			{
				System.out.println("No food");
				stopScript();
				return 1;
			}
		}

		if(getFightMode() != 3)
		{
			System.out.println("Setting Fight Mode!");
			setFightMode(3);
		}

		if(getFatigue() > 90)
		{
			useSleepingBag();
			return 1000;
		}
		int[] rat = getAllNpcById(npcs);
		if(rat[0] != -1) 
		{
			mageNpc(rat[0], spellID);
			return random(0, 100) + 1750;
		}

		return random(0, 100) + 1750;
	}
}

