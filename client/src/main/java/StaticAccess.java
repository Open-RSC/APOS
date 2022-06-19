import com.aposbot.Constants;
import com.aposbot._default.IStaticAccess;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Provides access to static fields in the client.
 */
final class StaticAccess implements IStaticAccess {
	private static final String[] SKILL_NAMES = {
		"Attack", "Defense", "Strength", "Hits", "Ranged", "Prayer", "Magic",
		"Cooking", "Woodcut", "Fletching", "Fishing", "Firemaking", "Crafting",
		"Smithing", "Mining", "Herblaw", "Agility", "Thieving"
	};

	private static final String[] SPELL_NAMES = {
		"Wind strike", "Confuse", "Water strike", "Enchant lvl-1 amulet",
		"Earth strike", "Weaken", "Fire strike", "Bones to bananas",
		"Wind bolt", "Curse", "Low level alchemy", "Water bolt",
		"Varrock teleport", "Enchant lvl-2 amulet", "Earth bolt",
		"Lumbridge teleport", "Telekinetic grab", "Fire bolt",
		"Falador teleport", "Crumble undead", "Wind blast", "Superheat item",
		"Camelot teleport", "Water blast", "Enchant lvl-3 amulet",
		"Iban blast", "Ardougne teleport", "Earth blast", "High level alchemy",
		"Charge Water Orb", "Enchant lvl-4 amulet", "Watchtower teleport",
		"Fire blast", "Claws of Guthix", "Saradomin strike",
		"Flames of Zamorak", "Charge earth Orb", "Wind wave",
		"Charge Fire Orb", "Water wave", "Charge air Orb", "Vulnerability",
		"Enchant lvl-5 amulet", "Earth wave", "Enfeeble", "Fire wave", "Stun",
		"Charge"
	};

	private static final StaticAccess instance = new StaticAccess();

	static void setStrings() {
		ac.x[165] += " (Guam Leaf)";
		ac.x[435] += " (Marrentill)";
		ac.x[436] += " (Tarromin)";
		ac.x[437] += " (Harralander)";
		ac.x[438] += " (Ranarr Weed)";
		ac.x[439] += " (Irit Leaf)";
		ac.x[440] += " (Avantoe)";
		ac.x[441] += " (Kwuarm)";
		ac.x[442] += " (Cadantine)";
		ac.x[443] += " (Dwarf Weed)";
		ac.x[933] += " (Torstol)";
		ac.x[221] += " (4)";
		ac.x[222] += " (3)";
		ac.x[223] += " (2)";
		ac.x[224] += " (1)";
		ac.x[454] = "Unf. (Guam)";
		ac.x[455] = "Unf. (Marrentill)";
		ac.x[456] = "Unf. (Tarromin)";
		ac.x[457] = "Unf. (Harralander)";
		ac.x[458] = "Unf. (Ranarr)";
		ac.x[459] = "Unf. (Irit)";
		ac.x[460] = "Unf. (Avantoe)";
		ac.x[461] = "Unf. (Kwuarm)";
		ac.x[462] = "Unf. (Cadantine)";
		ac.x[463] = "Unf. (Dwarfweed)";
		ac.x[935] = "Unf. (Torstol)";
		for (int i = 0; i < 9; ++i) {
			ac.x[474 + (i * 3)] += " (3)";
			ac.x[474 + (i * 3) + 1] += " (2)";
			ac.x[474 + (i * 3) + 2] += " (1)";
		}
		for (int i = 0; i < 2; ++i) {
			ac.x[566 + (i * 3)] += " (3)";
			ac.x[566 + (i * 3) + 1] += " (2)";
			ac.x[566 + (i * 3) + 2] += " (1)";
		}
		ac.x[963] += " (3)";
		ac.x[964] += " (2)";
		ac.x[965] += " (1)";
	}

	static StaticAccess getInstance() {
		return instance;
	}

	static boolean loadFont(final e game, String name, final String replacement, final int index) {
		// Modification of client's qa.a
		// added "replacement" argument
		boolean flag = false;
		name = name.toLowerCase();
		boolean flag1 = false;
		if (name.startsWith("helvetica")) {
			name = name.substring(9);
		}
		if (name.startsWith("h")) {
			name = name.substring(1);
		}
		if (name.startsWith("f")) {
			name = name.substring(1);
			flag = true;
		}
		if (name.startsWith("d")) {
			name = name.substring(1);
			flag1 = true;
		}
		if (name.endsWith(".jf")) {
			name = name.substring(0, -3 + name.length());
		}
		int k1 = Font.PLAIN;
		if (name.endsWith("b")) {
			k1 = Font.BOLD;
			name = name.substring(0, name.length() - 1);
		}
		if (name.endsWith("p")) {
			name = name.substring(0, -1 + name.length());
		}
		final int size = Integer.parseInt(name);
		final Font font = new Font(replacement, k1, size);
		final FontMetrics fontmetrics = game.getFontMetrics(font);
		final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"\243$%^&*()-_=+[{]};:'@#~,<.>/?\\| ";
		b.c = 855;
		for (int i2 = 0; -96 < ~i2; i2++) {
			if (!s.a(index, font, i2, -95, game, characters.charAt(i2),
				fontmetrics, flag1)) {
				return false;
			}
		}

		m.b[index] = new byte[b.c];
		for (int j2 = 0; ~j2 > ~b.c; j2++) {
			m.b[index][j2] = qb.k[j2];
		}

		if (1 == k1 && fb.k[index]) {
			fb.k[index] = false;
			if (!loadFont(game, "f" + size + "p", replacement, index)) {
				return false;
			}
		}
		if (flag && !fb.k[index]) {
			fb.k[index] = false;
			return loadFont(game, "d" + size + "p", replacement, index);
		}
		return true;
	}

	static byte[] loadContentCrcsHook(final URL url, final boolean arg1, final boolean arg2) throws IOException {
		final boolean var5 = client.vh;

		++da.L;

		final String fileName = "contentcrcs";
		final Path path = Constants.PATH_CONTENT.resolve(fileName);

		final byte[] data;

		if (Files.exists(path)) {
			data = Files.readAllBytes(path);
		} else {
			data = da.a(url, arg1, arg2);
			System.out.printf("Loaded from url: %s%n", fileName);
		}

		return data;
	}

	static byte[] loadContentHook(final int arg0, final String name, final int arg1, final int index) throws IOException {
		ib.b++;

		if (la.g[index] != null) {
			return la.g[index];
		}

		nb.q = arg1;

		o.l = name;

		if (m.e != null) {
			final byte[] data = m.e.a(9395, index);

			if (data != null && mb.a(data, data.length, 0) == tb.l[index]) {
				la.g[index] = k.a(128, true, data);

				return la.g[index];
			}
		}

		final String fileName = ib.z[5] + index + "_" + Long.toHexString(tb.l[index]);
		final Path path = Constants.PATH_CONTENT.resolve(fileName);

		byte[] data = null;

		for (int j1 = 0; ~j1 > -4; j1++) {
			if (Files.exists(path)) {
				data = Files.readAllBytes(path);
			} else {
				data = da.a(new URL(ib.c, fileName), true, true);

				Files.write(path, data);
				System.out.printf("Loaded from url: %s%n", name);
			}

			if (~mb.a(data, Objects.requireNonNull(data).length, 0) != ~tb.l[index]) {
				continue;
			}

			if (m.e != null) {
				m.e.a(index, data.length, -97, data);
			}

			la.g[index] = k.a(128, true, data);

			return la.g[index];
		}

		if (data != null) {
			final StringBuilder stringbuilder =
				new StringBuilder(ib.z[4] + index + ib.z[3] + tb.l[index]);

			stringbuilder.append(ib.z[2]).append(data.length);

			for (int k1 = 0; ~data.length < ~k1 && -6 < ~k1; k1++) {
				stringbuilder.append(" ").append(data[k1]);
			}

			throw new IOException(stringbuilder.toString());
		} else {
			throw new IOException(ib.z[4] + index + ib.z[3] + tb.l[index]);
		}
	}

	@Override
	public String getNpcName(final int id) {
		return e.Mb[id];
	}

	@Override
	public String getNpcDesc(final int id) {
		return ba.ac[id];
	}

	@Override
	public int getNpcLevel(final int id) {
		return ((eb.b[id] + (la.a[id] + jb.k[id])) + fb.d[id]) / 4;
	}

	@Override
	public String getItemName(final int id) {
		return ac.x[id];
	}

	@Override
	public String getItemDesc(final int id) {
		return ga.b[id];
	}

	@Override
	public String getItemCommand(final int id) {
		return lb.ac[id];
	}

	@Override
	public int getItemBasePrice(final int id) {
		return kb.b[id];
	}

	@Override
	public boolean isItemStackable(final int id) {
		return fa.e[id] != 1;
	}

	@Override
	public boolean isItemTradable(final int id) {
		return kb.c[id] != 1;
	}

	@Override
	public String getObjectName(final int id) {
		return l.a[id];
	}

	@Override
	public String getObjectDesc(final int id) {
		return la.f[id];
	}

	@Override
	public String getBoundName(final int id) {
		return ta.r[id];
	}

	@Override
	public String getBoundDesc(final int id) {
		return ub.b[id];
	}

	@Override
	public int getSpellReqLevel(final int id) {
		return pa.f[id];
	}

	@Override
	public int getSpellType(final int i) {
		return qb.e[i];
	}

	@Override
	public int getReagentCount(final int id) {
		return o.p[id];
	}

	@Override
	public int getReagentId(final int spell, final int i) {
		return oa.d[spell][i];
	}

	@Override
	public int getReagentAmount(final int spell, final int i) {
		return da.J[spell][i];
	}

	@Override
	public int getFriendCount() {
		return n.g;
	}

	@Override
	public String getFriendName(final int i) {
		return ua.h[i].replace((char) 160, ' ');
	}

	@Override
	public int getIgnoredCount() {
		return db.g;
	}

	@Override
	public String getIgnoredName(final int i) {
		return ia.a[i].replace((char) 160, ' ');
	}

	@Override
	public int getPrayerCount() {
		return t.g;
	}

	@Override
	public int getPrayerLevel(final int i) {
		return ca.B[i];
	}

	@Override
	public String getPrayerName(final int i) {
		return t.h[i];
	}

	@Override
	public String[] getSpellNames() {
		return SPELL_NAMES;
	}

	@Override
	public String[] getSkillNames() {
		return SKILL_NAMES;
	}
}
