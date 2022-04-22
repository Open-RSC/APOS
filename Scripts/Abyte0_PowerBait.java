/**
* CREATED by AByte0
*/
public class Abyte0_PowerBait extends Script 
{

	String param = "";

	public Abyte0_PowerBait(Extension e)
	{
		super(e);
	}

	public void init(String params)
	{
		param = params;
	}
	
	public int main()
	{
		if(getFatigue() > 80)
		{
			useSleepingBag();
			return 5000;
		}
		if(isObjectAt(414,502))
		{
			atObject2(414,502);
			System.out.println("Bait");
			return 2000;
		}
		else
		{
			System.out.println("Aucun Object");
			stopScript();
			return 1;
		}
	}
}

