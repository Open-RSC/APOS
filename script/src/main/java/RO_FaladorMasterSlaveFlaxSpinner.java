import com.aposbot.StandardCloseHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * Falador flax spinner with one master and multiple slaves.
 * Select role and specify master/slave names in the GUI. Separate multiple slave names with comma.
 *
 * Master:
 * - Start at Falador spinning wheel house.
 * - Inventory must have exactly 24 slots reserved for flax/bow strings.
 * - Fill other 6 with random items.
 * - Start 24 flax/bow string in inventory.
 * - Make sure master can receive private messages from slaves.
 *
 * Slave:
 *  - Start at bank/spinning wheel house.
 *  - Inventory must have exactly 24 slots reserved for flax/bow strings.
 *  - Fill other 6 with random items.
 *  - Start with either 24 or 0 flax/bow strings in inventory.
 *  - Make sure slaves have master in their friends list.
 *
 * Author: Rene Ott
 */
public final class RO_FaladorMasterSlaveFlaxSpinner extends RO_Script implements ActionListener {

    private static final int PAINT_OFFSET_X = 212;
    private static final int PAINT_OFFSET_Y = 50;

    private static final String MESSAGE_TRADE_DONE = "trade_done";
    private static final String MESSAGE_RAN_OUT_OF_FLAX = "ran_out_of_flax";

    private static final RSArea HOUSE_AREA = new RSArea(new Position(295, 580), new Position(299, 577));
    private static final RSArea BANK_AREA = new RSArea(new Position(286, 573), new Position(280, 564));

    private static final RSObject HOUSE_DOOR = new RSObject(2, new Position(297, 577));
    private static final RSObject BANK_DOOR = new RSObject(64, new Position(287, 571));
    private static final RSObject BED = new RSObject(15, new Position(298, 579));
    private static final RSObject SPINNING_WHEEL = new RSObject(121, new Position(295, 579));

    private static final RSItem FLAX = new RSItem(675);
    private static final RSItem BOW_STRING = new RSItem(676);

    private static final int INVENTORY_FULL_ITEM_COUNT = 24;
    private static final int TRADE_FULL_ITEM_COUNT = 12;

    private JFrame frame;
    private JComboBox<String> roleComboBox;
    private JTextField playerNameTextField;
    private JLabel playerNameTextFieldLabel;
    private JButton okButton;

    private final PlayerState playerState = new PlayerState();

    private Role role;
    private String masterName;
    private Slave[] slaves;

    private int spunFlaxCount;
    private long startTime;

    public RO_FaladorMasterSlaveFlaxSpinner(Extension ex) {
        super(ex);
    }

    @Override
    public void init(String params) {
        initGui();
        startTime = System.currentTimeMillis();
    }

    @Override
    public int main() {
        switch (getState()) {
            case RAN_OUT_OF_SLAVES:
                return stopScript("Stopping script - ran out of slaves");
            case BANK:
                return bank();
            case SPIN:
                return spin();
            case TRADE:
                return trade();
            case WALK_TO_BANK:
                return walkToBank();
            case WALK_TO_SPIN:
                return walkToSpin();
            case WAIT_FOR_A_SLAVE:
                return waitForASlave();
            default:
                throw new IllegalStateException("Invalid state");
        }
    }

    private State getState() {
        if (isSlave()) {
            if (inArea(BANK_AREA)) {
                if (isInventoryEmptyOfStrings() && isInventoryFullOfFlax() && !isBanking()) {
                    return State.WALK_TO_SPIN;
                } else {
                    return State.BANK;
                }
            } else if (inArea(HOUSE_AREA)) {
                if (isFlaxInInventory() || playerState.isInTrade()) {
                    return State.TRADE;
                } else {
                    return State.WALK_TO_BANK;
                }
            } else if (isInventoryEmptyOfFlax()) {
                return State.WALK_TO_BANK;
            } else {
                return State.WALK_TO_SPIN;
            }
        } else {
            if (areAllSlavesInactive()) {
                return State.RAN_OUT_OF_SLAVES;
            }
            if (isInventoryFullOfStrings()) {
                if (isSlaveInSpinArea()) {
                    return State.TRADE;
                } else {
                    return State.WAIT_FOR_A_SLAVE;
                }
            } else if (playerState.isInTrade()) {
                return State.TRADE;
            } else {
                return State.SPIN;
            }
        }
    }

    private int walkToSpin() {
        if (isObjectReachable(BANK_DOOR)) {
            atObject(BANK_DOOR);
            return SLEEP_ONE_SECOND;
        }

        if (isWallObjectReachable(HOUSE_DOOR)) {
            atWallObject(HOUSE_DOOR);
            return SLEEP_ONE_SECOND;
        }

        walkTo(HOUSE_DOOR.getPosition().addY(1));
        return SLEEP_ONE_TICK;
    }

    private int walkToBank() {
        if (isWallObjectReachable(HOUSE_DOOR)) {
            atWallObject(HOUSE_DOOR);
            return SLEEP_ONE_SECOND;
        }

        if (isObjectReachable(BANK_DOOR)) {
            atObject(BANK_DOOR);
            return SLEEP_ONE_SECOND;
        }

        walkTo(BANK_DOOR.getPosition().addX(-1));
        return SLEEP_ONE_TICK;
    }

    private int spin() {
        if (getFatigue() > 90) {
            atObject(BED);
            return SLEEP_HALF_TICK;
        }

        if (isObjectReachable(SPINNING_WHEEL)) {
            useItemOnObjectByPosition(FLAX.getId(), SPINNING_WHEEL);
        }
        return SLEEP_HALF_TICK;
    }

    private int bank() {
        if (isBanking()) {
            if (isInventoryFullOfFlax()) {
                closeBank();
                BOW_STRING.resetCountUpdateFlag();
                return SLEEP_HALF_TICK;
            }

            int inventoryCount = getInventoryCount(BOW_STRING.getId());
            if (inventoryCount > 0) {
                BOW_STRING.incrementCount(inventoryCount);
                deposit(BOW_STRING.getId(), inventoryCount);
                return SLEEP_HALF_TICK;
            }

            int flaxCountInBank = bankCount(FLAX.getId());
            FLAX.setCount(flaxCountInBank);
            if (flaxCountInBank <= INVENTORY_FULL_ITEM_COUNT) {
                return stopScript("Stopping script - ran out of flax");
            }
            if (!isInventoryFullOfFlax()) {
                withdraw(FLAX.getId(), INVENTORY_FULL_ITEM_COUNT);
                return SLEEP_HALF_TICK;
            }

            return SLEEP_HALF_TICK;
        }

        return openBank();
    }

    private int trade() {
        if (isInTradeConfirm()) {
            confirmTrade();
            return SLEEP_HALF_TICK;
        }

        if (isSlave()) {
            return tradeGiveFlax();
        } else {
            return tradeTakeFlax();
        }
    }

    private int tradeGiveFlax() {
        if (isFlaxInInventory()) {
            playerState.setInTrade(true);
        }

        if (isInventoryEmptyOfFlax()) {
            sendPrivateMessage(MESSAGE_TRADE_DONE, masterName);
            playerState.setInTrade(false);
            return SLEEP_HALF_TICK;
        }

        if (isInTradeOffer()) {
            if (getLocalTradeItemCount() < TRADE_FULL_ITEM_COUNT) {
                int slot = getInventoryIndex(FLAX.getId());
                if (slot == -1) {
                    System.out.println("Error: In trade, we don't have any flax");
                    return SLEEP_HALF_TICK;
                }

                int amount = getInventoryCount(FLAX.getId());
                offerItemTrade(slot, amount);
                return SLEEP_HALF_TICK;
            } else {
                if (getRemoteTradeItemCount() == TRADE_FULL_ITEM_COUNT) {
                    if (hasLocalAcceptedTrade()) {
                        return SLEEP_HALF_TICK;
                    } else {
                        acceptTrade();
                        return SLEEP_HALF_TICK;
                    }
                } else {
                    System.out.printf("Waiting for the master [%s] to offer bow strings%n", masterName);
                    return SLEEP_TWO_SECONDS;
                }
            }
        } else {
            int[] master = getPlayerByName(masterName);
            if (master[0] == -1 || !inArea(Position.createFrom(master), HOUSE_AREA)) {
                return SLEEP_ONE_SECOND;
            }

            sendTradeRequest(getPlayerPID(master[0]));
            return SLEEP_ONE_SECOND;
        }
    }

    private int tradeTakeFlax() {
        if (isStringInInventory()) {
            playerState.setSlaveInTrade(playerState.getSlaveInTrade());
        }

        if (isInventoryEmptyOfStrings()) {
            playerState.setSlaveInTrade(null);
            return SLEEP_HALF_TICK;
        }

        if (isInTradeOffer()) {
            if (getLocalTradeItemCount() < TRADE_FULL_ITEM_COUNT) {
                int slot = getInventoryIndex(BOW_STRING.getId());
                if (slot == -1) {
                    System.out.println("Error: In trade, but we don't have any bow strings");
                    return SLEEP_TWO_SECONDS;
                }

                int amount = getInventoryCount(BOW_STRING.getId());
                offerItemTrade(slot, amount);
                return SLEEP_HALF_TICK;
            } else {
                if (hasRemoteAcceptedTrade()) {
                    acceptTrade();
                    return SLEEP_HALF_TICK;
                }
                System.out.printf("Waiting for the slave [%s] to accept the trade offer%n", playerState.getSlaveInTrade());
                return SLEEP_ONE_SECOND;
            }
        }

        System.out.println("Waiting for a slave to send a trade request");
        return SLEEP_ONE_SECOND;
    }

    private int waitForASlave() {
        System.out.println("Waiting for a slave to reach in the house");
        return SLEEP_ONE_SECOND;
    }

    public int stopScript(String message) {
        if (isSlave()) {
            sendPrivateMessage(masterName, MESSAGE_RAN_OUT_OF_FLAX);
        }
        setAutoLogin(false);
        stopScript();

        return SLEEP_ONE_SECOND;
    }

    private boolean areAllSlavesInactive() {
        return Arrays.stream(slaves).noneMatch(it -> it.active);
    }

    private boolean isSlave() {
        return role == Role.SLAVE;
    }

    private boolean isInventoryEmptyOfFlax() {
        return getInventoryCount(FLAX.getId()) == 0;
    }

    private boolean isInventoryFullOfFlax() {
        return getInventoryCount(FLAX.getId()) == INVENTORY_FULL_ITEM_COUNT;
    }

    private boolean isInventoryEmptyOfStrings() {
        return getInventoryCount(BOW_STRING.getId()) == 0;
    }

    private boolean isInventoryFullOfStrings() {
        return getInventoryCount(BOW_STRING.getId()) == INVENTORY_FULL_ITEM_COUNT;
    }

    private boolean isSlaveInSpinArea() {
        return Arrays.stream(slaves).anyMatch(it -> {
            int[] player = getPlayerByName(it.name);
            return player[0] != -1 && inArea(new Position(player[1], player[2]), HOUSE_AREA);
        });
    }

    private boolean isFlaxInInventory() {
        return getInventoryCount(FLAX.getId()) > 0;
    }

    private boolean isStringInInventory() {
        return getInventoryCount(BOW_STRING.getId()) > 0;
    }

    @Override
    public void onTradeRequest(String name) {
        if (isSlave()) {
            return;
        }

        boolean isUnknownPlayersTradeRequest = Arrays
            .stream(slaves)
            .noneMatch(it -> it.name.equalsIgnoreCase(name));

        if (isUnknownPlayersTradeRequest) {
            return;
        }

        boolean isMasterBusySpinningFlax = isFlaxInInventory() && !playerState.isInTrade();
        if (isMasterBusySpinningFlax) {
            return;
        }

        String currentSlaveInTrade = playerState.getSlaveInTrade();

        boolean ignoreAnotherSlavesTradeRequest = currentSlaveInTrade != null && !name.equalsIgnoreCase(currentSlaveInTrade);
        if (ignoreAnotherSlavesTradeRequest) {
            return;
        }

        String tradingSlaveName = currentSlaveInTrade != null
                ? currentSlaveInTrade
                : name;

        int[] slave = getPlayerByName(tradingSlaveName);

        boolean isSlaveUnreachableForTrade = slave[0] == -1 || !inArea(Position.createFrom(slave), HOUSE_AREA);
        if (isSlaveUnreachableForTrade) {
            return;
        }

        sendTradeRequest(getPlayerPID(slave[0]));
        playerState.setSlaveInTrade(tradingSlaveName);
    }

    @Override
    public void onPrivateMessage(String msg, String name, boolean mod, boolean admin) {
        if (isSlave()) {
            return;
        }

        boolean isUnknownPlayersPrivateMessage = Arrays.stream(slaves).noneMatch(it -> it.name.equalsIgnoreCase(name));
        if (isUnknownPlayersPrivateMessage) {
            return;
        }

        if (msg.equalsIgnoreCase(MESSAGE_RAN_OUT_OF_FLAX)) {
            Optional<Slave> slave = Arrays.stream(slaves).filter(it -> it.name.equalsIgnoreCase(name)).findFirst();
            if (!slave.isPresent()) {
                return;
            }
            slave.get().setInactive();

        } else if (msg.equalsIgnoreCase(MESSAGE_TRADE_DONE)) {
            String currentTradingSlaveName = playerState.getSlaveInTrade();

            boolean ignoreTradeDoneMessageFromSlave = currentTradingSlaveName == null || !currentTradingSlaveName.equalsIgnoreCase(name);
            if (ignoreTradeDoneMessageFromSlave) {
                return;
            }

            playerState.setSlaveInTrade(null);
        }
    }

    @Override
    public void onServerMessage(String message) {
        if (message.contains("bow string")) {
            spunFlaxCount++;
        }
    }

    @Override
    public void paint() {
        int x = PAINT_OFFSET_X;
        int y = PAINT_OFFSET_Y;

        drawString(x, y, "@gre@%s (@whi@%s@gre@)", getScriptName(), getRunningTime(startTime));

        y += PAINT_OFFSET_Y_HALF_INCREMENT;
        drawHLine(x, y, 240, PAINT_COLOR);
        y += PAINT_OFFSET_Y_FULL_INCREMENT;

        if (isSlave()) {
            drawString(x, y, "@gre@Flax in bank: @whi@%s@gre@", FLAX.getCount());
            y += PAINT_OFFSET_Y_FULL_INCREMENT;
            drawString(x, y, "@gre@Master: @whi@%s", masterName);
        } else {
            drawString(x, y, "@gre@Flax spun: @whi@%s @gre@(@whi@%s/h@gre@)", spunFlaxCount, getCountPerHour(spunFlaxCount, startTime));
            y += PAINT_OFFSET_Y_FULL_INCREMENT;
            double inThousand = 15.0 * spunFlaxCount / 1000;
            drawString(x, y, "@gre@XP gained: @whi@%sK @gre@(@whi@%sK/h@gre@)", DECIMAL_FORMAT.format(inThousand), getCountPerHour(inThousand, startTime));
            y += PAINT_OFFSET_Y_HALF_INCREMENT;
            drawHLine(x, y, 240, PAINT_COLOR);
            y += PAINT_OFFSET_Y_FULL_INCREMENT;
            drawString(x, y, "@gre@Slaves:");
            for (Slave slave: getOrderedSlaves()) {
                y += PAINT_OFFSET_Y_FULL_INCREMENT;
                drawString(x, y, " @gre@ - %s%s", slave.isActive() ? "@whi@" : "@or1@", getSlaveInTradeText(slave.getName()));
            }
        }
    }

    private String getSlaveInTradeText(String slaveName) {
        if (playerState.slaveInTradeName == null) {
            return slaveName;
        }

        return String.format("[%s]", slaveName);
    }

    private Slave[] getOrderedSlaves() {
        return Arrays.stream(slaves).sorted(Comparator.comparing(Slave::isActive).reversed()).toArray(Slave[]::new);
    }

    private void initGui() {
        frame = new JFrame(getScriptName());
        frame.setMinimumSize(new Dimension(300, 100));
        frame.addWindowListener(new StandardCloseHandler(frame, StandardCloseHandler.HIDE));
        JComponent contentPane = (JComponent) frame.getContentPane();
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        frame.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        JPanel rolePanel = new JPanel(new GridLayout(0, 2, 5, 5));
        rolePanel.add(new JLabel("Role:"));
        roleComboBox = new JComboBox<>();
        roleComboBox.addItem(Role.MASTER.getLabel());
        roleComboBox.addItem(Role.SLAVE.getLabel());
        roleComboBox.addActionListener(this);
        rolePanel.add(roleComboBox);

        JPanel playerNamePanel = new JPanel(new GridLayout(0, 2, 5, 5));
        playerNameTextFieldLabel = new JLabel("Slaves:");
        playerNamePanel.add(playerNameTextFieldLabel);
        playerNameTextField = new JTextField();
        playerNamePanel.add(playerNameTextField);

        JPanel buttonPanel = new JPanel();
        okButton = new JButton("OK");
        okButton.addActionListener(this);
        buttonPanel.add(okButton);

        frame.add(rolePanel);
        frame.add(playerNamePanel);
        frame.add(buttonPanel);

        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.toFront();
        frame.requestFocus();
        frame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Role role = Role.valueFromLabel((String) Objects.requireNonNull(roleComboBox.getSelectedItem()));

        if (e.getSource() == roleComboBox) {
            playerNameTextFieldLabel.setText(role == Role.MASTER ? "Slaves:" : "Master:");
            playerNameTextField.setText("");
        }

        if (e.getSource() == okButton) {
            this.role = role;

            if (role == Role.MASTER) {
                slaves = Arrays.stream(playerNameTextField.getText().split(","))
                        .map(Slave::new)
                        .toArray(Slave[]::new);
            } else {
                masterName = playerNameTextField.getText();
            }
            frame.setVisible(false);
        }
    }

    private enum State {
        RAN_OUT_OF_SLAVES,
        SPIN,
        TRADE,
        BANK,
        WALK_TO_BANK,
        WALK_TO_SPIN,
        WAIT_FOR_A_SLAVE,
    }

    private enum Role {
        MASTER("Master"),
        SLAVE("Slave");

        private final String label;

        Role(String label) {
            this.label = label;
        }

        String getLabel() {
            return label;
        }

        public static Role valueFromLabel(String label) {
            return Role.valueOf(label.toUpperCase());
        }
    }

    private static final class PlayerState {
        private boolean isTrading;
        private String slaveInTradeName;

        public void setSlaveInTrade(String name) {
            slaveInTradeName = name;
            isTrading = name != null;
        }

        public String getSlaveInTrade() {
            return slaveInTradeName;
        }

        public void setInTrade(boolean trading) {
            isTrading = trading;
        }

        public boolean isInTrade() {
            return isTrading;
        }
    }

    private static final class Slave {
        public boolean active;
        public String name;

        public Slave(String name) {
            this.name = name;
            this.active = true;
        }

        public String getName() {
            return name;
        }

        private boolean isActive() {
            return active;
        }

        public void setInactive() {
            active = false;
        }
    }
}
