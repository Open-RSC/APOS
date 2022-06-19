import com.aposbot.Constants;

/**
 * A script for pickpocketing Paladins at the Ardougne Castle.
 * Start script at Paladins with sleeping bag and food in inventory.
 * <p>
 * Optional Parameter:
 * <tuna|lobster|swordfish|bass|shark> (Default shark)
 *
 * @Author Chomp
 */
public class AA_PaladinThiever extends AA_Script {
	private static final Coordinate COORD_BANK = new Coordinate(586, 576);

	private static final int ITEM_ID_COINS = 10;
	private static final int ITEM_ID_CHAOS_RUNE = 41;

	private static final int NPC_ID_PALADIN = 323;
	private static final int Y_COORD_DOOR = 1548;
	private static final int Y_COORD_CASTLE = 1000;

	private static final int MAX_FATIGUE = 100;

	private Coordinate prevCoord;
	private Food food;
	private java.lang.Object paladin;

	private double initialThievingXp;
	private double totalAttempts;

	private long startTime;
	private long thieveTimeout;
	private long eatTimeout;
	private long withdrawFoodTimeout;
	private long depositFoodTimeout;
	private long depositChaosTimeout;
	private long depositCoinsTimeout;

	private int eatAt;
	private int success;
	private int fail;
	private int foodCount;
	private int cmbRoundCount;

	private int playerX;
	private int playerY;

	private boolean banking;
	private boolean flee;
	private boolean idle;

	public AA_PaladinThiever(final Extension bot) {
		super(bot);
		combatStyle = CombatStyle.DEFENSE;
	}

	@Override
	public void init(final String parameters) {
		food = parameters.isEmpty() ? Food.SHARK : Food.valueOf(parameters.toUpperCase());

		if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
			throw new IllegalStateException("Sleeping bag missing from inventory.");
		}

		setCombatStyle(combatStyle.getIndex());
		banking = !hasInventoryItem(food.getId()) || isBankOpen();
		eatAt = getBaseHits() - food.getHealAmount();
		initialThievingXp = getSkillExperience(Skill.THIEVING.getIndex());
		startTime = System.currentTimeMillis();
	}

	@Override
	public int main() {
		playerX = getPlayerX();
		playerY = getPlayerY();

		if (bot.getCombatStyle() != combatStyle.getIndex()) setCombatStyle(combatStyle.getIndex());

		return banking ? bank() : thieve();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("attempt", 4)) {
			totalAttempts++;
			thieveTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		} else if (message.startsWith("pick", 4)) {
			success++;
			thieveTimeout = 0L;
		} else if (message.startsWith("fail", 4)) {
			fail++;
			thieveTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("enough") || message.endsWith("combat")) {
			thieveTimeout = 0L;
		} else if (message.endsWith("area")) {
			idle = true;
			prevCoord = new Coordinate(getPlayerX(), getPlayerY());
		} else if (message.startsWith("eat", 4)) {
			eatTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.startsWith("advanced", 14)) {
			success = 0;
			fail = 0;
			totalAttempts = 0;
		} else {
			super.onServerMessage(message);
		}
	}

	private int bank() {
		if (playerY <= COORD_BANK.getY()) {
			if (getCurrentHits() <= eatAt) {
				final int foodIndex = getInventoryItemIndex(food.getId());

				if (foodIndex != -1) {
					return eat(foodIndex);
				}
			}

			if (!isBankOpen()) {
				return openBank();
			}

			int reqEmptySpaces = 2;

			final int chaosIndex = getInventoryItemIndex(ITEM_ID_CHAOS_RUNE);

			if (chaosIndex != -1) {
				reqEmptySpaces--;

				final int chaosCount = getInventoryItemCount(chaosIndex);

				if (chaosCount > 1) {
					if (System.currentTimeMillis() <= depositChaosTimeout) {
						return 0;
					}

					deposit(ITEM_ID_CHAOS_RUNE, chaosCount - 1);
					depositChaosTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
					return 0;
				}
			}

			final int coinsIndex = getInventoryItemIndex(ITEM_ID_COINS);

			if (coinsIndex != -1) {
				reqEmptySpaces--;

				final int coinsCount = getInventoryItemCount(coinsIndex);

				if (coinsCount > 1) {
					if (System.currentTimeMillis() <= depositCoinsTimeout) {
						return 0;
					}

					deposit(ITEM_ID_COINS, coinsCount - 1);
					depositCoinsTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
					return 0;
				}
			}

			final int emptyCount = getInventoryEmptyCount();

			if (emptyCount == reqEmptySpaces) {
				banking = false;
				return 0;
			}

			if (emptyCount < reqEmptySpaces) {
				if (System.currentTimeMillis() <= depositFoodTimeout) {
					return 0;
				}

				deposit(food.getId(), reqEmptySpaces - emptyCount);
				depositFoodTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
				return 0;
			}

			if (System.currentTimeMillis() <= withdrawFoodTimeout) {
				return 0;
			}

			if (!hasBankItem(food.getId())) {
				return exit("Out of food.");
			}

			withdraw(food.getId(), emptyCount - reqEmptySpaces);
			withdrawFoodTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			return 0;
		}

		if (playerY < Y_COORD_CASTLE) {
			final Coordinate doors = Object.DOORS.getCoordinate();

			if (playerX > doors.getX()) {
				if (getObjectId(doors.getX(), doors.getY()) == Object.DOORS.getId()) {
					atObject(doors.getX(), doors.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(doors.getX(), doors.getY());
				return SLEEP_ONE_TICK;
			}

			final Coordinate gate = Object.GATE.getCoordinate();

			if (playerX > gate.getX()) {
				if (getObjectId(gate.getX(), gate.getY()) == Object.GATE.getId()) {
					atObject(gate.getX(), gate.getY());
					return SLEEP_ONE_SECOND;
				}

				walkTo(gate.getX(), gate.getY());
				return SLEEP_ONE_TICK;
			}

			walkTo(COORD_BANK.getX(), COORD_BANK.getY());
			return SLEEP_ONE_TICK;
		}

		final Coordinate door = Object.DOOR.getCoordinate();

		if (playerY >= door.getY()) {
			atWallObject(door.getX(), door.getY());
		} else {
			final Coordinate stairs = Object.STAIRS_DOWN.getCoordinate();
			atObject(stairs.getX(), stairs.getY());
		}

		return SLEEP_ONE_SECOND;
	}

	private int thieve() {
		if (playerY >= Y_COORD_DOOR) {
			if (isInCombat()) {
				if (flee) {
					walkTo(getPlayerX(), getPlayerY());

					if (paladin != null && getFatiguePercent() < MAX_FATIGUE && getCurrentHits() > eatAt) {
						bot.createPacket(Constants.OP_NPC_ACTION);
						bot.put2(bot.getMobServerIndex(paladin));
						bot.finishPacket();
					}

					return SLEEP_ONE_TICK;
				}

				return 0;
			}

			if (flee) {
				flee = false;
				cmbRoundCount = 0;
				thieveTimeout = 0L;
			}

			if (thieveTimeout != 0L && System.currentTimeMillis() <= thieveTimeout) {
				return 0;
			}

			if (idle) {
				if (playerX == prevCoord.getX() && playerY == prevCoord.getY()) {
					final Coordinate coord = getWalkableCoordinate();

					if (coord != null) {
						walkTo(coord.getX(), coord.getY());
						return SLEEP_ONE_SECOND;
					}
				}

				idle = false;
			}

			if (getFatiguePercent() >= MAX_FATIGUE) {
				return sleep();
			}

			if (getCurrentHits() <= eatAt) {
				final int foodIndex = getInventoryItemIndex(food.getId());

				if (foodIndex == -1) {
					banking = true;
					return 0;
				}

				return eat(foodIndex);
			}

			paladin = getNearestNpcNotInCombat(NPC_ID_PALADIN);

			if (paladin == null) {
				return 0;
			}

			if (distanceTo(paladin) > 2) {
				walkTo(paladin);
			} else {
				bot.createPacket(Constants.OP_NPC_ACTION);
				bot.put2(bot.getMobServerIndex(paladin));
				bot.finishPacket();
			}

			return SLEEP_ONE_TICK;
		}

		if (playerY > Y_COORD_CASTLE) {
			final Coordinate door = Object.DOOR.getCoordinate();
			atWallObject2(door.getX(), door.getY());
			return SLEEP_ONE_TICK;
		}

		final Coordinate doors = Object.DOORS.getCoordinate();

		if (playerX > doors.getX()) {
			final Coordinate stairs = Object.STAIRS_UP.getCoordinate();

			if (isInCombat() || playerX != stairs.getX() + 1 || playerY != stairs.getY() - 1) {
				walkTo(stairs.getX() + 1, stairs.getY() - 1);
			} else {
				useObject1(stairs.getX(), stairs.getY());
			}

			return SLEEP_ONE_TICK;
		}

		final Coordinate gate = Object.GATE.getCoordinate();

		if (playerX > gate.getX()) {
			if (getObjectId(doors.getX(), doors.getY()) == Object.DOORS.getId()) {
				atObject(doors.getX(), doors.getY());
				return SLEEP_ONE_SECOND;
			}

			walkTo(doors.getX() + 1, doors.getY());
			return SLEEP_ONE_TICK;
		}

		if (distanceTo(gate.getX(), gate.getY()) > 1) {
			walkTo(gate.getX(), gate.getY());
			return SLEEP_ONE_TICK;
		}

		if (getObjectId(gate.getX(), gate.getY()) == Object.GATE.getId()) {
			atObject(gate.getX(), gate.getY());
			return SLEEP_ONE_SECOND;
		}

		walkTo(gate.getX() + 1, gate.getY());
		return SLEEP_ONE_TICK;
	}

	private int eat(final int inventoryIndex) {
		if (System.currentTimeMillis() <= eatTimeout) {
			return 0;
		}

		foodCount++;
		useItem(inventoryIndex);
		eatTimeout = System.currentTimeMillis() + TIMEOUT_ONE_SECOND;
		return 0;
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Paladin Thiever", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		final double xpGained = getSkillExperience(Skill.THIEVING.getIndex()) - initialThievingXp;

		bot.drawString(String.format("@yel@Xp: @whi@%s @cya@(@whi@%s@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(xpGained), toUnitsPerHour((int) xpGained, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString(String.format("@yel@Pickpocket: @whi@%s @cya@(@whi@%s@cya@/@whi@hr@cya@)",
				DECIMAL_FORMAT.format(totalAttempts), toUnitsPerHour((int) totalAttempts, startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (success > 0) {
			bot.drawString(String.format("@gr1@Success Rate: @whi@%.2f%%", (success / totalAttempts) * 100),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

			bot.drawString(String.format("Fail Rate: @whi@%.2f%%", (fail / totalAttempts) * 100),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0xE0142D);
		}
	}

	@Override
	public void onPlayerDamaged(final java.lang.Object player) {
		if (player == bot.getPlayer()) {
			cmbRoundCount++;
		}
	}

	@Override
	public void onNpcDamaged(final java.lang.Object npc) {
		if (cmbRoundCount >= 2) {
			flee = true;
		}
	}

	private enum Object implements RSObject {
		GATE(57, new Coordinate(598, 603)),
		DOORS(64, new Coordinate(607, 603)),
		STAIRS_UP(342, new Coordinate(611, 601)),
		STAIRS_DOWN(44, new Coordinate(611, 1545)),
		DOOR(97, new Coordinate(609, 1548));

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
