/** * This script is used to cook your food in catherby. * 
* start at catherby bank, change the rawID and cookedID and burntID 
* varables too cook whatever you want. * * 
* v 1.5 * 
*   - yomama` edited by XcendroX
* Edited by Abyte0
 */
//Version 2.0 Updated to OpenRSC
public class Abyte0_CatherbyCooker extends Abyte0_Script 
{   
	int rawID = 366;   
	//enter raw fishies ID   
	int cookedID = 367;
	//enter cooked fishies ID  
	int burntID = 368; 
	//enter burnt fishies ID  
	
	int[] Tunas = new int[]{366,367,368};  
	//366: Raw Tuna
	//367: Tuna
	//368: Burnt fish
	
	int[] Swordfishs = new int[]{369,370,371};  
	//369: Raw Swordfish
	//370: Swordfish
	//371: Burnt Swordfish
	
	int[] Lobsters = new int[]{372,373,374};  
	//372: Raw Lobster
	//373: Lobster
	//374: Burnt Lobster
	 
	int[] cookArea = new int[]{435, 485};   
	int[] bankArea = new int[]{439, 495}; 
	     
	public Abyte0_CatherbyCooker(Extension e) {super(e);} 
	     
	public void init(String params)
	{
		print("Default Tunas, L = Lobs, S = Swordys");
		if(params.equals("L"))
		{
			rawID = Lobsters[0];
			cookedID = Lobsters[1];
			burntID = Lobsters[2];
			print("Doing Lobsters");
		}
		else if(params.equals("S"))
		{
			rawID = Swordfishs[0];
			cookedID = Swordfishs[1];
			burntID = Swordfishs[2];
			print("Doing Swordfishs");
		}
		else
			print("Doing Default Tunas");

		print("Version 2.0 Udpated to OpenRSC");
	}
	   
	public int main() 
	{      
		if(getFatigue() > 80) 
		{         
			useSleepingBag();         
			return 1000;      
		}            
		if(isBanking()) 
		{         
			if(getInventoryCount(cookedID) != 0) 
			{            
				deposit(cookedID, getInventoryCount(cookedID));            
				return random(800, 1000);               
			}         
			if(getInventoryCount() == 30) 
			{            
				closeBank();            
				return random(500, 600);         
			}                  
			withdraw(rawID, 30 - getInventoryCount());         
			return random(600, 800);      
		}            
		if(isQuestMenu()) 
		{         
			answer(0);         
			return random(1000, 1500);      
		}            
		if(getInventoryCount(rawID) != 0) 
		{         
			if(distanceTo(cookArea[0], cookArea[1]) < 10) 
			{             
				useItemOnObject(rawID, 11);             
				return random(1500, 2000);          
			}          
			walkTo(cookArea[0], cookArea[1]);          
			return random(11000, 13000);      
		}          
		//if we burnt fishies, they will be dropped            
		if(getInventoryCount(burntID) > 0) 
		{               
			dropItem(getLastInventoryIndex(burntID));               
			return random(1500, 1600);            
		}                          
		if(distanceTo(bankArea[0], bankArea[1]) < 10) 
		{         
			int banker[] = getNpcByIdNotTalk(BANKERS);           
			if(banker[0] != -1)               
				talkToNpc(banker[0]);           
			return 2500;      
		}
		else
		{        
			walkTo(bankArea[0], bankArea[1]);
			if(getFatigue() > 10) 
			{         
				useSleepingBag();
				return 1000;      
			} 
		}			
		return random(800, 1500);   
	}
	
	public final void print(String gameText)
	{
		System.out.println(gameText);
	}
}