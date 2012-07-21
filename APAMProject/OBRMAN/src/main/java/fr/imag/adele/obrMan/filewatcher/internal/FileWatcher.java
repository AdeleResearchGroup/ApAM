package fr.imag.adele.obrMan.filewatcher.internal;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.TimerTask;

public abstract class FileWatcher extends TimerTask {
	private long timeStamp;
	private File file;
	private SimpleDateFormat sdf;

	public FileWatcher(File file) {
		this.file = file;
		this.timeStamp = file.lastModified();
		
		sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		System.out.println("Watching repository ("+file.exists()+") "+ file + " version " + sdf.format(timeStamp));
	}

	public final void run() {
		
		long timeStamp = file.lastModified();
		if (this.timeStamp != timeStamp) {
			this.timeStamp = timeStamp;
			onChange(file,sdf.format(timeStamp));
		}
	}

	protected abstract void onChange(File file, String date);
}