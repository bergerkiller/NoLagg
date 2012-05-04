package com.bergerkiller.bukkit.nolagg.examine.reader;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileFilter;

import com.bergerkiller.bukkit.nolagg.examine.segments.ExamFile;
import com.bergerkiller.bukkit.nolagg.examine.segments.Segment;
import com.bergerkiller.bukkit.nolagg.examine.segments.SegmentData;

public class ExamReader {

	private static JFileChooser filec;

	public static MainWindow window;
	public static JTextArea description;
	public static JTextField filepath;
	
	public static Segment selectedSegment;
		
	public static void loadSegment(int index) {
		if (selectedSegment != null) {
			if (index == -1) {
				loadSegment(selectedSegment.getParent());
			} else {
				loadSegment(selectedSegment.getSegment(index));
			}
		}
	}
	
	public static void loadSegment(Segment segment) {
		if (segment == null) return;
		selectedSegment = segment;
		window.reset(segment.getDuration());
		for (SegmentData data : segment.getData()) {
			GraphArea area = window.add(data.getName(), data.getTotal());
			int x = 0;
			for (double value : data.getTimes()) {
				area.setValue(x, value);
				x++;
			}
		}
		window.orderAreas();
		description.setText(segment.getDescription());
		description.select(0, 0);
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
					ExamFile file = ExamFile.read(filec.getSelectedFile());
					if (file != null) {
						loadSegment(file);
						filepath.setText(filec.getSelectedFile().toString());
					}
				}
			}
		});
		
		filepath = window.filepath;
		description = window.description;
		
		filepath.setText("You can drop files into this box or click the 'Open' button to choose a file");
		filepath.setDropTarget(new DropTarget() {
			private static final long serialVersionUID = 1L;
			
			public synchronized void drop(DropTargetDropEvent event) {
				event.acceptDrop(event.getDropAction());
				for (File f : fillFiles(event.getTransferable())) {
					if (f.toString().toLowerCase().endsWith(".exam")) {
						ExamFile ex = ExamFile.read(f);
						if (ex != null) {
							loadSegment(ex);
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

	public static void msgbox(String message) {
		JOptionPane.showMessageDialog(null, message);
	}

}
