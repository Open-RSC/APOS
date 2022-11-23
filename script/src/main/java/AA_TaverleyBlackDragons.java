import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DESCRIPTION
 * -----------
 * Kills Black Dragons in Taverley Dungeon. Trades with mule for Catherby banking.
 * Start Master on the island and Mule across from the island.
 * Have anti dragon breath shield, prayer gear, or tank gear with an optional shield switch.
 * <p>
 * FEATURES
 * --------
 * Pray setup or tank setup.
 * Super sets or no pots.
 * Shield switch in combat if tanking.
 * Combat style switching if tanking.
 * Prayer flicking if praying.
 * <p>
 * KEYBINDS
 * --------
 * l - toggle loot paint
 * s - toggle settings paint
 * <p>
 * NOTES
 * -----
 * Items must be equipped on start and must not move inventory slots.
 * Script will stop if food runs out, prayer pots run out, pm fails, or too many items in inventory.
 * <p>
 *
 * @Author Chomp
 */
public class AA_TaverleyBlackDragons extends AA_Script {
	private static final Coordinate COORD_LOAD_BANK = new Coordinate(416, 499);
	private static final Coordinate COORD_TRADE_MASTER = new Coordinate(412, 506);
	private static final Coordinate COORD_TRADE_MULE = new Coordinate(411, 503);
	private static final Coordinate COORD_WAIT_RESPAWN = new Coordinate(410, 3336);

	private static final Set<Integer> ITEM_IDS_LOOT = Stream.of(
		10, 11, 31, 33, 38, 40, 42, 75, 79, 81, 93,
		120, 130, 174,
		403, 404, 405, 408, 438, 439, 441, 442, 443,
		518, 520, 523, 526, 527,
		619, 795, 814, 1092, 1277
	).collect(Collectors.toCollection(HashSet::new));

	private static final Set<Integer> ITEM_IDS_LOOT_STACKABLE = Stream.of(
		10, 11, 31, 33, 38, 40, 42, 518, 520, 619
	).collect(Collectors.toCollection(HashSet::new));

	private static final int ITEM_ID_ANTI_DRAGON_BREATH_SHIELD = 420;
	private static final int ITEM_ID_VIAL = 465;
	private static final int NPC_ID_BLACK_DRAGON = 291;
	private static final int HITS_XP_BLACK_DRAGON = 105;
	private static final int PRAYER_ID_PARALYZE_MONSTER = 12;
	private static final int MAX_FATIGUE = 95;
	private static final int MAX_DIST_FROM_DRAGON = 2;
	private static final int PAINT_OFFSET_X_SETTINGS = 110;
	private static final int PAINT_OFFSET_Y_LOOT = 188;

	private final Map<Integer, Spawn> spawnMap = new HashMap<>();
	private final Map<Integer, Integer> lootMap = new TreeMap<>();

	private final Set<Integer> equipmentIndexes = new HashSet<>();
	private final List<String> traders = new ArrayList<>();

	private final int[] loot = new int[3];

	private Role role;
	private Food food;
	private String currentTrader;
	private State state;
	private SupplyRequest supplyRequest;
	private Coordinate nextRespawn;

	private double initHitsXp;

	private long startTime;

	private long pmTimeout;
	private long tradeReqTimeout;
	private long foodTimeout;
	private long consumeTimeout;
	private long disablePrayerTimeout;

	private long tickCount;
	private long disableTick;

	private int playerX;
	private int playerY;

	private int foodCount;
	private int foodAte;

	private int minHits;
	private int minPrayer;
	private int minAtk;
	private int minStr;
	private int minDef;

	private int eatAt;
	private int prayAt;

	private int antiShieldIndex;
	private int shieldIndex;

	private boolean idle;
	private boolean spawnCamp;

	private boolean paintLoot;
	private boolean paintSettings;

	private boolean initialized;

	public AA_TaverleyBlackDragons(final Extension ex) {
		super(ex);
	}

	@Override
	public void init(final String parameters) {
		if (!bot.isLoggedIn()) throw new IllegalStateException("Error: Must be logged-in to start this script.");
		createGUI();
	}

	@Override
	public int main() {
		if (!initialized) {
			bot.displayMessage("@red@ERROR: Script was not initialized.");
			return exit("Error: Script was not initialized.");
		}

		playerX = getPlayerX();
		playerY = getPlayerY();

		return role == Role.MASTER ? master() : mule();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("eat", 4)) {
			foodAte++;
			consumeTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("prayer potion")) {
			Potion.PRAYER.sip();
		} else if (message.endsWith("attack potion")) {
			Potion.SUPER_ATTACK.sip();
		} else if (message.endsWith("strength potion")) {
			Potion.SUPER_STRENGTH.sip();
		} else if (message.endsWith("defense potion")) {
			Potion.SUPER_DEFENSE.sip();
		} else if (message.endsWith("left") || message.startsWith("finished", 9)) {
			consumeTimeout = System.currentTimeMillis() + TIMEOUT_ONE_TICK;
		} else if (message.endsWith("successfully")) {
			pmTimeout = 0L;
			tradeReqTimeout = 0L;
			foodTimeout = 0L;
			consumeTimeout = 0L;
		} else if (message.startsWith("declined", 17)) {
			pmTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			tradeReqTimeout = 0L;
		} else if (message.endsWith("area")) {
			idle = true;
		} else if (message.startsWith("Unable") || message.startsWith("Other") || message.endsWith("objects")) {
			setState(State.STOP);
		} else {
			super.onServerMessage(message);
		}
	}

	private void createGUI() {
		// Top-level container
		final JFrame frame = new JFrame("Taverley Black Dragons");
		final Container contentPane = frame.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

		// Panel with shared settings for all Roles
		final JPanel sharedPanel = new JPanel(new GridLayout(0, 2, 5, 5));
		sharedPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Panel with settings for Master Role
		final JPanel masterPanel = new JPanel(new GridLayout(0, 2, 5, 5));
		masterPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Panel with Ok / Cancel Buttons
		final JPanel buttonPanel = new JPanel(new GridLayout(0, 2));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Choose Role
		final JComboBox<Role> roleComboBox = new JComboBox<>(Role.values());
		roleComboBox.addActionListener(e -> {
			final JComboBox cb = (JComboBox) e.getSource();
			for (final Component component : masterPanel.getComponents()) {
				component.setEnabled(cb.getSelectedItem() == Role.MASTER);
			}
		});

		sharedPanel.add(new JLabel("Role"));
		sharedPanel.add(roleComboBox);

		// Specify Traders
		final JTextField traderTextField = new JTextField(12);

		sharedPanel.add(new JLabel("Traders (comma sep.)"));
		sharedPanel.add(traderTextField);

		// Choose Food
		final JComboBox<Food> foodComboBox = new JComboBox<>(new Food[]{Food.SHARK, Food.SWORDFISH, Food.LOBSTER, Food.BASS});

		sharedPanel.add(new JLabel("Food"));
		sharedPanel.add(foodComboBox);

		// Set Food Count
		final JSpinner foodCountSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 28, 1));

		sharedPanel.add(new JLabel("Food Count"));
		sharedPanel.add(foodCountSpinner);

		// Set Prayer Pot Count
		final JSpinner ppSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 27, 1));
		ppSpinner.setValue(0);

		sharedPanel.add(new JLabel("Prayer Potions"));
		sharedPanel.add(ppSpinner);

		// Set Super Atk Pot Count
		final JSpinner supAtkSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 27, 1));
		supAtkSpinner.setValue(0);

		sharedPanel.add(new JLabel("Super Attack Potions"));
		sharedPanel.add(supAtkSpinner);

		// Set Super Str Pot Count
		final JSpinner supStrSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 27, 1));
		supStrSpinner.setValue(0);

		sharedPanel.add(new JLabel("Super Strength Potions"));
		sharedPanel.add(supStrSpinner);

		// Set Super Def Pot Count
		final JSpinner supDefSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 27, 1));
		supDefSpinner.setValue(0);

		sharedPanel.add(new JLabel("Super Defense Potions"));
		sharedPanel.add(supDefSpinner);

		// Choose Combat Style
		final JComboBox<CombatStyle> combatStyleComboBox = new JComboBox<>(new CombatStyle[]{CombatStyle.ATTACK, CombatStyle.STRENGTH, CombatStyle.DEFENSE});

		masterPanel.add(new JLabel("Combat Style"));
		masterPanel.add(combatStyleComboBox);

		// Set Shield Swap
		final JComboBox<Shield> shieldComboBox = new JComboBox<>(Shield.values());

		masterPanel.add(new JLabel("Shield Swap"));
		masterPanel.add(shieldComboBox);

		// Set Min Hits
		int defaultValue;

		defaultValue = (int) Math.ceil(getBaseHits() * 0.60);
		final JSpinner minHitsSpinner = new JSpinner(new SpinnerNumberModel(defaultValue, 1, 99, 1));

		masterPanel.add(new JLabel("Min. Hits"));
		masterPanel.add(minHitsSpinner);

		// Set Min Prayer
		defaultValue = 10;
		final JSpinner minPrayerSpinner = new JSpinner(new SpinnerNumberModel(defaultValue, 1, 99, 1));

		masterPanel.add(new JLabel("Min. Prayer"));
		masterPanel.add(minPrayerSpinner);

		// Set Min Attack
		defaultValue = getBaseSkillLevel(Skill.ATTACK.getIndex()) + (calcSuperPotBoost(Skill.ATTACK.getIndex()) / 2);
		final JSpinner minAtkSpinner = new JSpinner(new SpinnerNumberModel(defaultValue, 1, 118, 1));

		masterPanel.add(new JLabel("Min. Attack"));
		masterPanel.add(minAtkSpinner);

		// Set Min Strength
		defaultValue = getBaseSkillLevel(Skill.STRENGTH.getIndex()) + (calcSuperPotBoost(Skill.STRENGTH.getIndex()) / 2);
		final JSpinner minStrSpinner = new JSpinner(new SpinnerNumberModel(defaultValue, 1, 118, 1));

		masterPanel.add(new JLabel("Min. Strength"));
		masterPanel.add(minStrSpinner);

		// Set Min Defense
		defaultValue = getBaseSkillLevel(Skill.DEFENSE.getIndex()) + (calcSuperPotBoost(Skill.DEFENSE.getIndex()) / 2);
		final JSpinner minDefSpinner = new JSpinner(new SpinnerNumberModel(defaultValue, 1, 118, 1));

		masterPanel.add(new JLabel("Min. Defense"));
		masterPanel.add(minDefSpinner);

		// Enable Spawn Camp
		final JCheckBox spawnCampCheckBox = new JCheckBox("Spawn Camp");

		masterPanel.add(spawnCampCheckBox);
		masterPanel.add(Box.createGlue());

		// Ok Button
		final JButton okButton = new JButton("OK");

		okButton.addActionListener(e -> {
			role = (Role) roleComboBox.getSelectedItem();

			if (role == Role.MASTER) {
				if (!hasInventoryItem(ITEM_ID_SLEEPING_BAG)) {
					System.err.println("Error: Sleeping bag missing from inventory.");
					return;
				}

				if ((antiShieldIndex = getInventoryItemIndex(ITEM_ID_ANTI_DRAGON_BREATH_SHIELD)) == -1) {
					System.err.println("Error: Anti dragon breath shield missing from inventory.");
					return;
				}
			}

			final String text = traderTextField.getText().trim();

			if (text.contains(" ")) {
				System.err.println("Error: Traders field cannot contain spaces. Use underscores.");
				return;
			}

			final String[] traders = text.split(",");

			if (traders.length == 0 || traders[0].isEmpty()) {
				System.err.println("Error: Traders text field is empty.");
				return;
			}

			if (role == Role.MASTER) {
				if (traders.length > 1) {
					System.err.println("Error: Master can only trade one mule.");
					return;
				}
				currentTrader = traders[0].replace('_', ' ');
				if (!isOnFriendList(currentTrader)) addToFriendList(currentTrader);
			} else {
				for (final String trader : traders) {
					final String rsn = trader.replace('_', ' ');
					if (!isOnFriendList(rsn)) addToFriendList(rsn);
					this.traders.add(rsn);
				}
			}

			food = (Food) foodComboBox.getSelectedItem();
			foodCount = (int) foodCountSpinner.getValue();

			int potionCount;

			potionCount = (int) ppSpinner.getValue();
			Potion.PRAYER.setUsing(potionCount > 0);
			Potion.PRAYER.setCount(potionCount);

			potionCount = (int) supAtkSpinner.getValue();
			Potion.SUPER_ATTACK.setUsing(potionCount > 0);
			Potion.SUPER_ATTACK.setCount(potionCount);

			potionCount = (int) supStrSpinner.getValue();
			Potion.SUPER_STRENGTH.setUsing(potionCount > 0);
			Potion.SUPER_STRENGTH.setCount(potionCount);

			potionCount = (int) supDefSpinner.getValue();
			Potion.SUPER_DEFENSE.setUsing(potionCount > 0);
			Potion.SUPER_DEFENSE.setCount(potionCount);

			if (Potion.PRAYER.isUsing() && Potion.SUPER_DEFENSE.isUsing()) {
				System.err.println("Error: Cannot use both ppots and sup def pots.");
				return;
			}

			if (role == Role.MASTER) {
				eatAt = getBaseHits() - food.getHealAmount();
				prayAt = getBaseSkillLevel(Skill.PRAYER.getIndex()) - (int) (getBaseSkillLevel(Skill.PRAYER.getIndex()) * 0.25 + 7);

				combatStyle = (CombatStyle) combatStyleComboBox.getSelectedItem();

				final Shield shield = (Shield) shieldComboBox.getSelectedItem();
				assert shield != null;
				shieldIndex = getInventoryItemIndex(shield.getItemId());
				if (shieldIndex != -1) equipmentIndexes.add(shieldIndex);

				minHits = (int) minHitsSpinner.getValue();
				minPrayer = (int) minPrayerSpinner.getValue();
				minAtk = (int) minAtkSpinner.getValue();
				minStr = (int) minStrSpinner.getValue();
				minDef = (int) minDefSpinner.getValue();

				spawnCamp = spawnCampCheckBox.isSelected();

				for (int index = 0; index < getInventoryItemCount(); index++) {
					if (!bot.isEquipped(index)) continue;
					equipmentIndexes.add(index);
				}

				playerX = getPlayerX();
				playerY = getPlayerY();

				if (TBDArea.BLACK_DRAGONS.contains(playerX, playerY)) {
					setState(State.KILL);
				} else if (TBDArea.ISLAND.contains(playerX, playerY)) {
					setState(State.TRADE);
				} else {
					System.err.println("Error: Must start script at Black Dragons or on the island.");
					return;
				}

				bot.setCombatStyle(combatStyle.getIndex());
				initHitsXp = getSkillExperience(Skill.HITS.getIndex());
			} else {
				if (getInventoryItemIdCount(food.getId()) < foodCount) {
					setState(State.BANK);
				} else {
					for (final Potion potion : Potion.VALUES) {
						if (potion.isUsing() && getInventoryItemIdCount(potion.getIds()[2]) < potion.getCount()) {
							setState(State.BANK);
							break;
						}
					}
				}

				if (state == null) setState(State.TRADE);
			}

			System.out.println("Info: Press 'l' to display loot paint.");
			System.out.println("Info: Press 's' to display settings paint.");
			System.out.printf("[%s] Script ready.%n", this);

			startTime = System.currentTimeMillis();
			initialized = true;
			frame.dispose();
		});

		// Cancel Button
		final JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> frame.dispose());

		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		// Build
		frame.add(sharedPanel);
		frame.add(Box.createVerticalStrut(5));
		frame.add(masterPanel);
		frame.add(buttonPanel);

		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		frame.pack();
		frame.setVisible(true);
		frame.toFront();
		frame.requestFocus();
	}

	private int calcSuperPotBoost(final int skillIndex) {
		return (int) (getBaseSkillLevel(skillIndex) * 0.15 + 5);
	}

	private void setState(final State state) {
		switch (state) {
			case BANK:
				if (traders.size() > 1) currentTrader = null;
				updateLootMap();
				break;
			case TRADE:
				if (role == Role.MASTER) updateLootMap();
				break;
			case KILL:
			case FATIGUE:
			case STOP:
				break;
		}

		this.state = state;
	}

	private void updateLootMap() {
		for (int index = 0; index < getInventoryItemCount(); index++) {
			if (equipmentIndexes.contains(index)) continue;
			final int itemId = getInventoryItemId(index);
			if (!ITEM_IDS_LOOT.contains(itemId)) continue;
			final int count = getInventoryItemCount(index);
			lootMap.merge(itemId, count, Integer::sum);
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Taverley Black Dragons", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@State: @whi@%s", state.description),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (role == Role.MASTER) {
			final double gainedHitsXp = getSkillExperience(Skill.HITS.getIndex()) - initHitsXp;
			final int kills = (int) (gainedHitsXp / HITS_XP_BLACK_DRAGON);

			if (kills != 0) {
				bot.drawString(String.format("@yel@Dragons: @whi@%d @cya@(@whi@%s @whi@kills@cya@/@whi@hr@cya@)",
						kills, toUnitsPerHour(kills, startTime)),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);
			}

			if (foodAte != 0) {
				bot.drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s @whi@food@cya@/@whi@hr@cya@)",
						food, foodAte, toUnitsPerHour(foodAte, startTime)),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
			}

			for (final Potion potion : Potion.VALUES) {
				if (!potion.isUsing()) continue;
				final int consumed = potion.getSips() / 3;
				if (consumed == 0) continue;

				bot.drawString(String.format("@yel@%s: @whi@%d @cya@(@whi@%s @whi@pots@cya@/@whi@hr@cya@)",
						potion.getDesc(), consumed, toUnitsPerHour(consumed, startTime)),
					PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
			}
		}

		if (paintLoot) {
			y = PAINT_OFFSET_Y_LOOT;
			int index = 0;
			int xOffset = 107;

			for (final Map.Entry<Integer, Integer> entry : lootMap.entrySet()) {
				final int itemId = entry.getKey();

				if (++index % 9 == 0) {
					xOffset += 100;
					y = PAINT_OFFSET_Y_LOOT;
				}

				String name = getItemName(itemId);
				if (name.length() > 12) name = name.substring(0, 12);

				bot.drawString(String.format("%s: @whi@%d", name, entry.getValue()),
					xOffset, y += PAINT_OFFSET_Y_INCREMENT, 1, 0xFF0000);
			}
		} else if (paintSettings) {
			y = PAINT_OFFSET_Y;

			bot.drawString(String.format("Role: @whi@%s", role),
				PAINT_OFFSET_X_SETTINGS, y, 1, 0x00FF00);

			if (role == Role.MASTER) {
				bot.drawString(String.format("Trader: @whi@%s", currentTrader),
					PAINT_OFFSET_X_SETTINGS, y += PAINT_OFFSET_Y_INCREMENT, 1, 0x00FF00);

				bot.drawString(String.format("Spawn Camp: @whi@%s", spawnCamp),
					PAINT_OFFSET_X_SETTINGS, y += PAINT_OFFSET_Y_INCREMENT, 1, 0x00FF00);

				bot.drawString(String.format("Shield Index: @whi@%d", shieldIndex),
					PAINT_OFFSET_X_SETTINGS, y += PAINT_OFFSET_Y_INCREMENT, 1, 0x00FF00);

				bot.drawString(String.format("Anti Index: @whi@%d", antiShieldIndex),
					PAINT_OFFSET_X_SETTINGS, y += PAINT_OFFSET_Y_INCREMENT, 1, 0x00FF00);

				bot.drawString(String.format("Min Stats: @whi@H%d P%d A%d S%d D%d",
						minHits, minPrayer, minAtk, minStr, minDef),
					PAINT_OFFSET_X_SETTINGS, y += PAINT_OFFSET_Y_INCREMENT, 1, 0x00FF00);

				bot.drawString(String.format("Thresholds: @whi@H%d P%d", eatAt, prayAt),
					PAINT_OFFSET_X_SETTINGS, y += PAINT_OFFSET_Y_INCREMENT, 1, 0x00FF00);
			} else {
				for (final String trader : traders) {
					bot.drawString(String.format("Trader: @whi@%s", trader),
						PAINT_OFFSET_X_SETTINGS, y += PAINT_OFFSET_Y_INCREMENT, 1, 0x00FF00);
				}
			}

			bot.drawString(String.format("%s: @whi@%d", food, foodCount),
				PAINT_OFFSET_X_SETTINGS, y += PAINT_OFFSET_Y_INCREMENT, 1, 0x00FF00);

			for (final Potion potion : Potion.VALUES) {
				bot.drawString(String.format("%s: @whi@%d", potion.getDesc(), potion.count),
					PAINT_OFFSET_X_SETTINGS, y += PAINT_OFFSET_Y_INCREMENT, 1, 0x00FF00);
			}
		}
	}

	@Override
	public void onTradeRequest(final String playerName) {
		if (role != Role.MULE || !isAtCoordinate(COORD_TRADE_MULE)) return;

		if (currentTrader != null) {
			if (!playerName.equalsIgnoreCase(currentTrader)) return;

			final Object player = getPlayerObjByName(playerName);

			if (player != null) {
				sendTradeRequest(getServerIndex(player));
			} else {
				currentTrader = null;
			}

			return;
		}

		for (final String trader : traders) {
			if (!playerName.equalsIgnoreCase(trader)) continue;
			final Object player = getPlayerObjByName(playerName);
			if (player == null) return;
			currentTrader = playerName;
			sendTradeRequest(getServerIndex(player));
			break;
		}
	}

	@Override
	public void onPrivateMessage(final String message, final String playerName, final boolean moderator,
								 final boolean administrator) {
		if (role == Role.MASTER) {
			if (!playerName.equalsIgnoreCase(currentTrader)) return;

			if (message.equalsIgnoreCase("stop")) {
				setState(State.STOP);
			} else {
				final Potion potion = Potion.valueOf(message.toUpperCase());
				potion.setUsing(false);
			}
		} else {
			if (!playerName.equalsIgnoreCase(currentTrader)) return;

			if (message.equalsIgnoreCase("bank")) {
				setState(State.BANK);
			} else {
				supplyRequest = SupplyRequest.parse(message);
			}
		}
	}

	@Override
	public void onKeyPress(final KeyEvent keyEvent) {
		switch (keyEvent.getKeyCode()) {
			case KeyEvent.VK_L:
				if (lootMap.isEmpty()) {
					bot.displayMessage("@red@You have no loot to display.");
					keyEvent.consume();
					return;
				}
				if (paintSettings) paintSettings = false;
				paintLoot = !paintLoot;
				keyEvent.consume();
				break;
			case KeyEvent.VK_S:
				if (paintLoot) paintLoot = false;
				paintSettings = !paintSettings;
				keyEvent.consume();
				break;
		}
	}

	@Override
	public void onPlayerDamaged(final Object player) {
		if (!isInCombat() || player != bot.getPlayer()) return;
		final int index = combatStyle.getIndex();
		if (bot.getCombatStyle() != index) bot.setCombatStyle(index);
	}

	@Override
	public void onNpcDamaged(final Object npc) {
		if (!isInCombat() || getWaypointX(npc) != bot.getPlayerWaypointX() ||
			getWaypointY(npc) != bot.getPlayerWaypointY()) {
			return;
		}

		final int index;

		if (Potion.PRAYER.isUsing()) {
			index = combatStyle.getIndex();
			disableTick = tickCount + 1;
			enableParalyzeMonster();
		} else {
			index = CombatStyle.DEFENSE.getIndex();
			if (shieldIndex != -1 && !bot.isEquipped(shieldIndex)) equipItem(shieldIndex);
		}

		if (bot.getCombatStyle() != index) bot.setCombatStyle(index);
	}

	@Override
	public void onNpcSpawned(final java.lang.Object npc) {
		if (!spawnCamp || bot.getNpcId(npc) != NPC_ID_BLACK_DRAGON) return;

		final int serverIndex = getServerIndex(npc);
		final Spawn spawn = spawnMap.get(serverIndex);

		final int x = getX(npc);
		final int y = getY(npc);

		if (spawn != null) {
			spawn.getCoordinate().set(x, y);
			spawn.setTimestamp(Long.MAX_VALUE);
		} else {
			spawnMap.put(serverIndex, new Spawn(new Coordinate(x, y), Long.MAX_VALUE));
		}

		nextRespawn = getNextRespawn();
	}

	@Override
	public void onNpcDespawned(final Object npc) {
		if (!spawnCamp || bot.getNpcId(npc) != NPC_ID_BLACK_DRAGON || state != State.KILL) return;
		final int serverIndex = getServerIndex(npc);
		final Spawn spawn = spawnMap.get(serverIndex);
		if (spawn == null) return;
		spawn.setTimestamp(System.currentTimeMillis());
		nextRespawn = getNextRespawn();
	}

	@Override
	public void onPlayerCoord(final int x, final int y) {
		++tickCount;
		if (!isInCombat() || !Potion.PRAYER.isUsing()) return;
		if (tickCount == disableTick) disableParalyzeMonster();
	}

	private void disableParalyzeMonster() {
		if (!bot.isPrayerEnabled(PRAYER_ID_PARALYZE_MONSTER)) return;
		disablePrayer(PRAYER_ID_PARALYZE_MONSTER);
	}

	private Coordinate getNextRespawn() {
		if (spawnMap.isEmpty()) return null;
		return spawnMap.values().stream().min(Comparator.naturalOrder()).get().getCoordinate();
	}

	private void enableParalyzeMonster() {
		if (!Potion.PRAYER.isUsing() || bot.isPrayerEnabled(PRAYER_ID_PARALYZE_MONSTER) || !hasPrayer()) return;
		enablePrayer(PRAYER_ID_PARALYZE_MONSTER);
	}

	private boolean hasPrayer() {
		return getCurrentSkillLevel(Skill.PRAYER.getIndex()) > 0;
	}

	private int master() {
		switch (state) {
			case KILL:
				return kill();
			case FATIGUE:
				return fatigue();
			case TRADE:
				return tradeWithMule();
			case STOP:
				return stop();
			default:
				return exit(String.format("%s invalid state for role %s", state, role));
		}
	}

	private int kill() {
		if (TBDArea.ISLAND.contains(playerX, playerY)) {
			int index;
			if ((canEat() && (index = getInventoryItemIndex(food.getId())) != -1) ||
				(canRestorePrayer() && (index = getInventoryItemIndex(Potion.PRAYER.getIds())) != -1)) {
				return consumeIndex(index);
			}

			final Coordinate ladder = TBDObject.LADDER_DOWN.getCoordinate();
			atObject(ladder.getX(), ladder.getY());
			return SLEEP_ONE_TICK;
		}

		updateLoot();

		if (isInCombat()) {
			if (loot[0] != -1 ||
				needToEat() ||
				needToRestorePrayer() ||
				needToDrinkSuperAtkPot() ||
				needToDrinkSuperStrPot() ||
				needToDrinkSuperDefPot()) {
				walkTo(playerX, playerY);
				return SLEEP_ONE_TICK;
			}
			return 0;
		}

		if (!bot.isEquipped(antiShieldIndex)) {
			equipItem(antiShieldIndex);
			return SLEEP_ONE_TICK;
		}

		if (needToEat()) return consumeItemId(food.getId());
		if (needToRestorePrayer()) return consumeItemId(Potion.PRAYER.getIds());

		if (isInventoryFull() && !hasInventoryItem(food.getId()) && !hasInventoryItem(ITEM_ID_VIAL)) {
			setState(State.TRADE);
			return 0;
		}

		if (loot[0] != -1) return pickUpLoot();

		if (getFatiguePercent() >= MAX_FATIGUE) {
			setState(State.FATIGUE);
			return 0;
		}

		final Object dragon = getNearestNpcNotInCombat(NPC_ID_BLACK_DRAGON);

		if (dragon != null) {
			if (needToDrinkSuperAtkPot()) return consumeItemId(Potion.SUPER_ATTACK.getIds());
			if (needToDrinkSuperStrPot()) return consumeItemId(Potion.SUPER_STRENGTH.getIds());
			if (needToDrinkSuperDefPot()) return consumeItemId(Potion.SUPER_DEFENSE.getIds());
			return attack(dragon);
		}

		if (bot.isPrayerEnabled(PRAYER_ID_PARALYZE_MONSTER) && System.currentTimeMillis() > disablePrayerTimeout) {
			disableParalyzeMonster();
		}

		if ((nextRespawn != null && isAtCoordinate(nextRespawn)) || isAtCoordinate(COORD_WAIT_RESPAWN)) {
			return SLEEP_ONE_TICK;
		}

		int index;

		if ((canEat() && (index = getInventoryItemIndex(food.getId())) != -1) ||
			(canRestorePrayer() && (index = getInventoryItemIndex(Potion.PRAYER.getIds())) != -1)) {
			return consumeIndex(index);
		}

		final Coordinate coord = nextRespawn != null ? nextRespawn : COORD_WAIT_RESPAWN;
		walkTo(coord.getX(), coord.getY());
		return SLEEP_ONE_TICK;
	}

	private void updateLoot() {
		loot[0] = -1;
		int currentDistance = Integer.MAX_VALUE;

		for (int index = 0; index < bot.getGroundItemCount(); index++) {
			final int id = bot.getGroundItemId(index);

			if (!ITEM_IDS_LOOT.contains(id)) continue;

			final int x = getGroundItemX(index);
			final int y = getGroundItemY(index);

			if (!TBDArea.BLACK_DRAGONS.contains(x, y)) continue;

			final int distance = distanceTo(x, y);

			if (distance >= currentDistance) continue;
			currentDistance = distance;

			loot[0] = id;
			loot[1] = x;
			loot[2] = y;
		}
	}

	private int pickUpLoot() {
		if (!isInventoryFull() || (ITEM_IDS_LOOT_STACKABLE.contains(loot[0]) && hasInventoryItem(loot[0]))) {
			pickupItem(loot[0], loot[1], loot[2]);
			return SLEEP_ONE_TICK;
		}

		int index;

		if ((index = getInventoryItemIndex(ITEM_ID_VIAL)) != -1) {
			dropItem(index);
			return SLEEP_TWO_SECONDS;
		}

		if ((index = getInventoryItemIndex(food.getId())) != -1) return consumeIndex(index);

		return 0;
	}

	private int attack(final Object dragon) {
		if (distanceTo(dragon) <= MAX_DIST_FROM_DRAGON) {
			enableParalyzeMonster();
			attackNpc(dragon);
			disablePrayerTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return SLEEP_ONE_TICK;
		}

		disableParalyzeMonster();

		int index;
		if ((canEat() && (index = getInventoryItemIndex(food.getId())) != -1) ||
			(canRestorePrayer() && (index = getInventoryItemIndex(Potion.PRAYER.getIds())) != -1)) {
			return consumeIndex(index);
		}

		walkTo(dragon);
		return SLEEP_ONE_TICK;
	}

	private int fatigue() {
		if (!TBDArea.ISLAND.contains(playerX, playerY)) return climbUpLadder();
		disableParalyzeMonster();
		if (getFatiguePercent() != 0) return sleep();
		setState(State.KILL);
		return 0;
	}

	private int tradeWithMule() {
		if (!TBDArea.ISLAND.contains(playerX, playerY)) return climbUpLadder();

		if (isTradeConfirmOpen()) {
			if (!isTradeConfirmed()) confirmTrade();
			return 0;
		}
		if (isTradeAcceptOpen()) return masterAcceptTrade();

		disableParalyzeMonster();

		final int vialIndex = getInventoryItemIndex(ITEM_ID_VIAL);

		if (vialIndex != -1) {
			dropItem(vialIndex);
			return SLEEP_TWO_SECONDS;
		}

		if (getInventoryItemIdCount(food.getId()) < foodCount) return requestTrade();
		if (hasLoot()) return requestTrade();

		for (final Potion potion : Potion.VALUES) {
			if (!potion.isUsing()) continue;
			if (getInventoryItemIdCount(potion.getIds()) < potion.getCount()) return requestTrade();
		}

		final Object player = getPlayerObjByName(currentTrader);

		if (player == null || getX(player) != COORD_TRADE_MULE.getX() || getY(player) != COORD_TRADE_MULE.getY()) {
			setState(State.KILL);
			return 0;
		}

		if (System.currentTimeMillis() <= pmTimeout) return 0;

		bot.sendPrivateMessage("bank", currentTrader);
		pmTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
		return 0;
	}

	private int climbUpLadder() {
		if (isInCombat()) {
			final Coordinate ladder = TBDObject.LADDER_UP.getCoordinate();
			walkTo(ladder.getX(), ladder.getY() - 1);
			return SLEEP_ONE_TICK;
		}

		if (!bot.isEquipped(antiShieldIndex)) {
			equipItem(antiShieldIndex);
			return SLEEP_ONE_TICK;
		}

		if (needToEat()) {
			final int index = getInventoryItemIndex(food.getId());
			if (index != -1) return consumeIndex(index);
		}

		if (needToRestorePrayer()) {
			final int index = getInventoryItemIndex(Potion.PRAYER.getIds());
			if (index != -1) return consumeIndex(index);
		}

		final Coordinate ladder = TBDObject.LADDER_UP.getCoordinate();

		if (distanceTo(ladder.getX(), ladder.getY()) > 1) {
			final Object npc = getBlockingNpc();
			if (npc != null) {
				attackNpc(npc);
			} else {
				walkTo(ladder.getX(), ladder.getY() - 1);
			}
		} else {
			atObject(ladder.getX(), ladder.getY());
		}

		return SLEEP_ONE_TICK;
	}

	private Object getBlockingNpc() {
		final Coordinate ladder = TBDObject.LADDER_UP.getCoordinate();

		for (int index = 0; index < bot.getNpcCount(); index++) {
			final Object npc = bot.getNpc(index);
			final int x = getX(npc);
			final int y = getY(npc);
			if (distanceTo(x, y) != 1 || distanceTo(x, y, ladder.getX(), ladder.getY()) != 1) continue;
			return npc;
		}

		return null;
	}

	private int masterAcceptTrade() {
		if (getTradeItemCount() == Math.min(getInventoryLootCount(), MAX_TRADE_SIZE)) {
			if (isTradeRecipientAccepted()) {
				if (!isTradeAccepted()) acceptTrade();
				return 0;
			}

			if (System.currentTimeMillis() <= pmTimeout) return 0;
			bot.sendPrivateMessage(createSupplyRequest(), currentTrader);
			pmTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
			return 0;
		}

		for (int index = 0; index < getInventoryItemCount(); index++) {
			final int id = getInventoryItemId(index);
			if (!ITEM_IDS_LOOT.contains(id)) continue;
			int count = getInventoryItemIdCount(id);
			if (equipmentIndexes.contains(index)) count--;
			if (count == getTradeItemIdCount(id)) continue;
			offerTradeItem(index, count);
		}

		return SLEEP_ONE_TICK;
	}

	private int getInventoryLootCount() {
		int count = 0;

		for (int index = 0; index < getInventoryItemCount(); index++) {
			if (ITEM_IDS_LOOT.contains(getInventoryItemId(index)) && !equipmentIndexes.contains(index)) {
				count++;
			}
		}

		return count;
	}

	private String createSupplyRequest() {
		final int ppCount = Potion.PRAYER.isUsing() ?
			Potion.PRAYER.getCount() - getInventoryItemIdCount(Potion.PRAYER.getIds()) :
			0;

		final int supAtkCount = Potion.SUPER_ATTACK.isUsing() ?
			Potion.SUPER_ATTACK.getCount() - getInventoryItemIdCount(Potion.SUPER_ATTACK.getIds()) :
			0;

		final int supStrCount = Potion.SUPER_STRENGTH.isUsing() ?
			Potion.SUPER_STRENGTH.getCount() - getInventoryItemIdCount(Potion.SUPER_STRENGTH.getIds()) :
			0;

		final int supDefCount = Potion.SUPER_DEFENSE.isUsing() ?
			Potion.SUPER_DEFENSE.getCount() - getInventoryItemIdCount(Potion.SUPER_DEFENSE.getIds()) :
			0;

		final int foodCount = this.foodCount - getInventoryItemIdCount(food.getId());

		return String.format("%d,%d,%d,%d,%d", ppCount, supAtkCount, supStrCount, supDefCount, foodCount);
	}

	private int requestTrade() {
		if (!isAtCoordinate(COORD_TRADE_MASTER)) {
			walkTo(COORD_TRADE_MASTER.getX(), COORD_TRADE_MASTER.getY());
			return SLEEP_ONE_TICK;
		}

		if (System.currentTimeMillis() <= tradeReqTimeout) return 0;

		final Object mule = getPlayerObjByName(currentTrader);

		if (mule == null || getX(mule) != COORD_TRADE_MULE.getX() || getY(mule) != COORD_TRADE_MULE.getY()) {
			return 0;
		}

		sendTradeRequest(getServerIndex(mule));
		tradeReqTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int stop() {
		if (role == Role.MASTER) {
			if (!TBDArea.ISLAND.contains(playerX, playerY)) return climbUpLadder();
			disableParalyzeMonster();
		} else {
			traders.forEach(trader -> bot.sendPrivateMessage("stop", trader));
		}

		return exit("Script stopped unexpectedly.");
	}

	private int mule() {
		switch (state) {
			case BANK:
				return bank();
			case TRADE:
				return tradeWithMaster();
			case STOP:
				return stop();
			default:
				return exit(String.format("%s invalid state for role %s", state, role));
		}
	}

	private int bank() {
		if (isBankOpen()) {
			for (int index = 0; index < getInventoryItemCount(); index++) {
				final int itemId = getInventoryItemId(index);
				if (bot.isEquipped(index) || !ITEM_IDS_LOOT.contains(itemId)) continue;
				deposit(itemId, getInventoryItemIdCount(itemId));
				return SLEEP_ONE_TICK;
			}

			for (final Potion potion : Potion.VALUES) {
				if (!potion.isUsing()) continue;

				final int id = potion.getIds()[2];
				final int invCount = getInventoryItemIdCount(id);

				if (invCount >= potion.getCount()) continue;
				if (System.currentTimeMillis() <= potion.getTimeout()) return 0;

				if (!hasBankItem(id)) {
					potion.setUsing(false);

					if (potion == Potion.PRAYER) {
						setState(State.STOP);
						return 0;
					}

					continue;
				}

				withdraw(id, potion.getCount() - invCount);
				potion.setTimeout(System.currentTimeMillis() + TIMEOUT_TWO_SECONDS);
				return 0;
			}

			final int foodCount = getInventoryItemIdCount(food.getId());

			if (foodCount < this.foodCount) {
				if (System.currentTimeMillis() <= foodTimeout) return 0;

				if (!hasBankItem(food.getId())) {
					setState(State.STOP);
					return 0;
				}

				withdraw(food.getId(), this.foodCount - foodCount);
				foodTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
				return 0;
			}

			setState(State.TRADE);
			return 0;
		}

		if (TBDArea.BANK.contains(playerX, playerY)) return openBank();

		if (playerX < COORD_LOAD_BANK.getX()) {
			walkTo(COORD_LOAD_BANK.getX(), COORD_LOAD_BANK.getY());
			return SLEEP_ONE_TICK;
		}

		final Coordinate doors = TBDObject.BANK_DOORS.getCoordinate();

		if (isAtCoordinate(doors)) {
			if (getObjectId(doors.getX(), doors.getY()) == TBDObject.BANK_DOORS.getId()) {
				atObject(doors.getX(), doors.getY());
			} else {
				walkTo(doors.getX(), doors.getY() - 1);
			}
		} else {
			walkTo(doors.getX(), doors.getY());
		}

		return SLEEP_ONE_TICK;
	}

	private int tradeWithMaster() {
		if (isTradeConfirmOpen()) {
			if (!isTradeConfirmed()) confirmTrade();
			return 0;
		}
		if (isTradeAcceptOpen()) return slaveAcceptTrade();

		if (supplyRequest != null) supplyRequest = null;

		if (idle) {
			if (isAtCoordinate(COORD_TRADE_MULE)) {
				walkTo(COORD_TRADE_MULE.getX(), COORD_TRADE_MULE.getY() - 1);
				return SLEEP_ONE_TICK;
			}

			idle = false;
		}

		if (isAtCoordinate(COORD_TRADE_MULE)) return 0;

		if (TBDArea.BANK.contains(playerX, playerY)) {
			final Coordinate doors = TBDObject.BANK_DOORS.getCoordinate();

			if (getObjectId(doors.getX(), doors.getY()) == TBDObject.BANK_DOORS.getId()) {
				atObject(doors.getX(), doors.getY());
				return SLEEP_ONE_SECOND;
			}
		}

		walkTo(COORD_TRADE_MULE.getX(), COORD_TRADE_MULE.getY());
		return SLEEP_ONE_TICK;
	}

	private int slaveAcceptTrade() {
		if (isTradeAccepted()) return 0;

		if (getTradeItemCount() == MAX_TRADE_SIZE) {
			acceptTrade();
			return 0;
		}

		if (supplyRequest == null) return 0;

		for (final Potion potion : Potion.VALUES) {
			if (potion.isUsing()) continue;

			boolean declineTrade = false;

			switch (potion) {
				case PRAYER:
					if (supplyRequest.ppCount == 0) continue;
					declineTrade = true;
					break;
				case SUPER_ATTACK:
					if (supplyRequest.supAtkCount == 0) continue;
					declineTrade = true;
					break;
				case SUPER_STRENGTH:
					if (supplyRequest.supStrCount == 0) continue;
					declineTrade = true;
					break;
				case SUPER_DEFENSE:
					if (supplyRequest.supDefCount == 0) continue;
					declineTrade = true;
					break;
			}

			if (declineTrade) {
				traders.forEach(trader -> bot.sendPrivateMessage(potion.name(), trader));
				supplyRequest = null;
				declineTrade();
				return 0;
			}
		}

		for (final Potion potion : Potion.VALUES) {
			if (!potion.isUsing()) continue;

			final int count;

			switch (potion) {
				case PRAYER:
					count = supplyRequest.ppCount;
					break;
				case SUPER_ATTACK:
					count = supplyRequest.supAtkCount;
					break;
				case SUPER_STRENGTH:
					count = supplyRequest.supStrCount;
					break;
				case SUPER_DEFENSE:
					count = supplyRequest.supDefCount;
					break;
				default:
					return exit("Unsupported potion: " + potion);
			}

			final int id = potion.getIds()[2];

			if (getTradeItemIdCount(id) >= count) continue;
			if (System.currentTimeMillis() <= potion.getTimeout()) return 0;

			if (getInventoryItemIdCount(id) < count) {
				setState(State.BANK);
				return 0;
			}

			offerTradeItemId(id, count);
			potion.setTimeout(System.currentTimeMillis() + TIMEOUT_TWO_SECONDS);
			return 0;
		}

		final int count = supplyRequest.foodCount;

		if (getTradeItemIdCount(food.getId()) < count) {
			if (System.currentTimeMillis() <= foodTimeout) return 0;

			if (getInventoryItemIdCount(food.getId()) < count) {
				setState(State.BANK);
				return 0;
			}

			offerTradeItemId(food.getId(), count);
			foodTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			return 0;
		}

		acceptTrade();
		return 0;
	}

	private int consumeItemId(final int... itemIds) {
		final int inventoryIndex = getInventoryItemIndex(itemIds);
		if (inventoryIndex != -1) return consumeIndex(inventoryIndex);
		if (System.currentTimeMillis() <= consumeTimeout) return 0;
		setState(State.TRADE);
		return 0;
	}

	private int consumeIndex(final int inventoryIndex) {
		if (System.currentTimeMillis() <= consumeTimeout) return 0;
		useItem(inventoryIndex);
		consumeTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private boolean hasLoot() {
		for (int index = 0; index < getInventoryItemCount(); index++) {
			if (!equipmentIndexes.contains(index) && ITEM_IDS_LOOT.contains(getInventoryItemId(index))) return true;
		}

		return false;
	}

	private boolean canEat() {
		return getCurrentHits() <= eatAt;
	}

	private boolean needToEat() {
		return getCurrentHits() < minHits;
	}

	private boolean canRestorePrayer() {
		return Potion.PRAYER.isUsing() && getCurrentSkillLevel(Skill.PRAYER.getIndex()) < prayAt;
	}

	private boolean needToRestorePrayer() {
		return Potion.PRAYER.isUsing() && getCurrentSkillLevel(Skill.PRAYER.getIndex()) < minPrayer;
	}

	private boolean needToDrinkSuperAtkPot() {
		return Potion.SUPER_ATTACK.isUsing() && getCurrentSkillLevel(Skill.ATTACK.getIndex()) < minAtk;
	}

	private boolean needToDrinkSuperStrPot() {
		return Potion.SUPER_STRENGTH.isUsing() && getCurrentSkillLevel(Skill.STRENGTH.getIndex()) < minStr;
	}

	private boolean needToDrinkSuperDefPot() {
		return Potion.SUPER_DEFENSE.isUsing() && getCurrentSkillLevel(Skill.DEFENSE.getIndex()) < minDef;
	}

	public enum Role {
		MASTER("Master"),
		MULE("Mule");

		private final String desc;

		Role(final String desc) {
			this.desc = desc;
		}

		@Override
		public String toString() {
			return desc;
		}
	}

	private enum State {
		KILL("Kill"),
		FATIGUE("Fatigue"),
		TRADE("Trade"),
		BANK("Bank"),
		STOP("Stop");

		private final String description;

		State(final String description) {
			this.description = description;
		}
	}

	/**
	 * Mutable
	 */
	private enum Potion {
		PRAYER(new int[]{485, 484, 483}, "Prayer"),
		SUPER_ATTACK(new int[]{488, 487, 486}, "Super Atk"),
		SUPER_STRENGTH(new int[]{494, 493, 492}, "Super Str"),
		SUPER_DEFENSE(new int[]{497, 496, 495}, "Super Def");

		private static final Potion[] VALUES = Potion.values();

		private final int[] ids;
		private final String desc;

		private boolean using;

		private int count;
		private int sips;

		private long timeout;

		Potion(final int[] ids, final String desc) {
			this.ids = ids;
			this.desc = desc;
		}

		public void sip() {
			sips++;
		}

		public int[] getIds() {
			return ids;
		}

		public String getDesc() {
			return desc;
		}

		public boolean isUsing() {
			return using;
		}

		public void setUsing(final boolean using) {
			this.using = using;
		}

		public int getCount() {
			return count;
		}

		public void setCount(final int count) {
			this.count = count;
		}

		public int getSips() {
			return sips;
		}

		public long getTimeout() {
			return timeout;
		}

		public void setTimeout(final long timeout) {
			this.timeout = timeout;
		}
	}

	private enum Shield {
		NONE(-1, "None"),
		RUNE_KITE_SHIELD(404, "Rune Kite Shield"),
		DRAGON_SQUARE_SHIELD(1278, "Dragon Square Shield");

		private final int itemId;
		private final String desc;

		Shield(final int itemId, final String desc) {
			this.itemId = itemId;
			this.desc = desc;
		}

		public int getItemId() {
			return itemId;
		}

		public String getDesc() {
			return desc;
		}

		@Override
		public String toString() {
			return desc;
		}
	}

	private enum TBDArea implements RSArea {
		ISLAND(new Coordinate(409, 505), new Coordinate(417, 512)),
		BLACK_DRAGONS(new Coordinate(392, 3327), new Coordinate(422, 3346)),
		BANK(new Coordinate(437, 491), new Coordinate(443, 496));
		private final Coordinate lowerBoundingCoordinate;
		private final Coordinate upperBoundingCoordinate;

		TBDArea(final Coordinate lowerBoundingCoordinate, final Coordinate upperBoundingCoordinate) {
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

	private enum TBDObject implements RSObject {
		LADDER_DOWN(6, new Coordinate(412, 507)),
		LADDER_UP(5, new Coordinate(412, 3339)),
		BANK_DOORS(64, new Coordinate(439, 497));
		private final int id;
		private final Coordinate coordinate;

		TBDObject(final int id, final Coordinate coordinate) {
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

	private static final class SupplyRequest {
		private final int ppCount;
		private final int supAtkCount;
		private final int supStrCount;
		private final int supDefCount;
		private final int foodCount;

		public SupplyRequest(final int ppCount, final int supAtkCount, final int supStrCount, final int supDefCount,
							 final int foodCount) {
			this.ppCount = ppCount;
			this.supAtkCount = supAtkCount;
			this.supStrCount = supStrCount;
			this.supDefCount = supDefCount;
			this.foodCount = foodCount;
		}

		private static SupplyRequest parse(final String request) {
			final String[] data = request.split(",");

			if (data.length != 5) {
				throw new IllegalArgumentException(String.format("Invalid supply request length: %d", data.length));
			}

			try {
				return new SupplyRequest(Integer.parseInt(data[0]), Integer.parseInt(data[1]),
					Integer.parseInt(data[2]), Integer.parseInt(data[3]), Integer.parseInt(data[4]));
			} catch (final NumberFormatException e) {
				throw new IllegalArgumentException(String.format("Received malformed supply request: %s", request), e);
			}
		}

		public boolean equals(final Object o) {
			if (o == this) return true;
			if (!(o instanceof SupplyRequest)) return false;
			final SupplyRequest other = (SupplyRequest) o;
			if (getPpCount() != other.getPpCount()) return false;
			if (getSupAtkCount() != other.getSupAtkCount()) return false;
			if (getSupStrCount() != other.getSupStrCount()) return false;
			if (getSupDefCount() != other.getSupDefCount()) return false;
			return getFoodCount() == other.getFoodCount();
		}

		public int getPpCount() {
			return ppCount;
		}

		public int getSupAtkCount() {
			return supAtkCount;
		}

		public int getSupStrCount() {
			return supStrCount;
		}

		public int getSupDefCount() {
			return supDefCount;
		}

		public int getFoodCount() {
			return foodCount;
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			result = result * PRIME + getPpCount();
			result = result * PRIME + getSupAtkCount();
			result = result * PRIME + getSupStrCount();
			result = result * PRIME + getSupDefCount();
			result = result * PRIME + getFoodCount();
			return result;
		}

		public String toString() {
			return getPpCount() + "," + getSupAtkCount() + "," + getSupStrCount() + "," + getSupDefCount() + "," +
				getFoodCount();
		}
	}
}
