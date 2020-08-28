package dk.aau.netsec.hostage.logging.formatter;


import dk.aau.netsec.hostage.logging.Record;
import dk.aau.netsec.hostage.logging.RecordAll;

public class TraCINgFormatter extends Formatter {

	private static Formatter INSTANCE = new TraCINgFormatter();

	public static Formatter getInstance() {
		return INSTANCE;
	}

	private TraCINgFormatter() {
	}

	@Override
	public synchronized String format(Record record) {
		return String
				.format("{ \"sensor\":{\"type\": \"Honeypot\", \"name\": \"HosTaGe\"}, \"type\": \"%s server access\", \"src\":{\"ip\": \"%s\", \"port\": %d}, \"dst\":{\"ip\": \"%s\", \"port\": %d} }",
						record.getProtocol(), record.getRemoteIP(), record.getRemotePort(), record.getExternalIP(), record.getLocalPort());
	}

	@Override
	public String format(RecordAll record) {
		return  String
				.format("{ \"sensor\":{\"type\": \"Honeypot\", \"name\": \"HosTaGe\"}, \"type\": \"%s server access\", \"src\":{\"ip\": \"%s\", \"port\": %d}, \"dst\":{\"ip\": \"%s\", \"port\": %d} }",
						record.getProtocol(), record.getRemoteIP(), record.getRemotePort(), record.getExternalIP(), record.getLocalPort());
	}

	@Override
	public String toString(){
		return "tracing";
	}
}
