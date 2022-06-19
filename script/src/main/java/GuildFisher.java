public class GuildFisher extends Abyte0_Script {
//public void print(String t) {System.out.println(t);}

	//Edited by Abyte0
	boolean walk1;
	boolean walk2;
	boolean certDone;
	int answer2;
	int checkLoop = 1;
	int npcId;
	int random;
	int sleepAt = 79;
	int[] fishId;
	int[] fishCoords;

	public GuildFisher(Extension e) {
		super(e);
	}


	public void init(String params) {
		System.out.println("Guild Fisher by mofo");
		if (params.equalsIgnoreCase("shark")) {
			System.out.println("Set to fish shark..");
			answer2 = 2;
			fishId = new int[]{545, 546, 547};
			fishCoords = new int[]{261, 585, 498};
			sleepAt = 79;
			npcId = 370;
		} else if (params.equalsIgnoreCase("lobster")) {
			System.out.println("Set to fish lobster..");
			answer2 = 0;
			fishId = new int[]{372, 373, 374};
			fishCoords = new int[]{376, 589, 501, 588, 500};
			sleepAt = 85;
			npcId = 369;
		} else {
			System.out.println("Not a valid option. Please use lobster or shark!");
			stopScript();
		}
	}

	public int main() {
		if (getFatigue() > sleepAt) {
			useSleepingBag();
			return 1000;
		}
		if (isQuestMenu() && getInventoryCount(fishId[1]) > 4) {
			certDone = true;
			if (questMenuCount() == 3) {
				answer(1);
				return random(1400, 1800);
			}
			if (questMenuCount() == 4) {
				answer(answer2);
				return random(1400, 1800);
			}
			if (questMenuCount() == 5) {
				if (getInventoryCount(fishId[1]) > 24)
					answer(4);
				if (getInventoryCount(fishId[1]) > 19 && getInventoryCount(fishId[1]) < 25)
					answer(3);
				if (getInventoryCount(fishId[1]) > 14 && getInventoryCount(fishId[1]) < 20)
					answer(2);
				if (getInventoryCount(fishId[1]) > 9 && getInventoryCount(fishId[1]) < 15)
					answer(1);
				if (getInventoryCount(fishId[1]) > 4 && getInventoryCount(fishId[1]) < 10)
					answer(0);
				random = random(1, 2);
				if (npcId == 369 && random > 1)
					fishCoords = new int[]{376, 589, 501};
				else if (npcId == 369 && random < 2)
					fishCoords = new int[]{376, 588, 500};
				return random(1000, 1500);
			}
			return 1000;
		}
		if (!isAtApproxCoords(587, 498, 5) && getInventoryCount(fishId[1]) < 5 && getInventoryCount() < 25) {
			int[] doors = getWallObjectById(2);
			if (walk2 == true && doors[1] == 603) {
				System.out.println("Someone shut us in while certing! Opening..");
				atWallObject(doors[1], doors[2]);
				return random(1500, 2000);
			}
			walkTo(587, 498 + random(-2, 2));
			return random(2500, 3000);
		}
		if (isAtApproxCoords(587, 498, 5) && getInventoryCount() != 30) {
			certDone = false;
			walk2 = false;
			checkLoop = 0;
			int[] fish = getObjectById(fishCoords[0]);
			if (fish[0] != -1 && npcId == 370) {
				atObject2(fishCoords[1], fishCoords[2]);
			} else {
				atObject(fishCoords[1], fishCoords[2]);
			}
			return random(700, 1100);
		}
		if (getInventoryCount(fishId[0]) > 0 && getInventoryCount() == 30 && !isAtApproxCoords(586, 521, 2)) {
			if (isAtApproxCoords(586, 511, 4))
				walk1 = true;
			if (!isAtApproxCoords(586, 511, 4) && walk1 == false) {
				walkTo(586, 511 + random(-2, 2));
				return random(1000, 1500);
			}
			int[] doors = getWallObjectById(2);
			if (walk1 == true && doors[1] == 586) {
				System.out.println("Someone shut the range door! Opening..");
				atWallObject(doors[1], doors[2]);
				return random(1500, 2000);
			}
			if (!isAtApproxCoords(586, 521, 2) && getInventoryCount() == 30) {
				walkTo(586 + random(-2, 2), 521 + random(-2, 2));
				return random(2000, 3000);
			}
			return 1000;
		}
		if (isAtApproxCoords(586, 521, 5) && getInventoryCount(fishId[0]) != 0) {
			int[] range = getObjectById(11);
			if (range[0] != -1) {
				if (getInventoryCount(fishId[0]) != 0) {
					useItemOnObject(fishId[0], 11);
					return random(700, 1100);
				}
				return 1000;
			}
			return 1000;
		}
		if (isAtApproxCoords(586, 521, 5) && getInventoryCount(fishId[2]) != 0) {
			if (getInventoryIndex(fishId[2]) != -1) {
				dropItem(getInventoryIndex(fishId[2]));
				return random(1000, 1200);
			}
			return 500;
		}
		if (!isAtApproxCoords(604, 503, 2) && getInventoryCount(fishId[1]) > 4) {
			int[] doors = getWallObjectById(2);
			if (walk1 == true && doors[1] == 586) {
				System.out.println("Someone shut us in while cooking! Opening..");
				atWallObject(doors[1], doors[2]);
				return random(1500, 2000);
			}
			if (isAtApproxCoords(597, 512, 5)) {
				walk1 = false;
				walk2 = true;
			}
			if (!isAtApproxCoords(597, 512, 5) && walk2 == false) {
				walkTo(597, 512 + random(-2, 2));
				return random(1500, 2000);
			}
			if (walk2 == true && doors[1] == 603) {
				System.out.println("Someone shut the certer door! Opening..");
				atWallObject(doors[1], doors[2]);
				return random(1500, 2000);
			}
			walkTo(604 + random(-1, 1), 503 + random(-2, 2));
			return random(1500, 2000);
		}
		if (isAtApproxCoords(604, 503, 2) && getInventoryCount(fishId[1]) > 4 && !isQuestMenu() && certDone == false) {
			int[] npc = getNpcById(npcId);
			if (npc[0] != -1) {
				talkToNpc(npc[0]);
				return random(1700, 2200);
			}
			return 1000;
		}
		checkLoop++;
		if (checkLoop > 5)
			certDone = false;
		return 1000;
	}
}
