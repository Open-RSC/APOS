public class Abyte0_BettyEyesShopper extends Abyte0_Script
{
	int betty = 149;
	int coins = 10;
	int eyes = 270;
	int fMode = 3;
	boolean buyRunes = true;
	private final String SCRIPT_VERSION = "1.0";
	
	
	public Abyte0_BettyEyesShopper(Extension e)
	{
		super(e);
	}    
	
	public void init( String params )
	{
		print("Started Abyte0 Betty Eyes Shopper");
		print("Version "+ SCRIPT_VERSION);
		print("Any param = Eyes only");
		print("No param = Eyes + runes");
		if(params.equals(""))
			buyRunes = true;
		else
			buyRunes = false;
	}    
	public int main()
	{
		if(getFightMode() != fMode)
		{
			setFightMode(fMode);
		}
		if(isBanking())
		{
			for(int i = 31; i<37; i++)
			{
				if(getInventoryCount(i) > 0)
				{                
					deposit(i,getInventoryCount(i));
					return 100;
				}
			}
			
			if(getInventoryCount(eyes) > 0)
			{               
				deposit(eyes,1);
				return 100;
			}          
			
			if(getInventoryCount(coins) < 2500)
			{                
				withdraw(coins,25000);
				return 1000;
			}     
			closeBank();
			return 1000;
		}
		if(isQuestMenu())
		{
			answer(0);
			return 1000 + random(300, 1200);
		}
		
		if(getInventoryCount() == 30 || getInventoryCount(coins) < 10)
		{
			if(getY() > 629)
			{                
				walkTo(278,629);
				return 1000;
			}
			while(getY() > 575)
			{
				walkTo(290,getY()-3);
				return 500;
			}
			if(getX() == 287 && getY() == 572)
			{	
				//Si a coter de la porte a exterieur
				print("Step Inside Bank");	
				//atObject(287,571);
				walkTo(286,571);
				return 3000;
			}
			if(getX() >= 280 && getX() <= 286 && getY() >= 564 && getY() <= 573)
			{
				//Si dans la banque
				print("Talking to Banker");											
				if(!isBanking())
				{					
					int banker[] = getNpcByIdNotTalk(new int[]{95});										
					if (banker[0] != -1 && !isBanking())
					{						
						talkToNpc(banker[0]);						
						return 3000;
					}
				}	
				return 1000;
			}
			if(getY() < 575)
			{
				walkTo(287,572);				
				return 1000 + random(300, 1200);
			}
			print("Walking to 287,571");
			walkTo(287,571);
			return 1000 + random(300, 1200);
		}
		else
		{
			if(shopWindowOpen())
			{
				//then we close the shop
				if(getInventoryCount() == 30)
				{
					closeShop();
					return 2500;
				}
				
				if(buyRunes)
				{					
					for(int i = 31; i<36; i++)
					{
						if(getShopItemIdAmount(i) > 0)
						{
							buyItemIdFromShop(i,50);
							return 200;
						}
					}
				}
				
				buyItemIdFromShop(eyes,1);
				return 100;
			}
			
			if(getX() == 286 && getY() == 571)
			{	
				//Si a coter de la porte a linterieur
				print("Step Outside Bank");	
				//atObject(287,571);
				walkTo(287,571);
				return random(121, 3500);
			}
			if(getX() >= 280 && getX() <= 286 && getY() >= 564 && getY() <= 573)
			{
				//Si dans la banque
				print("Walking to Door");	
				walkTo(286,571);
				return random(240, 2500);
			}
			while(getY() < 620)
			{
				walkTo(290,getY()+3);
				return 500;
			}
			if(getY() < 628)
			{
				walkTo(285,628);
				return 3000;
			}            
			if(getX() > 274)
			{
				walkTo(getX()-2,629);                
				return 1000;
			}
			
			if(getX() <= 274)
			{			
				int bettyRef[] = getNpcByIdNotTalk(new int[]{betty});										
				if (bettyRef[0] != -1)
				{						
					talkToNpc(bettyRef[0]);						
					return 2500;
				}
				return 3000;
			}
			return 5000;
		}
	}
	
	@Override
	public String getSctiptVersion()
	{
		return SCRIPT_VERSION;
	}
	
}