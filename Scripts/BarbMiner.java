public class Barbminer extends Script
{
    int fightMode = 2;
    boolean iron = false;
    public Barbminer (Extension e)
    {
        super(e);
    }

    public void init(String params)
    {
        params = params.trim().toLowerCase();
        if(params.equals("iron"))
            iron = true;
    }
 


    public int main()
    {
        if(getFightMode() != fightMode)
            setFightMode(fightMode);


        if(getFatigue() > 95)
        {
            useSleepingBag();
			return 1000;
        } 

        if(isBanking())
        {
            if(getInventoryCount(160) > 0)
            {            
                deposit(160, getInventoryCount(160));
                return random(200,800);
            }
 
            if(getInventoryCount(159) > 0)
            {
                deposit(159, getInventoryCount(159));
                return random(100,600);
            }
 
            if(getInventoryCount(158) > 0)
            {
                deposit(158, getInventoryCount(158));
                return random(150,1600);
            }
 
            if(getInventoryCount(157) > 0)
            {
                deposit(157, getInventoryCount(157));
                return random(100,600);
            }
 
            if(getInventoryCount(150) > 0)
            {
                deposit(150, getInventoryCount(150));
                return random(150,800);
            }
 
            if(getInventoryCount(151) > 0)
            {
                deposit(151, getInventoryCount(151));
                return random(150,600);
            }
 
            if(getInventoryCount(202) > 0)
 
            {
                //tin
                deposit(202, getInventoryCount(202));
                return random(150,600);
 
            }
            if(getInventoryCount(155) > 0)
            {
                deposit(155, getInventoryCount(155));
                return random(150,600);
            }
 
            closeBank();
            return random(500, 600);
 
        }
 
        if(getInventoryCount() == 30)
        {
            // menu open
            if(isQuestMenu())
            {
                answer(0);
                return random(500, 600);
            }
            int banker[] = getNpcByIdNotTalk(BANKERS);
 
            if(banker[0] != -1)
            {
                talkToNpc(banker[0]);
                return 1000;
            }

            if(getX() < 503) //gomine
            {
                walkTo(266, 505);
                return 1000;
            }
            return random(400, 500);
        }

        if(getX() > 454) //gobank
        {
            walkTo(216, 450);
            return 1000;
        } 

        int[] rock = getObjectById(102);
		if(rock.length > 0 && distanceTo(318,641,rock[1],rock[2]) < 5) 
		{                    
			atObject(rock[1],rock[2]);
			cptTry++;
		}
        return random(400, 500);
    }
} 
