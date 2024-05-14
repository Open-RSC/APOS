//Version 1 - 2024-05-01 - Fix report word to add extras P to avoid triggering admins warning

public class Abyte0_FlyFisher extends Abyte0_Script
{

	int extraDelay = 0;
	
	int initialXp = 0;
	long initialTime = 0;
	int defaultTime = 420;
	boolean needToMove = false;
	
	@Override
	public String[] getRandomQuotes()
	{
		String[] result = {"Fish level?","What is your cook level?","Trout!","Nice large salmon","Selling Fresh fish!","Dropping fresh Salmon!","There's a fine line between fishing and just standing on the shore like an idiot.","I like to fish. Fishing is always a way of relaxing.","I didn't think I had time for fishing before I fished","The fun of fishing is catching 'em, not killing 'em.","I'm a fisherman. I've always loved fishing. I grew up fishing for trout.","I only make movies to finance my fishing.","Fishing has always been an excuse to drink in the daytime","What I miss most about living in Alaska is the fishing...","I'm bad at fishing for information","Fishing has been part of our culture since we landed here","When I go Miami, I go deep-sea fishing. I love doing that.","Its either fishing rods or golf clubs for me, one or the other","I grew up hunting and fishing, as did my family. But then I served in the military","I am fishing to PK","I really enjoy fishing, do you?","I really enjoy fishing","Are you 99 fish yet?","I bet you are still level 60 fishing?","@ran@Bot! I'll reppppport you!","How many feathers do you got left?"};
		return result;
	}

	public Abyte0_FlyFisher(Extension e)
	{
		super(e);
	}    
	
	public void init( String params )
	{
		print("Abyte0 : Power Fly Fisher V1");
		print("Abyte0 : Use at barbar village");
		print("Abyte0 : F2 reset counters");
		print("Abyte0 : Numpad + increase delay");
		print("Abyte0 : Numpad - decrease delay");
		
		initialXp = getFishingXp();
		initialTime = System.currentTimeMillis();
	}
	
	public int main()
	{
		SayRandomQuote();
		if(needToMove)
		{
			changePosition();
			return 500;
		}
		
		if(getFightMode() != 3)
		{
			setFightMode(3);
		}
		
		if(getFatigue() > 95)
		{
			useSleepingBag();
			return 1000;
		}
		
		atObject(208,501);
		return defaultTime + extraDelay;
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
		initialXp = getFishingXp();
		initialTime = System.currentTimeMillis();
	}
	
	protected void reportXpChange()
	{
		
		int xpDifference = getFishingXp() - initialXp;
		long timeSpan = System.currentTimeMillis() - initialTime;
		long secondSpan = timeSpan / 1000;
		long xpRatio = xpDifference * 3600L / secondSpan; //The L set 3600 as long variable Forcing to calculate as long to avoid overflow
		

		print("=================================");
		print("initialXp: " + initialXp);
		print("initialTime: " + initialTime);
		print("total Fishing xp gained: " + xpDifference);
		print("time running: " + secondSpan + " s");
		print("xpRatio: " + xpRatio + "/h");
		print("=================================");
		print("fish time = " + (defaultTime + extraDelay));
	}
	
    @Override
    public void onServerMessage(String s) {

        if (s.contains("standing here for 5 mins!")) {
			needToMove = true;
        }
		
    }

    @Override
    public void onChatMessage(String msg, String name, boolean mod, boolean admin) {
        
		//Do not reply to yourself
		final String lname = client.getPlayerName(client.getPlayer());		
        if(name.equalsIgnoreCase(lname)) return;
		
		String receivedLC = msg.toLowerCase();
		
        if (receivedLC.contains("feather") && receivedLC.contains("?")) {
			if(receivedLC.contains("you"))
				Say("Nice, I still have " + getInventoryCount(381) + " feathers...");
			else
				Say("I got " + getInventoryCount(381) + " feathers left, what about you?");
        }
		
        if (receivedLC.contains("miami") || receivedLC.contains("landed") || receivedLC.contains("fisherman") || receivedLC.contains("hunting")) {
			Say("hehe, where are you from?");
		}
		
		super.onChatMessage(msg, name, mod, admin);
    }

    public void onKeyPress(int keyCode) {
		if (keyCode == 192 || keyCode == 128) { //#, ` next to 1 above tab
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
		if(getX() == 210)
			needToMove = false;
		else
			walkTo(210,501);
	}
	
	//97 fishing
	//240 = 53k / h after 37500 s
	//330 = 52k / h after 5700s
	//420 = 53k / h after 10300s
	//990 = 47k / h after 5200s
	
}