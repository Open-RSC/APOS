public class AA_WitchsHouseQuest extends AA_Quest {
	private static final String[] MENU_OPTIONS = new String[]{"What's the matter?", "Ok, I'll see what I can do"};

	private QuestStage stage;

	private long actionTimeout;

	public AA_WitchsHouseQuest(final Extension ex) {
		super(ex);
		this.stage = QuestStage.BOY;
	}

	@Override
	public int main() {
		if (this.pathWalker.walkPath()) {
			return 0;
		}

		switch (this.stage) {
			case BOY:
				return this.speakWithBoy();
			case KEY:
				return this.getFrontDoorKey();
			case ENTER_HOUSE:
				return this.enterHouse();
			case MAGNET:
				return this.getMagnet();
			case RAT:
				return this.magnetizeRat();
			case WITCH:
				return this.spawnWitch();
			case SHED:
				return this.unlockShed();
			case BALL:
				return this.getBall();
			case EXIT_HOUSE:
				return this.exitHouse();
			default:
				throw new QuestException("Invalid quest stage.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		switch (this.stage) {
			case BOY:
				if (message.equals("Boy: Thankyou") ||
					message.equals("Boy: Well it's in the shed in that garden")) {
					this.setStage(QuestStage.KEY);
					this.menuTimeout = 0L;
				}
				break;
			case ENTER_HOUSE:
				if (message.equals("you go through the door")) {
					this.setStage(QuestStage.MAGNET);
					this.actionTimeout = 0L;
				}
				break;
			case RAT:
				if (message.equals("You hear a click and whirr")) {
					this.setStage(QuestStage.WITCH);
				}
				break;
			case WITCH:
				if (message.equals("The footsteps approach the back door")) {
					this.setStage(QuestStage.SHED);
				}
				break;
			case SHED:
				if (message.equals("Leaving the shed door unlocked")) {
					this.setStage(QuestStage.BALL);
				}
				break;
			case EXIT_HOUSE:
				if (message.equals("you go through the door")) {
					this.setStage(QuestStage.BOY);
					this.actionTimeout = 0L;
					this.menuTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
				}
				break;
		}
	}

	@Override
	protected void initializeQuest(final String parameters) {
		if (!parameters.isEmpty()) {
			this.setStage(QuestStage.fromId(Integer.parseInt(parameters)));
		}
	}

	@Override
	protected Quest getQuest() {
		return Quest.WITCHS_HOUSE;
	}

	@Override
	protected Item[] getItems() {
		return QuestItem.values();
	}

	public Stage getStage() {
		return this.stage;
	}

	private void setStage(final QuestStage stage) {
		this.stage = stage;
		System.out.printf("[%s] Stage: %s(%d)%n", this, this.stage, this.stage.getId());
	}

	private int speakWithBoy() {
		if (this.isQuestComplete()) {
			return this.completeQuest();
		}

		if (this.isQuestMenu()) {
			return this.answerMenu(MENU_OPTIONS);
		}

		if (System.currentTimeMillis() <= this.menuTimeout) {
			return 0;
		}

		return this.talkTo(QuestEntity.BOY);
	}

	private int getFrontDoorKey() {
		if (this.hasInventoryItem(QuestItem.FRONT_DOOR_KEY.getId())) {
			this.setStage(QuestStage.ENTER_HOUSE);
			return 0;
		}

		if (this.isAtEntity(QuestEntity.DOOR_MAT)) {
			this.atObject(QuestEntity.DOOR_MAT);
		} else {
			this.walkTo(QuestEntity.DOOR_MAT);
		}

		return SLEEP_ONE_TICK;
	}

	private int enterHouse() {
		if (this.isAtEntity(QuestEntity.FRONT_DOOR)) {
			if (System.currentTimeMillis() <= this.actionTimeout) {
				return 0;
			}

			this.useItemOnWallObject(this.getItemIndex(QuestItem.FRONT_DOOR_KEY),
				QuestEntity.FRONT_DOOR.getTile().getX(), QuestEntity.FRONT_DOOR.getTile().getY());
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
			return 0;
		}

		this.walkTo(QuestEntity.FRONT_DOOR);
		return SLEEP_ONE_TICK;
	}

	private int getMagnet() {
		if (this.hasInventoryItem(QuestItem.MAGNET.getId())) {
			this.setStage(QuestStage.RAT);
			return 0;
		}

		if (this.getY() >= QuestEntity.LADDER_UP.getTile().getY()) {
			if (this.getY() > QuestEntity.GATE.getTile().getY()) {
				this.atObject(QuestEntity.CUPBOARD);
			} else {
				final int glovesIndex = this.getItemIndex(QuestItem.LEATHER_GLOVES);

				if (!this.isItemEquipped(glovesIndex)) {
					this.wearItem(glovesIndex);
					return SLEEP_ONE_TICK;
				}

				this.atObject(QuestEntity.GATE);
			}
		} else {
			this.atObject(QuestEntity.LADDER_DOWN);
		}

		return SLEEP_ONE_TICK;
	}

	private int magnetizeRat() {
		if (this.getX() < QuestEntity.RAT_DOOR_WEST.getTile().getX()) {
			final int[] rat = this.getAllNpcById(QuestEntity.RAT.getId());

			if (rat[0] != -1) {
				this.useOnNpc(rat[0], this.getItemIndex(QuestItem.MAGNET));
				return SLEEP_ONE_TICK;
			}

			final int[] cheese = this.getItemById(QuestItem.CHEESE.getId());

			if (cheese[0] != -1) {
				return 0;
			}

			final int cheeseIndex = this.getInventoryIndex(QuestItem.CHEESE.getId());

			if (cheeseIndex != -1) {
				this.dropItem(cheeseIndex);
				return SLEEP_TWO_SECONDS;
			}

			return SLEEP_ONE_TICK;
		}

		if (this.getY() < QuestEntity.LADDER_UP.getTile().getY()) {
			this.atWallObject(QuestEntity.RAT_DOOR_WEST);
		} else if (this.getY() <= QuestEntity.GATE.getTile().getY()) {
			this.atObject(QuestEntity.LADDER_UP);
		} else {
			this.atObject(QuestEntity.GATE);
		}

		return SLEEP_ONE_TICK;
	}

	private int spawnWitch() {
		if (this.getX() < QuestEntity.RAT_DOOR_EAST.getTile().getX()) {
			this.atWallObject(QuestEntity.SHED_DOOR);
		} else {
			this.atWallObject(QuestEntity.RAT_DOOR_EAST);
		}

		return SLEEP_ONE_TICK;
	}

	private int unlockShed() {
		if (this.getX() >= QuestEntity.SMALL_ROOM_DOOR.getTile().getX()) {
			return 0;
		}

		this.atWallObject(QuestEntity.SMALL_ROOM_DOOR);
		return SLEEP_ONE_TICK;
	}

	private int getBall() {
		if (this.getX() <= QuestEntity.SHED_DOOR.getTile().getX() &&
			this.getY() < QuestEntity.SHED_DOOR.getTile().getY()) {
			if (this.getCurrentLevel(3) <= 12) {
				if (this.inCombat()) {
					this.walkHere();
					return SLEEP_ONE_TICK;
				}

				return 0;
			}

			if (this.inCombat()) {
				return 0;
			}

			if (this.hasInventoryItem(QuestItem.BALL.getId())) {
				this.setStage(QuestStage.EXIT_HOUSE);
				return 0;
			}

			final int[] ball = this.getItemById(QuestItem.BALL.getId());

			if (ball[0] == -1) {
				return 0;
			}

			this.pickupItem(ball[0], ball[1], ball[2]);
			return SLEEP_ONE_TICK;
		}

		if (this.getX() < QuestEntity.SMALL_ROOM_DOOR.getTile().getX()) {
			this.atWallObject(QuestEntity.SHED_DOOR);
		} else {
			this.atWallObject(QuestEntity.SMALL_ROOM_DOOR);
		}

		return SLEEP_ONE_TICK;
	}

	private int exitHouse() {
		if (this.getX() >= QuestEntity.FRONT_DOOR.getTile().getX()) {
			return 0;
		}

		if (this.getX() >= QuestEntity.RAT_DOOR_WEST.getTile().getX()) {
			if (System.currentTimeMillis() <= this.actionTimeout) {
				return 0;
			}

			this.useItemOnWallObject(this.getItemIndex(QuestItem.FRONT_DOOR_KEY),
				QuestEntity.FRONT_DOOR.getTile().getX(), QuestEntity.FRONT_DOOR.getTile().getY());
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
			return 0;
		}

		if (this.getX() >= QuestEntity.RAT_DOOR_EAST.getTile().getX()) {
			this.atWallObject(QuestEntity.RAT_DOOR_WEST);
		} else if (this.getX() <= QuestEntity.SHED_DOOR.getTile().getX() &&
			this.getY() < QuestEntity.SHED_DOOR.getTile().getY()) {
			this.atWallObject(QuestEntity.SHED_DOOR);
		} else {
			this.atWallObject(QuestEntity.RAT_DOOR_EAST);
		}

		return SLEEP_ONE_TICK;
	}

	private enum QuestStage implements Stage {
		BOY(0),
		KEY(1),
		ENTER_HOUSE(2),
		MAGNET(3),
		RAT(4),
		WITCH(5),
		SHED(6),
		BALL(7),
		EXIT_HOUSE(8);

		private static final QuestStage[] VALUES = QuestStage.values();

		private final int id;

		private final String name = this.name().charAt(0) +
			this.name().substring(1).toLowerCase().replace('_', ' ');

		QuestStage(final int id) {
			this.id = id;
		}

		private static QuestStage fromId(final int id) {
			for (final QuestStage stage : VALUES) {
				if (id == stage.id) {
					return stage;
				}
			}

			throw new IllegalArgumentException("Invalid stage id.");
		}

		public int getId() {
			return this.id;
		}

		public String getName() {
			return this.name;
		}
	}

	private enum QuestEntity implements Entity {
		BOY(240, new Tile(357, 487)),
		RAT(241, new Tile(-1, -1)),
		DOOR_MAT(255, new Tile(363, 494)),
		FRONT_DOOR(69, new Tile(363, 494)),
		LADDER_DOWN(6, new Tile(362, 491)),
		LADDER_UP(5, new Tile(362, 3323)),
		GATE(256, new Tile(363, 3325)),
		CUPBOARD(259, new Tile(362, 3328)),
		RAT_DOOR_WEST(70, new Tile(358, 495)),
		RAT_DOOR_EAST(71, new Tile(356, 495)),
		SHED_DOOR(73, new Tile(351, 492)),
		SMALL_ROOM_DOOR(72, new Tile(356, 492));

		private final int id;
		private final Tile tile;

		QuestEntity(final int id, final Tile tile) {
			this.id = id;
			this.tile = tile;
		}

		public int getId() {
			return this.id;
		}

		public Tile getTile() {
			return this.tile;
		}
	}

	private enum QuestItem implements Item {
		LEATHER_GLOVES(16, ItemType.INVENTORY),
		CHEESE(319, ItemType.INVENTORY),
		FRONT_DOOR_KEY(538, ItemType.ACQUIRED),
		MAGNET(540, ItemType.ACQUIRED),
		BALL(539, ItemType.ACQUIRED);

		private final int id;
		private final ItemType itemType;

		QuestItem(final int id, final ItemType itemType) {
			this.id = id;
			this.itemType = itemType;
		}

		public int getId() {
			return this.id;
		}

		public ItemType getItemType() {
			return this.itemType;
		}
	}
}
