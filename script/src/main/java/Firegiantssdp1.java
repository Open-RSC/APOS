public class Firegiantssdp1 extends Abyte0_Script {
	int fightMode = 1;
	int dmed = 0;
	int dsq = 0;
	int coin = 0;
	int rbaxe = 0;
	int rbar = 0;
	int blood = 0;
	int death = 0;

	int teeth = 0;
	int loop = 0;
	int law = 0;
	int fire = 0;
	int raxe = 0;
	int r2h = 0;

	int rscim = 0;
	int rdsq = 0;
	int rspear = 0;

	int trip = -1;
	boolean bankingnow;
	boolean walkingnow;
	boolean fightingnow;

	public Firegiantssdp1(Extension paramExtension) {
		super(paramExtension);
	}

	public void init(String paramString) {
		System.out.println("[=== !~ Thanks for purchasing this script    ~! ===]");
		System.out.println("[=== !~ Started Waterfall Giants By Renafox  ~! ===]");
		System.out.println("[=== !~ Version 1.14 |Revision 1 By Abyte0|  ~! ===]");
		if (paramString.equalsIgnoreCase("bank")) {
			System.out.println("[Rena] We're banking now");
			this.bankingnow = true;
			this.walkingnow = false;
			this.fightingnow = false;
		} else if (paramString.equalsIgnoreCase("walk")) {
			System.out.println("[Rena] We're walking now");
			this.bankingnow = false;
			this.walkingnow = true;
			this.fightingnow = false;
		} else if (paramString.equalsIgnoreCase("fight")) {
			System.out.println("[Rena] We're fighting now");
			this.bankingnow = false;
			this.walkingnow = false;
			this.fightingnow = true;
		}
	}

	public int main() {
		if (getFightMode() != this.fightMode) {
			setFightMode(this.fightMode);
		}

		if ((isBanking()) && (this.bankingnow) && (!this.walkingnow) && (!this.fightingnow)) {
			if (getInventoryCount(795) > 1) {
				this.dmed += getInventoryCount(795);
				deposit(795, getInventoryCount(795) - 1);

				return random(1000, 1500);
			}


			if (getInventoryCount(1277) > 1) {
				this.dsq += getInventoryCount(1277);
				deposit(1277, getInventoryCount(1277) - 1);

				return random(1000, 1500);
			}

			if (getInventoryCount(93) > 0) {
				this.rbaxe += getInventoryCount(93);
				deposit(93, getInventoryCount(93));

				return random(1000, 1500);
			}

			if (getInventoryCount(408) > 0) {
				this.rbar += getInventoryCount(408);
				deposit(408, getInventoryCount(408));

				return random(1000, 1500);
			}

			if (getInventoryCount(619) > 0) {
				this.blood += getInventoryCount(619);
				deposit(619, getInventoryCount(619));

				return random(1000, 1500);
			}

			if (getInventoryCount(38) > 0) {
				this.death += getInventoryCount(38);
				deposit(38, getInventoryCount(38));

				return random(1000, 1500);
			}

			if (getInventoryCount(10) > 0) {
				this.coin += getInventoryCount(10);
				deposit(10, getInventoryCount(10));

				return random(1000, 1500);
			}

			if (getInventoryCount(526) > 0) {
				this.teeth += getInventoryCount(526);
				deposit(526, getInventoryCount(526));

				return random(1000, 1500);
			}

			if (getInventoryCount(527) > 0) {
				this.loop += getInventoryCount(527);
				deposit(527, getInventoryCount(527));

				return random(1000, 1500);
			}

			if (getInventoryCount(31) > 0) {
				this.fire += getInventoryCount(31);
				deposit(31, getInventoryCount(31));

				return random(1000, 1500);
			}


			if (getInventoryCount(405) > 0) {
				this.raxe += getInventoryCount(405);
				deposit(405, getInventoryCount(405));

				return random(1000, 1500);
			}
			if (getInventoryCount(81) > 0) {
				this.r2h += getInventoryCount(81);
				deposit(81, getInventoryCount(81));

				return random(1000, 1500);
			}


			if (getInventoryCount(398) > 0) {
				this.rscim += getInventoryCount(398);
				deposit(398, getInventoryCount(398));

				return random(1000, 1500);
			}

			if (getInventoryCount(403) > 0) {
				this.rdsq += getInventoryCount(403);
				deposit(403, getInventoryCount(403));

				return random(1000, 1500);
			}

			if (getInventoryCount(1092) > 0) {
				this.rspear += getInventoryCount(1092);
				deposit(1092, getInventoryCount(1092));

				return random(1000, 1500);
			}

			if (getInventoryCount(42) > 1) {
				this.law += getInventoryCount(42);
				deposit(42, getInventoryCount(42) - 1);
				return random(1000, 1500);
			}
			if (getInventoryCount(42) < 1) {
				withdraw(42, 1);
				return random(1000, 1500);
			}
			if (getInventoryCount(33) > 5) {
				deposit(33, getInventoryCount(33) - 5);
				return random(1000, 1500);
			}
			if (getInventoryCount(33) < 5) {
				withdraw(33, 5 - getInventoryCount(33));
				return random(1000, 1500);
			}

			if (getInventoryCount(497) > 1) {
				deposit(497, getInventoryCount(497) - 1);
				return random(1000, 1500);
			}
			if (getInventoryCount(497) < 1) {
				withdraw(497, 1);
				return random(1000, 1500);
			}


			if (getInventoryCount(237) > 1) {
				deposit(237, getInventoryCount(237) - 1);
				return random(1000, 1500);
			}
			if (getInventoryCount(237) < 1) {
				withdraw(237, 1);
				wearItem(getInventoryIndex(782));
				return random(1000, 1500);
			}
			if (getInventoryCount(782) > 1) {
				deposit(782, getInventoryCount(782) - 1);
				return random(1000, 1500);
			}
			if (getInventoryCount(782) < 1) {
				withdraw(782, 17);
				return random(1000, 1500);
			}
			if (getInventoryCount(546) > 15) {
				deposit(546, getInventoryCount(546) - 15);
				return random(1000, 1500);
			}
			if (getInventoryCount(546) < 15) {
				withdraw(546, 15);
				return random(1000, 1500);
			}
			if (getInventoryCount(373) > 0) {
				deposit(373, getInventoryCount(373));
				return random(1000, 1500);
			}


			closeBank();
			wearItem(getInventoryIndex(782));
			this.bankingnow = false;
			this.walkingnow = true;
			this.fightingnow = false;
			this.trip += 1;

			System.out.println("====================================================");
			System.out.println("================ = Dragon Loot =  ==================");
			System.out.println("Obtained " + this.dmed + " dragon medium helmets so far");
			System.out.println("Obtained " + this.dsq + " dragon square shields <half> so far");
			System.out.println("----------------------------------------------------");
			System.out.println("===================== ~MISC~ =======================");
			System.out.println("Picking up of coins is currently DISABLED");
			System.out.println("Obtained " + this.rbaxe + " rune battleaxes so far");
			System.out.println("Obtained " + this.rbar + " runite bars so far");
			System.out.println("Obtained " + this.blood + " blood runes so far");
			System.out.println("Obtained " + this.death + " death runes so far");
			System.out.println("Obtained " + this.teeth + " half teeth keys so far");
			System.out.println("Obtained " + this.loop + " half loop keys so far");
			System.out.println("Obtained " + this.fire + " fire runes so far");
			System.out.println("Obtained " + this.law + " law runes so far");
			System.out.println("Obtained " + this.rscim + " rune scimitars so far");
			System.out.println("Obtained " + this.rdsq + " rune squares so far");
			System.out.println("Obtained " + this.rspear + " rune spears so far");
			System.out.println("Obtained " + this.raxe + " rune axes so far");
			System.out.println("Obtained " + this.r2h + " rune 2 handers so far");
			System.out.println("----------------------------------------------------");
			System.out.println("Made / banked " + this.trip + " trips so far");
			System.out.println("====================================================");
			useSleepingBag();
			return random(1000, 1500);
		}
		int[] arrayOfInt1;
		int[] arrayOfInt2;
		if ((this.bankingnow) && (!this.walkingnow) && (!this.fightingnow)) {
			if (isQuestMenu()) {
				answer(0);

				return random(5000, 6000);
			}

			arrayOfInt1 = getNpcByIdNotTalk(95);

			if (arrayOfInt1[0] != -1) {
				talkToNpc(arrayOfInt1[0]);

				return 5500;
			}

			if ((getX() >= 455) && (getX() < 467)) {
				walkTo(467, 462);
				System.out.println("[Rena] Leaving Castle!");
				useItem(getInventoryIndex(373));
				return random(1000, 1500);
			}
			if (getInventoryCount(413) > 0) {
				useItem(getInventoryIndex(413));
				return random(300, 400);
			}


			if ((getX() == 467) && (getY() == 462)) {
				arrayOfInt2 = getObjectById(57);

				if (arrayOfInt2[0] != -1) {
					atObject(arrayOfInt2[1], arrayOfInt2[2]);
					walkTo(469, 464);

					return random(2500, 2600);
				}

				walkTo(469, 464);
				return random(150, 200);
			}

			if ((getX() >= 468) && (getX() < 489)) {
				walkTo(489, 461);

				return random(1000, 1500);
			}

			if ((getX() >= 489) && (getX() < 501)) {
				walkTo(501, 454);

				return random(1000, 1500);
			}

			arrayOfInt2 = getObjectById(64);
			if (arrayOfInt2[0] != -1) {
				atObject(arrayOfInt2[1], arrayOfInt2[2]);
				return random(200, 300);
			}

			walkTo(501, 454);
			return random(1000, 1500);
		}

		if ((!this.bankingnow) && (this.walkingnow) && (!this.fightingnow)) {
			if (isAtApproxCoords(498, 447, 6)) {
				arrayOfInt1 = getObjectById(64);
				if (arrayOfInt1[0] != -1) {
					atObject(arrayOfInt1[1], arrayOfInt1[2]);
					System.out.println("[Rena] Open bank door");
					return random(200, 300);
				}

				walkTo(500, 454);
				System.out.println("[Rena] Walk out bank");
				return random(1000, 1500);
			}

			if ((getX() >= 500) && (getX() < 523)) {
				walkTo(523, 458);
				return random(1000, 1500);
			}

			if ((getX() >= 523) && (getX() < 540)) {
				walkTo(540, 473);
				return random(1000, 1500);
			}

			if ((getX() >= 540) && (getX() < 548)) {
				walkTo(548, 476);
				return random(1000, 1500);
			}

			if ((getX() >= 548) && (getX() < 572)) {
				walkTo(572, 476);
				return random(1000, 1500);
			}

			if ((getX() >= 572) && (getX() < 590)) {
				walkTo(590, 461);
				return random(1000, 1500);
			}

			if ((getX() >= 590) && (getX() < 592)) {
				walkTo(592, 458);
				return random(1000, 1500);
			}

			if ((isAtApproxCoords(592, 458, 2)) && (getX() <= 592)) {
				arrayOfInt1 = getObjectById(680);

				if (arrayOfInt1[0] != -1) {
					atObject(arrayOfInt1[1], arrayOfInt1[2]);

					return random(500, 600);
				}
				return random(500, 600);
			}

			if ((getX() >= 597) && (getX() < 608)) {
				walkTo(608, 465);
				return random(1000, 1500);
			}

			if ((getX() >= 608) && (getX() < 617)) {
				walkTo(617, 473);
				return random(1000, 1500);
			}

			if ((getX() == 617) && (getY() == 473)) {
				arrayOfInt1 = getObjectById(57);

				if (arrayOfInt1[0] != -1) {
					atObject(arrayOfInt1[1], arrayOfInt1[2]);
					walkTo(617, 474);

					return random(2500, 2600);
				}

				walkTo(617, 474);
				return random(150, 200);
			}

			if ((getX() >= 617) && (getX() < 637) && (getY() < 3000)) {
				walkTo(637, 463);
				return random(1000, 1500);
			}

			if ((getX() >= 637) && (getX() < 651) && (getY() < 3000)) {
				walkTo(651, 448);
				System.out.println("[Rena] Arriving at house");
				return random(1000, 1500);
			}

			if ((getX() >= 651) && (getX() < 654) && (getY() < 3000)) {
				walkTo(654, 451);
				System.out.println("[Rena] Sneaking to house corner");
				return random(1000, 1500);
			}
			if ((getX() >= 654) && (getX() < 658) && (getY() < 3000)) {
				walkTo(658, 451);
				System.out.println("[Rena] Passing house");
				return random(1000, 1500);
			}
			if ((getX() >= 658) && (getX() < 659) && (getY() < 3000)) {
				walkTo(659, 449);
				System.out.println("[Rena] Going raft");
				return random(600, 900);
			}

			if ((getX() == 659) && (getY() == 449)) {
				arrayOfInt1 = getObjectById(464);

				atObject(arrayOfInt1[1], arrayOfInt1[2]);
				System.out.println("[Rena] Boarding raft!");

				return random(2000, 2500);
			}

			if ((getX() == 662) && (getY() == 463)) {
				useItemOnObject(237, 462);
				System.out.println("[Rena] Swinging first tree!");

				return random(5000, 6500);
			}

			if ((getX() == 662) && (getY() == 467)) {
				useItemOnObject(237, 463);
				System.out.println("[Rena] Swinging second tree!");

				return random(5000, 6500);
			}

			if ((getX() == 659) && (getY() == 471)) {
				useItemOnObject(237, 482);
				System.out.println("[Rena] Swinging third tree!");

				return random(5000, 6500);
			}

			if ((getY() > 3302) && (getY() <= 3305)) {
				arrayOfInt1 = getObjectById(471);

				if (arrayOfInt1[0] != -1) {
					atObject(arrayOfInt1[1], arrayOfInt1[2]);
					System.out.println("[Rena] Opening first door!");

					return random(500, 600);
				}

			}

			if ((getY() > 3295) && (getY() <= 3302)) {
				walkTo(659, 3295);
				System.out.println("[Rena] Walk to second door...!");
				return random(150, 200);
			}

			if ((getX() == 659) && (getY() == 3295)) {
				arrayOfInt1 = getObjectById(64);

				if (arrayOfInt1[0] != -1) {
					atObject(arrayOfInt1[1], arrayOfInt1[2]);
					walkTo(659, 3294);
					System.out.println("[Rena] Opening second door!");

					return random(1500, 1600);
				}

				walkTo(659, 3294);
				System.out.println("[Rena] Second door already opened, entering...!");
				return random(150, 200);
			}

			if ((getY() > 3289) && (getY() <= 3294)) {
				walkTo(659, 3289);
				System.out.println("[Rena] Walk to third door...!");
				return random(300, 400);
			}
//fornt door outside
//walkTo(659,3289);

//front door inside the fire room
//walkTo(659,3288);
			if ((getX() == 659) && (getY() == 3289)) {
				arrayOfInt1 = getObjectById(64);

				if (arrayOfInt1[0] != -1) {
					atObject(arrayOfInt1[1], arrayOfInt1[2]);
					//walkTo(659, 3286);
					System.out.println("[Rena] Opening third door!");
					//wearItem(getInventoryIndex(new int[] { 522 }));

					return random(500, 600);
				}
				//walling
				//Door ID: 7
				//x,y: 654,3292
				//i = 7
				//Door ID: 9
				//x,y: 658,3284
				//i = 9
				//Door ID: 20
				//x,y: 661,3287
				//i = 20
				//Door ID: 22
				//x,y: 655,3291
				//i = 22
				//Door ID: 51
				//x,y: 659,3290
				//i = 51
				//Door ID: 64
				//x,y: 659,3289
				//i = 64

				walkTo(659, 3286);
				System.out.println("[Rena] Extracting dragon armor from giants...");
				wearItem(getInventoryIndex(522));

				return random(150, 200);
			}

			if ((getY() > 3250) && (getY() <= 3288)) {
				this.bankingnow = false;
				this.walkingnow = false;
				this.fightingnow = true;
				System.out.println("[Rena] KillingTime!");
				return random(300, 400);
			}
		}

		if ((!this.bankingnow) && (!this.walkingnow) && (this.fightingnow)) {
			if ((getX() == 456) && (getY() == 456)) {
				this.bankingnow = true;
				this.walkingnow = false;
				this.fightingnow = false;
			}

			if (getCurrentLevel(3) <= 75) {
				if (getInventoryCount(546) == 0) {
					System.out.println("[Rena] All out of sharks, teleporting to camelot to bank!");

					castOnSelf(22);

					return 100;
				}

				if (getCurrentLevel(3) <= 25) {


					castOnSelf(22);
					return 100;
				}

				if (getInventoryCount(373) > 0) {
					if (inCombat()) {
						walkTo(getX(), getY());
					}
					useItem(getInventoryIndex(373));


					return random(900, 1200);
				}

				if (getInventoryCount(546) > 0) {
					if (inCombat()) {
						walkTo(getX(), getY());
					}
					useItem(getInventoryIndex(546));


					return random(900, 1200);
				}

			}

			if (getInventoryCount(546) == 30) {
				if (inCombat()) {
					walkTo(getX(), getY());
				}
				useItem(getInventoryIndex(413));
				return random(300, 400);
			}
			if (getInventoryCount(413) > 0) {
				if (inCombat()) {
					walkTo(getX(), getY());
				}
				useItem(getInventoryIndex(413));
				return random(300, 400);
			}


			if (getInventoryCount(546) == 30) {
				if (inCombat()) {
					walkTo(getX(), getY());
				}
				useItem(getInventoryIndex(223));
				return random(300, 400);
			}
			if (getInventoryCount(223) > 0) {
				if (inCombat()) {
					walkTo(getX(), getY());
				}
				useItem(getInventoryIndex(223));
				return random(300, 400);
			}


			if (getInventoryCount(546) == 30) {
				if (inCombat()) {
					walkTo(getX(), getY());
				}
				useItem(getInventoryIndex(497));
				return random(300, 400);
			}
			if (getInventoryCount(497) > 0) {
				if (inCombat()) {
					walkTo(getX(), getY());
				}
				useItem(getInventoryIndex(497));
				return random(300, 400);
			}


			if (getInventoryCount(546) == 30) {
				if (inCombat()) {
					walkTo(getX(), getY());
				}
				useItem(getInventoryIndex(224));
				return random(300, 400);
			}
			if (getInventoryCount(224) > 0) {
				if (inCombat()) {
					walkTo(getX(), getY());
				}
				useItem(getInventoryIndex(224));
				return random(300, 400);
			}


			if (getInventoryCount(546) == 30) {
				if (inCombat()) {
					walkTo(getX(), getY());
				}
				useItem(getInventoryIndex(465));
				return random(300, 400);
			}
			if (getInventoryCount(465) > 0) {
				if (inCombat()) {
					walkTo(getX(), getY());
				}
				dropItem(getInventoryIndex(465));
				return random(300, 400);
			}


			arrayOfInt1 = getItemById(795);
			if (arrayOfInt1[0] != -1) {
				walkTo(arrayOfInt1[1], arrayOfInt1[2]);
				pickupItem(795, arrayOfInt1[1], arrayOfInt1[2]);
				return random(200, 300);
			}

			arrayOfInt2 = getItemById(1277);
			if (arrayOfInt2[0] != -1) {
				walkTo(arrayOfInt2[1], arrayOfInt2[2]);
				pickupItem(1277, arrayOfInt2[1], arrayOfInt2[2]);
				return random(200, 300);
			}

			int[] arrayOfInt3 = getItemById(93);
			if (arrayOfInt3[0] != -1) {
				walkTo(arrayOfInt3[1], arrayOfInt3[2]);
				pickupItem(93, arrayOfInt3[1], arrayOfInt3[2]);
				return random(200, 300);
			}

			int[] arrayOfInt4 = getItemById(408);
			if (arrayOfInt4[0] != -1) {
				walkTo(arrayOfInt4[1], arrayOfInt4[2]);
				pickupItem(408, arrayOfInt4[1], arrayOfInt4[2]);
				return random(200, 300);
			}

			int[] arrayOfInt5 = getItemById(619);
			if (arrayOfInt5[0] != -1) {
				walkTo(arrayOfInt5[1], arrayOfInt5[2]);
				pickupItem(619, arrayOfInt5[1], arrayOfInt5[2]);
				return random(200, 300);
			}

			int[] arrayOfInt6 = getItemById(38);
			if (arrayOfInt6[0] != -1) {
				walkTo(arrayOfInt6[1], arrayOfInt6[2]);
				pickupItem(38, arrayOfInt6[1], arrayOfInt6[2]);
				return random(200, 300);
			}

			int[] arrayOfInt7 = getItemById(526);
			if (arrayOfInt7[0] != -1) {
				walkTo(arrayOfInt7[1], arrayOfInt7[2]);
				pickupItem(526, arrayOfInt7[1], arrayOfInt7[2]);
				return random(200, 300);
			}

			int[] arrayOfInt8 = getItemById(527);
			if (arrayOfInt8[0] != -1) {
				walkTo(arrayOfInt8[1], arrayOfInt8[2]);
				pickupItem(527, arrayOfInt8[1], arrayOfInt8[2]);
				return random(200, 300);
			}

			int[] arrayOfInt9 = getItemById(413);
			if (arrayOfInt9[0] != -1) {
				walkTo(arrayOfInt9[1], arrayOfInt9[2]);
				pickupItem(413, arrayOfInt9[1], arrayOfInt9[2]);
				return random(200, 300);
			}

			int[] arrayOfInt10 = getItemById(42);
			if (arrayOfInt10[0] != -1) {
				walkTo(arrayOfInt10[1], arrayOfInt10[2]);
				pickupItem(42, arrayOfInt10[1], arrayOfInt10[2]);
				return random(200, 300);
			}

			int[] arrayOfInt11 = getItemById(31);
			if (arrayOfInt11[0] != -1) {
				walkTo(arrayOfInt11[1], arrayOfInt11[2]);
				pickupItem(31, arrayOfInt11[1], arrayOfInt11[2]);
				return random(200, 300);
			}

			int[] arrayOfInt12 = getItemById(373);
			if (arrayOfInt12[0] != -1) {
				walkTo(arrayOfInt12[1], arrayOfInt12[2]);
				pickupItem(373, arrayOfInt12[1], arrayOfInt12[2]);
				return random(200, 300);
			}

			int[] arrayOfInt13 = getItemById(398);
			if (arrayOfInt13[0] != -1) {
				walkTo(arrayOfInt13[1], arrayOfInt13[2]);
				pickupItem(398, arrayOfInt13[1], arrayOfInt13[2]);
				return random(200, 300);
			}

			int[] arrayOfInt14 = getItemById(403);
			if (arrayOfInt14[0] != -1) {
				walkTo(arrayOfInt14[1], arrayOfInt14[2]);
				pickupItem(403, arrayOfInt14[1], arrayOfInt14[2]);
				return random(200, 300);
			}

			int[] arrayOfInt15 = getItemById(1092);
			if (arrayOfInt15[0] != -1) {
				walkTo(arrayOfInt15[1], arrayOfInt15[2]);
				pickupItem(1092, arrayOfInt15[1], arrayOfInt15[2]);
				pickupItem(1092, arrayOfInt15[1], arrayOfInt15[2]);
				return random(200, 300);
			}
			int[] arrayOfInt16 = getItemById(405);
			if (arrayOfInt16[0] != -1) {
				walkTo(arrayOfInt16[1], arrayOfInt15[2]);
				pickupItem(405, arrayOfInt16[1], arrayOfInt16[2]);
				pickupItem(405, arrayOfInt16[1], arrayOfInt16[2]);
				return random(200, 300);
			}

			int[] arrayOfInt18 = getItemById(223);
			if (arrayOfInt18[0] != -1) {
				walkTo(arrayOfInt18[1], arrayOfInt18[2]);
				pickupItem(223, arrayOfInt18[1], arrayOfInt18[2]);
				return random(200, 300);
			}


			int[] arrayOfInt19 = getItemById(81);
			if (arrayOfInt19[0] != -1) {
				walkTo(arrayOfInt19[1], arrayOfInt19[2]);
				pickupItem(81, arrayOfInt19[1], arrayOfInt19[2]);
				return random(200, 300);
			}


			if (getFatigue() > 55) {
				useSleepingBag();

				return 1000;
			}

			int[] arrayOfInt17 = getNpcInRadius(344, 656, 3282, 8);

			if ((arrayOfInt17[0] != -1) && (!inCombat())) {
				attackNpc(arrayOfInt17[0]);

				return random(900, 1100);
			}

			if (getCurrentLevel(3) <= 75) {
				if (getInventoryCount(546) == 0) {
					System.out.println("[Rena] All out of sharks, teleporting to camelot to bank!");

					castOnSelf(22);

					return 100;
				}

				if (getCurrentLevel(3) <= 25) {


					castOnSelf(22);
					return 100;
				}
			}
			if (!isAtApproxCoords(656, 3282, 9)) {
				walkTo(660, 3285);
				System.out.println("[Rena] Went outside firegiants zone ... Walking back!");
				return random(900, 1100);
			}

		}

		return random(400, 500);
	}
}
