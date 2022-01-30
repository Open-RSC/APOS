import com.aposbot.Constants;

import java.text.DecimalFormat;
import java.util.Objects;

public abstract class AA_Script extends Script {
	protected static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0");

	protected static final long TIMEOUT_ONE_TICK = 650L;
	protected static final long TIMEOUT_ONE_SECOND = 1000L;
	protected static final long TIMEOUT_TWO_SECONDS = 2000L;
	protected static final long TIMEOUT_THREE_SECONDS = 3000L;
	protected static final long TIMEOUT_FIVE_SECONDS = 5000L;
	protected static final long TIMEOUT_TEN_SECONDS = 10000L;

	protected static final int SLEEP_ONE_TICK = 650;
	protected static final int SLEEP_ONE_SECOND = 1000;
	protected static final int SLEEP_TWO_SECONDS = 2000;
	protected static final int SLEEP_THREE_SECONDS = 3000;
	protected static final int SLEEP_FIVE_SECONDS = 5000;
	protected static final int SLEEP_TEN_SECONDS = 10000;

	protected static final int PAINT_OFFSET_X = 312;
	protected static final int PAINT_OFFSET_X_ALT = 185;
	protected static final int PAINT_OFFSET_Y = 48;
	protected static final int PAINT_OFFSET_Y_INCREMENT = 14;
	protected static final int PAINT_COLOR = 0xFFFFFF;

	protected static final int MAX_TRADE_SIZE = 12;

	protected static final int ITEM_ID_SLEEPING_BAG = 1263;

	private static final int[] NPC_IDS_BANKER = new int[]{95, 224, 268, 485, 540, 617};

	protected final Extension extension;

	protected CombatStyle combatStyle = CombatStyle.STRENGTH;

	private long optionMenuTimeout;

	public AA_Script(final Extension extension) {
		super(extension);
		this.extension = extension;
	}

	protected static String getElapsedSeconds(final long seconds) {
		return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
	}

	protected static String getUnitsPerHour(final double processed, final long seconds) {
		return processed == 0 ? "0" : DECIMAL_FORMAT.format((processed * 60.0 * 60.0) / seconds);
	}

	protected static String getTTL(final double processed, final int remaining, final long elapsedSeconds) {
		return processed == 0 ? "0:00:00" : getElapsedSeconds((long) (remaining * (elapsedSeconds / processed)));
	}

	@Override
	public abstract void init(final String parameters);

	@Override
	public abstract int main();

	@Override
	public void onServerMessage(final String message) {
		if (message.endsWith("moment")) {
			this.optionMenuTimeout = 0L;
		}
	}

	@Override
	public void onDeath() {
		this.setAutoLogin(false);
		this.stopScript();
		System.out.println("Oh dear, you are dead.");
	}

	@Override
	public final String toString() {
		return this.getClass().getSimpleName();
	}

	protected final boolean isDead() {
		return this.extension.isDeathScreen() ||
			this.extension.getLocalX() < 0 || this.extension.getLocalX() > 96 ||
			this.extension.getLocalY() < 0 || this.extension.getLocalY() > 96;
	}

	protected final double getTotalCombatXp() {
		int total = 0;

		for (int i = 0; i < 4; i++) {
			total += this.extension.getExperience(i);
		}

		return total;
	}

	protected final boolean isInventoryFull() {
		return this.extension.getInventorySize() == MAX_INV_SIZE;
	}

	protected final boolean isInventoryEmpty() {
		return this.extension.getInventorySize() == 0;
	}

	protected final int getInventoryEmptyCount() {
		return MAX_INV_SIZE - this.extension.getInventorySize();
	}

	protected final boolean hasInventoryItem(final int[] itemIds) {
		for (final int itemId : itemIds) {
			for (int index = 0; index < this.extension.getInventorySize(); index++) {
				if (this.extension.getInventoryId(index) == itemId) {
					return true;
				}
			}
		}

		return false;
	}

	protected final boolean isItemIdEquipped(final int itemId) {
		for (int index = 0; index < this.extension.getInventorySize(); index++) {
			if (this.extension.getInventoryId(index) == itemId) {
				return this.extension.isEquipped(index);
			}
		}

		return false;
	}

	protected final int openBank() {
		return this.openGenericInterface(NPC_IDS_BANKER);
	}

	protected final int openShop(final int... shopkeepers) {
		return this.openGenericInterface(shopkeepers);
	}

	protected final int sleep() {
		final int index = this.getInventoryIndex(ITEM_ID_SLEEPING_BAG);

		if (index == -1) {
			this.setAutoLogin(false);
			this.stopScript();
			System.out.println("Sleeping bag missing from inventory.");
		}

		this.useItem(index);
		return SLEEP_ONE_SECOND;
	}

	protected final int exit(final String reason) {
		this.setAutoLogin(false);
		this.stopScript();
		System.err.println(reason);
		return 0;
	}

	protected final int getBaseHits() {
		return this.extension.getBaseLevel(3);
	}

	protected final int getCurrentHits() {
		return this.extension.getCurrentLevel(3);
	}

	protected final int getCurrentHits(final java.lang.Object character) {
		return ((ta) character).B;
	}

	protected final String getName(final java.lang.Object character) {
		final String name = ((ta) character).c;

		if (name == null) {
			return null;
		}

		return name.replace((char) 160, ' ');
	}

	protected final int getWaypointX(final java.lang.Object character) {
		return ((ta) character).i;
	}

	protected final int getWaypointY(final java.lang.Object character) {
		return ((ta) character).K;
	}

	protected final void useObject1(final int x, final int y) {
		this.extension.createPacket(Constants.OP_OBJECT_ACTION1);
		this.extension.put2(x);
		this.extension.put2(y);
		this.extension.finishPacket();
	}

	protected final void useObject2(final int x, final int y) {
		this.extension.createPacket(Constants.OP_OBJECT_ACTION2);
		this.extension.put2(x);
		this.extension.put2(y);
		this.extension.finishPacket();
	}

	protected final void useWithObject(final int inventoryIndex, final int x, final int y) {
		this.extension.createPacket(Constants.OP_OBJECT_USEWITH);
		this.extension.put2(x);
		this.extension.put2(y);
		this.extension.put2(inventoryIndex);
		this.extension.finishPacket();
	}

	protected final void takeGroundItem(final int itemId, final int x, final int y) {
		this.extension.createPacket(Constants.OP_GITEM_TAKE);
		this.extension.put2(x);
		this.extension.put2(y);
		this.extension.put2(itemId);
		this.extension.finishPacket();
	}

	protected final boolean hasTradeItem(final int[] itemIds) {
		for (final int itemId : itemIds) {
			for (int index = 0; index < this.extension.getLocalTradeItemCount(); index++) {
				if (this.extension.getLocalTradeItemId(index) == itemId) {
					return true;
				}
			}
		}

		return false;
	}

	protected final int getTradeItemIdCount(final int itemId) {
		int count = 0;

		for (int index = 0; index < this.extension.getLocalTradeItemCount(); index++) {
			if (this.extension.getLocalTradeItemId(index) == itemId) {
				count += this.extension.getLocalTradeItemStack(index);
			}
		}

		return count;
	}

	protected final int getTradeItemIndex(final int itemId) {
		for (int index = 0; index < this.extension.getLocalTradeItemCount(); index++) {
			if (this.extension.getLocalTradeItemId(index) == itemId) {
				return index;
			}
		}

		return -1;
	}

	protected final void offerTradeItemId(final int itemId, final int amount) {
		final int inventoryIndex = this.getInventoryIndex(itemId);

		if (inventoryIndex == -1) {
			return;
		}

		this.extension.offerItemTrade(inventoryIndex, amount);
	}

	protected final void removeTradeItem(final int tradeIndex, final int amount) {
		this.extension.c(amount, (byte) 124, tradeIndex);
	}

	protected final void removeTradeItemId(final int itemId, final int amount) {
		final int tradeIndex = this.getTradeItemIndex(itemId);

		if (tradeIndex == -1) {
			return;
		}

		this.removeTradeItem(tradeIndex, amount);
	}

	private int openGenericInterface(final int[] npcs) {
		if (this.extension.isDialogVisible()) {
			this.extension.createPacket(Constants.OP_DIALOG_ANSWER);
			this.extension.put1(0);
			this.extension.finishPacket();
			this.extension.setDialogVisible(false);
			this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		if (System.currentTimeMillis() <= this.optionMenuTimeout) {
			return 0;
		}

		final int[] npc = this.getNpcByIdNotTalk(npcs);

		if (npc[0] == -1) {
			return 0;
		}

		if (this.distanceTo(npc[1], npc[2]) > 2) {
			this.extension.walkDirectly(npc[1] - this.extension.getAreaX(),
				npc[2] - this.extension.getAreaY(), false);
			this.extension.setActionInd(24);
			return SLEEP_ONE_TICK;
		}

		this.talkToNpc(npc[0]);
		this.optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		return 0;
	}

	protected enum CombatStyle {
		CONTROLLED(0),
		STRENGTH(1),
		ATTACK(2),
		DEFENSE(3);

		private final int index;

		CombatStyle(final int index) {
			this.index = index;
		}

		@Override
		public String toString() {
			return this.name().charAt(0) + this.name().substring(1).toLowerCase();
		}

		public int getIndex() {
			return this.index;
		}
	}

	protected enum Skill {
		ATTACK(0),
		DEFENSE(1),
		STRENGTH(2),
		HITS(3),
		RANGED(4),
		PRAYER(5),
		MAGIC(6),
		COOKING(7),
		WOODCUT(8),
		FLETCHING(9),
		FISHING(10),
		FIREMAKING(11),
		CRAFTING(12),
		SMITHING(13),
		MINING(14),
		HERBLAW(15),
		AGILITY(16),
		THIEVING(17);

		private final int index;

		Skill(final int index) {
			this.index = index;
		}

		@Override
		public String toString() {
			return this.name().charAt(0) + this.name().substring(1).toLowerCase();
		}

		public int getIndex() {
			return this.index;
		}
	}

	protected enum Food {
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

		private final int id;
		private final int healAmount;
		private final String name;

		Food(final int id, final int healAmount, final String name) {
			this.id = id;
			this.healAmount = healAmount;
			this.name = name;
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
	}

	protected interface RSObject {
		int getId();

		Coordinate getCoordinate();
	}

	protected interface RSArea {
		Coordinate getLowerBoundingCoordinate();

		Coordinate getUpperBoundingCoordinate();

		default boolean contains(final int x, final int y) {
			return x >= this.getLowerBoundingCoordinate().getX() && x <= this.getUpperBoundingCoordinate().getX() &&
				y >= this.getLowerBoundingCoordinate().getY() && y <= this.getUpperBoundingCoordinate().getY();
		}
	}

	protected static final class Coordinate {
		private int x;

		private int y;

		public Coordinate(final int x, final int y) {
			this.x = x;
			this.y = y;
		}

		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof Coordinate)) {
				return false;
			}
			final Coordinate other = (Coordinate) o;
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
			return "AA_Script.Coordinate(x=" + this.getX() + ", y=" + this.getY() + ")";
		}

		public void set(final int x, final int y) {
			this.x = x;
			this.y = y;
		}

		public int getX() {
			return this.x;
		}

		public void setX(final int x) {
			this.x = x;
		}

		public int getY() {
			return this.y;
		}

		public void setY(final int y) {
			this.y = y;
		}
	}

	protected static final class Spawn implements Comparable<Spawn> {
		private final Coordinate coordinate;

		private long timestamp;

		public Spawn(final Coordinate coordinate, final long timestamp) {
			this.coordinate = coordinate;
			this.timestamp = timestamp;
		}

		@Override
		public int compareTo(final Spawn spawn) {
			return Long.compare(this.timestamp, spawn.timestamp);
		}

		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof Spawn)) {
				return false;
			}
			final Spawn other = (Spawn) o;
			final Object this$coordinate = this.getCoordinate();
			final Object other$coordinate = other.getCoordinate();
			if (!Objects.equals(this$coordinate, other$coordinate)) {
				return false;
			}
			return this.getTimestamp() == other.getTimestamp();
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $coordinate = this.getCoordinate();
			result = result * PRIME + ($coordinate == null ? 43 : $coordinate.hashCode());
			final long $timestamp = this.getTimestamp();
			result = result * PRIME + (int) ($timestamp >>> 32 ^ $timestamp);
			return result;
		}

		public String toString() {
			return "AA_Script.Spawn(coordinate=" + this.getCoordinate() + ", timestamp=" + this.getTimestamp() + ")";
		}

		public Coordinate getCoordinate() {
			return this.coordinate;
		}

		public long getTimestamp() {
			return this.timestamp;
		}

		public void setTimestamp(final long timestamp) {
			this.timestamp = timestamp;
		}
	}
}
