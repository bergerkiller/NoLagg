package com.bergerkiller.bukkit.nolagg.examine.reader;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;

public class GraphColors {

	public static Color get(int index) {
		return colors[index % colors.length];
	}

	private static Color[] colors;
	static {
		int[] codes = new int[] { 0, 128, 255 };
		colors = new Color[(int) Math.pow(codes.length, 3) - 2];
		int index = 0;
		for (int r : codes) {
			for (int g : codes) {
				for (int b : codes) {
					if (r == 0 && g == 0 && b == 0)
						continue;
					if (r == 255 && g == 255 && b == 255)
						continue;
					colors[index] = new Color(r, g, b);
					index++;
				}
			}
		}
		Arrays.sort(colors, new Comparator<Color>() {
			public int compare(Color c1, Color c2) {
				int diff = c1.getRed() - c2.getRed();
				diff += c1.getGreen() - c2.getGreen();
				diff += c1.getBlue() - c2.getBlue();
				return diff;
			}
		});
		reverse(colors);
	}

	/*
	 * Source:
	 * http://www.leepoint.net/notes-java/data/arrays/arrays-ex-reverse.html
	 */
	public static void reverse(Object[] b) {
		int left = 0; // index of leftmost element
		int right = b.length - 1; // index of rightmost element

		while (left < right) {
			// exchange the left and right elements
			Object temp = b[left];
			b[left] = b[right];
			b[right] = temp;

			// move the bounds toward the center
			left++;
			right--;
		}
	}

	public static int findOppositeColor(int color) {
		return (color ^ 0x80) & 0xff;
	}

	public static Color findOppositeColor(Color color) {
		int r = findOppositeColor(color.getRed());
		int g = findOppositeColor(color.getGreen());
		int b = findOppositeColor(color.getBlue());
		return new Color(r, g, b);
	}

	public static Color getAverage(Color c1, Color c2) {
		return new Color((c1.getRed() + c2.getRed()) / 2, (c1.getGreen() + c2.getGreen()) / 2, (c1.getBlue() + c2.getBlue()) / 2);
	}

}
