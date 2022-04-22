public class Abyte0_FlaxAlcher extends Abyte0_Script
{

	int alchId = 118;
	int natureRunes = 40;
	int idFlax = 675;
	
	int extraDelay = 0;
	
	int initialXp = 0;
	long initialTime = 0;
	int defaultCastTime = 990;
	
	public Abyte0_FlaxAlcher(Extension e)
	{
		super(e);
	}    
	
	public void init( String params )
	{
		print("Abyte0 : Flax Alcher");
		print("Abyte0 : Use between Seer village and Catherby");
		print("Abyte0 : F2 reset counters");
		print("Abyte0 : Numpad + increase delay");
		print("Abyte0 : Numpad - decrease delay");
		
		initialXp = getMagicXp();
		initialTime = System.currentTimeMillis();
	}
	
	public int main()
	{
		if(getFightMode() != 3)
		{
			setFightMode(3);
		}        
		if(getFatigue() > 95)
		{
			useSleepingBag();
			return 1000;
		}
		    
		if(getInventoryCount(idFlax) == 0)
		{
			atObject(489,486);
			return 5;
		}
		else
		{
			castOnItem(28,getInventoryIndex(idFlax));
			atObject(489,486);
			return defaultCastTime + extraDelay;
		}
	}
	
	private void decreaseDelay()
	{
		extraDelay -= 10;
		print("extra delay is now " + extraDelay);
	}
	
	private void increaseDelay()
	{
		extraDelay += 10;
		print("extra delay is now " + extraDelay);
	}
	
	private void resetCounters()
	{
		initialXp = getMagicXp();
		initialTime = System.currentTimeMillis();
	}
	
	@Override
	public void reportXpChange()
	{
		
		int xpDifference = getMagicXp() - initialXp;
		long timeSpan = System.currentTimeMillis() - initialTime;
		long secondSpan = timeSpan/1000;
		long xpRatio = xpDifference * 3600 / secondSpan;
		

		print("=================================");
		print("initialXp: " + initialXp);
		print("initialTime: " + initialTime);
		print("total Magic xp gained: " + xpDifference);
		print("time running: " + timeSpan/1000 + " s");
		print("xpRatio: " + xpRatio + "/h");
		print("=================================");
		print("cast time = " + (defaultCastTime + extraDelay));
	}
	
	private int getMagicXp()
	{
		return getXpForLevel(6);
	}

    @Override
    public void onServerMessage(String s) {

        if (s.contains("standing here for 5 mins!")) {
			changePosition();
        }
		
    }

    public void onKeyPress(int keyCode) {
		if (keyCode == 192) { //#
			reportXpChange();
        }
		if (keyCode == 107) { //+
			increaseDelay();
        }
		if (keyCode == 109) { //-
			decreaseDelay();
        }
		if (keyCode == 113) { //F2
			resetCounters();
        }
		
		
		//print(""+keyCode);
    }
    
	private void changePosition()
	{
		if(getX() == 490)
			walkTo(488,486);
		else
			walkTo(490,486);
	}
	//500 s = 193.4k/h @ 330
	//500 s = k/h @ 990
	//500 s = 155.5k/h @ 1750
	//500 s = 159.7k/h @ 1800
}