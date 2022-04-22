import com.aposbot.Constants;

import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Trades items one-way between accounts at Shantay Pass Bank.
 * <p>
 * Required:
 * Start script at Shantay Pass. Both the "Trader" and "Receiver" accounts should have empty inventories.
 * The "Trader" must have a lower pid than the "Receiver"!
 * <p>
 * Required Parameters (Trader account):
 * -r,--rsn <receiver>
 * -i,--itemid <#>
 * <p>
 * Example:
 * -r novar -i 1277 -i 795 -i 460
 * <p>
 * Notes:
 * Item stacks of a size less than 24 are skipped by default.
 * The -e,--experimental flag will enable trading of non-standard stack sizes.
 * Replace any spaces in an rsn with an underscore _.
 * Performance will suffer if the "Trader" and/or "Receiver" have high latency.
 * <p>
 * Author: Chomp
 */
public class AA_ShantayTrader extends AA_Script {
	private static final Coordinate COORDINATE_BANK_CHEST = new Coordinate(58, 731);

	private static final long MAX_TRADE_DURATION = 3000L;

	private static final int MAX_TRADE_SIZE = 12;
	private static final int TOTAL_TRADE_SIZE = MAX_TRADE_SIZE * 2;

	private int[] itemIds;
	private String rsn = "";
	private Instant startTime;

	private long tradeTimeout;
	private long tradeRequestTimeout;
	private long tradeOfferTimeout;

	private long bankTimeout;
	private long bankOpenTimeout;
	private long bankCloseTimeout;

	private int itemId;
	private int itemIdIndex;
	private int itemsTraded;
	private int itemsRemaining;

	private int previousPlayerY;

	private boolean trader;
	private boolean idle;

	private boolean waitingForBank;
	private boolean tradeAcceptSent;
	private boolean tradeConfirmSent;

	private boolean experimental;

	public AA_ShantayTrader(final Extension extension) {
		super(extension);
	}

	@Override
	public void init(final String parameters) {
		if (!parameters.isEmpty()) {
			List<Integer> ids = null;

			final String[] args = parameters.split(" ");

			for (int i = 0; i < args.length; i++) {
				switch (args[i].toLowerCase()) {
					case "-i":
					case "--itemid":
						if (ids == null) {
							ids = new ArrayList<>();
						}

						ids.add(Integer.parseInt(args[++i]));
						break;
					case "-r":
					case "--rsn":
						this.rsn = args[++i].replace('_', ' ');
						break;
					case "-e":
					case "--experimental":
						this.experimental = true;
						break;
					default:
						throw new IllegalArgumentException("Error: malformed parameters. Try again ...");
				}
			}

			if (ids != null) {
				this.itemIds = ids.stream().mapToInt(i -> i).toArray();
				this.itemId = this.itemIds[this.itemIdIndex];
			}
		}

		if (this.itemIds == null == !this.rsn.isEmpty()) {
			throw new IllegalArgumentException("Must specify both rsn AND item id(s).");
		}

		this.trader = !this.rsn.isEmpty();

		if (this.trader) {
			final int playerIndex = this.getPlayerIndexByName(this.rsn);

			if (playerIndex != -1) {
				final int traderPID = this.getPID();
				final int receiverPID = this.getPlayerPID(playerIndex);

				if (traderPID > receiverPID) {
					throw new IllegalStateException(
						String.format("You must have a lower pid (%d) than %s (%d)!", traderPID, this.rsn, receiverPID)
					);
				}
			}
		}

		this.startTime = Instant.now();
	}

	@Override
	public int main() {
		if (this.trader) {
			return this.extension.isBankVisible() || this.isInventoryEmpty() ? this.bank() : this.trade();
		}

		if (this.experimental) {
			final int inventoryCount = this.extension.getInventorySize();

			if (this.extension.isBankVisible() ||
				(!this.isTradeOpen() && inventoryCount > 0 && inventoryCount != MAX_TRADE_SIZE)) {
				return this.bank();
			}

			return this.trade();
		}

		return this.extension.getInventorySize() == TOTAL_TRADE_SIZE || this.extension.isBankVisible() ? this.bank() : this.trade();
	}

	@Override
	public void onServerMessage(final String message) {
		if (message.startsWith("declined", 17)) {
			this.reset();
		} else if (message.endsWith("successfully")) {
			this.itemsTraded += MAX_TRADE_SIZE;
			this.itemsRemaining -= MAX_TRADE_SIZE;
			this.reset();
		} else if (message.endsWith("men.") || message.endsWith("you.")) {
			this.bankOpenTimeout = System.currentTimeMillis() + TIMEOUT_FIVE_SECONDS;
		} else if (message.endsWith("area")) {
			this.idle = true;
			this.previousPlayerY = this.getY();
		}
	}

	@Override
	public void paint() {
		int y = PAINT_OFFSET_Y;

		this.drawString("@yel@Shantay Trader", PAINT_OFFSET_X, y, Font.BOLD, PAINT_COLOR);

		if (this.startTime == null) {
			return;
		}

		final long secondsElapsed = Duration.between(this.startTime, Instant.now()).getSeconds();

		this.drawString(String.format("@yel@Runtime: @whi@%s", getElapsedSeconds(secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Role: @gr1@%s", this.trader ? "Trader" : "Receiver"),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString(String.format("@yel@Pid: @cya@%d", this.getPID()),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.trader) {
			this.drawString(String.format("@yel@Item: @whi@%s", getItemNameId(this.itemId)),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}

		this.drawString(String.format("@yel@%s: @whi@%s @cya@(@whi@%s items@cya@/@whi@hr@cya@)",
				this.trader ? "Traded" : "Received", this.itemsTraded, getUnitsPerHour(this.itemsTraded, secondsElapsed)),
			PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

		if (this.itemsRemaining > 0) {
			this.drawString("", PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			this.drawString(String.format("@yel@Remaining: @whi@%d", this.itemsRemaining),
				PAINT_OFFSET_X, y += PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);

			this.drawString(String.format("@yel@Est. Time: @whi@%s",
					getTTL(this.itemsTraded, this.itemsRemaining, secondsElapsed)),
				PAINT_OFFSET_X, y + PAINT_OFFSET_Y_INCREMENT, Font.BOLD, PAINT_COLOR);
		}
	}

	@Override
	public void onTradeRequest(final String playerName) {
		if (this.rsn.isEmpty()) {
			this.rsn = playerName;
		}

		if (!playerName.equalsIgnoreCase(this.rsn) || this.isPlayerBusy()) {
			return;
		}

		final int playerIndex = this.getPlayerIndexByName(this.rsn);

		if (playerIndex == -1) {
			return;
		}

		this.requestTrade(playerIndex);
	}

	@Override
	public void declineTrade() {
		this.extension.createPacket(Constants.OP_TRADE_DECLINE);
		this.extension.finishPacket();
		this.extension.setInTradeOffer(false);
		this.extension.setInTradeConfirm(false);
		this.setTradeAccepted(false);
		this.setTradeConfirmed(false);
	}

	@Override
	public void walkTo(final int x, final int y) {
		this.extension.walkDirectly(x - this.extension.getAreaX(), y - this.extension.getAreaY(), false);
		this.extension.setActionInd(24);
	}

	@Override
	public void withdraw(final int id, final int amount) {
		this.extension.createPacket(Constants.OP_BANK_WITHDRAW);
		this.extension.put2(id);
		this.extension.put4(amount);
		this.extension.put4(305419896);
		this.extension.finishPacket();
	}

	@Override
	public void deposit(final int id, final int amount) {
		this.extension.createPacket(Constants.OP_BANK_DEPOSIT);
		this.extension.put2(id);
		this.extension.put4(amount);
		this.extension.put4(-2023406815);
		this.extension.finishPacket();
	}

	@Override
	public void acceptTrade() {
		this.extension.createPacket(Constants.OP_TRADE_ACCEPT);
		this.extension.finishPacket();
		this.setTradeAccepted(true);
	}

	@Override
	public void confirmTrade() {
		this.extension.createPacket(Constants.OP_TRADE_CONFIRM);
		this.extension.finishPacket();
		this.setTradeConfirmed(true);
	}

	@Override
	public void sendTradeRequest(final int serverIndex) {
		this.extension.createPacket(Constants.OP_PLAYER_TRADE);
		this.extension.put2(serverIndex);
		this.extension.finishPacket();
	}

	private int bank() {
		if (!this.extension.isBankVisible()) {
			return this.openBankChest();
		}

		if (this.trader) {
			if (System.currentTimeMillis() > this.bankTimeout && this.isInventoryEmpty()) {
				this.itemsRemaining = this.getBankItemIdCount(this.itemId);

				if (this.experimental) {
					if (this.itemsRemaining == 0 || this.itemsRemaining == MAX_TRADE_SIZE) {
						return this.getNextItem();
					}
				} else {
					if (this.itemsRemaining < TOTAL_TRADE_SIZE) {
						return this.getNextItem();
					}
				}

				this.withdraw(this.itemId, TOTAL_TRADE_SIZE);
				this.bankTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			}
		} else {
			if (System.currentTimeMillis() > this.bankTimeout && !this.isInventoryEmpty()) {
				this.deposit(this.getInventoryId(0), TOTAL_TRADE_SIZE);
				this.bankTimeout = System.currentTimeMillis() + TIMEOUT_THREE_SECONDS;
			}
		}

		return this.closeBankChest();
	}

	private int openBankChest() {
		if (System.currentTimeMillis() <= this.bankOpenTimeout) {
			return 0;
		}

		if (this.getX() - COORDINATE_BANK_CHEST.getX() > 1) {
			this.walkTo(COORDINATE_BANK_CHEST.getX() + 1, COORDINATE_BANK_CHEST.getY());
			return SLEEP_ONE_TICK;
		}

		this.extension.createPacket(Constants.OP_OBJECT_ACTION1);
		this.extension.put2(COORDINATE_BANK_CHEST.getX());
		this.extension.put2(COORDINATE_BANK_CHEST.getY());
		this.extension.finishPacket();

		this.waitingForBank = true;
		this.bankOpenTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int closeBankChest() {
		if (System.currentTimeMillis() <= this.bankCloseTimeout) {
			return 0;
		}

		this.extension.createPacket(Constants.OP_BANK_CLOSE);
		this.extension.finishPacket();
		this.extension.setBankVisible(false);

		this.waitingForBank = false;
		this.bankCloseTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int trade() {
		final int playerIndex = this.getPlayerIndexByName(this.rsn);

		if (playerIndex == -1) {
			if (this.isTradeOpen()) {
				this.declineTrade();
				this.reset();
			}

			return 0;
		}

		if (this.extension.isInTradeConfirm()) {
			return this.sendConfirmTrade();
		}

		if (this.extension.isInTradeOffer()) {
			if (this.trader &&
				System.currentTimeMillis() > this.tradeOfferTimeout &&
				this.extension.getLocalTradeItemCount() == 0) {
				this.extension.offerItemTrade(0, MAX_TRADE_SIZE);
				this.tradeOfferTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
			}

			return this.sendAcceptTrade();
		}

		if (this.idle) {
			return this.idle();
		}

		return this.requestTrade(playerIndex);
	}

	private int requestTrade(final int playerIndex) {
		if (System.currentTimeMillis() <= this.tradeRequestTimeout) {
			return 0;
		}

		this.sendTradeRequest(this.getPlayerPID(playerIndex));
		this.tradeTimeout = System.currentTimeMillis() + MAX_TRADE_DURATION;
		this.tradeRequestTimeout = System.currentTimeMillis() + TIMEOUT_TWO_SECONDS;
		return 0;
	}

	private int sendAcceptTrade() {
		if (!this.tradeAcceptSent && !this.extension.hasLocalAcceptedTrade()) {
			this.acceptTrade();
			this.extension.displayMessage("@or1@Trade accepted.");
			this.tradeAcceptSent = true;
		} else if (System.currentTimeMillis() > this.tradeTimeout) {
			this.declineTrade();
			this.reset();
		}

		return 0;
	}

	private int sendConfirmTrade() {
		if (!this.tradeConfirmSent && !this.extension.hasLocalConfirmedTrade()) {
			this.confirmTrade();
			this.extension.displayMessage("@or2@Trade confirmed.");
			this.tradeConfirmSent = true;
		} else if (System.currentTimeMillis() > this.tradeTimeout) {
			this.declineTrade();
			this.reset();
		}

		return 0;
	}

	private int idle() {
		if (this.getY() != this.previousPlayerY) {
			this.idle = false;
			return 0;
		}

		if (this.previousPlayerY == COORDINATE_BANK_CHEST.getY()) {
			this.walkTo(COORDINATE_BANK_CHEST.getX() + 1, COORDINATE_BANK_CHEST.getY() + 1);
		} else {
			this.walkTo(COORDINATE_BANK_CHEST.getX() + 1, COORDINATE_BANK_CHEST.getY());
		}

		return SLEEP_ONE_TICK;
	}

	private int getPlayerIndexByName(final String name) {
		Object player;
		String playerName;

		for (int index = 0; index < this.extension.getPlayerCount(); index++) {
			player = this.extension.getPlayer(index);

			if (player == null) {
				continue;
			}

			playerName = this.getPlayerName(player);

			if (name.equalsIgnoreCase(playerName)) {
				return index;
			}
		}

		return -1;
	}

	private String getPlayerName(final Object player) {
		final String name = ((ta) player).c;

		if (name == null) {
			return null;
		}

		return name.replace((char) 160, ' ');
	}

	private int getBankItemIdCount(final int itemId) {
		int count = 0;

		for (int index = 0; index < this.extension.getBankSize(); index++) {
			if (this.extension.getBankId(index) == itemId) {
				count += this.extension.getBankStack(index);
				break;
			}
		}

		return count;
	}

	private int getNextItem() {
		if (++this.itemIdIndex == this.itemIds.length) {
			return this.exit("Out of items to trade.");
		}

		this.itemId = this.itemIds[this.itemIdIndex];
		return 0;
	}

	private int getPID() {
		return this.extension.getMobServerIndex(this.extension.getPlayer());
	}

	private boolean isPlayerBusy() {
		return this.isTradeOpen() || this.extension.isBankVisible() || this.waitingForBank || this.idle;
	}

	private boolean isTradeOpen() {
		return this.extension.isInTradeOffer() || this.extension.isInTradeConfirm();
	}

	private void setTradeAccepted(final boolean accepted) {
		this.extension.Mi = accepted;
	}

	private void setTradeConfirmed(final boolean confirmed) {
		this.extension.Vi = confirmed;
	}

	private void reset() {
		this.tradeTimeout = 0L;
		this.tradeRequestTimeout = 0L;
		this.tradeOfferTimeout = 0L;

		this.bankTimeout = 0L;
		this.bankOpenTimeout = 0L;
		this.bankCloseTimeout = 0L;

		this.extension.setInTradeOffer(false);
		this.setTradeAccepted(false);

		this.extension.setInTradeConfirm(false);
		this.setTradeConfirmed(false);

		this.waitingForBank = false;
		this.tradeConfirmSent = false;
		this.tradeAcceptSent = false;
	}
}
