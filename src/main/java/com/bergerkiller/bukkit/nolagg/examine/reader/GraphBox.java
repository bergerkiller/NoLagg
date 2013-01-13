package com.bergerkiller.bukkit.nolagg.examine.reader;

import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public abstract class GraphBox extends JPanel {
	private static final long serialVersionUID = 1L;

	private JScrollPane scroller;
	private Graph graph;

	public abstract void onSelectionChange(GraphArea newarea);

	public abstract void onAreaClick(GraphArea area);

	public void orderAreas() {
		this.graph.orderAreas();
	}

	public void reset(int newduration) {
		this.graph.reset(newduration);
	}

	public GraphArea addArea() {
		return this.graph.addArea();
	}

	public void setSelection(int index) {
		this.graph.setSelection(index);
	}

	public GraphArea getArea(int index) {
		return this.graph.getArea(index);
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		this.scroller.setBounds(0, 0, width, height);
	}

	/**
	 * Gets the minimum x coordinate this graph box shows of the contents
	 * 
	 * @return minimum x coordinate
	 */
	public int getMinViewX() {
		return this.scroller.getHorizontalScrollBar().getValue();
	}

	public GraphBox(int x, int y, int width, int height) {
		final GraphBox me = this;

		// adjust size and set layout
		this.setLayout(null);

		// construct components
		this.graph = new Graph(this) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSelectionChange(GraphArea newarea) {
				me.onSelectionChange(newarea);
			}

			@Override
			public void onAreaClick(GraphArea area) {
				me.onAreaClick(area);
			}
		};
		this.scroller = new JScrollPane(graph);

		this.scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

		this.setBounds(x, y, width, height);

		// add components
		this.add(graph);
		this.add(scroller);

		this.scroller.getViewport().add(this.graph);
		this.scroller.setBounds(0, 0, width, height);
		this.graph.setBounds(0, 0, width, height);
		this.graph.setPreferredSize(new Dimension(width, 0));
	}

}
