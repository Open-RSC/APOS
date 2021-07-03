/**
*  Catherby Lobster Fishing *
 *  v1.0 please report bugs if any *
 *   XcendroX` */
//Edited By Abyte0
public class Abyte0_CatherbyFisher extends Abyte0_Script
{
	int sleepAt = 80;
	int FishType = 372;
	int FishType2 = 369;
	int cptTry = 0;
	public Abyte0_CatherbyFisher(Extension e)
	{      super(e);   }
	public void init(String params)
	{
		print("Started Abyte0 Catherby Fisher");
		print("Version 1.1");
		//Version 0 by XcendroX
		//Version 1 by Abyte0
		//2  Ways doors opener
		//Lobs OR Tuna/Swordy
		print("No param = Lobs , Anything as param = Tunas Swordy...");
		if(params.equals(""))
		{
			FishType = 372;
			print("Doing Lobs!");
		}
		else
		{
			FishType = 366;
			print("Doing Tunas and Swordy!");
		}
	}
	public int main()
	{
		if(getFatigue() > 80)
		{
			useSleepingBag();
			return 1000;
		}
		if(getInventoryCount() == 30)
		{
			if(isQuestMenu())
			{
				answer(0);
				return random(410, 987);
			}
			if(isBanking())
			{
				if(getInventoryCount(FishType) > 0 || getInventoryCount(FishType2) > 0)
				{
					if(getInventoryCount(FishType) > 0)
						deposit(FishType, getInventoryCount(FishType));
					if(getInventoryCount(FishType2) > 0)
						deposit(FishType2, getInventoryCount(FishType2));
				}	
				else
					closeBank();
				return random(523, 603);
			}
			if(getX() <	412)
			{
				walkTo(412, 501);
				return random(430, 1502);
			}
			if(getX() <	437)
			{
				walkTo(439, 497);
				return random(430, 1502);
			}
			if(getX() >	443)
			{
				walkTo(439, 497);
				return random(430, 1502);
			}
			if(getY() <	491)
			{
				walkTo(439, 497);
				return random(4030, 8502);
			}
			if(getX() == 439 && getY() == 497)
			{
				//System.out.println("Open + Step InSide Bank");
				atObject(439, 497);
				walkTo(439, 496);
				//On remet el compteur a 0;
				cptTry = 0;
				return random(100, 1500);
			}
			if(getY() == 497)
			{
				//si on est perdu sur le coter...
				walkTo(439, 497);
				//On remet el compteur a 0;
				cptTry = 0;
				return random(100, 1500);
			}
			int banker[] = getNpcByIdNotTalk(BANKERS);
			if(banker[0] != -1)
			{
				talkToNpc(banker[0]);
				return 1000+random(423, 501);
			}
			return random(400, 500);
		}
		else
		{
			if(getX() == 439 && getY() == 496)
			{
				//Si on ets a la porte on louvre et sort
				print("Open + Step OutSide Bank");
				atObject(439, 497);
				walkTo(439, 498);
				return random(100, 1500);
			}
			if(getY() < 497 && getX() > 436)
			{
				//Si on est dans la banque on va a coter de la porte
				walkTo(439, 496);
				print("Walk to Door");
				return 1000;
			}
			if(getX() == 439 && getY() == 497)
			{
				print(".wait.");
				//NOTHING waitting to be at 439, 498
			}
			if(getX() != 409 && getY() != 503)
			{
				print("Walk to Fish");
				//Si on est pas rndu au fishing on Marche
				walkTo(409,503);
				return 1000;
			}
			if(cptTry++ >= random(80,130))
			{
				//Si on a beaucoup dessaie on bouge pas loguer out
				walkTo(409,502);
				print("Moving because " + cptTry + " trys...");
				cptTry = 0;
				return random(1003,4221);
			}
			if(isAtApproxCoords(409,503, 10) && getInventoryCount() != 30)
			{
				int[] fish = getObjectById(new int[]{194});
				if( fish[0] != -1 )
				{
					if(FishType == 372)
						atObject2(fish[1], fish[2]);
					else
						atObject(fish[1], fish[2]);
					cptTry++;
					return random(403,1721);
				}
			}
		}
	return random(400, 500);
	}
}