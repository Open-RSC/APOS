public class Abyte0_DropParty extends Abyte0_Script
{
	private final String SCRIPT_VERSION = "0.6";
	
	@Override
	public String[] getRandomQuotes()
	{
		String[] result = {
			"Any drop yet?",
			"who picked the drag med?",
			"Nice hat!",
			"lucky",
			"10m dropped",
			"dropping 10k nats soon",
			"drop a phat!",
			"Can you drop a drag med?",
			"Please drop a "};
		return result;
	}

	int coins = 10;
	int FireRune = 31;
	int WaterRune = 32;
	int AirRune = 33;
	int EarthRune = 34;
	int MindRune = 35;
	int deathRunes = 38;
	int nature = 40;
	int law = 42;
	int bloodRune = 619;
	int Disk = 387;
	int Pumpkin = 422;
	int crystalKey = 525;
	int halfKey1 = 526;
	int halfKey2 = 527;
	int ChristmasCracker = 575;
	int PartyHat0 = 576; 
	int PartyHat1 = 577;
	int PartyHat2 = 578;
	int PartyHat3 = 579;
	int PartyHat4 = 580;
	int PartyHat5 = 581;
	int sharkCert = 630;
	int rawSharkCert = 631;
	int easterEgg = 677;
	int DragonMediumHelmet = 795;
	int halloweenMask0 = 828;
	int halloweenMask1 = 831;
	int halloweenMask2 = 832;
	int Santa = 971;
	int DragonBoneCertificate = 1270;
	int LympwurtCertificate = 1271;
	int PrayPotCertificate = 1272;
	int SupAttPotCertificate = 1273;
	int SupDefPotCertificate = 1274;
	int SupStrPotCertificate = 1274;
	int HalfDragonSquareShield1 = 1276;
	int HalfDragonSquareShield2 = 1277;
	int DragonSquareShield = 1278;
	int r2h = 81;
	int dragLong = 593;
	int dragBattle = 594;
	int dragAmmyUncharged = 522;
	int dragAmmy = 597;
	int dragAmmyUnenchanted = 610;
	
	int[] items = {easterEgg,ChristmasCracker,PartyHat0,PartyHat1,PartyHat2,PartyHat3,PartyHat4,PartyHat5,
	Disk,Pumpkin,halloweenMask0,halloweenMask1,halloweenMask2,sharkCert,rawSharkCert,
	DragonBoneCertificate,LympwurtCertificate,PrayPotCertificate,SupAttPotCertificate,SupDefPotCertificate,SupStrPotCertificate,
	coins,Santa,FireRune,WaterRune,AirRune,bloodRune,DragonMediumHelmet,HalfDragonSquareShield1,HalfDragonSquareShield2,DragonSquareShield,
	EarthRune,MindRune,deathRunes,nature,law,crystalKey,halfKey1,halfKey2,r2h,dragLong,dragBattle,
	dragAmmyUncharged,dragAmmy,dragAmmyUnenchanted};
	
	public Abyte0_DropParty(Extension e) {		super(e);	}
	
	public void init(String params)
	{
		print("Version " + SCRIPT_VERSION);
	}
	
	public int main()
	{
		if(needToMove)
		{
			changePosition();
			return 500;
		}
		
		for(int h = 0; h < items.length; h++)
		{
			int[] groundItems = getItemById(items[h]);
			if(groundItems[0] != -1)
			{
				pickupItem(groundItems[0], groundItems[1], groundItems[2]);
				return 800;
			}
		}
		
		return 100;
	}
	
	
    @Override
    public void onServerMessage(String s) {

        if (s.contains("standing here for 5 mins!")) {
			needToMoveFromX = getX();
			needToMoveFromY = getY();
			needToMove = true;
        }
		
    }

	private void changePosition()
	{
		int pid = getSelfPid();
		if(pid > 50)
		{
			print("bad pid, we can relog");
			needToMove = false;
			return;
		}
			
		if(getX() != needToMoveFromX || getY() != needToMoveFromY)
			needToMove = false;
		else
			walkTo(needToMoveFromX + random(0,2) - 1, needToMoveFromY + random(0,2) - 1);
	}
	
	int needToMoveFromX = -1;
	int needToMoveFromY = -1;
	boolean needToMove = false;
}