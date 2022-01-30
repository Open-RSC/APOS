import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public abstract class AA_Quest extends Script {
	protected static final long
		TIMEOUT_ONE_TICK = 650L,
		TIMEOUT_TWO_SECONDS = 2000L,
		TIMEOUT_THREE_SECONDS = 3000L,
		TIMEOUT_TEN_SECONDS = 10000L;

	protected static final int
		SLEEP_ONE_TICK = 650,
		SLEEP_ONE_SECOND = 1000,
		SLEEP_TWO_SECONDS = 2000,
		SLEEP_FIVE_SECONDS = 5000;

	protected static final int
		PAINT_OFFSET_X = 313,
		PAINT_OFFSET_Y = 48,
		PAINT_OFFSET_Y_INCREMENT = 14,
		PAINT_COLOR = 0xFFFFFF;

	protected static final int MAX_INVENTORY_SIZE = 30;

	private final Extension ex;

	protected Instant startTime;
	protected PathWalker pathWalker;

	protected long actionTimeout, menuTimeout;

	public AA_Quest(final Extension ex) {
		super(ex);
		this.ex = ex;
	}

	private static String getElapsedSeconds(final long seconds) {
		return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
	}

	@Override
	public final void init(final String parameters) {
		if (this.isQuestComplete()) {
			throw new QuestException("You have already completed this quest.");
		}

		final int requiredQuestPoints = this.getQuest().getRequiredQuestPoints();
		final int questCount = this.ex.ii;

		if (questCount < requiredQuestPoints) {
			throw new QuestException(String.format("Missing quest points: %d/%d", questCount,
				requiredQuestPoints));
		}

		for (final String requiredQuest : this.getQuest().getRequiredQuests()) {
			final Quest quest = Quest.valueOf(requiredQuest);

			if (!this.isQuestComplete(quest.getId())) {
				throw new QuestException("Missing quest: " + this.getQuestName(quest.getId()));
			}
		}

		for (final Quest.SkillReq skillReq : this.getQuest().getRequiredSkills()) {
			if (this.getLevel(skillReq.getSkill().getId()) < skillReq.getLevel()) {
				throw new QuestException(String.format("Missing skill: L%d %s", skillReq.getLevel(),
					skillReq.getSkill()));
			}
		}

		System.out.printf("[%s] Items required:%n", this);

		for (final Item item : this.getItems()) {
			switch (item.getItemType()) {
				case INVENTORY:
				case BANKED:
					System.out.printf("- %s (%s)%n", getItemNameId(item.getId()), item.getItemType());
					break;
				default:
					break;
			}
		}

		this.pathWalker = new PathWalker(this.ex);
		this.pathWalker.init(null);

		this.startTime = Instant.now();

		this.initializeQuest(parameters);
	}

	@Override
	public abstract int main();

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Chomp's Questor", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@gre@Quest: @whi@%s", this.getQuest().getName()),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@cya@Stage: @whi@%s", this.getStage().getName()),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
	}

	@Override
	public abstract void onServerMessage(final String message);

	@Override
	public void onDeath() {
		throw new QuestException("Oh dear, you are dead.");
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	protected abstract void initializeQuest(final String parameters);

	protected abstract Quest getQuest();

	protected abstract Stage getStage();

	protected abstract Item[] getItems();

	protected final boolean isQuestComplete() {
		return this.isQuestComplete(this.getQuest().getId());
	}

	protected final int completeQuest() {
		System.out.printf("Quest completed: %s%n", this.getQuest().getName());
		this.ex.displayMessage(String.format("@gre@Quest completed: @mag@%s", this.getQuest().getName()));
		this.stopScript();
		return 0;
	}

	protected final int getItemIndex(final Item item) throws QuestException {
		final int index = this.getInventoryIndex(item.getId());

		if (index == -1) {
			throw new QuestException(String.format("Missing item: %s", getItemNameId(item.getId())));
		}

		return index;
	}

	protected final int consumeItem(final int inventoryIndex) {
		if (System.currentTimeMillis() <= this.actionTimeout) {
			return 0;
		}

		this.useItem(inventoryIndex);
		this.actionTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	protected final int answerMenu(final String[] menuOptions) {
		int index;

		for (final String menuOption : menuOptions) {
			if ((index = this.getMenuIndex(menuOption)) != -1) {
				this.answer(index);
				this.menuTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
				return 0;
			}
		}

		return SLEEP_ONE_TICK;
	}

	protected final int talkTo(final Entity entity) {
		final int[] npc = this.getAllNpcById(entity.getId());

		if (npc[0] != -1) {
			this.talkToNpc(npc[0]);
			this.menuTimeout = System.currentTimeMillis() + TIMEOUT_TEN_SECONDS;
		} else {
			this.setPath(entity.getTile());
		}

		return 0;
	}

	protected final int distanceTo(final Entity entity) {
		return this.distanceTo(entity.getTile().getX(), entity.getTile().getY());
	}

	protected final boolean isAtEntity(final Entity entity) {
		return this.getX() == entity.getTile().getX() && this.getY() == entity.getTile().getY();
	}

	protected final boolean isObject(final Entity entity) {
		return this.getObjectIdFromCoords(entity.getTile().getX(), entity.getTile().getY()) == entity.getId();
	}

	protected final boolean isWallObject(final Entity entity) {
		return this.getWallObjectIdFromCoords(entity.getTile().getX(), entity.getTile().getY()) == entity.getId();
	}

	protected final void atObject(final Entity entity) {
		this.atObject(entity.getTile().getX(), entity.getTile().getY());
	}

	protected final void atWallObject(final Entity entity) {
		this.atWallObject(entity.getTile().getX(), entity.getTile().getY());
	}

	protected final void walkTo(final Entity entity) {
		this.walkTo(entity.getTile().getX(), entity.getTile().getY());
	}

	protected final void walkHere() {
		this.walkTo(this.getX(), this.getY());
	}

	protected final void setPath(final Tile tile) {
		final int x = this.getX();
		final int y = this.getY();

		final int maxAttempts = 20;

		PathWalker.Path path;

		int dx, dy, attempts = 0;

		do {
			dx = random(-3, 3);
			dy = random(-3, 3);
			path = this.pathWalker.calcPath(x + dx, y + dy, tile.getX(), tile.getY());
			attempts++;
		} while (path == null && attempts <= maxAttempts);

		if (path == null) {
			throw new QuestException(String.format("Failed to calculate path: (%d,%d) -> (%d,%d)",
				x, y, tile.getX(), tile.getY()));
		}

		this.pathWalker.setPath(path);
	}

	protected enum Quest implements Identifiable {
		BLACK_KNIGHTS_FORTRESS,
		COOKS_ASSISTANT,
		DEMON_SLAYER,
		DORICS_QUEST,
		THE_RESTLESS_GHOST,
		GOBLIN_DIPLOMACY,
		ERNEST_THE_CHICKEN,
		IMP_CATCHER,
		PIRATES_TREASURE,
		PRINCE_ALI_RESCUE,
		ROMEO_N_JULIET,
		SHEEP_SHEARER,
		SHIELD_OF_ARRAV,
		THE_KNIGHTS_SWORD,
		VAMPIRE_SLAYER,
		WITCHS_POTION,
		DRAGON_SLAYER,
		WITCHS_HOUSE,
		LOST_CITY(new SkillReq(Skill.CRAFTING, 31), new SkillReq(Skill.WOODCUT, 36)),
		HEROS_QUEST(
			55,
			new String[]{"SHIELD_OF_ARRAV", "DRAGON_SLAYER", "MERLINS_CRYSTAL", "LOST_CITY"},
			new SkillReq(Skill.COOKING, 53),
			new SkillReq(Skill.FIREMAKING, 53),
			new SkillReq(Skill.HERBLAW, 25),
			new SkillReq(Skill.MINING, 50)
		),
		DRUIDIC_RITUAL,
		MERLINS_CRYSTAL,
		SCORPION_CATCHER(new SkillReq(Skill.PRAYER, 31)),
		FAMILY_CREST(
			new SkillReq(Skill.MINING, 40),
			new SkillReq(Skill.SMITHING, 40),
			new SkillReq(Skill.CRAFTING, 40),
			new SkillReq(Skill.MAGIC, 59)
		),
		TRIBAL_TOTEM(new SkillReq(Skill.THIEVING, 21)),
		FISHING_CONTEST(new SkillReq(Skill.FISHING, 10)),
		MONKS_FRIEND,
		TEMPLE_OF_IKOV(new SkillReq(Skill.THIEVING, 42), new SkillReq(Skill.RANGED, 35)
		),
		CLOCK_TOWER,
		THE_HOLY_GRAIL(new String[]{"MERLINS_CRYSTAL"}, new SkillReq(Skill.ATTACK, 20)),
		FIGHT_ARENA,
		TREE_GNOME_VILLAGE,
		THE_HAZEEL_CULT,
		SHEEP_HERDER,
		PLAGUE_CITY,
		SEA_SLUG(new SkillReq(Skill.FIREMAKING, 30)),
		WATERFALL_QUEST,
		BIOHAZARD("PLAGUE_CITY"),
		JUNGLE_POTION("DRUIDIC_RITUAL"),
		GRAND_TREE(new SkillReq(Skill.AGILITY, 25)),
		SHILO_VILLAGE(
			new String[]{"JUNGLE_POTION"},
			new SkillReq(Skill.SMITHING, 4),
			new SkillReq(Skill.CRAFTING, 20),
			new SkillReq(Skill.AGILITY, 32)
		),
		UNDERGROUND_PASS(new String[]{"BIOHAZARD"}, new SkillReq(Skill.RANGED, 25)),
		OBSERVATORY_QUEST,
		TOURIST_TRAP(new SkillReq(Skill.FLETCHING, 10), new SkillReq(Skill.SMITHING, 20)),
		WATCHTOWER(
			new SkillReq(Skill.HERBLAW, 14),
			new SkillReq(Skill.MAGIC, 14),
			new SkillReq(Skill.THIEVING, 15),
			new SkillReq(Skill.AGILITY, 30),
			new SkillReq(Skill.MINING, 40)
		),
		DWARF_CANNON,
		MURDER_MYSTERY,
		DIGSITE(
			new String[]{"DRUIDIC_RITUAL"},
			new SkillReq(Skill.THIEVING, 25),
			new SkillReq(Skill.AGILITY, 10),
			new SkillReq(Skill.HERBLAW, 10)
		),
		GERTRUDES_CAT,
		LEGENDS_QUEST(
			107,
			new String[]{"HEROS_QUEST", "FAMILY_CREST", "WATERFALL_QUEST", "SHILO_VILLAGE", "UNDERGROUND_PASS"},
			new SkillReq(Skill.AGILITY, 50),
			new SkillReq(Skill.CRAFTING, 50),
			new SkillReq(Skill.HERBLAW, 45),
			new SkillReq(Skill.MAGIC, 56),
			new SkillReq(Skill.MINING, 52),
			new SkillReq(Skill.PRAYER, 42),
			new SkillReq(Skill.SMITHING, 50),
			new SkillReq(Skill.STRENGTH, 50),
			new SkillReq(Skill.THIEVING, 50),
			new SkillReq(Skill.WOODCUT, 50)
		);

		private final int id;
		private final String name;

		private final int requiredQuestPoints;
		private final String[] requiredQuests;
		private final SkillReq[] requiredSkills;

		Quest(final int requiredQuestPoints, final String[] requiredQuests, final SkillReq... requiredSkills) {
			this.id = this.ordinal();
			this.name = this.name().charAt(0) +
				this.name().substring(1).toLowerCase().replace('_', ' ');

			this.requiredQuestPoints = requiredQuestPoints;
			this.requiredQuests = requiredQuests;
			this.requiredSkills = requiredSkills;
		}

		Quest(final String[] requiredQuests, final SkillReq... requiredSkills) {
			this(0, requiredQuests, requiredSkills);
		}

		Quest(final String... requiredQuests) {
			this(0, requiredQuests);
		}

		Quest(final SkillReq... requiredSkills) {
			this(0, new String[0], requiredSkills);
		}

		Quest() {
			this(0, new String[0]);
		}

		public int getId() {
			return this.id;
		}

		public String getName() {
			return this.name;
		}

		public int getRequiredQuestPoints() {
			return this.requiredQuestPoints;
		}

		public String[] getRequiredQuests() {
			return this.requiredQuests;
		}

		public SkillReq[] getRequiredSkills() {
			return this.requiredSkills;
		}

		private static final class SkillReq {
			private final Skill skill;
			private final int level;

			public SkillReq(final Skill skill, final int level) {
				this.skill = skill;
				this.level = level;
			}

			public Skill getSkill() {
				return this.skill;
			}

			public int getLevel() {
				return this.level;
			}

			public boolean equals(final Object o) {
				if (o == this) {
					return true;
				}
				if (!(o instanceof SkillReq)) {
					return false;
				}
				final SkillReq other = (SkillReq) o;
				final Object this$skill = this.getSkill();
				final Object other$skill = other.getSkill();
				if (!Objects.equals(this$skill, other$skill)) {
					return false;
				}
				return this.getLevel() == other.getLevel();
			}

			public int hashCode() {
				final int PRIME = 59;
				int result = 1;
				final Object $skill = this.getSkill();
				result = result * PRIME + ($skill == null ? 43 : $skill.hashCode());
				result = result * PRIME + this.getLevel();
				return result;
			}

			public String toString() {
				return "AA_Quest.Quest.SkillReq(skill=" + this.getSkill() + ", level=" + this.getLevel() + ")";
			}
		}
	}

	private enum Skill implements Identifiable {
		ATTACK,
		DEFENSE,
		STRENGTH,
		HITS,
		RANGED,
		PRAYER,
		MAGIC,
		COOKING,
		WOODCUT,
		FLETCHING,
		FISHING,
		FIREMAKING,
		CRAFTING,
		SMITHING,
		MINING,
		HERBLAW,
		AGILITY,
		THIEVING;

		private final int id;
		private final String name;

		Skill() {
			this.id = this.ordinal();
			this.name = this.name().charAt(0) + this.name().substring(1).toLowerCase();
		}

		@Override
		public String toString() {
			return this.name;
		}

		public int getId() {
			return this.id;
		}

		public String getName() {
			return this.name;
		}
	}

	protected enum Food implements Identifiable {
		NONE(-1, 0, "None"),
		SHRIMP(350, 3, "Shrimp"),
		ANCHOVIES(352, 1, "Anchovies"),
		SARDINE(355, 4, "Sardine"),
		HERRING(362, 5, "Herring"),
		GIANT_CARP(718, 6, "Giant Carp"),
		MACKEREL(553, 6, "Mackerel"),
		TROUT(359, 7, "Trout"),
		COD(551, 7, "Cod"),
		PIKE(364, 8, "Pike"),
		SALMON(357, 9, "Salmon"),
		TUNA(367, 10, "Tuna"),
		LOBSTER(373, 12, "Lobster"),
		BASS(555, 13, "Bass"),
		SWORDFISH(370, 14, "Swordfish"),
		SHARK(546, 20, "Shark"),
		SEA_TURTLE(1193, 20, "Sea Turtle"),
		MANTA_RAY(1191, 20, "Manta Ray");

		public static final Food[] VALUES = Food.values();

		private final int id;
		private final int healAmount;
		private final String name;

		Food(final int id, final int healAmount, final String name) {
			this.id = id;
			this.healAmount = healAmount;
			this.name = name;
		}

		public static Food fromId(final int id) {
			for (final Food food : VALUES) {
				if (id == food.id) {
					return food;
				}
			}

			return Food.NONE;
		}

		@Override
		public String toString() {
			return this.name;
		}

		public int getId() {
			return this.id;
		}

		public int getHealAmount() {
			return this.healAmount;
		}

		public String getName() {
			return this.name;
		}
	}

	private interface Identifiable {
		int getId();
	}

	protected interface Stage extends Identifiable {
		String getName();
	}

	protected interface Entity extends Identifiable {
		Tile getTile();
	}

	protected interface Item extends Identifiable {
		ItemType getItemType();

		enum ItemType {
			INVENTORY,
			BANKED,
			ACQUIRED;

			private final String description = this.name().toLowerCase(Locale.ROOT);

			@Override
			public String toString() {
				return this.description;
			}
		}
	}

	protected static final class Tile {
		private final int x;
		private final int y;

		public Tile(final int x, final int y) {
			this.x = x;
			this.y = y;
		}

		public int getX() {
			return this.x;
		}

		public int getY() {
			return this.y;
		}

		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof Tile)) {
				return false;
			}
			final Tile other = (Tile) o;
			if (this.getX() != other.getX()) {
				return false;
			}
			return this.getY() == other.getY();
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			result = result * PRIME + this.getX();
			result = result * PRIME + this.getY();
			return result;
		}

		public String toString() {
			return "AA_Quest.Tile(x=" + this.getX() + ", y=" + this.getY() + ")";
		}
	}

	protected static final class QuestException extends RuntimeException {
		public QuestException(final String errorMessage) {
			this(errorMessage, null);
		}

		public QuestException(final String errorMessage, final Throwable throwable) {
			super(errorMessage, throwable);
		}
	}
}
