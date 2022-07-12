/**
 * Fish, cook, and cert at the Fishing Guild.
 * <p>
 * Required:
 * Start script at the fishing guild with sleeping bag, fishing tool, and cooking gauntlets (optional).
 * <p>
 * Required Parameter:
 * -f,--fish <lobster|shark>
 * <p>
 * Optional Parameters:
 * -c,--cook
 * -r,--receiver <rsn>
 * <p>
 * Notes:
 * A player matching the name of the receiver parameter can say "Trade" in chat to have certs traded to him/her.
 * <p>
 *
 * @Author Chomp
 */
public class AA_FishingGuild extends AA_Script {
	private static final int MAXIMUM_FATIGUE = 99;
	private static final int REQUIRED_FISH_COUNT_TO_CERT = 25;

	private Fish fish;
	private State state;
	private long startTime;
	private String receiver = "";

	private double initialFishingXp;
	private double initialCookingXp;

	private long clickTimeout;
	private long tradeRequestTimeout;
	private long optionMenuTimeout;

	private int playerX;
	private int playerY;

	private int fishCerted;
	private int fishCaught;
	private int fishCooked;
	private int fishBurnt;

	private boolean idle;
	private boolean trading;
	private boolean cooking;

	public AA_FishingGuild(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) printInstructions();

		final String[] args = parameters.split(" ");

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
				case "-f":
				case "--fish":
					fish = Fish.valueOf(args[++i].toUpperCase());
					break;
				case "-c":
				case "--cook":
					cooking = true;
					break;
				case "-r":
				case "--receiver":
					receiver = args[++i];
					break;
				default:
					throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
			}
		}

		if (fish == null) {
			throw new IllegalStateException("Missing fish parameter.");
		}

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!hasInventoryItem(fish.toolId)) {
			throw new IllegalStateException(String.format("%s missing from inventory.", getItemNameId(fish.toolId)));
		}

		state = State.FISH;
		initialFishingXp = getAccurateXpForLevel(Skill.FISHING.getIndex());
		initialCookingXp = getAccurateXpForLevel(Skill.COOKING.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		if (trading) {
			return trade();
		}

		switch (state) {
			case FISH:
				return fish();
			case COOK:
				return cook();
			case CERT:
				return cert();
			default:
				return exit("Invalid script state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("fail", 4)) {
			clickTimeout = 0;
		} else if (message.startsWith("catch", 4)) {
			fishCaught++;
			clickTimeout = 0;
		} else if (message.startsWith("burn", 17)) {
			fishBurnt++;
			clickTimeout = 0;
		} else if (message.endsWith("cooked")) {
			fishCooked++;
			clickTimeout = 0;
		} else if (message.endsWith("certificates")) {
			fishCerted += REQUIRED_FISH_COUNT_TO_CERT;
		} else if (message.endsWith("area")) {
			idle = true;
		} else if (message.startsWith("Trade") || message.endsWith("trade")) {
			clickTimeout = 0L;
			optionMenuTimeout = 0L;
			tradeRequestTimeout = 0L;
			trading = false;
		} else {
			super.onServerMessage(message);
		}
	}

	private int trade() {
		final int[] player = getPlayerByName(receiver);

		if (player[0] == -1) {
			trading = false;
			return 0;
		}

		final int inventoryIndex = getInventoryIndex(fish.certIds);

		if (inventoryIndex == -1) {
			trading = false;
			return 0;
		}

		if (isInTradeConfirm()) {
			if (bot.hasLocalConfirmedTrade()) {
				return 0;
			}

			confirmTrade();
			return SLEEP_ONE_TICK;
		}

		if (isInTradeOffer()) {
			if (getLocalTradeItemCount() == 0) {
				if (System.currentTimeMillis() <= clickTimeout) {
					return 0;
				}

				offerItemTrade(inventoryIndex, getInventoryStack(inventoryIndex));
				clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
				return 0;
			}

			if (bot.hasLocalAcceptedTrade()) {
				return 0;
			}

			acceptTrade();
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(player[1], player[2]) > 1) {
			walkTo(player[1], player[2]);
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= tradeRequestTimeout) {
			return 0;
		}

		sendTradeRequest(getPlayerPID(player[0]));
		tradeRequestTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
		return 0;
	}

	private int fish() {
		if (Area.DOCKS.contains(playerX, playerY)) {
			if (idle) {
				if (playerY == fish.coordinate.getY() + 2) {
					idle = false;
					return 0;
				}

				walkTo(fish.coordinate.getX() - 1, fish.coordinate.getY() + 2);
				return SLEEP_ONE_TICK;
			}

			if (getInventoryCount() == MAX_INV_SIZE) {
				state = cooking ? State.COOK : State.CERT;
				return 0;
			}

			if (getFatigue() >= MAXIMUM_FATIGUE) {
				return sleep();
			}

			if (System.currentTimeMillis() <= clickTimeout) {
				return 0;
			}

			final Coordinate fishCoordinate = fish.coordinate;

			if (playerX != fishCoordinate.getX() - 1 ||
				playerY != fishCoordinate.getY() + 1) {
				walkTo(fishCoordinate.getX() - 1, fishCoordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			useObject2(fishCoordinate.getX(), fishCoordinate.getY());
			clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (Area.RANGE_HOUSE.contains(playerX, playerY) &&
			getWallObjectIdFromCoords(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY()) == Object.RANGE_DOOR.id) {
			atWallObject(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (Area.CERT_HOUSE.contains(playerX, playerY) &&
			getWallObjectIdFromCoords(Object.CERT_DOOR.coordinate.getX(), Object.CERT_DOOR.coordinate.getY()) == Object.CERT_DOOR.id) {
			atWallObject(Object.CERT_DOOR.coordinate.getX(), Object.CERT_DOOR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(fish.coordinate.getX() - 1, fish.coordinate.getY() + 1);

		if (getFatigue() != 0 && isWalking()) {
			return sleep();
		}

		return SLEEP_ONE_TICK;
	}

	private int cook() {
		if (Area.RANGE_HOUSE.contains(playerX, playerY)) {
			final int burntIndex = getInventoryIndex(fish.burntId);

			if (burntIndex != -1) {
				dropItem(burntIndex);
				return SLEEP_ONE_TICK;
			}

			if (getFatigue() >= MAXIMUM_FATIGUE) {
				return sleep();
			}

			if (System.currentTimeMillis() <= clickTimeout) {
				return 0;
			}

			final int rawIndex = getInventoryIndex(fish.rawId);

			if (rawIndex != -1) {
				final Coordinate range = Object.RANGE.getCoordinate();

				if (playerX != range.getX() + 1 || playerY != range.getY()) {
					walkTo(range.getX() + 1, range.getY());
					return SLEEP_ONE_TICK;
				}

				useWithObject(rawIndex, range.getX(), range.getY());
				clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				return 0;
			}

			state = getInventoryCount(fish.cookedId) >= REQUIRED_FISH_COUNT_TO_CERT ? State.CERT : State.FISH;
			return 0;
		}

		if (distanceTo(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY()) <= 1) {
			if (getWallObjectIdFromCoords(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY()) == Object.RANGE_DOOR.id) {
				atWallObject(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY() - 1);

		if (getFatigue() != 0 && isWalking()) {
			return sleep();
		}

		return SLEEP_ONE_TICK;
	}

	private int cert() {
		if (Area.CERT_HOUSE.contains(playerX, playerY)) {
			final int itemId = cooking ? fish.cookedId : fish.rawId;

			if (getInventoryCount(itemId) < REQUIRED_FISH_COUNT_TO_CERT) {
				state = State.FISH;
				return 0;
			}

			if (isQuestMenu()) {
				int menuIndex;

				if ((menuIndex = getMenuIndex("I have some fish to trade in")) != -1 ||
					(menuIndex = getMenuIndex(cooking ? fish.cookedCertName : fish.rawCertName)) != -1 ||
					(menuIndex = getMenuIndex("Twentyfive")) != -1
				) {
					answer(menuIndex);
					optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
					return 0;
				}

				return SLEEP_ONE_SECOND;
			}

			if (System.currentTimeMillis() <= optionMenuTimeout) {
				return 0;
			}

			final int[] certer = getNpcByIdNotTalk(fish.certerId);

			if (certer[0] == -1) {
				return 0;
			}

			talkToNpc(certer[0]);
			optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (Area.RANGE_HOUSE.contains(playerX, playerY) &&
			getWallObjectIdFromCoords(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY()) == Object.RANGE_DOOR.id) {
			atWallObject(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (distanceTo(Object.CERT_DOOR.coordinate.getX(), Object.CERT_DOOR.coordinate.getY()) <= 1) {
			if (getWallObjectIdFromCoords(Object.CERT_DOOR.coordinate.getX(), Object.CERT_DOOR.coordinate.getY()) == Object.CERT_DOOR.id) {
				atWallObject(Object.CERT_DOOR.coordinate.getX(), Object.CERT_DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(Object.CERT_DOOR.coordinate.getX(), Object.CERT_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(Object.CERT_DOOR.coordinate.getX() - 1, Object.CERT_DOOR.coordinate.getY());

		if (getFatigue() != 0 && isWalking()) {
			return sleep();
		}

		return SLEEP_ONE_TICK;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Fishing Guild", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Catching: @gr1@%s", fish.cookedCertName),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double fishingXpGained = getAccurateXpForLevel(Skill.FISHING.getIndex()) - initialFishingXp;

		drawString(String.format("@cya@Fishing Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(fishingXpGained), toUnitsPerHour((int) fishingXpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@cya@Fish Caught: @whi@%s @cya@(@whi@%s fish@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(fishCaught), toUnitsPerHour(fishCaught, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (cooking) {
			final double cookingXpGained = getAccurateXpForLevel(Skill.COOKING.getIndex()) - initialCookingXp;

			drawString(String.format("@or1@Cooking Xp: @whi@%s @or1@(@whi@%s xp@or1@/@whi@hr@or1@)",
					DECIMAL_FORMAT.format(cookingXpGained), toUnitsPerHour((int) cookingXpGained, startTime)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

			drawString(String.format("@or1@Fish Cooked: @whi@%s", DECIMAL_FORMAT.format(fishCooked)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

			if (fishBurnt > 0) {
				drawString(String.format("@or1@Fish Burnt: @whi@%s", DECIMAL_FORMAT.format(fishBurnt)),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
			}
		}

		drawString(String.format("@gre@Fish Certed: @whi@%s", DECIMAL_FORMAT.format(fishCerted)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);
	}

	@Override
	public void onChatMessage(final String message, final String playerName, final boolean moderator, final boolean administrator) {
		if (receiver.isEmpty()) {
			return;
		}

		if (playerName.equals(receiver) && message.equals("Trade")) {
			trading = true;
		}
	}

	private enum State {
		FISH,
		COOK,
		CERT
	}

	private enum Fish {
		LOBSTER(372, 373, 374, 375, 369, new int[]{533, 534}, "Raw Lobster", "Lobster", new Coordinate(596, 501)),
		SHARK(545, 546, 547, 379, 370, new int[]{630, 631}, "Raw Shark", "Shark", new Coordinate(593, 501));

		private final int rawId;
		private final int cookedId;
		private final int burntId;
		private final int toolId;
		private final int certerId;
		private final int[] certIds;
		private final String rawCertName;
		private final String cookedCertName;
		private final Coordinate coordinate;

		Fish(final int rawId, final int cookedId, final int burntId, final int toolId, final int certerId, final int[] certIds, final String rawCertName, final String cookedCertName, final Coordinate coordinate) {
			this.rawId = rawId;
			this.cookedId = cookedId;
			this.burntId = burntId;
			this.toolId = toolId;
			this.certerId = certerId;
			this.certIds = certIds;
			this.rawCertName = rawCertName;
			this.cookedCertName = cookedCertName;
			this.coordinate = coordinate;
		}
	}

	private enum Area implements RSArea {
		DOCKS(new Coordinate(586, 496), new Coordinate(598, 513)),
		RANGE_HOUSE(new Coordinate(583, 519), new Coordinate(588, 523)),
		CERT_HOUSE(new Coordinate(602, 500), new Coordinate(605, 505)) {
			@Override
			public boolean contains(final int x, final int y) {
				return super.contains(x, y) || (x >= 603 && x <= 605 && y == 506);
			}
		};

		private final Coordinate lowerBoundingCoordinate;
		private final Coordinate upperBoundingCoordinate;

		Area(final Coordinate lowerBoundingCoordinate, final Coordinate upperBoundingCoordinate) {
			this.lowerBoundingCoordinate = lowerBoundingCoordinate;
			this.upperBoundingCoordinate = upperBoundingCoordinate;
		}

		public Coordinate getLowerBoundingCoordinate() {
			return lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return upperBoundingCoordinate;
		}
	}

	private enum Object implements RSObject {
		CERT_DOOR(2, new Coordinate(603, 506)),
		RANGE_DOOR(2, new Coordinate(586, 519)),
		RANGE(11, new Coordinate(583, 520));

		private final int id;
		private final Coordinate coordinate;

		Object(final int id, final Coordinate coordinate) {
			this.id = id;
			this.coordinate = coordinate;
		}

		public int getId() {
			return id;
		}

		public Coordinate getCoordinate() {
			return coordinate;
		}
	}
}
