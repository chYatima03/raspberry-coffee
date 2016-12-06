package servers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DataFileWriter implements Forwarder {
	private BufferedWriter dataFile;
	private String log;

	public DataFileWriter(String fName) throws Exception {
		this.log = fName;
		try {
			this.dataFile = new BufferedWriter(new FileWriter(fName));
		} catch (Exception ex) {
			throw ex;
		}
	}

	@Override
	public void write(byte[] message) {
		try {
			String mess = new String(message);
			if (!mess.isEmpty()) {
				this.dataFile.write(mess + '\n');
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	@Override
	public void close() {
		System.out.println("- Stop writing to " + this.getClass().getName());
		try {
			this.dataFile.close();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public String getLog() {
		return this.log;
	}
	public static class DataFileBean {
		String cls;
		String log;
		String type = "file";

		public DataFileBean(DataFileWriter instance) {
			cls = instance.getClass().getName();
			log = instance.log;
		}

		public String getLog() {
			return log;
		}
	}

	@Override
	public Object getBean() {
		return new DataFileBean(this);
	}
}
