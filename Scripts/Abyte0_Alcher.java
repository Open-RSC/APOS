public class Abyte0_Alcher extends Abyte0_Script
{

	int steelPl8 = 118;
	int natureRunes = 40;
	int nombreSP = 26;
	
	int nbNatsWithdraw = 0;
	int nbPl8Withdraw = 0;
	int MoneyDeposit = 0;
	
	int banker = 95;
	
	String barType;
	
	int[] doorObj;

	public Abyte0_Alcher(Extension e)
	{
		super(e);
	}    
	public void init( String params )
	{
		print("Abyte0 : Alcher...");
		print("Version 2");
		print("No param = steel pl8 , yew = yewlognbow, magic = magic long bow");
		
		int yewLongBow = 654;
		int magicLongBow = 656;
		
		if(params.equals("yew"))
			steelPl8 = yewLongBow;
		else if(params.equals("magic"))
			steelPl8 = magicLongBow;
		else
			steelPl8 = 118;
	}    
	public int main()
	{
		return Mine();
	}    
	public int Mine()
	{
		if(getFightMode() != 2)
		{
			setFightMode(3);
		}        
		if(getFatigue() > 70)
		{
			useSleepingBag();
			return 1000;
		}
		if(isBanking())
		{      
			if(getInventoryCount(natureRunes) <= 30)
			{             
				//on withdraw les nats et on depose les steel pl8 avant pour que les steel pl8 soit apres les nats pas alcher de nats
				nbPl8Withdraw = nbPl8Withdraw - getInventoryCount(steelPl8);
				deposit(steelPl8,getInventoryCount(steelPl8)); 
				withdraw(natureRunes,100);
				nbNatsWithdraw = nbNatsWithdraw + 100; 
				return 1000 + random(10,500);
			}
			if(getInventoryCount(10) >=500000)
			{             
				//Deposit coins after 500k
				deposit(10,500000); 
				MoneyDeposit = MoneyDeposit + 500000; 
				return 1000 + random(10,500);
			}
			if(getInventoryCount(197) == 0)
			{             
				//staff
				withdraw(197,1);
				return 1000 + random(10,500);
			}
			else if(getInventoryCount(steelPl8) < nombreSP)
			{               
				nbPl8Withdraw = nbPl8Withdraw + nombreSP-getInventoryCount(steelPl8);
				withdraw(steelPl8,nombreSP-getInventoryCount(steelPl8));
				return 1000 + random(10,500);
			}                    
			else if(getInventoryCount(steelPl8) == nombreSP && getInventoryCount(natureRunes) > 0)
			{
				closeBank();
				print("nbNatsWithdraw: "+ nbNatsWithdraw);
				print("nbPl8Withdraw: "+ nbPl8Withdraw);
				print("MoneyDeposit: "+ MoneyDeposit);
			}
			else
			{
				if(getInventoryCount(40) == 0 || getInventoryCount(197) == 0)
				{
					//print("No more Natures or Fire Staff");
					stopScript();
				}
				//Si les quantite sont buguer on depose tout et on recommence
				nbPl8Withdraw = nbPl8Withdraw - getInventoryCount(steelPl8);
				deposit(steelPl8,getInventoryCount(steelPl8));//ore 1
				return 1000 + random(5,500);
			}			
		}
		if(isQuestMenu())
		{
			answer(0);
			return 1000 + random(300, 1200);
		}        
		if(getInventoryCount(steelPl8) == 0 || getInventoryCount(natureRunes) == 0)
		{
			int banker[] = getNpcByIdNotTalk(new int[]{95});										
			if (banker[0] != -1 && !isBanking())
			{						
				talkToNpc(banker[0]);						
			}
			return random(2000,3000);
		}
		else
		{
			castOnItem(28,getInventoryIndex(steelPl8));
			return random(450, 559);
		}
	}
}