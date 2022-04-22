public class Abyte00_Xmas extends Abyte0_Script
{

	int startX = 0;
	int startY = 0;
	int phatColor = 0;
	int currentPhat = 576;
	int firstPhat = 576;
	int lastPhat = 581;

	@Override
	public String[] getRandomQuotes()
	{
		//String[] result = {"@ran@Merry xmas!","@mag@Merry Christmas!","@red@Ho!Ho!Ho!","Merry Christmas","Whoa! it's cold outside!","Phats!","Crack it on me!","I wish you some good time","Take care","Peace","May the joy of this festive season fill your life with happiness and peace.","I’m telling you why...","Santa Claus is coming to town!","You better watch out, you better not cry, better not pout","I’m telling you why...","Santa Claus is coming to town!","You better watch out, you better not cry, better not pout"};
		String[] result = {""};
		return result;
	}

	public Abyte00_Xmas(Extension e)
	{
		super(e);
	}    
	
	public void init( String params )
	{
		print("Abyte0 : Xmas party");
		
		startX = getX();
		startY = getY();
		currentPhat = firstPhat;
	}
	
	public int main()
	{
		SayRandomQuote();
		
		wearNextColor();
		
		return 1000;
	}
	
	public int nextColor(int current)
	{
		//print("current is " + current);
		int next = current+1;
		//print("next is " + next);
		
		if(next > lastPhat)
		{
			next = firstPhat;
			
			//print("back to first");
		}
		
		
		if(getInventoryCount(next) > 0)
		{
			//print("next is available " + next );
			return next;
		}
		
		return nextColor(next);
	}
	
	private void wearNextColor()
	{
		int phat = nextColor(currentPhat);
		
		if(currentPhat != phat)
		{
			//print("switch from " + currentPhat +" to " + phat);
			wearItem(getInventoryIndex(phat));
			currentPhat = phat;
		}
	}
	
    @Override
    public void onServerMessage(String s) {

        if (s.contains("standing here for 5 mins!")) {
			changePosition();
        }
		
    }

    public void onKeyPress(int keyCode) {
		if (keyCode == 192) { //#
			setInitialPosition();
        }
		//if (keyCode == 107) { //+
		//	increaseDelay();
        //}
		//if (keyCode == 109) { //-
		//	decreaseDelay();
        //}
		//if (keyCode == 113) { //F2
		//	resetCounters();
        //}
		
		
		//print(""+keyCode);
    }
    
	private void setInitialPosition()
	{
		startX = getX();
		startY = getY();
		print("New position : " + startX + "," + startY);
	}
	
	private void changePosition()
	{
		if(getX() == startX && getY() == startY)
			walkTo(startX,startY+1);
		else
			walkTo(startX,startY);
	}
}