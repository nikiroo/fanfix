package be.nikiroo.utils.main;

import java.util.ArrayList;
import java.util.List;
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
		List<String> lines = new ArrayList<String>();
		Scanner scan = new Scanner(System.in);
		scan.useDelimiter("\r\n|[\r\n]");
		try {
			StringBuilder previous = null;
			StringBuilder tmp = new StringBuilder();
			while (scan.hasNext()) {
				String current = scan.next();
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

				if (previous == null) {
					previous = new StringBuilder();
				} else {
					if (current.isEmpty() || isFullLine(previous)) {
						lines.add(previous.toString());
						previous.setLength(0);
					} else {
						previous.append(' ');
					}
				}

				previous.append(current);
			}

			if (previous != null) {
				lines.add(previous.toString());
			}
		} finally {
			scan.close();
		}

		// TODO: supports bullet lines "- xxx" and sub levels
		for (String line : lines) {
			for (String subline : StringUtils.justifyText(line, width, align)) {
				System.out.println(subline);
			}
		}
	}

	static private boolean isFullLine(StringBuilder line) {
		return line.length() == 0 //
				|| line.charAt(line.length() - 1) == '.'
				|| line.charAt(line.length() - 1) == '"'
				|| line.charAt(line.length() - 1) == '»'
		;
	}
}
