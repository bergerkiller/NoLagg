package com.bergerkiller.bukkit.nolagg.examine.reader;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileFilter;

public class ExamReader {

	private static JFileChooser filec;

	public static MainWindow window;
	public static JTextArea locations;
	public static JTextField filepath;
	private static ExamFile source = null;
	public static Segment[] displayedSegments;
	public static PluginInfo selectedPlugin;
	public static boolean isSingleSelected = false;
	public static void load(ExamFile newsource) {
		newsource.sort();
		source = newsource;
		selectPlugin((PluginInfo) null);
	}

	public static void setSelectedInfo(int index) {
		StringBuilder info = new StringBuilder();
		double dur = 0.0;
		double total = 0.0;
		if (index == -1) {
			if (selectedPlugin != null) {
				if (selectedPlugin.plugin.startsWith("#")) {
					info.append("Selected server operation: ").append(selectedPlugin.plugin.substring(1));
				} else {
					info.append("Selected plugin: ").append(selectedPlugin.plugin);
				}
				info.append("\nEvent count: ");
				info.append(selectedPlugin.segments.size() - selectedPlugin.taskcount);
				info.append("\nTask count: ").append(selectedPlugin.taskcount);
				//get average duration
				for (double time : selectedPlugin.getAll().times) {
					total += time;
					dur += time / (double) source.duration;
				}
			} else {
				info.append("Plugin count: " + source.getPluginCount());
				info.append("\nRunning tasks: " + source.taskcount);
				//get average duration
				PluginInfo tinfo = new PluginInfo(source.duration, null);
				tinfo.segments.addAll(source.segments);
				for (double time : tinfo.getAll().times) {
					total += time;
					dur += time / (double) source.duration;
				}
			}
			info.append("\nAverage duration: ");
			info.append(Math.round(dur * 1000.0) / 1000.0).append(" ms/tick");
			info.append("\nTotal duration: ");
			info.append(Math.round(total * 1000.0) / 1000.0).append(" ms/");
			info.append(source.duration).append(" ticks");
		} else {
			String loc = displayedSegments[index].location;
			if (displayedSegments[index].plugin.startsWith("#")) {
				info.append("Server operation: ").append(displayedSegments[index].plugin.substring(1));
			} else {
				info.append("Plugin: ").append(displayedSegments[index].plugin);
			}
			for (double time : selectedPlugin.segments.get(index).times) {
				total += time;
				dur += time / (double) source.duration;
			}
			info.append("\nAverage duration: ");
			info.append(Math.round(dur * 1000.0) / 1000.0).append(" ms/tick");
			info.append("\nTotal duration: ");
			info.append(Math.round(total * 1000.0) / 1000.0).append(" ms/");
			info.append(source.duration).append(" ticks");
			
			if (loc == null) {
				info.append("\nNo creation stacktrace available");
			} else {
				info.append('\n').append(loc);
			}

		}
		locations.setText(info.toString());
		locations.select(0, 0);
	}

	public static void selectSegment(int index) {
		setSelectedInfo(index);
		displayedSegments = new Segment[] {displayedSegments[index]};
		isSingleSelected = true;
		window.reset(displayedSegments[0].times.length);
		for (Segment seg : displayedSegments) {
			GraphArea area = window.add(seg.name, seg.getTotal());
			for (int x = 0; x < source.duration; x++) {
				area.setValue(x, seg.times[x]);
			}
		}
		window.orderAreas();
	}
	
	public static void selectPlugin(String pluginname) {
		if (source == null) return;
		selectPlugin(pluginname == null ? null : source.getInfo(pluginname));
	}
	public static void selectPlugin(PluginInfo plugin) {
		if (source == null) return;
		window.reset(source.duration);
		selectedPlugin = plugin;
		if (plugin == null) {
			displayedSegments = new Segment[source.pluginSegments.size()];
			int segi = 0;
			for (PluginInfo info : source.pluginSegments.values()) {
				Segment seg = info.getAll();
				displayedSegments[segi] = seg;
				segi++;
				GraphArea area = window.add(info.plugin, info.getTotal());
				for (int x = 0; x < source.duration; x++) {
					area.setValue(x, seg.times[x]);
				}
			}
		} else {
			displayedSegments = plugin.segments.toArray(new Segment[0]);
			for (Segment seg : plugin.segments) {
				GraphArea area = window.add(seg.name, seg.getTotal());
				for (int x = 0; x < source.duration; x++) {
					area.setValue(x, seg.times[x]);
				}
			}
		}
		window.orderAreas();
		setSelectedInfo(-1);
		isSingleSelected = false;
	}

	public static void main(String[] args) {
		filec = new JFileChooser();
		filec.setFileFilter(new FileFilter(){
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().toLowerCase().endsWith(".exam");
			}
			public String getDescription() {
				return "Examination files";
			}
		});

		window = new MainWindow();
		JButton loadbutton = window.append(new JButton());
		loadbutton.setText("Open");
		loadbutton.setBounds(5, 5, 100, 30);
		loadbutton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//open a new dialog
				if (filec.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
					ExamFile file = read(filec.getSelectedFile());
					if (file != null) {
						load(file);
						filepath.setText(filec.getSelectedFile().toString());
					}
				}
			}
		});
		
		filepath = window.filepath;
		locations = window.locations;
		
		filepath.setText("You can drop files into this box or click the 'Open' button to choose a file");
		filepath.setDropTarget(new DropTarget() {
			private static final long serialVersionUID = 1L;
			
			public synchronized void drop(DropTargetDropEvent event) {
				event.acceptDrop(event.getDropAction());
				for (File f : fillFiles(event.getTransferable())) {
					if (f.toString().toLowerCase().endsWith(".exam")) {
						ExamFile ex = read(f);
						if (ex != null) {
							load(ex);
							filepath.setText(f.toString());
						}
						break;
					}
				}
			}
			
		});
		
		filepath.setTransferHandler(new TransferHandler() {
			private static final long serialVersionUID = 1L;
			
			public boolean canImport(TransferSupport support) {
				if (support.isDrop()) {
					for (File f : fillFiles(support.getTransferable())) {
						if (f.isFile()) {
							String path = f.toString().toLowerCase();
							if (path.endsWith(".exam")) {
								support.setDropAction(LINK);
								return true;
							}
						}
					}
				}
				return false;
			}
			
			public boolean importData(TransferSupport support) {
				return canImport(support);
			}

		});
	}
	
	private static List<File> filebuff;
	
	@SuppressWarnings("unchecked")
	public static List<File> fillFiles(Transferable transferable) {
		try {
			filebuff = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
		} catch (UnsupportedFlavorException  ex) {
			filebuff = new ArrayList<File>();
			try {
				final DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
				String data = (String)transferable.getTransferData(nixFileDataFlavor);
				for(StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens();)
				{
				    String token = st.nextToken().trim();
				    if(token.startsWith("#") || token.isEmpty()) {
				         continue;
				    }
				    filebuff.add(new File(new URI(token)));
				}
			} catch (Exception ex2) {
				ex2.printStackTrace();
				filebuff.clear();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (filebuff == null) filebuff = new ArrayList<File>(0);
		return filebuff;
	}

	public static String getPrio(int slot) {
		switch (slot) {
		case 0 : return "LOWEST";
		case 1 : return "LOW";
		case 3 : return "HIGH";
		case 4 : return "HIGHEST";
		case 5 : return "MONITOR";
		default : return "NORMAL";
		}
	}

	public static void msgbox(String message) {
		JOptionPane.showMessageDialog(null, message);
	}
	public static ExamFile read(File file) {
		ExamFile efile = null;
		try {
			DataInputStream stream = new DataInputStream(new InflaterInputStream(new FileInputStream(file)));
			try {
				try {
					efile = read(stream);
				} catch (ZipException ex) {
					stream.close();
					stream = new DataInputStream(new FileInputStream(file));
					efile = read(stream);
				}
			} catch (Exception ex) {
				msgbox("Failed to load file: \n\n" + ex.toString());
			} finally {
				stream.close();
			}
		} catch (Throwable ex) {
			msgbox("Failed to load file: \n\n" + ex.toString());
		}
		return efile;
	}

	public static ExamFile read(DataInputStream stream) throws IOException {
		ExamFile file = new ExamFile();

		int listenercount = stream.readInt();
		file.duration = stream.readInt();
		for (int i = 0; i < listenercount; i++) {
			if (stream.readBoolean()) {
				String plugin = stream.readUTF();
				String name = stream.readUTF() + "[" + getPrio(stream.readInt()) + "]";
				String loc = stream.readUTF();
				file.addSegment(name, loc, plugin, false).readLongValues(stream);
			}
		}
		file.taskcount = stream.readInt();
		for (int i = 0; i < file.taskcount; i++) {
			String name = stream.readUTF();
			String plugin = stream.readUTF();
			int loccount = stream.readInt();
			StringBuilder location = new StringBuilder(loccount * 300);
			if (!plugin.startsWith("#")) {
				location.append(name);
			}
			for (int j = 0; j < loccount; j++) {
				location.append('\n').append('\n').append(stream.readUTF());
			}
			String segname;
			if (plugin.startsWith("#")) {
				segname = location.toString();
			} else {
				segname = "Task #" + (i + 1);
			}
			file.addSegment(segname, location.toString(), plugin, true).readLongValues(stream);
		}

		return file;
	}

}
