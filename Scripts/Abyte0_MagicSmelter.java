public class Abyte0_MagicSmelter extends Script
{
//Made by Abyte0
//Public Release: 2011-12-22
//Used to smelt bars using magic....

//Updates
/* =============================================== */
//Updated : 2012-01-23 - Abyte0 - Added Iron - 0r5
//Updated : 2012-01-24 - Abyte0 - Fixed banking - 1
//Updated : 2012-12-10 - Abyte0 - Added Silver and Gold - 1.1
//Updated : 2012-12-18 - Abyte0 - Fixed Addy Amount - 1.2
/* =============================================== */

//Thanks
/* =============================================== */
/*  Thanks "Tarooki" ---> gold and silver id                      */
/* =============================================== */

    int[] mix;
    
	//Ores
    int oreIron = 151;
    int oreMith = 153;
    int oreAddy = 150;
    int oreRune = 409;
    int oreCoal = 155;
    int oreSilver = 383;
    int oreGold = 152;
    
	//Bars
	int barIron = 170;
	int barSteel = 171;
	int barMith = 173;
	int barAddy = 174;
	int barRune = 408;
    int barSilver = 384;
    int barGold = 172;

    boolean oneItemSmelting = false;
    
    int banker = 95;
    
    String barType;
    
    int[] doorObj;

    public Abyte0_MagicSmelter(Extension e)
    {
        super(e);
    }    
    public void init( String params )
    {
        print("Abyte0 : AnyBank Magic Smelter...");
        print("Version 1.2");
        
        oneItemSmelting = false;
    
        if(params.equals("silver"))
        {
            mix = new int[]{barSilver,0,oreSilver,27,oreCoal,0};
            oneItemSmelting = true;
        }
        else if(params.equals("gold"))
        {
            mix = new int[]{barGold,0,oreGold,27,oreCoal,0};
            oneItemSmelting = true;
        }
        else if(params.equals("i"))
        {
            mix = new int[]{barIron,0,oreIron,27,oreCoal,0};
            oneItemSmelting = true;
        }
        else if(params.equals("s"))
            mix = new int[]{barSteel,9,oreIron,9,oreCoal,18};
        else if(params.equals("m"))
            mix = new int[]{barMith,5,oreMith,5,oreCoal,20};
        else if(params.equals("a"))
            mix = new int[]{barAddy,3,oreAddy,3,oreCoal,18};
        else if(params.equals("r"))
            mix = new int[]{barRune,3,oreRune,3,oreCoal,24};
        else
        {
            print("LowerCase Letter Param ARE: i = iron, s = steel, m = mith, a = addy, r = runite");
			print("LowerCase Letter Param ARE: silver = silver, gold = gold");
            print("exemple for rune type: abyte0_magicsmelter r");
            stopScript();
        }
        
        barType = params;
        
    }    
    public int main()
    {
        return Mine();
    }    
    public int Mine()
    {
        if(getFightMode() != 2)
        {
            setFightMode(2);
        }        
        if(getFatigue() > 70)
        {
            useSleepingBag();
            return 1000;
        }
        if(isBanking())
        {      
            if(getInventoryCount(40) <= 30)
            {            
                //on withdraw les nats
                withdraw(40,100);
                return 1000 + random(10,500);
            }
            if(getInventoryCount(197) == 0)
            {            
                //on withdraw le fire staff
                withdraw(197,1);
                return 1000 + random(10,500);
            }
            if(oneItemSmelting)
            {            
                if(getInventoryCount(mix[0]) > 0)
                {            
                    deposit(mix[0],getInventoryCount(mix[0]));
                    return 1000 + random(10,500);
                }
                else if(getInventoryCount(mix[2]) == 0)
                {                
                    withdraw(mix[2],mix[3]);
                    return 1000 + random(10,500);
                }                
                else if(getInventoryCount(mix[2]) == mix[3] && getInventoryCount(mix[0]) == 0)
                    closeBank();
                else
                {
                    if(getInventoryCount(40) == 0 || getInventoryCount(197) == 0)
                    {
                        print("No more Natures or Fire Staff");
                        stopScript();
                    }
                    //Si les quantit� sont buguer on depose tout et on recommence
                    deposit(mix[2],getInventoryCount(mix[2]));//ore 1
                    deposit(mix[4],getInventoryCount(mix[4]));//ore 2
                    deposit(mix[0],getInventoryCount(mix[0]));//bars
                    return 1000 + random(5,500);
                }
            }
            else
            {
                if(getInventoryCount(mix[0]) > 0)
                {            
                    deposit(mix[0],getInventoryCount(mix[0]));
                    return 1000 + random(10,500);
                }
                else if(getInventoryCount(mix[2]) == 0)
                {                
                    withdraw(mix[2],mix[3]);
                    return 1000 + random(10,500);
                }
                else if(getInventoryCount(mix[4]) == 0)
                {              
                    withdraw(mix[4],mix[5]);
                    //withdraw(oreCoal,8);//18 only take out 10 so i try 10 then 8 after...
                    return 1000 + random(10,500);
                }                    
                else if(getInventoryCount(mix[2]) == mix[3] && getInventoryCount(mix[4]) == mix[5] && getInventoryCount(mix[0]) == 0)
                    closeBank();
                else
                {
                    if(getInventoryCount(40) == 0 || getInventoryCount(197) == 0)
                    {
                        print("No more Natures or Fire Staff");
                        stopScript();
                    }
                    //Si les quantit� sont buguer on depose tout et on recommence
                    deposit(mix[2],getInventoryCount(mix[2]));//ore 1
                    deposit(mix[4],getInventoryCount(mix[4]));//ore 2
                    deposit(mix[0],getInventoryCount(mix[0]));//bars
                    return 1000 + random(5,500);
                }
            }
        }
        if(isQuestMenu())
        {
            answer(0);
            return 1000 + random(300, 1200);
        }
        if(oneItemSmelting)
        {
            if(getInventoryCount(mix[2]) == 0 || getInventoryCount(40) < 30)
            {
                return talkBanker();
            }
            else
            {
                castOnItem(21,getInventoryIndex(mix[2]));
                return random(1352, 1659);
            }
        }
        else
        {
            if(getInventoryCount(mix[2]) == 0 || getInventoryCount(mix[4]) <= 1 || getInventoryCount(40) < 30)
            {
                return talkBanker();
            }
            else
            {
                castOnItem(21,getInventoryIndex(mix[2]));
                return random(1352, 1659);
            }
        }
    }
    
    public final void print(String gameText)
    {
        System.out.println(gameText);
    }

    public final int talkBanker()
    {
        int banker[] = getNpcByIdNotTalk(new int[]{95});                                        
        if (banker[0] != -1 && !isBanking())
        {                        
            //print("Hello you Banker!");
            talkToNpc(banker[0]);                        
            return random(2000,3000);
        }
        else
        {
            print("No banker!");
            stopScript();
            return 1;
        }
    }
}