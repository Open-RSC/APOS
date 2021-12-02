public class Abyte0_Nature extends Abyte0_Script {
	int fmode = 3;
	int cpt = 0;

	public Abyte0_Nature(Extension e) {
		super(e);
	}

	@Override
	public String[] getRandomQuotes() {
		String[] result = {"Helllo", "Where that dam key!", "Lets steal that chest!", "I hope nobody catch me stealing that chest!", "I'm a pirate!"};
		return result;
	}

	public void init(String params) {
		String[] str = params.split(",");

		if (str.length == 1) {
			fmode = Integer.parseInt(str[0]);
		}

		print("Started Abyte0 Nat Thiever");
		print("Version 3");
		//Walk to avoid log out (work with the 2 houses)
		//fix 1.1 Lower click time
		//Version 3 Updated for OpenRSC
		if (isAtApproxCoords(539, 1545, 15))
			print("Cake Nat");
		else
			print("Line Nat");


		print("Fmode = " + fmode);

	}

	public int main() {
		SayRandomQuote();
		if (getFightMode() != fmode) {
			setFightMode(fmode);
			return random(300, 2500);
		}

		if (getFatigue() > 75) {
			useSleepingBag();
			return random(921, 1000);
		}
		if (cpt > random(50, 100)) {
			//print("Walk");
			if (isAtApproxCoords(539, 1545, 15))
				walkTo(539, 1545);
			else
				walkTo(582, 1525);
			cpt = 0;
			return 2000;
		}

		int[] natsChest = getObjectById(335);
		if (natsChest[0] != -1) {
			atObject2(natsChest[1], natsChest[2]);
		}


		int[] lottedChest = getObjectById(340); //might be lucky
		if (lottedChest[0] != -1) {
			if ((lottedChest[1] == 582 && lottedChest[2] == 1527) || (lottedChest[1] == 539 && lottedChest[2] == 1547))
				atObject2(lottedChest[1], lottedChest[2]);
		}

		cpt++;
		return random(random(126, 567), 642);
	}
}

