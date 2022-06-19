/**
 * Smelts bars at the Alkharid furnace.
 * <p>
 * Requirements:
 * Start at Alkharid Bank with sleeping bag in inventory.
 * Have cannon ammo mould in inventory if smelting ore -> steel bar -> multi cannon ball.
 * <p>
 * Required Parameters:
 * <bronze|iron|silver|steel|gold|mithril|adamantite|runite>
 * <p>
 *
 * @Author Chomp
 */
public class AA_AlkharidSmelter extends AA_Script {
	private static final int ITEM_ID_MULTI_CANNON_BALL = 1041;
	private static final int ITEM_ID_CANNON_AMMO_MOULD = 1057;
	private static final int MAX_DIST_FROM_OBJ = 18;
	private static final int MAX_SLEEP_WALK_FATIGUE = 80;
	private static final int MAX_FATIGUE = 99;

	private Bar bar;

	private double initSmithXp;

	private long startTime;
	private long depositTimeout;
	private long withdrawPrimaryTimeout;
	private long withdrawSecondaryTimeout;
	private long smeltTimeout;

	private int playerX;
	private int playerY;

	private int barsSmelted;
	private int oreRemaining;

	private int initInvSize;

	private boolean smeltBalls;
	private boolean banking;

	public AA_AlkharidSmelter(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (parameters.isEmpty()) printInstructions();

		bar = Bar.valueOf(parameters.toUpperCase());

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		smeltBalls = bar == Bar.STEEL && hasInventoryItem(ITEM_ID_CANNON_AMMO_MOULD);
		initInvSize = smeltBalls ? 2 : 1;
		initSmithXp = getSkillExperience(Skill.SMITHING.getIndex());

		if (getInventoryItemCount() == initInvSize) {
			banking = true;
		} else {
			if (smeltBalls) {
				if (getInventoryItemId(initInvSize) == ITEM_ID_MULTI_CANNON_BALL) {
					banking = true;
				}
			} else {
				if (getInventoryItemId(initInvSize) != bar.primaryOreId) {
					banking = true;
				}
			}
		}

		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getPlayerX();
		playerY = getPlayerY();

		return banking ? bank() : smelt();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("bar", 15)) {
			barsSmelted++;

			if (oreRemaining > 0) {
				oreRemaining--;
			}

			smeltTimeout = 0L;
		} else if (message.endsWith("heavy") || message.startsWith("impure", 15)) {
			smeltTimeout = 0L;
		} else {
			super.onServerMessage(message);
		}
	}

	private int bank() {
		if (isBankOpen()) {
			if (getInventoryItemCount() > initInvSize) {
				if (getInventoryItemId(initInvSize) != bar.primaryOreId ||
					getInventoryItemIdCount(bar.primaryOreId) > bar.primaryOreWithdrawCount) {
					if (System.currentTimeMillis() <= depositTimeout) {
						return 0;
					}

					deposit(getInventoryItemId(initInvSize), MAX_INVENTORY_SIZE);
					depositTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
					return 0;
				}

				if (bar.secondaryOreId != -1) {
					final int primaryInvCount = getInventoryItemIdCount(bar.primaryOreId);
					final int secondaryInvCount = getInventoryItemIdCount(bar.secondaryOreId);

					final int missing = (primaryInvCount * bar.secondaryOreCount) - secondaryInvCount;

					if (missing > 0) {
						if (System.currentTimeMillis() <= withdrawSecondaryTimeout) {
							return 0;
						}

						final int primaryBankCount = getBankItemIdCount(bar.primaryOreId);
						final int secondaryBankCount = getBankItemIdCount(bar.secondaryOreId);

						if (secondaryBankCount < missing) {
							return exit(String.format("Ran out of %s.", getItemName(bar.secondaryOreId)));
						}

						oreRemaining = Math.min(primaryBankCount, secondaryBankCount / bar.secondaryOreCount);

						withdraw(bar.secondaryOreId, bar.secondaryOreWithdrawCount);
						withdrawSecondaryTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
						return 0;
					}
				}

				banking = false;
				return 0;
			}

			if (System.currentTimeMillis() <= withdrawPrimaryTimeout) {
				return 0;
			}

			oreRemaining = getBankItemIdCount(bar.primaryOreId);

			if (oreRemaining == 0) {
				return exit(String.format("Ran out of %s.", getItemName(bar.primaryOreId)));
			}

			withdraw(bar.primaryOreId, bar.primaryOreWithdrawCount);
			withdrawPrimaryTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		if (Area.BANK.contains(playerX, playerY)) {
			return openBank();
		}

		final Coordinate doors = Object.BANK_DOORS.getCoordinate();

		if (distanceTo(doors.getX(), doors.getY()) <= MAX_DIST_FROM_OBJ) {
			if (getObjectId(doors.getX(), doors.getY()) == Object.BANK_DOORS.id) {
				atObject(doors.getX(), doors.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(doors.getX() + 1, doors.getY());
			return SLEEP_ONE_TICK;
		}

		walkTo(doors.getX(), doors.getY());
		return SLEEP_ONE_TICK;
	}

	private int smelt() {
		if (Area.FURNACE.contains(playerX, playerY)) {
			if (smeltBalls) {
				if (getInventoryItemId(initInvSize) == ITEM_ID_MULTI_CANNON_BALL) {
					banking = true;
					return 0;
				}
			} else {
				if (getInventoryItemId(initInvSize) != bar.primaryOreId) {
					banking = true;
					return 0;
				}
			}

			if (System.currentTimeMillis() <= smeltTimeout) {
				return 0;
			}

			if (getFatiguePercent() >= MAX_FATIGUE) {
				return sleep();
			}

			final Coordinate furnace = Object.FURNACE.getCoordinate();

			if (playerX != furnace.getX() - 1 || playerY != furnace.getY()) {
				walkTo(furnace.getX() - 1, furnace.getY());
				return SLEEP_ONE_TICK;
			}

			useWithObject(initInvSize, furnace.getX(), furnace.getY());
			smeltTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			bot.displayMessage("@gre@Smelting...");
			return 0;
		}

		if (Area.BANK.contains(playerX, playerY)) {
			final Coordinate doors = Object.BANK_DOORS.getCoordinate();

			if (getObjectId(doors.getX(), doors.getY()) == Object.BANK_DOORS.id) {
				atObject(doors.getX(), doors.getY());
				return SLEEP_ONE_SECOND;
			}
		}

		final Coordinate furnace = Object.FURNACE.getCoordinate();

		walkTo(furnace.getX() - 1, furnace.getY());

		if (getFatiguePercent() >= MAX_SLEEP_WALK_FATIGUE && isWalking()) {
			return sleep();
		}

		return SLEEP_ONE_TICK;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Alkharid Smelter", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getSkillExperience(Skill.SMITHING.getIndex()) - initSmithXp;

		bot.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s xp@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s bars@cya@/@whi@hr@cya@)",
				bar, barsSmelted, toUnitsPerHour(barsSmelted, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (oreRemaining != 0) {
			bot.drawString(String.format("@yel@Remaining: @whi@%d", oreRemaining),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

			bot.drawString(String.format("@yel@Time remaining: @whi@%s",
					toTimeToCompletion(barsSmelted, oreRemaining, startTime)),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}
	}

	private enum Bar {
		BRONZE(150, 202, 1, 14, 14, "Bronze"),
		IRON(151, -1, 0, 29, 0, "Iron"),
		SILVER(383, -1, 0, 29, 0, "Silver"),
		STEEL(151, 155, 2, 9, 18, "Steel"),
		GOLD(152, -1, 0, 29, 0, "Gold"),
		MITHRIL(153, 155, 4, 5, 20, "Mithril"),
		ADAMANTITE(154, 155, 6, 4, 24, "Adamantite"),
		RUNITE(409, 155, 8, 3, 24, "Runite");

		private final int primaryOreId;
		private final int secondaryOreId;
		private final int secondaryOreCount;
		private final int primaryOreWithdrawCount;
		private final int secondaryOreWithdrawCount;
		private final String name;

		Bar(final int primaryOreId, final int secondaryOreId, final int secondaryOreCount, final int primaryOreWithdrawCount, final int secondaryOreWithdrawCount, final String name) {
			this.primaryOreId = primaryOreId;
			this.secondaryOreId = secondaryOreId;
			this.secondaryOreCount = secondaryOreCount;
			this.primaryOreWithdrawCount = primaryOreWithdrawCount;
			this.secondaryOreWithdrawCount = secondaryOreWithdrawCount;
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private enum Area implements RSArea {
		BANK(new Coordinate(87, 689), new Coordinate(93, 700)),
		FURNACE(new Coordinate(82, 678), new Coordinate(86, 681));

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
		FURNACE(118, new Coordinate(85, 679)),
		BANK_DOORS(64, new Coordinate(86, 695));

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
