//Abyte0
//2012-03-06

/*
abyte0_potion a = Attack Potion
abyte0_potion d = Defence Potion
abyte0_potion s = Strength Potion
abyte0_potion sa = Super Attack Potion
abyte0_potion sd = Super Defence Potion
abyte0_potion ss = Super Strength Potion
abyte0_potion p = Prayer Potion
abyte0_potion f = Fishing Potion
abyte0_potion sr = Stats Restore
*/

public class Abyte0_Potion extends Script 
{    
	int att1 = 474;
	int att2 = 475;
	int att3 = 476;
		
	int def1 = 480;
	int def2 = 481;
	int def3 = 482;
	
	int str1 = 222;
	int str2 = 223;
	int str3 = 224;

	int superAtt1 = 486;
	int superAtt2 = 487;
	int superAtt3 = 488;
		
	int superDef1 = 495;
	int superDef2 = 496;
	int superDef3 = 497;
	
	int superStr1 = 492;
	int superStr2 = 493;
	int superStr3 = 494;
	
	int pray1 = 483;
	int pray2 = 484;
	int pray3 = 485;
	
	int fish1 = 489;
	int fish2 = 490;
	int fish3 = 491;
	
	int statRest1 = 477;
	int statRest2 = 478;
	int statRest3 = 479;
	
	int vial = 465;
	
	int itemToDeposit = vial;
	int itemToWithdraw1;
	int itemToWithdraw2;
	int itemToWithdraw3;
	
	int fmode = 3;
	
	public Abyte0_Potion(Extension e) {super(e);}    
	
	public void init(String param)
	{
		print("==================================");
		print("Made by: Abyte0");
		print("Paid and Share by: I R BEST");
		print("==================================");
		print("Any Bank Potion Drinker By Abyte0");
		print("Commands : a d s sa sd ss p sr f");
		print("Version 0");
		print("==================================");
		print("Paid and Share by: I R BEST");
		print(".");
		print(".");
		print(".");
			
		itemToWithdraw1 = pray1;
		itemToWithdraw2 = pray2;
		itemToWithdraw3 = pray3;
			
		if(param.equals("p"))
		{
			print("Doing Prayer");
		}
		else if(param.equals("a"))
		{
			print("Doing Attack");
			itemToWithdraw1 = att1;
			itemToWithdraw2 = att2;
			itemToWithdraw3 = att3;
		}
		else if(param.equals("d"))
		{
			print("Doing Defence");
			itemToWithdraw1 = def1;
			itemToWithdraw2 = def2;
			itemToWithdraw3 = def3;
		}
		else if(param.equals("s"))
		{
			print("Doing Strength");
			itemToWithdraw1 = str1;
			itemToWithdraw2 = str2;
			itemToWithdraw3 = str3;
		}
		else if(param.equals("sa"))
		{
			print("Doing Super Attack");
			itemToWithdraw1 = superAtt1;
			itemToWithdraw2 = superAtt2;
			itemToWithdraw3 = superAtt3;
		}
		else if(param.equals("sd"))
		{
			print("Doing Super Defence");
			itemToWithdraw1 = superDef1;
			itemToWithdraw2 = superDef2;
			itemToWithdraw3 = superDef3;
		}
		else if(param.equals("ss"))
		{
			print("Doing Super Strength");
			itemToWithdraw1 = superStr1;
			itemToWithdraw2 = superStr2;
			itemToWithdraw3 = superStr3;
		}
		else if(param.equals("f"))
		{
			print("Doing Fishing");
			itemToWithdraw1 = fish1;
			itemToWithdraw2 = fish2;
			itemToWithdraw3 = fish3;
		}
		else if(param.equals("sr"))
		{
			print("Doing Stats Restore");
			itemToWithdraw1 = statRest1;
			itemToWithdraw2 = statRest2;
			itemToWithdraw3 = statRest3;
		}
		else
		{
			print("Default = Prayer Potion");
		}
	}    
	
	public int main() 
	{        
		if(getFightMode() != fmode) 
		{            
			setFightMode(fmode);            
			return 500;        
		}        
		if(getFatigue() > 85) 
		{
			useSleepingBag();            
			return random(800,1000);        
		}        
		if(isBanking())
		{
			if(getInventoryCount(itemToDeposit) > 0) 
			{
				deposit(itemToDeposit,getInventoryCount(itemToDeposit));
				return random(1000,1500);            
			}
			if(getInventoryCount() < 30) 
			{                
				withdraw(itemToWithdraw1,30-getInventoryCount());                
				return random(1000,1500);            
			}
			closeBank();  
			return random(1000,1500);
		}        
		if(isQuestMenu()) 
		{            
			answer(0);                
			return random(1000,1500);             
		}

		if(getInventoryCount(itemToWithdraw3) > 0)
		{
			drink(itemToWithdraw3);
			return random(500,1000);
		}
		if(getInventoryCount(itemToWithdraw2) > 0)
		{
			drink(itemToWithdraw2);
			return random(500,1000);
		}
		if(getInventoryCount(itemToWithdraw1) > 0)
		{
			drink(itemToWithdraw1);
			return random(500,1000);
		}
		else
		{
			int banker[] = getNpcByIdNotTalk(new int[]{95});
			if (banker[0] != -1 && !isBanking())
				talkToNpc(banker[0]);
			else
				print("No banker!"); 
			return random(240, 2500);
		}
	}
	
	private void print(String pString)
	{
		System.out.println(pString);
	}
	
	private void drink(int pId)
	{
		print("Drinking ID " + pId);
		useItem(getInventoryIndex(pId));
	}
}