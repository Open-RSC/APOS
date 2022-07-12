import com.aposbot.Constants;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Trades items one-way between accounts at Shantay Pass Bank Chest.
 * <p>
 * Required:
 * Start script at Shantay Pass Bank Chest with both accounts logged in.
 * Bank stacks or quantities of a size less than 24 are skipped.
 * <p>
 *
 * @Author Chomp
 */
public class AA_ShantayTrader extends AA_Script {
	private static final Coordinate COORDINATE_BANK_CHEST = new Coordinate(58, 731);
	private static final String MESSAGE_OUT_OF_ITEMS = "stop";

	private static final long MAX_TRADE_DURATION = 5000L;

	private static final int TOTAL_TRADE_SIZE = MAX_TRADE_SIZE * 2;

	private final Queue<Item> items;

	private String tradePartner;
	private Item item;
	private long startTime;

	private long tradeTimeout;
	private long tradeRequestTimeout;
	private long tradeOfferTimeout;
	private long bankTimeout;
	private long bankOpenTimeout;
	private long bankCloseTimeout;

	private int prevYCoord;

	private boolean trader;
	private boolean idle;
	private boolean waitingForBank;
	private boolean tradeAcceptSent;
	private boolean tradeConfirmSent;
	private boolean initialized;

	public AA_ShantayTrader(final Extension ex) {
		super(ex);
		items = new LinkedList<>();
		tradePartner = "";
	}

	@Override
	public void init(final String parameters) {
		if (!bot.isLoggedIn()) {
			throw new IllegalStateException("Must be logged-in to start this script.");
		}

		createGUI();
	}

	@Override
	public int main() {
		if (!initialized) {
			stopScript();
			System.err.println("Error: Script was not initialized.");
			return 0;
		}

		if (trader) {
			if (bot.isBankVisible() || isInventoryEmpty()) {
				return bank();
			}

			return trade();
		}

		if (bot.getInventorySize() == TOTAL_TRADE_SIZE || bot.isBankVisible()) {
			return bank();
		}

		return trade();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("declined", 17)) {
			reset();
		} else if (message.endsWith("successfully")) {
			item.update();
			reset();
		} else if (message.endsWith("men.") || message.endsWith("you.")) {
			bankOpenTimeout = System.currentTimeMillis() + 5000L;
		} else if (message.endsWith("area")) {
			idle = true;
			prevYCoord = getY();
		} else if (message.startsWith("Unable")) {
			stopScript();
			System.err.println("Error: Could not PM trade partner.");
		} else if (message.startsWith("Other") || message.endsWith("objects")) {
			stopScript();
			System.err.println("Error: Inventories are out of sync.");
		}
	}

	private void reset() {
		tradeTimeout = 0L;
		tradeRequestTimeout = 0L;
		tradeOfferTimeout = 0L;

		bankTimeout = 0L;
		bankOpenTimeout = 0L;
		bankCloseTimeout = 0L;

		bot.setInTradeOffer(false);
		setTradeAccepted(false);

		bot.setInTradeConfirm(false);
		setTradeConfirmed(false);

		waitingForBank = false;
		tradeConfirmSent = false;
		tradeAcceptSent = false;
	}

	private void setTradeAccepted(final boolean accepted) {
		bot.Mi = accepted;
	}

	private void setTradeConfirmed(final boolean confirmed) {
		bot.Vi = confirmed;
	}

	private void createGUI() {
		final int playerCount = bot.getPlayerCount();

		final List<String> playerNames = new ArrayList<>();

		for (int i = 0; i < playerCount; i++) {
			final Object player = bot.getPlayer(i);

			if (player == null || player == bot.getPlayer()) {
				continue;
			}

			final String playerName = getPlayerName(player);

			if (playerName == null) {
				continue;
			}

			playerNames.add(playerName);
		}

		if (playerNames.isEmpty()) {
			System.err.println("Error: No players found. Is your trade partner logged in?");
			return;
		}

		playerNames.sort(String::compareToIgnoreCase);

		final JFrame jFrame = new JFrame("Shantay");

		final JComboBox<String> jComboBox = new JComboBox<>(playerNames.toArray(new String[0]));
		jComboBox.setPrototypeDisplayValue("############");
		final JLabel jLabelComboBox = new JLabel("Trade Partner:");
		jLabelComboBox.setLabelFor(jComboBox);

		final JTextArea jTextArea = new JTextArea();

		final JLabel jLabelItemId = new JLabel("Item Id:");
		final JTextField jTextFieldItemId = new JTextField();
		jLabelItemId.setLabelFor(jTextFieldItemId);
		final JLabel jLabelItemAmount = new JLabel("Amount:");
		final JTextField jTextFieldItemAmount = new JTextField();
		jLabelItemAmount.setLabelFor(jTextFieldItemAmount);
		final JButton jButtonAddItem = new JButton("Add");
		jButtonAddItem.addActionListener(l -> {
			final String itemId = jTextFieldItemId.getText();
			if (itemId == null || itemId.isEmpty()) {
				return;
			}

			final StringBuilder sb = new StringBuilder(itemId.trim());

			final String itemAmount = jTextFieldItemAmount.getText();
			if (itemAmount != null && !itemAmount.isEmpty()) {
				sb.append(":").append(itemAmount.trim());
			}

			sb.append(System.lineSeparator());

			jTextFieldItemId.setText("");
			jTextFieldItemAmount.setText("");

			jTextArea.append(sb.toString());
			jTextArea.setCaretPosition(jTextArea.getDocument().getLength());
		});

		final JPanel jPanelAddItem = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 0.05;
		jPanelAddItem.add(jLabelItemId, constraints);
		constraints.weightx = 0.35;
		constraints.fill = GridBagConstraints.BOTH;
		jPanelAddItem.add(jTextFieldItemId, constraints);
		constraints.weightx = 0.05;
		constraints.fill = GridBagConstraints.NONE;
		jPanelAddItem.add(jLabelItemAmount, constraints);
		constraints.weightx = 0.35;
		constraints.fill = GridBagConstraints.BOTH;
		jPanelAddItem.add(jTextFieldItemAmount, constraints);
		constraints.weightx = 0.20;
		constraints.fill = GridBagConstraints.NONE;
		jPanelAddItem.add(jButtonAddItem, constraints);

		final JScrollPane jScrollPane = new JScrollPane(jTextArea);

		final JCheckBox jCheckBox = new JCheckBox("I'm Receiving Items");
		jCheckBox.addActionListener(l -> {
			final boolean enable = !jCheckBox.isSelected();
			jComboBox.setEnabled(enable);
			jComboBox.setSelectedIndex(enable ? 0 : -1);
			jTextArea.setEnabled(enable);
			jTextArea.setText("");
			jTextFieldItemId.setEnabled(enable);
			jTextFieldItemId.setText("");
			jTextFieldItemAmount.setEnabled(enable);
			jTextFieldItemAmount.setText("");
			jButtonAddItem.setEnabled(enable);
		});

		final JPanel jPanelButton = new JPanel(new GridLayout());
		final JButton jButtonOk = new JButton("Ok");
		jButtonOk.addActionListener(l -> {
			trader = !jCheckBox.isSelected();

			if (trader) {
				tradePartner = (String) jComboBox.getSelectedItem();

				if (!isFriend(tradePartner)) {
					bot.addFriend(tradePartner);
				}

				try {
					final String textField = jTextArea.getText()
						.replaceAll("[\\r\\n]+", ",")
						.replace(' ', ',');

					final String[] items = textField.split(",");

					for (final String item : items) {
						final int cIdx = item.indexOf(':');

						if (cIdx == -1) {
							final int itemId = Integer.parseInt(item);
							this.items.add(new Item(itemId));
						} else {
							final int itemId = Integer.parseInt(item.substring(0, cIdx));
							final int amount = Integer.parseInt(item.substring(cIdx + 1));
							this.items.add(new Item(itemId, amount));
						}
					}

					if (this.items.isEmpty()) {
						System.err.println("Error: Empty item list.");
						return;
					}

					item = this.items.poll();

					System.out.println("- Trade Partner: " + tradePartner);
					System.out.println("- Trade Items: " + textField);
				} catch (final Exception e) {
					System.err.println("Error: Failed to parse item ids.");
					return;
				}
			} else {
				item = Item.createDummyItem();
			}

			startTime = System.currentTimeMillis();
			initialized = true;
			jFrame.dispose();
		});
		final JButton jButtonCancel = new JButton("Cancel");
		jButtonCancel.addActionListener(l -> jFrame.dispose());
		jPanelButton.add(jButtonOk);
		jPanelButton.add(jButtonCancel);

		final JPanel jPanel = new JPanel(new GridBagLayout());
		jPanel.setPreferredSize(new Dimension(300, 250));
		constraints = new GridBagConstraints();
		constraints.insets = new Insets(3, 5, 3, 5);
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridy = 0;
		constraints.weightx = 0.2;
		jPanel.add(jLabelComboBox, constraints);
		constraints.weightx = 0.8;
		jPanel.add(jComboBox, constraints);
		constraints.gridy++;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.weightx = 0.0;
		constraints.weighty = 1.0;
		jPanel.add(jScrollPane, constraints);
		constraints.gridy++;
		constraints.weightx = constraints.weighty = 0.0;
		jPanel.add(jPanelAddItem, constraints);
		constraints.gridy++;
		jPanel.add(jCheckBox, constraints);
		constraints.gridy++;
		jPanel.add(jPanelButton, constraints);

		jFrame.add(jPanel);
		jFrame.pack();
		jFrame.setMinimumSize(jFrame.getSize());
		jFrame.setResizable(false);
		jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		jFrame.setLocationRelativeTo(null);
		jFrame.toFront();
		jFrame.requestFocus();
		jFrame.setVisible(true);
	}

	private String getPlayerName(final Object player) {
		final String name = ((ta) player).c;

		if (name == null) {
			return null;
		}

		return name.replace((char) 160, ' ');
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		bot.drawString("@yel@Shantay Trader", PAINT_OFFSET_X, y, 1, 0);

		bot.drawString(String.format("@yel@Runtime: @whi@%s", toDuration(startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		bot.drawString(String.format("@yel@Role: @gr1@%s", trader ? "Trader" : "Receiver"),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

		bot.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (trader) {
			bot.drawString(String.format("@yel@Item: @whi@%s", item.getName()),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}

		bot.drawString(String.format("@yel@%s: @whi@%s @cya@(@whi@%s items@cya@/@whi@hr@cya@)",
				trader ? "Traded" : "Received", item.getTraded(),
				toUnitsPerHour(item.getTraded(), startTime)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, 1, 0);

		if (item.getRemaining() > 0) {
			bot.drawString(String.format("@yel@Remaining: @whi@%d", item.getRemaining()),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT * 2, 1, 0);

			bot.drawString(String.format("@yel@Est. Time: @whi@%s",
					toTimeToCompletion(item.getTraded(), item.getRemaining(), startTime)),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, 1, 0);
		}
	}

	@Override
	public void onTradeRequest(final String playerName) {
		if (tradePartner.isEmpty()) {
			tradePartner = playerName;
			System.out.println("- Trade Partner: " + tradePartner);
		}

		if (!playerName.equalsIgnoreCase(tradePartner) || isPlayerBusy()) {
			return;
		}

		final int playerIndex = getPlayerIndexByName(tradePartner);

		if (playerIndex == -1) {
			return;
		}

		sendRequestTrade(playerIndex);
	}

	@Override
	public void onPrivateMessage(final String message, final String playerName, final boolean moderator,
								 final boolean administrator) {
		if (playerName.equalsIgnoreCase(tradePartner)) {
			if (message.equalsIgnoreCase(MESSAGE_OUT_OF_ITEMS)) {
				stopScript();
				System.err.println("Error: Trade partner is out of items.");
			}
		}
	}

	@Override
	public void sendTradeRequest(final int serverIndex) {
		bot.createPacket(Constants.OP_PLAYER_TRADE);
		bot.put2(serverIndex);
		bot.finishPacket();
	}

	@Override
	public void acceptTrade() {
		bot.createPacket(Constants.OP_TRADE_ACCEPT);
		bot.finishPacket();
		setTradeAccepted(true);
	}

	@Override
	public void confirmTrade() {
		bot.createPacket(Constants.OP_TRADE_CONFIRM);
		bot.finishPacket();
		setTradeConfirmed(true);
	}

	@Override
	public void declineTrade() {
		bot.createPacket(Constants.OP_TRADE_DECLINE);
		bot.finishPacket();
		bot.setInTradeOffer(false);
		bot.setInTradeConfirm(false);
		setTradeAccepted(false);
		setTradeConfirmed(false);
	}

	private boolean isPlayerBusy() {
		return isTradeOpen() || bot.isBankVisible() || waitingForBank || idle;
	}

	private int getPlayerIndexByName(final String name) {
		for (int index = 0; index < bot.getPlayerCount(); index++) {
			final Object player = bot.getPlayer(index);

			if (player == null) {
				continue;
			}

			final String playerName = getPlayerName(player);

			if (name.equalsIgnoreCase(playerName)) {
				return index;
			}
		}

		return -1;
	}

	private int sendRequestTrade(final int playerIndex) {
		if (System.currentTimeMillis() <= tradeRequestTimeout) {
			return 0;
		}

		sendTradeRequest(getPlayerPID(playerIndex));
		tradeTimeout = System.currentTimeMillis() + MAX_TRADE_DURATION;
		tradeRequestTimeout = System.currentTimeMillis() + 2000L;
		return 0;
	}

	private int bank() {
		if (!bot.isBankVisible()) {
			return openBankChest();
		}

		if (trader) {
			if (System.currentTimeMillis() > bankTimeout && isInventoryEmpty()) {
				final int bankCount = getBankItemIdCount(item.getId());
				item.syncRemaining(bankCount);

				if (item.getRemaining() < TOTAL_TRADE_SIZE) {
					return pollNextItem();
				}

				withdraw(item.getId(), TOTAL_TRADE_SIZE);
				bankTimeout = System.currentTimeMillis() + 3000L;
			}
		} else {
			if (System.currentTimeMillis() > bankTimeout && !isInventoryEmpty()) {
				deposit(getInventoryId(0), TOTAL_TRADE_SIZE);
				bankTimeout = System.currentTimeMillis() + 3000L;
			}
		}

		return closeBankChest();
	}

	private int openBankChest() {
		if (System.currentTimeMillis() <= bankOpenTimeout) {
			return 0;
		}

		if (getX() - COORDINATE_BANK_CHEST.getX() > 1) {
			walkTo(COORDINATE_BANK_CHEST.getX() + 1, COORDINATE_BANK_CHEST.getY());
			return SLEEP_ONE_TICK;
		}

		useObject1(COORDINATE_BANK_CHEST.getX(), COORDINATE_BANK_CHEST.getY());
		waitingForBank = true;
		bankOpenTimeout = System.currentTimeMillis() + 2000L;
		return 0;
	}

	private int closeBankChest() {
		if (System.currentTimeMillis() <= bankCloseTimeout) {
			return 0;
		}

		bot.createPacket(Constants.OP_BANK_CLOSE);
		bot.finishPacket();
		bot.setBankVisible(false);

		waitingForBank = false;
		bankCloseTimeout = System.currentTimeMillis() + 2000L;
		return 0;
	}

	private int trade() {
		final int playerIndex = getPlayerIndexByName(tradePartner);

		if (playerIndex == -1) {
			if (isTradeOpen()) {
				declineTrade();
				reset();
			}

			return 0;
		}

		if (bot.isInTradeConfirm()) {
			return sendConfirmTrade();
		}

		if (bot.isInTradeOffer()) {
			if (trader &&
				System.currentTimeMillis() > tradeOfferTimeout &&
				bot.getLocalTradeItemCount() == 0) {
				bot.offerItemTrade(0, MAX_TRADE_SIZE);
				tradeOfferTimeout = System.currentTimeMillis() + 2000L;
			}

			return sendAcceptTrade();
		}

		if (idle) {
			return idle();
		}

		return sendRequestTrade(playerIndex);
	}

	private int sendAcceptTrade() {
		if (!tradeAcceptSent && !bot.hasLocalAcceptedTrade()) {
			if (trader || getRemoteTradeItemCount() > 0 || hasRemoteAcceptedTrade()) {
				acceptTrade();
				bot.displayMessage("@or1@Trade accepted.");
				tradeAcceptSent = true;
			}
		} else if (System.currentTimeMillis() > tradeTimeout) {
			declineTrade();
			reset();
		}

		return 0;
	}

	private int sendConfirmTrade() {
		if (!tradeConfirmSent && !bot.hasLocalConfirmedTrade()) {
			confirmTrade();
			bot.displayMessage("@or2@Trade confirmed.");
			tradeConfirmSent = true;
		} else if (System.currentTimeMillis() > tradeTimeout) {
			declineTrade();
			reset();
		}

		return 0;
	}

	private int idle() {
		if (getY() != prevYCoord) {
			idle = false;
			return 0;
		}

		if (prevYCoord == COORDINATE_BANK_CHEST.getY()) {
			walkTo(COORDINATE_BANK_CHEST.getX() + 1, COORDINATE_BANK_CHEST.getY() + 1);
		} else {
			walkTo(COORDINATE_BANK_CHEST.getX() + 1, COORDINATE_BANK_CHEST.getY());
		}

		return SLEEP_ONE_TICK;
	}

	private int pollNextItem() {
		final Item item = items.poll();

		if (item != null) {
			this.item = item;
			startTime = System.currentTimeMillis();
		} else {
			stopScript();
			sendPrivateMessage(MESSAGE_OUT_OF_ITEMS);
			System.err.println("Error: Out of items to trade.");
		}

		return 0;
	}

	private void sendPrivateMessage(final String message) {
		if (!isFriend(tradePartner)) {
			bot.addFriend(tradePartner);
		}

		bot.sendPrivateMessage(message, tradePartner);
	}

	private static final class Item {
		private static Item DUMMY;
		private final int id;
		private final String name;
		private int traded;
		private int remaining;

		private Item(final int id) {
			this(id, -1);
		}

		private Item(final int id, final int remaining) {
			this(id, remaining, getItemNameId(id));
		}

		private Item(final int id, final int remaining, final String name) {
			this.id = id;
			this.remaining = remaining;
			this.name = name;
		}

		private static Item createDummyItem() {
			if (DUMMY != null) {
				return DUMMY;
			}

			return (DUMMY = new Item(-1, -1, ""));
		}

		private int getId() {
			return id;
		}

		private String getName() {
			return name;
		}

		private int getTraded() {
			return traded;
		}

		private int getRemaining() {
			return remaining;
		}

		private void update() {
			traded += MAX_TRADE_SIZE;
			remaining = Math.max(0, remaining - MAX_TRADE_SIZE);
		}

		private void syncRemaining(final int bankCount) {
			if (remaining == -1 || remaining > bankCount) {
				remaining = bankCount;
			}
		}
	}
}
