import com.aposbot.Constants;

/**
 * Travels from Lumbridge to Catherby by boat. Intended for use by low level accounts.
 * Death is possible.
 * <p>
 * Requirements:
 * Start script logged-in at Lumbridge (or anywhere along the route).
 * <p>
 * Inventory (Optional):
 * 60 gold coins
 * <p>
 * Parameters (Optional, space delimited):
 * "thieve" - Enables thieving Men for coins.
 * "banana" - Enables picking Bananas for food.
 * "ardougne" - Enables stopping at Ardougne instead of Catherby.
 * <p>
 * Notes:
 * e.g. "banana thieve ardougne" will thieve for coins, pick 10 bananas, and stop script at Ardougne.
 * Any extra coins (< 10gp) and/or bananas will be dropped upon arrival at destination to clean-up inventory.
 * <p>
 *
 * @author Pomch
 */
public class AA_CatherbyWalker extends AA_Script {
	private static final String[] MENU_OPTIONS_CUSTOMS_OFFICIAL = new String[]{"Can I board this ship?",
		"Search away I have nothing to hide", "Ok"};

	private static final int[] NPC_IDS_SAILOR = new int[]{166, 170, 171};

	private static final int NPC_ID_CUSTOMS_OFFICIAL = 317;
	private static final int ITEM_ID_COINS = 10;
	private static final int ITEM_ID_BANANA = 249;
	private static final int OBJ_ID_BANANA_TREE = 183;
	private static final int BOAT_COST = 60;
	private static final int BANANA_COUNT = 10;
	private static final int NPC_ID_MAN = 11;
	private static final int RADIUS = 5;

	private static final Coordinate karamjaGate = new Coordinate(434, 682);
	private Coordinate deathCoord;
	private State state;

	private long timeout;
	private long startTime;

	private int deathCount;

	private boolean dead;
	private boolean thieveForCoins;
	private boolean pickBananas;
	private boolean stopAtArdougne;

	public AA_CatherbyWalker(Extension bot) {
		super(bot);
	}

	@Override
	public void init(String parameters) {
		if (!bot.isLoggedIn()) throw new IllegalStateException("Start script logged in.");
		thieveForCoins = parameters.contains("thi");
		pickBananas = parameters.contains("ban");
		stopAtArdougne = parameters.contains("ard");
		state = State.UNKNOWN;
		startTime = System.currentTimeMillis();
		System.out.println("Warning: You may DIE to a wizard, highway-man, scorpion, mugger, bear, or bat.");
		bot.displayMessage("@red@Warning: @or1@You may DIE to a wizard, highway-man, scorpion, mugger, bear, or bat.");
	}

	@Override
	public int main() {
		if (dead) {
			if (isDead()) return 0;
			dead = false;
		}

		for (final Area area : Area.AREAS) {
			if (area.contains(getPlayerX(), getPlayerY())) return handleArea(area);
		}

		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("board") || message.endsWith("legal") || message.endsWith("30 gold")) {
			timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void onDeath() {
		deathCoord = new Coordinate(getPlayerX(), getPlayerY());
		deathCount++;
		dead = true;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Catherby Walker", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@mag@Action: @whi@%s", state),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);


		drawString(String.format("@gre@Destination: @whi@%s", stopAtArdougne ? "Ardougne" : "Catherby"),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@cya@Thieve: @whi@%b", thieveForCoins),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@or1@Pick Bananas: @whi@%b", pickBananas),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@dre@Deaths: @whi@%s", deathCount),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);
	}

	private int handleArea(final Area area) {
		switch (area) {
			case LUMBRIDGE_05050:
				final int coinCount = getInventoryItemIdCount(ITEM_ID_COINS);
				if (coinCount < BOAT_COST) {
					if (!thieveForCoins) {
						return exit("Coins missing from inventory. Enter script parameter \"thieve\".");
					}
					if (state != State.THIEVING) state = State.THIEVING;
					return thieve();
				}
				break;
			case PORT_SARIM_05350:
				if (distanceTo(area.destination.getX(), area.destination.getY()) < RADIUS) {
					if (state != State.BOAT) state = State.BOAT;
					return takePortSarimBoat();
				}
				break;
			case KARAMJA_05551:
				if (pickBananas) {
					if (getInventoryItemIdCount(ITEM_ID_BANANA) < BANANA_COUNT) {
						if (state != State.BANANAS) state = State.BANANAS;
						return pickBananas();
					}
					if (getCurrentHits() < getBaseHits()) {
						useItem(getInventoryItemIndex(ITEM_ID_BANANA));
						return SLEEP_ONE_TICK;
					}
				}
				break;
			case BRIMHAVEN_05751:
				if (canEat()) return eatBanana();
				if (getPlayerX() <= karamjaGate.getX()) {
					if (state != State.GATE) state = State.GATE;

					if (isInCombat()) {
						walkTo(karamjaGate.getX(), karamjaGate.getY());
					} else {
						atObject(karamjaGate.getX(), karamjaGate.getY());
					}

					return SLEEP_ONE_TICK;
				}
				break;
			case BRIMHAVEN_05750:
				if (distanceTo(area.destination.getX(), area.destination.getY()) < RADIUS) {
					if (state != State.BOAT) state = State.BOAT;
					return takeBrimhavenBoat();
				}
				break;
			case ARDOUGNE_05949:
				if (stopAtArdougne) return stop();
				break;
			case CATHERBY_05747:
				if (canEat()) return eatBanana();
				if (getPlayerX() == area.destination.getX() && getPlayerY() == area.destination.getY()) return stop();
				break;
			case ARDOUGNE_05849:
			case KARAMJA_05651:
			case LEGENDS_GUILD_05848:
			case SEERS_05847:
				if (canEat()) return eatBanana();
				break;
			case LUMBRIDGE_05150:
			case DRAYNOR_05250:
			case PORT_SARIM_05349:
			case KARAMJA_05451:
				break;
		}

		if (state != State.WALKING) state = State.WALKING;

		walkTo(area.destination.getX(), area.destination.getY());
		return SLEEP_ONE_TICK;
	}

	private int thieve() {
		if (isInCombat()) {
			walkTo(getPlayerX(), getPlayerY());
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= timeout) return 0;

		if (deathCoord != null) {
			if (isItemAt(ITEM_ID_COINS, deathCoord.getX(), deathCoord.getY())) {
				if (getPlayerX() != deathCoord.getX() || getPlayerY() != deathCoord.getY()) {
					walkTo(deathCoord.getX(), deathCoord.getY());
				} else {
					pickupItem(ITEM_ID_COINS, deathCoord.getX(), deathCoord.getY());
				}
				return SLEEP_ONE_TICK;
			} else {
				deathCoord = null;
			}
		}

		final Object man = getNearestNpcNotInCombat(NPC_ID_MAN);
		if (man == null) return SLEEP_ONE_TICK;

		bot.walkDirectly(bot.getMobLocalX(man), bot.getMobLocalY(man), true);
		bot.createPacket(Constants.OP_NPC_ACTION);
		bot.put2(bot.getMobServerIndex(man));
		bot.finishPacket();

		timeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		return 0;
	}

	private int takePortSarimBoat() {
		if (isOptionMenuOpen()) {
			answer(getOptionMenuCount() == 3 ? 1 : 0);
			timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= timeout) return 0;

		final Object sailor = getNearestNpcNotTalking(NPC_IDS_SAILOR);

		if (sailor == null) return SLEEP_ONE_TICK;

		talkToNpc(sailor);
		timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	private int takeBrimhavenBoat() {
		if (isOptionMenuOpen()) {
			int index;

			for (final String menuOption : MENU_OPTIONS_CUSTOMS_OFFICIAL) {
				index = getMenuIndex(menuOption);
				if (index == -1) continue;
				answer(index);
				timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				return 0;
			}

			return SLEEP_ONE_SECOND;
		}

		if (System.currentTimeMillis() <= timeout) return 0;

		final Object customsOfficial = getNearestNpcNotTalking(NPC_ID_CUSTOMS_OFFICIAL);

		if (customsOfficial == null) return SLEEP_ONE_TICK;

		talkToNpc(customsOfficial);
		timeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	private int pickBananas() {
		final int[] bananaTree = getObjectById(OBJ_ID_BANANA_TREE);
		if (bananaTree[0] == -1) return exit("Script error: Could not find banana trees. Report this.");
		atObject(bananaTree[1], bananaTree[2]);
		return SLEEP_ONE_TICK;
	}

	private boolean canEat() {
		return pickBananas && !inCombat() && getCurrentHits() < getBaseHits();
	}

	private int eatBanana() {
		final int idx = getInventoryItemIndex(ITEM_ID_BANANA);

		if (idx == -1) {
			pickBananas = false;
			return 0;
		}

		useItem(idx);
		return SLEEP_ONE_TICK;
	}

	private int stop() {
		int idx = getInventoryItemIndex(ITEM_ID_COINS);
		if (idx != -1 && getInventoryItemCount(idx) < 10) {
			dropItem(idx);
			return SLEEP_ONE_TICK;
		}
		idx = getInventoryItemIndex(ITEM_ID_BANANA);
		if (idx != -1) {
			dropItem(idx);
			return SLEEP_ONE_TICK;
		}
		return exit("Destination reached.");
	}

	private enum State {
		THIEVING("Thieving for gold"),
		WALKING("Walking"),
		BOAT("Taking boat"),
		BANANAS("Picking bananas"),
		GATE("Opening gate"),
		UNKNOWN("Calculating ...");

		private final String desc;

		State(final String desc) {
			this.desc = desc;
		}


		@Override
		public String toString() {
			return desc;
		}
	}

	private enum Area implements RSArea {
		LUMBRIDGE_05050(new Coordinate(96, 624), new Coordinate(142, 671), new Coordinate(143, 634)),
		LUMBRIDGE_05150(new Coordinate(143, 624), new Coordinate(190, 671), new Coordinate(191, 624)),
		DRAYNOR_05250(new Coordinate(191, 624), new Coordinate(238, 671), new Coordinate(239, 624)),
		PORT_SARIM_05349(new Coordinate(239, 576), new Coordinate(287, 623), new Coordinate(258, 624)),
		PORT_SARIM_05350(new Coordinate(239, 624), new Coordinate(287, 671), new Coordinate(269, 650)),
		KARAMJA_05451(new Coordinate(323, 672), new Coordinate(334, 719), new Coordinate(335, 713)),
		KARAMJA_05551(new Coordinate(335, 672), new Coordinate(382, 719), new Coordinate(383, 698)),
		KARAMJA_05651(new Coordinate(383, 672), new Coordinate(430, 719), new Coordinate(431, 683)),
		BRIMHAVEN_05751(new Coordinate(431, 672), new Coordinate(479, 719), new Coordinate(435, 671)),
		BRIMHAVEN_05750(new Coordinate(432, 624), new Coordinate(479, 671), new Coordinate(467, 651)),
		ARDOUGNE_05949(new Coordinate(528, 576), new Coordinate(575, 623), new Coordinate(527, 615)),
		ARDOUGNE_05849(new Coordinate(480, 577), new Coordinate(527, 623), new Coordinate(490, 576)),
		LEGENDS_GUILD_05848(new Coordinate(480, 529), new Coordinate(527, 576), new Coordinate(487, 528)),
		SEERS_05847(new Coordinate(481, 480), new Coordinate(527, 528), new Coordinate(480, 500)),
		CATHERBY_05747(new Coordinate(432, 480), new Coordinate(480, 527), new Coordinate(440, 500));

		private static final Area[] AREAS = Area.values();

		private final Coordinate lowerBoundingCoordinate;
		private final Coordinate upperBoundingCoordinate;
		private final Coordinate destination;

		Area(final Coordinate lowerBoundingCoordinate, final Coordinate upperBoundingCoordinate,
			 final Coordinate destination) {
			this.lowerBoundingCoordinate = lowerBoundingCoordinate;
			this.upperBoundingCoordinate = upperBoundingCoordinate;
			this.destination = destination;
		}

		public Coordinate getLowerBoundingCoordinate() {
			return lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return upperBoundingCoordinate;
		}
	}
}
