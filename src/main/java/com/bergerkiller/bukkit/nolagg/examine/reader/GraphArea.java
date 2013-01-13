package com.bergerkiller.bukkit.nolagg.examine.reader;

import java.awt.Color;
import java.awt.Graphics;

public class GraphArea {
	public final int width;
	private final int[] ymin;
	private final int[] ymax;
	public final Color color;
	public final Color selectcolor;
	public final double[] values;
	private boolean selected = false;
	public final int index;

	public GraphArea(final int width, final int index) {
		this.width = width;
		this.values = new double[width];
		this.ymin = new int[width];
		this.ymax = new int[width];
		this.color = GraphColors.get(index);
		this.selectcolor = GraphColors.findOppositeColor(this.color);
		this.index = index;
	}

	public boolean isIn(int x, int y) {
		return x >= 0 && x < this.width && y >= this.ymin[x] && y <= this.ymax[x];
	}

	public void setValue(int x, double value) {
		this.values[x] = value;
	}

	public double set(int x, double scale, double offset) {
		this.ymin[x] = (int) (scale * offset);
		offset += this.values[x];
		this.ymax[x] = (int) (scale * offset);
		return offset;
	}

	public void draw(Graphics graphics, final int minViewX, final int maxViewX) {
		graphics.setColor(this.selected ? Color.WHITE : this.color);
		for (int x = 0; x < this.width && x < maxViewX; x++) {
			if (x < minViewX) {
				continue;
			}
			graphics.drawLine(x, this.ymin[x], x, this.ymax[x]);
		}
	}

	public boolean isSelected() {
		return this.selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

}
