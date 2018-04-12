package be.nikiroo.utils.main;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import be.nikiroo.utils.StringUtils;
import be.nikiroo.utils.StringUtils.Alignment;

/**
 * Text justification (left, right, center, justify).
 * 
 * @author niki
 */
public class justify {
	/**
	 * Syntax: $0 ([left|right|center|justify]) (max width)
	 * <p>
	 * <ul>
	 * <li>mode: left, right, center or full justification (defaults to left)</li>
	 * <li>max width: the maximum width of a line, or "" for "no maximum"
	 * (defaults to "no maximum")</li>
	 * </ul>
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		int width = -1;
		StringUtils.Alignment align = Alignment.LEFT;

		if (args.length >= 1) {
			align = Alignment.valueOf(args[0].toUpperCase());
		}
		if (args.length >= 2) {
			width = Integer.parseInt(args[1]);
		}

		// TODO: move to utils?
		// Content <-> Bullet spacing (null = no spacing)
		List<Entry<String, String>> lines = new ArrayList<Entry<String, String>>();
		Scanner scan = new Scanner(System.in);
		scan.useDelimiter("\r\n|[\r\n]");
		try {
			StringBuilder previous = null;
			StringBuilder tmp = new StringBuilder();
			String previousItemBulletSpacing = null;
			String itemBulletSpacing = null;
			while (scan.hasNext()) {
				boolean previousLineComplete = true;

				String current = scan.next().replace("\t", "    ");
				itemBulletSpacing = getItemSpacing(current);
				boolean bullet = isItemLine(current);
				if ((previousItemBulletSpacing == null || itemBulletSpacing
						.length() <= previousItemBulletSpacing.length())
						&& !bullet) {
					itemBulletSpacing = null;
				}

				if (itemBulletSpacing != null) {
					current = current.trim();
					if (!current.isEmpty() && bullet) {
						current = current.substring(1);
					}
					current = current.trim();
					previousLineComplete = bullet;
				} else {
					tmp.setLength(0);
					for (String word : current.split(" ")) {
						if (word.isEmpty()) {
							continue;
						}

						if (tmp.length() > 0) {
							tmp.append(' ');
						}
						tmp.append(word.trim());
					}
					current = tmp.toString();

					previousLineComplete = current.isEmpty()
							|| previousItemBulletSpacing != null
							|| (previous != null && isFullLine(previous));
				}

				if (previous == null) {
					previous = new StringBuilder();
				} else {
					if (previousLineComplete) {
						lines.add(new AbstractMap.SimpleEntry<String, String>(
								previous.toString(), previousItemBulletSpacing));
						previous.setLength(0);
						previousItemBulletSpacing = itemBulletSpacing;
					} else {
						previous.append(' ');
					}
				}

				previous.append(current);

			}

			if (previous != null) {
				lines.add(new AbstractMap.SimpleEntry<String, String>(previous
						.toString(), previousItemBulletSpacing));
			}
		} finally {
			scan.close();
		}

		for (Entry<String, String> line : lines) {
			String content = line.getKey();
			String spacing = line.getValue();

			String bullet = "- ";
			if (spacing == null) {
				bullet = "";
				spacing = "";
			}

			if (spacing.length() > width + 3) {
				spacing = "";
			}

			for (String subline : StringUtils.justifyText(content, width
					- (spacing.length() + bullet.length()), align)) {
				System.out.println(spacing + bullet + subline);
				if (!bullet.isEmpty()) {
					bullet = "  ";
				}
			}
		}
	}

	static private boolean isFullLine(StringBuilder line) {
		return line.length() == 0 //
				|| line.charAt(line.length() - 1) == '.'
				|| line.charAt(line.length() - 1) == '"'
				|| line.charAt(line.length() - 1) == '»';
	}

	static private boolean isItemLine(String line) {
		String spacing = getItemSpacing(line);
		return spacing != null && line.charAt(spacing.length()) == '-';
	}

	static private String getItemSpacing(String line) {
		int i;
		for (i = 0; i < line.length(); i++) {
			if (line.charAt(i) != ' ') {
				return line.substring(0, i);
			}
		}

		return "";
	}
}
