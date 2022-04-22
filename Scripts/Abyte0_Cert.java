public class Abyte0_Cert extends Script
{

	int[] type = null;
	int[] itemtype = null;

	int[] fish = {370, 369, 373, 372, 535, 536, 533, 534};
	int[] bars = {170, 171, 173, 172, 384, 528, 529, 530, 532, 531};
	int[] ore = {151, 155, 153, 152, 383, 517, 518, 519, 521, 520};
	int certer;
	int item;
	int loop;
	int answer;
	int answer2;
	boolean answered = false;
	boolean needBank = false;

	public Abyte0_Cert(Extension e)
	{
		super(e);
	}

	public void init(String params)
	{
		print("Draynor Cert by mofo");
		print("Edited by Abyte0");
		print("Version 0.2");
		//Version 0 by mofo
		//Version 1 by Abyte0
		//Withdraw certs when none left
		
		if(getInventoryCount(fish) != 0)
		{
			System.out.println("Doing fish");
			type = new int[] {369, 370, 373, 372, 535, 536, 533, 534};
			System.out.println(type[0]);
			certer = 227;
		}
		else if(getInventoryCount(bars) != 0)
		{
			System.out.println("Doing bars");
			type = new int[] {170, 171, 173, 172, 384, 528, 529, 530, 532, 531};
			certer = 226;
		}
		else if(getInventoryCount(ore) != 0)
		{
			System.out.println("Doing ore");
			type = new int[] {151, 155, 153, 152, 383, 517, 518, 519, 521, 520};
			certer = 225;
		}
		else
		{
			print("You did it wrong. Start with some type of item or certificate in your inventory.");
			stopScript();
		}
		for(int i = 0; i < type.length; i++)
		{
			if(getInventoryCount(type[i]) > 0)
			{
				item = type[i];
				answer = 1;
				answer2 = i;
				if(type[i] > 500)
				{
					answer = 0;
					answer2 = (i - (type.length / 2));
				}
				if(item > 369 && item < 374)
				{
					if(item > 370)
						answer2 = i - 2;
					else
						answer2 = i + 2;
				}
				break;
			}
		}
	}

	public int main()
	{
	    if (isBanking())
	    {
	        needBank = false;
	        if (item > 500)
	        {
			//si cest des certs quon veut defaire
				if (getInventoryCount(item) < 5)
				{
					//Si il ne reste plus asser de certs on en reprend 200 si possible sinon le max
					//if(bankCount(item) >= 5)
					//{
						withdraw(item, 100);
						print("Retire 100");
						return 2000;
					//}
					//else
					//{
					//	//Si on n'a pas de certs dans la banque
					//	stopScript();
					//	return 1;
					//}
	            }
				if (getInventoryCount(item) < 5)
				{
					stopScript();
				}
	            if (item > 532 && item < 537)
	            {
	                if (getInventoryCount(type[answer2]) > 0)
	                {
	                    deposit(type[answer2], getInventoryCount(type[answer2]));
	                    return random(1000, 1500);
	                }
	            }
	            if (getInventoryCount(type[answer2]) > 0)
	            {
	                deposit(type[answer2], getInventoryCount(type[answer2]));
	                return random(1000, 1500);
	            }
	            closeBank();
	            return 1000;
	        }
	        if (getInventoryCount() < 6)
	        {
			//sinon si on est rendu ici cest que on veut certer des items
				if (getInventoryCount(517) >= 105)
				{
					//si on cert et quon a plus de 105 iron certs
					deposit(517, getInventoryCount(517));
					print("Deposit "+ getInventoryCount(517)+" Iron certs");
					return 2000;
	            }
				if (getInventoryCount(518) >= 105)
				{
					//si on cert et quon a plus de 105 coals certs
					deposit(518, getInventoryCount(518));
					print("Deposit "+ getInventoryCount(518)+" Coals certs");
					return 2000;
	            }
	            if (item > 368 && item < 374)
	            {
	                if (item > 370)
						withdraw(type[answer2 + 2], 25);
					else
						withdraw(type[answer2 - 2], 25);
	                return random(1000, 1500);
	            }
	            withdraw(type[answer2], 25);
	            return random(1000, 1500);
	        }
	        closeBank();
	    }
	    if (isAtApproxCoords(219, 637, 3) && needBank)
	    {
	        if (isQuestMenu())
	        {
	            answer(0);
	            return 6000;
	        }
	        int banker[] = getNpcByIdNotTalk(new int[]
	        {
	            95
	        });
	        if (banker[0] != -1)
	        {
	            talkToNpc(banker[0]);
	            return random(2000, 2700);
	        }
	    }
	    if (isAtApproxCoords(230, 630, 7) && !needBank)
	    {
	        if (isQuestMenu())
	        {
	            if (questMenuCount() == 3)
	            {
	                answer(answer);
	                return random(1400, 1800);
	            }
	            if (questMenuCount() == 4)
	            {
	                answer(answer2);
	                return random(1400, 1800);
	            }
	            if (questMenuCount() == 5)
	            {
	                if (type.length == 10)
	                {
	                    if (!answered)
	                    {
	                        answer(answer2);
	                        answered = true;
	                        return random(1400, 1800);
	                    }
	                }
	                if (item > 500)
	                {
	                    if (getInventoryCount(item) > 4) answer(4);
	                    else answer(getInventoryCount(item) - 1);
	                    needBank = true;
	                    answered = false;
	                    return random(1400, 1800);
	                }
	                if (getInventoryCount(item) > 24) answer(4);
	                else answer((getInventoryCount(item) / 5) - 1);
	                needBank = true;
	                answered = false;
	                return random(1400, 1800);
	            }
	        }
	        int[] npc = getNpcById(certer);
	        if (npc[0] != -1) talkToNpc(npc[0]);
	        return random(2000, 2700);
	    }
	    if (needBank)
	    {
	        int[] bankdoors = getObjectById(64);
	        if (bankdoors[0] != -1)
	        {
	            atObject(bankdoors[1], bankdoors[2]);
	            return random(1300, 2000);
	        }
	        walkTo(219 + random(-1, 1), 637 + random(-1, 1));
	        return random(1300, 2000);
	    }
	    if (!needBank)
	    {
	        int[] bankdoors = getObjectById(64);
	        if (bankdoors[0] != -1)
	        {
	            atObject(bankdoors[1], bankdoors[2]);
				//print("DOOR position: "+bankdoors[1]+","+ bankdoors[2]);
	            return random(1300, 2000);
	        }
	        walkTo(230 + random(-3, 3), 630 + random(-3, 3));
	        return random(1300, 2000);
	    }
	    if (loop > 5)
	    {
	        answered = false;
	        needBank = true;
	    }
	    loop++;
	    return 1000;
	}
	
	public final void print(String gameText)
	{
		System.out.println(gameText);
	}
}
