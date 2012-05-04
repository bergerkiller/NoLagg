package com.bergerkiller.bukkit.nolagg.examine.reader;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;

public abstract class Graph extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private GraphArea selectedArea = null;
	public Graph() {
		super();
		this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		final Graph graph = this;
		final MouseMotionListener mouselistener = new MouseMotionListener() {

			public void mouseDragged(MouseEvent arg0) {}

			@Override
			public void mouseMoved(MouseEvent e) {
				int y = graph.getHeight() - e.getY() - graphYOffset;
				int x = e.getX() - graphXOffset - 1;
				
				if (graph.selectedArea != null) {
					graph.selectedArea.setSelected(false);
				}
				boolean selected = false;
				if (x < graph.duration && y > 0) {
					for (GraphArea area : graph.areas) {
						if (area.isIn(x, y)) {
							if (area != graph.selectedArea) {
								graph.onSelectionChange(area);
								graph.setSelection(area.index);
							}
							selected = true;
							break;
						}
					}
				}
				if (graph.selectedArea != null && !selected) {
					graph.onSelectionChange(null);
					graph.setSelection(-1);
				}
			}
		};
		
		this.addMouseMotionListener(mouselistener);
		this.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent event) {
				if (event.getWheelRotation() < 0) {
					graph.yscale /= 1.5;
				} else {
					graph.yscale *= 1.5;
				}
				graph.setYScale(graph.yscale);
			}
		});
		this.addMouseListener(new MouseListener() {

			public void mouseEntered(MouseEvent arg0) {}
			public void mouseExited(MouseEvent arg0) {}
			public void mousePressed(MouseEvent arg0) {}
			public void mouseReleased(MouseEvent arg0) {}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				mouselistener.mouseMoved(arg0);
				if (graph.selectedArea == null) {
					graph.onAreaClick(null);
				} else {
					graph.onAreaClick(graph.selectedArea);
				}
			}
		});
	}
	
	public void setSelection(int index) {
		boolean changed = false;
		boolean hasselect = false;
		GraphArea before = this.selectedArea;
		for (GraphArea area : this.areas) {
			if (area.index == index) {
				hasselect = true;
				if (this.selectedArea != area) {
					this.selectedArea = area;
					changed = true;
					area.setSelected(true);
				}
			} else if (area.isSelected()) {
				if (this.selectedArea == area) {
					this.selectedArea = null;
					changed = true;
				}
				area.setSelected(false);
			}
		}
		if (this.selectedArea != null && !hasselect) {
			this.selectedArea.setSelected(false);
			this.selectedArea = null;
			changed = true;
		}
		if (changed) {
			if (before != null) this.repaint(before);
			if (this.selectedArea != null) {
				this.repaint(this.selectedArea);
			}
		}
	}
	
	public abstract void onSelectionChange(GraphArea newarea);
	public abstract void onAreaClick(GraphArea area);
	
	private int duration = 500;
	private double scale = 1.0;
	private double yscale = 1.0;
	public int graphXOffset = 80;
	public int graphYOffset = 50;
	public double maxvalue = 1.0;
	private double[] offset = new double[500];
	
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		this.updateScale(height);
	}
	
	public void updateScale(int newheight) {
		this.scale = (double) (newheight - this.graphYOffset - 20) / this.maxvalue;
		this.scale *= this.yscale;
		this.generateAreas();
	}
	
	public GraphArea getSelectedArea() {
		return this.selectedArea;
	}
	
	public GraphArea getArea(int index) {
		return this.areas.get(index);
	}
	
	private List<GraphArea> areas = new ArrayList<GraphArea>();
	public GraphArea addArea() {
		GraphArea area = new GraphArea(this.duration, this.areas.size());
		this.areas.add(0, area);
		return area;
	}
	public void reset(int newduration) {
		this.areas.clear();
		this.duration = newduration;
		this.selectedArea = null;
		this.setPreferredSize(new Dimension(this.duration + graphXOffset, 100));
		if (this.offset.length != newduration) {
			this.offset = new double[this.duration];
		}
		Arrays.fill(this.offset, 0.0);
	}
	public void generateOffsets() {
		Arrays.fill(offset, 0.0);
		//find out the maximum possible value needed
		for (GraphArea area : this.areas) {
			for (int x = 0; x < this.duration; x++) {
				offset[x] += area.values[x];
			}
		}
		maxvalue = this.getMaxOffset();
		//find out the needed scale
		this.updateScale(this.getHeight());
	}
	public double getMaxOffset() {
		double max = 0.0;
		for (double value : offset) {
			max = Math.max(max, value);
		}
		return max;
	}
	public void generateAreas() {
		//generate
		Arrays.fill(offset, 0.0);
		for (GraphArea area : this.areas) {
			for (int x = 0; x < this.duration; x++) {
				offset[x] = area.set(x, this.scale, offset[x]);
			}
		}
	}
	public void orderAreas() {
		this.generateOffsets();
		this.setYScale(1.0);
	}
	
	public void drawText(double value, Graphics g, int x, int y, int mode) {
		drawText(Double.valueOf(value).toString(), g, x, y, mode);
	}
	public void drawText(int value, Graphics g, int x, int y, int mode) {
		drawText(Integer.valueOf(value).toString(), g, x, y, mode);
	}
	public void drawText(String text, Graphics g, int x, int y, int mode) {
        int stringLen = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();  
        if (mode == 1) {
            x -= stringLen / 2;
        } else if (mode == 2) {
            x -= stringLen;
        }
        g.drawString(text, x, y);  
	}
		
	public void repaint(GraphArea area) {
		Graphics2D g2d = (Graphics2D) this.getGraphics();
		g2d.translate(this.graphXOffset + 1, this.getHeight() - this.graphYOffset);
		g2d.scale(1.0, -1.0);    // invert
		area.draw(g2d);
	}
	
	public void setYScale(double yscale) {
		if (yscale < 1.0) {
			yscale = 1.0;
		}
		this.yscale = yscale;
		this.updateScale(this.getHeight());
		this.repaint();
	}
	
	protected void paintComponent(Graphics g) {

		g.setColor(Color.BLACK);
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		
		//correctly invert coordinates
		Graphics2D g2d = (Graphics2D) g;

		if (this.areas.isEmpty()) {
			g.setColor(Color.RED);
			drawText("In here the process time in every tick is displayed in a graph", g, 300, 50, 1);
			drawText("You can hover over the graph or selection box to the right to highlight", g, 300, 80, 1);
			drawText("By clicking on part of the graph you can look deeper into the results", g, 300, 110, 1);
			drawText("Clicking on the background will make you go back one step", g, 300, 140, 1);
		}
		
		g2d.translate(this.graphXOffset + 1, this.getHeight() - this.graphYOffset);
		g2d.scale(1.0, -1.0);    // invert

		for (GraphArea area : this.areas) {
			area.draw(g);
		}
		g.translate(-1, 0);
		
		g2d.scale(1.0, -1.0);    // invert
		
		int bleft = this.getHeight() - this.graphYOffset;
		
		//draw scaler
		g.setColor(Color.WHITE);
		g.drawLine(0, 0, this.duration, 0);
		g.drawLine(0, 0, 0, -bleft);
		
		//horizontal + ticks
		int xinterval = 30;
		for (int x = 0; x <= this.duration / xinterval; x++) {
			g.drawLine(x * xinterval, 0, x * xinterval, 10);
			drawText(x * xinterval, g, x * xinterval, 22, 1);
		}
		drawText("Time (ticks)", g, this.duration / 2, 40, 1);
		
		//vertical value using scale
		int yinterval = 30;
		int ymax = (this.getHeight() - this.graphYOffset) / yinterval;
		for (int y = 0; y <= ymax; y++) {
			g.drawLine(0, -y * yinterval, -10, -y * yinterval);
			
			double v = (y * yinterval) / this.scale;
			v = Math.round(v * 100.0) / 100.0;
			drawText(v, g, -19, -y * yinterval + 5, 2);
		}
		
		//rotated text
		g2d.rotate(-Math.PI * 0.5);
		drawText("Time spent (milliseconds)", g2d, ymax * yinterval / 2, -60, 1);
	}

}
