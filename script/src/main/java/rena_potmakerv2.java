public class rena_potmakerv2 extends Abyte0_Script {
	int item; //Herb
	int item2; //unf pot
	int dropIDs; //2nd ing
	int dropIDs2; //end product

	public rena_potmakerv2(Extension paramExtension) {
		super(paramExtension);
	}

	public void init(String params) {
		System.out.println(params);
		params = params.trim();
		if (params.equalsIgnoreCase("Attack Potion")) {
			this.dropIDs2 = 474;
			this.dropIDs = 270;
			this.item = 444;
			this.item2 = 454;
		} else if (params.equalsIgnoreCase("Strength Potion")) {
			this.dropIDs2 = 222;
			this.dropIDs = 220;
			this.item = 446;
			this.item2 = 456;
		} else if (params.equalsIgnoreCase("Stat Restore")) {
			this.dropIDs2 = 477;
			this.dropIDs = 219;
			this.item = 447;
			this.item2 = 457;
		} else if (params.equalsIgnoreCase("Defense Potion")) {
			this.dropIDs2 = 480;
			this.dropIDs = 471;
			this.item = 448;
			this.item2 = 458;
		} else if (params.equalsIgnoreCase("Restore Prayer")) {
			this.dropIDs2 = 483;
			this.dropIDs = 469;
			this.item = 448;
			this.item2 = 458;
		} else if (params.equalsIgnoreCase("Super Attack")) {
			this.dropIDs2 = 486;
			this.dropIDs = 270;
			this.item = 449;
			this.item2 = 459;
		} else if (params.equalsIgnoreCase("Fishing Potion")) {
			this.dropIDs2 = 489;
			this.dropIDs = 469;
			this.item = 450;
			this.item2 = 460;
		} else if (params.equalsIgnoreCase("Super Strength")) {
			this.dropIDs2 = 492;
			this.dropIDs = 220;
			this.item = 451;
			this.item2 = 461;
		} else if (params.equalsIgnoreCase("Weapon Poison")) {
			this.dropIDs2 = 572;
			this.dropIDs = 467;
			this.item = 451;
			this.item2 = 461;
		} else if (params.equalsIgnoreCase("Super Defense")) {
			this.dropIDs2 = 495;
			this.dropIDs = 471;
			this.item = 452;
			this.item2 = 462;
		} else if (params.equalsIgnoreCase("Ranging Potion")) {
			this.dropIDs2 = 498;
			this.dropIDs = 501;
			this.item = 453;
			this.item2 = 463;
		} else
			print("Error:  Params must contain potion name.. Ex: Attack Potion,Strength Potion,Super Defense,Super Attack");
		//loadIRCBot();
	}

	public String getScriptName() {
		return this.getClass().getName();
	}

	public int main() {
		if (getFatigue() > 90) {
			useSleepingBag();
			return 1000;
		}
		if (isQuestMenu()) {
			answer(0);
			return random(2500, 3000);
		}
		if (isBanking()) {
			if (getInventoryCount(this.dropIDs2) > 0) {
				deposit(this.dropIDs2, getInventoryCount(this.dropIDs2));
				return random(1500, 2000);
			}

			if (getInventoryCount(this.item) == 9 && getInventoryCount(464) == 9 && getInventoryCount(this.dropIDs) == 9) {
				closeBank();
				return 500;
			}
			if (getInventoryCount(464) < 9) {
				withdraw(464, 9 - getInventoryCount(464));
				return random(1450, 1500);
			}
			if (getInventoryCount(464) > 9) {
				deposit(464, getInventoryCount(464) - 9);
				return random(1500, 2000);
			}
			if (getInventoryCount(this.item) < 9) {
				withdraw(this.item, 9 - getInventoryCount(this.item));
				return random(1450, 1500);
			}
			if (getInventoryCount(this.item) > 9) {
				deposit(this.item, getInventoryCount(this.item) - 9);
				return random(1500, 2000);
			}


			if (getInventoryCount(this.dropIDs) < 9) {
				withdraw(this.dropIDs, 9 - getInventoryCount(this.dropIDs));
				return random(1450, 1500);
			}
		}


		if (getInventoryCount(this.item) > 0) {
			useItemWithItem(getInventoryIndex(this.item), getInventoryIndex(464));
			return random(250, 300);
		}
		if (getInventoryCount(this.item2) > 0) {
			useItemWithItem(getInventoryIndex(this.dropIDs), getInventoryIndex(this.item2));
			return random(250, 300);
		}
		int[] arrayOfInt = getNpcByIdNotTalk(BANKERS);
		if (arrayOfInt[0] != -1) {
			talkToNpc(arrayOfInt[0]);
			return random(3900, 4200);
		}
		return 1000;
	}
}
