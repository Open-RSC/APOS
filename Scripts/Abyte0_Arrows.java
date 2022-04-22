/*
Harry made this script quickly but full working and Abyte0 pimped it!
2012-02-22 RIP COD 2 ans
*/

public class Abyte0_Arrows extends Abyte0_Script {

	int shaft = 280;
	int feathers = 381;
	int headless = 
	637;
	int bronzeHead = 669;
	int ironHead = 670;
	int steelHead = 671;
	int mithHead = 672;
	int addyHead = 673;
	int runeHead = 674;

	int item1;
	int item2;

	public Abyte0_Arrows ( Extension e )
	{
		super(e);
	}

	public void init ( String param )
	{
		System.out.println("Original Script by: Harry");
		System.out.println("Edited by: Abyte0");

		if(param.equals("feathers"))
		{
			item1 = shaft;
			item2 = feathers;
		}
		else if(param.equals("bronze"))
		{
			item1 = headless;
			item2 = bronzeHead;
		}
		else if(param.equals("iron"))
		{
			item1 = headless;
			item2 = ironHead;
		}
		else if(param.equals("steel"))
		{
			item1 = headless;
			item2 = steelHead;
		}
		else if(param.equals("mith"))
		{
			item1 = headless;
			item2 = mithHead;
		}
		else if(param.equals("addy"))
		{
			item1 = headless;
			item2 = addyHead;
		}
		else if(param.equals("rune"))
		{
			item1 = headless;
			item2 = runeHead;
		}
		else
		{
			System.out.println("Params is one of those: feathers,bronze,iron,steel,mith,addy or rune");
			stopScript();
		}
	}

	public int main( )
	{
		if(getFatigue() > 65)
		{
			useSleepingBag();
			return random(1000,2000);
		}
		useItemWithItem(getInventoryIndex(item1), getInventoryIndex(item2));
		return random(1000,1500);
	}


}
