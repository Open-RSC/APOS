/**
 * Smiths items at the Varrock West anvils.
 * <p>
 * Requirements:
 * Start at Varrock West with sleeping bag and hammer in inventory.
 * <p>
 * Required Parameters:
 * -b,--bar <bronze|iron|steel|mithril|adamantite|runite>
 * -p,--product <plate_mail_body|kite_shield|two_handed_sword|...>
 * <p>
 * Products:
 * dagger
 * throwing_knife
 * short_sword
 * long_sword
 * scimitar
 * two_handed_sword
 * hatchet
 * battle_axe
 * mace
 * medium_helmet
 * large_helmet
 * square_shield
 * kite_shield
 * chain_mail_body
 * plate_mail_body
 * plate_mail_legs
 * plated_skirt
 * arrow_heads
 * bronze_wire
 * nails
 * <p>
 *
 * @Author Chomp
 */
public class AA_VarrockWestSmither extends AA_Script {
	private static final long HAMMERING_DELAY = 600L; // +- based on latency

	private static final int ITEM_ID_HAMMER = 168;
	private static final int INITIAL_INVENTORY_SIZE = 2;
	private static final int MAXIMUM_FATIGUE = 99;

	private Bar bar;
	private Product product;
	private long startTime;

	private double initialSmithingXp;

	private long bankDepositTimeout;
	private long bankWithdrawTimeout;
	private long doorOpenTimeout;
	private long optionMenuTimeout;

	private int playerX;
	private int playerY;

	private int barWithdrawCount;
	private int barsSmithed;
	private int barsRemaining;

	public AA_VarrockWestSmither(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) printInstructions();

		final String[] args = parameters.split(" ");

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
				case "-b":
				case "--bar":
					bar = Bar.valueOf(args[++i].toUpperCase());
					break;
				case "-p":
				case "--product":
					product = Product.valueOf(args[++i].toUpperCase());
					break;
				default:
					throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
			}
		}

		if (bar == null) {
			throw new IllegalArgumentException("Missing bar type parameter.");
		}

		if (product == null) {
			throw new IllegalArgumentException("Missing product parameter.");
		}

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		if (!hasInventoryItem(ITEM_ID_HAMMER)) {
			throw new IllegalStateException("Hammer missing from inventory.");
		}

		final int inventorySize = MAX_INV_SIZE - INITIAL_INVENTORY_SIZE;

		barWithdrawCount = inventorySize - (inventorySize % product.getBarCount());
		initialSmithingXp = getAccurateXpForLevel(Skill.SMITHING.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getX();
		playerY = getY();

		if (getInventoryCount(bar.id) < product.barCount) {
			return bank();
		}

		return smith();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("hammer", 4)) {
			barsSmithed += product.barCount;
			if (barsRemaining >= product.barCount) {
				barsRemaining -= product.barCount;
			}
			optionMenuTimeout = System.currentTimeMillis() + HAMMERING_DELAY;
		} else if (message.endsWith("shut") || message.endsWith("open")) {
			doorOpenTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else {
			super.onServerMessage(message);
		}
	}

	private int bank() {
		if (Area.BANK.contains(playerX, playerY)) {
			if (!isBanking()) {
				return openBank();
			}

			if (System.currentTimeMillis() <= bankWithdrawTimeout) {
				return 0;
			}

			if (getInventoryCount() == INITIAL_INVENTORY_SIZE) {
				if (bankCount(bar.id) < product.barCount) {
					return exit("Out of bars.");
				}

				barsRemaining = bankCount(bar.id);
				withdraw(bar.id, barWithdrawCount);
				bankWithdrawTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= bankDepositTimeout) {
				return 0;
			}

			final int itemId = getInventoryId(INITIAL_INVENTORY_SIZE);
			final int itemCount = getInventoryCount(itemId);
			deposit(itemId, itemCount);
			bankDepositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (Area.ANVIL_HOUSE.contains(playerX, playerY)) {
			if (getWallObjectIdFromCoords(Object.ANVIL_DOOR.coordinate.getX(), Object.ANVIL_DOOR.coordinate.getY()) == Object.ANVIL_DOOR.id) {
				if (System.currentTimeMillis() <= doorOpenTimeout) {
					return 0;
				}

				atWallObject(Object.ANVIL_DOOR.coordinate.getX(), Object.ANVIL_DOOR.coordinate.getY());
				doorOpenTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
				return 0;
			}

			walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
			if (System.currentTimeMillis() <= doorOpenTimeout) {
				return 0;
			}

			atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
			doorOpenTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
			return 0;
		}

		walkTo(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY() - 1);
		return SLEEP_ONE_TICK;
	}

	private int smith() {
		if (Area.ANVIL_HOUSE.contains(playerX, playerY)) {
			if (getFatigue() >= MAXIMUM_FATIGUE) {
				return sleep();
			}

			if (isQuestMenu()) {
				int index;

				for (final String menuOption : product.getMenuOptions()) {
					index = getMenuIndex(menuOption);

					if (index != -1) {
						answer(index);
						optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
						return 0;
					}
				}

				return SLEEP_ONE_SECOND;
			}

			if (System.currentTimeMillis() <= optionMenuTimeout) {
				return 0;
			}

			if (playerX != Object.ANVIL.coordinate.getX() ||
				playerY != Object.ANVIL.coordinate.getY() - 1) {
				walkTo(Object.ANVIL.coordinate.getX(), Object.ANVIL.coordinate.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			bot.displayMessage("@gre@Hammering...");
			final Coordinate anvil = Object.ANVIL.getCoordinate();
			useWithObject(INITIAL_INVENTORY_SIZE, anvil.getX(), anvil.getY());
			optionMenuTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (Area.BANK.contains(playerX, playerY)) {
			if (getObjectIdFromCoords(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY()) == Object.BANK_DOORS.id) {
				if (System.currentTimeMillis() <= doorOpenTimeout) {
					return 0;
				}

				atObject(Object.BANK_DOORS.coordinate.getX(), Object.BANK_DOORS.coordinate.getY());
				doorOpenTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
				return 0;
			}

			walkTo(Object.ANVIL_DOOR.coordinate.getX(), Object.ANVIL_DOOR.coordinate.getY());
			return SLEEP_ONE_TICK;
		}

		if (getWallObjectIdFromCoords(Object.ANVIL_DOOR.coordinate.getX(), Object.ANVIL_DOOR.coordinate.getY()) == Object.ANVIL_DOOR.id) {
			if (System.currentTimeMillis() <= doorOpenTimeout) {
				return 0;
			}

			atWallObject(Object.ANVIL_DOOR.coordinate.getX(), Object.ANVIL_DOOR.coordinate.getY());
			doorOpenTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
			return 0;
		}

		walkTo(Object.ANVIL_DOOR.coordinate.getX() - 1, Object.ANVIL_DOOR.coordinate.getY());
		return SLEEP_ONE_TICK;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		drawString("@yel@Varrock West Smither", PAINT_OFFSET_X, y, 1, 0);

		drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getAccurateXpForLevel(Skill.SMITHING.getIndex()) - initialSmithingXp;

		drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s bars@cya@/@whi@hr@cya@)",
				bar, barsSmithed, toUnitsPerHour(barsSmithed, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		drawString(String.format("@yel@Remaining: @whi@%d", barsRemaining),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		drawString(String.format("@yel@Time remaining: @whi@%s",
				toTimeToCompletion(barsSmithed, barsRemaining, startTime)),
			PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
	}

	private enum Bar {
		BRONZE(169, "Bronze"),
		IRON(170, "Iron"),
		STEEL(171, "Steel"),
		MITHRIL(173, "Mithril"),
		ADAMANTITE(174, "Adamantite"),
		RUNITE(408, "Runite");

		private final int id;
		private final String name;

		Bar(final int id, final String name) {
			this.id = id;
			this.name = name;
		}

		public int getId() {
			return id;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private enum Product {
		DAGGER(1, new String[]{"Make Weapon", "Dagger"}),
		THROWING_KNIFE(1, new String[]{"Make Weapon", "Throwing Knife"}),
		SHORT_SWORD(1, new String[]{"Make Weapon", "Sword", "Short sword"}),
		LONG_SWORD(2, new String[]{"Make Weapon", "Sword", "Long sword (2 bars)"}),
		SCIMITAR(2, new String[]{"Make Weapon", "Sword", "Scimitar (2 bars)"}),
		TWO_HANDED_SWORD(3, new String[]{"Make Weapon", "Sword", "2-handed sword (3 bars)"}),
		HATCHET(1, new String[]{"Make Weapon", "Axe", "Hatchet"}),
		BATTLE_AXE(3, new String[]{"Make Weapon", "Axe", "Battle Axe (3 bars)"}),
		MACE(1, new String[]{"Make Weapon", "Mace"}),
		MEDIUM_HELMET(1, new String[]{"Make Armour", "Helmet", "Medium Helmet"}),
		LARGE_HELMET(2, new String[]{"Make Armour", "Helmet", "Large Helmet (2 bars)"}),
		SQUARE_SHIELD(2, new String[]{"Make Armour", "Shield", "Square Shield (2 bars)"}),
		KITE_SHIELD(3, new String[]{"Make Armour", "Shield", "Kite Shield (3 bars)"}),
		CHAIN_MAIL_BODY(3, new String[]{"Make Armour", "Armour", "Chain mail body (3 bars)"}),
		PLATE_MAIL_BODY(5, new String[]{"Make Armour", "Armour", "Plate mail body (5 bars)"}),
		PLATE_MAIL_LEGS(3, new String[]{"Make Armour", "Armour", "Plate mail legs (3 bars)"}),
		PLATED_SKIRT(3, new String[]{"Make Armour", "Armour", "Plated Skirt (3 bars)"}),
		ARROW_HEADS(1, new String[]{"Make Missile Heads", "Make Arrow Heads."}),
		BRONZE_WIRE(1, new String[]{"Make Craft Item", "Bronze Wire(1 bar)"}),
		NAILS(1, new String[]{"Make Nails"});

		private final int barCount;
		private final String[] menuOptions;

		Product(final int barCount, final String[] menuOptions) {
			this.barCount = barCount;
			this.menuOptions = menuOptions;
		}

		public int getBarCount() {
			return barCount;
		}

		public String[] getMenuOptions() {
			return menuOptions;
		}
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(147, 498), new Coordinate(153, 506)),
		ANVIL_HOUSE(new Coordinate(145, 510), new Coordinate(148, 516));

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
		BANK_DOORS(64, new Coordinate(150, 507)),
		ANVIL_DOOR(2, new Coordinate(149, 512)),
		ANVIL(50, new Coordinate(148, 513));

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
