package com.aposbot.gui;

import com.aposbot._default.IClient;
import com.aposbot._default.IClientInit;
import com.aposbot._default.IStaticAccess;
import com.aposbot.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Provides debug information about in-game entities.
 */
final class DebugFrame extends JFrame {
	private static final long serialVersionUID = 4433346449171745421L;

	private final IClient client;
	private final IClientInit clientInit;
	private final JTable table;

	private int selected;

	DebugFrame(final IClient client, final IClientInit clientInit) {
		super("APOS Debugger");

		this.client = client;
		this.clientInit = clientInit;

		setFont(Constants.UI_FONT);
		setIconImages(Constants.ICONS);

		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		final JComboBox<String> choice = new JComboBox<>();
		choice.setFont(Constants.UI_FONT);
		choice.addItem("NPCs");
		choice.addItem("Players");
		choice.addItem("Objects");
		choice.addItem("Wall objects");
		choice.addItem("Inventory items");
		choice.addItem("Ground items");
		choice.addItem("Skills");
		choice.addItem("Bank");
		choice.addItem("Shop");
		choice.addItem("Friends list");
		choice.addItem("Ignore list");
		choice.addItem("Local trade");
		choice.addItem("Remote trade");
		choice.addItemListener(e -> {
			selected = choice.getSelectedIndex();
			populate();
		});

		table = new JTable();
		table.setFont(Constants.UI_FONT);
		final JScrollPane scroll = new JScrollPane(table);
		scroll.setFont(Constants.UI_FONT);
		add(scroll, BorderLayout.CENTER);

		final JPanel panel = new JPanel();
		panel.add(choice);

		JButton button = new JButton("Refresh");
		button.setFont(Constants.UI_FONT);
		button.addActionListener(e -> populate());
		panel.add(button);
		button = new JButton("Hide");
		button.setFont(Constants.UI_FONT);
		button.addActionListener(e -> setVisible(false));
		panel.add(button);

		add(panel, BorderLayout.SOUTH);

		pack();
		setMinimumSize(getSize());
		final Insets in = getInsets();
		setSize(in.right + in.left + 480, in.top + in.bottom + 500);
	}

	private void populate() {
		final DecimalFormat decimalFormat = new DecimalFormat("#,##0");
		final IStaticAccess staticAccess = clientInit.getStaticAccess();
		final int count;
		int i;
		final String[] columns;
		Object[][] rows;
		switch (selected) {
			case 0:
				columns = new String[]{
					"Name", "ID", "sidx", "X", "Y"
				};
				count = client.getNpcCount();
				rows = new Object[count][columns.length];
				for (i = 0; i < count; i++) {
					final int id = client.getNpcId(client.getNpc(i));
					final Object[] row = rows[i];
					row[0] = staticAccess.getNpcName(id);
					row[1] = id;
					row[2] = client.getMobServerIndex(client.getNpc(i));
					row[3] = client.getMobLocalX(client.getNpc(i)) + client.getAreaX();
					row[4] = client.getMobLocalY(client.getNpc(i)) + client.getAreaY();
				}
				break;
			case 1:
				columns = new String[]{
					"Name", "sidx", "HP lvl (in cmb)", "X", "Y"
				};
				count = client.getPlayerCount();
				rows = new Object[count][columns.length];
				for (i = 0; i < count; i++) {
					final Object[] row = rows[i];
					row[0] = client.getPlayerName(client.getPlayer(i));
					row[1] = client.getMobServerIndex(client.getPlayer(i));
					row[2] = client.getMobBaseHitpoints(client.getPlayer(i));
					row[3] = client.getMobLocalX(client.getPlayer(i)) + client.getAreaX();
					row[4] = client.getMobLocalY(client.getPlayer(i)) + client.getAreaY();
				}
				break;
			case 2:
				columns = new String[]{
					"Name", "ID", "X", "Y"
				};
				count = client.getObjectCount();
				rows = new Object[count][columns.length];
				for (i = 0; i < count; i++) {
					final int id = client.getObjectId(i);
					final Object[] row = rows[i];
					row[0] = staticAccess.getObjectName(id);
					row[1] = id;
					row[2] = client.getObjectLocalX(i) + client.getAreaX();
					row[3] = client.getObjectLocalY(i) + client.getAreaY();
				}
				break;
			case 3:
				columns = new String[]{
					"Name", "ID", "X", "Y"
				};
				count = client.getBoundCount();
				rows = new Object[count][columns.length];
				for (i = 0; i < count; i++) {
					final int id = client.getBoundId(i);
					final Object[] row = rows[i];
					row[0] = staticAccess.getBoundName(id);
					row[1] = id;
					row[2] = client.getBoundLocalX(i) + client.getAreaX();
					row[3] = client.getBoundLocalY(i) + client.getAreaY();
				}
				break;
			case 4:
				columns = new String[]{
					"Name", "ID", "Stack", "Equipped"
				};
				count = client.getInventorySize();
				rows = new Object[count][columns.length];
				for (i = 0; i < count; i++) {
					final int id = client.getInventoryId(i);
					final Object[] row = rows[i];
					row[0] = staticAccess.getItemName(id);
					row[1] = id;
					row[2] = decimalFormat.format(client.getInventoryStack(i));
					row[3] = client.isEquipped(i);
				}
				break;
			case 5:
				columns = new String[]{
					"Name", "ID", "X", "Y"
				};
				count = client.getGroundItemCount();
				rows = new Object[count][columns.length];
				for (i = 0; i < count; i++) {
					final int id = client.getGroundItemId(i);
					final Object[] row = rows[i];
					row[0] = staticAccess.getItemName(id);
					row[1] = id;
					row[2] = client.getGroundItemLocalX(i) + client.getAreaX();
					row[3] = client.getGroundItemLocalY(i) + client.getAreaY();
				}
				break;
			case 6:
				final DecimalFormat dformat = new DecimalFormat("#,##0.0#");
				columns = new String[]{
					"Name", "Current", "Base", "XP"
				};
				count = staticAccess.getSkillNames().length;
				rows = new Object[count][columns.length];
				for (i = 0; i < count; i++) {
					final Object[] row = rows[i];
					row[0] = staticAccess.getSkillNames()[i];
					row[1] = client.getCurrentLevel(i);
					row[2] = client.getBaseLevel(i);
					row[3] = dformat.format(client.getExperience(i));
				}
				break;
			case 7:
				columns = new String[]{
					"Name", "ID", "Stack"
				};
				count = client.getBankSize();
				rows = new Object[count][columns.length];
				for (i = 0; i < count; i++) {
					final Object[] row = rows[i];
					final int id = client.getBankId(i);
					row[0] = staticAccess.getItemName(id);
					row[1] = id;
					row[2] = decimalFormat.format(client.getBankStack(i));
				}
				break;
			case 8:
				columns = new String[]{
					"Name", "ID", "Stack"
				};
				boolean noShopData = true;
				count = client.getShopSize();
				rows = new Object[count][columns.length];
				int skippedItems = 0;
				for (i = 0; i < count; i++) {
					final int id = client.getShopId(i);
					if (id < 0 || (id == 0 && client.getShopStack(i) == 0)) {
						skippedItems++;
						continue;
					}
					final Object[] row = rows[i - skippedItems];
					if (id != 0) {
						noShopData = false;
					}
					row[0] = staticAccess.getItemName(id);
					row[1] = id;
					row[2] = decimalFormat.format(client.getShopStack(i));
				}
				if (noShopData) {
					rows = new Object[0][columns.length];
				} else if (skippedItems > 0) {
					final Object[][] newRows = new Object[count - skippedItems][columns.length];
					if (count - skippedItems >= 0) System.arraycopy(rows, 0, newRows, 0, count - skippedItems);
					rows = newRows;
				}
				break;
			case 9:
				columns = new String[]{
					"Name"
				};
				count = staticAccess.getFriendCount();
				rows = new Object[count][columns.length];
				for (i = 0; i < count; ++i) {
					final Object[] row = rows[i];
					row[0] = staticAccess.getFriendName(i);
				}
				break;
			case 10:
				columns = new String[]{
					"Name"
				};
				count = staticAccess.getIgnoredCount();
				rows = new Object[count][columns.length];
				for (i = 0; i < count; ++i) {
					final Object[] row = rows[i];
					row[0] = staticAccess.getIgnoredName(i);
				}
				break;
			case 11:
				columns = new String[]{
					"Name", "ID", "Stack"
				};
				count = client.getLocalTradeItemCount();
				rows = new Object[count][columns.length];
				for (i = 0; i < count; ++i) {
					final Object[] row = rows[i];
					final int id = client.getLocalTradeItemId(i);
					row[0] = staticAccess.getItemName(id);
					row[1] = id;
					row[2] = client.getLocalTradeItemStack(i);
				}
				break;
			case 12:
				columns = new String[]{
					"Name", "ID", "Stack"
				};
				count = client.getRemoteTradeItemCount();
				rows = new Object[count][columns.length];
				for (i = 0; i < count; ++i) {
					final Object[] row = rows[i];
					final int id = client.getRemoteTradeItemId(i);
					row[0] = staticAccess.getItemName(id);
					row[1] = id;
					row[2] = client.getRemoteTradeItemStack(i);
				}
				break;
			default:
				rows = new Object[0][0];
				columns = new String[0];
				break;
		}
		table.setAutoCreateRowSorter(true);
		table.setModel(new DefaultTableModel(rows, columns));
	}

	@Override
	public void setVisible(final boolean visible) {
		if (visible) {
			populate();
			toFront();
			requestFocus();
		}
		super.setVisible(visible);
	}
}
