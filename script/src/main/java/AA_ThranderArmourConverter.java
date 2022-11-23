/**
 * Converts plate mail legs and skirts at Thrander.
 * Start script at Varrock east bank.
 * <p>
 * Required parameter: <armour> <# to convert>
 * <p>
 * Armour:
 * BRONZE_LEGS
 * BRONZE_SKIRT
 * IRON_LEGS
 * IRON_SKIRT
 * STEEL_LEGS
 * STEEL_SKIRT
 * BLACK_LEGS
 * BLACK_SKIRT
 * MITHRIL_LEGS
 * MITHRIL_SKIRT
 * ADAMANTITE_LEGS
 * ADAMANTITE_SKIRT
 * RUNITE_LEGS
 * RUNITE_SKIRT
 * <p>
 * Notes:
 * e.g. parameter "RUNITE_LEGS 10" will convert 10 legs into skirts
 * <p>
 *
 * @author Pomch
 */
public class AA_ThranderArmourConverter extends AA_Script {
	private static final int NPC_ID_THRANDER = 160;

	private Item item;
	private long timeout;
	private long startTime;
	private int limit;
	private int converted;
	private int remaining;
	private boolean banking;

	public AA_ThranderArmourConverter(final Extension bot) {
		super(bot);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) {
			throw new IllegalArgumentException("Missing parameter. Format: <BRONZE_LEGS|BRONZE_SKIRT|STEEL_LEGS|...> <# amount>");
		}

		try {
			final String[] args = parameters.split(" ");
			item = Item.valueOf(args[0].toUpperCase());
			remaining = limit = Integer.parseInt(args[1]);
		} catch (final Exception e) {
			throw new IllegalArgumentException("Invalid parameter. Format: <BRONZE_LEGS|BRONZE_SKIRT|STEEL_LEGS|...> <# amount>", e);
		}

		banking = isInventoryEmpty() || getInventoryItemId(0) != item.getId();
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		return banking ? bank() : convert();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("Thrander gives")) {
			timeout = 0L;
			converted++;
			remaining = Math.max(0, limit - converted);
			if (remaining == 0) {
				banking = true;
			}
		} else {
			super.onServerMessage(message);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@mag@Thrander Armour Converter", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Converted: @whi@%s @cya@(@whi@%s per@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(converted), toUnitsPerHour(converted, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Remaining: @whi@%d", remaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(converted, remaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private int bank() {
		if (isBankOpen()) {
			if (!isInventoryEmpty()) {
				deposit(getInventoryItemId(0), getInventoryItemCount());
			}

			if (remaining == 0) return exit("Finished converting armor.");
			final int bankCount = getBankItemIdCount(item.getId());
			if (bankCount == 0) return exit("Out of items to convert.");

			withdraw(item.getId(), Math.min(MAX_INVENTORY_SIZE, Math.min(limit, bankCount)));
			closeBank();
			banking = false;
		}

		if (ScriptArea.BANK.contains(getPlayerX(), getPlayerY())) {
			return openBank();
		}

		if (ScriptArea.SHOP.contains(getPlayerX(), getPlayerY())) {
			final Coordinate door = ScriptObject.SHOP_DOOR.getCoordinate();
			if (getWallObjectId(door.getX(), door.getY()) == ScriptObject.SHOP_DOOR.getId()) {
				atWallObject(door.getX(), door.getY());
				return SLEEP_ONE_SECOND;
			}
		}

		final Coordinate doors = ScriptObject.BANK_DOORS.getCoordinate();

		if (getObjectId(doors.getX(), doors.getY()) == ScriptObject.BANK_DOORS.getId()) {
			atObject(doors.getX(), doors.getY());
			return SLEEP_ONE_SECOND;
		}

		return openBank();
	}

	private int convert() {
		if (ScriptArea.SHOP.contains(getPlayerX(), getPlayerY())) {
			if (System.currentTimeMillis() <= timeout) return 0;
			final Object thrander = getNearestNpcNotTalking(NPC_ID_THRANDER);
			if (thrander == null) return 0;
			if (isInventoryEmpty() || getInventoryItemId(0) != item.getId()) {
				banking = true;
				return 0;
			}
			useItemOnNpc(0, thrander);
			timeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (ScriptArea.BANK.contains(getPlayerX(), getPlayerY())) {
			final Coordinate doors = ScriptObject.BANK_DOORS.getCoordinate();
			if (getObjectId(doors.getX(), doors.getY()) == ScriptObject.BANK_DOORS.getId()) {
				atObject(doors.getX(), doors.getY());
				return SLEEP_ONE_SECOND;
			}
		}

		final Coordinate door = ScriptObject.SHOP_DOOR.getCoordinate();
		if (getWallObjectId(door.getX(), door.getY()) == ScriptObject.SHOP_DOOR.getId()) {
			atWallObject(door.getX(), door.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(door.getX(), door.getY());
		return SLEEP_ONE_TICK;
	}

	private enum ScriptArea implements RSArea {
		BANK(new Coordinate(98, 510), new Coordinate(106, 515)),
		SHOP(new Coordinate(102, 518), new Coordinate(106, 521));

		private final Coordinate lowerBoundingCoordinate;
		private final Coordinate upperBoundingCoordinate;

		ScriptArea(Coordinate lowerBoundingCoordinate, Coordinate upperBoundingCoordinate) {
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

	private enum ScriptObject implements RSObject {
		SHOP_DOOR(2, new Coordinate(104, 518)),
		BANK_DOORS(64, new Coordinate(102, 509));

		private final int id;
		private final Coordinate coordinate;

		ScriptObject(int id, Coordinate coordinate) {
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

	private enum Item {
		BRONZE_LEGS(206),
		BRONZE_SKIRT(214),
		IRON_LEGS(9),
		IRON_SKIRT(215),
		STEEL_LEGS(121),
		STEEL_SKIRT(225),
		BLACK_LEGS(248),
		BLACK_SKIRT(434),
		MITHRIL_LEGS(122),
		MITHRIL_SKIRT(226),
		ADAMANTITE_LEGS(123),
		ADAMANTITE_SKIRT(227),
		RUNITE_LEGS(402),
		RUNITE_SKIRT(406);

		private final int id;

		Item(int id) {
			this.id = id;
		}

		public int getId() {
			return this.id;
		}
	}
}
