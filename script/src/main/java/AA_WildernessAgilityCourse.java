/**
 * A script for the Wilderness Agility Course.
 * Start script at the agility course with sleeping bag and food in inventory.
 * Banks at the Mage Arena and walks back on death.
 * <p>
 * Optional Parameter:
 * <tuna|lobster|swordfish|...|shark>
 * <p>
 *
 * @Author Chomp
 */
public class AA_WildernessAgilityCourse extends AA_Script {
	private static final Coordinate[] PATH_TO_BANK = new Coordinate[]{
		new Coordinate(240, 117),
		new Coordinate(227, 105)
	};

	private static final Coordinate[] PATH_FROM_BANK = new Coordinate[]{
		new Coordinate(287, 137),
		new Coordinate(297, 134)
	};

	private static final Coordinate COORD_ICE_PLATEAU = new Coordinate(330, 150);

	private static final int[] ITEM_IDS_WEAPON = new int[]{594, 593, 81};

	private static final int NPC_ID_SKELETON = 45;
	private static final int MIN_HITS = 20;
	private static final int MAX_DIST = 12;
	private static final int MAX_FATIGUE = 97;

	private long startTime;
	private PathWalker pathWalker;
	private State state;
	private Obstacle obstacle;
	private Food food;

	private double initialAgilityXp;

	private long obstacleTimeout;
	private long eatTimeout;

	private int playerX;
	private int playerY;
	private int previousX;

	private int deathCount;
	private int lapCount;
	private int foodCount;
	private int bankCount;

	private boolean died;

	public AA_WildernessAgilityCourse(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (!bot.isLoggedIn()) {
			throw new IllegalStateException("Must start script while logged in.");
		}

		if (!parameters.isEmpty()) {
			try {
				food = Food.valueOf(parameters.toUpperCase());
			} catch (final IllegalArgumentException e) {
				throw new IllegalArgumentException("Parameters contain invalid Food type: " + parameters, e);
			}
		} else {
			food = Food.SHARK;
		}

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		playerX = getX();
		playerY = getY();

		setState(WACArea.isBelowWildernessGate(playerX, playerY) ? State.WALKBACK : State.BANK);
		initialAgilityXp = bot.getExperience(Skill.AGILITY.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		if (died) {
			if (isDead()) {
				return 0;
			}

			died = false;
		}

		playerX = getX();
		playerY = getY();

		switch (state) {
			case COURSE:
				return doCourse();
			case BANK:
				return bank();
			case WALKBACK:
				return walkback();
		}

		return 0;
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("pipe")) {
			obstacle = Obstacle.ROPESWING;
		} else if (message.endsWith("hole")) {
			obstacle = Obstacle.STONE;
		} else if (message.endsWith("walk across")) {
			obstacle = obstacle == Obstacle.STONE ? Obstacle.LEDGE : Obstacle.VINE;
		} else if (message.endsWith("cliff")) {
			lapCount++;
			obstacle = Obstacle.PIPE;
		} else if (message.endsWith("ouch")) {
			if (obstacle == Obstacle.LEDGE) {
				obstacle = Obstacle.STONE;
			}
		} else if (message.endsWith("lava")) {
			obstacleTimeout = 0L;
		} else if (message.startsWith("eat", 4)) {
			foodCount++;
			eatTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
			obstacleTimeout = 0L;
		} else if (message.endsWith("it") || message.endsWith("web")) {
			obstacleTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("train")) {
			sleep();
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void onDeath() {
		System.out.printf("[%s] Oh dear! You are dead...%n", this);
		deathCount++;
		died = true;
		setState(State.WALKBACK);
	}

	private void setState(final State state) {
		if (this.state != null && state == State.BANK) {
			bankCount++;
		}
		this.state = state;
		obstacle = WACArea.VINES.contains(playerX, playerY) ? Obstacle.VINE : Obstacle.PIPE;
		System.out.printf("[%s] State: %s%n", this, this.state);
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Wilderness Agility Course", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@State: @whi@%s", state),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Obstacle: @whi@%s", obstacle),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getSkillExperience(Skill.AGILITY.getIndex()) - initialAgilityXp;

		bot.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Laps: @whi@%d @cya@(@whi@%s laps@cya@/@whi@hr@cya@)",
				lapCount, toUnitsPerHour(lapCount, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s food@cya@/@whi@hr@cya@)",
				food, foodCount, toUnitsPerHour(foodCount, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Bank Trips: @whi@%d", bankCount),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (deathCount > 0) {
			bot.drawString(String.format("@yel@Deaths: %d", deathCount),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);
		}
	}

	private int doCourse() {
		if (getCurrentHits() <= MIN_HITS && !inCombat()) {
			final int foodIndex = getInventoryIndex(food.getId());

			if (foodIndex == -1) {
				setState(State.BANK);
				return 0;
			}

			return consume(foodIndex);
		}

		if (WACArea.isInsideSpikePit(playerY)) {
			return exitSpikePit();
		}

		if (getFatigue() >= MAX_FATIGUE && isWalking()) {
			return sleep();
		}

		if (obstacleTimeout != 0 && System.currentTimeMillis() <= obstacleTimeout) {
			if (playerX == previousX ||
				(obstacle == Obstacle.LEDGE && playerX != 297) ||
				(obstacle == Obstacle.VINE && playerX != 301)) {
				return 0;
			}

			obstacleTimeout = 0;
		}

		final Coordinate obstacle = this.obstacle.getCoordinate();

		if (distanceTo(obstacle.getX(), obstacle.getY()) > 1 || inCombat()) {
			final int adjX;
			final int adjY;

			switch (this.obstacle) {
				case PIPE:
				case ROPESWING:
					adjX = obstacle.getX() + 1;
					adjY = obstacle.getY() + 1;
					break;
				case STONE:
					adjX = obstacle.getX() - 1;
					adjY = obstacle.getY() + 1;
					break;
				case LEDGE:
				case VINE:
					adjX = obstacle.getX() - 1;
					adjY = obstacle.getY() - 1;
					break;
				default:
					return exit("Unsupported obstacle: " + this.obstacle);
			}

			walkTo(adjX, adjY);
			return SLEEP_ONE_TICK;
		}

		previousX = playerX;
		useObject1(obstacle.getX(), obstacle.getY());
		obstacleTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	private int exitSpikePit() {
		if (!isInCombat() && WACArea.isOnSpikes(playerX, playerY)) {
			final int blockingNpc = getBlockingNpc();

			if (blockingNpc != -1) {
				attackNpc(blockingNpc);
				return SLEEP_ONE_TICK;
			}
		}

		final Coordinate ladder = WACObject.PIT_LADDER.getCoordinate();

		if (playerX != ladder.getX() + 1 || playerY != ladder.getY() + 1 || inCombat()) {
			walkTo(ladder.getX() + 1, ladder.getY() + 1);
		} else {
			useObject1(ladder.getX(), ladder.getY());
		}

		return SLEEP_ONE_TICK;
	}

	private int bank() {
		if (!hasInventoryItem(food.getId())) {
			return enterBank();
		}

		return returnToCourse();
	}

	private int enterBank() {
		if (WACArea.isInsideBank(playerY)) {
			if (!isBanking()) {
				return openBank();
			}

			if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
				if (!hasBankItem(ITEM_ID_SLEEPING_BAG)) {
					return exit("Sleeping bag missing from bank.");
				}

				withdraw(ITEM_ID_SLEEPING_BAG, 1);
				return SLEEP_TWO_SECONDS;
			}

			if (!hasBankItem(food.getId())) {
				return exit("Food missing from bank.");
			}

			withdraw(food.getId(), getInventoryEmptyCount());
			return SLEEP_THREE_SECONDS;
		}

		if (WACArea.isAtWebs(playerX)) {
			return enterWebs();
		}

		if (WACArea.COURSE.contains(playerX, playerY)) {
			if (WACArea.isInsideSpikePit(playerY)) {
				return exitSpikePit();
			}

			if (WACArea.VINES.contains(playerX, playerY)) {
				if (System.currentTimeMillis() <= obstacleTimeout) {
					return 0;
				}

				final Coordinate vines = Obstacle.VINE.getCoordinate();

				if (distanceTo(vines.getX(), vines.getY()) > 1 || inCombat()) {
					walkTo(vines.getX() - 1, vines.getY() - 1);
					return SLEEP_ONE_TICK;
				}

				useObject1(vines.getX(), vines.getY());
				obstacleTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				return 0;
			}

			final Coordinate gate = WACObject.COURSE_GATE_2.getCoordinate();

			if (distanceTo(gate.getX(), gate.getY()) > 1 || inCombat()) {
				walkTo(gate.getX(), gate.getY());
				return SLEEP_ONE_TICK;
			}

			atObject(gate.getX(), gate.getY());
			return SLEEP_ONE_SECOND;
		}

		for (final Coordinate coordinate : PATH_TO_BANK) {
			if (playerX > coordinate.getX()) {
				walkTo(coordinate.getX(), coordinate.getY());
				break;
			}
		}

		return SLEEP_ONE_TICK;
	}

	private int enterWebs() {
		if (inCombat()) {
			walkTo(playerX, playerY);
			return SLEEP_ONE_TICK;
		}

		if (playerX < WACObject.DOOR_SOUTH.getCoordinate().getX()) {
			final Coordinate ladder = WACObject.LADDER_DOWN.getCoordinate();
			atObject(ladder.getX(), ladder.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerY >= WACObject.WEB_SOUTH.getCoordinate().getY()) {
			final Coordinate door = WACObject.DOOR_SOUTH.getCoordinate();

			if (getWallObjectIdFromCoords(door.getX(), door.getY()) == WACObject.DOOR_SOUTH.getId()) {
				atWallObject(door.getX(), door.getY());
				return SLEEP_ONE_SECOND;
			}

			final Coordinate ladder = WACObject.LADDER_DOWN.getCoordinate();
			atObject(ladder.getX(), ladder.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerY >= WACObject.WEB_NORTH.getCoordinate().getY()) {
			final Coordinate web = WACObject.WEB_SOUTH.getCoordinate();

			if (getWallObjectIdFromCoords(web.getX(), web.getY()) == WACObject.WEB_SOUTH.getId()) {
				return slashWeb(web);
			}

			final Coordinate door = WACObject.DOOR_SOUTH.getCoordinate();
			walkTo(door.getX(), door.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerY >= WACObject.DOOR_NORTH.getCoordinate().getY()) {
			Coordinate web = WACObject.WEB_NORTH.getCoordinate();

			if (getWallObjectIdFromCoords(web.getX(), web.getY()) == WACObject.WEB_NORTH.getId()) {
				return slashWeb(web);
			}

			web = WACObject.WEB_SOUTH.getCoordinate();
			walkTo(web.getX(), web.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		final Coordinate door = WACObject.DOOR_NORTH.getCoordinate();

		if (getWallObjectIdFromCoords(door.getX(), door.getY()) == WACObject.DOOR_NORTH.getId()) {
			atWallObject(door.getX(), door.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(door.getX(), door.getY());
		return SLEEP_ONE_TICK;
	}

	private int returnToCourse() {
		if (WACArea.COURSE.contains(playerX, playerY)) {
			setState(State.COURSE);
			return 0;
		}

		if (WACArea.isInsideBank(playerY)) {
			final Coordinate ladder = WACObject.LADDER_UP.getCoordinate();
			atObject(ladder.getX(), ladder.getY());
			return SLEEP_ONE_TICK;
		}

		if (getCurrentHits() <= MIN_HITS && !inCombat()) {
			return consume(getInventoryIndex(food.getId()));
		}

		if (WACArea.isAtWebs(playerX)) {
			return exitWebs();
		}

		final Coordinate gate = WACObject.COURSE_GATE_1.getCoordinate();

		if (distanceTo(gate.getX(), gate.getY()) <= MAX_DIST) {
			if (inCombat()) {
				walkTo(gate.getX(), gate.getY());
				return SLEEP_ONE_TICK;
			}

			atObject(gate.getX(), gate.getY());
			return SLEEP_ONE_SECOND;
		}

		for (final Coordinate coordinate : PATH_FROM_BANK) {
			if (playerX < coordinate.getX()) {
				walkTo(coordinate.getX(), coordinate.getY());
				break;
			}
		}

		return SLEEP_ONE_TICK;
	}

	private int exitWebs() {
		if (inCombat()) {
			walkTo(playerX, playerY);
			return SLEEP_ONE_TICK;
		}

		if (playerY < WACObject.WEB_NORTH.getCoordinate().getY()) {
			final Coordinate door = WACObject.DOOR_NORTH.getCoordinate();

			if (getWallObjectIdFromCoords(door.getX(), door.getY()) == WACObject.DOOR_NORTH.getId()) {
				atWallObject(door.getX(), door.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(door.getX() + 1, door.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (playerY < WACObject.WEB_SOUTH.getCoordinate().getY()) {
			final Coordinate web = WACObject.WEB_NORTH.getCoordinate();

			if (getWallObjectIdFromCoords(web.getX(), web.getY()) == WACObject.WEB_NORTH.getId()) {
				return slashWeb(web);
			}

			final Coordinate door = WACObject.DOOR_NORTH.getCoordinate();

			walkTo(door.getX(), door.getY());
			return SLEEP_ONE_TICK;
		}

		if (playerX >= WACObject.DOOR_SOUTH.getCoordinate().getX()) {
			Coordinate web = WACObject.WEB_SOUTH.getCoordinate();

			if (getWallObjectIdFromCoords(web.getX(), web.getY()) == WACObject.WEB_SOUTH.getId()) {
				return slashWeb(web);
			}

			web = WACObject.WEB_NORTH.getCoordinate();

			walkTo(web.getX(), web.getY());
			return SLEEP_ONE_TICK;
		}

		final Coordinate door = WACObject.DOOR_SOUTH.getCoordinate();

		if (getWallObjectIdFromCoords(door.getX(), door.getY()) == WACObject.DOOR_SOUTH.getId()) {
			atWallObject(door.getX(), door.getY());
			return SLEEP_ONE_SECOND;
		}

		final Coordinate web = WACObject.WEB_SOUTH.getCoordinate();

		walkTo(web.getX(), web.getY());
		return SLEEP_ONE_TICK;
	}

	private int walkback() {
		if (pathWalker != null) {
			if ((playerX != COORD_ICE_PLATEAU.getX() || playerY != COORD_ICE_PLATEAU.getY()) && pathWalker.walkPath()) {
				return 0;
			}

			pathWalker = null;
		}

		Coordinate gate = WACObject.COURSE_GATE_1.getCoordinate();

		if (playerX == gate.getX() && playerY == gate.getY()) {
			setState(State.BANK);
			return 0;
		}

		if (!WACArea.isBelowWildernessGate(playerX, playerY)) {
			walkTo(gate.getX(), gate.getY());
			return SLEEP_ONE_TICK;
		}

		gate = WACObject.ICE_PLATEAU_GATE.getCoordinate();

		if (distanceTo(gate.getX(), gate.getY()) <= MAX_DIST) {
			if (inCombat()) {
				walkTo(gate.getX(), gate.getY());
				return SLEEP_ONE_TICK;
			}

			atObject(gate.getX(), gate.getY());
			return SLEEP_ONE_SECOND;
		}

		pathWalker = new PathWalker(bot);
		pathWalker.init(null);

		final PathWalker.Path path = pathWalker.calcPath(getX(), getY(),
			COORD_ICE_PLATEAU.getX(), COORD_ICE_PLATEAU.getY());

		if (path == null) {
			return exit(String.format("Failed to calculate path (%d, %d) -> (%d, %d)",
				playerX, playerY, COORD_ICE_PLATEAU.getX(), COORD_ICE_PLATEAU.getY()));
		}

		pathWalker.setPath(path);
		return 0;
	}

	private int consume(final int inventoryIndex) {
		if (System.currentTimeMillis() <= eatTimeout) {
			return 0;
		}

		useItem(inventoryIndex);
		eatTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private int slashWeb(final Coordinate web) {
		if (System.currentTimeMillis() <= obstacleTimeout) {
			return 0;
		}

		final int weaponIndex = getInventoryIndex(ITEM_IDS_WEAPON);

		if (weaponIndex == -1) {
			return exit("Weapon missing from inventory. Cannot cut webs.");
		}

		useItemOnWallObject(weaponIndex, web.getX(), web.getY());
		obstacleTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private int getBlockingNpc() {
		for (int index = 0; index < bot.getNpcCount(); index++) {
			final Object npc = bot.getNpc(index);

			if (bot.getNpcId(npc) != NPC_ID_SKELETON || distanceTo(npc) > 1) {
				continue;
			}

			return index;
		}

		return -1;
	}

	private enum State {
		COURSE("Course"),
		BANK("Bank"),
		WALKBACK("Walkback");

		private final String description;

		State(final String description) {
			this.description = description;
		}

		@Override
		public String toString() {
			return description;
		}
	}

	private enum Obstacle {
		PIPE("Pipe", new Coordinate(294, 119)),
		ROPESWING("Rope Swing", new Coordinate(292, 110)),
		STONE("Lava Stones", new Coordinate(293, 105)),
		LEDGE("Ledge Balance", new Coordinate(296, 111)),
		VINE("Cliff Vines", new Coordinate(305, 118));

		private final String description;
		private final Coordinate coordinate;

		Obstacle(final String description, final Coordinate coordinate) {
			this.description = description;
			this.coordinate = coordinate;
		}

		@Override
		public String toString() {
			return description;
		}

		public String getDescription() {
			return description;
		}

		public Coordinate getCoordinate() {
			return coordinate;
		}
	}

	private enum WACObject implements RSObject {
		WEB_NORTH(24, new Coordinate(227, 107)),
		WEB_SOUTH(24, new Coordinate(227, 109)),
		DOOR_NORTH(2, new Coordinate(227, 106)),
		DOOR_SOUTH(2, new Coordinate(226, 110)),
		LADDER_UP(1187, new Coordinate(446, 3367)),
		LADDER_DOWN(1188, new Coordinate(223, 110)),
		COURSE_GATE_1(703, new Coordinate(297, 134)),
		COURSE_GATE_2(704, new Coordinate(297, 125)),
		ICE_PLATEAU_GATE(346, new Coordinate(331, 142)),
		PIT_LADDER(5, new Coordinate(289, 2933));

		private final int id;
		private final Coordinate coordinate;

		WACObject(final int id, final Coordinate coordinate) {
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

	private enum WACArea implements RSArea {
		COURSE(new Coordinate(288, 98), new Coordinate(312, 125)) {
			@Override
			public boolean contains(final int x, final int y) {
				return super.contains(x, y) || isInsideSpikePit(y);
			}
		},
		VINES(new Coordinate(296, 110), new Coordinate(312, 117)),
		SPIKES_1(new Coordinate(292, 2941), new Coordinate(293, 2942)),
		SPIKES_2(new Coordinate(296, 2942), new Coordinate(301, 2943)),
		SPIKES_3(new Coordinate(296, 2945), new Coordinate(301, 2946));

		private final Coordinate lowerBoundingCoordinate;
		private final Coordinate upperBoundingCoordinate;

		WACArea(final Coordinate lowerBoundingCoordinate, final Coordinate upperBoundingCoordinate) {
			this.lowerBoundingCoordinate = lowerBoundingCoordinate;
			this.upperBoundingCoordinate = upperBoundingCoordinate;
		}

		private static boolean isInsideBank(final int playerY) {
			return playerY >= 3000;
		}

		private static boolean isAtWebs(final int playerX) {
			return playerX <= 227;
		}

		private static boolean isBelowWildernessGate(final int playerX, final int playerY) {
			return playerY >= 142 && playerY < 2000;
		}

		private static boolean isInsideSpikePit(final int playerY) {
			return playerY >= 2000 && playerY < 3000;
		}

		private static boolean isOnSpikes(final int playerX, final int playerY) {
			return SPIKES_1.contains(playerX, playerY) || SPIKES_2.contains(playerX, playerY) ||
				SPIKES_3.contains(playerX, playerY);
		}

		public Coordinate getLowerBoundingCoordinate() {
			return lowerBoundingCoordinate;
		}

		public Coordinate getUpperBoundingCoordinate() {
			return upperBoundingCoordinate;
		}
	}
}
