//Version 5 Updated to OpenRSC
//5.1 Move to avoid log out
public class Abyte0_RuniteMiner extends Abyte0_Script {
	int oreRunite = 409; // Rune ore

	int RockRunite = 210; // Rune rock

	int gem1 = 160; // sapph
	int gem2 = 159; // emerald
	int gem3 = 158; // ruby
	int gem4 = 157; // diamond

	int fmode = 3;
	int foodId = 367;
	int nbFood = 6;

	int cptRuneBanked;
	int cptInventaireToBank = 2;

	boolean died = false;
	boolean readyToSleep = false;
	int cptReturns = 0;

	public Abyte0_RuniteMiner(Extension e) {
		super(e);
	}

	public void init(String params) {
		cptRuneBanked = 0;

		print("Abyte0 : Runite Miner...");
		print("Version 5.1 Updated to OpenRSC");

		print("Bank Ruby, Diamond, Runite ores + WALK BACK FROM LUMB");
		print(".");
		print(".");
		String[] str = params.split(",");

		if (str.length >= 3) {
			fmode = Integer.parseInt(str[0]);
			foodId = Integer.parseInt(str[1]);
			nbFood = Integer.parseInt(str[2]);
			if (str.length == 4) {
				cptRuneBanked = Integer.parseInt(str[3]);
			}
		} else {
			print("You should use: Abyte0_RuniteMiner Fmode,FoodId,nbFood (Optional: ,cptRunesBankedYet");
			print("You are Using Default: Defence Mode, 6 Tunas");
			print(".");
			print(".");
		}

		print("Fmode = " + fmode);
		print("foodId = " + foodId);
		print("nbFood = " + nbFood);
	}

	public int main() {
		return Mine();
	}

	public int Mine() {
		if (getFightMode() != fmode) {
			setFightMode(fmode);
		}
		if (inCombat()) {
			RunFromCombat();
			return random(100, 600);
		}
		if (getHpPercent() < 60) {
			//print("Low HP");
			if (IsStillHavingFood(foodId)) {
				EatFood(foodId);
				return random(500, 1500);
			}
		}
		if (readyToSleep) {
			if (getInventoryCount(1263) > 0) {
				//print("Im ready to sleep!");
				useSleepingBag();
				//print("Sleep");
			}
			readyToSleep = false;
			return 1000;
		}
		if (isBanking()) {
			//print("BANKING!");
			if (getFatigue() > 5) {
				withdraw(1263, 1);
				closeBank();
				readyToSleep = true;
				//print("wait 1 minute after withdraw");
				return 1000 + 60000;
			}
			if (getInventoryCount(1263) > 0) {
				deposit(1263, getInventoryCount(1263));
				//print("wait 1 minute after deposite");
				return 1000 + 60000;
			}
			// Deposit gems and ores
			if (getInventoryCount(gem4) > 0) {
				deposit(gem4, 1);
				return 1000;
			}
			if (getInventoryCount(gem3) > 0) {
				deposit(gem3, 1);
				return 1000;
			}
			if (getInventoryCount(gem2) > 0) {
				deposit(gem2, 1);
				return 1000;
			}
			if (getInventoryCount(gem1) > 0) {
				deposit(gem1, 1);
				return 1000;
			}
			if (getInventoryCount(oreRunite) > 0) {
				cptRuneBanked += getInventoryCount(oreRunite);
				deposit(oreRunite, getInventoryCount(oreRunite));
				print("Banked " + cptRuneBanked + " rune ore so far");
				return 1000 + 60000;
			}
			if (getInventoryCount(foodId) < nbFood) {
				withdraw(foodId, 1);
				return 1000;
			}
			closeBank();
			return 1000;
		}
		if (isQuestMenu()) {
			answer(0);
			return random(900, 1600);
		}
		if (getX() >= 280 && getX() <= 286 && getY() >= 564 && getY() <= 573 && (getInventoryCount(oreRunite) > 0 || !IsStillHavingFood(foodId))) {
			//Si dans la banque
			//print("Talking to Banker");
			if (!isBanking()) {
				int[] banker = getNpcByIdNotTalk(95);
				if (banker[0] != -1 && !isBanking()) {
					talkToNpc(banker[0]);
					return random(2000, 3000);
				}
			}
			return random(231, 1500);
		}
		if (getX() == 287 && getY() == 571 && (getInventoryCount(oreRunite) > 0 || !IsStillHavingFood(foodId))) {
			//Si a coter de la porte a exterieur
			//print("Step Inside Bank");
			atObject(287, 571);
			walkTo(286, 571);
			return random(100, 1500);
		}
		// Open door
		//if(distanceTo(getX(),getY(),287,571) < 5)
		//	if(getObjectById(64).length > 0)
		//		atObject(getObjectById(64)[1],getObjectById(64)[2]);
		// Talk to banker
		//if(distanceTo(286,571) < 6 && getInventoryCount() > 2)
		//{
		//	talkToNpc(getNpcById(banker)[0]);
		//	return 2002 + random(100, 100);
		//}
		if (distanceTo(120, 648) < 12) {
			died = true;
			print("Just Died...");
		}
		if (died) {
			print("Dead Action Method");
			if (getX() < 120) {
				//Walk Spawn
				walkTo(120, 648);
				return random(789, 1800);
			}
			if (getX() < 128) {
				//Walk shop lumb
				walkTo(128, 636);
				return random(789, 1800);
			}
			if (getX() < 136) {
				//Walk route apres sheep a voir
				walkTo(136, 616);
				return random(789, 1800);
			}
			if (getX() < 158) {
				//Walk Chickens Lumb
				walkTo(158, 613);
				return random(789, 1800);
			}
			if (getX() < 176) {
				//Walk Foin 1
				walkTo(176, 607);
				return random(789, 1800);
			}
			if (getX() < 196) {
				//Walk Foin 2
				walkTo(196, 605);
				return random(789, 1800);
			}
			if (getX() < 216) {
				//Walk
				walkTo(216, 605);
				return random(789, 1800);
			}
			if (getX() < 236) {
				//Walk
				walkTo(236, 608);
				return random(789, 1800);
			}
			if (getX() < 258) {
				//Walk
				walkTo(258, 609);
				return random(789, 1800);
			}
			if (getX() < 275) {
				//Walk Chiekens
				walkTo(275, 609);
				return random(789, 1800);
			}
			if (getX() < 282) {
				//Walk Road
				walkTo(282, 592);
				return random(789, 1800);
			}
			if (getX() < 290) {
				//Walk Guards
				walkTo(290, 571);
				died = false;
				print("not died anymore1");
				return random(789, 1800);
			}
			if (getX() == 290 && getY() == 571) {
				//Walk Guards
				died = false;
				print("not died anymore2");
				return random(789, 1800);
			}
			walkTo(290, 571);
		}
		if (getInventoryCount(oreRunite) >= cptInventaireToBank || !IsStillHavingFood(foodId)) {
			//print("We Should Bank!");
			if (getInventoryCount(gem1) > 0)//Drop Saphire
			{
				dropItem(getInventoryIndex(gem1));
				return random(1500, 3000);
			}
			if (getInventoryCount(gem2) > 0)//Drop Emeral
			{
				dropItem(getInventoryIndex(gem2));
				return random(1500, 3000);
			}
			//if(getX() >= 288 && getX() <= 291 && getY() >= 525 && getY() <= 528 && getFatigue() > 20)
			//{
			//	//Si dans la house et fatiguer de plsu de 20 on dors
			//	atObject(288,525);//rendu ici
			//	return random(231, 1500);
			//}
			//if(getX() >= 288 && getX() <= 291 && getY() >= 525 && getY() <= 528 && getFatigue() <= 20)
			//{
			//	//Si dans la house et pu fatiguer on sort esti
			//	atObject(287,571);
			//	walkTo(286,571);
			//	return random(100, 1500);
			//}

			if (getY() < 166) {
				//Walk sud rocks
				walkTo(269, 166);
				return random(789, 1800);
			}
			if (getY() < 177) {
				//Walk nort lesser
				walkTo(280, 177);
				return random(789, 1800);
			}
			if (getY() < 187) {
				//Walk west lesser
				walkTo(292, 187);
				return random(789, 1800);
			}
			if (getY() < 199) {
				//Walk ne altar
				walkTo(300, 199);
				return random(789, 1800);
			}
			if (getY() < 211) {
				//Walk east altar
				walkTo(307, 211);
				return random(789, 1800);
			}
			if (getY() < 222) {
				//Walk sud altar
				walkTo(321, 222);
				return random(789, 1800);
			}
			if (getY() < 239) {
				//Walk sud alta(32)
				walkTo(333, 239);
				return random(789, 1800);
			}
			if (getY() < 256) {
				//Walk 29
				walkTo(334, 256);
				return random(789, 1800);
			}
			if (getY() < 275) {
				//Walk ghost 26
				walkTo(335, 275);
				return random(789, 1800);
			}
			if (getY() < 297) {
				//Walk 22
				walkTo(335, 297);
				return random(789, 1800);
			}
			if (getY() < 319) {
				//Walk 19
				walkTo(334, 319);
				return random(789, 1800);
			}
			if (getY() < 333) {
				//Walk mush 16
				walkTo(333, 333);
				return random(789, 1800);
			}
			if (getY() < 352) {
				//Walk 13
				walkTo(335, 352);
				return random(789, 1800);
			}
			if (getY() < 371) {
				//Walk 10
				walkTo(335, 371);
				return random(789, 1800);
			}
			if (getY() < 389) {
				//Walk 7
				walkTo(335, 389);
				return random(789, 1800);
			}
			if (getY() < 408) {
				//Walk 4
				walkTo(334, 408);
				return random(789, 1800);
			}
			if (getY() < 419) {
				//Walk 2
				walkTo(320, 419);
				return random(789, 1800);
			}
			if (getY() < 436) {
				//Walk out wildy
				walkTo(306, 436);
				return random(789, 1800);
			}
			if (getY() < 460) {
				//Walk east goblin
				walkTo(305, 460);
				return random(789, 1800) + 60000;
			}
			if (getY() < 479) {
				//Walk
				walkTo(311, 479);
				return random(789, 1800);
			}
			if (getY() < 496) {
				//Walk FIXED
				walkTo(305, 496);
				return random(789, 1800);
			}
			if (getY() < 511) {
				//Walk cross road
				walkTo(312, 511);
				return random(789, 1800);
			}
			if (getY() < 526) {
				//Walk Devant maison OUT
				walkTo(292, 526);
				return random(789, 1800);
			}
			if (getY() < 541) {
				//Walk jardin
				walkTo(296, 541);
				return random(789, 1800);
			}
			if (getY() < 561) {
				//Walk sud jardin
				walkTo(291, 561);
				return random(789, 1800);
			}
			if (getY() < 571) {
				//Walk devant bank
				walkTo(287, 571);
				return random(789, 1800);
			}
			walkTo(287, 571);

			return random(1100, 1733);
		} else {
			//print("No Need to bank....");
			if (getX() == 286 && getY() == 571) {
				//Si a coter de la porte a linterieur
				//print("Step Outside Bank");
				atObject(287, 571);
				walkTo(287, 571);
				return random(121, 3500);
			}
			if (getX() >= 280 && getX() <= 286 && getY() >= 564 && getY() <= 573) {
				//Si dans la banque
				//print("Walking to Door");
				walkTo(286, 571);
				return random(240, 2500);
			}
			if (getY() > 561) {
				//Walk sud jardin
				walkTo(291, 561);
				return random(789, 1800);
			}
			if (getY() > 541) {
				//Walk jardin
				walkTo(296, 541);
				return random(789, 1800);
			}
			if (getY() > 526) {
				//Walk Devant maison OUT
				walkTo(292, 526);
				return random(789, 1800);
			}
			if (getY() > 511) {
				//Walk cross road
				walkTo(312, 511);
				return random(789, 1800);
			}
			if (getY() > 497) {
				//Walk
				walkTo(310, 497);
				return random(789, 1800);
			}
			if (getY() > 479) {
				//Walk
				walkTo(311, 479);
				return random(789, 1800);
			}
			if (getY() > 460) {
				//Walk east goblin
				walkTo(305, 460);
				return random(789, 1800);
			}
			if (getY() > 436) {
				//Walk out wildy
				walkTo(306, 436);
				return random(789, 1800);
			}
			if (getY() > 419) {
				//Walk 2
				walkTo(320, 419);
				return random(789, 1800);
			}
			if (getY() > 408) {
				//Walk 4
				walkTo(334, 408);
				return random(789, 1800);
			}
			if (getY() > 389) {
				//Walk 7
				walkTo(335, 389);
				return random(789, 1800);
			}
			if (getY() > 371) {
				//Walk 10
				walkTo(335, 371);
				return random(789, 1800);
			}
			if (getY() > 352) {
				//Walk 13
				walkTo(335, 352);
				return random(789, 1800);
			}
			if (getY() > 333) {
				//Walk mush 16
				walkTo(333, 333);
				return random(789, 1800);
			}
			if (getY() > 319) {
				//Walk 19
				walkTo(334, 319);
				return random(789, 1800);
			}
			if (getY() > 297) {
				//Walk 22
				walkTo(335, 297);
				return random(789, 1800);
			}
			if (getY() > 275) {
				//Walk ghost 26
				walkTo(335, 275);
				return random(789, 1800);
			}
			if (getY() > 256) {
				//Walk 29
				walkTo(334, 256);
				return random(789, 1800);
			}
			if (getY() > 239) {
				//Walk sud alta(32)
				walkTo(333, 239);
				return random(789, 1800);
			}
			if (getY() > 222) {
				//Walk sud altar
				walkTo(321, 222);
				return random(789, 1800);
			}
			if (getY() > 211) {
				//Walk east altar
				walkTo(307, 211);
				return random(789, 1800);
			}
			if (getY() > 199) {
				//Walk ne altar
				walkTo(300, 199);
				return random(789, 1800);
			}
			if (getY() > 187) {
				//Walk west lesser
				walkTo(292, 187);
				return random(789, 1800);
			}
			if (getY() > 177) {
				//Walk nort lesser
				walkTo(280, 177);
				return random(789, 1800);
			}
			if (getY() > 166) {
				//Walk sud rocks
				walkTo(269, 166);
				return random(789, 1800);
			}
			if (distanceTo(257, 157) < 25) {
				cptReturns++;
				int nombreRunite = mineOre(RockRunite);
				if (nombreRunite > 0)
					return random(1500, 3601);

				if (cptReturns > 50) {
					cptReturns = 0;
					walkTo(257, random(156, 158));
					return random(1000, 10000);
				}
			}
			//print("ELSE walkTo RockRunite");

			if (getX() != 257 || getY() != 157)
				walkTo(257, 157);

			return random(400, 1103);
		}
	}

	public final void print(String gameText) {
		System.out.println(gameText);
	}

	public void RunFromCombat() {
		int x = getX();
		int y = getY();
		walkTo(x, y);
	}

	public final int mineOre(int id) {
		int[] rock = getObjectById(id);
		if (rock[0] != -1) {
			//Si on est a la swamp mine
			atObject(rock[1], rock[2]);
			//print("Mining " + getObjectIdFromCoords(rock[1], rock[2]));
			return 1;
		}
		return 0;
	}
}
