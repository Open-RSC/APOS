public class AA_LostCityQuest extends AA_Quest {
	private static final String[] MENU_OPTIONS = new String[]{
		"What are you camped out here for?",
		"What makes you think it's out here",
		"If it's hidden how are you planning to find it",
		"looks like you don't know either if you're sitting around here",
		"I've been in that shed, I didn't see a city",
		"Yes, Okay I'm ready to go",
		"Well that is a risk I will have to take"
	};

	private QuestStage stage;

	private Food food = Food.NONE;

	public AA_LostCityQuest(final Extension ex) {
		super(ex);
		this.stage = QuestStage.ADVENTURER;
	}

	@Override
	public int main() {
		if (this.pathWalker.walkPath()) {
			return 0;
		}

		switch (this.stage) {
			case ADVENTURER:
				return this.talkToAdventurer();
			case LEPRECHAUN:
				return this.talkToLeprechaun();
			case BANK:
				return this.bankInDraynor();
			case TRAVEL_ENTRANA:
				return this.travelToEntrana();
			case ENTER_DUNGEON:
				return this.enterDungeon();
			case AXE:
				return this.getAxe();
			case DRAMEN_BRANCH:
				return this.getDramenBranch();
			case DRAMEN_STAFF:
				return this.makeDramenStaff();
			case ZANARIS:
				return this.enterZanaris();
			default:
				throw new QuestException("Invalid quest stage.");
		}
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("eat", 4)) {
			this.actionTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
			return;
		}

		switch (this.stage) {
			case ADVENTURER:
				if (message.equals("thankyou very much")) {
					this.setStage(QuestStage.LEPRECHAUN);
					this.menuTimeout = 0L;
				}
				break;
			case LEPRECHAUN:
				if (message.equals("The leprechaun magically disapeers")) {
					this.setStage(QuestStage.BANK);
				}
				break;
			case TRAVEL_ENTRANA:
				if (message.equals("The ship arrives at Entrana")) {
					this.setStage(QuestStage.ENTER_DUNGEON);
				}
				break;
			case ENTER_DUNGEON:
				if (message.equals("You climb down the ladder")) {
					this.setStage(QuestStage.AXE);
				}
				break;
		}
	}

	@Override
	protected void initializeQuest(final String parameters) {
		if (parameters.isEmpty()) {
			return;
		}

		this.setStage(QuestStage.fromId(Integer.parseInt(parameters)));

		switch (this.stage) {
			case TRAVEL_ENTRANA:
			case ENTER_DUNGEON:
			case AXE:
			case DRAMEN_BRANCH:
				this.food = this.getInventoryFood();
				break;
		}
	}

	@Override
	protected Quest getQuest() {
		return Quest.LOST_CITY;
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

	private int talkToAdventurer() {
		if (this.isQuestMenu()) {
			return this.answerMenu(MENU_OPTIONS);
		}

		if (System.currentTimeMillis() <= this.menuTimeout) {
			return 0;
		}

		return this.talkTo(QuestEntity.ADVENTURER);
	}

	private int talkToLeprechaun() {
		if (this.isQuestMenu()) {
			return this.answerMenu(MENU_OPTIONS);
		}

		if (System.currentTimeMillis() <= this.menuTimeout) {
			return 0;
		}

		final int[] leprechaun = this.getAllNpcById(QuestEntity.LEPRECHAUN.getId());

		if (leprechaun[0] != -1) {
			this.talkToNpc(leprechaun[0]);
			this.menuTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
			return 0;
		}

		final int[] tree = this.getObjectById(QuestEntity.TREE.getId());

		if (tree[0] != -1) {
			this.atObject(QuestEntity.TREE);
			return SLEEP_ONE_TICK;
		}

		this.walkTo(QuestEntity.TREE.getTile().getX() + 1, QuestEntity.TREE.getTile().getY());
		return SLEEP_ONE_TICK;
	}

	private int bankInDraynor() {
		if (this.isBanking()) {
			for (int index = 0; index < this.getInventoryCount(); index++) {
				final int itemId = this.getInventoryId(index);

				if (itemId == this.food.getId() || itemId == QuestItem.KNIFE.getId()) {
					continue;
				}

				final int count = this.getInventoryCount(itemId);
				this.deposit(itemId, count);
				return SLEEP_ONE_TICK;
			}

			if (!this.hasInventoryItem(QuestItem.KNIFE.getId())) {
				if (!this.hasBankItem(QuestItem.KNIFE.getId())) {
					throw new QuestException(String.format("Missing item: %s", getItemNameId(QuestItem.KNIFE.getId())));
				}

				this.withdraw(QuestItem.KNIFE.getId(), 1);
				return SLEEP_TWO_SECONDS;
			}

			if (this.food == Food.NONE) {
				final Food bankedFood = this.getBankedFood();

				if (bankedFood == Food.NONE) {
					this.setStage(QuestStage.TRAVEL_ENTRANA);
					return 0;
				}

				this.food = bankedFood;
			}

			final int invCount = this.getInventoryCount(this.food.getId());
			final int bankCount = this.bankCount(this.food.getId());
			final int withdrawCount = Math.min(bankCount + invCount, MAX_INVENTORY_SIZE - this.getInventoryCount());

			if (invCount < withdrawCount) {
				this.withdraw(this.food.getId(), withdrawCount - invCount);
				return SLEEP_ONE_TICK;
			}

			this.setStage(QuestStage.TRAVEL_ENTRANA);
			return 0;
		}

		if (this.isQuestMenu()) {
			this.answer(0);
			this.menuTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= this.menuTimeout) {
			return 0;
		}

		return this.talkTo(QuestEntity.BANKER);
	}

	private Food getBankedFood() {
		for (int i = Food.VALUES.length - 1; i >= 0; i--) {
			final Food food = Food.VALUES[i];

			if (this.bankCount(food.getId()) >= MAX_INVENTORY_SIZE) {
				return food;
			}
		}

		return Food.NONE;
	}

	private Food getInventoryFood() {
		for (int i = Food.VALUES.length - 1; i >= 0; i--) {
			final Food food = Food.VALUES[i];

			if (this.hasInventoryItem(food.getId())) {
				return food;
			}
		}

		return Food.NONE;
	}

	private int travelToEntrana() {
		if (this.isQuestMenu()) {
			return this.answerMenu(MENU_OPTIONS);
		}

		if (System.currentTimeMillis() <= this.menuTimeout) {
			return 0;
		}

		return this.talkTo(QuestEntity.MONK_PORT_SARIM);
	}

	private int enterDungeon() {
		if (this.isQuestMenu()) {
			return this.answerMenu(MENU_OPTIONS);
		}

		if (System.currentTimeMillis() <= this.menuTimeout) {
			return 0;
		}

		return this.talkTo(QuestEntity.MONK_ENTRANA);
	}

	private int getAxe() {
		if (this.hasInventoryItem(QuestItem.BRONZE_AXE.getId())) {
			this.setStage(QuestStage.DRAMEN_BRANCH);
			return 0;
		}

		if (this.food != Food.NONE &&
			this.getCurrentLevel(3) <= this.getLevel(3) - this.food.getHealAmount()) {
			final int index = this.getInventoryIndex(this.food.getId());

			if (index != -1) {
				if (this.inCombat()) {
					this.walkHere();
					return SLEEP_ONE_TICK;
				}

				return this.consumeItem(index);
			}
		}

		final int[] axe = this.getItemById(QuestItem.BRONZE_AXE.getId());

		if (axe[0] != -1) {
			if (this.inCombat()) {
				this.walkHere();
				return SLEEP_ONE_TICK;
			}

			if (this.getInventoryCount() == MAX_INVENTORY_SIZE) {
				return this.consumeItem(this.getInventoryIndex(this.food.getId()));
			}

			this.pickupItem(axe[0], axe[1], axe[2]);
			return SLEEP_ONE_TICK;
		}

		if (this.inCombat()) {
			return 0;
		}

		final int[] zombie = this.getNpcById(QuestEntity.ZOMBIE.getId());

		if (zombie[0] == -1) {
			return 0;
		}

		this.attackNpc(zombie[0]);
		return SLEEP_ONE_TICK;
	}

	private int getDramenBranch() {
		if (this.hasInventoryItem(QuestItem.DRAMEN_BRANCH.getId())) {
			this.setStage(QuestStage.DRAMEN_STAFF);
			return 0;
		}

		if (this.food != Food.NONE &&
			this.getCurrentLevel(3) <= this.getLevel(3) - this.food.getHealAmount()) {
			final int index = this.getInventoryIndex(this.food.getId());

			if (index != -1) {
				if (this.inCombat()) {
					this.walkHere();
					return SLEEP_ONE_TICK;
				}

				return this.consumeItem(index);
			}
		}

		if (this.distanceTo(QuestEntity.DRAMEN_TREE) > 3) {
			this.walkTo(QuestEntity.DRAMEN_TREE.getTile().getX(),
				QuestEntity.DRAMEN_TREE.getTile().getY() - 1);
			return SLEEP_ONE_TICK;
		}

		final int axeIndex = this.getItemIndex(QuestItem.BRONZE_AXE);

		if (!this.isItemEquipped(axeIndex)) {
			this.wearItem(axeIndex);
			return SLEEP_ONE_TICK;
		}

		final int[] treeSpirit = this.getNpcById(QuestEntity.TREE_SPIRIT.getId());

		if (treeSpirit[0] != -1) {
			if (this.distanceTo(treeSpirit[1], treeSpirit[2]) > 2) {
				this.walkTo(treeSpirit[1], treeSpirit[2]);
			} else {
				this.attackNpc(treeSpirit[0]);
			}

			return SLEEP_ONE_TICK;
		}

		if (this.inCombat()) {
			return 0;
		}

		if (this.getInventoryCount() == MAX_INVENTORY_SIZE && this.food != Food.NONE) {
			final int foodIndex = this.getInventoryIndex(this.food.getId());

			if (foodIndex != -1) {
				return this.consumeItem(foodIndex);
			}
		}

		this.atObject(QuestEntity.DRAMEN_TREE);
		return SLEEP_ONE_TICK;
	}

	private int makeDramenStaff() {
		if (this.hasInventoryItem(QuestItem.DRAMEN_STAFF.getId())) {
			this.setStage(QuestStage.ZANARIS);
			return 0;
		}

		final int knifeIndex = this.getItemIndex(QuestItem.KNIFE);

		final int branchIndex = this.getInventoryIndex(QuestItem.DRAMEN_BRANCH.getId());

		if (branchIndex == -1) {
			return 0;
		}

		this.useItemWithItem(knifeIndex, branchIndex);
		return SLEEP_ONE_TICK;
	}

	private int enterZanaris() {
		final int ZANARIS_Y_COORD = 3500;

		if (this.getY() >= ZANARIS_Y_COORD) {
			if (this.isQuestComplete()) {
				return this.completeQuest();
			}

			return 0;
		}

		if (this.isWallObject(QuestEntity.DOOR)) {
			final int staffIndex = this.getItemIndex(QuestItem.DRAMEN_STAFF);

			if (!this.isItemEquipped(staffIndex)) {
				this.wearItem(staffIndex);
				return SLEEP_ONE_TICK;
			}

			if (this.distanceTo(QuestEntity.DOOR) > 1) {
				this.walkTo(QuestEntity.DOOR);
				return SLEEP_ONE_TICK;
			}

			this.atWallObject(QuestEntity.DOOR);
			return SLEEP_FIVE_SECONDS * 2;
		}

		if (this.isWallObject(QuestEntity.MAGIC_DOOR)) {
			if (this.inCombat() || this.distanceTo(QuestEntity.MAGIC_DOOR) > 1) {
				this.walkTo(QuestEntity.MAGIC_DOOR);
				return SLEEP_ONE_TICK;
			}

			this.atWallObject(QuestEntity.MAGIC_DOOR);
			return SLEEP_FIVE_SECONDS;
		}

		final Tile shedTile = new Tile(131, 682);

		this.setPath(shedTile);
		return 0;
	}

	private enum QuestStage implements Stage {
		ADVENTURER(0),
		LEPRECHAUN(1),
		BANK(2),
		TRAVEL_ENTRANA(3),
		ENTER_DUNGEON(4),
		AXE(5),
		DRAMEN_BRANCH(6),
		DRAMEN_STAFF(7),
		ZANARIS(8);

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
		ADVENTURER(209, new Tile(179, 669)),
		TREE(237, new Tile(172, 662)),
		LEPRECHAUN(211, new Tile(174, 662)),
		BANKER(95, new Tile(220, 636)),
		MONK_PORT_SARIM(212, new Tile(270, 658)),
		MONK_ENTRANA(213, new Tile(427, 547)),
		ZOMBIE(214, new Tile(412, 3378)),
		DRAMEN_TREE(245, new Tile(412, 3402)),
		TREE_SPIRIT(216, new Tile(412, 3401)),
		MAGIC_DOOR(65, new Tile(406, 3392)),
		DOOR(66, new Tile(126, 686));

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
		KNIFE(13, ItemType.INVENTORY),
		ANY_FOOD(546, ItemType.BANKED),
		BRONZE_AXE(87, ItemType.ACQUIRED),
		DRAMEN_BRANCH(510, ItemType.ACQUIRED),
		DRAMEN_STAFF(509, ItemType.ACQUIRED);

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
