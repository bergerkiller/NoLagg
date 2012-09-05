package com.bergerkiller.bukkit.nolagg.examine.reader;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.*;

public abstract class SelectionBox extends JPanel {

	private static final long serialVersionUID = 1L;

	private SelectionList list;
	private JScrollPane scroller;
	private int previndex = -1;
	private Vector<Element> data = new Vector<Element>();

	public void setSelection(int index) {
		if (index > data.size() - 1)
			index = -1;
		if (index < -1)
			index = -1;
		if (index == this.previndex)
			return;
		this.previndex = index;
		if (this.previndex == -1) {
			this.list.clearSelection();
		} else {
			this.list.setSelectedIndex(index);
		}
	}

	public SelectionBox(int x, int y, int width, int height) {
		final SelectionBox me = this;

		// adjust size and set layout
		this.setLayout(null);

		// construct components
		this.list = new SelectionList(this.data);
		this.scroller = new JScrollPane(list);

		// add components
		this.add(list);
		this.add(scroller);

		this.scroller.getViewport().add(this.list);
		this.setBounds(x, y, width, height);
		this.list.setBounds(0, 0, width, height);

		this.list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent sel) {
				me.onSelectionChange(me.previndex);
			}
		});

		this.list.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent event) {
				me.setSelection(event.getY() / 26);
			}

			public void mouseDragged(MouseEvent e) {
			}
		});

		this.list.addMouseListener(new MouseListener() {

			public void mouseEntered(MouseEvent arg0) {
			}

			public void mouseExited(MouseEvent arg0) {
			}

			public void mousePressed(MouseEvent arg0) {
			}

			public void mouseReleased(MouseEvent arg0) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				me.setSelection(e.getY() / 26);
				me.onItemClick(me.previndex);
			}

		});
	}

	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		this.scroller.setBounds(0, 0, width, height);
	}

	public void clear() {
		this.data.clear();
		this.previndex = -1;
	}

	public void add(String text, Color color, double totalduration) {
		this.data.add(new Element(text, color, totalduration));
		this.list.setListData(this.data);
	}

	public String getText(int index) {
		return this.data.get(index).text;
	}

	public abstract void onItemClick(int index);

	public abstract void onSelectionChange(int selectedindex);

	private static class Element extends JPanel {
		private static final long serialVersionUID = 1L;

		public final Color selectcolor;
		public boolean selected = false;
		public final String text;

		public Element(final String text, Color color, double totalduration) {
			this.setBackground(color);
			this.text = text;
			this.add(new JLabel(text));
			if (totalduration > 100) {
				totalduration = Math.round(totalduration);
			} else if (totalduration > 10) {
				totalduration = (Math.round(totalduration * 10.0) / 10.0);
			} else if (totalduration > 1) {
				totalduration = (Math.round(totalduration * 100.0) / 100.0);
			} else {
				totalduration = (Math.round(totalduration * 1000.0) / 1000.0);
			}
			this.add(new JLabel(totalduration + " ms"));
			this.selectcolor = GraphColors.findOppositeColor(color);
			for (Component comp : this.getComponents()) {
				comp.setForeground(this.selectcolor);
			}
		}

		public void paint(Graphics graph) {
			super.paint(graph);
			if (this.selected) {
				graph.setColor(Color.RED);
				graph.fillOval(6, 7, 10, 10);
				graph.setColor(Color.BLACK);
				graph.drawOval(6, 7, 10, 10);
			}
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
			if (selected) {
				this.setBorder(new LineBorder(this.selectcolor));
			} else {
				this.setBorder(null);
			}
		}

	}

	private static class SelectionList extends JList {
		private static final long serialVersionUID = 1L;

		public SelectionList(Vector<Element> items) {
			super(items);
			this.setCellRenderer(new CustomCellRenderer());
		}

		class CustomCellRenderer implements ListCellRenderer {
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

				Element component = (Element) value;
				component.setSelected(isSelected);
				return component;
			}
		}
	}

}
