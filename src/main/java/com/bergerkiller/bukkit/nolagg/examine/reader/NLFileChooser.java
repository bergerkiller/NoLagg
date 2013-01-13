package com.bergerkiller.bukkit.nolagg.examine.reader;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class NLFileChooser extends JFileChooser {
	private static final long serialVersionUID = 1398781849789244927L;
	private final FileNameExtensionFilter filter;

	public NLFileChooser(String title, String type, String ext) {
		super(System.getProperty("user.home"));
		setDialogTitle(title);
		setFileFilter(filter = new FileNameExtensionFilter(type, ext));
	}

	@Override
	public File getSelectedFile() {
		File file = super.getSelectedFile();
		if (this.getFileFilter() == filter && file != null) {
			String path = file.toString();
			for (String ext : filter.getExtensions()) {
				if (path.toLowerCase().endsWith("." + ext)) {
					return file;
				}
			}
			file = new File(path + "." + filter.getExtensions()[0]);
		}
		return file;
	}
}
