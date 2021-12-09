import com.aposbot.Constants;

import java.awt.Font;
import java.time.Duration;
import java.time.Instant;

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
 * Author: Chomp
 */
public class AA_FishingGuild extends AA_Script {
	private static final int SKILL_INDEX_FISHING = 10;
	private static final int SKILL_INDEX_COOKING = 7;

	private static final int MAXIMUM_FATIGUE = 99;
	private static final int REQUIRED_FISH_COUNT_TO_CERT = 25;

	private Fish fish;
	private State state;
	private Instant startTime;
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
		if (parameters.isEmpty()) {
			throw new IllegalStateException("Empty parameters. Must at least specify fish type.");
		}

		final String[] args = parameters.split(" ");

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
				case "-f":
				case "--fish":
					this.fish = Fish.valueOf(args[++i].toUpperCase());
					break;
				case "-c":
				case "--cook":
					this.cooking = true;
					break;
				case "-r":
				case "--receiver":
					this.receiver = args[++i];
					break;
				default:
					throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
			}
		}

		if (this.fish == null) {
			throw new IllegalStateException("Missing fish parameter.");
		}

		if (!this.hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!this.hasInventoryItem(this.fish.toolId)) {
			throw new IllegalStateException(String.format("%s missing from inventory.", getItemNameId(this.fish.toolId)));
		}

		this.state = State.FISH;
		this.initialFishingXp = this.getAccurateXpForLevel(SKILL_INDEX_FISHING);
		this.initialCookingXp = this.getAccurateXpForLevel(SKILL_INDEX_COOKING);
		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		this.playerX = this.getX();
		this.playerY = this.getY();

		if (this.trading) {
			return this.trade();
		}

		switch (this.state) {
			case FISH:
				return this.fish();
			case COOK:
				return this.cook();
			case CERT:
				return this.cert();
			default:
				return this.exit("Invalid script state.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("fail", 4)) {
			this.clickTimeout = 0;
		} else if (message.startsWith("catch", 4)) {
			this.fishCaught++;
			this.clickTimeout = 0;
		} else if (message.startsWith("burn", 17)) {
			this.fishBurnt++;
			this.clickTimeout = 0;
		} else if (message.endsWith("cooked")) {
			this.fishCooked++;
			this.clickTimeout = 0;
		} else if (message.endsWith("certificates")) {
			this.fishCerted += REQUIRED_FISH_COUNT_TO_CERT;
		} else if (message.endsWith("area")) {
			this.idle = true;
		} else if (message.startsWith("Trade") || message.endsWith("trade")) {
			this.clickTimeout = 0L;
			this.optionMenuTimeout = 0L;
			this.tradeRequestTimeout = 0L;
			this.trading = false;
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Fishing Guild", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);
		this.drawString(String.format("@yel@Catching: @gr1@%s", this.fish.cookedCertName),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		final double fishingXpGained = this.getAccurateXpForLevel(SKILL_INDEX_FISHING) - this.initialFishingXp;

		this.drawString(String.format("@cya@Fishing Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(fishingXpGained), getUnitsPerHour(fishingXpGained, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@cya@Fish Caught: @whi@%s @cya@(@whi@%s fish@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(this.fishCaught), getUnitsPerHour(this.fishCaught, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.cooking) {
			this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			final double cookingXpGained = this.getAccurateXpForLevel(SKILL_INDEX_COOKING) - this.initialCookingXp;

			this.drawString(String.format("@or1@Cooking Xp: @whi@%s @or1@(@whi@%s xp@or1@/@whi@hr@or1@)",
					DECIMAL_FORMAT.format(cookingXpGained), getUnitsPerHour(cookingXpGained, secondsElapsed)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			this.drawString(String.format("@or1@Fish Cooked: @whi@%s", DECIMAL_FORMAT.format(this.fishCooked)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			if (this.fishBurnt > 0) {
				this.drawString(String.format("@or1@Fish Burnt: @whi@%s", DECIMAL_FORMAT.format(this.fishBurnt)),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
			}
		}

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@gre@Fish Certed: @whi@%s", DECIMAL_FORMAT.format(this.fishCerted)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
	}

	@Override
	public void onChatMessage(final String message, final String playerName, final boolean moderator, final boolean administrator) {
		if (this.receiver.isEmpty()) {
			return;
		}

		if (playerName.equals(this.receiver) && message.equals("Trade")) {
			this.trading = true;
		}
	}

	private int trade() {
		final int[] player = this.getPlayerByName(this.receiver);

		if (player[0] == -1) {
			this.trading = false;
			return 0;
		}

		final int inventoryIndex = this.getInventoryIndex(this.fish.certIds);

		if (inventoryIndex == -1) {
			this.trading = false;
			return 0;
		}

		if (this.isInTradeConfirm()) {
			if (this.extension.hasLocalConfirmedTrade()) {
				return 0;
			}

			this.confirmTrade();
			return SLEEP_ONE_TICK;
		}

		if (this.isInTradeOffer()) {
			if (this.getLocalTradeItemCount() == 0) {
				if (System.currentTimeMillis() <= this.clickTimeout) {
					return 0;
				}

				this.offerItemTrade(inventoryIndex, this.getInventoryStack(inventoryIndex));
				this.clickTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
				return 0;
			}

			if (this.extension.hasLocalAcceptedTrade()) {
				return 0;
			}

			this.acceptTrade();
			return SLEEP_ONE_TICK;
		}

		if (this.distanceTo(player[1], player[2]) > 1) {
			this.walkTo(player[1], player[2]);
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= this.tradeRequestTimeout) {
			return 0;
		}

		this.sendTradeRequest(this.getPlayerPID(player[0]));
		this.tradeRequestTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
		return 0;
	}

	private int fish() {
		if (Area.DOCKS.contains(this.playerX, this.playerY)) {
			if (this.idle) {
				if (this.playerY == this.fish.coordinate.getY() + 2) {
					this.idle = false;
					return 0;
				}

				this.walkTo(this.fish.coordinate.getX() - 1, this.fish.coordinate.getY() + 2);
				return SLEEP_ONE_TICK;
			}

			if (this.getInventoryCount() == MAX_INV_SIZE) {
				this.state = this.cooking ? State.COOK : State.CERT;
				return 0;
			}

			if (this.getFatigue() >= MAXIMUM_FATIGUE) {
				return this.sleep();
			}

			if (System.currentTimeMillis() <= this.clickTimeout) {
				return 0;
			}

			final Coordinate fishCoordinate = this.fish.coordinate;

			if (this.playerX != fishCoordinate.getX() - 1 ||
				this.playerY != fishCoordinate.getY() + 1) {
				this.walkTo(fishCoordinate.getX() - 1, fishCoordinate.getY() + 1);
				return SLEEP_ONE_TICK;
			}

			this.extension.createPacket(Constants.OP_OBJECT_ACTION2);
			this.extension.put2(fishCoordinate.getX());
			this.extension.put2(fishCoordinate.getY());
			this.extension.finishPacket();

			this.clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (Area.RANGE_HOUSE.contains(this.playerX, this.playerY) &&
			this.getWallObjectIdFromCoords(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY()) == Object.RANGE_DOOR.id) {
			this.atWallObject(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (Area.CERT_HOUSE.contains(this.playerX, this.playerY) &&
			this.getWallObjectIdFromCoords(Object.CERT_DOOR.coordinate.getX(), Object.CERT_DOOR.coordinate.getY()) == Object.CERT_DOOR.id) {
			this.atWallObject(Object.CERT_DOOR.coordinate.getX(), Object.CERT_DOOR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		this.walkTo(this.fish.coordinate.getX() - 1, this.fish.coordinate.getY() + 1);

		if (this.getFatigue() != 0 && this.isWalking()) {
			return this.sleep();
		}

		return SLEEP_ONE_TICK;
	}

	private int cook() {
		if (Area.RANGE_HOUSE.contains(this.playerX, this.playerY)) {
			final int burntIndex = this.getInventoryIndex(this.fish.burntId);

			if (burntIndex != -1) {
				this.dropItem(burntIndex);
				return SLEEP_ONE_TICK;
			}

			if (this.getFatigue() >= MAXIMUM_FATIGUE) {
				return this.sleep();
			}

			if (System.currentTimeMillis() <= this.clickTimeout) {
				return 0;
			}

			final int rawIndex = this.getInventoryIndex(this.fish.rawId);

			if (rawIndex != -1) {
				if (this.playerX != Object.RANGE.coordinate.getX() + 1 ||
					this.playerY != Object.RANGE.coordinate.getY()) {
					this.walkTo(Object.RANGE.coordinate.getX() + 1, Object.RANGE.coordinate.getY());
					return SLEEP_ONE_TICK;
				}

				this.extension.createPacket(Constants.OP_OBJECT_USEWITH);
				this.extension.put2(Object.RANGE.coordinate.getX());
				this.extension.put2(Object.RANGE.coordinate.getY());
				this.extension.put2(rawIndex);
				this.extension.finishPacket();

				this.clickTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
				return 0;
			}

			this.state = this.getInventoryCount(this.fish.cookedId) >= REQUIRED_FISH_COUNT_TO_CERT ? State.CERT : State.FISH;
			return 0;
		}

		if (this.distanceTo(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY()) <= 1) {
			if (this.getWallObjectIdFromCoords(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY()) == Object.RANGE_DOOR.id) {
				this.atWallObject(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY() - 1);

		if (this.getFatigue() != 0 && this.isWalking()) {
			return this.sleep();
		}

		return SLEEP_ONE_TICK;
	}

	private int cert() {
		if (Area.CERT_HOUSE.contains(this.playerX, this.playerY)) {
			final int itemId = this.cooking ? this.fish.cookedId : this.fish.rawId;

			if (this.getInventoryCount(itemId) < REQUIRED_FISH_COUNT_TO_CERT) {
				this.state = State.FISH;
				return 0;
			}

			if (this.isQuestMenu()) {
				int menuIndex;

				if ((menuIndex = this.getMenuIndex("I have some fish to trade in")) != -1 ||
					(menuIndex = this.getMenuIndex(this.cooking ? this.fish.cookedCertName : this.fish.rawCertName)) != -1 ||
					(menuIndex = this.getMenuIndex("Twentyfive")) != -1
				) {
					this.answer(menuIndex);
					this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
					return 0;
				}

				return SLEEP_ONE_SECOND;
			}

			if (System.currentTimeMillis() <= this.optionMenuTimeout) {
				return 0;
			}

			final int[] certer = this.getNpcByIdNotTalk(this.fish.certerId);

			if (certer[0] == -1) {
				return 0;
			}

			this.talkToNpc(certer[0]);
			this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (Area.RANGE_HOUSE.contains(this.playerX, this.playerY) &&
			this.getWallObjectIdFromCoords(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY()) == Object.RANGE_DOOR.id) {
			this.atWallObject(Object.RANGE_DOOR.coordinate.getX(), Object.RANGE_DOOR.coordinate.getY());
			return SLEEP_ONE_SECOND;
		}

		if (this.distanceTo(Object.CERT_DOOR.coordinate.getX(), Object.CERT_DOOR.coordinate.getY()) <= 1) {
			if (this.getWallObjectIdFromCoords(Object.CERT_DOOR.coordinate.getX(), Object.CERT_DOOR.coordinate.getY()) == Object.CERT_DOOR.id) {
				this.atWallObject(Object.CERT_DOOR.coordinate.getX(), Object.CERT_DOOR.coordinate.getY());
				return SLEEP_ONE_SECOND;
			}

			this.walkTo(Object.CERT_DOOR.coordinate.getX(), Object.CERT_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		this.walkTo(Object.CERT_DOOR.coordinate.getX() - 1, Object.CERT_DOOR.coordinate.getY());

		if (this.getFatigue() != 0 && this.isWalking()) {
			return this.sleep();
		}

		return SLEEP_ONE_TICK;
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
			return this.lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return this.upperBoundingCoordinate;
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
			return this.id;
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
		}
	}
}
